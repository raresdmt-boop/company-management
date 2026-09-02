# Code review — company-management

Proiect Spring Boot 4.1.1 / Java 21, temă JPA 1—* (`Department` 1—* `Employee`) expusă prin REST.

---

## Runda 3 — 2026-09-02 — sha `edebe10`

**Poarta de compilare:** ✅ trece.
**Verificat prin rulare:** pornit pe H2, 14 probe HTTP. Fiecare 🔴 de mai jos are cererea și răspunsul exact.

Rundă mare și, în bună parte, bună: ai închis 7 constatări, ai scris de la zero `EmployeeController`, `EmployeeQueryService`, PUT/PATCH/DELETE pe departament și ai refăcut seeder-ul. Diagnosticul de mai jos e lung pentru că ai scris mult cod nou, nu pentru că ai regresat.

### ✅ Ce ai închis

| Din | Constatare | Verificat |
|---|---|---|
| R2 B1 | Buildul e rupt (rename de fișier fără rename de tip) | `mvn compile` trece |
| R1 B3 | Lista goală tratată ca eroare, la departamente | `GET /api/department` pe DB goală → **`200 []`** |
| R1 B5 | `jobTitle` DTO 50 vs coloană 20 | acum `@Size(min = 2, max = 20)` ↔ `length = 20`; ai pus și `@Size` pe `email` în entitate |
| R1 B1 | `@Valid` nu coboară în `List<EmployeeCreateRequest>` | rezolvat **prin eliminare** — ai scos lista din `DepartmentCreateRequest` (vezi nota de mai jos) |
| R1 M2 | `hibernate.dialect` hardcodat | scos din `application.yaml` |
| R1 M3 | Un `GET` = două SELECT-uri, la departamente | acum unul singur |
| R1 M6 | Niciun serviciu `@Transactional` | pus pe toate patru |
| R1 M9 | `EmployeeComandService` | redenumit corect, fișier + tip |

### ⭐ Un lucru pe care l-ai făcut mai bine decât ceream

`DepartmentRepository.java:19`

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update Department d set d.budget = :newBudget where d.name = :name")
```

Cele două flag-uri nu ți le-a cerut nimeni, și sunt exact ce trebuie. Merită să știi **de ce**, pentru că e capcana clasică a lui `@Modifying`:

Un `update` JPQL de tip bulk se duce **direct în baza de date**, ocolind persistence context-ul. Hibernate nu are cum să ghicească ce entități din memorie a invalidat. Fără `clearAutomatically = true`, entitatea `Department` pe care o ai deja încărcată rămâne cu bugetul vechi, și orice citire ulterioară din aceeași tranzacție îți dă valoarea veche — un update care „nu se vede", cel mai frustrant bug din JPA. `flushAutomatically = true` rezolvă simetricul: forțează scrierea modificărilor în așteptare **înainte** de bulk update, ca acesta să nu lucreze pe date pe cale să fie suprascrise.

Ține minte perechea. E aceeași capcană pe care ai avut-o și la `academy-hub-api`.

---

## 🔴 Critice

### B1 — `changeBudget` modifică după **nume**, nu după id: schimbi bugetul unui departament și se schimbă al altora

`department/services/DepartmentCommandServiceImpl.java:73-82`

```java
Department department = departmentRepository.findById(id).orElseThrow();
int updatedRows = departmentRepository.updateBudgetByName(department.getName(), changeBudgetRequest.budget());
```

**CUM E ACUM.** Am creat două departamente, ambele numite „Legal" — unul la Cluj cu buget 1000, unul la Iași cu buget 2000. Am cerut schimbarea bugetului **doar** pentru id-ul 4:

```
PATCH /api/department/4/budget   {"budget": 999999.0}
-> {"id":4,"name":"Legal","budget":999999.0,"updatedRow":2}

id 4 (Cluj): budget 999999.0   <- cerut
id 5 (Iasi): budget 999999.0   <- NU a cerut nimeni asta
```

**DE CE.** Ai făcut căutarea corect — `findById(id)` — și apoi ai aruncat id-ul. Din entitatea găsită ai luat `getName()` și ai trimis **numele** în clauza `where` a update-ului. Iar numele nu e unic în tabelul tău: n-ai nicio constrângere care să împiedice două departamente „Legal" (e M10 mai jos, deschisă din runda 1). Un `update ... where d.name = :name` lovește **toate** rândurile care se potrivesc, nu unul.

Update-ul bulk nici măcar nu știe că tu ai încărcat o entitate anume — pentru el, `Department` cu id 4 nu există ca noțiune; există doar o condiție pe o coloană. Cheia primară e singura coloană despre care baza de date îți garantează că selectează exact un rând. Orice altceva e o presupunere.

Și observă că API-ul tău **își mărturisește singur** greșeala: `"updatedRow": 2`. Ai returnat numărul de rânduri afectate, clientul a cerut un departament, iar răspunsul spune negru pe alb că s-au modificat două. Un câmp care nu poate fi decât `1` la un endpoint corect e o alarmă gratuită — dar numai dacă cineva o citește.

**FIX:** `where d.id = :id`, și trimite `id`, nu `department.getName()`. Cu asta, `findById` de dinainte nu-ți mai trebuie decât ca să dai 404 când id-ul nu există.

### B2 — Tot nu există `@RestControllerAdvice`: acum ai patru excepții și toate ies ca 500

A treia rundă la rând pentru constatarea asta.

**CUM E ACUM.** Patru cauze complet diferite, același răspuns gol:

```
PATCH /api/department/999/budget  -> 500   [NoSuchElementException]
DELETE /api/department/999        -> 500   [NoSuchElementException]
PUT   /api/department/999         -> 500   [DepartmentIdNotFound]
GET   /api/employees  (0 angajati)-> 500   [NoEmployeesFound]
```

**DE CE.** Când o excepție iese din controller, Spring o dă pe rând la niște `HandlerExceptionResolver`. Cel implicit știe să traducă **doar excepțiile lui**: `MethodArgumentNotValidException` → 400, `HttpMessageNotReadableException` → 400, `NoHandlerFoundException` → 404. `NoEmployeesFound` sau `DepartmentIdNotFound` nu-i spun nimic — sunt `RuntimeException`-uri anonime pentru el. Când niciun resolver nu recunoaște excepția, cererea ajunge la ultima plasă, cea a containerului, care nu poate presupune nimic despre ce s-a întâmplat și răspunde onest: 500, „ceva a crăpat pe server".

`@RestControllerAdvice` e exact locul unde tu îi dai lipsa asta de vocabular: un `@ExceptionHandler` per excepție, cu `@ResponseStatus`-ul potrivit. E singurul strat din aplicație care are voie să știe și de business (`NoEmployeesFound`) și de HTTP (404), fără să le amestece în servicii.

Cât timp lipsește, un client al API-ului tău nu poate distinge „nu există" de „ai trimis prostii" de „a picat baza de date". Iar tu depanezi din log, nu din răspuns.

**FIX:** un `@RestControllerAdvice` cu handlere pentru: excepțiile tale de „nu găsesc" → 404, `MethodArgumentNotValidException` → 400 cu lista câmpurilor, `ConstraintViolationException` → 400, `DataIntegrityViolationException` → 409.

### B3 — `orElseThrow()` fără argument: arunci `NoSuchElementException`, deși ai scris clase de excepție pentru asta

`DepartmentCommandServiceImpl.java:74` (`changeBudget`) și `:86` (`deleteDepartment`)

```java
Department department = departmentRepository.findById(id).orElseThrow();
```

**CUM E ACUM.** `PATCH /api/department/999/budget` și `DELETE /api/department/999` aruncă `java.util.NoSuchElementException` — confirmat în log, de două ori.

**DE CE.** `Optional` are două metode cu același nume și înțelesuri diferite. `orElseThrow()` fără argument e varianta „nu-mi pasă ce excepție, doar nu-mi da null" — aruncă `NoSuchElementException`, o excepție din `java.util` care nu spune nimic despre domeniul tău. `orElseThrow(Supplier)` e varianta în care **tu** alegi excepția, și abia aia poate fi tradusă mai târziu în 404 de către advice.

Diferența devine vizibilă abia când adaugi advice-ul din B2: `NoSuchElementException` n-ai cum s-o mapezi la 404 fără să prinzi, la grămadă, orice altă bibliotecă care aruncă la fel. Excepția ta proprie e adresabilă.

Detaliul care doare: ai **scris** `DepartmentNotFound` în runda asta (`department/exceptions/DepartmentNotFound.java`) și n-o folosești nicăieri. Ai clasa, ai mesajul, doar n-ai pus-o unde trebuia.

**FIX:** `.orElseThrow(DepartmentIdNotFound::new)` în ambele locuri.

### B4 — `DELETE` pe un departament îi șterge și angajații

`department/models/Department.java:43-48`

**CUM E ACUM.** Am șters departamentul IT, care avea 3 angajați:

```
DELETE /api/department/1  -> {"id":1,"name":"IT"}

GET /api/employees  inainte: 7 angajati
GET /api/employees  dupa:    4 angajati  (Rares, Andrei si Maria au disparut)
```

**DE CE.** Două setări de pe `@OneToMany` lucrează împreună:

```java
cascade = CascadeType.ALL,
orphanRemoval = true
```

`CascadeType.ALL` înseamnă „propagă către copii **toate** operațiile pe care le fac pe părinte" — inclusiv `REMOVE`. `orphanRemoval = true` merge mai departe: „dacă un copil e scos din colecție, șterge-l din baza de date, chiar dacă n-am cerut explicit `remove`".

Amândouă sunt corecte când copiii nu au sens fără părinte — pozițiile unei facturi, de exemplu: nu există „linie de factură orfană". Un angajat **nu** e în categoria asta. Un om există independent de departamentul în care lucrează; când desființezi departamentul, oamenii se mută, nu dispar.

Nu e neapărat un bug — e o **decizie de design pe care ai luat-o fără să o iei**, prin copierea unui `cascade = ALL` implicit. Întrebarea la care trebuie să răspunzi tu: în aplicația asta, ce înseamnă „șterg un departament"?

**FIX**, în funcție de răspuns:
- „angajații rămân, fără departament" → `cascade = {PERSIST, MERGE}`, fără `orphanRemoval`, iar în `deleteDepartment` dezleagă întâi angajații (`department_id = null`).
- „nu poți șterge un departament cu angajați" → verifici `!department.getEmployees().isEmpty()` și arunci o excepție de business → 409.

### B5 — Încă nu se poate **crea** un angajat prin API

`employee/controllers/EmployeeController.java` are un singur `@GetMapping`. `createEmployeeWithDepartment` există, e corect, e `@Transactional` — și e chemat exclusiv de `DataSeeder`.

**CUM E ACUM.** Toate variantele plauzibile de path:

```
POST /api/employees            -> 405 Method Not Allowed
POST /api/department/2/employees -> 404
POST /api/employee             -> 404
```

**DE CE.** `405` de la prima e chiar semnalul util: Spring îți spune „path-ul `/api/employees` există, dar nu am mapping pentru metoda POST" — adică ai controller-ul, îți lipsește doar metoda. La celelalte două nici path-ul nu există.

Practic, singurul mod în care un angajat ajunge în baza de date e seeder-ul de la pornire. Un client al API-ului tău poate citi angajați, dar nu poate adăuga niciunul.

E a treia rundă pentru constatarea asta (B4 în runda 1, B6 în runda 2). Ai construit între timp tot ce e în jur — serviciu, DTO, validare, tranzacție — și ai sărit peste cele 5 rânduri care le fac accesibile.

**FIX:** un `@PostMapping("/{departmentId}/employees")` în `DepartmentController` (angajatul se creează *în* un departament, deci resursa e imbricată), care primește `@Valid @RequestBody EmployeeCreateRequest` și `@PathVariable Long departmentId`, și returnează `201 Created`.

### B6 — Ai reparat regula într-un fișier și ai rescris-o greșit în celălalt, în aceeași rundă

`employee/services/EmployeeQueryServiceImpl.java:26-29`

```java
if(employeeRepository.findAll().isEmpty()) {
    throw new NoEmployeesFound();
}
return employeeRepository.findAll()
```

**CUM E ACUM.** Exact cele două defecte pe care tocmai le-ai șters din `DepartmentQueryServiceImpl`:

```
GET /api/employees  (0 angajati)  -> 500     (o colectie goala nu e o eroare)
GET /api/employees  (cu date)     -> 2 SELECT-uri identice, masurat cu show-sql
```

**DE CE.** Asta nu e o greșeală de Spring, e una de metodă de lucru, și e cel mai util lucru din review-ul ăsta. În runda asta ai deschis `DepartmentQueryServiceImpl`, ai șters `isEmpty() + throw`, ai pus rezultatul într-o singură chemare — perfect. Apoi ai scris `EmployeeQueryServiceImpl` de la zero și ai reprodus **fix aceleași două erori**, pentru că le-ai reparat ca pe niște *linii*, nu ca pe o *regulă*.

Tiparul ăsta („repar apariția semnalată, las celelalte apariții") ți l-am semnalat și la `academy-hub-api`, unde regula era apărată la `create` și cădea la `update`. E același mecanism.

Antidotul e mecanic, nu ține de talent: **după ce închizi o constatare, caută-i forma în tot proiectul înainte de commit.** Aici erau două căutări de câte zece secunde:

```
grep -rn "isEmpty()" src/main/java
grep -rn "findAll()" src/main/java
```

Prima ți-ar fi arătat al doilea `throw`, a doua ți-ar fi arătat dubla interogare.

**FIX:** scoate `if/throw`, o singură chemare `findAll()` într-o variabilă, `200 []` pe listă goală.

---

## 🟡 Importante

**M1 — interfața de serviciu importă adnotări de Spring Web.** `department/services/interfaces/DepartmentCommandService.java:12-13` — `@PathVariable` și `@RequestBody` pe parametrii unei metode de serviciu. Nu fac nimic acolo (le citește doar `DispatcherServlet`-ul, pe metodele de controller), dar semnalul e mai grav decât efectul: stratul de business ajunge să știe că deasupra lui e HTTP. Mâine, când chemi același serviciu dintr-un job programat sau dintr-un test, adnotările n-au niciun sens. Șterge-le, cu tot cu importuri.

**M2 — `PUT` cu semantică de `PATCH`.** `DepartmentCommandServiceImpl.java:54-62` actualizează doar câmpurile ne-null. Efectul, măsurat: `PUT /api/department/4` cu body `{}` → **200**, și nu se schimbă nimic. `PUT` înseamnă „înlocuiește resursa cu ce-ți trimit"; câmpurile lipsă ar trebui respinse (`@NotBlank` pe `DepartmentUpdateRequest`) sau șterse. Actualizarea parțială o ai deja, separat, pe `PATCH /{id}/budget`. Alege: ori faci `PUT`-ul strict, ori îl transformi în `PATCH`.

**M3 — `existsById` + `findById` = două interogări.** `DepartmentCommandServiceImpl.java:49-52`. `findById(id).orElseThrow(DepartmentIdNotFound::new)` face aceeași treabă cu o singură interogare — și rezolvă și B3 în același timp.

**M4 — trei excepții pentru aceeași idee.** `DepartmentIdNotFound`, `DepartmentNotFound` (scrisă în runda asta, nefolosită nicăieri) și `NoDepartmentFound` (rămasă din runda 1, acum nefolosită). Păstrează una, șterge două. Trei nume pentru „nu găsesc departamentul" înseamnă că peste o lună o să pui `@ExceptionHandler` pe cea greșită.

**M5 — `@Transactional` de la `jakarta.transaction`, nu de la Spring.** Toate cele patru servicii. Funcționează, dar varianta `org.springframework.transaction.annotation.Transactional` are `readOnly = true` — iar cele două servicii de query sunt acum marcate ca tranzacții de scriere. `readOnly` scutește Hibernate de dirty checking la commit și, pe unele baze, îi permite driverului să rutdeze citirea către o replică.

**M6 — POST tot răspunde `200 OK`** în loc de `201 Created` + `Location`. Deschis din runda 1.

**M7 — colecția Postman: „GET all join fetch" cere tot `/api/company`** → 404. Ai rescris-o aproape complet și frumos (Department / Employee, PUT, PATCH, DELETE), dar cererea asta a rămas pe path-ul șters în `19753fd`.

**M8 — `/api/department` la singular, `/api/employees` la plural.** Acum inconsistența e în interiorul aceleiași aplicații. Pluralul e convenția: `/api/departments`.

**M9 — `ChangeBudgetResponse.updatedRow`** expune clientului un detaliu de implementare (că sub capotă e un update bulk). Într-un API corect, numărul de rânduri afectate de un `PATCH` pe **o** resursă e întotdeauna 1 — deci câmpul n-are ce comunica. Scoate-l după ce repari B1.

**M10 — numele departamentului tot nu e unic.** E cauza-rădăcină a lui B1. Dacă regula de business e „un departament pe nume", pune `@UniqueConstraint` pe `department(name)` — ai făcut-o corect pe `employee(email)` și pe `access_code`, deci știi cum.

**M11 — `mvnw` tot comis fără bit de execuție** (mod `100644`): `git update-index --chmod=+x mvnw`.

---

## 🟢 Cleanups

- **C1 — importuri nefolosite, inclusiv unele adăugate în runda asta:** `EmployeeCreateRequest.java:4` și `EmployeeQueryService.java:4` importă `Employee` (un DTO și o interfață de serviciu n-au ce ști despre entitate) · `DepartmentCreateRequest.java:3,5,7` — `Nullable`, `EmployeeCreateRequest`, `List`, rămase după ce ai scos lista de angajați · `DepartmentQueryServiceImpl.java:7,11` — `NoDepartmentFound` și `ArrayList`, rămase după ce ai reparat B3 din runda 1 · `DepartmentCommandServiceImpl.java:12` — `Employee`.
- **C2 — mapare duplicată.** `DepartmentCommandServiceImpl.java:37-43` reconstruiește `DepartmentResponse` de mână deși există `DepartmentResponse.from(...)`. Idem în `EmployeeCommandServiceImpl.java:50-54`.
- **C3 — `jakarta.annotation.Nullable`** pe `Department.java:38` nu face nimic la runtime.
- **C4 — typo:** `EmployeeCreateRequest.java:24` „Salary must be **pozitive**" → `positive`.
- **C5 — `root`/`root`** în `application.yaml:4-5` → `${DB_USER}` / `${DB_PASSWORD}`.
- **C6 — `@Order(1)`** pe singurul `CommandLineRunner` nu ordonează nimic.
- **C7 — fișiere fără newline la final** (`EmployeeCreateRequest.java`, `application.yaml`) — diff-urile ies murdare.
- **C8 — `pom.xml`** cu `<name/>`, `<licenses><license/></licenses>` etc. goale.

---

## Before / After

| # | Acum | Corect |
|---|---|---|
| **B1** | `int updatedRows = departmentRepository`<br>&nbsp;&nbsp;`.updateBudgetByName(department.getName(), req.budget());`<br><br>`@Query("update Department d set d.budget = :newBudget`<br>&nbsp;&nbsp;`where d.name = :name")` | `departmentRepository.updateBudgetById(id, req.budget());`<br><br>`@Query("update Department d set d.budget = :newBudget`<br>&nbsp;&nbsp;`where d.id = :id")` |
| **B2** | *(nu există)* | `@RestControllerAdvice`<br>`class GlobalExceptionHandler {`<br>&nbsp;&nbsp;`@ExceptionHandler(DepartmentIdNotFound.class)`<br>&nbsp;&nbsp;`@ResponseStatus(HttpStatus.NOT_FOUND)`<br>&nbsp;&nbsp;`ApiError handle(DepartmentIdNotFound e) { ... }`<br>`}`<br>+ handlere pentru `MethodArgumentNotValidException` (400), `ConstraintViolationException` (400), `DataIntegrityViolationException` (409) |
| **B3** | `findById(id).orElseThrow();` | `findById(id).orElseThrow(DepartmentIdNotFound::new);` |
| **B4** | `@OneToMany(`<br>&nbsp;&nbsp;`mappedBy = "department",`<br>&nbsp;&nbsp;`cascade = CascadeType.ALL,`<br>&nbsp;&nbsp;`orphanRemoval = true,`<br>&nbsp;&nbsp;`fetch = FetchType.LAZY)` | `@OneToMany(`<br>&nbsp;&nbsp;`mappedBy = "department",`<br>&nbsp;&nbsp;`cascade = {CascadeType.PERSIST, CascadeType.MERGE},`<br>&nbsp;&nbsp;`fetch = FetchType.LAZY)`<br><br>*(și în `deleteDepartment`, dezlegi angajații înainte de `delete`)* |
| **B5** | *(niciun endpoint de creare)* | `@PostMapping("/{departmentId}/employees")`<br>`@ResponseStatus(HttpStatus.CREATED)`<br>`EmployeeResponse addEmployee(`<br>&nbsp;&nbsp;`@PathVariable Long departmentId,`<br>&nbsp;&nbsp;`@Valid @RequestBody EmployeeCreateRequest request)` |
| **B6** | `if(employeeRepository.findAll().isEmpty()) {`<br>&nbsp;&nbsp;`throw new NoEmployeesFound();`<br>`}`<br>`return employeeRepository.findAll()`<br>&nbsp;&nbsp;`.stream().map(EmployeeResponse::from).toList();` | `return employeeRepository.findAll()`<br>&nbsp;&nbsp;`.stream().map(EmployeeResponse::from).toList();` |

---

## Ordinea de atac

1. **B1** — e singurul care strică date deja salvate. `where d.id = :id`, două minute.
2. **B6** — șterge `if/throw` și dubla chemare, apoi rulează cele două `grep`-uri de mai sus pe tot proiectul.
3. **B2** — advice-ul global. După el, B3 devine vizibil ca 404, nu ca 500.
4. **B3** — `orElseThrow(DepartmentIdNotFound::new)` în ambele locuri, plus M3 în același timp.
5. **B5** — endpoint-ul de creare angajat; abia atunci API-ul e complet.
6. **B4** — decizia despre cascade; discut-o cu mine dacă nu ești sigur ce vrei să însemne „șterg un departament".
7. Apoi 🟡-urile, în ordinea din listă.

---

## Q&A — verifică-ți înțelegerea

1. La `PATCH /api/department/4/budget` răspunsul a fost `"updatedRow": 2`. Ai fi putut prinde bug-ul B1 **fără** să te uit în cod, doar citind răspunsul acela. Ce anume din el e imposibil pentru un `PATCH` corect pe o singură resursă, și de ce?

2. Ai pus `clearAutomatically = true` pe `@Modifying` — bine. Descrie ce s-ar fi întâmplat fără el, concret: după `updateBudgetByName`, în aceeași tranzacție, ce ar fi returnat `department.getBudget()` și de ce Hibernate n-are cum să afle singur că valoarea din memorie s-a învechit?

3. `GET /api/employees` pe o bază fără angajați dă 500. Ai reparat exact aceeași greșeală în `DepartmentQueryServiceImpl` **în aceeași rundă**. Ce anume din felul în care ai lucrat a făcut ca reparația să nu se transfere, și ce verificare de zece secunde ar fi prins-o înainte de commit?

---

**Următorul pas:** repară în ordinea de mai sus, commit (cu `mvn -DskipTests compile` înainte), și scrie-mi „next".

---

## Runda 2 — 2026-09-02 — sha `e8f05ad`

- B1 🔴 Nu compilează: `EmployeeCommandService.java:8` declara `interface EmployeeComandService` — fișier redenumit, tip nu. Cele 19 `cannot find symbol: getId()` care urmau erau colaterale Lombok. → ✅ **ÎNCHIS**
- B2 🔴 Commit-ul `e8f05ad` avea mesaj „changed creation process for Employee…" și conținea doar rename-ul (`0 insertions, 0 deletions`). → ✅ munca reală a venit în `2e16475` + `edebe10`
- B3–B7 🔴 restanțele din runda 1. → 3 închise în runda 3 (`@Valid`, listă goală, `jobTitle`), 2 încă deschise (advice, endpoint employee — vezi B2 și B5 runda 3)
- M1 🟡 cele 4 DTO-uri noi erau goale, 2 din 4 `class` în loc de `record`. → ✅ **ÎNCHIS** — toate patru sunt acum `record`-uri completate, cu validare

## Runda 1 — 2026-09-02 — sha `943d7ad`

- B1 🔴 `@Valid` lipsă pe `List<EmployeeCreateRequest>` → ✅ ÎNCHIS prin eliminarea listei · B2 🔴 zero `@RestControllerAdvice` → 🔴 **DESCHIS, runda 3** · B3 🔴 listă goală = eroare → ✅ ÎNCHIS la departamente, 🔴 **REAPĂRUT la angajați** · B4 🔴 fără `EmployeeController` → 🔴 **PARȚIAL: GET există, POST nu** · B5 🔴 `jobTitle` 50 vs 20 → ✅ ÎNCHIS
- M1 postman rupt → ✅ aproape (o cerere pe `/api/company`) · M2 dialect hardcodat → ✅ ÎNCHIS · M3 dublă interogare → ✅ la departamente, 🔴 REAPĂRUT la angajați · M4 `mvnw` fără bit exec → 🔴 deschis · M5 POST 200 în loc de 201 → 🔴 deschis · M6 fără `@Transactional` → ✅ ÎNCHIS · M7 `orElse(null)` + `if/else` → 🔴 deschis (`EmployeeCommandServiceImpl.java:32,43-46`) · M8 nume departament neunic → 🔴 deschis, e cauza lui B1 runda 3 · M9 naming → ✅ parțial (`EmployeeCommandService`); rămân pachetul `CompanyManagement`, `repository` singular, `services/interfaces/` · M10 `/api/department` singular → 🔴 deschis
- 🟢 C1–C7 → majoritatea deschise; vezi 🟢 runda 3

**✅ Ce e bine — nu strica astea la refactor**
`equals`/`hashCode` pe entități scrise **corect** pentru JPA (`hashCode()` constant pe clasă, `equals` pe `id` cu gardă) · constructor `protected` + constructor de business, fără `@Setter` pe `id` · helper-ele bidirecționale `addEmployee`/`removeEmployee` · `left join fetch` dă **un singur SELECT**, fără N+1 și fără duplicate (Hibernate 6+ deduplică singur) · separarea Command/Query · `@Modifying(clearAutomatically, flushAutomatically)` — vezi runda 3.

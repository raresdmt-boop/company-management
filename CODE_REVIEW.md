# Code review — company-management

Proiect Spring Boot 4.1.1 / Java 21, temă JPA 1—* (`Department` 1—* `Employee`) expusă prin REST.

---

## Runda 1 — 2026-09-02 — sha `943d7ad`

**Poarta de compilare:** ✅ `mvn -DskipTests compile` trece.
**Verificare prin rulare:** aplicația a fost pornită pe H2 in-memory (copie în scratchpad, repo-ul tău neatins) și fiecare constatare 🔴 de mai jos a fost **reprodusă cu cerere HTTP reală**, nu dedusă din citit. Sub fiecare găsești cererea și răspunsul exact.

### Ce e bine — nu strica astea la refactor

- `equals`/`hashCode` pe entități sunt scrise **corect** pentru JPA: `hashCode()` constant pe clasă, `equals` pe `id` cu gardă `id != null`. Ăsta e exact tiparul care nu se strică atunci când o entitate intră într-un `HashSet` înainte de flush. Puțini îl nimeresc din prima.
- Constructor `protected` fără argumente + constructor de business, fără `@Setter` pe `id` — entitatea nu poate fi construită într-o stare invalidă din afară.
- Helper-ele bidirecționale `addEmployee`/`removeEmployee` (`Department.java:60-68`) — corecte, setează ambele capete.
- `left join fetch` în `DepartmentRepository.java:12-16`: `GET /api/department` face **un singur SELECT** pentru toate departamentele cu tot cu angajați. Fără N+1 și **fără duplicate** — Hibernate 6+ deduplică singur rezultatul unui `join fetch`, deci `distinct` nu-ți mai trebuie.
- Separarea Command / Query pe servicii.
- Tranzacția face rollback corect: la proba B1 de mai jos departamentul **nu** a rămas salvat pe jumătate (`GET` de după arată tot `[IT, HR, Finance]`).

---

## 🔴 Critice

### B1 — `@Valid` nu se propagă în listă: angajații din payload nu sunt validați deloc

`department/dtos/DepartmentCreateRequest.java:21-22`

```java
@Nullable
List<EmployeeCreateRequest> employees
```

`@Valid` din controller validează **doar primul nivel**. Ca să coboare în elementele unei colecții, adnotarea trebuie pusă **pe colecție**. Așa cum e acum, toate regulile din `EmployeeCreateRequest` (`@Email`, `@Positive`, `@Size`) sunt ignorate la POST.

Reprodus:

```
POST /api/department
{"name":"Legal","location":"Cluj","budget":1000,
 "employees":[{"firstName":"","lastName":"","email":"nu-e-email",
               "salary":-500,"jobTitle":"x","accessCode":"ab"}]}

HTTP 500
ConstraintViolationException: Validation failed for classes [...Employee] during persist time
  Email must be a valid address / Salary must be positive / Access code must have exactly 6 characters / ...
```

Observă unde crapă: **„during persist time"**. Regulile au prins, dar abia cele de pe *entitate*, în Hibernate, după ce cererea a trecut deja de controller. Rezultat: clientul primește 500 fără niciun mesaj, în loc de 400 cu lista câmpurilor greșite.

**Aceeași regulă, alte locuri de verificat când o repari:** deocamdată `employees` e singura colecție imbricată din proiect. În clipa în care mai apare una (ex. un `DepartmentUpdateRequest`), se aplică identic. Regula de reținut: **orice `List<AltDto>` dintr-un request DTO vrea `@Valid` pe ea**.

### B2 — Nu există `@RestControllerAdvice`: orice excepție de business iese ca 500

Ai scris două excepții cu mesaj (`NoDepartmentFound`, `DepartmentIdNotFound`) și... nimeni nu le traduce în cod HTTP. Sunt `RuntimeException` simple, fără `@ResponseStatus`, și nu există niciun handler global.

Reprodus, trei cauze diferite, **același 500 gol**:

```
GET  /api/department        (DB goală)          -> 500   [NoDepartmentFound]
POST /api/department        accessCode duplicat -> 500   [DataIntegrityViolationException:
                                                          Unique index ... EMPLOYEE(ACCESS_CODE) '123456']
POST /api/department        angajat invalid     -> 500   [ConstraintViolationException, vezi B1]
```

Un client al API-ului tău nu poate distinge „nu există" de „ai trimis prostii" de „serverul e picat". Îți trebuie un `@RestControllerAdvice` care mapează cel puțin: `NoDepartmentFound`/`DepartmentIdNotFound` → 404, `MethodArgumentNotValidException` → 400 cu lista câmpurilor, `ConstraintViolationException` → 400, `DataIntegrityViolationException` → 409.

Notă: e **exact** constatarea rămasă deschisă și la `academy-hub-api`. Aceeași regulă, alt repo.

### B3 — Lista goală nu e o eroare

`department/services/DepartmentQueryServiceImpl.java:25-26`

```java
if(departmentRepository.getDepartmentsWithEmployees().isEmpty())
    throw new NoDepartmentFound();
```

Reprodus pe o instanță pornită fără seeder:

```
GET /api/department  ->  HTTP 500
```

Semantica REST: **404 înseamnă „resursa asta nu există"**, nu „colecția e goală". `GET /api/department` pe o bază fără departamente are un răspuns perfect valid: `200 []`. Aruncă `NoDepartmentFound` doar când ceri **un** departament după id și nu-l găsești.

### B4 — Jumătatea „employee" a temei nu e accesibilă: nu există `EmployeeController`

`EmployeeComandService` + `EmployeeCommandServiceImpl.createEmployeeWithDepartment(...)` sunt scrise, injectate, compilate — și **cod mort**. Niciun controller nu le cheamă.

```
POST /api/employee -> HTTP 404 (nu există mapping)
```

Ai probat serviciul direct, în afara HTTP-ului, și logica lui răspunde:

```
PROBE-FAIL B  departmentId=999 inexistent -> DepartmentIdNotFound: Department id not found   (corect ca intenție)
PROBE-FAIL C  departmentId=null           -> InvalidDataAccessApiUsageException: The given id must not be null
PROBE-FAIL D  payload invalid             -> ConstraintViolationException: ... (vezi M7)
```

Deci serviciul funcționează; îi lipsește ușa. `POST /api/departments/{id}/employees` e mapping-ul natural.

### B5 — `jobTitle`: DTO-ul acceptă 50 de caractere, coloana are 20

`employee/dtos/EmployeeCreateRequest.java:29` spune `@Size(min = 2, max = 50)`.
`employee/models/Employee.java:51` spune `@Column(name = "job_title", length = 20)`.

O cerere **perfect validă după contractul tău public** moare în baza de date:

```
POST /api/department  ... "jobTitle":"Senior Principal Staff Engineer"  (31 caractere)

HTTP 500
JdbcSQLDataException: Value too long for column "JOB_TITLE CHARACTER VARYING(20)"
```

**Aceeași regulă, verifică toate perechile DTO ↔ coloană:**

| Câmp | `@Size` în DTO | `length` în entitate | |
|---|---|---|---|
| `Department.name` | 50 | 50 | ✅ |
| `Department.location` | 20 | 20 | ✅ |
| `Employee.email` | 50 | 50 | ✅ dar entitatea n-are `@Size`, doar `length` |
| `Employee.accessCode` | 6..6 | 6 | ✅ |
| `Employee.jobTitle` | **2..50** | **20** | 🔴 |

Pe `email` s-a văzut de ce contează și a doua coloană a tabelului: pentru că `@Valid` nu coboară (B1), un email de 58 de caractere a ajuns până la INSERT și a dat tot `Value too long for column "EMAIL CHARACTER VARYING(50)"` — 500, nu 400. Regula: **fiecare `@Column(length = N)` vrea un `@Size(max = N)` lângă el, pe entitate, și același N în DTO.**

---

## 🟡 Importante

**M1 — colecția Postman comisă e ruptă.** `postman/company-management-api.postman_collection.json` cere `GET http://127.0.0.1:8099/api/company` — path-ul pe care **tu l-ai șters** în commit-ul `19753fd` când ai redenumit `CompanyController` → `DepartmentController`. Verificat: `404`. Iar a doua cerere, „POST create department", are `"method": "GET"` și URL gol. Un fișier de livrat care nu rulează e mai rău decât lipsa lui.

**M2 — `hibernate.dialect` hardcodat, a treia oară.** `application.yaml:14`. Ți-am spus regula pe `rares-unu-la-multi` și e în notele tale: dialectul hardcodat pe MySQL **rupe testele `@DataJpaTest` pe H2**. Hibernate îl detectează singur din conexiune (în rularea mea pe H2, log-ul zice `Database dialect: H2Dialect` — fără ca eu să configurez nimic). Scoate linia.

**M3 — un GET = două SELECT-uri identice.** `DepartmentQueryServiceImpl.java:25` și `:28` cheamă aceeași metodă de repository de două ori: o dată ca să testeze `isEmpty()`, o dată ca să mapeze. Măsurat cu `show-sql`, un singur `GET /api/department` emite **2 `select`**. Pune rezultatul într-o variabilă. (După ce repari B3, verificarea de `isEmpty` dispare oricum.)

**M4 — `mvnw` comis fără bit de execuție.** `git ls-files -s mvnw` → mod `100644`, nu `100755`. Pe macOS/Linux `./mvnw` dă `permission denied`. Ai `.gitattributes` care fixează line ending-urile — bun reflex — dar bitul de execuție e separat: `git update-index --chmod=+x mvnw`.

**M5 — POST răspunde `200 OK`.** `DepartmentController.java:35`. Crearea unei resurse noi e `201 Created` + header `Location: /api/departments/{id}`.

**M6 — niciun serviciu nu e `@Transactional`.** Merge acum pentru că `save()` își deschide singur tranzacția, dar `EmployeeCommandServiceImpl` face deja **două** operații de repository (`findById` + `save`) — adică două tranzacții separate. În clipa în care între ele apare o a treia operație, nu mai ai atomicitate. Pune `@Transactional` pe metodele de comandă și `@Transactional(readOnly = true)` pe cele de query. (Ai importat `jakarta.transaction.Transactional` în `DepartmentQueryServiceImpl.java:3` și nu l-ai folosit — intenția era acolo. Folosește varianta Spring: `org.springframework.transaction.annotation.Transactional`.)

**M7 — `orElse(null)` urmat de `if (x != null) ... else throw`.** `EmployeeCommandServiceImpl.java:30, 41-44`. Scoți `Optional`-ul din cutie doar ca să reconstruiești manual exact ce face `orElseThrow`:

```java
Department department = departmentRepository.findById(departmentId)
        .orElseThrow(DepartmentIdNotFound::new);
department.addEmployee(employee);
```

Bonus: și cu `departmentId = null` cererea crapă urât (`InvalidDataAccessApiUsageException`) — cu `orElseThrow` tot crapă, dar la nivel de controller `@PathVariable Long id` nu poate fi null, deci problema dispare de la sine odată ce apare controller-ul din B4.

**M8 — numele de departament nu e unic.** Reprodus: două POST-uri identice cu `"name":"Legal"` → două rânduri, `id` 6 și 7. Dacă regula de business e „un departament pe nume", îți trebuie `@UniqueConstraint` pe `department(name)` — l-ai pus corect pe `employee(email)`, deci știi cum. Dacă nu e regulă, ignoră punctul.

**M9 — naming și structură de pachete.**
- `EmployeeComandService` — lipsește un `m`: **Comand**Service. Redenumește acum, cât e într-un singur loc.
- Pachetul `unu_la_multi.company_management.CompanyManagement.department...` are un segment cu majusculă în mijloc, iar `DataSeeder` + clasa de `main` stau cu un nivel mai sus, în afara lui. Convenția Java: totul lowercase, un singur pachet rădăcină.
- Plural inconsistent: `models`, `dtos`, `controllers`, `services` la plural, dar `repository` la singular.
- `services/interfaces/` — același comentariu ca la `academy-hub-api`: convenția uzuală e `services/` (interfețele) + `services/impl/`.

**M10 — `/api/department` la singular.** Un endpoint care întoarce o listă se numește `/api/departments`. Consecvent: `/api/departments/{id}/employees`.

---

## 🟢 Cleanups

- **C1 — importuri nefolosite:** `DepartmentRepository.java:5` (`DepartmentResponse` — un repository nu trebuie să știe de DTO-uri, ăsta e semnalul cel mai util din listă), `DepartmentQueryServiceImpl.java:3`, `EmployeeCreateRequest.java:4` (`jakarta.persistence.Column`) și `:6` (`org.hibernate.engine.spi.ManagedEntity` — API intern de Hibernate, n-are ce căuta într-un DTO), `EmployeeComandService.java:6`, plus `org.springframework.stereotype.Service` importat degeaba în ambele interfețe de serviciu.
- **C2 — mapare duplicată.** Ai scris `DepartmentResponse.from(...)` și `EmployeeResponse.from(...)`, apoi în `DepartmentCommandServiceImpl.java:48-54` și `EmployeeCommandServiceImpl.java:48-52` reconstruiești răspunsul de mână. Cheamă factory-ul.
- **C3 — `jakarta.annotation.Nullable`** pe `Department.java:38` și `DepartmentCreateRequest.java:21` nu face nimic la runtime (nu e adnotare de validare). Un câmp fără `@NotNull` e deja opțional.
- **C4 — typo în mesaj:** `EmployeeCreateRequest.java:26` „Salary must be **pozitive**" → `positive`. Mesajele de eroare rămân în engleză, dar corectă.
- **C5 — `root`/`root` în `application.yaml:4-5`.** Într-un repo public. Mută pe variabile de mediu: `${DB_USER}` / `${DB_PASSWORD}`.
- **C6 — `@Order(1)`** pe singurul `CommandLineRunner` din aplicație nu ordonează nimic.
- **C7 — `pom.xml`** are `<name/>`, `<description/>`, `<licenses><license/></licenses>`, `<developers>`, `<scm>` goale, generate de Spring Initializr. Completează-le sau șterge-le.

---

## Before / After (constatările critice)

| # | Acum | Corect |
|---|---|---|
| **B1** | `@Nullable`<br>`List<EmployeeCreateRequest> employees` | `@Valid`<br>`List<EmployeeCreateRequest> employees` |
| **B2** | *(nu există)* | `@RestControllerAdvice`<br>`class GlobalExceptionHandler {`<br>&nbsp;&nbsp;`@ExceptionHandler(NoDepartmentFound.class)`<br>&nbsp;&nbsp;`@ResponseStatus(HttpStatus.NOT_FOUND)`<br>&nbsp;&nbsp;`ApiError handle(NoDepartmentFound e) { ... }`<br>`}`<br>+ handlere pentru `MethodArgumentNotValidException` (400), `ConstraintViolationException` (400), `DataIntegrityViolationException` (409) |
| **B3** | `if(repo.getDepartmentsWithEmployees().isEmpty())`<br>&nbsp;&nbsp;&nbsp;&nbsp;`throw new NoDepartmentFound();`<br>`return repo.getDepartmentsWithEmployees()`<br>&nbsp;&nbsp;&nbsp;&nbsp;`.stream().map(DepartmentResponse::from).toList();` | `return repo.getDepartmentsWithEmployees()`<br>&nbsp;&nbsp;&nbsp;&nbsp;`.stream().map(DepartmentResponse::from).toList();` |
| **B4** | *(niciun controller pentru employee)* | `@PostMapping("/{departmentId}/employees")`<br>`ResponseEntity<EmployeeResponse> addEmployee(`<br>&nbsp;&nbsp;&nbsp;&nbsp;`@PathVariable Long departmentId,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`@Valid @RequestBody EmployeeCreateRequest request)` |
| **B5** | DTO: `@Size(min = 2, max = 50) String jobTitle`<br>Entitate: `@Column(name = "job_title", length = 20)` | DTO: `@Size(min = 2, max = 50) String jobTitle`<br>Entitate: `@Size(max = 50)`<br>&nbsp;&nbsp;&nbsp;&nbsp;`@Column(name = "job_title", length = 50)` |

---

## Ordinea în care le-aș repara

1. **B2** (advice global) — după el, toate celelalte erori încep să spună ce s-a întâmplat, deci le repari cu feedback, nu pe orbește.
2. **B1** + **B5** — validarea intrării, ca 400-urile să apară înainte de baza de date.
3. **B3** — lista goală = `200 []`.
4. **B4** — controller-ul de employee; abia atunci tema e completă.
5. M1–M4, apoi restul.

---

## Q&A — verifică-ți înțelegerea

1. La proba B1, mesajele de validare **au apărut** („Email must be a valid address"), dar ca 500, iar în stack trace scria `during persist time`. De unde au venit ele, dacă `EmployeeCreateRequest` n-a fost validat deloc? Și de ce e o problemă că vin de acolo, și nu din controller?

2. `GET /api/department` întoarce toate departamentele cu angajații lor și emite **un singur** SELECT către bază. Ce anume din `DepartmentRepository` face asta și ce s-ar fi întâmplat (câte interogări) dacă ștergeai `left join fetch` și lăsai doar `findAll()`?

3. `DepartmentCommandServiceImpl.createDepartment` salvează **un singur** obiect — `departmentRepository.save(department)` — și totuși în baza de date apar și rândurile din `employee`, cu `department_id` completat. Ce două lucruri din `Department.java` fac împreună să se întâmple asta? (Indiciu: unul e pe adnotarea `@OneToMany`, celălalt e o linie dintr-o metodă.)

---

**Următorul pas:** repară în ordinea de mai sus, commit, și scrie-mi „next" — runda 2 verifică prin rulare fix aceleași cereri de mai sus și le marchează ✅ sau 🔴 NEATINS.

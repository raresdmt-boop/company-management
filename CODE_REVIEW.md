# Code review — company-management

Proiect Spring Boot 4.1.1 / Java 21, temă JPA 1—* (`Department` 1—* `Employee`) expusă prin REST.

---

## Regulă de reținut — `join fetch` sau `@EntityGraph`?

O să dai peste alegerea asta la aproape fiecare endpoint nou din `IDEI-ENDPOINTURI.pdf`, așa că ține regula scurtă:

> **Scrii tu JPQL-ul → `join fetch`. Nu-l scrii tu, sau returnezi `Page` → `@EntityGraph(attributePaths = …)`.**

Amândouă rezolvă aceeași problemă (colecție `LAZY` + N+1). Diferența e **unde stă decizia de încărcare**: `join fetch` o lipește de interogare, `@EntityGraph` o ține separat și o atașează peste o interogare pe care nu tu o scrii.

**De ce contează la paginare.** Rulat pe cele 4 departamente ale tale, aceeași paginare în ambele feluri:

| | `content` | `totalElements` | `totalPages` |
|---|---|---|---|
| `@Query("… left join fetch …")` + `Page` | 2 | **8** | **4** |
| `@EntityGraph(attributePaths = "employees")` + `Page` | 2 | 4 | 2 |

Sunt patru departamente. Prima variantă raportează opt. Când returnezi `Page`, Spring Data derivă interogarea de `count` din JPQL-ul tău — **cu tot cu join** — deci numără rânduri de join, nu departamente: 3 + 2 + 2 + 1 = 8. Cu `@EntityGraph`, interogarea de bază n-are join în ea (îl adaugă graful, separat), deci count-ul iese curat.

Cel mai urât e că nu crapă nimic: primești 200, primești datele corecte pe pagină, și doar `totalPages` minte. Frontend-ul afișează patru pagini, din care două goale.

**Când rămâi pe `join fetch`:** când scrii oricum JPQL **și** vrei să filtrezi colecția încărcată — `left join fetch d.employees e where e.salary > :prag`. Un `@EntityGraph` nu poate face asta; el spune „adu colecția", nu „adu bucata asta din colecție". În `getDepartmentsWithEmployees()` `join fetch` e alegerea corectă, nu-l schimba.

**Două capcane, verificate prin rulare:**

- `@EntityGraph(attributePaths = "typooo")` → crapă zgomotos: `IllegalArgumentException: Unable to locate Attribute with the given name`. Bine.
- `@EntityGraph(value = "NumeDeGrafCareNuExistă")` → **ignorat în tăcere**. Aplicația pornește, interogarea rulează, N+1-ul rămâne. Ai impresia că l-ai rezolvat.

De aici: folosește **`attributePaths`**, nu grafuri numite. Prima formă e verificată la pornire, a doua nu.

---

## Runda 5 — 2026-09-03 — sha `e642a5f`

**Poarta de compilare:** ✅ trece.
**Verificat prin rulare:** pornit pe H2 cu `show-sql`, ca să pot **număra interogările**, nu doar să citesc răspunsurile. 13 probe HTTP.

Două commit-uri: `3b08cf2` (reparații din runda 4) și `e642a5f` (primele două endpoint-uri din `IDEI-ENDPOINTURI.pdf`).

### ✅ Ce ai închis

| Din | Constatare | Verificat |
|---|---|---|
| R4 B1 | `@NotBlank` pe `Double budget` omora `PUT /api/department/{id}` | `@NotNull`, endpoint-ul răspunde |
| R4 B2 | `PATCH .../salary` returna salariul vechi | returnezi `request.salary()` |
| R4 B3 | `PATCH /{id}/{newSalary}` accepta salariu negativ | mapping-ul e șters |
| **R4 B6** | `orElseThrow()` gol în 4 din 7 locuri | **`grep -rn "orElseThrow()" src/main/java` → zero rezultate** |
| R4 M3 | `existsById` + `findById` = două interogări | o singură chemare, cu `orElseThrow(DepartmentIdNotFound::new)` |
| PDF 1–2 | lipseau `GET /api/department/{id}` și `GET /api/employees/{id}` | scrise, răspund corect (dar vezi B2 — criteriul de acceptare nu e atins) |

**B6 merită scos în evidență.** Trei runde la rând ți-am semnalat tiparul „repar linia semnalată, las celelalte apariții". De data asta ai rulat `grep`-ul și ai reparat toate cele șapte locuri dintr-o mișcare, inclusiv patru pe care nu ți le arătasem individual. Ăsta e exact reflexul care trebuia să apară. Păstrează-l.

---

## 🔴 Critice

### B1 — `getAllEmployeeCounts()` e un N+1 scris de mână

`department/services/DepartmentQueryServiceImpl.java:56-64`

```java
List<Department> departments = departmentRepository.findAll();
List<DepartmentEmployeeCount> counts = new ArrayList<>();
for(Department d : departments){
    counts.add(getEmployeeCount(d.getId()));
}
```

**CUM E ACUM.** Măsurat cu `show-sql`, pe patru departamente:

```
GET /api/department/employee-count   ->  200, rezultat CORECT
                                     ->  6 SELECT-uri
```

Rezultatul e bun (`Legal` apare cu 0, deci `left join` + `count(e.id)` sunt corecte). Numărul de interogări nu e.

**DE CE.** Ăsta e N+1-ul, în forma lui cea mai curată: **1** interogare ca să afli *care* sunt departamentele, apoi **N** interogări, câte una pentru fiecare, ca să afli câte ceva despre fiecare. Cu 4 departamente sunt 5–6 drumuri la bază. Cu 400 de departamente sunt peste 400 — și fiecare drum e un round-trip pe rețea, nu un apel de metodă.

Partea perfidă: **nu se vede la dezvoltare**. Cu datele din seeder endpoint-ul răspunde instant, testul din Postman e verde, totul pare în regulă. Se vede abia în producție, când tabelul crește, și atunci arată ca „aplicația a devenit lentă", nu ca „bucla asta face 400 de interogări".

Ai deja unealta care rezolvă asta într-un singur SELECT — `group by`. Cu ea, baza de date face gruparea o dată, pentru toate departamentele deodată, în loc să răspundă la aceeași întrebare de N ori.

**FIX** — în `DepartmentRepository`:

```java
@Query("""
        select d.name as name, count(e.id) as employeeCount
        from Department d
        left join d.employees e
        group by d.id, d.name
        order by d.name
        """)
List<DepartmentEmployeeCount> countEmployeesPerDepartment();
```

…iar serviciul devine o singură linie. `left join` (nu `inner`) ca departamentele goale să rămână în listă, și `count(e.id)` (nu `count(*)`) ca ele să dea 0, nu 1.

**Verificarea, după ce repari:** pornește cu `spring.jpa.show-sql=true`, dă un `GET`, și numără liniile `Hibernate:` din consolă. Trebuie să fie **una**. Fă-ți obiceiul ăsta la fiecare endpoint care întoarce o listă — e singurul mod în care vezi un N+1 înainte să-l vadă clientul.

### B2 — Ai declarat un `@NamedEntityGraph` și nu-l folosește nimeni

`department/models/Department.java:16-19`

```java
@NamedEntityGraph(
        name = "Department.withEmployees",
        attributeNodes = @NamedAttributeNode("employees")
)
```

`DepartmentRepository.java:3` importă și `org.springframework.data.jpa.repository.EntityGraph`. Niciunul dintre ele nu e folosit nicăieri — `grep -rn "EntityGraph" src/` întoarce exact aceste două linii.

**CUM E ACUM.** Locul unde graful ți-ar fi folosit e chiar în cele două endpoint-uri pe care tocmai le-ai scris din PDF. Măsurat, cu SQL-ul din consolă:

```
GET /api/department/2   ->  2 SELECT-uri
   select ... from department d1_0 where d1_0.id=?          <- findById
   select ... from employee e1_0 where e1_0.department_id=? <- colectia LAZY, atinsa de DepartmentResponse.from

GET /api/employees/4    ->  3 SELECT-uri
   ...
   select ... from department d1_0 where d1_0.id=?          <- asocierea LAZY, atinsa de getDepartment().getName()
```

Criteriul de acceptare pe care ți l-am scris în PDF la endpoint-ul 1 era **„angajații vin fără interogare suplimentară"**. Nu e atins: vin, dar cu o interogare în plus. Nu e o greșeală de logică — răspunsul e corect — e fix golul pe care `@EntityGraph` îl umple.

Aceeași poveste la `getEmployee`: `EmployeeResponse.from` cheamă `getDepartment().getName()`, iar `department` e `LAZY`, deci încă un drum la bază.

**DE CE.** `@NamedEntityGraph` declară graful **pe entitate**; el nu face nimic până nu-l ceri explicit pe o metodă de repository, cu `@EntityGraph(value = "Department.withEmployees")`. Declarația singură e ca o rețetă scrisă și pusă în sertar.

Și aici e capcana pe care vreau să o eviți din start: forma cu **nume** e cea care **eșuează în tăcere**. Am verificat-o: dacă pui `@EntityGraph(value = "UnNumeGreșit")` pe o metodă, aplicația **pornește normal**, interogarea rulează, nu apare nicio eroare — doar că graful nu se aplică și rămâi cu N+1-ul, convins că l-ai rezolvat. Forma cu `attributePaths` e verificată la pornire: un typo dă `IllegalArgumentException: Unable to locate Attribute with the given name`.

**FIX:** șterge `@NamedEntityGraph` de pe entitate și pune graful direct pe metodele care au nevoie de el — în ambele repository-uri:

```java
@EntityGraph(attributePaths = "employees")
Optional<Department> findById(Long id);
```

```java
@EntityGraph(attributePaths = "department")
Optional<Employee> findById(Long id);
```

Da, poți adnota `findById` deși n-ai scris-o tu — vine din `JpaRepository`, o redeclari în interfața ta și îi pui graful deasupra. Ăsta e chiar motivul pentru care `@EntityGraph` există: pe metodele moștenite nu ai unde să scrii un `join fetch`. După modificare, ambele endpoint-uri trebuie să emită **o singură** linie `Hibernate:`.

Regula completă — când `join fetch`, când `@EntityGraph` — e în secțiunea *„Regulă de reținut"* de la runda 4, cu cifrele care arată de ce contează la paginare.

### B3 — Tot nu există `@RestControllerAdvice`. A cincea rundă.

```
GET /api/department/count-employees-per-department?departmentId=999   ->  500
GET /api/department/999                                               ->  500
GET /api/employees/999                                                ->  500
```

Ultimele două sunt endpoint-uri scrise **azi**, cu `orElseThrow(...NotFound::new)` corect în serviciu — excepția potrivită e aruncată, și tot 500 iese. Excepția corectă fără advice nu valorează nimic.

Un id inexistent e un 404, nu o defecțiune de server. Mecanismul e explicat la runda 3; efortul e o clasă și ~30 de rânduri, o singură dată. Fiecare rundă adaugă între timp endpoint-uri noi care moștenesc problema — cu cele patru din runda asta ai ajuns la nouă.

E cea mai veche constatare deschisă din proiect, din runda 1. **Fă-o pe asta înaintea oricărui endpoint nou.**

### B4 — Răspuns construit cu `Map<String, Object>`, cu typo în cheie

`department/controllers/DepartmentController.java:57-62`

```java
return ResponseEntity.ok(Map.of(
        "department", departmentQueryService.getDepartmentById(departmentId).name(),
        "emmployee count", departmentQueryService.countEmployeesByDepartmentId(departmentId)));
```

**CUM E ACUM.**

```json
{"department":"IT","emmployee count":3}
```

Două probleme într-o linie: cheia are un `m` în plus, și are un spațiu în ea — un client JavaScript nu poate scrie `response.emmployee count`, e obligat la `response["emmployee count"]`.

**DE CE contează mai mult decât typo-ul.** Un `Map` nu are contract. Compilatorul nu verifică nimic despre el: nu știe ce chei conține, ce tipuri au, dacă lipsește vreuna. Ai scris greșit un nume de câmp și **nimic** nu te-a oprit — nici compilarea, nici pornirea, nici testul din Postman, pentru că din exteriorul aplicației `emmployee count` arată la fel de valid ca orice. Cu un `record`, aceeași greșeală ar fi fost o eroare de compilare.

Ironia: ai deja tipul potrivit, l-ai scris chiar în runda asta — `DepartmentEmployeeCount(String name, Long employeeCount)`. Îl folosești la celălalt endpoint și nu la ăsta.

**FIX:** returnează `DepartmentEmployeeCount`. Ai deja și metoda care îl construiește, `getEmployeeCount(departmentId)` — controller-ul n-are de ce să cheme două servicii și să lipească rezultatele el însuși.

---

## 🟡 Importante

**M1 — adnotare de web într-o interfață de serviciu, a patra rundă.** `department/services/interfaces/DepartmentQueryService.java:3` importă `org.springframework.web.bind.annotation.RequestParam`. Ți-am semnalat exact asta pe `DepartmentCommandService` la runda 3 (M1) și runda 4 (M8); acum ai adăugat-o într-un fișier nou. Aceeași regulă, al treilea fișier. Rulează `grep -rn "org.springframework.web" src/main/java/**/services` — nimic din stratul de servicii n-are voie să știe de HTTP.

**M2 — numele celor două endpoint-uri sunt inversate față de ce fac.** `/count-employees-per-department` cere un `departmentId` și întoarce **un** departament; `/employee-count` le întoarce **pe toate**. Un cititor înțelege exact pe dos. Plus, ambele stau sub `/api/department` ca segmente literale, ceea ce le pune în competiție cu un viitor `GET /api/departments/{id}`. Formele naturale: `GET /api/departments/{id}/employee-count` și `GET /api/departments/employee-counts`.

**M3 — `getEmployeeCount` face două interogări pentru un rezultat** (`countEmployeesByDepartmentId` + `findById`). După ce repari B1 cu `group by`, metoda dispare oricum.

**M4 — a treia excepție, folosită a treia oară diferit.** `getDepartmentById` aruncă `NoDepartmentFound`, în timp ce tot restul aruncă `DepartmentIdNotFound`, iar `DepartmentNotFound` există și nu e folosită deloc. Când vei scrie advice-ul, va trebui să te uiți în trei locuri ca să știi ce mapezi. Păstrează una.

**M5 — cod mort nou:** `ListDepartmentEmployeeCount` (record scris în runda asta, nefolosit) · `changeSalaryThruUrl` rămasă în `EmployeeCommandServiceImpl.java:109` și în interfață după ce ai șters mapping-ul · `import java.math.BigDecimal` rămas în `EmployeeController`.

**M6 — `departmentName` repetat în fiecare angajat, în interiorul propriului departament.** `GET /api/department/1` întoarce departamentul „IT" cu trei angajați, fiecare purtând `"departmentName":"IT"`. Informația e deja în obiectul părinte. `EmployeeResponse` e folosit în două contexte diferite (listă plată de angajați, unde câmpul e util; listă imbricată sub departament, unde e zgomot) — dacă vrei să scapi de el, îți trebuie două DTO-uri, nu unul.

**M7–M12 — deschise din rundele anterioare:** `PUT` cu email duplicat răspunde 200 tăcut (R4 B4) · `DELETE` pe departament șterge angajații (R4 B7 / R3 B4) · `GET /api/employees` pe listă goală dă 500 (R3 B6) · POST răspunde 200 în loc de 201 · `/api/department` singular vs `/api/employees` plural · `mvnw` fără bit de execuție · nume de departament neunic.

---

## Ordinea de atac

1. **B3** — advice-ul global. Blochează totul de la runda 1 și face ca următoarele să fie verificabile.
2. **B1** — `group by` în loc de buclă; verifică apoi cu `show-sql` că numeri **o** linie `Hibernate:`.
3. **B2** — scoate `@NamedEntityGraph` de pe entitate, pune `@EntityGraph(attributePaths = "employees")` pe `findById`.
4. **B4** — `DepartmentEmployeeCount` în loc de `Map`.
5. **M1** — `grep` după `org.springframework.web` în servicii.
6. Restanțele: email duplicat → 409, cascade la delete, lista goală de angajați.

---

## Q&A — runda 5

1. `GET /api/department/employee-count` întoarce răspunsul corect și emite 6 interogări acolo unde una ar ajunge. De ce nu poți depista problema asta uitându-te la răspuns sau la timpul de execuție în dezvoltare, și care e singura verificare care ți-ar fi arătat-o înainte de commit?

2. Ai declarat `@NamedEntityGraph` pe `Department` și nu s-a întâmplat nimic. Ce ar mai fi trebuit scris ca graful să se activeze — și de ce îți recomand `attributePaths` în loc de forma cu nume, deși amândouă fac același lucru când sunt scrise corect?

3. Cheia `"emmployee count"` a trecut de compilare, de pornire și de testul din Postman. Ce anume din alegerea lui `Map<String, Object>` ca tip de răspuns a făcut ca niciuna dintre cele trei porți să n-o prindă?

---

**Următorul pas:** repară în ordinea de mai sus și scrie-mi „next". `IDEI-ENDPOINTURI.pdf` a fost actualizat în runda asta — criteriile de acceptare cer acum explicit numărul de interogări și, unde e cazul, `@EntityGraph`.

---

## Runda 4 — 2026-09-03 — sha `24e6f9c`

Verificată prin rulare pe H2, 11 probe HTTP. Stare la runda 5:

- B1 🔴 `@NotBlank` pe `Double budget` → `PUT /api/department/{id}` dădea 500 la orice cerere (`HV000030`) → ✅ **ÎNCHIS** (`@NotNull`)
- B2 🔴 `PATCH .../salary` returna salariul vechi (entitate detașată după `clearAutomatically`) → ✅ **ÎNCHIS** (returnezi `request.salary()`)
- B3 🔴 `PATCH /{id}/{newSalary}` accepta salariu negativ (path variable nevalidat + bulk update ocolește Bean Validation) → ✅ **ÎNCHIS** (mapping șters; metoda de serviciu a rămas, vezi M5 runda 5)
- B4 🔴 `PUT` cu email duplicat răspunde `200` și nu schimbă nimic, în loc de 409 → 🔴 **DESCHIS**
- B5 🔴 fără `@RestControllerAdvice` → 🔴 **DESCHIS, a 5-a rundă**
- B6 🔴 `orElseThrow()` gol în 4 din 7 locuri → ✅ **ÎNCHIS, toate 7** (vezi nota din runda 5)
- B7 🔴 `DELETE` pe departament șterge angajații → 🔴 **DESCHIS**
- 🟡 M1 `POST /api/employees/{departmentid}` — path-ul spune altceva decât face → deschis · M2 `changeSalary` vs `changeSalaryThruUrl` duplicate → ✅ parțial · M3 gărzi moarte în `updateEmployee` → deschis · M4 `EmployeeResponse.from` presupune că orice angajat are departament → deschis · M5 POST 200 în loc de 201 → deschis · M6 `updatedRows` expus → deschis · M7 `@Transactional` jakarta fără `readOnly` → deschis · M8 adnotări de web în interfețe de serviciu → 🔴 **agravat, al treilea fișier** (M1 runda 5) · M9 `/api/department` singular → deschis · M10 nume departament neunic + `mvnw` fără bit exec → deschise
- 🟢 C1–C8 → majoritatea deschise; C3 (typo „pozitive" migrat prin copy-paste) → deschis în `EmployeeUpdateRequest.java:23`

## Runda 3 — 2026-09-02 — sha `edebe10`

Verificată prin rulare pe H2, 14 probe HTTP. Stare la runda 4:

- B1 🔴 `changeBudget` făcea `update ... where d.name` — PATCH pe un id schimba bugetul altui departament omonim (`"updatedRow": 2`) → ✅ **ÎNCHIS** (`updateBudgetById`, `where d.id = :id`)
- B2 🔴 fără `@RestControllerAdvice`, 4 excepții → toate 500 → 🔴 **DESCHIS, a 4-a rundă** (acum 5 clase de excepție, zero handlere)
- B3 🔴 `orElseThrow()` gol → `NoSuchElementException` → 🔴 **ÎNCHIS 3 din 7 locuri** (vezi B6 runda 4)
- B4 🔴 `DELETE` pe departament șterge și angajații (cascade ALL + orphanRemoval) → 🔴 **DESCHIS** (vezi B7 runda 4)
- B5 🔴 nu se putea crea un angajat prin API → ✅ **ÎNCHIS** (`POST /api/employees/{departmentid}`; vezi M1 runda 4 pentru forma path-ului)
- B6 🔴 `EmployeeQueryServiceImpl` reproducea `isEmpty()+throw` și dubla `findAll()` reparate în aceeași rundă la departamente → 🔴 **DESCHIS** (`GET /api/employees` pe listă goală dă tot 500)
- 🟡 M1 adnotări de web în interfața de serviciu → deschis · M2 `PUT` cu semantică de `PATCH` → ✅ închis, dar cu `@NotBlank` pe `Double` (B1 runda 4) · M3 `existsById`+`findById` → deschis · M4 trei excepții pentru aceeași idee → deschis, acum șase · M5 `@Transactional` jakarta fără `readOnly` → deschis · M6 POST 200 în loc de 201 → deschis · M7 postman pe `/api/company` → ✅ **ÎNCHIS** · M8 `/api/department` singular → deschis · M9 `updatedRow` expus → deschis · M10 nume departament neunic → deschis · M11 `mvnw` fără bit exec → deschis
- ⭐ `@Modifying(clearAutomatically = true, flushAutomatically = true)` pus **singur**, fără să i se ceară — vezi B2 runda 4 pentru fața cealaltă a monedei.

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

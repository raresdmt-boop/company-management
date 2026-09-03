# Code review — company-management

Proiect Spring Boot 4.1.1 / Java 21, temă JPA 1—* (`Department` 1—* `Employee`) expusă prin REST.

---

## Runda 4 — 2026-09-03 — sha `24e6f9c`

**Poarta de compilare:** ✅ trece.
**Verificat prin rulare:** pornit pe H2, 11 probe HTTP. Fiecare 🔴 are cererea și răspunsul exact.

Ai construit CRUD-ul complet pe angajat — create, update, două variante de patch pe salariu, delete — plus DTO-uri și excepție proprie. Constatările de mai jos sunt aproape toate **în codul nou**, nu regresii în cel vechi.

### ✅ Ce ai închis

| Din | Constatare | Verificat |
|---|---|---|
| R3 B1 | `changeBudget` făcea `update ... where d.name` | acum `updateBudgetById(id, ...)`, `where d.id = :id` |
| R3 B5 | Nu se putea crea un angajat prin API | `POST /api/employees/{departmentid}` există și funcționează |
| R3 M7 | Postman cerea `/api/company` | scos; colecția acoperă acum tot CRUD-ul, 11 cereri |
| R3 🟢 C4 | typo „pozitive" | reparat în `EmployeeCreateRequest`… dar copiat în cel nou (vezi C3) |

---

## 🔴 Critice

### B1 — `PUT /api/department/{id}` e mort: `@NotBlank` pe un `Double`

`department/dtos/DepartmentUpdateRequest.java:14-16`

```java
@NotBlank(message = "Updating all rows is necessary for PUT mapping")
@PositiveOrZero(message = "Department budget cannot be negative")
Double budget
```

**CUM E ACUM.** *Orice* cerere, oricât de corectă:

```
PUT /api/department/1   {"name":"IT2","location":"Cluj","budget":5000.0}
-> 500

jakarta.validation.UnexpectedTypeException: HV000030: No validator could be found
for constraint 'jakarta.validation.constraints.NotBlank' validating type
'java.lang.Double'. Check configuration for 'budget'
```

**DE CE.** O adnotare de validare nu validează nimic singură — ea doar *numește* o regulă. Munca o face un `ConstraintValidator`, iar fiecare adnotare vine cu o listă fixă de tipuri pe care le știe verifica. `@NotBlank` are exact una: `CharSequence`. Înseamnă „nu e null, și după `trim()` mai rămâne ceva" — o întrebare care pur și simplu nu are sens pentru un număr.

Partea care te-a păcălit: potrivirea adnotare ↔ tip **nu se face la compilare**. Compilatorul vede o adnotare validă pusă pe un câmp, atât. Hibernate Validator caută validatorul abia când chiar validează, adică la prima cerere care intră pe endpoint. De-aia `mvn compile` trece verde și endpoint-ul e mort.

Ține minte împărțirea, e mică: `@NotNull` pentru orice tip (inclusiv numere și obiecte), `@NotEmpty` pentru texte și colecții (nu e null și are lungime > 0), `@NotBlank` **doar** pentru texte.

**FIX:** pe `budget` pui `@NotNull`. Pe `name` și `location`, `@NotBlank` e corect.

### B2 — `PATCH .../salary` returnează salariul **vechi**

`employee/services/EmployeeCommandServiceImpl.java:95-105`

**CUM E ACUM.** Trei cereri consecutive pe același angajat, care pornește de la 7500:

```
PATCH /api/employees/1/salary  {"salary":12345}  ->  {"id":1,"salary":7500.00, "updatedRows":1}
PATCH /api/employees/1/salary  {"salary":22222}  ->  {"id":1,"salary":12345.00,"updatedRows":1}
```

Baza de date e corectă — a doua cerere dovedește că prima chiar a scris 12345. **Răspunsul e cu un pas în urmă, mereu.**

**DE CE.** E fața cealaltă a monedei pe care ai nimerit-o singur în runda trecută, și de care te-am și lăudat. `@Modifying(clearAutomatically = true)` face exact ce promite: după update-ul bulk, golește persistence context-ul, ca nimeni să nu mai lucreze cu date învechite.

Numai că „nimeni" te include și pe tine. Variabila `employee` de la linia 96 e un obiect Java obișnuit, încărcat **înainte** de update. Golirea contextului nu-i schimbă câmpurile — nu are cum, e doar o referință în stiva ta. Ea îl *detașează*, adică rupe legătura prin care Hibernate ar mai fi putut să-l resincronizeze. Rezultatul: `employee.getSalary()` de la linia 102 citește fotografia de dinainte de update.

Observă că la buget nu ai pățit-o — dar din noroc, nu din construcție: acolo returnezi `changeBudgetRequest.budget()`, adică valoarea cerută, nu una citită din entitate.

**FIX**, două variante:
- simplu — returnează `request.salary()`, valoarea pe care tocmai ai scris-o (ca la buget);
- corect în general — dacă ai nevoie de starea reală din baza de date după un bulk update, o reciteşti: `employeeRepository.findById(id).orElseThrow(...)`.

### B3 — `PATCH /api/employees/{id}/{newSalary}` scrie orice în baza de date, inclusiv salarii negative

`employee/controllers/EmployeeController.java:54-60`

**CUM E ACUM.**

```
PATCH /api/employees/1/-5000     ->  200 OK
PATCH /api/employees/1/salary  {"salary":1}  ->  {"id":1,"salary":-5000.00,...}
```

A doua cerere e dovada: salariul citit din baza de date **este** −5000. A intrat și a rămas acolo.

**DE CE.** Două plase de siguranță, ocolite pe rând.

Prima: `@PathVariable BigDecimal newSalary` n-are nicio adnotare de validare. `ChangeSalaryRequest` are `@NotNull` și `@Positive` — dar endpoint-ul ăsta nu trece pe acolo, ia numărul direct din URL. Un parametru fără constrângere nu e validat, indiferent că `@Validated` stă pe clasa controller-ului.

A doua, cea interesantă: entitatea `Employee` **are** `@Positive` pe `salary` (`Employee.java:45`) — și totuși n-a oprit nimic. Motivul e că validarea entităților în Hibernate se agață de evenimentele ciclului de viață al obiectelor: `pre-insert` și `pre-update`, adică momentele în care Hibernate scrie în baza de date **o entitate din persistence context**. Un `update ... set` JPQL de tip bulk nu are entitate: nu încarcă niciun obiect, nu face dirty checking, nu emite niciun eveniment. Trimite un `UPDATE` direct la baza de date și numără rândurile.

De aici o regulă care merită ținută minte: **datele care intră printr-un bulk update sunt validate doar de tine și de constrângerile din schemă.** Bean Validation nu e acolo. Dacă vrei ca `salary > 0` să fie garantat indiferent pe unde intră datele, îl pui și ca `CHECK` în baza de date.

**FIX:** șterge endpoint-ul — duplică `PATCH /{id}/salary`, care are deja validarea corectă. Dacă vrei totuși să-l păstrezi, parametrul are nevoie de `@Positive` **și** de un `@Min` pe path, iar update-ul de un `CHECK (salary > 0)` în schemă.

*(Bonus: `@PatchMapping("/{id}/{newSalary}")` se suprapune cu `@PatchMapping("/{id}/salary")`. `PATCH /api/employees/5/salary` s-ar putea potrivi la amândouă; merge doar pentru că Spring preferă segmentul literal celui variabil. E o regulă de departajare pe care te bazezi fără să o fi ales.)*

### B4 — `PUT` pe angajat ignoră în tăcere emailul deja folosit și răspunde `200 OK`

`EmployeeCommandServiceImpl.java:68-70`

```java
if(request.email()!=null && !request.email().isBlank() && !employeeRepository.existsByEmail(request.email())){
    employee.setEmail(request.email());
}
```

**CUM E ACUM.** Angajatul 2 e Andrei (`andrei@gmail.com`). Cer să-i schimb emailul în cel al Mariei, care există deja:

```
PUT /api/employees/2  {..., "email":"maria@gmail.com", ...}
-> 200 OK
   {"id":2,"firstName":"Andrei",...,"email":"andrei@gmail.com",...}
```

Statusul zice „am făcut ce ai cerut". Emailul din răspuns e cel vechi. Clientul are 200 în mână și o modificare care nu s-a întâmplat.

**DE CE.** Ai transformat o **regulă de business** într-o **condiție de atribuire**. `if (email e liber) setează` are un `else` implicit pe care nu l-ai scris: *nu face nimic, și nu spune nimănui*. Din exterior, „am refuzat" și „am reușit" arată identic.

Un API poate răspunde la o cerere în trei feluri: o face, o refuză cu un motiv, sau crapă. Tăcerea nu e printre ele. Aici cazul corect e refuzul: emailul e unic prin constrângere de bază de date (`uk_employee_email`), deci un conflict e un rezultat legitim și numit — `409 Conflict`.

Mai e o consecință pe care n-ai văzut-o: `existsByEmail` e adevărat și pentru **propriul** email al angajatului. Un `PUT` care păstrează emailul neschimbat intră tot pe ramura tăcută. Nu strică nimic azi, dar arată că verificarea nu e „e liber?", ci „e liber pentru altcineva?".

**FIX:**

```java
employeeRepository.findByEmail(request.email())
        .filter(other -> !other.getId().equals(id))
        .ifPresent(other -> { throw new EmailAlreadyUsed(); });
employee.setEmail(request.email());
```

…și `EmailAlreadyUsed` → `409` din advice-ul de la B5.

### B5 — Tot nu există `@RestControllerAdvice`. A patra rundă.

**CUM E ACUM.**

```
POST /api/employees/999   (departament inexistent)  -> 500
PUT  /api/employees/999   (angajat inexistent)      -> 500
PUT  /api/department/1    (bug-ul B1)               -> 500
```

Primele două sunt „nu găsesc resursa" — un 404 curat. Al treilea e un bug real de configurare, deci un 500 onest. **Toate trei arată identic pentru client.** Asta e problema: fără advice, n-ai cum să deosebești o cerere greșită de o aplicație stricată, nici din afară, nici din monitorizare.

Mecanismul l-am descris în runda 3 (rezolverele implicite ale Spring știu să traducă doar excepțiile Spring; ale tale sunt `RuntimeException`-uri anonime pentru ele, deci cad până la ultima plasă, care răspunde 500).

E constatarea cea mai veche deschisă din proiect — din runda 1. Fiecare rundă a adăugat între timp excepții noi: acum ai **șase** clase de excepție (`DepartmentIdNotFound`, `DepartmentNotFound`, `NoDepartmentFound`, `EmployeeIdNotFound`, `NoEmployeesFound`, plus cea de la B4 pe care o vei scrie) și zero handlere. Cu cât amâni, cu atât advice-ul devine mai mare de scris — deși e același efort pe excepție.

**FIX:** o clasă, `@RestControllerAdvice`, cu un `@ExceptionHandler` pentru fiecare familie. 30 de rânduri, o dată.

### B6 — Ai reparat `orElseThrow()` în trei locuri din șapte

Ți-am semnalat în runda 3 că `orElseThrow()` fără argument aruncă `NoSuchElementException`. Ai reparat — în serviciul de angajați, la trei metode:

| Fișier:linie | Metodă | Stare |
|---|---|---|
| `EmployeeCommandServiceImpl.java:96` | `changeSalary` | ✅ `orElseThrow(EmployeeIdNotFound::new)` |
| `EmployeeCommandServiceImpl.java:109` | `changeSalaryThruUrl` | ✅ |
| `EmployeeCommandServiceImpl.java:122` | `deleteEmployee` | ✅ |
| `EmployeeCommandServiceImpl.java:34` | `createEmployeeWithDepartment` | 🔴 `orElseThrow()` gol |
| `EmployeeCommandServiceImpl.java:60` | `updateEmployee` | 🔴 gol |
| `DepartmentCommandServiceImpl.java:75` | `changeBudget` | 🔴 gol |
| `DepartmentCommandServiceImpl.java:87` | `deleteDepartment` | 🔴 gol |

Patru dintre ele sunt în **același fișier** cu cele trei reparate, două chiar deasupra lor.

**DE CE contează, dincolo de cele patru linii.** E a doua rundă la rând când repari apariția semnalată și lași restul. În runda 3 era `isEmpty() + throw`, reparat la departamente și rescris identic la angajați. Acum e `orElseThrow()`, reparat în jumătatea de jos a unui fișier și lăsat în jumătatea de sus.

Nu e neatenție — e ordinea în care lucrezi. Citești constatarea, te duci la `file:line`, repari acolo, treci mai departe. Pasul care lipsește e **unul singur**, și durează zece secunde:

```
grep -rn "orElseThrow()" src/main/java
```

Rulează-l acum. Îți dă exact cele patru linii de mai sus, fără să le caut eu. Fă-l reflex după fiecare constatare închisă, **înainte** de commit — altfel fiecare rundă redeschide subiectul precedent și ajungem să discutăm de trei ori aceeași regulă în loc să trecem la următoarea.

### B7 — `DELETE` pe departament tot șterge angajații (deschis din runda 3)

**CUM E ACUM.** Confirmat din nou:

```
GET /api/employees            -> 7 angajati
DELETE /api/department/2
GET /api/employees            -> 5 angajati
```

Mecanismul e explicat în runda 3 (B4): `cascade = CascadeType.ALL` propagă și `REMOVE`, iar `orphanRemoval = true` șterge copiii scoși din colecție. Amândouă sunt potrivite pentru copii care nu există fără părinte (linii de factură), nu pentru oameni.

Acum e mai grav decât în runda 3, pentru un motiv nou: atunci angajații intrau în baza de date doar prin seeder. Azi ai `POST /api/employees/{departmentid}`, deci un client îți poate crea 50 de angajați și îi poate pierde pe toți cu un `DELETE` care, din afară, pare că șterge un departament gol.

**Rămâne o decizie de design pe care trebuie s-o iei tu** — variantele sunt în runda 3.

---

## 🟡 Importante

**M1 — `POST /api/employees/{departmentid}` spune altceva decât face.** Într-un URL REST, segmentul de după numele colecției e id-ul unei resurse **din acea colecție** — deci `/api/employees/7` înseamnă „angajatul 7". Tu pui acolo id-ul unui departament. Cine citește URL-ul înțelege exact pe dos. Angajatul se creează *într-un* departament, deci resursa e imbricată: `POST /api/departments/{id}/employees`.

**M2 — `changeSalary` și `changeSalaryThruUrl` fac același lucru.** A doua e cea nevalidată (B3). Păstreaz-o pe prima.

**M3 — gărzile din `updateEmployee` sunt cod mort.** `EmployeeUpdateRequest` are acum `@NotBlank`/`@NotNull` pe toate câmpurile — bine, e semantică de `PUT` corectă. Dar asta înseamnă că `if(request.firstName()!=null && !request.firstName().isBlank())` din serviciu nu poate fi niciodată fals: validarea a respins deja cererea înainte să ajungă acolo. Șase `if`-uri care nu se pot închide. Scoate-le.

**M4 — `EmployeeResponse.from` presupune că orice angajat are departament.** `employee.getDepartment().getName()` — dar coloana `department_id` e nullable, iar în varianta „angajații rămân după ștergerea departamentului" din B7 vei avea exact angajați fără departament. Atunci endpoint-ul de listare va da `NullPointerException` → 500. Tratează cazul acum: `department == null ? null : department.getName()`.

**M5 — POST tot răspunde `200 OK`** în loc de `201 Created` + `Location`. Deschis din runda 1, acum pe două endpoint-uri.

**M6 — `ChangeSalaryResponse.updatedRows`** — același comentariu ca la buget: pe un `PATCH` care vizează o singură resursă, numărul de rânduri afectate nu poate fi decât 1, deci nu comunică nimic clientului.

**M7 — `@Transactional` de la `jakarta.transaction`**, iar cele două servicii de query n-au `readOnly = true`. Deschis din runda 3.

**M8 — adnotări de web în interfețe de serviciu.** `DepartmentCommandService.java:12-13` importă și folosește `@PathVariable`/`@RequestBody`; `EmployeeCommandService.java:4` importă `@Validated` fără să-l folosească.

**M9 — `/api/department` singular, `/api/employees` plural.** Aceeași aplicație, două convenții.

**M10 — numele departamentului tot nu e unic;** `mvnw` tot comis fără bit de execuție (`git update-index --chmod=+x mvnw`).

---

## 🟢 Cleanups

- **C1 — `EmployeeCreateRequest.java:4`** importă entitatea `Employee` într-un DTO; **`EmployeeQueryService.java:4`** la fel. Deschise din runda 3.
- **C2 — mapare duplicată:** `EmployeeCommandServiceImpl.java:49-54` reconstruiește `EmployeeResponse` de mână, deși `EmployeeResponse.from(employee)` face exact asta, acum inclusiv cu `departmentName`.
- **C3 — typo migrat.** Ai reparat „Salary must be **pozitive**" în `EmployeeCreateRequest.java:24` ✅ — și l-ai copiat, greșit, în `EmployeeUpdateRequest.java:23`. Aceeași poveste ca la B6, în miniatură: reparat unde ți-am arătat, reintrodus prin copy-paste alături.
- **C4 — mesajul „Updating all rows is necessary for PUT mapping"** apare de trei ori, pe trei câmpuri diferite, și nu spune utilizatorului ce e greșit. Mesajele de validare se adresează clientului API-ului: `"Department name required"`.
- **C5** — `root`/`root` în `application.yaml:4-5`; **C6** — `@Order(1)` inutil; **C7** — fișiere fără newline final; **C8** — `pom.xml` cu taguri goale.

---

## Before / After

| # | Acum | Corect |
|---|---|---|
| **B1** | `@NotBlank(...)`<br>`@PositiveOrZero(...)`<br>`Double budget` | `@NotNull(message = "Department budget required")`<br>`@PositiveOrZero(...)`<br>`Double budget` |
| **B2** | `return new ChangeSalaryResponse(`<br>&nbsp;&nbsp;`employee.getId(),`<br>&nbsp;&nbsp;`employee.getSalary(),`<br>&nbsp;&nbsp;`updatedRows);` | `return new ChangeSalaryResponse(`<br>&nbsp;&nbsp;`employee.getId(),`<br>&nbsp;&nbsp;`request.salary(),`<br>&nbsp;&nbsp;`updatedRows);` |
| **B3** | `@PatchMapping("/{id}/{newSalary}")`<br>`... changeSalaryThruUrl(`<br>&nbsp;&nbsp;`@PathVariable Long employeeId,`<br>&nbsp;&nbsp;`@PathVariable BigDecimal newSalary)` | *(șterge endpoint-ul; `PATCH /{id}/salary` îl acoperă cu validare)* |
| **B4** | `if(... && !employeeRepository.existsByEmail(request.email())){`<br>&nbsp;&nbsp;`employee.setEmail(request.email());`<br>`}` | `employeeRepository.findByEmail(request.email())`<br>&nbsp;&nbsp;`.filter(other -> !other.getId().equals(id))`<br>&nbsp;&nbsp;`.ifPresent(o -> { throw new EmailAlreadyUsed(); });`<br>`employee.setEmail(request.email());` |
| **B5** | *(nu există)* | `@RestControllerAdvice` cu `@ExceptionHandler` pentru: excepțiile „nu găsesc" → 404, `MethodArgumentNotValidException` → 400, `ConstraintViolationException` → 400, `DataIntegrityViolationException` / `EmailAlreadyUsed` → 409 |
| **B6** | `findById(id).orElseThrow();`<br>*(4 locuri)* | `findById(id).orElseThrow(EmployeeIdNotFound::new);`<br>`findById(id).orElseThrow(DepartmentIdNotFound::new);` |

---

## Ordinea de atac

1. **B1** — un endpoint întreg e mort. `@NotNull` în loc de `@NotBlank`, zece secunde.
2. **B3** — șterge endpoint-ul cu salariul în URL; e singurul prin care intră date invalide în baza de date.
3. **B6** — rulează `grep -rn "orElseThrow()" src/main/java` și repară toate cele patru dintr-o dată.
4. **B5** — advice-ul global. După el, B6 devine vizibil ca 404, iar B4 poate returna 409.
5. **B4** — refuz explicit în loc de tăcere.
6. **B2** — răspunsul corect la patch-ul de salariu.
7. **B7** — decizia despre cascade; scrie-mi ce vrei să însemne „șterg un departament" și o discutăm.
8. Apoi 🟡-urile.

---

## Q&A — verifică-ți înțelegerea

1. `mvn compile` trece verde, dar `PUT /api/department/1` dă 500 la orice cerere, din cauza unei adnotări. De ce nu poate compilatorul să prindă `@NotBlank` pus pe un `Double`, și în ce moment exact al unei cereri se descoperă problema?

2. Entitatea `Employee` are `@Positive` pe `salary`, și totuși `PATCH /api/employees/1/-5000` a scris −5000 în baza de date. Explică de ce adnotarea nu s-a activat — ce anume din felul în care funcționează un `update` JPQL de tip bulk o ocolește?

3. `PUT` cu un email deja folosit a răspuns `200 OK` și n-a schimbat nimic. Din perspectiva cuiva care consumă API-ul tău dintr-o aplicație de frontend: ce ar afișa utilizatorului după răspunsul ăsta, și de ce `409` cu un mesaj e mai ușor de folosit decât `200` cu date nemodificate?

---

**Următorul pas:** repară în ordinea de mai sus, rulează cele două `grep`-uri înainte de commit, și scrie-mi „next".

**Alături de review:** `IDEI-ENDPOINTURI.pdf` în rădăcina repo-ului — 12 endpoint-uri noi de implementat, pe trei niveluri, toate pe modelul pe care îl ai deja. Ia-le după ce închizi 🔴-urile de mai sus.

---

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

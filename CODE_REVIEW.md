# Code review — company-management

Proiect Spring Boot 4.1.1 / Java 21, temă JPA 1—* (`Department` 1—* `Employee`) expusă prin REST.

---

## Runda 2 — 2026-09-02 — sha `e8f05ad`

> ### 🔴 POARTA DE COMPILARE: ÎNCHISĂ
> `mvn -DskipTests compile` **eșuează**. Nimic din ce urmează nu a putut fi verificat pe repo-ul tău așa cum e — a trebuit să repar întâi numele interfeței, într-o copie de lucru, ca să pot porni aplicația.

### B1 🔴 Nu compilează: ai redenumit fișierul, nu și tipul

`employee/services/interfaces/EmployeeCommandService.java:8`

```java
public interface EmployeeComandService {
```

Fișierul se numește acum `EmployeeCommandService.java` (bine, era M9 din runda 1), dar interfața dinăuntru a rămas `EmployeeComandService`. În Java, **un tip `public` trebuie să aibă exact numele fișierului**. Compilatorul e clar:

```
[ERROR] EmployeeCommandService.java:[8,8] interface EmployeeComandService is public,
        should be declared in a file named EmployeeComandService.java
```

Și `EmployeeCommandServiceImpl.java:12` (import) și `:16` (`implements`) încă trimit la vechiul nume.

**Partea importantă, și motivul pentru care punctul ăsta merită citit de două ori.** Când rulezi buildul vezi ~20 de erori, iar 19 dintre ele arată așa:

```
[ERROR] DepartmentResponse.java:[15,27] cannot find symbol
  symbol:   method getId()
  location: variable department of type ...Department
```

Astea **nu sunt bug-uri**. Lombok generează getterele în timpul compilării, ca annotation processor. Când javac cade în prima rundă de procesare, Lombok nu mai apucă să genereze nimic, deci brusc „dispar" toate metodele `@Getter` din tot proiectul. E o singură cauză reală care se multiplică în 19 simptome false.

Dovedit: am copiat proiectul în scratchpad, am schimbat **doar** numele interfeței (2 fișiere, 3 linii) și nimic altceva — `mvn compile` a trecut curat, toate cele 19 „cannot find symbol" au dispărut.

> **Regula de lucru, a doua oară în echipă:** rulezi buildul **înainte** de commit. David și Ștefan au comis fiecare cod care nu compila; nu intra și tu în lista aia. Un `mvn -DskipTests compile` durează 4 secunde.
>
> Și când vezi un morman de erori: **citește-o pe prima**. Restul sunt aproape întotdeauna consecința ei.

### B2 🔴 Mesajul commit-ului promite o schimbare care nu e în commit

```
e8f05ad  "changed creation process for Employee, removed Employee list creation from Department creation"

 .../{EmployeeComandService.java => EmployeeCommandService.java} | 0
 1 file changed, 0 insertions(+), 0 deletions(-)
```

Commit-ul conține **exclusiv** redenumirea fișierului. Zero linii schimbate. Nici „creation process for Employee", nici scoaterea listei de angajați din crearea departamentului — nimic din ce scrie în mesaj nu există în cod.

Ori ai uitat să dai `git add` la fișierele modificate, ori ai scris mesajul pentru ce *aveai de gând* să faci. Ambele variante te costă la fel: peste două săptămâni, `git log` te minte. Verifică întotdeauna cu `git status` și `git diff --staged` înainte de `git commit`.

(Separat: dacă chiar vrei să scoți crearea de angajați din `POST /api/department`, discutăm întâi — momentan e singurul mod prin care un angajat ajunge în baza de date, pentru că B6 de mai jos e încă deschis.)

### Restanțe din runda 1 — **toate 5 sunt neatinse**

`git diff 943d7ad..e8f05ad` atinge 5 fișiere: 4 DTO-uri goale + o redenumire. Niciunul dintre fișierele semnalate în runda 1 nu a fost deschis.

Reverificat prin rulare (pe copia reparată, ca să pot porni aplicația):

| # | Constatare | Fișier | Status |
|---|---|---|---|
| **B3** | `@Valid` lipsește pe `List<EmployeeCreateRequest>` → validarea angajaților nu rulează | `DepartmentCreateRequest.java:21-22` | 🔴 NEATINS — POST cu angajat invalid: **500** |
| **B4** | Nu există `@RestControllerAdvice` → orice eroare = 500 gol | *(nu există fișierul)* | 🔴 NEATINS — `accessCode` duplicat: **500** |
| **B5** | Lista goală tratată ca eroare | `DepartmentQueryServiceImpl.java:25-26` | 🔴 NEATINS — cod identic cu runda 1 |
| **B6** | Nu există `EmployeeController`; serviciul de employee e cod mort | *(nu există pachetul `employee/controllers/`)* | 🔴 NEATINS — `POST /api/employee`: **404**, `POST /api/departments/1/employees`: **404** |
| **B7** | `jobTitle`: DTO `@Size(max=50)` vs coloană `length = 20` | `EmployeeCreateRequest.java:29` ↔ `Employee.java:51` | 🔴 NEATINS — `jobTitle` de 31 caractere: **500** |

Detaliile complete, cu cererile HTTP și stack trace-urile, sunt în runda 1 de mai jos și în istoricul git (commit `8dd67fc`).

**Observație onestă:** dintre cele 6 constatări pe care le-ai atins în vreun fel, ai închis una singură (redenumirea din M9) — și aia a spart buildul. Nu-i o critică la adresa efortului, e o problemă de ordine: ai trecut la funcționalitate nouă (`ChangeBudget`, `DepartmentUpdate`) peste un API care încă răspunde 500 la fiecare eroare și căruia îi lipsește jumătate din endpoint-uri. Construcția nouă se sprijină pe fundația care încă are cele 5 găuri.

### 🟡 Nou în runda asta

**M1 — cele 4 DTO-uri noi sunt schelete goale, două dintre ele greșit ca formă.**

```java
public record ChangeBudgetRequest() { }
public record ChangeBudgetResponse() { }
public class DepartmentUpdateRequest { }
public class DepartmentUpdateResponse { }
```

Două probleme, separate:
- **Sunt goale.** Un `record` fără componente nu poate transporta nimic; deserializarea unui JSON în el dă un obiect vid, tăcut. Dacă nu ești gata să le completezi, nu le comite — un fișier gol în repo arată ca funcționalitate existentă.
- **Două sunt `class`, două sunt `record`.** Toate celelalte DTO-uri din proiect sunt `record` (`DepartmentCreateRequest`, `DepartmentResponse`, `EmployeeCreateRequest`, `EmployeeResponse`) — și pe bună dreptate: sunt imutabile, cu `equals`/`hashCode` gratis. `DepartmentUpdateRequest` și `DepartmentUpdateResponse` trebuie să fie tot `record`.

Când le completezi, `ChangeBudgetRequest` are nevoie de exact aceleași reguli ca la creare — altfel apare tiparul pe care ți l-am semnalat deja pe `academy-hub-api`: **regula e apărată la `create` și cade la `update`**.

```java
public record ChangeBudgetRequest(
        @NotNull(message = "Budget required")
        @PositiveOrZero(message = "Department budget cannot be negative")
        Double budget
) { }
```

**M2 — restul de 🟡/🟢 din runda 1 sunt toate deschise**, inclusiv colecția Postman ruptă (`/api/company` → 404), `hibernate.dialect` hardcodat (`application.yaml:14`) și `mvnw` fără bit de execuție. Vezi runda 1.

### Before / After — runda 2

| # | Acum | Corect |
|---|---|---|
| **B1** | `EmployeeCommandService.java`<br>`public interface EmployeeComandService {`<br><br>`EmployeeCommandServiceImpl.java`<br>`import ...interfaces.EmployeeComandService;`<br>`... implements EmployeeComandService {` | `EmployeeCommandService.java`<br>`public interface EmployeeCommandService {`<br><br>`EmployeeCommandServiceImpl.java`<br>`import ...interfaces.EmployeeCommandService;`<br>`... implements EmployeeCommandService {` |
| **M1** | `public class DepartmentUpdateRequest { }` | `public record DepartmentUpdateRequest(`<br>&nbsp;&nbsp;&nbsp;&nbsp;`@NotBlank @Size(max = 50) String name,`<br>&nbsp;&nbsp;&nbsp;&nbsp;`@NotBlank @Size(max = 20) String location`<br>`) { }` |

### Ordinea de atac

1. **B1** — repară numele interfeței, rulează `mvn -DskipTests compile`, abia apoi commit.
2. **B4** (advice global) — după el, toate celelalte erori încep să spună ce s-a întâmplat.
3. **B3** + **B7** — validarea intrării, ca 400-urile să apară înainte de baza de date.
4. **B5** — lista goală = `200 []`.
5. **B6** — `EmployeeController`; abia atunci tema e completă.
6. Apoi `ChangeBudget` / `DepartmentUpdate`, cu regulile de validare duplicate din `create`.

### Q&A — runda 2

1. Buildul a scos 20 de erori, dintre care 19 spuneau `cannot find symbol: method getId()` pe clase pe care nu le-ai atins deloc. De ce dispar getterele generate de Lombok atunci când o *altă* clasă are numele greșit? Ce anume din felul în care Lombok își face treaba explică asta?

2. `git show --stat e8f05ad` arată `1 file changed, 0 insertions(+), 0 deletions(-)`. Ce comandă ai fi rulat **înainte** de `git commit` ca să vezi că nu ai pus în commit ce credeai că pui?

3. Rămâne întrebarea 1 din runda 1, încă deschisă: la POST-ul cu angajat invalid, mesajele de validare apar, dar ca 500, iar stack trace-ul zice `during persist time`. Dacă `EmployeeCreateRequest` nu e validat deloc, **de unde vin mesajele** și de ce e o problemă că vin de acolo?

---

## Runda 1 — 2026-09-02 — sha `943d7ad`

Verificată prin rulare pe H2, 13 probe HTTP. Stare la runda 2:

**🔴 Critice**
- B1 `@Valid` lipsă pe `List<EmployeeCreateRequest>` (`DepartmentCreateRequest.java:21`) — validarea angajaților nu rulează; erorile ies ca 500 `during persist time`. → 🔴 **NEATINS**
- B2 zero `@RestControllerAdvice` — `NoDepartmentFound`, `DepartmentIdNotFound`, `DataIntegrityViolationException`, `ConstraintViolationException`, toate ies ca 500 gol. → 🔴 **NEATINS**
- B3 listă goală tratată ca eroare (`DepartmentQueryServiceImpl.java:25-26`) — `GET` pe DB goală = 500 în loc de `200 []`. → 🔴 **NEATINS**
- B4 `EmployeeCommandServiceImpl` e cod mort, nu există `EmployeeController` — `POST /api/employee` → 404. → 🔴 **NEATINS**
- B5 `jobTitle` DTO `@Size(max=50)` vs coloană `length = 20` — cerere validă după contract = 500 `Value too long for column "JOB_TITLE VARCHAR(20)"`. → 🔴 **NEATINS**

**🟡 Importante** — toate deschise, cu o excepție:
M1 colecția Postman cere `/api/company`, path șters în `19753fd` → 404, iar „POST create department" e metodă `GET` cu URL gol · M2 `hibernate.dialect` hardcodat (`application.yaml:14`), a treia oară aceeași regulă, rupe `@DataJpaTest` pe H2 · M3 un `GET` = două SELECT-uri identice (`DepartmentQueryServiceImpl.java:25` și `:28`) · M4 `mvnw` comis fără bit de execuție (mod `100644`) → `./mvnw` = `permission denied` · M5 POST răspunde `200` în loc de `201 Created` + `Location` · M6 niciun serviciu nu e `@Transactional` · M7 `orElse(null)` + `if/else throw` în loc de `orElseThrow` (`EmployeeCommandServiceImpl.java:30, 41-44`) · M8 numele de departament nu e unic · **M9 naming — ⚠️ ÎNCHIS PARȚIAL: `EmployeeComandService` redenumit, dar doar fișierul, nu și tipul → vezi B1 runda 2**; rămân pachetul `CompanyManagement` cu majusculă, pluralul inconsistent `repository`, `services/interfaces/` · M10 `/api/department` la singular.

**🟢 Cleanups** — toate deschise: C1 importuri nefolosite (`DepartmentRepository.java:5` importă un DTO, `EmployeeCreateRequest.java:4,6` importă `jakarta.persistence.Column` și API intern de Hibernate) · C2 mapare duplicată în loc de `DepartmentResponse.from` / `EmployeeResponse.from` · C3 `jakarta.annotation.Nullable` fără efect · C4 typo „pozitive" → `positive` · C5 `root`/`root` în `application.yaml:4-5` · C6 `@Order(1)` inutil · C7 `pom.xml` cu taguri goale.

**✅ Ce e bine — nu strica astea la refactor**
`equals`/`hashCode` pe entități sunt scrise **corect** pentru JPA (`hashCode()` constant pe clasă, `equals` pe `id` cu gardă `id != null`) — rar nimerit din prima · constructor `protected` + constructor de business, fără `@Setter` pe `id` · helper-ele bidirecționale `addEmployee`/`removeEmployee` · `left join fetch` (`DepartmentRepository.java:12-16`) dă **un singur SELECT**, fără N+1 și fără duplicate — Hibernate 6+ deduplică singur, `distinct` nu-ți mai trebuie · separarea Command/Query · rollback corect: departamentul nu rămâne salvat pe jumătate când un angajat pică validarea.

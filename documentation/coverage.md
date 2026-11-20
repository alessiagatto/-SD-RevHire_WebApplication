# Report sulla Code Coverage – RevHire WebApplication

Questo documento descrive la configurazione di **JaCoCo** e l'analisi dei risultati di copertura dei test per **RevHire WebApplication**.

---

## 1. Panoramica

L'analisi della code coverage è affidata al plugin **`jacoco-maven-plugin` (versione `0.8.11`)** configurato nel `pom.xml` ed eseguito automaticamente durante la fase Maven `test` (`mvn clean test -Dspring.profiles.active=test`):

- **Esecuzioni configurate:**
  1. `prepare-agent`: strumenta il bytecode a runtime durante l'esecuzione della suite JUnit/Mockito.
  2. `report`: genera i report navigabili in formato HTML e XML (`target/site/jacoco/`), escludendo classi di utilità (`com/RevHire/util/**`) e la classe principale di bootstrap (`com/RevHire/*.class`).
  3. `check` (Quality Gate): verifica il rispetto delle soglie minime di copertura prima di completare la build.

---

## 2. Lombok

Poiché il progetto utilizza **Lombok** per generare automaticamente metodi boilerplate (`getter`, `setter`, `equals`, `hashCode`, `toString`), tali metodi venivano inizialmente conteggiati da JaCoCo come non coperti, abbassando artificialmente le metriche. È stato quindi introdotto il file `lombok.config` nella root del repository con `lombok.addLombokGeneratedAnnotation = true`, consentendo a JaCoCo di riconoscere l'annotazione `@lombok.Generated` ed escludere automaticamente il codice boilerplate dal calcolo.

---

## 3. Perimetro e soglie

Coerentemente con la strategia di testing del progetto, il controllo di soglia è stato applicato **esclusivamente al package `com/RevHire/service/impl/**`**, dove risiede tutta la logica di business, le validazioni e la gestione delle eccezioni (mentre `entity`/`dto` contengono solo strutture dati e i `repository` sono interfacce Spring Data JPA isolate via mock). Le soglie configurate sono:
  - **Line Coverage:** minimo **80%**
  - **Branch Coverage:** minimo **75%**
  - **Class Coverage:** **100%**

---

## 4. Analisi dei risultati di Coverage

A seguito dell'introduzione di `lombok.config` e del potenziamento mirato delle sette suite di test dei servizi (`AuthServiceImplTest`, `JobServiceImplTest`, `JobSeekerServiceImplTest`, `ApplicationServiceImplTest`, `ResumeServiceImplTest`, `EmployerServiceImplTest`, `NotificationServiceImplTest`), il quality gate configurato sul package `com.RevHire.service.impl` viene superato con successo:

- **Line Coverage (`com.RevHire.service.impl`):** superiore alla soglia minima dell'**80%**.
- **Branch Coverage (`com.RevHire.service.impl`):** superiore alla soglia minima del **75%**.
- **Metodi non coperti dai test:** I metodi privi di copertura residua riguardano esclusivamente metodi accessori o helper interni secondari non invocati nei flussi principali (oltre ai metodi di configurazione, controller web ed entity/DTO esclusi dal perimetro del quality gate).

---

## 5. Piano di mantenimento della Coverage

Per consolidare ulteriormente l'affidabilità della suite nelle prossime iterazioni, viene mantenuto attivo il goal `jacoco:check` nella pipeline GitHub Actions (`.github/workflows/ci.yml`).
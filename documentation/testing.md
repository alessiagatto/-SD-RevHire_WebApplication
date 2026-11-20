# Report sui test unitari – RevHire WebApplication

Questo documento descrive la strategia di test unitari, l'applicazione dei principi FIRST e l'integrazione nella pipeline CI adottati per **RevHire WebApplication**.

---

## 1. Panoramica

Il progetto disponeva già di una suite di test di base, ma si è scelto di potenziare e riscrivere in modo mirato i test del package `com.RevHire.service.impl`, lasciando invariati quelli già presenti negli altri package. Questa scelta deriva dal fatto che la logica di business, le validazioni critiche (duplicati, stati non validi, gestione file) e la gestione delle eccezioni risiedono nei *Service*, mentre `entity`/`dto` contengono solo strutture dati e i `repository` sono interfacce JPA già isolate tramite `@Mock`. In questo modo si massimizza l'affidabilità dei flussi critici mantenendo l'esecuzione veloce e indipendente da risorse esterne come il database Oracle o il filesystem reale.

L'infrastruttura di test si basa su due strumenti principali configurati in `pom.xml`:
- **JUnit 6 (`junit-jupiter:6.1.3`)**
- **Mockito 5 (`mockito-junit-jupiter:5.24.0`)**
---

## 2. Principi FIRST e strategia di isolamento

Tutte le classi di test utilizzano `@ExtendWith(MockitoExtension.class)` per mantenere l'esecuzione aderente ai principi **FIRST**:

- **Fast (Veloce):** assenza di contesto Spring completo e di connessioni al database, con esecuzione in memoria nell'ordine dei millisecondi.
- **Independent (Indipendente):** ogni metodo di test istanzia e configura i propri mock in isolamento, senza condividere stato mutabile con altri test.
- **Repeatable (Ripetibile):** input deterministici e comportamento dei repository controllato tramite stubbing (`when(...).thenReturn(...)`), evitando dipendenze da rete, orologio o file locali.
- **Self-validating (Autovalidante):** esito determinato automaticamente tramite asserzioni JUnit (`assertEquals`, `assertThrows`, `assertTrue`, `assertFalse`) e verifiche Mockito (`verify`).
- **Thorough (Approfondito):** copertura dei flussi principali, dei rami condizionali, dei casi limite (valori nulli, vuoti o duplicati) e delle eccezioni, mantenendo aderenza al comportamento reale del codice di produzione.

---

## 3. Suite di test e casi coperti

Le sette classi di test dei servizi applicativi sono state riviste per verificare non solo il funzionamento standard, ma anche i casi limite, le regole di validazione e la gestione degli errori:

- **`AuthServiceImplTest`:** copre l'intero ciclo di gestione dell'account — registrazione con controllo sui duplicati, autenticazione con verifica e aggiornamento automatico delle password cifrate, recupero/modifica delle credenziali e rimozione dell'utente, includendo i casi di dati errati o account inesistenti.
- **`JobServiceImplTest`:** verifica la gestione completa delle offerte di lavoro — pubblicazione con valori predefiniti, consultazione e ricerca filtrata, modifica, cambio di stato (`OPEN` ↔ `CLOSED`), eliminazione, ordinamento e suggerimenti mirati, gestendo anche l'assenza dell'azienda o dell'offerta.
- **`JobSeekerServiceImplTest`:** testa le funzionalità principali del candidato — gestione del profilo, creazione automatica del curriculum, caricamento di file PDF (inclusa la gestione di errori I/O), salvataggio dei preferiti senza duplicati, lettura delle notifiche e ricerca/raccomandazione delle offerte.
- **`ApplicationServiceImplTest`:** valida il flusso delle candidature — invio con o senza allegato, blocco delle candidature duplicate, controlli sull'appartenenza dei file al candidato, ritiro della candidatura, aggiornamento degli stati con invio automatico di notifiche e inserimento di note da parte dell'azienda.
- **`ResumeServiceImplTest`:** verifica la gestione strutturata del curriculum — creazione e aggiornamento del profilo professionale, gestione di formazione, esperienze, competenze, progetti e certificazioni, operazioni su file (upload, download, eliminazione) e generazione dell'anteprima HTML.
- **`EmployerServiceImplTest`:** copre la gestione del profilo aziendale (creazione, aggiornamento, validazione dell'email e conflitti), il recupero dei dati e il calcolo delle statistiche e della percentuale di completamento nella dashboard.
- **`NotificationServiceImplTest`:** verifica la creazione e il salvataggio delle notifiche (controllando destinatario, messaggio e stato iniziale non letto), il recupero dello storico per utente e la marcatura come letta, inclusi i casi di utente o notifica assenti.
---

## 4. Integrazione CI/CD

L'esecuzione della suite è automatizzata nella pipeline **CI/CD** (`.github/workflows/ci.yml`) su runner `ubuntu-latest` con **JDK 21** tramite Maven (`mvn clean test -Dspring.profiles.active=test`):

- **Quality gate:** qualsiasi fallimento nei test interrompe immediatamente la pipeline e blocca la generazione dell'artefatto JAR e il successivo workflow Docker.
- **Tracciabilità:** i report XML/testuali generati da Surefire vengono archiviati in `target/surefire-reports/`.
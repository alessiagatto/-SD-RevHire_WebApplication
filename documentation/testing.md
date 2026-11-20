# Report sui test unitari – RevHire WebApplication

Questo documento descrive la strategia di test unitari, l'applicazione dei principi FIRST e l'integrazione nella pipeline CI adottati per **RevHire WebApplication**.

---

## 1. Panoramica

Il progetto disponeva già di una suite di test di base, ma si è scelto di potenziare e riscrivere in modo mirato i test del package `com.RevHire.service.impl`, lasciando invariati quelli degli altri package. La logica di business, le validazioni critiche (duplicati, stati non validi, gestione file) e la gestione delle eccezioni risiedono infatti nei *Service*, mentre `entity`/`dto` contengono solo strutture dati e i `repository` sono interfacce JPA isolate tramite `@Mock`. Così si garantisce l'affidabilità dei flussi critici con test veloci e indipendenti da risorse esterne come il database Oracle o il filesystem.

L'infrastruttura di test si basa su due strumenti configurati in `pom.xml`:
- **JUnit 6** (`junit-jupiter:6.1.3`)
- **Mockito 5** (`mockito-junit-jupiter:5.24.0`)

---

## 2. Principi FIRST e strategia di isolamento

Tutte le classi di test usano `@ExtendWith(MockitoExtension.class)` per rispettare i principi **FIRST**:

- **Fast:** nessun contesto Spring completo né connessione al database; esecuzione in memoria nell'ordine dei millisecondi.
- **Independent:** ogni test configura i propri mock, senza condividere stato mutabile con altri test.
- **Repeatable:** input deterministici e repository controllati tramite stubbing (`when(...).thenReturn(...)`), senza dipendere da rete, orologio o file locali.
- **Self-validating:** esito stabilito automaticamente da asserzioni JUnit (`assertEquals`, `assertThrows`, ...) e verifiche Mockito (`verify`).
- **Thorough:** copertura dei flussi principali, dei rami condizionali, dei casi limite (valori nulli, vuoti o duplicati) e delle eccezioni.

### Un caso concreto: un test che falliva a intermittenza

Durante le verifiche, un test di `JobSeekerServiceImplTest` non rispettava il principio *Repeatable*, poiché dava esiti diversi a parità di codice: in alcune esecuzioni passava, in altre falliva con l'errore "Seeker not found".

La causa era nel modo in cui i mock venivano collegati al servizio. Il servizio ha due dipendenze dello stesso tipo, e l'annotazione `@InjectMocks` le assegnava in base al tipo e non al nome, quindi poteva scambiarle in modo imprevedibile. Il problema è stato risolto creando il servizio a mano nel metodo `setUp()`, passando ogni mock nel punto giusto.

---

## 3. Suite di test e casi coperti

Le sette classi di test dei servizi che verificavano solo il funzionamento standard, ora verificano anche i casi limite, le regole di validazione e la gestione degli errori:

- **`AuthServiceImplTest`:** registrazione con controllo dei duplicati, autenticazione con aggiornamento delle password cifrate, recupero/modifica delle credenziali, rimozione dell'utente; casi di dati errati o account inesistenti.
- **`JobServiceImplTest`:** pubblicazione con valori predefiniti, ricerca filtrata, modifica, cambio di stato (`OPEN` ↔ `CLOSED`), eliminazione, ordinamento e suggerimenti; assenza di azienda o offerta.
- **`JobSeekerServiceImplTest`:** profilo del candidato, creazione automatica del curriculum, caricamento PDF (con errori I/O), preferiti senza duplicati, notifiche, ricerca e raccomandazione delle offerte.
- **`ApplicationServiceImplTest`:** invio candidature con o senza allegato, blocco dei duplicati, controllo di appartenenza dei file, ritiro, aggiornamento stati con notifiche automatiche e note dell'azienda.
- **`ResumeServiceImplTest`:** creazione e aggiornamento del curriculum (formazione, esperienze, competenze, progetti, certificazioni), operazioni sui file e anteprima HTML.
- **`EmployerServiceImplTest`:** profilo aziendale (creazione, aggiornamento, validazione email, conflitti), statistiche e percentuale di completamento della dashboard.
- **`NotificationServiceImplTest`:** creazione e salvataggio delle notifiche (destinatario, messaggio, stato non letto), storico per utente e marcatura come letta, con utente o notifica assenti.

---

## 4. Integrazione CI/CD

La suite è eseguita nella pipeline **CI/CD** (`.github/workflows/ci.yml`) su `ubuntu-latest` con **JDK 21** tramite Maven (`mvn clean test -Dspring.profiles.active=test`):

- **Quality gate:** un test fallito interrompe la pipeline e blocca la generazione del JAR e il successivo workflow Docker.
- **Tracciabilità:** i report di Surefire sono archiviati in `target/surefire-reports/`.
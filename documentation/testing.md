# Report sui test unitari – RevHire WebApplication

Questo documento descrive la strategia di test unitari, l'applicazione dei principi FIRST e l'integrazione nella pipeline CI/CD adottati per **RevHire WebApplication**.

---

## 1. Panoramica

La suite di test unitari verifica in isolamento i componenti principali dello strato applicativo (`com.RevHire.service.impl`), garantendo che la logica di business, le regole di validazione e la gestione degli errori funzionino in modo deterministico senza dipendere da risorse esterne come il database Oracle o il filesystem reale.

L'infrastruttura di test si basa su due strumenti principali configurati in `pom.xml`:
- **JUnit 6 (`junit-jupiter:6.1.3`)**: framework di esecuzione e asserzione dei casi di test.
- **Mockito 5 (`mockito-junit-jupiter:5.24.0`)**: libreria per la creazione di dipendenze simulate (`@Mock`), l'iniezione automatica nelle classi sotto test (`@InjectMocks`) e la verifica delle interazioni e degli argomenti salvati (`ArgumentCaptor`).

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

Le classi di test sono state riviste e strutturate per coprire i branch rilevanti dei sette servizi applicativi:

- **`AuthServiceImplTest`:** registrazione (successo, email duplicata, codifica password), login BCrypt (successo, password errata, prefisso `$2b`), login con password in chiaro con upgrade ad hash e utente inesistente, reset/aggiornamento password (successo, dati errati, utente assente) ed eliminazione utente.
- **`JobServiceImplTest`:** creazione offerta (valori predefiniti `OPEN`/`active=true`, employer assente), recupero offerte aperte e filtraggio/ricerca con mapping DTO, chiusura/eliminazione/aggiornamento/toggle stato (`OPEN` ↔ `CLOSED`), ordinamento (`name`, `recent`, default) e raccomandazioni.
- **`JobSeekerServiceImplTest`:** creazione/recupero/aggiornamento profilo, gestione e creazione automatica resume, upload file PDF ed errori I/O, preferiti (inclusi duplicati e mapping), notifiche/lettura/casi vuoti e ricerca/raccomandazioni.
- **`ApplicationServiceImplTest`:** candidatura con/senza allegato, duplicati, entità mancanti, controlli su resume/file/appartenenza file, mapping per seeker/job/employer, ritiro candidatura, aggiornamento stato e notifiche (`SHORTLISTED`, `REJECTED`, altri stati) e note dell'employer.
- **`ResumeServiceImplTest`:** CRUD resume, recupero per user/seeker/ID, gestione di education, experience, skill (inclusa sostituzione completa), certificazioni e progetti, upload/download/eliminazione/metadati file e generazione HTML con gestione delle assenze.
- **`EmployerServiceImplTest`:** creazione/aggiornamento profilo, validazione email nulla/vuota, utente assente e conflitto email, recupero profilo, statistiche dashboard e calcolo percentuali di completamento.
- **`NotificationServiceImplTest`:** invio notifica verificato con `ArgumentCaptor` (utente, messaggio, stato iniziale non letto), destinatario inesistente, mapping DTO/lista vuota e marcatura come letta.

---

## 4. Integrazione CI/CD

L'esecuzione della suite è automatizzata nella pipeline **CI/CD** (`.github/workflows/ci.yml`) su runner `ubuntu-latest` con **JDK 21** tramite Maven (`mvn clean test -Dspring.profiles.active=test`):

- **Quality gate:** qualsiasi fallimento nei test interrompe immediatamente la pipeline e blocca la generazione dell'artefatto JAR e il successivo workflow Docker.
- **Tracciabilità:** i report XML/testuali generati da Surefire vengono archiviati in `target/surefire-reports/`.
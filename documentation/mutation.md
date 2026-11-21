# Report sul Mutation Testing – RevHire WebApplication

Questo documento descrive la configurazione di **PITest**, l'analisi dei mutanti generati e gli interventi effettuati sulla suite di test.

---

## 1. Configurazione del Mutation Testing (`pom.xml`)

Per verificare l'effettiva capacità dei test unitari di rilevare regressioni nel codice applicativo, è stato integrato nel `pom.xml` il plugin **`pitest-maven`** (con supporto JUnit 5 tramite `pitest-junit5-plugin`):

- **Perimetro di analisi (`targetClasses` e `targetTests`):** Coerentemente con la strategia *Risk-Based* adottata per la coverage, l'analisi di mutazione è stata circoscritta esclusivamente alle implementazioni dei servizi applicativi (`com.RevHire.service.impl.*`) e alle rispettive classi di test (`com.RevHire.service.impl.*Test`). In questo modo si evita di generare mutanti su classi strutturali prive di logica decisionale (`entity`, `dto`, `repository` JPA) e si mantengono rapidi i tempi di esecuzione.
- **Operatori di mutazione:** È stato utilizzato il set predefinito (`DEFAULTS`), che introduce modifiche mirate sul bytecode quali inversione dei rami condizionali (`NegateConditionalsMutator`), rimozione di chiamate a metodi `void` (`VoidMethodCallMutator`), alterazione dei valori di ritorno (`NullReturnValsMutator`, `PrimitiveReturnsMutator`) e modifiche ai confini relazionali (`ConditionalsBoundaryMutator`).
- **Quality Gate (`mutationThreshold`):** La soglia minima di *mutation score* è stata configurata al **75%**, bloccando la build Maven (`exit code 1`) qualora la percentuale di mutanti uccisi risulti inferiore.

---

## 2. Analisi dei risultati e raggiunta soglia del 75%

L'esecuzione di PITest sulle sette classi di servizio ha evidenziato un'evoluzione significativa tra la prima esecuzione e la versione consolidata della suite:

- **Stato iniziale (sotto soglia):** Alla prima esecuzione il *mutation score* complessivo si attestava intorno al **62%** (inferiore al quality gate del 75%), con servizi lineari come `AuthServiceImpl` e `NotificationServiceImpl` già al **100%**, ma con numerosi mutanti sopravvissuti (`SURVIVED`) nelle classi più articolate (`ResumeServiceImpl`, `JobServiceImpl`, `JobSeekerServiceImpl`, `ApplicationServiceImpl`, `EmployerServiceImpl`).
- **Analisi dei mutanti sopravvissuti:** I mutanti non rilevati appartenevano principalmente a tre categorie:
  1. **Setter non verificati (`VoidMethodCallMutator`):** rimozione di assegnazioni su entità o DTO prima del salvataggio su repository, non intercettata da test che controllavano solo l'invocazione di `save(...)` o un singolo campo.
  2. **Lambda delle eccezioni (`NullReturnValsMutator`):** supplier passati a `.orElseThrow(...)` verificati solo sul tipo generico `RuntimeException` senza controllarne il messaggio.
  3. **Rami condizionali (`NegateConditionalsMutator`):** condizioni `if/else` parzialmente esplorate (es. assenza di notifiche su stati non notificabili o gestione di sezioni nulle/vuote).
- **Interventi effettuati ad alto livello:** Sono state rafforzate le cinque classi di test dei servizi principali e adeguati i relativi rami applicativi, introducendo verifiche puntuali sugli oggetti salvati tramite `ArgumentCaptor` e asserzioni sui singoli campi, controlli espliciti sui messaggi delle eccezioni e copertura dei rami alternativi e di ordinamento.
- **Risultato finale:** Con l'esecuzione mirata dei **44 test aggiornati** (tutti superati), PITest registra **207 mutanti uccisi su 276** (69 sopravvissuti), raggiungendo esattamente il **mutation score del 75,0%** e superando il quality gate configurato.

---

## 3. Piano di mantenimento del Mutation Testing

Per consolidare ulteriormente la capacità della suite di individuare regressioni nelle prossime iterazioni, l'esecuzione del goal `org.pitest:pitest-maven:mutationCoverage` viene integrata e mantenuta attiva come quality gate in una pipeline GitHub Actions (`.github/workflows/mutation.yml`), garantendo il rispetto continuo del `mutationThreshold` configurato al 75% sul package `com.RevHire.service.impl`. Il test viene non viene inserito nella pipeline di CI `.github/workflows/ci.yml` per non appesantire il carico lavorativo d'integrazione.

# Report sulla pipeline CI/CD – RevHire WebApplication

Questo documento descrive la pipeline di integrazione continua definita in `.github/workflows/ci.yml`.

---

## 1. Panoramica

La pipeline ha un unico job (`test`, "Build and Test with Java 21") su `ubuntu-latest`. Parte a ogni `push` su `main` e a ogni `pull_request` verso `main`, così nessuna modifica entra in `main` senza essere compilata e testata. Imposta `SPRING_PROFILES_ACTIVE=test` per usare il profilo Spring di test, e limita il `GITHUB_TOKEN` con `contents: read`.

---

## 2. Le fasi

1. **Checkout code** (`actions/checkout@v5`): scarica il commit che ha avviato la pipeline; test e build lavorano sulla stessa revisione.
2. **Set up JDK 21** (`actions/setup-java@v4`, Temurin): installa una versione del JDK dichiarata esplicitamente nel workflow.
3. **Cache Maven dependencies** (`actions/cache@v4`): salva `~/.m2` con chiave `${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}`. Se i `pom.xml` non cambiano, le dipendenze non vengono riscaricate; se cambiano, la chiave cambia e la cache si rigenera.
4. **Run tests** (`mvn clean test -Dspring.profiles.active=test`): `clean` elimina `target/` e forza una compilazione da zero, `test` compila ed esegue la suite. Un test fallito interrompe la pipeline (fail-fast) e blocca gli step successivi.
5. **Build application** (`mvn package -DskipTests`): produce il JAR `RevHire_WebApplication-1.0.0.jar`. I test non vengono rieseguiti perché già superati allo step 4.
6. **Upload JAR artifact** (`actions/upload-artifact@v4`, solo se `success()`): pubblica il JAR come `revhire-<SHA del commit>`, conservato 30 giorni.

---

## 3. Riproducibilità

Dato che la build produce lo stesso risultato indipendentemente da chi la esegue e da dove,
viene mantenuta la ripsoducibilità attraverso le seguenti caratteristiche:

1. **Ambiente pulito ed effimero.** Ogni esecuzione avviene su un runner nuovo, senza stato lasciato da build precedenti.
2. **Toolchain dichiarata nel codice.** Versione e distribuzione del JDK (Temurin 21) sono fissate nel workflow, versionato insieme al progetto: chiunque può vedere con quali strumenti viene costruita l'applicazione.
3. **Build da zero (`mvn clean`).** La directory `target/` viene sempre eliminata, quindi nessun file compilato in precedenza può influenzare il risultato.
4. **Dipendenze governate dal `pom.xml`.** Le librerie sono risolte da Maven a partire dal `pom.xml` versionato. La cache è indicizzata sull'hash dei `pom.xml`: cambiando le dipendenze la cache non viene riusata in modo errato, e la cache accelera la build senza alterarne il contenuto.
5. **Configurazione di test fissata.** Il profilo Spring `test` è impostato sia come variabile d'ambiente sia come parametro Maven, rendendo l'esecuzione dei test indipendente dalla configurazione locale.
6. **Azioni versionate.** Le azioni GitHub sono referenziate con tag di versione (`@v4`, `@v5`), evitando cambiamenti inattesi dovuti a nuove release major.
7. **Tracciabilità dell'artefatto.** Il JAR è nominato con lo SHA del commit e viene prodotto dallo stesso commit che ha superato i test, per cui dato un artefatto, è sempre possibile risalire al codice sorgente esatto da cui proviene.
8. **Pipeline come codice.** Il file `ci.yml` è sotto controllo di versione, quindi le modifiche al processo di build sono tracciate e sottoposte a revisione come qualsiasi altro codice.

---

## 4. Qualità del codice

1. **Quality gate automatico sui test.** Nessuna modifica passa inosservata: i test vengono eseguiti a ogni push su `main` e a ogni pull request verso `main`. Un test fallito blocca la pipeline e impedisce la produzione dell'artefatto.
2. **Verifica prima della distribuzione.** L'ordine degli step (test → build → upload) assicura che solo codice che ha superato la suite di test arrivi a diventare un artefatto distribuibile.
3. **Feedback rapido.** Gli sviluppatori vedono l'esito direttamente nella pull request (grazie a `checks: write` e `pull-requests: write`) e possono correggere gli errori prima del merge, riducendo il costo dei difetti individuati tardi.
4. **Compilazione come controllo implicito.** Errori di sintassi, incompatibilità di tipi o dipendenze mancanti vengono intercettati già nella fase di compilazione di `mvn clean test`.
5. **Isolamento dei test.** Il profilo `test` evita che i test dipendano da risorse di sviluppo o produzione, rendendo i risultati più affidabili e ripetibili.
6. **Principio del minimo privilegio (parziale).** `contents: read` impedisce che la pipeline modifichi il codice del repository.
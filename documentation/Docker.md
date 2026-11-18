# Report sulla containerizzazione – RevHire WebApplication

Questo documento descrive la configurazione Docker e Docker Compose e l'audit del workflow di riferimento per il rilascio dell'immagine.

---

## 1. Panoramica

L'architettura containerizzata separa l'applicazione web dal database relazionale mediante due servizi orchestrati in `docker-compose.yml` su una rete privata dedicata (`revhire-network`):

- **`web` (`revhire-web`)**: applicazione Spring Boot eseguita con Java 21 (Eclipse Temurin JRE) sulla porta `8082`, costruita tramite un `Dockerfile` multi-stage.
- **`db` (`revhire-oracle-db`)**: istanza Oracle Database Free (`gvenzl/oracle-free:23-slim-faststart`) con persistenza su volume nominato (`oracle_data`).

---

## 2. Configurazione dei file

### 2.1 `Dockerfile` (Multi-Stage Build)

Il `Dockerfile` adotta una costruzione a due stadi per separare la compilazione dall'ambiente di esecuzione finale:

1. **Stadio 1 (`builder` – Compilazione):** Parte da `maven:3.9.6-eclipse-temurin-21` ed esegue la build in `/build`. Copia prima il solo `pom.xml` ed esegue `mvn dependency:go-offline -B` per sfruttare la cache dei layer sulle dipendenze; poi copia `src/` e compila `RevHire_WebApplication-1.0.0.jar` con `mvn clean package -DskipTests`.
2. **Stadio 2 (Runtime leggero e sicuro):** Parte dall'immagine minimale `eclipse-temurin:21-jre-jammy` (priva di Maven e JDK), imposta `/app` e `PORT=8082`, e copia dal primo stadio solo il JAR compilato (`app.jar`).
3. **Esecuzione non-root e avvio:** Crea l'utente privo di privilegi `appuser`, gli assegna `/app`, imposta `USER appuser`, espone la porta `8082` (`EXPOSE 8082`) e avvia la JVM in forma `exec` (`CMD ["java", "-jar", "app.jar"]`).

### 2.2 `docker-compose.yml`

Il file `docker-compose.yml` coordina lo stack multi-container su una rete virtuale isolata (`revhire-network`):

1. **Servizio Database (`db`):** Avvia Oracle Database da `gvenzl/oracle-free:23-slim-faststart` con `restart: on-failure`. Le credenziali provengono da un file locale `.env` escluso dal versionamento. La porta `1521` non è pubblicata sull'host ma resta accessibile solo nella rete interna.
2. **Persistenza dei dati:** Il volume nominato `oracle_data` è montato su `/opt/oracle/oradata`, preservando i dati anche dopo `docker compose down` / `up`.
3. **Healthcheck e sincronizzazione:** `db` definisce un `healthcheck` tramite `healthcheck.sh` (ogni 15s); `web` usa `depends_on` con `condition: service_healthy`, attendendo che Oracle sia operativo prima di avviare Spring Boot.
4. **Servizio Applicazione (`web`):** Costruisce l'immagine dal `Dockerfile` locale, espone `8082:8082`, imposta `restart: on-failure` e sovrascrive a runtime le proprietà JDBC (`SPRING_DATASOURCE_URL` puntato all'hostname interno `db` e `SPRING_JPA_HIBERNATE_DDL_AUTO=update`).

---

## 3. Anti-pattern evitati e affidabilità

1. **Versioni esplicite (no `:latest`).** Tutte le immagini (`maven:3.9.6-eclipse-temurin-21`, `eclipse-temurin:21-jre-jammy`, `gvenzl/oracle-free:23-slim-faststart`) fissano tag precisi per garantire determinismo e riproducibilità.
2. **Esecuzione senza privilegi di `root`.** Lo stadio finale crea l'utente dedicato `appuser` e imposta `USER appuser`, rispettando il principio del minimo privilegio.
3. **Nessun segreto hardcodato.** Password e credenziali sono iniettate a runtime via variabili d'ambiente da un file `.env` escluso tramite `.gitignore`.
4. **Uso esclusivo di `COPY` al posto di `ADD`.** Evita comportamenti impliciti come estrazione automatica di archivi o fetch da URL remoti.
5. **Ottimizzazione multi-stage e forma `exec`.** Maven e JDK restano confinati in `builder` riducendo dimensioni e superficie d'attacco; `CMD ["java", "-jar", "app.jar"]` mantiene la JVM come `PID 1` per gestire correttamente i segnali di arresto.
6. **Tolleranza ai guasti e persistenza.** `healthcheck` + `depends_on: condition: service_healthy` evitano avvii prematuri; `restart: on-failure` garantisce il ripristino automatico; il volume `oracle_data` conserva i dati e l'assenza di `ports` su `1521` isola il database dall'esterno.

---

## 4. Audit del workflow di riferimento (`docker-build.yml`)

1. **Meccanismo di trigger.** Usa `workflow_run` in ascolto su `CI/CD Pipeline` (`types: [completed]`, branch `main`) con `if: github.event.workflow_run.conclusion == 'success'`, avviando la build solo a test superati.
2. **Gestione dei segreti.** Le credenziali DockerHub sono salvate nei GitHub Secrets (`DOCKER_USERNAME` e `DOCKER_PASSWORD` con Personal Access Token) e passate a runtime a `docker/login-action@v3`.
3. **Supporto multi-piattaforma.** Con `docker/setup-buildx-action@v3`, compila l'immagine per `linux/amd64` e `linux/arm64`, garantendo portabilità tra server x86_64 e sistemi ARM64.
4. **Strategia di caching.** Usa `actions/cache@v4` per le dipendenze Maven (`~/.m2`) e la cache nativa GitHub Actions (`cache-from: type=gha`, `cache-to: type=gha,mode=max`) per i layer Docker.
5. **Gestione dei metadati.** `docker/metadata-action@v5` genera dinamicamente tag (`latest` e versione) e label OCI dal contesto del repository.
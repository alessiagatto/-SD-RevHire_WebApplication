# Report sulla containerizzazione – RevHire WebApplication

Questo documento descrive la configurazione Docker, Docker Compose e il workflow di rilascio dell'immagine adottati per **RevHire WebApplication**.

---

## 1. Panoramica

L'architettura containerizzata separa l'applicazione web dal database relazionale mediante due servizi orchestrati in `docker-compose.yml` su una rete privata dedicata (`revhire-network`):

- **`web` (`revhire-web`)**: applicazione Spring Boot eseguita con Java 21 (Eclipse Temurin JRE) sulla porta `8082`, impacchettata tramite un `Dockerfile` single-stage.
- **`db` (`revhire-oracle-db`)**: istanza Oracle Database Free (`gvenzl/oracle-free:23-slim-faststart`) con persistenza su volume nominato (`oracle_data`).

---

## 2. Configurazione dei file

### 2.1 `Dockerfile` (Single-Stage Runtime)

Per evitare compilazioni ridondanti e garantire che nel container giri esattamente l'artefatto verificato dalla pipeline CI, il `Dockerfile` utilizza un'immagine di runtime leggera senza strumenti di build:

1. **Base image esplicita:** Parte da `eclipse-temurin:21-jre-jammy` (contenente solo il JRE 21, privo di Maven e compilatore JDK), imposta la directory di lavoro `/app` e la variabile `PORT=8082`.
2. **Copia dell'artefatto compilato:** Copia esclusivamente il file JAR già compilato e testato (`target/RevHire_WebApplication-1.0.0.jar`) rinominandolo `app.jar`.
3. **Esecuzione non-root e avvio:** Crea l'utente dedicato privo di privilegi `appuser`, gli assegna la proprietà di `/app`, cambia contesto con `USER appuser`, documenta la porta `8082` (`EXPOSE 8082`) e avvia la JVM in forma `exec` (`CMD ["java", "-jar", "app.jar"]`).

### 2.2 `docker-compose.yml`

Il file `docker-compose.yml` coordina lo stack multi-container su una rete virtuale isolata (`revhire-network`):

1. **Servizio Database (`db`):** Avvia Oracle Database da `gvenzl/oracle-free:23-slim-faststart` con `restart: on-failure`. Le credenziali provengono da un file locale `.env` escluso dal versionamento. La porta `1521` non è pubblicata sull'host ma resta accessibile solo nella rete interna.
2. **Persistenza dei dati:** Il volume nominato `oracle_data` è montato su `/opt/oracle/oradata`, preservando i dati anche dopo `docker compose down` / `up`.
3. **Healthcheck e sincronizzazione:** `db` definisce un `healthcheck` tramite `healthcheck.sh` (ogni 15s); `web` usa `depends_on` con `condition: service_healthy`, attendendo che Oracle sia operativo prima di avviare Spring Boot.
4. **Servizio Applicazione (`web`):** Costruisce l'immagine dal `Dockerfile` locale, espone `8082:8082`, imposta `restart: on-failure` e sovrascrive a runtime le proprietà JDBC (`SPRING_DATASOURCE_URL` puntato all'hostname interno `db` e `SPRING_JPA_HIBERNATE_DDL_AUTO=update`).

---

## 3. Anti-pattern evitati e affidabilità

1. **Versioni esplicite (no `:latest`).** Tutte le immagini (`eclipse-temurin:21-jre-jammy`, `gvenzl/oracle-free:23-slim-faststart`) fissano tag precisi per garantire determinismo e riproducibilità.
2. **Esecuzione senza privilegi di `root`.** Il container applicativo crea l'utente dedicato `appuser` e imposta `USER appuser`, rispettando il principio del minimo privilegio.
3. **Nessun segreto hardcodato.** Password e credenziali sono iniettate a runtime via variabili d'ambiente da un file `.env` escluso tramite `.gitignore`.
4. **Uso esclusivo di `COPY` al posto di `ADD`.** Evita comportamenti impliciti come estrazione automatica di archivi o fetch da URL remoti.
5. **Immagine minimale e forma `exec`.** L'assenza di Maven e JDK riduce dimensione e superficie d'attacco dell'immagine; `CMD ["java", "-jar", "app.jar"]` mantiene la JVM come `PID 1` per gestire correttamente i segnali di arresto.
6. **Tolleranza ai guasti e persistenza.** `healthcheck` + `depends_on: condition: service_healthy` evitano avvii prematuri; `restart: on-failure` garantisce il ripristino automatico; il volume `oracle_data` conserva i dati e l'assenza di `ports` su `1521` isola il database dall'esterno.

---

## 4. Pipeline di build e rilascio (`.github/workflows/docker-build.yml`)

Il workflow automatizza la creazione e pubblicazione dell'immagine Docker su DockerHub su runner `ubuntu-latest`:

1. **Trigger condizionato (`workflow_run`):** Si attiva al completamento (`types: [completed]`) della `CI/CD Pipeline` sul branch `main` e solo se l'esito è positivo (`if: github.event.workflow_run.conclusion == 'success'`), oltre a supportare l'avvio manuale (`workflow_dispatch`).
2. **Checkout e recupero artefatto CI:** Scarica il repository (`actions/checkout@v5`) e recupera tramite `actions/download-artifact@v4` il JAR (`revhire-<SHA>`) già compilato e testato dalla `CI/CD Pipeline`, salvandolo in `target/` ed evitando una seconda compilazione.
3. **Buildx e autenticazione sicura:** Configura Docker Buildx (`docker/setup-buildx-action@v3`) ed effettua il login su DockerHub tramite `docker/login-action@v3` leggendo i GitHub Secrets `DOCKER_USERNAME` e `DOCKER_PASSWORD` (Personal Access Token).
4. **Metadati e tag:** `docker/metadata-action@v5` genera automaticamente i tag `latest` e `v1.0` e le label OCI per il repository `${{ secrets.DOCKER_USERNAME }}/revhire-webapplication`.
5. **Build multi-piattaforma e push:** `docker/build-push-action@v6` costruisce l'immagine per `linux/amd64` e `linux/arm64` impacchettando l'artefatto della CI, la pubblica su DockerHub (`push: true`) e sfrutta la cache di GitHub Actions (`cache-from: type=gha`, `cache-to: type=gha,mode=max`).
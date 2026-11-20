<p align="center"> <img src="https://capsule-render.vercel.app/api?type=waving&color=0:1e3c72,100:2a5298&height=200&section=header&text=REVHIRE%20%E2%80%93%20SD&fontSize=45&fontColor=ffffff&animation=fadeIn&fontAlignY=35"/> </p> <p align="center"> <b>🧪 RevHire WebApplication – Fork per il corso di Software Dependability</b><br> <i>Università degli Studi di Salerno</i> </p> <p align="center"> <a href="https://github.com/RevHire-Team/RevHire_WebApplication"> <img src="https://img.shields.io/badge/Progetto%20originale-RevHire%20Team-black?style=for-the-badge&logo=github"/> </a> <a href="https://github.com/alessiagatto/-SD-RevHire_WebApplication/actions/workflows/ci.yml"> <img src="https://img.shields.io/github/actions/workflow/status/alessiagatto/-SD-RevHire_WebApplication/ci.yml?branch=main&style=for-the-badge&label=CI&logo=githubactions&logoColor=white"/> </a> <img src="https://img.shields.io/badge/Corso-Software%20Dependability-blue?style=for-the-badge"/> </p>

---

# 🛍️ Cos'è questa repository

Questa repository è un **fork** di [RevHire WebApplication](https://github.com/RevHire-Team/RevHire_WebApplication) ed è usata come caso di studio per il corso di **Software Dependability** dell'**Università degli Studi di Salerno**.

RevHire è un portale di ricerca del lavoro che mette in contatto **candidati** e **aziende**: i candidati cercano offerte e si candidano, le aziende pubblicano annunci e gestiscono le candidature.

> 🙏 Il progetto è opera del team [RevHire-Team](https://github.com/RevHire-Team). Per la descrizione completa si rimanda al loro [README ufficiale](https://github.com/RevHire-Team/RevHire_WebApplication/blob/main/README.md).

---

# 🚀 Tecnologie

<p align="center">
  <img src="https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white"/>
  <img src="https://img.shields.io/badge/Oracle%20DB-F80000?style=for-the-badge&logo=oracle&logoColor=white"/>
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"/>
</p>

---

# 🧪 Laboratori di Dependability

- ✅ **Lab 1** – CI workflow → [CI.md](documentation/CI.md)
- ✅ **Lab 2** – Docker e Docker compose → [Docker.md](documentation/Docker.md)
- ✅ **Lab 3** – Unit Testing con JUnit e Mockito → [Testing.md](documentation/Testing.md)
- ✅ **Lab 4** – Coverage e Mutation test → [Coverage.md](documentation/coverage.md) e [Mutation.md](documentation/mutation.md)



---

# ⚙️ Installazione

**Requisiti:** Git e JDK 21. Maven non va installato: il repository include il Maven Wrapper (`mvnw`).

1. 📥 Clona il repository:

   ```bash
   git clone https://github.com/alessiagatto/-SD-RevHire_WebApplication.git
   cd -SD-RevHire_WebApplication
   ```

2. 🗄️ Configura il database. L'applicazione originale usa **Oracle DB**: imposta URL, utente e password nel file di configurazione Spring in `src/main/resources/`.

3. 🧪 Compila ed esegui i test (su Windows usa `mvnw.cmd` al posto di `./mvnw`):

   ```bash
   ./mvnw clean test -Dspring.profiles.active=test
   ```

4. ▶️ Genera il JAR e avvia l'applicazione:

   ```bash
   ./mvnw package -DskipTests
   java -jar target/RevHire_WebApplication-1.0.0.jar
   ```

---

# 👥 Team

<p align="center">
  <a href="https://github.com/Boneelli"><b>Alessandro Bonelli</b></a> •
  <a href="https://github.com/alessiagatto"><b>Alessia Gatto</b></a> •
  <a href="https://github.com/elisapic13"><b>Elisa Picilli</b></a>
</p>

---

# 📂 Informazioni

- 🔗 Progetto originale: https://github.com/RevHire-Team/RevHire_WebApplication
- 👨‍💻 Sviluppatori originali: https://github.com/RevHire-Team
- 🏫 Corso: Software Dependability – Università degli Studi di Salerno

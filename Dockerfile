# --- STADIO 1: Build (con Maven e JDK 21) ---
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# Copia solo il pom.xml e scarica le dipendenze (sfrutta la cache di Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia il sorgente e compila il JAR
COPY src ./src
RUN mvn clean package -DskipTests

# --- STADIO 2: Runtime (solo JRE 21, leggero e sicuro) ---
FROM eclipse-temurin:21-jre-jammy
ENV PORT=8082
WORKDIR /app

# Copia solo il JAR compilato dallo stadio precedente
COPY --from=builder /build/target/RevHire_WebApplication-1.0.0.jar app.jar

# Sicurezza: esecuzione con utente non-root
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
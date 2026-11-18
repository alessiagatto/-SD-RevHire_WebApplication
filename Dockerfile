FROM eclipse-temurin:21-jre-jammy
ENV PORT=8082
WORKDIR /app

# Copia il JAR già compilato dalla CI (o in locale con mvn package)
COPY target/RevHire_WebApplication-1.0.0.jar app.jar

# Sicurezza: esecuzione con utente non-root
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
FROM eclipse-temurin@sha256:2d5d5e9b533342a565907c84f47a2ec298c40cde7b13a3ca85d4950fa456a641 AS build
WORKDIR /source/backend
COPY backend/ ./
RUN chmod +x gradlew && ./gradlew --no-daemon --console=plain bootJar -x test
COPY infra/local-app/Health.java /health/Health.java
RUN javac -d /health /health/Health.java

FROM eclipse-temurin@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f
WORKDIR /app
COPY --from=build --chown=10001:10001 /source/backend/build/libs/concurso-simulator-0.1.0-SNAPSHOT.jar /app/application.jar
COPY --from=build --chown=10001:10001 /health/Health.class /app/health/Health.class
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 CMD ["java", "-cp", "/app/health", "Health"]
ENTRYPOINT ["java", "-jar", "/app/application.jar"]

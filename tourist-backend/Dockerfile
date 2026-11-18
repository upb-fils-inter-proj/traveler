# The build context is the repository root, because the build reads api/internal-api.yaml:
#   docker build -f tourist-backend/Dockerfile -t traveler/tourist-backend .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY api ./api
COPY tourist-backend ./tourist-backend
WORKDIR /workspace/tourist-backend
RUN mvn -B -ntp -Dmaven.test.skip=true -Dspotless.check.skip=true package

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/tourist-backend/target/tourist-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

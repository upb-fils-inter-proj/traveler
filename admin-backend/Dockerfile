# The build context is the repository root, because the build reads api/internal-api.yaml:
#   docker build -f admin-backend/Dockerfile -t traveler/admin-backend .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY api ./api
COPY admin-backend ./admin-backend
WORKDIR /workspace/admin-backend
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -Dspotless.check.skip=true package

FROM eclipse-temurin:24-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/admin-backend/target/admin-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

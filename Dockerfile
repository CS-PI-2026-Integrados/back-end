FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system sicape && useradd --system --gid sicape sicape \
    && mkdir -p /app/storage && chown sicape:sicape /app/storage
COPY --from=build /build/target/sicape-api-0.0.1-SNAPSHOT.jar /app/app.jar
USER sicape
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

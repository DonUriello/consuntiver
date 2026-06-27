# ---- Stage 1: build del jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Prima le dipendenze (cache di Docker), poi il codice
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Stage 2: immagine di runtime, leggera ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

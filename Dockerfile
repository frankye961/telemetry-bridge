# ---------- BUILD STAGE ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

# Copy sources and build
COPY src src
RUN ./mvnw -q -DskipTests package

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Spring Boot reactive bridge port
EXPOSE 8080

# Optional JVM tuning
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

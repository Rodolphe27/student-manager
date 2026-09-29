# Stage 1 — build the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2 — run the JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Size the heap from the container's memory limit (e.g. Render's 512 MB instances)
# instead of the host's RAM, leaving headroom for metaspace and threads.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# The app always listens on 5030 — point the platform at it (Render: PORT=5030).
EXPOSE 5030
ENTRYPOINT ["java", "-jar", "app.jar"]

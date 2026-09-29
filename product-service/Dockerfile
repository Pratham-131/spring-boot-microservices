FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --system --create-home --uid 10001 app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/product-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 4002
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
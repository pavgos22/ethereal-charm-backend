FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8881
ENTRYPOINT ["java","-jar","app.jar"]
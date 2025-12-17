FROM eclipse-temurin:17-jre
WORKDIR /app

COPY build/libs/Green-University-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8881
ENTRYPOINT ["java","-jar","/app/app.jar"]

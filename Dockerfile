FROM eclipse-temurin:latest AS Builder
WORKDIR /LythmServer
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:latest
COPY --from=Builder /LythmServer/build/libs/*.jar LythmServer.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/LythmServer.jar"]
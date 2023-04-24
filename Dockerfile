FROM eclipse-temurin:latest AS Builder
WORKDIR /LythmServer
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:latest
ENV HOST=
ENV PORT=
COPY --from=Builder /LythmServer/build/libs/*.jar LythmServer.jar
EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "-Dsocket-server.host=${HOST}", "-Dsocket-server.port=${PORT}", "/LythmServer.jar"]

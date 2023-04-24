FROM eclipse-temurin:latest AS Builder
WORKDIR /LythmServer
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

FROM eclipse-temurin:latest
ENV HOST=
ENV PORT=
ENV SSL_ENABLED=
ENV KEY_STORE_PATH=
ENV KEY_STORE_PASSWORD=
COPY --from=Builder /LythmServer/build/libs/*.jar LythmServer.jar
EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "-Dsocket-server.host=${HOST}", "-Dsocket-server.port=${PORT}", "-Dserver.ssl.enabled=${SSL_ENABLED}", "-Dserver.ssl.key-store=${KEY_STORE_PATH}", "-Dserver.ssl.key-store-password=${KEY_STORE_PASSWORD}", "/LythmServer.jar"]

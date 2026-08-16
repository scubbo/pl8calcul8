# Build from the repository root:
#   docker build -f server/Dockerfile -t pl8calcul8-server .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts gradle.properties gradlew ./
COPY gradle/ gradle/
COPY shared/ shared/
COPY server/ server/
RUN ./gradlew --no-daemon :server:installDist

FROM eclipse-temurin:17-jre
COPY --from=build /src/server/build/install/server /opt/server
RUN mkdir /data && chown 1000 /data
USER 1000
ENV DATA_DIR=/data
EXPOSE 8080
ENTRYPOINT ["/opt/server/bin/server"]

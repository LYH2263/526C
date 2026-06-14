FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dmaven.wagon.http.pool=false"
COPY settings.xml .
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s settings.xml -B dependency:go-offline -Dmaven.test.skip=true

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dmaven.wagon.http.pool=false"
COPY --from=deps /root/.m2 /root/.m2
COPY settings.xml .
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s settings.xml -B package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre AS runtime
RUN groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R app:app /app
USER app
ENV LANG C.UTF-8
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8526
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar app.jar"]

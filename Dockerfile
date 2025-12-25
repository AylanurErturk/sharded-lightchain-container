FROM eclipse-temurin:21-jre

RUN mkdir /app
COPY target/sharded-lightchain-container-*-jar-with-dependencies.jar /app
COPY src/main/resources/log4j.properties /app
COPY simulation.config /app


WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-Xss512k -Xms80g -Xmx80g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -XX:+ExitOnOutOfMemoryError"

# NOTE: I hard coded the version here
# TODO: Make the filename below change dynamically with new versions
CMD ["java", "-cp", "sharded-lightchain-container-0.0.1-SNAPSHOT-jar-with-dependencies.jar", "simulation.SimulationDriver"]

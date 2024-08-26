FROM openjdk:17-alpine
VOLUME /tmp
ARG JAVA_OPTS
ENV JAVA_OPTS=$JAVA_OPTS
COPY target/data-run-api-0.0.1-SNAPSHOT.jar datarunapi.jar
EXPOSE 8080
# ENTRYPOINT exec java $JAVA_OPTS -jar datarunapi.jar
# For Spring-Boot project, use the entrypoint below to reduce Tomcat startup time.
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar datarunapi.jar

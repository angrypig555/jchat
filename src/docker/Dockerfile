FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY jchat.jar app.jar
COPY entrypoint.sh entrypoint.sh

ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
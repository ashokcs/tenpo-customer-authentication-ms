FROM openjdk:8-alpine

ADD https://s3.amazonaws.com/mine.metadata.dev/postgres/root.crt /root/.postgresql/
ADD target/*.jar /opt/ms/customer-authentication-ms.jar
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/ms/customer-authentication-ms.jar
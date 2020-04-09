FROM openjdk:8-alpine

ADD postgres.crt /root/.postgresql/root.crt
ADD target/*.jar /opt/ms/customer-authentication-ms.jar
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/ms/customer-authentication-ms.jar
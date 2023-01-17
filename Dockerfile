FROM azul/zulu-openjdk:17.0.4.1-17.36.17
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
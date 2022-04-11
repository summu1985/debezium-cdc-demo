FROM registry.access.redhat.com/ubi8/openjdk-11
USER root:root
COPY target/debezium-cdc-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]

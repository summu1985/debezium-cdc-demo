FROM registry.redhat.io/amq7/amq-streams-kafka-24-rhel7:1.4.0
USER root:root
RUN mkdir -p /opt/kafka/plugins/debezium
COPY ./plugins/debezium-connector-mysql/ /opt/kafka/plugins/debezium/
USER 1001

# debezium-cdc-demo

1. Install AMQ Streams operator on OCP

2. Install Kafka cluster

Kafka - 1 node zookeeper and broker cluster and ephemeral

```kafka.yml

apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: my-cluster
spec:
  kafka:
    config:
      num.partitions: 1
      default.replication.factor: 1
      offsets.topic.replication.factor: 1
    replicas: 1
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    storage:
      type: ephemeral
  zookeeper:
    replicas: 1
    storage:
      type: ephemeral
  entityOperator:
    topicOperator: {}
```

Wait for zookeeper cluster, kafka cluster and cluster entity operator pods to be ready.

Verify that the cluster is ready by the command

```echo "Hello world" | oc exec -i -c kafka my-cluster-kafka-0 -- /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test```

You can ignore the error message like so : `[2022-06-28 10:33:09,905] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 1 : {test=LEADER_NOT_AVAILABLE} (org.
apache.kafka.clients.NetworkClient)`

Now read back the message produced in earlier step.

`oc exec -c kafka my-cluster-kafka-0 -- /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test
 --from-beginning --max-messages 1`

Expected response is so : 
`Hello world
Processed a total of 1 messages`

This means that the cluster is up.

3. Install Kafka Connect (source) cluster to support needed DBs

Use pre-built image for Kafka connect with support for all drivers

```prebuilt-kafkaconnector.yaml

apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnect
metadata:
  annotations:
    strimzi.io/use-connector-resources: 'true'
  name: 'my-connect-cluster'
spec:
  bootstrapServers: 'my-cluster-kafka-bootstrap:9092'
  config:
    config.storage.topic: 'debezium-cluster-configs'
    group.id: 'debezium-cluster'
    offset.storage.topic: 'debezium-cluster-offsets'
    status.storage.topic: 'debezium-cluster-status'
    config.storage.replication.factor: 1
    offset.storage.replication.factor: 1
    status.storage.replication.factor: 1
  image: 'quay.io/redhatintegration/rhi-cdc-connect:2021-Q1'
  jvmOptions:
    gcLoggingEnabled: false
  replicas: 1
  resources:
    limits:
      memory: 1Gi
    requests:
      memory: 1Gi
```

`oc apply -f prebuilt-kafkaconnector.yaml`

OR if you want to install a kafka connect image with support for only the DB you need,
you can use the below yaml manifest. 

__USE EITHER THE ABOVE KAFKA CONNECT OR THE BELOW ONE. DO NOT USE BOTH__

```kafka_connect.yaml

apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnect
metadata:
  name: debezium-kafka-source-connect-cluster
  annotations:
    strimzi.io/use-connector-resources: "true"
spec:
  version: 3.00
  build:
    output:
      type: imagestream
      image: debezium-streams-connect:latest
    plugins:
      - name: debezium-connectors
        artifacts:
          - type: zip
            url: https://maven.repository.redhat.com/ga/io/debezium/debezium-connector-sqlserver/1.7.2.Final-redhat-00003/debezium-connector-sqlserver-1.7.2.Final-redhat-00003-plugin.zip
          - type: zip
            url: https://maven.repository.redhat.com/ga/io/apicurio/apicurio-registry-distro-connect-converter/2.0.3.Final-redhat-00002/apicurio-registry-distro-connect-converter-2.0.3.Final-redhat-00002.zip
          - type: zip
            url: https://maven.repository.redhat.com/ga/io/debezium/debezium-scripting/1.7.2.Final-redhat-00003/debezium-scripting-1.7.2.Final-redhat-00003.zip
          - type: zip
            url: https://maven.repository.redhat.com/ga/io/debezium/debezium-connector-mysql/1.7.2.Final-redhat-00003/debezium-connector-mysql-1.7.2.Final-redhat-00003-plugin.zip

  bootstrapServers: my-cluster-kafka-bootstrap:9093

  ```


4. Create source DB and table

Login to OCP developer mode and deploy a MySQL DB with service name as mysql.
Once the MySQL DB is up, login to its terminal using root user (password not needed
when logging in through pod terminal).

## MYSQL
```
mysql -uroot 

create database inventory;

create USER 'debezium'@'%' IDENTIFIED WITH mysql_native_password BY 'debezium'; 

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium';

GRANT ALL PRIVILEGES ON inventory.* TO 'debezium'@'%';

FLUSH PRIVILEGES;

use inventory;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)  ENGINE=INNODB;
```

5. Install Kafka Connector for specific DB

## MySQL connector
```mysql-source-connector.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
metadata:
  name: mysql-source-connector
  labels:
    strimzi.io/cluster: my-connect-cluster
spec:
  class: io.debezium.connector.mysql.MySqlConnector
  tasksMax: 1
  config:
    database.server.name: mysql-inventory
    database.hostname: mysql-source
    database.password: <your DB password>
    database.port: '3306'
    database.whitelist: inventory
    database.history.kafka.topic: schema-changes.inventory
    database.history.kafka.bootstrap.servers: 'my-cluster-kafka-bootstrap:9092'
    database.dbname: inventory
    database.user: debezium
    transforms: unwrap
    transforms.unwrap.delete.handling.mode: rewrite
    transforms.unwrap.drop.tombstones: false
    transforms.unwrap.type: io.debezium.transforms.ExtractNewRecordState
    transforms.unwrap.add.fields: op,table,db,source.ts_ms,source.db
    key.converter: org.apache.kafka.connect.json.JsonConverter
    key.converter.schemas.enable: false
    value.converter: org.apache.kafka.connect.json.JsonConverter
    value.converter.schemas.enable: false
```
## SQL server
```sqlserver-source-connector.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
metadata:
  labels:
    strimzi.io/cluster: my-connect-cluster
  name: sqlserver-source-connector
spec:
  class: io.debezium.connector.sqlserver.SqlServerConnector
  tasksMax: 1
  config:
    database.history.kafka.bootstrap.servers: 'my-cluster-kafka-bootstrap:9092'
    database.history.kafka.topic: schema-changes.azuresrcdb
    database.hostname: <you DB hostname>
    database.port: 1433
    database.user: <your DB username>
    database.password: <your DB password>
    database.dbname: azure-src-db
    database.server.name: sqlserver
    transforms: unwrap
    transforms.unwrap.delete.handling.mode: rewrite
    transforms.unwrap.drop.tombstones: false
    transforms.unwrap.type: io.debezium.transforms.ExtractNewRecordState
    transforms.unwrap.add.fields: op,table,db,source.ts_ms,source.db
    key.converter: org.apache.kafka.connect.json.JsonConverter
    key.converter.schemas.enable: false
    value.converter: org.apache.kafka.connect.json.JsonConverter
    value.converter.schemas.enable: false
```
6. Insert data in source table

## MYSQL

```
use inventory;
INSERT INTO customers (name) VALUES ('Sumit Mukherjee'); 
```

7. Install Kafdrop to see messages in kafka topics from browser (optional)

Go to add
select container image
configure the following
 - image : docker.io/obsidiandynamics/kafdrop:latest
 - application-name : cdc-demo
 - name : kafdrop
 - select DeploymentConfig
 - create a route - unchecked
 - Go to deployment options
   - Auto deploy when new Image is available - checked
   - Auto deploy when deployment configuration changes - checked
   - add an env variable : KAFKA_BROKERCONNECT = my-cluster-kafka-bootstrap:9092

Click create
Once pods are up, go to the kafdrop service, and edit the yaml to modify the port and target port to 9000
Go to Routes - > create route
Give the name as kafdrop
select service -> kafdrop
select port : 9000 -> 9000

Now navigate to the url of the route

The inserted data would appear in a topic named inventory.inventory.customers
If using SQL server, the data would appear in topic named sqlserver.dbo.customers

8. Install Camel-K connector from Operator Hub

9. Install kamel cli on your workstation to view kamelet logs

10. Create sink DB and table

Deploy a MySQL DB to act as sink DB and name the sql service as mysql-sink

## MYSQL
```
create database mysqlsinkdb;

create USER 'debezium'@'%' IDENTIFIED WITH mysql_native_password BY 'debezium'; 

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium';

GRANT ALL PRIVILEGES ON mysqlsinkdb.* TO 'debezium'@'%';

FLUSH PRIVILEGES;

use mysqlsinkdb;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at VARCHAR(255),
    insert_user VARCHAR(255),
    insert_timestamp VARCHAR(255),
    update_user VARCHAR(255),
    update_timestamp VARCHAR(255),
    src_db_name VARCHAR(255),
    src_table_name VARCHAR(255)
)  ENGINE=INNODB;
```

## SQL Server
```
create table customerssink ( customer_id int, name varchar(50), created_at varchar(100), insert_user varchar(100), insert_timestamp varchar(255), update_user varchar(50), update_timestamp varchar(255), src_db_name varchar(50), src_table_name varchar(50) CONSTRAINT "PK_Customers" PRIMARY KEY CLUSTERED ("customer_id") );
```

11. Install Kafka Topic to Sink DB connector kamelet binding - handles source DB inserts only

## My SQL
```mysql-sink-binding.yaml

apiVersion: camel.apache.org/v1alpha1
kind: KameletBinding
metadata:
  name: mysql-sink-binding
spec:
  source:
    ref:
      kind: KafkaTopic
      apiVersion: kafka.strimzi.io/v1beta1
      name: mysql-inventory.inventory.customers
  sink:
    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: mysql-sink
    properties:
      databaseName: mysqlsinkdb
      password: <your DB password>
      query: "INSERT INTO customers (customer_id, name, created_at, insert_user, insert_timestamp, update_user, update_timestamp, src_db_name, src_table_name) VALUES (:#customer_id,:#name,:#created_at, 'debezium', :#__source_ts_ms, 'debezium', :#__source_ts_ms, :#__db, :#__table)"
      serverName: mysql-sink
      username: debezium
```

## SQL server

```sqlserver-sink-binding.yml

apiVersion: camel.apache.org/v1alpha1
kind: KameletBinding
metadata:
  name: azuresql-to-azuresql-sink-binding
spec:
  source:
    ref:
      kind: KafkaTopic
      apiVersion: kafka.strimzi.io/v1beta1
      name: sqlserver.dbo.customers
  sink:
    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: sqlserver-sink
    properties:
      databaseName: azure-sink-db
      password: <your DB password>
      query: "INSERT INTO customerssink (customer_id, name, created_at, insert_user, insert_timestamp, src_db_name, src_table_name) VALUES (:#customer_id,:#name,:#created_at, 'debezium', :#__source_ts_ms, :#__db, :#__table)"
      serverName: <your DB hostname>
      username: <your DB username>

```

12. Create the kamelet binding 
`oc apply -f mysql-sink-binding.yaml`
or
`oc apply -f sqlserver-sink-binding.yml`

13. Check the status of kameletbindings

`oc get kameletbindings`

The output should be similar

```
bash-3.2$ oc get kameletbindings
NAME                                    PHASE   REPLICAS
azuresql-to-azuresql-sink-binding       Ready   1
azuresql-to-azuresql-sink-binding-new   Ready   1
sqlserver-sink-binding                  Ready   1
```
The PHASE can be Building and it may take some time to actually be ready.

14. Monitor the kamelet binding log

`kamel logs sqlserver-sink-binding`

15. Insert some new data in source table and the data should be populated in the sink DB.


create database mysqlsinkdb;

create USER 'debezium'@'%' IDENTIFIED WITH mysql_native_password BY 'debezium'; 

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium';

GRANT ALL PRIVILEGES ON mysqlsinkdb.* TO 'debezium'@'%';

FLUSH PRIVILEGES;

use mysqlsinkdb;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    insert_user VARCHAR(255),
    insert_timestamp TIMESTAMP,
    update_user VARCHAR(255),
    update_timestamp TIMESTAMP,
    src_db_name VARCHAR(255)
)  ENGINE=INNODB;

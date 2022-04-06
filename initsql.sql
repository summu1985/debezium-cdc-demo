create USER 'debezium'@'%' IDENTIFIED WITH mysql_native_password BY 'dbz'; 

GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT  ON *.* TO 'debezium';

create database inventory;

use inventory;

CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)  ENGINE=INNODB;

INSERT INTO customers (name) VALUES ('Sumit Mukherjee'); 

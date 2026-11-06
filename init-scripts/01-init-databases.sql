-- Automatically create isolated databases for each microservice
CREATE DATABASE auth_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;

-- Grant all privileges to the default user
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;

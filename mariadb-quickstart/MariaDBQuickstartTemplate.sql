-- MariaDB CoinAccount Quickstart Script
-- This script will perform the following actions:
-- 1) Create a user named "coinaccount" identified by a user-provided password.
-- 2) Create a database named "coinaccountdb" for the service.
-- 3) Grant all privileges for the new database to the new user.
CREATE USER coinaccount IDENTIFIED BY '{PASSWORD}';
CREATE DATABASE coinaccountdb;
GRANT ALL PRIVILEGES ON coinaccountdb.* TO coinaccount;
FLUSH PRIVILEGES;
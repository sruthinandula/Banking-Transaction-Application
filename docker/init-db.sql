-- Database-per-service: the event processor gets its own database.
-- (Runs only on the first start of a fresh Postgres volume.)
CREATE DATABASE eventsdb;

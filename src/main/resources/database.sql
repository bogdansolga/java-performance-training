-- run these commands before starting the project, in order to setup the database and the connecting user
CREATE USER performance_training_admin WITH PASSWORD 'performance_training_admin';

CREATE DATABASE performance_training;
GRANT ALL PRIVILEGES ON DATABASE performance_training TO performance_training_admin;

\c performance_training;
GRANT ALL ON SCHEMA public TO performance_training_admin;
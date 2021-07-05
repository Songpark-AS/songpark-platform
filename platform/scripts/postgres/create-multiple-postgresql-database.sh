#!/bin/bash

psql --username "$POSTGRES_USER" <<EOSQL
 CREATE DATABASE songparktest;
 ALTER DATABASE songparktest OWNER TO $POSTGRES_USER;
EOSQL

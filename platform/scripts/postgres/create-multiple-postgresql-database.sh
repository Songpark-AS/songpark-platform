#!/bin/bash

psql --username "$POSTGRES_USER" <<EOSQL
 CREATE DATABASE test;
 ALTER DATABASE test OWNER TO $POSTGRES_USER;
EOSQL

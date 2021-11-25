#!/bin/bash

DEPLOYMENT_DIR=deployment
PROJECT_DIR=platform
PROJECT_NAME=songpark-platform

echo "Copying configuration files"
cp $DEPLOYMENT_DIR/config.staging.edn $PROJECT_DIR/resources/config.edn

echo "Copying VERSION.git to resources folder"
cp VERSION.git $PROJECT_DIR/resources/VERSION.git

echo "Compiling $PROJECT_NAME"
(cd $PROJECT_DIR && lein uberjar)


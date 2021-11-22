#!/bin/bash

DEPLOYMENTDIR=deployment
BACKEND=platform
PROJECT_NAME=songpark-platform

echo "Copying configuration files"
cp $DEPLOYMENTDIR/config.staging.edn $BACKEND/resources/config.edn

echo "Copying VERSION.git to resources folder"
cp VERSION.git $BACKEND/resources/VERSION.git

echo "Compiling $PROJECT_NAME"
(cd $BACKEND && lein uberjar)


#!/usr/bin/env bash

DEPLOYMENT_DIR=deployment
TEMPLATE_FILE=songpark-platform.template.yaml
YAML_FILE=songpark-platform.yaml
TAG=$1
VERSION=$2

echo "Copying template file"
cp $DEPLOYMENT_DIR/$TEMPLATE_FILE $DEPLOYMENT_DIR/$YAML_FILE

echo "Updating variables"
sed -i '' 's/VAR__VERSION/'"$VERSION"'/g' $DEPLOYMENT_DIR/$YAML_FILE
sed -i '' 's/VAR__TAG/'"$TAG"'/g' $DEPLOYMENT_DIR/$YAML_FILE

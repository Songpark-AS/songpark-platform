#!/usr/bin/env bash

DEPLOYMENT_DIR=deployment
TEMPLATE_FILE=songpark-platform.template.yaml
YAML_FILE=songpark-platform.yaml
TAG=$1
VERSION=$2
NAMESPACE=songpark-$TAG
HOSTNAME=spp-$TAG.inonit.no

echo "Copying template file"
cp $DEPLOYMENT_DIR/$TEMPLATE_FILE $DEPLOYMENT_DIR/$YAML_FILE

echo "Updating variables"
sed -i -e 's/VAR__VERSION/'"$VERSION"'/g' $DEPLOYMENT_DIR/$YAML_FILE
sed -i -e 's/VAR__TAG/'"$TAG"'/g' $DEPLOYMENT_DIR/$YAML_FILE
sed -i -e 's/VAR__NAMESPACE/'"$NAMESPACE"'/g' $DEPLOYMENT_DIR/$YAML_FILE
sed -i -e 's/VAR__HOSTNAME/'"$HOSTNAME"'/g' $DEPLOYMENT_DIR/$YAML_FILE

sed -i -e 's/spp-production\.inonit\.no/'"spp.inonit.no"'/g' $DEPLOYMENT_DIR/$YAML_FILE

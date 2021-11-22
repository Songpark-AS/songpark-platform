#!/usr/bin/env bash

DEPLOYMENTDIR=deployment
TEMPLATEFILE=songpark-platform.template.yaml
YMLFILE=songpark-platform.yaml
TAG=$1
VERSION=$2

echo "Copying template file"
cp $DEPLOYMENTDIR/$TEMPLATEFILE $DEPLOYMENTDIR/$YMLFILE

echo "Updating variables"
sed -i '' 's/VAR__VERSION/'"$VERSION"'/g' $DEPLOYMENTDIR/$YMLFILE
sed -i '' 's/VAR__TAG/'"$TAG"'/g' $DEPLOYMENTDIR/$YMLFILE

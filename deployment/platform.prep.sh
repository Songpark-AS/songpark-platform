#!/usr/bin/env bash

DEPLOYMENT_DIR=deployment
PROJECT_DIR=platform

scp $PROJECT_DIR/target/platform.jar root@platform.songpark.com:/opt/platform/platform-new.jar
scp $DEPLOYMENT_DIR/config.staging.edn root@platform.songpark.com:/opt/platform/config-new.edn

ssh root@platform.songpark.com systemctl stop songpark.platform
ssh root@platform.songpark.com mv /opt/platform/platform-new.jar /opt/platform/platform.jar
ssh root@platform.songpark.com mv /opt/platform/config-new.edn /opt/platform/config.edn
ssh root@platform.songpark.com systemctl start songpark.platform

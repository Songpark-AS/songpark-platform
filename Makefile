#!/usr/bin/env make -f

export AWS=523581807964.dkr.ecr.eu-central-1.amazonaws.com
export VERSION ?=$(shell git rev-parse HEAD)
export COMPANY_NAME=inonit
export PROJECT_NAME=songpark-platform
export LOCAL_BUILD_NAME=$(COMPANY_NAME)/$(PROJECT_NAME)
export AWS_BUILD_NAME=$(AWS)/$(PROJECT_NAME)

DEPLOYMENTDIR=deployment


$(shell git rev-parse HEAD > VERSION.git)

default:
	@echo "Check commands"

# AWS
aws-login:
	@echo "Logging in to AWS"
	@(aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $(AWS))


# pushing

push-staging: aws-login
	@echo "Pushing staging image to AWS ECR"
	docker push $(AWS_BUILD_NAME):staging
	docker push $(AWS_BUILD_NAME):$(VERSION)
	docker push $(AWS_BUILD_NAME):latest

push-production: aws-login
	@echo "Pushing production image to AWS ECR"
	docker push $(AWS_BUILD_NAME):production
	docker push $(AWS_BUILD_NAME):$(VERSION)
	docker push $(AWS_BUILD_NAME):latest

push-debug: aws-login
	@echo "Pushing debug image to AWS ECR"
	docker push $(AWS_BUILD_NAME):debug
	docker push $(AWS_BUILD_NAME):$(VERSION)
	docker push $(AWS_BUILD_NAME):latest

push-dev: aws-login
	@echo "Pushing dev image to AWS ECR"
	docker push $(AWS_BUILD_NAME):dev
	docker push $(AWS_BUILD_NAME):$(VERSION)
	docker push $(AWS_BUILD_NAME):latest


# builds

build-staging: prebuild-staging docker-build docker-tag-version docker-tag-staging
build-debug: prebuild-debug docker-build docker-tag-version docker-tag-debug
build-dev: prebuild-dev docker-build docker-tag-version docker-tag-dev
build-production: prebuild-production docker-build docker-tag-version docker-tag-production


# 

prebuild-staging:
	@echo "Prebuilding staging"
	sh $(DEPLOYMENTDIR)/prebuild.staging.sh

prebuild-production:
	@echo "Prebuilding production"
	sh $(DEPLOYMENTDIR)/prebuild.production.sh

prebuild-dev:
	@echo "Prebuilding dev"
	sh $(DEPLOYMENTDIR)/prebuild.dev.sh

prebuild-debug:
	@echo "Prebuilding debug"
	sh $(DEPLOYMENTDIR)/prebuild.debug.sh

# Docker tagging

docker-tag-version:
	@echo "Tagging AWS docker image to version"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):$(VERSION)

docker-tag-latest:
	@echo "Tagging docker image to latest"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):latest
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(LOCAL_BUILD_NAME):latest

docker-tag-staging:
	@echo "Tagging docker image to staging"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):staging
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(LOCAL_BUILD_NAME):staging

docker-tag-debug:
	@echo "Tagging docker image to debug"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):debug
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(LOCAL_BUILD_NAME):debug

docker-tag-dev:
	@echo "Tagging docker image to dev"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):dev
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(LOCAL_BUILD_NAME):dev

docker-tag-production:
	@echo "Tagging docker image to production"
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(AWS_BUILD_NAME):production
	docker tag $(LOCAL_BUILD_NAME):$(VERSION) $(LOCAL_BUILD_NAME):production


docker-clean-and-build:
	@echo "Building dockerfile"
	docker build --no-cache \
		-f $(DEPLOYMENTDIR)/Dockerfile \
		-t $(LOCAL_BUILD_NAME):$(VERSION) \
		.

docker-build:
	@echo "Building dockerfile"
	docker build \
		-f $(DEPLOYMENTDIR)/Dockerfile \
		-t $(LOCAL_BUILD_NAME):$(VERSION) \
		.

kube-prep-staging:
	@echo "Preparing k8s deployment file"
	sh $(DEPLOYMENTDIR)/k8s.prep.sh staging $(VERSION)

kube-remove:
	@echo "Deleting from k8s"
	kubectl delete -f $(DEPLOYMENTDIR)/$(PROJECT_NAME).yaml

kube-deploy:
	@echo @echo "Deploying to k8s"
	kubectl apply -f $(DEPLOYMENTDIR)/$(PROJECT_NAME).yaml

NAME:=songpark-platform
TAG:=latest

all: build push

aws-pw: 
	aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 523581807964.dkr.ecr.eu-central-1.amazonaws.com

build:
	docker build -t ${NAME}:${TAG} ../.

tag: build aws-pw
	docker tag ${NAME}:${TAG} 523581807964.dkr.ecr.eu-central-1.amazonaws.com/${NAME}:${TAG}

push: tag
	docker push 523581807964.dkr.ecr.eu-central-1.amazonaws.com/${NAME}:${TAG}

remove:
	kubectl delete -f ${NAME}.yaml

deploy: tag
	kubectl apply -f ${NAME}.yaml

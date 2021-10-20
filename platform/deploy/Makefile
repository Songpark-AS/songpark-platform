all: build push

aws-pw: 
	aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 523581807964.dkr.ecr.eu-central-1.amazonaws.com

build:
	docker build -t songpark-platform:latest ../.

tag: build aws-pw
	docker tag songpark-platform:latest 523581807964.dkr.ecr.eu-central-1.amazonaws.com/songpark-platform:latest

push: tag
	docker push 523581807964.dkr.ecr.eu-central-1.amazonaws.com/songpark-platform:latest

remove:
	kubectl delete -f songpark-platform.yaml

deploy: tag
	kubectl apply -f songpark-platform.yaml


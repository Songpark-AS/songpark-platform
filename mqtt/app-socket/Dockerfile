FROM python:3.7.2-slim

# install netcat
RUN apt-get update && \
    apt-get -y install netcat && \
    apt-get clean

# set working directory
WORKDIR /usr/src/app

# add and install requirements
RUN pip install websocket-client

# add app
COPY . /usr/src/app

# run server
CMD ["python", "app.py"]



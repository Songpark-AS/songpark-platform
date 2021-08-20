# TX/PY

This Branch contains work in progress on the development of a simulation of the TP-mobile app communication. As a brief the Songpark project is made up of the Teleporter (which is the central component of this whole project and we here by refer to that as the TP). Inside the TP we have the infamous CS7 (CantaviStreamer 7) which is a C app which handles everything on the TP. In an attempt to designthe platform we have design the TPX/Py which is meant to be a python script that interfaces CS7 and the TP to the mobile app and to carryout this communication between the two we have selected MQTT and Sockets.

In this directory /mqtt we have packaged up a simple implementation of the solution, this implementation is based on docker and we shall be using docker-compose to manage the various containers what we shall have to start. There are four containers and each container handles a specific part of the entire deployment.
## Mosquitto

this is the main container that will serve as the server for communication, we have used the Eclipse Mosquitto image as it is compatible with ARM32, ARM64 and x86 additionally it offers a very simplified implementation process that is based on a config file. The Config file is allows us to instruct the mosquitto server to carryout certain operations or to behave in a certain prefared way. Currently mosquitto is running a MQTT server on port 1883 and a raw socket server on port 8000. We have setup docker to map your local ports to the corresponding ports to the docker network allowing you to access both services on localhost.

### How to use Mosquitto server

1. Mosquitto is currently set to annonymous access which means that anyone who has access to the network can access the server. For security client authentication should be used; and this can be configured in the mosquitto.conf file 
2. The Mosquitto.conf file is located in the /mosquitto directory and you can edit this file to expose more functionality. it is possible to use a different .conf file with a different name just make use you point to that file in the docker-compose.yml file.

## MQTT Client App

the directory /app features an example MQTT client application which is meant to be a test application, this application is actually meant to be the actual TPX/py which will run alongside CS7 passing requests from the queue and to CS7. This app is still work in progress.

## Socker Client App

the directory /app-socket features an example Socket client application which is meant to be a test application, this application should be a socket based version of TPX/py which will run alongside CS7 passing requests from the queue and to CS7. This app is still work in progress.

## CS7

the directory /cs7 contains the actual C application, however this is a scripted down version of the application solely for the purposes of TP simulation via containerization. Feel free to test out some scripts using:

1. Run `docker build -t cs7 . ` to build cs7 
2. Run `docker run -it cs7 0 0 0 0 0 0` to get the interactive prompt to use cs7

### Commands to run 
1. `vgg <int>` increases and decreases volume wrt int
2. `exit` exits the interactive prompt 
3. `reboot now` reboots the entire engine 

## Running the Project 
To run the MQTT server and all apps ensure that you have docker running then run the following command:

`docker-compose up`

in the event of any changes run:

`docker-compose up --build`

to run specific commands within a specific container run 

`docker-compose exec <container-name> <command>`


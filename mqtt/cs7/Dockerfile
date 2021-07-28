FROM mitchallen/pi-cross-compile

USER root

RUN apt-get update && \
    apt-get -y install gcc-arm-linux-gnueabihf

COPY . /app
WORKDIR /app/

# variables for compilation process
ARG CROSS_COMPILE?=arm-linux-gnueabihf-
ARG APP_NAME=app
ARG CC=arm-linux-gnueabihf-gcc
ARG CCSIZE=arm-linux-gnueabihf-size
ARG IDIR=/include
ARG CFLAGS=-I$IDIR --std=gnu99 -O0
ARG LIBS=-lrt -lpthread
ARG OBJS=adau1761_controller.o zedboardOLED.o reg_io.o udpclient.o stream_control.o volume_control.o filter_control.o ui_control.o app_main.o

# compilation process begins now
RUN $CC $CFLAGS adau1761_controller.o zedboardOLED.o reg_io.o udpclient.o stream_control.o volume_control.o filter_control.o ui_control.o app_main.o -lrt -lpthread -o app
RUN ls 
CMD ["qemu-arm-static"]
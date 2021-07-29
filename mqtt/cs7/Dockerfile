FROM gcc:4.9
COPY . /app
WORKDIR /app/
RUN gcc -Iinclude --std=gnu99 adau1761_controller.c zedboardOLED.c reg_io.c udpclient.c stream_control.c volume_control.c filter_control.c ui_control.c app_main.c -lrt -lpthread -o app
CMD ["./app"]
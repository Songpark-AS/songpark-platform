FROM gcc:4.9
COPY . /app
WORKDIR /app/
RUN gcc ui_control.c -o app --std=gnu99 -O0
CMD ["./app"]
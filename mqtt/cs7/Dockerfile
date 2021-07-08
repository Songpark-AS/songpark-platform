FROM gcc:4.9
COPY . /app
WORKDIR /app/
RUN gcc -o app stream_control.c
CMD ["./app"]
FROM openjdk:11 AS songpark-platform
RUN mkdir /app
WORKDIR /app
COPY VERSION.git /app
COPY target/platform.jar /app
CMD java -server -jar platform.jar

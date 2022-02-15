FROM openjdk:11 AS songpark-platform
RUN mkdir /app
WORKDIR /app
COPY VERSION.git /app
COPY inonit-signing-public.key /app
COPY apt_auth.conf /etc/apt/auth.conf
COPY platform/target/platform.jar /app

# Setup apt repo
RUN apt-get update
RUN apt-get install -y ca-certificates gnupg2
RUN apt-key add inonit-signing-public.key
RUN echo "deb https://nexus.inonit.no/repository/songpark-apt/ stable main" >> /etc/apt/sources.list.d/songpark.list
RUN apt-get update

CMD java -server -jar platform.jar

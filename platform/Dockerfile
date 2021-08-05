FROM openjdk:8u181-alpine3.8

WORKDIR /

# Replace <jar-file-name> with the name of the oupt jar found in the target dir
# to geneate the jar file compile the project first with lein uberjar
COPY target/platform.jar platform.jar
EXPOSE 3000

CMD java -jar platform.jar
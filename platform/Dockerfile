FROM openjdk:8u181-alpine3.8

WORKDIR /

# Replace <jar-file-name> with the name of the oupt jar found in the target dir
# to geneate the jar file compile the project first with lein uberjar
COPY target/<jar-file-name> <jar-file-name>
EXPOSE 3000

CMD java -jar <jar-file-name>
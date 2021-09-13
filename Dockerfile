FROM openjdk:11-jre-slim

RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 982 eventgenerator && \
    useradd -r -u 982 -g eventgenerator eventgenerator
USER eventgenerator
COPY target/evgen*.jar /opt/eventgenerator.jar

ENTRYPOINT [ "java", "-jar", "/opt/eventgenerator.jar" ]

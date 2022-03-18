FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y openjdk-16-jdk openjdk-16-jre nginx vim && \
    apt-get clean;

ENV JAVA_HOME=/usr/lib/jvm/java-16-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:${PATH}"

WORKDIR /app

COPY src .

CMD service nginx start && javac TcpServer.java && java TcpServer

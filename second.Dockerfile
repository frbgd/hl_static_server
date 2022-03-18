FROM ubuntu:latest

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y build-essential libssl-dev unzip openjdk-16-jdk openjdk-16-jre nginx vim git && \
    apt-get clean;

CMD cd /usr/local/src && git clone https://github.com/wg/wrk.git && cd wrk && make && cp -r wrk /usr/local/bin

ENV JAVA_HOME=/usr/lib/jvm/java-16-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:${PATH}"

CMD git clone https://github.com/init/http-test-suite.git /static/http-test-suite && cp -r /static/http-test-suite /var/www/html

WORKDIR /app

COPY src .

CMD service nginx start && javac TcpServer.java && java TcpServer

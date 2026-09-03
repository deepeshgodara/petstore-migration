FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    openjdk-8-jdk \
    curl \
    unzip \
    tar \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Apache TomEE 1.7.5 Plus (J2EE 1.3 / EJB 2.0 certified container)
WORKDIR /opt
RUN curl -sSL https://archive.apache.org/dist/tomee/tomee-1.7.5/apache-tomee-1.7.5-plus.tar.gz | tar -xz && \
    mv apache-tomee-plus-1.7.5 /opt/tomee

WORKDIR /petstore

# Copy configs and entrypoint
COPY config /opt/tomee/conf/
COPY data /opt/tomee/data/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8000 8088 61616

ENTRYPOINT ["/entrypoint.sh"]

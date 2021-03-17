# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM openjdk:8-jre-alpine

WORKDIR /usr/bin

# Set versions to use, override them with the `--build-arg` flag.
# https://docs.docker.com/engine/reference/builder/#arg
ARG KAFKA_VERSION="2.7.0"
ARG SCALA_VERSION="2.12"

# Set variables with default values used by the `start-kafka.sh` script.
# Override them wiht the `--env` or `-e` flag.
# https://docs.docker.com/engine/reference/builder/#env
ENV KAFKA_TOPIC="${KAFKA_TOPIC:-messages}"
ENV KAFKA_ADDRESS="${KAFKA_ADDRESS:-localhost}"
ENV KAFKA_PORT="${KAFKA_PORT:-9092}"
ENV ZOOKEEPER_PORT="${ZOOKEEPER_PORT:-2181}"

COPY *.sh ./
COPY key.json ./

RUN apk add --no-cache py-pip curl which bash

# Download and install Apache Kafka.
RUN apk add --no-cache bash \
  && wget https://apache.mivzakim.net/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz \
    -O /tmp/kafka.tgz \
  && tar xzf /tmp/kafka.tgz -C /opt && rm /tmp/kafka.tgz \
  && ln -s /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka

RUN curl -sSL https://sdk.cloud.google.com | bash

RUN mkdir /certs

RUN /root/google-cloud-sdk/bin/gcloud auth activate-service-account --key-file=/usr/bin/key.json
RUN /root/google-cloud-sdk/bin/gsutil cp gs://kafka-test-certs/server.keystore.jks /certs/key.jks
RUN /root/google-cloud-sdk/bin/gsutil cp gs://kafka-test-certs/server.truststore.jks /certs/trust.jks

RUN /root/google-cloud-sdk/bin/gsutil cp gs://kafka-test-certs/client.truststore.jks /certs/client-trust.jks
RUN /root/google-cloud-sdk/bin/gsutil cp gs://kafka-test-certs/client.keystore.jks /certs/client-key.jks

# Export the KAFKA_HOME and add it to PATH as well.
ENV KAFKA_HOME="/opt/kafka"
ENV PATH="${KAFKA_HOME}/bin:/usr/bin:/usr/bin/google-cloud-sdk/bin:/root/google-cloud-sdk/bin:${PATH}"

# Expose Zookeeper and Kafka ports for externa communication.
# https://docs.docker.com/engine/reference/builder/#expose
EXPOSE ${ZOOKEEPER_PORT} ${KAFKA_PORT} 9093/tcp

# Set the default command to the start-kafka.sh script.
ENTRYPOINT ["bash", "/usr/bin/start-kafka.sh"]
# Kafka to BigQuery Dataflow Template
The pipeline template read data from Kafka (Support SSL), transform the data and outputs the resulting records to BigQuery

## Getting Started

### Requirements
* Java 11
* Maven
* The Kafka topic(s) exists 
* Valid JSON messages
* The BigQuery output table exists.
* The Kafka brokers are reachable from the Dataflow worker machines.
* Upload your truststore & keystore, in case the SSL enable, to GCP Storage


#### Configurations
* Set Environment Variables
```sh
export PROJECT="$(gcloud config get-value project)" (Required)
export TEMPLATE_BUCKET_NAME=gs://<bucket-name> (Required)
export STAGING_LOCATION=${TEMPLATE_BUCKET_NAME}/staging (Required)
export TEMP_LOCATION=${TEMPLATE_BUCKET_NAME}/staging (Required)
export TEMPLATE_LOCATION=${TEMPLATE_BUCKET_NAME}/kafka-to-bq (Required)
export INPUT_TOPIC=<topic_name> (Required)
export OUTPUT_TABLE=<table_name> (Optional, default: "kafka-bq.sample")
export KAFKA_BROKER=<broker_host:port> (Optional, default: localhost:9092)
export ENABLE_SSL=<true/false> (Optional, default: false)
export REGION=${"$(gcloud config get-value compute/region)":-"us-central1"} (Reuired)
```

* Set Environment Variables for SSL, just when SSL enable
```sh
export KEYSTORE_PASS=<keystore_password> (Required)
export TRUSTSTORE_PASS=<truststore_password> (Required)
export KEYSTORE_PATH=<keystore_path> (Optional, default: "/tmp/kafka.keystore")
export TRUSTSTORE_PATH=<truststore_path> (Optional, default: "/tmp/kafka.truststore")
export KEYSTORE_OBJECT_NAME=<object_name> (Required)
export TRUSTSTORE_OBJECT_NAME=<object_name> (Required)
export SSL_BUCKET_NAME=<bucket_name> (Reuired)
```

### Building Template
This template is a classic template

The template requires the following parameters:
* project: Your GCP project ID
* inputTopics: Kafka topic to read the messages.
* outputTable: BigQuery table to write the results.
* region: Name of your region. 
* bootstrapServers: Comma separated list of bootstrap servers.
* stagingLocation
* gcpTempLocation
* templateLocation: Location for your template.

The template allows for the user to supply the following optional parameters:
* isEnableSSL: Enable/Disable SSL connection with Kafka

The template allows for the user to supply the following optional parameters in case of SSL ENABLED:
* keystorePath: Path to your keystore file
* truststorePath: Path to you truststore file


* Build the template 
```sh
mvn compile exec:java \
     -Dexec.mainClass=com.example.template.KafkaToBigquery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=$PROJECT \
                  --stagingLocation=$STAGING_LOCATION \
                  --gcpTempLocation=$TEMP_LOCATION \
                  --templateLocation=$TEMPLATE_LOCATION \
                  --inputTopic=$INPUT_TOPIC \
                  --outputTable=$OUTPUT_TABLE \
                  --bootstrapServer=$KAFKA_BROKER \
                  --region=$REGION"
```
Optionally specify window size (10 minutes example):
```sh
mvn compile exec:java \
     -Dexec.mainClass=com.example.template.KafkaToBigquery \
     -Dexec.args="--runner=DataflowRunner \
                  --project=$PROJECT \
                  --stagingLocation=$STAGING_LOCATION \
                  --gcpTempLocation=$TEMP_LOCATION \
                  --templateLocation=$TEMPLATE_LOCATION \
                  --inputTopic=$INPUT_TOPIC \
                  --outputTable=$OUTPUT_TABLE \
                  --bootstrapServer=$KAFKA_BROKER \
                  --windowSize=10 \
                  --region=$REGION"
```

### Enable SSL for the template building
```sh
--isEnableSSL=$ENABLE_SSL \
--keystorePath=$KEYSTORE_PATH \ 
--truststorePath=$TRUSTSTORE_PATH \
--keystorePassword=$KEYSTORE_PASS \
--truststorePassword=$TRUSTSTORE_PASS \
--keystoreObjName=$KEYSTORE_OBJECT_NAME \
--truststoreObjName=$TRUSTSTORE_OBJECT_NAME \
--bucketName=$SSL_BUCKET_NAME

--keystorePath and  --truststorePath are Optional as describe above
```

### Executing Template

Template can be executed using the following gcloud command.
```sh
gcloud dataflow jobs run JOB_NAME --gcs-location ${TEMPLATE_LOCATION}
```

### Next Version

Next version will include the following things:
* Docker compose file for local dev (Kafka, zookeeper ..)
* Dead leater
* More flexebility 
* More configurations
* Script to generate keystore & truststore
* Tests

### Stay Tune


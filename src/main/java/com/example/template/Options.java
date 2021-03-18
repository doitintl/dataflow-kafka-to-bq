package com.example.template;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.avro.reflect.Nullable;

public interface Options extends StreamingOptions {
    @Description("Use Kafka, if false set to Google PubSub")
    @Default.Boolean(false)
    Boolean getIsKafka();
    void setIsKafka(Boolean value);

    @Description("Apache Kafka/PubSub topic to read from.")
    @Validation.Required
    String getInputTopic();
    void setInputTopic(String value);

    @Description("BigQuery table to write to, in the form 'project:dataset.table' or 'dataset.table'.")
    @Default.String("kafka-bq.sample")
    String getOutputTable();
    void setOutputTable(String value);

    @Description("Apache Kafka bootstrap servers in the form 'hostname:port'.")
    @Default.String("localhost:9092")
    String getBootstrapServer();
    void setBootstrapServer(String value);

    @Description("Kafka truststore path")
    @Default.String("/tmp/kafka.truststore")
    String getTruststorePath();
    void setTruststorePath(String value);

    @Description("Kafka keystore path")
    @Default.String("/tmp/kafka.keystore")
    String getKeystorePath();
    void setKeystorePath(String value);

    @Description("Kafka keystore password")
    @Nullable
    String getKeystorePassword();
    void setKeystorePassword(String value);

    @Description("Kafka truststore password")
    @Nullable
    String getTruststorePassword();
    void setTruststorePassword(String value);

    @Description("Kafka keystore object name in GCS")
    @Nullable
    String getKeystoreObjName();
    void setKeystoreObjName(String value);

    @Description("Kafka truststore object name in GCS")
    @Nullable
    String getTruststoreObjName();
    void setTruststoreObjName(String value);

    @Description("SSL bucket name")
    @Nullable
    String getBucketName();
    void setBucketName(String value);

    @Description("Enable SSL")
    @Default.Boolean(false)
    Boolean getIsEnableSSL();
    void setIsEnableSSL(Boolean value);

    @Description("Window Size in Minutes")
    @Default.Integer(1)
    Integer getWindowSize();
    void setWindowSize(Integer value);
}

package com.example.template;

import java.util.Arrays;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.gson.Gson;

import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.WithTimestamps;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Apache Beam pipeline that reads JSON encoded messages from Kafka and
 * writes them to a BigQuery table.
 */
public class KafkaToBigquery {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaToBigquery.class);
    private static final Gson GSON = new Gson();

    public interface Options extends StreamingOptions {
        @Description("Apache Kafka topic to read from.")
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

    }

    @DefaultCoder(AvroCoder.class)
    private static class PageRating {
        Instant processingTime;
        @Nullable String url;
        @Nullable String rating;
    }


    public static void main(final String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
        options.setStreaming(true);

        SSLConfig sslConfig = new SSLConfig(
                options.getKeystorePath(),
                options.getKeystorePassword(),
                options.getKeystoreObjName(),
                options.getTruststorePath(),
                options.getTruststorePassword(),
                options.getTruststoreObjName(),
                options.getBucketName(),
                options.getIsEnableSSL()
        );

        var pipeline = Pipeline.create(options);
        pipeline
                .apply("Read messages from Kafka",
                        KafkaIO.<String, String>read()
                                .withBootstrapServers(options.getBootstrapServer())
                                .withTopic(options.getInputTopic())
                                .withConsumerFactoryFn(new ConsumerFactoryFn(sslConfig))
                                .withKeyDeserializer(StringDeserializer.class)
                                .withValueDeserializer(StringDeserializer.class)
                                .withoutMetadata())
                .apply("Get message contents", Values.<String>create())
                .apply("Log messages", MapElements.into(TypeDescriptor.of(String.class))
                        .via(message -> {
                            LOG.info("Received: {}", message);
                            return message;
                        }))
                .apply("Parse JSON", MapElements.into(TypeDescriptor.of(PageRating.class))
                        .via(message -> GSON.fromJson(message, PageRating.class)))

                .apply("Add processing time", WithTimestamps.of((pageRating) -> new Instant(pageRating.processingTime)))
                .apply("Fixed-size windows", Window.into(FixedWindows.of(Duration.standardMinutes(1))))

                .apply("Convert to BigQuery TableRow", MapElements.into(TypeDescriptor.of(TableRow.class))
                        .via(pageRating -> new TableRow()
                                .set("processing_time", pageRating.processingTime.toString())
                                .set("url", pageRating.url)
                                .set("rating", pageRating.rating)))
                .apply("Write to BigQuery", BigQueryIO.writeTableRows()
                        .to(options.getOutputTable())
                        .withSchema(new TableSchema().setFields(Arrays.asList(
                                new TableFieldSchema().setName("processing_time").setType("TIMESTAMP"),
                                new TableFieldSchema().setName("url").setType("STRING"),
                                new TableFieldSchema().setName("rating").setType("STRING"))))
                        .withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
                        .withWriteDisposition(WriteDisposition.WRITE_APPEND));

        // For a Dataflow Flex Template, do NOT waitUntilFinish().
        pipeline.run();
    }
}

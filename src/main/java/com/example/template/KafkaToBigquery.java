package com.example.template;

import java.nio.charset.StandardCharsets;
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
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.WithTimestamps;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
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

    @DefaultCoder(AvroCoder.class)
    private static class PageRating {
        Instant processingTime;
        @Nullable String url;
        @Nullable String rating;
    }


    public static void main(final String[] args) {
        final Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
        options.setStreaming(true);
        final Duration windowSizeInMinutes = Duration.standardMinutes(options.getWindowSize());

        final SSLConfig sslConfig = new SSLConfig(
                options.getKeystorePath(),
                options.getKeystorePassword(),
                options.getKeystoreObjName(),
                options.getTruststorePath(),
                options.getTruststorePassword(),
                options.getTruststoreObjName(),
                options.getBucketName(),
                options.getIsEnableSSL()
        );

        final var pipeline = Pipeline.create(options);

        final PCollection<PageRating> stream = getStream(options, sslConfig, pipeline);

        stream
                .apply("Add processing time", WithTimestamps.of((pageRating) -> new Instant(pageRating.processingTime)))
                .apply("Fixed-size windows", Window.into(FixedWindows.of(windowSizeInMinutes)))

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

        pipeline.run();
    }

    private static PCollection<PageRating> getStream(Options options, SSLConfig sslConfig, Pipeline pipeline) {
        if(options.getIsKafka()) {
            return getKafkaStream(options, sslConfig, pipeline);
        } else {
            return getPubSubStream(options, pipeline);
        }
    }

    private static PCollection<PageRating> getPubSubStream(Options options, Pipeline pipeline) {
        return pipeline.apply("Read messages from Google PubSub", PubsubIO.readMessages()
            .fromTopic(options.getInputTopic())
        ).apply("Log messages and Parse JSON", MapElements.into(TypeDescriptor.of(PageRating.class))
                .via(message -> {
                    final String stringPayload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    LOG.debug("Received: {}", stringPayload);
                    return GSON.fromJson(stringPayload, PageRating.class);
                })
        );
    }

    private static PCollection<PageRating> getKafkaStream(Options options, SSLConfig sslConfig, Pipeline pipeline) {
        return pipeline
                .apply("Read messages from Kafka",
                        KafkaIO.<String, String>read()
                                .withBootstrapServers(options.getBootstrapServer())
                                .withTopic(options.getInputTopic())
                                .withConsumerFactoryFn(new ConsumerFactoryFn(sslConfig))
                                .withKeyDeserializer(StringDeserializer.class)
                                .withValueDeserializer(StringDeserializer.class)
                                .withoutMetadata())
                .apply("Get message contents", Values.<String>create())
                .apply("Log messages and Parse JSON", MapElements.into(TypeDescriptor.of(PageRating.class))
                        .via(message -> {
                            LOG.debug("Received: {}", message);
                            return GSON.fromJson(message, PageRating.class);
                        })
                );
    }
}

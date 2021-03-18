package com.example.template;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Blob;
import java.util.Map;
import java.io.File;
import java.nio.file.Paths;

import static java.lang.String.format;


public class ConsumerFactoryFn implements SerializableFunction<Map<String, Object>, Consumer<byte[], byte[]>> {
    private static final long serialVersionUID = 1L;
    private final SSLConfig sslConfig;

    public ConsumerFactoryFn(final SSLConfig sslConfiguration) {
        this.sslConfig = sslConfiguration;
    }

    public Consumer<byte[], byte[]> apply(final Map<String, Object> config) {
        if (sslConfig.isEnable) {
            return new KafkaConsumer<>(setSSLConfig(config));
        }
        return new KafkaConsumer<>(config);
    }

    private Map<String, Object> setSSLConfig(final Map<String, Object> config) {
        final Storage storage = StorageOptions.getDefaultInstance().getService();

        config.put("security.protocol", "SSL");
        config.put("ssl.keystore.location", sslConfig.keystorePath);
        config.put("ssl.keystore.password", sslConfig.keystorePassword);
        config.put("ssl.key.password", sslConfig.keystorePassword);
        config.put("ssl.truststore.location", sslConfig.truststorePath);
        config.put("ssl.truststore.password", sslConfig.truststorePassword);

        getBlob(storage, sslConfig.truststorePath, sslConfig.truststorePath);
        getBlob(storage, sslConfig.keyObjectName, sslConfig.keystorePath);

        return config;
    }

    private void getBlob(final Storage storage, final String path, final String objectName) {
        final Blob blob = storage.get(this.sslConfig.bucketName, objectName);
        blob.downloadTo(Paths.get(path));
        final File keyfile = new File(path);
        if (!keyfile.exists()) {
            throw new RuntimeException(format("SSL key file %s does not exist", objectName));
        }
    }
}


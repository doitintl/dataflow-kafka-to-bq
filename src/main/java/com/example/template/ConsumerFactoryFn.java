package com.example.template;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Blob;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import jdk.internal.org.jline.utils.Log;

public class ConsumerFactoryFn implements SerializableFunction<Map<String, Object>, Consumer<byte[], byte[]>> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerFactoryFn.class);
    private SSLConfig sslConfig;

    public ConsumerFactoryFn(SSLConfig sslConfiguration) {
        this.sslConfig = sslConfiguration;
    }

    public Consumer<byte[], byte[]> apply(Map<String, Object> config) {
        if (sslConfig.isEnable) {
            try {
                config.put("security.protocol", (Object) "SSL");
                config = setTruststore(config);
                config = setKeystore(config);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

        return new KafkaConsumer<byte[], byte[]>(config);
    }

    private Map<String, Object> setTruststore(Map<String, Object> config) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Blob blob = storage.get(this.sslConfig.bucketName, this.sslConfig.trustObjectName);
            blob.downloadTo(Paths.get(this.sslConfig.truststorePath));
            File f = new File(this.sslConfig.truststorePath); // assuring the store file exists
            if (f.exists()) {
                LOG.debug("key exists");

            } else {
                LOG.error("key does not exist");

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage());
        }

        config.put("ssl.truststore.location", (Object) this.sslConfig.truststorePath);
        config.put("ssl.truststore.password", (Object) this.sslConfig.truststorePassword);
        return config;
    }

    private Map<String, Object> setKeystore(Map<String, Object> config) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            // Storage storage =
            // StorageOptions.newBuilder().setProjectId("oren-playground-297209")
            // .setCredentials(GoogleCredentials.getApplicationDefault()).build().getService();
            Blob blob = storage.get(this.sslConfig.bucketName, this.sslConfig.keyObjectName);
            blob.downloadTo(Paths.get(this.sslConfig.keystorePath));
            File f = new File(this.sslConfig.keystorePath); // assuring the store file exists
            if (f.exists()) {
                LOG.debug("key exists");

            } else {
                LOG.error("key does not exist");

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error(e.getMessage());
        }

        config.put("ssl.keystore.location", (Object) this.sslConfig.keystorePath);
        config.put("ssl.keystore.password", (Object) this.sslConfig.keystorePassword);
        config.put("ssl.key.password", (Object) this.sslConfig.keystorePassword);
        return config;
    }
}


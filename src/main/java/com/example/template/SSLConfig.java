package com.example.template;

import java.io.Serializable;
import java.util.Map;

public class SSLConfig implements Serializable {
    final String keystorePath;
    final String truststorePath;
    final String keystorePassword;
    final String truststorePassword;
    final String bucketName;
    final String keyObjectName;
    final String trustObjectName;
    final Boolean isEnable;

    public SSLConfig(String keystorePath, String keystorePassword, String keyObjectName,
                          String truststorePath, String truststorePassword, String trustObjectName, String bucketName,
                     Boolean isEnable) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.keyObjectName = keyObjectName;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.trustObjectName = trustObjectName;
        this.bucketName = bucketName;
        this.isEnable = isEnable;
    }
}

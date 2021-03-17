package com.example.template;

import java.io.Serializable;
import java.util.Map;

public class SSLConfig implements Serializable {
    String keystorePath;
    String truststorePath;
    String keystorePassword;
    String truststorePassword;
    String bucketName;
    String keyObjectName;
    String trustObjectName;
    Boolean isEnable;

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

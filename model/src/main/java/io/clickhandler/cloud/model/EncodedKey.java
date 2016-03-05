package io.clickhandler.cloud.model;



import io.clickhandler.sql.Column;

import java.io.Serializable;

public class EncodedKey implements Serializable {
    private static final String DEFAULT_ALGORITHM = "AES256";

    @Column
    private String algorithm = DEFAULT_ALGORITHM;
    @Column(length = 128)
    private String key;
    @Column(length = 128)
    private String md5;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}

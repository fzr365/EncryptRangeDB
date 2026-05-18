package com.encryprangedb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "encryprangedb")
public class CryptoProperties {

    private Crypto crypto = new Crypto();
    private Eafs eafs = new Eafs();

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public Eafs getEafs() {
        return eafs;
    }

    public void setEafs(Eafs eafs) {
        this.eafs = eafs;
    }

    public static class Crypto {
        private String aesMasterKey;
        private String opeMasterKey;
        private String integrityMasterKey;
        private String keyVersion = "v1";
        private int sensitivity = 32;

        public String getAesMasterKey() {
            return aesMasterKey;
        }

        public void setAesMasterKey(String aesMasterKey) {
            this.aesMasterKey = aesMasterKey;
        }

        public String getOpeMasterKey() {
            return opeMasterKey;
        }

        public void setOpeMasterKey(String opeMasterKey) {
            this.opeMasterKey = opeMasterKey;
        }

        public String getIntegrityMasterKey() {
            return integrityMasterKey;
        }

        public void setIntegrityMasterKey(String integrityMasterKey) {
            this.integrityMasterKey = integrityMasterKey;
        }

        public String getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(String keyVersion) {
            this.keyVersion = keyVersion;
        }

        public int getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(int sensitivity) {
            this.sensitivity = sensitivity;
        }
    }

    public static class Eafs {
        private int bucketSize = 100;

        public int getBucketSize() {
            return bucketSize;
        }

        public void setBucketSize(int bucketSize) {
            this.bucketSize = bucketSize;
        }
    }
}

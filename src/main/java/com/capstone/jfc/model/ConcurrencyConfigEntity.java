package com.capstone.jfc.model;

import jakarta.persistence.*;

@Entity
@Table(name = "concurrency_config")
public class ConcurrencyConfigEntity {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey; // e.g. "SCAN_PULL_CODESCAN" or "tenant-1"

    @Column(name = "config_value")
    private int configValue;

    public ConcurrencyConfigEntity() {}

    public ConcurrencyConfigEntity(String configKey, int configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public int getConfigValue() {
        return configValue;
    }
    public void setConfigValue(int configValue) {
        this.configValue = configValue;
    }
}

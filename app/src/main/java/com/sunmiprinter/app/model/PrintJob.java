package com.sunmiprinter.app.model;

import java.util.Map;

public class PrintJob {
    private long timestamp;
    private Map<String, String> data;
    private String rawJson;

    public PrintJob(long timestamp, Map<String, String> data, String rawJson) {
        this.timestamp = timestamp;
        this.data = data;
        this.rawJson = rawJson;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}

package com.sunmiprinter.app.model;

import java.util.List;

public class PrintLayout {
    private String name;
    private long createdAt;
    private List<PrintElement> elements;

    public PrintLayout(String name, List<PrintElement> elements) {
        this.name = name;
        this.elements = elements;
        this.createdAt = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<PrintElement> getElements() {
        return elements;
    }

    public void setElements(List<PrintElement> elements) {
        this.elements = elements;
    }
}

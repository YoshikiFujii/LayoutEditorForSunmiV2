package com.sunmiprinter.app.model;

public abstract class PrintElement {
    private String id;
    private int alignment; // 0: Left, 1: Center, 2: Right

    public PrintElement(String id, int alignment) {
        this.id = id;
        this.alignment = alignment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public abstract String getType(); // "Text" or "Image"
}

package com.sunmiprinter.app.model;

public class SpacePrintElement extends PrintElement {
    private int height; // Number of lines

    public SpacePrintElement(String id, int height) {
        super(id, 0); // Alignment doesn't matter for space
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String getType() {
        return "Space";
    }
}

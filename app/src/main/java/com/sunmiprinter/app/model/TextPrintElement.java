package com.sunmiprinter.app.model;

public class TextPrintElement extends PrintElement {
    private String content;
    private float fontSize;
    private boolean isBold;
    private String typeface; // "DEFAULT", "SANS_SERIF", "SERIF", "MONOSPACE"

    public TextPrintElement(String id, int alignment, String content, float fontSize, boolean isBold, String typeface) {
        super(id, alignment);
        this.content = content;
        this.fontSize = fontSize;
        this.isBold = isBold;
        this.typeface = typeface;
    }

    // Legacy Constructor support
    public TextPrintElement(String id, int alignment, String content, float fontSize, boolean isBold) {
        this(id, alignment, content, fontSize, isBold, "DEFAULT");
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isBold() {
        return isBold;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public String getTypeface() {
        return typeface;
    }

    public void setTypeface(String typeface) {
        this.typeface = typeface;
    }

    @Override
    public String getType() {
        return "Text";
    }
}

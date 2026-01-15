package com.sunmiprinter.app.model;

import android.graphics.Bitmap;

public class ImagePrintElement extends PrintElement {
    private transient Bitmap bitmap; // transient to ignore by default GSON, using path instead
    private String imagePath;
    private int width;
    private int height;

    public ImagePrintElement(String id, int alignment, Bitmap bitmap, int width, int height, String imagePath) {
        super(id, alignment);
        this.bitmap = bitmap;
        this.width = width;
        this.height = height;
        this.imagePath = imagePath;
    }

    // Legacy constructor wrapper
    public ImagePrintElement(String id, int alignment, Bitmap bitmap, int width, int height) {
        this(id, alignment, bitmap, width, height, null);
    }

    public ImagePrintElement(String id, int alignment, Bitmap bitmap) {
        super(id, alignment);
        this.bitmap = bitmap;
        if (bitmap != null) {
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
        }
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        // Don't auto-update width/height here if we want to preserve resize settings
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String getType() {
        return "Image";
    }
}

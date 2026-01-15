package com.sunmiprinter.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.sunmiprinter.app.model.ImagePrintElement;
import com.sunmiprinter.app.model.PrintElement;
import com.sunmiprinter.app.model.PrintLayout;
import com.sunmiprinter.app.model.SpacePrintElement;
import com.sunmiprinter.app.model.TextPrintElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LayoutStorageManager {

    private static final String FILENAME = "print_layouts.json";
    private static final String IMAGES_DIR = "images";

    public static void saveLayouts(Context context, List<PrintLayout> layouts) {
        Gson gson = createGson();
        String json = gson.toJson(layouts);
        try (FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
                OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<PrintLayout> loadLayouts(Context context) {
        List<PrintLayout> layouts = new ArrayList<>();
        if (!new File(context.getFilesDir(), FILENAME).exists()) {
            return layouts;
        }

        try (FileInputStream fis = context.openFileInput(FILENAME);
                InputStreamReader reader = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(reader)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = createGson();
            Type listType = new TypeToken<List<PrintLayout>>() {
            }.getType();
            layouts = gson.fromJson(sb.toString(), listType);

            // Post-process images: Load Bitmaps from path
            for (PrintLayout layout : layouts) {
                for (PrintElement element : layout.getElements()) {
                    if (element instanceof ImagePrintElement) {
                        ImagePrintElement imgElement = (ImagePrintElement) element;
                        if (imgElement.getImagePath() != null) {
                            File imgFile = new File(imgElement.getImagePath());
                            if (imgFile.exists()) {
                                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                                imgElement.setBitmap(bitmap);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return layouts;
    }

    public static void reloadBitmaps(PrintLayout layout) {
        if (layout == null)
            return;
        for (PrintElement element : layout.getElements()) {
            if (element instanceof ImagePrintElement) {
                ImagePrintElement imgElement = (ImagePrintElement) element;
                if (imgElement.getImagePath() != null) {
                    File imgFile = new File(imgElement.getImagePath());
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        imgElement.setBitmap(bitmap);
                    }
                }
            }
        }
    }

    public static File exportLayout(Context context, PrintLayout layout) {
        // Create a deep copy or temporary object to modify image paths to Base64
        Gson gson = createGson();
        String jsonStr = gson.toJson(layout);
        PrintLayout exportLayout = gson.fromJson(jsonStr, PrintLayout.class);

        // Convert images to Base64
        for (PrintElement element : exportLayout.getElements()) {
            if (element instanceof ImagePrintElement) {
                ImagePrintElement img = (ImagePrintElement) element;
                if (img.getImagePath() != null) {
                    File imgFile = new File(img.getImagePath());
                    if (imgFile.exists()) {
                        String base64 = encodeImageToBase64(imgFile);
                        // Hack: Store Base64 in imagePath field? Or add a new field.
                        // For simplicity, we use imagePath as the carrier if we control serialization.
                        // But better to use a dedicated flow.
                        // Let's assume we prepend "BASE64:" to the string.
                        if (base64 != null) {
                            img.setImagePath("BASE64:" + base64);
                        }
                    }
                }
            }
        }

        String exportJson = gson.toJson(exportLayout);

        File cacheDir = new File(context.getCacheDir(), "exports");
        if (!cacheDir.exists())
            cacheDir.mkdirs();

        File file = new File(cacheDir, "layout_" + layout.getName() + ".json");
        try (FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(exportJson);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PrintLayout importLayout(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
                InputStreamReader reader = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(reader)) {

            Gson gson = createGson();
            PrintLayout layout = gson.fromJson(br, PrintLayout.class);

            // Restore images
            for (PrintElement element : layout.getElements()) {
                if (element instanceof ImagePrintElement) {
                    ImagePrintElement img = (ImagePrintElement) element;
                    String path = img.getImagePath();
                    if (path != null && path.startsWith("BASE64:")) {
                        String base64 = path.substring(7);
                        String localPath = saveBase64Image(context, base64);
                        img.setImagePath(localPath);
                        // Helper will reload bitmap
                        if (localPath != null) {
                            Bitmap bmp = BitmapFactory.decodeFile(localPath);
                            img.setBitmap(bmp);
                        }
                    }
                }
            }
            return layout;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String encodeImageToBase64(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String saveBase64Image(Context context, String base64) {
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            return saveBitmapToInternal(context, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String saveImageToInternal(Context context, Uri imageUri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null)
                return null;

            File dir = new File(context.getFilesDir(), IMAGES_DIR);
            if (!dir.exists())
                dir.mkdir();

            String filename = "img_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, filename);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Save Bitmap directly (already resized)
    public static String saveBitmapToInternal(Context context, Bitmap bitmap) {
        try {
            File dir = new File(context.getFilesDir(), IMAGES_DIR);
            if (!dir.exists())
                dir.mkdir();

            String filename = "img_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, filename);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(PrintElement.class, new InterfaceAdapter())
                .create();
    }

    // Custom Adapter to handle polymorphism
    private static class InterfaceAdapter implements JsonSerializer<PrintElement>, JsonDeserializer<PrintElement> {
        @Override
        public JsonElement serialize(PrintElement src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.add("type", new JsonPrimitive(src.getClass().getSimpleName()));
            result.add("data", context.serialize(src, src.getClass()));
            return result;
        }

        @Override
        public PrintElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String type = jsonObject.get("type").getAsString();
            JsonElement element = jsonObject.get("data");

            try {
                String fullClassName = "com.sunmiprinter.app.model." + type;
                Class<?> clazz = Class.forName(fullClassName);
                return context.deserialize(element, clazz);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException("Unknown element type: " + type, e);
            }
        }
    }
}

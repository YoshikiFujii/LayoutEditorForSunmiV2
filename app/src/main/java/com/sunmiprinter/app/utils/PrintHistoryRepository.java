package com.sunmiprinter.app.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sunmiprinter.app.model.PrintJob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrintHistoryRepository {

    private static final String TAG = "PrintHistoryRepo";
    private static final String FILE_PREFIX = "received_data_";
    private static final String FILE_EXT = ".json";

    private final Context context;
    private final Gson gson;

    public PrintHistoryRepository(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public void savePrintJob(PrintJob job) {
        String dateStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date(job.getTimestamp()));
        String fileName = FILE_PREFIX + dateStr + FILE_EXT;
        File file = new File(context.getExternalFilesDir(null), fileName);

        List<PrintJob> jobs = loadHistory(file);
        jobs.add(job);

        try (FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            gson.toJson(jobs, writer);
        } catch (IOException e) {
            Log.e(TAG, "Error saving print job", e);
        }
    }

    public List<PrintJob> loadHistory(File file) {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
                InputStreamReader reader = new InputStreamReader(fis)) {
            List<PrintJob> jobs = gson.fromJson(reader, new TypeToken<List<PrintJob>>() {
            }.getType());
            if (jobs == null)
                return new ArrayList<>();
            return jobs;
        } catch (IOException e) {
            Log.e(TAG, "Error loading history", e);
            return new ArrayList<>();
        }
    }

    public List<File> getHistoryFiles() {
        File dir = context.getExternalFilesDir(null);
        if (dir == null || !dir.exists())
            return new ArrayList<>();

        File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_EXT));
        if (files == null)
            return new ArrayList<>();

        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o2.getName().compareTo(o1.getName()); // Descending (newest first)
            }
        });
        return fileList;
    }
}

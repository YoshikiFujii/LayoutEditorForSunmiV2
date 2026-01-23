package com.sunmiprinter.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.adapter.PrintJobAdapter;
import com.sunmiprinter.app.model.PrintJob;
import com.sunmiprinter.app.utils.PrintHistoryRepository;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class HistoryDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PrintJobAdapter adapter;
    private PrintHistoryRepository repository;
    private TextView textTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        textTitle = findViewById(R.id.text_title);
        recyclerView = findViewById(R.id.recycler_detail_jobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PrintJobAdapter();
        recyclerView.setAdapter(adapter);

        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            File file = new File(filePath);
            textTitle.setText(file.getName().replace("received_data_", "").replace(".json", ""));

            repository = new PrintHistoryRepository(this);
            List<PrintJob> jobs = repository.loadHistory(file);
            Collections.reverse(jobs); // Newest first
            adapter.setJobs(jobs);
        } else {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Disable reprint in history viewer? Or keep it?
        // Prompt says "see the data". Doesn't explicitly say "reprint from history".
        // But the adapter has a button. If we don't set a listener, the button does
        // nothing.
        // Or we can hide the button in the adapter if binding for history?
        // For now, let's leave it as is (active button but no action) or implement a
        // simple "Can't reprint" toast.
        // Actually, if we want to support reprint here, we'd need to pass the layout to
        // this activity too, which is complex.
        // So I'll just show a Toast "Reprint not available in History view" if clicked.

        adapter.setOnReprintListener(new PrintJobAdapter.OnReprintListener() {
            @Override
            public void onReprint(PrintJob job) {
                Toast.makeText(HistoryDetailActivity.this, "Reprinting from History is not supported yet.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

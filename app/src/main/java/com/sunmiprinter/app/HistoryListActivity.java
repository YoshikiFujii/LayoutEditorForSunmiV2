package com.sunmiprinter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.adapter.HistoryFileAdapter;
import com.sunmiprinter.app.utils.PrintHistoryRepository;

import java.io.File;
import java.util.List;

public class HistoryListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryFileAdapter adapter;
    private PrintHistoryRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        recyclerView = findViewById(R.id.recycler_history_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryFileAdapter();
        recyclerView.setAdapter(adapter);

        repository = new PrintHistoryRepository(this);
        List<File> files = repository.getHistoryFiles();
        adapter.setFiles(files);

        adapter.setOnFileClickListener(new HistoryFileAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(File file) {
                Intent intent = new Intent(HistoryListActivity.this, HistoryDetailActivity.class);
                intent.putExtra("file_path", file.getAbsolutePath());
                startActivity(intent);
            }
        });
    }
}

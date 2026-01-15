package com.sunmiprinter.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.model.PrintLayout;
import com.sunmiprinter.app.utils.LayoutStorageManager;

import androidx.core.content.FileProvider;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LayoutListAdapter.OnLayoutActionListener {

    private RecyclerView recyclerView;
    private Button btnNewLayout;
    private Button btnImportLayout;
    private List<PrintLayout> savedLayouts;
    private LayoutListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        recyclerView = findViewById(R.id.recycler_view_layouts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnNewLayout = findViewById(R.id.btn_new_layout);
        btnImportLayout = findViewById(R.id.btn_import_layout);

        btnNewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditLayoutActivity.class);
                startActivity(intent);
            }
        });

        btnImportLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/json");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select Layout JSON"), 1001);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLayouts();
    }

    private void loadLayouts() {
        savedLayouts = LayoutStorageManager.loadLayouts(this);
        adapter = new LayoutListAdapter(savedLayouts, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onEdit(PrintLayout layout, int position) {
        Intent intent = new Intent(MainActivity.this, EditLayoutActivity.class);
        String json = LayoutStorageManager.createGson().toJson(layout);
        intent.putExtra("layout_data", json);
        intent.putExtra("layout_index", position);
        startActivity(intent);
    }

    @Override
    public void onDelete(final PrintLayout layout, final int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Layout")
                .setMessage("Are you sure you want to delete '" + layout.getName() + "'?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        savedLayouts.remove(position);
                        LayoutStorageManager.saveLayouts(MainActivity.this, savedLayouts);
                        // Notify adapter
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, savedLayouts.size());
                        Toast.makeText(MainActivity.this, "Layout deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onActivate(PrintLayout layout, int position) {
        Intent intent = new Intent(MainActivity.this, ReceiveAndPrintActivity.class);
        String json = LayoutStorageManager.createGson().toJson(layout);
        intent.putExtra("layout_data", json);
        startActivity(intent);
    }

    @Override
    public void onExport(PrintLayout layout, int position) {
        File file = LayoutStorageManager.exportLayout(this, layout);
        if (file != null) {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Layout"));
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                PrintLayout layout = LayoutStorageManager.importLayout(this, uri);
                if (layout != null) {
                    savedLayouts.add(layout);
                    LayoutStorageManager.saveLayouts(this, savedLayouts);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Layout Imported", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

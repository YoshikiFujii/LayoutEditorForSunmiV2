package com.sunmiprinter.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HistoryFileAdapter extends RecyclerView.Adapter<HistoryFileAdapter.ViewHolder> {

    private List<File> files = new ArrayList<>();
    private OnFileClickListener listener;

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<File> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
        }

        public void bind(final File file) {
            // Filename format: received_data_YYYYMMDD.json
            String name = file.getName();
            String datePart = name.replace("received_data_", "").replace(".json", "");
            textDate.setText(datePart);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onFileClick(file);
                    }
                }
            });
        }
    }
}

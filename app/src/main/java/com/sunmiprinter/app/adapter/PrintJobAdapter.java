package com.sunmiprinter.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.R;
import com.sunmiprinter.app.model.PrintJob;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PrintJobAdapter extends RecyclerView.Adapter<PrintJobAdapter.ViewHolder> {

    private List<PrintJob> jobs = new ArrayList<>();
    private OnReprintListener onReprintListener;

    public interface OnReprintListener {
        void onReprint(PrintJob job);
    }

    public void setOnReprintListener(OnReprintListener listener) {
        this.onReprintListener = listener;
    }

    public void setJobs(List<PrintJob> jobs) {
        this.jobs = jobs;
        notifyDataSetChanged();
    }

    public void addJob(int index, PrintJob job) {
        jobs.add(index, job);
        notifyItemInserted(index);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_print_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrintJob job = jobs.get(position);
        holder.bind(job);
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTimestamp;
        TextView textContent;
        Button btnReprint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textContent = itemView.findViewById(R.id.text_content_preview);
            btnReprint = itemView.findViewById(R.id.btn_reprint);
        }

        public void bind(final PrintJob job) {
            String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date(job.getTimestamp()));
            textTimestamp.setText(timeStr);

            StringBuilder contentBuilder = new StringBuilder();
            if (job.getData() != null) {
                for (Map.Entry<String, String> entry : job.getData().entrySet()) {
                    contentBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            textContent.setText(contentBuilder.toString().trim());

            btnReprint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onReprintListener != null) {
                        onReprintListener.onReprint(job);
                    }
                }
            });
        }
    }
}

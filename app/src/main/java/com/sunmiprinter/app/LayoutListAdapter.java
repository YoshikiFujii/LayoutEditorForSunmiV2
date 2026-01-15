package com.sunmiprinter.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.model.PrintLayout;

import java.util.List;

public class LayoutListAdapter extends RecyclerView.Adapter<LayoutListAdapter.LayoutViewHolder> {

    private List<PrintLayout> layouts;
    private OnLayoutActionListener listener;

    public interface OnLayoutActionListener {
        void onEdit(PrintLayout layout, int position);

        void onDelete(PrintLayout layout, int position);

        void onActivate(PrintLayout layout, int position);

        void onExport(PrintLayout layout, int position);
    }

    public LayoutListAdapter(List<PrintLayout> layouts, OnLayoutActionListener listener) {
        this.layouts = layouts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LayoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_layout, parent, false);
        return new LayoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LayoutViewHolder holder, int position) {
        PrintLayout layout = layouts.get(position);
        holder.bind(layout, position);
    }

    @Override
    public int getItemCount() {
        return layouts.size();
    }

    class LayoutViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        Button btnActivate, btnExport;
        ImageButton btnEdit, btnDelete;

        public LayoutViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_layout_name);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnActivate = itemView.findViewById(R.id.btn_activate);
            btnExport = itemView.findViewById(R.id.btn_export);
        }

        public void bind(final PrintLayout layout, final int position) {
            nameText.setText(layout.getName() + " (" + layout.getElements().size() + " items)");

            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEdit(layout, getAdapterPosition());
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onDelete(layout, getAdapterPosition());
                    }
                }
            });

            btnActivate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onActivate(layout, getAdapterPosition());
                    }
                }
            });

            btnExport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onExport(layout, getAdapterPosition());
                    }
                }
            });
        }
    }
}

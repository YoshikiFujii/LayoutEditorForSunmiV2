package com.sunmiprinter.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.model.ImagePrintElement;
import com.sunmiprinter.app.model.PrintElement;
import com.sunmiprinter.app.model.SpacePrintElement;
import com.sunmiprinter.app.model.TextPrintElement;

import java.util.Collections;
import java.util.List;

public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ElementViewHolder> {

    private List<PrintElement> elements;
    private OnItemClickListener listener;
    private OnOrderChangeListener orderListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnOrderChangeListener {
        void onMoveUp(int position);

        void onMoveDown(int position);
    }

    public ElementAdapter(List<PrintElement> elements, OnItemClickListener listener) {
        this.elements = elements;
        this.listener = listener;
    }

    public void setOnOrderChangeListener(OnOrderChangeListener listener) {
        this.orderListener = listener;
    }

    @NonNull
    @Override
    public ElementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_print_element, parent, false);
        return new ElementViewHolder(view, listener, orderListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ElementViewHolder holder, int position) {
        PrintElement element = elements.get(position);
        holder.bind(element);
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }

    static class ElementViewHolder extends RecyclerView.ViewHolder {
        TextView type, id, align, preview;
        ImageView imagePreview;
        Button btnUp, btnDown;

        public ElementViewHolder(@NonNull View itemView, final OnItemClickListener listener,
                final OnOrderChangeListener orderListener) {
            super(itemView);
            type = itemView.findViewById(R.id.element_type);
            id = itemView.findViewById(R.id.element_id);
            align = itemView.findViewById(R.id.element_align);
            preview = itemView.findViewById(R.id.element_preview);
            imagePreview = itemView.findViewById(R.id.element_image_preview);
            btnUp = itemView.findViewById(R.id.btn_up);
            btnDown = itemView.findViewById(R.id.btn_down);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });

            btnUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (orderListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            orderListener.onMoveUp(position);
                        }
                    }
                }
            });

            btnDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (orderListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            orderListener.onMoveDown(position);
                        }
                    }
                }
            });
        }

        public void bind(PrintElement element) {
            type.setText(element.getType());
            id.setText("ID: " + element.getId());

            String alignText = "Left";
            if (element.getAlignment() == 1)
                alignText = "Center";
            else if (element.getAlignment() == 2)
                alignText = "Right";
            align.setText(alignText);

            if (element instanceof TextPrintElement) {
                preview.setVisibility(View.VISIBLE);
                imagePreview.setVisibility(View.GONE);
                TextPrintElement textElement = (TextPrintElement) element;
                preview.setText(textElement.getContent());

                // Apply Font Preview
                android.graphics.Typeface tf = android.graphics.Typeface.DEFAULT;
                String typeStr = textElement.getTypeface();
                if (typeStr != null) {
                    switch (typeStr) {
                        case "SANS_SERIF":
                            tf = android.graphics.Typeface.SANS_SERIF;
                            break;
                        case "SERIF":
                            tf = android.graphics.Typeface.SERIF;
                            break;
                        case "MONOSPACE":
                            tf = android.graphics.Typeface.MONOSPACE;
                            break;
                    }
                }
                // Apply Bold
                if (textElement.isBold()) {
                    preview.setTypeface(tf, android.graphics.Typeface.BOLD);
                } else {
                    preview.setTypeface(tf, android.graphics.Typeface.NORMAL);
                }

            } else if (element instanceof ImagePrintElement) {
                preview.setVisibility(View.GONE);
                imagePreview.setVisibility(View.VISIBLE);
                ImagePrintElement imageElement = (ImagePrintElement) element;
                if (imageElement.getBitmap() != null) {
                    imagePreview.setImageBitmap(imageElement.getBitmap());
                }
            } else if (element instanceof SpacePrintElement) {
                preview.setVisibility(View.VISIBLE);
                imagePreview.setVisibility(View.GONE);
                preview.setText("Height: " + ((SpacePrintElement) element).getHeight() + " lines");
                align.setText("-"); // Alignment not used
            }
        }
    }
}

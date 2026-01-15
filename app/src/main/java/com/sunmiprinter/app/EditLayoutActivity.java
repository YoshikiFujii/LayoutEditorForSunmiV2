package com.sunmiprinter.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sunmiprinter.app.model.ImagePrintElement;
import com.sunmiprinter.app.model.PrintElement;
import com.sunmiprinter.app.model.SpacePrintElement;
import com.sunmiprinter.app.model.TextPrintElement;
import com.sunmiprinter.app.model.PrintLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditLayoutActivity extends AppCompatActivity implements ElementAdapter.OnItemClickListener {

    private TextView statusText;
    private Button btnConnect, btnAddText, btnAddImage, btnAddSpace, btnPrint;
    private RecyclerView recyclerView;
    private ElementAdapter adapter;
    private List<PrintElement> printElements;
    private int currentLayoutIndex = -1; // -1 for new
    private String currentLayoutName = null;

    private static final int REQUEST_IMAGE_PICK = 1001;
    private ImageView tempImagePreview; // For the dialog
    private EditText tempInputWidth, tempInputHeight; // To update width/height when image picked
    private Bitmap tempSelectedBitmap;

    private ImagePrintElement editingImageElement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        btnConnect = findViewById(R.id.btn_connect);
        btnAddText = findViewById(R.id.btn_add_text);
        btnAddImage = findViewById(R.id.btn_add_image);
        btnAddSpace = findViewById(R.id.btn_add_space);
        btnPrint = findViewById(R.id.btn_print);
        Button btnSave = findViewById(R.id.btn_save_layout);
        Button btnSaveAs = findViewById(R.id.btn_save_as);
        recyclerView = findViewById(R.id.recycler_view);

        printElements = new ArrayList<>();
        adapter = new ElementAdapter(printElements, this);
        adapter.setOnOrderChangeListener(new ElementAdapter.OnOrderChangeListener() {
            @Override
            public void onMoveUp(int position) {
                if (position > 0) {
                    java.util.Collections.swap(printElements, position, position - 1);
                    adapter.notifyItemMoved(position, position - 1);
                }
            }

            @Override
            public void onMoveDown(int position) {
                if (position < printElements.size() - 1) {
                    java.util.Collections.swap(printElements, position, position + 1);
                    adapter.notifyItemMoved(position, position + 1);
                }
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load data if passed
        if (getIntent().hasExtra("layout_data")) {
            String json = getIntent().getStringExtra("layout_data");
            int index = getIntent().getIntExtra("layout_index", -1);
            currentLayoutIndex = index;
            PrintLayout layout = com.sunmiprinter.app.utils.LayoutStorageManager.createGson().fromJson(json,
                    PrintLayout.class);
            if (layout != null) {
                currentLayoutName = layout.getName(); // Store name
                printElements.addAll(layout.getElements());
                // Post-process images to load bitmaps
                for (PrintElement element : printElements) {
                    if (element instanceof ImagePrintElement) {
                        ImagePrintElement imgElement = (ImagePrintElement) element;
                        if (imgElement.getImagePath() != null) {
                            java.io.File imgFile = new java.io.File(imgElement.getImagePath());
                            if (imgFile.exists()) {
                                imgElement.setBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        } else {
            currentLayoutIndex = -1; // New Layout
            currentLayoutName = null;
        }

        btnAddText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextDialog(null);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLayoutInternal();
            }
        });

        btnSaveAs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveAsDialog();
            }
        });

        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog(null);
            }
        });

        btnAddSpace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSpaceDialog(null);
            }
        });

        btnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printAllElements();
            }
        });

        SunmiPrintHelper.getInstance().connect(this, new SunmiPrintHelper.OnConnectionStatusListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    statusText.setText("Status: Connected");
                    Toast.makeText(EditLayoutActivity.this, "Printer Connected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> statusText.setText("Status: Disconnected"));
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        PrintElement element = printElements.get(position);
        if (element instanceof TextPrintElement) {
            showTextDialog((TextPrintElement) element);
        } else if (element instanceof ImagePrintElement) {
            showImageDialog((ImagePrintElement) element);
        } else if (element instanceof SpacePrintElement) {
            showSpaceDialog((SpacePrintElement) element);
        }
    }

    private void showTextDialog(final TextPrintElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(element == null ? "Add Text" : "Edit Text");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_text, null);
        builder.setView(view);

        final EditText inputId = view.findViewById(R.id.input_id);
        final EditText inputContent = view.findViewById(R.id.input_content);
        final EditText inputFontSize = view.findViewById(R.id.input_font_size);
        final CheckBox checkBold = view.findViewById(R.id.check_bold);
        final Spinner spinnerFont = view.findViewById(R.id.spinner_font);
        final RadioGroup radioGroup = view.findViewById(R.id.radio_group_align);

        // Setup Spinner
        String[] fonts = { "DEFAULT", "SANS_SERIF", "SERIF", "MONOSPACE" };
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fonts);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFont.setAdapter(adapterSpinner);

        if (element != null) {
            inputId.setText(element.getId());
            inputContent.setText(element.getContent());
            inputFontSize.setText(String.valueOf(element.getFontSize()));
            checkBold.setChecked(element.isBold());

            // Set Spinner Selection
            String typeface = element.getTypeface();
            if (typeface != null) {
                for (int i = 0; i < fonts.length; i++) {
                    if (fonts[i].equals(typeface)) {
                        spinnerFont.setSelection(i);
                        break;
                    }
                }
            }

            int align = element.getAlignment();
            if (align == 0)
                radioGroup.check(R.id.radio_left);
            else if (align == 1)
                radioGroup.check(R.id.radio_center);
            else if (align == 2)
                radioGroup.check(R.id.radio_right);
        }

        builder.setPositiveButton(element == null ? "Add" : "Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String id = inputId.getText().toString();
                String content = inputContent.getText().toString();
                float fontSize = 24;
                try {
                    fontSize = Float.parseFloat(inputFontSize.getText().toString());
                } catch (Exception e) {
                }
                boolean isBold = checkBold.isChecked();
                String selectedFont = fonts[spinnerFont.getSelectedItemPosition()];

                int align = 0;
                int checkedId = radioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.radio_center)
                    align = 1;
                else if (checkedId == R.id.radio_right)
                    align = 2;

                if (id.isEmpty())
                    id = "text_" + System.currentTimeMillis();

                if (element == null) {
                    printElements.add(new TextPrintElement(id, align, content, fontSize, isBold, selectedFont));
                } else {
                    element.setId(id);
                    element.setContent(content);
                    element.setFontSize(fontSize);
                    element.setBold(isBold);
                    element.setAlignment(align);
                    element.setTypeface(selectedFont);
                }
                adapter.notifyDataSetChanged();
            }
        });

        if (element != null) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    printElements.remove(element);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showImageDialog(final ImagePrintElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(element == null ? "Add Image" : "Edit Image");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_image, null);
        builder.setView(view);

        final EditText inputId = view.findViewById(R.id.input_id);
        tempInputWidth = view.findViewById(R.id.input_width);
        tempInputHeight = view.findViewById(R.id.input_height);
        Button btnSelect = view.findViewById(R.id.btn_select_image);
        tempImagePreview = view.findViewById(R.id.preview_image);
        final RadioGroup radioGroup = view.findViewById(R.id.radio_group_align);

        tempSelectedBitmap = null;
        editingImageElement = element;

        if (element != null) {
            inputId.setText(element.getId());
            if (element.getBitmap() != null) {
                tempImagePreview.setImageBitmap(element.getBitmap());
                tempSelectedBitmap = element.getBitmap(); // Keep reference to current bitmap
                tempInputWidth.setText(String.valueOf(element.getWidth()));
                tempInputHeight.setText(String.valueOf(element.getHeight()));
            }
            int align = element.getAlignment();
            if (align == 0)
                radioGroup.check(R.id.radio_left);
            else if (align == 1)
                radioGroup.check(R.id.radio_center);
            else if (align == 2)
                radioGroup.check(R.id.radio_right);
        }

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_IMAGE_PICK);
            }
        });

        builder.setPositiveButton(element == null ? "Add" : "Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String id = inputId.getText().toString();
                if (id.isEmpty())
                    id = "image_" + System.currentTimeMillis();
                int align = 1;
                int checkedId = radioGroup.getCheckedRadioButtonId();
                if (checkedId == R.id.radio_left)
                    align = 0;
                else if (checkedId == R.id.radio_right)
                    align = 2;

                // Process Dimensions
                int targetWidth = 0;
                int targetHeight = 0;
                try {
                    targetWidth = Integer.parseInt(tempInputWidth.getText().toString());
                    targetHeight = Integer.parseInt(tempInputHeight.getText().toString());
                } catch (Exception e) {
                }

                // Resize Bitmap if needed
                Bitmap finalBitmap = tempSelectedBitmap;
                if (finalBitmap != null && targetWidth > 0 && targetHeight > 0) {
                    if (finalBitmap.getWidth() != targetWidth || finalBitmap.getHeight() != targetHeight) {
                        finalBitmap = Bitmap.createScaledBitmap(finalBitmap, targetWidth, targetHeight, false);
                    }
                }

                if (element == null) {
                    if (finalBitmap == null) {
                        Toast.makeText(EditLayoutActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    printElements.add(new ImagePrintElement(id, align, finalBitmap, targetWidth, targetHeight));
                } else {
                    element.setId(id);
                    element.setAlignment(align);
                    element.setWidth(targetWidth);
                    element.setHeight(targetHeight);
                    if (finalBitmap != null) {
                        element.setBitmap(finalBitmap);
                        // Force save of new bitmap
                        element.setImagePath(null);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        if (element != null) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    printElements.remove(element);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSpaceDialog(final SpacePrintElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(element == null ? "Add Space" : "Edit Space");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_space, null);
        builder.setView(view);

        final EditText inputId = view.findViewById(R.id.input_id);
        final EditText inputHeight = view.findViewById(R.id.input_height);

        if (element != null) {
            inputId.setText(element.getId());
            inputHeight.setText(String.valueOf(element.getHeight()));
        }

        builder.setPositiveButton(element == null ? "Add" : "Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String id = inputId.getText().toString();
                int height = 1;
                try {
                    height = Integer.parseInt(inputHeight.getText().toString());
                } catch (Exception e) {
                }

                if (id.isEmpty())
                    id = "space_" + System.currentTimeMillis();

                if (element == null) {
                    printElements.add(new SpacePrintElement(id, height));
                } else {
                    element.setId(id);
                    element.setHeight(height);
                }
                adapter.notifyDataSetChanged();
            }
        });

        if (element != null) {
            builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    printElements.remove(element);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                tempSelectedBitmap = BitmapFactory.decodeStream(imageStream);

                // Set initial dimensions in input fields
                if (tempSelectedBitmap != null) {
                    if (tempInputWidth != null) {
                        tempInputWidth.setText(String.valueOf(tempSelectedBitmap.getWidth()));
                    }
                    if (tempInputHeight != null) {
                        tempInputHeight.setText(String.valueOf(tempSelectedBitmap.getHeight()));
                    }
                }

                if (tempImagePreview != null) {
                    tempImagePreview.setImageBitmap(tempSelectedBitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void printAllElements() {
        if (printElements.isEmpty()) {
            Toast.makeText(this, "List is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                SunmiPrintHelper helper = SunmiPrintHelper.getInstance();
                for (PrintElement element : printElements) {
                    helper.printElement(element);
                }
                helper.feedPaper(1);
            }
        }).start();
    }

    private void saveLayoutInternal() {
        if (currentLayoutIndex != -1 && currentLayoutName != null) {
            // Overwrite existing
            performSave(currentLayoutName, true);
        } else {
            // New layout, ask for name
            showSaveAsDialog();
        }
    }

    private void showSaveAsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Layout As");
        final EditText inputName = new EditText(this);
        inputName.setHint("Layout Name");
        if (currentLayoutName != null) {
            inputName.setText(currentLayoutName + "_copy");
        }
        builder.setView(inputName);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputName.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(EditLayoutActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                performSave(name, false);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performSave(String name, boolean overwrite) {
        // Save Images to Internal Storage first
        for (PrintElement element : printElements) {
            if (element instanceof ImagePrintElement) {
                ImagePrintElement img = (ImagePrintElement) element;
                if (img.getImagePath() == null && img.getBitmap() != null) {
                    // Save bitmap
                    String path = com.sunmiprinter.app.utils.LayoutStorageManager
                            .saveBitmapToInternal(EditLayoutActivity.this, img.getBitmap());
                    img.setImagePath(path);
                }
            }
        }

        PrintLayout layout = new PrintLayout(name, printElements);
        List<PrintLayout> layouts = com.sunmiprinter.app.utils.LayoutStorageManager
                .loadLayouts(EditLayoutActivity.this);

        if (overwrite && currentLayoutIndex >= 0 && currentLayoutIndex < layouts.size()) {
            layouts.set(currentLayoutIndex, layout);
            Toast.makeText(EditLayoutActivity.this, "Layout Overwritten", Toast.LENGTH_SHORT).show();
        } else {
            // Save As New
            layouts.add(layout);
            currentLayoutIndex = layouts.size() - 1; // Update to point to the new one
            currentLayoutName = name;
            Toast.makeText(EditLayoutActivity.this, "Saved as new layout: " + name, Toast.LENGTH_SHORT).show();
        }

        com.sunmiprinter.app.utils.LayoutStorageManager.saveLayouts(EditLayoutActivity.this, layouts);
        // Do NOT finish. Stay in editor.
    }
}

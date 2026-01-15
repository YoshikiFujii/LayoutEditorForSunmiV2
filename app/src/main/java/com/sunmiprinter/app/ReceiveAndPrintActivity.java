package com.sunmiprinter.app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.ScrollingMovementMethod;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sunmiprinter.app.model.PrintElement;
import com.sunmiprinter.app.model.PrintLayout;
import com.sunmiprinter.app.model.TextPrintElement;
import com.sunmiprinter.app.utils.LayoutStorageManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;

public class ReceiveAndPrintActivity extends AppCompatActivity {

    private static final String TAG = "ReceiveAndPrint";
    private static final String NAME = "SunmiPrinterServer";
    private static final UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); // Custom UUID

    private TextView textStatus;
    private TextView textReceived;
    private Button btnCancel;

    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private PrintLayout currentLayout;
    private String originalLayoutJson;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_print);

        textStatus = findViewById(R.id.text_status);
        textReceived = findViewById(R.id.text_received_content);
        textReceived.setMovementMethod(new ScrollingMovementMethod());
        btnCancel = findViewById(R.id.btn_cancel);

        String layoutJson = getIntent().getStringExtra("layout_data");
        if (layoutJson != null) {
            originalLayoutJson = layoutJson;
            // Validate JSON
            try {
                LayoutStorageManager.createGson().fromJson(originalLayoutJson, PrintLayout.class);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid layout data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "No layout data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show();
            // In a real app, we should ask for permission/enable it via Intent
            // For now, assuming user will enable it or it is enabled.
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanup();
                finish();
            }
        });

        // specific connection for this activity
        SunmiPrintHelper.getInstance().connect(this, new SunmiPrintHelper.OnConnectionStatusListener() {
            @Override
            public void onConnected() {
                updateStatus("Printer Connected. Waiting for connection...");
                startServer();
            }

            @Override
            public void onDisconnected() {
                updateStatus("Printer Disconnected");
            }
        });
    }

    @SuppressLint("MissingPermission") // Permissions should be handled by caller/manifest
    private void startServer() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
        updateStatus("Waiting for connection...");
    }

    private void updateStatus(final String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                textStatus.setText(status);
            }
        });
    }

    private void appendLog(final String log) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                textReceived.append("\n" + log);
            }
        });
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // Try Insecure connection for better compatibility
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    if (mmServerSocket == null)
                        break;
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null)
                    mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        updateStatus("Connected! Receiving data...");
        // Use standard InputStream read to avoid newline dependency
        try (InputStream inputStream = socket.getInputStream()) {
            StringBuilder buffer = new StringBuilder();
            byte[] bytes = new byte[1024];
            int bytesRead;

            // Read until the connection is closed or timeout?
            // For now, read in loop. Typical file transfer closes stream at end.
            while ((bytesRead = inputStream.read(bytes)) != -1) {
                String part = new String(bytes, 0, bytesRead);
                buffer.append(part);
                appendLog("Read " + bytesRead + " bytes chunk...");

                // Heuristic: If we have a complete JSON object, stop reading
                String currentStr = buffer.toString().trim();
                if (currentStr.startsWith("{") && currentStr.endsWith("}")) {
                    break;
                }
            }

            final String receivedData = buffer.toString();
            Log.d(TAG, "Received: " + receivedData);
            appendLog("Received data: " + receivedData);

            processReceivedData(receivedData);

            // Cleanup
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading data", e);
            updateStatus("Error receiving data");
        } finally {
            // Reset/Restart logic
            try {
                // Ensure socket is closed
                if (socket != null && socket.isConnected()) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Give Bluetooth stack a moment to reset
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            startServer();
        }
    }

    private void processReceivedData(String jsonString) {
        try {
            // Reset layout from original
            currentLayout = LayoutStorageManager.createGson().fromJson(originalLayoutJson, PrintLayout.class);
            LayoutStorageManager.reloadBitmaps(currentLayout);

            Gson gson = new Gson();
            Map<String, String> data = gson.fromJson(jsonString, new TypeToken<Map<String, String>>() {
            }.getType());

            if (data == null || data.isEmpty()) {
                appendLog("Error: Empty or invalid JSON map");
                appendLog("Proceeding to print original layout...");
                printLayout();
                return;
            }
            appendLog("JSON Parsed. Processing " + data.size() + " entries...");

            StringBuilder errorBuilder = new StringBuilder();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                String id = entry.getKey();
                String val = entry.getValue();

                boolean found = false;
                for (PrintElement element : currentLayout.getElements()) {
                    if (element instanceof TextPrintElement) {
                        TextPrintElement textElement = (TextPrintElement) element;
                        if (id.equals(textElement.getId())) {
                            // User Rule: Replace "¥n" with actual newline
                            String content = val == null ? "" : val;
                            content = content.replace("¥n", "\n");

                            // User Rule: "¥|" for Table (Left | Right)
                            if (content.contains("¥|")) {
                                String[] parts = content.split("¥\\|"); // escaping pipe for regex
                                if (parts.length >= 2) {
                                    // Override standard text printing by clearing content and printing table
                                    // directly?
                                    // PrintElement structure doesn't support multiple parts easily.
                                    // So we print it immediately? But `processReceivedData` iterates and updates
                                    // layout, then prints later.
                                    // We need to change how printing works or "inject" this logic.

                                    // Hack: If we set content to empty, it prints empty.
                                    // We can't easily inject a Table Element into the layout if it's not supported
                                    // by the model.
                                    // However, `printLayout` calls `helper.printElement`.
                                    // We can create a Custom PrintElement or handle "¥|" inside `printLayout` or
                                    // `SunmiPrintHelper.printElement`.

                                    // Better approach: Let `SunmiPrintHelper.printElement` handle the "¥|" logic if
                                    // it's a TextElement.
                                    // So here we just set the content with "¥|" preserved (but newline replaced).
                                    // And modify SunmiPrintHelper to detect it.
                                    // But I already modified this file.

                                    // Wait, `printLayout` is in THIS activity.
                                    // Let's modify `printLayout` loop below instead.
                                    textElement.setContent(content); // Keep "¥|"
                                } else {
                                    textElement.setContent(content);
                                }
                            } else {
                                textElement.setContent(content);
                            }

                            found = true;
                            appendLog("Updated " + id);
                            break; // Assumes unique IDs
                        }
                    }
                }

                if (!found) {
                    errorBuilder.append("ID not found: ").append(id).append("\n");
                }
            }

            if (errorBuilder.length() > 0) {
                final String errorMsg = errorBuilder.toString();
                appendLog("Errors:\n" + errorMsg);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReceiveAndPrintActivity.this, "Some IDs not found", Toast.LENGTH_LONG).show();
                    }
                });
            }

            printLayout();

        } catch (Exception e) {
            e.printStackTrace();
            appendLog("Error parsing JSON: " + e.getMessage());
            // Even on error, maybe print original?
            // "Print as layout" implies we try to print.
            if (currentLayout != null)
                printLayout();
        }
    }

    private void printLayout() {
        if (SunmiPrintHelper.getInstance().getConnectionStatus() != 2) { // 2 = Connected
            appendLog("Error: Printer Service NOT Connected!");
            return;
        }

        updateStatus("Printing...");
        appendLog("Starting Print Job...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                SunmiPrintHelper helper = SunmiPrintHelper.getInstance();
                try {
                    int count = 0;
                    for (PrintElement element : currentLayout.getElements()) {
                        String type = element.getType();
                        String id = element.getId();
                        appendLog("Printing Item " + (++count) + ": " + type + " (ID: " + id + ")");

                        if (element instanceof TextPrintElement) {
                            String txt = ((TextPrintElement) element).getContent();
                            if (txt != null && txt.contains("¥|")) {
                                // Handle multiline content
                                String[] lines = txt.split("\n");
                                for (String line : lines) {
                                    boolean isSpecialStyle = false;
                                    String contentToPrint = line;

                                    // Check for Style Rule: starts with ##
                                    if (contentToPrint.startsWith("##")) {
                                        isSpecialStyle = true;
                                        contentToPrint = contentToPrint.substring(2); // Remove ## prefix
                                        // If it ends with ## (legacy rule or typo), remove it too just in case
                                        if (contentToPrint.endsWith("##")) {
                                            contentToPrint = contentToPrint.substring(0, contentToPrint.length() - 2);
                                        }

                                        helper.setBold(true);
                                        helper.setFontSize(30);
                                    }

                                    // Check for ¥| separator
                                    if (contentToPrint.contains("¥|")) {
                                        String[] parts = contentToPrint.split("¥\\|");
                                        // Expecting "Name ¥| Price ¥|" or "Name ¥| Price"
                                        if (parts.length >= 2) {
                                            String left = parts[0];
                                            String right = parts[1];
                                            helper.printTable(new String[] { left, right }, new int[] { 2, 1 },
                                                    new int[] { 0, 2 }); // 0=Left, 2=Right
                                        } else {
                                            // Only one part found? Fallback
                                            ((TextPrintElement) element).setContent(contentToPrint);
                                            helper.printElement(element);
                                        }
                                    } else {
                                        // Normal line
                                        ((TextPrintElement) element).setContent(contentToPrint);
                                        helper.printElement(element);
                                    }

                                    // Reset Style
                                    if (isSpecialStyle) {
                                        helper.setBold(false);
                                        helper.setFontSize(24); // Default
                                    }
                                }
                                continue; // Skip standard printing for this element
                            }
                        }

                        helper.printElement(element);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    helper.feedPaper(3);
                    appendLog("Print Job Completed.");
                    updateStatus("Printing Completed. Waiting for next...");
                } catch (Exception e) {
                    e.printStackTrace();
                    appendLog("Print Error: " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    private void cleanup() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }
}

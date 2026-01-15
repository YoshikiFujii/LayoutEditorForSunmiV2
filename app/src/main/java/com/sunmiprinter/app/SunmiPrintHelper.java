package com.sunmiprinter.app;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class SunmiPrintHelper {
    private static final String TAG = "SunmiPrintHelper";
    private static SunmiPrintHelper instance = new SunmiPrintHelper();
    private SunmiPrinterService sunmiPrinterService;
    private int connectionStatus = 0; // 0: disconnected, 1: connecting, 2: connected

    private SunmiPrintHelper() {
    }

    public static SunmiPrintHelper getInstance() {
        return instance;
    }

    public void connect(Context context, final OnConnectionStatusListener listener) {
        if (sunmiPrinterService != null) {
            if (listener != null)
                listener.onConnected();
            return;
        }

        try {
            boolean result = InnerPrinterManager.getInstance().bindService(context, new InnerPrinterCallback() {
                @Override
                protected void onConnected(SunmiPrinterService service) {
                    sunmiPrinterService = service;
                    connectionStatus = 2;
                    Log.d(TAG, "Service Connected");
                    if (listener != null)
                        listener.onConnected();
                }

                @Override
                protected void onDisconnected() {
                    sunmiPrinterService = null;
                    connectionStatus = 0;
                    Log.d(TAG, "Service Disconnected");
                    if (listener != null)
                        listener.onDisconnected();
                }
            });

            if (!result) {
                if (listener != null)
                    listener.onDisconnected();
            }
        } catch (InnerPrinterException e) {
            e.printStackTrace();
            if (listener != null)
                listener.onDisconnected();
        }
    }

    public void printElement(com.sunmiprinter.app.model.PrintElement element) {
        if (sunmiPrinterService == null)
            return;

        try {
            // Set Alignment
            sunmiPrinterService.setAlignment(element.getAlignment(), null);

            if (element instanceof com.sunmiprinter.app.model.TextPrintElement) {
                com.sunmiprinter.app.model.TextPrintElement textElement = (com.sunmiprinter.app.model.TextPrintElement) element;

                // Set Style (Bold)
                if (textElement.isBold()) {
                    sunmiPrinterService.sendRAWData(new byte[] { 0x1B, 0x45, 0x01 }, null); // Bold ON
                } else {
                    sunmiPrinterService.sendRAWData(new byte[] { 0x1B, 0x45, 0x00 }, null); // Bold OFF
                }

                // Print Text with Font Size
                // Pass Typeface if available
                String typeface = textElement.getTypeface();
                if (typeface == null)
                    typeface = "";

                String content = textElement.getContent();
                if (content == null)
                    content = "";

                Log.d(TAG, "Printing Text: " + content);
                sunmiPrinterService.printTextWithFont(content + "\n", typeface,
                        textElement.getFontSize(), null);

                // Reset Bold
                sunmiPrinterService.sendRAWData(new byte[] { 0x1B, 0x45, 0x00 }, null);

            } else if (element instanceof com.sunmiprinter.app.model.ImagePrintElement) {
                com.sunmiprinter.app.model.ImagePrintElement imageElement = (com.sunmiprinter.app.model.ImagePrintElement) element;
                if (imageElement.getBitmap() != null) {
                    sunmiPrinterService.printBitmap(imageElement.getBitmap(), null);
                    sunmiPrinterService.lineWrap(1, null); // Spacing after image
                }
            } else if (element instanceof com.sunmiprinter.app.model.SpacePrintElement) {
                com.sunmiprinter.app.model.SpacePrintElement spaceElement = (com.sunmiprinter.app.model.SpacePrintElement) element;
                sunmiPrinterService.lineWrap(spaceElement.getHeight(), null);
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printTable(String[] cols, int[] weights, int[] aligns) {
        if (sunmiPrinterService == null)
            return;
        try {
            sunmiPrinterService.printColumnsString(cols, weights, aligns, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void feedPaper(int lines) {
        if (sunmiPrinterService == null)
            return;
        try {
            sunmiPrinterService.lineWrap(lines, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setFontSize(float size) {
        if (sunmiPrinterService == null)
            return;
        try {
            sunmiPrinterService.setFontSize(size, null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setBold(boolean isBold) {
        if (sunmiPrinterService == null)
            return;
        try {
            if (isBold) {
                sunmiPrinterService.sendRAWData(new byte[] { 0x1B, 0x45, 0x01 }, null);
            } else {
                sunmiPrinterService.sendRAWData(new byte[] { 0x1B, 0x45, 0x00 }, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public interface OnConnectionStatusListener {
        void onConnected();

        void onDisconnected();
    }
}

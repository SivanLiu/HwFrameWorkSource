package com.android.server.usb.descriptors.report;

import android.hardware.usb.UsbDeviceConnection;
import android.net.util.NetworkConstants;

public abstract class ReportCanvas {
    private static final String TAG = "ReportCanvas";
    private final UsbDeviceConnection mConnection;

    public abstract void closeHeader(int i);

    public abstract void closeList();

    public abstract void closeListItem();

    public abstract void closeParagraph();

    public abstract void openHeader(int i);

    public abstract void openList();

    public abstract void openListItem();

    public abstract void openParagraph(boolean z);

    public abstract void write(String str);

    public abstract void writeParagraph(String str, boolean z);

    public ReportCanvas(UsbDeviceConnection connection) {
        this.mConnection = connection;
    }

    public UsbDeviceConnection getConnection() {
        return this.mConnection;
    }

    public void writeHeader(int level, String text) {
        openHeader(level);
        write(text);
        closeHeader(level);
    }

    public void writeListItem(String text) {
        openListItem();
        write(text);
        closeListItem();
    }

    public static String getHexString(byte value) {
        return "0x" + Integer.toHexString(value & 255).toUpperCase();
    }

    public static String getBCDString(int valueBCD) {
        return "" + ((valueBCD >> 8) & 15) + "." + ((valueBCD >> 4) & 15) + (valueBCD & 15);
    }

    public static String getHexString(int value) {
        return "0x" + Integer.toHexString(value & NetworkConstants.ARP_HWTYPE_RESERVED_HI).toUpperCase();
    }

    public void dumpHexArray(byte[] rawData, StringBuilder builder) {
        if (rawData != null) {
            openParagraph(false);
            for (byte hexString : rawData) {
                builder.append(getHexString(hexString)).append(" ");
            }
            closeParagraph();
        }
    }
}

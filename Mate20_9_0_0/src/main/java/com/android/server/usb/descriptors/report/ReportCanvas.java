package com.android.server.usb.descriptors.report;

import android.net.util.NetworkConstants;
import com.android.server.usb.descriptors.UsbDescriptorParser;

public abstract class ReportCanvas {
    private static final String TAG = "ReportCanvas";
    private final UsbDescriptorParser mParser;

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

    public ReportCanvas(UsbDescriptorParser parser) {
        this.mParser = parser;
    }

    public UsbDescriptorParser getParser() {
        return this.mParser;
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(value & 255).toUpperCase());
        return stringBuilder.toString();
    }

    public static String getBCDString(int valueBCD) {
        int major = (valueBCD >> 8) & 15;
        int minor = (valueBCD >> 4) & 15;
        int subminor = valueBCD & 15;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(major);
        stringBuilder.append(".");
        stringBuilder.append(minor);
        stringBuilder.append(subminor);
        return stringBuilder.toString();
    }

    public static String getHexString(int value) {
        int intValue = NetworkConstants.ARP_HWTYPE_RESERVED_HI & value;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(intValue).toUpperCase());
        return stringBuilder.toString();
    }

    public void dumpHexArray(byte[] rawData, StringBuilder builder) {
        if (rawData != null) {
            int index = 0;
            openParagraph(false);
            while (index < rawData.length) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getHexString(rawData[index]));
                stringBuilder.append(" ");
                builder.append(stringBuilder.toString());
                index++;
            }
            closeParagraph();
        }
    }
}

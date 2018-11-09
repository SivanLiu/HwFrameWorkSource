package com.android.server.usb.descriptors.report;

import android.hardware.usb.UsbDeviceConnection;

public final class TextReportCanvas extends ReportCanvas {
    private static final int LIST_INDENT_AMNT = 2;
    private static final String TAG = "TextReportCanvas";
    private int mListIndent;
    private final StringBuilder mStringBuilder;

    public TextReportCanvas(UsbDeviceConnection connection, StringBuilder stringBuilder) {
        super(connection);
        this.mStringBuilder = stringBuilder;
    }

    private void writeListIndent() {
        for (int space = 0; space < this.mListIndent; space++) {
            this.mStringBuilder.append(" ");
        }
    }

    public void write(String text) {
        this.mStringBuilder.append(text);
    }

    public void openHeader(int level) {
        writeListIndent();
        this.mStringBuilder.append("[");
    }

    public void closeHeader(int level) {
        this.mStringBuilder.append("]\n");
    }

    public void openParagraph(boolean emphasis) {
        writeListIndent();
    }

    public void closeParagraph() {
        this.mStringBuilder.append("\n");
    }

    public void writeParagraph(String text, boolean inRed) {
        openParagraph(inRed);
        if (inRed) {
            this.mStringBuilder.append("*").append(text).append("*");
        } else {
            this.mStringBuilder.append(text);
        }
        closeParagraph();
    }

    public void openList() {
        this.mListIndent += 2;
    }

    public void closeList() {
        this.mListIndent -= 2;
    }

    public void openListItem() {
        writeListIndent();
        this.mStringBuilder.append("- ");
    }

    public void closeListItem() {
        this.mStringBuilder.append("\n");
    }
}

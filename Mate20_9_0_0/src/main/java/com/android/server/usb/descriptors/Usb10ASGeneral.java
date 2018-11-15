package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public final class Usb10ASGeneral extends UsbACInterface {
    private static final String TAG = "Usb10ASGeneral";
    private byte mDelay;
    private int mFormatTag;
    private byte mTerminalLink;

    public Usb10ASGeneral(int length, byte type, byte subtype, int subclass) {
        super(length, type, subtype, subclass);
    }

    public byte getTerminalLink() {
        return this.mTerminalLink;
    }

    public byte getDelay() {
        return this.mDelay;
    }

    public int getFormatTag() {
        return this.mFormatTag;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mTerminalLink = stream.getByte();
        this.mDelay = stream.getByte();
        this.mFormatTag = stream.unpackUsbShort();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Delay: ");
        stringBuilder.append(this.mDelay);
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Terminal Link: ");
        stringBuilder.append(this.mTerminalLink);
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Format: ");
        stringBuilder.append(UsbStrings.getAudioFormatName(this.mFormatTag));
        stringBuilder.append(" - ");
        stringBuilder.append(ReportCanvas.getHexString(this.mFormatTag));
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

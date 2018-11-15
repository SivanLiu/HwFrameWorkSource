package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACOutputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb10ACOutputTerminal";
    private byte mSourceID;
    private byte mTerminal;

    public Usb10ACOutputTerminal(int length, byte type, byte subtype, int subClass) {
        super(length, type, subtype, subClass);
    }

    public byte getSourceID() {
        return this.mSourceID;
    }

    public byte getTerminal() {
        return this.mTerminal;
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        this.mSourceID = stream.getByte();
        this.mTerminal = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Source ID: ");
        stringBuilder.append(ReportCanvas.getHexString(getSourceID()));
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbACTerminal extends UsbACInterface {
    private static final String TAG = "UsbACTerminal";
    protected byte mAssocTerminal;
    protected byte mTerminalID;
    protected int mTerminalType;

    public UsbACTerminal(int length, byte type, byte subtype, int subclass) {
        super(length, type, subtype, subclass);
    }

    public byte getTerminalID() {
        return this.mTerminalID;
    }

    public int getTerminalType() {
        return this.mTerminalType;
    }

    public byte getAssocTerminal() {
        return this.mAssocTerminal;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mTerminalID = stream.getByte();
        this.mTerminalType = stream.unpackUsbShort();
        this.mAssocTerminal = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        int terminalType = getTerminalType();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Type: ");
        stringBuilder.append(ReportCanvas.getHexString(terminalType));
        stringBuilder.append(": ");
        stringBuilder.append(UsbStrings.getTerminalName(terminalType));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("ID: ");
        stringBuilder.append(ReportCanvas.getHexString(getTerminalID()));
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb20ASGeneral extends UsbACInterface {
    private static final String TAG = "Usb20ASGeneral";
    private int mChannelConfig;
    private byte mChannelNames;
    private byte mControls;
    private byte mFormatType;
    private int mFormats;
    private byte mNumChannels;
    private byte mTerminalLink;

    public Usb20ASGeneral(int length, byte type, byte subtype, int subclass) {
        super(length, type, subtype, subclass);
    }

    public byte getTerminalLink() {
        return this.mTerminalLink;
    }

    public byte getControls() {
        return this.mControls;
    }

    public byte getFormatType() {
        return this.mFormatType;
    }

    public int getFormats() {
        return this.mFormats;
    }

    public byte getNumChannels() {
        return this.mNumChannels;
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChannelNames() {
        return this.mChannelNames;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mTerminalLink = stream.getByte();
        this.mControls = stream.getByte();
        this.mFormatType = stream.getByte();
        this.mFormats = stream.unpackUsbInt();
        this.mNumChannels = stream.getByte();
        this.mChannelConfig = stream.unpackUsbInt();
        this.mChannelNames = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Terminal Link: ");
        stringBuilder.append(getTerminalLink());
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Controls: ");
        stringBuilder.append(ReportCanvas.getHexString(getControls()));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Format Type: ");
        stringBuilder.append(ReportCanvas.getHexString(getFormatType()));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Formats: ");
        stringBuilder.append(ReportCanvas.getHexString(getFormats()));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Num Channels: ");
        stringBuilder.append(getNumChannels());
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Channel Config: ");
        stringBuilder.append(ReportCanvas.getHexString(getChannelConfig()));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Channel Names String ID: ");
        stringBuilder.append(getChannelNames());
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

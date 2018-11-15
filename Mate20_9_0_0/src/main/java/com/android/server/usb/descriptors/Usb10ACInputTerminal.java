package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACInputTerminal extends UsbACTerminal {
    private static final String TAG = "Usb10ACInputTerminal";
    private int mChannelConfig;
    private byte mChannelNames;
    private byte mNrChannels;
    private byte mTerminal;

    public Usb10ACInputTerminal(int length, byte type, byte subtype, int subclass) {
        super(length, type, subtype, subclass);
    }

    public byte getNrChannels() {
        return this.mNrChannels;
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChannelNames() {
        return this.mChannelNames;
    }

    public byte getTerminal() {
        return this.mTerminal;
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        this.mNrChannels = stream.getByte();
        this.mChannelConfig = stream.unpackUsbShort();
        this.mChannelNames = stream.getByte();
        this.mTerminal = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Associated Terminal: ");
        stringBuilder.append(ReportCanvas.getHexString(getAssocTerminal()));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(getNrChannels());
        stringBuilder.append(" Chans. Config: ");
        stringBuilder.append(ReportCanvas.getHexString(getChannelConfig()));
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbACMidiEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACMidiEndpoint";
    private byte[] mJackIds;
    private byte mNumJacks;

    public UsbACMidiEndpoint(int length, byte type, int subclass) {
        super(length, type, subclass);
    }

    public byte getNumJacks() {
        return this.mNumJacks;
    }

    public byte[] getJackIds() {
        return this.mJackIds;
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        this.mNumJacks = stream.getByte();
        this.mJackIds = new byte[this.mNumJacks];
        for (byte jack = (byte) 0; jack < this.mNumJacks; jack++) {
            this.mJackIds[jack] = stream.getByte();
        }
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AC Midi Endpoint: ");
        stringBuilder.append(ReportCanvas.getHexString(getType()));
        stringBuilder.append(" Length: ");
        stringBuilder.append(getLength());
        canvas.writeHeader(3, stringBuilder.toString());
        canvas.openList();
        stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(getNumJacks());
        stringBuilder.append(" Jacks.");
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

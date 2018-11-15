package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbACMidiEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACMidiEndpoint";
    private byte[] mJackIds;
    private byte mNumJacks;

    public /* bridge */ /* synthetic */ byte getSubclass() {
        return super.getSubclass();
    }

    public /* bridge */ /* synthetic */ byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACMidiEndpoint(int length, byte type, byte subclass) {
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
        canvas.writeHeader(3, "AC Midi Endpoint: " + ReportCanvas.getHexString(getType()) + " Length: " + getLength());
        canvas.openList();
        canvas.writeListItem("" + getNumJacks() + " Jacks.");
        canvas.closeList();
    }
}

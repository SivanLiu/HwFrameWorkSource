package com.android.server.usb.descriptors;

public class UsbACAudioStreamEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACAudioStreamEndpoint";

    public /* bridge */ /* synthetic */ byte getSubclass() {
        return super.getSubclass();
    }

    public /* bridge */ /* synthetic */ byte getSubtype() {
        return super.getSubtype();
    }

    public UsbACAudioStreamEndpoint(int length, byte type, byte subclass) {
        super(length, type, subclass);
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        stream.advance(this.mLength - stream.getReadCount());
        return this.mLength;
    }
}

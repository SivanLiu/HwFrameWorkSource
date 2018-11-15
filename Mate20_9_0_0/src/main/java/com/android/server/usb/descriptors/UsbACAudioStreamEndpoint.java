package com.android.server.usb.descriptors;

public class UsbACAudioStreamEndpoint extends UsbACEndpoint {
    private static final String TAG = "UsbACAudioStreamEndpoint";

    public UsbACAudioStreamEndpoint(int length, byte type, int subclass) {
        super(length, type, subclass);
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        stream.advance(this.mLength - stream.getReadCount());
        return this.mLength;
    }
}

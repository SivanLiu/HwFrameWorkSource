package com.android.server.usb.descriptors;

import android.util.Log;

abstract class UsbACEndpoint extends UsbDescriptor {
    private static final String TAG = "UsbACEndpoint";
    protected final byte mSubclass;
    protected byte mSubtype;

    UsbACEndpoint(int length, byte type, byte subclass) {
        super(length, type);
        this.mSubclass = subclass;
    }

    public byte getSubclass() {
        return this.mSubclass;
    }

    public byte getSubtype() {
        return this.mSubtype;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mSubtype = stream.getByte();
        return this.mLength;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, int length, byte type) {
        byte subClass = parser.getCurInterface().getUsbSubclass();
        switch (subClass) {
            case (byte) 1:
                return new UsbACAudioControlEndpoint(length, type, subClass);
            case (byte) 2:
                return new UsbACAudioStreamEndpoint(length, type, subClass);
            case (byte) 3:
                return new UsbACMidiEndpoint(length, type, subClass);
            default:
                Log.w(TAG, "Unknown Audio Class Endpoint id:0x" + Integer.toHexString(subClass));
                return null;
        }
    }
}

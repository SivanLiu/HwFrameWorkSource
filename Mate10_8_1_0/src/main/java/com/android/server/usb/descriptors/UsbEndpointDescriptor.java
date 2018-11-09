package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public class UsbEndpointDescriptor extends UsbDescriptor {
    public static final byte DIRECTION_INPUT = Byte.MIN_VALUE;
    public static final byte DIRECTION_OUTPUT = (byte) 0;
    public static final byte MASK_ATTRIBS_SYNCTYPE = (byte) 12;
    public static final byte MASK_ATTRIBS_TRANSTYPE = (byte) 3;
    public static final byte MASK_ATTRIBS_USEAGE = (byte) 48;
    public static final byte MASK_ENDPOINT_ADDRESS = (byte) 15;
    public static final byte MASK_ENDPOINT_DIRECTION = Byte.MIN_VALUE;
    public static final byte SYNCTYPE_ADAPTSYNC = (byte) 8;
    public static final byte SYNCTYPE_ASYNC = (byte) 4;
    public static final byte SYNCTYPE_NONE = (byte) 0;
    public static final byte SYNCTYPE_RESERVED = (byte) 12;
    private static final String TAG = "UsbEndpointDescriptor";
    public static final byte TRANSTYPE_BULK = (byte) 2;
    public static final byte TRANSTYPE_CONTROL = (byte) 0;
    public static final byte TRANSTYPE_INTERRUPT = (byte) 3;
    public static final byte TRANSTYPE_ISO = (byte) 1;
    public static final byte USEAGE_DATA = (byte) 0;
    public static final byte USEAGE_EXPLICIT = (byte) 32;
    public static final byte USEAGE_FEEDBACK = (byte) 16;
    public static final byte USEAGE_RESERVED = (byte) 48;
    private byte mAttributes;
    private byte mEndpointAddress;
    private byte mInterval;
    private int mPacketSize;
    private byte mRefresh;
    private byte mSyncAddress;

    public UsbEndpointDescriptor(int length, byte type) {
        super(length, type);
        this.mHierarchyLevel = 4;
    }

    public byte getEndpointAddress() {
        return this.mEndpointAddress;
    }

    public byte getAttributes() {
        return this.mAttributes;
    }

    public int getPacketSize() {
        return this.mPacketSize;
    }

    public byte getInterval() {
        return this.mInterval;
    }

    public byte getRefresh() {
        return this.mRefresh;
    }

    public byte getSyncAddress() {
        return this.mSyncAddress;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mEndpointAddress = stream.getByte();
        this.mAttributes = stream.getByte();
        this.mPacketSize = stream.unpackUsbShort();
        this.mInterval = stream.getByte();
        if (this.mLength == 9) {
            this.mRefresh = stream.getByte();
            this.mSyncAddress = stream.getByte();
        }
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        byte address = getEndpointAddress();
        canvas.writeListItem("Address: " + ReportCanvas.getHexString(address & 15) + ((address & -128) == 0 ? " [out]" : " [in]"));
        byte attributes = getAttributes();
        canvas.openListItem();
        canvas.write("Attributes: " + ReportCanvas.getHexString(attributes) + " ");
        switch (attributes & 3) {
            case 0:
                canvas.write("Control");
                break;
            case 1:
                canvas.write("Iso");
                break;
            case 2:
                canvas.write("Bulk");
                break;
            case 3:
                canvas.write("Interrupt");
                break;
        }
        canvas.closeListItem();
        if ((attributes & 3) == 1) {
            canvas.openListItem();
            canvas.write("Aync: ");
            switch (attributes & 12) {
                case 0:
                    canvas.write("NONE");
                    break;
                case 4:
                    canvas.write("ASYNC");
                    break;
                case 8:
                    canvas.write("ADAPTIVE ASYNC");
                    break;
            }
            canvas.closeListItem();
            canvas.openListItem();
            canvas.write("Useage: ");
            switch (attributes & 48) {
                case 0:
                    canvas.write("DATA");
                    break;
                case 16:
                    canvas.write("FEEDBACK");
                    break;
                case 32:
                    canvas.write("EXPLICIT FEEDBACK");
                    break;
                case 48:
                    canvas.write("RESERVED");
                    break;
            }
            canvas.closeListItem();
        }
        canvas.writeListItem("Package Size: " + getPacketSize());
        canvas.writeListItem("Interval: " + getInterval());
        canvas.closeList();
    }
}

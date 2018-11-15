package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public final class UsbDeviceDescriptor extends UsbDescriptor {
    private static final String TAG = "UsbDeviceDescriptor";
    public static final int USBSPEC_1_0 = 256;
    public static final int USBSPEC_1_1 = 272;
    public static final int USBSPEC_2_0 = 512;
    private byte mDevClass;
    private byte mDevSubClass;
    private int mDeviceRelease;
    private byte mMfgIndex;
    private byte mNumConfigs;
    private byte mPacketSize;
    private int mProductID;
    private byte mProductIndex;
    private byte mProtocol;
    private byte mSerialNum;
    private int mSpec;
    private int mVendorID;

    UsbDeviceDescriptor(int length, byte type) {
        super(length, type);
        this.mHierarchyLevel = 1;
    }

    public int getSpec() {
        return this.mSpec;
    }

    public byte getDevClass() {
        return this.mDevClass;
    }

    public byte getDevSubClass() {
        return this.mDevSubClass;
    }

    public byte getProtocol() {
        return this.mProtocol;
    }

    public byte getPacketSize() {
        return this.mPacketSize;
    }

    public int getVendorID() {
        return this.mVendorID;
    }

    public int getProductID() {
        return this.mProductID;
    }

    public int getDeviceRelease() {
        return this.mDeviceRelease;
    }

    public byte getMfgIndex() {
        return this.mMfgIndex;
    }

    public byte getProductIndex() {
        return this.mProductIndex;
    }

    public byte getSerialNum() {
        return this.mSerialNum;
    }

    public byte getNumConfigs() {
        return this.mNumConfigs;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mSpec = stream.unpackUsbShort();
        this.mDevClass = stream.getByte();
        this.mDevSubClass = stream.getByte();
        this.mProtocol = stream.getByte();
        this.mPacketSize = stream.getByte();
        this.mVendorID = stream.unpackUsbShort();
        this.mProductID = stream.unpackUsbShort();
        this.mDeviceRelease = stream.unpackUsbShort();
        this.mMfgIndex = stream.getByte();
        this.mProductIndex = stream.getByte();
        this.mSerialNum = stream.getByte();
        this.mNumConfigs = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("Spec: " + ReportCanvas.getBCDString(getSpec()));
        byte devClass = getDevClass();
        String classStr = UsbStrings.getClassName(devClass);
        byte devSubClass = getDevSubClass();
        canvas.writeListItem("Class " + devClass + ": " + classStr + " Subclass" + devSubClass + ": " + UsbStrings.getClassName(devSubClass));
        canvas.writeListItem("Vendor ID: " + ReportCanvas.getHexString(getVendorID()) + " Product ID: " + ReportCanvas.getHexString(getProductID()) + " Product Release: " + ReportCanvas.getBCDString(getDeviceRelease()));
        byte mfgIndex = getMfgIndex();
        String manufacturer = UsbDescriptor.getUsbDescriptorString(canvas.getConnection(), mfgIndex);
        byte productIndex = getProductIndex();
        canvas.writeListItem("Manufacturer " + mfgIndex + ": " + manufacturer + " Product " + productIndex + ": " + UsbDescriptor.getUsbDescriptorString(canvas.getConnection(), productIndex));
        canvas.closeList();
    }
}

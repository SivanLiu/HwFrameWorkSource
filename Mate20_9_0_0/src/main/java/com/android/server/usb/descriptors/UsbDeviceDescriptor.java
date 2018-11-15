package com.android.server.usb.descriptors;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;
import java.util.ArrayList;

public final class UsbDeviceDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbDeviceDescriptor";
    public static final int USBSPEC_1_0 = 256;
    public static final int USBSPEC_1_1 = 272;
    public static final int USBSPEC_2_0 = 512;
    private ArrayList<UsbConfigDescriptor> mConfigDescriptors;
    private int mDevClass;
    private int mDevSubClass;
    private int mDeviceRelease;
    private byte mMfgIndex;
    private byte mNumConfigs;
    private byte mPacketSize;
    private int mProductID;
    private byte mProductIndex;
    private int mProtocol;
    private byte mSerialIndex;
    private int mSpec;
    private int mVendorID;

    UsbDeviceDescriptor(int length, byte type) {
        super(length, type);
        this.mConfigDescriptors = new ArrayList();
        this.mHierarchyLevel = 1;
    }

    public int getSpec() {
        return this.mSpec;
    }

    public int getDevClass() {
        return this.mDevClass;
    }

    public int getDevSubClass() {
        return this.mDevSubClass;
    }

    public int getProtocol() {
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

    public String getDeviceReleaseString() {
        int hundredths = this.mDeviceRelease & 15;
        int tenths = (this.mDeviceRelease & 240) >> 4;
        int ones = (this.mDeviceRelease & 3840) >> 8;
        return String.format("%d.%d%d", new Object[]{Integer.valueOf((((this.mDeviceRelease & 61440) >> 12) * 10) + ones), Integer.valueOf(tenths), Integer.valueOf(hundredths)});
    }

    public byte getMfgIndex() {
        return this.mMfgIndex;
    }

    public String getMfgString(UsbDescriptorParser p) {
        return p.getDescriptorString(this.mMfgIndex);
    }

    public byte getProductIndex() {
        return this.mProductIndex;
    }

    public String getProductString(UsbDescriptorParser p) {
        return p.getDescriptorString(this.mProductIndex);
    }

    public byte getSerialIndex() {
        return this.mSerialIndex;
    }

    public String getSerialString(UsbDescriptorParser p) {
        return p.getDescriptorString(this.mSerialIndex);
    }

    public byte getNumConfigs() {
        return this.mNumConfigs;
    }

    void addConfigDescriptor(UsbConfigDescriptor config) {
        this.mConfigDescriptors.add(config);
    }

    public UsbDevice toAndroid(UsbDescriptorParser parser) {
        UsbDevice device = new UsbDevice(parser.getDeviceAddr(), this.mVendorID, this.mProductID, this.mDevClass, this.mDevSubClass, this.mProtocol, getMfgString(parser), getProductString(parser), getDeviceReleaseString(), getSerialString(parser));
        UsbConfiguration[] configs = new UsbConfiguration[this.mConfigDescriptors.size()];
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(configs.length);
        stringBuilder.append(" configs");
        Log.d(str, stringBuilder.toString());
        for (int index = 0; index < this.mConfigDescriptors.size(); index++) {
            configs[index] = ((UsbConfigDescriptor) this.mConfigDescriptors.get(index)).toAndroid(parser);
        }
        UsbDescriptorParser usbDescriptorParser = parser;
        device.setConfigurations(configs);
        return device;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mSpec = stream.unpackUsbShort();
        this.mDevClass = stream.getUnsignedByte();
        this.mDevSubClass = stream.getUnsignedByte();
        this.mProtocol = stream.getUnsignedByte();
        this.mPacketSize = stream.getByte();
        this.mVendorID = stream.unpackUsbShort();
        this.mProductID = stream.unpackUsbShort();
        this.mDeviceRelease = stream.unpackUsbShort();
        this.mMfgIndex = stream.getByte();
        this.mProductIndex = stream.getByte();
        this.mSerialIndex = stream.getByte();
        this.mNumConfigs = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        int spec = getSpec();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Spec: ");
        stringBuilder.append(ReportCanvas.getBCDString(spec));
        canvas.writeListItem(stringBuilder.toString());
        int devClass = getDevClass();
        String classStr = UsbStrings.getClassName(devClass);
        int devSubClass = getDevSubClass();
        String subClasStr = UsbStrings.getClassName(devSubClass);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Class ");
        stringBuilder2.append(devClass);
        stringBuilder2.append(": ");
        stringBuilder2.append(classStr);
        stringBuilder2.append(" Subclass");
        stringBuilder2.append(devSubClass);
        stringBuilder2.append(": ");
        stringBuilder2.append(subClasStr);
        canvas.writeListItem(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Vendor ID: ");
        stringBuilder2.append(ReportCanvas.getHexString(getVendorID()));
        stringBuilder2.append(" Product ID: ");
        stringBuilder2.append(ReportCanvas.getHexString(getProductID()));
        stringBuilder2.append(" Product Release: ");
        stringBuilder2.append(ReportCanvas.getBCDString(getDeviceRelease()));
        canvas.writeListItem(stringBuilder2.toString());
        UsbDescriptorParser parser = canvas.getParser();
        byte mfgIndex = getMfgIndex();
        String manufacturer = parser.getDescriptorString(mfgIndex);
        byte productIndex = getProductIndex();
        String product = parser.getDescriptorString(productIndex);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Manufacturer ");
        stringBuilder3.append(mfgIndex);
        stringBuilder3.append(": ");
        stringBuilder3.append(manufacturer);
        stringBuilder3.append(" Product ");
        stringBuilder3.append(productIndex);
        stringBuilder3.append(": ");
        stringBuilder3.append(product);
        canvas.writeListItem(stringBuilder3.toString());
        canvas.closeList();
    }
}

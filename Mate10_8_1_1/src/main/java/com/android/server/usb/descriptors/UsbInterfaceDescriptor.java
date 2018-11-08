package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public class UsbInterfaceDescriptor extends UsbDescriptor {
    private static final String TAG = "UsbInterfaceDescriptor";
    protected byte mAlternateSetting;
    protected byte mDescrIndex;
    protected byte mInterfaceNumber;
    protected byte mNumEndpoints;
    protected byte mProtocol;
    protected byte mUsbClass;
    protected byte mUsbSubclass;

    UsbInterfaceDescriptor(int length, byte type) {
        super(length, type);
        this.mHierarchyLevel = 3;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mInterfaceNumber = stream.getByte();
        this.mAlternateSetting = stream.getByte();
        this.mNumEndpoints = stream.getByte();
        this.mUsbClass = stream.getByte();
        this.mUsbSubclass = stream.getByte();
        this.mProtocol = stream.getByte();
        this.mDescrIndex = stream.getByte();
        return this.mLength;
    }

    public byte getInterfaceNumber() {
        return this.mInterfaceNumber;
    }

    public byte getAlternateSetting() {
        return this.mAlternateSetting;
    }

    public byte getNumEndpoints() {
        return this.mNumEndpoints;
    }

    public byte getUsbClass() {
        return this.mUsbClass;
    }

    public byte getUsbSubclass() {
        return this.mUsbSubclass;
    }

    public byte getProtocol() {
        return this.mProtocol;
    }

    public byte getDescrIndex() {
        return this.mDescrIndex;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        byte usbClass = getUsbClass();
        byte usbSubclass = getUsbSubclass();
        byte protocol = getProtocol();
        String className = UsbStrings.getClassName(usbClass);
        String subclassName = "";
        if (usbClass == (byte) 1) {
            subclassName = UsbStrings.getAudioSubclassName(usbSubclass);
        }
        canvas.openList();
        canvas.writeListItem("Interface #" + getInterfaceNumber());
        canvas.writeListItem("Class: " + ReportCanvas.getHexString(usbClass) + ": " + className);
        canvas.writeListItem("Subclass: " + ReportCanvas.getHexString(usbSubclass) + ": " + subclassName);
        canvas.writeListItem("Protocol: " + protocol + ": " + ReportCanvas.getHexString(protocol));
        canvas.writeListItem("Endpoints: " + getNumEndpoints());
        canvas.closeList();
    }
}

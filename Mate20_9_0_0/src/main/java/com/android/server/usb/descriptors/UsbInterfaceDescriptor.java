package com.android.server.usb.descriptors;

import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;
import java.util.ArrayList;

public class UsbInterfaceDescriptor extends UsbDescriptor {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbInterfaceDescriptor";
    protected byte mAlternateSetting;
    protected byte mDescrIndex;
    private ArrayList<UsbEndpointDescriptor> mEndpointDescriptors;
    protected int mInterfaceNumber;
    protected byte mNumEndpoints;
    protected int mProtocol;
    protected int mUsbClass;
    protected int mUsbSubclass;

    UsbInterfaceDescriptor(int length, byte type) {
        super(length, type);
        this.mEndpointDescriptors = new ArrayList();
        this.mHierarchyLevel = 3;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mInterfaceNumber = stream.getUnsignedByte();
        this.mAlternateSetting = stream.getByte();
        this.mNumEndpoints = stream.getByte();
        this.mUsbClass = stream.getUnsignedByte();
        this.mUsbSubclass = stream.getUnsignedByte();
        this.mProtocol = stream.getUnsignedByte();
        this.mDescrIndex = stream.getByte();
        return this.mLength;
    }

    public int getInterfaceNumber() {
        return this.mInterfaceNumber;
    }

    public byte getAlternateSetting() {
        return this.mAlternateSetting;
    }

    public byte getNumEndpoints() {
        return this.mNumEndpoints;
    }

    public int getUsbClass() {
        return this.mUsbClass;
    }

    public int getUsbSubclass() {
        return this.mUsbSubclass;
    }

    public int getProtocol() {
        return this.mProtocol;
    }

    public byte getDescrIndex() {
        return this.mDescrIndex;
    }

    void addEndpointDescriptor(UsbEndpointDescriptor endpoint) {
        this.mEndpointDescriptors.add(endpoint);
    }

    UsbInterface toAndroid(UsbDescriptorParser parser) {
        UsbInterface ntrface = new UsbInterface(this.mInterfaceNumber, this.mAlternateSetting, parser.getDescriptorString(this.mDescrIndex), this.mUsbClass, this.mUsbSubclass, this.mProtocol);
        UsbEndpoint[] endpoints = new UsbEndpoint[this.mEndpointDescriptors.size()];
        for (int index = 0; index < this.mEndpointDescriptors.size(); index++) {
            endpoints[index] = ((UsbEndpointDescriptor) this.mEndpointDescriptors.get(index)).toAndroid(parser);
        }
        ntrface.setEndpoints(endpoints);
        return ntrface;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        int usbClass = getUsbClass();
        int usbSubclass = getUsbSubclass();
        int protocol = getProtocol();
        String className = UsbStrings.getClassName(usbClass);
        String subclassName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (usbClass == 1) {
            subclassName = UsbStrings.getAudioSubclassName(usbSubclass);
        }
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Interface #");
        stringBuilder.append(getInterfaceNumber());
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Class: ");
        stringBuilder.append(ReportCanvas.getHexString(usbClass));
        stringBuilder.append(": ");
        stringBuilder.append(className);
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Subclass: ");
        stringBuilder.append(ReportCanvas.getHexString(usbSubclass));
        stringBuilder.append(": ");
        stringBuilder.append(subclassName);
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Protocol: ");
        stringBuilder.append(protocol);
        stringBuilder.append(": ");
        stringBuilder.append(ReportCanvas.getHexString(protocol));
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Endpoints: ");
        stringBuilder.append(getNumEndpoints());
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

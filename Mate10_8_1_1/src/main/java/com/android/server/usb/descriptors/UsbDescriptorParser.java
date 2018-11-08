package com.android.server.usb.descriptors;

import android.util.Log;
import java.util.ArrayList;

public final class UsbDescriptorParser {
    private static final float IN_HEADSET_TRIGGER = 0.75f;
    private static final float OUT_HEADSET_TRIGGER = 0.75f;
    private static final String TAG = "UsbDescriptorParser";
    private int mACInterfacesSpec = 256;
    private UsbInterfaceDescriptor mCurInterfaceDescriptor;
    private ArrayList<UsbDescriptor> mDescriptors = new ArrayList();
    private UsbDeviceDescriptor mDeviceDescriptor;

    private native byte[] getRawDescriptors(String str);

    public int getUsbSpec() {
        if (this.mDeviceDescriptor != null) {
            return this.mDeviceDescriptor.getSpec();
        }
        throw new IllegalArgumentException();
    }

    public void setACInterfaceSpec(int spec) {
        this.mACInterfacesSpec = spec;
    }

    public int getACInterfaceSpec() {
        return this.mACInterfacesSpec;
    }

    private UsbDescriptor allocDescriptor(ByteStream stream) {
        stream.resetReadCount();
        int length = stream.getUnsignedByte();
        byte type = stream.getByte();
        UsbDescriptor descriptor = null;
        switch (type) {
            case (byte) 1:
                descriptor = new UsbDeviceDescriptor(length, type);
                this.mDeviceDescriptor = descriptor;
                break;
            case (byte) 2:
                descriptor = new UsbConfigDescriptor(length, type);
                break;
            case (byte) 4:
                descriptor = new UsbInterfaceDescriptor(length, type);
                this.mCurInterfaceDescriptor = descriptor;
                break;
            case (byte) 5:
                descriptor = new UsbEndpointDescriptor(length, type);
                break;
            case (byte) 11:
                descriptor = new UsbInterfaceAssoc(length, type);
                break;
            case (byte) 33:
                descriptor = new UsbHIDDescriptor(length, type);
                break;
            case (byte) 36:
                descriptor = UsbACInterface.allocDescriptor(this, stream, length, type);
                break;
            case (byte) 37:
                descriptor = UsbACEndpoint.allocDescriptor(this, length, type);
                break;
        }
        if (descriptor != null) {
            return descriptor;
        }
        Log.i(TAG, "Unknown Descriptor len: " + length + " type:0x" + Integer.toHexString(type));
        return new UsbUnknown(length, type);
    }

    public UsbDeviceDescriptor getDeviceDescriptor() {
        return this.mDeviceDescriptor;
    }

    public UsbInterfaceDescriptor getCurInterface() {
        return this.mCurInterfaceDescriptor;
    }

    public void parseDescriptors(byte[] descriptors) {
        this.mDescriptors.clear();
        ByteStream stream = new ByteStream(descriptors);
        while (stream.available() > 0) {
            UsbDescriptor usbDescriptor = null;
            try {
                usbDescriptor = allocDescriptor(stream);
            } catch (Exception ex) {
                Log.e(TAG, "Exception allocating USB descriptor.", ex);
            }
            if (usbDescriptor != null) {
                try {
                    usbDescriptor.parseRawDescriptors(stream);
                    usbDescriptor.postParse(stream);
                } catch (Exception ex2) {
                    Log.e(TAG, "Exception parsing USB descriptors.", ex2);
                    usbDescriptor.setStatus(4);
                } finally {
                    this.mDescriptors.add(usbDescriptor);
                }
            }
        }
    }

    public boolean parseDevice(String deviceAddr) {
        return parseDevice(deviceAddr, null);
    }

    public boolean parseDevice(String deviceAddr, boolean[] hasRawDescriptors) {
        byte[] rawDescriptors = getRawDescriptors(deviceAddr);
        if (hasRawDescriptors != null && hasRawDescriptors.length == 1) {
            boolean z;
            if (rawDescriptors != null) {
                z = true;
            } else {
                z = false;
            }
            hasRawDescriptors[0] = z;
        }
        if (rawDescriptors == null) {
            return false;
        }
        parseDescriptors(rawDescriptors);
        return true;
    }

    public int getParsingSpec() {
        return this.mDeviceDescriptor != null ? this.mDeviceDescriptor.getSpec() : 0;
    }

    public ArrayList<UsbDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public ArrayList<UsbDescriptor> getDescriptors(byte type) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        for (UsbDescriptor descriptor : this.mDescriptors) {
            if (descriptor.getType() == type) {
                list.add(descriptor);
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getInterfaceDescriptorsForClass(byte usbClass) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        for (UsbDescriptor descriptor : this.mDescriptors) {
            if (descriptor.getType() == (byte) 4) {
                if (!(descriptor instanceof UsbInterfaceDescriptor)) {
                    Log.w(TAG, "Unrecognized Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
                } else if (((UsbInterfaceDescriptor) descriptor).getUsbClass() == usbClass) {
                    list.add(descriptor);
                }
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getACInterfaceDescriptors(byte subtype, byte subclass) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        for (UsbDescriptor descriptor : this.mDescriptors) {
            if (descriptor.getType() == UsbDescriptor.DESCRIPTORTYPE_AUDIO_INTERFACE) {
                if (descriptor instanceof UsbACInterface) {
                    UsbACInterface acDescriptor = (UsbACInterface) descriptor;
                    if (acDescriptor.getSubtype() == subtype && acDescriptor.getSubclass() == subclass) {
                        list.add(descriptor);
                    }
                } else {
                    Log.w(TAG, "Unrecognized Audio Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
                }
            }
        }
        return list;
    }

    public boolean hasHIDDescriptor() {
        return getInterfaceDescriptorsForClass((byte) 3).isEmpty() ^ 1;
    }

    public boolean hasMIDIInterface() {
        for (UsbDescriptor descriptor : getInterfaceDescriptorsForClass((byte) 1)) {
            if (!(descriptor instanceof UsbInterfaceDescriptor)) {
                Log.w(TAG, "Undefined Audio Class Interface l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
            } else if (((UsbInterfaceDescriptor) descriptor).getUsbSubclass() == (byte) 3) {
                return true;
            }
        }
        return false;
    }

    public float getInputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        float probability = 0.0f;
        boolean hasMic = false;
        for (UsbDescriptor descriptor : getACInterfaceDescriptors((byte) 2, (byte) 1)) {
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal inDescr = (UsbACTerminal) descriptor;
                if (inDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_IN_MIC && inDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_BIDIR_HEADSET && inDescr.getTerminalType() != 1024) {
                    if (inDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_EXTERN_LINE) {
                    }
                }
                hasMic = true;
                break;
            }
            Log.w(TAG, "Undefined Audio Input terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
        }
        boolean hasSpeaker = false;
        for (UsbDescriptor descriptor2 : getACInterfaceDescriptors((byte) 3, (byte) 1)) {
            if (descriptor2 instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor2;
                if (outDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_OUT_SPEAKER && outDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_OUT_HEADPHONES) {
                    if (outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_BIDIR_HEADSET) {
                    }
                }
                hasSpeaker = true;
                break;
            }
            Log.w(TAG, "Undefined Audio Output terminal l: " + descriptor2.getLength() + " t:0x" + Integer.toHexString(descriptor2.getType()));
        }
        if (hasMic && hasSpeaker) {
            probability = 0.75f;
        }
        if (hasMic && hasHIDDescriptor()) {
            probability += 0.25f;
        }
        return probability;
    }

    public boolean isInputHeadset() {
        Log.i(TAG, "---- isInputHeadset() prob:" + (getInputHeadsetProbability() * 100.0f) + "%");
        return getInputHeadsetProbability() >= 0.75f;
    }

    public float getOutputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        float probability = 0.0f;
        boolean hasSpeaker = false;
        for (UsbDescriptor descriptor : getACInterfaceDescriptors((byte) 3, (byte) 1)) {
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                if (outDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_OUT_SPEAKER && outDescr.getTerminalType() != UsbTerminalTypes.TERMINAL_OUT_HEADPHONES) {
                    if (outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_BIDIR_HEADSET) {
                    }
                }
                hasSpeaker = true;
                break;
            }
            Log.w(TAG, "Undefined Audio Output terminal l: " + descriptor.getLength() + " t:0x" + Integer.toHexString(descriptor.getType()));
        }
        if (hasSpeaker) {
            probability = 0.75f;
        }
        if (hasSpeaker && hasHIDDescriptor()) {
            probability += 0.25f;
        }
        return probability;
    }

    public boolean isOutputHeadset() {
        Log.i(TAG, "---- isOutputHeadset() prob:" + (getOutputHeadsetProbability() * 100.0f) + "%");
        return getOutputHeadsetProbability() >= 0.75f;
    }
}

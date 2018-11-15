package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDevice;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;

public final class UsbDescriptorParser {
    private static final boolean DEBUG = false;
    private static final int DESCRIPTORS_ALLOC_SIZE = 128;
    private static final float IN_HEADSET_TRIGGER = 0.75f;
    private static final float OUT_HEADSET_TRIGGER = 0.75f;
    private static final String TAG = "UsbDescriptorParser";
    private int mACInterfacesSpec = 256;
    private UsbConfigDescriptor mCurConfigDescriptor;
    private UsbInterfaceDescriptor mCurInterfaceDescriptor;
    private final ArrayList<UsbDescriptor> mDescriptors;
    private final String mDeviceAddr;
    private UsbDeviceDescriptor mDeviceDescriptor;

    private class UsbDescriptorsStreamFormatException extends Exception {
        String mMessage;

        UsbDescriptorsStreamFormatException(String message) {
            this.mMessage = message;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Descriptor Stream Format Exception: ");
            stringBuilder.append(this.mMessage);
            return stringBuilder.toString();
        }
    }

    private native String getDescriptorString_native(String str, int i);

    private native byte[] getRawDescriptors_native(String str);

    public UsbDescriptorParser(String deviceAddr, ArrayList<UsbDescriptor> descriptors) {
        this.mDeviceAddr = deviceAddr;
        this.mDescriptors = descriptors;
        this.mDeviceDescriptor = (UsbDeviceDescriptor) descriptors.get(0);
    }

    public UsbDescriptorParser(String deviceAddr, byte[] rawDescriptors) {
        this.mDeviceAddr = deviceAddr;
        this.mDescriptors = new ArrayList(128);
        parseDescriptors(rawDescriptors);
    }

    public String getDeviceAddr() {
        return this.mDeviceAddr;
    }

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

    private UsbDescriptor allocDescriptor(ByteStream stream) throws UsbDescriptorsStreamFormatException {
        stream.resetReadCount();
        int length = stream.getUnsignedByte();
        byte type = stream.getByte();
        UsbDescriptor descriptor = null;
        UsbDescriptor usbDeviceDescriptor;
        switch (type) {
            case (byte) 1:
                usbDeviceDescriptor = new UsbDeviceDescriptor(length, type);
                this.mDeviceDescriptor = usbDeviceDescriptor;
                descriptor = usbDeviceDescriptor;
                break;
            case (byte) 2:
                usbDeviceDescriptor = new UsbConfigDescriptor(length, type);
                this.mCurConfigDescriptor = usbDeviceDescriptor;
                descriptor = usbDeviceDescriptor;
                if (this.mDeviceDescriptor != null) {
                    this.mDeviceDescriptor.addConfigDescriptor(this.mCurConfigDescriptor);
                    break;
                }
                Log.e(TAG, "Config Descriptor found with no associated Device Descriptor!");
                throw new UsbDescriptorsStreamFormatException("Config Descriptor found with no associated Device Descriptor!");
            case (byte) 4:
                usbDeviceDescriptor = new UsbInterfaceDescriptor(length, type);
                this.mCurInterfaceDescriptor = usbDeviceDescriptor;
                descriptor = usbDeviceDescriptor;
                if (this.mCurConfigDescriptor != null) {
                    this.mCurConfigDescriptor.addInterfaceDescriptor(this.mCurInterfaceDescriptor);
                    break;
                }
                Log.e(TAG, "Interface Descriptor found with no associated Config Descriptor!");
                throw new UsbDescriptorsStreamFormatException("Interface Descriptor found with no associated Config Descriptor!");
            case (byte) 5:
                descriptor = new UsbEndpointDescriptor(length, type);
                if (this.mCurInterfaceDescriptor != null) {
                    this.mCurInterfaceDescriptor.addEndpointDescriptor((UsbEndpointDescriptor) descriptor);
                    break;
                }
                Log.e(TAG, "Endpoint Descriptor found with no associated Interface Descriptor!");
                throw new UsbDescriptorsStreamFormatException("Endpoint Descriptor found with no associated Interface Descriptor!");
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Descriptor len: ");
        stringBuilder.append(length);
        stringBuilder.append(" type:0x");
        stringBuilder.append(Integer.toHexString(type));
        Log.i(str, stringBuilder.toString());
        return new UsbUnknown(length, type);
    }

    public UsbDeviceDescriptor getDeviceDescriptor() {
        return this.mDeviceDescriptor;
    }

    public UsbInterfaceDescriptor getCurInterface() {
        return this.mCurInterfaceDescriptor;
    }

    public void parseDescriptors(byte[] descriptors) {
        ByteStream stream = new ByteStream(descriptors);
        while (stream.available() > 0) {
            UsbDescriptor descriptor = null;
            try {
                descriptor = allocDescriptor(stream);
            } catch (Exception ex) {
                Log.e(TAG, "Exception allocating USB descriptor.", ex);
            }
            if (descriptor != null) {
                try {
                    descriptor.parseRawDescriptors(stream);
                    descriptor.postParse(stream);
                } catch (Exception ex2) {
                    Log.e(TAG, "Exception parsing USB descriptors.", ex2);
                    descriptor.setStatus(4);
                } catch (Throwable th) {
                    this.mDescriptors.add(descriptor);
                }
                this.mDescriptors.add(descriptor);
            }
        }
    }

    public byte[] getRawDescriptors() {
        return getRawDescriptors_native(this.mDeviceAddr);
    }

    public String getDescriptorString(int stringId) {
        return getDescriptorString_native(this.mDeviceAddr, stringId);
    }

    public int getParsingSpec() {
        return this.mDeviceDescriptor != null ? this.mDeviceDescriptor.getSpec() : 0;
    }

    public ArrayList<UsbDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public UsbDevice toAndroidUsbDevice() {
        if (this.mDeviceDescriptor == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR - No Device Descriptor");
            return null;
        }
        UsbDevice device = this.mDeviceDescriptor.toAndroid(this);
        if (device == null) {
            Log.e(TAG, "toAndroidUsbDevice() ERROR Creating Device");
        }
        return device;
    }

    public ArrayList<UsbDescriptor> getDescriptors(byte type) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        Iterator it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor.getType() == type) {
                list.add(descriptor);
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getInterfaceDescriptorsForClass(int usbClass) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        Iterator it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor.getType() == (byte) 4) {
                if (!(descriptor instanceof UsbInterfaceDescriptor)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unrecognized Interface l: ");
                    stringBuilder.append(descriptor.getLength());
                    stringBuilder.append(" t:0x");
                    stringBuilder.append(Integer.toHexString(descriptor.getType()));
                    Log.w(str, stringBuilder.toString());
                } else if (((UsbInterfaceDescriptor) descriptor).getUsbClass() == usbClass) {
                    list.add(descriptor);
                }
            }
        }
        return list;
    }

    public ArrayList<UsbDescriptor> getACInterfaceDescriptors(byte subtype, int subclass) {
        ArrayList<UsbDescriptor> list = new ArrayList();
        Iterator it = this.mDescriptors.iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor.getType() == UsbDescriptor.DESCRIPTORTYPE_AUDIO_INTERFACE) {
                if (descriptor instanceof UsbACInterface) {
                    UsbACInterface acDescriptor = (UsbACInterface) descriptor;
                    if (acDescriptor.getSubtype() == subtype && acDescriptor.getSubclass() == subclass) {
                        list.add(descriptor);
                    }
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unrecognized Audio Interface l: ");
                    stringBuilder.append(descriptor.getLength());
                    stringBuilder.append(" t:0x");
                    stringBuilder.append(Integer.toHexString(descriptor.getType()));
                    Log.w(str, stringBuilder.toString());
                }
            }
        }
        return list;
    }

    public boolean hasInput() {
        Iterator it = getACInterfaceDescriptors((byte) 2, 1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor instanceof UsbACTerminal) {
                int terminalCategory = ((UsbACTerminal) descriptor).getTerminalType() & -256;
                if (!(terminalCategory == 256 || terminalCategory == 768)) {
                    return true;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Undefined Audio Input terminal l: ");
            stringBuilder.append(descriptor.getLength());
            stringBuilder.append(" t:0x");
            stringBuilder.append(Integer.toHexString(descriptor.getType()));
            Log.w(str, stringBuilder.toString());
        }
        return false;
    }

    public boolean hasOutput() {
        Iterator it = getACInterfaceDescriptors((byte) 3, 1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor instanceof UsbACTerminal) {
                int terminalCategory = ((UsbACTerminal) descriptor).getTerminalType() & -256;
                if (!(terminalCategory == 256 || terminalCategory == 512)) {
                    return true;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Undefined Audio Input terminal l: ");
            stringBuilder.append(descriptor.getLength());
            stringBuilder.append(" t:0x");
            stringBuilder.append(Integer.toHexString(descriptor.getType()));
            Log.w(str, stringBuilder.toString());
        }
        return false;
    }

    public boolean hasMic() {
        Iterator it = getACInterfaceDescriptors((byte) 2, 1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal inDescr = (UsbACTerminal) descriptor;
                if (inDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_IN_MIC || inDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_BIDIR_HEADSET || inDescr.getTerminalType() == 1024 || inDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_EXTERN_LINE) {
                    return true;
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Undefined Audio Input terminal l: ");
                stringBuilder.append(descriptor.getLength());
                stringBuilder.append(" t:0x");
                stringBuilder.append(Integer.toHexString(descriptor.getType()));
                Log.w(str, stringBuilder.toString());
            }
        }
        return false;
    }

    public boolean hasSpeaker() {
        Iterator it = getACInterfaceDescriptors((byte) 3, 1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                if (outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_OUT_SPEAKER || outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_OUT_HEADPHONES || outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_BIDIR_HEADSET) {
                    return true;
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Undefined Audio Output terminal l: ");
                stringBuilder.append(descriptor.getLength());
                stringBuilder.append(" t:0x");
                stringBuilder.append(Integer.toHexString(descriptor.getType()));
                Log.w(str, stringBuilder.toString());
            }
        }
        return false;
    }

    public boolean hasAudioInterface() {
        return true ^ getInterfaceDescriptorsForClass(1).isEmpty();
    }

    public boolean hasHIDInterface() {
        return getInterfaceDescriptorsForClass(3).isEmpty() ^ 1;
    }

    public boolean hasStorageInterface() {
        return getInterfaceDescriptorsForClass(8).isEmpty() ^ 1;
    }

    public boolean hasMIDIInterface() {
        Iterator it = getInterfaceDescriptorsForClass(1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (!(descriptor instanceof UsbInterfaceDescriptor)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Undefined Audio Class Interface l: ");
                stringBuilder.append(descriptor.getLength());
                stringBuilder.append(" t:0x");
                stringBuilder.append(Integer.toHexString(descriptor.getType()));
                Log.w(str, stringBuilder.toString());
            } else if (((UsbInterfaceDescriptor) descriptor).getUsbSubclass() == 3) {
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
        boolean hasMic = hasMic();
        boolean hasSpeaker = hasSpeaker();
        if (hasMic && hasSpeaker) {
            probability = 0.0f + 0.75f;
        }
        if (hasMic && hasHIDInterface()) {
            probability += 0.25f;
        }
        return probability;
    }

    public boolean isInputHeadset() {
        return getInputHeadsetProbability() >= 0.75f;
    }

    public float getOutputHeadsetProbability() {
        if (hasMIDIInterface()) {
            return 0.0f;
        }
        float probability = 0.0f;
        boolean hasSpeaker = false;
        Iterator it = getACInterfaceDescriptors((byte) 3, 1).iterator();
        while (it.hasNext()) {
            UsbDescriptor descriptor = (UsbDescriptor) it.next();
            if (descriptor instanceof UsbACTerminal) {
                UsbACTerminal outDescr = (UsbACTerminal) descriptor;
                if (outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_OUT_SPEAKER || outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_OUT_HEADPHONES || outDescr.getTerminalType() == UsbTerminalTypes.TERMINAL_BIDIR_HEADSET) {
                    hasSpeaker = true;
                    break;
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Undefined Audio Output terminal l: ");
                stringBuilder.append(descriptor.getLength());
                stringBuilder.append(" t:0x");
                stringBuilder.append(Integer.toHexString(descriptor.getType()));
                Log.w(str, stringBuilder.toString());
            }
        }
        if (hasSpeaker) {
            probability = 0.0f + 0.75f;
        }
        if (hasSpeaker && hasHIDInterface()) {
            probability += 0.25f;
        }
        return probability;
    }

    public boolean isOutputHeadset() {
        return getOutputHeadsetProbability() >= 0.75f;
    }
}

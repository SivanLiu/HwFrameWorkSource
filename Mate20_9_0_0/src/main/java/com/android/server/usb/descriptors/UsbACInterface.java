package com.android.server.usb.descriptors;

import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbACInterface extends UsbDescriptor {
    public static final byte ACI_CLOCK_MULTIPLIER = (byte) 12;
    public static final byte ACI_CLOCK_SELECTOR = (byte) 11;
    public static final byte ACI_CLOCK_SOURCE = (byte) 10;
    public static final byte ACI_EXTENSION_UNIT = (byte) 8;
    public static final byte ACI_FEATURE_UNIT = (byte) 6;
    public static final byte ACI_HEADER = (byte) 1;
    public static final byte ACI_INPUT_TERMINAL = (byte) 2;
    public static final byte ACI_MIXER_UNIT = (byte) 4;
    public static final byte ACI_OUTPUT_TERMINAL = (byte) 3;
    public static final byte ACI_PROCESSING_UNIT = (byte) 7;
    public static final byte ACI_SAMPLE_RATE_CONVERTER = (byte) 13;
    public static final byte ACI_SELECTOR_UNIT = (byte) 5;
    public static final byte ACI_UNDEFINED = (byte) 0;
    public static final byte ASI_FORMAT_SPECIFIC = (byte) 3;
    public static final byte ASI_FORMAT_TYPE = (byte) 2;
    public static final byte ASI_GENERAL = (byte) 1;
    public static final byte ASI_UNDEFINED = (byte) 0;
    public static final int FORMAT_III_IEC1937AC3 = 8193;
    public static final int FORMAT_III_IEC1937_MPEG1_Layer1 = 8194;
    public static final int FORMAT_III_IEC1937_MPEG1_Layer2 = 8195;
    public static final int FORMAT_III_IEC1937_MPEG2_EXT = 8196;
    public static final int FORMAT_III_IEC1937_MPEG2_Layer1LS = 8197;
    public static final int FORMAT_III_UNDEFINED = 8192;
    public static final int FORMAT_II_AC3 = 4098;
    public static final int FORMAT_II_MPEG = 4097;
    public static final int FORMAT_II_UNDEFINED = 4096;
    public static final int FORMAT_I_ALAW = 4;
    public static final int FORMAT_I_IEEE_FLOAT = 3;
    public static final int FORMAT_I_MULAW = 5;
    public static final int FORMAT_I_PCM = 1;
    public static final int FORMAT_I_PCM8 = 2;
    public static final int FORMAT_I_UNDEFINED = 0;
    public static final byte MSI_ELEMENT = (byte) 4;
    public static final byte MSI_HEADER = (byte) 1;
    public static final byte MSI_IN_JACK = (byte) 2;
    public static final byte MSI_OUT_JACK = (byte) 3;
    public static final byte MSI_UNDEFINED = (byte) 0;
    private static final String TAG = "UsbACInterface";
    protected final int mSubclass;
    protected final byte mSubtype;

    public UsbACInterface(int length, byte type, byte subtype, int subclass) {
        super(length, type);
        this.mSubtype = subtype;
        this.mSubclass = subclass;
    }

    public byte getSubtype() {
        return this.mSubtype;
    }

    public int getSubclass() {
        return this.mSubclass;
    }

    private static UsbDescriptor allocAudioControlDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type, byte subtype, int subClass) {
        switch (subtype) {
            case (byte) 1:
                int acInterfaceSpec = stream.unpackUsbShort();
                parser.setACInterfaceSpec(acInterfaceSpec);
                if (acInterfaceSpec == 512) {
                    return new Usb20ACHeader(length, type, subtype, subClass, acInterfaceSpec);
                }
                return new Usb10ACHeader(length, type, subtype, subClass, acInterfaceSpec);
            case (byte) 2:
                if (parser.getACInterfaceSpec() == 512) {
                    return new Usb20ACInputTerminal(length, type, subtype, subClass);
                }
                return new Usb10ACInputTerminal(length, type, subtype, subClass);
            case (byte) 3:
                if (parser.getACInterfaceSpec() == 512) {
                    return new Usb20ACOutputTerminal(length, type, subtype, subClass);
                }
                return new Usb10ACOutputTerminal(length, type, subtype, subClass);
            case (byte) 4:
                if (parser.getACInterfaceSpec() == 512) {
                    return new Usb20ACMixerUnit(length, type, subtype, subClass);
                }
                return new Usb10ACMixerUnit(length, type, subtype, subClass);
            case (byte) 5:
                return new UsbACSelectorUnit(length, type, subtype, subClass);
            case (byte) 6:
                return new UsbACFeatureUnit(length, type, subtype, subClass);
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown Audio Class Interface subtype:0x");
                stringBuilder.append(Integer.toHexString(subtype));
                Log.w(str, stringBuilder.toString());
                return new UsbACInterfaceUnparsed(length, type, subtype, subClass);
        }
    }

    private static UsbDescriptor allocAudioStreamingDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type, byte subtype, int subClass) {
        int acInterfaceSpec = parser.getACInterfaceSpec();
        switch (subtype) {
            case (byte) 1:
                if (acInterfaceSpec == 512) {
                    return new Usb20ASGeneral(length, type, subtype, subClass);
                }
                return new Usb10ASGeneral(length, type, subtype, subClass);
            case (byte) 2:
                return UsbASFormat.allocDescriptor(parser, stream, length, type, subtype, subClass);
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown Audio Streaming Interface subtype:0x");
                stringBuilder.append(Integer.toHexString(subtype));
                Log.w(str, stringBuilder.toString());
                return null;
        }
    }

    private static UsbDescriptor allocMidiStreamingDescriptor(int length, byte type, byte subtype, int subClass) {
        switch (subtype) {
            case (byte) 1:
                return new UsbMSMidiHeader(length, type, subtype, subClass);
            case (byte) 2:
                return new UsbMSMidiInputJack(length, type, subtype, subClass);
            case (byte) 3:
                return new UsbMSMidiOutputJack(length, type, subtype, subClass);
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown MIDI Streaming Interface subtype:0x");
                stringBuilder.append(Integer.toHexString(subtype));
                Log.w(str, stringBuilder.toString());
                return null;
        }
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type) {
        byte subtype = stream.getByte();
        int subClass = parser.getCurInterface().getUsbSubclass();
        switch (subClass) {
            case 1:
                return allocAudioControlDescriptor(parser, stream, length, type, subtype, subClass);
            case 2:
                return allocAudioStreamingDescriptor(parser, stream, length, type, subtype, subClass);
            case 3:
                return allocMidiStreamingDescriptor(length, type, subtype, subClass);
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown Audio Class Interface Subclass: 0x");
                stringBuilder.append(Integer.toHexString(subClass));
                Log.w(str, stringBuilder.toString());
                return null;
        }
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        int subClass = getSubclass();
        String subClassName = UsbStrings.getACInterfaceSubclassName(subClass);
        byte subtype = getSubtype();
        String subTypeName = UsbStrings.getACControlInterfaceName(subtype);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Subclass: ");
        stringBuilder.append(ReportCanvas.getHexString(subClass));
        stringBuilder.append(" ");
        stringBuilder.append(subClassName);
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Subtype: ");
        stringBuilder.append(ReportCanvas.getHexString(subtype));
        stringBuilder.append(" ");
        stringBuilder.append(subTypeName);
        canvas.writeListItem(stringBuilder.toString());
        canvas.closeList();
    }
}

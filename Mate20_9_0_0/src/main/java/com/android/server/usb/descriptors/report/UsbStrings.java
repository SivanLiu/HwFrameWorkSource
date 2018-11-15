package com.android.server.usb.descriptors.report;

import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.util.HashMap;

public final class UsbStrings {
    private static final String TAG = "UsbStrings";
    private static HashMap<Byte, String> sACControlInterfaceNames;
    private static HashMap<Byte, String> sACStreamingInterfaceNames;
    private static HashMap<Integer, String> sAudioEncodingNames;
    private static HashMap<Integer, String> sAudioSubclassNames;
    private static HashMap<Integer, String> sClassNames;
    private static HashMap<Byte, String> sDescriptorNames;
    private static HashMap<Integer, String> sFormatNames;
    private static HashMap<Integer, String> sTerminalNames;

    static {
        allocUsbStrings();
    }

    private static void initDescriptorNames() {
        sDescriptorNames = new HashMap();
        sDescriptorNames.put(Byte.valueOf((byte) 1), "Device");
        sDescriptorNames.put(Byte.valueOf((byte) 2), "Config");
        sDescriptorNames.put(Byte.valueOf((byte) 3), "String");
        sDescriptorNames.put(Byte.valueOf((byte) 4), "Interface");
        sDescriptorNames.put(Byte.valueOf((byte) 5), "Endpoint");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_BOS), "BOS (whatever that means)");
        sDescriptorNames.put(Byte.valueOf((byte) 11), "Interface Association");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_CAPABILITY), "Capability");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_HID), "HID");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_REPORT), "Report");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_PHYSICAL), "Physical");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_AUDIO_INTERFACE), "Audio Class Interface");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_AUDIO_ENDPOINT), "Audio Class Endpoint");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_HUB), "Hub");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_SUPERSPEED_HUB), "Superspeed Hub");
        sDescriptorNames.put(Byte.valueOf(UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION), "Endpoint Companion");
    }

    private static void initACControlInterfaceNames() {
        sACControlInterfaceNames = new HashMap();
        sACControlInterfaceNames.put(Byte.valueOf((byte) 0), "Undefined");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 1), "Header");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 2), "Input Terminal");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 3), "Output Terminal");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 4), "Mixer Unit");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 5), "Selector Unit");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 6), "Feature Unit");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 7), "Processing Unit");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 8), "Extension Unit");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 10), "Clock Source");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 11), "Clock Selector");
        sACControlInterfaceNames.put(Byte.valueOf((byte) 12), "Clock Multiplier");
        sACControlInterfaceNames.put(Byte.valueOf(UsbACInterface.ACI_SAMPLE_RATE_CONVERTER), "Sample Rate Converter");
    }

    private static void initACStreamingInterfaceNames() {
        sACStreamingInterfaceNames = new HashMap();
        sACStreamingInterfaceNames.put(Byte.valueOf((byte) 0), "Undefined");
        sACStreamingInterfaceNames.put(Byte.valueOf((byte) 1), "General");
        sACStreamingInterfaceNames.put(Byte.valueOf((byte) 2), "Format Type");
        sACStreamingInterfaceNames.put(Byte.valueOf((byte) 3), "Format Specific");
    }

    private static void initClassNames() {
        sClassNames = new HashMap();
        sClassNames.put(Integer.valueOf(0), "Device");
        sClassNames.put(Integer.valueOf(1), "Audio");
        sClassNames.put(Integer.valueOf(2), "Communications");
        sClassNames.put(Integer.valueOf(3), "HID");
        sClassNames.put(Integer.valueOf(5), "Physical");
        sClassNames.put(Integer.valueOf(6), "Image");
        sClassNames.put(Integer.valueOf(7), "Printer");
        sClassNames.put(Integer.valueOf(8), "Storage");
        sClassNames.put(Integer.valueOf(9), "Hub");
        sClassNames.put(Integer.valueOf(10), "CDC Control");
        sClassNames.put(Integer.valueOf(11), "Smart Card");
        sClassNames.put(Integer.valueOf(13), "Security");
        sClassNames.put(Integer.valueOf(14), "Video");
        sClassNames.put(Integer.valueOf(15), "Healthcare");
        sClassNames.put(Integer.valueOf(16), "Audio/Video");
        sClassNames.put(Integer.valueOf(17), "Billboard");
        sClassNames.put(Integer.valueOf(18), "Type C Bridge");
        sClassNames.put(Integer.valueOf(220), "Diagnostic");
        sClassNames.put(Integer.valueOf(UsbDescriptor.CLASSID_WIRELESS), "Wireless");
        sClassNames.put(Integer.valueOf(UsbDescriptor.CLASSID_MISC), "Misc");
        sClassNames.put(Integer.valueOf(UsbDescriptor.CLASSID_APPSPECIFIC), "Application Specific");
        sClassNames.put(Integer.valueOf(255), "Vendor Specific");
    }

    private static void initAudioSubclassNames() {
        sAudioSubclassNames = new HashMap();
        sAudioSubclassNames.put(Integer.valueOf(0), "Undefinded");
        sAudioSubclassNames.put(Integer.valueOf(1), "Audio Control");
        sAudioSubclassNames.put(Integer.valueOf(2), "Audio Streaming");
        sAudioSubclassNames.put(Integer.valueOf(3), "MIDI Streaming");
    }

    private static void initAudioEncodingNames() {
        sAudioEncodingNames = new HashMap();
        sAudioEncodingNames.put(Integer.valueOf(0), "Format I Undefined");
        sAudioEncodingNames.put(Integer.valueOf(1), "Format I PCM");
        sAudioEncodingNames.put(Integer.valueOf(2), "Format I PCM8");
        sAudioEncodingNames.put(Integer.valueOf(3), "Format I FLOAT");
        sAudioEncodingNames.put(Integer.valueOf(4), "Format I ALAW");
        sAudioEncodingNames.put(Integer.valueOf(5), "Format I MuLAW");
        sAudioEncodingNames.put(Integer.valueOf(4096), "FORMAT_II Undefined");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_II_MPEG), "FORMAT_II MPEG");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_II_AC3), "FORMAT_II AC3");
        sAudioEncodingNames.put(Integer.valueOf(8192), "FORMAT_III Undefined");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_III_IEC1937AC3), "FORMAT_III IEC1937 AC3");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1), "FORMAT_III MPEG1 Layer 1");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer2), "FORMAT_III MPEG1 Layer 2");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_III_IEC1937_MPEG2_EXT), "FORMAT_III MPEG2 EXT");
        sAudioEncodingNames.put(Integer.valueOf(UsbACInterface.FORMAT_III_IEC1937_MPEG2_Layer1LS), "FORMAT_III MPEG2 Layer1LS");
    }

    private static void initTerminalNames() {
        sTerminalNames = new HashMap();
        sTerminalNames.put(Integer.valueOf(257), "USB Streaming");
        sTerminalNames.put(Integer.valueOf(512), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_MIC), "Microphone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_DESKTOP_MIC), "Desktop Microphone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC), "Personal (headset) Microphone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_OMNI_MIC), "Omni Microphone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_MIC_ARRAY), "Microphone Array");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_IN_PROC_MIC_ARRAY), "Proecessing Microphone Array");
        sTerminalNames.put(Integer.valueOf(768), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_SPEAKER), "Speaker");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_HEADPHONES), "Headphones");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_HEADMOUNTED), "Head Mounted Speaker");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_DESKTOPSPEAKER), "Desktop Speaker");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_ROOMSPEAKER), "Room Speaker");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_COMSPEAKER), "Communications Speaker");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER), "Low Frequency Speaker");
        sTerminalNames.put(Integer.valueOf(1024), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_BIDIR_HANDSET), "Handset");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_BIDIR_HEADSET), "Headset");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE), "Speaker Phone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_SUPRESS), "Speaker Phone (echo supressing)");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_BIDIR_SKRPHONE_CANCEL), "Speaker Phone (echo canceling)");
        sTerminalNames.put(Integer.valueOf(1280), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_TELE_PHONELINE), "Phone Line");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_TELE_PHONE), "Telephone");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_TELE_DOWNLINEPHONE), "Down Line Phone");
        sTerminalNames.put(Integer.valueOf(1536), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_ANALOG), "Analog Connector");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_DIGITAL), "Digital Connector");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_LINE), "Line Connector");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_LEGACY), "Legacy Audio Connector");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_SPIDF), "S/PIDF Interface");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_1394DA), "1394 Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EXTERN_1394DV), "1394 Audio/Video");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_UNDEFINED), "Undefined");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_CALNOISE), "Calibration Nose");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_EQNOISE), "EQ Noise");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_CDPLAYER), "CD Player");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_DAT), "DAT");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_DCC), "DCC");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_MINIDISK), "Mini Disk");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_ANALOGTAPE), "Analog Tap");
        sTerminalNames.put(Integer.valueOf(1800), "Phonograph");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_VCRAUDIO), "VCR Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_VIDDISKAUDIO), "Video Disk Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_DVDAUDIO), "DVD Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_TVAUDIO), "TV Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_SATELLITEAUDIO), "Satellite Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_CABLEAUDIO), "Cable Tuner Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_DSSAUDIO), "DSS Audio");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_RADIOTRANSMITTER), "Radio Transmitter");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_MULTITRACK), "Multitrack Recorder");
        sTerminalNames.put(Integer.valueOf(UsbTerminalTypes.TERMINAL_EMBED_SYNTHESIZER), "Synthesizer");
    }

    public static String getTerminalName(int terminalType) {
        String name = (String) sTerminalNames.get(Integer.valueOf(terminalType));
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Terminal Type 0x");
        stringBuilder.append(Integer.toHexString(terminalType));
        return stringBuilder.toString();
    }

    private static void initFormatNames() {
        sFormatNames = new HashMap();
        sFormatNames.put(Integer.valueOf(1), "FORMAT_TYPE_I");
        sFormatNames.put(Integer.valueOf(2), "FORMAT_TYPE_II");
        sFormatNames.put(Integer.valueOf(3), "FORMAT_TYPE_III");
        sFormatNames.put(Integer.valueOf(4), "FORMAT_TYPE_IV");
        sFormatNames.put(Integer.valueOf(-127), "EXT_FORMAT_TYPE_I");
        sFormatNames.put(Integer.valueOf(-126), "EXT_FORMAT_TYPE_II");
        sFormatNames.put(Integer.valueOf(-125), "EXT_FORMAT_TYPE_III");
    }

    public static String getFormatName(int format) {
        String name = (String) sFormatNames.get(Integer.valueOf(format));
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Format Type 0x");
        stringBuilder.append(Integer.toHexString(format));
        return stringBuilder.toString();
    }

    private static void allocUsbStrings() {
        initDescriptorNames();
        initACControlInterfaceNames();
        initACStreamingInterfaceNames();
        initClassNames();
        initAudioSubclassNames();
        initAudioEncodingNames();
        initTerminalNames();
        initFormatNames();
    }

    public static String getDescriptorName(byte descriptorID) {
        String name = (String) sDescriptorNames.get(Byte.valueOf(descriptorID));
        int iDescriptorID = descriptorID & 255;
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Descriptor [0x");
        stringBuilder.append(Integer.toHexString(iDescriptorID));
        stringBuilder.append(":");
        stringBuilder.append(iDescriptorID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getACControlInterfaceName(byte subtype) {
        String name = (String) sACControlInterfaceNames.get(Byte.valueOf(subtype));
        int iSubType = subtype & 255;
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown subtype [0x");
        stringBuilder.append(Integer.toHexString(iSubType));
        stringBuilder.append(":");
        stringBuilder.append(iSubType);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getACStreamingInterfaceName(byte subtype) {
        String name = (String) sACStreamingInterfaceNames.get(Byte.valueOf(subtype));
        int iSubType = subtype & 255;
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Subtype [0x");
        stringBuilder.append(Integer.toHexString(iSubType));
        stringBuilder.append(":");
        stringBuilder.append(iSubType);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getClassName(int classID) {
        String name = (String) sClassNames.get(Integer.valueOf(classID));
        int iClassID = classID & 255;
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Class ID [0x");
        stringBuilder.append(Integer.toHexString(iClassID));
        stringBuilder.append(":");
        stringBuilder.append(iClassID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getAudioSubclassName(int subClassID) {
        String name = (String) sAudioSubclassNames.get(Integer.valueOf(subClassID));
        int iSubclassID = subClassID & 255;
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Audio Subclass [0x");
        stringBuilder.append(Integer.toHexString(iSubclassID));
        stringBuilder.append(":");
        stringBuilder.append(iSubclassID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getAudioFormatName(int formatID) {
        String name = (String) sAudioEncodingNames.get(Integer.valueOf(formatID));
        if (name != null) {
            return name;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown Format (encoding) ID [0x");
        stringBuilder.append(Integer.toHexString(formatID));
        stringBuilder.append(":");
        stringBuilder.append(formatID);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String getACInterfaceSubclassName(int subClassID) {
        return subClassID == 1 ? "AC Control" : "AC Streaming";
    }
}

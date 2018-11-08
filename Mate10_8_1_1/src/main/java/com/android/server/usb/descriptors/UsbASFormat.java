package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.UsbStrings;

public class UsbASFormat extends UsbACInterface {
    public static final byte EXT_FORMAT_TYPE_I = (byte) -127;
    public static final byte EXT_FORMAT_TYPE_II = (byte) -126;
    public static final byte EXT_FORMAT_TYPE_III = (byte) -125;
    public static final byte FORMAT_TYPE_I = (byte) 1;
    public static final byte FORMAT_TYPE_II = (byte) 2;
    public static final byte FORMAT_TYPE_III = (byte) 3;
    public static final byte FORMAT_TYPE_IV = (byte) 4;
    private static final String TAG = "UsbASFormat";
    private final byte mFormatType;

    public UsbASFormat(int length, byte type, byte subtype, byte formatType, byte mSubclass) {
        super(length, type, subtype, mSubclass);
        this.mFormatType = formatType;
    }

    public byte getFormatType() {
        return this.mFormatType;
    }

    public int[] getSampleRates() {
        return null;
    }

    public int[] getBitDepths() {
        return null;
    }

    public int[] getChannelCounts() {
        return null;
    }

    public static UsbDescriptor allocDescriptor(UsbDescriptorParser parser, ByteStream stream, int length, byte type, byte subtype, byte subclass) {
        byte formatType = stream.getByte();
        int acInterfaceSpec = parser.getACInterfaceSpec();
        switch (formatType) {
            case (byte) 1:
                if (acInterfaceSpec == 512) {
                    return new Usb20ASFormatI(length, type, subtype, formatType, subclass);
                }
                return new Usb10ASFormatI(length, type, subtype, formatType, subclass);
            case (byte) 2:
                if (acInterfaceSpec == 512) {
                    return new Usb20ASFormatII(length, type, subtype, formatType, subclass);
                }
                return new Usb10ASFormatII(length, type, subtype, formatType, subclass);
            case (byte) 3:
                return new Usb20ASFormatIII(length, type, subtype, formatType, subclass);
            default:
                return new UsbASFormat(length, type, subtype, formatType, subclass);
        }
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.writeParagraph(UsbStrings.getFormatName(getFormatType()), false);
    }
}

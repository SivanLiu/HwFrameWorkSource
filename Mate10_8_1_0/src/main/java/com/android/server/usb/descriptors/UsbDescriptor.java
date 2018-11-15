package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.Reporting;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbDescriptor implements Reporting {
    public static final byte AUDIO_AUDIOCONTROL = (byte) 1;
    public static final byte AUDIO_AUDIOSTREAMING = (byte) 2;
    public static final byte AUDIO_MIDISTREAMING = (byte) 3;
    public static final byte AUDIO_SUBCLASS_UNDEFINED = (byte) 0;
    public static final byte CLASSID_APPSPECIFIC = (byte) -2;
    public static final byte CLASSID_AUDIO = (byte) 1;
    public static final byte CLASSID_AUDIOVIDEO = (byte) 16;
    public static final byte CLASSID_BILLBOARD = (byte) 17;
    public static final byte CLASSID_CDC_CONTROL = (byte) 10;
    public static final byte CLASSID_COM = (byte) 2;
    public static final byte CLASSID_DEVICE = (byte) 0;
    public static final byte CLASSID_DIAGNOSTIC = (byte) -36;
    public static final byte CLASSID_HEALTHCARE = (byte) 15;
    public static final byte CLASSID_HID = (byte) 3;
    public static final byte CLASSID_HUB = (byte) 9;
    public static final byte CLASSID_IMAGE = (byte) 6;
    public static final byte CLASSID_MISC = (byte) -17;
    public static final byte CLASSID_PHYSICAL = (byte) 5;
    public static final byte CLASSID_PRINTER = (byte) 7;
    public static final byte CLASSID_SECURITY = (byte) 13;
    public static final byte CLASSID_SMART_CARD = (byte) 11;
    public static final byte CLASSID_STORAGE = (byte) 8;
    public static final byte CLASSID_TYPECBRIDGE = (byte) 18;
    public static final byte CLASSID_VENDSPECIFIC = (byte) -1;
    public static final byte CLASSID_VIDEO = (byte) 14;
    public static final byte CLASSID_WIRELESS = (byte) -32;
    public static final byte DESCRIPTORTYPE_AUDIO_ENDPOINT = (byte) 37;
    public static final byte DESCRIPTORTYPE_AUDIO_INTERFACE = (byte) 36;
    public static final byte DESCRIPTORTYPE_BOS = (byte) 15;
    public static final byte DESCRIPTORTYPE_CAPABILITY = (byte) 16;
    public static final byte DESCRIPTORTYPE_CONFIG = (byte) 2;
    public static final byte DESCRIPTORTYPE_DEVICE = (byte) 1;
    public static final byte DESCRIPTORTYPE_ENDPOINT = (byte) 5;
    public static final byte DESCRIPTORTYPE_ENDPOINT_COMPANION = (byte) 48;
    public static final byte DESCRIPTORTYPE_HID = (byte) 33;
    public static final byte DESCRIPTORTYPE_HUB = (byte) 41;
    public static final byte DESCRIPTORTYPE_INTERFACE = (byte) 4;
    public static final byte DESCRIPTORTYPE_INTERFACEASSOC = (byte) 11;
    public static final byte DESCRIPTORTYPE_PHYSICAL = (byte) 35;
    public static final byte DESCRIPTORTYPE_REPORT = (byte) 34;
    public static final byte DESCRIPTORTYPE_STRING = (byte) 3;
    public static final byte DESCRIPTORTYPE_SUPERSPEED_HUB = (byte) 42;
    public static final int REQUEST_CLEAR_FEATURE = 1;
    public static final int REQUEST_GET_ADDRESS = 5;
    public static final int REQUEST_GET_CONFIGURATION = 8;
    public static final int REQUEST_GET_DESCRIPTOR = 6;
    public static final int REQUEST_GET_STATUS = 0;
    public static final int REQUEST_SET_CONFIGURATION = 9;
    public static final int REQUEST_SET_DESCRIPTOR = 7;
    public static final int REQUEST_SET_FEATURE = 3;
    private static final int SIZE_STRINGBUFFER = 256;
    public static final int STATUS_PARSED_OK = 1;
    public static final int STATUS_PARSED_OVERRUN = 3;
    public static final int STATUS_PARSED_UNDERRUN = 2;
    public static final int STATUS_PARSE_EXCEPTION = 4;
    public static final int STATUS_UNPARSED = 0;
    private static final String TAG = "UsbDescriptor";
    private static String[] sStatusStrings = new String[]{"UNPARSED", "PARSED - OK", "PARSED - UNDERRUN", "PARSED - OVERRUN"};
    private static byte[] sStringBuffer = new byte[256];
    protected int mHierarchyLevel;
    protected final int mLength;
    private int mOverUnderRunCount;
    private byte[] mRawData;
    private int mStatus = 0;
    protected final byte mType;

    UsbDescriptor(int length, byte type) {
        if (length < 2) {
            throw new IllegalArgumentException();
        }
        this.mLength = length;
        this.mType = type;
    }

    public int getLength() {
        return this.mLength;
    }

    public byte getType() {
        return this.mType;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public void setStatus(int status) {
        this.mStatus = status;
    }

    public int getOverUnderRunCount() {
        return this.mOverUnderRunCount;
    }

    public String getStatusString() {
        return sStatusStrings[this.mStatus];
    }

    public byte[] getRawData() {
        return this.mRawData;
    }

    public void postParse(ByteStream stream) {
        int bytesRead = stream.getReadCount();
        if (bytesRead < this.mLength) {
            stream.advance(this.mLength - bytesRead);
            this.mStatus = 2;
            this.mOverUnderRunCount = this.mLength - bytesRead;
            Log.w(TAG, "UNDERRUN t:0x" + Integer.toHexString(this.mType) + " r: " + bytesRead + " < l: " + this.mLength);
        } else if (bytesRead > this.mLength) {
            stream.reverse(bytesRead - this.mLength);
            this.mStatus = 3;
            this.mOverUnderRunCount = bytesRead - this.mLength;
            Log.w(TAG, "OVERRRUN t:0x" + Integer.toHexString(this.mType) + " r: " + bytesRead + " > l: " + this.mLength);
        } else {
            this.mStatus = 1;
        }
    }

    public int parseRawDescriptors(ByteStream stream) {
        int dataLen = this.mLength - stream.getReadCount();
        if (dataLen > 0) {
            this.mRawData = new byte[dataLen];
            for (int index = 0; index < dataLen; index++) {
                this.mRawData[index] = stream.getByte();
            }
        }
        return this.mLength;
    }

    public static String getUsbDescriptorString(UsbDeviceConnection connection, byte strIndex) {
        String usbStr = "";
        if (strIndex == (byte) 0) {
            return usbStr;
        }
        try {
            int rdo = connection.controlTransfer(128, 6, strIndex | UsbTerminalTypes.TERMINAL_OUT_UNDEFINED, 0, sStringBuffer, 255, 0);
            if (rdo >= 0) {
                return new String(sStringBuffer, 2, rdo - 2, "UTF-16LE");
            }
            return "?";
        } catch (Exception e) {
            Log.e(TAG, "Can not communicate with USB device", e);
            return usbStr;
        }
    }

    private void reportParseStatus(ReportCanvas canvas) {
        switch (getStatus()) {
            case 0:
            case 2:
            case 3:
                canvas.writeParagraph("status: " + getStatusString() + " [" + getOverUnderRunCount() + "]", true);
                return;
            default:
                return;
        }
    }

    public void report(ReportCanvas canvas) {
        String text = UsbStrings.getDescriptorName(getType()) + ": " + ReportCanvas.getHexString(getType()) + " Len: " + getLength();
        if (this.mHierarchyLevel != 0) {
            canvas.writeHeader(this.mHierarchyLevel, text);
        } else {
            canvas.writeParagraph(text, false);
        }
        if (getStatus() != 1) {
            reportParseStatus(canvas);
        }
    }

    public void shortReport(ReportCanvas canvas) {
        canvas.writeParagraph(UsbStrings.getDescriptorName(getType()) + ": " + ReportCanvas.getHexString(getType()) + " Len: " + getLength(), false);
    }
}

package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;
import com.android.server.usb.descriptors.report.ReportCanvas;
import com.android.server.usb.descriptors.report.Reporting;
import com.android.server.usb.descriptors.report.UsbStrings;

public abstract class UsbDescriptor implements Reporting {
    public static final int AUDIO_AUDIOCONTROL = 1;
    public static final int AUDIO_AUDIOSTREAMING = 2;
    public static final int AUDIO_MIDISTREAMING = 3;
    public static final int AUDIO_SUBCLASS_UNDEFINED = 0;
    public static final int CLASSID_APPSPECIFIC = 254;
    public static final int CLASSID_AUDIO = 1;
    public static final int CLASSID_AUDIOVIDEO = 16;
    public static final int CLASSID_BILLBOARD = 17;
    public static final int CLASSID_CDC_CONTROL = 10;
    public static final int CLASSID_COM = 2;
    public static final int CLASSID_DEVICE = 0;
    public static final int CLASSID_DIAGNOSTIC = 220;
    public static final int CLASSID_HEALTHCARE = 15;
    public static final int CLASSID_HID = 3;
    public static final int CLASSID_HUB = 9;
    public static final int CLASSID_IMAGE = 6;
    public static final int CLASSID_MISC = 239;
    public static final int CLASSID_PHYSICAL = 5;
    public static final int CLASSID_PRINTER = 7;
    public static final int CLASSID_SECURITY = 13;
    public static final int CLASSID_SMART_CARD = 11;
    public static final int CLASSID_STORAGE = 8;
    public static final int CLASSID_TYPECBRIDGE = 18;
    public static final int CLASSID_VENDSPECIFIC = 255;
    public static final int CLASSID_VIDEO = 14;
    public static final int CLASSID_WIRELESS = 224;
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
        if (length >= 2) {
            this.mLength = length;
            this.mType = type;
            return;
        }
        throw new IllegalArgumentException();
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
        String str;
        StringBuilder stringBuilder;
        if (bytesRead < this.mLength) {
            stream.advance(this.mLength - bytesRead);
            this.mStatus = 2;
            this.mOverUnderRunCount = this.mLength - bytesRead;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("UNDERRUN t:0x");
            stringBuilder.append(Integer.toHexString(this.mType));
            stringBuilder.append(" r: ");
            stringBuilder.append(bytesRead);
            stringBuilder.append(" < l: ");
            stringBuilder.append(this.mLength);
            Log.w(str, stringBuilder.toString());
        } else if (bytesRead > this.mLength) {
            stream.reverse(bytesRead - this.mLength);
            this.mStatus = 3;
            this.mOverUnderRunCount = bytesRead - this.mLength;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("OVERRRUN t:0x");
            stringBuilder.append(Integer.toHexString(this.mType));
            stringBuilder.append(" r: ");
            stringBuilder.append(bytesRead);
            stringBuilder.append(" > l: ");
            stringBuilder.append(this.mLength);
            Log.w(str, stringBuilder.toString());
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
        String usbStr = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (strIndex == (byte) 0) {
            return usbStr;
        }
        try {
            int rdo = connection.controlTransfer(128, 6, 768 | strIndex, 0, sStringBuffer, 255, 0);
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("status: ");
                stringBuilder.append(getStatusString());
                stringBuilder.append(" [");
                stringBuilder.append(getOverUnderRunCount());
                stringBuilder.append("]");
                canvas.writeParagraph(stringBuilder.toString(), true);
                return;
            default:
                return;
        }
    }

    public void report(ReportCanvas canvas) {
        String descTypeStr = UsbStrings.getDescriptorName(getType());
        String text = new StringBuilder();
        text.append(descTypeStr);
        text.append(": ");
        text.append(ReportCanvas.getHexString(getType()));
        text.append(" Len: ");
        text.append(getLength());
        text = text.toString();
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
        String descTypeStr = UsbStrings.getDescriptorName(getType());
        String text = new StringBuilder();
        text.append(descTypeStr);
        text.append(": ");
        text.append(ReportCanvas.getHexString(getType()));
        text.append(" Len: ");
        text.append(getLength());
        canvas.writeParagraph(text.toString(), false);
    }
}

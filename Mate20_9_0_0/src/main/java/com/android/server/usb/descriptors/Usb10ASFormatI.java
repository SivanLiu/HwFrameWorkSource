package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ASFormatI extends UsbASFormat {
    private static final String TAG = "Usb10ASFormatI";
    private byte mBitResolution;
    private byte mNumChannels;
    private byte mSampleFreqType;
    private int[] mSampleRates;
    private byte mSubframeSize;

    public Usb10ASFormatI(int length, byte type, byte subtype, byte formatType, int subclass) {
        super(length, type, subtype, formatType, subclass);
    }

    public byte getNumChannels() {
        return this.mNumChannels;
    }

    public byte getSubframeSize() {
        return this.mSubframeSize;
    }

    public byte getBitResolution() {
        return this.mBitResolution;
    }

    public byte getSampleFreqType() {
        return this.mSampleFreqType;
    }

    public int[] getSampleRates() {
        return this.mSampleRates;
    }

    public int[] getBitDepths() {
        return new int[]{this.mBitResolution};
    }

    public int[] getChannelCounts() {
        return new int[]{this.mNumChannels};
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mNumChannels = stream.getByte();
        this.mSubframeSize = stream.getByte();
        this.mBitResolution = stream.getByte();
        this.mSampleFreqType = stream.getByte();
        byte index = (byte) 0;
        if (this.mSampleFreqType != (byte) 0) {
            this.mSampleRates = new int[this.mSampleFreqType];
            while (true) {
                byte index2 = index;
                if (index2 >= this.mSampleFreqType) {
                    break;
                }
                this.mSampleRates[index2] = stream.unpackUsbTriple();
                index = index2 + 1;
            }
        } else {
            this.mSampleRates = new int[2];
            this.mSampleRates[0] = stream.unpackUsbTriple();
            this.mSampleRates[1] = stream.unpackUsbTriple();
        }
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(getNumChannels());
        stringBuilder.append(" Channels.");
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Subframe Size: ");
        stringBuilder.append(getSubframeSize());
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Bit Resolution: ");
        stringBuilder.append(getBitResolution());
        canvas.writeListItem(stringBuilder.toString());
        byte sampleFreqType = getSampleFreqType();
        int[] sampleRates = getSampleRates();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Sample Freq Type: ");
        stringBuilder2.append(sampleFreqType);
        canvas.writeListItem(stringBuilder2.toString());
        canvas.openList();
        byte index = (byte) 0;
        StringBuilder stringBuilder3;
        if (sampleFreqType == (byte) 0) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("min: ");
            stringBuilder3.append(sampleRates[0]);
            canvas.writeListItem(stringBuilder3.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("max: ");
            stringBuilder2.append(sampleRates[1]);
            canvas.writeListItem(stringBuilder2.toString());
        } else {
            while (index < sampleFreqType) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                stringBuilder3.append(sampleRates[index]);
                canvas.writeListItem(stringBuilder3.toString());
                index++;
            }
        }
        canvas.closeList();
        canvas.closeList();
    }
}

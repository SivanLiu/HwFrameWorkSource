package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ASFormatII extends UsbASFormat {
    private static final String TAG = "Usb10ASFormatII";
    private int mMaxBitRate;
    private byte mSamFreqType;
    private int[] mSampleRates;
    private int mSamplesPerFrame;

    public Usb10ASFormatII(int length, byte type, byte subtype, byte formatType, int subclass) {
        super(length, type, subtype, formatType, subclass);
    }

    public int getMaxBitRate() {
        return this.mMaxBitRate;
    }

    public int getSamplesPerFrame() {
        return this.mSamplesPerFrame;
    }

    public byte getSamFreqType() {
        return this.mSamFreqType;
    }

    public int[] getSampleRates() {
        return this.mSampleRates;
    }

    public int parseRawDescriptors(ByteStream stream) {
        this.mMaxBitRate = stream.unpackUsbShort();
        this.mSamplesPerFrame = stream.unpackUsbShort();
        this.mSamFreqType = stream.getByte();
        int numFreqs = this.mSamFreqType == (byte) 0 ? 2 : this.mSamFreqType;
        this.mSampleRates = new int[numFreqs];
        for (int index = 0; index < numFreqs; index++) {
            this.mSampleRates[index] = stream.unpackUsbTriple();
        }
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Max Bit Rate: ");
        stringBuilder.append(getMaxBitRate());
        canvas.writeListItem(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Samples Per Frame: ");
        stringBuilder.append(getMaxBitRate());
        canvas.writeListItem(stringBuilder.toString());
        byte sampleFreqType = getSamFreqType();
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

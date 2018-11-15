package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ASFormatI extends UsbASFormat {
    private static final String TAG = "Usb10ASFormatI";
    private byte mBitResolution;
    private byte mNumChannels;
    private byte mSampleFreqType;
    private int[] mSampleRates;
    private byte mSubframeSize;

    public Usb10ASFormatI(int length, byte type, byte subtype, byte formatType, byte subclass) {
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
        if (this.mSampleFreqType == (byte) 0) {
            this.mSampleRates = new int[2];
            this.mSampleRates[0] = stream.unpackUsbTriple();
            this.mSampleRates[1] = stream.unpackUsbTriple();
        } else {
            this.mSampleRates = new int[this.mSampleFreqType];
            for (byte index = (byte) 0; index < this.mSampleFreqType; index++) {
                this.mSampleRates[index] = stream.unpackUsbTriple();
            }
        }
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        super.report(canvas);
        canvas.openList();
        canvas.writeListItem("" + getNumChannels() + " Channels.");
        canvas.writeListItem("Subframe Size: " + getSubframeSize());
        canvas.writeListItem("Bit Resolution: " + getBitResolution());
        byte sampleFreqType = getSampleFreqType();
        int[] sampleRates = getSampleRates();
        canvas.writeListItem("Sample Freq Type: " + sampleFreqType);
        canvas.openList();
        if (sampleFreqType == (byte) 0) {
            canvas.writeListItem("min: " + sampleRates[0]);
            canvas.writeListItem("max: " + sampleRates[1]);
        } else {
            for (byte index = (byte) 0; index < sampleFreqType; index++) {
                canvas.writeListItem("" + sampleRates[index]);
            }
        }
        canvas.closeList();
        canvas.closeList();
    }
}

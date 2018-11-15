package com.android.server.usb.descriptors;

import com.android.server.usb.descriptors.report.ReportCanvas;

public final class Usb10ACMixerUnit extends UsbACMixerUnit {
    private static final String TAG = "Usb10ACMixerUnit";
    private byte mChanNameID;
    private int mChannelConfig;
    private byte[] mControls;
    private byte mNameID;

    public Usb10ACMixerUnit(int length, byte type, byte subtype, int subClass) {
        super(length, type, subtype, subClass);
    }

    public int getChannelConfig() {
        return this.mChannelConfig;
    }

    public byte getChanNameID() {
        return this.mChanNameID;
    }

    public byte[] getControls() {
        return this.mControls;
    }

    public byte getNameID() {
        return this.mNameID;
    }

    public int parseRawDescriptors(ByteStream stream) {
        super.parseRawDescriptors(stream);
        this.mChannelConfig = stream.unpackUsbShort();
        this.mChanNameID = stream.getByte();
        int controlArraySize = UsbACMixerUnit.calcControlArraySize(this.mNumInputs, this.mNumOutputs);
        this.mControls = new byte[controlArraySize];
        for (int index = 0; index < controlArraySize; index++) {
            this.mControls[index] = stream.getByte();
        }
        this.mNameID = stream.getByte();
        return this.mLength;
    }

    public void report(ReportCanvas canvas) {
        StringBuilder stringBuilder;
        super.report(canvas);
        int ctrl = 0;
        canvas.writeParagraph("Mixer Unit", false);
        canvas.openList();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unit ID: ");
        stringBuilder2.append(ReportCanvas.getHexString(getUnitID()));
        canvas.writeListItem(stringBuilder2.toString());
        byte numInputs = getNumInputs();
        byte[] inputIDs = getInputIDs();
        canvas.openListItem();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Num Inputs: ");
        stringBuilder3.append(numInputs);
        stringBuilder3.append(" [");
        canvas.write(stringBuilder3.toString());
        for (byte input = (byte) 0; input < numInputs; input++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append(ReportCanvas.getHexString(inputIDs[input]));
            canvas.write(stringBuilder.toString());
            if (input < numInputs - 1) {
                canvas.write(" ");
            }
        }
        canvas.write("]");
        canvas.closeListItem();
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Num Outputs: ");
        stringBuilder3.append(getNumOutputs());
        canvas.writeListItem(stringBuilder3.toString());
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Channel Config: ");
        stringBuilder3.append(ReportCanvas.getHexString(getChannelConfig()));
        canvas.writeListItem(stringBuilder3.toString());
        byte[] controls = getControls();
        canvas.openListItem();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Controls: ");
        stringBuilder.append(controls.length);
        stringBuilder.append(" [");
        canvas.write(stringBuilder.toString());
        while (ctrl < controls.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder.append(controls[ctrl]);
            canvas.write(stringBuilder.toString());
            if (ctrl < controls.length - 1) {
                canvas.write(" ");
            }
            ctrl++;
        }
        canvas.write("]");
        canvas.closeListItem();
        canvas.closeList();
    }
}

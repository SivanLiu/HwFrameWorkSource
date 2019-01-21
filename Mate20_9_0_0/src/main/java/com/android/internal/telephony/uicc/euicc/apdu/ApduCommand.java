package com.android.internal.telephony.uicc.euicc.apdu;

class ApduCommand {
    public final int channel;
    public final int cla;
    public final String cmdHex;
    public final int ins;
    public final int p1;
    public final int p2;
    public final int p3;

    ApduCommand(int channel, int cla, int ins, int p1, int p2, int p3, String cmdHex) {
        this.channel = channel;
        this.cla = cla;
        this.ins = ins;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.cmdHex = cmdHex;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ApduCommand(channel=");
        stringBuilder.append(this.channel);
        stringBuilder.append(", cla=");
        stringBuilder.append(this.cla);
        stringBuilder.append(", ins=");
        stringBuilder.append(this.ins);
        stringBuilder.append(", p1=");
        stringBuilder.append(this.p1);
        stringBuilder.append(", p2=");
        stringBuilder.append(this.p2);
        stringBuilder.append(", p3=");
        stringBuilder.append(this.p3);
        stringBuilder.append(", cmd=");
        stringBuilder.append(this.cmdHex);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}

package com.android.server.hidata.wavemapping.chr.entity;

public class HistAppQoeChrInfo extends ChrInfo {
    public int AppName = 0;
    public int dataRx = 0;
    public int dataTx = 0;
    public int duration = 0;
    public int goodCnt = 0;
    public int modelVer_all = 0;
    public int modelVer_cell = 0;
    public int modelVer_main = 0;
    public short netFreq = (short) 0;
    public int netIdCnt = 0;
    public String netName = "UNKNOWN";
    public byte netType = (byte) 0;
    public int poorCnt = 0;
    public short recordDays = (short) 0;
    public short spaceId_all = (short) 0;
    public short spaceId_cell = (short) 0;
    public short spaceId_main = (short) 0;

    public void setSpaceInfo(short spaceId_a, int modelVer_a, short spaceId_m, int modelVer_m, short spaceId_c, int modelVer_c) {
        this.spaceId_all = spaceId_a;
        this.modelVer_all = modelVer_a;
        this.spaceId_main = spaceId_m;
        this.modelVer_main = modelVer_m;
        this.spaceId_cell = spaceId_c;
        this.modelVer_cell = modelVer_c;
    }

    public void setNetInfo(int netId, String name, short freq, byte type) {
        this.netIdCnt = netId;
        this.netName = name;
        this.netFreq = freq;
        this.netType = type;
    }

    public void setRecords(short days, int dur, int good, int poor, int rx, int tx) {
        this.recordDays = days;
        this.duration = dur;
        this.goodCnt = good;
        this.poorCnt = poor;
        this.dataRx = rx;
        this.dataTx = tx;
    }

    public void setAppName(int app) {
        this.AppName = app;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HistAppQoeChrInfo{spaceId_all=");
        stringBuilder.append(this.spaceId_all);
        stringBuilder.append(", modelVer_all=");
        stringBuilder.append(this.modelVer_all);
        stringBuilder.append(", spaceId_main=");
        stringBuilder.append(this.spaceId_main);
        stringBuilder.append(", modelVer_main=");
        stringBuilder.append(this.modelVer_main);
        stringBuilder.append(", spaceId_cell=");
        stringBuilder.append(this.spaceId_cell);
        stringBuilder.append(", modelVer_cell=");
        stringBuilder.append(this.modelVer_cell);
        stringBuilder.append(", netIdCnt=");
        stringBuilder.append(this.netIdCnt);
        stringBuilder.append(", netName=");
        stringBuilder.append(this.netName);
        stringBuilder.append(", netFreq=");
        stringBuilder.append(this.netFreq);
        stringBuilder.append(", netType=");
        stringBuilder.append(this.netType);
        stringBuilder.append(", recordDays=");
        stringBuilder.append(this.recordDays);
        stringBuilder.append(", duration=");
        stringBuilder.append(this.duration);
        stringBuilder.append(", goodCnt=");
        stringBuilder.append(this.goodCnt);
        stringBuilder.append(", poorCnt=");
        stringBuilder.append(this.poorCnt);
        stringBuilder.append(", dataRx=");
        stringBuilder.append(this.dataRx);
        stringBuilder.append(", dataTx=");
        stringBuilder.append(this.dataTx);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}

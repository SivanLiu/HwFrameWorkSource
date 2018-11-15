package com.android.server.hidata.appqoe;

public class HwAPPChrExcpInfo {
    public int netType = -1;
    public int para1 = -1;
    public int para2 = -1;
    public int para3 = -1;
    public int para4 = -1;
    public int rsPacket = -1;
    public int rssi = -1;
    public int rtt = -1;
    public int rxByte = -1;
    public int rxPacket = -1;
    public int txByte = -1;
    public int txPacket = -1;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" HwAPPChrExcpInfo netType = ");
        stringBuilder.append(this.netType);
        stringBuilder.append(" rssi = ");
        stringBuilder.append(this.rssi);
        stringBuilder.append(" rtt = ");
        stringBuilder.append(this.rtt);
        stringBuilder.append(" txPacket = ");
        stringBuilder.append(this.txPacket);
        stringBuilder.append(" txByte = ");
        stringBuilder.append(this.txByte);
        stringBuilder.append(" rxPacket = ");
        stringBuilder.append(this.rxPacket);
        stringBuilder.append(" rxByte = ");
        stringBuilder.append(this.rxByte);
        stringBuilder.append(" rsPacket = ");
        stringBuilder.append(this.rsPacket);
        stringBuilder.append(" para1 = ");
        stringBuilder.append(this.para1);
        stringBuilder.append(" para2 = ");
        stringBuilder.append(this.para2);
        stringBuilder.append(" para3 = ");
        stringBuilder.append(this.para3);
        stringBuilder.append(" para4 = ");
        stringBuilder.append(this.para4);
        return stringBuilder.toString();
    }
}

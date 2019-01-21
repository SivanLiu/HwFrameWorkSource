package android.net.metrics;

import android.net.MacAddress;
import java.util.StringJoiner;

public class WakeupEvent {
    public MacAddress dstHwAddr;
    public String dstIp;
    public int dstPort;
    public int ethertype;
    public String iface;
    public int ipNextHeader;
    public String srcIp;
    public int srcPort;
    public long timestampMs;
    public int uid;

    public String toString() {
        StringJoiner j = new StringJoiner(", ", "WakeupEvent(", ")");
        j.add(String.format("%tT.%tL", new Object[]{Long.valueOf(this.timestampMs), Long.valueOf(this.timestampMs)}));
        j.add(this.iface);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uid: ");
        stringBuilder.append(Integer.toString(this.uid));
        j.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("eth=0x");
        stringBuilder.append(Integer.toHexString(this.ethertype));
        j.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("dstHw=");
        stringBuilder.append(this.dstHwAddr);
        j.add(stringBuilder.toString());
        if (this.ipNextHeader > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ipNxtHdr=");
            stringBuilder.append(this.ipNextHeader);
            j.add(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("srcIp=");
            stringBuilder.append(this.srcIp);
            j.add(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("dstIp=");
            stringBuilder.append(this.dstIp);
            j.add(stringBuilder.toString());
            if (this.srcPort > -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("srcPort=");
                stringBuilder.append(this.srcPort);
                j.add(stringBuilder.toString());
            }
            if (this.dstPort > -1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("dstPort=");
                stringBuilder.append(this.dstPort);
                j.add(stringBuilder.toString());
            }
        }
        return j.toString();
    }
}

package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class WifiDebugPacketFateFrameType {
    public static final int ETHERNET_II = 1;
    public static final int MGMT_80211 = 2;
    public static final int UNKNOWN = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "ETHERNET_II";
        }
        if (o == 2) {
            return "MGMT_80211";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("UNKNOWN");
        if ((o & 1) == 1) {
            list.add("ETHERNET_II");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("MGMT_80211");
            flipped |= 2;
        }
        if (o != flipped) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString((~flipped) & o));
            list.add(stringBuilder.toString());
        }
        return String.join(" | ", list);
    }
}

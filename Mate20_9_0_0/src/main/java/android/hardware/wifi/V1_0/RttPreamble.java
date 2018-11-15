package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class RttPreamble {
    public static final int HT = 2;
    public static final int LEGACY = 1;
    public static final int VHT = 4;

    public static final String toString(int o) {
        if (o == 1) {
            return "LEGACY";
        }
        if (o == 2) {
            return "HT";
        }
        if (o == 4) {
            return "VHT";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        if ((o & 1) == 1) {
            list.add("LEGACY");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("HT");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("VHT");
            flipped |= 4;
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

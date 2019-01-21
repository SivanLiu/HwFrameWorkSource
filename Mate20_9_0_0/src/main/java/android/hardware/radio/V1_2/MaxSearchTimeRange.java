package android.hardware.radio.V1_2;

import java.util.ArrayList;

public final class MaxSearchTimeRange {
    public static final int MAX = 3600;
    public static final int MIN = 60;

    public static final String toString(int o) {
        if (o == 60) {
            return "MIN";
        }
        if (o == MAX) {
            return "MAX";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        if ((o & 60) == 60) {
            list.add("MIN");
            flipped = 0 | 60;
        }
        if ((o & MAX) == MAX) {
            list.add("MAX");
            flipped |= MAX;
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

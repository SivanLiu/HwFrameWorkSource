package android.hardware.radio.V1_2;

import java.util.ArrayList;

public final class IncrementalResultsPeriodicityRange {
    public static final int MAX = 10;
    public static final int MIN = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "MIN";
        }
        if (o == 10) {
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
        if ((o & 1) == 1) {
            list.add("MIN");
            flipped = 0 | 1;
        }
        if ((o & 10) == 10) {
            list.add("MAX");
            flipped |= 10;
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

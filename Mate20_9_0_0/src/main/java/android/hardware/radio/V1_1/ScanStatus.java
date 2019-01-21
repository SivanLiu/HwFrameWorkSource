package android.hardware.radio.V1_1;

import java.util.ArrayList;

public final class ScanStatus {
    public static final int COMPLETE = 2;
    public static final int PARTIAL = 1;

    public static final String toString(int o) {
        if (o == 1) {
            return "PARTIAL";
        }
        if (o == 2) {
            return "COMPLETE";
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
            list.add("PARTIAL");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("COMPLETE");
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

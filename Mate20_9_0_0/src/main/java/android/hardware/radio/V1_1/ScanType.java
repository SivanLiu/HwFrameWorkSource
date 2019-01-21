package android.hardware.radio.V1_1;

import java.util.ArrayList;

public final class ScanType {
    public static final int ONE_SHOT = 0;
    public static final int PERIODIC = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "ONE_SHOT";
        }
        if (o == 1) {
            return "PERIODIC";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("ONE_SHOT");
        if ((o & 1) == 1) {
            list.add("PERIODIC");
            flipped = 0 | 1;
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

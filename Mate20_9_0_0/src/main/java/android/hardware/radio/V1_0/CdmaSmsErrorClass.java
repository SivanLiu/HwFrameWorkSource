package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class CdmaSmsErrorClass {
    public static final int ERROR = 1;
    public static final int NO_ERROR = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "NO_ERROR";
        }
        if (o == 1) {
            return "ERROR";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("NO_ERROR");
        if ((o & 1) == 1) {
            list.add("ERROR");
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

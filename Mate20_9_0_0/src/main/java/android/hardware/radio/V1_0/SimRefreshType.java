package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class SimRefreshType {
    public static final int SIM_FILE_UPDATE = 0;
    public static final int SIM_INIT = 1;
    public static final int SIM_RESET = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "SIM_FILE_UPDATE";
        }
        if (o == 1) {
            return "SIM_INIT";
        }
        if (o == 2) {
            return "SIM_RESET";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("SIM_FILE_UPDATE");
        if ((o & 1) == 1) {
            list.add("SIM_INIT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("SIM_RESET");
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

package android.hardware.radio.V1_2;

import java.util.ArrayList;

public final class DataRequestReason {
    public static final int HANDOVER = 3;
    public static final int NORMAL = 1;
    public static final int SHUTDOWN = 2;

    public static final String toString(int o) {
        if (o == 1) {
            return "NORMAL";
        }
        if (o == 2) {
            return "SHUTDOWN";
        }
        if (o == 3) {
            return "HANDOVER";
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
            list.add("NORMAL");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("SHUTDOWN");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("HANDOVER");
            flipped |= 3;
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

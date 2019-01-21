package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class RadioCapabilityStatus {
    public static final int FAIL = 2;
    public static final int NONE = 0;
    public static final int SUCCESS = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "NONE";
        }
        if (o == 1) {
            return "SUCCESS";
        }
        if (o == 2) {
            return "FAIL";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("NONE");
        if ((o & 1) == 1) {
            list.add("SUCCESS");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("FAIL");
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

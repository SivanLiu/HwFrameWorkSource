package android.hardware.radio.V1_1;

import java.util.ArrayList;

public final class KeepaliveStatusCode {
    public static final int ACTIVE = 0;
    public static final int INACTIVE = 1;
    public static final int PENDING = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "ACTIVE";
        }
        if (o == 1) {
            return "INACTIVE";
        }
        if (o == 2) {
            return "PENDING";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("ACTIVE");
        if ((o & 1) == 1) {
            list.add("INACTIVE");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("PENDING");
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

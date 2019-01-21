package android.hardware.radio.V1_1;

import java.util.ArrayList;

public final class KeepaliveType {
    public static final int NATT_IPV4 = 0;
    public static final int NATT_IPV6 = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "NATT_IPV4";
        }
        if (o == 1) {
            return "NATT_IPV6";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("NATT_IPV4");
        if ((o & 1) == 1) {
            list.add("NATT_IPV6");
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

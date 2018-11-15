package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanSubscribeType {
    public static final int ACTIVE = 1;
    public static final int PASSIVE = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "PASSIVE";
        }
        if (o == 1) {
            return "ACTIVE";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("PASSIVE");
        if ((o & 1) == 1) {
            list.add("ACTIVE");
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

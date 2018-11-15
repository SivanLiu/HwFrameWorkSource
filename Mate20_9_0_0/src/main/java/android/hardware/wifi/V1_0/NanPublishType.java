package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanPublishType {
    public static final int SOLICITED = 1;
    public static final int UNSOLICITED = 0;
    public static final int UNSOLICITED_SOLICITED = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "UNSOLICITED";
        }
        if (o == 1) {
            return "SOLICITED";
        }
        if (o == 2) {
            return "UNSOLICITED_SOLICITED";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("UNSOLICITED");
        if ((o & 1) == 1) {
            list.add("SOLICITED");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("UNSOLICITED_SOLICITED");
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

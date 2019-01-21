package android.hardware.radio.V1_0;

import java.util.ArrayList;

public final class SapTransferProtocol {
    public static final int T0 = 0;
    public static final int T1 = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "T0";
        }
        if (o == 1) {
            return "T1";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("T0");
        if ((o & 1) == 1) {
            list.add("T1");
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

package vendor.huawei.hardware.hisiradio.V1_0;

import java.util.ArrayList;

public final class RadioIndicationType {
    public static final int UNSOLICITED = 0;
    public static final int UNSOLICITED_ACK_EXP = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "UNSOLICITED";
        }
        if (o == 1) {
            return "UNSOLICITED_ACK_EXP";
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
            list.add("UNSOLICITED_ACK_EXP");
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

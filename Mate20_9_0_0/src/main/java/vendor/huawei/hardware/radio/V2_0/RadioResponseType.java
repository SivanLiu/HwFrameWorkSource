package vendor.huawei.hardware.radio.V2_0;

import java.util.ArrayList;

public final class RadioResponseType {
    public static final int SOLICITED = 0;
    public static final int SOLICITED_ACK = 1;
    public static final int SOLICITED_ACK_EXP = 2;

    public static final String toString(int o) {
        if (o == 0) {
            return "SOLICITED";
        }
        if (o == 1) {
            return "SOLICITED_ACK";
        }
        if (o == 2) {
            return "SOLICITED_ACK_EXP";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("SOLICITED");
        if ((o & 1) == 1) {
            list.add("SOLICITED_ACK");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("SOLICITED_ACK_EXP");
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

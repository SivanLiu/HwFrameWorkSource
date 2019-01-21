package vendor.huawei.hardware.hisiradio.V1_0;

import java.util.ArrayList;

public final class RILCardUimLock {
    public static final int RIL_CARD_UIM_LOCKED = 2;
    public static final int RIL_CARD_UIM_UNKNOWN_LOCK = 0;
    public static final int RIL_CARD_UIM_UNLOCKED = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_CARD_UIM_UNKNOWN_LOCK";
        }
        if (o == 1) {
            return "RIL_CARD_UIM_UNLOCKED";
        }
        if (o == 2) {
            return "RIL_CARD_UIM_LOCKED";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("RIL_CARD_UIM_UNKNOWN_LOCK");
        if ((o & 1) == 1) {
            list.add("RIL_CARD_UIM_UNLOCKED");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("RIL_CARD_UIM_LOCKED");
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

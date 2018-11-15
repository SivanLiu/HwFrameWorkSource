package vendor.huawei.hardware.hwstp.V1_0;

import java.util.ArrayList;

public final class StpCategory {
    public static final int STP_ACTIVE_THREAT = 2;
    public static final int STP_INTEGRITY_THREAT = 4;
    public static final int STP_POTENTIAL_THREAT = 1;
    public static final int STP_ROOT_THREAT = 8;

    public static final String toString(int o) {
        if (o == 1) {
            return "STP_POTENTIAL_THREAT";
        }
        if (o == 2) {
            return "STP_ACTIVE_THREAT";
        }
        if (o == 4) {
            return "STP_INTEGRITY_THREAT";
        }
        if (o == 8) {
            return "STP_ROOT_THREAT";
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
            list.add("STP_POTENTIAL_THREAT");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("STP_ACTIVE_THREAT");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("STP_INTEGRITY_THREAT");
            flipped |= 4;
        }
        if ((o & 8) == 8) {
            list.add("STP_ROOT_THREAT");
            flipped |= 8;
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

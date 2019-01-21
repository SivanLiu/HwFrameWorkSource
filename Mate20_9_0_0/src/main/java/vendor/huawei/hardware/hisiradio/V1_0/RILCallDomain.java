package vendor.huawei.hardware.hisiradio.V1_0;

import java.util.ArrayList;

public final class RILCallDomain {
    public static final int RIL_CALL_DOMAIN_AUTOMATIC = 3;
    public static final int RIL_CALL_DOMAIN_CS = 1;
    public static final int RIL_CALL_DOMAIN_PS = 2;
    public static final int RIL_CALL_DOMAIN_UNKNOWN = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_CALL_DOMAIN_UNKNOWN";
        }
        if (o == 1) {
            return "RIL_CALL_DOMAIN_CS";
        }
        if (o == 2) {
            return "RIL_CALL_DOMAIN_PS";
        }
        if (o == 3) {
            return "RIL_CALL_DOMAIN_AUTOMATIC";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("RIL_CALL_DOMAIN_UNKNOWN");
        if ((o & 1) == 1) {
            list.add("RIL_CALL_DOMAIN_CS");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("RIL_CALL_DOMAIN_PS");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("RIL_CALL_DOMAIN_AUTOMATIC");
            flipped |= 3;
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

package vendor.huawei.hardware.hisiradio.V1_0;

import java.util.ArrayList;

public final class RILUICCAUTHFILEOPTCMDENUM {
    public static final int RIL_UICC_AUTH_FILE_OPT_READ = 0;
    public static final int RIL_UICC_AUTH_FILE_OPT_WRITE = 1;

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_UICC_AUTH_FILE_OPT_READ";
        }
        if (o == 1) {
            return "RIL_UICC_AUTH_FILE_OPT_WRITE";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("RIL_UICC_AUTH_FILE_OPT_READ");
        if ((o & 1) == 1) {
            list.add("RIL_UICC_AUTH_FILE_OPT_WRITE");
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

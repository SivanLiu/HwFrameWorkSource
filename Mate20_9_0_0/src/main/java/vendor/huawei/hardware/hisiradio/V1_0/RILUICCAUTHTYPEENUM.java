package vendor.huawei.hardware.hisiradio.V1_0;

import java.util.ArrayList;

public final class RILUICCAUTHTYPEENUM {
    public static final int UICC_AUTH_TYPE_ISIM = 1;
    public static final int UICC_AUTH_TYPE_USIM = 0;

    public static final String toString(int o) {
        if (o == 0) {
            return "UICC_AUTH_TYPE_USIM";
        }
        if (o == 1) {
            return "UICC_AUTH_TYPE_ISIM";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        list.add("UICC_AUTH_TYPE_USIM");
        if ((o & 1) == 1) {
            list.add("UICC_AUTH_TYPE_ISIM");
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

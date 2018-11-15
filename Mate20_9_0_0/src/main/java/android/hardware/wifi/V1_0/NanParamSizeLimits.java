package android.hardware.wifi.V1_0;

import java.util.ArrayList;

public final class NanParamSizeLimits {
    public static final int MAX_PASSPHRASE_LENGTH = 63;
    public static final int MIN_PASSPHRASE_LENGTH = 8;

    public static final String toString(int o) {
        if (o == 8) {
            return "MIN_PASSPHRASE_LENGTH";
        }
        if (o == 63) {
            return "MAX_PASSPHRASE_LENGTH";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        if ((o & 8) == 8) {
            list.add("MIN_PASSPHRASE_LENGTH");
            flipped = 0 | 8;
        }
        if ((o & 63) == 63) {
            list.add("MAX_PASSPHRASE_LENGTH");
            flipped |= 63;
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

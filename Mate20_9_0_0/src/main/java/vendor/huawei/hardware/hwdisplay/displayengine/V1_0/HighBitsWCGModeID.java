package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

import java.util.ArrayList;

public final class HighBitsWCGModeID {
    public static final int MODE_ADOBERGB = 4096;
    public static final int MODE_DISPLAYP3 = 8192;
    public static final int MODE_SUPERGAMUT = 12288;

    public static final String toString(int o) {
        if (o == 4096) {
            return "MODE_ADOBERGB";
        }
        if (o == 8192) {
            return "MODE_DISPLAYP3";
        }
        if (o == MODE_SUPERGAMUT) {
            return "MODE_SUPERGAMUT";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(o));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList();
        int flipped = 0;
        if ((o & 4096) == 4096) {
            list.add("MODE_ADOBERGB");
            flipped = 0 | 4096;
        }
        if ((o & 8192) == 8192) {
            list.add("MODE_DISPLAYP3");
            flipped |= 8192;
        }
        if ((o & MODE_SUPERGAMUT) == MODE_SUPERGAMUT) {
            list.add("MODE_SUPERGAMUT");
            flipped |= MODE_SUPERGAMUT;
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

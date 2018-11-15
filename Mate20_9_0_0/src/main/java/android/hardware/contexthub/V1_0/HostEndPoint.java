package android.hardware.contexthub.V1_0;

import java.util.ArrayList;

public final class HostEndPoint {
    public static final short BROADCAST = (short) -1;
    public static final short UNSPECIFIED = (short) -2;

    public static final String toString(short o) {
        if (o == (short) -1) {
            return "BROADCAST";
        }
        if (o == (short) -2) {
            return "UNSPECIFIED";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(Short.toUnsignedInt(o)));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(short o) {
        ArrayList<String> list = new ArrayList();
        short flipped = (short) 0;
        if ((o & -1) == -1) {
            list.add("BROADCAST");
            flipped = (short) (0 | -1);
        }
        if ((o & -2) == -2) {
            list.add("UNSPECIFIED");
            flipped = (short) (flipped | -2);
        }
        if (o != flipped) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString(Short.toUnsignedInt((short) ((~flipped) & o))));
            list.add(stringBuilder.toString());
        }
        return String.join(" | ", list);
    }
}

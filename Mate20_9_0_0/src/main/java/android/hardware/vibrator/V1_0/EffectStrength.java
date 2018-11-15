package android.hardware.vibrator.V1_0;

import java.util.ArrayList;

public final class EffectStrength {
    public static final byte LIGHT = (byte) 0;
    public static final byte MEDIUM = (byte) 1;
    public static final byte STRONG = (byte) 2;

    public static final String toString(byte o) {
        if (o == (byte) 0) {
            return "LIGHT";
        }
        if (o == (byte) 1) {
            return "MEDIUM";
        }
        if (o == (byte) 2) {
            return "STRONG";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(Byte.toUnsignedInt(o)));
        return stringBuilder.toString();
    }

    public static final String dumpBitfield(byte o) {
        ArrayList<String> list = new ArrayList();
        byte flipped = (byte) 0;
        list.add("LIGHT");
        if ((o & 1) == 1) {
            list.add("MEDIUM");
            flipped = (byte) (0 | 1);
        }
        if ((o & 2) == 2) {
            list.add("STRONG");
            flipped = (byte) (flipped | 2);
        }
        if (o != flipped) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString(Byte.toUnsignedInt((byte) ((~flipped) & o))));
            list.add(stringBuilder.toString());
        }
        return String.join(" | ", list);
    }
}

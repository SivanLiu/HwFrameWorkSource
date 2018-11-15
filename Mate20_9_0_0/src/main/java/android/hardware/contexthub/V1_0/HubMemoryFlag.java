package android.hardware.contexthub.V1_0;

import java.util.ArrayList;

public final class HubMemoryFlag {
    public static final int EXEC = 4;
    public static final int READ = 1;
    public static final int WRITE = 2;

    public static final String toString(int o) {
        if (o == 1) {
            return "READ";
        }
        if (o == 2) {
            return "WRITE";
        }
        if (o == 4) {
            return "EXEC";
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
            list.add("READ");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("WRITE");
            flipped |= 2;
        }
        if ((o & 4) == 4) {
            list.add("EXEC");
            flipped |= 4;
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

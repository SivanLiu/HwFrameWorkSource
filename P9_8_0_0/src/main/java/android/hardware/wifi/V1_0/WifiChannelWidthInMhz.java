package android.hardware.wifi.V1_0;

public final class WifiChannelWidthInMhz {
    public static final int WIDTH_10 = 6;
    public static final int WIDTH_160 = 3;
    public static final int WIDTH_20 = 0;
    public static final int WIDTH_40 = 1;
    public static final int WIDTH_5 = 5;
    public static final int WIDTH_80 = 2;
    public static final int WIDTH_80P80 = 4;
    public static final int WIDTH_INVALID = -1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.V1_0.WifiChannelWidthInMhz.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.V1_0.WifiChannelWidthInMhz.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "WIDTH_20";
        }
        if (o == 1) {
            return "WIDTH_40";
        }
        if (o == 2) {
            return "WIDTH_80";
        }
        if (o == 3) {
            return "WIDTH_160";
        }
        if (o == 4) {
            return "WIDTH_80P80";
        }
        if (o == 5) {
            return "WIDTH_5";
        }
        if (o == 6) {
            return "WIDTH_10";
        }
        if (o == -1) {
            return "WIDTH_INVALID";
        }
        return "0x" + Integer.toHexString(o);
    }
}

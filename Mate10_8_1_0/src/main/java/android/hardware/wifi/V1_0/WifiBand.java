package android.hardware.wifi.V1_0;

public final class WifiBand {
    public static final int BAND_24GHZ = 1;
    public static final int BAND_24GHZ_5GHZ = 3;
    public static final int BAND_24GHZ_5GHZ_WITH_DFS = 7;
    public static final int BAND_5GHZ = 2;
    public static final int BAND_5GHZ_DFS = 4;
    public static final int BAND_5GHZ_WITH_DFS = 6;
    public static final int BAND_UNSPECIFIED = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.V1_0.WifiBand.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.V1_0.WifiBand.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "BAND_UNSPECIFIED";
        }
        if (o == 1) {
            return "BAND_24GHZ";
        }
        if (o == 2) {
            return "BAND_5GHZ";
        }
        if (o == 4) {
            return "BAND_5GHZ_DFS";
        }
        if (o == 6) {
            return "BAND_5GHZ_WITH_DFS";
        }
        if (o == 3) {
            return "BAND_24GHZ_5GHZ";
        }
        if (o == 7) {
            return "BAND_24GHZ_5GHZ_WITH_DFS";
        }
        return "0x" + Integer.toHexString(o);
    }
}

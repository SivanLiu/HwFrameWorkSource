package android.hardware.wifi.supplicant.V1_0;

public final class P2pGroupCapabilityMask {
    public static final int CROSS_CONN = 16;
    public static final int GROUP_FORMATION = 64;
    public static final int GROUP_LIMIT = 4;
    public static final int GROUP_OWNER = 1;
    public static final int INTRA_BSS_DIST = 8;
    public static final int PERSISTENT_GROUP = 2;
    public static final int PERSISTENT_RECONN = 32;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.supplicant.V1_0.P2pGroupCapabilityMask.dumpBitfield(int):java.lang.String
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.supplicant.V1_0.P2pGroupCapabilityMask.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "GROUP_OWNER";
        }
        if (o == 2) {
            return "PERSISTENT_GROUP";
        }
        if (o == 4) {
            return "GROUP_LIMIT";
        }
        if (o == 8) {
            return "INTRA_BSS_DIST";
        }
        if (o == 16) {
            return "CROSS_CONN";
        }
        if (o == 32) {
            return "PERSISTENT_RECONN";
        }
        if (o == 64) {
            return "GROUP_FORMATION";
        }
        return "0x" + Integer.toHexString(o);
    }
}

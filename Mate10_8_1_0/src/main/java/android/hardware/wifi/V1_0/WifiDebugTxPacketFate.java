package android.hardware.wifi.V1_0;

public final class WifiDebugTxPacketFate {
    public static final int ACKED = 0;
    public static final int DRV_DROP_INVALID = 7;
    public static final int DRV_DROP_NOBUFS = 8;
    public static final int DRV_DROP_OTHER = 9;
    public static final int DRV_QUEUED = 6;
    public static final int FW_DROP_INVALID = 3;
    public static final int FW_DROP_NOBUFS = 4;
    public static final int FW_DROP_OTHER = 5;
    public static final int FW_QUEUED = 2;
    public static final int SENT = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.V1_0.WifiDebugTxPacketFate.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.V1_0.WifiDebugTxPacketFate.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "ACKED";
        }
        if (o == 1) {
            return "SENT";
        }
        if (o == 2) {
            return "FW_QUEUED";
        }
        if (o == 3) {
            return "FW_DROP_INVALID";
        }
        if (o == 4) {
            return "FW_DROP_NOBUFS";
        }
        if (o == 5) {
            return "FW_DROP_OTHER";
        }
        if (o == 6) {
            return "DRV_QUEUED";
        }
        if (o == 7) {
            return "DRV_DROP_INVALID";
        }
        if (o == 8) {
            return "DRV_DROP_NOBUFS";
        }
        if (o == 9) {
            return "DRV_DROP_OTHER";
        }
        return "0x" + Integer.toHexString(o);
    }
}

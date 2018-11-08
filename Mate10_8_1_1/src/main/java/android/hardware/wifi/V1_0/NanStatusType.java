package android.hardware.wifi.V1_0;

public final class NanStatusType {
    public static final int ALREADY_ENABLED = 10;
    public static final int FOLLOWUP_TX_QUEUE_FULL = 11;
    public static final int INTERNAL_FAILURE = 1;
    public static final int INVALID_ARGS = 5;
    public static final int INVALID_NDP_ID = 7;
    public static final int INVALID_PEER_ID = 6;
    public static final int INVALID_SESSION_ID = 3;
    public static final int NAN_NOT_ALLOWED = 8;
    public static final int NO_OTA_ACK = 9;
    public static final int NO_RESOURCES_AVAILABLE = 4;
    public static final int PROTOCOL_FAILURE = 2;
    public static final int SUCCESS = 0;
    public static final int UNSUPPORTED_CONCURRENCY_NAN_DISABLED = 12;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.V1_0.NanStatusType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.V1_0.NanStatusType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "SUCCESS";
        }
        if (o == 1) {
            return "INTERNAL_FAILURE";
        }
        if (o == 2) {
            return "PROTOCOL_FAILURE";
        }
        if (o == 3) {
            return "INVALID_SESSION_ID";
        }
        if (o == 4) {
            return "NO_RESOURCES_AVAILABLE";
        }
        if (o == 5) {
            return "INVALID_ARGS";
        }
        if (o == 6) {
            return "INVALID_PEER_ID";
        }
        if (o == 7) {
            return "INVALID_NDP_ID";
        }
        if (o == 8) {
            return "NAN_NOT_ALLOWED";
        }
        if (o == 9) {
            return "NO_OTA_ACK";
        }
        if (o == 10) {
            return "ALREADY_ENABLED";
        }
        if (o == 11) {
            return "FOLLOWUP_TX_QUEUE_FULL";
        }
        if (o == 12) {
            return "UNSUPPORTED_CONCURRENCY_NAN_DISABLED";
        }
        return "0x" + Integer.toHexString(o);
    }
}

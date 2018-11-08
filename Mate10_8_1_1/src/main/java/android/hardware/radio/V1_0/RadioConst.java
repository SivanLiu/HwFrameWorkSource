package android.hardware.radio.V1_0;

public final class RadioConst {
    public static final int CARD_MAX_APPS = 8;
    public static final int CDMA_ALPHA_INFO_BUFFER_LENGTH = 64;
    public static final int CDMA_MAX_NUMBER_OF_INFO_RECS = 10;
    public static final int CDMA_NUMBER_INFO_BUFFER_LENGTH = 81;
    public static final int MAX_CLIENT_ID_LENGTH = 2;
    public static final int MAX_DEBUG_SOCKET_NAME_LENGTH = 12;
    public static final int MAX_QEMU_PIPE_NAME_LENGTH = 11;
    public static final int MAX_RILDS = 3;
    public static final int MAX_SOCKET_NAME_LENGTH = 6;
    public static final int MAX_UUID_LENGTH = 64;
    public static final int NUM_SERVICE_CLASSES = 7;
    public static final int NUM_TX_POWER_LEVELS = 5;
    public static final int SS_INFO_MAX = 4;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RadioConst.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RadioConst.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 64) {
            return "CDMA_ALPHA_INFO_BUFFER_LENGTH";
        }
        if (o == 81) {
            return "CDMA_NUMBER_INFO_BUFFER_LENGTH";
        }
        if (o == 3) {
            return "MAX_RILDS";
        }
        if (o == 6) {
            return "MAX_SOCKET_NAME_LENGTH";
        }
        if (o == 2) {
            return "MAX_CLIENT_ID_LENGTH";
        }
        if (o == 12) {
            return "MAX_DEBUG_SOCKET_NAME_LENGTH";
        }
        if (o == 11) {
            return "MAX_QEMU_PIPE_NAME_LENGTH";
        }
        if (o == 64) {
            return "MAX_UUID_LENGTH";
        }
        if (o == 8) {
            return "CARD_MAX_APPS";
        }
        if (o == 10) {
            return "CDMA_MAX_NUMBER_OF_INFO_RECS";
        }
        if (o == 4) {
            return "SS_INFO_MAX";
        }
        if (o == 7) {
            return "NUM_SERVICE_CLASSES";
        }
        if (o == 5) {
            return "NUM_TX_POWER_LEVELS";
        }
        return "0x" + Integer.toHexString(o);
    }
}

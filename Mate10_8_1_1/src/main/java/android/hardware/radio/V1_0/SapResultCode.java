package android.hardware.radio.V1_0;

public final class SapResultCode {
    public static final int CARD_ALREADY_POWERED_OFF = 3;
    public static final int CARD_ALREADY_POWERED_ON = 5;
    public static final int CARD_NOT_ACCESSSIBLE = 2;
    public static final int CARD_REMOVED = 4;
    public static final int DATA_NOT_AVAILABLE = 6;
    public static final int GENERIC_FAILURE = 1;
    public static final int NOT_SUPPORTED = 7;
    public static final int SUCCESS = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.SapResultCode.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.SapResultCode.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "SUCCESS";
        }
        if (o == 1) {
            return "GENERIC_FAILURE";
        }
        if (o == 2) {
            return "CARD_NOT_ACCESSSIBLE";
        }
        if (o == 3) {
            return "CARD_ALREADY_POWERED_OFF";
        }
        if (o == 4) {
            return "CARD_REMOVED";
        }
        if (o == 5) {
            return "CARD_ALREADY_POWERED_ON";
        }
        if (o == 6) {
            return "DATA_NOT_AVAILABLE";
        }
        if (o == 7) {
            return "NOT_SUPPORTED";
        }
        return "0x" + Integer.toHexString(o);
    }
}

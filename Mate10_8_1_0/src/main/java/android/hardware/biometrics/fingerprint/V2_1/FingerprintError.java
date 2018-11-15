package android.hardware.biometrics.fingerprint.V2_1;

public final class FingerprintError {
    public static final int ERROR_CANCELED = 5;
    public static final int ERROR_HW_UNAVAILABLE = 1;
    public static final int ERROR_LOCKOUT = 7;
    public static final int ERROR_NO_ERROR = 0;
    public static final int ERROR_NO_SPACE = 4;
    public static final int ERROR_TIMEOUT = 3;
    public static final int ERROR_UNABLE_TO_PROCESS = 2;
    public static final int ERROR_UNABLE_TO_REMOVE = 6;
    public static final int ERROR_VENDOR = 8;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.biometrics.fingerprint.V2_1.FingerprintError.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.biometrics.fingerprint.V2_1.FingerprintError.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "ERROR_NO_ERROR";
        }
        if (o == 1) {
            return "ERROR_HW_UNAVAILABLE";
        }
        if (o == 2) {
            return "ERROR_UNABLE_TO_PROCESS";
        }
        if (o == 3) {
            return "ERROR_TIMEOUT";
        }
        if (o == 4) {
            return "ERROR_NO_SPACE";
        }
        if (o == 5) {
            return "ERROR_CANCELED";
        }
        if (o == 6) {
            return "ERROR_UNABLE_TO_REMOVE";
        }
        if (o == 7) {
            return "ERROR_LOCKOUT";
        }
        if (o == 8) {
            return "ERROR_VENDOR";
        }
        return "0x" + Integer.toHexString(o);
    }
}

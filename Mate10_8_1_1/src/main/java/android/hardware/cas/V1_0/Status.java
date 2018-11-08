package android.hardware.cas.V1_0;

public final class Status {
    public static final int BAD_VALUE = 6;
    public static final int ERROR_CAS_CANNOT_HANDLE = 4;
    public static final int ERROR_CAS_DECRYPT = 13;
    public static final int ERROR_CAS_DECRYPT_UNIT_NOT_INITIALIZED = 12;
    public static final int ERROR_CAS_DEVICE_REVOKED = 11;
    public static final int ERROR_CAS_INSUFFICIENT_OUTPUT_PROTECTION = 9;
    public static final int ERROR_CAS_INVALID_STATE = 5;
    public static final int ERROR_CAS_LICENSE_EXPIRED = 2;
    public static final int ERROR_CAS_NOT_PROVISIONED = 7;
    public static final int ERROR_CAS_NO_LICENSE = 1;
    public static final int ERROR_CAS_RESOURCE_BUSY = 8;
    public static final int ERROR_CAS_SESSION_NOT_OPENED = 3;
    public static final int ERROR_CAS_TAMPER_DETECTED = 10;
    public static final int ERROR_CAS_UNKNOWN = 14;
    public static final int OK = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.cas.V1_0.Status.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.cas.V1_0.Status.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "OK";
        }
        if (o == 1) {
            return "ERROR_CAS_NO_LICENSE";
        }
        if (o == 2) {
            return "ERROR_CAS_LICENSE_EXPIRED";
        }
        if (o == 3) {
            return "ERROR_CAS_SESSION_NOT_OPENED";
        }
        if (o == 4) {
            return "ERROR_CAS_CANNOT_HANDLE";
        }
        if (o == 5) {
            return "ERROR_CAS_INVALID_STATE";
        }
        if (o == 6) {
            return "BAD_VALUE";
        }
        if (o == 7) {
            return "ERROR_CAS_NOT_PROVISIONED";
        }
        if (o == 8) {
            return "ERROR_CAS_RESOURCE_BUSY";
        }
        if (o == 9) {
            return "ERROR_CAS_INSUFFICIENT_OUTPUT_PROTECTION";
        }
        if (o == 10) {
            return "ERROR_CAS_TAMPER_DETECTED";
        }
        if (o == 11) {
            return "ERROR_CAS_DEVICE_REVOKED";
        }
        if (o == 12) {
            return "ERROR_CAS_DECRYPT_UNIT_NOT_INITIALIZED";
        }
        if (o == 13) {
            return "ERROR_CAS_DECRYPT";
        }
        if (o == 14) {
            return "ERROR_CAS_UNKNOWN";
        }
        return "0x" + Integer.toHexString(o);
    }
}

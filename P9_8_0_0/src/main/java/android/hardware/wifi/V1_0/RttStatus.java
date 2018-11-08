package android.hardware.wifi.V1_0;

public final class RttStatus {
    public static final int ABORTED = 8;
    public static final int FAILURE = 1;
    public static final int FAIL_AP_ON_DIFF_CHANNEL = 6;
    public static final int FAIL_BUSY_TRY_LATER = 12;
    public static final int FAIL_FTM_PARAM_OVERRIDE = 15;
    public static final int FAIL_INVALID_TS = 9;
    public static final int FAIL_NOT_SCHEDULED_YET = 4;
    public static final int FAIL_NO_CAPABILITY = 7;
    public static final int FAIL_NO_RSP = 2;
    public static final int FAIL_PROTOCOL = 10;
    public static final int FAIL_REJECTED = 3;
    public static final int FAIL_SCHEDULE = 11;
    public static final int FAIL_TM_TIMEOUT = 5;
    public static final int INVALID_REQ = 13;
    public static final int NO_WIFI = 14;
    public static final int SUCCESS = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.V1_0.RttStatus.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.V1_0.RttStatus.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "SUCCESS";
        }
        if (o == 1) {
            return "FAILURE";
        }
        if (o == 2) {
            return "FAIL_NO_RSP";
        }
        if (o == 3) {
            return "FAIL_REJECTED";
        }
        if (o == 4) {
            return "FAIL_NOT_SCHEDULED_YET";
        }
        if (o == 5) {
            return "FAIL_TM_TIMEOUT";
        }
        if (o == 6) {
            return "FAIL_AP_ON_DIFF_CHANNEL";
        }
        if (o == 7) {
            return "FAIL_NO_CAPABILITY";
        }
        if (o == 8) {
            return "ABORTED";
        }
        if (o == 9) {
            return "FAIL_INVALID_TS";
        }
        if (o == 10) {
            return "FAIL_PROTOCOL";
        }
        if (o == 11) {
            return "FAIL_SCHEDULE";
        }
        if (o == 12) {
            return "FAIL_BUSY_TRY_LATER";
        }
        if (o == 13) {
            return "INVALID_REQ";
        }
        if (o == 14) {
            return "NO_WIFI";
        }
        if (o == 15) {
            return "FAIL_FTM_PARAM_OVERRIDE";
        }
        return "0x" + Integer.toHexString(o);
    }
}

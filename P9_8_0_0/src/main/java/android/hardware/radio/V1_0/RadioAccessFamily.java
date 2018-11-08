package android.hardware.radio.V1_0;

public final class RadioAccessFamily {
    public static final int EDGE = 4;
    public static final int EHRPD = 8192;
    public static final int EVDO_0 = 128;
    public static final int EVDO_A = 256;
    public static final int EVDO_B = 4096;
    public static final int GPRS = 2;
    public static final int GSM = 65536;
    public static final int HSDPA = 512;
    public static final int HSPA = 2048;
    public static final int HSPAP = 32768;
    public static final int HSUPA = 1024;
    public static final int IS95A = 16;
    public static final int IS95B = 32;
    public static final int LTE = 16384;
    public static final int LTE_CA = 524288;
    public static final int ONE_X_RTT = 64;
    public static final int TD_SCDMA = 131072;
    public static final int UMTS = 8;
    public static final int UNKNOWN = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RadioAccessFamily.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RadioAccessFamily.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "UNKNOWN";
        }
        if (o == 2) {
            return "GPRS";
        }
        if (o == 4) {
            return "EDGE";
        }
        if (o == 8) {
            return "UMTS";
        }
        if (o == 16) {
            return "IS95A";
        }
        if (o == 32) {
            return "IS95B";
        }
        if (o == 64) {
            return "ONE_X_RTT";
        }
        if (o == 128) {
            return "EVDO_0";
        }
        if (o == 256) {
            return "EVDO_A";
        }
        if (o == 512) {
            return "HSDPA";
        }
        if (o == HSUPA) {
            return "HSUPA";
        }
        if (o == HSPA) {
            return "HSPA";
        }
        if (o == EVDO_B) {
            return "EVDO_B";
        }
        if (o == EHRPD) {
            return "EHRPD";
        }
        if (o == 16384) {
            return "LTE";
        }
        if (o == 32768) {
            return "HSPAP";
        }
        if (o == 65536) {
            return "GSM";
        }
        if (o == TD_SCDMA) {
            return "TD_SCDMA";
        }
        if (o == 524288) {
            return "LTE_CA";
        }
        return "0x" + Integer.toHexString(o);
    }
}

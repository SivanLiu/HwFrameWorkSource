package android.hardware.radio.V1_0;

public final class RadioTechnology {
    public static final int EDGE = 2;
    public static final int EHRPD = 13;
    public static final int EVDO_0 = 7;
    public static final int EVDO_A = 8;
    public static final int EVDO_B = 12;
    public static final int GPRS = 1;
    public static final int GSM = 16;
    public static final int HSDPA = 9;
    public static final int HSPA = 11;
    public static final int HSPAP = 15;
    public static final int HSUPA = 10;
    public static final int IS95A = 4;
    public static final int IS95B = 5;
    public static final int IWLAN = 18;
    public static final int LTE = 14;
    public static final int LTE_CA = 19;
    public static final int ONE_X_RTT = 6;
    public static final int TD_SCDMA = 17;
    public static final int UMTS = 3;
    public static final int UNKNOWN = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RadioTechnology.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RadioTechnology.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "GPRS";
        }
        if (o == 2) {
            return "EDGE";
        }
        if (o == 3) {
            return "UMTS";
        }
        if (o == 4) {
            return "IS95A";
        }
        if (o == 5) {
            return "IS95B";
        }
        if (o == 6) {
            return "ONE_X_RTT";
        }
        if (o == 7) {
            return "EVDO_0";
        }
        if (o == 8) {
            return "EVDO_A";
        }
        if (o == 9) {
            return "HSDPA";
        }
        if (o == 10) {
            return "HSUPA";
        }
        if (o == 11) {
            return "HSPA";
        }
        if (o == 12) {
            return "EVDO_B";
        }
        if (o == 13) {
            return "EHRPD";
        }
        if (o == 14) {
            return "LTE";
        }
        if (o == 15) {
            return "HSPAP";
        }
        if (o == 16) {
            return "GSM";
        }
        if (o == 17) {
            return "TD_SCDMA";
        }
        if (o == 18) {
            return "IWLAN";
        }
        if (o == 19) {
            return "LTE_CA";
        }
        return "0x" + Integer.toHexString(o);
    }
}

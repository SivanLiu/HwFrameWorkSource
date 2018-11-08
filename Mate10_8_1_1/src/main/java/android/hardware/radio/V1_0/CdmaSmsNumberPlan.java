package android.hardware.radio.V1_0;

public final class CdmaSmsNumberPlan {
    public static final int DATA = 3;
    public static final int PRIVATE = 9;
    public static final int RESERVED_10 = 10;
    public static final int RESERVED_11 = 11;
    public static final int RESERVED_12 = 12;
    public static final int RESERVED_13 = 13;
    public static final int RESERVED_14 = 14;
    public static final int RESERVED_15 = 15;
    public static final int RESERVED_2 = 2;
    public static final int RESERVED_5 = 5;
    public static final int RESERVED_6 = 6;
    public static final int RESERVED_7 = 7;
    public static final int RESERVED_8 = 8;
    public static final int TELEPHONY = 1;
    public static final int TELEX = 4;
    public static final int UNKNOWN = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.CdmaSmsNumberPlan.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.CdmaSmsNumberPlan.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "TELEPHONY";
        }
        if (o == 2) {
            return "RESERVED_2";
        }
        if (o == 3) {
            return "DATA";
        }
        if (o == 4) {
            return "TELEX";
        }
        if (o == 5) {
            return "RESERVED_5";
        }
        if (o == 6) {
            return "RESERVED_6";
        }
        if (o == 7) {
            return "RESERVED_7";
        }
        if (o == 8) {
            return "RESERVED_8";
        }
        if (o == 9) {
            return "PRIVATE";
        }
        if (o == 10) {
            return "RESERVED_10";
        }
        if (o == 11) {
            return "RESERVED_11";
        }
        if (o == 12) {
            return "RESERVED_12";
        }
        if (o == 13) {
            return "RESERVED_13";
        }
        if (o == 14) {
            return "RESERVED_14";
        }
        if (o == 15) {
            return "RESERVED_15";
        }
        return "0x" + Integer.toHexString(o);
    }
}

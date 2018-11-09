package android.hardware.radio.V1_1;

public final class UtranBands {
    public static final int BAND_1 = 1;
    public static final int BAND_10 = 10;
    public static final int BAND_11 = 11;
    public static final int BAND_12 = 12;
    public static final int BAND_13 = 13;
    public static final int BAND_14 = 14;
    public static final int BAND_19 = 19;
    public static final int BAND_2 = 2;
    public static final int BAND_20 = 20;
    public static final int BAND_21 = 21;
    public static final int BAND_22 = 22;
    public static final int BAND_25 = 25;
    public static final int BAND_26 = 26;
    public static final int BAND_3 = 3;
    public static final int BAND_4 = 4;
    public static final int BAND_5 = 5;
    public static final int BAND_6 = 6;
    public static final int BAND_7 = 7;
    public static final int BAND_8 = 8;
    public static final int BAND_9 = 9;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_1.UtranBands.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_1.UtranBands.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "BAND_1";
        }
        if (o == 2) {
            return "BAND_2";
        }
        if (o == 3) {
            return "BAND_3";
        }
        if (o == 4) {
            return "BAND_4";
        }
        if (o == 5) {
            return "BAND_5";
        }
        if (o == 6) {
            return "BAND_6";
        }
        if (o == 7) {
            return "BAND_7";
        }
        if (o == 8) {
            return "BAND_8";
        }
        if (o == 9) {
            return "BAND_9";
        }
        if (o == 10) {
            return "BAND_10";
        }
        if (o == 11) {
            return "BAND_11";
        }
        if (o == 12) {
            return "BAND_12";
        }
        if (o == 13) {
            return "BAND_13";
        }
        if (o == 14) {
            return "BAND_14";
        }
        if (o == 19) {
            return "BAND_19";
        }
        if (o == 20) {
            return "BAND_20";
        }
        if (o == 21) {
            return "BAND_21";
        }
        if (o == 22) {
            return "BAND_22";
        }
        if (o == 25) {
            return "BAND_25";
        }
        if (o == 26) {
            return "BAND_26";
        }
        return "0x" + Integer.toHexString(o);
    }
}

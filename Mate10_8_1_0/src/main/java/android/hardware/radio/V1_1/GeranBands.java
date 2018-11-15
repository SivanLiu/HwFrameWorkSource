package android.hardware.radio.V1_1;

public final class GeranBands {
    public static final int BAND_450 = 3;
    public static final int BAND_480 = 4;
    public static final int BAND_710 = 5;
    public static final int BAND_750 = 6;
    public static final int BAND_850 = 8;
    public static final int BAND_DCS1800 = 12;
    public static final int BAND_E900 = 10;
    public static final int BAND_ER900 = 14;
    public static final int BAND_P900 = 9;
    public static final int BAND_PCS1900 = 13;
    public static final int BAND_R900 = 11;
    public static final int BAND_T380 = 1;
    public static final int BAND_T410 = 2;
    public static final int BAND_T810 = 7;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_1.GeranBands.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_1.GeranBands.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "BAND_T380";
        }
        if (o == 2) {
            return "BAND_T410";
        }
        if (o == 3) {
            return "BAND_450";
        }
        if (o == 4) {
            return "BAND_480";
        }
        if (o == 5) {
            return "BAND_710";
        }
        if (o == 6) {
            return "BAND_750";
        }
        if (o == 7) {
            return "BAND_T810";
        }
        if (o == 8) {
            return "BAND_850";
        }
        if (o == 9) {
            return "BAND_P900";
        }
        if (o == 10) {
            return "BAND_E900";
        }
        if (o == 11) {
            return "BAND_R900";
        }
        if (o == 12) {
            return "BAND_DCS1800";
        }
        if (o == 13) {
            return "BAND_PCS1900";
        }
        if (o == 14) {
            return "BAND_ER900";
        }
        return "0x" + Integer.toHexString(o);
    }
}

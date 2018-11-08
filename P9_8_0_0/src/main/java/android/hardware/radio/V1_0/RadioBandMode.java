package android.hardware.radio.V1_0;

public final class RadioBandMode {
    public static final int BAND_MODE_10_800M_2 = 15;
    public static final int BAND_MODE_5_450M = 10;
    public static final int BAND_MODE_7_700M_2 = 12;
    public static final int BAND_MODE_8_1800M = 13;
    public static final int BAND_MODE_9_900M = 14;
    public static final int BAND_MODE_AUS = 4;
    public static final int BAND_MODE_AUS_2 = 5;
    public static final int BAND_MODE_AWS = 17;
    public static final int BAND_MODE_CELL_800 = 6;
    public static final int BAND_MODE_EURO = 1;
    public static final int BAND_MODE_EURO_PAMR_400M = 16;
    public static final int BAND_MODE_IMT2000 = 11;
    public static final int BAND_MODE_JPN = 3;
    public static final int BAND_MODE_JTACS = 8;
    public static final int BAND_MODE_KOREA_PCS = 9;
    public static final int BAND_MODE_PCS = 7;
    public static final int BAND_MODE_UNSPECIFIED = 0;
    public static final int BAND_MODE_USA = 2;
    public static final int BAND_MODE_USA_2500M = 18;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RadioBandMode.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RadioBandMode.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "BAND_MODE_UNSPECIFIED";
        }
        if (o == 1) {
            return "BAND_MODE_EURO";
        }
        if (o == 2) {
            return "BAND_MODE_USA";
        }
        if (o == 3) {
            return "BAND_MODE_JPN";
        }
        if (o == 4) {
            return "BAND_MODE_AUS";
        }
        if (o == 5) {
            return "BAND_MODE_AUS_2";
        }
        if (o == 6) {
            return "BAND_MODE_CELL_800";
        }
        if (o == 7) {
            return "BAND_MODE_PCS";
        }
        if (o == 8) {
            return "BAND_MODE_JTACS";
        }
        if (o == 9) {
            return "BAND_MODE_KOREA_PCS";
        }
        if (o == 10) {
            return "BAND_MODE_5_450M";
        }
        if (o == 11) {
            return "BAND_MODE_IMT2000";
        }
        if (o == 12) {
            return "BAND_MODE_7_700M_2";
        }
        if (o == 13) {
            return "BAND_MODE_8_1800M";
        }
        if (o == 14) {
            return "BAND_MODE_9_900M";
        }
        if (o == 15) {
            return "BAND_MODE_10_800M_2";
        }
        if (o == 16) {
            return "BAND_MODE_EURO_PAMR_400M";
        }
        if (o == 17) {
            return "BAND_MODE_AWS";
        }
        if (o == 18) {
            return "BAND_MODE_USA_2500M";
        }
        return "0x" + Integer.toHexString(o);
    }
}

package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class FeatureID {
    public static final int FEATURE_ALL = 16;
    public static final int FEATURE_BLC = 13;
    public static final int FEATURE_CONTRAST = 2;
    public static final int FEATURE_DDIC_CABC = 12;
    public static final int FEATURE_DDIC_COLOR = 10;
    public static final int FEATURE_DDIC_GAMM = 9;
    public static final int FEATURE_DDIC_RGBW = 11;
    public static final int FEATURE_EYE_PROTECT = 15;
    public static final int FEATURE_GAMMA = 7;
    public static final int FEATURE_GMP = 3;
    public static final int FEATURE_HUE = 5;
    public static final int FEATURE_IGAMMA = 8;
    public static final int FEATURE_MAX = 17;
    public static final int FEATURE_PANEL_INFO = 14;
    public static final int FEATURE_SAT = 6;
    public static final int FEATURE_SHARP = 0;
    public static final int FEATURE_SHARP2P = 1;
    public static final int FEATURE_XCC = 4;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.FeatureID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.FeatureID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "FEATURE_SHARP";
        }
        if (o == 1) {
            return "FEATURE_SHARP2P";
        }
        if (o == 2) {
            return "FEATURE_CONTRAST";
        }
        if (o == 3) {
            return "FEATURE_GMP";
        }
        if (o == 4) {
            return "FEATURE_XCC";
        }
        if (o == 5) {
            return "FEATURE_HUE";
        }
        if (o == 6) {
            return "FEATURE_SAT";
        }
        if (o == 7) {
            return "FEATURE_GAMMA";
        }
        if (o == 8) {
            return "FEATURE_IGAMMA";
        }
        if (o == 9) {
            return "FEATURE_DDIC_GAMM";
        }
        if (o == 10) {
            return "FEATURE_DDIC_COLOR";
        }
        if (o == 11) {
            return "FEATURE_DDIC_RGBW";
        }
        if (o == 12) {
            return "FEATURE_DDIC_CABC";
        }
        if (o == 13) {
            return "FEATURE_BLC";
        }
        if (o == 14) {
            return "FEATURE_PANEL_INFO";
        }
        if (o == 15) {
            return "FEATURE_EYE_PROTECT";
        }
        if (o == 16) {
            return "FEATURE_ALL";
        }
        if (o == 17) {
            return "FEATURE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }
}

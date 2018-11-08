package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class TagID {
    public static final int TAG_AL = 10;
    public static final int TAG_ALL = 0;
    public static final int TAG_BACKLIGHT = 21;
    public static final int TAG_BL = 11;
    public static final int TAG_COUNT = 53;
    public static final int TAG_DELTA = 5;
    public static final int TAG_DO_BLC = 17;
    public static final int TAG_DO_HDR10 = 20;
    public static final int TAG_DO_LCE = 16;
    public static final int TAG_DO_LRE = 19;
    public static final int TAG_DO_SRE = 18;
    public static final int TAG_ENABLE = 2;
    public static final int TAG_HBM_PARAMETER = 52;
    public static final int TAG_HIACE_HUE = 29;
    public static final int TAG_HIACE_SATURATION = 30;
    public static final int TAG_HIACE_VALUE = 31;
    public static final int TAG_HIST = 6;
    public static final int TAG_HIST_SIZE = 7;
    public static final int TAG_LEVEL = 4;
    public static final int TAG_LHIST_SFT = 28;
    public static final int TAG_LUT = 8;
    public static final int TAG_LUT_SIZE = 9;
    public static final int TAG_METADATA = 13;
    public static final int TAG_METADATA_LEN = 14;
    public static final int TAG_MODE = 3;
    public static final int TAG_NR_LEVEL = 24;
    public static final int TAG_PANEL_NAME = 27;
    public static final int TAG_PARAM = 1;
    public static final int TAG_RESULT = 22;
    public static final int TAG_RGB_WEIGHT = 25;
    public static final int TAG_S3_BLUE_SIGMA03 = 41;
    public static final int TAG_S3_BLUE_SIGMA45 = 42;
    public static final int TAG_S3_BYPASS_NR = 32;
    public static final int TAG_S3_FILTER_LEVEL = 45;
    public static final int TAG_S3_GREEN_SIGMA03 = 37;
    public static final int TAG_S3_GREEN_SIGMA45 = 38;
    public static final int TAG_S3_HUE = 48;
    public static final int TAG_S3_MIN_MAX_SIGMA = 36;
    public static final int TAG_S3_RED_SIGMA03 = 39;
    public static final int TAG_S3_RED_SIGMA45 = 40;
    public static final int TAG_S3_SATURATION = 49;
    public static final int TAG_S3_SIMILARIT_COEFF = 46;
    public static final int TAG_S3_SKIN_GAIN = 51;
    public static final int TAG_S3_SOME_BRIGHTNESS01 = 33;
    public static final int TAG_S3_SOME_BRIGHTNESS23 = 34;
    public static final int TAG_S3_SOME_BRIGHTNESS4 = 35;
    public static final int TAG_S3_VALUE = 50;
    public static final int TAG_S3_V_FILTER_WEIGHT_ADJ = 47;
    public static final int TAG_S3_WHITE_SIGMA03 = 43;
    public static final int TAG_S3_WHITE_SIGMA45 = 44;
    public static final int TAG_SKIN_GAIN = 23;
    public static final int TAG_SRE_ON_THREHOLD = 26;
    public static final int TAG_THMINV = 12;
    public static final int TAG_TIME = 15;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.TagID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.TagID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "TAG_ALL";
        }
        if (o == 1) {
            return "TAG_PARAM";
        }
        if (o == 2) {
            return "TAG_ENABLE";
        }
        if (o == 3) {
            return "TAG_MODE";
        }
        if (o == 4) {
            return "TAG_LEVEL";
        }
        if (o == 5) {
            return "TAG_DELTA";
        }
        if (o == 6) {
            return "TAG_HIST";
        }
        if (o == 7) {
            return "TAG_HIST_SIZE";
        }
        if (o == 8) {
            return "TAG_LUT";
        }
        if (o == 9) {
            return "TAG_LUT_SIZE";
        }
        if (o == 10) {
            return "TAG_AL";
        }
        if (o == 11) {
            return "TAG_BL";
        }
        if (o == 12) {
            return "TAG_THMINV";
        }
        if (o == 13) {
            return "TAG_METADATA";
        }
        if (o == 14) {
            return "TAG_METADATA_LEN";
        }
        if (o == 15) {
            return "TAG_TIME";
        }
        if (o == 16) {
            return "TAG_DO_LCE";
        }
        if (o == 17) {
            return "TAG_DO_BLC";
        }
        if (o == 18) {
            return "TAG_DO_SRE";
        }
        if (o == 19) {
            return "TAG_DO_LRE";
        }
        if (o == 20) {
            return "TAG_DO_HDR10";
        }
        if (o == 21) {
            return "TAG_BACKLIGHT";
        }
        if (o == 22) {
            return "TAG_RESULT";
        }
        if (o == 23) {
            return "TAG_SKIN_GAIN";
        }
        if (o == 24) {
            return "TAG_NR_LEVEL";
        }
        if (o == 25) {
            return "TAG_RGB_WEIGHT";
        }
        if (o == 26) {
            return "TAG_SRE_ON_THREHOLD";
        }
        if (o == 27) {
            return "TAG_PANEL_NAME";
        }
        if (o == 28) {
            return "TAG_LHIST_SFT";
        }
        if (o == 29) {
            return "TAG_HIACE_HUE";
        }
        if (o == 30) {
            return "TAG_HIACE_SATURATION";
        }
        if (o == 31) {
            return "TAG_HIACE_VALUE";
        }
        if (o == 32) {
            return "TAG_S3_BYPASS_NR";
        }
        if (o == 33) {
            return "TAG_S3_SOME_BRIGHTNESS01";
        }
        if (o == 34) {
            return "TAG_S3_SOME_BRIGHTNESS23";
        }
        if (o == 35) {
            return "TAG_S3_SOME_BRIGHTNESS4";
        }
        if (o == 36) {
            return "TAG_S3_MIN_MAX_SIGMA";
        }
        if (o == 37) {
            return "TAG_S3_GREEN_SIGMA03";
        }
        if (o == 38) {
            return "TAG_S3_GREEN_SIGMA45";
        }
        if (o == 39) {
            return "TAG_S3_RED_SIGMA03";
        }
        if (o == 40) {
            return "TAG_S3_RED_SIGMA45";
        }
        if (o == 41) {
            return "TAG_S3_BLUE_SIGMA03";
        }
        if (o == 42) {
            return "TAG_S3_BLUE_SIGMA45";
        }
        if (o == 43) {
            return "TAG_S3_WHITE_SIGMA03";
        }
        if (o == 44) {
            return "TAG_S3_WHITE_SIGMA45";
        }
        if (o == 45) {
            return "TAG_S3_FILTER_LEVEL";
        }
        if (o == 46) {
            return "TAG_S3_SIMILARIT_COEFF";
        }
        if (o == 47) {
            return "TAG_S3_V_FILTER_WEIGHT_ADJ";
        }
        if (o == 48) {
            return "TAG_S3_HUE";
        }
        if (o == 49) {
            return "TAG_S3_SATURATION";
        }
        if (o == 50) {
            return "TAG_S3_VALUE";
        }
        if (o == 51) {
            return "TAG_S3_SKIN_GAIN";
        }
        if (o == 52) {
            return "TAG_HBM_PARAMETER";
        }
        if (o == 53) {
            return "TAG_COUNT";
        }
        return "0x" + Integer.toHexString(o);
    }
}

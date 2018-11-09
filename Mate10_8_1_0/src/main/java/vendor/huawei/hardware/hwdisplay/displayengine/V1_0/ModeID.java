package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class ModeID {
    public static final int MODE_3D_COLORTMP = 17;
    public static final int MODE_AMBIENT_LUX_CHANGE = 28;
    public static final int MODE_AMBIENT_LUX_OFFSET = 29;
    public static final int MODE_BROWSER = 20;
    public static final int MODE_CAMERA = 10;
    public static final int MODE_CAMERA_PRO = 11;
    public static final int MODE_DEFAULT = 1;
    public static final int MODE_DISABLE = 0;
    public static final int MODE_EBOOK = 13;
    public static final int MODE_GAME = 16;
    public static final int MODE_HDR10 = 6;
    public static final int MODE_HDR10_DISABLE = 7;
    public static final int MODE_IM = 18;
    public static final int MODE_IMAGE = 8;
    public static final int MODE_IMAGE_DISABLE = 9;
    public static final int MODE_IM_DISABLE = 19;
    public static final int MODE_INVALID = -1;
    public static final int MODE_MARKET = 27;
    public static final int MODE_MAX = 35;
    public static final int MODE_MONEY = 25;
    public static final int MODE_NAVIGATION = 24;
    public static final int MODE_NEWS_CLIENT = 21;
    public static final int MODE_RGLED = 32;
    public static final int MODE_SCALING_DOWN = 15;
    public static final int MODE_SCALING_UP = 14;
    public static final int MODE_SCREEN_OFF = 34;
    public static final int MODE_SCREEN_ON = 33;
    public static final int MODE_SHOP = 22;
    public static final int MODE_SMS = 23;
    public static final int MODE_SPORTS = 26;
    public static final int MODE_UI = 3;
    public static final int MODE_USER = 2;
    public static final int MODE_VIDEO = 4;
    public static final int MODE_VIDEO_DISABLE = 5;
    public static final int MODE_WEB = 12;
    public static final int MODE_XCC_CHANGE = 30;
    public static final int MODE_XNIT_CHANGE_INTEGRATION = 31;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ModeID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ModeID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == -1) {
            return "MODE_INVALID";
        }
        if (o == 0) {
            return "MODE_DISABLE";
        }
        if (o == 1) {
            return "MODE_DEFAULT";
        }
        if (o == 2) {
            return "MODE_USER";
        }
        if (o == 3) {
            return "MODE_UI";
        }
        if (o == 4) {
            return "MODE_VIDEO";
        }
        if (o == 5) {
            return "MODE_VIDEO_DISABLE";
        }
        if (o == 6) {
            return "MODE_HDR10";
        }
        if (o == 7) {
            return "MODE_HDR10_DISABLE";
        }
        if (o == 8) {
            return "MODE_IMAGE";
        }
        if (o == 9) {
            return "MODE_IMAGE_DISABLE";
        }
        if (o == 10) {
            return "MODE_CAMERA";
        }
        if (o == 11) {
            return "MODE_CAMERA_PRO";
        }
        if (o == 12) {
            return "MODE_WEB";
        }
        if (o == 13) {
            return "MODE_EBOOK";
        }
        if (o == 14) {
            return "MODE_SCALING_UP";
        }
        if (o == 15) {
            return "MODE_SCALING_DOWN";
        }
        if (o == 16) {
            return "MODE_GAME";
        }
        if (o == 17) {
            return "MODE_3D_COLORTMP";
        }
        if (o == 18) {
            return "MODE_IM";
        }
        if (o == 19) {
            return "MODE_IM_DISABLE";
        }
        if (o == 20) {
            return "MODE_BROWSER";
        }
        if (o == 21) {
            return "MODE_NEWS_CLIENT";
        }
        if (o == 22) {
            return "MODE_SHOP";
        }
        if (o == 23) {
            return "MODE_SMS";
        }
        if (o == 24) {
            return "MODE_NAVIGATION";
        }
        if (o == 25) {
            return "MODE_MONEY";
        }
        if (o == 26) {
            return "MODE_SPORTS";
        }
        if (o == 27) {
            return "MODE_MARKET";
        }
        if (o == 28) {
            return "MODE_AMBIENT_LUX_CHANGE";
        }
        if (o == 29) {
            return "MODE_AMBIENT_LUX_OFFSET";
        }
        if (o == 30) {
            return "MODE_XCC_CHANGE";
        }
        if (o == 31) {
            return "MODE_XNIT_CHANGE_INTEGRATION";
        }
        if (o == 32) {
            return "MODE_RGLED";
        }
        if (o == 33) {
            return "MODE_SCREEN_ON";
        }
        if (o == 34) {
            return "MODE_SCREEN_OFF";
        }
        if (o == 35) {
            return "MODE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }
}

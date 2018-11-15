package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class ModeExID {
    public static final int MODE_AL = 35;
    public static final int MODE_DATA = 36;
    public static final int MODE_DATA_HDR10 = 37;
    public static final int MODE_DATA_XCC = 38;
    public static final int MODE_DIMMING = 40;
    public static final int MODE_EX_MAX = 42;
    public static final int MODE_HBM = 41;
    public static final int MODE_XNIT_CHANGE = 39;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ModeExID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ModeExID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 35) {
            return "MODE_AL";
        }
        if (o == 36) {
            return "MODE_DATA";
        }
        if (o == 37) {
            return "MODE_DATA_HDR10";
        }
        if (o == 38) {
            return "MODE_DATA_XCC";
        }
        if (o == 39) {
            return "MODE_XNIT_CHANGE";
        }
        if (o == 40) {
            return "MODE_DIMMING";
        }
        if (o == 41) {
            return "MODE_HBM";
        }
        if (o == 42) {
            return "MODE_EX_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }
}

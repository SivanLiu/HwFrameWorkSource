package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class SegmentationMode {
    public static final int HIGH_BIT_AL_MODE = 1;
    public static final int HIGH_BIT_COMP_MODE = 4;
    public static final int HIGH_BIT_DETAIL_MODE = 3;
    public static final int HIGH_BIT_WCG_MODE = 2;
    public static final int LOW_BIT_MODE = 0;
    public static final int SEG_MODE_MAX = 5;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.SegmentationMode.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.SegmentationMode.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "LOW_BIT_MODE";
        }
        if (o == 1) {
            return "HIGH_BIT_AL_MODE";
        }
        if (o == 2) {
            return "HIGH_BIT_WCG_MODE";
        }
        if (o == 3) {
            return "HIGH_BIT_DETAIL_MODE";
        }
        if (o == 4) {
            return "HIGH_BIT_COMP_MODE";
        }
        if (o == 5) {
            return "SEG_MODE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }
}

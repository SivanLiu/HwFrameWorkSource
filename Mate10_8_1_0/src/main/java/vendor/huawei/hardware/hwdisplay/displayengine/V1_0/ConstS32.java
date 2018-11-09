package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class ConstS32 {
    public static final int HIGH_BITS_AL_MARK = 3840;
    public static final int HIGH_BITS_COMP_MARK = 2146435072;
    public static final int HIGH_BITS_DETAIL_MARK = 983040;
    public static final int HIGH_BITS_WCG_MARK = 61440;
    public static final int LOW_BITS_MARK = 255;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ConstS32.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.ConstS32.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 255) {
            return "LOW_BITS_MARK";
        }
        if (o == HIGH_BITS_AL_MARK) {
            return "HIGH_BITS_AL_MARK";
        }
        if (o == HIGH_BITS_WCG_MARK) {
            return "HIGH_BITS_WCG_MARK";
        }
        if (o == 983040) {
            return "HIGH_BITS_DETAIL_MARK";
        }
        if (o == HIGH_BITS_COMP_MARK) {
            return "HIGH_BITS_COMP_MARK";
        }
        return "0x" + Integer.toHexString(o);
    }
}

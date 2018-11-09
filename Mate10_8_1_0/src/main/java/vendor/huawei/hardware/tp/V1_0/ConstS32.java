package vendor.huawei.hardware.tp.V1_0;

public final class ConstS32 {
    public static final int TP_CAP_TEST_RESULT_LEN = 100;
    public static final int TS_CHIP_INFO_LEN = 128;
    public static final int TS_GESTURE_DATA_LEN = 12;
    public static final int TS_ROI_DATA_LEN = 47;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.tp.V1_0.ConstS32.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.tp.V1_0.ConstS32.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 47) {
            return "TS_ROI_DATA_LEN";
        }
        if (o == 128) {
            return "TS_CHIP_INFO_LEN";
        }
        if (o == 12) {
            return "TS_GESTURE_DATA_LEN";
        }
        if (o == 100) {
            return "TP_CAP_TEST_RESULT_LEN";
        }
        return "0x" + Integer.toHexString(o);
    }
}

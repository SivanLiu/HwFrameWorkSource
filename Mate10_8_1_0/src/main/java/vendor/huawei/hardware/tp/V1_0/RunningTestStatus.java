package vendor.huawei.hardware.tp.V1_0;

public final class RunningTestStatus {
    public static final int RUNNING_TEST_LAST_TIME = 2;
    public static final int RUNNING_TEST_LCD_TEST = 4;
    public static final int RUNNING_TEST_OTHER_TEST = 1;
    public static final int RUNNING_TEST_UNKNOWN = 255;
    public static final int RUNNING_TEST_VIDEO_TEST = 8;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.tp.V1_0.RunningTestStatus.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.tp.V1_0.RunningTestStatus.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "RUNNING_TEST_OTHER_TEST";
        }
        if (o == 2) {
            return "RUNNING_TEST_LAST_TIME";
        }
        if (o == 4) {
            return "RUNNING_TEST_LCD_TEST";
        }
        if (o == 8) {
            return "RUNNING_TEST_VIDEO_TEST";
        }
        if (o == 255) {
            return "RUNNING_TEST_UNKNOWN";
        }
        return "0x" + Integer.toHexString(o);
    }
}

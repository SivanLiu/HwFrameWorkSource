package vendor.huawei.hardware.tp.V1_0;

public final class TpSnrTestResult {
    public static final int TP_SNR_TEST_FAIL_PANEL = 2;
    public static final int TP_SNR_TEST_FAIL_SOFTWARE = 1;
    public static final int TP_SNR_TEST_FAIL_UNKNOWN = 3;
    public static final int TP_SNR_TEST_PASS = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.tp.V1_0.TpSnrTestResult.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.tp.V1_0.TpSnrTestResult.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "TP_SNR_TEST_PASS";
        }
        if (o == 1) {
            return "TP_SNR_TEST_FAIL_SOFTWARE";
        }
        if (o == 2) {
            return "TP_SNR_TEST_FAIL_PANEL";
        }
        if (o == 3) {
            return "TP_SNR_TEST_FAIL_UNKNOWN";
        }
        return "0x" + Integer.toHexString(o);
    }
}

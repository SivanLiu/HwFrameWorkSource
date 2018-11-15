package vendor.huawei.hardware.dolby.dms.V1_0;

public final class DolbyClientType {
    public static final int DOLBY_CLIENT_AC4DEC = 5;
    public static final int DOLBY_CLIENT_DAP_EFF = 1;
    public static final int DOLBY_CLIENT_DP_EFF = 2;
    public static final int DOLBY_CLIENT_INVALID = -1;
    public static final int DOLBY_CLIENT_LAST = 6;
    public static final int DOLBY_CLIENT_NATIVE_SERVICE = 3;
    public static final int DOLBY_CLIENT_UDC = 0;
    public static final int DOLBY_CLIENT_UDC_JOC = 4;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.dolby.dms.V1_0.DolbyClientType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.dolby.dms.V1_0.DolbyClientType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == -1) {
            return "DOLBY_CLIENT_INVALID";
        }
        if (o == 0) {
            return "DOLBY_CLIENT_UDC";
        }
        if (o == 1) {
            return "DOLBY_CLIENT_DAP_EFF";
        }
        if (o == 2) {
            return "DOLBY_CLIENT_DP_EFF";
        }
        if (o == 3) {
            return "DOLBY_CLIENT_NATIVE_SERVICE";
        }
        if (o == 4) {
            return "DOLBY_CLIENT_UDC_JOC";
        }
        if (o == 5) {
            return "DOLBY_CLIENT_AC4DEC";
        }
        if (o == 6) {
            return "DOLBY_CLIENT_LAST";
        }
        return "0x" + Integer.toHexString(o);
    }
}

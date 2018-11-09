package vendor.huawei.hardware.radio.V1_0;

public final class RILUICCAUTHFILEOPTCMDENUM {
    public static final int RIL_UICC_AUTH_FILE_OPT_READ = 0;
    public static final int RIL_UICC_AUTH_FILE_OPT_WRITE = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.RILUICCAUTHFILEOPTCMDENUM.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.RILUICCAUTHFILEOPTCMDENUM.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_UICC_AUTH_FILE_OPT_READ";
        }
        if (o == 1) {
            return "RIL_UICC_AUTH_FILE_OPT_WRITE";
        }
        return "0x" + Integer.toHexString(o);
    }
}

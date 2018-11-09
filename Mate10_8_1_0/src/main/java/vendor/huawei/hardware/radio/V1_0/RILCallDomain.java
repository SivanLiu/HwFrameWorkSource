package vendor.huawei.hardware.radio.V1_0;

public final class RILCallDomain {
    public static final int RIL_CALL_DOMAIN_AUTOMATIC = 3;
    public static final int RIL_CALL_DOMAIN_CS = 1;
    public static final int RIL_CALL_DOMAIN_PS = 2;
    public static final int RIL_CALL_DOMAIN_UNKNOWN = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.RILCallDomain.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.RILCallDomain.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_CALL_DOMAIN_UNKNOWN";
        }
        if (o == 1) {
            return "RIL_CALL_DOMAIN_CS";
        }
        if (o == 2) {
            return "RIL_CALL_DOMAIN_PS";
        }
        if (o == 3) {
            return "RIL_CALL_DOMAIN_AUTOMATIC";
        }
        return "0x" + Integer.toHexString(o);
    }
}

package vendor.huawei.hardware.eid.V1_0;

public final class ENCRY_METHOD_E {
    public static final int ENCRY_GET_SEC_IMG = 5;
    public static final int ENCRY_GET_SM4_DATA = 4;
    public static final int ENCRY_SET_SECMODE = 3;
    public static final int ENCRY_SM4_LIVE = 1;
    public static final int ENCRY_UNION_LIVE = 2;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.eid.V1_0.ENCRY_METHOD_E.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.eid.V1_0.ENCRY_METHOD_E.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "ENCRY_SM4_LIVE";
        }
        if (o == 2) {
            return "ENCRY_UNION_LIVE";
        }
        if (o == 3) {
            return "ENCRY_SET_SECMODE";
        }
        if (o == 4) {
            return "ENCRY_GET_SM4_DATA";
        }
        if (o == 5) {
            return "ENCRY_GET_SEC_IMG";
        }
        return "0x" + Integer.toHexString(o);
    }
}

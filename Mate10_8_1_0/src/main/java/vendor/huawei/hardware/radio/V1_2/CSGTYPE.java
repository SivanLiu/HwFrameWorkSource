package vendor.huawei.hardware.radio.V1_2;

public final class CSGTYPE {
    public static final int CSG_ALLOW_LIST = 1;
    public static final int CSG_OPERATOR_LIST_ALLOW = 2;
    public static final int CSG_OPERATOR_LIST_FORBIDEN = 3;
    public static final int CSG_UNKNOW_LIST = 4;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_2.CSGTYPE.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_2.CSGTYPE.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "CSG_ALLOW_LIST";
        }
        if (o == 2) {
            return "CSG_OPERATOR_LIST_ALLOW";
        }
        if (o == 3) {
            return "CSG_OPERATOR_LIST_FORBIDEN";
        }
        if (o == 4) {
            return "CSG_UNKNOW_LIST";
        }
        return "0x" + Integer.toHexString(o);
    }
}

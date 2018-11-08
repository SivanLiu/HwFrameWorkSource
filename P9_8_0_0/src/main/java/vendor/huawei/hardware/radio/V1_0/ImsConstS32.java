package vendor.huawei.hardware.radio.V1_0;

public final class ImsConstS32 {
    public static final int IPV4_ADDR_LEN = 4;
    public static final int IPV6_ADDR_LEN = 16;
    public static final int MAX_ECONF_CALLED_NUM = 5;
    public static final int MAX_IMS_CALL_TYPE = 4;
    public static final int MAX_IMS_TECH_TYPE = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.ImsConstS32.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.ImsConstS32.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 4) {
            return "IPV4_ADDR_LEN";
        }
        if (o == 16) {
            return "IPV6_ADDR_LEN";
        }
        if (o == 4) {
            return "MAX_IMS_CALL_TYPE";
        }
        if (o == 1) {
            return "MAX_IMS_TECH_TYPE";
        }
        if (o == 5) {
            return "MAX_ECONF_CALLED_NUM";
        }
        return "0x" + Integer.toHexString(o);
    }
}

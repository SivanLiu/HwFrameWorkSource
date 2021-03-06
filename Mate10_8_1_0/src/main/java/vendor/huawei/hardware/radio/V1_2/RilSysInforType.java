package vendor.huawei.hardware.radio.V1_2;

public final class RilSysInforType {
    public static final int ATTACH_EPS_INFO = 4;
    public static final int DETACH_REATTACH_INFO = 1;
    public static final int SIB1_ECC_INFO = 2;
    public static final int SIB2_AC_ECC_INFO = 3;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_2.RilSysInforType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_2.RilSysInforType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "DETACH_REATTACH_INFO";
        }
        if (o == 2) {
            return "SIB1_ECC_INFO";
        }
        if (o == 3) {
            return "SIB2_AC_ECC_INFO";
        }
        if (o == 4) {
            return "ATTACH_EPS_INFO";
        }
        return "0x" + Integer.toHexString(o);
    }
}

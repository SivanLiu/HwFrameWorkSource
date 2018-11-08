package vendor.huawei.hardware.radio.V1_0;

public final class RILImsCallType {
    public static final int CALL_TYPE_CS_VS_RX = 6;
    public static final int CALL_TYPE_CS_VS_TX = 5;
    public static final int CALL_TYPE_PS_VS_RX = 8;
    public static final int CALL_TYPE_PS_VS_TX = 7;
    public static final int CALL_TYPE_SMS = 10;
    public static final int CALL_TYPE_UNKNOWN = 9;
    public static final int CALL_TYPE_UT = 11;
    public static final int CALL_TYPE_VOICE = 0;
    public static final int CALL_TYPE_VT = 3;
    public static final int CALL_TYPE_VT_NODIR = 4;
    public static final int CALL_TYPE_VT_RX = 2;
    public static final int CALL_TYPE_VT_TX = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.RILImsCallType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.RILImsCallType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "CALL_TYPE_VOICE";
        }
        if (o == 1) {
            return "CALL_TYPE_VT_TX";
        }
        if (o == 2) {
            return "CALL_TYPE_VT_RX";
        }
        if (o == 3) {
            return "CALL_TYPE_VT";
        }
        if (o == 4) {
            return "CALL_TYPE_VT_NODIR";
        }
        if (o == 5) {
            return "CALL_TYPE_CS_VS_TX";
        }
        if (o == 6) {
            return "CALL_TYPE_CS_VS_RX";
        }
        if (o == 7) {
            return "CALL_TYPE_PS_VS_TX";
        }
        if (o == 8) {
            return "CALL_TYPE_PS_VS_RX";
        }
        if (o == 9) {
            return "CALL_TYPE_UNKNOWN";
        }
        if (o == 10) {
            return "CALL_TYPE_SMS";
        }
        if (o == 11) {
            return "CALL_TYPE_UT";
        }
        return "0x" + Integer.toHexString(o);
    }
}

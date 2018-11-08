package vendor.huawei.hardware.eid.V1_0;

public final class BUFF_LEN_E {
    public static final int CERTIFICATE_MAX_LEN = 8192;
    public static final int CERT_REQ_MSG_MAX_LEN = 2048;
    public static final int DE_SKEY_MAX_LEN = 2048;
    public static final int ID_INFO_MAX_LEN = 5120;
    public static final int IMAGE_NV21_SIZE = 460800;
    public static final int INFO_MAX_LEN = 2048;
    public static final int INFO_SIGN_MAX_LEN = 4096;
    public static final int INPUT_MAX_TRANSPOT_LEN = 153600;
    public static final int INPUT_TRANSPOT_TIMES = 3;
    public static final int MAX_AID_LEN = 256;
    public static final int MAX_LOGO_SIZE = 24576;
    public static final int OUTPUT_MAX_TRANSPOT_LEN = 163840;
    public static final int OUTPUT_TRANSPOT_TIMES = 3;
    public static final int SEC_IMAGE_MAX_LEN = 491520;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.eid.V1_0.BUFF_LEN_E.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.eid.V1_0.BUFF_LEN_E.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 8192) {
            return "CERTIFICATE_MAX_LEN";
        }
        if (o == SEC_IMAGE_MAX_LEN) {
            return "SEC_IMAGE_MAX_LEN";
        }
        if (o == 2048) {
            return "DE_SKEY_MAX_LEN";
        }
        if (o == 2048) {
            return "CERT_REQ_MSG_MAX_LEN";
        }
        if (o == 2048) {
            return "INFO_MAX_LEN";
        }
        if (o == 4096) {
            return "INFO_SIGN_MAX_LEN";
        }
        if (o == IMAGE_NV21_SIZE) {
            return "IMAGE_NV21_SIZE";
        }
        if (o == ID_INFO_MAX_LEN) {
            return "ID_INFO_MAX_LEN";
        }
        if (o == OUTPUT_MAX_TRANSPOT_LEN) {
            return "OUTPUT_MAX_TRANSPOT_LEN";
        }
        if (o == INPUT_MAX_TRANSPOT_LEN) {
            return "INPUT_MAX_TRANSPOT_LEN";
        }
        if (o == 3) {
            return "OUTPUT_TRANSPOT_TIMES";
        }
        if (o == 3) {
            return "INPUT_TRANSPOT_TIMES";
        }
        if (o == 256) {
            return "MAX_AID_LEN";
        }
        if (o == MAX_LOGO_SIZE) {
            return "MAX_LOGO_SIZE";
        }
        return "0x" + Integer.toHexString(o);
    }
}

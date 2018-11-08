package vendor.huawei.hardware.eid.V1_0;

public final class ERR_CODE_E {
    public static final int ADDRESS_INFO_ERR = 5;
    public static final int CAMERA_REGISTER_FAIL = 34;
    public static final int DLSYM_ERROR = 28;
    public static final int FACEID_CHANGED = 14;
    public static final int FACEID_DEL_FAIL = 18;
    public static final int FACEID_NOCHANGE = 15;
    public static final int FACEID_NOTUSED = 16;
    public static final int FACEID_SET_FAIL = 17;
    public static final int GET_APPLET_INFO_FAIL = 25;
    public static final int GET_IMAGE_FROM_SECIMAGE_FAIL = 30;
    public static final int GET_PUBLICKEY_FAIL = 22;
    public static final int GET_SEC_IMAGE_FAIL = 23;
    public static final int LIVE_DECT_FAIL = 29;
    public static final int LOAD_LIVELIB_FAIL = 21;
    public static final int MAP_STATUS_ERR = 6;
    public static final int MAP_UNKNOW_ERR = 7;
    public static final int MEM_ALLOC_FAIL = 33;
    public static final int NEED_FACE_AUTH = 31;
    public static final int NO_REPLY_MSG_ERR = 8;
    public static final int NO_TEE_MEMORY = 19;
    public static final int OK = 0;
    public static final int PARA_ERR = 1;
    public static final int REPLY_MSG_ERR = 2;
    public static final int REPLY_MSG_PARA_ERR = 9;
    public static final int SEND_MSG_ERR = 3;
    public static final int SET_SEC_MODE_FAIL = 32;
    public static final int TA_FREE_FAIL = 20;
    public static final int TUI_PIN_LEN_ERR = 13;
    public static final int TUI_PIN_OVER_TIMES = 12;
    public static final int TUI_PIN_SYS_ERR = 10;
    public static final int TUI_PIN_USER_CANCEL = 11;
    public static final int UNKNOWN_CMD = 4;
    public static final int USER_NO_AGREE_TUI_INFO = 27;
    public static final int USE_TEE_SM_FAIL = 24;
    public static final int USE_TUI_FAIL = 26;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.eid.V1_0.ERR_CODE_E.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.eid.V1_0.ERR_CODE_E.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "OK";
        }
        if (o == 1) {
            return "PARA_ERR";
        }
        if (o == 2) {
            return "REPLY_MSG_ERR";
        }
        if (o == 3) {
            return "SEND_MSG_ERR";
        }
        if (o == 4) {
            return "UNKNOWN_CMD";
        }
        if (o == 5) {
            return "ADDRESS_INFO_ERR";
        }
        if (o == 6) {
            return "MAP_STATUS_ERR";
        }
        if (o == 7) {
            return "MAP_UNKNOW_ERR";
        }
        if (o == 8) {
            return "NO_REPLY_MSG_ERR";
        }
        if (o == 9) {
            return "REPLY_MSG_PARA_ERR";
        }
        if (o == 10) {
            return "TUI_PIN_SYS_ERR";
        }
        if (o == 11) {
            return "TUI_PIN_USER_CANCEL";
        }
        if (o == 12) {
            return "TUI_PIN_OVER_TIMES";
        }
        if (o == 13) {
            return "TUI_PIN_LEN_ERR";
        }
        if (o == 14) {
            return "FACEID_CHANGED";
        }
        if (o == 15) {
            return "FACEID_NOCHANGE";
        }
        if (o == 16) {
            return "FACEID_NOTUSED";
        }
        if (o == 17) {
            return "FACEID_SET_FAIL";
        }
        if (o == 18) {
            return "FACEID_DEL_FAIL";
        }
        if (o == 19) {
            return "NO_TEE_MEMORY";
        }
        if (o == 20) {
            return "TA_FREE_FAIL";
        }
        if (o == 21) {
            return "LOAD_LIVELIB_FAIL";
        }
        if (o == 22) {
            return "GET_PUBLICKEY_FAIL";
        }
        if (o == 23) {
            return "GET_SEC_IMAGE_FAIL";
        }
        if (o == 24) {
            return "USE_TEE_SM_FAIL";
        }
        if (o == 25) {
            return "GET_APPLET_INFO_FAIL";
        }
        if (o == 26) {
            return "USE_TUI_FAIL";
        }
        if (o == 27) {
            return "USER_NO_AGREE_TUI_INFO";
        }
        if (o == 28) {
            return "DLSYM_ERROR";
        }
        if (o == 29) {
            return "LIVE_DECT_FAIL";
        }
        if (o == 30) {
            return "GET_IMAGE_FROM_SECIMAGE_FAIL";
        }
        if (o == 31) {
            return "NEED_FACE_AUTH";
        }
        if (o == 32) {
            return "SET_SEC_MODE_FAIL";
        }
        if (o == 33) {
            return "MEM_ALLOC_FAIL";
        }
        if (o == 34) {
            return "CAMERA_REGISTER_FAIL";
        }
        return "0x" + Integer.toHexString(o);
    }
}

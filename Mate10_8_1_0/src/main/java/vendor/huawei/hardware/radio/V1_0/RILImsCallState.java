package vendor.huawei.hardware.radio.V1_0;

public final class RILImsCallState {
    public static final int RIL_IMS_CALL_ACTIVE = 0;
    public static final int RIL_IMS_CALL_ALERTING = 3;
    public static final int RIL_IMS_CALL_DIALING = 2;
    public static final int RIL_IMS_CALL_HOLDING = 1;
    public static final int RIL_IMS_CALL_INCOMING = 4;
    public static final int RIL_IMS_CALL_WAITING = 5;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.radio.V1_0.RILImsCallState.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.radio.V1_0.RILImsCallState.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "RIL_IMS_CALL_ACTIVE";
        }
        if (o == 1) {
            return "RIL_IMS_CALL_HOLDING";
        }
        if (o == 2) {
            return "RIL_IMS_CALL_DIALING";
        }
        if (o == 3) {
            return "RIL_IMS_CALL_ALERTING";
        }
        if (o == 4) {
            return "RIL_IMS_CALL_INCOMING";
        }
        if (o == 5) {
            return "RIL_IMS_CALL_WAITING";
        }
        return "0x" + Integer.toHexString(o);
    }
}

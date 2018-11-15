package android.hardware.radio.V1_0;

public final class RegState {
    public static final int NOT_REG_MT_NOT_SEARCHING_OP = 0;
    public static final int NOT_REG_MT_NOT_SEARCHING_OP_EM = 10;
    public static final int NOT_REG_MT_SEARCHING_OP = 2;
    public static final int NOT_REG_MT_SEARCHING_OP_EM = 12;
    public static final int REG_DENIED = 3;
    public static final int REG_DENIED_EM = 13;
    public static final int REG_HOME = 1;
    public static final int REG_ROAMING = 5;
    public static final int UNKNOWN = 4;
    public static final int UNKNOWN_EM = 14;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RegState.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RegState.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "NOT_REG_MT_NOT_SEARCHING_OP";
        }
        if (o == 1) {
            return "REG_HOME";
        }
        if (o == 2) {
            return "NOT_REG_MT_SEARCHING_OP";
        }
        if (o == 3) {
            return "REG_DENIED";
        }
        if (o == 4) {
            return "UNKNOWN";
        }
        if (o == 5) {
            return "REG_ROAMING";
        }
        if (o == 10) {
            return "NOT_REG_MT_NOT_SEARCHING_OP_EM";
        }
        if (o == 12) {
            return "NOT_REG_MT_SEARCHING_OP_EM";
        }
        if (o == 13) {
            return "REG_DENIED_EM";
        }
        if (o == 14) {
            return "UNKNOWN_EM";
        }
        return "0x" + Integer.toHexString(o);
    }
}

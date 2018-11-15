package android.hardware.radio.V1_0;

public final class CdmaOtaProvisionStatus {
    public static final int A_KEY_EXCHANGED = 2;
    public static final int COMMITTED = 8;
    public static final int IMSI_DOWNLOADED = 6;
    public static final int MDN_DOWNLOADED = 5;
    public static final int NAM_DOWNLOADED = 4;
    public static final int OTAPA_ABORTED = 11;
    public static final int OTAPA_STARTED = 9;
    public static final int OTAPA_STOPPED = 10;
    public static final int PRL_DOWNLOADED = 7;
    public static final int SPC_RETRIES_EXCEEDED = 1;
    public static final int SPL_UNLOCKED = 0;
    public static final int SSD_UPDATED = 3;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.CdmaOtaProvisionStatus.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.CdmaOtaProvisionStatus.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "SPL_UNLOCKED";
        }
        if (o == 1) {
            return "SPC_RETRIES_EXCEEDED";
        }
        if (o == 2) {
            return "A_KEY_EXCHANGED";
        }
        if (o == 3) {
            return "SSD_UPDATED";
        }
        if (o == 4) {
            return "NAM_DOWNLOADED";
        }
        if (o == 5) {
            return "MDN_DOWNLOADED";
        }
        if (o == 6) {
            return "IMSI_DOWNLOADED";
        }
        if (o == 7) {
            return "PRL_DOWNLOADED";
        }
        if (o == 8) {
            return "COMMITTED";
        }
        if (o == 9) {
            return "OTAPA_STARTED";
        }
        if (o == 10) {
            return "OTAPA_STOPPED";
        }
        if (o == 11) {
            return "OTAPA_ABORTED";
        }
        return "0x" + Integer.toHexString(o);
    }
}

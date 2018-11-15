package android.hardware.radio.V1_0;

public final class SsServiceType {
    public static final int ALL_BARRING = 16;
    public static final int BAIC = 14;
    public static final int BAIC_ROAMING = 15;
    public static final int BAOC = 11;
    public static final int BAOIC = 12;
    public static final int BAOIC_EXC_HOME = 13;
    public static final int CFU = 0;
    public static final int CF_ALL = 4;
    public static final int CF_ALL_CONDITIONAL = 5;
    public static final int CF_BUSY = 1;
    public static final int CF_NOT_REACHABLE = 3;
    public static final int CF_NO_REPLY = 2;
    public static final int CLIP = 6;
    public static final int CLIR = 7;
    public static final int COLP = 8;
    public static final int COLR = 9;
    public static final int INCOMING_BARRING = 18;
    public static final int OUTGOING_BARRING = 17;
    public static final int WAIT = 10;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.SsServiceType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.SsServiceType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "CFU";
        }
        if (o == 1) {
            return "CF_BUSY";
        }
        if (o == 2) {
            return "CF_NO_REPLY";
        }
        if (o == 3) {
            return "CF_NOT_REACHABLE";
        }
        if (o == 4) {
            return "CF_ALL";
        }
        if (o == 5) {
            return "CF_ALL_CONDITIONAL";
        }
        if (o == 6) {
            return "CLIP";
        }
        if (o == 7) {
            return "CLIR";
        }
        if (o == 8) {
            return "COLP";
        }
        if (o == 9) {
            return "COLR";
        }
        if (o == 10) {
            return "WAIT";
        }
        if (o == 11) {
            return "BAOC";
        }
        if (o == 12) {
            return "BAOIC";
        }
        if (o == 13) {
            return "BAOIC_EXC_HOME";
        }
        if (o == 14) {
            return "BAIC";
        }
        if (o == 15) {
            return "BAIC_ROAMING";
        }
        if (o == 16) {
            return "ALL_BARRING";
        }
        if (o == 17) {
            return "OUTGOING_BARRING";
        }
        if (o == 18) {
            return "INCOMING_BARRING";
        }
        return "0x" + Integer.toHexString(o);
    }
}

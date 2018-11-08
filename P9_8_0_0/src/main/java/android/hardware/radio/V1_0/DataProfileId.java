package android.hardware.radio.V1_0;

public final class DataProfileId {
    public static final int CBS = 4;
    public static final int DEFAULT = 0;
    public static final int FOTA = 3;
    public static final int IMS = 2;
    public static final int INVALID = -1;
    public static final int OEM_BASE = 1000;
    public static final int TETHERED = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.DataProfileId.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.DataProfileId.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "DEFAULT";
        }
        if (o == 1) {
            return "TETHERED";
        }
        if (o == 2) {
            return "IMS";
        }
        if (o == 3) {
            return "FOTA";
        }
        if (o == 4) {
            return "CBS";
        }
        if (o == 1000) {
            return "OEM_BASE";
        }
        if (o == -1) {
            return "INVALID";
        }
        return "0x" + Integer.toHexString(o);
    }
}

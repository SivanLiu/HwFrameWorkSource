package android.hardware.radio.V1_0;

public final class CdmaSmsNumberType {
    public static final int ABBREVIATED = 6;
    public static final int ALPHANUMERIC = 5;
    public static final int INTERNATIONAL_OR_DATA_IP = 1;
    public static final int NATIONAL_OR_INTERNET_MAIL = 2;
    public static final int NETWORK = 3;
    public static final int RESERVED_7 = 7;
    public static final int SUBSCRIBER = 4;
    public static final int UNKNOWN = 0;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.CdmaSmsNumberType.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.CdmaSmsNumberType.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "UNKNOWN";
        }
        if (o == 1) {
            return "INTERNATIONAL_OR_DATA_IP";
        }
        if (o == 2) {
            return "NATIONAL_OR_INTERNET_MAIL";
        }
        if (o == 3) {
            return "NETWORK";
        }
        if (o == 4) {
            return "SUBSCRIBER";
        }
        if (o == 5) {
            return "ALPHANUMERIC";
        }
        if (o == 6) {
            return "ABBREVIATED";
        }
        if (o == 7) {
            return "RESERVED_7";
        }
        return "0x" + Integer.toHexString(o);
    }
}

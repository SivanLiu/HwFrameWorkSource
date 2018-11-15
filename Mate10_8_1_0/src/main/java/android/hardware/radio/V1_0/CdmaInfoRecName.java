package android.hardware.radio.V1_0;

public final class CdmaInfoRecName {
    public static final int CALLED_PARTY_NUMBER = 1;
    public static final int CALLING_PARTY_NUMBER = 2;
    public static final int CONNECTED_NUMBER = 3;
    public static final int DISPLAY = 0;
    public static final int EXTENDED_DISPLAY = 7;
    public static final int LINE_CONTROL = 6;
    public static final int REDIRECTING_NUMBER = 5;
    public static final int SIGNAL = 4;
    public static final int T53_AUDIO_CONTROL = 10;
    public static final int T53_CLIR = 8;
    public static final int T53_RELEASE = 9;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.CdmaInfoRecName.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.CdmaInfoRecName.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 0) {
            return "DISPLAY";
        }
        if (o == 1) {
            return "CALLED_PARTY_NUMBER";
        }
        if (o == 2) {
            return "CALLING_PARTY_NUMBER";
        }
        if (o == 3) {
            return "CONNECTED_NUMBER";
        }
        if (o == 4) {
            return "SIGNAL";
        }
        if (o == 5) {
            return "REDIRECTING_NUMBER";
        }
        if (o == 6) {
            return "LINE_CONTROL";
        }
        if (o == 7) {
            return "EXTENDED_DISPLAY";
        }
        if (o == 8) {
            return "T53_CLIR";
        }
        if (o == 9) {
            return "T53_RELEASE";
        }
        if (o == 10) {
            return "T53_AUDIO_CONTROL";
        }
        return "0x" + Integer.toHexString(o);
    }
}

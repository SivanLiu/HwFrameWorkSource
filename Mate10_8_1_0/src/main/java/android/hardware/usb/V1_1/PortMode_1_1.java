package android.hardware.usb.V1_1;

public final class PortMode_1_1 {
    public static final int AUDIO_ACCESSORY = 4;
    public static final int DEBUG_ACCESSORY = 8;
    public static final int DFP = 2;
    public static final int DRP = 3;
    public static final int NONE = 0;
    public static final int NUM_MODES = 4;
    public static final int NUM_MODES_1_1 = 16;
    public static final int UFP = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.usb.V1_1.PortMode_1_1.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.usb.V1_1.PortMode_1_1.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 4) {
            return "AUDIO_ACCESSORY";
        }
        if (o == 8) {
            return "DEBUG_ACCESSORY";
        }
        if (o == 16) {
            return "NUM_MODES_1_1";
        }
        return "0x" + Integer.toHexString(o);
    }
}

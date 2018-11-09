package android.hardware.wifi.supplicant.V1_0;

public final class WpsConfigMethods {
    public static final short DISPLAY = (short) 8;
    public static final short ETHERNET = (short) 2;
    public static final short EXT_NFC_TOKEN = (short) 16;
    public static final short INT_NFC_TOKEN = (short) 32;
    public static final short KEYPAD = (short) 256;
    public static final short LABEL = (short) 4;
    public static final short NFC_INTERFACE = (short) 64;
    public static final short P2PS = (short) 4096;
    public static final short PHY_DISPLAY = (short) 16392;
    public static final short PHY_PUSHBUTTON = (short) 1152;
    public static final short PUSHBUTTON = (short) 128;
    public static final short USBA = (short) 1;
    public static final short VIRT_DISPLAY = (short) 8200;
    public static final short VIRT_PUSHBUTTON = (short) 640;

    public static final java.lang.String dumpBitfield(short r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.wifi.supplicant.V1_0.WpsConfigMethods.dumpBitfield(short):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.wifi.supplicant.V1_0.WpsConfigMethods.dumpBitfield(short):java.lang.String");
    }

    public static final String toString(short o) {
        if (o == (short) 1) {
            return "USBA";
        }
        if (o == (short) 2) {
            return "ETHERNET";
        }
        if (o == (short) 4) {
            return "LABEL";
        }
        if (o == (short) 8) {
            return "DISPLAY";
        }
        if (o == (short) 16) {
            return "EXT_NFC_TOKEN";
        }
        if (o == (short) 32) {
            return "INT_NFC_TOKEN";
        }
        if (o == (short) 64) {
            return "NFC_INTERFACE";
        }
        if (o == PUSHBUTTON) {
            return "PUSHBUTTON";
        }
        if (o == KEYPAD) {
            return "KEYPAD";
        }
        if (o == VIRT_PUSHBUTTON) {
            return "VIRT_PUSHBUTTON";
        }
        if (o == PHY_PUSHBUTTON) {
            return "PHY_PUSHBUTTON";
        }
        if (o == P2PS) {
            return "P2PS";
        }
        if (o == VIRT_DISPLAY) {
            return "VIRT_DISPLAY";
        }
        if (o == PHY_DISPLAY) {
            return "PHY_DISPLAY";
        }
        return "0x" + Integer.toHexString(Short.toUnsignedInt(o));
    }
}

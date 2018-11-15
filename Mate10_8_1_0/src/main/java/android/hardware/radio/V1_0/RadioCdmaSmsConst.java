package android.hardware.radio.V1_0;

public final class RadioCdmaSmsConst {
    public static final int ADDRESS_MAX = 36;
    public static final int BEARER_DATA_MAX = 255;
    public static final int IP_ADDRESS_SIZE = 4;
    public static final int MAX_UD_HEADERS = 7;
    public static final int SUBADDRESS_MAX = 36;
    public static final int UDH_ANIM_NUM_BITMAPS = 4;
    public static final int UDH_EO_DATA_SEGMENT_MAX = 131;
    public static final int UDH_LARGE_BITMAP_SIZE = 32;
    public static final int UDH_LARGE_PIC_SIZE = 128;
    public static final int UDH_MAX_SND_SIZE = 128;
    public static final int UDH_OTHER_SIZE = 226;
    public static final int UDH_SMALL_BITMAP_SIZE = 8;
    public static final int UDH_SMALL_PIC_SIZE = 32;
    public static final int UDH_VAR_PIC_SIZE = 134;
    public static final int USER_DATA_MAX = 229;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.radio.V1_0.RadioCdmaSmsConst.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.radio.V1_0.RadioCdmaSmsConst.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 36) {
            return "ADDRESS_MAX";
        }
        if (o == 36) {
            return "SUBADDRESS_MAX";
        }
        if (o == 255) {
            return "BEARER_DATA_MAX";
        }
        if (o == 128) {
            return "UDH_MAX_SND_SIZE";
        }
        if (o == 131) {
            return "UDH_EO_DATA_SEGMENT_MAX";
        }
        if (o == 7) {
            return "MAX_UD_HEADERS";
        }
        if (o == 229) {
            return "USER_DATA_MAX";
        }
        if (o == 128) {
            return "UDH_LARGE_PIC_SIZE";
        }
        if (o == 32) {
            return "UDH_SMALL_PIC_SIZE";
        }
        if (o == 134) {
            return "UDH_VAR_PIC_SIZE";
        }
        if (o == 4) {
            return "UDH_ANIM_NUM_BITMAPS";
        }
        if (o == 32) {
            return "UDH_LARGE_BITMAP_SIZE";
        }
        if (o == 8) {
            return "UDH_SMALL_BITMAP_SIZE";
        }
        if (o == 226) {
            return "UDH_OTHER_SIZE";
        }
        if (o == 4) {
            return "IP_ADDRESS_SIZE";
        }
        return "0x" + Integer.toHexString(o);
    }
}

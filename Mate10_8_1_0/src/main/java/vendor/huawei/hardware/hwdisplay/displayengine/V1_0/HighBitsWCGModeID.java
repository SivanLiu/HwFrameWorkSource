package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class HighBitsWCGModeID {
    public static final int MODE_ADOBERGB = 4096;
    public static final int MODE_DISPLAYP3 = 8192;
    public static final int MODE_SUPERGAMUT = 12288;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsWCGModeID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsWCGModeID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 4096) {
            return "MODE_ADOBERGB";
        }
        if (o == 8192) {
            return "MODE_DISPLAYP3";
        }
        if (o == MODE_SUPERGAMUT) {
            return "MODE_SUPERGAMUT";
        }
        return "0x" + Integer.toHexString(o);
    }
}

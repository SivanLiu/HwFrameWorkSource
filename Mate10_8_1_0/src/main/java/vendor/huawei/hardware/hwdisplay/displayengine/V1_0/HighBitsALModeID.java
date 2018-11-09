package vendor.huawei.hardware.hwdisplay.displayengine.V1_0;

public final class HighBitsALModeID {
    public static final int MODE_LRE = 512;
    public static final int MODE_LRE_DISABLE = 1024;
    public static final int MODE_SRE = 256;
    public static final int MODE_SRE_DISABLE = 768;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsALModeID.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsALModeID.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 256) {
            return "MODE_SRE";
        }
        if (o == 512) {
            return "MODE_LRE";
        }
        if (o == MODE_SRE_DISABLE) {
            return "MODE_SRE_DISABLE";
        }
        if (o == 1024) {
            return "MODE_LRE_DISABLE";
        }
        return "0x" + Integer.toHexString(o);
    }
}

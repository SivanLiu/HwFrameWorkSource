package android.hardware.biometrics.fingerprint.V2_1;

public final class RequestStatus {
    public static final int SYS_EACCES = -13;
    public static final int SYS_EAGAIN = -11;
    public static final int SYS_EBUSY = -16;
    public static final int SYS_EFAULT = -14;
    public static final int SYS_EINTR = -4;
    public static final int SYS_EINVAL = -22;
    public static final int SYS_EIO = -5;
    public static final int SYS_ENOENT = -2;
    public static final int SYS_ENOMEM = -12;
    public static final int SYS_ENOSPC = -28;
    public static final int SYS_ETIMEDOUT = -110;
    public static final int SYS_OK = 0;
    public static final int SYS_UNKNOWN = 1;

    public static final java.lang.String dumpBitfield(int r1) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: android.hardware.biometrics.fingerprint.V2_1.RequestStatus.dumpBitfield(int):java.lang.String
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
        throw new UnsupportedOperationException("Method not decompiled: android.hardware.biometrics.fingerprint.V2_1.RequestStatus.dumpBitfield(int):java.lang.String");
    }

    public static final String toString(int o) {
        if (o == 1) {
            return "SYS_UNKNOWN";
        }
        if (o == 0) {
            return "SYS_OK";
        }
        if (o == -2) {
            return "SYS_ENOENT";
        }
        if (o == -4) {
            return "SYS_EINTR";
        }
        if (o == -5) {
            return "SYS_EIO";
        }
        if (o == -11) {
            return "SYS_EAGAIN";
        }
        if (o == -12) {
            return "SYS_ENOMEM";
        }
        if (o == -13) {
            return "SYS_EACCES";
        }
        if (o == -14) {
            return "SYS_EFAULT";
        }
        if (o == -16) {
            return "SYS_EBUSY";
        }
        if (o == -22) {
            return "SYS_EINVAL";
        }
        if (o == -28) {
            return "SYS_ENOSPC";
        }
        if (o == SYS_ETIMEDOUT) {
            return "SYS_ETIMEDOUT";
        }
        return "0x" + Integer.toHexString(o);
    }
}

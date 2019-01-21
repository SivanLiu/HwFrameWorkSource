package java.lang;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;

final class ProcessImpl {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    private ProcessImpl() {
    }

    private static byte[] toCString(String s) {
        if (s == null) {
            return null;
        }
        byte[] bytes = s.getBytes();
        byte[] result = new byte[(bytes.length + 1)];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:89:0x0158 A:{SYNTHETIC, Splitter:B:89:0x0158} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0174 A:{SYNTHETIC, Splitter:B:105:0x0174} */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x0182  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static Process start(String[] cmdarray, Map<String, String> environment, String dir, Redirect[] redirects, boolean redirectErrorStream) throws IOException {
        int i;
        byte[] arg;
        int[] iArr;
        Throwable th;
        int[] iArr2;
        int i2;
        byte[] bArr;
        Throwable th2;
        Throwable th3;
        String[] strArr = cmdarray;
        byte[][] args = new byte[(strArr.length - 1)][];
        int size = args.length;
        for (i = 0; i < args.length; i++) {
            args[i] = strArr[i + 1].getBytes();
            size += args[i].length;
        }
        byte[] argBlock = new byte[size];
        int i3 = 0;
        for (byte[] arg2 : args) {
            System.arraycopy(arg2, 0, argBlock, i3, arg2.length);
            i3 += arg2.length + 1;
        }
        int[] envc = new int[1];
        byte[] envBlock = ProcessEnvironment.toEnvironmentBlock(environment, envc);
        FileInputStream f0 = null;
        FileOutputStream f1 = null;
        FileOutputStream f2 = null;
        if (redirects == null) {
            try {
                iArr = new int[3];
                iArr = new int[]{-1, -1, -1};
            } catch (Throwable th4) {
                th = th4;
                iArr2 = envc;
                i2 = i3;
                bArr = argBlock;
            }
        } else {
            try {
                iArr = new int[3];
                if (redirects[0] == Redirect.PIPE) {
                    iArr[0] = -1;
                } else if (redirects[0] == Redirect.INHERIT) {
                    iArr[0] = 0;
                } else {
                    f0 = new FileInputStream(redirects[0].file());
                    iArr[0] = f0.getFD().getInt$();
                }
                if (redirects[1] == Redirect.PIPE) {
                    iArr[1] = -1;
                } else if (redirects[1] == Redirect.INHERIT) {
                    iArr[1] = 1;
                } else {
                    f1 = new FileOutputStream(redirects[1].file(), redirects[1].append());
                    iArr[1] = f1.getFD().getInt$();
                }
                if (redirects[2] == Redirect.PIPE) {
                    iArr[2] = -1;
                } else if (redirects[2] == Redirect.INHERIT) {
                    iArr[2] = 2;
                } else {
                    f2 = new FileOutputStream(redirects[2].file(), redirects[2].append());
                    iArr[2] = f2.getFD().getInt$();
                }
            } catch (Throwable th5) {
                th = th5;
                iArr2 = envc;
                i2 = i3;
                bArr = argBlock;
            }
        }
        int[] std_fds = iArr;
        FileInputStream f02 = f0;
        FileOutputStream f12 = f1;
        FileOutputStream f22 = f2;
        try {
            arg2 = toCString(strArr[0]);
            UNIXProcess uNIXProcess = uNIXProcess;
            FileOutputStream f23 = f22;
            FileOutputStream f13 = f12;
            try {
                uNIXProcess = new UNIXProcess(arg2, argBlock, args.length, envBlock, envc[0], toCString(dir), std_fds, redirectErrorStream);
                if (f02 != null) {
                    try {
                        f02.close();
                    } catch (Throwable th6) {
                        if (f23 != null) {
                            f23.close();
                        }
                        th2 = th6;
                    }
                }
                if (f13 != null) {
                    try {
                        f13.close();
                    } catch (Throwable th62) {
                        if (f23 != null) {
                            f23.close();
                        }
                        th2 = th62;
                    }
                }
                if (f23 != null) {
                    f23.close();
                }
                return uNIXProcess;
            } catch (Throwable th7) {
                th62 = th7;
                f2 = f23;
                f1 = f13;
                f0 = f02;
                if (f0 != null) {
                }
                if (f1 != null) {
                }
                if (f2 != null) {
                }
                throw th62;
            }
        } catch (Throwable th8) {
            th62 = th8;
            iArr2 = envc;
            i2 = i3;
            bArr = argBlock;
            f2 = f22;
            f1 = f12;
            f0 = f02;
            if (f0 != null) {
                try {
                    f0.close();
                } catch (Throwable th622) {
                    if (f2 != null) {
                        f2.close();
                    }
                    th3 = th622;
                }
            }
            if (f1 != null) {
                try {
                    f1.close();
                } catch (Throwable th6222) {
                    if (f2 != null) {
                        f2.close();
                    }
                    th3 = th6222;
                }
            }
            if (f2 != null) {
                f2.close();
            }
            throw th6222;
        }
    }
}

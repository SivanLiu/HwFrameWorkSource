package java.lang;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;

final class ProcessImpl {
    static final /* synthetic */ boolean -assertionsDisabled = (ProcessImpl.class.desiredAssertionStatus() ^ 1);

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

    static Process start(String[] cmdarray, Map<String, String> environment, String dir, Redirect[] redirects, boolean redirectErrorStream) throws IOException {
        Throwable th;
        if (-assertionsDisabled || (cmdarray != null && cmdarray.length > 0)) {
            int i;
            int[] std_fds;
            byte[][] args = new byte[(cmdarray.length - 1)][];
            int size = args.length;
            for (i = 0; i < args.length; i++) {
                args[i] = cmdarray[i + 1].getBytes();
                size += args[i].length;
            }
            byte[] argBlock = new byte[size];
            i = 0;
            for (byte[] arg : args) {
                System.arraycopy(arg, 0, argBlock, i, arg.length);
                i += arg.length + 1;
            }
            int[] envc = new int[1];
            byte[] envBlock = ProcessEnvironment.toEnvironmentBlock(environment, envc);
            FileInputStream fileInputStream = null;
            FileOutputStream f1 = null;
            FileOutputStream f2 = null;
            if (redirects == null) {
                try {
                    std_fds = new int[]{-1, -1, -1};
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th3) {
                            if (f2 != null) {
                                f2.close();
                            }
                        }
                    }
                    if (f1 != null) {
                        try {
                            f1.close();
                        } catch (Throwable th4) {
                            if (f2 != null) {
                                f2.close();
                            }
                        }
                    }
                    if (f2 != null) {
                        f2.close();
                    }
                    throw th;
                }
            }
            FileOutputStream fileOutputStream;
            std_fds = new int[3];
            if (redirects[0] == Redirect.PIPE) {
                std_fds[0] = -1;
            } else if (redirects[0] == Redirect.INHERIT) {
                std_fds[0] = 0;
            } else {
                FileInputStream f0 = new FileInputStream(redirects[0].file());
                try {
                    std_fds[0] = f0.getFD().getInt$();
                    fileInputStream = f0;
                } catch (Throwable th5) {
                    th = th5;
                    fileInputStream = f0;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (f1 != null) {
                        f1.close();
                    }
                    if (f2 != null) {
                        f2.close();
                    }
                    throw th;
                }
            }
            if (redirects[1] == Redirect.PIPE) {
                std_fds[1] = -1;
            } else if (redirects[1] == Redirect.INHERIT) {
                std_fds[1] = 1;
            } else {
                fileOutputStream = new FileOutputStream(redirects[1].file(), redirects[1].append());
                try {
                    std_fds[1] = fileOutputStream.getFD().getInt$();
                    f1 = fileOutputStream;
                } catch (Throwable th6) {
                    th = th6;
                    f1 = fileOutputStream;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (f1 != null) {
                        f1.close();
                    }
                    if (f2 != null) {
                        f2.close();
                    }
                    throw th;
                }
            }
            if (redirects[2] == Redirect.PIPE) {
                std_fds[2] = -1;
            } else if (redirects[2] == Redirect.INHERIT) {
                std_fds[2] = 2;
            } else {
                fileOutputStream = new FileOutputStream(redirects[2].file(), redirects[2].append());
                try {
                    std_fds[2] = fileOutputStream.getFD().getInt$();
                    f2 = fileOutputStream;
                } catch (Throwable th7) {
                    th = th7;
                    f2 = fileOutputStream;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (f1 != null) {
                        f1.close();
                    }
                    if (f2 != null) {
                        f2.close();
                    }
                    throw th;
                }
            }
            Process uNIXProcess = new UNIXProcess(toCString(cmdarray[0]), argBlock, args.length, envBlock, envc[0], toCString(dir), std_fds, redirectErrorStream);
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th8) {
                    if (f2 != null) {
                        f2.close();
                    }
                }
            }
            if (f1 != null) {
                try {
                    f1.close();
                } catch (Throwable th9) {
                    if (f2 != null) {
                        f2.close();
                    }
                }
            }
            if (f2 != null) {
                f2.close();
            }
            return uNIXProcess;
        }
        throw new AssertionError();
    }
}

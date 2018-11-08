package android.util;

import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class HwLogExceptionInner implements LogException {
    public static final int LEVEL_A = 65;
    public static final int LEVEL_B = 66;
    public static final int LEVEL_C = 67;
    public static final int LEVEL_D = 68;
    private static final int LOG_ID_EXCEPTION = 5;
    public static final String TAG = "HwLogExceptionInner";
    private static Set<String> mLogBlackList = new HashSet();
    private static LogException mLogExceptionInner = null;

    public static native int println_exception_native(String str, int i, String str2, String str3);

    public static native int setliblogparam_native(int i, String str);

    static {
        System.loadLibrary("hwlog_jni");
    }

    public static synchronized LogException getInstance() {
        LogException logException;
        synchronized (HwLogExceptionInner.class) {
            if (mLogExceptionInner == null) {
                mLogExceptionInner = new HwLogExceptionInner();
            }
            logException = mLogExceptionInner;
        }
        return logException;
    }

    public int cmd(String tag, String contain) {
        return println_exception_native(tag, 0, "command", contain);
    }

    public int msg(String category, int level, String header, String body) {
        return println_exception_native(category, level, "message", header + '\n' + body);
    }

    public int msg(String category, int level, int mask, String header, String body) {
        return println_exception_native(category, level, "message", "mask=" + mask + ";" + header + '\n' + body);
    }

    public int setliblogparam(int paramid, String val) {
        return setliblogparam_native(paramid, val);
    }

    public void initLogBlackList() {
        initLogBlackList_static();
    }

    public boolean isInLogBlackList(String packageName) {
        return isInLogBlackList_static(packageName);
    }

    public static void initLogBlackList_static() {
        Throwable th;
        File blackListFile = new File(Environment.getRootDirectory().getPath() + "/etc/hiview/log_blacklist.cfg");
        if (blackListFile.isFile() && blackListFile.canRead()) {
            BufferedReader bufferedReader = null;
            InputStreamReader is = null;
            FileInputStream fileInputStream = null;
            try {
                FileInputStream fi = new FileInputStream(blackListFile);
                try {
                    InputStreamReader is2 = new InputStreamReader(fi, "UTF-8");
                    try {
                        BufferedReader in = new BufferedReader(is2);
                        while (true) {
                            try {
                                String blackPackageName = in.readLine();
                                if (blackPackageName == null) {
                                    break;
                                }
                                mLogBlackList.add(blackPackageName);
                            } catch (IOException e) {
                                fileInputStream = fi;
                                is = is2;
                                bufferedReader = in;
                            } catch (Throwable th2) {
                                th = th2;
                                fileInputStream = fi;
                                is = is2;
                                bufferedReader = in;
                            }
                        }
                        if (fi != null) {
                            try {
                                fi.close();
                            } catch (IOException e2) {
                                Log.e(TAG, "close fi IOException");
                            }
                        }
                        if (is2 != null) {
                            try {
                                is2.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "close is IOException");
                            }
                        }
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e4) {
                                Log.e(TAG, "close in IOException");
                            }
                        }
                    } catch (IOException e5) {
                        fileInputStream = fi;
                        is = is2;
                        try {
                            Log.e(TAG, "initLogBlackList_static IOException");
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e6) {
                                    Log.e(TAG, "close fi IOException");
                                }
                            }
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e7) {
                                    Log.e(TAG, "close is IOException");
                                }
                            }
                            if (bufferedReader == null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e8) {
                                    Log.e(TAG, "close in IOException");
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e9) {
                                    Log.e(TAG, "close fi IOException");
                                }
                            }
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e10) {
                                    Log.e(TAG, "close is IOException");
                                }
                            }
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e11) {
                                    Log.e(TAG, "close in IOException");
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        fileInputStream = fi;
                        is = is2;
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        if (is != null) {
                            is.close();
                        }
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        throw th;
                    }
                } catch (IOException e12) {
                    fileInputStream = fi;
                    Log.e(TAG, "initLogBlackList_static IOException");
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    if (bufferedReader == null) {
                        bufferedReader.close();
                    }
                } catch (Throwable th5) {
                    th = th5;
                    fileInputStream = fi;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    throw th;
                }
            } catch (IOException e13) {
                Log.e(TAG, "initLogBlackList_static IOException");
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (is != null) {
                    is.close();
                }
                if (bufferedReader == null) {
                    bufferedReader.close();
                }
            }
        }
    }

    public static boolean isInLogBlackList_static(String packageName) {
        return mLogBlackList.contains(packageName);
    }
}

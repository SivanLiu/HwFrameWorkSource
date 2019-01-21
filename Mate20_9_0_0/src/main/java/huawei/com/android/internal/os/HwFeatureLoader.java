package huawei.com.android.internal.os;

import android.common.HwFrameworkFactory.IHwFeatureLoader;
import android.os.SystemClock;
import android.os.Trace;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.VMRuntime;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HwFeatureLoader implements IHwFeatureLoader {
    private static final String FEATURE_PRELOADED_CLASSES = "/feature/preloaded-classes";
    private static final String PATH_FEATURE = "/feature/dexpaths";
    private static final int ROOT_GID = 0;
    private static final int ROOT_UID = 0;
    private static final String TAG = "HwFeatureLoader";
    private static final int UNPRIVILEGED_GID = 9999;
    private static final int UNPRIVILEGED_UID = 9999;

    public void addDexPaths() {
        BaseDexClassLoader systemload = (BaseDexClassLoader) ClassLoader.getSystemClassLoader();
        if (systemload == null) {
            Log.e(TAG, "SystemClassLoader is null!");
            return;
        }
        File pathFile = HwCfgFilePolicy.getCfgFile(PATH_FEATURE, 0);
        if (pathFile == null) {
            Log.d(TAG, "get pathFile :/feature/dexpaths failed!");
            return;
        }
        BufferedReader br = null;
        try {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(pathFile), "UTF-8"), 256);
                while (true) {
                    String readLine = br.readLine();
                    String line = readLine;
                    if (readLine != null) {
                        readLine = line.trim();
                        if (!readLine.startsWith("#")) {
                            if (!readLine.equals("")) {
                                line = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("addDexPath: ");
                                stringBuilder.append(readLine);
                                Log.i(line, stringBuilder.toString());
                                systemload.addDexPath(readLine);
                            }
                        }
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                            Log.e(TAG, "Error in close BufferedReader /feature/dexpaths.", e);
                        }
                    }
                }
                br.close();
            } catch (IOException e2) {
                Log.e(TAG, "Error reading /feature/dexpaths.", e2);
                if (br != null) {
                    br.close();
                }
            } catch (Throwable th) {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Error in close BufferedReader /feature/dexpaths.", e3);
                    }
                }
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "Couldn't find /feature/dexpaths.");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:101:0x01d1 A:{SYNTHETIC, Splitter:B:101:0x01d1} */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x01e5  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x01fb A:{SYNTHETIC, Splitter:B:117:0x01fb} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x020f  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x01d1 A:{SYNTHETIC, Splitter:B:101:0x01d1} */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x01e5  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x01fb A:{SYNTHETIC, Splitter:B:117:0x01fb} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x020f  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x01d1 A:{SYNTHETIC, Splitter:B:101:0x01d1} */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x01e5  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x01fb A:{SYNTHETIC, Splitter:B:117:0x01fb} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x020f  */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x01fb A:{SYNTHETIC, Splitter:B:117:0x01fb} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x020f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void preloadClasses() {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        Throwable th;
        IOException e;
        int i;
        int i2;
        IOException iOException;
        BaseDexClassLoader systemload = (BaseDexClassLoader) ClassLoader.getSystemClassLoader();
        if (systemload == null) {
            Log.e(TAG, "SystemClassLoader is null!");
            return;
        }
        VMRuntime runtime = VMRuntime.getRuntime();
        File classesFile = HwCfgFilePolicy.getCfgFile(FEATURE_PRELOADED_CLASSES, 0);
        if (classesFile == null) {
            Log.d(TAG, "get classesFile :/feature/preloaded-classes failed!");
            return;
        }
        BufferedReader br = null;
        File classesFile2;
        try {
            InputStream is = new FileInputStream(classesFile);
            Log.i(TAG, "Preloading classes...");
            long startTime = SystemClock.uptimeMillis();
            int reuid = Os.getuid();
            int regid = Os.getgid();
            boolean droppedPriviliges = false;
            if (reuid == 0 && regid == 0) {
                try {
                    Os.setregid(0, 9999);
                    Os.setreuid(0, 9999);
                    droppedPriviliges = true;
                } catch (ErrnoException ex) {
                    throw new RuntimeException("Failed to drop root", ex);
                }
            }
            float defaultUtilization = runtime.getTargetHeapUtilization();
            runtime.setTargetHeapUtilization(0.8f);
            try {
                int count;
                String readLine;
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 256);
                int count2 = 0;
                while (true) {
                    count = count2;
                    readLine = br.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    try {
                        line = line.trim();
                        if (line.startsWith("#")) {
                            classesFile2 = classesFile;
                        } else if (line.equals("")) {
                            classesFile2 = classesFile;
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("PreloadClass ");
                            stringBuilder3.append(line);
                            classesFile2 = classesFile;
                            try {
                                Trace.traceBegin(16384, stringBuilder3.toString());
                                Class.forName(line, true, systemload);
                                count++;
                            } catch (ClassNotFoundException count22) {
                                Object obj = count22;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Class not found for preloading: ");
                                stringBuilder.append(line);
                                Log.w(str, stringBuilder.toString());
                            } catch (UnsatisfiedLinkError e2) {
                                UnsatisfiedLinkError unsatisfiedLinkError = e2;
                                String str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Problem preloading ");
                                stringBuilder2.append(line);
                                stringBuilder2.append(": ");
                                stringBuilder2.append(e2);
                                Log.w(str2, stringBuilder2.toString());
                            } catch (IOException e3) {
                                e = e3;
                                i = reuid;
                                i2 = regid;
                                try {
                                    Log.e(TAG, "Error reading /feature/preloaded-classes.", e);
                                    if (br != null) {
                                    }
                                    runtime.setTargetHeapUtilization(defaultUtilization);
                                    if (droppedPriviliges) {
                                    }
                                } catch (Throwable t) {
                                    th = t;
                                    if (br != null) {
                                    }
                                    runtime.setTargetHeapUtilization(defaultUtilization);
                                    if (droppedPriviliges) {
                                    }
                                    throw th;
                                }
                            } catch (Throwable t2) {
                                th = t2;
                                i = reuid;
                                i2 = regid;
                                if (br != null) {
                                }
                                runtime.setTargetHeapUtilization(defaultUtilization);
                                if (droppedPriviliges) {
                                }
                                throw th;
                            }
                            count22 = count;
                            Trace.traceEnd(16384);
                            classesFile = classesFile2;
                        }
                        count22 = count;
                        classesFile = classesFile2;
                    } catch (IOException e4) {
                        e = e4;
                        classesFile2 = classesFile;
                        i = reuid;
                        i2 = regid;
                        Log.e(TAG, "Error reading /feature/preloaded-classes.", e);
                        if (br != null) {
                        }
                        runtime.setTargetHeapUtilization(defaultUtilization);
                        if (droppedPriviliges) {
                        }
                    } catch (Throwable t22) {
                        classesFile2 = classesFile;
                        th = t22;
                        i = reuid;
                        i2 = regid;
                        if (br != null) {
                        }
                        runtime.setTargetHeapUtilization(defaultUtilization);
                        if (droppedPriviliges) {
                        }
                        throw th;
                    }
                }
                try {
                    readLine = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("...preloaded ");
                    stringBuilder2.append(count);
                    stringBuilder2.append(" classes in ");
                    try {
                        stringBuilder2.append(SystemClock.uptimeMillis() - startTime);
                        stringBuilder2.append("ms.");
                        Log.i(readLine, stringBuilder2.toString());
                        try {
                            br.close();
                        } catch (IOException e5) {
                            iOException = e5;
                            Log.e(TAG, "Error in close BufferedReader /feature/preloaded-classes.", e5);
                        }
                        runtime.setTargetHeapUtilization(defaultUtilization);
                        if (droppedPriviliges) {
                            try {
                                Os.setreuid(0, 0);
                                Os.setregid(0, 0);
                            } catch (ErrnoException ex2) {
                                throw new RuntimeException("Failed to restore root", ex2);
                            }
                        }
                    } catch (IOException e6) {
                        e5 = e6;
                    }
                } catch (IOException e7) {
                    e5 = e7;
                    i = reuid;
                    i2 = regid;
                    Log.e(TAG, "Error reading /feature/preloaded-classes.", e5);
                    if (br != null) {
                    }
                    runtime.setTargetHeapUtilization(defaultUtilization);
                    if (droppedPriviliges) {
                    }
                } catch (Throwable t222) {
                    i = reuid;
                    i2 = regid;
                    th = t222;
                    if (br != null) {
                    }
                    runtime.setTargetHeapUtilization(defaultUtilization);
                    if (droppedPriviliges) {
                    }
                    throw th;
                }
            } catch (IOException e8) {
                e5 = e8;
                classesFile2 = classesFile;
                i = reuid;
                i2 = regid;
                Log.e(TAG, "Error reading /feature/preloaded-classes.", e5);
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e52) {
                        iOException = e52;
                        Log.e(TAG, "Error in close BufferedReader /feature/preloaded-classes.", e52);
                    }
                }
                runtime.setTargetHeapUtilization(defaultUtilization);
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex22) {
                        throw new RuntimeException("Failed to restore root", ex22);
                    }
                }
            } catch (Throwable t2222) {
                classesFile2 = classesFile;
                i = reuid;
                i2 = regid;
                th = t2222;
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e522) {
                        IOException iOException2 = e522;
                        Log.e(TAG, "Error in close BufferedReader /feature/preloaded-classes.", e522);
                    }
                }
                runtime.setTargetHeapUtilization(defaultUtilization);
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex222) {
                        throw new RuntimeException("Failed to restore root", ex222);
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            classesFile2 = classesFile;
            Log.e(TAG, "Couldn't find /feature/preloaded-classes.");
        }
    }
}

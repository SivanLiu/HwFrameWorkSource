package com.android.internal.os;

import android.common.HwFrameworkFactory;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.icu.impl.CacheValue;
import android.icu.impl.CacheValue.Strength;
import android.icu.text.DecimalFormatSymbols;
import android.icu.util.ULocale;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Environment;
import android.os.IInstalld;
import android.os.IInstalld.Stub;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.ZygoteProcess;
import android.os.storage.StorageManager;
import android.security.keystore.AndroidKeyStoreProvider;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructCapUserData;
import android.system.StructCapUserHeader;
import android.text.Hyphenator;
import android.util.EventLog;
import android.util.Jlog;
import android.util.Log;
import android.util.TimingsTraceLog;
import android.webkit.WebViewFactory;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.Preconditions;
import com.huawei.featurelayer.HwFeatureLoader.SystemFeature;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import dalvik.system.ZygoteHooks;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Provider;
import java.security.Security;
import libcore.io.IoUtils;

public class ZygoteInit {
    private static final String ABI_LIST_ARG = "--abi-list=";
    private static final int BOOT_PRELOAD_CLASS_RAM = 307200;
    private static final int LOG_BOOT_PROGRESS_PRELOAD_END = 3030;
    private static final int LOG_BOOT_PROGRESS_PRELOAD_START = 3020;
    private static final String PRELOADED_CLASSES = "/system/etc/preloaded-classes";
    private static final int PRELOAD_GC_THRESHOLD = 67108864;
    public static final boolean PRELOAD_RESOURCES = true;
    private static final String PROPERTY_DISABLE_OPENGL_PRELOADING = "ro.zygote.disable_gl_preload";
    private static final String PROPERTY_GFX_DRIVER = "ro.gfx.driver.0";
    private static final int ROOT_GID = 0;
    private static final int ROOT_UID = 0;
    private static final String SOCKET_NAME_ARG = "--socket-name=";
    private static final String TAG = "Zygote";
    private static final int UNPRIVILEGED_GID = 9999;
    private static final int UNPRIVILEGED_UID = 9999;
    private static boolean isPrimaryCpuAbi = false;
    private static Resources mResources;
    private static boolean sPreloadComplete;

    private static native void nativePreloadAppProcessHALs();

    private static final native void nativeZygoteInit();

    static void preload(TimingsTraceLog bootTimingsTraceLog) {
        Log.d(TAG, "begin preload");
        bootTimingsTraceLog.traceBegin("BeginIcuCachePinning");
        beginIcuCachePinning();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadClasses");
        preloadClasses();
        SystemFeature.loadFeatureFramework(null);
        SystemFeature.preloadClasses();
        bootTimingsTraceLog.traceEnd();
        bootTimingsTraceLog.traceBegin("PreloadResources");
        preloadResources();
        bootTimingsTraceLog.traceEnd();
        Trace.traceBegin(16384, "PreloadAppProcessHALs");
        nativePreloadAppProcessHALs();
        Trace.traceEnd(16384);
        Trace.traceBegin(16384, "PreloadOpenGL");
        preloadOpenGL();
        Trace.traceEnd(16384);
        preloadSharedLibraries();
        preloadTextResources();
        preloadHwThemeZipsAndSomeIcons(0);
        WebViewFactory.prepareWebViewInZygote();
        endIcuCachePinning();
        warmUpJcaProviders();
        Log.d(TAG, "end preload");
        sPreloadComplete = true;
    }

    public static void lazyPreload() {
        Preconditions.checkState(sPreloadComplete ^ 1);
        Log.i(TAG, "Lazily preloading resources.");
        preload(new TimingsTraceLog("ZygoteInitTiming_lazy", 16384));
    }

    private static void beginIcuCachePinning() {
        Log.i(TAG, "Installing ICU cache reference pinning...");
        CacheValue.setStrength(Strength.STRONG);
        Log.i(TAG, "Preloading ICU data...");
        localesToPin = new ULocale[3];
        int i = 0;
        localesToPin[0] = ULocale.ROOT;
        localesToPin[1] = ULocale.US;
        localesToPin[2] = ULocale.getDefault();
        int length = localesToPin.length;
        while (i < length) {
            DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(localesToPin[i]);
            i++;
        }
    }

    private static void endIcuCachePinning() {
        CacheValue.setStrength(Strength.SOFT);
        Log.i(TAG, "Uninstalled ICU cache reference pinning...");
    }

    private static void preloadSharedLibraries() {
        Log.i(TAG, "Preloading shared libraries...");
        System.loadLibrary("android");
        System.loadLibrary("compiler_rt");
        System.loadLibrary("jnigraphics");
    }

    private static void preloadOpenGL() {
        String driverPackageName = SystemProperties.get(PROPERTY_GFX_DRIVER);
        if (!SystemProperties.getBoolean(PROPERTY_DISABLE_OPENGL_PRELOADING, false)) {
            if (driverPackageName == null || driverPackageName.isEmpty()) {
                EGL14.eglGetDisplay(0);
            }
        }
    }

    private static void preloadTextResources() {
        Hyphenator.init();
        TextView.preloadFontCache();
    }

    private static void warmUpJcaProviders() {
        long startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(16384, "Starting installation of AndroidKeyStoreProvider");
        AndroidKeyStoreProvider.install();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Installed AndroidKeyStoreProvider in ");
        stringBuilder.append(SystemClock.uptimeMillis() - startTime);
        stringBuilder.append("ms.");
        Log.i(str, stringBuilder.toString());
        Trace.traceEnd(16384);
        startTime = SystemClock.uptimeMillis();
        Trace.traceBegin(16384, "Starting warm up of JCA providers");
        for (Provider p : Security.getProviders()) {
            p.warmUpServiceProvision();
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Warmed up JCA providers in ");
        stringBuilder.append(SystemClock.uptimeMillis() - startTime);
        stringBuilder.append("ms.");
        Log.i(str, stringBuilder.toString());
        Trace.traceEnd(16384);
    }

    private static void preloadClasses() {
        String str;
        StringBuilder stringBuilder;
        VMRuntime runtime = VMRuntime.getRuntime();
        try {
            InputStream is = new FileInputStream(PRELOADED_CLASSES);
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
            long j = 16384;
            int count;
            String line;
            StringBuilder stringBuilder2;
            try {
                int count2;
                String readLine;
                BufferedReader br = new BufferedReader(new InputStreamReader(is), BOOT_PRELOAD_CLASS_RAM);
                count = 0;
                while (true) {
                    count2 = count;
                    readLine = br.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    line = line.trim();
                    if (!line.startsWith("#")) {
                        if (!line.equals("")) {
                            Trace.traceBegin(j, line);
                            Class.forName(line, true, null);
                            count2++;
                            count = count2;
                            Trace.traceEnd(16384);
                            j = 16384;
                        }
                    }
                    count = count2;
                    j = 16384;
                }
                readLine = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("...preloaded ");
                stringBuilder2.append(count2);
                stringBuilder2.append(" classes in ");
                stringBuilder2.append(SystemClock.uptimeMillis() - startTime);
                stringBuilder2.append("ms.");
                Log.i(readLine, stringBuilder2.toString());
                IoUtils.closeQuietly(is);
                runtime.setTargetHeapUtilization(defaultUtilization);
                Trace.traceBegin(16384, "PreloadDexCaches");
                runtime.preloadDexCaches();
                Trace.traceEnd(16384);
                if (droppedPriviliges) {
                    try {
                        Os.setreuid(0, 0);
                        Os.setregid(0, 0);
                    } catch (ErrnoException ex2) {
                        throw new RuntimeException("Failed to restore root", ex2);
                    }
                }
            } catch (ClassNotFoundException count3) {
                Object obj = count3;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Class not found for preloading: ");
                stringBuilder.append(line);
                Log.w(str, stringBuilder.toString());
            } catch (UnsatisfiedLinkError e) {
                UnsatisfiedLinkError unsatisfiedLinkError = e;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Problem preloading ");
                stringBuilder.append(line);
                stringBuilder.append(": ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            } catch (IOException e2) {
                try {
                    Log.e(TAG, "Error reading /system/etc/preloaded-classes.", e2);
                    IoUtils.closeQuietly(is);
                    runtime.setTargetHeapUtilization(defaultUtilization);
                    Trace.traceBegin(16384, "PreloadDexCaches");
                    runtime.preloadDexCaches();
                    Trace.traceEnd(16384);
                    if (droppedPriviliges) {
                        try {
                            Os.setreuid(0, 0);
                            Os.setregid(0, 0);
                        } catch (ErrnoException ex22) {
                            throw new RuntimeException("Failed to restore root", ex22);
                        }
                    }
                } catch (Throwable th) {
                    IoUtils.closeQuietly(is);
                    runtime.setTargetHeapUtilization(defaultUtilization);
                    Trace.traceBegin(16384, "PreloadDexCaches");
                    runtime.preloadDexCaches();
                    Trace.traceEnd(16384);
                    if (droppedPriviliges) {
                        try {
                            Os.setreuid(0, 0);
                            Os.setregid(0, 0);
                        } catch (ErrnoException ex222) {
                            throw new RuntimeException("Failed to restore root", ex222);
                        }
                    }
                }
            } catch (Throwable t) {
                Throwable th2 = t;
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error preloading ");
                stringBuilder2.append(line);
                stringBuilder2.append(".");
                Log.e(str2, stringBuilder2.toString(), t);
                RuntimeException runtimeException;
                if (t instanceof Error) {
                    Error error = (Error) t;
                } else if (t instanceof RuntimeException) {
                    runtimeException = (RuntimeException) t;
                } else {
                    runtimeException = new RuntimeException(t);
                }
            }
        } catch (FileNotFoundException e3) {
            Log.e(TAG, "Couldn't find /system/etc/preloaded-classes.");
        }
    }

    private static void preloadResources() {
        VMRuntime runtime = VMRuntime.getRuntime();
        try {
            mResources = Resources.getSystem(true);
            mResources.startPreloading();
            Log.i(TAG, "Preloading resources...");
            long startTime = SystemClock.uptimeMillis();
            TypedArray ar = mResources.obtainTypedArray(17236068);
            int N = preloadDrawables(ar);
            ar.recycle();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("...preloaded ");
            stringBuilder.append(N);
            stringBuilder.append(" resources in ");
            stringBuilder.append(SystemClock.uptimeMillis() - startTime);
            stringBuilder.append("ms.");
            Log.i(str, stringBuilder.toString());
            startTime = SystemClock.uptimeMillis();
            ar = mResources.obtainTypedArray(17236067);
            N = preloadColorStateLists(ar);
            ar.recycle();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("...preloaded ");
            stringBuilder.append(N);
            stringBuilder.append(" resources in ");
            stringBuilder.append(SystemClock.uptimeMillis() - startTime);
            stringBuilder.append("ms.");
            Log.i(str, stringBuilder.toString());
            if (mResources.getBoolean(17956977)) {
                startTime = SystemClock.uptimeMillis();
                ar = mResources.obtainTypedArray(17236069);
                N = preloadDrawables(ar);
                ar.recycle();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("...preloaded ");
                stringBuilder.append(N);
                stringBuilder.append(" resource in ");
                stringBuilder.append(SystemClock.uptimeMillis() - startTime);
                stringBuilder.append("ms.");
                Log.i(str, stringBuilder.toString());
            }
            if (isPrimaryCpuAbi) {
                startTime = SystemClock.uptimeMillis();
                ar = mResources.obtainTypedArray(33816576);
                N = preloadDrawables(ar);
                ar.recycle();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("...preloaded ");
                stringBuilder.append(N);
                stringBuilder.append(" hwextdrawable resources in ");
                stringBuilder.append(SystemClock.uptimeMillis() - startTime);
                stringBuilder.append("ms.");
                Log.w(str, stringBuilder.toString());
            }
            mResources.finishPreloading();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failure preloading resources", e);
        }
    }

    private static int preloadColorStateLists(TypedArray ar) {
        int N = ar.length();
        int i = 0;
        while (i < N) {
            int id = ar.getResourceId(i, 0);
            if (id == 0 || mResources.getColorStateList(id, null) != null) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to find preloaded color resource #0x");
                stringBuilder.append(Integer.toHexString(id));
                stringBuilder.append(" (");
                stringBuilder.append(ar.getString(i));
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return N;
    }

    private static int preloadDrawables(TypedArray ar) {
        int N = ar.length();
        int i = 0;
        while (i < N) {
            int id = ar.getResourceId(i, 0);
            if (id == 0 || mResources.getDrawable(id, null) != null) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to find preloaded drawable resource #0x");
                stringBuilder.append(Integer.toHexString(id));
                stringBuilder.append(" (");
                stringBuilder.append(ar.getString(i));
                stringBuilder.append(")");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return N;
    }

    public static void preloadHwThemeZipsAndSomeIcons(int currentUserId) {
        if (mResources == null) {
            mResources = Resources.getSystem(true);
        }
        Log.i(TAG, "preloadHwThemeZipsAndSomeIcons");
        mResources.getImpl().getHwResourcesImpl().preloadHwThemeZipsAndSomeIcons(currentUserId);
    }

    public static void clearHwThemeZipsAndSomeIcons() {
        if (mResources == null) {
            mResources = Resources.getSystem(true);
        }
        Log.i(TAG, "clearHwThemeZipsAndSomeIcons");
        mResources.getImpl().getHwResourcesImpl().clearHwThemeZipsAndSomeIcons();
    }

    static void gcAndFinalize() {
        VMRuntime runtime = VMRuntime.getRuntime();
        System.gc();
        runtime.runFinalizationSync();
        System.gc();
    }

    private static Runnable handleSystemServerProcess(Arguments parsedArgs) {
        ExitCatch.enable(Process.myPid(), 7);
        Os.umask(OsConstants.S_IRWXG | OsConstants.S_IRWXO);
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        String systemServerClasspath = Os.getenv("SYSTEMSERVERCLASSPATH");
        if (systemServerClasspath != null) {
            performSystemServerDexOpt(systemServerClasspath);
            if (SystemProperties.getBoolean("dalvik.vm.profilesystemserver", false) && (Build.IS_USERDEBUG || Build.IS_ENG)) {
                try {
                    prepareSystemServerProfile(systemServerClasspath);
                } catch (Exception e) {
                    Log.wtf(TAG, "Failed to set up system server profile", e);
                }
            }
        }
        if (parsedArgs.invokeWith != null) {
            String[] args = parsedArgs.remainingArgs;
            if (systemServerClasspath != null) {
                String[] amendedArgs = new String[(args.length + 2)];
                amendedArgs[0] = "-cp";
                amendedArgs[1] = systemServerClasspath;
                System.arraycopy(args, 0, amendedArgs, 2, args.length);
                args = amendedArgs;
            }
            WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion, VMRuntime.getCurrentInstructionSet(), null, args);
            throw new IllegalStateException("Unexpected return from WrapperInit.execApplication");
        }
        ClassLoader cl = null;
        if (systemServerClasspath != null) {
            cl = createPathClassLoader(systemServerClasspath, parsedArgs.targetSdkVersion);
            Thread.currentThread().setContextClassLoader(cl);
        }
        return zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, cl);
    }

    private static void prepareSystemServerProfile(String systemServerClasspath) throws RemoteException {
        if (!systemServerClasspath.isEmpty()) {
            String[] codePaths = systemServerClasspath.split(":");
            String systemServerPackageName = "android";
            String systemServerProfileName = "primary.prof";
            Stub.asInterface(ServiceManager.getService("installd")).prepareAppProfile(systemServerPackageName, 0, UserHandle.getAppId(1000), systemServerProfileName, codePaths[0], null);
            VMRuntime.registerAppInfo(new File(Environment.getDataProfilesDePackageDirectory(0, systemServerPackageName), systemServerProfileName).getAbsolutePath(), codePaths);
        }
    }

    public static void setApiBlacklistExemptions(String[] exemptions) {
        VMRuntime.getRuntime().setHiddenApiExemptions(exemptions);
    }

    public static void setHiddenApiAccessLogSampleRate(int percent) {
        VMRuntime.getRuntime().setHiddenApiAccessLogSamplingRate(percent);
    }

    static ClassLoader createPathClassLoader(String classPath, int targetSdkVersion) {
        String libraryPath = System.getProperty("java.library.path");
        return ClassLoaderFactory.createClassLoader(classPath, libraryPath, libraryPath, ClassLoader.getSystemClassLoader(), targetSdkVersion, true, null);
    }

    private static void performSystemServerDexOpt(String classPath) {
        String classPathElement;
        String classPathForElement;
        int i;
        int i2;
        StringBuilder stringBuilder;
        String classPathForElement2;
        Exception e;
        String[] classPathElements = classPath.split(":");
        IInstalld installd = Stub.asInterface(ServiceManager.getService("installd"));
        String instructionSet = VMRuntime.getRuntime().vmInstructionSet();
        int length = classPathElements.length;
        String classPathForElement3 = "";
        int i3 = 0;
        while (i3 < length) {
            int dexoptNeeded;
            String classPathElement2 = classPathElements[i3];
            String systemServerFilter = SystemProperties.get("dalvik.vm.systemservercompilerfilter", "speed");
            try {
                dexoptNeeded = DexFile.getDexOptNeeded(classPathElement2, instructionSet, systemServerFilter, null, false, false);
            } catch (FileNotFoundException ignored) {
                classPathElement = classPathElement2;
                classPathForElement = classPathForElement3;
                i = i3;
                i2 = length;
                FileNotFoundException fileNotFoundException = ignored;
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Missing classpath element for system server: ");
                stringBuilder2.append(classPathElement);
                Log.w(str, stringBuilder2.toString());
                classPathForElement3 = classPathForElement;
            } catch (IOException e2) {
                IOException iOException = e2;
                classPathForElement = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error checking classpath element for system server: ");
                stringBuilder.append(classPathElement2);
                Log.w(classPathForElement, stringBuilder.toString(), e2);
                dexoptNeeded = 0;
            }
            int dexoptNeeded2 = dexoptNeeded;
            if (dexoptNeeded2 != 0) {
                String packageName = PhoneConstants.APN_TYPE_ALL;
                int targetSdkVersion = 0;
                String classPathElement3;
                try {
                    classPathElement3 = classPathElement2;
                    classPathForElement2 = classPathForElement3;
                    i = i3;
                    i2 = length;
                    try {
                        installd.dexopt(classPathElement2, 1000, PhoneConstants.APN_TYPE_ALL, instructionSet, dexoptNeeded2, null, 0, systemServerFilter, StorageManager.UUID_PRIVATE_INTERNAL, getSystemServerClassLoaderContext(classPathForElement3), null, false, 0, null, null, "server-dexopt");
                        classPathElement = classPathElement3;
                    } catch (RemoteException | ServiceSpecificException e3) {
                        e = e3;
                        classPathForElement = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed compiling classpath element for system server: ");
                        classPathElement = classPathElement3;
                        stringBuilder.append(classPathElement);
                        Log.w(classPathForElement, stringBuilder.toString(), e);
                        classPathForElement3 = encodeSystemServerClassPath(classPathForElement2, classPathElement);
                        i3 = i + 1;
                        length = i2;
                    }
                } catch (RemoteException | ServiceSpecificException e4) {
                    e = e4;
                    classPathElement3 = classPathElement2;
                    classPathForElement2 = classPathForElement3;
                    i = i3;
                    i2 = length;
                    classPathForElement = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed compiling classpath element for system server: ");
                    classPathElement = classPathElement3;
                    stringBuilder.append(classPathElement);
                    Log.w(classPathForElement, stringBuilder.toString(), e);
                    classPathForElement3 = encodeSystemServerClassPath(classPathForElement2, classPathElement);
                    i3 = i + 1;
                    length = i2;
                }
            } else {
                classPathElement = classPathElement2;
                classPathForElement2 = classPathForElement3;
                i = i3;
                i2 = length;
            }
            classPathForElement3 = encodeSystemServerClassPath(classPathForElement2, classPathElement);
            i3 = i + 1;
            length = i2;
        }
        classPathForElement = classPathForElement3;
    }

    private static String getSystemServerClassLoaderContext(String classPath) {
        if (classPath == null) {
            return "PCL[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PCL[");
        stringBuilder.append(classPath);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    private static String encodeSystemServerClassPath(String classPath, String newElement) {
        if (classPath == null || classPath.isEmpty()) {
            return newElement;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(classPath);
        stringBuilder.append(":");
        stringBuilder.append(newElement);
        return stringBuilder.toString();
    }

    private static Runnable forkSystemServer(String abiList, String socketName, ZygoteServer zygoteServer) {
        IllegalArgumentException ex;
        StructCapUserHeader structCapUserHeader;
        long capabilities = posixCapabilitiesAsBits(OsConstants.CAP_IPC_LOCK, OsConstants.CAP_KILL, OsConstants.CAP_NET_ADMIN, OsConstants.CAP_NET_BIND_SERVICE, OsConstants.CAP_NET_BROADCAST, OsConstants.CAP_NET_RAW, OsConstants.CAP_SYS_MODULE, OsConstants.CAP_SYS_NICE, OsConstants.CAP_SYS_PTRACE, OsConstants.CAP_SYS_TIME, OsConstants.CAP_SYS_TTY_CONFIG, OsConstants.CAP_WAKE_ALARM, OsConstants.CAP_BLOCK_SUSPEND);
        StructCapUserHeader header = new StructCapUserHeader(OsConstants._LINUX_CAPABILITY_VERSION_3, 0);
        try {
            StructCapUserData[] data = Os.capget(header);
            long capabilities2 = ((((long) data[1].effective) << 32) | ((long) data[0].effective)) & capabilities;
            String[] strArr = new String[8];
            strArr[0] = "--setuid=1000";
            strArr[1] = "--setgid=1000";
            strArr[2] = "--setgroups=1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1018,1021,1023,1024,1032,1065,3001,3002,3003,3006,3007,3009,3010,3011";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--capabilities=");
            stringBuilder.append(capabilities2);
            stringBuilder.append(",");
            stringBuilder.append(capabilities2);
            strArr[3] = stringBuilder.toString();
            strArr[4] = "--nice-name=system_server";
            strArr[5] = "--runtime-args";
            strArr[6] = "--target-sdk-version=10000";
            strArr[7] = "com.android.server.SystemServer";
            Arguments parsedArgs = null;
            try {
                parsedArgs = new Arguments(strArr);
                ZygoteConnection.applyDebuggerSystemProperty(parsedArgs);
                ZygoteConnection.applyInvokeWithSystemProperty(parsedArgs);
                if (SystemProperties.getBoolean("dalvik.vm.profilesystemserver", false)) {
                    try {
                        parsedArgs.runtimeFlags |= 16384;
                    } catch (IllegalArgumentException e) {
                        ex = e;
                        structCapUserHeader = header;
                    }
                }
                try {
                    if (Zygote.forkSystemServer(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids, parsedArgs.runtimeFlags, null, parsedArgs.permittedCapabilities, parsedArgs.effectiveCapabilities) != 0) {
                        return null;
                    }
                    if (hasSecondZygote(abiList)) {
                        waitForSecondaryZygote(socketName);
                    }
                    zygoteServer.closeServerSocket();
                    return handleSystemServerProcess(parsedArgs);
                } catch (IllegalArgumentException e2) {
                    ex = e2;
                }
            } catch (IllegalArgumentException e3) {
                ex = e3;
                structCapUserHeader = header;
                throw new RuntimeException(ex);
            }
        } catch (ErrnoException ex2) {
            structCapUserHeader = header;
            header = ex2;
            throw new RuntimeException("Failed to capget()", ex2);
        }
    }

    private static long posixCapabilitiesAsBits(int... capabilities) {
        long result = 0;
        for (int capability : capabilities) {
            if (capability < 0 || capability > OsConstants.CAP_LAST_CAP) {
                throw new IllegalArgumentException(String.valueOf(capability));
            }
            result |= 1 << capability;
        }
        return result;
    }

    public static void main(String[] argv) {
        ZygoteServer zygoteServer = new ZygoteServer();
        ZygoteHooks.startZygoteNoThreadCreation();
        try {
            Os.setpgid(0, 0);
            HwFrameworkFactory.getLogException().initLogBlackList();
            try {
                StringBuilder stringBuilder;
                if (!"1".equals(SystemProperties.get("sys.boot_completed"))) {
                    MetricsLogger.histogram(null, "boot_zygote_init", (int) SystemClock.elapsedRealtime());
                }
                TimingsTraceLog bootTimingsTraceLog = new TimingsTraceLog(Process.is64Bit() ? "Zygote64Timing" : "Zygote32Timing", 16384);
                bootTimingsTraceLog.traceBegin("ZygoteInit");
                int myPriority = Process.getThreadPriority(Process.myPid());
                Process.setThreadPriority(-19);
                RuntimeInit.enableDdms();
                String socketName = "zygote";
                String abiList = null;
                boolean enableLazyPreload = false;
                boolean startSystemServer = false;
                for (int i = 1; i < argv.length; i++) {
                    if ("start-system-server".equals(argv[i])) {
                        startSystemServer = true;
                        isPrimaryCpuAbi = true;
                    } else if ("--enable-lazy-preload".equals(argv[i])) {
                        enableLazyPreload = true;
                    } else if (argv[i].startsWith(ABI_LIST_ARG)) {
                        abiList = argv[i].substring(ABI_LIST_ARG.length());
                    } else if (argv[i].startsWith(SOCKET_NAME_ARG)) {
                        socketName = argv[i].substring(SOCKET_NAME_ARG.length());
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown command line argument: ");
                        stringBuilder.append(argv[i]);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                }
                if (abiList != null) {
                    Runnable r;
                    zygoteServer.registerServerSocketFromEnv(socketName);
                    if (enableLazyPreload) {
                        Zygote.resetNicePriority();
                    } else {
                        bootTimingsTraceLog.traceBegin("ZygotePreload");
                        EventLog.writeEvent(3020, SystemClock.uptimeMillis());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("JL_BOOT_PROGRESS_PRELOAD_START:");
                        stringBuilder.append(argv[0]);
                        Jlog.d(28, stringBuilder.toString());
                        Log.initHWLog();
                        preload(bootTimingsTraceLog);
                        EventLog.writeEvent(LOG_BOOT_PROGRESS_PRELOAD_END, SystemClock.uptimeMillis());
                        bootTimingsTraceLog.traceEnd();
                        Jlog.d(29, "JL_BOOT_PROGRESS_PRELOAD_END");
                    }
                    bootTimingsTraceLog.traceBegin("PostZygoteInitGC");
                    gcAndFinalize();
                    bootTimingsTraceLog.traceEnd();
                    bootTimingsTraceLog.traceEnd();
                    Trace.setTracingEnabled(false, 0);
                    Zygote.nativeSecurityInit();
                    Process.setThreadPriority(myPriority);
                    Zygote.nativeUnmountStorageOnInit();
                    ZygoteHooks.stopZygoteNoThreadCreation();
                    if (startSystemServer) {
                        r = forkSystemServer(abiList, socketName, zygoteServer);
                        if (r != null) {
                            r.run();
                            zygoteServer.closeServerSocket();
                            return;
                        }
                    }
                    Log.i(TAG, "Accepting command socket connections");
                    r = zygoteServer.runSelectLoop(abiList);
                    zygoteServer.closeServerSocket();
                    if (r != null) {
                        r.run();
                    }
                    return;
                }
                throw new RuntimeException("No ABI list supplied.");
            } catch (Throwable th) {
                zygoteServer.closeServerSocket();
            }
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }
    }

    private static boolean hasSecondZygote(String abiList) {
        return SystemProperties.get("ro.product.cpu.abilist").equals(abiList) ^ 1;
    }

    private static void waitForSecondaryZygote(String socketName) {
        ZygoteProcess.waitForConnectionToZygote("zygote".equals(socketName) ? "zygote_secondary" : "zygote");
    }

    static boolean isPreloadComplete() {
        return sPreloadComplete;
    }

    private ZygoteInit() {
    }

    public static final Runnable zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
        Trace.traceBegin(64, "ZygoteInit");
        RuntimeInit.redirectLogStreams();
        RuntimeInit.commonInit();
        nativeZygoteInit();
        return RuntimeInit.applicationInit(targetSdkVersion, argv, classLoader);
    }

    static final Runnable childZygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
        Arguments args = new Arguments(argv);
        return RuntimeInit.findStaticMain(args.startClass, args.startArgs, classLoader);
    }
}

package com.android.internal.os;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport.ParcelableCrashInfo;
import android.ddm.DdmRegister;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.DeadObjectException;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.AndroidConfig;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.VMRuntime;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.LogManager;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public class RuntimeInit {
    static final boolean DEBUG = false;
    static final String TAG = "AndroidRuntime";
    private static boolean initialized;
    private static IBinder mApplicationObject;
    private static volatile boolean mCrashing = false;

    static class Arguments {
        String[] startArgs;
        String startClass;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int curArg = 0;
            while (curArg < args.length) {
                String arg = args[curArg];
                if (arg.equals("--")) {
                    curArg++;
                    break;
                } else if (!arg.startsWith("--")) {
                    break;
                } else {
                    curArg++;
                }
            }
            if (curArg != args.length) {
                int curArg2 = curArg + 1;
                this.startClass = args[curArg];
                this.startArgs = new String[(args.length - curArg2)];
                System.arraycopy(args, curArg2, this.startArgs, 0, this.startArgs.length);
                return;
            }
            throw new IllegalArgumentException("Missing classname argument to RuntimeInit!");
        }
    }

    private static class KillApplicationHandler implements UncaughtExceptionHandler {
        private final LoggingHandler mLoggingHandler;

        public KillApplicationHandler(LoggingHandler loggingHandler) {
            this.mLoggingHandler = (LoggingHandler) Objects.requireNonNull(loggingHandler);
        }

        public void uncaughtException(Thread t, Throwable e) {
            try {
                ensureLogging(t, e);
                if (RuntimeInit.mCrashing) {
                    Process.killProcess(Process.myPid());
                    System.exit(10);
                    return;
                }
                RuntimeInit.mCrashing = true;
                if (ActivityThread.currentActivityThread() != null) {
                    ActivityThread.currentActivityThread().stopProfiling();
                }
                ActivityManager.getService().handleApplicationCrash(RuntimeInit.mApplicationObject, new ParcelableCrashInfo(e));
                Process.killProcess(Process.myPid());
                System.exit(10);
            } catch (Throwable th) {
                Slog.e(RuntimeInit.TAG, "Even Clog_e() fails! in function KillApplicationHandler");
            }
        }

        private void ensureLogging(Thread t, Throwable e) {
            if (!this.mLoggingHandler.mTriggered) {
                try {
                    this.mLoggingHandler.uncaughtException(t, e);
                } catch (Throwable th) {
                }
            }
        }
    }

    private static class LoggingHandler implements UncaughtExceptionHandler {
        public volatile boolean mTriggered;

        private LoggingHandler() {
            this.mTriggered = false;
        }

        /* synthetic */ LoggingHandler(AnonymousClass1 x0) {
            this();
        }

        public void uncaughtException(Thread t, Throwable e) {
            this.mTriggered = true;
            if (!RuntimeInit.mCrashing) {
                if (RuntimeInit.mApplicationObject == null && 1000 == Process.myUid()) {
                    String str = RuntimeInit.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("*** FATAL EXCEPTION IN SYSTEM PROCESS: ");
                    stringBuilder.append(t.getName());
                    RuntimeInit.Clog_e(str, stringBuilder.toString(), e);
                    ExitCatch.disable(Process.myPid());
                } else {
                    StringBuilder message = new StringBuilder();
                    message.append("FATAL EXCEPTION: ");
                    message.append(t.getName());
                    message.append("\n");
                    String processName = ActivityThread.currentProcessName();
                    if (processName != null) {
                        message.append("Process: ");
                        message.append(processName);
                        message.append(", ");
                    }
                    message.append("PID: ");
                    message.append(Process.myPid());
                    if (Thread.getDefaultUncaughtExceptionHandler() == null || !(Thread.getDefaultUncaughtExceptionHandler() instanceof KillApplicationHandler)) {
                        try {
                            ParcelableCrashInfo crashInfo = new ParcelableCrashInfo(e);
                            StringBuilder stringBuilder2;
                            if (Process.myPid() == Process.myTid()) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(crashInfo.stackTrace);
                                stringBuilder2.append("-mainthread -loghandler");
                                crashInfo.stackTrace = stringBuilder2.toString();
                            } else {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(crashInfo.stackTrace);
                                stringBuilder2.append("-loghandler");
                                crashInfo.stackTrace = stringBuilder2.toString();
                            }
                            ActivityManager.getService().handleApplicationCrash(RuntimeInit.mApplicationObject, crashInfo);
                        } catch (Throwable th) {
                        }
                    }
                    RuntimeInit.Clog_e(RuntimeInit.TAG, message.toString(), e);
                }
            }
        }
    }

    static class MethodAndArgsCaller implements Runnable {
        private final String[] mArgs;
        private final Method mMethod;

        public MethodAndArgsCaller(Method method, String[] args) {
            this.mMethod = method;
            this.mArgs = args;
        }

        public void run() {
            try {
                this.mMethod.invoke(null, new Object[]{this.mArgs});
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex2) {
                Throwable cause = ex2.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                } else if (cause instanceof Error) {
                    throw ((Error) cause);
                } else {
                    throw new RuntimeException(ex2);
                }
            }
        }
    }

    private static final native void nativeFinishInit();

    private static final native void nativeSetExitWithoutCleanup(boolean z);

    private static int Clog_e(String tag, String msg, Throwable tr) {
        return Log.printlns(4, 6, tag, msg, tr);
    }

    protected static final void commonInit() {
        LoggingHandler loggingHandler = new LoggingHandler();
        Thread.setUncaughtExceptionPreHandler(loggingHandler);
        Thread.setDefaultUncaughtExceptionHandler(new KillApplicationHandler(loggingHandler));
        TimezoneGetter.setInstance(new TimezoneGetter() {
            public String getId() {
                return SystemProperties.get("persist.sys.timezone");
            }
        });
        TimeZone.setDefault(null);
        LogManager.getLogManager().reset();
        AndroidConfig androidConfig = new AndroidConfig();
        System.setProperty("http.agent", getDefaultUserAgent());
        NetworkManagementSocketTagger.install();
        if (SystemProperties.get("ro.kernel.android.tracing").equals("1")) {
            Slog.i(TAG, "NOTE: emulator trace profiling enabled");
            Debug.enableEmulatorTraceOutput();
        }
        initialized = true;
    }

    private static String getDefaultUserAgent() {
        String model;
        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version"));
        result.append(" (Linux; U; Android ");
        String version = VERSION.RELEASE;
        result.append(version.length() > 0 ? version : "1.0");
        if ("REL".equals(VERSION.CODENAME)) {
            model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }
        model = Build.ID;
        if (model.length() > 0) {
            result.append(" Build/");
            result.append(model);
        }
        result.append(")");
        return result.toString();
    }

    protected static Runnable findStaticMain(String className, String[] argv, ClassLoader classLoader) {
        StringBuilder stringBuilder;
        try {
            try {
                Method m = Class.forName(className, true, classLoader).getMethod("main", new Class[]{String[].class});
                int modifiers = m.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    return new MethodAndArgsCaller(m, argv);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Main method is not public and static on ");
                stringBuilder2.append(className);
                throw new RuntimeException(stringBuilder2.toString());
            } catch (NoSuchMethodException ex) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Missing static main on ");
                stringBuilder.append(className);
                throw new RuntimeException(stringBuilder.toString(), ex);
            } catch (SecurityException ex2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Problem getting static main on ");
                stringBuilder.append(className);
                throw new RuntimeException(stringBuilder.toString(), ex2);
            }
        } catch (ClassNotFoundException ex3) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Missing class when invoking static main ");
            stringBuilder3.append(className);
            throw new RuntimeException(stringBuilder3.toString(), ex3);
        }
    }

    public static final void main(String[] argv) {
        enableDdms();
        if (argv.length == 2 && argv[1].equals("application")) {
            redirectLogStreams();
        }
        commonInit();
        nativeFinishInit();
    }

    protected static Runnable applicationInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) {
        nativeSetExitWithoutCleanup(true);
        VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);
        Arguments args = new Arguments(argv);
        Trace.traceEnd(64);
        return findStaticMain(args.startClass, args.startArgs, classLoader);
    }

    public static void redirectLogStreams() {
        System.out.close();
        System.setOut(new AndroidPrintStream(4, "System.out"));
        System.err.close();
        System.setErr(new AndroidPrintStream(5, "System.err"));
    }

    public static void wtf(String tag, Throwable t, boolean system) {
        try {
            if (ActivityManager.getService().handleApplicationWtf(mApplicationObject, tag, system, new ParcelableCrashInfo(t))) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        } catch (Throwable t2) {
            if (!(t2 instanceof DeadObjectException)) {
                Slog.e(TAG, "Error reporting WTF", t2);
                Slog.e(TAG, "Original WTF:", t);
            }
        }
    }

    public static final void setApplicationObject(IBinder app) {
        mApplicationObject = app;
    }

    public static final IBinder getApplicationObject() {
        return mApplicationObject;
    }

    static final void enableDdms() {
        DdmRegister.registerHandlers();
    }
}

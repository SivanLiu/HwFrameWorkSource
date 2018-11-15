package com.android.commands.monkey;

import android.app.ActivityManager;
import android.app.IActivityController.Stub;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Monkey {
    private static final int DEBUG_ALLOW_ANY_RESTARTS = 0;
    private static final int DEBUG_ALLOW_ANY_STARTS = 0;
    private static int NUM_READ_TOMBSTONE_RETRIES = 5;
    private static final File TOMBSTONES_PATH = new File("/data/tombstones");
    private static final String TOMBSTONE_PREFIX = "tombstone_";
    public static Intent currentIntent;
    public static String currentPackage;
    private boolean mAbort;
    private IActivityManager mAm;
    private String[] mArgs;
    private long mBugreportFrequency = 10;
    int mCount = 1000;
    private boolean mCountEvents = true;
    private String mCurArgData;
    long mDeviceSleepTime = 30000;
    long mDroppedFlipEvents = 0;
    long mDroppedKeyEvents = 0;
    long mDroppedPointerEvents = 0;
    long mDroppedRotationEvents = 0;
    long mDroppedTrackballEvents = 0;
    MonkeyEventSource mEventSource;
    float[] mFactors = new float[12];
    private boolean mGenerateHprof;
    private boolean mGetPeriodicBugreport = false;
    private boolean mIgnoreCrashes;
    private boolean mIgnoreNativeCrashes;
    private boolean mIgnoreSecurityExceptions;
    private boolean mIgnoreTimeouts;
    private boolean mKillProcessAfterError;
    private ArrayList<ComponentName> mMainApps = new ArrayList();
    private ArrayList<String> mMainCategories = new ArrayList();
    private String mMatchDescription;
    private boolean mMonitorNativeCrashes;
    private MonkeyNetworkMonitor mNetworkMonitor = new MonkeyNetworkMonitor();
    private int mNextArg;
    private boolean mPermissionTargetSystem = false;
    private String mPkgBlacklistFile;
    private String mPkgWhitelistFile;
    private IPackageManager mPm;
    long mProfileWaitTime = 5000;
    Random mRandom = null;
    boolean mRandomizeScript = false;
    boolean mRandomizeThrottle = false;
    private String mReportProcessName;
    private boolean mRequestAnrBugreport = false;
    private boolean mRequestAnrTraces = false;
    private boolean mRequestAppCrashBugreport = false;
    private boolean mRequestBugreport = false;
    private boolean mRequestDumpsysMemInfo = false;
    private boolean mRequestPeriodicBugreport = false;
    private boolean mRequestProcRank = false;
    private boolean mRequestWatchdogBugreport = false;
    private ArrayList<String> mScriptFileNames = new ArrayList();
    boolean mScriptLog = false;
    long mSeed = 0;
    private boolean mSendNoEvents;
    private int mServerPort = -1;
    private String mSetupFileName = null;
    long mThrottle = 0;
    private HashSet<Long> mTombstones = null;
    private int mVerbose;
    private boolean mWatchdogWaiting = false;
    private IWindowManager mWm;

    private class ActivityController extends Stub {
        private ActivityController() {
        }

        public boolean activityStarting(Intent intent, String pkg) {
            boolean allow = isActivityStartingAllowed(intent, pkg);
            if (Monkey.this.mVerbose > 0) {
                ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
                Logger logger = Logger.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    // ");
                stringBuilder.append(allow ? "Allowing" : "Rejecting");
                stringBuilder.append(" start of ");
                stringBuilder.append(intent);
                stringBuilder.append(" in package ");
                stringBuilder.append(pkg);
                logger.println(stringBuilder.toString());
                StrictMode.setThreadPolicy(savedPolicy);
            }
            Monkey.currentPackage = pkg;
            Monkey.currentIntent = intent;
            return allow;
        }

        private boolean isActivityStartingAllowed(Intent intent, String pkg) {
            if (MonkeyUtils.getPackageFilter().checkEnteringPackage(pkg)) {
                return true;
            }
            Set<String> categories = intent.getCategories();
            if (intent.getAction() == "android.intent.action.MAIN" && categories != null && categories.contains("android.intent.category.HOME")) {
                try {
                    if (pkg.equals(Monkey.this.mPm.resolveIntent(intent, intent.getType(), 0, UserHandle.myUserId()).activityInfo.packageName)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Logger.err.println("** Failed talking with package manager!");
                    return false;
                }
            }
            return false;
        }

        public boolean activityResuming(String pkg) {
            ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger logger = Logger.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    // activityResuming(");
            stringBuilder.append(pkg);
            stringBuilder.append(")");
            logger.println(stringBuilder.toString());
            boolean allow = MonkeyUtils.getPackageFilter().checkEnteringPackage(pkg);
            if (!allow && Monkey.this.mVerbose > 0) {
                Logger logger2 = Logger.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    // ");
                stringBuilder2.append(allow ? "Allowing" : "Rejecting");
                stringBuilder2.append(" resume of package ");
                stringBuilder2.append(pkg);
                logger2.println(stringBuilder2.toString());
            }
            Monkey.currentPackage = pkg;
            StrictMode.setThreadPolicy(savedPolicy);
            return allow;
        }

        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
            ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("// CRASH: ");
            stringBuilder.append(processName);
            stringBuilder.append(" (pid ");
            stringBuilder.append(pid);
            stringBuilder.append(")");
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// Short Msg: ");
            stringBuilder.append(shortMsg);
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// Long Msg: ");
            stringBuilder.append(longMsg);
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// Build Label: ");
            stringBuilder.append(Build.FINGERPRINT);
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// Build Changelist: ");
            stringBuilder.append(VERSION.INCREMENTAL);
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// Build Time: ");
            stringBuilder.append(Build.TIME);
            logger.println(stringBuilder.toString());
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("// ");
            stringBuilder.append(stackTrace.replace("\n", "\n// "));
            logger.println(stringBuilder.toString());
            StrictMode.setThreadPolicy(savedPolicy);
            if ((Monkey.this.mMatchDescription != null && !shortMsg.contains(Monkey.this.mMatchDescription) && !longMsg.contains(Monkey.this.mMatchDescription) && !stackTrace.contains(Monkey.this.mMatchDescription)) || (Monkey.this.mIgnoreCrashes && !Monkey.this.mRequestBugreport)) {
                return false;
            }
            synchronized (Monkey.this) {
                if (!Monkey.this.mIgnoreCrashes) {
                    Monkey.this.mAbort = true;
                }
                if (Monkey.this.mRequestBugreport) {
                    Monkey.this.mRequestAppCrashBugreport = true;
                    Monkey.this.mReportProcessName = processName;
                }
            }
            return Monkey.this.mKillProcessAfterError ^ true;
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) {
            return 0;
        }

        public int appNotResponding(String processName, int pid, String processStats) {
            ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("// NOT RESPONDING: ");
            stringBuilder.append(processName);
            stringBuilder.append(" (pid ");
            stringBuilder.append(pid);
            stringBuilder.append(")");
            logger.println(stringBuilder.toString());
            Logger.err.println(processStats);
            StrictMode.setThreadPolicy(savedPolicy);
            if (Monkey.this.mMatchDescription == null || processStats.contains(Monkey.this.mMatchDescription)) {
                synchronized (Monkey.this) {
                    Monkey.this.mRequestAnrTraces = true;
                    Monkey.this.mRequestDumpsysMemInfo = true;
                    Monkey.this.mRequestProcRank = true;
                    if (Monkey.this.mRequestBugreport) {
                        Monkey.this.mRequestAnrBugreport = true;
                        Monkey.this.mReportProcessName = processName;
                    }
                }
                if (!Monkey.this.mIgnoreTimeouts) {
                    synchronized (Monkey.this) {
                        Monkey.this.mAbort = true;
                    }
                }
            }
            if (Monkey.this.mKillProcessAfterError) {
                return -1;
            }
            return 1;
        }

        public int systemNotResponding(String message) {
            ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("// WATCHDOG: ");
            stringBuilder.append(message);
            logger.println(stringBuilder.toString());
            StrictMode.setThreadPolicy(savedPolicy);
            synchronized (Monkey.this) {
                if (Monkey.this.mMatchDescription == null || message.contains(Monkey.this.mMatchDescription)) {
                    if (!Monkey.this.mIgnoreCrashes) {
                        Monkey.this.mAbort = true;
                    }
                    if (Monkey.this.mRequestBugreport) {
                        Monkey.this.mRequestWatchdogBugreport = true;
                    }
                }
                Monkey.this.mWatchdogWaiting = true;
            }
            synchronized (Monkey.this) {
                while (Monkey.this.mWatchdogWaiting) {
                    try {
                        Monkey.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (Monkey.this.mKillProcessAfterError) {
                return -1;
            }
            return 1;
        }
    }

    private void reportProcRank() {
        commandLineReport("procrank", "procrank");
    }

    private void reportAnrTraces() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        File[] recentTraces = new File("/data/anr/").listFiles();
        if (recentTraces != null) {
            File mostRecent = null;
            long mostRecentMtime = 0;
            for (File trace : recentTraces) {
                long mtime = trace.lastModified();
                if (mtime > mostRecentMtime) {
                    mostRecentMtime = mtime;
                    mostRecent = trace;
                }
            }
            if (mostRecent != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cat ");
                stringBuilder.append(mostRecent.getAbsolutePath());
                commandLineReport("anr traces", stringBuilder.toString());
            }
        }
    }

    private void reportDumpsysMemInfo() {
        commandLineReport("meminfo", "dumpsys meminfo");
    }

    private void commandLineReport(String reportName, String command) {
        Logger logger = Logger.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(reportName);
        stringBuilder.append(":");
        logger.println(stringBuilder.toString());
        Runtime rt = Runtime.getRuntime();
        Writer logOutput = null;
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (this.mRequestBugreport) {
                logOutput = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), reportName), true));
            }
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (true) {
                String readLine = inBuffer.readLine();
                String s = readLine;
                if (readLine == null) {
                    break;
                } else if (this.mRequestBugreport) {
                    try {
                        logOutput.write(s);
                        logOutput.write("\n");
                    } catch (IOException e) {
                        while (inBuffer.readLine() != null) {
                        }
                        Logger.err.println(e.toString());
                    }
                } else {
                    Logger.err.println(s);
                }
            }
            int status = p.waitFor();
            Logger logger2 = Logger.err;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("// ");
            stringBuilder2.append(reportName);
            stringBuilder2.append(" status was ");
            stringBuilder2.append(status);
            logger2.println(stringBuilder2.toString());
            if (logOutput != null) {
                logOutput.close();
            }
        } catch (Exception e2) {
            Logger logger3 = Logger.err;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("// Exception from ");
            stringBuilder3.append(reportName);
            stringBuilder3.append(":");
            logger3.println(stringBuilder3.toString());
            Logger.err.println(e2.toString());
        }
    }

    private void writeScriptLog(int count) {
        try {
            Writer output = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), "scriptlog.txt"), true));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iteration: ");
            stringBuilder.append(count);
            stringBuilder.append(" time: ");
            stringBuilder.append(MonkeyUtils.toCalendarTime(System.currentTimeMillis()));
            stringBuilder.append("\n");
            output.write(stringBuilder.toString());
            output.close();
        } catch (IOException e) {
            Logger.err.println(e.toString());
        }
    }

    private void getBugreport(String reportName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(reportName);
        stringBuilder.append(MonkeyUtils.toCalendarTime(System.currentTimeMillis()));
        String bugreportName = stringBuilder.toString().replaceAll("[ ,:]", "_");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(bugreportName);
        stringBuilder2.append(".txt");
        commandLineReport(stringBuilder2.toString(), "bugreport");
    }

    public static void main(String[] args) {
        Process.setArgV0("com.android.commands.monkey");
        Logger logger = Logger.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("args: ");
        stringBuilder.append(Arrays.toString(args));
        logger.println(stringBuilder.toString());
        System.exit(new Monkey().run(args));
    }

    private int run(String[] args) {
        int i;
        int i2;
        int runMonkeyCycles;
        String[] strArr = args;
        for (String s : strArr) {
            if ("--wait-dbg".equals(s)) {
                Debug.waitForDebugger();
            }
        }
        this.mVerbose = 0;
        this.mCount = 1000;
        this.mSeed = 0;
        this.mThrottle = 0;
        this.mArgs = strArr;
        for (String a : strArr) {
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" arg: \"");
            stringBuilder.append(a);
            stringBuilder.append("\"");
            logger.println(stringBuilder.toString());
        }
        this.mNextArg = 0;
        for (i2 = 0; i2 < 12; i2++) {
            this.mFactors[i2] = 1.0f;
        }
        if (!processOptions() || !loadPackageLists()) {
            return -1;
        }
        Logger logger2;
        if (this.mMainCategories.size() == 0) {
            this.mMainCategories.add("android.intent.category.LAUNCHER");
            this.mMainCategories.add("android.intent.category.MONKEY");
        }
        if (this.mSeed == 0) {
            this.mSeed = System.currentTimeMillis() + ((long) System.identityHashCode(this));
        }
        if (this.mVerbose > 0) {
            logger2 = Logger.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(":Monkey: seed=");
            stringBuilder2.append(this.mSeed);
            stringBuilder2.append(" count=");
            stringBuilder2.append(this.mCount);
            logger2.println(stringBuilder2.toString());
            MonkeyUtils.getPackageFilter().dump();
            if (this.mMainCategories.size() != 0) {
                Iterator<String> it = this.mMainCategories.iterator();
                while (it.hasNext()) {
                    Logger logger3 = Logger.out;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(":IncludeCategory: ");
                    stringBuilder3.append((String) it.next());
                    logger3.println(stringBuilder3.toString());
                }
            }
        }
        if (!checkInternalConfiguration()) {
            return -2;
        }
        if (!getSystemInterfaces()) {
            return -3;
        }
        if (!getMainApps()) {
            return -4;
        }
        StringBuilder stringBuilder4;
        this.mRandom = new Random(this.mSeed);
        if (this.mScriptFileNames != null && this.mScriptFileNames.size() == 1) {
            this.mEventSource = new MonkeySourceScript(this.mRandom, (String) this.mScriptFileNames.get(0), this.mThrottle, this.mRandomizeThrottle, this.mProfileWaitTime, this.mDeviceSleepTime);
            this.mEventSource.setVerbose(this.mVerbose);
            this.mCountEvents = false;
        } else if (this.mScriptFileNames != null && this.mScriptFileNames.size() > 1) {
            if (this.mSetupFileName != null) {
                this.mEventSource = new MonkeySourceRandomScript(this.mSetupFileName, this.mScriptFileNames, this.mThrottle, this.mRandomizeThrottle, this.mRandom, this.mProfileWaitTime, this.mDeviceSleepTime, this.mRandomizeScript);
                this.mCount++;
            } else {
                this.mEventSource = new MonkeySourceRandomScript(this.mScriptFileNames, this.mThrottle, this.mRandomizeThrottle, this.mRandom, this.mProfileWaitTime, this.mDeviceSleepTime, this.mRandomizeScript);
            }
            this.mEventSource.setVerbose(this.mVerbose);
            this.mCountEvents = false;
        } else if (this.mServerPort != -1) {
            try {
                this.mEventSource = new MonkeySourceNetwork(this.mServerPort);
                this.mCount = Integer.MAX_VALUE;
            } catch (IOException e) {
                Logger.out.println("Error binding to network socket.");
                return -5;
            }
        } else {
            if (this.mVerbose >= 2) {
                logger2 = Logger.out;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("// Seeded: ");
                stringBuilder4.append(this.mSeed);
                logger2.println(stringBuilder4.toString());
            }
            this.mEventSource = new MonkeySourceRandom(this.mRandom, this.mMainApps, this.mThrottle, this.mRandomizeThrottle, this.mPermissionTargetSystem);
            this.mEventSource.setVerbose(this.mVerbose);
            for (i2 = 0; i2 < 12; i2++) {
                if (this.mFactors[i2] <= 0.0f) {
                    ((MonkeySourceRandom) this.mEventSource).setFactors(i2, this.mFactors[i2]);
                }
            }
            ((MonkeySourceRandom) this.mEventSource).generateActivity();
        }
        if (!this.mEventSource.validate()) {
            return -5;
        }
        if (this.mGenerateHprof) {
            signalPersistentProcesses();
        }
        this.mNetworkMonitor.start();
        i = 0;
        try {
            runMonkeyCycles = runMonkeyCycles();
            new MonkeyRotationEvent(0, false).injectEvent(this.mWm, this.mAm, this.mVerbose);
            this.mNetworkMonitor.stop();
            synchronized (this) {
                StringBuilder stringBuilder5;
                if (this.mRequestAnrTraces) {
                    reportAnrTraces();
                    this.mRequestAnrTraces = false;
                }
                if (this.mRequestAnrBugreport) {
                    Logger.out.println("Print the anr report");
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("anr_");
                    stringBuilder5.append(this.mReportProcessName);
                    stringBuilder5.append("_");
                    getBugreport(stringBuilder5.toString());
                    this.mRequestAnrBugreport = false;
                }
                if (this.mRequestWatchdogBugreport) {
                    Logger.out.println("Print the watchdog report");
                    getBugreport("anr_watchdog_");
                    this.mRequestWatchdogBugreport = false;
                }
                if (this.mRequestAppCrashBugreport) {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("app_crash");
                    stringBuilder5.append(this.mReportProcessName);
                    stringBuilder5.append("_");
                    getBugreport(stringBuilder5.toString());
                    this.mRequestAppCrashBugreport = false;
                }
                if (this.mRequestDumpsysMemInfo) {
                    reportDumpsysMemInfo();
                    this.mRequestDumpsysMemInfo = false;
                }
                if (this.mRequestPeriodicBugreport) {
                    getBugreport("Bugreport_");
                    this.mRequestPeriodicBugreport = false;
                }
                if (this.mWatchdogWaiting) {
                    this.mWatchdogWaiting = false;
                    notifyAll();
                }
            }
            if (this.mGenerateHprof) {
                signalPersistentProcesses();
                if (this.mVerbose > 0) {
                    Logger.out.println("// Generated profiling reports in /data/misc");
                }
            }
            try {
                this.mAm.setActivityController(null, true);
                this.mNetworkMonitor.unregister(this.mAm);
            } catch (RemoteException e2) {
                if (runMonkeyCycles >= this.mCount) {
                    runMonkeyCycles = this.mCount - 1;
                }
            }
            if (this.mVerbose > 0) {
                logger2 = Logger.out;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(":Dropped: keys=");
                stringBuilder4.append(this.mDroppedKeyEvents);
                stringBuilder4.append(" pointers=");
                stringBuilder4.append(this.mDroppedPointerEvents);
                stringBuilder4.append(" trackballs=");
                stringBuilder4.append(this.mDroppedTrackballEvents);
                stringBuilder4.append(" flips=");
                stringBuilder4.append(this.mDroppedFlipEvents);
                stringBuilder4.append(" rotations=");
                stringBuilder4.append(this.mDroppedRotationEvents);
                logger2.println(stringBuilder4.toString());
            }
            this.mNetworkMonitor.dump();
            if (runMonkeyCycles < this.mCount - 1) {
                logger2 = Logger.err;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("** System appears to have crashed at event ");
                stringBuilder4.append(runMonkeyCycles);
                stringBuilder4.append(" of ");
                stringBuilder4.append(this.mCount);
                stringBuilder4.append(" using seed ");
                stringBuilder4.append(this.mSeed);
                logger2.println(stringBuilder4.toString());
                return runMonkeyCycles;
            }
            if (this.mVerbose > 0) {
                Logger.out.println("// Monkey finished");
            }
            return 0;
        } catch (Throwable th) {
            new MonkeyRotationEvent(0, false).injectEvent(this.mWm, this.mAm, this.mVerbose);
            Throwable th2 = th;
        }
    }

    private boolean processOptions() {
        if (this.mArgs.length < 1) {
            showUsage();
            return false;
        }
        Logger logger;
        StringBuilder stringBuilder;
        try {
            Set<String> validPackages = new HashSet();
            while (true) {
                String nextOption = nextOption();
                String opt = nextOption;
                if (nextOption == null) {
                    MonkeyUtils.getPackageFilter().addValidPackages(validPackages);
                    if (this.mServerPort == -1) {
                        String countStr = nextArg();
                        if (countStr == null) {
                            Logger.err.println("** Error: Count not specified");
                            showUsage();
                            return false;
                        }
                        try {
                            this.mCount = Integer.parseInt(countStr);
                        } catch (NumberFormatException e) {
                            Logger logger2 = Logger.err;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("** Error: Count is not a number: \"");
                            stringBuilder2.append(countStr);
                            stringBuilder2.append("\"");
                            logger2.println(stringBuilder2.toString());
                            showUsage();
                            return false;
                        }
                    }
                    return true;
                } else if (opt.equals("-s")) {
                    this.mSeed = nextOptionLong("Seed");
                } else if (opt.equals("-p")) {
                    validPackages.add(nextOptionData());
                } else if (opt.equals("-c")) {
                    this.mMainCategories.add(nextOptionData());
                } else if (opt.equals("-v")) {
                    this.mVerbose++;
                } else if (opt.equals("--ignore-crashes")) {
                    this.mIgnoreCrashes = true;
                } else if (opt.equals("--ignore-timeouts")) {
                    this.mIgnoreTimeouts = true;
                } else if (opt.equals("--ignore-security-exceptions")) {
                    this.mIgnoreSecurityExceptions = true;
                } else if (opt.equals("--monitor-native-crashes")) {
                    this.mMonitorNativeCrashes = true;
                } else if (opt.equals("--ignore-native-crashes")) {
                    this.mIgnoreNativeCrashes = true;
                } else if (opt.equals("--kill-process-after-error")) {
                    this.mKillProcessAfterError = true;
                } else if (opt.equals("--hprof")) {
                    this.mGenerateHprof = true;
                } else if (opt.equals("--match-description")) {
                    this.mMatchDescription = nextOptionData();
                } else if (opt.equals("--pct-touch")) {
                    this.mFactors[0] = (float) (-nextOptionLong("touch events percentage"));
                } else if (opt.equals("--pct-motion")) {
                    this.mFactors[1] = (float) (-nextOptionLong("motion events percentage"));
                } else if (opt.equals("--pct-trackball")) {
                    this.mFactors[3] = (float) (-nextOptionLong("trackball events percentage"));
                } else if (opt.equals("--pct-rotation")) {
                    this.mFactors[4] = (float) (-nextOptionLong("screen rotation events percentage"));
                } else if (opt.equals("--pct-syskeys")) {
                    this.mFactors[8] = (float) (-nextOptionLong("system (key) operations percentage"));
                } else if (opt.equals("--pct-nav")) {
                    this.mFactors[6] = (float) (-nextOptionLong("nav events percentage"));
                } else if (opt.equals("--pct-majornav")) {
                    this.mFactors[7] = (float) (-nextOptionLong("major nav events percentage"));
                } else if (opt.equals("--pct-appswitch")) {
                    this.mFactors[9] = (float) (-nextOptionLong("app switch events percentage"));
                } else if (opt.equals("--pct-flip")) {
                    this.mFactors[10] = (float) (-nextOptionLong("keyboard flip percentage"));
                } else if (opt.equals("--pct-anyevent")) {
                    this.mFactors[11] = (float) (-nextOptionLong("any events percentage"));
                } else if (opt.equals("--pct-pinchzoom")) {
                    this.mFactors[2] = (float) (-nextOptionLong("pinch zoom events percentage"));
                } else if (opt.equals("--pct-permission")) {
                    this.mFactors[5] = (float) (-nextOptionLong("runtime permission toggle events percentage"));
                } else if (opt.equals("--pkg-blacklist-file")) {
                    this.mPkgBlacklistFile = nextOptionData();
                } else if (opt.equals("--pkg-whitelist-file")) {
                    this.mPkgWhitelistFile = nextOptionData();
                } else if (opt.equals("--throttle")) {
                    this.mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                } else if (opt.equals("--randomize-throttle")) {
                    this.mRandomizeThrottle = true;
                } else if (!opt.equals("--wait-dbg")) {
                    if (opt.equals("--dbg-no-events")) {
                        this.mSendNoEvents = true;
                    } else if (opt.equals("--port")) {
                        this.mServerPort = (int) nextOptionLong("Server port to listen on for commands");
                    } else if (opt.equals("--setup")) {
                        this.mSetupFileName = nextOptionData();
                    } else if (opt.equals("-f")) {
                        this.mScriptFileNames.add(nextOptionData());
                    } else if (opt.equals("--profile-wait")) {
                        this.mProfileWaitTime = nextOptionLong("Profile delay (in milliseconds) to wait between user action");
                    } else if (opt.equals("--device-sleep-time")) {
                        this.mDeviceSleepTime = nextOptionLong("Device sleep time(in milliseconds)");
                    } else if (opt.equals("--randomize-script")) {
                        this.mRandomizeScript = true;
                    } else if (opt.equals("--script-log")) {
                        this.mScriptLog = true;
                    } else if (opt.equals("--bugreport")) {
                        this.mRequestBugreport = true;
                    } else if (opt.equals("--periodic-bugreport")) {
                        this.mGetPeriodicBugreport = true;
                        this.mBugreportFrequency = nextOptionLong("Number of iterations");
                    } else if (opt.equals("--permission-target-system")) {
                        this.mPermissionTargetSystem = true;
                    } else if (opt.equals("-h")) {
                        showUsage();
                        return false;
                    } else {
                        logger = Logger.err;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("** Error: Unknown option: ");
                        stringBuilder.append(opt);
                        logger.println(stringBuilder.toString());
                        showUsage();
                        return false;
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("** Error: ");
            stringBuilder.append(ex.toString());
            logger.println(stringBuilder.toString());
            showUsage();
            return false;
        }
    }

    private static boolean loadPackageListFromFile(String fileName, Set<String> list) {
        Logger logger;
        StringBuilder stringBuilder;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            while (true) {
                String readLine = reader.readLine();
                String s = readLine;
                if (readLine != null) {
                    readLine = s.trim();
                    if (readLine.length() > 0 && !readLine.startsWith("#")) {
                        list.add(readLine);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException ioe) {
                        logger = Logger.err;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("");
                        stringBuilder.append(ioe);
                        logger.println(stringBuilder.toString());
                    }
                }
            }
            reader.close();
            return true;
        } catch (IOException ioe2) {
            logger = Logger.err;
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(ioe2);
            logger.println(stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe3) {
                    Logger logger2 = Logger.err;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("");
                    stringBuilder2.append(ioe3);
                    logger2.println(stringBuilder2.toString());
                }
            }
            return false;
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe4) {
                    Logger logger3 = Logger.err;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("");
                    stringBuilder3.append(ioe4);
                    logger3.println(stringBuilder3.toString());
                }
            }
        }
    }

    private boolean loadPackageLists() {
        if ((this.mPkgWhitelistFile != null || MonkeyUtils.getPackageFilter().hasValidPackages()) && this.mPkgBlacklistFile != null) {
            Logger.err.println("** Error: you can not specify a package blacklist together with a whitelist or individual packages (via -p).");
            return false;
        }
        Set<String> validPackages = new HashSet();
        if (this.mPkgWhitelistFile != null && !loadPackageListFromFile(this.mPkgWhitelistFile, validPackages)) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addValidPackages(validPackages);
        Set<String> invalidPackages = new HashSet();
        if (this.mPkgBlacklistFile != null && !loadPackageListFromFile(this.mPkgBlacklistFile, invalidPackages)) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addInvalidPackages(invalidPackages);
        return true;
    }

    private boolean checkInternalConfiguration() {
        return true;
    }

    private boolean getSystemInterfaces() {
        this.mAm = ActivityManager.getService();
        if (this.mAm == null) {
            Logger.err.println("** Error: Unable to connect to activity manager; is the system running?");
            return false;
        }
        this.mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        if (this.mWm == null) {
            Logger.err.println("** Error: Unable to connect to window manager; is the system running?");
            return false;
        }
        this.mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (this.mPm == null) {
            Logger.err.println("** Error: Unable to connect to package manager; is the system running?");
            return false;
        }
        try {
            this.mAm.setActivityController(new ActivityController(), true);
            this.mNetworkMonitor.register(this.mAm);
            return true;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
            return false;
        }
    }

    private boolean getMainApps() {
        try {
            int N = this.mMainCategories.size();
            for (int i = 0; i < N; i++) {
                Intent intent = new Intent("android.intent.action.MAIN");
                String category = (String) this.mMainCategories.get(i);
                if (category.length() > 0) {
                    intent.addCategory(category);
                }
                List<ResolveInfo> mainApps = this.mPm.queryIntentActivities(intent, null, 0, UserHandle.myUserId()).getList();
                Logger logger;
                if (mainApps == null || mainApps.size() == 0) {
                    logger = Logger.err;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("// Warning: no activities found for category ");
                    stringBuilder.append(category);
                    logger.println(stringBuilder.toString());
                } else {
                    if (this.mVerbose >= 2) {
                        logger = Logger.out;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("// Selecting main activities from category ");
                        stringBuilder2.append(category);
                        logger.println(stringBuilder2.toString());
                    }
                    int NA = mainApps.size();
                    for (int a = 0; a < NA; a++) {
                        ResolveInfo r = (ResolveInfo) mainApps.get(a);
                        String packageName = r.activityInfo.applicationInfo.packageName;
                        Logger logger2;
                        StringBuilder stringBuilder3;
                        if (MonkeyUtils.getPackageFilter().checkEnteringPackage(packageName)) {
                            if (this.mVerbose >= 2) {
                                logger2 = Logger.out;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("//   + Using main activity ");
                                stringBuilder3.append(r.activityInfo.name);
                                stringBuilder3.append(" (from package ");
                                stringBuilder3.append(packageName);
                                stringBuilder3.append(")");
                                logger2.println(stringBuilder3.toString());
                            }
                            this.mMainApps.add(new ComponentName(packageName, r.activityInfo.name));
                        } else if (this.mVerbose >= 3) {
                            logger2 = Logger.out;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("//   - NOT USING main activity ");
                            stringBuilder3.append(r.activityInfo.name);
                            stringBuilder3.append(" (from package ");
                            stringBuilder3.append(packageName);
                            stringBuilder3.append(")");
                            logger2.println(stringBuilder3.toString());
                        }
                    }
                }
            }
            if (this.mMainApps.size() != 0) {
                return true;
            }
            Logger.out.println("** No activities found to run, monkey aborted.");
            return false;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with package manager!");
            return false;
        }
    }

    private int runMonkeyCycles() {
        StringBuilder stringBuilder;
        Logger logger;
        int cycleCounter = 0;
        boolean shouldReportAnrTraces = false;
        boolean shouldAbort = false;
        boolean shouldReportDumpsysMemInfo = false;
        int eventCounter = 0;
        boolean systemCrashed = false;
        while (!systemCrashed) {
            try {
                if (cycleCounter >= this.mCount) {
                    break;
                }
                synchronized (this) {
                    if (this.mRequestProcRank) {
                        reportProcRank();
                        this.mRequestProcRank = false;
                    }
                    if (this.mRequestAnrTraces) {
                        this.mRequestAnrTraces = false;
                        shouldReportAnrTraces = true;
                    }
                    if (this.mRequestAnrBugreport) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("anr_");
                        stringBuilder.append(this.mReportProcessName);
                        stringBuilder.append("_");
                        getBugreport(stringBuilder.toString());
                        this.mRequestAnrBugreport = false;
                    }
                    if (this.mRequestWatchdogBugreport) {
                        Logger.out.println("Print the watchdog report");
                        getBugreport("anr_watchdog_");
                        this.mRequestWatchdogBugreport = false;
                    }
                    if (this.mRequestAppCrashBugreport) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("app_crash");
                        stringBuilder.append(this.mReportProcessName);
                        stringBuilder.append("_");
                        getBugreport(stringBuilder.toString());
                        this.mRequestAppCrashBugreport = false;
                    }
                    if (this.mRequestPeriodicBugreport) {
                        getBugreport("Bugreport_");
                        this.mRequestPeriodicBugreport = false;
                    }
                    if (this.mRequestDumpsysMemInfo) {
                        this.mRequestDumpsysMemInfo = false;
                        shouldReportDumpsysMemInfo = true;
                    }
                    if (this.mMonitorNativeCrashes && checkNativeCrashes() && eventCounter > 0) {
                        Logger.out.println("** New native crash detected.");
                        if (this.mRequestBugreport) {
                            getBugreport("native_crash_");
                        }
                        boolean z = this.mAbort || !this.mIgnoreNativeCrashes || this.mKillProcessAfterError;
                        this.mAbort = z;
                    }
                    if (this.mAbort) {
                        shouldAbort = true;
                    }
                    if (this.mWatchdogWaiting) {
                        this.mWatchdogWaiting = false;
                        notifyAll();
                    }
                }
                if (shouldReportAnrTraces) {
                    shouldReportAnrTraces = false;
                    reportAnrTraces();
                }
                if (shouldReportDumpsysMemInfo) {
                    shouldReportDumpsysMemInfo = false;
                    reportDumpsysMemInfo();
                }
                if (!shouldAbort) {
                    if (!this.mSendNoEvents) {
                        if (this.mVerbose > 0 && eventCounter % 100 == 0 && eventCounter != 0) {
                            String calendarTime = MonkeyUtils.toCalendarTime(System.currentTimeMillis());
                            long systemUpTime = SystemClock.elapsedRealtime();
                            Logger logger2 = Logger.out;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("    //[calendar_time:");
                            stringBuilder2.append(calendarTime);
                            stringBuilder2.append(" system_uptime:");
                            stringBuilder2.append(systemUpTime);
                            stringBuilder2.append("]");
                            logger2.println(stringBuilder2.toString());
                            logger2 = Logger.out;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("    // Sending event #");
                            stringBuilder2.append(eventCounter);
                            logger2.println(stringBuilder2.toString());
                        }
                        MonkeyEvent ev = this.mEventSource.getNextEvent();
                        if (ev == null) {
                            if (this.mCountEvents) {
                                break;
                            }
                            cycleCounter++;
                            writeScriptLog(cycleCounter);
                            if (this.mGetPeriodicBugreport && ((long) cycleCounter) % this.mBugreportFrequency == 0) {
                                this.mRequestPeriodicBugreport = true;
                            }
                        } else {
                            int injectCode = ev.injectEvent(this.mWm, this.mAm, this.mVerbose);
                            if (injectCode == 0) {
                                Logger.out.println("    // Injection Failed");
                                if (ev instanceof MonkeyKeyEvent) {
                                    this.mDroppedKeyEvents++;
                                } else if (ev instanceof MonkeyMotionEvent) {
                                    this.mDroppedPointerEvents++;
                                } else if (ev instanceof MonkeyFlipEvent) {
                                    this.mDroppedFlipEvents++;
                                } else if (ev instanceof MonkeyRotationEvent) {
                                    this.mDroppedRotationEvents++;
                                }
                            } else if (injectCode == -1) {
                                systemCrashed = true;
                                Logger.err.println("** Error: RemoteException while injecting event.");
                            } else if (injectCode == -2) {
                                systemCrashed = 1 ^ this.mIgnoreSecurityExceptions;
                                if (systemCrashed) {
                                    Logger.err.println("** Error: SecurityException while injecting event.");
                                }
                            }
                            if (!(ev instanceof MonkeyThrottleEvent)) {
                                eventCounter++;
                                if (this.mCountEvents) {
                                    cycleCounter++;
                                }
                            }
                        }
                    } else {
                        eventCounter++;
                        cycleCounter++;
                    }
                } else {
                    Logger.out.println("** Monkey aborted due to error.");
                    logger = Logger.out;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Events injected: ");
                    stringBuilder.append(eventCounter);
                    logger.println(stringBuilder.toString());
                    return eventCounter;
                }
            } catch (RuntimeException e) {
                Logger.error("** Error: A RuntimeException occurred:", e);
            }
        }
        logger = Logger.out;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Events injected: ");
        stringBuilder.append(eventCounter);
        logger.println(stringBuilder.toString());
        return eventCounter;
    }

    private void signalPersistentProcesses() {
        try {
            this.mAm.signalPersistentProcesses(10);
            synchronized (this) {
                wait(2000);
            }
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
        } catch (InterruptedException e2) {
        }
    }

    private boolean checkNativeCrashes() {
        String[] tombstones = TOMBSTONES_PATH.list();
        if (tombstones == null || tombstones.length == 0) {
            this.mTombstones = null;
            return false;
        }
        HashSet<Long> newStones = new HashSet();
        boolean result = false;
        for (String t : tombstones) {
            if (t.startsWith(TOMBSTONE_PREFIX)) {
                File f = new File(TOMBSTONES_PATH, t);
                newStones.add(Long.valueOf(f.lastModified()));
                if (this.mTombstones == null || !this.mTombstones.contains(Long.valueOf(f.lastModified()))) {
                    result = true;
                    waitForTombstoneToBeWritten(Paths.get(TOMBSTONES_PATH.getPath(), new String[]{t}));
                    Logger logger = Logger.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("** New tombstone found: ");
                    stringBuilder.append(f.getAbsolutePath());
                    stringBuilder.append(", size: ");
                    stringBuilder.append(f.length());
                    logger.println(stringBuilder.toString());
                }
            }
        }
        this.mTombstones = newStones;
        return result;
    }

    private void waitForTombstoneToBeWritten(Path path) {
        int i = 0;
        boolean isWritten = false;
        while (i < NUM_READ_TOMBSTONE_RETRIES) {
            try {
                long size = Files.size(path);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (size > 0) {
                    if (Files.size(path) == size) {
                        isWritten = true;
                        break;
                    }
                }
                i++;
            } catch (IOException e2) {
                Logger logger = Logger.err;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to get tombstone file size: ");
                stringBuilder.append(e2.toString());
                logger.println(stringBuilder.toString());
            }
        }
        if (!isWritten) {
            Logger.err.println("Incomplete tombstone file.");
        }
    }

    private String nextOption() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        if (!arg.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() <= 1 || arg.charAt(1) == '-') {
            this.mCurArgData = null;
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("arg=\"");
            stringBuilder.append(arg);
            stringBuilder.append("\" mCurArgData=\"");
            stringBuilder.append(this.mCurArgData);
            stringBuilder.append("\" mNextArg=");
            stringBuilder.append(this.mNextArg);
            stringBuilder.append(" argwas=\"");
            stringBuilder.append(this.mArgs[this.mNextArg - 1]);
            stringBuilder.append("\" nextarg=\"");
            stringBuilder.append(this.mArgs[this.mNextArg]);
            stringBuilder.append("\"");
            logger.println(stringBuilder.toString());
            return arg;
        } else if (arg.length() > 2) {
            this.mCurArgData = arg.substring(2);
            return arg.substring(0, 2);
        } else {
            this.mCurArgData = null;
            return arg;
        }
    }

    private String nextOptionData() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String data = this.mArgs[this.mNextArg];
        Logger logger = Logger.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("data=\"");
        stringBuilder.append(data);
        stringBuilder.append("\"");
        logger.println(stringBuilder.toString());
        this.mNextArg++;
        return data;
    }

    private long nextOptionLong(String opt) {
        try {
            return Long.parseLong(nextOptionData());
        } catch (NumberFormatException e) {
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("** Error: ");
            stringBuilder.append(opt);
            stringBuilder.append(" is not a number");
            logger.println(stringBuilder.toString());
            throw e;
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return arg;
    }

    private void showUsage() {
        StringBuffer usage = new StringBuffer();
        usage.append("usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]\n");
        usage.append("              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]\n");
        usage.append("              [--ignore-crashes] [--ignore-timeouts]\n");
        usage.append("              [--ignore-security-exceptions]\n");
        usage.append("              [--monitor-native-crashes] [--ignore-native-crashes]\n");
        usage.append("              [--kill-process-after-error] [--hprof]\n");
        usage.append("              [--match-description TEXT]\n");
        usage.append("              [--pct-touch PERCENT] [--pct-motion PERCENT]\n");
        usage.append("              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]\n");
        usage.append("              [--pct-nav PERCENT] [--pct-majornav PERCENT]\n");
        usage.append("              [--pct-appswitch PERCENT] [--pct-flip PERCENT]\n");
        usage.append("              [--pct-anyevent PERCENT] [--pct-pinchzoom PERCENT]\n");
        usage.append("              [--pct-permission PERCENT]\n");
        usage.append("              [--pkg-blacklist-file PACKAGE_BLACKLIST_FILE]\n");
        usage.append("              [--pkg-whitelist-file PACKAGE_WHITELIST_FILE]\n");
        usage.append("              [--wait-dbg] [--dbg-no-events]\n");
        usage.append("              [--setup scriptfile] [-f scriptfile [-f scriptfile] ...]\n");
        usage.append("              [--port port]\n");
        usage.append("              [-s SEED] [-v [-v] ...]\n");
        usage.append("              [--throttle MILLISEC] [--randomize-throttle]\n");
        usage.append("              [--profile-wait MILLISEC]\n");
        usage.append("              [--device-sleep-time MILLISEC]\n");
        usage.append("              [--randomize-script]\n");
        usage.append("              [--script-log]\n");
        usage.append("              [--bugreport]\n");
        usage.append("              [--periodic-bugreport]\n");
        usage.append("              [--permission-target-system]\n");
        usage.append("              COUNT\n");
        Logger.err.println(usage.toString());
    }
}

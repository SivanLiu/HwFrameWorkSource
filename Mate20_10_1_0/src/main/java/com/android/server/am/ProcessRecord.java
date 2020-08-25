package com.android.server.am;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;
import android.app.Dialog;
import android.app.IApplicationThread;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.VersionedPackage;
import android.content.res.CompatibilityInfo;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.EventLog;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.zrhung.IZrHung;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.Zygote;
import com.android.server.am.ProcessList;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.devicepolicy.HwLog;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessRecord implements WindowProcessListener {
    private static final String TAG = "ActivityManager";
    int adjSeq;
    Object adjSource;
    int adjSourceProcState;
    Object adjTarget;
    String adjType;
    int adjTypeCode;
    Dialog anrDialog;
    int anrType;
    final boolean appZygote;
    boolean bad;
    ProcessState baseProcessTracker;
    boolean cached;
    CompatibilityInfo compat;
    int completedAdjSeq;
    final ArrayList<ContentProviderConnection> conProviders = new ArrayList<>();
    int connectionGroup;
    int connectionImportance;
    ServiceRecord connectionService;
    final ArraySet<ConnectionRecord> connections = new ArraySet<>();
    boolean containsCycle;
    Dialog crashDialog;
    Runnable crashHandler;
    ActivityManager.ProcessErrorStateInfo crashingReport;
    int curAdj;
    long curCpuTime;
    BatteryStatsImpl.Uid.Proc curProcBatteryStats;
    final ArraySet<BroadcastRecord> curReceivers = new ArraySet<>();
    IBinder.DeathRecipient deathRecipient;
    boolean empty;
    String[] entryPointArgs;
    ComponentName errorReportReceiver;
    boolean execServicesFg;
    final ArraySet<ServiceRecord> executingServices = new ArraySet<>();
    boolean forceCrashReport;
    Object forcingToImportant;
    int[] gids;
    boolean hasAboveClient;
    boolean hasShownUi;
    boolean hasStartedServices;
    HostingRecord hostingRecord;
    public boolean inFullBackup;
    ApplicationInfo info;
    long initialIdlePss;
    String instructionSet;
    final boolean isolated;
    String isolatedEntryPoint;
    String[] isolatedEntryPointArgs;
    boolean killed;
    boolean killedByAm;
    long lastActivityTime;
    long lastCachedPss;
    long lastCachedSwapPss;
    int lastCompactAction;
    long lastCompactTime;
    long lastCpuTime;
    long lastLowMemory;
    Debug.MemoryInfo lastMemInfo;
    long lastMemInfoTime;
    long lastProviderTime;
    long lastPss;
    long lastPssTime;
    long lastRequestedGc;
    long lastStateTime;
    long lastSwapPss;
    long lastTopTime;
    boolean launchfromActivity;
    int lruSeq;
    final ArraySet<Binder> mAllowBackgroundActivityStartsTokens = new ArraySet<>();
    private final IZrHung mAppEyeANR = HwFrameworkFactory.getZrHung("appeye_anr");
    private ArraySet<Integer> mBoundClientUids = new ArraySet<>();
    boolean mCrashing;
    private int mCurProcState = 21;
    private int mCurRawAdj;
    private int mCurRawProcState = 21;
    int mCurSchedGroup;
    private boolean mDebugging;
    int mDisplayId;
    private long mFgInteractionTime;
    private int mFgServiceTypes;
    boolean mHasClientActivities;
    boolean mHasForegroundActivities;
    boolean mHasForegroundServices;
    private boolean mHasOverlayUi;
    private boolean mHasTopUi;
    ActiveInstrumentation mInstr;
    private long mInteractionEventTime;
    private boolean mNotResponding;
    private boolean mPendingUiClean;
    boolean mPersistent;
    private int mRepFgServiceTypes;
    private int mRepProcState = 21;
    private String mRequiredAbi;
    private final ActivityManagerService mService;
    private boolean mUsingWrapper;
    private long mWhenUnimportant;
    /* access modifiers changed from: private */
    public final WindowProcessController mWindowProcessController;
    int maxAdj;
    int mountMode;
    long nextPssTime;
    boolean notCachedSinceIdle;
    ActivityManager.ProcessErrorStateInfo notRespondingReport;
    boolean pendingStart;
    public int pid;
    ArraySet<String> pkgDeps;
    final PackageList pkgList = new PackageList();
    int preloadStatus;
    String procStatFile;
    boolean procStateChanged;
    final ProcessList.ProcStateMemTracker procStateMemTracker = new ProcessList.ProcStateMemTracker();
    public final String processName;
    int pssProcState = 21;
    int pssStatType;
    final ArrayMap<String, ContentProviderRecord> pubProviders = new ArrayMap<>();
    final ArraySet<ReceiverList> receivers = new ArraySet<>();
    volatile boolean removed;
    int renderThreadTid;
    boolean repForegroundActivities;
    boolean reportLowMemory;
    boolean reportedInteraction;
    int reqCompactAction;
    boolean runningRemoteAnimation;
    int savedPriority;
    String seInfo;
    boolean serviceHighRam;
    boolean serviceb;
    final ArraySet<ServiceRecord> services = new ArraySet<>();
    int setAdj;
    int setProcState = 21;
    int setRawAdj;
    int setSchedGroup;
    String shortStringName;
    long startSeq;
    long startTime;
    int startUid;
    boolean starting;
    String stringName;
    boolean systemNoUi;
    public IApplicationThread thread;
    boolean treatLikeActivity;
    int trimMemoryLevel;
    public final int uid;
    UidRecord uidRecord;
    boolean unlocked;
    final int userId;
    int verifiedAdj;
    Dialog waitDialog;
    boolean waitedForDebugger;
    String waitingToKill;
    boolean whitelistManager;

    final class PackageList {
        final ArrayMap<String, ProcessStats.ProcessStateHolder> mPkgList = new ArrayMap<>();

        PackageList() {
        }

        /* access modifiers changed from: package-private */
        public ProcessStats.ProcessStateHolder put(String key, ProcessStats.ProcessStateHolder value) {
            ProcessRecord.this.mWindowProcessController.addPackage(key);
            return this.mPkgList.put(key, value);
        }

        /* access modifiers changed from: package-private */
        public void clear() {
            this.mPkgList.clear();
            ProcessRecord.this.mWindowProcessController.clearPackageList();
        }

        /* access modifiers changed from: package-private */
        public int size() {
            return this.mPkgList.size();
        }

        /* access modifiers changed from: package-private */
        public String keyAt(int index) {
            return this.mPkgList.keyAt(index);
        }

        public ProcessStats.ProcessStateHolder valueAt(int index) {
            return this.mPkgList.valueAt(index);
        }

        /* access modifiers changed from: package-private */
        public ProcessStats.ProcessStateHolder get(String pkgName) {
            return this.mPkgList.get(pkgName);
        }

        /* access modifiers changed from: package-private */
        public boolean containsKey(Object key) {
            return this.mPkgList.containsKey(key);
        }
    }

    /* access modifiers changed from: package-private */
    public void setStartParams(int startUid2, HostingRecord hostingRecord2, String seInfo2, long startTime2) {
        this.startUid = startUid2;
        this.hostingRecord = hostingRecord2;
        this.seInfo = seInfo2;
        this.startTime = startTime2;
    }

    /* access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        long nowUptime = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print("user #");
        pw.print(this.userId);
        pw.print(" uid=");
        pw.print(this.info.uid);
        if (this.uid != this.info.uid) {
            pw.print(" ISOLATED uid=");
            pw.print(this.uid);
        }
        pw.print(" gids={");
        if (this.gids != null) {
            for (int gi = 0; gi < this.gids.length; gi++) {
                if (gi != 0) {
                    pw.print(", ");
                }
                pw.print(this.gids[gi]);
            }
        }
        pw.println("}");
        pw.print(prefix);
        pw.print("mRequiredAbi=");
        pw.print(this.mRequiredAbi);
        pw.print(" instructionSet=");
        pw.println(this.instructionSet);
        if (this.info.className != null) {
            pw.print(prefix);
            pw.print("class=");
            pw.println(this.info.className);
        }
        if (this.info.manageSpaceActivityName != null) {
            pw.print(prefix);
            pw.print("manageSpaceActivityName=");
            pw.println(this.info.manageSpaceActivityName);
        }
        pw.print(prefix);
        pw.print("dir=");
        pw.print(this.info.sourceDir);
        pw.print(" publicDir=");
        pw.print(this.info.publicSourceDir);
        pw.print(" data=");
        pw.println(this.info.dataDir);
        pw.print(prefix);
        pw.print("packageList={");
        for (int i = 0; i < this.pkgList.size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(this.pkgList.keyAt(i));
        }
        pw.println("}");
        if (this.pkgDeps != null) {
            pw.print(prefix);
            pw.print("packageDependencies={");
            for (int i2 = 0; i2 < this.pkgDeps.size(); i2++) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                pw.print(this.pkgDeps.valueAt(i2));
            }
            pw.println("}");
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.println(this.compat);
        if (this.mInstr != null) {
            pw.print(prefix);
            pw.print("mInstr=");
            pw.println(this.mInstr);
        }
        pw.print(prefix);
        pw.print("thread=");
        pw.println(this.thread);
        pw.print(prefix);
        pw.print("pid=");
        pw.print(this.pid);
        pw.print(" starting=");
        pw.println(this.starting);
        pw.print(prefix);
        pw.print("lastActivityTime=");
        TimeUtils.formatDuration(this.lastActivityTime, nowUptime, pw);
        pw.print(" lastPssTime=");
        TimeUtils.formatDuration(this.lastPssTime, nowUptime, pw);
        pw.print(" pssStatType=");
        pw.print(this.pssStatType);
        pw.print(" nextPssTime=");
        TimeUtils.formatDuration(this.nextPssTime, nowUptime, pw);
        pw.println();
        pw.print(prefix);
        pw.print("adjSeq=");
        pw.print(this.adjSeq);
        pw.print(" lruSeq=");
        pw.print(this.lruSeq);
        pw.print(" lastPss=");
        DebugUtils.printSizeValue(pw, this.lastPss * 1024);
        pw.print(" lastSwapPss=");
        DebugUtils.printSizeValue(pw, this.lastSwapPss * 1024);
        pw.print(" lastCachedPss=");
        DebugUtils.printSizeValue(pw, this.lastCachedPss * 1024);
        pw.print(" lastCachedSwapPss=");
        DebugUtils.printSizeValue(pw, this.lastCachedSwapPss * 1024);
        pw.println();
        pw.print(prefix);
        pw.print("procStateMemTracker: ");
        this.procStateMemTracker.dumpLine(pw);
        pw.print(prefix);
        pw.print("cached=");
        pw.print(this.cached);
        pw.print(" empty=");
        pw.println(this.empty);
        if (this.serviceb) {
            pw.print(prefix);
            pw.print("serviceb=");
            pw.print(this.serviceb);
            pw.print(" serviceHighRam=");
            pw.println(this.serviceHighRam);
        }
        if (this.notCachedSinceIdle) {
            pw.print(prefix);
            pw.print("notCachedSinceIdle=");
            pw.print(this.notCachedSinceIdle);
            pw.print(" initialIdlePss=");
            pw.println(this.initialIdlePss);
        }
        pw.print(prefix);
        pw.print("oom: max=");
        pw.print(this.maxAdj);
        pw.print(" curRaw=");
        pw.print(this.mCurRawAdj);
        pw.print(" setRaw=");
        pw.print(this.setRawAdj);
        pw.print(" cur=");
        pw.print(this.curAdj);
        pw.print(" set=");
        pw.println(this.setAdj);
        pw.print(prefix);
        pw.print("lastCompactTime=");
        pw.print(this.lastCompactTime);
        pw.print(" lastCompactAction=");
        pw.print(this.lastCompactAction);
        pw.print(prefix);
        pw.print("mCurSchedGroup=");
        pw.print(this.mCurSchedGroup);
        pw.print(" setSchedGroup=");
        pw.print(this.setSchedGroup);
        pw.print(" systemNoUi=");
        pw.print(this.systemNoUi);
        pw.print(" trimMemoryLevel=");
        pw.println(this.trimMemoryLevel);
        pw.print(prefix);
        pw.print("curProcState=");
        pw.print(getCurProcState());
        pw.print(" mRepProcState=");
        pw.print(this.mRepProcState);
        pw.print(" pssProcState=");
        pw.print(this.pssProcState);
        pw.print(" setProcState=");
        pw.print(this.setProcState);
        pw.print(" lastStateTime=");
        TimeUtils.formatDuration(this.lastStateTime, nowUptime, pw);
        pw.println();
        if (this.hasShownUi || this.mPendingUiClean || this.hasAboveClient || this.treatLikeActivity) {
            pw.print(prefix);
            pw.print("hasShownUi=");
            pw.print(this.hasShownUi);
            pw.print(" pendingUiClean=");
            pw.print(this.mPendingUiClean);
            pw.print(" hasAboveClient=");
            pw.print(this.hasAboveClient);
            pw.print(" treatLikeActivity=");
            pw.println(this.treatLikeActivity);
        }
        if (!(this.connectionService == null && this.connectionGroup == 0)) {
            pw.print(prefix);
            pw.print("connectionGroup=");
            pw.print(this.connectionGroup);
            pw.print(" Importance=");
            pw.print(this.connectionImportance);
            pw.print(" Service=");
            pw.println(this.connectionService);
        }
        if (hasTopUi() || hasOverlayUi() || this.runningRemoteAnimation) {
            pw.print(prefix);
            pw.print("hasTopUi=");
            pw.print(hasTopUi());
            pw.print(" hasOverlayUi=");
            pw.print(hasOverlayUi());
            pw.print(" runningRemoteAnimation=");
            pw.println(this.runningRemoteAnimation);
        }
        if (this.mHasForegroundServices || this.forcingToImportant != null) {
            pw.print(prefix);
            pw.print("mHasForegroundServices=");
            pw.print(this.mHasForegroundServices);
            pw.print(" forcingToImportant=");
            pw.println(this.forcingToImportant);
        }
        if (this.reportedInteraction || this.mFgInteractionTime != 0) {
            pw.print(prefix);
            pw.print("reportedInteraction=");
            pw.print(this.reportedInteraction);
            if (this.mInteractionEventTime != 0) {
                pw.print(" time=");
                TimeUtils.formatDuration(this.mInteractionEventTime, SystemClock.elapsedRealtime(), pw);
            }
            if (this.mFgInteractionTime != 0) {
                pw.print(" fgInteractionTime=");
                TimeUtils.formatDuration(this.mFgInteractionTime, SystemClock.elapsedRealtime(), pw);
            }
            pw.println();
        }
        if (this.mPersistent || this.removed) {
            pw.print(prefix);
            pw.print("persistent=");
            pw.print(this.mPersistent);
            pw.print(" removed=");
            pw.println(this.removed);
        }
        if (this.mHasClientActivities || this.mHasForegroundActivities || this.repForegroundActivities) {
            pw.print(prefix);
            pw.print("hasClientActivities=");
            pw.print(this.mHasClientActivities);
            pw.print(" foregroundActivities=");
            pw.print(this.mHasForegroundActivities);
            pw.print(" (rep=");
            pw.print(this.repForegroundActivities);
            pw.println(")");
        }
        if (this.lastProviderTime > 0) {
            pw.print(prefix);
            pw.print("lastProviderTime=");
            TimeUtils.formatDuration(this.lastProviderTime, nowUptime, pw);
            pw.println();
        }
        if (this.lastTopTime > 0) {
            pw.print(prefix);
            pw.print("lastTopTime=");
            TimeUtils.formatDuration(this.lastTopTime, nowUptime, pw);
            pw.println();
        }
        if (this.hasStartedServices) {
            pw.print(prefix);
            pw.print("hasStartedServices=");
            pw.println(this.hasStartedServices);
        }
        if (this.pendingStart) {
            pw.print(prefix);
            pw.print("pendingStart=");
            pw.println(this.pendingStart);
        }
        pw.print(prefix);
        pw.print("startSeq=");
        pw.println(this.startSeq);
        pw.print(prefix);
        pw.print("mountMode=");
        pw.println(DebugUtils.valueToString(Zygote.class, "MOUNT_EXTERNAL_", this.mountMode));
        if (this.setProcState > 11) {
            pw.print(prefix);
            pw.print("lastCpuTime=");
            pw.print(this.lastCpuTime);
            if (this.lastCpuTime > 0) {
                pw.print(" timeUsed=");
                TimeUtils.formatDuration(this.curCpuTime - this.lastCpuTime, pw);
            }
            pw.print(" whenUnimportant=");
            TimeUtils.formatDuration(this.mWhenUnimportant - nowUptime, pw);
            pw.println();
        }
        pw.print(prefix);
        pw.print("lastRequestedGc=");
        TimeUtils.formatDuration(this.lastRequestedGc, nowUptime, pw);
        pw.print(" lastLowMemory=");
        TimeUtils.formatDuration(this.lastLowMemory, nowUptime, pw);
        pw.print(" reportLowMemory=");
        pw.println(this.reportLowMemory);
        if (this.killed || this.killedByAm || this.waitingToKill != null) {
            pw.print(prefix);
            pw.print("killed=");
            pw.print(this.killed);
            pw.print(" killedByAm=");
            pw.print(this.killedByAm);
            pw.print(" waitingToKill=");
            pw.println(this.waitingToKill);
        }
        if (this.mDebugging || this.mCrashing || this.crashDialog != null || this.mNotResponding || this.anrDialog != null || this.bad) {
            pw.print(prefix);
            pw.print("mDebugging=");
            pw.print(this.mDebugging);
            pw.print(" mCrashing=");
            pw.print(this.mCrashing);
            pw.print(HwLog.PREFIX);
            pw.print(this.crashDialog);
            pw.print(" mNotResponding=");
            pw.print(this.mNotResponding);
            pw.print(HwLog.PREFIX);
            pw.print(this.anrDialog);
            pw.print(" bad=");
            pw.print(this.bad);
            if (this.errorReportReceiver != null) {
                pw.print(" errorReportReceiver=");
                pw.print(this.errorReportReceiver.flattenToShortString());
            }
            pw.println();
        }
        if (this.whitelistManager) {
            pw.print(prefix);
            pw.print("whitelistManager=");
            pw.println(this.whitelistManager);
        }
        if (!(this.isolatedEntryPoint == null && this.isolatedEntryPointArgs == null)) {
            pw.print(prefix);
            pw.print("isolatedEntryPoint=");
            pw.println(this.isolatedEntryPoint);
            pw.print(prefix);
            pw.print("isolatedEntryPointArgs=");
            pw.println(Arrays.toString(this.isolatedEntryPointArgs));
        }
        this.mWindowProcessController.dump(pw, prefix);
        if (this.services.size() > 0) {
            pw.print(prefix);
            pw.println("Services:");
            for (int i3 = 0; i3 < this.services.size(); i3++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.services.valueAt(i3));
            }
        }
        if (this.executingServices.size() > 0) {
            pw.print(prefix);
            pw.print("Executing Services (fg=");
            pw.print(this.execServicesFg);
            pw.println(")");
            for (int i4 = 0; i4 < this.executingServices.size(); i4++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.executingServices.valueAt(i4));
            }
        }
        if (this.connections.size() > 0) {
            pw.print(prefix);
            pw.println("Connections:");
            for (int i5 = 0; i5 < this.connections.size(); i5++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.connections.valueAt(i5));
            }
        }
        if (this.pubProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Published Providers:");
            for (int i6 = 0; i6 < this.pubProviders.size(); i6++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.pubProviders.keyAt(i6));
                pw.print(prefix);
                pw.print("    -> ");
                pw.println(this.pubProviders.valueAt(i6));
            }
        }
        if (this.conProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Connected Providers:");
            for (int i7 = 0; i7 < this.conProviders.size(); i7++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.conProviders.get(i7).toShortString());
            }
        }
        if (!this.curReceivers.isEmpty()) {
            pw.print(prefix);
            pw.println("Current Receivers:");
            for (int i8 = 0; i8 < this.curReceivers.size(); i8++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.curReceivers.valueAt(i8));
            }
        }
        if (this.receivers.size() > 0) {
            pw.print(prefix);
            pw.println("Receivers:");
            for (int i9 = 0; i9 < this.receivers.size(); i9++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.receivers.valueAt(i9));
            }
        }
        if (this.mAllowBackgroundActivityStartsTokens.size() > 0) {
            pw.print(prefix);
            pw.println("Background activity start whitelist tokens:");
            for (int i10 = 0; i10 < this.mAllowBackgroundActivityStartsTokens.size(); i10++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mAllowBackgroundActivityStartsTokens.valueAt(i10));
            }
        }
    }

    ProcessRecord(ActivityManagerService _service, ApplicationInfo _info, String _processName, int _uid) {
        this.mService = _service;
        this.info = _info;
        boolean z = true;
        this.isolated = _info.uid != _uid;
        this.appZygote = (UserHandle.getAppId(_uid) < 90000 || UserHandle.getAppId(_uid) > 98999) ? false : z;
        this.uid = _uid;
        this.userId = UserHandle.getUserId(_uid);
        this.processName = _processName;
        this.maxAdj = NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE;
        this.setRawAdj = -10000;
        this.mCurRawAdj = -10000;
        this.verifiedAdj = -10000;
        this.setAdj = -10000;
        this.curAdj = -10000;
        this.mPersistent = false;
        this.removed = false;
        this.preloadStatus = 0;
        long uptimeMillis = SystemClock.uptimeMillis();
        this.nextPssTime = uptimeMillis;
        this.lastPssTime = uptimeMillis;
        this.lastStateTime = uptimeMillis;
        this.mWindowProcessController = new WindowProcessController(this.mService.mActivityTaskManager, this.info, this.processName, this.uid, this.userId, this, this);
        this.pkgList.put(_info.packageName, new ProcessStats.ProcessStateHolder(_info.longVersionCode));
    }

    public void setPid(int _pid) {
        this.pid = _pid;
        this.mWindowProcessController.setPid(this.pid);
        this.procStatFile = null;
        this.shortStringName = null;
        this.stringName = null;
    }

    public void makeActive(IApplicationThread _thread, ProcessStatsService tracker) {
        if (this.thread == null) {
            ProcessState origBase = this.baseProcessTracker;
            if (origBase != null) {
                origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList.mPkgList);
                for (int ipkg = this.pkgList.size() - 1; ipkg >= 0; ipkg--) {
                    StatsLog.write(3, this.uid, this.processName, this.pkgList.keyAt(ipkg), ActivityManager.processStateAmToProto(-1), this.pkgList.valueAt(ipkg).appVersion);
                }
                origBase.makeInactive();
            }
            this.baseProcessTracker = tracker.getProcessStateLocked(this.info.packageName, this.info.uid, this.info.longVersionCode, this.processName);
            this.baseProcessTracker.makeActive();
            for (int i = 0; i < this.pkgList.size(); i++) {
                ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
                if (!(holder.state == null || holder.state == origBase)) {
                    holder.state.makeInactive();
                }
                tracker.updateProcessStateHolderLocked(holder, this.pkgList.keyAt(i), this.info.uid, this.info.longVersionCode, this.processName);
                if (holder.state != this.baseProcessTracker) {
                    holder.state.makeActive();
                }
            }
        }
        this.thread = _thread;
        this.mWindowProcessController.setThread(this.thread);
        if (this.preloadStatus == 1) {
            this.preloadStatus = 2;
            UidRecord uidRecord2 = this.uidRecord;
            if (uidRecord2 != null) {
                uidRecord2.isPreload = true;
            }
        }
    }

    public void makeInactive(ProcessStatsService tracker) {
        this.thread = null;
        this.mWindowProcessController.setThread((IApplicationThread) null);
        ProcessState origBase = this.baseProcessTracker;
        if (origBase != null) {
            origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList.mPkgList);
            for (int ipkg = this.pkgList.size() - 1; ipkg >= 0; ipkg--) {
                StatsLog.write(3, this.uid, this.processName, this.pkgList.keyAt(ipkg), ActivityManager.processStateAmToProto(-1), this.pkgList.valueAt(ipkg).appVersion);
            }
            origBase.makeInactive();
            this.baseProcessTracker = null;
            for (int i = 0; i < this.pkgList.size(); i++) {
                ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
                if (!(holder.state == null || holder.state == origBase)) {
                    holder.state.makeInactive();
                }
                holder.pkg = null;
                holder.state = null;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasActivities() {
        return this.mWindowProcessController.hasActivities();
    }

    /* access modifiers changed from: package-private */
    public boolean hasActivitiesOrRecentTasks() {
        return this.mWindowProcessController.hasActivitiesOrRecentTasks();
    }

    /* access modifiers changed from: package-private */
    public boolean hasRecentTasks() {
        return this.mWindowProcessController.hasRecentTasks();
    }

    public boolean isInterestingToUserLocked() {
        if (this.mWindowProcessController.isInterestingToUser()) {
            return true;
        }
        int servicesSize = this.services.size();
        for (int i = 0; i < servicesSize; i++) {
            if (this.services.valueAt(i).isForeground) {
                return true;
            }
        }
        return false;
    }

    public void unlinkDeathRecipient() {
        try {
            if (!(this.deathRecipient == null || this.thread == null)) {
                this.thread.asBinder().unlinkToDeath(this.deathRecipient, 0);
            }
            this.deathRecipient = null;
        } catch (RuntimeException e) {
            Slog.w("ActivityManager", "Uncaught exception to unlink deathRecipient:" + toShortString());
        }
    }

    /* access modifiers changed from: package-private */
    public void updateHasAboveClientLocked() {
        this.hasAboveClient = false;
        for (int i = this.connections.size() - 1; i >= 0; i--) {
            if ((this.connections.valueAt(i).flags & 8) != 0) {
                this.hasAboveClient = true;
                return;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int modifyRawOomAdj(int adj) {
        if (!this.hasAboveClient || adj < 0) {
            return adj;
        }
        if (adj < 100) {
            return 100;
        }
        if (adj < 200) {
            return 200;
        }
        if (adj < 250) {
            return 250;
        }
        if (adj < 900) {
            return 900;
        }
        if (adj < 999) {
            return adj + 1;
        }
        return adj;
    }

    /* access modifiers changed from: package-private */
    public void scheduleCrash(String message) {
        if (!this.killedByAm && this.thread != null) {
            if (this.pid == Process.myPid()) {
                Slog.w("ActivityManager", "scheduleCrash: trying to crash system process!");
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                this.thread.scheduleCrash(message);
            } catch (RemoteException e) {
                kill("scheduleCrash for '" + message + "' failed", true);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: package-private */
    public void kill(String reason, boolean noisy) {
        if (!this.killedByAm) {
            Trace.traceBegin(64, "kill");
            if (this.mService != null && (noisy || this.info.uid == this.mService.mCurOomAdjUid)) {
                ActivityManagerService activityManagerService = this.mService;
                activityManagerService.reportUidInfoMessageLocked("ActivityManager", "Killing " + toShortString() + " (adj " + this.setAdj + "): " + reason, this.info.uid);
            }
            if (this.pid > 0) {
                EventLog.writeEvent((int) EventLogTags.AM_KILL, Integer.valueOf(this.userId), Integer.valueOf(this.pid), this.processName, Integer.valueOf(this.setAdj), reason);
                Jlog.sendKilledJlog(this.pid, this.setAdj, reason, this.processName);
                Process.killProcessQuiet(this.pid);
                ProcessList.killProcessGroup(this.uid, this.pid);
            } else {
                this.pendingStart = false;
            }
            if (!this.mPersistent) {
                this.killed = true;
                this.killedByAm = true;
            }
            Trace.traceEnd(64);
        }
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        writeToProto(proto, fieldId, -1);
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, int lruIndex) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.pid);
        proto.write(1138166333442L, this.processName);
        proto.write(1120986464259L, this.info.uid);
        if (UserHandle.getAppId(this.info.uid) >= 10000) {
            proto.write(1120986464260L, this.userId);
            proto.write(1120986464261L, UserHandle.getAppId(this.info.uid));
        }
        if (this.uid != this.info.uid) {
            proto.write(1120986464262L, UserHandle.getAppId(this.uid));
        }
        proto.write(1133871366151L, this.mPersistent);
        if (lruIndex >= 0) {
            proto.write(1120986464264L, lruIndex);
        }
        proto.end(token);
    }

    public String toShortString() {
        String str = this.shortStringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        toShortString(sb);
        String sb2 = sb.toString();
        this.shortStringName = sb2;
        return sb2;
    }

    /* access modifiers changed from: package-private */
    public void toShortString(StringBuilder sb) {
        sb.append(this.pid);
        sb.append(':');
        sb.append(this.processName);
        sb.append('/');
        if (this.info.uid < 10000) {
            sb.append(this.uid);
            return;
        }
        sb.append('u');
        sb.append(this.userId);
        int appId = UserHandle.getAppId(this.info.uid);
        if (appId >= 10000) {
            sb.append('a');
            sb.append(appId - 10000);
        } else {
            sb.append('s');
            sb.append(appId);
        }
        if (this.uid != this.info.uid) {
            sb.append('i');
            sb.append(UserHandle.getAppId(this.uid) - 99000);
        }
    }

    public String toString() {
        String str = this.stringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ProcessRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        toShortString(sb);
        sb.append('}');
        String sb2 = sb.toString();
        this.stringName = sb2;
        return sb2;
    }

    public String makeAdjReason() {
        if (this.adjSource == null && this.adjTarget == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append(' ');
        Object obj = this.adjTarget;
        if (obj instanceof ComponentName) {
            sb.append(((ComponentName) obj).flattenToShortString());
        } else if (obj != null) {
            sb.append(obj.toString());
        } else {
            sb.append("{null}");
        }
        sb.append("<=");
        Object obj2 = this.adjSource;
        if (obj2 instanceof ProcessRecord) {
            sb.append("Proc{");
            sb.append(((ProcessRecord) this.adjSource).toShortString());
            sb.append("}");
        } else if (obj2 != null) {
            sb.append(obj2.toString());
        } else {
            sb.append("{null}");
        }
        return sb.toString();
    }

    public boolean addPackage(String pkg, long versionCode, ProcessStatsService tracker) {
        if (this.pkgList.containsKey(pkg)) {
            return false;
        }
        ProcessStats.ProcessStateHolder holder = new ProcessStats.ProcessStateHolder(versionCode);
        if (this.baseProcessTracker != null) {
            tracker.updateProcessStateHolderLocked(holder, pkg, this.info.uid, versionCode, this.processName);
            this.pkgList.put(pkg, holder);
            if (holder.state == this.baseProcessTracker) {
                return true;
            }
            holder.state.makeActive();
            return true;
        }
        this.pkgList.put(pkg, holder);
        return true;
    }

    public int getSetAdjWithServices() {
        if (this.setAdj < 900 || !this.hasStartedServices) {
            return this.setAdj;
        }
        return 800;
    }

    public void forceProcessStateUpTo(int newState) {
        if (this.mRepProcState > newState) {
            this.mRepProcState = newState;
            setCurProcState(newState);
            setCurRawProcState(newState);
            for (int ipkg = this.pkgList.size() - 1; ipkg >= 0; ipkg--) {
                StatsLog.write(3, this.uid, this.processName, this.pkgList.keyAt(ipkg), ActivityManager.processStateAmToProto(this.mRepProcState), this.pkgList.valueAt(ipkg).appVersion);
            }
        }
    }

    public void resetPackageList(ProcessStatsService tracker) {
        int N = this.pkgList.size();
        if (this.baseProcessTracker != null) {
            this.baseProcessTracker.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList.mPkgList);
            for (int ipkg = this.pkgList.size() - 1; ipkg >= 0; ipkg--) {
                StatsLog.write(3, this.uid, this.processName, this.pkgList.keyAt(ipkg), ActivityManager.processStateAmToProto(-1), this.pkgList.valueAt(ipkg).appVersion);
            }
            if (N != 1) {
                for (int i = 0; i < N; i++) {
                    ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
                    if (!(holder.state == null || holder.state == this.baseProcessTracker)) {
                        holder.state.makeInactive();
                    }
                }
                this.pkgList.clear();
                ProcessStats.ProcessStateHolder holder2 = new ProcessStats.ProcessStateHolder(this.info.longVersionCode);
                tracker.updateProcessStateHolderLocked(holder2, this.info.packageName, this.info.uid, this.info.longVersionCode, this.processName);
                this.pkgList.put(this.info.packageName, holder2);
                if (holder2.state != this.baseProcessTracker) {
                    holder2.state.makeActive();
                }
            }
        } else if (N != 1) {
            this.pkgList.clear();
            this.pkgList.put(this.info.packageName, new ProcessStats.ProcessStateHolder(this.info.longVersionCode));
        }
    }

    public String[] getPackageList() {
        int size = this.pkgList.size();
        if (size == 0) {
            return null;
        }
        String[] list = new String[size];
        for (int i = 0; i < this.pkgList.size(); i++) {
            list[i] = this.pkgList.keyAt(i);
        }
        return list;
    }

    public List<VersionedPackage> getPackageListWithVersionCode() {
        if (this.pkgList.size() == 0) {
            return null;
        }
        List<VersionedPackage> list = new ArrayList<>();
        for (int i = 0; i < this.pkgList.size(); i++) {
            list.add(new VersionedPackage(this.pkgList.keyAt(i), this.pkgList.valueAt(i).appVersion));
        }
        return list;
    }

    /* access modifiers changed from: package-private */
    public WindowProcessController getWindowProcessController() {
        return this.mWindowProcessController;
    }

    /* access modifiers changed from: package-private */
    public void setCurrentSchedulingGroup(int curSchedGroup) {
        this.mCurSchedGroup = curSchedGroup;
        this.mWindowProcessController.setCurrentSchedulingGroup(curSchedGroup);
    }

    /* access modifiers changed from: package-private */
    public int getCurrentSchedulingGroup() {
        return this.mCurSchedGroup;
    }

    /* access modifiers changed from: package-private */
    public void setCurProcState(int curProcState) {
        this.mCurProcState = curProcState;
        this.mWindowProcessController.setCurrentProcState(this.mCurProcState);
    }

    /* access modifiers changed from: package-private */
    public int getCurProcState() {
        return this.mCurProcState;
    }

    /* access modifiers changed from: package-private */
    public void setCurRawProcState(int curRawProcState) {
        this.mCurRawProcState = curRawProcState;
    }

    /* access modifiers changed from: package-private */
    public int getCurRawProcState() {
        return this.mCurRawProcState;
    }

    /* access modifiers changed from: package-private */
    public void setReportedProcState(int repProcState) {
        this.mRepProcState = repProcState;
        for (int ipkg = this.pkgList.size() - 1; ipkg >= 0; ipkg--) {
            StatsLog.write(3, this.uid, this.processName, this.pkgList.keyAt(ipkg), ActivityManager.processStateAmToProto(this.mRepProcState), this.pkgList.valueAt(ipkg).appVersion);
        }
        this.mWindowProcessController.setReportedProcState(repProcState);
    }

    /* access modifiers changed from: package-private */
    public int getReportedProcState() {
        return this.mRepProcState;
    }

    /* access modifiers changed from: package-private */
    public void setCrashing(boolean crashing) {
        this.mCrashing = crashing;
        this.mWindowProcessController.setCrashing(crashing);
    }

    /* access modifiers changed from: package-private */
    public boolean isCrashing() {
        return this.mCrashing;
    }

    /* access modifiers changed from: package-private */
    public void setNotResponding(boolean notResponding) {
        this.mNotResponding = notResponding;
        this.mWindowProcessController.setNotResponding(notResponding);
    }

    /* access modifiers changed from: package-private */
    public boolean isNotResponding() {
        return this.mNotResponding;
    }

    /* access modifiers changed from: package-private */
    public void setPersistent(boolean persistent) {
        this.mPersistent = persistent;
        this.mWindowProcessController.setPersistent(persistent);
    }

    /* access modifiers changed from: package-private */
    public boolean isPersistent() {
        return this.mPersistent;
    }

    public void setRequiredAbi(String requiredAbi) {
        this.mRequiredAbi = requiredAbi;
        this.mWindowProcessController.setRequiredAbi(requiredAbi);
    }

    /* access modifiers changed from: package-private */
    public String getRequiredAbi() {
        return this.mRequiredAbi;
    }

    /* access modifiers changed from: package-private */
    public void setHasForegroundServices(boolean hasForegroundServices, int fgServiceTypes) {
        this.mHasForegroundServices = hasForegroundServices;
        this.mFgServiceTypes = fgServiceTypes;
        this.mWindowProcessController.setHasForegroundServices(hasForegroundServices);
    }

    /* access modifiers changed from: package-private */
    public boolean hasForegroundServices() {
        return this.mHasForegroundServices;
    }

    /* access modifiers changed from: package-private */
    public boolean hasLocationForegroundServices() {
        return this.mHasForegroundServices && (this.mFgServiceTypes & 8) != 0;
    }

    /* access modifiers changed from: package-private */
    public int getForegroundServiceTypes() {
        if (this.mHasForegroundServices) {
            return this.mFgServiceTypes;
        }
        return 0;
    }

    /* access modifiers changed from: package-private */
    public int getReportedForegroundServiceTypes() {
        return this.mRepFgServiceTypes;
    }

    /* access modifiers changed from: package-private */
    public void setReportedForegroundServiceTypes(int foregroundServiceTypes) {
        this.mRepFgServiceTypes = foregroundServiceTypes;
    }

    /* access modifiers changed from: package-private */
    public void setHasForegroundActivities(boolean hasForegroundActivities) {
        this.mHasForegroundActivities = hasForegroundActivities;
        this.mWindowProcessController.setHasForegroundActivities(hasForegroundActivities);
    }

    /* access modifiers changed from: package-private */
    public boolean hasForegroundActivities() {
        return this.mHasForegroundActivities;
    }

    /* access modifiers changed from: package-private */
    public void setHasClientActivities(boolean hasClientActivities) {
        this.mHasClientActivities = hasClientActivities;
        this.mWindowProcessController.setHasClientActivities(hasClientActivities);
    }

    /* access modifiers changed from: package-private */
    public boolean hasClientActivities() {
        return this.mHasClientActivities;
    }

    /* access modifiers changed from: package-private */
    public void setHasTopUi(boolean hasTopUi) {
        this.mHasTopUi = hasTopUi;
        this.mWindowProcessController.setHasTopUi(hasTopUi);
    }

    /* access modifiers changed from: package-private */
    public boolean hasTopUi() {
        return this.mHasTopUi;
    }

    /* access modifiers changed from: package-private */
    public void setHasOverlayUi(boolean hasOverlayUi) {
        this.mHasOverlayUi = hasOverlayUi;
        this.mWindowProcessController.setHasOverlayUi(hasOverlayUi);
    }

    /* access modifiers changed from: package-private */
    public boolean hasOverlayUi() {
        return this.mHasOverlayUi;
    }

    /* access modifiers changed from: package-private */
    public void setInteractionEventTime(long interactionEventTime) {
        this.mInteractionEventTime = interactionEventTime;
        this.mWindowProcessController.setInteractionEventTime(interactionEventTime);
    }

    /* access modifiers changed from: package-private */
    public long getInteractionEventTime() {
        return this.mInteractionEventTime;
    }

    /* access modifiers changed from: package-private */
    public void setFgInteractionTime(long fgInteractionTime) {
        this.mFgInteractionTime = fgInteractionTime;
        this.mWindowProcessController.setFgInteractionTime(fgInteractionTime);
    }

    /* access modifiers changed from: package-private */
    public long getFgInteractionTime() {
        return this.mFgInteractionTime;
    }

    /* access modifiers changed from: package-private */
    public void setWhenUnimportant(long whenUnimportant) {
        this.mWhenUnimportant = whenUnimportant;
        this.mWindowProcessController.setWhenUnimportant(whenUnimportant);
    }

    /* access modifiers changed from: package-private */
    public long getWhenUnimportant() {
        return this.mWhenUnimportant;
    }

    /* access modifiers changed from: package-private */
    public void setDebugging(boolean debugging) {
        this.mDebugging = debugging;
        this.mWindowProcessController.setDebugging(debugging);
    }

    /* access modifiers changed from: package-private */
    public boolean isDebugging() {
        return this.mDebugging;
    }

    /* access modifiers changed from: package-private */
    public void setUsingWrapper(boolean usingWrapper) {
        this.mUsingWrapper = usingWrapper;
        this.mWindowProcessController.setUsingWrapper(usingWrapper);
    }

    /* access modifiers changed from: package-private */
    public boolean isUsingWrapper() {
        return this.mUsingWrapper;
    }

    /* access modifiers changed from: package-private */
    public void addAllowBackgroundActivityStartsToken(Binder entity) {
        if (entity != null) {
            this.mAllowBackgroundActivityStartsTokens.add(entity);
            this.mWindowProcessController.setAllowBackgroundActivityStarts(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeAllowBackgroundActivityStartsToken(Binder entity) {
        if (entity != null) {
            this.mAllowBackgroundActivityStartsTokens.remove(entity);
            this.mWindowProcessController.setAllowBackgroundActivityStarts(!this.mAllowBackgroundActivityStartsTokens.isEmpty());
        }
    }

    /* access modifiers changed from: package-private */
    public void addBoundClientUid(int clientUid) {
        this.mBoundClientUids.add(Integer.valueOf(clientUid));
        this.mWindowProcessController.setBoundClientUids(this.mBoundClientUids);
    }

    /* access modifiers changed from: package-private */
    public void updateBoundClientUids() {
        if (this.services.isEmpty()) {
            clearBoundClientUids();
            return;
        }
        ArraySet<Integer> boundClientUids = new ArraySet<>();
        int K = this.services.size();
        for (int j = 0; j < K; j++) {
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> conns = this.services.valueAt(j).getConnections();
            int N = conns.size();
            for (int conni = 0; conni < N; conni++) {
                ArrayList<ConnectionRecord> c = conns.valueAt(conni);
                for (int i = 0; i < c.size(); i++) {
                    boundClientUids.add(Integer.valueOf(c.get(i).clientUid));
                }
            }
        }
        this.mBoundClientUids = boundClientUids;
        this.mWindowProcessController.setBoundClientUids(this.mBoundClientUids);
    }

    /* access modifiers changed from: package-private */
    public void addBoundClientUidsOfNewService(ServiceRecord sr) {
        if (sr != null) {
            ArrayMap<IBinder, ArrayList<ConnectionRecord>> conns = sr.getConnections();
            for (int conni = conns.size() - 1; conni >= 0; conni--) {
                ArrayList<ConnectionRecord> c = conns.valueAt(conni);
                for (int i = 0; i < c.size(); i++) {
                    this.mBoundClientUids.add(Integer.valueOf(c.get(i).clientUid));
                }
            }
            this.mWindowProcessController.setBoundClientUids(this.mBoundClientUids);
        }
    }

    /* access modifiers changed from: package-private */
    public void clearBoundClientUids() {
        this.mBoundClientUids.clear();
        this.mWindowProcessController.setBoundClientUids(this.mBoundClientUids);
    }

    /* access modifiers changed from: package-private */
    public void setActiveInstrumentation(ActiveInstrumentation instr) {
        this.mInstr = instr;
        boolean z = true;
        boolean isInstrumenting = instr != null;
        WindowProcessController windowProcessController = this.mWindowProcessController;
        if (!isInstrumenting || !instr.mHasBackgroundActivityStartsPermission) {
            z = false;
        }
        windowProcessController.setInstrumenting(isInstrumenting, z);
    }

    /* access modifiers changed from: package-private */
    public ActiveInstrumentation getActiveInstrumentation() {
        return this.mInstr;
    }

    /* access modifiers changed from: package-private */
    public void setCurRawAdj(int curRawAdj) {
        this.mCurRawAdj = curRawAdj;
        this.mWindowProcessController.setPerceptible(curRawAdj <= 200);
    }

    /* access modifiers changed from: package-private */
    public int getCurRawAdj() {
        return this.mCurRawAdj;
    }

    public void clearProfilerIfNeeded() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (!(this.mService.mProfileData.getProfileProc() == null || this.mService.mProfileData.getProfilerInfo() == null)) {
                    if (this.mService.mProfileData.getProfileProc() == this) {
                        this.mService.clearProfilerLocked();
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void updateServiceConnectionActivities() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mServices.updateServiceConnectionActivitiesLocked(this);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setPendingUiClean(boolean pendingUiClean) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mPendingUiClean = pendingUiClean;
                this.mWindowProcessController.setPendingUiClean(pendingUiClean);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean hasPendingUiClean() {
        return this.mPendingUiClean;
    }

    public void setPendingUiCleanAndForceProcessStateUpTo(int newState) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                setPendingUiClean(true);
                forceProcessStateUpTo(newState);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void updateProcessInfo(boolean updateServiceConnectionActivities, boolean activityChange, boolean updateOomAdj) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (updateServiceConnectionActivities) {
                    this.mService.mServices.updateServiceConnectionActivitiesLocked(this);
                }
                this.mService.mProcessList.updateLruProcessLocked(this, activityChange, null);
                if (updateOomAdj) {
                    this.mService.updateOomAdjLocked("updateOomAdj_activityChange");
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isRemoved() {
        return this.removed;
    }

    public long getCpuTime() {
        return this.mService.mProcessCpuTracker.getCpuTimeForPid(this.pid);
    }

    public void onStartActivity(int topProcessState, boolean setProfileProc, String packageName, long versionCode) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.waitingToKill = null;
                if (setProfileProc) {
                    this.mService.mProfileData.setProfileProc(this);
                }
                if (packageName != null) {
                    addPackage(packageName, versionCode, this.mService.mProcessStats);
                }
                updateProcessInfo(false, true, true);
                this.hasShownUi = true;
                setPendingUiClean(true);
                forceProcessStateUpTo(topProcessState);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void appDied() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.appDiedLocked(this);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public long getInputDispatchingTimeout() {
        return this.mWindowProcessController.getInputDispatchingTimeout();
    }

    public int getProcessClassEnum() {
        if (this.pid == ActivityManagerService.MY_PID) {
            return 3;
        }
        ApplicationInfo applicationInfo = this.info;
        if (applicationInfo == null) {
            return 0;
        }
        return (applicationInfo.flags & 1) != 0 ? 2 : 1;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isSilentAnr() {
        return !getShowBackground() && !isInterestingForBackgroundTraces();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public List<ProcessRecord> getLruProcessList() {
        return this.mService.mProcessList.mLruProcesses;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isMonitorCpuUsage() {
        ActivityManagerService activityManagerService = this.mService;
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v9, resolved type: com.android.server.am.IHwActivityManagerServiceEx */
    /* JADX WARN: Multi-variable type inference failed */
    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:100:0x02a4, code lost:
        r3 = r2;
        r2 = r1;
        r1 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:?, code lost:
        android.util.Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x02bf, code lost:
        if (r1 != null) goto L_0x02c1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x02c7, code lost:
        if (r2 != null) goto L_0x02c9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:111:0x02c9, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:0x02cd, code lost:
        android.util.Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x02d6, code lost:
        r0 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x02df, code lost:
        if (isSilentAnr() == false) goto L_0x030a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x02e1, code lost:
        r1 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:0x02e5, code lost:
        if (r1 < com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST.length) goto L_0x02e7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x02f1, code lost:
        if (com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST[r1].equals(r35.processName) != false) goto L_0x02f3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:120:0x02f3, code lost:
        r16 = 0;
        r0 = new java.lang.String[]{r35.processName};
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x02fe, code lost:
        r1 = r1 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:122:0x0304, code lost:
        r16 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:123:0x0307, code lost:
        r19 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:124:0x030a, code lost:
        r16 = 0;
        r19 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x0311, code lost:
        if (r19 != null) goto L_0x0315;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:0x0313, code lost:
        r1 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:127:0x0315, code lost:
        r1 = android.os.Process.getPidsForCommands(r19);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:0x031b, code lost:
        if (r1 == null) goto L_0x0339;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:130:0x031d, code lost:
        r2 = new java.util.ArrayList<>(r1.length);
        r1 = r1.length;
        r2 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:131:0x0327, code lost:
        if (r2 < r1) goto L_0x0329;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x0329, code lost:
        r2.add(java.lang.Integer.valueOf(r1[r2]));
        r2 = r2 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:133:0x0336, code lost:
        r23 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:134:0x0339, code lost:
        r23 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:0x033b, code lost:
        r1 = r35.mService.mHwAMSEx;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:136:0x0344, code lost:
        if (isSilentAnr() == false) goto L_0x0348;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x0346, code lost:
        r6 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0348, code lost:
        r6 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:0x034d, code lost:
        if (isSilentAnr() == false) goto L_0x0352;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x034f, code lost:
        r25 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:142:0x0352, code lost:
        r25 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x0354, code lost:
        r17 = r1.dumpStackTraces(r35, true, r0, r6, r25, r23);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x036c, code lost:
        if (isMonitorCpuUsage() == false) goto L_0x0392;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:145:0x036e, code lost:
        r35.mService.updateCpuStatsNow();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x0377, code lost:
        monitor-enter(r35.mService.mProcessCpuTracker);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:148:?, code lost:
        r0 = r35.mService.mProcessCpuTracker.printCurrentState(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:150:0x0382, code lost:
        r0.append(r0.printCurrentLoad());
        r0.append(r0);
        r18 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:154:0x0392, code lost:
        r18 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x0394, code lost:
        r0.append(r0.printCurrentState(r7));
        android.util.Slog.e("ActivityManager", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:156:0x03a6, code lost:
        if (r17 == null) goto L_0x03a8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:157:0x03a8, code lost:
        android.os.Process.sendSignal(r35.pid, 3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:158:0x03ae, code lost:
        r2 = r35.uid;
        r3 = r35.processName;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:159:0x03b4, code lost:
        if (r36 != null) goto L_0x03bb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x03b6, code lost:
        r4 = com.android.server.UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x03bb, code lost:
        r4 = r36;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:0x03bc, code lost:
        r0 = r35.info;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:0x03be, code lost:
        if (r0 == null) goto L_0x03ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:165:0x03c4, code lost:
        if (r0.isInstantApp() != false) goto L_0x03c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:0x03c6, code lost:
        r6 = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:0x03c8, code lost:
        r6 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:168:0x03ca, code lost:
        r6 = r16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:0x03d0, code lost:
        if (isInterestingToUserLocked() == false) goto L_0x03d4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:0x03d2, code lost:
        r7 = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:172:0x03d4, code lost:
        r7 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:0x03d5, code lost:
        r0 = getProcessClassEnum();
        r5 = r35.info;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:174:0x03db, code lost:
        if (r5 == null) goto L_0x03e0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:175:0x03dd, code lost:
        r5 = r5.packageName;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:176:0x03e0, code lost:
        r5 = "";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:177:0x03e2, code lost:
        android.util.StatsLog.write(79, r2, r3, r4, r41, r6, r7, r0, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:178:0x03f3, code lost:
        if (r39 == null) goto L_0x03fb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:179:0x03f5, code lost:
        r7 = (com.android.server.am.ProcessRecord) r39.mOwner;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:180:0x03fb, code lost:
        r7 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:181:0x03fc, code lost:
        r35.mService.addErrorToDropBox("anr", r35, r35.processName, r36, r38, r7, r41, r18, r17, null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:182:0x042d, code lost:
        if (r35.mWindowProcessController.appNotResponding(r0.toString(), new com.android.server.am.$$Lambda$ProcessRecord$Cb3MKja7_iTlaFQrvQTzPvLyoT8(r35), new com.android.server.am.$$Lambda$ProcessRecord$2DImTokd0AWNTECl3WgBxJkOOqs(r35)) == false) goto L_0x0430;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:183:0x042f, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:185:0x0432, code lost:
        monitor-enter(r35.mService);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:187:?, code lost:
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:188:0x043a, code lost:
        if (r35.mService.mBatteryStatsService != null) goto L_0x043c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:189:0x043c, code lost:
        r35.mService.mBatteryStatsService.noteProcessAnr(r35.processName, r35.uid);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:191:0x044b, code lost:
        if (isSilentAnr() == false) goto L_0x045e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:194:0x0453, code lost:
        kill("bg anr", true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:196:0x045a, code lost:
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:197:0x045d, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:199:0x0460, code lost:
        if (r41 != null) goto L_0x0462;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:200:0x0462, code lost:
        r0 = "ANR " + r41;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:201:0x0474, code lost:
        r0 = "ANR";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:202:0x0476, code lost:
        makeAppNotRespondingLocked(r36, r0, r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:203:0x049b, code lost:
        if (r35.mService.zrHungSendEvent("showanrdialog", r35.pid, r35.uid, r35.info.packageName, null, "original") == false) goto L_0x04bf;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:206:0x04a3, code lost:
        r35.anrType = 1;
        r1 = android.os.Message.obtain();
        r1.what = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:208:?, code lost:
        r1.obj = new com.android.server.am.AppNotRespondingDialog.Data(r35, r37, r40);
        r35.mService.mUiHandler.sendMessage(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:211:0x04c4, code lost:
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:212:0x04c7, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:213:0x04c8, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:216:0x04ce, code lost:
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:217:0x04d1, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:218:0x04d2, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:219:0x04d4, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:220:0x04d5, code lost:
        r3 = r2;
        r2 = r1;
        r1 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:221:0x04e6, code lost:
        if (r2 != null) goto L_0x04e8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:223:?, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:226:0x04ee, code lost:
        if (r3 != null) goto L_0x04f0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:227:0x04f0, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:228:0x04f4, code lost:
        android.util.Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:229:0x04fd, code lost:
        throw r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x01da, code lost:
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x01df, code lost:
        if (r35.mAppEyeANR == null) goto L_0x021a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x01e1, code lost:
        r0 = new android.zrhung.ZrHungData();
        r0.putString("processName", r35.processName);
        r0.putInt("pid", r35.pid);
        r0.putBoolean("isSilentANR", isSilentAnr());
        r0.putString("packageName", r35.info.packageName);
        r0.putString(com.android.server.policy.PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, r41);
        r0.putString("activityName", r36);
        r35.mAppEyeANR.sendEvent(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x021a, code lost:
        r0 = new java.lang.StringBuilder();
        r0.setLength(0);
        r0.append("ANR in ");
        r0.append(r35.processName);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x022e, code lost:
        if (r36 == null) goto L_0x023d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x0230, code lost:
        r0.append(" (");
        r0.append(r36);
        r0.append(")");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x023d, code lost:
        r0.append("\n");
        r0.append("PID: ");
        r0.append(r35.pid);
        r0.append("\n");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0251, code lost:
        if (r41 == null) goto L_0x0260;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x0253, code lost:
        r0.append("Reason: ");
        r0.append(r41);
        r0.append("\n");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x0260, code lost:
        if (r38 == null) goto L_0x0275;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x0266, code lost:
        if (r38.equals(r36) == false) goto L_0x0275;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x0268, code lost:
        r0.append("Parent: ");
        r0.append(r38);
        r0.append("\n");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x0275, code lost:
        r0 = new com.android.internal.os.ProcessCpuTracker(true);
        r1 = null;
        r1 = null;
        r2 = null;
        r2 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:?, code lost:
        r2 = new java.io.FileOutputStream(new java.io.File("/proc/sysrq-trigger"));
        r1 = new java.io.OutputStreamWriter(r2, "UTF-8");
        r1.write("w");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:?, code lost:
        r1.close();
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x02a3, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Removed duplicated region for block: B:115:0x02e1  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x030a  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0313  */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x0315  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x031d  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x0339  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x0346  */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x0348  */
    /* JADX WARNING: Removed duplicated region for block: B:141:0x034f  */
    /* JADX WARNING: Removed duplicated region for block: B:142:0x0352  */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x036e  */
    /* JADX WARNING: Removed duplicated region for block: B:154:0x0392  */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x03a8  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x03b6  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x03bb  */
    /* JADX WARNING: Removed duplicated region for block: B:164:0x03c0  */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x03ca  */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x03d2  */
    /* JADX WARNING: Removed duplicated region for block: B:172:0x03d4  */
    /* JADX WARNING: Removed duplicated region for block: B:175:0x03dd  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x03e0  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x03f5  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x03fb  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x042f A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x0430  */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x04e8 A[SYNTHETIC, Splitter:B:222:0x04e8] */
    /* JADX WARNING: Removed duplicated region for block: B:227:0x04f0 A[Catch:{ IOException -> 0x04ec }] */
    public void appNotResponding(String activityShortComponentName, ApplicationInfo aInfo, String parentShortComponentName, WindowProcessController parentProcess, boolean aboveSystem, String annotation) {
        int myPid;
        ArrayList<Integer> firstPids = new ArrayList<>(5);
        SparseArray<Boolean> lastPids = new SparseArray<>(20);
        if (Log.HWINFO) {
            HwFrameworkFactory.getLogException().cmd(HwBroadcastRadarUtil.KEY_ACTION, "copy_systrace_to_cache");
        }
        this.mWindowProcessController.appEarlyNotResponding(annotation, new Runnable() {
            /* class com.android.server.am.$$Lambda$ProcessRecord$1qn6pj5yWgiSnKANZpVz3gwd30 */

            public final void run() {
                ProcessRecord.this.lambda$appNotResponding$0$ProcessRecord();
            }
        });
        long anrTime = SystemClock.uptimeMillis();
        if (isMonitorCpuUsage()) {
            this.mService.updateCpuStatsNow();
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mService.mAtmInternal.isShuttingDown()) {
                    try {
                        Slog.i("ActivityManager", "During shutdown skipping ANR: " + this + HwLog.PREFIX + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } else if (isNotResponding()) {
                    Slog.i("ActivityManager", "Skipping duplicate ANR: " + this + HwLog.PREFIX + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (isCrashing()) {
                    Slog.i("ActivityManager", "Crashing app skipping ANR: " + this + HwLog.PREFIX + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (this.killedByAm) {
                    Slog.i("ActivityManager", "App already killed by AM skipping ANR: " + this + HwLog.PREFIX + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (this.killed) {
                    Slog.i("ActivityManager", "Skipping died app ANR: " + this + HwLog.PREFIX + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else {
                    setNotResponding(true);
                    EventLog.writeEvent((int) EventLogTags.AM_ANR, Integer.valueOf(this.userId), Integer.valueOf(this.pid), this.processName, Integer.valueOf(this.info.flags), annotation);
                    firstPids.add(Integer.valueOf(this.pid));
                    if (!isSilentAnr()) {
                        int parentPid = this.pid;
                        if (parentProcess != null && parentProcess.getPid() > 0) {
                            parentPid = parentProcess.getPid();
                        }
                        if (parentPid != this.pid) {
                            firstPids.add(Integer.valueOf(parentPid));
                        }
                        if (!(ActivityManagerService.MY_PID == this.pid || ActivityManagerService.MY_PID == parentPid)) {
                            firstPids.add(Integer.valueOf(ActivityManagerService.MY_PID));
                        }
                        for (int i = getLruProcessList().size() - 1; i >= 0; i--) {
                            ProcessRecord r = getLruProcessList().get(i);
                            if (!(r == null || r.thread == null || (myPid = r.pid) <= 0 || myPid == this.pid || myPid == parentPid || myPid == ActivityManagerService.MY_PID)) {
                                if (r.isPersistent()) {
                                    firstPids.add(Integer.valueOf(myPid));
                                } else if (r.treatLikeActivity) {
                                    firstPids.add(Integer.valueOf(myPid));
                                } else {
                                    lastPids.put(myPid, Boolean.TRUE);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                while (true) {
                    break;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public /* synthetic */ void lambda$appNotResponding$0$ProcessRecord() {
        kill("anr", true);
    }

    public /* synthetic */ void lambda$appNotResponding$1$ProcessRecord() {
        kill("anr", true);
    }

    public /* synthetic */ void lambda$appNotResponding$2$ProcessRecord() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mServices.scheduleServiceTimeoutLocked(this);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void makeAppNotRespondingLocked(String activity, String shortMsg, String longMsg) {
        setNotResponding(true);
        if (this.mService.mAppErrors != null) {
            this.notRespondingReport = this.mService.mAppErrors.generateProcessError(this, 2, activity, shortMsg, longMsg, null);
        }
        startAppProblemLocked();
        getWindowProcessController().stopFreezingActivities();
    }

    /* access modifiers changed from: package-private */
    public void startAppProblemLocked() {
        this.errorReportReceiver = null;
        for (int userId2 : this.mService.mUserController.getCurrentProfileIds()) {
            if (this.userId == userId2) {
                this.errorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mService.mContext, this.info.packageName, this.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(this);
    }

    private boolean isInterestingForBackgroundTraces() {
        if (this.pid == ActivityManagerService.MY_PID || isInterestingToUserLocked()) {
            return true;
        }
        ApplicationInfo applicationInfo = this.info;
        if ((applicationInfo == null || !"com.android.systemui".equals(applicationInfo.packageName)) && !hasTopUi() && !hasOverlayUi()) {
            return false;
        }
        return true;
    }

    public void updateApplicationInfo(ApplicationInfo appInfo) {
        if (appInfo != null && this.info != appInfo && appInfo.packageName.equals(this.info.packageName) && appInfo.uid == this.info.uid) {
            this.info = appInfo;
            this.mWindowProcessController.updateApplicationInfo(appInfo);
            int size = this.services.size();
            for (int i = 0; i < size; i++) {
                ServiceRecord sr = this.services.valueAt(i);
                if (sr.packageName.equals(appInfo.packageName) && sr.appInfo.uid == appInfo.uid && TextUtils.equals(sr.appInfo.className, appInfo.className)) {
                    sr.updateApplicationInfo(appInfo);
                }
            }
        }
    }

    private boolean getShowBackground() {
        return Settings.Secure.getInt(this.mService.mContext.getContentResolver(), "anr_show_background", 0) != 0;
    }

    public void makeAppeyeAppNotRespondingLocked(String activity, String shortMsg, String longMsg) {
        makeAppNotRespondingLocked(activity, shortMsg, longMsg);
        setNotResponding(false);
    }
}

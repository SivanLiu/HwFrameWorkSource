package com.android.server.am;

import android.app.ActivityManager.ProcessErrorStateInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport;
import android.app.ApplicationErrorReport.AnrInfo;
import android.app.ApplicationErrorReport.CrashInfo;
import android.app.Dialog;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.rms.HwSysResource;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.app.ProcessMap;
import com.android.internal.logging.MetricsLogger;
import com.android.server.RescueParty;
import com.android.server.Watchdog;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.zrhung.IZRHungService;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class AppErrors {
    private static final boolean IS_DEBUG_VERSION;
    private static final String TAG = "ActivityManager";
    public static final Set<String> whitelist_for_short_time = new ArraySet();
    private final IZrHung mAppEyeANR = HwFrameworkFactory.getZrHung("appeye_anr");
    private final IZrHung mAppEyeBinderBlock = HwFrameworkFactory.getZrHung("appeye_ssbinderfull");
    private HwSysResource mAppResource;
    private ArraySet<String> mAppsNotReportingCrashes;
    private final ProcessMap<BadProcessInfo> mBadProcesses = new ProcessMap();
    private final Context mContext;
    private HwCustAppErrors mHwCustAppErrors = ((HwCustAppErrors) HwCustUtils.createObj(HwCustAppErrors.class, new Object[0]));
    private final boolean mIsNotShowAnrDialog = SystemProperties.getBoolean("ro.config.noshow_anrdialog", false);
    private final boolean mIswhitelist_for_short_time = SystemProperties.getBoolean("persist.sys.hwgmstemporary", false);
    private final ProcessMap<Long> mProcessCrashTimes = new ProcessMap();
    private final ProcessMap<Long> mProcessCrashTimesPersistent = new ProcessMap();
    private final ActivityManagerService mService;

    static final class BadProcessInfo {
        final String longMsg;
        final String shortMsg;
        final String stack;
        final long time;

        BadProcessInfo(long time, String shortMsg, String longMsg, String stack) {
            this.time = time;
            this.shortMsg = shortMsg;
            this.longMsg = longMsg;
            this.stack = stack;
        }
    }

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        IS_DEBUG_VERSION = z;
        whitelist_for_short_time.add("com.google.android.gms");
        whitelist_for_short_time.add("com.google.android.gsf");
        whitelist_for_short_time.add("com.google.android.gsf.login");
        whitelist_for_short_time.add("com.google.android.marvin.talkback");
        whitelist_for_short_time.add("com.android.chrome");
        whitelist_for_short_time.add("com.google.android.apps.books");
        whitelist_for_short_time.add("com.android.vending");
        whitelist_for_short_time.add("com.google.android.apps.docs");
        whitelist_for_short_time.add("com.google.android.apps.magazines");
        whitelist_for_short_time.add("com.google.android.apps.maps");
        whitelist_for_short_time.add("com.google.android.apps.photos");
        whitelist_for_short_time.add("com.google.android.apps.plus");
        whitelist_for_short_time.add("com.google.android.backuptransport");
        whitelist_for_short_time.add("com.google.android.configupdater");
        whitelist_for_short_time.add("com.google.android.ext.services");
        whitelist_for_short_time.add("com.google.android.ext.shared");
        whitelist_for_short_time.add("com.google.android.feedback");
        whitelist_for_short_time.add("com.google.android.gm");
        whitelist_for_short_time.add("com.google.android.googlequicksearchbox");
        whitelist_for_short_time.add("com.google.android.play.games");
        whitelist_for_short_time.add("com.google.android.inputmethod.latin");
        whitelist_for_short_time.add("com.google.android.music");
        whitelist_for_short_time.add("com.google.android.onetimeinitializer");
        whitelist_for_short_time.add("com.google.android.partnersetup");
        whitelist_for_short_time.add("com.google.android.play.games");
        whitelist_for_short_time.add("com.google.android.printservice.recommendation");
        whitelist_for_short_time.add("com.google.android.setupwizard");
        whitelist_for_short_time.add("com.google.android.syncadapters.calendar");
        whitelist_for_short_time.add("com.google.android.syncadapters.contacts");
        whitelist_for_short_time.add("com.google.android.talk");
        whitelist_for_short_time.add("com.google.android.tts");
        whitelist_for_short_time.add("com.google.android.videos");
        whitelist_for_short_time.add("com.google.android.youtube");
    }

    AppErrors(Context context, ActivityManagerService service) {
        context.assertRuntimeOverlayThemable();
        this.mService = service;
        this.mContext = context;
        if (this.mAppEyeANR != null) {
            this.mAppEyeANR.init(null);
        }
    }

    void writeToProto(ProtoOutputStream proto, long fieldId, String dumpPackage) {
        ProtoOutputStream protoOutputStream = proto;
        Object obj = dumpPackage;
        if (!this.mProcessCrashTimes.getMap().isEmpty() || !this.mBadProcesses.getMap().isEmpty()) {
            long now;
            int uidCount;
            long token;
            long token2 = proto.start(fieldId);
            long now2 = SystemClock.uptimeMillis();
            protoOutputStream.write(1112396529665L, now2);
            long j = 1138166333441L;
            long j2 = 2246267895810L;
            if (!this.mProcessCrashTimes.getMap().isEmpty()) {
                ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
                int procCount = pmap.size();
                int ip = 0;
                while (ip < procCount) {
                    int uidCount2;
                    ArrayMap<String, SparseArray<Long>> pmap2;
                    int procCount2;
                    String pname;
                    long ctoken = protoOutputStream.start(j2);
                    String pname2 = (String) pmap.keyAt(ip);
                    SparseArray<Long> uids = (SparseArray) pmap.valueAt(ip);
                    now = now2;
                    uidCount = uids.size();
                    protoOutputStream.write(j, pname2);
                    int i = 0;
                    while (i < uidCount) {
                        int puid = uids.keyAt(i);
                        ProcessRecord r = (ProcessRecord) this.mService.mProcessNames.get(pname2, puid);
                        if (obj != null) {
                            if (r != null) {
                                uidCount2 = uidCount;
                                if (!r.pkgList.containsKey(obj)) {
                                    token = token2;
                                    pmap2 = pmap;
                                    procCount2 = procCount;
                                    pname = pname2;
                                }
                            } else {
                                uidCount2 = uidCount;
                                token = token2;
                                pmap2 = pmap;
                                procCount2 = procCount;
                                pname = pname2;
                            }
                            i++;
                            uidCount = uidCount2;
                            pmap = pmap2;
                            procCount = procCount2;
                            pname2 = pname;
                            token2 = token;
                        } else {
                            uidCount2 = uidCount;
                        }
                        pmap2 = pmap;
                        procCount2 = procCount;
                        pname = pname2;
                        long etoken = protoOutputStream.start(2);
                        protoOutputStream.write(1120986464257L, puid);
                        token = token2;
                        protoOutputStream.write(1112396529666L, ((Long) uids.valueAt(i)).longValue());
                        protoOutputStream.end(etoken);
                        i++;
                        uidCount = uidCount2;
                        pmap = pmap2;
                        procCount = procCount2;
                        pname2 = pname;
                        token2 = token;
                    }
                    uidCount2 = uidCount;
                    pmap2 = pmap;
                    procCount2 = procCount;
                    pname = pname2;
                    protoOutputStream.end(ctoken);
                    ip++;
                    now2 = now;
                    j = 1138166333441L;
                    j2 = 2246267895810L;
                }
            }
            token = token2;
            now = now2;
            if (!this.mBadProcesses.getMap().isEmpty()) {
                ArrayMap<String, SparseArray<BadProcessInfo>> pmap3 = this.mBadProcesses.getMap();
                int processCount = pmap3.size();
                uidCount = 0;
                while (uidCount < processCount) {
                    String pname3;
                    SparseArray<BadProcessInfo> uids2;
                    String str;
                    long btoken = protoOutputStream.start(2246267895811L);
                    String pname4 = (String) pmap3.keyAt(uidCount);
                    SparseArray<BadProcessInfo> uids3 = (SparseArray) pmap3.valueAt(uidCount);
                    int uidCount3 = uids3.size();
                    protoOutputStream.write(1138166333441L, pname4);
                    int i2 = 0;
                    while (i2 < uidCount3) {
                        ArrayMap<String, SparseArray<BadProcessInfo>> pmap4;
                        int puid2 = uids3.keyAt(i2);
                        ProcessRecord r2 = (ProcessRecord) this.mService.mProcessNames.get(pname4, puid2);
                        if (obj == null || (r2 != null && r2.pkgList.containsKey(obj))) {
                            BadProcessInfo info = (BadProcessInfo) uids3.valueAt(i2);
                            pmap4 = pmap3;
                            pname3 = pname4;
                            uids2 = uids3;
                            long etoken2 = protoOutputStream.start(2246267895810L);
                            protoOutputStream.write(1120986464257L, puid2);
                            protoOutputStream.write(2, info.time);
                            protoOutputStream.write(1138166333443L, info.shortMsg);
                            protoOutputStream.write(1138166333444L, info.longMsg);
                            protoOutputStream.write(1138166333445L, info.stack);
                            protoOutputStream.end(etoken2);
                        } else {
                            pmap4 = pmap3;
                            pname3 = pname4;
                            uids2 = uids3;
                        }
                        i2++;
                        pmap3 = pmap4;
                        pname4 = pname3;
                        uids3 = uids2;
                        str = dumpPackage;
                    }
                    pname3 = pname4;
                    uids2 = uids3;
                    protoOutputStream.end(btoken);
                    uidCount++;
                    str = dumpPackage;
                }
            }
            protoOutputStream.end(token);
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0055, code:
            if (r4.pkgList.containsKey(r2) == null) goto L_0x005a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean dumpLocked(FileDescriptor fd, PrintWriter pw, boolean needSep, String dumpPackage) {
        boolean needSep2;
        int ip;
        int i;
        AppErrors appErrors = this;
        PrintWriter printWriter = pw;
        String str = dumpPackage;
        if (appErrors.mProcessCrashTimes.getMap().isEmpty()) {
            needSep2 = needSep;
        } else {
            long now = SystemClock.uptimeMillis();
            ArrayMap<String, SparseArray<Long>> pmap = appErrors.mProcessCrashTimes.getMap();
            int processCount = pmap.size();
            needSep2 = needSep;
            boolean printed = false;
            ip = 0;
            while (ip < processCount) {
                int processCount2;
                String pname = (String) pmap.keyAt(ip);
                SparseArray<Long> uids = (SparseArray) pmap.valueAt(ip);
                int uidCount = uids.size();
                boolean printed2 = printed;
                i = 0;
                while (i < uidCount) {
                    ArrayMap<String, SparseArray<Long>> pmap2;
                    int puid = uids.keyAt(i);
                    ProcessRecord r = (ProcessRecord) appErrors.mService.mProcessNames.get(pname, puid);
                    if (str != null) {
                        if (r != null) {
                            pmap2 = pmap;
                        } else {
                            pmap2 = pmap;
                        }
                        processCount2 = processCount;
                        i++;
                        pmap = pmap2;
                        processCount = processCount2;
                    } else {
                        pmap2 = pmap;
                    }
                    if (!printed2) {
                        if (needSep2) {
                            pw.println();
                        }
                        needSep2 = true;
                        printWriter.println("  Time since processes crashed:");
                        printed2 = true;
                    }
                    printWriter.print("    Process ");
                    printWriter.print(pname);
                    printWriter.print(" uid ");
                    printWriter.print(puid);
                    printWriter.print(": last crashed ");
                    processCount2 = processCount;
                    TimeUtils.formatDuration(now - ((Long) uids.valueAt(i)).longValue(), printWriter);
                    printWriter.println(" ago");
                    i++;
                    pmap = pmap2;
                    processCount = processCount2;
                }
                processCount2 = processCount;
                ip++;
                printed = printed2;
            }
        }
        if (!appErrors.mBadProcesses.getMap().isEmpty()) {
            ArrayMap<String, SparseArray<BadProcessInfo>> pmap3 = appErrors.mBadProcesses.getMap();
            int processCount3 = pmap3.size();
            boolean printed3 = false;
            ip = 0;
            while (ip < processCount3) {
                int processCount4;
                String pname2 = (String) pmap3.keyAt(ip);
                SparseArray<BadProcessInfo> uids2 = (SparseArray) pmap3.valueAt(ip);
                i = uids2.size();
                boolean printed4 = printed3;
                int i2 = 0;
                while (i2 < i) {
                    ArrayMap<String, SparseArray<BadProcessInfo>> pmap4;
                    int puid2 = uids2.keyAt(i2);
                    ProcessRecord r2 = (ProcessRecord) appErrors.mService.mProcessNames.get(pname2, puid2);
                    if (str == null || (r2 != null && r2.pkgList.containsKey(str))) {
                        if (!printed4) {
                            if (needSep2) {
                                pw.println();
                            }
                            needSep2 = true;
                            printWriter.println("  Bad processes:");
                            printed4 = true;
                        }
                        BadProcessInfo info = (BadProcessInfo) uids2.valueAt(i2);
                        printWriter.print("    Bad process ");
                        printWriter.print(pname2);
                        printWriter.print(" uid ");
                        printWriter.print(puid2);
                        printWriter.print(": crashed at time ");
                        pmap4 = pmap3;
                        processCount4 = processCount3;
                        printWriter.println(info.time);
                        if (info.shortMsg != null) {
                            printWriter.print("      Short msg: ");
                            printWriter.println(info.shortMsg);
                        }
                        if (info.longMsg != null) {
                            printWriter.print("      Long msg: ");
                            printWriter.println(info.longMsg);
                        }
                        if (info.stack != null) {
                            printWriter.println("      Stack:");
                            processCount3 = null;
                            for (pmap3 = null; pmap3 < info.stack.length(); pmap3++) {
                                if (info.stack.charAt(pmap3) == 10) {
                                    printWriter.print("        ");
                                    printWriter.write(info.stack, processCount3, pmap3 - processCount3);
                                    pw.println();
                                    processCount3 = pmap3 + 1;
                                }
                            }
                            if (processCount3 < info.stack.length()) {
                                printWriter.print("        ");
                                printWriter.write(info.stack, processCount3, info.stack.length() - processCount3);
                                pw.println();
                            }
                        }
                    } else {
                        pmap4 = pmap3;
                        processCount4 = processCount3;
                    }
                    i2++;
                    pmap3 = pmap4;
                    processCount3 = processCount4;
                    appErrors = this;
                }
                processCount4 = processCount3;
                ip++;
                printed3 = printed4;
                appErrors = this;
            }
        }
        return needSep2;
    }

    boolean isBadProcessLocked(ApplicationInfo info) {
        return this.mBadProcesses.get(info.processName, info.uid) != null;
    }

    void clearBadProcessLocked(ApplicationInfo info) {
        this.mBadProcesses.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(ApplicationInfo info) {
        this.mProcessCrashTimes.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(boolean resetEntireUser, int appId, int userId) {
        ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
        for (int ip = pmap.size() - 1; ip >= 0; ip--) {
            SparseArray<Long> ba = (SparseArray) pmap.valueAt(ip);
            for (int i = ba.size() - 1; i >= 0; i--) {
                boolean remove = false;
                int entUid = ba.keyAt(i);
                if (resetEntireUser) {
                    if (UserHandle.getUserId(entUid) == userId) {
                        remove = true;
                    }
                } else if (userId == -1) {
                    if (UserHandle.getAppId(entUid) == appId) {
                        remove = true;
                    }
                } else if (entUid == UserHandle.getUid(userId, appId)) {
                    remove = true;
                }
                if (remove) {
                    ba.removeAt(i);
                }
            }
            if (ba.size() == 0) {
                pmap.removeAt(ip);
            }
        }
    }

    void loadAppsNotReportingCrashesFromConfigLocked(String appsNotReportingCrashesConfig) {
        if (appsNotReportingCrashesConfig != null) {
            String[] split = appsNotReportingCrashesConfig.split(",");
            if (split.length > 0) {
                this.mAppsNotReportingCrashes = new ArraySet();
                Collections.addAll(this.mAppsNotReportingCrashes, split);
            }
        }
    }

    void killAppAtUserRequestLocked(ProcessRecord app, Dialog fromDialog) {
        app.crashing = false;
        app.crashingReport = null;
        app.notResponding = false;
        app.notRespondingReport = null;
        if (app.anrDialog == fromDialog) {
            app.anrDialog = null;
        }
        if (app.waitDialog == fromDialog) {
            app.waitDialog = null;
        }
        if (app.pid > 0 && app.pid != ActivityManagerService.MY_PID) {
            handleAppCrashLocked(app, "user-terminated", null, null, null, null);
            app.kill("user request after error", true);
        }
    }

    void scheduleAppCrashLocked(int uid, int initialPid, String packageName, int userId, String message) {
        ProcessRecord proc = null;
        synchronized (this.mService.mPidsSelfLocked) {
            for (int i = 0; i < this.mService.mPidsSelfLocked.size(); i++) {
                ProcessRecord p = (ProcessRecord) this.mService.mPidsSelfLocked.valueAt(i);
                if (uid < 0 || p.uid == uid) {
                    if (p.pid == initialPid) {
                        proc = p;
                        break;
                    } else if (p.pkgList.containsKey(packageName) && (userId < 0 || p.userId == userId)) {
                        proc = p;
                    }
                }
            }
        }
        if (proc == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("crashApplication: nothing for uid=");
            stringBuilder.append(uid);
            stringBuilder.append(" initialPid=");
            stringBuilder.append(initialPid);
            stringBuilder.append(" packageName=");
            stringBuilder.append(packageName);
            stringBuilder.append(" userId=");
            stringBuilder.append(userId);
            Slog.w("ActivityManager", stringBuilder.toString());
            return;
        }
        proc.scheduleCrash(message);
    }

    void crashApplication(ProcessRecord r, CrashInfo crashInfo) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            crashApplicationInner(r, crashInfo, callingPid, callingUid);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX WARNING: Missing block: B:107:0x01e8, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:108:0x01eb, code:
            if (r1 == null) goto L_0x0202;
     */
    /* JADX WARNING: Missing block: B:110:?, code:
            r11.mContext.startActivityAsUser(r1, new android.os.UserHandle(r12.userId));
     */
    /* JADX WARNING: Missing block: B:111:0x01fa, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:112:0x01fb, code:
            android.util.Slog.w("ActivityManager", "bug report receiver dissappeared", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void crashApplicationInner(ProcessRecord r, CrashInfo crashInfo, int callingPid, int callingUid) {
        Throwable th;
        AppErrorResult appErrorResult;
        CrashInfo crashInfo2;
        long orig;
        ProcessRecord processRecord = r;
        CrashInfo crashInfo3 = crashInfo;
        long timeMillis = System.currentTimeMillis();
        String shortMsg = crashInfo3.exceptionClassName;
        String longMsg = crashInfo3.exceptionMessage;
        String stackTrace = crashInfo3.stackTrace;
        if (shortMsg != null && longMsg != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(shortMsg);
            stringBuilder.append(": ");
            stringBuilder.append(longMsg);
            longMsg = stringBuilder.toString();
        } else if (shortMsg != null) {
            longMsg = shortMsg;
        }
        String longMsg2 = longMsg;
        if (processRecord != null && processRecord.persistent) {
            RescueParty.notePersistentAppCrash(this.mContext, processRecord.uid);
        }
        AppErrorResult result = new AppErrorResult();
        ActivityManagerService activityManagerService = this.mService;
        synchronized (activityManagerService) {
            ActivityManagerService activityManagerService2;
            String stackTrace2;
            String shortMsg2;
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                CrashInfo crashInfo4 = crashInfo3;
                AppErrorResult result2 = result;
                activityManagerService2 = activityManagerService;
                stackTrace2 = stackTrace;
                shortMsg2 = shortMsg;
                try {
                    if (handleAppCrashInActivityController(processRecord, crashInfo4, shortMsg, longMsg2, stackTrace, timeMillis, callingPid, callingUid)) {
                        try {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            appErrorResult = result2;
                            crashInfo2 = crashInfo;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    if (processRecord != null) {
                        if (processRecord.instr != null) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                    if (processRecord != null) {
                        this.mService.mBatteryStatsService.noteProcessCrash(processRecord.processName, processRecord.uid);
                    }
                    Data data = new Data();
                    data.result = result2;
                    data.proc = processRecord;
                    if (processRecord == null) {
                        crashInfo2 = crashInfo;
                    } else if (makeAppCrashingLocked(processRecord, shortMsg2, longMsg2, stackTrace2, data)) {
                        Message msg = Message.obtain();
                        msg.what = 1;
                        TaskRecord task = data.task;
                        msg.obj = data;
                        this.mService.mUiHandler.sendMessage(msg);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        int res = result2.get();
                        Intent appErrorIntent = null;
                        MetricsLogger.action(this.mContext, 316, res);
                        if (res == 6 || res == 7) {
                            res = 1;
                        }
                        int res2 = res;
                        synchronized (this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                if (res2 == 5) {
                                    try {
                                        stopReportingCrashesLocked(r);
                                    } catch (IllegalArgumentException e) {
                                        Set<String> cats = task.intent != null ? task.intent.getCategories() : null;
                                        if (cats != null && cats.contains("android.intent.category.LAUNCHER")) {
                                            this.mService.getActivityStartController().startActivityInPackage(task.mCallingUid, callingPid, callingUid, task.mCallingPackage, task.intent, null, null, null, 0, 0, new SafeActivityOptions(ActivityOptions.makeBasic()), task.userId, null, "AppErrors", false);
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        appErrorResult = result2;
                                        crashInfo2 = crashInfo;
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                if (res2 == 3) {
                                    this.mService.removeProcessLocked(processRecord, false, true, "crash");
                                    ActivityOptions options = ActivityOptions.makeBasic();
                                    if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(processRecord.mDisplayId)) {
                                        options.setLaunchDisplayId(processRecord.mDisplayId);
                                    }
                                    if (task != null) {
                                        this.mService.startActivityFromRecents(task.taskId, options.toBundle());
                                    }
                                }
                                if (res2 == 1) {
                                    orig = Binder.clearCallingIdentity();
                                    this.mService.mStackSupervisor.handleAppCrashLocked(processRecord);
                                    if (!processRecord.persistent) {
                                        this.mService.removeProcessLocked(processRecord, false, false, "crash");
                                        this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                                    }
                                    Binder.restoreCallingIdentity(orig);
                                }
                                if (res2 == 8) {
                                    appErrorIntent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("package:");
                                    stringBuilder2.append(processRecord.info.packageName);
                                    appErrorIntent.setData(Uri.parse(stringBuilder2.toString()));
                                    appErrorIntent.addFlags(268435456);
                                }
                                if (res2 == 2) {
                                    try {
                                        appErrorIntent = createAppErrorIntentLocked(processRecord, timeMillis, crashInfo);
                                    } catch (Throwable th4) {
                                        th = th4;
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                crashInfo2 = crashInfo;
                                if (!(processRecord == null || processRecord.isolated || res2 == 3)) {
                                    this.mProcessCrashTimes.put(processRecord.info.processName, processRecord.uid, Long.valueOf(SystemClock.uptimeMillis()));
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                appErrorResult = result2;
                                crashInfo2 = crashInfo;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                    } else {
                        appErrorResult = result2;
                        crashInfo2 = crashInfo;
                    }
                    try {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th6) {
                        th = th6;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    appErrorResult = result2;
                    crashInfo2 = crashInfo;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th8) {
                th = th8;
                appErrorResult = result;
                activityManagerService2 = activityManagerService;
                stackTrace2 = stackTrace;
                shortMsg2 = shortMsg;
                crashInfo2 = crashInfo3;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private boolean handleAppCrashInActivityController(ProcessRecord r, CrashInfo crashInfo, String shortMsg, String longMsg, String stackTrace, long timeMillis, int callingPid, int callingUid) {
        ProcessRecord processRecord = r;
        CrashInfo crashInfo2 = crashInfo;
        if (this.mService.mController == null) {
            return false;
        }
        RemoteException e;
        if (processRecord != null) {
            try {
                e = processRecord.processName;
            } catch (RemoteException e2) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        } else {
            e = null;
        }
        int pid = processRecord != null ? processRecord.pid : callingPid;
        int uid = processRecord != null ? processRecord.info.uid : callingUid;
        if (!this.mService.mController.appCrashed(e, pid, shortMsg, longMsg, timeMillis, crashInfo2.stackTrace)) {
            StringBuilder stringBuilder;
            int i;
            int i2;
            if ("1".equals(SystemProperties.get("ro.debuggable", "0")) && "Native crash".equals(crashInfo2.exceptionClassName)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skip killing native crashed app ");
                stringBuilder.append(e);
                stringBuilder.append("(");
                stringBuilder.append(pid);
                stringBuilder.append(") during testing");
                Slog.w("ActivityManager", stringBuilder.toString());
                i = uid;
                i2 = pid;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Force-killing crashed app ");
                stringBuilder.append(e);
                stringBuilder.append(" at watcher's request");
                Slog.w("ActivityManager", stringBuilder.toString());
                if (processRecord != null) {
                    if (!makeAppCrashingLocked(processRecord, shortMsg, longMsg, stackTrace, null)) {
                        processRecord.kill("crash", true);
                    }
                } else {
                    i = uid;
                    i2 = pid;
                    Process.killProcess(i2);
                    ActivityManagerService.killProcessGroup(i, i2);
                }
            }
            return true;
        }
        return false;
    }

    private boolean makeAppCrashingLocked(ProcessRecord app, String shortMsg, String longMsg, String stackTrace, Data data) {
        app.crashing = true;
        ProcessRecord processRecord = app;
        app.crashingReport = generateProcessError(processRecord, 1, null, shortMsg, longMsg, stackTrace);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
        return handleAppCrashLocked(processRecord, "force-crash", shortMsg, longMsg, stackTrace, data);
    }

    void startAppProblemLocked(ProcessRecord app) {
        app.errorReportReceiver = null;
        for (int userId : this.mService.mUserController.getCurrentProfileIds()) {
            if (app.userId == userId) {
                app.errorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mContext, app.info.packageName, app.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(app);
    }

    private ProcessErrorStateInfo generateProcessError(ProcessRecord app, int condition, String activity, String shortMsg, String longMsg, String stackTrace) {
        ProcessErrorStateInfo report = new ProcessErrorStateInfo();
        report.condition = condition;
        report.processName = app.processName;
        report.pid = app.pid;
        report.uid = app.info.uid;
        report.tag = activity;
        report.shortMsg = shortMsg;
        report.longMsg = longMsg;
        report.stackTrace = stackTrace;
        return report;
    }

    Intent createAppErrorIntentLocked(ProcessRecord r, long timeMillis, CrashInfo crashInfo) {
        ApplicationErrorReport report = createAppErrorReportLocked(r, timeMillis, crashInfo);
        if (report == null) {
            return null;
        }
        Intent result = new Intent("android.intent.action.APP_ERROR");
        result.setComponent(r.errorReportReceiver);
        result.putExtra("android.intent.extra.BUG_REPORT", report);
        result.addFlags(268435456);
        return result;
    }

    private ApplicationErrorReport createAppErrorReportLocked(ProcessRecord r, long timeMillis, CrashInfo crashInfo) {
        if (r.errorReportReceiver == null) {
            return null;
        }
        if (!r.crashing && !r.notResponding && !r.forceCrashReport) {
            return null;
        }
        ApplicationErrorReport report = new ApplicationErrorReport();
        report.packageName = r.info.packageName;
        report.installerPackageName = r.errorReportReceiver.getPackageName();
        report.processName = r.processName;
        report.time = timeMillis;
        report.systemApp = (r.info.flags & 1) != 0;
        if (r.crashing || r.forceCrashReport) {
            report.type = 1;
            report.crashInfo = crashInfo;
        } else if (r.notResponding) {
            report.type = 2;
            report.anrInfo = new AnrInfo();
            report.anrInfo.activity = r.notRespondingReport.tag;
            report.anrInfo.cause = r.notRespondingReport.shortMsg;
            report.anrInfo.info = r.notRespondingReport.longMsg;
        }
        return report;
    }

    /* JADX WARNING: Removed duplicated region for block: B:83:0x01f1 A:{SYNTHETIC, EDGE_INSN: B:83:0x01f1->B:70:0x01f1 ?: BREAK  , EDGE_INSN: B:83:0x01f1->B:70:0x01f1 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x01b7  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0216  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x01f5  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x021c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean handleAppCrashLocked(ProcessRecord app, String reason, String shortMsg, String longMsg, String stackTrace, Data data) {
        Long crashTime;
        Long crashTimePersistent;
        int i;
        boolean z;
        ArrayList<ActivityRecord> activities;
        int activityNdx;
        ProcessRecord processRecord = app;
        Data data2 = data;
        long now = SystemClock.uptimeMillis();
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        boolean procIsBoundForeground = processRecord.curProcState == 4;
        if (processRecord.isolated) {
            crashTime = null;
            crashTimePersistent = null;
        } else {
            crashTime = (Long) this.mProcessCrashTimes.get(processRecord.info.processName, processRecord.uid);
            crashTimePersistent = (Long) this.mProcessCrashTimesPersistent.get(processRecord.info.processName, processRecord.uid);
        }
        Long crashTimePersistent2 = crashTimePersistent;
        Long crashTime2 = crashTime;
        int i2 = processRecord.services.size() - 1;
        boolean tryAgain = false;
        while (true) {
            i = i2;
            if (i < 0) {
                break;
            }
            ServiceRecord sr = (ServiceRecord) processRecord.services.valueAt(i);
            if (now > sr.restartTime + 60000) {
                sr.crashCount = 1;
            } else {
                sr.crashCount++;
            }
            if (((long) sr.crashCount) < this.mService.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (sr.isForeground || procIsBoundForeground)) {
                tryAgain = true;
            }
            i2 = i - 1;
        }
        long now2;
        boolean z2;
        boolean tryAgain2;
        Long crashTimePersistent3;
        if (crashTime2 == null || now >= crashTime2.longValue() + 60000) {
            now2 = now;
            z2 = procIsBoundForeground;
            tryAgain2 = tryAgain;
            crashTimePersistent3 = crashTimePersistent2;
            TaskRecord affectedTask = this.mService.mStackSupervisor.finishTopCrashedActivitiesLocked(processRecord, reason);
            if (data2 != null) {
                data2.task = affectedTask;
            }
            if (!(data2 == null || crashTimePersistent3 == null || now2 >= crashTimePersistent3.longValue() + 60000)) {
                z = true;
                data2.repeating = true;
                if (data2 != null && tryAgain2) {
                    data2.isRestartableForService = z;
                }
                activities = processRecord.activities;
                if (processRecord == this.mService.mHomeProcess && activities.size() > 0 && (this.mService.mHomeProcess.info.flags & 1) == 0) {
                    i = activities.size() - 1;
                    while (true) {
                        activityNdx = i;
                        if (activityNdx >= 0) {
                            break;
                        }
                        ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
                        if (r.isActivityTypeHome()) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Clearing package preferred activities from ");
                            stringBuilder.append(r.packageName);
                            Log.i("ActivityManager", stringBuilder.toString());
                            try {
                                ActivityThread.getPackageManager().clearPackagePreferredActivities(r.packageName);
                            } catch (RemoteException e) {
                            }
                            this.mService.showUninstallLauncherDialog(r.packageName);
                        }
                        i = activityNdx - 1;
                    }
                }
                if (processRecord.isolated) {
                    long now3 = now2;
                    this.mProcessCrashTimes.put(processRecord.info.processName, processRecord.uid, Long.valueOf(now3));
                    this.mProcessCrashTimesPersistent.put(processRecord.info.processName, processRecord.uid, Long.valueOf(now3));
                }
                if (processRecord.crashHandler != null) {
                    this.mService.mHandler.post(processRecord.crashHandler);
                }
                return true;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Process ");
        stringBuilder2.append(processRecord.info.processName);
        stringBuilder2.append(" has crashed too many times: killing!");
        Slog.w("ActivityManager", stringBuilder2.toString());
        EventLog.writeEvent(EventLogTags.AM_PROCESS_CRASHED_TOO_MUCH, new Object[]{Integer.valueOf(processRecord.userId), processRecord.info.processName, Integer.valueOf(processRecord.uid)});
        this.mService.mStackSupervisor.handleAppCrashLocked(processRecord);
        if (processRecord.persistent) {
            now2 = now;
            z2 = procIsBoundForeground;
            tryAgain2 = tryAgain;
            crashTimePersistent3 = crashTimePersistent2;
        } else {
            EventLog.writeEvent(EventLogTags.AM_PROC_BAD, new Object[]{Integer.valueOf(processRecord.userId), Integer.valueOf(processRecord.uid), processRecord.info.processName});
            if (processRecord.isolated) {
                now2 = now;
                z2 = procIsBoundForeground;
                tryAgain2 = tryAgain;
                crashTimePersistent3 = crashTimePersistent2;
            } else {
                ProcessMap processMap = this.mBadProcesses;
                String str = processRecord.info.processName;
                int i3 = processRecord.uid;
                BadProcessInfo badProcessInfo = r4;
                long j = now;
                now2 = now;
                tryAgain2 = tryAgain;
                BadProcessInfo badProcessInfo2 = new BadProcessInfo(j, shortMsg, longMsg, stackTrace);
                processMap.put(str, i3, badProcessInfo);
                this.mProcessCrashTimes.remove(processRecord.info.processName, processRecord.uid);
            }
            processRecord.bad = true;
            processRecord.removed = true;
            this.mService.removeProcessLocked(processRecord, false, tryAgain2, "crash");
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
            if (!showBackground) {
                return false;
            }
        }
        this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        String str2 = reason;
        z = true;
        data2.isRestartableForService = z;
        activities = processRecord.activities;
        i = activities.size() - 1;
        while (true) {
            activityNdx = i;
            if (activityNdx >= 0) {
            }
            i = activityNdx - 1;
        }
        if (processRecord.isolated) {
        }
        if (processRecord.crashHandler != null) {
        }
        return true;
    }

    private boolean hasForegroundUI(ProcessRecord proc) {
        boolean hasForegroundUI = proc != null && proc.foregroundActivities;
        if (hasForegroundUI || proc == null) {
            return hasForegroundUI;
        }
        String packageName = proc.info.packageName;
        List<RunningTaskInfo> taskInfo = this.mService.getTasks(1);
        if (taskInfo == null || taskInfo.size() <= 0) {
            return hasForegroundUI;
        }
        ComponentName componentInfo = ((RunningTaskInfo) taskInfo.get(0)).topActivity;
        if (componentInfo == null || packageName == null || !packageName.equalsIgnoreCase(componentInfo.getPackageName())) {
            return hasForegroundUI;
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:163:0x0226 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x01c3 A:{SYNTHETIC, Splitter: B:133:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0166 A:{SYNTHETIC, Splitter: B:98:0x0166} */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x01c3 A:{SYNTHETIC, Splitter: B:133:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x01d6 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00e6 A:{SYNTHETIC, Splitter: B:65:0x00e6} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0134 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0132 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x014e A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x014c A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x0153 A:{SYNTHETIC, Splitter: B:90:0x0153} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0166 A:{SYNTHETIC, Splitter: B:98:0x0166} */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x0198 A:{Catch:{ all -> 0x0035 }} */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x01c3 A:{SYNTHETIC, Splitter: B:133:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x01d6 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Missing block: B:27:0x0073, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:28:0x0076, code:
            return;
     */
    /* JADX WARNING: Missing block: B:50:0x00c1, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:51:0x00c4, code:
            return;
     */
    /* JADX WARNING: Missing block: B:77:0x011f, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:78:0x0122, code:
            return;
     */
    /* JADX WARNING: Missing block: B:179:0x02b4, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
            r0 = r10;
            r2 = r12;
     */
    /* JADX WARNING: Missing block: B:180:0x02b9, code:
            if (r7 == null) goto L_0x02dc;
     */
    /* JADX WARNING: Missing block: B:181:0x02bb, code:
            r5 = new java.lang.StringBuilder();
            r5.append("Showing crash dialog for package ");
            r5.append(r0);
            r5.append(" u");
            r5.append(r2);
            android.util.Slog.i("ActivityManager", r5.toString());
            r7.show();
     */
    /* JADX WARNING: Missing block: B:182:0x02dc, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleShowAppErrorUi(Message msg) {
        Throwable th;
        boolean z;
        Data data = msg.obj;
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        AppErrorDialog dialogToShow = null;
        synchronized (this.mService) {
            ActivityManagerService.boostPriorityForLockedSection();
            ProcessRecord proc = data.proc;
            AppErrorResult res = data.result;
            if (proc == null) {
                try {
                    Slog.e("ActivityManager", "handleShowAppErrorUi: proc is null");
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    z = showBackground;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            StringBuilder stringBuilder;
            String packageName = proc.info.packageName;
            boolean isDebuggable = "1".equals(SystemProperties.get("ro.debuggable", "0"));
            if (!(isDebuggable || proc == null)) {
                if (proc.forceCrashReport) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping native crash dialog of ");
                    stringBuilder.append(proc);
                    Slog.w("ActivityManager", stringBuilder.toString());
                    if (res != null) {
                        res.set(AppErrorDialog.CANT_SHOW);
                    }
                }
            }
            if (packageName != null) {
                if (this.mIswhitelist_for_short_time && whitelist_for_short_time.contains(packageName)) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                } else if (this.mService.getRecordCust() != null) {
                    this.mService.getRecordCust().appExitRecord(packageName, "crash");
                }
            }
            try {
                int userId = proc.userId;
                if (proc.crashDialog != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("App already has crash dialog: ");
                    stringBuilder.append(proc);
                    Slog.e("ActivityManager", stringBuilder.toString());
                    if (res != null) {
                        res.set(AppErrorDialog.ALREADY_SHOWING);
                    }
                } else {
                    boolean isBackground;
                    boolean isBackground2;
                    boolean showFirstCrash;
                    boolean crashSilenced;
                    boolean isShowCrash;
                    if (UserHandle.getAppId(proc.uid) >= 10000) {
                        if (proc.pid != ActivityManagerService.MY_PID) {
                            isBackground = true;
                            isBackground2 = isBackground;
                            for (int profileId : this.mService.mUserController.getCurrentProfileIds()) {
                                isBackground2 &= userId != profileId ? 1 : 0;
                            }
                            if (isBackground2 || showBackground) {
                                showFirstCrash = Global.getInt(this.mContext.getContentResolver(), "show_first_crash_dialog", 0) == 0;
                                isBackground = Secure.getIntForUser(this.mContext.getContentResolver(), "show_first_crash_dialog_dev_option", 0, this.mService.mUserController.getCurrentUserId()) == 0;
                                if (this.mAppsNotReportingCrashes != null) {
                                    if (this.mAppsNotReportingCrashes.contains(proc.info.packageName)) {
                                        crashSilenced = true;
                                        if (this.mHwCustAppErrors != null) {
                                            if (this.mHwCustAppErrors.isCustom()) {
                                                isShowCrash = (this.mService.canShowErrorDialogs() || showBackground) && !crashSilenced && ((showFirstCrash || isBackground || data.repeating) && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode() && hasForegroundUI(proc));
                                                if (this.mAppResource == null) {
                                                    Slog.i("ActivityManager", "get AppResource");
                                                    this.mAppResource = HwFrameworkFactory.getHwResource(18);
                                                }
                                                if (this.mAppResource == null && this.mService.canShowErrorDialogs() && !crashSilenced && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode() && hasForegroundUI(proc)) {
                                                    int i;
                                                    if ((proc.info.flags & 1) != 0) {
                                                        if ((proc.info.hwFlags & DumpState.DUMP_HANDLE) == 0 && (proc.info.hwFlags & 67108864) == 0) {
                                                            i = 2;
                                                            if (2 == this.mAppResource.acquire(proc.uid, proc.info.packageName, i)) {
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("Failed to acquire AppResource:");
                                                                stringBuilder.append(proc.info.packageName);
                                                                stringBuilder.append(SliceAuthority.DELIMITER);
                                                                stringBuilder.append(proc.uid);
                                                                Slog.w("ActivityManager", stringBuilder.toString());
                                                            }
                                                        }
                                                    }
                                                    i = 0;
                                                    try {
                                                        if (2 == this.mAppResource.acquire(proc.uid, proc.info.packageName, i)) {
                                                        }
                                                    } catch (Throwable th3) {
                                                        th = th3;
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                z = showBackground;
                                                if (!isShowCrash) {
                                                    Context context = this.mContext;
                                                    if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(proc.mDisplayId)) {
                                                        Context tmpContext = HwPCUtils.getDisplayContext(context, proc.mDisplayId);
                                                        if (tmpContext != null) {
                                                            context = tmpContext;
                                                        }
                                                    }
                                                    Dialog appErrorDialog = new AppErrorDialog(context, this.mService, data);
                                                    dialogToShow = appErrorDialog;
                                                    proc.crashDialog = appErrorDialog;
                                                    if (this.mAppEyeANR != null) {
                                                        ZrHungData arg = new ZrHungData();
                                                        arg.putString("WpName", "APP_CRASH");
                                                        arg.putString("packageName", proc.info.packageName);
                                                        arg.putString(IZRHungService.PARA_PROCNAME, proc.processName);
                                                        arg.putInt(IZRHungService.PARAM_PID, proc.pid);
                                                        arg.putBoolean("isRepeating", data.repeating);
                                                        this.mAppEyeANR.sendEvent(arg);
                                                    }
                                                } else if (res != null) {
                                                    res.set(AppErrorDialog.CANT_SHOW);
                                                }
                                            }
                                        }
                                        isShowCrash = isDebuggable && ((this.mService.canShowErrorDialogs() || showBackground) && !crashSilenced && ((showFirstCrash || isBackground || data.repeating) && !HwFrameworkFactory.getVRSystemServiceManager().isVRMode() && hasForegroundUI(proc)));
                                        if (this.mAppResource == null) {
                                        }
                                        if (this.mAppResource == null) {
                                        }
                                        z = showBackground;
                                        if (!isShowCrash) {
                                        }
                                    }
                                }
                                crashSilenced = false;
                                if (this.mHwCustAppErrors != null) {
                                }
                                if (!isDebuggable) {
                                }
                                if (this.mAppResource == null) {
                                }
                                if (this.mAppResource == null) {
                                }
                                z = showBackground;
                                if (!isShowCrash) {
                                }
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Skipping crash dialog of ");
                                stringBuilder.append(proc);
                                stringBuilder.append(": background");
                                Slog.w("ActivityManager", stringBuilder.toString());
                                if (res != null) {
                                    res.set(AppErrorDialog.BACKGROUND_USER);
                                }
                            }
                        }
                    }
                    isBackground = false;
                    isBackground2 = isBackground;
                    while (isBackground < this.mService.mUserController.getCurrentProfileIds().length) {
                    }
                    if (isBackground2) {
                    }
                    if (Global.getInt(this.mContext.getContentResolver(), "show_first_crash_dialog", 0) == 0) {
                    }
                    if (Secure.getIntForUser(this.mContext.getContentResolver(), "show_first_crash_dialog_dev_option", 0, this.mService.mUserController.getCurrentUserId()) == 0) {
                    }
                    if (this.mAppsNotReportingCrashes != null) {
                    }
                    crashSilenced = false;
                    if (this.mHwCustAppErrors != null) {
                    }
                    if (isDebuggable) {
                    }
                    if (this.mAppResource == null) {
                    }
                    if (this.mAppResource == null) {
                    }
                    z = showBackground;
                    if (!isShowCrash) {
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                z = showBackground;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void stopReportingCrashesLocked(ProcessRecord proc) {
        if (this.mAppsNotReportingCrashes == null) {
            this.mAppsNotReportingCrashes = new ArraySet();
        }
        this.mAppsNotReportingCrashes.add(proc.info.packageName);
    }

    static boolean isInterestingForBackgroundTraces(ProcessRecord app) {
        boolean z = true;
        if (app.pid == ActivityManagerService.MY_PID) {
            return true;
        }
        if (!(app.isInterestingToUserLocked() || ((app.info != null && "com.android.systemui".equals(app.info.packageName)) || app.hasTopUi || app.hasOverlayUi))) {
            z = false;
        }
        return z;
    }

    final void appNotResponding(ProcessRecord app, ActivityRecord activity, ActivityRecord parent, boolean aboveSystem, String annotation) {
        appNotResponding(app.pid, app, activity, parent, aboveSystem, annotation);
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.am.AppErrors.appNotResponding(int, com.android.server.am.ProcessRecord, com.android.server.am.ActivityRecord, com.android.server.am.ActivityRecord, boolean, java.lang.String):void, dom blocks: [B:230:0x0534, B:234:0x053c]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0245 A:{SYNTHETIC, Splitter: B:70:0x0245} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0412  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x03eb  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x041d  */
    /* JADX WARNING: Removed duplicated region for block: B:177:0x041a  */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0425  */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x044c  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x0455  */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x0452  */
    /* JADX WARNING: Removed duplicated region for block: B:193:0x0478 A:{SYNTHETIC, Splitter: B:193:0x0478} */
    /* JADX WARNING: Removed duplicated region for block: B:306:0x0648 A:{SYNTHETIC, Splitter: B:306:0x0648} */
    /* JADX WARNING: Removed duplicated region for block: B:310:0x0650 A:{Catch:{ IOException -> 0x064c }} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x0567 A:{SYNTHETIC, Splitter: B:254:0x0567} */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x03eb  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0412  */
    /* JADX WARNING: Removed duplicated region for block: B:177:0x041a  */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x041d  */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x0425  */
    /* JADX WARNING: Removed duplicated region for block: B:186:0x044c  */
    /* JADX WARNING: Removed duplicated region for block: B:187:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x0452  */
    /* JADX WARNING: Removed duplicated region for block: B:190:0x0455  */
    /* JADX WARNING: Removed duplicated region for block: B:193:0x0478 A:{SYNTHETIC, Splitter: B:193:0x0478} */
    final void appNotResponding(int r42, com.android.server.am.ProcessRecord r43, com.android.server.am.ActivityRecord r44, com.android.server.am.ActivityRecord r45, boolean r46, java.lang.String r47) {
        /*
        r41 = this;
        r1 = r41;
        r2 = r42;
        r13 = r43;
        r14 = r44;
        r15 = r45;
        r12 = r47;
        r0 = IS_DEBUG_VERSION;
        r11 = 0;
        if (r0 == 0) goto L_0x0037;
    L_0x0011:
        r0 = new android.util.ArrayMap;
        r0.<init>();
        r3 = "checkType";
        r4 = "FocusWindowNullScene";
        r0.put(r3, r4);
        r3 = "anrActivityName";
        if (r14 == 0) goto L_0x0026;
    L_0x0021:
        r4 = r44.toString();
        goto L_0x0027;
    L_0x0026:
        r4 = r11;
    L_0x0027:
        r0.put(r3, r4);
        r3 = com.android.server.HwServiceFactory.getWinFreezeScreenMonitor();
        if (r3 == 0) goto L_0x0037;
    L_0x0030:
        r3 = com.android.server.HwServiceFactory.getWinFreezeScreenMonitor();
        r3.cancelCheckFreezeScreen(r0);
    L_0x0037:
        r0 = new java.util.ArrayList;
        r3 = 5;
        r0.<init>(r3);
        r10 = r0;
        r0 = new android.util.SparseArray;
        r4 = 20;
        r0.<init>(r4);
        r9 = r0;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r4 = "anr_event_sync: appPid=";
        r0.append(r4);
        r4 = r13.pid;
        r0.append(r4);
        r4 = ", appName=";
        r0.append(r4);
        r4 = r13.processName;
        r0.append(r4);
        r4 = ", category=";
        r0.append(r4);
        r0.append(r12);
        r8 = r0.toString();
        r4 = 64;
        android.os.Trace.traceBegin(r4, r8);
        android.os.Trace.traceEnd(r4);
        r0 = android.util.Log.HWINFO;
        if (r0 == 0) goto L_0x0082;
    L_0x0077:
        r0 = android.common.HwFrameworkFactory.getLogException();
        r4 = "action";
        r5 = "copy_systrace_to_cache";
        r0.cmd(r4, r5);
    L_0x0082:
        r0 = r1.mService;
        r0 = r0.mController;
        r7 = 1;
        if (r0 == 0) goto L_0x00af;
    L_0x0089:
        r0 = r1.mService;	 Catch:{ RemoteException -> 0x00a3 }
        r0 = r0.mController;	 Catch:{ RemoteException -> 0x00a3 }
        r4 = r13.processName;	 Catch:{ RemoteException -> 0x00a3 }
        r5 = r13.pid;	 Catch:{ RemoteException -> 0x00a3 }
        r0 = r0.appEarlyNotResponding(r4, r5, r12);	 Catch:{ RemoteException -> 0x00a3 }
        if (r0 >= 0) goto L_0x00a2;	 Catch:{ RemoteException -> 0x00a3 }
    L_0x0097:
        r4 = r13.pid;	 Catch:{ RemoteException -> 0x00a3 }
        r5 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ RemoteException -> 0x00a3 }
        if (r4 == r5) goto L_0x00a2;	 Catch:{ RemoteException -> 0x00a3 }
    L_0x009d:
        r4 = "anr";	 Catch:{ RemoteException -> 0x00a3 }
        r13.kill(r4, r7);	 Catch:{ RemoteException -> 0x00a3 }
    L_0x00a2:
        goto L_0x00af;
    L_0x00a3:
        r0 = move-exception;
        r4 = r1.mService;
        r4.mController = r11;
        r4 = com.android.server.Watchdog.getInstance();
        r4.setActivityController(r11);
    L_0x00af:
        r5 = android.os.SystemClock.uptimeMillis();
        r0 = r1.mService;
        r0.updateCpuStatsNow();
        r0 = r1.mContext;
        r0 = r0.getContentResolver();
        r4 = "anr_show_background";
        r11 = 0;
        r0 = android.provider.Settings.Secure.getInt(r0, r4, r11);
        if (r0 == 0) goto L_0x00c9;
    L_0x00c7:
        r0 = r7;
        goto L_0x00ca;
    L_0x00c9:
        r0 = r11;
    L_0x00ca:
        r17 = r0;
        r0 = r1.mService;
        r0 = r0.mShuttingDown;
        if (r0 == 0) goto L_0x00f1;
    L_0x00d2:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "During shutdown skipping ANR: ";
        r3.append(r4);
        r3.append(r13);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x00f1:
        r0 = r13.notResponding;
        if (r0 == 0) goto L_0x0114;
    L_0x00f5:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Skipping duplicate ANR: ";
        r3.append(r4);
        r3.append(r13);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x0114:
        r0 = r13.crashing;
        if (r0 == 0) goto L_0x0137;
    L_0x0118:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Crashing app skipping ANR: ";
        r3.append(r4);
        r3.append(r13);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x0137:
        r0 = r13.killedByAm;
        if (r0 == 0) goto L_0x015a;
    L_0x013b:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "App already killed by AM skipping ANR: ";
        r3.append(r4);
        r3.append(r13);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x015a:
        r0 = r13.killed;
        r4 = 2;
        if (r0 == 0) goto L_0x017e;
    L_0x015f:
        r0 = "ActivityManager";
        r11 = new java.lang.StringBuilder;
        r11.<init>();
        r3 = "Skipping died app ANR: ";
        r11.append(r3);
        r11.append(r13);
        r3 = " ";
        r11.append(r3);
        r11.append(r12);
        r3 = r11.toString();
        android.util.Slog.i(r0, r3);
        goto L_0x01e9;
    L_0x017e:
        r0 = r13.pid;
        if (r2 == r0) goto L_0x01b5;
    L_0x0182:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Skipping ANR because pid of ";
        r3.append(r4);
        r4 = r13.processName;
        r3.append(r4);
        r4 = " is changed: anr pid: ";
        r3.append(r4);
        r3.append(r2);
        r4 = ", new pid: ";
        r3.append(r4);
        r4 = r13.pid;
        r3.append(r4);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x01b5:
        r0 = r1.mService;
        r3 = r13.uid;
        r0 = r0.handleANRFilterFIFO(r3, r4);
        if (r0 == 0) goto L_0x01e9;
    L_0x01bf:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "During holding skipping ANR: ";
        r3.append(r4);
        r3.append(r13);
        r4 = " ";
        r3.append(r4);
        r3.append(r12);
        r4 = "uid = ";
        r3.append(r4);
        r4 = r13.uid;
        r3.append(r4);
        r3 = r3.toString();
        android.util.Slog.i(r0, r3);
        return;
    L_0x01e9:
        r13.notResponding = r7;
        r0 = 30008; // 0x7538 float:4.205E-41 double:1.4826E-319;
        r3 = 5;
        r3 = new java.lang.Object[r3];
        r11 = r13.userId;
        r11 = java.lang.Integer.valueOf(r11);
        r18 = 0;
        r3[r18] = r11;
        r11 = r13.pid;
        r11 = java.lang.Integer.valueOf(r11);
        r3[r7] = r11;
        r11 = r13.processName;
        r3[r4] = r11;
        r11 = r13.info;
        r11 = r11.flags;
        r11 = java.lang.Integer.valueOf(r11);
        r4 = 3;
        r3[r4] = r11;
        r11 = 4;
        r3[r11] = r12;
        android.util.EventLog.writeEvent(r0, r3);
        r0 = r13.pid;
        r0 = java.lang.Integer.valueOf(r0);
        r10.add(r0);
        r0 = r1.mAppEyeANR;
        if (r0 == 0) goto L_0x02db;
    L_0x0224:
        r0 = r1.mAppEyeANR;
        r3 = 0;
        r0 = r0.check(r3);
        if (r0 != 0) goto L_0x02db;
    L_0x022d:
        r3 = r1.mService;
        monitor-enter(r3);
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x02d1 }
        if (r17 != 0) goto L_0x0242;
    L_0x0235:
        r0 = isInterestingForBackgroundTraces(r43);	 Catch:{ all -> 0x023d }
        if (r0 != 0) goto L_0x0242;
    L_0x023b:
        r0 = r7;
        goto L_0x0243;
    L_0x023d:
        r0 = move-exception;
        r23 = r5;
        goto L_0x02d4;
    L_0x0242:
        r0 = 0;
    L_0x0243:
        if (r0 != 0) goto L_0x02c9;
    L_0x0245:
        r11 = r13.pid;	 Catch:{ all -> 0x02d1 }
        if (r15 == 0) goto L_0x0258;
    L_0x0249:
        r4 = r15.app;	 Catch:{ all -> 0x023d }
        if (r4 == 0) goto L_0x0258;	 Catch:{ all -> 0x023d }
    L_0x024d:
        r4 = r15.app;	 Catch:{ all -> 0x023d }
        r4 = r4.pid;	 Catch:{ all -> 0x023d }
        if (r4 <= 0) goto L_0x0258;	 Catch:{ all -> 0x023d }
    L_0x0253:
        r4 = r15.app;	 Catch:{ all -> 0x023d }
        r4 = r4.pid;	 Catch:{ all -> 0x023d }
        r11 = r4;
    L_0x0258:
        r4 = r13.pid;	 Catch:{ all -> 0x02d1 }
        if (r11 == r4) goto L_0x0263;
    L_0x025c:
        r4 = java.lang.Integer.valueOf(r11);	 Catch:{ all -> 0x023d }
        r10.add(r4);	 Catch:{ all -> 0x023d }
    L_0x0263:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x02d1 }
        r7 = r13.pid;	 Catch:{ all -> 0x02d1 }
        if (r4 == r7) goto L_0x0276;
    L_0x0269:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x023d }
        if (r4 == r11) goto L_0x0276;	 Catch:{ all -> 0x023d }
    L_0x026d:
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x023d }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ all -> 0x023d }
        r10.add(r4);	 Catch:{ all -> 0x023d }
    L_0x0276:
        r4 = r1.mService;	 Catch:{ all -> 0x02d1 }
        r4 = r4.mLruProcesses;	 Catch:{ all -> 0x02d1 }
        r4 = r4.size();	 Catch:{ all -> 0x02d1 }
        r7 = 1;	 Catch:{ all -> 0x02d1 }
        r4 = r4 - r7;	 Catch:{ all -> 0x02d1 }
    L_0x0280:
        if (r4 < 0) goto L_0x02c9;	 Catch:{ all -> 0x02d1 }
    L_0x0282:
        r7 = r1.mService;	 Catch:{ all -> 0x02d1 }
        r7 = r7.mLruProcesses;	 Catch:{ all -> 0x02d1 }
        r7 = r7.get(r4);	 Catch:{ all -> 0x02d1 }
        r7 = (com.android.server.am.ProcessRecord) r7;	 Catch:{ all -> 0x02d1 }
        if (r7 == 0) goto L_0x02c0;	 Catch:{ all -> 0x02d1 }
    L_0x028e:
        r2 = r7.thread;	 Catch:{ all -> 0x02d1 }
        if (r2 == 0) goto L_0x02c0;	 Catch:{ all -> 0x02d1 }
    L_0x0292:
        r2 = r7.pid;	 Catch:{ all -> 0x02d1 }
        if (r2 <= 0) goto L_0x02c0;
    L_0x0296:
        r23 = r5;
        r5 = r13.pid;	 Catch:{ all -> 0x02d9 }
        if (r2 == r5) goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x029c:
        if (r2 == r11) goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x029e:
        r5 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x02d9 }
        if (r2 == r5) goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x02a2:
        r5 = r7.persistent;	 Catch:{ all -> 0x02d9 }
        if (r5 == 0) goto L_0x02ae;	 Catch:{ all -> 0x02d9 }
    L_0x02a6:
        r5 = java.lang.Integer.valueOf(r2);	 Catch:{ all -> 0x02d9 }
        r10.add(r5);	 Catch:{ all -> 0x02d9 }
        goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x02ae:
        r5 = r7.treatLikeActivity;	 Catch:{ all -> 0x02d9 }
        if (r5 == 0) goto L_0x02ba;	 Catch:{ all -> 0x02d9 }
    L_0x02b2:
        r5 = java.lang.Integer.valueOf(r2);	 Catch:{ all -> 0x02d9 }
        r10.add(r5);	 Catch:{ all -> 0x02d9 }
        goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x02ba:
        r5 = java.lang.Boolean.TRUE;	 Catch:{ all -> 0x02d9 }
        r9.put(r2, r5);	 Catch:{ all -> 0x02d9 }
        goto L_0x02c2;	 Catch:{ all -> 0x02d9 }
    L_0x02c0:
        r23 = r5;	 Catch:{ all -> 0x02d9 }
    L_0x02c2:
        r4 = r4 + -1;	 Catch:{ all -> 0x02d9 }
        r5 = r23;	 Catch:{ all -> 0x02d9 }
        r2 = r42;	 Catch:{ all -> 0x02d9 }
        goto L_0x0280;	 Catch:{ all -> 0x02d9 }
    L_0x02c9:
        r23 = r5;	 Catch:{ all -> 0x02d9 }
        monitor-exit(r3);	 Catch:{ all -> 0x02d9 }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        r11 = r0;
        goto L_0x02de;
    L_0x02d1:
        r0 = move-exception;
        r23 = r5;
    L_0x02d4:
        monitor-exit(r3);	 Catch:{ all -> 0x02d9 }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        throw r0;
    L_0x02d9:
        r0 = move-exception;
        goto L_0x02d4;
    L_0x02db:
        r23 = r5;
        r11 = 0;
    L_0x02de:
        r2 = r11;
        r0 = r1.mAppEyeANR;
        if (r0 == 0) goto L_0x031c;
    L_0x02e3:
        r0 = new android.zrhung.ZrHungData;
        r0.<init>();
        r3 = "processName";
        r4 = r13.processName;
        r0.putString(r3, r4);
        r3 = "pid";
        r4 = r13.pid;
        r0.putInt(r3, r4);
        r3 = "isSilentANR";
        r0.putBoolean(r3, r2);
        r3 = "packageName";
        r4 = r13.info;
        r4 = r4.packageName;
        r0.putString(r3, r4);
        r3 = "reason";
        r0.putString(r3, r12);
        if (r14 == 0) goto L_0x0317;
    L_0x0310:
        r3 = "activityName";
        r4 = r14.shortComponentName;
        r0.putString(r3, r4);
    L_0x0317:
        r3 = r1.mAppEyeANR;
        r3.sendEvent(r0);
    L_0x031c:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r11 = r0;
        r3 = 0;
        r11.setLength(r3);
        r0 = "ANR in ";
        r11.append(r0);
        r0 = r13.processName;
        r11.append(r0);
        if (r14 == 0) goto L_0x0345;
    L_0x0332:
        r0 = r14.shortComponentName;
        if (r0 == 0) goto L_0x0345;
    L_0x0336:
        r0 = " (";
        r11.append(r0);
        r0 = r14.shortComponentName;
        r11.append(r0);
        r0 = ")";
        r11.append(r0);
    L_0x0345:
        r0 = "\n";
        r11.append(r0);
        r0 = "PID: ";
        r11.append(r0);
        r0 = r13.pid;
        r11.append(r0);
        r0 = "\n";
        r11.append(r0);
        if (r12 == 0) goto L_0x0368;
    L_0x035b:
        r0 = "Reason: ";
        r11.append(r0);
        r11.append(r12);
        r0 = "\n";
        r11.append(r0);
    L_0x0368:
        if (r15 == 0) goto L_0x037b;
    L_0x036a:
        if (r15 == r14) goto L_0x037b;
    L_0x036c:
        r0 = "Parent: ";
        r11.append(r0);
        r0 = r15.shortComponentName;
        r11.append(r0);
        r0 = "\n";
        r11.append(r0);
    L_0x037b:
        r0 = new com.android.internal.os.ProcessCpuTracker;
        r3 = 1;
        r0.<init>(r3);
        r7 = r0;
        r3 = 0;
        r16 = 0;
        r4 = r16;
        r0 = new java.io.FileOutputStream;	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r5 = new java.io.File;	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r6 = "/proc/sysrq-trigger";	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r5.<init>(r6);	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r0.<init>(r5);	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r4 = r0;	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r0 = new java.io.OutputStreamWriter;	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r5 = "UTF-8";	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r0.<init>(r4, r5);	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r3 = r0;	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r0 = "w";	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r3.write(r0);	 Catch:{ IOException -> 0x03c4, all -> 0x03ae }
        r3.close();	 Catch:{ IOException -> 0x03a7 }
        goto L_0x03a9;	 Catch:{ IOException -> 0x03a7 }
    L_0x03a7:
        r0 = move-exception;	 Catch:{ IOException -> 0x03a7 }
        goto L_0x03da;	 Catch:{ IOException -> 0x03a7 }
        r4.close();	 Catch:{ IOException -> 0x03a7 }
        goto L_0x03e3;
    L_0x03ae:
        r0 = move-exception;
        r29 = r2;
        r5 = r4;
        r33 = r7;
        r26 = r9;
        r18 = r10;
        r2 = r11;
        r6 = r12;
        r31 = r23;
        r7 = r46;
        r4 = r3;
        r23 = r8;
        r3 = r0;
        goto L_0x0646;
    L_0x03c4:
        r0 = move-exception;
        r5 = "ActivityManager";	 Catch:{ all -> 0x0632 }
        r6 = "Failed to write to /proc/sysrq-trigger";	 Catch:{ all -> 0x0632 }
        android.util.Slog.e(r5, r6);	 Catch:{ all -> 0x0632 }
        if (r3 == 0) goto L_0x03d4;
    L_0x03ce:
        r3.close();	 Catch:{ IOException -> 0x03d2 }
        goto L_0x03d4;	 Catch:{ IOException -> 0x03d2 }
    L_0x03d2:
        r0 = move-exception;	 Catch:{ IOException -> 0x03d2 }
        goto L_0x03da;	 Catch:{ IOException -> 0x03d2 }
    L_0x03d4:
        if (r4 == 0) goto L_0x03e3;	 Catch:{ IOException -> 0x03d2 }
    L_0x03d6:
        r4.close();	 Catch:{ IOException -> 0x03d2 }
        goto L_0x03e3;
        r5 = "ActivityManager";
        r6 = "Failed to write to /proc/sysrq-trigger";
        android.util.Slog.e(r5, r6);
        goto L_0x03e4;
    L_0x03e4:
        r19 = r3;
        r25 = r4;
        r0 = 0;
        if (r2 == 0) goto L_0x0412;
    L_0x03eb:
        r3 = 0;
    L_0x03ec:
        r4 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
        r4 = r4.length;
        if (r3 >= r4) goto L_0x040e;
    L_0x03f1:
        r4 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
        r4 = r4[r3];
        r5 = r13.processName;
        r4 = r4.equals(r5);
        if (r4 == 0) goto L_0x0408;
    L_0x03fd:
        r6 = 1;
        r4 = new java.lang.String[r6];
        r5 = r13.processName;
        r18 = 0;
        r4[r18] = r5;
        r0 = r4;
        goto L_0x0417;
    L_0x0408:
        r6 = 1;
        r18 = 0;
        r3 = r3 + 1;
        goto L_0x03ec;
    L_0x040e:
        r6 = 1;
        r18 = 0;
        goto L_0x0417;
    L_0x0412:
        r6 = 1;
        r18 = 0;
        r0 = com.android.server.Watchdog.NATIVE_STACKS_OF_INTEREST;
    L_0x0417:
        r5 = r0;
        if (r5 != 0) goto L_0x041d;
    L_0x041a:
        r0 = r16;
        goto L_0x0421;
    L_0x041d:
        r0 = android.os.Process.getPidsForCommands(r5);
    L_0x0421:
        r4 = r0;
        r0 = 0;
        if (r4 == 0) goto L_0x0445;
    L_0x0425:
        r3 = new java.util.ArrayList;
        r6 = r4.length;
        r3.<init>(r6);
        r0 = r3;
        r3 = r4.length;
        r6 = r18;
    L_0x042f:
        if (r6 >= r3) goto L_0x0445;
    L_0x0431:
        r27 = r3;
        r3 = r4[r6];
        r28 = r4;
        r4 = java.lang.Integer.valueOf(r3);
        r0.add(r4);
        r6 = r6 + 1;
        r3 = r27;
        r4 = r28;
        goto L_0x042f;
    L_0x0445:
        r28 = r4;
        r22 = r0;
        r4 = 1;
        if (r2 == 0) goto L_0x044f;
    L_0x044c:
        r6 = r16;
        goto L_0x0450;
    L_0x044f:
        r6 = r7;
    L_0x0450:
        if (r2 == 0) goto L_0x0455;
    L_0x0452:
        r0 = r16;
        goto L_0x0456;
    L_0x0455:
        r0 = r9;
    L_0x0456:
        r3 = r13;
        r29 = r2;
        r20 = r28;
        r2 = 3;
        r21 = r5;
        r30 = r23;
        r5 = r10;
        r23 = 1;
        r2 = r7;
        r7 = r0;
        r23 = r8;
        r8 = r22;
        r24 = com.android.server.am.ActivityManagerService.dumpStackTraces(r3, r4, r5, r6, r7, r8);
        r3 = 0;
        r0 = r1.mService;
        r0.updateCpuStatsNow();
        r0 = r1.mService;
        r5 = r0.mProcessCpuTracker;
        monitor-enter(r5);
        r0 = r1.mService;	 Catch:{ all -> 0x0621 }
        r0 = r0.mProcessCpuTracker;	 Catch:{ all -> 0x0621 }
        r7 = r30;
        r0 = r0.printCurrentState(r7);	 Catch:{ all -> 0x0613 }
        r6 = r0;
        monitor-exit(r5);	 Catch:{ all -> 0x0601 }
        r0 = r2.printCurrentLoad();
        r11.append(r0);
        r11.append(r6);
        r5 = r2.printCurrentState(r7);
        r11.append(r5);
        r0 = "ActivityManager";
        r3 = r11.toString();
        android.util.Slog.e(r0, r3);
        r0 = "ActivityManager";
        android.util.Slog.e(r0, r5);
        if (r24 != 0) goto L_0x04ab;
    L_0x04a5:
        r0 = r13.pid;
        r3 = 3;
        android.os.Process.sendSignal(r0, r3);
    L_0x04ab:
        r4 = r13.uid;
        r0 = r13.processName;
        if (r14 != 0) goto L_0x04b5;
    L_0x04b1:
        r26 = "unknown";
        goto L_0x04b9;
    L_0x04b5:
        r3 = r14.shortComponentName;
        r26 = r3;
    L_0x04b9:
        r3 = r13.info;
        if (r3 == 0) goto L_0x04cd;
    L_0x04bd:
        r3 = r13.info;
        r3 = r3.isInstantApp();
        if (r3 == 0) goto L_0x04c9;
        r27 = 2;
        goto L_0x04d0;
        r27 = 1;
        goto L_0x04d0;
        r27 = r18;
    L_0x04d0:
        if (r13 == 0) goto L_0x04e0;
    L_0x04d2:
        r3 = r43.isInterestingToUserLocked();
        if (r3 == 0) goto L_0x04dc;
        r18 = 2;
        goto L_0x04e1;
        r18 = 1;
        goto L_0x04e1;
    L_0x04e1:
        r3 = 79;
        r28 = r5;
        r5 = r0;
        r30 = r6;
        r6 = r26;
        r31 = r7;
        r7 = r12;
        r8 = r27;
        r26 = r9;
        r9 = r18;
        android.util.StatsLog.write(r3, r4, r5, r6, r7, r8, r9);
        r3 = r1.mService;
        r4 = "anr";
        r6 = r13.processName;
        r0 = 0;
        r5 = r13;
        r7 = r14;
        r8 = r15;
        r9 = r12;
        r18 = r10;
        r10 = r30;
        r33 = r2;
        r2 = r11;
        r15 = r16;
        r11 = r24;
        r12 = r0;
        r3.addErrorToDropBox(r4, r5, r6, r7, r8, r9, r10, r11, r12);
        r0 = r1.mService;
        r0 = r0.mController;
        if (r0 == 0) goto L_0x0563;
    L_0x0516:
        r0 = r1.mService;	 Catch:{ RemoteException -> 0x0555 }
        r0 = r0.mController;	 Catch:{ RemoteException -> 0x0555 }
        r3 = r13.processName;	 Catch:{ RemoteException -> 0x0555 }
        r4 = r13.pid;	 Catch:{ RemoteException -> 0x0555 }
        r5 = r2.toString();	 Catch:{ RemoteException -> 0x0555 }
        r0 = r0.appNotResponding(r3, r4, r5);	 Catch:{ RemoteException -> 0x0555 }
        r3 = r0;	 Catch:{ RemoteException -> 0x0555 }
        if (r3 == 0) goto L_0x0553;	 Catch:{ RemoteException -> 0x0555 }
    L_0x0529:
        if (r3 >= 0) goto L_0x0538;	 Catch:{ RemoteException -> 0x0555 }
    L_0x052b:
        r0 = r13.pid;	 Catch:{ RemoteException -> 0x0555 }
        r4 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ RemoteException -> 0x0555 }
        if (r0 == r4) goto L_0x0538;	 Catch:{ RemoteException -> 0x0555 }
    L_0x0531:
        r0 = "anr";	 Catch:{ RemoteException -> 0x0555 }
        r4 = 1;
        r13.kill(r0, r4);	 Catch:{ RemoteException -> 0x0551 }
        goto L_0x054a;	 Catch:{ RemoteException -> 0x0551 }
    L_0x0538:
        r4 = 1;	 Catch:{ RemoteException -> 0x0551 }
        r5 = r1.mService;	 Catch:{ RemoteException -> 0x0551 }
        monitor-enter(r5);	 Catch:{ RemoteException -> 0x0551 }
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x054b }
        r0 = r1.mService;	 Catch:{ all -> 0x054b }
        r0 = r0.mServices;	 Catch:{ all -> 0x054b }
        r0.scheduleServiceTimeoutLocked(r13);	 Catch:{ all -> 0x054b }
        monitor-exit(r5);	 Catch:{ all -> 0x054b }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();	 Catch:{ RemoteException -> 0x0551 }
    L_0x054a:
        return;
    L_0x054b:
        r0 = move-exception;
        monitor-exit(r5);	 Catch:{ all -> 0x054b }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();	 Catch:{ RemoteException -> 0x0551 }
        throw r0;	 Catch:{ RemoteException -> 0x0551 }
    L_0x0551:
        r0 = move-exception;
        goto L_0x0557;
    L_0x0553:
        r4 = 1;
        goto L_0x0564;
    L_0x0555:
        r0 = move-exception;
        r4 = 1;
    L_0x0557:
        r3 = r1.mService;
        r3.mController = r15;
        r3 = com.android.server.Watchdog.getInstance();
        r3.setActivityController(r15);
        goto L_0x0564;
    L_0x0563:
        r4 = 1;
    L_0x0564:
        r3 = r1.mService;
        monitor-enter(r3);
        com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();	 Catch:{ all -> 0x05f5 }
        r0 = r1.mService;	 Catch:{ all -> 0x05f5 }
        r0 = r0.mBatteryStatsService;	 Catch:{ all -> 0x05f5 }
        r5 = r13.processName;	 Catch:{ all -> 0x05f5 }
        r6 = r13.uid;	 Catch:{ all -> 0x05f5 }
        r0.noteProcessAnr(r5, r6);	 Catch:{ all -> 0x05f5 }
        if (r17 != 0) goto L_0x058d;	 Catch:{ all -> 0x05f5 }
    L_0x0577:
        r0 = r43.isInterestingToUserLocked();	 Catch:{ all -> 0x05f5 }
        if (r0 != 0) goto L_0x058d;	 Catch:{ all -> 0x05f5 }
    L_0x057d:
        r0 = r13.pid;	 Catch:{ all -> 0x05f5 }
        r5 = com.android.server.am.ActivityManagerService.MY_PID;	 Catch:{ all -> 0x05f5 }
        if (r0 == r5) goto L_0x058d;	 Catch:{ all -> 0x05f5 }
    L_0x0583:
        r0 = "bg anr";	 Catch:{ all -> 0x05f5 }
        r13.kill(r0, r4);	 Catch:{ all -> 0x05f5 }
        monitor-exit(r3);	 Catch:{ all -> 0x05f5 }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
        if (r14 == 0) goto L_0x0593;
    L_0x0590:
        r11 = r14.shortComponentName;	 Catch:{ all -> 0x05f5 }
        goto L_0x0594;
    L_0x0593:
        r11 = r15;
    L_0x0594:
        r6 = r47;
        if (r6 == 0) goto L_0x05ae;
    L_0x0598:
        r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x05aa }
        r0.<init>();	 Catch:{ all -> 0x05aa }
        r5 = "ANR ";	 Catch:{ all -> 0x05aa }
        r0.append(r5);	 Catch:{ all -> 0x05aa }
        r0.append(r6);	 Catch:{ all -> 0x05aa }
        r0 = r0.toString();	 Catch:{ all -> 0x05aa }
        goto L_0x05b0;	 Catch:{ all -> 0x05aa }
    L_0x05aa:
        r0 = move-exception;	 Catch:{ all -> 0x05aa }
        r7 = r46;	 Catch:{ all -> 0x05aa }
        goto L_0x05fa;	 Catch:{ all -> 0x05aa }
    L_0x05ae:
        r0 = "ANR";	 Catch:{ all -> 0x05aa }
    L_0x05b0:
        r5 = r2.toString();	 Catch:{ all -> 0x05aa }
        r1.makeAppNotRespondingLocked(r13, r11, r0, r5);	 Catch:{ all -> 0x05aa }
        r0 = android.os.Message.obtain();	 Catch:{ all -> 0x05aa }
        r5 = 2;	 Catch:{ all -> 0x05aa }
        r0.what = r5;	 Catch:{ all -> 0x05aa }
        r5 = new com.android.server.am.AppNotRespondingDialog$Data;	 Catch:{ all -> 0x05aa }
        r7 = r46;
        r5.<init>(r13, r14, r7);	 Catch:{ all -> 0x05ff }
        r0.obj = r5;	 Catch:{ all -> 0x05ff }
        r5 = r1.mService;	 Catch:{ all -> 0x05ff }
        r35 = "showanrdialog";	 Catch:{ all -> 0x05ff }
        r8 = r13.pid;	 Catch:{ all -> 0x05ff }
        r9 = r13.uid;	 Catch:{ all -> 0x05ff }
        r10 = r13.info;	 Catch:{ all -> 0x05ff }
        r10 = r10.packageName;	 Catch:{ all -> 0x05ff }
        r39 = 0;	 Catch:{ all -> 0x05ff }
        r40 = "original";	 Catch:{ all -> 0x05ff }
        r34 = r5;	 Catch:{ all -> 0x05ff }
        r36 = r8;	 Catch:{ all -> 0x05ff }
        r37 = r9;	 Catch:{ all -> 0x05ff }
        r38 = r10;	 Catch:{ all -> 0x05ff }
        r5 = r34.zrHungSendEvent(r35, r36, r37, r38, r39, r40);	 Catch:{ all -> 0x05ff }
        if (r5 == 0) goto L_0x05f0;	 Catch:{ all -> 0x05ff }
    L_0x05e7:
        r13.anrType = r4;	 Catch:{ all -> 0x05ff }
        r4 = r1.mService;	 Catch:{ all -> 0x05ff }
        r4 = r4.mUiHandler;	 Catch:{ all -> 0x05ff }
        r4.sendMessage(r0);	 Catch:{ all -> 0x05ff }
    L_0x05f0:
        monitor-exit(r3);	 Catch:{ all -> 0x05ff }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        return;
    L_0x05f5:
        r0 = move-exception;
        r7 = r46;
        r6 = r47;
    L_0x05fa:
        monitor-exit(r3);	 Catch:{ all -> 0x05ff }
        com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
        throw r0;
    L_0x05ff:
        r0 = move-exception;
        goto L_0x05fa;
    L_0x0601:
        r0 = move-exception;
        r33 = r2;
        r30 = r6;
        r31 = r7;
        r26 = r9;
        r18 = r10;
        r2 = r11;
        r6 = r12;
        r7 = r46;
        r3 = r30;
        goto L_0x062e;
    L_0x0613:
        r0 = move-exception;
        r33 = r2;
        r31 = r7;
        r26 = r9;
        r18 = r10;
        r2 = r11;
        r6 = r12;
        r7 = r46;
        goto L_0x062e;
    L_0x0621:
        r0 = move-exception;
        r7 = r46;
        r33 = r2;
        r26 = r9;
        r18 = r10;
        r2 = r11;
        r6 = r12;
        r31 = r30;
    L_0x062e:
        monitor-exit(r5);	 Catch:{ all -> 0x0630 }
        throw r0;
    L_0x0630:
        r0 = move-exception;
        goto L_0x062e;
    L_0x0632:
        r0 = move-exception;
        r29 = r2;
        r33 = r7;
        r26 = r9;
        r18 = r10;
        r2 = r11;
        r6 = r12;
        r31 = r23;
        r7 = r46;
        r23 = r8;
        r5 = r4;
        r4 = r3;
        r3 = r0;
    L_0x0646:
        if (r4 == 0) goto L_0x064e;
    L_0x0648:
        r4.close();	 Catch:{ IOException -> 0x064c }
        goto L_0x064e;	 Catch:{ IOException -> 0x064c }
    L_0x064c:
        r0 = move-exception;	 Catch:{ IOException -> 0x064c }
        goto L_0x0654;	 Catch:{ IOException -> 0x064c }
    L_0x064e:
        if (r5 == 0) goto L_0x065d;	 Catch:{ IOException -> 0x064c }
    L_0x0650:
        r5.close();	 Catch:{ IOException -> 0x064c }
        goto L_0x065d;
        r8 = "ActivityManager";
        r9 = "Failed to write to /proc/sysrq-trigger";
        android.util.Slog.e(r8, r9);
    L_0x065d:
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.AppErrors.appNotResponding(int, com.android.server.am.ProcessRecord, com.android.server.am.ActivityRecord, com.android.server.am.ActivityRecord, boolean, java.lang.String):void");
    }

    private void makeAppNotRespondingLocked(ProcessRecord app, String activity, String shortMsg, String longMsg) {
        app.notResponding = true;
        app.notRespondingReport = generateProcessError(app, 2, activity, shortMsg, longMsg, null);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
    }

    /* JADX WARNING: Missing block: B:56:0x00fe, code:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:57:0x0101, code:
            if (r2 == null) goto L_0x0106;
     */
    /* JADX WARNING: Missing block: B:58:0x0103, code:
            r2.show();
     */
    /* JADX WARNING: Missing block: B:59:0x0106, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleShowAnrUi(Message msg) {
        Throwable th;
        Dialog dialogToShow = null;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                Data data = msg.obj;
                ProcessRecord proc = data.proc;
                if (proc == null) {
                    Slog.e("ActivityManager", "handleShowAnrUi: proc is null");
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (proc.info != null) {
                    String packageName = proc.info.packageName;
                    if (this.mIswhitelist_for_short_time && whitelist_for_short_time.contains(packageName)) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    try {
                        if (this.mService.getRecordCust() != null) {
                            this.mService.getRecordCust().appExitRecord(packageName, "anr");
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                if (proc.anrDialog != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("App already has anr dialog: ");
                    stringBuilder.append(proc);
                    Slog.e("ActivityManager", stringBuilder.toString());
                    MetricsLogger.action(this.mContext, 317, -2);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Intent intent = new Intent("android.intent.action.ANR");
                if (!this.mService.mProcessesReady) {
                    intent.addFlags(1342177280);
                }
                this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, 0);
                boolean z = false;
                if (Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0) {
                    z = true;
                }
                boolean showBackground = z;
                if ((!this.mService.canShowErrorDialogs() && !showBackground) || HwFrameworkFactory.getVRSystemServiceManager().isVRMode() || this.mIsNotShowAnrDialog) {
                    MetricsLogger.action(this.mContext, 317, -1);
                    this.mService.killAppAtUsersRequest(proc, null);
                } else {
                    Context context = this.mContext;
                    if (HwPCUtils.isPcCastModeInServer() && HwPCUtils.isValidExtDisplayId(proc.mDisplayId)) {
                        Context tmpContext = HwPCUtils.getDisplayContext(context, proc.mDisplayId);
                        if (tmpContext != null) {
                            context = tmpContext;
                        }
                    }
                    dialogToShow = new AppNotRespondingDialog(this.mService, context, data);
                    proc.anrDialog = dialogToShow;
                }
            } catch (Throwable th3) {
                th = th3;
                Message message = msg;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void makeAppeyeAppNotRespondingLocked(ProcessRecord app, String activity, String shortMsg, String longMsg) {
        app.notRespondingReport = generateProcessError(app, 2, activity, shortMsg, longMsg, null);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
    }
}

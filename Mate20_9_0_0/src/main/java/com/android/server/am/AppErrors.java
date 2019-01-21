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
import android.os.Trace;
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
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.HwServiceFactory;
import com.android.server.RescueParty;
import com.android.server.Watchdog;
import com.android.server.pm.DumpState;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.zrhung.IZRHungService;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    /* JADX WARNING: Missing block: B:10:0x0055, code skipped:
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

    /* JADX WARNING: Missing block: B:107:0x01e8, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:108:0x01eb, code skipped:
            if (r1 == null) goto L_0x0202;
     */
    /* JADX WARNING: Missing block: B:110:?, code skipped:
            r11.mContext.startActivityAsUser(r1, new android.os.UserHandle(r12.userId));
     */
    /* JADX WARNING: Missing block: B:111:0x01fa, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:112:0x01fb, code skipped:
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

    /* JADX WARNING: Removed duplicated region for block: B:164:0x0226 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x01c3 A:{SYNTHETIC, Splitter:B:134:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0166 A:{SYNTHETIC, Splitter:B:98:0x0166} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x01c3 A:{SYNTHETIC, Splitter:B:134:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x01d6 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x00e6 A:{SYNTHETIC, Splitter:B:65:0x00e6} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x0134 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x0132 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x014e A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x014c A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x0153 A:{SYNTHETIC, Splitter:B:90:0x0153} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0166 A:{SYNTHETIC, Splitter:B:98:0x0166} */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x0198 A:{Catch:{ all -> 0x0035 }} */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x01c3 A:{SYNTHETIC, Splitter:B:134:0x01c3} */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x01d6 A:{Catch:{ all -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x02ac A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x0251 A:{Catch:{ all -> 0x02e5 }} */
    /* JADX WARNING: Missing block: B:27:0x0073, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:28:0x0076, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:50:0x00c1, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:51:0x00c4, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:77:0x011f, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:78:0x0122, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:181:0x02b4, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
            r0 = r10;
            r2 = r12;
     */
    /* JADX WARNING: Missing block: B:182:0x02b9, code skipped:
            if (r7 == null) goto L_0x02dc;
     */
    /* JADX WARNING: Missing block: B:183:0x02bb, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("Showing crash dialog for package ");
            r5.append(r0);
            r5.append(" u");
            r5.append(r2);
            android.util.Slog.i("ActivityManager", r5.toString());
            r7.show();
     */
    /* JADX WARNING: Missing block: B:184:0x02dc, code skipped:
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
                                                    AppErrorDialog appErrorDialog = new AppErrorDialog(context, this.mService, data);
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

    /* JADX WARNING: Removed duplicated region for block: B:70:0x0245 A:{SYNTHETIC, Splitter:B:70:0x0245} */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0412  */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x03eb  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x041d  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x041a  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x0425  */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:188:0x044c  */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x0455  */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0452  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0478 A:{SYNTHETIC, Splitter:B:195:0x0478} */
    /* JADX WARNING: Removed duplicated region for block: B:256:0x0567 A:{SYNTHETIC, Splitter:B:256:0x0567} */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x03eb  */
    /* JADX WARNING: Removed duplicated region for block: B:176:0x0412  */
    /* JADX WARNING: Removed duplicated region for block: B:179:0x041a  */
    /* JADX WARNING: Removed duplicated region for block: B:180:0x041d  */
    /* JADX WARNING: Removed duplicated region for block: B:183:0x0425  */
    /* JADX WARNING: Removed duplicated region for block: B:188:0x044c  */
    /* JADX WARNING: Removed duplicated region for block: B:189:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:191:0x0452  */
    /* JADX WARNING: Removed duplicated region for block: B:192:0x0455  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x0478 A:{SYNTHETIC, Splitter:B:195:0x0478} */
    /* JADX WARNING: Removed duplicated region for block: B:308:0x0648 A:{SYNTHETIC, Splitter:B:308:0x0648} */
    /* JADX WARNING: Removed duplicated region for block: B:313:0x0650 A:{Catch:{ IOException -> 0x064c }} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:232:0x0534, B:236:0x053c] */
    /* JADX WARNING: Missing block: B:119:0x02cc, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
            r11 = r0;
     */
    /* JADX WARNING: Missing block: B:203:0x0484, code skipped:
            r11.append(r2.printCurrentLoad());
            r11.append(r6);
            r5 = r2.printCurrentState(r7);
            r11.append(r5);
            android.util.Slog.e("ActivityManager", r11.toString());
            android.util.Slog.e("ActivityManager", r5);
     */
    /* JADX WARNING: Missing block: B:204:0x04a3, code skipped:
            if (r24 != null) goto L_0x04ab;
     */
    /* JADX WARNING: Missing block: B:205:0x04a5, code skipped:
            android.os.Process.sendSignal(r13.pid, 3);
     */
    /* JADX WARNING: Missing block: B:206:0x04ab, code skipped:
            r4 = r13.uid;
            r0 = r13.processName;
     */
    /* JADX WARNING: Missing block: B:207:0x04af, code skipped:
            if (r14 != null) goto L_0x04b5;
     */
    /* JADX WARNING: Missing block: B:208:0x04b1, code skipped:
            r26 = com.android.server.UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
     */
    /* JADX WARNING: Missing block: B:209:0x04b5, code skipped:
            r26 = r14.shortComponentName;
     */
    /* JADX WARNING: Missing block: B:211:0x04bb, code skipped:
            if (r13.info == null) goto L_0x04cd;
     */
    /* JADX WARNING: Missing block: B:213:0x04c3, code skipped:
            if (r13.info.isInstantApp() == false) goto L_0x04c9;
     */
    /* JADX WARNING: Missing block: B:214:0x04c5, code skipped:
            r27 = 2;
     */
    /* JADX WARNING: Missing block: B:215:0x04c9, code skipped:
            r27 = 1;
     */
    /* JADX WARNING: Missing block: B:216:0x04cd, code skipped:
            r27 = r18;
     */
    /* JADX WARNING: Missing block: B:217:0x04d0, code skipped:
            if (r13 == null) goto L_0x04e1;
     */
    /* JADX WARNING: Missing block: B:219:0x04d6, code skipped:
            if (r43.isInterestingToUserLocked() == false) goto L_0x04dc;
     */
    /* JADX WARNING: Missing block: B:220:0x04d8, code skipped:
            r18 = 2;
     */
    /* JADX WARNING: Missing block: B:221:0x04dc, code skipped:
            r18 = 1;
     */
    /* JADX WARNING: Missing block: B:222:0x04e1, code skipped:
            r28 = r5;
            r30 = r6;
            r31 = r7;
            r26 = r9;
            android.util.StatsLog.write(79, r4, r0, r26, r12, r27, r18);
            r18 = r10;
            r33 = r2;
            r2 = r11;
            r15 = null;
            r1.mService.addErrorToDropBox("anr", r13, r13.processName, r14, r15, r12, r30, r24, null);
     */
    /* JADX WARNING: Missing block: B:223:0x0514, code skipped:
            if (r1.mService.mController == null) goto L_0x0563;
     */
    /* JADX WARNING: Missing block: B:225:?, code skipped:
            r3 = r1.mService.mController.appNotResponding(r13.processName, r13.pid, r2.toString());
     */
    /* JADX WARNING: Missing block: B:226:0x0527, code skipped:
            if (r3 == 0) goto L_0x0553;
     */
    /* JADX WARNING: Missing block: B:227:0x0529, code skipped:
            if (r3 >= 0) goto L_0x0538;
     */
    /* JADX WARNING: Missing block: B:229:0x052f, code skipped:
            if (r13.pid == com.android.server.am.ActivityManagerService.MY_PID) goto L_0x0538;
     */
    /* JADX WARNING: Missing block: B:231:0x0533, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:233:?, code skipped:
            r13.kill("anr", true);
     */
    /* JADX WARNING: Missing block: B:234:0x0538, code skipped:
            r4 = true;
            r5 = r1.mService;
     */
    /* JADX WARNING: Missing block: B:235:0x053b, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:237:?, code skipped:
            com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();
            r1.mService.mServices.scheduleServiceTimeoutLocked(r13);
     */
    /* JADX WARNING: Missing block: B:238:0x0546, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:240:?, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:241:0x054a, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:246:?, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:249:0x0553, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:251:0x0556, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:252:0x0557, code skipped:
            r1.mService.mController = r15;
            com.android.server.Watchdog.getInstance().setActivityController(r15);
     */
    /* JADX WARNING: Missing block: B:253:0x0563, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:255:0x0566, code skipped:
            monitor-enter(r1.mService);
     */
    /* JADX WARNING: Missing block: B:257:?, code skipped:
            com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();
            r1.mService.mBatteryStatsService.noteProcessAnr(r13.processName, r13.uid);
     */
    /* JADX WARNING: Missing block: B:258:0x0575, code skipped:
            if (r17 != false) goto L_0x058e;
     */
    /* JADX WARNING: Missing block: B:263:0x0583, code skipped:
            r13.kill("bg anr", r4);
     */
    /* JADX WARNING: Missing block: B:265:0x0589, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:266:0x058c, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:267:0x058e, code skipped:
            if (r14 != null) goto L_0x0590;
     */
    /* JADX WARNING: Missing block: B:269:?, code skipped:
            r11 = r14.shortComponentName;
     */
    /* JADX WARNING: Missing block: B:270:0x0593, code skipped:
            r11 = r15;
     */
    /* JADX WARNING: Missing block: B:271:0x0594, code skipped:
            r6 = r47;
     */
    /* JADX WARNING: Missing block: B:272:0x0596, code skipped:
            if (r6 != null) goto L_0x0598;
     */
    /* JADX WARNING: Missing block: B:274:?, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("ANR ");
            r0.append(r6);
            r0 = r0.toString();
     */
    /* JADX WARNING: Missing block: B:275:0x05aa, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:276:0x05ab, code skipped:
            r7 = r46;
     */
    /* JADX WARNING: Missing block: B:277:0x05ae, code skipped:
            r0 = "ANR";
     */
    /* JADX WARNING: Missing block: B:278:0x05b0, code skipped:
            makeAppNotRespondingLocked(r13, r11, r0, r2.toString());
            r0 = android.os.Message.obtain();
            r0.what = 2;
     */
    /* JADX WARNING: Missing block: B:281:?, code skipped:
            r0.obj = new com.android.server.am.AppNotRespondingDialog.Data(r13, r14, r46);
     */
    /* JADX WARNING: Missing block: B:282:0x05e5, code skipped:
            if (r1.mService.zrHungSendEvent(com.android.server.zrhung.IZRHungService.EVENT_SHOWANRDIALOG, r13.pid, r13.uid, r13.info.packageName, null, com.android.server.zrhung.IZRHungService.TYPE_ORIGINAL) != false) goto L_0x05e7;
     */
    /* JADX WARNING: Missing block: B:283:0x05e7, code skipped:
            r13.anrType = r4;
            r1.mService.mUiHandler.sendMessage(r0);
     */
    /* JADX WARNING: Missing block: B:285:0x05f1, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:286:0x05f4, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:287:0x05f5, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:288:0x05f6, code skipped:
            r7 = r46;
            r6 = r47;
     */
    /* JADX WARNING: Missing block: B:291:0x05fb, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:292:0x05fe, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:293:0x05ff, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void appNotResponding(int anrPid, ProcessRecord app, ActivityRecord activity, ActivityRecord parent, boolean aboveSystem, String annotation) {
        Throwable th;
        String[] nativeProcs;
        String[] nativeProcs2;
        int[] pids;
        ArrayList<Integer> nativePids;
        ArrayList<Integer> nativePids2;
        ProcessCpuTracker processCpuTracker;
        SparseArray<Boolean> sparseArray;
        ProcessCpuTracker processCpuTracker2;
        File tracesFile;
        boolean z;
        FileOutputStream sysrq_trigger_io_stream;
        ProcessCpuTracker processCpuTracker3;
        SparseArray<Boolean> sparseArray2;
        ArrayList<Integer> arrayList;
        String str;
        long j;
        OutputStreamWriter sysrq_trigger;
        StringBuilder processCpuTracker4;
        int i = anrPid;
        ProcessRecord processRecord = app;
        ActivityRecord activityRecord = activity;
        ActivityRecord activityRecord2 = parent;
        String str2 = annotation;
        if (IS_DEBUG_VERSION) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put("checkType", "FocusWindowNullScene");
            params.put("anrActivityName", activityRecord != null ? activity.toString() : null);
            if (HwServiceFactory.getWinFreezeScreenMonitor() != null) {
                HwServiceFactory.getWinFreezeScreenMonitor().cancelCheckFreezeScreen(params);
            }
        }
        ArrayList<Integer> firstPids = new ArrayList(5);
        SparseArray<Boolean> lastPids = new SparseArray(20);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("anr_event_sync: appPid=");
        stringBuilder.append(processRecord.pid);
        stringBuilder.append(", appName=");
        stringBuilder.append(processRecord.processName);
        stringBuilder.append(", category=");
        stringBuilder.append(str2);
        String traceMark = stringBuilder.toString();
        Trace.traceBegin(64, traceMark);
        Trace.traceEnd(64);
        if (Log.HWINFO) {
            HwFrameworkFactory.getLogException().cmd(HwBroadcastRadarUtil.KEY_ACTION, "copy_systrace_to_cache");
        }
        if (this.mService.mController != null) {
            try {
                if (this.mService.mController.appEarlyNotResponding(processRecord.processName, processRecord.pid, str2) < 0 && processRecord.pid != ActivityManagerService.MY_PID) {
                    processRecord.kill("anr", true);
                }
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        }
        long anrTime = SystemClock.uptimeMillis();
        this.mService.updateCpuStatsNow();
        boolean showBackground = Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        StringBuilder stringBuilder2;
        if (this.mService.mShuttingDown) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("During shutdown skipping ANR: ");
            stringBuilder2.append(processRecord);
            stringBuilder2.append(" ");
            stringBuilder2.append(str2);
            Slog.i("ActivityManager", stringBuilder2.toString());
        } else if (processRecord.notResponding) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Skipping duplicate ANR: ");
            stringBuilder2.append(processRecord);
            stringBuilder2.append(" ");
            stringBuilder2.append(str2);
            Slog.i("ActivityManager", stringBuilder2.toString());
        } else if (processRecord.crashing) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Crashing app skipping ANR: ");
            stringBuilder2.append(processRecord);
            stringBuilder2.append(" ");
            stringBuilder2.append(str2);
            Slog.i("ActivityManager", stringBuilder2.toString());
        } else if (processRecord.killedByAm) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("App already killed by AM skipping ANR: ");
            stringBuilder2.append(processRecord);
            stringBuilder2.append(" ");
            stringBuilder2.append(str2);
            Slog.i("ActivityManager", stringBuilder2.toString());
        } else {
            StringBuilder stringBuilder3;
            long anrTime2;
            boolean isSilentANR;
            int[] pids2;
            long anrTime3;
            int i2;
            int i3;
            if (processRecord.killed) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Skipping died app ANR: ");
                stringBuilder3.append(processRecord);
                stringBuilder3.append(" ");
                stringBuilder3.append(str2);
                Slog.i("ActivityManager", stringBuilder3.toString());
            } else if (i != processRecord.pid) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skipping ANR because pid of ");
                stringBuilder2.append(processRecord.processName);
                stringBuilder2.append(" is changed: anr pid: ");
                stringBuilder2.append(i);
                stringBuilder2.append(", new pid: ");
                stringBuilder2.append(processRecord.pid);
                stringBuilder2.append(" ");
                stringBuilder2.append(str2);
                Slog.i("ActivityManager", stringBuilder2.toString());
                return;
            } else if (this.mService.handleANRFilterFIFO(processRecord.uid, 2)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("During holding skipping ANR: ");
                stringBuilder2.append(processRecord);
                stringBuilder2.append(" ");
                stringBuilder2.append(str2);
                stringBuilder2.append("uid = ");
                stringBuilder2.append(processRecord.uid);
                Slog.i("ActivityManager", stringBuilder2.toString());
                return;
            }
            processRecord.notResponding = true;
            EventLog.writeEvent(EventLogTags.AM_ANR, new Object[]{Integer.valueOf(processRecord.userId), Integer.valueOf(processRecord.pid), processRecord.processName, Integer.valueOf(processRecord.info.flags), str2});
            firstPids.add(Integer.valueOf(processRecord.pid));
            if (this.mAppEyeANR == null || this.mAppEyeANR.check(null)) {
                anrTime2 = anrTime;
                isSilentANR = false;
            } else {
                synchronized (this.mService) {
                    boolean isSilentANR2;
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (!showBackground) {
                        try {
                            if (!isInterestingForBackgroundTraces(app)) {
                                isSilentANR2 = true;
                                if (!isSilentANR2) {
                                    int parentPid = processRecord.pid;
                                    if (activityRecord2 != null) {
                                        if (activityRecord2.app != null && activityRecord2.app.pid > 0) {
                                            parentPid = activityRecord2.app.pid;
                                        }
                                    }
                                    try {
                                        if (parentPid != processRecord.pid) {
                                            firstPids.add(Integer.valueOf(parentPid));
                                        }
                                        if (ActivityManagerService.MY_PID != processRecord.pid) {
                                            if (ActivityManagerService.MY_PID != parentPid) {
                                                firstPids.add(Integer.valueOf(ActivityManagerService.MY_PID));
                                            }
                                        }
                                        int i4 = this.mService.mLruProcesses.size() - 1;
                                        while (i4 >= 0) {
                                            ProcessRecord r = (ProcessRecord) this.mService.mLruProcesses.get(i4);
                                            if (!(r == null || r.thread == null)) {
                                                i = r.pid;
                                                if (i > 0) {
                                                    anrTime2 = anrTime;
                                                    try {
                                                        if (!(i == processRecord.pid || i == parentPid || i == ActivityManagerService.MY_PID)) {
                                                            if (r.persistent) {
                                                                firstPids.add(Integer.valueOf(i));
                                                            } else if (r.treatLikeActivity) {
                                                                firstPids.add(Integer.valueOf(i));
                                                            } else {
                                                                lastPids.put(i, Boolean.TRUE);
                                                            }
                                                        }
                                                        i4--;
                                                        anrTime = anrTime2;
                                                        i = anrPid;
                                                    } catch (Throwable th2) {
                                                        th = th2;
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                            }
                                            anrTime2 = anrTime;
                                            i4--;
                                            anrTime = anrTime2;
                                            i = anrPid;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        anrTime2 = anrTime;
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                anrTime2 = anrTime;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            anrTime2 = anrTime;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    isSilentANR2 = false;
                    if (isSilentANR2) {
                    }
                    anrTime2 = anrTime;
                }
            }
            boolean isSilentANR3 = isSilentANR;
            if (this.mAppEyeANR != null) {
                ZrHungData arg = new ZrHungData();
                arg.putString(IZRHungService.PARA_PROCNAME, processRecord.processName);
                arg.putInt(IZRHungService.PARAM_PID, processRecord.pid);
                arg.putBoolean("isSilentANR", isSilentANR3);
                arg.putString("packageName", processRecord.info.packageName);
                arg.putString(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, str2);
                if (activityRecord != null) {
                    arg.putString("activityName", activityRecord.shortComponentName);
                }
                this.mAppEyeANR.sendEvent(arg);
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.setLength(0);
            stringBuilder3.append("ANR in ");
            stringBuilder3.append(processRecord.processName);
            if (!(activityRecord == null || activityRecord.shortComponentName == null)) {
                stringBuilder3.append(" (");
                stringBuilder3.append(activityRecord.shortComponentName);
                stringBuilder3.append(")");
            }
            stringBuilder3.append("\n");
            stringBuilder3.append("PID: ");
            stringBuilder3.append(processRecord.pid);
            stringBuilder3.append("\n");
            if (str2 != null) {
                stringBuilder3.append("Reason: ");
                stringBuilder3.append(str2);
                stringBuilder3.append("\n");
            }
            if (!(activityRecord2 == null || activityRecord2 == activityRecord)) {
                stringBuilder3.append("Parent: ");
                stringBuilder3.append(activityRecord2.shortComponentName);
                stringBuilder3.append("\n");
            }
            ProcessCpuTracker processCpuTracker5 = new ProcessCpuTracker(true);
            OutputStreamWriter sysrq_trigger2 = null;
            FileOutputStream sysrq_trigger_io_stream2 = null;
            try {
                sysrq_trigger_io_stream2 = new FileOutputStream(new File("/proc/sysrq-trigger"));
                sysrq_trigger2 = new OutputStreamWriter(sysrq_trigger_io_stream2, "UTF-8");
                sysrq_trigger2.write("w");
                try {
                    sysrq_trigger2.close();
                    sysrq_trigger_io_stream2.close();
                } catch (IOException e2) {
                    Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
                    nativeProcs = null;
                    if (isSilentANR3) {
                    }
                    nativeProcs2 = nativeProcs;
                    if (nativeProcs2 == null) {
                    }
                    pids = nativeProcs2 == null ? null : Process.getPidsForCommands(nativeProcs2);
                    nativePids = null;
                    if (pids != null) {
                    }
                    pids2 = pids;
                    nativePids2 = nativePids;
                    if (isSilentANR3) {
                    }
                    if (isSilentANR3) {
                    }
                    anrTime3 = anrTime2;
                    processCpuTracker2 = processCpuTracker5;
                    anrTime2 = traceMark;
                    tracesFile = ActivityManagerService.dumpStackTraces(processRecord, true, firstPids, processCpuTracker, sparseArray, nativePids2);
                    this.mService.updateCpuStatsNow();
                    synchronized (this.mService.mProcessCpuTracker) {
                    }
                }
            } catch (IOException e3) {
                Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
                if (sysrq_trigger2 != null) {
                    try {
                        sysrq_trigger2.close();
                    } catch (IOException e4) {
                        Slog.e("ActivityManager", "Failed to write to /proc/sysrq-trigger");
                        nativeProcs = null;
                        if (isSilentANR3) {
                        }
                        nativeProcs2 = nativeProcs;
                        if (nativeProcs2 == null) {
                        }
                        pids = nativeProcs2 == null ? null : Process.getPidsForCommands(nativeProcs2);
                        nativePids = null;
                        if (pids != null) {
                        }
                        pids2 = pids;
                        nativePids2 = nativePids;
                        if (isSilentANR3) {
                        }
                        if (isSilentANR3) {
                        }
                        anrTime3 = anrTime2;
                        processCpuTracker2 = processCpuTracker5;
                        anrTime2 = traceMark;
                        tracesFile = ActivityManagerService.dumpStackTraces(processRecord, true, firstPids, processCpuTracker, sparseArray, nativePids2);
                        this.mService.updateCpuStatsNow();
                        synchronized (this.mService.mProcessCpuTracker) {
                        }
                    }
                }
                if (sysrq_trigger_io_stream2 != null) {
                    sysrq_trigger_io_stream2.close();
                }
            } catch (Throwable th5) {
                z = isSilentANR3;
                processCpuTracker3 = processCpuTracker5;
                sparseArray2 = lastPids;
                arrayList = firstPids;
                str = str2;
                j = anrTime2;
                processCpuTracker5 = aboveSystem;
                sysrq_trigger_io_stream = sysrq_trigger_io_stream2;
                sysrq_trigger = sysrq_trigger2;
                sysrq_trigger2 = th5;
                if (sysrq_trigger != null) {
                }
                if (sysrq_trigger_io_stream != null) {
                }
                throw sysrq_trigger2;
            }
            nativeProcs = null;
            if (isSilentANR3) {
                for (String equals : Watchdog.NATIVE_STACKS_OF_INTEREST) {
                    if (equals.equals(processRecord.processName)) {
                        String[] strArr = new String[1];
                        i3 = 0;
                        strArr[0] = processRecord.processName;
                        nativeProcs = strArr;
                        break;
                    }
                }
                i3 = 0;
            } else {
                i3 = 0;
                nativeProcs = Watchdog.NATIVE_STACKS_OF_INTEREST;
            }
            nativeProcs2 = nativeProcs;
            pids = nativeProcs2 == null ? null : Process.getPidsForCommands(nativeProcs2);
            nativePids = null;
            if (pids != null) {
                nativePids = new ArrayList(pids.length);
                i2 = pids.length;
                int i5 = i3;
                while (i5 < i2) {
                    int i6 = i2;
                    pids2 = pids;
                    nativePids.add(Integer.valueOf(pids[i5]));
                    i5++;
                    i2 = i6;
                    pids = pids2;
                }
            }
            pids2 = pids;
            nativePids2 = nativePids;
            processCpuTracker = isSilentANR3 ? null : processCpuTracker5;
            sparseArray = isSilentANR3 ? null : lastPids;
            anrTime3 = anrTime2;
            processCpuTracker2 = processCpuTracker5;
            anrTime2 = traceMark;
            tracesFile = ActivityManagerService.dumpStackTraces(processRecord, true, firstPids, processCpuTracker, sparseArray, nativePids2);
            this.mService.updateCpuStatsNow();
            synchronized (this.mService.mProcessCpuTracker) {
                try {
                    long anrTime4 = anrTime3;
                    try {
                        str = this.mService.mProcessCpuTracker.printCurrentState(anrTime4);
                    } catch (Throwable th6) {
                        th5 = th6;
                        processCpuTracker3 = processCpuTracker2;
                        j = anrTime4;
                        sparseArray2 = lastPids;
                        arrayList = firstPids;
                        processCpuTracker4 = stringBuilder3;
                        str = str2;
                        anrTime4 = aboveSystem;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th7) {
                                th5 = th7;
                            }
                        }
                        throw th5;
                    }
                    try {
                    } catch (Throwable th8) {
                        th5 = th8;
                        processCpuTracker3 = processCpuTracker2;
                        anrTime3 = str;
                        j = anrTime4;
                        sparseArray2 = lastPids;
                        arrayList = firstPids;
                        processCpuTracker4 = stringBuilder3;
                        str = str2;
                        anrTime4 = aboveSystem;
                        sysrq_trigger2 = anrTime3;
                        while (true) {
                            break;
                        }
                        throw th5;
                    }
                } catch (Throwable th9) {
                    th5 = th9;
                    processCpuTracker5 = aboveSystem;
                    processCpuTracker3 = processCpuTracker2;
                    sparseArray2 = lastPids;
                    arrayList = firstPids;
                    isSilentANR3 = stringBuilder3;
                    str = str2;
                    j = anrTime3;
                    while (true) {
                        break;
                    }
                    throw th5;
                }
            }
        }
    }

    private void makeAppNotRespondingLocked(ProcessRecord app, String activity, String shortMsg, String longMsg) {
        app.notResponding = true;
        app.notRespondingReport = generateProcessError(app, 2, activity, shortMsg, longMsg, null);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
    }

    /* JADX WARNING: Missing block: B:56:0x00fe, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:57:0x0101, code skipped:
            if (r2 == null) goto L_0x0106;
     */
    /* JADX WARNING: Missing block: B:58:0x0103, code skipped:
            r2.show();
     */
    /* JADX WARNING: Missing block: B:59:0x0106, code skipped:
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

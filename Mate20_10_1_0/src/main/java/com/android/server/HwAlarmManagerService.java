package com.android.server;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.CollectData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Flog;
import android.util.Slog;
import com.android.server.AlarmManagerService;
import com.android.server.devicepolicy.plugins.SettingsMDMPlugin;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import com.android.server.rms.iaware.feature.AlarmManagerFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import huawei.android.app.IHwAlarmManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HwAlarmManagerService extends AlarmManagerService {
    private static final String ACTION_ALARM_WAKEUP = "com.android.deskclock.ALARM_ALERT";
    private static final String ALARM_ID = "intent.extra.alarm_id";
    private static final String ALARM_WHEN = "intent.extra.alarm_when";
    /* access modifiers changed from: private */
    public static final boolean DEBUG_SHB = SystemProperties.getBoolean("persist.sys.shb.debug", false);
    private static final String DEFAULT_PACKAGENAME = "android";
    private static final String DESKCLOCK_PACKAGENAME_NEW = "com.huawei.deskclock";
    private static final String DESKCLOCK_PACKAGENAME_OLD = "com.android.deskclock";
    private static final String HWAIRPLANESTATE_PROPERTY = "persist.sys.hwairplanestate";
    private static final int INVALID_DATA = -1;
    private static final long INVALID_TIME = -1;
    private static final String IS_OUT_OF_DATA_ALARM = "is_out_of_data_alarm";
    private static final String IS_OWNER_ALARM = "is_owner_alarm";
    private static final boolean IS_POWEROFF_ALARM_ENABLED = "true".equals(SystemProperties.get("ro.poweroff_alarm", "true"));
    private static final String KEY_BOOT_MDM = "boot_alarm_mdm";
    private static final String KEY_PRESIST_SHUT_ALARM = "persist.sys.shut_alarm";
    private static final String LAST_TIME_CHANGED_RTC = "last_time_changed_rtc";
    private static final int LENGTH_OF_SHUT_ALARM_INFO = 2;
    private static final long MAX_OFF_SET_LOWER = 5000;
    private static final long MAX_OFF_SET_UPPER = 30000;
    private static final int MILLITOSECOND = 1000;
    private static final int NONE_LISTVIEW = 1;
    private static final int ONE_BOOT_LISTVIEW = 5;
    private static final int ONE_DESKCLOCK_LISTVIEW = 4;
    private static final int ONE_TWO_BOOT_LISTVIEW = 7;
    private static final int ONE_TWO_DESKCLOCK_LISTVIEW = 6;
    private static final String REMOVE_POWEROFF_ALARM_ANYWAY = "remove_poweroff_alarm_anyway";
    private static final String SAVE_TO_REGISTER = "save_to_register";
    private static final String SETTINGS_PACKAGENAME = "com.android.providers.settings";
    private static final String TAG = "HwAlarmManagerService";
    private static final int TWO_BOOT_DLISTVIEW = 3;
    private static final int TWO_DESKCLOCK_DLISTVIEW = 2;
    private static Map<String, Alarm> mHwWakeupBoot = new HashMap();
    private static String sPropHwRegisterName = "error";
    private static String sTimeInRegister = null;
    private Context mContext;
    private String mDeskClockName = "";
    private long mFirstElapsed;
    private long mFirstRTC;
    private boolean mIsAdjustedRTCAlarm = false;
    private boolean mIsDeskClockSetAlarm = false;
    private boolean mIsFirstPowerOffAlarm = true;
    private PendingIntent mPendingAlarm = null;
    private Alarm mPresentationAlarm = null;
    /* access modifiers changed from: private */
    public SmartHeartBeatDummy mSmartHeartBeat;

    public HwAlarmManagerService(Context context) {
        super(context);
        this.mSmartHeartBeat = SmartHeartBeat.getInstance(context, this);
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.HwAlarmManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [android.os.IBinder, com.android.server.HwAlarmManagerService$HwBinderService] */
    public void onStart() {
        HwAlarmManagerService.super.onStart();
        publishBinderService("hwAlarmService", new HwBinderService());
    }

    public void onBootPhase(int phase) {
        int alarmId;
        long alarmWhen;
        Object obj;
        HwAlarmManagerService.super.onBootPhase(phase);
        if (phase == 1000) {
            getDeskClockName();
            if (IS_POWEROFF_ALARM_ENABLED && !this.mIsDeskClockSetAlarm && ActivityManager.getCurrentUser() == 0) {
                int alarmId2 = -1;
                String[] shutAlarmInfos = SystemProperties.get(KEY_PRESIST_SHUT_ALARM, HwAPPQoEUserAction.DEFAULT_CHIP_TYPE).split(" ");
                if (shutAlarmInfos.length == 2) {
                    try {
                        alarmId2 = Integer.parseInt(shutAlarmInfos[0]);
                        alarmId = alarmId2;
                        alarmWhen = Long.parseLong(shutAlarmInfos[1]);
                    } catch (NumberFormatException e) {
                        Slog.e(TAG, "NumberFormatException : " + e);
                        alarmId = alarmId2;
                        alarmWhen = 0;
                    }
                } else {
                    alarmId = -1;
                    alarmWhen = 0;
                }
                Slog.i(TAG, "boot completed, alarmId:" + alarmId + " alarmWhen:" + alarmWhen);
                if (0 != alarmWhen) {
                    Intent intent = new Intent(ACTION_ALARM_WAKEUP);
                    intent.addFlags(16777216);
                    intent.putExtra(ALARM_ID, alarmId);
                    Slog.i(TAG, "putExtra alarmWhen " + alarmWhen);
                    intent.putExtra(ALARM_WHEN, alarmWhen);
                    intent.putExtra(IS_OWNER_ALARM, true);
                    if (alarmWhen < System.currentTimeMillis()) {
                        intent.putExtra(IS_OUT_OF_DATA_ALARM, true);
                        Slog.i(TAG, "put is_out_of_data_alarm true");
                    }
                    Object obj2 = this.mLock;
                    synchronized (obj2) {
                        try {
                            this.mPendingAlarm = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
                            obj = obj2;
                            setImpl(0, alarmWhen, 0, 0, this.mPendingAlarm, null, null, 9, null, null, 1000, "android");
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                }
            }
        }
    }

    protected class HwBinderService extends IHwAlarmManagerEx.Stub {
        protected HwBinderService() {
        }

        public void setAlarmsPending(List<String> pkgList, List<String> actionList, boolean isPending, int type) {
            if (Binder.getCallingUid() != 1000) {
                Slog.e(HwAlarmManagerService.TAG, "SmartHeartBeat:setAlarmsPending, permission not allowed. uid = " + Binder.getCallingUid());
            } else if (pkgList == null || pkgList.size() <= 0) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:setAlarmsPending, pkgList=" + pkgList);
            } else {
                if (HwAlarmManagerService.DEBUG_SHB) {
                    Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:setAlarmsPending, isPending=" + isPending + ", type=" + type);
                }
                HwAlarmManagerService.this.mSmartHeartBeat.setAlarmsPending(pkgList, actionList, isPending, type);
            }
        }

        public void removeAllPendingAlarms() {
            if (Binder.getCallingUid() != 1000) {
                Slog.e(HwAlarmManagerService.TAG, "SmartHeartBeat:removeAllPendingAlarms, permission not allowed. uid = " + Binder.getCallingUid());
                return;
            }
            if (HwAlarmManagerService.DEBUG_SHB) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:remove all pending alarms");
            }
            HwAlarmManagerService.this.mSmartHeartBeat.removeAllPendingAlarms();
        }

        public void setAlarmsAdjust(List<String> pkgList, List<String> actionList, boolean isAdjust, int type, long interval, int mode) {
            if (Binder.getCallingUid() != 1000) {
                Slog.e(HwAlarmManagerService.TAG, "SmartHeartBeat:setAlarmsAdjust, uid: " + Binder.getCallingUid());
                return;
            }
            if (HwAlarmManagerService.DEBUG_SHB) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:setAlarmsAdjust " + isAdjust);
            }
            HwAlarmManagerService.this.mSmartHeartBeat.setAlarmsAdjust(pkgList, actionList, isAdjust, type, interval, mode);
        }

        public void removeAllAdjustAlarms() {
            if (Binder.getCallingUid() != 1000) {
                Slog.e(HwAlarmManagerService.TAG, "SmartHeartBeat:removeAllAdjustAlarms, uid: " + Binder.getCallingUid());
                return;
            }
            if (HwAlarmManagerService.DEBUG_SHB) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:remove all adjust alarms.");
            }
            HwAlarmManagerService.this.mSmartHeartBeat.removeAllAdjustAlarms();
        }

        /* access modifiers changed from: protected */
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (HwAlarmManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump HwAlarmManagerService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            HwAlarmManagerService.this.mSmartHeartBeat.dump(pw);
        }
    }

    /* access modifiers changed from: protected */
    public void adjustAlarmLocked(Alarm a) {
        this.mSmartHeartBeat.adjustAlarmIfNeeded(a);
    }

    private void printDebugLog(String s) {
        if (DEBUG_SHB) {
            Slog.i(TAG, s);
        }
    }

    private List<Batch> removeBatchesOfPkgOrProcLocked(boolean isByPkgs, List<String> pkgList, List<String> procList) {
        List<Batch> batches = new ArrayList<>();
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            Iterator it = b.alarms.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Alarm a = (Alarm) it.next();
                if (!isByPkgs || !pkgList.contains(a.packageName)) {
                    if (!isByPkgs && pkgList.contains(a.packageName) && a.procName != null && procList != null && procList.contains(a.procName)) {
                        batches.add(b);
                        this.mAlarmBatches.remove(i);
                        printDebugLog("SmartHeartBeat:rebatchPkgAlarmsLocked by proc " + a.procName + " alarm:" + a);
                        break;
                    }
                } else {
                    batches.add(b);
                    this.mAlarmBatches.remove(i);
                    break;
                }
            }
        }
        return batches;
    }

    private void reAddPendingNonWakeupAlarmsOfPkgOrProcLocked(boolean isByPkgs, List<String> pkgList, List<String> procList, long nowElapsed) {
        boolean isContain;
        for (int i = this.mPendingNonWakeupAlarms.size() - 1; i >= 0; i--) {
            Alarm a = (Alarm) this.mPendingNonWakeupAlarms.get(i);
            if (isByPkgs) {
                isContain = pkgList.contains(a.packageName);
            } else {
                isContain = pkgList.contains(a.packageName) && a.procName != null && procList != null && procList.contains(a.procName);
                if (DEBUG_SHB) {
                    Slog.i(TAG, "SmartHeartBeat:rebatchPkgAlarmsLocked by pkg " + a.packageName + " proc " + a.procName + " alarm:" + a + " isContain:" + isContain);
                }
            }
            if (isContain && this.mSmartHeartBeat.shouldPendingAlarm(a)) {
                this.mPendingNonWakeupAlarms.remove(i);
                printDebugLog("readd PendingNonWakeupAlarms of " + a.packageName + " " + a);
                reAddAlarmLocked(a, nowElapsed, true);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void rebatchPkgAlarmsLocked(boolean isByPkgs, List<String> pkgList, List<String> procList) {
        if (pkgList != null && !pkgList.isEmpty()) {
            if (DEBUG_SHB) {
                Slog.i(TAG, "SmartHeartBeat:rebatchPkgAlarmsLocked, isByPkgs: " + isByPkgs + " pkgList: " + pkgList + " procList: " + procList);
            }
            Alarm oldPendingIdleUntil = this.mPendingIdleUntil;
            List<Batch> batches = removeBatchesOfPkgOrProcLocked(isByPkgs, pkgList, procList);
            long nowElapsed = SystemClock.elapsedRealtime();
            reAddPendingNonWakeupAlarmsOfPkgOrProcLocked(isByPkgs, pkgList, procList, nowElapsed);
            if (batches.size() != 0) {
                this.mIsCancelRemoveAction = true;
                for (Batch batch : batches) {
                    int batchSize = batch.size();
                    for (int i = 0; i < batchSize; i++) {
                        reAddAlarmLocked(batch.get(i), nowElapsed, true);
                    }
                }
                this.mIsCancelRemoveAction = false;
                if (!(oldPendingIdleUntil == null || oldPendingIdleUntil == this.mPendingIdleUntil)) {
                    Slog.wtf(TAG, "pkg Rebatching: idle until changed from " + oldPendingIdleUntil + " to " + this.mPendingIdleUntil);
                    if (this.mPendingIdleUntil == null) {
                        restorePendingWhileIdleAlarmsLocked();
                    }
                }
                rescheduleKernelAlarmsLocked();
                updateNextAlarmClockLocked();
                if (DEBUG_SHB) {
                    Slog.i(TAG, "SmartHeartBeat:rebatchPkgAlarmsLocked end");
                }
            }
        }
    }

    private String getDeskClockName() {
        if ("".equals(this.mDeskClockName)) {
            this.mDeskClockName = getSystemAppForDeskClock(DESKCLOCK_PACKAGENAME_NEW, DESKCLOCK_PACKAGENAME_OLD);
        }
        return this.mDeskClockName;
    }

    private String getSystemAppForDeskClock(String packageName1, String packageName2) {
        if (isSystemApp(packageName1)) {
            return packageName1;
        }
        if (isSystemApp(packageName2)) {
            return packageName2;
        }
        return "";
    }

    private boolean isSystemApp(String packageName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            return false;
        }
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            if (!(appInfo == null || (appInfo.flags & 1) == 0)) {
                Slog.i(TAG, packageName + " is SystemApp.");
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, packageName + " not found.");
        }
        return false;
    }

    private void decideRtcPrioritySet(Alarm decidingAlarm) {
        String mayInvolvedPackageName;
        String targetPackageName = decidingAlarm.operation.getTargetPackage();
        if ("android".equals(targetPackageName)) {
            targetPackageName = getDeskClockName();
        }
        if (getDeskClockName().equals(targetPackageName)) {
            mayInvolvedPackageName = SETTINGS_PACKAGENAME;
        } else if (SETTINGS_PACKAGENAME.equals(targetPackageName)) {
            mayInvolvedPackageName = getDeskClockName();
        } else {
            Slog.w(TAG, "decideRtcPrioritySet--packagename error, return directly");
            return;
        }
        if (mHwWakeupBoot.containsKey(targetPackageName)) {
            mHwWakeupBoot.remove(targetPackageName);
        }
        Slog.i(TAG, "decideRtcPrioritySet--put " + targetPackageName + ", to map");
        mHwWakeupBoot.put(targetPackageName, decidingAlarm);
        Alarm mayInvolvedAlarm = null;
        if (mayInvolvedPackageName != null && mHwWakeupBoot.containsKey(mayInvolvedPackageName)) {
            mayInvolvedAlarm = mHwWakeupBoot.get(mayInvolvedPackageName);
        }
        Alarm decideResultAlarm = null;
        if (mayInvolvedAlarm == null) {
            decideResultAlarm = decidingAlarm;
        } else if (decidingAlarm.when > mayInvolvedAlarm.when) {
            decideResultAlarm = mayInvolvedAlarm;
        } else if (decidingAlarm.when < mayInvolvedAlarm.when) {
            decideResultAlarm = decidingAlarm;
        } else if (getDeskClockName().equals(decidingAlarm.operation.getTargetPackage())) {
            decideResultAlarm = decidingAlarm;
        } else if (getDeskClockName().equals(mayInvolvedAlarm.operation.getTargetPackage())) {
            decideResultAlarm = mayInvolvedAlarm;
        }
        if (decideResultAlarm != null) {
            Slog.i(TAG, "have calculate RTC result and will set to lock");
            hwSetRtcLocked(decideResultAlarm);
        }
    }

    private void decideRtcPriorityRemove(PendingIntent operation) {
        String mayInvolvedPackageName;
        String targetPackageName = operation.getTargetPackage();
        if ("android".equals(targetPackageName)) {
            targetPackageName = getDeskClockName();
        }
        if (getDeskClockName().equals(targetPackageName)) {
            mayInvolvedPackageName = SETTINGS_PACKAGENAME;
        } else if (SETTINGS_PACKAGENAME.equals(targetPackageName)) {
            mayInvolvedPackageName = getDeskClockName();
        } else {
            Slog.w(TAG, "decideRtcPriorityRemove--packagename error, return directly");
            return;
        }
        if (mHwWakeupBoot.isEmpty()) {
            Slog.w(TAG, "decideRtcPriorityRemove--mHwWakeupBoot is empty, return directly");
            hwRemoveRtcLocked();
            return;
        }
        if (mHwWakeupBoot.containsKey(targetPackageName)) {
            Slog.w(TAG, "decideRtcPriorityRemove--remove " + targetPackageName);
            mHwWakeupBoot.remove(targetPackageName);
        }
        if (mayInvolvedPackageName == null || !mHwWakeupBoot.containsKey(mayInvolvedPackageName)) {
            mHwWakeupBoot.clear();
            Slog.i(TAG, "mHwWakeupBoot have clear");
            hwRemoveRtcLocked();
            return;
        }
        Alarm decideResultAlarm = mHwWakeupBoot.get(mayInvolvedPackageName);
        if (decideResultAlarm != null) {
            Slog.i(TAG, "decideRtcPriorityRemove, hwSetRtcLocked");
            hwSetRtcLocked(decideResultAlarm);
        }
    }

    private boolean isSettings(Alarm a) {
        if (!SETTINGS_PACKAGENAME.equals(a.operation.getTargetPackage()) || !isOperationHasExtra(a.operation, SAVE_TO_REGISTER, false)) {
            return false;
        }
        return true;
    }

    private boolean isDeskClock(Alarm a) {
        if (!getDeskClockName().equals(a.operation.getTargetPackage()) || !isOperationHasExtra(a.operation, IS_OWNER_ALARM, false)) {
            return "android".equals(a.operation.getTargetPackage()) && isOperationHasExtra(a.operation, IS_OWNER_ALARM, false);
        }
        return true;
    }

    private boolean isDeleteDeskClock(Alarm a) {
        if (!getDeskClockName().equals(a.operation.getTargetPackage()) || !isOperationHasExtra(a.operation, REMOVE_POWEROFF_ALARM_ANYWAY, false) || ActivityManager.getCurrentUser() != 0) {
            return false;
        }
        return true;
    }

    private void saveDeskClock(Alarm a) {
        if (getDeskClockName().equals(a.operation.getTargetPackage()) && isOperationHasExtra(a.operation, IS_OWNER_ALARM, false)) {
            this.mIsDeskClockSetAlarm = true;
            synchronized (this.mLock) {
                if (this.mPendingAlarm != null) {
                    Slog.i(TAG, "set deskclock after boot, remove Shutdown alarm from framework");
                    removeLocked(this.mPendingAlarm, null);
                    this.mPendingAlarm = null;
                }
            }
            long alarmWhen = a.when;
            int alarmId = resetIntExtraCallingIdentity(a.operation, ALARM_ID, -1);
            if (alarmId != -1) {
                Slog.i(TAG, "set shutdownAlarm " + alarmId + ", " + alarmWhen);
                StringBuilder sb = new StringBuilder();
                sb.append(alarmId);
                sb.append(" ");
                sb.append(alarmWhen);
                SystemProperties.set(KEY_PRESIST_SHUT_ALARM, sb.toString());
            }
        }
    }

    private void removeDeskClock(Alarm a) {
        if (isDeskClock(a) || isDeleteDeskClock(a)) {
            setShutDownAlarmNone();
        }
    }

    /* access modifiers changed from: protected */
    public void removeDeskClockFromFWK(PendingIntent operation) {
        if (operation != null && isOperationHasExtra(operation, REMOVE_POWEROFF_ALARM_ANYWAY, false) && ActivityManager.getCurrentUser() == 0) {
            synchronized (this.mLock) {
                Slog.i(TAG, "no deskClock alarm enable, remove Shutdown alarm from framework");
                if (this.mPendingAlarm != null) {
                    removeLocked(this.mPendingAlarm, null);
                    this.mPendingAlarm = null;
                }
            }
            setShutDownAlarmNone();
        }
    }

    private void setShutDownAlarmNone() {
        Slog.i(TAG, "remove shutdownAlarm : none");
        SystemProperties.set(KEY_PRESIST_SHUT_ALARM, HwAPPQoEUserAction.DEFAULT_CHIP_TYPE);
    }

    private void hwSetRtcLocked(Alarm alarm) {
        long alarmSeconds;
        long alarmSeconds2;
        long alarmWhen = alarm.when;
        if (System.currentTimeMillis() > alarmWhen) {
            Slog.i(TAG, "hwSetRtcLocked--missed alarm, not set RTC to driver");
            return;
        }
        if (alarmWhen < 0) {
            alarmSeconds2 = 0;
            alarmSeconds = 0;
        } else {
            alarmSeconds2 = alarmWhen / 1000;
            alarmSeconds = 1000 * (alarmWhen % 1000) * 1000;
        }
        Slog.i(TAG, "set RTC alarm Locked, time = " + getTimeString(alarmWhen));
        sTimeInRegister = getTimeString(alarmWhen);
        sPropHwRegisterName = alarm.operation.getTargetPackage();
        this.mInjector.hwSetClockRTC(alarmSeconds2, alarmSeconds);
    }

    private void hwRemoveRtcLocked() {
        Slog.i(TAG, "remove RTC alarm time Locked");
        sTimeInRegister = HwAPPQoEUtils.INVALID_STRING_VALUE;
        sPropHwRegisterName = "error";
        this.mInjector.hwSetClockRTC(0, 0);
    }

    private String getTimeString(long milliSec) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(milliSec)).toString();
    }

    /* access modifiers changed from: protected */
    public void printHwWakeupBoot(PrintWriter pw) {
        Map<String, Alarm> map;
        if (!this.mIsAdjustedRTCAlarm && (map = mHwWakeupBoot) != null && map.size() > 0) {
            pw.println();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw.print("HW WakeupBoot MAP: ");
            pw.println();
            pw.print("Register time:");
            pw.print(sTimeInRegister);
            pw.println();
            if (mHwWakeupBoot.size() > 0) {
                for (Map.Entry<String, Alarm> entry : mHwWakeupBoot.entrySet()) {
                    pw.print("packageName=");
                    pw.print(entry.getKey());
                    pw.print("  time=");
                    pw.print(sdf.format(new Date(entry.getValue().when)));
                    pw.println();
                }
            }
        }
    }

    private boolean isPresentationAlarm(Alarm alarm) {
        if (!"android".equals(alarm.operation.getTargetPackage()) || !isOperationHasExtra(alarm.operation, KEY_BOOT_MDM, false)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void hwRemoveRtcAlarm(Alarm alarm, boolean isCancel) {
        if (alarm != null) {
            reportAlarmForAware(alarm, isCancel ? 1 : 2);
            if (alarm.operation != null) {
                if (isPresentationAlarm(alarm)) {
                    this.mPresentationAlarm = null;
                    Slog.i(TAG, "hwRemoveRtcAlarm MDM");
                }
                if (IS_POWEROFF_ALARM_ENABLED && alarm.type == 0) {
                    removeDeskClock(alarm);
                    if (!this.mIsAdjustedRTCAlarm) {
                        if (isSettings(alarm) || isDeskClock(alarm) || isDeleteDeskClock(alarm)) {
                            decideRtcPriorityRemove(alarm.operation);
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void hwSetRtcAlarm(Alarm alarm) {
        if (alarm != null) {
            reportAlarmForAware(alarm, 0);
            if (alarm.operation != null) {
                if (isPresentationAlarm(alarm)) {
                    this.mPresentationAlarm = alarm;
                    Slog.i(TAG, "hwSetRtcAlarm MDM");
                }
                if (IS_POWEROFF_ALARM_ENABLED && alarm.type == 0) {
                    saveDeskClock(alarm);
                    if (!this.mIsAdjustedRTCAlarm) {
                        if (isSettings(alarm) || isDeskClock(alarm)) {
                            decideRtcPrioritySet(alarm);
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void hwAddFirstFlagForRtcAlarm(Alarm alarm, Intent backgroundIntent) {
        if (alarm != null && alarm.operation != null && isDeskClock(alarm)) {
            if (!this.mIsFirstPowerOffAlarm || !"RTC".equals(SystemProperties.get("persist.sys.powerup_reason", SettingsMDMPlugin.CONFIG_NORMAL_VALUE))) {
                setIntentInfo(backgroundIntent, false);
                return;
            }
            setIntentInfo(backgroundIntent, true);
            this.mIsFirstPowerOffAlarm = false;
        }
    }

    private void setIntentInfo(Intent backgroundIntent, boolean isFirstPowerOffAlarm) {
        Slog.i(TAG, "FLAG_IS_FIRST_POWER_OFF_ALARM :" + isFirstPowerOffAlarm);
        backgroundIntent.putExtra("FLAG_IS_FIRST_POWER_OFF_ALARM", isFirstPowerOffAlarm);
    }

    /* access modifiers changed from: protected */
    public long checkHasHwRTCAlarmLock(String packageName) {
        Alarm rtcAlarm = null;
        long time = -1;
        synchronized (this.mLock) {
            if (packageName != null) {
                try {
                    if (mHwWakeupBoot.containsKey(packageName)) {
                        rtcAlarm = mHwWakeupBoot.get(packageName);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (rtcAlarm != null) {
                if (rtcAlarm.operation != null) {
                    if (isSettings(rtcAlarm) || isDeskClock(rtcAlarm)) {
                        time = rtcAlarm.when;
                    }
                    return time;
                }
            }
            return -1;
        }
    }

    /* access modifiers changed from: protected */
    public void adjustHwRTCAlarmLock(boolean isDeskClock, boolean isBoot, int typeState) {
        Slog.i(TAG, "adjust RTC Alarm");
        synchronized (this.mLock) {
            this.mIsAdjustedRTCAlarm = true;
            switch (typeState) {
                case 1:
                    clearRtcAlarm(typeState);
                    break;
                case 2:
                case 3:
                case 6:
                case 7:
                    if (!isDeskClock && !isBoot) {
                        clearRtcAlarm(typeState);
                        break;
                    } else {
                        if (!isDeskClock || isBoot) {
                            if (!isDeskClock && isBoot) {
                                Slog.i(TAG, "typeState:" + typeState + ", user cancel deskClockTime RTC alarm");
                                decideRtcPriorityRemove(mHwWakeupBoot.get(getDeskClockName()).operation);
                                break;
                            }
                        } else {
                            Slog.i(TAG, "typeState:" + typeState + ", user cancel bootOnTime RTC alarm");
                            decideRtcPriorityRemove(mHwWakeupBoot.get(SETTINGS_PACKAGENAME).operation);
                        }
                        break;
                    }
                case 4:
                    if (!isDeskClock) {
                        clearRtcAlarm(typeState);
                        break;
                    }
                    break;
                case 5:
                    if (!isBoot) {
                        clearRtcAlarm(typeState);
                        break;
                    }
                    break;
            }
        }
    }

    private void clearRtcAlarm(int typeState) {
        mHwWakeupBoot.clear();
        Slog.i(TAG, "typeState:" + typeState + ", user cancel RTC alarm");
        hwRemoveRtcLocked();
    }

    private void setHwAirPlaneStatePropLock() {
        SystemProperties.set(HWAIRPLANESTATE_PROPERTY, sPropHwRegisterName);
        Slog.i(TAG, "hw airplane prop locked = " + sPropHwRegisterName);
    }

    /* access modifiers changed from: protected */
    public void setHwRTCAlarmLock() {
        Alarm optAlarm2;
        Alarm alarm = this.mPresentationAlarm;
        if (alarm != null) {
            hwSetRtcLocked(alarm);
            setHwAirPlaneStatePropLock();
            return;
        }
        long deskTime = checkHasHwRTCAlarmLock(getDeskClockName());
        long settingsTime = checkHasHwRTCAlarmLock(SETTINGS_PACKAGENAME);
        if (deskTime == -1 && settingsTime == -1) {
            Slog.i(TAG, "setHwRTCAlarmLock-- not set RTC to driver");
            setHwAirPlaneStatePropLock();
            return;
        }
        if (deskTime == -1) {
            optAlarm2 = mHwWakeupBoot.get(SETTINGS_PACKAGENAME);
        } else if (settingsTime == -1) {
            optAlarm2 = mHwWakeupBoot.get(getDeskClockName());
        } else {
            Alarm optAlarm1 = mHwWakeupBoot.get(getDeskClockName());
            optAlarm2 = mHwWakeupBoot.get(SETTINGS_PACKAGENAME);
            if (optAlarm1.when <= optAlarm2.when) {
                optAlarm2 = optAlarm1;
            }
        }
        hwSetRtcLocked(optAlarm2);
        setHwAirPlaneStatePropLock();
    }

    /* access modifiers changed from: protected */
    public void hwRecordFirstTime() {
        this.mFirstRTC = System.currentTimeMillis();
        this.mFirstElapsed = SystemClock.elapsedRealtime();
        if (Settings.Global.getLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, 0) == 0) {
            Flog.i(500, "hwRecordFirstTime init for TimeKeeper at " + this.mFirstRTC);
            Settings.Global.putLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, this.mFirstRTC);
        }
    }

    /* access modifiers changed from: protected */
    public void hwRecordTimeChangeRTC(long nowRTC, long nowElapsed, long lastTimeChangeClockTime, long expectedClockTime) {
        long maxOffset;
        long expectedRTC;
        if (lastTimeChangeClockTime == 0) {
            expectedRTC = this.mFirstRTC + (nowElapsed - this.mFirstElapsed);
            maxOffset = 30000;
        } else {
            expectedRTC = expectedClockTime;
            maxOffset = 5000;
        }
        long offset = nowRTC - expectedRTC;
        if (offset < (-maxOffset) || nowRTC > maxOffset) {
            Flog.i(500, "hwRecordTimeChangeRTC for TimeKeeper at " + nowRTC + ", offset=" + offset);
            Settings.Global.putLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, nowRTC);
        }
    }

    private boolean isOperationHasExtra(PendingIntent operation, String extra, boolean isExtraValue) {
        long identity = Binder.clearCallingIdentity();
        boolean isExtra = false;
        try {
            isExtra = operation.getIntent().getBooleanExtra(extra, isExtraValue);
        } catch (Exception e) {
            Slog.e(TAG, "getBooleanExtra error, extra:" + extra);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        return isExtra;
    }

    private int resetIntExtraCallingIdentity(PendingIntent operation, String extra, int value) {
        long identity = Binder.clearCallingIdentity();
        int extraRes = -1;
        try {
            extraRes = operation.getIntent().getIntExtra(extra, value);
        } catch (Exception e) {
            Slog.e(TAG, "getIntExtra error, extra:" + extra);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
        return extraRes;
    }

    /* access modifiers changed from: protected */
    public boolean isContainsAppUidInWorksource(WorkSource workSource, String packageName) {
        if (workSource == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            if (!containsUidInWorksource(workSource, getContext().getPackageManager().getApplicationInfo(packageName, 1).uid)) {
                return false;
            }
            Slog.i(TAG, "isContainsAppUidInWorksource-->worksource contains app's.uid");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean containsUidInWorksource(WorkSource workSource, int uid) {
        if (workSource == null || workSource.size() <= 0) {
            return false;
        }
        int length = workSource.size();
        for (int i = 0; i < length; i++) {
            if (uid == workSource.get(i)) {
                return true;
            }
        }
        return false;
    }

    public void removePackageAlarm(final String pkg, final List<String> tags, final int targetUid) {
        if (this.mHandler != null) {
            this.mHandler.post(new Runnable() {
                /* class com.android.server.HwAlarmManagerService.AnonymousClass1 */

                public void run() {
                    synchronized (HwAlarmManagerService.this.mLock) {
                        HwAlarmManagerService.this.removeByTagLocked(pkg, tags, targetUid);
                    }
                }
            });
        }
    }

    private boolean removeSingleTagLocked(String packageName, String tag, int targetUid) {
        boolean didRemove = false;
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            didRemove |= removeFromBatch(b, packageName, tag, targetUid);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (int i2 = this.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
            Alarm a = (Alarm) this.mPendingWhileIdleAlarms.get(i2);
            if (a.matches(packageName) && targetUid == a.creatorUid) {
                this.mPendingWhileIdleAlarms.remove(i2);
                decrementAlarmCount(a.uid, 1);
                Flog.i(500, "remove pending idle:" + a + " by pkg:" + packageName + ", tag:" + tag + ", uid:" + targetUid);
            }
        }
        return didRemove;
    }

    /* access modifiers changed from: private */
    public void removeByTagLocked(String packageName, List<String> tags, int targetUid) {
        boolean didRemove = false;
        if (packageName != null) {
            if (tags == null) {
                didRemove = removeSingleTagLocked(packageName, null, targetUid);
            } else {
                for (String tag : tags) {
                    didRemove |= removeSingleTagLocked(packageName, tag, targetUid);
                }
            }
            if (didRemove) {
                rebatchAllAlarmsLocked(true);
                rescheduleKernelAlarmsLocked();
                updateNextAlarmClockLocked();
            }
        }
    }

    private boolean canRemove(Alarm alarm, String tag) {
        if (tag == null) {
            return true;
        }
        String alarmTag = Alarm.makeTag(alarm.operation, alarm.listenerTag, alarm.type);
        if (alarmTag == null) {
            return false;
        }
        String[] splits = alarmTag.split(AwarenessInnerConstants.COLON_KEY);
        if (splits.length > 1 && splits[1].equals(tag)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean removeFromBatch(Batch batch, String packageName, String tag, int targetUid) {
        if (packageName == null) {
            return false;
        }
        boolean didRemove = false;
        long newStart = 0;
        long newEnd = Long.MAX_VALUE;
        int newFlags = 0;
        int i = 1;
        for (int i2 = batch.alarms.size() - 1; i2 >= 0; i2--) {
            Alarm alarm = (Alarm) batch.alarms.get(i2);
            if (!alarm.matches(packageName) || targetUid != alarm.creatorUid) {
                if (alarm.whenElapsed > newStart) {
                    newStart = alarm.whenElapsed;
                }
                if (alarm.maxWhenElapsed < newEnd) {
                    newEnd = alarm.maxWhenElapsed;
                }
                newFlags |= alarm.flags;
            } else if (canRemove(alarm, tag)) {
                batch.alarms.remove(i2);
                decrementAlarmCount(alarm.uid, i);
                didRemove = true;
                Flog.i(500, "remove " + alarm + " by pkg:" + packageName + ", tag:" + tag + ", targetUid: " + targetUid);
                i = 1;
                hwRemoveRtcAlarm(alarm, true);
                if (alarm.alarmClock != null) {
                    this.mNextAlarmClockMayChange = true;
                }
            }
        }
        if (didRemove) {
            batch.start = newStart;
            batch.end = newEnd;
            batch.flags = newFlags;
        }
        return didRemove;
    }

    private void reportAlarmForAware(Alarm alarm, int operation) {
        HwSysResManager resManager;
        if (alarm != null && alarm.packageName != null && alarm.statsTag != null && (resManager = HwSysResManager.getInstance()) != null && resManager.isResourceNeeded(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC))) {
            Bundle bundleArgs = new Bundle();
            bundleArgs.putString(MemoryConstant.MEM_PREREAD_ITEM_NAME, alarm.packageName);
            bundleArgs.putString("statstag", alarm.statsTag);
            bundleArgs.putInt("relationType", 22);
            bundleArgs.putInt("alarm_operation", operation);
            bundleArgs.putInt("tgtUid", alarm.creatorUid);
            CollectData data = new CollectData(AwareConstant.ResourceType.getReousrceId(AwareConstant.ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }

    /* access modifiers changed from: protected */
    public void modifyAlarmIfOverload(Alarm alarm) {
        AwareWakeUpManager.getInstance().modifyAlarmIfOverload(new HwAlarm(alarm));
    }

    /* access modifiers changed from: protected */
    public void reportWakeupAlarms(ArrayList<Alarm> alarms) {
        ArrayList<HwAlarm> hwAlarms = new ArrayList<>();
        Iterator<Alarm> it = alarms.iterator();
        while (it.hasNext()) {
            hwAlarms.add(new HwAlarm(it.next()));
        }
        AwareWakeUpManager.getInstance().reportWakeupAlarms(hwAlarms);
    }

    /* access modifiers changed from: protected */
    public boolean isAwareAlarmManagerEnabled() {
        return AlarmManagerFeature.isEnable();
    }

    /* access modifiers changed from: protected */
    public int getWakeUpNumImpl(int uid, String pkg) {
        int i;
        synchronized (this.mLock) {
            ArrayMap<String, BroadcastStats> uidStats = (ArrayMap) this.mBroadcastStats.get(uid);
            if (uidStats == null) {
                uidStats = new ArrayMap<>();
                this.mBroadcastStats.put(uid, uidStats);
            }
            BroadcastStats bs = uidStats.get(pkg);
            if (bs == null) {
                bs = new BroadcastStats(uid, pkg);
                uidStats.put(pkg, bs);
            }
            i = bs.numWakeup;
        }
        return i;
    }

    public static class HwAlarm {
        private Alarm mAlarm = null;

        HwAlarm(Alarm alarm) {
            this.mAlarm = alarm;
        }

        public int getType() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return -1;
            }
            return alarm.type;
        }

        public int getUid() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return -1;
            }
            return alarm.uid;
        }

        public String getPkgName() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return null;
            }
            return alarm.packageName;
        }

        public boolean getWakeup() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return false;
            }
            return alarm.wakeup;
        }

        public String getStatsTag() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return null;
            }
            return alarm.statsTag;
        }

        public long getWhenElapsed() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return 0;
            }
            return alarm.whenElapsed;
        }

        public long getWindowLength() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return 0;
            }
            return alarm.windowLength;
        }

        public long getRepeatInterval() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return 0;
            }
            return alarm.repeatInterval;
        }

        public long getMaxWhenElapsed() {
            Alarm alarm = this.mAlarm;
            if (alarm == null) {
                return 0;
            }
            return alarm.maxWhenElapsed;
        }

        public void setWhenElapsed(long whenElapsed) {
            Alarm alarm = this.mAlarm;
            if (alarm != null) {
                alarm.whenElapsed = whenElapsed;
            }
        }

        public void setMaxWhenElapsed(long maxWhenElapsed) {
            Alarm alarm = this.mAlarm;
            if (alarm != null) {
                alarm.maxWhenElapsed = maxWhenElapsed;
            }
        }

        public void setWakeup(boolean isWakeup) {
            Alarm alarm = this.mAlarm;
            if (alarm != null) {
                alarm.wakeup = isWakeup;
            }
        }
    }
}

package com.android.server;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Slog;
import com.android.server.AlarmManagerService.Alarm;
import com.android.server.AlarmManagerService.AlarmHandler;
import com.android.server.AlarmManagerService.Batch;
import com.android.server.devicepolicy.plugins.SettingsMDMPlugin;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import com.android.server.rms.iaware.feature.AlarmManagerFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.android.app.IHwAlarmManagerEx.Stub;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

public class HwAlarmManagerService extends AlarmManagerService {
    private static final String ACTION_ALARM_WAKEUP = "com.android.deskclock.ALARM_ALERT";
    private static final String ALARM_ID = "intent.extra.alarm_id";
    private static final String ALARM_WHEN = "intent.extra.alarm_when";
    private static boolean DEBUG_SHB = false;
    private static final boolean DEBUG_ST = false;
    private static final String DESKCLOCK_PACKAGENAME = "com.android.deskclock";
    private static final String HWAIRPLANESTATE_PROPERTY = "persist.sys.hwairplanestate";
    private static final boolean IS_DEVICE_ENCRYPTED = "encrypted".equals(SystemProperties.get("ro.crypto.state", ""));
    private static final String IS_OUT_OF_DATA_ALARM = "is_out_of_data_alarm";
    private static final String IS_OWNER_ALARM = "is_owner_alarm";
    private static final boolean IS_POWEROFF_ALARM_ENABLED = "true".equals(SystemProperties.get("ro.poweroff_alarm", "true"));
    private static final String KEY_BOOT_MDM = "boot_alarm_mdm";
    private static final String LAST_TIME_CHANGED_RTC = "last_time_changed_rtc";
    private static final int NONE_LISTVIEW = 1;
    private static final int ONE_BOOT_LISTVIEW = 5;
    private static final int ONE_DESKCLOCK_LISTVIEW = 4;
    private static final int ONE_TWO_BOOT_LISTVIEW = 7;
    private static final int ONE_TWO_DESKCLOCK_LISTVIEW = 6;
    private static final String REMOVE_POWEROFF_ALARM_ANYWAY = "remove_poweroff_alarm_anyway";
    private static final String SAVE_TO_REGISTER = "save_to_register";
    private static final String SETTINGS_PACKAGENAME = "com.android.providers.settings";
    static final String TAG = "HwAlarmManagerService";
    private static final int TRIM_ALARM_POST_MSG_DELAY = 10;
    private static final int TWO_BOOT_DLISTVIEW = 3;
    private static final int TWO_DESKCLOCK_DLISTVIEW = 2;
    private static HashMap<String, Alarm> mHwWakeupBoot = new HashMap();
    private static String mPropHwRegisterName = "error";
    private static String timeInRegister = null;
    private boolean hasDeskClocksetAlarm = false;
    private boolean isSetHwMDMAlarm = false;
    Context mContext;
    private long mFirstELAPSED;
    private long mFirstRTC;
    private boolean mHwAlarmLock = false;
    private Alarm mHwMDMAlarm = null;
    private boolean mIsFirstPowerOffAlarm = true;
    private PendingIntent mPendingAlarm = null;
    private SmartHeartBeatDummy mSmartHB = null;
    private HashSet<String> mTrimAlarmPkg = null;

    public class HwAlarmHandler extends AlarmHandler {
        public static final int TRIM_PKG_ALARM = 5;

        public HwAlarmHandler() {
            super(HwAlarmManagerService.this);
        }

        public void handleMessage(Message msg) {
            if (5 == msg.what) {
                HwAlarmManagerService.this.removeByPkg_hwHsm(HwAlarmManagerService.this.mTrimAlarmPkg);
                HwAlarmManagerService.this.mTrimAlarmPkg.clear();
            }
            super.handleMessage(msg);
        }
    }

    protected class HwBinderService extends Stub {
        protected HwBinderService() {
        }

        public void setAlarmsPending(List<String> pkgList, List<String> actionList, boolean pending, int type) {
            String str;
            StringBuilder stringBuilder;
            if (1000 != Binder.getCallingUid()) {
                str = HwAlarmManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:setAlarmsPending, permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
            } else if (pkgList == null || pkgList.size() <= 0) {
                str = HwAlarmManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:setAlarmsPending, pkgList=");
                stringBuilder.append(pkgList);
                Slog.i(str, stringBuilder.toString());
            } else {
                if (HwAlarmManagerService.DEBUG_SHB) {
                    str = HwAlarmManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SmartHeartBeat:setAlarmsPending, pending=");
                    stringBuilder.append(pending);
                    stringBuilder.append(", type=");
                    stringBuilder.append(type);
                    Slog.i(str, stringBuilder.toString());
                }
                HwAlarmManagerService.this.mSmartHB.setAlarmsPending(pkgList, actionList, pending, type);
            }
        }

        public void removeAllPendingAlarms() {
            if (1000 != Binder.getCallingUid()) {
                String str = HwAlarmManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:removeAllPendingAlarms, permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return;
            }
            if (HwAlarmManagerService.DEBUG_SHB) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:remove all pending alarms");
            }
            HwAlarmManagerService.this.mSmartHB.removeAllPendingAlarms();
        }

        public void setAlarmsAdjust(List<String> pkgList, List<String> actionList, boolean adjust, int type, long interval, int mode) {
            String str;
            StringBuilder stringBuilder;
            if (1000 != Binder.getCallingUid()) {
                str = HwAlarmManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:setAlarmsAdjust, uid: ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return;
            }
            boolean z;
            if (HwAlarmManagerService.DEBUG_SHB) {
                str = HwAlarmManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:setAlarmsAdjust ");
                z = adjust;
                stringBuilder.append(z);
                Slog.i(str, stringBuilder.toString());
            } else {
                z = adjust;
            }
            HwAlarmManagerService.this.mSmartHB.setAlarmsAdjust(pkgList, actionList, z, type, interval, mode);
        }

        public void removeAllAdjustAlarms() {
            if (1000 != Binder.getCallingUid()) {
                String str = HwAlarmManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:removeAllAdjustAlarms, uid: ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return;
            }
            if (HwAlarmManagerService.DEBUG_SHB) {
                Slog.i(HwAlarmManagerService.TAG, "SmartHeartBeat:remove all adjust alarms.");
            }
            HwAlarmManagerService.this.mSmartHB.removeAllAdjustAlarms();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (HwAlarmManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: can't dump HwAlarmManagerService from pid=");
                stringBuilder.append(Binder.getCallingPid());
                stringBuilder.append(", uid=");
                stringBuilder.append(Binder.getCallingUid());
                pw.println(stringBuilder.toString());
                return;
            }
            HwAlarmManagerService.this.mSmartHB.dump(pw);
        }
    }

    public HwAlarmManagerService(Context context) {
        super(context);
        this.mSmartHB = SmartHeartBeat.getInstance(context, this);
        DEBUG_SHB = SmartHeartBeat.DEBUG_HEART_BEAT;
        this.mContext = context;
    }

    public void onStart() {
        super.onStart();
        publishBinderService("hwAlarmService", new HwBinderService());
    }

    public void onBootPhase(int phase) {
        Throwable th;
        super.onBootPhase(phase);
        if (phase == 1000 && IS_POWEROFF_ALARM_ENABLED && !this.hasDeskClocksetAlarm && ActivityManager.getCurrentUser() == 0) {
            int alarm_id = -1;
            long alarm_when = 0;
            String shutAlarm = SystemProperties.get("persist.sys.shut_alarm", "none");
            String[] s = shutAlarm.split(" ");
            if (s.length == 2) {
                try {
                    alarm_id = Integer.parseInt(s[0]);
                    alarm_when = Long.parseLong(s[1]);
                } catch (NumberFormatException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("NumberFormatException : ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                }
            }
            int alarm_id2 = alarm_id;
            long alarm_when2 = alarm_when;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("boot completed, alarmId:");
            stringBuilder2.append(alarm_id2);
            stringBuilder2.append(" alarm_when:");
            stringBuilder2.append(alarm_when2);
            Slog.d(str2, stringBuilder2.toString());
            if (0 != alarm_when2) {
                long now = System.currentTimeMillis();
                Intent intent = new Intent(ACTION_ALARM_WAKEUP);
                intent.addFlags(16777216);
                intent.putExtra(ALARM_ID, alarm_id2);
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("putExtra alarm_when ");
                stringBuilder2.append(alarm_when2);
                Slog.d(str2, stringBuilder2.toString());
                intent.putExtra(ALARM_WHEN, alarm_when2);
                intent.putExtra(IS_OWNER_ALARM, true);
                if (alarm_when2 < now) {
                    intent.putExtra(IS_OUT_OF_DATA_ALARM, true);
                    Slog.d(TAG, "put is_out_of_data_alarm true");
                }
                Object obj = this.mLock;
                synchronized (obj) {
                    Object obj2;
                    try {
                        this.mPendingAlarm = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
                        obj2 = obj;
                        setImpl(0, alarm_when2, 0, 0, this.mPendingAlarm, null, null, 9, null, null, 1000, "android");
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
            }
        }
    }

    protected void adjustAlarmLocked(Alarm a) {
        this.mSmartHB.adjustAlarmIfNeeded(a);
    }

    protected void rebatchPkgAlarmsLocked(List<String> pkgList) {
        if (pkgList != null && !pkgList.isEmpty()) {
            int i;
            if (DEBUG_SHB) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SmartHeartBeat:rebatchPkgAlarmsLocked, pkgList: ");
                stringBuilder.append(pkgList);
                Slog.i(str, stringBuilder.toString());
            }
            Alarm oldPendingIdleUntil = this.mPendingIdleUntil;
            ArrayList<Batch> batches = new ArrayList();
            int i2 = this.mAlarmBatches.size() - 1;
            while (true) {
                int j = 0;
                if (i2 < 0) {
                    break;
                }
                Batch b = (Batch) this.mAlarmBatches.get(i2);
                int alarmSize = b.alarms.size();
                while (j < alarmSize) {
                    if (pkgList.contains(((Alarm) b.alarms.get(j)).packageName)) {
                        batches.add(b);
                        this.mAlarmBatches.remove(i2);
                        break;
                    }
                    j++;
                }
                i2--;
            }
            long nowElapsed = SystemClock.elapsedRealtime();
            for (i = this.mPendingNonWakeupAlarms.size() - 1; i >= 0; i--) {
                Alarm a = (Alarm) this.mPendingNonWakeupAlarms.get(i);
                if (pkgList.contains(a.packageName) && this.mSmartHB.shouldPendingAlarm(a)) {
                    this.mPendingNonWakeupAlarms.remove(i);
                    if (DEBUG_SHB) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("readd PendingNonWakeupAlarms of ");
                        stringBuilder2.append(a.packageName);
                        stringBuilder2.append(" ");
                        stringBuilder2.append(a);
                        Slog.i(str2, stringBuilder2.toString());
                    }
                    reAddAlarmLocked(a, nowElapsed, true);
                }
            }
            if (batches.size() != 0) {
                this.mCancelRemoveAction = true;
                i = batches.size();
                for (int batchNum = 0; batchNum < i; batchNum++) {
                    Batch batch = (Batch) batches.get(batchNum);
                    int N = batch.size();
                    for (int i3 = 0; i3 < N; i3++) {
                        reAddAlarmLocked(batch.get(i3), nowElapsed, true);
                    }
                }
                this.mCancelRemoveAction = false;
                if (!(oldPendingIdleUntil == null || oldPendingIdleUntil == this.mPendingIdleUntil)) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("pkg Rebatching: idle until changed from ");
                    stringBuilder3.append(oldPendingIdleUntil);
                    stringBuilder3.append(" to ");
                    stringBuilder3.append(this.mPendingIdleUntil);
                    Slog.wtf(str3, stringBuilder3.toString());
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

    public Object getHwAlarmHandler() {
        return new HwAlarmHandler();
    }

    public void postTrimAlarm(HashSet<String> pkgList) {
        if (pkgList != null && pkgList.size() != 0) {
        }
    }

    private void removeByPkg_hwHsm(HashSet<String> sPkgs) {
        if (sPkgs != null) {
            try {
                if (sPkgs.size() != 0) {
                }
            } catch (Exception e) {
                Slog.e(TAG, "AlarmManagerService removeByPkg_hwHsm", e);
            }
        }
    }

    private void decideRtcPrioritySet(Alarm decidingAlarm) {
        if (decidingAlarm != null && decidingAlarm.operation != null) {
            String mayInvolvedPackageName;
            String targetPackageName = decidingAlarm.operation.getTargetPackage();
            if (targetPackageName.equals("android")) {
                targetPackageName = DESKCLOCK_PACKAGENAME;
            }
            Alarm mayInvolvedAlarm = null;
            Alarm decideResultAlarm = null;
            if (DESKCLOCK_PACKAGENAME.equals(targetPackageName)) {
                mayInvolvedPackageName = SETTINGS_PACKAGENAME;
            } else if (SETTINGS_PACKAGENAME.equals(targetPackageName)) {
                mayInvolvedPackageName = DESKCLOCK_PACKAGENAME;
            } else {
                Slog.w(TAG, "decideRtcPrioritySet--packagename error, decideRtcPrioritySet 3, return directly");
                return;
            }
            if (mHwWakeupBoot.containsKey(targetPackageName)) {
                mHwWakeupBoot.remove(targetPackageName);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("decideRtcPrioritySet--put ");
            stringBuilder.append(targetPackageName);
            stringBuilder.append(",to map");
            Slog.d(str, stringBuilder.toString());
            mHwWakeupBoot.put(targetPackageName, decidingAlarm);
            if (mHwWakeupBoot.containsKey(mayInvolvedPackageName)) {
                mayInvolvedAlarm = (Alarm) mHwWakeupBoot.get(mayInvolvedPackageName);
            }
            if (mayInvolvedAlarm == null) {
                decideResultAlarm = decidingAlarm;
            } else if (decidingAlarm.when > mayInvolvedAlarm.when) {
                decideResultAlarm = mayInvolvedAlarm;
            } else if (decidingAlarm.when < mayInvolvedAlarm.when) {
                decideResultAlarm = decidingAlarm;
            } else if (DESKCLOCK_PACKAGENAME.equals(decidingAlarm.operation.getTargetPackage())) {
                decideResultAlarm = decidingAlarm;
            } else if (DESKCLOCK_PACKAGENAME.equals(mayInvolvedAlarm.operation.getTargetPackage())) {
                decideResultAlarm = mayInvolvedAlarm;
            }
            if (decideResultAlarm != null) {
                Slog.d(TAG, "have calculate RTC result and will set to lock");
                hwSetRtcLocked(decideResultAlarm);
            }
        }
    }

    private boolean decideRtcPriorityEnable(Alarm a) {
        if (a == null || a.operation == null) {
            return false;
        }
        boolean ret = false;
        boolean deskClockEnable = false;
        boolean settingProviderEnable = false;
        if (a.type == 0) {
            if (IS_POWEROFF_ALARM_ENABLED && a.operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME) && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, true)) {
                deskClockEnable = true;
            }
            if (a.operation.getTargetPackage().equals(SETTINGS_PACKAGENAME) && resetStrExtraCallingIdentity(a.operation, SAVE_TO_REGISTER, false)) {
                settingProviderEnable = true;
            }
        } else {
            ret = false;
        }
        if (settingProviderEnable || deskClockEnable) {
            ret = true;
        }
        return ret;
    }

    private void decideRtcPriorityRemove(PendingIntent operation) {
        if (operation != null) {
            String mayInvolvedPackageName;
            String targetPackageName = operation.getTargetPackage();
            if (targetPackageName.equals("android")) {
                targetPackageName = DESKCLOCK_PACKAGENAME;
            }
            if (DESKCLOCK_PACKAGENAME.equals(targetPackageName)) {
                mayInvolvedPackageName = SETTINGS_PACKAGENAME;
            } else if (SETTINGS_PACKAGENAME.equals(targetPackageName)) {
                mayInvolvedPackageName = DESKCLOCK_PACKAGENAME;
            } else {
                Slog.w(TAG, "packagename error, decideRtcPriorityRemove, return directly");
                return;
            }
            if (mHwWakeupBoot.isEmpty()) {
                Slog.w(TAG, "error, mHwWakeupBoot is empty, return directly");
                hwRemoveRtcLocked();
                return;
            }
            if (mHwWakeupBoot.containsKey(targetPackageName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("decideRtcPriorityRemove--remove ");
                stringBuilder.append(targetPackageName);
                Slog.w(str, stringBuilder.toString());
                mHwWakeupBoot.remove(targetPackageName);
            }
            if (mHwWakeupBoot.containsKey(mayInvolvedPackageName)) {
                Alarm decideResultAlarm = (Alarm) mHwWakeupBoot.get(mayInvolvedPackageName);
                if (decideResultAlarm != null) {
                    Slog.d(TAG, "decideRtcPriorityRemove ,hwSetRtcLocked");
                    hwSetRtcLocked(decideResultAlarm);
                }
            } else {
                mHwWakeupBoot.clear();
                Slog.d(TAG, "mHwWakeupBoot have clear");
                hwRemoveRtcLocked();
            }
        }
    }

    private boolean isDeskClock(Alarm a) {
        if (a == null || a.operation == null || !IS_POWEROFF_ALARM_ENABLED) {
            return false;
        }
        if (a.type == 0 && IS_POWEROFF_ALARM_ENABLED && a.operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME) && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, false)) {
            return true;
        }
        if (a.type == 0 && IS_POWEROFF_ALARM_ENABLED && a.operation.getTargetPackage().equals("android") && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, false)) {
            return true;
        }
        return false;
    }

    private void saveDeskClock(Alarm a) {
        if (a != null && a.operation != null && a.operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME) && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, false)) {
            this.hasDeskClocksetAlarm = true;
            synchronized (this.mLock) {
                if (this.mPendingAlarm != null) {
                    Slog.d(TAG, "set deskclock after boot, remove Shutdown alarm from framework");
                    removeLocked(this.mPendingAlarm, null);
                    this.mPendingAlarm = null;
                }
            }
            long alarmWhen = a.when;
            int alarmId = resetIntExtraCallingIdentity(a.operation, ALARM_ID, -1);
            if (alarmId != -1) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set shutdownAlarm ");
                stringBuilder.append(alarmId);
                stringBuilder.append(" ");
                stringBuilder.append(alarmWhen);
                Slog.d(str, stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(alarmId);
                stringBuilder.append(" ");
                stringBuilder.append(alarmWhen);
                SystemProperties.set("persist.sys.shut_alarm", stringBuilder.toString());
            }
        }
    }

    private void removeDeskClock(Alarm a) {
        if (a != null && a.operation != null && IS_POWEROFF_ALARM_ENABLED) {
            if (a.operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME) && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, false)) {
                Slog.d(TAG, "remove shutdownAlarm : none");
                SystemProperties.set("persist.sys.shut_alarm", "none");
            }
            if (a.operation.getTargetPackage().equals("android") && resetStrExtraCallingIdentity(a.operation, IS_OWNER_ALARM, false)) {
                Slog.d(TAG, "remove shutdownAlarm : none");
                SystemProperties.set("persist.sys.shut_alarm", "none");
            }
        }
    }

    protected void removeDeskClockFromFWK(PendingIntent operation) {
        if (operation != null && resetStrExtraCallingIdentity(operation, REMOVE_POWEROFF_ALARM_ANYWAY, false) && ActivityManager.getCurrentUser() == 0) {
            synchronized (this.mLock) {
                Slog.d(TAG, "no alarm enable, so remove Shutdown Alarm from framework");
                if (this.mPendingAlarm != null) {
                    removeLocked(this.mPendingAlarm, null);
                    this.mPendingAlarm = null;
                }
            }
            Slog.d(TAG, "remove shutdownAlarm : none");
            SystemProperties.set("persist.sys.shut_alarm", "none");
        }
    }

    private void hwSetRtcLocked(Alarm setAlarm) {
        if (setAlarm != null && setAlarm.operation != null) {
            long alarmWhen = setAlarm.when;
            String targetPackageName = setAlarm.operation.getTargetPackage();
            if (System.currentTimeMillis() > alarmWhen) {
                Slog.d(TAG, "hwSetRtcLocked--missed alarm, not set RTC to driver");
                return;
            }
            long alarmSeconds;
            long alarmNanoseconds;
            if (alarmWhen < 0) {
                alarmSeconds = 0;
                alarmNanoseconds = 0;
            } else {
                alarmNanoseconds = 1000 * ((alarmWhen % 1000) * 1000);
                alarmSeconds = alarmWhen / 1000;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set RTC alarm Locked, time = ");
            stringBuilder.append(getTimeString(alarmWhen));
            Slog.d(str, stringBuilder.toString());
            timeInRegister = getTimeString(alarmWhen);
            mPropHwRegisterName = targetPackageName;
            hwSetClockRTC(this.mNativeData, alarmSeconds, alarmNanoseconds);
        }
    }

    private void hwRemoveRtcLocked() {
        Slog.d(TAG, "remove RTC alarm time Locked");
        timeInRegister = HwAPPQoEUtils.INVALID_STRING_VALUE;
        hwSetClockRTC(this.mNativeData, 0, 0);
    }

    private String getTimeString(long milliSec) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(milliSec)).toString();
    }

    protected void printHwWakeupBoot(PrintWriter pw) {
        if (!(this.mHwAlarmLock || mHwWakeupBoot == null || mHwWakeupBoot.size() <= 0)) {
            pw.println();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pw.print("HW WakeupBoot MAP: ");
            pw.println();
            pw.print("Register time:");
            pw.print(timeInRegister);
            pw.println();
            if (mHwWakeupBoot.size() > 0) {
                for (Entry entry : mHwWakeupBoot.entrySet()) {
                    String key = (String) entry.getKey();
                    Alarm PrintAlarms = (Alarm) entry.getValue();
                    pw.print("packageName=");
                    pw.print(key);
                    pw.print("  time=");
                    pw.print(sdf.format(new Date(PrintAlarms.when)));
                    pw.println();
                }
            }
        }
    }

    private boolean isMDMAlarm(Alarm alarm) {
        if (alarm == null || alarm.operation == null) {
            return false;
        }
        if ("android".equals(alarm.operation.getTargetPackage()) && resetStrExtraCallingIdentity(alarm.operation, KEY_BOOT_MDM, false)) {
            return true;
        }
        return false;
    }

    protected void hwRemoveRtcAlarm(Alarm alarm, boolean cancel) {
        if (alarm != null) {
            if (isMDMAlarm(alarm)) {
                this.isSetHwMDMAlarm = false;
                this.mHwMDMAlarm = null;
                Slog.i(TAG, "hwRemoveRtcAlarm MDM");
            }
            reportAlarmForAware(alarm, cancel ? 1 : 2);
            if (isDeskClock(alarm)) {
                removeDeskClock(alarm);
            }
            if (!this.mHwAlarmLock) {
                if (decideRtcPriorityEnable(alarm) || isDeskClock(alarm)) {
                    decideRtcPriorityRemove(alarm.operation);
                }
            }
        }
    }

    protected void hwSetRtcAlarm(Alarm alarm) {
        if (alarm != null) {
            if (alarm.listenerTag != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hwSetAlarm listenerTag: ");
                stringBuilder.append(alarm.listenerTag);
                Slog.i(str, stringBuilder.toString());
            }
            if (isMDMAlarm(alarm)) {
                this.isSetHwMDMAlarm = true;
                this.mHwMDMAlarm = alarm;
                Slog.i(TAG, "hwSetRtcAlarm MDM");
            }
            reportAlarmForAware(alarm, 0);
            if (isDeskClock(alarm)) {
                saveDeskClock(alarm);
            }
            if (!this.mHwAlarmLock) {
                if (decideRtcPriorityEnable(alarm) || isDeskClock(alarm)) {
                    decideRtcPrioritySet(alarm);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0040, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void hwRemoveAnywayRtcAlarm(PendingIntent operation) {
        if (operation != null && !this.mHwAlarmLock && IS_POWEROFF_ALARM_ENABLED && operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME) && resetStrExtraCallingIdentity(operation, REMOVE_POWEROFF_ALARM_ANYWAY, false) && ActivityManager.getCurrentUser() == 0) {
            Slog.d(TAG, "DeskClock not receive boot broadcast,remove alarm anyway");
            decideRtcPriorityRemove(operation);
            Slog.d(TAG, "remove shutdownAlarm : none");
            SystemProperties.set("persist.sys.shut_alarm", "none");
        }
    }

    protected void hwAddFirstFlagForRtcAlarm(Alarm alarm, Intent backgroundIntent) {
        if (alarm != null && alarm.operation != null) {
            if (decideRtcPriorityEnable(alarm) && alarm.operation.getTargetPackage().equals(DESKCLOCK_PACKAGENAME)) {
                if (true == this.mIsFirstPowerOffAlarm && "RTC".equals(SystemProperties.get("persist.sys.powerup_reason", SettingsMDMPlugin.CONFIG_NORMAL_VALUE))) {
                    backgroundIntent.putExtra("FLAG_IS_FIRST_POWER_OFF_ALARM", true);
                    this.mIsFirstPowerOffAlarm = false;
                } else {
                    backgroundIntent.putExtra("FLAG_IS_FIRST_POWER_OFF_ALARM", false);
                }
            } else if (isDeskClock(alarm)) {
                if (true == this.mIsFirstPowerOffAlarm && "RTC".equals(SystemProperties.get("persist.sys.powerup_reason", SettingsMDMPlugin.CONFIG_NORMAL_VALUE))) {
                    Slog.d(TAG, "FLAG_IS_FIRST_POWER_OFF_ALARM : true");
                    backgroundIntent.putExtra("FLAG_IS_FIRST_POWER_OFF_ALARM", true);
                    this.mIsFirstPowerOffAlarm = false;
                } else {
                    Slog.d(TAG, "FLAG_IS_FIRST_POWER_OFF_ALARM : false");
                    backgroundIntent.putExtra("FLAG_IS_FIRST_POWER_OFF_ALARM", false);
                }
            }
        }
    }

    protected long checkHasHwRTCAlarmLock(String packageName) {
        Alarm rtcAlarm = null;
        long time = -1;
        synchronized (this.mLock) {
            if (packageName != null) {
                try {
                    if (mHwWakeupBoot.containsKey(packageName)) {
                        rtcAlarm = (Alarm) mHwWakeupBoot.get(packageName);
                    }
                } finally {
                }
            }
            if (rtcAlarm != null && (decideRtcPriorityEnable(rtcAlarm) || isDeskClock(rtcAlarm))) {
                time = rtcAlarm.when;
            }
        }
        return time;
    }

    /* JADX WARNING: Missing block: B:27:0x009d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void adjustHwRTCAlarmLock(boolean deskClockTime, boolean bootOnTime, int typeState) {
        Slog.d(TAG, "adjust RTC Alarm");
        synchronized (this.mLock) {
            this.mHwAlarmLock = true;
            switch (typeState) {
                case 1:
                    mHwWakeupBoot.clear();
                    Slog.d(TAG, "NONE_LISTVIEW--user cancel bootOnTime and deskClockTime RTC alarm  ");
                    hwRemoveRtcLocked();
                    break;
                case 2:
                case 3:
                case 6:
                case 7:
                    if (!deskClockTime || !bootOnTime) {
                        if (deskClockTime || bootOnTime) {
                            Alarm optAlarm;
                            if (!deskClockTime || bootOnTime) {
                                if (!deskClockTime && bootOnTime) {
                                    optAlarm = (Alarm) mHwWakeupBoot.get(DESKCLOCK_PACKAGENAME);
                                    Slog.d(TAG, "user cancel deskClockTime RTC alarm");
                                    hwRemoveRtcAlarmWhenShut(optAlarm);
                                    break;
                                }
                            }
                            optAlarm = (Alarm) mHwWakeupBoot.get(SETTINGS_PACKAGENAME);
                            Slog.d(TAG, "user cancel bootOnTime RTC alarm");
                            hwRemoveRtcAlarmWhenShut(optAlarm);
                            break;
                        }
                        mHwWakeupBoot.clear();
                        Slog.d(TAG, "user cancel bootOnTime and deskClockTime RTC alarm  ");
                        hwRemoveRtcLocked();
                        break;
                    }
                    setHwAirPlaneStatePropLock();
                    return;
                    break;
                case 4:
                    if (!deskClockTime) {
                        mHwWakeupBoot.clear();
                        Slog.d(TAG, "ONE_DESKCLOCK_LISTVIEW ---user cancel deskClockTime RTC alarm  ");
                        hwRemoveRtcLocked();
                        break;
                    }
                    break;
                case 5:
                    if (!bootOnTime) {
                        mHwWakeupBoot.clear();
                        Slog.d(TAG, "ONE_BOOT_LISTVIEW ---user cancel bootOnTime RTC alarm  ");
                        hwRemoveRtcLocked();
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void hwRemoveRtcAlarmWhenShut(Alarm alarm) {
        if (alarm != null) {
            if (decideRtcPriorityEnable(alarm) || isDeskClock(alarm)) {
                Slog.d(TAG, "remove RTC alarm in shutdown view");
                decideRtcPriorityRemove(alarm.operation);
                setHwAirPlaneStatePropLock();
            }
        }
    }

    protected void setHwAirPlaneStatePropLock() {
        SystemProperties.set(HWAIRPLANESTATE_PROPERTY, mPropHwRegisterName);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hw airplane prop locked = ");
        stringBuilder.append(mPropHwRegisterName);
        Slog.d(str, stringBuilder.toString());
    }

    protected void setHwRTCAlarmLock() {
        if (this.isSetHwMDMAlarm) {
            hwSetRtcLocked(this.mHwMDMAlarm);
            return;
        }
        long deskTime = checkHasHwRTCAlarmLock(DESKCLOCK_PACKAGENAME);
        long settingsTime = checkHasHwRTCAlarmLock(SETTINGS_PACKAGENAME);
        if (deskTime == -1 && settingsTime == -1) {
            Slog.d(TAG, "setHwRTCAlarmLock-- not set RTC to driver");
            return;
        }
        Alarm rtcAlarm;
        if (deskTime == -1) {
            rtcAlarm = (Alarm) mHwWakeupBoot.get(SETTINGS_PACKAGENAME);
        } else if (settingsTime == -1) {
            rtcAlarm = (Alarm) mHwWakeupBoot.get(DESKCLOCK_PACKAGENAME);
        } else {
            Alarm optAlarm1 = (Alarm) mHwWakeupBoot.get(DESKCLOCK_PACKAGENAME);
            Alarm optAlarm2 = (Alarm) mHwWakeupBoot.get(SETTINGS_PACKAGENAME);
            if (optAlarm1.when <= optAlarm2.when) {
                rtcAlarm = optAlarm1;
            } else {
                rtcAlarm = optAlarm2;
            }
        }
        hwSetRtcLocked(rtcAlarm);
    }

    protected void hwRecordFirstTime() {
        this.mFirstRTC = System.currentTimeMillis();
        this.mFirstELAPSED = SystemClock.elapsedRealtime();
        if (0 == Global.getLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, 0)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hwRecordFirstTime init for TimeKeeper at ");
            stringBuilder.append(this.mFirstRTC);
            Flog.i(500, stringBuilder.toString());
            Global.putLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, this.mFirstRTC);
        }
    }

    protected void hwRecordTimeChangeRTC(long nowRTC, long nowELAPSED, long lastTimeChangeClockTime, long expectedClockTime) {
        long expectedRTC;
        long maxOffset;
        long j = nowRTC;
        if (lastTimeChangeClockTime == 0) {
            expectedRTC = this.mFirstRTC + (nowELAPSED - this.mFirstELAPSED);
            maxOffset = HwArbitrationDEFS.DelayTimeMillisA;
        } else {
            expectedRTC = expectedClockTime;
            maxOffset = 5000;
        }
        long offset = j - expectedRTC;
        if (offset < (-maxOffset) || j > maxOffset) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hwRecordTimeChangeRTC for TimeKeeper at ");
            stringBuilder.append(j);
            stringBuilder.append(", offset=");
            stringBuilder.append(offset);
            Flog.i(500, stringBuilder.toString());
            Global.putLong(getContext().getContentResolver(), LAST_TIME_CHANGED_RTC, j);
        }
    }

    private boolean resetStrExtraCallingIdentity(PendingIntent operation, String extra, boolean value) {
        long identity = Binder.clearCallingIdentity();
        boolean extraRes = false;
        try {
            extraRes = operation.getIntent().getBooleanExtra(extra, value);
        } catch (Throwable th) {
        }
        Binder.restoreCallingIdentity(identity);
        return extraRes;
    }

    private int resetIntExtraCallingIdentity(PendingIntent operation, String extra, int value) {
        long identity = Binder.clearCallingIdentity();
        int extraRes = -1;
        try {
            extraRes = operation.getIntent().getIntExtra(extra, value);
        } catch (Throwable th) {
        }
        Binder.restoreCallingIdentity(identity);
        return extraRes;
    }

    protected boolean isContainsAppUidInWorksource(WorkSource workSource, String packageName) {
        if (workSource == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            if (containsUidInWorksource(workSource, getContext().getPackageManager().getApplicationInfo(packageName, 1).uid)) {
                Slog.i(TAG, "isContainsAppUidInWorksource-->worksource contains app's.uid");
                return true;
            }
        } catch (NameNotFoundException e) {
            Slog.w(TAG, "isContainsAppUidInWorksource-->happend NameNotFoundException");
        } catch (Exception e2) {
            Slog.w(TAG, "isContainsAppUidInWorksource-->happend Exception");
        }
        return false;
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
                public void run() {
                    synchronized (HwAlarmManagerService.this.mLock) {
                        HwAlarmManagerService.this.removeByTagLocked(pkg, tags, targetUid);
                    }
                }
            });
        }
    }

    private boolean removeSingleTagLocked(String packageName, String tag, int targetUid) {
        int i;
        boolean didRemove = false;
        for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            didRemove |= removeFromBatch(b, packageName, tag, targetUid);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
            Alarm a = (Alarm) this.mPendingWhileIdleAlarms.get(i);
            if (a.matches(packageName) && targetUid == a.creatorUid) {
                this.mPendingWhileIdleAlarms.remove(i);
            }
        }
        return didRemove;
    }

    private void removeByTagLocked(String packageName, List<String> tags, int targetUid) {
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
        String[] splits = alarmTag.split(":");
        if (splits.length > 1 && splits[1].equals(tag)) {
            return true;
        }
        return false;
    }

    boolean removeFromBatch(Batch batch, String packageName, String tag, int targetUid) {
        Batch batch2 = batch;
        String str = packageName;
        String str2 = tag;
        int i = targetUid;
        if (str == null) {
            return false;
        }
        boolean didRemove = false;
        long newStart = 0;
        long newEnd = Long.MAX_VALUE;
        int newFlags = 0;
        for (int i2 = batch2.alarms.size() - 1; i2 >= 0; i2--) {
            Alarm alarm = (Alarm) batch2.alarms.get(i2);
            if (!alarm.matches(str) || i != alarm.creatorUid) {
                if (alarm.whenElapsed > newStart) {
                    newStart = alarm.whenElapsed;
                }
                if (alarm.maxWhenElapsed < newEnd) {
                    newEnd = alarm.maxWhenElapsed;
                }
                newFlags |= alarm.flags;
            } else if (canRemove(alarm, str2)) {
                batch2.alarms.remove(i2);
                didRemove = true;
                if (str2 != null) {
                    String str3 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("remove package alarm ");
                    stringBuilder.append(str);
                    stringBuilder.append("' (");
                    stringBuilder.append(str2);
                    stringBuilder.append(") ,targetUid: ");
                    stringBuilder.append(i);
                    Slog.d(str3, stringBuilder.toString());
                }
                hwRemoveRtcAlarm(alarm, true);
                if (alarm.alarmClock != null) {
                    this.mNextAlarmClockMayChange = true;
                }
            }
        }
        if (didRemove) {
            batch2.start = newStart;
            batch2.end = newEnd;
            batch2.flags = newFlags;
        }
        return didRemove;
    }

    private void reportAlarmForAware(Alarm alarm, int operation) {
        if (alarm != null && alarm.packageName != null && alarm.statsTag != null) {
            HwSysResManager resManager = HwSysResManager.getInstance();
            if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
                Bundle bundleArgs = new Bundle();
                bundleArgs.putString(MemoryConstant.MEM_PREREAD_ITEM_NAME, alarm.packageName);
                bundleArgs.putString("statstag", alarm.statsTag);
                bundleArgs.putInt("relationType", 22);
                bundleArgs.putInt("alarm_operation", operation);
                bundleArgs.putInt("tgtUid", alarm.creatorUid);
                CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), bundleArgs);
                long id = Binder.clearCallingIdentity();
                resManager.reportData(data);
                Binder.restoreCallingIdentity(id);
            }
        }
    }

    protected void modifyAlarmIfOverload(Alarm alarm) {
        AwareWakeUpManager.getInstance().modifyAlarmIfOverload(alarm);
    }

    protected void reportWakeupAlarms(ArrayList<Alarm> alarms) {
        AwareWakeUpManager.getInstance().reportWakeupAlarms(alarms);
    }

    protected boolean isAwareAlarmManagerEnabled() {
        return AlarmManagerFeature.isEnable();
    }
}

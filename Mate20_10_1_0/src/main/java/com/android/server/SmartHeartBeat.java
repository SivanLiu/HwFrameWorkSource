package com.android.server;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.AlarmManagerService;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SmartHeartBeat extends SmartHeartBeatDummy {
    static final int ALARM_ADJUST_BACKWARD = 2;
    static final int ALARM_ADJUST_FORWARD = 0;
    static final int ALARM_ADJUST_MSG = 102;
    static final int ALARM_ADJUST_NEAR = 1;
    static final int ALARM_ADJUST_NONE = -1;
    static final int ALARM_PENDING_MSG = 101;
    static final int ALL_ALARM_TYPE_BY_PKG = 0;
    static final int ALL_ALARM_TYPE_BY_PROC = 10;
    static final boolean DEBUG_HEART_BEAT = SystemProperties.getBoolean("persist.sys.shb.debug", false);
    static final boolean SHB_MODULE_SWITCHER;
    static final String TAG = "SmartHeartBeat";
    /* access modifiers changed from: private */
    public static HwAlarmManagerService mAlarmService;
    private static Context mContext;
    private static SmartHeartBeatDummy mInstance = null;
    private static final Object mLock = new Object();
    private HeartBeatDatabase mDataMap;
    private Handler mHandler;
    private HandlerThread mThread = new HandlerThread("HeartBeatHandlerThread");

    static {
        boolean z = true;
        if (!SystemProperties.getBoolean("persist.sys.shb.switcher", true) || SystemProperties.getBoolean("ro.config.pg_disable_pg", false)) {
            z = false;
        }
        SHB_MODULE_SWITCHER = z;
    }

    public static synchronized SmartHeartBeatDummy getInstance(Context context, HwAlarmManagerService service) {
        SmartHeartBeatDummy smartHeartBeatDummy;
        synchronized (SmartHeartBeat.class) {
            if (mInstance == null) {
                if (SHB_MODULE_SWITCHER) {
                    mContext = context;
                    mAlarmService = service;
                    Slog.i(TAG, "getInstance new");
                    mInstance = new SmartHeartBeat();
                } else {
                    Slog.i(TAG, "SmartHeartBeat is turn off !");
                    mInstance = new SmartHeartBeatDummy();
                }
            }
            if (DEBUG_HEART_BEAT) {
                Slog.i(TAG, "getInstance return");
            }
            smartHeartBeatDummy = mInstance;
        }
        return smartHeartBeatDummy;
    }

    private SmartHeartBeat() {
        this.mThread.start();
        this.mHandler = new HeartBeatHandler(this.mThread.getLooper());
        this.mDataMap = new HeartBeatDatabase();
        Slog.i(TAG, "SmartHeartBeat getInstance init success!");
    }

    private boolean isPending(AlarmManagerService.Alarm alarm) {
        boolean pending = false;
        if (alarm == null) {
            Slog.d(TAG, "isPending, null == alarm");
            return false;
        }
        if (this.mDataMap.isPendingAlarm(alarm)) {
            pending = true;
        }
        if (DEBUG_HEART_BEAT) {
            Slog.d(TAG, "isPending, ret:" + pending + ", alarm:" + alarm);
        }
        return pending;
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public boolean shouldPendingAlarm(AlarmManagerService.Alarm alarm) {
        boolean isPending;
        synchronized (mLock) {
            isPending = isPending(alarm);
        }
        return isPending;
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void setAlarmsPending(List<String> pkgList, List<String> actionList, boolean pending, int pendingType) {
        if (10 == pendingType) {
            setAlarmsPendingByPidsInner(pkgList, actionList, pending, pendingType);
        } else if (pendingType == 0) {
            setAlarmsPendingInner(pkgList, actionList, pending, pendingType);
        } else {
            Slog.w(TAG, "unknown pending " + pkgList + " by " + pendingType);
        }
    }

    private void setAlarmsPendingInner(List<String> pkgList, List<String> actionList, boolean pending, int pendingType) {
        if (pkgList == null || pkgList.size() <= 0) {
            Slog.d(TAG, "setAlarmsPending, pkgList=" + pkgList);
            return;
        }
        if (DEBUG_HEART_BEAT) {
            Slog.i(TAG, "setAlarmsPending, pkgList=" + pkgList + ", action=" + actionList + ", pending=" + pending + ", pendingType=" + pendingType);
        } else {
            Slog.d(TAG, "setAlarmsPending ...");
        }
        synchronized (mLock) {
            for (String pkgName : pkgList) {
                if (pending) {
                    if (actionList != null) {
                        this.mDataMap.addPendingPackageForActions(pkgName, actionList);
                    } else {
                        this.mDataMap.addPendingPackage(pkgName);
                    }
                } else if (actionList != null) {
                    this.mDataMap.removePendingPackageForActions(pkgName, actionList);
                } else {
                    this.mDataMap.removePendingPackage(pkgName);
                }
            }
        }
        MessageInfo args = new MessageInfo();
        args.state = pending;
        args.byPkgs = true;
        args.pkgList = pkgList;
        Message msg = this.mHandler.obtainMessage();
        msg.what = 101;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
    }

    private void setAlarmsPendingByPidsInner(List<String> pkgList, List<String> procList, boolean pending, int pendingType) {
        if (pkgList == null || pkgList.size() <= 0 || procList == null || procList.size() <= 0 || procList.size() != pkgList.size()) {
            Slog.w(TAG, "pending alarm , invaild para");
            return;
        }
        if (DEBUG_HEART_BEAT) {
            Slog.i(TAG, "pending alarm, pkgs=" + pkgList + ", procs=" + procList + ", pending=" + pending + ", pendingType=" + pendingType);
        } else {
            Slog.d(TAG, "pending alarm by p ...");
        }
        synchronized (mLock) {
            int size = pkgList.size();
            for (int i = 0; i < size; i++) {
                String pkg = pkgList.get(i);
                String proc = procList.get(i);
                if (pending) {
                    this.mDataMap.addPendingProc(pkg, proc);
                } else {
                    this.mDataMap.removePendingProc(pkg, proc);
                }
            }
        }
        MessageInfo args = new MessageInfo();
        args.state = pending;
        args.byPkgs = false;
        args.pkgList = pkgList;
        args.procList = procList;
        Message msg = this.mHandler.obtainMessage();
        msg.what = 101;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void removeAllPendingAlarms() {
        synchronized (mLock) {
            this.mDataMap.removeAllPendingPackages();
        }
        MessageInfo args = new MessageInfo();
        args.state = true;
        args.pkgList = null;
        Message msg = this.mHandler.obtainMessage();
        msg.what = 101;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
        Slog.i(TAG, "remove all pending alarms");
    }

    private boolean isAdjustAlarm(AlarmManagerService.Alarm a) {
        String pkg = a.packageName;
        String action = getActionByClearCallingIdentity(a.operation);
        int type = a.type;
        if (this.mDataMap.adjustPackageForActions.containsAdjustKey(pkg, action)) {
            if (!DEBUG_HEART_BEAT) {
                return true;
            }
            Slog.d(TAG, "isAdjustAlarm: true,pkg: " + pkg + ",action: " + action);
            return true;
        } else if (type != 0 || a.alarmClock == null) {
            boolean isAdjustAlarm = this.mDataMap.adjustPkgsMap.containsAdjustKey(pkg);
            if (!isAdjustAlarm && a.procName != null) {
                isAdjustAlarm = this.mDataMap.isAdjustAlarmByProc(pkg, a.procName);
            }
            if (DEBUG_HEART_BEAT) {
                Slog.d(TAG, "isAdjustAlarm: " + isAdjustAlarm + ",pkg:" + pkg + ",proc:" + a.procName);
            }
            return isAdjustAlarm;
        } else if (!DEBUG_HEART_BEAT) {
            return false;
        } else {
            Slog.d(TAG, "isAdjustAlarm: false,pkg: " + pkg + ",action: " + action + ",not adjustalarm with AlarmClockInfo");
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x00a8 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x00a9  */
    private long calAlarmWhenElapsed(AlarmManagerService.Alarm a) {
        long interval;
        long interval2;
        long interval3 = 0;
        int mode = -1;
        long adjustWhenElapsed = a.whenElapsed;
        String pkg = a.packageName;
        String action = getActionByClearCallingIdentity(a.operation);
        int type = a.type;
        long whenElapsed = a.whenElapsed;
        AdjustValue adjustValue = this.mDataMap.adjustPackageForActions.getAdjustValue(pkg, action);
        if (adjustValue != null) {
            interval3 = adjustValue.interval;
            mode = adjustValue.mode;
        }
        if (interval3 == 0) {
            interval3 = this.mDataMap.adjustPkgsMap.getAdjustPkgInterval(pkg);
            mode = this.mDataMap.adjustPkgsMap.getAdjustPkgMode(pkg);
        }
        if (interval3 != 0 || a.procName == null) {
            interval2 = interval3;
        } else {
            interval2 = interval3;
            if (this.mDataMap.isAdjustAlarmByProc(pkg, a.procName)) {
                interval = this.mDataMap.getAdjustProc(pkg).getAdjustProcInterval(a.procName);
                mode = this.mDataMap.getAdjustProc(pkg).getAdjustProcMode(a.procName);
                if (DEBUG_HEART_BEAT) {
                    Slog.d(TAG, "calAlarmWhenElapsed, pkg:" + pkg + ",proc:" + a.procName + ",interval:" + interval + ",mode:" + mode);
                }
                if (interval != 0) {
                    return adjustWhenElapsed;
                }
                if ((type == 0 || 1 == type) && whenElapsed < 0) {
                    Slog.d(TAG, "Maybe the whenElapsed time is incorrect: pkg=" + pkg + " whenElapsed = " + whenElapsed);
                    whenElapsed = SystemClock.elapsedRealtime();
                }
                if (mode == 0 || mode == 1 || mode == 2) {
                    adjustWhenElapsed = ((((interval / 2) * ((long) mode)) + whenElapsed) / interval) * interval;
                    if (mode != 2) {
                        if (adjustWhenElapsed < SystemClock.elapsedRealtime()) {
                            adjustWhenElapsed += interval;
                        }
                    } else if (2 == mode && adjustWhenElapsed - whenElapsed == interval) {
                        adjustWhenElapsed = whenElapsed;
                    }
                }
                if (DEBUG_HEART_BEAT) {
                    Slog.d(TAG, "calAlarmWhenElapsed pkg: " + pkg + ",proc:" + a.procName + ", action: " + action + ", whenElapsed: " + whenElapsed + ", adjusted WhenElapsed: " + adjustWhenElapsed + ", interval: " + interval + ", mode: " + mode);
                }
                return adjustWhenElapsed;
            }
        }
        interval = interval2;
        if (interval != 0) {
        }
    }

    private long getAdjustAlarmWhenElapsed(AlarmManagerService.Alarm a) {
        long val = isAdjustAlarm(a) ? calAlarmWhenElapsed(a) : a.whenElapsed;
        if (DEBUG_HEART_BEAT) {
            Slog.d(TAG, "getAdjustAlarmWhenElapsed, val: " + val + ", pkg: " + a.packageName + ", proc: " + a.procName + ", whenElapsed: " + a.whenElapsed + ", type: " + a.type);
        }
        return val;
    }

    /* access modifiers changed from: private */
    public static String getActionByClearCallingIdentity(PendingIntent operation) {
        long identity = Binder.clearCallingIdentity();
        String action = null;
        if (operation != null) {
            action = operation.getIntent().getAction();
        }
        Binder.restoreCallingIdentity(identity);
        return action;
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void setAlarmsAdjust(List<String> pkgList, List<String> actionList, boolean adjust, int type, long interval, int mode) {
        if (10 == type) {
            setAlarmsAdjustByPidsInner(pkgList, actionList, adjust, type, interval, mode);
        } else if (type == 0) {
            setAlarmsAdjustInner(pkgList, actionList, adjust, type, interval, mode);
        } else {
            Slog.w(TAG, "unknown adjust " + pkgList + " by " + type);
        }
    }

    private void setAlarmsAdjustInner(List<String> pkgList, List<String> actionList, boolean adjust, int type, long interval, int mode) {
        Iterator<String> it;
        if (pkgList != null) {
            if (pkgList.size() > 0) {
                if (DEBUG_HEART_BEAT) {
                    Slog.i(TAG, "setAlarmsAdjust, pkgList=" + pkgList + ", action=" + actionList + ", adjust=" + adjust + ", type=" + type + ", interval=" + interval + ", mode=" + mode);
                } else {
                    Slog.d(TAG, "setAlarmsAdjust ...");
                }
                synchronized (mLock) {
                    Iterator<String> it2 = pkgList.iterator();
                    while (it2.hasNext()) {
                        String pkg = it2.next();
                        if (!adjust) {
                            it = it2;
                            if (actionList != null) {
                                this.mDataMap.adjustPackageForActions.removeAdjustPkg(pkg, actionList);
                                it2 = it;
                            } else {
                                this.mDataMap.adjustPkgsMap.removeAdjustPkg(pkg);
                            }
                        } else if (interval <= 0) {
                            Slog.d(TAG, "setAlarmsAdjust interval must be greater than 0.");
                            return;
                        } else if (actionList != null) {
                            this.mDataMap.adjustPackageForActions.putAdjustPkg(pkg, actionList, interval, mode);
                            it2 = it2;
                        } else {
                            it = it2;
                            this.mDataMap.adjustPkgsMap.putAdjustPkg(pkg, interval, mode);
                        }
                        it2 = it;
                    }
                    MessageInfo args = new MessageInfo();
                    args.byPkgs = true;
                    args.pkgList = pkgList;
                    Message msg = this.mHandler.obtainMessage();
                    msg.what = 102;
                    msg.obj = args;
                    this.mHandler.sendMessage(msg);
                    return;
                }
            }
        }
        Slog.i(TAG, "setAlarmsAdjust, pkgList: " + pkgList);
    }

    private void setAlarmsAdjustByPidsInner(List<String> pkgList, List<String> procList, boolean adjust, int type, long interval, int mode) {
        if (pkgList == null || pkgList.size() <= 0 || procList == null || procList.size() <= 0 || procList.size() != pkgList.size()) {
            Slog.w(TAG, "adjust alarm , invaild para");
            return;
        }
        if (DEBUG_HEART_BEAT) {
            Slog.i(TAG, "adjust alarm by p, pkgs=" + pkgList + ", procs=" + procList + ", adjust=" + adjust + ", type=" + type + ", interval=" + interval + ", mode=" + mode);
        } else {
            Slog.d(TAG, "adjust alarm by p ...");
        }
        synchronized (mLock) {
            int size = pkgList.size();
            for (int i = 0; i < size; i++) {
                String pkg = pkgList.get(i);
                String proc = procList.get(i);
                if (adjust) {
                    if (interval <= 0) {
                        Slog.d(TAG, "setAlarmsAdjust interval must be greater than 0.");
                        return;
                    }
                    this.mDataMap.getAdjustProc(pkg).putAdjustProc(proc, interval, mode);
                } else if (this.mDataMap.getAdjustProc(pkg).removeAdjustProc(proc) <= 0) {
                    this.mDataMap.removeAdjustProc(pkg);
                }
            }
            MessageInfo args = new MessageInfo();
            args.byPkgs = false;
            args.pkgList = pkgList;
            args.procList = procList;
            Message msg = this.mHandler.obtainMessage();
            msg.what = 102;
            msg.obj = args;
            this.mHandler.sendMessage(msg);
        }
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void removeAllAdjustAlarms() {
        Slog.i(TAG, "remove all adjust alarms.");
        synchronized (mLock) {
            this.mDataMap.adjustPkgsMap.clearAdjustPkg();
            this.mDataMap.adjustPackageForActions.clearAdjustPkg();
            this.mDataMap.adjustProcMap.clear();
        }
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void adjustAlarmIfNeeded(AlarmManagerService.Alarm a) {
        if (a == null) {
            Slog.e(TAG, "adjustAlarmIfNeeded, alarm is null!");
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (mLock) {
            long delay = 259200000;
            if (now < a.whenElapsed && a.whenElapsed - now > 259200000) {
                if (DEBUG_HEART_BEAT) {
                    Slog.i(TAG, "adjustAlarmIfNeeded, no need change alarm: " + a.packageName + ", whenElapsed: " + a.whenElapsed + ", now: " + now);
                }
            } else if (isPending(a)) {
                if (now >= a.whenElapsed) {
                    delay = 259200000 + (now - a.whenElapsed);
                }
                a.when += delay;
                a.whenElapsed += delay;
                a.maxWhenElapsed += delay;
                if (DEBUG_HEART_BEAT) {
                    Slog.i(TAG, "adjustAlarmIfNeeded, is pending alarm: " + a.packageName + ", tag: " + a.statsTag + ", whenElapsed: " + a.whenElapsed + ", now: " + now);
                }
            } else {
                long whenElapsed = getAdjustAlarmWhenElapsed(a);
                if (whenElapsed != a.whenElapsed) {
                    long delta = whenElapsed - a.whenElapsed;
                    a.whenElapsed += delta;
                    a.when += delta;
                    a.maxWhenElapsed = a.whenElapsed > a.maxWhenElapsed ? a.whenElapsed : a.maxWhenElapsed;
                    if (DEBUG_HEART_BEAT) {
                        Slog.i(TAG, "adjustAlarmLocked, is unify alarm: " + a.packageName + ", tag: " + a.statsTag + ", whenElapsed: " + a.whenElapsed + ", now: " + now);
                    }
                }
            }
        }
    }

    @Override // com.android.server.SmartHeartBeatDummy
    public void dump(PrintWriter pw) {
        synchronized (mLock) {
            this.mDataMap.dump(pw);
        }
    }

    private static class HeartBeatHandler extends Handler {
        HeartBeatHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 101) {
                pendingAlarmMessage(msg);
            } else if (i == 102) {
                adjustAlarmMessage(msg);
            }
        }

        private void adjustAlarmMessage(Message msg) {
            MessageInfo args = (MessageInfo) msg.obj;
            if (args == null) {
                Slog.d(SmartHeartBeat.TAG, "adjust alarm message, null == args");
                return;
            }
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "adjust alarm message, byPkg: " + args.byPkgs + " pkg: " + args.pkgList + " proc: " + args.procList);
            }
            synchronized (SmartHeartBeat.mAlarmService.mLock) {
                SmartHeartBeat.mAlarmService.rebatchPkgAlarmsLocked(args.byPkgs, args.pkgList, args.procList);
            }
        }

        private void pendingAlarmMessage(Message msg) {
            MessageInfo args = (MessageInfo) msg.obj;
            if (args == null) {
                Slog.d(SmartHeartBeat.TAG, "pendingAlarmMessage, null == args");
                return;
            }
            boolean pending = args.state;
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "pendingAlarmMessage, pending: " + pending + ", byPkgs: " + args.byPkgs + " pkg:" + args.pkgList);
            }
            synchronized (SmartHeartBeat.mAlarmService.mLock) {
                if (args.pkgList == null) {
                    SmartHeartBeat.mAlarmService.rebatchAllAlarmsLocked(true);
                } else {
                    SmartHeartBeat.mAlarmService.rebatchPkgAlarmsLocked(args.byPkgs, args.pkgList, args.procList);
                }
            }
        }
    }

    private static class MessageInfo {
        boolean byPkgs;
        List<String> pkgList;
        List<String> procList;
        boolean state;

        private MessageInfo() {
        }
    }

    private static class HeartBeatDatabase {
        final AdjustPackageForActions adjustPackageForActions;
        final AdjustPackage adjustPkgsMap;
        final HashMap<String, AdjustProc> adjustProcMap;
        /* access modifiers changed from: private */
        public Object lock;
        private final ArrayList<String> pendingMapByPkg;
        private final MultiMap<String, String> pendingMapByProc;
        private final MultiMap<String, String> pendingMapForActions;

        private HeartBeatDatabase() {
            this.pendingMapByPkg = new ArrayList<>();
            this.pendingMapByProc = new MultiMap<>();
            this.pendingMapForActions = new MultiMap<>();
            this.adjustPkgsMap = new AdjustPackage();
            this.adjustProcMap = new HashMap<>();
            this.adjustPackageForActions = new AdjustPackageForActions();
            this.lock = new Object();
        }

        /* access modifiers changed from: package-private */
        public void addPendingPackage(String pkgName) {
            if (!this.pendingMapByPkg.contains(pkgName)) {
                if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                    Slog.d(SmartHeartBeat.TAG, "addPendingPackage, pkgName: " + pkgName);
                }
                this.pendingMapByPkg.add(pkgName);
            }
        }

        /* access modifiers changed from: package-private */
        public void addPendingProc(String pkg, String proc) {
            this.pendingMapByProc.put(pkg, proc);
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "addPendingProc, pkg: " + pkg + " proc: " + proc);
            }
        }

        /* access modifiers changed from: package-private */
        public void addPendingPackageForActions(String pkgName, List<String> actionList) {
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "addPendingPackageForActions, pkgName: " + pkgName + ", actionList: " + actionList);
            }
            for (String action : actionList) {
                this.pendingMapForActions.put(pkgName, action);
            }
        }

        /* access modifiers changed from: package-private */
        public void removePendingPackage(String pkgName) {
            this.pendingMapByPkg.remove(pkgName);
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removePendingPackage, pkgName: " + pkgName);
            }
        }

        /* access modifiers changed from: package-private */
        public void removePendingProc(String pkg, String proc) {
            this.pendingMapByProc.remove(pkg, proc);
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removePendingProc, pkg: " + pkg + " proc: " + proc);
            }
        }

        /* access modifiers changed from: package-private */
        public void removePendingPackageForActions(String pkgName, List<String> actionList) {
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removePendingPackageForActions, pkgName: " + pkgName + ", actionList: " + actionList);
            }
            for (String action : actionList) {
                this.pendingMapForActions.remove(pkgName, action);
            }
        }

        /* access modifiers changed from: package-private */
        public void removeAllPendingPackages() {
            this.pendingMapByPkg.clear();
            this.pendingMapForActions.clear();
            this.pendingMapByProc.clear();
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removeAllPendingPackages");
            }
        }

        /* access modifiers changed from: package-private */
        public boolean isPendingAlarmByProc(String pkg, String proc) {
            List<String> procs = this.pendingMapByProc.getAll(pkg);
            return procs != null && procs.contains(proc);
        }

        /* access modifiers changed from: package-private */
        public boolean isAdjustAlarmByProc(String pkg, String proc) {
            AdjustProc adjustProc = this.adjustProcMap.get(pkg);
            if (adjustProc != null) {
                return adjustProc.containsAdjustKey(proc);
            }
            return false;
        }

        /* access modifiers changed from: package-private */
        public boolean isPendingAlarm(AlarmManagerService.Alarm alarm) {
            String pkgName = alarm.packageName;
            String action = SmartHeartBeat.getActionByClearCallingIdentity(alarm.operation);
            List<String> actionList = this.pendingMapForActions.getAll(pkgName);
            if (actionList.size() > 0 && action != null && actionList.contains(action)) {
                if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                    Slog.d(SmartHeartBeat.TAG, "isPendingAlarm, true, pkg:" + pkgName + ", action:" + action);
                }
                return true;
            } else if (alarm.type == 0 && alarm.alarmClock != null) {
                Slog.d(SmartHeartBeat.TAG, "isPendingAlarm, false, pkg:" + pkgName + ", action:" + action + ",not pending alarm with AlarmClockInfo");
                return false;
            } else if (this.pendingMapByPkg.contains(pkgName)) {
                if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                    Slog.d(SmartHeartBeat.TAG, "isPendingAlarm, true, pkg:" + pkgName);
                }
                return true;
            } else if (alarm.procName == null || !isPendingAlarmByProc(pkgName, alarm.procName)) {
                return false;
            } else {
                if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                    Slog.d(SmartHeartBeat.TAG, "isPendingAlarm, true, pkg:" + pkgName + " proc:" + alarm.procName);
                }
                return true;
            }
        }

        /* access modifiers changed from: package-private */
        public AdjustProc getAdjustProc(String pkgName) {
            AdjustProc ap = this.adjustProcMap.get(pkgName);
            if (ap != null) {
                return ap;
            }
            AdjustProc adjustProc = new AdjustProc();
            this.adjustProcMap.put(pkgName, adjustProc);
            return adjustProc;
        }

        /* access modifiers changed from: package-private */
        public void removeAdjustProc(String pkgName) {
            this.adjustProcMap.remove(pkgName);
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removeAdjustProc, pkg:" + pkgName);
            }
        }

        /* access modifiers changed from: package-private */
        public void dump(PrintWriter pw) {
            synchronized (this.lock) {
                pw.println("=======dumping shb bg");
                pw.println("===pending pkg:" + this.pendingMapByPkg);
                pw.println();
                pw.println("===pending proc:");
                this.pendingMapByProc.dump(pw);
                pw.println();
                pw.println("===pending actions:");
                this.pendingMapForActions.dump(pw);
                pw.println();
                pw.println("===adjust pkg:");
                this.adjustPkgsMap.dump(pw);
                pw.println();
                pw.println("===adjust actions:");
                this.adjustPackageForActions.dump(pw);
                pw.println("===adjust proc:");
                synchronized (this.lock) {
                    for (Map.Entry<String, AdjustProc> entry : this.adjustProcMap.entrySet()) {
                        pw.print("{" + ((Object) entry.getKey()) + AwarenessInnerConstants.COLON_KEY);
                        entry.getValue().dump(pw);
                        pw.println("}");
                    }
                }
                pw.println();
                pw.println("=======dumping shb end");
            }
        }

        private class AdjustPackage {
            private final HashMap<String, AdjustValue> adjustMap;

            private AdjustPackage() {
                this.adjustMap = new HashMap<>();
            }

            /* access modifiers changed from: package-private */
            public void clearAdjustPkg() {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.clear();
                }
            }

            /* access modifiers changed from: package-private */
            public void putAdjustPkg(String pkg, long interval, int mode) {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.put(pkg, new AdjustValue(interval, mode));
                }
            }

            /* access modifiers changed from: package-private */
            public long getAdjustPkgInterval(String pkg) {
                long interval;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue pkgValue = this.adjustMap.get(pkg);
                    interval = pkgValue == null ? 0 : pkgValue.getInterval();
                }
                return interval;
            }

            /* access modifiers changed from: package-private */
            public int getAdjustPkgMode(String pkg) {
                int mode;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue pkgValue = this.adjustMap.get(pkg);
                    mode = pkgValue == null ? 0 : pkgValue.getMode();
                }
                return mode;
            }

            /* access modifiers changed from: package-private */
            public void removeAdjustPkg(String pkg) {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.remove(pkg);
                }
            }

            /* access modifiers changed from: package-private */
            public boolean containsAdjustKey(String pkg) {
                boolean containsKey;
                synchronized (HeartBeatDatabase.this.lock) {
                    containsKey = this.adjustMap.containsKey(pkg);
                }
                return containsKey;
            }

            /* access modifiers changed from: package-private */
            public void dump(PrintWriter pw) {
                synchronized (HeartBeatDatabase.this.lock) {
                    for (Map.Entry<String, AdjustValue> entry : this.adjustMap.entrySet()) {
                        pw.print(((Object) entry.getKey()) + AwarenessInnerConstants.COLON_KEY);
                        pw.println(entry.getValue());
                    }
                }
            }
        }

        private class AdjustProc {
            private final HashMap<String, AdjustValue> adjustMap = new HashMap<>();

            AdjustProc() {
            }

            /* access modifiers changed from: package-private */
            public void clearAdjustProc() {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.clear();
                }
            }

            /* access modifiers changed from: package-private */
            public void putAdjustProc(String proc, long interval, int mode) {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.put(proc, new AdjustValue(interval, mode));
                }
            }

            /* access modifiers changed from: package-private */
            public long getAdjustProcInterval(String proc) {
                long interval;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue procValue = this.adjustMap.get(proc);
                    interval = procValue == null ? 0 : procValue.getInterval();
                }
                return interval;
            }

            /* access modifiers changed from: package-private */
            public int getAdjustProcMode(String proc) {
                int mode;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue procValue = this.adjustMap.get(proc);
                    mode = procValue == null ? 0 : procValue.getMode();
                }
                return mode;
            }

            /* access modifiers changed from: package-private */
            public int removeAdjustProc(String proc) {
                int size;
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.remove(proc);
                    size = this.adjustMap.size();
                }
                return size;
            }

            /* access modifiers changed from: package-private */
            public boolean containsAdjustKey(String proc) {
                boolean containsKey;
                synchronized (HeartBeatDatabase.this.lock) {
                    containsKey = this.adjustMap.containsKey(proc);
                }
                return containsKey;
            }

            /* access modifiers changed from: package-private */
            public void dump(PrintWriter pw) {
                synchronized (HeartBeatDatabase.this.lock) {
                    for (Map.Entry<String, AdjustValue> entry : this.adjustMap.entrySet()) {
                        pw.print("[" + ((Object) entry.getKey()) + AwarenessInnerConstants.COLON_KEY);
                        StringBuilder sb = new StringBuilder();
                        sb.append(entry.getValue());
                        sb.append("]");
                        pw.print(sb.toString());
                    }
                }
            }
        }

        private class AdjustPackageForActions {
            private final HashMap<String, HashMap<String, AdjustValue>> mPkgMap;

            private AdjustPackageForActions() {
                this.mPkgMap = new HashMap<>();
            }

            /* access modifiers changed from: package-private */
            public void clearAdjustPkg() {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.mPkgMap.clear();
                }
            }

            /* access modifiers changed from: package-private */
            public void putAdjustPkg(String pkg, List<String> actions, long interval, int mode) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = this.mPkgMap.get(pkg);
                    if (actionMap == null) {
                        actionMap = new HashMap<>();
                        this.mPkgMap.put(pkg, actionMap);
                    }
                    for (String action : actions) {
                        actionMap.put(action, new AdjustValue(interval, mode));
                    }
                }
            }

            /* access modifiers changed from: package-private */
            public AdjustValue getAdjustValue(String pkg, String action) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = this.mPkgMap.get(pkg);
                    if (actionMap == null) {
                        return null;
                    }
                    return actionMap.get(action);
                }
            }

            /* access modifiers changed from: package-private */
            public void removeAdjustPkg(String pkg, List<String> actions) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = this.mPkgMap.get(pkg);
                    if (actionMap != null) {
                        for (String action : actions) {
                            actionMap.remove(action);
                        }
                        if (actionMap.isEmpty()) {
                            this.mPkgMap.remove(pkg);
                        }
                    }
                }
            }

            /* access modifiers changed from: package-private */
            public boolean containsAdjustKey(String pkg, String action) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = this.mPkgMap.get(pkg);
                    if (actionMap == null) {
                        return false;
                    }
                    return actionMap.containsKey(action);
                }
            }

            /* access modifiers changed from: package-private */
            public void dump(PrintWriter pw) {
                synchronized (HeartBeatDatabase.this.lock) {
                    for (Map.Entry<String, HashMap<String, AdjustValue>> entry : this.mPkgMap.entrySet()) {
                        pw.println(((Object) entry.getKey()) + AwarenessInnerConstants.COLON_KEY);
                        for (Map.Entry<String, AdjustValue> entry2 : entry.getValue().entrySet()) {
                            pw.print("    " + ((Object) entry2.getKey()) + AwarenessInnerConstants.COLON_KEY);
                            pw.println(entry2.getValue());
                        }
                    }
                }
            }
        }
    }

    private static class AdjustValue {
        /* access modifiers changed from: private */
        public long interval = 0;
        /* access modifiers changed from: private */
        public int mode = 0;

        AdjustValue(long theInterval, int theMode) {
            this.interval = theInterval;
            this.mode = theMode;
        }

        /* access modifiers changed from: package-private */
        public long getInterval() {
            return this.interval;
        }

        /* access modifiers changed from: package-private */
        public int getMode() {
            return this.mode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(this.interval);
            sb.append(" ");
            sb.append(this.mode);
            return sb.toString();
        }
    }

    private static class MultiMap<K, V> {
        private HashMap<K, List<V>> store;

        private MultiMap() {
            this.store = new HashMap<>();
        }

        /* access modifiers changed from: package-private */
        public List<V> getAll(K key) {
            List<V> values = this.store.get(key);
            return values != null ? values : Collections.emptyList();
        }

        /* access modifiers changed from: package-private */
        public void put(K key, V val) {
            List<V> curVals = this.store.get(key);
            if (curVals == null) {
                curVals = new ArrayList(3);
                this.store.put(key, curVals);
            }
            if (!curVals.contains(val)) {
                curVals.add(val);
            }
        }

        /* access modifiers changed from: package-private */
        public void remove(K key, V val) {
            List<V> curVals = this.store.get(key);
            if (curVals != null) {
                curVals.remove(val);
                if (curVals.isEmpty()) {
                    this.store.remove(key);
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void removeAll(K key) {
            this.store.remove(key);
        }

        /* access modifiers changed from: package-private */
        public void clear() {
            this.store.clear();
        }

        /* access modifiers changed from: package-private */
        public void dump(PrintWriter pw) {
            for (Map.Entry<K, List<V>> entry : this.store.entrySet()) {
                pw.print(((Object) entry.getKey()) + AwarenessInnerConstants.COLON_KEY);
                pw.println(entry.getValue());
            }
        }
    }
}

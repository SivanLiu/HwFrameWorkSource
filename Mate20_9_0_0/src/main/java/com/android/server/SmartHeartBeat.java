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
import com.android.server.AlarmManagerService.Alarm;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class SmartHeartBeat extends SmartHeartBeatDummy {
    static final int ALARM_ADJUST_BACKWARD = 2;
    static final int ALARM_ADJUST_FORWARD = 0;
    static final int ALARM_ADJUST_NEAR = 1;
    static final int ALARM_ADJUST_NONE = -1;
    static final int ALARM_ADJUST_PACKAGE_MSG = 102;
    static final int ALARM_PENDING_PACKAGE_MSG = 101;
    static final int ALL_ALARM_TYPE = 0;
    static final boolean DEBUG_HEART_BEAT = SystemProperties.getBoolean("persist.sys.shb.debug", false);
    static final int NONE_WAKEUP_ALARM_TYPE = 2;
    static final boolean SHB_MODULE_SWITCHER = SystemProperties.getBoolean("persist.sys.shb.switcher", true);
    static final String TAG = "SmartHeartBeat";
    static final int WAKEUP_ALARM_TYPE = 1;
    private static HwAlarmManagerService mAlarmService;
    private static Context mContext;
    private static SmartHeartBeatDummy mInstance = null;
    private static final Object mLock = new Object();
    private HeartBeatDatabase mDataMap;
    private Handler mHandler;
    private HandlerThread mThread = new HandlerThread("HeartBeatHandlerThread");

    private static class AdjustValue {
        private long interval = 0;
        private int mode = 0;

        AdjustValue(long theInterval, int theMode) {
            this.interval = theInterval;
            this.mode = theMode;
        }

        public long getInterval() {
            return this.interval;
        }

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

    private static class HeartBeatDatabase {
        public final AdjustPackageForActions adjustPackageForActions;
        private Object lock;
        public final AdjustPackage noneWakeupAdjustPkgsMap;
        private final MultiMap<String, Integer> pendingMap;
        private final MultiMap<String, String> pendingMapForActions;
        public final AdjustPackage wakeupAdjustPkgsMap;

        private class AdjustPackage {
            private final HashMap<String, AdjustValue> adjustMap;

            private AdjustPackage() {
                this.adjustMap = new HashMap();
            }

            public void clearAdjustPkg() {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.clear();
                }
            }

            public void putAdjustPkg(String pkg, long interval, int mode) {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.put(pkg, new AdjustValue(interval, mode));
                }
            }

            public long getAdjustPkgInterval(String pkg) {
                long interval;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue pkgValue = (AdjustValue) this.adjustMap.get(pkg);
                    interval = pkgValue == null ? 0 : pkgValue.getInterval();
                }
                return interval;
            }

            public int getAdjustPkgMode(String pkg) {
                int mode;
                synchronized (HeartBeatDatabase.this.lock) {
                    AdjustValue pkgValue = (AdjustValue) this.adjustMap.get(pkg);
                    mode = pkgValue == null ? 0 : pkgValue.getMode();
                }
                return mode;
            }

            public void removeAdjustPkg(String pkg) {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.adjustMap.remove(pkg);
                }
            }

            public boolean containsAdjustKey(String pkg) {
                boolean containsKey;
                synchronized (HeartBeatDatabase.this.lock) {
                    containsKey = this.adjustMap.containsKey(pkg);
                }
                return containsKey;
            }

            public void dump(PrintWriter pw) {
                synchronized (HeartBeatDatabase.this.lock) {
                    for (Entry entry : this.adjustMap.entrySet()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(entry.getKey());
                        stringBuilder.append(":");
                        pw.print(stringBuilder.toString());
                        pw.println(entry.getValue());
                    }
                }
            }
        }

        private class AdjustPackageForActions {
            private final HashMap<String, HashMap<String, AdjustValue>> mPkgMap;

            private AdjustPackageForActions() {
                this.mPkgMap = new HashMap();
            }

            public void clearAdjustPkg() {
                synchronized (HeartBeatDatabase.this.lock) {
                    this.mPkgMap.clear();
                }
            }

            public void putAdjustPkg(String pkg, List<String> actions, long interval, int mode) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = (HashMap) this.mPkgMap.get(pkg);
                    if (actionMap == null) {
                        actionMap = new HashMap();
                        this.mPkgMap.put(pkg, actionMap);
                    }
                    if (actionMap != null) {
                        int listSize = actions.size();
                        for (int i = 0; i < listSize; i++) {
                            actionMap.put((String) actions.get(i), new AdjustValue(interval, mode));
                        }
                    }
                }
            }

            public AdjustValue getAdjustValue(String pkg, String action) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = (HashMap) this.mPkgMap.get(pkg);
                    if (actionMap != null) {
                        AdjustValue adjustValue = (AdjustValue) actionMap.get(action);
                        return adjustValue;
                    }
                    return null;
                }
            }

            public void removeAdjustPkg(String pkg, List<String> actions) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = (HashMap) this.mPkgMap.get(pkg);
                    if (actionMap != null) {
                        int listSize = actions.size();
                        for (int i = 0; i < listSize; i++) {
                            actionMap.remove(actions.get(i));
                        }
                        if (actionMap.isEmpty()) {
                            this.mPkgMap.remove(pkg);
                        }
                    }
                }
            }

            public boolean containsAdjustKey(String pkg, String action) {
                synchronized (HeartBeatDatabase.this.lock) {
                    HashMap<String, AdjustValue> actionMap = (HashMap) this.mPkgMap.get(pkg);
                    if (actionMap != null) {
                        boolean containsKey = actionMap.containsKey(action);
                        return containsKey;
                    }
                    return false;
                }
            }

            public void dump(PrintWriter pw) {
                synchronized (HeartBeatDatabase.this.lock) {
                    for (Entry entry : this.mPkgMap.entrySet()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(entry.getKey());
                        stringBuilder.append(":");
                        pw.println(stringBuilder.toString());
                        for (Entry entry2 : ((HashMap) entry.getValue()).entrySet()) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("    ");
                            stringBuilder2.append(entry2.getKey());
                            stringBuilder2.append(":");
                            pw.print(stringBuilder2.toString());
                            pw.println(entry2.getValue());
                        }
                    }
                }
            }
        }

        private HeartBeatDatabase() {
            this.pendingMap = new MultiMap();
            this.pendingMapForActions = new MultiMap();
            this.wakeupAdjustPkgsMap = new AdjustPackage();
            this.noneWakeupAdjustPkgsMap = new AdjustPackage();
            this.adjustPackageForActions = new AdjustPackageForActions();
            this.lock = new Object();
        }

        public void addPendingPackage(String pkgName, int type) {
            this.pendingMap.put(pkgName, Integer.valueOf(type));
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addPendingPackage, pkgName: ");
                stringBuilder.append(pkgName);
                stringBuilder.append(", type: ");
                stringBuilder.append(type);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public void addPendingPackageForActions(String pkgName, List<String> actionList) {
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("addPendingPackageForActions, pkgName: ");
                stringBuilder.append(pkgName);
                stringBuilder.append(", actionList: ");
                stringBuilder.append(actionList);
                Slog.d(str, stringBuilder.toString());
            }
            int listSize = actionList.size();
            for (int i = 0; i < listSize; i++) {
                this.pendingMapForActions.put(pkgName, (String) actionList.get(i));
            }
        }

        public void removePendingPackage(String pkgName, int type) {
            this.pendingMap.remove(pkgName, Integer.valueOf(type));
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePendingPackage, pkgName: ");
                stringBuilder.append(pkgName);
                stringBuilder.append(", type: ");
                stringBuilder.append(type);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public void removePendingPackageForActions(String pkgName, List<String> actionList) {
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removePendingPackageForActions, pkgName: ");
                stringBuilder.append(pkgName);
                stringBuilder.append(", actionList: ");
                stringBuilder.append(actionList);
                Slog.d(str, stringBuilder.toString());
            }
            int listSize = actionList.size();
            for (int i = 0; i < listSize; i++) {
                this.pendingMapForActions.remove(pkgName, (String) actionList.get(i));
            }
        }

        public void removeAllPendingPackages() {
            this.pendingMap.clear();
            this.pendingMapForActions.clear();
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                Slog.d(SmartHeartBeat.TAG, "removeAllPendingPackages");
            }
        }

        public boolean isPendingPackage(Alarm alarm) {
            String pkgName = alarm.packageName;
            String action = SmartHeartBeat.getActionByClearCallingIdentity(alarm.operation);
            List<String> actionList = this.pendingMapForActions.getAll(pkgName);
            String str;
            if (actionList.size() > 0 && action != null && actionList.contains(action)) {
                if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                    str = SmartHeartBeat.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isPendingPackage, true, pkg:");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(", action:");
                    stringBuilder.append(action);
                    Slog.d(str, stringBuilder.toString());
                }
                return true;
            } else if (alarm.type != 0 || alarm.alarmClock == null) {
                List<Integer> pendingTypeList = this.pendingMap.getAll(pkgName);
                if (pendingTypeList.size() > 0) {
                    for (Integer alarmType : pendingTypeList) {
                        if (alarmType.intValue() == alarm.type) {
                            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                                String str2 = SmartHeartBeat.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("isPendingPackage, true, pkg:");
                                stringBuilder2.append(pkgName);
                                Slog.d(str2, stringBuilder2.toString());
                            }
                            return true;
                        }
                    }
                }
                return false;
            } else {
                str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isPendingPackage, false, pkg:");
                stringBuilder3.append(pkgName);
                stringBuilder3.append(", action:");
                stringBuilder3.append(action);
                stringBuilder3.append(",not pending alarm with AlarmClockInfo");
                Slog.d(str, stringBuilder3.toString());
                return false;
            }
        }

        public void dump(PrintWriter pw) {
            synchronized (this.lock) {
                pw.println("=======dumping shb bg");
                pw.println("===pending pkg:");
                this.pendingMap.dump(pw);
                pw.println();
                pw.println("===pending actions:");
                this.pendingMapForActions.dump(pw);
                pw.println();
                pw.println("===adjust pkg wakeup:");
                this.wakeupAdjustPkgsMap.dump(pw);
                pw.println();
                pw.println("===adjust pkg none_wakeup:");
                this.noneWakeupAdjustPkgsMap.dump(pw);
                pw.println();
                pw.println("===adjust actions:");
                this.adjustPackageForActions.dump(pw);
                pw.println("=======dumping shb end");
            }
        }
    }

    private static class HeartBeatHandler extends Handler {
        public HeartBeatHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    pendingPackageMessage(msg);
                    return;
                case 102:
                    adjustPackageMessage(msg);
                    return;
                default:
                    return;
            }
        }

        private void adjustPackageMessage(Message msg) {
            List<String> pkgList = msg.obj;
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("adjustPackageMessage, pkgList: ");
                stringBuilder.append(pkgList);
                Slog.d(str, stringBuilder.toString());
            }
            synchronized (SmartHeartBeat.mAlarmService.mLock) {
                SmartHeartBeat.mAlarmService.rebatchPkgAlarmsLocked(pkgList);
            }
        }

        private void pendingPackageMessage(Message msg) {
            MessageInfo args = msg.obj;
            if (args == null) {
                Slog.d(SmartHeartBeat.TAG, "pendingPackageMessage, null == args");
                return;
            }
            boolean pending = args.state;
            if (SmartHeartBeat.DEBUG_HEART_BEAT) {
                String str = SmartHeartBeat.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pendingPackageMessage, pending: ");
                stringBuilder.append(pending);
                stringBuilder.append(", pkg:");
                stringBuilder.append(args.pkgList);
                Slog.d(str, stringBuilder.toString());
            }
            synchronized (SmartHeartBeat.mAlarmService.mLock) {
                if (args.pkgList == null) {
                    SmartHeartBeat.mAlarmService.rebatchAllAlarmsLocked(true);
                } else {
                    SmartHeartBeat.mAlarmService.rebatchPkgAlarmsLocked(args.pkgList);
                }
            }
        }
    }

    private static class MessageInfo {
        List<String> pkgList;
        boolean state;

        private MessageInfo() {
        }
    }

    private static class MultiMap<K, V> {
        private HashMap<K, List<V>> store;

        private MultiMap() {
            this.store = new HashMap();
        }

        List<V> getAll(K key) {
            List<V> values = (List) this.store.get(key);
            return values != null ? values : Collections.emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = (List) this.store.get(key);
            if (curVals == null) {
                curVals = new ArrayList(3);
                this.store.put(key, curVals);
            }
            if (!curVals.contains(val)) {
                curVals.add(val);
            }
        }

        void remove(K key, V val) {
            List<V> curVals = (List) this.store.get(key);
            if (curVals != null) {
                curVals.remove(val);
                if (curVals.isEmpty()) {
                    this.store.remove(key);
                }
            }
        }

        void removeAll(K key) {
            this.store.remove(key);
        }

        void clear() {
            this.store.clear();
        }

        void dump(PrintWriter pw) {
            for (Entry entry : this.store.entrySet()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(entry.getKey());
                stringBuilder.append(":");
                pw.print(stringBuilder.toString());
                pw.println(entry.getValue());
            }
        }
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

    protected void finalize() {
        this.mThread.quit();
        Slog.i(TAG, "SmartHeartBeat destructor !");
        try {
            super.finalize();
        } catch (Throwable th) {
        }
    }

    private boolean isPending(Alarm alarm) {
        boolean pending = false;
        if (alarm == null) {
            Slog.d(TAG, "isPending, null == alarm");
            return false;
        }
        if (this.mDataMap.isPendingPackage(alarm)) {
            pending = true;
        }
        if (DEBUG_HEART_BEAT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isPending, ret:");
            stringBuilder.append(pending);
            stringBuilder.append(", alarm:");
            stringBuilder.append(alarm);
            Slog.d(str, stringBuilder.toString());
        }
        return pending;
    }

    public boolean shouldPendingAlarm(Alarm alarm) {
        boolean isPending;
        synchronized (mLock) {
            isPending = isPending(alarm);
        }
        return isPending;
    }

    public void setAlarmsPending(List<String> pkgList, List<String> actionList, boolean pending, int pendingType) {
        String str;
        StringBuilder stringBuilder;
        if (pkgList == null || pkgList.size() <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAlarmsPending, pkgList=");
            stringBuilder.append(pkgList);
            Slog.d(str, stringBuilder.toString());
            return;
        }
        if (DEBUG_HEART_BEAT) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAlarmsPending, pkgList=");
            stringBuilder.append(pkgList);
            stringBuilder.append(", action=");
            stringBuilder.append(actionList);
            stringBuilder.append(", pending=");
            stringBuilder.append(pending);
            stringBuilder.append(", pendingType=");
            stringBuilder.append(pendingType);
            Slog.i(str, stringBuilder.toString());
        } else {
            Slog.d(TAG, "setAlarmsPending ...");
        }
        synchronized (mLock) {
            for (String pkgName : pkgList) {
                if (pending) {
                    if (actionList != null) {
                        this.mDataMap.addPendingPackageForActions(pkgName, actionList);
                    } else {
                        if (pendingType == 0 || 1 == pendingType) {
                            this.mDataMap.addPendingPackage(pkgName, 0);
                            this.mDataMap.addPendingPackage(pkgName, 2);
                        }
                        if (pendingType == 0 || 2 == pendingType) {
                            this.mDataMap.addPendingPackage(pkgName, 1);
                            this.mDataMap.addPendingPackage(pkgName, 3);
                        }
                    }
                } else if (actionList != null) {
                    this.mDataMap.removePendingPackageForActions(pkgName, actionList);
                } else {
                    if (pendingType == 0 || 1 == pendingType) {
                        this.mDataMap.removePendingPackage(pkgName, 0);
                        this.mDataMap.removePendingPackage(pkgName, 2);
                    }
                    if (pendingType == 0 || 2 == pendingType) {
                        this.mDataMap.removePendingPackage(pkgName, 1);
                        this.mDataMap.removePendingPackage(pkgName, 3);
                    }
                }
            }
        }
        MessageInfo args = new MessageInfo();
        args.state = pending;
        args.pkgList = pkgList;
        Message msg = this.mHandler.obtainMessage();
        msg.what = 101;
        msg.obj = args;
        this.mHandler.sendMessage(msg);
    }

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

    private boolean isAdjustAlarm(Alarm a) {
        String pkg = a.packageName;
        String action = getActionByClearCallingIdentity(a.operation);
        int type = a.type;
        String str;
        StringBuilder stringBuilder;
        if (this.mDataMap.adjustPackageForActions.containsAdjustKey(pkg, action)) {
            if (DEBUG_HEART_BEAT) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isAdjustAlarm: true,pkg: ");
                stringBuilder2.append(pkg);
                stringBuilder2.append(",action: ");
                stringBuilder2.append(action);
                Slog.d(str, stringBuilder2.toString());
            }
            return true;
        } else if (type != 0 || a.alarmClock == null) {
            boolean isAdjustAlarm;
            if (type == 0 || 2 == type) {
                isAdjustAlarm = this.mDataMap.wakeupAdjustPkgsMap.containsAdjustKey(pkg);
            } else if (1 == type || 3 == type) {
                isAdjustAlarm = this.mDataMap.noneWakeupAdjustPkgsMap.containsAdjustKey(pkg);
            } else {
                if (DEBUG_HEART_BEAT) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("isAdjustAlarm1: ");
                    stringBuilder.append(false);
                    Slog.d(str, stringBuilder.toString());
                }
                return false;
            }
            if (DEBUG_HEART_BEAT) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isAdjustAlarm: ");
                stringBuilder.append(isAdjustAlarm);
                stringBuilder.append(",pkg:");
                stringBuilder.append(pkg);
                Slog.d(str, stringBuilder.toString());
            }
            return isAdjustAlarm;
        } else {
            if (DEBUG_HEART_BEAT) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isAdjustAlarm: false,pkg: ");
                stringBuilder.append(pkg);
                stringBuilder.append(",action: ");
                stringBuilder.append(action);
                stringBuilder.append(",not adjustalarm with AlarmClockInfo");
                Slog.d(str, stringBuilder.toString());
            }
            return false;
        }
    }

    private long calAlarmWhenElapsed(Alarm a) {
        Alarm alarm = a;
        long interval = 0;
        int mode = -1;
        long adjustWhenElapsed = alarm.whenElapsed;
        String pkg = alarm.packageName;
        String action = getActionByClearCallingIdentity(alarm.operation);
        int type = alarm.type;
        long whenElapsed = alarm.whenElapsed;
        AdjustValue adjustValue = this.mDataMap.adjustPackageForActions.getAdjustValue(pkg, action);
        if (adjustValue != null) {
            interval = adjustValue.interval;
            mode = adjustValue.mode;
        }
        if (interval == 0) {
            if (type == 0 || 2 == type) {
                interval = this.mDataMap.wakeupAdjustPkgsMap.getAdjustPkgInterval(pkg);
                mode = this.mDataMap.wakeupAdjustPkgsMap.getAdjustPkgMode(pkg);
            } else if (1 != type && 3 != type) {
                return adjustWhenElapsed;
            } else {
                interval = this.mDataMap.noneWakeupAdjustPkgsMap.getAdjustPkgInterval(pkg);
                mode = this.mDataMap.noneWakeupAdjustPkgsMap.getAdjustPkgMode(pkg);
            }
        }
        if (0 == interval) {
            return adjustWhenElapsed;
        }
        if ((type == 0 || 1 == type) && 0 > whenElapsed) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Maybe the whenElapsed time is incorrect: pkg=");
            stringBuilder.append(pkg);
            stringBuilder.append(" whenElapsed = ");
            stringBuilder.append(whenElapsed);
            Slog.d(str, stringBuilder.toString());
            whenElapsed = SystemClock.elapsedRealtime();
        }
        switch (mode) {
            case 0:
            case 1:
            case 2:
                adjustWhenElapsed = ((((interval / 2) * ((long) mode)) + whenElapsed) / interval) * interval;
                if (mode == 2) {
                    if (2 == mode && adjustWhenElapsed - whenElapsed == interval) {
                        adjustWhenElapsed = whenElapsed;
                        break;
                    }
                } else if (adjustWhenElapsed < SystemClock.elapsedRealtime()) {
                    adjustWhenElapsed += interval;
                    break;
                }
                break;
        }
        if (DEBUG_HEART_BEAT) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("calAlarmWhenElapsed pkg: ");
            stringBuilder2.append(pkg);
            stringBuilder2.append(", action: ");
            stringBuilder2.append(action);
            stringBuilder2.append(", whenElapsed: ");
            stringBuilder2.append(whenElapsed);
            stringBuilder2.append(", adjusted WhenElapsed: ");
            stringBuilder2.append(adjustWhenElapsed);
            stringBuilder2.append(", interval: ");
            stringBuilder2.append(interval);
            stringBuilder2.append(", mode: ");
            stringBuilder2.append(mode);
            Slog.d(str2, stringBuilder2.toString());
        }
        return adjustWhenElapsed;
    }

    private long getAdjustAlarmWhenElapsed(Alarm a) {
        long val = isAdjustAlarm(a) ? calAlarmWhenElapsed(a) : a.whenElapsed;
        if (DEBUG_HEART_BEAT) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAdjustAlarmWhenElapsed, val: ");
            stringBuilder.append(val);
            stringBuilder.append(", pkg: ");
            stringBuilder.append(a.packageName);
            stringBuilder.append(", whenElapsed: ");
            stringBuilder.append(a.whenElapsed);
            stringBuilder.append(", type: ");
            stringBuilder.append(a.type);
            Slog.d(str, stringBuilder.toString());
        }
        return val;
    }

    private static String getActionByClearCallingIdentity(PendingIntent operation) {
        long identity = Binder.clearCallingIdentity();
        String action = null;
        if (operation != null) {
            try {
                action = operation.getIntent().getAction();
            } catch (Throwable th) {
            }
        }
        Binder.restoreCallingIdentity(identity);
        return action;
    }

    public void setAlarmsAdjust(List<String> pkgList, List<String> actionList, boolean adjust, int type, long interval, int mode) {
        List<String> list = pkgList;
        List<String> list2 = actionList;
        boolean z = adjust;
        int i = type;
        long j = interval;
        int i2 = mode;
        String str;
        StringBuilder stringBuilder;
        if (list == null || pkgList.size() <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAlarmsAdjust, pkgList: ");
            stringBuilder.append(list);
            Slog.i(str, stringBuilder.toString());
            return;
        }
        if (DEBUG_HEART_BEAT) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAlarmsAdjust, pkgList=");
            stringBuilder.append(list);
            stringBuilder.append(", action=");
            stringBuilder.append(list2);
            stringBuilder.append(", adjust=");
            stringBuilder.append(z);
            stringBuilder.append(", type=");
            stringBuilder.append(i);
            stringBuilder.append(", interval=");
            stringBuilder.append(j);
            stringBuilder.append(", mode=");
            stringBuilder.append(i2);
            Slog.i(str, stringBuilder.toString());
        } else {
            Slog.d(TAG, "setAlarmsAdjust ...");
        }
        synchronized (mLock) {
            Iterator<String> it = pkgList.iterator();
            while (it.hasNext()) {
                Iterator<String> it2;
                String pkg = (String) it.next();
                if (!z) {
                    it2 = it;
                    str = pkg;
                    if (list2 != null) {
                        this.mDataMap.adjustPackageForActions.removeAdjustPkg(str, list2);
                    } else {
                        if (i == 0 || 1 == i) {
                            this.mDataMap.wakeupAdjustPkgsMap.removeAdjustPkg(str);
                        }
                        if (i == 0 || 2 == i) {
                            this.mDataMap.noneWakeupAdjustPkgsMap.removeAdjustPkg(str);
                        }
                    }
                } else if (j <= 0) {
                    Slog.d(TAG, "setAlarmsAdjust interval must be greater than 0.");
                    return;
                } else if (list2 != null) {
                    it2 = it;
                    str = pkg;
                    this.mDataMap.adjustPackageForActions.putAdjustPkg(pkg, list2, j, i2);
                } else {
                    it2 = it;
                    str = pkg;
                    if (i == 0 || 1 == i) {
                        this.mDataMap.wakeupAdjustPkgsMap.putAdjustPkg(str, j, i2);
                    }
                    if (i == 0 || 2 == i) {
                        this.mDataMap.noneWakeupAdjustPkgsMap.putAdjustPkg(str, j, i2);
                    }
                }
                it = it2;
            }
            Message msg = this.mHandler.obtainMessage();
            msg.what = 102;
            msg.obj = list;
            this.mHandler.sendMessage(msg);
        }
    }

    public void removeAllAdjustAlarms() {
        Slog.i(TAG, "remove all adjust alarms.");
        synchronized (mLock) {
            this.mDataMap.wakeupAdjustPkgsMap.clearAdjustPkg();
            this.mDataMap.noneWakeupAdjustPkgsMap.clearAdjustPkg();
            this.mDataMap.adjustPackageForActions.clearAdjustPkg();
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0050, code:
            return;
     */
    /* JADX WARNING: Missing block: B:24:0x00ac, code:
            return;
     */
    /* JADX WARNING: Missing block: B:35:0x010d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void adjustAlarmIfNeeded(Alarm a) {
        if (a == null) {
            Slog.e(TAG, "adjustAlarmIfNeeded, alarm is null!");
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (mLock) {
            long j = 259200000;
            if (now >= a.whenElapsed || a.whenElapsed - now <= 259200000) {
                long delay;
                if (isPending(a)) {
                    if (now >= a.whenElapsed) {
                        j = 259200000 + (now - a.whenElapsed);
                    }
                    delay = j;
                    a.when += delay;
                    a.whenElapsed += delay;
                    a.maxWhenElapsed += delay;
                    if (DEBUG_HEART_BEAT) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("adjustAlarmIfNeeded, is pending alarm: ");
                        stringBuilder.append(a.packageName);
                        stringBuilder.append(", tag: ");
                        stringBuilder.append(a.statsTag);
                        stringBuilder.append(", whenElapsed: ");
                        stringBuilder.append(a.whenElapsed);
                        stringBuilder.append(", now: ");
                        stringBuilder.append(now);
                        Slog.i(str, stringBuilder.toString());
                    }
                } else {
                    delay = getAdjustAlarmWhenElapsed(a);
                    if (delay != a.whenElapsed) {
                        long delta = delay - a.whenElapsed;
                        a.whenElapsed += delta;
                        a.when += delta;
                        a.maxWhenElapsed = a.whenElapsed > a.maxWhenElapsed ? a.whenElapsed : a.maxWhenElapsed;
                        if (DEBUG_HEART_BEAT) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("adjustAlarmLocked, is unify alarm: ");
                            stringBuilder2.append(a.packageName);
                            stringBuilder2.append(", tag: ");
                            stringBuilder2.append(a.statsTag);
                            stringBuilder2.append(", whenElapsed: ");
                            stringBuilder2.append(a.whenElapsed);
                            stringBuilder2.append(", now: ");
                            stringBuilder2.append(now);
                            Slog.i(str2, stringBuilder2.toString());
                        }
                    }
                }
            } else if (DEBUG_HEART_BEAT) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("adjustAlarmIfNeeded, no need change alarm: ");
                stringBuilder3.append(a.packageName);
                stringBuilder3.append(", whenElapsed: ");
                stringBuilder3.append(a.whenElapsed);
                stringBuilder3.append(", now: ");
                stringBuilder3.append(now);
                Slog.i(str3, stringBuilder3.toString());
            }
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (mLock) {
            this.mDataMap.dump(pw);
        }
    }
}

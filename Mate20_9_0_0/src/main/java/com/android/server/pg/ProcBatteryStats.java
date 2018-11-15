package com.android.server.pg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.MutableInt;
import android.util.Slog;
import com.android.internal.os.BatteryStatsImpl.Clocks;
import com.android.internal.os.BatteryStatsImpl.StopwatchTimer;
import com.android.internal.os.BatteryStatsImpl.SystemClocks;
import com.android.internal.os.BatteryStatsImpl.TimeBase;
import java.util.ArrayList;
import java.util.List;

public class ProcBatteryStats {
    private static final String DESCRIPTOR = "com.huawei.pgmng.api.IPGManager";
    private static final int GET_POWER_STATS_TRANSACTION = 101;
    private static final int MAX_WAKELOCKS_PER_UID = 100;
    private static final int MAX_WAKERLOCKS_WEIXIN = 60;
    private static final int NOTE_RESET_ALL_INFO_TRANSACTION = 102;
    private static final int POWER_SOURCE_TYPE_WAKELOCK = 0;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 1;
    private static final int STATS_TYPE = 0;
    private static final int SYS_EVENT_POWER_CONNECTED = 1;
    private static final int SYS_EVENT_POWER_DISCONNECTED = 2;
    private static final int SYS_EVENT_SCREEN_OFF = 6;
    private static final int SYS_EVENT_SCREEN_ON = 5;
    private static final int SYS_EVENT_UID_REMOVED = 4;
    private static final int SYS_EVENT_USER_REMOVED = 3;
    private static final String TAG = "ProcBatteryStats";
    private boolean DEBUG_COMMON = false;
    private boolean DEBUG_SCREENON = false;
    private boolean DEBUG_WAKELOCK = false;
    private final ArrayMap<String, Integer> mActionIdMap = new ArrayMap();
    private Clocks mClocks = new SystemClocks();
    private Context mContext = null;
    private final TimeBase mOnBatteryScreenOffTimeBase = new TimeBase();
    private final TimeBase mOnBatteryTimeBase = new TimeBase();
    private final ArrayList<StopwatchTimer> mPartialTimers = new ArrayList();
    private int mScreenState = 1;
    private SysEventsHandler mSysEventsHandler = null;
    private final ArrayMap<String, ProcWakeLockTime> mWakeLockTimeMap = new ArrayMap();

    private static abstract class OverflowArrayMap<T> {
        private static final String OVERFLOW_NAME = "*overflow*";
        private static final String OVERFLOW_WEIXIN = "WakerLock:overflow";
        int M = 0;
        ArrayMap<String, MutableInt> mActiveOverflow;
        ArrayMap<String, MutableInt> mActiveOverflowWeixin;
        T mCurOverflow;
        T mCurOverflowWeixin;
        final ArrayMap<String, T> mMap = new ArrayMap();

        public abstract T instantiateObject();

        public ArrayMap<String, T> getMap() {
            return this.mMap;
        }

        public void clear() {
            this.mMap.clear();
            this.mCurOverflow = null;
            this.mActiveOverflow = null;
            this.mCurOverflowWeixin = null;
            this.mActiveOverflowWeixin = null;
        }

        public void add(String name, T obj) {
            if (name == null) {
                name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            this.mMap.put(name, obj);
            if (OVERFLOW_NAME.equals(name)) {
                this.mCurOverflow = obj;
            } else if (OVERFLOW_WEIXIN.equals(name)) {
                this.mCurOverflowWeixin = obj;
            }
            if (name.startsWith("WakerLock:")) {
                this.M++;
            }
        }

        public void cleanup() {
            String str;
            StringBuilder stringBuilder;
            if (this.mActiveOverflowWeixin != null && this.mActiveOverflowWeixin.size() == 0) {
                this.mActiveOverflowWeixin = null;
            }
            if (this.mActiveOverflowWeixin == null) {
                if (this.mMap.containsKey(OVERFLOW_WEIXIN)) {
                    str = ProcBatteryStats.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up with no active overflow weixin, but have overflow entry ");
                    stringBuilder.append(this.mMap.get(OVERFLOW_WEIXIN));
                    Slog.wtf(str, stringBuilder.toString());
                    this.mMap.remove(OVERFLOW_WEIXIN);
                }
                this.mCurOverflowWeixin = null;
            } else if (this.mCurOverflowWeixin == null || !this.mMap.containsKey(OVERFLOW_WEIXIN)) {
                str = ProcBatteryStats.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cleaning up with active overflow weixin, but no overflow entry: cur=");
                stringBuilder.append(this.mCurOverflowWeixin);
                stringBuilder.append(" map=");
                stringBuilder.append(this.mMap.get(OVERFLOW_WEIXIN));
                Slog.wtf(str, stringBuilder.toString());
            }
            if (this.mActiveOverflow != null && this.mActiveOverflow.size() == 0) {
                this.mActiveOverflow = null;
            }
            if (this.mActiveOverflow == null) {
                if (this.mMap.containsKey(OVERFLOW_NAME)) {
                    str = ProcBatteryStats.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up with no active overflow, but have overflow entry ");
                    stringBuilder.append(this.mMap.get(OVERFLOW_NAME));
                    Slog.wtf(str, stringBuilder.toString());
                    this.mMap.remove(OVERFLOW_NAME);
                }
                this.mCurOverflow = null;
            } else if (this.mCurOverflow == null || !this.mMap.containsKey(OVERFLOW_NAME)) {
                str = ProcBatteryStats.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cleaning up with active overflow, but no overflow entry: cur=");
                stringBuilder2.append(this.mCurOverflow);
                stringBuilder2.append(" map=");
                stringBuilder2.append(this.mMap.get(OVERFLOW_NAME));
                Slog.wtf(str, stringBuilder2.toString());
            }
        }

        public T startObject(String name) {
            if (name == null) {
                name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            MutableInt over;
            String str;
            StringBuilder stringBuilder;
            T instantiateObject;
            if (this.mActiveOverflowWeixin != null) {
                over = (MutableInt) this.mActiveOverflowWeixin.get(name);
                if (over != null) {
                    obj = this.mCurOverflowWeixin;
                    if (obj == null) {
                        str = ProcBatteryStats.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Have active overflow ");
                        stringBuilder.append(name);
                        stringBuilder.append(" but null overflow weixin");
                        Slog.wtf(str, stringBuilder.toString());
                        instantiateObject = instantiateObject();
                        this.mCurOverflowWeixin = instantiateObject;
                        obj = instantiateObject;
                        this.mMap.put(OVERFLOW_WEIXIN, obj);
                    }
                    over.value++;
                    return obj;
                }
            }
            if (name.startsWith("WakerLock:")) {
                this.M++;
                if (this.M > 60) {
                    obj = this.mCurOverflowWeixin;
                    if (obj == null) {
                        T instantiateObject2 = instantiateObject();
                        this.mCurOverflowWeixin = instantiateObject2;
                        obj = instantiateObject2;
                        this.mMap.put(OVERFLOW_WEIXIN, obj);
                    }
                    if (this.mActiveOverflowWeixin == null) {
                        this.mActiveOverflowWeixin = new ArrayMap();
                    }
                    this.mActiveOverflowWeixin.put(name, new MutableInt(1));
                    return obj;
                }
            }
            if (this.mActiveOverflow != null) {
                over = (MutableInt) this.mActiveOverflow.get(name);
                if (over != null) {
                    obj = this.mCurOverflow;
                    if (obj == null) {
                        str = ProcBatteryStats.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Have active overflow ");
                        stringBuilder.append(name);
                        stringBuilder.append(" but null overflow");
                        Slog.wtf(str, stringBuilder.toString());
                        instantiateObject = instantiateObject();
                        this.mCurOverflow = instantiateObject;
                        obj = instantiateObject;
                        this.mMap.put(OVERFLOW_NAME, obj);
                    }
                    over.value++;
                    return obj;
                }
            }
            if (this.mMap.size() >= 100) {
                str = ProcBatteryStats.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wakelocks more than 100, name: ");
                stringBuilder.append(name);
                Slog.i(str, stringBuilder.toString());
                obj = this.mCurOverflow;
                if (obj == null) {
                    instantiateObject = instantiateObject();
                    this.mCurOverflow = instantiateObject;
                    obj = instantiateObject;
                    this.mMap.put(OVERFLOW_NAME, obj);
                }
                if (this.mActiveOverflow == null) {
                    this.mActiveOverflow = new ArrayMap();
                }
                this.mActiveOverflow.put(name, new MutableInt(1));
                return obj;
            }
            obj = instantiateObject();
            this.mMap.put(name, obj);
            return obj;
        }

        public T stopObject(String name) {
            if (name == null) {
                name = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            MutableInt over;
            if (this.mActiveOverflowWeixin != null) {
                over = (MutableInt) this.mActiveOverflowWeixin.get(name);
                if (over != null) {
                    obj = this.mCurOverflowWeixin;
                    if (obj != null) {
                        over.value--;
                        if (over.value <= 0) {
                            this.mActiveOverflowWeixin.remove(name);
                        }
                        return obj;
                    }
                }
            }
            if (this.mActiveOverflow != null) {
                over = (MutableInt) this.mActiveOverflow.get(name);
                if (over != null) {
                    obj = this.mCurOverflow;
                    if (obj != null) {
                        over.value--;
                        if (over.value <= 0) {
                            this.mActiveOverflow.remove(name);
                        }
                        return obj;
                    }
                }
            }
            String str = ProcBatteryStats.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to find object for ");
            stringBuilder.append(name);
            stringBuilder.append(" mapsize=");
            stringBuilder.append(this.mMap.size());
            stringBuilder.append(" activeoverflow=");
            stringBuilder.append(this.mActiveOverflow);
            stringBuilder.append(" curoverflow=");
            stringBuilder.append(this.mCurOverflow);
            Slog.wtf(str, stringBuilder.toString());
            return null;
        }
    }

    private final class ProcWakeLockTime {
        String mProcName;
        int mUid;
        OverflowArrayMap<WakeLock> mWakelockStats = new OverflowArrayMap<WakeLock>() {
            public WakeLock instantiateObject() {
                return new WakeLock(ProcBatteryStats.this, null);
            }
        };

        public ProcWakeLockTime(String name, int uid) {
            this.mProcName = name;
            this.mUid = uid;
        }

        private OverflowArrayMap<WakeLock> getWakeLockStats() {
            return this.mWakelockStats;
        }

        private boolean reset() {
            if (ProcBatteryStats.this.DEBUG_WAKELOCK) {
                String str = ProcBatteryStats.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pwt, reset, mProcName: ");
                stringBuilder.append(this.mProcName);
                stringBuilder.append(", mUid: ");
                stringBuilder.append(this.mUid);
                Slog.d(str, stringBuilder.toString());
            }
            boolean active = false;
            ArrayMap<String, WakeLock> wakeStats = this.mWakelockStats.getMap();
            for (int iw = wakeStats.size() - 1; iw >= 0; iw--) {
                if (((WakeLock) wakeStats.valueAt(iw)).reset()) {
                    wakeStats.removeAt(iw);
                } else {
                    active = true;
                }
            }
            this.mWakelockStats.cleanup();
            if (active) {
                return false;
            }
            return true;
        }
    }

    private final class SysEventsHandler extends Handler {
        public SysEventsHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean screenOff = ProcBatteryStats.this.mScreenState == 0;
            Intent intent = msg.obj;
            int i = -1;
            switch (msg.what) {
                case 1:
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        Slog.i(ProcBatteryStats.TAG, "SYS_EVENT_POWER_CONNECTED");
                    }
                    ProcBatteryStats.this.updateTimeBases(false, screenOff);
                    return;
                case 2:
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        Slog.i(ProcBatteryStats.TAG, "SYS_EVENT_POWER_DISCONNECTED");
                    }
                    ProcBatteryStats.this.updateTimeBases(true, screenOff);
                    return;
                case 3:
                    int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    ProcBatteryStats.this.removeWlByUserId(userId);
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        String str = ProcBatteryStats.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SYS_EVENT_USER_REMOVED, userId: ");
                        stringBuilder.append(userId);
                        Slog.i(str, stringBuilder.toString());
                        return;
                    }
                    return;
                case 4:
                    Bundle intentExtras = intent.getExtras();
                    if (intentExtras != null) {
                        i = intentExtras.getInt("android.intent.extra.UID");
                    }
                    int uid = i;
                    ProcBatteryStats.this.removeWlByUid(uid);
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        String str2 = ProcBatteryStats.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SYS_EVENT_UID_REMOVED, uid: ");
                        stringBuilder2.append(uid);
                        Slog.i(str2, stringBuilder2.toString());
                        return;
                    }
                    return;
                case 5:
                    ProcBatteryStats.this.noteScreenState(1);
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        Slog.i(ProcBatteryStats.TAG, "SYS_EVENT_SCREEN_ON");
                        return;
                    }
                    return;
                case 6:
                    ProcBatteryStats.this.noteScreenState(0);
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        Slog.i(ProcBatteryStats.TAG, "SYS_EVENT_SCREEN_OFF");
                        return;
                    }
                    return;
                default:
                    String str3 = ProcBatteryStats.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("unexpected sysEvent: ");
                    stringBuilder3.append(msg.what);
                    Slog.w(str3, stringBuilder3.toString());
                    return;
            }
        }
    }

    private final class WakeLock {
        long mCounter;
        StopwatchTimer mTimerPartial;

        private WakeLock() {
            this.mCounter = 0;
            this.mTimerPartial = new StopwatchTimer(ProcBatteryStats.this.mClocks, null, 0, ProcBatteryStats.this.mPartialTimers, ProcBatteryStats.this.mOnBatteryScreenOffTimeBase);
        }

        /* synthetic */ WakeLock(ProcBatteryStats x0, AnonymousClass1 x1) {
            this();
        }

        private void startRunning(long realtime) {
            this.mTimerPartial.startRunningLocked(realtime);
            this.mCounter++;
        }

        private void stopRunning(long realtime) {
            this.mTimerPartial.stopRunningLocked(realtime);
        }

        private long getTotalTime(long realtime) {
            return this.mTimerPartial.getTotalTimeLocked(1000 * realtime, 0);
        }

        private StopwatchTimer getStopwatchTimer() {
            return this.mTimerPartial;
        }

        boolean reset() {
            this.mCounter = 0;
            if (!this.mTimerPartial.reset(false)) {
                return false;
            }
            this.mTimerPartial.detach();
            return true;
        }
    }

    public ProcBatteryStats(Context context) {
        this.mContext = context;
        initDebugSwitches();
        initTimeBases();
    }

    private void initDebugSwitches() {
        boolean z = false;
        int prop = SystemProperties.getInt("persist.procbatterystats.debug", 0);
        this.DEBUG_COMMON = (prop & 1) != 0;
        this.DEBUG_WAKELOCK = (prop & 2) != 0;
        if ((prop & 4) != 0) {
            z = true;
        }
        this.DEBUG_SCREENON = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("persist.procbatterystats.debug: ");
        stringBuilder.append(prop);
        Slog.d(str, stringBuilder.toString());
    }

    private void initTimeBases() {
        long realtimeUs = SystemClock.elapsedRealtime() * 1000;
        long uptimeUs = SystemClock.uptimeMillis() * 1000;
        this.mOnBatteryTimeBase.init(uptimeUs, realtimeUs);
        this.mOnBatteryScreenOffTimeBase.init(uptimeUs, realtimeUs);
    }

    private void updateTimeBases(boolean unplugged, boolean screenOff) {
        if (this.DEBUG_COMMON) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateTimeBases, unplugged: ");
            stringBuilder.append(unplugged);
            stringBuilder.append(", screenOff: ");
            stringBuilder.append(screenOff);
            Slog.d(str, stringBuilder.toString());
        }
        long realtimeUs = SystemClock.elapsedRealtime() * 1000;
        long uptimeUs = 1000 * SystemClock.uptimeMillis();
        this.mOnBatteryTimeBase.setRunning(unplugged, uptimeUs, realtimeUs);
        synchronized (this.mWakeLockTimeMap) {
            TimeBase timeBase = this.mOnBatteryScreenOffTimeBase;
            boolean z = unplugged && screenOff;
            timeBase.setRunning(z, uptimeUs, realtimeUs);
        }
    }

    public void onSystemReady() {
        startSystemEventHandleThread();
        Slog.i(TAG, "ProcBatteryStats--systemReady");
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case 101:
                data.enforceInterface(DESCRIPTOR);
                List<String> stats = getPowerStats(data.readInt());
                reply.writeNoException();
                reply.writeStringList(stats);
                return true;
            case 102:
                data.enforceInterface(DESCRIPTOR);
                noteResetAllProcInfo();
                reply.writeNoException();
                return true;
            default:
                return false;
        }
    }

    private void startSystemEventHandleThread() {
        HandlerThread sysHandlerthread = new HandlerThread("PgmsEventsHandler", 10);
        sysHandlerthread.start();
        this.mSysEventsHandler = new SysEventsHandler(sysHandlerthread.getLooper());
        registerBroadcast();
    }

    private void registerBroadcast() {
        if (this.mContext == null) {
            Slog.w(TAG, "null mContext!");
            return;
        }
        Slog.i(TAG, "ProcBatteryStats--registerBroadcast");
        this.mActionIdMap.put("android.intent.action.ACTION_POWER_CONNECTED", Integer.valueOf(1));
        this.mActionIdMap.put("android.intent.action.ACTION_POWER_DISCONNECTED", Integer.valueOf(2));
        this.mActionIdMap.put("android.intent.action.USER_REMOVED", Integer.valueOf(3));
        this.mActionIdMap.put("android.intent.action.UID_REMOVED", Integer.valueOf(4));
        this.mActionIdMap.put("android.intent.action.SCREEN_ON", Integer.valueOf(5));
        this.mActionIdMap.put("android.intent.action.SCREEN_OFF", Integer.valueOf(6));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.UID_REMOVED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    if (ProcBatteryStats.this.DEBUG_COMMON) {
                        Slog.d(ProcBatteryStats.TAG, "null intent");
                    }
                    return;
                }
                Message msg = ProcBatteryStats.this.mSysEventsHandler.obtainMessage(((Integer) ProcBatteryStats.this.mActionIdMap.get(intent.getAction())).intValue());
                msg.obj = intent;
                ProcBatteryStats.this.mSysEventsHandler.sendMessageDelayed(msg, 0);
            }
        }, UserHandle.ALL, filter, null, null);
    }

    public void noteResetAllProcInfo() {
        Slog.d(TAG, "noteResetAllProcInfo");
        initTimeBases();
        resetWakeLockTime();
    }

    public List<String> getPowerStats(int type) {
        String str;
        StringBuilder stringBuilder;
        if (this.DEBUG_COMMON) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getPowerStats, type: ");
            stringBuilder.append(type);
            Slog.d(str, stringBuilder.toString());
        }
        if (type == 0) {
            return getWakeLockStatsList();
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("unexpected Type: ");
        stringBuilder.append(type);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    public void noteScreenState(int state) {
        if (this.DEBUG_SCREENON) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("screenState, ");
            stringBuilder.append(this.mScreenState);
            stringBuilder.append(" -> ");
            stringBuilder.append(state);
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mScreenState != state) {
            this.mScreenState = state;
            if (state == 1) {
                updateTimeBases(this.mOnBatteryTimeBase.isRunning(), false);
            } else if (state == 0) {
                updateTimeBases(this.mOnBatteryTimeBase.isRunning(), true);
            }
        }
    }

    public void processWakeLock(int event, String tag, WorkSource ws, String pkgName, int uid) {
        String str;
        int i;
        String str2;
        int uid2;
        Throwable th;
        WorkSource workSource = ws;
        String pkgName2 = pkgName;
        if (this.DEBUG_WAKELOCK) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processWlInfo, event: ");
            i = event;
            stringBuilder.append(i);
            stringBuilder.append(", tag: ");
            str2 = tag;
            stringBuilder.append(str2);
            stringBuilder.append(", ws: ");
            stringBuilder.append(workSource);
            stringBuilder.append(", pkgName: ");
            stringBuilder.append(pkgName2);
            stringBuilder.append(", uid: ");
            uid2 = uid;
            stringBuilder.append(uid2);
            Slog.d(str, stringBuilder.toString());
        } else {
            i = event;
            str2 = tag;
            uid2 = uid;
        }
        long realtime = SystemClock.elapsedRealtime();
        if (pkgName2 == null) {
            pkgName2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            Slog.w(TAG, "name null");
        }
        String pkgName3 = pkgName2;
        ArrayMap arrayMap = this.mWakeLockTimeMap;
        synchronized (arrayMap) {
            ArrayMap arrayMap2;
            int i2;
            if (workSource == null) {
                arrayMap2 = arrayMap;
                try {
                    updateWlTimer(i, str2, pkgName3, uid2, realtime);
                } catch (Throwable th2) {
                    th = th2;
                    i2 = uid2;
                    throw th;
                }
            }
            arrayMap2 = arrayMap;
            int length = ws.size();
            int i3 = 0;
            int i4 = 0;
            while (true) {
                int i5 = i4;
                if (i5 < length) {
                    i2 = workSource.get(i5);
                    try {
                        str = workSource.getName(i5);
                        if (str == null) {
                            str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        } else if (str.indexOf(58) > 0) {
                            str = str.substring(i3, str.indexOf(58));
                        }
                        int i6 = i3;
                        int i7 = i5;
                        updateWlTimer(i, str2, str, i2, realtime);
                        i4 = i7 + 1;
                        uid2 = i2;
                        i3 = i6;
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
            }
        }
    }

    private void updateWlTimer(int event, String tag, String name, int uid, long realtime) {
        OverflowArrayMap<WakeLock> wakelockStats = getProcWakeLockTime(name, uid).getWakeLockStats();
        WakeLock wl;
        String str;
        StringBuilder stringBuilder;
        if (event == 160) {
            wl = (WakeLock) wakelockStats.startObject(tag);
            if (wl == null) {
                return;
            }
            if (wl.getStopwatchTimer().isRunningLocked()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("timer is running , not start, name: ");
                stringBuilder.append(name);
                stringBuilder.append(", uid: ");
                stringBuilder.append(uid);
                stringBuilder.append(", tag: ");
                stringBuilder.append(tag);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            if (this.DEBUG_WAKELOCK) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startRunWlTimer, name: ");
                stringBuilder.append(name);
                stringBuilder.append(", uid: ");
                stringBuilder.append(uid);
                stringBuilder.append(", tag: ");
                stringBuilder.append(tag);
                Slog.d(str, stringBuilder.toString());
            }
            wl.startRunning(realtime);
        } else if (event == 161) {
            wl = (WakeLock) wakelockStats.stopObject(tag);
            if (wl == null) {
                return;
            }
            if (wl.getStopwatchTimer().isRunningLocked()) {
                if (this.DEBUG_WAKELOCK) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("stopRunWlTimer, name: ");
                    stringBuilder.append(name);
                    stringBuilder.append(", uid: ");
                    stringBuilder.append(uid);
                    stringBuilder.append(", tag: ");
                    stringBuilder.append(tag);
                    Slog.d(str, stringBuilder.toString());
                }
                wl.stopRunning(realtime);
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("timer is not running , not stop, name: ");
            stringBuilder.append(name);
            stringBuilder.append(", uid: ");
            stringBuilder.append(uid);
            stringBuilder.append(", tag: ");
            stringBuilder.append(tag);
            Slog.w(str, stringBuilder.toString());
        }
    }

    private ProcWakeLockTime getProcWakeLockTime(String procName, int uid) {
        String key = new StringBuilder();
        key.append(procName);
        key.append(uid);
        key = key.toString();
        if (this.mWakeLockTimeMap.containsKey(key)) {
            return (ProcWakeLockTime) this.mWakeLockTimeMap.get(key);
        }
        ProcWakeLockTime pwt = new ProcWakeLockTime(procName, uid);
        this.mWakeLockTimeMap.put(key, pwt);
        if (!this.DEBUG_WAKELOCK) {
            return pwt;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getProcWakeLockTime, procName: ");
        stringBuilder.append(procName);
        stringBuilder.append(", uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(", size: ");
        stringBuilder.append(this.mWakeLockTimeMap.size());
        Slog.d(str, stringBuilder.toString());
        return pwt;
    }

    private void resetWakeLockTime() {
        synchronized (this.mWakeLockTimeMap) {
            for (int i = this.mWakeLockTimeMap.size() - 1; i >= 0; i--) {
                if (((ProcWakeLockTime) this.mWakeLockTimeMap.valueAt(i)).reset()) {
                    this.mWakeLockTimeMap.removeAt(i);
                }
            }
        }
    }

    protected void getWlBatteryStats(List<String> list) {
        Throwable th;
        long realtime = SystemClock.elapsedRealtime();
        int size = this.mWakeLockTimeMap.size();
        if (this.DEBUG_WAKELOCK) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getWlBatteryStats, size: ");
            stringBuilder.append(size);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mWakeLockTimeMap) {
            List<String> list2;
            int i = size - 1;
            while (i >= 0) {
                long totalTimeMs = 0;
                try {
                    StringBuilder sb = new StringBuilder(128);
                    ProcWakeLockTime pwt = (ProcWakeLockTime) this.mWakeLockTimeMap.valueAt(i);
                    ArrayMap<String, WakeLock> wakeStats = pwt.getWakeLockStats().getMap();
                    for (int iw = wakeStats.size() - 1; iw >= 0; iw--) {
                        totalTimeMs += ((WakeLock) wakeStats.valueAt(iw)).getTotalTime(realtime) / 1000;
                    }
                    String str2;
                    StringBuilder stringBuilder2;
                    if (totalTimeMs < 100) {
                        if (this.DEBUG_WAKELOCK) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("small time, procName: ");
                            stringBuilder2.append(pwt.mProcName);
                            stringBuilder2.append(", uid: ");
                            stringBuilder2.append(pwt.mUid);
                            stringBuilder2.append(", timeMs: ");
                            stringBuilder2.append(totalTimeMs);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                        list2 = list;
                    } else {
                        if (this.DEBUG_WAKELOCK) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("big time, procName: ");
                            stringBuilder2.append(pwt.mProcName);
                            stringBuilder2.append(", uid: ");
                            stringBuilder2.append(pwt.mUid);
                            stringBuilder2.append(", timeMs: ");
                            stringBuilder2.append(totalTimeMs);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                        sb.append(pwt.mProcName);
                        sb.append('%');
                        sb.append(pwt.mUid);
                        sb.append('%');
                        sb.append(totalTimeMs);
                        list.add(sb.toString());
                    }
                    i--;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
            list2 = list;
        }
    }

    private List<String> getWakeLockStatsList() {
        ProcBatteryStats procBatteryStats = this;
        ArrayList stats = new ArrayList();
        long realtime = SystemClock.elapsedRealtime();
        synchronized (procBatteryStats.mWakeLockTimeMap) {
            int i = procBatteryStats.mWakeLockTimeMap.size() - 1;
            while (i >= 0) {
                ProcWakeLockTime pwt = (ProcWakeLockTime) procBatteryStats.mWakeLockTimeMap.valueAt(i);
                String procName = pwt.mProcName;
                ArrayMap<String, WakeLock> wakeStats = pwt.getWakeLockStats().getMap();
                int iw = wakeStats.size() - 1;
                while (iw >= 0) {
                    String tag = (String) wakeStats.keyAt(iw);
                    long totalTimeMs = ((WakeLock) wakeStats.valueAt(iw)).getTotalTime(realtime) / 1000;
                    if (totalTimeMs > 0) {
                        StringBuilder sb = new StringBuilder(128);
                        sb.append("uid=");
                        sb.append(pwt.mUid);
                        sb.append(" prevent_time=");
                        sb.append(totalTimeMs);
                        sb.append(" ws_name=");
                        sb.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(procName) ? "NULL" : procName);
                        sb.append(" tag=");
                        sb.append(tag);
                        stats.add(sb.toString());
                        if (procBatteryStats.DEBUG_WAKELOCK) {
                            Slog.d(TAG, sb.toString());
                        }
                    }
                    iw--;
                    procBatteryStats = this;
                }
                i--;
                procBatteryStats = this;
            }
        }
        return stats;
    }

    private void removeWlByUid(int uid) {
        synchronized (this.mWakeLockTimeMap) {
            for (int i = this.mWakeLockTimeMap.size() - 1; i >= 0; i--) {
                if (((ProcWakeLockTime) this.mWakeLockTimeMap.valueAt(i)).mUid == uid) {
                    this.mWakeLockTimeMap.removeAt(i);
                }
            }
        }
    }

    private void removeWlByUserId(int userId) {
        synchronized (this.mWakeLockTimeMap) {
            for (int i = this.mWakeLockTimeMap.size() - 1; i >= 0; i--) {
                if (userId == UserHandle.getUserId(((ProcWakeLockTime) this.mWakeLockTimeMap.valueAt(i)).mUid)) {
                    this.mWakeLockTimeMap.removeAt(i);
                }
            }
        }
    }
}

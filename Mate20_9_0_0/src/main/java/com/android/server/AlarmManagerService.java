package com.android.server;

import android.app.ActivityManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.app.IAlarmCompleteListener;
import android.app.IAlarmListener;
import android.app.IAlarmManager;
import android.app.IAlarmManager.Stub;
import android.app.IUidObserver;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.PendingIntent.OnFinished;
import android.app.usage.UsageStatsManagerInternal;
import android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelableException;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.system.Os;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Flog;
import android.util.HwLog;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.LocalLog;
import com.android.internal.util.StatLogger;
import com.android.server.AppStateTracker.Listener;
import com.android.server.am.HwActivityManagerServiceUtil;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.utils.PriorityDump;
import com.huawei.pgmng.log.LogPower;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Predicate;

public class AlarmManagerService extends AbsAlarmManagerService {
    static final int ACTIVE_INDEX = 0;
    static final int ALARM_EVENT = 1;
    static final String BAIDU_PUSHSERVICE_METHOD = "com.baidu.android.pushservice.action.METHOD";
    static final boolean DEBUG_ALARM_CLOCK = false;
    static final boolean DEBUG_BATCH = false;
    static final boolean DEBUG_BG_LIMIT = false;
    static final boolean DEBUG_LISTENER_CALLBACK = false;
    static final boolean DEBUG_STANDBY = false;
    static final boolean DEBUG_TIMEZONE;
    static final boolean DEBUG_VALIDATE = false;
    static final boolean DEBUG_WAKELOCK = false;
    private static final int ELAPSED_REALTIME_MASK = 8;
    private static final int ELAPSED_REALTIME_WAKEUP_MASK = 4;
    static final int FREQUENT_INDEX = 2;
    static final int IS_WAKEUP_MASK = 5;
    static final long MIN_FUZZABLE_INTERVAL = 10000;
    static final int NEVER_INDEX = 4;
    private static final Intent NEXT_ALARM_CLOCK_CHANGED_INTENT = new Intent("android.app.action.NEXT_ALARM_CLOCK_CHANGED").addFlags(553648128);
    static final int PRIO_NORMAL = 2;
    static final int PRIO_TICK = 0;
    static final int PRIO_WAKEUP = 1;
    static final int RARE_INDEX = 3;
    static final boolean RECORD_ALARMS_IN_HISTORY = true;
    static final boolean RECORD_DEVICE_IDLE_ALARMS = false;
    private static final int RTC_MASK = 2;
    private static final int RTC_WAKEUP_MASK = 1;
    private static final String SYSTEM_UI_SELF_PERMISSION = "android.permission.systemui.IDENTITY";
    static final String TAG = "AlarmManager";
    static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    static final int TIME_CHANGED_MASK = 65536;
    static final int TYPE_NONWAKEUP_MASK = 1;
    static final boolean WAKEUP_STATS = false;
    static final int WORKING_INDEX = 1;
    static final boolean localLOGV = false;
    static final BatchTimeOrder sBatchOrder = new BatchTimeOrder();
    static final IncreasingTimeOrder sIncreasingTimeOrder = new IncreasingTimeOrder();
    final long RECENT_WAKEUP_PERIOD = 86400000;
    final ArrayList<Batch> mAlarmBatches = new ArrayList();
    final Comparator<Alarm> mAlarmDispatchComparator = new Comparator<Alarm>() {
        public int compare(Alarm lhs, Alarm rhs) {
            if (lhs.priorityClass.priority < rhs.priorityClass.priority) {
                return -1;
            }
            if (lhs.priorityClass.priority > rhs.priorityClass.priority) {
                return 1;
            }
            if (lhs.whenElapsed < rhs.whenElapsed) {
                return -1;
            }
            if (lhs.whenElapsed > rhs.whenElapsed) {
                return 1;
            }
            return 0;
        }
    };
    final ArrayList<IdleDispatchEntry> mAllowWhileIdleDispatches = new ArrayList();
    AppOpsManager mAppOps;
    private boolean mAppStandbyParole;
    private AppStateTracker mAppStateTracker;
    private final Intent mBackgroundIntent = new Intent().addFlags(4);
    int mBroadcastRefCount = 0;
    final SparseArray<ArrayMap<String, BroadcastStats>> mBroadcastStats = new SparseArray();
    boolean mCancelRemoveAction = false;
    ClockReceiver mClockReceiver;
    final Constants mConstants = new Constants(this.mHandler);
    int mCurrentSeq = 0;
    PendingIntent mDateChangeSender;
    final DeliveryTracker mDeliveryTracker = new DeliveryTracker();
    private final Listener mForceAppStandbyListener = new Listener() {
        public void unblockAllUnrestrictedAlarms() {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendAllUnrestrictedPendingBackgroundAlarmsLocked();
            }
        }

        public void unblockAlarmsForUid(int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(uid, null);
            }
        }

        public void unblockAlarmsForUidPackage(int uid, String packageName) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.sendPendingBackgroundAlarmsLocked(uid, packageName);
            }
        }

        public void onUidForeground(int uid, boolean foreground) {
            synchronized (AlarmManagerService.this.mLock) {
                if (foreground) {
                    AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(uid, true);
                }
            }
        }
    };
    final AlarmHandler mHandler = new AlarmHandler();
    private final SparseArray<AlarmClockInfo> mHandlerSparseAlarmClockArray = new SparseArray();
    Bundle mIdleOptions;
    ArrayList<InFlight> mInFlight = new ArrayList();
    boolean mInteractive = true;
    InteractiveStateReceiver mInteractiveStateReceiver;
    private final boolean mIsAlarmDataOnlyMode = SystemProperties.getBoolean("persist.sys.alarm_data_only", false);
    private boolean mIsScreenOn = true;
    private ArrayMap<Pair<String, Integer>, Long> mLastAlarmDeliveredForPackage = new ArrayMap();
    long mLastAlarmDeliveryTime;
    final SparseLongArray mLastAllowWhileIdleDispatch = new SparseLongArray();
    private long mLastTickAdded;
    private long mLastTickIssued;
    private long mLastTickReceived;
    private long mLastTickRemoved;
    private long mLastTickSet;
    long mLastTimeChangeClockTime;
    long mLastTimeChangeRealtime;
    private long mLastTrigger;
    boolean mLastWakeLockUnimportantForLogging;
    private long mLastWakeup;
    private long mLastWakeupSet;
    @GuardedBy("mLock")
    private int mListenerCount = 0;
    @GuardedBy("mLock")
    private int mListenerFinishCount = 0;
    com.android.server.DeviceIdleController.LocalService mLocalDeviceIdleController;
    protected Object mLock = new Object();
    final LocalLog mLog = new LocalLog(TAG);
    long mMaxDelayTime = 0;
    long mNativeData;
    private final SparseArray<AlarmClockInfo> mNextAlarmClockForUser = new SparseArray();
    protected boolean mNextAlarmClockMayChange;
    private long mNextNonWakeup;
    long mNextNonWakeupDeliveryTime;
    Alarm mNextWakeFromIdle = null;
    private long mNextWakeup;
    long mNonInteractiveStartTime;
    long mNonInteractiveTime;
    int mNumDelayedAlarms = 0;
    int mNumTimeChanged;
    SparseArray<ArrayList<Alarm>> mPendingBackgroundAlarms = new SparseArray();
    Alarm mPendingIdleUntil = null;
    ArrayList<Alarm> mPendingNonWakeupAlarms = new ArrayList();
    private final SparseBooleanArray mPendingSendNextAlarmClockChangedForUser = new SparseBooleanArray();
    ArrayList<Alarm> mPendingWhileIdleAlarms = new ArrayList();
    final HashMap<String, PriorityClass> mPriorities = new HashMap();
    Random mRandom;
    final LinkedList<WakeupEvent> mRecentWakeups = new LinkedList();
    @GuardedBy("mLock")
    private int mSendCount = 0;
    @GuardedBy("mLock")
    private int mSendFinishCount = 0;
    private final IBinder mService = new Stub() {
        public void set(String callingPackage, int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, WorkSource workSource, AlarmClockInfo alarmClock) {
            WorkSource workSource2 = workSource;
            int callingUid = Binder.getCallingUid();
            AlarmManagerService.this.mAppOps.checkPackage(callingUid, callingPackage);
            if (interval == 0 || directReceiver == null) {
                int flags2;
                int flags3;
                if (workSource2 != null) {
                    AlarmManagerService.this.getContext().enforcePermission("android.permission.UPDATE_DEVICE_STATS", Binder.getCallingPid(), callingUid, "AlarmManager.set");
                }
                int flags4 = flags & -11;
                if (callingUid != 1000) {
                    flags4 &= -17;
                }
                if (windowLength == 0) {
                    flags4 |= 1;
                }
                if (alarmClock != null) {
                    flags2 = flags4 | 3;
                } else if ((workSource2 == null && (callingUid < 10000 || UserHandle.isSameApp(callingUid, AlarmManagerService.this.mSystemUiUid) || (AlarmManagerService.this.mAppStateTracker != null && AlarmManagerService.this.mAppStateTracker.isUidPowerSaveUserWhitelisted(callingUid)))) || AlarmManagerService.this.isContainsAppUidInWorksource(workSource2, "com.android.email") || AlarmManagerService.this.isContainsAppUidInWorksource(workSource2, "com.android.exchange") || AlarmManagerService.this.isContainsAppUidInWorksource(workSource2, "com.google.android.gm")) {
                    flags2 = (flags4 | 8) & -5;
                } else {
                    flags3 = flags4;
                    AlarmManagerService.this.setImpl(type, triggerAtTime, windowLength, interval, operation, directReceiver, listenerTag, flags3, workSource2, alarmClock, callingUid, callingPackage);
                    return;
                }
                flags3 = flags2;
                AlarmManagerService.this.setImpl(type, triggerAtTime, windowLength, interval, operation, directReceiver, listenerTag, flags3, workSource2, alarmClock, callingUid, callingPackage);
                return;
            }
            throw new IllegalArgumentException("Repeating alarms cannot use AlarmReceivers");
        }

        public boolean setTime(long millis) {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setTime");
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setTime,uid is ");
            stringBuilder.append(uid);
            stringBuilder.append(",pid is ");
            stringBuilder.append(pid);
            Flog.i(500, stringBuilder.toString());
            return AlarmManagerService.this.setTimeImpl(millis);
        }

        public void setTimeZone(String tz) {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME_ZONE", "setTimeZone");
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setTimeZoneImpl,uid is ");
            stringBuilder.append(uid);
            stringBuilder.append(",pid is ");
            stringBuilder.append(pid);
            stringBuilder.append(",tz = ");
            stringBuilder.append(tz);
            Flog.i(500, stringBuilder.toString());
            long oldId = Binder.clearCallingIdentity();
            try {
                AlarmManagerService.this.setTimeZoneImpl(tz);
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }

        public void remove(PendingIntent operation, IAlarmListener listener) {
            if (operation == null && listener == null) {
                Slog.w(AlarmManagerService.TAG, "remove() with no intent or listener");
                return;
            }
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeLocked(operation, listener);
            }
        }

        public long getNextWakeFromIdleTime() {
            return AlarmManagerService.this.getNextWakeFromIdleTimeImpl();
        }

        public AlarmClockInfo getNextAlarmClock(int userId) {
            return AlarmManagerService.this.getNextAlarmClockImpl(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "getNextAlarmClock", null));
        }

        public long currentNetworkTimeMillis() {
            NtpTrustedTime time = NtpTrustedTime.getInstance(AlarmManagerService.this.getContext());
            if (time.hasCache()) {
                return time.currentTimeMillis();
            }
            throw new ParcelableException(new DateTimeException("Missing NTP fix"));
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(AlarmManagerService.this.getContext(), AlarmManagerService.TAG, pw)) {
                if (args.length <= 0 || !PriorityDump.PROTO_ARG.equals(args[0])) {
                    AlarmManagerService.this.dumpImpl(pw);
                } else {
                    AlarmManagerService.this.dumpProto(fd);
                }
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new ShellCmd(AlarmManagerService.this, null).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public void updateBlockedUids(int uid, boolean isBlocked) {
        }

        public int getWakeUpNum(int uid, String pkg) {
            if (1000 == Binder.getCallingUid()) {
                return AlarmManagerService.this.getWakeUpNumImpl(uid, pkg);
            }
            Slog.i(AlarmManagerService.TAG, "getWakeUpNum: permission not allowed.");
            return 0;
        }

        public long checkHasHwRTCAlarm(String packageName) {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "checkHasHwRTCAlarm");
            return AlarmManagerService.this.checkHasHwRTCAlarmLock(packageName);
        }

        public void adjustHwRTCAlarm(boolean deskClockTime, boolean bootOnTime, int typeState) {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "adjustHwRTCAlarm");
            Slog.i(AlarmManagerService.TAG, "adjustHwRTCAlarm : adjust RTC alarm");
            AlarmManagerService.this.adjustHwRTCAlarmLock(deskClockTime, bootOnTime, typeState);
        }

        public void setHwAirPlaneStateProp() {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setHwAirPlaneStateProp");
            Slog.i(AlarmManagerService.TAG, "setHwAirPlaneStateProp : set Prop Lock");
            AlarmManagerService.this.setHwAirPlaneStatePropLock();
        }

        public void setHwRTCAlarm() {
            AlarmManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_TIME", "setHwRTCAlarm");
            Slog.i(AlarmManagerService.TAG, "setHwRTCAlarm : set RTC alarm");
            AlarmManagerService.this.setHwRTCAlarmLock();
        }
    };
    long mStartCurrentDelayTime;
    private final StatLogger mStatLogger = new StatLogger(new String[]{"REBATCH_ALL_ALARMS", "REORDER_ALARMS_FOR_STANDBY"});
    int mSystemUiUid;
    PendingIntent mTimeTickSender;
    private final SparseArray<AlarmClockInfo> mTmpSparseAlarmClockArray = new SparseArray();
    long mTotalDelayTime = 0;
    private UninstallReceiver mUninstallReceiver;
    private UsageStatsManagerInternal mUsageStatsManagerInternal;
    final SparseBooleanArray mUseAllowWhileIdleShortTime = new SparseBooleanArray();
    WakeLock mWakeLock;

    @VisibleForTesting
    public static class Alarm {
        public final AlarmClockInfo alarmClock;
        public int count;
        public final int creatorUid;
        public long expectedMaxWhenElapsed;
        public long expectedWhenElapsed;
        public final int flags;
        public final IAlarmListener listener;
        public final String listenerTag;
        public long maxWhenElapsed;
        public final PendingIntent operation;
        public final long origWhen;
        public final String packageName;
        public PriorityClass priorityClass;
        public long repeatInterval;
        public final String sourcePackage;
        public final String statsTag;
        public final int type;
        public final int uid;
        public boolean wakeup;
        public long when;
        public long whenElapsed;
        public long windowLength;
        public final WorkSource workSource;

        public Alarm(int _type, long _when, long _whenElapsed, long _windowLength, long _maxWhen, long _interval, PendingIntent _op, IAlarmListener _rec, String _listenerTag, WorkSource _ws, int _flags, AlarmClockInfo _info, int _uid, String _pkgName) {
            int i = _type;
            long j = _when;
            long j2 = _whenElapsed;
            PendingIntent pendingIntent = _op;
            String str = _listenerTag;
            this.type = i;
            this.origWhen = j;
            boolean z = i == 2 || i == 0;
            this.wakeup = z;
            this.when = j;
            this.whenElapsed = j2;
            this.expectedWhenElapsed = j2;
            this.windowLength = _windowLength;
            long clampPositive = AlarmManagerService.clampPositive(_maxWhen);
            this.expectedMaxWhenElapsed = clampPositive;
            this.maxWhenElapsed = clampPositive;
            this.repeatInterval = _interval;
            this.operation = pendingIntent;
            this.listener = _rec;
            this.listenerTag = str;
            this.statsTag = makeTag(pendingIntent, str, i);
            this.workSource = _ws;
            this.flags = _flags;
            this.alarmClock = _info;
            this.uid = _uid;
            this.packageName = _pkgName;
            this.sourcePackage = this.operation != null ? this.operation.getCreatorPackage() : this.packageName;
            this.creatorUid = this.operation != null ? this.operation.getCreatorUid() : this.uid;
        }

        public static String makeTag(PendingIntent pi, String tag, int type) {
            String alarmString = (type == 2 || type == 0) ? "*walarm*:" : "*alarm*:";
            if (pi != null) {
                return pi.getTag(alarmString);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(alarmString);
            stringBuilder.append(tag);
            return stringBuilder.toString();
        }

        public WakeupEvent makeWakeupEvent(long nowRTC) {
            String access$800;
            int i = this.creatorUid;
            if (this.operation != null) {
                access$800 = AlarmManagerService.resetActionCallingIdentity(this.operation);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<listener>:");
                stringBuilder.append(this.listenerTag);
                access$800 = stringBuilder.toString();
            }
            return new WakeupEvent(nowRTC, i, access$800);
        }

        public boolean matches(PendingIntent pi, IAlarmListener rec) {
            if (this.operation != null) {
                return this.operation.equals(pi);
            }
            return rec != null && this.listener.asBinder().equals(rec.asBinder());
        }

        public boolean matches(String packageName) {
            return packageName.equals(this.sourcePackage);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Alarm{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" type ");
            sb.append(this.type);
            sb.append(" when ");
            sb.append(this.when);
            sb.append(" ");
            if (this.operation != null) {
                sb.append(this.operation);
            } else {
                sb.append(this.packageName);
            }
            sb.append(this.sourcePackage);
            sb.append('}');
            return sb.toString();
        }

        public void dump(PrintWriter pw, String prefix, long nowELAPSED, long nowRTC, SimpleDateFormat sdf) {
            boolean z = true;
            if (!(this.type == 1 || this.type == 0)) {
                z = false;
            }
            boolean isRtc = z;
            pw.print(prefix);
            pw.print("tag=");
            pw.println(this.statsTag);
            pw.print(prefix);
            pw.print("type=");
            pw.print(this.type);
            pw.print(prefix);
            pw.print("wakeup=");
            pw.print(this.wakeup);
            pw.print(" expectedWhenElapsed=");
            TimeUtils.formatDuration(this.expectedWhenElapsed, nowELAPSED, pw);
            pw.print(" expectedMaxWhenElapsed=");
            TimeUtils.formatDuration(this.expectedMaxWhenElapsed, nowELAPSED, pw);
            pw.print(" whenElapsed=");
            TimeUtils.formatDuration(this.whenElapsed, nowELAPSED, pw);
            pw.print(" maxWhenElapsed=");
            TimeUtils.formatDuration(this.maxWhenElapsed, nowELAPSED, pw);
            pw.print(" when=");
            if (isRtc) {
                pw.print(sdf.format(new Date(this.when)));
            } else {
                TimeUtils.formatDuration(this.when, nowELAPSED, pw);
            }
            pw.println();
            pw.print(prefix);
            pw.print("window=");
            TimeUtils.formatDuration(this.windowLength, pw);
            pw.print(" repeatInterval=");
            pw.print(this.repeatInterval);
            pw.print(" count=");
            pw.print(this.count);
            pw.print(" flags=0x");
            pw.println(Integer.toHexString(this.flags));
            if (this.alarmClock != null) {
                pw.print(prefix);
                pw.println("Alarm clock:");
                pw.print(prefix);
                pw.print("  triggerTime=");
                pw.println(sdf.format(new Date(this.alarmClock.getTriggerTime())));
                pw.print(prefix);
                pw.print("  showIntent=");
                pw.println(this.alarmClock.getShowIntent());
            }
            pw.print(prefix);
            pw.print("operation=");
            pw.println(this.operation);
            if (this.listener != null) {
                pw.print(prefix);
                pw.print("listener=");
                pw.println(this.listener.asBinder());
            }
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId, long nowElapsed, long nowRTC) {
            long token = proto.start(fieldId);
            proto.write(1138166333441L, this.statsTag);
            proto.write(1159641169922L, this.type);
            proto.write(1112396529667L, this.whenElapsed - nowElapsed);
            proto.write(1112396529668L, this.windowLength);
            proto.write(1112396529669L, this.repeatInterval);
            proto.write(1120986464262L, this.count);
            proto.write(1120986464263L, this.flags);
            if (this.alarmClock != null) {
                this.alarmClock.writeToProto(proto, 1146756268040L);
            }
            if (this.operation != null) {
                this.operation.writeToProto(proto, 1146756268041L);
            }
            if (this.listener != null) {
                proto.write(1138166333450L, this.listener.asBinder().toString());
            }
            proto.end(token);
        }
    }

    public class AlarmHandler extends Handler {
        public static final int ALARM_EVENT = 1;
        public static final int APP_STANDBY_BUCKET_CHANGED = 5;
        public static final int APP_STANDBY_PAROLE_CHANGED = 6;
        public static final int CLEAR_BAIDU_PUSHSERVICE = 100;
        public static final int LISTENER_TIMEOUT = 3;
        public static final int REMOVE_FOR_STOPPED = 7;
        public static final int REPORT_ALARMS_ACTIVE = 4;
        public static final int SEND_NEXT_ALARM_CLOCK_CHANGED = 2;

        public void postRemoveForStopped(int uid) {
            obtainMessage(7, uid, 0).sendToTarget();
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            int i2 = 0;
            if (i != 100) {
                switch (i) {
                    case 1:
                        ArrayList<Alarm> triggerList = new ArrayList();
                        synchronized (AlarmManagerService.this.mLock) {
                            long nowRTC = System.currentTimeMillis();
                            AlarmManagerService.this.triggerAlarmsLocked(triggerList, SystemClock.elapsedRealtime(), nowRTC);
                            AlarmManagerService.this.updateNextAlarmClockLocked();
                        }
                        while (true) {
                            int i3 = i2;
                            if (i3 < triggerList.size()) {
                                Alarm alarm = (Alarm) triggerList.get(i3);
                                try {
                                    if (alarm.operation != null) {
                                        alarm.operation.send();
                                    }
                                } catch (CanceledException e) {
                                    if (alarm.repeatInterval > 0) {
                                        AlarmManagerService.this.removeImpl(alarm.operation);
                                    }
                                }
                                i2 = i3 + 1;
                            } else {
                                return;
                            }
                        }
                    case 2:
                        AlarmManagerService.this.sendNextAlarmClockChanged();
                        return;
                    case 3:
                        AlarmManagerService.this.mDeliveryTracker.alarmTimedOut((IBinder) msg.obj);
                        return;
                    case 4:
                        if (AlarmManagerService.this.mLocalDeviceIdleController != null) {
                            boolean i4;
                            com.android.server.DeviceIdleController.LocalService localService = AlarmManagerService.this.mLocalDeviceIdleController;
                            if (msg.arg1 != 0) {
                                i4 = true;
                            }
                            localService.setAlarmsActive(i4);
                            return;
                        }
                        return;
                    case 5:
                        synchronized (AlarmManagerService.this.mLock) {
                            ArraySet<Pair<String, Integer>> filterPackages = new ArraySet();
                            filterPackages.add(Pair.create((String) msg.obj, Integer.valueOf(msg.arg1)));
                            if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(filterPackages)) {
                                AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                                AlarmManagerService.this.updateNextAlarmClockLocked();
                            }
                        }
                        return;
                    case 6:
                        synchronized (AlarmManagerService.this.mLock) {
                            AlarmManagerService.this.mAppStandbyParole = ((Boolean) msg.obj).booleanValue();
                            if (AlarmManagerService.this.reorderAlarmsBasedOnStandbyBuckets(null)) {
                                AlarmManagerService.this.rescheduleKernelAlarmsLocked();
                                AlarmManagerService.this.updateNextAlarmClockLocked();
                            }
                        }
                        return;
                    case 7:
                        synchronized (AlarmManagerService.this.mLock) {
                            AlarmManagerService.this.removeForStoppedLocked(msg.arg1);
                        }
                        return;
                    default:
                        return;
                }
            }
            ArrayList<String> array = msg.getData().getStringArrayList("clearlist");
            if (array != null && array.size() > 0) {
                while (i2 < array.size()) {
                    AlarmManagerService.this.removeImpl((String) array.get(i2), AlarmManagerService.BAIDU_PUSHSERVICE_METHOD);
                    i2++;
                }
            }
        }
    }

    private class AlarmThread extends Thread {
        /*  JADX ERROR: NullPointerException in pass: BlockFinish
            java.lang.NullPointerException
            	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
            	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
            	at java.util.ArrayList.forEach(ArrayList.java:1249)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
            	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
            	at java.util.ArrayList.forEach(ArrayList.java:1249)
            	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
            	at jadx.core.ProcessClass.process(ProcessClass.java:32)
            	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
            	at java.lang.Iterable.forEach(Iterable.java:75)
            	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
            	at jadx.core.ProcessClass.process(ProcessClass.java:37)
            	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
            	at jadx.api.JavaClass.decompile(JavaClass.java:62)
            	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
            */
        public void run() {
            /*
            r23 = this;
            r1 = r23;
            r0 = new java.util.ArrayList;
            r0.<init>();
            r8 = r0;
            r0 = com.android.server.AlarmManagerService.this;
            r0.hwRecordFirstTime();
        L_0x000d:
            r0 = com.android.server.AlarmManagerService.this;
            r2 = com.android.server.AlarmManagerService.this;
            r2 = r2.mNativeData;
            r2 = r0.waitForAlarm(r2);
            r6 = java.lang.System.currentTimeMillis();
            r4 = android.os.SystemClock.elapsedRealtime();
            r0 = com.android.server.AlarmManagerService.this;
            r3 = r0.mLock;
            monitor-enter(r3);
            r0 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01fb }
            r0.mLastWakeup = r4;	 Catch:{ all -> 0x01fb }
            monitor-exit(r3);	 Catch:{ all -> 0x01fb }
            r8.clear();
            r0 = 65536; // 0x10000 float:9.18355E-41 double:3.2379E-319;
            r3 = r2 & r0;
            r14 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
            if (r3 == 0) goto L_0x00c8;
        L_0x0035:
            r3 = com.android.server.AlarmManagerService.this;
            r3 = r3.mLock;
            monitor-enter(r3);
            r9 = com.android.server.AlarmManagerService.this;
            r9 = r9.mLastTimeChangeClockTime;
            r18 = r9;
            r9 = com.android.server.AlarmManagerService.this;
            r9 = r9.mLastTimeChangeRealtime;
            r9 = r4 - r9;
            r20 = r18 + r9;
            monitor-exit(r3);
            r9 = 0;
            r3 = (r18 > r9 ? 1 : (r18 == r9 ? 0 : -1));
            if (r3 == 0) goto L_0x005d;
        L_0x004f:
            r9 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            r11 = r20 - r9;
            r3 = (r6 > r11 ? 1 : (r6 == r11 ? 0 : -1));
            if (r3 < 0) goto L_0x005d;
        L_0x0057:
            r9 = r20 + r9;
            r3 = (r6 > r9 ? 1 : (r6 == r9 ? 0 : -1));
            if (r3 <= 0) goto L_0x00c8;
        L_0x005d:
            r3 = "Time changed notification from kernel; rebatching";
            android.util.Flog.i(r14, r3);
            r3 = com.android.server.AlarmManagerService.this;
            r9 = com.android.server.AlarmManagerService.this;
            r9 = r9.mTimeTickSender;
            r3.removeImpl(r9);
            r3 = com.android.server.AlarmManagerService.this;
            r9 = com.android.server.AlarmManagerService.this;
            r9 = r9.mDateChangeSender;
            r3.removeImpl(r9);
            r3 = com.android.server.AlarmManagerService.this;
            r3.rebatchAllAlarms();
            r3 = com.android.server.AlarmManagerService.this;
            r3 = r3.mClockReceiver;
            r3.scheduleTimeTickEvent();
            r3 = com.android.server.AlarmManagerService.this;
            r3 = r3.mClockReceiver;
            r3.scheduleDateChangedEvent();
            r3 = com.android.server.AlarmManagerService.this;
            r9 = r3.mLock;
            monitor-enter(r9);
            r3 = com.android.server.AlarmManagerService.this;
            r10 = r3.mNumTimeChanged;
            r10 = r10 + 1;
            r3.mNumTimeChanged = r10;
            r3 = com.android.server.AlarmManagerService.this;
            r3.mLastTimeChangeClockTime = r6;
            r3 = com.android.server.AlarmManagerService.this;
            r3.mLastTimeChangeRealtime = r4;
            monitor-exit(r9);
            r3 = new android.content.Intent;
            r9 = "android.intent.action.TIME_SET";
            r3.<init>(r9);
            r9 = 891289600; // 0x35200000 float:5.9604645E-7 double:4.40355572E-315;
            r3.addFlags(r9);
            r9 = com.android.server.AlarmManagerService.this;
            r9 = r9.getContext();
            r10 = android.os.UserHandle.ALL;
            r9.sendBroadcastAsUser(r3, r10);
            r9 = com.android.server.AlarmManagerService.this;
            r10 = r6;
            r12 = r4;
            r14 = r18;
            r16 = r20;
            r9.hwRecordTimeChangeRTC(r10, r12, r14, r16);
            r2 = r2 | 5;
            goto L_0x00c8;
        L_0x00c2:
            r0 = move-exception;
            monitor-exit(r9);
            throw r0;
        L_0x00c5:
            r0 = move-exception;
            monitor-exit(r3);
            throw r0;
        L_0x00c8:
            r9 = r2;
            if (r9 == r0) goto L_0x01e9;
        L_0x00cb:
            r0 = com.android.server.AlarmManagerService.this;
            r10 = r0.mLock;
            monitor-enter(r10);
            r0 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r0.mLastTrigger = r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r8;
            r11 = r4;
            r13 = r6;
            r0 = r2.triggerAlarmsLocked(r3, r4, r6);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r0 != 0) goto L_0x012d;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x00e0:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.checkAllowNonWakeupDelayLocked(r11);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 == 0) goto L_0x012d;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x00e8:
            r2 = "there are no wakeup alarms and the screen is off, we can delay what we have so far until the future";	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            android.util.Flog.i(r3, r2);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.size();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 != 0) goto L_0x010f;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x00fa:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.mStartCurrentDelayTime = r11;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3.currentNonWakeupFuzzLocked(r11);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = 3;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3 * r5;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = 2;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3 / r5;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3 + r11;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.mNextNonWakeupDeliveryTime = r3;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x010f:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.addAll(r8);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r2.mNumDelayedAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r4 = r8.size();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3 + r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.mNumDelayedAlarms = r3;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.rescheduleKernelAlarmsLocked();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.updateNextAlarmClockLocked();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            goto L_0x01e0;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x012d:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.isAwareAlarmManagerEnabled();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 == 0) goto L_0x015b;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0135:
            r2 = r9 & 5;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 == 0) goto L_0x015b;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0139:
            r2 = new java.util.ArrayList;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.<init>();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r8.iterator();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0142:
            r4 = r3.hasNext();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r4 == 0) goto L_0x0156;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0148:
            r4 = r3.next();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r4 = (com.android.server.AlarmManagerService.Alarm) r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = r4.wakeup;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r5 == 0) goto L_0x0155;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0152:
            r2.add(r4);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0155:
            goto L_0x0142;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0156:
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3.reportWakeupAlarms(r2);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x015b:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.size();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 <= 0) goto L_0x019c;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0165:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = r3.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.calculateDeliveryPriorities(r3);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r8.addAll(r2);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mAlarmDispatchComparator;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            java.util.Collections.sort(r8, r2);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mStartCurrentDelayTime;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r4 = r11 - r2;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r6 = r2.mTotalDelayTime;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r6 = r6 + r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.mTotalDelayTime = r6;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mMaxDelayTime;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r2 >= 0) goto L_0x0195;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0191:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.mMaxDelayTime = r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x0195:
            r2 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2 = r2.mPendingNonWakeupAlarms;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.clear();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x019c:
            r2 = new android.util.ArraySet;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.<init>();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = 0;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01a2:
            r4 = r8.size();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r3 >= r4) goto L_0x01cc;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01a8:
            r4 = r8.get(r3);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r4 = (com.android.server.AlarmManagerService.Alarm) r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = r5.isExemptFromAppStandby(r4);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            if (r5 != 0) goto L_0x01c9;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01b6:
            r5 = r4.sourcePackage;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r6 = r4.creatorUid;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r6 = android.os.UserHandle.getUserId(r6);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r6 = java.lang.Integer.valueOf(r6);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r5 = android.util.Pair.create(r5, r6);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r2.add(r5);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01c9:
            r3 = r3 + 1;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            goto L_0x01a2;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01cc:
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3.deliverAlarmsLocked(r8, r11);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3.reorderAlarmsBasedOnStandbyBuckets(r2);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3.rescheduleKernelAlarmsLocked();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3 = com.android.server.AlarmManagerService.this;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r3.updateNextAlarmClockLocked();	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01e0:
            monitor-exit(r10);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            goto L_0x01f6;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01e2:
            r0 = move-exception;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r11 = r4;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            r13 = r6;	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
        L_0x01e5:
            monitor-exit(r10);	 Catch:{ all -> 0x01e2, all -> 0x01e7 }
            throw r0;
        L_0x01e7:
            r0 = move-exception;
            goto L_0x01e5;
        L_0x01e9:
            r11 = r4;
            r13 = r6;
            r0 = com.android.server.AlarmManagerService.this;
            r4 = r0.mLock;
            monitor-enter(r4);
            r0 = com.android.server.AlarmManagerService.this;
            r0.rescheduleKernelAlarmsLocked();
            monitor-exit(r4);
        L_0x01f6:
            goto L_0x000d;
        L_0x01f8:
            r0 = move-exception;
            monitor-exit(r4);
            throw r0;
        L_0x01fb:
            r0 = move-exception;
            r11 = r4;
            r13 = r6;
        L_0x01fe:
            monitor-exit(r3);	 Catch:{ all -> 0x0200 }
            throw r0;
        L_0x0200:
            r0 = move-exception;
            goto L_0x01fe;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.AlarmManagerService.AlarmThread.run():void");
        }

        public AlarmThread() {
            super(AlarmManagerService.TAG);
        }
    }

    final class AppStandbyTracker extends AppIdleStateChangeListener {
        AppStandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            AlarmManagerService.this.mHandler.removeMessages(5);
            AlarmManagerService.this.mHandler.obtainMessage(5, userId, -1, packageName).sendToTarget();
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            AlarmManagerService.this.mHandler.removeMessages(5);
            AlarmManagerService.this.mHandler.removeMessages(6);
            AlarmManagerService.this.mHandler.obtainMessage(6, Boolean.valueOf(isParoleOn)).sendToTarget();
        }
    }

    public final class Batch {
        public final ArrayList<Alarm> alarms;
        long end;
        int flags;
        boolean standalone;
        long start;

        Batch() {
            this.alarms = new ArrayList();
            this.start = 0;
            this.end = JobStatus.NO_LATEST_RUNTIME;
            this.flags = 0;
        }

        Batch(Alarm seed) {
            this.alarms = new ArrayList();
            this.start = seed.whenElapsed;
            this.end = AlarmManagerService.clampPositive(seed.maxWhenElapsed);
            this.flags = seed.flags;
            this.alarms.add(seed);
            if (seed.operation == AlarmManagerService.this.mTimeTickSender) {
                AlarmManagerService.this.mLastTickAdded = System.currentTimeMillis();
            }
        }

        int size() {
            return this.alarms.size();
        }

        Alarm get(int index) {
            return (Alarm) this.alarms.get(index);
        }

        boolean canHold(long whenElapsed, long maxWhen) {
            return this.end >= whenElapsed && this.start <= maxWhen;
        }

        boolean add(Alarm alarm) {
            boolean newStart = false;
            int index = Collections.binarySearch(this.alarms, alarm, AlarmManagerService.sIncreasingTimeOrder);
            if (index < 0) {
                index = (0 - index) - 1;
            }
            this.alarms.add(index, alarm);
            if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                AlarmManagerService.this.mLastTickAdded = System.currentTimeMillis();
            }
            if (alarm.whenElapsed > this.start) {
                this.start = alarm.whenElapsed;
                newStart = true;
            }
            if (alarm.maxWhenElapsed < this.end) {
                this.end = alarm.maxWhenElapsed;
            }
            this.flags |= alarm.flags;
            return newStart;
        }

        static /* synthetic */ boolean lambda$remove$0(Alarm alarm, Alarm a) {
            return a == alarm;
        }

        boolean remove(Alarm alarm) {
            return remove(new -$$Lambda$AlarmManagerService$Batch$Xltkj5RTKUMuFVeuavpuY7-Ogzc(alarm));
        }

        boolean remove(Predicate<Alarm> predicate) {
            boolean didRemove = false;
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int newFlags = 0;
            int i = 0;
            while (i < this.alarms.size()) {
                Alarm alarm = (Alarm) this.alarms.get(i);
                if (predicate.test(alarm)) {
                    this.alarms.remove(i);
                    didRemove = true;
                    AlarmManagerService.this.hwRemoveRtcAlarm(alarm, true);
                    if (alarm.alarmClock != null) {
                        AlarmManagerService.this.mNextAlarmClockMayChange = true;
                    }
                    if (alarm.operation == AlarmManagerService.this.mTimeTickSender) {
                        AlarmManagerService.this.mLastTickRemoved = System.currentTimeMillis();
                    }
                } else {
                    if (alarm.whenElapsed > newStart) {
                        newStart = alarm.whenElapsed;
                    }
                    if (alarm.maxWhenElapsed < newEnd) {
                        newEnd = alarm.maxWhenElapsed;
                    }
                    newFlags |= alarm.flags;
                    i++;
                }
            }
            if (didRemove) {
                this.start = newStart;
                this.end = newEnd;
                this.flags = newFlags;
            }
            return didRemove;
        }

        boolean hasPackage(String packageName) {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                if (((Alarm) this.alarms.get(i)).matches(packageName)) {
                    return true;
                }
            }
            return false;
        }

        boolean hasWakeups() {
            int N = this.alarms.size();
            for (int i = 0; i < N; i++) {
                if ((((Alarm) this.alarms.get(i)).type & 1) == 0) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder b = new StringBuilder(40);
            b.append("Batch{");
            b.append(Integer.toHexString(hashCode()));
            b.append(" num=");
            b.append(size());
            b.append(" start=");
            b.append(this.start);
            b.append(" end=");
            b.append(this.end);
            if (this.flags != 0) {
                b.append(" flgs=0x");
                b.append(Integer.toHexString(this.flags));
            }
            b.append('}');
            return b.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId, long nowElapsed, long nowRTC) {
            ProtoOutputStream protoOutputStream = proto;
            long token = proto.start(fieldId);
            protoOutputStream.write(1112396529665L, this.start);
            protoOutputStream.write(1112396529666L, this.end);
            protoOutputStream.write(1120986464259L, this.flags);
            Iterator it = this.alarms.iterator();
            while (it.hasNext()) {
                ((Alarm) it.next()).writeToProto(protoOutputStream, 2246267895812L, nowElapsed, nowRTC);
            }
            protoOutputStream.end(token);
        }
    }

    static class BatchTimeOrder implements Comparator<Batch> {
        BatchTimeOrder() {
        }

        public int compare(Batch b1, Batch b2) {
            if (!(b1 == null || b2 == null)) {
                long when1 = b1.start;
                long when2 = b2.start;
                if (when1 > when2) {
                    return 1;
                }
                if (when1 < when2) {
                    return -1;
                }
            }
            return 0;
        }
    }

    static final class BroadcastStats {
        long aggregateTime;
        int count;
        final ArrayMap<String, FilterStats> filterStats = new ArrayMap();
        final String mPackageName;
        final int mUid;
        int nesting;
        int numWakeup;
        long startTime;

        BroadcastStats(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BroadcastStats{uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(", packageName=");
            stringBuilder.append(this.mPackageName);
            stringBuilder.append(", aggregateTime=");
            stringBuilder.append(this.aggregateTime);
            stringBuilder.append(", count=");
            stringBuilder.append(this.count);
            stringBuilder.append(", numWakeup=");
            stringBuilder.append(this.numWakeup);
            stringBuilder.append(", startTime=");
            stringBuilder.append(this.startTime);
            stringBuilder.append(", nesting=");
            stringBuilder.append(this.nesting);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1120986464257L, this.mUid);
            proto.write(1138166333442L, this.mPackageName);
            proto.write(1112396529667L, this.aggregateTime);
            proto.write(1120986464260L, this.count);
            proto.write(1120986464261L, this.numWakeup);
            proto.write(1112396529670L, this.startTime);
            proto.write(1120986464263L, this.nesting);
            proto.end(token);
        }
    }

    class ClockReceiver extends BroadcastReceiver {
        public ClockReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.DATE_CHANGED");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                Slog.v(AlarmManagerService.TAG, "Received TIME_TICK alarm; rescheduling");
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mLastTickReceived = System.currentTimeMillis();
                }
                scheduleTimeTickEvent();
            } else if (intent.getAction().equals("android.intent.action.DATE_CHANGED")) {
                Flog.i(500, "Received DATE_CHANGED alarm; rescheduling");
                AlarmManagerService.this.setKernelTimezone(AlarmManagerService.this.mNativeData, -(TimeZone.getTimeZone(SystemProperties.get(AlarmManagerService.TIMEZONE_PROPERTY)).getOffset(System.currentTimeMillis()) / 60000));
                scheduleDateChangedEvent();
            }
        }

        public void scheduleTimeTickEvent() {
            long currentTime = System.currentTimeMillis();
            long triggerAtTime = SystemClock.elapsedRealtime() + ((60000 * ((currentTime / 60000) + 1)) - currentTime);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleTimeTickEvent triggerAtTime = ");
            stringBuilder.append(triggerAtTime);
            Flog.i(500, stringBuilder.toString());
            AlarmManagerService.this.setImpl(3, triggerAtTime, 0, 0, AlarmManagerService.this.mTimeTickSender, null, "time_tick", 1, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME);
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mLastTickSet = currentTime;
            }
        }

        public void scheduleDateChangedEvent() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(11, 0);
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.set(14, 0);
            calendar.add(5, 1);
            AlarmManagerService.this.setImpl(1, calendar.getTimeInMillis(), 0, 0, AlarmManagerService.this.mDateChangeSender, null, null, 1, null, null, Process.myUid(), PackageManagerService.PLATFORM_PACKAGE_NAME);
        }
    }

    private final class Constants extends ContentObserver {
        private static final long DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME = 540000;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_SHORT_TIME = 5000;
        private static final long DEFAULT_ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000;
        private static final long DEFAULT_LISTENER_TIMEOUT = 5000;
        private static final long DEFAULT_MAX_INTERVAL = 31536000000L;
        private static final long DEFAULT_MIN_FUTURITY = 5000;
        private static final long DEFAULT_MIN_INTERVAL = 60000;
        private static final String KEY_ALLOW_WHILE_IDLE_LONG_TIME = "allow_while_idle_long_time";
        private static final String KEY_ALLOW_WHILE_IDLE_SHORT_TIME = "allow_while_idle_short_time";
        private static final String KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION = "allow_while_idle_whitelist_duration";
        private static final String KEY_LISTENER_TIMEOUT = "listener_timeout";
        private static final String KEY_MAX_INTERVAL = "max_interval";
        private static final String KEY_MIN_FUTURITY = "min_futurity";
        private static final String KEY_MIN_INTERVAL = "min_interval";
        public long ALLOW_WHILE_IDLE_LONG_TIME = DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME;
        public long ALLOW_WHILE_IDLE_SHORT_TIME = 5000;
        public long ALLOW_WHILE_IDLE_WHITELIST_DURATION = 10000;
        public long[] APP_STANDBY_MIN_DELAYS = new long[this.DEFAULT_APP_STANDBY_DELAYS.length];
        private final long[] DEFAULT_APP_STANDBY_DELAYS = new long[]{0, 360000, 1800000, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, 864000000};
        private final String[] KEYS_APP_STANDBY_DELAY = new String[]{"standby_active_delay", "standby_working_delay", "standby_frequent_delay", "standby_rare_delay", "standby_never_delay"};
        public long LISTENER_TIMEOUT = 5000;
        public long MAX_INTERVAL = 31536000000L;
        public long MIN_FUTURITY = 5000;
        public long MIN_INTERVAL = 60000;
        private long mLastAllowWhileIdleWhitelistDuration = -1;
        private final KeyValueListParser mParser = new KeyValueListParser(',');
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
            updateAllowWhileIdleWhitelistDurationLocked();
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Global.getUriFor("alarm_manager_constants"), false, this);
            updateConstants();
        }

        public void updateAllowWhileIdleWhitelistDurationLocked() {
            if (this.mLastAllowWhileIdleWhitelistDuration != this.ALLOW_WHILE_IDLE_WHITELIST_DURATION) {
                this.mLastAllowWhileIdleWhitelistDuration = this.ALLOW_WHILE_IDLE_WHITELIST_DURATION;
                BroadcastOptions opts = BroadcastOptions.makeBasic();
                opts.setTemporaryAppWhitelistDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
                AlarmManagerService.this.mIdleOptions = opts.toBundle();
            }
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (AlarmManagerService.this.mLock) {
                try {
                    this.mParser.setString(Global.getString(this.mResolver, "alarm_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(AlarmManagerService.TAG, "Bad alarm manager settings", e);
                }
                this.MIN_FUTURITY = this.mParser.getLong(KEY_MIN_FUTURITY, 5000);
                this.MIN_INTERVAL = this.mParser.getLong(KEY_MIN_INTERVAL, 60000);
                this.MAX_INTERVAL = this.mParser.getLong(KEY_MAX_INTERVAL, 31536000000L);
                this.ALLOW_WHILE_IDLE_SHORT_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_SHORT_TIME, 5000);
                this.ALLOW_WHILE_IDLE_LONG_TIME = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_LONG_TIME, DEFAULT_ALLOW_WHILE_IDLE_LONG_TIME);
                this.ALLOW_WHILE_IDLE_WHITELIST_DURATION = this.mParser.getLong(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION, 10000);
                this.LISTENER_TIMEOUT = this.mParser.getLong(KEY_LISTENER_TIMEOUT, 5000);
                this.APP_STANDBY_MIN_DELAYS[0] = this.mParser.getDurationMillis(this.KEYS_APP_STANDBY_DELAY[0], this.DEFAULT_APP_STANDBY_DELAYS[0]);
                for (int i = 1; i < this.KEYS_APP_STANDBY_DELAY.length; i++) {
                    this.APP_STANDBY_MIN_DELAYS[i] = this.mParser.getDurationMillis(this.KEYS_APP_STANDBY_DELAY[i], Math.max(this.APP_STANDBY_MIN_DELAYS[i - 1], this.DEFAULT_APP_STANDBY_DELAYS[i]));
                }
                updateAllowWhileIdleWhitelistDurationLocked();
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_MIN_FUTURITY);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_FUTURITY, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_INTERVAL);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_INTERVAL, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_INTERVAL);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_INTERVAL, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LISTENER_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LISTENER_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_SHORT_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_SHORT_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_LONG_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_LONG_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.ALLOW_WHILE_IDLE_WHITELIST_DURATION, pw);
            pw.println();
            for (int i = 0; i < this.KEYS_APP_STANDBY_DELAY.length; i++) {
                pw.print("    ");
                pw.print(this.KEYS_APP_STANDBY_DELAY[i]);
                pw.print("=");
                TimeUtils.formatDuration(this.APP_STANDBY_MIN_DELAYS[i], pw);
                pw.println();
            }
        }

        void dumpProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, this.MIN_FUTURITY);
            proto.write(1112396529666L, this.MIN_INTERVAL);
            proto.write(1112396529671L, this.MAX_INTERVAL);
            proto.write(1112396529667L, this.LISTENER_TIMEOUT);
            proto.write(1112396529668L, this.ALLOW_WHILE_IDLE_SHORT_TIME);
            proto.write(1112396529669L, this.ALLOW_WHILE_IDLE_LONG_TIME);
            proto.write(1112396529670L, this.ALLOW_WHILE_IDLE_WHITELIST_DURATION);
            proto.end(token);
        }
    }

    class DeliveryTracker extends IAlarmCompleteListener.Stub implements OnFinished {
        DeliveryTracker() {
        }

        private InFlight removeLocked(PendingIntent pi, Intent intent) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (((InFlight) AlarmManagerService.this.mInFlight.get(i)).mPendingIntent == pi) {
                    return (InFlight) AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            LocalLog localLog = AlarmManagerService.this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No in-flight alarm for ");
            stringBuilder.append(pi);
            stringBuilder.append(" ");
            stringBuilder.append(intent);
            localLog.w(stringBuilder.toString());
            return null;
        }

        private InFlight removeLocked(IBinder listener) {
            for (int i = 0; i < AlarmManagerService.this.mInFlight.size(); i++) {
                if (((InFlight) AlarmManagerService.this.mInFlight.get(i)).mListener == listener) {
                    return (InFlight) AlarmManagerService.this.mInFlight.remove(i);
                }
            }
            LocalLog localLog = AlarmManagerService.this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No in-flight alarm for listener ");
            stringBuilder.append(listener);
            localLog.w(stringBuilder.toString());
            return null;
        }

        private void updateStatsLocked(InFlight inflight) {
            long nowELAPSED = SystemClock.elapsedRealtime();
            BroadcastStats bs = inflight.mBroadcastStats;
            bs.nesting--;
            if (bs.nesting <= 0) {
                bs.nesting = 0;
                bs.aggregateTime += nowELAPSED - bs.startTime;
            }
            FilterStats fs = inflight.mFilterStats;
            fs.nesting--;
            if (fs.nesting <= 0) {
                fs.nesting = 0;
                fs.aggregateTime += nowELAPSED - fs.startTime;
            }
            ActivityManager.noteAlarmFinish(inflight.mPendingIntent, inflight.mWorkSource, inflight.mUid, inflight.mTag);
        }

        private void updateTrackingLocked(InFlight inflight) {
            if (inflight != null) {
                updateStatsLocked(inflight);
            }
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            alarmManagerService.mBroadcastRefCount--;
            int i = 0;
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.mHandler.obtainMessage(4, Integer.valueOf(0)).sendToTarget();
                AlarmManagerService.this.mWakeLock.release();
                if (AlarmManagerService.this.mInFlight.size() > 0) {
                    LocalLog localLog = AlarmManagerService.this.mLog;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Finished all dispatches with ");
                    stringBuilder.append(AlarmManagerService.this.mInFlight.size());
                    stringBuilder.append(" remaining inflights");
                    localLog.w(stringBuilder.toString());
                    while (true) {
                        int i2 = i;
                        if (i2 < AlarmManagerService.this.mInFlight.size()) {
                            LocalLog localLog2 = AlarmManagerService.this.mLog;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("  Remaining #");
                            stringBuilder.append(i2);
                            stringBuilder.append(": ");
                            stringBuilder.append(AlarmManagerService.this.mInFlight.get(i2));
                            localLog2.w(stringBuilder.toString());
                            i = i2 + 1;
                        } else {
                            AlarmManagerService.this.mInFlight.clear();
                            return;
                        }
                    }
                }
            } else if (AlarmManagerService.this.mInFlight.size() > 0) {
                InFlight inFlight = (InFlight) AlarmManagerService.this.mInFlight.get(0);
                AlarmManagerService.this.setWakelockWorkSource(inFlight.mPendingIntent, inFlight.mWorkSource, inFlight.mAlarmType, inFlight.mTag, -1, false);
            } else {
                AlarmManagerService.this.mLog.w("Alarm wakelock still held but sent queue empty");
                AlarmManagerService.this.mWakeLock.setWorkSource(null);
            }
        }

        public void alarmComplete(IBinder who) {
            if (who == null) {
                LocalLog localLog = AlarmManagerService.this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid alarmComplete: uid=");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" pid=");
                stringBuilder.append(Binder.getCallingPid());
                localLog.w(stringBuilder.toString());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (AlarmManagerService.this.mLock) {
                    AlarmManagerService.this.mHandler.removeMessages(3, who);
                    InFlight inflight = removeLocked(who);
                    if (inflight != null) {
                        updateTrackingLocked(inflight);
                        AlarmManagerService.this.mListenerFinishCount = AlarmManagerService.this.mListenerFinishCount + 1;
                    }
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onSendFinished(PendingIntent pi, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.mSendFinishCount = AlarmManagerService.this.mSendFinishCount + 1;
                updateTrackingLocked(removeLocked(pi, intent));
            }
        }

        public void alarmTimedOut(IBinder who) {
            synchronized (AlarmManagerService.this.mLock) {
                InFlight inflight = removeLocked(who);
                if (inflight != null) {
                    updateTrackingLocked(inflight);
                    AlarmManagerService.this.mListenerFinishCount = AlarmManagerService.this.mListenerFinishCount + 1;
                } else {
                    LocalLog localLog = AlarmManagerService.this.mLog;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Spurious timeout of listener ");
                    stringBuilder.append(who);
                    localLog.w(stringBuilder.toString());
                }
            }
        }

        @GuardedBy("mLock")
        public void deliverLocked(Alarm alarm, long nowELAPSED, boolean allowWhileIdle) {
            int i;
            Alarm alarm2 = alarm;
            long j = nowELAPSED;
            if (alarm2.operation != null) {
                AlarmManagerService.this.hwAddFirstFlagForRtcAlarm(alarm2, AlarmManagerService.this.mBackgroundIntent);
                AlarmManagerService.this.mBackgroundIntent.addHwFlags(2048);
                AlarmManagerService.this.mSendCount = AlarmManagerService.this.mSendCount + 1;
                if (alarm2.priorityClass.priority == 0) {
                    AlarmManagerService.this.mLastTickIssued = j;
                }
                try {
                    alarm2.operation.send(AlarmManagerService.this.getContext(), 0, AlarmManagerService.this.mBackgroundIntent.putExtra("android.intent.extra.ALARM_COUNT", alarm2.count), AlarmManagerService.this.mDeliveryTracker, AlarmManagerService.this.mHandler, null, allowWhileIdle ? AlarmManagerService.this.mIdleOptions : null);
                    AlarmManagerService.this.hwRemoveRtcAlarm(alarm2, false);
                } catch (CanceledException e) {
                    if (alarm2.operation == AlarmManagerService.this.mTimeTickSender) {
                        Slog.wtf(AlarmManagerService.TAG, "mTimeTickSender canceled");
                    }
                    if (alarm2.repeatInterval > 0) {
                        AlarmManagerService.this.removeImpl(alarm2.operation);
                    }
                    AlarmManagerService.this.mSendFinishCount = AlarmManagerService.this.mSendFinishCount + 1;
                    return;
                }
            }
            AlarmManagerService.this.mListenerCount = AlarmManagerService.this.mListenerCount + 1;
            try {
                alarm2.listener.doAlarm(this);
                AlarmManagerService.this.mHandler.sendMessageDelayed(AlarmManagerService.this.mHandler.obtainMessage(3, alarm2.listener.asBinder()), AlarmManagerService.this.mConstants.LISTENER_TIMEOUT);
            } catch (Exception e2) {
                AlarmManagerService.this.mListenerFinishCount = AlarmManagerService.this.mListenerFinishCount + 1;
                return;
            }
            if (AlarmManagerService.this.mBroadcastRefCount == 0) {
                AlarmManagerService.this.setWakelockWorkSource(alarm2.operation, alarm2.workSource, alarm2.type, alarm2.statsTag, alarm2.operation == null ? alarm2.uid : -1, true);
                AlarmManagerService.this.mWakeLock.acquire();
                AlarmManagerService.this.mHandler.obtainMessage(4, Integer.valueOf(1)).sendToTarget();
            }
            AlarmManagerService alarmManagerService = AlarmManagerService.this;
            PendingIntent pendingIntent = alarm2.operation;
            IAlarmListener iAlarmListener = alarm2.listener;
            WorkSource workSource = alarm2.workSource;
            int i2 = alarm2.uid;
            String str = alarm2.packageName;
            int i3 = alarm2.type;
            String str2 = alarm2.statsTag;
            int i4 = 1;
            InFlight inFlight = new InFlight(alarmManagerService, pendingIntent, iAlarmListener, workSource, i2, str, i3, str2, j);
            AlarmManagerService.this.mInFlight.add(inFlight);
            AlarmManagerService alarmManagerService2 = AlarmManagerService.this;
            alarmManagerService2.mBroadcastRefCount += i4;
            int i5;
            if (allowWhileIdle) {
                i = i4;
                alarm2 = alarm;
                AlarmManagerService.this.mLastAllowWhileIdleDispatch.put(alarm2.creatorUid, j);
                if (AlarmManagerService.this.mAppStateTracker == null || AlarmManagerService.this.mAppStateTracker.isUidInForeground(alarm2.creatorUid)) {
                    i5 = 0;
                    AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(alarm2.creatorUid, i);
                } else {
                    i5 = 0;
                    AlarmManagerService.this.mUseAllowWhileIdleShortTime.put(alarm2.creatorUid, false);
                }
            } else {
                i = i4;
                alarm2 = alarm;
                i5 = 0;
            }
            if (!AlarmManagerService.this.isExemptFromAppStandby(alarm2)) {
                AlarmManagerService.this.mLastAlarmDeliveredForPackage.put(Pair.create(alarm2.sourcePackage, Integer.valueOf(UserHandle.getUserId(alarm2.creatorUid))), Long.valueOf(nowELAPSED));
            }
            BroadcastStats bs = inFlight.mBroadcastStats;
            bs.count += i;
            if (bs.nesting == 0) {
                bs.nesting = i;
                bs.startTime = j;
            } else {
                bs.nesting += i;
            }
            FilterStats fs = inFlight.mFilterStats;
            fs.count += i;
            if (fs.nesting == 0) {
                fs.nesting = i;
                fs.startTime = j;
            } else {
                fs.nesting += i;
            }
            if (alarm2.type == 2 || alarm2.type == 0) {
                bs.numWakeup += i;
                fs.numWakeup += i;
                ActivityManager.noteWakeupAlarm(alarm2.operation, alarm2.workSource, alarm2.uid, alarm2.packageName, alarm2.statsTag);
            }
            String pkg = alarm2.packageName;
            if (!(pkg == null || (AlarmManagerService.this.mInteractive && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkg)))) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                if (alarm2.operation != null) {
                    Intent alarmIntent = alarm2.operation.getIntent();
                    if (alarmIntent != null) {
                        str = alarmIntent.getAction();
                    }
                } else if (alarm2.statsTag != null) {
                    str = alarm2.statsTag;
                }
                LogPower.push(121, pkg, String.valueOf(alarm2.type), String.valueOf(alarm2.repeatInterval), new String[]{String.valueOf(fs.count), str});
            }
        }
    }

    static final class FilterStats {
        long aggregateTime;
        int count;
        long lastTime;
        final BroadcastStats mBroadcastStats;
        final String mTag;
        int nesting;
        int numWakeup;
        long startTime;

        FilterStats(BroadcastStats broadcastStats, String tag) {
            this.mBroadcastStats = broadcastStats;
            this.mTag = tag;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FilterStats{tag=");
            stringBuilder.append(this.mTag);
            stringBuilder.append(", lastTime=");
            stringBuilder.append(this.lastTime);
            stringBuilder.append(", aggregateTime=");
            stringBuilder.append(this.aggregateTime);
            stringBuilder.append(", count=");
            stringBuilder.append(this.count);
            stringBuilder.append(", numWakeup=");
            stringBuilder.append(this.numWakeup);
            stringBuilder.append(", startTime=");
            stringBuilder.append(this.startTime);
            stringBuilder.append(", nesting=");
            stringBuilder.append(this.nesting);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1138166333441L, this.mTag);
            proto.write(1112396529666L, this.lastTime);
            proto.write(1112396529667L, this.aggregateTime);
            proto.write(1120986464260L, this.count);
            proto.write(1120986464261L, this.numWakeup);
            proto.write(1112396529670L, this.startTime);
            proto.write(1120986464263L, this.nesting);
            proto.end(token);
        }
    }

    static final class IdleDispatchEntry {
        long argRealtime;
        long elapsedRealtime;
        String op;
        String pkg;
        String tag;
        int uid;

        IdleDispatchEntry() {
        }
    }

    static final class InFlight {
        final int mAlarmType;
        final BroadcastStats mBroadcastStats;
        final FilterStats mFilterStats;
        final IBinder mListener;
        final PendingIntent mPendingIntent;
        final String mTag;
        final int mUid;
        final long mWhenElapsed;
        final WorkSource mWorkSource;

        InFlight(AlarmManagerService service, PendingIntent pendingIntent, IAlarmListener listener, WorkSource workSource, int uid, String alarmPkg, int alarmType, String tag, long nowELAPSED) {
            BroadcastStats access$300;
            this.mPendingIntent = pendingIntent;
            this.mWhenElapsed = nowELAPSED;
            this.mListener = listener != null ? listener.asBinder() : null;
            this.mWorkSource = workSource;
            this.mUid = uid;
            this.mTag = tag;
            if (pendingIntent != null) {
                access$300 = service.getStatsLocked(pendingIntent);
            } else {
                access$300 = service.getStatsLocked(uid, alarmPkg);
            }
            this.mBroadcastStats = access$300;
            FilterStats fs = (FilterStats) this.mBroadcastStats.filterStats.get(this.mTag);
            if (fs == null) {
                fs = new FilterStats(this.mBroadcastStats, this.mTag);
                this.mBroadcastStats.filterStats.put(this.mTag, fs);
            }
            fs.lastTime = nowELAPSED;
            this.mFilterStats = fs;
            this.mAlarmType = alarmType;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InFlight{pendingIntent=");
            stringBuilder.append(this.mPendingIntent);
            stringBuilder.append(", when=");
            stringBuilder.append(this.mWhenElapsed);
            stringBuilder.append(", workSource=");
            stringBuilder.append(this.mWorkSource);
            stringBuilder.append(", uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(", tag=");
            stringBuilder.append(this.mTag);
            stringBuilder.append(", broadcastStats=");
            stringBuilder.append(this.mBroadcastStats);
            stringBuilder.append(", filterStats=");
            stringBuilder.append(this.mFilterStats);
            stringBuilder.append(", alarmType=");
            stringBuilder.append(this.mAlarmType);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1120986464257L, this.mUid);
            proto.write(1138166333442L, this.mTag);
            proto.write(1112396529667L, this.mWhenElapsed);
            proto.write(1159641169924L, this.mAlarmType);
            if (this.mPendingIntent != null) {
                this.mPendingIntent.writeToProto(proto, 1146756268037L);
            }
            if (this.mBroadcastStats != null) {
                this.mBroadcastStats.writeToProto(proto, 1146756268038L);
            }
            if (this.mFilterStats != null) {
                this.mFilterStats.writeToProto(proto, 1146756268039L);
            }
            if (this.mWorkSource != null) {
                this.mWorkSource.writeToProto(proto, 1146756268040L);
            }
            proto.end(token);
        }
    }

    public static class IncreasingTimeOrder implements Comparator<Alarm> {
        public int compare(Alarm a1, Alarm a2) {
            long when1 = a1.whenElapsed;
            long when2 = a2.whenElapsed;
            if (when1 > when2) {
                return 1;
            }
            if (when1 < when2) {
                return -1;
            }
            return 0;
        }
    }

    class InteractiveStateReceiver extends BroadcastReceiver {
        public InteractiveStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.setPriority(1000);
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.interactiveStateChangedLocked("android.intent.action.SCREEN_ON".equals(intent.getAction()));
                if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    AlarmManagerService.this.mIsScreenOn = true;
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    AlarmManagerService.this.mIsScreenOn = false;
                }
            }
        }
    }

    final class PriorityClass {
        int priority = 2;
        int seq;

        PriorityClass() {
            this.seq = AlarmManagerService.this.mCurrentSeq - 1;
        }
    }

    private class ShellCmd extends ShellCommand {
        private ShellCmd() {
        }

        /* synthetic */ ShellCmd(AlarmManagerService x0, AnonymousClass1 x1) {
            this();
        }

        IAlarmManager getBinderService() {
            return Stub.asInterface(AlarmManagerService.this.mService);
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0036 A:{Catch:{ Exception -> 0x005d }} */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0047 A:{Catch:{ Exception -> 0x005d }} */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x003b A:{Catch:{ Exception -> 0x005d }} */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x0036 A:{Catch:{ Exception -> 0x005d }} */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0047 A:{Catch:{ Exception -> 0x005d }} */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x003b A:{Catch:{ Exception -> 0x005d }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            int i = -1;
            try {
                int hashCode = cmd.hashCode();
                if (hashCode == 1369384280) {
                    if (cmd.equals("set-time")) {
                        hashCode = 0;
                        switch (hashCode) {
                            case 0:
                                break;
                            case 1:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 2023087364 && cmd.equals("set-timezone")) {
                    hashCode = 1;
                    switch (hashCode) {
                        case 0:
                            if (getBinderService().setTime(Long.parseLong(getNextArgRequired()))) {
                                i = 0;
                            }
                            return i;
                        case 1:
                            getBinderService().setTimeZone(getNextArgRequired());
                            return 0;
                        default:
                            return handleDefaultCommands(cmd);
                    }
                }
                hashCode = -1;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Alarm manager service (alarm) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  set-time TIME");
            pw.println("    Set the system clock time to TIME where TIME is milliseconds");
            pw.println("    since the Epoch.");
            pw.println("  set-timezone TZ");
            pw.println("    Set the system timezone to TZ where TZ is an Olson id.");
        }
    }

    interface Stats {
        public static final int REBATCH_ALL_ALARMS = 0;
        public static final int REORDER_ALARMS_FOR_STANDBY = 1;
    }

    final class UidObserver extends IUidObserver.Stub {
        UidObserver() {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq) {
        }

        public void onUidGone(int uid, boolean disabled) {
            if (disabled) {
                AlarmManagerService.this.mHandler.postRemoveForStopped(uid);
            }
        }

        public void onUidActive(int uid) {
        }

        public void onUidIdle(int uid, boolean disabled) {
            if (disabled) {
                AlarmManagerService.this.mHandler.postRemoveForStopped(uid);
            }
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }
    }

    class UninstallReceiver extends BroadcastReceiver {
        public UninstallReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            filter.addDataScheme("package");
            AlarmManagerService.this.getContext().registerReceiver(this, filter);
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            sdFilter.addAction("android.intent.action.USER_STOPPED");
            sdFilter.addAction("android.intent.action.UID_REMOVED");
            AlarmManagerService.this.getContext().registerReceiver(this, sdFilter);
        }

        /* JADX WARNING: Missing block: B:74:0x0196, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            synchronized (AlarmManagerService.this.mLock) {
                String action = intent.getAction();
                String[] pkgList = null;
                String str = AlarmManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UninstallReceiver onReceive action:");
                stringBuilder.append(action);
                Slog.v(str, stringBuilder.toString());
                int i = 0;
                int length;
                if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.PACKAGES");
                    length = pkgList.length;
                    while (i < length) {
                        if (AlarmManagerService.this.lookForPackageLocked(pkgList[i])) {
                            setResultCode(-1);
                            return;
                        }
                        i++;
                    }
                    return;
                }
                int userHandle;
                if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                    pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    userHandle = intent.getIntExtra("android.intent.extra.user_handle", -1);
                    if (userHandle >= 0) {
                        AlarmManagerService.this.removeUserLocked(userHandle);
                        for (length = AlarmManagerService.this.mLastAlarmDeliveredForPackage.size() - 1; length >= 0; length--) {
                            if (((Integer) ((Pair) AlarmManagerService.this.mLastAlarmDeliveredForPackage.keyAt(length)).second).intValue() == userHandle) {
                                AlarmManagerService.this.mLastAlarmDeliveredForPackage.removeAt(length);
                            }
                        }
                    }
                } else if ("android.intent.action.UID_REMOVED".equals(action)) {
                    if (uid >= 0) {
                        AlarmManagerService.this.mLastAllowWhileIdleDispatch.delete(uid);
                        AlarmManagerService.this.mUseAllowWhileIdleShortTime.delete(uid);
                    }
                } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    return;
                } else {
                    Uri data = intent.getData();
                    if (!(data == null || data.getSchemeSpecificPart() == null)) {
                        pkgList = new String[]{data.getSchemeSpecificPart()};
                    }
                }
                if (pkgList != null && pkgList.length > 0) {
                    for (userHandle = AlarmManagerService.this.mLastAlarmDeliveredForPackage.size() - 1; userHandle >= 0; userHandle--) {
                        Pair<String, Integer> packageUser = (Pair) AlarmManagerService.this.mLastAlarmDeliveredForPackage.keyAt(userHandle);
                        if (ArrayUtils.contains(pkgList, (String) packageUser.first) && ((Integer) packageUser.second).intValue() == UserHandle.getUserId(uid)) {
                            AlarmManagerService.this.mLastAlarmDeliveredForPackage.removeAt(userHandle);
                        }
                    }
                    userHandle = pkgList.length;
                    while (i < userHandle) {
                        str = pkgList[i];
                        if (uid == NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE) {
                            String str2 = AlarmManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("removeLocked pkg: ");
                            stringBuilder2.append(str);
                            stringBuilder2.append(", uid:");
                            stringBuilder2.append(uid);
                            Slog.i(str2, stringBuilder2.toString());
                            AlarmManagerService.this.removeLocked(uid, str);
                        } else if (uid >= 0) {
                            AlarmManagerService.this.removeLocked(uid);
                        } else {
                            AlarmManagerService.this.removeLocked(str);
                        }
                        AlarmManagerService.this.mPriorities.remove(str);
                        for (int i2 = AlarmManagerService.this.mBroadcastStats.size() - 1; i2 >= 0; i2--) {
                            ArrayMap<String, BroadcastStats> uidStats = (ArrayMap) AlarmManagerService.this.mBroadcastStats.valueAt(i2);
                            if (!(uidStats == null || uidStats.remove(str) == null || uidStats.size() > 0)) {
                                AlarmManagerService.this.mBroadcastStats.removeAt(i2);
                            }
                        }
                        i++;
                    }
                }
            }
        }
    }

    static final class WakeupEvent {
        public String action;
        public int uid;
        public long when;

        public WakeupEvent(long theTime, int theUid, String theAction) {
            this.when = theTime;
            this.uid = theUid;
            this.action = theAction;
        }
    }

    private final class LocalService implements AlarmManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(AlarmManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void removeAlarmsForUid(int uid) {
            synchronized (AlarmManagerService.this.mLock) {
                AlarmManagerService.this.removeLocked(uid);
            }
        }
    }

    private native void close(long j);

    private native long init();

    private native int set(long j, int i, long j2, long j3);

    private native int setKernelTime(long j, long j2);

    private native int setKernelTimezone(long j, int i);

    private native int waitForAlarm(long j);

    protected native void hwSetClockRTC(long j, long j2, long j3);

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG_TIMEZONE = z;
    }

    void calculateDeliveryPriorities(ArrayList<Alarm> alarms) {
        int N = alarms.size();
        for (int i = 0; i < N; i++) {
            int alarmPrio;
            Alarm a = (Alarm) alarms.get(i);
            if (a.operation != null && "android.intent.action.TIME_TICK".equals(resetActionCallingIdentity(a.operation))) {
                alarmPrio = 0;
            } else if (a.wakeup) {
                alarmPrio = 1;
            } else {
                alarmPrio = 2;
            }
            PriorityClass packagePrio = a.priorityClass;
            String alarmPackage = a.sourcePackage;
            if (packagePrio == null) {
                packagePrio = (PriorityClass) this.mPriorities.get(alarmPackage);
            }
            if (packagePrio == null) {
                PriorityClass priorityClass = new PriorityClass();
                a.priorityClass = priorityClass;
                packagePrio = priorityClass;
                this.mPriorities.put(alarmPackage, packagePrio);
            }
            a.priorityClass = packagePrio;
            if (packagePrio.seq != this.mCurrentSeq) {
                packagePrio.priority = alarmPrio;
                packagePrio.seq = this.mCurrentSeq;
            } else if (alarmPrio < packagePrio.priority) {
                packagePrio.priority = alarmPrio;
            }
        }
    }

    public AlarmManagerService(Context context) {
        super(context);
        publishLocalService(AlarmManagerInternal.class, new LocalService(this, null));
    }

    static long convertToElapsed(long when, int type) {
        boolean isRtc = true;
        if (!(type == 1 || type == 0)) {
            isRtc = false;
        }
        if (isRtc) {
            return when - (System.currentTimeMillis() - SystemClock.elapsedRealtime());
        }
        return when;
    }

    public static long maxTriggerTime(long now, long triggerAtTime, long interval) {
        long futurity;
        if (interval == 0) {
            futurity = triggerAtTime - now;
        } else {
            futurity = interval;
        }
        if (futurity < 10000) {
            futurity = 0;
        }
        return clampPositive(((long) (0.75d * ((double) futurity))) + triggerAtTime);
    }

    static boolean addBatchLocked(ArrayList<Batch> list, Batch newBatch) {
        int index = Collections.binarySearch(list, newBatch, sBatchOrder);
        if (index < 0) {
            index = (0 - index) - 1;
        }
        list.add(index, newBatch);
        if (index == 0) {
            return true;
        }
        return false;
    }

    private void insertAndBatchAlarmLocked(Alarm alarm) {
        int whichBatch;
        adjustAlarmLocked(alarm);
        if ((alarm.flags & 1) != 0) {
            whichBatch = -1;
        } else {
            whichBatch = attemptCoalesceLocked(alarm.whenElapsed, alarm.maxWhenElapsed);
        }
        if (whichBatch < 0) {
            addBatchLocked(this.mAlarmBatches, new Batch(alarm));
            return;
        }
        Batch batch = (Batch) this.mAlarmBatches.get(whichBatch);
        if (batch.add(alarm)) {
            this.mAlarmBatches.remove(whichBatch);
            addBatchLocked(this.mAlarmBatches, batch);
        }
    }

    int attemptCoalesceLocked(long whenElapsed, long maxWhen) {
        int N = this.mAlarmBatches.size();
        for (int i = 0; i < N; i++) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            if ((b.flags & 1) == 0 && b.canHold(whenElapsed, maxWhen)) {
                return i;
            }
        }
        return -1;
    }

    static int getAlarmCount(ArrayList<Batch> batches) {
        int ret = 0;
        for (int i = 0; i < batches.size(); i++) {
            ret += ((Batch) batches.get(i)).size();
        }
        return ret;
    }

    boolean haveAlarmsTimeTickAlarm(ArrayList<Alarm> alarms) {
        if (alarms.size() == 0) {
            return false;
        }
        int batchSize = alarms.size();
        for (int j = 0; j < batchSize; j++) {
            if (((Alarm) alarms.get(j)).operation == this.mTimeTickSender) {
                return true;
            }
        }
        return false;
    }

    boolean haveBatchesTimeTickAlarm(ArrayList<Batch> batches) {
        int numBatches = batches.size();
        for (int i = 0; i < numBatches; i++) {
            if (haveAlarmsTimeTickAlarm(((Batch) batches.get(i)).alarms)) {
                return true;
            }
        }
        return false;
    }

    void rebatchAllAlarms() {
        synchronized (this.mLock) {
            rebatchAllAlarmsLocked(true);
        }
    }

    void rebatchAllAlarmsLocked(boolean doValidate) {
        boolean z;
        String str;
        StringBuilder stringBuilder;
        long start = this.mStatLogger.getTime();
        int oldCount = getAlarmCount(this.mAlarmBatches) + ArrayUtils.size(this.mPendingWhileIdleAlarms);
        boolean oldHasTick = haveBatchesTimeTickAlarm(this.mAlarmBatches) || haveAlarmsTimeTickAlarm(this.mPendingWhileIdleAlarms);
        ArrayList<Batch> oldSet = (ArrayList) this.mAlarmBatches.clone();
        this.mAlarmBatches.clear();
        Alarm oldPendingIdleUntil = this.mPendingIdleUntil;
        long nowElapsed = SystemClock.elapsedRealtime();
        int oldBatches = oldSet.size();
        this.mCancelRemoveAction = true;
        for (int batchNum = 0; batchNum < oldBatches; batchNum++) {
            Batch batch = (Batch) oldSet.get(batchNum);
            int N = batch.size();
            for (int i = 0; i < N; i++) {
                reAddAlarmLocked(batch.get(i), nowElapsed, doValidate);
            }
            z = doValidate;
        }
        z = doValidate;
        this.mCancelRemoveAction = false;
        if (!(oldPendingIdleUntil == null || oldPendingIdleUntil == this.mPendingIdleUntil)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Rebatching: idle until changed from ");
            stringBuilder2.append(oldPendingIdleUntil);
            stringBuilder2.append(" to ");
            stringBuilder2.append(this.mPendingIdleUntil);
            Slog.wtf(str2, stringBuilder2.toString());
            if (this.mPendingIdleUntil == null) {
                restorePendingWhileIdleAlarmsLocked();
            }
        }
        int newCount = getAlarmCount(this.mAlarmBatches) + ArrayUtils.size(this.mPendingWhileIdleAlarms);
        boolean z2 = haveBatchesTimeTickAlarm(this.mAlarmBatches) || haveAlarmsTimeTickAlarm(this.mPendingWhileIdleAlarms);
        boolean newHasTick = z2;
        if (oldCount != newCount) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Rebatching: total count changed from ");
            stringBuilder.append(oldCount);
            stringBuilder.append(" to ");
            stringBuilder.append(newCount);
            Slog.wtf(str, stringBuilder.toString());
        }
        if (oldHasTick != newHasTick) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Rebatching: hasTick changed from ");
            stringBuilder.append(oldHasTick);
            stringBuilder.append(" to ");
            stringBuilder.append(newHasTick);
            Slog.wtf(str, stringBuilder.toString());
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
        this.mStatLogger.logDurationStat(0, start);
    }

    boolean reorderAlarmsBasedOnStandbyBuckets(ArraySet<Pair<String, Integer>> targetPackages) {
        long start = this.mStatLogger.getTime();
        ArrayList<Alarm> rescheduledAlarms = new ArrayList();
        for (int batchIndex = this.mAlarmBatches.size() - 1; batchIndex >= 0; batchIndex--) {
            Batch batch = (Batch) this.mAlarmBatches.get(batchIndex);
            for (int alarmIndex = batch.size() - 1; alarmIndex >= 0; alarmIndex--) {
                Alarm alarm = batch.get(alarmIndex);
                Pair<String, Integer> packageUser = Pair.create(alarm.sourcePackage, Integer.valueOf(UserHandle.getUserId(alarm.creatorUid)));
                if ((targetPackages == null || targetPackages.contains(packageUser)) && adjustDeliveryTimeBasedOnStandbyBucketLocked(alarm)) {
                    batch.remove(alarm);
                    rescheduledAlarms.add(alarm);
                }
            }
            if (batch.size() == 0) {
                this.mAlarmBatches.remove(batchIndex);
            }
        }
        for (int i = 0; i < rescheduledAlarms.size(); i++) {
            insertAndBatchAlarmLocked((Alarm) rescheduledAlarms.get(i));
        }
        this.mStatLogger.logDurationStat(1, start);
        if (rescheduledAlarms.size() > 0) {
            return true;
        }
        return false;
    }

    void reAddAlarmLocked(Alarm a, long nowElapsed, boolean doValidate) {
        long maxElapsed;
        a.when = a.origWhen;
        long whenElapsed = convertToElapsed(a.when, a.type);
        if (a.windowLength == 0) {
            maxElapsed = whenElapsed;
        } else if (a.windowLength > 0) {
            maxElapsed = clampPositive(a.windowLength + whenElapsed);
        } else {
            maxElapsed = maxTriggerTime(nowElapsed, whenElapsed, a.repeatInterval);
        }
        a.whenElapsed = whenElapsed;
        a.maxWhenElapsed = maxElapsed;
        setImplLocked(a, true, doValidate);
    }

    static long clampPositive(long val) {
        return val >= 0 ? val : JobStatus.NO_LATEST_RUNTIME;
    }

    void sendPendingBackgroundAlarmsLocked(int uid, String packageName) {
        ArrayList<Alarm> alarmsForUid = (ArrayList) this.mPendingBackgroundAlarms.get(uid);
        if (alarmsForUid != null && alarmsForUid.size() != 0) {
            ArrayList<Alarm> alarmsToDeliver;
            if (packageName != null) {
                alarmsToDeliver = new ArrayList();
                for (int i = alarmsForUid.size() - 1; i >= 0; i--) {
                    if (((Alarm) alarmsForUid.get(i)).matches(packageName)) {
                        alarmsToDeliver.add((Alarm) alarmsForUid.remove(i));
                    }
                }
                if (alarmsForUid.size() == 0) {
                    this.mPendingBackgroundAlarms.remove(uid);
                }
            } else {
                alarmsToDeliver = alarmsForUid;
                this.mPendingBackgroundAlarms.remove(uid);
            }
            deliverPendingBackgroundAlarmsLocked(alarmsToDeliver, SystemClock.elapsedRealtime());
        }
    }

    void sendAllUnrestrictedPendingBackgroundAlarmsLocked() {
        ArrayList<Alarm> alarmsToDeliver = new ArrayList();
        findAllUnrestrictedPendingBackgroundAlarmsLockedInner(this.mPendingBackgroundAlarms, alarmsToDeliver, new -$$Lambda$AlarmManagerService$nSJw2tKfoL3YIrKDtszoL44jcSM(this));
        if (alarmsToDeliver.size() > 0) {
            deliverPendingBackgroundAlarmsLocked(alarmsToDeliver, SystemClock.elapsedRealtime());
        }
    }

    @VisibleForTesting
    static void findAllUnrestrictedPendingBackgroundAlarmsLockedInner(SparseArray<ArrayList<Alarm>> pendingAlarms, ArrayList<Alarm> unrestrictedAlarms, Predicate<Alarm> isBackgroundRestricted) {
        for (int uidIndex = pendingAlarms.size() - 1; uidIndex >= 0; uidIndex--) {
            int uid = pendingAlarms.keyAt(uidIndex);
            ArrayList<Alarm> alarmsForUid = (ArrayList) pendingAlarms.valueAt(uidIndex);
            for (int alarmIndex = alarmsForUid.size() - 1; alarmIndex >= 0; alarmIndex--) {
                Alarm alarm = (Alarm) alarmsForUid.get(alarmIndex);
                if (!isBackgroundRestricted.test(alarm)) {
                    unrestrictedAlarms.add(alarm);
                    alarmsForUid.remove(alarmIndex);
                }
            }
            if (alarmsForUid.size() == 0) {
                pendingAlarms.removeAt(uidIndex);
            }
        }
    }

    private void deliverPendingBackgroundAlarmsLocked(ArrayList<Alarm> alarms, long nowELAPSED) {
        long j;
        long j2;
        AlarmManagerService alarmManagerService;
        AlarmManagerService alarmManagerService2 = this;
        ArrayList arrayList = alarms;
        long j3 = nowELAPSED;
        int N = alarms.size();
        boolean hasWakeup = false;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= N) {
                break;
            }
            int N2;
            int i3;
            Alarm alarm = (Alarm) arrayList.get(i2);
            if (alarm.wakeup) {
                hasWakeup = true;
            }
            boolean hasWakeup2 = hasWakeup;
            alarm.count = 1;
            if (alarm.repeatInterval > 0) {
                alarm.count = (int) (((long) alarm.count) + ((j3 - alarm.expectedWhenElapsed) / alarm.repeatInterval));
                long delta = ((long) alarm.count) * alarm.repeatInterval;
                long nextElapsed = alarm.whenElapsed + delta;
                int i4 = alarm.type;
                long j4 = alarm.when + delta;
                long j5 = alarm.windowLength;
                long j6 = j5;
                long maxTriggerTime = maxTriggerTime(j3, nextElapsed, alarm.repeatInterval);
                j5 = alarm.repeatInterval;
                PendingIntent pendingIntent = alarm.operation;
                int i5 = alarm.flags;
                WorkSource workSource = alarm.workSource;
                AlarmClockInfo alarmClockInfo = alarm.alarmClock;
                i = alarm.uid;
                int i6 = i;
                WorkSource workSource2 = workSource;
                j = j4;
                j4 = j5;
                j2 = j6;
                N2 = N;
                i3 = i2;
                alarmManagerService2.setImplLocked(i4, j, nextElapsed, j2, maxTriggerTime, j4, pendingIntent, null, null, i5, true, workSource2, alarmClockInfo, i6, alarm.packageName);
            } else {
                N2 = N;
                i3 = i2;
            }
            i = i3 + 1;
            hasWakeup = hasWakeup2;
            N = N2;
            j3 = nowELAPSED;
            ArrayList<Alarm> arrayList2 = alarms;
            alarmManagerService2 = this;
        }
        if (hasWakeup) {
            alarmManagerService = this;
            j = nowELAPSED;
        } else {
            alarmManagerService = this;
            j = nowELAPSED;
            if (alarmManagerService.checkAllowNonWakeupDelayLocked(j)) {
                if (alarmManagerService.mPendingNonWakeupAlarms.size() == 0) {
                    alarmManagerService.mStartCurrentDelayTime = j;
                    alarmManagerService.mNextNonWakeupDeliveryTime = ((alarmManagerService.currentNonWakeupFuzzLocked(j) * 3) / 2) + j;
                }
                alarmManagerService.mPendingNonWakeupAlarms.addAll(alarms);
                alarmManagerService.mNumDelayedAlarms += alarms.size();
                return;
            }
        }
        ArrayList<Alarm> arrayList3 = alarms;
        if (alarmManagerService.mPendingNonWakeupAlarms.size() > 0) {
            arrayList3.addAll(alarmManagerService.mPendingNonWakeupAlarms);
            j2 = j - alarmManagerService.mStartCurrentDelayTime;
            alarmManagerService.mTotalDelayTime += j2;
            if (alarmManagerService.mMaxDelayTime < j2) {
                alarmManagerService.mMaxDelayTime = j2;
            }
            alarmManagerService.mPendingNonWakeupAlarms.clear();
        }
        calculateDeliveryPriorities(alarms);
        Collections.sort(arrayList3, alarmManagerService.mAlarmDispatchComparator);
        deliverAlarmsLocked(alarms, nowELAPSED);
    }

    void restorePendingWhileIdleAlarmsLocked() {
        if (this.mPendingWhileIdleAlarms.size() > 0) {
            ArrayList<Alarm> alarms = this.mPendingWhileIdleAlarms;
            this.mPendingWhileIdleAlarms = new ArrayList();
            long nowElapsed = SystemClock.elapsedRealtime();
            for (int i = alarms.size() - 1; i >= 0; i--) {
                reAddAlarmLocked((Alarm) alarms.get(i), nowElapsed, false);
            }
        }
        rescheduleKernelAlarmsLocked();
        updateNextAlarmClockLocked();
        try {
            this.mTimeTickSender.send();
        } catch (CanceledException e) {
        }
    }

    public void onStart() {
        this.mNativeData = init();
        this.mNextNonWakeup = 0;
        this.mNextWakeup = 0;
        Flog.d(500, "alarmmanagerservice onStart");
        setTimeZoneImpl(SystemProperties.get(TIMEZONE_PROPERTY));
        if (this.mNativeData != 0) {
            long systemBuildTime = Environment.getRootDirectory().lastModified();
            if ("normal".equals(SystemProperties.get("ro.runmode", "normal")) && System.currentTimeMillis() < systemBuildTime) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Current time only ");
                stringBuilder.append(System.currentTimeMillis());
                stringBuilder.append(", advancing to build time ");
                stringBuilder.append(systemBuildTime);
                Slog.i(str, stringBuilder.toString());
                setKernelTime(this.mNativeData, systemBuildTime);
            }
        }
        PackageManager packMan = getContext().getPackageManager();
        try {
            ApplicationInfo sysUi = packMan.getApplicationInfo(packMan.getPermissionInfo(SYSTEM_UI_SELF_PERMISSION, 0).packageName, 0);
            if ((sysUi.privateFlags & 8) != 0) {
                this.mSystemUiUid = sysUi.uid;
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SysUI permission android.permission.systemui.IDENTITY defined by non-privileged app ");
                stringBuilder2.append(sysUi.packageName);
                stringBuilder2.append(" - ignoring");
                Slog.e(str2, stringBuilder2.toString());
            }
        } catch (NameNotFoundException e) {
        }
        if (this.mSystemUiUid <= 0) {
            Slog.wtf(TAG, "SysUI package not found!");
        }
        this.mWakeLock = ((PowerManager) getContext().getSystemService("power")).newWakeLock(1, "*alarm*");
        this.mTimeTickSender = PendingIntent.getBroadcastAsUser(getContext(), 0, new Intent("android.intent.action.TIME_TICK").addFlags(1344274432), 0, UserHandle.ALL);
        Intent intent = new Intent("android.intent.action.DATE_CHANGED");
        intent.addFlags(538968064);
        this.mDateChangeSender = PendingIntent.getBroadcastAsUser(getContext(), 0, intent, 67108864, UserHandle.ALL);
        this.mClockReceiver = new ClockReceiver();
        this.mClockReceiver.scheduleTimeTickEvent();
        this.mClockReceiver.scheduleDateChangedEvent();
        this.mInteractiveStateReceiver = new InteractiveStateReceiver();
        this.mUninstallReceiver = new UninstallReceiver();
        if (this.mNativeData != 0) {
            new AlarmThread().start();
        } else {
            Slog.w(TAG, "Failed to open alarm driver. Falling back to a handler.");
        }
        try {
            ActivityManager.getService().registerUidObserver(new UidObserver(), 14, -1, null);
        } catch (RemoteException e2) {
        }
        publishBinderService("alarm", this.mService);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mConstants.start(getContext().getContentResolver());
            this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
            this.mLocalDeviceIdleController = (com.android.server.DeviceIdleController.LocalService) LocalServices.getService(com.android.server.DeviceIdleController.LocalService.class);
            this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            this.mUsageStatsManagerInternal.addAppIdleStateChangeListener(new AppStandbyTracker());
            this.mAppStateTracker = (AppStateTracker) LocalServices.getService(AppStateTracker.class);
            this.mAppStateTracker.addListener(this.mForceAppStandbyListener);
        }
    }

    protected void finalize() throws Throwable {
        try {
            close(this.mNativeData);
        } finally {
            super.finalize();
        }
    }

    boolean setTimeImpl(long millis) {
        boolean z = false;
        if (this.mNativeData == 0) {
            Slog.w(TAG, "Not setting time since no alarm driver is available.");
            return false;
        }
        synchronized (this.mLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delta=");
            stringBuilder.append(millis - System.currentTimeMillis());
            HwLog.dubaie("DUBAI_TAG_TIME_CHANGED", stringBuilder.toString());
            if (setKernelTime(this.mNativeData, millis) == 0) {
                z = true;
            }
        }
        return z;
    }

    void setTimeZoneImpl(String tz) {
        if (!TextUtils.isEmpty(tz)) {
            TimeZone zone = TimeZone.getTimeZone(tz);
            boolean timeZoneWasChanged = false;
            synchronized (this) {
                String current = SystemProperties.get(TIMEZONE_PROPERTY);
                if (current == null || !current.equals(zone.getID())) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("timezone changed: ");
                    stringBuilder.append(current);
                    stringBuilder.append(", new=");
                    stringBuilder.append(zone.getID());
                    Flog.i(500, stringBuilder.toString());
                    timeZoneWasChanged = true;
                    SystemProperties.set(TIMEZONE_PROPERTY, zone.getID());
                }
                int gmtOffset = zone.getOffset(System.currentTimeMillis());
                setKernelTimezone(this.mNativeData, -(gmtOffset / 60000));
                if (!TextUtils.isEmpty(current) && timeZoneWasChanged) {
                    int oldGmtOffset = TimeZone.getTimeZone(current).getOffset(System.currentTimeMillis());
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("oldId=");
                    stringBuilder2.append(current);
                    stringBuilder2.append(" oldGmtOffset=");
                    stringBuilder2.append(oldGmtOffset);
                    stringBuilder2.append(" newId=");
                    stringBuilder2.append(zone.getID());
                    stringBuilder2.append(" newGmtOffset=");
                    stringBuilder2.append(gmtOffset);
                    HwLog.dubaie("DUBAI_TAG_TIMEZONE_CHANGED", stringBuilder2.toString());
                }
            }
            TimeZone.setDefault(null);
            if (timeZoneWasChanged) {
                Intent intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
                intent.addFlags(555745280);
                intent.putExtra("time-zone", zone.getID());
                getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    void removeImpl(String pkg, String action) {
        if (pkg != null && action != null) {
            synchronized (this.mLock) {
                removeLocked(pkg, action);
            }
        }
    }

    void removeImpl(PendingIntent operation) {
        if (operation != null) {
            synchronized (this.mLock) {
                removeLocked(operation, null);
            }
            removeDeskClockFromFWK(operation);
        }
    }

    void setImpl(int type, long triggerAtTime, long windowLength, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, WorkSource workSource, AlarmClockInfo alarmClock, int callingUid, String callingPackage) {
        Throwable th;
        Object obj;
        long j;
        int i = type;
        long j2 = triggerAtTime;
        long windowLength2 = windowLength;
        long j3 = interval;
        PendingIntent pendingIntent = operation;
        if (!(pendingIntent == null && directReceiver == null) && (pendingIntent == null || directReceiver == null)) {
            String str;
            StringBuilder stringBuilder;
            long interval2;
            if (windowLength2 > SettingsObserver.DEFAULT_NOTIFICATION_TIMEOUT) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Window length ");
                stringBuilder.append(windowLength2);
                stringBuilder.append("ms suspiciously long; limiting to 1 hour");
                Slog.w(str, stringBuilder.toString());
                windowLength2 = SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
            }
            long minInterval = this.mConstants.MIN_INTERVAL;
            if (j3 > 0 && j3 < minInterval) {
                String str2 = TAG;
                interval2 = new StringBuilder();
                interval2.append("Suspiciously short interval ");
                interval2.append(j3);
                interval2.append(" millis; expanding to ");
                interval2.append(minInterval / 1000);
                interval2.append(" seconds");
                Slog.w(str2, interval2.toString());
                j3 = minInterval;
            } else if (j3 > this.mConstants.MAX_INTERVAL) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Suspiciously long interval ");
                stringBuilder.append(j3);
                stringBuilder.append(" millis; clamping");
                Slog.w(str, stringBuilder.toString());
                j3 = this.mConstants.MAX_INTERVAL;
            }
            interval2 = j3;
            long j4;
            if (i < 0 || i > 3) {
                j4 = minInterval;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid alarm type ");
                stringBuilder2.append(type);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            long triggerAtTime2;
            long windowLength3;
            if (j2 < 0) {
                j3 = (long) Binder.getCallingPid();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid alarm trigger time! ");
                stringBuilder.append(j2);
                stringBuilder.append(" from uid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(" pid=");
                stringBuilder.append(j3);
                Slog.w(str, stringBuilder.toString());
                triggerAtTime2 = 0;
            } else {
                int i2 = callingUid;
                triggerAtTime2 = triggerAtTime;
            }
            long nowElapsed = SystemClock.elapsedRealtime();
            long nominalTrigger = convertToElapsed(triggerAtTime2, i);
            long minTrigger = nowElapsed + this.mConstants.MIN_FUTURITY;
            long triggerElapsed = nominalTrigger > minTrigger ? nominalTrigger : minTrigger;
            if (windowLength2 == 0) {
                j2 = triggerElapsed;
                windowLength3 = windowLength2;
                j4 = minInterval;
                minInterval = triggerElapsed;
            } else {
                if (windowLength2 < 0) {
                    j2 = maxTriggerTime(nowElapsed, triggerElapsed, interval2);
                    minInterval = triggerElapsed;
                    windowLength2 = j2 - minInterval;
                } else {
                    minInterval = triggerElapsed;
                    j2 = minInterval + windowLength2;
                }
                windowLength3 = windowLength2;
            }
            long maxElapsed = j2;
            Object obj2 = this.mLock;
            synchronized (obj2) {
                if (i == 0 || 2 == i) {
                    try {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("set(");
                        stringBuilder3.append(pendingIntent);
                        stringBuilder3.append(") : type=");
                        stringBuilder3.append(i);
                        stringBuilder3.append(" triggerAtTime=");
                        stringBuilder3.append(triggerAtTime2);
                        stringBuilder3.append(" win=");
                        stringBuilder3.append(windowLength3);
                        stringBuilder3.append(" tElapsed=");
                        stringBuilder3.append(minInterval);
                        stringBuilder3.append(" maxElapsed=");
                        stringBuilder3.append(maxElapsed);
                        stringBuilder3.append(" interval=");
                        stringBuilder3.append(interval2);
                        stringBuilder3.append(" flags=0x");
                        stringBuilder3.append(Integer.toHexString(flags));
                        Flog.i(500, stringBuilder3.toString());
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                obj = obj2;
                j = minInterval;
                setImplLocked(i, triggerAtTime2, minInterval, windowLength3, maxElapsed, interval2, operation, directReceiver, listenerTag, flags, true, workSource, alarmClock, callingUid, callingPackage);
                return;
            }
        }
        Slog.w(TAG, "Alarms must either supply a PendingIntent or an AlarmReceiver");
    }

    private void setImplLocked(int type, long when, long whenElapsed, long windowLength, long maxWhen, long interval, PendingIntent operation, IAlarmListener directReceiver, String listenerTag, int flags, boolean doValidate, WorkSource workSource, AlarmClockInfo alarmClock, int callingUid, String callingPackage) {
        int i = callingUid;
        Alarm a = new Alarm(type, when, whenElapsed, windowLength, maxWhen, interval, operation, directReceiver, listenerTag, workSource, flags, alarmClock, callingUid, callingPackage);
        int i2;
        try {
            i2 = callingUid;
            try {
                if (ActivityManager.getService().isAppStartModeDisabled(i2, callingPackage)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Not setting alarm from ");
                    stringBuilder.append(i2);
                    stringBuilder.append(":");
                    stringBuilder.append(a);
                    stringBuilder.append(" -- package not allowed to start");
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
            } catch (RemoteException e) {
            }
        } catch (RemoteException e2) {
            i2 = callingUid;
            String str2 = callingPackage;
        }
        if (this.mCancelRemoveAction) {
            PendingIntent pendingIntent = operation;
            IAlarmListener iAlarmListener = directReceiver;
        } else {
            removeLocked(operation, directReceiver);
        }
        setImplLocked(a, false, doValidate);
    }

    private long getMinDelayForBucketLocked(int bucket) {
        int index;
        if (bucket == 50) {
            index = 4;
        } else if (bucket > 30) {
            index = 3;
        } else if (bucket > 20) {
            index = 2;
        } else if (bucket > 10) {
            index = 1;
        } else {
            index = 0;
        }
        return this.mConstants.APP_STANDBY_MIN_DELAYS[index];
    }

    private boolean adjustDeliveryTimeBasedOnStandbyBucketLocked(Alarm alarm) {
        Alarm alarm2 = alarm;
        if (isExemptFromAppStandby(alarm)) {
            return false;
        }
        if (!this.mAppStandbyParole) {
            long oldWhenElapsed = alarm2.whenElapsed;
            long oldMaxWhenElapsed = alarm2.maxWhenElapsed;
            String sourcePackage = alarm2.sourcePackage;
            int sourceUserId = UserHandle.getUserId(alarm2.creatorUid);
            int standbyBucket = this.mUsageStatsManagerInternal.getAppStandbyBucket(sourcePackage, sourceUserId, SystemClock.elapsedRealtime());
            long lastElapsed = ((Long) this.mLastAlarmDeliveredForPackage.getOrDefault(Pair.create(sourcePackage, Integer.valueOf(sourceUserId)), Long.valueOf(0))).longValue();
            if (lastElapsed > 0) {
                long minElapsed = getMinDelayForBucketLocked(standbyBucket) + lastElapsed;
                if (alarm2.expectedWhenElapsed < minElapsed) {
                    alarm2.maxWhenElapsed = minElapsed;
                    alarm2.whenElapsed = minElapsed;
                } else {
                    alarm2.whenElapsed = alarm2.expectedWhenElapsed;
                    alarm2.maxWhenElapsed = alarm2.expectedMaxWhenElapsed;
                }
            }
            boolean z = (oldWhenElapsed == alarm2.whenElapsed && oldMaxWhenElapsed == alarm2.maxWhenElapsed) ? false : true;
            return z;
        } else if (alarm2.whenElapsed <= alarm2.expectedWhenElapsed) {
            return false;
        } else {
            alarm2.whenElapsed = alarm2.expectedWhenElapsed;
            alarm2.maxWhenElapsed = alarm2.expectedMaxWhenElapsed;
            return true;
        }
    }

    private void setImplLocked(Alarm a, boolean rebatching, boolean doValidate) {
        if (!this.mIsAlarmDataOnlyMode && isAwareAlarmManagerEnabled()) {
            modifyAlarmIfOverload(a);
        }
        if ((a.flags & 16) != 0) {
            if (this.mNextWakeFromIdle != null && a.whenElapsed > this.mNextWakeFromIdle.whenElapsed) {
                long j = this.mNextWakeFromIdle.whenElapsed;
                a.maxWhenElapsed = j;
                a.whenElapsed = j;
                a.when = j;
            }
            int fuzz = fuzzForDuration(a.whenElapsed - SystemClock.elapsedRealtime());
            if (fuzz > 0) {
                if (this.mRandom == null) {
                    this.mRandom = new Random();
                }
                a.whenElapsed -= (long) this.mRandom.nextInt(fuzz);
                long j2 = a.whenElapsed;
                a.maxWhenElapsed = j2;
                a.when = j2;
            }
        } else if (this.mPendingIdleUntil != null && (a.flags & 14) == 0) {
            this.mPendingWhileIdleAlarms.add(a);
            return;
        }
        adjustDeliveryTimeBasedOnStandbyBucketLocked(a);
        insertAndBatchAlarmLocked(a);
        if (a.alarmClock != null) {
            this.mNextAlarmClockMayChange = true;
        }
        boolean needRebatch = false;
        if ((a.flags & 16) != 0) {
            if (!(this.mPendingIdleUntil == a || this.mPendingIdleUntil == null)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setImplLocked: idle until changed from ");
                stringBuilder.append(this.mPendingIdleUntil);
                stringBuilder.append(" to ");
                stringBuilder.append(a);
                Slog.wtfStack(str, stringBuilder.toString());
            }
            this.mPendingIdleUntil = a;
            needRebatch = true;
        } else if ((a.flags & 2) != 0 && (this.mNextWakeFromIdle == null || this.mNextWakeFromIdle.whenElapsed > a.whenElapsed)) {
            this.mNextWakeFromIdle = a;
            if (this.mPendingIdleUntil != null) {
                needRebatch = true;
            }
        }
        if (!rebatching) {
            if (needRebatch) {
                rebatchAllAlarmsLocked(false);
            }
            hwSetRtcAlarm(a);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    private int getWakeUpNumImpl(int uid, String pkg) {
        int i;
        synchronized (this.mLock) {
            ArrayMap<String, BroadcastStats> uidStats = (ArrayMap) this.mBroadcastStats.get(uid);
            if (uidStats == null) {
                uidStats = new ArrayMap();
                this.mBroadcastStats.put(uid, uidStats);
            }
            BroadcastStats bs = (BroadcastStats) uidStats.get(pkg);
            if (bs == null) {
                bs = new BroadcastStats(uid, pkg);
                uidStats.put(pkg, bs);
            }
            i = bs.numWakeup;
        }
        return i;
    }

    void dumpImpl(PrintWriter pw) {
        PrintWriter printWriter = pw;
        synchronized (this.mLock) {
            int i;
            int user;
            long time;
            long nextWakeupRTC;
            long nowUPTIME;
            SystemServiceManager ssm;
            int i2;
            ArrayMap<String, BroadcastStats> uidStats;
            int len;
            BroadcastStats bs;
            int len2;
            Comparator<FilterStats> comparator;
            printWriter.println("Current Alarm Manager state:");
            this.mConstants.dump(printWriter);
            pw.println();
            if (this.mAppStateTracker != null) {
                this.mAppStateTracker.dump(printWriter, "  ");
                pw.println();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  App Standby Parole: ");
            stringBuilder.append(this.mAppStandbyParole);
            printWriter.println(stringBuilder.toString());
            pw.println();
            long nowRTC = System.currentTimeMillis();
            long nowELAPSED = SystemClock.elapsedRealtime();
            long nowUPTIME2 = SystemClock.uptimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            printWriter.print("  nowRTC=");
            printWriter.print(nowRTC);
            printWriter.print("=");
            printWriter.print(sdf.format(new Date(nowRTC)));
            printWriter.print(" nowELAPSED=");
            printWriter.print(nowELAPSED);
            pw.println();
            printWriter.print("  mLastTimeChangeClockTime=");
            printWriter.print(this.mLastTimeChangeClockTime);
            printWriter.print("=");
            printWriter.println(sdf.format(new Date(this.mLastTimeChangeClockTime)));
            printWriter.print("  mLastTimeChangeRealtime=");
            printWriter.println(this.mLastTimeChangeRealtime);
            printWriter.print("  mLastTickIssued=");
            printWriter.println(sdf.format(new Date(nowRTC - (nowELAPSED - this.mLastTickIssued))));
            printWriter.print("  mLastTickReceived=");
            printWriter.println(sdf.format(new Date(this.mLastTickReceived)));
            printWriter.print("  mLastTickSet=");
            printWriter.println(sdf.format(new Date(this.mLastTickSet)));
            printWriter.print("  mLastTickAdded=");
            printWriter.println(sdf.format(new Date(this.mLastTickAdded)));
            printWriter.print("  mLastTickRemoved=");
            printWriter.println(sdf.format(new Date(this.mLastTickRemoved)));
            SystemServiceManager ssm2 = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            if (ssm2 != null) {
                pw.println();
                printWriter.print("  RuntimeStarted=");
                printWriter.print(sdf.format(new Date((nowRTC - nowELAPSED) + ssm2.getRuntimeStartElapsedTime())));
                if (ssm2.isRuntimeRestarted()) {
                    printWriter.print("  (Runtime restarted)");
                }
                pw.println();
                printWriter.print("  Runtime uptime (elapsed): ");
                TimeUtils.formatDuration(nowELAPSED, ssm2.getRuntimeStartElapsedTime(), printWriter);
                pw.println();
                printWriter.print("  Runtime uptime (uptime): ");
                TimeUtils.formatDuration(nowUPTIME2, ssm2.getRuntimeStartUptime(), printWriter);
                pw.println();
            }
            pw.println();
            if (!this.mInteractive) {
                printWriter.print("  Time since non-interactive: ");
                TimeUtils.formatDuration(nowELAPSED - this.mNonInteractiveStartTime, printWriter);
                pw.println();
            }
            printWriter.print("  Max wakeup delay: ");
            TimeUtils.formatDuration(currentNonWakeupFuzzLocked(nowELAPSED), printWriter);
            pw.println();
            printWriter.print("  Time since last dispatch: ");
            TimeUtils.formatDuration(nowELAPSED - this.mLastAlarmDeliveryTime, printWriter);
            pw.println();
            printWriter.print("  Next non-wakeup delivery time: ");
            TimeUtils.formatDuration(nowELAPSED - this.mNextNonWakeupDeliveryTime, printWriter);
            pw.println();
            long nowUPTIME3 = nowUPTIME2;
            nowUPTIME2 = this.mNextWakeup + (nowRTC - nowELAPSED);
            long nowRTC2 = nowRTC;
            nowRTC = this.mNextNonWakeup + (nowRTC - nowELAPSED);
            printWriter.print("  Next non-wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextNonWakeup, nowELAPSED, printWriter);
            printWriter.print(" = ");
            printWriter.print(this.mNextNonWakeup);
            printWriter.print(" = ");
            printWriter.println(sdf.format(new Date(nowRTC)));
            printWriter.print("  Next wakeup alarm: ");
            TimeUtils.formatDuration(this.mNextWakeup, nowELAPSED, printWriter);
            printWriter.print(" = ");
            printWriter.print(this.mNextWakeup);
            printWriter.print(" = ");
            printWriter.println(sdf.format(new Date(nowUPTIME2)));
            printWriter.print("    set at ");
            TimeUtils.formatDuration(this.mLastWakeupSet, nowELAPSED, printWriter);
            pw.println();
            printWriter.print("  Last wakeup: ");
            TimeUtils.formatDuration(this.mLastWakeup, nowELAPSED, printWriter);
            printWriter.print(" = ");
            printWriter.println(this.mLastWakeup);
            printWriter.print("  Last trigger: ");
            TimeUtils.formatDuration(this.mLastTrigger, nowELAPSED, printWriter);
            printWriter.print(" = ");
            printWriter.println(this.mLastTrigger);
            printWriter.print("  Num time change events: ");
            printWriter.println(this.mNumTimeChanged);
            pw.println();
            printWriter.println("  Next alarm clock information: ");
            TreeSet<Integer> users = new TreeSet();
            for (i = 0; i < this.mNextAlarmClockForUser.size(); i++) {
                users.add(Integer.valueOf(this.mNextAlarmClockForUser.keyAt(i)));
            }
            for (i = 0; i < this.mPendingSendNextAlarmClockChangedForUser.size(); i++) {
                users.add(Integer.valueOf(this.mPendingSendNextAlarmClockChangedForUser.keyAt(i)));
            }
            Iterator it = users.iterator();
            while (true) {
                long nextNonWakeupRTC = nowRTC;
                if (!it.hasNext()) {
                    break;
                }
                Iterator it2;
                user = ((Integer) it.next()).intValue();
                AlarmClockInfo next = (AlarmClockInfo) this.mNextAlarmClockForUser.get(user);
                time = next != null ? next.getTriggerTime() : 0;
                boolean pendingSend = this.mPendingSendNextAlarmClockChangedForUser.get(user);
                printWriter.print("    user:");
                printWriter.print(user);
                printWriter.print(" pendingSend:");
                printWriter.print(pendingSend);
                printWriter.print(" time:");
                nowRTC = time;
                printWriter.print(nowRTC);
                if (nowRTC > 0) {
                    it2 = it;
                    printWriter.print(" = ");
                    printWriter.print(sdf.format(new Date(nowRTC)));
                    printWriter.print(" = ");
                    nextWakeupRTC = nowUPTIME2;
                    nowUPTIME2 = nowRTC2;
                    TimeUtils.formatDuration(nowRTC, nowUPTIME2, printWriter);
                } else {
                    it2 = it;
                    nextWakeupRTC = nowUPTIME2;
                    nowUPTIME2 = nowRTC2;
                }
                pw.println();
                nowRTC2 = nowUPTIME2;
                nowRTC = nextNonWakeupRTC;
                it = it2;
                nowUPTIME2 = nextWakeupRTC;
            }
            nextWakeupRTC = nowUPTIME2;
            nowUPTIME2 = nowRTC2;
            if (this.mAlarmBatches.size() > 0) {
                pw.println();
                printWriter.print("  Pending alarm batches: ");
                printWriter.println(this.mAlarmBatches.size());
                Iterator it3 = this.mAlarmBatches.iterator();
                while (it3.hasNext()) {
                    Batch b = (Batch) it3.next();
                    printWriter.print(b);
                    printWriter.println(':');
                    TreeSet<Integer> users2 = users;
                    Iterator it4 = it3;
                    nowUPTIME = nowUPTIME3;
                    time = nextWakeupRTC;
                    nowUPTIME3 = nowUPTIME2;
                    ssm = ssm2;
                    dumpAlarmList(printWriter, b.alarms, "    ", nowELAPSED, nowUPTIME2, sdf);
                    nowUPTIME2 = nowUPTIME3;
                    users = users2;
                    ssm2 = ssm;
                    nowUPTIME3 = nowUPTIME;
                    nextWakeupRTC = time;
                    it3 = it4;
                }
            }
            ssm = ssm2;
            nowUPTIME = nowUPTIME3;
            time = nextWakeupRTC;
            int i3 = 0;
            nowUPTIME3 = nowUPTIME2;
            pw.println();
            printWriter.println("  Pending user blocked background alarms: ");
            boolean blocked = false;
            i = 0;
            while (true) {
                i2 = i;
                if (i2 >= this.mPendingBackgroundAlarms.size()) {
                    break;
                }
                int i4;
                ArrayList<Alarm> blockedAlarms = (ArrayList) this.mPendingBackgroundAlarms.valueAt(i2);
                if (blockedAlarms == null || blockedAlarms.size() <= 0) {
                    i4 = i2;
                } else {
                    blocked = true;
                    i4 = i2;
                    dumpAlarmList(printWriter, blockedAlarms, "    ", nowELAPSED, nowUPTIME3, sdf);
                }
                i = i4 + 1;
            }
            if (!blocked) {
                printWriter.println("    none");
            }
            printWriter.println("  mLastAlarmDeliveredForPackage:");
            for (i = 0; i < this.mLastAlarmDeliveredForPackage.size(); i++) {
                Pair<String, Integer> packageUser = (Pair) this.mLastAlarmDeliveredForPackage.keyAt(i);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    Package ");
                stringBuilder2.append((String) packageUser.first);
                stringBuilder2.append(", User ");
                stringBuilder2.append(packageUser.second);
                stringBuilder2.append(":");
                printWriter.print(stringBuilder2.toString());
                TimeUtils.formatDuration(((Long) this.mLastAlarmDeliveredForPackage.valueAt(i)).longValue(), nowELAPSED, printWriter);
                pw.println();
            }
            pw.println();
            if (this.mPendingIdleUntil != null || this.mPendingWhileIdleAlarms.size() > 0) {
                pw.println();
                printWriter.println("    Idle mode state:");
                printWriter.print("      Idling until: ");
                if (this.mPendingIdleUntil != null) {
                    printWriter.println(this.mPendingIdleUntil);
                    this.mPendingIdleUntil.dump(printWriter, "        ", nowELAPSED, nowUPTIME3, sdf);
                } else {
                    printWriter.println("null");
                }
                printWriter.println("      Pending alarms:");
                dumpAlarmList(printWriter, this.mPendingWhileIdleAlarms, "      ", nowELAPSED, nowUPTIME3, sdf);
            }
            if (this.mNextWakeFromIdle != null) {
                pw.println();
                printWriter.print("  Next wake from idle: ");
                printWriter.println(this.mNextWakeFromIdle);
                this.mNextWakeFromIdle.dump(printWriter, "    ", nowELAPSED, nowUPTIME3, sdf);
            }
            pw.println();
            printWriter.print("  Past-due non-wakeup alarms: ");
            if (this.mPendingNonWakeupAlarms.size() > 0) {
                printWriter.println(this.mPendingNonWakeupAlarms.size());
                dumpAlarmList(printWriter, this.mPendingNonWakeupAlarms, "    ", nowELAPSED, nowUPTIME3, sdf);
            } else {
                printWriter.println("(none)");
            }
            printWriter.print("    Number of delayed alarms: ");
            printWriter.print(this.mNumDelayedAlarms);
            printWriter.print(", total delay time: ");
            TimeUtils.formatDuration(this.mTotalDelayTime, printWriter);
            pw.println();
            printWriter.print("    Max delay time: ");
            TimeUtils.formatDuration(this.mMaxDelayTime, printWriter);
            printWriter.print(", max non-interactive time: ");
            TimeUtils.formatDuration(this.mNonInteractiveTime, printWriter);
            pw.println();
            pw.println();
            printWriter.print("  Broadcast ref count: ");
            printWriter.println(this.mBroadcastRefCount);
            printWriter.print("  PendingIntent send count: ");
            printWriter.println(this.mSendCount);
            printWriter.print("  PendingIntent finish count: ");
            printWriter.println(this.mSendFinishCount);
            printWriter.print("  Listener send count: ");
            printWriter.println(this.mListenerCount);
            printWriter.print("  Listener finish count: ");
            printWriter.println(this.mListenerFinishCount);
            pw.println();
            if (this.mInFlight.size() > 0) {
                printWriter.println("Outstanding deliveries:");
                for (i = 0; i < this.mInFlight.size(); i++) {
                    if (this.mInFlight.get(i) != null) {
                        printWriter.print("   #");
                        printWriter.print(i);
                        printWriter.print(": ");
                        printWriter.print(this.mInFlight.get(i));
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("##");
                        stringBuilder3.append(((InFlight) this.mInFlight.get(i)).mUid);
                        printWriter.print(stringBuilder3.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("##");
                        stringBuilder3.append(((InFlight) this.mInFlight.get(i)).mTag);
                        printWriter.print(stringBuilder3.toString());
                        printWriter.print("\n");
                    }
                }
                pw.println();
            }
            if (this.mLastAllowWhileIdleDispatch.size() > 0) {
                printWriter.println("  Last allow while idle dispatch times:");
                for (i = 0; i < this.mLastAllowWhileIdleDispatch.size(); i++) {
                    printWriter.print("    UID ");
                    user = this.mLastAllowWhileIdleDispatch.keyAt(i);
                    UserHandle.formatUid(printWriter, user);
                    printWriter.print(": ");
                    long lastTime = this.mLastAllowWhileIdleDispatch.valueAt(i);
                    TimeUtils.formatDuration(lastTime, nowELAPSED, printWriter);
                    long minInterval = getWhileIdleMinIntervalLocked(user);
                    printWriter.print("  Next allowed:");
                    TimeUtils.formatDuration(lastTime + minInterval, nowELAPSED, printWriter);
                    printWriter.print(" (");
                    TimeUtils.formatDuration(minInterval, 0, printWriter);
                    printWriter.print(")");
                    pw.println();
                }
            }
            printWriter.print("  mUseAllowWhileIdleShortTime: [");
            for (i = 0; i < this.mUseAllowWhileIdleShortTime.size(); i++) {
                if (this.mUseAllowWhileIdleShortTime.valueAt(i)) {
                    UserHandle.formatUid(printWriter, this.mUseAllowWhileIdleShortTime.keyAt(i));
                    printWriter.print(" ");
                }
            }
            printWriter.println("]");
            pw.println();
            if (this.mLog.dump(printWriter, "  Recent problems", "    ")) {
                pw.println();
            }
            FilterStats[] topFilters = new FilterStats[10];
            Comparator<FilterStats> comparator2 = new Comparator<FilterStats>() {
                public int compare(FilterStats lhs, FilterStats rhs) {
                    if (lhs.aggregateTime < rhs.aggregateTime) {
                        return 1;
                    }
                    if (lhs.aggregateTime > rhs.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            int len3 = 0;
            int iu = 0;
            while (iu < this.mBroadcastStats.size()) {
                uidStats = (ArrayMap) this.mBroadcastStats.valueAt(iu);
                len = len3;
                len3 = i3;
                while (len3 < uidStats.size()) {
                    ArrayMap<String, BroadcastStats> uidStats2;
                    bs = (BroadcastStats) uidStats.valueAt(len3);
                    i2 = len;
                    len = i3;
                    while (len < bs.filterStats.size()) {
                        SimpleDateFormat sdf2;
                        int binarySearch;
                        BroadcastStats bs2;
                        FilterStats fs = (FilterStats) bs.filterStats.valueAt(len);
                        if (i2 > 0) {
                            sdf2 = sdf;
                            binarySearch = Arrays.binarySearch(topFilters, null, i2, fs, comparator2);
                        } else {
                            sdf2 = sdf;
                            binarySearch = null;
                        }
                        sdf = binarySearch;
                        if (sdf < null) {
                            uidStats2 = uidStats;
                            sdf = (-sdf) - 1;
                        } else {
                            uidStats2 = uidStats;
                        }
                        if (sdf < topFilters.length) {
                            int copylen = (topFilters.length - sdf) - 1;
                            if (copylen > 0) {
                                bs2 = bs;
                                System.arraycopy(topFilters, sdf, topFilters, sdf + 1, copylen);
                            } else {
                                bs2 = bs;
                            }
                            topFilters[sdf] = fs;
                            if (i2 < topFilters.length) {
                                i2++;
                            }
                        } else {
                            bs2 = bs;
                        }
                        len++;
                        sdf = sdf2;
                        uidStats = uidStats2;
                        bs = bs2;
                    }
                    uidStats2 = uidStats;
                    len3++;
                    len = i2;
                    i3 = 0;
                }
                iu++;
                len3 = len;
                i3 = 0;
            }
            if (len3 > 0) {
                printWriter.println("  Top Alarms:");
                for (int i5 = 0; i5 < len3; i5++) {
                    FilterStats fs2 = topFilters[i5];
                    printWriter.print("    ");
                    if (fs2.nesting > 0) {
                        printWriter.print("*ACTIVE* ");
                    }
                    TimeUtils.formatDuration(fs2.aggregateTime, printWriter);
                    printWriter.print(" running, ");
                    printWriter.print(fs2.numWakeup);
                    printWriter.print(" wakeups, ");
                    printWriter.print(fs2.count);
                    printWriter.print(" alarms: ");
                    UserHandle.formatUid(printWriter, fs2.mBroadcastStats.mUid);
                    printWriter.print(":");
                    printWriter.print(fs2.mBroadcastStats.mPackageName);
                    pw.println();
                    printWriter.print("      ");
                    printWriter.print(fs2.mTag);
                    pw.println();
                }
            }
            printWriter.println(" ");
            printWriter.println("  Alarm Stats:");
            ArrayList<FilterStats> tmpFilters = new ArrayList();
            for (iu = 0; iu < this.mBroadcastStats.size(); iu++) {
                uidStats = (ArrayMap) this.mBroadcastStats.valueAt(iu);
                len = 0;
                while (len < uidStats.size()) {
                    bs = (BroadcastStats) uidStats.valueAt(len);
                    printWriter.print("  ");
                    if (bs.nesting > 0) {
                        printWriter.print("*ACTIVE* ");
                    }
                    UserHandle.formatUid(printWriter, bs.mUid);
                    printWriter.print(":");
                    printWriter.print(bs.mPackageName);
                    printWriter.print(" ");
                    len2 = len3;
                    ArrayMap<String, BroadcastStats> uidStats3 = uidStats;
                    TimeUtils.formatDuration(bs.aggregateTime, printWriter);
                    printWriter.print(" running, ");
                    printWriter.print(bs.numWakeup);
                    printWriter.println(" wakeups:");
                    tmpFilters.clear();
                    for (len3 = 0; len3 < bs.filterStats.size(); len3++) {
                        tmpFilters.add((FilterStats) bs.filterStats.valueAt(len3));
                    }
                    Collections.sort(tmpFilters, comparator2);
                    len3 = 0;
                    while (len3 < tmpFilters.size()) {
                        FilterStats fs3 = (FilterStats) tmpFilters.get(len3);
                        printWriter.print("    ");
                        if (fs3.nesting > 0) {
                            printWriter.print("*ACTIVE* ");
                        }
                        FilterStats[] topFilters2 = topFilters;
                        comparator = comparator2;
                        TimeUtils.formatDuration(fs3.aggregateTime, printWriter);
                        printWriter.print(" ");
                        printWriter.print(fs3.numWakeup);
                        printWriter.print(" wakes ");
                        printWriter.print(fs3.count);
                        printWriter.print(" alarms, last ");
                        TimeUtils.formatDuration(fs3.lastTime, nowELAPSED, printWriter);
                        printWriter.println(":");
                        printWriter.print("      ");
                        printWriter.print(fs3.mTag);
                        pw.println();
                        len3++;
                        topFilters = topFilters2;
                        comparator2 = comparator;
                    }
                    comparator = comparator2;
                    len++;
                    len3 = len2;
                    uidStats = uidStats3;
                }
                comparator = comparator2;
                len2 = len3;
            }
            comparator = comparator2;
            len2 = len3;
            pw.println();
            this.mStatLogger.dump(printWriter, "  ");
            printHwWakeupBoot(pw);
        }
    }

    void dumpProto(FileDescriptor fd) {
        AlarmManagerService alarmManagerService = this;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (alarmManagerService.mLock) {
            int i;
            int nextAlarmClockForUserSize;
            long nowRTC;
            int nextAlarmClockForUserSize2;
            long nowElapsed;
            int i2;
            int uid;
            ArrayMap<String, BroadcastStats> uidStats;
            int copylen;
            long nowRTC2 = System.currentTimeMillis();
            long nowElapsed2 = SystemClock.elapsedRealtime();
            proto.write(1112396529665L, nowRTC2);
            proto.write(1112396529666L, nowElapsed2);
            proto.write(1112396529667L, alarmManagerService.mLastTimeChangeClockTime);
            proto.write(1112396529668L, alarmManagerService.mLastTimeChangeRealtime);
            alarmManagerService.mConstants.dumpProto(proto, 1146756268037L);
            if (alarmManagerService.mAppStateTracker != null) {
                alarmManagerService.mAppStateTracker.dumpProto(proto, 1146756268038L);
            }
            proto.write(1133871366151L, alarmManagerService.mInteractive);
            if (!alarmManagerService.mInteractive) {
                proto.write(1112396529672L, nowElapsed2 - alarmManagerService.mNonInteractiveStartTime);
                proto.write(1112396529673L, alarmManagerService.currentNonWakeupFuzzLocked(nowElapsed2));
                proto.write(1112396529674L, nowElapsed2 - alarmManagerService.mLastAlarmDeliveryTime);
                proto.write(1112396529675L, nowElapsed2 - alarmManagerService.mNextNonWakeupDeliveryTime);
            }
            proto.write(1112396529676L, alarmManagerService.mNextNonWakeup - nowElapsed2);
            proto.write(1112396529677L, alarmManagerService.mNextWakeup - nowElapsed2);
            proto.write(1112396529678L, nowElapsed2 - alarmManagerService.mLastWakeup);
            proto.write(1112396529679L, nowElapsed2 - alarmManagerService.mLastWakeupSet);
            proto.write(1112396529680L, alarmManagerService.mNumTimeChanged);
            TreeSet<Integer> users = new TreeSet();
            int nextAlarmClockForUserSize3 = alarmManagerService.mNextAlarmClockForUser.size();
            for (i = 0; i < nextAlarmClockForUserSize3; i++) {
                users.add(Integer.valueOf(alarmManagerService.mNextAlarmClockForUser.keyAt(i)));
            }
            int pendingSendNextAlarmClockChangedForUserSize = alarmManagerService.mPendingSendNextAlarmClockChangedForUser.size();
            for (i = 0; i < pendingSendNextAlarmClockChangedForUserSize; i++) {
                users.add(Integer.valueOf(alarmManagerService.mPendingSendNextAlarmClockChangedForUser.keyAt(i)));
            }
            Iterator it = users.iterator();
            while (it.hasNext()) {
                int user = ((Integer) it.next()).intValue();
                AlarmClockInfo next = (AlarmClockInfo) alarmManagerService.mNextAlarmClockForUser.get(user);
                long time = next != null ? next.getTriggerTime() : 0;
                boolean pendingSend = alarmManagerService.mPendingSendNextAlarmClockChangedForUser.get(user);
                Iterator it2 = it;
                long aToken = proto.start(2246267895826L);
                nextAlarmClockForUserSize = nextAlarmClockForUserSize3;
                proto.write(1120986464257L, user);
                proto.write(1133871366146L, pendingSend);
                nowRTC = nowRTC2;
                proto.write(1112396529667L, time);
                proto.end(aToken);
                long j = 1112396529667L;
                it = it2;
                nextAlarmClockForUserSize3 = nextAlarmClockForUserSize;
                nowRTC2 = nowRTC;
                FileDescriptor fileDescriptor = fd;
            }
            nextAlarmClockForUserSize = nextAlarmClockForUserSize3;
            nowRTC = nowRTC2;
            long j2 = 1120986464257L;
            Iterator it3 = alarmManagerService.mAlarmBatches.iterator();
            while (it3.hasNext()) {
                int pendingSendNextAlarmClockChangedForUserSize2 = pendingSendNextAlarmClockChangedForUserSize;
                nowRTC2 = j2;
                nextAlarmClockForUserSize2 = nextAlarmClockForUserSize;
                nowElapsed = nowElapsed2;
                ((Batch) it3.next()).writeToProto(proto, 2246267895827L, nowElapsed2, nowRTC);
                j2 = nowRTC2;
                nextAlarmClockForUserSize = nextAlarmClockForUserSize2;
                nowElapsed2 = nowElapsed;
                pendingSendNextAlarmClockChangedForUserSize = pendingSendNextAlarmClockChangedForUserSize2;
            }
            nowRTC2 = j2;
            nowElapsed = nowElapsed2;
            nextAlarmClockForUserSize2 = nextAlarmClockForUserSize;
            for (i2 = 0; i2 < alarmManagerService.mPendingBackgroundAlarms.size(); i2++) {
                ArrayList<Alarm> blockedAlarms = (ArrayList) alarmManagerService.mPendingBackgroundAlarms.valueAt(i2);
                if (blockedAlarms != null) {
                    Iterator it4 = blockedAlarms.iterator();
                    while (it4.hasNext()) {
                        ArrayList<Alarm> blockedAlarms2 = blockedAlarms;
                        Iterator it5 = it4;
                        ((Alarm) it4.next()).writeToProto(proto, 2246267895828L, nowElapsed, nowRTC);
                        blockedAlarms = blockedAlarms2;
                        it4 = it5;
                    }
                }
            }
            if (alarmManagerService.mPendingIdleUntil != null) {
                alarmManagerService.mPendingIdleUntil.writeToProto(proto, 1146756268053L, nowElapsed, nowRTC);
            }
            it3 = alarmManagerService.mPendingWhileIdleAlarms.iterator();
            while (it3.hasNext()) {
                ((Alarm) it3.next()).writeToProto(proto, 2246267895830L, nowElapsed, nowRTC);
            }
            if (alarmManagerService.mNextWakeFromIdle != null) {
                alarmManagerService.mNextWakeFromIdle.writeToProto(proto, 1146756268055L, nowElapsed, nowRTC);
            }
            it3 = alarmManagerService.mPendingNonWakeupAlarms.iterator();
            while (it3.hasNext()) {
                ((Alarm) it3.next()).writeToProto(proto, 2246267895832L, nowElapsed, nowRTC);
            }
            proto.write(1120986464281L, alarmManagerService.mNumDelayedAlarms);
            proto.write(1112396529690L, alarmManagerService.mTotalDelayTime);
            proto.write(1112396529691L, alarmManagerService.mMaxDelayTime);
            proto.write(1112396529692L, alarmManagerService.mNonInteractiveTime);
            proto.write(1120986464285L, alarmManagerService.mBroadcastRefCount);
            proto.write(1120986464286L, alarmManagerService.mSendCount);
            proto.write(1120986464287L, alarmManagerService.mSendFinishCount);
            proto.write(1120986464288L, alarmManagerService.mListenerCount);
            proto.write(1120986464289L, alarmManagerService.mListenerFinishCount);
            it3 = alarmManagerService.mInFlight.iterator();
            while (it3.hasNext()) {
                ((InFlight) it3.next()).writeToProto(proto, 2246267895842L);
            }
            i2 = 0;
            while (i2 < alarmManagerService.mLastAllowWhileIdleDispatch.size()) {
                long token = proto.start(2246267895844L);
                uid = alarmManagerService.mLastAllowWhileIdleDispatch.keyAt(i2);
                j2 = alarmManagerService.mLastAllowWhileIdleDispatch.valueAt(i2);
                proto.write(nowRTC2, uid);
                proto.write(1112396529666L, j2);
                proto.write(1112396529667L, j2 + alarmManagerService.getWhileIdleMinIntervalLocked(uid));
                proto.end(token);
                i2++;
                nowRTC2 = 1120986464257L;
            }
            for (i2 = 0; i2 < alarmManagerService.mUseAllowWhileIdleShortTime.size(); i2++) {
                if (alarmManagerService.mUseAllowWhileIdleShortTime.valueAt(i2)) {
                    proto.write(2220498092067L, alarmManagerService.mUseAllowWhileIdleShortTime.keyAt(i2));
                }
            }
            alarmManagerService.mLog.writeToProto(proto, 1146756268069L);
            FilterStats[] topFilters = new FilterStats[10];
            Comparator<FilterStats> comparator = new Comparator<FilterStats>() {
                public int compare(FilterStats lhs, FilterStats rhs) {
                    if (lhs.aggregateTime < rhs.aggregateTime) {
                        return 1;
                    }
                    if (lhs.aggregateTime > rhs.aggregateTime) {
                        return -1;
                    }
                    return 0;
                }
            };
            uid = 0;
            pendingSendNextAlarmClockChangedForUserSize = 0;
            while (pendingSendNextAlarmClockChangedForUserSize < alarmManagerService.mBroadcastStats.size()) {
                uidStats = (ArrayMap) alarmManagerService.mBroadcastStats.valueAt(pendingSendNextAlarmClockChangedForUserSize);
                nextAlarmClockForUserSize3 = uid;
                uid = 0;
                while (uid < uidStats.size()) {
                    BroadcastStats bs = (BroadcastStats) uidStats.valueAt(uid);
                    int len = nextAlarmClockForUserSize3;
                    nextAlarmClockForUserSize3 = 0;
                    while (nextAlarmClockForUserSize3 < bs.filterStats.size()) {
                        TreeSet<Integer> users2;
                        FilterStats fs = (FilterStats) bs.filterStats.valueAt(nextAlarmClockForUserSize3);
                        int pos = len > 0 ? Arrays.binarySearch(topFilters, 0, len, fs, comparator) : 0;
                        if (pos < 0) {
                            pos = (-pos) - 1;
                        }
                        if (pos < topFilters.length) {
                            copylen = (topFilters.length - pos) - 1;
                            if (copylen > 0) {
                                users2 = users;
                                System.arraycopy(topFilters, pos, topFilters, pos + 1, copylen);
                            } else {
                                users2 = users;
                            }
                            topFilters[pos] = fs;
                            if (len < topFilters.length) {
                                len++;
                            }
                        } else {
                            users2 = users;
                        }
                        nextAlarmClockForUserSize3++;
                        users = users2;
                    }
                    uid++;
                    nextAlarmClockForUserSize3 = len;
                }
                pendingSendNextAlarmClockChangedForUserSize++;
                uid = nextAlarmClockForUserSize3;
            }
            for (int i3 = 0; i3 < uid; i3++) {
                j2 = proto.start(2246267895846L);
                FilterStats fs2 = topFilters[i3];
                proto.write(1120986464257L, fs2.mBroadcastStats.mUid);
                proto.write(1138166333442L, fs2.mBroadcastStats.mPackageName);
                fs2.writeToProto(proto, 1146756268035L);
                proto.end(j2);
            }
            ArrayList<FilterStats> tmpFilters = new ArrayList();
            pendingSendNextAlarmClockChangedForUserSize = 0;
            while (pendingSendNextAlarmClockChangedForUserSize < alarmManagerService.mBroadcastStats.size()) {
                uidStats = (ArrayMap) alarmManagerService.mBroadcastStats.valueAt(pendingSendNextAlarmClockChangedForUserSize);
                nextAlarmClockForUserSize3 = 0;
                while (nextAlarmClockForUserSize3 < uidStats.size()) {
                    ArrayList<FilterStats> tmpFilters2;
                    long token2 = proto.start(2246267895847L);
                    BroadcastStats bs2 = (BroadcastStats) uidStats.valueAt(nextAlarmClockForUserSize3);
                    bs2.writeToProto(proto, 1146756268033L);
                    tmpFilters.clear();
                    for (copylen = 0; copylen < bs2.filterStats.size(); copylen++) {
                        tmpFilters.add((FilterStats) bs2.filterStats.valueAt(copylen));
                    }
                    Collections.sort(tmpFilters, comparator);
                    Iterator it6 = tmpFilters.iterator();
                    while (it6.hasNext()) {
                        tmpFilters2 = tmpFilters;
                        ((FilterStats) it6.next()).writeToProto(proto, 2);
                        tmpFilters = tmpFilters2;
                    }
                    tmpFilters2 = tmpFilters;
                    proto.end(token2);
                    nextAlarmClockForUserSize3++;
                    tmpFilters = tmpFilters2;
                }
                pendingSendNextAlarmClockChangedForUserSize++;
                alarmManagerService = this;
            }
        }
        proto.flush();
    }

    private void logBatchesLocked(SimpleDateFormat sdf) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream(2048);
        PrintWriter pw = new PrintWriter(bs);
        long nowRTC = System.currentTimeMillis();
        long nowELAPSED = SystemClock.elapsedRealtime();
        int NZ = this.mAlarmBatches.size();
        int iz = 0;
        while (true) {
            int iz2 = iz;
            if (iz2 < NZ) {
                Batch bz = (Batch) this.mAlarmBatches.get(iz2);
                pw.append("Batch ");
                pw.print(iz2);
                pw.append(": ");
                pw.println(bz);
                int iz3 = iz2;
                dumpAlarmList(pw, bz.alarms, "  ", nowELAPSED, nowRTC, sdf);
                pw.flush();
                Slog.v(TAG, bs.toString());
                bs.reset();
                iz = iz3 + 1;
            } else {
                return;
            }
        }
    }

    private boolean validateConsistencyLocked() {
        return true;
    }

    private Batch findFirstWakeupBatchLocked() {
        int N = this.mAlarmBatches.size();
        int i = 0;
        while (i < N) {
            try {
                Batch b = (Batch) this.mAlarmBatches.get(i);
                if (b.hasWakeups()) {
                    return b;
                }
                i++;
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "Do nothing");
            }
        }
        return null;
    }

    private long findFirstWakeupEndLocked() {
        int N = this.mAlarmBatches.size();
        long lastWakeupEnd = -1;
        for (int i = 0; i < N; i++) {
            try {
                Batch b = (Batch) this.mAlarmBatches.get(i);
                if (b.hasWakeups()) {
                    int size = b.alarms.size();
                    for (int j = 0; j < size; j++) {
                        Alarm a = (Alarm) b.alarms.get(j);
                        if (a.wakeup) {
                            if (lastWakeupEnd == -1) {
                                lastWakeupEnd = a.maxWhenElapsed;
                            } else if (a.whenElapsed > lastWakeupEnd) {
                                return lastWakeupEnd;
                            } else {
                                if (a.maxWhenElapsed < lastWakeupEnd) {
                                    lastWakeupEnd = a.maxWhenElapsed;
                                }
                            }
                        }
                    }
                    continue;
                } else {
                    continue;
                }
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "Do nothing");
            }
        }
        return lastWakeupEnd;
    }

    long getNextWakeFromIdleTimeImpl() {
        long j;
        synchronized (this.mLock) {
            j = this.mNextWakeFromIdle != null ? this.mNextWakeFromIdle.whenElapsed : JobStatus.NO_LATEST_RUNTIME;
        }
        return j;
    }

    AlarmClockInfo getNextAlarmClockImpl(int userId) {
        AlarmClockInfo alarmClockInfo;
        synchronized (this.mLock) {
            alarmClockInfo = (AlarmClockInfo) this.mNextAlarmClockForUser.get(userId);
        }
        return alarmClockInfo;
    }

    void updateNextAlarmClockLocked() {
        if (this.mNextAlarmClockMayChange) {
            int i;
            int M;
            int i2 = 0;
            this.mNextAlarmClockMayChange = false;
            SparseArray<AlarmClockInfo> nextForUser = this.mTmpSparseAlarmClockArray;
            nextForUser.clear();
            int N = this.mAlarmBatches.size();
            for (i = 0; i < N; i++) {
                ArrayList<Alarm> alarms = ((Batch) this.mAlarmBatches.get(i)).alarms;
                M = alarms.size();
                for (int j = 0; j < M; j++) {
                    Alarm a = (Alarm) alarms.get(j);
                    if (a.alarmClock != null) {
                        int userId = UserHandle.getUserId(a.uid);
                        AlarmClockInfo current = (AlarmClockInfo) this.mNextAlarmClockForUser.get(userId);
                        if (nextForUser.get(userId) == null) {
                            nextForUser.put(userId, a.alarmClock);
                        } else if (a.alarmClock.equals(current) && current.getTriggerTime() <= ((AlarmClockInfo) nextForUser.get(userId)).getTriggerTime()) {
                            nextForUser.put(userId, current);
                        }
                    }
                }
            }
            i = nextForUser.size();
            while (i2 < i) {
                AlarmClockInfo newAlarm = (AlarmClockInfo) nextForUser.valueAt(i2);
                M = nextForUser.keyAt(i2);
                if (!newAlarm.equals((AlarmClockInfo) this.mNextAlarmClockForUser.get(M))) {
                    updateNextAlarmInfoForUserLocked(M, newAlarm);
                }
                i2++;
            }
            for (int i3 = this.mNextAlarmClockForUser.size() - 1; i3 >= 0; i3--) {
                M = this.mNextAlarmClockForUser.keyAt(i3);
                if (nextForUser.get(M) == null) {
                    updateNextAlarmInfoForUserLocked(M, null);
                }
            }
        }
    }

    private void updateNextAlarmInfoForUserLocked(int userId, AlarmClockInfo alarmClock) {
        if (alarmClock != null) {
            this.mNextAlarmClockForUser.put(userId, alarmClock);
        } else {
            this.mNextAlarmClockForUser.remove(userId);
        }
        this.mPendingSendNextAlarmClockChangedForUser.put(userId, true);
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    private void sendNextAlarmClockChanged() {
        int N;
        int i;
        SparseArray<AlarmClockInfo> pendingUsers = this.mHandlerSparseAlarmClockArray;
        pendingUsers.clear();
        synchronized (this.mLock) {
            N = this.mPendingSendNextAlarmClockChangedForUser.size();
            i = 0;
            for (int i2 = 0; i2 < N; i2++) {
                int userId = this.mPendingSendNextAlarmClockChangedForUser.keyAt(i2);
                pendingUsers.append(userId, (AlarmClockInfo) this.mNextAlarmClockForUser.get(userId));
            }
            this.mPendingSendNextAlarmClockChangedForUser.clear();
        }
        int N2 = pendingUsers.size();
        while (true) {
            N = i;
            if (N < N2) {
                i = pendingUsers.keyAt(N);
                System.putStringForUser(getContext().getContentResolver(), "next_alarm_formatted", formatNextAlarm(getContext(), (AlarmClockInfo) pendingUsers.valueAt(N), i), i);
                getContext().sendBroadcastAsUser(NEXT_ALARM_CLOCK_CHANGED_INTENT, new UserHandle(i));
                i = N + 1;
            } else {
                return;
            }
        }
    }

    private static String formatNextAlarm(Context context, AlarmClockInfo info, int userId) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, userId) ? "EHm" : "Ehma");
        if (info == null) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    void rescheduleKernelAlarmsLocked() {
        long nextNonWakeup = 0;
        if (this.mAlarmBatches.size() > 0) {
            Batch firstBatch = (Batch) this.mAlarmBatches.get(0);
            if (this.mIsAlarmDataOnlyMode || !isAwareAlarmManagerEnabled()) {
                Batch firstWakeup = findFirstWakeupBatchLocked();
                if (firstWakeup != null) {
                    this.mNextWakeup = firstWakeup.start;
                    this.mLastWakeupSet = SystemClock.elapsedRealtime();
                    setLocked(2, firstWakeup.start);
                }
                if (firstBatch != firstWakeup) {
                    nextNonWakeup = firstBatch.start;
                }
            } else {
                long firstWakeupEnd = findFirstWakeupEndLocked();
                if (firstWakeupEnd != -1) {
                    this.mNextWakeup = firstWakeupEnd;
                    this.mLastWakeupSet = SystemClock.elapsedRealtime();
                    setLocked(2, firstWakeupEnd);
                }
                if (firstBatch.start != firstWakeupEnd) {
                    nextNonWakeup = firstBatch.start;
                }
            }
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && (nextNonWakeup == 0 || this.mNextNonWakeupDeliveryTime < nextNonWakeup)) {
            nextNonWakeup = this.mNextNonWakeupDeliveryTime;
        }
        if (nextNonWakeup != 0) {
            if ("factory".equals(SystemProperties.get("ro.runmode", "normal")) && nextNonWakeup == this.mNextWakeup) {
                Flog.w(500, "no need set for the time had been set by type 2");
            } else {
                this.mNextNonWakeup = nextNonWakeup;
                setLocked(3, nextNonWakeup);
            }
        }
    }

    void removeLocked(PendingIntent operation, IAlarmListener directReceiver) {
        if (operation != null || directReceiver != null) {
            int i;
            boolean didRemove = false;
            Predicate whichAlarms = new -$$Lambda$AlarmManagerService$ZVedZIeWdB3G6AGM0_-9P_GEO24(operation, directReceiver);
            for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
                Batch b = (Batch) this.mAlarmBatches.get(i);
                if (b != null) {
                    didRemove |= b.remove(whichAlarms);
                    if (b.size() == 0) {
                        this.mAlarmBatches.remove(i);
                    }
                }
            }
            for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
                if (((Alarm) this.mPendingWhileIdleAlarms.get(i)).matches(operation, directReceiver)) {
                    this.mPendingWhileIdleAlarms.remove(i);
                }
            }
            for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
                ArrayList<Alarm> alarmsForUid = (ArrayList) this.mPendingBackgroundAlarms.valueAt(i);
                for (int j = alarmsForUid.size() - 1; j >= 0; j--) {
                    if (((Alarm) alarmsForUid.get(j)).matches(operation, directReceiver)) {
                        alarmsForUid.remove(j);
                    }
                }
                if (alarmsForUid.size() == 0) {
                    this.mPendingBackgroundAlarms.removeAt(i);
                }
            }
            hwRemoveAnywayRtcAlarm(operation);
            if (didRemove) {
                boolean restorePending = false;
                if (this.mPendingIdleUntil != null && this.mPendingIdleUntil.matches(operation, directReceiver)) {
                    this.mPendingIdleUntil = null;
                    restorePending = true;
                }
                if (this.mNextWakeFromIdle != null && this.mNextWakeFromIdle.matches(operation, directReceiver)) {
                    this.mNextWakeFromIdle = null;
                }
                rebatchAllAlarmsLocked(true);
                if (restorePending) {
                    restorePendingWhileIdleAlarmsLocked();
                }
                updateNextAlarmClockLocked();
            }
        }
    }

    static /* synthetic */ boolean lambda$removeLocked$0(PendingIntent operation, IAlarmListener directReceiver, Alarm a) {
        if (!a.matches(operation, directReceiver) && !HwActivityManagerServiceUtil.isPendingIntentCanceled(a.operation)) {
            return false;
        }
        if (operation != null && (a.packageName == null || !a.packageName.equals("com.google.android.gms"))) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove alarm:");
            stringBuilder.append(a);
            stringBuilder.append(" according to operation:");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(operation)));
            Slog.i(str, stringBuilder.toString());
        }
        return true;
    }

    void removeLocked(int uid) {
        if (uid == 1000) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeLocked: Shouldn't for UID=");
            stringBuilder.append(uid);
            Slog.wtf(str, stringBuilder.toString());
            return;
        }
        int i;
        boolean didRemove = false;
        Predicate whichAlarms = new -$$Lambda$AlarmManagerService$qehVSjTLWvtJYPGgKh2mkJ6ePnk(uid);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("remove ");
        stringBuilder2.append(whichAlarms);
        stringBuilder2.append(" according to uid:");
        stringBuilder2.append(uid);
        Slog.i(str2, stringBuilder2.toString());
        for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            didRemove |= b.remove(whichAlarms);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
            if (((Alarm) this.mPendingWhileIdleAlarms.get(i)).uid == uid) {
                this.mPendingWhileIdleAlarms.remove(i);
            }
        }
        for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
            ArrayList<Alarm> alarmsForUid = (ArrayList) this.mPendingBackgroundAlarms.valueAt(i);
            for (int j = alarmsForUid.size() - 1; j >= 0; j--) {
                if (((Alarm) alarmsForUid.get(j)).uid == uid) {
                    alarmsForUid.remove(j);
                }
            }
            if (alarmsForUid.size() == 0) {
                this.mPendingBackgroundAlarms.removeAt(i);
            }
        }
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static /* synthetic */ boolean lambda$removeLocked$1(int uid, Alarm a) {
        return a.uid == uid;
    }

    public void cleanupAlarmLocked(ArrayList<String> array) {
        if (array != null && array.size() != 0) {
            Message msg = Message.obtain();
            msg.what = 100;
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("clearlist", array);
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
        }
    }

    void removeLocked(String packageName) {
        removeLocked(packageName, null);
    }

    void removeLocked(String packageName, String action) {
        if (packageName != null && !packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            int i;
            boolean didRemove = false;
            Predicate whichAlarms = new -$$Lambda$AlarmManagerService$wKpZgVEkOm7Eyq4brSTAkfjCjTg(packageName, action);
            boolean oldHasTick = haveBatchesTimeTickAlarm(this.mAlarmBatches);
            for (int i2 = this.mAlarmBatches.size() - 1; i2 >= 0; i2--) {
                Batch b = (Batch) this.mAlarmBatches.get(i2);
                didRemove |= b.remove(whichAlarms);
                if (b.size() == 0) {
                    this.mAlarmBatches.remove(i2);
                }
            }
            boolean newHasTick = haveBatchesTimeTickAlarm(this.mAlarmBatches);
            if (oldHasTick != newHasTick) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeLocked: hasTick changed from ");
                stringBuilder.append(oldHasTick);
                stringBuilder.append(" to ");
                stringBuilder.append(newHasTick);
                Slog.wtf(str, stringBuilder.toString());
            }
            for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
                if (((Alarm) this.mPendingWhileIdleAlarms.get(i)).matches(packageName)) {
                    this.mPendingWhileIdleAlarms.remove(i);
                }
            }
            for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
                ArrayList<Alarm> alarmsForUid = (ArrayList) this.mPendingBackgroundAlarms.valueAt(i);
                for (int j = alarmsForUid.size() - 1; j >= 0; j--) {
                    if (((Alarm) alarmsForUid.get(j)).matches(packageName)) {
                        alarmsForUid.remove(j);
                    }
                }
                if (alarmsForUid.size() == 0) {
                    this.mPendingBackgroundAlarms.removeAt(i);
                }
            }
            if (didRemove) {
                rebatchAllAlarmsLocked(true);
                rescheduleKernelAlarmsLocked();
                updateNextAlarmClockLocked();
            }
        }
    }

    static /* synthetic */ boolean lambda$removeLocked$2(String packageName, String action, Alarm a) {
        if (!a.matches(packageName)) {
            return false;
        }
        if (action != null && (a.operation == null || !action.equals(resetActionCallingIdentity(a.operation)))) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove ");
        stringBuilder.append(a);
        stringBuilder.append(" according to ");
        stringBuilder.append(packageName);
        stringBuilder.append(" (");
        stringBuilder.append(action);
        stringBuilder.append(")");
        Slog.i(str, stringBuilder.toString());
        return true;
    }

    void removeLocked(int uid, String packageName) {
        if (uid == 1000) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeLocked: Shouldn't for UID=");
            stringBuilder.append(uid);
            Slog.wtf(str, stringBuilder.toString());
        } else if (packageName != null && !packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            int i;
            boolean didRemove = false;
            Predicate whichAlarms = new -$$Lambda$AlarmManagerService$eXOI2Us5i1sXicR2X5TTRDNki9w(uid, packageName);
            for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
                Batch b = (Batch) this.mAlarmBatches.get(i);
                didRemove |= b.remove(whichAlarms);
                if (b.size() == 0) {
                    this.mAlarmBatches.remove(i);
                }
            }
            for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
                Alarm a = (Alarm) this.mPendingWhileIdleAlarms.get(i);
                if (a.uid == uid && a.matches(packageName)) {
                    this.mPendingWhileIdleAlarms.remove(i);
                }
            }
            for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
                ArrayList<Alarm> alarmsForUid = (ArrayList) this.mPendingBackgroundAlarms.valueAt(i);
                int j = alarmsForUid.size() - 1;
                while (j >= 0) {
                    if (((Alarm) alarmsForUid.get(j)).uid == uid && ((Alarm) alarmsForUid.get(j)).matches(packageName)) {
                        alarmsForUid.remove(j);
                    }
                    j--;
                }
                if (alarmsForUid.size() == 0) {
                    this.mPendingBackgroundAlarms.removeAt(i);
                }
            }
            if (didRemove) {
                rebatchAllAlarmsLocked(true);
                rescheduleKernelAlarmsLocked();
                updateNextAlarmClockLocked();
            }
        }
    }

    static /* synthetic */ boolean lambda$removeLocked$3(int uid, String packageName, Alarm a) {
        if (a.uid != uid || !a.matches(packageName)) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove ");
        stringBuilder.append(a);
        stringBuilder.append(" according to ");
        stringBuilder.append(packageName);
        stringBuilder.append(" and ");
        stringBuilder.append(uid);
        Slog.i(str, stringBuilder.toString());
        return true;
    }

    void removeForStoppedLocked(int uid) {
        if (uid == 1000) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeForStoppedLocked: Shouldn't for UID=");
            stringBuilder.append(uid);
            Slog.wtf(str, stringBuilder.toString());
            return;
        }
        int i;
        boolean didRemove = false;
        Predicate whichAlarms = new -$$Lambda$AlarmManagerService$d1Nr3qXE-1WItEvvEEG1KMB46xw(uid);
        for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            didRemove |= b.remove(whichAlarms);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
            if (((Alarm) this.mPendingWhileIdleAlarms.get(i)).uid == uid) {
                this.mPendingWhileIdleAlarms.remove(i);
            }
        }
        for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
            if (this.mPendingBackgroundAlarms.keyAt(i) == uid) {
                this.mPendingBackgroundAlarms.removeAt(i);
            }
        }
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static /* synthetic */ boolean lambda$removeForStoppedLocked$4(int uid, Alarm a) {
        try {
            if (a.uid == uid && ActivityManager.getService().isAppStartModeDisabled(uid, a.packageName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove ");
                stringBuilder.append(a);
                stringBuilder.append(" according to uid:");
                stringBuilder.append(uid);
                Slog.i(str, stringBuilder.toString());
                return true;
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    void removeUserLocked(int userHandle) {
        if (userHandle == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeForStoppedLocked: Shouldn't for user=");
            stringBuilder.append(userHandle);
            Slog.wtf(str, stringBuilder.toString());
            return;
        }
        int i;
        boolean didRemove = false;
        Predicate whichAlarms = new -$$Lambda$AlarmManagerService$AyzIPVIMvB7gtaOddkJLWSr87BU(userHandle);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("remove ");
        stringBuilder2.append(whichAlarms);
        stringBuilder2.append(" according to userHandle:");
        stringBuilder2.append(userHandle);
        Slog.i(str2, stringBuilder2.toString());
        for (i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = (Batch) this.mAlarmBatches.get(i);
            didRemove |= b.remove(whichAlarms);
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        for (i = this.mPendingWhileIdleAlarms.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(((Alarm) this.mPendingWhileIdleAlarms.get(i)).creatorUid) == userHandle) {
                this.mPendingWhileIdleAlarms.remove(i);
            }
        }
        for (i = this.mPendingBackgroundAlarms.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mPendingBackgroundAlarms.keyAt(i)) == userHandle) {
                this.mPendingBackgroundAlarms.removeAt(i);
            }
        }
        for (i = this.mLastAllowWhileIdleDispatch.size() - 1; i >= 0; i--) {
            if (UserHandle.getUserId(this.mLastAllowWhileIdleDispatch.keyAt(i)) == userHandle) {
                this.mLastAllowWhileIdleDispatch.removeAt(i);
            }
        }
        if (didRemove) {
            rebatchAllAlarmsLocked(true);
            rescheduleKernelAlarmsLocked();
            updateNextAlarmClockLocked();
        }
    }

    static /* synthetic */ boolean lambda$removeUserLocked$5(int userHandle, Alarm a) {
        return UserHandle.getUserId(a.creatorUid) == userHandle;
    }

    void interactiveStateChangedLocked(boolean interactive) {
        if (this.mInteractive != interactive) {
            this.mInteractive = interactive;
            long nowELAPSED = SystemClock.elapsedRealtime();
            if (interactive) {
                long thisDelayTime;
                if (this.mPendingNonWakeupAlarms.size() > 0) {
                    thisDelayTime = nowELAPSED - this.mStartCurrentDelayTime;
                    this.mTotalDelayTime += thisDelayTime;
                    if (this.mMaxDelayTime < thisDelayTime) {
                        this.mMaxDelayTime = thisDelayTime;
                    }
                    deliverAlarmsLocked(this.mPendingNonWakeupAlarms, nowELAPSED);
                    this.mPendingNonWakeupAlarms.clear();
                }
                if (this.mNonInteractiveStartTime > 0) {
                    thisDelayTime = nowELAPSED - this.mNonInteractiveStartTime;
                    if (thisDelayTime > this.mNonInteractiveTime) {
                        this.mNonInteractiveTime = thisDelayTime;
                        return;
                    }
                    return;
                }
                return;
            }
            this.mNonInteractiveStartTime = nowELAPSED;
        }
    }

    boolean lookForPackageLocked(String packageName) {
        int i;
        for (i = 0; i < this.mAlarmBatches.size(); i++) {
            if (((Batch) this.mAlarmBatches.get(i)).hasPackage(packageName)) {
                return true;
            }
        }
        for (i = 0; i < this.mPendingWhileIdleAlarms.size(); i++) {
            if (((Alarm) this.mPendingWhileIdleAlarms.get(i)).matches(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void setLocked(int type, long when) {
        if (this.mNativeData != 0) {
            long alarmSeconds;
            long alarmNanoseconds;
            if (when < 0) {
                alarmSeconds = 0;
                alarmNanoseconds = 0;
            } else {
                alarmNanoseconds = 1000 * ((when % 1000) * 1000);
                alarmSeconds = when / 1000;
            }
            int result = set(this.mNativeData, type, alarmSeconds, alarmNanoseconds);
            if (result != 0) {
                long nowElapsed = SystemClock.elapsedRealtime();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to set kernel alarm, now=");
                stringBuilder.append(nowElapsed);
                stringBuilder.append(" type=");
                stringBuilder.append(type);
                stringBuilder.append(" when=");
                stringBuilder.append(when);
                stringBuilder.append(" @ (");
                stringBuilder.append(alarmSeconds);
                stringBuilder.append(",");
                stringBuilder.append(alarmNanoseconds);
                stringBuilder.append("), ret = ");
                stringBuilder.append(result);
                stringBuilder.append(" = ");
                stringBuilder.append(Os.strerror(result));
                Slog.wtf(str, stringBuilder.toString());
                return;
            }
            return;
        }
        Message msg = Message.obtain();
        msg.what = 1;
        this.mHandler.removeMessages(1);
        this.mHandler.sendMessageAtTime(msg, when);
    }

    private static final void dumpAlarmList(PrintWriter pw, ArrayList<Alarm> list, String prefix, String label, long nowELAPSED, long nowRTC, SimpleDateFormat sdf) {
        PrintWriter printWriter = pw;
        String str = prefix;
        int i = list.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                Alarm a = (Alarm) list.get(i2);
                printWriter.print(str);
                printWriter.print(label);
                printWriter.print(" #");
                printWriter.print(i2);
                printWriter.print(": ");
                printWriter.println(a);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append("  ");
                a.dump(printWriter, stringBuilder.toString(), nowELAPSED, nowRTC, sdf);
                i = i2 - 1;
            } else {
                ArrayList<Alarm> arrayList = list;
                String str2 = label;
                return;
            }
        }
    }

    private static final String labelForType(int type) {
        switch (type) {
            case 0:
                return "RTC_WAKEUP";
            case 1:
                return "RTC";
            case 2:
                return "ELAPSED_WAKEUP";
            case 3:
                return "ELAPSED";
            default:
                return "--unknown--";
        }
    }

    private static final void dumpAlarmList(PrintWriter pw, ArrayList<Alarm> list, String prefix, long nowELAPSED, long nowRTC, SimpleDateFormat sdf) {
        PrintWriter printWriter = pw;
        String str = prefix;
        int i = list.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                Alarm a = (Alarm) list.get(i2);
                String label = labelForType(a.type);
                printWriter.print(str);
                printWriter.print(label);
                printWriter.print(" #");
                printWriter.print(i2);
                printWriter.print(": ");
                printWriter.println(a);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append("  ");
                a.dump(printWriter, stringBuilder.toString(), nowELAPSED, nowRTC, sdf);
                i = i2 - 1;
            } else {
                ArrayList<Alarm> arrayList = list;
                return;
            }
        }
    }

    private boolean isBackgroundRestricted(Alarm alarm) {
        boolean z = true;
        boolean exemptOnBatterySaver = (alarm.flags & 4) != 0;
        if (alarm.alarmClock != null) {
            return false;
        }
        if (alarm.operation != null) {
            if (alarm.operation.isActivity()) {
                return false;
            }
            if (alarm.operation.isForegroundService()) {
                exemptOnBatterySaver = true;
            }
        }
        String sourcePackage = alarm.sourcePackage;
        int sourceUid = alarm.creatorUid;
        if (this.mAppStateTracker == null || !this.mAppStateTracker.areAlarmsRestricted(sourceUid, sourcePackage, exemptOnBatterySaver)) {
            z = false;
        }
        return z;
    }

    private long getWhileIdleMinIntervalLocked(int uid) {
        boolean ebs = false;
        boolean dozing = this.mPendingIdleUntil != null;
        if (this.mAppStateTracker != null && this.mAppStateTracker.isForceAllAppsStandbyEnabled()) {
            ebs = true;
        }
        if (!dozing && !ebs) {
            return this.mConstants.ALLOW_WHILE_IDLE_SHORT_TIME;
        }
        if (dozing) {
            return this.mConstants.ALLOW_WHILE_IDLE_LONG_TIME;
        }
        if (this.mUseAllowWhileIdleShortTime.get(uid)) {
            return this.mConstants.ALLOW_WHILE_IDLE_SHORT_TIME;
        }
        return this.mConstants.ALLOW_WHILE_IDLE_LONG_TIME;
    }

    /* JADX WARNING: Missing block: B:44:0x00ee, code:
            if (2 == r7.type) goto L_0x00f2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean triggerAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED, long nowRTC) {
        int i;
        AlarmManagerService alarmManagerService;
        AlarmManagerService alarmManagerService2 = this;
        ArrayList arrayList = triggerList;
        int i2 = 0;
        boolean hasWakeup = false;
        while (true) {
            int i3 = 500;
            if (alarmManagerService2.mAlarmBatches.size() <= 0) {
                break;
            }
            Batch batch = (Batch) alarmManagerService2.mAlarmBatches.get(i2);
            if (batch.start > nowELAPSED) {
                break;
            }
            ArrayList<Alarm> arrayList2;
            alarmManagerService2.mAlarmBatches.remove(i2);
            int N = batch.size();
            boolean hasWakeup2 = hasWakeup;
            int i4 = i2;
            while (true) {
                int i5 = i4;
                if (i5 >= N) {
                    break;
                }
                long minTime;
                int i6;
                Batch batch2;
                int N2;
                Alarm alarm = batch.get(i5);
                if ((alarm.flags & 4) != 0) {
                    long lastTime = alarmManagerService2.mLastAllowWhileIdleDispatch.get(alarm.creatorUid, -1);
                    minTime = lastTime + alarmManagerService2.getWhileIdleMinIntervalLocked(alarm.creatorUid);
                    if (lastTime >= 0 && nowELAPSED < minTime) {
                        alarm.whenElapsed = minTime;
                        alarm.expectedWhenElapsed = minTime;
                        if (alarm.maxWhenElapsed < minTime) {
                            alarm.maxWhenElapsed = minTime;
                        }
                        alarm.expectedMaxWhenElapsed = alarm.maxWhenElapsed;
                        alarmManagerService2.setImplLocked(alarm, true, i2);
                        i6 = i5;
                        batch2 = batch;
                        N2 = N;
                        i = i2;
                        alarmManagerService = alarmManagerService2;
                        i4 = i6 + 1;
                        arrayList2 = triggerList;
                        alarmManagerService2 = alarmManagerService;
                        i2 = i;
                        batch = batch2;
                        N = N2;
                        i3 = 500;
                    }
                }
                if (alarmManagerService2.isBackgroundRestricted(alarm)) {
                    ArrayList<Alarm> alarmsForUid = (ArrayList) alarmManagerService2.mPendingBackgroundAlarms.get(alarm.creatorUid);
                    if (alarmsForUid == null) {
                        alarmsForUid = new ArrayList();
                        alarmManagerService2.mPendingBackgroundAlarms.put(alarm.creatorUid, alarmsForUid);
                    }
                    alarmsForUid.add(alarm);
                    i6 = i5;
                    batch2 = batch;
                    N2 = N;
                    i = i2;
                    alarmManagerService = alarmManagerService2;
                    i4 = i6 + 1;
                    arrayList2 = triggerList;
                    alarmManagerService2 = alarmManagerService;
                    i2 = i;
                    batch = batch2;
                    N = N2;
                    i3 = 500;
                } else {
                    Alarm alarm2;
                    alarm.count = 1;
                    arrayList.add(alarm);
                    if ((alarm.flags & 2) != 0) {
                        EventLogTags.writeDeviceIdleWakeFromIdle(alarmManagerService2.mPendingIdleUntil != null ? 1 : i2, alarm.statsTag);
                    }
                    if (alarmManagerService2.mPendingIdleUntil == alarm) {
                        alarmManagerService2.mPendingIdleUntil = null;
                        alarmManagerService2.rebatchAllAlarmsLocked(i2);
                        restorePendingWhileIdleAlarmsLocked();
                    }
                    if (alarmManagerService2.mNextWakeFromIdle == alarm) {
                        alarmManagerService2.mNextWakeFromIdle = null;
                        alarmManagerService2.rebatchAllAlarmsLocked(i2);
                    }
                    if (alarm.repeatInterval > 0) {
                        alarm.count = (int) (((long) alarm.count) + ((nowELAPSED - alarm.expectedWhenElapsed) / alarm.repeatInterval));
                        long delta = ((long) alarm.count) * alarm.repeatInterval;
                        long nextElapsed = alarm.whenElapsed + delta;
                        if (alarm.type != 0) {
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("set again repeat alarm: next ,nextElapsed = ");
                        stringBuilder.append(nextElapsed);
                        stringBuilder.append(",repeatInterval = ");
                        stringBuilder.append(alarm.repeatInterval);
                        Flog.i(i3, stringBuilder.toString());
                        int i7 = alarm.type;
                        long j = alarm.when + delta;
                        long j2 = alarm.windowLength;
                        int i8 = i7;
                        long nextElapsed2 = nextElapsed;
                        minTime = maxTriggerTime(nowELAPSED, nextElapsed, alarm.repeatInterval);
                        nextElapsed = alarm.repeatInterval;
                        PendingIntent pendingIntent = alarm.operation;
                        int i9 = alarm.flags;
                        WorkSource workSource = alarm.workSource;
                        WorkSource workSource2 = workSource;
                        int i10 = i8;
                        PendingIntent pendingIntent2 = pendingIntent;
                        i6 = i5;
                        alarm2 = alarm;
                        batch2 = batch;
                        N2 = N;
                        i = i2;
                        alarmManagerService2.setImplLocked(i10, j, nextElapsed2, j2, minTime, nextElapsed, pendingIntent2, null, null, i9, true, workSource2, alarm.alarmClock, alarm.uid, alarm.packageName);
                    } else {
                        i6 = i5;
                        alarm2 = alarm;
                        batch2 = batch;
                        N2 = N;
                        i = i2;
                    }
                    Alarm alarm3 = alarm2;
                    if (alarm3.wakeup) {
                        hasWakeup2 = true;
                    }
                    if (alarm3.alarmClock != null) {
                        alarmManagerService = this;
                        alarmManagerService.mNextAlarmClockMayChange = true;
                        i4 = i6 + 1;
                        arrayList2 = triggerList;
                        alarmManagerService2 = alarmManagerService;
                        i2 = i;
                        batch = batch2;
                        N = N2;
                        i3 = 500;
                    } else {
                        alarmManagerService = this;
                        i4 = i6 + 1;
                        arrayList2 = triggerList;
                        alarmManagerService2 = alarmManagerService;
                        i2 = i;
                        batch = batch2;
                        N = N2;
                        i3 = 500;
                    }
                }
            }
            i = i2;
            alarmManagerService = alarmManagerService2;
            arrayList2 = triggerList;
            hasWakeup = hasWakeup2;
        }
        i = i2;
        alarmManagerService = alarmManagerService2;
        alarmManagerService.mCurrentSeq++;
        calculateDeliveryPriorities(triggerList);
        ArrayList<Alarm> arrayList3 = triggerList;
        Collections.sort(arrayList3, alarmManagerService.mAlarmDispatchComparator);
        ArrayList<Alarm> wakeupAlarms = new ArrayList();
        while (true) {
            int i11 = i;
            if (i11 >= triggerList.size()) {
                break;
            }
            Alarm talarm = (Alarm) arrayList3.get(i11);
            if (talarm.type != 0) {
                if (2 != talarm.type) {
                    i = i11 + 1;
                }
            }
            wakeupAlarms.add(talarm);
            StringBuilder alarmInfo = new StringBuilder();
            if (talarm.operation != null) {
                Intent intent = resetIntentCallingIdentity(talarm.operation);
                alarmInfo.append(" tag: ");
                alarmInfo.append(talarm.statsTag);
                alarmInfo.append(" window:");
                alarmInfo.append(talarm.windowLength);
                alarmInfo.append(" originWhen:");
                alarmInfo.append(convertToElapsed(talarm.when, talarm.type));
                alarmInfo.append(" when:");
                alarmInfo.append(talarm.whenElapsed);
                alarmInfo.append(" maxWhen:");
                alarmInfo.append(talarm.maxWhenElapsed);
                String triggerAction;
                if (intent != null) {
                    triggerAction = resetActionCallingIdentity(talarm.operation);
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mIsScreenOn is: ");
                    stringBuilder2.append(alarmManagerService.mIsScreenOn);
                    stringBuilder2.append(", WAKEUP alarm trigger action = ");
                    stringBuilder2.append(triggerAction);
                    stringBuilder2.append(" package name is: ");
                    stringBuilder2.append(talarm.packageName);
                    stringBuilder2.append(alarmInfo.toString());
                    Log.w(str, stringBuilder2.toString());
                } else {
                    triggerAction = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mIsScreenOn is: ");
                    stringBuilder3.append(alarmManagerService.mIsScreenOn);
                    stringBuilder3.append(", WAKEUP alarm intent == null and  package name is: ");
                    stringBuilder3.append(talarm.packageName);
                    stringBuilder3.append(" listenerTag is: ");
                    stringBuilder3.append(talarm.listenerTag);
                    stringBuilder3.append(" createor uid is: ");
                    stringBuilder3.append(talarm.creatorUid);
                    stringBuilder3.append(alarmInfo.toString());
                    Log.w(triggerAction, stringBuilder3.toString());
                }
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("mIsScreenOn is: ");
                stringBuilder4.append(alarmManagerService.mIsScreenOn);
                stringBuilder4.append(", WAKEUP alarm talarm.operation == null,package name is: ");
                stringBuilder4.append(talarm.packageName);
                stringBuilder4.append(" listenerTag is: ");
                stringBuilder4.append(talarm.listenerTag);
                stringBuilder4.append(" creator uid is: ");
                stringBuilder4.append(talarm.creatorUid);
                stringBuilder4.append(alarmInfo.toString());
                Log.w(str2, stringBuilder4.toString());
            }
            i = i11 + 1;
        }
        if (wakeupAlarms.size() > 0) {
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("Alarm triggering (type 0 or 2): ");
            stringBuilder5.append(wakeupAlarms);
            Flog.i(500, stringBuilder5.toString());
        }
        return hasWakeup;
    }

    void recordWakeupAlarms(ArrayList<Batch> batches, long nowELAPSED, long nowRTC) {
        int numBatches = batches.size();
        int nextBatch = 0;
        while (nextBatch < numBatches) {
            Batch b = (Batch) batches.get(nextBatch);
            if (b.start <= nowELAPSED) {
                int numAlarms = b.alarms.size();
                for (int nextAlarm = 0; nextAlarm < numAlarms; nextAlarm++) {
                    this.mRecentWakeups.add(((Alarm) b.alarms.get(nextAlarm)).makeWakeupEvent(nowRTC));
                }
                nextBatch++;
            } else {
                return;
            }
        }
    }

    long currentNonWakeupFuzzLocked(long nowELAPSED) {
        long timeSinceOn = nowELAPSED - this.mNonInteractiveStartTime;
        if (timeSinceOn < BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
            return JobStatus.DEFAULT_TRIGGER_MAX_DELAY;
        }
        if (timeSinceOn < 1800000) {
            return 900000;
        }
        return SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
    }

    static int fuzzForDuration(long duration) {
        if (duration < 900000) {
            return (int) duration;
        }
        if (duration < 5400000) {
            return 900000;
        }
        return 1800000;
    }

    boolean checkAllowNonWakeupDelayLocked(long nowELAPSED) {
        boolean z = false;
        if (this.mInteractive || this.mLastAlarmDeliveryTime <= 0) {
            return false;
        }
        if (this.mPendingNonWakeupAlarms.size() > 0 && this.mNextNonWakeupDeliveryTime < nowELAPSED) {
            return false;
        }
        if (nowELAPSED - this.mLastAlarmDeliveryTime <= currentNonWakeupFuzzLocked(nowELAPSED)) {
            z = true;
        }
        return z;
    }

    void deliverAlarmsLocked(ArrayList<Alarm> triggerList, long nowELAPSED) {
        long j = nowELAPSED;
        this.mLastAlarmDeliveryTime = j;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < triggerList.size()) {
                Alarm alarm = (Alarm) triggerList.get(i2);
                if (alarm == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("uid=0 name=NULL type=-1 tag=NULL size=");
                    stringBuilder.append(triggerList.size());
                    stringBuilder.append(" worksource=0");
                    HwLog.dubaie("DUBAI_TAG_ALARM_TRIGGER", stringBuilder.toString());
                } else {
                    boolean allowWhileIdle = (alarm.flags & 4) != 0;
                    StringBuilder stringBuilder2;
                    if (alarm.wakeup) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Dispatch wakeup alarm to ");
                        stringBuilder2.append(alarm.packageName);
                        Trace.traceBegin(131072, stringBuilder2.toString());
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Dispatch non-wakeup alarm to ");
                        stringBuilder2.append(alarm.packageName);
                        Trace.traceBegin(131072, stringBuilder2.toString());
                    }
                    try {
                        String packageName;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("sending alarm ");
                        stringBuilder3.append(alarm);
                        stringBuilder3.append(",repeatInterval = ");
                        stringBuilder3.append(alarm.repeatInterval);
                        stringBuilder3.append(",listenerTag =");
                        stringBuilder3.append(alarm.listenerTag);
                        Flog.i(500, stringBuilder3.toString());
                        if (alarm.operation == null) {
                            packageName = alarm.packageName;
                        } else {
                            packageName = alarm.operation.getTargetPackage();
                        }
                        int hasWorkSource = 0;
                        if (alarm.workSource != null && alarm.workSource.size() > 0) {
                            hasWorkSource = 1;
                        }
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("uid=");
                        stringBuilder4.append(alarm.uid);
                        stringBuilder4.append(" name=");
                        stringBuilder4.append(packageName);
                        stringBuilder4.append(" type=");
                        stringBuilder4.append(alarm.type);
                        stringBuilder4.append(" tag=");
                        stringBuilder4.append(alarm.statsTag);
                        stringBuilder4.append(" size=");
                        stringBuilder4.append(triggerList.size());
                        stringBuilder4.append(" worksource=");
                        stringBuilder4.append(hasWorkSource);
                        HwLog.dubaie("DUBAI_TAG_ALARM_TRIGGER", stringBuilder4.toString());
                        if (hasWorkSource == 1) {
                            int wi = 0;
                            while (wi < alarm.workSource.size()) {
                                String str = "DUBAI_TAG_ALARM_WORKSOURCE";
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("uid=");
                                stringBuilder5.append(alarm.workSource.get(wi));
                                stringBuilder5.append(" name=");
                                stringBuilder5.append(alarm.workSource.getName(wi));
                                stringBuilder5.append(" tag=");
                                stringBuilder5.append(alarm.statsTag);
                                stringBuilder5.append(" finished=");
                                stringBuilder5.append(wi == alarm.workSource.size() - 1 ? "1" : "0");
                                HwLog.dubaie(str, stringBuilder5.toString());
                                wi++;
                            }
                        }
                        ActivityManager.noteAlarmStart(alarm.operation, alarm.workSource, alarm.uid, alarm.statsTag);
                        this.mDeliveryTracker.deliverLocked(alarm, j, allowWhileIdle);
                    } catch (RuntimeException e) {
                        Slog.w(TAG, "Failure sending alarm.", e);
                    }
                    Trace.traceEnd(131072);
                }
                i = i2 + 1;
            } else {
                ArrayList<Alarm> arrayList = triggerList;
                return;
            }
        }
    }

    private boolean isExemptFromAppStandby(Alarm a) {
        return (a.alarmClock == null && !UserHandle.isCore(a.creatorUid) && (a.flags & 8) == 0) ? false : true;
    }

    void setWakelockWorkSource(PendingIntent pi, WorkSource ws, int type, String tag, int knownUid, boolean first) {
        try {
            boolean unimportant = pi == this.mTimeTickSender;
            this.mWakeLock.setUnimportantForLogging(unimportant);
            if (first || this.mLastWakeLockUnimportantForLogging) {
                this.mWakeLock.setHistoryTag(tag);
            } else {
                this.mWakeLock.setHistoryTag(null);
            }
            this.mLastWakeLockUnimportantForLogging = unimportant;
            if (ws != null) {
                this.mWakeLock.setWorkSource(ws);
                return;
            }
            int uid = knownUid >= 0 ? knownUid : ActivityManager.getService().getUidForIntentSender(pi.getTarget());
            if (uid >= 0) {
                this.mWakeLock.setWorkSource(new WorkSource(uid));
                return;
            }
            this.mWakeLock.setWorkSource(null);
        } catch (Exception e) {
        }
    }

    private final BroadcastStats getStatsLocked(PendingIntent pi) {
        return getStatsLocked(pi.getCreatorUid(), pi.getCreatorPackage());
    }

    private final BroadcastStats getStatsLocked(int uid, String pkgName) {
        ArrayMap<String, BroadcastStats> uidStats = (ArrayMap) this.mBroadcastStats.get(uid);
        if (uidStats == null) {
            uidStats = new ArrayMap();
            this.mBroadcastStats.put(uid, uidStats);
        }
        BroadcastStats bs = (BroadcastStats) uidStats.get(pkgName);
        if (bs != null) {
            return bs;
        }
        bs = new BroadcastStats(uid, pkgName);
        uidStats.put(pkgName, bs);
        return bs;
    }

    public ArrayList<Batch> getmAlarmBatches() {
        return this.mAlarmBatches;
    }

    protected void removeDeskClockFromFWK(PendingIntent operation) {
    }

    private static String resetActionCallingIdentity(PendingIntent operation) {
        long identity = Binder.clearCallingIdentity();
        String action = null;
        try {
            action = operation.getIntent().getAction();
        } catch (Throwable th) {
        }
        Binder.restoreCallingIdentity(identity);
        return action;
    }

    private static Intent resetIntentCallingIdentity(PendingIntent operation) {
        long identity = Binder.clearCallingIdentity();
        Intent intent = null;
        try {
            intent = operation.getIntent();
        } catch (Throwable th) {
        }
        Binder.restoreCallingIdentity(identity);
        return intent;
    }
}

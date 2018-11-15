package com.android.server.usage;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IUidObserver;
import android.app.IUidObserver.Stub;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.admin.DevicePolicyManagerInternal;
import android.app.usage.AppStandbyInfo;
import android.app.usage.ConfigurationStats;
import android.app.usage.EventStats;
import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.IHwShutdownThread;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class UsageStatsService extends SystemService implements StatsUpdatedListener {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final boolean ENABLE_KERNEL_UPDATES = true;
    public static final boolean ENABLE_TIME_CHANGE_CORRECTION = SystemProperties.getBoolean("persist.debug.time_correction", true);
    private static final long FLUSH_INTERVAL = 1200000;
    private static final File KERNEL_COUNTER_FILE = new File("/proc/uid_procstat/set");
    static final int MSG_FLUSH_TO_DISK = 1;
    static final int MSG_REMOVE_USER = 2;
    static final int MSG_REPORT_EVENT = 0;
    static final int MSG_UID_STATE_CHANGED = 3;
    static final String TAG = "UsageStatsService";
    private static final long TEN_SECONDS = 10000;
    private static final long TIME_CHANGE_THRESHOLD_MILLIS = 2000;
    private static final long TWENTY_MINUTES = 1200000;
    AppOpsManager mAppOps;
    AppStandbyController mAppStandby;
    AppTimeLimitController mAppTimeLimit;
    IDeviceIdleController mDeviceIdleController;
    DevicePolicyManagerInternal mDpmInternal;
    Handler mHandler;
    private final Object mLock = new Object();
    PackageManager mPackageManager;
    PackageManagerInternal mPackageManagerInternal;
    PackageMonitor mPackageMonitor;
    long mRealTimeSnapshot;
    private AppIdleStateChangeListener mStandbyChangeListener = new AppIdleStateChangeListener() {
        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            Event event = new Event();
            event.mEventType = 11;
            event.mBucketAndReason = (bucket << 16) | (NetworkConstants.ARP_HWTYPE_RESERVED_HI & reason);
            event.mPackage = packageName;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void onParoleStateChanged(boolean isParoleOn) {
        }
    };
    long mSystemTimeSnapshot;
    private final IUidObserver mUidObserver = new Stub() {
        public void onUidStateChanged(int uid, int procState, long procStateSeq) {
            UsageStatsService.this.mHandler.obtainMessage(3, uid, procState).sendToTarget();
        }

        public void onUidIdle(int uid, boolean disabled) {
        }

        public void onUidGone(int uid, boolean disabled) {
            onUidStateChanged(uid, 19, 0);
        }

        public void onUidActive(int uid) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }
    };
    private final SparseIntArray mUidToKernelCounter = new SparseIntArray();
    private File mUsageStatsDir;
    UserManager mUserManager;
    private final SparseArray<UserUsageStatsService> mUserState = new SparseArray();

    private final class BinderService extends IUsageStatsManager.Stub {
        private BinderService() {
        }

        /* synthetic */ BinderService(UsageStatsService x0, AnonymousClass1 x1) {
            this();
        }

        private boolean hasPermission(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            boolean z = true;
            if (callingUid == 1000) {
                return true;
            }
            int mode = UsageStatsService.this.mAppOps.noteOp(43, callingUid, callingPackage);
            if (mode == 3) {
                if (UsageStatsService.this.getContext().checkCallingPermission("android.permission.PACKAGE_USAGE_STATS") != 0) {
                    z = false;
                }
                return z;
            }
            if (mode != 0) {
                z = false;
            }
            return z;
        }

        private boolean hasObserverPermission(String callingPackage) {
            int callingUid = Binder.getCallingUid();
            DevicePolicyManagerInternal dpmInternal = UsageStatsService.this.getDpmInternal();
            boolean z = true;
            if (callingUid == 1000 || (dpmInternal != null && dpmInternal.isActiveAdminWithPolicy(callingUid, -1))) {
                return true;
            }
            if (UsageStatsService.this.getContext().checkCallingPermission("android.permission.OBSERVE_APP_USAGE") != 0) {
                z = false;
            }
            return z;
        }

        private void checkCallerIsSystemOrSameApp(String pkg) {
            if (!isCallingUidSystem()) {
                checkCallerIsSameApp(pkg);
            }
        }

        private void checkCallerIsSameApp(String pkg) {
            int callingUid = Binder.getCallingUid();
            if (UsageStatsService.this.mPackageManagerInternal.getPackageUid(pkg, 0, UserHandle.getUserId(callingUid)) != callingUid) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling uid ");
                stringBuilder.append(pkg);
                stringBuilder.append(" cannot query eventsfor package ");
                stringBuilder.append(pkg);
                throw new SecurityException(stringBuilder.toString());
            }
        }

        private boolean isCallingUidSystem() {
            return Binder.getCallingUid() == 1000;
        }

        public ParceledListSlice<UsageStats> queryUsageStats(int bucketType, long beginTime, long endTime, String callingPackage) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.USAGESTATES_QUERYUSAGESTATS);
            ParceledListSlice<UsageStats> parceledListSlice = null;
            if (!hasPermission(callingPackage)) {
                return null;
            }
            boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                List<UsageStats> results = UsageStatsService.this.queryUsageStats(userId, bucketType, beginTime, endTime, obfuscateInstantApps);
                if (results != null) {
                    parceledListSlice = new ParceledListSlice(results);
                    return parceledListSlice;
                }
                Binder.restoreCallingIdentity(token);
                return null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<ConfigurationStats> queryConfigurationStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
            ParceledListSlice<ConfigurationStats> parceledListSlice = null;
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                List<ConfigurationStats> results = UsageStatsService.this.queryConfigurationStats(userId, bucketType, beginTime, endTime);
                if (results != null) {
                    parceledListSlice = new ParceledListSlice(results);
                    return parceledListSlice;
                }
                Binder.restoreCallingIdentity(token);
                return null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public ParceledListSlice<EventStats> queryEventStats(int bucketType, long beginTime, long endTime, String callingPackage) throws RemoteException {
            ParceledListSlice<EventStats> parceledListSlice = null;
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                List<EventStats> results = UsageStatsService.this.queryEventStats(userId, bucketType, beginTime, endTime);
                if (results != null) {
                    parceledListSlice = new ParceledListSlice(results);
                    return parceledListSlice;
                }
                Binder.restoreCallingIdentity(token);
                return null;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEvents(long beginTime, long endTime, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
            int userId = UserHandle.getCallingUserId();
            long token = Binder.clearCallingIdentity();
            try {
                UsageEvents queryEvents = UsageStatsService.this.queryEvents(userId, beginTime, endTime, obfuscateInstantApps);
                return queryEvents;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForPackage(long beginTime, long endTime, String callingPackage) {
            int callingUserId = UserHandle.getUserId(Binder.getCallingUid());
            String str = callingPackage;
            checkCallerIsSameApp(str);
            long token = Binder.clearCallingIdentity();
            try {
                UsageEvents queryEventsForPackage = UsageStatsService.this.queryEventsForPackage(callingUserId, beginTime, endTime, str);
                return queryEventsForPackage;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForUser(long beginTime, long endTime, int userId, String callingPackage) {
            if (!hasPermission(callingPackage)) {
                return null;
            }
            int i = userId;
            if (i != UserHandle.getCallingUserId()) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), UserHandle.getCallingUserId());
            long token = Binder.clearCallingIdentity();
            try {
                UsageEvents queryEvents = UsageStatsService.this.queryEvents(i, beginTime, endTime, obfuscateInstantApps);
                return queryEvents;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public UsageEvents queryEventsForPackageForUser(long beginTime, long endTime, int userId, String pkg, String callingPackage) {
            String str = callingPackage;
            if (!hasPermission(str)) {
                return null;
            }
            int i = userId;
            if (i != UserHandle.getCallingUserId()) {
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to query usage stats for this user");
            }
            checkCallerIsSystemOrSameApp(pkg);
            long token = Binder.clearCallingIdentity();
            try {
                UsageEvents queryEventsForPackage = UsageStatsService.this.queryEventsForPackage(i, beginTime, endTime, str);
                return queryEventsForPackage;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isAppInactive(String packageName, int userId) {
            try {
                userId = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "isAppInactive", null);
                boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(Binder.getCallingUid(), userId);
                long token = Binder.clearCallingIdentity();
                try {
                    boolean isAppIdleFilteredOrParoled = UsageStatsService.this.mAppStandby.isAppIdleFilteredOrParoled(packageName, userId, SystemClock.elapsedRealtime(), obfuscateInstantApps);
                    return isAppIdleFilteredOrParoled;
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppInactive(String packageName, boolean idle, int userId) {
            try {
                userId = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setAppInactive", null);
                UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app idle state");
                long token = Binder.clearCallingIdentity();
                try {
                    if (UsageStatsService.this.mAppStandby.getAppId(packageName) >= 0) {
                        UsageStatsService.this.mAppStandby.setAppIdleAsync(packageName, idle, userId);
                        Binder.restoreCallingIdentity(token);
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public int getAppStandbyBucket(String packageName, String callingPackage, int userId) {
            int callingUid = Binder.getCallingUid();
            try {
                userId = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "getAppStandbyBucket", null);
                int packageUid = UsageStatsService.this.mPackageManagerInternal.getPackageUid(packageName, 0, userId);
                if (packageUid != callingUid && !hasPermission(callingPackage)) {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                } else if (packageUid >= 0) {
                    boolean obfuscateInstantApps = UsageStatsService.this.shouldObfuscateInstantAppsForCaller(callingUid, userId);
                    long token = Binder.clearCallingIdentity();
                    try {
                        int appStandbyBucket = UsageStatsService.this.mAppStandby.getAppStandbyBucket(packageName, userId, SystemClock.elapsedRealtime(), obfuscateInstantApps);
                        return appStandbyBucket;
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot get standby bucket for non existent package (");
                    stringBuilder.append(packageName);
                    stringBuilder.append(")");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBucket(String packageName, int bucket, int userId) {
            Throwable th;
            int i;
            String str = packageName;
            int i2 = bucket;
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            if (i2 < 10 || i2 > 50) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot set the standby bucket to ");
                stringBuilder.append(i2);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            int callingUid = Binder.getCallingUid();
            try {
                int i3;
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, "setAppStandbyBucket", null);
                boolean z = callingUid == 0 || callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME;
                boolean shellCaller = z;
                if (UserHandle.isCore(callingUid)) {
                    i3 = 1024;
                } else {
                    i3 = 1280;
                }
                int reason = i3;
                long token = Binder.clearCallingIdentity();
                int i4;
                try {
                    i3 = UsageStatsService.this.mPackageManagerInternal.getPackageUid(str, DumpState.DUMP_CHANGES, userId2);
                    if (i3 == callingUid) {
                        i4 = userId2;
                        callingUid = token;
                        throw new IllegalArgumentException("Cannot set your own standby bucket");
                    } else if (i3 >= 0) {
                        callingUid = token;
                        try {
                            UsageStatsService.this.mAppStandby.setAppStandbyBucket(str, userId2, i2, reason, SystemClock.elapsedRealtime(), shellCaller);
                            Binder.restoreCallingIdentity(callingUid);
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(callingUid);
                            throw th;
                        }
                    } else {
                        i4 = userId2;
                        callingUid = token;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cannot set standby bucket for non existent package (");
                        stringBuilder2.append(str);
                        stringBuilder2.append(")");
                        throw new IllegalArgumentException(stringBuilder2.toString());
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i = callingUid;
                    i4 = userId2;
                    callingUid = token;
                    Binder.restoreCallingIdentity(callingUid);
                    throw th;
                }
            } catch (RemoteException re) {
                i = callingUid;
                throw re.rethrowFromSystemServer();
            }
        }

        public ParceledListSlice<AppStandbyInfo> getAppStandbyBuckets(String callingPackageName, int userId) {
            try {
                userId = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, "getAppStandbyBucket", null);
                if (hasPermission(callingPackageName)) {
                    long token = Binder.clearCallingIdentity();
                    try {
                        ParceledListSlice<AppStandbyInfo> emptyList;
                        List<AppStandbyInfo> standbyBucketList = UsageStatsService.this.mAppStandby.getAppStandbyBuckets(userId);
                        if (standbyBucketList == null) {
                            emptyList = ParceledListSlice.emptyList();
                        } else {
                            emptyList = new ParceledListSlice(standbyBucketList);
                        }
                        Binder.restoreCallingIdentity(token);
                        return emptyList;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(token);
                    }
                } else {
                    throw new SecurityException("Don't have permission to query app standby bucket");
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void setAppStandbyBuckets(ParceledListSlice appBuckets, int userId) {
            UsageStatsService.this.getContext().enforceCallingPermission("android.permission.CHANGE_APP_IDLE_STATE", "No permission to change app standby state");
            int callingUid = Binder.getCallingUid();
            try {
                int i;
                int userId2 = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, true, "setAppStandbyBucket", null);
                boolean z = callingUid == 0 || callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME;
                boolean shellCaller = z;
                if (shellCaller) {
                    i = 1024;
                } else {
                    i = 1280;
                }
                int reason = i;
                long token = Binder.clearCallingIdentity();
                try {
                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    for (AppStandbyInfo bucketInfo : appBuckets.getList()) {
                        String packageName = bucketInfo.mPackageName;
                        int bucket = bucketInfo.mStandbyBucket;
                        int bucket2;
                        String str;
                        AppStandbyInfo appStandbyInfo;
                        if (bucket < 10 || bucket > 50) {
                            bucket2 = bucket;
                            str = packageName;
                            appStandbyInfo = bucketInfo;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Cannot set the standby bucket to ");
                            stringBuilder.append(bucket2);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        } else if (UsageStatsService.this.mPackageManagerInternal.getPackageUid(packageName, DumpState.DUMP_CHANGES, userId2) != callingUid) {
                            UsageStatsService.this.mAppStandby.setAppStandbyBucket(packageName, userId2, bucket, reason, elapsedRealtime, shellCaller);
                        } else {
                            bucket2 = bucket;
                            str = packageName;
                            appStandbyInfo = bucketInfo;
                            throw new IllegalArgumentException("Cannot set your own standby bucket");
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }

        public void whitelistAppTemporarily(String packageName, long duration, int userId) throws RemoteException {
            StringBuilder reason = new StringBuilder(32);
            reason.append("from:");
            UserHandle.formatUid(reason, Binder.getCallingUid());
            UsageStatsService.this.mDeviceIdleController.addPowerSaveTempWhitelistApp(packageName, duration, userId, reason.toString());
        }

        public void onCarrierPrivilegedAppsChanged() {
            UsageStatsService.this.getContext().enforceCallingOrSelfPermission("android.permission.BIND_CARRIER_SERVICES", "onCarrierPrivilegedAppsChanged can only be called by privileged apps.");
            UsageStatsService.this.mAppStandby.clearCarrierPrivilegedApps();
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(UsageStatsService.this.getContext(), UsageStatsService.TAG, pw)) {
                UsageStatsService.this.dump(args, pw);
            }
        }

        public void reportChooserSelection(String packageName, int userId, String contentType, String[] annotations, String action) {
            if (packageName == null) {
                Slog.w(UsageStatsService.TAG, "Event report user selecting a null package");
                return;
            }
            Event event = new Event();
            event.mPackage = packageName;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 9;
            event.mAction = action;
            event.mContentType = contentType;
            event.mContentAnnotations = annotations;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void registerAppUsageObserver(int observerId, String[] packages, long timeLimitMs, PendingIntent callbackIntent, String callingPackage) {
            Throwable th;
            String[] strArr = packages;
            if (!hasObserverPermission(callingPackage)) {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            } else if (strArr == null || strArr.length == 0) {
                throw new IllegalArgumentException("Must specify at least one package");
            } else if (callbackIntent != null) {
                int callingUid = Binder.getCallingUid();
                int userId = UserHandle.getUserId(callingUid);
                long token = Binder.clearCallingIdentity();
                long token2;
                try {
                    String[] strArr2 = strArr;
                    token2 = token;
                    try {
                        UsageStatsService.this.registerAppUsageObserver(callingUid, observerId, strArr2, timeLimitMs, callbackIntent, userId);
                        Binder.restoreCallingIdentity(token2);
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(token2);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    token2 = token;
                    Binder.restoreCallingIdentity(token2);
                    throw th;
                }
            } else {
                throw new NullPointerException("callbackIntent can't be null");
            }
        }

        public void unregisterAppUsageObserver(int observerId, String callingPackage) {
            if (hasObserverPermission(callingPackage)) {
                int callingUid = Binder.getCallingUid();
                int userId = UserHandle.getUserId(callingUid);
                long token = Binder.clearCallingIdentity();
                try {
                    UsageStatsService.this.unregisterAppUsageObserver(callingUid, observerId, userId);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new SecurityException("Caller doesn't have OBSERVE_APP_USAGE permission");
            }
        }
    }

    class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    UsageStatsService.this.reportEvent((Event) msg.obj, msg.arg1);
                    return;
                case 1:
                    UsageStatsService.this.flushToDisk();
                    return;
                case 2:
                    UsageStatsService.this.onUserRemoved(msg.arg1);
                    return;
                case 3:
                    int uid = msg.arg1;
                    int newCounter = msg.arg2 <= 2 ? 0 : 1;
                    synchronized (UsageStatsService.this.mUidToKernelCounter) {
                        if (newCounter != UsageStatsService.this.mUidToKernelCounter.get(uid, 0)) {
                            UsageStatsService.this.mUidToKernelCounter.put(uid, newCounter);
                            try {
                                File access$400 = UsageStatsService.KERNEL_COUNTER_FILE;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(uid);
                                stringBuilder.append(" ");
                                stringBuilder.append(newCounter);
                                FileUtils.stringToFile(access$400, stringBuilder.toString());
                            } catch (IOException e) {
                                String str = UsageStatsService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Failed to update counter set: ");
                                stringBuilder2.append(e);
                                Slog.w(str, stringBuilder2.toString());
                            }
                        }
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    private final class LocalService extends UsageStatsManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(UsageStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public void reportEvent(ComponentName component, int userId, int eventType) {
            if (component == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a component name");
                return;
            }
            Event event = new Event();
            event.mPackage = component.getPackageName();
            event.mClass = component.getClassName();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = eventType;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportEvent(ComponentName component, int userId, int eventType, int displayId) {
            if (component == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a component name");
                return;
            }
            Event event = new Event();
            event.mPackage = component.getPackageName();
            event.mClass = component.getClassName();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = eventType;
            event.mDisplayId = displayId;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportEvent(String packageName, int userId, int eventType) {
            if (packageName == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name");
                return;
            }
            Event event = new Event();
            event.mPackage = packageName;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = eventType;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportConfigurationChange(Configuration config, int userId) {
            if (config == null) {
                Slog.w(UsageStatsService.TAG, "Configuration event reported with a null config");
                return;
            }
            Event event = new Event();
            event.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 5;
            event.mConfiguration = new Configuration(config);
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportInterruptiveNotification(String packageName, String channelId, int userId) {
            if (packageName == null || channelId == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a channel ID");
                return;
            }
            Event event = new Event();
            event.mPackage = packageName.intern();
            event.mNotificationChannelId = channelId.intern();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 12;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportShortcutUsage(String packageName, String shortcutId, int userId) {
            if (packageName == null || shortcutId == null) {
                Slog.w(UsageStatsService.TAG, "Event reported without a package name or a shortcut ID");
                return;
            }
            Event event = new Event();
            event.mPackage = packageName.intern();
            event.mShortcutId = shortcutId.intern();
            event.mTimeStamp = SystemClock.elapsedRealtime();
            event.mEventType = 8;
            UsageStatsService.this.mHandler.obtainMessage(0, userId, 0, event).sendToTarget();
        }

        public void reportContentProviderUsage(String name, String packageName, int userId) {
            UsageStatsService.this.mAppStandby.postReportContentProviderUsage(name, packageName, userId);
        }

        public boolean isAppIdle(String packageName, int uidForAppId, int userId) {
            return UsageStatsService.this.mAppStandby.isAppIdleFiltered(packageName, uidForAppId, userId, SystemClock.elapsedRealtime());
        }

        public int getAppStandbyBucket(String packageName, int userId, long nowElapsed) {
            return UsageStatsService.this.mAppStandby.getAppStandbyBucket(packageName, userId, nowElapsed, false);
        }

        public int[] getIdleUidsForUser(int userId) {
            return UsageStatsService.this.mAppStandby.getIdleUidsForUser(userId);
        }

        public boolean isAppIdleParoleOn() {
            return UsageStatsService.this.mAppStandby.isParoledOrCharging();
        }

        public void prepareShutdown() {
            UsageStatsService.this.shutdown();
        }

        public void addAppIdleStateChangeListener(AppIdleStateChangeListener listener) {
            UsageStatsService.this.mAppStandby.addListener(listener);
            listener.onParoleStateChanged(isAppIdleParoleOn());
        }

        public void removeAppIdleStateChangeListener(AppIdleStateChangeListener listener) {
            UsageStatsService.this.mAppStandby.removeListener(listener);
        }

        public byte[] getBackupPayload(int user, String key) {
            synchronized (UsageStatsService.this.mLock) {
                if (user == 0) {
                    byte[] backupPayload = UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(user, UsageStatsService.this.checkAndGetTimeLocked()).getBackupPayload(key);
                    return backupPayload;
                }
                return null;
            }
        }

        public void applyRestoredPayload(int user, String key, byte[] payload) {
            synchronized (UsageStatsService.this.mLock) {
                if (user == 0) {
                    UsageStatsService.this.getUserDataAndInitializeIfNeededLocked(user, UsageStatsService.this.checkAndGetTimeLocked()).applyRestoredPayload(key, payload);
                }
            }
        }

        public List<UsageStats> queryUsageStatsForUser(int userId, int intervalType, long beginTime, long endTime, boolean obfuscateInstantApps) {
            return UsageStatsService.this.queryUsageStats(userId, intervalType, beginTime, endTime, obfuscateInstantApps);
        }

        public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
            UsageStatsService.this.mAppStandby.setLastJobRunTime(packageName, userId, elapsedRealtime);
        }

        public long getTimeSinceLastJobRun(String packageName, int userId) {
            return UsageStatsService.this.mAppStandby.getTimeSinceLastJobRun(packageName, userId);
        }

        public void reportAppJobState(String packageName, int userId, int numDeferredJobs, long timeSinceLastJobRun) {
        }

        public void onActiveAdminAdded(String packageName, int userId) {
            UsageStatsService.this.mAppStandby.addActiveDeviceAdmin(packageName, userId);
        }

        public void setActiveAdminApps(Set<String> packageNames, int userId) {
            UsageStatsService.this.mAppStandby.setActiveAdminApps(packageNames, userId);
        }

        public void onAdminDataAvailable() {
            UsageStatsService.this.mAppStandby.onAdminDataAvailable();
        }

        public void reportExemptedSyncScheduled(String packageName, int userId) {
            UsageStatsService.this.mAppStandby.postReportExemptedSyncScheduled(packageName, userId);
        }

        public void reportExemptedSyncStart(String packageName, int userId) {
            UsageStatsService.this.mAppStandby.postReportExemptedSyncStart(packageName, userId);
        }
    }

    private class UserActionsReceiver extends BroadcastReceiver {
        private UserActionsReceiver() {
        }

        /* synthetic */ UserActionsReceiver(UsageStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            String action = intent.getAction();
            if ("android.intent.action.USER_REMOVED".equals(action)) {
                if (userId >= 0) {
                    UsageStatsService.this.mHandler.obtainMessage(2, userId, 0).sendToTarget();
                }
            } else if ("android.intent.action.USER_STARTED".equals(action) && userId >= 0) {
                UsageStatsService.this.mAppStandby.postCheckIdleStates(userId);
            }
        }
    }

    public UsageStatsService(Context context) {
        super(context);
    }

    public void onStart() {
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mUserManager = (UserManager) getContext().getSystemService("user");
        this.mPackageManager = getContext().getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mHandler = new H(BackgroundThread.get().getLooper());
        this.mAppStandby = new AppStandbyController(getContext(), BackgroundThread.get().getLooper());
        this.mAppTimeLimit = new AppTimeLimitController(new -$$Lambda$UsageStatsService$VoLNrRDaTqGpWDfCW6NTYC92LRY(this), this.mHandler.getLooper());
        this.mAppStandby.addListener(this.mStandbyChangeListener);
        this.mUsageStatsDir = new File(new File(Environment.getDataDirectory(), "system"), "usagestats");
        this.mUsageStatsDir.mkdirs();
        if (this.mUsageStatsDir.exists()) {
            IntentFilter filter = new IntentFilter("android.intent.action.USER_REMOVED");
            filter.addAction("android.intent.action.USER_STARTED");
            getContext().registerReceiverAsUser(new UserActionsReceiver(this, null), UserHandle.ALL, filter, null, this.mHandler);
            synchronized (this.mLock) {
                cleanUpRemovedUsersLocked();
            }
            this.mRealTimeSnapshot = SystemClock.elapsedRealtime();
            this.mSystemTimeSnapshot = System.currentTimeMillis();
            publishLocalService(UsageStatsManagerInternal.class, new LocalService(this, null));
            publishBinderService("usagestats", new BinderService(this, null));
            getUserDataAndInitializeIfNeededLocked(0, this.mSystemTimeSnapshot);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Usage stats directory does not exist: ");
        stringBuilder.append(this.mUsageStatsDir.getAbsolutePath());
        throw new IllegalStateException(stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$onStart$0(UsageStatsService usageStatsService, int observerId, int userId, long timeLimit, long timeElapsed, PendingIntent callbackIntent) {
        Intent intent = new Intent();
        intent.putExtra("android.app.usage.extra.OBSERVER_ID", observerId);
        intent.putExtra("android.app.usage.extra.TIME_LIMIT", timeLimit);
        intent.putExtra("android.app.usage.extra.TIME_USED", timeElapsed);
        try {
            callbackIntent.send(usageStatsService.getContext(), 0, intent);
        } catch (CanceledException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't deliver callback: ");
            stringBuilder.append(callbackIntent);
            Slog.w(str, stringBuilder.toString());
        }
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mAppStandby.onBootPhase(phase);
            getDpmInternal();
            this.mDeviceIdleController = IDeviceIdleController.Stub.asInterface(ServiceManager.getService("deviceidle"));
            if (KERNEL_COUNTER_FILE.exists()) {
                try {
                    ActivityManager.getService().registerUidObserver(this.mUidObserver, 3, -1, null);
                    return;
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Missing procfs interface: ");
            stringBuilder.append(KERNEL_COUNTER_FILE);
            Slog.w(str, stringBuilder.toString());
        }
    }

    private DevicePolicyManagerInternal getDpmInternal() {
        if (this.mDpmInternal == null) {
            this.mDpmInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        }
        return this.mDpmInternal;
    }

    public void onStatsUpdated() {
        this.mHandler.sendEmptyMessageDelayed(1, 1200000);
    }

    public void onStatsReloaded() {
        this.mAppStandby.postOneTimeCheckIdleStates();
    }

    public void onNewUpdate(int userId) {
        this.mAppStandby.initializeDefaultsForSystemApps(userId);
    }

    private boolean shouldObfuscateInstantAppsForCaller(int callingUid, int userId) {
        return this.mPackageManagerInternal.canAccessInstantApps(callingUid, userId) ^ 1;
    }

    private void cleanUpRemovedUsersLocked() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        if (users == null || users.size() == 0) {
            throw new IllegalStateException("There can't be no users");
        }
        ArraySet<String> toDelete = new ArraySet();
        String[] fileNames = this.mUsageStatsDir.list();
        if (fileNames != null) {
            int i;
            toDelete.addAll(Arrays.asList(fileNames));
            int userCount = users.size();
            int i2 = 0;
            for (i = 0; i < userCount; i++) {
                toDelete.remove(Integer.toString(((UserInfo) users.get(i)).id));
            }
            i = toDelete.size();
            while (i2 < i) {
                deleteRecursively(new File(this.mUsageStatsDir, (String) toDelete.valueAt(i2)));
                i2++;
            }
        }
    }

    private static void deleteRecursively(File f) {
        File[] files = f.listFiles();
        if (files != null) {
            for (File subFile : files) {
                deleteRecursively(subFile);
            }
        }
        if (!f.delete()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to delete ");
            stringBuilder.append(f);
            Slog.e(str, stringBuilder.toString());
        }
    }

    private UserUsageStatsService getUserDataAndInitializeIfNeededLocked(int userId, long currentTimeMillis) {
        UserUsageStatsService service = (UserUsageStatsService) this.mUserState.get(userId);
        if (service != null) {
            return service;
        }
        service = new UserUsageStatsService(getContext(), userId, new File(this.mUsageStatsDir, Integer.toString(userId)), this);
        service.init(currentTimeMillis);
        this.mUserState.put(userId, service);
        return service;
    }

    private long checkAndGetTimeLocked() {
        long actualSystemTime = System.currentTimeMillis();
        long actualRealtime = SystemClock.elapsedRealtime();
        long expectedSystemTime = (actualRealtime - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
        long diffSystemTime = actualSystemTime - expectedSystemTime;
        if (Math.abs(diffSystemTime) > TIME_CHANGE_THRESHOLD_MILLIS && ENABLE_TIME_CHANGE_CORRECTION) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Time changed in UsageStats by ");
            stringBuilder.append(diffSystemTime / 1000);
            stringBuilder.append(" seconds");
            Slog.i(str, stringBuilder.toString());
            int userCount = this.mUserState.size();
            for (int i = 0; i < userCount; i++) {
                ((UserUsageStatsService) this.mUserState.valueAt(i)).onTimeChanged(expectedSystemTime, actualSystemTime);
            }
            this.mRealTimeSnapshot = actualRealtime;
            this.mSystemTimeSnapshot = actualSystemTime;
        }
        return actualSystemTime;
    }

    private void convertToSystemTimeLocked(Event event) {
        event.mTimeStamp = Math.max(0, event.mTimeStamp - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
    }

    void shutdown() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(0);
            flushToDiskLocked();
        }
    }

    void reportEvent(Event event, int userId) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            convertToSystemTimeLocked(event);
            if (event.getPackageName() != null && this.mPackageManagerInternal.isPackageEphemeral(userId, event.getPackageName())) {
                event.mFlags |= 1;
            }
            getUserDataAndInitializeIfNeededLocked(userId, timeNow).reportEvent(event);
            this.mAppStandby.reportEvent(event, elapsedRealtime, userId);
            switch (event.mEventType) {
                case 1:
                    this.mAppTimeLimit.moveToForeground(event.getPackageName(), event.getClassName(), userId);
                    break;
                case 2:
                    this.mAppTimeLimit.moveToBackground(event.getPackageName(), event.getClassName(), userId);
                    break;
            }
        }
    }

    void flushToDisk() {
        synchronized (this.mLock) {
            flushToDiskLocked();
        }
    }

    void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing user ");
            stringBuilder.append(userId);
            stringBuilder.append(" and all data.");
            Slog.i(str, stringBuilder.toString());
            this.mUserState.remove(userId);
            this.mAppStandby.onUserRemoved(userId);
            this.mAppTimeLimit.onUserRemoved(userId);
            cleanUpRemovedUsersLocked();
        }
    }

    /* JADX WARNING: Missing block: B:19:0x004e, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    List<UsageStats> queryUsageStats(int userId, int bucketType, long beginTime, long endTime, boolean obfuscateInstantApps) {
        int i = userId;
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (validRange(timeNow, beginTime, endTime)) {
                List<UsageStats> list = getUserDataAndInitializeIfNeededLocked(i, timeNow).queryUsageStats(bucketType, beginTime, endTime);
                if (list == null) {
                    return null;
                } else if (obfuscateInstantApps) {
                    for (int i2 = list.size() - 1; i2 >= 0; i2--) {
                        UsageStats stats = (UsageStats) list.get(i2);
                        if (this.mPackageManagerInternal.isPackageEphemeral(i, stats.mPackageName)) {
                            list.set(i2, stats.getObfuscatedForInstantApp());
                        }
                    }
                }
            } else {
                return null;
            }
        }
    }

    List<ConfigurationStats> queryConfigurationStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (validRange(timeNow, beginTime, endTime)) {
                List<ConfigurationStats> queryConfigurationStats = getUserDataAndInitializeIfNeededLocked(userId, timeNow).queryConfigurationStats(bucketType, beginTime, endTime);
                return queryConfigurationStats;
            }
            return null;
        }
    }

    List<EventStats> queryEventStats(int userId, int bucketType, long beginTime, long endTime) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (validRange(timeNow, beginTime, endTime)) {
                List<EventStats> queryEventStats = getUserDataAndInitializeIfNeededLocked(userId, timeNow).queryEventStats(bucketType, beginTime, endTime);
                return queryEventStats;
            }
            return null;
        }
    }

    UsageEvents queryEvents(int userId, long beginTime, long endTime, boolean shouldObfuscateInstantApps) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (validRange(timeNow, beginTime, endTime)) {
                UsageEvents queryEvents = getUserDataAndInitializeIfNeededLocked(userId, timeNow).queryEvents(beginTime, endTime, shouldObfuscateInstantApps);
                return queryEvents;
            }
            return null;
        }
    }

    UsageEvents queryEventsForPackage(int userId, long beginTime, long endTime, String packageName) {
        synchronized (this.mLock) {
            long timeNow = checkAndGetTimeLocked();
            if (validRange(timeNow, beginTime, endTime)) {
                UsageEvents queryEventsForPackage = getUserDataAndInitializeIfNeededLocked(userId, timeNow).queryEventsForPackage(beginTime, endTime, packageName);
                return queryEventsForPackage;
            }
            return null;
        }
    }

    private static boolean validRange(long currentTime, long beginTime, long endTime) {
        return beginTime <= currentTime && beginTime < endTime;
    }

    private void flushToDiskLocked() {
        int userCount = this.mUserState.size();
        for (int i = 0; i < userCount; i++) {
            ((UserUsageStatsService) this.mUserState.valueAt(i)).persistActiveStats();
            this.mAppStandby.flushToDisk(this.mUserState.keyAt(i));
        }
        this.mAppStandby.flushDurationsToDisk();
        this.mHandler.removeMessages(1);
    }

    void dump(String[] args, PrintWriter pw) {
        synchronized (this.mLock) {
            boolean compact;
            boolean checkin;
            IndentingPrintWriter idpw = new IndentingPrintWriter(pw, "  ");
            String pkg = null;
            int i = 0;
            if (args != null) {
                compact = false;
                checkin = false;
                for (String arg : args) {
                    if (!"--checkin".equals(arg)) {
                        if (!"-c".equals(arg)) {
                            if (!"flush".equals(arg)) {
                                if (!"is-app-standby-enabled".equals(arg)) {
                                    if (!(arg == null || arg.startsWith("-"))) {
                                        pkg = arg;
                                        break;
                                    }
                                }
                                pw.println(this.mAppStandby.mAppIdleEnabled);
                                return;
                            }
                            flushToDiskLocked();
                            pw.println("Flushed stats to disk");
                            return;
                        }
                        compact = true;
                    } else {
                        checkin = true;
                    }
                }
            } else {
                compact = false;
                checkin = false;
            }
            int userCount = this.mUserState.size();
            while (i < userCount) {
                int userId = this.mUserState.keyAt(i);
                idpw.printPair("user", Integer.valueOf(userId));
                idpw.println();
                idpw.increaseIndent();
                if (checkin) {
                    ((UserUsageStatsService) this.mUserState.valueAt(i)).checkin(idpw);
                } else {
                    ((UserUsageStatsService) this.mUserState.valueAt(i)).dump(idpw, pkg, compact);
                    idpw.println();
                }
                this.mAppStandby.dumpUser(idpw, userId, pkg);
                idpw.decreaseIndent();
                i++;
            }
            if (pkg == null) {
                pw.println();
                this.mAppStandby.dumpState(args, pw);
            }
            this.mAppTimeLimit.dump(pw);
        }
    }

    void registerAppUsageObserver(int callingUid, int observerId, String[] packages, long timeLimitMs, PendingIntent callbackIntent, int userId) {
        this.mAppTimeLimit.addObserver(callingUid, observerId, packages, timeLimitMs, callbackIntent, userId);
    }

    void unregisterAppUsageObserver(int callingUid, int observerId, int userId) {
        this.mAppTimeLimit.removeObserver(callingUid, observerId, userId);
    }
}

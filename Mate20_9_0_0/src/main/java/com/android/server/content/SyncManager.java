package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.accounts.AccountManagerInternal;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncAdapter;
import android.content.ISyncAdapterUnsyncableAccountCallback;
import android.content.ISyncContext.Stub;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.ServiceConnection;
import android.content.SyncActivityTooManyDeletes;
import android.content.SyncAdapterType;
import android.content.SyncAdaptersCache;
import android.content.SyncInfo;
import android.content.SyncResult;
import android.content.SyncStats;
import android.content.SyncStatusInfo;
import android.content.SyncStatusInfo.Stats;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.content.pm.RegisteredServicesCache.ServiceInfo;
import android.content.pm.RegisteredServicesCacheListener;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.BitmapFactory;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallback;
import android.os.RemoteCallback.OnResultListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.text.format.Time;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.function.QuadConsumer;
import com.android.server.DeviceIdleController.LocalService;
import com.android.server.LocalServices;
import com.android.server.accounts.AccountManagerService;
import com.android.server.backup.AccountSyncSettingsBackupHelper;
import com.android.server.content.SyncStorageEngine.AuthorityInfo;
import com.android.server.content.SyncStorageEngine.DayStats;
import com.android.server.content.SyncStorageEngine.EndPoint;
import com.android.server.content.SyncStorageEngine.SyncHistoryItem;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class SyncManager extends AbsSyncManager {
    private static final boolean DEBUG_ACCOUNT_ACCESS = false;
    private static final int DELAY_RETRY_SYNC_IN_PROGRESS_IN_SECONDS = 10;
    private static final boolean ENABLE_SUSPICIOUS_CHECK = Build.IS_DEBUGGABLE;
    private static final String HANDLE_SYNC_ALARM_WAKE_LOCK = "SyncManagerHandleSyncAlarm";
    private static final AccountAndUser[] INITIAL_ACCOUNTS_ARRAY = new AccountAndUser[0];
    private static final long LOCAL_SYNC_DELAY = SystemProperties.getLong("sync.local_sync_delay", 30000);
    private static final int MAX_SYNC_JOB_ID = 110000;
    private static final int MIN_SYNC_JOB_ID = 100000;
    private static final int SYNC_ADAPTER_CONNECTION_FLAGS = 21;
    private static final long SYNC_DELAY_ON_CONFLICT = 10000;
    private static final long SYNC_DELAY_ON_LOW_STORAGE = 3600000;
    private static final long SYNC_DELAY_ON_PROVISION = 600000;
    private static final String SYNC_LOOP_WAKE_LOCK = "SyncLoopWakeLock";
    private static final int SYNC_MONITOR_PROGRESS_THRESHOLD_BYTES = 10;
    private static final long SYNC_MONITOR_WINDOW_LENGTH_MILLIS = 60000;
    private static final int SYNC_OP_STATE_INVALID = 1;
    private static final int SYNC_OP_STATE_INVALID_NO_ACCOUNT_ACCESS = 2;
    private static final int SYNC_OP_STATE_VALID = 0;
    private static final String SYNC_WAKE_LOCK_PREFIX = "*sync*/";
    static final String TAG = "SyncManager";
    @GuardedBy("SyncManager.class")
    private static SyncManager sInstance;
    private static final Comparator<SyncOperation> sOpDumpComparator = -$$Lambda$SyncManager$bVs0A6OYdmGkOiq_lbp5MiBwelw.INSTANCE;
    private static final Comparator<SyncOperation> sOpRuntimeComparator = -$$Lambda$SyncManager$68MEyNkTh36YmYoFlURJoRa_-cY.INSTANCE;
    private final AccountManager mAccountManager;
    private final AccountManagerInternal mAccountManagerInternal;
    private final BroadcastReceiver mAccountsUpdatedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.updateRunningAccounts(new EndPoint(null, null, getSendingUserId()));
        }
    };
    protected final ArrayList<ActiveSyncContext> mActiveSyncContexts = Lists.newArrayList();
    private final IBatteryStats mBatteryStats;
    private volatile boolean mBootCompleted = false;
    private final BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            SyncManager.this.mBootCompleted = true;
            SyncManager.this.verifyJobScheduler();
            SyncManager.this.mSyncHandler.onBootCompleted();
        }
    };
    private ConnectivityManager mConnManagerDoNotUseDirectly;
    private BroadcastReceiver mConnectivityIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean wasConnected = SyncManager.this.mDataConnectionIsConnected;
            SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
            if (SyncManager.this.mDataConnectionIsConnected && !wasConnected) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Reconnection detected: clearing all backoffs");
                }
                SyncManager.this.clearAllBackoffs("network reconnect");
            }
        }
    };
    private final SyncManagerConstants mConstants;
    private Context mContext;
    private volatile boolean mDataConnectionIsConnected = false;
    private volatile boolean mDeviceIsIdle = false;
    private volatile WakeLock mHandleAlarmWakeLock;
    private JobScheduler mJobScheduler;
    private JobSchedulerInternal mJobSchedulerInternal;
    private volatile boolean mJobServiceReady = false;
    private final SyncLogger mLogger;
    private final NotificationManager mNotificationMgr;
    private final BroadcastReceiver mOtherIntentsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.TIME_SET".equals(intent.getAction())) {
                SyncManager.this.mSyncStorageEngine.setClockValid();
            }
        }
    };
    private final PackageManagerInternal mPackageManagerInternal;
    private final PowerManager mPowerManager;
    private volatile boolean mProvisioned;
    private final Random mRand;
    private volatile boolean mReportedSyncActive = false;
    private volatile AccountAndUser[] mRunningAccounts = INITIAL_ACCOUNTS_ARRAY;
    private BroadcastReceiver mShutdownIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.w("SyncManager", "Writing sync state before shutdown...");
            SyncManager.this.getSyncStorageEngine().writeAllState();
            SyncManager.this.mLogger.log(SyncManager.this.getJobStats());
            SyncManager.this.mLogger.log("Shutting down.");
        }
    };
    private BroadcastReceiver mStorageIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.DEVICE_STORAGE_LOW".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Internal storage is low.");
                }
                SyncManager.this.mStorageIsLow = true;
                SyncManager.this.cancelActiveSync(EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, null, "storage low");
            } else if ("android.intent.action.DEVICE_STORAGE_OK".equals(action)) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.v("SyncManager", "Internal storage is ok.");
                }
                SyncManager.this.mStorageIsLow = false;
                SyncManager.this.rescheduleSyncs(EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, "storage ok");
            }
        }
    };
    private volatile boolean mStorageIsLow = false;
    protected final SyncAdaptersCache mSyncAdapters;
    private final SyncHandler mSyncHandler;
    private SyncJobService mSyncJobService;
    private volatile WakeLock mSyncManagerWakeLock;
    private SyncStorageEngine mSyncStorageEngine;
    private final HandlerThread mThread;
    private BroadcastReceiver mUserIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId != -10000) {
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    SyncManager.this.onUserRemoved(userId);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    SyncManager.this.onUserUnlocked(userId);
                } else if ("android.intent.action.USER_STOPPED".equals(action)) {
                    SyncManager.this.onUserStopped(userId);
                }
            }
        }
    };
    private final UserManager mUserManager;

    private static class AccountSyncStats {
        long elapsedTime;
        String name;
        int times;

        /* synthetic */ AccountSyncStats(String x0, AnonymousClass1 x1) {
            this(x0);
        }

        private AccountSyncStats(String name) {
            this.name = name;
        }
    }

    class ActiveSyncContext extends Stub implements ServiceConnection, DeathRecipient {
        private static final int SOURCE_USER = 3;
        boolean mBound;
        long mBytesTransferredAtLastPoll;
        String mEventName;
        final long mHistoryRowId;
        boolean mIsLinkedToDeath = false;
        long mLastPolledTimeElapsed;
        final long mStartTime;
        ISyncAdapter mSyncAdapter;
        final int mSyncAdapterUid;
        SyncInfo mSyncInfo;
        final SyncOperation mSyncOperation;
        final WakeLock mSyncWakeLock;
        long mTimeoutStartTime;

        public ActiveSyncContext(SyncOperation syncOperation, long historyRowId, int syncAdapterUid) {
            this.mSyncAdapterUid = syncAdapterUid;
            this.mSyncOperation = syncOperation;
            this.mHistoryRowId = historyRowId;
            this.mSyncAdapter = null;
            this.mStartTime = SystemClock.elapsedRealtime();
            this.mTimeoutStartTime = this.mStartTime;
            this.mSyncWakeLock = SyncManager.this.mSyncHandler.getSyncWakeLock(this.mSyncOperation);
            this.mSyncWakeLock.setWorkSource(new WorkSource(syncAdapterUid));
            this.mSyncWakeLock.acquire();
        }

        public void sendHeartbeat() {
        }

        public void onFinished(SyncResult result) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onFinished: ");
                stringBuilder.append(this);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            SyncLogger access$1000 = SyncManager.this.mLogger;
            Object[] objArr = new Object[4];
            objArr[0] = "onFinished result=";
            objArr[1] = result;
            objArr[2] = " endpoint=";
            objArr[3] = this.mSyncOperation == null ? "null" : this.mSyncOperation.target;
            access$1000.log(objArr);
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, result);
        }

        public void toString(StringBuilder sb) {
            sb.append("startTime ");
            sb.append(this.mStartTime);
            sb.append(", mTimeoutStartTime ");
            sb.append(this.mTimeoutStartTime);
            sb.append(", mHistoryRowId ");
            sb.append(this.mHistoryRowId);
            sb.append(", syncOperation ");
            sb.append(this.mSyncOperation);
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 4;
            msg.obj = new ServiceConnectionData(this, service);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        public void onServiceDisconnected(ComponentName name) {
            Message msg = SyncManager.this.mSyncHandler.obtainMessage();
            msg.what = 5;
            msg.obj = new ServiceConnectionData(this, null);
            SyncManager.this.mSyncHandler.sendMessage(msg);
        }

        boolean bindToSyncAdapter(ComponentName serviceComponent, int userId, SyncOperation op) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bindToSyncAdapter: ");
                stringBuilder.append(serviceComponent);
                stringBuilder.append(", connection ");
                stringBuilder.append(this);
                Log.d("SyncManager", stringBuilder.toString());
            }
            Intent intent = SyncManager.getAdapterBindIntent(SyncManager.this.mContext, serviceComponent, userId);
            this.mBound = true;
            intent.addHwFlags(64);
            if (op.syncSource == 3) {
                intent.addHwFlags(128);
            }
            boolean bindResult = SyncManager.this.mContext.bindServiceAsUser(intent, this, 21, new UserHandle(this.mSyncOperation.target.userId));
            SyncManager.this.mLogger.log("bindService() returned=", Boolean.valueOf(this.mBound), " for ", this);
            if (bindResult) {
                try {
                    this.mEventName = this.mSyncOperation.wakeLockName();
                    SyncManager.this.mBatteryStats.noteSyncStart(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            } else {
                this.mBound = false;
            }
            return bindResult;
        }

        protected void close() {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unBindFromSyncAdapter: connection ");
                stringBuilder.append(this);
                Log.d("SyncManager", stringBuilder.toString());
            }
            if (this.mBound) {
                this.mBound = false;
                SyncManager.this.mLogger.log("unbindService for ", this);
                SyncManager.this.mContext.unbindService(this);
                try {
                    SyncManager.this.mBatteryStats.noteSyncFinish(this.mEventName, this.mSyncAdapterUid);
                } catch (RemoteException e) {
                }
            }
            this.mSyncWakeLock.release();
            this.mSyncWakeLock.setWorkSource(null);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        public void binderDied() {
            SyncManager.this.sendSyncFinishedOrCanceledMessage(this, null);
        }
    }

    private static class AuthoritySyncStats {
        Map<String, AccountSyncStats> accountMap;
        long elapsedTime;
        String name;
        int times;

        /* synthetic */ AuthoritySyncStats(String x0, AnonymousClass1 x1) {
            this(x0);
        }

        private AuthoritySyncStats(String name) {
            this.accountMap = Maps.newHashMap();
            this.name = name;
        }
    }

    interface OnReadyCallback {
        void onReady();
    }

    private static class OnUnsyncableAccountCheck implements ServiceConnection {
        static final long SERVICE_BOUND_TIME_MILLIS = 5000;
        private final OnReadyCallback mOnReadyCallback;
        private final ServiceInfo<SyncAdapterType> mSyncAdapterInfo;

        OnUnsyncableAccountCheck(ServiceInfo<SyncAdapterType> syncAdapterInfo, OnReadyCallback onReadyCallback) {
            this.mSyncAdapterInfo = syncAdapterInfo;
            this.mOnReadyCallback = onReadyCallback;
        }

        private void onReady() {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mOnReadyCallback.onReady();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                ISyncAdapter.Stub.asInterface(service).onUnsyncableAccount(new ISyncAdapterUnsyncableAccountCallback.Stub() {
                    public void onUnsyncableAccountDone(boolean isReady) {
                        if (isReady) {
                            OnUnsyncableAccountCheck.this.onReady();
                        }
                    }
                });
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not call onUnsyncableAccountDone ");
                stringBuilder.append(this.mSyncAdapterInfo);
                Slog.e("SyncManager", stringBuilder.toString(), e);
                onReady();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    static class PrintTable {
        private final int mCols;
        private ArrayList<String[]> mTable = Lists.newArrayList();

        PrintTable(int cols) {
            this.mCols = cols;
        }

        void set(int row, int col, Object... values) {
            if (values.length + col <= this.mCols) {
                int i;
                int i2 = this.mTable.size();
                while (true) {
                    i = 0;
                    if (i2 > row) {
                        break;
                    }
                    String[] list = new String[this.mCols];
                    this.mTable.add(list);
                    while (i < this.mCols) {
                        list[i] = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        i++;
                    }
                    i2++;
                }
                String[] rowArray = (String[]) this.mTable.get(row);
                while (i < values.length) {
                    Object value = values[i];
                    rowArray[col + i] = value == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : value.toString();
                    i++;
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Table only has ");
            stringBuilder.append(this.mCols);
            stringBuilder.append(" columns. can't set ");
            stringBuilder.append(values.length);
            stringBuilder.append(" at column ");
            stringBuilder.append(col);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }

        void writeTo(PrintWriter out) {
            int col;
            String[] formats = new String[this.mCols];
            int i = 0;
            int totalLength = 0;
            for (col = 0; col < this.mCols; col++) {
                int maxLength = 0;
                Iterator it = this.mTable.iterator();
                while (it.hasNext()) {
                    int length = ((Object[]) it.next())[col].toString().length();
                    if (length > maxLength) {
                        maxLength = length;
                    }
                }
                totalLength += maxLength;
                formats[col] = String.format("%%-%ds", new Object[]{Integer.valueOf(maxLength)});
            }
            formats[this.mCols - 1] = "%s";
            printRow(out, formats, (Object[]) this.mTable.get(0));
            totalLength += (this.mCols - 1) * 2;
            while (true) {
                col = i;
                if (col >= totalLength) {
                    break;
                }
                out.print("-");
                i = col + 1;
            }
            out.println();
            i = this.mTable.size();
            for (col = 1; col < i; col++) {
                printRow(out, formats, (Object[]) this.mTable.get(col));
            }
        }

        private void printRow(PrintWriter out, String[] formats, Object[] row) {
            int rowLength = row.length;
            for (int j = 0; j < rowLength; j++) {
                out.printf(String.format(formats[j], new Object[]{row[j].toString()}), new Object[0]);
                out.print("  ");
            }
            out.println();
        }

        public int getNumRows() {
            return this.mTable.size();
        }
    }

    private static class ScheduleSyncMessagePayload {
        final long minDelayMillis;
        final SyncOperation syncOperation;

        ScheduleSyncMessagePayload(SyncOperation syncOperation, long minDelayMillis) {
            this.syncOperation = syncOperation;
            this.minDelayMillis = minDelayMillis;
        }
    }

    class ServiceConnectionData {
        public final ActiveSyncContext activeSyncContext;
        public final IBinder adapter;

        ServiceConnectionData(ActiveSyncContext activeSyncContext, IBinder adapter) {
            this.activeSyncContext = activeSyncContext;
            this.adapter = adapter;
        }
    }

    private class SyncFinishedOrCancelledMessagePayload {
        public final ActiveSyncContext activeSyncContext;
        public final SyncResult syncResult;

        SyncFinishedOrCancelledMessagePayload(ActiveSyncContext syncContext, SyncResult syncResult) {
            this.activeSyncContext = syncContext;
            this.syncResult = syncResult;
        }
    }

    class SyncHandler extends Handler {
        private static final int MESSAGE_ACCOUNTS_UPDATED = 9;
        private static final int MESSAGE_CANCEL = 6;
        static final int MESSAGE_JOBSERVICE_OBJECT = 7;
        private static final int MESSAGE_MONITOR_SYNC = 8;
        private static final int MESSAGE_RELEASE_MESSAGES_FROM_QUEUE = 2;
        static final int MESSAGE_REMOVE_PERIODIC_SYNC = 14;
        static final int MESSAGE_SCHEDULE_SYNC = 12;
        private static final int MESSAGE_SERVICE_CONNECTED = 4;
        private static final int MESSAGE_SERVICE_DISCONNECTED = 5;
        static final int MESSAGE_START_SYNC = 10;
        static final int MESSAGE_STOP_SYNC = 11;
        private static final int MESSAGE_SYNC_FINISHED = 1;
        static final int MESSAGE_UPDATE_PERIODIC_SYNC = 13;
        public final SyncTimeTracker mSyncTimeTracker = new SyncTimeTracker(SyncManager.this, null);
        private List<Message> mUnreadyQueue = new ArrayList();
        private final HashMap<String, WakeLock> mWakeLocks = Maps.newHashMap();

        void onBootCompleted() {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Boot completed.");
            }
            checkIfDeviceReady();
        }

        void onDeviceProvisioned() {
            if (Log.isLoggable("SyncManager", 3)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mProvisioned=");
                stringBuilder.append(SyncManager.this.mProvisioned);
                Log.d("SyncManager", stringBuilder.toString());
            }
            checkIfDeviceReady();
        }

        void checkIfDeviceReady() {
            if (SyncManager.this.mProvisioned && SyncManager.this.mBootCompleted && SyncManager.this.mJobServiceReady) {
                synchronized (this) {
                    SyncManager.this.mSyncStorageEngine.restoreAllPeriodicSyncs();
                    obtainMessage(2).sendToTarget();
                }
            }
        }

        private boolean tryEnqueueMessageUntilReadyToRun(Message msg) {
            synchronized (this) {
                if (SyncManager.this.mBootCompleted && SyncManager.this.mProvisioned && SyncManager.this.mJobServiceReady) {
                    return false;
                }
                if (SyncManager.this.mProvisioned || !(msg.obj instanceof SyncOperation)) {
                    this.mUnreadyQueue.add(Message.obtain(msg));
                } else {
                    deferSyncH((SyncOperation) msg.obj, 600000, "delay on provision");
                }
                return true;
            }
        }

        public SyncHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                SyncManager.this.mSyncManagerWakeLock.acquire();
                if (msg.what == 7) {
                    Slog.i("SyncManager", "Got SyncJobService instance.");
                    SyncManager.this.mSyncJobService = (SyncJobService) msg.obj;
                    SyncManager.this.mJobServiceReady = true;
                    checkIfDeviceReady();
                } else if (msg.what == 9) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "handleSyncHandlerMessage: MESSAGE_ACCOUNTS_UPDATED");
                    }
                    updateRunningAccountsH(msg.obj);
                } else if (msg.what == 2) {
                    if (this.mUnreadyQueue != null) {
                        for (Message m : this.mUnreadyQueue) {
                            handleSyncMessage(m);
                        }
                        this.mUnreadyQueue = null;
                    }
                } else if (!tryEnqueueMessageUntilReadyToRun(msg)) {
                    handleSyncMessage(msg);
                }
                SyncManager.this.mSyncManagerWakeLock.release();
            } catch (Throwable th) {
                SyncManager.this.mSyncManagerWakeLock.release();
            }
        }

        private void handleSyncMessage(Message msg) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            try {
                SyncManager.this.mDataConnectionIsConnected = SyncManager.this.readDataConnectionState();
                boolean z = true;
                StringBuilder stringBuilder;
                ActiveSyncContext currentSyncContext;
                StringBuilder stringBuilder2;
                switch (msg.what) {
                    case 1:
                        SyncFinishedOrCancelledMessagePayload payload = msg.obj;
                        if (!SyncManager.this.isSyncStillActiveH(payload.activeSyncContext)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handleSyncHandlerMessage: dropping since the sync is no longer active: ");
                            stringBuilder.append(payload.activeSyncContext);
                            Log.d("SyncManager", stringBuilder.toString());
                            break;
                        }
                        if (isLoggable) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("syncFinished");
                            stringBuilder.append(payload.activeSyncContext.mSyncOperation);
                            Slog.v("SyncManager", stringBuilder.toString());
                        }
                        SyncManager.this.mSyncJobService.callJobFinished(payload.activeSyncContext.mSyncOperation.jobId, false, "sync finished");
                        runSyncFinishedOrCanceledH(payload.syncResult, payload.activeSyncContext);
                        break;
                    case 4:
                        ServiceConnectionData msgData = msg.obj;
                        if (Log.isLoggable("SyncManager", 2)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handleSyncHandlerMessage: MESSAGE_SERVICE_CONNECTED: ");
                            stringBuilder.append(msgData.activeSyncContext);
                            Log.d("SyncManager", stringBuilder.toString());
                        }
                        if (SyncManager.this.isSyncStillActiveH(msgData.activeSyncContext)) {
                            runBoundToAdapterH(msgData.activeSyncContext, msgData.adapter);
                            break;
                        }
                        break;
                    case 5:
                        currentSyncContext = ((ServiceConnectionData) msg.obj).activeSyncContext;
                        if (Log.isLoggable("SyncManager", 2)) {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("handleSyncHandlerMessage: MESSAGE_SERVICE_DISCONNECTED: ");
                            stringBuilder3.append(currentSyncContext);
                            Log.d("SyncManager", stringBuilder3.toString());
                        }
                        if (SyncManager.this.isSyncStillActiveH(currentSyncContext)) {
                            if (currentSyncContext.mSyncAdapter != null) {
                                SyncManager.this.mLogger.log("Calling cancelSync for SERVICE_DISCONNECTED ", currentSyncContext, " adapter=", currentSyncContext.mSyncAdapter);
                                currentSyncContext.mSyncAdapter.cancelSync(currentSyncContext);
                                SyncManager.this.mLogger.log("Canceled");
                            }
                            SyncResult syncResult = new SyncResult();
                            SyncStats syncStats = syncResult.stats;
                            syncStats.numIoExceptions++;
                            SyncManager.this.mSyncJobService.callJobFinished(currentSyncContext.mSyncOperation.jobId, false, "service disconnected");
                            runSyncFinishedOrCanceledH(syncResult, currentSyncContext);
                            break;
                        }
                        break;
                    case 6:
                        EndPoint endpoint = msg.obj;
                        Bundle extras = msg.peekData();
                        if (Log.isLoggable("SyncManager", 3)) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("handleSyncHandlerMessage: MESSAGE_CANCEL: ");
                            stringBuilder2.append(endpoint);
                            stringBuilder2.append(" bundle: ");
                            stringBuilder2.append(extras);
                            Log.d("SyncManager", stringBuilder2.toString());
                        }
                        cancelActiveSyncH(endpoint, extras, "MESSAGE_CANCEL");
                        break;
                    case 8:
                        ActiveSyncContext monitoredSyncContext = msg.obj;
                        if (Log.isLoggable("SyncManager", 3)) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("handleSyncHandlerMessage: MESSAGE_MONITOR_SYNC: ");
                            stringBuilder2.append(monitoredSyncContext.mSyncOperation.target);
                            Log.d("SyncManager", stringBuilder2.toString());
                        }
                        if (!isSyncNotUsingNetworkH(monitoredSyncContext)) {
                            SyncManager.this.postMonitorSyncProgressMessage(monitoredSyncContext);
                            break;
                        }
                        Log.w("SyncManager", String.format("Detected sync making no progress for %s. cancelling.", new Object[]{monitoredSyncContext}));
                        SyncManager.this.mSyncJobService.callJobFinished(monitoredSyncContext.mSyncOperation.jobId, false, "no network activity");
                        runSyncFinishedOrCanceledH(null, monitoredSyncContext);
                        break;
                    case 10:
                        startSyncH((SyncOperation) msg.obj);
                        break;
                    case 11:
                        SyncOperation op = msg.obj;
                        if (isLoggable) {
                            Slog.v("SyncManager", "Stop sync received.");
                        }
                        currentSyncContext = findActiveSyncContextH(op.jobId);
                        if (currentSyncContext != null) {
                            runSyncFinishedOrCanceledH(null, currentSyncContext);
                            boolean reschedule = msg.arg1 != 0;
                            if (msg.arg2 == 0) {
                                z = false;
                            }
                            boolean applyBackoff = z;
                            if (isLoggable) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Stopping sync. Reschedule: ");
                                stringBuilder4.append(reschedule);
                                stringBuilder4.append("Backoff: ");
                                stringBuilder4.append(applyBackoff);
                                Slog.v("SyncManager", stringBuilder4.toString());
                            }
                            if (applyBackoff) {
                                SyncManager.this.increaseBackoffSetting(op.target);
                            }
                            if (reschedule) {
                                deferStoppedSyncH(op, 0);
                                break;
                            }
                        }
                        break;
                    case 12:
                        ScheduleSyncMessagePayload syncPayload = msg.obj;
                        SyncManager.this.scheduleSyncOperationH(syncPayload.syncOperation, syncPayload.minDelayMillis);
                        break;
                    case 13:
                        UpdatePeriodicSyncMessagePayload data = msg.obj;
                        updateOrAddPeriodicSyncH(data.target, data.pollFrequency, data.flex, data.extras);
                        break;
                    case 14:
                        Pair<EndPoint, String> args = msg.obj;
                        removePeriodicSyncH((EndPoint) args.first, msg.getData(), (String) args.second);
                        break;
                }
            } catch (RemoteException e) {
                SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
            } catch (Throwable th) {
                this.mSyncTimeTracker.update();
            }
            this.mSyncTimeTracker.update();
        }

        private WakeLock getSyncWakeLock(SyncOperation operation) {
            String wakeLockKey = operation.wakeLockName();
            WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(wakeLockKey);
            if (wakeLock != null) {
                return wakeLock;
            }
            String name = new StringBuilder();
            name.append(SyncManager.SYNC_WAKE_LOCK_PREFIX);
            name.append(wakeLockKey);
            wakeLock = SyncManager.this.mPowerManager.newWakeLock(1, name.toString());
            wakeLock.setReferenceCounted(false);
            this.mWakeLocks.put(wakeLockKey, wakeLock);
            return wakeLock;
        }

        private void deferSyncH(SyncOperation op, long delay, String why) {
            SyncLogger access$1000 = SyncManager.this.mLogger;
            Object[] objArr = new Object[8];
            objArr[0] = "deferSyncH() ";
            objArr[1] = op.isPeriodic ? "periodic " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            objArr[2] = "sync.  op=";
            objArr[3] = op;
            objArr[4] = " delay=";
            objArr[5] = Long.valueOf(delay);
            objArr[6] = " why=";
            objArr[7] = why;
            access$1000.log(objArr);
            SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false, why);
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
                return;
            }
            SyncManager.this.cancelJob(op, "deferSyncH()");
            SyncManager.this.scheduleSyncOperationH(op, delay);
        }

        private void deferStoppedSyncH(SyncOperation op, long delay) {
            if (op.isPeriodic) {
                SyncManager.this.scheduleSyncOperationH(op.createOneTimeSyncOperation(), delay);
            } else {
                SyncManager.this.scheduleSyncOperationH(op, delay);
            }
        }

        private void deferActiveSyncH(ActiveSyncContext asc, String why) {
            SyncOperation op = asc.mSyncOperation;
            runSyncFinishedOrCanceledH(null, asc);
            deferSyncH(op, 10000, why);
        }

        /* JADX WARNING: Removed duplicated region for block: B:46:0x010b  */
        /* JADX WARNING: Removed duplicated region for block: B:49:0x011f  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void startSyncH(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            if (isLoggable) {
                Slog.v("SyncManager", op.toString());
            }
            SyncManager.this.mSyncStorageEngine.setClockValid();
            SyncManager.this.mSyncJobService.markSyncStarted(op.jobId);
            if (!SyncManager.this.mStorageIsLow) {
                int syncOpState;
                if (op.isPeriodic) {
                    for (SyncOperation syncOperation : SyncManager.this.getAllPendingSyncs()) {
                        if (syncOperation.sourcePeriodicId == op.jobId) {
                            SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false, "periodic sync, pending");
                            return;
                        }
                    }
                    Iterator it = SyncManager.this.mActiveSyncContexts.iterator();
                    while (it.hasNext()) {
                        if (((ActiveSyncContext) it.next()).mSyncOperation.sourcePeriodicId == op.jobId) {
                            SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false, "periodic sync, already running");
                            return;
                        }
                    }
                    if (SyncManager.this.isAdapterDelayed(op.target)) {
                        deferSyncH(op, 0, "backing off");
                        return;
                    }
                }
                Iterator it2 = SyncManager.this.mActiveSyncContexts.iterator();
                while (it2.hasNext()) {
                    ActiveSyncContext asc = (ActiveSyncContext) it2.next();
                    if (asc.mSyncOperation.isConflict(op)) {
                        if (asc.mSyncOperation.findPriority() >= op.findPriority()) {
                            if (isLoggable) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Rescheduling sync due to conflict ");
                                stringBuilder.append(op.toString());
                                Slog.v("SyncManager", stringBuilder.toString());
                            }
                            deferSyncH(op, 10000, "delay on conflict");
                            return;
                        }
                        if (isLoggable) {
                            Slog.v("SyncManager", "Pushing back running sync due to a higher priority sync");
                        }
                        deferActiveSyncH(asc, "preempted");
                        syncOpState = computeSyncOpState(op);
                        switch (syncOpState) {
                            case 1:
                            case 2:
                                SyncJobService access$3000 = SyncManager.this.mSyncJobService;
                                int i = op.jobId;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("invalid op state: ");
                                stringBuilder2.append(syncOpState);
                                access$3000.callJobFinished(i, false, stringBuilder2.toString());
                                return;
                            default:
                                if (!dispatchSyncOperation(op)) {
                                    SyncManager.this.mSyncJobService.callJobFinished(op.jobId, false, "dispatchSyncOperation() failed");
                                }
                                SyncManager.this.setAuthorityPendingState(op.target);
                                return;
                        }
                    }
                }
                syncOpState = computeSyncOpState(op);
                switch (syncOpState) {
                    case 1:
                    case 2:
                        break;
                    default:
                        break;
                }
            }
            deferSyncH(op, 3600000, "storage low");
        }

        private ActiveSyncContext findActiveSyncContextH(int jobId) {
            Iterator it = SyncManager.this.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                ActiveSyncContext asc = (ActiveSyncContext) it.next();
                SyncOperation op = asc.mSyncOperation;
                if (op != null && op.jobId == jobId) {
                    return asc;
                }
            }
            return null;
        }

        /* JADX WARNING: Removed duplicated region for block: B:30:0x011a  */
        /* JADX WARNING: Removed duplicated region for block: B:46:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:35:0x014c  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void updateRunningAccountsH(EndPoint syncTargets) {
            EndPoint endPoint = syncTargets;
            AccountAndUser[] oldAccounts = SyncManager.this.mRunningAccounts;
            SyncManager.this.mRunningAccounts = AccountManagerService.getSingleton().getRunningAccounts();
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Accounts list: ");
                for (AccountAndUser acc : SyncManager.this.mRunningAccounts) {
                    Slog.v("SyncManager", acc.toString());
                }
            }
            if (SyncManager.this.mLogger.enabled()) {
                SyncManager.this.mLogger.log("updateRunningAccountsH: ", Arrays.toString(SyncManager.this.mRunningAccounts));
            }
            if (SyncManager.this.mBootCompleted) {
                SyncManager.this.doDatabaseCleanup();
            }
            AccountAndUser[] accounts = SyncManager.this.mRunningAccounts;
            Iterator it = new ArrayList(SyncManager.this.mActiveSyncContexts).iterator();
            while (it.hasNext()) {
                ActiveSyncContext currentSyncContext = (ActiveSyncContext) it.next();
                if (!SyncManager.this.containsAccountAndUser(accounts, currentSyncContext.mSyncOperation.target.account, currentSyncContext.mSyncOperation.target.userId)) {
                    Log.d("SyncManager", "canceling sync since the account is no longer running");
                    SyncManager.this.sendSyncFinishedOrCanceledMessage(currentSyncContext, null);
                }
            }
            AccountAndUser[] access$3800 = SyncManager.this.mRunningAccounts;
            int length = access$3800.length;
            int i = 0;
            while (i < length) {
                AccountAndUser aau = access$3800[i];
                if (SyncManager.this.containsAccountAndUser(oldAccounts, aau.account, aau.userId)) {
                    i++;
                } else {
                    if (Log.isLoggable("SyncManager", 3)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Account ");
                        stringBuilder.append(aau.account);
                        stringBuilder.append(" added, checking sync restore data");
                        Log.d("SyncManager", stringBuilder.toString());
                    }
                    AccountSyncSettingsBackupHelper.accountAdded(SyncManager.this.mContext);
                    access$3800 = AccountManagerService.getSingleton().getAllAccounts();
                    for (SyncOperation op : SyncManager.this.getAllPendingSyncs()) {
                        if (!SyncManager.this.containsAccountAndUser(access$3800, op.target.account, op.target.userId)) {
                            SyncManager.this.mLogger.log("canceling: ", op);
                            SyncManager.this.cancelJob(op, "updateRunningAccountsH()");
                        }
                    }
                    if (endPoint == null) {
                        SyncManager.this.scheduleSync(endPoint.account, endPoint.userId, -2, endPoint.provider, null, -1, 0);
                        return;
                    }
                    return;
                }
            }
            access$3800 = AccountManagerService.getSingleton().getAllAccounts();
            for (SyncOperation op2 : SyncManager.this.getAllPendingSyncs()) {
            }
            if (endPoint == null) {
            }
        }

        private void maybeUpdateSyncPeriodH(SyncOperation syncOperation, long pollFrequencyMillis, long flexMillis) {
            if (pollFrequencyMillis != syncOperation.periodMillis || flexMillis != syncOperation.flexMillis) {
                if (Log.isLoggable("SyncManager", 2)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updating period ");
                    stringBuilder.append(syncOperation);
                    stringBuilder.append(" to ");
                    stringBuilder.append(pollFrequencyMillis);
                    stringBuilder.append(" and flex to ");
                    stringBuilder.append(flexMillis);
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                SyncOperation syncOperation2 = new SyncOperation(syncOperation, pollFrequencyMillis, flexMillis);
                syncOperation2.jobId = syncOperation.jobId;
                SyncManager.this.scheduleSyncOperationH(syncOperation2);
            }
        }

        private void updateOrAddPeriodicSyncH(EndPoint target, long pollFrequency, long flex, Bundle extras) {
            EndPoint endPoint = target;
            long j = pollFrequency;
            long j2 = flex;
            if (endPoint.account != null) {
                StringBuilder stringBuilder;
                SyncOperation op;
                Bundle bundle;
                boolean isLoggable = Log.isLoggable("SyncManager", 2);
                SyncManager.this.verifyJobScheduler();
                long pollFrequencyMillis = j * 1000;
                long flexMillis = j2 * 1000;
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Addition to periodic syncs requested: ");
                    stringBuilder.append(endPoint);
                    stringBuilder.append(" period: ");
                    stringBuilder.append(j);
                    stringBuilder.append(" flexMillis: ");
                    stringBuilder.append(j2);
                    stringBuilder.append(" extras: ");
                    stringBuilder.append(extras.toString());
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                for (SyncOperation op2 : ops) {
                    if (!op2.isPeriodic || !op2.target.matchesSpec(endPoint)) {
                        bundle = extras;
                    } else if (SyncManager.syncExtrasEquals(op2.extras, extras, true)) {
                        maybeUpdateSyncPeriodH(op2, pollFrequencyMillis, flexMillis);
                        return;
                    }
                }
                bundle = extras;
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Adding new periodic sync: ");
                    stringBuilder.append(endPoint);
                    stringBuilder.append(" period: ");
                    stringBuilder.append(j);
                    stringBuilder.append(" flexMillis: ");
                    stringBuilder.append(j2);
                    stringBuilder.append(" extras: ");
                    stringBuilder.append(extras.toString());
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(endPoint.provider, endPoint.account.type), endPoint.userId);
                if (syncAdapterInfo != null) {
                    op2 = new SyncOperation(endPoint, syncAdapterInfo.uid, syncAdapterInfo.componentName.getPackageName(), -4, 4, extras, ((SyncAdapterType) syncAdapterInfo.type).allowParallelSyncs(), true, -1, pollFrequencyMillis, flexMillis, 0);
                    int syncOpState = computeSyncOpState(op2);
                    List<SyncOperation> list;
                    int i;
                    switch (syncOpState) {
                        case 1:
                            list = ops;
                            i = syncOpState;
                            return;
                        case 2:
                            String packageName = op2.owningPackage;
                            int userId = UserHandle.getUserId(op2.owningUid);
                            if (SyncManager.this.mPackageManagerInternal.wasPackageEverLaunched(packageName, userId)) {
                                AccountManagerInternal access$4300 = SyncManager.this.mAccountManagerInternal;
                                Account account = op2.target.account;
                                i = syncOpState;
                                OnResultListener onResultListener = r0;
                                ServiceInfo<SyncAdapterType> serviceInfo = syncAdapterInfo;
                                list = ops;
                                -$$Lambda$SyncManager$SyncHandler$7-vThHsPImW4qB6AnVEnnD3dGhM -__lambda_syncmanager_synchandler_7-vthhspimw4qb6anvennd3dghm = new -$$Lambda$SyncManager$SyncHandler$7-vThHsPImW4qB6AnVEnnD3dGhM(this, endPoint, pollFrequency, flex, extras);
                                access$4300.requestAccountAccess(account, packageName, userId, new RemoteCallback(onResultListener));
                                return;
                            }
                            return;
                        default:
                            SyncManager.this.scheduleSyncOperationH(op2);
                            SyncManager.this.mSyncStorageEngine.reportChange(1);
                            return;
                    }
                }
            }
        }

        public static /* synthetic */ void lambda$updateOrAddPeriodicSyncH$0(SyncHandler syncHandler, EndPoint target, long pollFrequency, long flex, Bundle extras, Bundle result) {
            if (result != null && result.getBoolean("booleanResult")) {
                SyncManager.this.updateOrAddPeriodicSync(target, pollFrequency, flex, extras);
            }
        }

        private void removePeriodicSyncInternalH(SyncOperation syncOperation, String why) {
            for (SyncOperation op : SyncManager.this.getAllPendingSyncs()) {
                if (op.sourcePeriodicId == syncOperation.jobId || op.jobId == syncOperation.jobId) {
                    ActiveSyncContext asc = findActiveSyncContextH(syncOperation.jobId);
                    if (asc != null) {
                        SyncManager.this.mSyncJobService.callJobFinished(syncOperation.jobId, false, "removePeriodicSyncInternalH");
                        runSyncFinishedOrCanceledH(null, asc);
                    }
                    SyncManager.this.mLogger.log("removePeriodicSyncInternalH-canceling: ", op);
                    SyncManager.this.cancelJob(op, why);
                }
            }
        }

        private void removePeriodicSyncH(EndPoint target, Bundle extras, String why) {
            SyncManager.this.verifyJobScheduler();
            for (SyncOperation op : SyncManager.this.getAllPendingSyncs()) {
                if (op.isPeriodic && op.target.matchesSpec(target) && SyncManager.syncExtrasEquals(op.extras, extras, true)) {
                    removePeriodicSyncInternalH(op, why);
                }
            }
        }

        private boolean isSyncNotUsingNetworkH(ActiveSyncContext activeSyncContext) {
            boolean z;
            boolean z2;
            ActiveSyncContext activeSyncContext2 = activeSyncContext;
            long bytesTransferredCurrent = SyncManager.this.getTotalBytesTransferredByUid(activeSyncContext2.mSyncAdapterUid);
            long deltaBytesTransferred = bytesTransferredCurrent - activeSyncContext2.mBytesTransferredAtLastPoll;
            if (Log.isLoggable("SyncManager", 3)) {
                long remainder = deltaBytesTransferred;
                long mb = remainder / 1048576;
                remainder %= 1048576;
                long kb = remainder / 1024;
                long b = remainder % 1024;
                Object[] objArr = new Object[4];
                z = false;
                objArr[0] = Long.valueOf((SystemClock.elapsedRealtime() - activeSyncContext2.mLastPolledTimeElapsed) / 1000);
                z2 = true;
                objArr[1] = Long.valueOf(mb);
                objArr[2] = Long.valueOf(kb);
                objArr[3] = Long.valueOf(b);
                Log.d("SyncManager", String.format("Time since last update: %ds. Delta transferred: %dMBs,%dKBs,%dBs", objArr));
            } else {
                z = false;
                z2 = true;
            }
            return deltaBytesTransferred <= 10 ? z2 : z;
        }

        private int computeSyncOpState(SyncOperation op) {
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            EndPoint target = op.target;
            if (SyncManager.this.containsAccountAndUser(SyncManager.this.mRunningAccounts, target.account, target.userId)) {
                int state = SyncManager.this.computeSyncable(target.account, target.userId, target.provider, true);
                if (state == 3) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                    }
                    return 2;
                } else if (state == 0) {
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: isSyncable == NOT_SYNCABLE");
                    }
                    return 1;
                } else {
                    boolean syncEnabled = SyncManager.this.mSyncStorageEngine.getMasterSyncAutomatically(target.userId) && SyncManager.this.mSyncStorageEngine.getSyncAutomatically(target.account, target.userId, target.provider);
                    boolean ignoreSystemConfiguration = op.isIgnoreSettings() || state < 0;
                    if (syncEnabled || ignoreSystemConfiguration) {
                        if (HwDeviceManager.disallowOp(42) && !op.isManual() && state >= 0) {
                            NetworkInfo networkInfo = SyncManager.this.getConnectivityManager().getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isRoaming() && networkInfo.getType() == 0) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("    Dropping auto sync operation for ");
                                stringBuilder.append(target.provider);
                                stringBuilder.append(": disallowed by MDM disable auto sync when roaming.");
                                Slog.v("SyncManager", stringBuilder.toString());
                                return 1;
                            }
                        }
                        if (target.account == null || !HwDeviceManager.disallowOp(25, target.account.type) || op.isManual() || state < 0) {
                            return 0;
                        }
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("    Dropping auto sync operation for ");
                        stringBuilder2.append(target.provider);
                        stringBuilder2.append(": disallowed by MDM.");
                        Slog.v("SyncManager", stringBuilder2.toString());
                        return 1;
                    }
                    if (isLoggable) {
                        Slog.v("SyncManager", "    Dropping sync operation: disallowed by settings/network.");
                    }
                    return 1;
                }
            }
            if (isLoggable) {
                Slog.v("SyncManager", "    Dropping sync operation: account doesn't exist.");
            }
            return 1;
        }

        private boolean dispatchSyncOperation(SyncOperation op) {
            SyncOperation syncOperation = op;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dispatchSyncOperation:we are going to sync ");
            stringBuilder.append(syncOperation);
            Slog.i("SyncManager", stringBuilder.toString());
            if (Log.isLoggable("SyncManager", 2)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("num active syncs: ");
                stringBuilder.append(SyncManager.this.mActiveSyncContexts.size());
                Slog.v("SyncManager", stringBuilder.toString());
                Iterator it = SyncManager.this.mActiveSyncContexts.iterator();
                while (it.hasNext()) {
                    Slog.v("SyncManager", ((ActiveSyncContext) it.next()).toString());
                }
            }
            if (op.isAppStandbyExempted()) {
                UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
                if (usmi != null) {
                    usmi.reportExemptedSyncStart(syncOperation.owningPackage, UserHandle.getUserId(syncOperation.owningUid));
                }
            }
            EndPoint info = syncOperation.target;
            SyncAdapterType syncAdapterType = SyncAdapterType.newKey(info.provider, info.account.type);
            ServiceInfo<SyncAdapterType> syncAdapterInfo = SyncManager.this.mSyncAdapters.getServiceInfo(syncAdapterType, info.userId);
            if (syncAdapterInfo == null) {
                SyncManager.this.mLogger.log("dispatchSyncOperation() failed: no sync adapter info for ", syncAdapterType);
                stringBuilder = new StringBuilder();
                stringBuilder.append("can't find a sync adapter for ");
                stringBuilder.append(syncAdapterType);
                stringBuilder.append(", removing settings for it");
                Log.i("SyncManager", stringBuilder.toString());
                SyncManager.this.mSyncStorageEngine.removeAuthority(info);
                return false;
            }
            StringBuilder stringBuilder2;
            int targetUid = syncAdapterInfo.uid;
            ComponentName targetComponent = syncAdapterInfo.componentName;
            ActiveSyncContext activeSyncContext = new ActiveSyncContext(syncOperation, insertStartSyncEvent(op), targetUid);
            if (Log.isLoggable("SyncManager", 2)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("dispatchSyncOperation: starting ");
                stringBuilder2.append(activeSyncContext);
                Slog.v("SyncManager", stringBuilder2.toString());
            }
            activeSyncContext.mSyncInfo = SyncManager.this.mSyncStorageEngine.addActiveSync(activeSyncContext);
            SyncManager.this.mActiveSyncContexts.add(activeSyncContext);
            SyncManager.this.postMonitorSyncProgressMessage(activeSyncContext);
            if (activeSyncContext.bindToSyncAdapter(targetComponent, info.userId, syncOperation)) {
                return true;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bind attempt failed - target: ");
            stringBuilder2.append(targetComponent);
            Slog.e("SyncManager", stringBuilder2.toString());
            closeActiveSyncContext(activeSyncContext);
            return false;
        }

        private void runBoundToAdapterH(ActiveSyncContext activeSyncContext, IBinder syncAdapter) {
            SyncOperation syncOperation = activeSyncContext.mSyncOperation;
            try {
                activeSyncContext.mIsLinkedToDeath = true;
                syncAdapter.linkToDeath(activeSyncContext, 0);
                SyncLogger access$1000 = SyncManager.this.mLogger;
                r5 = new Object[9];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sync start: account=");
                stringBuilder.append(syncOperation.target.account);
                r5[0] = stringBuilder.toString();
                r5[1] = " authority=";
                r5[2] = syncOperation.target.provider;
                r5[3] = " reason=";
                r5[4] = SyncOperation.reasonToString(null, syncOperation.reason);
                r5[5] = " extras=";
                r5[6] = SyncOperation.extrasToString(syncOperation.extras);
                r5[7] = " adapter=";
                r5[8] = activeSyncContext.mSyncAdapter;
                access$1000.log(r5);
                activeSyncContext.mSyncAdapter = ISyncAdapter.Stub.asInterface(syncAdapter);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("run startSync:");
                stringBuilder2.append(syncOperation);
                Slog.i("SyncManager", stringBuilder2.toString());
                activeSyncContext.mSyncAdapter.startSync(activeSyncContext, syncOperation.target.provider, syncOperation.target.account, syncOperation.extras);
                SyncManager.this.mLogger.log("Sync is running now...");
            } catch (RemoteException remoteExc) {
                SyncManager.this.mLogger.log("Sync failed with RemoteException: ", remoteExc.toString());
                Log.d("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                Log.i("SyncManager", "maybeStartNextSync: caught a RemoteException, rescheduling", remoteExc);
                closeActiveSyncContext(activeSyncContext);
                SyncManager.this.increaseBackoffSetting(syncOperation.target);
                SyncManager.this.scheduleSyncOperationH(syncOperation);
            } catch (RuntimeException exc) {
                SyncManager.this.mLogger.log("Sync failed with RuntimeException: ", exc.toString());
                closeActiveSyncContext(activeSyncContext);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Caught RuntimeException while starting the sync ");
                stringBuilder3.append(syncOperation);
                Slog.e("SyncManager", stringBuilder3.toString(), exc);
            }
        }

        private void cancelActiveSyncH(EndPoint info, Bundle extras, String why) {
            Iterator it = new ArrayList(SyncManager.this.mActiveSyncContexts).iterator();
            while (it.hasNext()) {
                ActiveSyncContext activeSyncContext = (ActiveSyncContext) it.next();
                if (activeSyncContext != null) {
                    if (activeSyncContext.mSyncOperation.target.matchesSpec(info)) {
                        if (extras == null || SyncManager.syncExtrasEquals(activeSyncContext.mSyncOperation.extras, extras, false)) {
                            SyncManager.this.mSyncJobService.callJobFinished(activeSyncContext.mSyncOperation.jobId, false, why);
                            runSyncFinishedOrCanceledH(null, activeSyncContext);
                        }
                    }
                }
            }
        }

        private void reschedulePeriodicSyncH(SyncOperation syncOperation) {
            SyncOperation periodicSync = null;
            for (SyncOperation op : SyncManager.this.getAllPendingSyncs()) {
                if (op.isPeriodic && syncOperation.matchesPeriodicOperation(op)) {
                    periodicSync = op;
                    break;
                }
            }
            if (periodicSync != null) {
                SyncManager.this.scheduleSyncOperationH(periodicSync);
            }
        }

        private void runSyncFinishedOrCanceledH(SyncResult syncResult, ActiveSyncContext activeSyncContext) {
            String historyMessage;
            int downstreamActivity;
            SyncResult syncResult2 = syncResult;
            ActiveSyncContext activeSyncContext2 = activeSyncContext;
            boolean isLoggable = Log.isLoggable("SyncManager", 2);
            SyncOperation syncOperation = activeSyncContext2.mSyncOperation;
            EndPoint info = syncOperation.target;
            boolean upstreamActivity = false;
            if (activeSyncContext2.mIsLinkedToDeath) {
                activeSyncContext2.mSyncAdapter.asBinder().unlinkToDeath(activeSyncContext2, 0);
                activeSyncContext2.mIsLinkedToDeath = false;
            }
            long elapsedTime = SystemClock.elapsedRealtime() - activeSyncContext2.mStartTime;
            SyncManager.this.mLogger.log("runSyncFinishedOrCanceledH() op=", syncOperation, " result=", syncResult2);
            if (syncResult2 != null) {
                if (isLoggable) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("runSyncFinishedOrCanceled [finished]: ");
                    stringBuilder.append(syncOperation);
                    stringBuilder.append(", result ");
                    stringBuilder.append(syncResult2);
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                closeActiveSyncContext(activeSyncContext2);
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-finished");
                }
                if (syncResult.hasError()) {
                    Log.d("SyncManager", "failed sync operation ");
                    syncOperation.retries++;
                    if (syncOperation.retries > SyncManager.this.mConstants.getMaxRetriesWithAppStandbyExemption()) {
                        syncOperation.syncExemptionFlag = 0;
                    }
                    SyncManager.this.increaseBackoffSetting(syncOperation.target);
                    if (syncOperation.isPeriodic) {
                        SyncManager.this.postScheduleSyncMessage(syncOperation.createOneTimeSyncOperation(), 0);
                    } else {
                        SyncManager.this.maybeRescheduleSync(syncResult2, syncOperation);
                    }
                    historyMessage = ContentResolver.syncErrorToString(syncResultToErrorNumber(syncResult));
                    downstreamActivity = 0;
                } else {
                    historyMessage = SyncStorageEngine.MESG_SUCCESS;
                    downstreamActivity = 0;
                    upstreamActivity = false;
                    SyncManager.this.clearBackoffSetting(syncOperation.target, "sync success");
                    if (syncOperation.isDerivedFromFailedPeriodicSync()) {
                        reschedulePeriodicSyncH(syncOperation);
                    }
                }
                SyncManager.this.setDelayUntilTime(syncOperation.target, syncResult2.delayUntil);
            } else {
                if (isLoggable) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("runSyncFinishedOrCanceled [canceled]: ");
                    stringBuilder2.append(syncOperation);
                    Slog.v("SyncManager", stringBuilder2.toString());
                }
                if (!syncOperation.isPeriodic) {
                    SyncManager.this.cancelJob(syncOperation, "runSyncFinishedOrCanceledH()-canceled");
                }
                if (activeSyncContext2.mSyncAdapter != null) {
                    try {
                        SyncManager.this.mLogger.log("Calling cancelSync for runSyncFinishedOrCanceled ", activeSyncContext2, "  adapter=", activeSyncContext2.mSyncAdapter);
                        activeSyncContext2.mSyncAdapter.cancelSync(activeSyncContext2);
                        SyncManager.this.mLogger.log("Canceled");
                    } catch (RemoteException e) {
                        SyncManager.this.mLogger.log("RemoteException ", Log.getStackTraceString(e));
                    }
                }
                historyMessage = SyncStorageEngine.MESG_CANCELED;
                downstreamActivity = 0;
                upstreamActivity = false;
                closeActiveSyncContext(activeSyncContext2);
            }
            int downstreamActivity2 = downstreamActivity;
            stopSyncEvent(activeSyncContext2.mHistoryRowId, syncOperation, historyMessage, upstreamActivity, downstreamActivity2, elapsedTime);
            if (syncResult2 == null || !syncResult2.tooManyDeletions) {
                SyncManager.this.mNotificationMgr.cancelAsUser(Integer.toString(info.account.hashCode() ^ info.provider.hashCode()), 18, new UserHandle(info.userId));
            } else {
                installHandleTooManyDeletesNotification(info.account, info.provider, syncResult2.stats.numDeletes, info.userId);
            }
            if (syncResult2 == null || !syncResult2.fullSyncRequested) {
                return;
            }
            SyncManager.this.scheduleSyncOperationH(new SyncOperation(info.account, info.userId, syncOperation.owningUid, syncOperation.owningPackage, syncOperation.reason, syncOperation.syncSource, info.provider, new Bundle(), syncOperation.allowParallelSyncs, syncOperation.syncExemptionFlag));
        }

        private void closeActiveSyncContext(ActiveSyncContext activeSyncContext) {
            activeSyncContext.close();
            SyncManager.this.mActiveSyncContexts.remove(activeSyncContext);
            SyncManager.this.mSyncStorageEngine.removeActiveSync(activeSyncContext.mSyncInfo, activeSyncContext.mSyncOperation.target.userId);
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removing all MESSAGE_MONITOR_SYNC & MESSAGE_SYNC_EXPIRED for ");
                stringBuilder.append(activeSyncContext.toString());
                Slog.v("SyncManager", stringBuilder.toString());
            }
            SyncManager.this.mSyncHandler.removeMessages(8, activeSyncContext);
            SyncManager.this.mLogger.log("closeActiveSyncContext: ", activeSyncContext);
        }

        private int syncResultToErrorNumber(SyncResult syncResult) {
            if (syncResult.syncAlreadyInProgress) {
                return 1;
            }
            if (syncResult.stats.numAuthExceptions > 0) {
                return 2;
            }
            if (syncResult.stats.numIoExceptions > 0) {
                return 3;
            }
            if (syncResult.stats.numParseExceptions > 0) {
                return 4;
            }
            if (syncResult.stats.numConflictDetectedExceptions > 0) {
                return 5;
            }
            if (syncResult.tooManyDeletions) {
                return 6;
            }
            if (syncResult.tooManyRetries) {
                return 7;
            }
            if (syncResult.databaseError) {
                return 8;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("we are not in an error state, ");
            stringBuilder.append(syncResult);
            throw new IllegalStateException(stringBuilder.toString());
        }

        private void installHandleTooManyDeletesNotification(Account account, String authority, long numDeletes, int userId) {
            String str = authority;
            if (SyncManager.this.mNotificationMgr != null) {
                ProviderInfo providerInfo = SyncManager.this.mContext.getPackageManager().resolveContentProvider(str, 0);
                if (providerInfo != null) {
                    CharSequence authorityName = providerInfo.loadLabel(SyncManager.this.mContext.getPackageManager());
                    Intent clickIntent = new Intent(SyncManager.this.mContext, SyncActivityTooManyDeletes.class);
                    clickIntent.putExtra("account", account);
                    clickIntent.putExtra("authority", str);
                    clickIntent.putExtra("provider", authorityName.toString());
                    clickIntent.putExtra("numDeletes", numDeletes);
                    if (isActivityAvailable(clickIntent)) {
                        UserHandle user = new UserHandle(userId);
                        PendingIntent pendingIntent = PendingIntent.getActivityAsUser(SyncManager.this.mContext, 0, clickIntent, 268435456, null, user);
                        CharSequence tooManyDeletesDescFormat = SyncManager.this.mContext.getResources().getText(17039864);
                        Context contextForUser = SyncManager.this.getContextForUser(user);
                        Notification notification = new Builder(contextForUser, SystemNotificationChannels.ACCOUNT).setSmallIcon(17303480).setLargeIcon(BitmapFactory.decodeResource(SyncManager.this.mContext.getResources(), 33751680)).setTicker(SyncManager.this.mContext.getString(17039862)).setWhen(System.currentTimeMillis()).setColor(contextForUser.getColor(17170784)).setContentTitle(contextForUser.getString(17039863)).setContentText(String.format(tooManyDeletesDescFormat.toString(), new Object[]{authorityName})).setContentIntent(pendingIntent).build();
                        notification.flags |= 2;
                        SyncManager.this.mNotificationMgr.notifyAsUser(Integer.toString(account.hashCode() ^ authority.hashCode()), 18, notification, user);
                        return;
                    }
                    Log.w("SyncManager", "No activity found to handle too many deletes.");
                }
            }
        }

        private boolean isActivityAvailable(Intent intent) {
            List<ResolveInfo> list = SyncManager.this.mContext.getPackageManager().queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                if ((((ResolveInfo) list.get(i)).activityInfo.applicationInfo.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }

        public long insertStartSyncEvent(SyncOperation syncOperation) {
            long now = System.currentTimeMillis();
            EventLog.writeEvent(2720, syncOperation.toEventLog(0));
            return SyncManager.this.mSyncStorageEngine.insertStartSyncEvent(syncOperation, now);
        }

        public void stopSyncEvent(long rowId, SyncOperation syncOperation, String resultMessage, int upstreamActivity, int downstreamActivity, long elapsedTime) {
            EventLog.writeEvent(2720, syncOperation.toEventLog(1));
            SyncManager.this.mSyncStorageEngine.stopSyncEvent(rowId, elapsedTime, resultMessage, (long) downstreamActivity, (long) upstreamActivity);
        }
    }

    private class SyncTimeTracker {
        boolean mLastWasSyncing;
        private long mTimeSpentSyncing;
        long mWhenSyncStarted;

        private SyncTimeTracker() {
            this.mLastWasSyncing = false;
            this.mWhenSyncStarted = 0;
        }

        /* synthetic */ SyncTimeTracker(SyncManager x0, AnonymousClass1 x1) {
            this();
        }

        public synchronized void update() {
            boolean isSyncInProgress = SyncManager.this.mActiveSyncContexts.isEmpty() ^ 1;
            if (isSyncInProgress != this.mLastWasSyncing) {
                long now = SystemClock.elapsedRealtime();
                if (isSyncInProgress) {
                    this.mWhenSyncStarted = now;
                } else {
                    this.mTimeSpentSyncing += now - this.mWhenSyncStarted;
                }
                this.mLastWasSyncing = isSyncInProgress;
            }
        }

        public synchronized long timeSpentSyncing() {
            if (this.mLastWasSyncing) {
                return this.mTimeSpentSyncing + (SystemClock.elapsedRealtime() - this.mWhenSyncStarted);
            }
            return this.mTimeSpentSyncing;
        }
    }

    private class UpdatePeriodicSyncMessagePayload {
        public final Bundle extras;
        public final long flex;
        public final long pollFrequency;
        public final EndPoint target;

        UpdatePeriodicSyncMessagePayload(EndPoint target, long pollFrequency, long flex, Bundle extras) {
            this.target = target;
            this.pollFrequency = pollFrequency;
            this.flex = flex;
            this.extras = extras;
        }
    }

    static /* synthetic */ boolean access$1876(SyncManager x0, int x1) {
        boolean z = (byte) (x0.mProvisioned | x1);
        x0.mProvisioned = z;
        return z;
    }

    private boolean isJobIdInUseLockedH(int jobId, List<JobInfo> pendingJobs) {
        for (JobInfo job : pendingJobs) {
            if (job.getId() == jobId) {
                return true;
            }
        }
        Iterator it = this.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            if (((ActiveSyncContext) it.next()).mSyncOperation.jobId == jobId) {
                return true;
            }
        }
        return false;
    }

    private int getUnusedJobIdH() {
        int newJobId;
        do {
            newJobId = MIN_SYNC_JOB_ID + this.mRand.nextInt(10000);
        } while (isJobIdInUseLockedH(newJobId, this.mJobSchedulerInternal.getSystemScheduledPendingJobs()));
        return newJobId;
    }

    private List<SyncOperation> getAllPendingSyncs() {
        verifyJobScheduler();
        List<JobInfo> pendingJobs = this.mJobSchedulerInternal.getSystemScheduledPendingJobs();
        List<SyncOperation> pendingSyncs = new ArrayList(pendingJobs.size());
        for (JobInfo job : pendingJobs) {
            SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
            if (op != null) {
                pendingSyncs.add(op);
            }
        }
        return pendingSyncs;
    }

    private List<UserInfo> getAllUsers() {
        return this.mUserManager.getUsers();
    }

    private boolean containsAccountAndUser(AccountAndUser[] accounts, Account account, int userId) {
        int i = 0;
        while (i < accounts.length) {
            if (accounts[i].userId == userId && accounts[i].account.equals(account)) {
                return true;
            }
            i++;
        }
        return false;
    }

    private void updateRunningAccounts(EndPoint target) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_ACCOUNTS_UPDATED");
        }
        Message m = this.mSyncHandler.obtainMessage(9);
        m.obj = target;
        m.sendToTarget();
    }

    private void doDatabaseCleanup() {
        for (UserInfo user : this.mUserManager.getUsers(true)) {
            if (!user.partial) {
                this.mSyncStorageEngine.doDatabaseCleanup(AccountManagerService.getSingleton().getAccounts(user.id, this.mContext.getOpPackageName()), user.id);
            }
        }
    }

    private void clearAllBackoffs(String why) {
        this.mSyncStorageEngine.clearAllBackoffsLocked();
        rescheduleSyncs(EndPoint.USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL, why);
    }

    private boolean readDataConnectionState() {
        NetworkInfo networkInfo = getConnectivityManager().getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private String getJobStats() {
        String str;
        JobSchedulerInternal js = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("JobStats: ");
        if (js == null) {
            str = "(JobSchedulerInternal==null)";
        } else {
            str = js.getPersistStats().toString();
        }
        stringBuilder.append(str);
        return stringBuilder.toString();
    }

    private ConnectivityManager getConnectivityManager() {
        ConnectivityManager connectivityManager;
        synchronized (this) {
            if (this.mConnManagerDoNotUseDirectly == null) {
                this.mConnManagerDoNotUseDirectly = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            }
            connectivityManager = this.mConnManagerDoNotUseDirectly;
        }
        return connectivityManager;
    }

    private void cleanupJobs() {
        this.mSyncHandler.postAtFrontOfQueue(new Runnable() {
            public void run() {
                List<SyncOperation> ops = SyncManager.this.getAllPendingSyncs();
                Set<String> cleanedKeys = new HashSet();
                for (SyncOperation opx : ops) {
                    if (!cleanedKeys.contains(opx.key)) {
                        cleanedKeys.add(opx.key);
                        for (SyncOperation opy : ops) {
                            if (opx != opy) {
                                if (opx.key.equals(opy.key)) {
                                    SyncManager.this.mLogger.log("Removing duplicate sync: ", opy);
                                    SyncManager syncManager = SyncManager.this;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("cleanupJobs() x=");
                                    stringBuilder.append(opx);
                                    stringBuilder.append(" y=");
                                    stringBuilder.append(opy);
                                    syncManager.cancelJob(opy, stringBuilder.toString());
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /* JADX WARNING: Missing block: B:30:0x00d0, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void verifyJobScheduler() {
        if (this.mJobScheduler == null) {
            long token = Binder.clearCallingIdentity();
            try {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.d("SyncManager", "initializing JobScheduler object.");
                }
                this.mJobScheduler = (JobScheduler) this.mContext.getSystemService("jobscheduler");
                this.mJobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
                List<JobInfo> pendingJobs = this.mJobScheduler.getAllPendingJobs();
                int numPersistedPeriodicSyncs = 0;
                int numPersistedOneshotSyncs = 0;
                for (JobInfo job : pendingJobs) {
                    SyncOperation op = SyncOperation.maybeCreateFromJobExtras(job.getExtras());
                    if (op != null) {
                        if (op.isPeriodic) {
                            numPersistedPeriodicSyncs++;
                        } else {
                            numPersistedOneshotSyncs++;
                            this.mSyncStorageEngine.markPending(op.target, true);
                        }
                    }
                }
                String summary = new StringBuilder();
                summary.append("Loaded persisted syncs: ");
                summary.append(numPersistedPeriodicSyncs);
                summary.append(" periodic syncs, ");
                summary.append(numPersistedOneshotSyncs);
                summary.append(" oneshot syncs, ");
                summary.append(pendingJobs.size());
                summary.append(" total system server jobs, ");
                summary.append(getJobStats());
                summary = summary.toString();
                Slog.i("SyncManager", summary);
                this.mLogger.log(summary);
                cleanupJobs();
                if (ENABLE_SUSPICIOUS_CHECK && numPersistedPeriodicSyncs == 0 && likelyHasPeriodicSyncs()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Device booted with no persisted periodic syncs: ");
                    stringBuilder.append(summary);
                    Slog.wtf("SyncManager", stringBuilder.toString());
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private boolean likelyHasPeriodicSyncs() {
        boolean z = false;
        try {
            if (this.mSyncStorageEngine.getAuthorityCount() >= 6) {
                z = true;
            }
            return z;
        } catch (Throwable th) {
            return false;
        }
    }

    private JobScheduler getJobScheduler() {
        verifyJobScheduler();
        return this.mJobScheduler;
    }

    public SyncManager(Context context, boolean factoryTest) {
        IntentFilter intentFilter;
        synchronized (SyncManager.class) {
            if (sInstance == null) {
                sInstance = this;
            } else {
                Slog.wtf("SyncManager", "SyncManager instantiated multiple times");
            }
        }
        this.mContext = context;
        this.mLogger = SyncLogger.getInstance();
        SyncStorageEngine.init(context, BackgroundThread.get().getLooper());
        this.mSyncStorageEngine = SyncStorageEngine.getSingleton();
        this.mSyncStorageEngine.setOnSyncRequestListener(new OnSyncRequestListener() {
            public void onSyncRequest(EndPoint info, int reason, Bundle extras, int syncExemptionFlag) {
                SyncManager.this.scheduleSync(info.account, info.userId, reason, info.provider, extras, -2, syncExemptionFlag);
            }
        });
        this.mSyncStorageEngine.setPeriodicSyncAddedListener(new PeriodicSyncAddedListener() {
            public void onPeriodicSyncAdded(EndPoint target, Bundle extras, long pollFrequency, long flex) {
                SyncManager.this.updateOrAddPeriodicSync(target, pollFrequency, flex, extras);
            }
        });
        this.mSyncStorageEngine.setOnAuthorityRemovedListener(new OnAuthorityRemovedListener() {
            public void onAuthorityRemoved(EndPoint removedAuthority) {
                SyncManager.this.removeSyncsForAuthority(removedAuthority, "onAuthorityRemoved");
            }
        });
        this.mSyncAdapters = new SyncAdaptersCache(this.mContext);
        this.mThread = new HandlerThread("SyncManager", 10);
        this.mThread.start();
        this.mSyncHandler = new SyncHandler(this.mThread.getLooper());
        this.mSyncAdapters.setListener(new RegisteredServicesCacheListener<SyncAdapterType>() {
            public void onServiceChanged(SyncAdapterType type, int userId, boolean removed) {
                if (!removed) {
                    SyncManager.this.scheduleSync(null, -1, -3, type.authority, null, -2, 0);
                }
            }
        }, this.mSyncHandler);
        this.mRand = new Random(System.currentTimeMillis());
        this.mConstants = new SyncManagerConstants(context);
        context.registerReceiver(this.mConnectivityIntentReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (!factoryTest) {
            intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter.setPriority(1000);
            context.registerReceiver(this.mBootCompletedReceiver, intentFilter);
        }
        intentFilter = new IntentFilter("android.intent.action.DEVICE_STORAGE_LOW");
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        context.registerReceiver(this.mStorageIntentReceiver, intentFilter);
        intentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.setPriority(100);
        context.registerReceiver(this.mShutdownIntentReceiver, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        this.mContext.registerReceiverAsUser(this.mUserIntentReceiver, UserHandle.ALL, intentFilter, null, null);
        context.registerReceiver(this.mOtherIntentsReceiver, new IntentFilter("android.intent.action.TIME_SET"));
        if (factoryTest) {
            this.mNotificationMgr = null;
        } else {
            this.mNotificationMgr = (NotificationManager) context.getSystemService("notification");
        }
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mAccountManager = (AccountManager) this.mContext.getSystemService("account");
        this.mAccountManagerInternal = (AccountManagerInternal) LocalServices.getService(AccountManagerInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mAccountManagerInternal.addOnAppPermissionChangeListener(new -$$Lambda$SyncManager$HhiSFjEoPA_Hnv3xYZGfwkalc68(this));
        this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mHandleAlarmWakeLock = this.mPowerManager.newWakeLock(1, HANDLE_SYNC_ALARM_WAKE_LOCK);
        this.mHandleAlarmWakeLock.setReferenceCounted(false);
        this.mSyncManagerWakeLock = this.mPowerManager.newWakeLock(1, SYNC_LOOP_WAKE_LOCK);
        this.mSyncManagerWakeLock.setReferenceCounted(false);
        this.mProvisioned = isDeviceProvisioned();
        if (!this.mProvisioned) {
            final ContentResolver resolver = context.getContentResolver();
            ContentObserver provisionedObserver = new ContentObserver(null) {
                public void onChange(boolean selfChange) {
                    SyncManager.access$1876(SyncManager.this, SyncManager.this.isDeviceProvisioned());
                    if (SyncManager.this.mProvisioned) {
                        SyncManager.this.mSyncHandler.onDeviceProvisioned();
                        resolver.unregisterContentObserver(this);
                    }
                }
            };
            synchronized (this.mSyncHandler) {
                resolver.registerContentObserver(Global.getUriFor("device_provisioned"), false, provisionedObserver);
                this.mProvisioned |= isDeviceProvisioned();
                if (this.mProvisioned) {
                    resolver.unregisterContentObserver(provisionedObserver);
                }
            }
        }
        if (!factoryTest) {
            this.mContext.registerReceiverAsUser(this.mAccountsUpdatedReceiver, UserHandle.ALL, new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED"), null, null);
        }
        final Intent startServiceIntent = new Intent(this.mContext, SyncJobService.class);
        startServiceIntent.putExtra(SyncJobService.EXTRA_MESSENGER, new Messenger(this.mSyncHandler));
        new Handler(this.mContext.getMainLooper()).post(new Runnable() {
            public void run() {
                SyncManager.this.mContext.startService(startServiceIntent);
            }
        });
        whiteListExistingSyncAdaptersIfNeeded();
        SyncLogger syncLogger = this.mLogger;
        Object[] objArr = new Object[1];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sync manager initialized: ");
        stringBuilder.append(Build.FINGERPRINT);
        objArr[0] = stringBuilder.toString();
        syncLogger.log(objArr);
    }

    public static /* synthetic */ void lambda$new$0(SyncManager syncManager, Account account, int uid) {
        if (syncManager.mAccountManagerInternal.hasAccountAccess(account, uid)) {
            syncManager.scheduleSync(account, UserHandle.getUserId(uid), -2, null, null, 3, 0);
        }
    }

    public void onStartUser(int userHandle) {
        this.mSyncHandler.post(new -$$Lambda$SyncManager$CjX_2uO4O4xJPQnKzeqvGwd87Dc(this, userHandle));
    }

    public void onUnlockUser(int userHandle) {
        this.mSyncHandler.post(new -$$Lambda$SyncManager$6y-gkGdDn-rSLmR9G8Pz_n9zy2A(this, userHandle));
    }

    public void onStopUser(int userHandle) {
        this.mSyncHandler.post(new -$$Lambda$SyncManager$4nklbtZn-JuPLOkU32f34xZoiug(this, userHandle));
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            this.mConstants.start();
        }
    }

    private void whiteListExistingSyncAdaptersIfNeeded() {
        SyncManager syncManager = this;
        if (syncManager.mSyncStorageEngine.shouldGrantSyncAdaptersAccountAccess()) {
            List<UserInfo> users = syncManager.mUserManager.getUsers(true);
            int userCount = users.size();
            int i = 0;
            while (i < userCount) {
                UserHandle userHandle = ((UserInfo) users.get(i)).getUserHandle();
                int userId = userHandle.getIdentifier();
                for (ServiceInfo<SyncAdapterType> service : r0.mSyncAdapters.getAllServices(userId)) {
                    String packageName = service.componentName.getPackageName();
                    Account[] accountsByTypeAsUser = syncManager.mAccountManager.getAccountsByTypeAsUser(((SyncAdapterType) service.type).accountType, userHandle);
                    int length = accountsByTypeAsUser.length;
                    int i2 = 0;
                    while (i2 < length) {
                        Account account = accountsByTypeAsUser[i2];
                        if (!syncManager.canAccessAccount(account, packageName, userId)) {
                            syncManager.mAccountManager.updateAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", service.uid, true);
                        }
                        i2++;
                        syncManager = this;
                    }
                    syncManager = this;
                }
                i++;
                syncManager = this;
            }
        }
    }

    private boolean isDeviceProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private long jitterize(long minValue, long maxValue) {
        Random random = new Random(SystemClock.elapsedRealtime());
        long spread = maxValue - minValue;
        if (spread <= 2147483647L) {
            return ((long) random.nextInt((int) spread)) + minValue;
        }
        throw new IllegalArgumentException("the difference between the maxValue and the minValue must be less than 2147483647");
    }

    public SyncStorageEngine getSyncStorageEngine() {
        return this.mSyncStorageEngine;
    }

    private int getIsSyncable(Account account, int userId, String providerName) {
        int isSyncable = this.mSyncStorageEngine.getIsSyncable(account, userId, providerName);
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(userId);
        if (userInfo == null || !userInfo.isRestricted()) {
            return isSyncable;
        }
        ServiceInfo<SyncAdapterType> syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(providerName, account.type), userId);
        if (syncAdapterInfo == null) {
            return 0;
        }
        try {
            PackageInfo pInfo = AppGlobals.getPackageManager().getPackageInfo(syncAdapterInfo.componentName.getPackageName(), 0, userId);
            if (pInfo == null || pInfo.restrictedAccountType == null || !pInfo.restrictedAccountType.equals(account.type)) {
                return 0;
            }
            return isSyncable;
        } catch (RemoteException e) {
            return 0;
        }
    }

    private void setAuthorityPendingState(EndPoint info) {
        for (SyncOperation op : getAllPendingSyncs()) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                getSyncStorageEngine().markPending(info, true);
                return;
            }
        }
        getSyncStorageEngine().markPending(info, false);
    }

    public void scheduleSync(Account requestedAccount, int userId, int reason, String requestedAuthority, Bundle extras, int targetSyncState, int syncExemptionFlag) {
        scheduleSync(requestedAccount, userId, reason, requestedAuthority, extras, targetSyncState, 0, true, syncExemptionFlag);
    }

    /* JADX WARNING: Removed duplicated region for block: B:114:0x031f  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x0300  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00bb  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00b0  */
    /* JADX WARNING: Missing block: B:105:0x02f2, code:
            if (r12.mSyncStorageEngine.getSyncAutomatically(r11.account, r11.userId, r9) != false) goto L_0x02fb;
     */
    /* JADX WARNING: Missing block: B:126:0x0422, code:
            if (r11 == r57) goto L_0x0430;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void scheduleSync(Account requestedAccount, int userId, int reason, String requestedAuthority, Bundle extras, int targetSyncState, long minDelayMillis, boolean checkIfAccountReady, int syncExemptionFlag) {
        Bundle extras2;
        int i;
        int i2;
        AccountAndUser[] accounts;
        AccountAndUser[] accountAndUserArr;
        int owningUid;
        ServiceInfo<SyncAdapterType> serviceInfo;
        AccountAndUser[] accountAndUserArr2;
        String str;
        SyncManager syncManager = this;
        Account account = requestedAccount;
        int i3 = userId;
        Object obj = requestedAuthority;
        int i4 = targetSyncState;
        long j = minDelayMillis;
        int i5 = checkIfAccountReady;
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        if (extras == null) {
            extras2 = new Bundle();
        } else {
            extras2 = extras;
        }
        if (isLoggable) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("one-time sync for: ");
            stringBuilder.append(account);
            stringBuilder.append(" ");
            stringBuilder.append(extras2.toString());
            stringBuilder.append(" ");
            stringBuilder.append(obj);
            stringBuilder.append(" reason=");
            stringBuilder.append(reason);
            stringBuilder.append(" checkIfAccountReady=");
            stringBuilder.append(i5);
            stringBuilder.append(" syncExemptionFlag=");
            stringBuilder.append(syncExemptionFlag);
            Log.d("SyncManager", stringBuilder.toString());
        } else {
            i = reason;
            i2 = syncExemptionFlag;
        }
        if (account == null) {
            accounts = syncManager.mRunningAccounts;
        } else if (i3 != -1) {
            accounts = new AccountAndUser[]{new AccountAndUser(account, i3)};
        } else {
            accountAndUserArr = syncManager.mRunningAccounts;
            int length = accountAndUserArr.length;
            AccountAndUser[] accounts2 = null;
            int accounts3 = 0;
            while (accounts3 < length) {
                AccountAndUser runningAccount = accountAndUserArr[accounts3];
                AccountAndUser[] accountAndUserArr3 = accountAndUserArr;
                if (account.equals(runningAccount.account)) {
                    accounts2 = (AccountAndUser[]) ArrayUtils.appendElement(AccountAndUser.class, accounts2, runningAccount);
                }
                accounts3++;
                accountAndUserArr = accountAndUserArr3;
            }
            accountAndUserArr = accounts2;
            if (ArrayUtils.isEmpty(accountAndUserArr)) {
                int i6;
                Bundle extras3;
                long j2;
                int source;
                boolean uploadOnly = extras2.getBoolean("upload", false);
                boolean manualSync = extras2.getBoolean("force", false);
                if (manualSync) {
                    extras2.putBoolean("ignore_backoff", true);
                    extras2.putBoolean("ignore_settings", true);
                }
                boolean ignoreSettings = extras2.getBoolean("ignore_settings", false);
                if (uploadOnly) {
                    accounts3 = 1;
                } else if (manualSync) {
                    accounts3 = 3;
                } else if (obj == null) {
                    accounts3 = 2;
                } else if (extras2.containsKey("feed")) {
                    accounts3 = 5;
                } else {
                    accounts3 = 0;
                }
                length = accounts3;
                HashSet<String> syncableAuthorities = accountAndUserArr.length;
                int i7 = 0;
                while (i7 < syncableAuthorities) {
                    HashSet<String> hashSet;
                    AccountAndUser[] accounts4;
                    int i8;
                    AccountAndUser account2 = accountAndUserArr[i7];
                    if (i3 < 0 || account2.userId < 0 || i3 == account2.userId) {
                        boolean hasSyncAdapter;
                        HashSet<String> syncableAuthorities2 = new HashSet();
                        for (ServiceInfo<SyncAdapterType> syncAdapter : syncManager.mSyncAdapters.getAllServices(account2.userId)) {
                            hashSet = syncableAuthorities;
                            syncableAuthorities2.add(((SyncAdapterType) syncAdapter.type).authority);
                            syncableAuthorities = hashSet;
                        }
                        hashSet = syncableAuthorities;
                        syncableAuthorities = syncableAuthorities2;
                        if (obj != null) {
                            hasSyncAdapter = syncableAuthorities.contains(obj);
                            syncableAuthorities.clear();
                            if (hasSyncAdapter) {
                                syncableAuthorities.add(obj);
                            }
                        }
                        Iterator syncAdapterInfo = syncableAuthorities.iterator();
                        while (syncAdapterInfo.hasNext()) {
                            String authority = (String) syncAdapterInfo.next();
                            Iterator it = syncAdapterInfo;
                            AccountAndUser[] accounts5 = accountAndUserArr;
                            accountAndUserArr = syncManager.computeSyncable(account2.account, account2.userId, authority, i5 ^ 1);
                            if (accountAndUserArr != null) {
                                String authority2 = authority;
                                ServiceInfo<SyncAdapterType> syncAdapterInfo2 = syncManager.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(authority, account2.account.type), account2.userId);
                                if (syncAdapterInfo2 != null) {
                                    int owningUid2 = syncAdapterInfo2.uid;
                                    HashSet<String> syncableAuthorities3;
                                    int source2;
                                    Iterator it2;
                                    int isSyncable;
                                    Bundle extras4;
                                    AccountAndUser account3;
                                    String str2;
                                    int i9;
                                    if (accountAndUserArr == 3) {
                                        if (isLoggable) {
                                            owningUid = owningUid2;
                                            Slog.v("SyncManager", "    Not scheduling sync operation: isSyncable == SYNCABLE_NO_ACCOUNT_ACCESS");
                                        } else {
                                            owningUid = owningUid2;
                                        }
                                        owningUid2 = length;
                                        length = new Bundle(extras2);
                                        String packageName = syncAdapterInfo2.componentName.getPackageName();
                                        try {
                                            syncableAuthorities3 = syncableAuthorities;
                                            String syncableAuthorities4 = packageName;
                                            try {
                                                if (syncManager.mPackageManagerInternal.wasPackageEverLaunched(syncableAuthorities4, i3)) {
                                                    source2 = owningUid2;
                                                    it2 = it;
                                                    isSyncable = accountAndUserArr;
                                                    accounts4 = accounts5;
                                                    extras4 = extras2;
                                                    i6 = i5;
                                                    i8 = i7;
                                                    account3 = account2;
                                                    syncManager.mAccountManagerInternal.requestAccountAccess(account2.account, syncableAuthorities4, i3, new RemoteCallback(new -$$Lambda$SyncManager$o7UdjgcI2E4HDw_-2JMHW-T1SJs(syncManager, account2, i3, reason, authority2, length, i4, minDelayMillis, syncExemptionFlag)));
                                                    i2 = syncExemptionFlag;
                                                    accountAndUserArr = accounts4;
                                                    i7 = i8;
                                                    syncAdapterInfo = it2;
                                                    i5 = i6;
                                                    syncableAuthorities = syncableAuthorities3;
                                                    length = source2;
                                                    extras2 = extras4;
                                                    account2 = account3;
                                                    i4 = targetSyncState;
                                                } else {
                                                    length = owningUid2;
                                                    syncAdapterInfo = it;
                                                    accountAndUserArr = accounts5;
                                                    syncableAuthorities = syncableAuthorities3;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                source2 = owningUid2;
                                                serviceInfo = syncAdapterInfo2;
                                                accountAndUserArr2 = accountAndUserArr;
                                                extras4 = extras2;
                                                i6 = i5;
                                                i8 = i7;
                                                account3 = account2;
                                                owningUid2 = syncableAuthorities4;
                                                it2 = it;
                                                accounts4 = accounts5;
                                                str2 = authority2;
                                                i9 = owningUid;
                                                i = reason;
                                                i2 = syncExemptionFlag;
                                                accountAndUserArr = accounts4;
                                                i7 = i8;
                                                syncAdapterInfo = it2;
                                                i5 = i6;
                                                syncableAuthorities = syncableAuthorities3;
                                                length = source2;
                                                extras2 = extras4;
                                                account2 = account3;
                                                i4 = targetSyncState;
                                                str = requestedAuthority;
                                            }
                                        } catch (IllegalArgumentException e2) {
                                            source2 = owningUid2;
                                            serviceInfo = syncAdapterInfo2;
                                            accountAndUserArr2 = accountAndUserArr;
                                            extras4 = extras2;
                                            i6 = i5;
                                            i8 = i7;
                                            account3 = account2;
                                            syncableAuthorities3 = syncableAuthorities;
                                            it2 = it;
                                            accounts4 = accounts5;
                                            str2 = authority2;
                                            i9 = owningUid;
                                            owningUid2 = packageName;
                                            i = reason;
                                            i2 = syncExemptionFlag;
                                            accountAndUserArr = accounts4;
                                            i7 = i8;
                                            syncAdapterInfo = it2;
                                            i5 = i6;
                                            syncableAuthorities = syncableAuthorities3;
                                            length = source2;
                                            extras2 = extras4;
                                            account2 = account3;
                                            i4 = targetSyncState;
                                            str = requestedAuthority;
                                        }
                                    } else {
                                        AccountAndUser account4;
                                        int isSyncable2;
                                        i9 = owningUid2;
                                        isSyncable = accountAndUserArr;
                                        extras4 = extras2;
                                        source2 = length;
                                        i6 = i5;
                                        i8 = i7;
                                        account3 = account2;
                                        syncableAuthorities3 = syncableAuthorities;
                                        it2 = it;
                                        accounts4 = accounts5;
                                        str2 = authority2;
                                        ServiceInfo<SyncAdapterType> syncAdapterInfo3 = syncAdapterInfo2;
                                        hasSyncAdapter = ((SyncAdapterType) syncAdapterInfo3.type).allowParallelSyncs();
                                        boolean isAlwaysSyncable = ((SyncAdapterType) syncAdapterInfo3.type).isAlwaysSyncable();
                                        if (i6 == 0 && isSyncable < 0 && isAlwaysSyncable) {
                                            account4 = account3;
                                            syncManager.mSyncStorageEngine.setIsSyncable(account4.account, account4.userId, str2, 1, -1);
                                            isSyncable2 = 1;
                                        } else {
                                            account4 = account3;
                                            isSyncable2 = isSyncable;
                                        }
                                        i5 = targetSyncState;
                                        if ((i5 == -2 || i5 == isSyncable2) && (((SyncAdapterType) syncAdapterInfo3.type).supportsUploading() || !uploadOnly)) {
                                            boolean z;
                                            String authority3;
                                            if (isSyncable2 < 0 || ignoreSettings) {
                                                authority3 = str2;
                                            } else {
                                                authority3 = syncManager.mSyncStorageEngine.getMasterSyncAutomatically(account4.userId) ? str2 : str2;
                                                z = false;
                                                StringBuilder stringBuilder2;
                                                if (!z) {
                                                    AccountAndUser account5;
                                                    Bundle extras5;
                                                    long j3;
                                                    AccountAndUser account6;
                                                    EndPoint info = new EndPoint(account4.account, authority3, account4.userId);
                                                    long delayUntil = syncManager.mSyncStorageEngine.getDelayUntilTime(info);
                                                    String owningPackage = syncAdapterInfo3.componentName.getPackageName();
                                                    boolean allowParallelSyncs;
                                                    AccountAndUser[] accountAndUserArr4;
                                                    EndPoint endPoint;
                                                    int isSyncable3;
                                                    boolean z2;
                                                    AccountAndUser account7;
                                                    if (isSyncable2 != -1) {
                                                        allowParallelSyncs = hasSyncAdapter;
                                                        accountAndUserArr4 = -1;
                                                        endPoint = info;
                                                        isSyncable3 = isSyncable2;
                                                        account5 = account4;
                                                        z2 = isAlwaysSyncable;
                                                        isSyncable2 = source2;
                                                        extras5 = extras4;
                                                        j3 = minDelayMillis;
                                                        i4 = targetSyncState;
                                                        if (i4 == -2) {
                                                            accounts3 = isSyncable3;
                                                        }
                                                        if (isLoggable) {
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("scheduleSync: delay until ");
                                                            stringBuilder2.append(delayUntil);
                                                            stringBuilder2.append(", source ");
                                                            stringBuilder2.append(isSyncable2);
                                                            stringBuilder2.append(", account , authority ");
                                                            stringBuilder2.append(authority3);
                                                            stringBuilder2.append(", extras ");
                                                            accountAndUserArr = extras5;
                                                            stringBuilder2.append(accountAndUserArr);
                                                            Slog.v("SyncManager", stringBuilder2.toString());
                                                        } else {
                                                            accountAndUserArr = extras5;
                                                        }
                                                        SyncOperation syncOperation = r1;
                                                        extras3 = accountAndUserArr;
                                                        j2 = j3;
                                                        account6 = account5;
                                                        source = isSyncable2;
                                                        SyncOperation syncOperation2 = new SyncOperation(account5.account, account5.userId, i9, owningPackage, reason, isSyncable2, authority3, extras3, allowParallelSyncs, syncExemptionFlag);
                                                        syncManager.postScheduleSyncMessage(syncOperation, j2);
                                                        i = reason;
                                                        i4 = targetSyncState;
                                                        i2 = syncExemptionFlag;
                                                        extras2 = extras3;
                                                        account2 = account6;
                                                        length = source;
                                                        accountAndUserArr = accounts4;
                                                        i7 = i8;
                                                        syncAdapterInfo = it2;
                                                        i5 = i6;
                                                        syncableAuthorities = syncableAuthorities3;
                                                        i3 = userId;
                                                    } else if (i6 != 0) {
                                                        Bundle extras6 = extras4;
                                                        extras2 = new Bundle(extras6);
                                                        Context context = syncManager.mContext;
                                                        i3 = account4.userId;
                                                        extras5 = extras6;
                                                        accountAndUserArr4 = -1;
                                                        accountAndUserArr = account4;
                                                        account7 = account4;
                                                        account4 = delayUntil;
                                                        sendOnUnsyncableAccount(context, syncAdapterInfo3, i3, new -$$Lambda$SyncManager$Dly2yZUw2lCDXffoc_fe8npXe2U(syncManager, accountAndUserArr, reason, authority3, extras2, i5, minDelayMillis, syncExemptionFlag));
                                                        syncAdapterInfo3 = minDelayMillis;
                                                        source = source2;
                                                        extras3 = extras5;
                                                        account6 = account7;
                                                        syncManager = this;
                                                        i = reason;
                                                        i4 = targetSyncState;
                                                        i2 = syncExemptionFlag;
                                                        extras2 = extras3;
                                                        account2 = account6;
                                                        length = source;
                                                        accountAndUserArr = accounts4;
                                                        i7 = i8;
                                                        syncAdapterInfo = it2;
                                                        i5 = i6;
                                                        syncableAuthorities = syncableAuthorities3;
                                                        i3 = userId;
                                                    } else {
                                                        allowParallelSyncs = hasSyncAdapter;
                                                        accountAndUserArr4 = -1;
                                                        endPoint = info;
                                                        String authority4 = authority3;
                                                        isSyncable3 = isSyncable2;
                                                        account7 = account4;
                                                        z2 = isAlwaysSyncable;
                                                        extras5 = extras4;
                                                        account4 = delayUntil;
                                                        hasSyncAdapter = new Bundle();
                                                        hasSyncAdapter.putBoolean("initialize", true);
                                                        if (isLoggable) {
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("schedule initialisation Sync:, delay until ");
                                                            stringBuilder2.append(account4);
                                                            stringBuilder2.append(", run by ");
                                                            stringBuilder2.append(0);
                                                            stringBuilder2.append(", flexMillis ");
                                                            stringBuilder2.append(0);
                                                            stringBuilder2.append(", source ");
                                                            isSyncable2 = source2;
                                                            stringBuilder2.append(isSyncable2);
                                                            stringBuilder2.append(", account , authority ");
                                                            authority3 = authority4;
                                                            stringBuilder2.append(authority3);
                                                            stringBuilder2.append(", extras ");
                                                            stringBuilder2.append(hasSyncAdapter);
                                                            Slog.v("SyncManager", stringBuilder2.toString());
                                                        } else {
                                                            isSyncable2 = source2;
                                                            authority3 = authority4;
                                                        }
                                                        account5 = account7;
                                                        j3 = minDelayMillis;
                                                        delayUntil = account4;
                                                        syncManager = this;
                                                        syncManager.postScheduleSyncMessage(new SyncOperation(account5.account, account5.userId, i9, owningPackage, reason, isSyncable2, authority3, hasSyncAdapter, allowParallelSyncs, syncExemptionFlag), j3);
                                                    }
                                                    syncAdapterInfo3 = j3;
                                                    account6 = account5;
                                                    source = isSyncable2;
                                                    extras3 = extras5;
                                                    i = reason;
                                                    i4 = targetSyncState;
                                                    i2 = syncExemptionFlag;
                                                    extras2 = extras3;
                                                    account2 = account6;
                                                    length = source;
                                                    accountAndUserArr = accounts4;
                                                    i7 = i8;
                                                    syncAdapterInfo = it2;
                                                    i5 = i6;
                                                    syncableAuthorities = syncableAuthorities3;
                                                    i3 = userId;
                                                } else if (isLoggable) {
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("scheduleSync: sync of , ");
                                                    stringBuilder2.append(authority3);
                                                    stringBuilder2.append(" is not allowed, dropping request");
                                                    Log.d("SyncManager", stringBuilder2.toString());
                                                }
                                            }
                                            z = true;
                                            if (!z) {
                                            }
                                        }
                                        i = reason;
                                        i2 = syncExemptionFlag;
                                        account2 = account4;
                                        accountAndUserArr = accounts4;
                                        i7 = i8;
                                        syncAdapterInfo = it2;
                                        syncableAuthorities = syncableAuthorities3;
                                        length = source2;
                                        extras2 = extras4;
                                        str = requestedAuthority;
                                        i4 = i5;
                                        i5 = i6;
                                    }
                                    str = requestedAuthority;
                                }
                            }
                            syncAdapterInfo = it;
                            accountAndUserArr = accounts5;
                        }
                        accounts4 = accountAndUserArr;
                        extras3 = extras2;
                        source = length;
                        i6 = i5;
                        i8 = i7;
                    } else {
                        accounts4 = accountAndUserArr;
                        extras3 = extras2;
                        source = length;
                        i6 = i5;
                        i8 = i7;
                        hashSet = syncableAuthorities;
                    }
                    j2 = minDelayMillis;
                    i7 = i8 + 1;
                    i = reason;
                    i4 = targetSyncState;
                    i2 = syncExemptionFlag;
                    extras2 = extras3;
                    length = source;
                    accountAndUserArr = accounts4;
                    i5 = i6;
                    syncableAuthorities = hashSet;
                    i3 = userId;
                    str = requestedAuthority;
                }
                extras3 = extras2;
                source = length;
                i6 = i5;
                j2 = minDelayMillis;
                return;
            }
            if (isLoggable) {
                Slog.v("SyncManager", "scheduleSync: no accounts configured, dropping");
            }
            return;
        }
        accountAndUserArr = accounts;
        if (ArrayUtils.isEmpty(accountAndUserArr)) {
        }
    }

    public static /* synthetic */ void lambda$scheduleSync$4(SyncManager syncManager, AccountAndUser account, int userId, int reason, String authority, Bundle finalExtras, int targetSyncState, long minDelayMillis, int syncExemptionFlag, Bundle result) {
        Bundle bundle = result;
        if (bundle == null || !bundle.getBoolean("booleanResult")) {
            AccountAndUser accountAndUser = account;
            return;
        }
        syncManager.scheduleSync(account.account, userId, reason, authority, finalExtras, targetSyncState, minDelayMillis, true, syncExemptionFlag);
    }

    public static /* synthetic */ void lambda$scheduleSync$5(SyncManager syncManager, AccountAndUser account, int reason, String authority, Bundle finalExtras, int targetSyncState, long minDelayMillis, int syncExemptionFlag) {
        AccountAndUser accountAndUser = account;
        syncManager.scheduleSync(accountAndUser.account, accountAndUser.userId, reason, authority, finalExtras, targetSyncState, minDelayMillis, false, syncExemptionFlag);
    }

    public int computeSyncable(Account account, int userId, String authority, boolean checkAccountAccess) {
        int status = getIsSyncable(account, userId, authority);
        if (status == 0) {
            return 0;
        }
        ServiceInfo<SyncAdapterType> syncAdapterInfo = this.mSyncAdapters.getServiceInfo(SyncAdapterType.newKey(authority, account.type), userId);
        if (syncAdapterInfo == null) {
            return 0;
        }
        int owningUid = syncAdapterInfo.uid;
        String owningPackage = syncAdapterInfo.componentName.getPackageName();
        try {
            if (ActivityManager.getService().isAppStartModeDisabled(owningUid, owningPackage)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not scheduling job ");
                stringBuilder.append(syncAdapterInfo.uid);
                stringBuilder.append(":");
                stringBuilder.append(syncAdapterInfo.componentName);
                stringBuilder.append(" -- package not allowed to start");
                Slog.w("SyncManager", stringBuilder.toString());
                return 0;
            }
        } catch (RemoteException e) {
        }
        if (!checkAccountAccess || canAccessAccount(account, owningPackage, owningUid)) {
            return status;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Access to ");
        stringBuilder2.append(account);
        stringBuilder2.append(" denied for package ");
        stringBuilder2.append(owningPackage);
        stringBuilder2.append(" in UID ");
        stringBuilder2.append(syncAdapterInfo.uid);
        Log.w("SyncManager", stringBuilder2.toString());
        return 3;
    }

    private boolean canAccessAccount(Account account, String packageName, int uid) {
        if (this.mAccountManager.hasAccountAccess(account, packageName, UserHandle.getUserHandleForUid(uid))) {
            return true;
        }
        try {
            this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, DumpState.DUMP_DEXOPT, UserHandle.getUserId(uid));
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void removeSyncsForAuthority(EndPoint info, String why) {
        this.mLogger.log("removeSyncsForAuthority: ", info);
        verifyJobScheduler();
        for (SyncOperation op : getAllPendingSyncs()) {
            if (op.target.matchesSpec(info)) {
                this.mLogger.log("canceling: ", op);
                cancelJob(op, why);
            }
        }
    }

    public void removePeriodicSync(EndPoint target, Bundle extras, String why) {
        Message m = this.mSyncHandler;
        SyncHandler syncHandler = this.mSyncHandler;
        m = m.obtainMessage(14, Pair.create(target, why));
        m.setData(extras);
        m.sendToTarget();
    }

    public void updateOrAddPeriodicSync(EndPoint target, long pollFrequency, long flex, Bundle extras) {
        this.mSyncHandler.obtainMessage(13, new UpdatePeriodicSyncMessagePayload(target, pollFrequency, flex, extras)).sendToTarget();
    }

    public List<PeriodicSync> getPeriodicSyncs(EndPoint target) {
        EndPoint endPoint;
        List<SyncOperation> ops = getAllPendingSyncs();
        List<PeriodicSync> periodicSyncs = new ArrayList();
        for (SyncOperation op : ops) {
            if (!op.isPeriodic) {
                endPoint = target;
            } else if (op.target.matchesSpec(target)) {
                periodicSyncs.add(new PeriodicSync(op.target.account, op.target.provider, op.extras, op.periodMillis / 1000, op.flexMillis / 1000));
            }
        }
        endPoint = target;
        return periodicSyncs;
    }

    public void scheduleLocalSync(Account account, int userId, int reason, String authority, int syncExemptionFlag) {
        Bundle extras = new Bundle();
        extras.putBoolean("upload", true);
        scheduleSync(account, userId, reason, authority, extras, -2, LOCAL_SYNC_DELAY, true, syncExemptionFlag);
    }

    public SyncAdapterType[] getSyncAdapterTypes(int userId) {
        Collection<ServiceInfo<SyncAdapterType>> serviceInfos = this.mSyncAdapters.getAllServices(userId);
        SyncAdapterType[] types = new SyncAdapterType[serviceInfos.size()];
        int i = 0;
        for (ServiceInfo<SyncAdapterType> serviceInfo : serviceInfos) {
            types[i] = (SyncAdapterType) serviceInfo.type;
            i++;
        }
        return types;
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String authority, int userId) {
        return this.mSyncAdapters.getSyncAdapterPackagesForAuthority(authority, userId);
    }

    private void sendSyncFinishedOrCanceledMessage(ActiveSyncContext syncContext, SyncResult syncResult) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_SYNC_FINISHED");
        }
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 1;
        msg.obj = new SyncFinishedOrCancelledMessagePayload(syncContext, syncResult);
        this.mSyncHandler.sendMessage(msg);
    }

    private void sendCancelSyncsMessage(EndPoint info, Bundle extras, String why) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "sending MESSAGE_CANCEL");
        }
        this.mLogger.log("sendCancelSyncsMessage() ep=", info, " why=", why);
        Message msg = this.mSyncHandler.obtainMessage();
        msg.what = 6;
        msg.setData(extras);
        msg.obj = info;
        this.mSyncHandler.sendMessage(msg);
    }

    private void postMonitorSyncProgressMessage(ActiveSyncContext activeSyncContext) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "posting MESSAGE_SYNC_MONITOR in 60s");
        }
        activeSyncContext.mBytesTransferredAtLastPoll = getTotalBytesTransferredByUid(activeSyncContext.mSyncAdapterUid);
        activeSyncContext.mLastPolledTimeElapsed = SystemClock.elapsedRealtime();
        this.mSyncHandler.sendMessageDelayed(this.mSyncHandler.obtainMessage(8, activeSyncContext), 60000);
    }

    private void postScheduleSyncMessage(SyncOperation syncOperation, long minDelayMillis) {
        ScheduleSyncMessagePayload payload = new ScheduleSyncMessagePayload(syncOperation, minDelayMillis);
        SyncHandler syncHandler = this.mSyncHandler;
        SyncHandler syncHandler2 = this.mSyncHandler;
        syncHandler.obtainMessage(12, payload).sendToTarget();
    }

    private long getTotalBytesTransferredByUid(int uid) {
        return TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
    }

    private void clearBackoffSetting(EndPoint target, String why) {
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        if (backoff == null || ((Long) backoff.first).longValue() != -1 || ((Long) backoff.second).longValue() != -1) {
            if (Log.isLoggable("SyncManager", 2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Clearing backoffs for ");
                stringBuilder.append(target);
                Slog.v("SyncManager", stringBuilder.toString());
            }
            this.mSyncStorageEngine.setBackoff(target, -1, -1);
            rescheduleSyncs(target, why);
        }
    }

    private void increaseBackoffSetting(EndPoint target) {
        long initialRetryMs;
        long newDelayInMs;
        EndPoint endPoint = target;
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> previousSettings = this.mSyncStorageEngine.getBackoff(endPoint);
        long newDelayInMs2 = -1;
        if (previousSettings != null) {
            if (now < ((Long) previousSettings.first).longValue()) {
                if (Log.isLoggable("SyncManager", 2)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Still in backoff, do not increase it. Remaining: ");
                    stringBuilder.append((((Long) previousSettings.first).longValue() - now) / 1000);
                    stringBuilder.append(" seconds.");
                    Slog.v("SyncManager", stringBuilder.toString());
                }
                return;
            }
            newDelayInMs2 = (long) (((float) ((Long) previousSettings.second).longValue()) * this.mConstants.getRetryTimeIncreaseFactor());
        }
        if (newDelayInMs2 <= 0) {
            initialRetryMs = (long) (this.mConstants.getInitialSyncRetryTimeInSeconds() * 1000);
            newDelayInMs2 = jitterize(initialRetryMs, (long) (((double) initialRetryMs) * 1.1d));
        }
        initialRetryMs = (long) this.mConstants.getMaxSyncRetryTimeInSeconds();
        if (newDelayInMs2 > initialRetryMs * 1000) {
            newDelayInMs = 1000 * initialRetryMs;
        } else {
            newDelayInMs = newDelayInMs2;
        }
        long backoff = now + newDelayInMs;
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Backoff until: ");
            stringBuilder2.append(backoff);
            stringBuilder2.append(", delayTime: ");
            stringBuilder2.append(newDelayInMs);
            Slog.v("SyncManager", stringBuilder2.toString());
        }
        this.mSyncStorageEngine.setBackoff(endPoint, backoff, newDelayInMs);
        rescheduleSyncs(endPoint, "increaseBackoffSetting");
    }

    private void rescheduleSyncs(EndPoint target, String why) {
        this.mLogger.log("rescheduleSyncs() ep=", target, " why=", why);
        int count = 0;
        for (SyncOperation op : getAllPendingSyncs()) {
            if (!op.isPeriodic && op.target.matchesSpec(target)) {
                count++;
                cancelJob(op, why);
                postScheduleSyncMessage(op, 0);
            }
        }
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Rescheduled ");
            stringBuilder.append(count);
            stringBuilder.append(" syncs for ");
            stringBuilder.append(target);
            Slog.v("SyncManager", stringBuilder.toString());
        }
    }

    private void setDelayUntilTime(EndPoint target, long delayUntilSeconds) {
        long newDelayUntilTime;
        long delayUntil = 1000 * delayUntilSeconds;
        long absoluteNow = System.currentTimeMillis();
        if (delayUntil > absoluteNow) {
            newDelayUntilTime = SystemClock.elapsedRealtime() + (delayUntil - absoluteNow);
        } else {
            newDelayUntilTime = 0;
        }
        this.mSyncStorageEngine.setDelayUntilTime(target, newDelayUntilTime);
        if (Log.isLoggable("SyncManager", 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Delay Until time set to ");
            stringBuilder.append(newDelayUntilTime);
            stringBuilder.append(" for ");
            stringBuilder.append(target);
            Slog.v("SyncManager", stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("delayUntil newDelayUntilTime: ");
        stringBuilder2.append(newDelayUntilTime);
        rescheduleSyncs(target, stringBuilder2.toString());
    }

    private boolean isAdapterDelayed(EndPoint target) {
        long now = SystemClock.elapsedRealtime();
        Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(target);
        if ((backoff == null || ((Long) backoff.first).longValue() == -1 || ((Long) backoff.first).longValue() <= now) && this.mSyncStorageEngine.getDelayUntilTime(target) <= now) {
            return false;
        }
        return true;
    }

    public void cancelActiveSync(EndPoint info, Bundle extras, String why) {
        sendCancelSyncsMessage(info, extras, why);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation) {
        scheduleSyncOperationH(syncOperation, 0);
    }

    private void scheduleSyncOperationH(SyncOperation syncOperation, long minDelay) {
        SyncOperation syncOperation2 = syncOperation;
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        if (syncOperation2 == null) {
            Slog.e("SyncManager", "Can't schedule null sync operation.");
            return;
        }
        long minDelay2;
        boolean z;
        if (syncOperation.ignoreBackoff()) {
            minDelay2 = minDelay;
        } else {
            Pair<Long, Long> backoff = this.mSyncStorageEngine.getBackoff(syncOperation2.target);
            if (backoff == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't find backoff values for ");
                stringBuilder.append(syncOperation2.target);
                Slog.e("SyncManager", stringBuilder.toString());
                backoff = new Pair(Long.valueOf(-1), Long.valueOf(-1));
            }
            long now = SystemClock.elapsedRealtime();
            long backoffDelay = ((Long) backoff.first).longValue() == -1 ? 0 : ((Long) backoff.first).longValue() - now;
            long delayUntil = this.mSyncStorageEngine.getDelayUntilTime(syncOperation2.target);
            long delayUntilDelay = delayUntil > now ? delayUntil - now : 0;
            if (isLoggable) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("backoff delay:");
                stringBuilder2.append(backoffDelay);
                stringBuilder2.append(" delayUntil delay:");
                stringBuilder2.append(delayUntilDelay);
                Slog.v("SyncManager", stringBuilder2.toString());
            }
            Pair<Long, Long> pair = backoff;
            minDelay2 = Math.max(minDelay, Math.max(backoffDelay, delayUntilDelay));
        }
        if (minDelay2 < 0) {
            minDelay2 = 0;
        }
        if (!syncOperation2.isPeriodic) {
            int duplicatesCount;
            int inheritedSyncExemptionFlag = 0;
            Iterator it = this.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                if (((ActiveSyncContext) it.next()).mSyncOperation.key.equals(syncOperation2.key)) {
                    if (isLoggable) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Duplicate sync is already running. Not scheduling ");
                        stringBuilder3.append(syncOperation2);
                        Log.v("SyncManager", stringBuilder3.toString());
                    }
                    return;
                }
            }
            int duplicatesCount2 = 0;
            syncOperation2.expectedRuntime = SystemClock.elapsedRealtime() + minDelay2;
            List<SyncOperation> pending = getAllPendingSyncs();
            SyncOperation syncToRun = syncOperation2;
            for (SyncOperation op : pending) {
                if (!op.isPeriodic) {
                    if (op.key.equals(syncOperation2.key)) {
                        duplicatesCount = duplicatesCount2;
                        if (syncToRun.expectedRuntime > op.expectedRuntime) {
                            syncToRun = op;
                        }
                        duplicatesCount2 = duplicatesCount + 1;
                    } else {
                        duplicatesCount = duplicatesCount2;
                    }
                }
            }
            duplicatesCount = duplicatesCount2;
            if (duplicatesCount2 > 1) {
                Slog.e("SyncManager", "FATAL ERROR! File a bug if you see this.");
            }
            if (syncOperation2 != syncToRun && minDelay2 == 0 && syncToRun.syncExemptionFlag < syncOperation2.syncExemptionFlag) {
                syncToRun = syncOperation2;
                inheritedSyncExemptionFlag = Math.max(0, syncToRun.syncExemptionFlag);
            }
            for (SyncOperation op2 : pending) {
                if (!op2.isPeriodic) {
                    if (op2.key.equals(syncOperation2.key) && op2 != syncToRun) {
                        if (isLoggable) {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Cancelling duplicate sync ");
                            stringBuilder4.append(op2);
                            Slog.v("SyncManager", stringBuilder4.toString());
                        }
                        inheritedSyncExemptionFlag = Math.max(inheritedSyncExemptionFlag, op2.syncExemptionFlag);
                        cancelJob(op2, "scheduleSyncOperationH-duplicate");
                    }
                }
            }
            if (syncToRun != syncOperation2) {
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Not scheduling because a duplicate exists:");
                stringBuilder5.append(syncOperation2);
                Slog.i("SyncManager", stringBuilder5.toString());
                return;
            } else if (inheritedSyncExemptionFlag > 0) {
                syncOperation2.syncExemptionFlag = inheritedSyncExemptionFlag;
            }
        }
        if (syncOperation2.jobId == -1) {
            syncOperation2.jobId = getUnusedJobIdH();
        }
        if (isLoggable) {
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("scheduling sync operation ");
            stringBuilder6.append(syncOperation.toString());
            Slog.v("SyncManager", stringBuilder6.toString());
        }
        JobInfo.Builder b = new JobInfo.Builder(syncOperation2.jobId, new ComponentName(this.mContext, SyncJobService.class)).setExtras(syncOperation.toJobInfoExtras()).setRequiredNetworkType(syncOperation.isNotAllowedOnMetered() ? 2 : 1).setPersisted(true).setPriority(syncOperation.findPriority()).setFlags(syncOperation.isAppStandbyExempted() ? 8 : 0);
        if (syncOperation2.isPeriodic) {
            b.setPeriodic(syncOperation2.periodMillis, syncOperation2.flexMillis);
            z = true;
        } else {
            if (minDelay2 > 0) {
                b.setMinimumLatency(minDelay2);
            }
            z = true;
            getSyncStorageEngine().markPending(syncOperation2.target, true);
        }
        if (syncOperation2.extras.getBoolean("require_charging")) {
            b.setRequiresCharging(z);
        }
        if (syncOperation2.syncExemptionFlag == 2) {
            LocalService dic = (LocalService) LocalServices.getService(LocalService.class);
            if (dic != null) {
                dic.addPowerSaveTempWhitelistApp(1000, syncOperation2.owningPackage, (long) (this.mConstants.getKeyExemptionTempWhitelistDurationInSeconds() * 1000), UserHandle.getUserId(syncOperation2.owningUid), false, "sync by top app");
            }
        }
        if (syncOperation.isAppStandbyExempted()) {
            UsageStatsManagerInternal usmi = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
            if (usmi != null) {
                usmi.reportExemptedSyncScheduled(syncOperation2.owningPackage, UserHandle.getUserId(syncOperation2.owningUid));
            }
        }
        getJobScheduler().scheduleAsPackage(b.build(), syncOperation2.owningPackage, syncOperation2.target.userId, syncOperation.wakeLockName());
    }

    public void clearScheduledSyncOperations(EndPoint info) {
        for (SyncOperation op : getAllPendingSyncs()) {
            if (!op.isPeriodic && op.target.matchesSpec(info)) {
                cancelJob(op, "clearScheduledSyncOperations");
                getSyncStorageEngine().markPending(op.target, false);
            }
        }
        this.mSyncStorageEngine.setBackoff(info, -1, -1);
    }

    public void cancelScheduledSyncOperation(EndPoint info, Bundle extras) {
        for (SyncOperation op : getAllPendingSyncs()) {
            if (!op.isPeriodic && op.target.matchesSpec(info) && syncExtrasEquals(extras, op.extras, false)) {
                cancelJob(op, "cancelScheduledSyncOperation");
            }
        }
        setAuthorityPendingState(info);
        if (!this.mSyncStorageEngine.isSyncPending(info)) {
            this.mSyncStorageEngine.setBackoff(info, -1, -1);
        }
    }

    private void maybeRescheduleSync(SyncResult syncResult, SyncOperation operation) {
        StringBuilder stringBuilder;
        boolean isLoggable = Log.isLoggable("SyncManager", 3);
        if (isLoggable) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("encountered error(s) during the sync: ");
            stringBuilder.append(syncResult);
            stringBuilder.append(", ");
            stringBuilder.append(operation);
            Log.d("SyncManager", stringBuilder.toString());
        }
        if (operation.extras.getBoolean("ignore_backoff", false)) {
            operation.extras.remove("ignore_backoff");
        }
        if (!operation.extras.getBoolean("do_not_retry", false) || syncResult.syncAlreadyInProgress) {
            if (operation.extras.getBoolean("upload", false) && !syncResult.syncAlreadyInProgress) {
                operation.extras.remove("upload");
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("retrying sync operation as a two-way sync because an upload-only sync encountered an error: ");
                    stringBuilder.append(operation);
                    Log.d("SyncManager", stringBuilder.toString());
                }
                scheduleSyncOperationH(operation);
            } else if (syncResult.tooManyRetries) {
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("not retrying sync operation because it retried too many times: ");
                    stringBuilder.append(operation);
                    Log.d("SyncManager", stringBuilder.toString());
                }
            } else if (syncResult.madeSomeProgress()) {
                if (isLoggable) {
                    Log.d("SyncManager", "retrying sync operation because even though it had an error it achieved some success");
                }
                scheduleSyncOperationH(operation);
            } else if (syncResult.syncAlreadyInProgress) {
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("retrying sync operation that failed because there was already a sync in progress: ");
                    stringBuilder.append(operation);
                    Log.d("SyncManager", stringBuilder.toString());
                }
                scheduleSyncOperationH(operation, 10000);
            } else if (syncResult.hasSoftError()) {
                if (isLoggable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("retrying sync operation because it encountered a soft error: ");
                    stringBuilder.append(operation);
                    Log.d("SyncManager", stringBuilder.toString());
                }
                scheduleSyncOperationH(operation);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("not retrying sync operation because the error is a hard error: ");
                stringBuilder.append(operation);
                Log.d("SyncManager", stringBuilder.toString());
            }
        } else if (isLoggable) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("not retrying sync operation because SYNC_EXTRAS_DO_NOT_RETRY was specified ");
            stringBuilder.append(operation);
            Log.d("SyncManager", stringBuilder.toString());
        }
    }

    private void onUserUnlocked(int userId) {
        AccountManagerService.getSingleton().validateAccounts(userId);
        this.mSyncAdapters.invalidateCache(userId);
        updateRunningAccounts(new EndPoint(null, null, userId));
        for (Account account : AccountManagerService.getSingleton().getAccounts(userId, this.mContext.getOpPackageName())) {
            scheduleSync(account, userId, -8, null, null, -1, 0);
        }
    }

    private void onUserStopped(int userId) {
        updateRunningAccounts(null);
        cancelActiveSync(new EndPoint(null, null, userId), null, "onUserStopped");
    }

    private void onUserRemoved(int userId) {
        this.mLogger.log("onUserRemoved: u", Integer.valueOf(userId));
        updateRunningAccounts(null);
        this.mSyncStorageEngine.doDatabaseCleanup(new Account[0], userId);
        for (SyncOperation op : getAllPendingSyncs()) {
            if (op.target.userId == userId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("user removed u");
                stringBuilder.append(userId);
                cancelJob(op, stringBuilder.toString());
            }
        }
    }

    static Intent getAdapterBindIntent(Context context, ComponentName syncAdapterComponent, int userId) {
        Intent intent = new Intent();
        intent.setAction("android.content.SyncAdapter");
        intent.setComponent(syncAdapterComponent);
        intent.putExtra("android.intent.extra.client_label", 17041223);
        intent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivityAsUser(context, 0, new Intent("android.settings.SYNC_SETTINGS"), 0, null, UserHandle.of(userId)));
        return intent;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, boolean dumpAll) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        dumpSyncState(ipw, new SyncAdapterStateFetcher());
        this.mConstants.dump(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        dumpSyncAdapters(ipw);
        if (dumpAll) {
            ipw.println("Detailed Sync History");
            this.mLogger.dumpAll(pw);
        }
    }

    static String formatTime(long time) {
        if (time == 0) {
            return "N/A";
        }
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    static /* synthetic */ int lambda$static$6(SyncOperation op1, SyncOperation op2) {
        int res = Integer.compare(op1.target.userId, op2.target.userId);
        if (res != 0) {
            return res;
        }
        Comparator<String> stringComparator = String.CASE_INSENSITIVE_ORDER;
        res = stringComparator.compare(op1.target.account.type, op2.target.account.type);
        if (res != 0) {
            return res;
        }
        res = stringComparator.compare(op1.target.account.name, op2.target.account.name);
        if (res != 0) {
            return res;
        }
        res = stringComparator.compare(op1.target.provider, op2.target.provider);
        if (res != 0) {
            return res;
        }
        res = Integer.compare(op1.reason, op2.reason);
        if (res != 0) {
            return res;
        }
        res = Long.compare(op1.periodMillis, op2.periodMillis);
        if (res != 0) {
            return res;
        }
        res = Long.compare(op1.expectedRuntime, op2.expectedRuntime);
        if (res != 0) {
            return res;
        }
        res = Long.compare((long) op1.jobId, (long) op2.jobId);
        if (res != 0) {
            return res;
        }
        return 0;
    }

    static /* synthetic */ int lambda$static$7(SyncOperation op1, SyncOperation op2) {
        int res = Long.compare(op1.expectedRuntime, op2.expectedRuntime);
        if (res != 0) {
            return res;
        }
        return sOpDumpComparator.compare(op1, op2);
    }

    private static <T> int countIf(Collection<T> col, Predicate<T> p) {
        int ret = 0;
        for (T item : col) {
            if (p.test(item)) {
                ret++;
            }
        }
        return ret;
    }

    protected void dumpPendingSyncs(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        pw.print("Pending Syncs: ");
        pw.println(countIf(pendingSyncs, -$$Lambda$SyncManager$rDUHWai3SU0BXk1TE0bLDap9gVc.INSTANCE));
        Collections.sort(pendingSyncs, sOpRuntimeComparator);
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (!op.isPeriodic) {
                pw.println(op.dump(null, false, buckets));
                count++;
            }
        }
        pw.println();
    }

    protected void dumpPeriodicSyncs(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        List<SyncOperation> pendingSyncs = getAllPendingSyncs();
        pw.print("Periodic Syncs: ");
        pw.println(countIf(pendingSyncs, -$$Lambda$SyncManager$ag0YGuZ1oL06fytmNlyErbNyYcw.INSTANCE));
        Collections.sort(pendingSyncs, sOpDumpComparator);
        int count = 0;
        for (SyncOperation op : pendingSyncs) {
            if (op.isPeriodic) {
                pw.println(op.dump(null, false, buckets));
                count++;
            }
        }
        pw.println();
    }

    public static StringBuilder formatDurationHMS(StringBuilder sb, long duration) {
        duration /= 1000;
        if (duration < 0) {
            sb.append('-');
            duration = -duration;
        }
        long seconds = duration % 60;
        duration /= 60;
        long minutes = duration % 60;
        duration /= 60;
        long hours = duration % 24;
        long days = duration / 24;
        boolean print = false;
        if (days > 0) {
            sb.append(days);
            sb.append('d');
            print = true;
        }
        if (!printTwoDigitNumber(sb, seconds, 's', printTwoDigitNumber(sb, minutes, 'm', printTwoDigitNumber(sb, hours, true, print)))) {
            sb.append("0s");
        }
        return sb;
    }

    private static boolean printTwoDigitNumber(StringBuilder sb, long value, char unit, boolean always) {
        if (!always && value == 0) {
            return false;
        }
        if (always && value < 10) {
            sb.append('0');
        }
        sb.append(value);
        sb.append(unit);
        return true;
    }

    protected void dumpSyncState(PrintWriter pw, SyncAdapterStateFetcher buckets) {
        List<UserInfo> users;
        AccountAndUser[] accounts;
        PackageManager pm;
        ArrayList<Pair<EndPoint, SyncStatusInfo>> statuses;
        int row1;
        PrintWriter printWriter = pw;
        StringBuilder sb = new StringBuilder();
        printWriter.print("Data connected: ");
        printWriter.println(this.mDataConnectionIsConnected);
        printWriter.print("Battery saver: ");
        int i = 0;
        boolean z = this.mPowerManager != null && this.mPowerManager.isPowerSaveMode();
        printWriter.println(z);
        printWriter.print("Background network restriction: ");
        ConnectivityManager cm = getConnectivityManager();
        int status = cm == null ? -1 : cm.getRestrictBackgroundStatus();
        switch (status) {
            case 1:
                printWriter.println(" disabled");
                break;
            case 2:
                printWriter.println(" whitelisted");
                break;
            case 3:
                printWriter.println(" enabled");
                break;
            default:
                printWriter.print("Unknown(");
                printWriter.print(status);
                printWriter.println(")");
                break;
        }
        printWriter.print("Auto sync: ");
        List<UserInfo> users2 = getAllUsers();
        if (users2 != null) {
            for (UserInfo user : users2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("u");
                stringBuilder.append(user.id);
                stringBuilder.append("=");
                stringBuilder.append(this.mSyncStorageEngine.getMasterSyncAutomatically(user.id));
                stringBuilder.append(" ");
                printWriter.print(stringBuilder.toString());
            }
            pw.println();
        }
        printWriter.print("Memory low: ");
        printWriter.println(this.mStorageIsLow);
        printWriter.print("Device idle: ");
        printWriter.println(this.mDeviceIsIdle);
        printWriter.print("Reported active: ");
        printWriter.println(this.mReportedSyncActive);
        printWriter.print("Clock valid: ");
        printWriter.println(this.mSyncStorageEngine.isClockValid());
        AccountAndUser[] accounts2 = AccountManagerService.getSingleton().getAllAccounts();
        printWriter.print("Accounts: ");
        if (accounts2 != INITIAL_ACCOUNTS_ARRAY) {
            printWriter.println(accounts2.length);
        } else {
            printWriter.println("not known yet");
        }
        long now = SystemClock.elapsedRealtime();
        printWriter.print("Now: ");
        printWriter.print(now);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" (");
        stringBuilder2.append(formatTime(System.currentTimeMillis()));
        stringBuilder2.append(")");
        printWriter.println(stringBuilder2.toString());
        sb.setLength(0);
        printWriter.print("Uptime: ");
        printWriter.print(formatDurationHMS(sb, now));
        pw.println();
        printWriter.print("Time spent syncing: ");
        sb.setLength(0);
        printWriter.print(formatDurationHMS(sb, this.mSyncHandler.mSyncTimeTracker.timeSpentSyncing()));
        printWriter.print(", sync ");
        printWriter.print(this.mSyncHandler.mSyncTimeTracker.mLastWasSyncing ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : "not ");
        printWriter.println("in progress");
        pw.println();
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Active Syncs: ");
        stringBuilder2.append(this.mActiveSyncContexts.size());
        printWriter.println(stringBuilder2.toString());
        PackageManager pm2 = this.mContext.getPackageManager();
        Iterator it = this.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            ActiveSyncContext activeSyncContext = (ActiveSyncContext) it.next();
            long durationInSeconds = now - activeSyncContext.mStartTime;
            printWriter.print("  ");
            sb.setLength(0);
            printWriter.print(formatDurationHMS(sb, durationInSeconds));
            printWriter.print(" - ");
            printWriter.print(activeSyncContext.mSyncOperation.dump(pm2, false, buckets));
            pw.println();
        }
        SyncAdapterStateFetcher syncAdapterStateFetcher = buckets;
        pw.println();
        dumpPendingSyncs(pw, buckets);
        dumpPeriodicSyncs(pw, buckets);
        printWriter.println("Sync Status");
        ArrayList<Pair<EndPoint, SyncStatusInfo>> statuses2 = new ArrayList();
        this.mSyncStorageEngine.resetTodayStats(false);
        int length = accounts2.length;
        int i2 = 0;
        while (i2 < length) {
            List<ServiceInfo<SyncAdapterType>> sorted;
            int i3;
            AccountAndUser account;
            int i4;
            AccountAndUser account2 = accounts2[i2];
            users = users2;
            printWriter.printf("Account %s u%d %s\n", new Object[]{"XXXXXXXXX", Integer.valueOf(account2.userId), account2.account.type});
            printWriter.println("=======================================================================");
            PrintTable table = new PrintTable(16);
            table.set(0, 0, "Authority", "Syncable", "Enabled", "Stats", "Loc", "Poll", "Per", "Feed", "User", "Othr", "Tot", "Fail", "Can", "Time", "Last Sync", "Backoff");
            List<ServiceInfo<SyncAdapterType>> sorted2 = Lists.newArrayList();
            sorted2.addAll(this.mSyncAdapters.getAllServices(account2.userId));
            Collections.sort(sorted2, new Comparator<ServiceInfo<SyncAdapterType>>() {
                public int compare(ServiceInfo<SyncAdapterType> lhs, ServiceInfo<SyncAdapterType> rhs) {
                    return ((SyncAdapterType) lhs.type).authority.compareTo(((SyncAdapterType) rhs.type).authority);
                }
            });
            Iterator it2 = sorted2.iterator();
            while (it2.hasNext()) {
                ServiceInfo<SyncAdapterType> syncAdapterType = (ServiceInfo) it2.next();
                Iterator it3 = it2;
                sorted = sorted2;
                if (((SyncAdapterType) syncAdapterType.type).accountType.equals(account2.account.type)) {
                    i = table.getNumRows();
                    accounts = accounts2;
                    pm = pm2;
                    i3 = length;
                    Pair<AuthorityInfo, SyncStatusInfo> syncAuthoritySyncStatus = this.mSyncStorageEngine.getCopyOfAuthorityWithSyncStatus(new EndPoint(account2.account, ((SyncAdapterType) syncAdapterType.type).authority, account2.userId));
                    AuthorityInfo settings = syncAuthoritySyncStatus.first;
                    SyncStatusInfo status2 = syncAuthoritySyncStatus.second;
                    statuses2.add(Pair.create(settings.target, status2));
                    String authority = settings.target.provider;
                    if (authority.length() > 50) {
                        authority = authority.substring(authority.length() - 50);
                    }
                    table.set(i, 0, authority, Integer.valueOf(settings.syncable), Boolean.valueOf(settings.enabled));
                    QuadConsumer<String, Stats, Function<Integer, String>, Integer> c = new -$$Lambda$SyncManager$9EoLpTk5JrHZn9R-uS0lqCVrpRw(sb, table);
                    StringBuilder sb2 = sb;
                    account = account2;
                    c.accept("Total", status2.totalStats, -$$Lambda$SyncManager$pdoEVnuSkmOrvULQ9M7Ic-lU5vw.INSTANCE, Integer.valueOf(i));
                    c.accept("Today", status2.todayStats, new -$$Lambda$SyncManager$EMXCZP9LDjgUTYbLsEoVu9Ccntw(this), Integer.valueOf(i + 1));
                    c.accept("Yestr", status2.yesterdayStats, new -$$Lambda$SyncManager$EMXCZP9LDjgUTYbLsEoVu9Ccntw(this), Integer.valueOf(i + 2));
                    int row12 = i;
                    int LAST_SYNC;
                    int BACKOFF;
                    if (settings.delayUntil > now) {
                        int row13 = row12 + 1;
                        LAST_SYNC = 14;
                        sb = new Object[1];
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("D: ");
                        statuses = statuses2;
                        BACKOFF = 15;
                        stringBuilder3.append((settings.delayUntil - now) / 1000);
                        sb[0] = stringBuilder3.toString();
                        table.set(row12, 15, sb);
                        if (settings.backoffTime > now) {
                            sb = row13 + 1;
                            Object[] objArr = new Object[1];
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("B: ");
                            i4 = i2;
                            stringBuilder4.append((settings.backoffTime - now) / 1000);
                            objArr[0] = stringBuilder4.toString();
                            table.set(row13, 15, objArr);
                            row12 = sb + 1;
                            table.set(sb, 15, Long.valueOf(settings.backoffDelay / 1000));
                        } else {
                            i4 = i2;
                            row12 = row13;
                        }
                    } else {
                        LAST_SYNC = 14;
                        QuadConsumer<String, Stats, Function<Integer, String>, Integer> quadConsumer = c;
                        statuses = statuses2;
                        BACKOFF = 15;
                        i4 = i2;
                    }
                    sb = i;
                    if (status2.lastSuccessTime != 0) {
                        int row14 = sb + 1;
                        Object[] objArr2 = new Object[1];
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append(SyncStorageEngine.SOURCES[status2.lastSuccessSource]);
                        stringBuilder5.append(" SUCCESS");
                        objArr2[0] = stringBuilder5.toString();
                        table.set(sb, 14, objArr2);
                        sb = row14 + 1;
                        table.set(row14, 14, formatTime(status2.lastSuccessTime));
                    }
                    if (status2.lastFailureTime != 0) {
                        row1 = sb + 1;
                        Object[] objArr3 = new Object[1];
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append(SyncStorageEngine.SOURCES[status2.lastFailureSource]);
                        stringBuilder6.append(" FAILURE");
                        objArr3[0] = stringBuilder6.toString();
                        table.set(sb, 14, objArr3);
                        sb = row1 + 1;
                        table.set(row1, 14, formatTime(status2.lastFailureTime));
                        row1 = sb + 1;
                        table.set(sb, 14, status2.lastFailureMesg);
                    }
                    it2 = it3;
                    sorted2 = sorted;
                    accounts2 = accounts;
                    pm2 = pm;
                    length = i3;
                    sb = sb2;
                    account2 = account;
                    statuses2 = statuses;
                    i2 = i4;
                    syncAdapterStateFetcher = buckets;
                } else {
                    it2 = it3;
                    sorted2 = sorted;
                }
            }
            sorted = sorted2;
            accounts = accounts2;
            pm = pm2;
            statuses = statuses2;
            i3 = length;
            i4 = i2;
            account = account2;
            table.writeTo(printWriter);
            i2 = i4 + 1;
            users2 = users;
            i = 0;
            syncAdapterStateFetcher = buckets;
        }
        users = users2;
        accounts = accounts2;
        pm = pm2;
        statuses = statuses2;
        dumpSyncHistory(pw);
        pw.println();
        printWriter.println("Per Adapter History");
        printWriter.println("(SERVER is now split up to FEED and OTHER)");
        int i5 = 0;
        while (true) {
            ArrayList<Pair<EndPoint, SyncStatusInfo>> statuses3 = statuses;
            if (i5 < statuses3.size()) {
                Pair<EndPoint, SyncStatusInfo> event = (Pair) statuses3.get(i5);
                printWriter.print("  ");
                printWriter.print("XXXXXXXXX");
                printWriter.print('/');
                printWriter.print(((EndPoint) event.first).account.type);
                printWriter.print(" u");
                printWriter.print(((EndPoint) event.first).userId);
                printWriter.print(" [");
                printWriter.print(((EndPoint) event.first).provider);
                printWriter.print("]");
                pw.println();
                printWriter.println("    Per source last syncs:");
                for (row1 = 0; row1 < SyncStorageEngine.SOURCES.length; row1++) {
                    printWriter.print("      ");
                    printWriter.print(String.format("%8s", new Object[]{SyncStorageEngine.SOURCES[row1]}));
                    printWriter.print("  Success: ");
                    printWriter.print(formatTime(((SyncStatusInfo) event.second).perSourceLastSuccessTimes[row1]));
                    printWriter.print("  Failure: ");
                    printWriter.println(formatTime(((SyncStatusInfo) event.second).perSourceLastFailureTimes[row1]));
                }
                printWriter.println("    Last syncs:");
                for (row1 = 0; row1 < ((SyncStatusInfo) event.second).getEventCount(); row1++) {
                    printWriter.print("      ");
                    printWriter.print(formatTime(((SyncStatusInfo) event.second).getEventTime(row1)));
                    printWriter.print(' ');
                    printWriter.print(((SyncStatusInfo) event.second).getEvent(row1));
                    pw.println();
                }
                if (((SyncStatusInfo) event.second).getEventCount() == 0) {
                    printWriter.println("      N/A");
                }
                i5++;
                statuses = statuses3;
            } else {
                return;
            }
        }
    }

    static /* synthetic */ void lambda$dumpSyncState$10(StringBuilder sb, PrintTable table, String label, Stats stats, Function filter, Integer r) {
        sb.setLength(0);
        table.set(r.intValue(), 3, label, filter.apply(Integer.valueOf(stats.numSourceLocal)), filter.apply(Integer.valueOf(stats.numSourcePoll)), filter.apply(Integer.valueOf(stats.numSourcePeriodic)), filter.apply(Integer.valueOf(stats.numSourceFeed)), filter.apply(Integer.valueOf(stats.numSourceUser)), filter.apply(Integer.valueOf(stats.numSourceOther)), filter.apply(Integer.valueOf(stats.numSyncs)), filter.apply(Integer.valueOf(stats.numFailures)), filter.apply(Integer.valueOf(stats.numCancels)), formatDurationHMS(sb, stats.totalElapsedTime));
    }

    private String zeroToEmpty(int value) {
        return value != 0 ? Integer.toString(value) : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private void dumpTimeSec(PrintWriter pw, long time) {
        pw.print(time / 1000);
        pw.print('.');
        pw.print((time / 100) % 10);
        pw.print('s');
    }

    private void dumpDayStatistic(PrintWriter pw, DayStats ds) {
        pw.print("Success (");
        pw.print(ds.successCount);
        if (ds.successCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.successTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.successTime / ((long) ds.successCount));
        }
        pw.print(") Failure (");
        pw.print(ds.failureCount);
        if (ds.failureCount > 0) {
            pw.print(" for ");
            dumpTimeSec(pw, ds.failureTime);
            pw.print(" avg=");
            dumpTimeSec(pw, ds.failureTime / ((long) ds.failureCount));
        }
        pw.println(")");
    }

    protected void dumpSyncHistory(PrintWriter pw) {
        dumpRecentHistory(pw);
        dumpDayStatistics(pw);
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x044a  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0438  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0438  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x044a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dumpRecentHistory(PrintWriter pw) {
        String separator = this;
        PrintWriter printWriter = pw;
        ArrayList<SyncHistoryItem> items = separator.mSyncStorageEngine.getSyncHistory();
        String str;
        PrintWriter printWriter2;
        ArrayList<SyncHistoryItem> arrayList;
        if (items == null || items.size() <= 0) {
            str = separator;
            printWriter2 = printWriter;
            arrayList = items;
            return;
        }
        int length;
        long elapsedTime;
        ArrayList<SyncHistoryItem> items2;
        int N;
        int maxAuthority;
        int maxAccount;
        Object[] objArr;
        PrintWriter printWriter3;
        SyncManager thisR;
        int N2;
        long totalTimes;
        int maxAuthority2;
        int maxAccount2;
        long eventTime;
        int N3;
        ArrayList<SyncHistoryItem> items3;
        String format;
        Map<String, AuthoritySyncStats> authorityMap = Maps.newHashMap();
        long totalElapsedTime = 0;
        long totalTimes2 = 0;
        int N4 = items.size();
        int maxAuthority3 = 0;
        int maxAccount3 = 0;
        String accountKey = items.iterator();
        while (accountKey.hasNext()) {
            String authorityName;
            String str2;
            AuthoritySyncStats authoritySyncStats;
            SyncHistoryItem item = (SyncHistoryItem) accountKey.next();
            AuthorityInfo authorityInfo = separator.mSyncStorageEngine.getAuthority(item.authorityId);
            if (authorityInfo != null) {
                authorityName = authorityInfo.target.provider;
                StringBuilder stringBuilder = new StringBuilder();
                str2 = accountKey;
                stringBuilder.append(authorityInfo.target.account.name);
                stringBuilder.append(SliceAuthority.DELIMITER);
                stringBuilder.append(authorityInfo.target.account.type);
                stringBuilder.append(" u");
                stringBuilder.append(authorityInfo.target.userId);
                accountKey = stringBuilder.toString();
            } else {
                str2 = accountKey;
                authorityName = "Unknown";
                accountKey = "Unknown";
            }
            length = authorityName.length();
            if (length > maxAuthority3) {
                maxAuthority3 = length;
            }
            length = accountKey.length();
            if (length > maxAccount3) {
                maxAccount3 = length;
            }
            int maxAuthority4 = maxAuthority3;
            int maxAccount4 = maxAccount3;
            elapsedTime = item.elapsedTime;
            totalTimes2++;
            AuthoritySyncStats authoritySyncStats2 = (AuthoritySyncStats) authorityMap.get(authorityName);
            long totalElapsedTime2 = totalElapsedTime + elapsedTime;
            if (authoritySyncStats2 == null) {
                authoritySyncStats = new AuthoritySyncStats(authorityName, null);
                authorityMap.put(authorityName, authoritySyncStats);
            } else {
                authoritySyncStats = authoritySyncStats2;
            }
            long totalTimes3 = totalTimes2;
            authoritySyncStats.elapsedTime += elapsedTime;
            authoritySyncStats.times++;
            Map<String, AccountSyncStats> accountMap = authoritySyncStats.accountMap;
            AccountSyncStats accountSyncStats = (AccountSyncStats) accountMap.get(accountKey);
            if (accountSyncStats == null) {
                accountSyncStats = new AccountSyncStats(accountKey, null);
                accountMap.put(accountKey, accountSyncStats);
            }
            accountSyncStats.elapsedTime += elapsedTime;
            accountSyncStats.times++;
            accountKey = str2;
            maxAuthority3 = maxAuthority4;
            maxAccount3 = maxAccount4;
            totalElapsedTime = totalElapsedTime2;
            totalTimes2 = totalTimes3;
        }
        long totalElapsedTime3;
        if (totalElapsedTime > 0) {
            pw.println();
            printWriter.printf("Detailed Statistics (Recent history):  %d (# of times) %ds (sync time)\n", new Object[]{Long.valueOf(totalTimes2), Long.valueOf(totalElapsedTime / 1000)});
            List<AuthoritySyncStats> sortedAuthorities = new ArrayList(authorityMap.values());
            Collections.sort(sortedAuthorities, new Comparator<AuthoritySyncStats>() {
                public int compare(AuthoritySyncStats lhs, AuthoritySyncStats rhs) {
                    int compare = Integer.compare(rhs.times, lhs.times);
                    if (compare == 0) {
                        return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                    }
                    return compare;
                }
            });
            int maxLength = Math.max(maxAuthority3, maxAccount3 + 3);
            length = (((4 + maxLength) + 2) + 10) + 11;
            char[] chars = new char[length];
            Arrays.fill(chars, '-');
            String separator2 = new String(chars);
            String authorityFormat = String.format("  %%-%ds: %%-9s  %%-11s\n", new Object[]{Integer.valueOf(maxLength + 2)});
            items2 = items;
            items = String.format("    %%-%ds:   %%-9s  %%-11s\n", new Object[]{Integer.valueOf(maxLength)});
            printWriter.println(separator2);
            Iterator it = sortedAuthorities.iterator();
            while (it.hasNext()) {
                String authorityFormat2;
                List<AccountSyncStats> sortedAccounts;
                String name;
                AuthoritySyncStats authoritySyncStats3 = (AuthoritySyncStats) it.next();
                List<AuthoritySyncStats> sortedAuthorities2 = sortedAuthorities;
                accountKey = authoritySyncStats3.name;
                int maxLength2 = maxLength;
                Iterator it2 = it;
                long elapsedTime2 = authoritySyncStats3.elapsedTime;
                N = N4;
                N4 = authoritySyncStats3.times;
                maxAuthority = maxAuthority3;
                maxAccount = maxAccount3;
                String separator3 = separator2;
                separator = String.format("%ds/%d%%", new Object[]{Long.valueOf(elapsedTime2 / 1000), Long.valueOf((elapsedTime2 * 100) / totalElapsedTime)});
                objArr = new Object[2];
                objArr[0] = Integer.valueOf(N4);
                objArr[1] = Long.valueOf(((long) (N4 * 100)) / totalTimes2);
                String timesStr = String.format("%d/%d%%", objArr);
                printWriter3 = pw;
                printWriter3.printf(authorityFormat, new Object[]{accountKey, timesStr, separator});
                List<AccountSyncStats> sortedAccounts2 = new ArrayList(authoritySyncStats3.accountMap.values());
                Collections.sort(sortedAccounts2, new Comparator<AccountSyncStats>() {
                    public int compare(AccountSyncStats lhs, AccountSyncStats rhs) {
                        int compare = Integer.compare(rhs.times, lhs.times);
                        if (compare == 0) {
                            return Long.compare(rhs.elapsedTime, lhs.elapsedTime);
                        }
                        return compare;
                    }
                });
                Iterator it3 = sortedAccounts2.iterator();
                while (it3.hasNext()) {
                    AccountSyncStats stats = (AccountSyncStats) it3.next();
                    elapsedTime2 = stats.elapsedTime;
                    int times = stats.times;
                    authorityFormat2 = authorityFormat;
                    sortedAccounts = sortedAccounts2;
                    Object[] objArr2 = new Object[2];
                    Iterator it4 = it3;
                    name = accountKey;
                    objArr2[0] = Long.valueOf(elapsedTime2 / 1000);
                    objArr2[1] = Long.valueOf((elapsedTime2 * 100) / totalElapsedTime);
                    separator = String.format("%ds/%d%%", objArr2);
                    objArr = new Object[2];
                    objArr[0] = Integer.valueOf(times);
                    totalElapsedTime3 = totalElapsedTime;
                    objArr[1] = Long.valueOf(((long) (times * 100)) / totalTimes2);
                    printWriter3.printf(items, new Object[]{"XXXXXXXXX", String.format("%d/%d%%", objArr), separator});
                    int i = times;
                    Object timesStr2 = authorityMap;
                    authorityFormat = authorityFormat2;
                    sortedAccounts2 = sortedAccounts;
                    accountKey = name;
                    it3 = it4;
                    totalElapsedTime = totalElapsedTime3;
                }
                String str3 = separator;
                String str4 = timesStr2;
                authorityFormat2 = authorityFormat;
                totalElapsedTime3 = totalElapsedTime;
                sortedAccounts = sortedAccounts2;
                name = accountKey;
                separator = separator3;
                printWriter3.println(separator);
                printWriter = printWriter3;
                sortedAuthorities = sortedAuthorities2;
                maxLength = maxLength2;
                it = it2;
                N4 = N;
                maxAuthority3 = maxAuthority;
                maxAccount3 = maxAccount;
                separator2 = separator;
                Object separator4 = this;
            }
            thisR = separator4;
            totalElapsedTime3 = totalElapsedTime;
            N = N4;
            maxAuthority = maxAuthority3;
            maxAccount = maxAccount3;
            printWriter3 = printWriter;
        } else {
            thisR = separator4;
            items2 = items;
            Map<String, AuthoritySyncStats> map = authorityMap;
            totalElapsedTime3 = totalElapsedTime;
            N = N4;
            maxAuthority = maxAuthority3;
            maxAccount = maxAccount3;
            printWriter3 = printWriter;
        }
        pw.println();
        printWriter3.println("Recent Sync History");
        printWriter3.println("(SERVER is now split up to FEED and OTHER)");
        separator4 = new StringBuilder();
        separator4.append("  %-");
        maxAccount3 = maxAccount;
        separator4.append(maxAccount3);
        separator4.append("s  %-");
        maxAuthority3 = maxAuthority;
        separator4.append(maxAuthority3);
        separator4.append("s %s\n");
        separator4 = separator4.toString();
        Map<String, Long> lastTimeMap = Maps.newHashMap();
        PackageManager pm = thisR.mContext.getPackageManager();
        int i2 = 0;
        while (true) {
            N2 = N;
            if (i2 >= N2) {
                break;
            }
            String authorityName2;
            String accountKey2;
            String format2;
            long elapsedTime3;
            String authorityName3;
            Object[] objArr3;
            ArrayList<SyncHistoryItem> items4 = items2;
            SyncHistoryItem item2 = (SyncHistoryItem) items4.get(i2);
            AuthorityInfo authorityInfo2 = thisR.mSyncStorageEngine.getAuthority(item2.authorityId);
            if (authorityInfo2 != null) {
                authorityName2 = authorityInfo2.target.provider;
                StringBuilder stringBuilder2 = new StringBuilder();
                totalTimes = totalTimes2;
                stringBuilder2.append(authorityInfo2.target.account.name);
                stringBuilder2.append(SliceAuthority.DELIMITER);
                stringBuilder2.append(authorityInfo2.target.account.type);
                stringBuilder2.append(" u");
                stringBuilder2.append(authorityInfo2.target.userId);
                accountKey2 = stringBuilder2.toString();
            } else {
                totalTimes = totalTimes2;
                authorityName2 = "Unknown";
                accountKey2 = "Unknown";
            }
            maxAuthority2 = maxAuthority3;
            maxAccount2 = maxAccount3;
            elapsedTime = item2.elapsedTime;
            Time time = new Time();
            eventTime = item2.eventTime;
            time.set(eventTime);
            str = new StringBuilder();
            str.append(authorityName2);
            str.append(SliceAuthority.DELIMITER);
            str.append(accountKey2);
            str = str.toString();
            Long lastEventTime = (Long) lastTimeMap.get(str);
            String diffString;
            if (lastEventTime == null) {
                diffString = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                format2 = separator4;
                N3 = N2;
                items3 = items4;
            } else {
                items3 = items4;
                String str5 = accountKey2;
                long diff = (lastEventTime.longValue() - eventTime) / 1000;
                Long l;
                if (diff < 60) {
                    diffString = String.valueOf(diff);
                    format2 = separator4;
                    N3 = N2;
                } else if (diff < 3600) {
                    l = lastEventTime;
                    format2 = separator4;
                    N3 = N2;
                    r0 = new Object[2];
                    elapsedTime3 = elapsedTime;
                    r0[0] = Long.valueOf(diff / 60);
                    r0[1] = Long.valueOf(diff % 60);
                    diffString = String.format("%02d:%02d", r0);
                    authorityName3 = authorityName2;
                    lastTimeMap.put(str, Long.valueOf(eventTime));
                    printWriter2 = pw;
                    printWriter2.printf("  #%-3d: %s %8s  %5.1fs  %8s", new Object[]{Integer.valueOf(i2 + 1), formatTime(eventTime), SyncStorageEngine.SOURCES[item2.source], Float.valueOf(((float) elapsedTime3) / 1000.0f), separator4});
                    format = format2;
                    printWriter2.printf(format, new Object[]{"XXXXXXXXX", authorityName3, SyncOperation.reasonToString(pm, item2.reason)});
                    if (item2.event == 1) {
                        if (item2.upstreamActivity == 0 && item2.downstreamActivity == 0) {
                            String str6 = str;
                            if (!(item2.mesg == null || SyncStorageEngine.MESG_SUCCESS.equals(item2.mesg))) {
                                printWriter2.printf("    mesg=%s\n", new Object[]{item2.mesg});
                            }
                            i2++;
                            separator4 = format;
                            printWriter3 = printWriter2;
                            totalTimes2 = totalTimes;
                            maxAccount3 = maxAccount2;
                            maxAuthority3 = maxAuthority2;
                            items2 = items3;
                            N = N3;
                            thisR = this;
                        }
                    }
                    objArr3 = new Object[3];
                    objArr3[1] = Long.valueOf(item2.upstreamActivity);
                    objArr3[2] = Long.valueOf(item2.downstreamActivity);
                    printWriter2.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", objArr3);
                    printWriter2.printf("    mesg=%s\n", new Object[]{item2.mesg});
                    i2++;
                    separator4 = format;
                    printWriter3 = printWriter2;
                    totalTimes2 = totalTimes;
                    maxAccount3 = maxAccount2;
                    maxAuthority3 = maxAuthority2;
                    items2 = items3;
                    N = N3;
                    thisR = this;
                } else {
                    format2 = separator4;
                    N3 = N2;
                    elapsedTime3 = elapsedTime;
                    l = lastEventTime;
                    long sec = diff % 3600;
                    objArr = new Object[3];
                    authorityName3 = authorityName2;
                    objArr[0] = Long.valueOf(diff / 3600);
                    objArr[1] = Long.valueOf(sec / 60);
                    objArr[2] = Long.valueOf(sec % 60);
                    diffString = String.format("%02d:%02d:%02d", objArr);
                    lastTimeMap.put(str, Long.valueOf(eventTime));
                    printWriter2 = pw;
                    printWriter2.printf("  #%-3d: %s %8s  %5.1fs  %8s", new Object[]{Integer.valueOf(i2 + 1), formatTime(eventTime), SyncStorageEngine.SOURCES[item2.source], Float.valueOf(((float) elapsedTime3) / 1000.0f), separator4});
                    format = format2;
                    printWriter2.printf(format, new Object[]{"XXXXXXXXX", authorityName3, SyncOperation.reasonToString(pm, item2.reason)});
                    if (item2.event == 1) {
                    }
                    objArr3 = new Object[3];
                    objArr3[1] = Long.valueOf(item2.upstreamActivity);
                    objArr3[2] = Long.valueOf(item2.downstreamActivity);
                    printWriter2.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", objArr3);
                    printWriter2.printf("    mesg=%s\n", new Object[]{item2.mesg});
                    i2++;
                    separator4 = format;
                    printWriter3 = printWriter2;
                    totalTimes2 = totalTimes;
                    maxAccount3 = maxAccount2;
                    maxAuthority3 = maxAuthority2;
                    items2 = items3;
                    N = N3;
                    thisR = this;
                }
            }
            elapsedTime3 = elapsedTime;
            authorityName3 = authorityName2;
            lastTimeMap.put(str, Long.valueOf(eventTime));
            printWriter2 = pw;
            printWriter2.printf("  #%-3d: %s %8s  %5.1fs  %8s", new Object[]{Integer.valueOf(i2 + 1), formatTime(eventTime), SyncStorageEngine.SOURCES[item2.source], Float.valueOf(((float) elapsedTime3) / 1000.0f), separator4});
            format = format2;
            printWriter2.printf(format, new Object[]{"XXXXXXXXX", authorityName3, SyncOperation.reasonToString(pm, item2.reason)});
            if (item2.event == 1) {
            }
            objArr3 = new Object[3];
            objArr3[1] = Long.valueOf(item2.upstreamActivity);
            objArr3[2] = Long.valueOf(item2.downstreamActivity);
            printWriter2.printf("    event=%d upstreamActivity=%d downstreamActivity=%d\n", objArr3);
            printWriter2.printf("    mesg=%s\n", new Object[]{item2.mesg});
            i2++;
            separator4 = format;
            printWriter3 = printWriter2;
            totalTimes2 = totalTimes;
            maxAccount3 = maxAccount2;
            maxAuthority3 = maxAuthority2;
            items2 = items3;
            N = N3;
            thisR = this;
        }
        N3 = N2;
        totalTimes = totalTimes2;
        printWriter2 = printWriter3;
        maxAuthority2 = maxAuthority3;
        maxAccount2 = maxAccount3;
        items3 = items2;
        format = separator4;
        pw.println();
        printWriter2.println("Recent Sync History Extras");
        printWriter2.println("(SERVER is now split up to FEED and OTHER)");
        int i3 = 0;
        while (true) {
            i2 = N3;
            if (i3 < i2) {
                Map<String, Long> lastTimeMap2;
                PackageManager pm2;
                int N5;
                ArrayList<SyncHistoryItem> items5 = items3;
                SyncHistoryItem item3 = (SyncHistoryItem) items5.get(i3);
                Bundle extras = item3.extras;
                if (extras == null) {
                    lastTimeMap2 = lastTimeMap;
                    pm2 = pm;
                    N5 = i2;
                    arrayList = items5;
                } else if (extras.size() == 0) {
                    lastTimeMap2 = lastTimeMap;
                    pm2 = pm;
                    N5 = i2;
                    arrayList = items5;
                } else {
                    String authorityName4;
                    AuthorityInfo authorityInfo3 = this.mSyncStorageEngine.getAuthority(item3.authorityId);
                    if (authorityInfo3 != null) {
                        authorityName4 = authorityInfo3.target.provider;
                        accountKey = new StringBuilder();
                        accountKey.append(authorityInfo3.target.account.name);
                        accountKey.append(SliceAuthority.DELIMITER);
                        accountKey.append(authorityInfo3.target.account.type);
                        accountKey.append(" u");
                        accountKey.append(authorityInfo3.target.userId);
                        accountKey = accountKey.toString();
                    } else {
                        authorityName4 = "Unknown";
                        accountKey = "Unknown";
                    }
                    Time time2 = new Time();
                    eventTime = item3.eventTime;
                    time2.set(eventTime);
                    lastTimeMap2 = lastTimeMap;
                    pm2 = pm;
                    N5 = i2;
                    Object[] objArr4 = new Object[3];
                    objArr4[0] = Integer.valueOf(i3 + 1);
                    objArr4[1] = formatTime(eventTime);
                    arrayList = items5;
                    objArr4[2] = SyncStorageEngine.SOURCES[item3.source];
                    printWriter2.printf("  #%-3d: %s %8s ", objArr4);
                    printWriter2.printf(format, new Object[]{"XXXXXXXXX", authorityName4, extras});
                }
                i3++;
                lastTimeMap = lastTimeMap2;
                pm = pm2;
                N3 = N5;
                items3 = arrayList;
            } else {
                return;
            }
        }
    }

    private void dumpDayStatistics(PrintWriter pw) {
        DayStats[] dses = this.mSyncStorageEngine.getDayStatistics();
        if (dses != null && dses[0] != null) {
            pw.println();
            pw.println("Sync Statistics");
            pw.print("  Today:  ");
            dumpDayStatistic(pw, dses[0]);
            int today = dses[0].day;
            int i = 1;
            while (i <= 6 && i < dses.length) {
                DayStats ds = dses[i];
                if (ds == null) {
                    break;
                }
                int delta = today - ds.day;
                if (delta > 6) {
                    break;
                }
                pw.print("  Day-");
                pw.print(delta);
                pw.print(":  ");
                dumpDayStatistic(pw, ds);
                i++;
            }
            int i2 = i;
            i = today;
            while (i2 < dses.length) {
                DayStats aggr = null;
                i -= 7;
                while (i2 < dses.length) {
                    DayStats ds2 = dses[i2];
                    if (ds2 == null) {
                        i2 = dses.length;
                        break;
                    } else if (i - ds2.day > 6) {
                        break;
                    } else {
                        i2++;
                        if (aggr == null) {
                            aggr = new DayStats(i);
                        }
                        aggr.successCount += ds2.successCount;
                        aggr.successTime += ds2.successTime;
                        aggr.failureCount += ds2.failureCount;
                        aggr.failureTime += ds2.failureTime;
                    }
                }
                if (aggr != null) {
                    pw.print("  Week-");
                    pw.print((today - i) / 7);
                    pw.print(": ");
                    dumpDayStatistic(pw, aggr);
                }
            }
        }
    }

    private void dumpSyncAdapters(IndentingPrintWriter pw) {
        pw.println();
        List<UserInfo> users = getAllUsers();
        if (users != null) {
            for (UserInfo user : users) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sync adapters for ");
                stringBuilder.append(user);
                stringBuilder.append(":");
                pw.println(stringBuilder.toString());
                pw.increaseIndent();
                for (ServiceInfo<?> info : this.mSyncAdapters.getAllServices(user.id)) {
                    pw.println(info);
                }
                pw.decreaseIndent();
                pw.println();
            }
        }
    }

    static void sendOnUnsyncableAccount(Context context, ServiceInfo<SyncAdapterType> syncAdapterInfo, int userId, OnReadyCallback onReadyCallback) {
        OnUnsyncableAccountCheck connection = new OnUnsyncableAccountCheck(syncAdapterInfo, onReadyCallback);
        if (context.bindServiceAsUser(getAdapterBindIntent(context, syncAdapterInfo.componentName, userId), connection, 21, UserHandle.of(userId))) {
            new Handler(Looper.getMainLooper()).postDelayed(new -$$Lambda$SyncManager$zZUXjd-GLFQgHtMQ3vq0EWHvir8(context, connection), 5000);
        } else {
            connection.onReady();
        }
    }

    public static boolean readyToSync() {
        boolean z;
        synchronized (SyncManager.class) {
            z = sInstance != null && sInstance.mProvisioned && sInstance.mBootCompleted && sInstance.mJobServiceReady;
        }
        return z;
    }

    private boolean isSyncStillActiveH(ActiveSyncContext activeSyncContext) {
        Iterator it = this.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            if (((ActiveSyncContext) it.next()) == activeSyncContext) {
                return true;
            }
        }
        return false;
    }

    public static boolean syncExtrasEquals(Bundle b1, Bundle b2, boolean includeSyncSettings) {
        if (b1 == b2) {
            return true;
        }
        if (includeSyncSettings && b1.size() != b2.size()) {
            return false;
        }
        Bundle bigger = b1.size() > b2.size() ? b1 : b2;
        Bundle smaller = b1.size() > b2.size() ? b2 : b1;
        for (String key : bigger.keySet()) {
            if (includeSyncSettings || !isSyncSetting(key)) {
                if (!smaller.containsKey(key) || !Objects.equals(bigger.get(key), smaller.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSyncSetting(String key) {
        if (key.equals("expedited") || key.equals("ignore_settings") || key.equals("ignore_backoff") || key.equals("do_not_retry") || key.equals("force") || key.equals("upload") || key.equals("deletions_override") || key.equals("discard_deletions") || key.equals("expected_upload") || key.equals("expected_download") || key.equals("sync_priority") || key.equals("allow_metered") || key.equals("initialize")) {
            return true;
        }
        return false;
    }

    private Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (NameNotFoundException e) {
            return this.mContext;
        }
    }

    private void cancelJob(SyncOperation op, String why) {
        if (op == null) {
            Slog.wtf("SyncManager", "Null sync operation detected.");
            return;
        }
        if (op.isPeriodic) {
            this.mLogger.log("Removing periodic sync ", op, " for ", why);
        }
        getJobScheduler().cancel(op.jobId);
    }

    private void wtfWithLog(String message) {
        Slog.wtf("SyncManager", message);
        this.mLogger.log("WTF: ", message);
    }

    public void resetTodayStats() {
        this.mSyncStorageEngine.resetTodayStats(true);
    }
}

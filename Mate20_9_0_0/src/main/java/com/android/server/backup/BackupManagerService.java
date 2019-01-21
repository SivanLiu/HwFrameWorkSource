package com.android.server.backup;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.IBackupAgent;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.os.storage.IStorageManager.Stub;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.backup.fullbackup.FullBackupEntry;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.backup.internal.BackupRequest;
import com.android.server.backup.internal.ClearDataObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.internal.Operation;
import com.android.server.backup.internal.PerformInitializeTask;
import com.android.server.backup.internal.ProvisionedObserver;
import com.android.server.backup.internal.RunBackupReceiver;
import com.android.server.backup.internal.RunInitializeReceiver;
import com.android.server.backup.params.AdbBackupParams;
import com.android.server.backup.params.AdbParams;
import com.android.server.backup.params.AdbRestoreParams;
import com.android.server.backup.params.BackupParams;
import com.android.server.backup.params.ClearParams;
import com.android.server.backup.params.ClearRetryParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.restore.ActiveRestoreSession;
import com.android.server.backup.restore.PerformUnifiedRestoreTask;
import com.android.server.backup.transport.TransportClient;
import com.android.server.backup.transport.TransportNotRegisteredException;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.backup.utils.SparseArrayUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.google.android.collect.Sets;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupManagerService implements BackupManagerServiceInterface {
    private static final String BACKUP_ENABLE_FILE = "backup_enabled";
    public static final String BACKUP_FILE_HEADER_MAGIC = "ANDROID BACKUP\n";
    public static final int BACKUP_FILE_VERSION = 5;
    public static final String BACKUP_FINISHED_ACTION = "android.intent.action.BACKUP_FINISHED";
    public static final String BACKUP_FINISHED_PACKAGE_EXTRA = "packageName";
    public static final String BACKUP_MANIFEST_FILENAME = "_manifest";
    public static final int BACKUP_MANIFEST_VERSION = 1;
    public static final String BACKUP_METADATA_FILENAME = "_meta";
    public static final int BACKUP_METADATA_VERSION = 1;
    public static final int BACKUP_WIDGET_METADATA_TOKEN = 33549569;
    private static final int BUSY_BACKOFF_FUZZ = 7200000;
    private static final long BUSY_BACKOFF_MIN_MILLIS = 3600000;
    private static final boolean COMPRESS_FULL_BACKUPS = true;
    private static final int CURRENT_ANCESTRAL_RECORD_VERSION = 1;
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_BACKUP_TRACE = true;
    public static final boolean DEBUG_SCHEDULING = true;
    private static final long INITIALIZATION_DELAY_MILLIS = 3000;
    private static final String INIT_SENTINEL_FILE_NAME = "_need_init_";
    public static final String KEY_WIDGET_STATE = "￭￭widget";
    public static final boolean MORE_DEBUG = false;
    private static final int OP_ACKNOWLEDGED = 1;
    public static final int OP_PENDING = 0;
    private static final int OP_TIMEOUT = -1;
    public static final int OP_TYPE_BACKUP = 2;
    public static final int OP_TYPE_BACKUP_WAIT = 0;
    public static final int OP_TYPE_RESTORE_WAIT = 1;
    public static final String PACKAGE_MANAGER_SENTINEL = "@pm@";
    public static final String RUN_BACKUP_ACTION = "android.app.backup.intent.RUN";
    public static final String RUN_INITIALIZE_ACTION = "android.app.backup.intent.INIT";
    private static final int SCHEDULE_FILE_VERSION = 1;
    private static final String SERVICE_ACTION_TRANSPORT_HOST = "android.backup.TRANSPORT_HOST";
    public static final String SETTINGS_PACKAGE = "com.android.providers.settings";
    public static final String SHARED_BACKUP_AGENT_PACKAGE = "com.android.sharedstoragebackup";
    public static final String TAG = "BackupManagerService";
    private static final long TIMEOUT_FULL_CONFIRMATION = 60000;
    private static final long TIMEOUT_INTERVAL = 10000;
    private static final long TRANSPORT_RETRY_INTERVAL = 3600000;
    static Trampoline sInstance;
    private ActiveRestoreSession mActiveRestoreSession;
    private IActivityManager mActivityManager;
    private final SparseArray<AdbParams> mAdbBackupRestoreConfirmations = new SparseArray();
    private final Object mAgentConnectLock = new Object();
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private AlarmManager mAlarmManager;
    private Set<String> mAncestralPackages = null;
    private long mAncestralToken = 0;
    private boolean mAutoRestore;
    private BackupHandler mBackupHandler;
    private IBackupManager mBackupManagerBinder;
    private final SparseArray<HashSet<String>> mBackupParticipants = new SparseArray();
    private final BackupPasswordManager mBackupPasswordManager;
    private BackupPolicyEnforcer mBackupPolicyEnforcer;
    private volatile boolean mBackupRunning;
    private final List<String> mBackupTrace = new ArrayList();
    private File mBaseStateDir;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Missing block: B:40:0x00b5, code skipped:
            r11 = java.lang.System.currentTimeMillis();
            r13 = r7.length;
            r14 = 0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            boolean changed;
            String str;
            Throwable th;
            Intent intent2 = intent;
            String action = intent.getAction();
            boolean replacing = false;
            boolean replacing2 = false;
            boolean added = false;
            Bundle extras = intent.getExtras();
            String[] pkgList = null;
            int i = 0;
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                changed = intent.getData();
                if (changed) {
                    String pkgName = changed.getSchemeSpecificPart();
                    if (pkgName != null) {
                        pkgList = new String[]{pkgName};
                    }
                    added = "android.intent.action.PACKAGE_CHANGED".equals(action);
                    if (added) {
                        BackupManagerService.this.mBackupHandler.post(new -$$Lambda$BackupManagerService$2$k3_lOimiIJDhWdG7_SCrtoKbtjY(this, pkgName, intent2.getStringArrayExtra("android.intent.extra.changed_component_name_list")));
                        return;
                    }
                    replacing2 = "android.intent.action.PACKAGE_ADDED".equals(action);
                    replacing = extras.getBoolean("android.intent.extra.REPLACING", false);
                } else {
                    return;
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                replacing2 = true;
                pkgList = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                replacing2 = false;
                pkgList = intent2.getStringArrayExtra("android.intent.extra.changed_package_list");
            }
            changed = added;
            added = replacing2;
            replacing2 = replacing;
            if (pkgList == null) {
            } else if (pkgList.length == 0) {
                str = action;
            } else {
                int uid = extras.getInt("android.intent.extra.UID");
                if (added) {
                    synchronized (BackupManagerService.this.mBackupParticipants) {
                        if (replacing2) {
                            try {
                                BackupManagerService.this.removePackageParticipantsLocked(pkgList, uid);
                            } catch (Throwable th2) {
                                th = th2;
                                str = action;
                            }
                        }
                        try {
                            BackupManagerService.this.addPackageParticipantsLocked(pkgList);
                        } catch (Throwable th3) {
                            th = th3;
                            str = action;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            }
                            throw th;
                        }
                    }
                }
                if (!replacing2) {
                    synchronized (BackupManagerService.this.mBackupParticipants) {
                        BackupManagerService.this.removePackageParticipantsLocked(pkgList, uid);
                    }
                }
                for (String action2 : pkgList) {
                    BackupManagerService.this.mBackupHandler.post(new -$$Lambda$BackupManagerService$2$PXK_S3ijBAkFZ4wQtjneIECynPo(this, action2));
                }
            }
            return;
            int i2;
            while (i2 < r13) {
                String packageName = pkgList[i2];
                try {
                    PackageInfo app = BackupManagerService.this.mPackageManager.getPackageInfo(packageName, i);
                    if (AppBackupUtils.appGetsFullBackup(app) && AppBackupUtils.appIsEligibleForBackup(app.applicationInfo, BackupManagerService.this.mPackageManager)) {
                        BackupManagerService.this.enqueueFullBackup(packageName, now);
                        str = action2;
                        try {
                            BackupManagerService.this.scheduleNextFullBackupJob(0);
                        } catch (NameNotFoundException e) {
                        }
                    } else {
                        str = action2;
                        synchronized (BackupManagerService.this.mQueueLock) {
                            BackupManagerService.this.dequeueFullBackupLocked(packageName);
                        }
                        BackupManagerService.this.writeFullBackupScheduleAsync();
                    }
                    BackupManagerService.this.mBackupHandler.post(new -$$Lambda$BackupManagerService$2$8WilE3DKM3p1qJhvhqvZiHtD9hI(this, packageName));
                } catch (NameNotFoundException e2) {
                    str = action2;
                    String str2 = BackupManagerService.TAG;
                    action2 = new StringBuilder();
                    action2.append("Can't resolve new app ");
                    action2.append(packageName);
                    Slog.w(str2, action2.toString());
                    i2++;
                    action2 = str;
                    intent2 = intent;
                    i = 0;
                }
                i2++;
                action2 = str;
                intent2 = intent;
                i = 0;
            }
            BackupManagerService.this.dataChangedImpl(BackupManagerService.PACKAGE_MANAGER_SENTINEL);
        }
    };
    private final Object mClearDataLock = new Object();
    private volatile boolean mClearingData;
    private IBackupAgent mConnectedAgent;
    private volatile boolean mConnecting;
    private BackupManagerConstants mConstants;
    private Context mContext;
    private final Object mCurrentOpLock = new Object();
    @GuardedBy("mCurrentOpLock")
    private final SparseArray<Operation> mCurrentOperations = new SparseArray();
    private long mCurrentToken = 0;
    private File mDataDir;
    private boolean mEnabled;
    @GuardedBy("mQueueLock")
    private ArrayList<FullBackupEntry> mFullBackupQueue;
    private File mFullBackupScheduleFile;
    private Runnable mFullBackupScheduleWriter = new Runnable() {
        public void run() {
            synchronized (BackupManagerService.this.mQueueLock) {
                try {
                    ByteArrayOutputStream bufStream = new ByteArrayOutputStream(4096);
                    DataOutputStream bufOut = new DataOutputStream(bufStream);
                    bufOut.writeInt(1);
                    int N = BackupManagerService.this.mFullBackupQueue.size();
                    bufOut.writeInt(N);
                    for (int i = 0; i < N; i++) {
                        FullBackupEntry entry = (FullBackupEntry) BackupManagerService.this.mFullBackupQueue.get(i);
                        bufOut.writeUTF(entry.packageName);
                        bufOut.writeLong(entry.lastBackup);
                    }
                    bufOut.flush();
                    AtomicFile af = new AtomicFile(BackupManagerService.this.mFullBackupScheduleFile);
                    FileOutputStream out = af.startWrite();
                    out.write(bufStream.toByteArray());
                    af.finishWrite(out);
                } catch (Exception e) {
                    Slog.e(BackupManagerService.TAG, "Unable to write backup schedule!", e);
                }
            }
        }
    };
    @GuardedBy("mPendingRestores")
    private boolean mIsRestoreInProgress;
    private DataChangedJournal mJournal;
    private File mJournalDir;
    private volatile long mLastBackupPass;
    final AtomicInteger mNextToken = new AtomicInteger();
    private PackageManager mPackageManager;
    private IPackageManager mPackageManagerBinder;
    private HashMap<String, BackupRequest> mPendingBackups = new HashMap();
    private final ArraySet<String> mPendingInits = new ArraySet();
    @GuardedBy("mPendingRestores")
    private final Queue<PerformUnifiedRestoreTask> mPendingRestores = new ArrayDeque();
    private PowerManager mPowerManager;
    private ProcessedPackagesJournal mProcessedPackagesJournal;
    private boolean mProvisioned;
    private ContentObserver mProvisionedObserver;
    private final Object mQueueLock = new Object();
    private final long mRegisterTransportsRequestedTime;
    private final SecureRandom mRng = new SecureRandom();
    private PendingIntent mRunBackupIntent;
    private BroadcastReceiver mRunBackupReceiver;
    private PendingIntent mRunInitIntent;
    private BroadcastReceiver mRunInitReceiver;
    @GuardedBy("mQueueLock")
    private PerformFullTransportBackupTask mRunningFullBackupTask;
    private IStorageManager mStorageManager;
    private File mTokenFile;
    private final Random mTokenGenerator = new Random();
    private final TransportManager mTransportManager;
    private WakeLock mWakelock;

    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
            BackupManagerService.sInstance = new Trampoline(context);
        }

        public void onStart() {
            publishBinderService(HealthServiceWrapper.INSTANCE_HEALTHD, BackupManagerService.sInstance);
        }

        public void onUnlockUser(int userId) {
            if (userId == 0) {
                BackupManagerService.sInstance.unlockSystemUser();
            }
        }
    }

    static Trampoline getInstance() {
        return sInstance;
    }

    public BackupManagerConstants getConstants() {
        return this.mConstants;
    }

    public BackupAgentTimeoutParameters getAgentTimeoutParameters() {
        return this.mAgentTimeoutParameters;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public PackageManager getPackageManager() {
        return this.mPackageManager;
    }

    public void setPackageManager(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    public IPackageManager getPackageManagerBinder() {
        return this.mPackageManagerBinder;
    }

    public void setPackageManagerBinder(IPackageManager packageManagerBinder) {
        this.mPackageManagerBinder = packageManagerBinder;
    }

    public IActivityManager getActivityManager() {
        return this.mActivityManager;
    }

    public void setActivityManager(IActivityManager activityManager) {
        this.mActivityManager = activityManager;
    }

    public AlarmManager getAlarmManager() {
        return this.mAlarmManager;
    }

    public void setAlarmManager(AlarmManager alarmManager) {
        this.mAlarmManager = alarmManager;
    }

    public void setBackupManagerBinder(IBackupManager backupManagerBinder) {
        this.mBackupManagerBinder = backupManagerBinder;
    }

    public TransportManager getTransportManager() {
        return this.mTransportManager;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isProvisioned() {
        return this.mProvisioned;
    }

    public void setProvisioned(boolean provisioned) {
        this.mProvisioned = provisioned;
    }

    public WakeLock getWakelock() {
        return this.mWakelock;
    }

    public void setWakelock(WakeLock wakelock) {
        this.mWakelock = wakelock;
    }

    public Handler getBackupHandler() {
        return this.mBackupHandler;
    }

    public void setBackupHandler(BackupHandler backupHandler) {
        this.mBackupHandler = backupHandler;
    }

    public PendingIntent getRunInitIntent() {
        return this.mRunInitIntent;
    }

    public void setRunInitIntent(PendingIntent runInitIntent) {
        this.mRunInitIntent = runInitIntent;
    }

    public HashMap<String, BackupRequest> getPendingBackups() {
        return this.mPendingBackups;
    }

    public void setPendingBackups(HashMap<String, BackupRequest> pendingBackups) {
        this.mPendingBackups = pendingBackups;
    }

    public Object getQueueLock() {
        return this.mQueueLock;
    }

    public boolean isBackupRunning() {
        return this.mBackupRunning;
    }

    public void setBackupRunning(boolean backupRunning) {
        this.mBackupRunning = backupRunning;
    }

    public long getLastBackupPass() {
        return this.mLastBackupPass;
    }

    public void setLastBackupPass(long lastBackupPass) {
        this.mLastBackupPass = lastBackupPass;
    }

    public Object getClearDataLock() {
        return this.mClearDataLock;
    }

    public boolean isClearingData() {
        return this.mClearingData;
    }

    public void setClearingData(boolean clearingData) {
        this.mClearingData = clearingData;
    }

    public boolean isRestoreInProgress() {
        return this.mIsRestoreInProgress;
    }

    public void setRestoreInProgress(boolean restoreInProgress) {
        this.mIsRestoreInProgress = restoreInProgress;
    }

    public Queue<PerformUnifiedRestoreTask> getPendingRestores() {
        return this.mPendingRestores;
    }

    public ActiveRestoreSession getActiveRestoreSession() {
        return this.mActiveRestoreSession;
    }

    public void setActiveRestoreSession(ActiveRestoreSession activeRestoreSession) {
        this.mActiveRestoreSession = activeRestoreSession;
    }

    public SparseArray<Operation> getCurrentOperations() {
        return this.mCurrentOperations;
    }

    public Object getCurrentOpLock() {
        return this.mCurrentOpLock;
    }

    public SparseArray<AdbParams> getAdbBackupRestoreConfirmations() {
        return this.mAdbBackupRestoreConfirmations;
    }

    public File getBaseStateDir() {
        return this.mBaseStateDir;
    }

    public void setBaseStateDir(File baseStateDir) {
        this.mBaseStateDir = baseStateDir;
    }

    public File getDataDir() {
        return this.mDataDir;
    }

    public void setDataDir(File dataDir) {
        this.mDataDir = dataDir;
    }

    public DataChangedJournal getJournal() {
        return this.mJournal;
    }

    public void setJournal(DataChangedJournal journal) {
        this.mJournal = journal;
    }

    public SecureRandom getRng() {
        return this.mRng;
    }

    public Set<String> getAncestralPackages() {
        return this.mAncestralPackages;
    }

    public void setAncestralPackages(Set<String> ancestralPackages) {
        this.mAncestralPackages = ancestralPackages;
    }

    public long getAncestralToken() {
        return this.mAncestralToken;
    }

    public void setAncestralToken(long ancestralToken) {
        this.mAncestralToken = ancestralToken;
    }

    public long getCurrentToken() {
        return this.mCurrentToken;
    }

    public void setCurrentToken(long currentToken) {
        this.mCurrentToken = currentToken;
    }

    public ArraySet<String> getPendingInits() {
        return this.mPendingInits;
    }

    public void clearPendingInits() {
        this.mPendingInits.clear();
    }

    public PerformFullTransportBackupTask getRunningFullBackupTask() {
        return this.mRunningFullBackupTask;
    }

    public void setRunningFullBackupTask(PerformFullTransportBackupTask runningFullBackupTask) {
        this.mRunningFullBackupTask = runningFullBackupTask;
    }

    public void unlockSystemUser() {
        Trace.traceBegin(64, "backup migrate");
        if (!backupSettingMigrated(0)) {
            Slog.i(TAG, "Backup enable apparently not migrated");
            ContentResolver r = sInstance.mContext.getContentResolver();
            int enableState = Secure.getIntForUser(r, BACKUP_ENABLE_FILE, -1, 0);
            if (enableState >= 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Migrating enable state ");
                boolean z = true;
                stringBuilder.append(enableState != 0);
                Slog.i(str, stringBuilder.toString());
                if (enableState == 0) {
                    z = false;
                }
                writeBackupEnableState(z, 0);
                Secure.putStringForUser(r, BACKUP_ENABLE_FILE, null, 0);
            } else {
                Slog.i(TAG, "Backup not yet configured; retaining null enable state");
            }
        }
        Trace.traceEnd(64);
        Trace.traceBegin(64, "backup enable");
        try {
            sInstance.setBackupEnabled(readBackupEnableState(0));
        } catch (RemoteException e) {
        }
        Trace.traceEnd(64);
    }

    public int generateRandomIntegerToken() {
        int token = this.mTokenGenerator.nextInt();
        if (token < 0) {
            token = -token;
        }
        return (token & -256) | (this.mNextToken.incrementAndGet() & 255);
    }

    public PackageManagerBackupAgent makeMetadataAgent() {
        PackageManagerBackupAgent pmAgent = new PackageManagerBackupAgent(this.mPackageManager);
        pmAgent.attach(this.mContext);
        pmAgent.onCreate();
        return pmAgent;
    }

    public PackageManagerBackupAgent makeMetadataAgent(List<PackageInfo> packages) {
        PackageManagerBackupAgent pmAgent = new PackageManagerBackupAgent(this.mPackageManager, packages);
        pmAgent.attach(this.mContext);
        pmAgent.onCreate();
        return pmAgent;
    }

    public void addBackupTrace(String s) {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.add(s);
        }
    }

    public void clearBackupTrace() {
        synchronized (this.mBackupTrace) {
            this.mBackupTrace.clear();
        }
    }

    public static BackupManagerService create(Context context, Trampoline parent, HandlerThread backupThread) {
        Set<ComponentName> transportWhitelist = SystemConfig.getInstance().getBackupTransportWhitelist();
        if (transportWhitelist == null) {
            transportWhitelist = Collections.emptySet();
        }
        String transport = Secure.getString(context.getContentResolver(), "backup_transport");
        if (TextUtils.isEmpty(transport)) {
            transport = null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Starting with transport ");
        stringBuilder.append(transport);
        Slog.v(str, stringBuilder.toString());
        return new BackupManagerService(context, parent, backupThread, new File(Environment.getDataDirectory(), HealthServiceWrapper.INSTANCE_HEALTHD), new File(Environment.getDownloadCacheDirectory(), "backup_stage"), new TransportManager(context, transportWhitelist, transport));
    }

    @VisibleForTesting
    BackupManagerService(Context context, Trampoline parent, HandlerThread backupThread, File baseStateDir, File dataDir, TransportManager transportManager) {
        Context context2 = context;
        this.mContext = context2;
        this.mPackageManager = context.getPackageManager();
        this.mPackageManagerBinder = AppGlobals.getPackageManager();
        this.mActivityManager = ActivityManager.getService();
        this.mAlarmManager = (AlarmManager) context2.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context2.getSystemService("power");
        this.mStorageManager = Stub.asInterface(ServiceManager.getService("mount"));
        this.mBackupManagerBinder = Trampoline.asInterface(parent.asBinder());
        this.mAgentTimeoutParameters = new BackupAgentTimeoutParameters(Handler.getMain(), this.mContext.getContentResolver());
        this.mAgentTimeoutParameters.start();
        this.mBackupHandler = new BackupHandler(this, backupThread.getLooper());
        ContentResolver resolver = context.getContentResolver();
        this.mProvisioned = Global.getInt(resolver, "device_provisioned", 0) != 0;
        this.mAutoRestore = Secure.getInt(resolver, "backup_auto_restore", 1) != 0;
        this.mProvisionedObserver = new ProvisionedObserver(this, this.mBackupHandler);
        resolver.registerContentObserver(Global.getUriFor("device_provisioned"), false, this.mProvisionedObserver);
        this.mBaseStateDir = baseStateDir;
        this.mBaseStateDir.mkdirs();
        if (!SELinux.restorecon(this.mBaseStateDir)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SELinux restorecon failed on ");
            stringBuilder.append(this.mBaseStateDir);
            Slog.e(str, stringBuilder.toString());
        }
        this.mDataDir = dataDir;
        this.mBackupPasswordManager = new BackupPasswordManager(this.mContext, this.mBaseStateDir, this.mRng);
        this.mRunBackupReceiver = new RunBackupReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RUN_BACKUP_ACTION);
        context2.registerReceiver(this.mRunBackupReceiver, filter, "android.permission.BACKUP", null);
        this.mRunInitReceiver = new RunInitializeReceiver(this);
        filter = new IntentFilter();
        filter.addAction(RUN_INITIALIZE_ACTION);
        context2.registerReceiver(this.mRunInitReceiver, filter, "android.permission.BACKUP", null);
        Intent backupIntent = new Intent(RUN_BACKUP_ACTION);
        backupIntent.addFlags(1073741824);
        this.mRunBackupIntent = PendingIntent.getBroadcast(context2, 0, backupIntent, 0);
        Intent initIntent = new Intent(RUN_INITIALIZE_ACTION);
        initIntent.addFlags(1073741824);
        this.mRunInitIntent = PendingIntent.getBroadcast(context2, 0, initIntent, 0);
        this.mJournalDir = new File(this.mBaseStateDir, "pending");
        this.mJournalDir.mkdirs();
        this.mJournal = null;
        this.mConstants = new BackupManagerConstants(this.mBackupHandler, this.mContext.getContentResolver());
        this.mConstants.start();
        TransportManager transportManager2 = this.mBaseStateDir;
        this.mFullBackupScheduleFile = new File(transportManager2, "fb-schedule");
        initPackageTracking();
        synchronized (this.mBackupParticipants) {
            try {
                addPackageParticipantsLocked(null);
            } finally {
                transportManager2 = transportManager;
                while (true) {
                }
            }
        }
        this.mTransportManager = transportManager2;
        this.mTransportManager.setOnTransportRegisteredListener(new -$$Lambda$BackupManagerService$QlgHuOXOPKAZpwyUhPFAintPnqM(this));
        this.mRegisterTransportsRequestedTime = SystemClock.elapsedRealtime();
        BackupHandler backupHandler = this.mBackupHandler;
        TransportManager transportManager3 = this.mTransportManager;
        Objects.requireNonNull(transportManager3);
        backupHandler.postDelayed(new -$$Lambda$pM_c5tVAGDtxjxLF_ONtACWWq6Q(transportManager3), INITIALIZATION_DELAY_MILLIS);
        this.mBackupHandler.postDelayed(new -$$Lambda$BackupManagerService$7naKh6MW6ryzdPxgJfM5jV1nHp4(this), INITIALIZATION_DELAY_MILLIS);
        this.mWakelock = this.mPowerManager.newWakeLock(1, "*backup*");
        this.mBackupPolicyEnforcer = new BackupPolicyEnforcer(context2);
    }

    private void initPackageTracking() {
        this.mTokenFile = new File(this.mBaseStateDir, "ancestral");
        DataInputStream tokenStream;
        try {
            tokenStream = new DataInputStream(new BufferedInputStream(new FileInputStream(this.mTokenFile)));
            if (tokenStream.readInt() == 1) {
                this.mAncestralToken = tokenStream.readLong();
                this.mCurrentToken = tokenStream.readLong();
                int numPackages = tokenStream.readInt();
                if (numPackages >= 0) {
                    this.mAncestralPackages = new HashSet();
                    for (int i = 0; i < numPackages; i++) {
                        this.mAncestralPackages.add(tokenStream.readUTF());
                    }
                }
            }
            $closeResource(null, tokenStream);
        } catch (FileNotFoundException e) {
            Slog.v(TAG, "No ancestral data");
        } catch (IOException e2) {
            Slog.w(TAG, "Unable to read token file", e2);
        } catch (Throwable th) {
            $closeResource(r1, tokenStream);
        }
        this.mProcessedPackagesJournal = new ProcessedPackagesJournal(this.mBaseStateDir);
        this.mProcessedPackagesJournal.init();
        synchronized (this.mQueueLock) {
            this.mFullBackupQueue = readFullBackupSchedule();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:105:0x0162, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:106:0x0164, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:107:0x0165, code skipped:
            r5 = r8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArrayList<FullBackupEntry> readFullBackupSchedule() {
        FileInputStream fstream;
        int version;
        int N;
        String str;
        StringBuilder stringBuilder;
        Throwable th;
        Throwable th2;
        boolean changed = false;
        ArrayList<FullBackupEntry> schedule = null;
        List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(this.mPackageManager);
        if (this.mFullBackupScheduleFile.exists()) {
            try {
                fstream = new FileInputStream(this.mFullBackupScheduleFile);
                Throwable th3 = null;
                try {
                    BufferedInputStream bufStream = new BufferedInputStream(fstream);
                    try {
                        DataInputStream in = new DataInputStream(bufStream);
                        try {
                            int version2 = in.readInt();
                            if (version2 != 1) {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unknown backup schedule version ");
                                stringBuilder2.append(version2);
                                Slog.e(str2, stringBuilder2.toString());
                                $closeResource(null, in);
                                $closeResource(null, bufStream);
                                $closeResource(null, fstream);
                                return null;
                            }
                            int N2 = in.readInt();
                            schedule = new ArrayList(N2);
                            HashSet<String> foundApps = new HashSet(N2);
                            int i = 0;
                            int i2 = 0;
                            while (true) {
                                int i3 = i2;
                                if (i3 >= N2) {
                                    break;
                                }
                                try {
                                    long lastBackup = in.readLong();
                                    String pkgName = in.readUTF();
                                    foundApps.add(pkgName);
                                    try {
                                        PackageInfo pkg = this.mPackageManager.getPackageInfo(pkgName, i);
                                        if (AppBackupUtils.appGetsFullBackup(pkg) && AppBackupUtils.appIsEligibleForBackup(pkg.applicationInfo, this.mPackageManager)) {
                                            version = version2;
                                            N = N2;
                                            try {
                                                schedule.add(new FullBackupEntry(pkgName, lastBackup));
                                            } catch (NameNotFoundException e) {
                                                str = TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("Package ");
                                                stringBuilder.append(pkgName);
                                                stringBuilder.append(" not installed; dropping from full backup");
                                                Slog.i(str, stringBuilder.toString());
                                                i2 = i3 + 1;
                                                version2 = version;
                                                N2 = N;
                                                th3 = null;
                                                i = 0;
                                            }
                                            i2 = i3 + 1;
                                            version2 = version;
                                            N2 = N;
                                            th3 = null;
                                            i = 0;
                                        } else {
                                            version = version2;
                                            N = N2;
                                            version2 = lastBackup;
                                            str = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Package ");
                                            stringBuilder.append(pkgName);
                                            stringBuilder.append(" no longer eligible for full backup");
                                            Slog.i(str, stringBuilder.toString());
                                            i2 = i3 + 1;
                                            version2 = version;
                                            N2 = N;
                                            th3 = null;
                                            i = 0;
                                        }
                                    } catch (NameNotFoundException e2) {
                                        version = version2;
                                        N = N2;
                                        version2 = lastBackup;
                                        str = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Package ");
                                        stringBuilder.append(pkgName);
                                        stringBuilder.append(" not installed; dropping from full backup");
                                        Slog.i(str, stringBuilder.toString());
                                        i2 = i3 + 1;
                                        version2 = version;
                                        N2 = N;
                                        th3 = null;
                                        i = 0;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    th2 = null;
                                    th3 = null;
                                    $closeResource(th3, in);
                                    throw th;
                                }
                            }
                            version = version2;
                            N = N2;
                            try {
                                for (PackageInfo app : apps) {
                                    if (AppBackupUtils.appGetsFullBackup(app) && AppBackupUtils.appIsEligibleForBackup(app.applicationInfo, this.mPackageManager) && !foundApps.contains(app.packageName)) {
                                        schedule.add(new FullBackupEntry(app.packageName, 0));
                                        changed = true;
                                    }
                                }
                                Collections.sort(schedule);
                                th2 = null;
                            } catch (Throwable th5) {
                                th = th5;
                                th2 = null;
                                th3 = null;
                                $closeResource(th3, in);
                                throw th;
                            }
                            try {
                                $closeResource(null, in);
                                $closeResource(null, bufStream);
                                $closeResource(null, fstream);
                            } catch (Throwable th6) {
                                th = th6;
                                th3 = th2;
                                $closeResource(th3, bufStream);
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            th2 = null;
                            $closeResource(th3, in);
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        th2 = null;
                        $closeResource(th3, bufStream);
                        throw th;
                    }
                } catch (Throwable th9) {
                    th3 = th9;
                }
            } catch (Exception e3) {
                Slog.e(TAG, "Unable to read backup schedule", e3);
                this.mFullBackupScheduleFile.delete();
                schedule = null;
            }
        }
        if (schedule == null) {
            changed = true;
            schedule = new ArrayList(apps.size());
            for (PackageInfo app2 : apps) {
                if (AppBackupUtils.appGetsFullBackup(app2) && AppBackupUtils.appIsEligibleForBackup(app2.applicationInfo, this.mPackageManager)) {
                    schedule.add(new FullBackupEntry(app2.packageName, 0));
                }
            }
        }
        if (changed) {
            writeFullBackupScheduleAsync();
        }
        return schedule;
        $closeResource(th2, fstream);
        throw th9;
    }

    private void writeFullBackupScheduleAsync() {
        this.mBackupHandler.removeCallbacks(this.mFullBackupScheduleWriter);
        this.mBackupHandler.post(this.mFullBackupScheduleWriter);
    }

    private void parseLeftoverJournals() {
        Iterator it = DataChangedJournal.listJournals(this.mJournalDir).iterator();
        while (it.hasNext()) {
            DataChangedJournal journal = (DataChangedJournal) it.next();
            if (!journal.equals(this.mJournal)) {
                try {
                    journal.forEach(new -$$Lambda$BackupManagerService$-mOc1e-1SsZws3njOjKXfyubq98(this));
                } catch (IOException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't read ");
                    stringBuilder.append(journal);
                    Slog.e(str, stringBuilder.toString(), e);
                }
            }
        }
    }

    public static /* synthetic */ void lambda$parseLeftoverJournals$0(BackupManagerService backupManagerService, String packageName) {
        Slog.i(TAG, "Found stale backup journal, scheduling");
        backupManagerService.dataChangedImpl(packageName);
    }

    public byte[] randomBytes(int bits) {
        byte[] array = new byte[(bits / 8)];
        this.mRng.nextBytes(array);
        return array;
    }

    public boolean setBackupPassword(String currentPw, String newPw) {
        return this.mBackupPasswordManager.setBackupPassword(currentPw, newPw);
    }

    public boolean hasBackupPassword() {
        return this.mBackupPasswordManager.hasBackupPassword();
    }

    public boolean backupPasswordMatches(String currentPw) {
        return this.mBackupPasswordManager.backupPasswordMatches(currentPw);
    }

    public void recordInitPending(boolean isPending, String transportName, String transportDirName) {
        synchronized (this.mQueueLock) {
            File initPendingFile = new File(new File(this.mBaseStateDir, transportDirName), INIT_SENTINEL_FILE_NAME);
            if (isPending) {
                this.mPendingInits.add(transportName);
                try {
                    new FileOutputStream(initPendingFile).close();
                } catch (IOException e) {
                }
            } else {
                initPendingFile.delete();
                this.mPendingInits.remove(transportName);
            }
        }
    }

    public void resetBackupState(File stateFileDir) {
        int i;
        int i2;
        synchronized (this.mQueueLock) {
            this.mProcessedPackagesJournal.reset();
            this.mCurrentToken = 0;
            writeRestoreTokens();
            i = 0;
            for (File sf : stateFileDir.listFiles()) {
                if (!sf.getName().equals(INIT_SENTINEL_FILE_NAME)) {
                    sf.delete();
                }
            }
        }
        synchronized (this.mBackupParticipants) {
            int N = this.mBackupParticipants.size();
            while (true) {
                i2 = i;
                if (i2 < N) {
                    HashSet<String> participants = (HashSet) this.mBackupParticipants.valueAt(i2);
                    if (participants != null) {
                        Iterator it = participants.iterator();
                        while (it.hasNext()) {
                            dataChangedImpl((String) it.next());
                        }
                    }
                    i = i2 + 1;
                }
            }
        }
    }

    private void onTransportRegistered(String transportName, String transportDirName) {
        long timeMs = SystemClock.elapsedRealtime() - this.mRegisterTransportsRequestedTime;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transport ");
        stringBuilder.append(transportName);
        stringBuilder.append(" registered ");
        stringBuilder.append(timeMs);
        stringBuilder.append("ms after first request (delay = ");
        stringBuilder.append(INITIALIZATION_DELAY_MILLIS);
        stringBuilder.append("ms)");
        Slog.d(str, stringBuilder.toString());
        File stateDir = new File(this.mBaseStateDir, transportDirName);
        stateDir.mkdirs();
        if (new File(stateDir, INIT_SENTINEL_FILE_NAME).exists()) {
            synchronized (this.mQueueLock) {
                this.mPendingInits.add(transportName);
                this.mAlarmManager.set(0, System.currentTimeMillis() + 60000, this.mRunInitIntent);
            }
        }
    }

    private void addPackageParticipantsLocked(String[] packageNames) {
        List<PackageInfo> targetApps = allAgentPackages();
        if (packageNames != null) {
            for (String packageName : packageNames) {
                addPackageParticipantsLockedInner(packageName, targetApps);
            }
            return;
        }
        addPackageParticipantsLockedInner(null, targetApps);
    }

    private void addPackageParticipantsLockedInner(String packageName, List<PackageInfo> targetPkgs) {
        for (PackageInfo pkg : targetPkgs) {
            if (packageName == null || pkg.packageName.equals(packageName)) {
                int uid = pkg.applicationInfo.uid;
                HashSet<String> set = (HashSet) this.mBackupParticipants.get(uid);
                if (set == null) {
                    set = new HashSet();
                    this.mBackupParticipants.put(uid, set);
                }
                set.add(pkg.packageName);
                this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(16, pkg.packageName));
            }
        }
    }

    private void removePackageParticipantsLocked(String[] packageNames, int oldUid) {
        if (packageNames == null) {
            Slog.w(TAG, "removePackageParticipants with null list");
            return;
        }
        for (String pkg : packageNames) {
            HashSet<String> set = (HashSet) this.mBackupParticipants.get(oldUid);
            if (set != null && set.contains(pkg)) {
                removePackageFromSetLocked(set, pkg);
                if (set.isEmpty()) {
                    this.mBackupParticipants.remove(oldUid);
                }
            }
        }
    }

    private void removePackageFromSetLocked(HashSet<String> set, String packageName) {
        if (set.contains(packageName)) {
            set.remove(packageName);
            this.mPendingBackups.remove(packageName);
        }
    }

    private List<PackageInfo> allAgentPackages() {
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackages(134217728);
        for (int a = packages.size() - 1; a >= 0; a--) {
            PackageInfo pkg = (PackageInfo) packages.get(a);
            try {
                ApplicationInfo app = pkg.applicationInfo;
                if (!((app.flags & 32768) == 0 || app.backupAgentName == null)) {
                    if ((app.flags & 67108864) == 0) {
                        app = this.mPackageManager.getApplicationInfo(pkg.packageName, 1024);
                        pkg.applicationInfo.sharedLibraryFiles = app.sharedLibraryFiles;
                    }
                }
                packages.remove(a);
            } catch (NameNotFoundException e) {
                packages.remove(a);
            }
        }
        return packages;
    }

    public void logBackupComplete(String packageName) {
        if (!packageName.equals(PACKAGE_MANAGER_SENTINEL)) {
            for (String receiver : this.mConstants.getBackupFinishedNotificationReceivers()) {
                Intent notification = new Intent();
                notification.setAction(BACKUP_FINISHED_ACTION);
                notification.setPackage(receiver);
                notification.addFlags(268435488);
                notification.putExtra("packageName", packageName);
                this.mContext.sendBroadcastAsUser(notification, UserHandle.OWNER);
            }
            this.mProcessedPackagesJournal.addPackage(packageName);
        }
    }

    public void writeRestoreTokens() {
        RandomAccessFile af;
        try {
            af = new RandomAccessFile(this.mTokenFile, "rwd");
            af.writeInt(1);
            af.writeLong(this.mAncestralToken);
            af.writeLong(this.mCurrentToken);
            if (this.mAncestralPackages == null) {
                af.writeInt(-1);
            } else {
                af.writeInt(this.mAncestralPackages.size());
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ancestral packages:  ");
                stringBuilder.append(this.mAncestralPackages.size());
                Slog.v(str, stringBuilder.toString());
                for (String pkgName : this.mAncestralPackages) {
                    af.writeUTF(pkgName);
                }
            }
            $closeResource(null, af);
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write token file:", e);
        } catch (Throwable th) {
            $closeResource(r1, af);
        }
    }

    public IBackupAgent bindToAgentSynchronous(ApplicationInfo app, int mode) {
        IBackupAgent agent = null;
        synchronized (this.mAgentConnectLock) {
            this.mConnecting = true;
            this.mConnectedAgent = null;
            try {
                if (this.mActivityManager.bindBackupAgent(app.packageName, mode, 0)) {
                    String str;
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("awaiting agent for ");
                    stringBuilder.append(app);
                    Slog.d(str2, stringBuilder.toString());
                    long timeoutMark = System.currentTimeMillis() + 10000;
                    while (this.mConnecting && this.mConnectedAgent == null && System.currentTimeMillis() < timeoutMark) {
                        try {
                            this.mAgentConnectLock.wait(5000);
                        } catch (InterruptedException e) {
                            String str3 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Interrupted: ");
                            stringBuilder2.append(e);
                            Slog.w(str3, stringBuilder2.toString());
                            this.mConnecting = false;
                            this.mConnectedAgent = null;
                        }
                    }
                    if (this.mConnecting) {
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Timeout waiting for agent ");
                        stringBuilder3.append(app);
                        Slog.w(str, stringBuilder3.toString());
                        this.mConnectedAgent = null;
                    }
                    str = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("got agent ");
                    stringBuilder4.append(this.mConnectedAgent);
                    Slog.i(str, stringBuilder4.toString());
                    agent = this.mConnectedAgent;
                }
            } catch (RemoteException e2) {
            }
        }
        if (agent == null) {
            try {
                this.mActivityManager.clearPendingBackup();
            } catch (RemoteException e3) {
            }
        }
        return agent;
    }

    public void clearApplicationDataSynchronous(String packageName, boolean keepSystemState) {
        try {
            if ((this.mPackageManager.getPackageInfo(packageName, 0).applicationInfo.flags & 64) != 0) {
                ClearDataObserver observer = new ClearDataObserver(this);
                synchronized (this.mClearDataLock) {
                    this.mClearingData = true;
                    try {
                        this.mActivityManager.clearApplicationUserData(packageName, keepSystemState, observer, 0);
                    } catch (RemoteException e) {
                    }
                    long timeoutMark = System.currentTimeMillis() + 10000;
                    while (this.mClearingData && System.currentTimeMillis() < timeoutMark) {
                        try {
                            this.mClearDataLock.wait(5000);
                        } catch (InterruptedException e2) {
                            this.mClearingData = false;
                        }
                    }
                }
            }
        } catch (NameNotFoundException e3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tried to clear data for ");
            stringBuilder.append(packageName);
            stringBuilder.append(" but not found");
            Slog.w(str, stringBuilder.toString());
        }
    }

    public long getAvailableRestoreToken(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreToken");
        long token = this.mAncestralToken;
        synchronized (this.mQueueLock) {
            if (this.mCurrentToken != 0 && this.mProcessedPackagesJournal.hasBeenProcessed(packageName)) {
                token = this.mCurrentToken;
            }
        }
        return token;
    }

    public int requestBackup(String[] packages, IBackupObserver observer, int flags) {
        return requestBackup(packages, observer, null, flags);
    }

    public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) {
        IBackupManagerMonitor monitor2;
        String[] strArr = packages;
        IBackupObserver iBackupObserver = observer;
        IBackupManagerMonitor iBackupManagerMonitor = monitor;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "requestBackup");
        if (strArr == null || strArr.length < 1) {
            Slog.e(TAG, "No packages named for backup request");
            BackupObserverUtils.sendBackupFinished(iBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            IBackupManagerMonitor monitor3 = BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, 49, null, 1, null);
            throw new IllegalArgumentException("No packages are provided for backup");
        } else if (this.mEnabled && this.mProvisioned) {
            try {
                String transportDirName = this.mTransportManager.getTransportDirName(this.mTransportManager.getCurrentTransportName());
                TransportClient transportClient = this.mTransportManager.getCurrentTransportClientOrThrow("BMS.requestBackup()");
                OnTaskFinishedListener listener = new -$$Lambda$BackupManagerService$d1gjNfZ3ZYIuaY4s01CFoLZa4Z0(this, transportClient);
                ArrayList<String> fullBackupList = new ArrayList();
                ArrayList<String> kvBackupList = new ArrayList();
                for (String packageName : strArr) {
                    if (PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
                        kvBackupList.add(packageName);
                    } else {
                        try {
                            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 134217728);
                            if (!AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo, this.mPackageManager)) {
                                BackupObserverUtils.sendBackupOnPackageResult(iBackupObserver, packageName, -2001);
                            } else if (AppBackupUtils.appGetsFullBackup(packageInfo)) {
                                fullBackupList.add(packageInfo.packageName);
                            } else {
                                kvBackupList.add(packageInfo.packageName);
                            }
                        } catch (NameNotFoundException e) {
                            BackupObserverUtils.sendBackupOnPackageResult(iBackupObserver, packageName, -2002);
                        }
                    }
                }
                EventLog.writeEvent(EventLogTags.BACKUP_REQUESTED, new Object[]{Integer.valueOf(strArr.length), Integer.valueOf(kvBackupList.size()), Integer.valueOf(fullBackupList.size())});
                boolean nonIncrementalBackup = (flags & 1) != 0;
                Message msg = this.mBackupHandler.obtainMessage(15);
                BackupParams backupParams = r3;
                BackupParams backupParams2 = new BackupParams(transportClient, transportDirName, kvBackupList, fullBackupList, iBackupObserver, iBackupManagerMonitor, listener, 1, nonIncrementalBackup);
                msg.obj = backupParams;
                this.mBackupHandler.sendMessage(msg);
                return 0;
            } catch (TransportNotRegisteredException e2) {
                BackupObserverUtils.sendBackupFinished(iBackupObserver, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                monitor2 = BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, 50, null, 1, null);
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
        } else {
            int logTag;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Backup requested but e=");
            stringBuilder.append(this.mEnabled);
            stringBuilder.append(" p=");
            stringBuilder.append(this.mProvisioned);
            Slog.i(str, stringBuilder.toString());
            BackupObserverUtils.sendBackupFinished(iBackupObserver, -2001);
            if (this.mProvisioned) {
                logTag = 13;
            } else {
                logTag = 14;
            }
            monitor2 = BackupManagerMonitorUtils.monitorEvent(iBackupManagerMonitor, logTag, null, 3, null);
            return -2001;
        }
    }

    public void cancelBackups() {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "cancelBackups");
        long oldToken = Binder.clearCallingIdentity();
        try {
            List<Integer> operationsToCancel = new ArrayList();
            synchronized (this.mCurrentOpLock) {
                for (int i = 0; i < this.mCurrentOperations.size(); i++) {
                    Operation op = (Operation) this.mCurrentOperations.valueAt(i);
                    int token = this.mCurrentOperations.keyAt(i);
                    if (op.type == 2) {
                        operationsToCancel.add(Integer.valueOf(token));
                    }
                }
            }
            for (Integer token2 : operationsToCancel) {
                handleCancel(token2.intValue(), true);
            }
            KeyValueBackupJob.schedule(this.mContext, SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT, this.mConstants);
            FullBackupJob.schedule(this.mContext, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT, this.mConstants);
            Binder.restoreCallingIdentity(oldToken);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public void prepareOperationTimeout(int token, long interval, BackupRestoreTask callback, int operationType) {
        if (operationType == 0 || operationType == 1) {
            synchronized (this.mCurrentOpLock) {
                this.mCurrentOperations.put(token, new Operation(0, callback, operationType));
                this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(getMessageIdForOperationType(operationType), token, 0, callback), interval);
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prepareOperationTimeout() doesn't support operation ");
        stringBuilder.append(Integer.toHexString(token));
        stringBuilder.append(" of type ");
        stringBuilder.append(operationType);
        Slog.wtf(str, stringBuilder.toString());
    }

    private int getMessageIdForOperationType(int operationType) {
        switch (operationType) {
            case 0:
                return 17;
            case 1:
                return 18;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getMessageIdForOperationType called on invalid operation type: ");
                stringBuilder.append(operationType);
                Slog.wtf(str, stringBuilder.toString());
                return -1;
        }
    }

    public void removeOperation(int token) {
        synchronized (this.mCurrentOpLock) {
            if (this.mCurrentOperations.get(token) == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Duplicate remove for operation. token=");
                stringBuilder.append(Integer.toHexString(token));
                Slog.w(str, stringBuilder.toString());
            }
            this.mCurrentOperations.remove(token);
        }
    }

    /* JADX WARNING: Missing block: B:13:?, code skipped:
            r0 = r1.state;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean waitUntilOperationComplete(int token) {
        Operation op;
        int finalState = 0;
        synchronized (this.mCurrentOpLock) {
            while (true) {
                op = (Operation) this.mCurrentOperations.get(token);
                if (op != null) {
                    if (op.state != 0) {
                        break;
                    }
                    try {
                        this.mCurrentOpLock.wait();
                    } catch (InterruptedException e) {
                    }
                } else {
                    break;
                }
            }
        }
        removeOperation(token);
        if (op != null) {
            this.mBackupHandler.removeMessages(getMessageIdForOperationType(op.type));
        }
        if (finalState == 1) {
            return true;
        }
        return false;
    }

    public void handleCancel(int token, boolean cancelAll) {
        Operation op;
        synchronized (this.mCurrentOpLock) {
            op = (Operation) this.mCurrentOperations.get(token);
            int state = op != null ? op.state : -1;
            if (state == 1) {
                Slog.w(TAG, "Operation already got an ack.Should have been removed from mCurrentOperations.");
                op = null;
                this.mCurrentOperations.delete(token);
            } else if (state == 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cancel: token=");
                stringBuilder.append(Integer.toHexString(token));
                Slog.v(str, stringBuilder.toString());
                op.state = -1;
                if (op.type == 0 || op.type == 1) {
                    this.mBackupHandler.removeMessages(getMessageIdForOperationType(op.type));
                }
            }
            this.mCurrentOpLock.notifyAll();
        }
        if (op != null && op.callback != null) {
            op.callback.handleCancel(cancelAll);
        }
    }

    public boolean isBackupOperationInProgress() {
        synchronized (this.mCurrentOpLock) {
            for (int i = 0; i < this.mCurrentOperations.size(); i++) {
                Operation op = (Operation) this.mCurrentOperations.valueAt(i);
                if (op.type == 2 && op.state == 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public void tearDownAgentAndKill(ApplicationInfo app) {
        if (app != null) {
            try {
                this.mActivityManager.unbindBackupAgent(app);
                if (app.uid >= 10000 && !app.packageName.equals("com.android.backupconfirm")) {
                    this.mActivityManager.killApplicationProcess(app.processName, app.uid);
                }
            } catch (RemoteException e) {
                Slog.d(TAG, "Lost app trying to shut down");
            }
        }
    }

    public boolean deviceIsEncrypted() {
        boolean z = true;
        try {
            if (this.mStorageManager.getEncryptionState() == 1 || this.mStorageManager.getPasswordType() == 1) {
                z = false;
            }
            return z;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to communicate with storagemanager service: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return true;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x004b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void scheduleNextFullBackupJob(long transportMinLatency) {
        Throwable th;
        synchronized (this.mQueueLock) {
            long j;
            try {
                if (this.mFullBackupQueue.size() > 0) {
                    long upcomingLastBackup = ((FullBackupEntry) this.mFullBackupQueue.get(0)).lastBackup;
                    long timeSinceLast = System.currentTimeMillis() - upcomingLastBackup;
                    long interval = this.mConstants.getFullBackupIntervalMilliseconds();
                    final long latency = Math.max(transportMinLatency, timeSinceLast < interval ? interval - timeSinceLast : 0);
                    this.mBackupHandler.postDelayed(new Runnable() {
                        public void run() {
                            FullBackupJob.schedule(BackupManagerService.this.mContext, latency, BackupManagerService.this.mConstants);
                        }
                    }, 2500);
                } else {
                    j = transportMinLatency;
                    Slog.i(TAG, "Full backup queue empty; not scheduling");
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    @GuardedBy("mQueueLock")
    private void dequeueFullBackupLocked(String packageName) {
        for (int i = this.mFullBackupQueue.size() - 1; i >= 0; i--) {
            if (packageName.equals(((FullBackupEntry) this.mFullBackupQueue.get(i)).packageName)) {
                this.mFullBackupQueue.remove(i);
            }
        }
    }

    public void enqueueFullBackup(String packageName, long lastBackedUp) {
        FullBackupEntry newEntry = new FullBackupEntry(packageName, lastBackedUp);
        synchronized (this.mQueueLock) {
            dequeueFullBackupLocked(packageName);
            int which = -1;
            if (lastBackedUp > 0) {
                which = this.mFullBackupQueue.size() - 1;
                while (which >= 0) {
                    if (((FullBackupEntry) this.mFullBackupQueue.get(which)).lastBackup <= lastBackedUp) {
                        this.mFullBackupQueue.add(which + 1, newEntry);
                        break;
                    }
                    which--;
                }
            }
            if (which < 0) {
                this.mFullBackupQueue.add(0, newEntry);
            }
        }
        writeFullBackupScheduleAsync();
    }

    private boolean fullBackupAllowable(String transportName) {
        if (this.mTransportManager.isTransportRegistered(transportName)) {
            try {
                if (new File(new File(this.mBaseStateDir, this.mTransportManager.getTransportDirName(transportName)), PACKAGE_MANAGER_SENTINEL).length() > 0) {
                    return true;
                }
                Slog.i(TAG, "Full backup requested but dataset not yet initialized");
                return false;
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to get transport name: ");
                stringBuilder.append(e.getMessage());
                Slog.w(str, stringBuilder.toString());
                return false;
            }
        }
        Slog.w(TAG, "Transport not registered; full data backup not performed");
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:104:0x017a  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00f4 A:{SYNTHETIC, Splitter:B:77:0x00f4} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01ae  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01ac  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01ac  */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01ae  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01ae  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01ac  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01ac  */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01ae  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:120:0x01ae  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x01ac  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x028b A:{LOOP_END, LOOP:0: B:26:0x0062->B:156:0x028b} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01dc A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean beginFullBackup(FullBackupJob scheduledJob) {
        long fullBackupInterval;
        long keyValueBackupInterval;
        Throwable th;
        Object obj;
        PowerSaveState powerSaveState;
        long j;
        boolean runBackup;
        boolean headBusy;
        long j2;
        long latency;
        long now = System.currentTimeMillis();
        synchronized (this.mConstants) {
            fullBackupInterval = this.mConstants.getFullBackupIntervalMilliseconds();
            keyValueBackupInterval = this.mConstants.getKeyValueBackupIntervalMilliseconds();
        }
        FullBackupEntry entry = null;
        long latency2 = fullBackupInterval;
        if (!this.mEnabled) {
        } else if (this.mProvisioned) {
            PowerSaveState result = this.mPowerManager.getPowerSaveState(4);
            if (result.batterySaverEnabled) {
                Slog.i(TAG, "Deferring scheduled full backups in battery saver mode");
                FullBackupJob.schedule(this.mContext, keyValueBackupInterval, this.mConstants);
                return false;
            }
            Slog.i(TAG, "Beginning scheduled full backup operation");
            Object obj2 = this.mQueueLock;
            synchronized (obj2) {
                try {
                    if (this.mRunningFullBackupTask != null) {
                        try {
                            Slog.e(TAG, "Backup triggered but one already/still running!");
                            return false;
                        } catch (Throwable th2) {
                            th = th2;
                            obj = obj2;
                            powerSaveState = result;
                            j = keyValueBackupInterval;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    }
                    FullBackupEntry entry2;
                    long latency3;
                    boolean runBackup2 = true;
                    while (this.mFullBackupQueue.size() != 0) {
                        FullBackupEntry entry3;
                        try {
                            long latency4;
                            boolean headBusy2 = false;
                            String transportName = this.mTransportManager.getCurrentTransportName();
                            if (!fullBackupAllowable(transportName)) {
                                runBackup2 = false;
                                latency2 = keyValueBackupInterval;
                            }
                            if (runBackup2) {
                                try {
                                    try {
                                        entry = (FullBackupEntry) this.mFullBackupQueue.get(null);
                                        latency4 = latency2;
                                        try {
                                            latency2 = now - entry.lastBackup;
                                            boolean runBackup3 = latency2 >= fullBackupInterval;
                                            if (!runBackup3) {
                                                entry3 = fullBackupInterval - latency2;
                                                entry2 = entry;
                                                runBackup2 = runBackup3;
                                                latency3 = entry3;
                                                break;
                                            }
                                            String str;
                                            try {
                                                try {
                                                    PackageInfo appInfo = this.mPackageManager.getPackageInfo(entry.packageName, 0);
                                                    if (AppBackupUtils.appGetsFullBackup(appInfo)) {
                                                        boolean z;
                                                        int privFlags = appInfo.applicationInfo.privateFlags;
                                                        if ((privFlags & 8192) == 0) {
                                                            if (this.mActivityManager.isAppForeground(appInfo.applicationInfo.uid)) {
                                                                z = true;
                                                                headBusy2 = z;
                                                                if (headBusy2) {
                                                                    runBackup = runBackup3;
                                                                    headBusy = headBusy2;
                                                                    str = transportName;
                                                                } else {
                                                                    try {
                                                                        runBackup = runBackup3;
                                                                    } catch (NameNotFoundException e) {
                                                                        runBackup = runBackup3;
                                                                        headBusy = headBusy2;
                                                                        str = transportName;
                                                                        if (this.mFullBackupQueue.size() <= 1) {
                                                                        }
                                                                        runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                                        if (headBusy2) {
                                                                        }
                                                                    } catch (RemoteException e2) {
                                                                        runBackup = runBackup3;
                                                                        headBusy = headBusy2;
                                                                        str = transportName;
                                                                        runBackup2 = runBackup;
                                                                        if (headBusy2) {
                                                                        }
                                                                    }
                                                                    try {
                                                                        headBusy = headBusy2;
                                                                        latency2 = (System.currentTimeMillis() + SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) + ((long) this.mTokenGenerator.nextInt(true));
                                                                        try {
                                                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                                            String str2 = TAG;
                                                                            StringBuilder stringBuilder = new StringBuilder();
                                                                            try {
                                                                                stringBuilder.append("Full backup time but ");
                                                                                stringBuilder.append(entry.packageName);
                                                                                stringBuilder.append(" is busy; deferring to ");
                                                                                stringBuilder.append(sdf.format(new Date(latency2)));
                                                                                Slog.i(str2, stringBuilder.toString());
                                                                                enqueueFullBackup(entry.packageName, latency2 - fullBackupInterval);
                                                                            } catch (NameNotFoundException e3) {
                                                                                headBusy2 = headBusy;
                                                                            } catch (RemoteException e4) {
                                                                                headBusy2 = headBusy;
                                                                                runBackup2 = runBackup;
                                                                                if (headBusy2) {
                                                                                }
                                                                            }
                                                                        } catch (NameNotFoundException e5) {
                                                                            str = transportName;
                                                                            headBusy2 = headBusy;
                                                                            if (this.mFullBackupQueue.size() <= 1) {
                                                                            }
                                                                            runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                                            if (headBusy2) {
                                                                            }
                                                                        } catch (RemoteException e6) {
                                                                            str = transportName;
                                                                            headBusy2 = headBusy;
                                                                            runBackup2 = runBackup;
                                                                            if (headBusy2) {
                                                                            }
                                                                        }
                                                                    } catch (NameNotFoundException e7) {
                                                                        headBusy = headBusy2;
                                                                        str = transportName;
                                                                        if (this.mFullBackupQueue.size() <= 1) {
                                                                        }
                                                                        runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                                        if (headBusy2) {
                                                                        }
                                                                    } catch (RemoteException e8) {
                                                                        headBusy = headBusy2;
                                                                        str = transportName;
                                                                        runBackup2 = runBackup;
                                                                        if (headBusy2) {
                                                                        }
                                                                    }
                                                                }
                                                                runBackup2 = runBackup;
                                                                headBusy2 = headBusy;
                                                            }
                                                        }
                                                        z = false;
                                                        headBusy2 = z;
                                                        if (headBusy2) {
                                                        }
                                                        runBackup2 = runBackup;
                                                        headBusy2 = headBusy;
                                                    } else {
                                                        try {
                                                            this.mFullBackupQueue.remove(0);
                                                            headBusy2 = true;
                                                            runBackup2 = runBackup3;
                                                        } catch (NameNotFoundException e9) {
                                                            runBackup = runBackup3;
                                                            str = transportName;
                                                            if (this.mFullBackupQueue.size() <= 1) {
                                                            }
                                                            runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                            if (headBusy2) {
                                                            }
                                                        } catch (RemoteException e10) {
                                                            runBackup = runBackup3;
                                                            str = transportName;
                                                            runBackup2 = runBackup;
                                                            if (headBusy2) {
                                                            }
                                                        }
                                                    }
                                                } catch (NameNotFoundException e11) {
                                                    runBackup = runBackup3;
                                                    str = transportName;
                                                    runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                    if (headBusy2) {
                                                    }
                                                } catch (RemoteException e12) {
                                                    runBackup = runBackup3;
                                                    str = transportName;
                                                    runBackup2 = runBackup;
                                                    if (headBusy2) {
                                                    }
                                                }
                                            } catch (NameNotFoundException e13) {
                                                j2 = latency2;
                                                runBackup = runBackup3;
                                                str = transportName;
                                                if (this.mFullBackupQueue.size() <= 1) {
                                                }
                                                runBackup2 = this.mFullBackupQueue.size() <= 1;
                                                if (headBusy2) {
                                                }
                                            } catch (RemoteException e14) {
                                                j2 = latency2;
                                                runBackup = runBackup3;
                                                str = transportName;
                                                runBackup2 = runBackup;
                                                if (headBusy2) {
                                                }
                                            }
                                        } catch (Throwable th4) {
                                            th = th4;
                                            obj = obj2;
                                            powerSaveState = result;
                                            j = keyValueBackupInterval;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                        latency4 = latency2;
                                        obj = obj2;
                                        powerSaveState = result;
                                        j = keyValueBackupInterval;
                                        entry = entry;
                                        while (true) {
                                            break;
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    entry3 = entry;
                                    latency4 = latency2;
                                    obj = obj2;
                                    powerSaveState = result;
                                    j = keyValueBackupInterval;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            latency4 = latency2;
                            if (headBusy2) {
                                entry2 = entry;
                                latency3 = latency4;
                                break;
                            }
                            latency2 = latency4;
                        } catch (Throwable th7) {
                            th = th7;
                            entry3 = entry;
                            obj = obj2;
                            powerSaveState = result;
                            j = keyValueBackupInterval;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                    Slog.i(TAG, "Backup queue empty; doing nothing");
                    runBackup2 = false;
                    latency3 = latency2;
                    entry2 = entry;
                    if (runBackup2) {
                        latency = latency3;
                        try {
                            this.mFullBackupQueue.remove(0);
                            boolean z2 = true;
                            obj = obj2;
                            try {
                                this.mRunningFullBackupTask = PerformFullTransportBackupTask.newWithCurrentTransport(this, null, new String[]{entry2.packageName}, true, scheduledJob, new CountDownLatch(1), null, null, false, "BMS.beginFullBackup()");
                                this.mWakelock.acquire();
                                new Thread(this.mRunningFullBackupTask).start();
                                return z2;
                            } catch (Throwable th8) {
                                th = th8;
                                entry = entry2;
                                latency2 = latency;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            obj = obj2;
                            powerSaveState = result;
                            j = keyValueBackupInterval;
                            entry = entry2;
                            entry2 = latency;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                    try {
                        String str3 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Nothing pending full backup; rescheduling +");
                        stringBuilder2.append(latency3);
                        Slog.i(str3, stringBuilder2.toString());
                        latency = latency3;
                        latency3 = latency3;
                    } catch (Throwable th10) {
                        th = th10;
                        entry = entry2;
                        obj = obj2;
                        powerSaveState = result;
                        j = keyValueBackupInterval;
                        entry2 = latency3;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                    try {
                        this.mBackupHandler.post(new Runnable() {
                            public void run() {
                                FullBackupJob.schedule(BackupManagerService.this.mContext, latency3, BackupManagerService.this.mConstants);
                            }
                        });
                        return false;
                    } catch (Throwable th11) {
                        th = th11;
                        entry = entry2;
                        obj = obj2;
                        powerSaveState = result;
                        j = keyValueBackupInterval;
                        entry2 = latency;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    obj = obj2;
                    powerSaveState = result;
                    j = keyValueBackupInterval;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        } else {
            j = keyValueBackupInterval;
        }
        return false;
    }

    public void endFullBackup() {
        new Thread(new Runnable() {
            public void run() {
                PerformFullTransportBackupTask pftbt = null;
                synchronized (BackupManagerService.this.mQueueLock) {
                    if (BackupManagerService.this.mRunningFullBackupTask != null) {
                        pftbt = BackupManagerService.this.mRunningFullBackupTask;
                    }
                }
                if (pftbt != null) {
                    Slog.i(BackupManagerService.TAG, "Telling running backup to stop");
                    pftbt.handleCancel(true);
                }
            }
        }, "end-full-backup").start();
    }

    public void restoreWidgetData(String packageName, byte[] widgetData) {
        AppWidgetBackupBridge.restoreWidgetState(packageName, widgetData, 0);
    }

    public void dataChangedImpl(String packageName) {
        dataChangedImpl(packageName, dataChangedTargets(packageName));
    }

    private void dataChangedImpl(String packageName, HashSet<String> targets) {
        if (targets == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dataChanged but no participant pkg='");
            stringBuilder.append(packageName);
            stringBuilder.append("' uid=");
            stringBuilder.append(Binder.getCallingUid());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mQueueLock) {
            if (targets.contains(packageName)) {
                if (this.mPendingBackups.put(packageName, new BackupRequest(packageName)) == null) {
                    writeToJournalLocked(packageName);
                }
            }
        }
        KeyValueBackupJob.schedule(this.mContext, this.mConstants);
    }

    private HashSet<String> dataChangedTargets(String packageName) {
        HashSet hashSet;
        if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
            synchronized (this.mBackupParticipants) {
                hashSet = (HashSet) this.mBackupParticipants.get(Binder.getCallingUid());
            }
            return hashSet;
        } else if (PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
            return Sets.newHashSet(new String[]{PACKAGE_MANAGER_SENTINEL});
        } else {
            synchronized (this.mBackupParticipants) {
                hashSet = SparseArrayUtils.union(this.mBackupParticipants);
            }
            return hashSet;
        }
    }

    private void writeToJournalLocked(String str) {
        try {
            if (this.mJournal == null) {
                this.mJournal = DataChangedJournal.newJournal(this.mJournalDir);
            }
            this.mJournal.addPackage(str);
        } catch (IOException e) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't write ");
            stringBuilder.append(str);
            stringBuilder.append(" to backup journal");
            Slog.e(str2, stringBuilder.toString(), e);
            this.mJournal = null;
        }
    }

    public void dataChanged(final String packageName) {
        if (UserHandle.getCallingUserId() == 0) {
            final HashSet<String> targets = dataChangedTargets(packageName);
            if (targets == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("dataChanged but no participant pkg='");
                stringBuilder.append(packageName);
                stringBuilder.append("' uid=");
                stringBuilder.append(Binder.getCallingUid());
                Slog.w(str, stringBuilder.toString());
                return;
            }
            this.mBackupHandler.post(new Runnable() {
                public void run() {
                    BackupManagerService.this.dataChangedImpl(packageName, targets);
                }
            });
        }
    }

    public void initializeTransports(String[] transportNames, IBackupObserver observer) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "initializeTransport");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initializeTransport(): ");
        stringBuilder.append(Arrays.asList(transportNames));
        Slog.v(str, stringBuilder.toString());
        long oldId = Binder.clearCallingIdentity();
        try {
            this.mWakelock.acquire();
            this.mBackupHandler.post(new PerformInitializeTask(this, transportNames, observer, new -$$Lambda$BackupManagerService$uWCtISrzNRpV2diTzD5MWI0bdDM(this)));
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void clearBackupData(String transportName, String packageName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clearBackupData() of ");
        stringBuilder.append(packageName);
        stringBuilder.append(" on ");
        stringBuilder.append(transportName);
        Slog.v(str, stringBuilder.toString());
        try {
            Set<String> apps;
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 134217728);
            if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                apps = (Set) this.mBackupParticipants.get(Binder.getCallingUid());
            } else {
                apps = this.mProcessedPackagesJournal.getPackagesCopy();
            }
            if (apps.contains(packageName)) {
                this.mBackupHandler.removeMessages(12);
                synchronized (this.mQueueLock) {
                    TransportClient transportClient = this.mTransportManager.getTransportClient(transportName, "BMS.clearBackupData()");
                    if (transportClient == null) {
                        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(12, new ClearRetryParams(transportName, packageName)), SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                        return;
                    }
                    long oldId = Binder.clearCallingIdentity();
                    OnTaskFinishedListener listener = new -$$Lambda$BackupManagerService$drk8n83Z0hBmm5D4bbaFMr5WuzA(this, transportClient);
                    this.mWakelock.acquire();
                    this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(4, new ClearParams(transportClient, info, listener)));
                    Binder.restoreCallingIdentity(oldId);
                }
            }
        } catch (NameNotFoundException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No such package '");
            stringBuilder2.append(packageName);
            stringBuilder2.append("' - not clearing backup data");
            Slog.d(str2, stringBuilder2.toString());
        }
    }

    /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void backupNow() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "backupNow");
        if (this.mPowerManager.getPowerSaveState(5).batterySaverEnabled) {
            Slog.v(TAG, "Not running backup while in battery save mode");
            KeyValueBackupJob.schedule(this.mContext, this.mConstants);
            return;
        }
        Slog.v(TAG, "Scheduling immediate backup pass");
        synchronized (this.mQueueLock) {
            try {
                this.mRunBackupIntent.send();
            } catch (CanceledException e) {
                Slog.e(TAG, "run-backup intent cancelled!");
            }
            KeyValueBackupJob.cancel(this.mContext);
        }
    }

    public boolean deviceIsProvisioned() {
        return Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public void adbBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean compress, boolean doKeyValue, String[] pkgList) {
        IOException iOException;
        String str;
        StringBuilder stringBuilder;
        Throwable th;
        boolean z = includeShared;
        boolean z2 = doAllApps;
        Object obj = pkgList;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbBackup");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Backup supported only for the device owner");
        } else if (z2 || z || !(obj == null || obj.length == 0)) {
            long oldId = Binder.clearCallingIdentity();
            long oldId2;
            try {
                if (deviceIsProvisioned()) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Requesting backup: apks=");
                    boolean z3 = includeApks;
                    stringBuilder2.append(z3);
                    stringBuilder2.append(" obb=");
                    boolean z4 = includeObbs;
                    stringBuilder2.append(z4);
                    stringBuilder2.append(" shared=");
                    stringBuilder2.append(z);
                    stringBuilder2.append(" all=");
                    stringBuilder2.append(z2);
                    stringBuilder2.append(" system=");
                    stringBuilder2.append(includeSystem);
                    stringBuilder2.append(" includekeyvalue=");
                    stringBuilder2.append(doKeyValue);
                    stringBuilder2.append(" pkgs=");
                    stringBuilder2.append(obj);
                    Slog.v(str2, stringBuilder2.toString());
                    Slog.i(TAG, "Beginning adb backup...");
                    AdbBackupParams adbBackupParams = adbBackupParams;
                    boolean z5 = z;
                    oldId2 = oldId;
                    try {
                        adbBackupParams = new AdbBackupParams(fd, z3, z4, z5, doWidgets, z2, includeSystem, compress, doKeyValue, obj);
                        int token = generateRandomIntegerToken();
                        synchronized (this.mAdbBackupRestoreConfirmations) {
                            this.mAdbBackupRestoreConfirmations.put(token, adbBackupParams);
                        }
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Starting backup confirmation UI, token=");
                        stringBuilder3.append(token);
                        Slog.d(str2, stringBuilder3.toString());
                        if (startConfirmationUi(token, "fullback")) {
                            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
                            startConfirmationTimeout(token, adbBackupParams);
                            Slog.d(TAG, "Waiting for backup completion...");
                            waitForCompletion(adbBackupParams);
                            try {
                                fd.close();
                            } catch (IOException e) {
                                iOException = e;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("IO error closing output for adb backup: ");
                                stringBuilder.append(e.getMessage());
                                Slog.e(str, stringBuilder.toString());
                            }
                            Binder.restoreCallingIdentity(oldId2);
                            Slog.d(TAG, "Adb backup processing complete.");
                            return;
                        }
                        Slog.e(TAG, "Unable to launch backup confirmation UI");
                        this.mAdbBackupRestoreConfirmations.delete(token);
                        try {
                            fd.close();
                        } catch (IOException e2) {
                            IOException iOException2 = e2;
                            String str3 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("IO error closing output for adb backup: ");
                            stringBuilder4.append(e2.getMessage());
                            Slog.e(str3, stringBuilder4.toString());
                        }
                        Binder.restoreCallingIdentity(oldId2);
                        Slog.d(TAG, "Adb backup processing complete.");
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } else {
                    try {
                        Slog.i(TAG, "Backup not supported before setup");
                        try {
                            fd.close();
                        } catch (IOException e22) {
                            iOException = e22;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IO error closing output for adb backup: ");
                            stringBuilder.append(e22.getMessage());
                            Slog.e(str, stringBuilder.toString());
                        }
                        Binder.restoreCallingIdentity(oldId);
                        Slog.d(TAG, "Adb backup processing complete.");
                    } catch (Throwable th22) {
                        th = th22;
                        oldId2 = oldId;
                        try {
                            fd.close();
                        } catch (IOException e222) {
                            IOException iOException3 = e222;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IO error closing output for adb backup: ");
                            stringBuilder.append(e222.getMessage());
                            Slog.e(TAG, stringBuilder.toString());
                        }
                        Binder.restoreCallingIdentity(oldId2);
                        Slog.d(TAG, "Adb backup processing complete.");
                        throw th;
                    }
                }
            } catch (Throwable th222) {
                oldId2 = oldId;
                th = th222;
                fd.close();
                Binder.restoreCallingIdentity(oldId2);
                Slog.d(TAG, "Adb backup processing complete.");
                throw th;
            }
        } else {
            throw new IllegalArgumentException("Backup requested but neither shared nor any apps named");
        }
    }

    public void fullTransportBackup(String[] pkgNames) {
        Throwable th;
        String[] strArr = pkgNames;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullTransportBackup");
        if (UserHandle.getCallingUserId() == 0) {
            if (fullBackupAllowable(this.mTransportManager.getCurrentTransportName())) {
                Slog.d(TAG, "fullTransportBackup()");
                long oldId = Binder.clearCallingIdentity();
                long now;
                try {
                    CountDownLatch latch = new CountDownLatch(1);
                    long oldId2 = oldId;
                    try {
                        PerformFullTransportBackupTask task = PerformFullTransportBackupTask.newWithCurrentTransport(this, null, strArr, false, null, latch, null, null, false, "BMS.fullTransportBackup()");
                        this.mWakelock.acquire();
                        new Thread(task, "full-transport-master").start();
                        while (true) {
                            try {
                                latch.await();
                                break;
                            } catch (InterruptedException e) {
                                oldId2 = oldId2;
                            }
                        }
                        now = System.currentTimeMillis();
                        int length = strArr.length;
                        int i = 0;
                        while (i < length) {
                            try {
                                enqueueFullBackup(strArr[i], now);
                                i++;
                            } catch (Throwable th2) {
                                th = th2;
                                now = oldId2;
                                Binder.restoreCallingIdentity(now);
                                throw th;
                            }
                        }
                        Binder.restoreCallingIdentity(oldId2);
                    } catch (Throwable th3) {
                        th = th3;
                        now = oldId2;
                        Binder.restoreCallingIdentity(now);
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    now = oldId;
                    Binder.restoreCallingIdentity(now);
                    throw th;
                }
            }
            Slog.i(TAG, "Full backup not currently possible -- key/value backup not yet run?");
            Slog.d(TAG, "Done with full transport backup.");
            return;
        }
        throw new IllegalStateException("Restore supported only for the device owner");
    }

    public void adbRestore(ParcelFileDescriptor fd) {
        String str;
        StringBuilder stringBuilder;
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbRestore");
        if (UserHandle.getCallingUserId() == 0) {
            long oldId = Binder.clearCallingIdentity();
            try {
                if (deviceIsProvisioned()) {
                    Slog.i(TAG, "Beginning restore...");
                    AdbRestoreParams params = new AdbRestoreParams(fd);
                    int token = generateRandomIntegerToken();
                    synchronized (this.mAdbBackupRestoreConfirmations) {
                        this.mAdbBackupRestoreConfirmations.put(token, params);
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Starting restore confirmation UI, token=");
                    stringBuilder2.append(token);
                    Slog.d(str2, stringBuilder2.toString());
                    if (startConfirmationUi(token, "fullrest")) {
                        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
                        startConfirmationTimeout(token, params);
                        Slog.d(TAG, "Waiting for restore completion...");
                        waitForCompletion(params);
                        try {
                            fd.close();
                        } catch (IOException e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Error trying to close fd after adb restore: ");
                            stringBuilder.append(e);
                            Slog.w(str, stringBuilder.toString());
                        }
                        Binder.restoreCallingIdentity(oldId);
                        Slog.i(TAG, "adb restore processing complete.");
                        return;
                    }
                    Slog.e(TAG, "Unable to launch restore confirmation");
                    this.mAdbBackupRestoreConfirmations.delete(token);
                    try {
                        fd.close();
                    } catch (IOException e2) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Error trying to close fd after adb restore: ");
                        stringBuilder3.append(e2);
                        Slog.w(str3, stringBuilder3.toString());
                    }
                    Binder.restoreCallingIdentity(oldId);
                    Slog.i(TAG, "adb restore processing complete.");
                    return;
                }
                Slog.i(TAG, "Full restore not permitted before setup");
                try {
                    fd.close();
                } catch (IOException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error trying to close fd after adb restore: ");
                    stringBuilder.append(e3);
                    Slog.w(str, stringBuilder.toString());
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.i(TAG, "adb restore processing complete.");
            } catch (Throwable th) {
                try {
                    fd.close();
                } catch (IOException e4) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error trying to close fd after adb restore: ");
                    stringBuilder.append(e4);
                    Slog.w(TAG, stringBuilder.toString());
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.i(TAG, "adb restore processing complete.");
            }
        } else {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
    }

    private boolean startConfirmationUi(int token, String action) {
        try {
            Intent confIntent = new Intent(action);
            confIntent.setClassName("com.android.backupconfirm", "com.android.backupconfirm.BackupRestoreConfirmation");
            confIntent.putExtra("conftoken", token);
            confIntent.addFlags(536870912);
            this.mContext.startActivityAsUser(confIntent, UserHandle.SYSTEM);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    private void startConfirmationTimeout(int token, AdbParams params) {
        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(9, token, 0, params), 60000);
    }

    private void waitForCompletion(AdbParams params) {
        synchronized (params.latch) {
            while (!params.latch.get()) {
                try {
                    params.latch.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void signalAdbBackupRestoreCompletion(AdbParams params) {
        synchronized (params.latch) {
            params.latch.set(true);
            params.latch.notifyAll();
        }
    }

    public void acknowledgeAdbBackupOrRestore(int token, boolean allow, String curPassword, String encPpassword, IFullBackupRestoreObserver observer) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("acknowledgeAdbBackupOrRestore : token=");
        stringBuilder.append(token);
        stringBuilder.append(" allow=");
        stringBuilder.append(allow);
        Slog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "acknowledgeAdbBackupOrRestore");
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mAdbBackupRestoreConfirmations) {
                AdbParams params = (AdbParams) this.mAdbBackupRestoreConfirmations.get(token);
                if (params != null) {
                    this.mBackupHandler.removeMessages(9, params);
                    this.mAdbBackupRestoreConfirmations.delete(token);
                    if (allow) {
                        int verb;
                        if (params instanceof AdbBackupParams) {
                            verb = 2;
                        } else {
                            verb = 10;
                        }
                        params.observer = observer;
                        params.curPassword = curPassword;
                        params.encryptPassword = encPpassword;
                        this.mWakelock.acquire();
                        this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(verb, params));
                    } else {
                        Slog.w(TAG, "User rejected full backup/restore operation");
                        signalAdbBackupRestoreCompletion(params);
                    }
                } else {
                    Slog.w(TAG, "Attempted to ack full backup/restore with invalid token");
                }
            }
            Binder.restoreCallingIdentity(oldId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    private static boolean backupSettingMigrated(int userId) {
        return new File(new File(Environment.getDataDirectory(), HealthServiceWrapper.INSTANCE_HEALTHD), BACKUP_ENABLE_FILE).exists();
    }

    private static boolean readBackupEnableState(int userId) {
        File enableFile = new File(new File(Environment.getDataDirectory(), HealthServiceWrapper.INSTANCE_HEALTHD), BACKUP_ENABLE_FILE);
        if (enableFile.exists()) {
            FileInputStream fin;
            try {
                boolean z;
                fin = new FileInputStream(enableFile);
                if (fin.read() != 0) {
                    z = true;
                } else {
                    z = false;
                }
                $closeResource(null, fin);
                return z;
            } catch (IOException e) {
                Slog.e(TAG, "Cannot read enable state; assuming disabled");
            } catch (Throwable th) {
                $closeResource(r4, fin);
            }
        } else {
            Slog.i(TAG, "isBackupEnabled() => false due to absent settings file");
            return false;
        }
    }

    private static void writeBackupEnableState(boolean enable, int userId) {
        Throwable th;
        Throwable th2;
        File base = new File(Environment.getDataDirectory(), HealthServiceWrapper.INSTANCE_HEALTHD);
        File enableFile = new File(base, BACKUP_ENABLE_FILE);
        File stage = new File(base, "backup_enabled-stage");
        try {
            FileOutputStream fout = new FileOutputStream(stage);
            try {
                fout.write(enable);
                fout.close();
                stage.renameTo(enableFile);
                $closeResource(null, fout);
                return;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, fout);
            throw th;
        } catch (IOException | RuntimeException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to record backup enable state; reverting to disabled: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            Secure.putStringForUser(sInstance.mContext.getContentResolver(), BACKUP_ENABLE_FILE, null, userId);
            enableFile.delete();
            stage.delete();
        }
    }

    public void setBackupEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupEnabled");
        if (enable || this.mBackupPolicyEnforcer.getMandatoryBackupTransport() == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Backup enabled => ");
            stringBuilder.append(enable);
            Slog.i(str, stringBuilder.toString());
            long oldId = Binder.clearCallingIdentity();
            try {
                boolean wasEnabled = this.mEnabled;
                synchronized (this) {
                    writeBackupEnableState(enable, 0);
                    this.mEnabled = enable;
                }
                synchronized (this.mQueueLock) {
                    if (enable && !wasEnabled) {
                        if (this.mProvisioned) {
                            KeyValueBackupJob.schedule(this.mContext, this.mConstants);
                            scheduleNextFullBackupJob(0);
                        }
                    }
                    if (!enable) {
                        KeyValueBackupJob.cancel(this.mContext);
                        if (wasEnabled && this.mProvisioned) {
                            List<String> transportNames = new ArrayList();
                            List<String> transportDirNames = new ArrayList();
                            this.mTransportManager.forEachRegisteredTransport(new -$$Lambda$BackupManagerService$Yom7ZUYhsBBc6e92Mh_gepfydaQ(this, transportNames, transportDirNames));
                            for (int i = 0; i < transportNames.size(); i++) {
                                recordInitPending(true, (String) transportNames.get(i), (String) transportDirNames.get(i));
                            }
                            this.mAlarmManager.set(0, System.currentTimeMillis(), this.mRunInitIntent);
                        }
                    }
                }
                Binder.restoreCallingIdentity(oldId);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(oldId);
            }
        } else {
            Slog.w(TAG, "Cannot disable backups when the mandatory backups policy is active.");
        }
    }

    public static /* synthetic */ void lambda$setBackupEnabled$4(BackupManagerService backupManagerService, List transportNames, List transportDirNames, String name) {
        try {
            String dirName = backupManagerService.mTransportManager.getTransportDirName(name);
            transportNames.add(name);
            transportDirNames.add(dirName);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "Unexpected unregistered transport", e);
        }
    }

    public void setAutoRestore(boolean doAutoRestore) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setAutoRestore");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Auto restore => ");
        stringBuilder.append(doAutoRestore);
        Slog.i(str, stringBuilder.toString());
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Secure.putInt(this.mContext.getContentResolver(), "backup_auto_restore", doAutoRestore);
                this.mAutoRestore = doAutoRestore;
            }
            Binder.restoreCallingIdentity(oldId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void setBackupProvisioned(boolean available) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupProvisioned");
    }

    public boolean isBackupEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isBackupEnabled");
        return this.mEnabled;
    }

    public String getCurrentTransport() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getCurrentTransport");
        return this.mTransportManager.getCurrentTransportName();
    }

    public String[] listAllTransports() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransports");
        return this.mTransportManager.getRegisteredTransportNames();
    }

    public ComponentName[] listAllTransportComponents() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransportComponents");
        return this.mTransportManager.getRegisteredTransportComponents();
    }

    public String[] getTransportWhitelist() {
        Set<ComponentName> whitelistedComponents = this.mTransportManager.getTransportWhitelist();
        String[] whitelistedTransports = new String[whitelistedComponents.size()];
        int i = 0;
        for (ComponentName component : whitelistedComponents) {
            whitelistedTransports[i] = component.flattenToShortString();
            i++;
        }
        return whitelistedTransports;
    }

    public void updateTransportAttributes(ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, String dataManagementLabel) {
        updateTransportAttributes(Binder.getCallingUid(), transportComponent, name, configurationIntent, currentDestinationString, dataManagementIntent, dataManagementLabel);
    }

    @VisibleForTesting
    void updateTransportAttributes(int callingUid, ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, String dataManagementLabel) {
        Throwable th;
        NameNotFoundException e;
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "updateTransportAttributes");
        ComponentName componentName = transportComponent;
        Preconditions.checkNotNull(componentName, "transportComponent can't be null");
        String str = name;
        Preconditions.checkNotNull(str, "name can't be null");
        String str2 = currentDestinationString;
        Preconditions.checkNotNull(str2, "currentDestinationString can't be null");
        boolean z = true;
        if ((dataManagementIntent == null) != (dataManagementLabel == null)) {
            z = false;
        }
        Preconditions.checkArgument(z, "dataManagementLabel should be null iff dataManagementIntent is null");
        try {
            if (callingUid == this.mContext.getPackageManager().getPackageUid(transportComponent.getPackageName(), 0)) {
                long oldId = Binder.clearCallingIdentity();
                long oldId2;
                try {
                    ComponentName componentName2 = componentName;
                    oldId2 = oldId;
                    try {
                        this.mTransportManager.updateTransportAttributes(componentName2, str, configurationIntent, str2, dataManagementIntent, dataManagementLabel);
                        Binder.restoreCallingIdentity(oldId2);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(oldId2);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    oldId2 = oldId;
                    Binder.restoreCallingIdentity(oldId2);
                    throw th;
                }
            }
            try {
                throw new SecurityException("Only the transport can change its description");
            } catch (NameNotFoundException e2) {
                e = e2;
                throw new SecurityException("Transport package not found", e);
            }
        } catch (NameNotFoundException e3) {
            e = e3;
            int i = callingUid;
            throw new SecurityException("Transport package not found", e);
        }
    }

    @Deprecated
    public String selectBackupTransport(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransport");
        if (isAllowedByMandatoryBackupTransportPolicy(transportName)) {
            long oldId = Binder.clearCallingIdentity();
            try {
                String previousTransportName = this.mTransportManager.selectTransport(transportName);
                updateStateForTransport(transportName);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("selectBackupTransport(transport = ");
                stringBuilder.append(transportName);
                stringBuilder.append("): previous transport = ");
                stringBuilder.append(previousTransportName);
                Slog.v(str, stringBuilder.toString());
                return previousTransportName;
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } else {
            Slog.w(TAG, "Failed to select transport - disallowed by device owner policy.");
            return this.mTransportManager.getCurrentTransportName();
        }
    }

    public void selectBackupTransportAsync(ComponentName transportComponent, ISelectBackupTransportCallback listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransportAsync");
        if (isAllowedByMandatoryBackupTransportPolicy(transportComponent)) {
            long oldId = Binder.clearCallingIdentity();
            try {
                String transportString = transportComponent.flattenToShortString();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("selectBackupTransportAsync(transport = ");
                stringBuilder.append(transportString);
                stringBuilder.append(")");
                Slog.v(str, stringBuilder.toString());
                this.mBackupHandler.post(new -$$Lambda$BackupManagerService$DOiHwWNGzZZlYYmgVyeCon2E8lc(this, transportComponent, listener));
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } else {
            if (listener != null) {
                try {
                    Slog.w(TAG, "Failed to select transport - disallowed by device owner policy.");
                    listener.onFailure(-2001);
                } catch (RemoteException e) {
                    Slog.e(TAG, "ISelectBackupTransportCallback listener not available");
                }
            }
        }
    }

    public static /* synthetic */ void lambda$selectBackupTransportAsync$5(BackupManagerService backupManagerService, ComponentName transportComponent, ISelectBackupTransportCallback listener) {
        String transportName = null;
        int result = backupManagerService.mTransportManager.registerAndSelectTransport(transportComponent);
        if (result == 0) {
            try {
                transportName = backupManagerService.mTransportManager.getTransportName(transportComponent);
                backupManagerService.updateStateForTransport(transportName);
            } catch (TransportNotRegisteredException e) {
                Slog.e(TAG, "Transport got unregistered");
                result = -1;
            }
        }
        if (listener == null) {
            return;
        }
        if (transportName != null) {
            try {
                listener.onSuccess(transportName);
                return;
            } catch (RemoteException e2) {
                Slog.e(TAG, "ISelectBackupTransportCallback listener not available");
                return;
            }
        }
        listener.onFailure(result);
    }

    private boolean isAllowedByMandatoryBackupTransportPolicy(String transportName) {
        ComponentName mandatoryBackupTransport = this.mBackupPolicyEnforcer.getMandatoryBackupTransport();
        if (mandatoryBackupTransport == null) {
            return true;
        }
        try {
            return TextUtils.equals(this.mTransportManager.getTransportName(mandatoryBackupTransport), transportName);
        } catch (TransportNotRegisteredException e) {
            Slog.e(TAG, "mandatory backup transport not registered!");
            return false;
        }
    }

    private boolean isAllowedByMandatoryBackupTransportPolicy(ComponentName transport) {
        ComponentName mandatoryBackupTransport = this.mBackupPolicyEnforcer.getMandatoryBackupTransport();
        if (mandatoryBackupTransport == null) {
            return true;
        }
        return mandatoryBackupTransport.equals(transport);
    }

    private void updateStateForTransport(String newTransportName) {
        Secure.putString(this.mContext.getContentResolver(), "backup_transport", newTransportName);
        String callerLogString = "BMS.updateStateForTransport()";
        TransportClient transportClient = this.mTransportManager.getTransportClient(newTransportName, callerLogString);
        if (transportClient != null) {
            try {
                this.mCurrentToken = transportClient.connectOrThrow(callerLogString).getCurrentRestoreSet();
            } catch (Exception e) {
                this.mCurrentToken = 0;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Transport ");
                stringBuilder.append(newTransportName);
                stringBuilder.append(" not available: current token = 0");
                Slog.w(str, stringBuilder.toString());
            }
            this.mTransportManager.disposeOfTransportClient(transportClient, callerLogString);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Transport ");
        stringBuilder2.append(newTransportName);
        stringBuilder2.append(" not registered: current token = 0");
        Slog.w(str2, stringBuilder2.toString());
        this.mCurrentToken = 0;
    }

    public Intent getConfigurationIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getConfigurationIntent");
        try {
            return this.mTransportManager.getTransportConfigurationIntent(transportName);
        } catch (TransportNotRegisteredException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get configuration intent from transport: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public String getDestinationString(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDestinationString");
        try {
            return this.mTransportManager.getTransportCurrentDestinationString(transportName);
        } catch (TransportNotRegisteredException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get destination string from transport: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public Intent getDataManagementIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementIntent");
        try {
            return this.mTransportManager.getTransportDataManagementIntent(transportName);
        } catch (TransportNotRegisteredException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get management intent from transport: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public String getDataManagementLabel(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementLabel");
        try {
            return this.mTransportManager.getTransportDataManagementLabel(transportName);
        } catch (TransportNotRegisteredException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to get management label from transport: ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    public void agentConnected(String packageName, IBinder agentBinder) {
        synchronized (this.mAgentConnectLock) {
            String str;
            StringBuilder stringBuilder;
            if (Binder.getCallingUid() == 1000) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("agentConnected pkg=");
                stringBuilder.append(packageName);
                stringBuilder.append(" agent=");
                stringBuilder.append(agentBinder);
                Slog.d(str, stringBuilder.toString());
                this.mConnectedAgent = IBackupAgent.Stub.asInterface(agentBinder);
                this.mConnecting = false;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Non-system process uid=");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" claiming agent connected");
                Slog.w(str, stringBuilder.toString());
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void agentDisconnected(String packageName) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                this.mConnectedAgent = null;
                this.mConnecting = false;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Non-system process uid=");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" claiming agent disconnected");
                Slog.w(str, stringBuilder.toString());
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void restoreAtInstall(String packageName, int token) {
        if (Binder.getCallingUid() != 1000) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Non-system process uid=");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" attemping install-time restore");
            Slog.w(str, stringBuilder.toString());
            return;
        }
        boolean skip = false;
        long restoreSet = getAvailableRestoreToken(packageName);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("restoreAtInstall pkg=");
        stringBuilder2.append(packageName);
        stringBuilder2.append(" token=");
        stringBuilder2.append(Integer.toHexString(token));
        stringBuilder2.append(" restoreSet=");
        stringBuilder2.append(Long.toHexString(restoreSet));
        Slog.v(str2, stringBuilder2.toString());
        if (restoreSet == 0) {
            skip = true;
        }
        TransportClient transportClient = this.mTransportManager.getCurrentTransportClient("BMS.restoreAtInstall()");
        if (transportClient == null) {
            Slog.w(TAG, "No transport client");
            skip = true;
        }
        if (!this.mAutoRestore) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Non-restorable state: auto=");
            stringBuilder2.append(this.mAutoRestore);
            Slog.w(str2, stringBuilder2.toString());
            skip = true;
        }
        if (!skip) {
            try {
                this.mWakelock.acquire();
                OnTaskFinishedListener listener = new -$$Lambda$BackupManagerService$XAHW8jFVbxm2U5esUnLTgJC_Z6Y(this, transportClient);
                Message msg = this.mBackupHandler.obtainMessage(3);
                msg.obj = RestoreParams.createForRestoreAtInstall(transportClient, null, null, restoreSet, packageName, token, listener);
                this.mBackupHandler.sendMessage(msg);
            } catch (Exception e) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to contact transport: ");
                stringBuilder3.append(e.getMessage());
                Slog.e(str3, stringBuilder3.toString());
                skip = true;
            }
        }
        if (skip) {
            if (transportClient != null) {
                this.mTransportManager.disposeOfTransportClient(transportClient, "BMS.restoreAtInstall()");
            }
            Slog.v(TAG, "Finishing install immediately");
            try {
                this.mPackageManagerBinder.finishPackageInstall(token, false);
            } catch (RemoteException e2) {
            }
        }
    }

    public static /* synthetic */ void lambda$restoreAtInstall$6(BackupManagerService backupManagerService, TransportClient transportClient, String caller) {
        backupManagerService.mTransportManager.disposeOfTransportClient(transportClient, caller);
        backupManagerService.mWakelock.release();
    }

    public IRestoreSession beginRestoreSession(String packageName, String transport) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("beginRestoreSession: pkg=");
        stringBuilder.append(packageName);
        stringBuilder.append(" transport=");
        stringBuilder.append(transport);
        Slog.v(str, stringBuilder.toString());
        boolean needPermission = true;
        if (transport == null) {
            transport = this.mTransportManager.getCurrentTransportName();
            if (packageName != null) {
                PackageInfo app = null;
                try {
                    if (this.mPackageManager.getPackageInfo(packageName, 0).applicationInfo.uid == Binder.getCallingUid()) {
                        needPermission = false;
                    }
                } catch (NameNotFoundException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Asked to restore nonexistent pkg ");
                    stringBuilder2.append(packageName);
                    Slog.w(TAG, stringBuilder2.toString());
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Package ");
                    stringBuilder3.append(packageName);
                    stringBuilder3.append(" not found");
                    throw new IllegalArgumentException(stringBuilder3.toString());
                }
            }
        }
        if (needPermission) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "beginRestoreSession");
        } else {
            Slog.d(TAG, "restoring self on current transport; no permission needed");
        }
        synchronized (this) {
            if (this.mActiveRestoreSession != null) {
                Slog.i(TAG, "Restore session requested but one already active");
                return null;
            } else if (this.mBackupRunning) {
                Slog.i(TAG, "Restore session requested but currently running backups");
                return null;
            } else {
                this.mActiveRestoreSession = new ActiveRestoreSession(this, packageName, transport);
                this.mBackupHandler.sendEmptyMessageDelayed(8, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis());
                return this.mActiveRestoreSession;
            }
        }
    }

    public void clearRestoreSession(ActiveRestoreSession currentSession) {
        synchronized (this) {
            if (currentSession != this.mActiveRestoreSession) {
                Slog.e(TAG, "ending non-current restore session");
            } else {
                Slog.v(TAG, "Clearing restore session and halting timeout");
                this.mActiveRestoreSession = null;
                this.mBackupHandler.removeMessages(8);
            }
        }
    }

    public void opComplete(int token, long result) {
        Operation op;
        synchronized (this.mCurrentOpLock) {
            op = (Operation) this.mCurrentOperations.get(token);
            if (op != null) {
                if (op.state == -1) {
                    op = null;
                    this.mCurrentOperations.delete(token);
                } else if (op.state == 1) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Received duplicate ack for token=");
                    stringBuilder.append(Integer.toHexString(token));
                    Slog.w(str, stringBuilder.toString());
                    op = null;
                    this.mCurrentOperations.remove(token);
                } else if (op.state == 0) {
                    op.state = 1;
                }
            }
            this.mCurrentOpLock.notifyAll();
        }
        if (op != null && op.callback != null) {
            this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(21, Pair.create(op.callback, Long.valueOf(result))));
        }
    }

    public boolean isAppEligibleForBackup(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isAppEligibleForBackup");
        long oldToken = Binder.clearCallingIdentity();
        try {
            String callerLogString = "BMS.isAppEligibleForBackup";
            TransportClient transportClient = this.mTransportManager.getCurrentTransportClient(callerLogString);
            boolean eligible = AppBackupUtils.appIsRunningAndEligibleForBackupWithTransport(transportClient, packageName, this.mPackageManager);
            if (transportClient != null) {
                this.mTransportManager.disposeOfTransportClient(transportClient, callerLogString);
            }
            Binder.restoreCallingIdentity(oldToken);
            return eligible;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public String[] filterAppsEligibleForBackup(String[] packages) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "filterAppsEligibleForBackup");
        long oldToken = Binder.clearCallingIdentity();
        try {
            String callerLogString = "BMS.filterAppsEligibleForBackup";
            TransportClient transportClient = this.mTransportManager.getCurrentTransportClient(callerLogString);
            List<String> eligibleApps = new LinkedList();
            for (String packageName : packages) {
                if (AppBackupUtils.appIsRunningAndEligibleForBackupWithTransport(transportClient, packageName, this.mPackageManager)) {
                    eligibleApps.add(packageName);
                }
            }
            if (transportClient != null) {
                this.mTransportManager.disposeOfTransportClient(transportClient, callerLogString);
            }
            String[] strArr = (String[]) eligibleApps.toArray(new String[eligibleApps.size()]);
            return strArr;
        } finally {
            Binder.restoreCallingIdentity(oldToken);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            long identityToken = Binder.clearCallingIdentity();
            if (args != null) {
                try {
                    int length = args.length;
                    int i = 0;
                    while (i < length) {
                        String arg = args[i];
                        if ("-h".equals(arg)) {
                            pw.println("'dumpsys backup' optional arguments:");
                            pw.println("  -h       : this help text");
                            pw.println("  a[gents] : dump information about defined backup agents");
                            Binder.restoreCallingIdentity(identityToken);
                            return;
                        } else if ("agents".startsWith(arg)) {
                            dumpAgents(pw);
                            Binder.restoreCallingIdentity(identityToken);
                            return;
                        } else if ("transportclients".equals(arg.toLowerCase())) {
                            this.mTransportManager.dumpTransportClients(pw);
                            Binder.restoreCallingIdentity(identityToken);
                            return;
                        } else if ("transportstats".equals(arg.toLowerCase())) {
                            this.mTransportManager.dumpTransportStats(pw);
                            return;
                        } else {
                            i++;
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
            dumpInternal(pw);
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void dumpAgents(PrintWriter pw) {
        List<PackageInfo> agentPackages = allAgentPackages();
        pw.println("Defined backup agents:");
        for (PackageInfo pkg : agentPackages) {
            pw.print("  ");
            pw.print(pkg.packageName);
            pw.println(':');
            pw.print("      ");
            pw.println(pkg.applicationInfo.backupAgentName);
        }
    }

    private void dumpInternal(PrintWriter pw) {
        synchronized (this.mQueueLock) {
            int keyAt;
            StringBuilder stringBuilder;
            String str;
            String s;
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Backup Manager is ");
            stringBuilder3.append(this.mEnabled ? "enabled" : "disabled");
            stringBuilder3.append(" / ");
            stringBuilder3.append(!this.mProvisioned ? "not " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder3.append("provisioned / ");
            stringBuilder3.append(this.mPendingInits.size() == 0 ? "not " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            stringBuilder3.append("pending init");
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Auto-restore is ");
            stringBuilder3.append(this.mAutoRestore ? "enabled" : "disabled");
            pw.println(stringBuilder3.toString());
            if (this.mBackupRunning) {
                pw.println("Backup currently running");
            }
            pw.println(isBackupOperationInProgress() ? "Backup in progress" : "No backups running");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Last backup pass started: ");
            stringBuilder3.append(this.mLastBackupPass);
            stringBuilder3.append(" (now = ");
            stringBuilder3.append(System.currentTimeMillis());
            stringBuilder3.append(')');
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("  next scheduled: ");
            stringBuilder3.append(KeyValueBackupJob.nextScheduled());
            pw.println(stringBuilder3.toString());
            pw.println("Transport whitelist:");
            for (ComponentName transport : this.mTransportManager.getTransportWhitelist()) {
                pw.print("    ");
                pw.println(transport.flattenToShortString());
            }
            pw.println("Available transports:");
            String[] transports = listAllTransports();
            int i = 0;
            if (transports != null) {
                for (String t : transports) {
                    stringBuilder = new StringBuilder();
                    if (t.equals(this.mTransportManager.getCurrentTransportName())) {
                        str = "  * ";
                    } else {
                        str = "    ";
                    }
                    stringBuilder.append(str);
                    stringBuilder.append(t);
                    pw.println(stringBuilder.toString());
                    StringBuilder stringBuilder4;
                    try {
                        File dir = new File(this.mBaseStateDir, this.mTransportManager.getTransportDirName(t));
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("       destination: ");
                        stringBuilder4.append(this.mTransportManager.getTransportCurrentDestinationString(t));
                        pw.println(stringBuilder4.toString());
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("       intent: ");
                        stringBuilder4.append(this.mTransportManager.getTransportConfigurationIntent(t));
                        pw.println(stringBuilder4.toString());
                        for (File f : dir.listFiles()) {
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("       ");
                            stringBuilder5.append(f.getName());
                            stringBuilder5.append(" - ");
                            stringBuilder5.append(f.length());
                            stringBuilder5.append(" state bytes");
                            pw.println(stringBuilder5.toString());
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Error in transport", e);
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("        Error: ");
                        stringBuilder4.append(e);
                        pw.println(stringBuilder4.toString());
                    }
                }
            }
            this.mTransportManager.dumpTransportClients(pw);
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("Pending init: ");
            stringBuilder6.append(this.mPendingInits.size());
            pw.println(stringBuilder6.toString());
            Iterator it = this.mPendingInits.iterator();
            while (it.hasNext()) {
                s = (String) it.next();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    ");
                stringBuilder2.append(s);
                pw.println(stringBuilder2.toString());
            }
            synchronized (this.mBackupTrace) {
                if (!this.mBackupTrace.isEmpty()) {
                    pw.println("Most recent backup trace:");
                    for (String t2 : this.mBackupTrace) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("   ");
                        stringBuilder.append(t2);
                        pw.println(stringBuilder.toString());
                    }
                }
            }
            pw.print("Ancestral: ");
            pw.println(Long.toHexString(this.mAncestralToken));
            pw.print("Current:   ");
            pw.println(Long.toHexString(this.mCurrentToken));
            int size = this.mBackupParticipants.size();
            pw.println("Participants:");
            while (i < size) {
                keyAt = this.mBackupParticipants.keyAt(i);
                pw.print("  uid: ");
                pw.println(keyAt);
                Iterator it2 = ((HashSet) this.mBackupParticipants.valueAt(i)).iterator();
                while (it2.hasNext()) {
                    str = (String) it2.next();
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append("    ");
                    stringBuilder7.append(str);
                    pw.println(stringBuilder7.toString());
                }
                i++;
            }
            StringBuilder stringBuilder8 = new StringBuilder();
            stringBuilder8.append("Ancestral packages: ");
            stringBuilder8.append(this.mAncestralPackages == null ? "none" : Integer.valueOf(this.mAncestralPackages.size()));
            pw.println(stringBuilder8.toString());
            if (this.mAncestralPackages != null) {
                for (String s2 : this.mAncestralPackages) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    ");
                    stringBuilder2.append(s2);
                    pw.println(stringBuilder2.toString());
                }
            }
            Set<String> processedPackages = this.mProcessedPackagesJournal.getPackagesCopy();
            StringBuilder stringBuilder9 = new StringBuilder();
            stringBuilder9.append("Ever backed up: ");
            stringBuilder9.append(processedPackages.size());
            pw.println(stringBuilder9.toString());
            for (String t22 : processedPackages) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(t22);
                pw.println(stringBuilder.toString());
            }
            stringBuilder9 = new StringBuilder();
            stringBuilder9.append("Pending key/value backup: ");
            stringBuilder9.append(this.mPendingBackups.size());
            pw.println(stringBuilder9.toString());
            for (BackupRequest req : this.mPendingBackups.values()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(req);
                pw.println(stringBuilder.toString());
            }
            stringBuilder9 = new StringBuilder();
            stringBuilder9.append("Full backup queue:");
            stringBuilder9.append(this.mFullBackupQueue.size());
            pw.println(stringBuilder9.toString());
            Iterator it3 = this.mFullBackupQueue.iterator();
            while (it3.hasNext()) {
                FullBackupEntry entry = (FullBackupEntry) it3.next();
                pw.print("    ");
                pw.print(entry.lastBackup);
                pw.print(" : ");
                pw.println(entry.packageName);
            }
        }
    }

    public IBackupManager getBackupManagerBinder() {
        return this.mBackupManagerBinder;
    }
}

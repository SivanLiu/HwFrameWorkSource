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
import android.app.backup.SelectBackupTransportCallback;
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
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
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
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.DumpUtils;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.backup.fullbackup.FullBackupEntry;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.backup.internal.BackupRequest;
import com.android.server.backup.internal.ClearDataObserver;
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
import java.io.EOFException;
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
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class RefactoredBackupManagerService implements BackupManagerServiceInterface {
    private static final String BACKUP_ENABLE_FILE = "backup_enabled";
    public static final String BACKUP_FILE_HEADER_MAGIC = "ANDROID BACKUP\n";
    public static final int BACKUP_FILE_VERSION = 5;
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
    private static final String INIT_SENTINEL_FILE_NAME = "_need_init_";
    public static final String KEY_WIDGET_STATE = "￭￭widget";
    private static final long MIN_FULL_BACKUP_INTERVAL = 86400000;
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
    public static final long TIMEOUT_BACKUP_INTERVAL = 30000;
    public static final long TIMEOUT_FULL_BACKUP_INTERVAL = 300000;
    private static final long TIMEOUT_FULL_CONFIRMATION = 60000;
    private static final long TIMEOUT_INTERVAL = 10000;
    public static final long TIMEOUT_RESTORE_FINISHED_INTERVAL = 30000;
    public static final long TIMEOUT_RESTORE_INTERVAL = 60000;
    public static final long TIMEOUT_SHARED_BACKUP_INTERVAL = 1800000;
    private static final long TRANSPORT_RETRY_INTERVAL = 3600000;
    static Trampoline sInstance;
    private ActiveRestoreSession mActiveRestoreSession;
    private IActivityManager mActivityManager;
    private final SparseArray<AdbParams> mAdbBackupRestoreConfirmations = new SparseArray();
    private final Object mAgentConnectLock = new Object();
    private AlarmManager mAlarmManager;
    private Set<String> mAncestralPackages = null;
    private long mAncestralToken = 0;
    private boolean mAutoRestore;
    private BackupHandler mBackupHandler;
    private IBackupManager mBackupManagerBinder;
    private final SparseArray<HashSet<String>> mBackupParticipants = new SparseArray();
    private final BackupPasswordManager mBackupPasswordManager;
    private volatile boolean mBackupRunning;
    private final List<String> mBackupTrace = new ArrayList();
    private File mBaseStateDir;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String pkgName;
            String action = intent.getAction();
            boolean replacing = false;
            boolean z = false;
            Bundle extras = intent.getExtras();
            String[] strArr = null;
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                Uri uri = intent.getData();
                if (uri != null) {
                    pkgName = uri.getSchemeSpecificPart();
                    if (pkgName != null) {
                        strArr = new String[]{pkgName};
                    }
                    if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                        RefactoredBackupManagerService.this.mBackupHandler.post(new com.android.server.backup.-$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak.AnonymousClass2((byte) 1, this, pkgName, intent.getStringArrayExtra("android.intent.extra.changed_component_name_list")));
                        return;
                    }
                    z = "android.intent.action.PACKAGE_ADDED".equals(action);
                    replacing = extras.getBoolean("android.intent.extra.REPLACING", false);
                } else {
                    return;
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                z = true;
                strArr = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                z = false;
                strArr = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
            }
            if (strArr != null && strArr.length != 0) {
                int uid = extras.getInt("android.intent.extra.UID");
                if (z) {
                    synchronized (RefactoredBackupManagerService.this.mBackupParticipants) {
                        if (replacing) {
                            RefactoredBackupManagerService.this.removePackageParticipantsLocked(strArr, uid);
                        }
                        RefactoredBackupManagerService.this.addPackageParticipantsLocked(strArr);
                    }
                    long now = System.currentTimeMillis();
                    for (String packageName : strArr) {
                        try {
                            PackageInfo app = RefactoredBackupManagerService.this.mPackageManager.getPackageInfo(packageName, 0);
                            if (AppBackupUtils.appGetsFullBackup(app) && AppBackupUtils.appIsEligibleForBackup(app.applicationInfo)) {
                                RefactoredBackupManagerService.this.enqueueFullBackup(packageName, now);
                                RefactoredBackupManagerService.this.scheduleNextFullBackupJob(0);
                            } else {
                                synchronized (RefactoredBackupManagerService.this.mQueueLock) {
                                    RefactoredBackupManagerService.this.dequeueFullBackupLocked(packageName);
                                }
                                RefactoredBackupManagerService.this.writeFullBackupScheduleAsync();
                            }
                            RefactoredBackupManagerService.this.mBackupHandler.post(new com.android.server.backup.-$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak.AnonymousClass1((byte) 2, this, packageName));
                        } catch (NameNotFoundException e) {
                            Slog.w(RefactoredBackupManagerService.TAG, "Can't resolve new app " + packageName);
                        }
                    }
                    RefactoredBackupManagerService.this.dataChangedImpl(RefactoredBackupManagerService.PACKAGE_MANAGER_SENTINEL);
                } else {
                    if (!replacing) {
                        synchronized (RefactoredBackupManagerService.this.mBackupParticipants) {
                            RefactoredBackupManagerService.this.removePackageParticipantsLocked(strArr, uid);
                        }
                    }
                    for (String pkgName2 : strArr) {
                        RefactoredBackupManagerService.this.mBackupHandler.post(new com.android.server.backup.-$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak.AnonymousClass1((byte) 3, this, pkgName2));
                    }
                }
            }
        }

        /* synthetic */ void lambda$-com_android_server_backup_RefactoredBackupManagerService$3_49338(String pkgName, String[] components) {
            RefactoredBackupManagerService.this.mTransportManager.onPackageChanged(pkgName, components);
        }

        /* synthetic */ void lambda$-com_android_server_backup_RefactoredBackupManagerService$3_52086(String packageName) {
            RefactoredBackupManagerService.this.mTransportManager.onPackageAdded(packageName);
        }

        /* synthetic */ void lambda$-com_android_server_backup_RefactoredBackupManagerService$3_53299(String pkgName) {
            RefactoredBackupManagerService.this.mTransportManager.onPackageRemoved(pkgName);
        }
    };
    private final Object mClearDataLock = new Object();
    private volatile boolean mClearingData;
    private IBackupAgent mConnectedAgent;
    private volatile boolean mConnecting;
    private Context mContext;
    private final Object mCurrentOpLock = new Object();
    @GuardedBy("mCurrentOpLock")
    private final SparseArray<Operation> mCurrentOperations = new SparseArray();
    private long mCurrentToken = 0;
    private File mDataDir;
    private boolean mEnabled;
    private File mEverStored;
    private HashSet<String> mEverStoredApps = new HashSet();
    @GuardedBy("mQueueLock")
    private ArrayList<FullBackupEntry> mFullBackupQueue;
    private File mFullBackupScheduleFile;
    private Runnable mFullBackupScheduleWriter = new Runnable() {
        public void run() {
            synchronized (RefactoredBackupManagerService.this.mQueueLock) {
                try {
                    ByteArrayOutputStream bufStream = new ByteArrayOutputStream(4096);
                    DataOutputStream bufOut = new DataOutputStream(bufStream);
                    bufOut.writeInt(1);
                    int N = RefactoredBackupManagerService.this.mFullBackupQueue.size();
                    bufOut.writeInt(N);
                    for (int i = 0; i < N; i++) {
                        FullBackupEntry entry = (FullBackupEntry) RefactoredBackupManagerService.this.mFullBackupQueue.get(i);
                        bufOut.writeUTF(entry.packageName);
                        bufOut.writeLong(entry.lastBackup);
                    }
                    bufOut.flush();
                    AtomicFile af = new AtomicFile(RefactoredBackupManagerService.this.mFullBackupScheduleFile);
                    FileOutputStream out = af.startWrite();
                    out.write(bufStream.toByteArray());
                    af.finishWrite(out);
                } catch (Exception e) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Unable to write backup schedule!", e);
                }
            }
        }
    };
    private HandlerThread mHandlerThread;
    @GuardedBy("mPendingRestores")
    private boolean mIsRestoreInProgress;
    private DataChangedJournal mJournal;
    private File mJournalDir;
    private volatile long mLastBackupPass;
    private PackageManager mPackageManager;
    private IPackageManager mPackageManagerBinder;
    private HashMap<String, BackupRequest> mPendingBackups = new HashMap();
    private ArraySet<String> mPendingInits = new ArraySet();
    @GuardedBy("mPendingRestores")
    private final Queue<PerformUnifiedRestoreTask> mPendingRestores = new ArrayDeque();
    private PowerManager mPowerManager;
    private boolean mProvisioned;
    private ContentObserver mProvisionedObserver;
    private final Object mQueueLock = new Object();
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
    private TransportBoundListener mTransportBoundListener = new TransportBoundListener() {
        public boolean onTransportBound(IBackupTransport transport) {
            String str = null;
            try {
                str = transport.name();
                File stateDir = new File(RefactoredBackupManagerService.this.mBaseStateDir, transport.transportDirName());
                stateDir.mkdirs();
                if (new File(stateDir, RefactoredBackupManagerService.INIT_SENTINEL_FILE_NAME).exists()) {
                    synchronized (RefactoredBackupManagerService.this.mQueueLock) {
                        RefactoredBackupManagerService.this.mPendingInits.add(str);
                        RefactoredBackupManagerService.this.mAlarmManager.set(0, System.currentTimeMillis() + 60000, RefactoredBackupManagerService.this.mRunInitIntent);
                    }
                }
                return true;
            } catch (Exception e) {
                Slog.w(RefactoredBackupManagerService.TAG, "Failed to regiser transport: " + str);
                return false;
            }
        }
    };
    private final TransportManager mTransportManager;
    private WakeLock mWakelock;

    public static final class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
            RefactoredBackupManagerService.sInstance = new Trampoline(context);
        }

        public void onStart() {
            publishBinderService("backup", RefactoredBackupManagerService.sInstance);
        }

        public void onUnlockUser(int userId) {
            boolean z = true;
            if (userId == 0) {
                RefactoredBackupManagerService.sInstance.initialize(userId);
                if (!RefactoredBackupManagerService.backupSettingMigrated(userId)) {
                    Slog.i(RefactoredBackupManagerService.TAG, "Backup enable apparently not migrated");
                    ContentResolver r = RefactoredBackupManagerService.sInstance.mContext.getContentResolver();
                    int enableState = Secure.getIntForUser(r, RefactoredBackupManagerService.BACKUP_ENABLE_FILE, -1, userId);
                    if (enableState >= 0) {
                        boolean z2;
                        String str = RefactoredBackupManagerService.TAG;
                        StringBuilder append = new StringBuilder().append("Migrating enable state ");
                        if (enableState != 0) {
                            z2 = true;
                        } else {
                            z2 = false;
                        }
                        Slog.i(str, append.append(z2).toString());
                        if (enableState == 0) {
                            z = false;
                        }
                        RefactoredBackupManagerService.writeBackupEnableState(z, userId);
                        Secure.putStringForUser(r, RefactoredBackupManagerService.BACKUP_ENABLE_FILE, null, userId);
                    } else {
                        Slog.i(RefactoredBackupManagerService.TAG, "Backup not yet configured; retaining null enable state");
                    }
                }
                try {
                    RefactoredBackupManagerService.sInstance.setBackupEnabled(RefactoredBackupManagerService.readBackupEnableState(userId));
                } catch (RemoteException e) {
                }
            }
        }
    }

    static Trampoline getInstance() {
        return sInstance;
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

    public BackupHandler getBackupHandler() {
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

    public int generateRandomIntegerToken() {
        int token;
        do {
            synchronized (this.mTokenGenerator) {
                token = this.mTokenGenerator.nextInt();
            }
        } while (token < 0);
        return token;
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

    public RefactoredBackupManagerService(Context context, Trampoline parent) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mPackageManagerBinder = AppGlobals.getPackageManager();
        this.mActivityManager = ActivityManager.getService();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mStorageManager = Stub.asInterface(ServiceManager.getService("mount"));
        this.mBackupManagerBinder = Trampoline.asInterface(parent.asBinder());
        this.mHandlerThread = new HandlerThread("backup", 10);
        this.mHandlerThread.start();
        this.mBackupHandler = new BackupHandler(this, this.mHandlerThread.getLooper());
        ContentResolver resolver = context.getContentResolver();
        this.mProvisioned = Global.getInt(resolver, "device_provisioned", 0) != 0;
        this.mAutoRestore = Secure.getInt(resolver, "backup_auto_restore", 1) != 0;
        this.mProvisionedObserver = new ProvisionedObserver(this, this.mBackupHandler);
        resolver.registerContentObserver(Global.getUriFor("device_provisioned"), false, this.mProvisionedObserver);
        this.mBaseStateDir = new File(Environment.getDataDirectory(), "backup");
        this.mBaseStateDir.mkdirs();
        if (!SELinux.restorecon(this.mBaseStateDir)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBaseStateDir);
        }
        this.mDataDir = new File(Environment.getDownloadCacheDirectory(), "backup_stage");
        this.mBackupPasswordManager = new BackupPasswordManager(this.mContext, this.mBaseStateDir, this.mRng);
        this.mRunBackupReceiver = new RunBackupReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RUN_BACKUP_ACTION);
        context.registerReceiver(this.mRunBackupReceiver, filter, "android.permission.BACKUP", null);
        this.mRunInitReceiver = new RunInitializeReceiver(this);
        filter = new IntentFilter();
        filter.addAction(RUN_INITIALIZE_ACTION);
        context.registerReceiver(this.mRunInitReceiver, filter, "android.permission.BACKUP", null);
        Intent backupIntent = new Intent(RUN_BACKUP_ACTION);
        backupIntent.addFlags(1073741824);
        this.mRunBackupIntent = PendingIntent.getBroadcast(context, 0, backupIntent, 0);
        Intent initIntent = new Intent(RUN_INITIALIZE_ACTION);
        initIntent.addFlags(1073741824);
        this.mRunInitIntent = PendingIntent.getBroadcast(context, 0, initIntent, 0);
        this.mJournalDir = new File(this.mBaseStateDir, "pending");
        this.mJournalDir.mkdirs();
        this.mJournal = null;
        this.mFullBackupScheduleFile = new File(this.mBaseStateDir, "fb-schedule");
        initPackageTracking();
        synchronized (this.mBackupParticipants) {
            addPackageParticipantsLocked(null);
        }
        Set<ComponentName> transportWhitelist = SystemConfig.getInstance().getBackupTransportWhitelist();
        String transport = Secure.getString(context.getContentResolver(), "backup_transport");
        if (TextUtils.isEmpty(transport)) {
            transport = null;
        }
        String currentTransport = transport;
        Slog.v(TAG, "Starting with transport " + currentTransport);
        this.mTransportManager = new TransportManager(context, transportWhitelist, currentTransport, this.mTransportBoundListener, this.mHandlerThread.getLooper());
        this.mTransportManager.registerAllTransports();
        this.mBackupHandler.post(new -$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak((byte) 1, this));
        this.mWakelock = this.mPowerManager.newWakeLock(1, "*backup*");
    }

    /* synthetic */ void lambda$-com_android_server_backup_RefactoredBackupManagerService_30299() {
        parseLeftoverJournals();
    }

    private void initPackageTracking() {
        IOException e;
        File file;
        RandomAccessFile randomAccessFile;
        RandomAccessFile randomAccessFile2;
        RandomAccessFile in;
        String pkg;
        Throwable th;
        Throwable th2;
        Throwable th3;
        IntentFilter filter;
        IntentFilter sdFilter;
        this.mTokenFile = new File(this.mBaseStateDir, "ancestral");
        Throwable th4 = null;
        RandomAccessFile randomAccessFile3 = null;
        try {
            RandomAccessFile randomAccessFile4 = new RandomAccessFile(this.mTokenFile, "r");
            try {
                if (randomAccessFile4.readInt() == 1) {
                    this.mAncestralToken = randomAccessFile4.readLong();
                    this.mCurrentToken = randomAccessFile4.readLong();
                    int numPackages = randomAccessFile4.readInt();
                    if (numPackages >= 0) {
                        this.mAncestralPackages = new HashSet();
                        for (int i = 0; i < numPackages; i++) {
                            this.mAncestralPackages.add(randomAccessFile4.readUTF());
                        }
                    }
                }
                if (randomAccessFile4 != null) {
                    try {
                        randomAccessFile4.close();
                    } catch (Throwable th5) {
                        th4 = th5;
                    }
                }
                if (th4 != null) {
                    try {
                        throw th4;
                    } catch (FileNotFoundException e2) {
                        randomAccessFile3 = randomAccessFile4;
                    } catch (IOException e3) {
                        e = e3;
                        randomAccessFile3 = randomAccessFile4;
                        Slog.w(TAG, "Unable to read token file", e);
                        this.mEverStored = new File(this.mBaseStateDir, "processed");
                        file = new File(this.mBaseStateDir, "processed.new");
                        if (file.exists()) {
                            file.delete();
                        }
                        if (this.mEverStored.exists()) {
                            th4 = null;
                            randomAccessFile = null;
                            randomAccessFile2 = null;
                            try {
                                randomAccessFile4 = new RandomAccessFile(file, "rws");
                                try {
                                    in = new RandomAccessFile(this.mEverStored, "r");
                                    while (true) {
                                        try {
                                            pkg = in.readUTF();
                                            try {
                                                this.mPackageManager.getPackageInfo(pkg, 0);
                                                this.mEverStoredApps.add(pkg);
                                                randomAccessFile4.writeUTF(pkg);
                                            } catch (NameNotFoundException e4) {
                                            }
                                        } catch (Throwable th6) {
                                            th = th6;
                                            randomAccessFile2 = in;
                                            randomAccessFile = randomAccessFile4;
                                        }
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    randomAccessFile = randomAccessFile4;
                                    if (randomAccessFile2 != null) {
                                        try {
                                            randomAccessFile2.close();
                                        } catch (Throwable th8) {
                                            th3 = th8;
                                            if (th4 != null) {
                                                if (th4 != th3) {
                                                    th4.addSuppressed(th3);
                                                    th3 = th4;
                                                }
                                            }
                                        }
                                    }
                                    th3 = th4;
                                    if (randomAccessFile != null) {
                                        try {
                                            randomAccessFile.close();
                                        } catch (Throwable th9) {
                                            th4 = th9;
                                            if (th3 != null) {
                                                if (th3 != th4) {
                                                    th3.addSuppressed(th4);
                                                    th4 = th3;
                                                }
                                            }
                                        }
                                    }
                                    th4 = th3;
                                    if (th4 == null) {
                                        throw th;
                                    }
                                    try {
                                        throw th4;
                                    } catch (EOFException e5) {
                                        if (!file.renameTo(this.mEverStored)) {
                                            Slog.e(TAG, "Error renaming " + file + " to " + this.mEverStored);
                                        }
                                    } catch (IOException e6) {
                                        Slog.e(TAG, "Error in processed file", e6);
                                    }
                                }
                            } catch (Throwable th10) {
                                th = th10;
                                if (randomAccessFile2 != null) {
                                    randomAccessFile2.close();
                                }
                                th3 = th4;
                                if (randomAccessFile != null) {
                                    randomAccessFile.close();
                                }
                                th4 = th3;
                                if (th4 == null) {
                                    throw th4;
                                } else {
                                    throw th;
                                }
                            }
                        }
                        synchronized (this.mQueueLock) {
                            this.mFullBackupQueue = readFullBackupSchedule();
                        }
                        filter = new IntentFilter();
                        filter.addAction("android.intent.action.PACKAGE_ADDED");
                        filter.addAction("android.intent.action.PACKAGE_REMOVED");
                        filter.addAction("android.intent.action.PACKAGE_CHANGED");
                        filter.addDataScheme(HwBroadcastRadarUtil.KEY_PACKAGE);
                        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
                        sdFilter = new IntentFilter();
                        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
                        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
                        this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
                    }
                }
                randomAccessFile3 = randomAccessFile4;
                this.mEverStored = new File(this.mBaseStateDir, "processed");
                file = new File(this.mBaseStateDir, "processed.new");
                if (file.exists()) {
                    file.delete();
                }
                if (this.mEverStored.exists()) {
                    th4 = null;
                    randomAccessFile = null;
                    randomAccessFile2 = null;
                    randomAccessFile4 = new RandomAccessFile(file, "rws");
                    in = new RandomAccessFile(this.mEverStored, "r");
                    while (true) {
                        pkg = in.readUTF();
                        this.mPackageManager.getPackageInfo(pkg, 0);
                        this.mEverStoredApps.add(pkg);
                        randomAccessFile4.writeUTF(pkg);
                    }
                }
                synchronized (this.mQueueLock) {
                    this.mFullBackupQueue = readFullBackupSchedule();
                }
                filter = new IntentFilter();
                filter.addAction("android.intent.action.PACKAGE_ADDED");
                filter.addAction("android.intent.action.PACKAGE_REMOVED");
                filter.addAction("android.intent.action.PACKAGE_CHANGED");
                filter.addDataScheme(HwBroadcastRadarUtil.KEY_PACKAGE);
                this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
                sdFilter = new IntentFilter();
                sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
                sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
                this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
            } catch (Throwable th11) {
                th = th11;
                randomAccessFile3 = randomAccessFile4;
                if (randomAccessFile3 != null) {
                    try {
                        randomAccessFile3.close();
                    } catch (Throwable th32) {
                        if (th4 == null) {
                            th4 = th32;
                        } else if (th4 != th32) {
                            th4.addSuppressed(th32);
                        }
                    }
                }
                if (th4 == null) {
                    try {
                        throw th4;
                    } catch (FileNotFoundException e7) {
                    } catch (IOException e8) {
                        e6 = e8;
                        Slog.w(TAG, "Unable to read token file", e6);
                        this.mEverStored = new File(this.mBaseStateDir, "processed");
                        file = new File(this.mBaseStateDir, "processed.new");
                        if (file.exists()) {
                            file.delete();
                        }
                        if (this.mEverStored.exists()) {
                            th4 = null;
                            randomAccessFile = null;
                            randomAccessFile2 = null;
                            randomAccessFile4 = new RandomAccessFile(file, "rws");
                            in = new RandomAccessFile(this.mEverStored, "r");
                            while (true) {
                                pkg = in.readUTF();
                                this.mPackageManager.getPackageInfo(pkg, 0);
                                this.mEverStoredApps.add(pkg);
                                randomAccessFile4.writeUTF(pkg);
                            }
                        }
                        synchronized (this.mQueueLock) {
                            this.mFullBackupQueue = readFullBackupSchedule();
                        }
                        filter = new IntentFilter();
                        filter.addAction("android.intent.action.PACKAGE_ADDED");
                        filter.addAction("android.intent.action.PACKAGE_REMOVED");
                        filter.addAction("android.intent.action.PACKAGE_CHANGED");
                        filter.addDataScheme(HwBroadcastRadarUtil.KEY_PACKAGE);
                        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
                        sdFilter = new IntentFilter();
                        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
                        sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
                        this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
                    }
                }
                throw th;
            }
            Slog.v(TAG, "No ancestral data");
            this.mEverStored = new File(this.mBaseStateDir, "processed");
            file = new File(this.mBaseStateDir, "processed.new");
            if (file.exists()) {
                file.delete();
            }
            if (this.mEverStored.exists()) {
                th4 = null;
                randomAccessFile = null;
                randomAccessFile2 = null;
                randomAccessFile4 = new RandomAccessFile(file, "rws");
                in = new RandomAccessFile(this.mEverStored, "r");
                while (true) {
                    pkg = in.readUTF();
                    this.mPackageManager.getPackageInfo(pkg, 0);
                    this.mEverStoredApps.add(pkg);
                    randomAccessFile4.writeUTF(pkg);
                }
            }
            synchronized (this.mQueueLock) {
                this.mFullBackupQueue = readFullBackupSchedule();
            }
            filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme(HwBroadcastRadarUtil.KEY_PACKAGE);
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
            sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            this.mContext.registerReceiver(this.mBroadcastReceiver, sdFilter);
        } catch (Throwable th12) {
            th = th12;
            if (randomAccessFile3 != null) {
                randomAccessFile3.close();
            }
            if (th4 == null) {
                throw th;
            } else {
                throw th4;
            }
        }
    }

    private ArrayList<FullBackupEntry> readFullBackupSchedule() {
        Throwable th;
        Exception e;
        ArrayList<FullBackupEntry> arrayList;
        Throwable th2;
        boolean changed = false;
        ArrayList<FullBackupEntry> arrayList2 = null;
        List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(this.mPackageManager);
        if (this.mFullBackupScheduleFile.exists()) {
            Throwable th3 = null;
            FileInputStream fileInputStream = null;
            BufferedInputStream bufferedInputStream = null;
            DataInputStream dataInputStream = null;
            try {
                FileInputStream fstream = new FileInputStream(this.mFullBackupScheduleFile);
                try {
                    BufferedInputStream bufStream = new BufferedInputStream(fstream);
                    try {
                        DataInputStream dataInputStream2 = new DataInputStream(bufStream);
                        try {
                            int version = dataInputStream2.readInt();
                            if (version != 1) {
                                Slog.e(TAG, "Unknown backup schedule version " + version);
                                if (dataInputStream2 != null) {
                                    try {
                                        dataInputStream2.close();
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                th = null;
                                if (bufStream != null) {
                                    try {
                                        bufStream.close();
                                    } catch (Throwable th5) {
                                        th3 = th5;
                                        if (th != null) {
                                            if (th != th3) {
                                                th.addSuppressed(th3);
                                                th3 = th;
                                            }
                                        }
                                    }
                                }
                                th3 = th;
                                if (fstream != null) {
                                    try {
                                        fstream.close();
                                    } catch (Throwable th6) {
                                        th = th6;
                                        if (th3 != null) {
                                            if (th3 != th) {
                                                th3.addSuppressed(th);
                                                th = th3;
                                            }
                                        }
                                    }
                                }
                                th = th3;
                                if (th == null) {
                                    return null;
                                }
                                try {
                                    throw th;
                                } catch (Exception e2) {
                                    e = e2;
                                    dataInputStream = dataInputStream2;
                                    fileInputStream = fstream;
                                }
                            } else {
                                int N = dataInputStream2.readInt();
                                arrayList = new ArrayList(N);
                                try {
                                    HashSet<String> foundApps = new HashSet(N);
                                    int i = 0;
                                    while (i < N) {
                                        String pkgName = dataInputStream2.readUTF();
                                        long lastBackup = dataInputStream2.readLong();
                                        foundApps.add(pkgName);
                                        try {
                                            PackageInfo pkg = this.mPackageManager.getPackageInfo(pkgName, 0);
                                            if (AppBackupUtils.appGetsFullBackup(pkg) && AppBackupUtils.appIsEligibleForBackup(pkg.applicationInfo)) {
                                                arrayList.add(new FullBackupEntry(pkgName, lastBackup));
                                                i++;
                                            } else {
                                                Slog.i(TAG, "Package " + pkgName + " no longer eligible for full backup");
                                                i++;
                                            }
                                        } catch (NameNotFoundException e3) {
                                            Slog.i(TAG, "Package " + pkgName + " not installed; dropping from full backup");
                                        }
                                    }
                                    for (PackageInfo app : apps) {
                                        if (AppBackupUtils.appGetsFullBackup(app) && AppBackupUtils.appIsEligibleForBackup(app.applicationInfo) && !foundApps.contains(app.packageName)) {
                                            arrayList.add(new FullBackupEntry(app.packageName, 0));
                                            changed = true;
                                        }
                                    }
                                    Collections.sort(arrayList);
                                    if (dataInputStream2 != null) {
                                        try {
                                            dataInputStream2.close();
                                        } catch (Throwable th7) {
                                            th = th7;
                                        }
                                    }
                                    th = null;
                                    if (bufStream != null) {
                                        try {
                                            bufStream.close();
                                        } catch (Throwable th8) {
                                            th3 = th8;
                                            if (th != null) {
                                                if (th != th3) {
                                                    th.addSuppressed(th3);
                                                    th3 = th;
                                                }
                                            }
                                        }
                                    }
                                    th3 = th;
                                    if (fstream != null) {
                                        try {
                                            fstream.close();
                                        } catch (Throwable th9) {
                                            th = th9;
                                            if (th3 != null) {
                                                if (th3 != th) {
                                                    th3.addSuppressed(th);
                                                    th = th3;
                                                }
                                            }
                                        }
                                    }
                                    th = th3;
                                    if (th != null) {
                                        try {
                                            throw th;
                                        } catch (Exception e4) {
                                            e = e4;
                                            dataInputStream = dataInputStream2;
                                            arrayList2 = arrayList;
                                            Slog.e(TAG, "Unable to read backup schedule", e);
                                            this.mFullBackupScheduleFile.delete();
                                            arrayList2 = null;
                                            if (arrayList2 == null) {
                                                changed = true;
                                                arrayList = new ArrayList(apps.size());
                                                for (PackageInfo info : apps) {
                                                    arrayList.add(new FullBackupEntry(info.packageName, 0));
                                                }
                                            }
                                            if (changed) {
                                                writeFullBackupScheduleAsync();
                                            }
                                            return arrayList2;
                                        }
                                    }
                                    arrayList2 = arrayList;
                                } catch (Throwable th10) {
                                    th = th10;
                                    dataInputStream = dataInputStream2;
                                    bufferedInputStream = bufStream;
                                    fileInputStream = fstream;
                                    arrayList2 = arrayList;
                                }
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            dataInputStream = dataInputStream2;
                            bufferedInputStream = bufStream;
                            fileInputStream = fstream;
                            if (dataInputStream != null) {
                                try {
                                    dataInputStream.close();
                                } catch (Throwable th22) {
                                    if (th3 == null) {
                                        th3 = th22;
                                    } else if (th3 != th22) {
                                        th3.addSuppressed(th22);
                                    }
                                }
                            }
                            if (bufferedInputStream != null) {
                                try {
                                    bufferedInputStream.close();
                                } catch (Throwable th12) {
                                    th22 = th12;
                                    if (th3 != null) {
                                        if (th3 != th22) {
                                            th3.addSuppressed(th22);
                                            th22 = th3;
                                        }
                                    }
                                }
                            }
                            th22 = th3;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (Throwable th13) {
                                    th3 = th13;
                                    if (th22 != null) {
                                        if (th22 != th3) {
                                            th22.addSuppressed(th3);
                                            th3 = th22;
                                        }
                                    }
                                }
                            }
                            th3 = th22;
                            if (th3 == null) {
                                throw th;
                            }
                            try {
                                throw th3;
                            } catch (Exception e5) {
                                e = e5;
                            }
                        }
                    } catch (Throwable th14) {
                        th = th14;
                        bufferedInputStream = bufStream;
                        fileInputStream = fstream;
                        if (dataInputStream != null) {
                            dataInputStream.close();
                        }
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        th22 = th3;
                        if (fileInputStream != null) {
                            fileInputStream.close();
                        }
                        th3 = th22;
                        if (th3 == null) {
                            throw th3;
                        } else {
                            throw th;
                        }
                    }
                } catch (Throwable th15) {
                    th = th15;
                    fileInputStream = fstream;
                    if (dataInputStream != null) {
                        dataInputStream.close();
                    }
                    if (bufferedInputStream != null) {
                        bufferedInputStream.close();
                    }
                    th22 = th3;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    th3 = th22;
                    if (th3 == null) {
                        throw th;
                    }
                    throw th3;
                }
            } catch (Throwable th16) {
                th = th16;
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
                th22 = th3;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                th3 = th22;
                if (th3 == null) {
                    throw th3;
                } else {
                    throw th;
                }
            }
        }
        if (arrayList2 == null) {
            changed = true;
            arrayList = new ArrayList(apps.size());
            for (PackageInfo info2 : apps) {
                if (AppBackupUtils.appGetsFullBackup(info2) && AppBackupUtils.appIsEligibleForBackup(info2.applicationInfo)) {
                    arrayList.add(new FullBackupEntry(info2.packageName, 0));
                }
            }
        }
        if (changed) {
            writeFullBackupScheduleAsync();
        }
        return arrayList2;
    }

    private void writeFullBackupScheduleAsync() {
        this.mBackupHandler.removeCallbacks(this.mFullBackupScheduleWriter);
        this.mBackupHandler.post(this.mFullBackupScheduleWriter);
    }

    private void parseLeftoverJournals() {
        for (DataChangedJournal journal : DataChangedJournal.listJournals(this.mJournalDir)) {
            if (!journal.equals(this.mJournal)) {
                try {
                    journal.forEach(new -$Lambda$HVG81oAnYYGYP1QjJ-JXVZWHuJs(this));
                } catch (IOException e) {
                    Slog.e(TAG, "Can't read " + journal, e);
                }
            }
        }
    }

    /* synthetic */ void lambda$-com_android_server_backup_RefactoredBackupManagerService_40916(String packageName) {
        Slog.i(TAG, "Found stale backup journal, scheduling");
        dataChangedImpl(packageName);
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

    public void recordInitPendingLocked(boolean isPending, String transportName) {
        this.mBackupHandler.removeMessages(11);
        try {
            IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
            if (transport != null) {
                File initPendingFile = new File(new File(this.mBaseStateDir, transport.transportDirName()), INIT_SENTINEL_FILE_NAME);
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
                return;
            }
        } catch (Exception e2) {
            Slog.e(TAG, "Transport " + transportName + " failed to report name: " + e2.getMessage());
        }
        if (isPending) {
            this.mPendingInits.add(transportName);
            this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(11, isPending ? 1 : 0, 0, transportName), 3600000);
        }
    }

    public void resetBackupState(File stateFileDir) {
        synchronized (this.mQueueLock) {
            this.mEverStoredApps.clear();
            this.mEverStored.delete();
            this.mCurrentToken = 0;
            writeRestoreTokens();
            for (File sf : stateFileDir.listFiles()) {
                if (!sf.getName().equals(INIT_SENTINEL_FILE_NAME)) {
                    sf.delete();
                }
            }
        }
        synchronized (this.mBackupParticipants) {
            int N = this.mBackupParticipants.size();
            for (int i = 0; i < N; i++) {
                HashSet<String> participants = (HashSet) this.mBackupParticipants.valueAt(i);
                if (participants != null) {
                    for (String packageName : participants) {
                        dataChangedImpl(packageName);
                    }
                }
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
        List<PackageInfo> packages = this.mPackageManager.getInstalledPackages(64);
        int a = packages.size() - 1;
        while (a >= 0) {
            PackageInfo pkg = (PackageInfo) packages.get(a);
            try {
                ApplicationInfo app = pkg.applicationInfo;
                if ((app.flags & 32768) == 0 || app.backupAgentName == null || (app.flags & 67108864) != 0) {
                    packages.remove(a);
                    a--;
                } else {
                    app = this.mPackageManager.getApplicationInfo(pkg.packageName, 1024);
                    pkg.applicationInfo.sharedLibraryFiles = app.sharedLibraryFiles;
                    a--;
                }
            } catch (NameNotFoundException e) {
                packages.remove(a);
            }
        }
        return packages;
    }

    public void logBackupComplete(String packageName) {
        Throwable th;
        Throwable th2 = null;
        if (!packageName.equals(PACKAGE_MANAGER_SENTINEL)) {
            synchronized (this.mEverStoredApps) {
                if (this.mEverStoredApps.add(packageName)) {
                    RandomAccessFile randomAccessFile = null;
                    try {
                        RandomAccessFile out = new RandomAccessFile(this.mEverStored, "rws");
                        try {
                            out.seek(out.length());
                            out.writeUTF(packageName);
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                try {
                                    throw th2;
                                } catch (IOException e) {
                                    randomAccessFile = out;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            randomAccessFile = out;
                            if (randomAccessFile != null) {
                                try {
                                    randomAccessFile.close();
                                } catch (Throwable th5) {
                                    if (th2 == null) {
                                        th2 = th5;
                                    } else if (th2 != th5) {
                                        th2.addSuppressed(th5);
                                    }
                                }
                            }
                            if (th2 == null) {
                                try {
                                    throw th2;
                                } catch (IOException e2) {
                                }
                            } else {
                                throw th;
                            }
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        if (randomAccessFile != null) {
                            randomAccessFile.close();
                        }
                        if (th2 == null) {
                            throw th;
                        } else {
                            throw th2;
                        }
                    }
                }
                return;
            }
        }
        return;
        Slog.e(TAG, "Can't log backup of " + packageName + " to " + this.mEverStored);
    }

    void removeEverBackedUp(String packageName) {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        Slog.v(TAG, "Removing backed-up knowledge of " + packageName);
        synchronized (this.mEverStoredApps) {
            File tempKnownFile = new File(this.mBaseStateDir, "processed.new");
            RandomAccessFile randomAccessFile = null;
            try {
                RandomAccessFile known = new RandomAccessFile(tempKnownFile, "rws");
                try {
                    this.mEverStoredApps.remove(packageName);
                    for (String s : this.mEverStoredApps) {
                        known.writeUTF(s);
                    }
                    known.close();
                    if (tempKnownFile.renameTo(this.mEverStored)) {
                        if (known != null) {
                            try {
                                known.close();
                            } catch (Throwable th3) {
                                th2 = th3;
                            }
                        }
                        if (th2 != null) {
                            try {
                                throw th2;
                            } catch (IOException e2) {
                                e = e2;
                                randomAccessFile = known;
                            }
                        }
                    } else {
                        throw new IOException("Can't rename " + tempKnownFile + " to " + this.mEverStored);
                    }
                } catch (Throwable th4) {
                    th = th4;
                    randomAccessFile = known;
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (IOException e3) {
                            e = e3;
                        }
                    } else {
                        throw th;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (th2 == null) {
                    throw th;
                } else {
                    throw th2;
                }
            }
        }
        Slog.w(TAG, "Error rewriting " + this.mEverStored, e);
        this.mEverStoredApps.clear();
        tempKnownFile.delete();
        this.mEverStored.delete();
    }

    public void writeRestoreTokens() {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile af = new RandomAccessFile(this.mTokenFile, "rwd");
            try {
                af.writeInt(1);
                af.writeLong(this.mAncestralToken);
                af.writeLong(this.mCurrentToken);
                if (this.mAncestralPackages == null) {
                    af.writeInt(-1);
                } else {
                    af.writeInt(this.mAncestralPackages.size());
                    Slog.v(TAG, "Ancestral packages:  " + this.mAncestralPackages.size());
                    for (String pkgName : this.mAncestralPackages) {
                        af.writeUTF(pkgName);
                    }
                }
                if (af != null) {
                    try {
                        af.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 != null) {
                    try {
                        throw th2;
                    } catch (IOException e2) {
                        e = e2;
                        randomAccessFile = af;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                randomAccessFile = af;
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e3) {
                    e = e3;
                    Slog.w(TAG, "Unable to write token file:", e);
                }
            }
        } catch (Throwable th6) {
            th = th6;
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
            if (th2 == null) {
                throw th2;
            }
            throw th;
        }
    }

    private String getTransportName(IBackupTransport transport) {
        return this.mTransportManager.getTransportName(transport);
    }

    public IBackupAgent bindToAgentSynchronous(ApplicationInfo app, int mode) {
        IBackupAgent agent = null;
        synchronized (this.mAgentConnectLock) {
            this.mConnecting = true;
            this.mConnectedAgent = null;
            if (this.mActivityManager.bindBackupAgent(app.packageName, mode, 0)) {
                Slog.d(TAG, "awaiting agent for " + app);
                long timeoutMark = System.currentTimeMillis() + 10000;
                while (this.mConnecting && this.mConnectedAgent == null && System.currentTimeMillis() < timeoutMark) {
                    try {
                        this.mAgentConnectLock.wait(5000);
                    } catch (InterruptedException e) {
                        try {
                            Slog.w(TAG, "Interrupted: " + e);
                            this.mConnecting = false;
                            this.mConnectedAgent = null;
                        } catch (RemoteException e2) {
                        }
                    }
                }
                if (this.mConnecting) {
                    Slog.w(TAG, "Timeout waiting for agent " + app);
                    this.mConnectedAgent = null;
                }
                Slog.i(TAG, "got agent " + this.mConnectedAgent);
                agent = this.mConnectedAgent;
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

    public void clearApplicationDataSynchronous(String packageName) {
        try {
            if ((this.mPackageManager.getPackageInfo(packageName, 0).applicationInfo.flags & 64) != 0) {
                ClearDataObserver observer = new ClearDataObserver(this);
                synchronized (this.mClearDataLock) {
                    this.mClearingData = true;
                    try {
                        this.mActivityManager.clearApplicationUserData(packageName, observer, 0);
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
            Slog.w(TAG, "Tried to clear data for " + packageName + " but not found");
        }
    }

    public long getAvailableRestoreToken(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreToken");
        long token = this.mAncestralToken;
        synchronized (this.mQueueLock) {
            if (this.mEverStoredApps.contains(packageName)) {
                token = this.mCurrentToken;
            }
        }
        return token;
    }

    public int requestBackup(String[] packages, IBackupObserver observer, int flags) {
        return requestBackup(packages, observer, null, flags);
    }

    public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "requestBackup");
        if (packages == null || packages.length < 1) {
            Slog.e(TAG, "No packages named for backup request");
            BackupObserverUtils.sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            monitor = BackupManagerMonitorUtils.monitorEvent(monitor, 49, null, 1, null);
            throw new IllegalArgumentException("No packages are provided for backup");
        } else if (this.mEnabled && (this.mProvisioned ^ 1) == 0) {
            IBackupTransport transport = this.mTransportManager.getCurrentTransportBinder();
            if (transport == null) {
                BackupObserverUtils.sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                monitor = BackupManagerMonitorUtils.monitorEvent(monitor, 50, null, 1, null);
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            ArrayList<String> fullBackupList = new ArrayList();
            ArrayList<String> kvBackupList = new ArrayList();
            for (String packageName : packages) {
                if (PACKAGE_MANAGER_SENTINEL.equals(packageName)) {
                    kvBackupList.add(packageName);
                } else {
                    try {
                        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 64);
                        if (!AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo)) {
                            BackupObserverUtils.sendBackupOnPackageResult(observer, packageName, -2001);
                        } else if (AppBackupUtils.appGetsFullBackup(packageInfo)) {
                            fullBackupList.add(packageInfo.packageName);
                        } else {
                            kvBackupList.add(packageInfo.packageName);
                        }
                    } catch (NameNotFoundException e) {
                        BackupObserverUtils.sendBackupOnPackageResult(observer, packageName, -2002);
                    }
                }
            }
            EventLog.writeEvent(EventLogTags.BACKUP_REQUESTED, new Object[]{Integer.valueOf(packages.length), Integer.valueOf(kvBackupList.size()), Integer.valueOf(fullBackupList.size())});
            try {
                String dirName = transport.transportDirName();
                boolean nonIncrementalBackup = (flags & 1) != 0;
                Message msg = this.mBackupHandler.obtainMessage(15);
                msg.obj = new BackupParams(transport, dirName, kvBackupList, fullBackupList, observer, monitor, true, nonIncrementalBackup);
                this.mBackupHandler.sendMessage(msg);
                return 0;
            } catch (Exception e2) {
                Slog.e(TAG, "Transport unavailable while attempting backup: " + e2.getMessage());
                BackupObserverUtils.sendBackupFinished(observer, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
        } else {
            int logTag;
            Slog.i(TAG, "Backup requested but e=" + this.mEnabled + " p=" + this.mProvisioned);
            BackupObserverUtils.sendBackupFinished(observer, -2001);
            if (this.mProvisioned) {
                logTag = 13;
            } else {
                logTag = 14;
            }
            monitor = BackupManagerMonitorUtils.monitorEvent(monitor, logTag, null, 3, null);
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
            KeyValueBackupJob.schedule(this.mContext, 3600000);
            FullBackupJob.schedule(this.mContext, 7200000);
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
        Slog.wtf(TAG, "prepareOperationTimeout() doesn't support operation " + Integer.toHexString(token) + " of type " + operationType);
    }

    private int getMessageIdForOperationType(int operationType) {
        switch (operationType) {
            case 0:
                return 17;
            case 1:
                return 18;
            default:
                Slog.wtf(TAG, "getMessageIdForOperationType called on invalid operation type: " + operationType);
                return -1;
        }
    }

    public void removeOperation(int token) {
        synchronized (this.mCurrentOpLock) {
            if (this.mCurrentOperations.get(token) == null) {
                Slog.w(TAG, "Duplicate remove for operation. token=" + Integer.toHexString(token));
            }
            this.mCurrentOperations.remove(token);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean waitUntilOperationComplete(int token) {
        int finalState = 0;
        synchronized (this.mCurrentOpLock) {
            while (true) {
                Operation op = (Operation) this.mCurrentOperations.get(token);
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
        synchronized (this.mCurrentOpLock) {
            Operation op = (Operation) this.mCurrentOperations.get(token);
            int state = op != null ? op.state : -1;
            if (state == 1) {
                Slog.w(TAG, "Operation already got an ack.Should have been removed from mCurrentOperations.");
                op = null;
                this.mCurrentOperations.delete(token);
            } else if (state == 0) {
                Slog.v(TAG, "Cancel: token=" + Integer.toHexString(token));
                op.state = -1;
                this.mBackupHandler.removeMessages(getMessageIdForOperationType(op.type));
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
                if (app.uid >= 10000 && (app.packageName.equals("com.android.backupconfirm") ^ 1) != 0) {
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
            if (this.mStorageManager.getEncryptionState() == 1) {
                z = false;
            } else if (this.mStorageManager.getPasswordType() == 1) {
                z = false;
            }
            return z;
        } catch (Exception e) {
            Slog.e(TAG, "Unable to communicate with storagemanager service: " + e.getMessage());
            return true;
        }
    }

    public void scheduleNextFullBackupJob(long transportMinLatency) {
        synchronized (this.mQueueLock) {
            if (this.mFullBackupQueue.size() > 0) {
                long timeSinceLast = System.currentTimeMillis() - ((FullBackupEntry) this.mFullBackupQueue.get(0)).lastBackup;
                final long latency = Math.max(transportMinLatency, timeSinceLast < 86400000 ? 86400000 - timeSinceLast : 0);
                this.mBackupHandler.postDelayed(new Runnable() {
                    public void run() {
                        FullBackupJob.schedule(RefactoredBackupManagerService.this.mContext, latency);
                    }
                }, 2500);
            } else {
                Slog.i(TAG, "Full backup queue empty; not scheduling");
            }
        }
    }

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

    private boolean fullBackupAllowable(IBackupTransport transport) {
        if (transport == null) {
            Slog.w(TAG, "Transport not present; full data backup not performed");
            return false;
        }
        try {
            if (new File(new File(this.mBaseStateDir, transport.transportDirName()), PACKAGE_MANAGER_SENTINEL).length() > 0) {
                return true;
            }
            Slog.i(TAG, "Full backup requested but dataset not yet initialized");
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "Unable to get transport name: " + e.getMessage());
            return false;
        }
    }

    public boolean beginFullBackup(FullBackupJob scheduledJob) {
        long now = System.currentTimeMillis();
        FullBackupEntry entry = null;
        long latency = 86400000;
        if (!this.mEnabled || (this.mProvisioned ^ 1) != 0) {
            return false;
        }
        if (this.mPowerManager.getPowerSaveState(4).batterySaverEnabled) {
            Slog.i(TAG, "Deferring scheduled full backups in battery saver mode");
            FullBackupJob.schedule(this.mContext, 14400000);
            return false;
        }
        Slog.i(TAG, "Beginning scheduled full backup operation");
        synchronized (this.mQueueLock) {
            if (this.mRunningFullBackupTask != null) {
                Slog.e(TAG, "Backup triggered but one already/still running!");
                return false;
            }
            boolean runBackup = true;
            while (this.mFullBackupQueue.size() != 0) {
                boolean headBusy = false;
                if (!fullBackupAllowable(this.mTransportManager.getCurrentTransportBinder())) {
                    runBackup = false;
                    latency = 14400000;
                }
                if (runBackup) {
                    entry = (FullBackupEntry) this.mFullBackupQueue.get(0);
                    long timeSinceRun = now - entry.lastBackup;
                    runBackup = timeSinceRun >= 86400000;
                    if (!runBackup) {
                        latency = 86400000 - timeSinceRun;
                        break;
                    }
                    try {
                        PackageInfo appInfo = this.mPackageManager.getPackageInfo(entry.packageName, 0);
                        if (AppBackupUtils.appGetsFullBackup(appInfo)) {
                            if ((appInfo.applicationInfo.privateFlags & 8192) == 0) {
                                headBusy = this.mActivityManager.isAppForeground(appInfo.applicationInfo.uid);
                            } else {
                                headBusy = false;
                            }
                            if (headBusy) {
                                long nextEligible = (System.currentTimeMillis() + 3600000) + ((long) this.mTokenGenerator.nextInt(BUSY_BACKOFF_FUZZ));
                                Slog.i(TAG, "Full backup time but " + entry.packageName + " is busy; deferring to " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(nextEligible)));
                                enqueueFullBackup(entry.packageName, nextEligible - 86400000);
                            }
                        } else {
                            this.mFullBackupQueue.remove(0);
                            headBusy = true;
                        }
                    } catch (NameNotFoundException e) {
                        runBackup = this.mFullBackupQueue.size() > 1;
                    } catch (RemoteException e2) {
                    }
                }
                if (!headBusy) {
                    break;
                }
            }
            Slog.i(TAG, "Backup queue empty; doing nothing");
            runBackup = false;
            if (runBackup) {
                this.mFullBackupQueue.remove(0);
                this.mRunningFullBackupTask = new PerformFullTransportBackupTask(this, null, new String[]{entry.packageName}, true, scheduledJob, new CountDownLatch(1), null, null, false);
                this.mWakelock.acquire();
                new Thread(this.mRunningFullBackupTask).start();
                return true;
            }
            Slog.i(TAG, "Nothing pending full backup; rescheduling +" + latency);
            final long deferTime = latency;
            this.mBackupHandler.post(new Runnable() {
                public void run() {
                    FullBackupJob.schedule(RefactoredBackupManagerService.this.mContext, deferTime);
                }
            });
            return false;
        }
    }

    public void endFullBackup() {
        synchronized (this.mQueueLock) {
            if (this.mRunningFullBackupTask != null) {
                Slog.i(TAG, "Telling running backup to stop");
                this.mRunningFullBackupTask.handleCancel(true);
            }
        }
    }

    public void restoreWidgetData(String packageName, byte[] widgetData) {
        AppWidgetBackupBridge.restoreWidgetState(packageName, widgetData, 0);
    }

    public void dataChangedImpl(String packageName) {
        dataChangedImpl(packageName, dataChangedTargets(packageName));
    }

    private void dataChangedImpl(String packageName, HashSet<String> targets) {
        if (targets == null) {
            Slog.w(TAG, "dataChanged but no participant pkg='" + packageName + "'" + " uid=" + Binder.getCallingUid());
            return;
        }
        synchronized (this.mQueueLock) {
            if (targets.contains(packageName)) {
                if (this.mPendingBackups.put(packageName, new BackupRequest(packageName)) == null) {
                    writeToJournalLocked(packageName);
                }
            }
        }
        KeyValueBackupJob.schedule(this.mContext);
    }

    private HashSet<String> dataChangedTargets(String packageName) {
        HashSet<String> hashSet;
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
            Slog.e(TAG, "Can't write " + str + " to backup journal", e);
            this.mJournal = null;
        }
    }

    public void dataChanged(final String packageName) {
        if (UserHandle.getCallingUserId() == 0) {
            final HashSet<String> targets = dataChangedTargets(packageName);
            if (targets == null) {
                Slog.w(TAG, "dataChanged but no participant pkg='" + packageName + "'" + " uid=" + Binder.getCallingUid());
            } else {
                this.mBackupHandler.post(new Runnable() {
                    public void run() {
                        RefactoredBackupManagerService.this.dataChangedImpl(packageName, targets);
                    }
                });
            }
        }
    }

    public void initializeTransports(String[] transportNames, IBackupObserver observer) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "initializeTransport");
        Slog.v(TAG, "initializeTransport(): " + Arrays.asList(transportNames));
        long oldId = Binder.clearCallingIdentity();
        try {
            this.mWakelock.acquire();
            this.mBackupHandler.post(new PerformInitializeTask(this, transportNames, observer));
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void clearBackupData(String transportName, String packageName) {
        Slog.v(TAG, "clearBackupData() of " + packageName + " on " + transportName);
        try {
            HashSet<String> apps;
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 64);
            if (this.mContext.checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid()) == -1) {
                apps = (HashSet) this.mBackupParticipants.get(Binder.getCallingUid());
            } else {
                apps = SparseArrayUtils.union(this.mBackupParticipants);
            }
            if (apps.contains(packageName)) {
                this.mBackupHandler.removeMessages(12);
                synchronized (this.mQueueLock) {
                    IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
                    if (transport == null) {
                        this.mBackupHandler.sendMessageDelayed(this.mBackupHandler.obtainMessage(12, new ClearRetryParams(transportName, packageName)), 3600000);
                        return;
                    }
                    long oldId = Binder.clearCallingIdentity();
                    this.mWakelock.acquire();
                    this.mBackupHandler.sendMessage(this.mBackupHandler.obtainMessage(4, new ClearParams(transport, info)));
                    Binder.restoreCallingIdentity(oldId);
                }
            }
        } catch (NameNotFoundException e) {
            Slog.d(TAG, "No such package '" + packageName + "' - not clearing backup data");
        }
    }

    public void backupNow() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "backupNow");
        if (this.mPowerManager.getPowerSaveState(5).batterySaverEnabled) {
            Slog.v(TAG, "Not running backup while in battery save mode");
            KeyValueBackupJob.schedule(this.mContext);
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
        if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            return true;
        }
        return false;
    }

    public void adbBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean compress, boolean doKeyValue, String[] pkgList) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbBackup");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Backup supported only for the device owner");
        } else if (doAllApps || includeShared || !(pkgList == null || pkgList.length == 0)) {
            long oldId = Binder.clearCallingIdentity();
            try {
                if (deviceIsProvisioned()) {
                    Slog.v(TAG, "Requesting backup: apks=" + includeApks + " obb=" + includeObbs + " shared=" + includeShared + " all=" + doAllApps + " system=" + includeSystem + " includekeyvalue=" + doKeyValue + " pkgs=" + pkgList);
                    Slog.i(TAG, "Beginning adb backup...");
                    AdbBackupParams params = new AdbBackupParams(fd, includeApks, includeObbs, includeShared, doWidgets, doAllApps, includeSystem, compress, doKeyValue, pkgList);
                    int token = generateRandomIntegerToken();
                    synchronized (this.mAdbBackupRestoreConfirmations) {
                        this.mAdbBackupRestoreConfirmations.put(token, params);
                    }
                    Slog.d(TAG, "Starting backup confirmation UI, token=" + token);
                    if (startConfirmationUi(token, "fullback")) {
                        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
                        startConfirmationTimeout(token, params);
                        Slog.d(TAG, "Waiting for backup completion...");
                        waitForCompletion(params);
                        try {
                            fd.close();
                        } catch (IOException e) {
                            Slog.e(TAG, "IO error closing output for adb backup: " + e.getMessage());
                        }
                        Binder.restoreCallingIdentity(oldId);
                        Slog.d(TAG, "Adb backup processing complete.");
                        return;
                    }
                    Slog.e(TAG, "Unable to launch backup confirmation UI");
                    this.mAdbBackupRestoreConfirmations.delete(token);
                    try {
                        fd.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "IO error closing output for adb backup: " + e2.getMessage());
                    }
                    Binder.restoreCallingIdentity(oldId);
                    Slog.d(TAG, "Adb backup processing complete.");
                    return;
                }
                Slog.i(TAG, "Backup not supported before setup");
            } finally {
                try {
                    fd.close();
                } catch (IOException e22) {
                    Slog.e(TAG, "IO error closing output for adb backup: " + e22.getMessage());
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.d(TAG, "Adb backup processing complete.");
            }
        } else {
            throw new IllegalArgumentException("Backup requested but neither shared nor any apps named");
        }
    }

    public void fullTransportBackup(String[] pkgNames) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "fullTransportBackup");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        if (fullBackupAllowable(this.mTransportManager.getCurrentTransportBinder())) {
            Slog.d(TAG, "fullTransportBackup()");
            long oldId = Binder.clearCallingIdentity();
            try {
                CountDownLatch latch = new CountDownLatch(1);
                PerformFullTransportBackupTask task = new PerformFullTransportBackupTask(this, null, pkgNames, false, null, latch, null, null, false);
                this.mWakelock.acquire();
                new Thread(task, "full-transport-master").start();
                while (true) {
                    try {
                        latch.await();
                        break;
                    } catch (InterruptedException e) {
                    }
                }
                long now = System.currentTimeMillis();
                for (String pkg : pkgNames) {
                    enqueueFullBackup(pkg, now);
                }
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } else {
            Slog.i(TAG, "Full backup not currently possible -- key/value backup not yet run?");
        }
        Slog.d(TAG, "Done with full transport backup.");
    }

    public void adbRestore(ParcelFileDescriptor fd) {
        this.mContext.enforceCallingPermission("android.permission.BACKUP", "adbRestore");
        if (UserHandle.getCallingUserId() != 0) {
            throw new IllegalStateException("Restore supported only for the device owner");
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            if (deviceIsProvisioned()) {
                Slog.i(TAG, "Beginning restore...");
                AdbRestoreParams params = new AdbRestoreParams(fd);
                int token = generateRandomIntegerToken();
                synchronized (this.mAdbBackupRestoreConfirmations) {
                    this.mAdbBackupRestoreConfirmations.put(token, params);
                }
                Slog.d(TAG, "Starting restore confirmation UI, token=" + token);
                if (startConfirmationUi(token, "fullrest")) {
                    this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
                    startConfirmationTimeout(token, params);
                    Slog.d(TAG, "Waiting for restore completion...");
                    waitForCompletion(params);
                    try {
                        fd.close();
                    } catch (IOException e) {
                        Slog.w(TAG, "Error trying to close fd after adb restore: " + e);
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
                    Slog.w(TAG, "Error trying to close fd after adb restore: " + e2);
                }
                Binder.restoreCallingIdentity(oldId);
                Slog.i(TAG, "adb restore processing complete.");
                return;
            }
            Slog.i(TAG, "Full restore not permitted before setup");
        } finally {
            try {
                fd.close();
            } catch (IOException e22) {
                Slog.w(TAG, "Error trying to close fd after adb restore: " + e22);
            }
            Binder.restoreCallingIdentity(oldId);
            Slog.i(TAG, "adb restore processing complete.");
        }
    }

    private boolean startConfirmationUi(int token, String action) {
        try {
            Intent confIntent = new Intent(action);
            confIntent.setClassName("com.android.backupconfirm", "com.android.backupconfirm.BackupRestoreConfirmation");
            confIntent.putExtra("conftoken", token);
            confIntent.addFlags(268435456);
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
        Slog.d(TAG, "acknowledgeAdbBackupOrRestore : token=" + token + " allow=" + allow);
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
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    private static boolean backupSettingMigrated(int userId) {
        return new File(new File(Environment.getDataDirectory(), "backup"), BACKUP_ENABLE_FILE).exists();
    }

    private static boolean readBackupEnableState(int userId) {
        Throwable th;
        Throwable th2 = null;
        File enableFile = new File(new File(Environment.getDataDirectory(), "backup"), BACKUP_ENABLE_FILE);
        if (enableFile.exists()) {
            FileInputStream fileInputStream = null;
            try {
                FileInputStream fin = new FileInputStream(enableFile);
                try {
                    boolean z;
                    if (fin.read() != 0) {
                        z = true;
                    } else {
                        z = false;
                    }
                    if (fin != null) {
                        try {
                            fin.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 == null) {
                        return z;
                    }
                    try {
                        throw th2;
                    } catch (IOException e) {
                        fileInputStream = fin;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    fileInputStream = fin;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        try {
                            throw th2;
                        } catch (IOException e2) {
                            Slog.e(TAG, "Cannot read enable state; assuming disabled");
                            return false;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (th2 == null) {
                    throw th;
                }
                throw th2;
            }
        }
        Slog.i(TAG, "isBackupEnabled() => false due to absent settings file");
        return false;
    }

    private static void writeBackupEnableState(boolean enable, int userId) {
        Throwable th;
        Exception e;
        Throwable th2;
        File base = new File(Environment.getDataDirectory(), "backup");
        File enableFile = new File(base, BACKUP_ENABLE_FILE);
        File stage = new File(base, "backup_enabled-stage");
        FileOutputStream fileOutputStream = null;
        try {
            FileOutputStream fout = new FileOutputStream(stage);
            try {
                fout.write(enable ? 1 : 0);
                fout.close();
                stage.renameTo(enableFile);
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                th = null;
                if (th != null) {
                    try {
                        throw th;
                    } catch (IOException e2) {
                        e = e2;
                        fileOutputStream = fout;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                fileOutputStream = fout;
                th2 = null;
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                try {
                    throw th2;
                } catch (IOException e3) {
                    e = e3;
                    Slog.e(TAG, "Unable to record backup enable state; reverting to disabled: " + e.getMessage());
                    Secure.putStringForUser(sInstance.mContext.getContentResolver(), BACKUP_ENABLE_FILE, null, userId);
                    enableFile.delete();
                    stage.delete();
                }
            }
        } catch (Throwable th6) {
            th = th6;
            th2 = null;
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (th2 == null) {
                throw th2;
            }
            throw th;
        }
    }

    public void setBackupEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setBackupEnabled");
        Slog.i(TAG, "Backup enabled => " + enable);
        long oldId = Binder.clearCallingIdentity();
        try {
            boolean wasEnabled = this.mEnabled;
            synchronized (this) {
                writeBackupEnableState(enable, 0);
                this.mEnabled = enable;
            }
            synchronized (this.mQueueLock) {
                if (enable && (wasEnabled ^ 1) != 0) {
                    if (this.mProvisioned) {
                        KeyValueBackupJob.schedule(this.mContext);
                        scheduleNextFullBackupJob(0);
                    }
                }
                if (!enable) {
                    KeyValueBackupJob.cancel(this.mContext);
                    if (wasEnabled && this.mProvisioned) {
                        for (String transport : this.mTransportManager.getBoundTransportNames()) {
                            recordInitPendingLocked(true, transport);
                        }
                        this.mAlarmManager.set(0, System.currentTimeMillis(), this.mRunInitIntent);
                    }
                }
            }
            Binder.restoreCallingIdentity(oldId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void setAutoRestore(boolean doAutoRestore) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "setAutoRestore");
        Slog.i(TAG, "Auto restore => " + doAutoRestore);
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                Secure.putInt(this.mContext.getContentResolver(), "backup_auto_restore", doAutoRestore ? 1 : 0);
                this.mAutoRestore = doAutoRestore;
            }
        } finally {
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
        return this.mTransportManager.getBoundTransportNames();
    }

    public ComponentName[] listAllTransportComponents() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "listAllTransportComponents");
        return this.mTransportManager.getAllTransportCompenents();
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

    public String selectBackupTransport(String transport) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransport");
        long oldId = Binder.clearCallingIdentity();
        try {
            String prevTransport = this.mTransportManager.selectTransport(transport);
            Secure.putString(this.mContext.getContentResolver(), "backup_transport", transport);
            Slog.v(TAG, "selectBackupTransport() set " + this.mTransportManager.getCurrentTransportName() + " returning " + prevTransport);
            return prevTransport;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public void selectBackupTransportAsync(final ComponentName transport, final ISelectBackupTransportCallback listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "selectBackupTransportAsync");
        long oldId = Binder.clearCallingIdentity();
        Slog.v(TAG, "selectBackupTransportAsync() called with transport " + transport.flattenToShortString());
        this.mTransportManager.ensureTransportReady(transport, new SelectBackupTransportCallback() {
            public void onSuccess(String transportName) {
                RefactoredBackupManagerService.this.mTransportManager.selectTransport(transportName);
                Secure.putString(RefactoredBackupManagerService.this.mContext.getContentResolver(), "backup_transport", RefactoredBackupManagerService.this.mTransportManager.getCurrentTransportName());
                Slog.v(RefactoredBackupManagerService.TAG, "Transport successfully selected: " + transport.flattenToShortString());
                try {
                    listener.onSuccess(transportName);
                } catch (RemoteException e) {
                }
            }

            public void onFailure(int reason) {
                Slog.v(RefactoredBackupManagerService.TAG, "Failed to select transport: " + transport.flattenToShortString());
                try {
                    listener.onFailure(reason);
                } catch (RemoteException e) {
                }
            }
        });
        Binder.restoreCallingIdentity(oldId);
    }

    public Intent getConfigurationIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getConfigurationIntent");
        IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
        if (transport != null) {
            try {
                return transport.configurationIntent();
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get configuration intent from transport: " + e.getMessage());
            }
        }
        return null;
    }

    public String getDestinationString(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDestinationString");
        IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
        if (transport != null) {
            try {
                return transport.currentDestinationString();
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get string from transport: " + e.getMessage());
            }
        }
        return null;
    }

    public Intent getDataManagementIntent(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementIntent");
        IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
        if (transport != null) {
            try {
                return transport.dataManagementIntent();
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get management intent from transport: " + e.getMessage());
            }
        }
        return null;
    }

    public String getDataManagementLabel(String transportName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "getDataManagementLabel");
        IBackupTransport transport = this.mTransportManager.getTransportBinder(transportName);
        if (transport != null) {
            try {
                return transport.dataManagementLabel();
            } catch (Exception e) {
                Slog.e(TAG, "Unable to get management label from transport: " + e.getMessage());
            }
        }
        return null;
    }

    public void agentConnected(String packageName, IBinder agentBinder) {
        synchronized (this.mAgentConnectLock) {
            if (Binder.getCallingUid() == 1000) {
                Slog.d(TAG, "agentConnected pkg=" + packageName + " agent=" + agentBinder);
                this.mConnectedAgent = IBackupAgent.Stub.asInterface(agentBinder);
                this.mConnecting = false;
            } else {
                Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent connected");
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
                Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " claiming agent disconnected");
            }
            this.mAgentConnectLock.notifyAll();
        }
    }

    public void restoreAtInstall(String packageName, int token) {
        if (Binder.getCallingUid() != 1000) {
            Slog.w(TAG, "Non-system process uid=" + Binder.getCallingUid() + " attemping install-time restore");
            return;
        }
        boolean skip = false;
        long restoreSet = getAvailableRestoreToken(packageName);
        Slog.v(TAG, "restoreAtInstall pkg=" + packageName + " token=" + Integer.toHexString(token) + " restoreSet=" + Long.toHexString(restoreSet));
        if (restoreSet == 0) {
            skip = true;
        }
        IBackupTransport transport = this.mTransportManager.getCurrentTransportBinder();
        if (transport == null) {
            Slog.w(TAG, "No transport");
            skip = true;
        }
        if (!this.mAutoRestore) {
            Slog.w(TAG, "Non-restorable state: auto=" + this.mAutoRestore);
            skip = true;
        }
        if (!skip) {
            try {
                String dirName = transport.transportDirName();
                this.mWakelock.acquire();
                Message msg = this.mBackupHandler.obtainMessage(3);
                msg.obj = new RestoreParams(transport, dirName, null, null, restoreSet, packageName, token);
                this.mBackupHandler.sendMessage(msg);
            } catch (Exception e) {
                Slog.e(TAG, "Unable to contact transport: " + e.getMessage());
                skip = true;
            }
        }
        if (skip) {
            Slog.v(TAG, "Finishing install immediately");
            try {
                this.mPackageManagerBinder.finishPackageInstall(token, false);
            } catch (RemoteException e2) {
            }
        }
    }

    public IRestoreSession beginRestoreSession(String packageName, String transport) {
        Slog.v(TAG, "beginRestoreSession: pkg=" + packageName + " transport=" + transport);
        boolean needPermission = true;
        if (transport == null) {
            transport = this.mTransportManager.getCurrentTransportName();
            if (packageName != null) {
                try {
                    if (this.mPackageManager.getPackageInfo(packageName, 0).applicationInfo.uid == Binder.getCallingUid()) {
                        needPermission = false;
                    }
                } catch (NameNotFoundException e) {
                    Slog.w(TAG, "Asked to restore nonexistent pkg " + packageName);
                    throw new IllegalArgumentException("Package " + packageName + " not found");
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
                this.mBackupHandler.sendEmptyMessageDelayed(8, 60000);
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
        synchronized (this.mCurrentOpLock) {
            Operation op = (Operation) this.mCurrentOperations.get(token);
            if (op != null) {
                if (op.state == -1) {
                    op = null;
                    this.mCurrentOperations.delete(token);
                } else if (op.state == 1) {
                    Slog.w(TAG, "Received duplicate ack for token=" + Integer.toHexString(token));
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

    private static boolean appIsDisabled(ApplicationInfo app, PackageManager pm) {
        switch (pm.getApplicationEnabledSetting(app.packageName)) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    public boolean isAppEligibleForBackup(String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isAppEligibleForBackup");
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 64);
            if (!AppBackupUtils.appIsEligibleForBackup(packageInfo.applicationInfo) || AppBackupUtils.appIsStopped(packageInfo.applicationInfo) || appIsDisabled(packageInfo.applicationInfo, this.mPackageManager)) {
                return false;
            }
            IBackupTransport transport = this.mTransportManager.getCurrentTransportBinder();
            if (transport != null) {
                try {
                    return transport.isAppEligibleForBackup(packageInfo, AppBackupUtils.appGetsFullBackup(packageInfo));
                } catch (Exception e) {
                    Slog.e(TAG, "Unable to ask about eligibility: " + e.getMessage());
                }
            }
            return true;
        } catch (NameNotFoundException e2) {
            return false;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            long identityToken = Binder.clearCallingIdentity();
            if (args != null) {
                int i = 0;
                int length = args.length;
                while (i < length) {
                    String arg = args[i];
                    if ("-h".equals(arg)) {
                        pw.println("'dumpsys backup' optional arguments:");
                        pw.println("  -h       : this help text");
                        pw.println("  a[gents] : dump information about defined backup agents");
                        Binder.restoreCallingIdentity(identityToken);
                        return;
                    }
                    try {
                        if ("agents".startsWith(arg)) {
                            dumpAgents(pw);
                            return;
                        }
                        i++;
                    } finally {
                        Binder.restoreCallingIdentity(identityToken);
                    }
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
            pw.println("Backup Manager is " + (this.mEnabled ? "enabled" : "disabled") + " / " + (!this.mProvisioned ? "not " : "") + "provisioned / " + (this.mPendingInits.size() == 0 ? "not " : "") + "pending init");
            pw.println("Auto-restore is " + (this.mAutoRestore ? "enabled" : "disabled"));
            if (this.mBackupRunning) {
                pw.println("Backup currently running");
            }
            pw.println("Last backup pass started: " + this.mLastBackupPass + " (now = " + System.currentTimeMillis() + ')');
            pw.println("  next scheduled: " + KeyValueBackupJob.nextScheduled());
            pw.println("Transport whitelist:");
            for (ComponentName transport : this.mTransportManager.getTransportWhitelist()) {
                pw.print("    ");
                pw.println(transport.flattenToShortString());
            }
            pw.println("Available transports:");
            if (listAllTransports() != null) {
                for (String t : listAllTransports()) {
                    String str;
                    StringBuilder stringBuilder = new StringBuilder();
                    if (t.equals(this.mTransportManager.getCurrentTransportName())) {
                        str = "  * ";
                    } else {
                        str = "    ";
                    }
                    pw.println(stringBuilder.append(str).append(t).toString());
                    try {
                        IBackupTransport transport2 = this.mTransportManager.getTransportBinder(t);
                        File dir = new File(this.mBaseStateDir, transport2.transportDirName());
                        pw.println("       destination: " + transport2.currentDestinationString());
                        pw.println("       intent: " + transport2.configurationIntent());
                        for (File f : dir.listFiles()) {
                            pw.println("       " + f.getName() + " - " + f.length() + " state bytes");
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "Error in transport", e);
                        pw.println("        Error: " + e);
                    }
                }
            }
            pw.println("Pending init: " + this.mPendingInits.size());
            for (String s : this.mPendingInits) {
                pw.println("    " + s);
            }
            synchronized (this.mBackupTrace) {
                if (!this.mBackupTrace.isEmpty()) {
                    pw.println("Most recent backup trace:");
                    for (String s2 : this.mBackupTrace) {
                        pw.println("   " + s2);
                    }
                }
            }
            pw.print("Ancestral: ");
            pw.println(Long.toHexString(this.mAncestralToken));
            pw.print("Current:   ");
            pw.println(Long.toHexString(this.mCurrentToken));
            int N = this.mBackupParticipants.size();
            pw.println("Participants:");
            for (int i = 0; i < N; i++) {
                int uid = this.mBackupParticipants.keyAt(i);
                pw.print("  uid: ");
                pw.println(uid);
                for (String app : (HashSet) this.mBackupParticipants.valueAt(i)) {
                    pw.println("    " + app);
                }
            }
            pw.println("Ancestral packages: " + (this.mAncestralPackages == null ? "none" : Integer.valueOf(this.mAncestralPackages.size())));
            if (this.mAncestralPackages != null) {
                for (String pkg : this.mAncestralPackages) {
                    pw.println("    " + pkg);
                }
            }
            pw.println("Ever backed up: " + this.mEverStoredApps.size());
            for (String pkg2 : this.mEverStoredApps) {
                pw.println("    " + pkg2);
            }
            pw.println("Pending key/value backup: " + this.mPendingBackups.size());
            for (BackupRequest req : this.mPendingBackups.values()) {
                pw.println("    " + req);
            }
            pw.println("Full backup queue:" + this.mFullBackupQueue.size());
            for (FullBackupEntry entry : this.mFullBackupQueue) {
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

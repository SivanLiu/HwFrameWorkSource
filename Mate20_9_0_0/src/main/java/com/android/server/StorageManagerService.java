package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerInternal.ScreenObserver;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.admin.SecurityLog;
import android.app.usage.StorageStatsManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.ObbInfo;
import android.database.ContentObserver;
import android.hdm.HwDeviceManager;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Environment.UserEnvironment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IStoraged;
import android.os.IVold;
import android.os.IVoldListener;
import android.os.IVoldListener.Stub;
import android.os.IVoldTaskListener;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.IObbActionListener;
import android.os.storage.IStorageEventListener;
import android.os.storage.IStorageShutdownObserver;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageManagerInternal.ExternalStorageMountPolicy;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.DataUnit;
import android.util.Flog;
import android.util.Pair;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.os.AppFuseMount;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.HwServiceFactory.IHwStorageManagerService;
import com.android.server.Watchdog.Monitor;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.storage.AppFuseBridge;
import com.android.server.storage.AppFuseBridge.MountScope;
import com.android.server.usage.UnixCalendar;
import huawei.cust.HwCustUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class StorageManagerService extends AbsStorageManagerService implements Monitor, ScreenObserver {
    private static final String ATTR_CREATED_MILLIS = "createdMillis";
    private static final String ATTR_FS_UUID = "fsUuid";
    private static final String ATTR_LAST_BENCH_MILLIS = "lastBenchMillis";
    private static final String ATTR_LAST_TRIM_MILLIS = "lastTrimMillis";
    private static final String ATTR_NICKNAME = "nickname";
    private static final String ATTR_PART_GUID = "partGuid";
    private static final String ATTR_PRIMARY_STORAGE_UUID = "primaryStorageUuid";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USER_FLAGS = "userFlags";
    private static final String ATTR_VERSION = "version";
    private static final int CHECK_VOLUME_COMPLETED = 0;
    private static final int CRYPTO_ALGORITHM_KEY_SIZE = 128;
    public static final String[] CRYPTO_TYPES = new String[]{"password", HealthServiceWrapper.INSTANCE_VENDOR, "pattern", "pin"};
    private static final boolean DEBUG_EVENTS = false;
    private static final boolean DEBUG_OBB = false;
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    public static final boolean DEFAULT_VALUE = true;
    private static final boolean EMULATE_FBE_SUPPORTED = true;
    private static final int H_ABNORMAL_SD_BROADCAST = 13;
    private static final int H_ABORT_IDLE_MAINT = 12;
    private static final int H_BACKUP_DEV_MOUNT = 14;
    private static final int H_DAEMON_CONNECTED = 2;
    private static final int H_FSTRIM = 4;
    private static final int H_INTERNAL_BROADCAST = 7;
    private static final int H_PARTITION_FORGET = 9;
    private static final int H_RESET = 10;
    private static final int H_RUN_IDLE_MAINT = 11;
    private static final int H_SHUTDOWN = 3;
    private static final int H_SYSTEM_READY = 1;
    protected static final int H_VOLUME_BROADCAST = 6;
    private static final int H_VOLUME_MOUNT = 5;
    private static final int H_VOLUME_UNMOUNT = 8;
    private static final int INTERNAL = 0;
    private static final String LAST_FSTRIM_FILE = "last-fstrim";
    private static final int MOVE_STATUS_COPY_FINISHED = 82;
    private static final int OBB_FLUSH_MOUNT_STATE = 5;
    private static final int OBB_MCS_BOUND = 2;
    private static final int OBB_MCS_RECONNECT = 4;
    private static final int OBB_MCS_UNBIND = 3;
    private static final int OBB_RUN_ACTION = 1;
    private static final int PBKDF2_HASH_ROUNDS = 1024;
    private static final String PRIMARYSD = "persist.sys.primarysd";
    public static final String PROPERTIES_PRIVACY_SPACE_HIDSD = "ro.config.hw_privacySpace_hidSd";
    private static final int SDCARD = 1;
    private static final String TAG = "StorageManagerService";
    private static final String TAG_STORAGE_BENCHMARK = "storage_benchmark";
    private static final String TAG_STORAGE_TRIM = "storage_trim";
    private static final String TAG_VOLUME = "volume";
    private static final String TAG_VOLUMES = "volumes";
    private static final int VERSION_ADD_PRIMARY = 2;
    private static final int VERSION_FIX_PRIMARY = 3;
    private static final int VERSION_INIT = 1;
    private static final boolean WATCHDOG_ENABLE = false;
    private static final String ZRAM_ENABLED_PROPERTY = "persist.sys.zram_enabled";
    static StorageManagerService sSelf = null;
    @GuardedBy("mAppFuseLock")
    private AppFuseBridge mAppFuseBridge = null;
    private final Object mAppFuseLock = new Object();
    private volatile boolean mBootCompleted = false;
    private final Callbacks mCallbacks;
    private IMediaContainerService mContainerService = null;
    private final Context mContext;
    private volatile int mCurrentUserId = 0;
    private HwCustMountService mCustMountService = ((HwCustMountService) HwCustUtils.createObj(HwCustMountService.class, new Object[0]));
    private volatile boolean mDaemonConnected = false;
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    @GuardedBy("mLock")
    private ArrayMap<String, CountDownLatch> mDiskScanLatches = new ArrayMap();
    @GuardedBy("mLock")
    protected ArrayMap<String, DiskInfo> mDisks = new ArrayMap();
    protected final Handler mHandler;
    private long mLastMaintenance;
    private final File mLastMaintenanceFile;
    private final IVoldListener mListener = new Stub() {
        public void onLockedDiskAdd() {
            Slog.d(StorageManagerService.TAG, "sdlock IVoldListener onLockedDiskAdd");
            StorageManagerService.sSelf.onLockedDiskAdd();
        }

        public void onLockedDiskRemove() {
            Slog.d(StorageManagerService.TAG, "sdlock IVoldListener onLockedDiskRemove");
            StorageManagerService.sSelf.onLockedDiskRemove();
        }

        public void onDiskCreated(String diskId, int flags) {
            int deviceCode = StorageManagerService.this.getUsbDeviceExInfo();
            synchronized (StorageManagerService.this.mLock) {
                String value = SystemProperties.get("persist.sys.adoptable");
                int i = -1;
                int hashCode = value.hashCode();
                if (hashCode != 464944051) {
                    if (hashCode == 1528363547) {
                        if (value.equals("force_off")) {
                            i = 1;
                        }
                    }
                } else if (value.equals("force_on")) {
                    i = 0;
                }
                switch (i) {
                    case 0:
                        flags |= 1;
                        break;
                    case 1:
                        flags &= -2;
                        break;
                    default:
                        break;
                }
                flags |= 1;
                if ((flags & 8) != 0) {
                    if (deviceCode == 0) {
                        flags |= 64;
                    } else if (deviceCode == 1) {
                        flags |= 128;
                    }
                }
                StorageManagerService.this.mDisks.put(diskId, new DiskInfo(diskId, flags));
            }
            if ((flags & 4) != 0) {
                StorageManagerService.sSelf.onLockedDiskChange();
            } else if ((flags & 8) != 0) {
                StorageManagerService.this.notifyDeviceStateToTelephony("SD", "in", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
        }

        public void onDiskScanned(String diskId) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.get(diskId);
                if (disk != null) {
                    StorageManagerService.this.onDiskScannedLocked(disk);
                }
            }
        }

        public void onDiskMetadataChanged(String diskId, long sizeBytes, String label, String sysPath) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.get(diskId);
                if (disk != null) {
                    disk.size = sizeBytes;
                    disk.label = label;
                    disk.sysPath = sysPath;
                }
            }
        }

        public void onDiskDestroyed(String diskId) {
            synchronized (StorageManagerService.this.mLock) {
                DiskInfo disk = (DiskInfo) StorageManagerService.this.mDisks.remove(diskId);
                if (disk != null) {
                    StorageManagerService.this.mCallbacks.notifyDiskDestroyed(disk);
                }
                if (disk.isUsb()) {
                    StorageManagerService.this.notifyDeviceStateToTelephony("SD", "out", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
            }
        }

        public void onVolumeCreated(String volId, int type, String diskId, String partGuid) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = new VolumeInfo(volId, type, (DiskInfo) StorageManagerService.this.mDisks.get(diskId), partGuid);
                StorageManagerService.this.mVolumes.put(volId, vol);
                StorageManagerService.this.onVolumeCreatedLocked(vol);
            }
        }

        /* JADX WARNING: Missing block: B:11:0x005a, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCheckVolumeCompleted(String volId, String diskId, String partGuid, int isSucc) {
            String str = StorageManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCheckVolumeCompleted :  volId = ");
            stringBuilder.append(volId);
            stringBuilder.append(",diskId = ");
            stringBuilder.append(diskId);
            stringBuilder.append(",partGuid = ");
            stringBuilder.append(partGuid);
            stringBuilder.append(",isSucc = ");
            stringBuilder.append(isSucc);
            Slog.i(str, stringBuilder.toString());
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol == null) {
                } else if (isSucc == 0) {
                    StorageManagerService.this.onCheckVolumeCompletedLocked(vol);
                } else {
                    int oldState = vol.state;
                    vol.state = 6;
                    StorageManagerService.this.mCallbacks.notifyVolumeStateChanged(vol, oldState, 6);
                }
            }
        }

        public void onVolumeStateChanged(String volId, int state) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    int oldState = vol.state;
                    int newState = state;
                    vol.state = newState;
                    StorageManagerService.this.onVolumeStateChangedLocked(vol, oldState, newState);
                }
            }
        }

        public void onVolumeMetadataChanged(String volId, String fsType, String fsUuid, String fsLabel) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.fsType = fsType;
                    vol.fsUuid = fsUuid;
                    vol.fsLabel = fsLabel;
                }
            }
        }

        public void onVolumePathChanged(String volId, String path) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.path = path;
                }
            }
        }

        public void onVolumeInternalPathChanged(String volId, String internalPath) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    vol.internalPath = internalPath;
                }
            }
        }

        public void onVolumeDestroyed(String volId) {
            synchronized (StorageManagerService.this.mLock) {
                StorageManagerService.this.mVolumes.remove(volId);
            }
        }

        public void onSdHealthReport(String volId, int newState) {
            synchronized (StorageManagerService.this.mLock) {
                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.get(volId);
                if (vol != null) {
                    int oldState = vol.state;
                    Intent intent = new Intent("com.huawei.storage.MEDIA_ABNORMAL_SD");
                    intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", vol);
                    intent.putExtra("android.os.storage.extra.VOLUME_OLD_STATE", oldState);
                    intent.putExtra("android.os.storage.extra.VOLUME_NEW_STATE", newState);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AbNormal SD card volumeInfo = ");
                    stringBuilder.append(vol.toString());
                    stringBuilder.append(",errorCode = ");
                    stringBuilder.append(newState);
                    Flog.i(1002, stringBuilder.toString());
                    StorageManagerService.this.mHandler.obtainMessage(13, intent).sendToTarget();
                }
            }
        }

        public void onCryptsdMessage(String message) {
            String str = StorageManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sdcrypt IVoldListener onCryptsdMessage");
            stringBuilder.append(message);
            Slog.d(str, stringBuilder.toString());
            StorageManagerService.sSelf.onCryptsdMessage(message);
        }
    };
    @GuardedBy("mLock")
    protected int[] mLocalUnlockedUsers = EmptyArray.INT;
    protected final Object mLock = LockGuard.installNewLock(4);
    private final LockPatternUtils mLockPatternUtils;
    @GuardedBy("mLock")
    private IPackageMoveObserver mMoveCallback;
    @GuardedBy("mLock")
    private String mMoveTargetUuid;
    @GuardedBy("mAppFuseLock")
    private int mNextAppFuseName = 0;
    private final ObbActionHandler mObbActionHandler;
    private final Map<IBinder, List<ObbState>> mObbMounts = new HashMap();
    private final Map<String, ObbState> mObbPathToStateMap = new HashMap();
    private PackageManagerService mPms;
    @GuardedBy("mLock")
    private String mPrimaryStorageUuid;
    @GuardedBy("mLock")
    private ArrayMap<String, VolumeRecord> mRecords = new ArrayMap();
    private BroadcastReceiver mScreenAndDreamingReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action) || "android.intent.action.DREAMING_STARTED".equals(action)) {
                FstrimServiceIdler.setScreenOn(false);
                FstrimServiceIdler.scheduleFstrim(StorageManagerService.this.mContext);
            } else if ("android.intent.action.SCREEN_ON".equals(action) || "android.intent.action.DREAMING_STOPPED".equals(action)) {
                FstrimServiceIdler.setScreenOn(true);
                FstrimServiceIdler.cancelFstrim(StorageManagerService.this.mContext);
                StorageManagerService.this.abortIdleMaint(null);
            }
        }
    };
    private volatile boolean mSecureKeyguardShowing = true;
    private final AtomicFile mSettingsFile;
    private final StorageManagerInternalImpl mStorageManagerInternal = new StorageManagerInternalImpl(this, null);
    private volatile IStoraged mStoraged;
    private volatile boolean mSystemReady = false;
    @GuardedBy("mLock")
    protected int[] mSystemUnlockedUsers = EmptyArray.INT;
    private final BroadcastReceiver mToVoldBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals("android.intent.action.BOOT_COMPLETED")) {
                StorageManagerService.this.bootCompleteToVold();
            }
        }
    };
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            int i = 0;
            Preconditions.checkArgument(userId >= 0);
            try {
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    UserManager um = (UserManager) StorageManagerService.this.mContext.getSystemService(UserManager.class);
                    StorageManagerService.this.mVold.onUserAdded(userId, um.getUserSerialNumber(userId));
                    Slog.i(StorageManagerService.TAG, "add user before getuserinfo");
                    UserInfo userInfo = um.getUserInfo(userId);
                    if (SystemProperties.getBoolean(StorageManagerService.PROPERTIES_PRIVACY_SPACE_HIDSD, true) && userInfo.isHwHiddenSpace()) {
                        synchronized (StorageManagerService.this.mVolumes) {
                            int size = StorageManagerService.this.mVolumes.size();
                            while (i < size) {
                                VolumeInfo vol = (VolumeInfo) StorageManagerService.this.mVolumes.valueAt(i);
                                if (StorageManagerService.this.isExternalSDcard(vol) && vol.isMountedReadable()) {
                                    Slog.i(StorageManagerService.TAG, "begin to send block_id");
                                    vol.blockedUserId = userId;
                                    StorageManagerService.this.mVold.sendBlockUserId(vol.id, vol.blockedUserId);
                                }
                                i++;
                            }
                        }
                    }
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    synchronized (StorageManagerService.this.mVolumes) {
                        UserManager um2 = (UserManager) StorageManagerService.this.mContext.getSystemService(UserManager.class);
                        Slog.i(StorageManagerService.TAG, "remove user before getuserinfo");
                        UserInfo userInfo2 = um2.getUserInfo(userId);
                        int size2 = StorageManagerService.this.mVolumes.size();
                        while (i < size2) {
                            VolumeInfo vol2 = (VolumeInfo) StorageManagerService.this.mVolumes.valueAt(i);
                            if (vol2.mountUserId == userId) {
                                vol2.mountUserId = -10000;
                                StorageManagerService.this.mHandler.obtainMessage(8, vol2).sendToTarget();
                            } else if (SystemProperties.getBoolean(StorageManagerService.PROPERTIES_PRIVACY_SPACE_HIDSD, true) && userInfo2.isHwHiddenSpace() && StorageManagerService.this.isExternalSDcard(vol2) && vol2.isMountedReadable()) {
                                Slog.i(StorageManagerService.TAG, "begin to reset block_id");
                                vol2.blockedUserId = -1;
                                StorageManagerService.this.mVold.sendBlockUserId(vol2.id, vol2.blockedUserId);
                            }
                            i++;
                        }
                    }
                    StorageManagerService.this.mVold.onUserRemoved(userId);
                }
            } catch (Exception e) {
                Slog.wtf(StorageManagerService.TAG, e);
            }
        }
    };
    private boolean mUserStartedFinish = false;
    protected volatile IVold mVold;
    @GuardedBy("mLock")
    protected final ArrayMap<String, VolumeInfo> mVolumes = new ArrayMap();

    private static class Callbacks extends Handler {
        private static final int MSG_DISK_DESTROYED = 6;
        private static final int MSG_DISK_SCANNED = 5;
        private static final int MSG_STORAGE_STATE_CHANGED = 1;
        private static final int MSG_VOLUME_FORGOTTEN = 4;
        private static final int MSG_VOLUME_RECORD_CHANGED = 3;
        private static final int MSG_VOLUME_STATE_CHANGED = 2;
        private final RemoteCallbackList<IStorageEventListener> mCallbacks = new RemoteCallbackList();

        public Callbacks(Looper looper) {
            super(looper);
        }

        public void register(IStorageEventListener callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IStorageEventListener callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            SomeArgs args = msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    invokeCallback((IStorageEventListener) this.mCallbacks.getBroadcastItem(i), msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IStorageEventListener callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onStorageStateChanged((String) args.arg1, (String) args.arg2, (String) args.arg3);
                    return;
                case 2:
                    callback.onVolumeStateChanged((VolumeInfo) args.arg1, args.argi2, args.argi3);
                    return;
                case 3:
                    callback.onVolumeRecordChanged((VolumeRecord) args.arg1);
                    return;
                case 4:
                    callback.onVolumeForgotten((String) args.arg1);
                    return;
                case 5:
                    callback.onDiskScanned((DiskInfo) args.arg1, args.argi2);
                    return;
                case 6:
                    callback.onDiskDestroyed((DiskInfo) args.arg1);
                    return;
                default:
                    return;
            }
        }

        private void notifyStorageStateChanged(String path, String oldState, String newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = path;
            args.arg2 = oldState;
            args.arg3 = newState;
            obtainMessage(1, args).sendToTarget();
        }

        private void notifyVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = vol.clone();
            args.argi2 = oldState;
            args.argi3 = newState;
            obtainMessage(2, args).sendToTarget();
        }

        private void notifyVolumeRecordChanged(VolumeRecord rec) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = rec.clone();
            obtainMessage(3, args).sendToTarget();
        }

        private void notifyVolumeForgotten(String fsUuid) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = fsUuid;
            obtainMessage(4, args).sendToTarget();
        }

        private void notifyDiskScanned(DiskInfo disk, int volumeCount) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            args.argi2 = volumeCount;
            obtainMessage(5, args).sendToTarget();
        }

        private void notifyDiskDestroyed(DiskInfo disk) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = disk.clone();
            obtainMessage(6, args).sendToTarget();
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            StorageManagerService.this.mObbActionHandler.sendMessage(StorageManagerService.this.mObbActionHandler.obtainMessage(2, IMediaContainerService.Stub.asInterface(service)));
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    abstract class ObbAction {
        private static final int MAX_RETRIES = 3;
        ObbState mObbState;
        private int mRetries;

        abstract void handleExecute() throws ObbException;

        ObbAction(ObbState obbState) {
            this.mObbState = obbState;
        }

        public void execute(ObbActionHandler handler) {
            try {
                this.mRetries++;
                if (this.mRetries > 3) {
                    StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
                    notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                    return;
                }
                handleExecute();
                StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
            } catch (ObbException e) {
                notifyObbStateChange(e);
                StorageManagerService.this.mObbActionHandler.sendEmptyMessage(3);
            }
        }

        protected ObbInfo getObbInfo() throws ObbException {
            try {
                ObbInfo obbInfo = StorageManagerService.this.mContainerService.getObbInfo(this.mObbState.canonicalPath);
                if (obbInfo != null) {
                    return obbInfo;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Missing OBB info for: ");
                stringBuilder.append(this.mObbState.canonicalPath);
                throw new ObbException(20, stringBuilder.toString());
            } catch (Exception e) {
                throw new ObbException(25, e);
            }
        }

        protected void notifyObbStateChange(ObbException e) {
            Slog.w(StorageManagerService.TAG, e);
            notifyObbStateChange(e.status);
        }

        protected void notifyObbStateChange(int status) {
            if (this.mObbState != null && this.mObbState.token != null) {
                try {
                    this.mObbState.token.onObbResult(this.mObbState.rawPath, this.mObbState.nonce, status);
                } catch (RemoteException e) {
                    Slog.w(StorageManagerService.TAG, "StorageEventListener went away while calling onObbStateChanged");
                }
            }
        }
    }

    private class ObbActionHandler extends Handler {
        private final List<ObbAction> mActions = new LinkedList();
        private boolean mBound = false;

        ObbActionHandler(Looper l) {
            super(l);
        }

        public void handleMessage(Message msg) {
            ObbAction action;
            switch (msg.what) {
                case 1:
                    action = (ObbAction) msg.obj;
                    if (this.mBound || connectToService()) {
                        this.mActions.add(action);
                        break;
                    } else {
                        action.notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                        return;
                    }
                case 2:
                    if (msg.obj != null) {
                        StorageManagerService.this.mContainerService = (IMediaContainerService) msg.obj;
                    }
                    if (StorageManagerService.this.mContainerService != null) {
                        if (this.mActions.size() <= 0) {
                            Slog.w(StorageManagerService.TAG, "Empty queue");
                            break;
                        }
                        action = (ObbAction) this.mActions.get(0);
                        if (action != null) {
                            action.execute(this);
                            break;
                        }
                    }
                    for (ObbAction action2 : this.mActions) {
                        action2.notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                    }
                    this.mActions.clear();
                    break;
                    break;
                case 3:
                    if (this.mActions.size() > 0) {
                        this.mActions.remove(0);
                    }
                    if (this.mActions.size() == 0) {
                        if (this.mBound) {
                            disconnectService();
                            break;
                        }
                    }
                    StorageManagerService.this.mObbActionHandler.sendEmptyMessage(2);
                    break;
                    break;
                case 4:
                    if (this.mActions.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            for (ObbAction action22 : this.mActions) {
                                action22.notifyObbStateChange(new ObbException(20, "Failed to bind to media container service"));
                            }
                            this.mActions.clear();
                            break;
                        }
                    }
                    break;
                case 5:
                    String path = msg.obj;
                    synchronized (StorageManagerService.this.mObbMounts) {
                        List<ObbState> obbStatesToRemove = new LinkedList();
                        for (ObbState state : StorageManagerService.this.mObbPathToStateMap.values()) {
                            if (state.canonicalPath.startsWith(path)) {
                                obbStatesToRemove.add(state);
                            }
                        }
                        for (ObbState obbState : obbStatesToRemove) {
                            StorageManagerService.this.removeObbStateLocked(obbState);
                            try {
                                obbState.token.onObbResult(obbState.rawPath, obbState.nonce, 2);
                            } catch (RemoteException e) {
                                String str = StorageManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Couldn't send unmount notification for  OBB: ");
                                stringBuilder.append(obbState.rawPath);
                                Slog.i(str, stringBuilder.toString());
                            }
                        }
                    }
                    break;
            }
        }

        private boolean connectToService() {
            if (!StorageManagerService.this.mContext.bindServiceAsUser(new Intent().setComponent(StorageManagerService.DEFAULT_CONTAINER_COMPONENT), StorageManagerService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                return false;
            }
            this.mBound = true;
            return true;
        }

        private void disconnectService() {
            StorageManagerService.this.mContainerService = null;
            this.mBound = false;
            StorageManagerService.this.mContext.unbindService(StorageManagerService.this.mDefContainerConn);
        }
    }

    private static class ObbException extends Exception {
        public final int status;

        public ObbException(int status, String message) {
            super(message);
            this.status = status;
        }

        public ObbException(int status, Throwable cause) {
            super(cause.getMessage(), cause);
            this.status = status;
        }
    }

    class ObbState implements DeathRecipient {
        final String canonicalPath;
        final int nonce;
        final int ownerGid;
        final String rawPath;
        final IObbActionListener token;
        String volId;

        public ObbState(String rawPath, String canonicalPath, int callingUid, IObbActionListener token, int nonce, String volId) {
            this.rawPath = rawPath;
            this.canonicalPath = canonicalPath;
            this.ownerGid = UserHandle.getSharedAppGid(callingUid);
            this.token = token;
            this.nonce = nonce;
            this.volId = volId;
        }

        public IBinder getBinder() {
            return this.token.asBinder();
        }

        public void binderDied() {
            StorageManagerService.this.mObbActionHandler.sendMessage(StorageManagerService.this.mObbActionHandler.obtainMessage(1, new UnmountObbAction(this, true)));
        }

        public void link() throws RemoteException {
            getBinder().linkToDeath(this, 0);
        }

        public void unlink() {
            getBinder().unlinkToDeath(this, 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ObbState{");
            sb.append("rawPath=");
            sb.append(this.rawPath);
            sb.append(",canonicalPath=");
            sb.append(this.canonicalPath);
            sb.append(",ownerGid=");
            sb.append(this.ownerGid);
            sb.append(",token=");
            sb.append(this.token);
            sb.append(",binder=");
            sb.append(getBinder());
            sb.append(",volId=");
            sb.append(this.volId);
            sb.append('}');
            return sb.toString();
        }
    }

    private final class StorageManagerInternalImpl extends StorageManagerInternal {
        private final CopyOnWriteArrayList<ExternalStorageMountPolicy> mPolicies;

        private StorageManagerInternalImpl() {
            this.mPolicies = new CopyOnWriteArrayList();
        }

        /* synthetic */ StorageManagerInternalImpl(StorageManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void addExternalStoragePolicy(ExternalStorageMountPolicy policy) {
            this.mPolicies.add(policy);
        }

        public void onExternalStoragePolicyChanged(int uid, String packageName) {
            StorageManagerService.this.remountUidExternalStorage(uid, getExternalStorageMountMode(uid, packageName));
        }

        public int getExternalStorageMountMode(int uid, String packageName) {
            int mountMode = HwBootFail.STAGE_BOOT_SUCCESS;
            Iterator it = this.mPolicies.iterator();
            while (it.hasNext()) {
                int policyMode = ((ExternalStorageMountPolicy) it.next()).getMountMode(uid, packageName);
                if (policyMode == 0) {
                    return 0;
                }
                mountMode = Math.min(mountMode, policyMode);
            }
            if (mountMode == HwBootFail.STAGE_BOOT_SUCCESS) {
                return 0;
            }
            return mountMode;
        }

        public boolean hasExternalStorage(int uid, String packageName) {
            if (uid == 1000) {
                return true;
            }
            Iterator it = this.mPolicies.iterator();
            while (it.hasNext()) {
                if (!((ExternalStorageMountPolicy) it.next()).hasExternalStorage(uid, packageName)) {
                    return false;
                }
            }
            return true;
        }
    }

    class StorageManagerServiceHandler extends Handler {
        public StorageManagerServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = 0;
            String str;
            switch (msg.what) {
                case 1:
                    StorageManagerService.this.handleSystemReady();
                    return;
                case 2:
                    StorageManagerService.this.handleDaemonConnected();
                    return;
                case 3:
                    IStorageShutdownObserver obs = msg.obj;
                    boolean success = false;
                    try {
                        StorageManagerService.this.mVold.shutdown();
                        success = true;
                    } catch (Exception e) {
                        Slog.wtf(StorageManagerService.TAG, e);
                    }
                    if (obs != null) {
                        if (!success) {
                            i = -1;
                        }
                        try {
                            obs.onShutDownComplete(i);
                            return;
                        } catch (Exception e2) {
                            return;
                        }
                    }
                    return;
                case 4:
                    Slog.i(StorageManagerService.TAG, "Running fstrim idle maintenance");
                    try {
                        StorageManagerService.this.mLastMaintenance = System.currentTimeMillis();
                        StorageManagerService.this.mLastMaintenanceFile.setLastModified(StorageManagerService.this.mLastMaintenance);
                    } catch (Exception e3) {
                        Slog.e(StorageManagerService.TAG, "Unable to record last fstrim!");
                    }
                    StorageManagerService.this.fstrim(0, null);
                    Runnable callback = msg.obj;
                    if (callback != null) {
                        callback.run();
                        return;
                    }
                    return;
                case 5:
                    VolumeInfo vol = msg.obj;
                    StringBuilder stringBuilder;
                    if (StorageManagerService.this.isMountDisallowed(vol)) {
                        str = StorageManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring mount ");
                        stringBuilder.append(vol.getId());
                        stringBuilder.append(" due to policy");
                        Slog.i(str, stringBuilder.toString());
                        return;
                    } else if (HwDeviceManager.disallowOp(12) && !vol.id.contains("public:179") && vol.id.contains("public:")) {
                        Slog.i(StorageManagerService.TAG, "Usb mass device is disabled by dpm");
                        return;
                    } else if (StorageManagerService.this.isSDCardDisable(vol)) {
                        str = StorageManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring mount ");
                        stringBuilder.append(vol.getId());
                        stringBuilder.append(" due to cryptsd");
                        Slog.i(str, stringBuilder.toString());
                        return;
                    } else if (StorageManagerService.this.mUserStartedFinish || vol.getType() != 0 || vol.getDiskId() == null) {
                        int mountFlags = vol.mountFlags;
                        if (HwDeviceManager.disallowOp(100) && vol.type == 0 && vol.getDisk() != null && vol.getDisk().isSd()) {
                            mountFlags |= 64;
                            if (StorageManagerService.this.sendPrimarySDCardROBroadcastIfNeeded(vol)) {
                                return;
                            }
                        }
                        if (vol.type == 0 && vol.getDisk() != null && vol.getDisk().isSd() && 1 == SystemProperties.getInt(StorageManagerService.PRIMARYSD, 0)) {
                            mountFlags |= 128;
                        }
                        try {
                            StorageManagerService.this.mVold.mount(vol.id, mountFlags, vol.mountUserId);
                            if (StorageManagerService.this.isExternalSDcard(vol)) {
                                StorageManagerService.this.mVold.sendBlockUserId(vol.id, vol.blockedUserId);
                                return;
                            }
                            return;
                        } catch (Exception e4) {
                            Slog.wtf(StorageManagerService.TAG, e4);
                            return;
                        }
                    } else {
                        str = StorageManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("waiting user start finish; trying again , diskId = ");
                        stringBuilder.append(vol.getDiskId());
                        Slog.i(str, stringBuilder.toString());
                        sendMessageDelayed(obtainMessage(5, msg.obj), 500);
                        return;
                    }
                case 6:
                    StorageVolume userVol = msg.obj;
                    str = userVol.getState();
                    String str2 = StorageManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Volume ");
                    stringBuilder2.append(userVol.getId());
                    stringBuilder2.append(" broadcasting ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" to ");
                    stringBuilder2.append(userVol.getOwner());
                    Slog.d(str2, stringBuilder2.toString());
                    str2 = VolumeInfo.getBroadcastForEnvironment(str);
                    if (str2 != null) {
                        Intent intent = new Intent(str2, Uri.fromFile(userVol.getPathFile()));
                        intent.putExtra("android.os.storage.extra.STORAGE_VOLUME", userVol);
                        intent.addFlags(83886080);
                        StorageManagerService.this.mContext.sendBroadcastAsUser(intent, userVol.getOwner());
                        return;
                    }
                    return;
                case 7:
                    StorageManagerService.this.mContext.sendBroadcastAsUser(msg.obj, UserHandle.ALL, "android.permission.WRITE_MEDIA_STORAGE");
                    return;
                case 8:
                    try {
                        StorageManagerService.this.unmount(msg.obj.getId());
                        return;
                    } catch (IllegalStateException e5) {
                        return;
                    }
                case 9:
                    VolumeRecord rec = msg.obj;
                    StorageManagerService.this.forgetPartition(rec.partGuid, rec.fsUuid);
                    return;
                case 10:
                    StorageManagerService.this.resetIfReadyAndConnected();
                    return;
                case 11:
                    Slog.i(StorageManagerService.TAG, "Running idle maintenance");
                    StorageManagerService.this.runIdleMaint((Runnable) msg.obj);
                    return;
                case 12:
                    Slog.i(StorageManagerService.TAG, "Aborting idle maintenance");
                    StorageManagerService.this.abortIdleMaint((Runnable) msg.obj);
                    return;
                case 13:
                    StorageManagerService.this.mContext.sendBroadcastAsUser(msg.obj, UserHandle.ALL);
                    return;
                case 14:
                    StorageManagerService.this.checkIfBackUpDeviceMount(((Integer) msg.obj).intValue());
                    return;
                default:
                    return;
            }
        }
    }

    class VoldResponseCode {
        public static final int VOLUMED_SD_MOUNTED_RO = 870;

        VoldResponseCode() {
        }
    }

    class AppFuseMountScope extends MountScope {
        boolean opened = false;

        public AppFuseMountScope(int uid, int pid, int mountId) {
            super(uid, pid, mountId);
        }

        public ParcelFileDescriptor open() throws NativeDaemonConnectorException {
            try {
                return new ParcelFileDescriptor(StorageManagerService.this.mVold.mountAppFuse(this.uid, Process.myPid(), this.mountId));
            } catch (Exception e) {
                throw new NativeDaemonConnectorException("Failed to mount", e);
            }
        }

        public void close() throws Exception {
            if (this.opened) {
                StorageManagerService.this.mVold.unmountAppFuse(this.uid, Process.myPid(), this.mountId);
                this.opened = false;
            }
        }
    }

    public static class Lifecycle extends SystemService {
        private StorageManagerService mStorageManagerService;

        public Lifecycle(Context context) {
            super(context);
            IHwStorageManagerService iSMS = HwServiceFactory.getHwStorageManagerService();
            if (iSMS != null) {
                this.mStorageManagerService = iSMS.getInstance(getContext());
            } else {
                this.mStorageManagerService = new StorageManagerService(getContext());
            }
        }

        public void onStart() {
            publishBinderService("mount", this.mStorageManagerService);
            this.mStorageManagerService.start();
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mStorageManagerService.systemReady();
            } else if (phase == 1000) {
                this.mStorageManagerService.bootCompleted();
            }
        }

        public void onSwitchUser(int userHandle) {
            this.mStorageManagerService.mCurrentUserId = userHandle;
        }

        public void onUnlockUser(int userHandle) {
            this.mStorageManagerService.onUnlockUser(userHandle);
        }

        public void onCleanupUser(int userHandle) {
            this.mStorageManagerService.onCleanupUser(userHandle);
        }
    }

    class MountObbAction extends ObbAction {
        private final int mCallingUid;
        private final String mKey;

        MountObbAction(ObbState obbState, String key, int callingUid) {
            super(obbState);
            this.mKey = key;
            this.mCallingUid = callingUid;
        }

        public void handleExecute() throws ObbException {
            StorageManagerService.this.warnOnNotMounted();
            ObbInfo obbInfo = getObbInfo();
            if (StorageManagerService.this.isUidOwnerOfPackageOrSystem(obbInfo.packageName, this.mCallingUid)) {
                boolean isMounted;
                synchronized (StorageManagerService.this.mObbMounts) {
                    isMounted = StorageManagerService.this.mObbPathToStateMap.containsKey(this.mObbState.rawPath);
                }
                if (isMounted) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Attempt to mount OBB which is already mounted: ");
                    stringBuilder.append(obbInfo.filename);
                    throw new ObbException(24, stringBuilder.toString());
                }
                String binderKey;
                if (this.mKey == null) {
                    String hashedKey = "none";
                    binderKey = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                } else {
                    try {
                        String hashedKey2 = new BigInteger(SecretKeyFactory.getInstance(BackupPasswordManager.PBKDF_CURRENT).generateSecret(new PBEKeySpec(this.mKey.toCharArray(), obbInfo.salt, 1024, 128)).getEncoded()).toString(16);
                        binderKey = hashedKey2;
                    } catch (GeneralSecurityException e) {
                        throw new ObbException(20, e);
                    }
                }
                try {
                    this.mObbState.volId = StorageManagerService.this.mVold.createObb(this.mObbState.canonicalPath, binderKey, this.mObbState.ownerGid);
                    StorageManagerService.this.mVold.mount(this.mObbState.volId, 0, -1);
                    synchronized (StorageManagerService.this.mObbMounts) {
                        StorageManagerService.this.addObbStateLocked(this.mObbState);
                    }
                    notifyObbStateChange(1);
                    return;
                } catch (Exception e2) {
                    throw new ObbException(21, e2);
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Denied attempt to mount OBB ");
            stringBuilder2.append(obbInfo.filename);
            stringBuilder2.append(" which is owned by ");
            stringBuilder2.append(obbInfo.packageName);
            throw new ObbException(25, stringBuilder2.toString());
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MountObbAction{");
            sb.append(this.mObbState);
            sb.append('}');
            return sb.toString();
        }
    }

    class UnmountObbAction extends ObbAction {
        private final boolean mForceUnmount;

        UnmountObbAction(ObbState obbState, boolean force) {
            super(obbState);
            this.mForceUnmount = force;
        }

        public void handleExecute() throws ObbException {
            ObbState existingState;
            StorageManagerService.this.warnOnNotMounted();
            synchronized (StorageManagerService.this.mObbMounts) {
                existingState = (ObbState) StorageManagerService.this.mObbPathToStateMap.get(this.mObbState.rawPath);
            }
            if (existingState == null) {
                throw new ObbException(23, "Missing existingState");
            } else if (existingState.ownerGid != this.mObbState.ownerGid) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Permission denied to unmount OBB ");
                stringBuilder.append(existingState.rawPath);
                stringBuilder.append(" (owned by GID ");
                stringBuilder.append(existingState.ownerGid);
                stringBuilder.append(")");
                notifyObbStateChange(new ObbException(25, stringBuilder.toString()));
            } else {
                try {
                    StorageManagerService.this.mVold.unmount(this.mObbState.volId);
                    StorageManagerService.this.mVold.destroyObb(this.mObbState.volId);
                    this.mObbState.volId = null;
                    synchronized (StorageManagerService.this.mObbMounts) {
                        StorageManagerService.this.removeObbStateLocked(existingState);
                    }
                    notifyObbStateChange(2);
                } catch (Exception e) {
                    throw new ObbException(22, e);
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UnmountObbAction{");
            sb.append(this.mObbState);
            sb.append(",force=");
            sb.append(this.mForceUnmount);
            sb.append('}');
            return sb.toString();
        }
    }

    private VolumeInfo findVolumeByIdOrThrow(String id) {
        synchronized (this.mLock) {
            VolumeInfo vol = (VolumeInfo) this.mVolumes.get(id);
            if (vol != null) {
                return vol;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No volume found for ID ");
            stringBuilder.append(id);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private String findVolumeIdForPathOrThrow(String path) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mVolumes.size()) {
                VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                if (vol.path == null || !path.startsWith(vol.path)) {
                    i++;
                } else {
                    String str = vol.id;
                    return str;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No volume found for path ");
            stringBuilder.append(path);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private VolumeRecord findRecordForPath(String path) {
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mVolumes.size()) {
                VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                if (vol.path == null || !path.startsWith(vol.path)) {
                    i++;
                } else {
                    VolumeRecord volumeRecord = (VolumeRecord) this.mRecords.get(vol.fsUuid);
                    return volumeRecord;
                }
            }
            return null;
        }
    }

    private String scrubPath(String path) {
        if (path.startsWith(Environment.getDataDirectory().getAbsolutePath())) {
            return "internal";
        }
        VolumeRecord rec = findRecordForPath(path);
        if (rec == null || rec.createdMillis == 0) {
            return Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ext:");
        stringBuilder.append((int) ((System.currentTimeMillis() - rec.createdMillis) / UnixCalendar.WEEK_IN_MILLIS));
        stringBuilder.append("w");
        return stringBuilder.toString();
    }

    private VolumeInfo findStorageForUuid(String volumeUuid) {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            return storage.findVolumeById("emulated");
        }
        if (Objects.equals("primary_physical", volumeUuid)) {
            return storage.getPrimaryPhysicalVolume();
        }
        return storage.findEmulatedForPrivate(storage.findVolumeByUuid(volumeUuid));
    }

    private boolean shouldBenchmark() {
        long benchInterval = Global.getLong(this.mContext.getContentResolver(), "storage_benchmark_interval", UnixCalendar.WEEK_IN_MILLIS);
        if (benchInterval == -1) {
            return false;
        }
        if (benchInterval == 0) {
            return true;
        }
        synchronized (this.mLock) {
            int i = 0;
            while (i < this.mVolumes.size()) {
                VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                VolumeRecord rec = (VolumeRecord) this.mRecords.get(vol.fsUuid);
                if (!vol.isMountedWritable() || rec == null || System.currentTimeMillis() - rec.lastBenchMillis < benchInterval) {
                    i++;
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private CountDownLatch findOrCreateDiskScanLatch(String diskId) {
        CountDownLatch latch;
        synchronized (this.mLock) {
            latch = (CountDownLatch) this.mDiskScanLatches.get(diskId);
            if (latch == null) {
                latch = new CountDownLatch(1);
                this.mDiskScanLatches.put(diskId, latch);
            }
        }
        return latch;
    }

    private void waitForLatch(CountDownLatch latch, String condition, long timeoutMillis) throws TimeoutException {
        long startMillis = SystemClock.elapsedRealtime();
        while (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            StringBuilder stringBuilder;
            try {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Thread ");
                stringBuilder.append(Thread.currentThread().getName());
                stringBuilder.append(" still waiting for ");
                stringBuilder.append(condition);
                stringBuilder.append("...");
                Slog.w(str, stringBuilder.toString());
            } catch (InterruptedException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Interrupt while waiting for ");
                stringBuilder2.append(condition);
                Slog.w(str2, stringBuilder2.toString());
            }
            if (timeoutMillis > 0) {
                if (SystemClock.elapsedRealtime() > startMillis + timeoutMillis) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Thread ");
                    stringBuilder.append(Thread.currentThread().getName());
                    stringBuilder.append(" gave up waiting for ");
                    stringBuilder.append(condition);
                    stringBuilder.append(" after ");
                    stringBuilder.append(timeoutMillis);
                    stringBuilder.append("ms");
                    throw new TimeoutException(stringBuilder.toString());
                }
            }
        }
    }

    private void handleSystemReady() {
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        MountServiceIdler.scheduleIdlePass(this.mContext);
        FstrimServiceIdler.schedulePreFstrim(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("zram_enabled"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                StorageManagerService.this.refreshZramSettings();
            }
        });
        refreshZramSettings();
    }

    private void refreshZramSettings() {
        String propertyValue = SystemProperties.get(ZRAM_ENABLED_PROPERTY);
        if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(propertyValue)) {
            String desiredPropertyValue = Global.getInt(this.mContext.getContentResolver(), "zram_enabled", 1) != 0 ? "1" : "0";
            if (!desiredPropertyValue.equals(propertyValue)) {
                SystemProperties.set(ZRAM_ENABLED_PROPERTY, desiredPropertyValue);
            }
        }
    }

    @Deprecated
    private void killMediaProvider(List<UserInfo> users) {
        if (users != null) {
            long token = Binder.clearCallingIdentity();
            try {
                for (UserInfo user : users) {
                    if (!user.isSystemOnly()) {
                        ProviderInfo provider = this.mPms.resolveContentProvider("media", 786432, user.id);
                        if (provider != null) {
                            try {
                                ActivityManager.getService().killApplication(provider.applicationInfo.packageName, UserHandle.getAppId(provider.applicationInfo.uid), -1, "vold reset");
                                break;
                            } catch (RemoteException e) {
                            }
                        }
                    }
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    @GuardedBy("mLock")
    private void addInternalVolumeLocked() {
        VolumeInfo internal = new VolumeInfo("private", 1, null, null);
        internal.state = 2;
        internal.path = Environment.getDataDirectory().getAbsolutePath();
        this.mVolumes.put(internal.id, internal);
    }

    private void initIfReadyAndConnected() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Thinking about init, mSystemReady=");
        stringBuilder.append(this.mSystemReady);
        stringBuilder.append(", mDaemonConnected=");
        stringBuilder.append(this.mDaemonConnected);
        Slog.d(str, stringBuilder.toString());
        if (this.mSystemReady && this.mDaemonConnected && !StorageManager.isFileEncryptedNativeOnly()) {
            boolean initLocked = StorageManager.isFileEncryptedEmulatedOnly();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Setting up emulation state, initlocked=");
            stringBuilder2.append(initLocked);
            Slog.d(str2, stringBuilder2.toString());
            for (UserInfo user : ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers()) {
                if (initLocked) {
                    try {
                        this.mVold.lockUserKey(user.id);
                    } catch (Exception e) {
                        Slog.wtf(TAG, e);
                    }
                } else {
                    this.mVold.unlockUserKey(user.id, user.serialNumber, encodeBytes(null), encodeBytes(null));
                }
            }
        }
    }

    private void resetIfReadyAndConnected() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Thinking about reset, mSystemReady=");
        stringBuilder.append(this.mSystemReady);
        stringBuilder.append(", mDaemonConnected=");
        stringBuilder.append(this.mDaemonConnected);
        Slog.d(str, stringBuilder.toString());
        if (this.mSystemReady && this.mDaemonConnected) {
            List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
            killMediaProvider(users);
            synchronized (this.mLock) {
                int[] systemUnlockedUsers = this.mSystemUnlockedUsers;
                this.mDisks.clear();
                this.mVolumes.clear();
                addInternalVolumeLocked();
            }
            try {
                this.mVold.reset();
                for (UserInfo user : users) {
                    this.mVold.onUserAdded(user.id, user.serialNumber);
                }
                for (int userId : systemUnlockedUsers) {
                    this.mVold.onUserStarted(userId);
                    this.mStoraged.onUserStarted(userId);
                }
                this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
            } catch (Exception e) {
                Slog.wtf(TAG, e);
            }
        }
    }

    private void onUnlockUser(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUnlockUser ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        try {
            this.mVold.onUserStarted(userId);
            this.mStoraged.onUserStarted(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("hava send user_started to vold , userId = ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        this.mUserStartedFinish = true;
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                if (vol.isVisibleForRead(userId) && vol.isMountedReadable()) {
                    StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                    this.mHandler.obtainMessage(6, userVol).sendToTarget();
                    String envState = VolumeInfo.getEnvironmentForState(vol.getState());
                    this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), envState, envState);
                }
            }
            this.mSystemUnlockedUsers = ArrayUtils.appendInt(this.mSystemUnlockedUsers, userId);
        }
    }

    private void onCleanupUser(int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCleanupUser ");
        stringBuilder.append(userId);
        Slog.d(str, stringBuilder.toString());
        try {
            this.mVold.onUserStopped(userId);
            this.mStoraged.onUserStopped(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
        synchronized (this.mLock) {
            this.mSystemUnlockedUsers = ArrayUtils.removeInt(this.mSystemUnlockedUsers, userId);
        }
    }

    public void onAwakeStateChanged(boolean isAwake) {
    }

    public void onKeyguardStateChanged(boolean isShowing) {
        KeyguardManager keyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
        if (keyguardManager == null) {
            Slog.i(TAG, "KeyguradManager service is null");
            return;
        }
        boolean z = isShowing && keyguardManager.isDeviceSecure();
        this.mSecureKeyguardShowing = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onKeyguardStateChanged ");
        stringBuilder.append(isShowing);
        Slog.i(str, stringBuilder.toString());
        try {
            this.mVold.onSecureKeyguardStateChanged(this.mSecureKeyguardShowing);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    void runIdleMaintenance(Runnable callback) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, callback));
    }

    public void runMaintenance() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        runIdleMaintenance(null);
    }

    public long lastMaintenance() {
        return this.mLastMaintenance;
    }

    public void onDaemonConnected() {
        this.mDaemonConnected = true;
        this.mHandler.obtainMessage(2).sendToTarget();
    }

    private void handleDaemonConnected() {
        initIfReadyAndConnected();
        resetIfReadyAndConnected();
        if (BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(SystemProperties.get("vold.encrypt_progress"))) {
            copyLocaleFromMountService();
        }
    }

    private void copyLocaleFromMountService() {
        try {
            String systemLocale = getField("SystemLocale");
            if (!TextUtils.isEmpty(systemLocale)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got locale ");
                stringBuilder.append(systemLocale);
                stringBuilder.append(" from mount service");
                Slog.d(str, stringBuilder.toString());
                Locale locale = Locale.forLanguageTag(systemLocale);
                Configuration config = new Configuration();
                config.setLocale(locale);
                try {
                    ActivityManager.getService().updatePersistentConfiguration(config);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error setting system locale from mount service", e);
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Setting system properties to ");
                stringBuilder2.append(systemLocale);
                stringBuilder2.append(" from mount service");
                Slog.d(str2, stringBuilder2.toString());
                SystemProperties.set("persist.sys.locale", locale.toLanguageTag());
            }
        } catch (RemoteException e2) {
        } catch (IllegalStateException e3) {
            Slog.e(TAG, "copyLocaleFromMountService, getField threw IllegalStateException");
        }
    }

    @GuardedBy("mLock")
    private void onDiskScannedLocked(DiskInfo disk) {
        int volumeCount = 0;
        for (int i = 0; i < this.mVolumes.size(); i++) {
            if (Objects.equals(disk.id, ((VolumeInfo) this.mVolumes.valueAt(i)).getDiskId())) {
                volumeCount++;
            }
        }
        Intent intent = new Intent("android.os.storage.action.DISK_SCANNED");
        intent.addFlags(83886080);
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.id);
        intent.putExtra("android.os.storage.extra.VOLUME_COUNT", volumeCount);
        this.mHandler.obtainMessage(7, intent).sendToTarget();
        CountDownLatch latch = (CountDownLatch) this.mDiskScanLatches.remove(disk.id);
        if (latch != null) {
            latch.countDown();
        }
        disk.volumeCount = volumeCount;
        this.mCallbacks.notifyDiskScanned(disk, volumeCount);
    }

    public int getPrivacySpaceUserId() {
        Context context = this.mContext;
        Context context2 = this.mContext;
        for (UserInfo info : ((UserManager) context.getSystemService("user")).getUsers()) {
            if (info.isHwHiddenSpace()) {
                return info.id;
            }
        }
        return -1;
    }

    @GuardedBy("mLock")
    protected void onVolumeCreatedLocked(VolumeInfo vol) {
        String str;
        StringBuilder stringBuilder;
        if (this.mPms.isOnlyCoreApps()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("System booted in core-only mode; ignoring volume ");
            stringBuilder.append(vol.getId());
            Slog.d(str, stringBuilder.toString());
            return;
        }
        if (vol.type == 2) {
            VolumeInfo privateVol = ((StorageManager) this.mContext.getSystemService(StorageManager.class)).findPrivateForEmulated(vol);
            String str2;
            StringBuilder stringBuilder2;
            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid) && "private".equals(privateVol.id)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Found primary storage at ");
                stringBuilder2.append(vol);
                Slog.v(str2, stringBuilder2.toString());
                vol.mountFlags = 1 | vol.mountFlags;
                vol.mountFlags |= 2;
                this.mHandler.obtainMessage(5, vol).sendToTarget();
            } else if (Objects.equals(privateVol.fsUuid, this.mPrimaryStorageUuid)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Found primary storage at ");
                stringBuilder2.append(vol);
                Slog.v(str2, stringBuilder2.toString());
                vol.mountFlags = 1 | vol.mountFlags;
                vol.mountFlags |= 2;
                this.mHandler.obtainMessage(5, vol).sendToTarget();
            }
        } else if (vol.type == 0) {
            if (Objects.equals("primary_physical", this.mPrimaryStorageUuid) && vol.disk.isDefaultPrimary()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Found primary storage at ");
                stringBuilder.append(vol);
                Slog.v(str, stringBuilder.toString());
                vol.mountFlags |= 1;
                vol.mountFlags |= 2;
            }
            if (vol.disk.isAdoptable()) {
                vol.mountFlags |= 2;
            }
            vol.mountUserId = this.mCurrentUserId;
            vol.blockedUserId = -1;
            if (SystemProperties.getBoolean(PROPERTIES_PRIVACY_SPACE_HIDSD, true)) {
                Slog.i(TAG, "onVolumeCreatedLocked before getPrivacySpaceUserId");
                vol.blockedUserId = getPrivacySpaceUserId();
            }
        } else if (vol.type == 1) {
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Skipping automatic mounting of ");
            stringBuilder.append(vol);
            Slog.d(str, stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    protected void onCheckVolumeCompletedLocked(VolumeInfo vol) {
        String str;
        StringBuilder stringBuilder;
        if (this.mPms.isOnlyCoreApps()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCheckVolumeCompletedLocked : System booted in core-only mode; ignoring volume ");
            stringBuilder.append(vol.getId());
            Slog.d(str, stringBuilder.toString());
            return;
        }
        if (vol.type == 0) {
            this.mHandler.obtainMessage(5, vol).sendToTarget();
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCheckVolumeCompletedLocked : Skipping automatic mounting of ");
            stringBuilder.append(vol);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private boolean isBroadcastWorthy(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
            case 1:
            case 2:
                switch (vol.getState()) {
                    case 0:
                    case 2:
                    case 3:
                    case 5:
                    case 6:
                    case 8:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    @GuardedBy("mLock")
    private void onVolumeStateChangedLocked(VolumeInfo vol, int oldState, int newState) {
        if (HwFrameworkFactory.getHwApiCacheManagerEx() != null) {
            HwFrameworkFactory.getHwApiCacheManagerEx().notifyVolumeStateChanged(oldState, newState);
        }
        if (vol.isMountedReadable() && !TextUtils.isEmpty(vol.fsUuid)) {
            VolumeRecord rec = (VolumeRecord) this.mRecords.get(vol.fsUuid);
            if (rec == null) {
                rec = new VolumeRecord(vol.type, vol.fsUuid);
                rec.partGuid = vol.partGuid;
                rec.createdMillis = System.currentTimeMillis();
                if (vol.type == 1) {
                    rec.nickname = vol.disk.getDescription();
                }
                this.mRecords.put(rec.fsUuid, rec);
                writeSettingsLocked();
            } else if (TextUtils.isEmpty(rec.partGuid)) {
                rec.partGuid = vol.partGuid;
                writeSettingsLocked();
            }
        }
        this.mCallbacks.notifyVolumeStateChanged(vol, oldState, newState);
        if (this.mBootCompleted && isBroadcastWorthy(vol)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onVolumeStateChangedLocked volumeInfo = ");
            stringBuilder.append(vol.toString());
            Flog.i(1002, stringBuilder.toString());
            Intent intent = new Intent("android.os.storage.action.VOLUME_STATE_CHANGED");
            intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.id);
            intent.putExtra("android.os.storage.extra.VOLUME_STATE", newState);
            intent.putExtra("android.os.storage.extra.FS_UUID", vol.fsUuid);
            intent.addFlags(83886080);
            this.mHandler.obtainMessage(7, intent).sendToTarget();
        }
        String oldStateEnv = VolumeInfo.getEnvironmentForState(oldState);
        String newStateEnv = VolumeInfo.getEnvironmentForState(newState);
        if (!Objects.equals(oldStateEnv, newStateEnv)) {
            for (int userId : this.mSystemUnlockedUsers) {
                if (vol.isVisibleForRead(userId)) {
                    StorageVolume userVol = vol.buildStorageVolume(this.mContext, userId, false);
                    this.mHandler.obtainMessage(6, userVol).sendToTarget();
                    this.mCallbacks.notifyStorageStateChanged(userVol.getPath(), oldStateEnv, newStateEnv);
                }
            }
            if (3 == newState) {
                Intent intent2 = new Intent("com.huawei.android.MEDIA_MOUNTED_RO");
                intent2.putExtra("android.os.storage.extra.STORAGE_VOLUME", vol);
                intent2.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SD card romounted volumeInfo = ");
                stringBuilder2.append(vol.toString());
                Flog.i(1002, stringBuilder2.toString());
                this.mHandler.obtainMessage(7, intent2).sendToTarget();
            }
        }
        if (vol.type == 0 && vol.state == 5) {
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(5, vol.path));
        }
        maybeLogMediaMount(vol, newState);
    }

    private void maybeLogMediaMount(VolumeInfo vol, int newState) {
        if (SecurityLog.isLoggingEnabled()) {
            DiskInfo disk = vol.getDisk();
            if (disk != null && (disk.flags & 12) != 0) {
                String label = disk.label != null ? disk.label.trim() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                if (newState == 2 || newState == 3) {
                    SecurityLog.writeEvent(210013, new Object[]{vol.path, label});
                } else if (newState == 0 || newState == 8) {
                    SecurityLog.writeEvent(210014, new Object[]{vol.path, label});
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void onMoveStatusLocked(int status) {
        if (this.mMoveCallback == null) {
            Slog.w(TAG, "Odd, status but no move requested");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        try {
            this.mMoveCallback.onStatusChanged(-1, status, -1);
        } catch (RemoteException e) {
        }
        if (status == 82) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Move to ");
            stringBuilder.append(this.mMoveTargetUuid);
            stringBuilder.append(" copy phase finshed; persisting");
            Slog.d(str, stringBuilder.toString());
            this.mPrimaryStorageUuid = this.mMoveTargetUuid;
            writeSettingsLocked();
        }
        if (PackageManager.isMoveStatusFinished(status)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Move to ");
            stringBuilder.append(this.mMoveTargetUuid);
            stringBuilder.append(" finished with status ");
            stringBuilder.append(status);
            Slog.d(str, stringBuilder.toString());
            this.mMoveCallback = null;
            this.mMoveTargetUuid = null;
        }
    }

    protected void enforcePermission(String perm) {
        this.mContext.enforceCallingOrSelfPermission(perm, perm);
    }

    private boolean isMountDisallowed(VolumeInfo vol) {
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        boolean isUsbRestricted = false;
        if (vol.disk != null && vol.disk.isUsb()) {
            isUsbRestricted = userManager.hasUserRestriction("no_usb_file_transfer", Binder.getCallingUserHandle());
        }
        boolean isTypeRestricted = false;
        if (vol.type == 0 || vol.type == 1) {
            isTypeRestricted = userManager.hasUserRestriction("no_physical_media", Binder.getCallingUserHandle());
        }
        if (isUsbRestricted || isTypeRestricted) {
            return true;
        }
        return false;
    }

    private boolean isSDCardDisable(VolumeInfo vol) {
        if (!"wait_unlock".equals(getCryptsdState()) || vol.type != 0 || vol.getDisk() == null || !vol.getDisk().isSd()) {
            return false;
        }
        Slog.i(TAG, "wait_unlock, disallow domount");
        return true;
    }

    private boolean sendPrimarySDCardROBroadcastIfNeeded(VolumeInfo vol) {
        long ident = Binder.clearCallingIdentity();
        try {
            if (1 == SystemProperties.getInt(PRIMARYSD, 0)) {
                Intent intentReboot = new Intent("com.huawei.android.PRIMARYSD_MOUNTED_RO");
                intentReboot.addFlags(DumpState.DUMP_SERVICE_PERMISSIONS);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Primary SD card ro volumeInfo = ");
                stringBuilder.append(vol.toString());
                Flog.i(1002, stringBuilder.toString());
                this.mHandler.obtainMessage(7, intentReboot).sendToTarget();
                return true;
            }
            Binder.restoreCallingIdentity(ident);
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private String getCryptsdState() {
        long ident = Binder.clearCallingIdentity();
        try {
            String state;
            if (SystemProperties.getBoolean("ro.config.support_sdcard_crypt", true)) {
                state = SystemProperties.get("vold.cryptsd.state", "none");
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CryptsdState: ");
                stringBuilder.append(state);
                Slog.i(str, stringBuilder.toString());
                Binder.restoreCallingIdentity(ident);
                return state;
            }
            state = "none";
            return state;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void enforceAdminUser() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        int callingUserId = UserHandle.getCallingUserId();
        long token = Binder.clearCallingIdentity();
        try {
            boolean isAdmin = um.getUserInfo(callingUserId).isAdmin();
            if (!isAdmin) {
                throw new SecurityException("Only admin users can adopt sd cards");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public StorageManagerService(Context context) {
        sSelf = this;
        this.mContext = context;
        this.mCallbacks = new Callbacks(FgThread.get().getLooper());
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPms = (PackageManagerService) ServiceManager.getService("package");
        HandlerThread hthread = new HandlerThread(TAG);
        hthread.start();
        this.mHandler = new StorageManagerServiceHandler(hthread.getLooper());
        this.mObbActionHandler = new ObbActionHandler(IoThread.get().getLooper());
        this.mLastMaintenanceFile = new File(new File(Environment.getDataDirectory(), "system"), LAST_FSTRIM_FILE);
        if (this.mLastMaintenanceFile.exists()) {
            this.mLastMaintenance = this.mLastMaintenanceFile.lastModified();
        } else {
            try {
                new FileOutputStream(this.mLastMaintenanceFile).close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create fstrim record ");
                stringBuilder.append(this.mLastMaintenanceFile.getPath());
                Slog.e(str, stringBuilder.toString());
            }
        }
        this.mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "storage.xml"), "storage-settings");
        synchronized (this.mLock) {
            readSettingsLocked();
        }
        LocalServices.addService(StorageManagerInternal.class, this.mStorageManagerInternal);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        IntentFilter bootFilter = new IntentFilter();
        bootFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mToVoldBroadcastReceiver, bootFilter, null, this.mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.DREAMING_STARTED");
        this.mContext.registerReceiver(this.mScreenAndDreamingReceiver, filter, null, this.mHandler);
        synchronized (this.mLock) {
            addInternalVolumeLocked();
        }
    }

    private void start() {
        connect();
    }

    private void connect() {
        IBinder binder = ServiceManager.getService("storaged");
        if (binder != null) {
            try {
                binder.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "storaged died; reconnecting");
                        StorageManagerService.this.mStoraged = null;
                        StorageManagerService.this.connect();
                    }
                }, 0);
            } catch (RemoteException e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mStoraged = IStoraged.Stub.asInterface(binder);
        } else {
            Slog.w(TAG, "storaged not found; trying again");
        }
        binder = ServiceManager.getService("vold");
        if (binder != null) {
            try {
                binder.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        Slog.w(StorageManagerService.TAG, "vold died; reconnecting");
                        StorageManagerService.this.mVold = null;
                        StorageManagerService.this.connect();
                    }
                }, 0);
            } catch (RemoteException e2) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mVold = IVold.Stub.asInterface(binder);
            try {
                this.mVold.setListener(this.mListener);
            } catch (RemoteException e3) {
                this.mVold = null;
                Slog.w(TAG, "vold listener rejected; trying again", e3);
            }
        } else {
            Slog.w(TAG, "vold not found; trying again");
        }
        if (this.mStoraged == null || this.mVold == null) {
            BackgroundThread.getHandler().postDelayed(new -$$Lambda$StorageManagerService$buonS_4R9p4O7J-YZEALUUKVETQ(this), 1000);
        } else {
            onDaemonConnected();
        }
    }

    private void systemReady() {
        ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
        this.mSystemReady = true;
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void bootCompleted() {
        this.mBootCompleted = true;
        int deviceCode = getUsbDeviceExInfo();
        if (deviceCode < 0) {
            Slog.i(TAG, "getUsbDeviceExInfo deviceCode < 0 ,return .");
        } else {
            this.mHandler.obtainMessage(14, Integer.valueOf(deviceCode)).sendToTarget();
        }
    }

    private String getDefaultPrimaryStorageUuid() {
        if (SystemProperties.getBoolean("ro.vold.primary_physical", false)) {
            return "primary_physical";
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    @GuardedBy("mLock")
    private void readSettingsLocked() {
        this.mRecords.clear();
        this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
        FileInputStream fis = null;
        try {
            fis = this.mSettingsFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            while (true) {
                int next = in.next();
                int type = next;
                boolean z = true;
                if (next == 1) {
                    break;
                } else if (type == 2) {
                    String tag = in.getName();
                    if (TAG_VOLUMES.equals(tag)) {
                        int version = XmlUtils.readIntAttribute(in, ATTR_VERSION, 1);
                        boolean primaryPhysical = SystemProperties.getBoolean("ro.vold.primary_physical", false);
                        if (version < 3) {
                            if (version < 2 || primaryPhysical) {
                                z = false;
                            }
                        }
                        if (z) {
                            this.mPrimaryStorageUuid = XmlUtils.readStringAttribute(in, ATTR_PRIMARY_STORAGE_UUID);
                        }
                    } else if (TAG_VOLUME.equals(tag)) {
                        VolumeRecord rec = readVolumeRecord(in);
                        this.mRecords.put(rec.fsUuid, rec);
                    }
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            Slog.wtf(TAG, "Failed reading metadata", e2);
        } catch (XmlPullParserException e3) {
            Slog.wtf(TAG, "Failed reading metadata", e3);
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(fis);
    }

    @GuardedBy("mLock")
    private void writeSettingsLocked() {
        FileOutputStream fos = null;
        try {
            fos = this.mSettingsFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_VOLUMES);
            XmlUtils.writeIntAttribute(out, ATTR_VERSION, 3);
            XmlUtils.writeStringAttribute(out, ATTR_PRIMARY_STORAGE_UUID, this.mPrimaryStorageUuid);
            int size = this.mRecords.size();
            for (int i = 0; i < size; i++) {
                writeVolumeRecord(out, (VolumeRecord) this.mRecords.valueAt(i));
            }
            out.endTag(null, TAG_VOLUMES);
            out.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mSettingsFile.failWrite(fos);
            }
        }
    }

    public static VolumeRecord readVolumeRecord(XmlPullParser in) throws IOException {
        VolumeRecord meta = new VolumeRecord(XmlUtils.readIntAttribute(in, "type"), XmlUtils.readStringAttribute(in, ATTR_FS_UUID));
        meta.partGuid = XmlUtils.readStringAttribute(in, ATTR_PART_GUID);
        meta.nickname = XmlUtils.readStringAttribute(in, ATTR_NICKNAME);
        meta.userFlags = XmlUtils.readIntAttribute(in, ATTR_USER_FLAGS);
        meta.createdMillis = XmlUtils.readLongAttribute(in, ATTR_CREATED_MILLIS);
        meta.lastTrimMillis = XmlUtils.readLongAttribute(in, ATTR_LAST_TRIM_MILLIS);
        meta.lastBenchMillis = XmlUtils.readLongAttribute(in, ATTR_LAST_BENCH_MILLIS);
        return meta;
    }

    public static void writeVolumeRecord(XmlSerializer out, VolumeRecord rec) throws IOException {
        out.startTag(null, TAG_VOLUME);
        XmlUtils.writeIntAttribute(out, "type", rec.type);
        XmlUtils.writeStringAttribute(out, ATTR_FS_UUID, rec.fsUuid);
        XmlUtils.writeStringAttribute(out, ATTR_PART_GUID, rec.partGuid);
        XmlUtils.writeStringAttribute(out, ATTR_NICKNAME, rec.nickname);
        XmlUtils.writeIntAttribute(out, ATTR_USER_FLAGS, rec.userFlags);
        XmlUtils.writeLongAttribute(out, ATTR_CREATED_MILLIS, rec.createdMillis);
        XmlUtils.writeLongAttribute(out, ATTR_LAST_TRIM_MILLIS, rec.lastTrimMillis);
        XmlUtils.writeLongAttribute(out, ATTR_LAST_BENCH_MILLIS, rec.lastBenchMillis);
        out.endTag(null, TAG_VOLUME);
    }

    public void registerListener(IStorageEventListener listener) {
        this.mCallbacks.register(listener);
    }

    public void unregisterListener(IStorageEventListener listener) {
        this.mCallbacks.unregister(listener);
    }

    public void shutdown(IStorageShutdownObserver observer) {
        enforcePermission("android.permission.SHUTDOWN");
        Slog.i(TAG, "Shutting down");
        this.mHandler.obtainMessage(3, observer).sendToTarget();
    }

    public boolean isExternalSDcard(VolumeInfo vol) {
        if (vol != null && vol.type == 0 && vol.getDisk() != null && vol.getDisk().isSd()) {
            return true;
        }
        return false;
    }

    private void bootCompleteToVold() {
        try {
            Flog.i(1002, "begin send BootCompleteToVold");
        } catch (Exception e) {
            Flog.e(1002, "other Exception ");
        }
    }

    public void mount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        VolumeInfo vol = findVolumeByIdOrThrow(volId);
        StringBuilder stringBuilder;
        if (isMountDisallowed(vol)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Mounting ");
            stringBuilder.append(volId);
            stringBuilder.append(" restricted by policy");
            throw new SecurityException(stringBuilder.toString());
        } else if (HwDeviceManager.disallowOp(12) && !vol.id.contains("public:179") && vol.id.contains("public:")) {
            Slog.i(TAG, "Usb mass device is disabled by dpm");
            stringBuilder = new StringBuilder();
            stringBuilder.append("Mounting ");
            stringBuilder.append(volId);
            stringBuilder.append(" failed because of USB otg is disabled by dpm");
            throw new SecurityException(stringBuilder.toString());
        } else if (isSDCardDisable(vol)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Mounting ");
            stringBuilder.append(volId);
            stringBuilder.append(" restricted by cryptsd");
            throw new SecurityException(stringBuilder.toString());
        } else {
            int mountFlags = vol.mountFlags;
            if (HwDeviceManager.disallowOp(100) && vol.type == 0 && vol.getDisk() != null && vol.getDisk().isSd()) {
                mountFlags |= 64;
                if (sendPrimarySDCardROBroadcastIfNeeded(vol)) {
                    return;
                }
            }
            vol.blockedUserId = -1;
            if (SystemProperties.getBoolean(PROPERTIES_PRIVACY_SPACE_HIDSD, true)) {
                Slog.i(TAG, "mount before getPrivacySpaceUserId");
                vol.blockedUserId = getPrivacySpaceUserId();
            }
            if (vol.type == 0 && vol.getDisk() != null && vol.getDisk().isSd() && 1 == SystemProperties.getInt(PRIMARYSD, 0)) {
                mountFlags |= 128;
            }
            try {
                this.mVold.mount(vol.id, mountFlags, vol.mountUserId);
                if (isExternalSDcard(vol)) {
                    this.mVold.sendBlockUserId(vol.id, vol.blockedUserId);
                }
            } catch (Exception e) {
                Slog.wtf(TAG, e);
            }
        }
    }

    public void unmount(String volId) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        try {
            this.mVold.unmount(findVolumeByIdOrThrow(volId).id);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void format(String volId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.format(findVolumeByIdOrThrow(volId).id, Shell.NIGHT_MODE_STR_AUTO);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void benchmark(String volId, final IVoldTaskListener listener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.benchmark(volId, new IVoldTaskListener.Stub() {
                public void onStatus(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnStatus(listener, status, extras);
                }

                public void onFinished(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnFinished(listener, status, extras);
                    String path = extras.getString("path");
                    String ident = extras.getString("ident");
                    long create = extras.getLong("create");
                    long run = extras.getLong("run");
                    long destroy = extras.getLong("destroy");
                    DropBoxManager dropBox = (DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class);
                    if (dropBox != null) {
                        String str = StorageManagerService.TAG_STORAGE_BENCHMARK;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(StorageManagerService.this.scrubPath(path));
                        stringBuilder.append(" ");
                        stringBuilder.append(ident);
                        stringBuilder.append(" ");
                        stringBuilder.append(create);
                        stringBuilder.append(" ");
                        stringBuilder.append(run);
                        stringBuilder.append(" ");
                        stringBuilder.append(destroy);
                        dropBox.addText(str, stringBuilder.toString());
                    }
                    synchronized (StorageManagerService.this.mLock) {
                        VolumeRecord rec = StorageManagerService.this.findRecordForPath(path);
                        if (rec != null) {
                            rec.lastBenchMillis = System.currentTimeMillis();
                            StorageManagerService.this.writeSettingsLocked();
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void partitionPublic(String diskId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mVold.partition(diskId, 0, -1);
            waitForLatch(latch, "partitionPublic", 180000);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionPrivate(String diskId) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mVold.partition(diskId, 1, -1);
            waitForLatch(latch, "partitionPrivate", 180000);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void partitionMixed(String diskId, int ratio) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        enforceAdminUser();
        CountDownLatch latch = findOrCreateDiskScanLatch(diskId);
        try {
            this.mVold.partition(diskId, 2, ratio);
            waitForLatch(latch, "partitionMixed", 180000);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void setVolumeNickname(String fsUuid, String nickname) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = (VolumeRecord) this.mRecords.get(fsUuid);
            rec.nickname = nickname;
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void setVolumeUserFlags(String fsUuid, int flags, int mask) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = (VolumeRecord) this.mRecords.get(fsUuid);
            rec.userFlags = (rec.userFlags & (~mask)) | (flags & mask);
            this.mCallbacks.notifyVolumeRecordChanged(rec);
            writeSettingsLocked();
        }
    }

    public void forgetVolume(String fsUuid) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        Preconditions.checkNotNull(fsUuid);
        synchronized (this.mLock) {
            VolumeRecord rec = (VolumeRecord) this.mRecords.remove(fsUuid);
            if (!(rec == null || TextUtils.isEmpty(rec.partGuid))) {
                this.mHandler.obtainMessage(9, rec).sendToTarget();
            }
            this.mCallbacks.notifyVolumeForgotten(fsUuid);
            if (Objects.equals(this.mPrimaryStorageUuid, fsUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
                this.mHandler.obtainMessage(10).sendToTarget();
            }
            writeSettingsLocked();
        }
    }

    public void forgetAllVolumes() {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                String fsUuid = (String) this.mRecords.keyAt(i);
                VolumeRecord rec = (VolumeRecord) this.mRecords.valueAt(i);
                if (!TextUtils.isEmpty(rec.partGuid)) {
                    this.mHandler.obtainMessage(9, rec).sendToTarget();
                }
                this.mCallbacks.notifyVolumeForgotten(fsUuid);
            }
            this.mRecords.clear();
            if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, this.mPrimaryStorageUuid)) {
                this.mPrimaryStorageUuid = getDefaultPrimaryStorageUuid();
            }
            writeSettingsLocked();
            this.mHandler.obtainMessage(10).sendToTarget();
        }
    }

    private void forgetPartition(String partGuid, String fsUuid) {
        try {
            this.mVold.forgetPartition(partGuid, fsUuid);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fstrim(int flags, final IVoldTaskListener listener) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.fstrim(flags, new IVoldTaskListener.Stub() {
                public void onStatus(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnStatus(listener, status, extras);
                    if (status == 0) {
                        String path = extras.getString("path");
                        long bytes = extras.getLong("bytes");
                        long time = extras.getLong("time");
                        DropBoxManager dropBox = (DropBoxManager) StorageManagerService.this.mContext.getSystemService(DropBoxManager.class);
                        if (dropBox != null) {
                            String str = StorageManagerService.TAG_STORAGE_TRIM;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(StorageManagerService.this.scrubPath(path));
                            stringBuilder.append(" ");
                            stringBuilder.append(bytes);
                            stringBuilder.append(" ");
                            stringBuilder.append(time);
                            dropBox.addText(str, stringBuilder.toString());
                        }
                        synchronized (StorageManagerService.this.mLock) {
                            VolumeRecord rec = StorageManagerService.this.findRecordForPath(path);
                            if (rec != null) {
                                rec.lastTrimMillis = System.currentTimeMillis();
                                StorageManagerService.this.writeSettingsLocked();
                            }
                        }
                    }
                }

                public void onFinished(int status, PersistableBundle extras) {
                    StorageManagerService.this.dispatchOnFinished(listener, status, extras);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    void runIdleMaint(final Runnable callback) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mLastMaintenance = System.currentTimeMillis();
            this.mLastMaintenanceFile.setLastModified(this.mLastMaintenance);
        } catch (Exception e) {
            Slog.e(TAG, "Unable to record last fstrim!");
        }
        try {
            this.mVold.runIdleMaint(new IVoldTaskListener.Stub() {
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    if (callback != null) {
                        BackgroundThread.getHandler().post(callback);
                    }
                }
            });
        } catch (Exception e2) {
            Slog.wtf(TAG, e2);
        }
    }

    public void runIdleMaintenance() {
        runIdleMaint(null);
    }

    void abortIdleMaint(final Runnable callback) {
        enforcePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        try {
            this.mVold.abortIdleMaint(new IVoldTaskListener.Stub() {
                public void onStatus(int status, PersistableBundle extras) {
                }

                public void onFinished(int status, PersistableBundle extras) {
                    if (callback != null) {
                        BackgroundThread.getHandler().post(callback);
                    }
                }
            });
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void abortIdleMaintenance() {
        abortIdleMaint(null);
    }

    private void remountUidExternalStorage(int uid, int mode) {
        try {
            this.mVold.remountUid(uid, mode);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void setDebugFlags(int flags, int mask) {
        String value;
        long token;
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        boolean z = true;
        if ((mask & 4) != 0) {
            if (StorageManager.isFileEncryptedNativeOnly()) {
                throw new IllegalStateException("Emulation not supported on device with native FBE");
            } else if (this.mLockPatternUtils.isCredentialRequiredToDecrypt(false)) {
                throw new IllegalStateException("Emulation requires disabling 'Secure start-up' in Settings > Security");
            } else {
                long token2 = Binder.clearCallingIdentity();
                try {
                    SystemProperties.set("persist.sys.emulate_fbe", Boolean.toString((flags & 4) != 0));
                    ((PowerManager) this.mContext.getSystemService(PowerManager.class)).reboot(null);
                } finally {
                    Binder.restoreCallingIdentity(token2);
                }
            }
        }
        if ((mask & 3) != 0) {
            if ((flags & 1) != 0) {
                value = "force_on";
            } else if ((flags & 2) != 0) {
                value = "force_off";
            } else {
                value = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            token = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.adoptable", value);
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        if ((mask & 24) != 0) {
            if ((flags & 8) != 0) {
                value = "force_on";
            } else if ((flags & 16) != 0) {
                value = "force_off";
            } else {
                value = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            token = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.sdcardfs", value);
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        if ((mask & 32) != 0) {
            if ((flags & 32) == 0) {
                z = false;
            }
            boolean enabled = z;
            long token3 = Binder.clearCallingIdentity();
            try {
                SystemProperties.set("persist.sys.virtual_disk", Boolean.toString(enabled));
                this.mHandler.obtainMessage(10).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(token3);
            }
        }
    }

    public String getPrimaryStorageUuid() {
        String str;
        synchronized (this.mLock) {
            str = this.mPrimaryStorageUuid;
        }
        return str;
    }

    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r8.mVold.moveStorage(r2.id, r3.id, new com.android.server.StorageManagerService.AnonymousClass12(r8));
     */
    /* JADX WARNING: Missing block: B:35:0x00d0, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:36:0x00d1, code skipped:
            android.util.Slog.wtf(TAG, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPrimaryStorageUuid(String volumeUuid, IPackageMoveObserver callback) {
        enforcePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        synchronized (this.mLock) {
            if (Objects.equals(this.mPrimaryStorageUuid, volumeUuid)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Primary storage already at ");
                stringBuilder.append(volumeUuid);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.mMoveCallback == null) {
                this.mMoveCallback = callback;
                this.mMoveTargetUuid = volumeUuid;
                for (UserInfo user : ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers()) {
                    if (StorageManager.isFileEncryptedNativeOrEmulated() && !isUserKeyUnlocked(user.id)) {
                        String str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failing move due to locked user ");
                        stringBuilder2.append(user.id);
                        Slog.w(str, stringBuilder2.toString());
                        onMoveStatusLocked(-10);
                        return;
                    }
                }
                if (!Objects.equals("primary_physical", this.mPrimaryStorageUuid)) {
                    if (!Objects.equals("primary_physical", volumeUuid)) {
                        VolumeInfo from = findStorageForUuid(this.mPrimaryStorageUuid);
                        VolumeInfo to = findStorageForUuid(volumeUuid);
                        String str2;
                        StringBuilder stringBuilder3;
                        if (from == null) {
                            str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Failing move due to missing from volume ");
                            stringBuilder3.append(this.mPrimaryStorageUuid);
                            Slog.w(str2, stringBuilder3.toString());
                            onMoveStatusLocked(-6);
                            return;
                        } else if (to == null) {
                            str2 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Failing move due to missing to volume ");
                            stringBuilder3.append(volumeUuid);
                            Slog.w(str2, stringBuilder3.toString());
                            onMoveStatusLocked(-6);
                            return;
                        }
                    }
                }
                Slog.d(TAG, "Skipping move to/from primary physical");
                onMoveStatusLocked(82);
                onMoveStatusLocked(-100);
                this.mHandler.obtainMessage(10).sendToTarget();
            } else {
                throw new IllegalStateException("Move already in progress");
            }
        }
    }

    private void warnOnNotMounted() {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mVolumes.size(); i++) {
                VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                if (vol.isPrimary() && vol.isMountedWritable()) {
                    return;
                }
            }
            Slog.w(TAG, "No primary storage mounted!");
        }
    }

    private boolean isUidOwnerOfPackageOrSystem(String packageName, int callerUid) {
        boolean z = true;
        if (callerUid == 1000) {
            return true;
        }
        if (packageName == null) {
            return false;
        }
        if (callerUid != this.mPms.getPackageUid(packageName, 268435456, UserHandle.getUserId(callerUid))) {
            z = false;
        }
        return z;
    }

    public String getMountedObbPath(String rawPath) {
        ObbState state;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        warnOnNotMounted();
        synchronized (this.mObbMounts) {
            state = (ObbState) this.mObbPathToStateMap.get(rawPath);
        }
        if (state != null) {
            return findVolumeByIdOrThrow(state.volId).getPath().getAbsolutePath();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to find OBB mounted at ");
        stringBuilder.append(rawPath);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    public boolean isObbMounted(String rawPath) {
        boolean containsKey;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            containsKey = this.mObbPathToStateMap.containsKey(rawPath);
        }
        return containsKey;
    }

    public void mountObb(String rawPath, String canonicalPath, String key, IObbActionListener token, int nonce) {
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        Preconditions.checkNotNull(canonicalPath, "canonicalPath cannot be null");
        Preconditions.checkNotNull(token, "token cannot be null");
        int callingUid = Binder.getCallingUid();
        this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, new MountObbAction(new ObbState(rawPath, canonicalPath, callingUid, token, nonce, null), key, callingUid)));
    }

    public void unmountObb(String rawPath, boolean force, IObbActionListener token, int nonce) {
        ObbState existingState;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            existingState = (ObbState) this.mObbPathToStateMap.get(rawPath);
        }
        if (existingState != null) {
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, new UnmountObbAction(new ObbState(rawPath, existingState.canonicalPath, Binder.getCallingUid(), token, nonce, existingState.volId), force)));
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown OBB mount at ");
        stringBuilder.append(rawPath);
        Slog.w(str, stringBuilder.toString());
    }

    public int getEncryptionState() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.fdeComplete();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int decryptStorage(String password) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeCheckPassword(password);
            this.mHandler.postDelayed(new -$$Lambda$StorageManagerService$FwBKfigwURaFfB6T4-qmNa5mSBY(this), 1000);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public static /* synthetic */ void lambda$decryptStorage$1(StorageManagerService storageManagerService) {
        try {
            storageManagerService.mVold.fdeRestart();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public int encryptStorage(int type, String password) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (type == 1) {
            password = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeEnable(type, password, 0);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int changeEncryptionPassword(int type, String password) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (StorageManager.isFileEncryptedNativeOnly()) {
            return -1;
        }
        if (type == 1) {
            password = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        try {
            this.mVold.fdeChangePassword(type, password);
            return 0;
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public int verifyEncryptionPassword(String password) throws RemoteException {
        if (Binder.getCallingUid() == 1000) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
            if (TextUtils.isEmpty(password)) {
                throw new IllegalArgumentException("password cannot be empty");
            }
            try {
                this.mVold.fdeVerifyPassword(password);
                return 0;
            } catch (Exception e) {
                Slog.wtf(TAG, e);
                return -1;
            }
        }
        throw new SecurityException("no permission to access the crypt keeper");
    }

    public int getPasswordType() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.fdeGetPasswordType();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return -1;
        }
    }

    public void setField(String field, String contents) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (!StorageManager.isFileEncryptedNativeOnly()) {
            try {
                this.mVold.fdeSetField(field, contents);
            } catch (Exception e) {
                Slog.wtf(TAG, e);
            }
        }
    }

    public String getField(String field) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (StorageManager.isFileEncryptedNativeOnly()) {
            return null;
        }
        try {
            return this.mVold.fdeGetField(field);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return null;
        }
    }

    public boolean isConvertibleToFBE() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        try {
            return this.mVold.isConvertibleToFbe();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return false;
        }
    }

    public String getPassword() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "only keyguard can retrieve password");
        try {
            return this.mVold.fdeGetPassword();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
            return null;
        }
    }

    public void clearPassword() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "only keyguard can clear password");
        try {
            this.mVold.fdeClearPassword();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void createUserKey(int userId, int serialNumber, boolean ephemeral) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.createUserKey(userId, serialNumber, ephemeral);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void destroyUserKey(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.destroyUserKey(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    protected String encodeBytes(byte[] bytes) {
        if (ArrayUtils.isEmpty(bytes)) {
            return "!";
        }
        return HexDump.toHexString(bytes);
    }

    public void addUserKeyAuth(int userId, int serialNumber, byte[] token, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.addUserKeyAuth(userId, serialNumber, encodeBytes(token), encodeBytes(secret));
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void fixateNewestUserKeyAuth(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.fixateNewestUserKeyAuth(userId);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void unlockUserKey(int userId, int serialNumber, byte[] token, byte[] secret) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        if (StorageManager.isFileEncryptedNativeOrEmulated()) {
            if (this.mLockPatternUtils.isSecure(userId) && ArrayUtils.isEmpty(secret)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Secret required to unlock secure user ");
                stringBuilder.append(userId);
                throw new IllegalStateException(stringBuilder.toString());
            }
            try {
                this.mVold.unlockUserKey(userId, serialNumber, encodeBytes(token), encodeBytes(secret));
            } catch (Exception e) {
                Slog.wtf(TAG, e);
                return;
            }
        }
        synchronized (this.mLock) {
            this.mLocalUnlockedUsers = ArrayUtils.appendInt(this.mLocalUnlockedUsers, userId);
        }
    }

    public void lockUserKey(int userId) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.lockUserKey(userId);
            synchronized (this.mLock) {
                this.mLocalUnlockedUsers = ArrayUtils.removeInt(this.mLocalUnlockedUsers, userId);
            }
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public boolean isUserKeyUnlocked(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mLocalUnlockedUsers, userId);
        }
        return contains;
    }

    public void prepareUserStorage(String volumeUuid, int userId, int serialNumber, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.prepareUserStorage(volumeUuid, userId, serialNumber, flags);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public void destroyUserStorage(String volumeUuid, int userId, int flags) {
        enforcePermission("android.permission.STORAGE_INTERNAL");
        try {
            this.mVold.destroyUserStorage(volumeUuid, userId, flags);
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public AppFuseMount mountProxyFileDescriptorBridge() {
        Slog.v(TAG, "mountProxyFileDescriptorBridge");
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        while (true) {
            AppFuseMount appFuseMount;
            synchronized (this.mAppFuseLock) {
                boolean newlyCreated = false;
                if (this.mAppFuseBridge == null) {
                    this.mAppFuseBridge = new AppFuseBridge();
                    new Thread(this.mAppFuseBridge, AppFuseBridge.TAG).start();
                    newlyCreated = true;
                }
                try {
                    int name = this.mNextAppFuseName;
                    this.mNextAppFuseName = name + 1;
                    appFuseMount = new AppFuseMount(name, this.mAppFuseBridge.addBridge(new AppFuseMountScope(uid, pid, name)));
                    break;
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                } catch (FuseUnavailableMountException e2) {
                    if (newlyCreated) {
                        Slog.e(TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e2);
                        return null;
                    }
                    this.mAppFuseBridge = null;
                }
            }
            return appFuseMount;
        }
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int mountId, int fileId, int mode) {
        Slog.v(TAG, "mountProxyFileDescriptor");
        int pid = Binder.getCallingPid();
        try {
            synchronized (this.mAppFuseLock) {
                if (this.mAppFuseBridge == null) {
                    Slog.e(TAG, "FuseBridge has not been created");
                    return null;
                }
                ParcelFileDescriptor openFile = this.mAppFuseBridge.openFile(pid, mountId, fileId, mode);
                return openFile;
            }
        } catch (FuseUnavailableMountException | InterruptedException error) {
            Slog.v(TAG, "The mount point has already been invalid", error);
            return null;
        }
    }

    public void mkdirs(String callingPkg, String appPath) {
        StringBuilder stringBuilder;
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        UserEnvironment userEnv = new UserEnvironment(userId);
        String propertyName = new StringBuilder();
        propertyName.append("sys.user.");
        propertyName.append(userId);
        propertyName.append(".ce_available");
        propertyName = propertyName.toString();
        StringBuilder stringBuilder2;
        if (!isUserKeyUnlocked(userId)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to prepare ");
            stringBuilder2.append(appPath);
            throw new IllegalStateException(stringBuilder2.toString());
        } else if (userId != 0 || SystemProperties.getBoolean(propertyName, false)) {
            ((AppOpsManager) this.mContext.getSystemService("appops")).checkPackage(Binder.getCallingUid(), callingPkg);
            try {
                File appFile = new File(appPath).getCanonicalFile();
                if (FileUtils.contains(userEnv.buildExternalStorageAppDataDirs(callingPkg), appFile) || FileUtils.contains(userEnv.buildExternalStorageAppObbDirs(callingPkg), appFile) || FileUtils.contains(userEnv.buildExternalStorageAppMediaDirs(callingPkg), appFile)) {
                    appPath = appFile.getAbsolutePath();
                    if (!appPath.endsWith(SliceAuthority.DELIMITER)) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(appPath);
                        stringBuilder3.append(SliceAuthority.DELIMITER);
                        appPath = stringBuilder3.toString();
                    }
                    try {
                        this.mVold.mkdirs(appPath);
                        return;
                    } catch (Exception e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to prepare ");
                        stringBuilder.append(appPath);
                        stringBuilder.append(": ");
                        stringBuilder.append(e);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Invalid mkdirs path: ");
                stringBuilder4.append(appFile);
                throw new SecurityException(stringBuilder4.toString());
            } catch (IOException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to resolve ");
                stringBuilder.append(appPath);
                stringBuilder.append(": ");
                stringBuilder.append(e2);
                throw new IllegalStateException(stringBuilder.toString());
            }
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to prepare ");
            stringBuilder2.append(appPath);
            throw new IllegalStateException(stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Missing block: B:54:0x00b4, code skipped:
            if (r2.getPath() != null) goto L_0x00b9;
     */
    /* JADX WARNING: Missing block: B:79:0x0101, code skipped:
            if (r17 != false) goto L_0x0156;
     */
    /* JADX WARNING: Missing block: B:80:0x0103, code skipped:
            android.util.Log.w(TAG, "No primary storage defined yet; hacking together a stub");
            r0 = android.os.SystemProperties.getBoolean("ro.vold.primary_physical", false);
            r2 = "stub_primary";
            r4 = android.os.Environment.getLegacyExternalStorageDirectory();
            r16 = "removed";
            r36 = r0;
            r11.add(0, new android.os.storage.StorageVolume("stub_primary", r4, r4, r1.mContext.getString(17039374), true, r0, r0 ^ 1, false, 0, new android.os.UserHandle(r3), null, "removed"));
     */
    /* JADX WARNING: Missing block: B:82:0x0162, code skipped:
            return (android.os.storage.StorageVolume[]) r11.toArray(new android.os.storage.StorageVolume[r11.size()]);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public StorageVolume[] getVolumeList(int uid, String packageName, int flags) {
        Throwable th;
        int i = flags;
        int userId = UserHandle.getUserId(uid);
        boolean forWrite = (i & 256) != 0;
        boolean realState = (i & 512) != 0;
        boolean includeInvisible = (i & 1024) != 0;
        long token = Binder.clearCallingIdentity();
        UserInfo userInfo = null;
        boolean z;
        try {
            boolean userKeyUnlocked = isUserKeyUnlocked(userId);
            boolean storagePermission = this.mStorageManagerInternal.hasExternalStorage(uid, packageName);
            if (this.mCurrentUserId != userId) {
                try {
                    userInfo = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUserInfo(userId);
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            UserInfo userInfo2 = userInfo;
            Binder.restoreCallingIdentity(token);
            boolean userKeyUnlocked2 = userKeyUnlocked;
            userInfo = new ArrayList();
            synchronized (this.mLock) {
                boolean foundPrimary = false;
                int i2 = 0;
                while (i2 < this.mVolumes.size()) {
                    UserInfo userInfo3;
                    String str;
                    VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i2);
                    int type = vol.getType();
                    if (type == 0 || type == 2) {
                        boolean match;
                        boolean z2;
                        int i3;
                        if (forWrite) {
                            if (userInfo2 != null) {
                                try {
                                    if (userInfo2.isClonedProfile()) {
                                        i3 = userInfo2.profileGroupId;
                                        match = vol.isVisibleForWrite(i3);
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    userInfo3 = userInfo2;
                                    z = forWrite;
                                    throw th;
                                }
                            }
                            i3 = userId;
                            match = vol.isVisibleForWrite(i3);
                        } else {
                            i3 = (userInfo2 == null || !userInfo2.isClonedProfile()) ? userId : userInfo2.profileGroupId;
                            try {
                                if (!vol.isVisibleForRead(i3)) {
                                    if (includeInvisible) {
                                    }
                                    z2 = false;
                                    match = z2;
                                }
                                z2 = true;
                                match = z2;
                            } catch (Throwable th4) {
                                th = th4;
                                userInfo3 = userInfo2;
                                z = forWrite;
                                throw th;
                            }
                        }
                        if (match) {
                            z2 = false;
                            userInfo3 = userInfo2;
                            try {
                                z = forWrite;
                                if (vol.getType() == true && !userKeyUnlocked2) {
                                    z2 = true;
                                } else if (!(storagePermission || realState)) {
                                    z2 = true;
                                }
                                userInfo2 = vol.buildStorageVolume(this.mContext, userId, z2);
                                if (vol.isPrimary()) {
                                    userInfo.add(0, userInfo2);
                                    foundPrimary = true;
                                } else {
                                    userInfo.add(userInfo2);
                                }
                                i2++;
                                userInfo2 = userInfo3;
                                forWrite = z;
                                i = flags;
                                type = uid;
                                str = packageName;
                            } catch (Throwable th5) {
                                th = th5;
                                throw th;
                            }
                        }
                    }
                    userInfo3 = userInfo2;
                    z = forWrite;
                    i2++;
                    userInfo2 = userInfo3;
                    forWrite = z;
                    i = flags;
                    type = uid;
                    str = packageName;
                }
                z = forWrite;
            }
        } catch (Throwable th6) {
            th = th6;
            z = forWrite;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public DiskInfo[] getDisks() {
        DiskInfo[] res;
        synchronized (this.mLock) {
            res = new DiskInfo[this.mDisks.size()];
            for (int i = 0; i < this.mDisks.size(); i++) {
                res[i] = (DiskInfo) this.mDisks.valueAt(i);
            }
        }
        return res;
    }

    public VolumeInfo[] getVolumes(int flags) {
        VolumeInfo[] volumeInfoArr;
        synchronized (this.mLock) {
            ArrayList<VolumeInfo> list = new ArrayList();
            int privacy_id = -1;
            if (SystemProperties.getBoolean(PROPERTIES_PRIVACY_SPACE_HIDSD, true)) {
                long token = Binder.clearCallingIdentity();
                try {
                    privacy_id = getPrivacySpaceUserId();
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
            int i = 0;
            while (i < this.mVolumes.size()) {
                if (UserHandle.getUserId(Binder.getCallingUid()) != privacy_id || !isExternalSDcard((VolumeInfo) this.mVolumes.valueAt(i)) || UserHandle.getAppId(Binder.getCallingUid()) == 1000) {
                    list.add((VolumeInfo) this.mVolumes.valueAt(i));
                }
                i++;
            }
            volumeInfoArr = (VolumeInfo[]) list.toArray(new VolumeInfo[list.size()]);
        }
        return volumeInfoArr;
    }

    public VolumeRecord[] getVolumeRecords(int flags) {
        VolumeRecord[] res;
        synchronized (this.mLock) {
            res = new VolumeRecord[this.mRecords.size()];
            for (int i = 0; i < this.mRecords.size(); i++) {
                res[i] = (VolumeRecord) this.mRecords.valueAt(i);
            }
        }
        return res;
    }

    public long getCacheQuotaBytes(String volumeUuid, int uid) {
        if (uid != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long token = Binder.clearCallingIdentity();
        try {
            long cacheQuotaBytes = ((StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class)).getCacheQuotaBytes(volumeUuid, uid);
            return cacheQuotaBytes;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getCacheSizeBytes(String volumeUuid, int uid) {
        if (uid != Binder.getCallingUid()) {
            this.mContext.enforceCallingPermission("android.permission.STORAGE_INTERNAL", TAG);
        }
        long token = Binder.clearCallingIdentity();
        try {
            long cacheBytes = ((StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class)).queryStatsForUid(volumeUuid, uid).getCacheBytes();
            Binder.restoreCallingIdentity(token);
            return cacheBytes;
        } catch (IOException e) {
            throw new ParcelableException(e);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private int adjustAllocateFlags(int flags, int callingUid, String callingPackage) {
        if ((flags & 1) != 0) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ALLOCATE_AGGRESSIVE", TAG);
        }
        flags = (flags & -3) & -5;
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        long token = Binder.clearCallingIdentity();
        try {
            if (appOps.isOperationActive(26, callingUid, callingPackage)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UID ");
                stringBuilder.append(callingUid);
                stringBuilder.append(" is actively using camera; letting them defy reserved cached data");
                Slog.d(str, stringBuilder.toString());
                flags |= 4;
            }
            Binder.restoreCallingIdentity(token);
            return flags;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getAllocatableBytes(String volumeUuid, int flags, String callingPackage) {
        IOException e;
        Throwable th;
        String str = volumeUuid;
        int flags2 = adjustAllocateFlags(flags, Binder.getCallingUid(), callingPackage);
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        StorageStatsManager stats = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        long token = Binder.clearCallingIdentity();
        StorageManager storageManager;
        try {
            File path = storage.findPathForUuid(str);
            long usable = path.getUsableSpace();
            long lowReserved = storage.getStorageLowBytes(path);
            long fullReserved = storage.getStorageFullBytes(path);
            long lowReserved2 = lowReserved;
            if (stats.isQuotaSupported(str) && SystemProperties.getInt("persist.sys.install_no_quota", 1) == 1) {
                lowReserved = stats.getCacheBytes(str);
                try {
                    path = Math.max(0, lowReserved - storage.getStorageCacheBytes(path, flags2));
                    long max;
                    if ((flags2 & 1) != 0) {
                        max = Math.max(0, (usable + path) - fullReserved);
                        Binder.restoreCallingIdentity(token);
                        return max;
                    }
                    max = Math.max(0, (usable + path) - lowReserved2);
                    Binder.restoreCallingIdentity(token);
                    return max;
                } catch (IOException e2) {
                    e = e2;
                    try {
                        throw new ParcelableException(e);
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(token);
                        throw th;
                    }
                }
            }
            storageManager = storage;
            long max2;
            if ((flags2 & 1) != 0) {
                max2 = Math.max(0, usable - fullReserved);
                Binder.restoreCallingIdentity(token);
                return max2;
            }
            max2 = Math.max(0, usable - lowReserved2);
            Binder.restoreCallingIdentity(token);
            return max2;
        } catch (IOException e3) {
            e = e3;
            storageManager = storage;
            throw new ParcelableException(e);
        } catch (Throwable th3) {
            th = th3;
            storageManager = storage;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public void allocateBytes(String volumeUuid, long bytes, int flags, String callingPackage) {
        flags = adjustAllocateFlags(flags, Binder.getCallingUid(), callingPackage);
        long allocatableBytes = getAllocatableBytes(volumeUuid, flags, callingPackage);
        if (bytes <= allocatableBytes) {
            StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
            long token = Binder.clearCallingIdentity();
            try {
                File path = storage.findPathForUuid(volumeUuid);
                if ((flags & 1) != 0) {
                    bytes += storage.getStorageFullBytes(path);
                } else {
                    bytes += storage.getStorageLowBytes(path);
                }
                this.mPms.freeStorage(volumeUuid, bytes, flags);
                Binder.restoreCallingIdentity(token);
            } catch (IOException e) {
                throw new ParcelableException(e);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to allocate ");
            stringBuilder.append(bytes);
            stringBuilder.append(" because only ");
            stringBuilder.append(allocatableBytes);
            stringBuilder.append(" allocatable");
            throw new ParcelableException(new IOException(stringBuilder.toString()));
        }
    }

    private void addObbStateLocked(ObbState obbState) throws RemoteException {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = (List) this.mObbMounts.get(binder);
        if (obbStates == null) {
            obbStates = new ArrayList();
            this.mObbMounts.put(binder, obbStates);
        } else {
            for (ObbState o : obbStates) {
                if (o.rawPath.equals(obbState.rawPath)) {
                    throw new IllegalStateException("Attempt to add ObbState twice. This indicates an error in the StorageManagerService logic.");
                }
            }
        }
        obbStates.add(obbState);
        try {
            obbState.link();
            this.mObbPathToStateMap.put(obbState.rawPath, obbState);
        } catch (RemoteException e) {
            obbStates.remove(obbState);
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
            throw e;
        }
    }

    private void removeObbStateLocked(ObbState obbState) {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = (List) this.mObbMounts.get(binder);
        if (obbStates != null) {
            if (obbStates.remove(obbState)) {
                obbState.unlink();
            }
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
        }
        this.mObbPathToStateMap.remove(obbState.rawPath);
    }

    private void dispatchOnStatus(IVoldTaskListener listener, int status, PersistableBundle extras) {
        if (listener != null) {
            try {
                listener.onStatus(status, extras);
            } catch (RemoteException e) {
            }
        }
    }

    private void dispatchOnFinished(IVoldTaskListener listener, int status, PersistableBundle extras) {
        if (listener != null) {
            try {
                listener.onFinished(status, extras);
            } catch (RemoteException e) {
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, writer)) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ", 160);
            synchronized (this.mLock) {
                int i;
                pw.println("Disks:");
                pw.increaseIndent();
                int i2 = 0;
                for (i = 0; i < this.mDisks.size(); i++) {
                    ((DiskInfo) this.mDisks.valueAt(i)).dump(pw);
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("Volumes:");
                pw.increaseIndent();
                for (i = 0; i < this.mVolumes.size(); i++) {
                    VolumeInfo vol = (VolumeInfo) this.mVolumes.valueAt(i);
                    if (!"private".equals(vol.id)) {
                        vol.dump(pw);
                    }
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("Records:");
                pw.increaseIndent();
                while (i2 < this.mRecords.size()) {
                    ((VolumeRecord) this.mRecords.valueAt(i2)).dump(pw);
                    i2++;
                }
                pw.decreaseIndent();
                pw.println();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Primary storage UUID: ");
                stringBuilder.append(this.mPrimaryStorageUuid);
                pw.println(stringBuilder.toString());
                Pair<String, Long> pair = StorageManager.getPrimaryStoragePathAndSize();
                if (pair == null) {
                    pw.println("Internal storage total size: N/A");
                } else {
                    pw.print("Internal storage (");
                    pw.print((String) pair.first);
                    pw.print(") total size: ");
                    pw.print(pair.second);
                    pw.print(" (");
                    pw.print(DataUnit.MEBIBYTES.toBytes(((Long) pair.second).longValue()));
                    pw.println(" MiB)");
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Local unlocked users: ");
                stringBuilder2.append(Arrays.toString(this.mLocalUnlockedUsers));
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("System unlocked users: ");
                stringBuilder2.append(Arrays.toString(this.mSystemUnlockedUsers));
                pw.println(stringBuilder2.toString());
            }
            synchronized (this.mObbMounts) {
                pw.println();
                pw.println("mObbMounts:");
                pw.increaseIndent();
                for (Entry<IBinder, List<ObbState>> e : this.mObbMounts.entrySet()) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(e.getKey());
                    stringBuilder3.append(":");
                    pw.println(stringBuilder3.toString());
                    pw.increaseIndent();
                    for (ObbState obbState : (List) e.getValue()) {
                        pw.println(obbState);
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
                pw.println();
                pw.println("mObbPathToStateMap:");
                pw.increaseIndent();
                for (Entry<String, ObbState> e2 : this.mObbPathToStateMap.entrySet()) {
                    pw.print((String) e2.getKey());
                    pw.print(" -> ");
                    pw.println(e2.getValue());
                }
                pw.decreaseIndent();
            }
            pw.println();
            pw.print("Last maintenance: ");
            pw.println(TimeUtils.formatForLogging(this.mLastMaintenance));
        }
    }

    public void monitor() {
        try {
            this.mVold.monitor();
        } catch (Exception e) {
            Slog.wtf(TAG, e);
        }
    }

    public boolean isSecure() {
        return new LockPatternUtils(this.mContext).isSecure(ActivityManager.getCurrentUser());
    }

    public boolean isSecureEx(int userId) {
        return new LockPatternUtils(this.mContext).isSecure(userId);
    }

    public void onCryptsdMessage(String message) {
    }

    public int getUsbDeviceExInfo() {
        return -1;
    }

    protected void checkIfBackUpDeviceMount(int deviceCode) {
    }
}

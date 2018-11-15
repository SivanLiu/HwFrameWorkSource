package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ServiceWatcher implements ServiceConnection {
    public static final String EXTRA_SERVICE_IS_MULTIUSER = "serviceIsMultiuser";
    public static final String EXTRA_SERVICE_VERSION = "serviceVersion";
    private static final int MAX_REBIND_NUM = 3;
    private static final long RETRY_TIME_INTERVAL = 5000;
    private final String mAction;
    @GuardedBy("mLock")
    private ComponentName mBoundComponent;
    @GuardedBy("mLock")
    private String mBoundPackageName;
    @GuardedBy("mLock")
    private IBinder mBoundService;
    @GuardedBy("mLock")
    private int mBoundUserId = -10000;
    @GuardedBy("mLock")
    private int mBoundVersion = Integer.MIN_VALUE;
    private final Context mContext;
    @GuardedBy("mLock")
    private int mCurrentUserId = 0;
    private final Handler mHandler;
    private final Object mLock = new Object();
    private final Runnable mNewServiceWork;
    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        public void onPackageUpdateFinished(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public void onPackageAdded(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public void onPackageRemoved(String packageName, int uid) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName));
            }
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            synchronized (ServiceWatcher.this.mLock) {
                ServiceWatcher.this.bindBestPackageLocked(null, Objects.equals(packageName, ServiceWatcher.this.mBoundPackageName));
            }
            return super.onPackageChanged(packageName, uid, components);
        }
    };
    private final PackageManager mPm;
    private int mRetryCount;
    Runnable mRetryRunnable = new Runnable() {
        /* JADX WARNING: Missing block: B:12:0x006e, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (ServiceWatcher.this.mLock) {
                if (ServiceWatcher.this.mBoundService != null) {
                } else if (ServiceWatcher.this.mRetryCount < 3) {
                    String access$300 = ServiceWatcher.this.mTag;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("rebind count:");
                    stringBuilder.append(ServiceWatcher.this.mRetryCount);
                    Log.w(access$300, stringBuilder.toString());
                    ServiceWatcher.this.mRetryCount = ServiceWatcher.this.mRetryCount + 1;
                    ServiceWatcher.this.unbindLocked();
                    ServiceWatcher.this.bindBestPackageLocked(ServiceWatcher.this.mServicePackageName, false);
                    ServiceWatcher.this.mHandler.postDelayed(ServiceWatcher.this.mRetryRunnable, ServiceWatcher.RETRY_TIME_INTERVAL);
                } else {
                    Log.e(ServiceWatcher.this.mTag, "max rebind failed");
                }
            }
        }
    };
    private final String mServicePackageName;
    private final List<HashSet<Signature>> mSignatureSets;
    private final String mTag;

    public interface BinderRunner {
        void run(IBinder iBinder);
    }

    public static ArrayList<HashSet<Signature>> getSignatureSets(Context context, List<String> initialPackageNames) {
        PackageManager pm = context.getPackageManager();
        ArrayList<HashSet<Signature>> sigSets = new ArrayList();
        int size = initialPackageNames.size();
        for (int i = 0; i < size; i++) {
            String pkg = (String) initialPackageNames.get(i);
            try {
                HashSet<Signature> set = new HashSet();
                set.addAll(Arrays.asList(pm.getPackageInfo(pkg, 1048640).signatures));
                sigSets.add(set);
            } catch (NameNotFoundException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(pkg);
                stringBuilder.append(" not found");
                Log.w("ServiceWatcher", stringBuilder.toString());
            }
        }
        return sigSets;
    }

    public ServiceWatcher(Context context, String logTag, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Runnable newServiceWork, Handler handler) {
        this.mContext = context;
        this.mTag = logTag;
        this.mAction = action;
        this.mPm = this.mContext.getPackageManager();
        this.mNewServiceWork = newServiceWork;
        this.mHandler = handler;
        Resources resources = context.getResources();
        boolean enableOverlay = resources.getBoolean(overlaySwitchResId);
        ArrayList<String> initialPackageNames = new ArrayList();
        String str;
        StringBuilder stringBuilder;
        if (enableOverlay) {
            String[] pkgs = resources.getStringArray(initialPackageNamesResId);
            if (pkgs != null) {
                initialPackageNames.addAll(Arrays.asList(pkgs));
            }
            this.mServicePackageName = null;
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Overlay enabled, packages=");
            stringBuilder.append(Arrays.toString(pkgs));
            Log.i(str, stringBuilder.toString());
        } else {
            String servicePackageName = resources.getString(defaultServicePackageNameResId);
            if (servicePackageName != null) {
                initialPackageNames.add(servicePackageName);
            }
            this.mServicePackageName = servicePackageName;
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Overlay disabled, default package=");
            stringBuilder.append(servicePackageName);
            Log.i(str, stringBuilder.toString());
        }
        this.mSignatureSets = getSignatureSets(context, initialPackageNames);
    }

    public boolean start() {
        if (isServiceMissing()) {
            return false;
        }
        synchronized (this.mLock) {
            bindBestPackageLocked(this.mServicePackageName, false);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    ServiceWatcher.this.switchUser(userId);
                } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    ServiceWatcher.this.unlockUser(userId);
                }
            }
        }, UserHandle.ALL, intentFilter, null, this.mHandler);
        if (this.mServicePackageName == null) {
            this.mPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
        }
        return true;
    }

    private boolean isServiceMissing() {
        return this.mPm.queryIntentServicesAsUser(new Intent(this.mAction), 786432, this.mCurrentUserId).isEmpty();
    }

    @GuardedBy("mLock")
    private boolean bindBestPackageLocked(String justCheckThisPackage, boolean forceRebind) {
        String str;
        StringBuilder stringBuilder;
        String str2 = justCheckThisPackage;
        Intent intent = new Intent(this.mAction);
        if (str2 != null) {
            intent.setPackage(str2);
        }
        List<ResolveInfo> rInfos = this.mPm.queryIntentServicesAsUser(intent, 268435584, this.mCurrentUserId);
        int bestVersion = Integer.MIN_VALUE;
        ComponentName bestComponent = null;
        boolean bestIsMultiuser = false;
        if (rInfos != null) {
            String str3;
            String str4;
            String str5;
            boolean bestIsMultiuser2 = false;
            ComponentName bestComponent2 = null;
            int bestVersion2 = Integer.MIN_VALUE;
            for (ResolveInfo rInfo : rInfos) {
                ComponentName component = rInfo.serviceInfo.getComponentName();
                String packageName = component.getPackageName();
                try {
                    if (isSignatureMatch(this.mPm.getPackageInfo(packageName, 268435520).signatures)) {
                        bestVersion = Integer.MIN_VALUE;
                        boolean isMultiuser = false;
                        if (rInfo.serviceInfo.metaData != null) {
                            bestVersion = rInfo.serviceInfo.metaData.getInt(EXTRA_SERVICE_VERSION, Integer.MIN_VALUE);
                            isMultiuser = rInfo.serviceInfo.metaData.getBoolean(EXTRA_SERVICE_IS_MULTIUSER);
                        }
                        if (!HwServiceFactory.getHwNLPManager().skipForeignNlpPackage(this.mAction, packageName)) {
                            if (HwServiceFactory.getHwNLPManager().useCivilNlpPackage(this.mAction, packageName)) {
                                str3 = this.mTag;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(packageName);
                                stringBuilder2.append(" useCivilNlpPackage");
                                Log.d(str3, stringBuilder2.toString());
                                bestComponent = component;
                                bestIsMultiuser = isMultiuser;
                                break;
                            } else if (bestVersion > bestVersion2) {
                                bestVersion2 = bestVersion;
                                bestComponent2 = component;
                                bestIsMultiuser2 = isMultiuser;
                            }
                        }
                    } else {
                        String str6 = this.mTag;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(packageName);
                        stringBuilder3.append(" resolves service ");
                        stringBuilder3.append(this.mAction);
                        stringBuilder3.append(", but has wrong signature, ignoring");
                        Log.w(str6, stringBuilder3.toString());
                    }
                } catch (NameNotFoundException e) {
                    Log.wtf(this.mTag, e);
                }
            }
            bestVersion = bestVersion2;
            bestComponent = bestComponent2;
            bestIsMultiuser = bestIsMultiuser2;
            str = this.mTag;
            str3 = "bindBestPackage for %s : %s found %d, %s";
            Object[] objArr = new Object[4];
            objArr[0] = this.mAction;
            if (str2 == null) {
                str4 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            } else {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("(");
                stringBuilder4.append(str2);
                stringBuilder4.append(") ");
                str4 = stringBuilder4.toString();
            }
            objArr[1] = str4;
            objArr[2] = Integer.valueOf(rInfos.size());
            if (bestComponent == null) {
                str5 = "no new best component";
            } else {
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("new best component: ");
                stringBuilder5.append(bestComponent);
                str5 = stringBuilder5.toString();
            }
            objArr[3] = str5;
            Log.i(str, String.format(str3, objArr));
        } else {
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to query intent services for action: ");
            stringBuilder.append(this.mAction);
            Log.i(str, stringBuilder.toString());
        }
        if (bestComponent == null) {
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Odd, no component found for service ");
            stringBuilder.append(this.mAction);
            Slog.w(str, stringBuilder.toString());
            unbindLocked();
            return false;
        }
        boolean z = false;
        int userId = bestIsMultiuser ? 0 : this.mCurrentUserId;
        if (Objects.equals(bestComponent, this.mBoundComponent) && bestVersion == this.mBoundVersion && userId == this.mBoundUserId) {
            z = true;
        }
        boolean alreadyBound = z;
        if (forceRebind || !alreadyBound) {
            unbindLocked();
            bindToPackageLocked(bestComponent, bestVersion, userId);
        }
        return true;
    }

    @GuardedBy("mLock")
    private void unbindLocked() {
        ComponentName component = this.mBoundComponent;
        this.mBoundComponent = null;
        this.mBoundPackageName = null;
        this.mBoundVersion = Integer.MIN_VALUE;
        this.mBoundUserId = -10000;
        if (component != null) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unbinding ");
            stringBuilder.append(component);
            Log.i(str, stringBuilder.toString());
            this.mBoundService = null;
            this.mContext.unbindService(this);
        }
    }

    @GuardedBy("mLock")
    private void bindToPackageLocked(ComponentName component, int version, int userId) {
        Intent intent = new Intent(this.mAction);
        intent.setComponent(component);
        this.mBoundComponent = component;
        this.mBoundPackageName = component.getPackageName();
        this.mBoundVersion = version;
        this.mBoundUserId = userId;
        String str = this.mTag;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("binding ");
        stringBuilder.append(component);
        stringBuilder.append(" (v");
        stringBuilder.append(version);
        stringBuilder.append(") (u");
        stringBuilder.append(userId);
        stringBuilder.append(")");
        Log.i(str, stringBuilder.toString());
        this.mContext.bindServiceAsUser(intent, this, 1073741829, new UserHandle(userId));
    }

    /* JADX WARNING: Missing block: B:23:0x0067, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void bindToPackageWithLock(String packageName, int version, boolean isMultiuser) {
        if (packageName != null) {
            synchronized (this.mLock) {
                List<ResolveInfo> rInfos = this.mPm.queryIntentServicesAsUser(new Intent(this.mAction), 268435584, this.mCurrentUserId);
                if (rInfos != null) {
                    for (ResolveInfo rInfo : rInfos) {
                        ComponentName component = rInfo.serviceInfo.getComponentName();
                        if (packageName.equals(component.getPackageName())) {
                            if (rInfo.serviceInfo.metaData != null) {
                                version = rInfo.serviceInfo.metaData.getInt(EXTRA_SERVICE_VERSION, Integer.MIN_VALUE);
                                isMultiuser = rInfo.serviceInfo.metaData.getBoolean(EXTRA_SERVICE_IS_MULTIUSER);
                            }
                            bindToPackageLocked(component, version, isMultiuser ? 0 : this.mCurrentUserId);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static boolean isSignatureMatch(Signature[] signatures, List<HashSet<Signature>> sigSets) {
        if (signatures == null) {
            return false;
        }
        HashSet<Signature> inputSet = new HashSet();
        for (Signature s : signatures) {
            inputSet.add(s);
        }
        for (HashSet<Signature> referenceSet : sigSets) {
            if (referenceSet.equals(inputSet)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSignatureMatch(Signature[] signatures) {
        return isSignatureMatch(signatures, this.mSignatureSets);
    }

    public void onServiceConnected(ComponentName component, IBinder binder) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (component.equals(this.mBoundComponent)) {
                str = this.mTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append(component);
                stringBuilder.append(" connected");
                Log.i(str, stringBuilder.toString());
                this.mBoundService = binder;
                if (!(this.mHandler == null || this.mNewServiceWork == null)) {
                    this.mHandler.post(this.mNewServiceWork);
                }
            } else {
                str = this.mTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected onServiceConnected: ");
                stringBuilder.append(component);
                Log.w(str, stringBuilder.toString());
            }
        }
    }

    public void onServiceDisconnected(ComponentName component) {
        synchronized (this.mLock) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(component);
            stringBuilder.append(" disconnected");
            Log.i(str, stringBuilder.toString());
            if (component.equals(this.mBoundComponent)) {
                this.mBoundService = null;
                if (this.mHandler != null) {
                    this.mRetryCount = 0;
                    Log.i(this.mTag, "delay rebind begin");
                    this.mHandler.postDelayed(this.mRetryRunnable, RETRY_TIME_INTERVAL);
                }
            }
        }
    }

    public String getBestPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mBoundPackageName;
        }
        return str;
    }

    public int getBestVersion() {
        int i;
        synchronized (this.mLock) {
            i = this.mBoundVersion;
        }
        return i;
    }

    public boolean runOnBinder(BinderRunner runner) {
        synchronized (this.mLock) {
            if (this.mBoundService == null) {
                return false;
            }
            runner.run(this.mBoundService);
            return true;
        }
    }

    public IBinder getBinder() {
        IBinder iBinder;
        synchronized (this.mLock) {
            iBinder = this.mBoundService;
        }
        return iBinder;
    }

    public void switchUser(int userId) {
        synchronized (this.mLock) {
            this.mCurrentUserId = userId;
            bindBestPackageLocked(this.mServicePackageName, false);
        }
    }

    public void unlockUser(int userId) {
        synchronized (this.mLock) {
            if (userId == this.mCurrentUserId) {
                bindBestPackageLocked(this.mServicePackageName, false);
            }
        }
    }
}

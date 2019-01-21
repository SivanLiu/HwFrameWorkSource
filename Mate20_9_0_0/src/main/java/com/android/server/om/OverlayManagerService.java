package com.android.server.om;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager.Stub;
import android.content.om.OverlayInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.FgThread;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;

public final class OverlayManagerService extends SystemService {
    static final boolean DEBUG = false;
    private static final String DEFAULT_OVERLAYS_PROP = "ro.boot.vendor.overlay.theme";
    private static final String FWK_DARK_TAG = "com.android.frameworkhwext.dark";
    static final String TAG = "OverlayManager";
    private final OverlayManagerServiceImpl mImpl;
    private Future<?> mInitCompleteSignal;
    private final Object mLock = new Object();
    private final PackageManagerHelper mPackageManager = new PackageManagerHelper();
    private final AtomicBoolean mPersistSettingsScheduled = new AtomicBoolean(false);
    private final IBinder mService = new Stub() {
        public Map<String, List<OverlayInfo>> getAllOverlays(int userId) throws RemoteException {
            Map overlaysForUser;
            userId = handleIncomingUser(userId, "getAllOverlays");
            synchronized (OverlayManagerService.this.mLock) {
                overlaysForUser = OverlayManagerService.this.mImpl.getOverlaysForUser(userId);
            }
            return overlaysForUser;
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String targetPackageName, int userId) throws RemoteException {
            userId = handleIncomingUser(userId, "getOverlayInfosForTarget");
            if (targetPackageName == null) {
                return Collections.emptyList();
            }
            List overlayInfosForTarget;
            synchronized (OverlayManagerService.this.mLock) {
                overlayInfosForTarget = OverlayManagerService.this.mImpl.getOverlayInfosForTarget(targetPackageName, userId);
            }
            return overlayInfosForTarget;
        }

        public OverlayInfo getOverlayInfo(String packageName, int userId) throws RemoteException {
            userId = handleIncomingUser(userId, "getOverlayInfo");
            if (packageName == null) {
                return null;
            }
            OverlayInfo overlayInfo;
            synchronized (OverlayManagerService.this.mLock) {
                overlayInfo = OverlayManagerService.this.mImpl.getOverlayInfo(packageName, userId);
            }
            return overlayInfo;
        }

        public boolean setEnabled(String packageName, boolean enable, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setEnabled");
            userId = handleIncomingUser(userId, "setEnabled");
            if (packageName == null) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean enabled;
                synchronized (OverlayManagerService.this.mLock) {
                    enabled = OverlayManagerService.this.mImpl.setEnabled(packageName, enable, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return enabled;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setEnabledExclusive(String packageName, boolean enable, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setEnabled");
            userId = handleIncomingUser(userId, "setEnabled");
            if (packageName == null || !enable) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean enabledExclusive;
                synchronized (OverlayManagerService.this.mLock) {
                    enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(packageName, false, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return enabledExclusive;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setEnabledExclusiveInCategory(String packageName, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setEnabled");
            userId = handleIncomingUser(userId, "setEnabled");
            if (packageName == null) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean enabledExclusive;
                synchronized (OverlayManagerService.this.mLock) {
                    enabledExclusive = OverlayManagerService.this.mImpl.setEnabledExclusive(packageName, true, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return enabledExclusive;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setPriority(String packageName, String parentPackageName, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setPriority");
            userId = handleIncomingUser(userId, "setPriority");
            if (packageName == null || parentPackageName == null) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean priority;
                synchronized (OverlayManagerService.this.mLock) {
                    priority = OverlayManagerService.this.mImpl.setPriority(packageName, parentPackageName, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return priority;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setHighestPriority(String packageName, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setHighestPriority");
            userId = handleIncomingUser(userId, "setHighestPriority");
            if (packageName == null) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean highestPriority;
                synchronized (OverlayManagerService.this.mLock) {
                    highestPriority = OverlayManagerService.this.mImpl.setHighestPriority(packageName, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return highestPriority;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setLowestPriority(String packageName, int userId) throws RemoteException {
            enforceChangeOverlayPackagesPermission("setLowestPriority");
            userId = handleIncomingUser(userId, "setLowestPriority");
            if (packageName == null) {
                return false;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                boolean lowestPriority;
                synchronized (OverlayManagerService.this.mLock) {
                    lowestPriority = OverlayManagerService.this.mImpl.setLowestPriority(packageName, userId);
                }
                Binder.restoreCallingIdentity(ident);
                return lowestPriority;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new OverlayManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] argv) {
            enforceDumpPermission("dump");
            boolean z = false;
            if (argv.length > 0 && "--verbose".equals(argv[0])) {
                z = true;
            }
            boolean verbose = z;
            synchronized (OverlayManagerService.this.mLock) {
                OverlayManagerService.this.mImpl.onDump(pw);
                OverlayManagerService.this.mPackageManager.dump(pw, verbose);
            }
        }

        private int handleIncomingUser(int userId, String message) {
            return ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, message, null);
        }

        private void enforceChangeOverlayPackagesPermission(String message) {
            OverlayManagerService.this.getContext().enforceCallingPermission("android.permission.CHANGE_OVERLAY_PACKAGES", message);
        }

        private void enforceDumpPermission(String message) {
            OverlayManagerService.this.getContext().enforceCallingPermission("android.permission.DUMP", message);
        }
    };
    private final OverlayManagerSettings mSettings;
    private final AtomicFile mSettingsFile = new AtomicFile(new File(Environment.getDataSystemDirectory(), "overlays.xml"), "overlays");
    private final UserManagerService mUserManager = UserManagerService.getInstance();

    private final class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        /* synthetic */ PackageReceiver(OverlayManagerService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:28:0x0080  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x007c  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x0080  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x007c  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0072  */
        /* JADX WARNING: Missing block: B:15:0x0056, code skipped:
            if (r7.equals("android.intent.action.PACKAGE_ADDED") == false) goto L_0x006d;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data == null) {
                Slog.e(OverlayManagerService.TAG, "Cannot handle package broadcast with null data");
                return;
            }
            String packageName = data.getSchemeSpecificPart();
            boolean z = false;
            boolean replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            int[] userIds = intent.getIntExtra("android.intent.extra.UID", -10000) == -10000 ? OverlayManagerService.this.mUserManager.getUserIds() : new int[]{UserHandle.getUserId(intent.getIntExtra("android.intent.extra.UID", -10000))};
            String action = intent.getAction();
            int hashCode = action.hashCode();
            if (hashCode != 172491798) {
                if (hashCode != 525384130) {
                    if (hashCode == 1544582882) {
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    z = true;
                    switch (z) {
                        case false:
                            if (!replacing) {
                                onPackageAdded(packageName, userIds);
                                break;
                            } else {
                                onPackageUpgraded(packageName, userIds);
                                break;
                            }
                        case true:
                            onPackageChanged(packageName, userIds);
                            break;
                        case true:
                            if (!replacing) {
                                onPackageRemoved(packageName, userIds);
                                break;
                            } else {
                                onPackageUpgrading(packageName, userIds);
                                break;
                            }
                    }
                }
            } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                }
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
            }
        }

        private void onPackageAdded(String packageName, int[] userIds) {
            for (int userId : userIds) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo pi = OverlayManagerService.this.mPackageManager.getPackageInfo(packageName, userId, false);
                    if (pi != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(packageName, userId, pi);
                        if (pi.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageAdded(packageName, userId);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageAdded(packageName, userId);
                        }
                    }
                }
            }
        }

        private void onPackageChanged(String packageName, int[] userIds) {
            for (int userId : userIds) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo pi = OverlayManagerService.this.mPackageManager.getPackageInfo(packageName, userId, false);
                    if (pi != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(packageName, userId, pi);
                        if (pi.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageChanged(packageName, userId);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageChanged(packageName, userId);
                        }
                    }
                }
            }
        }

        private void onPackageUpgrading(String packageName, int[] userIds) {
            for (int userId : userIds) {
                synchronized (OverlayManagerService.this.mLock) {
                    OverlayManagerService.this.mPackageManager.forgetPackageInfo(packageName, userId);
                    if (OverlayManagerService.this.mImpl.getOverlayInfo(packageName, userId) != null) {
                        OverlayManagerService.this.mImpl.onOverlayPackageUpgrading(packageName, userId);
                    } else {
                        OverlayManagerService.this.mImpl.onTargetPackageUpgrading(packageName, userId);
                    }
                }
            }
        }

        private void onPackageUpgraded(String packageName, int[] userIds) {
            for (int userId : userIds) {
                synchronized (OverlayManagerService.this.mLock) {
                    PackageInfo pi = OverlayManagerService.this.mPackageManager.getPackageInfo(packageName, userId, false);
                    if (pi != null) {
                        OverlayManagerService.this.mPackageManager.cachePackageInfo(packageName, userId, pi);
                        if (pi.isOverlayPackage()) {
                            OverlayManagerService.this.mImpl.onOverlayPackageUpgraded(packageName, userId);
                        } else {
                            OverlayManagerService.this.mImpl.onTargetPackageUpgraded(packageName, userId);
                        }
                    }
                }
            }
        }

        private void onPackageRemoved(String packageName, int[] userIds) {
            for (int userId : userIds) {
                synchronized (OverlayManagerService.this.mLock) {
                    OverlayManagerService.this.mPackageManager.forgetPackageInfo(packageName, userId);
                    if (OverlayManagerService.this.mImpl.getOverlayInfo(packageName, userId) != null) {
                        OverlayManagerService.this.mImpl.onOverlayPackageRemoved(packageName, userId);
                    } else {
                        OverlayManagerService.this.mImpl.onTargetPackageRemoved(packageName, userId);
                    }
                }
            }
        }
    }

    private final class UserReceiver extends BroadcastReceiver {
        private UserReceiver() {
        }

        /* synthetic */ UserReceiver(OverlayManagerService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:40:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x0034  */
        /* JADX WARNING: Removed duplicated region for block: B:40:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x0034  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            Object obj;
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            String action = intent.getAction();
            int hashCode = action.hashCode();
            if (hashCode == -2061058799) {
                if (action.equals("android.intent.action.USER_REMOVED")) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            break;
                        case 1:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 1121780209 && action.equals("android.intent.action.USER_ADDED")) {
                obj = null;
                switch (obj) {
                    case null:
                        if (userId != -10000) {
                            ArrayList<String> targets;
                            synchronized (OverlayManagerService.this.mLock) {
                                targets = OverlayManagerService.this.mImpl.updateOverlaysForUser(userId);
                            }
                            OverlayManagerService.this.updateOverlayPaths(userId, targets);
                            return;
                        }
                        return;
                    case 1:
                        if (userId != -10000) {
                            synchronized (OverlayManagerService.this.mLock) {
                                OverlayManagerService.this.mImpl.onUserRemoved(userId);
                                OverlayManagerService.this.mPackageManager.forgetAllPackageInfos(userId);
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
            obj = -1;
            switch (obj) {
                case null:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
    }

    private final class OverlayChangeListener implements OverlayChangeListener {
        private OverlayChangeListener() {
        }

        /* synthetic */ OverlayChangeListener(OverlayManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onOverlaysChanged(String targetPackageName, int userId) {
            OverlayManagerService.this.schedulePersistSettings();
            FgThread.getHandler().post(new -$$Lambda$OverlayManagerService$OverlayChangeListener$u9oeN2C0PDMo0pYiLqfMBkwuMNA(this, userId, targetPackageName));
        }

        public static /* synthetic */ void lambda$onOverlaysChanged$0(OverlayChangeListener overlayChangeListener, int userId, String targetPackageName) {
            String str = targetPackageName;
            OverlayManagerService.this.updateAssets(userId, str);
            Intent intent = new Intent("android.intent.action.OVERLAY_CHANGED", Uri.fromParts("package", str, null));
            intent.setFlags(67108864);
            try {
                try {
                    ActivityManager.getService().broadcastIntent(null, intent, null, null, 0, null, null, null, -1, null, false, false, userId);
                } catch (RemoteException e) {
                }
            } catch (RemoteException e2) {
                Intent intent2 = intent;
            }
        }
    }

    private static final class PackageManagerHelper implements PackageManagerHelper {
        private static final String TAB1 = "    ";
        private static final String TAB2 = "        ";
        private final SparseArray<HashMap<String, PackageInfo>> mCache = new SparseArray();
        private final IPackageManager mPackageManager = AppGlobals.getPackageManager();
        private final PackageManagerInternal mPackageManagerInternal = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));

        PackageManagerHelper() {
        }

        public PackageInfo getPackageInfo(String packageName, int userId, boolean useCache) {
            PackageInfo cachedPi;
            if (useCache) {
                cachedPi = getCachedPackageInfo(packageName, userId);
                if (cachedPi != null) {
                    return cachedPi;
                }
            }
            try {
                cachedPi = this.mPackageManager.getPackageInfo(packageName, 0, userId);
                if (useCache && cachedPi != null) {
                    cachePackageInfo(packageName, userId, cachedPi);
                }
                return cachedPi;
            } catch (RemoteException e) {
                return null;
            }
        }

        public PackageInfo getPackageInfo(String packageName, int userId) {
            return getPackageInfo(packageName, userId, true);
        }

        public boolean signaturesMatching(String packageName1, String packageName2, int userId) {
            boolean z = false;
            try {
                if (this.mPackageManager.checkSignatures(packageName1, packageName2) == 0) {
                    z = true;
                }
                return z;
            } catch (RemoteException e) {
                return false;
            }
        }

        public List<PackageInfo> getOverlayPackages(int userId) {
            return this.mPackageManagerInternal.getOverlayPackages(userId);
        }

        public PackageInfo getCachedPackageInfo(String packageName, int userId) {
            HashMap<String, PackageInfo> map = (HashMap) this.mCache.get(userId);
            return map == null ? null : (PackageInfo) map.get(packageName);
        }

        public void cachePackageInfo(String packageName, int userId, PackageInfo pi) {
            HashMap<String, PackageInfo> map = (HashMap) this.mCache.get(userId);
            if (map == null) {
                map = new HashMap();
                this.mCache.put(userId, map);
            }
            map.put(packageName, pi);
        }

        public void forgetPackageInfo(String packageName, int userId) {
            HashMap<String, PackageInfo> map = (HashMap) this.mCache.get(userId);
            if (map != null) {
                map.remove(packageName);
                if (map.isEmpty()) {
                    this.mCache.delete(userId);
                }
            }
        }

        public void forgetAllPackageInfos(int userId) {
            this.mCache.delete(userId);
        }

        public void dump(PrintWriter pw, boolean verbose) {
            pw.println("PackageInfo cache");
            int i = 0;
            int count;
            int N;
            if (!verbose) {
                count = 0;
                N = this.mCache.size();
                while (i < N) {
                    count += ((HashMap) this.mCache.get(this.mCache.keyAt(i))).size();
                    i++;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(TAB1);
                stringBuilder.append(count);
                stringBuilder.append(" package(s)");
                pw.println(stringBuilder.toString());
            } else if (this.mCache.size() == 0) {
                pw.println("    <empty>");
            } else {
                count = this.mCache.size();
                while (i < count) {
                    N = this.mCache.keyAt(i);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    User ");
                    stringBuilder2.append(N);
                    pw.println(stringBuilder2.toString());
                    for (Entry<String, PackageInfo> entry : ((HashMap) this.mCache.get(N)).entrySet()) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(TAB2);
                        stringBuilder3.append((String) entry.getKey());
                        stringBuilder3.append(": ");
                        stringBuilder3.append(entry.getValue());
                        pw.println(stringBuilder3.toString());
                    }
                    i++;
                }
            }
        }
    }

    public OverlayManagerService(Context context, Installer installer) {
        super(context);
        IdmapManager im = new IdmapManager(installer);
        this.mSettings = new OverlayManagerSettings();
        this.mImpl = new OverlayManagerServiceImpl(this.mPackageManager, im, this.mSettings, getDefaultOverlayPackages(), new OverlayChangeListener(this, null));
        this.mInitCompleteSignal = SystemServerInitThreadPool.get().submit(new -$$Lambda$OverlayManagerService$mX9VnR-_2XOwgKo9C81uZcpqETM(this), "Init OverlayManagerService");
    }

    public static /* synthetic */ void lambda$new$0(OverlayManagerService overlayManagerService) {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        overlayManagerService.getContext().registerReceiverAsUser(new PackageReceiver(overlayManagerService, null), UserHandle.ALL, packageFilter, null, null);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        overlayManagerService.getContext().registerReceiverAsUser(new UserReceiver(overlayManagerService, null), UserHandle.ALL, userFilter, null, null);
        overlayManagerService.restoreSettings();
        overlayManagerService.initIfNeeded();
        overlayManagerService.onSwitchUser(0);
        overlayManagerService.publishBinderService("overlay", overlayManagerService.mService);
        overlayManagerService.publishLocalService(OverlayManagerService.class, overlayManagerService);
    }

    public void onStart() {
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            ConcurrentUtils.waitForFutureNoInterrupt(this.mInitCompleteSignal, "Wait for OverlayManagerService init");
            this.mInitCompleteSignal = null;
        }
    }

    private void initIfNeeded() {
        List<UserInfo> users = ((UserManager) getContext().getSystemService(UserManager.class)).getUsers(true);
        synchronized (this.mLock) {
            int userCount = users.size();
            for (int i = 0; i < userCount; i++) {
                UserInfo userInfo = (UserInfo) users.get(i);
                if (!(userInfo.supportsSwitchTo() || userInfo.id == 0)) {
                    updateOverlayPaths(((UserInfo) users.get(i)).id, this.mImpl.updateOverlaysForUser(((UserInfo) users.get(i)).id));
                }
            }
        }
    }

    public void onSwitchUser(int newUserId) {
        synchronized (this.mLock) {
            updatePrimaryUserPackagesEnable(newUserId);
            List targets = this.mImpl.updateOverlaysForUser(newUserId);
            if (targets != null && (targets.contains(PackageManagerService.PLATFORM_PACKAGE_NAME) || targets.contains("androidhwext"))) {
                Slog.i(TAG, "targets contains android or androidhwext");
            }
            updateAssets(newUserId, targets);
        }
        schedulePersistSettings();
    }

    private void updatePrimaryUserPackagesEnable(int newUserId) {
        List<OverlayInfo> fwkOverlayInfos = this.mImpl.getOverlayInfosForTarget("androidhwext", newUserId);
        int overlaysSize = fwkOverlayInfos == null ? 0 : fwkOverlayInfos.size();
        for (int i = 0; i < overlaysSize; i++) {
            String packageName = ((OverlayInfo) fwkOverlayInfos.get(i)).packageName;
            if ("com.android.frameworkhwext.dark".equals(packageName)) {
                OverlayInfo overlayInfo = this.mImpl.getOverlayInfo(packageName, 0);
                if (overlayInfo != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("persist.deep.theme_");
                    stringBuilder.append(newUserId);
                    String newStatus = SystemProperties.get(stringBuilder.toString(), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    if (newUserId != 0) {
                        if (TextUtils.isEmpty(newStatus) && overlayInfo.isEnabled() && !this.mImpl.setEnabled(packageName, false, 0)) {
                            Slog.w(TAG, String.format("Failed to set false for %s user %d", new Object[]{packageName, Integer.valueOf(0)}));
                        }
                        if (!(!"dark".equals(newStatus) || overlayInfo.isEnabled() || this.mImpl.setEnabled(packageName, true, 0))) {
                            Slog.w(TAG, String.format("Failed to set true for %s user %d", new Object[]{packageName, Integer.valueOf(0)}));
                        }
                    }
                    if (newUserId == 0) {
                        boolean isPrimaryDark = "dark".equals(SystemProperties.get("persist.deep.theme_0", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
                        if (!(isPrimaryDark == overlayInfo.isEnabled() || this.mImpl.setEnabled(packageName, isPrimaryDark, 0))) {
                            String str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to restore ");
                            stringBuilder2.append(String.valueOf(isPrimaryDark));
                            stringBuilder2.append(" for %s user %d");
                            Slog.w(str, String.format(stringBuilder2.toString(), new Object[]{packageName, Integer.valueOf(0)}));
                        }
                    }
                }
            }
        }
    }

    private static String[] getDefaultOverlayPackages() {
        String str = SystemProperties.get(DEFAULT_OVERLAYS_PROP);
        if (TextUtils.isEmpty(str)) {
            return EmptyArray.STRING;
        }
        ArraySet<String> defaultPackages = new ArraySet();
        for (String packageName : str.split(";")) {
            if (!TextUtils.isEmpty(packageName)) {
                defaultPackages.add(packageName);
            }
        }
        return (String[]) defaultPackages.toArray(new String[defaultPackages.size()]);
    }

    private void updateOverlayPaths(int userId, List<String> targetPackageNames) {
        int i = userId;
        List<String> targetPackageNames2 = targetPackageNames;
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        boolean updateFrameworkRes = targetPackageNames2.contains(PackageManagerService.PLATFORM_PACKAGE_NAME);
        boolean updateFrameworkReshwext = targetPackageNames2.contains("androidhwext");
        if (updateFrameworkRes || updateFrameworkReshwext) {
            targetPackageNames2 = pm.getTargetPackageNames(i);
        }
        List<String> targetPackageNames3 = targetPackageNames2;
        ArrayMap pendingChanges = new ArrayMap(targetPackageNames3.size());
        synchronized (this.mLock) {
            targetPackageNames2 = this.mImpl.getEnabledOverlayPackageNames(PackageManagerService.PLATFORM_PACKAGE_NAME, i);
            List<String> tmpFwkOverlays = new ArrayList(targetPackageNames2.size());
            tmpFwkOverlays.addAll(targetPackageNames2);
            tmpFwkOverlays.remove(OverlayManagerSettings.FWK_DARK_OVERLAY_TAG);
            List<String> frameworkhwextOverlays = this.mImpl.getEnabledOverlayPackageNames("androidhwext", i);
            int N = targetPackageNames3.size();
            int i2 = 0;
            while (i2 < N) {
                String targetPackageName = (String) targetPackageNames3.get(i2);
                boolean isInDataSkinDir = isInDataSkinDir(targetPackageName);
                List<String> list = new ArrayList();
                List<String> frameworkOverlays = targetPackageNames2;
                if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(targetPackageName)) {
                    list.addAll(isInDataSkinDir ? tmpFwkOverlays : frameworkOverlays);
                }
                if (!("androidhwext".equals(targetPackageName) || isInDataSkinDir)) {
                    list.addAll(frameworkhwextOverlays);
                }
                list.addAll(this.mImpl.getEnabledOverlayPackageNames(targetPackageName, i));
                pendingChanges.put(targetPackageName, list);
                i2++;
                targetPackageNames2 = frameworkOverlays;
            }
        }
        int N2 = targetPackageNames3.size();
        for (int i3 = 0; i3 < N2; i3++) {
            String targetPackageName2 = (String) targetPackageNames3.get(i3);
            if (!pm.setEnabledOverlayPackages(i, targetPackageName2, (List) pendingChanges.get(targetPackageName2))) {
                Slog.e(TAG, String.format("Failed to change enabled overlays for %s user %d", new Object[]{targetPackageName2, Integer.valueOf(userId)}));
            }
        }
    }

    private boolean isInDataSkinDir(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        String themePath = new StringBuilder();
        themePath.append(Environment.getDataDirectory());
        themePath.append("/themes/");
        themePath.append(UserHandle.myUserId());
        File root = new File(themePath.toString());
        if (!root.exists()) {
            return false;
        }
        File[] files = root.listFiles();
        int size = files == null ? 0 : files.length;
        for (int i = 0; i < size; i++) {
            File file = files[i];
            if (file.isFile() && packageName.equals(file.getName())) {
                return true;
            }
        }
        return false;
    }

    private void updateAssets(int userId, String targetPackageName) {
        updateAssets(userId, Collections.singletonList(targetPackageName));
    }

    private void updateAssets(int userId, List<String> targetPackageNames) {
        updateOverlayPaths(userId, targetPackageNames);
        try {
            ActivityManager.getService().scheduleApplicationInfoChanged(targetPackageNames, userId);
        } catch (RemoteException e) {
        }
    }

    private void schedulePersistSettings() {
        if (!this.mPersistSettingsScheduled.getAndSet(true)) {
            IoThread.getHandler().post(new -$$Lambda$OverlayManagerService$YGMOwF5u3kvuRAEYnGl_xpXcVC4(this));
        }
    }

    public static /* synthetic */ void lambda$schedulePersistSettings$1(OverlayManagerService overlayManagerService) {
        overlayManagerService.mPersistSettingsScheduled.set(false);
        synchronized (overlayManagerService.mLock) {
            FileOutputStream stream = null;
            try {
                stream = overlayManagerService.mSettingsFile.startWrite();
                overlayManagerService.mSettings.persist(stream);
                overlayManagerService.mSettingsFile.finishWrite(stream);
            } catch (IOException | XmlPullParserException e) {
                overlayManagerService.mSettingsFile.failWrite(stream);
                Slog.e(TAG, "failed to persist overlay state", e);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x007c A:{ExcHandler: IOException | XmlPullParserException (r1_5 'e' java.lang.Exception), Splitter:B:7:0x0011} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:32:0x0073, code skipped:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r2.addSuppressed(r4);
     */
    /* JADX WARNING: Missing block: B:37:0x007c, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            android.util.Slog.e(TAG, "failed to restore overlay state", r1);
     */
    /* JADX WARNING: Missing block: B:41:0x0085, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void restoreSettings() {
        synchronized (this.mLock) {
            if (this.mSettingsFile.getBaseFile().exists()) {
                FileInputStream stream;
                try {
                    stream = this.mSettingsFile.openRead();
                    this.mSettings.restore(stream);
                    List<UserInfo> liveUsers = this.mUserManager.getUsers(true);
                    int[] liveUserIds = new int[liveUsers.size()];
                    int i = 0;
                    for (int i2 = 0; i2 < liveUsers.size(); i2++) {
                        liveUserIds[i2] = ((UserInfo) liveUsers.get(i2)).getUserHandle().getIdentifier();
                    }
                    Arrays.sort(liveUserIds);
                    int[] users = this.mSettings.getUsers();
                    int length = users.length;
                    while (i < length) {
                        int userId = users[i];
                        if (Arrays.binarySearch(liveUserIds, userId) < 0) {
                            this.mSettings.removeUser(userId);
                        }
                        i++;
                    }
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException | XmlPullParserException e) {
                } catch (Throwable th) {
                    if (stream != null) {
                        if (r2 != null) {
                            stream.close();
                        } else {
                            stream.close();
                        }
                    }
                }
            }
        }
    }
}

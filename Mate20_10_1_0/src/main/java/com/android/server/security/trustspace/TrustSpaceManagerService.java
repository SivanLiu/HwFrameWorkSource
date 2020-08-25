package com.android.server.security.trustspace;

import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.trustspace.ITrustSpaceManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.permissionmanager.util.PermConst;
import java.util.ArrayList;
import java.util.List;

public class TrustSpaceManagerService extends ITrustSpaceManager.Stub implements IHwSecurityPlugin {
    private static final String ACTION_INTENT_PREVENTED = "huawei.intent.action.TRUSTSPACE_INTENT_PREVENTED";
    private static final String ACTION_PACKAGE_ADDED = "huawei.intent.action.TRUSTSPACE_PACKAGE_ADDED";
    private static final String ACTION_PACKAGE_REMOVED = "huawei.intent.action.TRUSTSPACE_PACKAGE_REMOVED";
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.trustspace.TrustSpaceManagerService.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            if (TrustSpaceManagerService.HW_DEBUG) {
                Slog.d(TrustSpaceManagerService.TAG, "createPlugin");
            }
            return new TrustSpaceManagerService(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return TrustSpaceManagerService.MANAGE_TRUSTSPACE;
        }
    };
    private static final String HW_APPMARKET_PACKAGENAME = "com.huawei.appmarket";
    /* access modifiers changed from: private */
    public static final boolean HW_DEBUG = Log.HWINFO;
    private static final String HW_TRUSTSPACE_LAUNCHER = "com.huawei.trustspace.mainscreen.LoadActivity";
    private static final String HW_TRUSTSPACE_PACKAGENAME = "com.huawei.trustspace";
    private static final int INVALID_CLONED_PROFILE = -1000;
    private static final boolean IS_SUPPORT_CLONE_APP = SystemProperties.getBoolean("ro.config.hw_support_clone_app", false);
    private static final String MANAGE_TRUSTSPACE = "com.huawei.permission.MANAGE_TRUSTSPACE";
    private static final int RESULT_CODE_ERROR = -1;
    private static final String SETTINGS_TRUSTSPACE_CONTROL = "trust_space_switch";
    private static final String SETTINGS_TRUSTSPACE_SWITCH = "is_trustspace_enabled";
    private static final String TAG = "TrustSpaceManagerService";
    private static final int TYPE_RISK = 303;
    private static final int TYPE_VIRUS = 305;
    private int mClonedProfileUserId;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public boolean mEnableTrustSpace = false;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private TrustSpaceSettings mSettings;
    private final MySettingsObserver mSettingsObserver = new MySettingsObserver();
    /* access modifiers changed from: private */
    public boolean mSupportTrustSpace = false;
    private final ArraySet<String> mSystemApps = new ArraySet<>();
    private volatile boolean mSystemReady = false;
    private final ArraySet<Integer> mSystemUids = new ArraySet<>();
    private final ArrayMap<String, Integer> mVirusScanResult = new ArrayMap<>();

    public TrustSpaceManagerService(Context context) {
        this.mContext = context;
        this.mSettings = new TrustSpaceSettings();
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        Slog.d(TAG, "TrustSpaceManagerService Start");
        LocalServices.addService(TrustSpaceManagerInternal.class, new LocalServiceImpl());
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.security.trustspace.TrustSpaceManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return this;
    }

    private final class LocalServiceImpl implements TrustSpaceManagerInternal {
        private LocalServiceImpl() {
        }

        @Override // com.android.server.security.trustspace.TrustSpaceManagerInternal
        public boolean checkIntent(int type, String calleePackage, int callerUid, int callerPid, String callingPackage, int userId) {
            return TrustSpaceManagerService.this.checkIntent(type, calleePackage, callerUid, callerPid, callingPackage, userId);
        }

        @Override // com.android.server.security.trustspace.TrustSpaceManagerInternal
        public void initTrustSpace() {
            TrustSpaceManagerService.this.initTrustSpace();
        }

        @Override // com.android.server.security.trustspace.TrustSpaceManagerInternal
        public boolean isIntentProtectedApp(String pkg) {
            return TrustSpaceManagerService.this.isIntentProtectedAppInner(pkg);
        }

        @Override // com.android.server.security.trustspace.TrustSpaceManagerInternal
        public int getProtectionLevel(String packageName) {
            return TrustSpaceManagerService.this.getProtectionLevel(packageName);
        }
    }

    private final class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            TrustSpaceManagerService.this.handlePackageAppeared(packageName, 3, uid);
        }

        public void onPackageRemoved(String packageName, int uid) {
            TrustSpaceManagerService.this.handlePackageDisappeared(packageName, 3, uid);
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            TrustSpaceManagerService.this.handlePackageAppeared(packageName, 1, uid);
        }
    }

    class MyBroadcastReceiver extends BroadcastReceiver {
        MyBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                Slog.d(TrustSpaceManagerService.TAG, "action add user" + userId);
                if (!(userId == -10000 || userId == 0)) {
                    TrustSpaceManagerService.this.enableTrustSpaceApp(false, false, userId);
                }
                if (TrustSpaceManagerService.this.isClonedProfile(userId)) {
                    TrustSpaceManagerService.this.updateClonedProfileUserId(userId);
                }
            }
        }
    }

    private class MySettingsObserver extends ContentObserver {
        private final Uri TRUSTSPACE_SWITCH_URI = Settings.Secure.getUriFor(TrustSpaceManagerService.SETTINGS_TRUSTSPACE_SWITCH);

        public MySettingsObserver() {
            super(new Handler());
        }

        public void registerContentObserver() {
            TrustSpaceManagerService.this.mContext.getContentResolver().registerContentObserver(this.TRUSTSPACE_SWITCH_URI, false, this, 0);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (this.TRUSTSPACE_SWITCH_URI.equals(uri)) {
                int i = 0;
                boolean enable = Settings.Secure.getIntForUser(TrustSpaceManagerService.this.mContext.getContentResolver(), TrustSpaceManagerService.SETTINGS_TRUSTSPACE_SWITCH, 1, 0) == 1;
                Slog.i(TrustSpaceManagerService.TAG, "TrustSpace Enabled = " + enable);
                boolean unused = TrustSpaceManagerService.this.mEnableTrustSpace = enable;
                if (TrustSpaceManagerService.this.mSupportTrustSpace) {
                    TrustSpaceManagerService.this.enableTrustSpaceApp(true, enable, 0);
                    ContentResolver contentResolver = TrustSpaceManagerService.this.mContext.getContentResolver();
                    if (enable) {
                        i = 1;
                    }
                    Settings.System.putInt(contentResolver, TrustSpaceManagerService.SETTINGS_TRUSTSPACE_CONTROL, i);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePackageAppeared(String packageName, int reason, int uid) {
        if (isUseTrustSpace()) {
            int userId = UserHandle.getUserId(uid);
            if (userId == 0 || isClonedProfile(userId)) {
                if (HW_DEBUG) {
                    Slog.d(TAG, "onPackageAppeared:" + packageName + " reason=" + reason + " uid=" + uid);
                }
                if (userId == 0) {
                    removeVirusScanResult(packageName);
                }
                Intent intent = new Intent(ACTION_PACKAGE_ADDED);
                intent.putExtra("packageName", packageName);
                intent.putExtra("reason", reason);
                intent.putExtra(PermConst.USER_ID, userId);
                intent.setPackage(HW_TRUSTSPACE_PACKAGENAME);
                this.mContext.startService(intent);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePackageDisappeared(String packageName, int reason, int uid) {
        if (isUseTrustSpace()) {
            int userId = UserHandle.getUserId(uid);
            if (userId == 0 || isClonedProfile(userId)) {
                if (HW_DEBUG) {
                    Slog.d(TAG, "onPackageDisappeared:" + packageName + " reason=" + reason + " uid=" + uid);
                }
                if (userId == 0) {
                    removeVirusScanResult(packageName);
                    synchronized (this) {
                        boolean configChange = false;
                        boolean isProtectedApp = this.mSettings.isIntentProtectedApp(packageName);
                        boolean isTrustApp = this.mSettings.isTrustApp(packageName);
                        if (isProtectedApp && reason == 3) {
                            this.mSettings.removeIntentProtectedApp(packageName);
                            configChange = true;
                        }
                        if (isTrustApp && reason == 3) {
                            this.mSettings.removeTrustApp(packageName);
                            configChange = true;
                        }
                        if (configChange) {
                            this.mSettings.writePackages();
                        }
                    }
                }
                Intent intent = new Intent(ACTION_PACKAGE_REMOVED);
                intent.putExtra("packageName", packageName);
                intent.putExtra("reason", reason);
                intent.putExtra(PermConst.USER_ID, userId);
                intent.setPackage(HW_TRUSTSPACE_PACKAGENAME);
                this.mContext.startService(intent);
            }
        }
    }

    private boolean isTrustSpaceEnable() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), SETTINGS_TRUSTSPACE_CONTROL, 1, 0) == 1;
    }

    private boolean isUseTrustSpace() {
        return this.mSupportTrustSpace && this.mEnableTrustSpace;
    }

    private boolean calledFromValidUser() {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (uid == 1000 || userId == 0) {
            return true;
        }
        return false;
    }

    private void removeVirusScanResult(String packageName) {
        synchronized (this.mVirusScanResult) {
            this.mVirusScanResult.remove(packageName);
        }
    }

    private void addVirusScanResult(String packageName, int type) {
        synchronized (this.mVirusScanResult) {
            this.mVirusScanResult.put(packageName, Integer.valueOf(type));
        }
    }

    private boolean isSystemPackageInstalled(String packageName, int flag) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, flag, 0);
        } catch (RemoteException e) {
            Log.d(TAG, "Can't find package:" + packageName);
        }
        if (appInfo == null || !appInfo.isSystemApp()) {
            return false;
        }
        return true;
    }

    private boolean isSupportTrustSpaceInner() {
        if (isSystemPackageInstalled(HW_APPMARKET_PACKAGENAME, 0)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void enableTrustSpaceApp(boolean enablePackage, boolean enableLauncher, int userId) {
        int newPackageState;
        IPackageManager pm = AppGlobals.getPackageManager();
        ComponentName cName = new ComponentName(HW_TRUSTSPACE_PACKAGENAME, HW_TRUSTSPACE_LAUNCHER);
        int newLauncherState = 0;
        if (enablePackage) {
            newPackageState = 0;
        } else {
            newPackageState = 2;
        }
        if (!enableLauncher) {
            newLauncherState = 2;
        }
        try {
            pm.setApplicationEnabledSetting(HW_TRUSTSPACE_PACKAGENAME, newPackageState, 0, userId, (String) null);
            if (enablePackage) {
                pm.setComponentEnabledSetting(cName, newLauncherState, 1, userId);
            }
        } catch (Exception e) {
            Slog.w(TAG, "enableTrustSpaceApp fail");
        }
    }

    private void loadSystemPackages() {
        ArraySet<Integer> tempUidSet = new ArraySet<>();
        ArraySet<String> tempPkgSet = new ArraySet<>();
        List<ApplicationInfo> allApp = null;
        try {
            allApp = AppGlobals.getPackageManager().getInstalledApplications(8192, 0).getList();
        } catch (RemoteException e) {
            Slog.e(TAG, "Get Installed Applications fail");
        }
        if (allApp != null) {
            for (ApplicationInfo app : allApp) {
                if (app.isSystemApp() && (app.hwFlags & 33554432) == 0) {
                    tempUidSet.add(Integer.valueOf(app.uid));
                    tempPkgSet.add(app.packageName);
                }
            }
            this.mSystemUids.clear();
            this.mSystemUids.addAll((ArraySet<? extends Integer>) tempUidSet);
            this.mSystemApps.clear();
            this.mSystemApps.addAll((ArraySet<? extends String>) tempPkgSet);
        }
    }

    private int getClonedProfileId() {
        if (!IS_SUPPORT_CLONE_APP) {
            return -1000;
        }
        try {
            for (UserInfo info : ServiceManager.getService("user").getProfiles(0, false)) {
                if (info.isClonedProfile()) {
                    return info.id;
                }
            }
            Slog.d(TAG, "Cloned Profile is not exist");
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to getProfiles");
        }
        return -1000;
    }

    /* access modifiers changed from: private */
    public boolean isClonedProfile(int userId) {
        if (!IS_SUPPORT_CLONE_APP) {
            return false;
        }
        try {
            UserInfo userInfo = ServiceManager.getService("user").getUserInfo(userId);
            if (userInfo == null || !userInfo.isClonedProfile()) {
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to getUserInfo");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void updateClonedProfileUserId(int userId) {
        if (IS_SUPPORT_CLONE_APP && userId != -1000) {
            this.mClonedProfileUserId = userId;
            Slog.i(TAG, "Cloned profile user" + userId);
        }
    }

    private boolean checkClonedProfile(int userId) {
        if (!IS_SUPPORT_CLONE_APP || userId == -1000 || userId != this.mClonedProfileUserId) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void initTrustSpace() {
        if (!this.mSystemReady) {
            Slog.i(TAG, "TrustSpaceManagerService init begin");
            if (!isSystemPackageInstalled(HW_TRUSTSPACE_PACKAGENAME, 0)) {
                Slog.e(TAG, "TrustSpace application is not exist");
                return;
            }
            this.mSupportTrustSpace = isSupportTrustSpaceInner();
            if (this.mSupportTrustSpace) {
                synchronized (this) {
                    this.mSettings.readPackages();
                }
                loadSystemPackages();
                this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, false);
                this.mSettingsObserver.registerContentObserver();
                updateClonedProfileUserId(getClonedProfileId());
                this.mEnableTrustSpace = isTrustSpaceEnable();
            }
            IntentFilter broadcastFilter = new IntentFilter();
            broadcastFilter.addAction("android.intent.action.USER_ADDED");
            this.mContext.registerReceiver(new MyBroadcastReceiver(), broadcastFilter);
            Slog.i(TAG, "Enable TrustSpace App: " + this.mSupportTrustSpace);
            if (this.mSupportTrustSpace) {
                enableTrustSpaceApp(true, this.mEnableTrustSpace, 0);
            } else {
                disableTrustSpaceForAllUsers();
            }
            this.mSystemReady = true;
            Slog.i(TAG, "TrustSpaceManagerService init end");
        }
    }

    private void disableTrustSpaceForAllUsers() {
        try {
            for (UserInfo user : ServiceManager.getService("user").getUsers(false)) {
                enableTrustSpaceApp(false, false, user.id);
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "disableTrustSpaceForAllUsers, failed to getUserInfo");
        }
    }

    private boolean isValidUser(int type, int userId) {
        boolean z = false;
        boolean isValidUser = userId == 0;
        if (type == 1) {
            if (userId == -1) {
                z = true;
            }
            isValidUser |= z;
        }
        return checkClonedProfile(userId) | isValidUser;
    }

    private boolean isSpecialCaller(String packageName, int uid) {
        if (packageName != null) {
            return this.mSystemApps.contains(packageName);
        }
        if (uid < 10000 || this.mSystemUids.contains(Integer.valueOf(uid))) {
            return true;
        }
        return false;
    }

    private boolean isSelfCall(String calleePackage, String callingPackage) {
        return calleePackage != null && calleePackage.equals(callingPackage);
    }

    private boolean isSpecialCallee(String packageName) {
        return this.mSystemApps.contains(packageName);
    }

    private boolean isSpecialPackage(String callingPackage, int callerUid, String calleePackage) {
        boolean isCallerIntentProtected;
        boolean isCalleeIntentProtected;
        boolean isSpecialCaller = isSpecialCaller(callingPackage, callerUid);
        boolean isSpecialCallee = isSpecialCallee(calleePackage);
        synchronized (this) {
            isCallerIntentProtected = this.mSettings.isIntentProtectedApp(callingPackage);
            isCalleeIntentProtected = this.mSettings.isIntentProtectedApp(calleePackage);
        }
        return (isSpecialCaller && !isCallerIntentProtected) || (isSpecialCallee && !isCalleeIntentProtected);
    }

    private boolean shouldNotify(int type, boolean isHarmful) {
        if ((type == 0 || type == 2 || type == 3) && !isHarmful) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x006a, code lost:
        if (r4 != 0) goto L_0x006f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x006c, code lost:
        if (r5 != 0) goto L_0x006f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x006e, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0071, code lost:
        if (com.android.server.security.trustspace.TrustSpaceManagerService.HW_DEBUG == false) goto L_0x00c1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x0073, code lost:
        android.util.Slog.d(com.android.server.security.trustspace.TrustSpaceManagerService.TAG, "check Intent, type: " + r15 + " calleePackage: " + r26 + " calleelevel=" + r4 + " callerPid:" + r28 + " callerUid:" + r27 + " callerPackage:" + r29 + " callerLevel=" + r5 + " userId:" + r30);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00c1, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00c3, code lost:
        if (r0 == false) goto L_0x00db;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00c5, code lost:
        android.util.Slog.i(com.android.server.security.trustspace.TrustSpaceManagerService.TAG, "find calleePackage Malicious App:" + r26);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00db, code lost:
        if (r0 == false) goto L_0x00f3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00dd, code lost:
        android.util.Slog.i(com.android.server.security.trustspace.TrustSpaceManagerService.TAG, "find callingPackage Malicious App:" + r29);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00f4, code lost:
        if (r4 != 1) goto L_0x0103;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00f6, code lost:
        if (r5 != 0) goto L_0x0102;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00f8, code lost:
        if (r0 == false) goto L_0x00fb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00fb, code lost:
        if (r0 == false) goto L_0x0139;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00fd, code lost:
        r19 = true;
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x0102, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0103, code lost:
        if (r5 != 1) goto L_0x0112;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x0105, code lost:
        if (r4 != 0) goto L_0x0111;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x0107, code lost:
        if (r0 == false) goto L_0x010a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x010a, code lost:
        if (r0 == false) goto L_0x0139;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x010c, code lost:
        r19 = true;
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x0111, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x0113, code lost:
        if (r4 != 2) goto L_0x012d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0115, code lost:
        if (r5 != 0) goto L_0x012c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x0117, code lost:
        if (r0 == false) goto L_0x011a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x011a, code lost:
        if (r0 == false) goto L_0x011d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x011c, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0121, code lost:
        if (shouldNotify(r25, r0) == false) goto L_0x0128;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x0123, code lost:
        r19 = r0;
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x0128, code lost:
        r19 = r0;
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x012c, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x012d, code lost:
        if (r5 != 2) goto L_0x0139;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x012f, code lost:
        if (r0 != false) goto L_0x0132;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0131, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x0132, code lost:
        if (r0 == false) goto L_0x0139;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0134, code lost:
        r19 = true;
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0139, code lost:
        r19 = false;
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x013c, code lost:
        if (r0 != false) goto L_0x0140;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x013e, code lost:
        if (r0 == false) goto L_0x0141;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x0140, code lost:
        r1 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0142, code lost:
        if (r1 == false) goto L_0x015e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0144, code lost:
        if (r25 == 1) goto L_0x015e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x0146, code lost:
        r21 = r4;
        r22 = r5;
        r9 = r1;
        notifyIntentPrevented(r15, r26, r4, r29, r22, true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x015e, code lost:
        r21 = r4;
        r22 = r5;
        r9 = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0165, code lost:
        if (r0 == false) goto L_0x0176;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x0167, code lost:
        notifyIntentPrevented(r15, r26, r21, r29, r22, false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x0176, code lost:
        if (r19 == false) goto L_0x01d4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x0178, code lost:
        android.util.Slog.i(com.android.server.security.trustspace.TrustSpaceManagerService.TAG, "prevent Intent, type: " + r15 + " calleePackage: " + r26 + " calleelevel=" + r21 + " callerPid:" + r28 + " callerUid:" + r27 + " callerPackage:" + r29 + " callerLevel=" + r22 + " isMalicious=" + r9 + " userId:" + r30);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x01d8, code lost:
        return r19;
     */
    public boolean checkIntent(int type, String calleePackage, int callerUid, int callerPid, String callingPackage, int userId) {
        boolean isMalicious = false;
        if (!isUseTrustSpace() || !isValidUser(type, userId) || isSelfCall(calleePackage, callingPackage) || isSpecialPackage(callingPackage, callerUid, calleePackage)) {
            return false;
        }
        String typeString = TrustSpaceSettings.componentTypeToString(type);
        if (calleePackage == null || callingPackage == null) {
            Slog.w(TAG, "unknown Intent, type: " + typeString + " calleePackage: " + calleePackage + " callerPid:" + callerPid + " callerUid:" + callerUid + " callerPackage:" + callingPackage + " userId:" + userId);
            return false;
        }
        synchronized (this) {
            try {
                boolean isTrustcallee = this.mSettings.isTrustApp(calleePackage);
                try {
                    boolean isTrustcaller = this.mSettings.isTrustApp(callingPackage);
                    try {
                        int calleeLevel = this.mSettings.getProtectionLevel(calleePackage) & 255;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                    try {
                        int callerLevel = this.mSettings.getProtectionLevel(callingPackage) & 255;
                        try {
                            boolean isCalleeHarmful = this.mSettings.isHarmfulApp(calleePackage);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                    try {
                        boolean isCallerHarmful = this.mSettings.isHarmfulApp(callingPackage);
                        try {
                        } catch (Throwable th4) {
                            th = th4;
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                throw th;
            }
        }
    }

    private void notifyIntentPrevented(final String typeString, final String calleePackage, final int calleeLevel, final String callerPackage, final int callerLevel, final boolean isMalicious) {
        UiThread.getHandler().post(new Runnable() {
            /* class com.android.server.security.trustspace.TrustSpaceManagerService.AnonymousClass2 */

            public void run() {
                Intent intent = new Intent(TrustSpaceManagerService.ACTION_INTENT_PREVENTED);
                intent.putExtra("component", typeString);
                intent.putExtra("callee", calleePackage);
                intent.putExtra("caller", callerPackage);
                intent.putExtra("calleeLevel", calleeLevel);
                intent.putExtra("callerLevel", callerLevel);
                intent.putExtra("isMalicious", isMalicious);
                intent.setPackage(TrustSpaceManagerService.HW_TRUSTSPACE_PACKAGENAME);
                TrustSpaceManagerService.this.mContext.startService(intent);
                Slog.d(TrustSpaceManagerService.TAG, "Notify intent prevented.");
            }
        });
    }

    /* access modifiers changed from: private */
    public boolean isIntentProtectedAppInner(String packageName) {
        boolean isIntentProtectedApp;
        if (!calledFromValidUser() || packageName == null) {
            return false;
        }
        synchronized (this) {
            isIntentProtectedApp = this.mSettings.isIntentProtectedApp(packageName);
        }
        return isIntentProtectedApp;
    }

    public boolean addIntentProtectedApps(List<String> packages, int flags) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this) {
            for (String packageName : packages) {
                this.mSettings.addIntentProtectedApp(packageName, flags);
                if (HW_DEBUG) {
                    Slog.d(TAG, "add " + packageName + " to intent protected list, flags=" + flags);
                }
            }
            this.mSettings.writePackages();
        }
        return true;
    }

    public boolean removeIntentProtectedApp(String packageName) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packageName == null) {
            return false;
        }
        synchronized (this) {
            this.mSettings.removeIntentProtectedApp(packageName);
            this.mSettings.writePackages();
            if (HW_DEBUG) {
                Slog.d(TAG, "remove " + packageName + " from intent protected list");
            }
        }
        return true;
    }

    public List<String> getIntentProtectedApps(int flags) {
        List<String> intentProtectedApps;
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser()) {
            return new ArrayList(0);
        }
        synchronized (this) {
            intentProtectedApps = this.mSettings.getIntentProtectedApps(flags);
        }
        return intentProtectedApps;
    }

    public boolean removeIntentProtectedApps(List<String> packages, int flags) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this) {
            this.mSettings.removeIntentProtectedApps(packages, flags);
            this.mSettings.writePackages();
            if (HW_DEBUG) {
                Slog.d(TAG, "remove apps in intent protected list, flag=" + flags);
            }
        }
        return true;
    }

    public boolean updateTrustApps(List<String> packages, int flag) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packages == null) {
            return false;
        }
        synchronized (this) {
            this.mSettings.updateTrustApps(packages, flag);
            this.mSettings.writePackages();
            if (HW_DEBUG) {
                Slog.d(TAG, "update trust apps, flag=" + flag);
            }
        }
        return true;
    }

    public boolean isIntentProtectedApp(String packageName) {
        boolean isIntentProtectedApp;
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packageName == null) {
            return false;
        }
        synchronized (this) {
            isIntentProtectedApp = this.mSettings.isIntentProtectedApp(packageName);
        }
        return isIntentProtectedApp;
    }

    public int getProtectionLevel(String packageName) {
        int protectionLevel;
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packageName == null) {
            return 0;
        }
        synchronized (this) {
            protectionLevel = this.mSettings.getProtectionLevel(packageName);
        }
        return protectionLevel;
    }

    public boolean isSupportTrustSpace() {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser()) {
            return false;
        }
        return this.mSupportTrustSpace;
    }

    public boolean isHwTrustSpace(int userId) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        boolean z = false;
        if (!calledFromValidUser()) {
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = ServiceManager.getService("user").getUserInfo(userId);
            if (userInfo != null && userInfo.isHwTrustSpace()) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            Slog.d(TAG, "failed to getUserInfo");
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}

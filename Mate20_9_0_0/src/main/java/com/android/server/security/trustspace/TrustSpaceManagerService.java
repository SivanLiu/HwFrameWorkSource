package com.android.server.security.trustspace;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.trustspace.ITrustSpaceManager.Stub;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.content.PackageMonitor;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import com.android.server.security.securityprofile.ScreenshotProtectorCallback;
import com.android.server.security.securityprofile.SecurityProfileInternal;
import java.util.ArrayList;
import java.util.List;

public class TrustSpaceManagerService extends Stub implements IHwSecurityPlugin {
    private static final String ACTION_INTENT_PREVENTED = "huawei.intent.action.TRUSTSPACE_INTENT_PREVENTED";
    private static final String ACTION_PACKAGE_ADDED = "huawei.intent.action.TRUSTSPACE_PACKAGE_ADDED";
    private static final String ACTION_PACKAGE_REMOVED = "huawei.intent.action.TRUSTSPACE_PACKAGE_REMOVED";
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            if (TrustSpaceManagerService.HW_DEBUG) {
                Slog.d(TrustSpaceManagerService.TAG, "createPlugin");
            }
            return new TrustSpaceManagerService(context);
        }

        public String getPluginPermission() {
            return TrustSpaceManagerService.MANAGE_TRUSTSPACE;
        }
    };
    private static final String HW_APPMARKET_PACKAGENAME = "com.huawei.appmarket";
    private static final boolean HW_DEBUG = Log.HWINFO;
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
    private Context mContext;
    private boolean mEnableTrustSpace = false;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor(this, null);
    private TrustSpaceSettings mSettings;
    private final MySettingsObserver mSettingsObserver = new MySettingsObserver();
    private boolean mSupportTrustSpace = false;
    private final ArraySet<String> mSystemApps = new ArraySet();
    private volatile boolean mSystemReady = false;
    private final ArraySet<Integer> mSystemUids = new ArraySet();
    private final ArrayMap<String, Integer> mVirusScanResult = new ArrayMap();
    private SecurityProfileInternal securityProfileInternal = null;

    class MyBroadcastReceiver extends BroadcastReceiver {
        MyBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_ADDED".equals(intent.getAction())) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                String str = TrustSpaceManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("action add user");
                stringBuilder.append(userId);
                Slog.d(str, stringBuilder.toString());
                if (!(userId == -10000 || userId == 0)) {
                    TrustSpaceManagerService.this.enableTrustSpaceApp(false, false, userId);
                }
                if (TrustSpaceManagerService.this.isClonedProfile(userId)) {
                    TrustSpaceManagerService.this.updateClonedProfileUserId(userId);
                }
            }
        }
    }

    private final class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        /* synthetic */ MyPackageMonitor(TrustSpaceManagerService x0, AnonymousClass1 x1) {
            this();
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

    private class MySettingsObserver extends ContentObserver {
        private final Uri TRUSTSPACE_SWITCH_URI = Secure.getUriFor(TrustSpaceManagerService.SETTINGS_TRUSTSPACE_SWITCH);

        public MySettingsObserver() {
            super(new Handler());
        }

        public void registerContentObserver() {
            TrustSpaceManagerService.this.mContext.getContentResolver().registerContentObserver(this.TRUSTSPACE_SWITCH_URI, false, this, 0);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (this.TRUSTSPACE_SWITCH_URI.equals(uri)) {
                int i = 0;
                boolean enable = Secure.getIntForUser(TrustSpaceManagerService.this.mContext.getContentResolver(), TrustSpaceManagerService.SETTINGS_TRUSTSPACE_SWITCH, 1, 0) == 1;
                String str = TrustSpaceManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("TrustSpace Enabled = ");
                stringBuilder.append(enable);
                Slog.i(str, stringBuilder.toString());
                TrustSpaceManagerService.this.mEnableTrustSpace = enable;
                if (TrustSpaceManagerService.this.mSupportTrustSpace) {
                    TrustSpaceManagerService.this.enableTrustSpaceApp(true, enable, 0);
                    ContentResolver contentResolver = TrustSpaceManagerService.this.mContext.getContentResolver();
                    String str2 = TrustSpaceManagerService.SETTINGS_TRUSTSPACE_CONTROL;
                    if (enable) {
                        i = 1;
                    }
                    System.putInt(contentResolver, str2, i);
                }
            }
        }
    }

    private final class LocalServiceImpl implements TrustSpaceManagerInternal {
        private LocalServiceImpl() {
        }

        /* synthetic */ LocalServiceImpl(TrustSpaceManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean checkIntent(int type, String calleePackage, int callerUid, int callerPid, String callingPackage, int userId) {
            return TrustSpaceManagerService.this.checkIntent(type, calleePackage, callerUid, callerPid, callingPackage, userId);
        }

        public void initTrustSpace() {
            TrustSpaceManagerService.this.initTrustSpace();
        }

        public boolean isIntentProtectedApp(String pkg) {
            return TrustSpaceManagerService.this.isIntentProtectedAppInner(pkg);
        }

        public int getProtectionLevel(String packageName) {
            return TrustSpaceManagerService.this.getProtectionLevel(packageName);
        }
    }

    private class ScreenshotProtector extends ScreenshotProtectorCallback {
        private ScreenshotProtector() {
        }

        /* synthetic */ ScreenshotProtector(TrustSpaceManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean isProtectedApp(String packageName) {
            if (ActivityManager.getCurrentUser() == 0 && TrustSpaceManagerService.this.getProtectionLevel(packageName) == 2) {
                return true;
            }
            return false;
        }

        public void notifyInfo(String projectionPack) {
            String text = TrustSpaceManagerService.this.mContext.getResources().getString(33686191);
            if (text != null) {
                String packageLabel = projectionPack;
                PackageManager pm = TrustSpaceManagerService.this.mContext.getPackageManager();
                if (pm != null) {
                    try {
                        ApplicationInfo info = pm.getApplicationInfoAsUser(projectionPack, 0, 0);
                        if (info != null) {
                            CharSequence seq = pm.getApplicationLabel(info);
                            packageLabel = seq == null ? null : seq.toString();
                        }
                    } catch (NameNotFoundException e) {
                        String str = TrustSpaceManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("can not find ");
                        stringBuilder.append(projectionPack);
                        Slog.e(str, stringBuilder.toString());
                    }
                }
                if (packageLabel != null) {
                    text = text.replace("%s", packageLabel);
                }
                showToast(text);
            }
        }

        private void showToast(final String text) {
            UiThread.getHandler().post(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(TrustSpaceManagerService.this.mContext, text, 1);
                    LayoutParams windowParams = toast.getWindowParams();
                    windowParams.privateFlags |= 16;
                    toast.show();
                }
            });
        }
    }

    public TrustSpaceManagerService(Context context) {
        this.mContext = context;
        this.mSettings = new TrustSpaceSettings();
    }

    public void onStart() {
        Slog.d(TAG, "TrustSpaceManagerService Start");
        LocalServices.addService(TrustSpaceManagerInternal.class, new LocalServiceImpl(this, null));
        this.securityProfileInternal = (SecurityProfileInternal) LocalServices.getService(SecurityProfileInternal.class);
        if (this.securityProfileInternal != null) {
            this.securityProfileInternal.registerScreenshotProtector(new ScreenshotProtector(this, null));
        }
    }

    public void onStop() {
    }

    public IBinder asBinder() {
        return this;
    }

    private void handlePackageAppeared(String packageName, int reason, int uid) {
        if (isUseTrustSpace()) {
            int userId = UserHandle.getUserId(uid);
            if (userId == 0 || isClonedProfile(userId)) {
                if (HW_DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onPackageAppeared:");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" reason=");
                    stringBuilder.append(reason);
                    stringBuilder.append(" uid=");
                    stringBuilder.append(uid);
                    Slog.d(str, stringBuilder.toString());
                }
                if (userId == 0) {
                    removeVirusScanResult(packageName);
                }
                Intent intent = new Intent(ACTION_PACKAGE_ADDED);
                intent.putExtra("packageName", packageName);
                intent.putExtra("reason", reason);
                intent.putExtra("userId", userId);
                intent.setPackage(HW_TRUSTSPACE_PACKAGENAME);
                this.mContext.startService(intent);
            }
        }
    }

    private void handlePackageDisappeared(String packageName, int reason, int uid) {
        if (isUseTrustSpace()) {
            int userId = UserHandle.getUserId(uid);
            if (userId == 0 || isClonedProfile(userId)) {
                if (HW_DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onPackageDisappeared:");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" reason=");
                    stringBuilder.append(reason);
                    stringBuilder.append(" uid=");
                    stringBuilder.append(uid);
                    Slog.d(str, stringBuilder.toString());
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
                intent.putExtra("userId", userId);
                intent.setPackage(HW_TRUSTSPACE_PACKAGENAME);
                this.mContext.startService(intent);
            }
        }
    }

    private boolean isTrustSpaceEnable() {
        return System.getIntForUser(this.mContext.getContentResolver(), SETTINGS_TRUSTSPACE_CONTROL, 1, 0) == 1;
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

    /* JADX WARNING: Missing block: B:18:0x0040, code skipped:
            r1 = android.os.SystemClock.uptimeMillis();
            r6 = com.huawei.hsm.permission.StubController.getHoldService();
     */
    /* JADX WARNING: Missing block: B:19:0x0048, code skipped:
            if (r6 != null) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:21:0x004c, code skipped:
            if (HW_DEBUG == false) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:22:0x004e, code skipped:
            android.util.Slog.e(TAG, "isMaliciousApp, service is null!");
     */
    /* JADX WARNING: Missing block: B:23:0x0055, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:24:0x0056, code skipped:
            r7 = new android.os.Bundle();
            r7.putString("packageName", r15);
            r8 = null;
     */
    /* JADX WARNING: Missing block: B:27:0x0067, code skipped:
            r8 = r6.callHsmService("isVirusApk", r7);
     */
    /* JADX WARNING: Missing block: B:28:0x0069, code skipped:
            r9 = move-exception;
     */
    /* JADX WARNING: Missing block: B:29:0x006a, code skipped:
            r10 = TAG;
            r11 = new java.lang.StringBuilder();
            r11.append("callHsmService fail: ");
            r11.append(r9.getMessage());
            android.util.Slog.e(r10, r11.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isMaliciousApp(String packageName) {
        String str;
        StringBuilder stringBuilder;
        if (packageName == null) {
            return false;
        }
        synchronized (this.mVirusScanResult) {
            if (this.mVirusScanResult.containsKey(packageName)) {
                int type = ((Integer) this.mVirusScanResult.get(packageName)).intValue();
                if (type != 303) {
                    if (type != 305) {
                        return false;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("find Malicious App:");
                stringBuilder.append(packageName);
                Slog.i(str, stringBuilder.toString());
                return true;
            }
        }
        long costTime = SystemClock.uptimeMillis() - start;
        if (HW_DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isMaliciousApp cost: ");
            stringBuilder2.append(costTime);
            Slog.d(str2, stringBuilder2.toString());
        }
        if (res == null) {
            Slog.i(TAG, "isVirusApk, res is null");
            return false;
        }
        int resultCode = res.getInt("result_code", -1);
        if (resultCode != -1) {
            addVirusScanResult(packageName, resultCode);
        }
        if (resultCode != 303 && resultCode != 305) {
            return false;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("find Malicious App:");
        stringBuilder.append(packageName);
        Slog.i(str, stringBuilder.toString());
        return true;
    }

    private boolean isSystemPackageInstalled(String packageName, int flag) {
        ApplicationInfo appInfo = null;
        try {
            appInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, flag, 0);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find package:");
            stringBuilder.append(packageName);
            Log.d(str, stringBuilder.toString());
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

    private void enableTrustSpaceApp(boolean enablePackage, boolean enableLauncher, int userId) {
        IPackageManager pm = AppGlobals.getPackageManager();
        ComponentName cName = new ComponentName(HW_TRUSTSPACE_PACKAGENAME, HW_TRUSTSPACE_LAUNCHER);
        int i = 2;
        int newPackageState = enablePackage ? 0 : 2;
        if (enableLauncher) {
            i = 0;
        }
        int newLauncherState = i;
        try {
            pm.setApplicationEnabledSetting(HW_TRUSTSPACE_PACKAGENAME, newPackageState, 0, userId, null);
            if (enablePackage) {
                pm.setComponentEnabledSetting(cName, newLauncherState, 1, userId);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableTrustSpaceApp fail: ");
            stringBuilder.append(e.getMessage());
            Slog.w(str, stringBuilder.toString());
        }
    }

    private void loadSystemPackages() {
        ArraySet<Integer> tempUidSet = new ArraySet();
        ArraySet<String> tempPkgSet = new ArraySet();
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
            this.mSystemUids.addAll(tempUidSet);
            this.mSystemApps.clear();
            this.mSystemApps.addAll(tempPkgSet);
        }
    }

    private int getClonedProfileId() {
        if (!IS_SUPPORT_CLONE_APP) {
            return -1000;
        }
        try {
            for (UserInfo info : ((IUserManager) ServiceManager.getService("user")).getProfiles(0, false)) {
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

    private boolean isClonedProfile(int userId) {
        boolean z = false;
        if (!IS_SUPPORT_CLONE_APP) {
            return false;
        }
        try {
            UserInfo userInfo = ((IUserManager) ServiceManager.getService("user")).getUserInfo(userId);
            if (userInfo != null && userInfo.isClonedProfile()) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to getUserInfo");
            return false;
        }
    }

    private void updateClonedProfileUserId(int userId) {
        if (IS_SUPPORT_CLONE_APP && userId != -1000) {
            this.mClonedProfileUserId = userId;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cloned profile user");
            stringBuilder.append(userId);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private boolean checkClonedProfile(int userId) {
        boolean z = false;
        if (!IS_SUPPORT_CLONE_APP || userId == -1000) {
            return false;
        }
        if (userId == this.mClonedProfileUserId) {
            z = true;
        }
        return z;
    }

    private void initTrustSpace() {
        if (!this.mSystemReady) {
            Slog.i(TAG, "TrustSpaceManagerService init begin");
            if (isSystemPackageInstalled(HW_TRUSTSPACE_PACKAGENAME, 0)) {
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enable TrustSpace App: ");
                stringBuilder.append(this.mSupportTrustSpace);
                Slog.i(str, stringBuilder.toString());
                if (this.mSupportTrustSpace) {
                    enableTrustSpaceApp(true, this.mEnableTrustSpace, 0);
                } else {
                    disableTrustSpaceForAllUsers();
                }
                this.mSystemReady = true;
                Slog.i(TAG, "TrustSpaceManagerService init end");
                return;
            }
            Slog.e(TAG, "TrustSpace application is not exist");
        }
    }

    private void disableTrustSpaceForAllUsers() {
        try {
            for (UserInfo user : ((IUserManager) ServiceManager.getService("user")).getUsers(false)) {
                enableTrustSpaceApp(false, false, user.id);
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "disableTrustSpaceForAllUsers, failed to getUserInfo");
        }
    }

    private boolean isValidUser(int type, int userId) {
        int i = 0;
        boolean isValidUser = userId == 0;
        if (type == 1) {
            if (userId == -1) {
                i = 1;
            }
            isValidUser |= i;
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

    private boolean shouldNotify(int type, String target) {
        if ((type == 0 || type == 2 || type == 3) && !isMaliciousApp(target)) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:27:0x0059, code skipped:
            if (r7 != 0) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:28:0x005b, code skipped:
            if (r6 != 0) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:29:0x005d, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:31:0x0060, code skipped:
            if (HW_DEBUG == false) goto L_0x00b0;
     */
    /* JADX WARNING: Missing block: B:32:0x0062, code skipped:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("check Intent, type: ");
            r2.append(r15);
            r2.append(" calleePackage: ");
            r2.append(r10);
            r2.append(" calleelevel=");
            r2.append(r7);
            r2.append(" callerPid:");
            r2.append(r12);
            r2.append(" callerUid:");
            r2.append(r11);
            r2.append(" callerPackage:");
            r2.append(r13);
            r2.append(" callerLevel=");
            r2.append(r6);
            r2.append(" userId:");
            r2.append(r14);
            android.util.Slog.d(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:33:0x00b0, code skipped:
            r0 = false;
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:34:0x00b3, code skipped:
            if (r7 != 1) goto L_0x00c3;
     */
    /* JADX WARNING: Missing block: B:35:0x00b5, code skipped:
            if (r6 != 0) goto L_0x00c2;
     */
    /* JADX WARNING: Missing block: B:36:0x00b7, code skipped:
            if (r17 == false) goto L_0x00ba;
     */
    /* JADX WARNING: Missing block: B:38:0x00be, code skipped:
            if (isMaliciousApp(r13) == false) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:39:0x00c0, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:40:0x00c2, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:41:0x00c3, code skipped:
            if (r6 != 1) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:42:0x00c5, code skipped:
            if (r7 != 0) goto L_0x00d2;
     */
    /* JADX WARNING: Missing block: B:43:0x00c7, code skipped:
            if (r16 == false) goto L_0x00ca;
     */
    /* JADX WARNING: Missing block: B:45:0x00ce, code skipped:
            if (isMaliciousApp(r10) == false) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:46:0x00d0, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:47:0x00d2, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:49:0x00d4, code skipped:
            if (r7 != 2) goto L_0x00e5;
     */
    /* JADX WARNING: Missing block: B:50:0x00d6, code skipped:
            if (r6 != 0) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:51:0x00d8, code skipped:
            if (r17 == false) goto L_0x00db;
     */
    /* JADX WARNING: Missing block: B:52:0x00db, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:53:0x00e0, code skipped:
            if (shouldNotify(r9, r13) == false) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:54:0x00e2, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:55:0x00e4, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:56:0x00e5, code skipped:
            if (r6 != 2) goto L_0x00ef;
     */
    /* JADX WARNING: Missing block: B:58:0x00eb, code skipped:
            if (isMaliciousApp(r10) != false) goto L_0x00ee;
     */
    /* JADX WARNING: Missing block: B:59:0x00ed, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:60:0x00ee, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:61:0x00ef, code skipped:
            r18 = r0;
            r0 = r2;
     */
    /* JADX WARNING: Missing block: B:62:0x00f6, code skipped:
            if (isMaliciousApp(r13) != false) goto L_0x0100;
     */
    /* JADX WARNING: Missing block: B:64:0x00fc, code skipped:
            if (isMaliciousApp(r10) == false) goto L_0x0101;
     */
    /* JADX WARNING: Missing block: B:65:0x0100, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:66:0x0101, code skipped:
            r5 = r1;
     */
    /* JADX WARNING: Missing block: B:67:0x0102, code skipped:
            if (r5 == false) goto L_0x0119;
     */
    /* JADX WARNING: Missing block: B:68:0x0104, code skipped:
            if (r9 == 1) goto L_0x0119;
     */
    /* JADX WARNING: Missing block: B:69:0x0106, code skipped:
            r20 = r5;
            r21 = r6;
            r22 = r7;
            notifyIntentPrevented(r15, r10, r7, r13, r6, 1);
     */
    /* JADX WARNING: Missing block: B:70:0x0119, code skipped:
            r20 = r5;
            r21 = r6;
            r22 = r7;
     */
    /* JADX WARNING: Missing block: B:71:0x011f, code skipped:
            if (r0 == false) goto L_0x012d;
     */
    /* JADX WARNING: Missing block: B:72:0x0121, code skipped:
            notifyIntentPrevented(r15, r10, r22, r13, r21, false);
     */
    /* JADX WARNING: Missing block: B:73:0x012d, code skipped:
            if (r18 == false) goto L_0x018c;
     */
    /* JADX WARNING: Missing block: B:74:0x012f, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("prevent Intent, type: ");
            r2.append(r15);
            r2.append(" calleePackage: ");
            r2.append(r10);
            r2.append(" calleelevel=");
            r2.append(r22);
            r2.append(" callerPid:");
            r2.append(r12);
            r2.append(" callerUid:");
            r2.append(r11);
            r2.append(" callerPackage:");
            r2.append(r13);
            r2.append(" callerLevel=");
            r2.append(r21);
            r2.append(" isMalicious=");
            r2.append(r20);
            r2.append(" userId:");
            r2.append(r14);
            android.util.Slog.i(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:75:0x018c, code skipped:
            r5 = r20;
            r4 = r21;
            r3 = r22;
     */
    /* JADX WARNING: Missing block: B:76:0x0192, code skipped:
            return r18;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean checkIntent(int type, String calleePackage, int callerUid, int callerPid, String callingPackage, int userId) {
        boolean isTrustcaller;
        Throwable th;
        int calleeLevel;
        boolean z;
        boolean z2;
        int i = type;
        String str = calleePackage;
        int i2 = callerUid;
        int i3 = callerPid;
        String str2 = callingPackage;
        int i4 = userId;
        boolean z3 = false;
        if (!isUseTrustSpace() || !isValidUser(i, i4) || isSelfCall(str, str2) || isSpecialPackage(str2, i2, str)) {
            return false;
        }
        String typeString = TrustSpaceSettings.componentTypeToString(type);
        if (str == null || str2 == null) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown Intent, type: ");
            stringBuilder.append(typeString);
            stringBuilder.append(" calleePackage: ");
            stringBuilder.append(str);
            stringBuilder.append(" callerPid:");
            stringBuilder.append(i3);
            stringBuilder.append(" callerUid:");
            stringBuilder.append(i2);
            stringBuilder.append(" callerPackage:");
            stringBuilder.append(str2);
            stringBuilder.append(" userId:");
            stringBuilder.append(i4);
            Slog.w(str3, stringBuilder.toString());
            return false;
        }
        synchronized (this) {
            try {
                boolean isTrustcallee = this.mSettings.isTrustApp(str);
                try {
                    isTrustcaller = this.mSettings.isTrustApp(str2);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
                try {
                    calleeLevel = this.mSettings.getProtectionLevel(str) & 255;
                    try {
                        int callerLevel = this.mSettings.getProtectionLevel(str2) & 255;
                    } catch (Throwable th3) {
                        th = th3;
                        z = isTrustcallee;
                        z2 = isTrustcaller;
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    z = isTrustcallee;
                    throw th;
                }
                try {
                } catch (Throwable th5) {
                    th = th5;
                    z = isTrustcallee;
                    int i5 = calleeLevel;
                    z2 = isTrustcaller;
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                throw th;
            }
        }
    }

    private void notifyIntentPrevented(String typeString, String calleePackage, int calleeLevel, String callerPackage, int callerLevel, boolean isMalicious) {
        final String str = typeString;
        final String str2 = calleePackage;
        final String str3 = callerPackage;
        final int i = calleeLevel;
        final int i2 = callerLevel;
        final boolean z = isMalicious;
        UiThread.getHandler().post(new Runnable() {
            public void run() {
                Intent intent = new Intent(TrustSpaceManagerService.ACTION_INTENT_PREVENTED);
                intent.putExtra("component", str);
                intent.putExtra("callee", str2);
                intent.putExtra("caller", str3);
                intent.putExtra("calleeLevel", i);
                intent.putExtra("callerLevel", i2);
                intent.putExtra("isMalicious", z);
                intent.setPackage(TrustSpaceManagerService.HW_TRUSTSPACE_PACKAGENAME);
                TrustSpaceManagerService.this.mContext.startService(intent);
                Slog.d(TrustSpaceManagerService.TAG, "Notify intent prevented.");
            }
        });
    }

    private boolean isIntentProtectedAppInner(String packageName) {
        if (!calledFromValidUser() || packageName == null) {
            return false;
        }
        boolean isIntentProtectedApp;
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("add ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" to intent protected list, flags=");
                    stringBuilder.append(flags);
                    Slog.d(str, stringBuilder.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove ");
                stringBuilder.append(packageName);
                stringBuilder.append(" from intent protected list");
                Slog.d(str, stringBuilder.toString());
            }
        }
        return true;
    }

    public List<String> getIntentProtectedApps(int flags) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser()) {
            return new ArrayList(0);
        }
        List intentProtectedApps;
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove apps in intent protected list, flag=");
                stringBuilder.append(flags);
                Slog.d(str, stringBuilder.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update trust apps, flag=");
                stringBuilder.append(flag);
                Slog.d(str, stringBuilder.toString());
            }
        }
        return true;
    }

    public boolean isIntentProtectedApp(String packageName) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packageName == null) {
            return false;
        }
        boolean isIntentProtectedApp;
        synchronized (this) {
            isIntentProtectedApp = this.mSettings.isIntentProtectedApp(packageName);
        }
        return isIntentProtectedApp;
    }

    public int getProtectionLevel(String packageName) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (!calledFromValidUser() || packageName == null) {
            return 0;
        }
        int protectionLevel;
        synchronized (this) {
            protectionLevel = this.mSettings.getProtectionLevel(packageName);
        }
        return protectionLevel;
    }

    public boolean isSupportTrustSpace() {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        if (calledFromValidUser()) {
            return this.mSupportTrustSpace;
        }
        return false;
    }

    public boolean isHwTrustSpace(int userId) {
        this.mContext.enforceCallingOrSelfPermission(MANAGE_TRUSTSPACE, null);
        boolean z = false;
        if (!calledFromValidUser()) {
            return false;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = ((IUserManager) ServiceManager.getService("user")).getUserInfo(userId);
            if (userInfo != null && userInfo.isHwTrustSpace()) {
                z = true;
            }
            Binder.restoreCallingIdentity(ident);
            return z;
        } catch (RemoteException e) {
            Slog.d(TAG, "failed to getUserInfo");
            Binder.restoreCallingIdentity(ident);
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }
}

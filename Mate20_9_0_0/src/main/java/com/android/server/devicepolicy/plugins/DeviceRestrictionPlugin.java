package com.android.server.devicepolicy.plugins;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.hdm.HwDeviceManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowManager.Stub;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import huawei.android.os.HwProtectAreaManager;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DeviceRestrictionPlugin extends DevicePolicyPlugin {
    private static final String DISABLE_APPLICATIONS_LIST = "disable-applications-list";
    private static final String DISABLE_APPLICATIONS_LIST_ITEM = "disable-applications-list/disable-applications-list-item";
    private static final String DISABLE_CHANGE_WALLPAPER = "disable-change-wallpaper";
    private static final String DISABLE_HEADPHONE = "disable-headphone";
    private static final String DISABLE_MICROPHONE = "disable-microphone";
    private static final String DISABLE_NAVIGATIONBAR = "disable-navigationbar";
    private static final String DISABLE_NOTIFICATION = "disable-notification";
    private static final String DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
    private static final String DISABLE_SYSTEM_BROWSER = "disable-system-browser";
    private static final String DISABLE_SYSTEM_UPDATE = "disable-system-update";
    private static final String FLOAT_TASK_STATE = "float_task_state";
    private static final int FLOAT_TASK_STATE_OFF = 0;
    private static final int FLOAT_TASK_STATE_ON = 1;
    private static final String KEY_BOOT_MDM = "boot_alarm_mdm";
    public static final String PERMISSION_MDM_UPDATESTATE_MANAGER = "com.huawei.permission.sec.MDM_UPDATESTATE_MANAGER";
    private static final String SETTINGS_FALLBACK_ACTIVITY_NAME = "com.android.settings.FallbackHome";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    public static final String TAG = DeviceRestrictionPlugin.class.getSimpleName();
    private AlarmManager mAlarmManager;
    private PendingIntent mBootPendingIntent;
    ArrayList<String> mDisableApplicationsList = new ArrayList();
    Handler mHandler = new Handler(Looper.myLooper());
    private PendingIntent mShutdownPendingIntent;

    public DeviceRestrictionPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(DISABLE_SYSTEM_UPDATE, PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-system-browser", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-screen-capture", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-notification", PolicyType.STATE, new String[]{"value"});
        struct.addStruct(DISABLE_APPLICATIONS_LIST_ITEM, PolicyType.LIST, new String[]{"value"});
        struct.addStruct("disable-clipboard", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-google-account-autosync", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-microphone", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-headphone", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-send-notification", PolicyType.STATE, new String[]{"value"});
        struct.addStruct(DISABLE_CHANGE_WALLPAPER, PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-power-shutdown", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-shutdownmenu", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-volume", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-fingerprint-authentication", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("force-enable-wifi", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("force-enable-BT", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable_voice_outgoing", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable_voice_incoming", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-navigationbar", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-charge", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("force_scheduled_power_on", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("force_scheduled_power_off", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable_float_task", PolicyType.STATE, new String[]{"value"});
        struct.addStruct("policy-file-share-disabled", PolicyType.STATE, new String[]{"value"});
        return struct;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkCallingPermission(ComponentName who, String policyName) {
        boolean z;
        HwLog.i(TAG, "checkCallingPermission");
        switch (policyName.hashCode()) {
            case -2065188737:
                if (policyName.equals("force_scheduled_power_on")) {
                    z = true;
                    break;
                }
            case -1462770845:
                if (policyName.equals("disable-applications-list")) {
                    z = true;
                    break;
                }
            case -1418767509:
                if (policyName.equals("disable-send-notification")) {
                    z = true;
                    break;
                }
            case -1333051633:
                if (policyName.equals("disable-system-browser")) {
                    z = true;
                    break;
                }
            case -694001423:
                if (policyName.equals("disable-clipboard")) {
                    z = true;
                    break;
                }
            case -595558097:
                if (policyName.equals("disable-microphone")) {
                    z = true;
                    break;
                }
            case -304109734:
                if (policyName.equals("disable-navigationbar")) {
                    z = true;
                    break;
                }
            case -168307399:
                if (policyName.equals("disable-charge")) {
                    z = true;
                    break;
                }
            case -31729136:
                if (policyName.equals("disable_voice_outgoing")) {
                    z = true;
                    break;
                }
            case -614710:
                if (policyName.equals("disable_voice_incoming")) {
                    z = true;
                    break;
                }
            case 153563136:
                if (policyName.equals("policy-file-share-disabled")) {
                    z = true;
                    break;
                }
            case 382441887:
                if (policyName.equals("disable-volume")) {
                    z = true;
                    break;
                }
            case 403658447:
                if (policyName.equals("force_scheduled_power_off")) {
                    z = true;
                    break;
                }
            case 458488698:
                if (policyName.equals("disable-shutdownmenu")) {
                    z = true;
                    break;
                }
            case 476421226:
                if (policyName.equals("disable-screen-capture")) {
                    z = true;
                    break;
                }
            case 539407267:
                if (policyName.equals("disable-power-shutdown")) {
                    z = true;
                    break;
                }
            case 594183088:
                if (policyName.equals("disable-notification")) {
                    z = true;
                    break;
                }
            case 702979817:
                if (policyName.equals("disable-headphone")) {
                    z = true;
                    break;
                }
            case 731920490:
                if (policyName.equals(DISABLE_CHANGE_WALLPAPER)) {
                    z = true;
                    break;
                }
            case 775851010:
                if (policyName.equals(DISABLE_SYSTEM_UPDATE)) {
                    z = false;
                    break;
                }
            case 1389850009:
                if (policyName.equals("disable-google-account-autosync")) {
                    z = true;
                    break;
                }
            case 1658369855:
                if (policyName.equals("disable_float_task")) {
                    z = true;
                    break;
                }
            case 1785346365:
                if (policyName.equals("force-enable-wifi")) {
                    z = true;
                    break;
                }
            case 1946452102:
                if (policyName.equals("disable-fingerprint-authentication")) {
                    z = true;
                    break;
                }
            case 1981742202:
                if (policyName.equals("force-enable-BT")) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                this.mContext.enforceCallingOrSelfPermission(PERMISSION_MDM_UPDATESTATE_MANAGER, "does not have system_update_management MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CAPTURE_SCREEN", "does not have capture_screen_management MDM permission!");
                break;
            case true:
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_CLIPBOARD", "does not have clipboard MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_GOOGLE_ACCOUNT", "does not have google account MDM permission!");
                break;
            case true:
            case true:
            case true:
            case true:
            case true:
            case true:
            case true:
                HwLog.i(TAG, "check the calling Permission");
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                return true;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_FINGERPRINT", "does not have fingerprint MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_WIFI", "does not have wifi MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_BLUETOOTH", "does not have bluetooth MDM permission!");
                break;
            case true:
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_PHONE", "does not have google account MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
            case true:
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                break;
        }
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        String str = policyName;
        Bundle bundle = policyData;
        HwLog.i(TAG, "onSetPolicy");
        boolean isSetSucess = true;
        PackageManager pm = this.mContext.getPackageManager();
        long token = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            switch (policyName.hashCode()) {
                case -2065188737:
                    if (str.equals("force_scheduled_power_on")) {
                        z = true;
                        break;
                    }
                    break;
                case -1462770845:
                    if (str.equals("disable-applications-list")) {
                        z = true;
                        break;
                    }
                    break;
                case -694001423:
                    if (str.equals("disable-clipboard")) {
                        z = true;
                        break;
                    }
                    break;
                case -168307399:
                    if (str.equals("disable-charge")) {
                        z = true;
                        break;
                    }
                    break;
                case 403658447:
                    if (str.equals("force_scheduled_power_off")) {
                        z = true;
                        break;
                    }
                    break;
                case 476421226:
                    if (str.equals("disable-screen-capture")) {
                        z = true;
                        break;
                    }
                    break;
                case 775851010:
                    if (str.equals(DISABLE_SYSTEM_UPDATE)) {
                        z = false;
                        break;
                    }
                    break;
                case 1389850009:
                    if (str.equals("disable-google-account-autosync")) {
                        z = true;
                        break;
                    }
                    break;
                case 1658369855:
                    if (str.equals("disable_float_task")) {
                        z = true;
                        break;
                    }
                    break;
                case 1785346365:
                    if (str.equals("force-enable-wifi")) {
                        z = true;
                        break;
                    }
                    break;
                case 1981742202:
                    if (str.equals("force-enable-BT")) {
                        z = true;
                        break;
                    }
                    break;
                default:
                    break;
            }
            int i;
            String app;
            switch (z) {
                case false:
                    if (effective) {
                        isSetSucess = disableSystemUpdate(bundle.getBoolean("value"));
                        break;
                    }
                    break;
                case true:
                    ArrayList<String> list = bundle.getStringArrayList("value");
                    if (!(list == null || list.size() == 0)) {
                        int j = list.size();
                        for (i = 0; i < j; i++) {
                            app = (String) list.get(i);
                            isSetSucess = disableComponentForPackage(app, true, pm, 0);
                            this.mDisableApplicationsList.add(app);
                        }
                        break;
                    }
                case true:
                    i = UserHandle.getCallingUserId();
                    synchronized (this) {
                        isSetSucess = updateScreenCaptureDisabledInWindowManager(i, bundle.getBoolean("value"));
                    }
                    break;
                case true:
                    break;
                case true:
                    z = bundle.getBoolean("value", false);
                    if (effective && z) {
                        new Thread(new Runnable() {
                            public void run() {
                                DeviceRestrictionPlugin.this.disableGoogleAccountSyncAutomatically();
                            }
                        }).start();
                        break;
                    }
                case true:
                    openWifi();
                    break;
                case true:
                    openBt();
                    break;
                case true:
                    if (!bundle.getBoolean("value", false)) {
                        enableCharging(true);
                        break;
                    }
                    enableCharging(false);
                    break;
                case true:
                    if (!bundle.getBoolean("value", false)) {
                        HwLog.i(TAG, " ORCE_SCHEDULED_POWER_ON cancel");
                        cancelAlarm(this.mBootPendingIntent);
                        break;
                    }
                    long whenBoot = bundle.getLong("when", 0);
                    String boradcastBoot = bundle.getString("boradcast", "");
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" FORCE_SCHEDULED_POWER_ON boradcast:");
                    stringBuilder.append(boradcastBoot);
                    stringBuilder.append(",when:");
                    stringBuilder.append(whenBoot);
                    HwLog.i(str2, stringBuilder.toString());
                    Intent intent = new Intent(boradcastBoot);
                    intent.putExtra(KEY_BOOT_MDM, true);
                    this.mBootPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
                    setAlarm(whenBoot, this.mBootPendingIntent);
                    break;
                case true:
                    if (!bundle.getBoolean("value", false)) {
                        HwLog.i(TAG, " FORCE_SCHEDULED_POWER_OFF cancel");
                        cancelAlarm(this.mShutdownPendingIntent);
                        break;
                    }
                    long whenShutdown = bundle.getLong("when", 0);
                    app = bundle.getString("boradcast", "");
                    String str3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" FORCE_SCHEDULED_POWER_OFF boradcast:");
                    stringBuilder2.append(app);
                    stringBuilder2.append(",when:");
                    stringBuilder2.append(whenShutdown);
                    HwLog.i(str3, stringBuilder2.toString());
                    this.mShutdownPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(app), 0);
                    setAlarm(whenShutdown, this.mShutdownPendingIntent);
                    break;
                case true:
                    z = isFloatTaskRunning();
                    storeFloatTaskState(z);
                    if (z) {
                        setFloatTaskEnabled(false);
                        break;
                    }
                    break;
                default:
                    break;
            }
            Binder.restoreCallingIdentity(token);
            return isSetSucess;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void onSetPolicyCompleted(ComponentName who, String policyName, boolean changed) {
        HwLog.i(TAG, "onSetPolicyCompleted");
        long token = Binder.clearCallingIdentity();
        Object obj = -1;
        try {
            if (policyName.hashCode() == -304109734) {
                if (policyName.equals("disable-navigationbar")) {
                    obj = null;
                }
            }
            if (obj == null) {
                changeNavigationBarStatus(UserHandle.getCallingUserId(), HwDeviceManager.disallowOp(true));
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void setAlarm(long triggerAtMillis, PendingIntent intent) {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        }
        this.mAlarmManager.setExact(0, triggerAtMillis, intent);
    }

    private void cancelAlarm(PendingIntent intent) {
        if (this.mAlarmManager != null && intent != null) {
            this.mAlarmManager.cancel(intent);
        }
    }

    private void enableCharging(boolean charge) {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager == null) {
            HwLog.w(TAG, "powermanager is null");
        } else {
            powerManager.setPowerState(charge);
        }
    }

    private void changeNavigationBarStatus(int navBarUserHandle, boolean nav_policyData) {
        String enableNavbar;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MDM policy changeNavigationBarStatus nav_policyData = ");
        stringBuilder.append(nav_policyData);
        HwLog.i(str, stringBuilder.toString());
        str = "1";
        String NAVIGATIONBAR_DISABLE = "0";
        if (nav_policyData) {
            enableNavbar = "0";
        } else {
            enableNavbar = "1";
        }
        List<UserInfo> userInfo = UserManager.get(this.mContext).getUsers();
        Intent intent = new Intent("com.huawei.navigationbar.statuschange");
        intent.setPackage(FingerViewController.PKGNAME_OF_KEYGUARD);
        intent.putExtra("minNavigationBar", nav_policyData);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        int usersSize = userInfo.size();
        for (int i = 0; i < usersSize; i++) {
            System.putStringForUser(this.mContext.getContentResolver(), "enable_navbar", enableNavbar, ((UserInfo) userInfo.get(i)).id);
        }
    }

    private void openWifi() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private void openBt() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.enable();
        }
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        HwLog.i(TAG, "onRemovePolicy");
        PackageManager pm = this.mContext.getPackageManager();
        long token = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            int hashCode = policyName.hashCode();
            if (hashCode != -1462770845) {
                if (hashCode == 1658369855) {
                    if (policyName.equals("disable_float_task")) {
                        z = true;
                    }
                }
            } else if (policyName.equals("disable-applications-list")) {
                z = false;
            }
            switch (z) {
                case false:
                    ArrayList<String> list = policyData.getStringArrayList("value");
                    if (!(list == null || list.size() == 0)) {
                        int j = list.size();
                        for (hashCode = 0; hashCode < j; hashCode++) {
                            disableComponentForPackage((String) list.get(hashCode), false, pm, 0);
                        }
                        break;
                    }
                case true:
                    if (isFloatTaskEnableBefore()) {
                        setFloatTaskEnabled(true);
                        break;
                    }
                    break;
                default:
                    break;
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00a8 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x009b A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0078 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00a8 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x009b A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0078 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00a8 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x009b A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0078 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006d A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00a8 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x009b A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0078 A:{Catch:{ all -> 0x00b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x006e A:{Catch:{ all -> 0x00b3 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        PackageManager pm = this.mContext.getPackageManager();
        long token = Binder.clearCallingIdentity();
        try {
            Iterator it = removedPolicies.iterator();
            while (it.hasNext()) {
                boolean z;
                String policyName = ((PolicyItem) it.next()).getPolicyName();
                int hashCode = policyName.hashCode();
                if (hashCode == -1462770845) {
                    if (policyName.equals("disable-applications-list")) {
                        z = true;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 476421226) {
                    if (policyName.equals("disable-screen-capture")) {
                        z = true;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 775851010) {
                    if (policyName.equals(DISABLE_SYSTEM_UPDATE)) {
                        z = false;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (hashCode == 1658369855) {
                    if (policyName.equals("disable_float_task")) {
                        z = true;
                        int userHandle;
                        switch (z) {
                            case false:
                                disableSystemUpdate(false);
                                break;
                            case true:
                                userHandle = UserHandle.getCallingUserId();
                                synchronized (this) {
                                    updateScreenCaptureDisabledInWindowManager(userHandle, false);
                                }
                                break;
                            case true:
                                if (!(this.mDisableApplicationsList == null || this.mDisableApplicationsList.size() == 0)) {
                                    int j = this.mDisableApplicationsList.size();
                                    for (userHandle = 0; userHandle < j; userHandle++) {
                                        disableComponentForPackage((String) this.mDisableApplicationsList.get(userHandle), false, pm, 0);
                                    }
                                    break;
                                }
                            case true:
                                if (!isFloatTaskEnableBefore()) {
                                    break;
                                }
                                setFloatTaskEnabled(true);
                                break;
                            default:
                                break;
                        }
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
                    case true:
                        break;
                    default:
                        break;
                }
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean disableSystemUpdate(boolean disable) {
        String[] readBuf = new String[]{"AA"};
        int[] errorRet = new int[1];
        String writeValue = disable ? "1" : "0";
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("writeProtectArea :");
        stringBuilder.append(writeValue);
        HwLog.i(str, stringBuilder.toString());
        if (HwProtectAreaManager.getInstance().writeProtectArea("SYSTEM_UPDATE_STATE", writeValue.length(), writeValue, errorRet) == 0) {
            int readRet = HwProtectAreaManager.getInstance().readProtectArea("SYSTEM_UPDATE_STATE", 4, readBuf, errorRet);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("readProtectArea: readRet = ");
            stringBuilder2.append(readRet);
            stringBuilder2.append("ReadBuf = ");
            stringBuilder2.append(Arrays.toString(readBuf));
            stringBuilder2.append("errorRet = ");
            stringBuilder2.append(Arrays.toString(errorRet));
            HwLog.i(str2, stringBuilder2.toString());
            if (readRet == 0 && readBuf.length >= 1 && writeValue.equals(readBuf[0])) {
                String updateState = disable ? "false" : "true";
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("writeProtectArea Success! Set sys.update.state to ");
                stringBuilder3.append(updateState);
                Log.i(str3, stringBuilder3.toString());
                SystemProperties.set("sys.update.state", updateState);
                return true;
            }
        }
        return false;
    }

    private boolean disableComponentForPackage(String packageName, boolean disable, PackageManager pm, int userId) {
        String str = packageName;
        PackageManager packageManager = pm;
        boolean setSuccess = true;
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        boolean newState = disable ? true : false;
        LauncherApps launcherApps = (LauncherApps) this.mContext.getSystemService("launcherapps");
        try {
            int i;
            PackageInfo packageInfo = packageManager.getPackageInfo(str, 786959);
            if (!(packageInfo == null || packageInfo.receivers == null || packageInfo.receivers.length == 0)) {
                for (ActivityInfo activityInfo : packageInfo.receivers) {
                    packageManager.setComponentEnabledSetting(new ComponentName(str, activityInfo.name), newState, 0);
                }
            }
            if (!(packageInfo == null || packageInfo.services == null || packageInfo.services.length == 0)) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    packageManager.setComponentEnabledSetting(new ComponentName(str, serviceInfo.name), newState, 0);
                }
            }
            if (!(packageInfo == null || packageInfo.providers == null || packageInfo.providers.length == 0)) {
                for (ProviderInfo providerInfo : packageInfo.providers) {
                    packageManager.setComponentEnabledSetting(new ComponentName(str, providerInfo.name), newState, 0);
                }
            }
            if (!(packageInfo == null || packageInfo.activities == null || packageInfo.activities.length == 0)) {
                i = 0;
                while (i < packageInfo.activities.length) {
                    ComponentName componentName = new ComponentName(str, packageInfo.activities[i].name);
                    if (disable) {
                        for (LauncherActivityInfo launcherApp : launcherApps.getActivityList(str, new UserHandle(UserHandle.myUserId()))) {
                            if (!(launcherApp.getComponentName().getClassName().contains(packageInfo.activities[i].name) || packageInfo.activities[i].name.contains(SETTINGS_FALLBACK_ACTIVITY_NAME))) {
                                packageManager.setComponentEnabledSetting(componentName, newState, 0);
                            }
                        }
                    } else {
                        packageManager.setComponentEnabledSetting(componentName, newState, 0);
                    }
                    i++;
                }
            }
            if (disable) {
                packageManager.clearPackagePreferredActivities(str);
            }
        } catch (NameNotFoundException e) {
            setSuccess = false;
        }
        pm.flushPackageRestrictionsAsUser(userId);
        if (disable) {
            killApplicationInner(packageName);
        }
        return setSuccess;
    }

    private void killApplicationInner(String packageName) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<RunningTaskInfo> taskList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(ActivityManager.getMaxRecentTasksStatic());
            if (!(taskList == null || taskList.isEmpty())) {
                for (RunningTaskInfo ti : taskList) {
                    ComponentName baseActivity = ti.baseActivity;
                    if (baseActivity != null && TextUtils.equals(baseActivity.getPackageName(), packageName)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("The killed packageName: ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(" task id: ");
                        stringBuilder.append(ti.id);
                        HwLog.d(str, stringBuilder.toString());
                        ActivityManager.getService().removeTask(ti.id);
                    }
                }
            }
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("killApplicationInner pkg:");
            stringBuilder2.append(packageName);
            stringBuilder2.append(", RemoteException");
            HwLog.d(str2, stringBuilder2.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        Binder.restoreCallingIdentity(ident);
    }

    IWindowManager getIWindowManager() {
        return Stub.asInterface(ServiceManager.getService("window"));
    }

    private boolean updateScreenCaptureDisabledInWindowManager(int userHandle, boolean disabled) {
        try {
            getIWindowManager().refreshScreenCaptureDisabled(userHandle);
            return true;
        } catch (RemoteException e) {
            HwLog.e(TAG, "Unable to notify WindowManager.");
            return false;
        }
    }

    private void disableGoogleAccountSyncAutomatically() {
        Throwable th;
        String GOOGLE_ACCOUNT_TYPE = "com.google";
        long identityToken = Binder.clearCallingIdentity();
        try {
            SyncAdapterType[] syncs = ContentResolver.getSyncAdapterTypes();
            try {
                Account[] accounts = AccountManager.get(this.mContext).getAccountsByType("com.google");
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get google accounts, size=");
                stringBuilder.append(accounts.length);
                HwLog.i(str, stringBuilder.toString());
                for (Account account : accounts) {
                    for (SyncAdapterType adapter : syncs) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getCurrentSyncs, type=");
                        stringBuilder2.append(adapter.accountType);
                        stringBuilder2.append(", authority=");
                        stringBuilder2.append(adapter.authority);
                        stringBuilder2.append(", visible=");
                        stringBuilder2.append(adapter.isUserVisible());
                        HwLog.i(str2, stringBuilder2.toString());
                        if ("com.google".equals(adapter.accountType) && adapter.isUserVisible() && ContentResolver.getSyncAutomatically(account, adapter.authority)) {
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("setSyncAutomatically to false for google account authority: ");
                            stringBuilder3.append(adapter.authority);
                            HwLog.i(str3, stringBuilder3.toString());
                            ContentResolver.setSyncAutomatically(account, adapter.authority, false);
                        }
                    }
                }
                Binder.restoreCallingIdentity(identityToken);
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(identityToken);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    private void setFloatTaskEnabled(boolean enable) {
        Secure.putIntForUser(this.mContext.getContentResolver(), FLOAT_TASK_STATE, enable, UserHandle.myUserId());
    }

    private boolean isFloatTaskRunning() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), FLOAT_TASK_STATE, 0, UserHandle.myUserId()) == 1;
    }

    private boolean isFloatTaskEnableBefore() {
        return System.getInt(this.mContext.getContentResolver(), "float_task_state_before", 0) == 1;
    }

    private void storeFloatTaskState(boolean enable) {
        System.putInt(this.mContext.getContentResolver(), "float_task_state_before", enable);
    }
}

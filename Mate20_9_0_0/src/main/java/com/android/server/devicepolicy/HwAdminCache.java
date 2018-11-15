package com.android.server.devicepolicy;

import android.os.Bundle;
import com.android.server.devicepolicy.plugins.SettingsMDMPlugin;
import java.util.ArrayList;
import java.util.List;

public class HwAdminCache {
    public static final int DISABLED_DEACTIVE_MDM_PACKAGES = 18;
    public static final int DISABLE_ADB = 11;
    public static final int DISABLE_APPLICATIONS_LIST = 28;
    public static final String DISABLE_APPLICATIONS_LIST_POLICY = "disable-applications-list";
    public static final int DISABLE_BACK = 16;
    public static final int DISABLE_BLUETOOTH = 8;
    public static final int DISABLE_CHANGE_LAUNCHER = 17;
    public static final int DISABLE_CLIPBOARD = 25;
    public static final int DISABLE_DECRYPT_SDCARD = 19;
    public static final int DISABLE_GOOGLE_ACCOUNT_AUTOSYNC = 26;
    public static final int DISABLE_GOOGLE_PLAY_STORE = 24;
    public static final String DISABLE_GOOGLE_PLAY_STORE_POLICY = "disable-google-play-store";
    public static final int DISABLE_GPS = 13;
    public static final String DISABLE_HEADPHONE = "disable-headphone";
    public static final int DISABLE_HOME = 14;
    public static final int DISABLE_INFRARED = 32;
    public static final String DISABLE_INFRARED_POLICY = "infrared_item_policy_name";
    public static final int DISABLE_INSTALLSOURCE = 2;
    public static final String DISABLE_MICROPHONE = "disable-microphone";
    public static final String DISABLE_NAVIGATIONBAR_POLICY = "disable-navigationbar";
    public static final String DISABLE_NOTIFICATION_POLICY = "disable-notification";
    public static final String DISABLE_PASSIVE_PROVIDER_POLICY = "passive_location_disallow_item";
    public static final int DISABLE_SAFEMODE = 10;
    public static final int DISABLE_SCREEN_CAPTURE = 21;
    public static final String DISABLE_SCREEN_CAPTURE_POLICY = "disable-screen-capture";
    public static final String DISABLE_SDWRITING_POLICY = "disable-sdwriting";
    public static final int DISABLE_SETTINGS = 23;
    public static final String DISABLE_SETTINGS_POLICY = "disable-settings";
    public static final String DISABLE_SYNC = "disable-sync";
    public static final int DISABLE_SYSTEM_BROWSER = 22;
    public static final String DISABLE_SYSTEM_BROWSER_POLICY = "disable-system-browser";
    public static final int DISABLE_TASK = 15;
    public static final int DISABLE_USBOTG = 12;
    public static final int DISABLE_VOICE = 1;
    public static final int DISABLE_WIFI = 0;
    public static final int DISABLE_WIFIP2P = 29;
    public static final String DISABLE_WIFIP2P_POLICY = "wifi_p2p_item_policy_name";
    public static final int DISALLOWEDRUNNING_APP_LIST = 5;
    public static final int DISALLOWEDUNINSTALL_PACKAGE_LIST = 7;
    public static final int IGNORE_FREQUENT_RELAUNCH_APP_LIST = 27;
    public static final int INSTALLPACKAGE_WHITELIST = 6;
    public static final int INSTALLSOURCE_WHITELIST = 3;
    public static final int INSTALL_APKS_BLACK_LIST = 20;
    public static final String INSTALL_APKS_BLACK_LIST_POLICY = "install-packages-black-list";
    public static final int NETWORK_ACCESS_WHITELIST = 9;
    public static final int PERSISTENTAPP_LIST = 4;
    public static final String SUPER_WHITE_LIST_APP = "super-whitelist-hwsystemmanager";
    private Bundle allowAccessibilityServices = null;
    private boolean disableAdb = false;
    private boolean disableBack = false;
    private boolean disableBluetooth = false;
    private boolean disableChangeLauncher = false;
    private Bundle disableChangeWallpaper = null;
    private boolean disableClipboard = false;
    private boolean disableDecryptSDCard = false;
    private Bundle disableFingerprintAuthentication = null;
    private boolean disableGPS = false;
    private boolean disableGoogleAccountAutosync = false;
    private boolean disableGooglePlayStore = false;
    private Bundle disableHeadphone = null;
    private boolean disableHome = false;
    private boolean disableInfrared = false;
    private boolean disableInstallSource = false;
    private Bundle disableMicrophone = null;
    private Bundle disablePassiveProvider = null;
    private Bundle disablePowerShutdown = null;
    private boolean disableSafeMode = false;
    private boolean disableScreenCapture = false;
    private Bundle disableScreenOff = null;
    private Bundle disableSendNotification = null;
    private boolean disableSettings = false;
    private Bundle disableShutdownMenu = null;
    private Bundle disableSync = null;
    private boolean disableSystemBrowser = false;
    private boolean disableTask = false;
    private boolean disableUSBOtg = false;
    private boolean disableVoice = false;
    private Bundle disableVolume = null;
    private boolean disableWifi = false;
    private boolean disableWifiP2P = false;
    private Bundle disabledAndroidAnimation = null;
    private ArrayList<String> disabledApplicationlist = null;
    private List<String> disabledDeactiveMdmPackagesList = null;
    private List<String> disallowedRunningAppList = null;
    private List<String> disallowedUninstallPackageList = null;
    private Bundle exampleValue = null;
    private Bundle forceEnableBT = null;
    private Bundle forceEnableWifi = null;
    private Bundle forceEnablefileShare = null;
    private List<String> ignoreFrequentRelaunchAppList = null;
    private ArrayList<String> installPackageBlacklist = null;
    private List<String> installPackageWhitelist = null;
    private List<String> installSourceWhitelist = null;
    private Bundle mDisableLocationMode = null;
    private Bundle mDisableLocationService = null;
    private Bundle mDisableNotificationBundle = null;
    private Bundle mDisableSDCardWritingBundle = null;
    private Bundle mDisablenNavigationBarBundle = null;
    private Object mLock = new Object();
    private Bundle mSettingsPolicyNetworkLocationStatus;
    private List<String> networkAccessWhitelist = null;
    private List<String> persistentAppList = null;
    private Bundle singleApp = null;
    private Bundle superWhiteListApp = null;

    public void syncHwAdminCache(int type, boolean value) {
        synchronized (this.mLock) {
            if (type == 8) {
                this.disableBluetooth = value;
            } else if (type != 19) {
                switch (type) {
                    case 0:
                        this.disableWifi = value;
                        break;
                    case 1:
                        this.disableVoice = value;
                        break;
                    case 2:
                        this.disableInstallSource = value;
                        break;
                    default:
                        switch (type) {
                            case 10:
                                this.disableSafeMode = value;
                                break;
                            case 11:
                                this.disableAdb = value;
                                break;
                            case 12:
                                this.disableUSBOtg = value;
                                break;
                            case 13:
                                this.disableGPS = value;
                                break;
                            case 14:
                                this.disableHome = value;
                                break;
                            case 15:
                                this.disableTask = value;
                                break;
                            case 16:
                                this.disableBack = value;
                                break;
                            case 17:
                                this.disableChangeLauncher = value;
                                break;
                        }
                        break;
                }
            } else {
                this.disableDecryptSDCard = value;
            }
        }
    }

    public void syncHwAdminCache(int type, List<String> list) {
        synchronized (this.mLock) {
            if (type == 9) {
                this.networkAccessWhitelist = list;
            } else if (type != 18) {
                switch (type) {
                    case 3:
                        this.installSourceWhitelist = list;
                        break;
                    case 4:
                        this.persistentAppList = list;
                        break;
                    case 5:
                        this.disallowedRunningAppList = list;
                        break;
                    case 6:
                        this.installPackageWhitelist = list;
                        break;
                    case 7:
                        this.disallowedUninstallPackageList = list;
                        break;
                }
            } else {
                this.disabledDeactiveMdmPackagesList = list;
            }
        }
    }

    public void syncHwAdminCache(String policyName, Bundle bundle) {
        synchronized (this.mLock) {
            boolean z = true;
            switch (policyName.hashCode()) {
                case -2032892315:
                    if (policyName.equals(DISABLE_PASSIVE_PROVIDER_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -1809352329:
                    if (policyName.equals(DISABLE_GOOGLE_PLAY_STORE_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -1462770845:
                    if (policyName.equals(DISABLE_APPLICATIONS_LIST_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -1418767509:
                    if (policyName.equals("disable-send-notification")) {
                        z = true;
                        break;
                    }
                    break;
                case -1333051633:
                    if (policyName.equals(DISABLE_SYSTEM_BROWSER_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -1032082848:
                    if (policyName.equals(DISABLE_SYNC)) {
                        z = true;
                        break;
                    }
                    break;
                case -1002053434:
                    if (policyName.equals(DISABLE_SDWRITING_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -851772941:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF)) {
                        z = true;
                        break;
                    }
                    break;
                case -694001423:
                    if (policyName.equals("disable-clipboard")) {
                        z = true;
                        break;
                    }
                    break;
                case -595558097:
                    if (policyName.equals(DISABLE_MICROPHONE)) {
                        z = true;
                        break;
                    }
                    break;
                case -414055785:
                    if (policyName.equals("policy-single-app")) {
                        z = true;
                        break;
                    }
                    break;
                case -336519577:
                    if (policyName.equals(DISABLE_WIFIP2P_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -304109734:
                    if (policyName.equals(DISABLE_NAVIGATIONBAR_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case -144764070:
                    if (policyName.equals(INSTALL_APKS_BLACK_LIST_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case 43563530:
                    if (policyName.equals(DISABLE_INFRARED_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case 114516600:
                    if (policyName.equals("xxxxx")) {
                        z = false;
                        break;
                    }
                    break;
                case 153563136:
                    if (policyName.equals("policy-file-share-disabled")) {
                        z = true;
                        break;
                    }
                    break;
                case 382441887:
                    if (policyName.equals("disable-volume")) {
                        z = true;
                        break;
                    }
                    break;
                case 458488698:
                    if (policyName.equals("disable-shutdownmenu")) {
                        z = true;
                        break;
                    }
                    break;
                case 476421226:
                    if (policyName.equals(DISABLE_SCREEN_CAPTURE_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case 539407267:
                    if (policyName.equals("disable-power-shutdown")) {
                        z = true;
                        break;
                    }
                    break;
                case 591717814:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE)) {
                        z = true;
                        break;
                    }
                    break;
                case 594183088:
                    if (policyName.equals(DISABLE_NOTIFICATION_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case 702979817:
                    if (policyName.equals(DISABLE_HEADPHONE)) {
                        z = true;
                        break;
                    }
                    break;
                case 731752599:
                    if (policyName.equals(SUPER_WHITE_LIST_APP)) {
                        z = true;
                        break;
                    }
                    break;
                case 731920490:
                    if (policyName.equals("disable-change-wallpaper")) {
                        z = true;
                        break;
                    }
                    break;
                case 853982814:
                    if (policyName.equals("ignore-frequent-relaunch-app")) {
                        z = true;
                        break;
                    }
                    break;
                case 1044365373:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION)) {
                        z = true;
                        break;
                    }
                    break;
                case 1389850009:
                    if (policyName.equals("disable-google-account-autosync")) {
                        z = true;
                        break;
                    }
                    break;
                case 1463869800:
                    if (policyName.equals(DISABLE_SETTINGS_POLICY)) {
                        z = true;
                        break;
                    }
                    break;
                case 1695181060:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST)) {
                        z = true;
                        break;
                    }
                    break;
                case 1785346365:
                    if (policyName.equals("force-enable-wifi")) {
                        z = true;
                        break;
                    }
                    break;
                case 1946452102:
                    if (policyName.equals("disable-fingerprint-authentication")) {
                        z = true;
                        break;
                    }
                    break;
                case 1947338901:
                    if (policyName.equals(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION)) {
                        z = true;
                        break;
                    }
                    break;
                case 1981742202:
                    if (policyName.equals("force-enable-BT")) {
                        z = true;
                        break;
                    }
                    break;
                case 2076917186:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE)) {
                        z = true;
                        break;
                    }
                    break;
            }
            switch (z) {
                case false:
                    this.exampleValue = bundle;
                    break;
                case true:
                    this.mSettingsPolicyNetworkLocationStatus = bundle;
                    break;
                case true:
                    this.installPackageBlacklist = bundle.getStringArrayList("value");
                    break;
                case true:
                    this.disableScreenCapture = bundle.getBoolean("value");
                    break;
                case true:
                    this.disableSystemBrowser = bundle.getBoolean("value");
                    break;
                case true:
                    this.disableSettings = bundle.getBoolean("value");
                    break;
                case true:
                    this.disableGooglePlayStore = bundle.getBoolean("value");
                    break;
                case true:
                    this.disabledApplicationlist = bundle.getStringArrayList("value");
                    break;
                case true:
                    this.mDisableSDCardWritingBundle = bundle;
                    break;
                case true:
                    this.mDisableNotificationBundle = bundle;
                    break;
                case true:
                    this.disableClipboard = bundle.getBoolean("value", false);
                    break;
                case true:
                    this.disableGoogleAccountAutosync = bundle.getBoolean("value", false);
                    break;
                case true:
                    this.ignoreFrequentRelaunchAppList = bundle.getStringArrayList("value");
                    break;
                case true:
                    this.disableMicrophone = bundle;
                    break;
                case true:
                    this.superWhiteListApp = bundle;
                    break;
                case true:
                    this.disableHeadphone = bundle;
                    break;
                case true:
                    this.disableSendNotification = bundle;
                    break;
                case true:
                    this.singleApp = bundle;
                    break;
                case true:
                    this.disableChangeWallpaper = bundle;
                    break;
                case true:
                    this.disableScreenOff = bundle;
                    break;
                case true:
                    this.disablePowerShutdown = bundle;
                    break;
                case true:
                    this.disableShutdownMenu = bundle;
                    break;
                case true:
                    this.disableVolume = bundle;
                    break;
                case true:
                    this.mDisableLocationService = bundle;
                    break;
                case true:
                    this.mDisableLocationMode = bundle;
                    break;
                case true:
                    this.disableSync = bundle;
                    break;
                case true:
                    this.disablePassiveProvider = bundle;
                    break;
                case true:
                    this.disableWifiP2P = bundle.getBoolean("wifi_p2p_policy_item_value");
                    break;
                case true:
                    this.disableInfrared = bundle.getBoolean("infrared_item_policy_value");
                    break;
                case true:
                    this.disableFingerprintAuthentication = bundle;
                    break;
                case true:
                    this.forceEnableBT = bundle;
                    break;
                case true:
                    this.forceEnableWifi = bundle;
                    break;
                case true:
                    this.allowAccessibilityServices = bundle;
                    break;
                case true:
                    this.mDisablenNavigationBarBundle = bundle;
                    break;
                case true:
                    this.forceEnablefileShare = bundle;
                    break;
                case true:
                    this.disabledAndroidAnimation = bundle;
                    break;
            }
        }
    }

    public Bundle getCachedBundle(String policyName) {
        Bundle result = null;
        synchronized (this.mLock) {
            Object obj = -1;
            switch (policyName.hashCode()) {
                case -2032892315:
                    if (policyName.equals(DISABLE_PASSIVE_PROVIDER_POLICY)) {
                        obj = 17;
                        break;
                    }
                    break;
                case -1418767509:
                    if (policyName.equals("disable-send-notification")) {
                        obj = 7;
                        break;
                    }
                    break;
                case -1032082848:
                    if (policyName.equals(DISABLE_SYNC)) {
                        obj = 16;
                        break;
                    }
                    break;
                case -1002053434:
                    if (policyName.equals(DISABLE_SDWRITING_POLICY)) {
                        obj = 3;
                        break;
                    }
                    break;
                case -851772941:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF)) {
                        obj = 10;
                        break;
                    }
                    break;
                case -595558097:
                    if (policyName.equals(DISABLE_MICROPHONE)) {
                        obj = 2;
                        break;
                    }
                    break;
                case -414055785:
                    if (policyName.equals("policy-single-app")) {
                        obj = 8;
                        break;
                    }
                    break;
                case -304109734:
                    if (policyName.equals(DISABLE_NAVIGATIONBAR_POLICY)) {
                        obj = 22;
                        break;
                    }
                    break;
                case 3694080:
                    if (policyName.equals("xxxx")) {
                        obj = 1;
                        break;
                    }
                    break;
                case 153563136:
                    if (policyName.equals("policy-file-share-disabled")) {
                        obj = 23;
                        break;
                    }
                    break;
                case 382441887:
                    if (policyName.equals("disable-volume")) {
                        obj = 13;
                        break;
                    }
                    break;
                case 458488698:
                    if (policyName.equals("disable-shutdownmenu")) {
                        obj = 12;
                        break;
                    }
                    break;
                case 539407267:
                    if (policyName.equals("disable-power-shutdown")) {
                        obj = 11;
                        break;
                    }
                    break;
                case 591717814:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE)) {
                        obj = 15;
                        break;
                    }
                    break;
                case 594183088:
                    if (policyName.equals(DISABLE_NOTIFICATION_POLICY)) {
                        obj = 4;
                        break;
                    }
                    break;
                case 702979817:
                    if (policyName.equals(DISABLE_HEADPHONE)) {
                        obj = 6;
                        break;
                    }
                    break;
                case 731752599:
                    if (policyName.equals(SUPER_WHITE_LIST_APP)) {
                        obj = 5;
                        break;
                    }
                    break;
                case 731920490:
                    if (policyName.equals("disable-change-wallpaper")) {
                        obj = 9;
                        break;
                    }
                    break;
                case 1044365373:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION)) {
                        obj = null;
                        break;
                    }
                    break;
                case 1695181060:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST)) {
                        obj = 21;
                        break;
                    }
                    break;
                case 1785346365:
                    if (policyName.equals("force-enable-wifi")) {
                        obj = 20;
                        break;
                    }
                    break;
                case 1946452102:
                    if (policyName.equals("disable-fingerprint-authentication")) {
                        obj = 18;
                        break;
                    }
                    break;
                case 1947338901:
                    if (policyName.equals(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION)) {
                        obj = 24;
                        break;
                    }
                    break;
                case 1981742202:
                    if (policyName.equals("force-enable-BT")) {
                        obj = 19;
                        break;
                    }
                    break;
                case 2076917186:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE)) {
                        obj = 14;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    result = this.mSettingsPolicyNetworkLocationStatus;
                    break;
                case 1:
                    result = this.exampleValue;
                    break;
                case 2:
                    result = this.disableMicrophone;
                    break;
                case 3:
                    result = this.mDisableSDCardWritingBundle;
                    break;
                case 4:
                    result = this.mDisableNotificationBundle;
                    break;
                case 5:
                    result = this.superWhiteListApp;
                    break;
                case 6:
                    result = this.disableHeadphone;
                    break;
                case 7:
                    result = this.disableSendNotification;
                    break;
                case 8:
                    result = this.singleApp;
                    break;
                case 9:
                    result = this.disableChangeWallpaper;
                    break;
                case 10:
                    result = this.disableScreenOff;
                    break;
                case 11:
                    result = this.disablePowerShutdown;
                    break;
                case 12:
                    result = this.disableShutdownMenu;
                    break;
                case 13:
                    result = this.disableVolume;
                    break;
                case 14:
                    result = this.mDisableLocationService;
                    break;
                case 15:
                    result = this.mDisableLocationMode;
                    break;
                case 16:
                    result = this.disableSync;
                    break;
                case 17:
                    result = this.disablePassiveProvider;
                    break;
                case 18:
                    result = this.disableFingerprintAuthentication;
                    break;
                case 19:
                    result = this.forceEnableBT;
                    break;
                case 20:
                    result = this.forceEnableWifi;
                    break;
                case 21:
                    result = this.allowAccessibilityServices;
                    break;
                case 22:
                    result = this.mDisablenNavigationBarBundle;
                    break;
                case 23:
                    result = this.forceEnablefileShare;
                    break;
                case 24:
                    result = this.disabledAndroidAnimation;
                    break;
            }
        }
        return result;
    }

    public boolean getCachedValue(int type) {
        boolean result = false;
        synchronized (this.mLock) {
            if (type == 8) {
                result = this.disableBluetooth;
            } else if (type == 19) {
                result = this.disableDecryptSDCard;
            } else if (type == 29) {
                result = this.disableWifiP2P;
            } else if (type != 32) {
                switch (type) {
                    case 0:
                        result = this.disableWifi;
                        break;
                    case 1:
                        result = this.disableVoice;
                        break;
                    case 2:
                        result = this.disableInstallSource;
                        break;
                    default:
                        switch (type) {
                            case 10:
                                result = this.disableSafeMode;
                                break;
                            case 11:
                                result = this.disableAdb;
                                break;
                            case 12:
                                result = this.disableUSBOtg;
                                break;
                            case 13:
                                result = this.disableGPS;
                                break;
                            case 14:
                                result = this.disableHome;
                                break;
                            case 15:
                                result = this.disableTask;
                                break;
                            case 16:
                                result = this.disableBack;
                                break;
                            case 17:
                                result = this.disableChangeLauncher;
                                break;
                            default:
                                switch (type) {
                                    case 21:
                                        result = this.disableScreenCapture;
                                        break;
                                    case 22:
                                        result = this.disableSystemBrowser;
                                        break;
                                    case 23:
                                        result = this.disableSettings;
                                        break;
                                    case 24:
                                        result = this.disableGooglePlayStore;
                                        break;
                                    case 25:
                                        result = this.disableClipboard;
                                        break;
                                    case 26:
                                        result = this.disableGoogleAccountAutosync;
                                        break;
                                }
                                break;
                        }
                }
            } else {
                result = this.disableInfrared;
            }
        }
        return result;
    }

    public List<String> getCachedList(int type) {
        List<String> result = null;
        synchronized (this.mLock) {
            if (type == 9) {
                result = this.networkAccessWhitelist;
            } else if (type == 18) {
                result = this.disabledDeactiveMdmPackagesList;
            } else if (type != 20) {
                switch (type) {
                    case 3:
                        result = this.installSourceWhitelist;
                        break;
                    case 4:
                        result = this.persistentAppList;
                        break;
                    case 5:
                        result = this.disallowedRunningAppList;
                        break;
                    case 6:
                        result = this.installPackageWhitelist;
                        break;
                    case 7:
                        result = this.disallowedUninstallPackageList;
                        break;
                    default:
                        switch (type) {
                            case 27:
                                result = this.ignoreFrequentRelaunchAppList;
                                break;
                            case 28:
                                result = this.disabledApplicationlist;
                                break;
                        }
                        break;
                }
            } else {
                result = this.installPackageBlacklist;
            }
        }
        return result;
    }
}

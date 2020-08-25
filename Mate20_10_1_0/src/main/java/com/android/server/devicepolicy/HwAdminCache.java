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
    public static final String DISABLE_SCREEN_TURN_OFF = "disable-screen-turn-off";
    public static final String DISABLE_SDWRITING_POLICY = "disable-sdwriting";
    public static final int DISABLE_SETTINGS = 23;
    public static final String DISABLE_SETTINGS_POLICY = "disable-settings";
    public static final String DISABLE_STATUS_BAR = "disable_status_bar";
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
    public static final String POLICY_DEFAULTE_LAUNCHER = "set-default-launcher";
    public static final String POLICY_DISABLE_MULTIWINDOW = "disable-multi-window";
    public static final String POLICY_NETWORK_BLACK_DOMAIN_LIST = "network-black-domain-list";
    public static final String POLICY_NETWORK_BLACK_IP_LIST = "network-black-ip-list";
    public static final String POLICY_NETWORK_WHITE_DOMAIN_LIST = "network-white-domain-list";
    public static final String POLICY_NETWORK_WHITE_IP_LIST = "network-white-ip-list";
    public static final String SUPER_WHITE_LIST_APP = "super-whitelist-hwsystemmanager";
    public static final String UNAVAILABLE_SSID_LIST = "unavailable-ssid-list";
    private Bundle allowAccessibilityServices = null;
    private Bundle disableChangeWallpaper = null;
    private Bundle disableFingerprintAuthentication = null;
    private Bundle disableHeadphone = null;
    private Bundle disableMicrophone = null;
    private Bundle disablePassiveProvider = null;
    private Bundle disablePowerShutdown = null;
    private Bundle disableScreenOff = null;
    private Bundle disableSendNotification = null;
    private Bundle disableShutdownMenu = null;
    private Bundle disableSync = null;
    private Bundle disableVolume = null;
    private Bundle disabledAndroidAnimation = null;
    private ArrayList<String> disabledApplicationlist = null;
    private List<String> disabledDeactiveMdmPackagesList = null;
    private List<String> disallowedRunningAppList = null;
    private List<String> disallowedUninstallPackageList = null;
    private Bundle exampleValue = null;
    private Bundle forceEnableBt = null;
    private Bundle forceEnableWifi = null;
    private Bundle forceEnablefileShare = null;
    private List<String> ignoreFrequentRelaunchAppList = null;
    private ArrayList<String> installPackageBlacklist = null;
    private List<String> installPackageWhitelist = null;
    private List<String> installSourceWhitelist = null;
    private boolean isDisableAdb = false;
    private boolean isDisableBack = false;
    private boolean isDisableBluetooth = false;
    private boolean isDisableChangeLauncher = false;
    private boolean isDisableClipboard = false;
    private boolean isDisableDecryptSdCard = false;
    private boolean isDisableGoogleAccountAutosync = false;
    private boolean isDisableGooglePlayStore = false;
    private boolean isDisableGps = false;
    private boolean isDisableHome = false;
    private boolean isDisableInfrared = false;
    private boolean isDisableInstallSource = false;
    private boolean isDisableSafeMode = false;
    private boolean isDisableScreenCapture = false;
    private boolean isDisableSettings = false;
    private boolean isDisableSystemBrowser = false;
    private boolean isDisableTask = false;
    private boolean isDisableUsbOtg = false;
    private boolean isDisableVoice = false;
    private boolean isDisableWifi = false;
    private boolean isDisableWifiP2P = false;
    private Bundle isSleepByPowerButtonDisabled = null;
    private Bundle mDefaultLauncher = null;
    private Bundle mDisableLocationMode = null;
    private Bundle mDisableLocationService = null;
    private Bundle mDisableNotificationBundle = null;
    private Bundle mDisableSDCardWritingBundle = null;
    private Bundle mDisableStatusBarBundle = null;
    private Bundle mDisabledApplicationLock = null;
    private Bundle mDisabledMultiWindow = null;
    private Bundle mDisabledParentControl = null;
    private Bundle mDisabledPhoneFind = null;
    private Bundle mDisabledSimLock = null;
    private Bundle mDisablenNavigationBarBundle = null;
    private Bundle mGetUnavailableSsidList = null;
    private final Object mLock = new Object();
    private Bundle mNetworkBlackDomainList = null;
    private Bundle mNetworkBlackIpList = null;
    private Bundle mNetworkWhiteDomainList = null;
    private Bundle mNetworkWhiteIpList = null;
    private Bundle mSettingsPolicyForceEncryptSdcard;
    private Bundle mSettingsPolicyNetworkLocationStatus;
    private List<String> networkAccessWhitelist = null;
    private List<String> persistentAppList = null;
    private Bundle singleApp = null;
    private Bundle superWhiteListApp = null;

    public void syncHwAdminCache(int type, boolean isDisable) {
        synchronized (this.mLock) {
            if (type == 0) {
                this.isDisableWifi = isDisable;
            } else if (type == 1) {
                this.isDisableVoice = isDisable;
            } else if (type == 2) {
                this.isDisableInstallSource = isDisable;
            } else if (type == 8) {
                this.isDisableBluetooth = isDisable;
            } else if (type != 19) {
                switch (type) {
                    case 10:
                        this.isDisableSafeMode = isDisable;
                        break;
                    case 11:
                        this.isDisableAdb = isDisable;
                        break;
                    case 12:
                        this.isDisableUsbOtg = isDisable;
                        break;
                    case 13:
                        this.isDisableGps = isDisable;
                        break;
                    case 14:
                        this.isDisableHome = isDisable;
                        break;
                    case 15:
                        this.isDisableTask = isDisable;
                        break;
                    case 16:
                        this.isDisableBack = isDisable;
                        break;
                    case 17:
                        this.isDisableChangeLauncher = isDisable;
                        break;
                }
            } else {
                this.isDisableDecryptSdCard = isDisable;
            }
        }
    }

    public void syncHwAdminCache(int type, List<String> list) {
        synchronized (this.mLock) {
            if (type == 3) {
                this.installSourceWhitelist = list;
            } else if (type == 4) {
                this.persistentAppList = list;
            } else if (type == 5) {
                this.disallowedRunningAppList = list;
            } else if (type == 6) {
                this.installPackageWhitelist = list;
            } else if (type == 7) {
                this.disallowedUninstallPackageList = list;
            } else if (type == 9) {
                this.networkAccessWhitelist = list;
            } else if (type == 18) {
                this.disabledDeactiveMdmPackagesList = list;
            }
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public void syncHwAdminCache(String policyName, Bundle bundle) {
        if (bundle != null) {
            synchronized (this.mLock) {
                char c = 65535;
                switch (policyName.hashCode()) {
                    case -2032892315:
                        if (policyName.equals(DISABLE_PASSIVE_PROVIDER_POLICY)) {
                            c = 27;
                            break;
                        }
                        break;
                    case -1809352329:
                        if (policyName.equals(DISABLE_GOOGLE_PLAY_STORE_POLICY)) {
                            c = 7;
                            break;
                        }
                        break;
                    case -1586399269:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_SIM_LOCK)) {
                            c = '&';
                            break;
                        }
                        break;
                    case -1462770845:
                        if (policyName.equals(DISABLE_APPLICATIONS_LIST_POLICY)) {
                            c = '\b';
                            break;
                        }
                        break;
                    case -1418767509:
                        if (policyName.equals("disable-send-notification")) {
                            c = 17;
                            break;
                        }
                        break;
                    case -1371405315:
                        if (policyName.equals(DISABLE_STATUS_BAR)) {
                            c = 1;
                            break;
                        }
                        break;
                    case -1333051633:
                        if (policyName.equals(DISABLE_SYSTEM_BROWSER_POLICY)) {
                            c = 5;
                            break;
                        }
                        break;
                    case -1032082848:
                        if (policyName.equals(DISABLE_SYNC)) {
                            c = 26;
                            break;
                        }
                        break;
                    case -1002053434:
                        if (policyName.equals(DISABLE_SDWRITING_POLICY)) {
                            c = '\t';
                            break;
                        }
                        break;
                    case -851772941:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF)) {
                            c = 20;
                            break;
                        }
                        break;
                    case -694001423:
                        if (policyName.equals("disable-clipboard")) {
                            c = 11;
                            break;
                        }
                        break;
                    case -595558097:
                        if (policyName.equals(DISABLE_MICROPHONE)) {
                            c = 14;
                            break;
                        }
                        break;
                    case -414055785:
                        if (policyName.equals("policy-single-app")) {
                            c = 18;
                            break;
                        }
                        break;
                    case -336519577:
                        if (policyName.equals(DISABLE_WIFIP2P_POLICY)) {
                            c = 28;
                            break;
                        }
                        break;
                    case -304109734:
                        if (policyName.equals(DISABLE_NAVIGATIONBAR_POLICY)) {
                            c = '\"';
                            break;
                        }
                        break;
                    case -144764070:
                        if (policyName.equals(INSTALL_APKS_BLACK_LIST_POLICY)) {
                            c = 3;
                            break;
                        }
                        break;
                    case -30728870:
                        if (policyName.equals(POLICY_NETWORK_BLACK_DOMAIN_LIST)) {
                            c = '1';
                            break;
                        }
                        break;
                    case 43563530:
                        if (policyName.equals(DISABLE_INFRARED_POLICY)) {
                            c = 29;
                            break;
                        }
                        break;
                    case 81510017:
                        if (policyName.equals(POLICY_NETWORK_WHITE_IP_LIST)) {
                            c = '.';
                            break;
                        }
                        break;
                    case 114516600:
                        if (policyName.equals("xxxxx")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 153563136:
                        if (policyName.equals("policy-file-share-disabled")) {
                            c = '#';
                            break;
                        }
                        break;
                    case 382441887:
                        if (policyName.equals("disable-volume")) {
                            c = 23;
                            break;
                        }
                        break;
                    case 458488698:
                        if (policyName.equals("disable-shutdownmenu")) {
                            c = 22;
                            break;
                        }
                        break;
                    case 476421226:
                        if (policyName.equals(DISABLE_SCREEN_CAPTURE_POLICY)) {
                            c = 4;
                            break;
                        }
                        break;
                    case 520557972:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_APPLICATION_LOCK)) {
                            c = '\'';
                            break;
                        }
                        break;
                    case 539407267:
                        if (policyName.equals("disable-power-shutdown")) {
                            c = 21;
                            break;
                        }
                        break;
                    case 579571639:
                        if (policyName.equals(POLICY_DEFAULTE_LAUNCHER)) {
                            c = '*';
                            break;
                        }
                        break;
                    case 591717814:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE)) {
                            c = 25;
                            break;
                        }
                        break;
                    case 594183088:
                        if (policyName.equals(DISABLE_NOTIFICATION_POLICY)) {
                            c = '\n';
                            break;
                        }
                        break;
                    case 702979817:
                        if (policyName.equals(DISABLE_HEADPHONE)) {
                            c = 16;
                            break;
                        }
                        break;
                    case 731752599:
                        if (policyName.equals(SUPER_WHITE_LIST_APP)) {
                            c = 15;
                            break;
                        }
                        break;
                    case 731920490:
                        if (policyName.equals("disable-change-wallpaper")) {
                            c = 19;
                            break;
                        }
                        break;
                    case 746015831:
                        if (policyName.equals(POLICY_NETWORK_BLACK_IP_LIST)) {
                            c = '0';
                            break;
                        }
                        break;
                    case 853982814:
                        if (policyName.equals("ignore-frequent-relaunch-app")) {
                            c = '\r';
                            break;
                        }
                        break;
                    case 1044365373:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 1173183091:
                        if (policyName.equals(UNAVAILABLE_SSID_LIST)) {
                            c = '-';
                            break;
                        }
                        break;
                    case 1187313158:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_PARENT_CONTROL)) {
                            c = '%';
                            break;
                        }
                        break;
                    case 1240910281:
                        if (policyName.equals(POLICY_DISABLE_MULTIWINDOW)) {
                            c = ',';
                            break;
                        }
                        break;
                    case 1297508996:
                        if (policyName.equals(POLICY_NETWORK_WHITE_DOMAIN_LIST)) {
                            c = '/';
                            break;
                        }
                        break;
                    case 1389850009:
                        if (policyName.equals("disable-google-account-autosync")) {
                            c = '\f';
                            break;
                        }
                        break;
                    case 1463869800:
                        if (policyName.equals(DISABLE_SETTINGS_POLICY)) {
                            c = 6;
                            break;
                        }
                        break;
                    case 1502491755:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_FORCE_ENCRYPT_SDCARD)) {
                            c = ')';
                            break;
                        }
                        break;
                    case 1695181060:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST)) {
                            c = '!';
                            break;
                        }
                        break;
                    case 1785346365:
                        if (policyName.equals("force-enable-wifi")) {
                            c = ' ';
                            break;
                        }
                        break;
                    case 1946452102:
                        if (policyName.equals("disable-fingerprint-authentication")) {
                            c = 30;
                            break;
                        }
                        break;
                    case 1947338901:
                        if (policyName.equals(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION)) {
                            c = '(';
                            break;
                        }
                        break;
                    case 1981742202:
                        if (policyName.equals("force-enable-BT")) {
                            c = 31;
                            break;
                        }
                        break;
                    case 2066934651:
                        if (policyName.equals(DISABLE_SCREEN_TURN_OFF)) {
                            c = '+';
                            break;
                        }
                        break;
                    case 2074833572:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_PHONE_FIND)) {
                            c = '$';
                            break;
                        }
                        break;
                    case 2076917186:
                        if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE)) {
                            c = 24;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.exampleValue = bundle;
                        break;
                    case 1:
                        this.mDisableStatusBarBundle = bundle;
                        break;
                    case 2:
                        this.mSettingsPolicyNetworkLocationStatus = bundle;
                        break;
                    case 3:
                        this.installPackageBlacklist = bundle.getStringArrayList("value");
                        break;
                    case 4:
                        this.isDisableScreenCapture = bundle.getBoolean("value");
                        break;
                    case 5:
                        this.isDisableSystemBrowser = bundle.getBoolean("value");
                        break;
                    case 6:
                        this.isDisableSettings = bundle.getBoolean("value");
                        break;
                    case 7:
                        this.isDisableGooglePlayStore = bundle.getBoolean("value");
                        break;
                    case '\b':
                        this.disabledApplicationlist = bundle.getStringArrayList("value");
                        break;
                    case '\t':
                        this.mDisableSDCardWritingBundle = bundle;
                        break;
                    case '\n':
                        this.mDisableNotificationBundle = bundle;
                        break;
                    case 11:
                        this.isDisableClipboard = bundle.getBoolean("value", false);
                        break;
                    case '\f':
                        this.isDisableGoogleAccountAutosync = bundle.getBoolean("value", false);
                        break;
                    case '\r':
                        this.ignoreFrequentRelaunchAppList = bundle.getStringArrayList("value");
                        break;
                    case 14:
                        this.disableMicrophone = bundle;
                        break;
                    case 15:
                        this.superWhiteListApp = bundle;
                        break;
                    case 16:
                        this.disableHeadphone = bundle;
                        break;
                    case 17:
                        this.disableSendNotification = bundle;
                        break;
                    case 18:
                        this.singleApp = bundle;
                        break;
                    case 19:
                        this.disableChangeWallpaper = bundle;
                        break;
                    case 20:
                        this.disableScreenOff = bundle;
                        break;
                    case 21:
                        this.disablePowerShutdown = bundle;
                        break;
                    case 22:
                        this.disableShutdownMenu = bundle;
                        break;
                    case 23:
                        this.disableVolume = bundle;
                        break;
                    case 24:
                        this.mDisableLocationService = bundle;
                        break;
                    case 25:
                        this.mDisableLocationMode = bundle;
                        break;
                    case 26:
                        this.disableSync = bundle;
                        break;
                    case 27:
                        this.disablePassiveProvider = bundle;
                        break;
                    case 28:
                        this.isDisableWifiP2P = bundle.getBoolean("wifi_p2p_policy_item_value");
                        break;
                    case 29:
                        this.isDisableInfrared = bundle.getBoolean("infrared_item_policy_value");
                        break;
                    case 30:
                        this.disableFingerprintAuthentication = bundle;
                        break;
                    case 31:
                        this.forceEnableBt = bundle;
                        break;
                    case ' ':
                        this.forceEnableWifi = bundle;
                        break;
                    case '!':
                        this.allowAccessibilityServices = bundle;
                        break;
                    case '\"':
                        this.mDisablenNavigationBarBundle = bundle;
                        break;
                    case '#':
                        this.forceEnablefileShare = bundle;
                        break;
                    case '$':
                        this.mDisabledPhoneFind = bundle;
                        break;
                    case '%':
                        this.mDisabledParentControl = bundle;
                        break;
                    case '&':
                        this.mDisabledSimLock = bundle;
                        break;
                    case '\'':
                        this.mDisabledApplicationLock = bundle;
                        break;
                    case '(':
                        this.disabledAndroidAnimation = bundle;
                        break;
                    case ')':
                        this.mSettingsPolicyForceEncryptSdcard = bundle;
                        break;
                    case '*':
                        this.mDefaultLauncher = bundle;
                        break;
                    case '+':
                        this.isSleepByPowerButtonDisabled = bundle;
                        break;
                    case ',':
                        this.mDisabledMultiWindow = bundle;
                        break;
                    case '-':
                        this.mGetUnavailableSsidList = bundle;
                        break;
                    case '.':
                        this.mNetworkWhiteIpList = bundle;
                        break;
                    case '/':
                        this.mNetworkWhiteDomainList = bundle;
                        break;
                    case '0':
                        this.mNetworkBlackIpList = bundle;
                        break;
                    case '1':
                        this.mNetworkBlackDomainList = bundle;
                        break;
                }
            }
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public Bundle getCachedBundle(String policyName) {
        Bundle result = null;
        synchronized (this.mLock) {
            char c = 65535;
            switch (policyName.hashCode()) {
                case -2032892315:
                    if (policyName.equals(DISABLE_PASSIVE_PROVIDER_POLICY)) {
                        c = 18;
                        break;
                    }
                    break;
                case -1586399269:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_SIM_LOCK)) {
                        c = 27;
                        break;
                    }
                    break;
                case -1418767509:
                    if (policyName.equals("disable-send-notification")) {
                        c = '\b';
                        break;
                    }
                    break;
                case -1371405315:
                    if (policyName.equals(DISABLE_STATUS_BAR)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1032082848:
                    if (policyName.equals(DISABLE_SYNC)) {
                        c = 17;
                        break;
                    }
                    break;
                case -1002053434:
                    if (policyName.equals(DISABLE_SDWRITING_POLICY)) {
                        c = 4;
                        break;
                    }
                    break;
                case -851772941:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_SCREEN_OFF)) {
                        c = 11;
                        break;
                    }
                    break;
                case -595558097:
                    if (policyName.equals(DISABLE_MICROPHONE)) {
                        c = 3;
                        break;
                    }
                    break;
                case -414055785:
                    if (policyName.equals("policy-single-app")) {
                        c = '\t';
                        break;
                    }
                    break;
                case -304109734:
                    if (policyName.equals(DISABLE_NAVIGATIONBAR_POLICY)) {
                        c = 23;
                        break;
                    }
                    break;
                case -30728870:
                    if (policyName.equals(POLICY_NETWORK_BLACK_DOMAIN_LIST)) {
                        c = '&';
                        break;
                    }
                    break;
                case 3694080:
                    if (policyName.equals("xxxx")) {
                        c = 2;
                        break;
                    }
                    break;
                case 81510017:
                    if (policyName.equals(POLICY_NETWORK_WHITE_IP_LIST)) {
                        c = '#';
                        break;
                    }
                    break;
                case 153563136:
                    if (policyName.equals("policy-file-share-disabled")) {
                        c = 24;
                        break;
                    }
                    break;
                case 382441887:
                    if (policyName.equals("disable-volume")) {
                        c = 14;
                        break;
                    }
                    break;
                case 458488698:
                    if (policyName.equals("disable-shutdownmenu")) {
                        c = '\r';
                        break;
                    }
                    break;
                case 520557972:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_APPLICATION_LOCK)) {
                        c = 28;
                        break;
                    }
                    break;
                case 539407267:
                    if (policyName.equals("disable-power-shutdown")) {
                        c = '\f';
                        break;
                    }
                    break;
                case 579571639:
                    if (policyName.equals(POLICY_DEFAULTE_LAUNCHER)) {
                        c = 31;
                        break;
                    }
                    break;
                case 591717814:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_MODE)) {
                        c = 16;
                        break;
                    }
                    break;
                case 594183088:
                    if (policyName.equals(DISABLE_NOTIFICATION_POLICY)) {
                        c = 5;
                        break;
                    }
                    break;
                case 702979817:
                    if (policyName.equals(DISABLE_HEADPHONE)) {
                        c = 7;
                        break;
                    }
                    break;
                case 731752599:
                    if (policyName.equals(SUPER_WHITE_LIST_APP)) {
                        c = 6;
                        break;
                    }
                    break;
                case 731920490:
                    if (policyName.equals("disable-change-wallpaper")) {
                        c = '\n';
                        break;
                    }
                    break;
                case 746015831:
                    if (policyName.equals(POLICY_NETWORK_BLACK_IP_LIST)) {
                        c = '%';
                        break;
                    }
                    break;
                case 1044365373:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_NETWORK_LOCATION)) {
                        c = 1;
                        break;
                    }
                    break;
                case 1173183091:
                    if (policyName.equals(UNAVAILABLE_SSID_LIST)) {
                        c = '\"';
                        break;
                    }
                    break;
                case 1187313158:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_PARENT_CONTROL)) {
                        c = 26;
                        break;
                    }
                    break;
                case 1240910281:
                    if (policyName.equals(POLICY_DISABLE_MULTIWINDOW)) {
                        c = '!';
                        break;
                    }
                    break;
                case 1297508996:
                    if (policyName.equals(POLICY_NETWORK_WHITE_DOMAIN_LIST)) {
                        c = '$';
                        break;
                    }
                    break;
                case 1502491755:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORCE_ENCRYPT_SDCARD)) {
                        c = 30;
                        break;
                    }
                    break;
                case 1695181060:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_ACCESSIBILITY_SERVICES_WHITE_LIST)) {
                        c = 22;
                        break;
                    }
                    break;
                case 1785346365:
                    if (policyName.equals("force-enable-wifi")) {
                        c = 21;
                        break;
                    }
                    break;
                case 1946452102:
                    if (policyName.equals("disable-fingerprint-authentication")) {
                        c = 19;
                        break;
                    }
                    break;
                case 1947338901:
                    if (policyName.equals(SettingsMDMPlugin.DISABLED_ANDROID_ANIMATION)) {
                        c = 29;
                        break;
                    }
                    break;
                case 1981742202:
                    if (policyName.equals("force-enable-BT")) {
                        c = 20;
                        break;
                    }
                    break;
                case 2066934651:
                    if (policyName.equals(DISABLE_SCREEN_TURN_OFF)) {
                        c = ' ';
                        break;
                    }
                    break;
                case 2074833572:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_PHONE_FIND)) {
                        c = 25;
                        break;
                    }
                    break;
                case 2076917186:
                    if (policyName.equals(SettingsMDMPlugin.POLICY_FORBIDDEN_LOCATION_SERVICE)) {
                        c = 15;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    result = this.mDisableStatusBarBundle;
                    break;
                case 1:
                    result = this.mSettingsPolicyNetworkLocationStatus;
                    break;
                case 2:
                    result = this.exampleValue;
                    break;
                case 3:
                    result = this.disableMicrophone;
                    break;
                case 4:
                    result = this.mDisableSDCardWritingBundle;
                    break;
                case 5:
                    result = this.mDisableNotificationBundle;
                    break;
                case 6:
                    result = this.superWhiteListApp;
                    break;
                case 7:
                    result = this.disableHeadphone;
                    break;
                case '\b':
                    result = this.disableSendNotification;
                    break;
                case '\t':
                    result = this.singleApp;
                    break;
                case '\n':
                    result = this.disableChangeWallpaper;
                    break;
                case 11:
                    result = this.disableScreenOff;
                    break;
                case '\f':
                    result = this.disablePowerShutdown;
                    break;
                case '\r':
                    result = this.disableShutdownMenu;
                    break;
                case 14:
                    result = this.disableVolume;
                    break;
                case 15:
                    result = this.mDisableLocationService;
                    break;
                case 16:
                    result = this.mDisableLocationMode;
                    break;
                case 17:
                    result = this.disableSync;
                    break;
                case 18:
                    result = this.disablePassiveProvider;
                    break;
                case 19:
                    result = this.disableFingerprintAuthentication;
                    break;
                case 20:
                    result = this.forceEnableBt;
                    break;
                case 21:
                    result = this.forceEnableWifi;
                    break;
                case 22:
                    result = this.allowAccessibilityServices;
                    break;
                case 23:
                    result = this.mDisablenNavigationBarBundle;
                    break;
                case 24:
                    result = this.forceEnablefileShare;
                    break;
                case 25:
                    result = this.mDisabledPhoneFind;
                    break;
                case 26:
                    result = this.mDisabledParentControl;
                    break;
                case 27:
                    result = this.mDisabledSimLock;
                    break;
                case 28:
                    result = this.mDisabledApplicationLock;
                    break;
                case 29:
                    result = this.disabledAndroidAnimation;
                    break;
                case 30:
                    result = this.mSettingsPolicyForceEncryptSdcard;
                    break;
                case 31:
                    result = this.mDefaultLauncher;
                    break;
                case ' ':
                    result = this.isSleepByPowerButtonDisabled;
                    break;
                case '!':
                    result = this.mDisabledMultiWindow;
                    break;
                case '\"':
                    result = this.mGetUnavailableSsidList;
                    break;
                case '#':
                    result = this.mNetworkWhiteIpList;
                    break;
                case '$':
                    result = this.mNetworkWhiteDomainList;
                    break;
                case '%':
                    result = this.mNetworkBlackIpList;
                    break;
                case '&':
                    result = this.mNetworkBlackDomainList;
                    break;
            }
        }
        return result;
    }

    public boolean getCachedValue(int type) {
        boolean isDisable = false;
        synchronized (this.mLock) {
            if (type == 0) {
                isDisable = this.isDisableWifi;
            } else if (type == 1) {
                isDisable = this.isDisableVoice;
            } else if (type == 2) {
                isDisable = this.isDisableInstallSource;
            } else if (type == 8) {
                isDisable = this.isDisableBluetooth;
            } else if (type == 19) {
                isDisable = this.isDisableDecryptSdCard;
            } else if (type == 29) {
                isDisable = this.isDisableWifiP2P;
            } else if (type != 32) {
                switch (type) {
                    case 10:
                        isDisable = this.isDisableSafeMode;
                        break;
                    case 11:
                        isDisable = this.isDisableAdb;
                        break;
                    case 12:
                        isDisable = this.isDisableUsbOtg;
                        break;
                    case 13:
                        isDisable = this.isDisableGps;
                        break;
                    case 14:
                        isDisable = this.isDisableHome;
                        break;
                    case 15:
                        isDisable = this.isDisableTask;
                        break;
                    case 16:
                        isDisable = this.isDisableBack;
                        break;
                    case 17:
                        isDisable = this.isDisableChangeLauncher;
                        break;
                    default:
                        switch (type) {
                            case 21:
                                isDisable = this.isDisableScreenCapture;
                                break;
                            case 22:
                                isDisable = this.isDisableSystemBrowser;
                                break;
                            case 23:
                                isDisable = this.isDisableSettings;
                                break;
                            case 24:
                                isDisable = this.isDisableGooglePlayStore;
                                break;
                            case 25:
                                isDisable = this.isDisableClipboard;
                                break;
                            case 26:
                                isDisable = this.isDisableGoogleAccountAutosync;
                                break;
                        }
                }
            } else {
                isDisable = this.isDisableInfrared;
            }
        }
        return isDisable;
    }

    public List<String> getCachedList(int type) {
        List<String> result = null;
        synchronized (this.mLock) {
            if (type == 3) {
                result = this.installSourceWhitelist;
            } else if (type == 4) {
                result = this.persistentAppList;
            } else if (type == 5) {
                result = this.disallowedRunningAppList;
            } else if (type == 6) {
                result = this.installPackageWhitelist;
            } else if (type == 7) {
                result = this.disallowedUninstallPackageList;
            } else if (type == 9) {
                result = this.networkAccessWhitelist;
            } else if (type == 18) {
                result = this.disabledDeactiveMdmPackagesList;
            } else if (type == 20) {
                result = this.installPackageBlacklist;
            } else if (type == 27) {
                result = this.ignoreFrequentRelaunchAppList;
            } else if (type == 28) {
                result = this.disabledApplicationlist;
            }
        }
        return result;
    }
}

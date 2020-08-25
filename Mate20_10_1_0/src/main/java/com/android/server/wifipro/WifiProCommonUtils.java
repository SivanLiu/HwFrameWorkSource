package com.android.server.wifipro;

import android.app.ActivityManager;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.HwTelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.server.location.HwLogRecordManager;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

public class WifiProCommonUtils implements IHwWifiProCommonUtilsEx {
    public static final String BROWSER_LAUNCHED_BY_WIFI_PORTAL = "wifi_portal";
    public static final String BROWSER_LAUNCH_FROM = "launch_from";
    public static final String COUNTRY_CODE_CN = "460";
    public static final int DNS_REACHALBE = 0;
    public static final int DNS_UNREACHALBE = -1;
    public static final int ENTERPRISE_HOTSPOT_THRESHOLD = 4;
    public static final int HISTORY_ITEM_INTERNET = 1;
    public static final int HISTORY_ITEM_NO_INTERNET = 0;
    public static final int HISTORY_ITEM_PORTAL = 2;
    public static final int HISTORY_ITEM_UNCHECKED = -1;
    public static final int HISTORY_TYPE_EMPTY = 103;
    public static final int HISTORY_TYPE_HAS_INTERNET_EVER = 104;
    public static final int HISTORY_TYPE_INTERNET = 100;
    public static final int HISTORY_TYPE_PORTAL = 102;
    public static final int HTTP_MAX_REDIRECT = 399;
    public static final int HTTP_REACHALBE_GOOLE = 204;
    public static final int HTTP_REACHALBE_HOME = 200;
    public static final int HTTP_REDIRECTED = 302;
    public static final int HTTP_UNREACHALBE = 599;
    public static final String HUAWEI_SETTINGS = "com.android.settings";
    public static final String HUAWEI_SETTINGS_WLAN = "com.android.settings.Settings$WifiSettingsActivity";
    private static final String HW_WIFI_SELF_CURING = "net.wifi.selfcuring";
    public static final int ID_PORTAL_AUTH_EXPIRATION_INFO = 909009072;
    public static final int ID_UPDATE_AUTO_OPEN_WIFI_FAILED_INFO = 909002064;
    public static final int ID_UPDATE_DUAL_BAND_WIFI_INFO = 909009065;
    public static final int ID_UPDATE_PORTAL_DETECT_STAT_INFO = 909002060;
    public static final int ID_UPDATE_PORTAL_LOAD_PAGE_FAILED = 909002062;
    public static final int ID_UPDATE_PORTAL_POPUP_BROWSER_FAILED = 909002061;
    public static final int ID_UPDATE_PORTAL_POPUP_OTHER_BROWSER_STAT_INFO = -1;
    public static final int ID_WIFIPRO_DISABLE_INFO = 909002066;
    public static final int ID_WIFI_USER_CLOSE_WIFI_STAT_INFO = 909002058;
    public static final int ID_WIFI_USER_CONNECT_OTHER_WIFI_STAT_INFO = 909002059;
    private static final int INDEX_CELL_ID = 0;
    private static final int INDEX_DOMAINS = 1;
    private static final int INDEX_FLAG = 4;
    private static final int INDEX_GATEWAY = 6;
    private static final int INDEX_IP_ADDRESS = 2;
    private static final int INDEX_PREFLENGTH = 3;
    private static final int INDEX_SCOPE = 5;
    public static final String INTERNET_HISTORY_INIT = "-1/-1/-1/-1/-1/-1/-1/-1/-1/-1";
    private static final int INVALID_CELL_ID = -1;
    private static final int IPV4_ADDRESS_LEN = 4;
    public static final String KEY_MGMT_OWE = "OWE";
    public static final String KEY_MGMT_QUALCOMM_WAPI_CERT = "QUALCOMM_WAPI_CERT";
    public static final String KEY_MGMT_QUALCOMM_WAPI_PSK = "QUALCOMM_WAPI_PSK";
    public static final String KEY_MGMT_SAE = "SAE";
    public static final String KEY_MGMT_SUITE_B = "SUITE_B_192";
    public static final String KEY_MGMT_WAPI_CERT = "WAPI_CERT";
    public static final String KEY_MGMT_WAPI_PSK = "WAPI_PSK";
    public static final String KEY_PORTAL_CONFIG_KEY = "portal_config_key";
    public static final String KEY_PORTAL_DETECT_STAT_INFO = "portal_detect_stat_info";
    public static final String KEY_PORTAL_FIRST_DETECT = "portal_first_detect";
    public static final String KEY_PORTAL_HTTP_RESP_CODE = "portal_http_resp_code";
    public static final String KEY_PORTAL_REDIRECTED_URL = "portal_redirected_url";
    public static final String KEY_PROP_LOCALE = "ro.product.locale.region";
    public static final String KEY_WIFIPRO_MANUAL_CONNECT = "wifipro_manual_connect_ap";
    public static final String KEY_WIFI_PRO_SWITCH = "smart_network_switching";
    private static final String KEY_WIFI_SECURE = "wifi_cloud_security_check";
    private static final int MAX_PHONE_ID = 2;
    public static final String[] NON_OPEN_PORTALS = {"\"0000docomo\"WPA_PSK"};
    public static final int PORTAL_CONNECTED_AND_UNLOGIN = 1;
    public static final int PORTAL_DISCONNECTED_OR_LOGIN = 0;
    public static final String PORTAL_NETWORK_FLAG = "HW_WIFI_PORTAL_FLAG";
    public static final int PORTAL_USER_SELECT_FROM_NOTIFICATION = 2;
    public static final long RECHECK_DELAYED_MS = 3600000;
    public static final float RECOVERY_PERCENTAGE = 0.8f;
    public static final int RESP_CODE_ABNORMAL_SERVER = 604;
    public static final int RESP_CODE_CONN_RESET = 606;
    public static final int RESP_CODE_GATEWAY = 602;
    public static final int RESP_CODE_INVALID_URL = 603;
    public static final int RESP_CODE_REDIRECTED_HOST_CHANGED = 605;
    public static final int RESP_CODE_TIMEOUT = 600;
    public static final int RESP_CODE_UNSTABLE = 601;
    public static final int SCE_STATE_IDLE = 0;
    public static final int SCE_STATE_REASSOC = 101;
    public static final int SCE_STATE_RECONNECT = 103;
    public static final int SCE_STATE_RESET = 102;
    private static final String TAG = "WifiProCommonUtils";
    public static final int WIFI_2G_BAND_SCORE = 20;
    public static final int WIFI_5G_BAND_SCORE = 50;
    private static final String WIFI_BACKGROUND_CONN_TAG = "wifipro_recommending_access_points";
    public static final int WIFI_CATEGORY_WIFI5_SCORE = 1;
    public static final int WIFI_CATEGORY_WIFI6 = 2;
    public static final int WIFI_CATEGORY_WIFI6_160M_SCORE = 3;
    public static final int WIFI_CATEGORY_WIFI6_NONE_160M_SCORE = 2;
    public static final int WIFI_CATEGORY_WIFI6_PLUS = 3;
    public static final int WIFI_CATEGORY_WIFI6_PLUS_160M_SCORE = 5;
    public static final int WIFI_CATEGORY_WIFI6_PLUS_NONE_160M_SCORE = 4;
    public static final int WIFI_DEFAULT_SCORE = -1;
    public static final int WIFI_LEVEL_FOUR = 4;
    public static final int WIFI_LEVEL_FOUR_SCORE = 80;
    public static final int WIFI_LEVEL_THREE = 3;
    public static final int WIFI_LEVEL_THREE_SCORE = 60;
    public static final int WIFI_LEVEL_TWO = 2;
    public static final int WIFI_LEVEL_TWO_SCORE = 20;
    public static final int WIFI_PRO_SOFT_RECONNECT = 104;
    private static final Object mBackgroundLock = new Object();
    private static WifiProCommonUtils mWifiProCommonUtils = null;

    public boolean hwIsWifiProSwitchOn(Context context) {
        return isWifiProSwitchOn(context);
    }

    public boolean hwIsAllowWifiConfigRecovery(String internetHistory) {
        return allowWifiConfigRecovery(internetHistory);
    }

    public static synchronized WifiProCommonUtils getDefault() {
        WifiProCommonUtils wifiProCommonUtils;
        synchronized (WifiProCommonUtils.class) {
            if (mWifiProCommonUtils == null) {
                mWifiProCommonUtils = new WifiProCommonUtils();
            }
            wifiProCommonUtils = mWifiProCommonUtils;
        }
        return wifiProCommonUtils;
    }

    public static boolean isWifiProSwitchOn(Context context) {
        return context != null && Settings.System.getInt(context.getContentResolver(), "smart_network_switching", 0) == 1;
    }

    public static boolean isWifiProPropertyEnabled(Context context) {
        return context != null && "true".equalsIgnoreCase(Settings.Global.getString(context.getContentResolver(), "hw_wifipro_enable"));
    }

    public static boolean isWifiProLitePropertyEnabled(Context context) {
        return context != null && "lite".equalsIgnoreCase(Settings.Global.getString(context.getContentResolver(), "hw_wifipro_enable"));
    }

    public static WifiConfiguration getCurrentWifiConfig(WifiManager wifiManager) {
        if (wifiManager == null) {
            return null;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<WifiConfiguration> configNetworks = wifiManager.getConfiguredNetworks();
        if (configNetworks == null || wifiInfo == null || !SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
            return null;
        }
        for (int i = 0; i < configNetworks.size(); i++) {
            WifiConfiguration config = configNetworks.get(i);
            if (config.networkId == wifiInfo.getNetworkId() && config.networkId != -1) {
                return config;
            }
        }
        return null;
    }

    public static String getCurrentSsid(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null || !SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
            return null;
        }
        return wifiInfo.getSSID();
    }

    public static String getCurrentBssid(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null || !SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
            return null;
        }
        return wifiInfo.getBSSID();
    }

    public static int getCurrentRssi(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null || !SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
            return -127;
        }
        return wifiInfo.getRssi();
    }

    public static int getBssidCounter(WifiConfiguration config, List<ScanResult> scanResults) {
        if (config == null || scanResults == null) {
            return 0;
        }
        String currentSsid = config.SSID;
        String configKey = config.configKey();
        if (TextUtils.isEmpty(currentSsid) || TextUtils.isEmpty(configKey)) {
            return 0;
        }
        int counter = 0;
        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult nextResult = scanResults.get(i);
            String scanSsid = "\"" + nextResult.SSID + "\"";
            String capabilities = nextResult.capabilities;
            if (currentSsid.equals(scanSsid) && isSameEncryptType(capabilities, configKey)) {
                counter++;
            }
        }
        return counter;
    }

    public static boolean isWifiConnected(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null || wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
            return false;
        }
        return true;
    }

    public static boolean isWifi5GConnected(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null || !wifiInfo.is5GHz()) {
            return false;
        }
        return true;
    }

    public static boolean isWifiConnectedOrConnecting(WifiManager wifiManager) {
        WifiInfo wifiInfo;
        if (wifiManager == null || (wifiInfo = wifiManager.getConnectionInfo()) == null) {
            return false;
        }
        return SupplicantState.isConnecting(wifiInfo.getSupplicantState());
    }

    public static boolean isWifiConnectedActive(Context context) {
        NetworkInfo ni;
        if (context == null || (ni = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo()) == null || !ni.isConnected() || ni.getType() != 1) {
            return false;
        }
        return true;
    }

    public static boolean isWpaOrWpa2(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        int authType = config.allowedKeyManagement.cardinality() > 1 ? -1 : config.getAuthType();
        if (authType == 1 || authType == 4) {
            return true;
        }
        return false;
    }

    public static boolean isSae(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if ((config.allowedKeyManagement.cardinality() > 1 ? -1 : config.getAuthType()) == 8) {
            return true;
        }
        return false;
    }

    public static boolean isWapi(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        switch (config.allowedKeyManagement.cardinality() > 1 ? -1 : config.getAuthType()) {
            case 16:
            case 17:
            case 18:
            case 19:
                return true;
            default:
                return false;
        }
    }

    public static boolean isEncrypted(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        if (isWpaOrWpa2(config) || isWapi(config) || isSae(config)) {
            return true;
        }
        return false;
    }

    public static boolean isEncryptedAuthType(int authType) {
        if (!(authType == 1 || authType == 4 || authType == 6 || authType == 8 || authType == 11)) {
            switch (authType) {
                case 16:
                case 17:
                case 18:
                case 19:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public static boolean isQueryActivityMatched(Context context, String activityName) {
        List<ActivityManager.RunningTaskInfo> runningTaskInfos;
        ComponentName cn;
        if (context == null || activityName == null || (runningTaskInfos = ((ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningTasks(1)) == null || runningTaskInfos.isEmpty() || (cn = runningTaskInfos.get(0).topActivity) == null || cn.getClassName() == null || !cn.getClassName().startsWith(activityName)) {
            return false;
        }
        return true;
    }

    public static boolean isManualConnecting(Context context) {
        if (context == null || Settings.System.getInt(context.getContentResolver(), KEY_WIFIPRO_MANUAL_CONNECT, 0) != 1) {
            return false;
        }
        return true;
    }

    public static boolean isInMonitorList(String input, String[] list) {
        if (input == null || list == null) {
            return false;
        }
        for (String str : list) {
            if (input.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMobileDataOff(Context context) {
        if (context == null || Settings.Global.getInt(context.getContentResolver(), "mobile_data", 1) != 0) {
            return false;
        }
        return true;
    }

    public static boolean isMobileDataInactive(Context context) {
        NetworkInfo ni;
        if (context == null || (ni = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo()) == null || !ni.isConnected() || ni.getType() != 0) {
            return true;
        }
        return false;
    }

    public static boolean isCalling(Context context) {
        if (context == null) {
            return false;
        }
        int callState = ((TelephonyManager) context.getSystemService("phone")).getCallState();
        if (2 == callState || 1 == callState) {
            return true;
        }
        return false;
    }

    public static boolean isNoSIMCard(Context context) {
        if (context == null || ((TelephonyManager) context.getSystemService("phone")).getSimState() != 1) {
            return false;
        }
        return true;
    }

    public static boolean useOperatorOverSea() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        if (operator == null || operator.length() <= 0 || operator.startsWith(COUNTRY_CODE_CN)) {
            return false;
        }
        return true;
    }

    public static boolean allowRecheckForNoInternet(WifiConfiguration config, ScanResult scanResult, Context context) {
        boolean allowed = false;
        synchronized (mBackgroundLock) {
            if (config != null) {
                if (config.noInternetAccess && !allowWifiConfigRecovery(config.internetHistory) && config.internetRecoveryStatus == 3 && !isQueryActivityMatched(context, HUAWEI_SETTINGS_WLAN) && scanResult != null && ((scanResult.is24GHz() && scanResult.level >= -75) || (scanResult.is5GHz() && scanResult.level >= -72))) {
                    allowed = true;
                }
            }
        }
        return allowed;
    }

    public static void setBackgroundConnTag(Context context, boolean background) {
        synchronized (mBackgroundLock) {
            if (context != null) {
                Settings.Secure.putInt(context.getContentResolver(), WIFI_BACKGROUND_CONN_TAG, background ? 1 : 0);
            }
        }
    }

    public static String getProductLocale() {
        return SystemProperties.get(KEY_PROP_LOCALE, "");
    }

    public static void setWifiSelfCureStatus(int state) {
        try {
            SystemProperties.set(HW_WIFI_SELF_CURING, String.valueOf(state));
        } catch (RuntimeException e) {
            HwHiLog.e(TAG, false, "SystemProperties set RuntimeException.", new Object[0]);
        }
    }

    public static boolean isWifiSelfCuring() {
        return !String.valueOf(0).equals(SystemProperties.get(HW_WIFI_SELF_CURING, String.valueOf(0)));
    }

    public static int getSelfCuringState() {
        try {
            return Integer.parseInt(SystemProperties.get(HW_WIFI_SELF_CURING, String.valueOf(0)));
        } catch (NumberFormatException e) {
            HwHiLog.e(TAG, false, "getSelfCuringState failed", new Object[0]);
            return 0;
        }
    }

    public static boolean isWifiSecDetectOn(Context context) {
        if (context == null || Settings.Global.getInt(context.getContentResolver(), KEY_WIFI_SECURE, 0) != 1) {
            return false;
        }
        return true;
    }

    public static boolean matchedRequestByHistory(String internetHistory, int type) {
        int i;
        boolean matched = false;
        if (internetHistory == null || internetHistory.lastIndexOf("/") == -1) {
            return false;
        }
        String[] temp = internetHistory.split("/");
        int[] items = new int[temp.length];
        int numHasInet = 0;
        int numPortal = 0;
        int numTarget = 0;
        int i2 = 0;
        int numNoInet = 0;
        int numChecked = 0;
        while (i2 < temp.length) {
            try {
                items[i2] = Integer.parseInt(temp[i2]);
                if (items[i2] != -1) {
                    numChecked++;
                }
                if (items[i2] == 0) {
                    numNoInet++;
                } else if (items[i2] == 1) {
                    numHasInet++;
                } else if (items[i2] == 2) {
                    numPortal++;
                }
                i2++;
            } catch (NumberFormatException e) {
                HwHiLog.e(TAG, false, "matchedRequestByHistory broken network history: parse internetHistory failed", new Object[0]);
            }
        }
        int itemValue = -1;
        if (type == 100) {
            itemValue = 1;
            numTarget = numHasInet;
            i = 1;
        } else if (type == 102) {
            if (numPortal >= 1) {
                return true;
            }
            return false;
        } else if (type == 103) {
            if (numChecked == 0) {
                return true;
            }
            return false;
        } else if (type != 104) {
            i = 1;
        } else if (numHasInet >= 1) {
            return true;
        } else {
            return false;
        }
        if (numChecked >= i && items[0] == itemValue) {
            matched = true;
        }
        if (!matched && numChecked == 2 && (items[0] == itemValue || items[1] == itemValue)) {
            matched = true;
        }
        if (matched || numChecked < 3 || ((float) numTarget) / ((float) numChecked) < 0.8f) {
            return matched;
        }
        return true;
    }

    public static boolean isOpenType(WifiConfiguration config) {
        return config != null && config.allowedKeyManagement.cardinality() <= 1 && config.getAuthType() == 0;
    }

    public static boolean isOpenAndPortal(WifiConfiguration config) {
        return isOpenType(config) && config.portalNetwork;
    }

    public static boolean isOpenAndMaybePortal(WifiConfiguration config) {
        return isOpenType(config) && !config.noInternetAccess && matchedRequestByHistory(config.internetHistory, 103);
    }

    public static String parseHostByUrlLocation(String requestUrl) {
        int end;
        if (requestUrl != null) {
            int start = 0;
            if (requestUrl.startsWith("http://")) {
                start = 7;
            } else if (requestUrl.startsWith("https://")) {
                start = 8;
            }
            int end2 = requestUrl.indexOf("/", start);
            if (end2 == -1 || "/".length() + end2 > requestUrl.length()) {
                end = requestUrl.indexOf("?", start);
            } else {
                int tmpEnd = requestUrl.substring(start, end2).indexOf("?", 0);
                end = tmpEnd != -1 ? tmpEnd + start : end2;
            }
            if (end != -1 && 0 <= end && "/".length() + end <= requestUrl.length()) {
                return requestUrl.substring(0, end);
            }
        }
        return requestUrl;
    }

    public static boolean invalidUrlLocation(String location) {
        if (location == null) {
            return true;
        }
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return false;
        }
        return true;
    }

    public static String dhcpResults2String(DhcpResults dhcpResults, int cellid) {
        if (dhcpResults == null || dhcpResults.ipAddress == null || dhcpResults.ipAddress.getAddress() == null || dhcpResults.dnsServers == null) {
            return null;
        }
        StringBuilder lastDhcpResults = new StringBuilder();
        lastDhcpResults.append(String.valueOf(cellid) + "|");
        StringBuilder sb = new StringBuilder();
        String str = "";
        sb.append(dhcpResults.domains == null ? str : dhcpResults.domains);
        sb.append("|");
        lastDhcpResults.append(sb.toString());
        lastDhcpResults.append(dhcpResults.ipAddress.getAddress().getHostAddress() + "|");
        lastDhcpResults.append(dhcpResults.ipAddress.getPrefixLength() + "|");
        lastDhcpResults.append(dhcpResults.ipAddress.getFlags() + "|");
        lastDhcpResults.append(dhcpResults.ipAddress.getScope() + "|");
        StringBuilder sb2 = new StringBuilder();
        if (dhcpResults.gateway != null) {
            str = dhcpResults.gateway.getHostAddress();
        }
        sb2.append(str);
        sb2.append("|");
        lastDhcpResults.append(sb2.toString());
        Iterator it = dhcpResults.dnsServers.iterator();
        while (it.hasNext()) {
            lastDhcpResults.append(((InetAddress) it.next()).getHostAddress() + "|");
        }
        return lastDhcpResults.toString();
    }

    private static InetAddress ipStrToInetAddress(String ipAddress) {
        byte[] ipAddrAarrys = new byte[4];
        String[] ipStrAarrys = ipAddress.split("\\.");
        if (ipStrAarrys.length != 4) {
            HwHiLog.e(TAG, false, "invalid IPv4 address length", new Object[0]);
            return null;
        }
        int i = 0;
        while (i < 4) {
            try {
                ipAddrAarrys[i] = (byte) Integer.parseInt(ipStrAarrys[i]);
                i++;
            } catch (NumberFormatException | UnknownHostException e) {
                HwHiLog.e(TAG, false, "Exception happens", new Object[0]);
                return null;
            }
        }
        return InetAddress.getByAddress(ipAddrAarrys);
    }

    public static StaticIpConfiguration dhcpResults2StaticIpConfig(String lastDhcpResults) {
        if (lastDhcpResults == null || lastDhcpResults.length() <= 0) {
            return null;
        }
        StaticIpConfiguration staticIpConfig = new StaticIpConfiguration();
        String[] dhcpResults = lastDhcpResults.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE);
        InetAddress ipAddr = null;
        int prefLength = -1;
        int flag = -1;
        int scope = -1;
        int i = 0;
        while (i < dhcpResults.length) {
            try {
                if (i != 0) {
                    if (i == 1) {
                        staticIpConfig.domains = dhcpResults[i];
                    } else if (i == 2) {
                        ipAddr = ipStrToInetAddress(dhcpResults[i]);
                    } else if (i == 3) {
                        prefLength = Integer.parseInt(dhcpResults[i]);
                    } else if (i == 4) {
                        flag = Integer.parseInt(dhcpResults[i]);
                    } else if (i == 5) {
                        scope = Integer.parseInt(dhcpResults[i]);
                    } else if (i == 6) {
                        staticIpConfig.gateway = ipStrToInetAddress(dhcpResults[i]);
                    } else {
                        InetAddress dnsServer = ipStrToInetAddress(dhcpResults[i]);
                        if (dnsServer != null) {
                            staticIpConfig.dnsServers.add(dnsServer);
                        }
                    }
                }
                i++;
            } catch (IllegalArgumentException e) {
                HwHiLog.w(TAG, false, "Exception happens", new Object[0]);
            }
        }
        if (!(ipAddr == null || prefLength == -1 || staticIpConfig.gateway == null || staticIpConfig.dnsServers.size() <= 0)) {
            staticIpConfig.ipAddress = new LinkAddress(ipAddr, prefLength, flag, scope);
            return staticIpConfig;
        }
        return null;
    }

    public static String dhcpResults2Gateway(String lastDhcpResults) {
        if (lastDhcpResults == null || lastDhcpResults.length() <= 0) {
            return null;
        }
        try {
            String[] dhcpResults = lastDhcpResults.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE);
            if (dhcpResults.length >= 7) {
                return InetAddress.getByName(dhcpResults[6]).toString();
            }
            return null;
        } catch (UnknownHostException e) {
            HwHiLog.e(TAG, false, "dhcpResults2Gateway UnknownHostException failed", new Object[0]);
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00c7  */
    public static boolean isAllowWifiSwitch(List<ScanResult> scanResults, List<WifiConfiguration> configNetworks, String currBssid, String currSsid, String currConfigKey, int rssiRequired) {
        boolean sameConfigKey;
        if (scanResults == null) {
            return false;
        }
        if (scanResults.size() == 0) {
            return false;
        }
        if (configNetworks == null) {
            return false;
        }
        if (configNetworks.size() == 0) {
            return false;
        }
        for (int i = 0; i < scanResults.size(); i++) {
            ScanResult nextResult = scanResults.get(i);
            String scanSsid = "\"" + nextResult.SSID + "\"";
            String scanResultEncrypt = nextResult.capabilities;
            boolean sameBssid = currBssid != null && currBssid.equals(nextResult.BSSID);
            if (currSsid != null && currSsid.equals(scanSsid)) {
                if (isSameEncryptType(scanResultEncrypt, currConfigKey)) {
                    sameConfigKey = true;
                    if (sameBssid) {
                        if (!sameConfigKey) {
                            if (nextResult.level < rssiRequired) {
                                continue;
                            } else {
                                for (int j = 0; j < configNetworks.size(); j++) {
                                    WifiConfiguration nextConfig = configNetworks.get(j);
                                    int disableReason = nextConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                                    if ((!nextConfig.noInternetAccess || allowWifiConfigRecovery(nextConfig.internetHistory)) && disableReason <= 0 && !isOpenAndPortal(nextConfig) && !isOpenAndMaybePortal(nextConfig) && nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid) && isSameEncryptType(scanResultEncrypt, nextConfig.configKey())) {
                                        return true;
                                    }
                                }
                                continue;
                            }
                        }
                    }
                }
            }
            sameConfigKey = false;
            if (sameBssid) {
            }
        }
        return false;
    }

    public static boolean unreachableRespCodeByAndroid(int code) {
        return code != 204 && !isRedirectedRespCodeByGoogle(code);
    }

    public static boolean unreachableRespCode(int code) {
        return code == 599 || code == 600;
    }

    public static boolean httpUnreachableOrAbnormal(int code) {
        return code >= 599;
    }

    public static boolean isRedirectedRespCode(int respCode) {
        return respCode == 301 || respCode == 302 || respCode == 303 || respCode == 307;
    }

    public static boolean isRedirectedRespCodeByGoogle(int respCode) {
        if (respCode == 204 || respCode < 200 || respCode > 399) {
            return false;
        }
        return true;
    }

    public static boolean httpReachableOrRedirected(int code) {
        return code >= 200 && code <= 399;
    }

    public static boolean httpReachableHome(int code) {
        return code == 200;
    }

    public static int getReachableCode(boolean googleServer) {
        return googleServer ? 204 : 200;
    }

    public static boolean isEncryptionWep(String encryption) {
        return encryption.contains("WEP");
    }

    public static boolean isEncryptionPsk(String encryption) {
        return encryption.contains("PSK");
    }

    public static boolean isEncryptionEap(String encryption) {
        return encryption.contains("EAP");
    }

    public static boolean isEncryptionOwe(String encryption) {
        if (encryption == null) {
            return false;
        }
        return encryption.contains(KEY_MGMT_OWE);
    }

    public static boolean isEncryptionSae(String encryption) {
        if (encryption == null) {
            return false;
        }
        return encryption.contains(KEY_MGMT_SAE);
    }

    public static boolean isEncryptionWapi(String encryption) {
        if (encryption == null) {
            return false;
        }
        if (encryption.contains(KEY_MGMT_WAPI_PSK) || encryption.contains(KEY_MGMT_WAPI_CERT) || encryption.contains(KEY_MGMT_QUALCOMM_WAPI_PSK) || encryption.contains(KEY_MGMT_QUALCOMM_WAPI_CERT)) {
            return true;
        }
        return false;
    }

    public static boolean isNormalOpenNetwork(String encryption) {
        if (encryption != null && !isEncryptionWep(encryption) && !isEncryptionPsk(encryption) && !isEncryptionEap(encryption) && !isEncryptionOwe(encryption) && !isEncryptionSae(encryption) && !isEncryptionWapi(encryption)) {
            return true;
        }
        return false;
    }

    public static boolean isOpenNetwork(String encryption) {
        if (encryption == null) {
            return false;
        }
        if (isNormalOpenNetwork(encryption) || isEncryptionOwe(encryption)) {
            return true;
        }
        return false;
    }

    public static boolean isSameEncryptType(String encryptTypeA, String encryptTypeB) {
        if (encryptTypeA == null || encryptTypeB == null) {
            return false;
        }
        if (isEncryptionWep(encryptTypeA) && isEncryptionWep(encryptTypeB)) {
            return true;
        }
        if (isEncryptionSae(encryptTypeA) && isEncryptionSae(encryptTypeB)) {
            return true;
        }
        if (isEncryptionPsk(encryptTypeA) && isEncryptionPsk(encryptTypeB)) {
            return true;
        }
        if (isEncryptionEap(encryptTypeA) && isEncryptionEap(encryptTypeB)) {
            return true;
        }
        if (isEncryptionWapi(encryptTypeA) && isEncryptionWapi(encryptTypeB)) {
            return true;
        }
        if (isEncryptionOwe(encryptTypeA) && isEncryptionOwe(encryptTypeB)) {
            return true;
        }
        if (!isNormalOpenNetwork(encryptTypeA) || !isNormalOpenNetwork(encryptTypeB)) {
            return false;
        }
        return true;
    }

    public static boolean isLandscapeMode(Context context) {
        if (context == null || context.getResources() == null || context.getResources().getConfiguration() == null || context.getResources().getConfiguration().orientation != 2) {
            return false;
        }
        return true;
    }

    public static int getForegroundAppUid(Context context) {
        List<ActivityManager.RunningAppProcessInfo> lr;
        if (context == null || (lr = ((ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG)).getRunningAppProcesses()) == null) {
            return -1;
        }
        for (ActivityManager.RunningAppProcessInfo ra : lr) {
            if (ra.importance == 100) {
                return ra.uid;
            }
        }
        return -1;
    }

    public static String getPackageName(Context context, int uid) {
        if (uid == -1 || context == null) {
            return "total";
        }
        String name = context.getPackageManager().getNameForUid(uid);
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        return "unknown:" + uid;
    }

    public static String insertWifiConfigHistory(String internetHistory, int status) {
        if (internetHistory == null || internetHistory.lastIndexOf("/") == -1) {
            return INTERNET_HISTORY_INIT;
        }
        String newInternetHistory = String.valueOf(status) + "/" + internetHistory.substring(0, internetHistory.lastIndexOf("/"));
        HwHiLog.d(TAG, false, "insertWifiConfigHistory, newInternetHistory = %{public}s", new Object[]{newInternetHistory});
        return newInternetHistory;
    }

    public static String updateWifiConfigHistory(String internetHistory, int status) {
        if (internetHistory == null || internetHistory.lastIndexOf("/") == -1) {
            return INTERNET_HISTORY_INIT;
        }
        String newInternetHistory = String.valueOf(status) + "/" + internetHistory.substring(internetHistory.indexOf("/") + 1);
        HwHiLog.d(TAG, false, "updateWifiConfigHistory, newInternetHistory = %{public}s", new Object[]{newInternetHistory});
        return newInternetHistory;
    }

    public static boolean allowWifiConfigRecovery(String internetHistory) {
        boolean allowRecovery = false;
        if (internetHistory == null || internetHistory.lastIndexOf("/") == -1) {
            HwHiLog.w(TAG, false, "allowWifiConfigRecovery, inputed arg is invalid, internetHistory = null", new Object[0]);
            return false;
        }
        String[] temp = internetHistory.split("/");
        int[] items = new int[temp.length];
        int numChecked = 0;
        int numNoInet = 0;
        int numHasInet = 0;
        int i = 0;
        while (i < temp.length) {
            try {
                items[i] = Integer.parseInt(temp[i]);
                if (items[i] != -1) {
                    numChecked++;
                }
                if (items[i] == 0) {
                    numNoInet++;
                } else if (items[i] == 1) {
                    numHasInet++;
                }
                i++;
            } catch (NumberFormatException e) {
                HwHiLog.w(TAG, false, "Broken network history: parse internetHistory failed.", new Object[0]);
            }
        }
        if (numChecked >= 2) {
            if (items[0] != 1 && items[1] != 1) {
                return false;
            }
            allowRecovery = true;
            int i2 = 1;
            while (true) {
                if (i2 >= numChecked) {
                    break;
                } else if (items[i2] != 1) {
                    allowRecovery = false;
                    break;
                } else {
                    i2++;
                }
            }
        }
        if (!allowRecovery && numChecked >= 3 && items[1] == 1 && items[2] == 1) {
            allowRecovery = true;
        }
        if (allowRecovery || numChecked < 3 || ((float) numHasInet) / ((float) numChecked) < 0.8f) {
            return allowRecovery;
        }
        return true;
    }

    public static boolean isNetworkReachableByICMP(String ipAddress, int timeout) {
        try {
            return Inet4Address.getByName(ipAddress).isReachable(timeout);
        } catch (IOException e) {
            HwHiLog.w(TAG, false, "Exception, Network is not Reachable by ICMP!", new Object[0]);
            return false;
        }
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm;
        if (context == null || (cm = (ConnectivityManager) context.getSystemService("connectivity")) == null || cm.isNetworkSupported(0)) {
            return false;
        }
        return true;
    }

    public static int getCurrenSignalLevel(WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            return HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi());
        }
        return 0;
    }

    public static int getSignalLevel(int frequency, int rssi) {
        return HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(frequency, rssi);
    }

    public static int getCurrentCellId() {
        int phoneId;
        CellLocation cellLocation;
        int subId = HwTelephonyManager.getDefault().getPreferredDataSubscription();
        if (subId < 0 || (phoneId = SubscriptionManager.getPhoneId(subId)) < 0 || phoneId >= 2 || (cellLocation = HwTelephonyManager.getDefault().getCellLocation(phoneId)) == null) {
            return -1;
        }
        if (cellLocation instanceof CdmaCellLocation) {
            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
            int cellId = cdmaCellLocation.getBaseStationId();
            if (cellId < 0) {
                return cdmaCellLocation.getCid();
            }
            return cellId;
        } else if (cellLocation instanceof GsmCellLocation) {
            return ((GsmCellLocation) cellLocation).getCid();
        } else {
            return -1;
        }
    }

    public static boolean getAirplaneModeOn(Context mContext) {
        return Settings.Global.getInt(mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    public static int calculateScore(ScanResult scanResult) {
        int levelScore;
        int categoryScore;
        if (scanResult == null) {
            HwHiLog.d(TAG, false, "scanResult is null", new Object[0]);
            return -1;
        }
        int signalLevel = getSignalLevel(scanResult.frequency, scanResult.level);
        if (signalLevel == 2) {
            levelScore = 20;
        } else if (signalLevel == 3) {
            levelScore = 60;
        } else if (signalLevel != 4) {
            return -1;
        } else {
            levelScore = 80;
        }
        int bandScore = ScanResult.is5GHz(scanResult.frequency) ? 50 : 20;
        int i = scanResult.supportedWifiCategory;
        if (i != 2) {
            if (i != 3) {
                categoryScore = 1;
            } else if (scanResult.channelWidth == 3) {
                categoryScore = 5;
            } else {
                categoryScore = 4;
            }
        } else if (scanResult.channelWidth == 3) {
            categoryScore = 3;
        } else {
            categoryScore = 2;
        }
        return levelScore + bandScore + categoryScore;
    }
}

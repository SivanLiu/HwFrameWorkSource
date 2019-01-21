package huawei.android.net.hwmplink;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.huawei.hsm.permission.StubController;

public class HwHiDataCommonUtils {
    public static final int APP_WIFI_NO_SLEEP = 0;
    public static final int APP_WIFI_SLEEP_UNKNOWN = -1;
    private static final String ARBITRATION_BCM_PERMISSION = "huawei.permission.SMART_NOTIFY_FAULT";
    private static final String ARBITRATION_NOTIFY_HICURE_ACTION = "huawei.intent.action.HI_DATA_CHECK";
    public static final String ARBITRATION_NOTIFY_HICURE_RESULT_ACTION = "huawei.intent.action.HICURE_RESULT";
    public static final String BASE_TAG = "HiData_";
    public static final int BLOCK_LEVEL_EXTREMELY_BAD = 1;
    public static final int BLOCK_LEVEL_SLIGHTLY_BAD = 0;
    private static final String BroadcastKey = "com.android.server.hidata.arbitration.HwArbitrationStateMachine";
    private static final String BroadcastNetworkKey = "MPLinkSuccessNetWorkKey";
    private static final String BroadcastUIDKey = "MPLinkSuccessUIDKey";
    public static final int CELL_NETWORK = 0;
    public static final int DEFAULT_HICURE_OVERTIME = 30;
    public static final int DEFAULT_HICURE_RESULT = 3;
    private static final String EXTRA_APPUID = "extra_uid";
    private static final String EXTRA_BLOCKING_TYPE = "extra_blocking_type";
    private static final String EXTRA_CELL_LINK_STATUS = "extra_cell_link_status";
    private static final String EXTRA_CELL_SWITCH_STATUS = "extra_cell_switch_status";
    private static final String EXTRA_CURRENT_NETWORK = "extra_current_network";
    public static final String EXTRA_HICURE_DIAGNOSE_RESULT = "extra_diagnose_result";
    public static final String EXTRA_HICURE_METHOD = "extra_method";
    public static final String EXTRA_HICURE_OVERTIME = "extra_timer_result";
    public static final String EXTRA_HICURE_RESULT = "extra_result";
    private static final String EXTRA_WIFI_LINK_STATUS = "extra_wifi_link_status";
    private static final String EXTRA_WIFI_SWITCH_STATUS = "extra_wifi_switch_status";
    private static final String HICURE_CLASS_NAME_USBLIQUID = "com.huawei.hwdetectrepair.smartnotify.eventlistener.HiDataCheckReceiver";
    private static final String HICURE_PACKAGE_NAME_USBLIQUID = "com.huawei.hwdetectrepair";
    public static final boolean HIDATA_DEBUG = SystemProperties.getBoolean("ro.config.mplink_log", false);
    public static final int NONE_NETWORK = -1;
    private static final String TAG;
    public static final int WHITE_LIST_TYPE_WIFI_SLEEP = 7;
    public static final int WIFI_NETWORK = 1;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HiData_");
        stringBuilder.append(HwHiDataCommonUtils.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static void startHiCure(Context context, int appUID, int blockingType, int currentNetwork, boolean wifiSwitchStatus, boolean wifiLinkStatus, boolean cellSwitchStatus, boolean cellLinkStatus) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyHiCure appUID = ");
        stringBuilder.append(appUID);
        stringBuilder.append(", blockingType = ");
        stringBuilder.append(blockingType);
        stringBuilder.append(", currentNetwork = ");
        stringBuilder.append(currentNetwork);
        stringBuilder.append(", wifiSwtichStatus = ");
        stringBuilder.append(wifiSwitchStatus);
        stringBuilder.append(", wifiLinkStatus = ");
        stringBuilder.append(wifiLinkStatus);
        stringBuilder.append(", cellSwitchStatus = ");
        stringBuilder.append(cellSwitchStatus);
        stringBuilder.append(", cellLinkStatus = ");
        stringBuilder.append(cellLinkStatus);
        logD(str, stringBuilder.toString());
        Intent intent = new Intent(ARBITRATION_NOTIFY_HICURE_ACTION);
        intent.setFlags(StubController.RMD_PERMISSION_CODE);
        intent.setClassName(HICURE_PACKAGE_NAME_USBLIQUID, HICURE_CLASS_NAME_USBLIQUID);
        intent.putExtra(EXTRA_APPUID, appUID);
        intent.putExtra(EXTRA_BLOCKING_TYPE, blockingType);
        intent.putExtra(EXTRA_CURRENT_NETWORK, currentNetwork);
        intent.putExtra(EXTRA_WIFI_SWITCH_STATUS, wifiSwitchStatus);
        intent.putExtra(EXTRA_WIFI_LINK_STATUS, wifiLinkStatus);
        intent.putExtra(EXTRA_CELL_SWITCH_STATUS, cellSwitchStatus);
        intent.putExtra(EXTRA_CELL_LINK_STATUS, cellLinkStatus);
        context.sendBroadcastAsUser(intent, UserHandle.ALL, ARBITRATION_BCM_PERMISSION);
    }

    public static void logD(String tag, String log) {
        Log.d(tag, log);
    }

    public static void logI(String tag, String log) {
        if (HIDATA_DEBUG) {
            Log.i(tag, log);
        }
    }

    public static String getCurrentSsid(WifiManager wifiManager) {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
                return wifiInfo.getSSID();
            }
        }
        return null;
    }

    public static String getCurrentBssid(WifiManager wifiManager) {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
                return wifiInfo.getBSSID();
            }
        }
        return null;
    }

    public static int getCurrentRssi(WifiManager wifiManager) {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && SupplicantState.isConnecting(wifiInfo.getSupplicantState())) {
                return wifiInfo.getRssi();
            }
        }
        return -127;
    }

    public static boolean isWifiConnected(WifiManager wifiManager) {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWifi5GConnected(WifiManager wifiManager) {
        boolean z = false;
        if (wifiManager == null) {
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.is5GHz()) {
            z = true;
        }
        return z;
    }

    public static boolean isWifiConnectedOrConnecting(WifiManager wifiManager) {
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                return SupplicantState.isConnecting(wifiInfo.getSupplicantState());
            }
        }
        return false;
    }

    public static boolean isWpaOrWpa2(WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return false;
        }
        int authType = config.allowedKeyManagement.cardinality() > 1 ? -1 : config.getAuthType();
        if (authType == 1 || authType == 4) {
            z = true;
        }
        return z;
    }
}

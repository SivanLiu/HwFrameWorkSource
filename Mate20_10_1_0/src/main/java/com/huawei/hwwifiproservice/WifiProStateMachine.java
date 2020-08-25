package com.huawei.hwwifiproservice;

import android.app.ActivityManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.booster.IHwCommBoosterCallback;
import android.net.booster.IHwCommBoosterServiceManager;
import android.net.wifi.HwInnerNetworkManagerImpl;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.wifi.HwHiLog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.HwWifiCHRHilink;
import com.android.server.wifi.MSS.HwMSSUtils;
import com.android.server.wifi.grs.GrsApiManager;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.hwcoex.HiCoexUtils;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class WifiProStateMachine extends StateMachine implements INetworkQosCallBack, INetworksHandoverCallBack, IWifiProUICallBack, IDualBandManagerCallback {
    private static final int ACCESS_SERVER_LETENCY_ONE_SECOND = 1000;
    private static final int ACCESS_TYPE = 1;
    private static final String ACCESS_WEB_RECORD_PORTAL = "isPortalAP";
    private static final String ACCESS_WEB_RECORD_REASON = "reason";
    private static final int ACCESS_WEB_RECORD_REASON_INTERNET = 0;
    private static final String ACCESS_WEB_RECORD_SUCC = "succ";
    private static final boolean AUTO_EVALUATE_SWITCH = false;
    private static final int BAD_RSSI_LEVEL = 2;
    private static final int BASE = 136168;
    private static final boolean BQE_TEST = false;
    private static final int CELL_2RECOVERY_WIFI = 6;
    private static final int CELL_2STRONG_WIFI = 7;
    private static final int CHECK_SMART_CARD_STATE_INTERVEL = 10000;
    private static final int CHR_AVAILIABLE_AP_COUNTER = 2;
    private static final int CHR_CHECK_WIFI_HANDOVER_TIMEOUT = 20000;
    private static final int CHR_DUALBAND_LEVEL1 = 0;
    private static final int CHR_DUALBAND_LEVEL2 = 1;
    private static final int CHR_DUALBAND_LEVEL3 = 2;
    private static final int CHR_DUALBAND_LEVEL4 = 3;
    private static final int CHR_DUALBAND_ONLINE_THRESHOLD_TIME = 300000;
    private static final int CHR_ID_DUAL_BAND_EXCEPTION = 909002085;
    private static final int CHR_ID_HANDOVER_EXCEPTION_NETWORK_QUALITY = 909002083;
    private static final int CHR_ID_WIFI_HANDOVER_TYPE = 909009129;
    private static final int CHR_ID_WIFI_HANDOVER_UNEXPECTED_TYPE = 909009131;
    private static final int CHR_USER_CLOSE_WIFI = 0;
    private static final int CHR_USER_CONNECT_BACK = 2;
    private static final int CHR_USER_FORGET_NETWORK = 3;
    private static final int CHR_USER_SWITCH_BACK = 1;
    private static final int CHR_WIFI_HANDOVER_COUNT = 1;
    private static final int CMD_UPDATE_WIFIPRO_CONFIGURATIONS = 131672;
    private static final String COUNTRY_CODE_CN = "460";
    private static final int CSP_INVISIBILITY = 0;
    private static final int CSP_VISIBILITY = 1;
    private static final boolean DBG = true;
    private static final boolean DDBG = false;
    private static final boolean DEFAULT_WIFI_PRO_ENABLED = false;
    private static final int DELAYED_TIME_LAUNCH_BROWSER = 500;
    private static final int DELAY_EVALUTE_NEXT_AP_TIME = 2000;
    private static final int DELAY_PERIODIC_PORTAL_CHECK_TIME_FAST = 2000;
    private static final int DELAY_PERIODIC_PORTAL_CHECK_TIME_SLOW = 600000;
    private static final int DELAY_START_WIFI_EVALUTE_TIME = 6000;
    private static final int DELAY_TIME_BASE = 2;
    private static final long DELAY_UPLOAD_MS = 120000;
    private static final int DELTA_RSSI = 10;
    private static final int DETECT_SERVER_FAILURE_THREE_TIMES = 3;
    private static final int DETECT_SERVER_FAILURE_TWO_TIMES = 2;
    private static final int DETECT_SERVER_PING_PANG_TIME = 60000;
    private static final int DETECT_SERVER_TIME_INTERVAL_MAX = 60000;
    private static final int DETECT_SERVER_TIME_INTERVAL_MID = 30000;
    private static final int DETECT_SERVER_TIME_INTERVAL_MIN = 15000;
    private static final int DOWNLINK_LIMIT = SystemProperties.getInt("ro.config.network_limit_speed", (int) LIMIT_DEFAULT_VALUE);
    private static final int[] DUALBAND_DURATION_INTERVAL = {2000, HwMSSUtils.MSS_SYNC_AFT_CONNECTED, HiCoexUtils.TIMEOUT_CONNECT};
    private static final int[] DUALBAND_SCAN_COUNT_INTERVAL = {1, 3, 10};
    private static final int[] DUALBAND_TARGET_AP_COUNT_INTERVAL = {1, 5, 10};
    private static final int DURATION_FROM_10_TO_15S = 2;
    private static final int DURATION_FROM_7_TO_10S = 1;
    private static final int DURATION_IN_7S = 0;
    private static final int DURATION_OUT_15S = 3;
    private static final int EVALUATE_ALL_TIMEOUT = 75000;
    private static final int EVALUATE_VALIDITY_TIMEOUT = 120000;
    private static final int EVALUATE_WIFI_CONNECTED_TIMEOUT = 35000;
    private static final int EVALUATE_WIFI_RTT_BQE_INTERVAL = 3000;
    private static final int EVENT_BQE_ANALYZE_NETWORK_QUALITY = 136317;
    private static final int EVENT_CALL_STATE_CHANGED = 136201;
    private static final int EVENT_CHECK_AVAILABLE_AP_RESULT = 136176;
    private static final int EVENT_CHECK_MOBILE_QOS_RESULT = 136180;
    private static final int EVENT_CHECK_PORTAL_AUTH_CHECK_RESULT = 136208;
    private static final int EVENT_CHECK_WIFI_INTERNET = 136192;
    private static final int EVENT_CHECK_WIFI_INTERNET_RESULT = 136181;
    private static final int EVENT_CHECK_WIFI_NETWORK_BACKGROUND = 136211;
    private static final int EVENT_CHECK_WIFI_NETWORK_LATENCY = 136212;
    private static final int EVENT_CHECK_WIFI_NETWORK_RESULT = 136213;
    private static final int EVENT_CHECK_WIFI_NETWORK_STATUS = 136210;
    private static final int EVENT_CHECK_WIFI_NETWORK_STATUS_TIMEOUT = 5000;
    private static final int EVENT_CHR_ALARM_EXPIRED = 136321;
    private static final int EVENT_CHR_CHECK_WIFI_HANDOVER = 136214;
    private static final int EVENT_CONFIGURATION_CHANGED = 136197;
    private static final int EVENT_CONFIGURED_NETWORKS_CHANGED = 136308;
    private static final String EVENT_DATA = "eventData";
    private static final int EVENT_DELAY_EVALUTE_NEXT_AP = 136314;
    private static final int EVENT_DELAY_REINITIALIZE_WIFI_MONITOR = 136184;
    private static final int EVENT_DEVICE_SCREEN_OFF = 136206;
    private static final int EVENT_DEVICE_SCREEN_ON = 136170;
    private static final int EVENT_DEVICE_USER_PRESENT = 136207;
    private static final int EVENT_DIALOG_CANCEL = 136183;
    private static final int EVENT_DIALOG_OK = 136182;
    private static final int EVENT_DUALBAND_5GAP_AVAILABLE = 136370;
    private static final int EVENT_DUALBAND_DELAY_RETRY = 136372;
    private static final int EVENT_DUALBAND_NETWROK_TYPE = 136316;
    private static final int EVENT_DUALBAND_RSSITH_RESULT = 136368;
    private static final int EVENT_DUALBAND_SCORE_RESULT = 136369;
    private static final String EVENT_DUALBAND_SWITCH_FINISHED = "DualBandSwitchFinished";
    private static final int EVENT_DUALBAND_WIFI_HANDOVER_RESULT = 136371;
    private static final int EVENT_EMUI_CSP_SETTINGS_CHANGE = 136190;
    private static final int EVENT_EVALUATE_COMPLETE = 136295;
    private static final int EVENT_EVALUATE_INITIATE = 136294;
    private static final int EVENT_EVALUATE_START_CHECK_INTERNET = 136307;
    private static final int EVENT_EVALUATE_VALIDITY_TIMEOUT = 136296;
    private static final int EVENT_EVALUTE_ABANDON = 136305;
    private static final int EVENT_EVALUTE_STOP = 136303;
    private static final int EVENT_EVALUTE_TIMEOUT = 136304;
    private static final int EVENT_GET_WIFI_TCPRX = 136311;
    private static final int EVENT_HTTP_REACHABLE_RESULT = 136195;
    private static final String EVENT_ID = "eventId";
    private static final int EVENT_LAA_STATUS_CHANGED = 136200;
    private static final int EVENT_LAST_EVALUTE_VALID = 136302;
    private static final int EVENT_LAUNCH_BROWSER = 136320;
    private static final int EVENT_LOAD_CONFIG_INTERNET_INFO = 136315;
    private static final int EVENT_MOBILE_CONNECTIVITY = 136175;
    private static final int EVENT_MOBILE_DATA_STATE_CHANGED_ACTION = 136186;
    private static final int EVENT_MOBILE_QOS_CHANGE = 136173;
    private static final int EVENT_MOBILE_RECOVERY_TO_WIFI = 136189;
    private static final int EVENT_MOBILE_SWITCH_DELAY = 136194;
    private static final int EVENT_NETWORK_CONNECTIVITY_CHANGE = 136177;
    private static final int EVENT_NETWORK_USER_CONNECT = 136202;
    private static final int EVENT_NOTIFY_WIFI_LINK_POOR = 136198;
    private static final int EVENT_PERIODIC_PORTAL_CHECK_FAST = 136205;
    private static final int EVENT_PERIODIC_PORTAL_CHECK_SLOW = 136204;
    private static final int EVENT_PORTAL_SELECTED = 136319;
    private static final int EVENT_PROCESS_GRS = 136374;
    private static final int EVENT_RECHECK_SMART_CARD_STATE = 136209;
    private static final int EVENT_REQUEST_SCAN_DELAY = 136196;
    private static final int EVENT_RETRY_WIFI_TO_WIFI = 136191;
    private static final int EVENT_SCAN_RESULTS_AVAILABLE = 136293;
    private static final String EVENT_SSID_SWITCH_FINISHED = "SsidSwitchFinished";
    private static final int EVENT_START_BQE = 136306;
    private static final int EVENT_SUPPLICANT_STATE_CHANGE = 136297;
    private static final int EVENT_TRY_WIFI_ROVE_OUT = 136199;
    private static final int EVENT_USER_ROVE_IN = 136193;
    private static final int EVENT_WIFIPRO_EVALUTE_STATE_CHANGE = 136298;
    private static final int EVENT_WIFIPRO_WORKING_STATE_CHANGE = 136171;
    private static final int EVENT_WIFI_CHECK_UNKOWN = 136309;
    private static final int EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED = 136203;
    private static final int EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT = 136301;
    private static final int EVENT_WIFI_EVALUTE_TCPRTT_RESULT = 136299;
    private static final int EVENT_WIFI_FIRST_CONNECTED = 136373;
    private static final int EVENT_WIFI_GOOD_INTERVAL_TIMEOUT = 136187;
    private static final int EVENT_WIFI_HANDOVER_WIFI_RESULT = 136178;
    private static final int EVENT_WIFI_NETWORK_QOS_DETECT = 501;
    private static final int EVENT_WIFI_NETWORK_STATE_CHANGE = 136169;
    private static final int EVENT_WIFI_NO_INTERNET_NOTIFICATION = 136318;
    private static final int EVENT_WIFI_P2P_CONNECTION_CHANGED = 136310;
    private static final int EVENT_WIFI_QOS_CHANGE = 136172;
    private static final int EVENT_WIFI_RECOVERY_TIMEOUT = 136188;
    private static final int EVENT_WIFI_RSSI_CHANGE = 136179;
    private static final int EVENT_WIFI_SECURITY_QUERY_TIMEOUT = 136313;
    private static final int EVENT_WIFI_SECURITY_RESPONSE = 136312;
    private static final int EVENT_WIFI_SEMIAUTO_EVALUTE_CHANGE = 136300;
    private static final int EVENT_WIFI_STATE_CHANGED_ACTION = 136185;
    private static final int FULLSCREEN = 5;
    private static final int GOOD_LINK_DETECTED = 131874;
    private static final int GRS_DELAY_TIME = 500;
    public static final int HANDOVER_5G_DIFFERENCE_SCORE = 5;
    private static final int HANDOVER_5G_DIRECTLY_RSSI = -70;
    public static final int HANDOVER_5G_DIRECTLY_SCORE = 40;
    public static final int HANDOVER_5G_MAX_RSSI = -45;
    public static final int HANDOVER_5G_SINGLE_RSSI = -65;
    private static final String HANDOVER_BETTER_CNT = "betterCnt";
    private static final String HANDOVER_CNT = "count";
    private static final int HANDOVER_MIN_LEVEL_INTERVAL = 2;
    private static final int HANDOVER_NO_REASON = -1;
    private static final String HANDOVER_OK_CNT = "okCnt";
    private static final String HANDOVER_SUCC_CNT = "succCnt";
    private static final String HANDOVER_TYPE = "type";
    private static final int HAND_OVER_PINGPONG = 2;
    private static final int HARD_SWITCH_TYPE = 1;
    private static final int HILINK_NO_CONFIG = 2;
    private static final String HWSYNC_DEVICE_CONNECTED_KEY = "huaweishare_device_connected";
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final int ID_WIFI_DUALBAND_DURATION_INFO = 909009137;
    private static final int ID_WIFI_DUALBAND_FAIL_REASON_INFO = 909009135;
    private static final int ID_WIFI_DUALBAND_SCAN_INFO = 909009136;
    private static final int ID_WIFI_DUALBAND_TARGET_AP_INFO = 909009138;
    private static final int ID_WIFI_DUALBAND_TRIGGER_INFO = 909009134;
    private static final int ID_WIFI_HANDOVER_FAIL_INFO = 909009130;
    private static final int ID_WIFI_HANDOVER_REASON_INFO = 909009132;
    private static final String ILLEGAL_BSSID_01 = "any";
    private static final String ILLEGAL_BSSID_02 = "00:00:00:00:00:00";
    private static final String IMS_SERVICE_STATE_BROADCAST_PERMISSION = "com.huawei.ims.permission.GET_IMS_SERVICE_STATE";
    private static final String IMS_SERVICE_STATE_CHANGED = "huawei.intent.action.IMS_SERVICE_STATE_CHANGED";
    private static final String IMS_STATE_REGISTERED = "REGISTERED";
    private static final String IMS_STATE_UNREGISTERED = "UNREGISTERED";
    private static final int INVALID = -1;
    private static final int INVALID_CHR_RCD_TIME = 0;
    private static final int INVALID_LINK_DETECTED = 131875;
    private static final int INVALID_PID = -1;
    private static final int ISCALLING = 6;
    private static final int ISLANDSCAPEMODE = 11;
    private static final int JUDGE_WIFI_FAST_REOPEN_TIME = 30000;
    private static final String KEY_EMUI_WIFI_TO_PDP = "wifi_to_pdp";
    private static final int KEY_MOBILE_HANDOVER_WIFI = 2;
    private static final String KEY_SMART_DUAL_CARD_STATE = "persist.sys.smartDualCardState";
    public static final String KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY = "wifipro_manual_connect_ap_configkey";
    private static final String KEY_WIFIPRO_RECOMMEND_NETWORK = "wifipro_auto_recommend";
    private static final String KEY_WIFIPRO_RECOMMEND_NETWORK_SAVED_STATE = "wifipro_auto_recommend_saved_state";
    private static final int KEY_WIFI_HANDOVER_MOBILE = 1;
    private static final int LAST_CONNECTED_NETWORK_EXPIRATION_AGE_MILLIS = 10000;
    private static final int LAST_WIFIPRO_DISABLE_EXPIRATION_AGE_MILLIS = 7200000;
    private static final int LIMIT_DEFAULT_VALUE = 3174;
    private static final int LINKSPEED_RX_TH_2G = 27;
    private static final int LINKSPEED_RX_TH_5G = 40;
    private static final int LINKSPEED_TX_TH_2G = 13;
    private static final int LINKSPEED_TX_TH_5G = 27;
    private static final int MAIN_MODEM_ID = 0;
    private static final int MANUAL_WLANPRO_CLOSE = 10;
    private static final String MAPS_LOCATION_FLAG = "hw_higeo_maps_location";
    private static final int MILLISECONDS_OF_ONE_SECOND = 1000;
    private static final int MOBILE = 0;
    private static final int MOBILE_DATA_INACTIVE = 12;
    private static final int MOBILE_DATA_OFF_SWITCH_DELAY_MS = 3000;
    private static float MOBILE_TRAFFIC_USED_RATE_THRESHOLD = 0.8f;
    private static final int NETWORK_CONNECTIVITY_CHANGE = 1;
    private static final int NETWORK_POOR_2CELL = 1;
    private static final int NETWORK_POOR_2WIFI = 3;
    private static final int NETWORK_POOR_LEVEL_THRESHOLD = 2;
    private static final int[] NORMAL_DURATION_INTERVAL = {7000, HiCoexUtils.TIMEOUT_CONNECT, 15000};
    /* access modifiers changed from: private */
    public static final int[] NORMAL_SCAN_INTERVAL = {15000, 15000, 30000};
    /* access modifiers changed from: private */
    public static final int[] NORMAL_SCAN_MAX_COUNTER = {4, 4, 2};
    private static final int NOT_CONNECT_TO_NETWORK = -1;
    private static final int NO_CANDIDATE_AP = 7;
    private static final int NO_INTERNET_2CELL = 0;
    private static final int NO_INTERNET_2WIFI = 2;
    private static final String NO_INTERNET_TO_CELL_EVENT = "noInterToCellEvent";
    private static final int OOBE_COMPLETE = 1;
    private static final int OTA_CHANNEL_LOAD_INVALID = -1;
    private static final int OTA_CHANNEL_LOAD_MAYBE_BAD_2G = 500;
    private static final int OTA_CHANNEL_LOAD_MAYBE_BAD_5G = 500;
    private static final int PING_PONG_TIME_INTERVAL_MAX = 300000;
    private static final int PING_PONG_TIME_INTERVAL_MIN = 60000;
    private static final int POOR_LINK_DETECTED = 131873;
    private static final int POOR_LINK_RSSI_THRESHOLD = -75;
    private static final int PORTAL_HANDOVER_DELAY_TIME = 15000;
    private static final String PORTAL_STATUS_BAR_TAG = "wifipro_portal_expired_status_bar";
    private static final int QOS_LEVEL = 2;
    private static final int QOS_SCORE = 3;
    /* access modifiers changed from: private */
    public static final int[] QUICK_SCAN_INTERVAL = {HiCoexUtils.TIMEOUT_CONNECT, HiCoexUtils.TIMEOUT_CONNECT, 15000};
    /* access modifiers changed from: private */
    public static final int[] QUICK_SCAN_MAX_COUNTER = {20, 20, 10};
    private static final int REASON_SIGNAL_LEVEL_3_TOP_UID_BAD = 207;
    private static final int REASON_SIGNAL_LEVEL_4_TOP_UID_BAD = 208;
    private static final String RESP_CODE_INTERNET_AVAILABLE = "204";
    private static final String RESP_CODE_INTERNET_UNREACHABLE = "599";
    private static final String RESP_CODE_PORTAL = "302";
    private static final int ROAM_SCENE = 1;
    private static final int ROAM_SWITCH_TYPE = 2;
    private static final int SCORE_DIFFERENCE_EXCEEDS_FIVE = 2;
    private static final int SCORE_OVER_FORTY_REASON = 1;
    private static final int SCREEN_OFF = 9;
    private static final int SELF_CURE_RSSI_THRESHOLD = -70;
    private static final int SEND_MESSAGE_DELAY_TIME = 30000;
    private static final String SETTING_SECURE_CONN_WIFI_PID = "wifipro_connect_wifi_app_pid";
    private static final String SETTING_SECURE_VPN_WORK_VALUE = "wifipro_network_vpn_state";
    private static final String SETTING_SECURE_WIFI_NO_INT = "wifi_no_internet_access";
    private static final int SIGNAL0_TOP_UID_BAD = 6;
    private static final int SIGNAL0_WEAK_DISCONNECT = 4;
    private static final int SIGNAL1_TOP_UID_BAD = 0;
    private static final int SIGNAL1_WEAK_TO_WIFI = 5;
    private static final int SIGNAL2_TOP_UID_BAD = 1;
    private static final int SIGNAL3_TOP_UID_BAD = 2;
    private static final int SIGNAL4_TOP_UID_BAD = 3;
    private static final int SIGNAL_LEVEL_3 = 3;
    private static final int SIM_SLOT_FIRST = 0;
    private static final int SIM_SLOT_INVALID = -1;
    private static final int SIM_SLOT_SECOND = 1;
    private static final int SK_UDP_TX_ERROR = 2;
    private static final String SLOW_INTERNET_TO_CELL_EVENT = "slowInterToCellEvent";
    private static final int STOP_BY_HIDATA = 1;
    private static final int SYSTEM_UID = 1000;
    private static final String SYS_OPER_CMCC = "ro.config.operators";
    private static final String SYS_PROPERT_PDP = "hw_RemindWifiToPdp";
    private static final String TAG = "WiFi_PRO_WifiProStateMachine";
    private static final int TCP_IP = 101;
    private static final int TCP_RTT_MAYBE_BAD = 700;
    private static final int TCP_RX_THRESHOLD_SCREEN_OFF = 5;
    private static final int TCP_RX_THRESHOLD_SCREEN_ON = 3;
    private static final int THE_SAME_AP_REASON = 3;
    private static final int THRESHOD_RSSI = -82;
    private static final int THRESHOD_RSSI_HIGH = -76;
    private static final int THRESHOD_RSSI_LOW = -88;
    private static final int TURN_OFF_MOBILE = 0;
    private static final int TURN_OFF_WIFI = 1;
    private static final int TURN_OFF_WIFI_PRO = 2;
    private static final int TURN_ON_WIFI_PRO = 3;
    private static final int TYPE_NETWORK_SPEED_LIMIT = 3;
    private static final int TYPE_USER_PREFERENCE = 1;
    private static final int UPGRADE_RSSI_THRESH = 10;
    private static final int USER_SWITCH = 3;
    private static final int USER_UNEXPECTED_WIFI_HANDOVER_TIME = 30000;
    private static final int VALUE_WIFI_TO_PDP_ALWAYS_SHOW_DIALOG = 0;
    private static final int VALUE_WIFI_TO_PDP_AUTO_HANDOVER_MOBILE = 1;
    private static final int VALUE_WIFI_TO_PDP_CANNOT_HANDOVER_MOBILE = 2;
    private static final String VEHICLE_STATE_FLAG = "hw_higeo_vehicle_state";
    private static final int VPN_IS_USING = 0;
    private static final int W2WFAIL_2WIFI = 8;
    private static final int W2W_FAILED_TO_WIFI = 8;
    private static final int WEAK_SIGNAL_2WIFI = 4;
    private static final int WIFI = 1;
    private static final int WIFI_CHECK_DELAY_TIME = 30000;
    private static final int WIFI_CHECK_UNKNOW_TIMER = 1;
    private static final String WIFI_CSP_DISPALY_STATE = "wifi_csp_dispaly_state";
    private static final int WIFI_DUALBAND_RECORD_MAX_TIME = 180000;
    private static final String WIFI_EVALUATE_TAG = "wifipro_recommending_access_points";
    private static final int WIFI_GOOD_LINK_MAX_TIME_LIMIT = 1800000;
    private static final String[] WIFI_HANDOVER_5G_AP_NUM_LEVEL_TYPES = {"0-1", "1-5", "5-10", "10"};
    private static final String[] WIFI_HANDOVER_5G_DURA_LEVEL_TYPES = {"0-2s", "2-5s", "5-10s", "10s"};
    private static final String[] WIFI_HANDOVER_5G_SCAN_LEVEL_TYPES = {"0-1s", "1-3s", "3-10s", "10s"};
    private static final String[] WIFI_HANDOVER_CAUSE_TYPES = {"SIGNAL1_TOP_UID_BAD", "SIGNAL2_TOP_UID_BAD", "SIGNAL3_TOP_UID_BAD", "SIGNAL4_TOP_UID_BAD", "SIGNAL0_WEAK_DISCONNECT", "SIGNAL1_WEAK_TO_WIFI", "SIGNAL0_TOP_UID_BAD"};
    private static final String[] WIFI_HANDOVER_DURAS = {"0-7s", "7-10s", "10-15s", "15s"};
    private static final String[] WIFI_HANDOVER_FAIL_TYPES = {"VPN_IS_USING", "STOP_BY_HIDATA", "HILINK_NO_CONFIG", "USER_SWITCH", "WIFI_REPEATER_MODE", "FULLSCREEN", "ISCALLING", "NO_CANDIDATE_AP", "W2W_FAILED_TO_WIFI", "SCREEN_OFF", "MANUAL_WLANPRO_CLOSE", "ISLANDSCAPEMODE", "MOBILE_DATA_INACTIVE"};
    private static final int WIFI_HANDOVER_MOBILE_TIMER_LIMIT = 4;
    private static final int WIFI_HANDOVER_TIMERS = 2;
    /* access modifiers changed from: private */
    public static final String[] WIFI_HANDOVER_TYPES = {"NOINTERNET2CELL", "NETWORKPOOR2CELL", "NOINTERNET2WIFI", "NETWORKPOOR2WIFI", "WEAKSIGNAL2WIFI", "WIFI_ROAM", "CELL2RECOVERYWIFI", "CELL2STRONGWIFI", "W2WIFIFAIL2WIFI"};
    /* access modifiers changed from: private */
    public static final String[] WIFI_HANDOVER_UNEXPECTED_TYPES = {"USER_CLOSE_WIFI", "USER_SWITCH_BACK", "USER_CONNECT_BACK", "USER_FORGET_NETWORK", "WIFI_PINGPONG_SWITCH", "CHOOSE_NO_SWITCH"};
    private static final int WIFI_NO_INTERNET_DIVISOR = 4;
    private static final int WIFI_NO_INTERNET_MAX = 12;
    private static final int WIFI_REPEATER_MODE = 4;
    private static final int WIFI_REPEATER_OPEN = 1;
    private static final int WIFI_REPEATER_OPEN_GO_WITHOUT_THTHER = 6;
    private static final int WIFI_ROAM = 5;
    private static final int WIFI_SCAN_COUNT = 4;
    private static final int WIFI_SCAN_INTERVAL_MAX = 12;
    private static final int WIFI_SIGNAL_TWO_LEVEL = 2;
    private static final int WIFI_SWITCH_NEW_ALGORITHM_TIMER_LIMIT = 2;
    private static final String WIFI_SWITCH_REASON = "switchReason";
    private static final long WIFI_SWITCH_RECORD_MAX_TIME = 1209600000;
    private static final String WIFI_SWITCH_TIME_LEVEL = "level";
    private static final int WIFI_TCPRX_STATISTICS_INTERVAL = 5000;
    private static final String WIFI_TO_CELL_CNT = "wifiToCellCnt";
    private static final String WIFI_TO_CELL_SUCC_CNT = "wifiToCellSuccCnt";
    private static final int WIFI_TO_WIFI_THRESHOLD = 3;
    private static final int WIFI_VERYY_INTERVAL_TIME = 30000;
    /* access modifiers changed from: private */
    public static boolean mIsWifiManualEvaluating = false;
    /* access modifiers changed from: private */
    public static boolean mIsWifiSemiAutoEvaluating = false;
    private static WifiProStateMachine mWifiProStateMachine;
    /* access modifiers changed from: private */
    public long connectStartTime = 0;
    /* access modifiers changed from: private */
    public int detectionNumSlow = 0;
    /* access modifiers changed from: private */
    public boolean hasHandledNoInternetResult = false;
    /* access modifiers changed from: private */
    public boolean[] imsRegisteredState = {false, false};
    /* access modifiers changed from: private */
    public boolean isDialogUpWhenConnected;
    /* access modifiers changed from: private */
    public boolean isMapNavigating;
    /* access modifiers changed from: private */
    public boolean isPeriodicDet = false;
    private boolean isVariableInited;
    /* access modifiers changed from: private */
    public boolean isVehicleState;
    /* access modifiers changed from: private */
    public boolean isVerifyWifiNoInternetTimeOut = false;
    private List<String> mAppWhitelists;
    /* access modifiers changed from: private */
    public int mAvailable5GAPAuthType = 0;
    /* access modifiers changed from: private */
    public String mAvailable5GAPBssid = null;
    /* access modifiers changed from: private */
    public String mAvailable5GAPSsid = null;
    /* access modifiers changed from: private */
    public String mBadBssid;
    /* access modifiers changed from: private */
    public String mBadSsid;
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public long mChrDualbandConnectedStartTime = 0;
    /* access modifiers changed from: private */
    public int mChrQosLevelBeforeHandover;
    /* access modifiers changed from: private */
    public long mChrRoveOutStartTime = 0;
    /* access modifiers changed from: private */
    public long mChrWifiDidableStartTime = 0;
    /* access modifiers changed from: private */
    public long mChrWifiDisconnectStartTime = 0;
    /* access modifiers changed from: private */
    public String mChrWifiHandoverType = "";
    private boolean mCloseBySystemui = false;
    /* access modifiers changed from: private */
    public int mConnectWiFiAppPid;
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public WifiInfo mCurrWifiInfo;
    /* access modifiers changed from: private */
    public String mCurrentBssid;
    /* access modifiers changed from: private */
    public int mCurrentRssi;
    /* access modifiers changed from: private */
    public String mCurrentSsid;
    /* access modifiers changed from: private */
    public long mCurrentTime;
    /* access modifiers changed from: private */
    public int mCurrentVerfyCounter;
    /* access modifiers changed from: private */
    public WifiConfiguration mCurrentWifiConfig;
    /* access modifiers changed from: private */
    public int mCurrentWifiLevel;
    private DefaultState mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public boolean mDelayedRssiChangedByCalling = false;
    /* access modifiers changed from: private */
    public boolean mDelayedRssiChangedByFullScreen = false;
    /* access modifiers changed from: private */
    public String mDualBandConnectApBssid = null;
    /* access modifiers changed from: private */
    public long mDualBandConnectTime;
    private ArrayList<WifiProEstimateApInfo> mDualBandEstimateApList = new ArrayList<>();
    /* access modifiers changed from: private */
    public int mDualBandEstimateInfoSize = 0;
    /* access modifiers changed from: private */
    public HwDualBandManager mDualBandManager;
    /* access modifiers changed from: private */
    public ArrayList<HwDualBandMonitorInfo> mDualBandMonitorApList = new ArrayList<>();
    /* access modifiers changed from: private */
    public int mDualBandMonitorInfoSize = 0;
    /* access modifiers changed from: private */
    public boolean mDualBandMonitorStart = false;
    /* access modifiers changed from: private */
    public int mDuanBandHandoverType = 0;
    /* access modifiers changed from: private */
    public volatile int mEmuiPdpSwichValue;
    private BroadcastReceiver mHMDBroadcastReceiver;
    private IntentFilter mHMDIntentFilter;
    /* access modifiers changed from: private */
    public int mHandoverFailReason = -1;
    /* access modifiers changed from: private */
    public int mHandoverReason = -1;
    /* access modifiers changed from: private */
    public boolean mHiLinkUnconfig = false;
    private IHwCommBoosterCallback mHwCommBoosterCallback = new IHwCommBoosterCallback.Stub() {
        /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass12 */

        public void callBack(int type, Bundle bundle) {
            HwHiLog.e(WifiProStateMachine.TAG, false, "receive booster callback type " + type, new Object[0]);
            if (bundle == null) {
                HwHiLog.e(WifiProStateMachine.TAG, false, "data is null", new Object[0]);
            } else if (type != 3) {
                HwHiLog.e(WifiProStateMachine.TAG, false, "unexpected event type = " + type, new Object[0]);
            } else {
                WifiProStateMachine.this.handleNetworkSpeedLimit(bundle);
            }
        }
    };
    /* access modifiers changed from: private */
    public HwDualBandBlackListManager mHwDualBandBlackListMgr;
    /* access modifiers changed from: private */
    public HwIntelligenceWiFiManager mHwIntelligenceWiFiManager;
    private BroadcastReceiver mImsStateChangedReceiver = new BroadcastReceiver() {
        /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(WifiProStateMachine.TAG, "intent is null!");
            } else if (!WifiProStateMachine.IMS_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
                Log.e(WifiProStateMachine.TAG, "no need to process IMS_SERVICE_STATE_CHANGED message");
            } else {
                int slotId = intent.getIntExtra("slot", -1);
                boolean isVoWifi = intent.getBooleanExtra("vowifi_state", true);
                String regState = intent.getStringExtra("state");
                if ((slotId == 0 || slotId == 1) && !isVoWifi && (WifiProStateMachine.IMS_STATE_REGISTERED.equals(regState) || WifiProStateMachine.IMS_STATE_UNREGISTERED.equals(regState))) {
                    Log.i(WifiProStateMachine.TAG, "slotId = " + slotId + ", isVoWifi = " + isVoWifi + ", regState = " + regState);
                    WifiProStateMachine.this.imsRegisteredState[slotId] = WifiProStateMachine.IMS_STATE_REGISTERED.equals(regState);
                    return;
                }
                Log.e(WifiProStateMachine.TAG, "invalid slotId or isVoWifi or regState, slotId = " + slotId + ", isVoWifi = " + isVoWifi + ", regState = " + regState);
            }
        }
    };
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public boolean mIsAllowEvaluate;
    /* access modifiers changed from: private */
    public boolean mIsChrQosBetterAfterDualbandHandover = false;
    private boolean mIsLimitedSpeed = false;
    /* access modifiers changed from: private */
    public boolean mIsMobileDataEnabled;
    /* access modifiers changed from: private */
    public boolean mIsNetworkAuthen;
    /* access modifiers changed from: private */
    public boolean mIsP2PConnectedOrConnecting;
    /* access modifiers changed from: private */
    public boolean mIsPortalAp;
    /* access modifiers changed from: private */
    public boolean mIsPrimaryUser;
    /* access modifiers changed from: private */
    public boolean mIsRoveOutToDisconn = false;
    /* access modifiers changed from: private */
    public boolean mIsScanedRssiLow;
    /* access modifiers changed from: private */
    public boolean mIsScanedRssiMiddle;
    /* access modifiers changed from: private */
    public boolean mIsUserHandoverWiFi;
    /* access modifiers changed from: private */
    public boolean mIsUserManualConnectSuccess = false;
    /* access modifiers changed from: private */
    public volatile boolean mIsVpnWorking;
    /* access modifiers changed from: private */
    public boolean mIsWiFiInternetCHRFlag;
    /* access modifiers changed from: private */
    public boolean mIsWiFiNoInternet;
    /* access modifiers changed from: private */
    public boolean mIsWiFiProAutoEvaluateAP;
    /* access modifiers changed from: private */
    public boolean mIsWiFiProEnabled;
    /* access modifiers changed from: private */
    public boolean mIsWifi2CellInStrongSignalEnabled = false;
    private boolean mIsWifiAdvancedChipUser = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiSemiAutoEvaluateComplete;
    /* access modifiers changed from: private */
    public boolean mIsWifiSwitchRobotAlgorithmEnabled = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiproDisableOnReboot = true;
    /* access modifiers changed from: private */
    public boolean mIsWifiproInLinkMonitorLast = false;
    private int mLastCSPState = -1;
    /* access modifiers changed from: private */
    public String mLastConnectedSsid = "";
    /* access modifiers changed from: private */
    public int mLastDisconnectedRssi;
    /* access modifiers changed from: private */
    public long mLastDisconnectedTime;
    /* access modifiers changed from: private */
    public long mLastDisconnectedTimeStamp = -1;
    /* access modifiers changed from: private */
    public int mLastHandoverFailReason = -1;
    /* access modifiers changed from: private */
    public long mLastTime;
    /* access modifiers changed from: private */
    public int mLastWifiLevel;
    private long mLastWifiproDisableTime = 0;
    /* access modifiers changed from: private */
    public boolean mLoseInetRoveOut = false;
    /* access modifiers changed from: private */
    public String mManualConnectAp = "";
    private ContentObserver mMapNavigatingStateChangeObserver = new ContentObserver(null) {
        /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass11 */

        public void onChange(boolean selfChange) {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            boolean z = false;
            if (Settings.Secure.getInt(wifiProStateMachine.mContentResolver, WifiProStateMachine.MAPS_LOCATION_FLAG, 0) == 1) {
                z = true;
            }
            boolean unused = wifiProStateMachine.isMapNavigating = z;
            WifiProStateMachine.this.logI("MapNavigating state change, MapNavigating: " + WifiProStateMachine.this.isMapNavigating);
        }
    };
    /* access modifiers changed from: private */
    public boolean mNeedRetryMonitor;
    /* access modifiers changed from: private */
    public NetworkBlackListManager mNetworkBlackListManager;
    /* access modifiers changed from: private */
    public final Object mNetworkCheckLock = new Object();
    /* access modifiers changed from: private */
    public HwNetworkPropertyChecker mNetworkPropertyChecker = null;
    /* access modifiers changed from: private */
    public NetworkQosMonitor mNetworkQosMonitor = null;
    /* access modifiers changed from: private */
    public String mNewSelect_bssid;
    /* access modifiers changed from: private */
    public int mNotifyWifiLinkPoorReason = -1;
    /* access modifiers changed from: private */
    public int mOpenAvailableAPCounter;
    /* access modifiers changed from: private */
    public boolean mPhoneStateListenerRegisted = false;
    /* access modifiers changed from: private */
    public int mPortalNotificationId = -1;
    private String mPortalUsedUrl = null;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public String mRoSsid = null;
    /* access modifiers changed from: private */
    public boolean mRoveOutStarted = false;
    /* access modifiers changed from: private */
    public List<ScanResult> mScanResultList;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public String mUserManualConnecConfigKey = "";
    private ContentObserver mVehicleStateChangeObserver = new ContentObserver(null) {
        /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass10 */

        public void onChange(boolean selfChange) {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            boolean z = false;
            if (Settings.Secure.getInt(wifiProStateMachine.mContentResolver, WifiProStateMachine.VEHICLE_STATE_FLAG, 0) == 1) {
                z = true;
            }
            boolean unused = wifiProStateMachine.isVehicleState = z;
            WifiProStateMachine.this.logI("VehicleState state change, VehicleState: " + WifiProStateMachine.this.isVehicleState);
        }
    };
    /* access modifiers changed from: private */
    public boolean mVerfyingToConnectedState = false;
    /* access modifiers changed from: private */
    public WiFiLinkMonitorState mWiFiLinkMonitorState = new WiFiLinkMonitorState();
    /* access modifiers changed from: private */
    public int mWiFiNoInternetReason;
    private WiFiProDisabledState mWiFiProDisabledState = new WiFiProDisabledState();
    /* access modifiers changed from: private */
    public WiFiProEnableState mWiFiProEnableState = new WiFiProEnableState();
    /* access modifiers changed from: private */
    public WiFiProEvaluateController mWiFiProEvaluateController;
    /* access modifiers changed from: private */
    public volatile int mWiFiProPdpSwichValue;
    /* access modifiers changed from: private */
    public WiFiProVerfyingLinkState mWiFiProVerfyingLinkState = new WiFiProVerfyingLinkState();
    /* access modifiers changed from: private */
    public WifiConnectedState mWifiConnectedState = new WifiConnectedState();
    /* access modifiers changed from: private */
    public WifiDisConnectedState mWifiDisConnectedState = new WifiDisConnectedState();
    /* access modifiers changed from: private */
    public long mWifiDualBandStartTime = 0;
    /* access modifiers changed from: private */
    public WifiHandover mWifiHandover;
    /* access modifiers changed from: private */
    public long mWifiHandoverStartTime = 0;
    /* access modifiers changed from: private */
    public long mWifiHandoverSucceedTimestamp = 0;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public WifiProConfigStore mWifiProConfigStore;
    private WifiProConfigurationManager mWifiProConfigurationManager;
    /* access modifiers changed from: private */
    public WifiProStatisticsManager mWifiProStatisticsManager;
    /* access modifiers changed from: private */
    public WifiProUIDisplayManager mWifiProUIDisplayManager;
    /* access modifiers changed from: private */
    public WifiSemiAutoEvaluateState mWifiSemiAutoEvaluateState = new WifiSemiAutoEvaluateState();
    /* access modifiers changed from: private */
    public WifiSemiAutoScoreState mWifiSemiAutoScoreState = new WifiSemiAutoScoreState();
    /* access modifiers changed from: private */
    public int mWifiTcpRxCount;
    /* access modifiers changed from: private */
    public int mWifiToWifiType = 0;
    /* access modifiers changed from: private */
    public AsyncChannel mWsmChannel;
    /* access modifiers changed from: private */
    public WifiProPhoneStateListener phoneStateListener = null;
    /* access modifiers changed from: private */
    public String respCodeChrInfo = "";
    /* access modifiers changed from: private */
    public WifiProChrUploadManager uploadManager;

    static /* synthetic */ String access$10184(WifiProStateMachine x0, Object x1) {
        String str = x0.respCodeChrInfo + x1;
        x0.respCodeChrInfo = str;
        return str;
    }

    static /* synthetic */ int access$15308(WifiProStateMachine x0) {
        int i = x0.mCurrentVerfyCounter;
        x0.mCurrentVerfyCounter = i + 1;
        return i;
    }

    static /* synthetic */ int access$25208(WifiProStateMachine x0) {
        int i = x0.mOpenAvailableAPCounter;
        x0.mOpenAvailableAPCounter = i + 1;
        return i;
    }

    private WifiProStateMachine(Context context, Messenger dstMessenger, Looper looper) {
        super("WifiProStateMachine", looper);
        boolean z = false;
        this.mContext = context;
        this.mIsWifiSwitchRobotAlgorithmEnabled = WifiProCommonUtils.isWifiSwitchRobotAlgorithmEnabled();
        this.mIsWifi2CellInStrongSignalEnabled = WifiProCommonUtils.isWifi2CellInStrongSiganalEnabled();
        this.mIsWifiAdvancedChipUser = WifiProCommonUtils.isAdvancedChipUser();
        this.mWsmChannel = new AsyncChannel();
        this.mWsmChannel.connectSync(this.mContext, getHandler(), dstMessenger);
        this.mContentResolver = context.getContentResolver();
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        WifiProStatisticsManager.initStatisticsManager(this.mContext, getHandler() != null ? getHandler().getLooper() : null);
        this.mWifiProStatisticsManager = WifiProStatisticsManager.getInstance();
        this.mNetworkBlackListManager = NetworkBlackListManager.getNetworkBlackListManagerInstance(this.mContext);
        this.mWifiProUIDisplayManager = WifiProUIDisplayManager.createInstance(context, this);
        this.mWifiProConfigurationManager = WifiProConfigurationManager.createWifiProConfigurationManager(this.mContext);
        this.mWifiProConfigStore = new WifiProConfigStore(this.mContext, this.mWsmChannel);
        this.mAppWhitelists = this.mWifiProConfigurationManager.getAppWhitelists();
        this.mNetworkQosMonitor = new NetworkQosMonitor(this.mContext, this, dstMessenger, this.mWifiProUIDisplayManager);
        this.mWifiHandover = new WifiHandover(this.mContext, this);
        this.mIsWiFiProEnabled = WifiProCommonUtils.isWifiProSwitchOn(context);
        this.mIsPrimaryUser = ActivityManager.getCurrentUser() == 0 ? true : z;
        logI("UserID =  " + ActivityManager.getCurrentUser() + ", mIsPrimaryUser = " + this.mIsPrimaryUser);
        if (HwWifiProFeatureControl.sWifiProDualBandCtrl) {
            this.mDualBandManager = HwDualBandManager.createInstance(context, this);
        }
        this.mHwDualBandBlackListMgr = HwDualBandBlackListManager.getHwDualBandBlackListMgrInstance();
        this.phoneStateListener = new WifiProPhoneStateListener();
        this.mLastTime = 0;
        this.mCurrentTime = 0;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(IMS_SERVICE_STATE_CHANGED);
        this.mContext.registerReceiver(this.mImsStateChangedReceiver, intentFilter, IMS_SERVICE_STATE_BROADCAST_PERMISSION, null);
        this.uploadManager = WifiProChrUploadManager.getInstance(this.mContext);
        init(context);
    }

    public static WifiProStateMachine createWifiProStateMachine(Context context, Messenger dstMessenger, Looper looper) {
        if (mWifiProStateMachine == null) {
            mWifiProStateMachine = new WifiProStateMachine(context, dstMessenger, looper);
        }
        mWifiProStateMachine.start();
        return mWifiProStateMachine;
    }

    public static WifiProStateMachine getWifiProStateMachineImpl() {
        return mWifiProStateMachine;
    }

    /* access modifiers changed from: private */
    public static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return Settings.System.getInt(cr, name, def ? 1 : 0) == 1;
    }

    /* access modifiers changed from: private */
    public static boolean getSettingsGlobalBoolean(ContentResolver cr, String name, boolean def) {
        return Settings.Global.getInt(cr, name, def ? 1 : 0) == 1;
    }

    /* access modifiers changed from: private */
    public static boolean getSettingsSecureBoolean(ContentResolver cr, String name, boolean def) {
        return Settings.Secure.getInt(cr, name, def ? 1 : 0) == 1;
    }

    /* access modifiers changed from: private */
    public static int getSettingsSystemInt(ContentResolver cr, String name, int def) {
        return Settings.System.getInt(cr, name, def);
    }

    private void init(Context context) {
        NetworkQosMonitor networkQosMonitor = this.mNetworkQosMonitor;
        if (networkQosMonitor != null) {
            this.mNetworkPropertyChecker = networkQosMonitor.getNetworkPropertyChecker();
        }
        addState(this.mDefaultState);
        addState(this.mWiFiProDisabledState, this.mDefaultState);
        addState(this.mWiFiProEnableState, this.mDefaultState);
        addState(this.mWifiConnectedState, this.mWiFiProEnableState);
        addState(this.mWiFiProVerfyingLinkState, this.mWiFiProEnableState);
        addState(this.mWifiDisConnectedState, this.mWiFiProEnableState);
        addState(this.mWiFiLinkMonitorState, this.mWiFiProEnableState);
        addState(this.mWifiSemiAutoEvaluateState, this.mWiFiProEnableState);
        addState(this.mWifiSemiAutoScoreState, this.mWifiSemiAutoEvaluateState);
        this.mWiFiProEvaluateController = new WiFiProEvaluateController(context);
        this.mIsPortalAp = false;
        this.mIsNetworkAuthen = false;
        registerMapNavigatingStateChanges();
        registerVehicleStateChanges();
        registerForSettingsChanges();
        registerForMobileDataChanges();
        registerForMobilePDPSwitchChanges();
        registerNetworkReceiver();
        registerOOBECompleted();
        registerForVpnSettingsChanges();
        registerForAppPidChanges();
        registerForAPEvaluateChanges();
        registerForManualConnectChanges();
        setInitialState(this.mWiFiProEnableState);
        HwHiLog.d(TAG, false, "System Create WifiProStateMachine Complete!", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void defaulVariableInit() {
        if (!this.isVariableInited) {
            this.mIsMobileDataEnabled = getSettingsGlobalBoolean(this.mContentResolver, "mobile_data", false);
            this.mEmuiPdpSwichValue = getSettingsSystemInt(this.mContentResolver, KEY_EMUI_WIFI_TO_PDP, 1);
            this.mIsWiFiProAutoEvaluateAP = getSettingsSecureBoolean(this.mContentResolver, KEY_WIFIPRO_RECOMMEND_NETWORK, false);
            this.mIsVpnWorking = getSettingsSystemBoolean(this.mContentResolver, SETTING_SECURE_VPN_WORK_VALUE, false);
            if (this.mIsVpnWorking) {
                Settings.System.putInt(this.mContext.getContentResolver(), SETTING_SECURE_VPN_WORK_VALUE, 0);
                this.mIsVpnWorking = false;
            }
            Settings.System.putString(this.mContext.getContentResolver(), KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY, "");
            setWifiEvaluateTag(false);
            this.isVariableInited = true;
            logI("Variable Init Complete!");
        }
    }

    private void registerNetworkReceiver() {
        this.mBroadcastReceiver = new NetworkBroadcastReceiver();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mIntentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mIntentFilter.addAction(WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_ROVE_IN);
        this.mIntentFilter.addAction(WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_DELETE);
        this.mIntentFilter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        this.mIntentFilter.addAction(HW_SYSTEM_SERVER_START);
        this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION);
        this.mIntentFilter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mIntentFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.p2p.CONNECT_STATE_CHANGE");
        this.mIntentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_PORTAL_USED_BY_USER);
        this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mIntentFilter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
        this.mHMDBroadcastReceiver = new HMDBroadcastReceiver();
        this.mHMDIntentFilter = new IntentFilter();
        this.mHMDIntentFilter.addAction(WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_ROVE_IN);
        this.mHMDIntentFilter.addAction(WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_DELETE);
        this.mHMDIntentFilter.addAction("com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY");
        this.mContext.registerReceiverAsUser(this.mHMDBroadcastReceiver, UserHandle.ALL, this.mHMDIntentFilter, null, null);
    }

    /* access modifiers changed from: private */
    public void updateChrToCell(boolean hasInternet) {
        if (this.uploadManager != null) {
            Bundle wifiToCellData = new Bundle();
            if (!hasInternet) {
                this.uploadManager.addChrSsidBundleStat(NO_INTERNET_TO_CELL_EVENT, WIFI_TO_CELL_CNT, wifiToCellData);
            } else {
                this.uploadManager.addChrSsidBundleStat(SLOW_INTERNET_TO_CELL_EVENT, WIFI_TO_CELL_CNT, wifiToCellData);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateChrToCellSucc(boolean hasInternet) {
        if (this.uploadManager != null) {
            Bundle wifiToCellSuccData = new Bundle();
            if (!hasInternet) {
                this.uploadManager.addChrSsidBundleStat(NO_INTERNET_TO_CELL_EVENT, WIFI_TO_CELL_SUCC_CNT, wifiToCellSuccData);
            } else {
                this.uploadManager.addChrSsidBundleStat(SLOW_INTERNET_TO_CELL_EVENT, WIFI_TO_CELL_SUCC_CNT, wifiToCellSuccData);
            }
        }
    }

    private class NetworkBroadcastReceiver extends BroadcastReceiver {
        private NetworkBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                handleNetworkStateChange(intent);
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE, intent);
            } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                if (WifiProStateMachine.this.mWifiManager.getWifiState() == 1) {
                    String unused = WifiProStateMachine.this.mUserManualConnecConfigKey = "";
                }
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_STATE_CHANGED_ACTION);
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DEVICE_SCREEN_ON);
                if (WifiProStateMachine.this.mWifiProStatisticsManager != null) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.sendScreenOnEvent();
                }
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DEVICE_USER_PRESENT);
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DEVICE_SCREEN_OFF);
            } else if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CONFIGURATION_CHANGED, intent);
            } else if ("android.net.wifi.SCAN_RESULTS".equals(action)) {
                WifiProStateMachine.this.handleScanResult();
            } else if ("android.net.wifi.supplicant.STATE_CHANGE".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_SUPPLICANT_STATE_CHANGE, intent);
            } else if ("android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(action)) {
                handleNetworkConfigChange(intent);
            } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_RSSI_CHANGE, intent);
            } else if (WifiProStateMachine.HW_SYSTEM_SERVER_START.equals(action)) {
                WifiProStateMachine.this.logI("recieve HW_SYSTEM_SERVER_START!!");
                WifiProStateMachine.this.registerBoosterService();
            } else {
                handleBroadCast(intent);
            }
        }

        private void handleBroadCast(Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.USER_SWITCHED".equals(action)) {
                boolean z = false;
                int userID = intent.getIntExtra("android.intent.extra.user_handle", 0);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                if (userID == 0) {
                    z = true;
                }
                boolean unused = wifiProStateMachine.mIsPrimaryUser = z;
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("user has switched,new userID = " + userID);
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE);
            } else if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(action)) {
                WifiProStateMachine.this.handleP2pConnectionChange(intent);
            } else if ("android.net.wifi.p2p.CONNECT_STATE_CHANGE".equals(action)) {
                WifiProStateMachine.this.handleP2pConnectStateChange(intent);
            } else if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_LOAD_CONFIG_INTERNET_INFO, 5000);
            } else if (WifiProCommonDefs.ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION.equals(action)) {
                WifiProStateMachine.this.logI("broadcast WifiProCommonDefs.ACTION_FIRST_CHECK_NO_INTERNET_NOTIFICATION received");
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_NO_INTERNET_NOTIFICATION);
            } else if (WifiProCommonDefs.ACTION_PORTAL_USED_BY_USER.equals(intent.getAction())) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_PORTAL_SELECTED);
            }
        }

        private void handleNetworkConfigChange(Intent intent) {
            WifiProStateMachine.this.mWiFiProEvaluateController.reSetEvaluateRecord(intent);
            if (intent.getIntExtra("changeReason", -1) == 1) {
                WifiProStateMachine.this.logI("UNEXPECT_SWITCH_EVENT: forgetAp: enter:");
                WifiProStateMachine.this.uploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, "forgetAp");
                if (SystemClock.elapsedRealtime() - WifiProStateMachine.this.mWifiHandoverSucceedTimestamp < 30000) {
                    WifiProStateMachine.this.uploadChrHandoverUnexpectedTypes(WifiProStateMachine.WIFI_HANDOVER_UNEXPECTED_TYPES[3]);
                }
            }
            if (WifiProStateMachine.this.getCurrentState() != WifiProStateMachine.this.mWifiSemiAutoScoreState) {
                WifiConfiguration connCfg = null;
                Object objConfig = intent.getParcelableExtra("wifiConfiguration");
                if (objConfig instanceof WifiConfiguration) {
                    connCfg = (WifiConfiguration) objConfig;
                } else {
                    WifiProStateMachine.this.logE("handleNetworkConfigChange:class is not match");
                }
                if (connCfg != null) {
                    int changeReason = intent.getIntExtra("changeReason", -1);
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("ssid = " + StringUtilEx.safeDisplaySsid(connCfg.SSID) + ", change reson " + changeReason + ", isTempCreated = " + connCfg.isTempCreated);
                    if (connCfg.isTempCreated && changeReason != 1) {
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        wifiProStateMachine2.logI("WiFiProDisabledState, forget " + StringUtilEx.safeDisplaySsid(connCfg.SSID));
                        WifiProStateMachine.this.mWifiManager.forget(connCfg.networkId, null);
                    }
                }
            }
            WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CONFIGURED_NETWORKS_CHANGED, intent);
        }

        private void handleNetworkStateChange(Intent intent) {
            NetworkInfo info = null;
            Object objNetworkInfo = intent.getParcelableExtra("networkInfo");
            if (objNetworkInfo instanceof NetworkInfo) {
                info = (NetworkInfo) objNetworkInfo;
            } else {
                WifiProStateMachine.this.logE("handleNetworkStateChange:Class is not match");
            }
            if (info != null) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("handleNetworkStateChange currentState = " + info.getState());
            }
            if (WifiProStateMachine.isWifiEvaluating() && WifiProStateMachine.this.mIsWiFiProEnabled) {
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                String unused = wifiProStateMachine2.mManualConnectAp = Settings.System.getString(wifiProStateMachine2.mContentResolver, WifiProStateMachine.KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY);
                if (!TextUtils.isEmpty(WifiProStateMachine.this.mManualConnectAp)) {
                    WifiProStateMachine.this.logI("ManualConnectedWiFi  AP, ,isWifiEvaluating ");
                    WifiProStateMachine.this.setWifiEvaluateTag(false);
                    WifiProStateMachine.this.mWiFiProEvaluateController.cleanEvaluateRecords();
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWiFiProEnableState);
                }
            }
            if (info != null && NetworkInfo.DetailedState.OBTAINING_IPADDR == info.getDetailedState()) {
                WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                wifiProStateMachine4.logI("wifi is conencted, WiFiProEnabled = " + WifiProStateMachine.this.mIsWiFiProEnabled + ", VpnWorking " + WifiProStateMachine.this.mIsVpnWorking);
            }
            WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE, intent);
        }
    }

    private void resetWifiEvaluteInternetType() {
        String str;
        WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
        if (wifiConfiguration != null && (str = this.mCurrentSsid) != null) {
            this.mWifiProConfigStore.updateWifiEvaluateConfig(wifiConfiguration, 1, 0, str);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 0);
            logI("After reset wifi network type, mCurrentSsid = " + StringUtilEx.safeDisplaySsid(this.mCurrentSsid) + ", accessType = " + this.mCurrentWifiConfig.internetAccessType + ", qosLevel = " + this.mCurrentWifiConfig.networkQosLevel);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 82, null);
        }
    }

    /* access modifiers changed from: private */
    public void resetWifiEvaluteQosLevel() {
        String str;
        WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
        if (wifiConfiguration != null && (str = this.mCurrentSsid) != null) {
            this.mWifiProConfigStore.updateWifiEvaluateConfig(wifiConfiguration, 2, 0, str);
            this.mWiFiProEvaluateController.updateScoreInfoLevel(this.mCurrentSsid, 0);
            logI("After reset wifiqoslevel, mCurrentSsid = " + StringUtilEx.safeDisplaySsid(this.mCurrentSsid) + ", accessType = " + this.mCurrentWifiConfig.internetAccessType + ", qosLevel = " + this.mCurrentWifiConfig.networkQosLevel);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 82, null);
        }
    }

    /* access modifiers changed from: private */
    public void handleScanResult() {
        if (this.mWifiProUIDisplayManager.mIsNotificationShown && this.mWiFiProEvaluateController.isAccessAPOutOfRange(this.mWifiManager.getScanResults())) {
            this.mWifiProUIDisplayManager.shownAccessNotification(false);
        }
        sendMessage(EVENT_SCAN_RESULTS_AVAILABLE);
    }

    /* access modifiers changed from: private */
    public void handleP2pConnectStateChange(Intent intent) {
        int p2pState = intent.getIntExtra("extraState", -1);
        if (p2pState == 1 || p2pState == 2) {
            this.mIsP2PConnectedOrConnecting = true;
        } else {
            this.mIsP2PConnectedOrConnecting = false;
        }
        sendMessage(EVENT_WIFI_P2P_CONNECTION_CHANGED);
    }

    /* access modifiers changed from: private */
    public void handleP2pConnectionChange(Intent intent) {
        NetworkInfo p2pNetworkInfo = null;
        Object objNetworkInfo = intent.getParcelableExtra("networkInfo");
        if (objNetworkInfo instanceof NetworkInfo) {
            p2pNetworkInfo = (NetworkInfo) objNetworkInfo;
        } else {
            logE("handleP2pConnectionChange:Class is not match");
        }
        if (p2pNetworkInfo != null) {
            this.mIsP2PConnectedOrConnecting = p2pNetworkInfo.isConnectedOrConnecting();
        }
        if (!this.mIsP2PConnectedOrConnecting) {
            sendMessage(EVENT_WIFI_P2P_CONNECTION_CHANGED);
        }
    }

    private class HMDBroadcastReceiver extends BroadcastReceiver {
        private HMDBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_ROVE_IN.equals(action)) {
                WifiProStateMachine.this.logI("ACTION_HIGH_MOBILE_DATA  rove in event received.");
                WifiProStateMachine.this.userHandoverWifi();
                if (WifiProStateMachine.this.mWifiProStatisticsManager != null) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseHighMobileDataBtnRiCount();
                }
            } else if (WifiProUIDisplayManager.ACTION_HIGH_MOBILE_DATA_DELETE.equals(action)) {
                WifiProStateMachine.this.logI("ACTION_HIGH_MOBILE_DATA  delete event received, stop notify.");
                if (WifiProStateMachine.this.mNetworkQosMonitor != null) {
                    WifiProStateMachine.this.mNetworkQosMonitor.setRoveOutToMobileState(0);
                }
                if (WifiProStateMachine.this.mWifiProStatisticsManager != null) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseUserDelNotifyCount();
                }
            } else if ("com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY".equals(action)) {
                WifiProStateMachine.this.logI("**receive wifi connected concurrently**");
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EVALUATE_START_CHECK_INTERNET);
            }
        }
    }

    private void unregisterReceiver() {
        BroadcastReceiver broadcastReceiver = this.mBroadcastReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            this.mBroadcastReceiver = null;
        }
        BroadcastReceiver broadcastReceiver2 = this.mHMDBroadcastReceiver;
        if (broadcastReceiver2 != null) {
            this.mContext.unregisterReceiver(broadcastReceiver2);
            this.mHMDBroadcastReceiver = null;
        }
    }

    private void registerOOBECompleted() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("device_provisioned"), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass2 */

            public void onChange(boolean selfChange) {
                if (WifiProStateMachine.getSettingsSystemInt(WifiProStateMachine.this.mContentResolver, "device_provisioned", 0) == 1) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateInitialWifiproState(WifiProStateMachine.this.mIsWiFiProEnabled);
                }
            }
        });
    }

    private void registerForSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(WifiProCommonUtils.KEY_WIFI_PRO_SWITCH), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass3 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                boolean unused = wifiProStateMachine.mIsWiFiProEnabled = WifiProStateMachine.getSettingsSystemBoolean(wifiProStateMachine.mContentResolver, WifiProCommonUtils.KEY_WIFI_PRO_SWITCH, false);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("Wifi pro setting has changed,WiFiProEnabled == " + WifiProStateMachine.this.mIsWiFiProEnabled);
                if (WifiProStateMachine.isWifiEvaluating() && !WifiProStateMachine.this.mIsWiFiProEnabled) {
                    boolean unused2 = WifiProStateMachine.this.restoreWiFiConfig();
                    WifiProStateMachine.this.setWifiEvaluateTag(false);
                }
                if (!WifiProStateMachine.this.mIsWiFiProEnabled) {
                    WifiProStateMachine.this.uploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, "closeWifiPro");
                }
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateWifiproState(WifiProStateMachine.this.mIsWiFiProEnabled);
                WifiProStateMachine.this.logI("OPEN_CLOSE_EVENT ready");
                WifiProStateMachine.this.uploadManager.addChrCntStat("openCloseEvent", "");
            }
        });
    }

    private void registerForMobileDataChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass4 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                boolean unused = wifiProStateMachine.mIsMobileDataEnabled = WifiProStateMachine.getSettingsGlobalBoolean(wifiProStateMachine.mContentResolver, "mobile_data", false);
                if (WifiProStateMachine.this.mIsMobileDataEnabled) {
                    HwWifiConnectivityMonitor.getInstance().notifyHandoverConditionsChangeToEnabled();
                }
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("MobileData has changed,isMobileDataEnabled = " + WifiProStateMachine.this.mIsMobileDataEnabled);
            }
        });
    }

    private void registerForMobilePDPSwitchChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_EMUI_WIFI_TO_PDP), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass5 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                int unused = wifiProStateMachine.mEmuiPdpSwichValue = WifiProStateMachine.getSettingsSystemInt(wifiProStateMachine.mContentResolver, WifiProStateMachine.KEY_EMUI_WIFI_TO_PDP, 1);
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EMUI_CSP_SETTINGS_CHANGE);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                int unused2 = wifiProStateMachine2.mWiFiProPdpSwichValue = wifiProStateMachine2.mEmuiPdpSwichValue;
                if (WifiProStateMachine.this.mWifiProStatisticsManager != null) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseSelCspSettingChgCount(WifiProStateMachine.this.mWiFiProPdpSwichValue);
                }
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.logI("Mobile PDP setting changed, mWiFiProPdpSwichValue = mWiFiProPdpSwichValue = " + WifiProStateMachine.this.mWiFiProPdpSwichValue);
            }
        });
    }

    private void registerForVpnSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SETTING_SECURE_VPN_WORK_VALUE), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass6 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                boolean unused = wifiProStateMachine.mIsVpnWorking = WifiProStateMachine.getSettingsSystemBoolean(wifiProStateMachine.mContentResolver, WifiProStateMachine.SETTING_SECURE_VPN_WORK_VALUE, false);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("vpn state has changed,mIsVpnWorking == " + WifiProStateMachine.this.mIsVpnWorking);
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.notifyVPNStateChanged(wifiProStateMachine3.mIsVpnWorking);
                if (WifiProStateMachine.this.mIsVpnWorking && WifiProStateMachine.this.getCurrentState() == WifiProStateMachine.this.mWiFiLinkMonitorState && WifiProStateMachine.this.mIsUserManualConnectSuccess) {
                    boolean unused2 = WifiProStateMachine.this.mIsWifiproInLinkMonitorLast = true;
                }
                if (WifiProStateMachine.this.getCurrentState() != WifiProStateMachine.this.mWifiDisConnectedState) {
                    if (WifiProStateMachine.this.mIsVpnWorking || WifiProStateMachine.this.getCurrentState() != WifiProStateMachine.this.mWifiConnectedState || !WifiProStateMachine.this.mIsUserManualConnectSuccess || !WifiProStateMachine.this.mIsWifiproInLinkMonitorLast) {
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE);
                    } else {
                        WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                        wifiProStateMachine4.transitionTo(wifiProStateMachine4.mWiFiLinkMonitorState);
                    }
                }
                if (!WifiProStateMachine.this.mIsVpnWorking) {
                    boolean unused3 = WifiProStateMachine.this.mIsWifiproInLinkMonitorLast = false;
                }
            }
        });
    }

    private void registerForAppPidChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SETTING_SECURE_CONN_WIFI_PID), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass7 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                int unused = wifiProStateMachine.mConnectWiFiAppPid = WifiProStateMachine.getSettingsSystemInt(wifiProStateMachine.mContentResolver, WifiProStateMachine.SETTING_SECURE_CONN_WIFI_PID, -1);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                StringBuilder sb = new StringBuilder();
                sb.append("current APP name == ");
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                sb.append(wifiProStateMachine3.getAppName(wifiProStateMachine3.mConnectWiFiAppPid));
                wifiProStateMachine2.logI(sb.toString());
            }
        });
    }

    private void registerForAPEvaluateChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_WIFIPRO_RECOMMEND_NETWORK), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass8 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                boolean unused = wifiProStateMachine.mIsWiFiProAutoEvaluateAP = WifiProStateMachine.getSettingsSecureBoolean(wifiProStateMachine.mContentResolver, WifiProStateMachine.KEY_WIFIPRO_RECOMMEND_NETWORK, false);
            }
        });
    }

    private void registerForManualConnectChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY), false, new ContentObserver(getHandler()) {
            /* class com.huawei.hwwifiproservice.WifiProStateMachine.AnonymousClass9 */

            public void onChange(boolean selfChange) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                String unused = wifiProStateMachine.mManualConnectAp = Settings.System.getString(wifiProStateMachine.mContentResolver, WifiProStateMachine.KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("mManualConnectAp has change:  " + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mManualConnectAp) + ", wifipro state = " + WifiProStateMachine.this.getCurrentState().getName());
                if (!TextUtils.isEmpty(WifiProStateMachine.this.mManualConnectAp)) {
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    String unused2 = wifiProStateMachine3.mUserManualConnecConfigKey = wifiProStateMachine3.mManualConnectAp;
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void resetVariables() {
        this.mNetworkQosMonitor.stopBqeService();
        this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
        this.mIsWiFiInternetCHRFlag = false;
        this.mWiFiProPdpSwichValue = 0;
        this.mNetworkQosMonitor.stopALLMonitor();
        this.mNetworkQosMonitor.resetMonitorStatus();
        this.mWifiProUIDisplayManager.cancelAllDialog();
        this.mCurrentVerfyCounter = 0;
        this.mIsUserHandoverWiFi = false;
        refreshConnectedNetWork();
        this.mIsWifiSemiAutoEvaluateComplete = false;
        this.mIsUserManualConnectSuccess = false;
        resetWifiProManualConnect();
        stopDualBandMonitor();
    }

    /* access modifiers changed from: private */
    public void resetScanedRssiVariable() {
        this.mIsScanedRssiLow = false;
        this.mIsScanedRssiMiddle = false;
    }

    /* access modifiers changed from: private */
    public void updateWifiInternetStateChange(int lenvel) {
        if (!WifiProCommonUtils.isWifiConnectedOrConnecting(this.mWifiManager)) {
            return;
        }
        if (this.mLastWifiLevel == lenvel) {
            logI("wifi lenvel is not change, don't report, lenvel = " + lenvel);
            return;
        }
        this.mLastWifiLevel = lenvel;
        if (-1 == lenvel) {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            Settings.Secure.putString(contentResolver, SETTING_SECURE_WIFI_NO_INT, "true," + this.mCurrentSsid);
            this.mWifiProUIDisplayManager.notificateNetAccessChange(true);
            logI("mIsPortalAp = " + this.mIsPortalAp + ", mIsNetworkAuthen = " + this.mIsNetworkAuthen);
            if (!this.mIsPortalAp || this.mIsNetworkAuthen) {
                this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, true, 0, false);
                this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 2, this.mCurrentSsid);
                this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 2);
                return;
            }
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, true, 1, false);
            this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 3, this.mCurrentSsid);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 3);
        } else if (6 == lenvel) {
            ContentResolver contentResolver2 = this.mContext.getContentResolver();
            Settings.Secure.putString(contentResolver2, SETTING_SECURE_WIFI_NO_INT, "true," + this.mCurrentSsid);
            this.mWifiProUIDisplayManager.notificateNetAccessChange(true);
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, true, 1, false);
            this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 3, this.mCurrentSsid);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 3);
        } else {
            Settings.Secure.putString(this.mContext.getContentResolver(), SETTING_SECURE_WIFI_NO_INT, "");
            this.mWifiProUIDisplayManager.notificateNetAccessChange(false);
            this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, false, 0, false);
            this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 4, this.mCurrentSsid);
            this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 4);
        }
    }

    /* access modifiers changed from: private */
    public void reSetWifiInternetState() {
        logI("reSetWifiInternetState");
        Settings.Secure.putString(this.mContext.getContentResolver(), SETTING_SECURE_WIFI_NO_INT, "");
    }

    /* access modifiers changed from: private */
    public void setWifiCSPState(int state) {
        if (this.mLastCSPState == state) {
            logI("setWifiCSPState state is not change,ignor! mLastCSPState:" + this.mLastCSPState);
            return;
        }
        logI("setWifiCSPState new state = " + state);
        this.mLastCSPState = state;
        Settings.System.putInt(this.mContext.getContentResolver(), WIFI_CSP_DISPALY_STATE, state);
    }

    /* access modifiers changed from: private */
    public void registerCallBack() {
        this.mNetworkQosMonitor.registerCallBack(this);
        this.mWifiHandover.registerCallBack(this, this.mNetworkQosMonitor);
        this.mWifiProUIDisplayManager.registerCallBack(this);
    }

    /* access modifiers changed from: private */
    public void unRegisterCallBack() {
        this.mNetworkQosMonitor.unRegisterCallBack();
        this.mWifiHandover.unRegisterCallBack();
        this.mWifiProUIDisplayManager.unRegisterCallBack();
    }

    /* access modifiers changed from: private */
    public boolean isWiFiPoorer(int wifi_level, int mobile_level) {
        logI("WiFi Qos =[ " + wifi_level + " ] ,  Mobile Qos =[ " + mobile_level + "]");
        if (mobile_level == 0) {
            return false;
        }
        if (this.mIsWiFiNoInternet) {
            if (-1 < mobile_level) {
                return true;
            }
            return false;
        } else if (wifi_level < mobile_level) {
            return true;
        } else {
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean isMobileDataConnected() {
        if (5 != this.mTelephonyManager.getSimState() || !this.mIsMobileDataEnabled || isAirModeOn()) {
            return false;
        }
        return true;
    }

    private boolean isAirModeOn() {
        Context context = this.mContext;
        if (context != null && Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) == 1) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public synchronized boolean isWifiConnected() {
        WifiInfo conInfo;
        if (!this.mWifiManager.isWifiEnabled() || (conInfo = this.mWifiManager.getConnectionInfo()) == null || conInfo.getNetworkId() == -1 || conInfo.getBSSID() == null || "00:00:00:00:00:00".equals(conInfo.getBSSID()) || conInfo.getSupplicantState() != SupplicantState.COMPLETED) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void notifyManualConnectAP(boolean isUserManualConnect, boolean isUserHandoverWiFi) {
        Bundle data = new Bundle();
        data.putBoolean("isUserManualConnect", isUserManualConnect);
        data.putBoolean("isUserHandoverWiFi", isUserHandoverWiFi);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 35, data);
        if (isUserManualConnect && this.mWifiHandoverSucceedTimestamp != 0 && SystemClock.elapsedRealtime() - this.mWifiHandoverSucceedTimestamp < 30000) {
            WifiProChrUploadManager wifiProChrUploadManager = this.uploadManager;
            if (wifiProChrUploadManager != null) {
                wifiProChrUploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, WifiproUtils.UNEXPECTED_EVENT_USER_REJECT);
            }
            uploadChrHandoverUnexpectedTypes(WIFI_HANDOVER_UNEXPECTED_TYPES[2]);
        }
    }

    /* access modifiers changed from: private */
    public void notifyVPNStateChanged(boolean isVpnConnected) {
        Bundle data = new Bundle();
        data.putBoolean("isVpnConnected", isVpnConnected);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 36, data);
    }

    /* access modifiers changed from: private */
    public boolean isKeepCurrWiFiConnected() {
        this.mLastHandoverFailReason = this.mHandoverFailReason;
        if (this.mIsVpnWorking) {
            this.mHandoverFailReason = 0;
            logW("vpn is working,should keep current connect");
        }
        if (this.mIsUserManualConnectSuccess && !this.mIsWiFiProEnabled) {
            logW("user manual connect and wifi+ disabled, keep connect and no dialog.");
        }
        Bundle data = new Bundle();
        data.putParcelable("CurrWifiInfo", this.mCurrWifiInfo);
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 40, data);
        boolean isKeepCurrMplinkConnected = false;
        if (result != null) {
            isKeepCurrMplinkConnected = result.getBoolean("isKeepCurrMplinkConnected");
        }
        if (this.mIsUserHandoverWiFi) {
            this.mHandoverFailReason = 3;
        }
        if (this.mHiLinkUnconfig) {
            this.mHandoverFailReason = 2;
        }
        if (isWifiRepeaterOn()) {
            this.mHandoverFailReason = 4;
        }
        int i = this.mHandoverFailReason;
        if (i != this.mLastHandoverFailReason) {
            uploadWifiSwitchFailTypeStatistics(i);
            this.mLastHandoverFailReason = this.mHandoverFailReason;
        }
        if (this.mIsVpnWorking || this.mIsUserHandoverWiFi || this.mHiLinkUnconfig || isAppinWhitelists() || isWifiRepeaterOn() || isKeepCurrMplinkConnected) {
            return true;
        }
        return false;
    }

    private boolean isWifiRepeaterOn() {
        int state = Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_repeater_on", 0);
        if (1 == state || 6 == state) {
            return true;
        }
        return false;
    }

    private boolean isHwSyncClinetConnected() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), HWSYNC_DEVICE_CONNECTED_KEY, 0) != 0) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isAllowWiFiAutoEvaluate() {
        boolean z = this.mIsWiFiProAutoEvaluateAP;
        return this.mIsWiFiProEnabled && !this.mIsVpnWorking;
    }

    /* access modifiers changed from: private */
    public void refreshConnectedNetWork() {
        if (WifiProCommonUtils.isWifiConnectedOrConnecting(this.mWifiManager)) {
            WifiInfo conInfo = this.mWifiManager.getConnectionInfo();
            this.mCurrWifiInfo = conInfo;
            if (conInfo != null) {
                this.mCurrentBssid = conInfo.getBSSID();
                this.mCurrentSsid = conInfo.getSSID();
                this.mCurrentRssi = conInfo.getRssi();
                List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
                if (configNetworks != null) {
                    for (WifiConfiguration config : configNetworks) {
                        if (config.networkId == conInfo.getNetworkId()) {
                            this.mCurrentWifiConfig = config;
                        }
                    }
                    return;
                }
                return;
            }
        }
        this.mCurrentBssid = null;
        this.mCurrentSsid = null;
        this.mCurrentRssi = WifiHandover.INVALID_RSSI;
    }

    /* access modifiers changed from: private */
    public boolean isAllowWifi2Mobile() {
        if (!this.mIsWiFiProEnabled || !this.mIsPrimaryUser || !isMobileDataConnected() || !this.mPowerManager.isScreenOn() || !HwWifiProFeatureControl.sWifiProToCellularCtrl || this.mEmuiPdpSwichValue == 2 || WifiProCommonUtils.isCellNetworkClass2g(this.mTelephonyManager)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isPdpAvailable() {
        if ("true".equals(Settings.Global.getString(this.mContext.getContentResolver(), SYS_PROPERT_PDP))) {
            logI("SYS_PROPERT_PDP hw_RemindWifiToPdp is true");
            return true;
        }
        logI("SYS_PROPERT_PDP hw_RemindWifiToPdp is false");
        return false;
    }

    /* access modifiers changed from: private */
    public String getAppName(int pid) {
        Object objManager = this.mContext.getSystemService("activity");
        ActivityManager am = null;
        if (objManager instanceof ActivityManager) {
            am = (ActivityManager) objManager;
        }
        if (am == null) {
            logE("getAppName:class is not match");
            return "";
        }
        List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
        if (appProcessList == null) {
            return "";
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return "";
    }

    private boolean isAppinWhitelists() {
        List<String> list;
        WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
        if (wifiConfiguration == null) {
            return false;
        }
        String currentAppName = wifiConfiguration.creatorName;
        logI("isAppinWhitelists, currentAppName =  " + currentAppName);
        if (!TextUtils.isEmpty(currentAppName) && (list = this.mAppWhitelists) != null) {
            for (String appName : list) {
                if (currentAppName.equals(appName)) {
                    logI("curr name in the  Whitelists ");
                    return true;
                }
            }
        }
        return false;
    }

    private void resetWifiProManualConnect() {
        if (!TextUtils.isEmpty(this.mManualConnectAp)) {
            Settings.System.putString(this.mContext.getContentResolver(), KEY_WIFIPRO_MANUAL_CONNECT_CONFIGKEY, "");
        }
    }

    /* access modifiers changed from: private */
    public void notifyPortalStatusChanged(boolean popUp, String configKey, boolean hasInternetAccess) {
        Bundle data = new Bundle();
        data.putBoolean("popUp", popUp);
        data.putString("configKey", configKey);
        data.putBoolean("hasInternetAccess", hasInternetAccess);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 33, data);
    }

    /* access modifiers changed from: private */
    public void updateWifiConfig(WifiConfiguration config) {
        if (config != null) {
            Bundle data = new Bundle();
            data.putInt("messageWhat", 131672);
            data.putParcelable("messageObj", config);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            WifiProStateMachine.this.logI("DefaultState is Enter");
            WifiProStateMachine.this.defaulVariableInit();
        }

        public void exit() {
            WifiProStateMachine.this.logI("DefaultState is Exit");
        }

        public boolean processMessage(Message msg) {
            if (msg.what != WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE) {
                return false;
            }
            if (!WifiProStateMachine.this.mIsWiFiProEnabled || !WifiProStateMachine.this.mIsPrimaryUser) {
                WifiProStateMachine.this.onDisableWiFiPro();
                return true;
            }
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiProEnableState);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class WiFiProEnableState extends State {
        WiFiProEnableState() {
        }

        public void enter() {
            WifiProStateMachine.this.logI("WiFiProEnableState is Enter");
            boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
            int unused2 = WifiProStateMachine.this.mWiFiProPdpSwichValue = 0;
            WifiProStateMachine.this.registerCallBack();
            WifiProStateMachine.this.mNetworkQosMonitor.setWifiWatchDogEnabled(true);
            boolean unused3 = WifiProStateMachine.mIsWifiManualEvaluating = false;
            boolean unused4 = WifiProStateMachine.mIsWifiSemiAutoEvaluating = false;
            boolean unused5 = WifiProStateMachine.this.mIsWifiSemiAutoEvaluateComplete = false;
            if (WifiProStateMachine.this.mIsWiFiProEnabled) {
                boolean unused6 = WifiProStateMachine.this.mIsWifiproDisableOnReboot = false;
                WifiProStateMachine.this.startDualBandManager();
                if (WifiProStateMachine.this.mHwIntelligenceWiFiManager != null) {
                    WifiProStateMachine.this.mHwIntelligenceWiFiManager.start();
                }
            }
            transitionNetState();
        }

        public void exit() {
            WifiProStateMachine.this.logI("WiFiProEnableState is Exit");
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE /*{ENCODED_INT: 136171}*/:
                    if (!WifiProStateMachine.this.mIsWiFiProEnabled || !WifiProStateMachine.this.mIsPrimaryUser) {
                        WifiProStateMachine.this.onDisableWiFiPro();
                        return true;
                    }
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiProEnableState);
                    return true;
                case WifiProStateMachine.EVENT_WIFI_STATE_CHANGED_ACTION /*{ENCODED_INT: 136185}*/:
                    handleWifiStateChanged();
                    return true;
                case WifiProStateMachine.EVENT_CHR_CHECK_WIFI_HANDOVER /*{ENCODED_INT: 136214}*/:
                    WifiProStateMachine.this.handleChrWifiHandoverCheck();
                    return true;
                case WifiProStateMachine.EVENT_SCAN_RESULTS_AVAILABLE /*{ENCODED_INT: 136293}*/:
                    handleScanResult();
                    return true;
                case WifiProStateMachine.EVENT_WIFIPRO_EVALUTE_STATE_CHANGE /*{ENCODED_INT: 136298}*/:
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWiFiProEnableState);
                    return true;
                case WifiProStateMachine.EVENT_LOAD_CONFIG_INTERNET_INFO /*{ENCODED_INT: 136315}*/:
                    WifiProStateMachine.this.logI("WiFiProEnableState EVENT_LOAD_CONFIG_INTERNET_INFO");
                    WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
                    WifiProStateMachine.this.mWiFiProEvaluateController.initWifiProEvaluateRecords();
                    return true;
                case WifiProStateMachine.EVENT_DUALBAND_NETWROK_TYPE /*{ENCODED_INT: 136316}*/:
                    handleDualBandNetworkType(msg);
                    return true;
                case WifiProStateMachine.EVENT_PROCESS_GRS /*{ENCODED_INT: 136374}*/:
                    WifiProStateMachine.this.logI("get probe urls by GRS");
                    new GrsApiManager(WifiProStateMachine.this.mContext).ayncGetGrsUrls();
                    return true;
                default:
                    return false;
            }
        }

        private void handleDualBandNetworkType(Message msg) {
            List<HwDualBandMonitorInfo> apList = null;
            if (msg.obj != null) {
                apList = (List) msg.obj;
            }
            if (apList == null || apList.size() == 0) {
                WifiProStateMachine.this.loge("onDualBandNetWorkType apList null error");
            } else if (WifiProStateMachine.this.mIsUserManualConnectSuccess) {
                WifiProStateMachine.this.logI("keep curreny connect,ignore dualband ap handover");
            } else {
                int type = msg.arg1;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("onDualBandNetWorkType type = " + type + " apList.size() = " + apList.size());
                WifiProStateMachine.this.mDualBandMonitorApList.clear();
                int unused = WifiProStateMachine.this.mDualBandMonitorInfoSize = apList.size();
                for (HwDualBandMonitorInfo monitorInfo : apList) {
                    WifiProStateMachine.this.mDualBandMonitorApList.add(monitorInfo);
                    WifiProEstimateApInfo apInfo = new WifiProEstimateApInfo();
                    apInfo.setApBssid(monitorInfo.mBssid);
                    apInfo.setApRssi(monitorInfo.mCurrentRssi);
                    apInfo.setApAuthType(monitorInfo.mAuthType);
                    WifiProStateMachine.this.mNetworkQosMonitor.get5GApRssiThreshold(apInfo);
                }
            }
        }

        private void transitionNetState() {
            if (WifiProStateMachine.this.isWifiConnected()) {
                WifiProStateMachine.this.logI("WiFiProEnableState,go to WifiConnectedState");
                WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiConnectedState);
                return;
            }
            WifiProStateMachine.this.logI("WiFiProEnableState, go to mWifiDisConnectedState");
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiDisConnectedState);
        }

        private void handleScanResult() {
            if (!TextUtils.isEmpty(WifiProStateMachine.this.mUserManualConnecConfigKey)) {
                WifiProStateMachine.this.logI("User manual connecting ap, ignor this evaluate scan result");
            } else if (WifiProStateMachine.this.isAllowWiFiAutoEvaluate() && WifiProStateMachine.this.mPowerManager.isScreenOn() && WifiProStateMachine.this.mWifiManager.isWifiEnabled()) {
                if (System.currentTimeMillis() - WifiProStateMachine.this.mLastDisconnectedTime < 6000) {
                    WifiProStateMachine.this.logI("Disconnected time less than 6s, ignor this scan result");
                    return;
                }
                NetworkInfo wifiInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(1);
                if (wifiInfo != null) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    List unused = wifiProStateMachine.mScanResultList = wifiProStateMachine.mWifiManager.getScanResults();
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    List unused2 = wifiProStateMachine2.mScanResultList = wifiProStateMachine2.mWiFiProEvaluateController.scanResultListFilter(WifiProStateMachine.this.mScanResultList);
                    if (WifiProStateMachine.this.mScanResultList != null && WifiProStateMachine.this.mScanResultList.size() != 0) {
                        boolean issetting = WifiProStateMachine.this.isSettingsActivity();
                        int evaluateType = 0;
                        if (issetting) {
                            evaluateType = 1;
                        }
                        WifiProStateMachine.this.handleEvaluteScanResult(wifiInfo, issetting, evaluateType);
                    }
                }
            }
        }

        private void handleWifiStateChanged() {
            if (WifiProStateMachine.this.mWifiManager.getWifiState() == 1) {
                WifiProStateMachine.this.logI("wifi state is DISABLED, go to wifi disconnected");
                if (0 != WifiProStateMachine.this.mChrRoveOutStartTime) {
                    WifiProStateMachine.this.logI("BQE bad rove out, wifi disable time recorded.");
                    long unused = WifiProStateMachine.this.mChrWifiDidableStartTime = System.currentTimeMillis();
                }
                WifiProStateMachine.this.logI("UNEXPECT_SWITCH_EVENT: closeWifi: enter:");
                WifiProStateMachine.this.uploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, "closeWifi");
                if (SystemClock.elapsedRealtime() - WifiProStateMachine.this.mWifiHandoverSucceedTimestamp < 30000) {
                    WifiProStateMachine.this.uploadChrHandoverUnexpectedTypes(WifiProStateMachine.WIFI_HANDOVER_UNEXPECTED_TYPES[0]);
                }
                if (WifiProStateMachine.this.shouldUploadCloseWifiEvent()) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.uploadWifiproEvent(WifiProCommonUtils.ID_WIFI_USER_CLOSE_WIFI_STAT_INFO);
                }
                WifiProStateMachine.this.mWifiProUIDisplayManager.shownAccessNotification(false);
                if (WifiProStateMachine.this.getCurrentState() != WifiProStateMachine.this.mWifiDisConnectedState) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiDisConnectedState);
                }
            } else if (WifiProStateMachine.this.mWifiManager.getWifiState() == 3) {
                WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
                WifiProStateMachine.this.mWiFiProEvaluateController.initWifiProEvaluateRecords();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleEvaluteScanResult(NetworkInfo wifiInfo, boolean issetting, int evaluateType) {
        if (WifiProCommonUtils.isWifiConnectedOrConnecting(this.mWifiManager) || wifiInfo.getDetailedState() != NetworkInfo.DetailedState.DISCONNECTED) {
            this.mWiFiProEvaluateController.updateEvaluateRecords(this.mScanResultList, evaluateType, this.mCurrentSsid);
        } else if (this.isMapNavigating || this.isVehicleState) {
            logI("MapNavigatingOrVehicleState, ignor this scan result");
        } else if (this.mIsP2PConnectedOrConnecting) {
            logI("P2PConnectedOrConnecting, ignor this scan result");
            this.mWiFiProEvaluateController.updateEvaluateRecords(this.mScanResultList, evaluateType, this.mCurrentSsid);
        } else if (!this.mWiFiProEvaluateController.isAllowAutoEvaluate(this.mScanResultList)) {
            this.mWiFiProEvaluateController.updateEvaluateRecords(this.mScanResultList, evaluateType, this.mCurrentSsid);
        } else {
            for (ScanResult scanResult : this.mScanResultList) {
                if (this.mWiFiProEvaluateController.isAllowEvaluate(scanResult, evaluateType) && !this.mWiFiProEvaluateController.isLastEvaluateValid(scanResult, evaluateType)) {
                    this.mWiFiProEvaluateController.addEvaluateRecords(scanResult, evaluateType);
                }
            }
            this.mWiFiProEvaluateController.orderByRssi();
            boolean isfactorymode = "factory".equals(SystemProperties.get("ro.runmode", "normal"));
            if (this.mWiFiProEvaluateController.isUnEvaluateAPRecordsEmpty() || isfactorymode) {
                logE("UnEvaluateAPRecords is Empty");
                return;
            }
            this.mWiFiProEvaluateController.unEvaluateAPQueueDump();
            logI("transition to mwifiSemiAutoEvaluateState, to evaluate ap");
            if (issetting) {
                this.mWifiProStatisticsManager.updateBGChrStatistic(2);
            } else {
                this.mWifiProStatisticsManager.updateBGChrStatistic(1);
            }
            transitionTo(this.mWifiSemiAutoEvaluateState);
        }
    }

    /* access modifiers changed from: private */
    public void reportNetworkConnectivity(boolean hasConnectivity) {
        Network[] networks = this.mConnectivityManager.getAllNetworks();
        int length = networks.length;
        int i = 0;
        while (i < length) {
            Network nw = networks[i];
            NetworkCapabilities nc = this.mConnectivityManager.getNetworkCapabilities(nw);
            if (nc == null || !nc.hasTransport(1) || !nc.hasCapability(12)) {
                i++;
            } else {
                this.mConnectivityManager.reportNetworkConnectivity(nw, hasConnectivity);
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isWifiNetworkCapabilityValidated() {
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (!(connectivityManager == null || connectivityManager.getAllNetworks() == null)) {
            for (Network network : this.mConnectivityManager.getAllNetworks()) {
                NetworkCapabilities nc = this.mConnectivityManager.getNetworkCapabilities(network);
                if (nc != null && nc.hasTransport(1)) {
                    return nc.hasCapability(16);
                }
            }
        }
        return false;
    }

    class WiFiProDisabledState extends State {
        WiFiProDisabledState() {
        }

        public void enter() {
            WifiProStateMachine.this.logI("WiFiProDisabledState is Enter");
            boolean unused = WifiProStateMachine.mIsWifiManualEvaluating = false;
            boolean unused2 = WifiProStateMachine.mIsWifiSemiAutoEvaluating = false;
            WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            WifiProStateMachine.this.mNetworkQosMonitor.stopBqeService();
            WifiProStateMachine.this.unRegisterCallBack();
            WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
            WifiProStateMachine.this.mWifiProUIDisplayManager.shownAccessNotification(false);
            WifiProStateMachine.this.mWiFiProEvaluateController.cleanEvaluateRecords();
            WifiProStateMachine.this.mNetworkQosMonitor.setWifiWatchDogEnabled(false);
            WifiProStateMachine.this.stopDualBandManager();
            if (WifiProStateMachine.this.isWifiConnected()) {
                WifiProStateMachine.this.logI("WiFiProDisabledState , wifi is connect ");
                WifiInfo cInfo = WifiProStateMachine.this.mWifiManager.getConnectionInfo();
                if (cInfo != null && SupplicantState.COMPLETED == cInfo.getSupplicantState() && NetworkInfo.DetailedState.OBTAINING_IPADDR == WifiInfo.getDetailedStateOf(SupplicantState.COMPLETED)) {
                    WifiProStateMachine.this.logI("wifi State == VERIFYING_POOR_LINK");
                    WifiProStateMachine.this.mWsmChannel.sendMessage(131874);
                }
                WifiProStateMachine.this.setWifiCSPState(1);
            }
            WifiProStateMachine.this.resetVariables();
        }

        public void exit() {
            WifiProStateMachine.this.logI("WiFiProDisabledState is Exit");
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 131873:
                    WifiProStateMachine.this.logI("receive POOR_LINK_DETECTED sendMessageDelayed");
                    WifiProStateMachine.this.mWsmChannel.sendMessage(131874);
                    return true;
                case 131874:
                case WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE /*{ENCODED_INT: 136177}*/:
                case WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION /*{ENCODED_INT: 136186}*/:
                    return true;
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                    WifiProStateMachine.this.handleWifiNetWorkChange(msg);
                    return true;
                case WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE /*{ENCODED_INT: 136171}*/:
                    if (!WifiProStateMachine.this.mIsWiFiProEnabled || !WifiProStateMachine.this.mIsPrimaryUser) {
                        WifiProStateMachine.this.onDisableWiFiPro();
                        return true;
                    }
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiProEnableState);
                    return true;
                case WifiProStateMachine.EVENT_WIFI_STATE_CHANGED_ACTION /*{ENCODED_INT: 136185}*/:
                    if (WifiProStateMachine.this.mWifiManager.getWifiState() != 3) {
                        return true;
                    }
                    WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
                    return true;
                case WifiProStateMachine.EVENT_CONFIGURED_NETWORKS_CHANGED /*{ENCODED_INT: 136308}*/:
                    WifiProStateMachine.this.handleConfigNetworkChange(msg);
                    return true;
                default:
                    return false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleConfigNetworkChange(Message msg) {
        if (msg.obj instanceof Intent) {
            Intent configIntent = (Intent) msg.obj;
            WifiConfiguration connCfg = null;
            if (configIntent.getParcelableExtra("wifiConfiguration") instanceof WifiConfiguration) {
                connCfg = (WifiConfiguration) configIntent.getParcelableExtra("wifiConfiguration");
            } else {
                logE("handleConfigNetworkChange:WifiConfiguration is not match the class");
            }
            if (connCfg != null) {
                int changeReason = configIntent.getIntExtra("changeReason", -1);
                if (connCfg.isTempCreated && changeReason != 1) {
                    logI("WiFiProDisabledState, forget " + StringUtilEx.safeDisplaySsid(connCfg.SSID));
                    this.mWifiManager.forget(connCfg.networkId, null);
                    return;
                }
                return;
            }
            return;
        }
        logE("handleConfigNetworkChange:configIntent is not match the class");
    }

    /* access modifiers changed from: private */
    public void handleWifiNetWorkChange(Message msg) {
        if (msg.obj instanceof Intent) {
            Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
            NetworkInfo networkInfo = null;
            if (objNetworkInfo instanceof NetworkInfo) {
                networkInfo = (NetworkInfo) objNetworkInfo;
            }
            if (networkInfo != null && NetworkInfo.DetailedState.VERIFYING_POOR_LINK == networkInfo.getDetailedState()) {
                this.mWsmChannel.sendMessage(131874);
            } else if (networkInfo != null && NetworkInfo.State.CONNECTING == networkInfo.getState()) {
                this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            } else if (networkInfo != null && NetworkInfo.State.CONNECTED == networkInfo.getState()) {
                setWifiCSPState(1);
            }
        } else {
            logE("handleWifiNetWorkChange:Class is not match");
        }
    }

    /* access modifiers changed from: package-private */
    public class WifiConnectedState extends State {
        private int internetFailureDetectedCount;
        private boolean isChrShouldReport;
        private boolean isIgnorAvailableWifiCheck;
        private boolean isKeepConnected;
        private boolean isPortalAP;
        private boolean isPortalChrEverUploaded;
        private boolean isToastDisplayed;
        private int oldType;
        private int portalCheckCounter;

        WifiConnectedState() {
        }

        private void initConnectedState() {
            WifiProStateMachine.this.setWifiEvaluateTag(false);
            boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
            boolean unused2 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = false;
            this.isKeepConnected = false;
            this.isPortalAP = false;
            this.portalCheckCounter = 0;
            boolean unused3 = WifiProStateMachine.this.mIsScanedRssiLow = false;
            boolean unused4 = WifiProStateMachine.this.mIsScanedRssiMiddle = false;
            this.isIgnorAvailableWifiCheck = true;
            boolean unused5 = WifiProStateMachine.this.isDialogUpWhenConnected = false;
            boolean unused6 = WifiProStateMachine.this.mIsPortalAp = false;
            boolean unused7 = WifiProStateMachine.this.mIsNetworkAuthen = false;
            this.isPortalChrEverUploaded = false;
            WifiProStateMachine.this.refreshConnectedNetWork();
            int unused8 = WifiProStateMachine.this.mLastWifiLevel = 0;
            this.internetFailureDetectedCount = 0;
            WifiProStateMachine.this.setWifiCSPState(1);
            boolean unused9 = WifiProStateMachine.this.mHiLinkUnconfig = isHiLinkUnconfigRouter();
            if (!TextUtils.isEmpty(WifiProStateMachine.this.mUserManualConnecConfigKey) && WifiProStateMachine.this.mCurrentWifiConfig != null && WifiProStateMachine.this.mUserManualConnecConfigKey.equals(WifiProStateMachine.this.mCurrentWifiConfig.configKey())) {
                boolean unused10 = WifiProStateMachine.this.mIsUserManualConnectSuccess = true;
                long deltaTime = System.currentTimeMillis() - WifiProStateMachine.this.mLastDisconnectedTimeStamp;
                if (WifiProStateMachine.this.mCurrentSsid != null && !WifiProStateMachine.this.mCurrentSsid.equals(WifiProStateMachine.this.mLastConnectedSsid) && deltaTime < 10000 && WifiProStateMachine.this.mLastDisconnectedRssi < -75) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.uploadWifiproEvent(WifiProCommonUtils.ID_WIFI_USER_CONNECT_OTHER_WIFI_STAT_INFO);
                }
                WifiProStateMachine.this.logI("User manual connect ap success!");
            }
            String unused11 = WifiProStateMachine.this.mUserManualConnecConfigKey = "";
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.notifyManualConnectAP(wifiProStateMachine.mIsUserManualConnectSuccess, WifiProStateMachine.this.mIsUserHandoverWiFi);
            if (WifiProStateMachine.this.isKeepCurrWiFiConnected()) {
                WifiProStateMachine.this.refreshConnectedNetWork();
                WifiProStateMachine.this.mWifiProConfigStore.cleanWifiProConfig(WifiProStateMachine.this.mCurrentWifiConfig);
                WifiProStateMachine.this.mWiFiProEvaluateController.reSetEvaluateRecord(WifiProStateMachine.this.mCurrentSsid);
                WifiProStateMachine.this.mWifiProUIDisplayManager.notificateNetAccessChange(false);
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DIALOG_CANCEL);
            }
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.logI("isAllowWiFiAutoEvaluate == " + WifiProStateMachine.this.isAllowWiFiAutoEvaluate());
            WifiProStateMachine.this.initWifiConfig();
        }

        private void reportDiffTypeCHR(int newType) {
            if (!this.isChrShouldReport) {
                this.isChrShouldReport = true;
                WifiProStateMachine.this.mWiFiProEvaluateController.updateWifiProbeMode(WifiProStateMachine.this.mCurrentSsid, 0);
                int diffType = WifiProStateMachine.this.mWiFiProEvaluateController.getChrDiffType(this.oldType, newType);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("reportDiffTypeCHR is Enter, diffType  == " + diffType);
                if (diffType != 0) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(WifiProStateMachine.this.mCurrentSsid);
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseBgAcDiffType(diffType);
                }
                if (this.oldType == newType) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseActiveCheckRsSame();
                }
            }
        }

        public void enter() {
            WifiProStateMachine.this.logI("WifiConnectedState is Enter");
            WifiProStateMachine.this.mWifiProUIDisplayManager.shownAccessNotification(false);
            WifiProStateMachine.this.refreshConnectedNetWork();
            this.oldType = WifiProStateMachine.this.mWiFiProEvaluateController.getOldNetworkType(WifiProStateMachine.this.mCurrentSsid);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("WiFiProConnected oldType = " + this.oldType);
            NetworkInfo wifiInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(1);
            if (wifiInfo != null && wifiInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
                WifiProStateMachine.this.logI(" POOR_LINK_DETECTED sendMessageDelayed");
                WifiProStateMachine.this.mWsmChannel.sendMessage(131874);
            }
            if (WifiProStateMachine.this.mNetworkBlackListManager.isInTempWifiBlackList(WifiProStateMachine.this.mWifiManager.getConnectionInfo().getBSSID())) {
                WifiProStateMachine.this.logI("cleanTempBlackList for this bssid.");
                WifiProStateMachine.this.mNetworkBlackListManager.cleanTempWifiBlackList();
            }
            if (!WifiProStateMachine.this.mPhoneStateListenerRegisted) {
                WifiProStateMachine.this.logI("start PhoneStateListener");
                WifiProStateMachine.this.mTelephonyManager.listen(WifiProStateMachine.this.phoneStateListener, 32);
                boolean unused = WifiProStateMachine.this.mPhoneStateListenerRegisted = true;
            }
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PROCESS_GRS, 500);
            initConnectedState();
        }

        public void exit() {
            WifiProStateMachine.this.logI("WifiConnectedState is Exit");
            this.isToastDisplayed = false;
            this.isChrShouldReport = false;
            this.oldType = 0;
            boolean unused = WifiProStateMachine.this.mVerfyingToConnectedState = false;
            WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
            cancelPortalExpiredNotifyStatusBar();
            String unused2 = WifiProStateMachine.this.respCodeChrInfo = "";
            int unused3 = WifiProStateMachine.this.detectionNumSlow = 0;
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHR_ALARM_EXPIRED);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                    WifiProStateMachine.this.handleWifiNetworkStateChange(msg);
                    break;
                case WifiProStateMachine.EVENT_DEVICE_SCREEN_ON /*{ENCODED_INT: 136170}*/:
                case WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION /*{ENCODED_INT: 136186}*/:
                case WifiProStateMachine.EVENT_EMUI_CSP_SETTINGS_CHANGE /*{ENCODED_INT: 136190}*/:
                    if (WifiProStateMachine.this.mCurrentWifiConfig != null && WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoHandoverNetwork && WifiProStateMachine.this.mIsWiFiNoInternet && WifiProStateMachine.this.isAllowWifi2Mobile()) {
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mWiFiNoInternetReason, false);
                        break;
                    }
                case WifiProStateMachine.EVENT_CHECK_AVAILABLE_AP_RESULT /*{ENCODED_INT: 136176}*/:
                    handleCheckAvailableApResult(msg);
                    break;
                case WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE /*{ENCODED_INT: 136177}*/:
                    break;
                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT /*{ENCODED_INT: 136181}*/:
                    handleCheckWifiInternetResultWithConnected(msg);
                    break;
                case WifiProStateMachine.EVENT_DIALOG_OK /*{ENCODED_INT: 136182}*/:
                    handleUserSelectDialogOk();
                    break;
                case WifiProStateMachine.EVENT_DIALOG_CANCEL /*{ENCODED_INT: 136183}*/:
                    handleDialogCancel();
                    break;
                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET /*{ENCODED_INT: 136192}*/:
                    WifiProStateMachine.this.handleCheckWifiInternet();
                    break;
                case WifiProStateMachine.EVENT_HTTP_REACHABLE_RESULT /*{ENCODED_INT: 136195}*/:
                    handleHttpResult(msg);
                    break;
                case WifiProStateMachine.EVENT_NETWORK_USER_CONNECT /*{ENCODED_INT: 136202}*/:
                    if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                        boolean unused = WifiProStateMachine.this.mIsUserManualConnectSuccess = true;
                        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                        wifiProStateMachine.logI("receive EVENT_NETWORK_USER_CONNECT, set mIsUserManualConnectSuccess = " + WifiProStateMachine.this.mIsUserManualConnectSuccess);
                        break;
                    }
                case WifiProStateMachine.EVENT_WIFI_SEMIAUTO_EVALUTE_CHANGE /*{ENCODED_INT: 136300}*/:
                    if (WifiProStateMachine.this.isAllowWiFiAutoEvaluate()) {
                        WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                        break;
                    }
                    break;
                case WifiProStateMachine.EVENT_WIFI_CHECK_UNKOWN /*{ENCODED_INT: 136309}*/:
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
                    break;
                case WifiProStateMachine.EVENT_GET_WIFI_TCPRX /*{ENCODED_INT: 136311}*/:
                    WifiProStateMachine.this.handleGetWifiTcpRx();
                    break;
                case WifiProStateMachine.EVENT_WIFI_NO_INTERNET_NOTIFICATION /*{ENCODED_INT: 136318}*/:
                    WifiProStateMachine.this.handleNotidication();
                    break;
                case WifiProStateMachine.EVENT_PORTAL_SELECTED /*{ENCODED_INT: 136319}*/:
                    handlePortalSelected();
                    break;
                case WifiProStateMachine.EVENT_LAUNCH_BROWSER /*{ENCODED_INT: 136320}*/:
                    HwAutoConnectManager hwAutoConnectManager = HwAutoConnectManager.getInstance();
                    if (hwAutoConnectManager != null) {
                        hwAutoConnectManager.launchBrowserForPortalLogin(WifiProStateMachine.this.mCurrentWifiConfig.configKey());
                        break;
                    }
                    break;
                case WifiProStateMachine.EVENT_CHR_ALARM_EXPIRED /*{ENCODED_INT: 136321}*/:
                    WifiProStateMachine.this.logI("alarm expired, upload CHR");
                    if (!this.isPortalChrEverUploaded) {
                        WifiProStateMachine.this.uploadPortalAuthExpirationStatistics(false);
                        this.isPortalChrEverUploaded = true;
                        break;
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }

        private void handleHttpResult(Message msg) {
            if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                this.internetFailureDetectedCount = 0;
                WifiProStateMachine.this.logI("EVENT_HTTP_REACHABLE_RESULT, SCE notify WLAN+ to check wifi immediately.");
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
            } else if (msg.obj != null && !((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine.this.logI("EVENT_HTTP_REACHABLE_RESULT, SCE notify WLAN+ the http unreachable.");
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT, -1);
            }
        }

        private void handlePortalSelected() {
            WifiProStateMachine.this.logI("###MSG_PORTAL_SELECTED");
            if (WifiProStateMachine.this.mCurrentWifiConfig != null) {
                if (!this.isPortalChrEverUploaded) {
                    WifiProStateMachine.this.logI("user clicks the notification, upload CHR");
                    WifiProStateMachine.this.uploadPortalAuthExpirationStatistics(true);
                    this.isPortalChrEverUploaded = true;
                }
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHR_ALARM_EXPIRED);
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_LAUNCH_BROWSER, 500);
            }
        }

        private void cancelPortalExpiredNotifyStatusBar() {
            if (WifiProStateMachine.this.mPortalNotificationId != -1 && WifiProStateMachine.this.mCurrentWifiConfig != null) {
                Settings.Global.putInt(WifiProStateMachine.this.mContext.getContentResolver(), "captive_portal_notification_shown", 0);
                WifiProStateMachine.this.logI("portal notification is dismissed, change CAPTIVE_PORTAL_NOTIFICATION_SHOWN to 0");
                WifiProStateMachine.this.mWifiProUIDisplayManager.cancelPortalNotificationStatusBar(WifiProStateMachine.PORTAL_STATUS_BAR_TAG, WifiProStateMachine.this.mPortalNotificationId);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.notifyPortalStatusChanged(false, wifiProStateMachine.mCurrentWifiConfig.configKey(), WifiProStateMachine.this.mCurrentWifiConfig.lastHasInternetTimestamp > 0);
                int unused = WifiProStateMachine.this.mPortalNotificationId = -1;
            }
        }

        private boolean isContinueToKeepConnected(int internetQos) {
            if (!this.isKeepConnected || WifiProStateMachine.this.mIsUserHandoverWiFi) {
                return false;
            }
            if (internetQos != -1 && internetQos != 6) {
                WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, false, 0, false);
                return true;
            } else if (WifiProStateMachine.this.mWiFiNoInternetReason == 0) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                int unused = wifiProStateMachine.mWifiTcpRxCount = wifiProStateMachine.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                    WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                }
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                return true;
            } else {
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
                return true;
            }
        }

        private void handleCheckWifiInternetResultWithConnected(Message msg) {
            WifiProStateMachine.this.logI("WiFi internet check level = " + msg.arg1 + ", isKeepConnected = " + this.isKeepConnected + ", mIsUserHandoverWiFi = " + WifiProStateMachine.this.mIsUserHandoverWiFi);
            WifiProStateMachine.this.notifyNetworkCheckResult(msg.arg1);
            reportDiffTypeCHR(WifiProStateMachine.this.mWiFiProEvaluateController.getNewNetworkType(msg.arg1));
            if (isContinueToKeepConnected(msg.arg1)) {
                WifiProStateMachine.this.logI("continue to keep connected");
                return;
            }
            this.isKeepConnected = false;
            int internetLevel = msg.arg1;
            if (!WifiProStateMachine.this.isDialogUpWhenConnected || !(internetLevel == -1 || internetLevel == 6)) {
                if (this.isPortalAP) {
                    if (WifiProStateMachine.this.mPowerManager.isScreenOn()) {
                        this.portalCheckCounter++;
                    }
                    WifiProStateMachine.this.logI("portalCheckCounter = " + this.portalCheckCounter);
                    WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                    if (internetLevel == 6 || internetLevel == -1) {
                        if (internetLevel == -1) {
                            internetFailureSelfcure();
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 15000);
                        return;
                    }
                }
                if (internetLevel == -1) {
                    WifiProStateMachine.this.logI("WiFi NO internet,isPortalAP = " + this.isPortalAP);
                    if (!WifiProStateMachine.this.mIsWiFiInternetCHRFlag && !this.isPortalAP) {
                        WifiProStateMachine.this.logI("upload WIFI_ACCESS_INTERNET_FAILED event for FIRST_CONNECT_NO_INTERNET,ssid:" + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mCurrentSsid));
                        Bundle data = new Bundle();
                        data.putInt(WifiProStateMachine.EVENT_ID, 87);
                        data.putString(WifiProStateMachine.EVENT_DATA, "FIRST_CONNECT_NO_INTERNET");
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 4, data);
                    }
                    boolean unused = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = true;
                    boolean unused2 = WifiProStateMachine.this.mIsWiFiNoInternet = true;
                    WifiProStateMachine.this.updateWifiInternetStateChange(-1);
                    if (WifiProStateMachine.this.mIsUserManualConnectSuccess) {
                        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                        int unused3 = wifiProStateMachine.mWifiTcpRxCount = wifiProStateMachine.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                        if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                        return;
                    }
                    if (this.isPortalAP) {
                        this.isPortalAP = false;
                        int unused4 = WifiProStateMachine.this.mWiFiNoInternetReason = 1;
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 3);
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 3, WifiProStateMachine.this.mCurrentSsid);
                    } else {
                        WifiProStateMachine.this.mWifiProUIDisplayManager.notificateNetAccessChange(true);
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 2);
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 2, WifiProStateMachine.this.mCurrentSsid);
                        int unused5 = WifiProStateMachine.this.mWiFiNoInternetReason = 0;
                        WifiProStateMachine.this.mWifiProStatisticsManager.increaseNoInetRemindCount(true);
                    }
                    if (this.isIgnorAvailableWifiCheck) {
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mWiFiNoInternetReason, false);
                    } else if (WifiProStateMachine.this.mCurrentWifiConfig != null) {
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mWiFiNoInternetReason, WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoHandoverNetwork);
                    }
                    if (this.isIgnorAvailableWifiCheck) {
                        HwWifiProFeatureControl.getInstance();
                        if (!HwWifiProFeatureControl.isSelfCureOngoing() && !WifiProStateMachine.this.isKeepCurrWiFiConnected()) {
                            WifiProStateMachine.this.logI("inquire the surrounding AP for wifiHandover");
                            this.isIgnorAvailableWifiCheck = false;
                            WifiProStateMachine.this.mWifiHandover.hasAvailableWifiNetwork(WifiProStateMachine.this.mNetworkBlackListManager.getWifiBlacklist(), -82, WifiProStateMachine.this.mCurrentBssid, WifiProStateMachine.this.mCurrentSsid);
                            return;
                        }
                    }
                    if (WifiProStateMachine.this.mWiFiNoInternetReason == 0) {
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        int unused6 = wifiProStateMachine2.mWifiTcpRxCount = wifiProStateMachine2.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                        if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                        return;
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
                } else if (internetLevel == 6) {
                    WifiProStateMachine.this.logI("WifiConnectedState: WiFi is protal");
                    this.isPortalAP = true;
                    WifiProStateMachine.this.setWifiMonitorEnabled(true);
                    boolean unused7 = WifiProStateMachine.this.mIsPortalAp = true;
                    boolean unused8 = WifiProStateMachine.this.mIsNetworkAuthen = false;
                    int unused9 = WifiProStateMachine.this.mWiFiNoInternetReason = 1;
                    WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 3);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 3, WifiProStateMachine.this.mCurrentSsid);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, true, WifiProStateMachine.this.mWiFiNoInternetReason, false);
                    if (WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp != 0) {
                        WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp = 0;
                        WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                        wifiProStateMachine3.updateWifiConfig(wifiProStateMachine3.mCurrentWifiConfig);
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 15000);
                } else {
                    Bundle recordData = new Bundle();
                    recordData.putInt("reason", 0);
                    recordData.putBoolean(WifiProStateMachine.ACCESS_WEB_RECORD_SUCC, true);
                    recordData.putBoolean(WifiProStateMachine.ACCESS_WEB_RECORD_PORTAL, this.isPortalAP);
                    WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 32, recordData);
                    this.isKeepConnected = false;
                    boolean unused10 = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                    boolean unused11 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = false;
                    boolean unused12 = WifiProStateMachine.this.mIsNetworkAuthen = true;
                    WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 4);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 4, WifiProStateMachine.this.mCurrentSsid);
                    Bundle data2 = new Bundle();
                    data2.putBoolean(HwWifiCHRHilink.WEB_DELAY_NEEDUPLOAD, true);
                    WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 41, data2);
                    if (this.isPortalAP) {
                        notifyPortalHasInternetAccess();
                    }
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    wifiProStateMachine4.transitionTo(wifiProStateMachine4.mWiFiLinkMonitorState);
                }
            } else {
                WifiProStateMachine.this.logI("AP is noInternet or Protal AP , Continue DisplayDialog");
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
            }
        }

        private void notifyPortalHasInternetAccess() {
            if (isProvisioned(WifiProStateMachine.this.mContext)) {
                WifiProStateMachine.this.logI("portal has internet access, force network re-evaluation");
                ConnectivityManager connMgr = ConnectivityManager.from(WifiProStateMachine.this.mContext);
                if (connMgr == null) {
                    WifiProStateMachine.this.logE("notifyPortalHasInternetAccess connMgr is null");
                    return;
                }
                Network[] info = connMgr.getAllNetworks();
                int length = info.length;
                int i = 0;
                while (i < length) {
                    Network nw = info[i];
                    NetworkCapabilities nc = connMgr.getNetworkCapabilities(nw);
                    if (!nc.hasTransport(1) || !nc.hasCapability(12)) {
                        i++;
                    } else {
                        WifiProStateMachine.this.logI("Network has capability");
                        connMgr.reportNetworkConnectivity(nw, false);
                        return;
                    }
                }
            }
        }

        private boolean isProvisioned(Context context) {
            return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1;
        }

        private void handleCheckAvailableApResult(Message msg) {
            if (!this.isIgnorAvailableWifiCheck) {
                if (WifiProStateMachine.this.mIsWiFiNoInternet && ((Boolean) msg.obj).booleanValue()) {
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mWiFiNoInternetReason, false);
                    if (1 != WifiProStateMachine.this.mWiFiNoInternetReason) {
                        WifiProStateMachine.this.logI("AllowWifi2Wifi, transitionTo mWiFiLinkMonitorState");
                        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                        wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiLinkMonitorState);
                    }
                } else if (!this.isToastDisplayed) {
                    WifiProStateMachine.this.logW("There is no network can switch");
                    if (!WifiProStateMachine.this.mIsUserManualConnectSuccess && WifiProStateMachine.this.mIsWiFiNoInternet && !this.isPortalAP && !WifiProStateMachine.this.isKeepCurrWiFiConnected()) {
                        WifiProStateMachine.this.logI("try to switch cell!! directly");
                        WifiProStateMachine.this.mWsmChannel.sendMessage((int) WifiProStateMachine.INVALID_LINK_DETECTED);
                    }
                    this.isToastDisplayed = true;
                    WifiProStateMachine.this.mWifiProUIDisplayManager.showWifiProToast(3);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(WifiProStateMachine.this.mCurrentWifiConfig, WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mWiFiNoInternetReason, true);
                    if (WifiProStateMachine.this.mWiFiNoInternetReason == 0) {
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        int unused = wifiProStateMachine2.mWifiTcpRxCount = wifiProStateMachine2.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                        if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                        return;
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 60000);
                }
            }
        }

        private void handleUserSelectDialogOk() {
            WifiProStateMachine.this.logI("Intelligent choice other network,go to mWiFiLinkMonitorState");
            boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = true;
            boolean unused2 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = true;
            this.isKeepConnected = false;
            boolean unused3 = WifiProStateMachine.this.mIsUserManualConnectSuccess = false;
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiLinkMonitorState);
        }

        private boolean isHiLinkUnconfigRouter() {
            Bundle data = new Bundle();
            data.putString("CurrentSsid", WifiProStateMachine.this.mCurrentSsid);
            data.putString("CurrentBssid", WifiProStateMachine.this.mCurrentBssid);
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 34, data);
            if (result != null) {
                return result.getBoolean("isHiLinkUnconfigRouter");
            }
            return false;
        }

        private void handleDialogCancel() {
            WifiProStateMachine.this.logI("Keep this network,do nothing!!!");
            this.isIgnorAvailableWifiCheck = true;
            WifiProStateMachine.this.mNetworkQosMonitor.stopBqeService();
            this.isKeepConnected = true;
            if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                if (WifiProStateMachine.this.mWiFiNoInternetReason == 0) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    int unused = wifiProStateMachine.mWifiTcpRxCount = wifiProStateMachine.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                    if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                } else {
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
                }
            }
            if (WifiProStateMachine.this.isDialogUpWhenConnected && WifiProStateMachine.this.mIsWiFiNoInternet) {
                WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetUserCancelCount();
            }
        }

        private void internetFailureSelfcure() {
            HwWifiProFeatureControl.getInstance();
            if (!HwWifiProFeatureControl.isSelfCureOngoing() && this.internetFailureDetectedCount == 0 && WifiProStateMachine.this.isWifiConnected()) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                int unused = wifiProStateMachine.mCurrentRssi = WifiProCommonUtils.getCurrentRssi(wifiProStateMachine.mWifiManager);
                WifiProStateMachine.this.logI("internetFailureSelfcure mCurrentRssi = " + WifiProStateMachine.this.mCurrentRssi);
                if (WifiProStateMachine.this.mCurrentRssi >= -70) {
                    HwWifiProFeatureControl.getInstance();
                    HwWifiProFeatureControl.notifyInternetFailureDetected(true);
                    this.internetFailureDetectedCount++;
                    boolean unused2 = WifiProStateMachine.this.mVerfyingToConnectedState = false;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleCheckWifiInternet() {
        int result;
        if (this.mPowerManager.isScreenOn()) {
            this.mNetworkQosMonitor.queryNetworkQos(1, this.mIsPortalAp, this.mIsNetworkAuthen, false);
            return;
        }
        logI("Screen off, cancel network check! mIsPortalAp " + this.mIsPortalAp);
        if (this.mIsPortalAp) {
            result = 6;
        } else {
            result = -1;
        }
        sendMessage(EVENT_CHECK_WIFI_INTERNET_RESULT, result);
    }

    /* access modifiers changed from: private */
    public void handleNotidication() {
        this.mWifiProConfigStore.updateWifiNoInternetAccessConfig(this.mCurrentWifiConfig, true, 0, false);
        this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, 2, this.mCurrentSsid);
        this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, 2);
        this.mWifiProUIDisplayManager.notificateNetAccessChange(true);
    }

    /* access modifiers changed from: private */
    public void handleWifiNetworkStateChange(Message msg) {
        if (msg.obj instanceof Intent) {
            Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
            NetworkInfo networkInfo = null;
            if (objNetworkInfo instanceof NetworkInfo) {
                networkInfo = (NetworkInfo) objNetworkInfo;
            }
            if (networkInfo != null && NetworkInfo.State.DISCONNECTED == networkInfo.getState()) {
                transitionTo(this.mWifiDisConnectedState);
                return;
            }
            return;
        }
        logE("handleWifiNetworkStateChange: msg.obj is null or not intent");
    }

    /* access modifiers changed from: private */
    public void initWifiConfig() {
        WifiConfiguration cfg = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (cfg != null) {
            int accessType = cfg.internetAccessType;
            int qosLevel = cfg.networkQosLevel;
            logI("accessType = : " + accessType + ",qosLevel = " + qosLevel + ",wifiProNoInternetAccess = " + cfg.wifiProNoInternetAccess);
            if (cfg.isTempCreated) {
                this.mWifiProStatisticsManager.updateBGChrStatistic(19);
            }
            resetWifiEvaluteInternetType();
            this.mWifiProUIDisplayManager.notificateNetAccessChange(false);
        } else {
            logE("cfg= null ");
        }
        if (isAllowWiFiAutoEvaluate()) {
            this.mWiFiProEvaluateController.addEvaluateRecords(this.mCurrWifiInfo, 1);
        }
    }

    /* access modifiers changed from: package-private */
    public class WiFiLinkMonitorState extends State {
        private int currWifiPoorlevel;
        private int detectCounter = 0;
        private int internetFailureDetectedCnt;
        private boolean isAllowWiFiHandoverMobile;
        private boolean isBQERequestCheckWiFi;
        private boolean isCancelCHRTypeReport;
        private boolean isCheckWiFiForUpdateSetting;
        private boolean isDialogDisplayed;
        private boolean isDisableWifiAutoSwitch = false;
        private boolean isNoInternetDialogShowing;
        private boolean isNotifyInvalidLinkDetection = false;
        private boolean isRequestWifInetCheck = false;
        private boolean isRssiLowOrMiddleWifi2Wifi = false;
        private boolean isScreenOffMonitor;
        private boolean isSwitching;
        private boolean isToastDisplayed;
        private boolean isWiFiHandoverPriority;
        private boolean isWifi2MobileUIShowing;
        private boolean isWifi2WifiProcess;
        private boolean isWifiHandoverMobileToastShowed = false;
        private int mLastUpdatedQosLevel = 0;
        private int mWifi2WifiThreshod = WifiHandover.INVALID_RSSI;
        private boolean portalCheck = false;
        private int rssiLevel0Or1ScanedCounter = 0;
        private int rssiLevel2ScanedCounter = 0;
        private long wifiLinkHoldTime;
        private int wifiMonitorCounter;

        WiFiLinkMonitorState() {
        }

        private void wiFiLinkMonitorStateInit(boolean internetRecheck) {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("wiFiLinkMonitorStateInit is Start, internetRecheck = " + internetRecheck);
            String unused = WifiProStateMachine.this.mBadBssid = null;
            int unused2 = WifiProStateMachine.this.mHandoverFailReason = -1;
            int unused3 = WifiProStateMachine.this.mLastHandoverFailReason = -1;
            this.isSwitching = false;
            this.isWifi2WifiProcess = false;
            this.isRssiLowOrMiddleWifi2Wifi = false;
            this.isWifi2MobileUIShowing = false;
            this.isCheckWiFiForUpdateSetting = false;
            this.isDialogDisplayed = false;
            this.isNoInternetDialogShowing = false;
            this.detectCounter = 0;
            WifiProStateMachine.this.setWifiCSPState(1);
            this.mLastUpdatedQosLevel = 0;
            if (!internetRecheck) {
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.logI("mIsWiFiNoInternet is true,sendMessage wifi Qos is -1");
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_QOS_CHANGE, -1, 0, false);
                } else {
                    HwWifiProFeatureControl.getInstance();
                    HwWifiProFeatureControl.notifyInternetAccessRecovery();
                    WifiProStateMachine.this.setWifiMonitorEnabled(true);
                }
            }
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
            boolean unused4 = WifiProStateMachine.this.mNeedRetryMonitor = false;
        }

        public void enter() {
            WifiProStateMachine.this.logI("WiFiLinkMonitorState is Enter");
            NetworkInfo wifiInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(1);
            if (wifiInfo != null && wifiInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
                WifiProStateMachine.this.logI(" POOR_LINK_DETECTED sendMessageDelayed");
                WifiProStateMachine.this.mWsmChannel.sendMessage(131874);
            }
            if (WifiProStateMachine.this.isAllowWiFiAutoEvaluate()) {
                updateWifiQosLevel(WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mNetworkQosMonitor.getCurrentWiFiLevel());
            }
            this.wifiMonitorCounter = 0;
            this.internetFailureDetectedCnt = 0;
            this.rssiLevel2ScanedCounter = 0;
            this.rssiLevel0Or1ScanedCounter = 0;
            this.isScreenOffMonitor = false;
            this.isAllowWiFiHandoverMobile = true;
            this.isCancelCHRTypeReport = false;
            this.isDisableWifiAutoSwitch = false;
            this.isRequestWifInetCheck = false;
            this.isNotifyInvalidLinkDetection = false;
            boolean unused = WifiProStateMachine.this.isVerifyWifiNoInternetTimeOut = false;
            boolean unused2 = WifiProStateMachine.this.hasHandledNoInternetResult = false;
            wiFiLinkMonitorStateInit(false);
            this.currWifiPoorlevel = 3;
            this.wifiLinkHoldTime = System.currentTimeMillis();
            if (0 != WifiProStateMachine.this.mChrRoveOutStartTime && (WifiProStateMachine.this.mChrWifiDisconnectStartTime > WifiProStateMachine.this.mChrRoveOutStartTime || WifiProStateMachine.this.mChrWifiDidableStartTime > WifiProStateMachine.this.mChrRoveOutStartTime)) {
                long disableRestoreTime = System.currentTimeMillis() - WifiProStateMachine.this.mChrWifiDidableStartTime;
                boolean ssidIsSame = false;
                if (!(WifiProStateMachine.this.mRoSsid == null || WifiProStateMachine.this.mCurrentSsid == null)) {
                    ssidIsSame = WifiProStateMachine.this.mRoSsid.equals(WifiProStateMachine.this.mCurrentSsid);
                }
                if (ssidIsSame && disableRestoreTime <= 30000) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseUserReopenWifiRiCount();
                }
            }
            long unused3 = WifiProStateMachine.this.mChrRoveOutStartTime = 0;
            long unused4 = WifiProStateMachine.this.mChrWifiDisconnectStartTime = 0;
            long unused5 = WifiProStateMachine.this.mChrWifiDidableStartTime = 0;
            if (WifiProStateMachine.this.mCurrentWifiConfig.portalNetwork) {
                this.portalCheck = true;
                String unused6 = WifiProStateMachine.this.respCodeChrInfo = "";
                int unused7 = WifiProStateMachine.this.detectionNumSlow = 0;
                long unused8 = WifiProStateMachine.this.connectStartTime = System.currentTimeMillis();
                if (WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp == 0) {
                    WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp = System.currentTimeMillis();
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.updateWifiConfig(wifiProStateMachine.mCurrentWifiConfig);
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.logI("periodic portal check: update portalAuthTimestamp =" + WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp);
                }
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
            }
        }

        public void exit() {
            WifiProStateMachine.this.logI("WiFiLinkMonitorState is Exit");
            WifiProStateMachine.this.mNetworkQosMonitor.stopBqeService();
            WifiProStateMachine.this.setWifiMonitorEnabled(false);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_DUALBAND_DELAY_RETRY);
            WifiProStateMachine.this.stopDualBandMonitor();
            this.detectCounter = 0;
            this.portalCheck = false;
            this.isToastDisplayed = false;
            this.isDialogDisplayed = false;
            boolean unused = WifiProStateMachine.this.mDelayedRssiChangedByFullScreen = false;
            boolean unused2 = WifiProStateMachine.this.mDelayedRssiChangedByCalling = false;
            this.isWiFiHandoverPriority = false;
            this.isWifiHandoverMobileToastShowed = false;
            if (System.currentTimeMillis() - this.wifiLinkHoldTime > 1800000) {
                int unused3 = WifiProStateMachine.this.mCurrentVerfyCounter = 0;
            }
            if (System.currentTimeMillis() - this.wifiLinkHoldTime >= 60000) {
                long unused4 = WifiProStateMachine.this.mLastTime = 0;
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            switch (i) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                    handleWifiNetworkStateChange(msg);
                    break;
                case WifiProStateMachine.EVENT_DEVICE_SCREEN_ON /*{ENCODED_INT: 136170}*/:
                    if (!this.isScreenOffMonitor) {
                        WifiProStateMachine.this.logI("device screen on,but isScreenOffMonitor is false");
                        break;
                    } else {
                        WifiProStateMachine.this.logI("device screen on,reinitialize wifi monitor");
                        this.isScreenOffMonitor = false;
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR);
                        break;
                    }
                default:
                    switch (i) {
                        case WifiProStateMachine.EVENT_WIFI_QOS_CHANGE /*{ENCODED_INT: 136172}*/:
                            handleWifiQosChangedInLinkMonitorState(msg);
                            break;
                        case WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION /*{ENCODED_INT: 136186}*/:
                            handleMobileDataStateChange();
                            break;
                        case WifiProStateMachine.EVENT_CALL_STATE_CHANGED /*{ENCODED_INT: 136201}*/:
                            handleCallStateChanged(msg);
                            break;
                        case WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_STATUS /*{ENCODED_INT: 136210}*/:
                            checkWifiNetworkStatus();
                            break;
                        case WifiProStateMachine.EVENT_GET_WIFI_TCPRX /*{ENCODED_INT: 136311}*/:
                            WifiProStateMachine.this.handleGetWifiTcpRx();
                            break;
                        default:
                            switch (i) {
                                case WifiProStateMachine.EVENT_CHECK_AVAILABLE_AP_RESULT /*{ENCODED_INT: 136176}*/:
                                    handleCheckResultInLinkMonitorState(msg);
                                    break;
                                case WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE /*{ENCODED_INT: 136177}*/:
                                    handleNetworkConnectivityChange(msg);
                                    break;
                                case WifiProStateMachine.EVENT_WIFI_HANDOVER_WIFI_RESULT /*{ENCODED_INT: 136178}*/:
                                    handleWifiHandoverResult(msg);
                                    break;
                                case WifiProStateMachine.EVENT_WIFI_RSSI_CHANGE /*{ENCODED_INT: 136179}*/:
                                    handleRssiChangedInLinkMonitorState(msg);
                                    break;
                                case WifiProStateMachine.EVENT_CHECK_MOBILE_QOS_RESULT /*{ENCODED_INT: 136180}*/:
                                    tryWifi2Mobile(msg.arg1);
                                    break;
                                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT /*{ENCODED_INT: 136181}*/:
                                    handleWifiInternetResultInLinkMonitorState(msg);
                                    break;
                                case WifiProStateMachine.EVENT_DIALOG_OK /*{ENCODED_INT: 136182}*/:
                                    handleEventDialog(msg);
                                    break;
                                case WifiProStateMachine.EVENT_DIALOG_CANCEL /*{ENCODED_INT: 136183}*/:
                                    handleDilaogCancel(msg);
                                    break;
                                case WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR /*{ENCODED_INT: 136184}*/:
                                    handleDelayResetWifi();
                                    break;
                                default:
                                    switch (i) {
                                        case WifiProStateMachine.EVENT_EMUI_CSP_SETTINGS_CHANGE /*{ENCODED_INT: 136190}*/:
                                            handleEmuiCspSettingChange();
                                            break;
                                        case WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI /*{ENCODED_INT: 136191}*/:
                                            retryWifi2Wifi();
                                            break;
                                        case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET /*{ENCODED_INT: 136192}*/:
                                            handleCheckWifiInternet();
                                            break;
                                        default:
                                            switch (i) {
                                                case WifiProStateMachine.EVENT_HTTP_REACHABLE_RESULT /*{ENCODED_INT: 136195}*/:
                                                    handleHttpReachableResult(msg);
                                                    break;
                                                case WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY /*{ENCODED_INT: 136196}*/:
                                                    handleReuqestScanInLinkMonitorState(msg);
                                                    break;
                                                case WifiProStateMachine.EVENT_CONFIGURATION_CHANGED /*{ENCODED_INT: 136197}*/:
                                                    handleOrientationChanged(msg);
                                                    break;
                                                case WifiProStateMachine.EVENT_NOTIFY_WIFI_LINK_POOR /*{ENCODED_INT: 136198}*/:
                                                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                                                    wifiProStateMachine.logI("EVENT_NOTIFY_WIFI_LINK_POOR isDisableWifiAutoSwitch = " + this.isDisableWifiAutoSwitch);
                                                    if (!this.isDisableWifiAutoSwitch) {
                                                        int unused = WifiProStateMachine.this.mCurrentWifiLevel = 0;
                                                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_QOS_CHANGE, 0, 0, false);
                                                        break;
                                                    }
                                                    break;
                                                case WifiProStateMachine.EVENT_TRY_WIFI_ROVE_OUT /*{ENCODED_INT: 136199}*/:
                                                    handleWiFiRoveOut();
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW /*{ENCODED_INT: 136204}*/:
                                                            if (WifiProStateMachine.this.mPowerManager.isScreenOn()) {
                                                                WifiProStateMachine.this.handlePeriodPortalCheck();
                                                                break;
                                                            }
                                                            break;
                                                        case WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST /*{ENCODED_INT: 136205}*/:
                                                            if (HwAutoConnectManager.getInstance() != null) {
                                                                HwAutoConnectManager.getInstance().checkPortalAuthExpiration();
                                                                break;
                                                            }
                                                            break;
                                                        case WifiProStateMachine.EVENT_DEVICE_SCREEN_OFF /*{ENCODED_INT: 136206}*/:
                                                            if (this.portalCheck) {
                                                                WifiProStateMachine.this.logI("periodic portal check: screen off, remove msg");
                                                                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST);
                                                                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW);
                                                                break;
                                                            }
                                                            break;
                                                        case WifiProStateMachine.EVENT_DEVICE_USER_PRESENT /*{ENCODED_INT: 136207}*/:
                                                            handleDeviceUserPresent();
                                                            break;
                                                        case WifiProStateMachine.EVENT_CHECK_PORTAL_AUTH_CHECK_RESULT /*{ENCODED_INT: 136208}*/:
                                                            handlePortalAuthCheckResultInLinkMonitorState(msg);
                                                            break;
                                                        default:
                                                            switch (i) {
                                                                case WifiProStateMachine.EVENT_WIFI_EVALUTE_TCPRTT_RESULT /*{ENCODED_INT: 136299}*/:
                                                                    handleEvaluteResult(msg);
                                                                    break;
                                                                case WifiProStateMachine.EVENT_WIFI_SEMIAUTO_EVALUTE_CHANGE /*{ENCODED_INT: 136300}*/:
                                                                    WifiProStateMachine.this.handleWifiEvaluteChange();
                                                                    break;
                                                                default:
                                                                    switch (i) {
                                                                        case WifiProStateMachine.EVENT_DUALBAND_RSSITH_RESULT /*{ENCODED_INT: 136368}*/:
                                                                            handleDualbandRssithResult(msg);
                                                                            break;
                                                                        case WifiProStateMachine.EVENT_DUALBAND_SCORE_RESULT /*{ENCODED_INT: 136369}*/:
                                                                            handleDualbandScoreResult(msg);
                                                                            break;
                                                                        case WifiProStateMachine.EVENT_DUALBAND_5GAP_AVAILABLE /*{ENCODED_INT: 136370}*/:
                                                                            handleDualbandApAvailable();
                                                                            break;
                                                                        case WifiProStateMachine.EVENT_DUALBAND_WIFI_HANDOVER_RESULT /*{ENCODED_INT: 136371}*/:
                                                                            handleDualbandHandoverResult(msg);
                                                                            break;
                                                                        case WifiProStateMachine.EVENT_DUALBAND_DELAY_RETRY /*{ENCODED_INT: 136372}*/:
                                                                            WifiProStateMachine.this.logI("receive dual band wifi handover delay retry");
                                                                            WifiProStateMachine.this.retryDualBandAPMonitor();
                                                                            break;
                                                                        default:
                                                                            return false;
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
            return true;
        }

        private void handleHttpReachableResult(Message msg) {
            if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                this.internetFailureDetectedCnt = 0;
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.reportNetworkConnectivity(true);
                }
                boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                boolean unused2 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = false;
                WifiProStateMachine.this.onNetworkDetectionResult(1, 5);
            } else if (msg.obj != null && !((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine.this.logI("EVENT_HTTP_REACHABLE_RESULT = false, SCE notify WLAN+.");
                this.isNotifyInvalidLinkDetection = true;
                if (WifiProStateMachine.this.mIsUserManualConnectSuccess && !WifiProStateMachine.this.mIsWiFiProEnabled) {
                    WifiProStateMachine.this.updateChrToCell(false);
                    WifiProStateMachine.this.mWsmChannel.sendMessage((int) WifiProStateMachine.INVALID_LINK_DETECTED);
                }
                WifiProStateMachine.this.updateWifiInternetStateChange(-1);
                boolean unused3 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = true;
                boolean unused4 = WifiProStateMachine.this.mIsWiFiNoInternet = true;
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_QOS_CHANGE, 0, 0, false);
            }
        }

        private void handleEvaluteResult(Message msg) {
            if (!WifiProStateMachine.this.mIsWiFiNoInternet) {
                int rttlevel = msg.arg1;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI(StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mCurrentSsid) + "  TCPRTT  level = " + rttlevel);
                if (rttlevel <= 0 || rttlevel > 3) {
                    rttlevel = 0;
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(23);
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(WifiProStateMachine.this.mCurrentSsid);
                }
                updateWifiQosLevel(false, rttlevel);
            }
        }

        private void handleDilaogCancel(Message msg) {
            if (msg.arg1 == 101) {
                WifiProStateMachine.this.logI("WiFiLinkMonitorState::Click CANCEL ,User don't want wifi switch.");
                this.isDisableWifiAutoSwitch = true;
                this.isNoInternetDialogShowing = false;
            } else if (this.isWifi2MobileUIShowing) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("isDialogDisplayed : " + this.isDialogDisplayed + ", mIsWiFiNoInternet " + WifiProStateMachine.this.mIsWiFiNoInternet);
                if (this.isDialogDisplayed) {
                    if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                        WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetUserCancelCount();
                    } else {
                        WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(2);
                    }
                } else if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetSettingCancelCount();
                } else {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseBqeBadSettingCancelCount();
                }
                this.isDialogDisplayed = false;
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                this.isWifi2MobileUIShowing = false;
                this.isAllowWiFiHandoverMobile = false;
                WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
                this.isSwitching = false;
                int unused = WifiProStateMachine.this.mWiFiProPdpSwichValue = 2;
                WifiProStateMachine.this.logI("Click Cancel ,is not allow wifi handover mobile, WiFiProPdp is CANNOT");
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    this.isCheckWiFiForUpdateSetting = true;
                    if (WifiProStateMachine.this.mCurrentWifiConfig == null || !WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetAccess || WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetReason != 0) {
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, 30000);
                        return;
                    }
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    int unused2 = wifiProStateMachine2.mWifiTcpRxCount = wifiProStateMachine2.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                    if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                    return;
                }
                WifiProStateMachine.this.setWifiMonitorEnabled(true);
            }
        }

        private void handleEventDialog(Message msg) {
            int roReason;
            if (msg.arg1 == 101) {
                WifiProStateMachine.this.logI("WiFiLinkMonitorState::Click OK ,User start wifi switch.");
                this.isDisableWifiAutoSwitch = false;
                this.isNoInternetDialogShowing = false;
                this.isSwitching = true;
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_TRY_WIFI_ROVE_OUT);
            } else if (this.isWifi2MobileUIShowing) {
                this.isDialogDisplayed = false;
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                this.isWifi2MobileUIShowing = false;
                int unused = WifiProStateMachine.this.mWiFiProPdpSwichValue = 1;
                WifiProStateMachine.this.setWifiCSPState(0);
                WifiProStateMachine.this.logI("Click OK ,is send message to wifi handover mobile ,WiFiProPdp is AUTO");
                if (WifiProStateMachine.this.mIsMobileDataEnabled && WifiProStateMachine.this.mPowerManager.isScreenOn() && WifiProStateMachine.this.mEmuiPdpSwichValue != 2) {
                    this.isAllowWiFiHandoverMobile = true;
                    WifiProStateMachine.this.logI("mWsmChannel send Poor Link Detected");
                    WifiProStateMachine.this.mWsmChannel.sendMessage(131873);
                    if (this.currWifiPoorlevel == -1) {
                        roReason = 2;
                        WifiProStateMachine.this.mWifiProStatisticsManager.increaseNoInetHandoverCount();
                    } else {
                        roReason = 1;
                    }
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("roReason = " + roReason);
                    WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveOutEvent(roReason);
                }
                WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
            }
        }

        private void handleDualbandScoreResult(Message msg) {
            if (this.isWifi2WifiProcess) {
                WifiProStateMachine.this.logI("isWifi2WifiProcess is true, ignore this message");
                boolean unused = WifiProStateMachine.this.mNeedRetryMonitor = true;
                return;
            }
            WifiProEstimateApInfo estimateApInfo = null;
            if (msg.obj instanceof WifiProEstimateApInfo) {
                estimateApInfo = (WifiProEstimateApInfo) msg.obj;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("EVENT_DUALBAND_SCORE_RESULT estimateApInfo: " + estimateApInfo.toString());
            } else {
                WifiProStateMachine.this.logE("handleDualbandScoreResult:Class is not match");
            }
            if (WifiProStateMachine.this.mDualBandEstimateInfoSize > 0) {
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                int unused2 = wifiProStateMachine2.mDualBandEstimateInfoSize = wifiProStateMachine2.mDualBandEstimateInfoSize - 1;
                WifiProStateMachine.this.updateDualBandEstimateInfo(estimateApInfo);
            }
            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
            wifiProStateMachine3.logI("mDualBandEstimateInfoSize = " + WifiProStateMachine.this.mDualBandEstimateInfoSize);
            if (WifiProStateMachine.this.mDualBandEstimateInfoSize == 0) {
                WifiProStateMachine.this.chooseAvalibleDualBandAp();
            }
        }

        private void handleDualbandRssithResult(Message msg) {
            if (this.isWifi2WifiProcess) {
                WifiProStateMachine.this.logI("isWifi2WifiProcess is true, ignore this message");
                boolean unused = WifiProStateMachine.this.mNeedRetryMonitor = true;
                return;
            }
            WifiProEstimateApInfo apInfo = null;
            if (msg.obj instanceof WifiProEstimateApInfo) {
                apInfo = (WifiProEstimateApInfo) msg.obj;
            } else {
                WifiProStateMachine.this.logE("handleDualbandRssithResult:Class is not match");
            }
            if (WifiProStateMachine.this.mDualBandMonitorInfoSize > 0) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                int unused2 = wifiProStateMachine.mDualBandMonitorInfoSize = wifiProStateMachine.mDualBandMonitorInfoSize - 1;
                WifiProStateMachine.this.updateDualBandMonitorInfo(apInfo);
            }
            if (WifiProStateMachine.this.mDualBandMonitorInfoSize == 0 && WifiProStateMachine.this.mDualBandManager != null) {
                boolean unused3 = WifiProStateMachine.this.mDualBandMonitorStart = true;
                WifiProStateMachine.this.logI("Start dual band Manager monitor");
                WifiProStateMachine.this.mDualBandManager.startMonitor(WifiProStateMachine.this.mDualBandMonitorApList);
            }
        }

        private void handleWifiHandoverResult(Message msg) {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("receive wifi handover wifi Result,isWifi2WifiProcess = " + this.isWifi2WifiProcess);
            if (!this.isWifi2WifiProcess) {
                WifiProStateMachine.this.logE("isWifi2WifiProcess false, return");
            } else if (((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine.this.logI(" wifi --> wifi is  succeed");
                WifiProChrUploadManager.uploadDisconnectedEvent(WifiProStateMachine.EVENT_SSID_SWITCH_FINISHED);
                if (WifiProStateMachine.this.uploadManager != null) {
                    Bundle ssidSwitchSucc = new Bundle();
                    ssidSwitchSucc.putInt(WifiproUtils.SWITCH_SUCCESS_INDEX, 0);
                    WifiProStateMachine.this.uploadManager.addChrBundleStat(WifiproUtils.WIFI_SWITCH_EVENT, WifiproUtils.WIFI_SWITCH_SUCC_EVENT, ssidSwitchSucc);
                }
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.uploadChrWifiHandoverTypeStatistics(wifiProStateMachine2.mChrWifiHandoverType, WifiProStateMachine.HANDOVER_SUCC_CNT);
                WifiProStateMachine.this.uploadWifiSwitchStatistics();
                long unused = WifiProStateMachine.this.mWifiHandoverSucceedTimestamp = SystemClock.elapsedRealtime();
                this.isSwitching = false;
                boolean unused2 = WifiProStateMachine.this.mIsUserManualConnectSuccess = false;
                WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mBadBssid);
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.addDualBandBlackList(wifiProStateMachine3.mBadBssid);
                WifiProStateMachine.this.resetWifiEvaluteQosLevel();
                WifiProStateMachine.this.refreshConnectedNetWork();
                WifiProStateMachine.this.mWiFiProEvaluateController.reSetEvaluateRecord(WifiProStateMachine.this.mCurrentSsid);
                WifiProStateMachine.this.mWifiProConfigStore.cleanWifiProConfig(WifiProStateMachine.this.mCurrentWifiConfig);
                this.isWifi2WifiProcess = false;
                WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                wifiProStateMachine4.transitionTo(wifiProStateMachine4.mWifiConnectedState);
            } else {
                if (msg.arg1 == 22) {
                    this.isSwitching = false;
                    if (WifiProStateMachine.this.mHandoverFailReason != 7) {
                        int unused3 = WifiProStateMachine.this.mHandoverFailReason = 7;
                        WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                        wifiProStateMachine5.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine5.mHandoverFailReason);
                    }
                    if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                        WifiProStateMachine.this.logI("WiFi2WiFi failed because no candidate, try to trigger selfcure");
                        pendingMsgBySelfCureEngine(WifiproUtils.REQUEST_WIFI_INET_CHECK);
                    }
                }
                wifi2WifiFailed();
            }
        }

        private void handleMobileDataStateChange() {
            WifiProStateMachine.this.logI("WiFiLinkMonitorState : Receive Mobile changed");
            if (WifiProStateMachine.this.isMobileDataConnected() && this.isAllowWiFiHandoverMobile) {
                this.isCheckWiFiForUpdateSetting = false;
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                } else {
                    WifiProStateMachine.this.setWifiMonitorEnabled(true);
                }
            }
        }

        private void handleDelayResetWifi() {
            WifiProStateMachine.this.logI("ReIniitalize,ScreenOn == " + WifiProStateMachine.this.mPowerManager.isScreenOn());
            if (WifiProStateMachine.this.mPowerManager.isScreenOn()) {
                this.wifiMonitorCounter++;
                int i = this.wifiMonitorCounter;
                if (i >= 4) {
                    if (i > 12) {
                        i = 12;
                    }
                    this.wifiMonitorCounter = i;
                    long delayTime = ((long) Math.pow(2.0d, (double) (this.wifiMonitorCounter / 4))) * 60 * 1000;
                    WifiProStateMachine.this.logI("delayTime = " + delayTime);
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI, delayTime);
                    if (WifiProStateMachine.this.mIsWiFiNoInternet && !this.isCheckWiFiForUpdateSetting) {
                        this.isCheckWiFiForUpdateSetting = true;
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
                    }
                    if (WifiProStateMachine.this.mCurrentWifiConfig != null && WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetAccess && WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetReason == 0) {
                        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                        int unused = wifiProStateMachine.mWifiTcpRxCount = wifiProStateMachine.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                        if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                    }
                } else {
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI);
                }
                WifiProStateMachine.this.logI("wifiMonitorCounter = " + this.wifiMonitorCounter);
                return;
            }
            this.isScreenOffMonitor = true;
        }

        private void handleDeviceUserPresent() {
            if (this.portalCheck) {
                WifiProStateMachine.this.logI("periodic portal check: screen unlocked, perform portal check right now");
                boolean unused = WifiProStateMachine.this.isPeriodicDet = false;
                if (HwAutoConnectManager.getInstance() != null) {
                    HwAutoConnectManager.getInstance().checkPortalAuthExpiration();
                }
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST);
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
            }
        }

        private void handleCheckWifiInternet() {
            if (!WifiProStateMachine.this.mIsWiFiNoInternet) {
                return;
            }
            if (this.isCheckWiFiForUpdateSetting || this.isDialogDisplayed) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("queryNetworkQos for wifi , isCheckWiFiForUpdateSetting =" + this.isCheckWiFiForUpdateSetting + ", isDialogDisplayed =" + this.isDialogDisplayed);
                WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                int unused = wifiProStateMachine2.mWifiTcpRxCount = wifiProStateMachine2.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                    WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                }
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
            }
        }

        private void retryWifi2Wifi() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("receive : EVENT_RETRY_WIFI_TO_WIFI, no internet = " + WifiProStateMachine.this.mIsWiFiNoInternet);
            boolean internetRecheck = false;
            if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                internetRecheck = true;
                WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
            }
            this.isCheckWiFiForUpdateSetting = false;
            wiFiLinkMonitorStateInit(internetRecheck);
        }

        private void handleWifiNetworkStateChange(Message msg) {
            if (msg.obj instanceof Intent) {
                Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
                NetworkInfo networkInfo = null;
                if (objNetworkInfo instanceof NetworkInfo) {
                    networkInfo = (NetworkInfo) objNetworkInfo;
                } else {
                    WifiProStateMachine.this.logE("handleWifiNetworkStateChange:networkInfo is not match the class");
                }
                if (networkInfo != null && NetworkInfo.DetailedState.VERIFYING_POOR_LINK == networkInfo.getDetailedState()) {
                    WifiProStateMachine.this.logI("wifi handover mobile is Complete!");
                    this.isSwitching = false;
                    WifiProStateMachine.this.uploadWifiSwitchStatistics();
                    WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[1], WifiProStateMachine.HANDOVER_SUCC_CNT);
                    WifiProStateMachine.this.mWifiProUIDisplayManager.showWifiProToast(1);
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.transitionTo(wifiProStateMachine.mWiFiProVerfyingLinkState);
                } else if (networkInfo != null && NetworkInfo.State.DISCONNECTED == networkInfo.getState()) {
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.logI("wifi has disconnected,isWifi2WifiProcess = " + this.isWifi2WifiProcess);
                    if (!this.isWifi2WifiProcess && !WifiProStateMachine.this.mIsWiFiNoInternet && WifiProStateMachine.this.mCurrentRssi < -75) {
                        WifiProStateMachine.this.setLastDisconnectNetwork();
                    }
                    if (!this.isWifi2WifiProcess || !WifiProStateMachine.this.mWifiManager.isWifiEnabled()) {
                        WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                        wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiDisConnectedState);
                    }
                }
            } else {
                WifiProStateMachine.this.logE("handleWifiNetworkStateChange:intent is not match the class");
            }
        }

        private boolean isStrongRssi() {
            return WifiProCommonUtils.getCurrenSignalLevel(WifiProStateMachine.this.mWifiManager.getConnectionInfo()) >= 3;
        }

        private void handleNetworkConnectivityChange(Message msg) {
            if (msg.obj instanceof Intent) {
                int networkType = ((Intent) msg.obj).getIntExtra("networkType", 1);
                NetworkInfo mobileInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(0);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("network change, isWifi2WifiProcess = " + this.isWifi2WifiProcess);
                if (networkType == 0 && mobileInfo != null && mobileInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && !WifiProStateMachine.this.isWifiNetworkCapabilityValidated() && !this.isWifi2WifiProcess) {
                    if (WifiProStateMachine.this.mIsWiFiNoInternet && !this.isWifiHandoverMobileToastShowed) {
                        WifiProStateMachine.this.logI("network change to mobile,show toast.");
                        WifiProUIDisplayManager access$7100 = WifiProStateMachine.this.mWifiProUIDisplayManager;
                        WifiProUIDisplayManager unused = WifiProStateMachine.this.mWifiProUIDisplayManager;
                        access$7100.showWifiProToast(1);
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        wifiProStateMachine2.updateChrToCellSucc(!wifiProStateMachine2.mIsWiFiNoInternet);
                        WifiProStateMachine.this.uploadWifiSwitchStatistics();
                        WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[0], WifiProStateMachine.HANDOVER_SUCC_CNT);
                    }
                    if (!WifiProStateMachine.this.mIsWiFiNoInternet && WifiProStateMachine.this.mLastWifiLevel != -1) {
                        WifiProStateMachine.this.updateWifiInternetStateChange(-1);
                        this.isCheckWiFiForUpdateSetting = true;
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT, -1, 1);
                    }
                    this.isWifiHandoverMobileToastShowed = true;
                    return;
                }
                return;
            }
            WifiProStateMachine.this.logE("handleNetworkConnectivityChange;Class is not match");
        }

        private void showPortalStatusBar() {
            boolean z = false;
            if (Settings.Global.getInt(WifiProStateMachine.this.mContext.getContentResolver(), "captive_portal_notification_shown", 0) == 1) {
                WifiProStateMachine.this.logE("portal notification has been shown already, not show again.");
            } else if (WifiProStateMachine.this.mCurrentWifiConfig != null && !TextUtils.isEmpty(WifiProStateMachine.this.mCurrentWifiConfig.SSID) && !WifiProStateMachine.this.mCurrentWifiConfig.SSID.equals("<unknown ssid>")) {
                Settings.Global.putInt(WifiProStateMachine.this.mContext.getContentResolver(), "captive_portal_notification_shown", 1);
                WifiProStateMachine.this.logI("periodic portal check: showPortalStatusBar, portal network = " + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mCurrentWifiConfig.getPrintableSsid()));
                if (WifiProStateMachine.this.mPortalNotificationId == -1) {
                    int unused = WifiProStateMachine.this.mPortalNotificationId = new SecureRandom().nextInt(100000);
                }
                WifiProStateMachine.this.mWifiProUIDisplayManager.showPortalNotificationStatusBar(WifiProStateMachine.this.mCurrentWifiConfig.SSID, WifiProStateMachine.PORTAL_STATUS_BAR_TAG, WifiProStateMachine.this.mPortalNotificationId, null);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                String configKey = wifiProStateMachine.mCurrentWifiConfig.configKey();
                if (WifiProStateMachine.this.mCurrentWifiConfig.lastHasInternetTimestamp > 0) {
                    z = true;
                }
                wifiProStateMachine.notifyPortalStatusChanged(true, configKey, z);
            }
        }

        private void handlePortalAuthCheckResultInLinkMonitorState(Message msg) {
            int wifiInternetLevel = msg.arg1;
            WifiProStateMachine.this.logI("periodic portal check, handlePortalAuthCheckResultInLinkMonitorState : wifiInternetLevel = " + wifiInternetLevel);
            if (6 == msg.arg1) {
                WifiProStateMachine.this.logI("periodic portal check: detectCounter = " + this.detectCounter);
                if (WifiProStateMachine.this.respCodeChrInfo.length() != 0) {
                    WifiProStateMachine.access$10184(WifiProStateMachine.this, "/");
                }
                WifiProStateMachine.access$10184(WifiProStateMachine.this, WifiProStateMachine.RESP_CODE_PORTAL);
                if (this.detectCounter >= 2) {
                    long mPortalValidityDuration = System.currentTimeMillis() - WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp;
                    if (((WifiProStateMachine.this.mCurrentWifiConfig.portalValidityDuration != 0 && WifiProStateMachine.this.mCurrentWifiConfig.portalValidityDuration > mPortalValidityDuration) || WifiProStateMachine.this.mCurrentWifiConfig.portalValidityDuration == 0) && mPortalValidityDuration > 0) {
                        WifiProStateMachine.this.mCurrentWifiConfig.portalValidityDuration = mPortalValidityDuration;
                    }
                    if (WifiProStateMachine.this.mNetworkPropertyChecker != null) {
                        Settings.Global.putString(WifiProStateMachine.this.mContext.getContentResolver(), "captive_portal_server", WifiProStateMachine.this.mNetworkPropertyChecker.getCaptiveUsedServer());
                    }
                    WifiProStateMachine.this.mCurrentWifiConfig.portalAuthTimestamp = 0;
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.updateWifiConfig(wifiProStateMachine.mCurrentWifiConfig);
                    showPortalStatusBar();
                    this.detectCounter = 0;
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHR_ALARM_EXPIRED, WifiProStateMachine.DELAY_UPLOAD_MS);
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.deferMessage(wifiProStateMachine2.obtainMessage(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT, 6));
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiConnectedState);
                    return;
                }
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST, 2000);
                this.detectCounter++;
            } else if (this.detectCounter > 0) {
                if (wifiInternetLevel < 1 || wifiInternetLevel > 5) {
                    WifiProStateMachine.access$10184(WifiProStateMachine.this, "/");
                    WifiProStateMachine.access$10184(WifiProStateMachine.this, WifiProStateMachine.RESP_CODE_INTERNET_UNREACHABLE);
                } else {
                    WifiProStateMachine.access$10184(WifiProStateMachine.this, "/");
                    WifiProStateMachine.access$10184(WifiProStateMachine.this, WifiProStateMachine.RESP_CODE_INTERNET_AVAILABLE);
                }
                WifiProStateMachine.this.logI("respCode changes in consecutive checks, upload CHR");
                WifiProStateMachine.this.uploadPortalAuthExpirationStatistics(false);
                this.detectCounter = 0;
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
            }
        }

        private void handleWifiInternetResultInLinkMonitorState(Message msg) {
            if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_STATUS)) {
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_STATUS);
            }
            int wifiInternetLevel = msg.arg1;
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("WiFiLinkMonitorState : wifiInternetLevel = " + wifiInternetLevel);
            if (isNeedToIgnoreNoInternetResult(wifiInternetLevel)) {
                WifiProStateMachine.this.logI("WLAN+ has handled NoInternet Message, don't need to handle repeatedly.");
                return;
            }
            if (-1 == msg.arg1 || (6 == msg.arg1 && !this.portalCheck)) {
                if (!WifiProStateMachine.this.mIsWiFiInternetCHRFlag && msg.arg2 != 1 && !isSatisfySelfCureConditions()) {
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.logI("upload WIFI_ACCESS_INTERNET_FAILED event for TRANS_TO_NO_INTERNET,ssid:" + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mCurrentSsid));
                    Bundle data = new Bundle();
                    data.putInt(WifiProStateMachine.EVENT_ID, 87);
                    data.putString(WifiProStateMachine.EVENT_DATA, "TRANS_TO_NO_INTERNET");
                    WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 4, data);
                    boolean unused = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = true;
                }
                boolean unused2 = WifiProStateMachine.this.mIsWiFiNoInternet = true;
                this.currWifiPoorlevel = -1;
                wifiInternetLevel = this.currWifiPoorlevel;
                if (this.isBQERequestCheckWiFi) {
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseNoInetRemindCount(false);
                }
                if (this.isCheckWiFiForUpdateSetting) {
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    int unused3 = wifiProStateMachine3.mWifiTcpRxCount = wifiProStateMachine3.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                    if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                }
            } else if (msg.arg1 != 6 || !this.portalCheck) {
                WifiProStateMachine.this.sendNetworkCheckingStatus(WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, wifiInternetLevel);
                this.wifiMonitorCounter = 0;
                this.isCheckWiFiForUpdateSetting = false;
                this.isWifiHandoverMobileToastShowed = false;
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.mWifiProUIDisplayManager.notificateNetAccessChange(false);
                }
                boolean unused4 = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                boolean unused5 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = false;
                updateWifiQosLevel(WifiProStateMachine.this.mIsWiFiNoInternet, WifiProStateMachine.this.mNetworkQosMonitor.getCurrentWiFiLevel());
                WifiProStateMachine.this.reSetWifiInternetState();
                WifiProStateMachine.this.setWifiMonitorEnabled(true);
            } else {
                WifiProStateMachine.this.logD("periodic portal check: portal is detected triggered by watchdog, perform portal check right now");
                if (HwAutoConnectManager.getInstance() != null) {
                    HwAutoConnectManager.getInstance().checkPortalAuthExpiration();
                }
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_FAST);
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_PERIODIC_PORTAL_CHECK_SLOW, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
                return;
            }
            this.isBQERequestCheckWiFi = false;
            if (msg.arg2 != 1) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_QOS_CHANGE, wifiInternetLevel, 0, false);
            } else {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_WIFI_QOS_CHANGE, wifiInternetLevel, 1, false);
            }
            WifiProStateMachine.this.mWifiHandover.updateWiFiInternetAccess(!WifiProStateMachine.this.mIsWiFiNoInternet);
        }

        private void checkWifiNetworkStatus() {
            WifiProStateMachine.this.logI("Five seconds is up and There is detection results back");
            boolean unused = WifiProStateMachine.this.isVerifyWifiNoInternetTimeOut = true;
            WifiProStateMachine.this.onNetworkDetectionResult(1, -1);
        }

        private boolean isNeedToIgnoreNoInternetResult(int checkResult) {
            if (WifiProStateMachine.this.hasHandledNoInternetResult) {
                boolean unused = WifiProStateMachine.this.hasHandledNoInternetResult = false;
                if (checkResult == -1) {
                    return true;
                }
            }
            if (WifiProStateMachine.this.isVerifyWifiNoInternetTimeOut && checkResult == -1) {
                boolean unused2 = WifiProStateMachine.this.isVerifyWifiNoInternetTimeOut = false;
                boolean unused3 = WifiProStateMachine.this.hasHandledNoInternetResult = true;
            }
            return false;
        }

        private void handleWifiQosChangedInLinkMonitorState(Message msg) {
            if (!handleMsgBySwitchOrDialogStatus(msg.arg1)) {
                boolean updateUiOnly = false;
                if (msg.obj instanceof Boolean) {
                    updateUiOnly = ((Boolean) msg.obj).booleanValue();
                } else {
                    WifiProStateMachine.this.logE("handleWifiQosChangedInLinkMonitorState:Class is not match");
                }
                if (!updateUiOnly || -103 == msg.arg1 || -104 == msg.arg1) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("WiFiLinkMonitorState receive wifi Qos currWifiPoorlevel = " + msg.arg1 + ", dialog = " + this.isNoInternetDialogShowing + ", updateSettings = " + this.isCheckWiFiForUpdateSetting);
                    if (-103 == msg.arg1) {
                        if (!WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_STATUS)) {
                            WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_STATUS, 5000);
                            this.isBQERequestCheckWiFi = true;
                            this.isRequestWifInetCheck = true;
                        }
                    } else if (-104 == msg.arg1) {
                        WifiProStateMachine.this.logI("REQUEST_POOR_RSSI_INET_CHECK, no HTTP GET, wait APPs to report poor link.");
                    } else {
                        updateQosLevel(msg);
                    }
                } else {
                    this.currWifiPoorlevel = msg.arg1;
                    if (msg.arg1 == 0 || msg.arg1 == 1) {
                        updateWifiQosLevel(false, 1);
                    }
                    if (WifiProStateMachine.this.mIsWiFiNoInternet && msg.arg1 == 3) {
                        WifiProStateMachine.this.updateWifiInternetStateChange(msg.arg1);
                        boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                        HwWifiProFeatureControl.getInstance();
                        HwWifiProFeatureControl.notifyInternetAccessRecovery();
                    }
                }
            }
        }

        private void updateQosLevel(Message msg) {
            if (!this.isCheckWiFiForUpdateSetting || WifiProStateMachine.this.mIsWiFiNoInternet) {
                this.currWifiPoorlevel = msg.arg1;
                if (msg.arg1 <= 2 && !this.isNoInternetDialogShowing) {
                    if (this.currWifiPoorlevel == -1) {
                        boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = true;
                        if (msg.arg2 != 1) {
                            boolean unused2 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = true;
                        }
                    }
                    int unused3 = WifiProStateMachine.this.mWifiToWifiType = 0;
                    if (this.currWifiPoorlevel == -2) {
                        WifiProStateMachine.this.refreshConnectedNetWork();
                        tryWifiHandoverPreferentially(WifiProStateMachine.this.mCurrentRssi);
                        return;
                    }
                    updateWifiQosLevel(WifiProStateMachine.this.mIsWiFiNoInternet, 1);
                    if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                        int unused4 = WifiProStateMachine.this.mWifiToWifiType = 1;
                    }
                    WifiProStateMachine.this.logW("WiFiLinkMonitorState : try wifi --> wifi --> mobile data");
                    this.isWiFiHandoverPriority = false;
                    tryWifi2Wifi();
                }
                if (this.isRequestWifInetCheck && this.currWifiPoorlevel == -1) {
                    this.isRequestWifInetCheck = false;
                    this.isNotifyInvalidLinkDetection = true;
                    WifiProStateMachine.this.logI("Monitoring to the broken network, Maybe needs to be informed the message to networkmonitor");
                    return;
                }
                return;
            }
            WifiProStateMachine.this.setWifiMonitorEnabled(true);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.sendMessage(WifiProStateMachine.EVENT_WIFI_EVALUTE_TCPRTT_RESULT, wifiProStateMachine.mNetworkQosMonitor.getCurrentWiFiLevel());
        }

        private void handleOrientationChanged(Message msg) {
            if (WifiProStateMachine.this.mDelayedRssiChangedByFullScreen && !WifiProStateMachine.this.isFullscreen()) {
                boolean unused = WifiProStateMachine.this.mDelayedRssiChangedByFullScreen = false;
                if (WifiProCommonUtils.getCurrenSignalLevel(WifiProStateMachine.this.mWifiManager.getConnectionInfo()) < 3) {
                    WifiProStateMachine.this.logI("handleOrientationChanged, continue full screen skiped scan.");
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasWifiSwitchRecord()));
                }
            }
        }

        private void handleCallStateChanged(Message msg) {
            if (WifiProStateMachine.this.mDelayedRssiChangedByCalling && msg.arg1 == 0) {
                boolean unused = WifiProStateMachine.this.mDelayedRssiChangedByCalling = false;
                if (WifiProCommonUtils.getCurrenSignalLevel(WifiProStateMachine.this.mWifiManager.getConnectionInfo()) < 3) {
                    WifiProStateMachine.this.logI("handleCallStateChanged, continue scan.");
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasWifiSwitchRecord()));
                }
            }
        }

        private void handleEmuiCspSettingChange() {
            if (WifiProStateMachine.this.mEmuiPdpSwichValue != 2) {
                this.isAllowWiFiHandoverMobile = true;
                this.isCheckWiFiForUpdateSetting = false;
                this.isSwitching = false;
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                } else {
                    WifiProStateMachine.this.setWifiMonitorEnabled(true);
                }
            }
        }

        private void handleRssiChangedInLinkMonitorState(Message msg) {
            if (msg.obj instanceof Intent) {
                int unused = WifiProStateMachine.this.mCurrentRssi = ((Intent) msg.obj).getIntExtra("newRssi", WifiHandover.INVALID_RSSI);
                if (WifiProStateMachine.this.isFullscreen()) {
                    boolean unused2 = WifiProStateMachine.this.mDelayedRssiChangedByFullScreen = true;
                } else if (WifiProCommonUtils.isCalling(WifiProStateMachine.this.mContext)) {
                    boolean unused3 = WifiProStateMachine.this.mDelayedRssiChangedByCalling = true;
                } else {
                    boolean unused4 = WifiProStateMachine.this.mDelayedRssiChangedByCalling = false;
                    boolean unused5 = WifiProStateMachine.this.mDelayedRssiChangedByFullScreen = false;
                    if (!this.isWiFiHandoverPriority && !this.isWifi2WifiProcess && !WifiProStateMachine.this.mIsWiFiNoInternet && !WifiProStateMachine.this.hasMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY)) {
                        int rssilevel = WifiProCommonUtils.getCurrenSignalLevel(WifiProStateMachine.this.mWifiManager.getConnectionInfo());
                        boolean hasSwitchRecord = hasWifiSwitchRecord();
                        if (rssilevel >= 3) {
                            if (rssilevel == 4 && hasSwitchRecord) {
                                this.rssiLevel2ScanedCounter = 0;
                                this.rssiLevel0Or1ScanedCounter = 0;
                            }
                            if (WifiProStateMachine.this.hasMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY)) {
                                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY);
                                return;
                            }
                            return;
                        }
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasSwitchRecord));
                    }
                }
            } else {
                WifiProStateMachine.this.logE("handleRssiChangedInLinkMonitorState:Class is not match");
            }
        }

        private boolean hasWifiSwitchRecord() {
            WifiProStateMachine.this.refreshConnectedNetWork();
            if (WifiProStateMachine.this.mCurrentWifiConfig == null || WifiProStateMachine.this.mCurrentWifiConfig.lastTrySwitchWifiTimestamp <= 0) {
                return false;
            }
            return System.currentTimeMillis() - WifiProStateMachine.this.mCurrentWifiConfig.lastTrySwitchWifiTimestamp < WifiProStateMachine.WIFI_SWITCH_RECORD_MAX_TIME;
        }

        private void handleReuqestScanInLinkMonitorState(Message msg) {
            int scanMaxCounter;
            int scanInterval;
            int i;
            int i2;
            if (WifiProStateMachine.this.isFullscreen()) {
                boolean unused = WifiProStateMachine.this.mDelayedRssiChangedByFullScreen = true;
                WifiProStateMachine.this.logE("handleReuqestScanInLinkMonitorState, don't try to swithch wifi when full screen.");
            } else if (WifiProCommonUtils.isCalling(WifiProStateMachine.this.mContext)) {
                WifiProStateMachine.this.logE("handleReuqestScanInLinkMonitorState, don't try to swithch wifi when calling.");
                boolean unused2 = WifiProStateMachine.this.mDelayedRssiChangedByCalling = true;
            } else {
                boolean hasWifiSwitchRecord = false;
                if (msg.obj instanceof Boolean) {
                    hasWifiSwitchRecord = ((Boolean) msg.obj).booleanValue();
                } else {
                    WifiProStateMachine.this.logE("handleReuqestScanInLinkMonitorState:Class is not match");
                }
                HwWifiProFeatureControl.getInstance();
                if (HwWifiProFeatureControl.isSelfCureOngoing()) {
                    if (WifiProStateMachine.this.hasMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY)) {
                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY);
                    }
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.sendMessageDelayed(wifiProStateMachine.obtainMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasWifiSwitchRecord)), 10000);
                    return;
                }
                int rssilevel = WifiProCommonUtils.getCurrenSignalLevel(WifiProStateMachine.this.mWifiManager.getConnectionInfo());
                if (rssilevel < 3) {
                    if (!WifiProStateMachine.this.mIsUserManualConnectSuccess || rssilevel != 2 || this.currWifiPoorlevel <= 2) {
                        if (hasWifiSwitchRecord) {
                            scanInterval = WifiProStateMachine.QUICK_SCAN_INTERVAL[rssilevel];
                            scanMaxCounter = WifiProStateMachine.QUICK_SCAN_MAX_COUNTER[rssilevel];
                        } else {
                            scanInterval = WifiProStateMachine.NORMAL_SCAN_INTERVAL[rssilevel];
                            scanMaxCounter = WifiProStateMachine.NORMAL_SCAN_MAX_COUNTER[rssilevel];
                        }
                        if (rssilevel == 2 && (i2 = this.rssiLevel2ScanedCounter) < scanMaxCounter) {
                            this.rssiLevel2ScanedCounter = i2 + 1;
                            WifiProStateMachine.this.mWifiManager.startScan();
                            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                            wifiProStateMachine2.sendMessageDelayed(wifiProStateMachine2.obtainMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasWifiSwitchRecord)), (long) scanInterval);
                        } else if (rssilevel < 2 && (i = this.rssiLevel0Or1ScanedCounter) < scanMaxCounter) {
                            this.rssiLevel0Or1ScanedCounter = i + 1;
                            WifiProStateMachine.this.mWifiManager.startScan();
                            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                            wifiProStateMachine3.sendMessageDelayed(wifiProStateMachine3.obtainMessage(WifiProStateMachine.EVENT_REQUEST_SCAN_DELAY, Boolean.valueOf(hasWifiSwitchRecord)), (long) scanInterval);
                        }
                    } else {
                        WifiProStateMachine.this.logI("handleReuqestScanInLinkMonitorState, user click and signal = 2, but wifi link is good, don't trigger scan.");
                    }
                }
            }
        }

        private void handleCheckResultInLinkMonitorState(Message msg) {
            if (WifiProStateMachine.this.isFullscreen() || WifiProCommonUtils.isCalling(WifiProStateMachine.this.mContext) || WifiProStateMachine.this.mIsWiFiNoInternet || !((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logW("mIsWiFiNoInternet" + WifiProStateMachine.this.mIsWiFiNoInternet + "isFullscreen" + WifiProStateMachine.this.isFullscreen());
                return;
            }
            if (!this.isWiFiHandoverPriority && !this.isWifi2WifiProcess) {
                HwWifiProFeatureControl.getInstance();
                if (!HwWifiProFeatureControl.isSelfCureOngoing()) {
                    if (WifiProStateMachine.this.mNetworkQosMonitor.isHighDataFlowModel()) {
                        WifiProStateMachine.this.logw("has good rssi network, but user is in high data mode, don't handle wifi switch.");
                        return;
                    }
                    WifiInfo wifiInfo = WifiProStateMachine.this.mWifiManager.getConnectionInfo();
                    if (wifiInfo == null || wifiInfo.getRssi() == -127) {
                        WifiProStateMachine.this.logW("wifiInfo RSSI is invalid");
                        return;
                    }
                    int preferType = msg.arg2;
                    int curRssiLevel = WifiProCommonUtils.getCurrenSignalLevel(wifiInfo);
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.logI("handleCheckResultInLinkMonitorState, prefer=" + preferType + ", manual=" + WifiProStateMachine.this.mIsUserManualConnectSuccess);
                    if (1 == preferType) {
                        WifiProStateMachine.this.logI("handleCheckResultInLinkMonitorState, go wifi2wifi");
                        tryWifiHandoverWithoutRssiCheck(curRssiLevel);
                        return;
                    } else if (curRssiLevel < 3) {
                        int targetRssiLevel = msg.arg1;
                        WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                        wifiProStateMachine3.logI("curRssiLevel = " + curRssiLevel + ", targetRssiLevel " + targetRssiLevel);
                        if (targetRssiLevel - curRssiLevel >= 2) {
                            this.isRssiLowOrMiddleWifi2Wifi = true;
                            tryWifiHandoverPreferentially(curRssiLevel);
                            return;
                        }
                        return;
                    } else {
                        return;
                    }
                }
            }
            WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
            wifiProStateMachine4.logW("isWiFiHandoverPriority" + this.isWiFiHandoverPriority + "isWifi2WifiProcess" + this.isWifi2WifiProcess);
        }

        private void handleDualbandApAvailable() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("receive EVENT_DUALBAND_5GAP_AVAILABLE isSwitching = " + this.isSwitching);
            int switchType = 1;
            if (this.isSwitching) {
                boolean unused = WifiProStateMachine.this.mNeedRetryMonitor = true;
                return;
            }
            if (WifiProStateMachine.this.mCurrentWifiConfig != null && WifiProStateMachine.this.mCurrentWifiConfig.SSID != null && WifiProStateMachine.this.mAvailable5GAPSsid != null && WifiProStateMachine.this.mCurrentWifiConfig.SSID.equals(WifiProStateMachine.this.mAvailable5GAPSsid) && WifiProStateMachine.this.mCurrentWifiConfig.allowedKeyManagement.cardinality() <= 1 && WifiProStateMachine.this.mCurrentWifiConfig.getAuthType() == WifiProStateMachine.this.mAvailable5GAPAuthType) {
                int unused2 = WifiProStateMachine.this.mDuanBandHandoverType = 1;
                WifiProStateMachine.this.logI("handleDualbandApAvailable 5G and 2.4G AP have the same ssid and auth type");
            }
            WifiProStateMachine.this.logI("do dual band wifi handover");
            if (!WifiProStateMachine.this.isFullscreen() && !WifiProCommonUtils.isCalling(WifiProStateMachine.this.mContext)) {
                HwWifiProFeatureControl.getInstance();
                if (!HwWifiProFeatureControl.isSelfCureOngoing()) {
                    this.isSwitching = true;
                    this.isWifi2WifiProcess = true;
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    String unused3 = wifiProStateMachine2.mBadBssid = wifiProStateMachine2.mCurrentBssid;
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    String unused4 = wifiProStateMachine3.mBadSsid = wifiProStateMachine3.mCurrentSsid;
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    wifiProStateMachine4.logI("do dual band wifi handover, mCurrentBssid = " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mCurrentBssid) + ", mAvailable5GAPBssid = " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mAvailable5GAPBssid) + ", mDuanBandHandoverType = " + WifiProStateMachine.this.mDuanBandHandoverType);
                    WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                    String unused5 = wifiProStateMachine5.mNewSelect_bssid = wifiProStateMachine5.mAvailable5GAPBssid;
                    if (WifiProStateMachine.this.mDuanBandHandoverType == 1) {
                        switchType = 2;
                    }
                    WifiProStateMachine wifiProStateMachine6 = WifiProStateMachine.this;
                    wifiProStateMachine6.logI("do dual band wifi handover, switchType = " + switchType);
                    int dualbandReason = WifiProStateMachine.this.mWifiHandover.handleDualBandWifiConnect(WifiProStateMachine.this.mAvailable5GAPBssid, WifiProStateMachine.this.mAvailable5GAPSsid, WifiProStateMachine.this.mAvailable5GAPAuthType, switchType);
                    if (dualbandReason != 0) {
                        dualBandhandoverFailed(dualbandReason);
                        return;
                    }
                    if (WifiProStateMachine.this.mCurrentWifiConfig != null) {
                        WifiProStateMachine wifiProStateMachine7 = WifiProStateMachine.this;
                        int unused6 = wifiProStateMachine7.mChrQosLevelBeforeHandover = wifiProStateMachine7.mCurrentWifiConfig.networkQosLevel;
                    }
                    long unused7 = WifiProStateMachine.this.mWifiDualBandStartTime = SystemClock.elapsedRealtime();
                    return;
                }
            }
            WifiProStateMachine.this.logI("keep in current AP,now is in calling/full screen/selfcure and switch by hardhandover");
        }

        private void tryWifiHandoverPreferentially(int curRssiLevel) {
            if ((!WifiProStateMachine.this.mIsUserManualConnectSuccess || WifiProStateMachine.this.mIsWiFiProEnabled) && curRssiLevel <= 2) {
                if (curRssiLevel < 1 && !WifiProStateMachine.this.mIsScanedRssiLow) {
                    boolean unused = WifiProStateMachine.this.mIsScanedRssiLow = true;
                } else if (curRssiLevel >= 1 && !WifiProStateMachine.this.mIsScanedRssiMiddle) {
                    boolean unused2 = WifiProStateMachine.this.mIsScanedRssiMiddle = true;
                    int unused3 = WifiProStateMachine.this.mHandoverReason = 5;
                } else {
                    return;
                }
                this.isWiFiHandoverPriority = true;
                long unused4 = WifiProStateMachine.this.mWifiHandoverStartTime = SystemClock.elapsedRealtime();
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logW("try wifi --> wifi only, current rssi = " + WifiProStateMachine.this.mCurrentRssi);
                this.mWifi2WifiThreshod = WifiProStateMachine.this.mCurrentRssi;
                tryWifi2Wifi();
            }
        }

        private void tryWifiHandoverWithoutRssiCheck(int curRssiLevel) {
            if (!WifiProStateMachine.this.mIsWiFiProEnabled) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logE("tryWifiHandoverWithoutRssiCheck: mIsWiFiProEnabled = " + WifiProStateMachine.this.mIsWiFiProEnabled);
                return;
            }
            this.isWiFiHandoverPriority = true;
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.logW("try wifi --> wifi only, current rssi = " + WifiProStateMachine.this.mCurrentRssi);
            this.mWifi2WifiThreshod = WifiProStateMachine.this.mCurrentRssi;
            tryWifi2Wifi();
        }

        private void tryWifi2Wifi() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.uploadChrWifiHandoverWifi(!wifiProStateMachine.mIsWiFiNoInternet, this.isWiFiHandoverPriority);
            if ((WifiProStateMachine.this.mIsUserManualConnectSuccess && !WifiProStateMachine.this.mIsWiFiProEnabled) || WifiProStateMachine.this.isKeepCurrWiFiConnected()) {
                WifiProStateMachine.this.logE("User manual connect wifi, and wifi+ disabled. don't try wifi switch!");
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.logE("WiFi2WiFi failed because cannot switch, try to trigger selfcure");
                    pendingMsgBySelfCureEngine(WifiproUtils.REQUEST_WIFI_INET_CHECK);
                }
                if (WifiProStateMachine.this.mHandoverFailReason != 10) {
                    int unused = WifiProStateMachine.this.mHandoverFailReason = 10;
                    WifiProStateMachine.this.uploadWifiSwitchFailTypeStatistics(10);
                }
            } else if (HwWifiProFeatureControl.sWifiProToWifiCtrl) {
                this.isSwitching = true;
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_TRY_WIFI_ROVE_OUT);
            }
        }

        private boolean isSatisfiedWifiToCellCondition(boolean isMpLinkState) {
            boolean isMobileHotspot = HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(WifiProStateMachine.this.mContext);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("isWifi2WifiProcess = " + this.isWifi2WifiProcess + ", isAllowWifi2Mobile = " + WifiProStateMachine.this.isAllowWifi2Mobile() + ", mIsAllowWiFiHandoverMobile = " + this.isAllowWiFiHandoverMobile + ", mIsWiFiNoInternet = " + WifiProStateMachine.this.mIsWiFiNoInternet + ", isStrongRssi = " + isStrongRssi() + ", isOpenAndPortal = " + WifiProCommonUtils.isOpenAndPortal(WifiProStateMachine.this.mCurrentWifiConfig) + ", isMpLinkState = " + isMpLinkState + ", oversea = " + WifiProCommonUtils.isOversea() + ", isDomesticBetaUser = " + WifiProCommonUtils.isDomesticBetaUser() + ", isMobileHotspot = " + isMobileHotspot);
            if (this.isWifi2WifiProcess || !WifiProStateMachine.this.isAllowWifi2Mobile() || !this.isAllowWiFiHandoverMobile || !WifiProStateMachine.this.mPowerManager.isScreenOn() || isCallingInCs(WifiProStateMachine.this.mContext) || WifiProStateMachine.this.mIsWiFiNoInternet || isMpLinkState) {
                WifiProStateMachine.this.logI("can not hand over to mobile, keep monitor qos");
                return false;
            } else if (!isStrongRssi()) {
                return true;
            } else {
                if ((WifiProStateMachine.this.mIsWifiSwitchRobotAlgorithmEnabled || !WifiProCommonUtils.isOpenAndPortal(WifiProStateMachine.this.mCurrentWifiConfig)) && (!WifiProStateMachine.this.mIsWifiSwitchRobotAlgorithmEnabled || !WifiProStateMachine.this.mIsWifi2CellInStrongSignalEnabled || isMobileHotspot || (!WifiProCommonUtils.isDomesticBetaUser() && (!WifiProCommonUtils.isInstallHuaweiCustomizedApp(WifiProStateMachine.this.mContext) || WifiProCommonUtils.isOversea())))) {
                    WifiProStateMachine.this.logI("can not hand over to mobile in strong rssi");
                    return false;
                }
                WifiProStateMachine.this.logI("can hand over to mobile in strong rssi");
                return true;
            }
        }

        private void tryWifi2Mobile(int mobile_level) {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("Receive mobile QOS  mobile_level = " + mobile_level + ", isSwitching =" + this.isSwitching);
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.updateChrToCell(wifiProStateMachine2.mIsWiFiNoInternet ^ true);
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 39, new Bundle());
            boolean wifiProFromBrainFlag = false;
            if (result != null) {
                wifiProFromBrainFlag = result.getBoolean("isWifiProFromBrainFlag");
            }
            if (!isSatisfiedWifiToCellCondition(wifiProFromBrainFlag)) {
                this.isSwitching = false;
                if (wifiProFromBrainFlag && WifiProStateMachine.this.mHandoverFailReason != 1) {
                    int unused = WifiProStateMachine.this.mHandoverFailReason = 1;
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine3.mHandoverFailReason);
                }
                if (!WifiProStateMachine.this.mPowerManager.isScreenOn() && WifiProStateMachine.this.mHandoverFailReason != 9) {
                    int unused2 = WifiProStateMachine.this.mHandoverFailReason = 9;
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    wifiProStateMachine4.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine4.mHandoverFailReason);
                }
                if (isCallingInCs(WifiProStateMachine.this.mContext) && WifiProStateMachine.this.mHandoverFailReason != 6) {
                    int unused3 = WifiProStateMachine.this.mHandoverFailReason = 6;
                    WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                    wifiProStateMachine5.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine5.mHandoverFailReason);
                }
                if (!WifiProStateMachine.this.hasMessages(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR)) {
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR, 30000);
                }
            } else if (!WifiProStateMachine.this.isWiFiPoorer(this.currWifiPoorlevel, mobile_level)) {
                WifiProStateMachine.this.logI("mobile is poorer,continue monitor");
                this.isSwitching = false;
                if (WifiProStateMachine.this.mIsWiFiNoInternet && !this.isToastDisplayed) {
                    this.isToastDisplayed = true;
                    WifiProStateMachine.this.mWifiProUIDisplayManager.showWifiProToast(3);
                }
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR, 30000);
            } else if (!this.isSwitching || !WifiProStateMachine.this.isAllowWifi2Mobile()) {
                WifiProStateMachine.this.logW("no handover,DELAY Transit to Monitor");
                this.isSwitching = false;
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR, 30000);
            } else {
                WifiProStateMachine wifiProStateMachine6 = WifiProStateMachine.this;
                wifiProStateMachine6.updateChrToCellSucc(!wifiProStateMachine6.mIsWiFiNoInternet);
                WifiProStateMachine wifiProStateMachine7 = WifiProStateMachine.this;
                wifiProStateMachine7.logI("mobile is better than wifi,and ScreenOn, try wifi --> mobile,show Dialog mEmuiPdpSwichValue = " + WifiProStateMachine.this.mEmuiPdpSwichValue + ", mIsWiFiNoInternet =" + WifiProStateMachine.this.mIsWiFiNoInternet);
                if (this.isWifi2MobileUIShowing) {
                    WifiProStateMachine wifiProStateMachine8 = WifiProStateMachine.this;
                    wifiProStateMachine8.logE("isWifi2MobileUIShowing = true, not dispaly " + this.isWifi2MobileUIShowing);
                    return;
                }
                this.isWifi2MobileUIShowing = true;
                if (WifiProStateMachine.this.isPdpAvailable()) {
                    WifiProStateMachine wifiProStateMachine9 = WifiProStateMachine.this;
                    wifiProStateMachine9.logI("mobile is cmcc and wifi pdp, mEmuiPdpSwichValue = " + WifiProStateMachine.this.mEmuiPdpSwichValue + " ,mWiFiProPdpSwichValue = " + WifiProStateMachine.this.mWiFiProPdpSwichValue + " last rssi signal=" + WifiProStateMachine.this.mCurrentRssi);
                    int emuiPdpSwichType = WifiProStateMachine.this.mEmuiPdpSwichValue;
                    if (WifiProStateMachine.this.isDialogUpWhenConnected) {
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DIALOG_OK);
                        return;
                    }
                    if (emuiPdpSwichType == 0) {
                        emuiPdpSwichType = 1;
                    }
                    if (emuiPdpSwichType == 1) {
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DIALOG_OK);
                    } else if (emuiPdpSwichType == 2) {
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DIALOG_CANCEL);
                    }
                } else {
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_DIALOG_OK);
                }
            }
        }

        private void wifi2WifiFailed() {
            if (!(WifiProStateMachine.this.mHandoverFailReason == 8 || WifiProStateMachine.this.mHandoverFailReason == 7)) {
                int unused = WifiProStateMachine.this.mHandoverFailReason = 8;
                WifiProStateMachine.this.uploadWifiSwitchFailTypeStatistics(8);
            }
            if (WifiProStateMachine.this.mNewSelect_bssid != null && !WifiProStateMachine.this.mNewSelect_bssid.equals(WifiProStateMachine.this.mBadBssid)) {
                WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mNewSelect_bssid);
            }
            WifiProStateMachine.this.logI("wifi to Wifi Failed Finally!");
            if (this.isNotifyInvalidLinkDetection && WifiProStateMachine.this.mIsWiFiNoInternet) {
                WifiProStateMachine.this.updateChrToCell(false);
                this.isNotifyInvalidLinkDetection = false;
                WifiProStateMachine.this.logI("We detection no internet, And wifi2WifiFailed, So we need notify msg to networkmonitor");
                WifiProStateMachine.this.mWsmChannel.sendMessage((int) WifiProStateMachine.INVALID_LINK_DETECTED);
            }
            this.isWifi2WifiProcess = false;
            this.isSwitching = false;
            if (this.isRssiLowOrMiddleWifi2Wifi) {
                this.isRssiLowOrMiddleWifi2Wifi = false;
                WifiProStateMachine.this.resetScanedRssiVariable();
            }
            if (WifiProCommonUtils.isWifiConnectedOrConnecting(WifiProStateMachine.this.mWifiManager)) {
                handlewifi2WifiFailedInConnectedOrConnecting();
                return;
            }
            WifiProStateMachine.this.logI("wifi handover over Failed and system auto conning ap");
            if (!WifiProStateMachine.this.mIsWiFiNoInternet) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("try to connect : " + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mBadSsid));
                WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[8], WifiProStateMachine.HANDOVER_CNT);
                WifiProStateMachine.this.mWifiHandover.connectWifiNetwork(WifiProStateMachine.this.mBadBssid);
            }
        }

        private void handlewifi2WifiFailedInConnectedOrConnecting() {
            if (WifiProStateMachine.this.mNeedRetryMonitor) {
                WifiProStateMachine.this.logI("need retry dualband handover monitor");
                WifiProStateMachine.this.retryDualBandAPMonitor();
                boolean unused = WifiProStateMachine.this.mNeedRetryMonitor = false;
            }
            if (this.isWiFiHandoverPriority) {
                WifiProStateMachine.this.logI("wifi handover wifi failed,continue monitor wifi Qos");
                if (!WifiProStateMachine.this.mIsUserManualConnectSuccess || this.currWifiPoorlevel != -2) {
                    this.isWiFiHandoverPriority = false;
                    return;
                }
                return;
            }
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("wifi --> wifi is Failure, but wifi is connected, isMobileDataConnected() = " + WifiProStateMachine.this.isMobileDataConnected() + ", isAllowWiFiHandoverMobile =  " + this.isAllowWiFiHandoverMobile + " , mEmuiPdpSwichValue = " + WifiProStateMachine.this.mEmuiPdpSwichValue + ", mPowerManager.isScreenOn =" + WifiProStateMachine.this.mPowerManager.isScreenOn() + ", currWifiPoorlevel = " + this.currWifiPoorlevel + ", mIsWiFiNoInternet = " + WifiProStateMachine.this.mIsWiFiNoInternet);
            handleWifi2WifiFalied();
        }

        private void notifyChrPreventWifiSwitchMobile() {
            if (WifiProStateMachine.this.isMobileDataConnected() && WifiProStateMachine.this.mPowerManager != null && WifiProStateMachine.this.mPowerManager.isScreenOn() && WifiProStateMachine.this.mEmuiPdpSwichValue == 2 && !this.isCancelCHRTypeReport) {
                this.isCancelCHRTypeReport = true;
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.logI("call increaseNotInetSettingCancelCount.");
                    WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetSettingCancelCount();
                    return;
                }
                WifiProStateMachine.this.logI("call increaseBQE_BadSettingCancelCount.");
                WifiProStateMachine.this.mWifiProStatisticsManager.increaseBqeBadSettingCancelCount();
            }
        }

        private void handleWifi2WifiFalied() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.uploadChrWifiHandoverCell(!wifiProStateMachine.mIsWiFiNoInternet);
            if (WifiProStateMachine.this.mIsWiFiNoInternet || !WifiProStateMachine.this.isAllowWifi2Mobile() || WifiProStateMachine.this.mNotifyWifiLinkPoorReason == -1 || (isStrongRssi() && (!WifiProStateMachine.this.mIsWifiSwitchRobotAlgorithmEnabled || !WifiProStateMachine.this.mIsWifi2CellInStrongSignalEnabled || !WifiProStateMachine.this.isNeedToSwitch2Cell()))) {
                WifiProStateMachine.this.logI("wifi --> wifi is Failure,and can not handover to mobile ,delay 30s go to Monitor");
                notifyChrPreventWifiSwitchMobile();
                if (!WifiProStateMachine.this.isMobileDataConnected() && WifiProStateMachine.this.mHandoverFailReason != 12) {
                    int unused = WifiProStateMachine.this.mHandoverFailReason = 12;
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine2.mHandoverFailReason);
                }
                if (!WifiProStateMachine.this.mPowerManager.isScreenOn() && WifiProStateMachine.this.mHandoverFailReason != 9) {
                    int unused2 = WifiProStateMachine.this.mHandoverFailReason = 9;
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.uploadWifiSwitchFailTypeStatistics(wifiProStateMachine3.mHandoverFailReason);
                }
                if (WifiProStateMachine.this.mIsWiFiNoInternet && !this.isToastDisplayed) {
                    this.isToastDisplayed = true;
                    WifiProStateMachine.this.mWifiProUIDisplayManager.showWifiProToast(3);
                }
                WifiProStateMachine.this.setWifiMonitorEnabled(false);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_REINITIALIZE_WIFI_MONITOR, 30000);
                if (WifiProStateMachine.this.mCurrentWifiConfig != null && WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetAccess && WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetReason == 0) {
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    int unused3 = wifiProStateMachine4.mWifiTcpRxCount = wifiProStateMachine4.mNetworkQosMonitor.requestTcpRxPacketsCounter();
                    if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX)) {
                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_GET_WIFI_TCPRX);
                    }
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_GET_WIFI_TCPRX, 5000);
                    return;
                }
                return;
            }
            WifiProStateMachine.this.logI("try to wifi --> mobile,Query mobile Qos");
            this.isSwitching = true;
            WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(0, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
        }

        private void dualBandhandoverFailed(int reason) {
            if (!(10 == reason || 11 == reason)) {
                if (WifiProStateMachine.this.mNewSelect_bssid != null && !WifiProStateMachine.this.mNewSelect_bssid.equals(WifiProStateMachine.this.mBadBssid)) {
                    WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mAvailable5GAPBssid);
                    WifiProStateMachine.this.mHwDualBandBlackListMgr.addWifiBlacklist(WifiProStateMachine.this.mAvailable5GAPBssid, false);
                }
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("dualBandhandoverFailed  mAvailable5GAPBssid = " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mAvailable5GAPBssid));
                if (!(WifiProStateMachine.this.mAvailable5GAPBssid == null || WifiProStateMachine.this.mAvailable5GAPSsid == null)) {
                    WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mAvailable5GAPBssid);
                    WifiProStateMachine.this.mHwDualBandBlackListMgr.addWifiBlacklist(WifiProStateMachine.this.mAvailable5GAPBssid, false);
                }
            }
            this.isWifi2WifiProcess = false;
            WifiProStateMachine.this.uploadWifiDualBandFailReason(reason);
            this.isSwitching = false;
            if (!WifiProStateMachine.this.isWifiConnected()) {
                WifiProStateMachine.this.logI("wifi dual band handover over Failed and system auto connecting ap");
                WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[8], WifiProStateMachine.HANDOVER_CNT);
                WifiProStateMachine.this.mWifiHandover.connectWifiNetwork(WifiProStateMachine.this.mBadBssid);
            }
        }

        private void updateWifiQosLevel(boolean isWiFiNoInternet, int qosLevel) {
            WifiProStateMachine.this.refreshConnectedNetWork();
            WifiProStateMachine.this.mWiFiProEvaluateController.addEvaluateRecords(WifiProStateMachine.this.mCurrWifiInfo, 1);
            if (!WifiProStateMachine.this.mPowerManager.isScreenOn() && isWiFiNoInternet && this.mLastUpdatedQosLevel == 2) {
                return;
            }
            if (WifiProStateMachine.this.mPowerManager.isScreenOn() || isWiFiNoInternet || WifiProStateMachine.this.mCurrentWifiConfig == null || WifiProStateMachine.this.mCurrentWifiConfig.wifiProNoInternetAccess) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("updateWifiQosLevel, mCurrentSsid: " + StringUtilEx.safeDisplaySsid(WifiProStateMachine.this.mCurrentSsid) + " ,isWiFiNoInternet: " + isWiFiNoInternet + ", qosLevel: " + qosLevel);
                if (isWiFiNoInternet) {
                    this.mLastUpdatedQosLevel = 2;
                    WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 2);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 2, WifiProStateMachine.this.mCurrentSsid);
                    return;
                }
                this.mLastUpdatedQosLevel = qosLevel;
                WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 4);
                WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoLevel(WifiProStateMachine.this.mCurrentSsid, qosLevel);
                WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 4, qosLevel, 0);
                return;
            }
            this.mLastUpdatedQosLevel = qosLevel;
        }

        private boolean handleMsgBySwitchOrDialogStatus(int level) {
            if (!this.isSwitching || !this.isDialogDisplayed) {
                return this.isSwitching || !this.isAllowWiFiHandoverMobile;
            }
            if (level > 2 && level != 6) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("Dialog is  Displayed, Qos is" + level + ", Cancel dialog.");
                WifiProStateMachine.this.updateWifiInternetStateChange(level);
                WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
                boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                boolean unused2 = WifiProStateMachine.this.mIsWiFiInternetCHRFlag = false;
                wiFiLinkMonitorStateInit(false);
            }
            return true;
        }

        private void handleDualbandHandoverResult(Message msg) {
            WifiProStateMachine.this.logI("receive dual band wifi handover resust");
            if (this.isWifi2WifiProcess) {
                if (((Boolean) msg.obj).booleanValue()) {
                    if (WifiProStateMachine.this.uploadManager != null) {
                        Bundle dualbandEvent = new Bundle();
                        dualbandEvent.putInt(WifiproUtils.SWITCH_SUCCESS_INDEX, 2);
                        WifiProStateMachine.this.uploadManager.addChrBundleStat(WifiproUtils.WIFI_SWITCH_EVENT, WifiproUtils.WIFI_SWITCH_SUCC_EVENT, dualbandEvent);
                    }
                    WifiProChrUploadManager.uploadDisconnectedEvent(WifiProStateMachine.EVENT_DUALBAND_SWITCH_FINISHED);
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("dual band wifi handover is succeed, bssid = " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mNewSelect_bssid) + ", mBadBssid = " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mBadBssid));
                    this.isSwitching = false;
                    WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mBadBssid);
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    String unused = wifiProStateMachine2.mDualBandConnectApBssid = wifiProStateMachine2.mNewSelect_bssid;
                    long unused2 = WifiProStateMachine.this.mDualBandConnectTime = System.currentTimeMillis();
                    long unused3 = WifiProStateMachine.this.mWifiHandoverSucceedTimestamp = SystemClock.elapsedRealtime();
                    long unused4 = WifiProStateMachine.this.mChrDualbandConnectedStartTime = SystemClock.elapsedRealtime();
                    WifiProStateMachine.this.uploadWifiDualBandHandoverDura();
                    if (HwDualBandRelationManager.isDualBandAP(WifiProStateMachine.this.mBadBssid, WifiProStateMachine.this.mAvailable5GAPBssid)) {
                        WifiProStateMachine.this.uploadChrDualBandSameApCount();
                    }
                    if (WifiProStateMachine.this.mDuanBandHandoverType == 1) {
                        WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, false);
                    } else {
                        WifiProStateMachine.this.resetWifiEvaluteQosLevel();
                    }
                    this.isWifi2WifiProcess = false;
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiConnectedState);
                } else {
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    wifiProStateMachine4.logI("dual band wifi handover is  failure, error reason = " + msg.arg1);
                    dualBandhandoverFailed(msg.arg1);
                }
                uploadDualbandSwitchInfo(((Boolean) msg.obj).booleanValue());
            }
        }

        private void uploadDualbandSwitchInfo(boolean success) {
            Bundle data = new Bundle();
            int i = 0;
            data.putInt("Roam5GSuccCnt", (WifiProStateMachine.this.mDuanBandHandoverType != 1 || !success) ? 0 : 1);
            data.putInt("Roam5GFailedCnt", (WifiProStateMachine.this.mDuanBandHandoverType != 1 || success) ? 0 : 1);
            data.putInt("NoRoam5GSuccCnt", (WifiProStateMachine.this.mDuanBandHandoverType != 0 || !success) ? 0 : 1);
            if (WifiProStateMachine.this.mDuanBandHandoverType == 0 && !success) {
                i = 1;
            }
            data.putInt("NoRoam5GFailedCnt", i);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(WifiProStateMachine.EVENT_ID, WifiProCommonUtils.ID_UPDATE_DUAL_BAND_WIFI_INFO);
            dftEventData.putBundle(WifiProStateMachine.EVENT_DATA, data);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }

        private boolean pendingMsgBySelfCureEngine(int level) {
            WifiProStateMachine.this.logI("pendingMsgBySelfCureEngine, level = " + level + " isSwitching" + this.isSwitching + " internetFailureDetectedCnt " + this.internetFailureDetectedCnt);
            if (level == -103 && !this.isSwitching) {
                HwWifiProFeatureControl.getInstance();
                if (HwWifiProFeatureControl.isSelfCureOngoing() || !WifiProStateMachine.this.isWifiConnected()) {
                    WifiProStateMachine.this.logI("rcv EVENT_WIFI_QOS_CHANGE, level = " + level + ", but ignored because of self curing or supplicant not completed.");
                    return true;
                } else if (isSatisfySelfCureConditions()) {
                    this.internetFailureDetectedCnt++;
                    HwWifiProFeatureControl.getInstance();
                    HwWifiProFeatureControl.notifyInternetFailureDetected(false);
                    WifiProStateMachine.this.logI("rcv EVENT_WIFI_QOS_CHANGE, level = " + level + ", request to do selfcure.");
                    return true;
                }
            }
            if (level == 0 && !this.isSwitching) {
                HwWifiProFeatureControl.getInstance();
                if (HwWifiProFeatureControl.isSelfCureOngoing()) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSatisfySelfCureConditions() {
            if (this.internetFailureDetectedCnt != 0 || HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(WifiProStateMachine.this.mContext) || WifiProStateMachine.this.mCurrentRssi < -70) {
                return false;
            }
            return true;
        }

        private int getCallingSlot(Context context) {
            if (context == null) {
                Log.e(WifiProStateMachine.TAG, "getCallingSlot : context is null");
                return -1;
            }
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            int phoneCount = telephonyManager.getPhoneCount();
            for (int slotId = 0; slotId < phoneCount; slotId++) {
                int simState = telephonyManager.getSimState(slotId);
                int callState = telephonyManager.getCallStateForSlot(slotId);
                if (simState != 1 && (callState == 2 || callState == 1)) {
                    return slotId;
                }
            }
            return -1;
        }

        private boolean isCallingInCs(Context context) {
            if (!WifiProCommonUtils.isCalling(context)) {
                return false;
            }
            int slotId = getCallingSlot(context);
            int validationLength = WifiProStateMachine.this.imsRegisteredState.length;
            if (slotId <= -1 || slotId >= validationLength || WifiProStateMachine.this.imsRegisteredState[slotId]) {
                return false;
            }
            return true;
        }

        private void handleWiFiRoveOut() {
            if (this.isDisableWifiAutoSwitch || isCallingInCs(WifiProStateMachine.this.mContext)) {
                Log.w(WifiProStateMachine.TAG, "isDisableWifiAutoSwitch = " + this.isDisableWifiAutoSwitch + "isCallingInCs = " + isCallingInCs(WifiProStateMachine.this.mContext));
                this.isSwitching = false;
                if (WifiProStateMachine.this.mHandoverFailReason != 6) {
                    int unused = WifiProStateMachine.this.mHandoverFailReason = 6;
                    WifiProStateMachine.this.uploadWifiSwitchFailTypeStatistics(6);
                }
                if (this.isRssiLowOrMiddleWifi2Wifi) {
                    this.isRssiLowOrMiddleWifi2Wifi = false;
                    WifiProStateMachine.this.resetScanedRssiVariable();
                    return;
                }
                return;
            }
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            StringBuilder sb = new StringBuilder();
            sb.append("EVENT_TRY_WIFI_ROVE_OUT, allow wifi to mobile ");
            sb.append(!this.isWiFiHandoverPriority);
            wifiProStateMachine.logI(sb.toString());
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            String unused2 = wifiProStateMachine2.mBadBssid = wifiProStateMachine2.mCurrentBssid;
            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
            String unused3 = wifiProStateMachine3.mBadSsid = wifiProStateMachine3.mCurrentSsid;
            this.isWifi2WifiProcess = true;
            int threshodRssi = -82;
            if (this.isWiFiHandoverPriority) {
                threshodRssi = this.mWifi2WifiThreshod + 10;
            }
            if (!WifiProStateMachine.this.mWifiHandover.handleWifiToWifi(WifiProStateMachine.this.mNetworkBlackListManager.getWifiBlacklist(), threshodRssi, 0)) {
                if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                    WifiProStateMachine.this.logI("WiFi2WiFi failed before Scan, try to trigger selfcure");
                    pendingMsgBySelfCureEngine(WifiproUtils.REQUEST_WIFI_INET_CHECK);
                }
                wifi2WifiFailed();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleWifiEvaluteChange() {
        int accessType;
        if (isAllowWiFiAutoEvaluate()) {
            removeMessages(EVENT_WIFI_EVALUTE_TCPRTT_RESULT);
            if (this.mIsWiFiNoInternet) {
                accessType = 4;
            } else {
                accessType = 2;
            }
            if (this.mWiFiProEvaluateController.updateScoreInfoType(this.mCurrentSsid, accessType)) {
                logI("mCurrentSsid   = " + StringUtilEx.safeDisplaySsid(this.mCurrentSsid) + ", updateScoreInfoType  " + accessType);
                this.mWifiProConfigStore.updateWifiEvaluateConfig(this.mCurrentWifiConfig, 1, accessType, this.mCurrentSsid);
            }
            if (accessType == 4) {
                sendMessage(EVENT_WIFI_EVALUTE_TCPRTT_RESULT, this.mNetworkQosMonitor.getCurrentWiFiLevel());
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePeriodPortalCheck() {
        logI("receive : EVENT_PERIODIC_PORTAL_CHECK_SLOW");
        this.isPeriodicDet = true;
        if (HwAutoConnectManager.getInstance() != null) {
            HwAutoConnectManager.getInstance().checkPortalAuthExpiration();
        }
        this.detectionNumSlow++;
        sendMessageDelayed(EVENT_PERIODIC_PORTAL_CHECK_SLOW, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
    }

    /* access modifiers changed from: package-private */
    public class WiFiProVerfyingLinkState extends State {
        private int internetFailureDetectedCount;
        private volatile boolean isRecoveryWifi;
        private boolean isWifiGoodIntervalTimerOut;
        private volatile boolean isWifiHandoverWifi;
        private boolean isWifiRecoveryTimerOut;
        private boolean isWifiScanScreenOff;
        private int mFailedDetectedCount;
        private int mLastLinkPoorRssi;
        private HashMap<Integer, String> mTopAppWhiteList;
        private int wifiInternetLevel;
        private int wifiNoInternetCounter;
        private int wifiQosLevel;
        private int wifiScanCounter;

        WiFiProVerfyingLinkState() {
        }

        private void startScanAndMonitor(long time) {
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI, WifiProStateMachine.DELAY_UPLOAD_MS);
            WifiProStateMachine.this.mNetworkQosMonitor.setIpQosEnabled(true);
            WifiProStateMachine.this.mNetworkQosMonitor.setMonitorMobileQos(true);
            if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                int unused = WifiProStateMachine.this.mCurrentWifiLevel = -1;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("WiFiProVerfyingLinkState, wifi is No Internet,delay check time = " + time);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, time);
                return;
            }
            WifiProStateMachine.this.mNetworkQosMonitor.setMonitorWifiQos(2, true);
        }

        private void cancelScanAndMonitor() {
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI);
            WifiProStateMachine.this.mNetworkQosMonitor.setIpQosEnabled(false);
            WifiProStateMachine.this.mNetworkQosMonitor.setMonitorMobileQos(false);
            WifiProStateMachine.this.mNetworkQosMonitor.setMonitorWifiQos(2, false);
        }

        private void restoreWifiConnect() {
            if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE)) {
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE);
            }
            cancelScanAndMonitor();
            WifiProStateMachine.this.logI("restoreWifiConnect, mWsmChannel send GOOD Link Detected");
            WifiProStateMachine.this.mWsmChannel.sendMessage(131874);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.notifyManualConnectAP(wifiProStateMachine.mIsUserManualConnectSuccess, WifiProStateMachine.this.mIsUserHandoverWiFi);
            WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[7], WifiProStateMachine.HANDOVER_CNT);
        }

        public void enter() {
            WifiProStateMachine.this.logI("WiFiProVerfyingLinkState is Enter");
            this.isRecoveryWifi = false;
            this.isWifiHandoverWifi = false;
            this.isWifiRecoveryTimerOut = false;
            this.isWifiGoodIntervalTimerOut = true;
            boolean unused = WifiProStateMachine.this.mIsUserManualConnectSuccess = false;
            this.wifiNoInternetCounter = 0;
            this.internetFailureDetectedCount = 0;
            WifiProStateMachine.this.mWifiProUIDisplayManager.cancelAllDialog();
            this.wifiScanCounter = 0;
            this.isWifiScanScreenOff = false;
            this.mLastLinkPoorRssi = -127;
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            int unused2 = wifiProStateMachine.mChrQosLevelBeforeHandover = wifiProStateMachine.mCurrentWifiConfig.networkQosLevel;
            if (WifiProStateMachine.this.mCurrentVerfyCounter > 2) {
                int unused3 = WifiProStateMachine.this.mCurrentVerfyCounter = 2;
            }
            long delayTime = ((long) Math.pow(2.0d, (double) WifiProStateMachine.this.mCurrentVerfyCounter)) * 60 * 1000;
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.logI("WiFiProVerfyingLinkState : CurrentWifiLevel = " + WifiProStateMachine.this.mCurrentWifiLevel + ", CurrentVerfyCounter = " + WifiProStateMachine.this.mCurrentVerfyCounter + ", delayTime = " + delayTime);
            WifiProStateMachine.access$15308(WifiProStateMachine.this);
            startScanAndMonitor(delayTime);
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_WIFI_RECOVERY_TIMEOUT, delayTime);
            if (WifiProStateMachine.this.mCurrentVerfyCounter == 2) {
                WifiProStateMachine.this.logW("network has handover 3 times,maybe ping-pong");
                WifiProStateMachine.this.logI("UNEXPECT_SWITCH_EVENT: pingPong: enter:");
                WifiProStateMachine.this.uploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, "pingPong");
                WifiProStateMachine.this.mWifiProStatisticsManager.increasePingPongCount();
            }
            HwWifiConnectivityMonitor.getInstance().notifyVerifyingLinkState(true);
            WifiProStateMachine.this.mNetworkQosMonitor.setRoveOutToMobileState(1);
            if (!WifiProStateMachine.this.mIsWiFiNoInternet) {
                WifiProStateMachine.this.logI("BQE bad rove out started.");
                long unused4 = WifiProStateMachine.this.mChrRoveOutStartTime = System.currentTimeMillis();
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                String unused5 = wifiProStateMachine3.mRoSsid = wifiProStateMachine3.mCurrentSsid;
                boolean unused6 = WifiProStateMachine.this.mLoseInetRoveOut = false;
            } else {
                boolean unused7 = WifiProStateMachine.this.mLoseInetRoveOut = true;
            }
            boolean unused8 = WifiProStateMachine.this.mRoveOutStarted = true;
            boolean unused9 = WifiProStateMachine.this.mIsRoveOutToDisconn = false;
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_LAA_STATUS_CHANGED, 3000);
            this.mFailedDetectedCount = 0;
            HashMap<Integer, String> hashMap = this.mTopAppWhiteList;
            if (hashMap == null || hashMap.isEmpty()) {
                this.mTopAppWhiteList = WifiProCommonUtils.getAppInWhitelist();
            }
            WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
            wifiProStateMachine4.logI("WiFiProVerfyingLinkState mNotifyWifiLinkPoorReason=" + WifiProStateMachine.this.mNotifyWifiLinkPoorReason);
            if (!WifiProStateMachine.this.mIsWifi2CellInStrongSignalEnabled) {
                return;
            }
            if (WifiProStateMachine.this.mNotifyWifiLinkPoorReason == 207 || WifiProStateMachine.this.mNotifyWifiLinkPoorReason == 208) {
                this.mLastLinkPoorRssi = WifiProCommonUtils.getCurrentRssi(WifiProStateMachine.this.mWifiManager);
                WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                wifiProStateMachine5.logI("WiFiProVerfyingLinkState mLastLinkPoorRssi=" + this.mLastLinkPoorRssi);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 15000);
            }
        }

        public void exit() {
            WifiProStateMachine.this.logI("WiFiProVerfyingLinkState is Exit");
            cancelScanAndMonitor();
            boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = false;
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_GOOD_INTERVAL_TIMEOUT);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_RECOVERY_TIMEOUT);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_MOBILE_SWITCH_DELAY);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_LAA_STATUS_CHANGED);
            WifiProStateMachine.this.mNetworkQosMonitor.setRoveOutToMobileState(0);
            HwWifiConnectivityMonitor.getInstance().notifyVerifyingLinkState(false);
            this.mFailedDetectedCount = 0;
            int unused2 = WifiProStateMachine.this.mNotifyWifiLinkPoorReason = -1;
            this.mLastLinkPoorRssi = -127;
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            switch (i) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                    handleWifiNetworkStateChange(msg);
                    break;
                case WifiProStateMachine.EVENT_DEVICE_SCREEN_ON /*{ENCODED_INT: 136170}*/:
                    handleDeviceScreenOn();
                    break;
                case WifiProStateMachine.EVENT_WIFIPRO_WORKING_STATE_CHANGE /*{ENCODED_INT: 136171}*/:
                    handleWifiProStateChange();
                    break;
                case WifiProStateMachine.EVENT_WIFI_QOS_CHANGE /*{ENCODED_INT: 136172}*/:
                    handleWifiQosChangedInVerifyLinkState(msg);
                    break;
                case WifiProStateMachine.EVENT_MOBILE_QOS_CHANGE /*{ENCODED_INT: 136173}*/:
                    handleMobileQosChange(msg);
                    break;
                default:
                    switch (i) {
                        case WifiProStateMachine.EVENT_CHECK_AVAILABLE_AP_RESULT /*{ENCODED_INT: 136176}*/:
                            handleCheckAvalableApResult(msg);
                            break;
                        case WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE /*{ENCODED_INT: 136177}*/:
                            handleNetworkConnectivityChange(true);
                            break;
                        case WifiProStateMachine.EVENT_WIFI_HANDOVER_WIFI_RESULT /*{ENCODED_INT: 136178}*/:
                            handleWifiHandoverWifiResult(msg);
                            break;
                        default:
                            switch (i) {
                                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT /*{ENCODED_INT: 136181}*/:
                                    handleCheckInternetResultInVerifyLinkState(msg);
                                    break;
                                case WifiProStateMachine.EVENT_DIALOG_OK /*{ENCODED_INT: 136182}*/:
                                case WifiProStateMachine.EVENT_DIALOG_CANCEL /*{ENCODED_INT: 136183}*/:
                                    break;
                                default:
                                    switch (i) {
                                        case WifiProStateMachine.EVENT_WIFI_STATE_CHANGED_ACTION /*{ENCODED_INT: 136185}*/:
                                            handleWifiStateChange();
                                            break;
                                        case WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION /*{ENCODED_INT: 136186}*/:
                                            handleMobileDataStateChange();
                                            break;
                                        case WifiProStateMachine.EVENT_WIFI_GOOD_INTERVAL_TIMEOUT /*{ENCODED_INT: 136187}*/:
                                            this.isWifiGoodIntervalTimerOut = true;
                                            break;
                                        case WifiProStateMachine.EVENT_WIFI_RECOVERY_TIMEOUT /*{ENCODED_INT: 136188}*/:
                                            this.isWifiRecoveryTimerOut = true;
                                            WifiProStateMachine.this.logI("isWifiRecoveryTimerOut is true,mobile can handover to wifi");
                                            break;
                                        case WifiProStateMachine.EVENT_MOBILE_RECOVERY_TO_WIFI /*{ENCODED_INT: 136189}*/:
                                            WifiProStateMachine.this.logW("WiFiProVerfyingLinkState::EVENT_MOBILE_RECOVERY_TO_WIFI, handle it.");
                                            this.isWifiHandoverWifi = false;
                                            restoreWifiConnect();
                                            WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(3);
                                            break;
                                        default:
                                            switch (i) {
                                                case WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI /*{ENCODED_INT: 136191}*/:
                                                    handleRetryWifiToWifi();
                                                    break;
                                                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET /*{ENCODED_INT: 136192}*/:
                                                    handleCheckInternetInVerifyLinkState(msg);
                                                    break;
                                                case WifiProStateMachine.EVENT_USER_ROVE_IN /*{ENCODED_INT: 136193}*/:
                                                    handleUserRoveIn();
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case WifiProStateMachine.EVENT_HTTP_REACHABLE_RESULT /*{ENCODED_INT: 136195}*/:
                                                            WifiProStateMachine.this.logW("WiFiProVerfyingLinkState::EVENT_HTTP_REACHABLE_RESULT, handle it.");
                                                            this.isRecoveryWifi = false;
                                                            if (!(msg.obj instanceof Boolean)) {
                                                                WifiProStateMachine.this.logE("WiFiProVerfyingLinkState::EVENT_HTTP_REACHABLE_RESULT, Class is not match");
                                                                break;
                                                            } else {
                                                                boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = ((Boolean) msg.obj).booleanValue();
                                                                break;
                                                            }
                                                        case WifiProStateMachine.EVENT_LAA_STATUS_CHANGED /*{ENCODED_INT: 136200}*/:
                                                            Bundle data = new Bundle();
                                                            data.putBoolean("is24gConnected", !WifiProCommonUtils.isWifi5GConnected(WifiProStateMachine.this.mWifiManager));
                                                            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 31, data);
                                                            break;
                                                        case WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE /*{ENCODED_INT: 136209}*/:
                                                            handleNetworkConnectivityChange(false);
                                                            break;
                                                        case WifiProStateMachine.EVENT_SCAN_RESULTS_AVAILABLE /*{ENCODED_INT: 136293}*/:
                                                            break;
                                                        default:
                                                            switch (i) {
                                                                case WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND /*{ENCODED_INT: 136211}*/:
                                                                    handleStrongRssiDetectInternet();
                                                                    break;
                                                                case WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_LATENCY /*{ENCODED_INT: 136212}*/:
                                                                    WifiProStateMachine.this.logI("aceess server has spent more than one second!!!");
                                                                    handleStrongRssiRebackToWiFi(599, 1000);
                                                                    break;
                                                                case WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_RESULT /*{ENCODED_INT: 136213}*/:
                                                                    WifiProStateMachine.this.logI("detect result has return!!!");
                                                                    if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_LATENCY)) {
                                                                        WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_LATENCY);
                                                                        handleStrongRssiRebackToWiFi(msg.arg1, (long) msg.arg2);
                                                                        break;
                                                                    }
                                                                    break;
                                                                default:
                                                                    return false;
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
            return true;
        }

        private void handleStrongRssiDetectInternet() {
            WifiInfo conInfo = WifiProStateMachine.this.mWifiManager.getConnectionInfo();
            if (conInfo == null) {
                WifiProStateMachine.this.logE("WifiInfo is null, exit!!");
                return;
            }
            int chload = conInfo.getChload();
            if (chload <= -1) {
                WifiProStateMachine.this.logE("WiFi chip does not support channel load parameter query");
                chload = 0;
            }
            int currentSignalLevel = WifiProCommonUtils.getCurrenSignalLevel(conInfo);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("OTA para signal level=" + currentSignalLevel + " chload=" + chload);
            if (currentSignalLevel <= 2 || ((conInfo.is24GHz() && chload > 500 && WifiProCommonUtils.getEnterpriseCount(WifiProStateMachine.this.mWifiManager) > 5) || (conInfo.is5GHz() && chload > 500 && WifiProCommonUtils.getEnterpriseCount(WifiProStateMachine.this.mWifiManager) > 5))) {
                WifiProStateMachine.this.logE("OTA para does not meet the requirements!!");
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 15000);
                return;
            }
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_LATENCY, 1000);
            new NetworkCheckThread(false, false).start();
        }

        private void handleStrongRssiRebackToWiFi(int responseCode, long detectTime) {
            if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND)) {
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND);
            }
            WifiInfo info = WifiProStateMachine.this.mWifiManager.getConnectionInfo();
            if (info == null) {
                WifiProStateMachine.this.logI("WifiInfo is null");
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 60000);
                return;
            }
            int txRate = info.getTxLinkSpeedMbps();
            int rxRate = info.getRxLinkSpeedMbps();
            WifiProStateMachine.this.logI("txRate = " + txRate + ", rxRate = " + rxRate + ", mFailedDetectedCount" + this.mFailedDetectedCount + ", responseCode = " + responseCode + ", detectTime = " + detectTime);
            if (responseCode != 204 || detectTime > 700 || ((info.is24GHz() && (txRate <= 13 || rxRate <= 27)) || (info.is5GHz() && (txRate <= 27 || rxRate <= 40)))) {
                WifiProStateMachine.this.logI("Para does not meet the requirements");
                this.mFailedDetectedCount++;
                int i = this.mFailedDetectedCount;
                if (i >= 3) {
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 60000);
                } else if (i >= 2) {
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 30000);
                } else {
                    WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 15000);
                }
            } else {
                this.mFailedDetectedCount = 0;
                rebackToWifi();
            }
        }

        private void rebackToWifi() {
            long unused = WifiProStateMachine.this.mCurrentTime = SystemClock.elapsedRealtime();
            long deltaTime = WifiProStateMachine.this.mCurrentTime - WifiProStateMachine.this.mLastTime;
            int deltaRssi = -127;
            if (!(WifiProCommonUtils.getCurrentRssi(WifiProStateMachine.this.mWifiManager) == -127 || this.mLastLinkPoorRssi == -127)) {
                deltaRssi = WifiProCommonUtils.getCurrentRssi(WifiProStateMachine.this.mWifiManager) - this.mLastLinkPoorRssi;
            }
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("mLastTime = " + WifiProStateMachine.this.mLastTime + ", mCurrentTime = " + WifiProStateMachine.this.mCurrentTime + ", deltaTime = " + deltaTime + ", deltaRssi = " + deltaRssi);
            if (WifiProStateMachine.this.mLastTime == 0 || deltaTime >= 300000 || (deltaTime >= 60000 && deltaRssi >= 10)) {
                WifiProStateMachine.this.notifyWifiLinkPoor(false, 0);
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                long unused2 = wifiProStateMachine2.mLastTime = wifiProStateMachine2.mCurrentTime;
                WifiProStateMachine.this.logI("notify link good");
            } else {
                WifiProStateMachine.this.logI("continue to monitor");
            }
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_BACKGROUND, 30000);
        }

        private void handleDeviceScreenOn() {
            if (this.isWifiScanScreenOff) {
                WifiProStateMachine.this.logI("isWifiScanScreenOff = true, retry scan wifi");
                this.isWifiScanScreenOff = false;
                this.wifiScanCounter = 0;
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI);
                return;
            }
            WifiProStateMachine.this.logI("isWifiScanScreenOff = false, wait a moment, retry scan wifi later");
        }

        private void handleUserRoveIn() {
            boolean unused = WifiProStateMachine.this.mIsUserHandoverWiFi = true;
            this.isWifiHandoverWifi = false;
            if (WifiProStateMachine.this.mIsWiFiNoInternet) {
                WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetUserManualRICount();
            } else {
                Log.i(WifiProStateMachine.TAG, "UNEXPECT_SWITCH_EVENT: USER_SELECT_OLD: enter:");
                WifiProStateMachine.this.uploadManager.addChrSsidCntStat(WifiproUtils.UNEXPECTED_WIFI_SWITCH_EVENT, "userSelectOld");
                WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(4);
            }
            WifiProStateMachine.this.uploadChrHandoverUnexpectedTypes(WifiProStateMachine.WIFI_HANDOVER_UNEXPECTED_TYPES[1]);
            restoreWifiConnect();
        }

        private void handleWifiHandoverWifiResult(Message msg) {
            this.isWifiHandoverWifi = false;
            if (((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("connect other AP wifi : succeed ,go to WifiConnectedState, add WifiBlacklist: " + StringUtilEx.safeDisplayBssid(WifiProStateMachine.this.mCurrentBssid));
                WifiProStateMachine.this.resetWifiEvaluteQosLevel();
                WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mCurrentBssid);
                cancelScanAndMonitor();
                WifiProStateMachine.this.refreshConnectedNetWork();
                WifiProStateMachine.this.mWiFiProEvaluateController.reSetEvaluateRecord(WifiProStateMachine.this.mCurrentSsid);
                WifiProStateMachine.this.mWifiProConfigStore.cleanWifiProConfig(WifiProStateMachine.this.mCurrentWifiConfig);
                restoreWifiConnect();
            } else if (WifiProStateMachine.this.mNewSelect_bssid != null && !WifiProStateMachine.this.mNewSelect_bssid.equals(WifiProStateMachine.this.mCurrentBssid)) {
                WifiProStateMachine.this.logW("connect other AP wifi : Fallure");
                WifiProStateMachine.this.mNetworkBlackListManager.addWifiBlacklist(WifiProStateMachine.this.mNewSelect_bssid);
                WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[8], WifiProStateMachine.HANDOVER_CNT);
                WifiProStateMachine.this.mWifiHandover.connectWifiNetwork(WifiProStateMachine.this.mCurrentBssid);
            } else if (WifiProStateMachine.this.isWifiConnected()) {
                WifiProStateMachine.this.logI("wifi handover wifi fail, continue monitor");
            }
        }

        private void handleCheckAvalableApResult(Message msg) {
            if (this.isRecoveryWifi || this.isWifiHandoverWifi) {
                WifiProStateMachine.this.logI("receive check available ap result,but is isRecoveryWifi");
            } else if (((Boolean) msg.obj).booleanValue()) {
                WifiProStateMachine.this.logI("Exist a vailable AP,connect this AP and cancel Sacn Timer");
                this.isWifiHandoverWifi = true;
                if (!WifiProStateMachine.this.mWifiHandover.handleWifiToWifi(WifiProStateMachine.this.mNetworkBlackListManager.getWifiBlacklist(), -82, 0)) {
                    this.isWifiHandoverWifi = false;
                }
            } else {
                WifiProStateMachine.this.logE("There is no vailble ap, continue verfyinglink");
            }
        }

        private void handleNetworkConnectivityChange(boolean isNeedCheckDualCard) {
            NetworkInfo mobileInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(0);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("networkConnetc change :mobileInfo : " + mobileInfo + ", mIsMobileDataEnabled = " + WifiProStateMachine.this.mIsMobileDataEnabled);
            if (WifiProStateMachine.this.mIsMobileDataEnabled && mobileInfo != null && NetworkInfo.State.DISCONNECTED == mobileInfo.getState()) {
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI("mobile network service is disconnected, mIsWiFiNoInternet = " + WifiProStateMachine.this.mIsWiFiNoInternet);
                NetworkInfo wInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(1);
                if (!WifiProStateMachine.this.mIsWiFiNoInternet && wInfo != null && NetworkInfo.DetailedState.VERIFYING_POOR_LINK == wInfo.getDetailedState()) {
                    int isSmartDualCardState = 0;
                    if (isNeedCheckDualCard) {
                        isSmartDualCardState = SystemProperties.getInt(WifiProStateMachine.KEY_SMART_DUAL_CARD_STATE, 0);
                    }
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.logI("isSmartDualCardState = " + isSmartDualCardState);
                    if (isSmartDualCardState == 1) {
                        if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE)) {
                            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE);
                        }
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_RECHECK_SMART_CARD_STATE, 10000);
                        return;
                    }
                    this.isWifiHandoverWifi = false;
                    restoreWifiConnect();
                }
            } else if (!WifiProStateMachine.this.mIsMobileDataEnabled || mobileInfo == null || mobileInfo.getState() != NetworkInfo.State.CONNECTED) {
                WifiProStateMachine.this.logI("Skip this ConnectivityChange at this time");
            } else {
                handleSocketStrategy();
            }
        }

        private void handleSocketStrategy() {
            HwAutoConnectManager autoConnectManager = HwAutoConnectManager.getInstance();
            if (autoConnectManager == null) {
                WifiProStateMachine.this.logE("HwAutoConnectManager is null, return!!");
                return;
            }
            int topUid = autoConnectManager.getCurrentTopUid();
            String pktName = autoConnectManager.getCurrentPackageName();
            HashMap<Integer, String> hashMap = this.mTopAppWhiteList;
            if (hashMap == null || hashMap.isEmpty() || TextUtils.isEmpty(pktName) || topUid <= 0) {
                WifiProStateMachine.this.logE("params do not meet the requirement, return!!");
                return;
            }
            Object obj = HwFrameworkFactory.getHwInnerNetworkManager();
            if (!(obj instanceof HwInnerNetworkManagerImpl)) {
                WifiProStateMachine.this.logE("obj is not instanceof HwInnerNetworkManagerImpl, return!!");
                return;
            }
            HwInnerNetworkManagerImpl hwInnerNetworkManagerImpl = (HwInnerNetworkManagerImpl) obj;
            HashMap<Integer, String> hashMap2 = this.mTopAppWhiteList;
            if (hashMap2 != null && hashMap2.containsValue(pktName)) {
                hwInnerNetworkManagerImpl.closeSocketsForUid(topUid);
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("TCP closeSocketsForUid = " + topUid + " pktName=" + pktName);
            }
        }

        private void handleWifiNetworkStateChange(Message msg) {
            Intent intent = null;
            if (msg.obj instanceof Intent) {
                intent = (Intent) msg.obj;
            }
            if (intent != null && !this.isWifiHandoverWifi) {
                Object objNetworkInfo = intent.getParcelableExtra("networkInfo");
                NetworkInfo networkInfo = null;
                if (objNetworkInfo instanceof NetworkInfo) {
                    networkInfo = (NetworkInfo) objNetworkInfo;
                } else {
                    WifiProStateMachine.this.logE("handleWifiNetworkStateChange:Class is not match");
                }
                if (networkInfo != null) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("WiFiProVerfyingLinkState :Network state change " + networkInfo.getDetailedState());
                }
                if (networkInfo != null && NetworkInfo.State.DISCONNECTED == networkInfo.getState()) {
                    WifiProStateMachine.this.logI("WiFiProVerfyingLinkState : wifi has disconnected");
                    boolean unused = WifiProStateMachine.this.mIsRoveOutToDisconn = true;
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiDisConnectedState);
                } else if (networkInfo != null && NetworkInfo.State.CONNECTED == networkInfo.getState()) {
                    this.isRecoveryWifi = false;
                    String unused2 = WifiProStateMachine.this.mChrWifiHandoverType = WifiProStateMachine.WIFI_HANDOVER_TYPES[7];
                    WifiProStateMachine.this.uploadChrWifiHandoverTypeStatistics(WifiProStateMachine.WIFI_HANDOVER_TYPES[7], WifiProStateMachine.HANDOVER_SUCC_CNT);
                    WifiProStateMachine.this.mWifiProUIDisplayManager.showWifiProToast(4);
                    WifiProStateMachine.this.logI("WiFiProVerfyingLinkState: Restore the wifi successful,go to mWiFiLinkMonitorState");
                    WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, true);
                    boolean unused3 = WifiProStateMachine.this.mVerfyingToConnectedState = true;
                    WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                    wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiConnectedState);
                }
            }
        }

        private void handleMobileQosChange(Message msg) {
            if (msg.arg1 <= 2 && this.isWifiRecoveryTimerOut) {
                if (WifiProStateMachine.this.mIsWiFiNoInternet && msg.arg1 <= 0) {
                    WifiProStateMachine.this.logI("both wifi and mobile is unusable,can not restore wifi ");
                } else if (!this.isRecoveryWifi && !this.isWifiHandoverWifi && WifiProStateMachine.this.mCurrentWifiLevel != 0) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    if (!wifiProStateMachine.isWiFiPoorer(wifiProStateMachine.mCurrentWifiLevel, msg.arg1)) {
                        WifiProStateMachine.this.logI("Mobile Qos is poor,try restore wifi,mobile handover wifi");
                        this.isRecoveryWifi = true;
                        restoreWifiConnect();
                        WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(7);
                    }
                }
            }
        }

        private void handleRetryWifiToWifi() {
            if (WifiProStateMachine.this.mPowerManager.isScreenOn()) {
                WifiProStateMachine.this.logI("inquire the surrounding AP for wifiHandover");
                WifiProStateMachine.this.mWifiHandover.hasAvailableWifiNetwork(WifiProStateMachine.this.mNetworkBlackListManager.getWifiBlacklist(), -82, WifiProStateMachine.this.mCurrentBssid, WifiProStateMachine.this.mCurrentSsid);
                this.wifiScanCounter++;
                int i = this.wifiScanCounter;
                if (i > 12) {
                    i = 12;
                }
                this.wifiScanCounter = i;
                long delayScanTime = ((long) Math.pow(2.0d, (double) (this.wifiScanCounter / 4))) * 60 * 1000;
                WifiProStateMachine.this.logI("delayScanTime = " + delayScanTime);
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_RETRY_WIFI_TO_WIFI, delayScanTime);
                return;
            }
            this.isWifiScanScreenOff = true;
        }

        private void handleCheckInternetResultInVerifyLinkState(Message msg) {
            int i;
            this.wifiInternetLevel = msg.arg1;
            WifiProStateMachine.this.logI("WiFi internet level = " + this.wifiInternetLevel + ", wifiQosLevel = " + this.wifiQosLevel + ", isRecoveryWifi = " + this.isRecoveryWifi);
            WifiProStateMachine.this.logI("mIsWiFiNoInternet = " + WifiProStateMachine.this.mIsWiFiNoInternet + ", isWifiHandoverWifi = " + this.isWifiHandoverWifi + ", isWifiRecoveryTimerOut = " + this.isWifiRecoveryTimerOut);
            if (this.isWifiRecoveryTimerOut) {
                int i2 = this.wifiInternetLevel;
                if (i2 == -1 || i2 == 6) {
                    WifiProStateMachine.this.logI("WiFiProVerfyingLinkState wifi no internet detected time = " + this.wifiNoInternetCounter);
                    this.wifiQosLevel = 0;
                    this.wifiNoInternetCounter = this.wifiNoInternetCounter + 1;
                    if (this.wifiInternetLevel == -1 && this.internetFailureDetectedCount == 0) {
                        HwWifiProFeatureControl.getInstance();
                        if (!HwWifiProFeatureControl.isSelfCureOngoing() && WifiProStateMachine.this.isWifiConnected()) {
                            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                            int unused = wifiProStateMachine.mCurrentRssi = WifiProCommonUtils.getCurrentRssi(wifiProStateMachine.mWifiManager);
                            WifiProStateMachine.this.logI("WiFiProVerfyingLinkState mCurrentRssi = " + WifiProStateMachine.this.mCurrentRssi);
                            if (WifiProStateMachine.this.mCurrentRssi >= -70) {
                                HwWifiProFeatureControl.getInstance();
                                HwWifiProFeatureControl.notifyInternetFailureDetected(true);
                                this.internetFailureDetectedCount++;
                            }
                        }
                    }
                }
                if (!this.isWifiHandoverWifi && this.isRecoveryWifi) {
                    int i3 = this.wifiInternetLevel;
                    if (i3 == -1 || i3 == 6 || this.wifiQosLevel <= 2) {
                        this.isRecoveryWifi = false;
                    } else {
                        WifiProStateMachine.this.logI("wifi Qos is [" + this.wifiQosLevel + " ]Ok, wifiInternetLevel is [" + this.wifiInternetLevel + "] Restore the wifi connection");
                        WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(1);
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 4);
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoLevel(WifiProStateMachine.this.mCurrentSsid, 3);
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 4, 3, 0);
                        restoreWifiConnect();
                    }
                }
                if (WifiProStateMachine.this.mIsWiFiNoInternet && (i = this.wifiInternetLevel) != -1 && i != 6 && !this.isWifiHandoverWifi) {
                    boolean unused2 = WifiProStateMachine.this.mIsWiFiNoInternet = false;
                    if (!this.isRecoveryWifi) {
                        this.isRecoveryWifi = true;
                        WifiProStateMachine.this.logI("wifi Internet is better ,try restore wifi 2,mobile handover wifi");
                        WifiProStateMachine.this.mWifiProStatisticsManager.increaseNotInetRestoreRICount();
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(WifiProStateMachine.this.mCurrentSsid, 4);
                        WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mCurrentWifiConfig, 1, 4, WifiProStateMachine.this.mCurrentSsid);
                        restoreWifiConnect();
                    }
                }
                updateCurrentWifiInternetStatus();
            }
        }

        private void updateCurrentWifiInternetStatus() {
            if (!WifiProStateMachine.this.mIsWiFiNoInternet) {
                int i = this.wifiInternetLevel;
                if (i == -1 || i == 6) {
                    boolean unused = WifiProStateMachine.this.mIsWiFiNoInternet = true;
                    if (this.isRecoveryWifi && !this.isWifiHandoverWifi) {
                        this.isRecoveryWifi = false;
                    }
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("WiFiProVerfyingLinkState updateCurrentWifiInternetStatus mIsWiFiNoInternet = " + WifiProStateMachine.this.mIsWiFiNoInternet + ", isRecoveryWifi = " + this.isRecoveryWifi);
                }
            }
        }

        private void handleCheckInternetInVerifyLinkState(Message msg) {
            WifiProStateMachine.this.logW("start check wifi internet, wifiNoInternetCounter = " + this.wifiNoInternetCounter);
            int i = this.wifiNoInternetCounter;
            if (i > 12) {
                i = 12;
            }
            this.wifiNoInternetCounter = i;
            WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, true);
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET, ((long) Math.pow(2.0d, Math.floor(((double) this.wifiNoInternetCounter) / 4.0d))) * 30000);
        }

        private void handleWifiProStateChange() {
            if (!WifiProStateMachine.this.mIsWiFiProEnabled || !WifiProStateMachine.this.mIsPrimaryUser) {
                WifiProStateMachine.this.logI("wifiprochr user close wifipro");
                WifiProStateMachine.this.mWifiProStatisticsManager.sendWifiproRoveInEvent(5);
                boolean unused = WifiProStateMachine.this.mRoveOutStarted = false;
                WifiProStateMachine.this.onDisableWiFiPro();
            }
        }

        private void handleMobileDataStateChange() {
            if (!WifiProStateMachine.this.mIsMobileDataEnabled) {
                int delayMs = WifiProStateMachine.this.mIsWiFiNoInternet ? 3000 : 0;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("In verifying link state, MOBILE DATA is OFF, try to delay " + delayMs + " ms to switch back to wifi.");
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.sendMessageDelayed(wifiProStateMachine2.obtainMessage(WifiProStateMachine.EVENT_MOBILE_RECOVERY_TO_WIFI), (long) delayMs);
            } else if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_MOBILE_RECOVERY_TO_WIFI)) {
                WifiProStateMachine.this.logI("In verifying link state, MOBILE DATA is ON within delay time, cancel switching back to wifi.");
                WifiProStateMachine.this.getHandler().removeMessages(WifiProStateMachine.EVENT_MOBILE_RECOVERY_TO_WIFI);
            }
        }

        private void handleWifiStateChange() {
            if (WifiProStateMachine.this.mWifiManager.getWifiState() == 1) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("wifi state is : " + WifiProStateMachine.this.mWifiManager.getWifiState() + " ,go to wifi disconnected");
                boolean unused = WifiProStateMachine.this.mIsRoveOutToDisconn = true;
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiDisConnectedState);
            } else if (WifiProStateMachine.this.mWifiManager.getWifiState() == 3) {
                WifiProStateMachine.this.logI("wifi state is : enabled, forgetUntrustedOpenAp");
                WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            }
        }

        private void handleWifiQosChangedInVerifyLinkState(Message msg) {
            boolean updateUiOnly = false;
            if (msg.obj instanceof Boolean) {
                updateUiOnly = ((Boolean) msg.obj).booleanValue();
            } else {
                WifiProStateMachine.this.logE("handleWifiQosChangedInVerifyLinkState:Class is not match");
            }
            if (updateUiOnly) {
                WifiProStateMachine.this.logI("wifi is connected background, no UI to update");
                return;
            }
            if (msg.arg1 == 3) {
                this.isWifiRecoveryTimerOut = true;
                WifiProStateMachine.this.logI("force to switch to wifi if good enough.");
            }
            HwWifiProFeatureControl.getInstance();
            boolean isSelfCureOngoing = HwWifiProFeatureControl.isSelfCureOngoing();
            if (this.isRecoveryWifi || this.isWifiHandoverWifi || !this.isWifiRecoveryTimerOut || !WifiProStateMachine.this.isWifiConnected() || isSelfCureOngoing) {
                WifiProStateMachine.this.logI("isWifiHandoverWifi = " + this.isWifiHandoverWifi + ", isWifiRecoveryTimerOut = " + this.isWifiRecoveryTimerOut + ", isRecoveryWifi = " + this.isRecoveryWifi + ", isSelfCureOngoing = " + isSelfCureOngoing);
                return;
            }
            this.wifiQosLevel = msg.arg1;
            if (this.wifiQosLevel > 2 && this.isWifiGoodIntervalTimerOut) {
                WifiProStateMachine.this.logI("wifi Qos is [" + this.wifiQosLevel + " ]Ok, start check wifi internet, wifiNoInternetCounter = " + this.wifiNoInternetCounter);
                this.isRecoveryWifi = true;
                this.isWifiGoodIntervalTimerOut = false;
                int i = this.wifiNoInternetCounter;
                if (i > 12) {
                    i = 12;
                }
                this.wifiNoInternetCounter = i;
                long wifiCheckDelayTime = ((long) Math.pow(2.0d, Math.floor(((double) this.wifiNoInternetCounter) / 4.0d))) * 30000;
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_WIFI_GOOD_INTERVAL_TIMEOUT, wifiCheckDelayTime);
                WifiProStateMachine.this.logI("WifiCheckDelayTime=" + wifiCheckDelayTime);
                WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, true);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class WifiDisConnectedState extends State {
        WifiDisConnectedState() {
        }

        public void enter() {
            String unused = WifiProStateMachine.this.mChrWifiHandoverType = "";
            int unused2 = WifiProStateMachine.this.mChrQosLevelBeforeHandover = 0;
            long unused3 = WifiProStateMachine.this.mWifiHandoverStartTime = 0;
            boolean unused4 = WifiProStateMachine.this.mIsChrQosBetterAfterDualbandHandover = false;
            if (WifiProStateMachine.this.getHandler().hasMessages(WifiProStateMachine.EVENT_CHR_CHECK_WIFI_HANDOVER)) {
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_CHR_CHECK_WIFI_HANDOVER);
            }
            if (WifiProStateMachine.this.mChrDualbandConnectedStartTime != 0) {
                WifiProStateMachine.this.uploadChrDualBandOnLineTime();
                long unused5 = WifiProStateMachine.this.mChrDualbandConnectedStartTime = 0;
            }
            boolean unused6 = WifiProStateMachine.mIsWifiManualEvaluating = false;
            boolean unused7 = WifiProStateMachine.mIsWifiSemiAutoEvaluating = false;
            WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            WifiProStateMachine.this.setWifiEvaluateTag(false);
            if (WifiProStateMachine.this.mOpenAvailableAPCounter >= 2) {
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(10);
                int unused8 = WifiProStateMachine.this.mOpenAvailableAPCounter = 0;
            }
            WifiProStateMachine.this.logI("WifiDisConnectedState is Enter");
            long unused9 = WifiProStateMachine.this.mLastDisconnectedTime = System.currentTimeMillis();
            boolean unused10 = WifiProStateMachine.this.mIsPortalAp = false;
            boolean unused11 = WifiProStateMachine.this.mIsNetworkAuthen = false;
            WifiProStateMachine.this.resetWifiEvaluteQosLevel();
            WifiProStateMachine.this.resetVariables();
            boolean unused12 = WifiProStateMachine.this.mVerfyingToConnectedState = false;
            if (0 != WifiProStateMachine.this.mChrRoveOutStartTime) {
                WifiProStateMachine.this.logI("BQE bad rove out, disconnect time recorded.");
                long unused13 = WifiProStateMachine.this.mChrWifiDisconnectStartTime = System.currentTimeMillis();
            }
            if (WifiProStateMachine.this.mRoveOutStarted && WifiProStateMachine.this.mIsRoveOutToDisconn) {
                if (WifiProStateMachine.this.mLoseInetRoveOut) {
                    WifiProStateMachine.this.logI("Not Inet rove out and WIFI disconnect.");
                    WifiProStateMachine.this.mWifiProStatisticsManager.accuNotInetRoDisconnectData();
                } else {
                    WifiProStateMachine.this.logI("Qoe bad rove out and WIFI disconnect.");
                    WifiProStateMachine.this.mWifiProStatisticsManager.accuQOEBadRoDisconnectData();
                }
            }
            if (WifiProStateMachine.this.mPhoneStateListenerRegisted) {
                WifiProStateMachine.this.logI("stop PhoneStateListener");
                WifiProStateMachine.this.mTelephonyManager.listen(WifiProStateMachine.this.phoneStateListener, 0);
                boolean unused14 = WifiProStateMachine.this.mPhoneStateListenerRegisted = false;
            }
            boolean unused15 = WifiProStateMachine.this.mRoveOutStarted = false;
            boolean unused16 = WifiProStateMachine.this.mIsRoveOutToDisconn = false;
        }

        public void exit() {
            WifiProStateMachine.this.logI("WifiDisConnectedState is Exit");
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                    if (!(msg.obj instanceof Intent)) {
                        WifiProStateMachine.this.logE("processMessage:Intent is not match the Class");
                        break;
                    } else {
                        Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
                        NetworkInfo networkInfo = null;
                        if (objNetworkInfo instanceof NetworkInfo) {
                            networkInfo = (NetworkInfo) objNetworkInfo;
                        } else {
                            WifiProStateMachine.this.logE("processMessage:networkInfo is not match the Class");
                        }
                        if (networkInfo == null || NetworkInfo.State.CONNECTED != networkInfo.getState() || !WifiProStateMachine.this.isWifiConnected()) {
                            if (networkInfo != null && NetworkInfo.State.CONNECTING == networkInfo.getState()) {
                                WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
                                break;
                            }
                        } else {
                            WifiProStateMachine.this.logI("WifiDisConnectedState: wifi connect,to go connected");
                            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                            wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiConnectedState);
                            break;
                        }
                    }
                    break;
                case WifiProStateMachine.EVENT_MOBILE_DATA_STATE_CHANGED_ACTION /*{ENCODED_INT: 136186}*/:
                    if (!WifiProStateMachine.this.isMobileDataConnected()) {
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_NETWORK_CONNECTIVITY_CHANGE);
                        break;
                    }
                    break;
                case WifiProStateMachine.EVENT_NETWORK_USER_CONNECT /*{ENCODED_INT: 136202}*/:
                    if (msg.obj != null && ((Boolean) msg.obj).booleanValue()) {
                        boolean unused = WifiProStateMachine.this.mIsUserManualConnectSuccess = true;
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        wifiProStateMachine2.logI("receive EVENT_NETWORK_USER_CONNECT, set mIsUserManualConnectSuccess = " + WifiProStateMachine.this.mIsUserManualConnectSuccess);
                        break;
                    }
                case WifiProStateMachine.EVENT_CONFIGURED_NETWORKS_CHANGED /*{ENCODED_INT: 136308}*/:
                    if (!(msg.obj instanceof Intent)) {
                        WifiProStateMachine.this.logE("EVENT_CONFIGURED_NETWORKS_CHANGED:configIntent is not match");
                        break;
                    } else {
                        Intent configIntent = (Intent) msg.obj;
                        Object objConfiguration = configIntent.getParcelableExtra("wifiConfiguration");
                        WifiConfiguration connCfg = null;
                        if (objConfiguration instanceof WifiConfiguration) {
                            connCfg = (WifiConfiguration) objConfiguration;
                        } else {
                            WifiProStateMachine.this.logE("EVENT_CONFIGURED_NETWORKS_CHANGED:WifiConfiguration is not match");
                        }
                        if (connCfg != null) {
                            int changeReason = configIntent.getIntExtra("changeReason", -1);
                            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                            wifiProStateMachine3.logI("WifiDisConnectedState, change reson " + changeReason + ", isTempCreated = " + connCfg.isTempCreated);
                            if (connCfg.isTempCreated && changeReason != 1) {
                                WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                                wifiProStateMachine4.logI("WifiDisConnectedState, forget " + connCfg.SSID);
                                WifiProStateMachine.this.mWifiManager.forget(connCfg.networkId, null);
                                break;
                            }
                        }
                    }
                    break;
                case WifiProStateMachine.EVENT_WIFI_FIRST_CONNECTED /*{ENCODED_INT: 136373}*/:
                    WifiProStateMachine.this.logI("EVENT_WIFI_FIRST_CONNECTED: go to connected");
                    WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                    wifiProStateMachine5.transitionTo(wifiProStateMachine5.mWifiConnectedState);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class WifiSemiAutoEvaluateState extends State {
        WifiSemiAutoEvaluateState() {
        }

        public void enter() {
            WifiProStateMachine.this.logI("WifiSemiAutoEvaluateState enter");
            WifiProStateMachine.this.setWifiCSPState(0);
            if (!WifiProStateMachine.mIsWifiSemiAutoEvaluating) {
                WifiProStateMachine.this.setWifiEvaluateTag(true);
                boolean unused = WifiProStateMachine.mIsWifiSemiAutoEvaluating = true;
                boolean unused2 = WifiProStateMachine.this.mIsAllowEvaluate = true;
                if (WifiProStateMachine.this.mWiFiProEvaluateController.isUnEvaluateAPRecordsEmpty() || !HwWifiProFeatureControl.sWifiProOpenApEvaluateCtrl) {
                    WifiProStateMachine.this.logW("UnEvaluate AP records is empty !");
                } else {
                    int unused3 = WifiProStateMachine.this.mOpenAvailableAPCounter = 0;
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiSemiAutoScoreState);
                    return;
                }
            }
            boolean unused4 = WifiProStateMachine.mIsWifiSemiAutoEvaluating = false;
            WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EVALUATE_COMPLETE);
        }

        public void exit() {
            WifiProStateMachine.this.logI("WifiSemiAutoEvaluateState exit");
            if (WifiProStateMachine.this.mIsWifiSemiAutoEvaluateComplete || !WifiProStateMachine.this.isAllowWiFiAutoEvaluate()) {
                WifiProStateMachine.this.setWifiEvaluateTag(false);
                WifiProStateMachine.this.mNetworkQosMonitor.stopBqeService();
            }
            WifiProStateMachine.this.mWiFiProEvaluateController.cleanEvaluateCacheRecords();
            WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                case WifiProStateMachine.EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED /*{ENCODED_INT: 136203}*/:
                    handleWifiNetworkStateChange(msg);
                    return true;
                case WifiProStateMachine.EVENT_SCAN_RESULTS_AVAILABLE /*{ENCODED_INT: 136293}*/:
                    return true;
                case WifiProStateMachine.EVENT_EVALUATE_COMPLETE /*{ENCODED_INT: 136295}*/:
                    handleEvaluateComplete();
                    return true;
                case WifiProStateMachine.EVENT_WIFI_SEMIAUTO_EVALUTE_CHANGE /*{ENCODED_INT: 136300}*/:
                    if (WifiProStateMachine.this.isAllowWiFiAutoEvaluate()) {
                        return true;
                    }
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EVALUATE_COMPLETE);
                    return true;
                default:
                    return false;
            }
        }

        private void handleEvaluateComplete() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("Evaluate has complete, restore wifi Config, mOpenAvailableAPCounter = " + WifiProStateMachine.this.mOpenAvailableAPCounter);
            if (WifiProStateMachine.this.mOpenAvailableAPCounter >= 2) {
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(10);
                int unused = WifiProStateMachine.this.mOpenAvailableAPCounter = 0;
            }
            WiFiProEvaluateController unused2 = WifiProStateMachine.this.mWiFiProEvaluateController;
            WiFiProEvaluateController.evaluateAPHashMapDump();
            WifiProStateMachine.this.mWiFiProEvaluateController.cleanEvaluateRecords();
            boolean unused3 = WifiProStateMachine.mIsWifiSemiAutoEvaluating = false;
            boolean unused4 = WifiProStateMachine.this.mIsWifiSemiAutoEvaluateComplete = true;
            WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            NetworkInfo wifiInfo = WifiProStateMachine.this.mConnectivityManager.getNetworkInfo(1);
            if (wifiInfo == null) {
                WifiProStateMachine.this.logI("wifiInfo is null, go to mWiFiProEnableState");
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiDisConnectedState);
            } else if (wifiInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                WifiProStateMachine.this.logI("wifi has disconnected, go to mWifiDisConnectedState");
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiDisConnectedState);
            }
        }

        private void handleWifiNetworkStateChange(Message msg) {
            if (msg.obj instanceof Intent) {
                Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
                NetworkInfo networkInfo = null;
                if (objNetworkInfo instanceof NetworkInfo) {
                    networkInfo = (NetworkInfo) objNetworkInfo;
                }
                if (networkInfo != null && NetworkInfo.DetailedState.CONNECTED == networkInfo.getDetailedState() && WifiProStateMachine.this.isWifiConnected()) {
                    WifiProStateMachine.this.setWifiEvaluateTag(false);
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI("mIsWifiSemiAutoEvaluateComplete == " + WifiProStateMachine.this.mIsWifiSemiAutoEvaluateComplete);
                    WifiProStateMachine.this.logD("******WifiSemiAutoEvaluateState go to mWifiConnectedState *****");
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiConnectedState);
                } else if (networkInfo != null && NetworkInfo.DetailedState.DISCONNECTED == networkInfo.getDetailedState()) {
                    if (WifiProStateMachine.this.mIsWifiSemiAutoEvaluateComplete || !WifiProStateMachine.this.isAllowWiFiAutoEvaluate()) {
                        WifiProStateMachine.this.logW("Evaluate has complete, go to mWifiDisConnectedState");
                        WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                        wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiDisConnectedState);
                        WifiProStateMachine.this.setWifiEvaluateTag(false);
                    }
                }
            } else {
                WifiProStateMachine.this.logE("handleWifiNetworkStateChange msg.obj is null or not intent");
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class WifiSemiAutoScoreState extends State {
        private int checkCounter;
        private long checkTime;
        private long connectTime;
        private boolean isCheckRuning;
        private String nextBSSID = null;
        private String nextSSID = null;

        WifiSemiAutoScoreState() {
        }

        public void enter() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI("WifiSemiAutoScoreState enter,  mIsAllowEvaluate = " + WifiProStateMachine.this.mIsAllowEvaluate);
            if (isStopEvaluteNextAP()) {
                WifiProStateMachine.this.logI("WiFiPro auto Evaluate has  closed");
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiSemiAutoEvaluateState);
                return;
            }
            this.connectTime = 0;
            this.checkTime = 0;
            this.checkCounter = 0;
            this.isCheckRuning = false;
            this.nextSSID = WifiProStateMachine.this.mWiFiProEvaluateController.getNextEvaluateWiFiSSID();
            if (TextUtils.isEmpty(this.nextSSID)) {
                WifiProStateMachine.this.logI("ALL SemiAutoScore has Evaluate complete!");
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiSemiAutoEvaluateState);
                return;
            }
            WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
            wifiProStateMachine4.logI("***********start SemiAuto Evaluate nextSSID :" + StringUtilEx.safeDisplaySsid(this.nextSSID));
            if (WifiProStateMachine.this.mWiFiProEvaluateController.isAbandonEvaluate(this.nextSSID)) {
                WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EVALUTE_ABANDON);
                return;
            }
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_EVALUTE_TIMEOUT);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_EVALUATE_START_CHECK_INTERNET);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_EVALUTE_TCPRTT_RESULT);
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_EVALUTE_TIMEOUT, 75000);
            WifiProUIDisplayManager access$7100 = WifiProStateMachine.this.mWifiProUIDisplayManager;
            access$7100.showToastL("start  evaluate :" + StringUtilEx.safeDisplaySsid(this.nextSSID));
            WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoLevel(this.nextSSID, 0);
            WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 1, 0, this.nextSSID);
            WifiProStateMachine.this.refreshConnectedNetWork();
            if (!WifiProStateMachine.this.isWifiConnected() || !this.nextSSID.equals(WifiProStateMachine.this.mCurrentSsid)) {
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_EVALUTE_NEXT_AP, 2000);
                return;
            }
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT);
            WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, true);
            this.isCheckRuning = true;
        }

        public void exit() {
            WifiProStateMachine.this.logI("WifiSemiAutoScoreState exit");
            WifiProStateMachine.this.mNetworkQosMonitor.resetMonitorStatus();
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_EVALUTE_TIMEOUT);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT);
            WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_DELAY_EVALUTE_NEXT_AP);
            WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
            this.nextBSSID = null;
            if (!TextUtils.isEmpty(this.nextSSID)) {
                WifiProStateMachine.this.mWiFiProEvaluateController.updateWifiProbeMode(this.nextSSID, 1);
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case WifiProStateMachine.EVENT_WIFI_NETWORK_STATE_CHANGE /*{ENCODED_INT: 136169}*/:
                case WifiProStateMachine.EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED /*{ENCODED_INT: 136203}*/:
                    handleWifiStateChange(msg);
                    break;
                case WifiProStateMachine.EVENT_CHECK_WIFI_INTERNET_RESULT /*{ENCODED_INT: 136181}*/:
                    handleInternetCheckReusltInAutoScoreState(msg);
                    break;
                case WifiProStateMachine.EVENT_SCAN_RESULTS_AVAILABLE /*{ENCODED_INT: 136293}*/:
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    List unused = wifiProStateMachine.mScanResultList = wifiProStateMachine.mWiFiProEvaluateController.scanResultListFilter(WifiProStateMachine.this.mWifiManager.getScanResults());
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    boolean unused2 = wifiProStateMachine2.mIsAllowEvaluate = wifiProStateMachine2.mWiFiProEvaluateController.isAllowAutoEvaluate(WifiProStateMachine.this.mScanResultList);
                    if (!WifiProStateMachine.this.mIsAllowEvaluate) {
                        WifiProStateMachine.this.logI("discover save ap, stop allow evaluate");
                        WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                        wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiSemiAutoEvaluateState);
                        break;
                    }
                    break;
                case WifiProStateMachine.EVENT_SUPPLICANT_STATE_CHANGE /*{ENCODED_INT: 136297}*/:
                case WifiProStateMachine.EVENT_WIFI_SEMIAUTO_EVALUTE_CHANGE /*{ENCODED_INT: 136300}*/:
                    break;
                case WifiProStateMachine.EVENT_WIFI_EVALUTE_TCPRTT_RESULT /*{ENCODED_INT: 136299}*/:
                    handleWifiEvaluteTcpResult(msg);
                    break;
                case WifiProStateMachine.EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT /*{ENCODED_INT: 136301}*/:
                    handleWifiEvaluteConnectTimeout();
                    break;
                case WifiProStateMachine.EVENT_LAST_EVALUTE_VALID /*{ENCODED_INT: 136302}*/:
                    handleEventLastEvaluteValid();
                    break;
                case WifiProStateMachine.EVENT_EVALUTE_TIMEOUT /*{ENCODED_INT: 136304}*/:
                    handleEventEvaluteTimeout();
                    break;
                case WifiProStateMachine.EVENT_EVALUTE_ABANDON /*{ENCODED_INT: 136305}*/:
                    WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                    wifiProStateMachine4.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + "abandon evalute ");
                    WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(this.nextSSID, 1);
                    WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 1, 1, this.nextSSID);
                    WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                    wifiProStateMachine5.transitionTo(wifiProStateMachine5.mWifiSemiAutoScoreState);
                    break;
                case WifiProStateMachine.EVENT_EVALUATE_START_CHECK_INTERNET /*{ENCODED_INT: 136307}*/:
                    handleEvaluteCheck();
                    break;
                case WifiProStateMachine.EVENT_CONFIGURED_NETWORKS_CHANGED /*{ENCODED_INT: 136308}*/:
                    handleNetworkConfigChange(msg);
                    break;
                case WifiProStateMachine.EVENT_WIFI_P2P_CONNECTION_CHANGED /*{ENCODED_INT: 136310}*/:
                    if (WifiProStateMachine.this.mIsP2PConnectedOrConnecting) {
                        WifiProStateMachine.this.logI("P2PConnectedOrConnecting  , stop allow evaluate");
                        WifiProStateMachine wifiProStateMachine6 = WifiProStateMachine.this;
                        wifiProStateMachine6.transitionTo(wifiProStateMachine6.mWifiSemiAutoEvaluateState);
                        break;
                    }
                    break;
                case WifiProStateMachine.EVENT_WIFI_SECURITY_RESPONSE /*{ENCODED_INT: 136312}*/:
                case WifiProStateMachine.EVENT_WIFI_SECURITY_QUERY_TIMEOUT /*{ENCODED_INT: 136313}*/:
                    handleWifiSecurityTimeout(msg);
                    break;
                case WifiProStateMachine.EVENT_DELAY_EVALUTE_NEXT_AP /*{ENCODED_INT: 136314}*/:
                    if (!WifiProStateMachine.this.isWifiConnected()) {
                        evaluteNextAP();
                        break;
                    } else {
                        WifiProStateMachine.this.logI("wifi still connectd, delay 2s to evalute next ap");
                        WifiProStateMachine.this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
                        WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_DELAY_EVALUTE_NEXT_AP, 2000);
                        break;
                    }
                case WifiProStateMachine.EVENT_BQE_ANALYZE_NETWORK_QUALITY /*{ENCODED_INT: 136317}*/:
                    if (!WifiProStateMachine.this.mNetworkQosMonitor.isBqeServicesStarted()) {
                        WifiProStateMachine.this.logI("EVENT_BQE_ANALYZE_NETWORK_QUALITY, isBqeServicesStarted = false.");
                        WifiProStateMachine wifiProStateMachine7 = WifiProStateMachine.this;
                        wifiProStateMachine7.transitionTo(wifiProStateMachine7.mWifiSemiAutoScoreState);
                        break;
                    } else {
                        WifiProStateMachine.this.mNetworkQosMonitor.startWiFiBqeDetect(3000);
                        break;
                    }
                default:
                    return false;
            }
            return true;
        }

        private void handleEventLastEvaluteValid() {
            WiFiProScoreInfo wiFiProScoreInfo = WifiProStateMachine.this.mWiFiProEvaluateController.getCurrentWiFiProScoreInfo(this.nextSSID);
            if (wiFiProScoreInfo != null) {
                WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), wiFiProScoreInfo.internetAccessType, wiFiProScoreInfo.networkQosLevel, wiFiProScoreInfo.networkQosScore);
            }
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiSemiAutoScoreState);
        }

        private void handleWifiEvaluteConnectTimeout() {
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + " Conenct Time Out,connect fail! conenct Time = 35s");
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(15);
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(20);
            WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(this.nextSSID, 1);
            WifiProStateMachine.this.mWiFiProEvaluateController.increaseFailCounter(this.nextSSID);
            WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 1, 1, this.nextSSID);
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiSemiAutoScoreState);
        }

        private void handleEventEvaluteTimeout() {
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(11);
            WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(this.nextSSID, 1);
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
            WifiProStateMachine.this.mWiFiProEvaluateController.increaseFailCounter(this.nextSSID);
            WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 1, 1, this.nextSSID);
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + " evaluate Time = 70s");
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiSemiAutoScoreState);
        }

        private void handleWifiSecurityTimeout(Message msg) {
            if (msg.obj != null) {
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_SECURITY_QUERY_TIMEOUT);
                if (msg.obj instanceof Bundle) {
                    Bundle bundle = (Bundle) msg.obj;
                    String ssid = bundle.getString(WifiProCommonDefs.FLAG_SSID);
                    if (ssid == null || !ssid.equals(this.nextSSID)) {
                        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                        wifiProStateMachine.logI("handle EVENT_WIFI_SECURITY_RESPONSE, it's invalid ssid = " + StringUtilEx.safeDisplaySsid(ssid) + ", ignore the result.");
                        return;
                    }
                    int status = bundle.getInt(WifiProCommonDefs.FLAG_SECURITY_STATUS);
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.logI("handle EVENT_WIFI_SECURITY_RESPONSE, ssid = " + StringUtilEx.safeDisplaySsid(ssid) + ", status = " + status);
                    WifiProStateMachine.this.mWiFiProEvaluateController.updateWifiSecurityInfo(this.nextSSID, status);
                    if (status >= 2) {
                        WifiProStateMachine.this.logI("handle EVENT_WIFI_SECURITY_RESPONSE, unsecurity, upload CHR statistic.");
                        WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(4);
                    }
                } else {
                    WifiProStateMachine.this.logE("handleWifiSecurityTimeout bundle is null");
                    return;
                }
            } else {
                WifiProStateMachine.this.logW("EVENT_WIFI_SECURITY_RESPONSE, timeout happend.");
                WifiProStateMachine.this.mWiFiProEvaluateController.updateWifiSecurityInfo(this.nextSSID, -1);
            }
            WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreEvaluateStatus(this.nextSSID, true);
            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
            wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiSemiAutoScoreState);
        }

        private void handleWifiEvaluteTcpResult(Message msg) {
            int level = msg.arg1;
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + "  TCPRTT  level = " + level);
            if (WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoLevel(this.nextSSID, level)) {
                WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 2, level, this.nextSSID);
            }
            if (level == 0) {
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(11);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(23);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
            }
            boolean enabled = WifiProCommonUtils.isWifiSecDetectOn(WifiProStateMachine.this.mContext);
            int security = WifiProStateMachine.this.mWiFiProEvaluateController.getWifiSecurityInfo(this.nextSSID);
            WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
            wifiProStateMachine2.logI("security switch enabled = " + enabled + ", current security value = " + security);
            if (!enabled || !WifiProCommonUtils.isWifiConnected(WifiProStateMachine.this.mWifiManager) || !(security == -1 || security == 1)) {
                WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreEvaluateStatus(this.nextSSID, true);
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiSemiAutoScoreState);
                return;
            }
            this.nextBSSID = WifiProCommonUtils.getCurrentBssid(WifiProStateMachine.this.mWifiManager);
            WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
            wifiProStateMachine4.logI("recv BQE level = " + level + ", start to query wifi security, ssid = " + StringUtilEx.safeDisplaySsid(this.nextSSID));
            WifiProStateMachine.this.mNetworkQosMonitor.queryWifiSecurity(this.nextSSID, this.nextBSSID);
            WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_WIFI_SECURITY_QUERY_TIMEOUT, 30000);
        }

        private void handleEvaluteCheck() {
            WifiProStateMachine.this.logW("wifi conenct, start check internet,  checkCounter =   " + this.checkCounter);
            updateApEvaluateEvent(WifiproUtils.EVALUATION_TRIGGER);
            if (this.checkCounter == 0) {
                this.connectTime = (System.currentTimeMillis() - this.connectTime) / 1000;
                WifiProStateMachine.this.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + " background conenct Time =" + this.connectTime + " s");
                WifiProStateMachine.this.removeMessages(WifiProStateMachine.EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT);
            }
            this.checkTime = System.currentTimeMillis();
            this.checkCounter++;
            WifiProStateMachine.this.mNetworkQosMonitor.queryNetworkQos(1, WifiProStateMachine.this.mIsPortalAp, WifiProStateMachine.this.mIsNetworkAuthen, true);
            this.isCheckRuning = true;
        }

        private void handleWifiStateChange(Message msg) {
            if (msg.obj instanceof Intent) {
                NetworkInfo networkInfo = null;
                Object objNetworkInfo = ((Intent) msg.obj).getParcelableExtra("networkInfo");
                if (objNetworkInfo instanceof NetworkInfo) {
                    networkInfo = (NetworkInfo) objNetworkInfo;
                }
                WifiInfo cInfo = WifiProStateMachine.this.mWifiManager.getConnectionInfo();
                if (cInfo == null || networkInfo == null) {
                    WifiProStateMachine.this.logI("EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED:cInfo or networkInfo is null.");
                    return;
                }
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI(", nextSSID SSID = " + StringUtilEx.safeDisplaySsid(this.nextSSID) + ", networkInfo = " + networkInfo);
                if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED && networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                    if ((!TextUtils.isEmpty(this.nextSSID) && this.nextSSID.equals(cInfo.getSSID())) || "<unknown ssid>".equals(cInfo.getSSID())) {
                        WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(11);
                        WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(this.nextSSID, 1);
                        WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(22);
                        WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
                        WifiProStateMachine.this.mWiFiProEvaluateController.increaseFailCounter(this.nextSSID);
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiSemiAutoScoreState);
                    }
                } else if (NetworkInfo.State.CONNECTED == networkInfo.getState()) {
                    String extssid = cInfo.getSSID();
                    if (TextUtils.isEmpty(this.nextSSID) || TextUtils.isEmpty(extssid) || this.nextSSID.equals(extssid)) {
                        WifiConfiguration wifiConfig = WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID);
                        if (wifiConfig != null) {
                            int tag = Settings.Secure.getInt(WifiProStateMachine.this.mContentResolver, WifiProStateMachine.WIFI_EVALUATE_TAG, -1);
                            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                            wifiProStateMachine3.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + "is Connected, wifiConfig isTempCreated = " + wifiConfig.isTempCreated + ", Tag = " + tag);
                        }
                        WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                        wifiProStateMachine4.logI("receive connect msg ,ssid : " + StringUtilEx.safeDisplaySsid(extssid));
                        WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(16);
                        return;
                    }
                    WifiProStateMachine wifiProStateMachine5 = WifiProStateMachine.this;
                    wifiProStateMachine5.logI("Connected other ap ,ssid : " + StringUtilEx.safeDisplaySsid(extssid));
                    WifiProStateMachine wifiProStateMachine6 = WifiProStateMachine.this;
                    wifiProStateMachine6.transitionTo(wifiProStateMachine6.mWifiSemiAutoEvaluateState);
                } else if (NetworkInfo.State.CONNECTING == networkInfo.getState()) {
                    String currssid = cInfo.getSSID();
                    if (!TextUtils.isEmpty(this.nextSSID) && !TextUtils.isEmpty(currssid) && !this.nextSSID.equals(currssid)) {
                        WifiProStateMachine wifiProStateMachine7 = WifiProStateMachine.this;
                        wifiProStateMachine7.logI("Connect other ap ,ssid : " + StringUtilEx.safeDisplaySsid(currssid));
                        WifiProStateMachine wifiProStateMachine8 = WifiProStateMachine.this;
                        wifiProStateMachine8.transitionTo(wifiProStateMachine8.mWifiSemiAutoEvaluateState);
                    }
                }
            } else {
                WifiProStateMachine.this.logE("handleWifiStateChange: msg.obj is null or not intent");
            }
        }

        private void handleNetworkConfigChange(Message msg) {
            if (msg.obj instanceof Intent) {
                Intent confgIntent = (Intent) msg.obj;
                int changeReason = confgIntent.getIntExtra("changeReason", -1);
                WifiConfiguration connCfg = null;
                Object objConfig = confgIntent.getParcelableExtra("wifiConfiguration");
                if (objConfig instanceof WifiConfiguration) {
                    connCfg = (WifiConfiguration) objConfig;
                }
                if (connCfg != null) {
                    WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                    wifiProStateMachine.logI(", nextSSID SSID = " + StringUtilEx.safeDisplaySsid(this.nextSSID) + ", conf  " + StringUtilEx.safeDisplaySsid(connCfg.SSID));
                    if (changeReason == 0) {
                        handleWifiConfgChange(changeReason, connCfg);
                    } else if (changeReason == 2) {
                        WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                        wifiProStateMachine2.logI("--- changeReason =change,  change a ssid = " + StringUtilEx.safeDisplaySsid(connCfg.SSID) + ", status = " + connCfg.status + " isTempCreated " + connCfg.isTempCreated);
                        if (!connCfg.isTempCreated) {
                            if (!WifiProStateMachine.this.isWifiConnected()) {
                                WifiProStateMachine.this.logI("--- wifi has disconnect ----");
                            } else if (!TextUtils.isEmpty(this.nextSSID) && this.nextSSID.equals(connCfg.SSID)) {
                                WifiProStateMachine.this.mWiFiProEvaluateController.clearUntrustedOpenApList();
                                WifiProStateMachine.this.mWifiProConfigStore.resetTempCreatedConfig(connCfg);
                                if (connCfg.status == 1) {
                                    WifiProStateMachine.this.mWifiManager.connect(connCfg, null);
                                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(18);
                                }
                            }
                            WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                            wifiProStateMachine3.transitionTo(wifiProStateMachine3.mWifiSemiAutoEvaluateState);
                        }
                    }
                }
            } else {
                WifiProStateMachine.this.logE("handleNetworkConfigChange: msg.obj is null or not intent");
            }
        }

        private void handleInternetCheckReusltInAutoScoreState(Message msg) {
            if (!TextUtils.isEmpty(this.nextSSID) && this.isCheckRuning) {
                this.checkTime = (System.currentTimeMillis() - this.checkTime) / 1000;
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + " checkTime = " + this.checkTime + " s");
                int result = msg.arg1;
                int type = handleWifiCheckResult(result);
                if (7 == result) {
                    if (this.checkCounter == 1) {
                        WifiProStateMachine.this.logI("internet check timeout ,check again");
                        this.checkTime = System.currentTimeMillis();
                        WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_EVALUATE_START_CHECK_INTERNET);
                        return;
                    }
                    type = 1;
                    WifiProStateMachine.this.mWiFiProEvaluateController.increaseFailCounter(this.nextSSID);
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(11);
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(21);
                    WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
                }
                WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                wifiProStateMachine2.logI(StringUtilEx.safeDisplaySsid(this.nextSSID) + " type = " + type);
                WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreInfoType(this.nextSSID, type);
                WifiProStateMachine.this.mWifiProConfigStore.updateWifiEvaluateConfig(WifiProStateMachine.this.mWiFiProEvaluateController.getWifiConfiguration(this.nextSSID), 1, type, this.nextSSID);
                WifiProStateMachine.this.mWiFiProEvaluateController.updateScoreEvaluateStatus(this.nextSSID, true);
                WifiProStateMachine wifiProStateMachine3 = WifiProStateMachine.this;
                wifiProStateMachine3.logI("clean evaluate ap :" + StringUtilEx.safeDisplaySsid(this.nextSSID));
                WifiProStateMachine wifiProStateMachine4 = WifiProStateMachine.this;
                wifiProStateMachine4.transitionTo(wifiProStateMachine4.mWifiSemiAutoScoreState);
            }
        }

        private void evaluteNextAP() {
            WifiProStateMachine.this.logI("start evalute next ap");
            if (WifiProStateMachine.this.mWiFiProEvaluateController.connectWifi(this.nextSSID)) {
                this.connectTime = System.currentTimeMillis();
                WifiProStateMachine.this.sendMessageDelayed(WifiProStateMachine.EVENT_WIFI_EVALUTE_CONNECT_TIMEOUT, 35000);
                return;
            }
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(11);
            WifiProStateMachine.this.mWifiProStatisticsManager.updateBgApSsid(this.nextSSID);
            WifiProStateMachine.this.logI("background connect fail!");
            WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
            wifiProStateMachine.transitionTo(wifiProStateMachine.mWifiSemiAutoScoreState);
        }

        private int handleWifiCheckResult(int result) {
            if (5 == result) {
                updateApEvaluateEvent(WifiproUtils.EVALUATION_HAS_INTERNET);
                WifiProStateMachine.access$25208(WifiProStateMachine.this);
                WifiProStateMachine.this.mWifiProUIDisplayManager.shownAccessNotification(true);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(3);
                return 4;
            } else if (6 == result) {
                updateApEvaluateEvent(WifiproUtils.EVALUATION_PORTAL_INTERNET);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(6);
                return 3;
            } else if (-1 != result) {
                return 0;
            } else {
                updateApEvaluateEvent(WifiproUtils.EVALUATION_NO_INTERNET);
                WifiProStateMachine.this.mWifiProStatisticsManager.updateBGChrStatistic(5);
                return 2;
            }
        }

        private void updateApEvaluateEvent(String type) {
            if (WifiProStateMachine.this.uploadManager != null) {
                char c = 65535;
                switch (type.hashCode()) {
                    case -274122290:
                        if (type.equals(WifiproUtils.EVALUATION_PORTAL_INTERNET)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 125451224:
                        if (type.equals(WifiproUtils.EVALUATION_NO_INTERNET)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 509736101:
                        if (type.equals(WifiproUtils.EVALUATION_TRIGGER)) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1281947673:
                        if (type.equals(WifiproUtils.EVALUATION_HAS_INTERNET)) {
                            c = 0;
                            break;
                        }
                        break;
                }
                if (c == 0) {
                    WifiProStateMachine.this.uploadManager.addChrCntStat(WifiproUtils.EVALUATION_EVENT, WifiproUtils.EVALUATION_HAS_INTERNET);
                } else if (c == 1) {
                    WifiProStateMachine.this.uploadManager.addChrCntStat(WifiproUtils.EVALUATION_EVENT, WifiproUtils.EVALUATION_NO_INTERNET);
                } else if (c == 2) {
                    WifiProStateMachine.this.uploadManager.addChrCntStat(WifiproUtils.EVALUATION_EVENT, WifiproUtils.EVALUATION_PORTAL_INTERNET);
                } else if (c != 3) {
                    WifiProStateMachine.this.logI("Unknown AP Evaluate Event type = " + type);
                } else {
                    WifiProStateMachine.this.uploadManager.addChrCntStat(WifiproUtils.EVALUATION_EVENT, WifiproUtils.EVALUATION_TRIGGER);
                }
            }
        }

        private void handleWifiConfgChange(int reason, WifiConfiguration connCfg) {
            if (connCfg != null && reason == 0) {
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.this;
                wifiProStateMachine.logI("add a new connCfg,isTempCreated : " + connCfg.isTempCreated);
                if (!TextUtils.isEmpty(this.nextSSID) && this.nextSSID.equals(connCfg.SSID) && connCfg.isTempCreated) {
                    WifiProStateMachine.this.mWiFiProEvaluateController.addUntrustedOpenApList(connCfg.SSID);
                } else if (!TextUtils.isEmpty(connCfg.SSID)) {
                    WifiProStateMachine.this.logI("system connecting ap,stop background evaluate");
                    WifiProStateMachine wifiProStateMachine2 = WifiProStateMachine.this;
                    wifiProStateMachine2.transitionTo(wifiProStateMachine2.mWifiSemiAutoEvaluateState);
                }
            }
        }

        private boolean isStopEvaluteNextAP() {
            return !WifiProStateMachine.this.isAllowWiFiAutoEvaluate() || !TextUtils.isEmpty(WifiProStateMachine.this.mUserManualConnecConfigKey) || !WifiProStateMachine.this.mIsAllowEvaluate || WifiProCommonUtils.isWifiConnectedOrConnecting(WifiProStateMachine.this.mWifiManager);
        }
    }

    public static boolean isWifiEvaluating() {
        return mIsWifiManualEvaluating || mIsWifiSemiAutoEvaluating;
    }

    /* access modifiers changed from: private */
    public boolean isSettingsActivity() {
        return WifiProCommonUtils.isQueryActivityMatched(this.mContext, WifiProCommonUtils.HUAWEI_SETTINGS_WLAN);
    }

    public void setWifiApEvaluateEnabled(boolean enable) {
        logI("setWifiApEvaluateEnabled enabled " + enable);
        logI("system can not eavluate ap, ignor setting cmd");
    }

    /* access modifiers changed from: private */
    public void setWifiEvaluateTag(boolean evaluate) {
        logI("setWifiEvaluateTag Tag :" + evaluate);
        Settings.Secure.putInt(this.mContentResolver, WIFI_EVALUATE_TAG, evaluate ? 1 : 0);
    }

    /* access modifiers changed from: private */
    public boolean restoreWiFiConfig() {
        this.mIsWiFiProAutoEvaluateAP = getSettingsSecureBoolean(this.mContentResolver, KEY_WIFIPRO_RECOMMEND_NETWORK, false);
        this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
        NetworkInfo wifiInfo = this.mConnectivityManager.getNetworkInfo(1);
        if (wifiInfo == null || wifiInfo.getDetailedState() != NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
            return false;
        }
        this.mWifiManager.disconnect();
        return true;
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public synchronized void onNetworkQosChange(int type, int level, boolean updateUiOnly) {
        if (1 == type) {
            try {
                this.mCurrentWifiLevel = level;
                logI("onNetworkQosChange, currentWifiLevel == " + level + ", wifiNoInternet = " + this.mIsWiFiNoInternet + ", updateUiOnly = " + updateUiOnly);
                if (level == -103) {
                    this.mWifiHandoverStartTime = SystemClock.elapsedRealtime();
                }
                sendMessage(EVENT_WIFI_QOS_CHANGE, level, 0, Boolean.valueOf(updateUiOnly));
            } catch (Throwable th) {
                throw th;
            }
        } else if (type == 0) {
            sendMessage(EVENT_MOBILE_QOS_CHANGE, level);
        }
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public synchronized void onNetworkDetectionResult(int type, int level) {
        int levelVal = level;
        if (1 == type) {
            logI("wifi Detection level == " + levelVal);
            HwWifiProFeatureControl.getInstance();
            if (!HwWifiProFeatureControl.isSelfCureOngoing() || 5 == levelVal) {
                if ((5 == levelVal && this.mIsWiFiNoInternet) || this.mIsUserHandoverWiFi) {
                    logI("wifi no internet and recovered, notify SCE");
                    HwWifiProFeatureControl.getInstance();
                    HwWifiProFeatureControl.notifyInternetAccessRecovery();
                    reportNetworkConnectivity(true);
                    if (WIFI_HANDOVER_TYPES[6].equals(this.mChrWifiHandoverType)) {
                        uploadChrWifiHandoverTypeStatistics(WIFI_HANDOVER_TYPES[6], HANDOVER_SUCC_CNT);
                    }
                }
                if (-101 == levelVal) {
                    sendMessage(EVENT_WIFI_CHECK_UNKOWN, levelVal);
                } else if (!isWifiEvaluating()) {
                    if (7 == levelVal) {
                        levelVal = -1;
                    }
                    if (levelVal == -1 && levelVal != this.mLastWifiLevel && this.mIsUserManualConnectSuccess && !this.mIsWiFiProEnabled) {
                        updateChrToCell(false);
                        this.mWsmChannel.sendMessage((int) INVALID_LINK_DETECTED);
                    }
                    updateWifiInternetStateChange(levelVal);
                    sendMessage(EVENT_CHECK_WIFI_INTERNET_RESULT, levelVal);
                } else {
                    sendMessage(EVENT_CHECK_WIFI_INTERNET_RESULT, levelVal);
                }
            } else {
                logI("SelfCureOngoing, ignore wifi check result");
                if (this.isVerifyWifiNoInternetTimeOut || this.hasHandledNoInternetResult) {
                    this.isVerifyWifiNoInternetTimeOut = false;
                    this.hasHandledNoInternetResult = false;
                }
            }
        } else if (type == 0) {
            sendMessage(EVENT_CHECK_MOBILE_QOS_RESULT, levelVal);
        }
    }

    public void onPortalAuthCheckResult(int respCode) {
        int level = -1;
        if (respCode == 204) {
            level = 5;
        } else if (WifiProCommonUtils.isRedirectedRespCodeByGoogle(respCode)) {
            level = 6;
        } else if (respCode == 600) {
            level = 7;
        }
        sendMessage(EVENT_CHECK_PORTAL_AUTH_CHECK_RESULT, level);
    }

    @Override // com.huawei.hwwifiproservice.INetworksHandoverCallBack
    public synchronized void onWifiHandoverChange(int type, boolean result, String bssid, int errorReason) {
        if (1 == type) {
            if (result) {
                try {
                    this.mWifiProStatisticsManager.increaseWiFiHandoverWiFiCount(this.mWifiToWifiType);
                } catch (Throwable th) {
                    throw th;
                }
            }
            this.mNewSelect_bssid = bssid;
            sendMessage(EVENT_WIFI_HANDOVER_WIFI_RESULT, errorReason, -1, Boolean.valueOf(result));
        } else {
            if (4 == type) {
                this.mNewSelect_bssid = bssid;
                sendMessage(EVENT_DUALBAND_WIFI_HANDOVER_RESULT, errorReason, -1, Boolean.valueOf(result));
            }
            if (type == 2 && result) {
                this.mChrWifiHandoverType = WIFI_HANDOVER_TYPES[8];
                uploadChrWifiHandoverTypeStatistics(WIFI_HANDOVER_TYPES[8], HANDOVER_SUCC_CNT);
            }
        }
    }

    @Override // com.huawei.hwwifiproservice.IDualBandManagerCallback
    public void onDualBandNetWorkType(int type, List<HwDualBandMonitorInfo> apList, int count) {
        sendMessage(EVENT_DUALBAND_NETWROK_TYPE, type, -1, apList);
        if (type == 1 || type == 2) {
            uploadWifiDualBandTarget5gAp(count);
        }
    }

    @Override // com.huawei.hwwifiproservice.IDualBandManagerCallback
    public synchronized void onDualBandNetWorkFind(List<HwDualBandMonitorInfo> apList, int scanNum) {
        if (apList != null) {
            if (apList.size() != 0) {
                if (this.mDualBandMonitorStart) {
                    logI("onDualBandNetWorkFind  apList.size() = " + apList.size());
                    this.mDualBandMonitorStart = false;
                    this.mDualBandEstimateApList.clear();
                    this.mAvailable5GAPBssid = null;
                    this.mDualBandEstimateInfoSize = apList.size();
                    uploadWifiDualBandScanNum(scanNum);
                    for (HwDualBandMonitorInfo monitorInfo : apList) {
                        WifiProEstimateApInfo apInfo = new WifiProEstimateApInfo();
                        apInfo.setApBssid(monitorInfo.mBssid);
                        apInfo.setEstimateApSsid(monitorInfo.mSsid);
                        apInfo.setApAuthType(monitorInfo.mAuthType);
                        apInfo.setApRssi(monitorInfo.mCurrentRssi);
                        apInfo.setDualbandAPType(monitorInfo.mIsDualbandAp);
                        this.mDualBandEstimateApList.add(apInfo);
                        this.mNetworkQosMonitor.getApHistoryQualityScore(apInfo);
                        uploadDualbandAlgorithmicInfo(apInfo, scanNum);
                    }
                    refreshConnectedNetWork();
                    this.mDualBandEstimateInfoSize++;
                    WifiProEstimateApInfo currentApInfo = new WifiProEstimateApInfo();
                    currentApInfo.setApBssid(this.mCurrentBssid);
                    currentApInfo.setEstimateApSsid(this.mCurrentSsid);
                    currentApInfo.setApRssi(this.mCurrentRssi);
                    currentApInfo.set5GAP(false);
                    this.mDualBandEstimateApList.add(currentApInfo);
                    this.mNetworkQosMonitor.getApHistoryQualityScore(currentApInfo);
                    return;
                }
            }
        }
        loge("onDualBandNetWorkFind apList null error or mDualBandMonitorStart = " + this.mDualBandMonitorStart);
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public synchronized void onWifiBqeReturnRssiTH(WifiProEstimateApInfo apInfo) {
        if (apInfo == null) {
            loge("onWifiBqeReturnRssiTH apInfo null error");
        } else {
            sendMessage(EVENT_DUALBAND_RSSITH_RESULT, apInfo);
        }
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public synchronized void onWifiBqeReturnHistoryScore(WifiProEstimateApInfo apInfo) {
        if (apInfo == null) {
            loge("onWifiBqeReturnHistoryScore apInfo null error");
        } else {
            sendMessage(EVENT_DUALBAND_SCORE_RESULT, apInfo);
        }
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public synchronized void onWifiBqeReturnCurrentRssi(int rssi) {
        if (this.mDualBandManager != null) {
            this.mDualBandManager.updateCurrentRssi(rssi);
        }
    }

    /* access modifiers changed from: private */
    public void retryDualBandAPMonitor() {
        this.mDualBandMonitorInfoSize = this.mDualBandMonitorApList.size();
        if (this.mDualBandMonitorInfoSize == 0) {
            loge("retry dual band monitor error, monitorinfo size is zero");
            return;
        }
        Iterator<HwDualBandMonitorInfo> it = this.mDualBandMonitorApList.iterator();
        while (it.hasNext()) {
            HwDualBandMonitorInfo monitorInfo = it.next();
            WifiProEstimateApInfo apInfo = new WifiProEstimateApInfo();
            apInfo.setApBssid(monitorInfo.mBssid);
            apInfo.setApRssi(monitorInfo.mCurrentRssi);
            this.mNetworkQosMonitor.get5GApRssiThreshold(apInfo);
        }
    }

    /* access modifiers changed from: private */
    public void handleGetWifiTcpRx() {
        int currentWifiTcpRxCount = this.mNetworkQosMonitor.requestTcpRxPacketsCounter();
        int tcpRxThreshold = 3;
        PowerManager powerManager = this.mPowerManager;
        if (powerManager != null && !powerManager.isScreenOn()) {
            tcpRxThreshold = 5;
        }
        int increasedRxCount = currentWifiTcpRxCount - this.mWifiTcpRxCount;
        if (increasedRxCount > tcpRxThreshold) {
            logI("to query network Qos, tcpRxThreshold is " + tcpRxThreshold + ", increasedRxCount is " + increasedRxCount);
            String[] strArr = WIFI_HANDOVER_TYPES;
            this.mChrWifiHandoverType = strArr[6];
            uploadChrWifiHandoverTypeStatistics(strArr[6], HANDOVER_CNT);
            this.mNetworkQosMonitor.queryNetworkQos(1, this.mIsPortalAp, this.mIsNetworkAuthen, false);
            return;
        }
        if (getHandler().hasMessages(EVENT_GET_WIFI_TCPRX)) {
            removeMessages(EVENT_GET_WIFI_TCPRX);
        }
        sendMessageDelayed(EVENT_GET_WIFI_TCPRX, 5000);
    }

    /* access modifiers changed from: private */
    public void updateDualBandMonitorInfo(WifiProEstimateApInfo apInfo) {
        Iterator<HwDualBandMonitorInfo> it = this.mDualBandMonitorApList.iterator();
        while (it.hasNext()) {
            HwDualBandMonitorInfo monitorInfo = it.next();
            String bssid = monitorInfo.mBssid;
            if (bssid != null && apInfo != null && bssid.equals(apInfo.getApBssid())) {
                monitorInfo.mTargetRssi = apInfo.getRetRssiTH();
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateDualBandEstimateInfo(WifiProEstimateApInfo apInfo) {
        Iterator<WifiProEstimateApInfo> it = this.mDualBandEstimateApList.iterator();
        while (it.hasNext()) {
            WifiProEstimateApInfo estimateApInfo = it.next();
            String bssid = estimateApInfo.getApBssid();
            if (bssid != null && apInfo != null && bssid.equals(apInfo.getApBssid())) {
                estimateApInfo.setRetHistoryScore(apInfo.getRetHistoryScore());
                return;
            }
        }
    }

    private void showNoInternetDialog(int reason) {
        if (1 == reason) {
            this.mWifiProUIDisplayManager.showWifiProDialog(5);
        } else if (reason == 0) {
            this.mWifiProUIDisplayManager.showWifiProDialog(4);
        } else if (3 == reason) {
            this.mWifiProUIDisplayManager.showWifiProDialog(6);
        }
    }

    /* access modifiers changed from: private */
    public void chooseAvalibleDualBandAp() {
        logI("chooseAvalibleDualBandAp DualBandEstimateApList =" + this.mDualBandEstimateApList.toString());
        if (this.mDualBandEstimateApList.size() == 0 || this.mCurrentBssid == null) {
            Log.e(TAG, "chooseAvalibleDualBandAp ap size error");
            return;
        }
        this.mAvailable5GAPBssid = null;
        this.mAvailable5GAPSsid = null;
        this.mAvailable5GAPAuthType = 0;
        this.mDuanBandHandoverType = 0;
        WifiProEstimateApInfo bestAp = new WifiProEstimateApInfo();
        int currentApScore = 0;
        Iterator<WifiProEstimateApInfo> it = this.mDualBandEstimateApList.iterator();
        while (it.hasNext()) {
            WifiProEstimateApInfo apInfo = it.next();
            if (this.mCurrentBssid.equals(apInfo.getApBssid())) {
                currentApScore = apInfo.getRetHistoryScore();
            } else if (apInfo.getRetHistoryScore() > bestAp.getRetHistoryScore()) {
                bestAp = apInfo;
            }
        }
        logI("chooseAvalibleDualBandAp bestAp =" + bestAp.toString() + ", currentApScore =" + currentApScore);
        WifiInfo wifiInfo = this.mCurrWifiInfo;
        if (wifiInfo == null) {
            logI("chooseAvalibleDualBandAp mCurrWifiInfo is null");
            return;
        }
        if (this.mWiFiProEvaluateController.calculateSignalLevelHW(bestAp.is5GAP(), bestAp.getApRssi()) < WifiProCommonUtils.getCurrenSignalLevel(wifiInfo)) {
            logI("chooseAvalibleDualBandAp bestAp signalLevel is lower than current");
            uploadWifiDualBandFailReason(3);
            return;
        }
        handleScoreInfo(bestAp, currentApScore);
        chooseAvalible5GBand(bestAp);
    }

    private void handleScoreInfo(WifiProEstimateApInfo bestAp, int currentApScore) {
        if (bestAp != null) {
            int dualBandReason = 0;
            int score = bestAp.getRetHistoryScore();
            boolean isSameApReason = true;
            this.mIsChrQosBetterAfterDualbandHandover = score > currentApScore;
            if (score < 40 || bestAp.getApRssi() < -70) {
                boolean isSameAp = HwDualBandRelationManager.isDualBandAP(this.mCurrentBssid, bestAp.getApBssid());
                logI("isSameApHandOver = " + isSameAp);
                boolean isDiffReason = score >= currentApScore + 5 && bestAp.getApRssi() >= -70;
                if (!isSameAp || bestAp.getApRssi() < -65) {
                    isSameApReason = false;
                }
                if (isDiffReason || isSameApReason) {
                    setTarget5gApInfo(bestAp);
                    dualBandReason = isDiffReason ? 2 : 3;
                }
            } else {
                setTarget5gApInfo(bestAp);
                dualBandReason = 1;
            }
            uploadWifiDualBandInfo(dualBandReason);
        }
    }

    private void setTarget5gApInfo(WifiProEstimateApInfo targetAp) {
        if (targetAp != null) {
            this.mAvailable5GAPBssid = targetAp.getApBssid();
            this.mAvailable5GAPSsid = targetAp.getApSsid();
            this.mAvailable5GAPAuthType = targetAp.getApAuthType();
        }
    }

    private void chooseAvalible5GBand(WifiProEstimateApInfo bestAp) {
        HwDualBandManager hwDualBandManager;
        String str = this.mAvailable5GAPBssid;
        if (str == null) {
            List<HwDualBandMonitorInfo> mDualBandDeleteList = new ArrayList<>();
            Iterator<HwDualBandMonitorInfo> it = this.mDualBandMonitorApList.iterator();
            while (true) {
                if (it.hasNext()) {
                    HwDualBandMonitorInfo monitorInfo = it.next();
                    String bssid = monitorInfo.mBssid;
                    if (bssid != null && bssid.equals(bestAp.getApBssid()) && monitorInfo.mTargetRssi < -45) {
                        monitorInfo.mTargetRssi += 10;
                        break;
                    } else if (monitorInfo.mCurrentRssi >= -45) {
                        mDualBandDeleteList.add(monitorInfo);
                    }
                } else {
                    break;
                }
            }
            if (mDualBandDeleteList.size() > 0) {
                int dualBandDeleteListSize = mDualBandDeleteList.size();
                for (int i = 0; i < dualBandDeleteListSize; i++) {
                    logI("remove mix AP for RSSI > -45 DB RSSi = " + mDualBandDeleteList.get(i).mSsid);
                    this.mDualBandMonitorApList.remove(mDualBandDeleteList.get(i));
                }
            }
            if (this.mDualBandMonitorApList.size() != 0 && (hwDualBandManager = this.mDualBandManager) != null) {
                this.mDualBandMonitorStart = true;
                hwDualBandManager.startMonitor(this.mDualBandMonitorApList);
            }
        } else if (this.mHwDualBandBlackListMgr.isInWifiBlacklist(str) || this.mNetworkBlackListManager.isInWifiBlacklist(this.mAvailable5GAPBssid)) {
            long expiretime = this.mHwDualBandBlackListMgr.getExpireTimeForRetry(this.mAvailable5GAPBssid);
            logI("getExpireTimeForRetry for bssid " + StringUtilEx.safeDisplayBssid(this.mAvailable5GAPBssid) + ", time =" + expiretime);
            uploadWifiDualBandFailReason(2);
            sendMessageDelayed(EVENT_DUALBAND_DELAY_RETRY, expiretime);
        } else {
            logI("do dualband handover : " + bestAp.toString());
            sendMessage(EVENT_DUALBAND_5GAP_AVAILABLE);
        }
    }

    /* access modifiers changed from: private */
    public void addDualBandBlackList(String bssid) {
        String str;
        logI("addDualBandBlackList bssid = " + StringUtilEx.safeDisplayBssid(bssid) + ", mDualBandConnectApBssid = " + StringUtilEx.safeDisplayBssid(this.mDualBandConnectApBssid));
        if (bssid == null || (str = this.mDualBandConnectApBssid) == null || !str.equals(bssid)) {
            logI("addDualBandBlackList do nothing");
            return;
        }
        this.mDualBandConnectApBssid = null;
        if (System.currentTimeMillis() - this.mDualBandConnectTime > 1800000) {
            this.mHwDualBandBlackListMgr.addWifiBlacklist(bssid, true);
        } else {
            this.mHwDualBandBlackListMgr.addWifiBlacklist(bssid, false);
        }
    }

    /* access modifiers changed from: private */
    public void startDualBandManager() {
        HwDualBandManager hwDualBandManager = this.mDualBandManager;
        if (hwDualBandManager != null) {
            hwDualBandManager.startDualBandManger();
        } else {
            logE("ro.config.hw_wifipro_dualband is false, do nothing");
        }
    }

    /* access modifiers changed from: private */
    public void stopDualBandManager() {
        if (this.mDualBandManager != null) {
            stopDualBandMonitor();
            this.mDualBandManager.stopDualBandManger();
            return;
        }
        logE("ro.config.hw_wifipro_dualband is false, do nothing");
    }

    /* access modifiers changed from: private */
    public void stopDualBandMonitor() {
        HwDualBandManager hwDualBandManager;
        if (this.mDualBandMonitorStart && (hwDualBandManager = this.mDualBandManager) != null) {
            this.mDualBandMonitorStart = false;
            hwDualBandManager.stopMonitor();
        }
    }

    public int getNetwoksHandoverType() {
        return this.mWifiHandover.getNetwoksHandoverType();
    }

    /* access modifiers changed from: private */
    public void sendNetworkCheckingStatus(String action, String flag, int property) {
        Intent intent = new Intent(action);
        intent.setFlags(67108864);
        intent.putExtra(flag, property);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void notifyNetworkCheckResult(int result) {
        WifiConfiguration wifiConfiguration;
        int internetLevel = result;
        if (internetLevel == 5 && (wifiConfiguration = this.mCurrentWifiConfig) != null && WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration.internetHistory, 102)) {
            internetLevel = 6;
        }
        sendNetworkCheckingStatus(WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, internetLevel);
    }

    @Override // com.huawei.hwwifiproservice.INetworksHandoverCallBack
    public void onWifiConnected(boolean result, int reason) {
    }

    @Override // com.huawei.hwwifiproservice.INetworksHandoverCallBack
    public void onCheckAvailableWifi(boolean exist, int bestRssi, String targetBssid, int preferType, int freq) {
        boolean flag = exist;
        if (!isKeepCurrWiFiConnected()) {
            int rssilevel = WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo());
            int targetRssiLevel = WifiProCommonUtils.getSignalLevel(freq, bestRssi);
            if (flag && this.mNetworkBlackListManager.isInWifiBlacklist(targetBssid) && (targetRssiLevel <= 3 || targetRssiLevel - rssilevel < 2)) {
                logW("onCheckAvailableWifi, but wifi blacklists contain it, ignore the result.");
                flag = false;
            }
            logI("EVENT_CHECK_AVAILABLE_AP_RESULT: targetBssid = " + StringUtilEx.safeDisplayBssid(targetBssid) + ", exist = " + flag + ", prefer = " + preferType);
            sendMessage(EVENT_CHECK_AVAILABLE_AP_RESULT, targetRssiLevel, preferType, Boolean.valueOf(flag));
        }
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public void onWifiBqeDetectionResult(int result) {
        logI("onWifiBqeDetectionResult =  " + result);
        sendMessage(EVENT_WIFI_EVALUTE_TCPRTT_RESULT, result);
    }

    @Override // com.huawei.hwwifiproservice.INetworkQosCallBack
    public void onNotifyWifiSecurityStatus(Bundle bundle) {
        logI("onNotifyWifiSecurityStatus, bundle =  " + bundle);
        sendMessage(EVENT_WIFI_SECURITY_RESPONSE, bundle);
    }

    @Override // com.huawei.hwwifiproservice.IWifiProUICallBack
    public synchronized void onUserConfirm(int type, int status) {
        if (2 == status) {
            try {
                logI("UserConfirm  is OK ");
                sendMessage(EVENT_DIALOG_OK, type, -1);
            } catch (Throwable th) {
                throw th;
            }
        } else if (1 == status) {
            logI("UserConfirm  is CANCEL");
            sendMessage(EVENT_DIALOG_CANCEL, type, -1);
        }
    }

    public synchronized void userHandoverWifi() {
        logI("User Chose Rove In WiFi");
        sendMessage(EVENT_USER_ROVE_IN);
    }

    public void notifyHttpReachable(boolean isReachable) {
        if (isReachable || this.mPowerManager.isScreenOn()) {
            logI("SEC notifyHttpReachable " + isReachable);
            this.mNetworkQosMonitor.syncNotifyPowerSaveGenie(isReachable, 100, false);
        } else {
            logI("do not notify the PowerSaveGenie when the internet is unreachable becasue the screen is off ");
        }
        sendMessage(EVENT_HTTP_REACHABLE_RESULT, Boolean.valueOf(isReachable));
    }

    public void notifyHttpRedirectedForWifiPro() {
        logI("notifyHttpRedirectedForWifiPro");
        onNetworkDetectionResult(1, 6);
    }

    public void notifyRenewDhcpTimeoutForWifiPro() {
        logI("notifyRenewDhcpTimeoutForWifiPro");
        this.mIsWiFiNoInternet = true;
        this.mWsmChannel.sendMessage((int) INVALID_LINK_DETECTED);
        updateWifiInternetStateChange(-1);
        if (getHandler().hasMessages(EVENT_GET_WIFI_TCPRX)) {
            removeMessages(EVENT_GET_WIFI_TCPRX);
        }
        sendMessageDelayed(EVENT_GET_WIFI_TCPRX, 5000);
    }

    public void notifyWifiLinkPoor(boolean poorLink, int reason) {
        logI("HwWifiConnectivityMonitor notifyWifiLinkPoor = " + poorLink + " reason=" + reason);
        if (isKeepCurrWiFiConnected()) {
            return;
        }
        if (poorLink) {
            if (reason != 107 || !this.mIsWiFiNoInternet) {
                this.mHandoverReason = reason;
                this.mNotifyWifiLinkPoorReason = reason;
                this.mWifiHandoverStartTime = SystemClock.elapsedRealtime();
                sendMessage(EVENT_NOTIFY_WIFI_LINK_POOR, false);
                return;
            }
            logI("Wifi is nointernet, let NOINTERNET2WIFI to handle.");
        } else if (getCurrentState() == this.mWiFiProVerfyingLinkState) {
            onNetworkQosChange(1, 3, false);
        }
    }

    public void notifyRoamingCompleted(String newBssid) {
        if (newBssid != null && getCurrentState() == this.mWiFiProVerfyingLinkState) {
            sendMessageDelayed(EVENT_LAA_STATUS_CHANGED, 3000);
        }
    }

    /* access modifiers changed from: private */
    public void logI(String info) {
        Log.i(TAG, info);
    }

    /* access modifiers changed from: private */
    public void logD(String info) {
        Log.d(TAG, info);
    }

    /* access modifiers changed from: private */
    public void logW(String info) {
        Log.w(TAG, info);
    }

    /* access modifiers changed from: private */
    public void logE(String info) {
        Log.e(TAG, info);
    }

    public static void resetParameter() {
        mIsWifiManualEvaluating = false;
        mIsWifiSemiAutoEvaluating = false;
    }

    public void onDisableWiFiPro() {
        logI("WiFiProDisabledState is Enter");
        resetParameter();
        this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
        this.mWifiProUIDisplayManager.cancelAllDialog();
        this.mWifiProUIDisplayManager.shownAccessNotification(false);
        this.mWiFiProEvaluateController.cleanEvaluateRecords();
        HwIntelligenceWiFiManager hwIntelligenceWiFiManager = this.mHwIntelligenceWiFiManager;
        if (hwIntelligenceWiFiManager != null) {
            hwIntelligenceWiFiManager.stop();
        }
        stopDualBandManager();
        if (isWifiConnected()) {
            logI("WiFiProDisabledState , wifi is connect ");
            WifiInfo cInfo = this.mWifiManager.getConnectionInfo();
            if (cInfo != null && SupplicantState.COMPLETED == cInfo.getSupplicantState() && NetworkInfo.DetailedState.OBTAINING_IPADDR == WifiInfo.getDetailedStateOf(SupplicantState.COMPLETED)) {
                logI("wifi State == VERIFYING_POOR_LINK");
                this.mWsmChannel.sendMessage(131874);
            }
            setWifiCSPState(1);
        }
        diableResetVariables();
        disableTransitionNetState();
        if (this.mIsPrimaryUser) {
            uploadWifiproDisabledStatistics();
        }
    }

    private void diableResetVariables() {
        this.mWiFiProEvaluateController.forgetUntrustedOpenAp();
        this.mWiFiProPdpSwichValue = 0;
        this.mWifiProUIDisplayManager.cancelAllDialog();
        this.mCurrentVerfyCounter = 0;
        this.mIsUserHandoverWiFi = false;
        refreshConnectedNetWork();
        this.mIsWifiSemiAutoEvaluateComplete = false;
        resetWifiProManualConnect();
        stopDualBandMonitor();
    }

    private void disableTransitionNetState() {
        if (isWifiConnected()) {
            logI("onDisableWiFiPro,go to WifiConnectedState");
            this.mNetworkQosMonitor.queryNetworkQos(1, this.mIsPortalAp, this.mIsNetworkAuthen, false);
            transitionTo(this.mWifiConnectedState);
            return;
        }
        logI("onDisableWiFiPro, go to mWifiDisConnectedState");
        transitionTo(this.mWifiDisConnectedState);
    }

    private void registerMapNavigatingStateChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MAPS_LOCATION_FLAG), false, this.mMapNavigatingStateChangeObserver);
    }

    private void registerVehicleStateChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(VEHICLE_STATE_FLAG), false, this.mVehicleStateChangeObserver);
    }

    /* access modifiers changed from: private */
    public void setWifiMonitorEnabled(boolean enabled) {
        logI("setWifiLinkDataMonitorEnabled  is " + enabled);
        this.mNetworkQosMonitor.setMonitorWifiQos(1, enabled);
        this.mNetworkQosMonitor.setIpQosEnabled(enabled);
    }

    /* access modifiers changed from: private */
    public boolean isFullscreen() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 9, new Bundle());
        if (result != null) {
            return result.getBoolean("isFullscreen");
        }
        return false;
    }

    public Bundle getWifiDisplayInfo(NetworkInfo networkInfo) {
        Bundle result = new Bundle();
        result.putBoolean("result", false);
        if (networkInfo == null || this.mCurrentSsid == null) {
            return result;
        }
        if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
            result.putBoolean("result", true);
        }
        result.putString("ssid", this.mCurrentSsid);
        return result;
    }

    public void sendInternetCheckRequest() {
        logI("sendInternetCheckRequest");
        sendMessage(EVENT_WIFI_QOS_CHANGE, -1, 0, false);
    }

    public void notifyNetworkUserConnect(boolean isUserConnect) {
        logI("notifyNetworkUserConnect: isUserConnect = " + isUserConnect);
        sendMessage(EVENT_NETWORK_USER_CONNECT, Boolean.valueOf(isUserConnect));
    }

    public void notifyApkChangeWifiStatus(boolean enable, String packageName) {
        if (enable) {
            this.mCloseBySystemui = false;
        } else if (packageName.equals("com.android.systemui")) {
            this.mCloseBySystemui = true;
        }
    }

    public void notifyWifiDisconnected(Intent intent) {
        logI("notifyWifiDisconnected:EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED");
        sendMessage(EVENT_WIFI_DISCONNECTED_TO_DISCONNECTED, intent);
    }

    public void uploadWifiproDisabledStatistics() {
        long currentTimeMillis = SystemClock.elapsedRealtime();
        int topUid = -1;
        String pktName = "";
        HwAutoConnectManager autoConnectManager = HwAutoConnectManager.getInstance();
        if (autoConnectManager != null) {
            topUid = autoConnectManager.getCurrentTopUid();
            pktName = autoConnectManager.getCurrentPackageName();
            if (pktName != null && pktName.equals("com.huawei.hwstartupguide")) {
                this.mIsWifiproDisableOnReboot = false;
            }
        }
        if (topUid != -1 && pktName != null && !this.mIsWifiproDisableOnReboot) {
            long j = this.mLastWifiproDisableTime;
            if (j == 0 || currentTimeMillis - j > 7200000) {
                this.mLastWifiproDisableTime = currentTimeMillis;
                Bundle data = new Bundle();
                if (pktName.equals("com.android.settings")) {
                    data.putInt("appType", 101);
                    logI("appType == com.android.settings");
                } else if (pktName.equals("com.huawei.hwstartupguide")) {
                    data.putInt("appType", 102);
                    logI("appType == com.huawei.hwstartupguide");
                } else {
                    data.putInt("appType", 103);
                    logI("appType == 103");
                }
                Bundle dftEventData = new Bundle();
                dftEventData.putInt(EVENT_ID, WifiProCommonUtils.ID_WIFIPRO_DISABLE_INFO);
                dftEventData.putBundle(EVENT_DATA, data);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
            }
        }
    }

    public void uploadPortalAuthExpirationStatistics(boolean isNotificationClicked) {
        int validityDura = 0;
        int connDura = 0;
        if (this.mCurrentWifiConfig.portalValidityDuration < 86400000) {
            validityDura = (int) this.mCurrentWifiConfig.portalValidityDuration;
        }
        if (System.currentTimeMillis() - this.connectStartTime < 86400000) {
            connDura = (int) (System.currentTimeMillis() - this.connectStartTime);
        }
        logI("upload portal chr");
        Bundle data = new Bundle();
        data.putInt("dura", validityDura);
        data.putInt("isPeriodicDet", this.isPeriodicDet ? 1 : 0);
        data.putString("respCode", this.respCodeChrInfo);
        data.putInt("detNum", this.detectionNumSlow);
        data.putInt("connDura", connDura);
        data.putInt("isNotificationClicked", isNotificationClicked ? 1 : 0);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, WifiProCommonUtils.ID_PORTAL_AUTH_EXPIRATION_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        this.respCodeChrInfo = "";
        this.detectionNumSlow = 0;
    }

    /* access modifiers changed from: private */
    public boolean shouldUploadCloseWifiEvent() {
        if (this.mIsWiFiNoInternet || WifiProCommonUtils.getAirplaneModeOn(this.mContext) || this.mWifiManager.isWifiApEnabled()) {
            return false;
        }
        long deltaTime = System.currentTimeMillis() - this.mLastDisconnectedTimeStamp;
        if ((getCurrentState() != this.mWifiDisConnectedState && this.mCurrentRssi >= -75) || ((getCurrentState() == this.mWifiDisConnectedState && deltaTime > 10000) || this.mLastDisconnectedRssi >= -75)) {
            return false;
        }
        String pktName = "";
        HwAutoConnectManager autoConnectManager = HwAutoConnectManager.getInstance();
        if (autoConnectManager != null) {
            pktName = autoConnectManager.getCurrentPackageName();
        }
        if ("com.android.settings".equals(pktName) || this.mCloseBySystemui) {
            return true;
        }
        return false;
    }

    public void setLastDisconnectNetwork() {
        this.mLastConnectedSsid = this.mCurrentSsid;
        this.mLastDisconnectedTimeStamp = System.currentTimeMillis();
        this.mLastDisconnectedRssi = this.mCurrentRssi;
    }

    /* access modifiers changed from: private */
    public class WifiProPhoneStateListener extends PhoneStateListener {
        private WifiProPhoneStateListener() {
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CALL_STATE_CHANGED, state, -1);
        }
    }

    public void notifyWifiProConnect() {
        logI("notifyWifiProConnect");
        this.mNetworkQosMonitor.resetMonitorStatus();
        sendMessage(EVENT_WIFI_FIRST_CONNECTED);
    }

    /* access modifiers changed from: private */
    public void handleNetworkSpeedLimit(Bundle bundle) {
        if (bundle == null) {
            HwHiLog.e(TAG, false, "input bundle null.", new Object[0]);
        } else if (bundle.getInt("modemId", -1) != 0) {
            HwHiLog.d(TAG, false, "At present, the main card is only concerned.", new Object[0]);
        } else {
            int downLink = bundle.getInt("downLink");
            HwHiLog.d(TAG, false, "downLink = %{public}d, DOWNLINK_LIMIT = %{public}d", new Object[]{Integer.valueOf(downLink), Integer.valueOf(DOWNLINK_LIMIT)});
            if (downLink <= DOWNLINK_LIMIT) {
                HwHiLog.d(TAG, false, "show limit notify", new Object[0]);
                this.mIsLimitedSpeed = true;
                return;
            }
            HwHiLog.d(TAG, false, "show no limit notify", new Object[0]);
            this.mIsLimitedSpeed = false;
        }
    }

    private boolean isSmartDataSavingSwitchOff() {
        return this.mConnectivityManager.getRestrictBackgroundStatus() == 1;
    }

    /* access modifiers changed from: private */
    public boolean isNeedToSwitch2Cell() {
        if (this.mIsLimitedSpeed) {
            Log.i(TAG, "Cell Network speed is limited.");
            return false;
        } else if (isSmartDataSavingSwitchOff()) {
            return true;
        } else {
            Log.i(TAG, "SmartDataSavingSwitch is on");
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void registerBoosterService() {
        logI("registerBoosterCallback enter");
        IHwCommBoosterServiceManager hwCommBoosterServiceManager = HwFrameworkFactory.getHwCommBoosterServiceManager();
        if (hwCommBoosterServiceManager != null) {
            int ret = hwCommBoosterServiceManager.registerCallBack("com.huawei.hwwifiproservice", this.mHwCommBoosterCallback);
            if (ret != 0) {
                logE("registerCallBack failed, ret=" + ret);
                return;
            }
            return;
        }
        logE("HwCommBoosterServiceManager is null");
    }

    private class NetworkCheckThread extends Thread {
        private boolean mIsPortalNetwork = false;
        private boolean mIsWifiBackground = false;

        public NetworkCheckThread(boolean isPortal, boolean isWifiBackground) {
            super.setName("NetworkCheckThread");
            this.mIsPortalNetwork = isPortal;
            this.mIsWifiBackground = isWifiBackground;
        }

        public void run() {
            synchronized (WifiProStateMachine.this.mNetworkCheckLock) {
                String startBssid = WifiProCommonUtils.getCurrentBssid(WifiProStateMachine.this.mWifiManager);
                int respCode = WifiProStateMachine.this.mNetworkPropertyChecker.isCaptivePortal(true, this.mIsPortalNetwork, this.mIsWifiBackground);
                String endBssid = WifiProCommonUtils.getCurrentBssid(WifiProStateMachine.this.mWifiManager);
                if (startBssid != null && startBssid.equals(endBssid)) {
                    WifiProStateMachine.this.sendMessage(WifiProStateMachine.EVENT_CHECK_WIFI_NETWORK_RESULT, respCode, (int) WifiProStateMachine.this.mNetworkPropertyChecker.getDetectTime());
                }
            }
        }
    }

    public void uploadWifiSwitchFailTypeStatistics(int reason) {
        if (reason == 201) {
            this.mHandoverFailReason = 12;
        } else if (reason == 202) {
            this.mHandoverFailReason = 9;
        } else if (reason == 203) {
            this.mHandoverFailReason = 5;
        } else if (reason == 204) {
            this.mHandoverFailReason = 11;
        } else {
            this.mHandoverFailReason = reason;
        }
        logI("mHandoverFailReason = " + this.mHandoverFailReason);
        int i = this.mHandoverFailReason;
        if (i >= 0 && i < WIFI_HANDOVER_FAIL_TYPES.length) {
            Bundle data = new Bundle();
            data.putString(HANDOVER_TYPE, WIFI_HANDOVER_FAIL_TYPES[this.mHandoverFailReason]);
            data.putInt(HANDOVER_CNT, 1);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(EVENT_ID, ID_WIFI_HANDOVER_FAIL_INFO);
            dftEventData.putBundle(EVENT_DATA, data);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    public void setChrWifiDisconnectedReason() {
        this.mHandoverReason = 4;
    }

    public void uploadWifiSwitchStatistics() {
        String connTimeDes;
        Bundle data = new Bundle();
        data.putInt(HANDOVER_CNT, 1);
        int i = this.mHandoverReason;
        if (i >= 205) {
            this.mHandoverReason = i - 205;
        }
        if (this.mHandoverReason == 107) {
            this.mHandoverReason = 5;
        }
        int i2 = this.mHandoverReason;
        if (i2 >= 0) {
            String[] strArr = WIFI_HANDOVER_CAUSE_TYPES;
            if (i2 < strArr.length) {
                data.putString(WIFI_SWITCH_REASON, strArr[i2]);
                if (this.mHandoverReason != 4 || this.mWifiHandoverStartTime == 0) {
                    data.putString(WIFI_SWITCH_TIME_LEVEL, "");
                } else {
                    int connDura = (int) (SystemClock.elapsedRealtime() - this.mWifiHandoverStartTime);
                    int[] iArr = NORMAL_DURATION_INTERVAL;
                    if (connDura < iArr[0]) {
                        connTimeDes = WIFI_HANDOVER_DURAS[0];
                    } else if (connDura < iArr[1]) {
                        connTimeDes = WIFI_HANDOVER_DURAS[1];
                    } else if (connDura < iArr[2]) {
                        connTimeDes = WIFI_HANDOVER_DURAS[2];
                    } else {
                        connTimeDes = WIFI_HANDOVER_DURAS[3];
                    }
                    data.putString(WIFI_SWITCH_TIME_LEVEL, connTimeDes);
                }
                Bundle dftEventData = new Bundle();
                dftEventData.putInt(EVENT_ID, ID_WIFI_HANDOVER_REASON_INFO);
                dftEventData.putBundle(EVENT_DATA, data);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
                this.mWifiHandoverStartTime = 0;
            }
        }
        data.putString(WIFI_SWITCH_REASON, "");
        if (this.mHandoverReason != 4) {
        }
        data.putString(WIFI_SWITCH_TIME_LEVEL, "");
        Bundle dftEventData2 = new Bundle();
        dftEventData2.putInt(EVENT_ID, ID_WIFI_HANDOVER_REASON_INFO);
        dftEventData2.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData2);
        this.mWifiHandoverStartTime = 0;
    }

    private void uploadDualbandAlgorithmicInfo(WifiProEstimateApInfo apInfo, int scanNum) {
        Bundle apQualityInfo = this.mNetworkQosMonitor.getApHistoryQuality(apInfo);
        if (apQualityInfo != null) {
            apQualityInfo.putInt("ISSAMEAP", HwDualBandRelationManager.isDualBandAP(this.mCurrentBssid, apInfo.getApBssid()) ? 1 : 0);
            apQualityInfo.putInt("NUMSCAN", scanNum);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(EVENT_ID, CHR_ID_DUAL_BAND_EXCEPTION);
            dftEventData.putBundle(EVENT_DATA, apQualityInfo);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    /* access modifiers changed from: private */
    public void uploadChrWifiHandoverCell(boolean isValidated) {
        if (!isValidated) {
            uploadChrWifiHandoverTypeStatistics(WIFI_HANDOVER_TYPES[0], HANDOVER_CNT);
        } else {
            uploadChrWifiHandoverTypeStatistics(WIFI_HANDOVER_TYPES[1], HANDOVER_CNT);
        }
    }

    /* access modifiers changed from: private */
    public void uploadChrWifiHandoverWifi(boolean isValidated, boolean isWifiOnly) {
        if (!isValidated) {
            this.mChrWifiHandoverType = WIFI_HANDOVER_TYPES[2];
        } else if (isWifiOnly) {
            this.mChrWifiHandoverType = WIFI_HANDOVER_TYPES[4];
        } else {
            this.mChrWifiHandoverType = WIFI_HANDOVER_TYPES[3];
        }
        uploadChrWifiHandoverTypeStatistics(this.mChrWifiHandoverType, HANDOVER_CNT);
        uploadChrExceptionHandoverNetworkQuality(this.mChrWifiHandoverType);
    }

    public void uploadChrWifiHandoverTypeStatistics(String handoverType, String eventType) {
        if (TextUtils.isEmpty(handoverType) || TextUtils.isEmpty(eventType)) {
            logE("uploadChrWifiHandoverTypeStatistics error.");
            return;
        }
        if (HANDOVER_CNT.equals(eventType) && this.mCurrentWifiConfig != null && !WIFI_HANDOVER_TYPES[7].equals(handoverType)) {
            this.mChrQosLevelBeforeHandover = this.mCurrentWifiConfig.networkQosLevel;
        }
        if (HANDOVER_SUCC_CNT.equals(eventType) && getChrWifiHandoverTypeIndex(handoverType) >= 2) {
            if (getHandler().hasMessages(EVENT_CHR_CHECK_WIFI_HANDOVER)) {
                removeMessages(EVENT_CHR_CHECK_WIFI_HANDOVER);
            }
            sendMessageDelayed(EVENT_CHR_CHECK_WIFI_HANDOVER, 20000);
        }
        Bundle data = new Bundle();
        data.putString(HANDOVER_TYPE, handoverType);
        data.putInt(eventType, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, CHR_ID_WIFI_HANDOVER_TYPE);
        dftEventData.putBundle(EVENT_DATA, data);
        logI("uploadChrWifiHandoverTypeStatistics dftEventData = " + dftEventData);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    private int getChrWifiHandoverTypeIndex(String type) {
        if (TextUtils.isEmpty(type)) {
            return -1;
        }
        int index = 0;
        while (true) {
            String[] strArr = WIFI_HANDOVER_TYPES;
            if (index >= strArr.length) {
                return -1;
            }
            if (type.equals(strArr[index])) {
                return index;
            }
            index++;
        }
    }

    /* access modifiers changed from: private */
    public void handleChrWifiHandoverCheck() {
        if (TextUtils.isEmpty(this.mChrWifiHandoverType) || this.mCurrentWifiConfig == null) {
            logE("wifi is disconnected or not happen wifi handover.");
            return;
        }
        if (!this.mIsWiFiNoInternet) {
            logI("after wifi handover, wifi has internet.");
            uploadChrWifiHandoverTypeStatistics(this.mChrWifiHandoverType, HANDOVER_OK_CNT);
        }
        if (this.mCurrentWifiConfig.networkQosLevel > this.mChrQosLevelBeforeHandover) {
            logI("after wifi handover, wifi qos level is better.");
            uploadChrWifiHandoverTypeStatistics(this.mChrWifiHandoverType, HANDOVER_BETTER_CNT);
        }
    }

    /* access modifiers changed from: private */
    public void uploadChrHandoverUnexpectedTypes(String unexpectedType) {
        if (TextUtils.isEmpty(unexpectedType)) {
            logE("uploadChrHandoverUnexpectedTypes error.");
            return;
        }
        logI("uploadChrHandoverUnexpectedTypes unexpectedType = " + unexpectedType);
        Bundle data = new Bundle();
        data.putString(HANDOVER_TYPE, unexpectedType);
        data.putInt(HANDOVER_CNT, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, CHR_ID_WIFI_HANDOVER_UNEXPECTED_TYPE);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    private void uploadChrExceptionHandoverNetworkQuality(String handoverType) {
        if (TextUtils.isEmpty(handoverType)) {
            logE("uploadChrExceptionHandoverNetworkQuality error.");
            return;
        }
        Bundle chrNetworkQuality = this.mNetworkQosMonitor.getChrHandoverNetworkQuality();
        if (chrNetworkQuality != null) {
            chrNetworkQuality.putString("TYPE", handoverType);
            chrNetworkQuality.putInt("RSSI", this.mCurrentRssi);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(EVENT_ID, CHR_ID_HANDOVER_EXCEPTION_NETWORK_QUALITY);
            dftEventData.putBundle(EVENT_DATA, chrNetworkQuality);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    private void uploadWifiDualBandInfo(int dualBandReason) {
        if (dualBandReason != 0) {
            uploadWifiDualBandTriggerReason(dualBandReason);
        } else {
            uploadWifiDualBandFailReason(3);
        }
    }

    public void uploadWifiDualBandTriggerReason(int reason) {
        Bundle data = new Bundle();
        data.putString(HANDOVER_TYPE, String.format(Locale.ROOT, "reason%d", Integer.valueOf(reason)));
        data.putInt(HANDOVER_CNT, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_TRIGGER_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    public void uploadWifiDualBandFailReason(int reason) {
        int dualbandFailReason;
        if (reason == 1 || reason == 10 || reason == 11) {
            dualbandFailReason = 1;
        } else if (reason == 2 || reason == 3) {
            dualbandFailReason = reason;
        } else if (reason == -7 || reason == -6) {
            dualbandFailReason = 4;
        } else {
            logE("reason = " + reason);
            return;
        }
        Bundle data = new Bundle();
        data.putString(HANDOVER_TYPE, String.format(Locale.ROOT, "reason%d", Integer.valueOf(dualbandFailReason)));
        data.putInt(HANDOVER_CNT, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_FAIL_REASON_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    public void uploadWifiDualBandScanNum(int count) {
        String countLevel;
        if (count > DUALBAND_SCAN_COUNT_INTERVAL[0] || count < 0) {
            int[] iArr = DUALBAND_SCAN_COUNT_INTERVAL;
            if (count > iArr[1] || count <= iArr[0]) {
                int[] iArr2 = DUALBAND_SCAN_COUNT_INTERVAL;
                if (count > iArr2[2] || count <= iArr2[1]) {
                    countLevel = WIFI_HANDOVER_5G_SCAN_LEVEL_TYPES[3];
                } else {
                    countLevel = WIFI_HANDOVER_5G_SCAN_LEVEL_TYPES[2];
                }
            } else {
                countLevel = WIFI_HANDOVER_5G_SCAN_LEVEL_TYPES[1];
            }
        } else {
            countLevel = WIFI_HANDOVER_5G_SCAN_LEVEL_TYPES[0];
        }
        Bundle data = new Bundle();
        data.putString(WIFI_SWITCH_TIME_LEVEL, countLevel);
        data.putInt(HANDOVER_CNT, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_SCAN_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    public void uploadWifiDualBandHandoverDura() {
        String duraLevel;
        int dualBandDura = (int) (SystemClock.elapsedRealtime() - this.mWifiDualBandStartTime);
        if (dualBandDura > 0 && dualBandDura <= 180000) {
            int[] iArr = DUALBAND_DURATION_INTERVAL;
            if (dualBandDura <= iArr[0]) {
                duraLevel = WIFI_HANDOVER_5G_DURA_LEVEL_TYPES[0];
            } else if (dualBandDura > iArr[1] || dualBandDura <= iArr[0]) {
                int[] iArr2 = DUALBAND_DURATION_INTERVAL;
                if (dualBandDura > iArr2[2] || dualBandDura <= iArr2[1]) {
                    duraLevel = WIFI_HANDOVER_5G_DURA_LEVEL_TYPES[3];
                } else {
                    duraLevel = WIFI_HANDOVER_5G_DURA_LEVEL_TYPES[2];
                }
            } else {
                duraLevel = WIFI_HANDOVER_5G_DURA_LEVEL_TYPES[1];
            }
            Bundle data = new Bundle();
            if (this.mIsChrQosBetterAfterDualbandHandover) {
                data.putInt("BETTERNETCNT", 1);
            }
            data.putString(WIFI_SWITCH_TIME_LEVEL, duraLevel);
            data.putInt(HANDOVER_CNT, 1);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_DURATION_INFO);
            dftEventData.putBundle(EVENT_DATA, data);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    public void uploadWifiDualBandTarget5gAp(int count) {
        String countLevel;
        if (count > DUALBAND_TARGET_AP_COUNT_INTERVAL[0] || count < 0) {
            int[] iArr = DUALBAND_TARGET_AP_COUNT_INTERVAL;
            if (count > iArr[1] || count <= iArr[0]) {
                int[] iArr2 = DUALBAND_TARGET_AP_COUNT_INTERVAL;
                if (count > iArr2[2] || count <= iArr2[1]) {
                    countLevel = WIFI_HANDOVER_5G_AP_NUM_LEVEL_TYPES[3];
                } else {
                    countLevel = WIFI_HANDOVER_5G_AP_NUM_LEVEL_TYPES[2];
                }
            } else {
                countLevel = WIFI_HANDOVER_5G_AP_NUM_LEVEL_TYPES[1];
            }
        } else {
            countLevel = WIFI_HANDOVER_5G_AP_NUM_LEVEL_TYPES[0];
        }
        Bundle data = new Bundle();
        data.putString(WIFI_SWITCH_TIME_LEVEL, countLevel);
        data.putInt(HANDOVER_CNT, 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_TARGET_AP_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }

    /* access modifiers changed from: private */
    public void uploadChrDualBandOnLineTime() {
        if (SystemClock.elapsedRealtime() - this.mChrDualbandConnectedStartTime > 300000) {
            logI("chr dualband handover online time.");
            Bundle data = new Bundle();
            data.putInt("ONLINETIME", 1);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_DURATION_INFO);
            dftEventData.putBundle(EVENT_DATA, data);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    /* access modifiers changed from: private */
    public void uploadChrDualBandSameApCount() {
        Bundle data = new Bundle();
        data.putInt("SAMEAPCNT", 1);
        Bundle dftEventData = new Bundle();
        dftEventData.putInt(EVENT_ID, ID_WIFI_DUALBAND_DURATION_INFO);
        dftEventData.putBundle(EVENT_DATA, data);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
    }
}

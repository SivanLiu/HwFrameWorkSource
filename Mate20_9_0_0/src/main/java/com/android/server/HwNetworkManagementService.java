package com.android.server;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.booster.IHwCommBoosterCallback;
import android.net.booster.IHwCommBoosterCallback.Stub;
import android.net.booster.IHwCommBoosterServiceManager;
import android.net.wifi.HuaweiApConfiguration;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.SettingsEx.Systemex;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.NativeDaemonConnector.Command;
import com.android.server.NativeDaemonConnector.SensitiveArg;
import com.android.server.NetworkManagementService.SystemServices;
import com.android.server.display.Utils;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.dev.SceneInfo;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.systemui.shared.system.MetricsLoggerCompat;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HwNetworkManagementService extends NetworkManagementService {
    private static final String ACTION_WIFI_AP_STA_JOIN = "android.net.wifi.WIFI_AP_STA_JOIN";
    private static final String ACTION_WIFI_AP_STA_LEAVE = "android.net.wifi.WIFI_AP_STA_LEAVE";
    private static final String AD_APKDL_STRATEGY = "com.huawei.permission.AD_APKDL_STRATEGY";
    private static final String AD_APKDL_STRATEGY_PERMISSION = "com.huawei.permission.AD_APKDL_STRATEGY";
    private static final int AD_STRATEGY = 0;
    private static final int APK_CONTROL_STRATEGY = 2;
    private static final int APK_DL_STRATEGY = 1;
    private static final String ARG_ADD = "add";
    private static final String ARG_CLEAR = "clear";
    private static final String ARG_IP_WHITELIST = "ipwhitelist";
    private static final String ARG_SET = "set";
    private static final String BROWSER_UID_INFO = "BrowserUidInfo";
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final String CMD_NET_FILTER = "net_filter";
    private static final int CODE_AD_DEBUG = 1019;
    private static final int CODE_CLEAN_AD_STRATEGY = 1018;
    private static final int CODE_CLEAR_AD_APKDL_STRATEGY = 1103;
    private static final int CODE_CLOSE_SOCKETS_FOR_UID = 1107;
    private static final int CODE_GET_AD_KEY_LIST = 1016;
    private static final int CODE_GET_APLINKED_STA_LIST = 1005;
    private static final int CODE_GET_WIFI_DNS_STAT = 1011;
    private static final int CODE_PRINT_AD_APKDL_STRATEGY = 1104;
    private static final int CODE_REMOVE_LEGACYROUTE_TO_HOST = 1015;
    private static final int CODE_SET_AD_STRATEGY = 1017;
    private static final int CODE_SET_AD_STRATEGY_RULE = 1101;
    private static final int CODE_SET_APK_CONTROL_STRATEGY = 1109;
    private static final int CODE_SET_APK_DL_STRATEGY = 1102;
    private static final int CODE_SET_APK_DL_URL_USER_RESULT = 1105;
    private static final int CODE_SET_AP_CONFIGRATION_HW = 1008;
    private static final int CODE_SET_CHR_REPORT_APP_LIST = 1108;
    private static final int CODE_SET_FIREWALL_RULE_FOR_PID = 1110;
    private static final int CODE_SET_NETWORK_ACCESS_WHITELIST = 1106;
    private static final int CODE_SET_SOFTAP_DISASSOCIATESTA = 1007;
    private static final int CODE_SET_SOFTAP_MACFILTER = 1006;
    private static final int CODE_SET_SOFTAP_TX_POWER = 1009;
    private static final int DATA_SEND_TO_KERNEL_APP_QOE_RSRP = 4;
    private static final int DATA_SEND_TO_KERNEL_APP_QOE_UID = 3;
    private static final int DATA_SEND_TO_KERNEL_BS_SUPPORT_VIDEO_ACC = 1;
    private static final int DATA_SEND_TO_KERNEL_SUPPORT_AI_CHANGE = 2;
    private static final int DATA_SEND_TO_NETD_PRE_DNS_APP_UID = 5;
    private static final int DATA_SEND_TO_NETD_PRE_DNS_BROWSER_UID = 6;
    private static final int DATA_SEND_TO_NETD_PRE_DNS_TOP_DOMAIN = 7;
    private static final int DATA_SEND_TO_NETD_PRE_DNS_UID_FOREGROUND = 8;
    private static final int DEFAULT_ISMCOEX_WIFI_AP_CHANNEL = 11;
    private static final int DEFAULT_WIFI_AP_CHANNEL = 0;
    private static final int DEFAULT_WIFI_AP_MAXSCB = 8;
    private static final int DEFAULT_WIFI_AP_MAX_CONNECTIONS = 8;
    private static final String DESCRIPTOR = "android.net.wifi.INetworkManager";
    private static final String DESCRIPTOR_ADCLEANER_MANAGER_Ex = "android.os.AdCleanerManagerEx";
    private static final String DESCRIPTOR_HW_AD_CLEANER = "android.view.HwAdCleaner";
    private static final String DESCRIPTOR_NETWORKMANAGEMENT_SERVICE = "android.os.INetworkManagementService";
    private static final String DNS_DOMAIN_NAME = "DnsDomainName";
    private static final String EVENT_KEY = "event_key";
    private static final int EVENT_REGISTER_BOOSTER_CALLBACK = 0;
    private static final String EXP_INFO_REPORT_ENABLE = "ExpInfoReportState";
    private static final String EXTRA_CURRENT_TIME = "currentTime";
    private static final String EXTRA_STA_COUNT = "staCount";
    private static final String EXTRA_STA_INFO = "macInfo";
    private static final String FOREGROUND_STATE = "ForegroundState";
    private static final String FOREGROUND_UID = "ForegroundUid";
    private static final String HEX_STR = "0123456789ABCDEF";
    private static final int HIDATA_APP_QOE_TO_KERNEL_MSG_TYPE_RSRP = 1;
    private static final int HIDATA_APP_QOE_TO_KERNEL_MSG_TYPE_UID_PERIOD = 2;
    private static final int HSM_TRANSACT_CODE = 201;
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final String INTENT_APKDL_URL_DETECTED = "com.android.intent.action.apkdl_url_detected";
    private static final String INTENT_DS_WIFI_WEB_STAT_REPORT = "com.huawei.chr.wifi.action.web_stat_report";
    private static final String ISM_COEX_ON = "ro.config.hw_ismcoex";
    private static final int KERNEL_DATA_MEDIA_INFO = 1;
    private static final int KERNEL_DATA_NET_SPEED_EXP_INFO = 2;
    private static final String MAC_KEY = "mac_key";
    private static final int MAX_ARGC_PER_COMMAND = 12;
    private static final int PER_STRATEGY_SIZE = 470;
    private static final int PER_UID_LIST_SIZE = 50;
    private static final int STATE_ON = 1;
    private static final String STA_JOIN_EVENT = "STA_JOIN";
    private static final int STA_JOIN_HANDLE_DELAY = 5000;
    private static final String STA_LEAVE_EVENT = "STA_LEAVE";
    private static final String TAG = HwNetworkManagementService.class.getSimpleName();
    private static final String TOP_APP_UID_INFO = "TogAppUidInfo";
    private static final String VIDEO_INFO_REPORT_ENABLE = "VideoInfoReportState";
    private static final int WEB_STAT = 0;
    private static final int WIFI_STAT_DELTA = 231;
    private Map<String, List<String>> mAdIdMap = new HashMap();
    private Map<String, List<String>> mAdViewMap = new HashMap();
    private Handler mApLinkedStaHandler = new Handler() {
        public void handleMessage(Message msg) {
            String access$200;
            StringBuilder stringBuilder;
            String action = null;
            long mCurrentTime = System.currentTimeMillis();
            Bundle bundle = msg.getData();
            String event = bundle.getString(HwNetworkManagementService.EVENT_KEY);
            String macStr = bundle.getString(HwNetworkManagementService.MAC_KEY).toLowerCase();
            if (HwNetworkManagementService.STA_JOIN_EVENT.equals(event)) {
                action = HwNetworkManagementService.ACTION_WIFI_AP_STA_JOIN;
                if (HwNetworkManagementService.this.mMacList.contains(macStr)) {
                    access$200 = HwNetworkManagementService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(macStr);
                    stringBuilder.append(" had been added, but still get event ");
                    stringBuilder.append(event);
                    Slog.e(access$200, stringBuilder.toString());
                } else {
                    HwNetworkManagementService.this.mMacList.add(macStr);
                    HwNetworkManagementService.this.mLinkedStaCount = HwNetworkManagementService.this.mLinkedStaCount + 1;
                }
            } else if (HwNetworkManagementService.STA_LEAVE_EVENT.equals(event)) {
                action = HwNetworkManagementService.ACTION_WIFI_AP_STA_LEAVE;
                if (HwNetworkManagementService.this.mApLinkedStaHandler.hasMessages(msg.what)) {
                    HwNetworkManagementService.this.mApLinkedStaHandler.removeMessages(msg.what);
                    access$200 = HwNetworkManagementService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("event=");
                    stringBuilder.append(event);
                    stringBuilder.append(", remove STA_JOIN message");
                    Slog.d(access$200, stringBuilder.toString());
                } else if (HwNetworkManagementService.this.mMacList.contains(macStr)) {
                    HwNetworkManagementService.this.mMacList.remove(macStr);
                    HwNetworkManagementService.this.mLinkedStaCount = HwNetworkManagementService.this.mLinkedStaCount - 1;
                } else {
                    access$200 = HwNetworkManagementService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(macStr);
                    stringBuilder.append(" had been removed, but still get event ");
                    stringBuilder.append(event);
                    Slog.e(access$200, stringBuilder.toString());
                }
            }
            access$200 = HwNetworkManagementService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handle ");
            stringBuilder.append(event);
            stringBuilder.append(" event, mLinkedStaCount=");
            stringBuilder.append(HwNetworkManagementService.this.mLinkedStaCount);
            Slog.d(access$200, stringBuilder.toString());
            if (HwNetworkManagementService.this.mLinkedStaCount < 0 || HwNetworkManagementService.this.mLinkedStaCount > 8 || HwNetworkManagementService.this.mLinkedStaCount != HwNetworkManagementService.this.mMacList.size()) {
                access$200 = HwNetworkManagementService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mLinkedStaCount over flow, need synchronize. value = ");
                stringBuilder2.append(HwNetworkManagementService.this.mLinkedStaCount);
                Slog.e(access$200, stringBuilder2.toString());
                try {
                    String[] macList = HwNetworkManagementService.this.mConnector.doListCommand("softap", 121, new Object[]{"assoclist"});
                    HwNetworkManagementService.this.mMacList = new ArrayList();
                    if (macList == null) {
                        HwNetworkManagementService.this.mLinkedStaCount = 0;
                    } else {
                        for (String mac : macList) {
                            if (mac == null) {
                                Slog.e(HwNetworkManagementService.TAG, "get mac from macList is null");
                            } else {
                                HwNetworkManagementService.this.mMacList.add(mac.toLowerCase());
                            }
                        }
                        HwNetworkManagementService.this.mLinkedStaCount = HwNetworkManagementService.this.mMacList.size();
                    }
                } catch (NativeDaemonConnectorException e) {
                    Slog.e(HwNetworkManagementService.TAG, "Cannot communicate with native daemon to get linked stations list");
                    HwNetworkManagementService.this.mMacList = new ArrayList();
                    HwNetworkManagementService.this.mLinkedStaCount = 0;
                }
            }
            access$200 = String.format("MAC=%s TIME=%d STACNT=%d", new Object[]{macStr, Long.valueOf(mCurrentTime), Integer.valueOf(HwNetworkManagementService.this.mLinkedStaCount)});
            String access$2002 = HwNetworkManagementService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("send broadcast, event=");
            stringBuilder3.append(event);
            stringBuilder3.append(", extraInfo: ");
            stringBuilder3.append(access$200);
            Slog.e(access$2002, stringBuilder3.toString());
            Intent broadcast = new Intent(action);
            broadcast.putExtra(HwNetworkManagementService.EXTRA_STA_INFO, macStr);
            broadcast.putExtra(HwNetworkManagementService.EXTRA_CURRENT_TIME, mCurrentTime);
            broadcast.putExtra(HwNetworkManagementService.EXTRA_STA_COUNT, HwNetworkManagementService.this.mLinkedStaCount);
            HwNetworkManagementService.this.mContext.sendBroadcast(broadcast, "android.permission.ACCESS_WIFI_STATE");
        }
    };
    private boolean mBoosterEnabled = SystemProperties.getBoolean("ro.config.hw_booster", false);
    private boolean mBoosterNetAiChangeEnabled = SystemProperties.getBoolean("ro.config.hisi_net_ai_change", false);
    private boolean mBoosterPreDnsEnabled = SystemProperties.getBoolean("ro.config.pre_dns_query", false);
    private boolean mBoosterVideoAccEnabled = SystemProperties.getBoolean("ro.config.hisi_video_acc", false);
    private int mChannel;
    private AtomicInteger mCmdId;
    private NativeDaemonConnector mConnector;
    private Context mContext;
    private HuaweiApConfiguration mHwApConfig;
    private final BroadcastReceiver mHwSystemServerStartReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Slog.d(HwNetworkManagementService.TAG, "HwSystemServerStartReceiver intent=null");
                return;
            }
            String action = intent.getAction();
            String access$200 = HwNetworkManagementService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwSystemServerStartReceiver action=");
            stringBuilder.append(action);
            Slog.d(access$200, stringBuilder.toString());
            if (HwNetworkManagementService.HW_SYSTEM_SERVER_START.equals(action) && HwNetworkManagementService.this.mBoosterEnabled) {
                HwNetworkManagementService.this.handleRegisterBoosterCallback();
            }
        }
    };
    private IHwCommBoosterCallback mIHwCommBoosterCallback = new Stub() {
        public void callBack(int type, Bundle b) throws RemoteException {
            if (b != null) {
                switch (type) {
                    case 1:
                        if (1 != b.getInt(HwNetworkManagementService.VIDEO_INFO_REPORT_ENABLE)) {
                            HwNetworkManagementService.this.setNetBoosterVodEnabled(false);
                            break;
                        } else {
                            HwNetworkManagementService.this.setNetBoosterVodEnabled(true);
                            break;
                        }
                    case 2:
                        if (1 != b.getInt(HwNetworkManagementService.EXP_INFO_REPORT_ENABLE)) {
                            HwNetworkManagementService.this.setNetBoosterKsiEnabled(false);
                            break;
                        } else {
                            HwNetworkManagementService.this.setNetBoosterKsiEnabled(true);
                            break;
                        }
                    case 3:
                        HwNetworkManagementService.this.setNetBoosterAppUid(b);
                        break;
                    case 4:
                        HwNetworkManagementService.this.setNetBoosterRsrpRsrq(b);
                        break;
                    case 5:
                        HwNetworkManagementService.this.setNetBoosterPreDnsAppUid(b);
                        break;
                    case 6:
                        HwNetworkManagementService.this.setNetBoosterPreDnsBrowerUid(b);
                        break;
                    case 7:
                        HwNetworkManagementService.this.setNetBoosterPreDnsDomainName(b);
                        break;
                    case 8:
                        HwNetworkManagementService.this.setNetBoosterUidForeground(b);
                        break;
                }
            }
        }
    };
    private int mLinkedStaCount = 0;
    private List<String> mMacList = new ArrayList();
    private String mSoftapIface;
    private String mWlanIface;
    private Pattern p = Pattern.compile("^.*max=([0-9]+);idx=([0-9]+);(.*)$");
    private HashMap<String, Long> startTimeMap = new HashMap();
    private StringBuffer urlBuffer = new StringBuffer();

    public static class BoosterConstants {
        public static final int ERROR_INVALID_PARAM = -3;
        public static final int ERROR_NO_SERVICE = -1;
        public static final int ERROR_REMOTE_EXCEPTION = -2;
        public static final int SUCCESS = 0;
    }

    static class NetdResponseCode {
        public static final int ApLinkedStaListChangeHISI = 651;
        public static final int ApLinkedStaListChangeQCOM = 901;
        public static final int HwDnsStat = 130;
        public static final int SoftapDhcpListResult = 122;
        public static final int SoftapListResult = 121;

        NetdResponseCode() {
        }
    }

    private void handleRegisterBoosterCallback() {
        Slog.d(TAG, "handleRegisterBoosterCallback");
        IHwCommBoosterServiceManager bm = HwFrameworkFactory.getHwCommBoosterServiceManager();
        if (bm != null) {
            int ret = bm.registerCallBack("com.android.server", this.mIHwCommBoosterCallback);
            if (ret != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleRegisterBoosterCallback:registerCallBack failed, ret=");
                stringBuilder.append(ret);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            return;
        }
        Slog.e(TAG, "handleRegisterBoosterCallback:null HwCommBoosterServiceManager");
    }

    public HwNetworkManagementService(Context context, String socket, SystemServices services) {
        super(context, socket, services);
        this.mContext = context;
        this.mCmdId = new AtomicInteger(0);
        IntentFilter filter = new IntentFilter(HW_SYSTEM_SERVER_START);
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mHwSystemServerStartReceiver, filter);
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        List<String> result;
        String stats;
        if (code == 201) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            return executeHsmCommand(data, reply);
        } else if (code == 1005) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            result = getApLinkedStaList();
            reply.writeNoException();
            reply.writeStringList(result);
            return true;
        } else if (code == 1006) {
            Slog.d(TAG, "code == CODE_SET_SOFTAP_MACFILTER");
            data.enforceInterface(DESCRIPTOR);
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            setSoftapMacFilter(data.readString());
            reply.writeNoException();
            return true;
        } else if (code == 1007) {
            Slog.d(TAG, "code == CODE_SET_SOFTAP_DISASSOCIATESTA");
            data.enforceInterface(DESCRIPTOR);
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            setSoftapDisassociateSta(data.readString());
            reply.writeNoException();
            return true;
        } else if (code == 1008) {
            Slog.d(TAG, "code == CODE_SET_AP_CONFIGRATION_HW");
            data.enforceInterface(DESCRIPTOR);
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            this.mWlanIface = data.readString();
            this.mSoftapIface = data.readString();
            reply.writeNoException();
            setAccessPointHw(this.mWlanIface, this.mSoftapIface);
            return true;
        } else if (code == 1009) {
            Slog.d(TAG, "code == CODE_SET_SOFTAP_TX_POWER");
            data.enforceInterface(DESCRIPTOR);
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            setWifiTxPower(data.readString());
            reply.writeNoException();
            return true;
        } else if (code == 1011) {
            Slog.d(TAG, "code == CODE_GET_WIFI_DNS_STAT");
            data.enforceInterface(DESCRIPTOR);
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
            stats = getWiFiDnsStats(data.readInt());
            reply.writeNoException();
            reply.writeString(stats);
            return true;
        } else {
            boolean result2 = false;
            String str;
            StringBuilder stringBuilder;
            int size;
            String str2;
            StringBuilder stringBuilder2;
            String key;
            String str3;
            StringBuilder stringBuilder3;
            int i;
            String str4;
            int flag;
            ArrayList<String> adAppList;
            StringBuilder stringBuilder4;
            String appName;
            StringBuilder stringBuilder5;
            String[] pkgName;
            if (code == 1017) {
                Slog.d(TAG, "code == CODE_SET_AD_STRATEGY");
                data.enforceInterface(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.AD_APKDL_STRATEGY", "permission denied");
                boolean needReset = data.readInt() > 0;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CODE_SET_AD_STRATEGY, needReset: ");
                stringBuilder.append(needReset);
                Slog.d(str, stringBuilder.toString());
                if (needReset) {
                    this.mAdViewMap.clear();
                    this.mAdIdMap.clear();
                }
                size = data.readInt();
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CODE_SET_AD_STRATEGY, mAdViewMap size: ");
                stringBuilder2.append(size);
                Slog.d(str2, stringBuilder2.toString());
                if (size > 0) {
                    for (int i2 = 0; i2 < size; i2++) {
                        key = data.readString();
                        ArrayList<String> value = data.createStringArrayList();
                        str3 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("CODE_SET_AD_STRATEGY, mAdViewMap key: ");
                        stringBuilder3.append(key);
                        stringBuilder3.append(", at ");
                        stringBuilder3.append(i2);
                        Slog.d(str3, stringBuilder3.toString());
                        this.mAdViewMap.put(key, value);
                    }
                }
                size = data.readInt();
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CODE_SET_AD_STRATEGY, mAdIdMap size: ");
                stringBuilder2.append(size);
                Slog.d(str2, stringBuilder2.toString());
                if (size > 0) {
                    while (i < size) {
                        str2 = data.readString();
                        ArrayList<String> value2 = data.createStringArrayList();
                        str4 = TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("CODE_SET_AD_STRATEGY, mAdIdMap key: ");
                        stringBuilder6.append(str2);
                        stringBuilder6.append(", at ");
                        stringBuilder6.append(i);
                        Slog.d(str4, stringBuilder6.toString());
                        this.mAdIdMap.put(str2, value2);
                        i++;
                    }
                }
                reply.writeNoException();
                return true;
            } else if (code == 1018) {
                Slog.d(TAG, "code == CODE_CLEAN_AD_STRATEGY");
                data.enforceInterface(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.AD_APKDL_STRATEGY", "permission denied");
                flag = data.readInt();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CODE_CLEAN_AD_STRATEGY, flag: ");
                stringBuilder.append(flag);
                Slog.d(str, stringBuilder.toString());
                if (1 == flag) {
                    this.mAdViewMap.clear();
                    this.mAdIdMap.clear();
                } else if (flag == 0) {
                    adAppList = data.createStringArrayList();
                    Slog.d(TAG, "CODE_CLEAN_AD_STRATEGY adAppList: ");
                    if (adAppList != null) {
                        while (i < adAppList.size()) {
                            str2 = (String) adAppList.get(i);
                            key = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append(i);
                            stringBuilder4.append(" = ");
                            stringBuilder4.append(str2);
                            Slog.d(key, stringBuilder4.toString());
                            if (this.mAdViewMap.containsKey(str2)) {
                                this.mAdViewMap.remove(str2);
                            }
                            if (this.mAdIdMap.containsKey(str2)) {
                                this.mAdIdMap.remove(str2);
                            }
                            i++;
                        }
                    }
                }
                reply.writeNoException();
                return true;
            } else if (code == 1016) {
                Slog.d(TAG, "code == CODE_GET_AD_KEY_LIST");
                data.enforceInterface(DESCRIPTOR_HW_AD_CLEANER);
                try {
                    appName = data.readString();
                    if (appName == null || !this.mAdViewMap.containsKey(appName)) {
                        reply.writeStringList(new ArrayList());
                        stats = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("appName = ");
                        stringBuilder5.append(appName);
                        stringBuilder5.append("  is not in the mAdViewMap! reply none");
                        Slog.d(stats, stringBuilder5.toString());
                    } else {
                        reply.writeStringList((List) this.mAdViewMap.get(appName));
                        stats = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("appName = ");
                        stringBuilder5.append(appName);
                        stringBuilder5.append("  is in the mAdViewMap!");
                        Slog.d(stats, stringBuilder5.toString());
                    }
                    if (appName == null || !this.mAdIdMap.containsKey(appName)) {
                        reply.writeStringList(new ArrayList());
                        stats = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("appName = ");
                        stringBuilder5.append(appName);
                        stringBuilder5.append("  is not in the mAdIdMap! reply none");
                        Slog.d(stats, stringBuilder5.toString());
                    } else {
                        reply.writeStringList((List) this.mAdIdMap.get(appName));
                        stats = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("appName = ");
                        stringBuilder5.append(appName);
                        stringBuilder5.append("  is in the mAdIdMap !");
                        Slog.d(stats, stringBuilder5.toString());
                    }
                } catch (Exception e) {
                    Slog.d(TAG, "---------err: Exception ");
                }
                reply.writeNoException();
                return true;
            } else if (code == 1019) {
                Slog.d(TAG, "code == CODE_AD_DEBUG");
                data.enforceInterface(DESCRIPTOR_ADCLEANER_MANAGER_Ex);
                this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.AD_APKDL_STRATEGY", "permission denied");
                data.readInt();
                size = 0;
                StringBuffer print = new StringBuffer();
                if (this.mAdViewMap.isEmpty()) {
                    print.append("mAdViewMap is empty!");
                } else {
                    print.append("\n---------------- mAdViewMap is as followed ---------------\n");
                    Set<String> keysSet = this.mAdViewMap.keySet();
                    List<String> keysList = new ArrayList();
                    for (String keyString : keysSet) {
                        keysList.add(keyString);
                    }
                    for (flag = 0; flag < this.mAdViewMap.size(); flag++) {
                        str3 = (String) keysList.get(flag);
                        List<String> value3 = (List) this.mAdViewMap.get(str3);
                        StringBuilder stringBuilder7 = new StringBuilder();
                        stringBuilder7.append("\n(");
                        stringBuilder7.append(flag);
                        stringBuilder7.append(") apkName = ");
                        stringBuilder7.append(str3);
                        stringBuilder7.append("\n");
                        print.append(stringBuilder7.toString());
                        for (size = 
/*
Method generation error in method: com.android.server.HwNetworkManagementService.onTransact(int, android.os.Parcel, android.os.Parcel, int):boolean, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r3_29 'size' int) = (r3_28 'size' int), (r3_32 'size' int) binds: {(r3_28 'size' int)=B:189:0x0388, (r3_32 'size' int)=B:101:0x03ef} in method: com.android.server.HwNetworkManagementService.onTransact(int, android.os.Parcel, android.os.Parcel, int):boolean, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:191)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:130)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:130)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.connectElseIf(RegionGen.java:145)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:126)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 53 more

*/

    private boolean executeHsmCommand(Parcel data, Parcel reply) {
        try {
            String cmd = data.readString();
            Object[] args = data.readArray(null);
            if (this.mConnector != null) {
                reply.writeInt(this.mConnector.execute(cmd, args).isClassOk());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setConnector(NativeDaemonConnector connector) {
        this.mConnector = connector;
    }

    private String getChannel(WifiConfiguration wifiConfig) {
        if (wifiConfig.apBand == 0 && SystemProperties.getBoolean(ISM_COEX_ON, false)) {
            this.mChannel = 11;
        } else {
            this.mChannel = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_channel", 0);
            if (this.mChannel == 0 || ((wifiConfig.apBand == 0 && this.mChannel > 14) || (wifiConfig.apBand == 1 && this.mChannel < 34))) {
                this.mChannel = wifiConfig.apChannel;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("channel=");
        stringBuilder.append(this.mChannel);
        Slog.d(str, stringBuilder.toString());
        return String.valueOf(this.mChannel);
    }

    private String getMaxscb() {
        int maxscb = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_maxscb", 8);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("maxscb=");
        stringBuilder.append(maxscb);
        Slog.d(str, stringBuilder.toString());
        return String.valueOf(maxscb);
    }

    private static String getSecurityType(WifiConfiguration wifiConfig) {
        int authType = wifiConfig.getAuthType();
        if (authType == 1) {
            return "wpa-psk";
        }
        if (authType != 4) {
            return "open";
        }
        return "wpa2-psk";
    }

    public String getIgnorebroadcastssid() {
        String iIgnorebroadcastssidStr = "broadcast";
        if (1 != Systemex.getInt(this.mContext.getContentResolver(), "show_broadcast_ssid_config", 0)) {
            return iIgnorebroadcastssidStr;
        }
        iIgnorebroadcastssidStr = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_ignorebroadcastssid", 0) == 0 ? "broadcast" : "hidden";
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("iIgnorebroadcastssidStr=");
        stringBuilder.append(iIgnorebroadcastssidStr);
        Slog.d(str, stringBuilder.toString());
        return iIgnorebroadcastssidStr;
    }

    public void startAccessPointWithChannel(WifiConfiguration wifiConfig, String wlanIface) {
        if (wifiConfig != null) {
            try {
                this.mConnector.execute("softap", new Object[]{ARG_SET, wlanIface, wifiConfig.SSID, getIgnorebroadcastssid(), getChannel(wifiConfig), getSecurityType(wifiConfig), new SensitiveArg(wifiConfig.preSharedKey), getMaxscb()});
                this.mConnector.execute("softap", new Object[]{"startap"});
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public void sendDataSpeedSlowMessage(String[] cooked, String raw) {
        if (cooked.length < 2 || !cooked[1].equals("sourceAddress")) {
            String msg1 = String.format("Invalid event from daemon (%s)", new Object[]{raw});
            Slog.d(TAG, "receive DataSpeedSlowDetected,return error 1");
            throw new IllegalStateException(msg1);
        }
        int sourceAddress = Integer.parseInt(cooked[2]);
        NetworkInfo mobileNetinfo = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getNetworkInfo(0);
        Slog.d(TAG, "onEvent receive DataSpeedSlowDetected");
        if (mobileNetinfo != null && mobileNetinfo.isConnected()) {
            Slog.d(TAG, "onEvent receive DataSpeedSlowDetected,mobile network is connected!");
            Intent chrIntent = new Intent("com.android.intent.action.data_speed_slow");
            chrIntent.putExtra("sourceAddress", sourceAddress);
            this.mContext.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
        }
    }

    public void sendDSCPChangeMessage(String[] cooked, String raw) {
        if (cooked.length < 4 || !cooked[1].equals("DSCPINFO")) {
            String msg1 = String.format("Invalid event from daemon (%s)", new Object[]{raw});
            Slog.d(TAG, "receive sendDSCPChangeMessage,return error 1");
            throw new IllegalStateException(msg1);
        }
        NetworkInfo networkInfoWlan = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getNetworkInfo(1);
        if (networkInfoWlan != null && networkInfoWlan.isConnected()) {
            Intent chrIntent = new Intent("com.android.intent.action.wifi_dscp_change");
            chrIntent.putExtra("dscpvalue", Integer.parseInt(cooked[2]));
            chrIntent.putExtra("uid", Integer.parseInt(cooked[3]));
            this.mContext.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
        }
    }

    public void sendWebStatMessage(String[] cooked, String raw) {
        if (cooked.length < 20 || !cooked[1].equals("ReportType")) {
            throw new IllegalStateException(String.format("Invalid event from daemon (%s)", new Object[]{raw}));
        }
        try {
            NetworkInfo mobileNetinfo = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getNetworkInfo(0);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onEvent receive Web Stat Report:");
            stringBuilder.append(raw);
            Slog.d(str, stringBuilder.toString());
            if (mobileNetinfo != null && mobileNetinfo.isConnected()) {
                Intent chrIntent = new Intent("com.android.intent.action.web_stat_report");
                chrIntent.putExtra("ReportType", Integer.parseInt(cooked[2]));
                chrIntent.putExtra("RTT", Integer.parseInt(cooked[3]));
                chrIntent.putExtra("WebDelay", Integer.parseInt(cooked[4]));
                chrIntent.putExtra("SuccNum", Integer.parseInt(cooked[5]));
                chrIntent.putExtra("FailNum", Integer.parseInt(cooked[6]));
                chrIntent.putExtra("NoAckNum", Integer.parseInt(cooked[7]));
                chrIntent.putExtra("TotalNum", Integer.parseInt(cooked[8]));
                chrIntent.putExtra("TcpTotalNum", Integer.parseInt(cooked[9]));
                chrIntent.putExtra("DelayL1", Integer.parseInt(cooked[10]));
                chrIntent.putExtra("DelayL2", Integer.parseInt(cooked[11]));
                chrIntent.putExtra("DelayL3", Integer.parseInt(cooked[12]));
                chrIntent.putExtra("DelayL4", Integer.parseInt(cooked[13]));
                chrIntent.putExtra("DelayL5", Integer.parseInt(cooked[14]));
                chrIntent.putExtra("DelayL6", Integer.parseInt(cooked[15]));
                chrIntent.putExtra("RTTL1", Integer.parseInt(cooked[16]));
                chrIntent.putExtra("RTTL2", Integer.parseInt(cooked[17]));
                chrIntent.putExtra("RTTL3", Integer.parseInt(cooked[18]));
                chrIntent.putExtra("RTTL4", Integer.parseInt(cooked[19]));
                chrIntent.putExtra("RTTL5", Integer.parseInt(cooked[20]));
                chrIntent.putExtra("TcpSuccNum", Integer.parseInt(cooked[21]));
                chrIntent.putExtra("SocketUid", Integer.parseInt(cooked[22]));
                chrIntent.putExtra("WebFailCode", Integer.parseInt(cooked[23]));
                chrIntent.putExtra("App1RTT", Integer.parseInt(cooked[24]));
                chrIntent.putExtra("App1WebDelay", Integer.parseInt(cooked[25]));
                chrIntent.putExtra("App1SuccNum", Integer.parseInt(cooked[26]));
                chrIntent.putExtra("App1FailNum", Integer.parseInt(cooked[27]));
                chrIntent.putExtra("App1NoAckNum", Integer.parseInt(cooked[28]));
                chrIntent.putExtra("App1TotalNum", Integer.parseInt(cooked[29]));
                chrIntent.putExtra("App1TcpTotalNum", Integer.parseInt(cooked[30]));
                chrIntent.putExtra("App1TcpSuccNum", Integer.parseInt(cooked[31]));
                chrIntent.putExtra("App1DelayL1", Integer.parseInt(cooked[32]));
                chrIntent.putExtra("App1DelayL2", Integer.parseInt(cooked[33]));
                chrIntent.putExtra("App1DelayL3", Integer.parseInt(cooked[34]));
                chrIntent.putExtra("App1DelayL4", Integer.parseInt(cooked[35]));
                chrIntent.putExtra("App1DelayL5", Integer.parseInt(cooked[36]));
                chrIntent.putExtra("App1DelayL6", Integer.parseInt(cooked[37]));
                chrIntent.putExtra("App1RTTL1", Integer.parseInt(cooked[38]));
                chrIntent.putExtra("App1RTTL2", Integer.parseInt(cooked[39]));
                chrIntent.putExtra("App1RTTL3", Integer.parseInt(cooked[40]));
                chrIntent.putExtra("App1RTTL4", Integer.parseInt(cooked[41]));
                chrIntent.putExtra("App1RTTL5", Integer.parseInt(cooked[42]));
                chrIntent.putExtra("App2RTT", Integer.parseInt(cooked[43]));
                chrIntent.putExtra("App2WebDelay", Integer.parseInt(cooked[44]));
                chrIntent.putExtra("App2SuccNum", Integer.parseInt(cooked[45]));
                chrIntent.putExtra("App2FailNum", Integer.parseInt(cooked[46]));
                chrIntent.putExtra("App2NoAckNum", Integer.parseInt(cooked[47]));
                chrIntent.putExtra("App2TotalNum", Integer.parseInt(cooked[48]));
                chrIntent.putExtra("App2TcpTotalNum", Integer.parseInt(cooked[49]));
                chrIntent.putExtra("App2TcpSuccNum", Integer.parseInt(cooked[50]));
                chrIntent.putExtra("App2DelayL1", Integer.parseInt(cooked[51]));
                chrIntent.putExtra("App2DelayL2", Integer.parseInt(cooked[52]));
                chrIntent.putExtra("App2DelayL3", Integer.parseInt(cooked[53]));
                chrIntent.putExtra("App2DelayL4", Integer.parseInt(cooked[54]));
                chrIntent.putExtra("App2DelayL5", Integer.parseInt(cooked[55]));
                chrIntent.putExtra("App2DelayL6", Integer.parseInt(cooked[56]));
                chrIntent.putExtra("App2RTTL1", Integer.parseInt(cooked[57]));
                chrIntent.putExtra("App2RTTL2", Integer.parseInt(cooked[58]));
                chrIntent.putExtra("App2RTTL3", Integer.parseInt(cooked[59]));
                chrIntent.putExtra("App2RTTL4", Integer.parseInt(cooked[60]));
                chrIntent.putExtra("App2RTTL5", Integer.parseInt(cooked[61]));
                chrIntent.putExtra("App3RTT", Integer.parseInt(cooked[62]));
                chrIntent.putExtra("App3WebDelay", Integer.parseInt(cooked[63]));
                chrIntent.putExtra("App3SuccNum", Integer.parseInt(cooked[64]));
                chrIntent.putExtra("App3FailNum", Integer.parseInt(cooked[65]));
                chrIntent.putExtra("App3NoAckNum", Integer.parseInt(cooked[66]));
                chrIntent.putExtra("App3TotalNum", Integer.parseInt(cooked[67]));
                chrIntent.putExtra("App3TcpTotalNum", Integer.parseInt(cooked[68]));
                chrIntent.putExtra("App3TcpSuccNum", Integer.parseInt(cooked[69]));
                chrIntent.putExtra("App3DelayL1", Integer.parseInt(cooked[70]));
                chrIntent.putExtra("App3DelayL2", Integer.parseInt(cooked[71]));
                chrIntent.putExtra("App3DelayL3", Integer.parseInt(cooked[72]));
                chrIntent.putExtra("App3DelayL4", Integer.parseInt(cooked[73]));
                chrIntent.putExtra("App3DelayL5", Integer.parseInt(cooked[74]));
                chrIntent.putExtra("App3DelayL6", Integer.parseInt(cooked[75]));
                chrIntent.putExtra("App3RTTL1", Integer.parseInt(cooked[76]));
                chrIntent.putExtra("App3RTTL2", Integer.parseInt(cooked[77]));
                chrIntent.putExtra("App3RTTL3", Integer.parseInt(cooked[78]));
                chrIntent.putExtra("App3RTTL4", Integer.parseInt(cooked[79]));
                chrIntent.putExtra("App3RTTL5", Integer.parseInt(cooked[80]));
                chrIntent.putExtra("App4RTT", Integer.parseInt(cooked[81]));
                chrIntent.putExtra("App4WebDelay", Integer.parseInt(cooked[82]));
                chrIntent.putExtra("App4SuccNum", Integer.parseInt(cooked[83]));
                chrIntent.putExtra("App4FailNum", Integer.parseInt(cooked[84]));
                chrIntent.putExtra("App4NoAckNum", Integer.parseInt(cooked[85]));
                chrIntent.putExtra("App4TotalNum", Integer.parseInt(cooked[86]));
                chrIntent.putExtra("App4TcpTotalNum", Integer.parseInt(cooked[87]));
                chrIntent.putExtra("App4TcpSuccNum", Integer.parseInt(cooked[88]));
                chrIntent.putExtra("App4DelayL1", Integer.parseInt(cooked[89]));
                chrIntent.putExtra("App4DelayL2", Integer.parseInt(cooked[90]));
                chrIntent.putExtra("App4DelayL3", Integer.parseInt(cooked[91]));
                chrIntent.putExtra("App4DelayL4", Integer.parseInt(cooked[92]));
                chrIntent.putExtra("App4DelayL5", Integer.parseInt(cooked[93]));
                chrIntent.putExtra("App4DelayL6", Integer.parseInt(cooked[94]));
                chrIntent.putExtra("App4RTTL1", Integer.parseInt(cooked[95]));
                chrIntent.putExtra("App4RTTL2", Integer.parseInt(cooked[96]));
                chrIntent.putExtra("App4RTTL3", Integer.parseInt(cooked[97]));
                chrIntent.putExtra("App4RTTL4", Integer.parseInt(cooked[98]));
                chrIntent.putExtra("App4RTTL5", Integer.parseInt(cooked[99]));
                chrIntent.putExtra("App5RTT", Integer.parseInt(cooked[100]));
                chrIntent.putExtra("App5WebDelay", Integer.parseInt(cooked[101]));
                chrIntent.putExtra("App5SuccNum", Integer.parseInt(cooked[102]));
                chrIntent.putExtra("App5FailNum", Integer.parseInt(cooked[103]));
                chrIntent.putExtra("App5NoAckNum", Integer.parseInt(cooked[104]));
                chrIntent.putExtra("App5TotalNum", Integer.parseInt(cooked[105]));
                chrIntent.putExtra("App5TcpTotalNum", Integer.parseInt(cooked[106]));
                chrIntent.putExtra("App5TcpSuccNum", Integer.parseInt(cooked[107]));
                chrIntent.putExtra("App5DelayL1", Integer.parseInt(cooked[HwAPPQoEUtils.MSG_APP_STATE_UNKNOW]));
                chrIntent.putExtra("App5DelayL2", Integer.parseInt(cooked[109]));
                chrIntent.putExtra("App5DelayL3", Integer.parseInt(cooked[110]));
                chrIntent.putExtra("App5DelayL4", Integer.parseInt(cooked[HwArbitrationDEFS.MSG_INSTANT_PAY_APP_START]));
                chrIntent.putExtra("App5DelayL5", Integer.parseInt(cooked[HwArbitrationDEFS.MSG_INSTANT_PAY_APP_END]));
                chrIntent.putExtra("App5DelayL6", Integer.parseInt(cooked[113]));
                chrIntent.putExtra("App5RTTL1", Integer.parseInt(cooked[114]));
                chrIntent.putExtra("App5RTTL2", Integer.parseInt(cooked[115]));
                chrIntent.putExtra("App5RTTL3", Integer.parseInt(cooked[116]));
                chrIntent.putExtra("App5RTTL4", Integer.parseInt(cooked[CPUFeature.MSG_RESET_TOP_APP_CPUSET]));
                chrIntent.putExtra("App5RTTL5", Integer.parseInt(cooked[CPUFeature.MSG_UNIPERF_BOOST_ON]));
                chrIntent.putExtra("App6RTT", Integer.parseInt(cooked[CPUFeature.MSG_UNIPERF_BOOST_OFF]));
                chrIntent.putExtra("App6WebDelay", Integer.parseInt(cooked[120]));
                chrIntent.putExtra("App6SuccNum", Integer.parseInt(cooked[121]));
                chrIntent.putExtra("App6FailNum", Integer.parseInt(cooked[122]));
                chrIntent.putExtra("App6NoAckNum", Integer.parseInt(cooked[123]));
                chrIntent.putExtra("App6TotalNum", Integer.parseInt(cooked[124]));
                chrIntent.putExtra("App6TcpTotalNum", Integer.parseInt(cooked[CPUFeature.MSG_SET_CPUSETCONFIG_VR]));
                chrIntent.putExtra("App6TcpSuccNum", Integer.parseInt(cooked[CPUFeature.MSG_SET_CPUSETCONFIG_SCREENON]));
                chrIntent.putExtra("App6DelayL1", Integer.parseInt(cooked[127]));
                chrIntent.putExtra("App6DelayL2", Integer.parseInt(cooked[128]));
                chrIntent.putExtra("App6DelayL3", Integer.parseInt(cooked[129]));
                chrIntent.putExtra("App6DelayL4", Integer.parseInt(cooked[130]));
                chrIntent.putExtra("App6DelayL5", Integer.parseInt(cooked[131]));
                chrIntent.putExtra("App6DelayL6", Integer.parseInt(cooked[132]));
                chrIntent.putExtra("App6RTTL1", Integer.parseInt(cooked[133]));
                chrIntent.putExtra("App6RTTL2", Integer.parseInt(cooked[134]));
                chrIntent.putExtra("App6RTTL3", Integer.parseInt(cooked[CPUFeature.MSG_SET_BOOST_CPUS]));
                chrIntent.putExtra("App6RTTL4", Integer.parseInt(cooked[CPUFeature.MSG_RESET_BOOST_CPUS]));
                chrIntent.putExtra("App6RTTL5", Integer.parseInt(cooked[CPUFeature.MSG_SET_LIMIT_CGROUP]));
                chrIntent.putExtra("App7RTT", Integer.parseInt(cooked[138]));
                chrIntent.putExtra("App7WebDelay", Integer.parseInt(cooked[CPUFeature.MSG_SET_FG_CGROUP]));
                chrIntent.putExtra("App7SuccNum", Integer.parseInt(cooked[140]));
                chrIntent.putExtra("App7FailNum", Integer.parseInt(cooked[141]));
                chrIntent.putExtra("App7NoAckNum", Integer.parseInt(cooked[CPUFeature.MSG_SET_BG_UIDS]));
                chrIntent.putExtra("App7TotalNum", Integer.parseInt(cooked[CPUFeature.MSG_SET_VIP_THREAD]));
                chrIntent.putExtra("App7TcpTotalNum", Integer.parseInt(cooked[CPUFeature.MSG_RESET_VIP_THREAD]));
                chrIntent.putExtra("App7TcpSuccNum", Integer.parseInt(cooked[CPUFeature.MSG_ENABLE_EAS]));
                chrIntent.putExtra("App7DelayL1", Integer.parseInt(cooked[CPUFeature.MSG_SET_THREAD_TO_TA]));
                chrIntent.putExtra("App7DelayL2", Integer.parseInt(cooked[CPUFeature.MSG_ENTER_GAME_SCENE]));
                chrIntent.putExtra("App7DelayL3", Integer.parseInt(cooked[CPUFeature.MSG_EXIT_GAME_SCENE]));
                chrIntent.putExtra("App7DelayL4", Integer.parseInt(cooked[149]));
                chrIntent.putExtra("App7DelayL5", Integer.parseInt(cooked[150]));
                chrIntent.putExtra("App7DelayL6", Integer.parseInt(cooked[CPUFeature.MSG_BINDER_THREAD_CREATE]));
                chrIntent.putExtra("App7RTTL1", Integer.parseInt(cooked[152]));
                chrIntent.putExtra("App7RTTL2", Integer.parseInt(cooked[153]));
                chrIntent.putExtra("App7RTTL3", Integer.parseInt(cooked[CPUFeature.MSG_RESET_ON_FIRE]));
                chrIntent.putExtra("App7RTTL4", Integer.parseInt(cooked[155]));
                chrIntent.putExtra("App7RTTL5", Integer.parseInt(cooked[156]));
                chrIntent.putExtra("App8RTT", Integer.parseInt(cooked[CPUFeature.MSG_GAME_SCENE_LEVEL]));
                chrIntent.putExtra("App8WebDelay", Integer.parseInt(cooked[158]));
                chrIntent.putExtra("App8SuccNum", Integer.parseInt(cooked[159]));
                chrIntent.putExtra("App8FailNum", Integer.parseInt(cooked[160]));
                chrIntent.putExtra("App8NoAckNum", Integer.parseInt(cooked[161]));
                chrIntent.putExtra("App8TotalNum", Integer.parseInt(cooked[162]));
                chrIntent.putExtra("App8TcpTotalNum", Integer.parseInt(cooked[163]));
                chrIntent.putExtra("App8TcpSuccNum", Integer.parseInt(cooked[164]));
                chrIntent.putExtra("App8DelayL1", Integer.parseInt(cooked[165]));
                chrIntent.putExtra("App8DelayL2", Integer.parseInt(cooked[166]));
                chrIntent.putExtra("App8DelayL3", Integer.parseInt(cooked[167]));
                chrIntent.putExtra("App8DelayL4", Integer.parseInt(cooked[168]));
                chrIntent.putExtra("App8DelayL5", Integer.parseInt(cooked[HwSecDiagnoseConstant.OEMINFO_ID_ANTIMAL]));
                chrIntent.putExtra("App8DelayL6", Integer.parseInt(cooked[HwSecDiagnoseConstant.OEMINFO_ID_DEVICE_RENEW]));
                chrIntent.putExtra("App8RTTL1", Integer.parseInt(cooked[HwSecDiagnoseConstant.OEMINFO_ID_ROOT_CHECK]));
                chrIntent.putExtra("App8RTTL2", Integer.parseInt(cooked[172]));
                chrIntent.putExtra("App8RTTL3", Integer.parseInt(cooked[173]));
                chrIntent.putExtra("App8RTTL4", Integer.parseInt(cooked[174]));
                chrIntent.putExtra("App8RTTL5", Integer.parseInt(cooked[175]));
                chrIntent.putExtra("App9RTT", Integer.parseInt(cooked[176]));
                chrIntent.putExtra("App9WebDelay", Integer.parseInt(cooked[177]));
                chrIntent.putExtra("App9SuccNum", Integer.parseInt(cooked[178]));
                chrIntent.putExtra("App9FailNum", Integer.parseInt(cooked[179]));
                chrIntent.putExtra("App9NoAckNum", Integer.parseInt(cooked[GestureNavConst.GESTURE_GO_HOME_MIN_DISTANCE_THRESHOLD]));
                chrIntent.putExtra("App9TotalNum", Integer.parseInt(cooked[181]));
                chrIntent.putExtra("App9TcpTotalNum", Integer.parseInt(cooked[182]));
                chrIntent.putExtra("App9TcpSuccNum", Integer.parseInt(cooked[183]));
                chrIntent.putExtra("App9DelayL1", Integer.parseInt(cooked[184]));
                chrIntent.putExtra("App9DelayL2", Integer.parseInt(cooked[185]));
                chrIntent.putExtra("App9DelayL3", Integer.parseInt(cooked[186]));
                chrIntent.putExtra("App9DelayL4", Integer.parseInt(cooked[187]));
                chrIntent.putExtra("App9DelayL5", Integer.parseInt(cooked[188]));
                chrIntent.putExtra("App9DelayL6", Integer.parseInt(cooked[189]));
                chrIntent.putExtra("App9RTTL1", Integer.parseInt(cooked[190]));
                chrIntent.putExtra("App9RTTL2", Integer.parseInt(cooked[Utils.DEFAULT_COLOR_TEMPERATURE_SUNNY]));
                chrIntent.putExtra("App9RTTL3", Integer.parseInt(cooked[192]));
                chrIntent.putExtra("App9RTTL4", Integer.parseInt(cooked[193]));
                chrIntent.putExtra("App9RTTL5", Integer.parseInt(cooked[194]));
                chrIntent.putExtra("App10RTT", Integer.parseInt(cooked[195]));
                chrIntent.putExtra("App10WebDelay", Integer.parseInt(cooked[196]));
                chrIntent.putExtra("App10SuccNum", Integer.parseInt(cooked[197]));
                chrIntent.putExtra("App10FailNum", Integer.parseInt(cooked[198]));
                chrIntent.putExtra("App10NoAckNum", Integer.parseInt(cooked[199]));
                chrIntent.putExtra("App10TotalNum", Integer.parseInt(cooked[200]));
                chrIntent.putExtra("App10TcpTotalNum", Integer.parseInt(cooked[201]));
                chrIntent.putExtra("App10TcpSuccNum", Integer.parseInt(cooked[202]));
                chrIntent.putExtra("App10DelayL1", Integer.parseInt(cooked[203]));
                chrIntent.putExtra("App10DelayL2", Integer.parseInt(cooked[204]));
                chrIntent.putExtra("App10DelayL3", Integer.parseInt(cooked[205]));
                chrIntent.putExtra("App10DelayL4", Integer.parseInt(cooked[206]));
                chrIntent.putExtra("App10DelayL5", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_DISCONNETED]));
                chrIntent.putExtra("App10DelayL6", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED]));
                chrIntent.putExtra("App10RTTL1", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_WIFIPRO_SWITCH_ENABLE]));
                chrIntent.putExtra("App10RTTL2", Integer.parseInt(cooked[210]));
                chrIntent.putExtra("App10RTTL3", Integer.parseInt(cooked[211]));
                chrIntent.putExtra("App10RTTL4", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE]));
                chrIntent.putExtra("App10RTTL5", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED]));
                chrIntent.putExtra("HighestTcpRTT", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED]));
                chrIntent.putExtra("LowestTcpRTT", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED]));
                chrIntent.putExtra("LastTcpRTT", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED]));
                chrIntent.putExtra("HighestWebDelay", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_AIDEVICE_MPLINK_OPEN]));
                chrIntent.putExtra("LowestWebDelay", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_AIDEVICE_MPLINK_CLOSE]));
                chrIntent.putExtra("LastWebDelay", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN]));
                chrIntent.putExtra("ServerAddr", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE]));
                chrIntent.putExtra("RTTAbnServerAddr", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE]));
                chrIntent.putExtra("VideoAvgSpeed", Integer.parseInt(cooked[222]));
                chrIntent.putExtra("VideoFreezNum", Integer.parseInt(cooked[223]));
                chrIntent.putExtra("VideoTime", Integer.parseInt(cooked[MetricsLoggerCompat.OVERVIEW_ACTIVITY]));
                chrIntent.putExtra("AccVideoAvgSpeed", Integer.parseInt(cooked[225]));
                chrIntent.putExtra("AccVideoFreezNum", Integer.parseInt(cooked[226]));
                chrIntent.putExtra("AccVideoTime", Integer.parseInt(cooked[227]));
                chrIntent.putExtra("tcp_handshake_delay", Integer.parseInt(cooked[228]));
                chrIntent.putExtra("http_get_delay", Integer.parseInt(cooked[229]));
                chrIntent.putExtra("http_send_get_num", Integer.parseInt(cooked[HwMpLinkServiceImpl.MPLINK_MSG_DATA_SUB_CHANGE]));
                this.mContext.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
            }
            if (Integer.parseInt(cooked[234]) > 0) {
                Intent wifichrIntent = new Intent(INTENT_DS_WIFI_WEB_STAT_REPORT);
                wifichrIntent.putExtra("ReportType", Integer.parseInt(cooked[232]));
                wifichrIntent.putExtra("RTT", Integer.parseInt(cooked[233]));
                wifichrIntent.putExtra("WebDelay", Integer.parseInt(cooked[234]));
                wifichrIntent.putExtra("SuccNum", Integer.parseInt(cooked[235]));
                wifichrIntent.putExtra("FailNum", Integer.parseInt(cooked[236]));
                wifichrIntent.putExtra("NoAckNum", Integer.parseInt(cooked[237]));
                wifichrIntent.putExtra("TotalNum", Integer.parseInt(cooked[238]));
                wifichrIntent.putExtra("TcpTotalNum", Integer.parseInt(cooked[239]));
                wifichrIntent.putExtra("DelayL1", Integer.parseInt(cooked[240]));
                wifichrIntent.putExtra("DelayL2", Integer.parseInt(cooked[241]));
                wifichrIntent.putExtra("DelayL3", Integer.parseInt(cooked[242]));
                wifichrIntent.putExtra("DelayL4", Integer.parseInt(cooked[243]));
                wifichrIntent.putExtra("DelayL5", Integer.parseInt(cooked[244]));
                wifichrIntent.putExtra("DelayL6", Integer.parseInt(cooked[245]));
                wifichrIntent.putExtra("RTTL1", Integer.parseInt(cooked[246]));
                wifichrIntent.putExtra("RTTL2", Integer.parseInt(cooked[247]));
                wifichrIntent.putExtra("RTTL3", Integer.parseInt(cooked[248]));
                wifichrIntent.putExtra("RTTL4", Integer.parseInt(cooked[249]));
                wifichrIntent.putExtra("RTTL5", Integer.parseInt(cooked[GestureNavConst.GESTURE_MOVE_TIME_THRESHOLD_4]));
                wifichrIntent.putExtra("TcpSuccNum", Integer.parseInt(cooked[251]));
                wifichrIntent.putExtra("HighestTcpRTT", Integer.parseInt(cooked[444]));
                wifichrIntent.putExtra("LowestTcpRTT", Integer.parseInt(cooked[445]));
                wifichrIntent.putExtra("LastTcpRTT", Integer.parseInt(cooked[446]));
                wifichrIntent.putExtra("HighestWebDelay", Integer.parseInt(cooked[447]));
                wifichrIntent.putExtra("LowestWebDelay", Integer.parseInt(cooked[448]));
                wifichrIntent.putExtra("LastWebDelay", Integer.parseInt(cooked[449]));
                wifichrIntent.putExtra("ServerAddr", Integer.parseInt(cooked[450]));
                wifichrIntent.putExtra("RTTAbnServerAddr", Integer.parseInt(cooked[451]));
                wifichrIntent.putExtra("VideoAvgSpeed", Integer.parseInt(cooked[452]));
                wifichrIntent.putExtra("VideoFreezNum", Integer.parseInt(cooked[453]));
                wifichrIntent.putExtra("VideoTime", Integer.parseInt(cooked[454]));
                wifichrIntent.putExtra("AccVideoAvgSpeed", Integer.parseInt(cooked[455]));
                wifichrIntent.putExtra("AccVideoFreezNum", Integer.parseInt(cooked[456]));
                wifichrIntent.putExtra("AccVideoTime", Integer.parseInt(cooked[457]));
                wifichrIntent.putExtra("tcp_handshake_delay", Integer.parseInt(cooked[458]));
                wifichrIntent.putExtra("http_get_delay", Integer.parseInt(cooked[459]));
                this.mContext.sendBroadcast(wifichrIntent, CHR_BROADCAST_PERMISSION);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Web Stat Report Send Broadcast Fail.");
        }
    }

    public boolean handleApLinkedStaListChange(String raw, String[] cooked) {
        Slog.d(TAG, "handleApLinkedStaListChange is called");
        if (STA_JOIN_EVENT.equals(cooked[1]) || STA_LEAVE_EVENT.equals(cooked[1])) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Got sta list change event:");
            stringBuilder.append(cooked[1]);
            Slog.d(str, stringBuilder.toString());
            notifyApLinkedStaListChange(cooked[1], cooked[4]);
            return true;
        }
        throw new IllegalStateException(String.format("ApLinkedStaListChange: Invalid event from daemon (%s)", new Object[]{raw}));
    }

    public List<String> getApLinkedStaList() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        List<String> mDhcpList = getApLinkedDhcpList();
        Slog.d(TAG, "getApLinkedStaList: softap assoclist");
        List<String> infoList = new ArrayList();
        for (int index = 0; index < this.mMacList.size(); index++) {
            String mac = getApLinkedStaInfo((String) this.mMacList.get(index), mDhcpList);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getApLinkedStaList ApLinkedStaInfo = ");
            stringBuilder.append(mac);
            Slog.d(str, stringBuilder.toString());
            infoList.add(mac);
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getApLinkedStaList, info size=");
        stringBuilder2.append(infoList.size());
        Slog.d(str2, stringBuilder2.toString());
        return infoList;
    }

    public void setSoftapMacFilter(String macFilter) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("softap setmacfilter ");
            stringBuilder.append(macFilter);
            String cmdStr = String.format(stringBuilder.toString(), new Object[0]);
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setSoftapMacFilter:");
            stringBuilder2.append(cmdStr);
            Slog.d(str, stringBuilder2.toString());
            this.mConnector.doCommand("softap", new Object[]{"setmacfilter", macFilter});
        } catch (NativeDaemonConnectorException e) {
            throw new IllegalStateException("Cannot communicate with native daemon to set MAC Filter");
        }
    }

    public void setSoftapDisassociateSta(String mac) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("softap disassociatesta ");
            stringBuilder.append(mac);
            String cmdStr = String.format(stringBuilder.toString(), new Object[0]);
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setSoftapDisassociateSta:");
            stringBuilder2.append(cmdStr);
            Slog.d(str, stringBuilder2.toString());
            this.mConnector.doCommand("softap", new Object[]{"disassociatesta", mac});
        } catch (NativeDaemonConnectorException e) {
            throw new IllegalStateException("Cannot communicate with native daemon to disassociate a station");
        }
    }

    public void setAccessPointHw(String wlanIface, String softapIface) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_NETWORK_STATE", "NetworkManagementService");
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", "NetworkManagementService");
        HuaweiApConfiguration hwApConfig = new HuaweiApConfiguration();
        hwApConfig.channel = this.mChannel;
        hwApConfig.maxScb = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_maxscb", 8);
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("softap sethw ");
            stringBuilder.append(wlanIface);
            stringBuilder.append(" ");
            stringBuilder.append(softapIface);
            stringBuilder.append(" %d %d");
            String str = String.format(stringBuilder.toString(), new Object[]{Integer.valueOf(hwApConfig.channel), Integer.valueOf(hwApConfig.maxScb)});
            this.mConnector.doCommand("softap", new Object[]{"sethw", wlanIface, softapIface, String.valueOf(hwApConfig.channel), String.valueOf(hwApConfig.maxScb)});
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setAccessPointHw command: ");
            stringBuilder2.append(str);
            Slog.d(str2, stringBuilder2.toString());
        } catch (NativeDaemonConnectorException e) {
            throw new IllegalStateException("Error communicating to native daemon to set soft AP", e);
        }
    }

    private List<String> getApLinkedDhcpList() {
        try {
            Slog.d(TAG, "getApLinkedDhcpList: softap getdhcplease");
            Object[] objArr = new Object[1];
            int i = 0;
            objArr[0] = "getdhcplease";
            String[] dhcpleaseList = this.mConnector.doListCommand("softap", 122, objArr);
            if (dhcpleaseList == null) {
                Slog.e(TAG, "getApLinkedDhcpList Error: doListCommand return NULL");
                return null;
            }
            List<String> mDhcpList = new ArrayList();
            int length = dhcpleaseList.length;
            while (i < length) {
                String dhcplease = dhcpleaseList[i];
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getApLinkedDhcpList dhcpList = ");
                stringBuilder.append(dhcplease);
                Slog.d(str, stringBuilder.toString());
                mDhcpList.add(dhcplease);
                i++;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getApLinkedDhcpList: mDhcpList size=");
            stringBuilder2.append(mDhcpList.size());
            Slog.d(str2, stringBuilder2.toString());
            return mDhcpList;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Cannot communicate with native daemon to get dhcp lease information");
            return null;
        }
    }

    private String getApLinkedStaInfo(String mac, List<String> mDhcpList) {
        String ApLinkedStaInfo = String.format("MAC=%s", new Object[]{mac});
        mac = mac.toLowerCase();
        if (mDhcpList != null) {
            for (String dhcplease : mDhcpList) {
                if (dhcplease.contains(mac)) {
                    if (4 <= dhcplease.split(" ").length) {
                        Slog.d(TAG, "getApLinkedStaInfo: dhcplease token");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(ApLinkedStaInfo);
                        stringBuilder.append(" IP=%s DEVICE=%s");
                        ApLinkedStaInfo = String.format(stringBuilder.toString(), new Object[]{Tokens[2], Tokens[3]});
                    }
                }
            }
        }
        return ApLinkedStaInfo;
    }

    private void notifyApLinkedStaListChange(String event, String macStr) {
        int macHashCode = 0;
        if (macStr != null) {
            macHashCode = macStr.hashCode();
        }
        Message msg = new Message();
        Bundle bundle = new Bundle();
        msg.what = macHashCode;
        bundle.putString(EVENT_KEY, event);
        bundle.putString(MAC_KEY, macStr);
        msg.setData(bundle);
        if (STA_JOIN_EVENT.equals(event)) {
            this.mApLinkedStaHandler.sendMessageDelayed(msg, 5000);
        } else if (STA_LEAVE_EVENT.equals(event)) {
            this.mApLinkedStaHandler.sendMessage(msg);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("send ");
        stringBuilder.append(event);
        stringBuilder.append(" message, mLinkedStaCount=");
        stringBuilder.append(this.mLinkedStaCount);
        Slog.d(str, stringBuilder.toString());
    }

    public void setWifiTxPower(String reduceCmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiTxPower ");
        stringBuilder.append(reduceCmd);
        Slog.d(str, stringBuilder.toString());
        try {
            this.mConnector.execute("softap", new Object[]{reduceCmd});
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private String getWiFiDnsStats(int netid) {
        StringBuffer buf = new StringBuffer();
        try {
            r4 = new Object[2];
            int i = 0;
            r4[0] = "getdnsstat";
            r4[1] = Integer.valueOf(netid);
            String[] stats = this.mConnector.doListCommand("resolver", 130, r4);
            if (stats != null) {
                while (true) {
                    int i2 = i;
                    if (i2 >= stats.length) {
                        break;
                    }
                    buf.append(stats[i2]);
                    if (i2 < stats.length - 1) {
                        buf.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    }
                    i = i2 + 1;
                }
            }
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Cannot communicate with native daemon to get wifi dns stats");
        }
        return buf.toString();
    }

    private String strToHexStr(String str) {
        if (str == null) {
            return null;
        }
        byte[] bytes = str.getBytes(Charset.forName("UTF-8"));
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append("0123456789ABCDEF".charAt((bytes[i] & 240) >> 4));
            sb.append("0123456789ABCDEF".charAt((bytes[i] & 15) >> 0));
        }
        return sb.toString();
    }

    private String hexStrToStr(String hexStr) {
        if (hexStr == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(hexStr.length() / 2);
        for (int i = 0; i < hexStr.length(); i += 2) {
            baos.write(("0123456789ABCDEF".indexOf(hexStr.charAt(i)) << 4) | "0123456789ABCDEF".indexOf(hexStr.charAt(i + 1)));
        }
        return new String(baos.toByteArray(), Charset.forName("UTF-8"));
    }

    private ArrayList<String> convertPkgNameToUid(String[] pkgName) {
        String[] strArr = pkgName;
        if (strArr != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("convertPkgNameToUid, pkgName=");
            stringBuilder.append(Arrays.asList(pkgName));
            Slog.d(str, stringBuilder.toString());
        }
        ArrayList<String> uidList = new ArrayList();
        if (strArr != null && strArr.length > 0) {
            int userCount = UserManager.get(this.mContext).getUserCount();
            List<UserInfo> users = UserManager.get(this.mContext).getUsers();
            PackageManager pm = this.mContext.getPackageManager();
            StringBuilder appUidBuilder = new StringBuilder();
            int length = strArr.length;
            int uidCount = 0;
            int uidCount2 = 0;
            while (uidCount2 < length) {
                String pkg = strArr[uidCount2];
                int uidCount3 = uidCount;
                StringBuilder appUidBuilder2 = appUidBuilder;
                appUidBuilder = null;
                while (true) {
                    int n = appUidBuilder;
                    if (n >= userCount) {
                        break;
                    }
                    int uid = -1;
                    try {
                        uid = pm.getPackageUidAsUser(pkg, ((UserInfo) users.get(n)).id);
                        appUidBuilder = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("convertPkgNameToUid, pkg=");
                        stringBuilder2.append(pkg);
                        stringBuilder2.append(", uid=");
                        stringBuilder2.append(uid);
                        stringBuilder2.append(", under user.id=");
                        stringBuilder2.append(((UserInfo) users.get(n)).id);
                        Slog.d(appUidBuilder, stringBuilder2.toString());
                        uidCount3++;
                        if (uidCount3 % 50 == null) {
                            appUidBuilder2.append(uid);
                            appUidBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                            uidList.add(appUidBuilder2.toString());
                            appUidBuilder2 = new StringBuilder();
                        } else {
                            appUidBuilder2.append(uid);
                            appUidBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "convertPkgNameToUid, skip unknown packages!");
                    }
                    appUidBuilder = n + 1;
                }
                uidCount2++;
                appUidBuilder = appUidBuilder2;
                uidCount = uidCount3;
            }
            if (!TextUtils.isEmpty(appUidBuilder.toString())) {
                uidList.add(appUidBuilder.toString());
            }
        }
        return uidList;
    }

    private void setAdFilterRules(String adStrategy, boolean needReset) {
        String adStrategy2 = adStrategy;
        boolean z = needReset;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAdFilterRules, adStrategy=");
        stringBuilder.append(adStrategy2);
        stringBuilder.append(", needReset=");
        stringBuilder.append(z);
        Slog.d(str, stringBuilder.toString());
        String operation = z ? "reset" : "not_reset";
        int count = 0;
        int count2 = 0;
        if (adStrategy2 != null) {
            count2 = adStrategy.length();
            count = count2 / PER_STRATEGY_SIZE;
            if (count2 % PER_STRATEGY_SIZE != 0) {
                count++;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setAdFilterRules, adStrategy len=");
            stringBuilder2.append(count2);
            stringBuilder2.append(", divided count=");
            stringBuilder2.append(count);
            Slog.d(str2, stringBuilder2.toString());
        }
        int strategyLen = count2;
        count2 = count;
        int cmdId = this.mCmdId.incrementAndGet();
        try {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("setAdFilterRules, count=");
            stringBuilder3.append(count2);
            stringBuilder3.append(", cmdId=");
            stringBuilder3.append(cmdId);
            Slog.d(str, stringBuilder3.toString());
            this.mConnector.execute("hwfilter", new Object[]{"set_ad_strategy_rule", operation, Integer.valueOf(cmdId), Integer.valueOf(count2)});
            if (strategyLen == 0) {
                Slog.d(TAG, "setAdFilterRules, adStrategy is null!");
                return;
            }
            count = 1;
            while (adStrategy2.length() > 0) {
                String adStrategyTmp;
                StringBuilder stringBuilder4;
                if (adStrategy2.length() > PER_STRATEGY_SIZE) {
                    adStrategyTmp = adStrategy2.substring(0, PER_STRATEGY_SIZE);
                    String str3 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("setAdFilterRules, adStrategy len=");
                    stringBuilder4.append(adStrategyTmp.length());
                    stringBuilder4.append(", seq=");
                    stringBuilder4.append(count);
                    stringBuilder4.append(", cmdId=");
                    stringBuilder4.append(cmdId);
                    Slog.d(str3, stringBuilder4.toString());
                    this.mConnector.execute("hwfilter", new Object[]{"set_ad_strategy_buf", Integer.valueOf(cmdId), Integer.valueOf(count), adStrategyTmp});
                    adStrategy2 = adStrategy2.substring(PER_STRATEGY_SIZE);
                    count++;
                } else {
                    adStrategyTmp = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("setAdFilterRules, adStrategy len=");
                    stringBuilder4.append(adStrategy2.length());
                    stringBuilder4.append(", seq=");
                    stringBuilder4.append(count);
                    stringBuilder4.append(", cmdId=");
                    stringBuilder4.append(cmdId);
                    Slog.d(adStrategyTmp, stringBuilder4.toString());
                    this.mConnector.execute("hwfilter", new Object[]{"set_ad_strategy_buf", Integer.valueOf(cmdId), Integer.valueOf(count), adStrategy2});
                    return;
                }
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void setApkDlFilterRules(ArrayList<String> appUidList, boolean needReset) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApkDlFilterRules, appUidList=");
        stringBuilder.append(appUidList);
        stringBuilder.append(", needReset=");
        stringBuilder.append(needReset);
        Slog.d(str, stringBuilder.toString());
        str = needReset ? "reset" : "not_reset";
        if (appUidList != null) {
            try {
                if (appUidList.size() > 0) {
                    for (int i = 0; i < appUidList.size(); i++) {
                        if (i == 0) {
                            this.mConnector.execute("hwfilter", new Object[]{"set_apkdl_strategy_rule", appUidList.get(i), str});
                        } else {
                            this.mConnector.execute("hwfilter", new Object[]{"set_apkdl_strategy_rule", appUidList.get(i), "not_reset"});
                        }
                    }
                    return;
                }
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        this.mConnector.execute("hwfilter", new Object[]{"set_apkdl_strategy_rule", null, str});
    }

    private void clearAdOrApkDlFilterRules(ArrayList<String> appUidList, boolean needReset, int strategy) {
        NativeDaemonConnectorException e;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clearApkDlFilterRules, appUidList=");
        stringBuilder.append(appUidList);
        stringBuilder.append(", needReset=");
        stringBuilder.append(needReset);
        stringBuilder.append(", strategy=");
        stringBuilder.append(strategy);
        Slog.d(str, stringBuilder.toString());
        str = needReset ? "reset" : "not_reset";
        if (strategy == 0) {
            if (appUidList != null) {
                try {
                    if (appUidList.size() > 0) {
                        for (int i = 0; i < appUidList.size(); i++) {
                            if (i == 0) {
                                this.mConnector.execute("hwfilter", new Object[]{"clear_ad_strategy_rule", appUidList.get(i), str});
                            } else {
                                this.mConnector.execute("hwfilter", new Object[]{"clear_ad_strategy_rule", appUidList.get(i), "not_reset"});
                            }
                        }
                        return;
                    }
                } catch (NativeDaemonConnectorException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
            this.mConnector.execute("hwfilter", new Object[]{"clear_ad_strategy_rule", null, str});
        } else if (1 == strategy) {
            if (appUidList == null || appUidList.size() <= 0) {
                this.mConnector.execute("hwfilter", new Object[]{"clear_apkdl_strategy_rule", null, str});
                return;
            }
            for (e2 = null; e2 < appUidList.size(); e2++) {
                if (e2 == null) {
                    this.mConnector.execute("hwfilter", new Object[]{"clear_apkdl_strategy_rule", appUidList.get(e2), str});
                } else {
                    this.mConnector.execute("hwfilter", new Object[]{"clear_apkdl_strategy_rule", appUidList.get(e2), "not_reset"});
                }
            }
        } else if (2 == strategy) {
            Slog.d(TAG, "clearApkDlFilterRules strategy is APK_CONTROL_STRATEGY");
            if (appUidList == null || appUidList.size() <= 0) {
                Slog.d(TAG, "clearApkDlFilterRules else netd clear_delta_install_rule");
                this.mConnector.execute("hwfilter", new Object[]{"clear_delta_install_rule", null, str});
                return;
            }
            e2 = appUidList.size();
            for (int i2 = 0; i2 < e2; i2++) {
                if (i2 == 0) {
                    Slog.d(TAG, "clearApkDlFilterRules 0==i netd clear_delta_install_rule");
                    this.mConnector.execute("hwfilter", new Object[]{"clear_delta_install_rule", appUidList.get(i2), str});
                } else {
                    Slog.d(TAG, "clearApkDlFilterRules 0!=i netd clear_delta_install_rule");
                    this.mConnector.execute("hwfilter", new Object[]{"clear_delta_install_rule", appUidList.get(i2), "not_reset"});
                }
            }
        }
    }

    private void printAdOrApkDlFilterRules(int strategy) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("printAdOrApkDlFilterRules, strategy=");
        stringBuilder.append(strategy);
        Slog.d(str, stringBuilder.toString());
        if (strategy == 0) {
            try {
                this.mConnector.execute("hwfilter", new Object[]{"output_ad_strategy_rule"});
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        } else if (1 == strategy) {
            this.mConnector.execute("hwfilter", new Object[]{"output_apkdl_strategy_rule"});
        } else if (2 == strategy) {
            this.mConnector.execute("hwfilter", new Object[]{"output_delta_install_rule"});
        }
    }

    private void setApkDlUrlUserResult(String downloadId, boolean result) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApkDlUrlUserResult, downloadId=");
        stringBuilder.append(downloadId);
        stringBuilder.append(", result=");
        stringBuilder.append(result);
        Slog.d(str, stringBuilder.toString());
        str = result ? SceneInfo.ITEM_RULE_ALLOW : "reject";
        try {
            this.mConnector.execute("hwfilter", new Object[]{"apkdl_callback", downloadId, str});
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:47:0x0136  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0136  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getStrategyStr(int code, int size, Parcel data) {
        int userCount;
        int i = code;
        int i2 = size;
        if (i2 <= 0) {
            return null;
        }
        int userCount2 = UserManager.get(this.mContext).getUserCount();
        List<UserInfo> users = UserManager.get(this.mContext).getUsers();
        PackageManager pm = this.mContext.getPackageManager();
        StringBuilder StrategyBuilder = new StringBuilder();
        int i3 = 0;
        while (i3 < i2) {
            String key = data.readString();
            ArrayList<String> value = data.createStringArrayList();
            int i4 = i3 + 1;
            boolean isEmpty = TextUtils.isEmpty(key);
            int i5 = CODE_SET_AD_STRATEGY_RULE;
            if (isEmpty || value == null) {
                userCount = userCount2;
            } else if (value.size() == 0) {
                userCount = userCount2;
            } else {
                StringBuilder tmpUrlBuilder = new StringBuilder();
                String tmpUrlStr = null;
                i3 = 0;
                while (true) {
                    int n = i3;
                    if (n >= userCount2) {
                        break;
                    }
                    int uid;
                    try {
                        int uid2 = pm.getPackageUidAsUser(key, ((UserInfo) users.get(n)).id);
                        String str;
                        if (i5 == i) {
                            StringBuilder stringBuilder;
                            try {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("CODE_SET_AD_STRATEGY_RULE, adStrategy pkgName=");
                                stringBuilder.append(key);
                                stringBuilder.append(", uid=");
                                uid = uid2;
                            } catch (Exception e) {
                                uid = uid2;
                                userCount = userCount2;
                                if (CODE_SET_AD_STRATEGY_RULE == i) {
                                    Slog.e(TAG, "CODE_SET_AD_STRATEGY_RULE, skip unknown packages!");
                                } else if (CODE_SET_APK_CONTROL_STRATEGY == i) {
                                    Slog.e(TAG, "CODE_SET_APK_CONTROL_STRATEGY, skip unknown packages!");
                                }
                                i3 = n + 1;
                                userCount2 = userCount;
                                i2 = size;
                                i5 = CODE_SET_AD_STRATEGY_RULE;
                            }
                            try {
                                stringBuilder.append(uid);
                                stringBuilder.append(", under user.id=");
                                stringBuilder.append(((UserInfo) users.get(n)).id);
                                Slog.d(str, stringBuilder.toString());
                            } catch (Exception e2) {
                                userCount = userCount2;
                                if (CODE_SET_AD_STRATEGY_RULE == i) {
                                }
                                i3 = n + 1;
                                userCount2 = userCount;
                                i2 = size;
                                i5 = CODE_SET_AD_STRATEGY_RULE;
                            }
                        } else {
                            uid = uid2;
                            if (CODE_SET_APK_CONTROL_STRATEGY == i) {
                                str = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("CODE_SET_APK_CONTROL_STRATEGY, apkStrategy pkgName=");
                                stringBuilder2.append(key);
                                stringBuilder2.append(", uid=");
                                stringBuilder2.append(uid);
                                stringBuilder2.append(", under user.id=");
                                stringBuilder2.append(((UserInfo) users.get(n)).id);
                                Slog.d(str, stringBuilder2.toString());
                            }
                        }
                        StrategyBuilder.append(uid);
                        StrategyBuilder.append(":");
                        if (tmpUrlStr == null) {
                            i3 = 0;
                            i2 = 0;
                            i5 = value.size();
                            while (i2 < i5) {
                                userCount = userCount2;
                                tmpUrlBuilder.append(strToHexStr((String) value.get(i2)));
                                i3++;
                                if (i3 < value.size()) {
                                    tmpUrlBuilder.append(",");
                                } else {
                                    tmpUrlBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                                }
                                i2++;
                                userCount2 = userCount;
                            }
                            userCount = userCount2;
                            tmpUrlStr = tmpUrlBuilder.toString();
                        } else {
                            userCount = userCount2;
                        }
                        StrategyBuilder.append(tmpUrlStr);
                    } catch (Exception e3) {
                        userCount = userCount2;
                        uid = -1;
                        if (CODE_SET_AD_STRATEGY_RULE == i) {
                        }
                        i3 = n + 1;
                        userCount2 = userCount;
                        i2 = size;
                        i5 = CODE_SET_AD_STRATEGY_RULE;
                    }
                    i3 = n + 1;
                    userCount2 = userCount;
                    i2 = size;
                    i5 = CODE_SET_AD_STRATEGY_RULE;
                }
                userCount = userCount2;
                i3 = i4;
                userCount2 = userCount;
                i2 = size;
            }
            if (CODE_SET_AD_STRATEGY_RULE == i) {
                Slog.e(TAG, "CODE_SET_AD_STRATEGY_RULE, skip empty key or value!");
            } else if (CODE_SET_APK_CONTROL_STRATEGY == i) {
                Slog.e(TAG, "CODE_SET_APK_CONTROL_STRATEGY, skip empty key or value!");
            }
            i3 = i4;
            userCount2 = userCount;
            i2 = size;
        }
        return StrategyBuilder.toString();
    }

    private void setApkControlFilterRules(String apkStrategy, boolean needReset) {
        String apkStrategy2 = apkStrategy;
        boolean z = needReset;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApkControlFilterRules, apkStrategy=");
        stringBuilder.append(apkStrategy2);
        stringBuilder.append(", needReset=");
        stringBuilder.append(z);
        Slog.d(str, stringBuilder.toString());
        String operation = z ? "reset" : "not_reset";
        int count = 0;
        int count2 = 0;
        if (apkStrategy2 != null) {
            count2 = apkStrategy.length();
            count = count2 / PER_STRATEGY_SIZE;
            if (count2 % PER_STRATEGY_SIZE != 0) {
                count++;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setApkControlFilterRules, apkStrategy len=");
            stringBuilder2.append(count2);
            stringBuilder2.append(", divided count=");
            stringBuilder2.append(count);
            Slog.d(str2, stringBuilder2.toString());
        }
        int strategyLen = count2;
        count2 = count;
        int cmdId = this.mCmdId.incrementAndGet();
        try {
            str = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("setApkControlFilterRules, count=");
            stringBuilder3.append(count2);
            stringBuilder3.append(", cmdId=");
            stringBuilder3.append(cmdId);
            Slog.d(str, stringBuilder3.toString());
            this.mConnector.execute("hwfilter", new Object[]{"set_delta_install_rule", operation, Integer.valueOf(cmdId), Integer.valueOf(count2)});
            if (strategyLen == 0) {
                Slog.d(TAG, "setApkControlFilterRules, apkStrategy is null!");
                return;
            }
            count = 1;
            while (apkStrategy2.length() > 0) {
                String apkStrategyTmp;
                StringBuilder stringBuilder4;
                if (apkStrategy2.length() > PER_STRATEGY_SIZE) {
                    apkStrategyTmp = apkStrategy2.substring(0, PER_STRATEGY_SIZE);
                    String str3 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("setApkControlFilterRules, apkStrategy len=");
                    stringBuilder4.append(apkStrategyTmp.length());
                    stringBuilder4.append(", seq=");
                    stringBuilder4.append(count);
                    stringBuilder4.append(", cmdId=");
                    stringBuilder4.append(cmdId);
                    Slog.d(str3, stringBuilder4.toString());
                    this.mConnector.execute("hwfilter", new Object[]{"set_delta_install_buf", Integer.valueOf(cmdId), Integer.valueOf(count), apkStrategyTmp});
                    apkStrategy2 = apkStrategy2.substring(PER_STRATEGY_SIZE);
                    count++;
                } else {
                    apkStrategyTmp = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("setApkFilterRules, apkStrategy len=");
                    stringBuilder4.append(apkStrategy2.length());
                    stringBuilder4.append(", seq=");
                    stringBuilder4.append(count);
                    stringBuilder4.append(", cmdId=");
                    stringBuilder4.append(cmdId);
                    Slog.d(apkStrategyTmp, stringBuilder4.toString());
                    this.mConnector.execute("hwfilter", new Object[]{"set_delta_install_buf", Integer.valueOf(cmdId), Integer.valueOf(count), apkStrategy2});
                    return;
                }
            }
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void sendApkDownloadUrlBroadcast(String[] cooked, String raw) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receive report_apkdl_event, raw=");
        stringBuilder.append(raw);
        Slog.d(str, stringBuilder.toString());
        if (cooked.length >= 4) {
            long startTime = SystemClock.elapsedRealtime();
            str = cooked[1];
            String uid = cooked[2];
            if (!this.startTimeMap.containsKey(str)) {
                this.startTimeMap.put(str, Long.valueOf(startTime));
            }
            Matcher m = this.p.matcher(cooked[3]);
            String url;
            if (!m.matches() || m.groupCount() < 3) {
                url = hexStrToStr(cooked[3]);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onEvent receive report_apkdl_event, startTime=");
                stringBuilder2.append(startTime);
                stringBuilder2.append(", downloadId=");
                stringBuilder2.append(str);
                stringBuilder2.append(", uid=");
                stringBuilder2.append(uid);
                stringBuilder2.append(", url=");
                stringBuilder2.append(url);
                Slog.d(str2, stringBuilder2.toString());
                Intent intent = new Intent(INTENT_APKDL_URL_DETECTED);
                intent.putExtra("startTime", startTime);
                intent.putExtra("downloadId", str);
                intent.putExtra("uid", uid);
                intent.putExtra("url", url);
                this.mContext.sendBroadcast(intent, "com.huawei.permission.AD_APKDL_STRATEGY");
                return;
            }
            int max = Integer.parseInt(m.group(1));
            int idx = Integer.parseInt(m.group(2));
            String subUrl = m.group(3);
            if (idx == 1) {
                this.urlBuffer = new StringBuffer();
                this.urlBuffer.append(subUrl);
            } else {
                this.urlBuffer.append(subUrl);
            }
            if (max == idx) {
                url = hexStrToStr(this.urlBuffer.toString());
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onEvent receive report_apkdl_event, startTime=");
                stringBuilder3.append(startTime);
                stringBuilder3.append(", downloadId=");
                stringBuilder3.append(str);
                stringBuilder3.append(", uid=");
                stringBuilder3.append(uid);
                stringBuilder3.append(", url=");
                stringBuilder3.append(url);
                Slog.d(str3, stringBuilder3.toString());
                Intent intent2 = new Intent(INTENT_APKDL_URL_DETECTED);
                intent2.putExtra("startTime", (Serializable) this.startTimeMap.get(str));
                intent2.putExtra("downloadId", str);
                intent2.putExtra("uid", uid);
                intent2.putExtra("url", url);
                this.mContext.sendBroadcast(intent2, "com.huawei.permission.AD_APKDL_STRATEGY");
                return;
            }
            return;
        }
        str = String.format("Invalid event from daemon (%s)", new Object[]{raw});
        Slog.d(TAG, "receive report_apkdl_event, return error");
        throw new IllegalStateException(str);
    }

    public void systemReady() {
        super.systemReady();
        initNetworkAccessWhitelist();
    }

    private void initNetworkAccessWhitelist() {
        final List<String> networkAccessWhitelist = HwDeviceManager.getList(9);
        if (networkAccessWhitelist != null && !networkAccessWhitelist.isEmpty()) {
            Slog.d(TAG, "networkAccessWhitelist has been set");
            new Thread() {
                public void run() {
                    HwNetworkManagementService.this.setNetworkAccessWhitelist(networkAccessWhitelist);
                }
            }.start();
        }
    }

    public void setNetworkAccessWhitelist(List<String> addrList) {
        String str;
        if (addrList != null) {
            try {
                if (!addrList.isEmpty()) {
                    int size = addrList.size();
                    String res = (String) this.mConnector.doCommand(CMD_NET_FILTER, new Object[]{ARG_IP_WHITELIST, ARG_SET, addrList.get(0)}).get(0);
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set ipwhitelist:");
                    stringBuilder.append(res);
                    Slog.d(str, stringBuilder.toString());
                    for (int i = 1; i < size; i++) {
                        str = (String) this.mConnector.doCommand(CMD_NET_FILTER, new Object[]{ARG_IP_WHITELIST, ARG_ADD, addrList.get(i)}).get(0);
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("add ipwhitelist:");
                        stringBuilder2.append(str);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    return;
                }
            } catch (NullPointerException npe) {
                Slog.e(TAG, "runNetFilterCmd:", npe);
                return;
            } catch (NativeDaemonConnectorException npe2) {
                Slog.e(TAG, "runNetFilterCmd:", npe2);
                return;
            }
        }
        str = (String) this.mConnector.doCommand(CMD_NET_FILTER, new Object[]{ARG_IP_WHITELIST, ARG_CLEAR}).get(0);
        NullPointerException npe22 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("clear ipwhitelist:");
        stringBuilder3.append(str);
        Slog.d(npe22, stringBuilder3.toString());
    }

    private void removeLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("network", new Object[]{"route", "legacy", Integer.valueOf(uid), "remove", Integer.valueOf(netId)});
        LinkAddress la = routeInfo.getDestinationLinkAddress();
        cmd.appendArg(routeInfo.getInterface());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(la.getAddress().getHostAddress());
        stringBuilder.append("/");
        stringBuilder.append(la.getPrefixLength());
        cmd.appendArg(stringBuilder.toString());
        if (routeInfo.hasGateway()) {
            cmd.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean closeSocketsForUid(int uid) {
        try {
            this.mNetdService.socketDestroy(new UidRange[]{new UidRange(uid, uid)}, new int[0]);
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error closing sockets for uid ");
            stringBuilder.append(uid);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private void setChrReportUid(int index, int uid) {
        try {
            String res = (String) this.mConnector.doCommand("chr", new Object[]{"appuid", ARG_SET, Integer.valueOf(index), Integer.valueOf(uid)}).get(0);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("chr appuid set:");
            stringBuilder.append(res);
            Slog.d(str, stringBuilder.toString());
        } catch (NullPointerException npe) {
            Slog.e(TAG, "runChrCmd:", npe);
        } catch (NativeDaemonConnectorException nde) {
            Slog.e(TAG, "runChrCmd:", nde);
        }
    }

    public void setFirewallPidRule(int chain, int pid, int rule) {
        try {
            this.mConnector.execute("firewall", new Object[]{"set_pid_rule", getFirewallChainName(chain), Integer.valueOf(pid), getFirewallRuleNameHw(chain, rule)});
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private void setNetBoosterVodEnabled(boolean enable) {
        if (this.mBoosterVideoAccEnabled) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setNetBoosterVodEnabled enable=");
                stringBuilder.append(enable);
                Slog.d(str, stringBuilder.toString());
                NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                String str2 = "hwnb";
                Object[] objArr = new Object[2];
                objArr[0] = "vod";
                objArr[1] = enable ? "enable" : "disable";
                nativeDaemonConnector.execute(str2, objArr);
            } catch (NativeDaemonConnectorException e) {
                Slog.e(TAG, "setNetBoosterVodEnabled:netd cmd execute failed", e);
            }
        }
    }

    private void setNetBoosterKsiEnabled(boolean enable) {
        if (this.mBoosterNetAiChangeEnabled) {
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setNetBoosterKsiEnabled enable=");
                stringBuilder.append(enable);
                Slog.d(str, stringBuilder.toString());
                NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                String str2 = "hwnb";
                Object[] objArr = new Object[2];
                objArr[0] = "ksi";
                objArr[1] = enable ? "enable" : "disable";
                nativeDaemonConnector.execute(str2, objArr);
            } catch (NativeDaemonConnectorException e) {
                Slog.e(TAG, "setNetBoosterKsiEnabled:netd cmd execute failed", e);
            }
        }
    }

    private void setNetBoosterAppUid(Bundle data) {
        int appUid = data.getInt("appUid");
        int period = data.getInt("reportPeriod");
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterAppUid,appUid=");
            stringBuilder.append(appUid);
            stringBuilder.append(",period=");
            stringBuilder.append(period);
            Slog.d(str, stringBuilder.toString());
            this.mConnector.execute("hwnb", new Object[]{"appQoe", "uid", Integer.valueOf(appUid), Integer.valueOf(period)});
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "setNetBoosterAppUid:netd cmd execute failed", e);
        }
    }

    private void setNetBoosterRsrpRsrq(Bundle data) {
        int rsrp = data.getInt("rsrp");
        int rsrq = data.getInt("rsrq");
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterRsrpRsrq,rsrp=");
            stringBuilder.append(rsrp);
            stringBuilder.append(",rsrq=");
            stringBuilder.append(rsrq);
            Slog.d(str, stringBuilder.toString());
            this.mConnector.execute("hwnb", new Object[]{"appQoe", "rsrp", Integer.valueOf(rsrp), Integer.valueOf(rsrq)});
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "setNetBoosterRsrpRsrq:netd cmd execute failed", e);
        }
    }

    public void reportVodParams(int videoSegState, int videoProtocol, int videoRemainingPlayTime, int videoStatus, int aveCodeRate, int segSize, int flowInfoRemote, int flowInfoLocal, int segDuration, int segIndex) {
        int i = videoSegState;
        int i2 = videoProtocol;
        int i3 = videoRemainingPlayTime;
        int i4 = videoStatus;
        int i5 = aveCodeRate;
        int i6 = segSize;
        int i7 = segDuration;
        int i8 = segIndex;
        if (this.mBoosterVideoAccEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportVodParams:videoSegState=");
            stringBuilder.append(i);
            stringBuilder.append(",videoProtocol=");
            stringBuilder.append(i2);
            stringBuilder.append(",videoRemainingPlayTime=");
            stringBuilder.append(i3);
            stringBuilder.append(",videoStatus=");
            stringBuilder.append(i4);
            stringBuilder.append(",aveCodeRate=");
            stringBuilder.append(i5);
            stringBuilder.append(",segSize=");
            stringBuilder.append(i6);
            stringBuilder.append(",segDuration=");
            stringBuilder.append(i7);
            stringBuilder.append(",segIndex=");
            stringBuilder.append(i8);
            Slog.d(str, stringBuilder.toString());
            IHwCommBoosterServiceManager bm = HwFrameworkFactory.getHwCommBoosterServiceManager();
            if (bm != null) {
                Bundle data = new Bundle();
                data.putInt("videoSegState", i);
                data.putInt("videoProtocol", i2);
                data.putInt("videoRemainingPlayTime", i3);
                data.putInt("videoStatus", i4);
                data.putInt("aveCodeRate", i5);
                data.putInt("segSize", i6);
                data.putInt("flowInfoRemote", flowInfoRemote);
                data.putInt("flowInfoLocal", flowInfoLocal);
                data.putInt("segDuration", i7);
                data.putInt("segIndex", i8);
                int ret = bm.reportBoosterPara("com.android.server", 1, data);
                if (ret != 0) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("reportVodParams:reportBoosterPara failed, ret=");
                    stringBuilder2.append(ret);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } else {
                int i9 = flowInfoRemote;
                int i10 = flowInfoLocal;
                Slog.e(TAG, "reportVodParams:null HwCommBoosterServiceManager");
            }
        }
    }

    public void reportKsiParams(int slowType, int avgAmp, int duration, int timeStart) {
        if (this.mBoosterNetAiChangeEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportKsiParams:slowType=");
            stringBuilder.append(slowType);
            stringBuilder.append(",avgAmp=");
            stringBuilder.append(avgAmp);
            stringBuilder.append(",duration=");
            stringBuilder.append(duration);
            stringBuilder.append(",timeStart=");
            stringBuilder.append(timeStart);
            Slog.d(str, stringBuilder.toString());
            IHwCommBoosterServiceManager bm = HwFrameworkFactory.getHwCommBoosterServiceManager();
            if (bm != null) {
                Bundle data = new Bundle();
                data.putInt("slowType", slowType);
                data.putInt("avgAmp", avgAmp);
                data.putInt("duration", duration);
                data.putInt("timeStart", timeStart);
                int ret = bm.reportBoosterPara("com.android.server", 2, data);
                if (ret != 0) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("reportKsiParams:reportBoosterPara failed, ret=");
                    stringBuilder2.append(ret);
                    Slog.e(str2, stringBuilder2.toString());
                }
            } else {
                Slog.e(TAG, "reportKsiParams:null HwCommBoosterServiceManager");
            }
        }
    }

    private void setNetBoosterPreDnsAppUid(Bundle b) {
        if (this.mBoosterPreDnsEnabled) {
            String preDnsAppUid = b.getString(TOP_APP_UID_INFO);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterPreDnsAppUid: preDnsAppUid: ");
            stringBuilder.append(preDnsAppUid);
            Slog.d(str, stringBuilder.toString());
            String[] apps = preDnsAppUid.split(" ");
            Object[] argv = new Object[14];
            argv[0] = "dns";
            argv[1] = "top_app_uid";
            int argc = 2;
            for (int i = 0; i < apps.length; i++) {
                int argc2 = argc + 1;
                argv[argc] = apps[i];
                if (i == apps.length - 1 || argc2 == argv.length) {
                    try {
                        this.mConnector.execute("hwnb", Arrays.copyOf(argv, argc2));
                    } catch (NativeDaemonConnectorException e) {
                        Slog.e(TAG, "setNetBoosterPreDnsAppUid: top_app_uid cmd execute failed", e);
                    }
                    argc = 2;
                } else {
                    argc = argc2;
                }
            }
            try {
                this.mConnector.execute("hwnb", new Object[]{"dns", "top_app_uid", "end"});
            } catch (NativeDaemonConnectorException e2) {
                Slog.e(TAG, "setNetBoosterPreDnsAppUid: top_app_uid end execute failed", e2);
            }
        }
    }

    private void setNetBoosterPreDnsBrowerUid(Bundle b) {
        if (this.mBoosterPreDnsEnabled) {
            String preDnsBrowserUid = b.getString(BROWSER_UID_INFO);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterPreDnsBrowerUid: preDnsBrowserUid: ");
            stringBuilder.append(preDnsBrowserUid);
            Slog.d(str, stringBuilder.toString());
            String[] browsers = preDnsBrowserUid.split(" ");
            Object[] argv = new Object[14];
            int i = 0;
            argv[0] = "dns";
            argv[1] = "browser_uid";
            int argc = 2;
            while (i < browsers.length) {
                int argc2 = argc + 1;
                argv[argc] = browsers[i];
                if (i == browsers.length - 1 || argc2 == argv.length) {
                    try {
                        this.mConnector.execute("hwnb", Arrays.copyOf(argv, argc2));
                    } catch (NativeDaemonConnectorException e) {
                        Slog.e(TAG, "setNetBoosterPreDnsBrowerUid: browser_uid cmd execute failed", e);
                    }
                    argc = 2;
                } else {
                    argc = argc2;
                }
                i++;
            }
        }
    }

    private void setNetBoosterPreDnsDomainName(Bundle b) {
        if (this.mBoosterPreDnsEnabled) {
            String preDnsUrl = b.getString(DNS_DOMAIN_NAME);
            Command cmd = new Command("hwnb", new Object[]{"dns", "top_url"});
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterPreDnsDomainName: preDnsUrl: ");
            stringBuilder.append(preDnsUrl);
            Slog.d(str, stringBuilder.toString());
            String[] urls = preDnsUrl.split(",");
            int cmdLength = 0;
            Command cmd2 = cmd;
            for (String[] urlAlias : urls) {
                String[] urlAlias2 = urlAlias2.split(" ");
                try {
                    cmdLength = (Integer.parseInt(urlAlias2[1]) + cmdLength) + 2;
                } catch (NumberFormatException e) {
                    Slog.e(TAG, "setNetBoosterPreDnsDomainName: top_url cnt format err", e);
                }
                if (cmdLength > 12) {
                    try {
                        this.mConnector.execute(cmd2);
                    } catch (NativeDaemonConnectorException e2) {
                        Slog.e(TAG, "setNetBoosterPreDnsDomainName: top_url cmd execute failed", e2);
                    }
                    cmdLength = 0;
                    cmd2 = new Command("hwnb", new Object[]{"dns", "top_url"});
                }
                for (String s : urlAlias2) {
                    cmd2.appendArg(s);
                }
            }
            try {
                this.mConnector.execute(cmd2);
                this.mConnector.execute("hwnb", new Object[]{"dns", "top_url", "end"});
            } catch (NativeDaemonConnectorException e3) {
                Slog.e(TAG, "setNetBoosterPreDnsDomainName: top_url cmd execute failed", e3);
            }
        }
    }

    private void setNetBoosterUidForeground(Bundle b) {
        if (this.mBoosterPreDnsEnabled) {
            int uid = b.getInt(FOREGROUND_UID);
            boolean isForeground = b.getBoolean(FOREGROUND_STATE);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetBoosterUidForeground uid: ");
            stringBuilder.append(uid);
            stringBuilder.append(", foreground: ");
            stringBuilder.append(isForeground);
            Slog.i(str, stringBuilder.toString());
            try {
                NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                String str2 = "hwnb";
                Object[] objArr = new Object[4];
                objArr[0] = "dns";
                objArr[1] = "uid_foreground";
                objArr[2] = Integer.valueOf(uid);
                objArr[3] = isForeground ? "true" : "false";
                nativeDaemonConnector.execute(str2, objArr);
            } catch (NativeDaemonConnectorException e) {
                Slog.e(TAG, "setNetBoosterUidForeground: uid_foreground execute failed", e);
            }
        }
    }
}

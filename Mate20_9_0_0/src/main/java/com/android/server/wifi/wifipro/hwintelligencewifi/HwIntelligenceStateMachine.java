package com.android.server.wifi.wifipro.hwintelligencewifi;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.LocalServices;
import com.android.server.policy.AbsPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wifi.wifipro.WifiProUIDisplayManager;
import com.android.server.wifi.wifipro.WifiproUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.List;

public class HwIntelligenceStateMachine extends StateMachine {
    private static final String ACTION_WIFI_PRO_TIMER = "android.net.wifi.wifi_pro_timer";
    private static final String COUNTRY_CODE_CN = "460";
    private static final int LOCATION_AVAILABLE_TIME = 30000;
    private static final int PING_PONG_HOME_MAX_PUNISH_TIME = 60000;
    private static final int PING_PONG_INTERVAL_TIME = 1800000;
    private static final int PING_PONG_MAX_PUNISH_TIME = 300000;
    private static final int PING_PONG_PUNISH_TIME = 30000;
    private static final int PING_PONG_TIME = 5000;
    private static final int WIFI_PRO_TIMER = 0;
    private static HwIntelligenceStateMachine mHwIntelligenceStateMachine;
    private AlarmManager mAlarmManager;
    private ApInfoManager mApInfoManager;
    private int mAuthType = -1;
    private int mAutoCloseMessage = 0;
    private int mAutoCloseScanTimes = 0;
    OnAlarmListener mAutoCloseTimeoutListener = new OnAlarmListener() {
        public void onAlarm() {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receive auto close message mAutoCloseMessage = ");
            stringBuilder.append(HwIntelligenceStateMachine.this.mAutoCloseMessage);
            Log.w(str, stringBuilder.toString());
            if (HwIntelligenceStateMachine.this.mAutoCloseMessage == 25) {
                HwIntelligenceStateMachine.this.mHandler.sendEmptyMessage(25);
            } else if (HwIntelligenceStateMachine.this.mAutoCloseMessage == 9) {
                HwIntelligenceStateMachine.this.mHandler.sendEmptyMessage(9);
            }
            HwIntelligenceStateMachine.this.mAutoCloseMessage = 0;
        }
    };
    private boolean mAutoOpenWifiWaitLocation = false;
    private BroadcastReceiver mBroadcastReceiver;
    private CellStateMonitor mCellStateMonitor;
    private String mConnectFailedBssid = null;
    private int mConnectFailedReason = -1;
    private String mConnectFailedSsid = null;
    private State mConnectedState = new ConnectedState();
    private Context mContext;
    private State mDefaultState = new DefaultState();
    private State mDisabledState = new DisabledState();
    private State mDisconnectedState = new DisconnectedState();
    private State mEnabledState = new EnabledState();
    private Handler mHandler;
    private LocationAddress mHomeAddress = null;
    private HomeAddressDataManager mHomeAddressManager;
    private HwintelligenceWiFiCHR mHwintelligenceWiFiCHR;
    private State mInitialState = new InitialState();
    private IntentFilter mIntentFilter;
    private State mInternetReadyState = new InternetReadyState();
    private boolean mIsAutoClose = false;
    private boolean mIsAutoCloseSearch = false;
    private boolean mIsAutoOpenSearch = false;
    private boolean mIsInitialState = false;
    private boolean mIsMachineStared = false;
    private boolean mIsOversea = false;
    private boolean mIsScreenOn = false;
    private boolean mIsWaittingAutoClose = false;
    private boolean mIsWifiP2PConnected = false;
    private long mLastCellChangeScanTime = 0;
    private LocationAddress mLastLocationAddress = null;
    private long mLastScanPingpongTime = 0;
    private long mLocationRequestFailed = 0;
    private State mNoInternetState = new NoInternetState();
    private int mScanPingpongNum = 0;
    private State mStopState = new StopState();
    private List<APInfoData> mTargetApInfoDatas;
    private String mTargetSsid = null;
    private WiFiStateMonitor mWiFiStateMonitor;
    private WifiManager mWifiManager;

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "ConnectedState");
            if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose) {
                HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                Log.e(MessageUtil.TAG, "ConnectedState remove MSG_WIFI_HANDLE_DISABLE");
                HwintelligenceWiFiCHR access$1900 = HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR;
                HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR;
                access$1900.uploadAutoCloseFailed(1);
            }
            if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
                HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                if (HwIntelligenceStateMachine.this.mTargetApInfoDatas != null) {
                    HwIntelligenceStateMachine.this.mTargetApInfoDatas.clear();
                    HwIntelligenceStateMachine.this.mTargetApInfoDatas = null;
                }
            }
            updateConnectedInfo();
            HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.stopConnectTimer();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 1) {
                switch (i) {
                    case 11:
                        HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mInternetReadyState);
                        break;
                    case 12:
                        Log.e(MessageUtil.TAG, "ConnectedState MSG_WIFI_INTERNET_DISCONNECTED");
                        HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mNoInternetState);
                        break;
                    case 13:
                        Log.e(MessageUtil.TAG, "MSG_WIFI_IS_PORTAL");
                        WifiInfo mPortalInfo = HwIntelligenceStateMachine.this.mWifiManager.getConnectionInfo();
                        if (!(mPortalInfo == null || mPortalInfo.getSSID() == null)) {
                            if (HwIntelligenceStateMachine.this.mApInfoManager.getApInfoByBssid(mPortalInfo.getBSSID()) != null) {
                                HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.uploadPortalApInWhite(mPortalInfo.getBSSID(), mPortalInfo.getSSID());
                            }
                            HwIntelligenceStateMachine.this.mApInfoManager.delectApInfoBySsidForPortal(mPortalInfo);
                        }
                        HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mNoInternetState);
                        break;
                    default:
                        return false;
                }
            }
            updateConnectedInfo();
            return true;
        }

        private void updateConnectedInfo() {
            WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(HwIntelligenceStateMachine.this.mWifiManager);
            if (config != null) {
                HwIntelligenceStateMachine.this.mTargetSsid = config.SSID;
                if (config.allowedKeyManagement.cardinality() <= 1) {
                    HwIntelligenceStateMachine.this.mAuthType = config.getAuthType();
                } else {
                    HwIntelligenceStateMachine.this.mAuthType = -1;
                }
                String str = MessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mTargetSsid is ");
                stringBuilder.append(HwIntelligenceStateMachine.this.mTargetSsid);
                stringBuilder.append(" mAuthType ");
                stringBuilder.append(HwIntelligenceStateMachine.this.mAuthType);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            String str;
            StringBuilder stringBuilder;
            String str2;
            StringBuilder stringBuilder2;
            switch (i) {
                case 1:
                    HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mConnectedState);
                    break;
                case 2:
                    HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mDisconnectedState);
                    break;
                case 3:
                    i = Global.getInt(HwIntelligenceStateMachine.this.mContext.getContentResolver(), "wifi_on", 0);
                    str = MessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MSG_WIFI_ENABLED wifiEnableFlag = ");
                    stringBuilder.append(i);
                    stringBuilder.append(" mIsAutoOpenSearch =");
                    stringBuilder.append(HwIntelligenceStateMachine.this.mIsAutoOpenSearch);
                    Log.e(str, stringBuilder.toString());
                    if (i != 1 && i != 2) {
                        if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                            Log.e(MessageUtil.TAG, "MSG_WIFI_ENABLED start scan");
                            HwIntelligenceStateMachine.this.mWifiManager.startScan();
                            break;
                        }
                    }
                    HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mEnabledState);
                    break;
                    break;
                case 4:
                    HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mDisabledState);
                    break;
                case 5:
                    str2 = MessageUtil.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" DefaultState message.what = ");
                    stringBuilder2.append(message.what);
                    Log.e(str2, stringBuilder2.toString());
                    break;
                default:
                    Bundle data;
                    switch (i) {
                        case 7:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                            break;
                        case 8:
                            data = message.getData();
                            str = data.getString("bssid");
                            String ssid = data.getString("ssid");
                            String str3 = MessageUtil.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("MSG_WIFI_CONFIG_CHANGED ssid = ");
                            stringBuilder3.append(ssid);
                            Log.e(str3, stringBuilder3.toString());
                            if (ssid == null) {
                                HwIntelligenceStateMachine.this.mApInfoManager.delectApInfoByBssid(str);
                                break;
                            }
                            HwIntelligenceStateMachine.this.mApInfoManager.delectApInfoBySsid(ssid);
                            break;
                        case 14:
                            HwIntelligenceStateMachine.this.mIsWifiP2PConnected = true;
                            break;
                        case 15:
                            HwIntelligenceStateMachine.this.mIsWifiP2PConnected = false;
                            break;
                        default:
                            switch (i) {
                                case 20:
                                    break;
                                case 21:
                                    Log.e(MessageUtil.TAG, " DefaultState MSG_SCREEN_ON");
                                    HwIntelligenceStateMachine.this.sendMessage(23);
                                    break;
                                case 22:
                                    str2 = MessageUtil.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(" DefaultState MSG_SCREEN_OFF mIsAutoOpenSearch = ");
                                    stringBuilder2.append(HwIntelligenceStateMachine.this.mIsAutoOpenSearch);
                                    Log.e(str2, stringBuilder2.toString());
                                    if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                                        HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                                        break;
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case 24:
                                            data = message.getData();
                                            HwIntelligenceStateMachine.this.mConnectFailedReason = data.getInt("reason");
                                            HwIntelligenceStateMachine.this.mConnectFailedBssid = data.getString("bssid");
                                            HwIntelligenceStateMachine.this.mConnectFailedSsid = data.getString("ssid");
                                            str = MessageUtil.TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("MSG_CONNECT_FAILED ssid = ");
                                            stringBuilder.append(HwIntelligenceStateMachine.this.mConnectFailedSsid);
                                            stringBuilder.append(" mConnectFailedReason = ");
                                            stringBuilder.append(HwIntelligenceStateMachine.this.mConnectFailedReason);
                                            Log.e(str, stringBuilder.toString());
                                            break;
                                        case 25:
                                            break;
                                        default:
                                            switch (i) {
                                                case MessageUtil.MSG_UPDATE_LOCATION /*29*/:
                                                case 30:
                                                case MessageUtil.MSG_GET_LOCATION_FAIL /*31*/:
                                                case MessageUtil.MSG_UPDATE_TARGET_SSID /*32*/:
                                                    break;
                                                default:
                                                    switch (i) {
                                                        case 100:
                                                            HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mInitialState);
                                                            break;
                                                        case 101:
                                                            HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mStopState);
                                                            break;
                                                    }
                                                    break;
                                            }
                                    }
                            }
                    }
                    str2 = MessageUtil.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" DefaultState message.what = ");
                    stringBuilder2.append(message.what);
                    Log.e(str2, stringBuilder2.toString());
                    break;
            }
            return true;
        }
    }

    class DisabledState extends State {
        DisabledState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "DisabledState");
            if (HwIntelligenceStateMachine.this.mIsInitialState) {
                Log.e(MessageUtil.TAG, "mIsInitialState state is disable");
                HwIntelligenceStateMachine.this.mIsInitialState = false;
            } else if (HwIntelligenceStateMachine.this.isClosedByUser()) {
                if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose) {
                    Log.e(MessageUtil.TAG, "DisabledState remove MSG_WIFI_HANDLE_DISABLE");
                    HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                    HwintelligenceWiFiCHR access$1900 = HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR;
                    HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR;
                    access$1900.uploadAutoCloseFailed(2);
                }
                if (HwIntelligenceStateMachine.this.mIsAutoClose) {
                    HwIntelligenceStateMachine.this.mIsAutoClose = false;
                    HwIntelligenceStateMachine.this.setAutoOpenValue(false);
                    if (!HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                        HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
                        HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                        HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                    }
                    HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.increaseAutoCloseCount();
                } else if (HwIntelligenceStateMachine.this.isScreenOn(HwIntelligenceStateMachine.this.mContext)) {
                    HwIntelligenceStateMachine.this.setAutoOpenValue(false);
                    HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
                    HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                    HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                    List<ScanResult> mlist = null;
                    if (HwIntelligenceStateMachine.this.mWifiManager.isScanAlwaysAvailable()) {
                        mlist = WifiproUtils.getScanResultsFromWsm();
                    }
                    if (mlist == null || mlist.size() == 0) {
                        Log.d(MessageUtil.TAG, "getScanResultsFromWsm is null, get from WiFiProScanResultList.");
                        mlist = HwIntelligenceWiFiManager.getWiFiProScanResultList();
                    }
                    if ((mlist == null || mlist.size() == 0) && HwIntelligenceStateMachine.this.mTargetSsid != null) {
                        String str = MessageUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("WiFiProScanResultList is null, get from connected history. mTargetSsid is ");
                        stringBuilder.append(HwIntelligenceStateMachine.this.mTargetSsid);
                        stringBuilder.append(" mAuthType ");
                        stringBuilder.append(HwIntelligenceStateMachine.this.mAuthType);
                        Log.d(str, stringBuilder.toString());
                        HwIntelligenceStateMachine.this.mApInfoManager.setBlackListBySsid(HwIntelligenceStateMachine.this.mTargetSsid, HwIntelligenceStateMachine.this.mAuthType, true);
                    }
                    HwIntelligenceStateMachine.this.mTargetSsid = null;
                    HwIntelligenceStateMachine.this.mAuthType = -1;
                    HwIntelligenceStateMachine.this.mApInfoManager.resetBlackList(mlist, true);
                    HwIntelligenceWiFiManager.setWiFiProScanResultList(null);
                }
                HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.stopConnectTimer();
                HwIntelligenceStateMachine.this.initPunishParameter();
            } else {
                Log.e(MessageUtil.TAG, "MSG_WIFI_DISABLE by auto");
            }
        }

        public void exit() {
            Log.d(MessageUtil.TAG, "DisabledState exit");
            if (HwIntelligenceStateMachine.this.hasMessages(29)) {
                HwIntelligenceStateMachine.this.removeMessages(29);
            }
        }

        /* JADX WARNING: Missing block: B:25:0x008a, code:
            if (com.android.server.wifi.wifipro.hwintelligencewifi.HwIntelligenceStateMachine.access$4500(r0.this$0) != false) goto L_0x03ab;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            Message message2 = message;
            int i = message2.what;
            if (i != 2) {
                if (i != 7) {
                    String str;
                    if (!(i == 20 || i == 23)) {
                        if (i != 26) {
                            if (i != 102) {
                                switch (i) {
                                    case 4:
                                        break;
                                    case 5:
                                        Log.e(MessageUtil.TAG, "DisabledState MSG_WIFI_FIND_TARGET");
                                        HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                                        if (!HwIntelligenceStateMachine.this.mIsOversea && !HwIntelligenceStateMachine.this.isLocationAvaliable(HwIntelligenceStateMachine.this.mLastLocationAddress)) {
                                            HwIntelligenceStateMachine.this.requestLocationInfo();
                                            HwIntelligenceStateMachine.this.mAutoOpenWifiWaitLocation = true;
                                            HwIntelligenceStateMachine.this.mScanPingpongNum = 1;
                                            HwIntelligenceStateMachine.this.sendMessageDelayed(29, 3000);
                                            break;
                                        }
                                        HwIntelligenceStateMachine.this.sendMessageDelayed(26, 3000);
                                        break;
                                        break;
                                    default:
                                        switch (i) {
                                            case MessageUtil.MSG_CONFIGURATION_CHANGED /*28*/:
                                                break;
                                            case MessageUtil.MSG_UPDATE_LOCATION /*29*/:
                                            case MessageUtil.MSG_GET_LOCATION_FAIL /*31*/:
                                                if (HwIntelligenceStateMachine.this.mAutoOpenWifiWaitLocation) {
                                                    HwIntelligenceStateMachine.this.mLastLocationAddress = null;
                                                    HwIntelligenceStateMachine.this.mAutoOpenWifiWaitLocation = false;
                                                    HwIntelligenceStateMachine.this.sendMessage(26);
                                                    break;
                                                }
                                                break;
                                            case 30:
                                                Bundle data = message.getData();
                                                if (data != null) {
                                                    HwIntelligenceStateMachine.this.mLastLocationAddress = new LocationAddress(data.getDouble(HomeAddressDataManager.LATITUDE_KEY), data.getDouble(HomeAddressDataManager.LONGITUDE_KEY), data.getDouble(HomeAddressDataManager.DISTANCE_KEY), Long.valueOf(System.currentTimeMillis()));
                                                }
                                                if (HwIntelligenceStateMachine.this.mAutoOpenWifiWaitLocation) {
                                                    HwIntelligenceStateMachine.this.mAutoOpenWifiWaitLocation = false;
                                                    HwIntelligenceStateMachine.this.sendMessage(26);
                                                    break;
                                                }
                                                break;
                                            default:
                                                return false;
                                        }
                                }
                            }
                            Log.e(MessageUtil.TAG, "CMD_START_SCAN");
                            HwIntelligenceStateMachine.this.mWifiManager.startScan();
                        } else if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                            HwIntelligenceStateMachine.this.mIsScreenOn = HwIntelligenceStateMachine.this.isScreenOn(HwIntelligenceStateMachine.this.mContext);
                            str = MessageUtil.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("MSG_WIFI_HANDLE_OPEN mWifiManager.getWifiState() = ");
                            stringBuilder.append(HwIntelligenceStateMachine.this.mWifiManager.getWifiState());
                            stringBuilder.append("  mIsScreenOn = ");
                            stringBuilder.append(HwIntelligenceStateMachine.this.mIsScreenOn);
                            stringBuilder.append("  mIsFullScreen  ");
                            stringBuilder.append(HwIntelligenceStateMachine.this.isFullScreen());
                            stringBuilder.append(" mIsOversea = ");
                            stringBuilder.append(HwIntelligenceStateMachine.this.mIsOversea);
                            Log.d(str, stringBuilder.toString());
                            if (HwIntelligenceStateMachine.this.mLastLocationAddress != null) {
                                str = MessageUtil.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("mLastLocationAddress.isHome = ");
                                stringBuilder.append(HwIntelligenceStateMachine.this.mLastLocationAddress.isHome());
                                Log.d(str, stringBuilder.toString());
                            }
                            if (HwIntelligenceStateMachine.this.mWifiManager.getWifiState() == 1 && !HwIntelligenceStateMachine.this.mWifiManager.isWifiEnabled() && HwIntelligenceStateMachine.this.mIsScreenOn && !HwIntelligenceStateMachine.this.isFullScreen() && (HwIntelligenceStateMachine.this.mIsOversea || HwIntelligenceStateMachine.this.mLastLocationAddress == null || HwIntelligenceStateMachine.this.mLastLocationAddress.isHome())) {
                                HwIntelligenceStateMachine.this.setAutoOpenValue(true);
                                HwIntelligenceStateMachine.this.mWifiManager.setWifiEnabled(true);
                                HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.startConnectTimer();
                                HwIntelligenceStateMachine.this.mHwintelligenceWiFiCHR.increaseAutoOpenCount();
                            } else if (HwIntelligenceStateMachine.this.mWifiManager.getWifiState() == 0 && HwIntelligenceStateMachine.this.mIsScreenOn && !HwIntelligenceStateMachine.this.isFullScreen()) {
                                HwIntelligenceStateMachine.this.sendMessageDelayed(26, 3000);
                            }
                        }
                    }
                    HwIntelligenceStateMachine.this.mIsScreenOn = HwIntelligenceStateMachine.this.isScreenOn(HwIntelligenceStateMachine.this.mContext);
                    str = HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid();
                    String str2 = MessageUtil.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DisabledState cellid = ");
                    stringBuilder2.append(str);
                    Log.e(str2, stringBuilder2.toString());
                    if (str != null) {
                        if (HwIntelligenceStateMachine.this.mApInfoManager.isMonitorCellId(str)) {
                            if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose) {
                                HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                            }
                            str2 = MessageUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DisabledState current cell id is monitor ..... cellid = ");
                            stringBuilder2.append(str);
                            Log.d(str2, stringBuilder2.toString());
                            HwIntelligenceStateMachine.this.mTargetApInfoDatas = HwIntelligenceStateMachine.this.removeFromBlackList(HwIntelligenceStateMachine.this.mApInfoManager.getMonitorDatas(str));
                            if (HwIntelligenceStateMachine.this.mTargetApInfoDatas.size() > 0) {
                                String str3 = MessageUtil.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("DisabledState mTargetApInfoDatas.size() =");
                                stringBuilder3.append(HwIntelligenceStateMachine.this.mTargetApInfoDatas.size());
                                Log.d(str3, stringBuilder3.toString());
                                if (HwIntelligenceStateMachine.this.getSettingSwitchType() && HwIntelligenceStateMachine.this.mIsScreenOn && Global.getInt(HwIntelligenceStateMachine.this.mContext.getContentResolver(), "wifi_on", 0) == 0) {
                                    if (message2.what == 20) {
                                        HwIntelligenceStateMachine.this.setPingpongPunishTime();
                                        if (HwIntelligenceStateMachine.this.isInPingpongPunishTime()) {
                                            Log.d(MessageUtil.TAG, "DisabledState in punish time can not scan");
                                        } else {
                                            HwIntelligenceStateMachine.this.mLastScanPingpongTime = System.currentTimeMillis();
                                        }
                                    }
                                    Log.d(MessageUtil.TAG, "DisabledState start auto open search");
                                    HwIntelligenceStateMachine.this.mIsAutoOpenSearch = true;
                                    HwIntelligenceStateMachine.this.mApInfoManager.startScanAp();
                                }
                            } else {
                                Log.d(MessageUtil.TAG, "DisabledState mTargetApInfoDatas.size() == 0");
                                HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
                                HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                            }
                        } else {
                            if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch && HwIntelligenceStateMachine.this.mTargetApInfoDatas != null && HwIntelligenceStateMachine.this.mTargetApInfoDatas.size() > 0) {
                                List<ScanResult> mLists = WifiproUtils.getScanResultsFromWsm();
                                if (mLists != null && mLists.size() > 0 && HwIntelligenceStateMachine.this.mApInfoManager.isHasTargetAp(mLists)) {
                                    Log.d(MessageUtil.TAG, "DisabledState Learn new Cell id");
                                    HwIntelligenceStateMachine.this.mApInfoManager.processScanResult(HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid());
                                    HwIntelligenceStateMachine.this.mApInfoManager.updateScanResult();
                                }
                            }
                            String str4 = MessageUtil.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("current cell id is not monitor ..... cellid = ");
                            stringBuilder4.append(str);
                            Log.d(str4, stringBuilder4.toString());
                            HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
                            HwIntelligenceStateMachine.this.mApInfoManager.stopScanAp();
                        }
                    }
                } else {
                    Log.e(MessageUtil.TAG, " DisabledState MSG_WIFI_UPDATE_SCAN_RESULT");
                    if (HwIntelligenceStateMachine.this.mIsAutoOpenSearch) {
                        HwIntelligenceStateMachine.this.mApInfoManager.updateScanResult();
                    }
                }
            }
            return true;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "DisconnectedState enter");
            if (HwIntelligenceStateMachine.this.getAutoOpenValue()) {
                HwIntelligenceStateMachine.this.mIsAutoCloseSearch = true;
                HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                HwIntelligenceStateMachine.this.mWifiManager.startScan();
            } else {
                HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
            }
            HwIntelligenceStateMachine.this.mAutoCloseScanTimes = 0;
            HwIntelligenceStateMachine.this.mAutoCloseMessage = 0;
            if (HwIntelligenceStateMachine.this.mTargetSsid != null) {
                HwIntelligenceStateMachine.this.sendMessageDelayed(32, 10000);
            }
        }

        public void exit() {
            Log.e(MessageUtil.TAG, "DisconnectedState exit");
            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
            HwIntelligenceStateMachine.this.mAutoCloseScanTimes = 0;
            HwIntelligenceStateMachine.this.releaseAutoTimer();
            HwIntelligenceStateMachine.this.removeMessages(32);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 2) {
                String str;
                StringBuilder stringBuilder;
                if (i == 7) {
                    str = MessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DisconnectedState MSG_WIFI_UPDATE_SCAN_RESULT mIsAutoCloseSearch = ");
                    stringBuilder.append(HwIntelligenceStateMachine.this.mIsAutoCloseSearch);
                    stringBuilder.append(" mIsWaittingAutoClose = ");
                    stringBuilder.append(HwIntelligenceStateMachine.this.mIsWaittingAutoClose);
                    Log.d(str, stringBuilder.toString());
                    List<ScanResult> mLists;
                    if (HwIntelligenceStateMachine.this.mIsAutoCloseSearch) {
                        mLists = HwIntelligenceStateMachine.this.mWifiManager.getScanResults();
                        String str2;
                        StringBuilder stringBuilder2;
                        if (mLists.size() <= 0) {
                            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = true;
                            str2 = MessageUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DisconnectedState send disable message mAutoCloseMessage =");
                            stringBuilder2.append(HwIntelligenceStateMachine.this.mAutoCloseMessage);
                            Log.w(str2, stringBuilder2.toString());
                            HwIntelligenceStateMachine.this.setAutoTimer(9);
                        } else if (HwIntelligenceStateMachine.this.mApInfoManager.handleAutoScanResult(mLists)) {
                            Log.w(MessageUtil.TAG, "DisconnectedState learn new cell info");
                            HwIntelligenceStateMachine.this.mApInfoManager.processScanResult(HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid());
                            str2 = MessageUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DisconnectedState send MSG_WIFI_AUTO_CLOSE_SCAN message mAutoCloseMessage =");
                            stringBuilder2.append(HwIntelligenceStateMachine.this.mAutoCloseMessage);
                            Log.e(str2, stringBuilder2.toString());
                            HwIntelligenceStateMachine.this.setAutoTimer(25);
                        } else {
                            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = true;
                            str2 = MessageUtil.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DisconnectedState first send disable message mAutoCloseMessage = ");
                            stringBuilder2.append(HwIntelligenceStateMachine.this.mAutoCloseMessage);
                            Log.w(str2, stringBuilder2.toString());
                            HwIntelligenceStateMachine.this.setAutoTimer(9);
                        }
                    } else if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose) {
                        mLists = HwIntelligenceStateMachine.this.mWifiManager.getScanResults();
                        if (mLists.size() > 0 && HwIntelligenceStateMachine.this.mApInfoManager.handleAutoScanResult(mLists)) {
                            Log.d(MessageUtil.TAG, "DisconnectedState MSG_WIFI_UPDATE_SCAN_RESULT remove auto close message");
                            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = true;
                            HwIntelligenceStateMachine.this.mApInfoManager.processScanResult(HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid());
                            HwIntelligenceStateMachine.this.setAutoTimer(25);
                        }
                    }
                } else if (i == 9) {
                    str = MessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MessageUtil.MSG_WIFI_HANDLE_DISABLE mIsWifiP2PConnected = ");
                    stringBuilder.append(HwIntelligenceStateMachine.this.mIsWifiP2PConnected);
                    Log.w(str, stringBuilder.toString());
                    HwIntelligenceStateMachine.this.releaseAutoTimer();
                    HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                    if (!HwIntelligenceStateMachine.this.mIsWifiP2PConnected) {
                        HwIntelligenceStateMachine.this.autoDisbleWiFi();
                    }
                } else if (i == 23) {
                    str = MessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DisconnectedState MessageUtil.MSG_HANDLE_STATE_CHANGE mIsAutoCloseSearch = ");
                    stringBuilder.append(HwIntelligenceStateMachine.this.mIsAutoCloseSearch);
                    Log.w(str, stringBuilder.toString());
                    if (HwIntelligenceStateMachine.this.mIsAutoCloseSearch) {
                        HwIntelligenceStateMachine.this.mWifiManager.startScan();
                    }
                } else if (i == 25) {
                    Log.e(MessageUtil.TAG, "DisconnectedState MSG_WIFI_AUTO_CLOSE_SCAN");
                    HwIntelligenceStateMachine.this.mWifiManager.startScan();
                } else if (i != 27) {
                    if (i != 32) {
                        switch (i) {
                            case 14:
                                Log.w(MessageUtil.TAG, "DisconnectedState MessageUtil.MSG_WIFI_P2P_CONNECTED");
                                HwIntelligenceStateMachine.this.mIsWifiP2PConnected = true;
                                if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose || HwIntelligenceStateMachine.this.mIsAutoCloseSearch) {
                                    Log.e(MessageUtil.TAG, "DisconnectedState remove MSG_WIFI_HANDLE_DISABLE");
                                    HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                                    HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                                    HwIntelligenceStateMachine.this.releaseAutoTimer();
                                    break;
                                }
                            case 15:
                                Log.w(MessageUtil.TAG, "MessageUtil.MSG_WIFI_P2P_DISCONNECTED");
                                HwIntelligenceStateMachine.this.mIsWifiP2PConnected = false;
                                if (HwIntelligenceStateMachine.this.getAutoOpenValue()) {
                                    HwIntelligenceStateMachine.this.mIsAutoCloseSearch = true;
                                    HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                                    HwIntelligenceStateMachine.this.mWifiManager.startScan();
                                    break;
                                }
                                break;
                            default:
                                return false;
                        }
                    }
                    Log.d(MessageUtil.TAG, "DisconnectedState MessageUtil.MSG_UPDATE_TARGET_SSID");
                    HwIntelligenceStateMachine.this.mTargetSsid = null;
                } else if (HwIntelligenceStateMachine.this.mIsWaittingAutoClose || HwIntelligenceStateMachine.this.mIsAutoCloseSearch) {
                    Log.e(MessageUtil.TAG, "DisconnectedState MSG_WIFI_CONNECTING");
                    HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                    HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
                    HwIntelligenceStateMachine.this.releaseAutoTimer();
                }
            } else if (HwIntelligenceStateMachine.this.getAutoOpenValue()) {
                HwIntelligenceStateMachine.this.mIsAutoCloseSearch = true;
                HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
                HwIntelligenceStateMachine.this.mWifiManager.startScan();
            }
            return true;
        }
    }

    class EnabledState extends State {
        EnabledState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "EnabledState");
            HwIntelligenceStateMachine.this.mApInfoManager.resetAllBlackList();
        }

        public boolean processMessage(Message message) {
            if (message.what != 3) {
                return false;
            }
            return true;
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "InitialState");
            HwIntelligenceStateMachine.this.mTargetApInfoDatas = null;
            HwIntelligenceStateMachine.this.mIsAutoClose = false;
            HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
            HwIntelligenceStateMachine.this.mIsWifiP2PConnected = false;
            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
            HwIntelligenceStateMachine.this.mIsInitialState = false;
            if (!HwIntelligenceStateMachine.this.mWifiManager.isWifiEnabled() && Global.getInt(HwIntelligenceStateMachine.this.mContext.getContentResolver(), "wifi_on", 0) == 0) {
                Log.e(MessageUtil.TAG, "InitialState wifi is disable");
                HwIntelligenceStateMachine.this.mIsInitialState = true;
                HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mDisabledState);
            }
        }

        public boolean processMessage(Message message) {
            return false;
        }
    }

    class InternetReadyState extends State {
        InternetReadyState() {
        }

        public void enter() {
            boolean bMobileAP = isMobileAP();
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mInternetReadyState bMobileAP = ");
            stringBuilder.append(bMobileAP);
            Log.e(str, stringBuilder.toString());
            WifiInfo Info = HwIntelligenceStateMachine.this.mWifiManager.getConnectionInfo();
            if (Info != null && Info.getBSSID() != null && !bMobileAP) {
                HwIntelligenceStateMachine.this.mApInfoManager.addCurrentApInfo(HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid());
                if (HwIntelligenceStateMachine.this.mHomeAddress == null || System.currentTimeMillis() - HwIntelligenceStateMachine.this.mHomeAddress.getUpdateTime() >= 30000) {
                    Log.d(MessageUtil.TAG, "HomeAddress needs update");
                    HwIntelligenceStateMachine.this.mHomeAddress = HwIntelligenceStateMachine.this.mHomeAddressManager.getLastHomeAddress();
                    if (HwIntelligenceStateMachine.this.mHomeAddress != null) {
                        String str2 = MessageUtil.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("HomeAddress isOversea : ");
                        stringBuilder2.append(HwIntelligenceStateMachine.this.mHomeAddress.isOversea());
                        stringBuilder2.append(" , isInvalid : ");
                        stringBuilder2.append(HwIntelligenceStateMachine.this.mHomeAddress.isInvalid());
                        Log.d(str2, stringBuilder2.toString());
                        HwIntelligenceStateMachine.this.mIsOversea = HwIntelligenceStateMachine.this.mHomeAddress.isOversea();
                    }
                }
                if (!HwIntelligenceStateMachine.this.mIsOversea && !HwIntelligenceStateMachine.this.isLocationAvaliable(HwIntelligenceStateMachine.this.mLastLocationAddress)) {
                    HwIntelligenceStateMachine.this.mLocationRequestFailed = 0;
                    HwIntelligenceStateMachine.this.requestLocationInfo();
                }
            }
        }

        public void exit() {
            Log.d(MessageUtil.TAG, "InternetReadyState exit");
            HwIntelligenceStateMachine.this.mLocationRequestFailed = 0;
            if (HwIntelligenceStateMachine.this.hasMessages(29)) {
                HwIntelligenceStateMachine.this.removeMessages(29);
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 11) {
                switch (i) {
                    case 20:
                    case 21:
                        if (!isMobileAP()) {
                            String cellid = HwIntelligenceStateMachine.this.mCellStateMonitor.getCurrentCellid();
                            if (cellid != null) {
                                HwIntelligenceStateMachine.this.mApInfoManager.updataApInfo(cellid);
                                break;
                            }
                        }
                        break;
                    default:
                        switch (i) {
                            case MessageUtil.MSG_UPDATE_LOCATION /*29*/:
                                if (HwIntelligenceStateMachine.this.mLocationRequestFailed < 3) {
                                    HwIntelligenceStateMachine.this.requestLocationInfo();
                                    break;
                                }
                                break;
                            case 30:
                                Log.d(MessageUtil.TAG, "InternetReadyState ConnectedState MessageUtil.MSG_LACATION_READY");
                                HwIntelligenceStateMachine.this.mLocationRequestFailed = 0;
                                Bundle data = message.getData();
                                if (data != null) {
                                    HwIntelligenceStateMachine.this.mLastLocationAddress = new LocationAddress(data.getDouble(HomeAddressDataManager.LATITUDE_KEY), data.getDouble(HomeAddressDataManager.LONGITUDE_KEY), data.getDouble(HomeAddressDataManager.DISTANCE_KEY), Long.valueOf(System.currentTimeMillis()));
                                    if (HwIntelligenceStateMachine.this.isLocationAvaliable(HwIntelligenceStateMachine.this.mLastLocationAddress)) {
                                        HwIntelligenceStateMachine.this.mApInfoManager.updateCurrentApHomebySsid(HwIntelligenceStateMachine.this.mTargetSsid, HwIntelligenceStateMachine.this.mAuthType, HwIntelligenceStateMachine.this.mLastLocationAddress.isHome());
                                        break;
                                    }
                                }
                                break;
                            case MessageUtil.MSG_GET_LOCATION_FAIL /*31*/:
                                HwIntelligenceStateMachine.access$2914(HwIntelligenceStateMachine.this, 1);
                                HwIntelligenceStateMachine.this.sendMessageDelayed(29, 3000);
                                break;
                            default:
                                return false;
                        }
                }
            }
            Log.d(MessageUtil.TAG, "InternetReadyState MessageUtil.MSG_WIFI_INTERNET_CONNECTED");
            return true;
        }

        private boolean isMobileAP() {
            if (HwIntelligenceStateMachine.this.mContext != null) {
                return HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwIntelligenceStateMachine.this.mContext);
            }
            return false;
        }
    }

    class NoInternetState extends State {
        NoInternetState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "NoInternetState");
            WifiInfo mConnectInfo = HwIntelligenceStateMachine.this.mWifiManager.getConnectionInfo();
            if (mConnectInfo != null && mConnectInfo.getBSSID() != null) {
                HwIntelligenceStateMachine.this.mApInfoManager.resetBlackListByBssid(mConnectInfo.getBSSID(), true);
            }
        }

        public boolean processMessage(Message message) {
            return false;
        }
    }

    class StopState extends State {
        StopState() {
        }

        public void enter() {
            Log.e(MessageUtil.TAG, "StopState");
            HwIntelligenceStateMachine.this.mTargetApInfoDatas = null;
            HwIntelligenceStateMachine.this.setAutoOpenValue(false);
            HwIntelligenceStateMachine.this.mIsAutoClose = false;
            HwIntelligenceStateMachine.this.mIsAutoOpenSearch = false;
            HwIntelligenceStateMachine.this.mIsAutoCloseSearch = false;
            HwIntelligenceStateMachine.this.mIsWifiP2PConnected = false;
            HwIntelligenceStateMachine.this.mIsWaittingAutoClose = false;
        }

        public boolean processMessage(Message message) {
            if (message.what != 100) {
                return true;
            }
            HwIntelligenceStateMachine.this.transitionTo(HwIntelligenceStateMachine.this.mInitialState);
            return true;
        }
    }

    static /* synthetic */ long access$2914(HwIntelligenceStateMachine x0, long x1) {
        long j = x0.mLocationRequestFailed + x1;
        x0.mLocationRequestFailed = j;
        return j;
    }

    private void registerNetworkReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.CONFIGURATION_CHANGED".equals(intent.getAction())) {
                    HwIntelligenceStateMachine.this.sendMessageDelayed(28, 1000);
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    private boolean isFullScreen() {
        AbsPhoneWindowManager policy = (AbsPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
        return policy != null && policy.isTopIsFullscreen();
    }

    public static HwIntelligenceStateMachine createIntelligenceStateMachine(Context context, WifiProUIDisplayManager UIManager) {
        if (mHwIntelligenceStateMachine == null) {
            mHwIntelligenceStateMachine = new HwIntelligenceStateMachine(context, UIManager);
        }
        return mHwIntelligenceStateMachine;
    }

    private HwIntelligenceStateMachine(Context context, WifiProUIDisplayManager UIManager) {
        super("HwIntelligenceStateMachine");
        this.mContext = context;
        this.mHandler = getHandler();
        Context context2 = this.mContext;
        Context context3 = this.mContext;
        this.mWifiManager = (WifiManager) context2.getSystemService("wifi");
        this.mHwintelligenceWiFiCHR = HwintelligenceWiFiCHR.getInstance(this);
        this.mWiFiStateMonitor = new WiFiStateMonitor(context, getHandler());
        this.mCellStateMonitor = new CellStateMonitor(context, getHandler());
        this.mApInfoManager = new ApInfoManager(context, this, getHandler());
        this.mHomeAddressManager = new HomeAddressDataManager(context, getHandler());
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mEnabledState, this.mDefaultState);
        addState(this.mDisabledState, this.mDefaultState);
        addState(this.mConnectedState, this.mEnabledState);
        addState(this.mInternetReadyState, this.mConnectedState);
        addState(this.mNoInternetState, this.mConnectedState);
        addState(this.mDisconnectedState, this.mEnabledState);
        addState(this.mStopState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        registerNetworkReceiver();
        start();
    }

    private void setAutoTimer(int message) {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DisconnectedState setAutoTimer message = ");
        stringBuilder.append(message);
        Log.e(str, stringBuilder.toString());
        if (this.mAutoCloseMessage == message) {
            Log.e(MessageUtil.TAG, "DisconnectedState setAutoTimer mAutoCloseMessage == message");
            return;
        }
        if (message == 25) {
            this.mAutoCloseMessage = message;
            if (this.mAutoCloseScanTimes >= 1) {
                Log.e(MessageUtil.TAG, "DisconnectedState setAutoTimer mAutoCloseScanTimes >= 1");
                return;
            }
            str = MessageUtil.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DisconnectedState setAutoTimer mAutoCloseScanTimes =");
            stringBuilder2.append(this.mAutoCloseScanTimes);
            Log.e(str, stringBuilder2.toString());
            this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 120000, MessageUtil.TAG, this.mAutoCloseTimeoutListener, getHandler());
            this.mAutoCloseScanTimes++;
        } else {
            this.mAutoCloseMessage = message;
            this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 120000, MessageUtil.TAG, this.mAutoCloseTimeoutListener, getHandler());
        }
    }

    private void releaseAutoTimer() {
        Log.e(MessageUtil.TAG, "DisconnectedState releaseAutoTimer");
        this.mAutoCloseMessage = 0;
        this.mAlarmManager.cancel(this.mAutoCloseTimeoutListener);
    }

    private boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (pm == null || !pm.isScreenOn()) {
            return false;
        }
        return true;
    }

    private boolean isAirModeOn() {
        boolean z = false;
        if (this.mContext == null) {
            return false;
        }
        if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            z = true;
        }
        return z;
    }

    private boolean isClosedByUser() {
        if (isAirModeOn()) {
            return false;
        }
        return true;
    }

    private void setAutoOpenValue(boolean enable) {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAutoOpenValue =");
        stringBuilder.append(enable);
        Log.w(str, stringBuilder.toString());
        System.putInt(this.mContext.getContentResolver(), MessageUtil.WIFIPRO_AUTO_OPEN_STATE, enable);
    }

    private boolean getAutoOpenValue() {
        int value = System.getInt(this.mContext.getContentResolver(), MessageUtil.WIFIPRO_AUTO_OPEN_STATE, 0);
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAutoOpenValue  value = ");
        stringBuilder.append(value);
        Log.w(str, stringBuilder.toString());
        return false;
    }

    private List<APInfoData> removeFromBlackList(List<APInfoData> datas) {
        ArrayList<APInfoData> result = new ArrayList();
        for (APInfoData data : datas) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeFromBlackList ssid = ");
            stringBuilder.append(data.getSsid());
            stringBuilder.append(", isInBlackList = ");
            stringBuilder.append(data.isInBlackList());
            stringBuilder.append(", isHomeAp = ");
            stringBuilder.append(data.isHomeAp());
            stringBuilder.append(", mIsOversea = ");
            stringBuilder.append(this.mIsOversea);
            Log.d(str, stringBuilder.toString());
            if (!data.isInBlackList() && (data.isHomeAp() || this.mIsOversea)) {
                result.add(data);
            }
        }
        return result;
    }

    private boolean getSettingSwitchType() {
        Log.w(MessageUtil.TAG, "getSettingSwitchType in");
        int select = System.getInt(this.mContext.getContentResolver(), MessageUtil.WIFI_CONNECT_TYPE, 0);
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSettingSwitchType select = ");
        stringBuilder.append(select);
        Log.w(str, stringBuilder.toString());
        if (select == 1) {
            return false;
        }
        return true;
    }

    public List<APInfoData> getTargetApInfoDatas() {
        return this.mTargetApInfoDatas;
    }

    private void autoDisbleWiFi() {
        Log.e(MessageUtil.TAG, "autoDisbleWiFi close WIFI");
        this.mIsAutoClose = true;
        setAutoOpenValue(false);
        this.mWifiManager.setWifiEnabled(false);
    }

    public synchronized void onStart() {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStart mIsMachineStared = ");
        stringBuilder.append(this.mIsMachineStared);
        Log.e(str, stringBuilder.toString());
        if (!this.mIsMachineStared) {
            initPunishParameter();
            this.mIsMachineStared = true;
            getHandler().sendEmptyMessage(100);
            this.mApInfoManager.start();
            this.mWiFiStateMonitor.startMonitor();
            this.mCellStateMonitor.startMonitor();
            this.mIsOversea = useOperatorOverSea();
        }
    }

    public synchronized void onStop() {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStop mIsMachineStared = ");
        stringBuilder.append(this.mIsMachineStared);
        Log.e(str, stringBuilder.toString());
        if (this.mIsMachineStared) {
            this.mIsMachineStared = false;
            this.mWiFiStateMonitor.stopMonitor();
            this.mCellStateMonitor.stopMonitor();
            this.mApInfoManager.stop();
            getHandler().sendEmptyMessage(101);
        }
    }

    public int getConnectFailedReason() {
        return this.mConnectFailedReason;
    }

    public String getConnectFailedBssid() {
        return this.mConnectFailedBssid;
    }

    public String getConnectFailedSsid() {
        return this.mConnectFailedSsid;
    }

    private void requestLocationInfo() {
        Log.d(MessageUtil.TAG, "requestLocationInfo enter");
        if (this.mHomeAddress == null) {
            Log.d(MessageUtil.TAG, "requestLocationInfo mHomeAddress is null , need getLastHomeAddress");
            this.mHomeAddress = this.mHomeAddressManager.getLastHomeAddress();
        }
        if (this.mHomeAddress != null) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestLocationInfo mHomeAddress isInvalid = ");
            stringBuilder.append(this.mHomeAddress.isInvalid());
            stringBuilder.append(", isOversea = ");
            stringBuilder.append(this.mHomeAddress.isOversea());
            Log.d(str, stringBuilder.toString());
            if (!(this.mHomeAddress.isInvalid() || this.mHomeAddress.isOversea())) {
                this.mHomeAddressManager.setHomeDistanceCallback(this.mHomeAddress);
            }
        }
        Log.d(MessageUtil.TAG, "requestLocationInfo exit");
    }

    public boolean isLocationAvaliable(LocationAddress location) {
        return (location == null || location.isInvalid() || location.isOversea() || System.currentTimeMillis() - location.getUpdateTime() >= 30000) ? false : true;
    }

    private boolean useOperatorOverSea() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        if (operator == null || operator.length() <= 0) {
            if ("CN".equalsIgnoreCase(WifiProCommonUtils.getProductLocale())) {
                return false;
            }
        } else if (operator.startsWith(COUNTRY_CODE_CN)) {
            return false;
        }
        return true;
    }

    private void setPingpongPunishTime() {
        if (!this.mApInfoManager.isScaning()) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPingpongPunishTime mLastCellChangeScanTime = ");
            stringBuilder.append(this.mLastCellChangeScanTime);
            Log.e(str, stringBuilder.toString());
            if (this.mLastCellChangeScanTime == 0) {
                this.mLastCellChangeScanTime = System.currentTimeMillis();
                return;
            }
            if (System.currentTimeMillis() - this.mLastCellChangeScanTime < 5000) {
                Log.e(MessageUtil.TAG, "setPingpongPunishTime is inPunish time");
                if (this.mLastScanPingpongTime == 0) {
                    this.mScanPingpongNum = 1;
                    this.mLastScanPingpongTime = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - this.mLastScanPingpongTime > 1800000) {
                        this.mScanPingpongNum = 1;
                    } else {
                        this.mScanPingpongNum++;
                    }
                    str = MessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setPingpongPunishTime mScanPingpongNum = ");
                    stringBuilder.append(this.mScanPingpongNum);
                    Log.e(str, stringBuilder.toString());
                }
            } else {
                Log.e(MessageUtil.TAG, "setPingpongPunishTime is not inPunish time");
            }
            this.mLastCellChangeScanTime = System.currentTimeMillis();
        }
    }

    private boolean isInPingpongPunishTime() {
        String str = MessageUtil.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInPingpongPunishTime mScanPingpongNum = ");
        stringBuilder.append(this.mScanPingpongNum);
        Log.e(str, stringBuilder.toString());
        int punishTime = this.mScanPingpongNum * 30000;
        if (!this.mIsOversea && punishTime > 60000) {
            punishTime = 60000;
        } else if (punishTime > 300000) {
            punishTime = 300000;
        }
        if (System.currentTimeMillis() - this.mLastScanPingpongTime < ((long) punishTime)) {
            String str2 = MessageUtil.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isInPingpongPunishTime punishTime = ");
            stringBuilder2.append(punishTime);
            Log.e(str2, stringBuilder2.toString());
            return true;
        }
        Log.e(MessageUtil.TAG, "isInPingpongPunishTime is not in punishTime");
        return false;
    }

    public void initPunishParameter() {
        this.mScanPingpongNum = 1;
        this.mLastCellChangeScanTime = 0;
        this.mLastScanPingpongTime = 0;
    }

    public CellStateMonitor getCellStateMonitor() {
        return this.mCellStateMonitor;
    }
}

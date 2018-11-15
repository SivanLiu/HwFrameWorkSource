package com.android.server.wifi.HwQoE;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import com.android.server.wifi.wifipro.WifiHandover;
import com.android.server.wifi.wifipro.WifiProStateMachine;
import java.util.List;

public class HwQoEWeChatBooster {
    public static final int KOG_UDP_TYPE = 17;
    private static final int MSG_APP_STATE_CHANHED = 4;
    private static final int MSG_CONNECTIVITY_ACTION = 6;
    private static final int MSG_WIFI_CONNECTED = 2;
    private static final int MSG_WIFI_DELAY_DISCONNECT = 5;
    public static final int MSG_WIFI_DELAY_DISCONNECT_TIMER = 4000;
    private static final int MSG_WIFI_DISCONNECTED = 3;
    private static final int MSG_WIFI_RSSI_CHANHED = 1;
    private static final int MSG_WIFI_STATE_DISABLED = 7;
    private static final int MSG_WIFI_STATE_ENABLED = 8;
    private static final String TAG = "HiDATA_WeChatBooster";
    public static final int WECHART_AUDIO_THRESHOLD = 30;
    public static final int WECHART_DISABLE_FUNC = 0;
    public static final int WECHART_ENABLE_FUNC = 1;
    public static final int WECHART_NETWORK_CELLULAR = 0;
    public static final int WECHART_NETWORK_RTT_THRESHOLD = 300;
    public static final int WECHART_NETWORK_UNKNOW = -1;
    public static final int WECHART_NETWORK_WIFI = 1;
    public static final int WECHART_POOR_RSSI_LEVEL = 2;
    private static final int WECHART_PW_INIT = 5;
    private static final int WECHART_PW_START = 4;
    private static final int WECHART_PW_STOP = 3;
    public static final int WECHART_VIDEO_THRESHOLD = 120;
    private static final String WECHAT_NAME = "com.tencent.mm";
    private static HwQoEWeChatBooster mHwQoEWeChatBooster;
    private boolean isNetworkChecking = false;
    private boolean isNetworkHaveInternet = false;
    private boolean isPoorNetwork = false;
    private int isPoorNetworkNum = 0;
    private NetworkInfo mActiveNetworkInfo;
    private int mApAuthType;
    private int mApBlackType;
    private AvailabilityCallback mAvailabilityCallback = new AvailabilityCallback() {
        public void onCameraAvailable(String cameraId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCameraAvailable cameraId = ");
            stringBuilder.append(cameraId);
            HwQoEUtils.logD(stringBuilder.toString());
            if (cameraId.equals(HwQoEWeChatBooster.this.mCameraId)) {
                HwQoEWeChatBooster.this.mCameraId = "none";
            }
        }

        public void onCameraUnavailable(String cameraId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCameraUnavailable cameraId = ");
            stringBuilder.append(cameraId);
            HwQoEUtils.logD(stringBuilder.toString());
            HwQoEWeChatBooster.this.mCameraId = cameraId;
        }
    };
    private BroadcastReceiver mBroadcastReceiver;
    private String mCameraId = "none";
    private CameraManager mCameraManager;
    private long mCheckStartTime = 0;
    private List<HwQoEQualityInfo> mConnectedRecordList;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private int mCurrRssiLevel = -1;
    private String mCurrSSID;
    private int mCurrWeChatType = 0;
    private HiDataApBlackWhiteListManager mHiDataApBlackWhiteListManager;
    private HiDataUtilsManager mHiDataUtilsManager;
    private HidataWechatTraffic mHidataWechatTraffic;
    private HwQoEContentAware mHwQoEContentAware;
    private HwQoEQualityManager mHwQoEQualityManager;
    private HwQoEWiFiOptimization mHwQoEWiFiOptimization;
    private Handler mHwWeChatBoosterHandler;
    private HwWifiCHRService mHwWifiCHRService;
    private IntentFilter mIntentFilter;
    private boolean mIsCheckingRtt = false;
    private int mIsHandoverRSSI = 0;
    private boolean mIsHandoverToMobile = false;
    private boolean mIsUserManualConnectSuccess;
    private boolean mIsUserManualOpenSuccess;
    private boolean mIsVerifyState = false;
    private boolean mIsWeCharting = false;
    private NetworkInfo mNetworkInfo;
    private boolean mPhaseTwoIsEnable = false;
    private int mUserType;
    private int mWeChartUID = -1;
    private boolean mWeChatHoldWifi;
    private long mWeChatStartTime;
    private int mWechatNetwork = -1;
    private WifiInfo mWifiInfo;
    private WifiManager mWifiManager;

    public static HwQoEWeChatBooster createInstance(Context context) {
        if (mHwQoEWeChatBooster == null) {
            mHwQoEWeChatBooster = new HwQoEWeChatBooster(context);
        }
        return mHwQoEWeChatBooster;
    }

    public static HwQoEWeChatBooster getInstance() {
        return mHwQoEWeChatBooster;
    }

    private HwQoEWeChatBooster(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        initHwWeChatBoosterHandler();
        registerBroadcastReceiver();
        this.mHwQoEWiFiOptimization = HwQoEWiFiOptimization.getInstance(this.mContext);
        this.mHwQoEContentAware = HwQoEContentAware.getInstance();
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, null);
        this.mHwQoEQualityManager = HwQoEQualityManager.getInstance(this.mContext);
        this.mHidataWechatTraffic = new HidataWechatTraffic(this.mContext);
        this.mHiDataApBlackWhiteListManager = HiDataApBlackWhiteListManager.createInstance(this.mContext);
        this.mHiDataUtilsManager = HiDataUtilsManager.getInstance(this.mContext);
    }

    private void registerBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    HwQoEWeChatBooster.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (HwQoEWeChatBooster.this.mNetworkInfo == null) {
                        return;
                    }
                    if (HwQoEWeChatBooster.this.mNetworkInfo.getState() == State.DISCONNECTED) {
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(3);
                        HwQoEWeChatBooster.this.mIsVerifyState = false;
                    } else if (HwQoEWeChatBooster.this.mNetworkInfo.getState() == State.CONNECTED) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("DetailedState: ");
                        stringBuilder.append(HwQoEWeChatBooster.this.mNetworkInfo.getDetailedState());
                        HwQoEUtils.logD(stringBuilder.toString());
                        if (HwQoEWeChatBooster.this.mNetworkInfo.getDetailedState() == DetailedState.VERIFYING_POOR_LINK) {
                            HwQoEWeChatBooster.this.mIsVerifyState = true;
                            return;
                        }
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(2);
                        HwQoEWeChatBooster.this.mIsVerifyState = false;
                    }
                } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                    HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(1);
                } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                    HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(6);
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    int wifistatue = intent.getIntExtra("wifi_state", 4);
                    if (1 == wifistatue) {
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(7);
                    } else if (3 == wifistatue) {
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(8);
                    }
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    private void initHwWeChatBoosterHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_wechatbooster_handler_thread");
        handlerThread.start();
        this.mHwWeChatBoosterHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i != HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT) {
                    if (i != HwQoEUtils.QOE_MSG_UPDATE_QUALITY_INFO) {
                        switch (i) {
                            case 1:
                                HwQoEWeChatBooster.this.handleWifiRssiChanged();
                                break;
                            case 2:
                                HwQoEWeChatBooster.this.handleWifiConnected();
                                break;
                            case 3:
                                HwQoEWeChatBooster.this.handleWifiDisConnected();
                                break;
                            case 4:
                                i = msg.arg1;
                                int state = msg.arg2;
                                boolean isBackground = ((Boolean) msg.obj).booleanValue();
                                HwQoEWeChatBooster.this.mWeChartUID = i;
                                if (1 != state) {
                                    if (state != 0) {
                                        if (2 == state) {
                                            HwQoEWeChatBooster hwQoEWeChatBooster = HwQoEWeChatBooster.this;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("WECHAT_BACK_GROUND_CHANG  mIsWeCharting = ");
                                            stringBuilder.append(HwQoEWeChatBooster.this.mIsWeCharting);
                                            stringBuilder.append(" isBackground = ");
                                            stringBuilder.append(isBackground);
                                            hwQoEWeChatBooster.logD(stringBuilder.toString());
                                            if (HwQoEWeChatBooster.this.mIsWeCharting) {
                                                if (!isBackground) {
                                                    HwQoEWeChatBooster.this.startWiFiOptimization();
                                                    break;
                                                } else {
                                                    HwQoEWeChatBooster.this.stopWiFiOptimization();
                                                    break;
                                                }
                                            }
                                            return;
                                        }
                                    }
                                    HwQoEWeChatBooster.this.mIsUserManualOpenSuccess = false;
                                    HwQoEWeChatBooster.this.mIsWeCharting = false;
                                    if (HwQoEWeChatBooster.this.mWechatNetwork == 1) {
                                        HwQoEWeChatBooster hwQoEWeChatBooster2 = HwQoEWeChatBooster.this;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("WeChat phone trun off in wifi,holdWifi :");
                                        stringBuilder2.append(HwQoEWeChatBooster.this.mWeChatHoldWifi);
                                        hwQoEWeChatBooster2.logD(stringBuilder2.toString());
                                        if (HwQoEWeChatBooster.this.mWeChatHoldWifi && System.currentTimeMillis() - HwQoEWeChatBooster.this.mWeChatStartTime > HidataWechatTraffic.MIN_VALID_TIME) {
                                            HwQoEWeChatBooster.this.mHiDataApBlackWhiteListManager.updateHoldWiFiCounter(HwQoEWeChatBooster.this.mCurrSSID, HwQoEWeChatBooster.this.mApAuthType, HwQoEWeChatBooster.this.mCurrWeChatType);
                                        }
                                        HwQoEWeChatBooster.this.stopWiFiOptimization();
                                        HwQoEWeChatBooster.this.updateAppQualityRecords(HwQoEWeChatBooster.this.mConnectedRecordList);
                                    } else if (HwQoEWeChatBooster.this.mWechatNetwork == 0) {
                                        HwQoEWeChatBooster.this.logD("WeChat phone trun off in celluar");
                                        HwQoEWeChatBooster.this.mHidataWechatTraffic.updateMobileWechatStateChanged(0, 2, HwQoEWeChatBooster.this.mWeChartUID);
                                    } else if (-1 == HwQoEWeChatBooster.this.mWechatNetwork) {
                                        HwQoEWeChatBooster.this.logD("WeChat phone trun off ");
                                    }
                                    HwQoEWeChatBooster.this.mCurrWeChatType = 0;
                                    HwQoEWeChatBooster.this.mWeChatHoldWifi = false;
                                    if (HwQoEWeChatBooster.this.mIsHandoverToMobile) {
                                        WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiLinkPoor(false);
                                        HwQoEWeChatBooster.this.mIsHandoverToMobile = false;
                                        HwQoEWeChatBooster.this.mIsHandoverRSSI = 0;
                                        break;
                                    }
                                }
                                if (HwQoEWeChatBooster.this.mWechatNetwork == 1) {
                                    HwQoEWeChatBooster.this.logD("WeChat phone trun on in wifi");
                                    HwQoEWeChatBooster.this.mWeChatHoldWifi = true;
                                    HwQoEWeChatBooster.this.mWeChatStartTime = System.currentTimeMillis();
                                    HwQoEWeChatBooster.this.startWiFiOptimization();
                                } else if (HwQoEWeChatBooster.this.mWechatNetwork == 0) {
                                    HwQoEWeChatBooster.this.mWeChatHoldWifi = false;
                                    HwQoEWeChatBooster.this.logD("WeChat phone trun on in celluar");
                                }
                                HwQoEWeChatBooster.this.mIsWeCharting = true;
                                if (HwQoEWeChatBooster.this.isCameraOn()) {
                                    HwQoEWeChatBooster.this.logD("WeChat video call");
                                    HwQoEWeChatBooster.this.mCurrWeChatType = 1;
                                    HwQoEWeChatBooster.this.mHwWifiCHRService.setWeChatScene(1);
                                    if (HwQoEWeChatBooster.this.mWechatNetwork == 0) {
                                        HwQoEWeChatBooster.this.mHidataWechatTraffic.updateMobileWechatStateChanged(1, 1, HwQoEWeChatBooster.this.mWeChartUID);
                                    }
                                } else {
                                    HwQoEWeChatBooster.this.logD("WeChat audio call");
                                    HwQoEWeChatBooster.this.mCurrWeChatType = 2;
                                    HwQoEWeChatBooster.this.mHwWifiCHRService.setWeChatScene(2);
                                    if (HwQoEWeChatBooster.this.mWechatNetwork == 0) {
                                        HwQoEWeChatBooster.this.mHidataWechatTraffic.updateMobileWechatStateChanged(1, 2, HwQoEWeChatBooster.this.mWeChartUID);
                                    }
                                }
                                HwQoEWeChatBooster.this.handleWechatTurnOn(HwQoEWeChatBooster.this.mWechatNetwork);
                                break;
                                break;
                            case 5:
                                HwQoEUtils.logD("wechat MSG_WIFI_DELAY_DISCONNECT");
                                HwQoEService.getInstance().disconWiFiNetwork();
                                break;
                            case 6:
                                HwQoEWeChatBooster.this.handleConnectivityNetworkChange();
                                break;
                            case 7:
                                HwQoEWeChatBooster.this.mIsUserManualOpenSuccess = false;
                                HwQoEWeChatBooster hwQoEWeChatBooster3 = HwQoEWeChatBooster.this;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("WIFI_STATE_DISABLED: CurrRssiLevel= ");
                                stringBuilder3.append(HwQoEWeChatBooster.this.mCurrRssiLevel);
                                stringBuilder3.append(" , WeCharting: ");
                                stringBuilder3.append(HwQoEWeChatBooster.this.mIsWeCharting);
                                hwQoEWeChatBooster3.logD(stringBuilder3.toString());
                                if (HwQoEWeChatBooster.this.mCurrRssiLevel > 2 && HwQoEWeChatBooster.this.mIsWeCharting) {
                                    HwQoEWeChatBooster.this.mHiDataApBlackWhiteListManager.addHandoverBlackList(HwQoEWeChatBooster.this.mCurrSSID, HwQoEWeChatBooster.this.mApAuthType, HwQoEWeChatBooster.this.mCurrWeChatType);
                                    break;
                                }
                            case 8:
                                HwQoEWeChatBooster.this.mIsUserManualOpenSuccess = HwQoEWeChatBooster.this.mIsWeCharting;
                                break;
                            default:
                                switch (i) {
                                    case 103:
                                        HwQoEUtils.logD("wechat QOE_MSG_MONITOR_HAVE_INTERNET");
                                        if (HwQoEWeChatBooster.this.isNetworkChecking) {
                                            HwQoEWeChatBooster.this.isNetworkChecking = false;
                                            removeMessages(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT);
                                        }
                                        if (HwQoEWeChatBooster.this.mIsCheckingRtt) {
                                            HwQoEWeChatBooster.this.handlerCheckResult();
                                            break;
                                        }
                                        break;
                                    case 104:
                                        break;
                                }
                                break;
                        }
                    }
                    HwQoEWeChatBooster.this.onUpdateQualityInfo((long) msg.arg1, (long) msg.arg2);
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("wechat QOE_MSG_MONITOR_NO_INTERNET isNetworkChecking = ");
                stringBuilder4.append(HwQoEWeChatBooster.this.isNetworkChecking);
                stringBuilder4.append(" mIsCheckingRtt = ");
                stringBuilder4.append(HwQoEWeChatBooster.this.mIsCheckingRtt);
                HwQoEUtils.logD(stringBuilder4.toString());
                if (HwQoEWeChatBooster.this.isNetworkChecking) {
                    HwQoEWeChatBooster.this.isNetworkHaveInternet = false;
                    HwQoEWeChatBooster.this.isNetworkChecking = false;
                    HwQoEWeChatBooster.this.handlerNoInternet();
                }
                if (HwQoEWeChatBooster.this.mIsCheckingRtt) {
                    HwQoEWeChatBooster.this.handlerCheckResult();
                }
            }
        };
    }

    public void updateWifiConnectionMode(boolean isUserManualConnect, boolean isUserHandoverWiFi) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateWifiConnectionMode, manualConnect : ");
        stringBuilder.append(isUserManualConnect);
        stringBuilder.append(" , handoverWiFi: ");
        stringBuilder.append(isUserHandoverWiFi);
        logD(stringBuilder.toString());
        boolean z = isUserManualConnect || isUserHandoverWiFi;
        this.mIsUserManualConnectSuccess = z;
    }

    private void onUpdateQualityInfo(long outSpeed, long inSpeed) {
        if (this.mIsWeCharting && this.mWechatNetwork == 1) {
            int targetSpeed;
            int type;
            int poorTargetNum;
            WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
            int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEService: onUpdateQualityInfo  rssiLevel = ");
            stringBuilder.append(rssiLevel);
            stringBuilder.append(" wifiInfo.getRssi()");
            stringBuilder.append(wifiInfo.getRssi());
            stringBuilder.append(" outSpeed = ");
            stringBuilder.append(outSpeed);
            stringBuilder.append(" inSpeed = ");
            stringBuilder.append(inSpeed);
            HwQoEUtils.logD(stringBuilder.toString());
            if (isCameraOn()) {
                targetSpeed = 120;
                type = 1;
                poorTargetNum = 2;
            } else {
                targetSpeed = 30;
                type = 2;
                poorTargetNum = 4;
            }
            setWechatCallType(type);
            if (rssiLevel <= 2 && inSpeed > 0 && inSpeed < ((long) targetSpeed)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("HwQoEService: onUpdateQualityInfo  isPoorNetworkNum = ");
                stringBuilder2.append(this.isPoorNetworkNum);
                stringBuilder2.append(" poorTargetNum = ");
                stringBuilder2.append(poorTargetNum);
                HwQoEUtils.logD(stringBuilder2.toString());
                this.isPoorNetworkNum++;
                if (this.isPoorNetworkNum >= poorTargetNum) {
                    this.isPoorNetwork = true;
                    startNetworkCheckRtt();
                }
                this.isNetworkHaveInternet = true;
            } else if (inSpeed > ((long) targetSpeed)) {
                this.isPoorNetwork = false;
                this.isNetworkHaveInternet = true;
                this.isPoorNetworkNum = 0;
            } else if (inSpeed == 0) {
                startNetworkChecking();
            }
            if (this.isNetworkHaveInternet && rssiLevel == 2) {
                updateAPPQuality(wifiInfo, inSpeed, type);
            }
        }
    }

    private void setHighPriorityTransmit(int uid, int enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwQoEService: setGameKOGHighPriorityTransmit uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" enable: ");
        stringBuilder.append(enable);
        HwQoEUtils.logD(stringBuilder.toString());
        this.mHwQoEWiFiOptimization.hwQoEHighPriorityTransmit(uid, 17, enable);
    }

    private void setPSAndRetryMode(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwQoEService: setPSAndRetryMode  enable: ");
        stringBuilder.append(enable);
        HwQoEUtils.logD(stringBuilder.toString());
        if (enable) {
            this.mPhaseTwoIsEnable = true;
            this.mHwQoEWiFiOptimization.hwQoESetMode(5);
            this.mHwQoEWiFiOptimization.hwQoESetMode(4);
            return;
        }
        this.mPhaseTwoIsEnable = false;
        this.mHwQoEWiFiOptimization.hwQoESetMode(3);
    }

    private void setLimitedSpeed(int enable, int mode) {
        this.mHwQoEWiFiOptimization.hwQoELimitedSpeed(enable, mode);
    }

    private void setTXPower(int enable) {
        this.mHwQoEWiFiOptimization.setTXPower(enable);
    }

    private boolean isCameraOn() {
        if (this.mCameraId == null || this.mCameraId.equals("none")) {
            return false;
        }
        return true;
    }

    private boolean getMoblieDateSettings() {
        return getSettingsGlobalBoolean(this.mContext.getContentResolver(), "mobile_data", false);
    }

    private boolean getSettingsGlobalBoolean(ContentResolver cr, String name, boolean def) {
        return Global.getInt(cr, name, def) == 1;
    }

    private void setAppStateMonitor(int network) {
        if (this.mHwQoEContentAware != null) {
            this.mHwQoEContentAware.setAppStateMonitorEnabled(true, WECHAT_NAME, network);
        }
        if (!this.mHwWeChatBoosterHandler.hasMessages(5)) {
            this.mHwWeChatBoosterHandler.removeMessages(5);
        }
    }

    private void updateAPPQuality(WifiInfo wifiInfo, long inSpeed, int type) {
        HwQoEQualityInfo curRecord = null;
        if (this.mConnectedRecordList == null || inSpeed == 0) {
            HwQoEUtils.logE("updateAPPQuality error");
            return;
        }
        for (HwQoEQualityInfo record : this.mConnectedRecordList) {
            if (record.mRSSI == wifiInfo.getRssi() && record.mAPPType == type) {
                curRecord = record;
            }
        }
        if (curRecord != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("curRecord.mThoughtput = ");
            stringBuilder.append(curRecord.mThoughtput);
            stringBuilder.append(" inSpeed = ");
            stringBuilder.append(inSpeed);
            HwQoEUtils.logD(stringBuilder.toString());
            curRecord.mThoughtput = ((curRecord.mThoughtput * 8) / 10) + ((2 * inSpeed) / 10);
            stringBuilder = new StringBuilder();
            stringBuilder.append("curRecord.mThoughtput = ");
            stringBuilder.append(curRecord.mThoughtput);
            HwQoEUtils.logD(stringBuilder.toString());
        } else {
            HwQoEUtils.logD("updateAPPQuality add record");
            curRecord = new HwQoEQualityInfo();
            curRecord.mBSSID = wifiInfo.getBSSID();
            curRecord.mRSSI = wifiInfo.getRssi();
            curRecord.mAPPType = type;
            curRecord.mThoughtput = inSpeed;
            this.mConnectedRecordList.add(curRecord);
        }
    }

    private void updateAppQualityRecords(List<HwQoEQualityInfo> records) {
        if (records != null && records.size() != 0) {
            for (HwQoEQualityInfo record : records) {
                this.mHwQoEQualityManager.addOrUpdateAppQualityRcd(record);
            }
        }
    }

    private void startNetworkChecking() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startNetworkChecking  isNetworkChecking = ");
        stringBuilder.append(this.isNetworkChecking);
        stringBuilder.append(" isNetworkHaveInternet= ");
        stringBuilder.append(this.isNetworkHaveInternet);
        HwQoEUtils.logD(stringBuilder.toString());
        if (!this.isNetworkChecking && this.isNetworkHaveInternet) {
            this.isNetworkHaveInternet = false;
            this.isNetworkChecking = true;
            new HwQoENetworkChecker(this.mContext, this.mHwWeChatBoosterHandler).start();
            this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
        }
    }

    private void handlerNoInternet() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handlerNoInternet rssiLevel = ");
        stringBuilder.append(rssiLevel);
        HwQoEUtils.logD(stringBuilder.toString());
        this.mIsHandoverToMobile = true;
        WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiLinkPoor(true);
    }

    private void handleConnectivityNetworkChange() {
        this.mActiveNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (this.mActiveNetworkInfo != null && this.mActiveNetworkInfo.getType() == 0 && this.mActiveNetworkInfo.isConnected()) {
            this.mWechatNetwork = 0;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TYPE_MOBILE is Connected ,WeCharting:");
            stringBuilder.append(this.mIsWeCharting);
            logD(stringBuilder.toString());
            setAppStateMonitor(this.mWechatNetwork);
            if (this.mIsWeCharting) {
                this.mHidataWechatTraffic.updateMobileWechatStateChanged(1, this.mCurrWeChatType, this.mWeChartUID);
            }
        }
    }

    private void handleWifiRssiChanged() {
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mWifiInfo != null) {
            this.mCurrRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.mWifiInfo.getFrequency(), this.mWifiInfo.getRssi());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rssi changed,rssiLevel = ");
            stringBuilder.append(this.mCurrRssiLevel);
            HwQoEUtils.logD(stringBuilder.toString());
            if (this.mIsWeCharting && this.mWechatNetwork == 1) {
                if (this.mCurrRssiLevel <= 2 && !this.mPhaseTwoIsEnable) {
                    setPSAndRetryMode(true);
                    setTXPower(1);
                }
                if (this.mCurrRssiLevel >= 4 && this.mPhaseTwoIsEnable) {
                    setPSAndRetryMode(false);
                    setTXPower(0);
                }
            }
        }
    }

    private void handleWifiConnected() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleWifiConnected, ManualConnect:");
        stringBuilder.append(this.mIsUserManualConnectSuccess);
        stringBuilder.append(",WeCharting:");
        stringBuilder.append(this.mIsWeCharting);
        stringBuilder.append(", ManualOpen: ");
        stringBuilder.append(this.mIsUserManualOpenSuccess);
        logD(stringBuilder.toString());
        this.mWechatNetwork = 1;
        setAppStateMonitor(this.mWechatNetwork);
        this.mApAuthType = this.mHiDataUtilsManager.isPublicAP();
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mWifiInfo != null) {
            this.mCurrRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.mWifiInfo.getFrequency(), this.mWifiInfo.getRssi());
            this.mCurrSSID = this.mWifiInfo.getSSID();
            getAllWeChatData();
        }
        this.isPoorNetwork = false;
        if (this.mIsWeCharting && (this.mIsUserManualConnectSuccess || this.mIsUserManualOpenSuccess)) {
            this.mHiDataApBlackWhiteListManager.addHandoverWhiteList(this.mCurrSSID, this.mApAuthType, this.mCurrWeChatType);
        }
        if (this.mIsWeCharting) {
            this.mHidataWechatTraffic.updateMobileWechatStateChanged(0, this.mCurrWeChatType, this.mWeChartUID);
        }
    }

    private void handleWechatTurnOn(int network) {
        int i = 1;
        if (network == 1) {
            this.mApAuthType = this.mHiDataUtilsManager.isPublicAP();
            if (!this.mHidataWechatTraffic.wechatTrafficWealthy(this.mCurrWeChatType)) {
                i = 0;
            }
            this.mUserType = i;
            this.mApBlackType = this.mHiDataApBlackWhiteListManager.getApBlackType(this.mCurrSSID, this.mApAuthType, this.mCurrWeChatType, this.mUserType);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleWechatTurnOn, mApAuthType = ");
            stringBuilder.append(this.mApAuthType);
            stringBuilder.append(", mUserType = ");
            stringBuilder.append(this.mUserType);
            stringBuilder.append(", mApBlackType = ");
            stringBuilder.append(this.mApBlackType);
            logD(stringBuilder.toString());
        }
    }

    private void handleWifiDisConnected() {
        this.mWifiInfo = null;
        this.isPoorNetwork = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleWifiDisConnected  mIsWeCharting = ");
        stringBuilder.append(this.mIsWeCharting);
        stringBuilder.append(" mWechatNetwork = ");
        stringBuilder.append(this.mWechatNetwork);
        logD(stringBuilder.toString());
        this.mWeChatHoldWifi = false;
        if (this.mIsWeCharting && this.mWechatNetwork == 1) {
            setHighPriorityTransmit(this.mWeChartUID, 0);
            setLimitedSpeed(0, 1);
            if (this.mPhaseTwoIsEnable) {
                setPSAndRetryMode(false);
                setTXPower(0);
            }
            this.mWechatNetwork = -1;
            updateAppQualityRecords(this.mConnectedRecordList);
            this.mConnectedRecordList = null;
        }
        if (getMoblieDateSettings()) {
            this.mWechatNetwork = 0;
        } else {
            this.mHwQoEContentAware.setAppStateMonitorEnabled(false, WECHAT_NAME, -1);
        }
    }

    private void getAllWeChatData() {
        List<HwQoEQualityInfo> videoData = this.mHwQoEQualityManager.getAppQualityAllRcd(this.mWifiInfo.getBSSID(), 1);
        List<HwQoEQualityInfo> audioData = this.mHwQoEQualityManager.getAppQualityAllRcd(this.mWifiInfo.getBSSID(), 2);
        this.mConnectedRecordList = videoData;
        this.mConnectedRecordList.addAll(audioData);
    }

    private void setWechatCallType(int type) {
        if (!(this.mCurrWeChatType == type || type == 0)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWechatCallType mCurrWeChatType = ");
            stringBuilder.append(this.mCurrWeChatType);
            HwQoEUtils.logD(stringBuilder.toString());
        }
        this.mCurrWeChatType = type;
    }

    private void startNetworkCheckRtt() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startNetworkChecking  isNetworkChecking = ");
        stringBuilder.append(this.isNetworkChecking);
        stringBuilder.append(" isNetworkHaveInternet= ");
        stringBuilder.append(this.isNetworkHaveInternet);
        HwQoEUtils.logD(stringBuilder.toString());
        this.mCheckStartTime = System.currentTimeMillis();
        HwQoENetworkChecker mHwQoENetworkChecker = new HwQoENetworkChecker(this.mContext, this.mHwWeChatBoosterHandler);
        this.mIsCheckingRtt = true;
        mHwQoENetworkChecker.start();
        this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT, 1000);
    }

    private void handlerCheckResult() {
        StringBuilder stringBuilder;
        long time = System.currentTimeMillis() - this.mCheckStartTime;
        long arpTime = this.mHiDataUtilsManager.checkARPRTT();
        if (this.isPoorNetwork) {
            if (time > 300) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("network is bad mIsVerifyState = ");
                stringBuilder.append(this.mIsVerifyState);
                HwQoEUtils.logD(stringBuilder.toString());
                if (this.mHiDataUtilsManager.isMobileNetworkReady(this.mCurrWeChatType) && !this.mIsVerifyState) {
                    this.mWifiInfo = this.mWifiManager.getConnectionInfo();
                    if (this.mWifiInfo != null) {
                        this.mIsHandoverRSSI = this.mWifiInfo.getRssi();
                    }
                    this.mIsHandoverToMobile = true;
                    WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiLinkPoor(true);
                }
            }
            this.isPoorNetwork = false;
            this.isPoorNetworkNum = 0;
        }
        this.mIsCheckingRtt = false;
        this.mCheckStartTime = 0;
        stringBuilder = new StringBuilder();
        stringBuilder.append("handlerCheckResult time = ");
        stringBuilder.append(time);
        stringBuilder.append(" arpTime = ");
        stringBuilder.append(arpTime);
        HwQoEUtils.logD(stringBuilder.toString());
    }

    public void onSensitiveAppStateChange(int uid, int state, boolean isBackground) {
        this.mHwWeChatBoosterHandler.sendMessage(this.mHwWeChatBoosterHandler.obtainMessage(4, uid, state, Boolean.valueOf(isBackground)));
    }

    public void onPeriodSpeed(long outSpeed, long inSpeed) {
        this.mHwWeChatBoosterHandler.sendMessage(this.mHwWeChatBoosterHandler.obtainMessage(HwQoEUtils.QOE_MSG_UPDATE_QUALITY_INFO, (int) outSpeed, (int) inSpeed, null));
    }

    public boolean isConnectWhenWeChating(ScanResult scanResult) {
        int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(scanResult.frequency, scanResult.level);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isWeChating mIsWeCharting = ");
        stringBuilder.append(this.mIsWeCharting);
        stringBuilder.append(" scanResult.level = ");
        stringBuilder.append(scanResult.level);
        stringBuilder.append("  mIsHandoverRSSI = ");
        stringBuilder.append(this.mIsHandoverRSSI);
        HwQoEUtils.logD(stringBuilder.toString());
        if (!this.mIsWeCharting) {
            return true;
        }
        if (rssiLevel <= 1) {
            return false;
        }
        if (!this.mIsHandoverToMobile || this.mIsHandoverRSSI == 0) {
            if (rssiLevel >= 3) {
                return true;
            }
            int targetSpeed;
            int type;
            if (isCameraOn()) {
                targetSpeed = 120;
                type = 1;
            } else {
                targetSpeed = 30;
                type = 2;
            }
            for (HwQoEQualityInfo record : this.mHwQoEQualityManager.getAppQualityAllRcd(scanResult.BSSID, type)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("record.mRSSI = ");
                stringBuilder2.append(record.mRSSI);
                stringBuilder2.append(" record.mThoughtput = ");
                stringBuilder2.append(record.mThoughtput);
                HwQoEUtils.logD(stringBuilder2.toString());
                if (record.mRSSI <= scanResult.level && record.mThoughtput > ((long) (targetSpeed + 20))) {
                    return true;
                }
            }
            return false;
        } else if (scanResult.level >= this.mIsHandoverRSSI + 10) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isWeChating() {
        boolean result = this.mIsWeCharting;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isWeChating result = ");
        stringBuilder.append(result);
        stringBuilder.append(" mIsWeCharting = ");
        stringBuilder.append(this.mIsWeCharting);
        stringBuilder.append(" mIsHandoverToMobile = ");
        stringBuilder.append(this.mIsHandoverToMobile);
        HwQoEUtils.logD(stringBuilder.toString());
        return result;
    }

    public boolean isHandoverToMobile() {
        return this.mIsHandoverToMobile;
    }

    private void startWiFiOptimization() {
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        int currRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
        setHighPriorityTransmit(this.mWeChartUID, 1);
        setLimitedSpeed(1, 1);
        if (currRssiLevel <= 2) {
            HwQoEUtils.logD("wechat video on mCurrRssiLevel <=2");
            setPSAndRetryMode(true);
            setTXPower(1);
        }
    }

    private void stopWiFiOptimization() {
        setHighPriorityTransmit(this.mWeChartUID, 0);
        setLimitedSpeed(0, 1);
        if (this.mPhaseTwoIsEnable) {
            setPSAndRetryMode(false);
            setTXPower(0);
        }
    }

    private void logD(String info) {
        Log.d(TAG, info);
    }
}

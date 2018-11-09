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
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.wifi.HwWifiConnectivityMonitor;
import com.android.server.wifi.HwWifiStatStore;
import com.android.server.wifi.HwWifiStatStoreImpl;
import com.android.server.wifi.wifipro.WifiHandover;
import com.android.server.wifi.wifipro.WifiProStateMachine;
import java.util.List;

public class HwQoEWeChatBooster {
    public static final int KOG_UDP_TYPE = 17;
    private static final int MSG_APP_STATE_CHANHED = 4;
    private static final int MSG_CONNECTIVITY_ACTION = 5;
    private static final int MSG_WIFI_COLLECT_HANDOVER_INFO = 9;
    private static final int MSG_WIFI_COLLECT_MACHINE_INFO = 8;
    private static final int MSG_WIFI_CONNECTED = 2;
    private static final int MSG_WIFI_DISCONNECTED = 3;
    private static final int MSG_WIFI_RSSI_CHANHED = 1;
    private static final int MSG_WIFI_STATE_DISABLED = 6;
    private static final int MSG_WIFI_STATE_ENABLED = 7;
    private static final String TAG = "HiDATA_WeChatBooster";
    public static final int WECHART_AUDIO_THRESHOLD = 30;
    public static final int WECHART_COLLECT_HANDOVER_INFO_TIME = 10000;
    public static final int WECHART_COLLECT_MACHINE_INFO_TIME = 30000;
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
    private WifiInfo historyWiFiInfo = null;
    private boolean isNetworkChecking = false;
    private boolean isNetworkHaveInternet = false;
    private int isPoorNetworkARPNum = 0;
    private int isPoorNetworkNum = 0;
    private boolean isUploadMachineInfo = false;
    private int mARPCheckControlNum = 2;
    private NetworkInfo mActiveNetworkInfo;
    private int mApAuthType;
    private int mApBlackType;
    private int mAutoHandoverTimes = 0;
    private AvailabilityCallback mAvailabilityCallback = new AvailabilityCallback() {
        public void onCameraAvailable(String cameraId) {
            HwQoEUtils.logD("onCameraAvailable cameraId = " + cameraId);
            if (cameraId.equals(HwQoEWeChatBooster.this.mCameraId)) {
                HwQoEWeChatBooster.this.mCameraId = "none";
            }
        }

        public void onCameraUnavailable(String cameraId) {
            HwQoEUtils.logD("onCameraUnavailable cameraId = " + cameraId);
            HwQoEWeChatBooster.this.mCameraId = cameraId;
        }
    };
    private BroadcastReceiver mBroadcastReceiver;
    private HiDataCHRStatisticsInfo mCallStatisticsInfo;
    private String mCameraId = "none";
    private CameraManager mCameraManager;
    private List<HwQoEQualityInfo> mConnectedRecordList;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private int mCurrRssiLevel = -1;
    private String mCurrSSID;
    private int mCurrWeChatType = 0;
    private long mFirstArpRTT = 0;
    private long mFirstInSpeed = 0;
    private RssiPacketCountInfo mFirstMachineInfo = null;
    private HiDataApBlackWhiteListManager mHiDataApBlackWhiteListManager;
    private HiDataCHRHandoverInfo mHiDataCHRHandoverInfo;
    private HiDataCHRMachineInfo mHiDataCHRMachineInfo;
    private HiDataCHRManager mHiDataCHRManager;
    private HiDataUtilsManager mHiDataUtilsManager;
    private HidataWechatTraffic mHidataWechatTraffic;
    private HwQoEContentAware mHwQoEContentAware;
    private HwQoEQualityManager mHwQoEQualityManager;
    private HwQoEWiFiOptimization mHwQoEWiFiOptimization;
    private Handler mHwWeChatBoosterHandler;
    private HwWifiStatStore mHwWifiStatStore;
    private IntentFilter mIntentFilter;
    private int mIsHandoverRSSI = 0;
    private boolean mIsHandoverToMobile = false;
    private boolean mIsUserManualCloseSuccess;
    private boolean mIsUserManualConnectSuccess;
    private boolean mIsUserManualOpenSuccess;
    private boolean mIsVerifyState = false;
    private boolean mIsWeCharting = false;
    private long mLastCellularInSpeed = 0;
    private long mLastOutSpeed = 0;
    private NetworkInfo mNetworkInfo;
    private boolean mPhaseTwoIsEnable = false;
    private long mSecInSpeed = 0;
    private RssiPacketCountInfo mSecondMachineInfo = null;
    private TelephonyManager mTelephonyManager;
    private int mUserType;
    private int mWeChartUID = -1;
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
        this.mHwWifiStatStore = HwWifiStatStoreImpl.getDefault();
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, null);
        this.mHwQoEQualityManager = HwQoEQualityManager.getInstance(this.mContext);
        this.mHidataWechatTraffic = new HidataWechatTraffic(this.mContext);
        this.mHiDataApBlackWhiteListManager = HiDataApBlackWhiteListManager.createInstance(this.mContext);
        this.mHiDataUtilsManager = HiDataUtilsManager.getInstance(this.mContext);
        this.mHiDataCHRManager = HiDataCHRManager.getInstance(this.mContext);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
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
                        HwQoEUtils.logD("DetailedState: " + HwQoEWeChatBooster.this.mNetworkInfo.getDetailedState());
                        if (HwQoEWeChatBooster.this.mNetworkInfo.getDetailedState() == DetailedState.VERIFYING_POOR_LINK) {
                            HwQoEWeChatBooster.this.mIsVerifyState = true;
                            return;
                        }
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(2);
                        HwQoEWeChatBooster.this.mIsVerifyState = false;
                        HwQoEWeChatBooster.this.mIsHandoverToMobile = false;
                    }
                } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                    HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(1);
                } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                    HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(5);
                } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    int wifistatue = intent.getIntExtra("wifi_state", 4);
                    if (1 == wifistatue) {
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(6);
                    } else if (3 == wifistatue) {
                        HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessage(7);
                    }
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    private void initHwWeChatBoosterHandler() {
        HandlerThread handlerThread = new HandlerThread("hw_wechatbooster_handler_thread");
        handlerThread.start();
        this.mHwWeChatBoosterHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        HwQoEWeChatBooster.this.handleWifiRssiChanged();
                        return;
                    case 2:
                        HwQoEWeChatBooster.this.handleWifiConnected();
                        return;
                    case 3:
                        HwQoEWeChatBooster.this.handleWifiDisConnected();
                        return;
                    case 4:
                        HwQoEWeChatBooster.this.handlerWeChatStateChanged(msg.arg1, msg.arg2, ((Boolean) msg.obj).booleanValue());
                        return;
                    case 5:
                        HwQoEWeChatBooster.this.handleConnectivityNetworkChange();
                        return;
                    case 6:
                        HwQoEWeChatBooster.this.logD("WIFI_STATE_DISABLED: CurrRssiLevel= " + HwQoEWeChatBooster.this.mCurrRssiLevel + " , WeCharting: " + HwQoEWeChatBooster.this.mIsWeCharting);
                        if (HwQoEWeChatBooster.this.mCurrRssiLevel > 2 && HwQoEWeChatBooster.this.mIsWeCharting) {
                            HwQoEWeChatBooster.this.mHiDataApBlackWhiteListManager.addHandoverBlackList(HwQoEWeChatBooster.this.mCurrSSID, HwQoEWeChatBooster.this.mApAuthType, HwQoEWeChatBooster.this.mCurrWeChatType);
                        }
                        if (HwQoEWeChatBooster.this.mIsWeCharting && (HwQoEWeChatBooster.this.mIsUserManualCloseSuccess ^ 1) != 0) {
                            HwQoEWeChatBooster.this.mIsUserManualCloseSuccess = true;
                            HwQoEWeChatBooster.this.uploadCHRHandoverInfo(1);
                            return;
                        }
                        return;
                    case 7:
                        HwQoEWeChatBooster.this.logD("MSG_WIFI_STATE_ENABLED: mIsWeCharting= " + HwQoEWeChatBooster.this.mIsWeCharting);
                        if (HwQoEWeChatBooster.this.mIsWeCharting) {
                            HwQoEWeChatBooster.this.mIsUserManualOpenSuccess = true;
                            if (!HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.hasMessages(9)) {
                                HwQoEWeChatBooster.this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(9, 10000);
                                return;
                            }
                            return;
                        }
                        return;
                    case 8:
                        HwQoEUtils.logD("MSG_WIFI_COLLECT_MACHINE_INFO");
                        HwQoEWeChatBooster.this.isUploadMachineInfo = true;
                        return;
                    case 9:
                        HwQoEUtils.logD("MSG_WIFI_COLLECT_HANDOVER_INFO");
                        if (HwQoEWeChatBooster.this.mIsWeCharting && HwQoEWeChatBooster.this.mWechatNetwork == 1) {
                            HwQoEWeChatBooster.this.uploadCHRHandoverInfo(2);
                            return;
                        }
                        return;
                    case 103:
                        HwQoEUtils.logD("wechat QOE_MSG_MONITOR_HAVE_INTERNET");
                        if (HwQoEWeChatBooster.this.isNetworkChecking) {
                            HwQoEWeChatBooster.this.isNetworkChecking = false;
                            removeMessages(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT);
                            return;
                        }
                        return;
                    case 104:
                    case HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT /*112*/:
                        HwQoEUtils.logD("wechat QOE_MSG_MONITOR_NO_INTERNET isNetworkChecking = " + HwQoEWeChatBooster.this.isNetworkChecking);
                        if (HwQoEWeChatBooster.this.isNetworkChecking) {
                            HwQoEWeChatBooster.this.isNetworkHaveInternet = false;
                            HwQoEWeChatBooster.this.isNetworkChecking = false;
                            HwQoEWeChatBooster.this.handlerNoInternet();
                            return;
                        }
                        return;
                    case 122:
                        HwQoEWeChatBooster.this.onUpdateQualityInfo((long) msg.arg1, (long) msg.arg2);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public void updateWifiConnectionMode(boolean isUserManualConnect, boolean isUserHandoverWiFi) {
        logD("updateWifiConnectionMode, manualConnect : " + isUserManualConnect + " , handoverWiFi: " + isUserHandoverWiFi);
        this.mIsUserManualConnectSuccess = isUserHandoverWiFi;
        if (this.mIsWeCharting && this.mIsUserManualConnectSuccess) {
            this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(9, 10000);
        }
    }

    private void onUpdateQualityInfo(long outSpeed, long inSpeed) {
        if (this.mIsWeCharting) {
            int targetSpeed;
            int type;
            int poorTargetNum;
            if (isCameraOn()) {
                targetSpeed = 120;
                type = 1;
                poorTargetNum = 2;
            } else {
                targetSpeed = 30;
                type = 2;
                poorTargetNum = 4;
            }
            if (this.mWechatNetwork == 1) {
                if (isNeedStallCheck() && (isWiFiProEnable() ^ 1) == 0) {
                    WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
                    int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi());
                    HwQoEUtils.logD("HwQoEService: onUpdateQualityInfo  rssiLevel = " + rssiLevel + " wifiInfo.getRssi()" + wifiInfo.getRssi() + " outSpeed = " + outSpeed + " inSpeed = " + inSpeed + " txBad = " + wifiInfo.txBad + " txSuccess = " + wifiInfo.txSuccess + " txBadRate = " + wifiInfo.txBadRate);
                    setWechatCallType(type);
                    if (rssiLevel <= 2 && inSpeed > 0 && inSpeed < ((long) targetSpeed)) {
                        this.isPoorNetworkARPNum++;
                        this.isPoorNetworkNum++;
                        HwQoEUtils.logD("HwQoEService: onUpdateQualityInfo  isPoorNetworkNum = " + this.isPoorNetworkNum + " poorTargetNum = " + poorTargetNum + " isPoorNetworkARPNum = " + this.isPoorNetworkARPNum + " mARPCheckControlNum = " + this.mARPCheckControlNum);
                        if (this.isPoorNetworkARPNum < this.mARPCheckControlNum) {
                            this.mFirstArpRTT = this.mHiDataUtilsManager.checkARPRTT();
                        } else if (this.isPoorNetworkNum >= poorTargetNum) {
                            long secondArpRTT = this.mHiDataUtilsManager.checkARPRTT();
                            HwQoEUtils.logD("HwQoEService: onUpdateQualityInfo  mFirstArpRTT = " + this.mFirstArpRTT + " secondArpRTT = " + secondArpRTT);
                            if (isArpRTTBad(this.mFirstArpRTT) && isArpRTTBad(secondArpRTT)) {
                                handoverAction(secondArpRTT, 0);
                            } else if (isArpRTTBad(secondArpRTT) || isArpRTTBad(this.mFirstArpRTT)) {
                                this.isPoorNetworkARPNum = 0;
                                this.mARPCheckControlNum = 2;
                            } else {
                                int level;
                                if (this.mARPCheckControlNum < 8) {
                                    level = 5;
                                } else if (this.mARPCheckControlNum < 17) {
                                    level = 9;
                                } else {
                                    level = 16;
                                }
                                this.mARPCheckControlNum = this.isPoorNetworkARPNum + level;
                            }
                            this.isPoorNetworkNum = 0;
                        }
                        this.isNetworkHaveInternet = true;
                    } else if (inSpeed > ((long) targetSpeed)) {
                        this.isNetworkHaveInternet = true;
                        this.isNetworkChecking = false;
                        this.isPoorNetworkNum = 0;
                        this.isPoorNetworkARPNum = 0;
                        this.mARPCheckControlNum = 0;
                    } else if (inSpeed == 0 && isWiFiProEnable()) {
                        startNetworkChecking();
                    }
                    if (this.isNetworkHaveInternet && rssiLevel == 2) {
                        updateAPPQuality(wifiInfo, inSpeed, type);
                    }
                    saveWeChatCHRInfo(outSpeed, inSpeed);
                    updateWeChatMachineInfo();
                } else {
                    HwQoEUtils.logD("network is bad mIsVerifyState = " + this.mIsVerifyState + " mIsUserManualConnectSuccess = " + this.mIsUserManualConnectSuccess + " mHiDataUtilsManager.isMobileNetworkReady(mCurrWeChatType) = " + this.mHiDataUtilsManager.isMobileNetworkReady(this.mCurrWeChatType) + " isMobileDataConnected = " + isMobileDataConnected());
                    updateWeChatSpeedStatic(inSpeed, type);
                    return;
                }
            }
            updateWeChatSpeedStatic(inSpeed, type);
        }
    }

    private void setHighPriorityTransmit(int uid, int enable) {
        HwQoEUtils.logD("HwQoEService: setGameKOGHighPriorityTransmit uid: " + uid + " enable: " + enable);
        this.mHwQoEWiFiOptimization.hwQoEHighPriorityTransmit(uid, 17, enable);
    }

    private void setPSAndRetryMode(boolean enable) {
        HwQoEUtils.logD("HwQoEService: setPSAndRetryMode  enable: " + enable);
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
        return Global.getInt(cr, name, def ? 1 : 0) == 1;
    }

    private void setAppStateMonitor(int network) {
        if (this.mHwQoEContentAware != null) {
            this.mHwQoEContentAware.setAppStateMonitorEnabled(true, WECHAT_NAME, network);
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
            HwQoEUtils.logD("curRecord.mThoughtput = " + curRecord.mThoughtput + " inSpeed = " + inSpeed);
            curRecord.mThoughtput = ((curRecord.mThoughtput * 8) / 10) + ((2 * inSpeed) / 10);
            HwQoEUtils.logD("curRecord.mThoughtput = " + curRecord.mThoughtput);
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
        HwQoEUtils.logD("startNetworkChecking  isNetworkChecking = " + this.isNetworkChecking + " isNetworkHaveInternet= " + this.isNetworkHaveInternet);
        if (!this.isNetworkChecking && this.isNetworkHaveInternet) {
            this.isNetworkHaveInternet = false;
            this.isNetworkChecking = true;
            new HwQoENetworkChecker(this.mContext, this.mHwWeChatBoosterHandler).start();
            this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
        }
    }

    private void handlerNoInternet() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        HwQoEUtils.logD("handlerNoInternet rssiLevel = " + HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi()));
        this.mIsHandoverToMobile = true;
        this.mAutoHandoverTimes++;
        WifiProStateMachine.getWifiProStateMachineImpl().notifyHttpReachable(false);
    }

    private void handleConnectivityNetworkChange() {
        this.mActiveNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (this.mActiveNetworkInfo == null) {
            return;
        }
        if (this.mActiveNetworkInfo.getType() == 0) {
            if (this.mActiveNetworkInfo.isConnected()) {
                this.mWechatNetwork = 0;
                logD("TYPE_MOBILE is Connected ,WeCharting:" + this.mIsWeCharting);
                setAppStateMonitor(this.mWechatNetwork);
                if (this.mIsWeCharting) {
                    this.mHidataWechatTraffic.updateMobileWechatStateChanged(1, this.mCurrWeChatType, this.mWeChartUID, this.mCallStatisticsInfo);
                }
            } else if (this.mActiveNetworkInfo.getState() == State.DISCONNECTED && this.mIsWeCharting) {
                this.mHidataWechatTraffic.updateMobileWechatStateChanged(0, this.mCurrWeChatType, this.mWeChartUID, this.mCallStatisticsInfo);
            }
        } else if (1 != this.mActiveNetworkInfo.getType()) {
        } else {
            if (this.mActiveNetworkInfo.isConnected()) {
                if (this.mIsWeCharting) {
                    this.mWeChatStartTime = System.currentTimeMillis();
                }
            } else if (this.mActiveNetworkInfo.getState() == State.DISCONNECTED && this.mIsWeCharting && this.mWeChatStartTime != 0 && this.mCallStatisticsInfo != null) {
                long wifiCallTime = (System.currentTimeMillis() - this.mWeChatStartTime) / 1000;
                HiDataCHRStatisticsInfo hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
                hiDataCHRStatisticsInfo.mCallInWiFiDur = (int) (((long) hiDataCHRStatisticsInfo.mCallInWiFiDur) + wifiCallTime);
            }
        }
    }

    private void handleWifiRssiChanged() {
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mWifiInfo != null) {
            this.mCurrRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.mWifiInfo.getFrequency(), this.mWifiInfo.getRssi());
            if (this.mIsWeCharting && this.mWechatNetwork == 1) {
                if (this.mCurrRssiLevel <= 2 && (this.mPhaseTwoIsEnable ^ 1) != 0) {
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
        int i = 1;
        logD("handleWifiConnected, ManualConnect:" + this.mIsUserManualConnectSuccess + ",WeCharting:" + this.mIsWeCharting + ", ManualOpen: " + this.mIsUserManualOpenSuccess);
        this.mWechatNetwork = 1;
        setAppStateMonitor(this.mWechatNetwork);
        if (!this.mHiDataUtilsManager.isPublicAP()) {
            i = 0;
        }
        this.mApAuthType = i;
        this.mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mWifiInfo != null) {
            this.mCurrRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.mWifiInfo.getFrequency(), this.mWifiInfo.getRssi());
            this.mCurrSSID = this.mWifiInfo.getSSID();
            getAllWeChatData();
        }
        if (this.mIsWeCharting) {
            this.mHidataWechatTraffic.updateMobileWechatStateChanged(0, this.mCurrWeChatType, this.mWeChartUID, this.mCallStatisticsInfo);
        }
    }

    private void handleWechatTurnOn(int network) {
        int i = 1;
        if (network == 1) {
            this.mApAuthType = this.mHiDataUtilsManager.isPublicAP() ? 1 : 0;
            if (!this.mHidataWechatTraffic.wechatTrafficWealthy(this.mCurrWeChatType)) {
                i = 0;
            }
            this.mUserType = i;
            this.mApBlackType = this.mHiDataApBlackWhiteListManager.getApBlackType(this.mCurrSSID, this.mApAuthType, this.mCurrWeChatType, this.mUserType);
            logD("handleWechatTurnOn, mApAuthType = " + this.mApAuthType + ", mUserType = " + this.mUserType + ", mApBlackType = " + this.mApBlackType);
        }
    }

    private void handleWifiDisConnected() {
        this.mWifiInfo = null;
        logD("handleWifiDisConnected  mIsWeCharting = " + this.mIsWeCharting + " mWechatNetwork = " + this.mWechatNetwork);
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

    private void handlerWeChatStateChanged(int uid, int state, boolean isBackground) {
        this.mWeChartUID = uid;
        if (1 == state) {
            if (this.mWechatNetwork == 1) {
                logD("WeChat phone trun on in wifi");
                this.mWeChatStartTime = System.currentTimeMillis();
                startWiFiOptimization();
            }
            this.mIsWeCharting = true;
            if (isCameraOn()) {
                logD("WeChat video call");
                this.mCurrWeChatType = 1;
            } else {
                logD("WeChat audio call");
                this.mCurrWeChatType = 2;
            }
            this.mHwWifiStatStore.setWeChatScene(this.mCurrWeChatType);
            if (this.mWechatNetwork == 0) {
                this.mHidataWechatTraffic.updateMobileWechatStateChanged(1, this.mCurrWeChatType, this.mWeChartUID, null);
            }
            handleWechatTurnOn(this.mWechatNetwork);
            updateStartWeChatCallCHR();
        } else if (state == 0) {
            if (this.mWechatNetwork == 1) {
                logD("WeChat phone trun off in wifi");
                stopWiFiOptimization();
                updateAppQualityRecords(this.mConnectedRecordList);
                if (!(this.mWeChatStartTime == 0 || this.mCallStatisticsInfo == null)) {
                    long weChatWiFiTime = (System.currentTimeMillis() - this.mWeChatStartTime) / 1000;
                    HiDataCHRStatisticsInfo hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
                    hiDataCHRStatisticsInfo.mCallInWiFiDur = (int) (((long) hiDataCHRStatisticsInfo.mCallInWiFiDur) + weChatWiFiTime);
                }
            } else if (this.mWechatNetwork == 0) {
                logD("WeChat phone trun off in celluar");
                this.mHidataWechatTraffic.updateMobileWechatStateChanged(0, this.mCurrWeChatType, this.mWeChartUID, this.mCallStatisticsInfo);
            } else if (-1 == this.mWechatNetwork) {
                logD("WeChat phone trun off ");
            }
            this.mCurrWeChatType = 0;
            if (this.mIsHandoverToMobile) {
                handoverToMobile(false);
            }
            updateEndWeChatCallCHR(this.mCallStatisticsInfo);
            this.mIsWeCharting = false;
        } else if (2 == state) {
            logD("WECHAT_BACK_GROUND_CHANG  mIsWeCharting = " + this.mIsWeCharting + " isBackground = " + isBackground);
            if (!this.mIsWeCharting) {
                return;
            }
            if (isBackground) {
                stopWiFiOptimization();
            } else {
                startWiFiOptimization();
            }
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
            HwQoEUtils.logD("setWechatCallType mCurrWeChatType = " + this.mCurrWeChatType);
        }
        this.mCurrWeChatType = type;
    }

    private void handoverAction(long arpTime, long time) {
        uploadWeChatStallInfo(arpTime, time);
        handoverToMobile(true);
    }

    private boolean isNeedStallCheck() {
        return (isMobileDataConnected() && this.mHiDataUtilsManager.isMobileNetworkReady(this.mCurrWeChatType) && (this.mIsVerifyState ^ 1) != 0) ? this.mIsUserManualConnectSuccess ^ 1 : false;
    }

    private boolean isArpRTTBad(long rtt) {
        if (rtt > 100 || rtt == -1) {
            return true;
        }
        return false;
    }

    public void onSensitiveAppStateChange(int uid, int state, boolean isBackground) {
        this.mHwWeChatBoosterHandler.sendMessage(this.mHwWeChatBoosterHandler.obtainMessage(4, uid, state, Boolean.valueOf(isBackground)));
    }

    public void onPeriodSpeed(long outSpeed, long inSpeed) {
        this.mHwWeChatBoosterHandler.sendMessage(this.mHwWeChatBoosterHandler.obtainMessage(122, (int) outSpeed, (int) inSpeed, null));
    }

    public boolean isConnectWhenWeChating(ScanResult scanResult) {
        int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(scanResult.frequency, scanResult.level);
        HwQoEUtils.logD("isWeChating mIsWeCharting = " + this.mIsWeCharting + " scanResult.level = " + scanResult.level + "  mIsHandoverRSSI = " + this.mIsHandoverRSSI);
        if (!this.mIsWeCharting || rssiLevel <= 1) {
            return false;
        }
        if (this.mIsHandoverToMobile && this.mIsHandoverRSSI != 0) {
            return scanResult.level >= this.mIsHandoverRSSI + 10;
        } else {
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
                HwQoEUtils.logD("record.mRSSI = " + record.mRSSI + " record.mThoughtput = " + record.mThoughtput);
                if (record.mRSSI <= scanResult.level && record.mThoughtput > ((long) (targetSpeed + 20))) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isWeChating() {
        boolean result = this.mIsWeCharting;
        HwQoEUtils.logD("isWeChating result = " + result + " mIsWeCharting = " + this.mIsWeCharting + " mIsHandoverToMobile = " + this.mIsHandoverToMobile);
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

    private void handoverToMobile(boolean flag) {
        HwQoEUtils.logD("handoverToMobile flag = " + flag);
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (flag) {
            this.mIsHandoverRSSI = info.getRssi();
            this.mIsHandoverToMobile = true;
            this.mAutoHandoverTimes++;
            HwWifiConnectivityMonitor.getInstance().weChatPoorWiFi();
            return;
        }
        if (isNeedRoveToWiFi(info.getRssi(), info.getFrequency())) {
            WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiLinkPoor(false);
        }
        this.mIsHandoverToMobile = false;
        this.mIsHandoverRSSI = 0;
    }

    private boolean isNeedRoveToWiFi(int rssi, int frequency) {
        if (this.mIsHandoverRSSI == 0) {
            return true;
        }
        return this.mIsHandoverRSSI > 0 && rssi > this.mIsHandoverRSSI && HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(frequency, rssi) > 2;
    }

    private boolean isMobileDataConnected() {
        if (5 == this.mTelephonyManager.getSimState() && getMoblieDateSettings() && (isAirModeOn() ^ 1) != 0) {
            return true;
        }
        return false;
    }

    private boolean isAirModeOn() {
        boolean z = true;
        if (this.mContext == null) {
            return false;
        }
        if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
            z = false;
        }
        return z;
    }

    private static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return System.getInt(cr, name, def ? 1 : 0) == 1;
    }

    private boolean isWiFiProEnable() {
        return getSettingsSystemBoolean(this.mContext.getContentResolver(), "smart_network_switching", false);
    }

    private void logD(String info) {
        Log.d(TAG, info);
    }

    private void uploadWeChatStallInfo(long arpRtt, long netRtt) {
        HiDataCHRStallInfo exception = new HiDataCHRStallInfo();
        exception.mAPKName = WECHAT_NAME;
        exception.mScenario = this.mCurrWeChatType;
        exception.mUlTup = (int) this.mLastOutSpeed;
        exception.mDlTup = (int) this.mSecInSpeed;
        exception.mApRtt = (int) arpRtt;
        exception.mNetRtt = (int) netRtt;
        exception.mNeiborApRssi = 0;
        this.mHiDataUtilsManager.updateCellInfo(exception);
        this.mHiDataCHRManager.uploadWeChatStallInfo(exception);
        HwQoEUtils.logD("uploadWeChatStallInfo mAPKName = " + exception.mAPKName + " mScenario = " + exception.mScenario + " mUlTup = " + exception.mUlTup + " mDlTup = " + exception.mDlTup + " mApRtt = " + exception.mApRtt + " mNetRtt = " + exception.mNetRtt + " mRAT = " + exception.mRAT + " mCellSig = " + exception.mCellSig + " mCellRsrq = " + exception.mCellRsrq + " mCellSinr = " + exception.mCellSinr + " mNeiborApRssi = " + exception.mNeiborApRssi);
    }

    private void updateWeChatMachineInfo() {
        if (this.isUploadMachineInfo) {
            HwQoEUtils.logD("updateWeChatMachineInfo mIsWeCharting = " + this.mIsWeCharting + " mWechatNetwork = " + this.mWechatNetwork);
            if (this.mHiDataCHRMachineInfo == null) {
                this.mHiDataCHRMachineInfo = new HiDataCHRMachineInfo();
                this.mFirstMachineInfo = this.mHiDataUtilsManager.getOTAInfo();
            } else if (this.mSecondMachineInfo == null) {
                this.mSecondMachineInfo = this.mHiDataUtilsManager.getOTAInfo();
                txBad = this.mSecondMachineInfo.txbad - this.mFirstMachineInfo.txbad;
                txGood = this.mSecondMachineInfo.txgood - this.mFirstMachineInfo.txgood;
                HwQoEUtils.logD("updateWeChatMachineInfo txBad = " + txBad + " txGood = " + txGood);
                if (txBad + txGood > 0) {
                    this.mHiDataCHRMachineInfo.mTxFail1Bef = txBad / (txBad + txGood);
                }
                this.mFirstMachineInfo = this.mSecondMachineInfo;
            } else {
                this.mSecondMachineInfo = this.mHiDataUtilsManager.getOTAInfo();
                txBad = this.mSecondMachineInfo.txbad - this.mFirstMachineInfo.txbad;
                txGood = this.mSecondMachineInfo.txgood - this.mFirstMachineInfo.txgood;
                HwQoEUtils.logD("updateWeChatMachineInfo txBad = " + txBad + " txGood = " + txGood);
                if (txBad + txGood > 0) {
                    this.mHiDataCHRMachineInfo.mTxFail1Bef = txBad / (txBad + txGood);
                }
                this.mHiDataCHRMachineInfo.mRxTup1Bef = (int) this.mFirstInSpeed;
                this.mHiDataCHRMachineInfo.mRxTup2Bef = (int) this.mSecInSpeed;
                this.mHiDataCHRMachineInfo.mChLoad = this.mHiDataUtilsManager.getOTAChannelLoad();
                this.mHiDataCHRManager.uploadWeChatMachineInfo(this.mHiDataCHRMachineInfo);
                HwQoEUtils.logD("updateWeChatMachineInfo mRxTup1Bef = " + this.mHiDataCHRMachineInfo.mRxTup1Bef + " mRxTup2Bef = " + this.mHiDataCHRMachineInfo.mRxTup2Bef + " mTxFail1Bef = " + this.mHiDataCHRMachineInfo.mTxFail1Bef + " mTxFail2Bef = " + this.mHiDataCHRMachineInfo.mTxFail2Bef + " mChLoad = " + this.mHiDataCHRMachineInfo.mChLoad);
                this.mSecondMachineInfo = this.mFirstMachineInfo;
                this.isUploadMachineInfo = false;
                this.mFirstMachineInfo = null;
                this.mSecondMachineInfo = null;
                this.mHiDataCHRMachineInfo = null;
            }
        }
    }

    private void updateStartWeChatCallCHR() {
        if (isCameraOn()) {
            this.mCallStatisticsInfo = this.mHiDataCHRManager.getWeChatVideoStatistics();
            this.mCallStatisticsInfo.mAPPType = 1;
        } else {
            this.mCallStatisticsInfo = this.mHiDataCHRManager.getWeChatAudioStatistics();
            this.mCallStatisticsInfo.mAPPType = 2;
        }
        HiDataCHRStatisticsInfo hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
        hiDataCHRStatisticsInfo.mCallTotalCnt++;
        this.mCallStatisticsInfo.mStartTime = System.currentTimeMillis();
        if (this.mWechatNetwork == 0) {
            hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
            hiDataCHRStatisticsInfo.mStartInCellularCnt++;
        } else {
            hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
            hiDataCHRStatisticsInfo.mStartInWiFiCnt++;
        }
        this.mHwWeChatBoosterHandler.sendEmptyMessageDelayed(8, 30000);
    }

    private void updateWeChatSpeedStatic(long inSpeed, int type) {
        int targetSpeed;
        if (type == 1) {
            targetSpeed = 120;
        } else {
            targetSpeed = 30;
        }
        HiDataCHRStatisticsInfo hiDataCHRStatisticsInfo;
        if (inSpeed > ((long) targetSpeed)) {
            hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
            hiDataCHRStatisticsInfo.mStallSwitch0Cnt++;
        } else {
            hiDataCHRStatisticsInfo = this.mCallStatisticsInfo;
            hiDataCHRStatisticsInfo.mStallSwitch1Cnt++;
        }
        if (this.mWechatNetwork == 0) {
            this.mLastCellularInSpeed = inSpeed;
        }
    }

    private void updateEndWeChatCallCHR(HiDataCHRStatisticsInfo callStatisticsInfo) {
        long endTime = System.currentTimeMillis() - callStatisticsInfo.mStartTime;
        if (endTime < 300000) {
            callStatisticsInfo.mWiFiLv1Cnt++;
        } else if (endTime < 900000) {
            callStatisticsInfo.mWiFiLv2Cnt++;
        } else {
            callStatisticsInfo.mWiFiLv3Cnt++;
        }
        if (this.mAutoHandoverTimes > 0) {
            callStatisticsInfo.mStallSwitchCnt++;
        }
        if (this.mAutoHandoverTimes > 1) {
            callStatisticsInfo.mStallSwitchAbove1Cnt++;
        }
        if (this.mIsUserManualConnectSuccess) {
            callStatisticsInfo.mSwitch2WifiCnt++;
        }
        if (this.mIsUserManualOpenSuccess) {
            callStatisticsInfo.mVipSwitchCnt++;
        }
        if (this.mIsUserManualCloseSuccess) {
            callStatisticsInfo.mSwitch2CellCnt++;
        }
        this.mHiDataCHRManager.updateWeChatStatistics(callStatisticsInfo);
        this.mCallStatisticsInfo = null;
        this.mIsUserManualConnectSuccess = false;
        this.mIsUserManualOpenSuccess = false;
        this.mIsUserManualCloseSuccess = false;
    }

    private void saveWeChatCHRInfo(long outSpeeed, long inSpeed) {
        this.historyWiFiInfo = this.mWifiManager.getConnectionInfo();
        if (this.mFirstInSpeed == 0) {
            this.mFirstInSpeed = inSpeed;
            this.mSecInSpeed = inSpeed;
        } else {
            this.mFirstInSpeed = this.mSecInSpeed;
            this.mSecInSpeed = inSpeed;
        }
        this.mLastOutSpeed = outSpeeed;
    }

    private void uploadCHRHandoverInfo(int event) {
        WifiInfo info;
        this.mHiDataCHRHandoverInfo = new HiDataCHRHandoverInfo();
        this.mHiDataCHRHandoverInfo.mAPKname = WECHAT_NAME;
        this.mHiDataCHRHandoverInfo.mScenario = this.mCurrWeChatType;
        this.mHiDataCHRHandoverInfo.mEventType = event;
        if (event == 2) {
            info = this.mWifiManager.getConnectionInfo();
            this.mHiDataCHRHandoverInfo.mWifiSsidAft = info.getSSID();
            this.mHiDataCHRHandoverInfo.mWifiRssiAft = info.getRssi();
            this.mHiDataCHRHandoverInfo.mWifiChAft = info.getFrequency();
            this.mHiDataCHRHandoverInfo.mWifiRxTupAft = (int) this.mSecInSpeed;
        } else {
            info = this.historyWiFiInfo;
            if (info != null) {
                this.mHiDataCHRHandoverInfo.mWifiSsidBef = info.getSSID();
                this.mHiDataCHRHandoverInfo.mWifiRssiBef = info.getRssi();
                this.mHiDataCHRHandoverInfo.mWifiChBef = info.getFrequency();
                this.mHiDataCHRHandoverInfo.mWifiRxTup1Bef = (int) this.mFirstInSpeed;
                this.mHiDataCHRHandoverInfo.mWifiRxTup2Bef = (int) this.mSecInSpeed;
                this.mHiDataCHRHandoverInfo.mWifiTxFail1Bef = (int) info.txBadRate;
                this.mHiDataCHRHandoverInfo.mWifiTxFail2Bef = 0;
            } else {
                return;
            }
        }
        this.mHiDataCHRHandoverInfo.mCellRxTup = (int) this.mLastCellularInSpeed;
        this.mHiDataCHRHandoverInfo.mWifiChLoad = info.getChload();
        this.mHiDataCHRHandoverInfo.mCellSig = 0;
        this.mHiDataCHRHandoverInfo.mCellFreq = 0;
        this.mHiDataCHRHandoverInfo.mCellRat = 0;
        this.mHiDataCHRHandoverInfo.mSwitchCauseBef = 0;
        this.mHiDataCHRManager.uploadWeChatHandoverInfo(this.mHiDataCHRHandoverInfo);
        HwQoEUtils.logD("uploadCHRHandoverInfo mAPKname = " + this.mHiDataCHRHandoverInfo.mAPKname + " mScenario = " + this.mHiDataCHRHandoverInfo.mScenario + " mEventType = " + this.mHiDataCHRHandoverInfo.mEventType + " mWifiSsidAft = " + this.mHiDataCHRHandoverInfo.mWifiSsidAft + " mWifiRssiAft = " + this.mHiDataCHRHandoverInfo.mWifiRssiAft + " mWifiChAft = " + this.mHiDataCHRHandoverInfo.mWifiChAft + " mWifiChLoad = " + this.mHiDataCHRHandoverInfo.mWifiChLoad + " mWifiTxFail1Bef = " + this.mHiDataCHRHandoverInfo.mWifiTxFail1Bef + " mWifiRxTup1Bef = " + this.mHiDataCHRHandoverInfo.mWifiRxTup1Bef + " mWifiRxTup2Bef = " + this.mHiDataCHRHandoverInfo.mWifiRxTup2Bef + " mCellRat = " + this.mHiDataCHRHandoverInfo.mCellRat + " mWifiRssiBef = " + this.mHiDataCHRHandoverInfo.mWifiRssiBef + " mWifiTxFail2Bef = " + this.mHiDataCHRHandoverInfo.mWifiTxFail2Bef + " mCellSig = " + this.mHiDataCHRHandoverInfo.mCellSig + " mCellFreq = " + this.mHiDataCHRHandoverInfo.mCellFreq + " mWifiRxTupAft = " + this.mHiDataCHRHandoverInfo.mWifiRxTupAft + " mWifiSsidBef = " + this.mHiDataCHRHandoverInfo.mWifiSsidBef + " mCellRxTup = " + this.mHiDataCHRHandoverInfo.mCellRxTup + " mSwitchCauseBef = " + this.mHiDataCHRHandoverInfo.mSwitchCauseBef + " mWifiChBef = " + this.mHiDataCHRHandoverInfo.mWifiChBef);
    }
}

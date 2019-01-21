package com.android.server.wifi.wifipro;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanListener;
import android.net.wifi.WifiScanner.ScanSettings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.LocalServices;
import com.android.server.policy.AbsPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.List;

public class HwDualBandStateMachine extends StateMachine {
    private static final int CHR_WIFI_HIGH_SCAN_FREQUENCY = 5;
    private static final int CHR_WIFI_MID_SCAN_FREQUENCY = 3;
    private static final int WIFI_MAX_SCAN_THRESHOLD = -30;
    private static final int WIFI_MIN_SCAN_THRESHOLD = -90;
    private static final int WIFI_RSSI_GAP = 10;
    private static final int WIFI_SCANNING_CHANNEL_INDEX = 3;
    private static final int WIFI_SCANNING_CHANNEL_INDEX_MAX = 5;
    private static final int WIFI_SCANNING_CHANNEL_INDEX_MIN = 0;
    private static final int WIFI_SCAN_INTERVAL = 5;
    private WifiProDualbandExceptionRecord mCHRHandoverTooSlow = new WifiProDualbandExceptionRecord();
    private int mCHRMixAPScanCount = 0;
    private List<HwDualBandMonitorInfo> mCHRSavedAPList = new ArrayList();
    private int mCHRScanAPType = 0;
    private int mCHRSingleAPScanCount = 0;
    private State mConnectedState = new ConnectedState();
    private Context mContext;
    private final CustomizedScanListener mCustomizedScanListener = new CustomizedScanListener();
    private State mDefaultState = new DefaultState();
    private State mDisabledState = new DisabledState();
    private List<HwDualBandMonitorInfo> mDisappearAPList = new ArrayList();
    private State mDisconnectedState = new DisconnectedState();
    private FrameworkFacade mFrameworkFacade;
    private HwDualBandAdaptiveThreshold mHwDualBandAdaptiveThreshold;
    private HwDualBandInformationManager mHwDualBandInformationManager = null;
    private HwDualBandRelationManager mHwDualBandRelationManager = null;
    private HwDualBandWiFiMonitor mHwDualBandWiFiMonitor = null;
    private IDualBandManagerCallback mIDualbandManagerCallback = null;
    private State mInternetReadyState = new InternetReadyState();
    private boolean mIsDualbandScanning = false;
    private List<HwDualBandMonitorInfo> mMonitorAPList = new ArrayList();
    private State mMonitorState = new MonitorState();
    private PowerManager mPowerManager;
    private State mStopState = new StopState();
    private List<HwDualBandMonitorInfo> mTargetAPList;
    private WifiManager mWifiManager;
    private WifiScanner mWifiScanner;

    class ConnectedState extends State {
        ConnectedState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter ConnectedState");
            WifiInfo mWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (mWifiInfo != null && mWifiInfo.getBSSID() != null) {
                String str = HwDualBandMessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enter ConnectedState ssid = ");
                stringBuilder.append(mWifiInfo.getSSID());
                Log.e(str, stringBuilder.toString());
            }
        }

        public void exit() {
            HwDualBandStateMachine.this.removeMessages(11);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 1) {
                WifiInfo mInternetWifiInfo;
                switch (i) {
                    case 11:
                        Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED");
                        mInternetWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                        if (mInternetWifiInfo != null && mInternetWifiInfo.getBSSID() != null) {
                            HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mInternetReadyState);
                            HwDualBandStateMachine.this.sendMessage(104);
                            break;
                        }
                        if (mInternetWifiInfo == null) {
                            Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED mInternetWifiInfo == null");
                        } else {
                            Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_CONNECTED mInternetWifiInfo.getBSSID() == null");
                        }
                        HwDualBandStateMachine.this.sendMessageDelayed(11, 2000);
                        return true;
                        break;
                    case 12:
                    case 13:
                        Log.e(HwDualBandMessageUtil.TAG, "MSG_WIFI_INTERNET_DISCONNECTED");
                        mInternetWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                        if (!(mInternetWifiInfo == null || mInternetWifiInfo.getBSSID() == null)) {
                            WifiProDualBandApInfoRcd info = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mInternetWifiInfo.getBSSID());
                            if (info != null) {
                                info.mInetCapability = Short.valueOf((short) 2);
                                HwDualBandStateMachine.this.mHwDualBandInformationManager.updateAPInfo(info);
                                break;
                            }
                        }
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }
    }

    private class CustomizedScanListener implements ScanListener {
        private CustomizedScanListener() {
        }

        public void onSuccess() {
            Log.d(HwDualBandMessageUtil.TAG, "CustomizedScanListener onSuccess");
        }

        public void onFailure(int reason, String description) {
            Log.d(HwDualBandMessageUtil.TAG, "CustomizedScanListener onFailure");
        }

        public void onPeriodChanged(int periodInMs) {
            Log.d(HwDualBandMessageUtil.TAG, "CustomizedScanListener onPeriodChanged");
        }

        public void onResults(ScanData[] results) {
            Log.d(HwDualBandMessageUtil.TAG, "CustomizedScanListener onResults");
            HwDualBandStateMachine.this.sendMessage(7);
        }

        public void onFullResult(ScanResult fullScanResult) {
            Log.d(HwDualBandMessageUtil.TAG, "CustomizedScanListener onFullResult");
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DefaultState");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 4) {
                HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mDisabledState);
            } else if (i == 8) {
                Log.e(HwDualBandMessageUtil.TAG, "DefaultState MSG_WIFI_CONFIG_CHANGED");
                Bundle data = message.getData();
                String bssid = data.getString("bssid");
                String ssid = data.getString("ssid");
                int authtype = data.getInt(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE);
                String str = HwDualBandMessageUtil.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("MSG_WIFI_CONFIG_CHANGED ssid = ");
                stringBuilder.append(ssid);
                Log.e(str, stringBuilder.toString());
                if (ssid != null) {
                    HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, authtype);
                }
            } else if (i != 101) {
                switch (i) {
                    case 1:
                        Log.e(HwDualBandMessageUtil.TAG, "DefaultState MSG_WIFI_CONNECTED");
                        HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mConnectedState);
                        break;
                    case 2:
                        HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mDisconnectedState);
                        break;
                }
            } else {
                HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mStopState);
            }
            return true;
        }
    }

    class DisabledState extends State {
        DisabledState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DisabledState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (HwDualBandStateMachine.this.mTargetAPList != null && HwDualBandStateMachine.this.mTargetAPList.size() != 0) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what != 4) {
                return false;
            }
            return true;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter DisconnectedState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (!(HwDualBandStateMachine.this.mTargetAPList == null || HwDualBandStateMachine.this.mTargetAPList.size() == 0)) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
            if (HwDualBandStateMachine.this.mDisappearAPList != null && HwDualBandStateMachine.this.mDisappearAPList.size() != 0) {
                HwDualBandStateMachine.this.mDisappearAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what != 2) {
                return false;
            }
            return true;
        }
    }

    class InternetReadyState extends State {
        private HwDualBandMonitorInfo hasDualBandMonitorCandidate = null;
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;
        private int mLastRecordLevel = 0;

        InternetReadyState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter InternetReadyState");
            WifiInfo mWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
                Log.e(HwDualBandMessageUtil.TAG, "Enter InternetReadyState error info");
                return;
            }
            this.mCurrentSSID = mWifiInfo.getSSID();
            this.mCurrentBSSID = mWifiInfo.getBSSID();
            String str = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Enter InternetReadyState mCurrentSSID = ");
            stringBuilder.append(this.mCurrentSSID);
            Log.e(str, stringBuilder.toString());
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            int i2 = 1;
            WifiInfo mConnectedWifiInfo;
            if (i == 1) {
                mConnectedWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                if (!(mConnectedWifiInfo == null || mConnectedWifiInfo.getBSSID() == null || mConnectedWifiInfo.getBSSID().equals(this.mCurrentBSSID))) {
                    Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_CONNECTED");
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mConnectedState);
                }
            } else if (i != 7) {
                String str;
                StringBuilder stringBuilder;
                if (i == 19) {
                    Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_VERIFYING_POOR_LINK");
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mConnectedState);
                } else if (i == 102) {
                    HwDualBandStateMachine.this.mTargetAPList = message.getData().getParcelableArrayList(HwDualBandMessageUtil.MSG_KEY_APLIST);
                    if (HwDualBandStateMachine.this.mTargetAPList != null) {
                        str = HwDualBandMessageUtil.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("CMD_START_MONITOR size = ");
                        stringBuilder.append(HwDualBandStateMachine.this.mTargetAPList.size());
                        Log.e(str, stringBuilder.toString());
                    }
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mMonitorState);
                } else if (i != 104) {
                    switch (i) {
                        case 11:
                        case 13:
                            break;
                        case 12:
                            Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_INTERNET_DISCONNECTED");
                            HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mConnectedState);
                            HwDualBandStateMachine.this.sendMessage(12);
                            break;
                        default:
                            int i3 = 0;
                            WifiProDualBandApInfoRcd m5GAPInfo;
                            String str2;
                            StringBuilder stringBuilder2;
                            switch (i) {
                                case 16:
                                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE");
                                    mConnectedWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                                    if (!(mConnectedWifiInfo == null || mConnectedWifiInfo.getBSSID() == null)) {
                                        WifiProDualBandApInfoRcd apinfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mConnectedWifiInfo.getBSSID());
                                        if (apinfo != null) {
                                            List<WifiProRelateApRcd> mLists = apinfo.getRelateApRcds();
                                            if (mLists.size() > 0) {
                                                String str3 = HwDualBandMessageUtil.TAG;
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("MSG_DUAL_BAND_WIFI_TYPE_SINGLE mLists.size() = ");
                                                stringBuilder3.append(mLists.size());
                                                Log.e(str3, stringBuilder3.toString());
                                                WifiProRelateApRcd info = (WifiProRelateApRcd) mLists.get(0);
                                                m5GAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(info.mRelatedBSSID);
                                                if (m5GAPInfo != null) {
                                                    if (m5GAPInfo.isInBlackList != 1) {
                                                        HwDualBandMonitorInfo hwDualBandMonitorInfo = new HwDualBandMonitorInfo(info.mRelatedBSSID, m5GAPInfo.mApSSID, m5GAPInfo.mApAuthType.shortValue(), 0, 0, info.mRelateType);
                                                        hwDualBandMonitorInfo.mIsNearAP = 1;
                                                        List<HwDualBandMonitorInfo> apList = new ArrayList();
                                                        str2 = HwDualBandMessageUtil.TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("MSG_DUAL_BAND_WIFI_TYPE_SINGLE find ssid = ");
                                                        stringBuilder2.append(hwDualBandMonitorInfo.mSsid);
                                                        stringBuilder2.append(" m5GAPInfo.mApAuthType = ");
                                                        stringBuilder2.append(m5GAPInfo.mApAuthType);
                                                        Log.e(str2, stringBuilder2.toString());
                                                        apList.add(hwDualBandMonitorInfo);
                                                        HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(1, apList);
                                                        break;
                                                    }
                                                    String str4 = HwDualBandMessageUtil.TAG;
                                                    StringBuilder stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("MSG_DUAL_BAND_WIFI_TYPE_SINGLE m5GAPInfo.isInBlackList = ");
                                                    stringBuilder4.append(m5GAPInfo.isInBlackList);
                                                    Log.e(str4, stringBuilder4.toString());
                                                    break;
                                                }
                                                Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE m5GAPInfo == null");
                                                break;
                                            }
                                            Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE mLists.size() <= 0");
                                            break;
                                        }
                                        Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_SINGLE apinfo == null");
                                        break;
                                    }
                                case 17:
                                    Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX");
                                    this.hasDualBandMonitorCandidate = null;
                                    this.mLastRecordLevel = 0;
                                    mConnectedWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                                    if (!(mConnectedWifiInfo == null || mConnectedWifiInfo.getBSSID() == null)) {
                                        List<HwDualBandMonitorInfo> mMixAPList = new ArrayList();
                                        m5GAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mConnectedWifiInfo.getBSSID());
                                        if (m5GAPInfo != null) {
                                            List<ScanResult> lists = WifiproUtils.getScanResultsFromWsm();
                                            if (lists != null) {
                                                List<WifiProRelateApRcd> mMixLists = m5GAPInfo.getRelateApRcds();
                                                str2 = HwDualBandMessageUtil.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("MSG_DUAL_BAND_WIFI_TYPE_MIX mMixLists.size() = ");
                                                stringBuilder2.append(mMixLists.size());
                                                Log.e(str2, stringBuilder2.toString());
                                                for (WifiProRelateApRcd record : mMixLists) {
                                                    WifiProDualBandApInfoRcd m5GAPInfo2 = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(record.mRelatedBSSID);
                                                    if (m5GAPInfo2 == null) {
                                                        Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX m5GAPInfo == null");
                                                    } else if (m5GAPInfo2.isInBlackList == i2) {
                                                        String str5 = HwDualBandMessageUtil.TAG;
                                                        StringBuilder stringBuilder5 = new StringBuilder();
                                                        stringBuilder5.append("MSG_DUAL_BAND_WIFI_TYPE_MIX m5GAPInfo.mIsInblackList = ");
                                                        stringBuilder5.append(m5GAPInfo2.isInBlackList);
                                                        Log.e(str5, stringBuilder5.toString());
                                                    } else {
                                                        if (m5GAPInfo2.mInetCapability.shortValue() == i2) {
                                                            HwDualBandMonitorInfo hwDualBandMonitorInfo2 = new HwDualBandMonitorInfo(m5GAPInfo2.apBSSID, m5GAPInfo2.mApSSID, m5GAPInfo2.mApAuthType.shortValue(), 0, 0, record.mRelateType);
                                                            if (hwDualBandMonitorInfo2.mIsDualbandAP == i2 || isNearAP(record)) {
                                                                hwDualBandMonitorInfo2.mIsNearAP = i2;
                                                            } else {
                                                                hwDualBandMonitorInfo2.mIsNearAP = i3;
                                                            }
                                                            for (ScanResult result : lists) {
                                                                if (result.SSID != null && result.SSID.length() > 0 && result.BSSID != null && result.BSSID.equals(hwDualBandMonitorInfo2.mBssid)) {
                                                                    String str6 = HwDualBandMessageUtil.TAG;
                                                                    StringBuilder stringBuilder6 = new StringBuilder();
                                                                    stringBuilder6.append("MSG_DUAL_BAND_WIFI_TYPE_MIX find ssid = ");
                                                                    stringBuilder6.append(hwDualBandMonitorInfo2.mSsid);
                                                                    stringBuilder6.append(" , mApAuthType = ");
                                                                    stringBuilder6.append(m5GAPInfo2.mApAuthType);
                                                                    stringBuilder6.append(" , mIsNearAP = ");
                                                                    stringBuilder6.append(hwDualBandMonitorInfo2.mIsDualbandAP);
                                                                    stringBuilder6.append(" , level = ");
                                                                    stringBuilder6.append(result.level);
                                                                    Log.d(str6, stringBuilder6.toString());
                                                                    if (this.hasDualBandMonitorCandidate == null) {
                                                                        this.hasDualBandMonitorCandidate = hwDualBandMonitorInfo2;
                                                                        this.mLastRecordLevel = result.level;
                                                                    } else if (this.hasDualBandMonitorCandidate.mIsDualbandAP == 1 && this.mLastRecordLevel < 0 && this.mLastRecordLevel >= -65) {
                                                                        Log.d(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX  hasDualBandMonitorCandidate is AP_TYPE_SINGLE");
                                                                    } else if (hwDualBandMonitorInfo2.mIsDualbandAP != 1 || result.level < -65) {
                                                                        str = HwDualBandMessageUtil.TAG;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("MSG_DUAL_BAND_WIFI_TYPE_MIX result.level = ");
                                                                        stringBuilder.append(result.level);
                                                                        stringBuilder.append(", mLastRecordLevel = ");
                                                                        stringBuilder.append(this.mLastRecordLevel);
                                                                        Log.d(str, stringBuilder.toString());
                                                                        if (this.mLastRecordLevel < 0 && result.level > this.mLastRecordLevel) {
                                                                            this.hasDualBandMonitorCandidate = hwDualBandMonitorInfo2;
                                                                            this.mLastRecordLevel = result.level;
                                                                        }
                                                                    } else {
                                                                        this.hasDualBandMonitorCandidate = hwDualBandMonitorInfo2;
                                                                        this.mLastRecordLevel = result.level;
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            str = HwDualBandMessageUtil.TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("ssid = ");
                                                            stringBuilder.append(m5GAPInfo2.mApSSID);
                                                            stringBuilder.append(" have no internet");
                                                            Log.e(str, stringBuilder.toString());
                                                        }
                                                        i2 = 1;
                                                        i3 = 0;
                                                    }
                                                }
                                                if (this.hasDualBandMonitorCandidate != null) {
                                                    str = HwDualBandMessageUtil.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("MSG_DUAL_BAND_WIFI_TYPE_MIX select ssid = ");
                                                    stringBuilder.append(this.hasDualBandMonitorCandidate.mSsid);
                                                    Log.d(str, stringBuilder.toString());
                                                    mMixAPList.add(this.hasDualBandMonitorCandidate);
                                                }
                                                str = HwDualBandMessageUtil.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("MSG_DUAL_BAND_WIFI_TYPE_MIX after filter mMixAPList.size() = ");
                                                stringBuilder.append(mMixAPList.size());
                                                Log.e(str, stringBuilder.toString());
                                                if (mMixAPList.size() > 0) {
                                                    HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(2, mMixAPList);
                                                    break;
                                                }
                                            }
                                            Log.d(HwDualBandMessageUtil.TAG, "getScanResultsFromWsm lists is null");
                                            break;
                                        }
                                        Log.e(HwDualBandMessageUtil.TAG, "MSG_DUAL_BAND_WIFI_TYPE_MIX mMixAPinfo == null");
                                        break;
                                    }
                                    break;
                                default:
                                    return false;
                            }
                            break;
                    }
                } else {
                    mConnectedWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
                    if (mConnectedWifiInfo == null || mConnectedWifiInfo.getBSSID() == null) {
                        Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState mInternetWifiInfo == null");
                    } else if (HwDualBandStateMachine.this.mHwDualBandInformationManager.isEnterpriseSecurity(mConnectedWifiInfo.getNetworkId()) || HwDualBandStateMachine.this.mHwDualBandInformationManager.isEnterpriseAP(mConnectedWifiInfo.getBSSID()) || isMobileAP()) {
                        Log.e(HwDualBandMessageUtil.TAG, "InternetReadyState isEnterpriseAP");
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(mConnectedWifiInfo.getSSID(), HwDualBandStateMachine.this.mHwDualBandInformationManager.getAuthType(mConnectedWifiInfo.getNetworkId()));
                    } else {
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.saveAPInfo();
                        HwDualBandStateMachine.this.mHwDualBandRelationManager.updateAPRelation();
                    }
                }
            } else if (HwDualBandBlackListManager.getHwDualBandBlackListMgrInstance().getWifiBlacklist().isEmpty() && HwDualBandStateMachine.this.mWifiManager != null) {
                List<ScanResult> mLists2 = HwDualBandStateMachine.this.mWifiManager.getScanResults();
                if (mLists2 != null && mLists2.size() > 0 && is5gApAvailble(mLists2)) {
                    Log.d(HwDualBandMessageUtil.TAG, "InternetReadyState MSG_WIFI_UPDATE_SCAN_RESULT");
                    HwDualBandStateMachine.this.sendMessage(104);
                }
            }
            return true;
        }

        private boolean is5gApAvailble(List<ScanResult> scanResults) {
            if (scanResults == null || HwDualBandStateMachine.this.mWifiManager == null) {
                return false;
            }
            List<WifiConfiguration> configNetworks = HwDualBandStateMachine.this.mWifiManager.getConfiguredNetworks();
            int scanResultsSize = scanResults.size();
            for (int i = 0; i < scanResultsSize; i++) {
                ScanResult nextResult = (ScanResult) scanResults.get(i);
                if (!(nextResult == null || configNetworks == null || !nextResult.is5GHz())) {
                    int signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                    int configNetworksSize = configNetworks.size();
                    if (signalLevel >= 3) {
                        for (int k = 0; k < configNetworksSize; k++) {
                            WifiConfiguration nextConfig = (WifiConfiguration) configNetworks.get(k);
                            String scanSsid = new StringBuilder();
                            scanSsid.append("\"");
                            scanSsid.append(nextResult.SSID);
                            scanSsid.append("\"");
                            boolean networkMatched = nextConfig != null && nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid.toString()) && WifiProCommonUtils.isSameEncryptType(nextResult.capabilities, nextConfig.configKey());
                            if (networkMatched && !nextConfig.noInternetAccess && !WifiProCommonUtils.isOpenAndPortal(nextConfig)) {
                                return true;
                            }
                        }
                        continue;
                    } else {
                        continue;
                    }
                }
            }
            return false;
        }

        private boolean isNearAP(WifiProRelateApRcd record) {
            if (record.mMaxRelatedRSSI == 0 || record.mMinCurrentRSSI == 0) {
                if (record.mMaxCurrentRSSI - record.mMinRelatedRSSI <= 10) {
                    return true;
                }
            } else if (record.mMaxCurrentRSSI - record.mMinRelatedRSSI <= 10 && record.mMaxRelatedRSSI - record.mMinCurrentRSSI <= 10) {
                return true;
            }
            return false;
        }

        private boolean isMobileAP() {
            if (HwDualBandStateMachine.this.mContext != null) {
                return HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwDualBandStateMachine.this.mContext);
            }
            return false;
        }
    }

    class MonitorState extends State {
        private String m24GBssid = null;
        private int m24GRssi = -1;
        private int mScanIndex = 0;

        MonitorState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter MonitorState");
            if (HwDualBandStateMachine.this.mWifiScanner == null) {
                HwDualBandStateMachine.this.mWifiScanner = WifiInjector.getInstance().getWifiScanner();
            }
            HwDualBandStateMachine.this.mIsDualbandScanning = true;
            this.mScanIndex = 0;
            HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
            HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
            HwDualBandStateMachine.this.mCHRScanAPType = 0;
            this.m24GBssid = null;
            this.m24GRssi = -1;
            HwDualBandStateMachine.this.mDisappearAPList.clear();
            WifiInfo mWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null) {
                Log.e(HwDualBandMessageUtil.TAG, "mWifiInfo is null");
                return;
            }
            this.m24GBssid = mWifiInfo.getBSSID();
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mTargetAPList) {
                String str;
                StringBuilder stringBuilder;
                if (info.mDualBandApInfoRcd == null) {
                    str = HwDualBandMessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MonitorState info.mDualBandApInfoRcd == null, ssid = ");
                    stringBuilder.append(info.mSsid);
                    Log.e(str, stringBuilder.toString());
                } else if (info.mIsDualbandAP == 1) {
                    info.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(mWifiInfo.getBSSID(), info.mBssid, info.mTargetRssi);
                    str = HwDualBandMessageUtil.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MonitorState isdulabanAP 2.4G = ");
                    stringBuilder.append(mWifiInfo.getSSID());
                    stringBuilder.append(" 5G = ");
                    stringBuilder.append(info.mSsid);
                    stringBuilder.append(" info.mScanRssi = ");
                    stringBuilder.append(info.mScanRssi);
                    stringBuilder.append(" info.mTargetRssi = ");
                    stringBuilder.append(info.mTargetRssi);
                    Log.e(str, stringBuilder.toString());
                    HwDualBandStateMachine.this.mMonitorAPList.add(info);
                } else {
                    WifiProDualBandApInfoRcd APInfo = null;
                    WifiProRelateApRcd RelationInfo = null;
                    if (mWifiInfo.getBSSID() != null) {
                        APInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mWifiInfo.getBSSID());
                        RelationInfo = HwDualBandStateMachine.this.mHwDualBandRelationManager.getRelateAPInfo(mWifiInfo.getBSSID(), info.mBssid);
                    }
                    if (RelationInfo != null) {
                        if (APInfo != null) {
                            if (info.mIsNearAP != 1 || RelationInfo.mMinCurrentRSSI == 0) {
                                info.mScanRssi = RelationInfo.mMaxCurrentRSSI;
                            } else {
                                info.mScanRssi = info.mTargetRssi - 5;
                            }
                            info.mInitializationRssi = info.mScanRssi;
                            String str2 = HwDualBandMessageUtil.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("MonitorState mix AP 2.4G = ");
                            stringBuilder2.append(mWifiInfo.getSSID());
                            stringBuilder2.append(" 5G = ");
                            stringBuilder2.append(info.mSsid);
                            stringBuilder2.append(" info.mScanRssi = ");
                            stringBuilder2.append(info.mScanRssi);
                            stringBuilder2.append(" info.mTargetRssi = ");
                            stringBuilder2.append(info.mTargetRssi);
                            stringBuilder2.append(" info.mAuthType = ");
                            stringBuilder2.append(info.mAuthType);
                            stringBuilder2.append(" info.mIsNearAP = ");
                            stringBuilder2.append(info.mIsNearAP);
                            stringBuilder2.append(" APInfo.mChannelFrequency = ");
                            stringBuilder2.append(APInfo.mChannelFrequency);
                            stringBuilder2.append(" APInfo.mApAuthType = ");
                            stringBuilder2.append(APInfo.mApAuthType);
                            Log.e(str2, stringBuilder2.toString());
                            HwDualBandStateMachine.this.mMonitorAPList.add(info);
                        }
                    }
                }
            }
            if (HwDualBandStateMachine.this.mMonitorAPList.size() <= 0) {
                HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(0, HwDualBandStateMachine.this.mMonitorAPList);
                HwDualBandStateMachine.this.sendMessage(103);
            }
        }

        public void exit() {
            HwDualBandStateMachine.this.mIsDualbandScanning = false;
            this.mScanIndex = 0;
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
            HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
            HwDualBandStateMachine.this.mCHRScanAPType = 0;
        }

        public boolean processMessage(Message message) {
            boolean sceneLimited = false;
            String bssid;
            String ssid;
            switch (message.what) {
                case 2:
                    HwDualBandStateMachine.this.initDualbandChrHandoverTooSlow(this.m24GBssid, this.m24GRssi);
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mDisconnectedState);
                    break;
                case 7:
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_UPDATE_SCAN_RESULT");
                    List<ScanResult> mLists = HwDualBandStateMachine.this.mWifiManager.getScanResults();
                    if (mLists != null && mLists.size() > 0 && isSatisfiedScanResult(mLists)) {
                        Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_UPDATE_SCAN_RESULT find AP");
                        HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mInternetReadyState);
                        break;
                    }
                case 8:
                    Bundle remove_data = message.getData();
                    bssid = remove_data.getString("bssid");
                    ssid = remove_data.getString("ssid");
                    int authtype = remove_data.getInt(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE);
                    String str = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MonitorState MSG_WIFI_REMOVE_CONFIG_CHANGED ssid = ");
                    stringBuilder.append(ssid);
                    Log.e(str, stringBuilder.toString());
                    if (ssid != null) {
                        HwDualBandStateMachine.this.mHwDualBandInformationManager.delectDualBandAPInfoBySsid(ssid, authtype);
                        removeFromMonitorList(ssid);
                        ArrayList<HwDualBandMonitorInfo> tmpMonitorList = (ArrayList) HwDualBandStateMachine.this.mMonitorAPList;
                        String str2 = HwDualBandMessageUtil.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("MonitorState MSG_WIFI_REMOVE_CONFIG_CHANGED mMonitorAPList.size() = ");
                        stringBuilder2.append(HwDualBandStateMachine.this.mMonitorAPList.size());
                        Log.e(str2, stringBuilder2.toString());
                        HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkType(0, (ArrayList) tmpMonitorList.clone());
                        HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mInternetReadyState);
                        break;
                    }
                    break;
                case 18:
                    this.m24GRssi = message.getData().getInt(HwDualBandMessageUtil.MSG_KEY_RSSI);
                    if (isFullscreen() || WifiProCommonUtils.isCalling(HwDualBandStateMachine.this.mContext) || WifiProCommonUtils.isLandscapeMode(HwDualBandStateMachine.this.mContext) || !HwDualBandStateMachine.this.mPowerManager.isScreenOn() || !isSuppOnCompletedState()) {
                        sceneLimited = true;
                    }
                    bssid = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("MonitorState m24GRssi = ");
                    stringBuilder3.append(this.m24GRssi);
                    stringBuilder3.append(" , sceneLimited = ");
                    stringBuilder3.append(sceneLimited);
                    Log.d(bssid, stringBuilder3.toString());
                    if (!sceneLimited && isSatisfiedScanCondition(this.m24GRssi)) {
                        if (this.mScanIndex >= 3 || WifiProCommonUtils.isQueryActivityMatched(HwDualBandStateMachine.this.mContext, "com.android.settings.Settings$WifiSettingsActivity")) {
                            bssid = HwDualBandMessageUtil.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("startScan for full channels, mScanIndex = ");
                            stringBuilder3.append(this.mScanIndex);
                            Log.e(bssid, stringBuilder3.toString());
                            HwDualBandStateMachine.this.mWifiManager.startScan();
                        } else {
                            ScanSettings settings = getCustomizedScanSettings();
                            ssid = HwDualBandMessageUtil.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("startScan for restrict channels, mScanIndex = ");
                            stringBuilder4.append(this.mScanIndex);
                            Log.e(ssid, stringBuilder4.toString());
                            if (settings != null) {
                                HwDualBandStateMachine.this.startCustomizedScan(settings);
                            } else {
                                HwDualBandStateMachine.this.mWifiManager.startScan();
                            }
                        }
                        this.mScanIndex++;
                        int i = 5;
                        if (this.mScanIndex <= 5) {
                            i = this.mScanIndex;
                        }
                        this.mScanIndex = i;
                        if (HwDualBandStateMachine.this.mCHRScanAPType != 1) {
                            if (HwDualBandStateMachine.this.mCHRScanAPType == 2) {
                                HwDualBandStateMachine.this.mCHRMixAPScanCount = HwDualBandStateMachine.this.mCHRMixAPScanCount + 1;
                                break;
                            }
                        }
                        HwDualBandStateMachine.this.mCHRSingleAPScanCount = HwDualBandStateMachine.this.mCHRSingleAPScanCount + 1;
                        break;
                    }
                    break;
                case 19:
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState MSG_WIFI_VERIFYING_POOR_LINK");
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mConnectedState);
                    break;
                case 103:
                    Log.e(HwDualBandMessageUtil.TAG, "MonitorState CMD_STOP_MONITOR");
                    HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mInternetReadyState);
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean isFullscreen() {
            AbsPhoneWindowManager policy = (AbsPhoneWindowManager) LocalServices.getService(WindowManagerPolicy.class);
            return policy != null && policy.isTopIsFullscreen();
        }

        private boolean isSuppOnCompletedState() {
            WifiInfo info = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (info == null || info.getSupplicantState().ordinal() != SupplicantState.COMPLETED.ordinal()) {
                return false;
            }
            return true;
        }

        private boolean isSatisfiedScanCondition(int rssi) {
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mMonitorAPList) {
                HwDualBandStateMachine.this.mCHRScanAPType = info.mIsDualbandAP;
                if (info.mIsNearAP == 1) {
                    if (rssi >= info.mScanRssi) {
                        return true;
                    }
                } else if (rssi <= -90 || rssi > info.mInitializationRssi) {
                    info.mScanRssi = info.mInitializationRssi;
                    return false;
                } else if (rssi <= info.mScanRssi) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSatisfiedScanResult(List<ScanResult> mLists) {
            int scanResultsFound = 0;
            List<HwDualBandMonitorInfo> mAPList = new ArrayList();
            for (ScanResult result : mLists) {
                if (this.m24GBssid != null && this.m24GBssid.equals(result.BSSID)) {
                    this.m24GRssi = result.level;
                    String str = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("isSatisfiedScanResult m24GRssi = ");
                    stringBuilder.append(this.m24GRssi);
                    Log.e(str, stringBuilder.toString());
                }
                if (HwDualBandStateMachine.this.mDisappearAPList.size() > 0) {
                    for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mDisappearAPList) {
                        if (info.mBssid.equals(result.BSSID) && result.SSID != null && result.SSID.length() > 0 && !isInMonitorList(info)) {
                            HwDualBandStateMachine.this.mMonitorAPList.add(info);
                        }
                    }
                }
            }
            if (this.m24GRssi == -1) {
                Log.e(HwDualBandMessageUtil.TAG, "isSatisfiedScanResult m24GBssid == -1");
                return false;
            }
            for (HwDualBandMonitorInfo info2 : HwDualBandStateMachine.this.mMonitorAPList) {
                boolean isMonitorAPFound = false;
                for (ScanResult result2 : mLists) {
                    if (info2.mBssid.equals(result2.BSSID) && result2.SSID != null && result2.SSID.length() > 0) {
                        String str2 = HwDualBandMessageUtil.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("isSatisfiedScanResult result.SSID = ");
                        stringBuilder2.append(result2.SSID);
                        stringBuilder2.append(" result.level = ");
                        stringBuilder2.append(result2.level);
                        Log.e(str2, stringBuilder2.toString());
                        isMonitorAPFound = true;
                        scanResultsFound++;
                        str2 = new StringBuilder();
                        str2.append("\"");
                        str2.append(result2.SSID);
                        str2.append("\"");
                        str2 = str2.toString();
                        if (!(result2.frequency == info2.mDualBandApInfoRcd.mChannelFrequency && info2.mSsid.equals(str2))) {
                            String str3 = HwDualBandMessageUtil.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("isSatisfiedScanResult update AP frequency mChannelFrequency = ");
                            stringBuilder3.append(info2.mDualBandApInfoRcd.mChannelFrequency);
                            stringBuilder3.append(" new frequency = ");
                            stringBuilder3.append(result2.frequency);
                            Log.e(str3, stringBuilder3.toString());
                            info2.mDualBandApInfoRcd.mChannelFrequency = result2.frequency;
                            info2.mDualBandApInfoRcd.mApSSID = str2;
                            info2.mSsid = str2;
                            HwDualBandInformationManager.getInstance().updateAPInfo(info2.mDualBandApInfoRcd);
                        }
                        if (info2.mIsDualbandAP == 1) {
                            processSingleAPResult(info2, result2, mAPList);
                        } else {
                            processMixAPResult(info2, result2, mAPList);
                        }
                    }
                }
                if (!isMonitorAPFound && info2.mIsDualbandAP == 2) {
                    info2.mScanRssi = updateScanBssid(info2, WifiHandover.INVALID_RSSI);
                }
                if (this.m24GRssi > info2.mScanRssi && info2.mIsDualbandAP == 1 && !isMonitorAPFound) {
                    HwDualBandStateMachine.this.addDisappearAPList(info2);
                    HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.updateRSSIThreshold(this.m24GBssid, info2.mBssid, this.m24GRssi, -127, info2.mScanRssi, info2.mTargetRssi);
                    info2.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(this.m24GBssid, info2.mBssid, info2.mTargetRssi);
                    String str4 = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("isSatisfiedScanResult renew info.mSsid = ");
                    stringBuilder4.append(info2.mSsid);
                    stringBuilder4.append(" info.mScanRssi = ");
                    stringBuilder4.append(info2.mScanRssi);
                    stringBuilder4.append(" info.mTargetRssi = ");
                    stringBuilder4.append(info2.mTargetRssi);
                    Log.e(str4, stringBuilder4.toString());
                }
            }
            String str5 = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("isSatisfiedScanResult mMonitorAPList.size = ");
            stringBuilder5.append(HwDualBandStateMachine.this.mMonitorAPList.size());
            stringBuilder5.append(", scanResultsFound = ");
            stringBuilder5.append(scanResultsFound);
            stringBuilder5.append(", mScanIndex = ");
            stringBuilder5.append(this.mScanIndex);
            stringBuilder5.append(", mAPList.size = ");
            stringBuilder5.append(mAPList.size());
            stringBuilder5.append(", mDisappearAPList.size() = ");
            stringBuilder5.append(HwDualBandStateMachine.this.mDisappearAPList.size());
            Log.e(str5, stringBuilder5.toString());
            if (this.mScanIndex >= 5) {
                HwDualBandStateMachine.this.mCHRSingleAPScanCount = 0;
                HwDualBandStateMachine.this.mCHRMixAPScanCount = 0;
                HwDualBandStateMachine.this.mCHRScanAPType = 0;
                updateAPInfo(HwDualBandStateMachine.this.mDisappearAPList);
                HwDualBandStateMachine.this.sendMessage(103);
                return false;
            }
            if (scanResultsFound == HwDualBandStateMachine.this.mMonitorAPList.size()) {
                this.mScanIndex = 0;
            }
            str5 = HwDualBandMessageUtil.TAG;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append("isSatisfiedScanResult mAPList.size() = ");
            stringBuilder5.append(mAPList.size());
            stringBuilder5.append(", scanResultsFound = ");
            stringBuilder5.append(scanResultsFound);
            stringBuilder5.append(", mScanIndex = ");
            stringBuilder5.append(this.mScanIndex);
            Log.e(str5, stringBuilder5.toString());
            if (mAPList.size() <= 0) {
                return false;
            }
            HwDualBandStateMachine.this.mIDualbandManagerCallback.onDualBandNetWorkFind(mAPList);
            HwDualBandStateMachine.this.sendMessage(103);
            return true;
        }

        private void processSingleAPResult(HwDualBandMonitorInfo info, ScanResult result, List<HwDualBandMonitorInfo> mAPList) {
            String str;
            StringBuilder stringBuilder;
            if (this.m24GRssi >= info.mScanRssi) {
                HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.updateRSSIThreshold(this.m24GBssid, result.BSSID, this.m24GRssi, result.level, info.mScanRssi, info.mTargetRssi);
                info.mScanRssi = HwDualBandStateMachine.this.mHwDualBandAdaptiveThreshold.getScanRSSIThreshold(this.m24GBssid, result.BSSID, info.mTargetRssi);
                str = HwDualBandMessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processSingleAPResult renew info.mSsid = ");
                stringBuilder.append(info.mSsid);
                stringBuilder.append(" info.mScanRssi = ");
                stringBuilder.append(info.mScanRssi);
                stringBuilder.append(" info.mTargetRssi = ");
                stringBuilder.append(info.mTargetRssi);
                Log.e(str, stringBuilder.toString());
            }
            if (result.level >= info.mTargetRssi) {
                info.mCurrentRssi = result.level;
                mAPList.add(info);
                str = HwDualBandMessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processSingleAPResult info.mSsid = ");
                stringBuilder.append(info.mSsid);
                stringBuilder.append(" info.mCurrentRssi = ");
                stringBuilder.append(info.mCurrentRssi);
                stringBuilder.append(" info.mTargetRssi = ");
                stringBuilder.append(info.mTargetRssi);
                Log.e(str, stringBuilder.toString());
            }
            if (info.mDualBandApInfoRcd.mDisappearCount > 0) {
                str = HwDualBandMessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isSatisfiedScanResult update AP disappear number = ");
                stringBuilder.append(info.mDualBandApInfoRcd.mDisappearCount);
                stringBuilder.append(" --> 0");
                Log.e(str, stringBuilder.toString());
                info.mDualBandApInfoRcd.mDisappearCount = 0;
                HwDualBandInformationManager.getInstance().updateAPInfo(info.mDualBandApInfoRcd);
            }
        }

        private void processMixAPResult(HwDualBandMonitorInfo info, ScanResult result, List<HwDualBandMonitorInfo> mAPList) {
            String str;
            StringBuilder stringBuilder;
            if (result.level >= info.mTargetRssi) {
                info.mCurrentRssi = result.level;
                mAPList.add(info);
                str = HwDualBandMessageUtil.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processMixAPResult info.mSsid = ");
                stringBuilder.append(info.mSsid);
                stringBuilder.append(" info.mCurrentRssi = ");
                stringBuilder.append(info.mCurrentRssi);
                stringBuilder.append(" info.mTargetRssi = ");
                stringBuilder.append(info.mTargetRssi);
                Log.e(str, stringBuilder.toString());
                return;
            }
            info.mScanRssi = updateScanBssid(info, result.level);
            str = HwDualBandMessageUtil.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("processMixAPResult renew info.mSsid = ");
            stringBuilder.append(info.mSsid);
            stringBuilder.append(" info.mScanRssi = ");
            stringBuilder.append(info.mScanRssi);
            stringBuilder.append(" info.mTargetRssi = ");
            stringBuilder.append(info.mTargetRssi);
            Log.e(str, stringBuilder.toString());
        }

        /* JADX WARNING: Missing block: B:18:0x009b, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private ScanSettings getCustomizedScanSettings() {
            WifiInfo mWifiInfo = HwDualBandStateMachine.this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null || mWifiInfo.getBSSID() == null || HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(mWifiInfo.getBSSID()) == null) {
                return null;
            }
            ScanSettings settings = new ScanSettings();
            settings.band = 0;
            settings.reportEvents = 3;
            settings.numBssidsPerScan = 0;
            settings.channels = new ChannelSpec[HwDualBandStateMachine.this.mMonitorAPList.size()];
            int index = 0;
            for (HwDualBandMonitorInfo record : HwDualBandStateMachine.this.mMonitorAPList) {
                WifiProDualBandApInfoRcd scanAPInfo = HwDualBandStateMachine.this.mHwDualBandInformationManager.getDualBandAPInfo(record.mBssid);
                if (scanAPInfo != null) {
                    int index2 = index + 1;
                    settings.channels[index] = new ChannelSpec(scanAPInfo.mChannelFrequency);
                    String str = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getCustomizedScanSettings:  Frequency = ");
                    stringBuilder.append(scanAPInfo.mChannelFrequency);
                    Log.d(str, stringBuilder.toString());
                    index = index2;
                }
            }
            if (index != HwDualBandStateMachine.this.mMonitorAPList.size()) {
                return null;
            }
            return settings;
        }

        private int updateScanBssid(HwDualBandMonitorInfo info, int rssi) {
            int targetScanRssi;
            if (info.mIsNearAP == 1) {
                targetScanRssi = info.mScanRssi + 5;
            } else {
                targetScanRssi = info.mScanRssi - 5;
            }
            if (targetScanRssi >= HwDualBandStateMachine.WIFI_MAX_SCAN_THRESHOLD || targetScanRssi <= -90) {
                targetScanRssi = info.mInitializationRssi;
            }
            String str = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateScanBssid targetScanRssi = ");
            stringBuilder.append(targetScanRssi);
            stringBuilder.append(" scanRssi = ");
            stringBuilder.append(info.mScanRssi);
            stringBuilder.append(" mInitializationRssi = ");
            stringBuilder.append(info.mInitializationRssi);
            stringBuilder.append(" rssi = ");
            stringBuilder.append(rssi);
            Log.e(str, stringBuilder.toString());
            return targetScanRssi;
        }

        private void removeFromMonitorList(String ssid) {
            List<HwDualBandMonitorInfo> delectList = new ArrayList();
            for (HwDualBandMonitorInfo info : HwDualBandStateMachine.this.mMonitorAPList) {
                if (info.mSsid.equals(ssid)) {
                    delectList.add(info);
                }
            }
            for (HwDualBandMonitorInfo info2 : delectList) {
                HwDualBandStateMachine.this.mMonitorAPList.remove(info2);
            }
        }

        private void updateAPInfo(List<HwDualBandMonitorInfo> disappearAPList) {
            for (HwDualBandMonitorInfo info : disappearAPList) {
                if (info.mDualBandApInfoRcd != null) {
                    WifiProDualBandApInfoRcd wifiProDualBandApInfoRcd = info.mDualBandApInfoRcd;
                    wifiProDualBandApInfoRcd.mDisappearCount++;
                    String str = HwDualBandMessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAPInfo info.mSsid = ");
                    stringBuilder.append(info.mSsid);
                    stringBuilder.append(", info.mDualBandApInfoRcd.mDisappearCount = ");
                    stringBuilder.append(info.mDualBandApInfoRcd.mDisappearCount);
                    Log.e(str, stringBuilder.toString());
                    if (info.mDualBandApInfoRcd.mDisappearCount > 3) {
                        HwDualBandInformationManager.getInstance().delectDualBandAPInfoBySsid(info.mSsid, info.mAuthType);
                    } else {
                        HwDualBandInformationManager.getInstance().updateAPInfo(info.mDualBandApInfoRcd);
                    }
                }
                removeFromMonitorList(info.mSsid);
            }
        }

        private boolean isInMonitorList(HwDualBandMonitorInfo info) {
            if (HwDualBandStateMachine.this.mMonitorAPList.size() <= 0) {
                return false;
            }
            for (HwDualBandMonitorInfo monitorInfo : HwDualBandStateMachine.this.mMonitorAPList) {
                if (monitorInfo.mBssid.equals(info.mBssid)) {
                    return true;
                }
            }
            return false;
        }
    }

    class StopState extends State {
        StopState() {
        }

        public void enter() {
            Log.e(HwDualBandMessageUtil.TAG, "Enter StopState");
            if (!(HwDualBandStateMachine.this.mMonitorAPList == null || HwDualBandStateMachine.this.mMonitorAPList.size() == 0)) {
                HwDualBandStateMachine.this.mMonitorAPList.clear();
            }
            if (HwDualBandStateMachine.this.mTargetAPList != null && HwDualBandStateMachine.this.mTargetAPList.size() != 0) {
                HwDualBandStateMachine.this.mTargetAPList.clear();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what == 100) {
                HwDualBandStateMachine.this.transitionTo(HwDualBandStateMachine.this.mDefaultState);
            }
            return true;
        }
    }

    public HwDualBandStateMachine(Context context, IDualBandManagerCallback callBack) {
        super("HwDualBandStateMachine");
        this.mContext = context;
        this.mIDualbandManagerCallback = callBack;
        Context context2 = this.mContext;
        Context context3 = this.mContext;
        this.mWifiManager = (WifiManager) context2.getSystemService("wifi");
        this.mHwDualBandWiFiMonitor = new HwDualBandWiFiMonitor(context, getHandler());
        this.mHwDualBandInformationManager = HwDualBandInformationManager.createInstance(context);
        this.mHwDualBandRelationManager = HwDualBandRelationManager.createInstance(context, getHandler());
        this.mHwDualBandAdaptiveThreshold = new HwDualBandAdaptiveThreshold(context);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        addState(this.mDefaultState);
        addState(this.mDisabledState, this.mDefaultState);
        addState(this.mConnectedState, this.mDefaultState);
        addState(this.mMonitorState, this.mConnectedState);
        addState(this.mInternetReadyState, this.mConnectedState);
        addState(this.mDisconnectedState, this.mDefaultState);
        addState(this.mStopState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        start();
    }

    public void onStart() {
        getHandler().sendEmptyMessage(100);
        this.mHwDualBandWiFiMonitor.startMonitor();
    }

    public void onStop() {
        this.mHwDualBandWiFiMonitor.stopMonitor();
        getHandler().sendEmptyMessage(101);
    }

    public Handler getStateMachineHandler() {
        return getHandler();
    }

    public boolean isDualbandScanning() {
        return this.mIsDualbandScanning;
    }

    private void initDualbandChrHandoverTooSlow(String ssid, int rssi) {
        if (this.mCHRHandoverTooSlow != null) {
            this.mCHRHandoverTooSlow.mSSID_2G = ssid;
            this.mCHRHandoverTooSlow.mRSSI_2G = (short) rssi;
            String str = HwDualBandMessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("db_chr initDualbandChrHandoverTooSlow mCHR24GSsid");
            stringBuilder.append(ssid);
            stringBuilder.append(", m24GRssi = ");
            stringBuilder.append(this.mCHRHandoverTooSlow.mRSSI_2G);
            Log.e(str, stringBuilder.toString());
            for (HwDualBandMonitorInfo info : this.mMonitorAPList) {
                this.mCHRSavedAPList.add(info);
            }
        }
    }

    private void startCustomizedScan(ScanSettings requested) {
        this.mWifiScanner.startScan(requested, this.mCustomizedScanListener, null);
    }

    private void addDisappearAPList(HwDualBandMonitorInfo info) {
        boolean addFlag = true;
        for (HwDualBandMonitorInfo data : this.mDisappearAPList) {
            if (data.mBssid.equals(info.mBssid)) {
                addFlag = false;
                break;
            }
        }
        if (addFlag) {
            this.mDisappearAPList.add(info);
        }
    }
}

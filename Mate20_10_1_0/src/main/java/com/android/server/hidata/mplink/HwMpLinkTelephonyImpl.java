package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkTelephonyImpl {
    private static final String CONNECTED_FIELD = "CONNECTED";
    private static final int DEFAULT_DIVISOR = 2;
    private static final int DEFAULT_SWITCH_STATE = -1;
    private static final String DISCONNECTED_FIELD = "DISCONNECTED";
    private static final int FREQUENCY_UNIT = 10;
    public static final float INTER_DISTURB_BANDWIDTH_RATE = 0.1f;
    private static final int INVALID_SUB_ID = -1;
    private static final int KB_TO_MB_UNIT = 1000;
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 60000;
    private static final int SECOND_HARMONIC = 2;
    private static final String TAG = "HiData_HwMpLinkTelephonyImpl";
    private static final int THIRD_HARMONIC = 3;
    private int dealMobileDataRef = 0;
    private boolean isCurrentDataRoaming = false;
    private boolean isDataTechSuitable = false;
    private boolean isMobileConnected = false;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    private float mCurrentDataBandWidth = 0.0f;
    private float mCurrentDataFreq = 0.0f;
    private int mCurrentDataTechType = 0;
    private int mCurrentServiceState = 1;
    private int mCurrentWifiBandWidth = 0;
    private int mCurrentWifiFreq = 0;
    private Handler mHandler;
    private boolean mIsInterDisturbExist = false;
    /* access modifiers changed from: private */
    public boolean mIsMobileDataAvailable = false;
    private final ConnectivityManager.NetworkCallback mListenNetworkCallback = new ConnectivityManager.NetworkCallback() {
        /* class com.android.server.hidata.mplink.HwMpLinkTelephonyImpl.AnonymousClass1 */

        public void onAvailable(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, boolean isBlocked) {
            String iface = linkProperties.getInterfaceName();
            MpLinkCommonUtils.logD(HwMpLinkTelephonyImpl.TAG, false, "onAvailable,iface:%{public}s", new Object[]{iface});
            if (iface != null) {
                boolean unused = HwMpLinkTelephonyImpl.this.mIsMobileDataAvailable = true;
                HwMpLinkTelephonyImpl.this.sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE);
            }
        }

        public void onUnavailable() {
            MpLinkCommonUtils.logD(HwMpLinkTelephonyImpl.TAG, false, "onUnavailable", new Object[0]);
        }
    };
    private int mMobileDataSwitchState = -1;
    private String mMobileIface = "";
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    NetworkRequest mNetworkRequestForCallback = new NetworkRequest.Builder().addTransportType(0).addCapability(12).build();
    private int mReportRat = 0;

    public HwMpLinkTelephonyImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        getConnectivityManger();
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(this.mNetworkRequestForCallback, this.mListenNetworkCallback, this.mHandler);
        }
    }

    private void getConnectivityManger() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    /* access modifiers changed from: private */
    public void sendMessage(int what) {
        Handler handler = this.mHandler;
        handler.sendMessage(Message.obtain(handler, what));
    }

    public int getActiveNetworkType() {
        NetworkInfo netInfo;
        getConnectivityManger();
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager == null || (netInfo = connectivityManager.getActiveNetworkInfo()) == null) {
            return -1;
        }
        return netInfo.getType();
    }

    public boolean isMobileDataEnable() {
        boolean isMobileEnable = false;
        getConnectivityManger();
        ConnectivityManager connectivityManager = this.mConnectivityManager;
        if (connectivityManager != null) {
            isMobileEnable = connectivityManager.getMobileDataEnabled();
        } else {
            MpLinkCommonUtils.logD(TAG, false, "isMobileDataEnable mConnectivityManager is null", new Object[0]);
        }
        MpLinkCommonUtils.logD(TAG, false, "isMobileDataEnable %{public}s", new Object[]{String.valueOf(isMobileEnable)});
        return isMobileEnable;
    }

    public void closeMobileDataIfOpened() {
        MpLinkCommonUtils.logD(TAG, false, "closeMobileDataIfOpened dealMobileDataRef:" + this.dealMobileDataRef, new Object[0]);
        if (this.dealMobileDataRef > 0) {
            mpLinkSetMobileData(false);
        }
    }

    public void setDealMobileDataRef(boolean isEnabled) {
        if (this.dealMobileDataRef == 0 && isEnabled) {
            this.dealMobileDataRef = 1;
        }
    }

    public int mpLinkSetMobileData(boolean isEnabled) {
        MpLinkCommonUtils.logD(TAG, false, "mpLinkSetMobileData dealMobileDataRef: %{public}d, enable :%{public}s", new Object[]{Integer.valueOf(this.dealMobileDataRef), String.valueOf(isEnabled)});
        if (!isMobileDataEnable() && isEnabled) {
            MpLinkCommonUtils.logD(TAG, false, "mpLinkSetMobileData Mobile switch closed", new Object[0]);
            return 2;
        } else if (isEnabled) {
            this.dealMobileDataRef = 1;
            enableMobileData(true);
            return 0;
        } else if (this.dealMobileDataRef > 0) {
            enableMobileData(false);
            this.dealMobileDataRef = 0;
            return -1;
        } else {
            MpLinkCommonUtils.logD(TAG, false, "mpLinkSetMobileData mplink do not open MobileData", new Object[0]);
            return -1;
        }
    }

    private void enableMobileData(boolean isEnabled) {
        MpLinkCommonUtils.logD(TAG, false, "enableMobileData :%{public}s", new Object[]{String.valueOf(isEnabled)});
        if (isEnabled) {
            startNetworkForMpLink();
        } else {
            stopNetworkForMpLink();
        }
    }

    public boolean getCurrentDataTechSuitable() {
        return this.isDataTechSuitable;
    }

    private void startNetworkForMpLink() {
        ConnectivityManager connectivityManager;
        MpLinkCommonUtils.logD(TAG, false, "startNetworkForMpLink", new Object[0]);
        NetworkRequest mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(12).build();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback();
        ConnectivityManager.NetworkCallback networkCallback = this.mNetworkCallback;
        if (networkCallback != null && (connectivityManager = this.mConnectivityManager) != null) {
            connectivityManager.requestNetwork(mNetworkRequest, networkCallback, 60000);
        }
    }

    private void stopNetworkForMpLink() {
        ConnectivityManager connectivityManager;
        MpLinkCommonUtils.logD(TAG, false, "stopNetworkForMpLink", new Object[0]);
        ConnectivityManager.NetworkCallback networkCallback = this.mNetworkCallback;
        if (networkCallback != null && (connectivityManager = this.mConnectivityManager) != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException e) {
                MpLinkCommonUtils.logD(TAG, false, "Unregister network callback exception", new Object[0]);
            } catch (Throwable th) {
                this.mNetworkCallback = null;
                throw th;
            }
            this.mNetworkCallback = null;
        }
    }

    public int getDefaultDataSubId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    public boolean getCurrentDataRoamingState() {
        return this.isCurrentDataRoaming;
    }

    public int getCurrentServceState() {
        return this.mCurrentServiceState;
    }

    public boolean getCurrentMobileConnectState() {
        return this.isMobileConnected;
    }

    public void handleDataSubChange(int subId) {
        MpLinkCommonUtils.logD(TAG, false, "DataSub Change, new subId:%{public}d", new Object[]{Integer.valueOf(subId)});
        if (subId != -1) {
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_DATA_SUB_CHANGE);
        }
    }

    public int getCurrentDataTech() {
        return this.mCurrentDataTechType;
    }

    public boolean isFreqInterDisturbExist() {
        return this.mIsInterDisturbExist;
    }

    public void calculateInterDisturb() {
        this.mIsInterDisturbExist = harmonicInterDisturb(2) || harmonicInterDisturb(3);
    }

    public boolean harmonicInterDisturb(int num) {
        float intersectLowFreq;
        float intersectHighFreq;
        MpLinkCommonUtils.logD(TAG, false, "Enter harmonicInterDisturb: mCurrentWifiFreq is %{public}d,mCurrentWifiBandWidth is %{public}d ,mCurrentDataFreq is %{public}f ,mCurrentDataBandWidth is %{public}f", new Object[]{Integer.valueOf(this.mCurrentWifiFreq), Integer.valueOf(this.mCurrentWifiBandWidth), Float.valueOf(this.mCurrentDataFreq), Float.valueOf(this.mCurrentDataBandWidth)});
        if (this.mReportRat != 0) {
            float f = this.mCurrentDataFreq;
            if (f != 0.0f) {
                float f2 = this.mCurrentDataBandWidth;
                if (f2 != 0.0f) {
                    int i = this.mCurrentWifiBandWidth;
                    if (((float) i) > 0.0f) {
                        int i2 = this.mCurrentWifiFreq;
                        float wifiLowFreq = (float) (i2 - (i / 2));
                        float wifiHighFreq = (float) (i2 + (i / 2));
                        float dataLowFreq = (((float) num) * f) - ((((float) num) * f2) / 2.0f);
                        float dataHighFreq = (f * ((float) num)) + ((f2 * ((float) num)) / 2.0f);
                        if (wifiHighFreq <= dataLowFreq || dataHighFreq <= wifiLowFreq) {
                            return false;
                        }
                        if (Float.compare(wifiLowFreq, dataLowFreq) > 0) {
                            intersectLowFreq = wifiLowFreq;
                        } else {
                            intersectLowFreq = dataLowFreq;
                        }
                        if (Float.compare(wifiHighFreq, dataHighFreq) > 0) {
                            intersectHighFreq = dataHighFreq;
                        } else {
                            intersectHighFreq = wifiHighFreq;
                        }
                        if ((intersectHighFreq - intersectLowFreq) / ((float) this.mCurrentWifiBandWidth) >= 0.1f) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public void upDataCellUlFreqInfo(HwMpLinkInterDisturbInfo disturbInfo) {
        this.mReportRat = disturbInfo.mRat;
        this.mCurrentDataBandWidth = ((float) disturbInfo.mUlbw) / 1000.0f;
        this.mCurrentDataFreq = ((float) disturbInfo.mUlfreq) / 10.0f;
        MpLinkCommonUtils.logD(TAG, false, "upDataCellUlFreqInfo, mReportRat = %{public}d, mCurrentDataBandWidth = %{public}f , mCurrentDataFreq = %{public}f", new Object[]{Integer.valueOf(this.mReportRat), Float.valueOf(this.mCurrentDataBandWidth), Float.valueOf(this.mCurrentDataFreq)});
        calculateInterDisturb();
    }

    public boolean isMobileConnected() {
        return this.isMobileConnected;
    }

    public String getMobileIface() {
        return this.mMobileIface;
    }

    public boolean isMobileDataAvailable() {
        return this.mIsMobileDataAvailable;
    }

    public void handleTelephonyDataConnectionChanged(String state, String iface, int subId) {
        MpLinkCommonUtils.logI(TAG, false, "ACTION_ANY_DATA_CONNECTION_STATE_CHANGED subId:%{public}d,state:%{public}s", new Object[]{Integer.valueOf(subId), state});
        if (subId != SubscriptionManager.getDefaultDataSubscriptionId()) {
            return;
        }
        if ("CONNECTED".equals(state)) {
            this.isMobileConnected = true;
            this.mMobileIface = iface;
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED);
        } else if (DISCONNECTED_FIELD.equals(state)) {
            this.isMobileConnected = false;
            this.mMobileIface = "";
            this.mIsMobileDataAvailable = false;
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED);
        }
    }

    public void handleTelephonyServiceStateChanged(ServiceState serviceState, int subId) {
        MpLinkCommonUtils.logI(TAG, false, "ACTION_SERVICE_STATE_CHANGED subId:%{public}d", new Object[]{Integer.valueOf(subId)});
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (subId != -1 && subId == defaultDataSubId && serviceState != null) {
            int newServiceState = serviceState.getDataRegState();
            if (this.mCurrentServiceState != newServiceState) {
                this.mCurrentServiceState = newServiceState;
                handleRadioServiceStateChange(newServiceState);
            }
            int newDataTechType = serviceState.getDataNetworkType();
            if (this.mCurrentDataTechType != newDataTechType) {
                this.mCurrentDataTechType = newDataTechType;
                handleDataTechTypeChange(newDataTechType);
            }
            boolean isRoamingState = serviceState.getDataRoaming();
            if (this.isCurrentDataRoaming != isRoamingState) {
                this.isCurrentDataRoaming = isRoamingState;
                handleDataRoamingStateChange(isRoamingState);
            }
        }
    }

    public void handleDataTechTypeChange(int dataTech) {
        boolean isSuitable;
        MpLinkCommonUtils.logI(TAG, false, "handlerDataTechTypeChange dataTech :%{public}d", new Object[]{Integer.valueOf(dataTech)});
        if (dataTech == 3 || dataTech == 8 || dataTech == 9 || dataTech == 10 || dataTech == 15 || dataTech == 13 || dataTech == 19 || dataTech == 20) {
            isSuitable = true;
        } else {
            isSuitable = false;
        }
        if (this.isDataTechSuitable != isSuitable) {
            this.isDataTechSuitable = isSuitable;
            if (isSuitable) {
                sendMessage(201);
            } else {
                sendMessage(202);
            }
        }
    }

    public void handleRadioServiceStateChange(int state) {
        MpLinkCommonUtils.logI(TAG, false, "handlerDataTechTypeChange State :%{public}d", new Object[]{Integer.valueOf(state)});
        if (state != 0) {
            sendMessage(206);
        } else {
            sendMessage(205);
        }
    }

    public void handleDataRoamingStateChange(boolean isRoaming) {
        MpLinkCommonUtils.logI(TAG, false, "handlerDataTechTypeChange roaming :%{public}s", new Object[]{String.valueOf(isRoaming)});
        if (isRoaming) {
            sendMessage(204);
        } else {
            sendMessage(203);
        }
    }

    public void handleMobileDataSwitchChange(boolean isEnabled) {
        MpLinkCommonUtils.logD(TAG, false, "handleMobileDataSwitchChange:%{public}s", new Object[]{String.valueOf(isEnabled)});
        int iFlag = 0;
        if (isEnabled) {
            iFlag = 1;
        }
        if (this.mMobileDataSwitchState != iFlag) {
            MpLinkCommonUtils.logI(TAG, false, "handleMobileDataSwitchChange sendmsg:%{public}s", new Object[]{String.valueOf(isEnabled)});
            this.mMobileDataSwitchState = iFlag;
            if (isEnabled) {
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN);
            } else {
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE);
            }
        }
    }

    public void updateWifiLcfInfo(int freq, int bandWidth) {
        this.mCurrentWifiFreq = freq;
        this.mCurrentWifiBandWidth = bandWidth;
        MpLinkCommonUtils.logD(TAG, false, "updateWifiLcfInfo freq:%{public}d,bandWidth:%{public}d", new Object[]{Integer.valueOf(this.mCurrentWifiFreq), Integer.valueOf(this.mCurrentWifiBandWidth)});
    }
}

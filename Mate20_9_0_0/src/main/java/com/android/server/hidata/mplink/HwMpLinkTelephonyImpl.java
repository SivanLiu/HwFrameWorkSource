package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkTelephonyImpl {
    private static final int NETWORK_REQUEST_TIMEOUT_MILLIS = 60000;
    private static final String TAG = "HiData_HwMpLinkTelephonyImpl";
    private int dealMobileDataRef = 0;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    private boolean mCurrentDataRoamingState = false;
    private int mCurrentDataTechType = 0;
    private int mCurrentServceState = 1;
    private int mDefaultDataSubId = 0;
    private Handler mHandler;
    private boolean mIsDataTechSuitable = false;
    private boolean mIsMobileDataAvailable = false;
    private final NetworkCallback mListenNetworkCallback = new NetworkCallback() {
        {
            MpLinkCommonUtils.logD(HwMpLinkTelephonyImpl.TAG, "onUnavailable");
        }

        public void onAvailable(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            String iface = linkProperties.getInterfaceName();
            String str = HwMpLinkTelephonyImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAvailable,iface:");
            stringBuilder.append(iface);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            if (iface != null) {
                HwMpLinkTelephonyImpl.this.mIsMobileDataAvailable = true;
                HwMpLinkTelephonyImpl.this.sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE);
            }
        }

        public void onUnavailable() {
        }
    };
    private boolean mMobileConnectState = false;
    private int mMobileDataSwitchState = -1;
    private String mMobileIface = "";
    private NetworkCallback mNetworkCallback;
    NetworkRequest mNetworkRequestForCallback = new Builder().addTransportType(0).addCapability(12).build();

    public HwMpLinkTelephonyImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        getConnectiviyManger();
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        if (this.mConnectivityManager != null) {
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequestForCallback, this.mListenNetworkCallback, this.mHandler);
        }
    }

    private void getConnectiviyManger() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private void sendMessage(int what) {
        this.mHandler.sendMessage(Message.obtain(this.mHandler, what));
    }

    public int getActiveNetworkType() {
        getConnectiviyManger();
        if (this.mConnectivityManager == null) {
            return -1;
        }
        NetworkInfo netinfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (netinfo != null) {
            return netinfo.getType();
        }
        return -1;
    }

    public boolean isMobileDataEnable() {
        boolean isMobileEnable = false;
        getConnectiviyManger();
        if (this.mConnectivityManager != null) {
            isMobileEnable = this.mConnectivityManager.getMobileDataEnabled();
        } else {
            MpLinkCommonUtils.logD(TAG, "isMobileDataEnable mConnectivityManager is null");
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMobileDataEnable ");
        stringBuilder.append(isMobileEnable);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        return isMobileEnable;
    }

    public int mplinkSetMobileData(boolean enable) {
        int ret = -1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mplinkSetMobileData dealMobileDataRef: ");
        stringBuilder.append(this.dealMobileDataRef);
        stringBuilder.append(", enable :");
        stringBuilder.append(enable);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (isMobileDataEnable() || !enable) {
            boolean connectState = isMobileConnected();
            if (enable) {
                if (connectState) {
                    MpLinkCommonUtils.logD(TAG, "mplinkSetMobileData already connected");
                } else if (this.dealMobileDataRef == 0) {
                    this.dealMobileDataRef = 1;
                    enableMobileData(true);
                    ret = 0;
                }
            } else if (this.dealMobileDataRef > 0) {
                enableMobileData(false);
                this.dealMobileDataRef = 0;
            } else {
                MpLinkCommonUtils.logD(TAG, "mplinkSetMobileData mplink do not open MobileData");
            }
            return ret;
        }
        MpLinkCommonUtils.logD(TAG, "mplinkSetMobileData Mobile switch closed");
        return 2;
    }

    private void enableMobileData(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableMobileData :");
        stringBuilder.append(enable);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (enable) {
            startNetworkForMpLink();
        } else {
            stopNetworkForMpLink();
        }
    }

    public boolean getCurrentDataTechSuitable() {
        return this.mIsDataTechSuitable;
    }

    private void startNetworkForMpLink() {
        MpLinkCommonUtils.logD(TAG, "startNetworkForMpLink");
        NetworkRequest mNetworkRequest = new Builder().addTransportType(0).addCapability(12).setNetworkSpecifier(Integer.toString(getDefaultDataSubId())).build();
        this.mNetworkCallback = new NetworkCallback();
        if (this.mNetworkCallback != null && this.mConnectivityManager != null) {
            this.mConnectivityManager.requestNetwork(mNetworkRequest, this.mNetworkCallback, 60000);
        }
    }

    private void stopNetworkForMpLink() {
        MpLinkCommonUtils.logD(TAG, "stopNetworkForMpLink");
        if (this.mNetworkCallback != null && this.mConnectivityManager != null) {
            try {
                this.mConnectivityManager.unregisterNetworkCallback(this.mNetworkCallback);
            } catch (IllegalArgumentException e) {
                MpLinkCommonUtils.logD(TAG, "Unregister network callback exception");
            } catch (Throwable th) {
                this.mNetworkCallback = null;
            }
            this.mNetworkCallback = null;
        }
    }

    public int getDefaultDataSubId() {
        return this.mDefaultDataSubId;
    }

    public boolean getCurrentDataRoamingState() {
        return this.mCurrentDataRoamingState;
    }

    public int getCurrentServceState() {
        return this.mCurrentServceState;
    }

    public boolean getCurrentMobileConnectState() {
        return this.mMobileConnectState;
    }

    public void handleDataSubChange(int subId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DataSub Change, new subId:");
        stringBuilder.append(subId);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (subId != -1 && this.mDefaultDataSubId != subId) {
            this.mDefaultDataSubId = subId;
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_DATA_SUB_CHANGE);
        }
    }

    public boolean isMobileConnected() {
        return this.mMobileConnectState;
    }

    public String getMobileIface() {
        return this.mMobileIface;
    }

    public boolean getMobileDataAvaiable() {
        return this.mIsMobileDataAvailable;
    }

    public void handleTelephonyDataConnectionChanged(String state, String iface, int subId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ACTION_ANY_DATA_CONNECTION_STATE_CHANGED subId:");
        stringBuilder.append(subId);
        stringBuilder.append(",state:");
        stringBuilder.append(state);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        if (subId != this.mDefaultDataSubId) {
            return;
        }
        if (AwareJobSchedulerConstants.SERVICES_STATUS_CONNECTED.equals(state)) {
            this.mMobileConnectState = true;
            this.mMobileIface = iface;
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED);
        } else if ("DISCONNECTED".equals(state)) {
            this.mMobileConnectState = false;
            this.mMobileIface = "";
            this.mIsMobileDataAvailable = false;
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED);
        }
    }

    public void handleTelephonyServiceStateChanged(ServiceState serviceState, int subId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ACTION_SERVICE_STATE_CHANGED subId:");
        stringBuilder.append(subId);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        if (subId != -1 && subId == this.mDefaultDataSubId && serviceState != null) {
            int newServiceState = serviceState.getDataRegState();
            if (this.mCurrentServceState != newServiceState) {
                this.mCurrentServceState = newServiceState;
                handleRadioServiceStateChange(newServiceState);
            }
            int newDataTechType = serviceState.getDataNetworkType();
            if (this.mCurrentDataTechType != newDataTechType) {
                this.mCurrentDataTechType = newDataTechType;
                handleDataTechTypeChange(newDataTechType);
            }
            boolean newRoamingState = serviceState.getDataRoaming();
            if (this.mCurrentDataRoamingState != newRoamingState) {
                this.mCurrentDataRoamingState = newRoamingState;
                handleDataRoamingStateChange(newRoamingState);
            }
        }
    }

    public void handleDataTechTypeChange(int dataTech) {
        boolean newDataTechSuitable;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handlerDataTechTypeChange dataTech :");
        stringBuilder.append(dataTech);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        if (dataTech == 3 || dataTech == 8 || dataTech == 9 || dataTech == 10 || dataTech == 15 || dataTech == 13 || dataTech == 19) {
            newDataTechSuitable = true;
        } else {
            newDataTechSuitable = false;
        }
        if (this.mIsDataTechSuitable != newDataTechSuitable) {
            this.mIsDataTechSuitable = newDataTechSuitable;
            if (newDataTechSuitable) {
                sendMessage(201);
            } else {
                sendMessage(202);
            }
        }
    }

    public void handleRadioServiceStateChange(int State) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handlerDataTechTypeChange State :");
        stringBuilder.append(State);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        if (State != 0) {
            sendMessage(206);
        } else {
            sendMessage(205);
        }
    }

    public void handleDataRoamingStateChange(boolean roaming) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handlerDataTechTypeChange roaming :");
        stringBuilder.append(roaming);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        if (roaming) {
            sendMessage(204);
        } else {
            sendMessage(203);
        }
    }

    public void handleMobileDataSwitchChange(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMobileDataSwitchChange:");
        stringBuilder.append(enable);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        int iFlag = 0;
        if (enable) {
            iFlag = 1;
        }
        if (this.mMobileDataSwitchState != iFlag) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleMobileDataSwitchChange sendmsg:");
            stringBuilder2.append(enable);
            MpLinkCommonUtils.logI(str2, stringBuilder2.toString());
            this.mMobileDataSwitchState = iFlag;
            if (enable) {
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN);
            } else {
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE);
            }
        }
    }
}

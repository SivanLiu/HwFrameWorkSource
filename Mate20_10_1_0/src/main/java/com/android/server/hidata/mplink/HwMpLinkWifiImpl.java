package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.util.List;

public class HwMpLinkWifiImpl {
    public static final int BAND_WIDTH_160MHZ = 160;
    public static final int BAND_WIDTH_20MHZ = 20;
    public static final int BAND_WIDTH_40MHZ = 40;
    public static final int BAND_WIDTH_80MHZ = 80;
    private static final String TAG = "HiData_HwMpLinkWifiImpl";
    private boolean isWifiConnected = false;
    private boolean isWifiVpnConnected = false;
    private Context mContext;
    private String mCurrentBssid = null;
    public int mCurrentWifiBandWidth = 0;
    public int mCurrentWifiFreq = 0;
    private Handler mHandler;
    private WifiInfo mWifiInfo;
    private WifiManager mWifiManager;
    private List<ScanResult> scanResultList;

    public HwMpLinkWifiImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
    }

    private void sendMessage(int what) {
        Handler handler = this.mHandler;
        handler.sendMessage(Message.obtain(handler, what));
    }

    public boolean isWifiConnected() {
        return this.isWifiConnected;
    }

    public boolean getCurrentWifiConnectState() {
        return this.isWifiConnected;
    }

    public boolean getCurrentWifiVpnState() {
        return this.isWifiVpnConnected;
    }

    public void setCurrentWifiVpnState(boolean isVpnConnected) {
        this.isWifiVpnConnected = isVpnConnected;
    }

    public void handleWifiNetworkStateChanged(NetworkInfo netInfo) {
        if (netInfo != null) {
            MpLinkCommonUtils.logI(TAG, false, "WIFI NETWORK_STATE_CHANGED_ACTION state:%{public}d", new Object[]{netInfo.getState()});
            if (netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                this.isWifiConnected = false;
                HwMpLinkContentAware.getInstance(this.mContext).resetAiDeviceType();
                this.mCurrentBssid = null;
                this.mCurrentWifiFreq = 0;
                this.mCurrentWifiBandWidth = 0;
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED);
            } else if (netInfo.getState() == NetworkInfo.State.CONNECTED || netInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) {
                this.isWifiConnected = true;
                this.mWifiInfo = this.mWifiManager.getConnectionInfo();
                WifiInfo wifiInfo = this.mWifiInfo;
                if (wifiInfo != null) {
                    this.mCurrentBssid = wifiInfo.getBSSID();
                    this.mCurrentWifiFreq = this.mWifiInfo.getFrequency();
                    this.mCurrentWifiBandWidth = getCurrentWifiBandWidth();
                    if (this.mCurrentWifiBandWidth == 0) {
                        this.mCurrentWifiBandWidth = ScanResult.is5GHz(this.mCurrentWifiFreq) ? 40 : 20;
                    }
                    MpLinkCommonUtils.logD(TAG, false, "Freq:%{public}d, BandWidth:%{public}d", new Object[]{Integer.valueOf(this.mCurrentWifiFreq), Integer.valueOf(this.mCurrentWifiBandWidth)});
                }
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED);
            }
        }
    }

    public void handleVpnStateChange(boolean isVpnConnected) {
        if (this.isWifiVpnConnected != isVpnConnected) {
            this.isWifiVpnConnected = isVpnConnected;
            if (isVpnConnected) {
                MpLinkCommonUtils.logD(TAG, false, "WIFI_VPN_CONNECTED", new Object[0]);
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED);
                return;
            }
            MpLinkCommonUtils.logD(TAG, false, "WIFI_VPN_DISCONNECTED", new Object[0]);
            sendMessage(207);
        }
    }

    private int getCurrentWifiBandWidth() {
        this.scanResultList = this.mWifiManager.getScanResults();
        List<ScanResult> list = this.scanResultList;
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (ScanResult scanResult : this.scanResultList) {
            if (scanResult != null && !TextUtils.isEmpty(scanResult.BSSID) && !TextUtils.isEmpty(this.mCurrentBssid) && this.mCurrentBssid.equals(scanResult.BSSID)) {
                MpLinkCommonUtils.logD(TAG, false, "channelWidth:%{public}d", new Object[]{Integer.valueOf(scanResult.channelWidth)});
                return wifiBandWidthConverter(scanResult.channelWidth);
            }
        }
        return 0;
    }

    public int wifiBandWidthConverter(int channelWidth) {
        if (channelWidth == 0) {
            return 20;
        }
        if (channelWidth == 1) {
            return 40;
        }
        if (channelWidth == 2) {
            return 80;
        }
        if (channelWidth == 3 || channelWidth == 4) {
            return 160;
        }
        return 0;
    }
}

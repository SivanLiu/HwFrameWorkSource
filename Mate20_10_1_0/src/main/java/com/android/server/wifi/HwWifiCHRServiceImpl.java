package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.IpConfiguration;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.wifi.HwHiLog;
import com.huawei.hwwifiproservice.HwDualBandMessageUtil;
import com.huawei.ncdft.HwNcDftConnManager;
import java.io.UnsupportedEncodingException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class HwWifiCHRServiceImpl implements HwWifiCHRService {
    private static final int BYTE_OFFSET = 8;
    private static final int CMD_NET_STATS_POLL = 2;
    private static final int CMD_REPORT_WIFI_DFT = 0;
    private static final int CMD_WIFI_NETWORK_STATE_CHANGE = 1;
    private static final String DEFAULT_ENCODE_TYPE = "UTF-8";
    private static final int E909002024_NO_INTERNET = 909002024;
    private static final int EID_80211AX = 255;
    private static final int HE_CAPABILITY_80211AX = 35;
    private static final String KEY_MGMT_EAP_SUITE_B_192 = "EAP_SUITE_B_192";
    private static final String KEY_MGMT_OWE = "OWE";
    private static final String KEY_MGMT_SAE = "SAE";
    private static final String KEY_OF_BSSID = "bssid";
    private static final String KEY_OF_CAPABILITIES = "wpa3Caps";
    private static final String KEY_OF_CHECK_REASON = "cCheckReason";
    private static final String KEY_OF_FIRST_CONNECT = "isFirstConnect";
    private static final String KEY_OF_NO_INTERNET_REASON = "errReason";
    private static final String KEY_OF_SSID = "ssid";
    private static final String KEY_OF_VENDOR_INFO = "vendorInfo";
    private static final String LOG_TAG = "HwNcChrServiceImpl";
    private static final int MSG_ROUTER_INFO_COLLECT = 909002029;
    private static final int ROUTER_INFO_COLLECT_DURA = 3600000;
    private static final String RSN_80211AX = "[11AX]";
    private static final int TIME_POLL_AFTER_CONNECT_DELAYED = 2000;
    private static final int TIME_POLL_TRAFFIC_STATS_INTERVAL = 10000;
    private static final short WPS_MANUFACTURER_TYPE = 8464;
    private static final short WPS_MODEL_NAME_TYPE = 8976;
    private static final short WPS_MODEL_NUMBER_TYPE = 9232;
    private static final short WPS_SERIAL_NUMBER_TYPE = 16912;
    private static final int WPS_VENDOR_OUI_TYPE = 82989056;
    private static Context mContext = null;
    private static HwWifiCHRService mInstance = null;
    private String mCaps = "";
    /* access modifiers changed from: private */
    public HwNcDftConnManager mClient;
    private HandlerThread mHandlerThread;
    private HwWifiCHRHilink mHwWifiCHRHilink = null;
    /* access modifiers changed from: private */
    public NCDFTExceptionHandler mNCDFTExceptionHandler;
    /* access modifiers changed from: private */
    public HWNetstatManager mNetstatManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.wifi.HwWifiCHRServiceImpl.AnonymousClass1 */
        boolean isConnected = false;

        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo;
            if (intent != null && intent.getAction() != null && intent.getAction().equals("android.net.wifi.STATE_CHANGE") && (networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo")) != null && this.isConnected != networkInfo.isConnected()) {
                this.isConnected = networkInfo.isConnected();
                HwWifiCHRServiceImpl.this.mNCDFTExceptionHandler.obtainMessage(1, this.isConnected ? 1 : 0, 0).sendToTarget();
            }
        }
    };
    private WifiManager mWifiManager = null;

    public static synchronized HwWifiCHRService getInstance() {
        HwWifiCHRService hwWifiCHRService;
        synchronized (HwWifiCHRServiceImpl.class) {
            hwWifiCHRService = mInstance;
        }
        return hwWifiCHRService;
    }

    public static void init(Context context) {
        if (context == null) {
            HwHiLog.w(LOG_TAG, false, "HwNcChrServiceImpl init, context is null!", new Object[0]);
            return;
        }
        Context context2 = mContext;
        if (context2 == null) {
            mInstance = new HwWifiCHRServiceImpl(context);
        } else if (context2 != context) {
            HwHiLog.w(LOG_TAG, false, "Detect difference context while do init", new Object[0]);
        }
    }

    public HwWifiCHRServiceImpl(Context context) {
        if (mContext == null) {
            mContext = context;
            this.mClient = new HwNcDftConnManager(mContext);
            this.mHandlerThread = new HandlerThread(LOG_TAG);
            this.mHandlerThread.start();
            this.mNCDFTExceptionHandler = new NCDFTExceptionHandler(this.mHandlerThread.getLooper());
            this.mNetstatManager = HWNetstatManager.getInstance(mContext);
            registerForBroadcasts();
            NCDFTExceptionHandler nCDFTExceptionHandler = this.mNCDFTExceptionHandler;
            if (nCDFTExceptionHandler != null) {
                this.mNCDFTExceptionHandler.sendMessageDelayed(nCDFTExceptionHandler.obtainMessage(MSG_ROUTER_INFO_COLLECT), 3600000);
            }
        }
        this.mHwWifiCHRHilink = HwWifiCHRHilink.getInstance(mContext);
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    /* access modifiers changed from: private */
    public boolean isStaticIpWifiConfig() {
        List<WifiConfiguration> configuredNetworks;
        WifiInfo info;
        int netId;
        WifiManager wifiManager = (WifiManager) mContext.getSystemService("wifi");
        if (wifiManager == null || (configuredNetworks = wifiManager.getConfiguredNetworks()) == null || (info = wifiManager.getConnectionInfo()) == null || (netId = info.getNetworkId()) == -1) {
            return false;
        }
        for (WifiConfiguration config : configuredNetworks) {
            if (config.networkId == netId && config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public class NCDFTExceptionHandler extends Handler {
        int token = 0;

        public NCDFTExceptionHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            int i2 = 1;
            if (i != 0) {
                if (i == 1) {
                    HwWifiCHRServiceImpl.this.handleWiFiDnsStats(0);
                    HwWifiCHRServiceImpl.this.mNetstatManager.resetNetstats();
                    this.token++;
                    if (msg.arg1 == 1) {
                        HwWifiCHRServiceImpl hwWifiCHRServiceImpl = HwWifiCHRServiceImpl.this;
                        if (!hwWifiCHRServiceImpl.isStaticIpWifiConfig()) {
                            i2 = 2;
                        }
                        hwWifiCHRServiceImpl.setIpType(i2);
                        sendMessageDelayed(obtainMessage(2, this.token, 0), 2000);
                    }
                } else if (i != 2) {
                    if (i == HwWifiCHRServiceImpl.MSG_ROUTER_INFO_COLLECT) {
                        HwWifiCHRServiceImpl.this.handleRouterInfoCollect();
                    }
                } else if (msg.arg1 == this.token) {
                    HwWifiCHRServiceImpl.this.mNetstatManager.performPollAndLog();
                    sendMessageDelayed(obtainMessage(2, this.token, 0), 10000);
                }
            } else if (HwWifiCHRServiceImpl.this.mClient != null) {
                HwWifiCHRServiceImpl.this.mClient.reportToDft(1, msg.arg1, msg.getData());
            } else {
                HwHiLog.e(HwWifiCHRServiceImpl.LOG_TAG, false, "reportWifiDFTEvent,mClient is null", new Object[0]);
            }
        }
    }

    public void uploadDisconnectException(int reasoncode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1006, 0);
        Bundle data = new Bundle();
        this.mHwWifiCHRHilink.putFastsleepIdle(data);
        this.mHwWifiCHRHilink.putRxListenState(data);
        data.putInt("reasoncode", reasoncode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void uploadAssocRejectException(int status) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1022, 0);
        Bundle data = new Bundle();
        data.putInt("status", status);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateWifiException(int ucErrorCode, String ucSubErrorCode) {
        if (ucErrorCode == 87) {
            dispatchToHilink(ucErrorCode, ucSubErrorCode, true);
            return;
        }
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1023, 0);
        Bundle data = new Bundle();
        data.putInt("ucErrorCode", ucErrorCode);
        data.putString("ucSubErrorCode", ucSubErrorCode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateWifiAuthFailEvent(String iface, int reason) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1016, 0);
        Bundle data = new Bundle();
        data.putInt("reason", reason);
        data.putString("iface", iface);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateWifiTriggerState(boolean enable) {
        HwHiLog.e(LOG_TAG, false, "updateWifiTriggerState,enable:%{public}s", new Object[]{String.valueOf(enable)});
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1021, 0);
        Bundle data = new Bundle();
        data.putBoolean("enable", enable);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void reportHwCHRAccessNetworkEventInfoList(int reasoncode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1017, 0);
        Bundle data = new Bundle();
        data.putInt("reasoncode", reasoncode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateConnectType(String type) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1029, 0);
        Bundle data = new Bundle();
        data.putString("type", type);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateApkChangewWifiStatus(int apkAction, String apkName) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1030, 0);
        Bundle data = new Bundle();
        data.putString("apkName", apkName);
        data.putInt("apkAction", apkAction);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void connectFromUserByConfig(WifiConfiguration config) {
    }

    public void updateWIFIConfiguraionByConfig(WifiConfiguration config) {
    }

    public void setBackgroundScanReq(boolean isBackgroundReq) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1034, 0);
        Bundle data = new Bundle();
        data.putBoolean("isBackgroundReq", isBackgroundReq);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void handleSupplicantException() {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1011, 0);
        msg.setData(new Bundle());
        msg.sendToTarget();
    }

    public void updateAccessWebException(int checkReason, String errReason) {
        dispatchToHilink(checkReason, errReason, false);
    }

    public void updateMultiGWCount(byte count) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1101, 0);
        Bundle data = new Bundle();
        data.putByte("count", count);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void uploadDFTEvent(int type, String ucSubErrorCode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1102, 0);
        Bundle data = new Bundle();
        data.putString("ucSubErrorCode", ucSubErrorCode);
        data.putInt("type", type);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateMSSCHR(int switchType, int absState, int reasonCode, ArrayList list) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1103, 0);
        Bundle data = new Bundle();
        data.putSerializable("list", list);
        data.putInt("switchType", switchType);
        data.putInt("absState", absState);
        data.putInt("reasonCode", reasonCode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateGameBoostLag(String reasoncode, String gameName, int gameRTT, int TcpRtt) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1104, 0);
        Bundle data = new Bundle();
        data.putString("reasoncode", reasoncode);
        data.putString("gameName", gameName);
        data.putInt("gameRTT", gameRTT);
        data.putInt("TcpRtt", TcpRtt);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateMSSState(String state) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1105, 0);
        Bundle data = new Bundle();
        data.putString("state", state);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void handleWiFiDnsStats(int netid) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1106, 0);
        Bundle data = new Bundle();
        data.putInt("netid", netid);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateScCHRCount(int type) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1107, 0);
        Bundle data = new Bundle();
        data.putInt("type", type);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateABSTime(String ssid, int associateTimes, int associateFailedTimes, long mimoTime, long sisoTime, long mimoScreenOnTime, long sisoScreenOnTime) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1108, 0);
        Bundle data = new Bundle();
        data.putString("ssid", ssid);
        data.putInt("associateTimes", associateTimes);
        data.putInt("associateFailedTimes", associateFailedTimes);
        data.putLong("mimoTime", mimoTime);
        data.putLong("sisoTime", sisoTime);
        data.putLong("mimoScreenOnTime", mimoScreenOnTime);
        data.putLong("sisoScreenOnTime", sisoScreenOnTime);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateMssSucCont(int trigerReason, int reasonCode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1109, 0);
        Bundle data = new Bundle();
        data.putInt("trigerReason", trigerReason);
        data.putInt("reasonCode", reasonCode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void txPwrBoostChrStatic(Boolean txBoostEnable, int RTT, int RTTCnt, int txGood, int txBad, int TxRetry) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1110, 0);
        Bundle data = new Bundle();
        data.putBoolean("txBoostEnable", txBoostEnable.booleanValue());
        data.putInt("RTT", RTT);
        data.putInt("RTTCnt", RTTCnt);
        data.putInt("txGood", txGood);
        data.putInt("txBad", txBad);
        data.putInt("TxRetry", TxRetry);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateGameBoostStatic(String gameName, boolean gameCnt) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1111, 0);
        Bundle data = new Bundle();
        data.putBoolean("gameCnt", gameCnt);
        data.putString("gameName", gameName);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updataWeChartStatic(int weChartTimes, int lowRssiTimes, int disconnectTimes, int backGroundTimes, int videoTimes) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1112, 0);
        Bundle data = new Bundle();
        data.putInt("weChartTimes", weChartTimes);
        data.putInt("lowRssiTimes", lowRssiTimes);
        data.putInt("disconnectTimes", disconnectTimes);
        data.putInt("backGroundTimes", backGroundTimes);
        data.putInt("videoTimes", videoTimes);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setGameKogScene(int gameKogScene) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1113, 0);
        Bundle data = new Bundle();
        data.putInt("gameKogScene", gameKogScene);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setWeChatScene(int weChatScene) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1114, 0);
        Bundle data = new Bundle();
        data.putInt("weChatScene", weChatScene);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateDhcpState(int state) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1020, 0);
        Bundle data = new Bundle();
        data.putInt("state", state);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void uploadDhcpException(String strDhcpError) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1004, 0);
        Bundle data = new Bundle();
        data.putString("strDhcpError", strDhcpError);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updatePortalStatus(int respCode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1115, 0);
        Bundle data = new Bundle();
        data.putInt("respCode", respCode);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setIpType(int type) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1116, 0);
        Bundle data = new Bundle();
        data.putInt("type", type);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateRepeaterOpenOrCloseError(int eventId, int openOrClose, String reason) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1117, 0);
        Bundle data = new Bundle();
        data.putInt("eventId", eventId);
        data.putInt("openOrClose", openOrClose);
        data.putString("reason", reason);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateAssocByABS() {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1118, 0);
        msg.setData(new Bundle());
        msg.sendToTarget();
    }

    public void incrAccessWebRecord(int reason, boolean succ, boolean isPortal) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1119, 0);
        Bundle data = new Bundle();
        data.putBoolean("isPortal", isPortal);
        data.putBoolean("succ", succ);
        data.putInt("reason", reason);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updatePortalConnection(int isPortalconnection) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1120, 0);
        Bundle data = new Bundle();
        data.putInt("isPortalconnection", isPortalconnection);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateArpSummery(boolean succ, int spendTime, int rssi) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1121, 0);
        Bundle data = new Bundle();
        data.putBoolean("succ", succ);
        data.putInt("spendTime", spendTime);
        data.putInt(HwDualBandMessageUtil.MSG_KEY_RSSI, rssi);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateAPOpenState() {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1035, 0);
        msg.setData(new Bundle());
        msg.sendToTarget();
    }

    public void addWifiRepeaterOpenedCount(int count) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1122, 0);
        Bundle data = new Bundle();
        data.putInt("count", count);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setWifiRepeaterWorkingTime(long workingTime) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1123, 0);
        Bundle data = new Bundle();
        data.putLong("workingTime", workingTime);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setRepeaterMaxClientCount(int maxCount) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1124, 0);
        Bundle data = new Bundle();
        data.putInt("maxCount", maxCount);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void addRepeaterConnFailedCount(int failed) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1125, 0);
        Bundle data = new Bundle();
        data.putInt("failed", failed);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void updateAPVendorInfo(String apvendorinfo) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1036, 0);
        Bundle data = new Bundle();
        data.putString("apvendorinfo", apvendorinfo);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setWifiRepeaterFreq(int freq) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1126, 0);
        Bundle data = new Bundle();
        data.putInt("freq", freq);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void setWifiRepeaterStatus(boolean isopen) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1127, 0);
        Bundle data = new Bundle();
        data.putBoolean("isopen", isopen);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void uploadDFTEvent(int eventId, Bundle bundle) {
        if (bundle != null) {
            Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1128, 0);
            bundle.putInt("WifiChrErrID", eventId);
            msg.setData(bundle);
            msg.sendToTarget();
        }
    }

    private void dispatchToHilink(int checkReason, String errReason, Boolean isFirstConnect) {
        Bundle data = new Bundle();
        this.mHwWifiCHRHilink.putFastsleepIdle(data);
        this.mHwWifiCHRHilink.putRxListenState(data);
        data.putString(KEY_OF_NO_INTERNET_REASON, errReason);
        data.putInt(KEY_OF_CHECK_REASON, checkReason);
        data.putBoolean(KEY_OF_FIRST_CONNECT, isFirstConnect.booleanValue());
        this.mHwWifiCHRHilink.dispatchUploadEvent(E909002024_NO_INTERNET, data);
    }

    /* access modifiers changed from: private */
    public void handleRouterInfoCollect() {
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) mContext.getSystemService("wifi");
        }
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            if (scanResults != null && scanResults.size() > 0) {
                int scanResultSize = scanResults.size();
                for (int i = 0; i < scanResultSize; i++) {
                    ScanResult info = scanResults.get(i);
                    this.mCaps = info.capabilities;
                    boolean isNeedUpload = false;
                    if (this.mCaps.contains("OWE") || this.mCaps.contains("SAE") || this.mCaps.contains(KEY_MGMT_EAP_SUITE_B_192)) {
                        isNeedUpload = true;
                    }
                    if (isParse80211axEid(info.informationElements)) {
                        this.mCaps += RSN_80211AX;
                        isNeedUpload = true;
                    }
                    if (isNeedUpload) {
                        uploadRouterCollect(info);
                    }
                }
            } else {
                return;
            }
        }
        this.mNCDFTExceptionHandler.sendMessageDelayed(this.mNCDFTExceptionHandler.obtainMessage(MSG_ROUTER_INFO_COLLECT), 3600000);
    }

    private String parseVendorInfo(ScanResult.InformationElement[] ies) {
        if (ies == null) {
            return "";
        }
        for (ScanResult.InformationElement ie : ies) {
            if (ie != null && ie.id == 221) {
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                try {
                    if (data.getInt() == WPS_VENDOR_OUI_TYPE) {
                        return parseWpsInformationElement(data);
                    }
                } catch (BufferUnderflowException e) {
                    HwHiLog.e(LOG_TAG, false, "parseVendorInfo:BufferUnderflowException happen", new Object[0]);
                }
            }
        }
        return "";
    }

    private String parseWpsInformationElement(ByteBuffer data) {
        String vendorInfo = "";
        if (data == null) {
            return vendorInfo;
        }
        while (true) {
            try {
                if (data.position() >= data.limit()) {
                    break;
                }
                int type = data.getShort();
                int length = data.getShort() >> 8;
                if (length <= 0) {
                    break;
                } else if (data.position() + length >= data.limit()) {
                    break;
                } else {
                    if (type == 8464 || type == 8976 || type == 9232 || type == 16912) {
                        vendorInfo = vendorInfo + ";" + new String(data.array(), data.position(), length, DEFAULT_ENCODE_TYPE);
                    }
                    data.position(data.position() + length);
                }
            } catch (BufferUnderflowException e) {
                HwHiLog.e(LOG_TAG, false, "BufferUnderflowException position:%{public}d, limit:%{public}d", new Object[]{Integer.valueOf(data.position()), Integer.valueOf(data.limit())});
            } catch (IndexOutOfBoundsException e2) {
                HwHiLog.e(LOG_TAG, false, "IndexOutOfBoundsException position:%{public}d, limit:%{public}d", new Object[]{Integer.valueOf(data.position()), Integer.valueOf(data.limit())});
            } catch (UnsupportedEncodingException e3) {
                HwHiLog.e(LOG_TAG, false, "UnsupportedEncodingException, encodeType:%{public}s", new Object[]{DEFAULT_ENCODE_TYPE});
            }
        }
        return vendorInfo;
    }

    private boolean isParse80211axEid(ScanResult.InformationElement[] ies) {
        if (ies == null) {
            return false;
        }
        for (ScanResult.InformationElement ie : ies) {
            if (ie != null && ie.id == EID_80211AX) {
                try {
                    if (ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN).get() == 35) {
                        return true;
                    }
                } catch (BufferUnderflowException e) {
                    HwHiLog.e(LOG_TAG, false, "parse80211axEid:BufferUnderflowException happen", new Object[0]);
                }
            }
        }
        return false;
    }

    private void uploadRouterCollect(ScanResult info) {
        Bundle data = new Bundle();
        data.putString(KEY_OF_CAPABILITIES, this.mCaps);
        data.putString("bssid", info.BSSID);
        data.putString("ssid", info.SSID);
        data.putString(KEY_OF_VENDOR_INFO, parseVendorInfo(info.informationElements));
        uploadDFTEvent(MSG_ROUTER_INFO_COLLECT, data);
    }
}

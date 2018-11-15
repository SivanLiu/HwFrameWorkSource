package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.server.wifi.wifipro.HwDualBandMessageUtil;
import com.huawei.ncdft.HwNcDftConnManager;
import java.util.ArrayList;

public class HwWifiCHRServiceImpl implements HwWifiCHRService {
    private static final int CMD_REPORT_WIFI_DFT = 0;
    public static final String LOG_TAG = "HwNcChrServiceImpl";
    private static HwNcDftConnManager mClient = null;
    private static Context mContext;
    private static HwWifiCHRService mInstance = null;
    HandlerThread mHandlerThread = null;
    private NCDFTExceptionHandler mNCDFTExceptionHandler;

    private class NCDFTExceptionHandler extends Handler {
        public NCDFTExceptionHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String str = HwWifiCHRServiceImpl.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Run the of call hwNcChrService");
                stringBuilder.append(msg.arg1);
                Log.e(str, stringBuilder.toString());
                if (HwWifiCHRServiceImpl.mClient != null) {
                    HwWifiCHRServiceImpl.mClient.reportToDft(1, msg.arg1, msg.getData());
                } else {
                    Log.e(HwWifiCHRServiceImpl.LOG_TAG, "reportWifiDFTEvent,mClient is null");
                }
            }
        }
    }

    public static synchronized HwWifiCHRService getInstance() {
        HwWifiCHRService hwWifiCHRService;
        synchronized (HwWifiCHRServiceImpl.class) {
            hwWifiCHRService = mInstance;
        }
        return hwWifiCHRService;
    }

    public static void init(Context context) {
        if (context == null) {
            Log.d(LOG_TAG, "HwNcChrServiceImpl init, context is null!");
            return;
        }
        if (mContext == null) {
            mInstance = new HwWifiCHRServiceImpl(context);
        } else if (mContext != context) {
            Log.d(LOG_TAG, "Detect difference context while do init");
        }
    }

    public HwWifiCHRServiceImpl(Context context) {
        if (mContext == null) {
            mContext = context;
            mClient = new HwNcDftConnManager(mContext);
            this.mHandlerThread = new HandlerThread(LOG_TAG);
            this.mHandlerThread.start();
            this.mNCDFTExceptionHandler = new NCDFTExceptionHandler(this.mHandlerThread.getLooper());
        }
    }

    public void uploadDisconnectException(int reasoncode) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1006, 0);
        Bundle data = new Bundle();
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
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateWifiTriggerState,enable:");
        stringBuilder.append(enable);
        Log.e(str, stringBuilder.toString());
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

    public void updateAccessWebException(int cCheckReason, String errReason) {
        Message msg = this.mNCDFTExceptionHandler.obtainMessage(0, 1100, 0);
        Bundle data = new Bundle();
        data.putString("errReason", errReason);
        data.putInt("cCheckReason", cCheckReason);
        msg.setData(data);
        msg.sendToTarget();
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
}

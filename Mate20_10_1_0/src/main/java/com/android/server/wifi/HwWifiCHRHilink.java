package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.wifi.HwHilinkProxyController;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.fastsleep.FsArbitration;
import com.android.server.wifi.hwUtil.ScanResultRecords;
import com.android.server.wifi.rxlisten.RxListenArbitration;
import com.huawei.hilink.framework.aidl.ResponseCallbackWrapper;
import com.huawei.hwwifiproservice.WifiProCommonDefs;
import com.huawei.ncdft.HwNcDftConnManager;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;

public class HwWifiCHRHilink {
    private static final String ACTION_NETWORK_CONDITIONS_MEASURED = "android.net.conn.NETWORK_CONDITIONS_MEASURED";
    private static final int CHR_DOMAIN_WIFI = 1;
    private static final int CMD_GET_LONGSLEEPCNT = 118;
    private static final int CMD_GET_RXLISTENINTERVAL = 124;
    private static final int CMD_GET_RXLISTENSTATE = 123;
    private static final int CMD_GET_SHORTSLEEPCNT = 117;
    private static final String CRYPTO_NAME = "SHA-256";
    private static final int DETECTED_STATE = 2;
    private static final int DETECTING_STATE = 1;
    private static final int E909002022_HISTORY_INFO = 909002022;
    private static final int E909002024_NO_INTERNET = 909002024;
    private static final int E909002025_WEB_SLOW = 909002025;
    private static final int E909009110_WAN_STATE = 909009110;
    public static final int EVENT_UPLOAD_ROUTER_PARAM = 3001;
    private static final String EXTRA_FLAG_HILINK_DETECT_NOT_PORTAL = "detect_not_portal";
    private static final String EXTRA_IS_CAPTIVE_PORTAL = "extra_is_captive_portal";
    private static final int HILINK_SERVICE_READY_DURA = 3000;
    private static final int IDLE_STATE = 0;
    private static final String INTENT_DS_WIFI_WEB_STAT_REPORT = "com.huawei.chr.wifi.action.web_stat_report";
    private static final int INVALID = -1;
    private static final String KEY_CHECK_REASON = "cCheckReason";
    private static final String KEY_ERROR_REASON = "errReason";
    private static final String KEY_FIRST_CONNECT = "isFirstConnect";
    private static final String KEY_OF_LONG_IDLE = "longIdle";
    private static final String KEY_OF_RXLISTEN_INTERVAL = "rxlistenInterval";
    private static final String KEY_OF_RXLISTEN_STATE = "rxlistenState";
    private static final String KEY_OF_SHORT_IDLE = "shortIdle";
    private static final String KEY_ROUTER_PAYLOAD = "routerPayload";
    private static final int MSG_HILINK_SERVICE_OPEN_SUCC = 5;
    private static final int MSG_REQUEST_TIMEOUT = 2;
    private static final int MSG_REQUEST_TIMEOUT_DURA = 6000;
    private static final int MSG_RESPONSE_FROM_ROUTER = 1;
    private static final int MSG_WIFI_CONNECTED = 4;
    private static final int MSG_WIFI_DISCONNECT = 3;
    private static final String PARAM_EXCEPTION_CNT = "exception_cnt";
    private static final String PARAM_IPV6 = "reason";
    private static final String PARAM_SOCK_UID = "sock_uid";
    private static final String PHONE_L2ADDR = "l2Addr";
    private static final int RX_LISTEN_ON = 1;
    private static final String TAG = "HwWifiCHRHilink";
    private static final int THRESHOLD_HISTORY_INFO_REQUEST = 600000;
    private static final int TYPE_NOT_PORTAL = 3;
    private static final String TYPE_PORTAL = "2";
    private static final int WAN_STATE_CONNECTED = 1;
    private static final int WAN_STATE_DISCONNECT = 0;
    public static final String WEB_DELAY_NEEDUPLOAD = "success";
    /* access modifiers changed from: private */
    public static Map<Integer, Bundle> mDataMap = new HashMap();
    private static HwWifiCHRHilink mHwWifiCHRHilink = null;
    private static HwNcDftConnManager mNcdftClient;
    private BroadcastReceiver mBroadcastReceiver;
    private String mBssid = "";
    private final Context mContext;
    private int mDetectResult = -1;
    /* access modifiers changed from: private */
    public int mDetectState = 0;
    private IHwHilinkCallback mHwHilinkCallback = new IHwHilinkCallback() {
        /* class com.android.server.wifi.HwWifiCHRHilink.AnonymousClass1 */
        private static final int STATE_DTLS_FAILURE = 1;

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onProxyReadyStateChanged(int state) {
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onProxyLostStateChanged() {
            boolean unused = HwWifiCHRHilink.this.mIsEnterDiscover = false;
            boolean unused2 = HwWifiCHRHilink.this.mIsHilinkCHRSupport = false;
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectionStateChanged(int state) {
            if (state == 1) {
                boolean unused = HwWifiCHRHilink.this.mIsEnterDiscover = false;
                boolean unused2 = HwWifiCHRHilink.this.mIsHilinkCHRSupport = false;
            }
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectSuccessResult(int requestId) {
            if (HwWifiCHRHilink.this.mIsEnterDiscover && !HwWifiCHRHilink.this.mIsHilinkResponsed) {
                boolean unused = HwWifiCHRHilink.this.mIsEnterDiscover = false;
                boolean unused2 = HwWifiCHRHilink.this.mIsHilinkCHRSupport = true;
                HwWifiCHRHilink.this.sendPhoneInfo(requestId);
            }
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectFailedResult(int requestId, int errorCode) {
            if (HwWifiCHRHilink.this.mIsEnterDiscover && !HwWifiCHRHilink.this.mIsHilinkResponsed) {
                boolean unused = HwWifiCHRHilink.this.mIsEnterDiscover = false;
                boolean unused2 = HwWifiCHRHilink.this.mIsHilinkCHRSupport = false;
            }
        }
    };
    private HwHilinkProxyController mHwHilinkProxyController;
    private HwWifiCHRService mHwWifiCHRService = null;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public boolean mIsEnterDiscover = false;
    /* access modifiers changed from: private */
    public boolean mIsHilinkCHRSupport = false;
    /* access modifiers changed from: private */
    public boolean mIsHilinkResponsed = false;
    private String mL2Addr = "02:00:00:00:00:00";
    private HwHilinkProxyController.HwHilinkModuleType mModuleType = HwHilinkProxyController.HwHilinkModuleType.WIFICHR;
    private List<String> mRequestList = new ArrayList();
    private HwHilinkProxyController.HwHilinkServiceType mServiceType = HwHilinkProxyController.HwHilinkServiceType.WIFICHR;
    /* access modifiers changed from: private */
    public WifiCHRHandler mWifiCHRHandler;
    private WifiManager mWifiManager;

    private HwWifiCHRHilink(Context ctx) {
        Log.d(TAG, "HwWifiCHRHilink init");
        this.mContext = ctx;
        mNcdftClient = new HwNcDftConnManager(this.mContext);
        this.mWifiCHRHandler = new WifiCHRHandler();
        this.mHwHilinkProxyController = HwHilinkProxyController.getInstance();
        if (!this.mHwHilinkProxyController.isRegisterHilinkCallback(this.mHwHilinkCallback)) {
            Log.i(TAG, "registerHilinkCallback failed");
        }
        registerBroadcastReceiver();
    }

    public static synchronized HwWifiCHRHilink getInstance(Context ctx) {
        HwWifiCHRHilink hwWifiCHRHilink;
        synchronized (HwWifiCHRHilink.class) {
            if (mHwWifiCHRHilink == null) {
                mHwWifiCHRHilink = new HwWifiCHRHilink(ctx);
            }
            hwWifiCHRHilink = mHwWifiCHRHilink;
        }
        return hwWifiCHRHilink;
    }

    private void registerBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.android.server.wifi.HwWifiCHRHilink.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                WifiConfiguration newConfig;
                if (intent != null) {
                    String action = intent.getAction();
                    if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                        NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (netInfo == null) {
                            return;
                        }
                        if (netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                            HwWifiCHRHilink.this.mWifiCHRHandler.sendEmptyMessage(3);
                        } else if (netInfo.isConnected()) {
                            HwWifiCHRHilink.this.mWifiCHRHandler.sendEmptyMessage(4);
                        }
                    } else if (HwWifiCHRHilink.INTENT_DS_WIFI_WEB_STAT_REPORT.equals(action)) {
                        Bundle data = intent.getExtras();
                        if (data != null) {
                            HwWifiCHRHilink.this.dispatchUploadEvent(HwWifiCHRHilink.E909002025_WEB_SLOW, data);
                        }
                    } else if (HwWifiCHRHilink.ACTION_NETWORK_CONDITIONS_MEASURED.equals(action)) {
                        if (intent.getBooleanExtra(HwWifiCHRHilink.EXTRA_IS_CAPTIVE_PORTAL, false) && HwWifiCHRHilink.this.mDetectState == 0) {
                            HwWifiCHRHilink.this.startEvent(HwWifiCHRHilink.E909009110_WAN_STATE);
                        }
                    } else if (WifiProCommonDefs.ACTION_UPDATE_CONFIG_HISTORY.equals(action) && (newConfig = (WifiConfiguration) intent.getParcelableExtra(WifiProCommonDefs.EXTRA_FLAG_NEW_WIFI_CONFIG)) != null && newConfig.internetHistory != null && newConfig.internetHistory.lastIndexOf("/") != -1) {
                        String lastInternet = newConfig.internetHistory.substring(0, 1);
                        Log.i(HwWifiCHRHilink.TAG, "receive broadcast ACTION_UPDATE_CONFIG_HISTORY, lastInternet = " + lastInternet);
                        if (HwWifiCHRHilink.TYPE_PORTAL.equals(lastInternet) && HwWifiCHRHilink.this.mDetectState == 0) {
                            HwWifiCHRHilink.this.startEvent(HwWifiCHRHilink.E909009110_WAN_STATE);
                        }
                    }
                }
            }
        };
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction(INTENT_DS_WIFI_WEB_STAT_REPORT);
        this.mIntentFilter.addAction(ACTION_NETWORK_CONDITIONS_MEASURED);
        this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_UPDATE_CONFIG_HISTORY);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
    }

    /* access modifiers changed from: private */
    public class WifiCHRHandler extends Handler {
        private WifiCHRHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                HwWifiCHRHilink.this.handleRouterResp(msg);
            } else if (i == 2) {
                HwWifiCHRHilink.this.handleRequestTimeout(msg);
            } else if (i == 3) {
                HwWifiCHRHilink.this.initWifiCHRParams();
            } else if (i == 4) {
                HwWifiCHRHilink.this.handleWifiConnected();
            } else if (i == 5) {
                HwWifiCHRHilink.this.handleHilinkOpenSucc(msg);
            } else if (i != HwWifiCHRHilink.E909002022_HISTORY_INFO) {
                switch (i) {
                    case HwWifiCHRHilink.E909002024_NO_INTERNET /*{ENCODED_INT: 909002024}*/:
                        Bundle msgData = msg.getData();
                        if (msgData != null) {
                            HwWifiCHRHilink.mDataMap.put(Integer.valueOf((int) HwWifiCHRHilink.E909002024_NO_INTERNET), msgData);
                            HwWifiCHRHilink.this.startEvent(HwWifiCHRHilink.E909002024_NO_INTERNET);
                            return;
                        }
                        return;
                    case HwWifiCHRHilink.E909002025_WEB_SLOW /*{ENCODED_INT: 909002025}*/:
                        HwWifiCHRHilink.this.handleWebDelayMessage(msg.getData());
                        return;
                    default:
                        Log.i(HwWifiCHRHilink.TAG, "msg not found : " + msg.what);
                        return;
                }
            } else {
                Bundle msgData2 = msg.getData();
                if (msgData2 != null) {
                    HwWifiCHRHilink.mDataMap.put(Integer.valueOf((int) HwWifiCHRHilink.E909002022_HISTORY_INFO), msgData2);
                    HwWifiCHRHilink.this.startEvent(HwWifiCHRHilink.E909002022_HISTORY_INFO);
                }
            }
        }
    }

    private class ResponseCallback extends ResponseCallbackWrapper {
        private ResponseCallback() {
        }

        @Override // com.huawei.hilink.framework.aidl.ResponseCallbackWrapper, com.huawei.hilink.framework.aidl.IResponseCallback
        public void onRecieveError(int errorCode) throws RemoteException {
            boolean unused = HwWifiCHRHilink.this.mIsHilinkResponsed = false;
            Log.i(HwWifiCHRHilink.TAG, "response recieve error : " + errorCode);
        }

        @Override // com.huawei.hilink.framework.aidl.ResponseCallbackWrapper, com.huawei.hilink.framework.aidl.IResponseCallback
        public void onRecieveResponse(int callID, String payload) throws RemoteException {
            Log.i(HwWifiCHRHilink.TAG, "response recieve callID : " + callID + " payload: " + payload);
            boolean unused = HwWifiCHRHilink.this.mIsHilinkResponsed = true;
            if (HwWifiCHRHilink.this.mWifiCHRHandler != null) {
                Message msg = HwWifiCHRHilink.this.mWifiCHRHandler.obtainMessage();
                msg.what = 1;
                msg.obj = payload;
                msg.arg1 = callID;
                HwWifiCHRHilink.this.mWifiCHRHandler.sendMessage(msg);
            }
        }
    }

    private String buildHilinkPayload(int type) {
        JSONObject jsonObject = new JSONObject();
        this.mRequestList.clear();
        switch (type) {
            case E909002022_HISTORY_INFO /*{ENCODED_INT: 909002022}*/:
                if (mNcdftClient != null) {
                    this.mRequestList.add(Integer.toString(E909002022_HISTORY_INFO));
                    break;
                }
                break;
            case 909002023:
            default:
                Log.i(TAG, "event type not found : " + type);
                break;
            case E909002024_NO_INTERNET /*{ENCODED_INT: 909002024}*/:
                if (mNcdftClient != null) {
                    this.mRequestList.add(Integer.toString(E909002024_NO_INTERNET));
                    break;
                }
                break;
            case E909002025_WEB_SLOW /*{ENCODED_INT: 909002025}*/:
                if (mNcdftClient != null) {
                    this.mRequestList.add(Integer.toString(E909002025_WEB_SLOW));
                    break;
                }
                break;
        }
        try {
            jsonObject.put("event", type);
            jsonObject.put(PHONE_L2ADDR, JSONObject.wrap(this.mL2Addr));
            jsonObject.put("payload", "");
            jsonObject.put("routerHash", getEncryptDeviceId());
        } catch (JSONException e) {
            Log.e(TAG, "Json Exception", e);
        }
        return jsonObject.toString();
    }

    public void sendPhoneInfo(int eventId) {
        if (!this.mIsHilinkCHRSupport) {
            Log.i(TAG, "sendPhoneInfo,router don't support wifi chr!");
            return;
        }
        String payload = buildHilinkPayload(eventId);
        if (payload != null) {
            Log.i(TAG, "call request! eventId = " + eventId);
            int ret = this.mHwHilinkProxyController.call(eventId, payload, this.mServiceType.getValue(), new ResponseCallback());
            if (ret != 0) {
                Log.i(TAG, "call failed! ret = " + ret);
            }
        }
    }

    public void detectAndSendCHRInfo(int eventId) {
        Log.i(TAG, "discover Service! eventId = " + eventId);
        this.mIsEnterDiscover = true;
        HwHilinkProxyController hwHilinkProxyController = this.mHwHilinkProxyController;
        Objects.requireNonNull(hwHilinkProxyController);
        HwHilinkProxyController.ServiceFoundCallback serviceFoundCallback = new HwHilinkProxyController.ServiceFoundCallback();
        serviceFoundCallback.bssid = this.mBssid;
        serviceFoundCallback.requestId = eventId;
        serviceFoundCallback.serviceType = this.mServiceType.getValue();
        int ret = this.mHwHilinkProxyController.discover(serviceFoundCallback);
        if (ret != 0) {
            Log.i(TAG, "discover Service failed! ret = " + ret);
        }
    }

    private boolean isHilinkGateway() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager == null) {
            Log.w(TAG, "wifiManager is null!");
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.w(TAG, "wifiInfo is null!");
            return false;
        }
        this.mBssid = wifiInfo.getBSSID();
        if (this.mBssid == null) {
            Log.w(TAG, "l2addr is null!");
            return false;
        } else if (ScanResultRecords.getDefault().getHiLinkAp(this.mBssid) == 1) {
            return true;
        } else {
            return false;
        }
    }

    public void initWifiCHRParams() {
        Log.d(TAG, "initWifiCHRParams");
        this.mIsHilinkCHRSupport = false;
        this.mDetectState = 0;
        this.mDetectResult = -1;
        this.mIsEnterDiscover = false;
        this.mIsHilinkResponsed = false;
        if (this.mHwHilinkProxyController.isOpened()) {
            this.mHwHilinkProxyController.removeReferenceModule(this.mModuleType.getValue());
            this.mHwHilinkProxyController.closeHilinkServiceProxy();
        }
        if (this.mWifiCHRHandler.hasMessages(E909002022_HISTORY_INFO)) {
            this.mWifiCHRHandler.removeMessages(E909002022_HISTORY_INFO);
        }
    }

    public void dispatchUploadEvent(int eventId, Bundle data) {
        if (data != null) {
            Message msg = this.mWifiCHRHandler.obtainMessage();
            msg.what = eventId;
            msg.setData(data);
            this.mWifiCHRHandler.sendMessage(msg);
        }
    }

    private void uploadEvent(int eventId, Bundle data) {
        if (data != null) {
            data.putInt("eventId", eventId);
            this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
            if (this.mHwWifiCHRService != null) {
                Log.d(TAG, "upload event " + eventId);
                this.mHwWifiCHRService.uploadDFTEvent((int) EVENT_UPLOAD_ROUTER_PARAM, data);
            }
        }
    }

    public void putRxListenState(Bundle msgData) {
        RxListenArbitration rxListenArbitration = RxListenArbitration.getInstance();
        if (rxListenArbitration == null || msgData == null) {
            Log.i(TAG, "create rxListenArbitration fail or msgdata is null");
            return;
        }
        int rxListenState = rxListenArbitration.sendRxListenCmdtoDriver(123);
        int rxListenInterval = rxListenArbitration.sendRxListenCmdtoDriver(124);
        msgData.putInt(KEY_OF_RXLISTEN_STATE, rxListenState);
        if (rxListenState == 1) {
            msgData.putInt(KEY_OF_RXLISTEN_INTERVAL, rxListenInterval);
        } else {
            Log.w(TAG, "rxlisten is closed, don't record the rxlisten on interval");
        }
    }

    public void putFastsleepIdle(Bundle msgData) {
        int longIdle;
        int shortIdle;
        FsArbitration fsArbitration = FsArbitration.getInstance();
        if (fsArbitration == null || msgData == null) {
            Log.w(TAG, "create fsArbitration fail or msgdata is null");
            return;
        }
        int shortIdle2 = fsArbitration.sendFastSleepCmdtoDriver(117);
        int longIdle2 = fsArbitration.sendFastSleepCmdtoDriver(118);
        int lastFsOnShortIdle = fsArbitration.getFastSleepShortIdle();
        int lastFsOnLongIdle = fsArbitration.getFastSleepLongIdle();
        if (shortIdle2 < 0 || longIdle2 < 0 || shortIdle2 < lastFsOnShortIdle || longIdle2 < lastFsOnLongIdle) {
            shortIdle = 0;
            longIdle = 0;
        } else {
            shortIdle = shortIdle2 - lastFsOnShortIdle;
            longIdle = longIdle2 - lastFsOnLongIdle;
        }
        msgData.putInt(KEY_OF_SHORT_IDLE, shortIdle);
        msgData.putInt(KEY_OF_LONG_IDLE, longIdle);
    }

    /* access modifiers changed from: private */
    public void handleWebDelayMessage(Bundle msgData) {
        if (msgData == null) {
            return;
        }
        if (msgData.getInt(PARAM_SOCK_UID) > 0 || msgData.getInt(PARAM_EXCEPTION_CNT) > 0 || msgData.getInt("reason") > 0) {
            uploadEvent(E909002025_WEB_SLOW, msgData);
            return;
        }
        mDataMap.put(Integer.valueOf((int) E909002025_WEB_SLOW), msgData);
        int reportType = msgData.getInt("ReportType");
        int webDelay = msgData.getInt("WebDelay");
        int succNum = msgData.getInt("SuccNum");
        String ret = "fail";
        if (reportType == 0 && webDelay > 0) {
            this.mRequestList.clear();
            this.mRequestList.add(Integer.toString(E909002025_WEB_SLOW));
            this.mRequestList.add(Integer.toString(0));
            this.mRequestList.add(Integer.toString(webDelay));
            this.mRequestList.add(Integer.toString(succNum));
            ret = mNcdftClient.getFromDft(1, this.mRequestList);
            Log.i(TAG, "MSG_RESPONSE_FROM_ROUTER  needUpload = " + ret);
        }
        if (WEB_DELAY_NEEDUPLOAD.equals(ret)) {
            putFastsleepIdle(msgData);
            putRxListenState(msgData);
            startEvent(E909002025_WEB_SLOW);
            return;
        }
        uploadEvent(E909002025_WEB_SLOW, msgData);
        mDataMap.remove(Integer.valueOf((int) E909002025_WEB_SLOW));
    }

    /* access modifiers changed from: private */
    public void handleRouterResp(Message msg) {
        if (this.mWifiCHRHandler.hasMessages(2)) {
            this.mWifiCHRHandler.removeMessages(2);
        }
        String payload = (String) msg.obj;
        int event = msg.arg1;
        Bundle msgData = mDataMap.get(Integer.valueOf(event));
        if (event == E909009110_WAN_STATE) {
            this.mDetectState = 2;
            notifyWifiproWanState(msg);
        } else if (msgData != null) {
            if (payload != null) {
                msgData.putString(KEY_ROUTER_PAYLOAD, payload);
            }
            uploadEvent(event, msgData);
            mDataMap.remove(Integer.valueOf(event));
        }
    }

    /* access modifiers changed from: private */
    public void handleRequestTimeout(Message msg) {
        int event = msg.arg1;
        if (event == E909009110_WAN_STATE && this.mDetectState != 2) {
            this.mDetectState = 0;
        }
        Bundle msgData = mDataMap.get(Integer.valueOf(event));
        if (msgData != null) {
            Log.i(TAG, "upload event " + event);
            uploadEvent(event, msgData);
            mDataMap.remove(Integer.valueOf(event));
        }
    }

    /* access modifiers changed from: private */
    public void handleWifiConnected() {
        WifiInfo wifiInfo;
        Bundle historyData = new Bundle();
        historyData.putInt("event", E909002022_HISTORY_INFO);
        Message connMsg = this.mWifiCHRHandler.obtainMessage();
        connMsg.what = E909002022_HISTORY_INFO;
        connMsg.setData(historyData);
        if (this.mWifiCHRHandler.hasMessages(E909002022_HISTORY_INFO)) {
            this.mWifiCHRHandler.removeMessages(E909002022_HISTORY_INFO);
        }
        this.mWifiCHRHandler.sendMessageDelayed(connMsg, HwQoEService.GAME_RTT_NOTIFY_INTERVAL);
        Context context = this.mContext;
        if (context != null) {
            this.mWifiManager = (WifiManager) context.getSystemService("wifi");
            WifiManager wifiManager = this.mWifiManager;
            if (wifiManager != null && (wifiInfo = wifiManager.getConnectionInfo()) != null) {
                this.mL2Addr = wifiInfo.getMacAddress();
            }
        }
    }

    public void startEvent(int eventId) {
        if (isHilinkGateway()) {
            if (eventId == E909009110_WAN_STATE) {
                this.mDetectState = 1;
            }
            if (!this.mHwHilinkProxyController.isOpened()) {
                Log.i(TAG, "open mHilinkServiceOpened!");
                openHilinkService(eventId);
            } else {
                Log.i(TAG, "isHilinkGateway = true:" + eventId);
                if (this.mIsHilinkCHRSupport) {
                    sendPhoneInfo(eventId);
                } else {
                    detectAndSendCHRInfo(eventId);
                }
            }
        }
        Message msg = this.mWifiCHRHandler.obtainMessage();
        msg.what = 2;
        msg.arg1 = eventId;
        this.mWifiCHRHandler.sendMessageDelayed(msg, 6000);
    }

    /* access modifiers changed from: private */
    public void handleHilinkOpenSucc(Message msg) {
        int eventId = msg.arg1;
        if (this.mHwHilinkProxyController.isOpened() && eventId != -1) {
            if (this.mWifiCHRHandler.hasMessages(2)) {
                this.mWifiCHRHandler.removeMessages(2);
            }
            if (this.mIsHilinkCHRSupport) {
                sendPhoneInfo(eventId);
            } else {
                detectAndSendCHRInfo(eventId);
            }
        }
    }

    private void openHilinkService(int eventId) {
        Log.i(TAG, "openHilinkService:" + eventId);
        this.mIsEnterDiscover = false;
        this.mIsHilinkResponsed = false;
        this.mHwHilinkProxyController.openHilinkServiceProxy(this.mModuleType.getValue(), this.mContext);
        Message msg = this.mWifiCHRHandler.obtainMessage();
        msg.what = 5;
        msg.arg1 = eventId;
        this.mWifiCHRHandler.sendMessageDelayed(msg, 3000);
    }

    private void notifyWifiproWanState(Message msg) {
        if (msg != null && msg.obj != null) {
            String payload = (String) msg.obj;
            JSONObject json = null;
            try {
                json = new JSONObject(payload);
            } catch (JSONException e) {
                Log.e(TAG, "new json exception !");
            }
            if (json != null) {
                int wanStat = json.optInt("wanState", -1);
                Bundle msgData = new Bundle();
                msgData.putBoolean(KEY_FIRST_CONNECT, false);
                msgData.putInt(KEY_CHECK_REASON, E909009110_WAN_STATE);
                msgData.putString(KEY_ROUTER_PAYLOAD, payload);
                if (wanStat == 0) {
                    this.mDetectResult = 3;
                    Intent intent = new Intent(WifiProCommonDefs.ACTION_UPDATE_CONFIG_HISTORY);
                    intent.putExtra(EXTRA_FLAG_HILINK_DETECT_NOT_PORTAL, 3);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
                    msgData.putString(KEY_ERROR_REASON, EXTRA_FLAG_HILINK_DETECT_NOT_PORTAL);
                } else {
                    msgData.putString(KEY_ERROR_REASON, EXTRA_IS_CAPTIVE_PORTAL);
                }
                uploadEvent(E909002024_NO_INTERNET, msgData);
            }
        }
    }

    public int getHilinkDetectResult() {
        return this.mDetectResult;
    }

    private String getEncryptDeviceId() {
        String sn;
        if (Build.VERSION.SDK_INT < 26 || (sn = Build.getSerial()) == null) {
            return "";
        }
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance(CRYPTO_NAME).digest(sn.getBytes("UTF-8"))).substring(0, 20);
        } catch (UnsupportedEncodingException | IndexOutOfBoundsException | NoSuchAlgorithmException e) {
            Log.e(TAG, "error occurred when encrypting.");
            return "";
        }
    }
}

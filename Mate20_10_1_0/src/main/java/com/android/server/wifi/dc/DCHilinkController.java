package com.android.server.wifi.dc;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HwHilinkProxyController;
import com.android.server.wifi.HwWifiServiceManager;
import com.android.server.wifi.HwWifiServiceManagerImpl;
import com.android.server.wifi.IHwHilinkCallback;
import com.android.server.wifi.p2p.HwWifiP2pService;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.huawei.hilink.framework.aidl.ResponseCallbackWrapper;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DCHilinkController extends StateMachine {
    private static final int ADDRESS_LENGTH = 6;
    private static final int DC_ACTION_DELAY_MSEC = 3000;
    private static final int DC_ACTION_RETRY_LIMIT_TIMES = 5;
    private static final int DC_MANAGER_DETECT_INTVAL_MSEC = 3000;
    private static final int DC_P2P_DELETE_DELAY_MSEC = 1000;
    private static final int DISCONNECT_HILINK_DELAY_TIME_MSEC = 100000;
    private static final int DISCOVER_LIMIT_RETRY_TIMES = 3;
    private static final int HILINK_DISCOVER_TIMEOUT_MS = 3000;
    private static final String TAG = "DCHilinkController";
    private static DCHilinkController mDCHilinkController = null;
    /* access modifiers changed from: private */
    public ActiveState mActiveState = new ActiveState();
    private String mBssid = "";
    private Context mContext;
    /* access modifiers changed from: private */
    public int mDcActionRetryTimes = 0;
    /* access modifiers changed from: private */
    public int mDcActionType = 0;
    /* access modifiers changed from: private */
    public DcChr mDcChr;
    /* access modifiers changed from: private */
    public Handler mDcHilinkHandler;
    private DefaultState mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public int mDiscoverRetryTimes = 0;
    /* access modifiers changed from: private */
    public DiscoverState mDiscoverState = new DiscoverState();
    private long mElapsedScreenOffTime = 0;
    private IHwHilinkCallback mHwHilinkCallback = new IHwHilinkCallback() {
        /* class com.android.server.wifi.dc.DCHilinkController.AnonymousClass1 */
        private static final int STATE_DTLS_FAILURE = 1;

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onProxyReadyStateChanged(int state) {
            HwHiLog.d(DCHilinkController.TAG, false, "onProxyReady, state=%{public}d, isEnterDiscoverState=%{public}s", new Object[]{Integer.valueOf(state), String.valueOf(DCHilinkController.this.mIsEnterDiscoverState)});
            if (state != 0 || !DCHilinkController.this.mIsEnterDiscoverState) {
                DCHilinkController dCHilinkController = DCHilinkController.this;
                dCHilinkController.transitionTo(dCHilinkController.mIdleState);
                return;
            }
            DCHilinkController.this.sendMessage(19);
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onProxyLostStateChanged() {
            HwHiLog.d(DCHilinkController.TAG, false, "onProxyLostStateChanged", new Object[0]);
            DCHilinkController dCHilinkController = DCHilinkController.this;
            dCHilinkController.transitionTo(dCHilinkController.mIdleState);
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectionStateChanged(int state) {
            if (state == 1) {
                int unused = DCHilinkController.this.mDiscoverRetryTimes = 0;
                DCHilinkController.this.sendMessage(21);
            }
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectSuccessResult(int requestId) {
            HwHiLog.d(DCHilinkController.TAG, false, "onConnectSuccessResult, isDcConfigDetected=%{public}s,discoverRetryTimes=%{public}d", new Object[]{String.valueOf(DCHilinkController.this.mIsDcConfigDetected), Integer.valueOf(DCHilinkController.this.mDiscoverRetryTimes)});
            if (!DCHilinkController.this.mIsDcConfigDetected && DCHilinkController.this.mDiscoverRetryTimes > 0) {
                int unused = DCHilinkController.this.mDiscoverRetryTimes = 0;
                DCHilinkController.this.sendMessage(20);
            }
        }

        @Override // com.android.server.wifi.IHwHilinkCallback
        public void onConnectFailedResult(int requestId, int errorCode) {
            HwHiLog.d(DCHilinkController.TAG, false, "onConnectFailedResult, isDcConfigDetected=%{public}s,discoverRetryTimes=%{public}d", new Object[]{String.valueOf(DCHilinkController.this.mIsDcConfigDetected), Integer.valueOf(DCHilinkController.this.mDiscoverRetryTimes)});
            if (!DCHilinkController.this.mIsDcConfigDetected && DCHilinkController.this.mDiscoverRetryTimes > 0) {
                int unused = DCHilinkController.this.mDiscoverRetryTimes = 0;
                DCHilinkController.this.sendMessage(21);
            }
        }
    };
    private HwHilinkProxyController mHwHilinkProxyController;
    /* access modifiers changed from: private */
    public IdleState mIdleState = new IdleState();
    /* access modifiers changed from: private */
    public boolean mIsDcAllowedByRssi = false;
    /* access modifiers changed from: private */
    public boolean mIsDcConfigDetected = false;
    /* access modifiers changed from: private */
    public boolean mIsDcConnected = false;
    /* access modifiers changed from: private */
    public boolean mIsEnterDiscoverState = false;
    private HwHilinkProxyController.HwHilinkModuleType mModuleType = HwHilinkProxyController.HwHilinkModuleType.DC;
    private String mP2pMacAddress = "";
    private HwHilinkProxyController.HwHilinkServiceType mServiceType = HwHilinkProxyController.HwHilinkServiceType.DC;
    private WifiManager mWifiManager;

    public static DCHilinkController createDCHilinkController(Context context) {
        if (mDCHilinkController == null) {
            mDCHilinkController = new DCHilinkController(context);
        }
        return mDCHilinkController;
    }

    public static DCHilinkController getInstance() {
        return mDCHilinkController;
    }

    private DCHilinkController(Context context) {
        super(TAG);
        this.mContext = context;
        this.mDcHilinkHandler = getHandler();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mDcChr = DcChr.getInstance();
        this.mHwHilinkProxyController = HwHilinkProxyController.getInstance();
        HwHiLog.d(TAG, false, "registerHilinkCallback isRegisterSuccess=%{public}s", new Object[]{Boolean.valueOf(this.mHwHilinkProxyController.isRegisterHilinkCallback(this.mHwHilinkCallback))});
        addState(this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mDiscoverState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        setInitialState(this.mIdleState);
        start();
    }

    public Handler getDCHilinkHandler() {
        return this.mDcHilinkHandler;
    }

    /* access modifiers changed from: private */
    public void initHilinkControllerParams() {
        HwHiLog.d(TAG, false, "init DCHilinkController config", new Object[0]);
        this.mDiscoverRetryTimes = 0;
        this.mDcActionRetryTimes = 0;
        this.mIsEnterDiscoverState = false;
        if (this.mHwHilinkProxyController.isOpened()) {
            HwHiLog.d(TAG, false, "already open, now close", new Object[0]);
            this.mHwHilinkProxyController.removeReferenceModule(this.mModuleType.getValue());
            this.mHwHilinkProxyController.closeHilinkServiceProxy();
        }
    }

    /* access modifiers changed from: private */
    public void closeHilinkService() {
        this.mHwHilinkProxyController.clearReferenceModule();
        this.mHwHilinkProxyController.closeHilinkServiceProxy();
    }

    public boolean isWifiAndP2pStateAllowDc() {
        DCMonitor dcMonitor = DCMonitor.getInstance();
        if (dcMonitor == null) {
            HwHiLog.e(TAG, false, "dcMonitor is null", new Object[0]);
            return false;
        }
        boolean isP2pEnabled = false;
        boolean isP2pServiceExist = false;
        HwWifiServiceManager hwWifiServiceManager = HwWifiServiceManagerImpl.getDefault();
        if (hwWifiServiceManager instanceof HwWifiServiceManagerImpl) {
            WifiP2pServiceImpl wifiP2pServiceImpl = ((HwWifiServiceManagerImpl) hwWifiServiceManager).getHwWifiP2pService();
            if (wifiP2pServiceImpl instanceof HwWifiP2pService) {
                isP2pServiceExist = ((HwWifiP2pService) wifiP2pServiceImpl).hasP2pService();
                isP2pEnabled = wifiP2pServiceImpl.isP2pEnabled();
            }
        }
        boolean isWifiConnected = dcMonitor.isWifiConnected();
        HwHiLog.i(TAG, false, "p2pEnabled=%{public}s wifiConnected=%{public}s isP2pServiceExist=%{public}s", new Object[]{String.valueOf(isP2pEnabled), String.valueOf(isWifiConnected), String.valueOf(isP2pServiceExist)});
        if (!isP2pEnabled || !isWifiConnected || isP2pServiceExist) {
            return false;
        }
        return true;
    }

    public boolean isDcAllowed() {
        DCMonitor dcMonitor = DCMonitor.getInstance();
        DCArbitra dcArbitra = DCArbitra.getInstance();
        if (dcMonitor == null || dcArbitra == null) {
            HwHiLog.e(TAG, false, "dcMonitor or dcArbitra is null", new Object[0]);
            return false;
        }
        boolean isWifiAndP2pStateAllowDc = isWifiAndP2pStateAllowDc();
        boolean isHilinkGateway = dcArbitra.isHilinkGateway();
        boolean isGameStarted = dcMonitor.isGameStarted();
        HwHiLog.i(TAG, false, "isDcConnected=%{public}s isDcAllowedByRssi=%{public}s isWifiAndP2pStateAllowDc=%{public}s isHilinkGateway=%{public}s gameStarted=%{public}s", new Object[]{String.valueOf(this.mIsDcConnected), String.valueOf(this.mIsDcAllowedByRssi), String.valueOf(isWifiAndP2pStateAllowDc), String.valueOf(isHilinkGateway), String.valueOf(isGameStarted)});
        if (this.mIsDcConnected || !isWifiAndP2pStateAllowDc || !this.mIsDcAllowedByRssi || !isHilinkGateway || !isGameStarted) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void openHilinkService() {
        HwHiLog.d(TAG, false, "openHilinkService isHilinkServiceOpened=%{public}s", new Object[]{String.valueOf(this.mHwHilinkProxyController.isOpened())});
        this.mHwHilinkProxyController.openHilinkServiceProxy(this.mModuleType.getValue(), this.mContext);
        if (this.mHwHilinkProxyController.isOpened()) {
            sendMessage(19);
        }
    }

    public void handleScreenStateChanged(boolean isScreenOn) {
        HwHiLog.d(TAG, false, "handleScreenStateChanged, isScreenOn=%{public}s", new Object[]{Boolean.valueOf(isScreenOn)});
        if (!isScreenOn) {
            this.mElapsedScreenOffTime = SystemClock.elapsedRealtime();
        } else if (this.mElapsedScreenOffTime != 0 && this.mHwHilinkProxyController.isOpened() && SystemClock.elapsedRealtime() - this.mElapsedScreenOffTime > 100000) {
            HwHiLog.d(TAG, false, "screen off > 100s, reset hilink", new Object[0]);
            sendMessage(18);
            this.mElapsedScreenOffTime = 0;
            DCController.getInstance().getDCControllerHandler().sendEmptyMessage(18);
        }
    }

    public void handleP2pConnected(String p2pGroupInterface) {
        if (TextUtils.isEmpty(p2pGroupInterface)) {
            HwHiLog.d(TAG, false, "p2p interface is empty", new Object[0]);
            return;
        }
        String macAddress = "";
        try {
            NetworkInterface p2pInterface = NetworkInterface.getByName(p2pGroupInterface);
            if (!(p2pInterface == null || p2pInterface.getHardwareAddress() == null)) {
                if (p2pInterface.getHardwareAddress().length == 6) {
                    byte[] macBytes = p2pInterface.getHardwareAddress();
                    StringBuilder builder = new StringBuilder();
                    for (byte macByte : macBytes) {
                        builder.append(String.format(Locale.ENGLISH, "%02X:", Byte.valueOf(macByte)));
                    }
                    if (builder.length() > 0) {
                        builder.deleteCharAt(builder.length() - 1);
                    }
                    HwHiLog.d(TAG, false, "p2p interface deviceAddress %{private}s", new Object[]{builder.toString()});
                    macAddress = builder.toString();
                    this.mP2pMacAddress = macAddress;
                    return;
                }
            }
            HwHiLog.d(TAG, false, "p2pInterface is null", new Object[0]);
        } catch (SocketException e) {
            HwHiLog.e(TAG, false, "SocketException exception when getLocalMacAddress", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void detectDCManager() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null) {
            HwHiLog.e(TAG, false, "wifiManager is null", new Object[0]);
            return;
        }
        this.mBssid = wifiManager.getConnectionInfo().getBSSID();
        HwHiLog.d(TAG, false, "prepare to detect hilik ability, mDiscoverRetryTimes: %{public}d", new Object[]{Integer.valueOf(this.mDiscoverRetryTimes)});
        this.mDiscoverRetryTimes++;
        HwHilinkProxyController hwHilinkProxyController = this.mHwHilinkProxyController;
        Objects.requireNonNull(hwHilinkProxyController);
        HwHilinkProxyController.ServiceFoundCallback serviceFoundCallback = new HwHilinkProxyController.ServiceFoundCallback();
        serviceFoundCallback.bssid = this.mBssid;
        serviceFoundCallback.serviceType = this.mServiceType.getValue();
        int ret = this.mHwHilinkProxyController.discover(serviceFoundCallback);
        if (ret != 0) {
            HwHiLog.d(TAG, false, "discover Service failed! ret = %{public}d", new Object[]{Integer.valueOf(ret)});
            sendMessage(21);
        }
    }

    /* access modifiers changed from: private */
    public int parseErrcode(String payload) {
        try {
            int errcode = new JSONObject(payload).getInt("errcode");
            HwHiLog.d(TAG, false, "errcode: %{public}d", new Object[]{Integer.valueOf(errcode)});
            return errcode;
        } catch (JSONException e) {
            HwHiLog.e(TAG, false, "JSONException when parseErrcode", new Object[0]);
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public void sendDcActionFailMessage(int actionType) {
        if (actionType == 1) {
            sendMessage(23);
        } else if (actionType == 2) {
            sendMessage(26);
        } else if (actionType == 3) {
            sendMessage(28);
        }
    }

    private class ResponseCallback extends ResponseCallbackWrapper {
        private static final int ERRORCODE_MAX_REQUEST_NUM_REACHED = 9;
        private static final int ERRORCODE_NO_NETWORK = 3;
        private static final int ERRORCODE_RUNTIME = 4;
        private static final int ERRORCODE_TIMEOUT = 1;

        private ResponseCallback() {
        }

        @Override // com.huawei.hilink.framework.aidl.ResponseCallbackWrapper, com.huawei.hilink.framework.aidl.IResponseCallback
        public void onRecieveError(int errorCode) throws RemoteException {
            HwHiLog.d(DCHilinkController.TAG, false, "response recieve error : %{public}d", new Object[]{Integer.valueOf(errorCode)});
            if (errorCode == 1) {
                DCHilinkController dCHilinkController = DCHilinkController.this;
                dCHilinkController.sendDcActionFailMessage(dCHilinkController.mDcActionType);
            }
        }

        @Override // com.huawei.hilink.framework.aidl.ResponseCallbackWrapper, com.huawei.hilink.framework.aidl.IResponseCallback
        public void onRecieveResponse(int callId, String payload) throws RemoteException {
            HwHiLog.d(DCHilinkController.TAG, false, "response recieve callID : %{public}d, payload: %{private}s", new Object[]{Integer.valueOf(callId), payload});
            if (DCHilinkController.this.parseErrcode(payload) == 0) {
                int unused = DCHilinkController.this.mDcActionRetryTimes = 0;
                Handler dcHandler = DCController.getInstance().getDCControllerHandler();
                if (DCHilinkController.this.mDcActionType == 1) {
                    DCHilinkController.this.sendMessage(24);
                    dcHandler.sendMessage(dcHandler.obtainMessage(24, 0, 0, payload));
                } else if (DCHilinkController.this.mDcActionType == 2) {
                    DCHilinkController.this.sendMessage(14);
                    dcHandler.sendEmptyMessage(14);
                } else if (DCHilinkController.this.mDcActionType == 3) {
                    DCHilinkController.this.sendMessage(16);
                }
            } else {
                DCHilinkController dCHilinkController = DCHilinkController.this;
                dCHilinkController.sendDcActionFailMessage(dCHilinkController.mDcActionType);
            }
        }
    }

    public void sendActionToHilink(int actionType) {
        this.mDcActionType = actionType;
        this.mDcActionRetryTimes++;
        HwHiLog.d(TAG, false, "prepare to send hilik message, mDcActionType=%{public}d mDcActionRetryTimes=%{public}d", new Object[]{Integer.valueOf(this.mDcActionType), Integer.valueOf(this.mDcActionRetryTimes)});
        String payload = buildHilinkPayload(actionType);
        if (TextUtils.isEmpty(payload)) {
            HwHiLog.d(TAG, false, "payload is null", new Object[0]);
            sendDcActionFailMessage(this.mDcActionType);
            return;
        }
        HwHiLog.d(TAG, false, "payload length:%{public}d payload:%{private}s", new Object[]{Integer.valueOf(payload.length()), payload});
        int ret = this.mHwHilinkProxyController.call(1, payload, this.mServiceType.getValue(), new ResponseCallback());
        if (ret != 0) {
            HwHiLog.e(TAG, false, "call failed! ret=%{public}d", new Object[]{Integer.valueOf(ret)});
            sendDcActionFailMessage(this.mDcActionType);
        }
    }

    private String buildHilinkPayload(int actionType) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        String sn = Settings.Secure.getString(this.mContext.getContentResolver(), "android_id");
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        if (actionType == 1) {
            JSONObject jsonConnect = new JSONObject();
            String wifiAddr = wifiInfo.getMacAddress();
            if (TextUtils.isEmpty(wifiAddr)) {
                return null;
            }
            jsonConnect.put("mac", wifiAddr.toUpperCase(Locale.ROOT));
            String p2pAddr = DCUtils.wifiAddr2p2pAddr(wifiAddr);
            if (TextUtils.isEmpty(p2pAddr)) {
                return null;
            }
            JSONObject p2pConnect = new JSONObject();
            p2pConnect.put("mac", p2pAddr);
            HwHiLog.d(TAG, false, "%{private}s: snd_mac_addr: %{private}s", new Object[]{wifiAddr, p2pAddr});
            jsonArray.put(jsonConnect);
            jsonArray.put(p2pConnect);
            jsonObject.put("action", "get");
            jsonObject.put("sn", sn);
            jsonObject.put("deviceinfo", jsonArray);
        } else if (actionType != 2) {
            if (actionType != 3) {
                return null;
            }
            try {
                DCConfiguration selectedNetwork = DCArbitra.getInstance().getSelectedDCConfig();
                if (selectedNetwork != null) {
                    if (selectedNetwork.getInterface() != null) {
                        JSONObject jsonDeviceInfo = new JSONObject();
                        jsonDeviceInfo.put("interface", selectedNetwork.getInterface());
                        jsonDeviceInfo.put("mac", this.mP2pMacAddress);
                        jsonArray.put(jsonDeviceInfo);
                        jsonObject.put("action", "disconnect");
                        jsonObject.put("sn", sn);
                        jsonObject.put("deviceinfo", jsonArray);
                    }
                }
                return null;
            } catch (JSONException e) {
                HwHiLog.e(TAG, false, "Json Exception when buildHilinkPayload", new Object[0]);
            }
        } else if (TextUtils.isEmpty(this.mP2pMacAddress)) {
            return null;
        } else {
            DCConfiguration dcSelectedNetwork = DCArbitra.getInstance().getSelectedDCConfig();
            if (dcSelectedNetwork == null) {
                return null;
            }
            if (dcSelectedNetwork.getInterface() == null) {
                return null;
            }
            List<DCConfiguration> dcConfigList = DCArbitra.getInstance().getDCConfigList();
            if (dcConfigList == null) {
                return null;
            }
            if (dcConfigList.size() == 0) {
                return null;
            }
            DCConfiguration wifiDcConfig = null;
            Iterator<DCConfiguration> it = dcConfigList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                DCConfiguration dcConfig = it.next();
                if (dcConfig.getBSSID() != null && dcConfig.getBSSID().equalsIgnoreCase(wifiInfo.getBSSID())) {
                    wifiDcConfig = dcConfig;
                    break;
                }
            }
            if (wifiDcConfig == null) {
                return null;
            }
            if (wifiDcConfig.getInterface() == null) {
                return null;
            }
            JSONObject jsonObjectWifi = new JSONObject();
            JSONObject jsonObjectP2p = new JSONObject();
            jsonObjectWifi.put("interface", wifiDcConfig.getInterface());
            jsonObjectP2p.put("interface", dcSelectedNetwork.getInterface());
            jsonObjectWifi.put("mac", wifiInfo.getMacAddress().toUpperCase(Locale.ENGLISH));
            jsonObjectP2p.put("mac", this.mP2pMacAddress);
            jsonArray.put(jsonObjectWifi);
            jsonArray.put(jsonObjectP2p);
            jsonObject.put("action", "connect");
            jsonObject.put("sn", sn);
            jsonObject.put("deviceinfo", jsonArray);
        }
        return jsonObject.toString();
    }

    /* access modifiers changed from: private */
    public void logStateAndMessage(State state, Message message) {
        HwHiLog.d(TAG, false, "%{public}s: handle message: %{public}s", new Object[]{state.getClass().getSimpleName(), DCUtils.getStateAndMessageString(state, message)});
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            HwHiLog.d(DCHilinkController.TAG, false, "%{public}s enter.", new Object[]{getName()});
        }

        public boolean processMessage(Message message) {
            DCHilinkController.this.logStateAndMessage(this, message);
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class IdleState extends State {
        IdleState() {
        }

        public void enter() {
            HwHiLog.d(DCHilinkController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            DCHilinkController.this.initHilinkControllerParams();
        }

        public boolean processMessage(Message message) {
            DCHilinkController.this.logStateAndMessage(this, message);
            int i = message.what;
            if (!(i == 0 || i == 6 || i == 18)) {
                if (i == 29) {
                    if (!DCHilinkController.this.isWifiAndP2pStateAllowDc()) {
                        HwHiLog.d(DCHilinkController.TAG, false, "p2p interface exists, delay send msg", new Object[0]);
                        DCHilinkController.this.sendMessageDelayed(36, 1000);
                    }
                    DCHilinkController.this.closeHilinkService();
                } else if (i != 36) {
                    if (i != 33) {
                        if (i != 34) {
                            return true;
                        }
                        boolean unused = DCHilinkController.this.mIsDcAllowedByRssi = false;
                        return true;
                    }
                }
                boolean unused2 = DCHilinkController.this.mIsDcAllowedByRssi = true;
            }
            int unused3 = DCHilinkController.this.mDiscoverRetryTimes = 0;
            if (DCHilinkController.this.isDcAllowed()) {
                if (DCHilinkController.this.mDcHilinkHandler != null && DCHilinkController.this.mDcHilinkHandler.hasMessages(36)) {
                    DCHilinkController.this.mDcHilinkHandler.removeMessages(36);
                }
                DCHilinkController dCHilinkController = DCHilinkController.this;
                dCHilinkController.transitionTo(dCHilinkController.mDiscoverState);
            }
            return true;
        }
    }

    /* access modifiers changed from: package-private */
    public class DiscoverState extends State {
        DiscoverState() {
        }

        public void enter() {
            HwHiLog.d(DCHilinkController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            boolean unused = DCHilinkController.this.mIsEnterDiscoverState = true;
            DCHilinkController.this.openHilinkService();
            int unused2 = DCHilinkController.this.mDcActionRetryTimes = 0;
            int unused3 = DCHilinkController.this.mDiscoverRetryTimes = 0;
        }

        public boolean processMessage(Message message) {
            DCHilinkController.this.logStateAndMessage(this, message);
            int i = message.what;
            if (!(i == 1 || i == 7 || i == 29 || i == 3 || i == 4)) {
                if (i == 33) {
                    boolean unused = DCHilinkController.this.mIsDcAllowedByRssi = true;
                } else if (i != 34) {
                    switch (i) {
                        case 18:
                            DCHilinkController.this.deferMessage(message);
                            DCHilinkController dCHilinkController = DCHilinkController.this;
                            dCHilinkController.transitionTo(dCHilinkController.mIdleState);
                            break;
                        case 19:
                            DCHilinkController.this.detectDCManager();
                            break;
                        case 20:
                            DCHilinkController dCHilinkController2 = DCHilinkController.this;
                            dCHilinkController2.transitionTo(dCHilinkController2.mActiveState);
                            break;
                        case 21:
                            if (DCHilinkController.this.mDiscoverRetryTimes > 3) {
                                DCHilinkController dCHilinkController3 = DCHilinkController.this;
                                dCHilinkController3.transitionTo(dCHilinkController3.mIdleState);
                                break;
                            } else {
                                DCHilinkController.this.sendMessageDelayed(19, 3000);
                                break;
                            }
                        default:
                            return true;
                    }
                } else {
                    boolean unused2 = DCHilinkController.this.mIsDcAllowedByRssi = false;
                }
                return true;
            }
            DCHilinkController dCHilinkController4 = DCHilinkController.this;
            dCHilinkController4.transitionTo(dCHilinkController4.mIdleState);
            return true;
        }

        public void exit() {
            boolean unused = DCHilinkController.this.mIsEnterDiscoverState = false;
        }
    }

    /* access modifiers changed from: package-private */
    public class ActiveState extends State {
        ActiveState() {
        }

        public void enter() {
            HwHiLog.d(DCHilinkController.TAG, false, "%{public}s enter.", new Object[]{getName()});
            boolean unused = DCHilinkController.this.mIsDcConfigDetected = false;
            boolean unused2 = DCHilinkController.this.mIsDcConnected = false;
            DCHilinkController.this.sendMessage(22);
        }

        public boolean processMessage(Message message) {
            DCHilinkController.this.logStateAndMessage(this, message);
            DCController dcController = DCController.getInstance();
            DCMonitor dcMonitor = DCMonitor.getInstance();
            int i = message.what;
            if (!(i == 1 || i == 3)) {
                if (i != 7) {
                    if (i == 14) {
                        boolean unused = DCHilinkController.this.mIsDcConnected = true;
                    } else if (i != 16) {
                        if (i != 18) {
                            if (i == 31) {
                                DCHilinkController dCHilinkController = DCHilinkController.this;
                                dCHilinkController.transitionTo(dCHilinkController.mIdleState);
                            } else if (i == 33) {
                                boolean unused2 = DCHilinkController.this.mIsDcAllowedByRssi = true;
                            } else if (i != 34) {
                                switch (i) {
                                    case 10:
                                    case 11:
                                    case 12:
                                        break;
                                    default:
                                        switch (i) {
                                            case 22:
                                                if (!DCHilinkController.this.mIsDcConfigDetected) {
                                                    HwHiLog.d(DCHilinkController.TAG, false, "start to get DC config", new Object[0]);
                                                    DCHilinkController.this.sendActionToHilink(1);
                                                    break;
                                                }
                                                break;
                                            case 23:
                                                DCHilinkController.this.mDcChr.uploadDcGetConfigFailCount();
                                                if (DCHilinkController.this.mDcActionRetryTimes >= 5) {
                                                    DCHilinkController dCHilinkController2 = DCHilinkController.this;
                                                    dCHilinkController2.transitionTo(dCHilinkController2.mIdleState);
                                                    break;
                                                } else {
                                                    DCHilinkController.this.sendMessageDelayed(22, 3000);
                                                    break;
                                                }
                                            case 24:
                                                boolean unused3 = DCHilinkController.this.mIsDcConfigDetected = true;
                                                break;
                                            case 25:
                                                DCHilinkController.this.sendActionToHilink(2);
                                                break;
                                            case 26:
                                                DCHilinkController.this.mDcChr.uploadDcHilinkConnectFailCount();
                                                if (DCHilinkController.this.mDcActionRetryTimes >= 5) {
                                                    if (dcController != null) {
                                                        dcController.getDCControllerHandler().sendEmptyMessage(15);
                                                    }
                                                    DCHilinkController dCHilinkController3 = DCHilinkController.this;
                                                    dCHilinkController3.transitionTo(dCHilinkController3.mIdleState);
                                                    break;
                                                } else {
                                                    DCHilinkController.this.sendMessageDelayed(25, 3000);
                                                    break;
                                                }
                                            case 27:
                                                DCHilinkController.this.sendActionToHilink(3);
                                                break;
                                            case 28:
                                                if (DCHilinkController.this.mDcActionRetryTimes >= 5) {
                                                    if (dcController != null) {
                                                        dcController.getDCControllerHandler().sendEmptyMessage(17);
                                                    }
                                                    if (dcMonitor != null && dcMonitor.isGameStarted()) {
                                                        DCHilinkController.this.sendMessage(6);
                                                    }
                                                    DCHilinkController dCHilinkController4 = DCHilinkController.this;
                                                    dCHilinkController4.transitionTo(dCHilinkController4.mIdleState);
                                                    break;
                                                } else {
                                                    DCHilinkController.this.sendMessageDelayed(27, 3000);
                                                    break;
                                                }
                                            case 29:
                                                break;
                                            default:
                                                return true;
                                        }
                                }
                            } else {
                                boolean unused4 = DCHilinkController.this.mIsDcAllowedByRssi = false;
                            }
                        }
                        DCHilinkController.this.deferMessage(message);
                        DCHilinkController dCHilinkController5 = DCHilinkController.this;
                        dCHilinkController5.transitionTo(dCHilinkController5.mIdleState);
                    } else if (dcMonitor != null && dcMonitor.isGameStarted()) {
                        DCHilinkController.this.sendMessage(6);
                    }
                } else if (!DCHilinkController.this.mIsDcConnected) {
                    DCHilinkController dCHilinkController6 = DCHilinkController.this;
                    dCHilinkController6.transitionTo(dCHilinkController6.mIdleState);
                }
                return true;
            }
            DCHilinkController dCHilinkController7 = DCHilinkController.this;
            dCHilinkController7.transitionTo(dCHilinkController7.mIdleState);
            return true;
        }

        public void exit() {
            boolean unused = DCHilinkController.this.mIsDcConfigDetected = false;
            boolean unused2 = DCHilinkController.this.mIsDcConnected = false;
        }
    }
}

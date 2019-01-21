package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.rms.iaware.AppTypeRecoManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.scanner.ScanResultRecords;
import com.huawei.hilink.framework.aidl.CallRequest;
import com.huawei.hilink.framework.aidl.DiscoverRequest;
import com.huawei.hilink.framework.aidl.DiscoverRequest.Builder;
import com.huawei.hilink.framework.aidl.HilinkServiceProxy;
import com.huawei.hilink.framework.aidl.ResponseCallbackWrapper;
import com.huawei.hilink.framework.aidl.ServiceFoundCallbackWrapper;
import com.huawei.hilink.framework.aidl.ServiceRecord;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HwQoEHilink {
    private static final int ACC_LOOP_DELAY_MSEC = 2000;
    private static final int ACC_LOOP_INTVAL_MSEC = 30000;
    private static final int APP_NAME_MAX_LEN = 64;
    private static final int DISCOVER_LIMIT_RETRY_TIMES = 3;
    private static final int GAME_INFO_SPLIT_INDEX_PORT = 2;
    private static final int GAME_INFO_SPLIT_INDEX_UID = 8;
    private static final int GAME_INFO_SPLIT_MIN_NUM = 9;
    private static final int MSG_HIGAME_START = 0;
    private static final int PROTOCOL_TYPE_TCP = 6;
    private static final int PROTOCOL_TYPE_UDP = 17;
    private static final String TAG = "HwQoEHilink";
    private static HwQoEHilink mHwQoEHilink;
    private boolean mAccGameEnabled = false;
    private AccGameHandler mAccGameHandler;
    private String mAppName = "";
    private AppTypeRecoManager mAppTypeRecoManager;
    private final Context mContext;
    private int mHilinkAccRetryTimes = 0;
    private boolean mHilinkAccSupport = false;
    private boolean mHilinkServiceOpened = false;
    private HilinkServiceProxy mHilinkServiceProxy;
    private String mRemoteHilinkIp = "";
    private int mRemoteHilinkPort = -1;

    private class AccGameHandler extends Handler {
        private AccGameHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                HwQoEHilink.this.accGameAction(true);
                if (HwQoEHilink.this.mAccGameEnabled) {
                    HwQoEHilink.this.mAccGameHandler.sendEmptyMessageDelayed(0, 30000);
                }
            }
        }
    }

    private static class ResponseCallback extends ResponseCallbackWrapper {
        private ResponseCallback() {
        }

        public void onRecieveError(int errorCode) throws RemoteException {
            String str = HwQoEHilink.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:response recieve error : ");
            stringBuilder.append(errorCode);
            Log.d(str, stringBuilder.toString());
        }

        public void onRecieveResponse(int callID, String payload) throws RemoteException {
            String str = HwQoEHilink.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:response recieve callID : ");
            stringBuilder.append(callID);
            stringBuilder.append(" payload: ");
            stringBuilder.append(payload);
            Log.d(str, stringBuilder.toString());
        }
    }

    private class ServiceFoundCallback extends ServiceFoundCallbackWrapper {
        private ServiceFoundCallback() {
        }

        public void onFoundError(int errorCode) throws RemoteException {
            String str = HwQoEHilink.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:service found error = ");
            stringBuilder.append(errorCode);
            Log.d(str, stringBuilder.toString());
        }

        public void onFoundService(ServiceRecord serviceRecord) throws RemoteException {
            if (serviceRecord == null) {
                Log.d(HwQoEHilink.TAG, "HwQoEHilink:service Record is null!");
                return;
            }
            HwQoEHilink.this.mHilinkAccSupport = true;
            HwQoEHilink.this.mHilinkAccRetryTimes = 0;
            HwQoEHilink.this.mRemoteHilinkIp = serviceRecord.getRemoteIP();
            HwQoEHilink.this.mRemoteHilinkPort = serviceRecord.getRemotePort();
            HwQoEHilink.this.sendGameAccInfo(true);
        }
    }

    public static synchronized HwQoEHilink getInstance(Context ctx) {
        HwQoEHilink hwQoEHilink;
        synchronized (HwQoEHilink.class) {
            if (mHwQoEHilink == null) {
                mHwQoEHilink = new HwQoEHilink(ctx);
            }
            hwQoEHilink = mHwQoEHilink;
        }
        return hwQoEHilink;
    }

    public void initAccGameParams() {
        Log.d(TAG, "HwQoEHilink:init Game config");
        this.mAppName = "";
        this.mRemoteHilinkIp = "";
        this.mRemoteHilinkPort = -1;
        this.mAccGameEnabled = false;
        this.mHilinkAccSupport = false;
        this.mHilinkAccRetryTimes = 0;
        stopAccTimer();
        if (this.mHilinkServiceOpened) {
            this.mHilinkServiceProxy.close();
            this.mHilinkServiceOpened = false;
        }
    }

    public void handleAppStateChange(String appName) {
        if (!TextUtils.isEmpty(appName)) {
            int type = this.mAppTypeRecoManager.getAppType(appName);
            HwQoEContentAware hwQoEContentAware = HwQoEContentAware.getInstance();
            if (hwQoEContentAware == null) {
                Log.d(TAG, "HwQoEHilink:hwQoEContentAware is null");
                return;
            }
            if (hwQoEContentAware.isGameType(type, appName) && isHilinkGateway()) {
                setAccGameMode(true, appName);
            } else {
                setAccGameMode(false, appName);
            }
        }
    }

    private HwQoEHilink(Context ctx) {
        this.mContext = ctx;
        this.mAppTypeRecoManager = AppTypeRecoManager.getInstance();
        this.mAccGameHandler = new AccGameHandler();
    }

    private boolean isHilinkGateway() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager == null) {
            Log.d(TAG, "HwQoEHilink:wifiManager is null!");
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "HwQoEHilink:wifiInfo is null!");
            return false;
        }
        String bssid = wifiInfo.getBSSID();
        if (bssid == null) {
            Log.d(TAG, "HwQoEHilink:bssid is null!");
            return false;
        } else if (ScanResultRecords.getDefault().isHiLink(bssid)) {
            return true;
        } else {
            return false;
        }
    }

    private void setAccGameMode(boolean enable, String appName) {
        if (this.mAccGameEnabled != enable) {
            if (this.mHilinkAccSupport || this.mHilinkAccRetryTimes <= 3) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HwQoEHilink:enable : ");
                stringBuilder.append(enable);
                stringBuilder.append(", appName : ");
                stringBuilder.append(appName);
                Log.d(str, stringBuilder.toString());
                if (enable) {
                    this.mAppName = appName;
                }
                this.mAccGameEnabled = enable;
                if (!this.mHilinkServiceOpened) {
                    this.mHilinkServiceProxy = new HilinkServiceProxy(this.mContext);
                    this.mHilinkServiceOpened = true;
                }
                if (enable) {
                    startAccTimer();
                } else {
                    stopAccTimer();
                    if (this.mHilinkAccSupport) {
                        sendGameAccInfo(false);
                    }
                }
                return;
            }
            Log.d(TAG, "HwQoEHilink:setAccGameMode,router don't support game acceleration!");
            this.mAccGameEnabled = false;
            stopAccTimer();
        }
    }

    private void startAccTimer() {
        Log.d(TAG, "HwQoEHilink:start acc timer");
        this.mAccGameHandler.sendEmptyMessageDelayed(0, 2000);
    }

    private void stopAccTimer() {
        Log.d(TAG, "HwQoEHilink:stop acc timer");
        if (this.mAccGameHandler.hasMessages(0)) {
            this.mAccGameHandler.removeMessages(0);
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0025, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void accGameAction(boolean accelerate) {
        if (!this.mHilinkAccSupport && this.mHilinkAccRetryTimes > 3) {
            Log.d(TAG, "HwQoEHilink:accGameAction,router don't support game acceleration!");
            this.mAccGameEnabled = false;
            stopAccTimer();
        } else if (this.mHilinkAccSupport) {
            sendGameAccInfo(accelerate);
        } else {
            detectAndSendGameAccInfo();
        }
    }

    private void detectAndSendGameAccInfo() {
        Builder builder = new Builder();
        builder.setServiceType("st=appawareMngr");
        DiscoverRequest request = builder.build();
        Log.d(TAG, "HwQoEHilink:prepare to detect hilik ability");
        this.mHilinkAccRetryTimes++;
        if (request == null || !this.mHilinkServiceOpened) {
            Log.d(TAG, "HwQoEHilink:discover request or HilinkServiceProxy is null!");
            return;
        }
        int ret = this.mHilinkServiceProxy.discover(request, new ServiceFoundCallback());
        if (ret != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:discover Service failed! ret = ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void sendGameAccInfo(boolean accelerate) {
        if (!this.mHilinkAccSupport) {
            Log.d(TAG, "HwQoEHilink:sendGameAccInfo,router don't support game acceleration!");
        } else if (TextUtils.isEmpty(this.mRemoteHilinkIp) || this.mRemoteHilinkPort < 0) {
            Log.d(TAG, "HwQoEHilink:remote hilink ip/prot is invalid, don't send info to it");
        } else {
            Log.d(TAG, "HwQoEHilink:prepare to send hilik message");
            String payload = buildHilinkPayload(accelerate);
            if (payload != null) {
                CallRequest.Builder builder = new CallRequest.Builder();
                builder.setServiceID("appawareMngr");
                builder.setMethod(1);
                builder.setRemoteIP(this.mRemoteHilinkIp).setRemotePort(this.mRemoteHilinkPort);
                builder.setPayload(payload);
                CallRequest request = builder.build();
                if (request == null || !this.mHilinkServiceOpened) {
                    Log.d(TAG, "HwQoEHilink:call request or HilinkServiceProxy is null!");
                } else {
                    int ret = this.mHilinkServiceProxy.call(request, new ResponseCallback());
                    if (ret != 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HwQoEHilink:call failed! ret = ");
                        stringBuilder.append(ret);
                        Log.d(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private String buildHilinkPayload(boolean accelerate) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        if (this.mAppName == null) {
            Log.d(TAG, "HwQoEHilink:appName is null!");
            return null;
        }
        HwQoEContentAware hwQoEContentAware = HwQoEContentAware.getInstance();
        if (hwQoEContentAware == null) {
            Log.d(TAG, "HwQoEHilink:hwQoEContentAware is null");
            return null;
        }
        int appUid = hwQoEContentAware.getAppUid(this.mAppName);
        String str;
        StringBuilder stringBuilder;
        if (appUid < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:the game is not exist,appName : ");
            stringBuilder.append(this.mAppName);
            Log.d(str, stringBuilder.toString());
            this.mAccGameEnabled = false;
            stopAccTimer();
            return null;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("HwQoEHilink:enable:");
        stringBuilder.append(accelerate);
        stringBuilder.append(", appUid : ");
        stringBuilder.append(appUid);
        stringBuilder.append(", appName : ");
        stringBuilder.append(this.mAppName);
        Log.d(str, stringBuilder.toString());
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager == null) {
            Log.d(TAG, "HwQoEHilink:wifiManager is null!");
            return null;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "HwQoEHilink:wifiInfo is null!");
            return null;
        }
        String localIP = transIPHexToStr(wifiInfo.getIpAddress());
        try {
            String appName = "";
            if (this.mAppName.length() > APP_NAME_MAX_LEN) {
                appName = this.mAppName.substring(0, APP_NAME_MAX_LEN);
            } else {
                appName = this.mAppName;
            }
            if (accelerate) {
                buildSocketInfo(appUid, 6, jsonArray, localIP);
                buildSocketInfo(appUid, 17, jsonArray, localIP);
                if (jsonArray.length() == 0) {
                    Log.d(TAG, "HwQoEHilink:do not found game 5 elements");
                    return null;
                }
                jsonObject.put("action", "create");
                jsonObject.put("pkgName", appName);
                jsonObject.put("accelMode", "");
                jsonObject.put("data", jsonArray);
            } else {
                JSONObject json5elem = new JSONObject();
                json5elem.put("clientIp", localIP);
                json5elem.put("clientPort", 0);
                json5elem.put("serverIp", "");
                json5elem.put("serverPort", 0);
                json5elem.put("proto", 0);
                jsonArray.put(json5elem);
                jsonObject.put("action", "delete");
                jsonObject.put("pkgName", appName);
                jsonObject.put("accelMode", "");
                jsonObject.put("data", jsonArray);
            }
        } catch (JSONException e) {
            Log.e(TAG, "HwQoEHilink: Json Exception", e);
        } catch (IOException e2) {
            Log.e(TAG, "HwQoEHilink: IO Exception", e2);
        }
        return jsonObject.toString();
    }

    private String transIPHexToStr(int ip) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ip & 255);
        stringBuilder.append(".");
        stringBuilder.append((ip >> 8) & 255);
        stringBuilder.append(".");
        stringBuilder.append((ip >> 16) & 255);
        stringBuilder.append(".");
        stringBuilder.append((ip >> 24) & 255);
        return stringBuilder.toString();
    }

    private void buildSocketInfo(int uid, int protoType, JSONArray jsonArray, String clientIp) throws IOException, JSONException {
        String filePath;
        String str;
        JSONArray jSONArray;
        int i = protoType;
        if (i == 6) {
            filePath = "/proc/net/tcp";
        } else if (i != 17) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwQoEHilink:invalid protocol : ");
            stringBuilder.append(i);
            Log.d(str, stringBuilder.toString());
            return;
        } else {
            filePath = "/proc/net/udp";
        }
        str = filePath;
        InputStreamReader ir = null;
        String filePath2;
        try {
            FileInputStream fs = new FileInputStream(str);
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(fs, "UTF-8"));
                Pattern patternPort = Pattern.compile(":([0-9|A-F|a-f]{4})");
                String strLoopIp = "0100007F";
                String strUid = Integer.toString(uid);
                StringBuffer bufDbg = new StringBuffer();
                filePath = br.readLine();
                while (true) {
                    String lineData = filePath;
                    if (lineData != null) {
                        String[] splitData = lineData.split("\\s+");
                        if (splitData.length < 9) {
                            Log.d(TAG, "HwQoEHilink:file data is not correct!");
                            br.close();
                            return;
                        }
                        if (splitData[8].equals(strUid)) {
                            if (splitData[2].contains(strLoopIp)) {
                                filePath = br.readLine();
                            } else {
                                Matcher matcher = patternPort.matcher(splitData[2]);
                                if (matcher.find()) {
                                    int clientPort = 0;
                                    try {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("0x");
                                        stringBuilder2.append(matcher.group(1));
                                        clientPort = Integer.decode(stringBuilder2.toString()).intValue();
                                        JSONObject json5elem = new JSONObject();
                                        filePath2 = str;
                                        json5elem.put("clientIp", clientIp);
                                        json5elem.put("clientPort", clientPort);
                                        json5elem.put("serverIp", "");
                                        json5elem.put("serverPort", 0);
                                        json5elem.put("proto", i);
                                        jsonArray.put(json5elem);
                                        bufDbg.append(" ");
                                        bufDbg.append(clientPort);
                                        filePath = br.readLine();
                                        str = filePath2;
                                    } catch (Exception e) {
                                        jSONArray = jsonArray;
                                        filePath2 = str;
                                        str = clientIp;
                                        Log.e(TAG, "HwQoEHilink:getFlowInfo Exception", e);
                                        br.close();
                                        return;
                                    }
                                }
                            }
                        }
                        jSONArray = jsonArray;
                        filePath2 = str;
                        filePath = br.readLine();
                        str = filePath2;
                    } else {
                        jSONArray = jsonArray;
                        filePath2 = str;
                        filePath = bufDbg.toString();
                        if (!TextUtils.isEmpty(filePath)) {
                            str = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("HwQoEHilink:protocol: ");
                            stringBuilder3.append(i);
                            stringBuilder3.append(", port : ");
                            stringBuilder3.append(filePath);
                            Log.d(str, stringBuilder3.toString());
                        }
                        br.close();
                        return;
                    }
                }
            } catch (UnsupportedEncodingException e2) {
                jSONArray = jsonArray;
                filePath2 = str;
                Log.e(TAG, "HwQoEHilink:UnsupportedEncoding Exception", e2);
                fs.close();
            }
        } catch (FileNotFoundException e3) {
            jSONArray = jsonArray;
            filePath2 = str;
            Log.e(TAG, "HwQoEHilink:FileNotFound Exception", e3);
        }
    }
}

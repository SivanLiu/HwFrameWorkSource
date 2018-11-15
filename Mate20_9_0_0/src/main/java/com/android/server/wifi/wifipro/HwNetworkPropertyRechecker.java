package com.android.server.wifi.wifipro;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.server.HwNetworkPropertyChecker;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwNetworkPropertyRechecker extends HwNetworkPropertyChecker {
    public static final int MSG_REQUEST_NETWORK_CHECK = 101;
    private boolean checkRunning;
    private Object mCheckLock = new Object();
    private Handler mLocalHandler = null;
    private NetworkQosMonitor mNetworkQosMonitor;

    public HwNetworkPropertyRechecker(Context context, WifiManager wifiManager, TelephonyManager telManager, boolean enabled, NetworkAgentInfo agent, NetworkQosMonitor monitor) {
        super(context, wifiManager, telManager, enabled, agent, false);
        this.mNetworkQosMonitor = monitor;
        this.checkRunning = false;
        init();
    }

    private void init() {
        HandlerThread handlerThread = new HandlerThread("wifipro_network_rechecker_thread");
        handlerThread.start();
        this.mLocalHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == 101) {
                    synchronized (HwNetworkPropertyRechecker.this.mCheckLock) {
                        HwNetworkPropertyRechecker.this.checkRunning = true;
                        String startSsid = WifiProCommonUtils.getCurrentSsid(HwNetworkPropertyRechecker.this.mWifiManager);
                        boolean isWifiBackground = false;
                        if (msg.obj != null) {
                            isWifiBackground = ((Boolean) msg.obj).booleanValue();
                        }
                        int respCode = HwNetworkPropertyRechecker.this.recheckNetworkProperty(msg.arg1 == 1, msg.arg2 == 1, true, isWifiBackground);
                        String endSsid = WifiProCommonUtils.getCurrentSsid(HwNetworkPropertyRechecker.this.mWifiManager);
                        HwNetworkPropertyRechecker hwNetworkPropertyRechecker = HwNetworkPropertyRechecker.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("startSsid = ");
                        stringBuilder.append(startSsid);
                        stringBuilder.append(", endSsid = ");
                        stringBuilder.append(endSsid);
                        hwNetworkPropertyRechecker.LOGW(stringBuilder.toString());
                        if (startSsid != null && startSsid.equals(endSsid)) {
                            HwNetworkPropertyRechecker.this.mNetworkQosMonitor.notifyNetworkResult(respCode);
                        }
                        HwNetworkPropertyRechecker.this.checkRunning = false;
                    }
                }
                super.handleMessage(msg);
            }
        };
    }

    public void asyncRequestNetworkCheck(int portal, int authen, boolean wifiBackground) {
        this.mLocalHandler.sendMessage(Message.obtain(this.mLocalHandler, 101, portal, authen, Boolean.valueOf(wifiBackground)));
    }

    public int syncRequestNetworkCheck(boolean portal, boolean authen, boolean wifiBackground) {
        if (this.checkRunning) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("syncRequestNetworkCheck, checkRunning = ");
            stringBuilder.append(this.checkRunning);
            stringBuilder.append(", portal = ");
            stringBuilder.append(portal);
            LOGW(stringBuilder.toString());
            return portal ? 302 : 599;
        }
        int recheckNetworkProperty;
        synchronized (this.mCheckLock) {
            recheckNetworkProperty = recheckNetworkProperty(portal, authen, false, wifiBackground);
        }
        return recheckNetworkProperty;
    }

    private int recheckNetworkProperty(boolean portal, boolean authen, boolean needSleep, boolean wifiBackground) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ENTER: recheckNetworkProperty, portal = ");
        stringBuilder.append(portal);
        stringBuilder.append(", authen = ");
        stringBuilder.append(authen);
        stringBuilder.append(", needSleep = ");
        stringBuilder.append(needSleep);
        stringBuilder.append(" wifiBackground = ");
        stringBuilder.append(wifiBackground);
        LOGD(stringBuilder.toString());
        int respCode = isCaptivePortal(true, portal, wifiBackground);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("LEAVE: recheckNetworkProperty, respCode = ");
        stringBuilder2.append(respCode);
        LOGD(stringBuilder2.toString());
        return respCode;
    }

    public void release() {
        super.release();
        if (this.mLocalHandler != null) {
            Looper looper = this.mLocalHandler.getLooper();
            if (looper != null && looper != Looper.getMainLooper()) {
                looper.quitSafely();
                LOGD("HwNetworkPropertyRechecker$HandlerThread::Release");
            }
        }
    }
}

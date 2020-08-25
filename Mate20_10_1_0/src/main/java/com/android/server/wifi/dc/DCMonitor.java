package com.android.server.wifi.dc;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.IWifiActionListener;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.rms.iaware.AppTypeRecoManager;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.server.hidata.HwHidataAppStateInfo;
import com.android.server.hidata.HwHidataManager;
import com.android.server.hidata.IHwHidataCallback;
import com.huawei.hwwifiproservice.WifiHandover;

public class DCMonitor {
    private static final int APP_ACTION_INTERNET_TEACH_MASK = 2;
    private static final int APP_ACTION_TRUE = 2;
    private static final int LATENCY_SIGNAL_THRESHOLD = 2;
    private static final String TAG = "DCMonitor";
    private static final int WIFI_SIGNAL_CHANGED_DELAY_TIME = 4000;
    private static DCMonitor mDCMonitor = null;
    private ActionReceiver mActionReceiver;
    /* access modifiers changed from: private */
    public AppTypeRecoManager mAppTypeRecoManager;
    /* access modifiers changed from: private */
    public Context mContext;
    private String mCurrentBssid = "";
    /* access modifiers changed from: private */
    public DCController mDCController;
    /* access modifiers changed from: private */
    public Handler mDCControllerHandler;
    /* access modifiers changed from: private */
    public DCHilinkController mDCHilinkController;
    /* access modifiers changed from: private */
    public Handler mDCHilinkHandler;
    private IHwHidataCallback mHwHidataCallback = new IHwHidataCallback() {
        /* class com.android.server.wifi.dc.DCMonitor.AnonymousClass1 */

        public void onAppStateChangeCallBack(HwHidataAppStateInfo appStateInfo) {
            if (appStateInfo == null) {
                HwHiLog.d(DCMonitor.TAG, false, "gameStateInfo is null", new Object[0]);
                return;
            }
            String appName = DCMonitor.this.mContext.getPackageManager().getNameForUid(appStateInfo.getCurUid());
            if (appName == null || DCMonitor.this.mAppTypeRecoManager.getAppType(appName) == 9 || (appStateInfo.getAction() & 2) == 2) {
                int curState = appStateInfo.getCurState();
                int curScence = appStateInfo.getCurScence();
                HwHiLog.d(DCMonitor.TAG, false, "onGameStateCallBack: gameStateInfo CurState:%{public}d CurScence:%{public}d", new Object[]{Integer.valueOf(curState), Integer.valueOf(curScence)});
                switch (curState) {
                    case 100:
                    case 103:
                        DCMonitor.this.handleHidataAppState(curScence, true);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(6);
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(6);
                        return;
                    case 101:
                    case 104:
                        DCMonitor.this.handleHidataAppState(curScence, false);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(7);
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(7);
                        return;
                    case 102:
                    default:
                        return;
                }
            } else {
                HwHiLog.d(DCMonitor.TAG, false, "not game type:%{public}d, action:%{public}d", new Object[]{Integer.valueOf(DCMonitor.this.mAppTypeRecoManager.getAppType(appName)), Integer.valueOf(appStateInfo.getAction())});
            }
        }
    };
    private boolean mIsAudioStarted = false;
    private boolean mIsGameStarted = false;
    private boolean mIsHicallStarted = false;
    private boolean mIsHidataAppStarted = false;
    private boolean mIsVideoStarted = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiConnected = false;
    /* access modifiers changed from: private */
    public boolean mIsWifiSignalGood = false;
    private WifiManager mWifiManager;

    private DCMonitor(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mDCController = DCController.createDCController(this.mContext);
        this.mDCControllerHandler = this.mDCController.getDCControllerHandler();
        this.mDCHilinkController = DCHilinkController.createDCHilinkController(this.mContext);
        this.mDCHilinkHandler = this.mDCHilinkController.getDCHilinkHandler();
        this.mAppTypeRecoManager = AppTypeRecoManager.getInstance();
        registerActionReceiver();
    }

    public static DCMonitor createDCMonitor(Context context) {
        if (mDCMonitor == null) {
            mDCMonitor = new DCMonitor(context);
        }
        return mDCMonitor;
    }

    public static DCMonitor getInstance() {
        return mDCMonitor;
    }

    /* access modifiers changed from: package-private */
    public boolean isWifiConnected() {
        return this.mIsWifiConnected;
    }

    /* access modifiers changed from: package-private */
    public boolean isGameStarted() {
        return this.mIsHidataAppStarted;
    }

    /* access modifiers changed from: private */
    public void registerHiDateMonitor() {
        HwHidataManager hwHidataManager = HwHidataManager.getInstance();
        if (hwHidataManager != null) {
            HwHiLog.d(TAG, false, "registerHiDateMonitor", new Object[0]);
            hwHidataManager.registerHidataMonitor(this.mHwHidataCallback);
            return;
        }
        HwHiLog.e(TAG, false, "hwHidataManager is null", new Object[0]);
    }

    public void notifyNetworkRoamingCompleted(String newBssid) {
        if (this.mWifiManager == null || TextUtils.isEmpty(newBssid) || TextUtils.isEmpty(this.mCurrentBssid) || newBssid.equals(this.mCurrentBssid)) {
            HwHiLog.e(TAG, false, "notifyWifiRoamingCompleted, but bssid is unchanged, ignore it", new Object[0]);
            return;
        }
        this.mCurrentBssid = newBssid;
        HwHiLog.d(TAG, false, "NotifyNetworkRoaming completed", new Object[0]);
        Handler handler = this.mDCHilinkHandler;
        if (handler != null) {
            handler.sendEmptyMessage(29);
        }
        Handler handler2 = this.mDCControllerHandler;
        if (handler2 != null) {
            handler2.sendEmptyMessage(29);
        }
    }

    public void dcConnect(WifiConfiguration configuration, IWifiActionListener listener) {
        this.mDCController.dcConnect(configuration, listener);
    }

    public boolean dcDisconnect() {
        return this.mDCController.dcDisconnect();
    }

    /* access modifiers changed from: private */
    public synchronized void handleHidataAppState(int scence, boolean isStarted) {
        boolean z;
        switch (scence) {
            case 100105:
                this.mIsAudioStarted = isStarted;
                break;
            case 100106:
                this.mIsVideoStarted = isStarted;
                break;
            case 101101:
                this.mIsHicallStarted = isStarted;
                break;
            default:
                this.mIsGameStarted = isStarted;
                break;
        }
        if (!this.mIsGameStarted && !this.mIsVideoStarted && !this.mIsAudioStarted) {
            if (!this.mIsHicallStarted) {
                z = false;
                this.mIsHidataAppStarted = z;
            }
        }
        z = true;
        this.mIsHidataAppStarted = z;
    }

    private void registerActionReceiver() {
        this.mActionReceiver = new ActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        this.mContext.registerReceiver(this.mActionReceiver, filter);
    }

    /* access modifiers changed from: private */
    public void handleWifiRssiChanged() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null) {
            HwHiLog.e(TAG, false, "handleWifiRssiChanged: wifiManager is null", new Object[0]);
            return;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            HwHiLog.e(TAG, false, "handleWifiRssiChanged: wifiInfo is null", new Object[0]);
        } else if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(wifiInfo.getFrequency(), wifiInfo.getRssi()) <= 2) {
            if (this.mDCHilinkHandler.hasMessages(33)) {
                this.mDCHilinkHandler.removeMessages(33);
            }
            if (this.mIsWifiSignalGood) {
                this.mIsWifiSignalGood = false;
                this.mDCHilinkHandler.sendEmptyMessageDelayed(34, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
                this.mDCControllerHandler.sendEmptyMessageDelayed(34, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
            }
        } else {
            if (this.mDCHilinkHandler.hasMessages(34)) {
                this.mDCHilinkHandler.removeMessages(34);
            }
            if (this.mDCControllerHandler.hasMessages(34)) {
                this.mDCControllerHandler.removeMessages(34);
            }
            if (!this.mIsWifiSignalGood) {
                this.mIsWifiSignalGood = true;
                this.mDCHilinkHandler.sendEmptyMessageDelayed(33, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
            }
        }
    }

    /* access modifiers changed from: private */
    public synchronized void handleWifiStateChanged(NetworkInfo info) {
        if (info == null) {
            HwHiLog.w(TAG, false, "receive NETWORK_STATE_CHANGED_ACTION but network info is null", new Object[0]);
            return;
        }
        if (NetworkInfo.DetailedState.CONNECTED.equals(info.getDetailedState())) {
            this.mIsWifiConnected = true;
            if (this.mWifiManager == null) {
                HwHiLog.e(TAG, false, "receive NETWORK_STATE_CHANGED_ACTION but wifiManager is null", new Object[0]);
                return;
            }
            WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String wifiBssid = wifiInfo.getBSSID();
                if (!TextUtils.isEmpty(this.mCurrentBssid)) {
                    notifyNetworkRoamingCompleted(wifiBssid);
                }
                this.mCurrentBssid = wifiBssid;
            }
            this.mDCControllerHandler.sendEmptyMessage(0);
            this.mDCHilinkHandler.sendEmptyMessage(0);
        } else if (NetworkInfo.DetailedState.DISCONNECTED.equals(info.getDetailedState())) {
            this.mIsWifiConnected = false;
            this.mIsWifiSignalGood = false;
            this.mCurrentBssid = "";
            this.mDCControllerHandler.sendEmptyMessage(1);
            this.mDCHilinkHandler.sendEmptyMessage(1);
        }
    }

    private class ActionReceiver extends BroadcastReceiver {
        private ActionReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                    int wifiState = intent.getIntExtra("wifi_state", 4);
                    if (wifiState == 3) {
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(2);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(2);
                    } else if (wifiState == 1) {
                        boolean unused = DCMonitor.this.mIsWifiConnected = false;
                        boolean unused2 = DCMonitor.this.mIsWifiSignalGood = false;
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(3);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(3);
                    }
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    DCMonitor.this.handleWifiStateChanged((NetworkInfo) intent.getParcelableExtra("networkInfo"));
                } else if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(action)) {
                    NetworkInfo p2pNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (p2pNetworkInfo != null && p2pNetworkInfo.isConnected()) {
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(4);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(4);
                    } else if (p2pNetworkInfo != null && p2pNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                        DCMonitor.this.mDCControllerHandler.sendEmptyMessage(5);
                        DCMonitor.this.mDCHilinkHandler.sendEmptyMessage(5);
                    }
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    DCMonitor.this.mDCHilinkController.handleScreenStateChanged(false);
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    DCMonitor.this.mDCHilinkController.handleScreenStateChanged(true);
                } else if ("android.net.wifi.SCAN_RESULTS".equals(action)) {
                    DCMonitor.this.mDCController.handleUpdateScanResults();
                } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                    DCMonitor.this.registerHiDateMonitor();
                } else if ("android.net.wifi.RSSI_CHANGED".equals(action)) {
                    DCMonitor.this.handleWifiRssiChanged();
                } else {
                    HwHiLog.d(DCMonitor.TAG, false, "receive other broadcast", new Object[0]);
                }
            }
        }
    }
}

package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;

public class WifiStateAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "WifiStateAction";
    private static WifiStateAction instance = null;
    private boolean mIsFirstInitialized = true;
    private boolean mIsWifiOn = false;
    /* access modifiers changed from: private */
    public int mWifiState = -1;

    private WifiStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_OFF));
        OPCollectLog.r("WifiStateAction", "WifiStateAction");
    }

    public static WifiStateAction getInstance(Context context) {
        WifiStateAction wifiStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new WifiStateAction(context, "WifiStateAction");
            }
            wifiStateAction = instance;
        }
        return wifiStateAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        int state;
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new WifiStateBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            String value = SysEventUtil.OFF;
            if (wifiManager != null && ((state = wifiManager.getWifiState()) == 3 || state == 2)) {
                value = SysEventUtil.ON;
            }
            SysEventUtil.collectKVSysEventData("wireless_networks/wifi_status", SysEventUtil.WIFI_STATUS, value);
            OPCollectLog.r("WifiStateAction", "enabled");
        }
    }

    class WifiStateBroadcastReceiver extends BroadcastReceiver {
        WifiStateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("WifiStateAction", "onReceive: " + action);
                if ("android.net.wifi.WIFI_STATE_CHANGED".equalsIgnoreCase(action)) {
                    int unused = WifiStateAction.this.mWifiState = intent.getIntExtra("wifi_state", 1);
                    WifiStateAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mWifiState == 1) {
            if (this.mIsWifiOn || this.mIsFirstInitialized) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_WIFI_OFF);
                SysEventUtil.collectKVSysEventData("wireless_networks/wifi_status", SysEventUtil.WIFI_STATUS, SysEventUtil.OFF);
                if (this.mIsFirstInitialized) {
                    this.mIsFirstInitialized = false;
                }
                this.mIsWifiOn = false;
                return true;
            }
        } else if (this.mWifiState == 3 && !this.mIsWifiOn) {
            SysEventUtil.collectSysEventData(SysEventUtil.EVENT_WIFI_ON);
            SysEventUtil.collectKVSysEventData("wireless_networks/wifi_status", SysEventUtil.WIFI_STATUS, SysEventUtil.ON);
            this.mIsWifiOn = true;
            return true;
        }
        OPCollectLog.r("WifiStateAction", "ignore transition or duplicate data.");
        return false;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyWifiStateActionInstance();
        return true;
    }

    private static void destroyWifiStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}

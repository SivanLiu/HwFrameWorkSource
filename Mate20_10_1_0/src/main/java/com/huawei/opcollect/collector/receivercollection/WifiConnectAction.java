package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import com.huawei.opcollect.collector.servicecollection.LocationRecordAction;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiConnectAction extends ReceiverAction {
    private static final String DOT = ".";
    private static final Object LOCK = new Object();
    private static final String TAG = "WifiConnectAction";
    private static WifiConnectAction instance = null;
    /* access modifiers changed from: private */
    public int mConnectivityType = -1;
    private String mWifiInfoStr = "";

    private WifiConnectAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_CONNECTED) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_DISCONNECTED));
        OPCollectLog.r("WifiConnectAction", "WifiConnectAction");
    }

    public static WifiConnectAction getInstance(Context context) {
        WifiConnectAction wifiConnectAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new WifiConnectAction(context, "WifiConnectAction");
            }
            wifiConnectAction = instance;
        }
        return wifiConnectAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new WifiConnectBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            SysEventUtil.collectKVSysEventData("wireless_networks/wifi_connect_status", SysEventUtil.WIFI_CONNECT_STATUS, SysEventUtil.OFF);
            SysEventUtil.collectKVSysEventData("wireless_networks/mobile_connect_status", SysEventUtil.MOBILE_CONNECT_STATUS, SysEventUtil.OFF);
            OPCollectLog.r("WifiConnectAction", "enabled");
        }
    }

    class WifiConnectBroadcastReceiver extends BroadcastReceiver {
        WifiConnectBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Bundle bundle;
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("WifiConnectAction", "onReceive: " + action);
                if ("android.net.conn.CONNECTIVITY_CHANGE".equalsIgnoreCase(action) && (bundle = intent.getExtras()) != null) {
                    int unused = WifiConnectAction.this.mConnectivityType = bundle.getInt("networkType");
                    WifiConnectAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e("WifiConnectAction", "context is null.");
            return false;
        }
        ConnectivityManager manager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (manager == null) {
            OPCollectLog.e("WifiConnectAction", "manager is null!");
            return false;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (this.mConnectivityType == 1) {
            if (info == null || info.getType() != 1) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_WIFI_DISCONNECTED, this.mWifiInfoStr);
                SysEventUtil.collectKVSysEventData("wireless_networks/wifi_connect_status", SysEventUtil.WIFI_CONNECT_STATUS, SysEventUtil.OFF);
            } else {
                saveWifiInfo();
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_WIFI_CONNECTED, this.mWifiInfoStr);
                SysEventUtil.collectKVSysEventData("wireless_networks/wifi_connect_status", SysEventUtil.WIFI_CONNECT_STATUS, SysEventUtil.ON);
            }
            if (OdmfActionManager.getInstance().checkIfActionEnabled(OPCollectConstant.LOCATION_ACTION_NAME)) {
                LocationRecordAction locationRecordAction = LocationRecordAction.getInstance(this.mContext);
                synchronized (locationRecordAction.getLock()) {
                    Handler locationHandler = locationRecordAction.getLocationHandler();
                    if (locationHandler != null) {
                        locationHandler.obtainMessage(3, this.mWifiInfoStr).sendToTarget();
                    }
                }
            }
        } else if (this.mConnectivityType != 0) {
            return false;
        } else {
            if (info == null || info.getType() != 0) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_MOBILE_DISCONNECTED);
                SysEventUtil.collectKVSysEventData("wireless_networks/mobile_connect_status", SysEventUtil.MOBILE_CONNECT_STATUS, SysEventUtil.OFF);
            } else {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_MOBILE_CONNECTED);
                SysEventUtil.collectKVSysEventData("wireless_networks/mobile_connect_status", SysEventUtil.MOBILE_CONNECT_STATUS, SysEventUtil.ON);
            }
        }
        return true;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyWifiConnectActionInstance();
        return true;
    }

    private static void destroyWifiConnectActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    private void saveWifiInfo() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                this.mWifiInfoStr = "";
                return;
            }
            JSONObject object = new JSONObject();
            try {
                object.put(OPCollectConstant.WIFI_NAME, wifiInfo.getSSID());
                object.put(OPCollectConstant.WIFI_IP, intToIP(wifiInfo.getIpAddress()));
                object.put(OPCollectConstant.WIFI_SSID, wifiInfo.getSSID());
                object.put(OPCollectConstant.WIFI_BSSID, wifiInfo.getBSSID());
                object.put(OPCollectConstant.WIFI_LEVEL, wifiInfo.getRssi());
                this.mWifiInfoStr = object.toString();
            } catch (JSONException e) {
                this.mWifiInfoStr = "";
            }
        }
    }

    private String intToIP(int intIp) {
        StringBuffer sb = new StringBuffer("");
        sb.append(String.valueOf(intIp & 255));
        sb.append(DOT);
        sb.append(String.valueOf((65535 & intIp) >>> 8));
        sb.append(DOT);
        sb.append(String.valueOf((16777215 & intIp) >>> 16));
        sb.append(DOT);
        sb.append(String.valueOf(intIp >>> 24));
        return sb.toString();
    }
}

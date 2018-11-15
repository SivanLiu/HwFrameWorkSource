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
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.EventIdConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiConnectAction extends Action {
    private static final String TAG = "WifiConnectAction";
    private static WifiConnectAction sInstance = null;
    private int mConnectivityType = -1;
    private WifiConnectBroadcastReceiver mReceiver = null;
    private String mWifiInfoStr = EventIdConstant.PURPOSE_STR_BLANK;

    class WifiConnectBroadcastReceiver extends BroadcastReceiver {
        WifiConnectBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("WifiConnectAction", "onReceive: " + action);
                if ("android.net.conn.CONNECTIVITY_CHANGE".equalsIgnoreCase(action)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        WifiConnectAction.this.mConnectivityType = bundle.getInt("networkType");
                        WifiConnectAction.this.perform();
                    }
                }
            }
        }
    }

    public static synchronized WifiConnectAction getInstance(Context context) {
        WifiConnectAction wifiConnectAction;
        synchronized (WifiConnectAction.class) {
            if (sInstance == null) {
                sInstance = new WifiConnectAction(context, "WifiConnectAction");
            }
            wifiConnectAction = sInstance;
        }
        return wifiConnectAction;
    }

    private WifiConnectAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_CONNECTED) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_WIFI_DISCONNECTED));
        OPCollectLog.r("WifiConnectAction", "WifiConnectAction");
    }

    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new WifiConnectBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this.mContext.registerReceiver(this.mReceiver, intentFilter, OPCollectUtils.OPCOLLECT_PERMISSION, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("WifiConnectAction", "enabled");
        }
    }

    protected boolean execute() {
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
            } else {
                saveWifiInfo();
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_WIFI_CONNECTED, this.mWifiInfoStr);
            }
        } else if (this.mConnectivityType != 0) {
            return false;
        } else {
            if (info == null || info.getType() != 0) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_MOBILE_DISCONNECTED);
            } else {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_MOBILE_CONNECTED);
            }
        }
        return true;
    }

    public boolean perform() {
        return super.perform();
    }

    public void disable() {
        super.disable();
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (WifiConnectAction.class) {
            sInstance = null;
        }
    }

    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            if (this.mReceiver == null) {
                pw.println(indent + "receiver is null");
            } else {
                pw.println(indent + "receiver not null");
            }
        }
    }

    private void saveWifiInfo() {
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                this.mWifiInfoStr = EventIdConstant.PURPOSE_STR_BLANK;
                return;
            }
            JSONObject object = new JSONObject();
            try {
                object.put("name", wifiInfo.getSSID());
                object.put("ip", intToIP(wifiInfo.getIpAddress()));
                object.put("wifiSsid", wifiInfo.getSSID());
                object.put("wifiBssid", wifiInfo.getBSSID());
                this.mWifiInfoStr = object.toString();
            } catch (JSONException e) {
                this.mWifiInfoStr = EventIdConstant.PURPOSE_STR_BLANK;
            }
        }
    }

    private String intToIP(int intIp) {
        StringBuffer sb = new StringBuffer(EventIdConstant.PURPOSE_STR_BLANK);
        sb.append(String.valueOf(intIp & 255));
        sb.append(".");
        sb.append(String.valueOf((65535 & intIp) >>> 8));
        sb.append(".");
        sb.append(String.valueOf((16777215 & intIp) >>> 16));
        sb.append(".");
        sb.append(String.valueOf(intIp >>> 24));
        return sb.toString();
    }
}

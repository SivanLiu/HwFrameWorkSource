package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings.Global;
import com.android.server.hidata.arbitration.HwArbitrationFunction;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;

public class HwAPPQoESystemStateMonitor {
    private static String TAG = "HiData_HwAPPQoESystemStateMonitor";
    public HwAPPStateInfo curAPPStateInfo = new HwAPPStateInfo();
    private IntentFilter emcomIntentFilter = new IntentFilter();
    private IntentFilter intentFilter = new IntentFilter();
    private NetworkInfo mActiveNetworkInfo;
    private BroadcastReceiver mBroadcastReceiver = new SystemBroadcastReceiver();
    private ConnectivityManager mConnectivityManager;
    private int mConnectivityType = 802;
    private Context mContext;
    private Handler mHandler;
    private ContentResolver mResolver;
    private UserDataEnableObserver mUserDataEnableObserver;

    private class SystemBroadcastReceiver extends BroadcastReceiver {
        private SystemBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int wifistatus;
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                wifistatus = intent.getIntExtra("wifi_state", 4);
                if (wifistatus == 1) {
                    HwAPPQoEUtils.logE("WIFI_STATE_DISABLED");
                } else if (wifistatus == 3) {
                    HwAPPQoEUtils.logE("WIFI_STATE_ENABLED");
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                HwAPPQoEUtils.logD(HwAPPQoESystemStateMonitor.TAG, "Wifi Connection State Changed");
                HwAPPQoESystemStateMonitor.this.onReceiveWifiStateChanged(intent);
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                HwAPPQoEUtils.logD(HwAPPQoESystemStateMonitor.TAG, "ACTION_BOOT_COMPLETED");
                HwAPPQoESystemStateMonitor.this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
                HwAPPQoESystemStateMonitor.this.onConnectivityNetworkChange();
                HwAPPQoESystemStateMonitor.this.mHandler.sendEmptyMessage(200);
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                HwAPPQoEUtils.logD(HwAPPQoESystemStateMonitor.TAG, "Connectivity Changed");
                HwAPPQoESystemStateMonitor.this.onConnectivityNetworkChange();
            } else if ("com.android.server.hidata.arbitration.HwArbitrationStateMachine".equals(action)) {
                HwAPPQoEUtils.logD(HwAPPQoESystemStateMonitor.TAG, " HwArbitrationStateMachine broadcast test");
                wifistatus = intent.getIntExtra("MPLinkSuccessNetworkKey", 802);
                if (intent.getIntExtra("MPLinkSuccessUIDKey", -1) == HwAPPQoESystemStateMonitor.this.curAPPStateInfo.mAppUID) {
                    String access$100 = HwAPPQoESystemStateMonitor.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MPLINK_STATE_CHANGE:");
                    stringBuilder.append(wifistatus);
                    HwAPPQoEUtils.logD(access$100, stringBuilder.toString());
                    HwAPPQoESystemStateMonitor.this.mHandler.sendMessage(HwAPPQoESystemStateMonitor.this.mHandler.obtainMessage(201, Integer.valueOf(wifistatus)));
                }
            } else if (HwAPPQoEUtils.EMCOM_PARA_READY_ACTION.equals(action)) {
                wifistatus = intent.getIntExtra(HwAPPQoEUtils.EMCOM_PARA_READY_REC, 0);
                String access$1002 = HwAPPQoESystemStateMonitor.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("emcom para is ready: cotaParaBitRec:");
                stringBuilder2.append(wifistatus);
                HwAPPQoEUtils.logD(access$1002, stringBuilder2.toString());
                if ((wifistatus & 16) == 0) {
                    HwAPPQoEUtils.logD(HwAPPQoESystemStateMonitor.TAG, "broadcast is not for no cell");
                    return;
                }
                HwAPPQoEResourceManger mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
                if (mHwAPPQoEResourceManger != null) {
                    mHwAPPQoEResourceManger.onConfigFilePathChanged();
                }
            } else if ("android.net.wifi.RSSI_CHANGED".equals(action) || "android.intent.action.SCREEN_ON".equals(action)) {
                HwAPPQoESystemStateMonitor.this.startWeakNetworkMonitor();
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                HwAPPQoESystemStateMonitor.this.stopWeakNetworkMonitor();
            }
        }
    }

    private class UserDataEnableObserver extends ContentObserver {
        public UserDataEnableObserver(Handler handler) {
            super(handler);
            HwAPPQoESystemStateMonitor.this.mResolver = HwAPPQoESystemStateMonitor.this.mContext.getContentResolver();
        }

        public void onChange(boolean selfChange) {
            int state = Global.getInt(HwAPPQoESystemStateMonitor.this.mContext.getContentResolver(), "mobile_data", -1);
            String access$100 = HwAPPQoESystemStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User change Data service state = ");
            stringBuilder.append(state);
            HwAPPQoEUtils.logD(access$100, stringBuilder.toString());
            if (state == 0 && HwArbitrationFunction.isInMPLink(HwAPPQoESystemStateMonitor.this.mContext, HwAPPQoESystemStateMonitor.this.curAPPStateInfo.mAppUID)) {
                HwAPPQoEUserAction mHwAPPQoEUserAction = HwAPPQoEUserAction.getInstance();
                if (mHwAPPQoEUserAction != null) {
                    mHwAPPQoEUserAction.resetUserActionType(HwAPPQoESystemStateMonitor.this.curAPPStateInfo.mAppId);
                    HwAPPQoESystemStateMonitor.this.mHandler.sendMessage(HwAPPQoESystemStateMonitor.this.mHandler.obtainMessage(203, 2, -1));
                }
            }
        }

        public void register() {
            HwAPPQoESystemStateMonitor.this.mResolver.registerContentObserver(Global.getUriFor("mobile_data"), false, this);
        }

        public void unregister() {
            HwAPPQoESystemStateMonitor.this.mResolver.unregisterContentObserver(this);
        }
    }

    public HwAPPQoESystemStateMonitor(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        registerBroadcastReceiver();
        this.mUserDataEnableObserver = new UserDataEnableObserver(handler);
        this.mUserDataEnableObserver.register();
    }

    private void registerBroadcastReceiver() {
        this.intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.intentFilter.addAction("com.android.server.hidata.arbitration.HwArbitrationStateMachine");
        this.intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.intentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        this.intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter, "com.huawei.hidata.permission.MPLINK_START_CHECK", null);
        this.emcomIntentFilter.addAction(HwAPPQoEUtils.EMCOM_PARA_READY_ACTION);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.emcomIntentFilter, HwAPPQoEUtils.EMCOM_PARA_UPGRADE_PERMISSION, null);
    }

    private void onReceiveWifiStateChanged(Intent intent) {
        NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        if (netInfo != null && netInfo.getState() == State.CONNECTED) {
            HwAPPChrManager.getInstance().uploadAppChrInfo();
        }
    }

    private void onConnectivityNetworkChange() {
        if (this.mConnectivityManager != null) {
            this.mActiveNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
            String str;
            StringBuilder stringBuilder;
            if (this.mActiveNetworkInfo == null) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onConnectivityNetworkChange-prev_type is:");
                stringBuilder2.append(this.mConnectivityType);
                HwAPPQoEUtils.logD(str, stringBuilder2.toString());
                if (800 == this.mConnectivityType) {
                    this.mHandler.sendEmptyMessage(4);
                } else if (801 == this.mConnectivityType) {
                    this.mHandler.sendEmptyMessage(8);
                }
                this.mConnectivityType = 802;
            } else if (1 == this.mActiveNetworkInfo.getType()) {
                if (true == this.mActiveNetworkInfo.isConnected()) {
                    HwAPPQoEUtils.logD(TAG, "onConnectivityNetworkChange, MSG_WIFI_STATE_CONNECTED");
                    this.mHandler.sendEmptyMessage(3);
                    this.mConnectivityType = 800;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onConnectivityNetworkChange-Wifi:");
                    stringBuilder.append(this.mActiveNetworkInfo.getState());
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                }
            } else if (this.mActiveNetworkInfo.getType() == 0) {
                if (true == this.mActiveNetworkInfo.isConnected()) {
                    HwAPPQoEUtils.logD(TAG, "onConnectivityNetworkChange, MSG_CELL_STATE_CONNECTED");
                    this.mHandler.sendEmptyMessage(7);
                    this.mConnectivityType = 801;
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onConnectivityNetworkChange-Cell:");
                    stringBuilder.append(this.mActiveNetworkInfo.getState());
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                }
            }
        }
    }

    private void startWeakNetworkMonitor() {
        if (this.curAPPStateInfo != null && this.curAPPStateInfo.mAppId != -1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("system monitor curAPPStateInfo.mAppId = ");
            stringBuilder.append(this.curAPPStateInfo.mAppId);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            WifiInfo info = ((WifiManager) this.mContext.getSystemService("wifi")).getConnectionInfo();
            if (info != null) {
                int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
                if (rssiLevel <= 1 && !this.mHandler.hasMessages(109)) {
                    HwAPPQoEUtils.logD(TAG, "system monitor send weak network message");
                    this.mHandler.sendEmptyMessageDelayed(109, 4000);
                } else if (rssiLevel >= 2 && this.mHandler.hasMessages(109)) {
                    HwAPPQoEUtils.logD(TAG, "system monitor remove weak network message");
                    this.mHandler.removeMessages(109);
                }
            }
        }
    }

    private void stopWeakNetworkMonitor() {
        HwAPPQoEUtils.logD(TAG, "system monitor stopWeakNetworkMonitor");
        this.mHandler.removeMessages(109);
    }

    public boolean isDefaultApnType(String apnType) {
        return MemoryConstant.MEM_SCENE_DEFAULT.equals(apnType);
    }

    public boolean isSlotIdValid(int slotId) {
        return slotId >= 0 && 2 > slotId;
    }

    public boolean getMpLinkState() {
        return HwArbitrationFunction.isInMPLink(this.mContext, this.curAPPStateInfo.mAppUID);
    }
}

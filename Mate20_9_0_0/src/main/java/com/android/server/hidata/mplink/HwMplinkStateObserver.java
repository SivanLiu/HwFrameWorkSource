package com.android.server.hidata.mplink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.ServiceState;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMplinkStateObserver {
    private static final String TAG = "HiData_HwMplinkStateObserver";
    private IMpLinkStateObserverCallback mCallback;
    private Context mContext;

    private class StateBroadcastReceiver extends BroadcastReceiver {
        private StateBroadcastReceiver() {
        }

        /* synthetic */ StateBroadcastReceiver(HwMplinkStateObserver x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = HwMplinkStateObserver.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("action:");
            stringBuilder.append(action);
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
            if (action.equals("android.intent.action.SERVICE_STATE")) {
                HwMplinkStateObserver.this.mCallback.onTelephonyServiceStateChanged(ServiceState.newFromBundle(intent.getExtras()), intent.getIntExtra("subscription", -1));
            } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                HwMplinkStateObserver.this.mCallback.onTelephonyDefaultDataSubChanged(intent.getIntExtra("subscription", -1));
            } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                str = intent.getStringExtra("apnType");
                String str2 = HwMplinkStateObserver.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("apnType:");
                stringBuilder2.append(str);
                MpLinkCommonUtils.logD(str2, stringBuilder2.toString());
                if (MemoryConstant.MEM_SCENE_DEFAULT.equals(str)) {
                    HwMplinkStateObserver.this.mCallback.onTelephonyDataConnectionChanged(intent.getStringExtra("state"), intent.getStringExtra("iface"), intent.getIntExtra("subscription", -1));
                }
            } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                HwMplinkStateObserver.this.mCallback.onWifiNetworkStateChanged((NetworkInfo) intent.getParcelableExtra("networkInfo"));
            } else if (action.equals("mplink_intent_check_request_success")) {
                HwMplinkStateObserver.this.mCallback.onMpLinkRequestTimeout(intent.getIntExtra("mplink_intent_key_check_request", -1));
            }
        }
    }

    public HwMplinkStateObserver(Context context, IMpLinkStateObserverCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        registerMpLinkReceiver();
        registerMplinkSettingsChanges();
    }

    private void registerMpLinkReceiver() {
        IntentFilter filter = new IntentFilter();
        StateBroadcastReceiver receiver = new StateBroadcastReceiver(this, null);
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        filter.addAction("android.intent.action.ANY_DATA_STATE");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("mplink_intent_check_request_success");
        this.mContext.registerReceiver(receiver, filter);
    }

    private void registerMplinkSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("smart_network_switching"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                HwMplinkStateObserver.this.mCallback.onMplinkSwitchChange(HwMplinkStateObserver.this.getMpLinkSwitchState());
            }
        });
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("wifipro_network_vpn_state"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                HwMplinkStateObserver.this.mCallback.onVpnStateChange(HwMplinkStateObserver.this.getVpnConnectState());
            }
        });
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("mplink_simulate_hibrain_request_for_test"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                HwMplinkStateObserver.this.mCallback.onSimulateHiBrainRequestForDemo(MpLinkCommonUtils.getSettingsSystemBoolean(HwMplinkStateObserver.this.mContext.getContentResolver(), "mplink_simulate_hibrain_request_for_test", false));
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("mobile_data"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                HwMplinkStateObserver.this.mCallback.onMobileDataSwitchChange(MpLinkCommonUtils.getSettingsGlobalBoolean(HwMplinkStateObserver.this.mContext.getContentResolver(), "mobile_data", false));
            }
        });
    }

    public void initSimulateHibrain() {
        System.putInt(this.mContext.getContentResolver(), "mplink_simulate_hibrain_request_for_test", 0);
    }

    public boolean getMpLinkSwitchState() {
        return MpLinkCommonUtils.isMpLinkEnabled(this.mContext);
    }

    public boolean getVpnConnectState() {
        return MpLinkCommonUtils.getSettingsSystemBoolean(this.mContext.getContentResolver(), "wifipro_network_vpn_state", false);
    }
}

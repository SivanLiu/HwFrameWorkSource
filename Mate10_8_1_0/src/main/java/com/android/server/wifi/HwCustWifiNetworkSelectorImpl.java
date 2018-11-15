package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class HwCustWifiNetworkSelectorImpl extends HwCustWifiNetworkSelector {
    private static final String HOTSPOT_MATCH_LIST_VALUE = "hs20_match_list";
    private static final String TAG = "HwCustWifiNetworkSelector";
    private boolean mIsHs20Disabled = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                Log.w(HwCustWifiNetworkSelectorImpl.TAG, "context or intent is null,return");
            } else {
                HwCustWifiNetworkSelectorImpl.this.processBroadcast(context, intent);
            }
        }
    };

    public boolean isShouldRegisterBroadcast(Context context) {
        if (context == null || TextUtils.isEmpty(Global.getString(context.getContentResolver(), HOTSPOT_MATCH_LIST_VALUE))) {
            return false;
        }
        return true;
    }

    public boolean isShouldDisableHs20BySimState(Context context) {
        return this.mIsHs20Disabled;
    }

    public void registerSimCardStateReceiver(Context context) {
        if (context != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            context.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    private void processBroadcast(Context context, Intent intent) {
        if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
            Log.d(TAG, "receive broadcast intent, action:state " + intent.getStringExtra("ss"));
            if ("LOADED".equals(intent.getStringExtra("ss"))) {
                TelephonyManager telephonyManager = TelephonyManager.from(context);
                String mccMnc = "";
                if (telephonyManager != null) {
                    mccMnc = telephonyManager.getSimOperator();
                }
                String hs20MacthList = Global.getString(context.getContentResolver(), HOTSPOT_MATCH_LIST_VALUE);
                if (!TextUtils.isEmpty(hs20MacthList) && (TextUtils.isEmpty(mccMnc) ^ 1) != 0) {
                    boolean isMatch = false;
                    if (matchListLength > 0) {
                        for (String startsWith : hs20MacthList.trim().split(";")) {
                            if (startsWith.startsWith(mccMnc)) {
                                this.mIsHs20Disabled = false;
                                isMatch = true;
                                break;
                            }
                        }
                        if (!isMatch) {
                            this.mIsHs20Disabled = true;
                        }
                    }
                }
            } else if ("ABSENT".equals(intent.getStringExtra("ss"))) {
                this.mIsHs20Disabled = true;
            }
        }
    }
}

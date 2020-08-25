package com.android.server.hidata.mplink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.ServiceState;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkStateObserver {
    private static final int DEFAULT_VALUE = -1;
    private static final String HW_SIGNATURE_OR_SYSTEM = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final String TAG = "HiData_HwMpLinkStateObserver";
    /* access modifiers changed from: private */
    public IMpLinkStateObserverCallback mCallback;
    /* access modifiers changed from: private */
    public Context mContext;

    public HwMpLinkStateObserver(Context context, IMpLinkStateObserverCallback callback) {
        this.mContext = context;
        this.mCallback = callback;
        StateBroadcastReceiver receiver = new StateBroadcastReceiver();
        registerSystemBroadCastReceiver(receiver);
        registerMpLinkReceiver(receiver);
        registerMpLinkSettingsChanges();
    }

    private void registerSystemBroadCastReceiver(StateBroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_ANY_DATA_CONNECTION_STATE_CHANGED);
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED);
        this.mContext.registerReceiver(receiver, filter);
    }

    private void registerMpLinkReceiver(StateBroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("mplink_intent_check_request_success");
        this.mContext.registerReceiver(receiver, filter, HW_SIGNATURE_OR_SYSTEM, null);
    }

    private class StateBroadcastReceiver extends BroadcastReceiver {
        private StateBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                MpLinkCommonUtils.logI(HwMpLinkStateObserver.TAG, false, "action:%{public}s", new Object[]{action});
                if (action.equals("android.intent.action.SERVICE_STATE")) {
                    HwMpLinkStateObserver.this.mCallback.onTelephonyServiceStateChanged(ServiceState.newFromBundle(intent.getExtras()), intent.getIntExtra("subscription", -1));
                } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                    HwMpLinkStateObserver.this.mCallback.onTelephonyDefaultDataSubChanged(intent.getIntExtra("subscription", -1));
                } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                    String apnType = intent.getStringExtra("apnType");
                    MpLinkCommonUtils.logD(HwMpLinkStateObserver.TAG, false, "apnType:%{public}s", new Object[]{apnType});
                    if (MemoryConstant.MEM_SCENE_DEFAULT.equals(apnType)) {
                        HwMpLinkStateObserver.this.mCallback.onTelephonyDataConnectionChanged(intent.getStringExtra(SceneRecogFeature.DATA_STATE), intent.getStringExtra("iface"), intent.getIntExtra("subscription", -1));
                    }
                } else if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED)) {
                    HwMpLinkStateObserver.this.mCallback.onWifiNetworkStateChanged((NetworkInfo) intent.getParcelableExtra("networkInfo"));
                } else if (action.equals("mplink_intent_check_request_success")) {
                    HwMpLinkStateObserver.this.mCallback.onMpLinkRequestTimeout(intent.getIntExtra("mplink_intent_key_check_request", -1));
                }
            }
        }
    }

    private void registerMpLinkSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("smart_network_switching"), false, new ContentObserver(null) {
            /* class com.android.server.hidata.mplink.HwMpLinkStateObserver.AnonymousClass1 */

            public void onChange(boolean isChanged) {
                HwMpLinkStateObserver.this.mCallback.onMpLinkSwitchChange(HwMpLinkStateObserver.this.getMpLinkSwitchState());
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("wifipro_network_vpn_state"), false, new ContentObserver(null) {
            /* class com.android.server.hidata.mplink.HwMpLinkStateObserver.AnonymousClass2 */

            public void onChange(boolean isChanged) {
                HwMpLinkStateObserver.this.mCallback.onVpnStateChange(HwMpLinkStateObserver.this.getVpnConnectState());
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("mplink_simulate_hibrain_request_for_test"), false, new ContentObserver(null) {
            /* class com.android.server.hidata.mplink.HwMpLinkStateObserver.AnonymousClass3 */

            public void onChange(boolean isChanged) {
                HwMpLinkStateObserver.this.mCallback.onSimulateHiBrainRequestForDemo(MpLinkCommonUtils.getSettingsSystemBoolean(HwMpLinkStateObserver.this.mContext.getContentResolver(), "mplink_simulate_hibrain_request_for_test", false));
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, new ContentObserver(null) {
            /* class com.android.server.hidata.mplink.HwMpLinkStateObserver.AnonymousClass4 */

            public void onChange(boolean isChanged) {
                HwMpLinkStateObserver.this.mCallback.onMobileDataSwitchChange(MpLinkCommonUtils.getSettingsGlobalBoolean(HwMpLinkStateObserver.this.mContext.getContentResolver(), "mobile_data", false));
            }
        });
    }

    public void initSimulateHibrain() {
        Settings.System.putInt(this.mContext.getContentResolver(), "mplink_simulate_hibrain_request_for_test", 0);
    }

    public boolean getMpLinkSwitchState() {
        return MpLinkCommonUtils.isMpLinkEnabled(this.mContext);
    }

    public boolean getVpnConnectState() {
        return MpLinkCommonUtils.getSettingsSystemBoolean(this.mContext.getContentResolver(), "wifipro_network_vpn_state", false);
    }
}

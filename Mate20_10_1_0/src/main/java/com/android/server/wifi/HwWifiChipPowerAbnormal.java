package com.android.server.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.huawei.pgmng.log.LogPower;
import com.huawei.utils.reflect.EasyInvokeFactory;

class HwWifiChipPowerAbnormal {
    private static final String ACTION_CHECK_ABNORMAL = "huawei.intent.action.check_wifi_abnormal";
    private static final int BEACON_TIM_CMD_LENGTH = 4;
    private static final String BEACON_TIM_DATA = "BeaconTimData";
    private static final int CMD_GET_BEACON_TIM_CNT = 119;
    private static final int CMD_GET_DEVICE_FILTER_PKTS = 107;
    private static final int FILTER_CMD_LENGTH = 4;
    private static final String PACKET_FILTER_DATA = "PacketFilterData";
    private static final String[] PKG_FILTER_NAME = {"deviceFilterCnt", "arpFilterCnt", "apfFilterCnt", "icmpFilterCnt"};
    private static final String TAG = "HwWifiStateMachine_PAC";
    private static final boolean WIFI_CHIP_CHECK_ENABLE = SystemProperties.getBoolean("persist.sys.wifi_check_enable", true);
    private static final int WIFI_PKT_FILTER_ABNORMAL = 228;
    private static WifiStateMachineUtils wifiStateMachineUtils = EasyInvokeFactory.getInvokeUtils(WifiStateMachineUtils.class);
    private final long ALARM_INTERVAL = Long.parseLong(SystemProperties.get("persist.sys.wifi_check_internal", "240000"));
    private Context mContext;
    private HwWifiCHRService mHwWifiChrService;
    private PendingIntent mPendingIntent = null;
    private ClientModeImpl mWifiStateMachine = null;

    HwWifiChipPowerAbnormal(Context context, HwWifiCHRService hwWifiChrService, ClientModeImpl wifiStateMachine) {
        this.mContext = context;
        this.mHwWifiChrService = hwWifiChrService;
        this.mWifiStateMachine = wifiStateMachine;
        registerRecevier();
    }

    private void registerRecevier() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CHECK_ABNORMAL);
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            /* class com.android.server.wifi.HwWifiChipPowerAbnormal.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    Log.w(HwWifiChipPowerAbnormal.TAG, "intent is null.");
                    return;
                }
                String action = intent.getAction();
                if (action == null) {
                    Log.w(HwWifiChipPowerAbnormal.TAG, "action is null.");
                } else if (HwWifiChipPowerAbnormal.ACTION_CHECK_ABNORMAL.equals(action)) {
                    Log.i(HwWifiChipPowerAbnormal.TAG, "onReceive alarm: " + intent.getAction());
                    HwWifiChipPowerAbnormal.this.setRtcAlarm();
                    HwWifiChipPowerAbnormal.this.checkAbnormalPacket();
                }
            }
        }, UserHandle.ALL, filter, null, null);
    }

    private int[] getWifiChipData(String dataType) {
        int cmdStart;
        int cmdLength;
        byte[] buff = {0};
        if (PACKET_FILTER_DATA.equals(dataType)) {
            cmdLength = 4;
            cmdStart = 107;
        } else {
            cmdLength = 4;
            cmdStart = 119;
        }
        int[] wifiChipData = new int[cmdLength];
        for (int index = 0; index < cmdLength; index++) {
            wifiChipData[index] = WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.sendCmdToDriver(wifiStateMachineUtils.getInterfaceName(this.mWifiStateMachine), cmdStart + index, buff);
        }
        return wifiChipData;
    }

    private String formateData(int[] wifiChipData) {
        String result = "";
        if (wifiChipData == null || wifiChipData.length == 0) {
            return result;
        }
        for (int i = 0; i < wifiChipData.length; i++) {
            result = result + String.valueOf(wifiChipData[i]) + ",";
        }
        return result.substring(0, result.length() - 1);
    }

    private void reportApfFilterCount(int[] pktFilterCnt) {
        if (pktFilterCnt != null && pktFilterCnt.length == 4) {
            Bundle chrData = new Bundle();
            for (int i = 0; i < 4; i++) {
                chrData.putInt(PKG_FILTER_NAME[i], pktFilterCnt[i]);
            }
            HwWifiCHRService hwWifiCHRService = this.mHwWifiChrService;
            if (hwWifiCHRService != null) {
                hwWifiCHRService.uploadDFTEvent(12, chrData);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void enableCheckAlarm(boolean isEnable) {
        if (WIFI_CHIP_CHECK_ENABLE) {
            if (isEnable) {
                setRtcAlarm();
                checkAbnormalPacket();
                return;
            }
            cancelAlarm();
        }
    }

    /* access modifiers changed from: private */
    public void checkAbnormalPacket() {
        int[] pkgFilterCnt = getWifiChipData(PACKET_FILTER_DATA);
        reportApfFilterCount(pkgFilterCnt);
        int[] beaconTimCnt = getWifiChipData(BEACON_TIM_DATA);
        String pkgFilterData = formateData(pkgFilterCnt);
        String beaconTimData = formateData(beaconTimCnt);
        Log.i(TAG, "DeviceFilterCnt, ArpFilterCnt, ApfFilterCnt, IcmpFilterCnt: " + pkgFilterData);
        Log.i(TAG, "RealBeaconCnt, ExpectedBeaconCnt, ErrorTimSetCnt, TotalTimeSetCnt: " + beaconTimData);
        LogPower.push((int) WIFI_PKT_FILTER_ABNORMAL, "wifichip", "wifi_chip_data", pkgFilterData + "," + beaconTimData);
    }

    /* access modifiers changed from: private */
    public void setRtcAlarm() {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        if (this.mPendingIntent != null) {
            cancelAlarm();
        }
        Intent localIntent = new Intent(ACTION_CHECK_ABNORMAL);
        localIntent.setPackage("android");
        this.mPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, localIntent, 0);
        alarmManager.set(1, System.currentTimeMillis() + this.ALARM_INTERVAL, this.mPendingIntent);
    }

    private void cancelAlarm() {
        if (this.mPendingIntent != null) {
            ((AlarmManager) this.mContext.getSystemService("alarm")).cancel(this.mPendingIntent);
            this.mPendingIntent = null;
        }
    }
}

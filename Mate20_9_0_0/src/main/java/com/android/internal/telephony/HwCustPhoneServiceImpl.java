package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.Rlog;
import java.util.HashMap;

public class HwCustPhoneServiceImpl extends HwCustPhoneService {
    public static final String ACTION_PERMISSION = "com.huawei.phone.permission.DISABLE_2G_CONFIG";
    private static final int EVENT_SET_2G_SWITCH_DONE = 1;
    private static final String LOG_TAG = "HwCustPhoneServiceImpl";
    private static final int OFF_2G_SERVICE = 0;
    private static final int OFF_LTE_SERVICE = 0;
    private static final int ON_2G_SERVICE = 1;
    private static final int ON_LTE_SERVICE = 1;
    private static HashMap<Integer, Integer> mNetworkTypeWhen2gOffMapping = new HashMap();
    private Context mContext;
    private CustMainHandler mCustMainHandler;
    private HwPhone mHwPhone;
    private HwPhoneService mHwPhoneService;

    private class CustMainHandler extends Handler {
        public CustMainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Rlog.d(HwCustPhoneServiceImpl.LOG_TAG, "[PhoneIntfMgr] 2G-Switch EVENT_SET_2G_SWITCH_DONE");
                HwCustPhoneServiceImpl.this.handleSet2GSwitchDown(msg);
            }
        }
    }

    static {
        mNetworkTypeWhen2gOffMapping.put(Integer.valueOf(3), Integer.valueOf(2));
        mNetworkTypeWhen2gOffMapping.put(Integer.valueOf(9), Integer.valueOf(12));
    }

    public int getNetworkTypeBaseOnDisabled2G(int networkType) {
        int mappingNetworkType = getOffValueFromMapping(networkType);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch=getNetworkTypeBaseOnDisabled2G curPrefMode = ");
        stringBuilder.append(networkType);
        stringBuilder.append(" ,mappingNetworkType = ");
        stringBuilder.append(mappingNetworkType);
        Rlog.d(str, stringBuilder.toString());
        if (-1 != mappingNetworkType) {
            return mappingNetworkType;
        }
        return networkType;
    }

    public HwCustPhoneServiceImpl(HwPhoneService hwPhoneService, Looper looper) {
        this.mHwPhoneService = hwPhoneService;
        this.mCustMainHandler = new CustMainHandler(looper);
    }

    public void setPhone(HwPhone hwPhone, Context context) {
        this.mHwPhone = hwPhone;
        this.mContext = context;
        try {
            System.getInt(this.mContext.getContentResolver(), "disable_2g_visibility_key");
        } catch (SettingNotFoundException e) {
            Intent intent = new Intent("com.huawei.phone.ACTION_SET_VISIBILITY");
            intent.putExtra("disable_2g_visibility_key", 1);
            this.mContext.sendBroadcast(intent);
        }
        try {
            System.getInt(this.mContext.getContentResolver(), "disable_2g_default_key");
        } catch (SettingNotFoundException e2) {
            Intent intent2 = new Intent("com.huawei.phone.ACTION_SET_DEFAULT");
            intent2.putExtra("disable_2g_default_key", 0);
            this.mContext.sendBroadcast(intent2);
        }
        try {
            System.getInt(this.mContext.getContentResolver(), "disable_2g_key");
        } catch (SettingNotFoundException e3) {
            send2GServiceSwitchResult(false);
        }
    }

    public boolean isDisable2GServiceCapabilityEnabled() {
        return SystemProperties.getBoolean("ro.config.hw_disable_2g_cap", false);
    }

    public int getOffValueFromMapping(int curPrefMode) {
        if (mNetworkTypeWhen2gOffMapping.containsKey(Integer.valueOf(curPrefMode))) {
            return ((Integer) mNetworkTypeWhen2gOffMapping.get(Integer.valueOf(curPrefMode))).intValue();
        }
        return -1;
    }

    public int get2GServiceAbility() {
        int ability;
        int curPrefMode = getPreferredNetworkModeFromDb();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch= curPrefMode = ");
        stringBuilder.append(curPrefMode);
        Rlog.d(str, stringBuilder.toString());
        switch (curPrefMode) {
            case 0:
            case 1:
            case 3:
            case 4:
            case CSGNetworkList.RADIO_IF_UMTS /*5*/:
            case 7:
            case CSGNetworkList.RADIO_IF_LTE /*8*/:
            case CSGNetworkList.RADIO_IF_TDSCDMA /*9*/:
            case 10:
            case 16:
            case 17:
            case 18:
            case 20:
            case 21:
            case 22:
                ability = 1;
                break;
            default:
                ability = 0;
                break;
        }
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch= get2GServiceAbility() ability is ");
        stringBuilder.append(ability);
        Rlog.d(str, stringBuilder.toString());
        return ability;
    }

    public void set2GServiceAbility(int ability) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch= set2GServiceAbility: ability=");
        stringBuilder.append(ability);
        Rlog.d(str, stringBuilder.toString());
        int settingsNetworkMode = getPreferredNetworkModeFromDb();
        int networkMode = settingsNetworkMode;
        int lteServiceAbility = this.mHwPhoneService.getLteServiceAbility();
        if (ability == 0) {
            if (lteServiceAbility == 1) {
                networkMode = 12;
            } else {
                networkMode = 2;
            }
        } else if (ability == 1) {
            if (lteServiceAbility == 1) {
                networkMode = 9;
            } else {
                networkMode = 3;
            }
        }
        if (settingsNetworkMode != networkMode) {
            this.mHwPhone.setPreferredNetworkType(networkMode, this.mCustMainHandler.obtainMessage(1, Integer.valueOf(networkMode)));
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[PhoneIntfMgr] =2G-Switch= setPreferredNetworkType-> ");
            stringBuilder2.append(networkMode);
            Rlog.d(str2, stringBuilder2.toString());
        }
    }

    private void handleSet2GSwitchDown(Message msg) {
        Rlog.d(LOG_TAG, "[PhoneIntfMgr] =2G-Switch= in handleSet2GSwitchDown");
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            Rlog.e(LOG_TAG, "=2G-Switch= set prefer network mode failed!");
            send2GServiceSwitchResult(false);
            return;
        }
        int setPrefMode = ((Integer) ar.userObj).intValue();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch= set preferred network mode database to ");
        stringBuilder.append(setPrefMode);
        Rlog.d(str, stringBuilder.toString());
        int curPrefMode = getPreferredNetworkModeFromDb();
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[PhoneIntfMgr] =2G-Switch= curPrefMode = ");
        stringBuilder2.append(curPrefMode);
        Rlog.d(str2, stringBuilder2.toString());
        if (curPrefMode != setPrefMode) {
            savePreferredNetworkModeToDb(setPrefMode);
        }
        Rlog.d(LOG_TAG, "[PhoneIntfMgr] =2G-Switch= set prefer network mode success!");
        send2GServiceSwitchResult(true);
    }

    private void send2GServiceSwitchResult(boolean result) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] =2G-Switch= 2G service Switch result is ");
        stringBuilder.append(result);
        stringBuilder.append(". broadcast PREFERRED_2G_SWITCH_DONE");
        Rlog.d(str, stringBuilder.toString());
        if (this.mContext == null) {
            Rlog.e(LOG_TAG, "=2G-Switch= mContext is null. return!");
            return;
        }
        Intent intent = new Intent("com.huawei.telephony.PREF_2G_SWITCH_DONE");
        intent.putExtra("setting_result", result);
        this.mContext.sendBroadcast(intent);
    }

    private int getPreferredNetworkModeFromDb() {
        return Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode", Phone.PREFERRED_NT_MODE);
    }

    private void savePreferredNetworkModeToDb(int mode) {
        Global.putInt(this.mContext.getContentResolver(), "preferred_network_mode", mode);
    }
}

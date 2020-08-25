package com.android.server.intellicom.smartdualcard;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.HwTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.intellicom.common.HwAppStateObserver;
import com.android.server.intellicom.common.HwSettingsObserver;
import com.android.server.intellicom.common.NetLinkManager;
import com.android.server.intellicom.common.SmartDualCardUtil;
import java.util.Locale;

public class SmartDualCardRecommendNotify extends Handler {
    private static final int EVENT_APP_APPEARS_FOREGROUND = 0;
    private static final int EVENT_SWITCH_DUAL_CARD_SLOTS = 1;
    private static final String SETTINGS_PRIORITY_APP_NO_LONGER_PROMPT = "all_priority_app_no_longer_prompt";
    private static final String TAG = "SmartDualCardRecommend";
    private static PackageManager packageManager = null;
    private static Context sContext = null;
    private HwAppStateObserver mHwAppStateObserver = null;
    private HwSettingsObserver mHwSettingsObserver = null;

    public static SmartDualCardRecommendNotify getInstance() {
        return SingletonInstance.INSTANCE;
    }

    private static class SingletonInstance {
        /* access modifiers changed from: private */
        public static final SmartDualCardRecommendNotify INSTANCE = new SmartDualCardRecommendNotify();

        private SingletonInstance() {
        }
    }

    public void init(Context context) {
        sContext = context;
        packageManager = context.getPackageManager();
        this.mHwAppStateObserver = HwAppStateObserver.getInstance();
        this.mHwSettingsObserver = HwSettingsObserver.getInstance();
        this.mHwSettingsObserver.registerForSettingDbChange(4, this, 1, null);
        this.mHwAppStateObserver.registerForAppAppearsForeground(this, 0, null);
    }

    /* access modifiers changed from: private */
    public void goToIntelligentSwitchActivity() {
        Intent intent = new Intent();
        intent.setFlags(268435456);
        intent.setComponent(new ComponentName("com.huawei.dsdscardmanager", "com.huawei.dsdscardmanager.IntelligentSwitchActivity"));
        try {
            sContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "goToIntelligentSwitchActivity, ActivityNotFoundException");
        }
    }

    private String getSmartCardSwitchFirstPromtString(int uid, int lengthOfNoTrafficAppList) {
        String appName = getAppNameByUid(uid);
        if (TextUtils.isEmpty(appName)) {
            log("can not get appName");
            return "";
        }
        int slaveSlotId = SmartDualCardUtil.getSlaveCardSlotId() + 1;
        if (lengthOfNoTrafficAppList == 1) {
            String toastFormat = sContext.getResources().getString(33686208);
            return String.format(Locale.ROOT, toastFormat, appName, Integer.valueOf(slaveSlotId), Integer.valueOf(slaveSlotId));
        }
        String toastFormat2 = sContext.getResources().getQuantityString(34406413, lengthOfNoTrafficAppList - 1);
        return String.format(Locale.ROOT, toastFormat2, appName, Integer.valueOf(lengthOfNoTrafficAppList - 1), Integer.valueOf(slaveSlotId), Integer.valueOf(slaveSlotId));
    }

    /* access modifiers changed from: private */
    public void checkUserChoice(boolean isAllAppNoLongerNotify, boolean isDisableOne, int uid) {
        TelephonyManager telephonyManager = TelephonyManager.from(sContext);
        if (telephonyManager == null) {
            Log.e(TAG, "notifyWhenAppIsSlaveCardFreeTraffic: telephonyManager is null");
            return;
        }
        if (isDisableOne) {
            this.mHwSettingsObserver.setNotNotifyFlagForOnePriorityApp(HwTelephonyManager.getDefault().getSimSerialNumber(telephonyManager, SmartDualCardUtil.getSlaveCardSlotId()), uid);
        }
        if (isAllAppNoLongerNotify) {
            Settings.Global.putInt(sContext.getContentResolver(), SETTINGS_PRIORITY_APP_NO_LONGER_PROMPT, 1);
        }
    }

    private void showAlertDialog(final int uid, int lengthOfNoTrafficAppList) {
        if (lengthOfNoTrafficAppList <= 0) {
            log("lengthOfNoTrafficAppList less than 0");
            return;
        }
        String toastString = getSmartCardSwitchFirstPromtString(uid, lengthOfNoTrafficAppList);
        if (TextUtils.isEmpty(toastString)) {
            log("tosastString is empty");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(sContext, 33947691);
        View view = LayoutInflater.from(builder.getContext()).inflate(34013434, (ViewGroup) null);
        final CheckBox checkBox = (CheckBox) view.findViewById(34603056);
        ((TextView) view.findViewById(34603472)).setText(toastString);
        builder.setView(view);
        builder.setPositiveButton(33685733, new DialogInterface.OnClickListener() {
            /* class com.android.server.intellicom.smartdualcard.SmartDualCardRecommendNotify.AnonymousClass1 */

            public void onClick(DialogInterface dialoginterface, int i) {
                SmartDualCardRecommendNotify.this.checkUserChoice(checkBox.isChecked(), true, uid);
                SmartDualCardRecommendNotify.this.log("innerFreeTrafficAppRecommendation, setPositiveButton onClick");
            }
        });
        builder.setNegativeButton(33686207, new DialogInterface.OnClickListener() {
            /* class com.android.server.intellicom.smartdualcard.SmartDualCardRecommendNotify.AnonymousClass2 */

            public void onClick(DialogInterface dialogInterface, int i) {
                SmartDualCardRecommendNotify.this.checkUserChoice(checkBox.isChecked(), false, uid);
                SmartDualCardRecommendNotify.this.goToIntelligentSwitchActivity();
                SmartDualCardRecommendNotify.this.log("innerFreeTrafficAppRecommendation, setPositiveButton onClick");
            }
        });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
        dialog.setCancelable(false);
        dialog.show();
    }

    private String getAppNameByUid(int uid) {
        try {
            return (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageManager.getNameForUid(uid), 128));
        } catch (PackageManager.NameNotFoundException e) {
            log("NameNotFoundException");
            return "";
        }
    }

    private void notifyWhenAppIsSlaveCardFreeTraffic(Message msg) {
        TelephonyManager telephonyManager = TelephonyManager.from(sContext);
        if (telephonyManager == null) {
            Log.e(TAG, "notifyWhenAppIsSlaveCardFreeTraffic::telephonyManager is null");
            return;
        }
        String slaveIccid = HwTelephonyManager.getDefault().getSimSerialNumber(telephonyManager, SmartDualCardUtil.getSlaveCardSlotId());
        if (TextUtils.isEmpty(slaveIccid)) {
            log("slave imsi is empty");
            return;
        }
        int uid = ((Integer) ((AsyncResult) msg.obj).result).intValue();
        if (canShowAlertDialog(slaveIccid, uid)) {
            showAlertDialog(uid, this.mHwSettingsObserver.sizeOfFreeTrafficAppList(slaveIccid));
        }
    }

    private boolean isSlaveCardNetWorkGood() {
        TelephonyManager telephonyManager = TelephonyManager.from(sContext);
        if (telephonyManager == null) {
            Log.e(TAG, "notifyWhenAppIsSlaveCardFreeTraffic::telephonyManager is null");
            return false;
        }
        int netWorkClass = TelephonyManager.getNetworkClass(telephonyManager.getNetworkType(SmartDualCardUtil.getSlaveCardSubId()));
        if (netWorkClass == 3 || netWorkClass == 4) {
            return true;
        }
        log("netWorkClass is not 4G and 5G");
        return false;
    }

    private boolean canShowAlertDialog(String slaveIccid, int uid) {
        NetLinkManager netLinkManager = NetLinkManager.getInstance();
        if (netLinkManager.isIntelligenceCardOk() && netLinkManager.isDataConnectionOk() && Settings.Global.getInt(sContext.getContentResolver(), SETTINGS_PRIORITY_APP_NO_LONGER_PROMPT, 0) != 1 && this.mHwSettingsObserver.isNeedNotifyPriorityApp(slaveIccid, uid) && isSlaveCardNetWorkGood() && this.mHwSettingsObserver.isInFreeTrafficAppList(slaveIccid, uid) && netLinkManager.isRoamingAndVsimOk()) {
            return true;
        }
        return false;
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            log("handle msg is null");
        } else if (msg.what != 0) {
            log("handleMessage, can not deal this type, msg: " + msg);
        } else {
            notifyWhenAppIsSlaveCardFreeTraffic(msg);
        }
    }

    /* access modifiers changed from: private */
    public void log(String msg) {
        Log.i(TAG, msg);
    }
}

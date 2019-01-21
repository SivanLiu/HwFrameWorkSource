package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.StringNetworkSpecifier;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.ims.HwImsManagerInner;
import com.android.internal.telephony.AbstractSubscriptionController.OnDemandDdsLockNotifier;
import com.android.internal.telephony.AbstractSubscriptionController.SubscriptionControllerReference;
import com.android.internal.telephony.PhoneConstants.State;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import java.util.HashMap;
import java.util.List;

public class HwSubscriptionControllerReference implements SubscriptionControllerReference {
    public static final String ACTIONG_SET_USER_PREF_DATA_SLOTID_FAILED = "com.huawei.android.dualcard.ACTIONG_SET_USER_PREF_DATA_SLOTID_FAILED";
    private static final boolean DBG = true;
    private static final int EVENT_CHECK_SET_MAIN_SLOT = 2;
    private static final int EVENT_CHECK_SET_MAIN_SLOT_RETRY = 3;
    private static final int EVENT_SET_DEFAULT_DATA_DONE = 1;
    private static final String GMS_APP_NAME = "com.google.android.setupwizard";
    private static final int INT_INVALID_VALUE = -1;
    private static final String LOG_TAG = "HwSubscriptionControllerReference";
    private static final int SET_MAIN_SLOT_RETRY_INTERVAL = 5000;
    private static final int SET_MAIN_SLOT_RETRY_MAX_TIMES = 10;
    public static final String SET_PREF_DATA_SLOTID_FAILED_RECEIVER_PERMISSION = "com.huawei.permission.CARDS_SETTINGS";
    private static final boolean VDBG = false;
    private static SubscriptionControllerUtils subscriptionControllerUtils = new SubscriptionControllerUtils();
    private MainHandler mMainHandler;
    private HashMap<Integer, OnDemandDdsLockNotifier> mOnDemandDdsLockNotificationRegistrants = new HashMap();
    private int mSetMainSlotRetryTimes = 0;
    private SubscriptionController mSubscriptionController;

    private class DataConnectionHandler extends Handler {
        private DataConnectionHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                AsyncResult ar = msg.obj;
                HwSubscriptionControllerReference hwSubscriptionControllerReference = HwSubscriptionControllerReference.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_SET_DEFAULT_DATA_DONE subId:");
                stringBuilder.append((Integer) ar.result);
                hwSubscriptionControllerReference.logd(stringBuilder.toString());
                HwSubscriptionControllerReference.this.updateDataSubId(ar);
            }
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        public void handleMessage(Message msg) {
            HwSubscriptionControllerReference hwSubscriptionControllerReference = HwSubscriptionControllerReference.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage, msg,what = ");
            stringBuilder.append(msg.what);
            hwSubscriptionControllerReference.logd(stringBuilder.toString());
            switch (msg.what) {
                case 2:
                    AsyncResult ar = msg.obj;
                    if (ar == null || ar.exception != null) {
                        HwSubscriptionControllerReference.this.loge("EVENT_CHECK_SET_MAIN_SLOT fail, try again.");
                        HwSubscriptionControllerReference.this.tryToSwitchMainSlot(msg.arg1);
                        return;
                    }
                    if (HwSubscriptionManager.getInstance() != null) {
                        HwSubscriptionManager.getInstance().setUserPrefDataSlotId(msg.arg1);
                    } else {
                        HwSubscriptionControllerReference.this.loge("HwSubscriptionManager is null!!");
                    }
                    HwSubscriptionControllerReference.this.mSetMainSlotRetryTimes = 0;
                    return;
                case 3:
                    if (hasMessages(3)) {
                        removeMessages(3);
                    }
                    HwSubscriptionControllerReference.this.tryToSwitchMainSlot(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    public HwSubscriptionControllerReference(SubscriptionController subscriptionController) {
        this.mSubscriptionController = subscriptionController;
        this.mMainHandler = new MainHandler();
    }

    public int getHwSlotId(int subId) {
        if (subId == Integer.MAX_VALUE) {
            return this.mSubscriptionController.getDefaultSubId();
        }
        return subId;
    }

    public int[] getHwSubId(int slotIdx) {
        StringBuilder stringBuilder;
        if (slotIdx == Integer.MAX_VALUE) {
            slotIdx = this.mSubscriptionController.getSlotIndex(this.mSubscriptionController.getDefaultSubId());
            stringBuilder = new StringBuilder();
            stringBuilder.append("[getSubId] map default slotIdx=");
            stringBuilder.append(slotIdx);
            logd(stringBuilder.toString());
        }
        if (SubscriptionManager.isValidSlotIndex(slotIdx)) {
            return new int[]{slotIdx, slotIdx};
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("[getSubId]- invalid slotIdx=");
        stringBuilder.append(slotIdx);
        logd(stringBuilder.toString());
        return null;
    }

    public int getHwPhoneId(int subId) {
        if (subId != Integer.MAX_VALUE) {
            return subId;
        }
        subId = this.mSubscriptionController.getDefaultSubId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getPhoneId] asked for default subId=");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        return subId;
    }

    public boolean isSMSPromptEnabled() {
        boolean z = false;
        int value = 0;
        try {
            value = Global.getInt(this.mSubscriptionController.mContext.getContentResolver(), "multi_sim_sms_prompt");
        } catch (SettingNotFoundException e) {
            loge("Settings Exception Reading Dual Sim SMS Prompt Values");
        }
        if (value != 0) {
            z = true;
        }
        return z;
    }

    public void setSMSPromptEnabled(boolean enabled) {
        Global.putInt(this.mSubscriptionController.mContext.getContentResolver(), "multi_sim_sms_prompt", enabled);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSMSPromptOption to ");
        stringBuilder.append(enabled);
        logd(stringBuilder.toString());
    }

    public void activateSubId(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("activateSubId: subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        SubscriptionHelper.getInstance().setUiccSubscription(this.mSubscriptionController.getSlotIndex(subId), 1);
    }

    public void deactivateSubId(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deactivateSubId: subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        SubscriptionHelper.getInstance().setUiccSubscription(this.mSubscriptionController.getSlotIndex(subId), 0);
    }

    public void setNwMode(int subId, int nwMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setNwMode, nwMode: ");
        stringBuilder.append(nwMode);
        stringBuilder.append(" subId: ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        ContentValues value = new ContentValues(1);
        value.put("network_mode", Integer.valueOf(nwMode));
        ContentResolver contentResolver = this.mSubscriptionController.mContext.getContentResolver();
        Uri uri = SubscriptionManager.CONTENT_URI;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sim_id=");
        stringBuilder2.append(Integer.toString(subId));
        contentResolver.update(uri, value, stringBuilder2.toString(), null);
    }

    public int getNwMode(int subId) {
        SubscriptionInfo subInfo = this.mSubscriptionController.getActiveSubscriptionInfo(subId, this.mSubscriptionController.mContext.getOpPackageName());
        if (subInfo != null) {
            return subInfo.mNwMode;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNwMode: invalid subId = ");
        stringBuilder.append(subId);
        loge(stringBuilder.toString());
        return -1;
    }

    public int setSubState(int subId, int subStatus) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSubState, subStatus: ");
        stringBuilder.append(subStatus);
        stringBuilder.append(" subId: ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        ContentValues value = new ContentValues(1);
        value.put("sub_state", Integer.valueOf(subStatus));
        ContentResolver contentResolver = this.mSubscriptionController.mContext.getContentResolver();
        Uri uri = SubscriptionManager.CONTENT_URI;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sim_id=");
        stringBuilder2.append(Integer.toString(subId));
        int result = contentResolver.update(uri, value, stringBuilder2.toString(), null);
        this.mSubscriptionController.refreshCachedActiveSubscriptionInfoList();
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        intent.putExtra("_id", subId);
        intent.putExtra("subscription", subId);
        intent.putExtra("columnName", "sub_state");
        intent.putExtra("intContent", subStatus);
        intent.putExtra("stringContent", "None");
        this.mSubscriptionController.mContext.sendBroadcast(intent);
        this.mSubscriptionController.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
        this.mSubscriptionController.mContext.sendBroadcast(new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
        return result;
    }

    public int getSubState(int subId) {
        SubscriptionInfo subInfo = this.mSubscriptionController.getActiveSubscriptionInfo(subId, this.mSubscriptionController.mContext.getOpPackageName());
        if (subInfo == null || subInfo.getSimSlotIndex() < 0) {
            return 0;
        }
        return subInfo.mStatus;
    }

    public boolean isVoicePromptEnabled() {
        boolean z = false;
        int value = 0;
        try {
            value = Global.getInt(this.mSubscriptionController.mContext.getContentResolver(), "multi_sim_voice_prompt");
        } catch (SettingNotFoundException e) {
            loge("Settings Exception Reading Dual Sim Voice Prompt Values");
        }
        if (value != 0) {
            z = true;
        }
        return z;
    }

    public void setVoicePromptEnabled(boolean enabled) {
        Global.putInt(this.mSubscriptionController.mContext.getContentResolver(), "multi_sim_voice_prompt", enabled);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setVoicePromptOption to ");
        stringBuilder.append(enabled);
        logd(stringBuilder.toString());
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public int getSubIdFromNetworkRequest(NetworkRequest n) {
        if (n == null) {
            return this.mSubscriptionController.getDefaultDataSubId();
        }
        int subId;
        String str = null;
        NetworkSpecifier ns = n.networkCapabilities.getNetworkSpecifier();
        if (ns instanceof StringNetworkSpecifier) {
            str = ns.toString();
        } else {
            loge("NetworkSpecifier not instance of StringNetworkSpecifier, got subid failed!");
        }
        try {
            subId = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception e = ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            subId = this.mSubscriptionController.getDefaultDataSubId();
        }
        return subId;
    }

    public void startOnDemandDataSubscriptionRequest(NetworkRequest n) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startOnDemandDataSubscriptionRequest = ");
        stringBuilder.append(n);
        logd(stringBuilder.toString());
    }

    public void stopOnDemandDataSubscriptionRequest(NetworkRequest n) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopOnDemandDataSubscriptionRequest = ");
        stringBuilder.append(n);
        logd(stringBuilder.toString());
    }

    public int getCurrentDds() {
        return PhoneFactory.getTopPrioritySubscriptionId();
    }

    private void updateDataSubId(AsyncResult ar) {
        Integer subId = ar.result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" updateDataSubId,  subId=");
        stringBuilder.append(subId);
        stringBuilder.append(" exception ");
        stringBuilder.append(ar.exception);
        logd(stringBuilder.toString());
        if (ar.exception == null) {
            setDataSubId(subId.intValue());
        } else {
            HwTelephonyFactory.getHwDataServiceChrManager().sendIntentWhenSetDataSubFail(subId.intValue());
        }
        broadcastDefaultDataSubIdChanged(subId.intValue());
        subscriptionControllerUtils.updateAllDataConnectionTrackers(this.mSubscriptionController);
    }

    public boolean supportHwDualDataSwitch() {
        return true;
    }

    public void setDefaultDataSubIdHw(int subId) {
        if (NetworkFactory.isDualCellDataEnable()) {
            logd("Dual-Cell data is enabled so setDefaultDataSubId is return");
            this.mSubscriptionController.mContext.sendBroadcast(new Intent(ACTIONG_SET_USER_PREF_DATA_SLOTID_FAILED), SET_PREF_DATA_SLOTID_FAILED_RECEIVER_PERMISSION);
            return;
        }
        Intent intent = null;
        if (!HwModemCapability.isCapabilitySupport(0)) {
            Phone[] phones = PhoneFactory.getPhones();
            int length = phones.length;
            while (intent < length) {
                Phone phone = phones[intent];
                if (phone == null || phone.getState() == State.IDLE) {
                    intent++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[setDefaultDataSubId] phoneId:");
                    stringBuilder.append(phone.getPhoneId());
                    stringBuilder.append(" is calling, drop it and return");
                    logd(stringBuilder.toString());
                    this.mSubscriptionController.mContext.sendBroadcast(new Intent("com.android.huawei.DUAL_CARD_DATA_SUBSCRIPTION_CHANGE_FAILED"));
                    return;
                }
            }
        }
    }

    public void setDataSubId(int subId) {
        Global.putInt(this.mSubscriptionController.mContext.getContentResolver(), "multi_sim_data_call", subId);
    }

    public int getOnDemandDataSubId() {
        return getCurrentDds();
    }

    public void registerForOnDemandDdsLockNotification(int clientSubId, OnDemandDdsLockNotifier callback) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerForOnDemandDdsLockNotification for client=");
        stringBuilder.append(clientSubId);
        logd(stringBuilder.toString());
        this.mOnDemandDdsLockNotificationRegistrants.put(Integer.valueOf(clientSubId), callback);
    }

    public void notifyOnDemandDataSubIdChanged(NetworkRequest n) {
        OnDemandDdsLockNotifier notifier = (OnDemandDdsLockNotifier) this.mOnDemandDdsLockNotificationRegistrants.get(Integer.valueOf(getSubIdFromNetworkRequest(n)));
        if (notifier != null) {
            notifier.notifyOnDemandDdsLockGranted(n);
        } else {
            logd("No registrants for OnDemandDdsLockGranted event");
        }
    }

    private void broadcastDefaultDataSubIdChanged(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[broadcastDefaultDataSubIdChanged] subId=");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intent.addFlags(536870912);
        intent.putExtra("subscription", this.mSubscriptionController.getDefaultDataSubId());
        this.mSubscriptionController.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int updateClatForMobile(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            int phoneId = SubscriptionManager.getPhoneId(subId);
            if (this.mSubscriptionController != null) {
                try {
                    String mccMnc = TelephonyManager.from(this.mSubscriptionController.mContext).getSimOperatorNumericForPhone(phoneId);
                    String plmnsConfig = System.getString(this.mSubscriptionController.mContext.getContentResolver(), "disable_mobile_clatd");
                    if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(mccMnc)) {
                        logd("plmnsConfig is null, return ");
                        return 3;
                    } else if (plmnsConfig.contains(mccMnc)) {
                        logd("disable clatd!");
                        SystemProperties.set("gsm.net.doxlat", "false");
                        return 1;
                    } else {
                        SystemProperties.set("gsm.net.doxlat", "true");
                        return 2;
                    }
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception e = ");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                }
            }
        }
        return 4;
    }

    public void setSubscriptionPropertyIntoSettingsGlobal(int subId, String propKey, String propValue) {
        if (isImsPropKey(propKey)) {
            propKey = buildExtPropKey(subId, propKey);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setSubscriptionPropertyIntoSettingsGlobal] propKey=");
            stringBuilder.append(propKey);
            stringBuilder.append(",propValue=");
            stringBuilder.append(propValue);
            logd(stringBuilder.toString());
            Global.putInt(this.mSubscriptionController.mContext.getContentResolver(), propKey, Integer.parseInt(propValue));
        }
    }

    public String getSubscriptionPropertyFromSettingsGlobal(int subId, String propKey) {
        if (!isImsPropKey(propKey)) {
            return null;
        }
        int result = Global.getInt(this.mSubscriptionController.mContext.getContentResolver(), buildExtPropKey(subId, propKey), -1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getSubscriptionPropertyFromSettingsGlobal] getResult=");
        stringBuilder.append(result);
        logd(stringBuilder.toString());
        return Integer.toString(result);
    }

    public String buildExtPropKey(int subId, String propKey) {
        boolean isDualIms = HwImsManagerInner.isDualImsAvailable();
        StringBuilder sb = new StringBuilder(propKey);
        if (isDualIms) {
            sb.append("_");
            sb.append(subId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[buildPropKey] propKey=");
        stringBuilder.append(sb.toString());
        logd(stringBuilder.toString());
        return sb.toString();
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isImsPropKey(String propKey) {
        boolean z;
        switch (propKey.hashCode()) {
            case -1950380197:
                if (propKey.equals("volte_vt_enabled")) {
                    z = true;
                    break;
                }
            case -1218173306:
                if (propKey.equals("wfc_ims_enabled")) {
                    z = false;
                    break;
                }
            case -420099376:
                if (propKey.equals("vt_ims_enabled")) {
                    z = true;
                    break;
                }
            case 180938212:
                if (propKey.equals("wfc_ims_roaming_mode")) {
                    z = true;
                    break;
                }
            case 1334635646:
                if (propKey.equals("wfc_ims_mode")) {
                    z = true;
                    break;
                }
            case 1604840288:
                if (propKey.equals("wfc_ims_roaming_enabled")) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
            case true:
            case true:
            case true:
            case true:
            case true:
                return true;
            default:
                return false;
        }
    }

    private String getAppName(int pid) {
        String processName = "";
        List<RunningAppProcessInfo> l = ((ActivityManager) this.mSubscriptionController.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (l == null) {
            return processName;
        }
        for (RunningAppProcessInfo info : l) {
            if (info.pid == pid) {
                processName = info.processName;
                break;
            }
        }
        return processName;
    }

    private void tryToSwitchMainSlot(int subId) {
        if (subId == HwFullNetworkManager.getInstance().getUserSwitchDualCardSlots()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tryToSwitchMainSlot: subId(");
            stringBuilder.append(subId);
            stringBuilder.append(") is already main slot, return");
            logd(stringBuilder.toString());
            this.mSetMainSlotRetryTimes = 0;
            return;
        }
        boolean couldSwitch = (HwFullNetworkManager.getInstance().get4GSlotInProgress() || HwFullNetworkManager.getInstance().isRestartRildProgress()) ? false : true;
        if (this.mSetMainSlotRetryTimes < 10) {
            if (couldSwitch) {
                HwFullNetworkManager.getInstance().setMainSlot(subId, this.mMainHandler.obtainMessage(2, subId, -1));
            } else {
                this.mMainHandler.sendMessageDelayed(this.mMainHandler.obtainMessage(3, subId, -1), 5000);
            }
            this.mSetMainSlotRetryTimes++;
        } else {
            this.mSetMainSlotRetryTimes = 0;
        }
    }

    public void checkNeedSetMainSlotByPid(int subId, int pid) {
        String pkgName = getAppName(pid);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkNeedSetMainSlotByPid, subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(", pid = ");
        stringBuilder.append(pid);
        stringBuilder.append(", pkg = ");
        stringBuilder.append(pkgName);
        logd(stringBuilder.toString());
        if (GMS_APP_NAME.equals(pkgName)) {
            tryToSwitchMainSlot(subId);
        }
    }
}

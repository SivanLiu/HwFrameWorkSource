package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import java.util.ArrayList;

public class DeviceTelephonyPlugin extends DevicePolicyPlugin {
    private static final String CHANGE_PIN_CODE = "change_pin_code";
    private static final String DISABLE_AIR_PLANE_MODE = "disable_airplane_mode";
    private static final String DISABLE_DATA = "disable-data";
    private static final String DISABLE_PUSH = "disable-push";
    private static final String DISABLE_SUB = "disable-sub";
    private static final int DISABLE_SUB_ID = 1;
    private static final String DISABLE_SYNC = "disable-sync";
    private static final String INCOMING_SMS_EXCEPTION_PATTERN = "incoming_sms_exception_pattern";
    private static final String INCOMING_SMS_RESTRICTION_PATTERN = "incoming_sms_restriction_pattern";
    private static final String IS_OUTGOING = "isOutgoing";
    private static final String OUTGOING_SMS_EXCEPTION_PATTERN = "outgoing_sms_exception_pattern";
    private static final String OUTGOING_SMS_RESTRICTION_PATTERN = "outgoing_sms_restriction_pattern";
    private static final String PHONE_PACKAGE = "com.android.phone";
    private static final String REMOVE_TYPE = "removeType";
    private static final String SET_PIN_LOCK = "set_pin_lock";
    private static final int SUB0 = 0;
    private static final int SUB1 = 1;
    private static final String TAG = DeviceTelephonyPlugin.class.getSimpleName();
    private static final String TAG_ACTION_DISABLE_AIR_PLANE_MODE = "action_disable_airplane_mode";
    private static final String TAG_ACTION_DISABLE_DATA = "action_disable_data";
    private static final String TAG_ACTION_DISABLE_DATA_4G = "action_disable_data_4G";
    private static final String TAG_ACTION_DISABLE_SUB = "action_disable_sub";

    public DeviceTelephonyPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(DISABLE_SUB, PolicyType.STATE, new String[]{"value"});
        struct.addStruct(DISABLE_DATA, PolicyType.STATE, new String[]{"value"});
        struct.addStruct(DISABLE_AIR_PLANE_MODE, PolicyType.STATE, new String[]{"value"});
        struct.addStruct("disable-sync", PolicyType.STATE, new String[]{"value"});
        struct.addStruct(DISABLE_PUSH, PolicyType.STATE, new String[]{"value"});
        struct.addStruct(SET_PIN_LOCK, PolicyType.STATE, new String[]{"value"});
        struct.addStruct(CHANGE_PIN_CODE, PolicyType.STATE, new String[]{"value"});
        struct.addStruct("outgoing_day_limit", PolicyType.CONFIGURATION, new String[]{"day_mode", "limit_number_day", "day_mode_time"});
        struct.addStruct("outgoing_week_limit", PolicyType.CONFIGURATION, new String[]{"week_mode", "limit_number_week", "week_mode_time"});
        struct.addStruct("outgoing_month_limit", PolicyType.CONFIGURATION, new String[]{"month_mode", "limit_number_month", "month_mode_time"});
        struct.addStruct("incoming_day_limit", PolicyType.CONFIGURATION, new String[]{"day_mode", "limit_number_day", "day_mode_time"});
        struct.addStruct("incoming_week_limit", PolicyType.CONFIGURATION, new String[]{"week_mode", "limit_number_week", "week_mode_time"});
        struct.addStruct("incoming_month_limit", PolicyType.CONFIGURATION, new String[]{"month_mode", "limit_number_month", "month_mode_time"});
        struct.addStruct(INCOMING_SMS_EXCEPTION_PATTERN, PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(INCOMING_SMS_RESTRICTION_PATTERN, PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(OUTGOING_SMS_EXCEPTION_PATTERN, PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(OUTGOING_SMS_RESTRICTION_PATTERN, PolicyType.CONFIGURATION, new String[]{"value"});
        return struct;
    }

    public boolean onInit(PolicyStruct policyStruct) {
        HwLog.i(TAG, "onInit");
        if (policyStruct == null) {
            return false;
        }
        return true;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_TELEPHONY", "have no MDM_TELEPHONY permission!");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean changed) {
        HwLog.i(TAG, "onSetPolicy");
        boolean result = false;
        if (!checkCallingPermission(who, policyName)) {
            return false;
        }
        boolean z = true;
        boolean z2 = true;
        switch (policyName.hashCode()) {
            case -1888907677:
                if (policyName.equals("outgoing_week_limit")) {
                    z = true;
                    break;
                }
                break;
            case -1155809047:
                if (policyName.equals("outgoing_month_limit")) {
                    z = true;
                    break;
                }
                break;
            case -1032552593:
                if (policyName.equals(DISABLE_DATA)) {
                    z = true;
                    break;
                }
                break;
            case -1032175905:
                if (policyName.equals(DISABLE_PUSH)) {
                    z = true;
                    break;
                }
                break;
            case -1032082848:
                if (policyName.equals("disable-sync")) {
                    z = true;
                    break;
                }
                break;
            case -822171362:
                if (policyName.equals(INCOMING_SMS_RESTRICTION_PATTERN)) {
                    z = true;
                    break;
                }
                break;
            case -808925998:
                if (policyName.equals(SET_PIN_LOCK)) {
                    z = true;
                    break;
                }
                break;
            case 34406113:
                if (policyName.equals(INCOMING_SMS_EXCEPTION_PATTERN)) {
                    z = true;
                    break;
                }
                break;
            case 115663941:
                if (policyName.equals("outgoing_day_limit")) {
                    z = true;
                    break;
                }
                break;
            case 650237273:
                if (policyName.equals(DISABLE_AIR_PLANE_MODE)) {
                    z = true;
                    break;
                }
                break;
            case 890761828:
                if (policyName.equals(OUTGOING_SMS_RESTRICTION_PATTERN)) {
                    z = true;
                    break;
                }
                break;
            case 1195673727:
                if (policyName.equals("incoming_day_limit")) {
                    z = true;
                    break;
                }
                break;
            case 1352180187:
                if (policyName.equals(DISABLE_SUB)) {
                    z = false;
                    break;
                }
                break;
            case 1526624617:
                if (policyName.equals("incoming_week_limit")) {
                    z = true;
                    break;
                }
                break;
            case 1582555559:
                if (policyName.equals(OUTGOING_SMS_EXCEPTION_PATTERN)) {
                    z = true;
                    break;
                }
                break;
            case 1646476963:
                if (policyName.equals("incoming_month_limit")) {
                    z = true;
                    break;
                }
                break;
            case 1684123142:
                if (policyName.equals(CHANGE_PIN_CODE)) {
                    z = true;
                    break;
                }
                break;
        }
        String str;
        Intent intentDisableData4G;
        StringBuilder stringBuilder;
        String str2;
        int subId;
        String str3;
        switch (z) {
            case false:
                z = policyData.getBoolean("value");
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("disablesub: ");
                stringBuilder2.append(z);
                HwLog.i(str, stringBuilder2.toString());
                Intent intentDisableSub = new Intent(HwEmailMDMPlugin.DEVICE_POLICY_ACTION_POLICY_CHANGED);
                intentDisableSub.putExtra("action_tag", TAG_ACTION_DISABLE_SUB);
                intentDisableSub.putExtra("subId", 1);
                intentDisableSub.putExtra("subState", z);
                this.mContext.sendBroadcast(intentDisableSub);
                result = true;
                break;
            case true:
                z = policyData.getBoolean("value");
                intentDisableData4G = new Intent(HwEmailMDMPlugin.DEVICE_POLICY_ACTION_POLICY_CHANGED);
                intentDisableData4G.putExtra("action_tag", TAG_ACTION_DISABLE_DATA_4G);
                intentDisableData4G.putExtra("subId", 0);
                intentDisableData4G.putExtra("dataState", z);
                this.mContext.sendBroadcast(intentDisableData4G);
                result = true;
                break;
            case true:
                z = policyData.getBoolean("value");
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("disableAirPlane: ");
                stringBuilder.append(z);
                HwLog.i(str, stringBuilder.toString());
                if (Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0) {
                    z2 = false;
                }
                boolean isAirplaneModeOn = z2;
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isAirplaneModeOn: ");
                stringBuilder.append(isAirplaneModeOn);
                HwLog.i(str2, stringBuilder.toString());
                if (z && isAirplaneModeOn) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
                    long ident = Binder.clearCallingIdentity();
                    try {
                        connectivityManager.setAirplaneMode(false);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                intentDisableData4G = new Intent(HwEmailMDMPlugin.DEVICE_POLICY_ACTION_POLICY_CHANGED);
                intentDisableData4G.setPackage("com.android.settings");
                intentDisableData4G.putExtra("action_tag", TAG_ACTION_DISABLE_AIR_PLANE_MODE);
                intentDisableData4G.putExtra("airPlaneState", z);
                this.mContext.sendBroadcast(intentDisableData4G);
                result = true;
                break;
            case true:
            case true:
                result = true;
                break;
            case true:
                subId = policyData.getInt("slotId");
                str = policyData.getString("password");
                z2 = policyData.getBoolean("pinLockState");
                if (HwTelephonyManagerInner.getDefault() != null) {
                    str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setPinLock---- enablePinLock: ");
                    stringBuilder.append(z2);
                    stringBuilder.append("  subId: ");
                    stringBuilder.append(subId);
                    HwLog.i(str3, stringBuilder.toString());
                    result = HwTelephonyManagerInner.getDefault().setPinLockEnabled(z2, str, subId);
                    break;
                }
                break;
            case true:
                subId = policyData.getInt("slotId");
                str = policyData.getString("oldPinCode");
                str2 = policyData.getString("newPinCode");
                if (HwTelephonyManagerInner.getDefault() != null) {
                    str3 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("changePinLock----changPinId: ");
                    stringBuilder.append(subId);
                    HwLog.i(str3, stringBuilder.toString());
                    result = HwTelephonyManagerInner.getDefault().changeSimPinCode(str, str2, subId);
                    break;
                }
                break;
            case true:
            case true:
            case true:
            case true:
            case true:
            case true:
                result = true;
                break;
            case true:
            case true:
            case true:
            case true:
                result = true;
                break;
        }
        return result;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean changed) {
        boolean z;
        HwLog.i(TAG, "onRemovePolicy");
        Intent intentDisableSmsLimit = new Intent(HwEmailMDMPlugin.DEVICE_POLICY_ACTION_POLICY_CHANGED);
        intentDisableSmsLimit.setPackage(PHONE_PACKAGE);
        intentDisableSmsLimit.putExtra(REMOVE_TYPE, "remove_single_policy");
        switch (policyName.hashCode()) {
            case -1888907677:
                if (policyName.equals("outgoing_week_limit")) {
                    z = true;
                    break;
                }
            case -1155809047:
                if (policyName.equals("outgoing_month_limit")) {
                    z = true;
                    break;
                }
            case 115663941:
                if (policyName.equals("outgoing_day_limit")) {
                    z = false;
                    break;
                }
            case 1195673727:
                if (policyName.equals("incoming_day_limit")) {
                    z = true;
                    break;
                }
            case 1526624617:
                if (policyName.equals("incoming_week_limit")) {
                    z = true;
                    break;
                }
            case 1646476963:
                if (policyName.equals("incoming_month_limit")) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, true);
                intentDisableSmsLimit.putExtra("time_mode", "day_mode");
                break;
            case true:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, true);
                intentDisableSmsLimit.putExtra("time_mode", "week_mode");
                break;
            case true:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, true);
                intentDisableSmsLimit.putExtra("time_mode", "month_mode");
                break;
            case true:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, false);
                intentDisableSmsLimit.putExtra("time_mode", "day_mode");
                break;
            case true:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, false);
                intentDisableSmsLimit.putExtra("time_mode", "week_mode");
                break;
            case true:
                intentDisableSmsLimit.putExtra(IS_OUTGOING, false);
                intentDisableSmsLimit.putExtra("time_mode", "month_mode");
                break;
        }
        if (this.mContext != null) {
            this.mContext.sendBroadcast(intentDisableSmsLimit);
        }
        return true;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }

    public void onActiveAdminRemovedCompleted(ComponentName who, ArrayList<PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemovedCompleted");
        boolean isSMSLimitPolicy = false;
        if (removedPolicies == null) {
            HwLog.e(TAG, "removed policied list is null");
            return;
        }
        int removedPoliciesSize = removedPolicies.size();
        for (int i = 0; i < removedPoliciesSize; i++) {
            PolicyItem pi = (PolicyItem) removedPolicies.get(i);
            if (pi != null) {
                String policyName = pi.getPolicyName();
                if (pi.isGlobalPolicyChanged() && isSMSLimitPolicy(policyName)) {
                    isSMSLimitPolicy = true;
                }
            }
        }
        if (isSMSLimitPolicy) {
            Intent intentDisableSmsLimit = new Intent(HwEmailMDMPlugin.DEVICE_POLICY_ACTION_POLICY_CHANGED);
            intentDisableSmsLimit.setPackage(PHONE_PACKAGE);
            intentDisableSmsLimit.putExtra(REMOVE_TYPE, "remove_all_policy");
            if (this.mContext != null) {
                this.mContext.sendBroadcast(intentDisableSmsLimit);
            }
        }
    }

    private boolean isSMSLimitPolicy(String policyName) {
        return "outgoing_day_limit".equals(policyName) || "outgoing_week_limit".equals(policyName) || "outgoing_month_limit".equals(policyName) || "incoming_day_limit".equals(policyName) || "incoming_week_limit".equals(policyName) || "incoming_month_limit".equals(policyName);
    }
}

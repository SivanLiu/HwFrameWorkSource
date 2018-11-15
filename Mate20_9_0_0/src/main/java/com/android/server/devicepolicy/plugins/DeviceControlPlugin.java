package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.provider.Settings.System;
import android.util.Log;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import java.util.ArrayList;

public class DeviceControlPlugin extends DevicePolicyPlugin {
    private static final String DB_EYES_PROTECTION_MODE = "eyes_protection_mode";
    private static final int EYE_PROTECTION_CLOSE = 0;
    private static final int EYE_PROTECTION_ON = 1;
    public static final String POLICY_TURN_ON_EYE_COMFORT = "device_control_turn_on_eye_comfort";
    public static final String TAG = "DeviceControlPlugin";

    public DeviceControlPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(POLICY_TURN_ON_EYE_COMFORT, PolicyType.STATE, new String[]{"value"});
        return struct;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetPolicy: ");
        stringBuilder.append(policyName);
        Log.i(str, stringBuilder.toString());
        if (!checkCallingPermission(who, policyName) || policyData == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        boolean z = true;
        try {
            if (policyName.hashCode() == 1576234773 && policyName.equals(POLICY_TURN_ON_EYE_COMFORT)) {
                z = false;
            }
            if (!z) {
                onSetEyeComfotPolicy(policyData);
            }
            Binder.restoreCallingIdentity(identityToken);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void onSetEyeComfotPolicy(Bundle policyData) {
        System.putInt(this.mContext.getContentResolver(), "eyes_protection_mode", policyData.getBoolean("value", false));
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetPolicy: ");
        stringBuilder.append(policyName);
        Log.i(str, stringBuilder.toString());
        boolean z = false;
        if (!checkCallingPermission(who, policyName) || policyData == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        boolean z2 = true;
        try {
            if (policyName.hashCode() == 1576234773 && policyName.equals(POLICY_TURN_ON_EYE_COMFORT)) {
                z2 = false;
            }
            if (!z2) {
                if (System.getInt(this.mContext.getContentResolver(), "eyes_protection_mode", 0) != 0) {
                    z = true;
                }
                policyData.putBoolean("value", z);
            }
            Binder.restoreCallingIdentity(identityToken);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        HwLog.i(TAG, "onRemovePolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }
}

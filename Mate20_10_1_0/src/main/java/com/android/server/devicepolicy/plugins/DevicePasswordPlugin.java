package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.provider.Settings;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import java.util.ArrayList;
import java.util.Iterator;

public class DevicePasswordPlugin extends DevicePolicyPlugin {
    private static final String KEYGUARD_TYPE_UNKNOW = "-1";
    private static final String KEY_KEYGUARD_DISABLED = "keyguard_disabled";
    private static final String KEY_KEYGUARD_TYPE = "keyguard_type";
    private static final String POLICY_KEYGUARD_DISABLED = "policy_keyguard_disabled";
    private static final String PWD_CHANGE_EXTEND_TIME = "pwd-password-change-extendtime";
    private static final String PWD_NUM_SEQUENCE_MAX_LENGTH = "pwd-num-sequence-max-length";
    private static final String PWD_REPETITION_MAX_LENGTH = "pwd-repetition-max-length";
    public static final String TAG = DevicePasswordPlugin.class.getSimpleName();

    public DevicePasswordPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(PWD_NUM_SEQUENCE_MAX_LENGTH, PolicyStruct.PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(PWD_REPETITION_MAX_LENGTH, PolicyStruct.PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(PWD_CHANGE_EXTEND_TIME, PolicyStruct.PolicyType.CONFIGURATION, new String[]{"value"});
        struct.addStruct(POLICY_KEYGUARD_DISABLED, PolicyStruct.PolicyType.CONFIGURATION, new String[]{KEY_KEYGUARD_TYPE, KEY_KEYGUARD_DISABLED});
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
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_KEYGUARD", "does not have PERMISSION_MDM_KEYGUARD permission!");
        return true;
    }

    /* JADX INFO: finally extract failed */
    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isChanged) {
        HwLog.i(TAG, "onSetPolicy");
        boolean z = false;
        if (policyData == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            if (policyName.hashCode() != -581244632 || !policyName.equals(POLICY_KEYGUARD_DISABLED)) {
                z = true;
            }
            if (!z) {
                onSetKeyguardDisabled(policyData);
            }
            Binder.restoreCallingIdentity(identityToken);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
            throw th;
        }
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isChanged) {
        HwLog.i(TAG, "onRemovePolicy");
        return true;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        if (removedPolicies == null) {
            return true;
        }
        Iterator<PolicyStruct.PolicyItem> it = removedPolicies.iterator();
        while (it.hasNext()) {
            PolicyStruct.PolicyItem policy = it.next();
            if (policy != null) {
                String policyName = policy.getPolicyName();
                char c = 65535;
                if (policyName.hashCode() == -581244632 && policyName.equals(POLICY_KEYGUARD_DISABLED)) {
                    c = 0;
                }
                if (c == 0) {
                    Bundle policyData = new Bundle();
                    policyData.putString(KEY_KEYGUARD_TYPE, KEYGUARD_TYPE_UNKNOW);
                    policyData.putString(KEY_KEYGUARD_DISABLED, Boolean.FALSE.toString());
                    onSetKeyguardDisabled(policyData);
                }
            }
        }
        return true;
    }

    private void onSetKeyguardDisabled(Bundle policyData) {
        if (policyData == null) {
            HwLog.i(TAG, "onSetSlidKeyguardDisabled policyData is null");
            return;
        }
        String keyguardDisabled = policyData.getString(KEY_KEYGUARD_DISABLED, Boolean.FALSE.toString());
        String keyguardType = policyData.getString(KEY_KEYGUARD_TYPE, KEYGUARD_TYPE_UNKNOW);
        Settings.Secure.putString(this.mContext.getContentResolver(), KEY_KEYGUARD_DISABLED, keyguardDisabled);
        Settings.Secure.putString(this.mContext.getContentResolver(), KEY_KEYGUARD_TYPE, keyguardType);
    }
}

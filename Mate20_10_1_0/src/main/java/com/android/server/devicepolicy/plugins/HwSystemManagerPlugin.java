package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import java.util.ArrayList;
import java.util.Iterator;

public class HwSystemManagerPlugin extends DevicePolicyPlugin {
    private static final String POLICY_DISABLE_POWER_SAVER_MODE = "HwSystemManager-disable-powersavemode";
    private static final String POLICY_ENTERPRISE_WHITE_LIST = "enterprise-whitelist-hwsystemmanager";
    private static final String POLICY_SUPER_WHITELIST = "super-whitelist-hwsystemmanager";
    private static final String POLICY_VALUE = "value";
    private static final String SYSTEM_MANAGER_PERMISSION = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String SYSTEM_MANAGER_PERMISSION_MDM = "com.huawei.permission.sec.MDM_APP_MANAGEMENT";
    public static final String TAG = HwSystemManagerPlugin.class.getSimpleName();
    private static final String UPDATE_DATA_ENTERPRISE_LIST_ADD = "com.huawei.systemmanager.traffic.enterprisewhitelist.add";
    private static final String UPDATE_DATA_ENTERPRISE_LIST_REMOVE = "com.huawei.systemmanager.traffic.enterprisewhitelist.remove";
    private static final String UPDATE_DATA_SAVER_MODE = "com.huawei.systemmanager.netassistant.traffic.datasaver.update";
    private static final String UPDATE_DATA_SAVER_MODE_REMOVED = "com.huawei.systemmanager.netassistant.traffic.datasaver.update.remove";
    private static final String UPDATE_NOTIFICATION_SUPERWHITELIST = "com.huawei.notificationmanager.superwhitelist";
    private static final String UPDATE_NOTIFICATION_SUPERWHITELIST_REMOVE = "com.huawei.notificationmanager.superwhitelist.remove";
    private static final String UPDATE_POWER_SAVE_MODE_DISBALE = "com.huawei.systemmanager.powersavemode.disable";
    private Bundle policyBundle = null;

    public HwSystemManagerPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        PolicyStruct.PolicyType listType = PolicyStruct.PolicyType.LIST;
        PolicyStruct.PolicyType stateType = PolicyStruct.PolicyType.STATE;
        PolicyStruct.PolicyType enterproseListType = PolicyStruct.PolicyType.LIST;
        struct.addStruct("super-whitelist-hwsystemmanager/super-whitelist-hwsystemmanager-item", listType, new String[]{"value"});
        struct.addStruct("data-saver-mode", stateType, new String[]{"value"});
        struct.addStruct("enterprise-whitelist-hwsystemmanager/enterprise-whitelist-hwsystemmanager-item", enterproseListType, new String[]{"value"});
        struct.addStruct(POLICY_DISABLE_POWER_SAVER_MODE, stateType, new String[]{"value"});
        return struct;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission(SYSTEM_MANAGER_PERMISSION_MDM, "it does not have SYSTEM_MANAGER_PERMISSION_MDM permisson");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        this.policyBundle = policyData;
        return true;
    }

    public void onSetPolicyCompleted(ComponentName who, String policyName, boolean isChanged) {
        if (policyName != null && policyName.equals("data-saver-mode") && isChanged) {
            this.mContext.sendBroadcast(new Intent(UPDATE_DATA_SAVER_MODE), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "Broadcast TO HwSystemManager Data Saver");
        } else if (policyName != null && policyName.equals("super-whitelist-hwsystemmanager")) {
            Intent intent = new Intent(UPDATE_NOTIFICATION_SUPERWHITELIST);
            Bundle bundle = this.policyBundle;
            if (bundle != null) {
                intent.putExtras(bundle);
            }
            this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(Binder.getCallingUid()), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "Broadcast TO HwSystemManager");
        } else if (policyName != null && policyName.equals(POLICY_ENTERPRISE_WHITE_LIST) && isChanged) {
            this.mContext.sendBroadcast(new Intent(UPDATE_DATA_ENTERPRISE_LIST_ADD), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "Broadcast TO HwSystemManager");
        } else if (!isPowerSaveModeDisablePolicy(policyName) || !isChanged) {
            HwLog.i(TAG, "onSetPolicyCompleted not handle");
        } else {
            Intent intent2 = new Intent(UPDATE_POWER_SAVE_MODE_DISBALE);
            Bundle bundle2 = this.policyBundle;
            if (bundle2 != null) {
                intent2.putExtras(bundle2);
            }
            this.mContext.sendBroadcast(intent2, "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "Broadcast disable-powersavemode policy to HwSystemManager");
        }
    }

    private boolean isPowerSaveModeDisablePolicy(String policyName) {
        return POLICY_DISABLE_POWER_SAVER_MODE.equals(policyName);
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isGlobalPolicyChanged) {
        if (policyName != null && policyName.equals("data-saver-mode")) {
            this.mContext.sendBroadcast(new Intent(UPDATE_DATA_SAVER_MODE_REMOVED), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "onRemove-Broadcast TO HwSystemManager Data Saver");
            return true;
        } else if (policyName != null && policyName.equals("super-whitelist-hwsystemmanager")) {
            Intent intent = new Intent(UPDATE_NOTIFICATION_SUPERWHITELIST_REMOVE);
            intent.putExtras(policyData);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(Binder.getCallingUid()), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "onRemove-Broadcast TO HwSystemManager");
            return true;
        } else if (policyName != null && policyName.equals(POLICY_ENTERPRISE_WHITE_LIST)) {
            this.mContext.sendBroadcast(new Intent(UPDATE_DATA_ENTERPRISE_LIST_REMOVE), "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
            HwLog.i(TAG, "Broadcast TO HwSystemManager");
            return true;
        } else if (isPowerSaveModeDisablePolicy(policyName)) {
            notifyDevicePolicyChange(UPDATE_POWER_SAVE_MODE_DISBALE, null);
            return true;
        } else {
            HwLog.i(TAG, "onRemovePolicy not handle");
            return true;
        }
    }

    private void notifyDevicePolicyChange(String action, Bundle bundle) {
        Intent intent = new Intent(action);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        this.mContext.sendBroadcast(intent, "com.huawei.systemmanager.permission.ACCESS_INTERFACE");
        String str = TAG;
        HwLog.i(str, "Broadcast " + action + " TO HwSystemManager");
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> removedPolicies) {
        Iterator<PolicyStruct.PolicyItem> it = removedPolicies.iterator();
        while (it.hasNext()) {
            String policyName = it.next().getPolicyName();
            if (policyName != null) {
                char c = 65535;
                int hashCode = policyName.hashCode();
                if (hashCode != -1346523966) {
                    if (hashCode != 403749117) {
                        if (hashCode == 731752599 && policyName.equals("super-whitelist-hwsystemmanager")) {
                            c = 1;
                        }
                    } else if (policyName.equals(POLICY_ENTERPRISE_WHITE_LIST)) {
                        c = 0;
                    }
                } else if (policyName.equals(POLICY_DISABLE_POWER_SAVER_MODE)) {
                    c = 2;
                }
                if (c == 0) {
                    notifyDevicePolicyChange(UPDATE_DATA_ENTERPRISE_LIST_REMOVE, null);
                } else if (c == 1) {
                    notifyDevicePolicyChange(UPDATE_NOTIFICATION_SUPERWHITELIST_REMOVE, null);
                } else if (c == 2) {
                    notifyDevicePolicyChange(UPDATE_POWER_SAVE_MODE_DISBALE, null);
                }
            }
        }
        return true;
    }
}

package com.android.server.devicepolicy.plugins;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwDevicePolicyManagerServiceUtil;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import java.util.ArrayList;
import java.util.Collections;

public class DevicePackageManagerPlugin extends DevicePolicyPlugin {
    private static final String ALLOW_NOTIFY_DB_KEY = "allow_notification_white_apps";
    private static final String BROADCAST_PKG_NAME = "com.huawei.systemmanager";
    private static final String NOTIFICATION_INTENT_ACTION = "com.huawei.devicepolicy.action.POLICY_CHANGED";
    private static final String NOTIFY_APP_WHITE = "notification-app-bt-white-list";
    private static final String POLICY_NAME = "policy_name";
    private static final String POLICY_UPDATE_NFN_WHITE_LIST_DATA = "com.huawei.systemmanager.update_notification_white_list_data";
    public static final String TAG = DevicePackageManagerPlugin.class.getSimpleName();

    public DevicePackageManagerPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(NOTIFY_APP_WHITE, PolicyType.LIST, new String[]{"value"});
        return struct;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        HwLog.i(TAG, "checkCallingPermission");
        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        HwLog.i(TAG, "onSetPolicy");
        if (isNotEffective(who, policyName, policyData, effective)) {
            return false;
        }
        boolean z = true;
        if (policyName.hashCode() == 496906511 && policyName.equals(NOTIFY_APP_WHITE)) {
            z = false;
        }
        if (z) {
            return true;
        }
        return setNotificationDataToDb(policyData);
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        HwLog.i(TAG, "onRemovePolicy");
        if (isNotEffective(who, policyName, policyData, effective)) {
            return false;
        }
        boolean z = true;
        if (policyName.hashCode() == 496906511 && policyName.equals(NOTIFY_APP_WHITE)) {
            z = false;
        }
        if (z) {
            return true;
        }
        z = removeNotificationDataFromDb(policyData);
        if (z) {
            sendBroadCastForHwSystemManager();
        }
        return z;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        if (isNotEffective(who, policyName, policyData, true)) {
            return false;
        }
        boolean z = true;
        if (policyName.hashCode() == 496906511 && policyName.equals(NOTIFY_APP_WHITE)) {
            z = false;
        }
        if (z) {
            return true;
        }
        return getNotificationData(policyData);
    }

    public void onSetPolicyCompleted(ComponentName who, String policyName, boolean changed) {
        HwLog.i(TAG, "onSetPolicyCompleted");
        if (policyName != null && policyName.equals(NOTIFY_APP_WHITE) && changed) {
            sendBroadCastForHwSystemManager();
        }
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        if (removedPolicies != null && removedPolicies.size() > 0) {
            int k = removedPolicies.size();
            for (int j = 0; j < k; j++) {
                if (((PolicyItem) removedPolicies.get(j)).getPolicyName().equals(NOTIFY_APP_WHITE)) {
                    cleanAllowNotificationDb();
                    sendBroadCastForHwSystemManager();
                }
            }
        }
        return true;
    }

    private void cleanAllowNotificationDb() {
        Global.putStringForUser(this.mContext.getContentResolver(), ALLOW_NOTIFY_DB_KEY, "", 0);
    }

    public boolean isNotEffective(ComponentName who, String policyName, Bundle policyData, boolean effective) {
        if (policyData != null && NOTIFY_APP_WHITE.equals(policyName)) {
            ArrayList<String> packageNames = policyData.getStringArrayList("value");
            if (!HwDevicePolicyManagerServiceUtil.isValidatePackageNames(packageNames)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("packageName:");
                stringBuilder.append(packageNames);
                stringBuilder.append(" is invalid.");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return false;
    }

    private boolean removeNotificationDataFromDb(Bundle bundle) {
        ArrayList<String> listData = bundle.getStringArrayList("value");
        if (listData == null || listData.isEmpty()) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        boolean result = false;
        try {
            ArrayList<String> notifnlistData = getNotificationDataFromDb();
            StringBuffer buffer = new StringBuffer();
            int size = listData.size();
            for (int i = 0; i < size; i++) {
                String notifyData = (String) listData.get(i);
                if (notifnlistData.contains(notifyData)) {
                    notifnlistData.remove(notifyData);
                } else {
                    String str = new StringBuilder();
                    str.append(notifyData);
                    str.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    buffer.append(str.toString());
                }
            }
            boolean result2 = Global.putStringForUser(this.mContext.getContentResolver(), ALLOW_NOTIFY_DB_KEY, formatNotificationData(notifnlistData, buffer.toString()), 0);
            return result2;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private boolean getNotificationData(Bundle policyData) {
        if (policyData == null) {
            policyData = new Bundle();
        }
        policyData.putStringArrayList("value", getNotificationDataFromDb());
        return true;
    }

    private ArrayList<String> getNotificationDataFromDb() {
        String notificationWhiteApps = Global.getStringForUser(this.mContext.getContentResolver(), ALLOW_NOTIFY_DB_KEY, 0);
        if (TextUtils.isEmpty(notificationWhiteApps)) {
            return null;
        }
        ArrayList<String> list = new ArrayList();
        String[] notificationWhiteAppsArray = notificationWhiteApps.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        if (notificationWhiteAppsArray.length > 0) {
            Collections.addAll(list, notificationWhiteAppsArray);
        }
        return list;
    }

    private boolean setNotificationDataToDb(Bundle bundle) {
        ArrayList<String> list = bundle.getStringArrayList("value");
        boolean result = false;
        if (list == null || list.isEmpty()) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        try {
            result = Global.putStringForUser(this.mContext.getContentResolver(), ALLOW_NOTIFY_DB_KEY, formatNotificationData(list, Global.getStringForUser(this.mContext.getContentResolver(), ALLOW_NOTIFY_DB_KEY, 0)), 0);
            return result;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private String formatNotificationData(ArrayList<String> list, String notificationDatas) {
        StringBuffer buffer = TextUtils.isEmpty(notificationDatas) ? new StringBuffer() : new StringBuffer(notificationDatas);
        int size = list.size();
        for (int i = 0; i < size; i++) {
            String str = new StringBuilder();
            str.append((String) list.get(i));
            str.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            buffer.append(str.toString());
        }
        return buffer.toString();
    }

    private void sendBroadCastForHwSystemManager() {
        HwLog.i(TAG, "sendBroadCastForHwSystemManager");
        long callingId = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("com.huawei.devicepolicy.action.POLICY_CHANGED");
            intent.setPackage("com.huawei.systemmanager");
            intent.putExtra(POLICY_NAME, POLICY_UPDATE_NFN_WHITE_LIST_DATA);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(ActivityManager.getCurrentUser()));
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }
}

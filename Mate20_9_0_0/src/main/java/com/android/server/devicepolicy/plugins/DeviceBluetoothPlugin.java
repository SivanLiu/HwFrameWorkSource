package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.android.server.HwBluetoothBigDataService;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.PolicyStruct.PolicyItem;
import com.android.server.devicepolicy.PolicyStruct.PolicyType;
import java.util.ArrayList;

public class DeviceBluetoothPlugin extends DevicePolicyPlugin {
    private static final String ACTION_DEVICE_POLICY_CHANGED = "com.huawei.devicepolicy.action.POLICY_CHANGED";
    private static final String BLUETOOTH_BLACK_LIST = "bt-black-list";
    private static final String BLUETOOTH_BLACK_LIST_ITEM = "bt-black-list/bt-black-list-item";
    private static final String BLUETOOTH_CONTROL_PERMSSION = "com.huawei.permission.sec.MDM_BLUETOOTH";
    private static final String BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME = "disabled";
    private static final String BLUETOOTH_DISCOVER_EXTRA_DATA = "discover";
    private static final String BLUETOOTH_FILE_TRANSFER_STATE = "bt-file-transfer-state";
    private static final String BLUETOOTH_LIMITED_EXTRA_DATA = "limite";
    private static final String BLUETOOTH_OUTGOING_CALL_STATE = "bt-outgoing-call-state";
    private static final String BLUETOOTH_PAIRING_STATE = "bt-pairing-state";
    private static final String BLUETOOTH_POLICY_DISCOVERABLE = "bt-discover";
    private static final String BLUETOOTH_POLICY_EXTRA_DATA = "policydata";
    private static final String BLUETOOTH_POLICY_EXTRA_NAME = "policyname";
    private static final String BLUETOOTH_POLICY_EXTRA_TYPE = "type";
    private static final String BLUETOOTH_POLICY_LIMITED_DISCOVERABLE = "bt-limit-discover";
    private static final String BLUETOOTH_SECURE_PROFILE = "bt-secure-profiles";
    private static final String BLUETOOTH_SECURE_PROFILE_ITEM = "bt-secure-profiles/bt-secure-profiles-item";
    private static final String BLUETOOTH_WHITE_LIST = "bt-white-list";
    private static final String BLUETOOTH_WHITE_LIST_ITEM = "bt-white-list/bt-white-list-item";
    public static final String TAG = DeviceBluetoothPlugin.class.getSimpleName();
    private static int TYPE_REMOVE = 1;
    private static int TYPE_SET = 0;

    public DeviceBluetoothPlugin(Context context) {
        super(context);
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct(BLUETOOTH_BLACK_LIST, PolicyType.LIST, new String[0]);
        struct.addStruct(BLUETOOTH_BLACK_LIST_ITEM, PolicyType.LIST, new String[]{"value"});
        struct.addStruct(BLUETOOTH_WHITE_LIST, PolicyType.LIST, new String[0]);
        struct.addStruct(BLUETOOTH_WHITE_LIST_ITEM, PolicyType.LIST, new String[]{"value"});
        struct.addStruct(BLUETOOTH_PAIRING_STATE, PolicyType.STATE, new String[]{BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME});
        struct.addStruct(BLUETOOTH_FILE_TRANSFER_STATE, PolicyType.STATE, new String[]{BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME});
        struct.addStruct(BLUETOOTH_OUTGOING_CALL_STATE, PolicyType.STATE, new String[]{BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME});
        struct.addStruct(BLUETOOTH_SECURE_PROFILE, PolicyType.LIST, new String[0]);
        struct.addStruct(BLUETOOTH_SECURE_PROFILE_ITEM, PolicyType.LIST, new String[]{BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME});
        struct.addStruct(BLUETOOTH_POLICY_DISCOVERABLE, PolicyType.STATE, new String[]{BLUETOOTH_DISCOVER_EXTRA_DATA});
        struct.addStruct(BLUETOOTH_POLICY_LIMITED_DISCOVERABLE, PolicyType.STATE, new String[]{BLUETOOTH_LIMITED_EXTRA_DATA});
        return struct;
    }

    public boolean checkCallingPermission(ComponentName who, String policyName) {
        this.mContext.enforceCallingOrSelfPermission(BLUETOOTH_CONTROL_PERMSSION, "NEED BLUETOOTH PERMISSION.");
        return true;
    }

    /* JADX WARNING: Missing block: B:28:0x0077, code:
            if (r6.equals(BLUETOOTH_BLACK_LIST) != false) goto L_0x007b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean changed) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetPolicy policyName : ");
        stringBuilder.append(policyName);
        HwLog.i(str, stringBuilder.toString());
        boolean z = false;
        if (policyName == null) {
            HwLog.e(TAG, "params null.");
            return false;
        }
        switch (policyName.hashCode()) {
            case -1837809145:
                break;
            case -1224274072:
                if (policyName.equals(BLUETOOTH_OUTGOING_CALL_STATE)) {
                    z = true;
                    break;
                }
            case -786191612:
                if (policyName.equals(BLUETOOTH_POLICY_DISCOVERABLE)) {
                    z = true;
                    break;
                }
            case -231917807:
                if (policyName.equals(BLUETOOTH_PAIRING_STATE)) {
                    z = true;
                    break;
                }
            case 58724381:
                if (policyName.equals(BLUETOOTH_WHITE_LIST)) {
                    z = true;
                    break;
                }
            case 705160342:
                if (policyName.equals(BLUETOOTH_POLICY_LIMITED_DISCOVERABLE)) {
                    z = true;
                    break;
                }
            case 830337541:
                if (policyName.equals(BLUETOOTH_SECURE_PROFILE)) {
                    z = true;
                    break;
                }
            case 1892983461:
                if (policyName.equals(BLUETOOTH_FILE_TRANSFER_STATE)) {
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
                if (policyData != null) {
                    notifyBluetoohListPolicyChange(TYPE_SET, policyData.getStringArrayList("value"), policyName);
                    break;
                }
                break;
            case true:
            case true:
            case true:
                if (policyData != null && changed) {
                    notifyBluetoothPolicyStateChange(policyName, policyData);
                    break;
                }
            case true:
                if (policyData != null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("BLUETOOTH_SECURE_PROFILE GET Int : ");
                    stringBuilder.append(policyData.getStringArrayList(BLUETOOTH_DISABLE_PROFILE_STRUCT_NAME));
                    HwLog.i(str, stringBuilder.toString());
                    notifyBluetoothSecureProfileChange(policyData);
                    break;
                }
                break;
            case true:
            case true:
                if (policyData != null && changed) {
                    notifyBluetoothDiscoverChange(policyName);
                    break;
                }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:28:0x0077, code:
            if (r6.equals(BLUETOOTH_BLACK_LIST) != false) goto L_0x007b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean changed) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRemovePolicy policyName : ");
        stringBuilder.append(policyName);
        HwLog.i(str, stringBuilder.toString());
        boolean z = false;
        if (policyName == null) {
            HwLog.e(TAG, "params null.");
            return false;
        }
        switch (policyName.hashCode()) {
            case -1837809145:
                break;
            case -1224274072:
                if (policyName.equals(BLUETOOTH_OUTGOING_CALL_STATE)) {
                    z = true;
                    break;
                }
            case -786191612:
                if (policyName.equals(BLUETOOTH_POLICY_DISCOVERABLE)) {
                    z = true;
                    break;
                }
            case -231917807:
                if (policyName.equals(BLUETOOTH_PAIRING_STATE)) {
                    z = true;
                    break;
                }
            case 58724381:
                if (policyName.equals(BLUETOOTH_WHITE_LIST)) {
                    z = true;
                    break;
                }
            case 705160342:
                if (policyName.equals(BLUETOOTH_POLICY_LIMITED_DISCOVERABLE)) {
                    z = true;
                    break;
                }
            case 830337541:
                if (policyName.equals(BLUETOOTH_SECURE_PROFILE)) {
                    z = true;
                    break;
                }
            case 1892983461:
                if (policyName.equals(BLUETOOTH_FILE_TRANSFER_STATE)) {
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
                if (policyData != null) {
                    notifyBluetoohListPolicyChange(TYPE_REMOVE, policyData.getStringArrayList("value"), policyName);
                    break;
                }
                break;
            case true:
            case true:
            case true:
                if (policyData != null && changed) {
                    notifyBluetoothPolicyStateChange(policyName, policyData);
                    break;
                }
            case true:
                if (policyData != null) {
                    notifyBluetoothSecureProfileChange(policyData);
                    break;
                }
                break;
            case true:
            case true:
                if (policyData != null && changed) {
                    notifyBluetoothDiscoverChange(policyName);
                    break;
                }
        }
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        if (removedPolicies != null && removedPolicies.size() > 0) {
            int k = removedPolicies.size();
            for (int j = 0; j < k; j++) {
                PolicyItem item = (PolicyItem) removedPolicies.get(j);
                String policyName = item.getPolicyName();
                if (policyName.equals(BLUETOOTH_BLACK_LIST) || policyName.equals(BLUETOOTH_WHITE_LIST)) {
                    int n = item.getChildItem().size();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("name: ");
                    stringBuilder.append(policyName);
                    stringBuilder.append(" childItem size ");
                    stringBuilder.append(n);
                    HwLog.i(str, stringBuilder.toString());
                    for (int i = 0; i < n; i++) {
                        notifyBluetoohListPolicyChange(TYPE_REMOVE, ((PolicyItem) item.getChildItem().get(i)).getAttributes().getStringArrayList("value"), policyName);
                    }
                } else if (policyName.equals(BLUETOOTH_PAIRING_STATE) || policyName.equals(BLUETOOTH_FILE_TRANSFER_STATE) || policyName.equals(BLUETOOTH_OUTGOING_CALL_STATE)) {
                    if (item.isGlobalPolicyChanged()) {
                        notifyBluetoothPolicyStateChange(policyName, item.getAttributes());
                    }
                } else if (policyName.equals(BLUETOOTH_SECURE_PROFILE)) {
                    notifyBluetoothSecureProfileChange(item.getAttributes());
                } else if ((policyName.equals(BLUETOOTH_POLICY_DISCOVERABLE) || policyName.equals(BLUETOOTH_POLICY_LIMITED_DISCOVERABLE)) && item.isGlobalPolicyChanged()) {
                    notifyBluetoothDiscoverChange(policyName);
                }
            }
        }
        return true;
    }

    private void notifyBluetoohListPolicyChange(int type, ArrayList<String> policyData, String policyName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyBluetoothControlPolicyChange, type: ");
        stringBuilder.append(type);
        stringBuilder.append(" policyName: ");
        stringBuilder.append(policyName);
        HwLog.i(str, stringBuilder.toString());
        Intent intent = new Intent("com.huawei.devicepolicy.action.POLICY_CHANGED");
        intent.setPackage(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        intent.addFlags(1073741824);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_NAME, policyName);
        intent.putExtra("type", type);
        intent.putStringArrayListExtra(BLUETOOTH_POLICY_EXTRA_DATA, policyData);
        this.mContext.sendBroadcast(intent);
    }

    private void notifyBluetoothPolicyStateChange(String policyName, Bundle policyData) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyBluetoothControlPolicyChange, policyName: ");
        stringBuilder.append(policyName);
        stringBuilder.append("policyData");
        stringBuilder.append(policyData);
        HwLog.i(str, stringBuilder.toString());
        Intent intent = new Intent("com.huawei.devicepolicy.action.POLICY_CHANGED");
        intent.setPackage(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        intent.addFlags(1073741824);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_NAME, policyName);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_DATA, policyData);
        this.mContext.sendBroadcast(intent);
    }

    private void notifyBluetoothSecureProfileChange(Bundle policyData) {
        Intent intent = new Intent("com.huawei.devicepolicy.action.POLICY_CHANGED");
        intent.setPackage(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        intent.addFlags(1073741824);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_NAME, BLUETOOTH_SECURE_PROFILE);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_DATA, policyData);
        this.mContext.sendBroadcast(intent);
    }

    private void notifyBluetoothDiscoverChange(String policyName) {
        Intent intent = new Intent("com.huawei.devicepolicy.action.POLICY_CHANGED");
        intent.setPackage(HwBluetoothBigDataService.BIGDATA_RECEIVER_PACKAGENAME);
        intent.addFlags(1073741824);
        intent.putExtra(BLUETOOTH_POLICY_EXTRA_NAME, policyName);
        this.mContext.sendBroadcast(intent);
    }
}

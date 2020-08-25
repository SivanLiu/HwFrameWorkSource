package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import huawei.android.os.HwProtectAreaManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceControlPlugin extends DevicePolicyPlugin {
    private static final String ALLOW_CHARGING_ADB = "allow_charging_adb";
    private static final String DB_EYES_PROTECTION_MODE = "eyes_protection_mode";
    private static final int EYE_PROTECTION_CLOSE = 0;
    private static final int EYE_PROTECTION_ON = 1;
    private static final String OEMINFO_CC_MODE_STATE = "SYSTEM_CCMODE_STATE";
    public static final String POLICY_TURN_ON_EYE_COMFORT = "device_control_turn_on_eye_comfort";
    private static final int READ_LENGTH = 8;
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
        struct.addStruct(POLICY_TURN_ON_EYE_COMFORT, PolicyStruct.PolicyType.STATE, new String[]{"value"});
        struct.addStruct("device_control_turn_on_cc_mode", PolicyStruct.PolicyType.STATE, new String[]{"value"});
        struct.addStruct("device_control_unmount_usb_devices", PolicyStruct.PolicyType.STATE, new String[]{"value"});
        struct.addStruct("device_control_turn_on_usb_debug_mode", PolicyStruct.PolicyType.STATE, new String[]{"value"});
        return struct;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0056  */
    public boolean checkCallingPermission(ComponentName who, String policyName) {
        char c;
        HwLog.i(TAG, "checkCallingPermission");
        int hashCode = policyName.hashCode();
        if (hashCode != -2079610824) {
            if (hashCode != 1494933747) {
                if (hashCode == 1877933817 && policyName.equals("device_control_turn_on_cc_mode")) {
                    c = 0;
                    if (c != 0) {
                        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_DEVICE_MANAGER", "does not have device_manager MDM permission!");
                    } else if (c == 1 || c == 2) {
                        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_USB", "does not have MDM_USB permission!");
                    } else {
                        this.mContext.enforceCallingOrSelfPermission("com.huawei.permission.sec.MDM_APP_MANAGEMENT", "does not have app_management MDM permission!");
                    }
                    return true;
                }
            } else if (policyName.equals("device_control_turn_on_usb_debug_mode")) {
                c = 2;
                if (c != 0) {
                }
                return true;
            }
        } else if (policyName.equals("device_control_unmount_usb_devices")) {
            c = 1;
            if (c != 0) {
            }
            return true;
        }
        c = 65535;
        if (c != 0) {
        }
        return true;
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        Log.i(TAG, "onSetPolicy: " + policyName);
        boolean z = false;
        if (!checkCallingPermission(who, policyName) || policyData == null) {
            return false;
        }
        boolean isSetSuccess = true;
        long identityToken = Binder.clearCallingIdentity();
        try {
            switch (policyName.hashCode()) {
                case -2079610824:
                    if (policyName.equals("device_control_unmount_usb_devices")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1494933747:
                    if (policyName.equals("device_control_turn_on_usb_debug_mode")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1576234773:
                    if (policyName.equals(POLICY_TURN_ON_EYE_COMFORT)) {
                        break;
                    }
                    z = true;
                    break;
                case 1877933817:
                    if (policyName.equals("device_control_turn_on_cc_mode")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            if (!z) {
                onSetEyeComfotPolicy(policyData);
            } else if (z) {
                isSetSuccess = onSetCCModePolicy(policyData);
            } else if (z) {
                unmountUsbDevices(policyData);
            } else if (z) {
                isSetSuccess = onSetUsbDebugModePolicy(policyData);
            }
            return isSetSuccess;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    private void onSetEyeComfotPolicy(Bundle policyData) {
        if (policyData == null) {
            HwLog.i(TAG, "onSetEyeComfotPolicy policyData is null");
            return;
        }
        Settings.System.putInt(this.mContext.getContentResolver(), "eyes_protection_mode", policyData.getBoolean("value", false) ? 1 : 0);
    }

    private boolean onSetCCModePolicy(Bundle policyData) {
        if (!HuaweiTelephonyConfigs.isHisiPlatform()) {
            HwLog.i(TAG, "current platform unsupport cc mode");
            return false;
        }
        String modeState = policyData.getBoolean("value", false) ? "enable" : "disable";
        String[] readBuf = {"AA"};
        int[] errorRet = new int[1];
        int ret = HwProtectAreaManager.getInstance().writeProtectArea(OEMINFO_CC_MODE_STATE, modeState.length(), modeState, errorRet);
        if (ret != 0) {
            HwLog.i(TAG, "writeProtectArea: ret = " + ret + " errorRet = " + Arrays.toString(errorRet));
            return false;
        }
        int ret2 = HwProtectAreaManager.getInstance().readProtectArea(OEMINFO_CC_MODE_STATE, 8, readBuf, errorRet);
        if (ret2 != 0 || readBuf.length <= 0) {
            HwLog.i(TAG, "readProtectArea: ret = " + ret2 + " errorRet = " + Arrays.toString(errorRet));
            return false;
        }
        HwLog.i(TAG, "writeValue = " + modeState + " readValue = " + readBuf[0]);
        return modeState.equals(readBuf[0]);
    }

    private void unmountUsbDevices(Bundle policyData) {
        List<VolumeInfo> volumes;
        DiskInfo diskInfo;
        StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        if (storageManager == null) {
            HwLog.e(TAG, "storageManager is null, return!");
            return;
        }
        String uuid = policyData.getString("value");
        if (uuid != null) {
            VolumeInfo vol = storageManager.findVolumeByUuid(uuid);
            if (vol == null) {
                HwLog.i(TAG, "unmount USB device failed, VolumeInfo is null");
                return;
            } else {
                volumes = new ArrayList<>(1);
                volumes.add(vol);
            }
        } else {
            volumes = storageManager.getVolumes();
        }
        for (VolumeInfo vol2 : volumes) {
            if (!(vol2 == null || (diskInfo = vol2.getDisk()) == null || !diskInfo.isUsb())) {
                storageManager.unmount(vol2.getId());
            }
        }
    }

    /* JADX WARN: Type inference failed for: r1v2, types: [int, boolean] */
    private boolean onSetUsbDebugModePolicy(Bundle policyData) {
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager == null) {
            HwLog.e(TAG, "UserManager is null!");
            return false;
        }
        boolean hasUsbDebugRestriction = userManager.hasUserRestriction("no_debugging_features");
        if (HwDeviceManager.disallowOp(11) || hasUsbDebugRestriction) {
            HwLog.i(TAG, "adb is disabled");
            return false;
        }
        ?? r1 = policyData.getBoolean("value", false);
        if (!Settings.Global.putInt(this.mContext.getContentResolver(), ALLOW_CHARGING_ADB, r1 == true ? 1 : 0) || !Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", r1)) {
            return false;
        }
        return true;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        Log.i(TAG, "onGetPolicy: " + policyName);
        boolean isTurnedOn = false;
        if (!checkCallingPermission(who, policyName) || policyData == null) {
            return false;
        }
        long identityToken = Binder.clearCallingIdentity();
        char c = 65535;
        try {
            if (policyName.hashCode() == 1576234773 && policyName.equals(POLICY_TURN_ON_EYE_COMFORT)) {
                c = 0;
            }
            if (c == 0) {
                if (Settings.System.getInt(this.mContext.getContentResolver(), "eyes_protection_mode", 0) != 0) {
                    isTurnedOn = true;
                }
                policyData.putBoolean("value", isTurnedOn);
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isEffective) {
        HwLog.i(TAG, "onRemovePolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }
}

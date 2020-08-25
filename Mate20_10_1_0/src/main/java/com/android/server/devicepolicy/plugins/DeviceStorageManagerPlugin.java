package com.android.server.devicepolicy.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemProperties;
import com.android.server.devicepolicy.DevicePolicyPlugin;
import com.android.server.devicepolicy.HwLog;
import com.android.server.devicepolicy.PolicyStruct;
import com.android.server.devicepolicy.StorageUtils;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import java.util.ArrayList;

public class DeviceStorageManagerPlugin extends DevicePolicyPlugin {
    private static final String DISABLE_SDWRITING = "disable-sdwriting";
    private static final int MSG_DO_DISABLE = 0;
    private static final int MSG_DO_ENABLE = 1;
    private static final String PERMISSION_MDM_SDCARD_WRITING = "com.huawei.permission.sec.MDM_SDCARD";
    private static final String SD_CRYPT_STATE_DECRYPTING = "decrypting";
    private static final String SD_CRYPT_STATE_ENCRYPTING = "encrypting";
    public static final String TAG = "DeviceStorageManagerPlugin";
    private static boolean isUnfinishedBusiness = false;
    private static final Object sLock = new Object();
    private Context mContext;

    public DeviceStorageManagerPlugin(Context context) {
        super(context);
        this.mContext = context;
    }

    public String getPluginName() {
        return getClass().getSimpleName();
    }

    public PolicyStruct getPolicyStruct() {
        HwLog.i(TAG, "getPolicyStruct");
        PolicyStruct struct = new PolicyStruct(this);
        struct.addStruct("disable-sdwriting", PolicyStruct.PolicyType.STATE, new String[]{"value"});
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
        this.mContext.enforceCallingOrSelfPermission(PERMISSION_MDM_SDCARD_WRITING, "does not have com.huawei.permission.sec.MDM_SDCARD permission!");
        return true;
    }

    public boolean onSetPolicy(ComponentName who, String policyName, Bundle policyData, boolean isChanged) {
        HwLog.i(TAG, "onSetPolicy + policyName :" + policyName + ", changed :" + isChanged);
        if (policyName != null && policyData != null) {
            return doExecutePolicy(policyName, policyData, isChanged);
        }
        HwLog.i(TAG, "policyData is null");
        return false;
    }

    public boolean onRemovePolicy(ComponentName who, String policyName, Bundle policyData, boolean isChanged) {
        HwLog.i(TAG, "onRemovePolicy+ policyName :" + policyName + ", changed :" + isChanged);
        if (policyName != null && policyData != null) {
            return doRemovePolicy(policyName, isChanged);
        }
        HwLog.i(TAG, "policyData is null");
        return false;
    }

    public boolean onGetPolicy(ComponentName who, String policyName, Bundle policyData) {
        HwLog.i(TAG, "onGetPolicy");
        return true;
    }

    public boolean onActiveAdminRemoved(ComponentName who, ArrayList<PolicyStruct.PolicyItem> arrayList) {
        HwLog.i(TAG, "onActiveAdminRemoved");
        return true;
    }

    public void onActiveAdminRemovedCompleted(ComponentName who, ArrayList<PolicyStruct.PolicyItem> removedPolicies) {
        HwLog.i(TAG, "onActiveAdminRemovedCompleted");
        if (removedPolicies == null) {
            HwLog.i(TAG, "removedPolicies is null");
            return;
        }
        int size = removedPolicies.size();
        for (int i = 0; i < size; i++) {
            PolicyStruct.PolicyItem policyItem = removedPolicies.get(i);
            doRemovePolicy(policyItem.getPolicyName(), policyItem.isGlobalPolicyChanged());
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x003d, code lost:
        new java.lang.Thread(new com.android.server.devicepolicy.plugins.$$Lambda$DeviceStorageManagerPlugin$6pRoucB_xgKCPpKTQkCTVbs09W4(r7.getBoolean("value"))).start();
     */
    private boolean doExecutePolicy(String policyName, Bundle policyData, boolean isChanged) {
        if (policyData == null) {
            HwLog.i(TAG, "doExecutePolicy policyData is null");
            return false;
        } else if (!isChanged) {
            return true;
        } else {
            char c = 65535;
            if (policyName.hashCode() == -1002053434 && policyName.equals("disable-sdwriting")) {
                c = 0;
            }
            if (c == 0) {
                synchronized (sLock) {
                    if (!isUnfinishedBusiness && !isExternalSdcardDecryptingOrEncrypting()) {
                        if (!isExternalSdcardNotStable()) {
                            isUnfinishedBusiness = true;
                        }
                    }
                    return false;
                }
            }
            return true;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0033, code lost:
        new java.lang.Thread(new com.android.server.devicepolicy.plugins.$$Lambda$DeviceStorageManagerPlugin$NIiw0uYnJ01EzunbBv5v3MVsWV4()).start();
     */
    private boolean doRemovePolicy(String policyName, boolean isChanged) {
        if (!isChanged) {
            return true;
        }
        char c = 65535;
        if (policyName.hashCode() == -1002053434 && policyName.equals("disable-sdwriting")) {
            c = 0;
        }
        if (c == 0) {
            synchronized (sLock) {
                if (!isUnfinishedBusiness && !isExternalSdcardDecryptingOrEncrypting()) {
                    if (!isExternalSdcardNotStable()) {
                        isUnfinishedBusiness = true;
                    }
                }
                return false;
            }
        }
        return true;
    }

    public /* synthetic */ void lambda$doRemovePolicy$1$DeviceStorageManagerPlugin() {
        lambda$doExecutePolicy$0$DeviceStorageManagerPlugin(false);
    }

    private boolean shouldDoEnableWriting() {
        if (StorageUtils.isExternalSdcardMountedRO(this.mContext)) {
            return true;
        }
        return false;
    }

    private boolean shouldDoDisableWriting() {
        if (StorageUtils.isExternalSdcardMountedRW(this.mContext)) {
            return true;
        }
        return false;
    }

    private void doMountandUnMountSdcard() {
        StorageUtils.doUnMount(this.mContext);
        StorageUtils.doMount(this.mContext);
    }

    private boolean isExternalSdcardNotStable() {
        long token = Binder.clearCallingIdentity();
        try {
            return StorageUtils.isExternalSdcardNotStable(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isExternalSdcardDecryptingOrEncrypting() {
        long token = Binder.clearCallingIdentity();
        try {
            String state = SystemProperties.get("vold.cryptsd.state", HwAPPQoEUserAction.DEFAULT_CHIP_TYPE);
            return state.equals(SD_CRYPT_STATE_DECRYPTING) || state.equals(SD_CRYPT_STATE_ENCRYPTING);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: doWritingJob */
    public void lambda$doExecutePolicy$0$DeviceStorageManagerPlugin(boolean isDisable) {
        HwLog.i(TAG, "doWritingJob isDisable" + isDisable);
        if (isDisable) {
            if (shouldDoDisableWriting()) {
                doMountandUnMountSdcard();
            }
        } else if (shouldDoEnableWriting()) {
            doMountandUnMountSdcard();
        }
        synchronized (sLock) {
            isUnfinishedBusiness = false;
        }
    }
}

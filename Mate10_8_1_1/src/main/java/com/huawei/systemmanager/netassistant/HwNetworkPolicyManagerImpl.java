package com.huawei.systemmanager.netassistant;

import android.content.Context;
import android.net.HwNetworkPolicyManager;

class HwNetworkPolicyManagerImpl implements IHwNetworkPolicyManager {
    private static HwNetworkPolicyManagerImpl sInstance;
    private HwNetworkPolicyManager mPolicyManager;

    public HwNetworkPolicyManagerImpl(Context context) {
        this.mPolicyManager = HwNetworkPolicyManager.from(context);
    }

    public static synchronized IHwNetworkPolicyManager getInstance(Context context) {
        IHwNetworkPolicyManager iHwNetworkPolicyManager;
        synchronized (HwNetworkPolicyManagerImpl.class) {
            if (sInstance == null) {
                sInstance = new HwNetworkPolicyManagerImpl(context);
            }
            iHwNetworkPolicyManager = sInstance;
        }
        return iHwNetworkPolicyManager;
    }

    public void setHwUidPolicy(int uid, int policy) {
        if (this.mPolicyManager != null) {
            this.mPolicyManager.setHwUidPolicy(uid, policy);
        }
    }

    public int getHwUidPolicy(int uid) {
        if (this.mPolicyManager != null) {
            return this.mPolicyManager.getHwUidPolicy(uid);
        }
        return 0;
    }

    public void removeHwUidPolicy(int uid, int policy) {
        if (this.mPolicyManager != null) {
            this.mPolicyManager.removeHwUidPolicy(uid, policy);
        }
    }

    public void addHwUidPolicy(int uid, int policy) {
        if (this.mPolicyManager != null) {
            this.mPolicyManager.addHwUidPolicy(uid, policy);
        }
    }

    public void forceUpdatePolicy(boolean isRoaming) {
        if (this.mPolicyManager != null) {
            this.mPolicyManager.forceUpdatePolicy(isRoaming);
        }
    }
}

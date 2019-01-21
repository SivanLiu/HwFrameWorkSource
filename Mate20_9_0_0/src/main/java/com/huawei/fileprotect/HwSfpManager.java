package com.huawei.fileprotect;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import huawei.android.security.IHwSecurityService;
import huawei.android.security.IHwSecurityService.Stub;
import huawei.android.security.IHwSfpService;
import java.util.List;

public class HwSfpManager {
    private static final int HWDATAENCRYPTION_PLUGIN_ID = 11;
    private static final String SECURITY_SERVICE = "securityserver";
    public static final int STORAGE_ECE_TYPE = 2;
    public static final int STORAGE_SECE_TYPE = 3;
    private static final String TAG = "HwSfpManager";
    private static IHwSfpService mHwSfpService;
    private static final Object mInstanceSync = new Object();
    private static HwSfpManager sSelf = null;

    private HwSfpManager() {
    }

    public static HwSfpManager getDefault() {
        HwSfpManager hwSfpManager;
        synchronized (HwSfpManager.class) {
            if (sSelf == null) {
                sSelf = new HwSfpManager();
            }
            hwSfpManager = sSelf;
        }
        return hwSfpManager;
    }

    private static IHwSfpService getService() {
        synchronized (mInstanceSync) {
            if (mHwSfpService != null) {
                IHwSfpService iHwSfpService = mHwSfpService;
                return iHwSfpService;
            }
            IHwSecurityService secService = Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
            if (secService != null) {
                try {
                    mHwSfpService = IHwSfpService.Stub.asInterface(secService.querySecurityInterface(11));
                } catch (RemoteException e) {
                    Log.e(TAG, "remote service error");
                    return null;
                }
            }
            IHwSfpService iHwSfpService2 = mHwSfpService;
            return iHwSfpService2;
        }
    }

    public String getKeyDesc(int userId, int storageType) {
        if (getService() == null) {
            return null;
        }
        try {
            return mHwSfpService.getKeyDesc(userId, storageType);
        } catch (RemoteException e) {
            Log.e(TAG, "remote service error");
            return null;
        }
    }

    public List<String> getSensitiveDataPolicyList() {
        if (getService() == null) {
            return null;
        }
        try {
            return mHwSfpService.getSensitiveDataPolicyList();
        } catch (RemoteException e) {
            Log.e(TAG, "remote service error");
            return null;
        }
    }
}

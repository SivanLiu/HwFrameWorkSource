package com.android.server.security.antimal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.huawei.securitycenter.IHwSecService;

public class AntiMalDataPipeline {
    private static final Object LOCK = new Object();
    private static final String SERVICE_NAME = "com.huawei.securitycenter.mainservice.HwSecService";
    /* access modifiers changed from: private */
    public static final String TAG = AntiMalDataPipeline.class.getSimpleName();
    private static volatile AntiMalDataPipeline sAntiMalDataPipeline;
    /* access modifiers changed from: private */
    public IHwSecService mHwSecService;

    private AntiMalDataPipeline() {
    }

    public static AntiMalDataPipeline getInstance() {
        if (sAntiMalDataPipeline == null) {
            synchronized (LOCK) {
                if (sAntiMalDataPipeline == null) {
                    sAntiMalDataPipeline = new AntiMalDataPipeline();
                }
            }
        }
        return sAntiMalDataPipeline;
    }

    private IHwSecService getHwSecService() {
        IHwSecService iHwSecService = this.mHwSecService;
        if (iHwSecService != null) {
            return iHwSecService;
        }
        try {
            IBinder binder = ServiceManager.getService(SERVICE_NAME);
            if (binder != null) {
                this.mHwSecService = IHwSecService.Stub.asInterface(binder);
                binder.linkToDeath(new IBinder.DeathRecipient() {
                    /* class com.android.server.security.antimal.AntiMalDataPipeline.AnonymousClass1 */

                    public void binderDied() {
                        Log.e(AntiMalDataPipeline.TAG, "HwSecService client died.");
                        IHwSecService unused = AntiMalDataPipeline.this.mHwSecService = null;
                    }
                }, 0);
            } else {
                Log.e(TAG, "getHwSecService: error, bind is null.");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getHwSecService occurs RemoteException");
        } catch (Exception e2) {
            Log.e(TAG, "getHwSecService occurs Exception");
        } catch (Error e3) {
            Log.e(TAG, "getHwSecService occurs Error");
        }
        return this.mHwSecService;
    }

    public Bundle transferMalInformation(String module, Bundle bundle) {
        try {
            if (getHwSecService() != null) {
                return this.mHwSecService.call(module, bundle);
            }
            Log.e(TAG, "transferMalInformation: mHwSecService is null.");
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "transferMalInformation occurs RemoteException.");
            return null;
        } catch (Exception e2) {
            Log.e(TAG, "transferMalInformation occurs Exception.");
            return null;
        }
    }
}

package com.android.server.cota;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.huawei.cota.aidl.ICotaCallBack;
import com.huawei.cota.aidl.ICotaInterface.Stub;

public class CotaService extends Service {
    private static ICotaCallBack mICotaCallBack;
    private final Stub mBinder = new Stub() {
        public boolean registerCallBack(ICotaCallBack callback, String packageName) throws RemoteException {
            if (callback == null) {
                return false;
            }
            CotaService.mICotaCallBack = callback;
            return true;
        }

        public boolean unregisterCallBack(ICotaCallBack callback, String packageName) throws RemoteException {
            CotaService.mICotaCallBack = null;
            return true;
        }

        public void startInstallApks() throws RemoteException {
            CotaInstallImpl.getInstance().doInstall();
        }

        public int getApksInstallStatus() throws RemoteException {
            return CotaInstallImpl.getInstance().doGetStatus();
        }
    };

    public static ICotaCallBack getICotaCallBack() {
        return mICotaCallBack;
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}

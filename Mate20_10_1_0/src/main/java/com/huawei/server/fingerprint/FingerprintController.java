package com.huawei.server.fingerprint;

import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class FingerprintController {
    private static final int INVALID_VALUE = -1;
    private static final int REFRESH_RATE_CODE = 20000;
    private static final String TAG = "FingerprintController";
    private static FingerprintController sInstance;
    private int mScreenRefreshRateState = -1;

    private FingerprintController() {
    }

    public static synchronized FingerprintController getInstance() {
        FingerprintController fingerprintController;
        synchronized (FingerprintController.class) {
            if (sInstance == null) {
                sInstance = new FingerprintController();
            }
            fingerprintController = sInstance;
        }
        return fingerprintController;
    }

    public void setScreenRefreshRate(final int state, final String packageName, Handler handler) {
        if (handler == null) {
            Log.w(TAG, "setScreenRefreshRate handler is null");
            return;
        }
        synchronized (this) {
            if (this.mScreenRefreshRateState != state) {
                this.mScreenRefreshRateState = state;
                handler.post(new Runnable() {
                    /* class com.huawei.server.fingerprint.FingerprintController.AnonymousClass1 */

                    public void run() {
                        Parcel data = Parcel.obtain();
                        Parcel reply = Parcel.obtain();
                        try {
                            IBinder service = ServiceManager.getService("AGPService");
                            if (service != null) {
                                data.writeString(packageName);
                                data.writeInt(state);
                                service.transact(FingerprintController.REFRESH_RATE_CODE, data, reply, 1);
                                Log.i(FingerprintController.TAG, "refresh rate state:" + state + ",packageName=" + packageName);
                            }
                        } catch (RemoteException e) {
                            Log.e(FingerprintController.TAG, "updateRefreshRate error");
                        } catch (Throwable th) {
                            reply.recycle();
                            data.recycle();
                            throw th;
                        }
                        reply.recycle();
                        data.recycle();
                    }
                });
            }
        }
    }
}

package com.android.server.security.trustcircle;

import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import com.android.server.security.trustcircle.utils.LogHelper;
import huawei.android.security.ILifeCycleCallback;

public class CallbackManager {
    private static final String TAG = CallbackManager.class.getSimpleName();
    private static volatile CallbackManager sInstance;
    private DeathRecipient mILifeCycleBinderDieListener = new DeathRecipient() {
        public void binderDied() {
            CallbackManager.this.mILifeCycleCallback = null;
        }
    };
    private volatile ILifeCycleCallback mILifeCycleCallback = null;

    private CallbackManager() {
    }

    public static CallbackManager getInstance() {
        if (sInstance == null) {
            synchronized (CallbackManager.class) {
                if (sInstance == null) {
                    sInstance = new CallbackManager();
                }
            }
        }
        return sInstance;
    }

    public boolean registerILifeCycleCallback(ILifeCycleCallback callback) {
        if (callback == null || this.mILifeCycleCallback != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, register ILifeCycleCallback failed: ");
            String str2 = callback == null ? "registered callback is null" : this.mILifeCycleCallback != null ? "previous callback is not null" : "";
            stringBuilder.append(str2);
            LogHelper.e(str, stringBuilder.toString());
            return false;
        }
        this.mILifeCycleCallback = callback;
        try {
            this.mILifeCycleCallback.asBinder().linkToDeath(this.mILifeCycleBinderDieListener, 0);
            return true;
        } catch (RemoteException e) {
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("RemoteException in registerILifeCycleCallback: ");
            stringBuilder2.append(e.getMessage());
            LogHelper.e(str3, stringBuilder2.toString());
            this.mILifeCycleCallback = null;
            return false;
        }
    }

    public boolean isILifeCycleCallbackValid() {
        return this.mILifeCycleCallback != null;
    }

    public ILifeCycleCallback getILifeCycleCallback() {
        return this.mILifeCycleCallback;
    }

    public void unregisterILifeCycleCallback() {
        this.mILifeCycleCallback = null;
    }
}

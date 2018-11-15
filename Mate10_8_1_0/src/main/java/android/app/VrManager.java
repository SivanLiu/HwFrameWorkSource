package android.app;

import android.content.ComponentName;
import android.os.Handler;
import android.os.RemoteException;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IPersistentVrStateCallbacks.Stub;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.ArrayMap;
import java.util.Map;

public class VrManager {
    private Map<VrStateCallback, CallbackEntry> mCallbackMap = new ArrayMap();
    private final IVrManager mService;

    private static class CallbackEntry {
        final VrStateCallback mCallback;
        final Handler mHandler;
        final IPersistentVrStateCallbacks mPersistentStateCallback = new Stub() {
            /* synthetic */ void lambda$-android_app_VrManager$CallbackEntry$2_1220(boolean enabled) {
                CallbackEntry.this.mCallback.onPersistentVrStateChanged(enabled);
            }

            public void onPersistentVrStateChanged(boolean enabled) {
                CallbackEntry.this.mHandler.post(new -$Lambda$BjtyKj7ksh5kcpFCATScxTJ5PrQ((byte) 1, enabled, this));
            }
        };
        final IVrStateCallbacks mStateCallback = new IVrStateCallbacks.Stub() {
            /* synthetic */ void lambda$-android_app_VrManager$CallbackEntry$1_902(boolean enabled) {
                CallbackEntry.this.mCallback.onVrStateChanged(enabled);
            }

            public void onVrStateChanged(boolean enabled) {
                CallbackEntry.this.mHandler.post(new -$Lambda$BjtyKj7ksh5kcpFCATScxTJ5PrQ((byte) 0, enabled, this));
            }
        };

        CallbackEntry(VrStateCallback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }
    }

    public VrManager(IVrManager service) {
        this.mService = service;
    }

    public void registerVrStateCallback(VrStateCallback callback, Handler handler) {
        if (callback != null && !this.mCallbackMap.containsKey(callback)) {
            CallbackEntry entry = new CallbackEntry(callback, handler);
            this.mCallbackMap.put(callback, entry);
            try {
                this.mService.registerListener(entry.mStateCallback);
                this.mService.registerPersistentVrStateListener(entry.mPersistentStateCallback);
            } catch (RemoteException e) {
                try {
                    unregisterVrStateCallback(callback);
                } catch (Exception e2) {
                    e.rethrowFromSystemServer();
                }
            }
        }
    }

    public void unregisterVrStateCallback(VrStateCallback callback) {
        CallbackEntry entry = (CallbackEntry) this.mCallbackMap.remove(callback);
        if (entry != null) {
            try {
                this.mService.unregisterListener(entry.mStateCallback);
            } catch (RemoteException e) {
            }
            try {
                this.mService.unregisterPersistentVrStateListener(entry.mPersistentStateCallback);
            } catch (RemoteException e2) {
            }
        }
    }

    public boolean getVrModeEnabled() {
        try {
            return this.mService.getVrModeState();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public boolean getPersistentVrModeEnabled() {
        try {
            return this.mService.getPersistentVrModeEnabled();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return false;
        }
    }

    public void setPersistentVrModeEnabled(boolean enabled) {
        try {
            this.mService.setPersistentVrModeEnabled(enabled);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProp) {
        try {
            this.mService.setVr2dDisplayProperties(vr2dDisplayProp);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void setAndBindVrCompositor(ComponentName componentName) {
        String str = null;
        try {
            IVrManager iVrManager = this.mService;
            if (componentName != null) {
                str = componentName.flattenToString();
            }
            iVrManager.setAndBindCompositor(str);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }
}

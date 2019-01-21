package android.companion;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.PendingIntent;
import android.companion.IFindDeviceCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class CompanionDeviceManager {
    public static final String COMPANION_DEVICE_DISCOVERY_PACKAGE_NAME = "com.android.companiondevicemanager";
    private static final boolean DEBUG = false;
    public static final String EXTRA_DEVICE = "android.companion.extra.DEVICE";
    private static final String LOG_TAG = "CompanionDeviceManager";
    private final Context mContext;
    private final ICompanionDeviceManager mService;

    public static abstract class Callback {
        public abstract void onDeviceFound(IntentSender intentSender);

        public abstract void onFailure(CharSequence charSequence);
    }

    private class CallbackProxy extends Stub implements ActivityLifecycleCallbacks {
        private Callback mCallback;
        private Handler mHandler;
        final Object mLock;
        private AssociationRequest mRequest;

        private CallbackProxy(AssociationRequest request, Callback callback, Handler handler) {
            this.mLock = new Object();
            this.mCallback = callback;
            this.mHandler = handler;
            this.mRequest = request;
            CompanionDeviceManager.this.getActivity().getApplication().registerActivityLifecycleCallbacks(this);
        }

        public void onSuccess(PendingIntent launcher) {
            lockAndPost(-$$Lambda$OThxsns9MAD5QsKURFQAFbt-3qc.INSTANCE, launcher.getIntentSender());
        }

        public void onFailure(CharSequence reason) {
            lockAndPost(-$$Lambda$ZUPGnRMz08ZrG1ogNO-2O5Hso3I.INSTANCE, reason);
        }

        <T> void lockAndPost(BiConsumer<Callback, T> action, T payload) {
            synchronized (this.mLock) {
                if (this.mHandler != null) {
                    this.mHandler.post(new -$$Lambda$CompanionDeviceManager$CallbackProxy$gkUVA3m3QgEEk8G84_kcBFARHvo(this, action, payload));
                }
            }
        }

        public static /* synthetic */ void lambda$lockAndPost$0(CallbackProxy callbackProxy, BiConsumer action, Object payload) {
            Callback callback;
            synchronized (callbackProxy.mLock) {
                callback = callbackProxy.mCallback;
            }
            if (callback != null) {
                action.accept(callback, payload);
            }
        }

        public void onActivityDestroyed(Activity activity) {
            synchronized (this.mLock) {
                if (activity != CompanionDeviceManager.this.getActivity()) {
                    return;
                }
                try {
                    CompanionDeviceManager.this.mService.stopScan(this.mRequest, this, CompanionDeviceManager.this.getCallingPackage());
                } catch (RemoteException e) {
                    e.rethrowFromSystemServer();
                }
                CompanionDeviceManager.this.getActivity().getApplication().unregisterActivityLifecycleCallbacks(this);
                this.mCallback = null;
                this.mHandler = null;
                this.mRequest = null;
            }
        }

        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        public void onActivityStarted(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
        }

        public void onActivityPaused(Activity activity) {
        }

        public void onActivityStopped(Activity activity) {
        }

        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    public CompanionDeviceManager(ICompanionDeviceManager service, Context context) {
        this.mService = service;
        this.mContext = context;
    }

    public void associate(AssociationRequest request, Callback callback, Handler handler) {
        if (checkFeaturePresent()) {
            Preconditions.checkNotNull(request, "Request cannot be null");
            Preconditions.checkNotNull(callback, "Callback cannot be null");
            try {
                this.mService.associate(request, new CallbackProxy(request, callback, Handler.mainIfNull(handler)), getCallingPackage());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public List<String> getAssociations() {
        if (!checkFeaturePresent()) {
            return Collections.emptyList();
        }
        try {
            return this.mService.getAssociations(getCallingPackage(), this.mContext.getUserId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disassociate(String deviceMacAddress) {
        if (checkFeaturePresent()) {
            try {
                this.mService.disassociate(deviceMacAddress, getCallingPackage());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void requestNotificationAccess(ComponentName component) {
        if (checkFeaturePresent()) {
            try {
                PendingIntent pendingIntent = this.mService.requestNotificationAccess(component);
                if (pendingIntent != null) {
                    this.mContext.startIntentSender(pendingIntent.getIntentSender(), null, 0, 0, 0);
                } else {
                    Log.e(LOG_TAG, "pendingIntent is null in function requestNotificationAccess");
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (SendIntentException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public boolean hasNotificationAccess(ComponentName component) {
        if (!checkFeaturePresent()) {
            return false;
        }
        try {
            return this.mService.hasNotificationAccess(component);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private boolean checkFeaturePresent() {
        return this.mService != null;
    }

    private Activity getActivity() {
        return (Activity) this.mContext;
    }

    private String getCallingPackage() {
        return this.mContext.getPackageName();
    }
}

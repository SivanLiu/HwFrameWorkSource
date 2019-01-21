package android.telephony.ims.feature;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.Intent;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsCapabilityCallback.Stub;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

@SystemApi
public abstract class ImsFeature {
    public static final String ACTION_IMS_SERVICE_DOWN = "com.android.ims.IMS_SERVICE_DOWN";
    public static final String ACTION_IMS_SERVICE_UP = "com.android.ims.IMS_SERVICE_UP";
    public static final int CAPABILITY_ERROR_GENERIC = -1;
    public static final int CAPABILITY_SUCCESS = 0;
    public static final String EXTRA_PHONE_ID = "android:phone_id";
    public static final int FEATURE_EMERGENCY_MMTEL = 0;
    public static final int FEATURE_INVALID = -1;
    public static final int FEATURE_MAX = 3;
    public static final int FEATURE_MMTEL = 1;
    public static final int FEATURE_RCS = 2;
    private static final String LOG_TAG = "ImsFeature";
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_UNAVAILABLE = 0;
    private final RemoteCallbackList<IImsCapabilityCallback> mCapabilityCallbacks = new RemoteCallbackList();
    private Capabilities mCapabilityStatus = new Capabilities();
    protected Context mContext;
    private final Object mLock = new Object();
    private int mSlotId = -1;
    private int mState = 0;
    private final Set<IImsFeatureStatusCallback> mStatusCallbacks = Collections.newSetFromMap(new WeakHashMap());

    public static class Capabilities {
        protected int mCapabilities = 0;

        protected Capabilities(int capabilities) {
            this.mCapabilities = capabilities;
        }

        public void addCapabilities(int capabilities) {
            this.mCapabilities |= capabilities;
        }

        public void removeCapabilities(int capabilities) {
            this.mCapabilities &= ~capabilities;
        }

        public boolean isCapable(int capabilities) {
            return (this.mCapabilities & capabilities) == capabilities;
        }

        public Capabilities copy() {
            return new Capabilities(this.mCapabilities);
        }

        public int getMask() {
            return this.mCapabilities;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (!(o instanceof Capabilities)) {
                return false;
            }
            if (this.mCapabilities != ((Capabilities) o).mCapabilities) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return this.mCapabilities;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Capabilities: ");
            stringBuilder.append(Integer.toBinaryString(this.mCapabilities));
            return stringBuilder.toString();
        }
    }

    protected static class CapabilityCallbackProxy {
        private final IImsCapabilityCallback mCallback;

        public CapabilityCallbackProxy(IImsCapabilityCallback c) {
            this.mCallback = c;
        }

        public void onChangeCapabilityConfigurationError(int capability, int radioTech, int reason) {
            if (this.mCallback != null) {
                try {
                    this.mCallback.onChangeCapabilityConfigurationError(capability, radioTech, reason);
                } catch (RemoteException e) {
                    Log.e(ImsFeature.LOG_TAG, "onChangeCapabilityConfigurationError called on dead binder.");
                }
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsCapabilityError {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsState {
    }

    public static class CapabilityCallback extends Stub {
        public final void onCapabilitiesStatusChanged(int config) throws RemoteException {
            onCapabilitiesStatusChanged(new Capabilities(config));
        }

        public void onQueryCapabilityConfiguration(int capability, int radioTech, boolean isEnabled) {
        }

        public void onChangeCapabilityConfigurationError(int capability, int radioTech, int reason) {
        }

        public void onCapabilitiesStatusChanged(Capabilities config) {
        }
    }

    public abstract void changeEnabledCapabilities(CapabilityChangeRequest capabilityChangeRequest, CapabilityCallbackProxy capabilityCallbackProxy);

    protected abstract IInterface getBinder();

    public abstract void onFeatureReady();

    public abstract void onFeatureRemoved();

    public final void initialize(Context context, int slotId) {
        this.mContext = context;
        this.mSlotId = slotId;
    }

    public int getFeatureState() {
        int i;
        synchronized (this.mLock) {
            i = this.mState;
        }
        return i;
    }

    public final void setFeatureState(int state) {
        synchronized (this.mLock) {
            if (this.mState != state) {
                this.mState = state;
                notifyFeatureState(state);
            }
        }
    }

    @VisibleForTesting
    public void addImsFeatureStatusCallback(IImsFeatureStatusCallback c) {
        try {
            c.notifyImsFeatureStatus(getFeatureState());
            synchronized (this.mLock) {
                this.mStatusCallbacks.add(c);
            }
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't notify feature state: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
    }

    @VisibleForTesting
    public void removeImsFeatureStatusCallback(IImsFeatureStatusCallback c) {
        synchronized (this.mLock) {
            this.mStatusCallbacks.remove(c);
        }
    }

    private void notifyFeatureState(int state) {
        synchronized (this.mLock) {
            Iterator<IImsFeatureStatusCallback> iter = this.mStatusCallbacks.iterator();
            while (iter.hasNext()) {
                IImsFeatureStatusCallback callback = (IImsFeatureStatusCallback) iter.next();
                try {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("notifying ImsFeatureState=");
                    stringBuilder.append(state);
                    Log.i(str, stringBuilder.toString());
                    callback.notifyImsFeatureStatus(state);
                } catch (RemoteException e) {
                    iter.remove();
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Couldn't notify feature state: ");
                    stringBuilder2.append(e.getMessage());
                    Log.w(str2, stringBuilder2.toString());
                }
            }
        }
        sendImsServiceIntent(state);
    }

    private void sendImsServiceIntent(int state) {
        if (this.mContext != null && this.mSlotId != -1) {
            Intent intent;
            switch (state) {
                case 0:
                case 1:
                    intent = new Intent("com.android.ims.IMS_SERVICE_DOWN");
                    break;
                case 2:
                    intent = new Intent("com.android.ims.IMS_SERVICE_UP");
                    break;
                default:
                    intent = new Intent("com.android.ims.IMS_SERVICE_DOWN");
                    break;
            }
            intent.putExtra("android:phone_id", this.mSlotId);
            this.mContext.sendBroadcast(intent);
        }
    }

    public final void addCapabilityCallback(IImsCapabilityCallback c) {
        this.mCapabilityCallbacks.register(c);
    }

    public final void removeCapabilityCallback(IImsCapabilityCallback c) {
        this.mCapabilityCallbacks.unregister(c);
    }

    @VisibleForTesting
    public Capabilities queryCapabilityStatus() {
        Capabilities copy;
        synchronized (this.mLock) {
            copy = this.mCapabilityStatus.copy();
        }
        return copy;
    }

    @VisibleForTesting
    public final void requestChangeEnabledCapabilities(CapabilityChangeRequest request, IImsCapabilityCallback c) {
        if (request != null) {
            changeEnabledCapabilities(request, new CapabilityCallbackProxy(c));
            return;
        }
        throw new IllegalArgumentException("ImsFeature#requestChangeEnabledCapabilities called with invalid params.");
    }

    protected final void notifyCapabilitiesStatusChanged(Capabilities c) {
        synchronized (this.mLock) {
            this.mCapabilityStatus = c.copy();
        }
        int count = this.mCapabilityCallbacks.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                ((IImsCapabilityCallback) this.mCapabilityCallbacks.getBroadcastItem(i)).onCapabilitiesStatusChanged(c.mCapabilities);
            } catch (RemoteException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(e);
                stringBuilder.append(" notifyCapabilitiesStatusChanged() - Skipping callback.");
                Log.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mCapabilityCallbacks.finishBroadcast();
            }
        }
        this.mCapabilityCallbacks.finishBroadcast();
    }
}

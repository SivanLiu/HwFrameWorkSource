package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.ImsService.Listener;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsFeatureStatusCallback.Stub;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImsServiceController {
    private static final String LOG_TAG = "ImsServiceController";
    private static final int REBIND_MAXIMUM_DELAY_MS = 60000;
    private static final int REBIND_START_DELAY_MS = 2000;
    private ExponentialBackoff mBackoff;
    private ImsServiceControllerCallbacks mCallbacks;
    private final ComponentName mComponentName;
    protected final Context mContext;
    private Listener mFeatureChangedListener = new Listener() {
        public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) {
            if (ImsServiceController.this.mCallbacks != null) {
                ImsServiceController.this.mCallbacks.imsServiceFeaturesChanged(c, ImsServiceController.this);
            }
        }
    };
    private Set<ImsFeatureStatusCallback> mFeatureStatusCallbacks = new HashSet();
    private final HandlerThread mHandlerThread = new HandlerThread("ImsServiceControllerHandler");
    private IImsServiceController mIImsServiceController;
    private ImsDeathRecipient mImsDeathRecipient;
    private HashSet<ImsFeatureContainer> mImsFeatureBinders = new HashSet();
    private HashSet<FeatureSlotPair> mImsFeatures;
    private ImsServiceConnection mImsServiceConnection;
    private IBinder mImsServiceControllerBinder;
    private Set<IImsServiceFeatureCallback> mImsStatusCallbacks = ConcurrentHashMap.newKeySet();
    private boolean mIsBinding = false;
    private boolean mIsBound = false;
    protected final Object mLock = new Object();
    private final IPackageManager mPackageManager;
    private RebindRetry mRebindRetry = new RebindRetry() {
        public long getStartDelay() {
            return 2000;
        }

        public long getMaximumDelay() {
            return 60000;
        }
    };
    private Runnable mRestartImsServiceRunnable = new Runnable() {
        public void run() {
            synchronized (ImsServiceController.this.mLock) {
                if (ImsServiceController.this.mIsBound) {
                    return;
                }
                ImsServiceController.this.bind(ImsServiceController.this.mImsFeatures);
            }
        }
    };

    class ImsDeathRecipient implements DeathRecipient {
        private ComponentName mComponentName;

        ImsDeathRecipient(ComponentName name) {
            this.mComponentName = name;
        }

        public void binderDied() {
            String str = ImsServiceController.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsService(");
            stringBuilder.append(this.mComponentName);
            stringBuilder.append(") died. Restarting...");
            Log.e(str, stringBuilder.toString());
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
                ImsServiceController.this.mIsBound = false;
            }
            ImsServiceController.this.notifyAllFeaturesRemoved();
            ImsServiceController.this.cleanUpService();
            ImsServiceController.this.startDelayedRebindToService();
        }
    }

    private class ImsFeatureContainer {
        public int featureType;
        private IInterface mBinder;
        public int slotId;

        ImsFeatureContainer(int slotId, int featureType, IInterface binder) {
            this.slotId = slotId;
            this.featureType = featureType;
            this.mBinder = binder;
        }

        public <T extends IInterface> T resolve(Class<T> className) {
            return (IInterface) className.cast(this.mBinder);
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ImsFeatureContainer that = (ImsFeatureContainer) o;
            if (this.slotId != that.slotId || this.featureType != that.featureType) {
                return false;
            }
            if (this.mBinder != null) {
                z = this.mBinder.equals(that.mBinder);
            } else if (that.mBinder != null) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (31 * ((31 * this.slotId) + this.featureType)) + (this.mBinder != null ? this.mBinder.hashCode() : 0);
        }
    }

    private class ImsFeatureStatusCallback {
        private final IImsFeatureStatusCallback mCallback = new Stub() {
            public void notifyImsFeatureStatus(int featureStatus) throws RemoteException {
                String str = ImsServiceController.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyImsFeatureStatus: slot=");
                stringBuilder.append(ImsFeatureStatusCallback.this.mSlotId);
                stringBuilder.append(", feature=");
                stringBuilder.append(ImsFeatureStatusCallback.this.mFeatureType);
                stringBuilder.append(", status=");
                stringBuilder.append(featureStatus);
                Log.i(str, stringBuilder.toString());
                ImsServiceController.this.sendImsFeatureStatusChanged(ImsFeatureStatusCallback.this.mSlotId, ImsFeatureStatusCallback.this.mFeatureType, featureStatus);
            }
        };
        private int mFeatureType;
        private int mSlotId;

        ImsFeatureStatusCallback(int slotId, int featureType) {
            this.mSlotId = slotId;
            this.mFeatureType = featureType;
        }

        public IImsFeatureStatusCallback getCallback() {
            return this.mCallback;
        }
    }

    class ImsServiceConnection implements ServiceConnection {
        ImsServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            ImsServiceController.this.mBackoff.stop();
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBound = true;
                ImsServiceController.this.mIsBinding = false;
                String str = ImsServiceController.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ImsService(");
                stringBuilder.append(name);
                stringBuilder.append("): onServiceConnected with binder: ");
                stringBuilder.append(service);
                Log.d(str, stringBuilder.toString());
                if (service != null) {
                    ImsServiceController.this.mImsDeathRecipient = new ImsDeathRecipient(name);
                    try {
                        service.linkToDeath(ImsServiceController.this.mImsDeathRecipient, 0);
                        ImsServiceController.this.mImsServiceControllerBinder = service;
                        ImsServiceController.this.setServiceController(service);
                        ImsServiceController.this.notifyImsServiceReady();
                        Iterator it = ImsServiceController.this.mImsFeatures.iterator();
                        while (it.hasNext()) {
                            ImsServiceController.this.addImsServiceFeature((FeatureSlotPair) it.next());
                        }
                    } catch (RemoteException e) {
                        ImsServiceController.this.mIsBound = false;
                        ImsServiceController.this.mIsBinding = false;
                        if (ImsServiceController.this.mImsDeathRecipient != null) {
                            ImsServiceController.this.mImsDeathRecipient.binderDied();
                        }
                        String str2 = ImsServiceController.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ImsService(");
                        stringBuilder.append(name);
                        stringBuilder.append(") RemoteException:");
                        stringBuilder.append(e.getMessage());
                        Log.e(str2, stringBuilder.toString());
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
            }
            cleanupConnection();
            String str = ImsServiceController.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsService(");
            stringBuilder.append(name);
            stringBuilder.append("): onServiceDisconnected. Waiting...");
            Log.w(str, stringBuilder.toString());
        }

        public void onBindingDied(ComponentName name) {
            synchronized (ImsServiceController.this.mLock) {
                ImsServiceController.this.mIsBinding = false;
                ImsServiceController.this.mIsBound = false;
            }
            cleanupConnection();
            String str = ImsServiceController.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsService(");
            stringBuilder.append(name);
            stringBuilder.append("): onBindingDied. Starting rebind...");
            Log.w(str, stringBuilder.toString());
            ImsServiceController.this.startDelayedRebindToService();
        }

        private void cleanupConnection() {
            if (ImsServiceController.this.isServiceControllerAvailable()) {
                ImsServiceController.this.mImsServiceControllerBinder.unlinkToDeath(ImsServiceController.this.mImsDeathRecipient, 0);
            }
            ImsServiceController.this.notifyAllFeaturesRemoved();
            ImsServiceController.this.cleanUpService();
        }
    }

    public interface ImsServiceControllerCallbacks {
        void imsServiceFeatureCreated(int i, int i2, ImsServiceController imsServiceController);

        void imsServiceFeatureRemoved(int i, int i2, ImsServiceController imsServiceController);

        void imsServiceFeaturesChanged(ImsFeatureConfiguration imsFeatureConfiguration, ImsServiceController imsServiceController);
    }

    @VisibleForTesting
    public interface RebindRetry {
        long getMaximumDelay();

        long getStartDelay();
    }

    public ImsServiceController(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mCallbacks = callbacks;
        this.mHandlerThread.start();
        this.mBackoff = new ExponentialBackoff(this.mRebindRetry.getStartDelay(), this.mRebindRetry.getMaximumDelay(), 2, this.mHandlerThread.getLooper(), this.mRestartImsServiceRunnable);
        this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    @VisibleForTesting
    public ImsServiceController(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks, Handler handler, RebindRetry rebindRetry) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mCallbacks = callbacks;
        this.mBackoff = new ExponentialBackoff(rebindRetry.getStartDelay(), rebindRetry.getMaximumDelay(), 2, handler, this.mRestartImsServiceRunnable);
        this.mPackageManager = null;
    }

    /* JADX WARNING: Missing block: B:20:0x0094, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean bind(HashSet<FeatureSlotPair> imsFeatureSet) {
        synchronized (this.mLock) {
            if (this.mIsBound || this.mIsBinding) {
            } else {
                this.mIsBinding = true;
                this.mImsFeatures = imsFeatureSet;
                grantPermissionsToService();
                Intent imsServiceIntent = new Intent(getServiceInterface()).setComponent(this.mComponentName);
                this.mImsServiceConnection = new ImsServiceConnection();
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Binding ImsService:");
                stringBuilder.append(this.mComponentName);
                Log.i(str, stringBuilder.toString());
                try {
                    boolean bindSucceeded = startBindToService(imsServiceIntent, this.mImsServiceConnection, 67108929);
                    if (!bindSucceeded) {
                        this.mBackoff.notifyFailed();
                    }
                    return bindSucceeded;
                } catch (Exception e) {
                    this.mBackoff.notifyFailed();
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error binding (");
                    stringBuilder2.append(this.mComponentName);
                    stringBuilder2.append(") with exception: ");
                    stringBuilder2.append(e.getMessage());
                    stringBuilder2.append(", rebinding in ");
                    stringBuilder2.append(this.mBackoff.getCurrentDelay());
                    stringBuilder2.append(" ms");
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                }
            }
        }
    }

    protected boolean startBindToService(Intent intent, ImsServiceConnection connection, int flags) {
        return this.mContext.bindService(intent, connection, flags);
    }

    /* JADX WARNING: Missing block: B:12:0x0049, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void unbind() throws RemoteException {
        synchronized (this.mLock) {
            this.mBackoff.stop();
            if (this.mImsServiceConnection != null) {
                if (this.mImsDeathRecipient != null) {
                    changeImsServiceFeatures(new HashSet());
                    removeImsServiceFeatureCallbacks();
                    this.mImsServiceControllerBinder.unlinkToDeath(this.mImsDeathRecipient, 0);
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unbinding ImsService: ");
                    stringBuilder.append(this.mComponentName);
                    Log.i(str, stringBuilder.toString());
                    this.mContext.unbindService(this.mImsServiceConnection);
                    cleanUpService();
                }
            }
        }
    }

    public void changeImsServiceFeatures(HashSet<FeatureSlotPair> newImsFeatures) throws RemoteException {
        synchronized (this.mLock) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Features changed (");
            stringBuilder.append(this.mImsFeatures);
            stringBuilder.append("->");
            stringBuilder.append(newImsFeatures);
            stringBuilder.append(") for ImsService: ");
            stringBuilder.append(this.mComponentName);
            Log.i(str, stringBuilder.toString());
            HashSet<FeatureSlotPair> oldImsFeatures = new HashSet(this.mImsFeatures);
            this.mImsFeatures = newImsFeatures;
            if (this.mIsBound) {
                HashSet<FeatureSlotPair> newFeatures = new HashSet(this.mImsFeatures);
                newFeatures.removeAll(oldImsFeatures);
                Iterator it = newFeatures.iterator();
                while (it.hasNext()) {
                    addImsServiceFeature((FeatureSlotPair) it.next());
                }
                HashSet<FeatureSlotPair> oldFeatures = new HashSet(oldImsFeatures);
                oldFeatures.removeAll(this.mImsFeatures);
                Iterator it2 = oldFeatures.iterator();
                while (it2.hasNext()) {
                    removeImsServiceFeature((FeatureSlotPair) it2.next());
                }
            }
        }
    }

    @VisibleForTesting
    public IImsServiceController getImsServiceController() {
        return this.mIImsServiceController;
    }

    @VisibleForTesting
    public IBinder getImsServiceControllerBinder() {
        return this.mImsServiceControllerBinder;
    }

    @VisibleForTesting
    public long getRebindDelay() {
        return this.mBackoff.getCurrentDelay();
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    /* JADX WARNING: Missing block: B:20:0x003d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addImsServiceFeatureCallback(IImsServiceFeatureCallback callback) {
        if (callback != null) {
            this.mImsStatusCallbacks.add(callback);
            synchronized (this.mLock) {
                if (this.mImsFeatures == null || this.mImsFeatures.isEmpty()) {
                } else {
                    try {
                        Iterator it = this.mImsFeatures.iterator();
                        while (it.hasNext()) {
                            FeatureSlotPair i = (FeatureSlotPair) it.next();
                            callback.imsFeatureCreated(i.slotId, i.featureType);
                        }
                    } catch (RemoteException e) {
                        Log.w(LOG_TAG, "addImsServiceFeatureCallback: exception notifying callback");
                    }
                }
            }
        }
    }

    public void enableIms(int slotId) {
        try {
            synchronized (this.mLock) {
                if (isServiceControllerAvailable()) {
                    this.mIImsServiceController.enableIms(slotId);
                }
            }
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't enable IMS: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
    }

    public void disableIms(int slotId) {
        try {
            synchronized (this.mLock) {
                if (isServiceControllerAvailable()) {
                    this.mIImsServiceController.disableIms(slotId);
                }
            }
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't disable IMS: ");
            stringBuilder.append(e.getMessage());
            Log.w(str, stringBuilder.toString());
        }
    }

    public IImsMmTelFeature getMmTelFeature(int slotId) {
        synchronized (this.mLock) {
            ImsFeatureContainer f = getImsFeatureContainer(slotId, 1);
            if (f == null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Requested null MMTelFeature on slot ");
                stringBuilder.append(slotId);
                Log.w(str, stringBuilder.toString());
                return null;
            }
            IImsMmTelFeature iImsMmTelFeature = (IImsMmTelFeature) f.resolve(IImsMmTelFeature.class);
            return iImsMmTelFeature;
        }
    }

    public IImsRcsFeature getRcsFeature(int slotId) {
        synchronized (this.mLock) {
            ImsFeatureContainer f = getImsFeatureContainer(slotId, 2);
            if (f == null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Requested null RcsFeature on slot ");
                stringBuilder.append(slotId);
                Log.w(str, stringBuilder.toString());
                return null;
            }
            IImsRcsFeature iImsRcsFeature = (IImsRcsFeature) f.resolve(IImsRcsFeature.class);
            return iImsRcsFeature;
        }
    }

    public IImsRegistration getRegistration(int slotId) throws RemoteException {
        IImsRegistration registration;
        synchronized (this.mLock) {
            registration = isServiceControllerAvailable() ? this.mIImsServiceController.getRegistration(slotId) : null;
        }
        return registration;
    }

    public IImsConfig getConfig(int slotId) throws RemoteException {
        IImsConfig config;
        synchronized (this.mLock) {
            config = isServiceControllerAvailable() ? this.mIImsServiceController.getConfig(slotId) : null;
        }
        return config;
    }

    protected void notifyImsServiceReady() throws RemoteException {
        synchronized (this.mLock) {
            if (isServiceControllerAvailable()) {
                Log.d(LOG_TAG, "notifyImsServiceReady");
                this.mIImsServiceController.setListener(this.mFeatureChangedListener);
                this.mIImsServiceController.notifyImsServiceReadyForFeatureCreation();
            }
        }
    }

    protected String getServiceInterface() {
        return "android.telephony.ims.ImsService";
    }

    protected void setServiceController(IBinder serviceController) {
        this.mIImsServiceController = IImsServiceController.Stub.asInterface(serviceController);
    }

    public boolean isBound() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsBound;
        }
        return z;
    }

    protected boolean isServiceControllerAvailable() {
        return this.mIImsServiceController != null;
    }

    @VisibleForTesting
    public void removeImsServiceFeatureCallbacks() {
        this.mImsStatusCallbacks.clear();
    }

    private void startDelayedRebindToService() {
        this.mBackoff.start();
    }

    private void grantPermissionsToService() {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Granting Runtime permissions to:");
        stringBuilder.append(getComponentName());
        Log.i(str, stringBuilder.toString());
        String[] pkgToGrant = new String[]{this.mComponentName.getPackageName()};
        try {
            if (this.mPackageManager != null) {
                this.mPackageManager.grantDefaultPermissionsToEnabledImsServices(pkgToGrant, this.mContext.getUserId());
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Unable to grant permissions, binder died.");
        }
    }

    private void sendImsFeatureCreatedCallback(int slot, int feature) {
        Iterator<IImsServiceFeatureCallback> i = this.mImsStatusCallbacks.iterator();
        while (i.hasNext()) {
            try {
                ((IImsServiceFeatureCallback) i.next()).imsFeatureCreated(slot, feature);
            } catch (RemoteException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendImsFeatureCreatedCallback: Binder died, removing callback. Exception:");
                stringBuilder.append(e.getMessage());
                Log.w(str, stringBuilder.toString());
                i.remove();
            }
        }
    }

    private void sendImsFeatureRemovedCallback(int slot, int feature) {
        Iterator<IImsServiceFeatureCallback> i = this.mImsStatusCallbacks.iterator();
        while (i.hasNext()) {
            try {
                ((IImsServiceFeatureCallback) i.next()).imsFeatureRemoved(slot, feature);
            } catch (RemoteException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendImsFeatureRemovedCallback: Binder died, removing callback. Exception:");
                stringBuilder.append(e.getMessage());
                Log.w(str, stringBuilder.toString());
                i.remove();
            }
        }
    }

    private void sendImsFeatureStatusChanged(int slot, int feature, int status) {
        Iterator<IImsServiceFeatureCallback> i = this.mImsStatusCallbacks.iterator();
        while (i.hasNext()) {
            try {
                ((IImsServiceFeatureCallback) i.next()).imsStatusChanged(slot, feature, status);
            } catch (RemoteException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendImsFeatureStatusChanged: Binder died, removing callback. Exception:");
                stringBuilder.append(e.getMessage());
                Log.w(str, stringBuilder.toString());
                i.remove();
            }
        }
    }

    private void addImsServiceFeature(FeatureSlotPair featurePair) throws RemoteException {
        if (!isServiceControllerAvailable() || this.mCallbacks == null) {
            Log.w(LOG_TAG, "addImsServiceFeature called with null values.");
            return;
        }
        if (featurePair.featureType != 0) {
            ImsFeatureStatusCallback c = new ImsFeatureStatusCallback(featurePair.slotId, featurePair.featureType);
            this.mFeatureStatusCallbacks.add(c);
            addImsFeatureBinder(featurePair.slotId, featurePair.featureType, createImsFeature(featurePair.slotId, featurePair.featureType, c.getCallback()));
            this.mCallbacks.imsServiceFeatureCreated(featurePair.slotId, featurePair.featureType, this);
        } else {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("supports emergency calling on slot ");
            stringBuilder.append(featurePair.slotId);
            Log.i(str, stringBuilder.toString());
        }
        sendImsFeatureCreatedCallback(featurePair.slotId, featurePair.featureType);
    }

    private void removeImsServiceFeature(FeatureSlotPair featurePair) throws RemoteException {
        if (!isServiceControllerAvailable() || this.mCallbacks == null) {
            Log.w(LOG_TAG, "removeImsServiceFeature called with null values.");
            return;
        }
        if (featurePair.featureType != 0) {
            IImsFeatureStatusCallback iImsFeatureStatusCallback = null;
            ImsFeatureStatusCallback callbackToRemove = (ImsFeatureStatusCallback) this.mFeatureStatusCallbacks.stream().filter(new -$$Lambda$ImsServiceController$8NvoVXkZRS5LCradATGpNMBXAqg(featurePair)).findFirst().orElse(null);
            if (callbackToRemove != null) {
                this.mFeatureStatusCallbacks.remove(callbackToRemove);
            }
            int i = featurePair.slotId;
            int i2 = featurePair.featureType;
            if (callbackToRemove != null) {
                iImsFeatureStatusCallback = callbackToRemove.getCallback();
            }
            removeImsFeature(i, i2, iImsFeatureStatusCallback);
            removeImsFeatureBinder(featurePair.slotId, featurePair.featureType);
            this.mCallbacks.imsServiceFeatureRemoved(featurePair.slotId, featurePair.featureType, this);
        } else {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("doesn't support emergency calling on slot ");
            stringBuilder.append(featurePair.slotId);
            Log.i(str, stringBuilder.toString());
        }
        sendImsFeatureRemovedCallback(featurePair.slotId, featurePair.featureType);
    }

    static /* synthetic */ boolean lambda$removeImsServiceFeature$0(FeatureSlotPair featurePair, ImsFeatureStatusCallback c) {
        return c.mSlotId == featurePair.slotId && c.mFeatureType == featurePair.featureType;
    }

    protected IInterface createImsFeature(int slotId, int featureType, IImsFeatureStatusCallback c) throws RemoteException {
        switch (featureType) {
            case 1:
                return this.mIImsServiceController.createMmTelFeature(slotId, c);
            case 2:
                return this.mIImsServiceController.createRcsFeature(slotId, c);
            default:
                return null;
        }
    }

    protected void removeImsFeature(int slotId, int featureType, IImsFeatureStatusCallback c) throws RemoteException {
        this.mIImsServiceController.removeImsFeature(slotId, featureType, c);
    }

    private void addImsFeatureBinder(int slotId, int featureType, IInterface b) {
        this.mImsFeatureBinders.add(new ImsFeatureContainer(slotId, featureType, b));
    }

    private void removeImsFeatureBinder(int slotId, int featureType) {
        ImsFeatureContainer container = (ImsFeatureContainer) this.mImsFeatureBinders.stream().filter(new -$$Lambda$ImsServiceController$rO36xbdAp6IQ5hFqLNNXDJPMers(slotId, featureType)).findFirst().orElse(null);
        if (container != null) {
            this.mImsFeatureBinders.remove(container);
        }
    }

    static /* synthetic */ boolean lambda$removeImsFeatureBinder$1(int slotId, int featureType, ImsFeatureContainer f) {
        return f.slotId == slotId && f.featureType == featureType;
    }

    private ImsFeatureContainer getImsFeatureContainer(int slotId, int featureType) {
        return (ImsFeatureContainer) this.mImsFeatureBinders.stream().filter(new -$$Lambda$ImsServiceController$w3xbtqEhKr7IY81qFuw0e94p84Y(slotId, featureType)).findFirst().orElse(null);
    }

    static /* synthetic */ boolean lambda$getImsFeatureContainer$2(int slotId, int featureType, ImsFeatureContainer f) {
        return f.slotId == slotId && f.featureType == featureType;
    }

    private void notifyAllFeaturesRemoved() {
        if (this.mCallbacks == null) {
            Log.w(LOG_TAG, "notifyAllFeaturesRemoved called with invalid callbacks.");
            return;
        }
        synchronized (this.mLock) {
            Iterator it = this.mImsFeatures.iterator();
            while (it.hasNext()) {
                FeatureSlotPair feature = (FeatureSlotPair) it.next();
                if (feature.featureType != 0) {
                    this.mCallbacks.imsServiceFeatureRemoved(feature.slotId, feature.featureType, this);
                }
                sendImsFeatureRemovedCallback(feature.slotId, feature.featureType);
            }
        }
    }

    private void cleanUpService() {
        synchronized (this.mLock) {
            this.mImsDeathRecipient = null;
            this.mImsServiceConnection = null;
            this.mImsServiceControllerBinder = null;
            setServiceController(null);
        }
    }
}

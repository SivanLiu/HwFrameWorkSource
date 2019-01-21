package com.android.ims;

import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature.Capabilities;
import android.telephony.ims.feature.ImsFeature.CapabilityCallback;
import android.telephony.ims.feature.MmTelFeature.Listener;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.ims.internal.IImsServiceFeatureCallback.Stub;
import com.android.ims.internal.IImsUt;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MmTelFeatureConnection {
    protected static final String TAG = "MmTelFeatureConnection";
    protected IBinder mBinder;
    private final CapabilityCallbackManager mCapabilityCallbackManager = new CapabilityCallbackManager(this, null);
    private IImsConfig mConfigBinder;
    private Context mContext;
    private DeathRecipient mDeathRecipient = new -$$Lambda$MmTelFeatureConnection$ij8S4RNRiQPHfppwkejp36BG78I(this);
    private Integer mFeatureStateCached = null;
    private volatile boolean mIsAvailable = false;
    private final IImsServiceFeatureCallback mListenerBinder = new Stub() {
        /* JADX WARNING: Missing block: B:15:0x0054, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void imsFeatureCreated(int slotId, int feature) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (MmTelFeatureConnection.this.mSlotId == slotId) {
                    switch (feature) {
                        case 0:
                            MmTelFeatureConnection.this.mSupportsEmergencyCalling = true;
                            String str = MmTelFeatureConnection.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Emergency calling enabled on slotId: ");
                            stringBuilder.append(slotId);
                            Log.i(str, stringBuilder.toString());
                            break;
                        case 1:
                            if (!MmTelFeatureConnection.this.mIsAvailable) {
                                String str2 = MmTelFeatureConnection.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("MmTel enabled on slotId: ");
                                stringBuilder2.append(slotId);
                                Log.i(str2, stringBuilder2.toString());
                                MmTelFeatureConnection.this.mIsAvailable = true;
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        /* JADX WARNING: Missing block: B:12:0x004c, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void imsFeatureRemoved(int slotId, int feature) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (MmTelFeatureConnection.this.mSlotId == slotId) {
                    String str;
                    StringBuilder stringBuilder;
                    switch (feature) {
                        case 0:
                            MmTelFeatureConnection.this.mSupportsEmergencyCalling = false;
                            str = MmTelFeatureConnection.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Emergency calling disabled on slotId: ");
                            stringBuilder.append(slotId);
                            Log.i(str, stringBuilder.toString());
                            break;
                        case 1:
                            str = MmTelFeatureConnection.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("MmTel removed on slotId: ");
                            stringBuilder.append(slotId);
                            Log.i(str, stringBuilder.toString());
                            MmTelFeatureConnection.this.onRemovedOrDied();
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        public void imsStatusChanged(int slotId, int feature, int status) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                String str = MmTelFeatureConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("imsStatusChanged: slot: ");
                stringBuilder.append(slotId);
                stringBuilder.append(" feature: ");
                stringBuilder.append(feature);
                stringBuilder.append(" status: ");
                stringBuilder.append(status);
                Log.i(str, stringBuilder.toString());
                if (MmTelFeatureConnection.this.mSlotId == slotId && feature == 1) {
                    MmTelFeatureConnection.this.mFeatureStateCached = Integer.valueOf(status);
                    if (MmTelFeatureConnection.this.mStatusCallback != null) {
                        MmTelFeatureConnection.this.mStatusCallback.notifyStateChanged();
                    }
                }
            }
        }
    };
    private final Object mLock = new Object();
    private IImsRegistration mRegistrationBinder;
    private ImsRegistrationCallbackAdapter mRegistrationCallbackManager = new ImsRegistrationCallbackAdapter(this, null);
    protected final int mSlotId;
    private IFeatureUpdate mStatusCallback;
    private boolean mSupportsEmergencyCalling = false;

    private abstract class CallbackAdapterManager<T> {
        private static final String TAG = "CallbackAdapterManager";
        private boolean mHasConnected;
        protected final Set<T> mLocalCallbacks;

        abstract boolean createConnection() throws RemoteException;

        abstract void removeConnection();

        private CallbackAdapterManager() {
            this.mLocalCallbacks = Collections.newSetFromMap(new ConcurrentHashMap());
            this.mHasConnected = false;
        }

        /* synthetic */ CallbackAdapterManager(MmTelFeatureConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void addCallback(T localCallback) throws RemoteException {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (!this.mHasConnected) {
                    if (createConnection()) {
                        this.mHasConnected = true;
                    } else {
                        throw new RemoteException("Can not create connection!");
                    }
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Local callback added: ");
            stringBuilder.append(localCallback);
            Log.i(str, stringBuilder.toString());
            this.mLocalCallbacks.add(localCallback);
        }

        public void removeCallback(T localCallback) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Local callback removed: ");
            stringBuilder.append(localCallback);
            Log.i(str, stringBuilder.toString());
            this.mLocalCallbacks.remove(localCallback);
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (this.mHasConnected && this.mLocalCallbacks.isEmpty()) {
                    removeConnection();
                    this.mHasConnected = false;
                }
            }
        }

        public void close() {
            synchronized (MmTelFeatureConnection.this.mLock) {
                if (this.mHasConnected) {
                    removeConnection();
                    this.mHasConnected = false;
                }
            }
            Log.i(TAG, "Closing connection and clearing callbacks");
            this.mLocalCallbacks.clear();
        }
    }

    public interface IFeatureUpdate {
        void notifyStateChanged();

        void notifyUnavailable();
    }

    private class CapabilityCallbackManager extends CallbackAdapterManager<CapabilityCallback> {
        private final CapabilityCallbackAdapter mCallbackAdapter;

        private class CapabilityCallbackAdapter extends CapabilityCallback {
            private CapabilityCallbackAdapter() {
            }

            /* synthetic */ CapabilityCallbackAdapter(CapabilityCallbackManager x0, AnonymousClass1 x1) {
                this();
            }

            public void onCapabilitiesStatusChanged(Capabilities config) {
                CapabilityCallbackManager.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$CapabilityCallbackManager$CapabilityCallbackAdapter$Fu_TJxPrz_icRRAcE-hESmVfVRI(config));
            }
        }

        private CapabilityCallbackManager() {
            super(MmTelFeatureConnection.this, null);
            this.mCallbackAdapter = new CapabilityCallbackAdapter(this, null);
        }

        /* synthetic */ CapabilityCallbackManager(MmTelFeatureConnection x0, AnonymousClass1 x1) {
            this();
        }

        boolean createConnection() throws RemoteException {
            IImsMmTelFeature binder;
            synchronized (MmTelFeatureConnection.this.mLock) {
                MmTelFeatureConnection.this.checkServiceIsReady();
                binder = MmTelFeatureConnection.this.getServiceInterface(MmTelFeatureConnection.this.mBinder);
            }
            if (binder != null) {
                binder.addCapabilityCallback(this.mCallbackAdapter);
                return true;
            }
            Log.w(MmTelFeatureConnection.TAG, "create: Couldn't get IImsMmTelFeature binder");
            return false;
        }

        void removeConnection() {
            IImsMmTelFeature binder = null;
            synchronized (MmTelFeatureConnection.this.mLock) {
                try {
                    MmTelFeatureConnection.this.checkServiceIsReady();
                    binder = MmTelFeatureConnection.this.getServiceInterface(MmTelFeatureConnection.this.mBinder);
                } catch (RemoteException e) {
                }
            }
            if (binder != null) {
                try {
                    binder.removeCapabilityCallback(this.mCallbackAdapter);
                    return;
                } catch (RemoteException e2) {
                    Log.w(MmTelFeatureConnection.TAG, "remove: IImsMmTelFeature binder is dead");
                    return;
                }
            }
            Log.w(MmTelFeatureConnection.TAG, "remove: Couldn't get IImsMmTelFeature binder");
        }
    }

    private class ImsRegistrationCallbackAdapter extends CallbackAdapterManager<Callback> {
        private final RegistrationCallbackAdapter mRegistrationCallbackAdapter;

        private class RegistrationCallbackAdapter extends IImsRegistrationCallback.Stub {
            private RegistrationCallbackAdapter() {
            }

            /* synthetic */ RegistrationCallbackAdapter(ImsRegistrationCallbackAdapter x0, AnonymousClass1 x1) {
                this();
            }

            public void onRegistered(int imsRadioTech) {
                Log.i(MmTelFeatureConnection.TAG, "onRegistered ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$K3hccJ541Q6pLDm26Z8TPlTWIJY(imsRadioTech));
            }

            public void onRegistering(int imsRadioTech) {
                Log.i(MmTelFeatureConnection.TAG, "onRegistering ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$u4ZBOw30LePcwafim6pu64v4hNM(imsRadioTech));
            }

            public void onDeregistered(ImsReasonInfo imsReasonInfo) {
                Log.i(MmTelFeatureConnection.TAG, "onDeregistered ::");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$vxFS2t25rwEiTAgHUI462y3Hz90(imsReasonInfo));
            }

            public void onTechnologyChangeFailed(int targetRadioTech, ImsReasonInfo imsReasonInfo) {
                String str = MmTelFeatureConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onTechnologyChangeFailed :: targetAccessTech=");
                stringBuilder.append(targetRadioTech);
                stringBuilder.append(", imsReasonInfo=");
                stringBuilder.append(imsReasonInfo);
                Log.i(str, stringBuilder.toString());
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$MXrzNMmn7kmMT_nTAM0W7J2nTFU(targetRadioTech, imsReasonInfo));
            }

            public void onSubscriberAssociatedUriChanged(Uri[] uris) {
                Log.i(MmTelFeatureConnection.TAG, "onSubscriberAssociatedUriChanged");
                ImsRegistrationCallbackAdapter.this.mLocalCallbacks.forEach(new -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$0vZ6D8L8NEmVenYChls3pkTpxsQ(uris));
            }
        }

        private ImsRegistrationCallbackAdapter() {
            super(MmTelFeatureConnection.this, null);
            this.mRegistrationCallbackAdapter = new RegistrationCallbackAdapter(this, null);
        }

        /* synthetic */ ImsRegistrationCallbackAdapter(MmTelFeatureConnection x0, AnonymousClass1 x1) {
            this();
        }

        boolean createConnection() throws RemoteException {
            if (MmTelFeatureConnection.this.getRegistration() != null) {
                MmTelFeatureConnection.this.getRegistration().addRegistrationCallback(this.mRegistrationCallbackAdapter);
                return true;
            }
            Log.e(MmTelFeatureConnection.TAG, "ImsRegistration is null");
            return false;
        }

        void removeConnection() {
            if (MmTelFeatureConnection.this.getRegistration() != null) {
                try {
                    MmTelFeatureConnection.this.getRegistration().removeRegistrationCallback(this.mRegistrationCallbackAdapter);
                    return;
                } catch (RemoteException e) {
                    Log.w(MmTelFeatureConnection.TAG, "removeConnection: couldn't remove registration callback");
                    return;
                }
            }
            Log.e(MmTelFeatureConnection.TAG, "ImsRegistration is null");
        }
    }

    public static /* synthetic */ void lambda$new$0(MmTelFeatureConnection mmTelFeatureConnection) {
        Log.w(TAG, "DeathRecipient triggered, binder died.");
        mmTelFeatureConnection.onRemovedOrDied();
    }

    public static MmTelFeatureConnection create(Context context, int slotId) {
        MmTelFeatureConnection serviceProxy = new MmTelFeatureConnection(context, slotId);
        TelephonyManager tm = getTelephonyManager(context);
        if (tm == null) {
            Rlog.w(TAG, "create: TelephonyManager is null!");
            return serviceProxy;
        }
        IImsMmTelFeature binder = tm.getImsMmTelFeatureAndListen(slotId, serviceProxy.getListener());
        if (binder != null) {
            serviceProxy.setBinder(binder.asBinder());
            serviceProxy.getFeatureState();
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("create: binder is null! Slot Id: ");
            stringBuilder.append(slotId);
            Rlog.w(str, stringBuilder.toString());
        }
        return serviceProxy;
    }

    public static TelephonyManager getTelephonyManager(Context context) {
        return (TelephonyManager) context.getSystemService("phone");
    }

    public MmTelFeatureConnection(Context context, int slotId) {
        this.mSlotId = slotId;
        this.mContext = context;
    }

    private void onRemovedOrDied() {
        synchronized (this.mLock) {
            if (this.mIsAvailable) {
                this.mIsAvailable = false;
                this.mRegistrationBinder = null;
                this.mConfigBinder = null;
                if (this.mBinder != null) {
                    this.mBinder.unlinkToDeath(this.mDeathRecipient, 0);
                }
                if (this.mStatusCallback != null) {
                    this.mStatusCallback.notifyUnavailable();
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1 = getTelephonyManager(r4.mContext);
     */
    /* JADX WARNING: Missing block: B:10:0x0012, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:11:0x0014, code skipped:
            r0 = r1.getImsRegistration(r4.mSlotId, 1);
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            r2 = r0;
            r3 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:14:0x0020, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:17:0x0023, code skipped:
            if (r4.mRegistrationBinder != null) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:18:0x0025, code skipped:
            r4.mRegistrationBinder = r2;
     */
    /* JADX WARNING: Missing block: B:19:0x0027, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:21:0x002a, code skipped:
            return r4.mRegistrationBinder;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private IImsRegistration getRegistration() {
        synchronized (this.mLock) {
            if (this.mRegistrationBinder != null) {
                IImsRegistration iImsRegistration = this.mRegistrationBinder;
                return iImsRegistration;
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x000c, code skipped:
            r1 = getTelephonyManager(r4.mContext);
     */
    /* JADX WARNING: Missing block: B:10:0x0012, code skipped:
            if (r1 == null) goto L_0x001c;
     */
    /* JADX WARNING: Missing block: B:11:0x0014, code skipped:
            r0 = r1.getImsConfig(r4.mSlotId, 1);
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            r0 = null;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            r2 = r0;
            r3 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:14:0x0020, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:17:0x0023, code skipped:
            if (r4.mConfigBinder != null) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:18:0x0025, code skipped:
            r4.mConfigBinder = r2;
     */
    /* JADX WARNING: Missing block: B:19:0x0027, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:21:0x002a, code skipped:
            return r4.mConfigBinder;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private IImsConfig getConfig() {
        synchronized (this.mLock) {
            if (this.mConfigBinder != null) {
                IImsConfig iImsConfig = this.mConfigBinder;
                return iImsConfig;
            }
        }
    }

    public boolean isEmergencyMmTelAvailable() {
        return this.mSupportsEmergencyCalling;
    }

    public IImsServiceFeatureCallback getListener() {
        return this.mListenerBinder;
    }

    public void setBinder(IBinder binder) {
        synchronized (this.mLock) {
            this.mBinder = binder;
            try {
                if (this.mBinder != null) {
                    this.mBinder.linkToDeath(this.mDeathRecipient, 0);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void openConnection(Listener listener) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setListener(listener);
        }
    }

    public void closeConnection() {
        this.mRegistrationCallbackManager.close();
        this.mCapabilityCallbackManager.close();
        try {
            synchronized (this.mLock) {
                if (isBinderAlive()) {
                    getServiceInterface(this.mBinder).setListener(null);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "closeConnection: couldn't remove listener!");
        }
    }

    public void addRegistrationCallback(Callback callback) throws RemoteException {
        this.mRegistrationCallbackManager.addCallback(callback);
    }

    public void removeRegistrationCallback(Callback callback) throws RemoteException {
        this.mRegistrationCallbackManager.removeCallback(callback);
    }

    public void addCapabilityCallback(CapabilityCallback callback) throws RemoteException {
        this.mCapabilityCallbackManager.addCallback(callback);
    }

    public void removeCapabilityCallback(CapabilityCallback callback) throws RemoteException {
        this.mCapabilityCallbackManager.removeCallback(callback);
    }

    public void changeEnabledCapabilities(CapabilityChangeRequest request, CapabilityCallback callback) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).changeCapabilitiesConfiguration(request, callback);
        }
    }

    public void queryEnabledCapabilities(int capability, int radioTech, CapabilityCallback callback) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).queryCapabilityConfiguration(capability, radioTech, callback);
        }
    }

    public MmTelCapabilities queryCapabilityStatus() throws RemoteException {
        MmTelCapabilities mmTelCapabilities;
        synchronized (this.mLock) {
            checkServiceIsReady();
            mmTelCapabilities = new MmTelCapabilities(getServiceInterface(this.mBinder).queryCapabilityStatus());
        }
        return mmTelCapabilities;
    }

    public ImsCallProfile createCallProfile(int callServiceType, int callType) throws RemoteException {
        ImsCallProfile createCallProfile;
        synchronized (this.mLock) {
            checkServiceIsReady();
            createCallProfile = getServiceInterface(this.mBinder).createCallProfile(callServiceType, callType);
        }
        return createCallProfile;
    }

    public IImsCallSession createCallSession(ImsCallProfile profile) throws RemoteException {
        IImsCallSession createCallSession;
        synchronized (this.mLock) {
            checkServiceIsReady();
            createCallSession = getServiceInterface(this.mBinder).createCallSession(profile);
        }
        return createCallSession;
    }

    public IImsUt getUtInterface() throws RemoteException {
        IImsUt utInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            utInterface = getServiceInterface(this.mBinder).getUtInterface();
        }
        return utInterface;
    }

    public IImsConfig getConfigInterface() throws RemoteException {
        return getConfig();
    }

    public int getRegistrationTech() throws RemoteException {
        IImsRegistration registration = getRegistration();
        if (registration != null) {
            return registration.getRegistrationTechnology();
        }
        return -1;
    }

    public IImsEcbm getEcbmInterface() throws RemoteException {
        IImsEcbm ecbmInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            ecbmInterface = getServiceInterface(this.mBinder).getEcbmInterface();
        }
        return ecbmInterface;
    }

    public void setUiTTYMode(int uiTtyMode, Message onComplete) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setUiTtyMode(uiTtyMode, onComplete);
        }
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        IImsMultiEndpoint multiEndpointInterface;
        synchronized (this.mLock) {
            checkServiceIsReady();
            multiEndpointInterface = getServiceInterface(this.mBinder).getMultiEndpointInterface();
        }
        return multiEndpointInterface;
    }

    public void sendSms(int token, int messageRef, String format, String smsc, boolean isRetry, byte[] pdu) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).sendSms(token, messageRef, format, smsc, isRetry, pdu);
        }
    }

    public void acknowledgeSms(int token, int messageRef, int result) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).acknowledgeSms(token, messageRef, result);
        }
    }

    public void acknowledgeSmsReport(int token, int messageRef, int result) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).acknowledgeSmsReport(token, messageRef, result);
        }
    }

    public String getSmsFormat() throws RemoteException {
        String smsFormat;
        synchronized (this.mLock) {
            checkServiceIsReady();
            smsFormat = getServiceInterface(this.mBinder).getSmsFormat();
        }
        return smsFormat;
    }

    public void onSmsReady() throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).onSmsReady();
        }
    }

    public void setSmsListener(IImsSmsListener listener) throws RemoteException {
        synchronized (this.mLock) {
            checkServiceIsReady();
            getServiceInterface(this.mBinder).setSmsListener(listener);
        }
    }

    public int shouldProcessCall(boolean isEmergency, String[] numbers) throws RemoteException {
        if (!isEmergency || isEmergencyMmTelAvailable()) {
            int shouldProcessCall;
            synchronized (this.mLock) {
                checkServiceIsReady();
                shouldProcessCall = getServiceInterface(this.mBinder).shouldProcessCall(numbers);
            }
            return shouldProcessCall;
        }
        Log.i(TAG, "MmTel does not support emergency over IMS, fallback to CS.");
        return 1;
    }

    /* JADX WARNING: Missing block: B:11:0x0016, code skipped:
            r1 = retrieveFeatureState();
            r2 = r4.mLock;
     */
    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            monitor-enter(r2);
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            if (r1 != null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:17:0x0021, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:20:0x0024, code skipped:
            r4.mFeatureStateCached = r1;
     */
    /* JADX WARNING: Missing block: B:21:0x0026, code skipped:
            monitor-exit(r2);
     */
    /* JADX WARNING: Missing block: B:22:0x0027, code skipped:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("getFeatureState - returning ");
            r2.append(r1);
            android.util.Log.i(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:23:0x0041, code skipped:
            return r1.intValue();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getFeatureState() {
        synchronized (this.mLock) {
            if (!isBinderAlive() || this.mFeatureStateCached == null) {
            } else {
                int intValue = this.mFeatureStateCached.intValue();
                return intValue;
            }
        }
    }

    private Integer retrieveFeatureState() {
        if (this.mBinder != null) {
            try {
                return Integer.valueOf(getServiceInterface(this.mBinder).getFeatureState());
            } catch (RemoteException e) {
            }
        }
        return null;
    }

    public void setStatusCallback(IFeatureUpdate c) {
        this.mStatusCallback = c;
    }

    public boolean isBinderReady() {
        return isBinderAlive() && getFeatureState() == 2;
    }

    public boolean isBinderAlive() {
        return this.mIsAvailable && this.mBinder != null && this.mBinder.isBinderAlive();
    }

    protected void checkServiceIsReady() throws RemoteException {
        if (!isBinderReady()) {
            throw new RemoteException("ImsServiceProxy is not ready to accept commands.");
        }
    }

    private IImsMmTelFeature getServiceInterface(IBinder b) {
        return IImsMmTelFeature.Stub.asInterface(b);
    }

    protected void checkBinderConnection() throws RemoteException {
        if (!isBinderAlive()) {
            throw new RemoteException("ImsServiceProxy is not available for that feature.");
        }
    }
}

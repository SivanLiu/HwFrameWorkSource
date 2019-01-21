package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.net.Uri;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistration.Stub;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SystemApi
public class ImsRegistrationImplBase {
    private static final String LOG_TAG = "ImsRegistrationImplBase";
    private static final int REGISTRATION_STATE_NOT_REGISTERED = 0;
    private static final int REGISTRATION_STATE_REGISTERED = 2;
    private static final int REGISTRATION_STATE_REGISTERING = 1;
    private static final int REGISTRATION_STATE_UNKNOWN = -1;
    public static final int REGISTRATION_TECH_IWLAN = 1;
    public static final int REGISTRATION_TECH_LTE = 0;
    public static final int REGISTRATION_TECH_NONE = -1;
    private final IImsRegistration mBinder = new Stub() {
        public int getRegistrationTechnology() throws RemoteException {
            return ImsRegistrationImplBase.this.getConnectionType();
        }

        public void addRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
            ImsRegistrationImplBase.this.addRegistrationCallback(c);
        }

        public void removeRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
            ImsRegistrationImplBase.this.removeRegistrationCallback(c);
        }
    };
    private final RemoteCallbackList<IImsRegistrationCallback> mCallbacks = new RemoteCallbackList();
    private int mConnectionType = -1;
    private ImsReasonInfo mLastDisconnectCause = new ImsReasonInfo();
    private final Object mLock = new Object();
    private int mRegistrationState = -1;

    public static class Callback {
        public void onRegistered(int imsRadioTech) {
        }

        public void onRegistering(int imsRadioTech) {
        }

        public void onDeregistered(ImsReasonInfo info) {
        }

        public void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) {
        }

        public void onSubscriberAssociatedUriChanged(Uri[] uris) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsRegistrationTech {
    }

    public final IImsRegistration getBinder() {
        return this.mBinder;
    }

    private void addRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
        this.mCallbacks.register(c);
        updateNewCallbackWithState(c);
    }

    private void removeRegistrationCallback(IImsRegistrationCallback c) {
        this.mCallbacks.unregister(c);
    }

    public final void onRegistered(int imsRadioTech) {
        updateToState(imsRadioTech, 2);
        this.mCallbacks.broadcast(new -$$Lambda$ImsRegistrationImplBase$cWwTXSDsk-bWPbsDJYI--DUBMnE(imsRadioTech));
    }

    static /* synthetic */ void lambda$onRegistered$0(int imsRadioTech, IImsRegistrationCallback c) {
        try {
            c.onRegistered(imsRadioTech);
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append(" onRegistrationConnected() - Skipping callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    public final void onRegistering(int imsRadioTech) {
        updateToState(imsRadioTech, 1);
        this.mCallbacks.broadcast(new -$$Lambda$ImsRegistrationImplBase$sbjuTvW-brOSWMR74UInSZEIQB0(imsRadioTech));
    }

    static /* synthetic */ void lambda$onRegistering$1(int imsRadioTech, IImsRegistrationCallback c) {
        try {
            c.onRegistering(imsRadioTech);
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append(" onRegistrationProcessing() - Skipping callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    public final void onDeregistered(ImsReasonInfo info) {
        updateToDisconnectedState(info);
        this.mCallbacks.broadcast(new -$$Lambda$ImsRegistrationImplBase$s7PspXVbCf1Q_WSzodP2glP9TjI(info));
    }

    static /* synthetic */ void lambda$onDeregistered$2(ImsReasonInfo info, IImsRegistrationCallback c) {
        try {
            c.onDeregistered(info);
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append(" onRegistrationDisconnected() - Skipping callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    public final void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) {
        this.mCallbacks.broadcast(new -$$Lambda$ImsRegistrationImplBase$wDtW65cPmn_jF6dfimhBTfdg1kI(imsRadioTech, info));
    }

    static /* synthetic */ void lambda$onTechnologyChangeFailed$3(int imsRadioTech, ImsReasonInfo info, IImsRegistrationCallback c) {
        try {
            c.onTechnologyChangeFailed(imsRadioTech, info);
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append(" onRegistrationChangeFailed() - Skipping callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    public final void onSubscriberAssociatedUriChanged(Uri[] uris) {
        this.mCallbacks.broadcast(new -$$Lambda$ImsRegistrationImplBase$wwtkoeOtGwMjG5I0-ZTfjNpGU-s(uris));
    }

    static /* synthetic */ void lambda$onSubscriberAssociatedUriChanged$4(Uri[] uris, IImsRegistrationCallback c) {
        try {
            c.onSubscriberAssociatedUriChanged(uris);
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(e);
            stringBuilder.append(" onSubscriberAssociatedUriChanged() - Skipping callback.");
            Log.w(str, stringBuilder.toString());
        }
    }

    private void updateToState(int connType, int newState) {
        synchronized (this.mLock) {
            this.mConnectionType = connType;
            this.mRegistrationState = newState;
            this.mLastDisconnectCause = null;
        }
    }

    private void updateToDisconnectedState(ImsReasonInfo info) {
        synchronized (this.mLock) {
            updateToState(-1, 0);
            if (info != null) {
                this.mLastDisconnectCause = info;
            } else {
                Log.w(LOG_TAG, "updateToDisconnectedState: no ImsReasonInfo provided.");
                this.mLastDisconnectCause = new ImsReasonInfo();
            }
        }
    }

    @VisibleForTesting
    public final int getConnectionType() {
        int i;
        synchronized (this.mLock) {
            i = this.mConnectionType;
        }
        return i;
    }

    private void updateNewCallbackWithState(IImsRegistrationCallback c) throws RemoteException {
        int state;
        ImsReasonInfo disconnectInfo;
        synchronized (this.mLock) {
            state = this.mRegistrationState;
            disconnectInfo = this.mLastDisconnectCause;
        }
        switch (state) {
            case 0:
                c.onDeregistered(disconnectInfo);
                return;
            case 1:
                c.onRegistering(getConnectionType());
                return;
            case 2:
                c.onRegistered(getConnectionType());
                return;
            default:
                return;
        }
    }
}

package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.Network;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.hotspot2.OsuNetworkConnection.Callbacks;
import java.net.MalformedURLException;
import java.net.URL;

public class PasspointProvisioner {
    private static final int PROVISIONING_FAILURE = 1;
    private static final int PROVISIONING_STATUS = 0;
    private static final String TAG = "PasspointProvisioner";
    private static final String TLS_VERSION = "TLSv1";
    private int mCallingUid;
    private final Context mContext;
    private int mCurrentSessionId = 0;
    private final PasspointObjectFactory mObjectFactory;
    private final OsuNetworkCallbacks mOsuNetworkCallbacks;
    private final OsuNetworkConnection mOsuNetworkConnection;
    private final OsuServerConnection mOsuServerConnection;
    private final ProvisioningStateMachine mProvisioningStateMachine;
    private boolean mVerboseLoggingEnabled = false;
    private final WfaKeyStore mWfaKeyStore;

    public class OsuServerCallbacks {
        private final int mSessionId;

        OsuServerCallbacks(int sessionId) {
            this.mSessionId = sessionId;
        }

        public int getSessionId() {
            return this.mSessionId;
        }

        public void onServerValidationStatus(int sessionId, boolean succeeded) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                String str = PasspointProvisioner.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OSU Server Validation status=");
                stringBuilder.append(succeeded);
                stringBuilder.append(" sessionId=");
                stringBuilder.append(sessionId);
                Log.v(str, stringBuilder.toString());
            }
            if (succeeded) {
                PasspointProvisioner.this.mProvisioningStateMachine.getHandler().post(new -$$Lambda$PasspointProvisioner$OsuServerCallbacks$gVhGhQxrUva4Q4E9wm9P4Zz5wGA(this, sessionId));
            } else {
                PasspointProvisioner.this.mProvisioningStateMachine.getHandler().post(new -$$Lambda$PasspointProvisioner$OsuServerCallbacks$cVFwoTSKLIu6K3tbngy62AfqCUA(this, sessionId));
            }
        }
    }

    class ProvisioningStateMachine {
        private static final int INITIAL_STATE = 1;
        private static final int OSU_AP_CONNECTED = 3;
        private static final int OSU_PROVIDER_VERIFIED = 6;
        private static final int OSU_SERVER_CONNECTED = 4;
        private static final int OSU_SERVER_VALIDATED = 5;
        private static final String TAG = "ProvisioningStateMachine";
        private static final int WAITING_TO_CONNECT = 2;
        private Handler mHandler;
        private OsuProvider mOsuProvider;
        private IProvisioningCallback mProvisioningCallback;
        private URL mServerUrl;
        private int mState = 1;

        ProvisioningStateMachine() {
        }

        public void start(Handler handler) {
            this.mHandler = handler;
        }

        public Handler getHandler() {
            return this.mHandler;
        }

        public void startProvisioning(OsuProvider provider, IProvisioningCallback callback) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startProvisioning received in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mState != 1) {
                if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "State Machine needs to be reset before starting provisioning");
                }
                resetStateMachine(6);
            }
            if (PasspointProvisioner.this.mOsuServerConnection.canValidateServer()) {
                try {
                    this.mServerUrl = new URL(provider.getServerUri().toString());
                    this.mProvisioningCallback = callback;
                    this.mOsuProvider = provider;
                    PasspointProvisioner.this.mOsuNetworkConnection.setEventCallback(PasspointProvisioner.this.mOsuNetworkCallbacks);
                    PasspointProvisioner.this.mOsuServerConnection.setEventCallback(new OsuServerCallbacks(PasspointProvisioner.access$404(PasspointProvisioner.this)));
                    if (PasspointProvisioner.this.mOsuNetworkConnection.connect(this.mOsuProvider.getOsuSsid(), this.mOsuProvider.getNetworkAccessIdentifier())) {
                        invokeProvisioningCallback(0, 1);
                        changeState(2);
                        return;
                    }
                    resetStateMachine(1);
                    return;
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Invalid Server URL");
                    this.mProvisioningCallback = callback;
                    resetStateMachine(2);
                    return;
                }
            }
            Log.w(TAG, "Provisioning is not possible");
            this.mProvisioningCallback = callback;
            resetStateMachine(7);
        }

        public void handleWifiDisabled() {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Wifi Disabled in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mState == 1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Wifi Disable unhandled in state=");
                stringBuilder.append(this.mState);
                Log.w(str, stringBuilder.toString());
                return;
            }
            resetStateMachine(1);
        }

        public void handleServerValidationFailure(int sessionId) {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Server Validation failure received in ");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (sessionId != PasspointProvisioner.this.mCurrentSessionId) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected server validation callback for currentSessionId=");
                stringBuilder.append(PasspointProvisioner.this.mCurrentSessionId);
                Log.w(str, stringBuilder.toString());
            } else if (this.mState != 4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Server Validation Failure unhandled in mState=");
                stringBuilder.append(this.mState);
                Log.wtf(str, stringBuilder.toString());
            } else {
                resetStateMachine(4);
            }
        }

        public void handleServerValidationSuccess(int sessionId) {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Server Validation Success received in ");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (sessionId != PasspointProvisioner.this.mCurrentSessionId) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Expected server validation callback for currentSessionId=");
                stringBuilder.append(PasspointProvisioner.this.mCurrentSessionId);
                Log.w(str, stringBuilder.toString());
            } else if (this.mState != 4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Server validation success event unhandled in state=");
                stringBuilder.append(this.mState);
                Log.wtf(str, stringBuilder.toString());
            } else {
                changeState(5);
                invokeProvisioningCallback(0, 4);
                validateProvider();
            }
        }

        private void validateProvider() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Validating provider in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (PasspointProvisioner.this.mOsuServerConnection.validateProvider(this.mOsuProvider.getFriendlyName())) {
                changeState(6);
                invokeProvisioningCallback(0, 5);
                return;
            }
            resetStateMachine(5);
        }

        public void handleConnectedEvent(Network network) {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Connected event received in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mState != 2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Connection event unhandled in state=");
                stringBuilder.append(this.mState);
                Log.wtf(str, stringBuilder.toString());
                return;
            }
            invokeProvisioningCallback(0, 2);
            changeState(3);
            initiateServerConnection(network);
        }

        private void initiateServerConnection(Network network) {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Initiating server connection in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mState != 3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Initiating server connection aborted in invalid state=");
                stringBuilder.append(this.mState);
                Log.wtf(str, stringBuilder.toString());
            } else if (PasspointProvisioner.this.mOsuServerConnection.connect(this.mServerUrl, network)) {
                changeState(4);
                invokeProvisioningCallback(0, 3);
            } else {
                resetStateMachine(3);
            }
        }

        public void handleDisconnect() {
            String str;
            StringBuilder stringBuilder;
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Connection failed in state=");
                stringBuilder.append(this.mState);
                Log.v(str, stringBuilder.toString());
            }
            if (this.mState == 1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disconnect event unhandled in state=");
                stringBuilder.append(this.mState);
                Log.w(str, stringBuilder.toString());
                return;
            }
            resetStateMachine(1);
        }

        private void invokeProvisioningCallback(int callbackType, int status) {
            if (this.mProvisioningCallback == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Provisioning callback ");
                stringBuilder.append(callbackType);
                stringBuilder.append(" with status ");
                stringBuilder.append(status);
                stringBuilder.append(" not invoked");
                Log.e(str, stringBuilder.toString());
                return;
            }
            if (callbackType == 0) {
                try {
                    this.mProvisioningCallback.onProvisioningStatus(status);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Remote Exception while posting callback type=");
                    stringBuilder2.append(callbackType);
                    stringBuilder2.append(" status=");
                    stringBuilder2.append(status);
                    Log.e(str2, stringBuilder2.toString());
                }
            } else {
                this.mProvisioningCallback.onProvisioningFailure(status);
            }
        }

        private void changeState(int nextState) {
            if (nextState != this.mState) {
                if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Changing state from ");
                    stringBuilder.append(this.mState);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(nextState);
                    Log.v(str, stringBuilder.toString());
                }
                this.mState = nextState;
            }
        }

        private void resetStateMachine(int failureCode) {
            invokeProvisioningCallback(1, failureCode);
            PasspointProvisioner.this.mOsuNetworkConnection.setEventCallback(null);
            PasspointProvisioner.this.mOsuNetworkConnection.disconnectIfNeeded();
            PasspointProvisioner.this.mOsuServerConnection.setEventCallback(null);
            PasspointProvisioner.this.mOsuServerConnection.cleanup();
            changeState(1);
        }
    }

    class OsuNetworkCallbacks implements Callbacks {
        OsuNetworkCallbacks() {
        }

        public void onConnected(Network network) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                String str = PasspointProvisioner.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onConnected to ");
                stringBuilder.append(network);
                Log.v(str, stringBuilder.toString());
            }
            if (network == null) {
                PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
            } else {
                PasspointProvisioner.this.mProvisioningStateMachine.handleConnectedEvent(network);
            }
        }

        public void onDisconnected() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onDisconnected");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
        }

        public void onTimeOut() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "Timed out waiting for connection to OSU AP");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
        }

        public void onWifiEnabled() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onWifiEnabled");
            }
        }

        public void onWifiDisabled() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onWifiDisabled");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleWifiDisabled();
        }
    }

    static /* synthetic */ int access$404(PasspointProvisioner x0) {
        int i = x0.mCurrentSessionId + 1;
        x0.mCurrentSessionId = i;
        return i;
    }

    PasspointProvisioner(Context context, PasspointObjectFactory objectFactory) {
        this.mContext = context;
        this.mOsuNetworkConnection = objectFactory.makeOsuNetworkConnection(context);
        this.mProvisioningStateMachine = new ProvisioningStateMachine();
        this.mOsuNetworkCallbacks = new OsuNetworkCallbacks();
        this.mOsuServerConnection = objectFactory.makeOsuServerConnection();
        this.mWfaKeyStore = objectFactory.makeWfaKeyStore();
        this.mObjectFactory = objectFactory;
    }

    public void init(Looper looper) {
        this.mProvisioningStateMachine.start(new Handler(looper));
        this.mOsuNetworkConnection.init(this.mProvisioningStateMachine.getHandler());
        this.mProvisioningStateMachine.getHandler().post(new -$$Lambda$PasspointProvisioner$D6b75X8GL55-AmCExPWESj54yLE(this));
    }

    public static /* synthetic */ void lambda$init$0(PasspointProvisioner passpointProvisioner) {
        passpointProvisioner.mWfaKeyStore.load();
        passpointProvisioner.mOsuServerConnection.init(passpointProvisioner.mObjectFactory.getSSLContext(TLS_VERSION), passpointProvisioner.mObjectFactory.getTrustManagerImpl(passpointProvisioner.mWfaKeyStore.get()));
    }

    public void enableVerboseLogging(int level) {
        this.mVerboseLoggingEnabled = level > 0;
        this.mOsuNetworkConnection.enableVerboseLogging(level);
        this.mOsuServerConnection.enableVerboseLogging(level);
    }

    public boolean startSubscriptionProvisioning(int callingUid, OsuProvider provider, IProvisioningCallback callback) {
        this.mCallingUid = callingUid;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Provisioning started with ");
        stringBuilder.append(provider.toString());
        Log.v(str, stringBuilder.toString());
        this.mProvisioningStateMachine.getHandler().post(new -$$Lambda$PasspointProvisioner$GTqDpkw3tIstQq22m_peruc6pA4(this, provider, callback));
        return true;
    }
}

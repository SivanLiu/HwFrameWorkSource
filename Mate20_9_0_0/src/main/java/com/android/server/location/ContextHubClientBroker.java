package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubClient.Stub;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.NanoAppMessage;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextHubClientBroker extends Stub implements DeathRecipient {
    private static final String TAG = "ContextHubClientBroker";
    private final int mAttachedContextHubId;
    private final IContextHubClientCallback mCallbackInterface;
    private final ContextHubClientManager mClientManager;
    private final AtomicBoolean mConnectionOpen = new AtomicBoolean(true);
    private final Context mContext;
    private final IContexthub mContextHubProxy;
    private final short mHostEndPointId;

    ContextHubClientBroker(Context context, IContexthub contextHubProxy, ContextHubClientManager clientManager, int contextHubId, short hostEndPointId, IContextHubClientCallback callback) {
        this.mContext = context;
        this.mContextHubProxy = contextHubProxy;
        this.mClientManager = clientManager;
        this.mAttachedContextHubId = contextHubId;
        this.mHostEndPointId = hostEndPointId;
        this.mCallbackInterface = callback;
    }

    void attachDeathRecipient() throws RemoteException {
        this.mCallbackInterface.asBinder().linkToDeath(this, 0);
    }

    public int sendMessageToNanoApp(NanoAppMessage message) {
        ContextHubServiceUtil.checkPermissions(this.mContext);
        int result = 1;
        if (this.mConnectionOpen.get()) {
            try {
                result = this.mContextHubProxy.sendMessageToHub(this.mAttachedContextHubId, ContextHubServiceUtil.createHidlContextHubMessage(this.mHostEndPointId, message));
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException in sendMessageToNanoApp (target hub ID = ");
                stringBuilder.append(this.mAttachedContextHubId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        } else {
            Log.e(TAG, "Failed to send message to nanoapp: client connection is closed");
        }
        return ContextHubServiceUtil.toTransactionResult(result);
    }

    public void close() {
        if (this.mConnectionOpen.getAndSet(false)) {
            this.mClientManager.unregisterClient(this.mHostEndPointId);
        }
    }

    public void binderDied() {
        try {
            Stub.asInterface(this).close();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while closing client on death", e);
        }
    }

    int getAttachedContextHubId() {
        return this.mAttachedContextHubId;
    }

    short getHostEndPointId() {
        return this.mHostEndPointId;
    }

    void sendMessageToClient(NanoAppMessage message) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onMessageFromNanoApp(message);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while sending message to client (host endpoint ID = ");
                stringBuilder.append(this.mHostEndPointId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    void onNanoAppLoaded(long nanoAppId) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppLoaded(nanoAppId);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while calling onNanoAppLoaded on client (host endpoint ID = ");
                stringBuilder.append(this.mHostEndPointId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    void onNanoAppUnloaded(long nanoAppId) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppUnloaded(nanoAppId);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while calling onNanoAppUnloaded on client (host endpoint ID = ");
                stringBuilder.append(this.mHostEndPointId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    void onHubReset() {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onHubReset();
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while calling onHubReset on client (host endpoint ID = ");
                stringBuilder.append(this.mHostEndPointId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }

    void onNanoAppAborted(long nanoAppId, int abortCode) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppAborted(nanoAppId, abortCode);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException while calling onNanoAppAborted on client (host endpoint ID = ");
                stringBuilder.append(this.mHostEndPointId);
                stringBuilder.append(")");
                Log.e(str, stringBuilder.toString(), e);
            }
        }
    }
}

package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClient.Stub;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.NanoAppMessage;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class ContextHubClientManager {
    private static final boolean DEBUG_LOG_ENABLED = true;
    private static final int MAX_CLIENT_ID = 32767;
    private static final String TAG = "ContextHubClientManager";
    private final Context mContext;
    private final IContexthub mContextHubProxy;
    private final ConcurrentHashMap<Short, ContextHubClientBroker> mHostEndPointIdToClientMap = new ConcurrentHashMap();
    private int mNextHostEndpointId = 0;

    ContextHubClientManager(Context context, IContexthub contextHubProxy) {
        this.mContext = context;
        this.mContextHubProxy = contextHubProxy;
    }

    IContextHubClient registerClient(IContextHubClientCallback clientCallback, int contextHubId) {
        ContextHubClientBroker broker = createNewClientBroker(clientCallback, contextHubId);
        try {
            broker.attachDeathRecipient();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Registered client with host endpoint ID ");
            stringBuilder.append(broker.getHostEndPointId());
            Log.d(str, stringBuilder.toString());
            return Stub.asInterface(broker);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to attach death recipient to client");
            broker.close();
            return null;
        }
    }

    void onMessageFromNanoApp(int contextHubId, ContextHubMsg message) {
        NanoAppMessage clientMessage = ContextHubServiceUtil.createNanoAppMessage(message);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Received ");
        stringBuilder.append(clientMessage);
        Log.v(str, stringBuilder.toString());
        if (clientMessage.isBroadcastMessage()) {
            broadcastMessage(contextHubId, clientMessage);
            return;
        }
        ContextHubClientBroker proxy = (ContextHubClientBroker) this.mHostEndPointIdToClientMap.get(Short.valueOf(message.hostEndPoint));
        if (proxy != null) {
            proxy.sendMessageToClient(clientMessage);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Cannot send message to unregistered client (host endpoint ID = ");
        stringBuilder2.append(message.hostEndPoint);
        stringBuilder2.append(")");
        Log.e(str2, stringBuilder2.toString());
    }

    void unregisterClient(short hostEndPointId) {
        String str;
        StringBuilder stringBuilder;
        if (this.mHostEndPointIdToClientMap.remove(Short.valueOf(hostEndPointId)) != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unregistered client with host endpoint ID ");
            stringBuilder.append(hostEndPointId);
            Log.d(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Cannot unregister non-existing client with host endpoint ID ");
        stringBuilder.append(hostEndPointId);
        Log.e(str, stringBuilder.toString());
    }

    void onNanoAppLoaded(int contextHubId, long nanoAppId) {
        forEachClientOfHub(contextHubId, new -$$Lambda$ContextHubClientManager$VPD5ebhe8Z67S8QKuTR4KzeshK8(nanoAppId));
    }

    void onNanoAppUnloaded(int contextHubId, long nanoAppId) {
        forEachClientOfHub(contextHubId, new -$$Lambda$ContextHubClientManager$gN_vRogwyzr9qBjrQpKwwHzrFAo(nanoAppId));
    }

    void onHubReset(int contextHubId) {
        forEachClientOfHub(contextHubId, -$$Lambda$ContextHubClientManager$aRAV9Gn84ao-4XOiN6tFizfZjHo.INSTANCE);
    }

    void onNanoAppAborted(int contextHubId, long nanoAppId, int abortCode) {
        forEachClientOfHub(contextHubId, new -$$Lambda$ContextHubClientManager$WHzSH2f-YJ3FaiF7JXPP-7oX9EE(nanoAppId, abortCode));
    }

    private synchronized ContextHubClientBroker createNewClientBroker(IContextHubClientCallback clientCallback, int contextHubId) {
        ContextHubClientBroker broker;
        if (this.mHostEndPointIdToClientMap.size() != 32768) {
            broker = null;
            int i = 0;
            int id = this.mNextHostEndpointId;
            int i2 = 0;
            while (i2 <= MAX_CLIENT_ID) {
                if (this.mHostEndPointIdToClientMap.containsKey(Short.valueOf((short) id))) {
                    id = id == MAX_CLIENT_ID ? 0 : id + 1;
                    i2++;
                } else {
                    broker = new ContextHubClientBroker(this.mContext, this.mContextHubProxy, this, contextHubId, (short) id, clientCallback);
                    this.mHostEndPointIdToClientMap.put(Short.valueOf((short) id), broker);
                    if (id != MAX_CLIENT_ID) {
                        i = id + 1;
                    }
                    this.mNextHostEndpointId = i;
                }
            }
        } else {
            throw new IllegalStateException("Could not register client - max limit exceeded");
        }
        return broker;
    }

    private void broadcastMessage(int contextHubId, NanoAppMessage message) {
        forEachClientOfHub(contextHubId, new -$$Lambda$ContextHubClientManager$f15OSYbsSONpkXn7GinnrBPeumw(message));
    }

    private void forEachClientOfHub(int contextHubId, Consumer<ContextHubClientBroker> callback) {
        for (ContextHubClientBroker broker : this.mHostEndPointIdToClientMap.values()) {
            if (broker.getAttachedContextHubId() == contextHubId) {
                callback.accept(broker);
            }
        }
    }
}

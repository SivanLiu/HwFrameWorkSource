package com.android.server.wifi.aware;

import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import libcore.util.HexEncoding;

public class WifiAwareDiscoverySessionState {
    private static final boolean DBG = false;
    private static final String TAG = "WifiAwareDiscSessState";
    private static final boolean VDBG = false;
    private IWifiAwareDiscoverySessionCallback mCallback;
    private final long mCreationTime;
    private boolean mIsPublishSession;
    private int mNextPeerIdToBeAllocated = 100;
    private final SparseArray<PeerInfo> mPeerInfoByRequestorInstanceId = new SparseArray();
    private byte mPubSubId;
    private int mSessionId;
    private final WifiAwareNativeApi mWifiAwareNativeApi;

    static class PeerInfo {
        int mInstanceId;
        byte[] mMac;

        PeerInfo(int instanceId, byte[] mac) {
            this.mInstanceId = instanceId;
            this.mMac = mac;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("instanceId [");
            sb.append(this.mInstanceId).append(", mac=").append(HexEncoding.encode(this.mMac)).append("]");
            return sb.toString();
        }
    }

    public WifiAwareDiscoverySessionState(WifiAwareNativeApi wifiAwareNativeApi, int sessionId, byte pubSubId, IWifiAwareDiscoverySessionCallback callback, boolean isPublishSession, long creationTime) {
        this.mWifiAwareNativeApi = wifiAwareNativeApi;
        this.mSessionId = sessionId;
        this.mPubSubId = pubSubId;
        this.mCallback = callback;
        this.mIsPublishSession = isPublishSession;
        this.mCreationTime = creationTime;
    }

    public int getSessionId() {
        return this.mSessionId;
    }

    public int getPubSubId() {
        return this.mPubSubId;
    }

    public boolean isPublishSession() {
        return this.mIsPublishSession;
    }

    public long getCreationTime() {
        return this.mCreationTime;
    }

    public IWifiAwareDiscoverySessionCallback getCallback() {
        return this.mCallback;
    }

    public PeerInfo getPeerInfo(int peerId) {
        return (PeerInfo) this.mPeerInfoByRequestorInstanceId.get(peerId);
    }

    public void terminate() {
        this.mCallback = null;
        if (this.mIsPublishSession) {
            this.mWifiAwareNativeApi.stopPublish((short) 0, this.mPubSubId);
        } else {
            this.mWifiAwareNativeApi.stopSubscribe((short) 0, this.mPubSubId);
        }
    }

    public boolean isPubSubIdSession(int pubSubId) {
        return this.mPubSubId == pubSubId;
    }

    public boolean updatePublish(short transactionId, PublishConfig config) {
        if (this.mIsPublishSession) {
            boolean success = this.mWifiAwareNativeApi.publish(transactionId, this.mPubSubId, config);
            if (!success) {
                try {
                    this.mCallback.onSessionConfigFail(1);
                } catch (RemoteException e) {
                    Log.w(TAG, "updatePublish onSessionConfigFail(): RemoteException (FYI): " + e);
                }
            }
            return success;
        }
        Log.e(TAG, "A SUBSCRIBE session is being used to publish");
        try {
            this.mCallback.onSessionConfigFail(1);
        } catch (RemoteException e2) {
            Log.e(TAG, "updatePublish: RemoteException=" + e2);
        }
        return false;
    }

    public boolean updateSubscribe(short transactionId, SubscribeConfig config) {
        if (this.mIsPublishSession) {
            Log.e(TAG, "A PUBLISH session is being used to subscribe");
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e) {
                Log.e(TAG, "updateSubscribe: RemoteException=" + e);
            }
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.subscribe(transactionId, this.mPubSubId, config);
        if (!success) {
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e2) {
                Log.w(TAG, "updateSubscribe onSessionConfigFail(): RemoteException (FYI): " + e2);
            }
        }
        return success;
    }

    public boolean sendMessage(short transactionId, int peerId, byte[] message, int messageId) {
        PeerInfo peerInfo = (PeerInfo) this.mPeerInfoByRequestorInstanceId.get(peerId);
        if (peerInfo == null) {
            Log.e(TAG, "sendMessage: attempting to send a message to an address which didn't match/contact us");
            try {
                this.mCallback.onMessageSendFail(messageId, 1);
            } catch (RemoteException e) {
                Log.e(TAG, "sendMessage: RemoteException=" + e);
            }
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.sendMessage(transactionId, this.mPubSubId, peerInfo.mInstanceId, peerInfo.mMac, message, messageId);
        if (success) {
            return success;
        }
        try {
            this.mCallback.onMessageSendFail(messageId, 1);
        } catch (RemoteException e2) {
            Log.e(TAG, "sendMessage: RemoteException=" + e2);
        }
        return false;
    }

    public void onMatch(int requestorInstanceId, byte[] peerMac, byte[] serviceSpecificInfo, byte[] matchFilter) {
        try {
            this.mCallback.onMatch(getPeerIdOrAddIfNew(requestorInstanceId, peerMac), serviceSpecificInfo, matchFilter);
        } catch (RemoteException e) {
            Log.w(TAG, "onMatch: RemoteException (FYI): " + e);
        }
    }

    public void onMessageReceived(int requestorInstanceId, byte[] peerMac, byte[] message) {
        try {
            this.mCallback.onMessageReceived(getPeerIdOrAddIfNew(requestorInstanceId, peerMac), message);
        } catch (RemoteException e) {
            Log.w(TAG, "onMessageReceived: RemoteException (FYI): " + e);
        }
    }

    private int getPeerIdOrAddIfNew(int requestorInstanceId, byte[] peerMac) {
        for (int i = 0; i < this.mPeerInfoByRequestorInstanceId.size(); i++) {
            PeerInfo peerInfo = (PeerInfo) this.mPeerInfoByRequestorInstanceId.valueAt(i);
            if (peerInfo.mInstanceId == requestorInstanceId && Arrays.equals(peerMac, peerInfo.mMac)) {
                return this.mPeerInfoByRequestorInstanceId.keyAt(i);
            }
        }
        int newPeerId = this.mNextPeerIdToBeAllocated;
        this.mNextPeerIdToBeAllocated = newPeerId + 1;
        this.mPeerInfoByRequestorInstanceId.put(newPeerId, new PeerInfo(requestorInstanceId, peerMac));
        return newPeerId;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AwareSessionState:");
        pw.println("  mSessionId: " + this.mSessionId);
        pw.println("  mIsPublishSession: " + this.mIsPublishSession);
        pw.println("  mPubSubId: " + this.mPubSubId);
        pw.println("  mPeerInfoByRequestorInstanceId: [" + this.mPeerInfoByRequestorInstanceId + "]");
    }
}

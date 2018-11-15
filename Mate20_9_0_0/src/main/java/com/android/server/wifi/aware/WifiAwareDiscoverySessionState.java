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
    private static final String TAG = "WifiAwareDiscSessState";
    private static final boolean VDBG = false;
    private static int sNextPeerIdToBeAllocated = 100;
    private IWifiAwareDiscoverySessionCallback mCallback;
    private final long mCreationTime;
    boolean mDbg = false;
    private boolean mIsPublishSession;
    private boolean mIsRangingEnabled;
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
            sb.append(this.mInstanceId);
            sb.append(", mac=");
            sb.append(HexEncoding.encode(this.mMac));
            sb.append("]");
            return sb.toString();
        }
    }

    public WifiAwareDiscoverySessionState(WifiAwareNativeApi wifiAwareNativeApi, int sessionId, byte pubSubId, IWifiAwareDiscoverySessionCallback callback, boolean isPublishSession, boolean isRangingEnabled, long creationTime) {
        this.mWifiAwareNativeApi = wifiAwareNativeApi;
        this.mSessionId = sessionId;
        this.mPubSubId = pubSubId;
        this.mCallback = callback;
        this.mIsPublishSession = isPublishSession;
        this.mIsRangingEnabled = isRangingEnabled;
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

    public boolean isRangingEnabled() {
        return this.mIsRangingEnabled;
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updatePublish onSessionConfigFail(): RemoteException (FYI): ");
                    stringBuilder.append(e);
                    Log.w(str, stringBuilder.toString());
                }
            }
            return success;
        }
        Log.e(TAG, "A SUBSCRIBE session is being used to publish");
        try {
            this.mCallback.onSessionConfigFail(1);
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updatePublish: RemoteException=");
            stringBuilder2.append(e2);
            Log.e(str2, stringBuilder2.toString());
        }
        return false;
    }

    public boolean updateSubscribe(short transactionId, SubscribeConfig config) {
        if (this.mIsPublishSession) {
            Log.e(TAG, "A PUBLISH session is being used to subscribe");
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateSubscribe: RemoteException=");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.subscribe(transactionId, this.mPubSubId, config);
        if (!success) {
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSubscribe onSessionConfigFail(): RemoteException (FYI): ");
                stringBuilder2.append(e2);
                Log.w(str2, stringBuilder2.toString());
            }
        }
        return success;
    }

    public boolean sendMessage(short transactionId, int peerId, byte[] message, int messageId) {
        int i = messageId;
        PeerInfo peerInfo = (PeerInfo) this.mPeerInfoByRequestorInstanceId.get(peerId);
        if (peerInfo == null) {
            Log.e(TAG, "sendMessage: attempting to send a message to an address which didn't match/contact us");
            try {
                this.mCallback.onMessageSendFail(i, 1);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendMessage: RemoteException=");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.sendMessage(transactionId, this.mPubSubId, peerInfo.mInstanceId, peerInfo.mMac, message, i);
        if (success) {
            return success;
        }
        try {
            this.mCallback.onMessageSendFail(i, 1);
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendMessage: RemoteException=");
            stringBuilder2.append(e2);
            Log.e(str2, stringBuilder2.toString());
        }
        return false;
    }

    public void onMatch(int requestorInstanceId, byte[] peerMac, byte[] serviceSpecificInfo, byte[] matchFilter, int rangingIndication, int rangeMm) {
        int peerId = getPeerIdOrAddIfNew(requestorInstanceId, peerMac);
        if (rangingIndication == 0) {
            try {
                this.mCallback.onMatch(peerId, serviceSpecificInfo, matchFilter);
                return;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onMatch: RemoteException (FYI): ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
                return;
            }
        }
        this.mCallback.onMatchWithDistance(peerId, serviceSpecificInfo, matchFilter, rangeMm);
    }

    public void onMessageReceived(int requestorInstanceId, byte[] peerMac, byte[] message) {
        try {
            this.mCallback.onMessageReceived(getPeerIdOrAddIfNew(requestorInstanceId, peerMac), message);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMessageReceived: RemoteException (FYI): ");
            stringBuilder.append(e);
            Log.w(str, stringBuilder.toString());
        }
    }

    private int getPeerIdOrAddIfNew(int requestorInstanceId, byte[] peerMac) {
        int i;
        for (i = 0; i < this.mPeerInfoByRequestorInstanceId.size(); i++) {
            PeerInfo peerInfo = (PeerInfo) this.mPeerInfoByRequestorInstanceId.valueAt(i);
            if (peerInfo.mInstanceId == requestorInstanceId && Arrays.equals(peerMac, peerInfo.mMac)) {
                return this.mPeerInfoByRequestorInstanceId.keyAt(i);
            }
        }
        i = sNextPeerIdToBeAllocated;
        sNextPeerIdToBeAllocated = i + 1;
        this.mPeerInfoByRequestorInstanceId.put(i, new PeerInfo(requestorInstanceId, peerMac));
        return i;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AwareSessionState:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mSessionId: ");
        stringBuilder.append(this.mSessionId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mIsPublishSession: ");
        stringBuilder.append(this.mIsPublishSession);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPubSubId: ");
        stringBuilder.append(this.mPubSubId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPeerInfoByRequestorInstanceId: [");
        stringBuilder.append(this.mPeerInfoByRequestorInstanceId);
        stringBuilder.append("]");
        pw.println(stringBuilder.toString());
    }
}

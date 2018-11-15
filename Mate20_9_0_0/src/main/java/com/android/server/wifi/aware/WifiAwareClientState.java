package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class WifiAwareClientState {
    private static final byte[] ALL_ZERO_MAC = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    static final int CLUSTER_CHANGE_EVENT_JOINED = 1;
    static final int CLUSTER_CHANGE_EVENT_STARTED = 0;
    private static final String TAG = "WifiAwareClientState";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final IWifiAwareEventCallback mCallback;
    private final String mCallingPackage;
    private final int mClientId;
    private ConfigRequest mConfigRequest;
    private final Context mContext;
    private final long mCreationTime;
    boolean mDbg = false;
    private byte[] mLastDiscoveryInterfaceMac = ALL_ZERO_MAC;
    private final boolean mNotifyIdentityChange;
    private final int mPid;
    private final SparseArray<WifiAwareDiscoverySessionState> mSessions = new SparseArray();
    private final int mUid;

    public WifiAwareClientState(Context context, int clientId, int uid, int pid, String callingPackage, IWifiAwareEventCallback callback, ConfigRequest configRequest, boolean notifyIdentityChange, long creationTime) {
        this.mContext = context;
        this.mClientId = clientId;
        this.mUid = uid;
        this.mPid = pid;
        this.mCallingPackage = callingPackage;
        this.mCallback = callback;
        this.mConfigRequest = configRequest;
        this.mNotifyIdentityChange = notifyIdentityChange;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mCreationTime = creationTime;
    }

    public void destroy() {
        for (int i = 0; i < this.mSessions.size(); i++) {
            ((WifiAwareDiscoverySessionState) this.mSessions.valueAt(i)).terminate();
        }
        this.mSessions.clear();
        this.mConfigRequest = null;
    }

    public ConfigRequest getConfigRequest() {
        return this.mConfigRequest;
    }

    public int getClientId() {
        return this.mClientId;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getCallingPackage() {
        return this.mCallingPackage;
    }

    public boolean getNotifyIdentityChange() {
        return this.mNotifyIdentityChange;
    }

    public long getCreationTime() {
        return this.mCreationTime;
    }

    public SparseArray<WifiAwareDiscoverySessionState> getSessions() {
        return this.mSessions;
    }

    public WifiAwareDiscoverySessionState getAwareSessionStateForPubSubId(int pubSubId) {
        for (int i = 0; i < this.mSessions.size(); i++) {
            WifiAwareDiscoverySessionState session = (WifiAwareDiscoverySessionState) this.mSessions.valueAt(i);
            if (session.isPubSubIdSession(pubSubId)) {
                return session;
            }
        }
        return null;
    }

    public void addSession(WifiAwareDiscoverySessionState session) {
        int sessionId = session.getSessionId();
        if (this.mSessions.get(sessionId) != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createSession: sessionId already exists (replaced) - ");
            stringBuilder.append(sessionId);
            Log.w(str, stringBuilder.toString());
        }
        this.mSessions.put(sessionId, session);
    }

    public void removeSession(int sessionId) {
        if (this.mSessions.get(sessionId) == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeSession: sessionId doesn't exist - ");
            stringBuilder.append(sessionId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        this.mSessions.delete(sessionId);
    }

    public WifiAwareDiscoverySessionState terminateSession(int sessionId) {
        WifiAwareDiscoverySessionState session = (WifiAwareDiscoverySessionState) this.mSessions.get(sessionId);
        if (session == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("terminateSession: sessionId doesn't exist - ");
            stringBuilder.append(sessionId);
            Log.e(str, stringBuilder.toString());
            return null;
        }
        session.terminate();
        this.mSessions.delete(sessionId);
        return session;
    }

    public WifiAwareDiscoverySessionState getSession(int sessionId) {
        return (WifiAwareDiscoverySessionState) this.mSessions.get(sessionId);
    }

    public void onInterfaceAddressChange(byte[] mac) {
        if (this.mNotifyIdentityChange && !Arrays.equals(mac, this.mLastDiscoveryInterfaceMac)) {
            try {
                this.mCallback.onIdentityChanged(hasLocationingPermission() ? mac : ALL_ZERO_MAC);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onIdentityChanged: RemoteException - ignored: ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
        }
        this.mLastDiscoveryInterfaceMac = mac;
    }

    public void onClusterChange(int flag, byte[] mac, byte[] currentDiscoveryInterfaceMac) {
        if (this.mNotifyIdentityChange && !Arrays.equals(currentDiscoveryInterfaceMac, this.mLastDiscoveryInterfaceMac)) {
            try {
                byte[] bArr;
                boolean hasPermission = hasLocationingPermission();
                IWifiAwareEventCallback iWifiAwareEventCallback = this.mCallback;
                if (hasPermission) {
                    bArr = currentDiscoveryInterfaceMac;
                } else {
                    bArr = ALL_ZERO_MAC;
                }
                iWifiAwareEventCallback.onIdentityChanged(bArr);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onIdentityChanged: RemoteException - ignored: ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
        }
        this.mLastDiscoveryInterfaceMac = currentDiscoveryInterfaceMac;
    }

    private boolean hasLocationingPermission() {
        if (this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", this.mPid, this.mUid) == 0 && this.mAppOps.noteOp(0, this.mUid, this.mCallingPackage) == 0) {
            return true;
        }
        return false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AwareClientState:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mClientId: ");
        stringBuilder.append(this.mClientId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mConfigRequest: ");
        stringBuilder.append(this.mConfigRequest);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mNotifyIdentityChange: ");
        stringBuilder.append(this.mNotifyIdentityChange);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCallback: ");
        stringBuilder.append(this.mCallback);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mSessions: [");
        stringBuilder.append(this.mSessions);
        stringBuilder.append("]");
        pw.println(stringBuilder.toString());
        for (int i = 0; i < this.mSessions.size(); i++) {
            ((WifiAwareDiscoverySessionState) this.mSessions.valueAt(i)).dump(fd, pw, args);
        }
    }
}

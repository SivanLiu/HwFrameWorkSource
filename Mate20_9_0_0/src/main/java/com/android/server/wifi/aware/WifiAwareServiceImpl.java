package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.ConfigRequest.Builder;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager.Stub;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class WifiAwareServiceImpl extends Stub {
    private static final String TAG = "WifiAwareService";
    private static final boolean VDBG = false;
    private AppOpsManager mAppOps;
    private Context mContext;
    boolean mDbg = false;
    private final SparseArray<DeathRecipient> mDeathRecipientsByClientId = new SparseArray();
    private final Object mLock = new Object();
    private int mNextClientId = 1;
    private WifiAwareShellCommand mShellCommand;
    private WifiAwareStateManager mStateManager;
    private final SparseIntArray mUidByClientId = new SparseIntArray();
    private WifiPermissionsUtil mWifiPermissionsUtil;

    public WifiAwareServiceImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void start(HandlerThread handlerThread, WifiAwareStateManager awareStateManager, WifiAwareShellCommand awareShellCommand, WifiAwareMetrics awareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionsWrapper, FrameworkFacade frameworkFacade, WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi, WifiAwareNativeCallback wifiAwareNativeCallback) {
        FrameworkFacade frameworkFacade2 = frameworkFacade;
        Log.i(TAG, "Starting Wi-Fi Aware service");
        WifiPermissionsUtil wifiPermissionsUtil2 = wifiPermissionsUtil;
        this.mWifiPermissionsUtil = wifiPermissionsUtil2;
        WifiAwareStateManager wifiAwareStateManager = awareStateManager;
        this.mStateManager = wifiAwareStateManager;
        this.mShellCommand = awareShellCommand;
        this.mStateManager.start(this.mContext, handlerThread.getLooper(), awareMetrics, wifiPermissionsUtil2, permissionsWrapper);
        final FrameworkFacade frameworkFacade3 = frameworkFacade2;
        final WifiAwareStateManager wifiAwareStateManager2 = wifiAwareStateManager;
        final WifiAwareNativeManager wifiAwareNativeManager2 = wifiAwareNativeManager;
        final WifiAwareNativeApi wifiAwareNativeApi2 = wifiAwareNativeApi;
        final WifiAwareNativeCallback wifiAwareNativeCallback2 = wifiAwareNativeCallback;
        frameworkFacade2.registerContentObserver(this.mContext, Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(new Handler(handlerThread.getLooper())) {
            public void onChange(boolean selfChange) {
                WifiAwareServiceImpl.this.enableVerboseLogging(frameworkFacade3.getIntegerSetting(WifiAwareServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0), wifiAwareStateManager2, wifiAwareNativeManager2, wifiAwareNativeApi2, wifiAwareNativeCallback2);
            }
        });
        enableVerboseLogging(frameworkFacade2.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0), wifiAwareStateManager, wifiAwareNativeManager, wifiAwareNativeApi, wifiAwareNativeCallback);
    }

    private void enableVerboseLogging(int verbose, WifiAwareStateManager awareStateManager, WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi, WifiAwareNativeCallback wifiAwareNativeCallback) {
        boolean dbg;
        if (verbose > 0) {
            dbg = true;
        } else {
            dbg = false;
        }
        this.mDbg = dbg;
        awareStateManager.mDbg = dbg;
        if (awareStateManager.mDataPathMgr != null) {
            awareStateManager.mDataPathMgr.mDbg = dbg;
            WifiInjector.getInstance().getWifiMetrics().getWifiAwareMetrics().mDbg = dbg;
        }
        wifiAwareNativeCallback.mDbg = dbg;
        wifiAwareNativeManager.mDbg = dbg;
        wifiAwareNativeApi.mDbg = dbg;
    }

    public void startLate() {
        Log.i(TAG, "Late initialization of Wi-Fi Aware service");
        this.mStateManager.startLate();
    }

    public boolean isUsageEnabled() {
        enforceAccessPermission();
        return this.mStateManager.isUsageEnabled();
    }

    public Characteristics getCharacteristics() {
        enforceAccessPermission();
        if (this.mStateManager.getCapabilities() == null) {
            return null;
        }
        return this.mStateManager.getCapabilities().toPublicCharacteristics();
    }

    public void connect(IBinder binder, String callingPackage, IWifiAwareEventCallback callback, ConfigRequest configRequest, boolean notifyOnIdentityChanged) {
        int i;
        final IBinder iBinder = binder;
        String str = callingPackage;
        IWifiAwareEventCallback iWifiAwareEventCallback = callback;
        boolean z = notifyOnIdentityChanged;
        enforceAccessPermission();
        enforceChangePermission();
        DeathRecipient uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, str);
        if (iWifiAwareEventCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (iBinder != null) {
            ConfigRequest configRequest2;
            String str2;
            if (z) {
                enforceLocationPermission(str, getMockableCallingUid());
            }
            if (configRequest != null) {
                enforceNetworkStackPermission();
                configRequest2 = configRequest;
            } else {
                configRequest2 = new Builder().build();
            }
            configRequest2.validate();
            int pid = getCallingPid();
            synchronized (this.mLock) {
                int i2 = this.mNextClientId;
                this.mNextClientId = i2 + 1;
                uid = i2;
            }
            if (this.mDbg) {
                str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("connect: uid=");
                stringBuilder.append(uid);
                stringBuilder.append(", clientId=");
                stringBuilder.append(uid);
                stringBuilder.append(", configRequest");
                stringBuilder.append(configRequest2);
                stringBuilder.append(", notifyOnIdentityChanged=");
                stringBuilder.append(z);
                Log.v(str2, stringBuilder.toString());
            }
            DeathRecipient dr = new DeathRecipient() {
                public void binderDied() {
                    if (WifiAwareServiceImpl.this.mDbg) {
                        String str = WifiAwareServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("binderDied: clientId=");
                        stringBuilder.append(uid);
                        Log.v(str, stringBuilder.toString());
                    }
                    iBinder.unlinkToDeath(this, 0);
                    synchronized (WifiAwareServiceImpl.this.mLock) {
                        WifiAwareServiceImpl.this.mDeathRecipientsByClientId.delete(uid);
                        WifiAwareServiceImpl.this.mUidByClientId.delete(uid);
                    }
                    WifiAwareServiceImpl.this.mStateManager.disconnect(uid);
                }
            };
            try {
                iBinder.linkToDeath(dr, 0);
                synchronized (this.mLock) {
                    try {
                        this.mDeathRecipientsByClientId.put(uid, dr);
                        this.mUidByClientId.put(uid, uid);
                    } finally {
                        DeathRecipient deathRecipient = dr;
                        i = dr;
                        while (true) {
                        }
                    }
                }
                WifiAwareStateManager wifiAwareStateManager = this.mStateManager;
            } catch (RemoteException e) {
                Object obj = dr;
                i = uid;
                RemoteException e2 = e;
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error on linkToDeath - ");
                stringBuilder2.append(e2);
                Log.e(str2, stringBuilder2.toString());
                try {
                    iWifiAwareEventCallback.onConnectFail(1);
                } catch (RemoteException e3) {
                    RemoteException remoteException = e3;
                    Log.e(TAG, "Error on onConnectFail()");
                }
            }
        } else {
            throw new IllegalArgumentException("Binder must not be null");
        }
    }

    public void disconnect(int clientId, IBinder binder) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        enforceClientValidity(uid, clientId);
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("disconnect: uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", clientId=");
            stringBuilder.append(clientId);
            Log.v(str, stringBuilder.toString());
        }
        if (binder != null) {
            synchronized (this.mLock) {
                DeathRecipient dr = (DeathRecipient) this.mDeathRecipientsByClientId.get(clientId);
                if (dr != null) {
                    binder.unlinkToDeath(dr, 0);
                    this.mDeathRecipientsByClientId.delete(clientId);
                }
                this.mUidByClientId.delete(clientId);
            }
            this.mStateManager.disconnect(clientId);
            return;
        }
        throw new IllegalArgumentException("Binder must not be null");
    }

    public void terminateSession(int clientId, int sessionId) {
        enforceAccessPermission();
        enforceChangePermission();
        enforceClientValidity(getMockableCallingUid(), clientId);
        this.mStateManager.terminateSession(clientId, sessionId);
    }

    public void publish(String callingPackage, int clientId, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback callback) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, callingPackage);
        enforceLocationPermission(callingPackage, getMockableCallingUid());
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (publishConfig != null) {
            publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(uid, clientId);
            this.mStateManager.publish(clientId, publishConfig, callback);
        } else {
            throw new IllegalArgumentException("PublishConfig must not be null");
        }
    }

    public void updatePublish(int clientId, int sessionId, PublishConfig publishConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (publishConfig != null) {
            publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.updatePublish(clientId, sessionId, publishConfig);
            return;
        }
        throw new IllegalArgumentException("PublishConfig must not be null");
    }

    public void subscribe(String callingPackage, int clientId, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback callback) {
        enforceAccessPermission();
        enforceChangePermission();
        int uid = getMockableCallingUid();
        this.mAppOps.checkPackage(uid, callingPackage);
        enforceLocationPermission(callingPackage, getMockableCallingUid());
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        } else if (subscribeConfig != null) {
            subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(uid, clientId);
            this.mStateManager.subscribe(clientId, subscribeConfig, callback);
        } else {
            throw new IllegalArgumentException("SubscribeConfig must not be null");
        }
    }

    public void updateSubscribe(int clientId, int sessionId, SubscribeConfig subscribeConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (subscribeConfig != null) {
            subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.updateSubscribe(clientId, sessionId, subscribeConfig);
            return;
        }
        throw new IllegalArgumentException("SubscribeConfig must not be null");
    }

    public void sendMessage(int clientId, int sessionId, int peerId, byte[] message, int messageId, int retryCount) {
        enforceAccessPermission();
        enforceChangePermission();
        if (retryCount != 0) {
            enforceNetworkStackPermission();
        }
        if (message != null && message.length > this.mStateManager.getCharacteristics().getMaxServiceSpecificInfoLength()) {
            throw new IllegalArgumentException("Message length longer than supported by device characteristics");
        } else if (retryCount < 0 || retryCount > DiscoverySession.getMaxSendRetryCount()) {
            throw new IllegalArgumentException("Invalid 'retryCount' must be non-negative and <= DiscoverySession.MAX_SEND_RETRY_COUNT");
        } else {
            enforceClientValidity(getMockableCallingUid(), clientId);
            this.mStateManager.sendMessage(clientId, sessionId, peerId, message, messageId, retryCount);
        }
    }

    public void requestMacAddresses(int uid, List peerIds, IWifiAwareMacAddressProvider callback) {
        enforceNetworkStackPermission();
        this.mStateManager.requestMacAddresses(uid, peerIds, callback);
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        this.mShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: can't dump WifiAwareService from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            pw.println(stringBuilder.toString());
            return;
        }
        pw.println("Wi-Fi Aware Service");
        synchronized (this.mLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mNextClientId: ");
            stringBuilder2.append(this.mNextClientId);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDeathRecipientsByClientId: ");
            stringBuilder2.append(this.mDeathRecipientsByClientId);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mUidByClientId: ");
            stringBuilder2.append(this.mUidByClientId);
            pw.println(stringBuilder2.toString());
        }
        this.mStateManager.dump(fd, pw, args);
    }

    private void enforceClientValidity(int uid, int clientId) {
        synchronized (this.mLock) {
            int uidIndex = this.mUidByClientId.indexOfKey(clientId);
            if (uidIndex < 0 || this.mUidByClientId.valueAt(uidIndex) != uid) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Attempting to use invalid uid+clientId mapping: uid=");
                stringBuilder.append(uid);
                stringBuilder.append(", clientId=");
                stringBuilder.append(clientId);
                throw new SecurityException(stringBuilder.toString());
            }
        }
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationPermission(String callingPackage, int uid) {
        this.mWifiPermissionsUtil.enforceLocationPermission(callingPackage, uid);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }
}

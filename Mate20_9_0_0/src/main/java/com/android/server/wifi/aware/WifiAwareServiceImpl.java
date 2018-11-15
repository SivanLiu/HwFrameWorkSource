package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager.Stub;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
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

    /* renamed from: com.android.server.wifi.aware.WifiAwareServiceImpl$2 */
    class AnonymousClass2 implements DeathRecipient {
        final /* synthetic */ IBinder val$binder;
        final /* synthetic */ int val$clientId;

        AnonymousClass2(int i, IBinder iBinder) {
            this.val$clientId = i;
            this.val$binder = iBinder;
        }

        public void binderDied() {
            if (WifiAwareServiceImpl.this.mDbg) {
                String str = WifiAwareServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("binderDied: clientId=");
                stringBuilder.append(this.val$clientId);
                Log.v(str, stringBuilder.toString());
            }
            this.val$binder.unlinkToDeath(this, 0);
            synchronized (WifiAwareServiceImpl.this.mLock) {
                WifiAwareServiceImpl.this.mDeathRecipientsByClientId.delete(this.val$clientId);
                WifiAwareServiceImpl.this.mUidByClientId.delete(this.val$clientId);
            }
            WifiAwareServiceImpl.this.mStateManager.disconnect(this.val$clientId);
        }
    }

    /*  JADX ERROR: NullPointerException in pass: BlockFinish
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.fixSplitterBlock(BlockFinish.java:45)
        	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public void connect(android.os.IBinder r20, java.lang.String r21, android.net.wifi.aware.IWifiAwareEventCallback r22, android.net.wifi.aware.ConfigRequest r23, boolean r24) {
        /*
        r19 = this;
        r1 = r19;
        r2 = r20;
        r11 = r21;
        r12 = r22;
        r13 = r24;
        r19.enforceAccessPermission();
        r19.enforceChangePermission();
        r14 = r19.getMockableCallingUid();
        r0 = r1.mAppOps;
        r0.checkPackage(r14, r11);
        if (r12 == 0) goto L_0x00e6;
    L_0x001b:
        if (r2 == 0) goto L_0x00de;
    L_0x001d:
        if (r13 == 0) goto L_0x0026;
    L_0x001f:
        r0 = r19.getMockableCallingUid();
        r1.enforceLocationPermission(r11, r0);
    L_0x0026:
        if (r23 == 0) goto L_0x002e;
    L_0x0028:
        r19.enforceNetworkStackPermission();
        r15 = r23;
        goto L_0x0038;
    L_0x002e:
        r3 = new android.net.wifi.aware.ConfigRequest$Builder;
        r3.<init>();
        r0 = r3.build();
        r15 = r0;
    L_0x0038:
        r15.validate();
        r16 = getCallingPid();
        r3 = r1.mLock;
        monitor-enter(r3);
        r0 = r1.mNextClientId;
        r4 = r0 + 1;
        r1.mNextClientId = r4;
        r10 = r0;
        monitor-exit(r3);
        r0 = r1.mDbg;
        if (r0 == 0) goto L_0x007c;
    L_0x004e:
        r0 = "WifiAwareService";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "connect: uid=";
        r3.append(r4);
        r3.append(r14);
        r4 = ", clientId=";
        r3.append(r4);
        r3.append(r10);
        r4 = ", configRequest";
        r3.append(r4);
        r3.append(r15);
        r4 = ", notifyOnIdentityChanged=";
        r3.append(r4);
        r3.append(r13);
        r3 = r3.toString();
        android.util.Log.v(r0, r3);
    L_0x007c:
        r0 = new com.android.server.wifi.aware.WifiAwareServiceImpl$2;
        r0.<init>(r10, r2);
        r9 = r0;
        r0 = 0;
        r2.linkToDeath(r9, r0);	 Catch:{ RemoteException -> 0x00b0 }
        r3 = r1.mLock;
        monitor-enter(r3);
        r0 = r1.mDeathRecipientsByClientId;	 Catch:{ all -> 0x00a7 }
        r0.put(r10, r9);	 Catch:{ all -> 0x00a7 }
        r0 = r1.mUidByClientId;	 Catch:{ all -> 0x00a7 }
        r0.put(r10, r14);	 Catch:{ all -> 0x00a7 }
        monitor-exit(r3);	 Catch:{ all -> 0x00a7 }
        r3 = r1.mStateManager;
        r4 = r10;
        r5 = r14;
        r6 = r16;
        r7 = r11;
        r8 = r12;
        r17 = r9;
        r9 = r15;
        r18 = r10;
        r10 = r13;
        r3.connect(r4, r5, r6, r7, r8, r9, r10);
        return;
    L_0x00a7:
        r0 = move-exception;
        r17 = r9;
        r18 = r10;
    L_0x00ac:
        monitor-exit(r3);	 Catch:{ all -> 0x00ae }
        throw r0;
    L_0x00ae:
        r0 = move-exception;
        goto L_0x00ac;
    L_0x00b0:
        r0 = move-exception;
        r17 = r9;
        r18 = r10;
        r3 = r0;
        r0 = "WifiAwareService";
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Error on linkToDeath - ";
        r4.append(r5);
        r4.append(r3);
        r4 = r4.toString();
        android.util.Log.e(r0, r4);
        r0 = 1;
        r12.onConnectFail(r0);	 Catch:{ RemoteException -> 0x00d1 }
        goto L_0x00da;
    L_0x00d1:
        r0 = move-exception;
        r4 = r0;
        r4 = "WifiAwareService";
        r5 = "Error on onConnectFail()";
        android.util.Log.e(r4, r5);
    L_0x00da:
        return;
    L_0x00db:
        r0 = move-exception;
        monitor-exit(r3);
        throw r0;
    L_0x00de:
        r3 = new java.lang.IllegalArgumentException;
        r4 = "Binder must not be null";
        r3.<init>(r4);
        throw r3;
    L_0x00e6:
        r3 = new java.lang.IllegalArgumentException;
        r4 = "Callback must not be null";
        r3.<init>(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.aware.WifiAwareServiceImpl.connect(android.os.IBinder, java.lang.String, android.net.wifi.aware.IWifiAwareEventCallback, android.net.wifi.aware.ConfigRequest, boolean):void");
    }

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

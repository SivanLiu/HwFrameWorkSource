package com.android.server.devicepolicy;

import android.app.admin.ConnectEvent;
import android.app.admin.DnsEvent;
import android.app.admin.NetworkEvent;
import android.content.pm.PackageManagerInternal;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.net.INetdEventCallback.Stub;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.ServiceThread;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class NetworkLogger {
    private static final String TAG = NetworkLogger.class.getSimpleName();
    private final DevicePolicyManagerService mDpm;
    private ServiceThread mHandlerThread;
    private IIpConnectivityMetrics mIpConnectivityMetrics;
    private final AtomicBoolean mIsLoggingEnabled = new AtomicBoolean(false);
    private final INetdEventCallback mNetdEventCallback = new Stub() {
        public void onDnsEvent(String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
            if (NetworkLogger.this.mIsLoggingEnabled.get()) {
                sendNetworkEvent(new DnsEvent(hostname, ipAddresses, ipAddressesCount, NetworkLogger.this.mPm.getNameForUid(uid), timestamp));
            }
        }

        public void onConnectEvent(String ipAddr, int port, long timestamp, int uid) {
            if (NetworkLogger.this.mIsLoggingEnabled.get()) {
                sendNetworkEvent(new ConnectEvent(ipAddr, port, NetworkLogger.this.mPm.getNameForUid(uid), timestamp));
            }
        }

        private void sendNetworkEvent(NetworkEvent event) {
            Message msg = NetworkLogger.this.mNetworkLoggingHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putParcelable("network_event", event);
            msg.setData(bundle);
            NetworkLogger.this.mNetworkLoggingHandler.sendMessage(msg);
        }
    };
    private NetworkLoggingHandler mNetworkLoggingHandler;
    private final PackageManagerInternal mPm;

    boolean stopNetworkLogging() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x004c in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r4 = this;
        r3 = 1;
        r1 = TAG;
        r2 = "Stopping network logging";
        android.util.Log.d(r1, r2);
        r1 = r4.mIsLoggingEnabled;
        r2 = 0;
        r1.set(r2);
        r4.discardLogs();
        r1 = r4.checkIpConnectivityMetricsService();	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        if (r1 != 0) goto L_0x002a;	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
    L_0x0018:
        r1 = TAG;	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r2 = "Failed to unregister callback with IIpConnectivityMetrics.";	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        android.util.Slog.wtf(r1, r2);	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r1 = r4.mHandlerThread;
        if (r1 == 0) goto L_0x0029;
    L_0x0024:
        r1 = r4.mHandlerThread;
        r1.quitSafely();
    L_0x0029:
        return r3;
    L_0x002a:
        r1 = r4.mIpConnectivityMetrics;	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r1 = r1.unregisterNetdEventCallback();	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r2 = r4.mHandlerThread;
        if (r2 == 0) goto L_0x0039;
    L_0x0034:
        r2 = r4.mHandlerThread;
        r2.quitSafely();
    L_0x0039:
        return r1;
    L_0x003a:
        r0 = move-exception;
        r1 = TAG;	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r2 = "Failed to make remote calls to unregister the callback";	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        android.util.Slog.wtf(r1, r2, r0);	 Catch:{ RemoteException -> 0x003a, all -> 0x004d }
        r1 = r4.mHandlerThread;
        if (r1 == 0) goto L_0x004c;
    L_0x0047:
        r1 = r4.mHandlerThread;
        r1.quitSafely();
    L_0x004c:
        return r3;
    L_0x004d:
        r1 = move-exception;
        r2 = r4.mHandlerThread;
        if (r2 == 0) goto L_0x0057;
    L_0x0052:
        r2 = r4.mHandlerThread;
        r2.quitSafely();
    L_0x0057:
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.NetworkLogger.stopNetworkLogging():boolean");
    }

    NetworkLogger(DevicePolicyManagerService dpm, PackageManagerInternal pm) {
        this.mDpm = dpm;
        this.mPm = pm;
    }

    private boolean checkIpConnectivityMetricsService() {
        if (this.mIpConnectivityMetrics != null) {
            return true;
        }
        IIpConnectivityMetrics service = this.mDpm.mInjector.getIIpConnectivityMetrics();
        if (service == null) {
            return false;
        }
        this.mIpConnectivityMetrics = service;
        return true;
    }

    boolean startNetworkLogging() {
        Log.d(TAG, "Starting network logging.");
        if (checkIpConnectivityMetricsService()) {
            try {
                if (!this.mIpConnectivityMetrics.registerNetdEventCallback(this.mNetdEventCallback)) {
                    return false;
                }
                this.mHandlerThread = new ServiceThread(TAG, 10, false);
                this.mHandlerThread.start();
                this.mNetworkLoggingHandler = new NetworkLoggingHandler(this.mHandlerThread.getLooper(), this.mDpm);
                this.mNetworkLoggingHandler.scheduleBatchFinalization();
                this.mIsLoggingEnabled.set(true);
                return true;
            } catch (RemoteException re) {
                Slog.wtf(TAG, "Failed to make remote calls to register the callback", re);
                return false;
            }
        }
        Slog.wtf(TAG, "Failed to register callback with IIpConnectivityMetrics.");
        return false;
    }

    void pause() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.pause();
        }
    }

    void resume() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.resume();
        }
    }

    void discardLogs() {
        if (this.mNetworkLoggingHandler != null) {
            this.mNetworkLoggingHandler.discardLogs();
        }
    }

    List<NetworkEvent> retrieveLogs(long batchToken) {
        return this.mNetworkLoggingHandler.retrieveFullLogBatch(batchToken);
    }
}

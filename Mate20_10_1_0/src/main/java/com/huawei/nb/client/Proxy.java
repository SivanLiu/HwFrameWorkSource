package com.huawei.nb.client;

import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import com.huawei.nb.client.RemoteServiceConnection;
import com.huawei.nb.client.callback.CallbackManager;
import com.huawei.nb.notification.LocalObservable;
import com.huawei.nb.utils.Waiter;
import com.huawei.nb.utils.logger.DSLog;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Proxy<T> {
    private static final long CONNECT_TIMEOUT = 10000;
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);
    protected final CallbackManager callbackManager;
    protected volatile long callbackTimeout;
    private volatile ServiceConnectCallback connectCallback;
    protected final Context context;
    private final RemoteServiceConnection dsConnection;
    private final int id;
    protected final LocalObservable<?, ?, T> localObservable;
    private final Object mLock;
    protected final String pkgName;
    protected volatile T remote;
    protected final String serviceName;

    /* access modifiers changed from: protected */
    public abstract T asInterface(IBinder iBinder);

    /* access modifiers changed from: protected */
    public abstract LocalObservable<?, ?, T> newLocalObservable();

    protected static class ConnectCallback implements ServiceConnectCallback {
        private final Waiter waiter;

        public ConnectCallback(Waiter waiter2) {
            this.waiter = waiter2;
        }

        @Override // com.huawei.nb.client.ServiceConnectCallback
        public void onConnect() {
            if (this.waiter != null) {
                this.waiter.signal();
            }
        }

        @Override // com.huawei.nb.client.ServiceConnectCallback
        public void onDisconnect() {
            if (this.waiter != null) {
                this.waiter.signal();
            }
        }
    }

    protected Proxy(Context context2, String serviceName2, String action) {
        this(context2, ID_GENERATOR.incrementAndGet(), serviceName2, new RemoteServiceConnection(context2, action));
    }

    protected Proxy(Context context2, String serviceName2, String servicePackageName, String serviceClassName) {
        this(context2, ID_GENERATOR.incrementAndGet(), serviceName2, new RemoteServiceConnection(context2, servicePackageName, serviceClassName));
    }

    protected Proxy(Context context2, int id2, String serviceName2, String servicePackageName, String serviceClassName) {
        this(context2, id2, serviceName2, new RemoteServiceConnection(context2, servicePackageName, serviceClassName));
    }

    private Proxy(Context context2, int id2, String serviceName2, RemoteServiceConnection connection) {
        String str = null;
        this.remote = null;
        this.callbackTimeout = 0;
        this.mLock = new Object();
        this.connectCallback = null;
        this.id = id2;
        this.context = context2;
        this.serviceName = serviceName2;
        this.dsConnection = connection;
        this.pkgName = context2 != null ? context2.getPackageName() : str;
        this.callbackManager = new CallbackManager();
        this.localObservable = newLocalObservable();
        DSLog.init("HwNaturalBaseClient");
    }

    public boolean connect() {
        if (hasConnected()) {
            return true;
        }
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            DSLog.e("Failed to connect to %s, error: connect is invoked in the main thread", this.serviceName);
            return false;
        }
        Waiter waiter = new Waiter();
        if (!connect(new ConnectCallback(waiter))) {
            return false;
        }
        if (waiter.await(CONNECT_TIMEOUT)) {
            return true;
        }
        DSLog.e("Failed to connect to %s in %s ms.", this.serviceName, Long.valueOf((long) CONNECT_TIMEOUT));
        return false;
    }

    public boolean connect(ServiceConnectCallback callback) {
        synchronized (this.mLock) {
            if (this.remote != null) {
                if (callback != null) {
                    invokeCallbackInThread(callback);
                }
                return true;
            }
            this.connectCallback = callback;
            if (this.dsConnection.open(new ConnectionListener())) {
                DSLog.d("Succeed to open connection to %s.", this.serviceName);
                return true;
            }
            DSLog.e("Failed to open connection to %s.", this.serviceName);
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean disconnectInner() {
        synchronized (this.mLock) {
            if (this.remote != null) {
                onDisconnect(true);
                DSLog.w("Connection to %s is closed completely.", this.serviceName);
                this.dsConnection.close();
            } else {
                if (this.localObservable != null) {
                    this.localObservable.stop();
                }
                DSLog.i("Connection to %s has been closed already.", this.serviceName);
            }
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean virtualDisconnectInner() {
        synchronized (this.mLock) {
            if (this.remote != null) {
                onDisconnect(false);
                DSLog.w("Connection to %s is closed virtually.", this.serviceName);
                this.dsConnection.close();
            } else {
                DSLog.i("Connection to %s has been closed already.", this.serviceName);
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void onConnect(IBinder binder) {
        boolean z;
        this.remote = asInterface(binder);
        if (this.localObservable != null) {
            this.localObservable.start(this.remote);
        }
        Object[] objArr = new Object[1];
        if (this.remote != null) {
            z = true;
        } else {
            z = false;
        }
        objArr[0] = Boolean.valueOf(z);
        DSLog.i("Connection got remote is %s", objArr);
        invokeConnectCallback(true);
    }

    /* access modifiers changed from: private */
    public void onDisconnect(boolean clearObservers) {
        this.remote = null;
        invokeConnectCallback(false);
        this.callbackManager.interruptAll();
        if (this.localObservable == null) {
            return;
        }
        if (clearObservers) {
            this.localObservable.stop();
        } else {
            this.localObservable.pause();
        }
    }

    public boolean hasConnected() {
        return this.remote != null;
    }

    public int getId() {
        return this.id;
    }

    public void setExecutionTimeout(long timeout) {
        if (timeout > 0) {
            this.callbackTimeout = timeout;
        }
    }

    private void invokeConnectCallback(boolean connected) {
        if (this.connectCallback == null) {
            DSLog.i("Not process callback: connectCallback is null", new Object[0]);
        } else if (connected) {
            invokeCallbackInThread(this.connectCallback);
        } else {
            this.connectCallback.onDisconnect();
        }
    }

    private class ConnectionListener implements RemoteServiceConnection.OnConnectListener {
        private ConnectionListener() {
        }

        @Override // com.huawei.nb.client.RemoteServiceConnection.OnConnectListener
        public void onConnect(IBinder binder) {
            if (binder != null) {
                Proxy.this.onConnect(binder);
                DSLog.i("Succeed to connect to %s.", Proxy.this.serviceName);
                return;
            }
            DSLog.i("Not process callback: binder is null.", new Object[0]);
        }

        @Override // com.huawei.nb.client.RemoteServiceConnection.OnConnectListener
        public void onDisconnect() {
            Proxy.this.onDisconnect(false);
            DSLog.w("Connection to %s is broken down.", Proxy.this.serviceName);
        }
    }

    private void invokeCallbackInThread(final ServiceConnectCallback callback) {
        new Thread("connectCallback") {
            /* class com.huawei.nb.client.Proxy.AnonymousClass1 */

            public void run() {
                callback.onConnect();
            }
        }.start();
    }
}

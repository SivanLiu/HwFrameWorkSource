package com.android.server.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.tv.ITvRemoteProvider;
import android.media.tv.ITvRemoteServiceInput.Stub;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

final class TvRemoteProviderProxy implements ServiceConnection {
    private static final boolean DEBUG = Log.isLoggable(TAG, 2);
    private static final boolean DEBUG_KEY = false;
    protected static final String SERVICE_INTERFACE = "com.android.media.tv.remoteprovider.TvRemoteProvider";
    private static final String TAG = "TvRemoteProvProxy";
    private Connection mActiveConnection;
    private boolean mBound;
    private final ComponentName mComponentName;
    private boolean mConnectionReady;
    private final Context mContext;
    private final Handler mHandler;
    private final Object mLock = new Object();
    private ProviderMethods mProviderMethods;
    private boolean mRunning;
    private final int mUid;
    private final int mUserId;

    private final class Connection implements DeathRecipient {
        private final RemoteServiceInputProvider mServiceInputProvider = new RemoteServiceInputProvider(this);
        private final ITvRemoteProvider mTvRemoteProvider;

        public Connection(ITvRemoteProvider provider) {
            this.mTvRemoteProvider = provider;
        }

        public boolean register() {
            if (TvRemoteProviderProxy.DEBUG) {
                Slog.d(TvRemoteProviderProxy.TAG, "Connection::register()");
            }
            try {
                this.mTvRemoteProvider.asBinder().linkToDeath(this, 0);
                this.mTvRemoteProvider.setRemoteServiceInputSink(this.mServiceInputProvider);
                TvRemoteProviderProxy.this.mHandler.post(new Runnable() {
                    public void run() {
                        TvRemoteProviderProxy.this.onConnectionReady(Connection.this);
                    }
                });
                return true;
            } catch (RemoteException e) {
                binderDied();
                return false;
            }
        }

        public void dispose() {
            if (TvRemoteProviderProxy.DEBUG) {
                Slog.d(TvRemoteProviderProxy.TAG, "Connection::dispose()");
            }
            this.mTvRemoteProvider.asBinder().unlinkToDeath(this, 0);
            this.mServiceInputProvider.dispose();
        }

        public void onInputBridgeConnected(IBinder token) {
            if (TvRemoteProviderProxy.DEBUG) {
                String str = TvRemoteProviderProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": onInputBridgeConnected");
                Slog.d(str, stringBuilder.toString());
            }
            try {
                this.mTvRemoteProvider.onInputBridgeConnected(token);
            } catch (RemoteException ex) {
                Slog.e(TvRemoteProviderProxy.TAG, "Failed to deliver onInputBridgeConnected. ", ex);
            }
        }

        public void binderDied() {
            TvRemoteProviderProxy.this.mHandler.post(new Runnable() {
                public void run() {
                    TvRemoteProviderProxy.this.onConnectionDied(Connection.this);
                }
            });
        }

        /* JADX WARNING: Missing block: B:34:0x009e, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void openInputBridge(IBinder token, String name, int width, int height, int maxPointers) {
            Throwable th;
            synchronized (TvRemoteProviderProxy.this.mLock) {
                IBinder iBinder;
                String str;
                try {
                    String str2;
                    StringBuilder stringBuilder;
                    if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                        if (TvRemoteProviderProxy.DEBUG) {
                            str2 = TvRemoteProviderProxy.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(this);
                            stringBuilder.append(": openInputBridge, token=");
                            iBinder = token;
                            try {
                                stringBuilder.append(iBinder);
                                stringBuilder.append(", name=");
                                str = name;
                                stringBuilder.append(str);
                                Slog.d(str2, stringBuilder.toString());
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                        iBinder = token;
                        str = name;
                        long idToken = Binder.clearCallingIdentity();
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.openInputBridge(TvRemoteProviderProxy.this, iBinder, str, width, height, maxPointers);
                        }
                        Binder.restoreCallingIdentity(idToken);
                    } else {
                        iBinder = token;
                        str = name;
                        if (TvRemoteProviderProxy.DEBUG) {
                            str2 = TvRemoteProviderProxy.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("openInputBridge, Invalid connection or incorrect uid: ");
                            stringBuilder.append(Binder.getCallingUid());
                            Slog.w(str2, stringBuilder.toString());
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    iBinder = token;
                    str = name;
                    throw th;
                }
            }
        }

        void closeInputBridge(IBinder token) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                String str;
                StringBuilder stringBuilder;
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        str = TvRemoteProviderProxy.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this);
                        stringBuilder.append(": closeInputBridge, token=");
                        stringBuilder.append(token);
                        Slog.d(str, stringBuilder.toString());
                    }
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.closeInputBridge(TvRemoteProviderProxy.this, token);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    str = TvRemoteProviderProxy.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("closeInputBridge, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void clearInputBridge(IBinder token) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                String str;
                StringBuilder stringBuilder;
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    if (TvRemoteProviderProxy.DEBUG) {
                        str = TvRemoteProviderProxy.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this);
                        stringBuilder.append(": clearInputBridge, token=");
                        stringBuilder.append(token);
                        Slog.d(str, stringBuilder.toString());
                    }
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.clearInputBridge(TvRemoteProviderProxy.this, token);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    str = TvRemoteProviderProxy.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("clearInputBridge, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendTimestamp(IBinder token, long timestamp) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendTimeStamp(TvRemoteProviderProxy.this, token, timestamp);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendTimeStamp, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendKeyDown(IBinder token, int keyCode) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendKeyDown(TvRemoteProviderProxy.this, token, keyCode);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendKeyDown, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendKeyUp(IBinder token, int keyCode) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendKeyUp(TvRemoteProviderProxy.this, token, keyCode);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendKeyUp, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendPointerDown(IBinder token, int pointerId, int x, int y) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerDown(TvRemoteProviderProxy.this, token, pointerId, x, y);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendPointerDown, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendPointerUp(IBinder token, int pointerId) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerUp(TvRemoteProviderProxy.this, token, pointerId);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendPointerUp, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }

        void sendPointerSync(IBinder token) {
            synchronized (TvRemoteProviderProxy.this.mLock) {
                if (TvRemoteProviderProxy.this.mActiveConnection == this && Binder.getCallingUid() == TvRemoteProviderProxy.this.mUid) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        if (TvRemoteProviderProxy.this.mProviderMethods != null) {
                            TvRemoteProviderProxy.this.mProviderMethods.sendPointerSync(TvRemoteProviderProxy.this, token);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(idToken);
                    }
                } else if (TvRemoteProviderProxy.DEBUG) {
                    String str = TvRemoteProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sendPointerSync, Invalid connection or incorrect uid: ");
                    stringBuilder.append(Binder.getCallingUid());
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }
    }

    public interface ProviderMethods {
        void clearInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void closeInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void openInputBridge(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, String str, int i, int i2, int i3);

        void sendKeyDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendKeyUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendPointerDown(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i, int i2, int i3);

        void sendPointerSync(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder);

        void sendPointerUp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, int i);

        void sendTimeStamp(TvRemoteProviderProxy tvRemoteProviderProxy, IBinder iBinder, long j);
    }

    private static final class RemoteServiceInputProvider extends Stub {
        private final WeakReference<Connection> mConnectionRef;

        public RemoteServiceInputProvider(Connection connection) {
            this.mConnectionRef = new WeakReference(connection);
        }

        public void dispose() {
            this.mConnectionRef.clear();
        }

        public void openInputBridge(IBinder token, String name, int width, int height, int maxPointers) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.openInputBridge(token, name, width, height, maxPointers);
            }
        }

        public void closeInputBridge(IBinder token) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.closeInputBridge(token);
            }
        }

        public void clearInputBridge(IBinder token) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.clearInputBridge(token);
            }
        }

        public void sendTimestamp(IBinder token, long timestamp) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendTimestamp(token, timestamp);
            }
        }

        public void sendKeyDown(IBinder token, int keyCode) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendKeyDown(token, keyCode);
            }
        }

        public void sendKeyUp(IBinder token, int keyCode) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendKeyUp(token, keyCode);
            }
        }

        public void sendPointerDown(IBinder token, int pointerId, int x, int y) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerDown(token, pointerId, x, y);
            }
        }

        public void sendPointerUp(IBinder token, int pointerId) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerUp(token, pointerId);
            }
        }

        public void sendPointerSync(IBinder token) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.sendPointerSync(token);
            }
        }
    }

    public TvRemoteProviderProxy(Context context, ComponentName componentName, int userId, int uid) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mUserId = userId;
        this.mUid = uid;
        this.mHandler = new Handler();
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Proxy");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mUserId=");
        stringBuilder.append(this.mUserId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mRunning=");
        stringBuilder.append(this.mRunning);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mBound=");
        stringBuilder.append(this.mBound);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mActiveConnection=");
        stringBuilder.append(this.mActiveConnection);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mConnectionReady=");
        stringBuilder.append(this.mConnectionReady);
        pw.println(stringBuilder.toString());
    }

    public void setProviderSink(ProviderMethods provider) {
        this.mProviderMethods = provider;
    }

    public boolean hasComponentName(String packageName, String className) {
        return this.mComponentName.getPackageName().equals(packageName) && this.mComponentName.getClassName().equals(className);
    }

    public void start() {
        if (!this.mRunning) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": Starting");
                Slog.d(str, stringBuilder.toString());
            }
            this.mRunning = true;
            updateBinding();
        }
    }

    public void stop() {
        if (this.mRunning) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": Stopping");
                Slog.d(str, stringBuilder.toString());
            }
            this.mRunning = false;
            updateBinding();
        }
    }

    public void rebindIfDisconnected() {
        synchronized (this.mLock) {
            if (this.mActiveConnection == null && shouldBind()) {
                unbind();
                bind();
            }
        }
    }

    private void updateBinding() {
        if (shouldBind()) {
            bind();
        } else {
            unbind();
        }
    }

    private boolean shouldBind() {
        return this.mRunning;
    }

    private void bind() {
        if (!this.mBound) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": Binding");
                Slog.d(str, stringBuilder.toString());
            }
            Intent service = new Intent(SERVICE_INTERFACE);
            service.setComponent(this.mComponentName);
            try {
                this.mBound = this.mContext.bindServiceAsUser(service, this, 67108865, new UserHandle(this.mUserId));
                if (!this.mBound && DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this);
                    stringBuilder2.append(": Bind failed");
                    Slog.d(str2, stringBuilder2.toString());
                }
            } catch (SecurityException ex) {
                if (DEBUG) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(this);
                    stringBuilder3.append(": Bind failed");
                    Slog.d(str3, stringBuilder3.toString(), ex);
                }
            }
        }
    }

    private void unbind() {
        if (this.mBound) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": Unbinding");
                Slog.d(str, stringBuilder.toString());
            }
            this.mBound = false;
            disconnect();
            this.mContext.unbindService(this);
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this);
            stringBuilder.append(": onServiceConnected()");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mBound) {
            disconnect();
            ITvRemoteProvider provider = ITvRemoteProvider.Stub.asInterface(service);
            if (provider != null) {
                Connection connection = new Connection(provider);
                String str2;
                StringBuilder stringBuilder2;
                if (connection.register()) {
                    synchronized (this.mLock) {
                        this.mActiveConnection = connection;
                    }
                    if (DEBUG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this);
                        stringBuilder2.append(": Connected successfully.");
                        Slog.d(str2, stringBuilder2.toString());
                        return;
                    }
                    return;
                } else if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this);
                    stringBuilder2.append(": Registration failed");
                    Slog.d(str2, stringBuilder2.toString());
                    return;
                } else {
                    return;
                }
            }
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(this);
            stringBuilder3.append(": Service returned invalid remote-control provider binder");
            Slog.e(str3, stringBuilder3.toString());
        }
    }

    public void onServiceDisconnected(ComponentName name) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this);
            stringBuilder.append(": Service disconnected");
            Slog.d(str, stringBuilder.toString());
        }
        disconnect();
    }

    private void onConnectionReady(Connection connection) {
        synchronized (this.mLock) {
            if (DEBUG) {
                Slog.d(TAG, "onConnectionReady");
            }
            if (this.mActiveConnection == connection) {
                if (DEBUG) {
                    Slog.d(TAG, "mConnectionReady = true");
                }
                this.mConnectionReady = true;
            }
        }
    }

    private void onConnectionDied(Connection connection) {
        if (this.mActiveConnection == connection) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": Service connection died");
                Slog.d(str, stringBuilder.toString());
            }
            disconnect();
        }
    }

    private void disconnect() {
        synchronized (this.mLock) {
            if (this.mActiveConnection != null) {
                this.mConnectionReady = false;
                this.mActiveConnection.dispose();
                this.mActiveConnection = null;
            }
        }
    }

    public void inputBridgeConnected(IBinder token) {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": inputBridgeConnected token: ");
                stringBuilder.append(token);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mConnectionReady) {
                this.mActiveConnection.onInputBridgeConnected(token);
            }
        }
    }
}

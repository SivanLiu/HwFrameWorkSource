package com.android.server.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.IRemoteDisplayCallback.Stub;
import android.media.IRemoteDisplayProvider;
import android.media.RemoteDisplayState;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class RemoteDisplayProviderProxy implements ServiceConnection {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "RemoteDisplayProvider";
    private Connection mActiveConnection;
    private boolean mBound;
    private final ComponentName mComponentName;
    private boolean mConnectionReady;
    private final Context mContext;
    private int mDiscoveryMode;
    private RemoteDisplayState mDisplayState;
    private Callback mDisplayStateCallback;
    private final Runnable mDisplayStateChanged = new Runnable() {
        public void run() {
            RemoteDisplayProviderProxy.this.mScheduledDisplayStateChangedCallback = false;
            if (RemoteDisplayProviderProxy.this.mDisplayStateCallback != null) {
                RemoteDisplayProviderProxy.this.mDisplayStateCallback.onDisplayStateChanged(RemoteDisplayProviderProxy.this, RemoteDisplayProviderProxy.this.mDisplayState);
            }
        }
    };
    private final Handler mHandler;
    private boolean mRunning;
    private boolean mScheduledDisplayStateChangedCallback;
    private String mSelectedDisplayId;
    private final int mUserId;

    public interface Callback {
        void onDisplayStateChanged(RemoteDisplayProviderProxy remoteDisplayProviderProxy, RemoteDisplayState remoteDisplayState);
    }

    private final class Connection implements DeathRecipient {
        private final ProviderCallback mCallback = new ProviderCallback(this);
        private final IRemoteDisplayProvider mProvider;

        public Connection(IRemoteDisplayProvider provider) {
            this.mProvider = provider;
        }

        public boolean register() {
            try {
                this.mProvider.asBinder().linkToDeath(this, 0);
                this.mProvider.setCallback(this.mCallback);
                RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                    public void run() {
                        RemoteDisplayProviderProxy.this.onConnectionReady(Connection.this);
                    }
                });
                return true;
            } catch (RemoteException e) {
                binderDied();
                return false;
            }
        }

        public void dispose() {
            this.mProvider.asBinder().unlinkToDeath(this, 0);
            this.mCallback.dispose();
        }

        public void setDiscoveryMode(int mode) {
            try {
                this.mProvider.setDiscoveryMode(mode);
            } catch (RemoteException ex) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to set discovery mode.", ex);
            }
        }

        public void connect(String id) {
            try {
                this.mProvider.connect(id);
            } catch (RemoteException ex) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to connect to display.", ex);
            }
        }

        public void disconnect(String id) {
            try {
                this.mProvider.disconnect(id);
            } catch (RemoteException ex) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to disconnect from display.", ex);
            }
        }

        public void setVolume(String id, int volume) {
            try {
                this.mProvider.setVolume(id, volume);
            } catch (RemoteException ex) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to set display volume.", ex);
            }
        }

        public void adjustVolume(String id, int volume) {
            try {
                this.mProvider.adjustVolume(id, volume);
            } catch (RemoteException ex) {
                Slog.e(RemoteDisplayProviderProxy.TAG, "Failed to deliver request to adjust display volume.", ex);
            }
        }

        public void binderDied() {
            RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                public void run() {
                    RemoteDisplayProviderProxy.this.onConnectionDied(Connection.this);
                }
            });
        }

        void postStateChanged(final RemoteDisplayState state) {
            RemoteDisplayProviderProxy.this.mHandler.post(new Runnable() {
                public void run() {
                    RemoteDisplayProviderProxy.this.onDisplayStateChanged(Connection.this, state);
                }
            });
        }
    }

    private static final class ProviderCallback extends Stub {
        private final WeakReference<Connection> mConnectionRef;

        public ProviderCallback(Connection connection) {
            this.mConnectionRef = new WeakReference(connection);
        }

        public void dispose() {
            this.mConnectionRef.clear();
        }

        public void onStateChanged(RemoteDisplayState state) throws RemoteException {
            Connection connection = (Connection) this.mConnectionRef.get();
            if (connection != null) {
                connection.postStateChanged(state);
            }
        }
    }

    public RemoteDisplayProviderProxy(Context context, ComponentName componentName, int userId) {
        this.mContext = context;
        this.mComponentName = componentName;
        this.mUserId = userId;
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
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mDiscoveryMode=");
        stringBuilder.append(this.mDiscoveryMode);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mSelectedDisplayId=");
        stringBuilder.append(this.mSelectedDisplayId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mDisplayState=");
        stringBuilder.append(this.mDisplayState);
        pw.println(stringBuilder.toString());
    }

    public void setCallback(Callback callback) {
        this.mDisplayStateCallback = callback;
    }

    public RemoteDisplayState getDisplayState() {
        return this.mDisplayState;
    }

    public void setDiscoveryMode(int mode) {
        if (this.mDiscoveryMode != mode) {
            this.mDiscoveryMode = mode;
            if (this.mConnectionReady) {
                this.mActiveConnection.setDiscoveryMode(mode);
            }
            updateBinding();
        }
    }

    public void setSelectedDisplay(String id) {
        if (!Objects.equals(this.mSelectedDisplayId, id)) {
            if (this.mConnectionReady && this.mSelectedDisplayId != null) {
                this.mActiveConnection.disconnect(this.mSelectedDisplayId);
            }
            this.mSelectedDisplayId = id;
            if (this.mConnectionReady && id != null) {
                this.mActiveConnection.connect(id);
            }
            updateBinding();
        }
    }

    public void setDisplayVolume(int volume) {
        if (this.mConnectionReady && this.mSelectedDisplayId != null) {
            this.mActiveConnection.setVolume(this.mSelectedDisplayId, volume);
        }
    }

    public void adjustDisplayVolume(int delta) {
        if (this.mConnectionReady && this.mSelectedDisplayId != null) {
            this.mActiveConnection.adjustVolume(this.mSelectedDisplayId, delta);
        }
    }

    public boolean hasComponentName(String packageName, String className) {
        return this.mComponentName.getPackageName().equals(packageName) && this.mComponentName.getClassName().equals(className);
    }

    public String getFlattenedComponentName() {
        return this.mComponentName.flattenToShortString();
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
        if (this.mActiveConnection == null && shouldBind()) {
            unbind();
            bind();
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
        if (!this.mRunning || (this.mDiscoveryMode == 0 && this.mSelectedDisplayId == null)) {
            return false;
        }
        return true;
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
            Intent service = new Intent("com.android.media.remotedisplay.RemoteDisplayProvider");
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
            stringBuilder.append(": Connected");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mBound) {
            disconnect();
            IRemoteDisplayProvider provider = IRemoteDisplayProvider.Stub.asInterface(service);
            if (provider != null) {
                Connection connection = new Connection(provider);
                if (connection.register()) {
                    this.mActiveConnection = connection;
                    return;
                } else if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
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
            stringBuilder3.append(": Service returned invalid remote display provider binder");
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
        if (this.mActiveConnection == connection) {
            this.mConnectionReady = true;
            if (this.mDiscoveryMode != 0) {
                this.mActiveConnection.setDiscoveryMode(this.mDiscoveryMode);
            }
            if (this.mSelectedDisplayId != null) {
                this.mActiveConnection.connect(this.mSelectedDisplayId);
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

    private void onDisplayStateChanged(Connection connection, RemoteDisplayState state) {
        if (this.mActiveConnection == connection) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this);
                stringBuilder.append(": State changed, state=");
                stringBuilder.append(state);
                Slog.d(str, stringBuilder.toString());
            }
            setDisplayState(state);
        }
    }

    private void disconnect() {
        if (this.mActiveConnection != null) {
            if (this.mSelectedDisplayId != null) {
                this.mActiveConnection.disconnect(this.mSelectedDisplayId);
            }
            this.mConnectionReady = false;
            this.mActiveConnection.dispose();
            this.mActiveConnection = null;
            setDisplayState(null);
        }
    }

    private void setDisplayState(RemoteDisplayState state) {
        if (!Objects.equals(this.mDisplayState, state)) {
            this.mDisplayState = state;
            if (!this.mScheduledDisplayStateChangedCallback) {
                this.mScheduledDisplayStateChangedCallback = true;
                this.mHandler.post(this.mDisplayStateChanged);
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Service connection ");
        stringBuilder.append(this.mComponentName.flattenToShortString());
        return stringBuilder.toString();
    }
}

package android.net.lowpan;

import android.net.IpPrefix;
import android.net.lowpan.ILowpanInterfaceListener.Stub;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

public class LowpanCommissioningSession {
    private final LowpanBeaconInfo mBeaconInfo;
    private final ILowpanInterface mBinder;
    private Callback mCallback = null;
    private Handler mHandler;
    private final ILowpanInterfaceListener mInternalCallback = new InternalCallback();
    private volatile boolean mIsClosed = false;
    private final Looper mLooper;

    public static abstract class Callback {
        public void onReceiveFromCommissioner(byte[] packet) {
        }

        public void onClosed() {
        }
    }

    private class InternalCallback extends Stub {
        private InternalCallback() {
        }

        public void onStateChanged(String value) {
            if (!LowpanCommissioningSession.this.mIsClosed) {
                Object obj = -1;
                int hashCode = value.hashCode();
                if (hashCode != -1548612125) {
                    if (hashCode == 97204770 && value.equals("fault")) {
                        obj = 1;
                    }
                } else if (value.equals("offline")) {
                    obj = null;
                }
                switch (obj) {
                    case null:
                    case 1:
                        synchronized (LowpanCommissioningSession.this) {
                            LowpanCommissioningSession.this.lockedCleanup();
                        }
                        return;
                    default:
                        return;
                }
            }
        }

        public void onReceiveFromCommissioner(byte[] packet) {
            LowpanCommissioningSession.this.mHandler.post(new -$$Lambda$LowpanCommissioningSession$InternalCallback$TrrmDykqIWeXNdgrXO7t2-rqCTo(this, packet));
        }

        public static /* synthetic */ void lambda$onReceiveFromCommissioner$0(InternalCallback internalCallback, byte[] packet) {
            synchronized (LowpanCommissioningSession.this) {
                if (!(LowpanCommissioningSession.this.mIsClosed || LowpanCommissioningSession.this.mCallback == null)) {
                    LowpanCommissioningSession.this.mCallback.onReceiveFromCommissioner(packet);
                }
            }
        }

        public void onEnabledChanged(boolean value) {
        }

        public void onConnectedChanged(boolean value) {
        }

        public void onUpChanged(boolean value) {
        }

        public void onRoleChanged(String value) {
        }

        public void onLowpanIdentityChanged(LowpanIdentity value) {
        }

        public void onLinkNetworkAdded(IpPrefix value) {
        }

        public void onLinkNetworkRemoved(IpPrefix value) {
        }

        public void onLinkAddressAdded(String value) {
        }

        public void onLinkAddressRemoved(String value) {
        }
    }

    LowpanCommissioningSession(ILowpanInterface binder, LowpanBeaconInfo beaconInfo, Looper looper) {
        this.mBinder = binder;
        this.mBeaconInfo = beaconInfo;
        this.mLooper = looper;
        if (this.mLooper != null) {
            this.mHandler = new Handler(this.mLooper);
        } else {
            this.mHandler = new Handler();
        }
        try {
            this.mBinder.addListener(this.mInternalCallback);
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    private void lockedCleanup() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.removeListener(this.mInternalCallback);
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
            if (this.mCallback != null) {
                this.mHandler.post(new -$$Lambda$LowpanCommissioningSession$jqpl-iUq-e7YuWqkG33P8PNe7Ag(this));
            }
        }
        this.mCallback = null;
        this.mIsClosed = true;
    }

    public LowpanBeaconInfo getBeaconInfo() {
        return this.mBeaconInfo;
    }

    public void sendToCommissioner(byte[] packet) {
        if (!this.mIsClosed) {
            try {
                this.mBinder.sendToCommissioner(packet);
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
        }
    }

    public synchronized void setCallback(Callback cb, Handler handler) {
        if (!this.mIsClosed) {
            if (handler != null) {
                this.mHandler = handler;
            } else if (this.mLooper != null) {
                this.mHandler = new Handler(this.mLooper);
            } else {
                this.mHandler = new Handler();
            }
            this.mCallback = cb;
        }
    }

    public synchronized void close() {
        if (!this.mIsClosed) {
            try {
                this.mBinder.closeCommissioningSession();
                lockedCleanup();
            } catch (DeadObjectException e) {
            } catch (RemoteException x) {
                throw x.rethrowAsRuntimeException();
            }
        }
    }
}

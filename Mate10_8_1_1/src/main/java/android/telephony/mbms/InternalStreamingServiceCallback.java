package android.telephony.mbms;

import android.os.Handler;
import android.os.RemoteException;
import android.telephony.mbms.IStreamingServiceCallback.Stub;

public class InternalStreamingServiceCallback extends Stub {
    private final StreamingServiceCallback mAppCallback;
    private final Handler mHandler;
    private volatile boolean mIsStopped = false;

    public InternalStreamingServiceCallback(StreamingServiceCallback appCallback, Handler handler) {
        this.mAppCallback = appCallback;
        this.mHandler = handler;
    }

    public void onError(final int errorCode, final String message) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingServiceCallback.this.mAppCallback.onError(errorCode, message);
                }
            });
        }
    }

    public void onStreamStateUpdated(final int state, final int reason) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingServiceCallback.this.mAppCallback.onStreamStateUpdated(state, reason);
                }
            });
        }
    }

    public void onMediaDescriptionUpdated() throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingServiceCallback.this.mAppCallback.onMediaDescriptionUpdated();
                }
            });
        }
    }

    public void onBroadcastSignalStrengthUpdated(final int signalStrength) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingServiceCallback.this.mAppCallback.onBroadcastSignalStrengthUpdated(signalStrength);
                }
            });
        }
    }

    public void onStreamMethodUpdated(final int methodType) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingServiceCallback.this.mAppCallback.onStreamMethodUpdated(methodType);
                }
            });
        }
    }

    public void stop() {
        this.mIsStopped = true;
    }
}

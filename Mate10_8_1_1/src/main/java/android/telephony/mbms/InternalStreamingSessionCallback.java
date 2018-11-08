package android.telephony.mbms;

import android.os.Handler;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsStreamingSessionCallback.Stub;
import java.util.List;

public class InternalStreamingSessionCallback extends Stub {
    private final MbmsStreamingSessionCallback mAppCallback;
    private final Handler mHandler;
    private volatile boolean mIsStopped = false;

    public InternalStreamingSessionCallback(MbmsStreamingSessionCallback appCallback, Handler handler) {
        this.mAppCallback = appCallback;
        this.mHandler = handler;
    }

    public void onError(final int errorCode, final String message) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingSessionCallback.this.mAppCallback.onError(errorCode, message);
                }
            });
        }
    }

    public void onStreamingServicesUpdated(final List<StreamingServiceInfo> services) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingSessionCallback.this.mAppCallback.onStreamingServicesUpdated(services);
                }
            });
        }
    }

    public void onMiddlewareReady() throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalStreamingSessionCallback.this.mAppCallback.onMiddlewareReady();
                }
            });
        }
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void stop() {
        this.mIsStopped = true;
    }
}

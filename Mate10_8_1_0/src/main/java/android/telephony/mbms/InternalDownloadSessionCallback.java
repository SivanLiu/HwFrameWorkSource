package android.telephony.mbms;

import android.os.Handler;
import android.os.RemoteException;
import android.telephony.mbms.IMbmsDownloadSessionCallback.Stub;
import java.util.List;

public class InternalDownloadSessionCallback extends Stub {
    private final MbmsDownloadSessionCallback mAppCallback;
    private final Handler mHandler;
    private volatile boolean mIsStopped = false;

    public InternalDownloadSessionCallback(MbmsDownloadSessionCallback appCallback, Handler handler) {
        this.mAppCallback = appCallback;
        this.mHandler = handler;
    }

    public void onError(final int errorCode, final String message) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalDownloadSessionCallback.this.mAppCallback.onError(errorCode, message);
                }
            });
        }
    }

    public void onFileServicesUpdated(final List<FileServiceInfo> services) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalDownloadSessionCallback.this.mAppCallback.onFileServicesUpdated(services);
                }
            });
        }
    }

    public void onMiddlewareReady() throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalDownloadSessionCallback.this.mAppCallback.onMiddlewareReady();
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

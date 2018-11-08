package android.telephony.mbms;

import android.os.Handler;
import android.os.RemoteException;
import android.telephony.mbms.IDownloadStateCallback.Stub;

public class InternalDownloadStateCallback extends Stub {
    private final DownloadStateCallback mAppCallback;
    private final Handler mHandler;
    private volatile boolean mIsStopped = false;

    public InternalDownloadStateCallback(DownloadStateCallback appCallback, Handler handler) {
        this.mAppCallback = appCallback;
        this.mHandler = handler;
    }

    public void onProgressUpdated(DownloadRequest request, FileInfo fileInfo, int currentDownloadSize, int fullDownloadSize, int currentDecodedSize, int fullDecodedSize) throws RemoteException {
        if (!this.mIsStopped) {
            final DownloadRequest downloadRequest = request;
            final FileInfo fileInfo2 = fileInfo;
            final int i = currentDownloadSize;
            final int i2 = fullDownloadSize;
            final int i3 = currentDecodedSize;
            final int i4 = fullDecodedSize;
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalDownloadStateCallback.this.mAppCallback.onProgressUpdated(downloadRequest, fileInfo2, i, i2, i3, i4);
                }
            });
        }
    }

    public void onStateUpdated(final DownloadRequest request, final FileInfo fileInfo, final int state) throws RemoteException {
        if (!this.mIsStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    InternalDownloadStateCallback.this.mAppCallback.onStateUpdated(request, fileInfo, state);
                }
            });
        }
    }

    public void stop() {
        this.mIsStopped = true;
    }
}

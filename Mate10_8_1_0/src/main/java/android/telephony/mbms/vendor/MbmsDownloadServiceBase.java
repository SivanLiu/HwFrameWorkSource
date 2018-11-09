package android.telephony.mbms.vendor;

import android.os.Binder;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.DownloadStateCallback;
import android.telephony.mbms.FileInfo;
import android.telephony.mbms.FileServiceInfo;
import android.telephony.mbms.IDownloadStateCallback;
import android.telephony.mbms.IMbmsDownloadSessionCallback;
import android.telephony.mbms.MbmsDownloadSessionCallback;
import android.telephony.mbms.vendor.IMbmsDownloadService.Stub;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MbmsDownloadServiceBase extends Stub {
    private final Map<IBinder, DownloadStateCallback> mDownloadCallbackBinderMap = new HashMap();
    private final Map<IBinder, DeathRecipient> mDownloadCallbackDeathRecipients = new HashMap();

    private static abstract class FilteredDownloadStateCallback extends DownloadStateCallback {
        private final IDownloadStateCallback mCallback;

        protected abstract void onRemoteException(RemoteException remoteException);

        public FilteredDownloadStateCallback(IDownloadStateCallback callback, int callbackFlags) {
            super(callbackFlags);
            this.mCallback = callback;
        }

        public void onProgressUpdated(DownloadRequest request, FileInfo fileInfo, int currentDownloadSize, int fullDownloadSize, int currentDecodedSize, int fullDecodedSize) {
            if (isFilterFlagSet(1)) {
                try {
                    this.mCallback.onProgressUpdated(request, fileInfo, currentDownloadSize, fullDownloadSize, currentDecodedSize, fullDecodedSize);
                } catch (RemoteException e) {
                    onRemoteException(e);
                }
            }
        }

        public void onStateUpdated(DownloadRequest request, FileInfo fileInfo, int state) {
            if (isFilterFlagSet(2)) {
                try {
                    this.mCallback.onStateUpdated(request, fileInfo, state);
                } catch (RemoteException e) {
                    onRemoteException(e);
                }
            }
        }
    }

    public int initialize(int subscriptionId, MbmsDownloadSessionCallback callback) throws RemoteException {
        return 0;
    }

    public final int initialize(final int subscriptionId, final IMbmsDownloadSessionCallback callback) throws RemoteException {
        final int uid = Binder.getCallingUid();
        callback.asBinder().linkToDeath(new DeathRecipient() {
            public void binderDied() {
                MbmsDownloadServiceBase.this.onAppCallbackDied(uid, subscriptionId);
            }
        }, 0);
        return initialize(subscriptionId, new MbmsDownloadSessionCallback() {
            public void onError(int errorCode, String message) {
                try {
                    callback.onError(errorCode, message);
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onFileServicesUpdated(List<FileServiceInfo> services) {
                try {
                    callback.onFileServicesUpdated(services);
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }

            public void onMiddlewareReady() {
                try {
                    callback.onMiddlewareReady();
                } catch (RemoteException e) {
                    MbmsDownloadServiceBase.this.onAppCallbackDied(uid, subscriptionId);
                }
            }
        });
    }

    public int requestUpdateFileServices(int subscriptionId, List<String> list) throws RemoteException {
        return 0;
    }

    public int setTempFileRootDirectory(int subscriptionId, String rootDirectoryPath) throws RemoteException {
        return 0;
    }

    public int download(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    public int registerStateCallback(DownloadRequest downloadRequest, DownloadStateCallback callback) throws RemoteException {
        return 0;
    }

    public final int registerStateCallback(final DownloadRequest downloadRequest, final IDownloadStateCallback callback, int flags) throws RemoteException {
        final int uid = Binder.getCallingUid();
        DeathRecipient deathRecipient = new DeathRecipient() {
            public void binderDied() {
                MbmsDownloadServiceBase.this.onAppCallbackDied(uid, downloadRequest.getSubscriptionId());
                MbmsDownloadServiceBase.this.mDownloadCallbackBinderMap.remove(callback.asBinder());
                MbmsDownloadServiceBase.this.mDownloadCallbackDeathRecipients.remove(callback.asBinder());
            }
        };
        this.mDownloadCallbackDeathRecipients.put(callback.asBinder(), deathRecipient);
        callback.asBinder().linkToDeath(deathRecipient, 0);
        final DownloadRequest downloadRequest2 = downloadRequest;
        DownloadStateCallback exposedCallback = new FilteredDownloadStateCallback(callback, flags) {
            protected void onRemoteException(RemoteException e) {
                MbmsDownloadServiceBase.this.onAppCallbackDied(uid, downloadRequest2.getSubscriptionId());
            }
        };
        this.mDownloadCallbackBinderMap.put(callback.asBinder(), exposedCallback);
        return registerStateCallback(downloadRequest, exposedCallback);
    }

    public int unregisterStateCallback(DownloadRequest downloadRequest, DownloadStateCallback callback) throws RemoteException {
        return 0;
    }

    public final int unregisterStateCallback(DownloadRequest downloadRequest, IDownloadStateCallback callback) throws RemoteException {
        DeathRecipient deathRecipient = (DeathRecipient) this.mDownloadCallbackDeathRecipients.remove(callback.asBinder());
        if (deathRecipient == null) {
            throw new IllegalArgumentException("Unknown callback");
        }
        callback.asBinder().unlinkToDeath(deathRecipient, 0);
        DownloadStateCallback exposedCallback = (DownloadStateCallback) this.mDownloadCallbackBinderMap.remove(callback.asBinder());
        if (exposedCallback != null) {
            return unregisterStateCallback(downloadRequest, exposedCallback);
        }
        throw new IllegalArgumentException("Unknown callback");
    }

    public List<DownloadRequest> listPendingDownloads(int subscriptionId) throws RemoteException {
        return null;
    }

    public int cancelDownload(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    public int getDownloadStatus(DownloadRequest downloadRequest, FileInfo fileInfo) throws RemoteException {
        return 0;
    }

    public int resetDownloadKnowledge(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    public void dispose(int subscriptionId) throws RemoteException {
    }

    public void onAppCallbackDied(int uid, int subscriptionId) {
    }
}

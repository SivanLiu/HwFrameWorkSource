package android.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.DownloadStateCallback;
import android.telephony.mbms.FileInfo;
import android.telephony.mbms.InternalDownloadSessionCallback;
import android.telephony.mbms.InternalDownloadStateCallback;
import android.telephony.mbms.MbmsDownloadReceiver;
import android.telephony.mbms.MbmsDownloadSessionCallback;
import android.telephony.mbms.MbmsTempFileProvider;
import android.telephony.mbms.MbmsUtils;
import android.telephony.mbms.vendor.IMbmsDownloadService;
import android.telephony.mbms.vendor.IMbmsDownloadService.Stub;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MbmsDownloadSession implements AutoCloseable {
    public static final String DEFAULT_TOP_LEVEL_TEMP_DIRECTORY = "androidMbmsTempFileRoot";
    public static final String EXTRA_MBMS_COMPLETED_FILE_URI = "android.telephony.extra.MBMS_COMPLETED_FILE_URI";
    public static final String EXTRA_MBMS_DOWNLOAD_REQUEST = "android.telephony.extra.MBMS_DOWNLOAD_REQUEST";
    public static final String EXTRA_MBMS_DOWNLOAD_RESULT = "android.telephony.extra.MBMS_DOWNLOAD_RESULT";
    public static final String EXTRA_MBMS_FILE_INFO = "android.telephony.extra.MBMS_FILE_INFO";
    private static final String LOG_TAG = MbmsDownloadSession.class.getSimpleName();
    public static final String MBMS_DOWNLOAD_SERVICE_ACTION = "android.telephony.action.EmbmsDownload";
    public static final int RESULT_CANCELLED = 2;
    public static final int RESULT_EXPIRED = 3;
    public static final int RESULT_IO_ERROR = 4;
    public static final int RESULT_SUCCESSFUL = 1;
    public static final int STATUS_ACTIVELY_DOWNLOADING = 1;
    public static final int STATUS_PENDING_DOWNLOAD = 2;
    public static final int STATUS_PENDING_DOWNLOAD_WINDOW = 4;
    public static final int STATUS_PENDING_REPAIR = 3;
    public static final int STATUS_UNKNOWN = 0;
    private static AtomicBoolean sIsInitialized = new AtomicBoolean(false);
    private final Context mContext;
    private DeathRecipient mDeathRecipient = new DeathRecipient() {
        public void binderDied() {
            MbmsDownloadSession.this.sendErrorToApp(3, "Received death notification");
        }
    };
    private final InternalDownloadSessionCallback mInternalCallback;
    private final Map<DownloadStateCallback, InternalDownloadStateCallback> mInternalDownloadCallbacks = new HashMap();
    private AtomicReference<IMbmsDownloadService> mService = new AtomicReference(null);
    private int mSubscriptionId = -1;

    private MbmsDownloadSession(Context context, MbmsDownloadSessionCallback callback, int subscriptionId, Handler handler) {
        this.mContext = context;
        this.mSubscriptionId = subscriptionId;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        this.mInternalCallback = new InternalDownloadSessionCallback(callback, handler);
    }

    public static MbmsDownloadSession create(Context context, MbmsDownloadSessionCallback callback, Handler handler) {
        return create(context, callback, SubscriptionManager.getDefaultSubscriptionId(), handler);
    }

    public static MbmsDownloadSession create(Context context, final MbmsDownloadSessionCallback callback, int subscriptionId, Handler handler) {
        if (sIsInitialized.compareAndSet(false, true)) {
            MbmsDownloadSession session = new MbmsDownloadSession(context, callback, subscriptionId, handler);
            final int result = session.bindAndInitialize();
            if (result == 0) {
                return session;
            }
            sIsInitialized.set(false);
            handler.post(new Runnable() {
                public void run() {
                    callback.onError(result, null);
                }
            });
            return null;
        }
        throw new IllegalStateException("Cannot have two active instances");
    }

    private int bindAndInitialize() {
        return MbmsUtils.startBinding(this.mContext, MBMS_DOWNLOAD_SERVICE_ACTION, new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                IMbmsDownloadService downloadService = Stub.asInterface(service);
                try {
                    int result = downloadService.initialize(MbmsDownloadSession.this.mSubscriptionId, MbmsDownloadSession.this.mInternalCallback);
                    if (result != 0) {
                        MbmsDownloadSession.this.sendErrorToApp(result, "Error returned during initialization");
                        MbmsDownloadSession.sIsInitialized.set(false);
                        return;
                    }
                    try {
                        downloadService.asBinder().linkToDeath(MbmsDownloadSession.this.mDeathRecipient, 0);
                        MbmsDownloadSession.this.mService.set(downloadService);
                    } catch (RemoteException e) {
                        MbmsDownloadSession.this.sendErrorToApp(3, "Middleware lost during initialization");
                        MbmsDownloadSession.sIsInitialized.set(false);
                    }
                } catch (RemoteException e2) {
                    Log.e(MbmsDownloadSession.LOG_TAG, "Service died before initialization");
                    MbmsDownloadSession.sIsInitialized.set(false);
                } catch (RuntimeException e3) {
                    Log.e(MbmsDownloadSession.LOG_TAG, "Runtime exception during initialization");
                    MbmsDownloadSession.this.sendErrorToApp(103, e3.toString());
                    MbmsDownloadSession.sIsInitialized.set(false);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                MbmsDownloadSession.sIsInitialized.set(false);
                MbmsDownloadSession.this.mService.set(null);
            }
        });
    }

    public void requestUpdateFileServices(List<String> classList) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            int returnCode = downloadService.requestUpdateFileServices(this.mSubscriptionId, classList);
            if (returnCode != 0) {
                sendErrorToApp(returnCode, null);
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Remote process died");
            this.mService.set(null);
            sendErrorToApp(3, null);
        }
    }

    public void setTempFileRootDirectory(File tempFileRootDirectory) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            validateTempFileRootSanity(tempFileRootDirectory);
            try {
                String filePath = tempFileRootDirectory.getCanonicalPath();
                try {
                    int result = downloadService.setTempFileRootDirectory(this.mSubscriptionId, filePath);
                    if (result != 0) {
                        sendErrorToApp(result, null);
                    }
                    this.mContext.getSharedPreferences(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_FILE_NAME, 0).edit().putString(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_NAME, filePath).apply();
                } catch (RemoteException e) {
                    this.mService.set(null);
                    sendErrorToApp(3, null);
                }
            } catch (IOException e2) {
                throw new IllegalArgumentException("Unable to canonicalize the provided path: " + e2);
            }
        } catch (IOException e3) {
            throw new IllegalStateException("Got IOException checking directory sanity");
        }
    }

    private void validateTempFileRootSanity(File tempFileRootDirectory) throws IOException {
        if (!tempFileRootDirectory.exists()) {
            throw new IllegalArgumentException("Provided directory does not exist");
        } else if (tempFileRootDirectory.isDirectory()) {
            String canonicalTempFilePath = tempFileRootDirectory.getCanonicalPath();
            if (this.mContext.getDataDir().getCanonicalPath().equals(canonicalTempFilePath)) {
                throw new IllegalArgumentException("Temp file root cannot be your data dir");
            } else if (this.mContext.getCacheDir().getCanonicalPath().equals(canonicalTempFilePath)) {
                throw new IllegalArgumentException("Temp file root cannot be your cache dir");
            } else if (this.mContext.getFilesDir().getCanonicalPath().equals(canonicalTempFilePath)) {
                throw new IllegalArgumentException("Temp file root cannot be your files dir");
            }
        } else {
            throw new IllegalArgumentException("Provided File is not a directory");
        }
    }

    public File getTempFileRootDirectory() {
        String path = this.mContext.getSharedPreferences(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_FILE_NAME, 0).getString(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_NAME, null);
        if (path != null) {
            return new File(path);
        }
        return null;
    }

    public void download(DownloadRequest request) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        if (this.mContext.getSharedPreferences(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_FILE_NAME, 0).getString(MbmsTempFileProvider.TEMP_FILE_ROOT_PREF_NAME, null) == null) {
            File tempRootDirectory = new File(this.mContext.getFilesDir(), DEFAULT_TOP_LEVEL_TEMP_DIRECTORY);
            tempRootDirectory.mkdirs();
            setTempFileRootDirectory(tempRootDirectory);
        }
        writeDownloadRequestToken(request);
        try {
            downloadService.download(request);
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
        }
    }

    public List<DownloadRequest> listPendingDownloads() {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            return downloadService.listPendingDownloads(this.mSubscriptionId);
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
            return Collections.emptyList();
        }
    }

    public void registerStateCallback(DownloadRequest request, DownloadStateCallback callback, Handler handler) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        InternalDownloadStateCallback internalCallback = new InternalDownloadStateCallback(callback, handler);
        try {
            int result = downloadService.registerStateCallback(request, internalCallback, callback.getCallbackFilterFlags());
            if (result == 0) {
                this.mInternalDownloadCallbacks.put(callback, internalCallback);
            } else if (result == 402) {
                throw new IllegalArgumentException("Unknown download request.");
            } else {
                sendErrorToApp(result, null);
            }
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
        }
    }

    public void unregisterStateCallback(DownloadRequest request, DownloadStateCallback callback) {
        InternalDownloadStateCallback internalCallback;
        try {
            IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
            if (downloadService == null) {
                throw new IllegalStateException("Middleware not yet bound");
            }
            int result = downloadService.unregisterStateCallback(request, (InternalDownloadStateCallback) this.mInternalDownloadCallbacks.get(callback));
            if (result != 0) {
                if (result == 402) {
                    throw new IllegalArgumentException("Unknown download request.");
                }
                sendErrorToApp(result, null);
            }
            internalCallback = (InternalDownloadStateCallback) this.mInternalDownloadCallbacks.remove(callback);
            if (internalCallback != null) {
                internalCallback.stop();
            }
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
        } catch (Throwable th) {
            internalCallback = (InternalDownloadStateCallback) this.mInternalDownloadCallbacks.remove(callback);
            if (internalCallback != null) {
                internalCallback.stop();
            }
        }
    }

    public void cancelDownload(DownloadRequest downloadRequest) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            int result = downloadService.cancelDownload(downloadRequest);
            if (result == 0) {
                deleteDownloadRequestToken(downloadRequest);
            } else if (result == 402) {
                throw new IllegalArgumentException("Unknown download request.");
            } else {
                sendErrorToApp(result, null);
            }
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
        }
    }

    public int getDownloadStatus(DownloadRequest downloadRequest, FileInfo fileInfo) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            return downloadService.getDownloadStatus(downloadRequest, fileInfo);
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
            return 0;
        }
    }

    public void resetDownloadKnowledge(DownloadRequest downloadRequest) {
        IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
        if (downloadService == null) {
            throw new IllegalStateException("Middleware not yet bound");
        }
        try {
            int result = downloadService.resetDownloadKnowledge(downloadRequest);
            if (result == 0) {
                return;
            }
            if (result == 402) {
                throw new IllegalArgumentException("Unknown download request.");
            }
            sendErrorToApp(result, null);
        } catch (RemoteException e) {
            this.mService.set(null);
            sendErrorToApp(3, null);
        }
    }

    public void close() {
        try {
            IMbmsDownloadService downloadService = (IMbmsDownloadService) this.mService.get();
            if (downloadService == null) {
                Log.i(LOG_TAG, "Service already dead");
                return;
            }
            downloadService.dispose(this.mSubscriptionId);
            this.mService.set(null);
            sIsInitialized.set(false);
            this.mInternalCallback.stop();
        } catch (RemoteException e) {
            Log.i(LOG_TAG, "Remote exception while disposing of service");
        } finally {
            this.mService.set(null);
            sIsInitialized.set(false);
            this.mInternalCallback.stop();
        }
    }

    private void writeDownloadRequestToken(DownloadRequest request) {
        File token = getDownloadRequestTokenPath(request);
        if (!token.getParentFile().exists()) {
            token.getParentFile().mkdirs();
        }
        if (token.exists()) {
            Log.w(LOG_TAG, "Download token " + token.getName() + " already exists");
            return;
        }
        try {
            if (!token.createNewFile()) {
                throw new RuntimeException("Failed to create download token for request " + request);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create download token for request " + request + " due to IOException " + e);
        }
    }

    private void deleteDownloadRequestToken(DownloadRequest request) {
        File token = getDownloadRequestTokenPath(request);
        if (token.isFile()) {
            if (!token.delete()) {
                Log.w(LOG_TAG, "Couldn't delete download token at " + token);
            }
            return;
        }
        Log.w(LOG_TAG, "Attempting to delete non-existent download token at " + token);
    }

    private File getDownloadRequestTokenPath(DownloadRequest request) {
        return new File(MbmsUtils.getEmbmsTempFileDirForService(this.mContext, request.getFileServiceId()), request.getHash() + MbmsDownloadReceiver.DOWNLOAD_TOKEN_SUFFIX);
    }

    private void sendErrorToApp(int errorCode, String message) {
        try {
            this.mInternalCallback.onError(errorCode, message);
        } catch (RemoteException e) {
        }
    }
}

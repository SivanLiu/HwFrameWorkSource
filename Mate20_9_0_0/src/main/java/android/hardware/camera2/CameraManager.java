package android.hardware.camera2;

import android.content.Context;
import android.hardware.CameraStatus;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceListener.Stub;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.impl.CameraDeviceImpl.CameraDeviceCallbacks;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.CameraDeviceUserShim;
import android.hardware.camera2.legacy.LegacyMetadataMapper;
import android.hsm.HwSystemManager;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CameraManager {
    private static final int API_VERSION_1 = 1;
    private static final int API_VERSION_2 = 2;
    private static final int CAMERA_TYPE_ALL = 1;
    private static final int CAMERA_TYPE_BACKWARD_COMPATIBLE = 0;
    private static final String TAG = "CameraManager";
    private static final int USE_CALLING_UID = -1;
    private final boolean DEBUG = false;
    private final Context mContext;
    private ArrayList<String> mDeviceIdList;
    private final Object mLock = new Object();

    public static abstract class AvailabilityCallback {
        public void onCameraAvailable(String cameraId) {
        }

        public void onCameraUnavailable(String cameraId) {
        }
    }

    public static abstract class TorchCallback {
        public void onTorchModeUnavailable(String cameraId) {
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
        }
    }

    private static final class CameraManagerGlobal extends Stub implements DeathRecipient {
        private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
        private static final String TAG = "CameraManagerGlobal";
        private static final CameraManagerGlobal gCameraManager = new CameraManagerGlobal();
        public static final boolean sCameraServiceDisabled = SystemProperties.getBoolean("config.disable_cameraservice", false);
        private final int CAMERA_SERVICE_RECONNECT_DELAY_MS = 1000;
        private final boolean DEBUG = false;
        private final ArrayMap<AvailabilityCallback, Executor> mCallbackMap = new ArrayMap();
        private ICameraService mCameraService;
        private final ArrayMap<String, Integer> mDeviceStatus = new ArrayMap();
        private final Object mLock = new Object();
        private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
        private final ArrayMap<TorchCallback, Executor> mTorchCallbackMap = new ArrayMap();
        private Binder mTorchClientBinder = new Binder();
        private final ArrayMap<String, Integer> mTorchStatus = new ArrayMap();

        private CameraManagerGlobal() {
        }

        public static CameraManagerGlobal get() {
            return gCameraManager;
        }

        public IBinder asBinder() {
            return this;
        }

        public ICameraService getCameraService() {
            ICameraService iCameraService;
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (this.mCameraService == null && !sCameraServiceDisabled) {
                    Log.e(TAG, "Camera service is unavailable");
                }
                iCameraService = this.mCameraService;
            }
            return iCameraService;
        }

        private void connectCameraServiceLocked() {
            if (this.mCameraService == null && !sCameraServiceDisabled) {
                Log.i(TAG, "Connecting to camera service");
                IBinder cameraServiceBinder = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
                if (cameraServiceBinder != null) {
                    int i = 0;
                    try {
                        cameraServiceBinder.linkToDeath(this, 0);
                        ICameraService cameraService = ICameraService.Stub.asInterface(cameraServiceBinder);
                        try {
                            CameraMetadataNative.setupGlobalVendorTagDescriptor();
                        } catch (ServiceSpecificException e) {
                            handleRecoverableSetupErrors(e);
                        }
                        try {
                            CameraStatus[] cameraStatuses = cameraService.addListener(this);
                            int length = cameraStatuses.length;
                            while (i < length) {
                                CameraStatus c = cameraStatuses[i];
                                onStatusChangedLocked(c.status, c.cameraId);
                                i++;
                            }
                            this.mCameraService = cameraService;
                        } catch (ServiceSpecificException e2) {
                            throw new IllegalStateException("Failed to register a camera service listener", e2);
                        } catch (RemoteException e3) {
                        }
                    } catch (RemoteException e4) {
                    }
                }
            }
        }

        public String[] getCameraIdList() {
            String[] cameraIds;
            synchronized (this.mLock) {
                int i;
                connectCameraServiceLocked();
                int i2 = 0;
                int idCount = 0;
                for (i = 0; i < this.mDeviceStatus.size(); i++) {
                    int status = ((Integer) this.mDeviceStatus.valueAt(i)).intValue();
                    if (status != 0) {
                        if (status != 2) {
                            idCount++;
                        }
                    }
                }
                cameraIds = new String[idCount];
                i = 0;
                while (i2 < this.mDeviceStatus.size()) {
                    idCount = ((Integer) this.mDeviceStatus.valueAt(i2)).intValue();
                    if (idCount != 0) {
                        if (idCount != 2) {
                            cameraIds[i] = (String) this.mDeviceStatus.keyAt(i2);
                            i++;
                        }
                    }
                    i2++;
                }
            }
            Arrays.sort(cameraIds, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    int s1Int;
                    int s2Int;
                    try {
                        s1Int = Integer.parseInt(s1);
                    } catch (NumberFormatException e) {
                        s1Int = -1;
                    }
                    try {
                        s2Int = Integer.parseInt(s2);
                    } catch (NumberFormatException e2) {
                        s2Int = -1;
                    }
                    if (s1Int >= 0 && s2Int >= 0) {
                        return s1Int - s2Int;
                    }
                    if (s1Int >= 0) {
                        return -1;
                    }
                    if (s2Int >= 0) {
                        return 1;
                    }
                    return s1.compareTo(s2);
                }
            });
            return cameraIds;
        }

        public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
            synchronized (this.mLock) {
                if (cameraId != null) {
                    ICameraService cameraService = getCameraService();
                    if (cameraService != null) {
                        try {
                            cameraService.setTorchMode(cameraId, enabled, this.mTorchClientBinder);
                        } catch (ServiceSpecificException e) {
                            CameraManager.throwAsPublicException(e);
                        } catch (RemoteException e2) {
                            throw new CameraAccessException(2, "Camera service is currently unavailable");
                        }
                    } else {
                        throw new CameraAccessException(2, "Camera service is currently unavailable");
                    }
                }
                throw new IllegalArgumentException("cameraId was null");
            }
        }

        private void handleRecoverableSetupErrors(ServiceSpecificException e) {
            if (e.errorCode == 4) {
                Log.w(TAG, e.getMessage());
                return;
            }
            throw new IllegalStateException(e);
        }

        private boolean isAvailable(int status) {
            if (status != 1) {
                return false;
            }
            return true;
        }

        private boolean validStatus(int status) {
            if (status != -2) {
                switch (status) {
                    case 0:
                    case 1:
                    case 2:
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        private boolean validTorchStatus(int status) {
            switch (status) {
                case 0:
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }

        private void postSingleUpdate(final AvailabilityCallback callback, Executor executor, final String id, int status) {
            long ident;
            if (isAvailable(status)) {
                ident = Binder.clearCallingIdentity();
                try {
                    executor.execute(new Runnable() {
                        public void run() {
                            callback.onCameraAvailable(id);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                ident = Binder.clearCallingIdentity();
                try {
                    executor.execute(new Runnable() {
                        public void run() {
                            callback.onCameraUnavailable(id);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        private void postSingleTorchUpdate(TorchCallback callback, Executor executor, String id, int status) {
            long ident;
            switch (status) {
                case 1:
                case 2:
                    ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraManager$CameraManagerGlobal$CONvadOBAEkcHSpx8j61v67qRGM(callback, id, status));
                        return;
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                default:
                    ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraManager$CameraManagerGlobal$6Ptxoe4wF_VCkE_pml8t66mklao(callback, id));
                        return;
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
            }
        }

        static /* synthetic */ void lambda$postSingleTorchUpdate$0(TorchCallback callback, String id, int status) {
            callback.onTorchModeChanged(id, status == 2);
        }

        private void updateCallbackLocked(AvailabilityCallback callback, Executor executor) {
            for (int i = 0; i < this.mDeviceStatus.size(); i++) {
                postSingleUpdate(callback, executor, (String) this.mDeviceStatus.keyAt(i), ((Integer) this.mDeviceStatus.valueAt(i)).intValue());
            }
        }

        private void onStatusChangedLocked(int status, String id) {
            int i = 0;
            if (validStatus(status)) {
                Integer oldStatus;
                if (status == 0) {
                    oldStatus = (Integer) this.mDeviceStatus.remove(id);
                } else {
                    oldStatus = (Integer) this.mDeviceStatus.put(id, Integer.valueOf(status));
                }
                if (oldStatus != null && oldStatus.intValue() == status) {
                    return;
                }
                if (oldStatus == null || isAvailable(status) != isAvailable(oldStatus.intValue())) {
                    int callbackCount = this.mCallbackMap.size();
                    while (i < callbackCount) {
                        postSingleUpdate((AvailabilityCallback) this.mCallbackMap.keyAt(i), (Executor) this.mCallbackMap.valueAt(i), id, status);
                        i++;
                    }
                    return;
                }
                return;
            }
            Log.e(TAG, String.format("Ignoring invalid device %s status 0x%x", new Object[]{id, Integer.valueOf(status)}));
        }

        private void updateTorchCallbackLocked(TorchCallback callback, Executor executor) {
            for (int i = 0; i < this.mTorchStatus.size(); i++) {
                postSingleTorchUpdate(callback, executor, (String) this.mTorchStatus.keyAt(i), ((Integer) this.mTorchStatus.valueAt(i)).intValue());
            }
        }

        private void onTorchStatusChangedLocked(int status, String id) {
            int i = 0;
            if (validTorchStatus(status)) {
                Integer oldStatus = (Integer) this.mTorchStatus.put(id, Integer.valueOf(status));
                if (oldStatus == null || oldStatus.intValue() != status) {
                    int callbackCount = this.mTorchCallbackMap.size();
                    while (i < callbackCount) {
                        postSingleTorchUpdate((TorchCallback) this.mTorchCallbackMap.keyAt(i), (Executor) this.mTorchCallbackMap.valueAt(i), id, status);
                        i++;
                    }
                    return;
                }
                return;
            }
            Log.e(TAG, String.format("Ignoring invalid device %s torch status 0x%x", new Object[]{id, Integer.valueOf(status)}));
        }

        public void registerAvailabilityCallback(AvailabilityCallback callback, Executor executor) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (((Executor) this.mCallbackMap.put(callback, executor)) == null) {
                    updateCallbackLocked(callback, executor);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterAvailabilityCallback(AvailabilityCallback callback) {
            synchronized (this.mLock) {
                this.mCallbackMap.remove(callback);
            }
        }

        public void registerTorchCallback(TorchCallback callback, Executor executor) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (((Executor) this.mTorchCallbackMap.put(callback, executor)) == null) {
                    updateTorchCallbackLocked(callback, executor);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterTorchCallback(TorchCallback callback) {
            synchronized (this.mLock) {
                this.mTorchCallbackMap.remove(callback);
            }
        }

        public void onStatusChanged(int status, String cameraId) throws RemoteException {
            synchronized (this.mLock) {
                onStatusChangedLocked(status, cameraId);
            }
        }

        public void onTorchStatusChanged(int status, String cameraId) throws RemoteException {
            synchronized (this.mLock) {
                onTorchStatusChangedLocked(status, cameraId);
            }
        }

        private void scheduleCameraServiceReconnectionLocked() {
            if (!this.mCallbackMap.isEmpty() || !this.mTorchCallbackMap.isEmpty()) {
                try {
                    this.mScheduler.schedule(new -$$Lambda$CameraManager$CameraManagerGlobal$w1y8myi6vgxAcTEs8WArI-NN3R0(this), 1000, TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to schedule camera service re-connect: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }

        public static /* synthetic */ void lambda$scheduleCameraServiceReconnectionLocked$2(CameraManagerGlobal cameraManagerGlobal) {
            if (cameraManagerGlobal.getCameraService() == null) {
                synchronized (cameraManagerGlobal.mLock) {
                    cameraManagerGlobal.scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void binderDied() {
            synchronized (this.mLock) {
                if (this.mCameraService == null) {
                    return;
                }
                int i;
                this.mCameraService = null;
                for (i = 0; i < this.mDeviceStatus.size(); i++) {
                    onStatusChangedLocked(0, (String) this.mDeviceStatus.keyAt(i));
                }
                for (i = 0; i < this.mTorchStatus.size(); i++) {
                    onTorchStatusChangedLocked(0, (String) this.mTorchStatus.keyAt(i));
                }
                scheduleCameraServiceReconnectionLocked();
            }
        }
    }

    public CameraManager(Context context) {
        synchronized (this.mLock) {
            this.mContext = context;
        }
    }

    public String[] getCameraIdList() throws CameraAccessException {
        return CameraManagerGlobal.get().getCameraIdList();
    }

    public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler) {
        CameraManagerGlobal.get().registerAvailabilityCallback(callback, CameraDeviceImpl.checkAndWrapHandler(handler));
    }

    public void registerAvailabilityCallback(Executor executor, AvailabilityCallback callback) {
        if (executor != null) {
            CameraManagerGlobal.get().registerAvailabilityCallback(callback, executor);
            return;
        }
        throw new IllegalArgumentException("executor was null");
    }

    public void unregisterAvailabilityCallback(AvailabilityCallback callback) {
        CameraManagerGlobal.get().unregisterAvailabilityCallback(callback);
    }

    public void registerTorchCallback(TorchCallback callback, Handler handler) {
        CameraManagerGlobal.get().registerTorchCallback(callback, CameraDeviceImpl.checkAndWrapHandler(handler));
    }

    public void registerTorchCallback(Executor executor, TorchCallback callback) {
        if (executor != null) {
            CameraManagerGlobal.get().registerTorchCallback(callback, executor);
            return;
        }
        throw new IllegalArgumentException("executor was null");
    }

    public void unregisterTorchCallback(TorchCallback callback) {
        CameraManagerGlobal.get().unregisterTorchCallback(callback);
    }

    public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = null;
        if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        }
        synchronized (this.mLock) {
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService != null) {
                try {
                    if (supportsCamera2ApiLocked(cameraId)) {
                        characteristics = new CameraCharacteristics(cameraService.getCameraCharacteristics(cameraId));
                    } else {
                        int id = Integer.parseInt(cameraId);
                        characteristics = LegacyMetadataMapper.createCharacteristics(cameraService.getLegacyParameters(id), cameraService.getCameraInfo(id));
                    }
                } catch (ServiceSpecificException e) {
                    throwAsPublicException(e);
                } catch (RemoteException e2) {
                    throw new CameraAccessException(2, "Camera service is currently unavailable", e2);
                }
            } else {
                throw new CameraAccessException(2, "Camera service is currently unavailable");
            }
        }
        return characteristics;
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x011d A:{Catch:{ ServiceSpecificException -> 0x00dd, RemoteException -> 0x00cc, all -> 0x012a }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00e6 A:{Catch:{ ServiceSpecificException -> 0x00dd, RemoteException -> 0x00cc, all -> 0x012a }} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:16:0x0088, B:21:0x009a] */
    /* JADX WARNING: Missing block: B:29:0x00b1, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:31:0x00b3, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:32:0x00b4, code skipped:
            r6 = r0;
            r13 = new java.lang.StringBuilder();
            r13.append("Expected cameraId to be numeric, but it was: ");
            r13.append(r8);
     */
    /* JADX WARNING: Missing block: B:33:0x00cb, code skipped:
            throw new java.lang.IllegalArgumentException(r13.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private CameraDevice openCameraDeviceUserAsync(String cameraId, StateCallback callback, Executor executor, int uid) throws CameraAccessException {
        CameraDevice device;
        Throwable th;
        String str = cameraId;
        CameraCharacteristics characteristics = getCameraCharacteristics(cameraId);
        synchronized (this.mLock) {
            ICameraDeviceUser cameraUser = null;
            int i;
            try {
                CameraDevice deviceImpl = new CameraDeviceImpl(str, callback, executor, characteristics, this.mContext.getApplicationInfo().targetSdkVersion);
                CameraDeviceCallbacks callbacks = deviceImpl.getCallbacks();
                try {
                    int id;
                    if (supportsCamera2ApiLocked(cameraId)) {
                        if (!HwSystemManager.allowOp(1024)) {
                            throwAsPublicException(new ServiceSpecificException(6));
                        }
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("open camera: ");
                        stringBuilder.append(str);
                        stringBuilder.append(", package name: ");
                        stringBuilder.append(this.mContext.getOpPackageName());
                        Log.i(str2, stringBuilder.toString());
                        HwSystemManager.notifyBackgroundMgr(this.mContext.getOpPackageName(), Binder.getCallingPid(), Binder.getCallingUid(), 0, 1);
                        ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
                        if (cameraService != null) {
                            id = cameraService.connectDevice(callbacks, str, this.mContext.getOpPackageName(), uid);
                        } else {
                            i = uid;
                            throw new ServiceSpecificException(4, "Camera service is currently unavailable");
                        }
                    }
                    i = uid;
                    id = Integer.parseInt(cameraId);
                    Log.i(TAG, "Using legacy camera HAL.");
                    id = CameraDeviceUserShim.connectBinderShim(callbacks, id);
                    cameraUser = id;
                } catch (ServiceSpecificException e) {
                    ServiceSpecificException e2 = e;
                    i = uid;
                    if (e2.errorCode == 9) {
                        if (!(e2.errorCode == 7 || e2.errorCode == 8 || e2.errorCode == 6 || e2.errorCode == 4)) {
                            if (e2.errorCode != 10) {
                                throwAsPublicException(e2);
                                deviceImpl.setRemoteDevice(cameraUser);
                                device = deviceImpl;
                                return device;
                            }
                        }
                        deviceImpl.setRemoteFailure(e2);
                        if (e2.errorCode == 6 || e2.errorCode == 4 || e2.errorCode == 7) {
                            throwAsPublicException(e2);
                        }
                        deviceImpl.setRemoteDevice(cameraUser);
                        device = deviceImpl;
                        return device;
                    }
                    throw new AssertionError("Should've gone down the shim path");
                } catch (RemoteException e3) {
                    i = uid;
                    ServiceSpecificException sse = new ServiceSpecificException(4, "Camera service is currently unavailable");
                    deviceImpl.setRemoteFailure(sse);
                    throwAsPublicException(sse);
                    deviceImpl.setRemoteDevice(cameraUser);
                    device = deviceImpl;
                    return device;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
                deviceImpl.setRemoteDevice(cameraUser);
                device = deviceImpl;
                return device;
            } catch (Throwable th3) {
                th = th3;
                i = uid;
                throw th;
            }
        }
    }

    public void openCamera(String cameraId, StateCallback callback, Handler handler) throws CameraAccessException {
        openCameraForUid(cameraId, callback, CameraDeviceImpl.checkAndWrapHandler(handler), -1);
    }

    public void openCamera(String cameraId, Executor executor, StateCallback callback) throws CameraAccessException {
        if (executor != null) {
            openCameraForUid(cameraId, callback, executor, -1);
            return;
        }
        throw new IllegalArgumentException("executor was null");
    }

    public void openCameraForUid(String cameraId, StateCallback callback, Executor executor, int clientUid) throws CameraAccessException {
        if (cameraId == null) {
            throw new IllegalArgumentException("cameraId was null");
        } else if (callback == null) {
            throw new IllegalArgumentException("callback was null");
        } else if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        } else {
            openCameraDeviceUserAsync(cameraId, callback, executor, clientUid);
        }
    }

    public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
        if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        }
        CameraManagerGlobal.get().setTorchMode(cameraId, enabled);
    }

    public static void throwAsPublicException(Throwable t) throws CameraAccessException {
        if (t instanceof ServiceSpecificException) {
            int reason;
            ServiceSpecificException e = (ServiceSpecificException) t;
            switch (e.errorCode) {
                case 1:
                    throw new SecurityException(e.getMessage(), e);
                case 2:
                case 3:
                    throw new IllegalArgumentException(e.getMessage(), e);
                case 4:
                    reason = 2;
                    break;
                case 6:
                    reason = 1;
                    break;
                case 7:
                    reason = 4;
                    break;
                case 8:
                    reason = 5;
                    break;
                case 9:
                    reason = 1000;
                    break;
                default:
                    reason = 3;
                    break;
            }
            throw new CameraAccessException(reason, e.getMessage(), e);
        } else if (t instanceof DeadObjectException) {
            throw new CameraAccessException(2, "Camera service has died unexpectedly", t);
        } else if (t instanceof RemoteException) {
            throw new UnsupportedOperationException("An unknown RemoteException was thrown which should never happen.", t);
        } else if (t instanceof RuntimeException) {
            throw ((RuntimeException) t);
        }
    }

    private boolean supportsCamera2ApiLocked(String cameraId) {
        return supportsCameraApiLocked(cameraId, 2);
    }

    private boolean supportsCameraApiLocked(String cameraId, int apiVersion) {
        try {
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService == null) {
                return false;
            }
            return cameraService.supportsCameraApi(cameraId, apiVersion);
        } catch (RemoteException e) {
            return false;
        }
    }
}

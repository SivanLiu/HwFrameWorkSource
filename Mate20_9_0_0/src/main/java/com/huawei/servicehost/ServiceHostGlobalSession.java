package com.huawei.servicehost;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.servicehost.IGlobalListener.Stub;

public class ServiceHostGlobalSession {
    public static final String CAPTURE_RESULT_EXIF = "captureResultExif";
    public static final String CAPTURE_RESULT_FILE_PATH = "captureResultFilePath";
    public static final String CAPTURE_RESULT_HEIGHT = "captureResultHeight";
    public static final String CAPTURE_RESULT_MODE = "captureResultMode";
    public static final String CAPTURE_RESULT_SIZE = "captureResultSize";
    public static final String CAPTURE_RESULT_STATUS = "captureResultStatus";
    public static final String CAPTURE_RESULT_WIDTH = "captureResultWidth";
    private static final boolean DEBUG = false;
    private static final String GLOBAL_EVENT_AVALIABLE_CAPTURE_NUM = "AvaliableCapNum";
    private static final String GLOBAL_EVENT_CAPTURE_RESULT = "result";
    private static final String TAG = "ServiceHostGlobalSession";
    private DeathListener mDeathListener;
    private final Object mDeathListenerLock;
    private GlobalListener mGlobalListener;
    private final Object mGlobalListenerLock;
    private final Object mGlobalSessionLock;
    private IDeathListener mIDeathListener;
    private IGlobalListener mIGlobalListener;
    private IGlobalSession mIGlobalSession;

    public interface DeathListener {
        void onServiceHostDied();
    }

    public interface GlobalListener {
        void onPictureSaved(Bundle bundle);

        void onSnapshotNumUpdate(int i);
    }

    private class IDeathListener implements DeathRecipient {
        private IDeathListener() {
        }

        /* synthetic */ IDeathListener(ServiceHostGlobalSession x0, AnonymousClass1 x1) {
            this();
        }

        public void binderDied() {
            Log.e(ServiceHostGlobalSession.TAG, "servicehost died!");
            synchronized (ServiceHostGlobalSession.this.mDeathListenerLock) {
                if (ServiceHostGlobalSession.this.mDeathListener == null) {
                    Log.e(ServiceHostGlobalSession.TAG, "listener is null, cann't report to app!");
                    return;
                }
                ServiceHostGlobalSession.this.mDeathListener.onServiceHostDied();
            }
        }
    }

    private static class SingleGlobalSessionHolder {
        private static final ServiceHostGlobalSession mInstance = new ServiceHostGlobalSession();

        private SingleGlobalSessionHolder() {
        }
    }

    /* synthetic */ ServiceHostGlobalSession(AnonymousClass1 x0) {
        this();
    }

    public static final ServiceHostGlobalSession getInstance() {
        return SingleGlobalSessionHolder.mInstance;
    }

    private ServiceHostGlobalSession() {
        this.mIGlobalSession = null;
        this.mGlobalListener = null;
        this.mDeathListener = null;
        this.mGlobalSessionLock = new Object();
        this.mGlobalListenerLock = new Object();
        this.mDeathListenerLock = new Object();
        this.mIGlobalListener = new Stub() {
            public void onGlobalEvent(IIPEvent iipEvent) throws RemoteException {
                if (iipEvent != null) {
                    GlobalListener globalListener;
                    synchronized (ServiceHostGlobalSession.this.mGlobalListenerLock) {
                        globalListener = ServiceHostGlobalSession.this.mGlobalListener;
                    }
                    if (globalListener != null) {
                        IBinder obj = iipEvent.getObject();
                        String type = iipEvent.getType();
                        Object obj2 = -1;
                        int hashCode = type.hashCode();
                        if (hashCode != -934426595) {
                            if (hashCode == 420468279 && type.equals(ServiceHostGlobalSession.GLOBAL_EVENT_AVALIABLE_CAPTURE_NUM)) {
                                obj2 = 1;
                            }
                        } else if (type.equals(ServiceHostGlobalSession.GLOBAL_EVENT_CAPTURE_RESULT)) {
                            obj2 = null;
                        }
                        String str;
                        StringBuilder stringBuilder;
                        switch (obj2) {
                            case null:
                                IIPEvent4GlobalResult result = IIPEvent4GlobalResult.Stub.asInterface(obj);
                                if (result != null) {
                                    String path = result.getFilePath();
                                    str = ServiceHostGlobalSession.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("jpeg saved, path: ");
                                    stringBuilder.append(path);
                                    Log.i(str, stringBuilder.toString());
                                    int imageSaveState = result.getResult();
                                    String str2 = ServiceHostGlobalSession.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("servicehost save status: ");
                                    stringBuilder2.append(imageSaveState);
                                    Log.i(str2, stringBuilder2.toString());
                                    int captureMode = result.getCaptureMode();
                                    String str3 = ServiceHostGlobalSession.TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("capture mode: ");
                                    stringBuilder3.append(captureMode);
                                    Log.i(str3, stringBuilder3.toString());
                                    int captureWidth = result.getCaptureWidth();
                                    int captureHeight = result.getCaptureHeight();
                                    String str4 = ServiceHostGlobalSession.TAG;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("capture size: ");
                                    stringBuilder4.append(captureWidth);
                                    stringBuilder4.append(" x ");
                                    stringBuilder4.append(captureHeight);
                                    Log.i(str4, stringBuilder4.toString());
                                    int pictureSize = result.getPictureSize();
                                    String str5 = ServiceHostGlobalSession.TAG;
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append(", jpeg size: ");
                                    stringBuilder5.append(pictureSize);
                                    Log.i(str5, stringBuilder5.toString());
                                    Bundle picInfo = new Bundle();
                                    picInfo.putString(ServiceHostGlobalSession.CAPTURE_RESULT_FILE_PATH, path);
                                    picInfo.putInt(ServiceHostGlobalSession.CAPTURE_RESULT_STATUS, imageSaveState);
                                    picInfo.putInt(ServiceHostGlobalSession.CAPTURE_RESULT_MODE, captureMode);
                                    picInfo.putInt(ServiceHostGlobalSession.CAPTURE_RESULT_WIDTH, captureWidth);
                                    picInfo.putInt(ServiceHostGlobalSession.CAPTURE_RESULT_HEIGHT, captureHeight);
                                    picInfo.putInt(ServiceHostGlobalSession.CAPTURE_RESULT_SIZE, pictureSize);
                                    picInfo.putString(ServiceHostGlobalSession.CAPTURE_RESULT_EXIF, "");
                                    globalListener.onPictureSaved(picInfo);
                                    break;
                                }
                                Log.e(ServiceHostGlobalSession.TAG, "get result from global event return null!");
                                return;
                            case 1:
                                hashCode = IIPEvent4CapNumber.Stub.asInterface(obj).getLowCapNumber();
                                if (hashCode <= 0) {
                                    str = ServiceHostGlobalSession.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("update available number: ");
                                    stringBuilder.append(hashCode);
                                    Log.d(str, stringBuilder.toString());
                                }
                                globalListener.onSnapshotNumUpdate(hashCode);
                                break;
                            default:
                                String str6 = ServiceHostGlobalSession.TAG;
                                StringBuilder stringBuilder6 = new StringBuilder();
                                stringBuilder6.append("unknown type: ");
                                stringBuilder6.append(type);
                                Log.e(str6, stringBuilder6.toString());
                                break;
                        }
                    }
                }
            }
        };
        this.mIDeathListener = new IDeathListener(this, null);
        Log.i(TAG, "construct global session");
    }

    public void initialize(GlobalListener globalListener, DeathListener deathListener) {
        String str;
        StringBuilder stringBuilder;
        Log.i(TAG, "initialize global session.");
        synchronized (this.mGlobalSessionLock) {
            this.mIGlobalSession = ImageProcessManager.get().getGlobalSession();
            if (this.mIGlobalSession == null) {
                Log.d(TAG, "get global session return null!");
                return;
            }
            try {
                this.mIGlobalSession.addListener(this.mIGlobalListener);
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("add global session listener exception: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
            try {
                this.mIGlobalSession.asBinder().linkToDeath(this.mIDeathListener, 0);
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("register binder die notification exception:");
                stringBuilder.append(e2.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        while (true) {
        }
        synchronized (this.mGlobalListenerLock) {
            this.mGlobalListener = globalListener;
        }
        synchronized (this.mDeathListenerLock) {
            this.mDeathListener = deathListener;
        }
        Log.i(TAG, "link servicehost death.");
    }

    public void release() {
        String str;
        StringBuilder stringBuilder;
        Log.i(TAG, "release global session.");
        synchronized (this.mGlobalSessionLock) {
            if (this.mIGlobalSession == null) {
                Log.d(TAG, "global session is null!");
                return;
            }
            try {
                this.mIGlobalSession.removeListener(this.mIGlobalListener);
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("remove global session listener exception: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
            if (this.mIDeathListener == null) {
                Log.d(TAG, "servicehost death listener is null!");
                return;
            }
            try {
                this.mIGlobalSession.asBinder().unlinkToDeath(this.mIDeathListener, 0);
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unlinkToDeath exception: ");
                stringBuilder.append(e2.getMessage());
                Log.e(str, stringBuilder.toString());
            }
            this.mIGlobalSession = null;
            synchronized (this.mGlobalListenerLock) {
                this.mGlobalListener = null;
            }
            synchronized (this.mDeathListenerLock) {
                this.mDeathListener = null;
            }
            Log.i(TAG, "unlink servicehost death.");
        }
    }

    public CameraCharacteristics getCharacteristics(String cameraId, CameraCharacteristics character) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get servicehost characteristics, camera id: ");
        stringBuilder.append(cameraId);
        Log.i(str, stringBuilder.toString());
        if (character == null) {
            Log.i(TAG, "input characteristics is null!");
            return null;
        } else if (ServiceFetcher.checkConnected()) {
            CameraMetadataNative nativeMetadata = character.getNativeCopy();
            ImageProcessManager.get().queryCapability(cameraId, nativeMetadata);
            return new CameraCharacteristics(nativeMetadata);
        } else {
            Log.i(TAG, "do not connect to service host.");
            return character;
        }
    }

    public int getSupportedMode() {
        Log.i(TAG, "get servicehost supported mode.");
        if (ServiceFetcher.checkConnected()) {
            int supportedMode = ImageProcessManager.get().getSupportedMode();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("supported mode: ");
            stringBuilder.append(Integer.toHexString(supportedMode));
            Log.i(str, stringBuilder.toString());
            return supportedMode;
        }
        Log.i(TAG, "can not connect to service host.");
        return 0;
    }

    public int dualCameraMode() {
        Log.i(TAG, "dual camera mode.");
        if (ServiceFetcher.checkConnected()) {
            int dualCameraMode = ImageProcessManager.get().dualCameraMode();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dual camera mode: ");
            stringBuilder.append(Integer.toHexString(dualCameraMode));
            Log.i(str, stringBuilder.toString());
            return dualCameraMode;
        }
        Log.i(TAG, "can not connect to service host.");
        return 0;
    }
}

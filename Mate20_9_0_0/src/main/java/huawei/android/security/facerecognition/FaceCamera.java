package huawei.android.security.facerecognition;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.Flog;
import android.util.Range;
import android.view.Surface;
import huawei.android.security.facerecognition.FaceRecognizeManagerImpl.ServiceHolder;
import huawei.android.security.facerecognition.base.HwSecurityEventTask;
import huawei.android.security.facerecognition.base.HwSecurityTaskThread;
import huawei.android.security.facerecognition.utils.LogUtil;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FaceCamera {
    private static final Key<Integer> ANDROID_HW_BIO_FACE_MODE = new Key("com.huawei.capture.metadata.bioFaceMode", Integer.TYPE);
    private static final int AUTH_PAY_TYPE = 1;
    public static final int BD_REPORT_EVENT_ID_IMAGE = 502;
    public static final int BD_REPORT_EVNET_ID_OPEN = 501;
    private static final int BIO_FACE_MODE_3D_AUTHENTICATION = 5;
    private static final int BIO_FACE_MODE_3D_ENROLLMENT = 4;
    private static final int BIO_FACE_MODE_AUTHENTICATION = 2;
    private static final int BIO_FACE_MODE_ENROLLMENT = 1;
    private static final int BIO_FACE_MODE_PAY = 3;
    private static final int CAMERA_CLOSING = 2;
    private static final int CAMERA_IDLE = 0;
    private static final int CAMERA_OPENING = 3;
    private static final int CAMERA_READY = 1;
    private static final int HEIGHT = 480;
    private static final int MSG_CAMERA_CLOSED = 4;
    private static final int MSG_CAMERA_DISCONNECTED = 3;
    private static final int MSG_CREATE_REQUEST_OK = 20;
    private static final int MSG_CREATE_SESSION_CAMERA_CLOSED = 10;
    private static final int MSG_CREATE_SESSION_FAILED = 12;
    private static final int MSG_CREATE_SESSION_OK = 11;
    private static final int MSG_OPEN_CAMERA_ERROR = 2;
    private static final int MSG_OPEN_CAMERA_OK = 0;
    private static final int MSG_OPEN_CAMERA_TIME_OUT = 1;
    private static final int NO_MSG_CODE = -1;
    private static final int OP_CLOSE_CAMERA = 3;
    private static final int OP_CREATE_SESSION = 1;
    private static final int OP_OPEN_CAMERA = 0;
    private static final int OP_SEND_REQUEST = 2;
    private static final int REQUEST_SENDING = 6;
    private static final int REQUEST_WORKING = 7;
    public static final int RET_CREATE_SESSION_FAILED = 1004;
    public static final int RET_CREATE_SESSION_OK = 1003;
    public static final int RET_OPEN_CAMERA_FAILED = 1001;
    public static final int RET_OPEN_CAMERA_OK = 1000;
    public static final int RET_OPEN_CAMERA_TIMEOUT = 1002;
    public static final int RET_OP_ALLOW = 1;
    public static final int RET_OP_ALREADY = 0;
    public static final int RET_OP_DENY = 2;
    public static final int RET_REPEAT_REQUEST_FAILED = 1006;
    public static final int RET_REPEAT_REQUEST_OK = 1005;
    private static final int SECURE_CAMERA = 1;
    private static final int SECURE_CAMERA_TYPE_POSITION = 1;
    private static final int SECURE_CAMERA_WITH_3D = 4;
    private static final int SECURE_CAMERA_WITH_3D_AUTH_FPS = 60;
    private static final int SECURE_CAMERA_WITH_3D_ENROLL_FPS = 30;
    private static final int SECURE_CAMERA_WITH_DEPTH_MAP = 3;
    private static final int SECURE_MODE = 1;
    private static final int SESSION_CREATED = 5;
    private static final int SESSION_CREATING = 4;
    private static final int SKIP_IMAGE_QUEUE_SIZE = 2;
    private static final boolean SKIP_IMAGE_SWICH_ON = true;
    private static final int SUPPORT_FACE_MODE = SystemProperties.getInt("ro.config.support_face_mode", 1);
    public static final String SYSTEM_UI_PKG = "com.android.systemui";
    private static final String TAG = "FaceCamera";
    private static final int UNSECURE_CAMERA = 0;
    private static final int WIDTH = 640;
    private static final SensorEventListener mLightSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private static FaceCamera sInstance;
    private Range<Integer>[] fpsRanges;
    private boolean isEnrolling;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Semaphore mCameraCloseLock = new Semaphore(1);
    private CameraDevice mCameraDevice;
    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread = new HandlerThread("face_camera");
    private String mCameraId;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private final StateCallback mCameraStateCallback = new StateCallback() {
        public void onOpened(CameraDevice cameraDevice) {
            LogUtil.i(FaceCamera.TAG, "cb - onOpened");
            long current = System.nanoTime();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Time 2.1. call-back open camera --- ");
            stringBuilder.append(current);
            LogUtil.d("PerformanceTime", stringBuilder.toString());
            FaceCamera.this.mCameraOpenCloseLock.release();
            FaceCamera.this.mCameraDevice = cameraDevice;
            if (FaceCamera.this.updateStateTo(1)) {
                FaceCamera.this.handleMessage(0);
            }
        }

        public void onDisconnected(CameraDevice cameraDevice) {
            LogUtil.i(FaceCamera.TAG, "cb - onDisconnected");
            FaceCamera.this.updateStateTo(2);
            FaceCamera.this.unRegisterLightSensorListener();
            FaceCamera.this.mCameraOpenCloseLock.release();
            FaceCamera.this.mCameraCloseLock.release();
            FaceCamera.this.closeImageReader();
            FaceCamera.this.stopBackgroundThread();
            cameraDevice.close();
            FaceCamera.this.mCameraDevice = null;
            FaceCamera.this.handleMessage(3);
        }

        public void onError(CameraDevice cameraDevice, int error) {
            String str = FaceCamera.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cb - onError ");
            stringBuilder.append(error);
            LogUtil.i(str, stringBuilder.toString());
            FaceCamera.this.updateStateTo(0);
            FaceCamera.this.unRegisterLightSensorListener();
            FaceCamera.this.mCameraOpenCloseLock.release();
            FaceCamera.this.mCameraCloseLock.release();
            FaceCamera.this.closeImageReader();
            FaceCamera.this.stopBackgroundThread();
            cameraDevice.close();
            FaceCamera.this.mCameraDevice = null;
            FaceCamera.this.handleMessage(2, error);
        }

        public void onClosed(CameraDevice camera) {
            LogUtil.i(FaceCamera.TAG, "cb - onClosed");
            FaceCamera.this.updateStateTo(0);
            FaceCamera.this.handleMessage(4);
            FaceCamera.this.unRegisterLightSensorListener();
            FaceCamera.this.mCameraCloseLock.release();
            FaceCamera.this.closeImageReader();
            FaceCamera.this.stopBackgroundThread();
            FaceCamera.this.mCameraDevice = null;
        }
    };
    private CameraCaptureSession mCaptureSession;
    private CameraCharacteristics mCharacteristics;
    private Context mContext;
    private long mCurrentTime;
    private int mImageCount = 0;
    private Object mImageCountLock = new Object();
    private ImageReader mImageReader;
    private final Object mImageReaderLock = new Object();
    private volatile boolean mIsImageReported = false;
    private final OnImageAvailableListener mOnImageAvailableListener = new OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            String str;
            Throwable th;
            LogUtil.d("DebugImage", "OnImageAvailable.");
            synchronized (FaceCamera.this.mImageReaderLock) {
                if (FaceCamera.this.mImageReader == null) {
                    return;
                }
                Image image;
                StringBuilder stringBuilder;
                try {
                    image = reader.acquireNextImage();
                    if (image != null) {
                        try {
                            if (FaceCamera.this.mBackgroundHandler == null) {
                                if (image != null) {
                                    image.close();
                                }
                                return;
                            } else if (FaceCamera.this.isEnrolling || !FaceCamera.this.increaseAndCheckImageCount()) {
                                if (FaceCamera.this.printTimeLog) {
                                    long current = System.nanoTime();
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Time 4.2. call-back get First Image --- ");
                                    stringBuilder2.append(current);
                                    LogUtil.d("PerformanceTime", stringBuilder2.toString());
                                }
                                if (!FaceCamera.this.mIsImageReported && "com.android.systemui".equals(FaceCamera.this.mContext.getOpPackageName())) {
                                    LogUtil.d(FaceCamera.TAG, "Big data report image");
                                    long now = System.currentTimeMillis();
                                    Context access$700 = FaceCamera.this.mContext;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("{\"capture_picture_cost_ms\":\"");
                                    stringBuilder3.append(now - FaceCamera.this.mCurrentTime);
                                    stringBuilder3.append("\"}");
                                    Flog.bdReport(access$700, 502, stringBuilder3.toString());
                                    FaceCamera.this.mCurrentTime = now;
                                    FaceCamera.this.mIsImageReported = true;
                                } else if (!FaceCamera.this.mIsImageReported) {
                                    String str2 = FaceCamera.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Need report? : ");
                                    stringBuilder.append(FaceCamera.this.mIsImageReported);
                                    stringBuilder.append(", OP pkg name : ");
                                    stringBuilder.append(FaceCamera.this.mContext.getOpPackageName());
                                    LogUtil.d(str2, stringBuilder.toString());
                                    FaceCamera.this.mIsImageReported = true;
                                }
                                LogUtil.d("DebugImage", "Extract image");
                                final Rect crop = image.getCropRect();
                                Plane[] planes = image.getPlanes();
                                ByteBuffer[] planeArray = new ByteBuffer[planes.length];
                                int[] rowStrideArray = new int[planes.length];
                                int[] pixelStrideArray = new int[planes.length];
                                for (int i = 0; i < planes.length; i++) {
                                    Plane plane = planes[i];
                                    planeArray[i] = FaceCamera.this.cloneByteBuffer(plane.getBuffer());
                                    rowStrideArray[i] = plane.getRowStride();
                                    pixelStrideArray[i] = plane.getPixelStride();
                                }
                                LogUtil.d("DebugImage", "Extract image end");
                                final ByteBuffer[] byteBufferArr = planeArray;
                                final int[] iArr = rowStrideArray;
                                final int[] iArr2 = pixelStrideArray;
                                FaceCamera.this.mBackgroundHandler.post(new Runnable() {
                                    public void run() {
                                        if (FaceCamera.SUPPORT_FACE_MODE == 0 && FaceCamera.native_send_image(FaceCamera.this.getDataFromImage(crop, byteBufferArr, iArr, iArr2)) != 0) {
                                            LogUtil.e(FaceCamera.TAG, "SendImageData failed");
                                        }
                                        if (FaceCamera.this.printTimeLog) {
                                            long current = System.nanoTime();
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("Time 4.3. call-back Send First Image Data --- ");
                                            stringBuilder.append(current);
                                            LogUtil.d("PerformanceTime", stringBuilder.toString());
                                            FaceCamera.this.printTimeLog = false;
                                        }
                                    }
                                });
                            } else {
                                if (image != null) {
                                    image.close();
                                }
                                return;
                            }
                        } catch (Exception ex) {
                            str = FaceCamera.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Catch un-handle exception ");
                            stringBuilder.append(ex.getMessage());
                            LogUtil.e(str, stringBuilder.toString());
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } else {
                        LogUtil.d(FaceCamera.TAG, "Image is null.");
                    }
                    if (image != null) {
                        image.close();
                    }
                } catch (Exception ex2) {
                    str = FaceCamera.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Catch un-handle image exception ");
                    stringBuilder.append(ex2.getMessage());
                    LogUtil.e(str, stringBuilder.toString());
                } catch (Throwable th22) {
                    th.addSuppressed(th22);
                }
            }
        }
    };
    private Builder mPreviewRequestBuilder;
    private SensorManager mSensorManager;
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        private boolean needSendRequestMsg = true;

        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            LogUtil.i(FaceCamera.TAG, "cb - onConfigured");
            long current = System.nanoTime();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Time 3.1. call-back create session --- ");
            stringBuilder.append(current);
            LogUtil.d("PerformanceTime", stringBuilder.toString());
            if (FaceCamera.this.mCameraDevice == null) {
                FaceCamera.this.updateStateTo(0);
                FaceCamera.this.handleMessage(10);
                return;
            }
            if (FaceCamera.this.updateStateTo(5)) {
                FaceCamera.this.mCaptureSession = cameraCaptureSession;
                FaceCamera.this.handleMessage(11);
            }
        }

        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            LogUtil.i(FaceCamera.TAG, "cb - onConfiguredFailed");
            if (FaceCamera.this.updateStateTo(0)) {
                FaceCamera.this.handleMessage(12);
            }
        }

        public void onReady(CameraCaptureSession session) {
            LogUtil.i(FaceCamera.TAG, "cb - onReady");
            this.needSendRequestMsg = true;
        }

        public void onActive(CameraCaptureSession session) {
            LogUtil.i(FaceCamera.TAG, "cb - onActive");
            long current = System.nanoTime();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Time 4.1. call-back create request --- ");
            stringBuilder.append(current);
            LogUtil.d("PerformanceTime", stringBuilder.toString());
            if ("com.android.systemui".equals(FaceCamera.this.mContext.getOpPackageName())) {
                LogUtil.d(FaceCamera.TAG, "Big data report open");
                long now = System.currentTimeMillis();
                Context access$700 = FaceCamera.this.mContext;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("{\"open_camera_cost_ms\":\"");
                stringBuilder2.append(now - FaceCamera.this.mCurrentTime);
                stringBuilder2.append("\"}");
                Flog.bdReport(access$700, FaceCamera.BD_REPORT_EVNET_ID_OPEN, stringBuilder2.toString());
                FaceCamera.this.mCurrentTime = now;
                FaceCamera.this.mIsImageReported = false;
            } else {
                String str = FaceCamera.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Pkg name : ");
                stringBuilder3.append(FaceCamera.this.mContext.getOpPackageName());
                LogUtil.d(str, stringBuilder3.toString());
                FaceCamera.this.mIsImageReported = false;
            }
            if (this.needSendRequestMsg) {
                this.needSendRequestMsg = false;
                FaceCamera.this.handleMessage(20);
            }
        }
    };
    private int mState = 0;
    private final Object mStateLock = new Object();
    private volatile boolean printTimeLog = false;

    public static native int native_send_image(byte[] bArr);

    static {
        try {
            System.loadLibrary("FaceRecognizeSendImage");
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LoadLibrary occurs error ");
            stringBuilder.append(e.toString());
            LogUtil.e(str, stringBuilder.toString());
        }
    }

    private synchronized void registerLightSensorListener() {
        LogUtil.d(TAG, "registerLightSensorListener");
        if (this.mSensorManager == null) {
            this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        }
        this.mSensorManager.registerListener(mLightSensorListener, this.mSensorManager.getDefaultSensor(5), 3);
    }

    private synchronized void unRegisterLightSensorListener() {
        if (this.mSensorManager != null) {
            this.mSensorManager.unregisterListener(mLightSensorListener);
            this.mSensorManager = null;
            LogUtil.w(TAG, "unRegisterLightSensorListener");
        }
    }

    private boolean increaseAndCheckImageCount() {
        boolean z;
        synchronized (this.mImageCountLock) {
            int tmp = this.mImageCount;
            this.mImageCount = tmp + 1;
            z = tmp % 2 != 0;
        }
        return z;
    }

    private void resetImageCount() {
        synchronized (this.mImageCountLock) {
            this.mImageCount = 0;
        }
    }

    private ByteBuffer cloneByteBuffer(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }

    private void startBackgroundThread() {
        if (this.mBackgroundThread == null) {
            this.mBackgroundThread = new HandlerThread("ImageExtractorThread");
            this.mBackgroundThread.start();
            this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
            resetImageCount();
        }
    }

    private void stopBackgroundThread() {
        if (this.mBackgroundThread != null) {
            this.mBackgroundThread.quit();
            try {
                this.mBackgroundThread.join();
                this.mBackgroundThread = null;
                this.mBackgroundHandler = null;
                resetImageCount();
            } catch (InterruptedException e) {
                LogUtil.e(TAG, "Stop background thread occurs InterruptedException");
            }
        }
    }

    private byte[] getDataFromImage(Rect crop, ByteBuffer[] planeArray, int[] rowStrideArray, int[] pixelStrideArray) {
        Rect rect = crop;
        ByteBuffer[] byteBufferArr = planeArray;
        byte[] data = new byte[(((crop.width() * crop.height()) * ImageFormat.getBitsPerPixel(35)) / 8)];
        int i = 0;
        byte[] rowData = new byte[rowStrideArray[0]];
        int offsetV = 0;
        int offsetU = 0;
        int offset = 0;
        ByteBuffer buffer = null;
        int i2 = 0;
        while (i2 < byteBufferArr.length) {
            int shift = i2 == 0 ? i : 1;
            buffer = byteBufferArr[i2];
            int rowStride = rowStrideArray[i2];
            int pixelStride = pixelStrideArray[i2];
            i = crop.width() >> shift;
            int h = crop.height() >> shift;
            buffer.position(((rect.top >> shift) * rowStride) + ((rect.left >> shift) * pixelStride));
            int row = 0;
            while (row < h) {
                int length;
                int bytesPerPixel = ImageFormat.getBitsPerPixel(35) / 8;
                if (pixelStride == bytesPerPixel) {
                    length = i * bytesPerPixel;
                    buffer.get(data, offset, length);
                    offset += length;
                    offsetU = offset + 1;
                    offsetV = offset;
                    int i3 = bytesPerPixel;
                } else {
                    length = ((i - 1) * pixelStride) + bytesPerPixel;
                    buffer.get(rowData, 0, length);
                    if (i2 == 1) {
                        int offsetU2 = offsetU;
                        for (offsetU = 0; offsetU < i; offsetU++) {
                            data[offsetU2] = rowData[offsetU * pixelStride];
                            offsetU2 += 2;
                        }
                        offsetU = offsetU2;
                    }
                    if (i2 == 2) {
                        for (bytesPerPixel = 0; bytesPerPixel < i; bytesPerPixel++) {
                            data[offsetV] = rowData[bytesPerPixel * pixelStride];
                            offsetV += 2;
                        }
                    }
                }
                if (row < h - 1) {
                    buffer.position((buffer.position() + rowStride) - length);
                }
                row++;
                rect = crop;
            }
            i2++;
            rect = crop;
            byteBufferArr = planeArray;
            i = 0;
        }
        return data;
    }

    private FaceCamera() {
        this.mCameraHandlerThread.start();
        this.mCameraHandler = new Handler(this.mCameraHandlerThread.getLooper());
    }

    public static synchronized FaceCamera getInstance() {
        FaceCamera faceCamera;
        synchronized (FaceCamera.class) {
            if (sInstance == null) {
                sInstance = new FaceCamera();
            }
            faceCamera = sInstance;
        }
        return faceCamera;
    }

    public void init(Context context) {
        this.mContext = context;
    }

    public int openCamera(int requestType) {
        LogUtil.d(TAG, "call openCamera");
        long current = System.nanoTime();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time 2. call open camera --- ");
        stringBuilder.append(current);
        LogUtil.d("PerformanceTime", stringBuilder.toString());
        int checkOpRlt = checkOperation(0);
        if (checkOpRlt != 1) {
            return checkOpRlt;
        }
        updateStateTo(3);
        CameraManager manager = (CameraManager) this.mContext.getSystemService("camera");
        if (!"com.android.systemui".equals(this.mContext.getPackageName())) {
            registerLightSensorListener();
        }
        try {
            String[] cameraList = manager.getCameraIdList();
            if (cameraList != null) {
                if (cameraList.length != 0) {
                    for (int index = cameraList.length - 1; index > 0; index--) {
                        String cameraId = cameraList[index];
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        Integer facing = (Integer) characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null) {
                            if (facing.intValue() == 0) {
                                this.mCameraId = cameraId;
                                this.mCharacteristics = characteristics;
                                break;
                            }
                        }
                    }
                    if (this.mCameraOpenCloseLock.tryAcquire(300, TimeUnit.MILLISECONDS)) {
                        this.mCurrentTime = System.currentTimeMillis();
                        if (SUPPORT_FACE_MODE == 0 || ServiceHolder.getInstance().setSecureFaceMode((requestType << 1) | 1) == 0) {
                            manager.openCamera(this.mCameraId, this.mCameraStateCallback, this.mCameraHandler);
                            this.fpsRanges = (Range[]) this.mCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                            startBackgroundThread();
                            return 1;
                        }
                        LogUtil.e(TAG, "setSecureMode failed");
                        handleMessage(2);
                        updateStateTo(0);
                        unRegisterLightSensorListener();
                        this.mCameraOpenCloseLock.release();
                        return 2;
                    }
                    handleMessage(1);
                    updateStateTo(0);
                    LogUtil.e(TAG, "Query CameraOpenCloseLock Timeout!");
                    return 2;
                }
            }
            return 2;
        } catch (CameraAccessException | IllegalArgumentException | InterruptedException | SecurityException e) {
            unRegisterLightSensorListener();
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Occurs error ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(str, stringBuilder2.toString());
            updateStateTo(0);
            this.mCameraOpenCloseLock.drainPermits();
            this.mCameraOpenCloseLock.release();
            return 2;
        } catch (Exception e2) {
            unRegisterLightSensorListener();
            updateStateTo(0);
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Occurs un-handle error ");
            stringBuilder3.append(e2.getMessage());
            LogUtil.e(str2, stringBuilder3.toString());
            this.mCameraOpenCloseLock.drainPermits();
            this.mCameraOpenCloseLock.release();
            return 2;
        }
    }

    public int createPreviewSession(List<Surface> surfaces) {
        String str;
        StringBuilder stringBuilder;
        LogUtil.d(TAG, "call createPreviewSession");
        long current = System.nanoTime();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Time 3. call create session --- ");
        stringBuilder2.append(current);
        LogUtil.d("PerformanceTime", stringBuilder2.toString());
        int checkOpRlt = checkOperation(1);
        if (checkOpRlt != 1) {
            return checkOpRlt;
        }
        updateStateTo(4);
        try {
            this.mPreviewRequestBuilder = this.mCameraDevice.createCaptureRequest(1);
            synchronized (this.mImageReaderLock) {
                this.mImageReader = ImageReader.newInstance(WIDTH, HEIGHT, 35, 2);
                this.mImageReader.setOnImageAvailableListener(this.mOnImageAvailableListener, this.mCameraHandler);
                ArrayList<Surface> tmpSurfaces = new ArrayList();
                for (Surface singleSurface : surfaces) {
                    this.mPreviewRequestBuilder.addTarget(singleSurface);
                    tmpSurfaces.add(singleSurface);
                }
                if (SUPPORT_FACE_MODE == 0 || tmpSurfaces.isEmpty()) {
                    tmpSurfaces.add(this.mImageReader.getSurface());
                    this.mPreviewRequestBuilder.addTarget(this.mImageReader.getSurface());
                    LogUtil.d(TAG, "add image surface.");
                }
                this.mCameraDevice.createCaptureSession(tmpSurfaces, this.mSessionStateCallback, this.mCameraHandler);
            }
            return 1;
        } catch (CameraAccessException | IllegalArgumentException | IllegalStateException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Preview occurs un-handle error ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(str, stringBuilder.toString());
            return 2;
        } catch (RuntimeException ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Preview occurs un-handle error ");
            stringBuilder.append(ex.getMessage());
            LogUtil.e(str, stringBuilder.toString());
            return 2;
        }
    }

    public int createPreviewRequest(int requestType, int flag) {
        String str;
        StringBuilder stringBuilder;
        LogUtil.d(TAG, "call createPreviewRequest");
        long current = System.nanoTime();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Time 4. call create request --- ");
        stringBuilder2.append(current);
        LogUtil.d("PerformanceTime", stringBuilder2.toString());
        if (this.mCaptureSession == null || this.mPreviewRequestBuilder == null) {
            return 2;
        }
        int checkOpRlt = checkOperation(2);
        if (checkOpRlt != 1) {
            return checkOpRlt;
        }
        this.printTimeLog = true;
        int bioFaceMode = 1;
        int index = 0;
        if (requestType == 0) {
            try {
                this.isEnrolling = true;
            } catch (CameraAccessException | IllegalArgumentException | IllegalStateException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Request occurs un-handle error ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(str, stringBuilder.toString());
                return 2;
            } catch (RuntimeException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Request occurs un-handle error ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(str, stringBuilder.toString());
                return 2;
            }
        } else if (requestType == 1) {
            this.isEnrolling = false;
            bioFaceMode = flag == 1 ? 3 : 2;
        }
        if (SUPPORT_FACE_MODE == 4) {
            if (1 == bioFaceMode) {
                bioFaceMode = 4;
            } else {
                bioFaceMode = 5;
            }
        }
        if (SUPPORT_FACE_MODE != 0) {
            LogUtil.d(TAG, String.format("camera mode set mode:%d", new Object[]{Integer.valueOf(bioFaceMode)}));
            this.mPreviewRequestBuilder.set(ANDROID_HW_BIO_FACE_MODE, Integer.valueOf(bioFaceMode));
        }
        StringBuilder stringBuilder3;
        if (SUPPORT_FACE_MODE == 4) {
            LogUtil.d(TAG, "Range : set range");
            if (4 == bioFaceMode) {
                index = new Range(Integer.valueOf(30), Integer.valueOf(30));
            } else {
                index = new Range(Integer.valueOf(SECURE_CAMERA_WITH_3D_AUTH_FPS), Integer.valueOf(SECURE_CAMERA_WITH_3D_AUTH_FPS));
            }
            this.mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, index);
            String str2 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Range : set to success.");
            stringBuilder3.append(index.toString());
            LogUtil.d(str2, stringBuilder3.toString());
        } else if (SUPPORT_FACE_MODE != 3) {
            Range<Integer> targetFps = null;
            if (this.fpsRanges != null) {
                LogUtil.d(TAG, "Range : set range");
                while (index < this.fpsRanges.length) {
                    Range<Integer> fpsRange = this.fpsRanges[index];
                    if (fpsRange != null && (targetFps == null || (((Integer) fpsRange.getUpper()).intValue() <= ((Integer) targetFps.getUpper()).intValue() && ((Integer) fpsRange.getLower()).intValue() <= ((Integer) targetFps.getLower()).intValue()))) {
                        targetFps = fpsRange;
                    }
                    index++;
                }
            }
            if (targetFps != null) {
                this.mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetFps);
                String str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Range : set to success.");
                stringBuilder3.append(targetFps.toString());
                LogUtil.d(str3, stringBuilder3.toString());
            }
        }
        this.mCaptureSession.setRepeatingRequest(this.mPreviewRequestBuilder.build(), null, this.mCameraHandler);
        return 1;
    }

    public boolean close() {
        LogUtil.d(TAG, "call close");
        long current = System.nanoTime();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time 5. call close camera --- ");
        stringBuilder.append(current);
        LogUtil.d("PerformanceTime", stringBuilder.toString());
        int checkOpRlt = checkOperation(3);
        if (checkOpRlt == 0) {
            return true;
        }
        if (checkOpRlt == 2) {
            return false;
        }
        updateStateTo(2);
        unRegisterLightSensorListener();
        closeImageReader();
        closeCamera();
        try {
            if (!this.mCameraCloseLock.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                LogUtil.w(TAG, "Close Camera out of time.");
            }
        } catch (InterruptedException e) {
            LogUtil.e(TAG, "InterruptedException occurs on closing.");
        } catch (Throwable th) {
            updateStateTo(0);
            this.mCameraCloseLock.release();
        }
        updateStateTo(0);
        this.mCameraCloseLock.release();
        long current2 = System.nanoTime();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Time 5.1. call-back close camera --- ");
        stringBuilder2.append(current2);
        LogUtil.d("PerformanceTime", stringBuilder2.toString());
        return true;
    }

    private void closeImageReader() {
        synchronized (this.mImageReaderLock) {
            if (this.mImageReader != null) {
                LogUtil.d(TAG, "Close image surface.");
                this.mImageReader.close();
                this.mImageReader = null;
            }
        }
    }

    private void handleMessage(int msgType) {
        handleMessage(msgType, -1);
    }

    private void handleMessage(int msgType, int msgCode) {
        if (msgType != 20) {
            switch (msgType) {
                case 0:
                    LogUtil.d(TAG, "MSG_OPEN_CAMERA_OK");
                    HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(3, 1000)), 2);
                    return;
                case 1:
                    LogUtil.d(TAG, "MSG_OPEN_CAMERA_TIME_OUT");
                    HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(3, 1002)), 2);
                    return;
                case 2:
                    LogUtil.d(TAG, "MSG_OPEN_CAMERA_ERROR");
                    HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(3, 1001)), 2);
                    return;
                case 3:
                    LogUtil.d(TAG, "MSG_CAMERA_DISCONNECTED");
                    HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(6, new long[0])), 2);
                    return;
                case 4:
                    LogUtil.d(TAG, "MSG_CAMERA_CLOSED");
                    return;
                default:
                    switch (msgType) {
                        case 10:
                            LogUtil.d(TAG, "MSG_CREATE_SESSION_CAMERA_CLOSED");
                            return;
                        case 11:
                            LogUtil.d(TAG, "MSG_CREATE_SESSION_OK");
                            HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(4, 1003)), 2);
                            return;
                        case 12:
                            LogUtil.d(TAG, "MSG_CREATE_SESSION_FAILED");
                            HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(4, 1004)), 2);
                            return;
                        default:
                            return;
                    }
            }
        }
        LogUtil.d(TAG, "MSG_CREATE_REQUEST_OK");
        HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new FaceRecognizeEvent(5, 1005)), 2);
    }

    private void closeCamera() {
        String str;
        StringBuilder stringBuilder;
        try {
            this.mCameraCloseLock.acquire();
            if (!this.mCameraOpenCloseLock.tryAcquire(3000, TimeUnit.MILLISECONDS)) {
                LogUtil.e(TAG, "Lock not released, recycle resources now");
                updateStateTo(0);
                stopBackgroundThread();
                this.mCameraCloseLock.release();
            }
            if (this.mCaptureSession != null) {
                try {
                    this.mCaptureSession.abortCaptures();
                } catch (CameraAccessException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("abort capture CameraAccessException error ");
                    stringBuilder.append(e);
                    LogUtil.e(str, stringBuilder.toString());
                } catch (IllegalStateException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("abort capture IllegalStateException error ");
                    stringBuilder.append(e2);
                    LogUtil.e(str, stringBuilder.toString());
                }
                this.mCaptureSession.close();
                this.mCaptureSession = null;
            }
            if (this.mCameraDevice != null) {
                this.mCameraDevice.close();
                this.mCameraDevice = null;
            }
        } catch (InterruptedException e3) {
        } catch (Throwable th) {
            this.mCameraOpenCloseLock.release();
        }
        this.mCameraOpenCloseLock.release();
    }

    private boolean updateStateTo(int toState) {
        synchronized (this.mStateLock) {
            String oldState = getCurState();
            String str;
            StringBuilder stringBuilder;
            if (this.mState != 2) {
                this.mState = toState;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(oldState);
                stringBuilder.append("--> ");
                stringBuilder.append(getCurState());
                LogUtil.d(str, stringBuilder.toString());
                return true;
            } else if (toState != 0) {
                return false;
            } else {
                this.mState = toState;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(oldState);
                stringBuilder.append("--> ");
                stringBuilder.append(getCurState());
                LogUtil.d(str, stringBuilder.toString());
                return true;
            }
        }
    }

    /* JADX WARNING: Missing block: B:49:0x00df, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:68:0x012d, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int checkOperation(int operation) {
        synchronized (this.mStateLock) {
            String str;
            StringBuilder stringBuilder;
            String str2;
            switch (operation) {
                case 0:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("curState = ");
                    stringBuilder.append(getCurState());
                    stringBuilder.append(", Operate = ");
                    stringBuilder.append(getOperate(operation));
                    stringBuilder.append(" = ");
                    stringBuilder.append(this.mState == 0 ? "ok" : "fail");
                    LogUtil.d(str, stringBuilder.toString());
                    if (!(this.mState == 1 || this.mState == 4)) {
                        if (this.mState == 5) {
                            break;
                        } else if (this.mState == 0) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }
                case 1:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("curState = ");
                    stringBuilder.append(getCurState());
                    stringBuilder.append(", Operate = ");
                    stringBuilder.append(getOperate(operation));
                    stringBuilder.append(" = ");
                    stringBuilder.append(this.mState == 1 ? "ok" : "fail");
                    LogUtil.d(str, stringBuilder.toString());
                    if (this.mState != 5) {
                        if (this.mState == 4) {
                            break;
                        } else if (this.mState == 1) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }
                    break;
                case 2:
                    str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("curState = ");
                    stringBuilder2.append(getCurState());
                    stringBuilder2.append(", Operate = ");
                    stringBuilder2.append(getOperate(operation));
                    stringBuilder2.append(" = ");
                    stringBuilder2.append(this.mState == 5 ? "ok" : "fail");
                    LogUtil.d(str2, stringBuilder2.toString());
                    if (this.mState == 5) {
                        return 1;
                    }
                    return 2;
                case 3:
                    str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("curState = ");
                    stringBuilder3.append(getCurState());
                    stringBuilder3.append(", Operate = ");
                    stringBuilder3.append(getOperate(operation));
                    stringBuilder3.append(" = ");
                    str = (this.mState == 2 || this.mState == 0) ? "fail" : "ok";
                    stringBuilder3.append(str);
                    LogUtil.d(str2, stringBuilder3.toString());
                    if (this.mState == 0) {
                        return 0;
                    } else if (this.mState == 2) {
                        return 2;
                    } else {
                        return 1;
                    }
                default:
                    return 1;
            }
        }
    }

    private String getOperate(int operation) {
        switch (operation) {
            case 0:
                return "OP_OPEN_CAMERA";
            case 1:
                return "OP_CREATE_SESSION";
            case 2:
                return "OP_SEND_REQUEST";
            case 3:
                return "OP_CLOSE_CAMERA";
            default:
                return "ERROR_OP";
        }
    }

    private String getCurState() {
        switch (this.mState) {
            case 0:
                return "CAMERA_IDLE";
            case 1:
                return "CAMERA_READY";
            case 2:
                return "CAMERA_CLOSING";
            case 3:
                return "CAMERA_OPENING";
            case 4:
                return "SESSION_CREATING";
            case 5:
                return "SESSION_CREATED";
            case 6:
                return "REQUEST_SENDING";
            case 7:
                return "REQUEST_WORKING";
            default:
                return "ERROR_STATE";
        }
    }
}

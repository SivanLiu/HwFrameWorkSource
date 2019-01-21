package android.hardware.camera2.impl;

import android.app.ActivityThread;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.ICameraDeviceCallbacks.Stub;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.SubmitInfo;
import android.hardware.camera2.utils.SurfaceUtils;
import android.hsm.HwSystemManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraDeviceImpl extends CameraDevice implements DeathRecipient {
    private static final long NANO_PER_SECOND = 1000000000;
    private static final int REQUEST_ID_NONE = -1;
    private final boolean DEBUG = false;
    private final String TAG;
    private final int mAppTargetSdkVersion;
    private final Runnable mCallOnActive = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onActive(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final Runnable mCallOnBusy = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onBusy(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final Runnable mCallOnClosed = new Runnable() {
        private boolean mClosedOnce = false;

        public void run() {
            if (this.mClosedOnce) {
                throw new AssertionError("Don't post #onClosed more than once");
            }
            StateCallbackKK sessionCallback;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onClosed(CameraDeviceImpl.this);
            }
            CameraDeviceImpl.this.mDeviceCallback.onClosed(CameraDeviceImpl.this);
            this.mClosedOnce = true;
        }
    };
    private final Runnable mCallOnDisconnected = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onDisconnected(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            android.hardware.camera2.impl.CameraDeviceImpl.access$200(r3.this$0).onDisconnected(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:12:0x002a, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final Runnable mCallOnIdle = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onIdle(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final Runnable mCallOnOpened = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onOpened(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            android.hardware.camera2.impl.CameraDeviceImpl.access$200(r3.this$0).onOpened(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:12:0x002a, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final Runnable mCallOnUnconfigured = new Runnable() {
        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            if (r0 == null) goto L_0x001f;
     */
        /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            r0.onUnconfigured(r3.this$0);
     */
        /* JADX WARNING: Missing block: B:11:0x001f, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
        }
    };
    private final CameraDeviceCallbacks mCallbacks = new CameraDeviceCallbacks();
    private final String mCameraId;
    private final SparseArray<CaptureCallbackHolder> mCaptureCallbackMap = new SparseArray();
    private final CameraCharacteristics mCharacteristics;
    private final AtomicBoolean mClosing = new AtomicBoolean();
    private SimpleEntry<Integer, InputConfiguration> mConfiguredInput = new SimpleEntry(Integer.valueOf(-1), null);
    private final SparseArray<OutputConfiguration> mConfiguredOutputs = new SparseArray();
    private CameraCaptureSessionCore mCurrentSession;
    private final StateCallback mDeviceCallback;
    private final Executor mDeviceExecutor;
    private final FrameNumberTracker mFrameNumberTracker = new FrameNumberTracker();
    private boolean mIdle = true;
    private boolean mInError = false;
    final Object mInterfaceLock = new Object();
    private int mNextSessionId = 0;
    private ICameraDeviceUserWrapper mRemoteDevice;
    private int mRepeatingRequestId = -1;
    private final List<RequestLastFrameNumbersHolder> mRequestLastFrameNumbersList = new ArrayList();
    private volatile StateCallbackKK mSessionStateCallback;
    private final int mTotalPartialCount;

    private static class CameraHandlerExecutor implements Executor {
        private final Handler mHandler;

        public CameraHandlerExecutor(Handler handler) {
            this.mHandler = (Handler) Preconditions.checkNotNull(handler);
        }

        public void execute(Runnable command) {
            this.mHandler.post(command);
        }
    }

    public interface CaptureCallback {
        public static final int NO_FRAMES_CAPTURED = -1;

        void onCaptureBufferLost(CameraDevice cameraDevice, CaptureRequest captureRequest, Surface surface, long j);

        void onCaptureCompleted(CameraDevice cameraDevice, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);

        void onCaptureFailed(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureFailure captureFailure);

        void onCapturePartial(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureResult captureResult);

        void onCaptureProgressed(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureResult captureResult);

        void onCaptureSequenceAborted(CameraDevice cameraDevice, int i);

        void onCaptureSequenceCompleted(CameraDevice cameraDevice, int i, long j);

        void onCaptureStarted(CameraDevice cameraDevice, CaptureRequest captureRequest, long j, long j2);
    }

    static class CaptureCallbackHolder {
        private final CaptureCallback mCallback;
        private final Executor mExecutor;
        private final boolean mHasBatchedOutputs;
        private final boolean mRepeating;
        private final List<CaptureRequest> mRequestList;
        private final int mSessionId;

        CaptureCallbackHolder(CaptureCallback callback, List<CaptureRequest> requestList, Executor executor, boolean repeating, int sessionId) {
            if (callback == null || executor == null) {
                throw new UnsupportedOperationException("Must have a valid handler and a valid callback");
            }
            this.mRepeating = repeating;
            this.mExecutor = executor;
            this.mRequestList = new ArrayList(requestList);
            this.mCallback = callback;
            this.mSessionId = sessionId;
            boolean hasBatchedOutputs = true;
            int i = 0;
            while (i < requestList.size()) {
                CaptureRequest request = (CaptureRequest) requestList.get(i);
                if (request.isPartOfCRequestList()) {
                    if (i == 0 && request.getTargets().size() != 2) {
                        hasBatchedOutputs = false;
                        break;
                    }
                    i++;
                } else {
                    hasBatchedOutputs = false;
                    break;
                }
            }
            this.mHasBatchedOutputs = hasBatchedOutputs;
        }

        public boolean isRepeating() {
            return this.mRepeating;
        }

        public CaptureCallback getCallback() {
            return this.mCallback;
        }

        public CaptureRequest getRequest(int subsequenceId) {
            if (subsequenceId >= this.mRequestList.size()) {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is larger than request list size %d.", new Object[]{Integer.valueOf(subsequenceId), Integer.valueOf(this.mRequestList.size())}));
            } else if (subsequenceId >= 0) {
                return (CaptureRequest) this.mRequestList.get(subsequenceId);
            } else {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is negative", new Object[]{Integer.valueOf(subsequenceId)}));
            }
        }

        public CaptureRequest getRequest() {
            return getRequest(0);
        }

        public Executor getExecutor() {
            return this.mExecutor;
        }

        public int getSessionId() {
            return this.mSessionId;
        }

        public int getRequestCount() {
            return this.mRequestList.size();
        }

        public boolean hasBatchedOutputs() {
            return this.mHasBatchedOutputs;
        }
    }

    public class FrameNumberTracker {
        private long mCompletedFrameNumber = -1;
        private long mCompletedReprocessFrameNumber = -1;
        private final TreeMap<Long, Boolean> mFutureErrorMap = new TreeMap();
        private final HashMap<Long, List<CaptureResult>> mPartialResults = new HashMap();
        private final LinkedList<Long> mSkippedRegularFrameNumbers = new LinkedList();
        private final LinkedList<Long> mSkippedReprocessFrameNumbers = new LinkedList();

        private void update() {
            Iterator iter = this.mFutureErrorMap.entrySet().iterator();
            while (iter.hasNext()) {
                Entry pair = (Entry) iter.next();
                Long errorFrameNumber = (Long) pair.getKey();
                Boolean reprocess = (Boolean) pair.getValue();
                Boolean removeError = Boolean.valueOf(true);
                if (reprocess.booleanValue()) {
                    if (errorFrameNumber.longValue() == this.mCompletedReprocessFrameNumber + 1) {
                        this.mCompletedReprocessFrameNumber = errorFrameNumber.longValue();
                    } else if (this.mSkippedReprocessFrameNumbers.isEmpty() || errorFrameNumber != this.mSkippedReprocessFrameNumbers.element()) {
                        removeError = Boolean.valueOf(false);
                    } else {
                        this.mCompletedReprocessFrameNumber = errorFrameNumber.longValue();
                        this.mSkippedReprocessFrameNumbers.remove();
                    }
                } else if (errorFrameNumber.longValue() == this.mCompletedFrameNumber + 1) {
                    this.mCompletedFrameNumber = errorFrameNumber.longValue();
                } else if (this.mSkippedRegularFrameNumbers.isEmpty() || errorFrameNumber != this.mSkippedRegularFrameNumbers.element()) {
                    removeError = Boolean.valueOf(false);
                } else {
                    this.mCompletedFrameNumber = errorFrameNumber.longValue();
                    this.mSkippedRegularFrameNumbers.remove();
                }
                if (removeError.booleanValue()) {
                    iter.remove();
                }
            }
        }

        public void updateTracker(long frameNumber, boolean isError, boolean isReprocess) {
            if (isError) {
                this.mFutureErrorMap.put(Long.valueOf(frameNumber), Boolean.valueOf(isReprocess));
            } else if (isReprocess) {
                try {
                    updateCompletedReprocessFrameNumber(frameNumber);
                } catch (IllegalArgumentException e) {
                    Log.e(CameraDeviceImpl.this.TAG, e.getMessage());
                }
            } else {
                updateCompletedFrameNumber(frameNumber);
            }
            update();
        }

        public void updateTracker(long frameNumber, CaptureResult result, boolean partial, boolean isReprocess) {
            if (!partial) {
                updateTracker(frameNumber, false, isReprocess);
            } else if (result != null) {
                List<CaptureResult> partials = (List) this.mPartialResults.get(Long.valueOf(frameNumber));
                if (partials == null) {
                    partials = new ArrayList();
                    this.mPartialResults.put(Long.valueOf(frameNumber), partials);
                }
                partials.add(result);
            }
        }

        public List<CaptureResult> popPartialResults(long frameNumber) {
            return (List) this.mPartialResults.remove(Long.valueOf(frameNumber));
        }

        public long getCompletedFrameNumber() {
            return this.mCompletedFrameNumber;
        }

        public long getCompletedReprocessFrameNumber() {
            return this.mCompletedReprocessFrameNumber;
        }

        private void updateCompletedFrameNumber(long frameNumber) throws IllegalArgumentException {
            StringBuilder stringBuilder;
            if (frameNumber > this.mCompletedFrameNumber) {
                if (frameNumber > this.mCompletedReprocessFrameNumber) {
                    long i = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber);
                    while (true) {
                        i++;
                        if (i >= frameNumber) {
                            break;
                        }
                        this.mSkippedReprocessFrameNumbers.add(Long.valueOf(i));
                    }
                } else if (this.mSkippedRegularFrameNumbers.isEmpty() || frameNumber < ((Long) this.mSkippedRegularFrameNumbers.element()).longValue()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("frame number ");
                    stringBuilder.append(frameNumber);
                    stringBuilder.append(" is a repeat");
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (frameNumber <= ((Long) this.mSkippedRegularFrameNumbers.element()).longValue()) {
                    this.mSkippedRegularFrameNumbers.remove();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("frame number ");
                    stringBuilder.append(frameNumber);
                    stringBuilder.append(" comes out of order. Expecting ");
                    stringBuilder.append(this.mSkippedRegularFrameNumbers.element());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.mCompletedFrameNumber = frameNumber;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("frame number ");
            stringBuilder.append(frameNumber);
            stringBuilder.append(" is a repeat");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        private void updateCompletedReprocessFrameNumber(long frameNumber) throws IllegalArgumentException {
            StringBuilder stringBuilder;
            if (frameNumber >= this.mCompletedReprocessFrameNumber) {
                if (frameNumber >= this.mCompletedFrameNumber) {
                    long i = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber);
                    while (true) {
                        i++;
                        if (i >= frameNumber) {
                            break;
                        }
                        this.mSkippedRegularFrameNumbers.add(Long.valueOf(i));
                    }
                } else if (this.mSkippedReprocessFrameNumbers.isEmpty() || frameNumber < ((Long) this.mSkippedReprocessFrameNumbers.element()).longValue()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("frame number ");
                    stringBuilder.append(frameNumber);
                    stringBuilder.append(" is a repeat");
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (frameNumber <= ((Long) this.mSkippedReprocessFrameNumbers.element()).longValue()) {
                    this.mSkippedReprocessFrameNumbers.remove();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("frame number ");
                    stringBuilder.append(frameNumber);
                    stringBuilder.append(" comes out of order. Expecting ");
                    stringBuilder.append(this.mSkippedReprocessFrameNumbers.element());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.mCompletedReprocessFrameNumber = frameNumber;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("frame number ");
            stringBuilder.append(frameNumber);
            stringBuilder.append(" is a repeat");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    static class RequestLastFrameNumbersHolder {
        private final long mLastRegularFrameNumber;
        private final long mLastReprocessFrameNumber;
        private final int mRequestId;

        public RequestLastFrameNumbersHolder(List<CaptureRequest> requestList, SubmitInfo requestInfo) {
            long lastRegularFrameNumber = -1;
            long lastReprocessFrameNumber = -1;
            long frameNumber = requestInfo.getLastFrameNumber();
            if (requestInfo.getLastFrameNumber() >= ((long) (requestList.size() - 1))) {
                for (int i = requestList.size() - 1; i >= 0; i--) {
                    CaptureRequest request = (CaptureRequest) requestList.get(i);
                    if (request.isReprocess() && lastReprocessFrameNumber == -1) {
                        lastReprocessFrameNumber = frameNumber;
                    } else if (!request.isReprocess() && lastRegularFrameNumber == -1) {
                        lastRegularFrameNumber = frameNumber;
                    }
                    if (lastReprocessFrameNumber != -1 && lastRegularFrameNumber != -1) {
                        break;
                    }
                    frameNumber--;
                }
                this.mLastRegularFrameNumber = lastRegularFrameNumber;
                this.mLastReprocessFrameNumber = lastReprocessFrameNumber;
                this.mRequestId = requestInfo.getRequestId();
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("lastFrameNumber: ");
            stringBuilder.append(requestInfo.getLastFrameNumber());
            stringBuilder.append(" should be at least ");
            stringBuilder.append(requestList.size() - 1);
            stringBuilder.append(" for the number of  requests in the list: ");
            stringBuilder.append(requestList.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public RequestLastFrameNumbersHolder(int requestId, long lastRegularFrameNumber) {
            this.mLastRegularFrameNumber = lastRegularFrameNumber;
            this.mLastReprocessFrameNumber = -1;
            this.mRequestId = requestId;
        }

        public long getLastRegularFrameNumber() {
            return this.mLastRegularFrameNumber;
        }

        public long getLastReprocessFrameNumber() {
            return this.mLastReprocessFrameNumber;
        }

        public long getLastFrameNumber() {
            return Math.max(this.mLastRegularFrameNumber, this.mLastReprocessFrameNumber);
        }

        public int getRequestId() {
            return this.mRequestId;
        }
    }

    public static abstract class StateCallbackKK extends StateCallback {
        public void onUnconfigured(CameraDevice camera) {
        }

        public void onActive(CameraDevice camera) {
        }

        public void onBusy(CameraDevice camera) {
        }

        public void onIdle(CameraDevice camera) {
        }

        public void onRequestQueueEmpty() {
        }

        public void onSurfacePrepared(Surface surface) {
        }
    }

    public class CameraDeviceCallbacks extends Stub {
        public IBinder asBinder() {
            return this;
        }

        /* JADX WARNING: Missing block: B:21:0x005d, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onDeviceError(int errorCode, CaptureResultExtras resultExtras) {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    switch (errorCode) {
                        case 0:
                            long ident = Binder.clearCallingIdentity();
                            try {
                                CameraDeviceImpl.this.mDeviceExecutor.execute(CameraDeviceImpl.this.mCallOnDisconnected);
                                break;
                            } finally {
                                Binder.restoreCallingIdentity(ident);
                            }
                        case 1:
                            scheduleNotifyError(4);
                            break;
                        case 3:
                        case 4:
                        case 5:
                            onCaptureErrorLocked(errorCode, resultExtras);
                            break;
                        case 6:
                            scheduleNotifyError(3);
                            break;
                        default:
                            String access$400 = CameraDeviceImpl.this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown error from camera device: ");
                            stringBuilder.append(errorCode);
                            Log.e(access$400, stringBuilder.toString());
                            scheduleNotifyError(5);
                            break;
                    }
                }
            }
        }

        private void scheduleNotifyError(int code) {
            CameraDeviceImpl.this.mInError = true;
            long ident = Binder.clearCallingIdentity();
            try {
                CameraDeviceImpl.this.mDeviceExecutor.execute(PooledLambda.obtainRunnable(-$$Lambda$CameraDeviceImpl$CameraDeviceCallbacks$Sm85frAzwGZVMAK-NE_gwckYXVQ.INSTANCE, this, Integer.valueOf(code)));
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        private void notifyError(int code) {
            if (!CameraDeviceImpl.this.isClosed()) {
                CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, code);
            }
        }

        /* JADX WARNING: Missing block: B:12:0x0030, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:14:0x0032, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onRepeatingRequestError(long lastFrameNumber, int repeatingRequestId) {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    if (CameraDeviceImpl.this.mRepeatingRequestId != -1) {
                        CameraDeviceImpl.this.checkEarlyTriggerSequenceComplete(CameraDeviceImpl.this.mRepeatingRequestId, lastFrameNumber);
                        if (CameraDeviceImpl.this.mRepeatingRequestId == repeatingRequestId) {
                            CameraDeviceImpl.this.mRepeatingRequestId = -1;
                        }
                    }
                }
            }
        }

        public void onDeviceIdle() {
            Log.i(CameraDeviceImpl.this.TAG, "Camera now idle");
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                if (!CameraDeviceImpl.this.mIdle) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        CameraDeviceImpl.this.mDeviceExecutor.execute(CameraDeviceImpl.this.mCallOnIdle);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                CameraDeviceImpl.this.mIdle = true;
            }
        }

        public void onCaptureStarted(CaptureResultExtras resultExtras, long timestamp) {
            Throwable th;
            int i;
            int requestId = resultExtras.getRequestId();
            long frameNumber = resultExtras.getFrameNumber();
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                try {
                    if (CameraDeviceImpl.this.mRemoteDevice == null) {
                        try {
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            i = requestId;
                            throw th;
                        }
                    }
                    CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                    if (holder == null) {
                    } else if (CameraDeviceImpl.this.isClosed()) {
                    } else {
                        long ident = Binder.clearCallingIdentity();
                        long ident2;
                        try {
                            AnonymousClass1 anonymousClass1 = anonymousClass1;
                            final CaptureResultExtras captureResultExtras = resultExtras;
                            final CaptureCallbackHolder captureCallbackHolder = holder;
                            final long j = timestamp;
                            ident2 = ident;
                            ident = frameNumber;
                            try {
                                holder.getExecutor().execute(new Runnable() {
                                    public void run() {
                                        if (!CameraDeviceImpl.this.isClosed()) {
                                            int subsequenceId = captureResultExtras.getSubsequenceId();
                                            CaptureRequest request = captureCallbackHolder.getRequest(subsequenceId);
                                            if (captureCallbackHolder.hasBatchedOutputs()) {
                                                Range<Integer> fpsRange = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                                                for (int i = 0; i < captureCallbackHolder.getRequestCount(); i++) {
                                                    captureCallbackHolder.getCallback().onCaptureStarted(CameraDeviceImpl.this, captureCallbackHolder.getRequest(i), j - ((((long) (subsequenceId - i)) * CameraDeviceImpl.NANO_PER_SECOND) / ((long) ((Integer) fpsRange.getUpper()).intValue())), ident - ((long) (subsequenceId - i)));
                                                }
                                                return;
                                            }
                                            captureCallbackHolder.getCallback().onCaptureStarted(CameraDeviceImpl.this, captureCallbackHolder.getRequest(captureResultExtras.getSubsequenceId()), j, ident);
                                        }
                                    }
                                });
                                Binder.restoreCallingIdentity(ident2);
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i = requestId;
                            ident2 = ident;
                            Binder.restoreCallingIdentity(ident2);
                            throw th;
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    i = requestId;
                    throw th;
                }
            }
        }

        public void onResultReceived(CameraMetadataNative result, CaptureResultExtras resultExtras, PhysicalCaptureResultInfo[] physicalResults) throws RemoteException {
            Throwable th;
            Object obj;
            int i;
            CameraMetadataNative cameraMetadataNative = result;
            int requestId = resultExtras.getRequestId();
            long frameNumber = resultExtras.getFrameNumber();
            Object obj2 = CameraDeviceImpl.this.mInterfaceLock;
            synchronized (obj2) {
                long j;
                try {
                    if (CameraDeviceImpl.this.mRemoteDevice == null) {
                        try {
                            return;
                        } catch (Throwable th2) {
                            th = th2;
                            obj = obj2;
                            j = frameNumber;
                            i = requestId;
                            throw th;
                        }
                    }
                    cameraMetadataNative.set(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE, (Size) CameraDeviceImpl.this.getCharacteristics().get(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE));
                    CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                    CaptureRequest request = holder.getRequest(resultExtras.getSubsequenceId());
                    boolean isPartialResult = resultExtras.getPartialResultCount() < CameraDeviceImpl.this.mTotalPartialCount;
                    boolean isReprocess = request.isReprocess();
                    if (holder == null) {
                        CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult, isReprocess);
                    } else if (CameraDeviceImpl.this.isClosed()) {
                        CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult, isReprocess);
                    } else {
                        CameraMetadataNative cameraMetadataNative2;
                        final CaptureCallbackHolder captureCallbackHolder;
                        CameraDeviceCallbacks cameraDeviceCallbacks;
                        CaptureResult finalResult;
                        Runnable resultDispatch;
                        if (holder.hasBatchedOutputs()) {
                            cameraMetadataNative2 = new CameraMetadataNative(cameraMetadataNative);
                        } else {
                            cameraMetadataNative2 = null;
                        }
                        final CameraMetadataNative resultCopy = cameraMetadataNative2;
                        Runnable resultDispatch2;
                        CaptureRequest captureRequest;
                        if (isPartialResult) {
                            CaptureResultExtras captureResultExtras = resultExtras;
                            final CaptureResult resultAsCapture = new CaptureResult(cameraMetadataNative, request, captureResultExtras);
                            captureCallbackHolder = holder;
                            final CaptureResultExtras captureResultExtras2 = captureResultExtras;
                            final CaptureRequest captureRequest2 = request;
                            resultDispatch2 = new Runnable() {
                                public void run() {
                                    if (!CameraDeviceImpl.this.isClosed()) {
                                        if (captureCallbackHolder.hasBatchedOutputs()) {
                                            for (int i = 0; i < captureCallbackHolder.getRequestCount(); i++) {
                                                captureCallbackHolder.getCallback().onCaptureProgressed(CameraDeviceImpl.this, captureCallbackHolder.getRequest(i), new CaptureResult(new CameraMetadataNative(resultCopy), captureCallbackHolder.getRequest(i), captureResultExtras2));
                                            }
                                            return;
                                        }
                                        captureCallbackHolder.getCallback().onCaptureProgressed(CameraDeviceImpl.this, captureRequest2, resultAsCapture);
                                    }
                                }
                            };
                            CameraMetadataNative cameraMetadataNative3 = resultCopy;
                            obj = obj2;
                            captureCallbackHolder = holder;
                            captureRequest = request;
                            j = frameNumber;
                            i = requestId;
                            cameraDeviceCallbacks = this;
                            finalResult = resultAsCapture;
                            resultDispatch = resultDispatch2;
                        } else {
                            List<CaptureResult> partialResults = CameraDeviceImpl.this.mFrameNumberTracker.popPartialResults(frameNumber);
                            captureCallbackHolder = holder;
                            CaptureRequest request2 = request;
                            final long sensorTimestamp = ((Long) cameraMetadataNative.get(CaptureResult.SENSOR_TIMESTAMP)).longValue();
                            j = frameNumber;
                            final Range<Integer> fpsRange = (Range) request2.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                            try {
                                final int subsequenceId = resultExtras.getSubsequenceId();
                                final CaptureResult resultAsCapture2 = new TotalCaptureResult(cameraMetadataNative, request2, resultExtras, partialResults, captureCallbackHolder.getSessionId(), physicalResults);
                                Runnable anonymousClass3 = anonymousClass3;
                                captureRequest = request2;
                                final CaptureCallbackHolder captureCallbackHolder2 = captureCallbackHolder;
                                obj = obj2;
                                final CameraMetadataNative cameraMetadataNative4 = resultCopy;
                                final CaptureResultExtras captureResultExtras3 = resultExtras;
                                final List<CaptureResult> list = partialResults;
                                cameraDeviceCallbacks = this;
                                final CaptureRequest captureRequest3 = captureRequest;
                                try {
                                    anonymousClass3 = new Runnable() {
                                        public void run() {
                                            if (!CameraDeviceImpl.this.isClosed()) {
                                                if (captureCallbackHolder2.hasBatchedOutputs()) {
                                                    for (int i = 0; i < captureCallbackHolder2.getRequestCount(); i++) {
                                                        cameraMetadataNative4.set(CaptureResult.SENSOR_TIMESTAMP, Long.valueOf(sensorTimestamp - ((((long) (subsequenceId - i)) * CameraDeviceImpl.NANO_PER_SECOND) / ((long) ((Integer) fpsRange.getUpper()).intValue()))));
                                                        captureCallbackHolder2.getCallback().onCaptureCompleted(CameraDeviceImpl.this, captureCallbackHolder2.getRequest(i), new TotalCaptureResult(new CameraMetadataNative(cameraMetadataNative4), captureCallbackHolder2.getRequest(i), captureResultExtras3, list, captureCallbackHolder2.getSessionId(), new PhysicalCaptureResultInfo[0]));
                                                    }
                                                    return;
                                                }
                                                captureCallbackHolder2.getCallback().onCaptureCompleted(CameraDeviceImpl.this, captureRequest3, resultAsCapture2);
                                            }
                                        }
                                    };
                                    resultDispatch2 = anonymousClass3;
                                    finalResult = resultAsCapture2;
                                } catch (Throwable th3) {
                                    th = th3;
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                obj = obj2;
                                i = requestId;
                                throw th;
                            }
                        }
                        frameNumber = Binder.clearCallingIdentity();
                        captureCallbackHolder.getExecutor().execute(resultDispatch);
                        Binder.restoreCallingIdentity(frameNumber);
                        CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(j, finalResult, isPartialResult, isReprocess);
                        if (!isPartialResult) {
                            CameraDeviceImpl.this.checkAndFireSequenceComplete();
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    obj = obj2;
                    j = frameNumber;
                    i = requestId;
                    throw th;
                }
            }
        }

        public void onPrepared(int streamId) {
            OutputConfiguration output;
            StateCallbackKK sessionCallback;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                output = (OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(streamId);
                sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (sessionCallback != null) {
                if (output == null) {
                    Log.w(CameraDeviceImpl.this.TAG, "onPrepared invoked for unknown output Surface");
                    return;
                }
                for (Surface surface : output.getSurfaces()) {
                    sessionCallback.onSurfacePrepared(surface);
                }
            }
        }

        public void onRequestQueueEmpty() {
            StateCallbackKK sessionCallback;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onRequestQueueEmpty();
            }
        }

        private void onCaptureErrorLocked(int errorCode, CaptureResultExtras resultExtras) {
            int i = errorCode;
            int requestId = resultExtras.getRequestId();
            int subsequenceId = resultExtras.getSubsequenceId();
            long frameNumber = resultExtras.getFrameNumber();
            final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
            final CaptureRequest request = holder.getRequest(subsequenceId);
            if (i == 5) {
                List<Surface> surfaces = ((OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(resultExtras.getErrorStreamId())).getSurfaces();
                Iterator it = surfaces.iterator();
                while (it.hasNext()) {
                    final Surface surface = (Surface) it.next();
                    if (request.containsTarget(surface)) {
                        final CaptureCallbackHolder captureCallbackHolder = holder;
                        final CaptureRequest captureRequest = request;
                        Surface surface2 = surface;
                        List<Surface> surfaces2 = surfaces;
                        Iterator it2 = it;
                        surfaces = frameNumber;
                        AnonymousClass4 failureDispatch = new Runnable() {
                            public void run() {
                                if (!CameraDeviceImpl.this.isClosed()) {
                                    captureCallbackHolder.getCallback().onCaptureBufferLost(CameraDeviceImpl.this, captureRequest, surface, surfaces);
                                }
                            }
                        };
                        long ident = Binder.clearCallingIdentity();
                        try {
                            holder.getExecutor().execute(failureDispatch);
                            AnonymousClass4 anonymousClass4 = failureDispatch;
                            surfaces = surfaces2;
                            it = it2;
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }
                }
                return;
            }
            boolean mayHaveBuffers = i == 4;
            int reason = (CameraDeviceImpl.this.mCurrentSession == null || !CameraDeviceImpl.this.mCurrentSession.isAborting()) ? 0 : 1;
            Runnable failureDispatch2 = null;
            boolean z = true;
            final CaptureFailure failure = new CaptureFailure(request, reason, mayHaveBuffers, requestId, frameNumber);
            Runnable failureDispatch3 = new Runnable() {
                public void run() {
                    if (!CameraDeviceImpl.this.isClosed()) {
                        holder.getCallback().onCaptureFailed(CameraDeviceImpl.this, request, failure);
                    }
                }
            };
            CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, z, request.isReprocess());
            CameraDeviceImpl.this.checkAndFireSequenceComplete();
            long ident2 = Binder.clearCallingIdentity();
            try {
                holder.getExecutor().execute(failureDispatch3);
                Runnable runnable = failureDispatch3;
            } finally {
                Binder.restoreCallingIdentity(ident2);
            }
        }
    }

    public CameraDeviceImpl(String cameraId, StateCallback callback, Executor executor, CameraCharacteristics characteristics, int appTargetSdkVersion) {
        if (cameraId == null || callback == null || executor == null || characteristics == null) {
            throw new IllegalArgumentException("Null argument given");
        }
        this.mCameraId = cameraId;
        this.mDeviceCallback = callback;
        this.mDeviceExecutor = executor;
        this.mCharacteristics = characteristics;
        this.mAppTargetSdkVersion = appTargetSdkVersion;
        String tag = String.format("CameraDevice-JV-%s", new Object[]{this.mCameraId});
        if (tag.length() > 23) {
            tag = tag.substring(0, 23);
        }
        this.TAG = tag;
        Integer partialCount = (Integer) this.mCharacteristics.get(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT);
        if (partialCount == null) {
            this.mTotalPartialCount = 1;
        } else {
            this.mTotalPartialCount = partialCount.intValue();
        }
    }

    public CameraDeviceCallbacks getCallbacks() {
        return this.mCallbacks;
    }

    public void setRemoteDevice(ICameraDeviceUser remoteDevice) throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            if (this.mInError) {
                return;
            }
            this.mRemoteDevice = new ICameraDeviceUserWrapper(remoteDevice);
            IBinder remoteDeviceBinder = remoteDevice.asBinder();
            if (remoteDeviceBinder != null) {
                try {
                    remoteDeviceBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    this.mDeviceExecutor.execute(this.mCallOnDisconnected);
                    throw new CameraAccessException(2, "The camera device has encountered a serious error");
                }
            }
            this.mDeviceExecutor.execute(this.mCallOnOpened);
            this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
        }
    }

    public void setRemoteFailure(ServiceSpecificException failure) {
        int failureCode = 4;
        boolean failureIsError = true;
        int i = failure.errorCode;
        if (i == 4) {
            failureIsError = false;
        } else if (i != 10) {
            switch (i) {
                case 6:
                    failureCode = 3;
                    break;
                case 7:
                    failureCode = 1;
                    break;
                case 8:
                    failureCode = 2;
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected failure in opening camera device: ");
                    stringBuilder.append(failure.errorCode);
                    stringBuilder.append(failure.getMessage());
                    Log.e(str, stringBuilder.toString());
                    break;
            }
        } else {
            failureCode = 4;
        }
        i = failureCode;
        final boolean isError = failureIsError;
        synchronized (this.mInterfaceLock) {
            this.mInError = true;
            this.mDeviceExecutor.execute(new Runnable() {
                public void run() {
                    if (isError) {
                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, i);
                    } else {
                        CameraDeviceImpl.this.mDeviceCallback.onDisconnected(CameraDeviceImpl.this);
                    }
                }
            });
        }
    }

    public String getId() {
        return this.mCameraId;
    }

    public void configureOutputs(List<Surface> outputs) throws CameraAccessException {
        ArrayList<OutputConfiguration> outputConfigs = new ArrayList(outputs.size());
        for (Surface s : outputs) {
            outputConfigs.add(new OutputConfiguration(s));
        }
        configureStreamsChecked(null, outputConfigs, 0, null);
    }

    public boolean configureStreamsChecked(InputConfiguration inputConfig, List<OutputConfiguration> outputs, int operatingMode, CaptureRequest sessionParams) throws CameraAccessException {
        if (outputs == null) {
            outputs = new ArrayList();
        }
        if (outputs.size() != 0 || inputConfig == null) {
            checkInputConfiguration(inputConfig);
            synchronized (this.mInterfaceLock) {
                checkIfCameraClosedOrInError();
                HashSet<OutputConfiguration> addSet = new HashSet(outputs);
                List<Integer> deleteList = new ArrayList();
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    int streamId = this.mConfiguredOutputs.keyAt(i);
                    OutputConfiguration outConfig = (OutputConfiguration) this.mConfiguredOutputs.valueAt(i);
                    if (outputs.contains(outConfig)) {
                        if (!outConfig.isDeferredConfiguration()) {
                            addSet.remove(outConfig);
                        }
                    }
                    deleteList.add(Integer.valueOf(streamId));
                }
                this.mDeviceExecutor.execute(this.mCallOnBusy);
                stopRepeating();
                try {
                    waitUntilIdle();
                    this.mRemoteDevice.beginConfigure();
                    InputConfiguration currentInputConfig = (InputConfiguration) this.mConfiguredInput.getValue();
                    if (inputConfig != currentInputConfig && (inputConfig == null || !inputConfig.equals(currentInputConfig))) {
                        if (currentInputConfig != null) {
                            this.mRemoteDevice.deleteStream(((Integer) this.mConfiguredInput.getKey()).intValue());
                            this.mConfiguredInput = new SimpleEntry(Integer.valueOf(-1), null);
                        }
                        if (inputConfig != null) {
                            this.mConfiguredInput = new SimpleEntry(Integer.valueOf(this.mRemoteDevice.createInputStream(inputConfig.getWidth(), inputConfig.getHeight(), inputConfig.getFormat())), inputConfig);
                        }
                    }
                    for (Integer streamId2 : deleteList) {
                        this.mRemoteDevice.deleteStream(streamId2.intValue());
                        this.mConfiguredOutputs.delete(streamId2.intValue());
                    }
                    for (OutputConfiguration outConfig2 : outputs) {
                        if (addSet.contains(outConfig2)) {
                            this.mConfiguredOutputs.put(this.mRemoteDevice.createStream(outConfig2), outConfig2);
                        }
                    }
                    if (sessionParams != null) {
                        this.mRemoteDevice.endConfigure(operatingMode, sessionParams.getNativeCopy());
                    } else {
                        this.mRemoteDevice.endConfigure(operatingMode, null);
                    }
                    if (1 != null) {
                        if (outputs.size() > 0) {
                            this.mDeviceExecutor.execute(this.mCallOnIdle);
                        }
                    }
                    this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                } catch (IllegalArgumentException e) {
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Stream configuration failed due to: ");
                    stringBuilder.append(e.getMessage());
                    Log.w(str, stringBuilder.toString());
                    if (null != null) {
                        if (outputs.size() > 0) {
                            this.mDeviceExecutor.execute(this.mCallOnIdle);
                            return false;
                        }
                    }
                    this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                    return false;
                } catch (CameraAccessException e2) {
                    if (e2.getReason() == 4) {
                        throw new IllegalStateException("The camera is currently busy. You must wait until the previous operation completes.", e2);
                    }
                    throw e2;
                } catch (Throwable th) {
                    if (null == null || outputs.size() <= 0) {
                        this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                    } else {
                        this.mDeviceExecutor.execute(this.mCallOnIdle);
                    }
                }
            }
            return true;
        }
        throw new IllegalArgumentException("cannot configure an input stream without any output streams");
    }

    public void createCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        List<OutputConfiguration> outConfigurations = new ArrayList(outputs.size());
        for (Surface surface : outputs) {
            outConfigurations.add(new OutputConfiguration(surface));
        }
        createCaptureSessionInternal(null, outConfigurations, callback, checkAndWrapHandler(handler), 0, null);
    }

    public void createCaptureSessionByOutputConfigurations(List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        createCaptureSessionInternal(null, new ArrayList(outputConfigurations), callback, checkAndWrapHandler(handler), 0, null);
    }

    public void createReprocessableCaptureSession(InputConfiguration inputConfig, List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (inputConfig != null) {
            List<OutputConfiguration> outConfigurations = new ArrayList(outputs.size());
            for (Surface surface : outputs) {
                outConfigurations.add(new OutputConfiguration(surface));
            }
            createCaptureSessionInternal(inputConfig, outConfigurations, callback, checkAndWrapHandler(handler), 0, null);
            return;
        }
        throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
    }

    public void createReprocessableCaptureSessionByConfigurations(InputConfiguration inputConfig, List<OutputConfiguration> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (inputConfig == null) {
            throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
        } else if (outputs != null) {
            List<OutputConfiguration> currentOutputs = new ArrayList();
            for (OutputConfiguration output : outputs) {
                currentOutputs.add(new OutputConfiguration(output));
            }
            createCaptureSessionInternal(inputConfig, currentOutputs, callback, checkAndWrapHandler(handler), 0, null);
        } else {
            throw new IllegalArgumentException("Output configurations cannot be null when creating a reprocessable capture session");
        }
    }

    public void createConstrainedHighSpeedCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (outputs == null || outputs.size() == 0 || outputs.size() > 2) {
            throw new IllegalArgumentException("Output surface list must not be null and the size must be no more than 2");
        }
        List<OutputConfiguration> outConfigurations = new ArrayList(outputs.size());
        for (Surface surface : outputs) {
            outConfigurations.add(new OutputConfiguration(surface));
        }
        createCaptureSessionInternal(null, outConfigurations, callback, checkAndWrapHandler(handler), 1, null);
    }

    public void createCustomCaptureSession(InputConfiguration inputConfig, List<OutputConfiguration> outputs, int operatingMode, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        List<OutputConfiguration> currentOutputs = new ArrayList();
        for (OutputConfiguration output : outputs) {
            currentOutputs.add(new OutputConfiguration(output));
        }
        createCaptureSessionInternal(inputConfig, currentOutputs, callback, checkAndWrapHandler(handler), operatingMode, null);
    }

    public void createCaptureSession(SessionConfiguration config) throws CameraAccessException {
        if (config != null) {
            List<OutputConfiguration> outputConfigs = config.getOutputConfigurations();
            if (outputConfigs == null) {
                throw new IllegalArgumentException("Invalid output configurations");
            } else if (config.getExecutor() != null) {
                createCaptureSessionInternal(config.getInputConfiguration(), outputConfigs, config.getStateCallback(), config.getExecutor(), config.getSessionType(), config.getSessionParameters());
                return;
            } else {
                throw new IllegalArgumentException("Invalid executor");
            }
        }
        throw new IllegalArgumentException("Invalid session configuration");
    }

    private void createCaptureSessionInternal(InputConfiguration inputConfig, List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Executor executor, int operatingMode, CaptureRequest sessionParams) throws CameraAccessException {
        Throwable th;
        InputConfiguration inputConfiguration = inputConfig;
        int i = operatingMode;
        synchronized (this.mInterfaceLock) {
            try {
                boolean configureSuccess;
                CameraAccessException pendingException;
                Surface input;
                CameraCaptureSessionCore newSession;
                checkIfCameraClosedOrInError();
                boolean isConstrainedHighSpeed = i == 1;
                if (isConstrainedHighSpeed) {
                    if (inputConfiguration != null) {
                        throw new IllegalArgumentException("Constrained high speed session doesn't support input configuration yet.");
                    }
                }
                if (this.mCurrentSession != null) {
                    this.mCurrentSession.replaceSessionClose();
                }
                Surface input2 = null;
                try {
                    boolean configureSuccess2 = configureStreamsChecked(inputConfiguration, outputConfigurations, i, sessionParams);
                    if (configureSuccess2 && inputConfiguration != null) {
                        input2 = this.mRemoteDevice.getInputSurface();
                    }
                    configureSuccess = configureSuccess2;
                    pendingException = null;
                    input = input2;
                } catch (CameraAccessException e) {
                    CameraAccessException pendingException2 = e;
                    input = null;
                    configureSuccess = false;
                    pendingException = pendingException2;
                }
                int i2;
                if (isConstrainedHighSpeed) {
                    ArrayList<Surface> surfaces = new ArrayList(outputConfigurations.size());
                    for (OutputConfiguration outConfig : outputConfigurations) {
                        surfaces.add(outConfig.getSurface());
                    }
                    StreamConfigurationMap config = (StreamConfigurationMap) getCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    SurfaceUtils.checkConstrainedHighSpeedSurfaces(surfaces, null, config);
                    i2 = this.mNextSessionId;
                    this.mNextSessionId = i2 + 1;
                    newSession = new CameraConstrainedHighSpeedCaptureSessionImpl(i2, callback, executor, this, this.mDeviceExecutor, configureSuccess, this.mCharacteristics);
                } else {
                    i2 = this.mNextSessionId;
                    this.mNextSessionId = i2 + 1;
                    newSession = new CameraCaptureSessionImpl(i2, input, callback, executor, this, this.mDeviceExecutor, configureSuccess);
                }
                this.mCurrentSession = newSession;
                if (pendingException == null) {
                    this.mSessionStateCallback = this.mCurrentSession.getDeviceStateCallback();
                    return;
                }
                throw pendingException;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void setSessionListener(StateCallbackKK sessionCallback) {
        synchronized (this.mInterfaceLock) {
            this.mSessionStateCallback = sessionCallback;
        }
    }

    private void overrideEnableZsl(CameraMetadataNative request, boolean newValue) {
        if (((Boolean) request.get(CaptureRequest.CONTROL_ENABLE_ZSL)) != null) {
            request.set(CaptureRequest.CONTROL_ENABLE_ZSL, Boolean.valueOf(newValue));
        }
    }

    public Builder createCaptureRequest(int templateType, Set<String> physicalCameraIdSet) throws CameraAccessException {
        Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            for (String physicalId : physicalCameraIdSet) {
                if (physicalId == getId()) {
                    throw new IllegalStateException("Physical id matches the logical id!");
                }
            }
            CameraMetadataNative templatedRequest = this.mRemoteDevice.createDefaultRequest(templateType);
            if (this.mAppTargetSdkVersion < 26 || templateType != 2) {
                overrideEnableZsl(templatedRequest, false);
            }
            builder = new Builder(templatedRequest, false, -1, getId(), physicalCameraIdSet);
        }
        return builder;
    }

    public Builder createCaptureRequest(int templateType) throws CameraAccessException {
        Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            CameraMetadataNative templatedRequest = this.mRemoteDevice.createDefaultRequest(templateType);
            if (this.mAppTargetSdkVersion < 26 || templateType != 2) {
                overrideEnableZsl(templatedRequest, false);
            }
            builder = new Builder(templatedRequest, false, -1, getId(), null);
        }
        return builder;
    }

    public Builder createReprocessCaptureRequest(TotalCaptureResult inputResult) throws CameraAccessException {
        Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            builder = new Builder(new CameraMetadataNative(inputResult.getNativeCopy()), true, inputResult.getSessionId(), getId(), null);
        }
        return builder;
    }

    public void prepare(Surface surface) throws CameraAccessException {
        if (surface != null) {
            synchronized (this.mInterfaceLock) {
                int streamId = -1;
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    if (((OutputConfiguration) this.mConfiguredOutputs.valueAt(i)).getSurfaces().contains(surface)) {
                        streamId = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                }
                if (streamId != -1) {
                    this.mRemoteDevice.prepare(streamId);
                } else {
                    throw new IllegalArgumentException("Surface is not part of this session");
                }
            }
            return;
        }
        throw new IllegalArgumentException("Surface is null");
    }

    public void prepare(int maxCount, Surface surface) throws CameraAccessException {
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        } else if (maxCount > 0) {
            synchronized (this.mInterfaceLock) {
                int streamId = -1;
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    if (surface == ((OutputConfiguration) this.mConfiguredOutputs.valueAt(i)).getSurface()) {
                        streamId = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                }
                if (streamId != -1) {
                    this.mRemoteDevice.prepare2(maxCount, streamId);
                } else {
                    throw new IllegalArgumentException("Surface is not part of this session");
                }
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid maxCount given: ");
            stringBuilder.append(maxCount);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void updateOutputConfiguration(OutputConfiguration config) throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            int streamId = -1;
            for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                if (config.getSurface() == ((OutputConfiguration) this.mConfiguredOutputs.valueAt(i)).getSurface()) {
                    streamId = this.mConfiguredOutputs.keyAt(i);
                    break;
                }
            }
            if (streamId != -1) {
                this.mRemoteDevice.updateOutputConfiguration(streamId, config);
                this.mConfiguredOutputs.put(streamId, config);
            } else {
                throw new IllegalArgumentException("Invalid output configuration");
            }
        }
    }

    public void tearDown(Surface surface) throws CameraAccessException {
        if (surface != null) {
            synchronized (this.mInterfaceLock) {
                int streamId = -1;
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    if (surface == ((OutputConfiguration) this.mConfiguredOutputs.valueAt(i)).getSurface()) {
                        streamId = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                }
                if (streamId != -1) {
                    this.mRemoteDevice.tearDown(streamId);
                } else {
                    throw new IllegalArgumentException("Surface is not part of this session");
                }
            }
            return;
        }
        throw new IllegalArgumentException("Surface is null");
    }

    public void finalizeOutputConfigs(List<OutputConfiguration> outputConfigs) throws CameraAccessException {
        if (outputConfigs == null || outputConfigs.size() == 0) {
            throw new IllegalArgumentException("deferred config is null or empty");
        }
        synchronized (this.mInterfaceLock) {
            for (OutputConfiguration config : outputConfigs) {
                int streamId = -1;
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    if (config.equals(this.mConfiguredOutputs.valueAt(i))) {
                        streamId = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                }
                if (streamId == -1) {
                    throw new IllegalArgumentException("Deferred config is not part of this session");
                } else if (config.getSurfaces().size() != 0) {
                    this.mRemoteDevice.finalizeOutputConfigurations(streamId, config);
                    this.mConfiguredOutputs.put(streamId, config);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("The final config for stream ");
                    stringBuilder.append(streamId);
                    stringBuilder.append(" must have at least 1 surface");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }
    }

    public int capture(CaptureRequest request, CaptureCallback callback, Executor executor) throws CameraAccessException {
        List<CaptureRequest> requestList = new ArrayList();
        requestList.add(request);
        return submitCaptureRequest(requestList, callback, executor, false);
    }

    public int captureBurst(List<CaptureRequest> requests, CaptureCallback callback, Executor executor) throws CameraAccessException {
        if (requests != null && !requests.isEmpty()) {
            return submitCaptureRequest(requests, callback, executor, false);
        }
        throw new IllegalArgumentException("At least one request must be given");
    }

    private void checkEarlyTriggerSequenceComplete(final int requestId, long lastFrameNumber) {
        if (lastFrameNumber == -1) {
            int index = this.mCaptureCallbackMap.indexOfKey(requestId);
            final CaptureCallbackHolder holder = index >= 0 ? (CaptureCallbackHolder) this.mCaptureCallbackMap.valueAt(index) : null;
            if (holder != null) {
                this.mCaptureCallbackMap.removeAt(index);
            }
            if (holder != null) {
                Runnable resultDispatch = new Runnable() {
                    public void run() {
                        if (!CameraDeviceImpl.this.isClosed()) {
                            holder.getCallback().onCaptureSequenceAborted(CameraDeviceImpl.this, requestId);
                        }
                    }
                };
                long ident = Binder.clearCallingIdentity();
                try {
                    holder.getExecutor().execute(resultDispatch);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                Log.w(this.TAG, String.format("did not register callback to request %d", new Object[]{Integer.valueOf(requestId)}));
                return;
            }
        }
        this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(requestId, lastFrameNumber));
        checkAndFireSequenceComplete();
    }

    private int submitCaptureRequest(List<CaptureRequest> requestList, CaptureCallback callback, Executor executor, boolean repeating) throws CameraAccessException {
        SubmitInfo requestInfo;
        List list = requestList;
        CaptureCallback captureCallback = callback;
        boolean z = repeating;
        Executor executor2 = checkExecutor(executor, captureCallback);
        for (CaptureRequest request : requestList) {
            if (request.getTargets().isEmpty()) {
                throw new IllegalArgumentException("Each request must have at least one Surface target");
            }
            for (Surface surface : request.getTargets()) {
                if (surface != null) {
                    for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                        OutputConfiguration configuration = (OutputConfiguration) this.mConfiguredOutputs.valueAt(i);
                        if (configuration.isForPhysicalCamera() && configuration.getSurfaces().contains(surface) && request.isReprocess()) {
                            throw new IllegalArgumentException("Reprocess request on physical stream is not allowed");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Null Surface targets are not allowed");
                }
            }
        }
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (z) {
                stopRepeating();
            }
            CaptureRequest[] requestArray = (CaptureRequest[]) list.toArray(new CaptureRequest[requestList.size()]);
            for (CaptureRequest request2 : requestArray) {
                request2.convertSurfaceToStreamId(this.mConfiguredOutputs);
            }
            requestInfo = this.mRemoteDevice.submitRequestList(requestArray, z);
            for (CaptureRequest request22 : requestArray) {
                request22.recoverStreamIdToSurface();
            }
            if (captureCallback != null) {
                SparseArray sparseArray = this.mCaptureCallbackMap;
                CaptureCallbackHolder captureCallbackHolder = r2;
                int requestId = requestInfo.getRequestId();
                CaptureCallbackHolder captureCallbackHolder2 = new CaptureCallbackHolder(captureCallback, list, executor2, z, this.mNextSessionId - 1);
                sparseArray.put(requestId, captureCallbackHolder);
            }
            if (z) {
                if (this.mRepeatingRequestId != -1) {
                    checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, requestInfo.getLastFrameNumber());
                }
                this.mRepeatingRequestId = requestInfo.getRequestId();
            } else {
                this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(list, requestInfo));
            }
            if (this.mIdle) {
                this.mDeviceExecutor.execute(this.mCallOnActive);
            }
            this.mIdle = false;
        }
        return requestInfo.getRequestId();
    }

    public int setRepeatingRequest(CaptureRequest request, CaptureCallback callback, Executor executor) throws CameraAccessException {
        List<CaptureRequest> requestList = new ArrayList();
        requestList.add(request);
        return submitCaptureRequest(requestList, callback, executor, true);
    }

    public int setRepeatingBurst(List<CaptureRequest> requests, CaptureCallback callback, Executor executor) throws CameraAccessException {
        if (requests != null && !requests.isEmpty()) {
            return submitCaptureRequest(requests, callback, executor, true);
        }
        throw new IllegalArgumentException("At least one request must be given");
    }

    public void stopRepeating() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (this.mRepeatingRequestId != -1) {
                int requestId = this.mRepeatingRequestId;
                this.mRepeatingRequestId = -1;
                try {
                    checkEarlyTriggerSequenceComplete(requestId, this.mRemoteDevice.cancelRequest(requestId));
                } catch (IllegalArgumentException e) {
                    return;
                }
            }
        }
    }

    private void waitUntilIdle() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (this.mRepeatingRequestId == -1) {
                this.mRemoteDevice.waitUntilIdle();
            } else {
                throw new IllegalStateException("Active repeating request ongoing");
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0034, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void flush() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            this.mDeviceExecutor.execute(this.mCallOnBusy);
            if (this.mIdle) {
                Log.i(this.TAG, "camera device is idle now!");
                this.mDeviceExecutor.execute(this.mCallOnIdle);
                return;
            }
            long lastFrameNumber = this.mRemoteDevice.flush();
            if (this.mRepeatingRequestId != -1) {
                checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, lastFrameNumber);
                this.mRepeatingRequestId = -1;
            }
        }
    }

    public void close() {
        synchronized (this.mInterfaceLock) {
            if (this.mClosing.getAndSet(true)) {
                return;
            }
            if (this.mRemoteDevice != null) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("close camera: ");
                stringBuilder.append(this.mCameraId);
                stringBuilder.append(", package name: ");
                stringBuilder.append(ActivityThread.currentOpPackageName());
                Log.i(str, stringBuilder.toString());
                HwSystemManager.notifyBackgroundMgr(ActivityThread.currentOpPackageName(), Binder.getCallingPid(), Binder.getCallingUid(), 0, 0);
                this.mRemoteDevice.disconnect();
                this.mRemoteDevice.unlinkToDeath(this, 0);
            }
            if (this.mRemoteDevice != null || this.mInError) {
                this.mDeviceExecutor.execute(this.mCallOnClosed);
            }
            this.mRemoteDevice = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void checkInputConfiguration(InputConfiguration inputConfig) {
        if (inputConfig != null) {
            int format;
            StreamConfigurationMap configMap = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int i = 0;
            boolean validFormat = false;
            for (int format2 : configMap.getInputFormats()) {
                if (format2 == inputConfig.getFormat()) {
                    validFormat = true;
                }
            }
            if (validFormat) {
                boolean validSize = false;
                Size[] inputSizes = configMap.getInputSizes(inputConfig.getFormat());
                format2 = inputSizes.length;
                while (i < format2) {
                    Size s = inputSizes[i];
                    if (inputConfig.getWidth() == s.getWidth() && inputConfig.getHeight() == s.getHeight()) {
                        validSize = true;
                    }
                    i++;
                }
                if (!validSize) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("input size ");
                    stringBuilder.append(inputConfig.getWidth());
                    stringBuilder.append("x");
                    stringBuilder.append(inputConfig.getHeight());
                    stringBuilder.append(" is not valid");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("input format ");
            stringBuilder2.append(inputConfig.getFormat());
            stringBuilder2.append(" is not valid");
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    /* JADX WARNING: Missing block: B:32:0x006e, code skipped:
            r2 = r12;
     */
    /* JADX WARNING: Missing block: B:33:0x006f, code skipped:
            if (r2 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:34:0x0071, code skipped:
            if (r9 == false) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:35:0x0073, code skipped:
            r7.remove();
     */
    /* JADX WARNING: Missing block: B:36:0x0076, code skipped:
            if (r9 == false) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:37:0x0078, code skipped:
            r3 = new android.hardware.camera2.impl.CameraDeviceImpl.AnonymousClass10(r1);
            r11 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:39:?, code skipped:
            r2.getExecutor().execute(r3);
     */
    /* JADX WARNING: Missing block: B:41:0x008e, code skipped:
            android.os.Binder.restoreCallingIdentity(r11);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkAndFireSequenceComplete() {
        Iterator<RequestLastFrameNumbersHolder> iter;
        Throwable th;
        long j;
        long completedFrameNumber = this.mFrameNumberTracker.getCompletedFrameNumber();
        long completedReprocessFrameNumber = this.mFrameNumberTracker.getCompletedReprocessFrameNumber();
        Iterator<RequestLastFrameNumbersHolder> iter2 = this.mRequestLastFrameNumbersList.iterator();
        while (true) {
            iter = iter2;
            if (iter.hasNext()) {
                final RequestLastFrameNumbersHolder requestLastFrameNumbers = (RequestLastFrameNumbersHolder) iter.next();
                boolean sequenceCompleted = false;
                final int requestId = requestLastFrameNumbers.getRequestId();
                synchronized (this.mInterfaceLock) {
                    try {
                        if (this.mRemoteDevice == null) {
                            try {
                                Log.w(this.TAG, "Camera closed while checking sequences");
                                return;
                            } catch (Throwable th2) {
                                th = th2;
                                j = completedFrameNumber;
                            }
                        } else {
                            int index = this.mCaptureCallbackMap.indexOfKey(requestId);
                            CaptureCallbackHolder holder = index >= 0 ? (CaptureCallbackHolder) this.mCaptureCallbackMap.valueAt(index) : null;
                            if (holder != null) {
                                long lastRegularFrameNumber = requestLastFrameNumbers.getLastRegularFrameNumber();
                                long lastReprocessFrameNumber = requestLastFrameNumbers.getLastReprocessFrameNumber();
                                if (lastRegularFrameNumber <= completedFrameNumber && lastReprocessFrameNumber <= completedReprocessFrameNumber) {
                                    sequenceCompleted = true;
                                    j = completedFrameNumber;
                                    try {
                                        this.mCaptureCallbackMap.removeAt(index);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        throw th;
                                    }
                                }
                            }
                            j = completedFrameNumber;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        j = completedFrameNumber;
                        throw th;
                    }
                }
            }
            return;
            iter2 = iter;
            completedFrameNumber = j;
        }
        iter2 = iter;
        completedFrameNumber = j;
    }

    static Executor checkExecutor(Executor executor) {
        return executor == null ? checkAndWrapHandler(null) : executor;
    }

    public static <T> Executor checkExecutor(Executor executor, T callback) {
        return callback != null ? checkExecutor(executor) : executor;
    }

    public static Executor checkAndWrapHandler(Handler handler) {
        return new CameraHandlerExecutor(checkHandler(handler));
    }

    static Handler checkHandler(Handler handler) {
        if (handler != null) {
            return handler;
        }
        Looper looper = Looper.myLooper();
        if (looper != null) {
            return new Handler(looper);
        }
        throw new IllegalArgumentException("No handler given, and current thread has no looper!");
    }

    static <T> Handler checkHandler(Handler handler, T callback) {
        if (callback != null) {
            return checkHandler(handler);
        }
        return handler;
    }

    private void checkIfCameraClosedOrInError() throws CameraAccessException {
        if (this.mRemoteDevice == null) {
            throw new IllegalStateException("CameraDevice was already closed");
        } else if (this.mInError) {
            throw new CameraAccessException(3, "The camera device has encountered a serious error");
        }
    }

    private boolean isClosed() {
        return this.mClosing.get();
    }

    private CameraCharacteristics getCharacteristics() {
        return this.mCharacteristics;
    }

    public void binderDied() {
        String str = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CameraDevice ");
        stringBuilder.append(this.mCameraId);
        stringBuilder.append(" died unexpectedly");
        Log.w(str, stringBuilder.toString());
        if (this.mRemoteDevice != null) {
            this.mInError = true;
            Runnable r = new Runnable() {
                public void run() {
                    if (!CameraDeviceImpl.this.isClosed()) {
                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, 5);
                    }
                }
            };
            long ident = Binder.clearCallingIdentity();
            try {
                this.mDeviceExecutor.execute(r);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }
}

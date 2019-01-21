package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.impl.CallbackProxies.SessionStateCallbackProxy;
import android.hardware.camera2.impl.CameraDeviceImpl.StateCallbackKK;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.utils.TaskDrainer;
import android.hardware.camera2.utils.TaskDrainer.DrainListener;
import android.hardware.camera2.utils.TaskSingleDrainer;
import android.os.Binder;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.concurrent.Executor;

public class CameraCaptureSessionImpl extends CameraCaptureSession implements CameraCaptureSessionCore {
    private static final boolean DEBUG = false;
    private static final String TAG = "CameraCaptureSession";
    private final TaskSingleDrainer mAbortDrainer;
    private volatile boolean mAborting;
    private boolean mClosed = false;
    private final boolean mConfigureSuccess;
    private final Executor mDeviceExecutor;
    private final CameraDeviceImpl mDeviceImpl;
    private final int mId;
    private final String mIdString;
    private final TaskSingleDrainer mIdleDrainer;
    private final Surface mInput;
    private final TaskDrainer<Integer> mSequenceDrainer;
    private boolean mSkipUnconfigure = false;
    private final StateCallback mStateCallback;
    private final Executor mStateExecutor;

    private class AbortDrainListener implements DrainListener {
        private AbortDrainListener() {
        }

        /* synthetic */ AbortDrainListener(CameraCaptureSessionImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                    return;
                }
                CameraCaptureSessionImpl.this.mIdleDrainer.beginDrain();
            }
        }
    }

    private class IdleDrainListener implements DrainListener {
        private IdleDrainListener() {
        }

        /* synthetic */ IdleDrainListener(CameraCaptureSessionImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                    return;
                }
                try {
                    CameraCaptureSessionImpl.this.mDeviceImpl.configureStreamsChecked(null, null, 0, null);
                } catch (CameraAccessException e) {
                    String str = CameraCaptureSessionImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(CameraCaptureSessionImpl.this.mIdString);
                    stringBuilder.append("Exception while unconfiguring outputs: ");
                    Log.e(str, stringBuilder.toString(), e);
                } catch (IllegalStateException e2) {
                }
            }
        }
    }

    private class SequenceDrainListener implements DrainListener {
        private SequenceDrainListener() {
        }

        /* synthetic */ SequenceDrainListener(CameraCaptureSessionImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onDrained() {
            CameraCaptureSessionImpl.this.mStateCallback.onClosed(CameraCaptureSessionImpl.this);
            if (!CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                CameraCaptureSessionImpl.this.mAbortDrainer.beginDrain();
            }
        }
    }

    CameraCaptureSessionImpl(int id, Surface input, StateCallback callback, Executor stateExecutor, CameraDeviceImpl deviceImpl, Executor deviceStateExecutor, boolean configureSuccess) {
        if (callback != null) {
            this.mId = id;
            this.mIdString = String.format("Session %d: ", new Object[]{Integer.valueOf(this.mId)});
            this.mInput = input;
            this.mStateExecutor = (Executor) Preconditions.checkNotNull(stateExecutor, "stateExecutor must not be null");
            this.mStateCallback = createUserStateCallbackProxy(this.mStateExecutor, callback);
            this.mDeviceExecutor = (Executor) Preconditions.checkNotNull(deviceStateExecutor, "deviceStateExecutor must not be null");
            this.mDeviceImpl = (CameraDeviceImpl) Preconditions.checkNotNull(deviceImpl, "deviceImpl must not be null");
            this.mSequenceDrainer = new TaskDrainer(this.mDeviceExecutor, new SequenceDrainListener(this, null), "seq");
            this.mIdleDrainer = new TaskSingleDrainer(this.mDeviceExecutor, new IdleDrainListener(this, null), "idle");
            this.mAbortDrainer = new TaskSingleDrainer(this.mDeviceExecutor, new AbortDrainListener(this, null), "abort");
            if (configureSuccess) {
                this.mStateCallback.onConfigured(this);
                this.mConfigureSuccess = true;
                return;
            }
            this.mStateCallback.onConfigureFailed(this);
            this.mClosed = true;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mIdString);
            stringBuilder.append("Failed to create capture session; configuration failed");
            Log.e(str, stringBuilder.toString());
            this.mConfigureSuccess = false;
            return;
        }
        throw new IllegalArgumentException("callback must not be null");
    }

    public CameraDevice getDevice() {
        return this.mDeviceImpl;
    }

    public void prepare(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(surface);
    }

    public void prepare(int maxCount, Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(maxCount, surface);
    }

    public void tearDown(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.tearDown(surface);
    }

    public void finalizeOutputConfigurations(List<OutputConfiguration> outputConfigs) throws CameraAccessException {
        this.mDeviceImpl.finalizeOutputConfigs(outputConfigs);
    }

    public int capture(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
        int addPendingSequence;
        checkCaptureRequest(request);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            addPendingSequence = addPendingSequence(this.mDeviceImpl.capture(request, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceExecutor));
        }
        return addPendingSequence;
    }

    public int captureSingleRequest(CaptureRequest request, Executor executor, CaptureCallback callback) throws CameraAccessException {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        } else if (callback != null) {
            int addPendingSequence;
            checkCaptureRequest(request);
            synchronized (this.mDeviceImpl.mInterfaceLock) {
                checkNotClosed();
                addPendingSequence = addPendingSequence(this.mDeviceImpl.capture(request, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, callback), callback), this.mDeviceExecutor));
            }
            return addPendingSequence;
        } else {
            throw new IllegalArgumentException("callback must not be null");
        }
    }

    private void checkCaptureRequest(CaptureRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        } else if (request.isReprocess() && !isReprocessable()) {
            throw new IllegalArgumentException("this capture session cannot handle reprocess requests");
        } else if (request.isReprocess() && request.getReprocessableSessionId() != this.mId) {
            throw new IllegalArgumentException("capture request was created for another session");
        }
    }

    public int captureBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
        int addPendingSequence;
        checkCaptureRequests(requests);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            addPendingSequence = addPendingSequence(this.mDeviceImpl.captureBurst(requests, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceExecutor));
        }
        return addPendingSequence;
    }

    public int captureBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback callback) throws CameraAccessException {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        } else if (callback != null) {
            int addPendingSequence;
            checkCaptureRequests(requests);
            synchronized (this.mDeviceImpl.mInterfaceLock) {
                checkNotClosed();
                addPendingSequence = addPendingSequence(this.mDeviceImpl.captureBurst(requests, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, callback), callback), this.mDeviceExecutor));
            }
            return addPendingSequence;
        } else {
            throw new IllegalArgumentException("callback must not be null");
        }
    }

    private void checkCaptureRequests(List<CaptureRequest> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("Requests must not be null");
        } else if (requests.isEmpty()) {
            throw new IllegalArgumentException("Requests must have at least one element");
        } else {
            for (CaptureRequest request : requests) {
                if (request.isReprocess()) {
                    if (!isReprocessable()) {
                        throw new IllegalArgumentException("This capture session cannot handle reprocess requests");
                    } else if (request.getReprocessableSessionId() != this.mId) {
                        throw new IllegalArgumentException("Capture request was created for another session");
                    }
                }
            }
        }
    }

    public int setRepeatingRequest(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
        int addPendingSequence;
        checkRepeatingRequest(request);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            addPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingRequest(request, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceExecutor));
        }
        return addPendingSequence;
    }

    public int setSingleRepeatingRequest(CaptureRequest request, Executor executor, CaptureCallback callback) throws CameraAccessException {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        } else if (callback != null) {
            int addPendingSequence;
            checkRepeatingRequest(request);
            synchronized (this.mDeviceImpl.mInterfaceLock) {
                checkNotClosed();
                addPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingRequest(request, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, callback), callback), this.mDeviceExecutor));
            }
            return addPendingSequence;
        } else {
            throw new IllegalArgumentException("callback must not be null");
        }
    }

    private void checkRepeatingRequest(CaptureRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        } else if (request.isReprocess()) {
            throw new IllegalArgumentException("repeating reprocess requests are not supported");
        }
    }

    public int setRepeatingBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
        int addPendingSequence;
        checkRepeatingRequests(requests);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            addPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingBurst(requests, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceExecutor));
        }
        return addPendingSequence;
    }

    public int setRepeatingBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback callback) throws CameraAccessException {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        } else if (callback != null) {
            int addPendingSequence;
            checkRepeatingRequests(requests);
            synchronized (this.mDeviceImpl.mInterfaceLock) {
                checkNotClosed();
                addPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingBurst(requests, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, callback), callback), this.mDeviceExecutor));
            }
            return addPendingSequence;
        } else {
            throw new IllegalArgumentException("callback must not be null");
        }
    }

    private void checkRepeatingRequests(List<CaptureRequest> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        } else if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests must have at least one element");
        } else {
            for (CaptureRequest r : requests) {
                if (r.isReprocess()) {
                    throw new IllegalArgumentException("repeating reprocess burst requests are not supported");
                }
            }
        }
    }

    public void stopRepeating() throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            this.mDeviceImpl.stopRepeating();
        }
    }

    public void abortCaptures() throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mIdString);
            stringBuilder.append("abortCaptures");
            Log.i(str, stringBuilder.toString());
            checkNotClosed();
            if (this.mAborting) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mIdString);
                stringBuilder.append("abortCaptures - Session is already aborting; doing nothing");
                Log.w(str, stringBuilder.toString());
                return;
            }
            this.mAborting = true;
            this.mAbortDrainer.taskStarted();
            this.mDeviceImpl.flush();
        }
    }

    public void updateOutputConfiguration(OutputConfiguration config) throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            this.mDeviceImpl.updateOutputConfiguration(config);
        }
    }

    public boolean isReprocessable() {
        return this.mInput != null;
    }

    public Surface getInputSurface() {
        return this.mInput;
    }

    public void replaceSessionClose() {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            this.mSkipUnconfigure = true;
            close();
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0035, code skipped:
            if (r5.mInput == null) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:17:0x0037, code skipped:
            r5.mInput.release();
     */
    /* JADX WARNING: Missing block: B:18:0x003c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            if (this.mClosed) {
                return;
            }
            this.mClosed = true;
            try {
                this.mDeviceImpl.stopRepeating();
            } catch (IllegalStateException e) {
                this.mStateCallback.onClosed(this);
                return;
            } catch (CameraAccessException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mIdString);
                stringBuilder.append("Exception while stopping repeating: ");
                Log.e(str, stringBuilder.toString(), e2);
            }
            this.mSequenceDrainer.beginDrain();
        }
    }

    public boolean isAborting() {
        return this.mAborting;
    }

    private StateCallback createUserStateCallbackProxy(Executor executor, StateCallback callback) {
        return new SessionStateCallbackProxy(executor, callback);
    }

    private CameraDeviceImpl.CaptureCallback createCaptureCallbackProxy(Handler handler, CaptureCallback callback) {
        Executor executor;
        if (callback != null) {
            executor = CameraDeviceImpl.checkAndWrapHandler(handler);
        } else {
            executor = null;
        }
        return createCaptureCallbackProxyWithExecutor(executor, callback);
    }

    private CameraDeviceImpl.CaptureCallback createCaptureCallbackProxyWithExecutor(final Executor executor, final CaptureCallback callback) {
        return new CameraDeviceImpl.CaptureCallback() {
            public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp, long frameNumber) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$uPVvNnGFdZcxxscdYQ5erNgaRWA(this, callback, request, timestamp, frameNumber));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            public void onCapturePartial(CameraDevice camera, CaptureRequest request, CaptureResult result) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$HRzGZkXU2X5JDcudK0jcqdLZzV8(this, callback, request, result));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            public void onCaptureProgressed(CameraDevice camera, CaptureRequest request, CaptureResult partialResult) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$7mSdNTTAoYA0D3ITDxzDJKGykz0(this, callback, request, partialResult));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, TotalCaptureResult result) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$OA1Yz_YgzMO8qcV8esRjyt7ykp4(this, callback, request, result));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            public void onCaptureFailed(CameraDevice camera, CaptureRequest request, CaptureFailure failure) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$VsKq1alEqL3XH-hLTWXgi7fSF3s(this, callback, request, failure));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId, long frameNumber) {
                if (!(callback == null || executor == null)) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$KZ4tthx5TnA5BizPVljsPqqdHck(this, callback, sequenceId, frameNumber));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
            }

            public void onCaptureSequenceAborted(CameraDevice camera, int sequenceId) {
                if (!(callback == null || executor == null)) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$TIJELOXvjSbPh6mpBLfBJ5ciNic(this, callback, sequenceId));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
                CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
            }

            public void onCaptureBufferLost(CameraDevice camera, CaptureRequest request, Surface target, long frameNumber) {
                if (callback != null && executor != null) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new -$$Lambda$CameraCaptureSessionImpl$1$VuYVXvwmJMkbTnKaOD-h-DOjJpE(this, callback, request, target, frameNumber));
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        };
    }

    public StateCallbackKK getDeviceStateCallback() {
        final Object interfaceLock = this.mDeviceImpl.mInterfaceLock;
        return new StateCallbackKK() {
            private boolean mActive = false;
            private boolean mBusy = false;

            public void onOpened(CameraDevice camera) {
                throw new AssertionError("Camera must already be open before creating a session");
            }

            public void onDisconnected(CameraDevice camera) {
                CameraCaptureSessionImpl.this.close();
            }

            public void onError(CameraDevice camera, int error) {
                String str = CameraCaptureSessionImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(CameraCaptureSessionImpl.this.mIdString);
                stringBuilder.append("Got device error ");
                stringBuilder.append(error);
                Log.wtf(str, stringBuilder.toString());
            }

            public void onActive(CameraDevice camera) {
                CameraCaptureSessionImpl.this.mIdleDrainer.taskStarted();
                this.mActive = true;
                CameraCaptureSessionImpl.this.mStateCallback.onActive(this);
            }

            public void onIdle(CameraDevice camera) {
                synchronized (interfaceLock) {
                    boolean isAborting = CameraCaptureSessionImpl.this.mAborting;
                }
                if (this.mBusy && isAborting) {
                    CameraCaptureSessionImpl.this.mAbortDrainer.taskFinished();
                    synchronized (interfaceLock) {
                        CameraCaptureSessionImpl.this.mAborting = false;
                    }
                }
                if (this.mActive) {
                    CameraCaptureSessionImpl.this.mIdleDrainer.taskFinished();
                }
                this.mBusy = false;
                this.mActive = false;
                CameraCaptureSessionImpl.this.mStateCallback.onReady(this);
            }

            public void onBusy(CameraDevice camera) {
                this.mBusy = true;
            }

            public void onUnconfigured(CameraDevice camera) {
            }

            public void onRequestQueueEmpty() {
                CameraCaptureSessionImpl.this.mStateCallback.onCaptureQueueEmpty(this);
            }

            public void onSurfacePrepared(Surface surface) {
                CameraCaptureSessionImpl.this.mStateCallback.onSurfacePrepared(this, surface);
            }
        };
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void checkNotClosed() {
        if (this.mClosed) {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
    }

    private int addPendingSequence(int sequenceId) {
        this.mSequenceDrainer.taskStarted(Integer.valueOf(sequenceId));
        return sequenceId;
    }

    private void finishPendingSequence(int sequenceId) {
        try {
            this.mSequenceDrainer.taskFinished(Integer.valueOf(sequenceId));
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
        }
    }
}

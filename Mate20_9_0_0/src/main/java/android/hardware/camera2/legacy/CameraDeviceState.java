package android.hardware.camera2.legacy;

import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.Handler;
import android.util.Log;

public class CameraDeviceState {
    private static final boolean DEBUG = false;
    public static final int NO_CAPTURE_ERROR = -1;
    private static final int STATE_CAPTURING = 4;
    private static final int STATE_CONFIGURING = 2;
    private static final int STATE_ERROR = 0;
    private static final int STATE_IDLE = 3;
    private static final int STATE_UNCONFIGURED = 1;
    private static final String TAG = "CameraDeviceState";
    private static final String[] sStateNames = new String[]{"ERROR", "UNCONFIGURED", "CONFIGURING", "IDLE", "CAPTURING"};
    private int mCurrentError = -1;
    private Handler mCurrentHandler = null;
    private CameraDeviceStateListener mCurrentListener = null;
    private RequestHolder mCurrentRequest = null;
    private int mCurrentState = 1;

    public interface CameraDeviceStateListener {
        void onBusy();

        void onCaptureResult(CameraMetadataNative cameraMetadataNative, RequestHolder requestHolder);

        void onCaptureStarted(RequestHolder requestHolder, long j);

        void onConfiguring();

        void onError(int i, Object obj, RequestHolder requestHolder);

        void onIdle();

        void onRepeatingRequestError(long j, int i);

        void onRequestQueueEmpty();
    }

    public synchronized void setError(int error) {
        this.mCurrentError = error;
        doStateTransition(0);
    }

    public synchronized boolean setConfiguring() {
        doStateTransition(2);
        return this.mCurrentError == -1;
    }

    public synchronized boolean setIdle() {
        doStateTransition(3);
        return this.mCurrentError == -1;
    }

    public synchronized boolean setCaptureStart(RequestHolder request, long timestamp, int captureError) {
        this.mCurrentRequest = request;
        doStateTransition(4, timestamp, captureError);
        return this.mCurrentError == -1;
    }

    /* JADX WARNING: Missing block: B:8:0x002d, code skipped:
            return r3;
     */
    /* JADX WARNING: Missing block: B:21:0x0054, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean setCaptureResult(final RequestHolder request, final CameraMetadataNative result, final int captureError, final Object captureErrorArg) {
        boolean z = false;
        if (this.mCurrentState != 4) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot receive result while in state: ");
            stringBuilder.append(this.mCurrentState);
            Log.e(str, stringBuilder.toString());
            this.mCurrentError = 1;
            doStateTransition(0);
            if (this.mCurrentError == -1) {
                z = true;
            }
        } else {
            if (!(this.mCurrentHandler == null || this.mCurrentListener == null)) {
                if (captureError != -1) {
                    this.mCurrentHandler.post(new Runnable() {
                        public void run() {
                            CameraDeviceState.this.mCurrentListener.onError(captureError, captureErrorArg, request);
                        }
                    });
                } else {
                    this.mCurrentHandler.post(new Runnable() {
                        public void run() {
                            CameraDeviceState.this.mCurrentListener.onCaptureResult(result, request);
                        }
                    });
                }
            }
            if (this.mCurrentError == -1) {
                z = true;
            }
        }
    }

    public synchronized boolean setCaptureResult(RequestHolder request, CameraMetadataNative result) {
        return setCaptureResult(request, result, -1, null);
    }

    public synchronized void setRepeatingRequestError(final long lastFrameNumber, final int repeatingRequestId) {
        this.mCurrentHandler.post(new Runnable() {
            public void run() {
                CameraDeviceState.this.mCurrentListener.onRepeatingRequestError(lastFrameNumber, repeatingRequestId);
            }
        });
    }

    public synchronized void setRequestQueueEmpty() {
        this.mCurrentHandler.post(new Runnable() {
            public void run() {
                CameraDeviceState.this.mCurrentListener.onRequestQueueEmpty();
            }
        });
    }

    public synchronized void setCameraDeviceCallbacks(Handler handler, CameraDeviceStateListener listener) {
        this.mCurrentHandler = handler;
        this.mCurrentListener = listener;
    }

    private void doStateTransition(int newState) {
        doStateTransition(newState, 0, -1);
    }

    private void doStateTransition(int newState, final long timestamp, final int error) {
        String stateName;
        StringBuilder stringBuilder;
        if (newState != this.mCurrentState) {
            stateName = "UNKNOWN";
            if (newState >= 0 && newState < sStateNames.length) {
                stateName = sStateNames[newState];
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Legacy camera service transitioning to state ");
            stringBuilder.append(stateName);
            Log.i(str, stringBuilder.toString());
        }
        if (!(newState == 0 || newState == 3 || this.mCurrentState == newState || this.mCurrentHandler == null || this.mCurrentListener == null)) {
            this.mCurrentHandler.post(new Runnable() {
                public void run() {
                    CameraDeviceState.this.mCurrentListener.onBusy();
                }
            });
        }
        if (newState != 0) {
            switch (newState) {
                case 2:
                    if (this.mCurrentState == 1 || this.mCurrentState == 3) {
                        if (!(this.mCurrentState == 2 || this.mCurrentHandler == null || this.mCurrentListener == null)) {
                            this.mCurrentHandler.post(new Runnable() {
                                public void run() {
                                    CameraDeviceState.this.mCurrentListener.onConfiguring();
                                }
                            });
                        }
                        this.mCurrentState = 2;
                        return;
                    }
                    stateName = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot call configure while in state: ");
                    stringBuilder.append(this.mCurrentState);
                    Log.e(stateName, stringBuilder.toString());
                    this.mCurrentError = 1;
                    doStateTransition(0);
                    return;
                case 3:
                    if (this.mCurrentState != 3) {
                        if (this.mCurrentState == 2 || this.mCurrentState == 4) {
                            if (!(this.mCurrentState == 3 || this.mCurrentHandler == null || this.mCurrentListener == null)) {
                                this.mCurrentHandler.post(new Runnable() {
                                    public void run() {
                                        CameraDeviceState.this.mCurrentListener.onIdle();
                                    }
                                });
                            }
                            this.mCurrentState = 3;
                            return;
                        }
                        stateName = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot call idle while in state: ");
                        stringBuilder.append(this.mCurrentState);
                        Log.e(stateName, stringBuilder.toString());
                        this.mCurrentError = 1;
                        doStateTransition(0);
                        return;
                    }
                    return;
                case 4:
                    if (this.mCurrentState == 3 || this.mCurrentState == 4) {
                        if (!(this.mCurrentHandler == null || this.mCurrentListener == null)) {
                            if (error != -1) {
                                this.mCurrentHandler.post(new Runnable() {
                                    public void run() {
                                        CameraDeviceState.this.mCurrentListener.onError(error, null, CameraDeviceState.this.mCurrentRequest);
                                    }
                                });
                            } else {
                                this.mCurrentHandler.post(new Runnable() {
                                    public void run() {
                                        CameraDeviceState.this.mCurrentListener.onCaptureStarted(CameraDeviceState.this.mCurrentRequest, timestamp);
                                    }
                                });
                            }
                        }
                        this.mCurrentState = 4;
                        return;
                    }
                    stateName = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot call capture while in state: ");
                    stringBuilder.append(this.mCurrentState);
                    Log.e(stateName, stringBuilder.toString());
                    this.mCurrentError = 1;
                    doStateTransition(0);
                    return;
                default:
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Transition to unknown state: ");
                    stringBuilder2.append(newState);
                    throw new IllegalStateException(stringBuilder2.toString());
            }
        }
        if (!(this.mCurrentState == 0 || this.mCurrentHandler == null || this.mCurrentListener == null)) {
            this.mCurrentHandler.post(new Runnable() {
                public void run() {
                    CameraDeviceState.this.mCurrentListener.onError(CameraDeviceState.this.mCurrentError, null, CameraDeviceState.this.mCurrentRequest);
                }
            });
        }
        this.mCurrentState = 0;
    }
}

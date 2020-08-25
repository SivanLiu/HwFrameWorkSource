package com.android.server.fsm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;

class SensorFoldStateManager {
    private static final int LOG_TIME = 1000;
    private static final int MSG_HANDLE_FOLD_STATE_SENSOR = 0;
    private static final int POSTURE_SENSOR_LENGTH = 7;
    private static final int SENSOR_RATE = 25000;
    private static final String TAG = "Fsm_SensorFoldStateManager";
    private Sensor mAccSensor;
    private final SensorEventListener mAccSensorListener = new SensorEventListener() {
        /* class com.android.server.fsm.SensorFoldStateManager.AnonymousClass2 */

        public void onSensorChanged(SensorEvent event) {
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private ISensorPostureCallback mCallback;
    private Context mContext;
    private final SensorEventListener mFoldStateSensorListener = new SensorEventListener() {
        /* class com.android.server.fsm.SensorFoldStateManager.AnonymousClass1 */

        public void onSensorChanged(SensorEvent event) {
            long time = SystemClock.uptimeMillis();
            if (event.values.length == 7) {
                if (time - SensorFoldStateManager.this.mLastEventTime >= 1000) {
                    SensorPostureProcess.printPostureSensor(event.values);
                    long unused = SensorFoldStateManager.this.mLastEventTime = time;
                }
                SensorFoldStateManager.this.mHandler.removeMessages(0);
                Message msg = SensorFoldStateManager.this.mHandler.obtainMessage(0);
                msg.obj = event.values;
                SensorFoldStateManager.this.mHandler.sendMessage(msg);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /* access modifiers changed from: private */
    public SensorHandler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mInitPortraitState = true;
    private boolean mIsMagnWakeUp = false;
    /* access modifiers changed from: private */
    public long mLastEventTime = 0;
    private final Object mLock = new Object();
    private MagnetometerWakeupManager mMagnetometerWakeup;
    private Sensor mPostureSensor;
    private SensorManager mSensorManager;
    private int mState = 0;

    SensorFoldStateManager(Context context) {
        Slog.i("Fsm_SensorFoldStateManager", "SensorFoldStateManager init");
        this.mContext = context;
        this.mHandlerThread = new HandlerThread("Fsm_SensorFoldStateManager");
        this.mHandlerThread.start();
        this.mHandler = new SensorHandler(this.mHandlerThread.getLooper());
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mPostureSensor = this.mSensorManager.getDefaultSensor(65573);
        this.mAccSensor = this.mSensorManager.getDefaultSensor(65558);
        this.mSensorManager.registerListener(this.mAccSensorListener, this.mAccSensor, SENSOR_RATE, this.mHandler);
        this.mMagnetometerWakeup = MagnetometerWakeupManager.getInstance(this.mContext);
    }

    public boolean turnOnFoldStateSensor(ISensorPostureCallback callback, int wakeUpType) {
        boolean z = false;
        if (callback == null) {
            Slog.i("Fsm_SensorFoldStateManager", "turnOnFoldStateSensor callback is null");
            return false;
        }
        synchronized (this.mLock) {
            boolean z2 = true;
            if (this.mCallback == null) {
                this.mSensorManager.registerListener(this.mFoldStateSensorListener, this.mPostureSensor, SENSOR_RATE, this.mHandler);
                if (wakeUpType == 4) {
                    z = true;
                }
                this.mIsMagnWakeUp = z;
                this.mCallback = callback;
                Slog.i("Fsm_SensorFoldStateManager", "registerFoldStateSensor success");
                return true;
            }
            if (wakeUpType != 4) {
                z2 = false;
            }
            this.mIsMagnWakeUp = z2;
            Slog.i("Fsm_SensorFoldStateManager", "FoldStateSensor is already registered");
            return false;
        }
    }

    public boolean turnOffFoldStateSensor(ISensorPostureCallback callback) {
        if (callback == null) {
            Slog.i("Fsm_SensorFoldStateManager", "turnOffFoldStateSensor callback is null");
            return false;
        }
        synchronized (this.mLock) {
            if (this.mCallback == null || !callback.equals(this.mCallback)) {
                Slog.i("Fsm_SensorFoldStateManager", "FoldStateSensor is not registered");
                return false;
            }
            this.mCallback = null;
            this.mSensorManager.unregisterListener(this.mFoldStateSensorListener);
            this.mState = 0;
            this.mInitPortraitState = true;
            this.mIsMagnWakeUp = false;
            Slog.i("Fsm_SensorFoldStateManager", "unregisterFoldStateSensor success");
            return true;
        }
    }

    public int getPosture() {
        int i;
        synchronized (this.mLock) {
            i = this.mState;
        }
        return i;
    }

    /* access modifiers changed from: private */
    public void handleFoldStateSensor(float[] data) {
        int preState;
        synchronized (this.mLock) {
            if (this.mInitPortraitState) {
                this.mInitPortraitState = SensorPostureProcess.isPortraitState(data);
            }
            if (this.mCallback != null) {
                if (this.mInitPortraitState) {
                    Slog.i("Fsm_SensorFoldStateManager", "init state is Portrait");
                    if (this.mMagnetometerWakeup.getHallData() == 1) {
                        preState = 1;
                    } else {
                        preState = 2;
                    }
                } else {
                    preState = SensorPostureProcess.getFoldableState(data, this.mState);
                }
                if (this.mIsMagnWakeUp) {
                    if (preState == 1) {
                        this.mIsMagnWakeUp = false;
                    } else {
                        return;
                    }
                }
                if (preState != this.mState) {
                    this.mState = preState;
                    this.mCallback.onPostureChange(transFoldStateToPosture(this.mState));
                }
            }
        }
    }

    private int transFoldStateToPosture(int state) {
        if (state == 1) {
            return 109;
        }
        if (state == 2) {
            return 103;
        }
        if (state != 3) {
            return 100;
        }
        return 106;
    }

    /* access modifiers changed from: private */
    public final class SensorHandler extends Handler {
        SensorHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what != 0) {
                Slog.e("Fsm_SensorFoldStateManager", "Invalid message");
                return;
            }
            SensorFoldStateManager.this.handleFoldStateSensor((float[]) msg.obj);
        }
    }
}

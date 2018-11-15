package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class HwLightSensorController {
    private static boolean DEBUG = false;
    private static final int MSG_TIMER = 1;
    private static final String TAG = "HwLightSensorController";
    private int SENSOR_OPTION;
    private int mBackSensorValue;
    private final LightSensorCallbacks mCallbacks;
    private final Context mContext;
    private List<Integer> mCtDataList;
    private List<Integer> mDataList;
    private boolean mEnable;
    private long mEnableTime;
    private Handler mHandler;
    private HwDualSensorEventListenerImpl mHwDualSensorEventListenerImpl;
    private int mLastSensorCtValue;
    private int mLastSensorValue;
    private Sensor mLightSensor;
    private int mRateMillis;
    private final SensorEventListener mSensorEventListener;
    private SensorManager mSensorManager;
    private SensorObserver mSensorObserver;
    private boolean mWarmUpFlg;

    public interface LightSensorCallbacks {
        void processSensorData(long j, int i, int i2);
    }

    private class SensorObserver implements Observer {
        public void update(Observable o, Object arg) {
            long[] data = (long[]) arg;
            int lux = (int) data[0];
            int cct = (int) data[1];
            long timeStamp = data[2];
            if (HwLightSensorController.this.SENSOR_OPTION == 2) {
                HwLightSensorController.this.mBackSensorValue = (int) data[5];
            } else if (HwLightSensorController.this.SENSOR_OPTION == 1) {
                HwLightSensorController.this.mBackSensorValue = lux;
            }
            if (HwLightSensorController.this.mEnable && lux >= 0 && cct >= 0 && timeStamp >= 0) {
                HwLightSensorController.this.mCallbacks.processSensorData(timeStamp, lux, cct);
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwLightSensorController(Context context, LightSensorCallbacks callbacks, SensorManager sensorManager, int sensorRateMillis) {
        this(context, callbacks, sensorManager, sensorRateMillis, TAG);
    }

    public HwLightSensorController(Context context, LightSensorCallbacks callbacks, SensorManager sensorManager, int sensorRateMillis, String TagForDualSensor) {
        this.mLastSensorValue = -1;
        this.mLastSensorCtValue = -1;
        this.mWarmUpFlg = true;
        this.mRateMillis = 300;
        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
        this.SENSOR_OPTION = -1;
        this.mBackSensorValue = -1;
        this.mSensorEventListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                if (HwLightSensorController.this.mEnable) {
                    int lux = (int) event.values[0];
                    int cct = (int) event.values[1];
                    long timeStamp = event.timestamp / 1000000;
                    if (!HwLightSensorController.this.mWarmUpFlg) {
                        HwLightSensorController.this.setSensorData(lux);
                        HwLightSensorController.this.setSensorCtData(cct);
                    } else if (timeStamp < HwLightSensorController.this.mEnableTime) {
                        if (HwLightSensorController.DEBUG) {
                            Slog.i(HwLightSensorController.TAG, "sensor not ready yet");
                        }
                    } else {
                        HwLightSensorController.this.setSensorData(lux);
                        HwLightSensorController.this.setSensorCtData(cct);
                        HwLightSensorController.this.mWarmUpFlg = false;
                        HwLightSensorController.this.mHandler.sendEmptyMessage(1);
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    Slog.e(HwLightSensorController.TAG, "Invalid message");
                    return;
                }
                int lux = HwLightSensorController.this.getSensorData();
                int cct = HwLightSensorController.this.getSensorCtData();
                if (lux >= 0 && cct >= 0) {
                    try {
                        HwLightSensorController.this.mCallbacks.processSensorData(SystemClock.elapsedRealtime(), lux, cct);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (HwLightSensorController.this.mEnable) {
                    sendEmptyMessageDelayed(1, (long) HwLightSensorController.this.mRateMillis);
                }
            }
        };
        this.mContext = context;
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mRateMillis = sensorRateMillis;
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        this.mDataList = new ArrayList();
        this.mCtDataList = new ArrayList();
        this.mHwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.getInstance(this.mSensorManager, this.mContext);
        this.SENSOR_OPTION = this.mHwDualSensorEventListenerImpl.getModuleSensorOption(TagForDualSensor);
        this.mSensorObserver = new SensorObserver();
    }

    public void enableSensor() {
        if (!this.mEnable) {
            this.mEnable = true;
            this.mWarmUpFlg = true;
            this.mEnableTime = SystemClock.elapsedRealtime();
            int i = this.SENSOR_OPTION;
            HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
            if (i == -1) {
                this.mSensorManager.registerListener(this.mSensorEventListener, this.mLightSensor, this.mRateMillis * 1000);
                return;
            }
            i = this.SENSOR_OPTION;
            hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
            if (i == 0) {
                this.mHwDualSensorEventListenerImpl.attachFrontSensorData(this.mSensorObserver);
                return;
            }
            i = this.SENSOR_OPTION;
            hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
            if (i == 1) {
                this.mHwDualSensorEventListenerImpl.attachBackSensorData(this.mSensorObserver);
                return;
            }
            int i2 = this.SENSOR_OPTION;
            HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl2 = this.mHwDualSensorEventListenerImpl;
            if (i2 == 2) {
                this.mHwDualSensorEventListenerImpl.attachFusedSensorData(this.mSensorObserver);
            }
        }
    }

    public void disableSensor() {
        if (this.mEnable) {
            this.mEnable = false;
            int i = this.SENSOR_OPTION;
            HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
            if (i == -1) {
                this.mSensorManager.unregisterListener(this.mSensorEventListener);
                this.mHandler.removeMessages(1);
            } else {
                i = this.SENSOR_OPTION;
                hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                if (i == 0) {
                    this.mHwDualSensorEventListenerImpl.detachFrontSensorData(this.mSensorObserver);
                } else {
                    i = this.SENSOR_OPTION;
                    hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                    if (i == 1) {
                        this.mHwDualSensorEventListenerImpl.detachBackSensorData(this.mSensorObserver);
                    } else {
                        i = this.SENSOR_OPTION;
                        hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                        if (i == 2) {
                            this.mHwDualSensorEventListenerImpl.detachFusedSensorData(this.mSensorObserver);
                        }
                    }
                }
            }
            clearSensorData();
            clearSensorCtData();
        }
    }

    public boolean isBackSensorEnable() {
        return this.SENSOR_OPTION == 2 || this.SENSOR_OPTION == 1;
    }

    public int getBackSensorValue() {
        return this.mBackSensorValue;
    }

    private void setSensorData(int lux) {
        synchronized (this.mDataList) {
            this.mDataList.add(Integer.valueOf(lux));
        }
    }

    private void setSensorCtData(int cct) {
        synchronized (this.mCtDataList) {
            this.mCtDataList.add(Integer.valueOf(cct));
        }
    }

    private int getSensorData() {
        synchronized (this.mDataList) {
            int i;
            if (this.mDataList.isEmpty()) {
                i = this.mLastSensorValue;
                return i;
            }
            int average;
            i = 0;
            int sum = 0;
            for (Integer data : this.mDataList) {
                sum += data.intValue();
                i++;
            }
            if (i != 0) {
                average = sum / i;
                if (average >= 0) {
                    this.mLastSensorValue = average;
                }
            }
            this.mDataList.clear();
            average = this.mLastSensorValue;
            return average;
        }
    }

    private int getSensorCtData() {
        synchronized (this.mCtDataList) {
            int i;
            if (this.mCtDataList.isEmpty()) {
                i = this.mLastSensorCtValue;
                return i;
            }
            int average;
            i = 0;
            int sum = 0;
            for (Integer data : this.mCtDataList) {
                sum += data.intValue();
                i++;
            }
            if (i != 0) {
                average = sum / i;
                if (average >= 0) {
                    this.mLastSensorCtValue = average;
                }
            }
            this.mCtDataList.clear();
            average = this.mLastSensorCtValue;
            return average;
        }
    }

    private void clearSensorData() {
        synchronized (this.mDataList) {
            this.mDataList.clear();
            this.mLastSensorValue = -1;
        }
    }

    private void clearSensorCtData() {
        synchronized (this.mCtDataList) {
            this.mCtDataList.clear();
            this.mLastSensorCtValue = -1;
        }
    }
}

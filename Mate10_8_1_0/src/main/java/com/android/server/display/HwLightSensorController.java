package com.android.server.display;

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
    private int SENSOR_OPTION = -1;
    private final LightSensorCallbacks mCallbacks;
    private List<Integer> mCtDataList;
    private List<Integer> mDataList;
    private boolean mEnable;
    private long mEnableTime;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
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
                        return;
                    }
                    return;
                default:
                    Slog.e(HwLightSensorController.TAG, "Invalid message");
                    return;
            }
        }
    };
    private HwDualSensorEventListenerImpl mHwDualSensorEventListenerImpl;
    private int mLastSensorCtValue = -1;
    private int mLastSensorValue = -1;
    private Sensor mLightSensor;
    private int mRateMillis = 300;
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
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
    private SensorManager mSensorManager;
    private SensorObserver mSensorObserver;
    private boolean mWarmUpFlg = true;

    public interface LightSensorCallbacks {
        void processSensorData(long j, int i, int i2);
    }

    private class SensorObserver implements Observer {
        public void update(Observable o, Object arg) {
            long[] data = (long[]) arg;
            int lux = (int) data[0];
            int cct = (int) data[1];
            long timeStamp = data[2];
            if (HwLightSensorController.this.mEnable && lux >= 0 && cct >= 0 && timeStamp >= 0) {
                HwLightSensorController.this.mCallbacks.processSensorData(timeStamp, lux, cct);
            }
        }
    }

    static {
        boolean isLoggable = !Log.HWINFO ? Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false : true;
        DEBUG = isLoggable;
    }

    public HwLightSensorController(LightSensorCallbacks callbacks, SensorManager sensorManager, int sensorRateMillis) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mRateMillis = sensorRateMillis;
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        this.mDataList = new ArrayList();
        this.mCtDataList = new ArrayList();
        this.mHwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.getInstance(this.mSensorManager);
        this.SENSOR_OPTION = this.mHwDualSensorEventListenerImpl.getModuleSensorOption(TAG);
    }

    public void enableSensor() {
        if (!this.mEnable) {
            this.mEnable = true;
            this.mWarmUpFlg = true;
            this.mEnableTime = SystemClock.elapsedRealtime();
            if (this.SENSOR_OPTION == -1) {
                this.mSensorManager.registerListener(this.mSensorEventListener, this.mLightSensor, this.mRateMillis * 1000);
            } else if (this.SENSOR_OPTION == 0) {
                this.mSensorObserver = new SensorObserver();
                this.mHwDualSensorEventListenerImpl.attachFrontSensorData(this.mSensorObserver);
            } else if (this.SENSOR_OPTION == 1) {
                this.mSensorObserver = new SensorObserver();
                this.mHwDualSensorEventListenerImpl.attachBackSensorData(this.mSensorObserver);
            } else if (this.SENSOR_OPTION == 2) {
                this.mSensorObserver = new SensorObserver();
                this.mHwDualSensorEventListenerImpl.attachFusedSensorData(this.mSensorObserver);
            }
        }
    }

    public void disableSensor() {
        if (this.mEnable) {
            this.mEnable = false;
            if (this.SENSOR_OPTION == -1) {
                this.mSensorManager.unregisterListener(this.mSensorEventListener);
                this.mHandler.removeMessages(1);
            } else if (this.SENSOR_OPTION == 0) {
                if (this.mSensorObserver != null) {
                    this.mHwDualSensorEventListenerImpl.detachFrontSensorData(this.mSensorObserver);
                }
            } else if (this.SENSOR_OPTION == 1) {
                if (this.mSensorObserver != null) {
                    this.mHwDualSensorEventListenerImpl.detachBackSensorData(this.mSensorObserver);
                }
            } else if (this.SENSOR_OPTION == 2 && this.mSensorObserver != null) {
                this.mHwDualSensorEventListenerImpl.detachFusedSensorData(this.mSensorObserver);
            }
            clearSensorData();
            clearSensorCtData();
        }
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
            if (this.mDataList.isEmpty()) {
                int i = this.mLastSensorValue;
                return i;
            }
            int count = 0;
            int sum = 0;
            for (Integer data : this.mDataList) {
                sum += data.intValue();
                count++;
            }
            if (count != 0) {
                int average = sum / count;
                if (average >= 0) {
                    this.mLastSensorValue = average;
                }
            }
            this.mDataList.clear();
            i = this.mLastSensorValue;
            return i;
        }
    }

    private int getSensorCtData() {
        synchronized (this.mCtDataList) {
            if (this.mCtDataList.isEmpty()) {
                int i = this.mLastSensorCtValue;
                return i;
            }
            int count = 0;
            int sum = 0;
            for (Integer data : this.mCtDataList) {
                sum += data.intValue();
                count++;
            }
            if (count != 0) {
                int average = sum / count;
                if (average >= 0) {
                    this.mLastSensorCtValue = average;
                }
            }
            this.mCtDataList.clear();
            i = this.mLastSensorCtValue;
            return i;
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

package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.TorchCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwDualSensorEventListenerImpl {
    private static float AGAIN = 16.0f;
    private static float ATIME = 100.008f;
    private static float BACK_FLOOR_THRESH = 5.0f;
    private static long BACK_LUX_DEVIATION_THRESH = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
    private static final int BACK_MSG_TIMER = 2;
    private static float BACK_ROOF_THRESH = 400000.0f;
    private static int[] BACK_SENSOR_STABILITY_THRESHOLD = new int[]{-30, -4, 4, 30};
    private static float COLOR_MAT_HIGH_11 = -0.85740393f;
    private static float COLOR_MAT_HIGH_12 = 3.8810537f;
    private static float COLOR_MAT_HIGH_13 = -1.2211744f;
    private static float COLOR_MAT_HIGH_14 = 0.047183935f;
    private static float COLOR_MAT_HIGH_21 = -2.835158f;
    private static float COLOR_MAT_HIGH_22 = 6.03384f;
    private static float COLOR_MAT_HIGH_23 = -0.9592755f;
    private static float COLOR_MAT_HIGH_24 = 0.06024589f;
    private static float COLOR_MAT_HIGH_31 = -1.5958017f;
    private static float COLOR_MAT_HIGH_32 = 2.4951355f;
    private static float COLOR_MAT_HIGH_33 = 5.1863265f;
    private static float COLOR_MAT_HIGH_34 = -0.26762903f;
    private static float COLOR_MAT_LOW_11 = -0.85740393f;
    private static float COLOR_MAT_LOW_12 = 3.8810537f;
    private static float COLOR_MAT_LOW_13 = -1.2211744f;
    private static float COLOR_MAT_LOW_14 = 0.6837388f;
    private static float COLOR_MAT_LOW_21 = -2.835158f;
    private static float COLOR_MAT_LOW_22 = 6.03384f;
    private static float COLOR_MAT_LOW_23 = -0.9592755f;
    private static float COLOR_MAT_LOW_24 = 1.2456132f;
    private static float COLOR_MAT_LOW_31 = -1.5958017f;
    private static float COLOR_MAT_LOW_32 = 2.4951355f;
    private static float COLOR_MAT_LOW_33 = 5.1863265f;
    private static float COLOR_MAT_LOW_34 = -4.55602f;
    private static float DARK_ROOM_DELTA = 5.0f;
    private static float DARK_ROOM_DELTA1 = 10.0f;
    private static float DARK_ROOM_DELTA2 = 30.0f;
    private static float DARK_ROOM_THRESH = 3.0f;
    private static float DARK_ROOM_THRESH1 = 15.0f;
    private static float DARK_ROOM_THRESH2 = 35.0f;
    private static float DARK_ROOM_THRESH3 = 75.0f;
    private static boolean DEBUG = false;
    private static String DUAL_SENSOR_XML_PATH = "/product/etc/xml/lcd/DualSensorConfig.xml";
    private static final int FLASHLIGHT_DETECTION_MODE_CALLBACK = 1;
    private static final int FLASHLIGHT_DETECTION_MODE_THRESHOLD = 0;
    private static int FLASHLIGHT_OFF_TIME_THRESHOLD_MS = 600;
    private static final int FRONT_MSG_TIMER = 1;
    private static int[] FRONT_SENSOR_STABILITY_THRESHOLD = new int[]{-30, -4, 4, 30};
    private static final int FUSED_MSG_TIMER = 3;
    private static float IR_BOUNDRY = 1.0f;
    public static final int LIGHT_SENSOR_BACK = 1;
    public static final int LIGHT_SENSOR_DEFAULT = -1;
    public static final int LIGHT_SENSOR_DUAL = 2;
    public static final int LIGHT_SENSOR_FRONT = 0;
    private static float LUX_COEF_HIGH_BLUE = -0.06468f;
    private static float LUX_COEF_HIGH_GREEN = 0.19239f;
    private static float LUX_COEF_HIGH_IR = 0.5164043f;
    private static float LUX_COEF_HIGH_OFFSET = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private static float LUX_COEF_HIGH_RED = 0.02673f;
    private static float LUX_COEF_HIGH_X = -11.499684f;
    private static float LUX_COEF_HIGH_Y = 16.098469f;
    private static float LUX_COEF_HIGH_Z = -3.7601638f;
    private static float LUX_COEF_LOW_BLUE = -0.09306f;
    private static float LUX_COEF_LOW_GREEN = 0.02112f;
    private static float LUX_COEF_LOW_IR = -3.1549554f;
    private static float LUX_COEF_LOW_OFFSET = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private static float LUX_COEF_LOW_RED = 0.02442f;
    private static float LUX_COEF_LOW_X = -3.0825195f;
    private static float LUX_COEF_LOW_Y = 7.662419f;
    private static float LUX_COEF_LOW_Z = -6.400353f;
    private static final int MSG_TIMER = 0;
    private static float PRODUCTION_CALIBRATION_IR1 = 1.0f;
    private static float PRODUCTION_CALIBRATION_X = 1.0f;
    private static float PRODUCTION_CALIBRATION_Y = 1.0f;
    private static float PRODUCTION_CALIBRATION_Z = 1.0f;
    private static float RATIO_IR_GREEN = 0.5f;
    private static int[] STABILIZED_PROBABILITY_LUT = new int[25];
    private static final String TAG = "HwDualSensorEventListenerImpl";
    private static final int TIMER_DISABLE = -1;
    private static int VERSION_SENSOR = 1;
    private static final int WARM_UP_FLAG = 2;
    private static float X_CONSTANT = 0.56695f;
    private static float X_G_COEF = -1.33815f;
    private static float X_G_QUADRATIC_COEF = 1.36349f;
    private static float X_RG_COEF = 2.5769f;
    private static float X_R_COEF = -1.20192f;
    private static float X_R_QUADRATIC_COEF = 1.17303f;
    private static float Y_CONSTANT = -0.43006f;
    private static float Y_G_COEF = 1.6651f;
    private static float Y_G_QUADRATIC_COEF = -0.7337f;
    private static float Y_RG_COEF = -0.5513f;
    private static float Y_R_COEF = 0.77904f;
    private static float Y_R_QUADRATIC_COEF = 0.03152f;
    private static int mBackRateMillis = 300;
    private static final long mDebugPrintInterval = 1000000000;
    private static int mFlashlightDetectionMode = 0;
    private static int mFrontRateMillis = 300;
    private static int mFusedRateMillis = 300;
    private static volatile HwDualSensorEventListenerImpl mInstance;
    private static Hashtable<String, Integer> mModuleSensorMap = new Hashtable();
    private String mBackCameraId;
    private boolean mBackEnable = false;
    private long mBackEnableTime = -1;
    private Sensor mBackLightSensor;
    private SensorDataSender mBackSender;
    private int mBackSensorBypassCount = 0;
    private int mBackSensorBypassCountMax = 3;
    private SensorData mBackSensorData;
    private final SensorEventListener mBackSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            SensorEvent sensorEvent = event;
            if (HwDualSensorEventListenerImpl.this.mBackEnable) {
                int lux;
                int cct;
                long sensorClockTimeStamp = sensorEvent.timestamp;
                long systemClockTimeStamp = SystemClock.uptimeMillis();
                int[] arr = new int[]{0, 0};
                if (HwDualSensorEventListenerImpl.VERSION_SENSOR == 1) {
                    arr = HwDualSensorEventListenerImpl.this.convertXYZ2LuxAndCct(Float.valueOf(sensorEvent.values[0]), Float.valueOf(sensorEvent.values[1]), Float.valueOf(sensorEvent.values[2]), Float.valueOf(sensorEvent.values[3]));
                } else if (HwDualSensorEventListenerImpl.VERSION_SENSOR == 2) {
                    arr = HwDualSensorEventListenerImpl.this.convertRGB2LuxAndCct(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], sensorEvent.values[3]);
                } else if (HwDualSensorEventListenerImpl.DEBUG) {
                    String str = HwDualSensorEventListenerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid VERSION_SENSOR=");
                    stringBuilder.append(HwDualSensorEventListenerImpl.VERSION_SENSOR);
                    Slog.i(str, stringBuilder.toString());
                }
                int[] arr2 = arr;
                int lux2 = arr2[0];
                int cct2 = arr2[1];
                if (HwDualSensorEventListenerImpl.this.mBackWarmUpFlg >= 2) {
                    lux = lux2;
                    cct = cct2;
                    HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                } else if (sensorClockTimeStamp < HwDualSensorEventListenerImpl.this.mBackEnableTime) {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): Back sensor not ready yet!");
                    }
                    return;
                } else {
                    lux = lux2;
                    cct = cct2;
                    HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux2, cct2, systemClockTimeStamp, sensorClockTimeStamp);
                    HwDualSensorEventListenerImpl.this.mBackWarmUpFlg = HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + 1;
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(2);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(2);
                }
                if (!HwDualSensorEventListenerImpl.DEBUG || sensorClockTimeStamp - HwDualSensorEventListenerImpl.this.mBackEnableTime >= 1000000000) {
                    int i = cct;
                } else {
                    String str2 = HwDualSensorEventListenerImpl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SensorEventListener.onSensorChanged(): warmUp=");
                    stringBuilder2.append(HwDualSensorEventListenerImpl.this.mBackWarmUpFlg);
                    stringBuilder2.append(" lux=");
                    stringBuilder2.append(lux);
                    stringBuilder2.append(" cct=");
                    stringBuilder2.append(cct);
                    stringBuilder2.append(" sensorTime=");
                    stringBuilder2.append(sensorClockTimeStamp);
                    stringBuilder2.append(" systemTime=");
                    stringBuilder2.append(systemClockTimeStamp);
                    stringBuilder2.append(" Xraw=");
                    stringBuilder2.append(sensorEvent.values[0]);
                    stringBuilder2.append(" Yraw=");
                    stringBuilder2.append(sensorEvent.values[1]);
                    stringBuilder2.append(" Zraw=");
                    stringBuilder2.append(sensorEvent.values[2]);
                    Slog.d(str2, stringBuilder2.toString());
                }
                return;
            }
            Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): mBackEnable=false");
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mBackSensorLock = new Object();
    private int mBackSensorTimeOutTH = 5;
    private int mBackTimeOutCount = 0;
    private int mBackTimer = -1;
    private int mBackWarmUpFlg = 0;
    private CameraManager mCameraManager;
    private final Context mContext;
    private final HandlerThread mDualSensorProcessThread;
    private volatile long mFlashLightOffTimeStampMs = 0;
    private boolean mFrontEnable = false;
    private long mFrontEnableTime = -1;
    private Sensor mFrontLightSensor;
    private SensorDataSender mFrontSender;
    private SensorData mFrontSensorData;
    private final SensorEventListener mFrontSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (HwDualSensorEventListenerImpl.this.mFrontEnable) {
                long systemClockTimeStamp = SystemClock.uptimeMillis();
                int lux = (int) (event.values[0] + 0.5f);
                int cct = (int) (event.values[1] + 0.5f);
                long sensorClockTimeStamp = event.timestamp;
                if (HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg >= 2) {
                    HwDualSensorEventListenerImpl.this.mFrontSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                } else if (sensorClockTimeStamp < HwDualSensorEventListenerImpl.this.mFrontEnableTime) {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): Front sensor not ready yet!");
                    }
                    return;
                } else {
                    HwDualSensorEventListenerImpl.this.mFrontSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                    HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg = HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg + 1;
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(1);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(1);
                }
                if (HwDualSensorEventListenerImpl.DEBUG && sensorClockTimeStamp - HwDualSensorEventListenerImpl.this.mFrontEnableTime < 1000000000) {
                    String str = HwDualSensorEventListenerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SensorEventListener.onSensorChanged(): warmUp=");
                    stringBuilder.append(HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg);
                    stringBuilder.append(" lux=");
                    stringBuilder.append(lux);
                    stringBuilder.append(" cct=");
                    stringBuilder.append(cct);
                    stringBuilder.append(" sensorTime=");
                    stringBuilder.append(sensorClockTimeStamp);
                    stringBuilder.append(" systemTime=");
                    stringBuilder.append(systemClockTimeStamp);
                    Slog.d(str, stringBuilder.toString());
                }
                return;
            }
            Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): mFrontEnable=false");
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mFrontSensorLock = new Object();
    private int mFrontTimer = -1;
    private int mFrontWarmUpFlg = 0;
    private boolean mFusedEnable = false;
    private long mFusedEnableTime = -1;
    private SensorDataSender mFusedSender;
    private FusedSensorData mFusedSensorData;
    private final Object mFusedSensorLock = new Object();
    private int mFusedTimer = -1;
    private int mFusedWarmUpFlg = 0;
    private final Handler mHandler;
    private SensorManager mSensorManager;
    private int mTimer = -1;
    private TorchCallback mTorchCallback = new TorchCallback() {
        public void onTorchModeUnavailable(String cameraId) {
            super.onTorchModeUnavailable(cameraId);
            HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = -2;
            Slog.i(HwDualSensorEventListenerImpl.TAG, "onTorchModeUnavailable, mFlashLightOffTimeStampMs set -2");
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            if (!cameraId.equals(HwDualSensorEventListenerImpl.this.mBackCameraId)) {
                return;
            }
            if (enabled) {
                HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = -1;
                Slog.i(HwDualSensorEventListenerImpl.TAG, "mFlashLightOffTimeStampMs set -1");
                return;
            }
            HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = SystemClock.uptimeMillis();
            String str = HwDualSensorEventListenerImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mFlashLightOffTimeStampMs set ");
            stringBuilder.append(HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs);
            Slog.i(str, stringBuilder.toString());
        }
    };

    private final class DualSensorHandler extends Handler {
        private long mTimestamp = 0;

        public DualSensorHandler(Looper looper) {
            super(looper, null, true);
        }

        private void updateTimer(int newTimer) {
            if (newTimer == -1) {
                return;
            }
            if (HwDualSensorEventListenerImpl.this.mTimer == -1) {
                HwDualSensorEventListenerImpl.this.mTimer = newTimer;
            } else if (newTimer < HwDualSensorEventListenerImpl.this.mTimer) {
                HwDualSensorEventListenerImpl.this.mTimer = newTimer;
            }
        }

        private void updateTimerAndSendData(SensorDataSender sender) {
            sender.sendData();
            if (sender.needQueue()) {
                sender.setTimer(sender.getRate());
            } else {
                sender.setTimer(-1);
            }
        }

        private void processTimer(SensorDataSender sender) {
            int timer = sender.getTimer();
            if (timer == -1) {
                return;
            }
            if (HwDualSensorEventListenerImpl.this.mTimer != -1) {
                timer -= HwDualSensorEventListenerImpl.this.mTimer;
                if (timer < 0) {
                    timer = 0;
                }
                if (timer == 0) {
                    updateTimerAndSendData(sender);
                    return;
                }
                return;
            }
            sender.setTimer(-1);
            Slog.w(HwDualSensorEventListenerImpl.TAG, "Timer has been queued, but mTimer is invalid!");
        }

        private void processTimerAsTrigger(SensorDataSender sender) {
            updateTimerAndSendData(sender);
            if (sender.getTimer() == -1) {
                return;
            }
            if (HwDualSensorEventListenerImpl.this.mTimer == -1) {
                queueMessage(sender.getTimer());
                return;
            }
            if (HwDualSensorEventListenerImpl.this.mTimer - ((int) (SystemClock.uptimeMillis() - this.mTimestamp)) > sender.getTimer()) {
                queueMessage(sender.getTimer());
            }
        }

        private void queueMessage(int timer) {
            HwDualSensorEventListenerImpl.this.mTimer = timer;
            queueMessage();
        }

        private void queueMessage() {
            this.mTimestamp = SystemClock.uptimeMillis();
            removeMessages(0);
            sendEmptyMessageDelayed(0, (long) HwDualSensorEventListenerImpl.this.mTimer);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    processTimer(HwDualSensorEventListenerImpl.this.mFrontSender);
                    processTimer(HwDualSensorEventListenerImpl.this.mBackSender);
                    processTimer(HwDualSensorEventListenerImpl.this.mFusedSender);
                    HwDualSensorEventListenerImpl.this.mTimer = -1;
                    updateTimer(HwDualSensorEventListenerImpl.this.mFrontTimer);
                    updateTimer(HwDualSensorEventListenerImpl.this.mBackTimer);
                    updateTimer(HwDualSensorEventListenerImpl.this.mFusedTimer);
                    if (HwDualSensorEventListenerImpl.this.mTimer != -1) {
                        queueMessage();
                        return;
                    }
                    return;
                case 1:
                    processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mFrontSender);
                    return;
                case 2:
                    processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mBackSender);
                    return;
                case 3:
                    processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mFusedSender);
                    return;
                default:
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "handleMessage: Invalid message");
                    return;
            }
        }
    }

    private class FusedSensorData extends Observable {
        private float mAdjustMin;
        private float mBackFloorThresh;
        private long mBackLastValueSystemTime = -1;
        private int[] mBackLuxArray = new int[]{-1, -1, -1};
        private long mBackLuxDeviation;
        private int mBackLuxFiltered = -1;
        private int mBackLuxIndex = 0;
        private float mBackLuxRelativeChange = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private int mBackLuxStaProInd;
        private float mBackRoofThresh;
        public SensorDataObserver mBackSensorDataObserver;
        private boolean mBackUpdated;
        private int mCurBackCct = -1;
        private float mCurBackFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private int mCurBackLux = -1;
        private int mCurFrontCct = -1;
        private float mCurFrontFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private int mCurFrontLux = -1;
        private float mDarkRoomDelta;
        private float mDarkRoomDelta1;
        private float mDarkRoomDelta2;
        private float mDarkRoomThresh;
        private float mDarkRoomThresh1;
        private float mDarkRoomThresh2;
        private float mDarkRoomThresh3;
        private long mDebugPrintTime = -1;
        private long mFrontLastValueSystemTime = -1;
        private int[] mFrontLuxArray = new int[]{-1, -1, -1};
        private int mFrontLuxFiltered = -1;
        private int mFrontLuxIndex = 0;
        private float mFrontLuxRelativeChange = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private int mFrontLuxStaProInd;
        public SensorDataObserver mFrontSensorDataObserver;
        private boolean mFrontUpdated;
        private int mFusedCct = -1;
        private int mFusedLux = -1;
        private long mLastValueBackSensorTime = -1;
        private long mLastValueFrontSensorTime = -1;
        private float mModifyFactor;
        private float mPreBackFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private float mPreFrontFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private float mStaProb;
        private int[][] mStaProbLUT = ((int[][]) Array.newInstance(int.class, new int[]{5, 5}));

        public class SensorDataObserver implements Observer {
            private final String mName;
            private final FusedSensorDataRefresher mRefresher;

            public SensorDataObserver(FusedSensorDataRefresher refresher, String name) {
                this.mRefresher = refresher;
                this.mName = name;
            }

            public void update(Observable o, Object arg) {
                long[] data = (long[]) arg;
                int lux = (int) data[0];
                int cct = (int) data[1];
                long systemTimeStamp = data[2];
                long sensorTimeStamp = data[3];
                FusedSensorData.this.mDebugPrintTime = SystemClock.elapsedRealtimeNanos();
                if (HwDualSensorEventListenerImpl.DEBUG && FusedSensorData.this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                    String str = HwDualSensorEventListenerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("FusedSensorData.update(): ");
                    stringBuilder.append(this.mName);
                    stringBuilder.append(" warmUp=");
                    stringBuilder.append(HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg);
                    stringBuilder.append(" lux=");
                    stringBuilder.append(lux);
                    stringBuilder.append(" cct=");
                    stringBuilder.append(cct);
                    stringBuilder.append(" systemTime=");
                    stringBuilder.append(systemTimeStamp);
                    stringBuilder.append(" sensorTime=");
                    stringBuilder.append(sensorTimeStamp);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mRefresher.updateData(lux, cct, systemTimeStamp, sensorTimeStamp);
                if (HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg < 2) {
                    if (FusedSensorData.this.mFrontUpdated && FusedSensorData.this.mBackUpdated) {
                        HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg = HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg + 1;
                    }
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(3);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(3);
                }
            }
        }

        public FusedSensorData() {
            this.mStaProbLUT[0][0] = 0;
            this.mStaProbLUT[0][1] = 20;
            this.mStaProbLUT[0][2] = 90;
            this.mStaProbLUT[0][3] = 20;
            this.mStaProbLUT[0][4] = 0;
            this.mStaProbLUT[1][0] = 20;
            this.mStaProbLUT[1][1] = 50;
            this.mStaProbLUT[1][2] = 90;
            this.mStaProbLUT[1][3] = 50;
            this.mStaProbLUT[1][4] = 20;
            this.mStaProbLUT[2][0] = 90;
            this.mStaProbLUT[2][1] = 90;
            this.mStaProbLUT[2][2] = 95;
            this.mStaProbLUT[2][3] = 90;
            this.mStaProbLUT[2][4] = 90;
            this.mStaProbLUT[3][0] = 20;
            this.mStaProbLUT[3][1] = 50;
            this.mStaProbLUT[3][2] = 90;
            this.mStaProbLUT[3][3] = 50;
            this.mStaProbLUT[3][4] = 20;
            this.mStaProbLUT[4][0] = 0;
            this.mStaProbLUT[4][1] = 20;
            this.mStaProbLUT[4][2] = 90;
            this.mStaProbLUT[4][3] = 20;
            this.mStaProbLUT[4][4] = 0;
            this.mFrontLuxStaProInd = 0;
            this.mBackLuxStaProInd = 0;
            this.mAdjustMin = 1.0f;
            this.mBackRoofThresh = HwDualSensorEventListenerImpl.BACK_ROOF_THRESH;
            this.mBackFloorThresh = HwDualSensorEventListenerImpl.BACK_FLOOR_THRESH;
            this.mDarkRoomThresh = HwDualSensorEventListenerImpl.DARK_ROOM_THRESH;
            this.mDarkRoomDelta = HwDualSensorEventListenerImpl.DARK_ROOM_DELTA;
            this.mDarkRoomThresh1 = HwDualSensorEventListenerImpl.DARK_ROOM_THRESH1;
            this.mDarkRoomThresh2 = HwDualSensorEventListenerImpl.DARK_ROOM_THRESH2;
            this.mDarkRoomThresh3 = HwDualSensorEventListenerImpl.DARK_ROOM_THRESH3;
            this.mDarkRoomDelta1 = HwDualSensorEventListenerImpl.DARK_ROOM_DELTA1;
            this.mDarkRoomDelta2 = HwDualSensorEventListenerImpl.DARK_ROOM_DELTA2;
            this.mBackLuxDeviation = 0;
            this.mFrontSensorDataObserver = new SensorDataObserver(new FusedSensorDataRefresher(HwDualSensorEventListenerImpl.this) {
                public void updateData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
                    FusedSensorData.this.setFrontData(lux, cct, systemTimeStamp, sensorTimeStamp);
                }
            }, "FrontSensor");
            this.mBackSensorDataObserver = new SensorDataObserver(new FusedSensorDataRefresher(HwDualSensorEventListenerImpl.this) {
                public void updateData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
                    FusedSensorData.this.setBackData(lux, cct, systemTimeStamp, sensorTimeStamp);
                }
            }, "BackSensor");
            this.mFrontUpdated = false;
            this.mBackUpdated = false;
            setStabilityProbabilityLUT();
        }

        public void setFrontData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
            this.mFrontLuxStaProInd = getLuxStableProbabilityIndex(lux, true);
            this.mCurFrontCct = cct;
            this.mFrontLastValueSystemTime = systemTimeStamp;
            this.mLastValueFrontSensorTime = sensorTimeStamp;
            this.mFrontUpdated = true;
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                String str = HwDualSensorEventListenerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("FusedSensorData.setFrontData(): mCurFrontFiltered=");
                stringBuilder.append(this.mCurFrontFiltered);
                stringBuilder.append(" mFrontLuxRelativeChange= ");
                stringBuilder.append(this.mFrontLuxRelativeChange);
                stringBuilder.append(" lux=");
                stringBuilder.append(lux);
                stringBuilder.append(" cct=");
                stringBuilder.append(cct);
                stringBuilder.append(" systemTime=");
                stringBuilder.append(this.mFrontLastValueSystemTime);
                stringBuilder.append(" sensorTime=");
                stringBuilder.append(this.mLastValueFrontSensorTime);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public void setBackData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
            this.mBackLuxStaProInd = getLuxStableProbabilityIndex(lux, false);
            this.mCurBackCct = cct;
            this.mBackLastValueSystemTime = systemTimeStamp;
            this.mLastValueBackSensorTime = sensorTimeStamp;
            this.mBackUpdated = true;
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                String str = HwDualSensorEventListenerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("FusedSensorData.setBackData(): mCurBackFiltered=");
                stringBuilder.append(this.mCurBackFiltered);
                stringBuilder.append(" mBackLuxRelativeChange=");
                stringBuilder.append(this.mBackLuxRelativeChange);
                stringBuilder.append(" lux=");
                stringBuilder.append(lux);
                stringBuilder.append(" cct=");
                stringBuilder.append(cct);
                stringBuilder.append(" systemTime=");
                stringBuilder.append(this.mBackLastValueSystemTime);
                stringBuilder.append(" sensorTime=");
                stringBuilder.append(this.mLastValueBackSensorTime);
                Slog.d(str, stringBuilder.toString());
            }
        }

        private int getLuxStableProbabilityIndex(int lux, boolean frontSensorIsSelected) {
            int i = lux;
            boolean z = frontSensorIsSelected;
            if (i < 0 && HwDualSensorEventListenerImpl.DEBUG) {
                String str = HwDualSensorEventListenerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getLuxStableProbabilityIndex exception: lux=");
                stringBuilder.append(i);
                stringBuilder.append(" frontSensorIsSelected=");
                stringBuilder.append(z);
                Slog.d(str, stringBuilder.toString());
            }
            int[] luxArray = z ? this.mFrontLuxArray : this.mBackLuxArray;
            int luxArrayIndex = z ? this.mFrontLuxIndex : this.mBackLuxIndex;
            float preFiltered = z ? this.mPreFrontFiltered : this.mPreBackFiltered;
            int[] stabilityThresholdList = z ? HwDualSensorEventListenerImpl.FRONT_SENSOR_STABILITY_THRESHOLD : HwDualSensorEventListenerImpl.BACK_SENSOR_STABILITY_THRESHOLD;
            luxArray[luxArrayIndex] = i;
            int luxArrayIndex2 = (luxArrayIndex + 1) % 3;
            int count = 0;
            int curInd = 0;
            int sum = 0;
            for (luxArrayIndex = 0; luxArrayIndex < 3; luxArrayIndex++) {
                if (luxArray[luxArrayIndex] > 0) {
                    sum += luxArray[luxArrayIndex];
                    count++;
                }
            }
            float curFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            if (count > 0) {
                curFiltered = ((float) sum) / ((float) count);
            }
            if (z) {
            } else {
                this.mBackLuxDeviation = 0;
                while (true) {
                    int curInd2 = curInd;
                    if (curInd2 >= luxArray.length) {
                        break;
                    }
                    int count2 = count;
                    this.mBackLuxDeviation += Math.abs(((long) luxArray[curInd2]) - ((long) curFiltered));
                    curInd = curInd2 + 1;
                    count = count2;
                }
                this.mBackLuxDeviation /= (long) luxArray.length;
            }
            float luxRelativeChange = 100.0f;
            if (Float.compare(preFiltered, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
                luxRelativeChange = (100.0f * (curFiltered - preFiltered)) / preFiltered;
            }
            count = 0;
            curInd = stabilityThresholdList.length;
            while (count < curInd && Float.compare(luxRelativeChange, (float) stabilityThresholdList[count]) > 0) {
                count++;
            }
            if (count > curInd) {
                count = curInd;
            }
            if (z) {
                this.mCurFrontFiltered = curFiltered;
                this.mFrontLuxRelativeChange = luxRelativeChange;
                this.mFrontLuxIndex = luxArrayIndex2;
            } else {
                this.mCurBackFiltered = curFiltered;
                this.mBackLuxRelativeChange = luxRelativeChange;
                this.mBackLuxIndex = luxArrayIndex2;
            }
            return count;
        }

        private void updateFusedData() {
            getStabilizedLuxAndCct();
        }

        private void getStabilizedLuxAndCct() {
            String str;
            StringBuilder stringBuilder;
            this.mFrontLuxFiltered = (int) this.mCurFrontFiltered;
            this.mBackLuxFiltered = (int) this.mCurBackFiltered;
            if (Float.compare(this.mCurFrontFiltered, this.mCurBackFiltered) >= 0) {
                this.mCurFrontLux = (int) this.mCurFrontFiltered;
                this.mCurBackLux = (int) this.mCurBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    if (this.mCurFrontLux < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = this.mCurFrontLux;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    this.mFusedLux = this.mCurFrontLux;
                    this.mFusedCct = this.mCurFrontCct > this.mCurBackCct ? this.mCurFrontCct : this.mCurBackCct;
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        str = HwDualSensorEventListenerImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=");
                        stringBuilder.append(this.mCurFrontLux);
                        stringBuilder.append(" mCurBackLux=");
                        stringBuilder.append(this.mCurBackLux);
                        Slog.d(str, stringBuilder.toString());
                    }
                    return;
                }
            } else if (isFlashlightOn()) {
                this.mBackLuxFiltered = this.mFrontLuxFiltered;
                this.mCurFrontLux = (int) this.mCurFrontFiltered;
                this.mCurBackLux = (int) this.mCurBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    if (this.mCurFrontLux < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = this.mCurFrontLux;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    this.mFusedLux = this.mCurFrontLux;
                    this.mFusedCct = this.mCurFrontCct > this.mCurBackCct ? this.mCurFrontCct : this.mCurBackCct;
                    if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                        str = HwDualSensorEventListenerImpl.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("FusedSensorData.updateFusedData(): mFusedLux=");
                        stringBuilder2.append(this.mFusedLux);
                        stringBuilder2.append(" mCurFrontLux=");
                        stringBuilder2.append(this.mCurFrontLux);
                        stringBuilder2.append(" mCurBackLux=");
                        stringBuilder2.append(this.mCurBackLux);
                        stringBuilder2.append(" mFusedCct=");
                        stringBuilder2.append(this.mFusedCct);
                        stringBuilder2.append(" mCurFrontCct=");
                        stringBuilder2.append(this.mCurFrontCct);
                        stringBuilder2.append(" mCurBackCct=");
                        stringBuilder2.append(this.mCurBackCct);
                        Slog.d(str, stringBuilder2.toString());
                    }
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        str = HwDualSensorEventListenerImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=");
                        stringBuilder.append(this.mCurFrontLux);
                        stringBuilder.append(" mCurBackLux=");
                        stringBuilder.append(this.mCurBackLux);
                        Slog.d(str, stringBuilder.toString());
                    }
                    return;
                }
            } else {
                float curDarkRoomDelta;
                float darkRoomDelta3 = this.mCurBackFiltered - this.mCurFrontFiltered;
                if (this.mCurFrontFiltered >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && this.mCurFrontFiltered <= this.mDarkRoomThresh) {
                    curDarkRoomDelta = this.mDarkRoomDelta;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh1) <= 0) {
                    curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh) * (this.mDarkRoomDelta1 - this.mDarkRoomDelta)) / (this.mDarkRoomThresh1 - this.mDarkRoomThresh)) + this.mDarkRoomDelta;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh2) < 0) {
                    curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh1) * (this.mDarkRoomDelta2 - this.mDarkRoomDelta1)) / (this.mDarkRoomThresh2 - this.mDarkRoomThresh1)) + this.mDarkRoomDelta1;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh3) < 0) {
                    curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh2) * (darkRoomDelta3 - this.mDarkRoomDelta2)) / (this.mDarkRoomThresh3 - this.mDarkRoomThresh2)) + this.mDarkRoomDelta2;
                } else {
                    curDarkRoomDelta = darkRoomDelta3;
                }
                this.mBackFloorThresh = this.mCurFrontFiltered + curDarkRoomDelta;
                this.mCurBackFiltered = this.mCurBackFiltered < this.mBackFloorThresh ? this.mCurBackFiltered : this.mBackFloorThresh;
                this.mStaProb = (float) this.mStaProbLUT[this.mBackLuxStaProInd][this.mFrontLuxStaProInd];
                if (this.mStaProb == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    this.mPreFrontFiltered = this.mCurFrontFiltered;
                    this.mPreBackFiltered = this.mCurBackFiltered;
                } else {
                    if (this.mStaProb == 20.0f) {
                        this.mModifyFactor = 0.02f;
                    } else if (this.mStaProb == 50.0f) {
                        this.mModifyFactor = 0.01f;
                    } else if (this.mStaProb == 90.0f) {
                        this.mModifyFactor = 0.004f;
                    } else {
                        this.mModifyFactor = 0.002f;
                    }
                    float FrontAdjustment = this.mModifyFactor * this.mPreFrontFiltered;
                    if (FrontAdjustment < this.mAdjustMin) {
                        FrontAdjustment = this.mAdjustMin;
                    }
                    float BackAdjustment = this.mModifyFactor * this.mPreBackFiltered;
                    if (BackAdjustment < this.mAdjustMin) {
                        BackAdjustment = this.mAdjustMin;
                    }
                    if (this.mCurFrontFiltered < this.mPreFrontFiltered) {
                        this.mPreFrontFiltered -= FrontAdjustment;
                    } else if (this.mCurFrontFiltered > this.mPreFrontFiltered) {
                        this.mPreFrontFiltered += FrontAdjustment;
                    }
                    if (this.mCurBackFiltered < this.mPreBackFiltered) {
                        this.mPreBackFiltered -= BackAdjustment;
                    } else if (this.mCurFrontFiltered > this.mPreFrontFiltered) {
                        this.mPreBackFiltered += BackAdjustment;
                    }
                }
                this.mCurFrontLux = (int) this.mPreFrontFiltered;
                this.mCurBackLux = (int) this.mPreBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    if (this.mCurFrontLux < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = this.mCurFrontLux;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    this.mFusedLux = this.mCurFrontLux > this.mCurBackLux ? this.mCurFrontLux : this.mCurBackLux;
                    this.mFusedCct = this.mCurFrontCct > this.mCurBackCct ? this.mCurFrontCct : this.mCurBackCct;
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        String str2 = HwDualSensorEventListenerImpl.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=");
                        stringBuilder3.append(this.mCurFrontLux);
                        stringBuilder3.append(" mCurBackLux=");
                        stringBuilder3.append(this.mCurBackLux);
                        Slog.d(str2, stringBuilder3.toString());
                    }
                    return;
                }
            }
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                str = HwDualSensorEventListenerImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("FusedSensorData.updateFusedData(): mFusedLux=");
                stringBuilder.append(this.mFusedLux);
                stringBuilder.append(" mCurFrontLux=");
                stringBuilder.append(this.mCurFrontLux);
                stringBuilder.append(" mCurBackLux=");
                stringBuilder.append(this.mCurBackLux);
                stringBuilder.append(" mFusedCct=");
                stringBuilder.append(this.mFusedCct);
                stringBuilder.append(" mCurFrontCct=");
                stringBuilder.append(this.mCurFrontCct);
                stringBuilder.append(" mCurBackCct=");
                stringBuilder.append(this.mCurBackCct);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public void sendFusedData() {
            String str;
            StringBuilder stringBuilder;
            if (this.mFrontUpdated) {
                if (HwDualSensorEventListenerImpl.this.mBackSensorBypassCount <= 0 && !this.mBackUpdated) {
                    HwDualSensorEventListenerImpl.this.mBackTimeOutCount = HwDualSensorEventListenerImpl.this.mBackTimeOutCount + 1;
                    if (HwDualSensorEventListenerImpl.this.mBackTimeOutCount >= HwDualSensorEventListenerImpl.this.mBackSensorTimeOutTH) {
                        HwDualSensorEventListenerImpl.this.mBackSensorBypassCount = HwDualSensorEventListenerImpl.this.mBackSensorBypassCountMax;
                        str = HwDualSensorEventListenerImpl.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mBackTimeOutCount start: ");
                        stringBuilder2.append(HwDualSensorEventListenerImpl.this.mBackTimeOutCount);
                        Slog.i(str, stringBuilder2.toString());
                    } else {
                        if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                            str = HwDualSensorEventListenerImpl.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Skip this sendFusedData! mFrontUpdated=");
                            stringBuilder.append(this.mFrontUpdated);
                            stringBuilder.append(" mBackUpdated=");
                            stringBuilder.append(this.mBackUpdated);
                            Slog.d(str, stringBuilder.toString());
                        }
                        return;
                    }
                }
                HwDualSensorEventListenerImpl.this.mBackTimeOutCount = 0;
                updateFusedData();
                long lastValueSensorTime = this.mLastValueFrontSensorTime > this.mLastValueBackSensorTime ? this.mLastValueFrontSensorTime : this.mLastValueBackSensorTime;
                long lastValueSystemTime = this.mFrontLastValueSystemTime > this.mBackLastValueSystemTime ? this.mFrontLastValueSystemTime : this.mBackLastValueSystemTime;
                Object dataToSend = new long[]{(long) this.mFusedLux, (long) this.mFusedCct, lastValueSystemTime, lastValueSensorTime, (long) this.mFrontLuxFiltered, (long) this.mBackLuxFiltered};
                String str2;
                StringBuilder stringBuilder3;
                if (this.mFusedLux < 0 || this.mFusedCct < 0) {
                    str2 = HwDualSensorEventListenerImpl.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("FusedSensorData.sendFusedData(): skip. mFusedLux=");
                    stringBuilder3.append(this.mFusedLux);
                    stringBuilder3.append(" mFusedCct=");
                    stringBuilder3.append(this.mFusedCct);
                    Slog.i(str2, stringBuilder3.toString());
                    return;
                }
                if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                    str2 = HwDualSensorEventListenerImpl.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("FusedSensorData.sendFusedData(): lux=");
                    stringBuilder3.append(this.mFusedLux);
                    stringBuilder3.append(" cct=");
                    stringBuilder3.append(this.mFusedCct);
                    stringBuilder3.append(" sensorTime=");
                    stringBuilder3.append(lastValueSensorTime);
                    stringBuilder3.append(" FrontSensorTime=");
                    stringBuilder3.append(this.mLastValueFrontSensorTime);
                    stringBuilder3.append(" BackSensorTime=");
                    stringBuilder3.append(this.mLastValueBackSensorTime);
                    Slog.d(str2, stringBuilder3.toString());
                }
                setChanged();
                notifyObservers(dataToSend);
                return;
            }
            if (HwDualSensorEventListenerImpl.DEBUG) {
                str = HwDualSensorEventListenerImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skip this sendFusedData! mFrontUpdated=");
                stringBuilder.append(this.mFrontUpdated);
                stringBuilder.append(" mBackUpdated=");
                stringBuilder.append(this.mBackUpdated);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public int getSensorLuxData() {
            return this.mFusedLux;
        }

        public int getSensorCctData() {
            return this.mFusedCct;
        }

        public long getSensorDataTime() {
            return this.mFrontLastValueSystemTime > this.mBackLastValueSystemTime ? this.mFrontLastValueSystemTime : this.mBackLastValueSystemTime;
        }

        public void clearSensorData() {
            this.mCurFrontLux = -1;
            this.mCurFrontCct = -1;
            this.mCurBackLux = -1;
            this.mCurBackCct = -1;
            this.mFusedLux = -1;
            this.mFusedCct = -1;
            this.mLastValueFrontSensorTime = -1;
            this.mLastValueBackSensorTime = -1;
            this.mFrontLastValueSystemTime = -1;
            this.mBackLastValueSystemTime = -1;
            this.mDebugPrintTime = -1;
            this.mCurFrontFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mPreFrontFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mCurBackFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mPreBackFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mFrontLuxIndex = 0;
            this.mBackLuxIndex = 0;
            this.mFrontLuxRelativeChange = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mBackLuxRelativeChange = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mFrontLuxStaProInd = 0;
            this.mBackLuxStaProInd = 0;
            this.mAdjustMin = 1.0f;
            this.mFrontUpdated = false;
            this.mBackUpdated = false;
            this.mFrontLuxArray = new int[]{-1, -1, -1};
            this.mBackLuxArray = new int[]{-1, -1, -1};
        }

        public void setStabilityProbabilityLUT() {
            if (HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT == null) {
                if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "STABILIZED_PROBABILITY_LUT is null");
                }
            } else if (HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT.length != 25) {
                if (HwDualSensorEventListenerImpl.DEBUG) {
                    String str = HwDualSensorEventListenerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("STABILIZED_PROBABILITY_LUT.length=");
                    stringBuilder.append(HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT.length);
                    Slog.i(str, stringBuilder.toString());
                }
            } else {
                for (int rowInd = 0; rowInd < 5; rowInd++) {
                    for (int colInd = 0; colInd < 5; colInd++) {
                        this.mStaProbLUT[rowInd][colInd] = HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT[(rowInd * 5) + colInd];
                    }
                }
            }
        }

        private boolean isFlashlightOn() {
            boolean z = true;
            switch (HwDualSensorEventListenerImpl.mFlashlightDetectionMode) {
                case 0:
                    if (this.mCurBackFiltered <= this.mBackRoofThresh && this.mBackLuxDeviation <= HwDualSensorEventListenerImpl.BACK_LUX_DEVIATION_THRESH) {
                        z = false;
                    }
                    return z;
                case 1:
                    if (HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs == -1 || SystemClock.uptimeMillis() - HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs < ((long) HwDualSensorEventListenerImpl.FLASHLIGHT_OFF_TIME_THRESHOLD_MS)) {
                        return true;
                    }
                    if (HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs == -2) {
                        if (this.mCurBackFiltered <= this.mBackRoofThresh && this.mBackLuxDeviation <= HwDualSensorEventListenerImpl.BACK_LUX_DEVIATION_THRESH) {
                            z = false;
                        }
                        return z;
                    }
                    break;
            }
            return false;
        }
    }

    private interface FusedSensorDataRefresher {
        void updateData(int i, int i2, long j, long j2);
    }

    private static class SensorData extends Observable {
        private List<Integer> mCctDataList;
        private long mEnableTime;
        private int mLastSensorCctValue;
        private int mLastSensorLuxValue;
        private long mLastValueSensorTime;
        private long mLastValueSystemTime;
        private List<Integer> mLuxDataList;

        /* synthetic */ SensorData(int x0, AnonymousClass1 x1) {
            this(x0);
        }

        private SensorData(int rateMillis) {
            this.mLastSensorLuxValue = -1;
            this.mLastSensorCctValue = -1;
            this.mLastValueSystemTime = -1;
            this.mLastValueSensorTime = -1;
            this.mEnableTime = -1;
            this.mLuxDataList = new CopyOnWriteArrayList();
            this.mCctDataList = new CopyOnWriteArrayList();
        }

        private void setSensorData(int lux, int cct, long systemClockTimeStamp, long sensorClockTimeStamp) {
            this.mLuxDataList.add(Integer.valueOf(lux));
            this.mCctDataList.add(Integer.valueOf(cct));
            this.mLastValueSystemTime = systemClockTimeStamp;
            this.mLastValueSensorTime = sensorClockTimeStamp;
        }

        private void sendSensorData() {
            int average;
            int count = 0;
            int sum = 0;
            for (Integer data : this.mLuxDataList) {
                sum += data.intValue();
                count++;
            }
            if (count != 0) {
                average = sum / count;
                if (average >= 0) {
                    this.mLastSensorLuxValue = average;
                }
            }
            this.mLuxDataList.clear();
            count = 0;
            sum = 0;
            for (Integer data2 : this.mCctDataList) {
                sum += data2.intValue();
                count++;
            }
            if (count != 0) {
                average = sum / count;
                if (average >= 0) {
                    this.mLastSensorCctValue = average;
                }
            }
            this.mCctDataList.clear();
            if (this.mLastSensorLuxValue < 0 || this.mLastSensorCctValue < 0) {
                String str = HwDualSensorEventListenerImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SensorData.sendSensorData(): skip. mLastSensorLuxValue=");
                stringBuilder.append(this.mLastSensorLuxValue);
                stringBuilder.append(" mLastSensorCctValue=");
                stringBuilder.append(this.mLastSensorCctValue);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            Object data3 = new long[]{(long) this.mLastSensorLuxValue, (long) this.mLastSensorCctValue, this.mLastValueSystemTime, this.mLastValueSensorTime};
            if (HwDualSensorEventListenerImpl.DEBUG && SystemClock.elapsedRealtimeNanos() - this.mEnableTime < 1000000000) {
                String str2 = HwDualSensorEventListenerImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SensorData.sendSensorData(): lux=");
                stringBuilder2.append(this.mLastSensorLuxValue);
                stringBuilder2.append(" cct=");
                stringBuilder2.append(this.mLastSensorCctValue);
                stringBuilder2.append(" systemTime=");
                stringBuilder2.append(this.mLastValueSystemTime);
                stringBuilder2.append(" sensorTime=");
                stringBuilder2.append(this.mLastValueSensorTime);
                Slog.d(str2, stringBuilder2.toString());
            }
            Object dataToSend = data3;
            setChanged();
            notifyObservers(dataToSend);
        }

        private void setEnableTime(long timeNanos) {
            this.mEnableTime = timeNanos;
        }

        public int getSensorLuxData() {
            return this.mLastSensorLuxValue;
        }

        public int getSensorCctData() {
            return this.mLastSensorCctValue;
        }

        public long getSensorDataTime() {
            return this.mLastValueSystemTime;
        }

        private void clearSensorData() {
            this.mLuxDataList.clear();
            this.mCctDataList.clear();
            this.mLastSensorLuxValue = -1;
            this.mLastSensorCctValue = -1;
            this.mLastValueSystemTime = -1;
            this.mLastValueSensorTime = -1;
            this.mEnableTime = -1;
        }
    }

    private interface SensorDataSender {
        int getRate();

        int getTimer();

        boolean needQueue();

        void sendData();

        void setTimer(int i);
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    private HwDualSensorEventListenerImpl(Context context, SensorManager sensorManager) {
        this.mContext = context;
        if (!parseParameters()) {
            Slog.i(TAG, "parse parameters failed!");
        }
        this.mFrontSensorData = new SensorData(mFrontRateMillis, null);
        this.mBackSensorData = new SensorData(mBackRateMillis, null);
        this.mFusedSensorData = new FusedSensorData();
        this.mFrontSender = new SensorDataSender() {
            public int getRate() {
                return HwDualSensorEventListenerImpl.mFrontRateMillis;
            }

            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mFrontTimer;
            }

            public void setTimer(int timer) {
                HwDualSensorEventListenerImpl.this.mFrontTimer = timer;
            }

            public void sendData() {
                HwDualSensorEventListenerImpl.this.mFrontSensorData.sendSensorData();
            }

            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mFrontEnable && HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg >= 2;
            }
        };
        this.mBackSender = new SensorDataSender() {
            public int getRate() {
                return HwDualSensorEventListenerImpl.mBackRateMillis;
            }

            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mBackTimer;
            }

            public void setTimer(int timer) {
                HwDualSensorEventListenerImpl.this.mBackTimer = timer;
            }

            public void sendData() {
                HwDualSensorEventListenerImpl.this.mBackSensorData.sendSensorData();
            }

            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mBackEnable && HwDualSensorEventListenerImpl.this.mBackWarmUpFlg >= 2;
            }
        };
        this.mFusedSender = new SensorDataSender() {
            public int getRate() {
                return HwDualSensorEventListenerImpl.mFusedRateMillis;
            }

            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mFusedTimer;
            }

            public void setTimer(int timer) {
                HwDualSensorEventListenerImpl.this.mFusedTimer = timer;
            }

            public void sendData() {
                synchronized (HwDualSensorEventListenerImpl.this.mFusedSensorLock) {
                    HwDualSensorEventListenerImpl.this.mFusedSensorData.sendFusedData();
                }
            }

            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mFusedEnable && HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg >= 2;
            }
        };
        this.mDualSensorProcessThread = new HandlerThread(TAG);
        this.mDualSensorProcessThread.start();
        this.mHandler = new DualSensorHandler(this.mDualSensorProcessThread.getLooper());
        this.mSensorManager = sensorManager;
        if (mFlashlightDetectionMode == 1) {
            registerBackCameraTorchCallback();
        }
    }

    public static HwDualSensorEventListenerImpl getInstance(SensorManager sensorManager, Context context) {
        if (mInstance == null) {
            synchronized (HwDualSensorEventListenerImpl.class) {
                if (mInstance == null) {
                    mInstance = new HwDualSensorEventListenerImpl(context, sensorManager);
                }
            }
        }
        return mInstance;
    }

    public void attachFrontSensorData(Observer observer) {
        if (this.mSensorManager == null) {
            Slog.e(TAG, "SensorManager is null!");
            return;
        }
        synchronized (this.mFrontSensorLock) {
            if (this.mFrontLightSensor == null) {
                this.mFrontLightSensor = this.mSensorManager.getDefaultSensor(5);
                if (DEBUG) {
                    Slog.d(TAG, "Obtain mFrontLightSensor at firt time.");
                }
            }
            enableFrontSensor();
            this.mFrontSensorData.addObserver(observer);
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0042, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void attachBackSensorData(Observer observer) {
        if (this.mSensorManager == null) {
            Slog.e(TAG, "SensorManager is null!");
            return;
        }
        synchronized (this.mBackSensorLock) {
            if (this.mBackLightSensor == null) {
                List<Sensor> LightSensorList = this.mSensorManager.getSensorList(65554);
                if (LightSensorList.size() == 1) {
                    this.mBackLightSensor = (Sensor) LightSensorList.get(0);
                    if (DEBUG) {
                        Slog.d(TAG, "Obtain mBackLightSensor at firt time.");
                    }
                } else if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("more than one color sensor: ");
                    stringBuilder.append(LightSensorList.size());
                    Slog.d(str, stringBuilder.toString());
                }
            }
            enableBackSensor();
            this.mBackSensorData.addObserver(observer);
        }
    }

    public void attachFusedSensorData(Observer observer) {
        synchronized (this.mFusedSensorLock) {
            attachFrontSensorData(this.mFusedSensorData.mFrontSensorDataObserver);
            attachBackSensorData(this.mFusedSensorData.mBackSensorDataObserver);
            enableFusedSensor();
            this.mFusedSensorData.addObserver(observer);
        }
    }

    public void detachFrontSensorData(Observer observer) {
        if (observer != null) {
            synchronized (this.mFrontSensorLock) {
                this.mFrontSensorData.deleteObserver(observer);
                if (this.mFrontSensorData.countObservers() == 0) {
                    disableFrontSensor();
                }
            }
        }
    }

    public void detachBackSensorData(Observer observer) {
        if (observer != null) {
            synchronized (this.mBackSensorLock) {
                this.mBackSensorData.deleteObserver(observer);
                if (this.mBackSensorData.countObservers() == 0) {
                    disableBackSensor();
                }
            }
        }
    }

    public void detachFusedSensorData(Observer observer) {
        if (observer != null) {
            synchronized (this.mFusedSensorLock) {
                this.mFusedSensorData.deleteObserver(observer);
                if (this.mFusedSensorData.countObservers() == 0) {
                    disableFusedSensor();
                    detachFrontSensorData(this.mFusedSensorData.mFrontSensorDataObserver);
                    detachBackSensorData(this.mFusedSensorData.mBackSensorDataObserver);
                }
            }
        }
    }

    private void enableFrontSensor() {
        if (!this.mFrontEnable) {
            this.mFrontEnable = true;
            this.mFrontWarmUpFlg = 0;
            this.mFrontEnableTime = SystemClock.elapsedRealtimeNanos();
            this.mFrontSensorData.setEnableTime(this.mFrontEnableTime);
            this.mSensorManager.registerListener(this.mFrontSensorEventListener, this.mFrontLightSensor, mFrontRateMillis * 1000, this.mHandler);
            if (DEBUG) {
                Slog.d(TAG, "Front sensor enabled.");
            }
        }
    }

    private void enableBackSensor() {
        if (!this.mBackEnable) {
            this.mBackEnable = true;
            this.mBackWarmUpFlg = 0;
            this.mBackEnableTime = SystemClock.elapsedRealtimeNanos();
            this.mBackSensorData.setEnableTime(this.mBackEnableTime);
            this.mSensorManager.registerListener(this.mBackSensorEventListener, this.mBackLightSensor, mBackRateMillis * 1000, this.mHandler);
            if (DEBUG) {
                Slog.d(TAG, "Back sensor enabled.");
            }
        }
    }

    private void enableFusedSensor() {
        if (!this.mFusedEnable) {
            this.mFusedEnable = true;
            this.mFusedWarmUpFlg = 0;
            this.mFusedEnableTime = SystemClock.elapsedRealtimeNanos();
            this.mHandler.sendEmptyMessage(3);
            if (DEBUG) {
                Slog.d(TAG, "Fused sensor enabled.");
            }
            if (this.mBackSensorBypassCount > 0) {
                this.mBackSensorBypassCount--;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mBackSensorBypassCount-- :");
                stringBuilder.append(this.mBackSensorBypassCount);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    private void disableFrontSensor() {
        if (this.mFrontEnable) {
            this.mFrontEnable = false;
            this.mSensorManager.unregisterListener(this.mFrontSensorEventListener);
            this.mHandler.removeMessages(1);
            this.mFrontSensorData.clearSensorData();
            if (DEBUG) {
                Slog.d(TAG, "Front sensor disabled.");
            }
        }
    }

    private void disableBackSensor() {
        if (this.mBackEnable) {
            this.mBackEnable = false;
            this.mSensorManager.unregisterListener(this.mBackSensorEventListener);
            this.mHandler.removeMessages(2);
            this.mBackSensorData.clearSensorData();
            if (DEBUG) {
                Slog.d(TAG, "Back sensor disabled.");
            }
        }
    }

    private void disableFusedSensor() {
        if (this.mFusedEnable) {
            this.mFusedEnable = false;
            this.mHandler.removeMessages(3);
            this.mFusedSensorData.clearSensorData();
            if (DEBUG) {
                Slog.d(TAG, "Fused sensor disabled.");
            }
        }
    }

    private int[] convertXYZ2LuxAndCct(Float Xraw, Float Yraw, Float Zraw, Float IRraw) {
        float Xnorm = ((PRODUCTION_CALIBRATION_X * 1600.128f) * Xraw.floatValue()) / (ATIME * AGAIN);
        float Ynorm = ((PRODUCTION_CALIBRATION_Y * 1600.128f) * Yraw.floatValue()) / (ATIME * AGAIN);
        float Znorm = ((PRODUCTION_CALIBRATION_Z * 1600.128f) * Zraw.floatValue()) / (ATIME * AGAIN);
        float IR1norm = ((1600.128f * PRODUCTION_CALIBRATION_IR1) * IRraw.floatValue()) / (ATIME * AGAIN);
        int lux = 0;
        int cct = 0;
        if (Float.compare(Ynorm, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
            float lux_f;
            float X;
            float Y;
            float Z;
            float f;
            if (IR1norm / Ynorm > IR_BOUNDRY) {
                lux_f = ((((LUX_COEF_HIGH_X * Xnorm) + (LUX_COEF_HIGH_Y * Ynorm)) + (LUX_COEF_HIGH_Z * Znorm)) + (LUX_COEF_HIGH_IR * IR1norm)) + LUX_COEF_HIGH_OFFSET;
                X = (((COLOR_MAT_HIGH_11 * Xnorm) + (COLOR_MAT_HIGH_12 * Ynorm)) + (COLOR_MAT_HIGH_13 * Znorm)) + (COLOR_MAT_HIGH_14 * IR1norm);
                Y = (((COLOR_MAT_HIGH_21 * Xnorm) + (COLOR_MAT_HIGH_22 * Ynorm)) + (COLOR_MAT_HIGH_23 * Znorm)) + (COLOR_MAT_HIGH_24 * IR1norm);
                Z = (((COLOR_MAT_HIGH_31 * Xnorm) + (COLOR_MAT_HIGH_32 * Ynorm)) + (COLOR_MAT_HIGH_33 * Znorm)) + (COLOR_MAT_HIGH_34 * IR1norm);
            } else {
                lux_f = ((((LUX_COEF_LOW_X * Xnorm) + (LUX_COEF_LOW_Y * Ynorm)) + (LUX_COEF_LOW_Z * Znorm)) + (LUX_COEF_LOW_IR * IR1norm)) + LUX_COEF_LOW_OFFSET;
                X = (((COLOR_MAT_LOW_11 * Xnorm) + (COLOR_MAT_LOW_12 * Ynorm)) + (COLOR_MAT_LOW_13 * Znorm)) + (COLOR_MAT_LOW_14 * IR1norm);
                Y = (((COLOR_MAT_LOW_21 * Xnorm) + (COLOR_MAT_LOW_22 * Ynorm)) + (COLOR_MAT_LOW_23 * Znorm)) + (COLOR_MAT_LOW_24 * IR1norm);
                Z = (((COLOR_MAT_LOW_31 * Xnorm) + (COLOR_MAT_LOW_32 * Ynorm)) + (COLOR_MAT_LOW_33 * Znorm)) + (COLOR_MAT_LOW_34 * IR1norm);
            }
            float XYZ = (X + Y) + Z;
            if (Float.compare(XYZ, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
                float x = X / XYZ;
                float y = Y / XYZ;
                if (Float.compare(y - 0.1858f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
                    float tempvalue = (1.0f / (y - 0.1858f)) * (x - 0.332f);
                    f = 0.5f;
                    cct = (int) (((((((-499.0f * tempvalue) * tempvalue) * tempvalue) + ((3525.0f * tempvalue) * tempvalue)) - (6823.3f * tempvalue)) + 5520.33f) + 0.5f);
                    lux = (int) (lux_f + f);
                }
            }
            f = 0.5f;
            lux = (int) (lux_f + f);
        }
        return new int[]{lux, cct};
    }

    private int[] convertRGB2LuxAndCct(float red, float green, float blue, float IR) {
        float f = green;
        float cct_f = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (Float.compare(f, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) <= 0) {
            return new int[]{0, 0};
        }
        float lux_f;
        if (Float.compare(IR / f, 0.5f) > 0) {
            lux_f = ((LUX_COEF_HIGH_RED * red) + (LUX_COEF_HIGH_GREEN * f)) + (LUX_COEF_HIGH_BLUE * blue);
        } else {
            lux_f = ((LUX_COEF_LOW_RED * red) + (LUX_COEF_LOW_GREEN * f)) + (LUX_COEF_LOW_BLUE * blue);
        }
        float rgbSum = (red + f) + blue;
        if (Float.compare(rgbSum, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) <= 0) {
            if (DEBUG) {
                Slog.d(TAG, "convertRGB2LuxAndCct: Illegal sensor value, rgbSum <= 0!");
            }
            return new int[]{0, 0};
        }
        float lux_f2;
        float redRatio = red / rgbSum;
        float greenRatio = f / rgbSum;
        float x = ((((X_CONSTANT + (X_R_COEF * redRatio)) + ((X_R_QUADRATIC_COEF * redRatio) * redRatio)) + (X_G_COEF * greenRatio)) + ((X_RG_COEF * greenRatio) * redRatio)) + ((X_G_QUADRATIC_COEF * greenRatio) * greenRatio);
        float y = ((((Y_CONSTANT + (Y_R_COEF * redRatio)) + ((Y_R_QUADRATIC_COEF * redRatio) * redRatio)) + (Y_G_COEF * greenRatio)) + ((Y_RG_COEF * redRatio) * greenRatio)) + ((Y_G_QUADRATIC_COEF * greenRatio) * greenRatio);
        if (Float.compare(y, 0.1858f) >= 0) {
            float n = (x - 0.332f) / (y - 0.1858f);
            lux_f2 = lux_f;
            float cct_f2 = (((-449.0f * ((float) Math.pow((double) n, 3.0d))) + (3525.0f * ((float) Math.pow((double) n, 2.0d)))) - (6823.3f * n)) + 5520.33f;
            cct_f = 13000.0f;
            if (Float.compare(cct_f2, 13000.0f) < 0) {
                cct_f = cct_f2;
            }
            float cct_f3 = cct_f;
            cct_f = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            if (Float.compare(cct_f3, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) > 0) {
                cct_f = cct_f3;
            }
        } else {
            lux_f2 = lux_f;
            float f2 = redRatio;
        }
        return new int[]{(int) (lux_f2 + 0.5f), (int) (cct_f + 0.5f)};
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x006f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseParameters() {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        boolean parseResult = false;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(DUAL_SENSOR_XML_PATH);
            parseResult = parseParametersFromXML(inputStream);
            try {
                inputStream.close();
            } catch (IOException e2) {
                e = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (FileNotFoundException e3) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    e = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e5) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    e = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e7) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("input stream close uncorrectly: ");
                    stringBuilder.append(e7);
                    Slog.i(TAG, stringBuilder.toString());
                }
            }
        }
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("parseResult=");
            stringBuilder2.append(parseResult);
            Slog.d(str2, stringBuilder2.toString());
        }
        printParameters();
        return parseResult;
        stringBuilder.append("input stream close uncorrectly: ");
        stringBuilder.append(e);
        Slog.i(str, stringBuilder.toString());
        if (DEBUG) {
        }
        printParameters();
        return parseResult;
    }

    private boolean parseParametersFromXML(InputStream inStream) {
        String str;
        StringBuilder stringBuilder;
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("parse dual sensor parameters from xml: ");
            stringBuilder2.append(DUAL_SENSOR_XML_PATH);
            Slog.d(str2, stringBuilder2.toString());
        }
        boolean backSensorParamLoadStarted = false;
        boolean moduleSensorOptionsLoadStarted = false;
        boolean algorithmParametersLoadStarted = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                String nodeName;
                switch (eventType) {
                    case 2:
                        nodeName = parser.getName();
                        if (!nodeName.equals("BackSensorParameters")) {
                            if (!backSensorParamLoadStarted || !nodeName.equals("IRBoundry")) {
                                if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefficientsLow")) {
                                    if (!backSensorParamLoadStarted || !nodeName.equals("ColorMatrixLow")) {
                                        if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefficientsHigh")) {
                                            if (!backSensorParamLoadStarted || !nodeName.equals("ColorMatrixHigh")) {
                                                if (!backSensorParamLoadStarted || !nodeName.equals("ATime")) {
                                                    if (!backSensorParamLoadStarted || !nodeName.equals("AGain")) {
                                                        if (!backSensorParamLoadStarted || !nodeName.equals("ProductionCalibrationX")) {
                                                            if (!backSensorParamLoadStarted || !nodeName.equals("ProductionCalibrationY")) {
                                                                if (!backSensorParamLoadStarted || !nodeName.equals("ProductionCalibrationZ")) {
                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("ProductionCalibrationIR1")) {
                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("FrontSensorRateMillis")) {
                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("BackSensorRateMillis")) {
                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("FusedSensorRateMillis")) {
                                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("SensorVersion")) {
                                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("RatioIRGreen")) {
                                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefHighRed")) {
                                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefHighGreen")) {
                                                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefHighBlue")) {
                                                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefLowRed")) {
                                                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefLowGreen")) {
                                                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("LuxCoefLowBlue")) {
                                                                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("XConstant")) {
                                                                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("XRCoef")) {
                                                                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("XRQuadCoef")) {
                                                                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("XGCoef")) {
                                                                                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("XRGCoef")) {
                                                                                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("XGQuadCoef")) {
                                                                                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("YConstant")) {
                                                                                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("YRCoef")) {
                                                                                                                                                    if (!backSensorParamLoadStarted || !nodeName.equals("YRQuadCoef")) {
                                                                                                                                                        if (!backSensorParamLoadStarted || !nodeName.equals("YGCoef")) {
                                                                                                                                                            if (!backSensorParamLoadStarted || !nodeName.equals("YRGCoef")) {
                                                                                                                                                                if (!backSensorParamLoadStarted || !nodeName.equals("YGQuadCoef")) {
                                                                                                                                                                    if (!nodeName.equals("ModuleSensorOptions")) {
                                                                                                                                                                        if (!moduleSensorOptionsLoadStarted) {
                                                                                                                                                                            if (!nodeName.equals("AlgorithmParameters")) {
                                                                                                                                                                                if (!algorithmParametersLoadStarted || !nodeName.equals("FrontSensorStabilityThresholdList")) {
                                                                                                                                                                                    if (!algorithmParametersLoadStarted || !nodeName.equals("BackSensorStabilityThresholdList")) {
                                                                                                                                                                                        if (!algorithmParametersLoadStarted || !nodeName.equals("StabilizedProbabilityLUT")) {
                                                                                                                                                                                            if (!algorithmParametersLoadStarted || !nodeName.equals("BackRoofThresh")) {
                                                                                                                                                                                                if (!algorithmParametersLoadStarted || !nodeName.equals("BackFloorThresh")) {
                                                                                                                                                                                                    if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomThresh")) {
                                                                                                                                                                                                        if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomDelta")) {
                                                                                                                                                                                                            if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomDelta1")) {
                                                                                                                                                                                                                if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomDelta2")) {
                                                                                                                                                                                                                    if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomThresh1")) {
                                                                                                                                                                                                                        if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomThresh2")) {
                                                                                                                                                                                                                            if (!algorithmParametersLoadStarted || !nodeName.equals("DarkRoomThresh3")) {
                                                                                                                                                                                                                                if (!algorithmParametersLoadStarted || !nodeName.equals("BackLuxDeviationThresh")) {
                                                                                                                                                                                                                                    if (!algorithmParametersLoadStarted || !nodeName.equals("FlashlightDetectionMode")) {
                                                                                                                                                                                                                                        if (!algorithmParametersLoadStarted || !nodeName.equals("FlashlightOffTimeThresholdMs")) {
                                                                                                                                                                                                                                            if (!algorithmParametersLoadStarted || !nodeName.equals("BackSensorTimeOutTH")) {
                                                                                                                                                                                                                                                if (algorithmParametersLoadStarted && nodeName.equals("BackSensorBypassCountMax")) {
                                                                                                                                                                                                                                                    this.mBackSensorBypassCountMax = Integer.parseInt(parser.nextText());
                                                                                                                                                                                                                                                    break;
                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                            this.mBackSensorTimeOutTH = Integer.parseInt(parser.nextText());
                                                                                                                                                                                                                                            break;
                                                                                                                                                                                                                                        }
                                                                                                                                                                                                                                        FLASHLIGHT_OFF_TIME_THRESHOLD_MS = Integer.parseInt(parser.nextText());
                                                                                                                                                                                                                                        break;
                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                    mFlashlightDetectionMode = Integer.parseInt(parser.nextText());
                                                                                                                                                                                                                                    break;
                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                BACK_LUX_DEVIATION_THRESH = Long.parseLong(parser.nextText());
                                                                                                                                                                                                                                break;
                                                                                                                                                                                                                            }
                                                                                                                                                                                                                            DARK_ROOM_THRESH3 = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                                            break;
                                                                                                                                                                                                                        }
                                                                                                                                                                                                                        DARK_ROOM_THRESH2 = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                                        break;
                                                                                                                                                                                                                    }
                                                                                                                                                                                                                    DARK_ROOM_THRESH1 = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                                    break;
                                                                                                                                                                                                                }
                                                                                                                                                                                                                DARK_ROOM_DELTA2 = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                                break;
                                                                                                                                                                                                            }
                                                                                                                                                                                                            DARK_ROOM_DELTA1 = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                            break;
                                                                                                                                                                                                        }
                                                                                                                                                                                                        DARK_ROOM_DELTA = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                        break;
                                                                                                                                                                                                    }
                                                                                                                                                                                                    DARK_ROOM_THRESH = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                    break;
                                                                                                                                                                                                }
                                                                                                                                                                                                BACK_FLOOR_THRESH = Float.parseFloat(parser.nextText());
                                                                                                                                                                                                break;
                                                                                                                                                                                            }
                                                                                                                                                                                            BACK_ROOF_THRESH = Float.parseFloat(parser.nextText());
                                                                                                                                                                                            break;
                                                                                                                                                                                        }
                                                                                                                                                                                        parseStabilizedProbabilityLUT(parser.nextText());
                                                                                                                                                                                        break;
                                                                                                                                                                                    }
                                                                                                                                                                                    parseSensorStabilityThresholdList(parser.nextText(), false);
                                                                                                                                                                                    break;
                                                                                                                                                                                }
                                                                                                                                                                                parseSensorStabilityThresholdList(parser.nextText(), true);
                                                                                                                                                                                break;
                                                                                                                                                                            }
                                                                                                                                                                            algorithmParametersLoadStarted = true;
                                                                                                                                                                            break;
                                                                                                                                                                        }
                                                                                                                                                                        mModuleSensorMap.put(nodeName, Integer.valueOf(Integer.parseInt(parser.nextText())));
                                                                                                                                                                        break;
                                                                                                                                                                    }
                                                                                                                                                                    moduleSensorOptionsLoadStarted = true;
                                                                                                                                                                    break;
                                                                                                                                                                }
                                                                                                                                                                Y_G_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                                                break;
                                                                                                                                                            }
                                                                                                                                                            Y_RG_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                                            break;
                                                                                                                                                        }
                                                                                                                                                        Y_G_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                                        break;
                                                                                                                                                    }
                                                                                                                                                    Y_R_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                                    break;
                                                                                                                                                }
                                                                                                                                                Y_R_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                                break;
                                                                                                                                            }
                                                                                                                                            Y_CONSTANT = Float.parseFloat(parser.nextText());
                                                                                                                                            break;
                                                                                                                                        }
                                                                                                                                        X_G_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                        break;
                                                                                                                                    }
                                                                                                                                    X_RG_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                    break;
                                                                                                                                }
                                                                                                                                X_G_COEF = Float.parseFloat(parser.nextText());
                                                                                                                                break;
                                                                                                                            }
                                                                                                                            X_R_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                                                                                                                            break;
                                                                                                                        }
                                                                                                                        X_R_COEF = Float.parseFloat(parser.nextText());
                                                                                                                        break;
                                                                                                                    }
                                                                                                                    X_CONSTANT = Float.parseFloat(parser.nextText());
                                                                                                                    break;
                                                                                                                }
                                                                                                                LUX_COEF_LOW_BLUE = Float.parseFloat(parser.nextText());
                                                                                                                break;
                                                                                                            }
                                                                                                            LUX_COEF_LOW_GREEN = Float.parseFloat(parser.nextText());
                                                                                                            break;
                                                                                                        }
                                                                                                        LUX_COEF_LOW_RED = Float.parseFloat(parser.nextText());
                                                                                                        break;
                                                                                                    }
                                                                                                    LUX_COEF_HIGH_BLUE = Float.parseFloat(parser.nextText());
                                                                                                    break;
                                                                                                }
                                                                                                LUX_COEF_HIGH_GREEN = Float.parseFloat(parser.nextText());
                                                                                                break;
                                                                                            }
                                                                                            LUX_COEF_HIGH_RED = Float.parseFloat(parser.nextText());
                                                                                            break;
                                                                                        }
                                                                                        RATIO_IR_GREEN = Float.parseFloat(parser.nextText());
                                                                                        break;
                                                                                    }
                                                                                    VERSION_SENSOR = Integer.parseInt(parser.nextText());
                                                                                    break;
                                                                                }
                                                                                mFusedRateMillis = Integer.parseInt(parser.nextText());
                                                                                break;
                                                                            }
                                                                            mBackRateMillis = Integer.parseInt(parser.nextText());
                                                                            break;
                                                                        }
                                                                        mFrontRateMillis = Integer.parseInt(parser.nextText());
                                                                        break;
                                                                    }
                                                                    PRODUCTION_CALIBRATION_IR1 = Float.parseFloat(parser.nextText());
                                                                    break;
                                                                }
                                                                PRODUCTION_CALIBRATION_Z = Float.parseFloat(parser.nextText());
                                                                break;
                                                            }
                                                            PRODUCTION_CALIBRATION_Y = Float.parseFloat(parser.nextText());
                                                            break;
                                                        }
                                                        PRODUCTION_CALIBRATION_X = Float.parseFloat(parser.nextText());
                                                        break;
                                                    }
                                                    AGAIN = Float.parseFloat(parser.nextText());
                                                    break;
                                                }
                                                ATIME = Float.parseFloat(parser.nextText());
                                                break;
                                            }
                                            parseColorMatrix(parser.nextText(), false);
                                            break;
                                        }
                                        parseLuxCoefficients(parser.nextText(), false);
                                        break;
                                    }
                                    parseColorMatrix(parser.nextText(), true);
                                    break;
                                }
                                parseLuxCoefficients(parser.nextText(), true);
                                break;
                            }
                            IR_BOUNDRY = Float.parseFloat(parser.nextText());
                            break;
                        }
                        backSensorParamLoadStarted = true;
                        break;
                        break;
                    case 3:
                        nodeName = parser.getName();
                        if (!nodeName.equals("BackSensorParameters")) {
                            if (!nodeName.equals("ModuleSensorOptions")) {
                                if (nodeName.equals("AlgorithmParameters")) {
                                    algorithmParametersLoadStarted = false;
                                    break;
                                }
                            }
                            moduleSensorOptionsLoadStarted = false;
                            break;
                        }
                        backSensorParamLoadStarted = false;
                        break;
                        break;
                    default:
                        break;
                }
            }
            return true;
        } catch (XmlPullParserException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("XmlPullParserException: ");
            stringBuilder.append(e);
            Slog.i(str, stringBuilder.toString());
            return false;
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IOException: ");
            stringBuilder.append(e2);
            Slog.i(str, stringBuilder.toString());
            return false;
        } catch (NumberFormatException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException: ");
            stringBuilder.append(e3);
            Slog.i(str, stringBuilder.toString());
            return false;
        } catch (RuntimeException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("RuntimeException: ");
            stringBuilder.append(e4);
            Slog.i(str, stringBuilder.toString());
            return false;
        } catch (Exception e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception: ");
            stringBuilder.append(e5);
            Slog.i(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean parseLuxCoefficients(String nodeContent, boolean isLow) {
        if (nodeContent == null) {
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 5);
        if (isLow) {
            LUX_COEF_LOW_X = Float.parseFloat(splitedContent[0]);
            LUX_COEF_LOW_Y = Float.parseFloat(splitedContent[1]);
            LUX_COEF_LOW_Z = Float.parseFloat(splitedContent[2]);
            LUX_COEF_LOW_IR = Float.parseFloat(splitedContent[3]);
            LUX_COEF_LOW_OFFSET = Float.parseFloat(splitedContent[4]);
        } else {
            LUX_COEF_HIGH_X = Float.parseFloat(splitedContent[0]);
            LUX_COEF_HIGH_Y = Float.parseFloat(splitedContent[1]);
            LUX_COEF_HIGH_Z = Float.parseFloat(splitedContent[2]);
            LUX_COEF_HIGH_IR = Float.parseFloat(splitedContent[3]);
            LUX_COEF_HIGH_OFFSET = Float.parseFloat(splitedContent[4]);
        }
        return true;
    }

    private boolean parseColorMatrix(String nodeContent, boolean isLow) {
        if (nodeContent == null) {
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 12);
        if (isLow) {
            COLOR_MAT_LOW_11 = Float.parseFloat(splitedContent[0]);
            COLOR_MAT_LOW_12 = Float.parseFloat(splitedContent[1]);
            COLOR_MAT_LOW_13 = Float.parseFloat(splitedContent[2]);
            COLOR_MAT_LOW_14 = Float.parseFloat(splitedContent[3]);
            COLOR_MAT_LOW_21 = Float.parseFloat(splitedContent[4]);
            COLOR_MAT_LOW_22 = Float.parseFloat(splitedContent[5]);
            COLOR_MAT_LOW_23 = Float.parseFloat(splitedContent[6]);
            COLOR_MAT_LOW_24 = Float.parseFloat(splitedContent[7]);
            COLOR_MAT_LOW_31 = Float.parseFloat(splitedContent[8]);
            COLOR_MAT_LOW_32 = Float.parseFloat(splitedContent[9]);
            COLOR_MAT_LOW_33 = Float.parseFloat(splitedContent[10]);
            COLOR_MAT_LOW_34 = Float.parseFloat(splitedContent[11]);
        } else {
            COLOR_MAT_HIGH_11 = Float.parseFloat(splitedContent[0]);
            COLOR_MAT_HIGH_12 = Float.parseFloat(splitedContent[1]);
            COLOR_MAT_HIGH_13 = Float.parseFloat(splitedContent[2]);
            COLOR_MAT_HIGH_14 = Float.parseFloat(splitedContent[3]);
            COLOR_MAT_HIGH_21 = Float.parseFloat(splitedContent[4]);
            COLOR_MAT_HIGH_22 = Float.parseFloat(splitedContent[5]);
            COLOR_MAT_HIGH_23 = Float.parseFloat(splitedContent[6]);
            COLOR_MAT_HIGH_24 = Float.parseFloat(splitedContent[7]);
            COLOR_MAT_HIGH_31 = Float.parseFloat(splitedContent[8]);
            COLOR_MAT_HIGH_32 = Float.parseFloat(splitedContent[9]);
            COLOR_MAT_HIGH_33 = Float.parseFloat(splitedContent[10]);
            COLOR_MAT_HIGH_34 = Float.parseFloat(splitedContent[11]);
        }
        return true;
    }

    public int getModuleSensorOption(String moduleName) {
        String str;
        StringBuilder stringBuilder;
        if (mModuleSensorMap == null || !mModuleSensorMap.containsKey(moduleName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(moduleName);
            stringBuilder.append(" getModuleSensorOption=");
            stringBuilder.append(-1);
            Slog.i(str, stringBuilder.toString());
            return -1;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append(moduleName);
        stringBuilder.append(" getModuleSensorOption=");
        stringBuilder.append(mModuleSensorMap.get(moduleName));
        Slog.i(str, stringBuilder.toString());
        return ((Integer) mModuleSensorMap.get(moduleName)).intValue();
    }

    private boolean parseSensorStabilityThresholdList(String nodeContent, boolean isFrontSensor) {
        int index = 0;
        if (nodeContent == null) {
            Slog.i(TAG, "parseSensorStabilityThresholdList nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 4);
        int[] parsedArray = new int[4];
        while (index < 4) {
            parsedArray[index] = Integer.parseInt(splitedContent[index]);
            index++;
        }
        if (isFrontSensor) {
            FRONT_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
        } else {
            BACK_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
        }
        return true;
    }

    private boolean parseStabilizedProbabilityLUT(String nodeContent) {
        int index = 0;
        if (nodeContent == null) {
            Slog.d(TAG, "parseStabilizedProbabilityLUT nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 25);
        int[] parsedArray = new int[25];
        while (index < 25) {
            parsedArray[index] = Integer.parseInt(splitedContent[index]);
            index++;
        }
        STABILIZED_PROBABILITY_LUT = Arrays.copyOf(parsedArray, parsedArray.length);
        return true;
    }

    private void printParameters() {
        if (DEBUG) {
            String str;
            StringBuilder stringBuilder;
            if (VERSION_SENSOR == 1) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("VERSION_SENSOR=");
                stringBuilder.append(VERSION_SENSOR);
                stringBuilder.append(" IR_BOUNDRY=");
                stringBuilder.append(IR_BOUNDRY);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LUX_COEF_LOW_X=");
                stringBuilder.append(LUX_COEF_LOW_X);
                stringBuilder.append(" LUX_COEF_LOW_Y=");
                stringBuilder.append(LUX_COEF_LOW_Y);
                stringBuilder.append(" LUX_COEF_LOW_Z=");
                stringBuilder.append(LUX_COEF_LOW_Z);
                stringBuilder.append(" LUX_COEF_LOW_IR=");
                stringBuilder.append(LUX_COEF_LOW_IR);
                stringBuilder.append(" LUX_COEF_LOW_OFFSET=");
                stringBuilder.append(LUX_COEF_LOW_OFFSET);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LUX_COEF_HIGH_X=");
                stringBuilder.append(LUX_COEF_HIGH_X);
                stringBuilder.append(" LUX_COEF_HIGH_Y=");
                stringBuilder.append(LUX_COEF_HIGH_Y);
                stringBuilder.append(" LUX_COEF_HIGH_Z=");
                stringBuilder.append(LUX_COEF_HIGH_Z);
                stringBuilder.append(" LUX_COEF_HIGH_IR=");
                stringBuilder.append(LUX_COEF_HIGH_IR);
                stringBuilder.append(" LUX_COEF_HIGH_OFFSET=");
                stringBuilder.append(LUX_COEF_HIGH_OFFSET);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("COLOR_MAT_LOW_11=");
                stringBuilder.append(COLOR_MAT_LOW_11);
                stringBuilder.append(" COLOR_MAT_LOW_12=");
                stringBuilder.append(COLOR_MAT_LOW_12);
                stringBuilder.append(" COLOR_MAT_LOW_13=");
                stringBuilder.append(COLOR_MAT_LOW_13);
                stringBuilder.append(" COLOR_MAT_LOW_14=");
                stringBuilder.append(COLOR_MAT_LOW_14);
                stringBuilder.append(" COLOR_MAT_LOW_21=");
                stringBuilder.append(COLOR_MAT_LOW_21);
                stringBuilder.append(" COLOR_MAT_LOW_22=");
                stringBuilder.append(COLOR_MAT_LOW_22);
                stringBuilder.append(" COLOR_MAT_LOW_23=");
                stringBuilder.append(COLOR_MAT_LOW_23);
                stringBuilder.append(" COLOR_MAT_LOW_24=");
                stringBuilder.append(COLOR_MAT_LOW_24);
                stringBuilder.append(" COLOR_MAT_LOW_31=");
                stringBuilder.append(COLOR_MAT_LOW_31);
                stringBuilder.append(" COLOR_MAT_LOW_32=");
                stringBuilder.append(COLOR_MAT_LOW_32);
                stringBuilder.append(" COLOR_MAT_LOW_33=");
                stringBuilder.append(COLOR_MAT_LOW_33);
                stringBuilder.append(" COLOR_MAT_LOW_34=");
                stringBuilder.append(COLOR_MAT_LOW_34);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("COLOR_MAT_HIGH_11=");
                stringBuilder.append(COLOR_MAT_HIGH_11);
                stringBuilder.append(" COLOR_MAT_HIGH_12=");
                stringBuilder.append(COLOR_MAT_HIGH_12);
                stringBuilder.append(" COLOR_MAT_HIGH_13=");
                stringBuilder.append(COLOR_MAT_HIGH_13);
                stringBuilder.append(" COLOR_MAT_HIGH_14=");
                stringBuilder.append(COLOR_MAT_HIGH_14);
                stringBuilder.append(" COLOR_MAT_HIGH_21=");
                stringBuilder.append(COLOR_MAT_HIGH_21);
                stringBuilder.append(" COLOR_MAT_HIGH_22=");
                stringBuilder.append(COLOR_MAT_HIGH_22);
                stringBuilder.append(" COLOR_MAT_HIGH_23=");
                stringBuilder.append(COLOR_MAT_HIGH_23);
                stringBuilder.append(" COLOR_MAT_HIGH_24=");
                stringBuilder.append(COLOR_MAT_HIGH_24);
                stringBuilder.append(" COLOR_MAT_HIGH_31=");
                stringBuilder.append(COLOR_MAT_HIGH_31);
                stringBuilder.append(" COLOR_MAT_HIGH_32=");
                stringBuilder.append(COLOR_MAT_HIGH_32);
                stringBuilder.append(" COLOR_MAT_HIGH_33=");
                stringBuilder.append(COLOR_MAT_HIGH_33);
                stringBuilder.append(" COLOR_MAT_HIGH_34=");
                stringBuilder.append(COLOR_MAT_HIGH_34);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ATime=");
                stringBuilder.append(ATIME);
                stringBuilder.append(" AGain=");
                stringBuilder.append(AGAIN);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ProductionCalibration:");
                stringBuilder.append(PRODUCTION_CALIBRATION_X);
                stringBuilder.append(" ");
                stringBuilder.append(PRODUCTION_CALIBRATION_Y);
                stringBuilder.append(" ");
                stringBuilder.append(PRODUCTION_CALIBRATION_Z);
                stringBuilder.append(" ");
                stringBuilder.append(PRODUCTION_CALIBRATION_IR1);
                Slog.d(str, stringBuilder.toString());
            } else if (VERSION_SENSOR == 2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("VERSION_SENSOR=");
                stringBuilder.append(VERSION_SENSOR);
                stringBuilder.append(" RATIO_IR_GREEN=");
                stringBuilder.append(RATIO_IR_GREEN);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LUX_COEF_HIGH_RED=");
                stringBuilder.append(LUX_COEF_HIGH_RED);
                stringBuilder.append(" LUX_COEF_HIGH_GREEN=");
                stringBuilder.append(LUX_COEF_HIGH_GREEN);
                stringBuilder.append(" LUX_COEF_HIGH_BLUE=");
                stringBuilder.append(LUX_COEF_HIGH_BLUE);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LUX_COEF_LOW_RED=");
                stringBuilder.append(LUX_COEF_LOW_RED);
                stringBuilder.append(" LUX_COEF_LOW_GREEN=");
                stringBuilder.append(LUX_COEF_LOW_GREEN);
                stringBuilder.append(" LUX_COEF_LOW_BLUE=");
                stringBuilder.append(LUX_COEF_LOW_BLUE);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("X_CONSTANT=");
                stringBuilder.append(X_CONSTANT);
                stringBuilder.append(" X_R_COEF=");
                stringBuilder.append(X_R_COEF);
                stringBuilder.append(" X_R_QUADRATIC_COEF=");
                stringBuilder.append(X_R_QUADRATIC_COEF);
                stringBuilder.append(" X_G_COEF=");
                stringBuilder.append(X_G_COEF);
                stringBuilder.append(" X_RG_COEF=");
                stringBuilder.append(X_RG_COEF);
                stringBuilder.append(" X_G_QUADRATIC_COEF=");
                stringBuilder.append(X_G_QUADRATIC_COEF);
                Slog.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Y_CONSTANT=");
                stringBuilder.append(Y_CONSTANT);
                stringBuilder.append(" Y_R_COEF=");
                stringBuilder.append(Y_R_COEF);
                stringBuilder.append(" Y_R_QUADRATIC_COEF=");
                stringBuilder.append(Y_R_QUADRATIC_COEF);
                stringBuilder.append(" Y_G_COEF=");
                stringBuilder.append(Y_G_COEF);
                stringBuilder.append(" Y_RG_COEF=");
                stringBuilder.append(Y_RG_COEF);
                stringBuilder.append(" Y_G_QUADRATIC_COEF=");
                stringBuilder.append(Y_G_QUADRATIC_COEF);
                Slog.d(str, stringBuilder.toString());
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFrontRateMillis=");
            stringBuilder.append(mFrontRateMillis);
            stringBuilder.append(" mBackRateMillis=");
            stringBuilder.append(mBackRateMillis);
            stringBuilder.append(" mFusedRateMillis=");
            stringBuilder.append(mFusedRateMillis);
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("FRONT_SENSOR_STABILITY_THRESHOLD:");
            stringBuilder.append(Arrays.toString(FRONT_SENSOR_STABILITY_THRESHOLD));
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BACK_SENSOR_STABILITY_THRESHOLD:");
            stringBuilder.append(Arrays.toString(BACK_SENSOR_STABILITY_THRESHOLD));
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("STABILIZED_PROBABILITY_LUT:");
            stringBuilder.append(Arrays.toString(STABILIZED_PROBABILITY_LUT));
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BACK_ROOF_THRESH=");
            stringBuilder.append(BACK_ROOF_THRESH);
            stringBuilder.append(" BACK_FLOOR_THRESH=");
            stringBuilder.append(BACK_FLOOR_THRESH);
            stringBuilder.append(" DARK_ROOM_THRESH=");
            stringBuilder.append(DARK_ROOM_THRESH);
            stringBuilder.append(" DARK_ROOM_DELTA=");
            stringBuilder.append(DARK_ROOM_DELTA);
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("DARK_ROOM_THRESH1=");
            stringBuilder.append(DARK_ROOM_THRESH1);
            stringBuilder.append(" DARK_ROOM_THRESH2=");
            stringBuilder.append(DARK_ROOM_THRESH2);
            stringBuilder.append(" DARK_ROOM_THRESH3=");
            stringBuilder.append(DARK_ROOM_THRESH3);
            stringBuilder.append(" DARK_ROOM_DELTA1=");
            stringBuilder.append(DARK_ROOM_DELTA1);
            stringBuilder.append(" DARK_ROOM_DELTA2=");
            stringBuilder.append(DARK_ROOM_DELTA2);
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mFlashlightDetectionMode=");
            stringBuilder.append(mFlashlightDetectionMode);
            stringBuilder.append("FLASHLIGHT_OFF_TIME_THRESHOLD_MS=");
            stringBuilder.append(FLASHLIGHT_OFF_TIME_THRESHOLD_MS);
            Slog.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mBackSensorTimeOutTH=");
            stringBuilder.append(this.mBackSensorTimeOutTH);
            stringBuilder.append("mBackSensorBypassCountMax=");
            stringBuilder.append(this.mBackSensorBypassCountMax);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void registerBackCameraTorchCallback() {
        try {
            this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
            if (this.mCameraManager == null) {
                Slog.w(TAG, "mCameraManager is null");
                mFlashlightDetectionMode = 0;
                return;
            }
            Slog.i(TAG, "mCameraManager successfully obtained");
            for (String id : this.mCameraManager.getCameraIdList()) {
                CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                    this.mBackCameraId = id;
                }
            }
            if (this.mBackCameraId != null) {
                try {
                    this.mCameraManager.registerTorchCallback(this.mTorchCallback, null);
                    return;
                } catch (IllegalArgumentException e) {
                    mFlashlightDetectionMode = 0;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IllegalArgumentException ");
                    stringBuilder.append(e);
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
            }
            Slog.w(TAG, "mBackCameraId is null");
            mFlashlightDetectionMode = 0;
        } catch (CameraAccessException e2) {
            mFlashlightDetectionMode = 0;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CameraAccessException ");
            stringBuilder2.append(e2);
            Slog.w(str2, stringBuilder2.toString());
        }
    }
}

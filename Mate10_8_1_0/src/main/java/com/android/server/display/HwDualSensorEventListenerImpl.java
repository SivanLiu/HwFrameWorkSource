package com.android.server.display;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
    private static final int FRONT_MSG_TIMER = 1;
    private static int[] FRONT_SENSOR_STABILITY_THRESHOLD = new int[]{-30, -4, 4, 30};
    private static final int FUSED_MSG_TIMER = 3;
    private static float IR_BOUNDRY = 1.0f;
    public static final int LIGHT_SENSOR_BACK = 1;
    public static final int LIGHT_SENSOR_DEFAULT = -1;
    public static final int LIGHT_SENSOR_DUAL = 2;
    public static final int LIGHT_SENSOR_FRONT = 0;
    private static float LUX_COEF_HIGH_IR = 0.5164043f;
    private static float LUX_COEF_HIGH_OFFSET = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private static float LUX_COEF_HIGH_X = -11.499684f;
    private static float LUX_COEF_HIGH_Y = 16.098469f;
    private static float LUX_COEF_HIGH_Z = -3.7601638f;
    private static float LUX_COEF_LOW_IR = -3.1549554f;
    private static float LUX_COEF_LOW_OFFSET = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private static float LUX_COEF_LOW_X = -3.0825195f;
    private static float LUX_COEF_LOW_Y = 7.662419f;
    private static float LUX_COEF_LOW_Z = -6.400353f;
    private static final int MSG_TIMER = 0;
    private static float PRODUCTION_CALIBRATION_IR1 = 1.0f;
    private static float PRODUCTION_CALIBRATION_X = 1.0f;
    private static float PRODUCTION_CALIBRATION_Y = 1.0f;
    private static float PRODUCTION_CALIBRATION_Z = 1.0f;
    private static int[] STABILIZED_PROBABILITY_LUT = new int[25];
    private static final String TAG = "HwDualSensorEventListenerImpl";
    private static final int TIMER_DISABLE = -1;
    private static final int WARM_UP_FLAG = 2;
    private static int mBackRateMillis = 300;
    private static final long mDebugPrintInterval = 4000;
    private static int mFrontRateMillis = 300;
    private static int mFusedRateMillis = 300;
    private static volatile HwDualSensorEventListenerImpl mInstance;
    private static Hashtable<String, Integer> mModuleSensorMap = new Hashtable();
    private boolean mBackEnable = false;
    private long mBackEnableTime = -1;
    private Sensor mBackLightSensor;
    private SensorDataSender mBackSender;
    private SensorData mBackSensorData;
    private final SensorEventListener mBackSensorEventListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (HwDualSensorEventListenerImpl.this.mBackEnable) {
                long sensorClockTimeStamp = event.timestamp;
                long systemClockTimeStamp = SystemClock.uptimeMillis();
                int[] arr = HwDualSensorEventListenerImpl.this.convertXYZ2LuxAndCct(Float.valueOf(event.values[0]), Float.valueOf(event.values[1]), Float.valueOf(event.values[2]), Float.valueOf(event.values[3]));
                int lux = arr[0];
                int cct = arr[1];
                if (HwDualSensorEventListenerImpl.DEBUG && systemClockTimeStamp - (HwDualSensorEventListenerImpl.this.mBackEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp + " Xraw=" + event.values[0] + " Yraw=" + event.values[1] + " Zraw=" + event.values[2]);
                }
                if (HwDualSensorEventListenerImpl.this.mBackWarmUpFlg >= 2) {
                    HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                } else if (sensorClockTimeStamp < HwDualSensorEventListenerImpl.this.mBackEnableTime) {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): Back sensor not ready yet!");
                    }
                    return;
                } else {
                    HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                    HwDualSensorEventListenerImpl.this.mBackWarmUpFlg = HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + 1;
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(2);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(2);
                }
                return;
            }
            Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): mBackEnable=false");
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mBackSensorLock = new Object();
    private int mBackTimer = -1;
    private int mBackWarmUpFlg = 0;
    private final HandlerThread mDualSensorProcessThread;
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
                if (HwDualSensorEventListenerImpl.DEBUG && systemClockTimeStamp - (HwDualSensorEventListenerImpl.this.mFrontEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp);
                }
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

    private interface SensorDataSender {
        int getRate();

        int getTimer();

        boolean needQueue();

        void sendData();

        void setTimer(int i);
    }

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

    private interface FusedSensorDataRefresher {
        void updateData(int i, int i2, long j, long j2);
    }

    private class FusedSensorData extends Observable {
        private float mAdjustMin;
        private float mBackFloorThresh;
        private int[] mBackLuxArray = new int[]{-1, -1, -1};
        private long mBackLuxDeviation;
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
        private int[] mFrontLuxArray = new int[]{-1, -1, -1};
        private int mFrontLuxIndex = 0;
        private float mFrontLuxRelativeChange = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private int mFrontLuxStaProInd;
        public SensorDataObserver mFrontSensorDataObserver;
        private boolean mFrontUpdated;
        private int mFusedCct = -1;
        private int mFusedLux = -1;
        private long mLastValueBackSensorTime = -1;
        private long mLastValueFrontSensorTime = -1;
        private long mLastValueSystemTime = -1;
        private float mModifyFactor;
        private float mPreBackFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private float mPreFrontFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        private float mStaProb;
        private int[][] mStaProbLUT = ((int[][]) Array.newInstance(Integer.TYPE, new int[]{5, 5}));

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
                FusedSensorData.this.mDebugPrintTime = SystemClock.uptimeMillis();
                if (HwDualSensorEventListenerImpl.DEBUG && FusedSensorData.this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.update(): " + this.mName + " warmUp=" + HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg + " lux=" + lux + " cct=" + cct + " systemTime=" + systemTimeStamp + " sensorTime=" + sensorTimeStamp);
                }
                this.mRefresher.updateData(lux, cct, systemTimeStamp, sensorTimeStamp);
                if (HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg < 2) {
                    if (FusedSensorData.this.mFrontUpdated && FusedSensorData.this.mBackUpdated) {
                        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.this;
                        hwDualSensorEventListenerImpl.mFusedWarmUpFlg = hwDualSensorEventListenerImpl.mFusedWarmUpFlg + 1;
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
            this.mStaProbLUT[4][0] = false;
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
            this.mFrontSensorDataObserver = new SensorDataObserver(new FusedSensorDataRefresher() {
                public void updateData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
                    FusedSensorData.this.setFrontData(lux, cct, systemTimeStamp, sensorTimeStamp);
                }
            }, "FrontSensor");
            this.mBackSensorDataObserver = new SensorDataObserver(new FusedSensorDataRefresher() {
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
            this.mLastValueSystemTime = systemTimeStamp;
            this.mLastValueFrontSensorTime = sensorTimeStamp;
            this.mFrontUpdated = true;
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.setFrontData(): mCurFrontFiltered=" + this.mCurFrontFiltered + " mFrontLuxRelativeChange= " + this.mFrontLuxRelativeChange + " lux=" + lux + " cct=" + cct + " systemTime=" + this.mLastValueSystemTime + " sensorTime=" + this.mLastValueFrontSensorTime);
            }
        }

        public void setBackData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
            this.mBackLuxStaProInd = getLuxStableProbabilityIndex(lux, false);
            this.mCurBackCct = cct;
            this.mLastValueSystemTime = systemTimeStamp;
            this.mLastValueBackSensorTime = sensorTimeStamp;
            this.mBackUpdated = true;
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.setBackData(): mCurBackFiltered=" + this.mCurBackFiltered + " mBackLuxRelativeChange=" + this.mBackLuxRelativeChange + " lux=" + lux + " cct=" + cct + " systemTime=" + this.mLastValueSystemTime + " sensorTime=" + this.mLastValueBackSensorTime);
            }
        }

        private int getLuxStableProbabilityIndex(int lux, boolean frontSensorIsSelected) {
            if (lux < 0 && HwDualSensorEventListenerImpl.DEBUG) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "getLuxStableProbabilityIndex exception: lux=" + lux + " frontSensorIsSelected=" + frontSensorIsSelected);
            }
            int[] luxArray = frontSensorIsSelected ? this.mFrontLuxArray : this.mBackLuxArray;
            int luxArrayIndex = frontSensorIsSelected ? this.mFrontLuxIndex : this.mBackLuxIndex;
            float preFiltered = frontSensorIsSelected ? this.mPreFrontFiltered : this.mPreBackFiltered;
            int[] stabilityThresholdList = frontSensorIsSelected ? HwDualSensorEventListenerImpl.FRONT_SENSOR_STABILITY_THRESHOLD : HwDualSensorEventListenerImpl.BACK_SENSOR_STABILITY_THRESHOLD;
            luxArray[luxArrayIndex] = lux;
            luxArrayIndex = (luxArrayIndex + 1) % 3;
            int sum = 0;
            int count = 0;
            for (int i = 0; i < 3; i++) {
                if (luxArray[i] > 0) {
                    sum += luxArray[i];
                    count++;
                }
            }
            float curFiltered = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            if (count > 0) {
                curFiltered = ((float) sum) / ((float) count);
            }
            if (!frontSensorIsSelected) {
                this.mBackLuxDeviation = 0;
                for (int i2 : luxArray) {
                    this.mBackLuxDeviation += Math.abs(((long) i2) - ((long) curFiltered));
                }
                this.mBackLuxDeviation /= (long) luxArray.length;
            }
            float luxRelativeChange = 100.0f;
            if (Float.compare(preFiltered, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
                luxRelativeChange = ((curFiltered - preFiltered) * 100.0f) / preFiltered;
            }
            int targetIndex = 0;
            int stabilityThresholdListSize = stabilityThresholdList.length;
            while (targetIndex < stabilityThresholdListSize && Float.compare(luxRelativeChange, (float) stabilityThresholdList[targetIndex]) > 0) {
                targetIndex++;
            }
            if (targetIndex < 0) {
                targetIndex = 0;
            } else if (targetIndex > stabilityThresholdListSize) {
                targetIndex = stabilityThresholdListSize;
            }
            if (frontSensorIsSelected) {
                this.mCurFrontFiltered = curFiltered;
                this.mFrontLuxRelativeChange = luxRelativeChange;
                this.mFrontLuxIndex = luxArrayIndex;
            } else {
                this.mCurBackFiltered = curFiltered;
                this.mBackLuxRelativeChange = luxRelativeChange;
                this.mBackLuxIndex = luxArrayIndex;
            }
            return targetIndex;
        }

        private void updateFusedData() {
            getStabilizedLuxAndCct();
        }

        private void getStabilizedLuxAndCct() {
            int i;
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
                    if (this.mCurFrontCct > this.mCurBackCct) {
                        i = this.mCurFrontCct;
                    } else {
                        i = this.mCurBackCct;
                    }
                    this.mFusedCct = i;
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    }
                    return;
                }
            } else if (this.mCurBackFiltered > this.mBackRoofThresh || this.mBackLuxDeviation > HwDualSensorEventListenerImpl.BACK_LUX_DEVIATION_THRESH) {
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
                    if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): mFusedLux=" + this.mFusedLux + " mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux + " mFusedCct=" + this.mFusedCct + " mCurFrontCct=" + this.mCurFrontCct + " mCurBackCct=" + this.mCurBackCct);
                    }
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    }
                    return;
                }
            } else {
                float darkRoomDelta3 = this.mCurBackFiltered - this.mCurFrontFiltered;
                float curDarkRoomDelta = this.mDarkRoomDelta;
                if (this.mCurFrontFiltered >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && this.mCurFrontFiltered <= this.mDarkRoomThresh) {
                    curDarkRoomDelta = this.mDarkRoomDelta;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh1) <= 0) {
                    if (Float.compare(this.mDarkRoomThresh1 - this.mDarkRoomThresh, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) == 0) {
                        curDarkRoomDelta = this.mDarkRoomDelta;
                    } else {
                        curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh) * (this.mDarkRoomDelta1 - this.mDarkRoomDelta)) / (this.mDarkRoomThresh1 - this.mDarkRoomThresh)) + this.mDarkRoomDelta;
                    }
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh2) < 0) {
                    if (Float.compare(this.mDarkRoomThresh2 - this.mDarkRoomThresh1, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) == 0) {
                        curDarkRoomDelta = this.mDarkRoomDelta1;
                    } else {
                        curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh1) * (this.mDarkRoomDelta2 - this.mDarkRoomDelta1)) / (this.mDarkRoomThresh2 - this.mDarkRoomThresh1)) + this.mDarkRoomDelta1;
                    }
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh3) >= 0) {
                    curDarkRoomDelta = darkRoomDelta3;
                } else if (Float.compare(this.mDarkRoomThresh3 - this.mDarkRoomThresh2, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) == 0) {
                    curDarkRoomDelta = this.mDarkRoomDelta2;
                } else {
                    curDarkRoomDelta = (((this.mCurFrontFiltered - this.mDarkRoomThresh2) * (darkRoomDelta3 - this.mDarkRoomDelta2)) / (this.mDarkRoomThresh3 - this.mDarkRoomThresh2)) + this.mDarkRoomDelta2;
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
                    if (this.mCurFrontCct > this.mCurBackCct) {
                        i = this.mCurFrontCct;
                    } else {
                        i = this.mCurBackCct;
                    }
                    this.mFusedCct = i;
                } else {
                    if (HwDualSensorEventListenerImpl.DEBUG) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    }
                    return;
                }
            }
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): mFusedLux=" + this.mFusedLux + " mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux + " mFusedCct=" + this.mFusedCct + " mCurFrontCct=" + this.mCurFrontCct + " mCurBackCct=" + this.mCurBackCct);
            }
        }

        public void sendFusedData() {
            if (this.mFrontUpdated && (this.mBackUpdated ^ 1) == 0) {
                updateFusedData();
                Object data = new long[]{(long) this.mFusedLux, (long) this.mFusedCct, this.mLastValueSystemTime, this.mLastValueFrontSensorTime > this.mLastValueBackSensorTime ? this.mLastValueFrontSensorTime : this.mLastValueBackSensorTime, (long) this.mCurFrontLux};
                Object dataToSend = data;
                if (this.mFusedLux < 0 || this.mFusedCct < 0) {
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.sendFusedData(): skip. mFusedLux=" + this.mFusedLux + " mFusedCct=" + this.mFusedCct);
                    return;
                }
                if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - (HwDualSensorEventListenerImpl.this.mFusedEnableTime / 1000000) < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.sendFusedData(): lux=" + this.mFusedLux + " cct=" + this.mFusedCct + " sensorTime=" + lastValueSensorTime + " FrontSensorTime=" + this.mLastValueFrontSensorTime + " BackSensorTime=" + this.mLastValueBackSensorTime);
                }
                setChanged();
                notifyObservers(data);
                return;
            }
            if (HwDualSensorEventListenerImpl.DEBUG) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "Skip this sendFusedData! mFrontUpdated=" + this.mFrontUpdated + " mBackUpdated=" + this.mBackUpdated);
            }
        }

        public int getSensorLuxData() {
            return this.mFusedLux;
        }

        public int getSensorCctData() {
            return this.mFusedCct;
        }

        public long getSensorDataTime() {
            return this.mLastValueSystemTime;
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
            this.mLastValueSystemTime = -1;
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
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "STABILIZED_PROBABILITY_LUT.length=" + HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT.length);
                }
            } else {
                for (int rowInd = 0; rowInd < 5; rowInd++) {
                    for (int colInd = 0; colInd < 5; colInd++) {
                        this.mStaProbLUT[rowInd][colInd] = HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT[(rowInd * 5) + colInd];
                    }
                }
            }
        }
    }

    private static class SensorData extends Observable {
        private List<Integer> mCctDataList;
        private long mDebugPrintTime;
        private long mEnableTime;
        private int mLastSensorCctValue;
        private int mLastSensorLuxValue;
        private long mLastValueSensorTime;
        private long mLastValueSystemTime;
        private List<Integer> mLuxDataList;

        private SensorData(int rateMillis) {
            this.mLastSensorLuxValue = -1;
            this.mLastSensorCctValue = -1;
            this.mLastValueSystemTime = -1;
            this.mLastValueSensorTime = -1;
            this.mEnableTime = -1;
            this.mDebugPrintTime = -1;
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
                Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorData.sendSensorData(): skip. mLastSensorLuxValue=" + this.mLastSensorLuxValue + " mLastSensorCctValue=" + this.mLastSensorCctValue);
                return;
            }
            Object data3 = new long[]{(long) this.mLastSensorLuxValue, (long) this.mLastSensorCctValue, this.mLastValueSystemTime, this.mLastValueSensorTime};
            this.mDebugPrintTime = SystemClock.uptimeMillis();
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - this.mEnableTime < HwDualSensorEventListenerImpl.mDebugPrintInterval) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorData.sendSensorData(): lux=" + this.mLastSensorLuxValue + " cct=" + this.mLastSensorCctValue + " systemTime=" + this.mLastValueSystemTime + " sensorTime=" + this.mLastValueSensorTime);
            }
            Object dataToSend = data3;
            setChanged();
            notifyObservers(data3);
        }

        private void setEnableTime(long timeNanos) {
            this.mEnableTime = timeNanos / 1000000;
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
            this.mDebugPrintTime = -1;
        }
    }

    static {
        boolean isLoggable = !Log.HWINFO ? Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false : true;
        DEBUG = isLoggable;
    }

    private HwDualSensorEventListenerImpl(SensorManager sensorManager) {
        if (!parseParameters()) {
            Slog.i(TAG, "parse parameters failed!");
        }
        this.mFrontSensorData = new SensorData(mFrontRateMillis);
        this.mBackSensorData = new SensorData(mBackRateMillis);
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
                HwDualSensorEventListenerImpl.this.mFusedSensorData.sendFusedData();
            }

            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mFusedEnable && HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg >= 2;
            }
        };
        this.mDualSensorProcessThread = new HandlerThread(TAG);
        this.mDualSensorProcessThread.start();
        this.mHandler = new DualSensorHandler(this.mDualSensorProcessThread.getLooper());
        this.mSensorManager = sensorManager;
    }

    public static HwDualSensorEventListenerImpl getInstance(SensorManager sensorManager) {
        if (mInstance == null) {
            synchronized (HwDualSensorEventListenerImpl.class) {
                if (mInstance == null) {
                    mInstance = new HwDualSensorEventListenerImpl(sensorManager);
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

    /* JADX WARNING: inconsistent code. */
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
                    Slog.d(TAG, "more than one color sensor: " + LightSensorList.size());
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
                }
                detachFrontSensorData(this.mFusedSensorData.mFrontSensorDataObserver);
                detachBackSensorData(this.mFusedSensorData.mBackSensorDataObserver);
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
        float IR1norm = ((PRODUCTION_CALIBRATION_IR1 * 1600.128f) * IRraw.floatValue()) / (ATIME * AGAIN);
        int lux = 0;
        int cct = 0;
        if (Float.compare(Ynorm, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) != 0) {
            float lux_f;
            float X;
            float Y;
            float Z;
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
                    cct = (int) (0.5f + ((((((-499.0f * tempvalue) * tempvalue) * tempvalue) + ((3525.0f * tempvalue) * tempvalue)) - (6823.3f * tempvalue)) + 5520.33f));
                }
            }
            lux = (int) (0.5f + lux_f);
        }
        return new int[]{lux, cct};
    }

    private boolean parseParameters() {
        Throwable th;
        boolean parseResult = false;
        FileInputStream fileInputStream = null;
        try {
            FileInputStream inputStream = new FileInputStream(DUAL_SENSOR_XML_PATH);
            try {
                parseResult = parseParametersFromXML(inputStream);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
                fileInputStream = inputStream;
            } catch (FileNotFoundException e2) {
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e3) {
                    }
                }
                if (DEBUG) {
                    Slog.d(TAG, "parseResult=" + parseResult);
                }
                printParameters();
                return parseResult;
            } catch (IOException e4) {
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e5) {
                    }
                }
                if (DEBUG) {
                    Slog.d(TAG, "parseResult=" + parseResult);
                }
                printParameters();
                return parseResult;
            } catch (Exception e6) {
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e7) {
                    }
                }
                if (DEBUG) {
                    Slog.d(TAG, "parseResult=" + parseResult);
                }
                printParameters();
                return parseResult;
            } catch (Throwable th2) {
                th = th2;
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e8) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (DEBUG) {
                Slog.d(TAG, "parseResult=" + parseResult);
            }
            printParameters();
            return parseResult;
        } catch (IOException e10) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (DEBUG) {
                Slog.d(TAG, "parseResult=" + parseResult);
            }
            printParameters();
            return parseResult;
        } catch (Exception e11) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (DEBUG) {
                Slog.d(TAG, "parseResult=" + parseResult);
            }
            printParameters();
            return parseResult;
        } catch (Throwable th3) {
            th = th3;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            throw th;
        }
        if (DEBUG) {
            Slog.d(TAG, "parseResult=" + parseResult);
        }
        printParameters();
        return parseResult;
    }

    private boolean parseParametersFromXML(InputStream inStream) {
        if (DEBUG) {
            Slog.d(TAG, "parse dual sensor parameters from xml: " + DUAL_SENSOR_XML_PATH);
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
                                                                                    if (!nodeName.equals("ModuleSensorOptions")) {
                                                                                        if (!moduleSensorOptionsLoadStarted) {
                                                                                            if (!nodeName.equals("AlgorithmParameters")) {
                                                                                                boolean parseStabilizedProbabilityLUT;
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
                                                                                                                                                if (algorithmParametersLoadStarted && nodeName.equals("BackLuxDeviationThresh")) {
                                                                                                                                                    BACK_LUX_DEVIATION_THRESH = Long.parseLong(parser.nextText());
                                                                                                                                                    break;
                                                                                                                                                }
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
                                                                                                        parseStabilizedProbabilityLUT = parseStabilizedProbabilityLUT(parser.nextText());
                                                                                                        break;
                                                                                                    }
                                                                                                    parseStabilizedProbabilityLUT = parseSensorStabilityThresholdList(parser.nextText(), false);
                                                                                                    break;
                                                                                                }
                                                                                                parseStabilizedProbabilityLUT = parseSensorStabilityThresholdList(parser.nextText(), true);
                                                                                                break;
                                                                                            }
                                                                                            algorithmParametersLoadStarted = true;
                                                                                            break;
                                                                                        }
                                                                                        String moduleName = nodeName;
                                                                                        int sensorOption = Integer.parseInt(parser.nextText());
                                                                                        mModuleSensorMap.put(nodeName, Integer.valueOf(sensorOption));
                                                                                        break;
                                                                                    }
                                                                                    moduleSensorOptionsLoadStarted = true;
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
                                if (!nodeName.equals("AlgorithmParameters")) {
                                    break;
                                }
                                algorithmParametersLoadStarted = false;
                                break;
                            }
                            moduleSensorOptionsLoadStarted = false;
                            break;
                        }
                        backSensorParamLoadStarted = false;
                        break;
                    default:
                        break;
                }
            }
            return true;
        } catch (XmlPullParserException e) {
            return false;
        } catch (IOException e2) {
            return false;
        } catch (NumberFormatException e3) {
            return false;
        } catch (RuntimeException e4) {
            return false;
        } catch (Exception e5) {
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
        if (mModuleSensorMap == null || !mModuleSensorMap.containsKey(moduleName)) {
            Slog.i(TAG, moduleName + " getModuleSensorOption=" + -1);
            return -1;
        }
        Slog.i(TAG, moduleName + " getModuleSensorOption=" + mModuleSensorMap.get(moduleName));
        return ((Integer) mModuleSensorMap.get(moduleName)).intValue();
    }

    private boolean parseSensorStabilityThresholdList(String nodeContent, boolean isFrontSensor) {
        if (nodeContent == null) {
            Slog.i(TAG, "parseSensorStabilityThresholdList nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 4);
        int[] parsedArray = new int[4];
        for (int index = 0; index < 4; index++) {
            parsedArray[index] = Integer.parseInt(splitedContent[index]);
        }
        if (isFrontSensor) {
            FRONT_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
        } else {
            BACK_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
        }
        return true;
    }

    private boolean parseStabilizedProbabilityLUT(String nodeContent) {
        if (nodeContent == null) {
            Slog.d(TAG, "parseStabilizedProbabilityLUT nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 25);
        int[] parsedArray = new int[25];
        for (int index = 0; index < 25; index++) {
            parsedArray[index] = Integer.parseInt(splitedContent[index]);
        }
        STABILIZED_PROBABILITY_LUT = Arrays.copyOf(parsedArray, parsedArray.length);
        return true;
    }

    private void printParameters() {
        if (DEBUG) {
            Slog.d(TAG, "IR_BOUNDRY=" + IR_BOUNDRY);
            Slog.d(TAG, "LUX_COEF_LOW_X=" + LUX_COEF_LOW_X + " LUX_COEF_LOW_Y=" + LUX_COEF_LOW_Y + " LUX_COEF_LOW_Z=" + LUX_COEF_LOW_Z + " LUX_COEF_LOW_IR=" + LUX_COEF_LOW_IR + " LUX_COEF_LOW_OFFSET=" + LUX_COEF_LOW_OFFSET);
            Slog.d(TAG, "LUX_COEF_HIGH_X=" + LUX_COEF_HIGH_X + " LUX_COEF_HIGH_Y=" + LUX_COEF_HIGH_Y + " LUX_COEF_HIGH_Z=" + LUX_COEF_HIGH_Z + " LUX_COEF_HIGH_IR=" + LUX_COEF_HIGH_IR + " LUX_COEF_HIGH_OFFSET=" + LUX_COEF_HIGH_OFFSET);
            Slog.d(TAG, "COLOR_MAT_LOW_11=" + COLOR_MAT_LOW_11 + " COLOR_MAT_LOW_12=" + COLOR_MAT_LOW_12 + " COLOR_MAT_LOW_13=" + COLOR_MAT_LOW_13 + " COLOR_MAT_LOW_14=" + COLOR_MAT_LOW_14 + " COLOR_MAT_LOW_21=" + COLOR_MAT_LOW_21 + " COLOR_MAT_LOW_22=" + COLOR_MAT_LOW_22 + " COLOR_MAT_LOW_23=" + COLOR_MAT_LOW_23 + " COLOR_MAT_LOW_24=" + COLOR_MAT_LOW_24 + " COLOR_MAT_LOW_31=" + COLOR_MAT_LOW_31 + " COLOR_MAT_LOW_32=" + COLOR_MAT_LOW_32 + " COLOR_MAT_LOW_33=" + COLOR_MAT_LOW_33 + " COLOR_MAT_LOW_34=" + COLOR_MAT_LOW_34);
            Slog.d(TAG, "COLOR_MAT_HIGH_11=" + COLOR_MAT_HIGH_11 + " COLOR_MAT_HIGH_12=" + COLOR_MAT_HIGH_12 + " COLOR_MAT_HIGH_13=" + COLOR_MAT_HIGH_13 + " COLOR_MAT_HIGH_14=" + COLOR_MAT_HIGH_14 + " COLOR_MAT_HIGH_21=" + COLOR_MAT_HIGH_21 + " COLOR_MAT_HIGH_22=" + COLOR_MAT_HIGH_22 + " COLOR_MAT_HIGH_23=" + COLOR_MAT_HIGH_23 + " COLOR_MAT_HIGH_24=" + COLOR_MAT_HIGH_24 + " COLOR_MAT_HIGH_31=" + COLOR_MAT_HIGH_31 + " COLOR_MAT_HIGH_32=" + COLOR_MAT_HIGH_32 + " COLOR_MAT_HIGH_33=" + COLOR_MAT_HIGH_33 + " COLOR_MAT_HIGH_34=" + COLOR_MAT_HIGH_34);
            Slog.d(TAG, "ATime=" + ATIME + " AGain=" + AGAIN);
            Slog.d(TAG, "ProductionCalibration:" + PRODUCTION_CALIBRATION_X + " " + PRODUCTION_CALIBRATION_Y + " " + PRODUCTION_CALIBRATION_Z + " " + PRODUCTION_CALIBRATION_IR1);
            Slog.d(TAG, "mFrontRateMillis=" + mFrontRateMillis + " mBackRateMillis=" + mBackRateMillis + " mFusedRateMillis=" + mFusedRateMillis);
            Slog.d(TAG, "FRONT_SENSOR_STABILITY_THRESHOLD:" + Arrays.toString(FRONT_SENSOR_STABILITY_THRESHOLD));
            Slog.d(TAG, "BACK_SENSOR_STABILITY_THRESHOLD:" + Arrays.toString(BACK_SENSOR_STABILITY_THRESHOLD));
            Slog.d(TAG, "STABILIZED_PROBABILITY_LUT:" + Arrays.toString(STABILIZED_PROBABILITY_LUT));
            Slog.d(TAG, "BACK_ROOF_THRESH=" + BACK_ROOF_THRESH + " BACK_FLOOR_THRESH=" + BACK_FLOOR_THRESH + " DARK_ROOM_THRESH=" + DARK_ROOM_THRESH + " DARK_ROOM_DELTA=" + DARK_ROOM_DELTA);
            Slog.d(TAG, "DARK_ROOM_THRESH1=" + DARK_ROOM_THRESH1 + " DARK_ROOM_THRESH2=" + DARK_ROOM_THRESH2 + " DARK_ROOM_THRESH3=" + DARK_ROOM_THRESH3 + " DARK_ROOM_DELTA1=" + DARK_ROOM_DELTA1 + " DARK_ROOM_DELTA2=" + DARK_ROOM_DELTA2);
        }
    }
}

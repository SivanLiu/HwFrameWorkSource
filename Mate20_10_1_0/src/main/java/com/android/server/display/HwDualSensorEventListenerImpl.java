package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwDualSensorEventListenerImpl {
    private static float AGAIN = 16.0f;
    private static float AGAIN_RGBCW = 128.0f;
    private static float ATIME = 100.008f;
    private static float ATIME_RGBCW = 100.0f;
    /* access modifiers changed from: private */
    public static float BACK_FLOOR_THRESH = 5.0f;
    /* access modifiers changed from: private */
    public static long BACK_LUX_DEVIATION_THRESH = 10000;
    private static final int BACK_MSG_TIMER = 2;
    /* access modifiers changed from: private */
    public static float BACK_ROOF_THRESH = 400000.0f;
    /* access modifiers changed from: private */
    public static int[] BACK_SENSOR_STABILITY_THRESHOLD = {-30, -4, 4, 30};
    private static final int CCT_MIN_VALUE = 0;
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
    private static float COLOR_MAT_RGBCW_HIGH_11 = 0.057276506f;
    private static float COLOR_MAT_RGBCW_HIGH_12 = 0.14116754f;
    private static float COLOR_MAT_RGBCW_HIGH_13 = -0.15786317f;
    private static float COLOR_MAT_RGBCW_HIGH_14 = 0.15437019f;
    private static float COLOR_MAT_RGBCW_HIGH_15 = -0.083569825f;
    private static float COLOR_MAT_RGBCW_HIGH_21 = 0.05078651f;
    private static float COLOR_MAT_RGBCW_HIGH_22 = 0.08124515f;
    private static float COLOR_MAT_RGBCW_HIGH_23 = -0.06590506f;
    private static float COLOR_MAT_RGBCW_HIGH_24 = 0.11051277f;
    private static float COLOR_MAT_RGBCW_HIGH_25 = -0.07230203f;
    private static float COLOR_MAT_RGBCW_HIGH_31 = 0.068063304f;
    private static float COLOR_MAT_RGBCW_HIGH_32 = -0.0063071745f;
    private static float COLOR_MAT_RGBCW_HIGH_33 = -0.1753012f;
    private static float COLOR_MAT_RGBCW_HIGH_34 = 0.23409711f;
    private static float COLOR_MAT_RGBCW_HIGH_35 = -0.0483615f;
    private static float COLOR_MAT_RGBCW_LOW_11 = 0.028766254f;
    private static float COLOR_MAT_RGBCW_LOW_12 = 0.06764202f;
    private static float COLOR_MAT_RGBCW_LOW_13 = 0.04326469f;
    private static float COLOR_MAT_RGBCW_LOW_14 = 0.009134116f;
    private static float COLOR_MAT_RGBCW_LOW_15 = -0.054076713f;
    private static float COLOR_MAT_RGBCW_LOW_21 = 0.023575246f;
    private static float COLOR_MAT_RGBCW_LOW_22 = 0.030006304f;
    private static float COLOR_MAT_RGBCW_LOW_23 = 0.097151525f;
    private static float COLOR_MAT_RGBCW_LOW_24 = -0.0017650401f;
    private static float COLOR_MAT_RGBCW_LOW_25 = -0.05097885f;
    private static float COLOR_MAT_RGBCW_LOW_31 = 0.0585f;
    private static float COLOR_MAT_RGBCW_LOW_32 = -0.041188527f;
    private static float COLOR_MAT_RGBCW_LOW_33 = -0.082708225f;
    private static float COLOR_MAT_RGBCW_LOW_34 = 0.17139152f;
    private static float COLOR_MAT_RGBCW_LOW_35 = -0.03970633f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_DELTA = 5.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_DELTA1 = 10.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_DELTA2 = 30.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_THRESH = 3.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_THRESH1 = 15.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_THRESH2 = 35.0f;
    /* access modifiers changed from: private */
    public static float DARK_ROOM_THRESH3 = 75.0f;
    /* access modifiers changed from: private */
    public static boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String DUAL_SENSOR_XML_PATH = "/xml/lcd/DualSensorConfig.xml";
    private static final int FLASHLIGHT_DETECTION_MODE_CALLBACK = 1;
    private static final int FLASHLIGHT_DETECTION_MODE_THRESHOLD = 0;
    /* access modifiers changed from: private */
    public static int FLASHLIGHT_OFF_TIME_THRESHOLD_MS = 600;
    private static final int FRONT_MSG_TIMER = 1;
    /* access modifiers changed from: private */
    public static int[] FRONT_SENSOR_STABILITY_THRESHOLD = {-30, -4, 4, 30};
    private static final int FUSED_MSG_TIMER = 3;
    private static float IR_BOUNDRY = 1.0f;
    private static float IR_BOUNDRY_RGBCW = 0.3f;
    private static final boolean IS_HWDUAL_SENSOR_THREAD_DISABLE = SystemProperties.getBoolean("ro.config.hwdualsensorlistenthread.disable", false);
    public static final int LIGHT_SENSOR_BACK = 1;
    public static final int LIGHT_SENSOR_DEFAULT = -1;
    public static final int LIGHT_SENSOR_DUAL = 2;
    public static final int LIGHT_SENSOR_FRONT = 0;
    private static final int LUX_COEF = 100;
    private static float LUX_COEF_HIGH_1 = -0.004354524f;
    private static float LUX_COEF_HIGH_2 = -0.06864114f;
    private static float LUX_COEF_HIGH_3 = 0.24784172f;
    private static float LUX_COEF_HIGH_4 = -0.15217727f;
    private static float LUX_COEF_HIGH_5 = -6.927572E-4f;
    private static float LUX_COEF_HIGH_BLUE = -0.06468f;
    private static float LUX_COEF_HIGH_GREEN = 0.19239f;
    private static float LUX_COEF_HIGH_IR = 0.5164043f;
    private static float LUX_COEF_HIGH_OFFSET = 0.0f;
    private static float LUX_COEF_HIGH_RED = 0.02673f;
    private static float LUX_COEF_HIGH_X = -11.499684f;
    private static float LUX_COEF_HIGH_Y = 16.098469f;
    private static float LUX_COEF_HIGH_Z = -3.7601638f;
    private static float LUX_COEF_LOW_1 = 0.058521573f;
    private static float LUX_COEF_LOW_2 = -0.028247027f;
    private static float LUX_COEF_LOW_3 = -0.0530197f;
    private static float LUX_COEF_LOW_4 = -0.006297543f;
    private static float LUX_COEF_LOW_5 = 0.005755076f;
    private static float LUX_COEF_LOW_BLUE = -0.09306f;
    private static float LUX_COEF_LOW_GREEN = 0.02112f;
    private static float LUX_COEF_LOW_IR = -3.1549554f;
    private static float LUX_COEF_LOW_OFFSET = 0.0f;
    private static float LUX_COEF_LOW_RED = 0.02442f;
    private static float LUX_COEF_LOW_X = -3.0825195f;
    private static float LUX_COEF_LOW_Y = 7.662419f;
    private static float LUX_COEF_LOW_Z = -6.400353f;
    private static final int LUX_MIN_VALUE = 0;
    private static final int MSG_TIMER = 0;
    private static float PRODUCTION_CALIBRATION_B = 1.0f;
    private static float PRODUCTION_CALIBRATION_C = 1.0f;
    private static float PRODUCTION_CALIBRATION_G = 1.0f;
    private static float PRODUCTION_CALIBRATION_IR1 = 1.0f;
    private static float PRODUCTION_CALIBRATION_R = 1.0f;
    private static float PRODUCTION_CALIBRATION_W = 1.0f;
    private static float PRODUCTION_CALIBRATION_X = 1.0f;
    private static float PRODUCTION_CALIBRATION_Y = 1.0f;
    private static float PRODUCTION_CALIBRATION_Z = 1.0f;
    private static float RATIO_IR_GREEN = 0.5f;
    private static final int SPECTRUM_PARAM_NIR_NUM = 8;
    private static final int SPECTRUM_PARAM_NUM = 10;
    private static final int SPECTRUM_PARAM_POLY_NUM = 3;
    private static final int SPEC_LOW_LUX_TH_1 = 15;
    private static final int SPEC_LOW_LUX_TH_2 = 75;
    private static final int SPEC_LOW_LUX_TH_3 = 110;
    private static final float SPEC_NIR_WRIGHT = 0.15f;
    /* access modifiers changed from: private */
    public static int[] STABILIZED_PROBABILITY_LUT = new int[25];
    private static final String TAG = "HwDualSensorEventListenerImpl";
    private static final int TIMER_DISABLE = -1;
    private static final int VERSION_NORMALIZED_TYPE = 4;
    private static final int VERSION_NORMALIZED_TYPE_INDEX_CCT = 10;
    private static final int VERSION_NORMALIZED_TYPE_INDEX_LUX = 6;
    private static final int VERSION_NORMALIZED_TYPE_INDEX_MAX = 11;
    private static final int VERSION_RGBCW_TYPE = 3;
    private static final int VERSION_RGB_TYPE = 2;
    private static final int VERSION_SPECTRUM_TYPE = 12;
    private static final int VERSION_XYZ_TYPE = 1;
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
    /* access modifiers changed from: private */
    public static int mBackRateMillis = 300;
    private static final long mDebugPrintInterval = 1000000000;
    /* access modifiers changed from: private */
    public static int mFlashlightDetectionMode = 0;
    /* access modifiers changed from: private */
    public static int mFrontRateMillis = 300;
    /* access modifiers changed from: private */
    public static int mFusedRateMillis = 300;
    private static volatile HwDualSensorEventListenerImpl mInstance = null;
    private static Hashtable<String, Integer> mModuleSensorMap = new Hashtable<>();
    /* access modifiers changed from: private */
    public static int mSensorVersion = 1;
    private static final int norm_coefficient = 12800;
    /* access modifiers changed from: private */
    public String mBackCameraId;
    /* access modifiers changed from: private */
    public boolean mBackEnable = false;
    /* access modifiers changed from: private */
    public long mBackEnableTime = -1;
    private Sensor mBackLightSensor;
    /* access modifiers changed from: private */
    public SensorDataSender mBackSender;
    /* access modifiers changed from: private */
    public int mBackSensorBypassCount = 0;
    /* access modifiers changed from: private */
    public int mBackSensorBypassCountMax = 3;
    /* access modifiers changed from: private */
    public SensorData mBackSensorData;
    private final SensorEventListener mBackSensorEventListener = new SensorEventListener() {
        /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass5 */

        public void onSensorChanged(SensorEvent event) {
            int[] arr;
            if (!HwDualSensorEventListenerImpl.this.mBackEnable) {
                Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): mBackEnable=false");
                return;
            }
            long sensorClockTimeStamp = event.timestamp;
            long systemClockTimeStamp = SystemClock.uptimeMillis();
            int[] arr2 = {0, 0};
            if (HwDualSensorEventListenerImpl.mSensorVersion == 1) {
                arr = HwDualSensorEventListenerImpl.this.convertXYZ2LuxAndCct(Float.valueOf(event.values[0]), Float.valueOf(event.values[1]), Float.valueOf(event.values[2]), Float.valueOf(event.values[3]));
            } else if (HwDualSensorEventListenerImpl.mSensorVersion == 2) {
                arr = HwDualSensorEventListenerImpl.this.convertRGB2LuxAndCct(event.values[0], event.values[1], event.values[2], event.values[3]);
            } else if (HwDualSensorEventListenerImpl.mSensorVersion == 3) {
                arr = HwDualSensorEventListenerImpl.this.convertRGBCW2LuxAndCct(Float.valueOf(event.values[0]), Float.valueOf(event.values[1]), Float.valueOf(event.values[2]), Float.valueOf(event.values[3]), Float.valueOf(event.values[4]));
            } else {
                if (HwDualSensorEventListenerImpl.mSensorVersion == 4) {
                    if (event.values.length >= 11) {
                        arr = HwDualSensorEventListenerImpl.this.convertNormalizedType2LuxAndCct(event.values[6], event.values[10]);
                    }
                } else if (HwDualSensorEventListenerImpl.mSensorVersion == 12) {
                    arr = HwDualSensorEventListenerImpl.this.convertSpectrumToLux(event);
                } else if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "Invalid mSensorVersion=" + HwDualSensorEventListenerImpl.mSensorVersion);
                }
                arr = arr2;
            }
            int lux = arr[0];
            int cct = arr[1];
            if (HwDualSensorEventListenerImpl.this.mBackWarmUpFlg >= 2) {
                HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
            } else if (sensorClockTimeStamp >= HwDualSensorEventListenerImpl.this.mBackEnableTime) {
                HwDualSensorEventListenerImpl.this.mBackSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp);
                HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.this;
                int unused = hwDualSensorEventListenerImpl.mBackWarmUpFlg = hwDualSensorEventListenerImpl.mBackWarmUpFlg + 1;
                if (HwDualSensorEventListenerImpl.this.mHandler != null) {
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(2);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(2);
                }
            } else if (HwDualSensorEventListenerImpl.DEBUG) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): Back sensor not ready yet!");
                return;
            } else {
                return;
            }
            if (HwDualSensorEventListenerImpl.DEBUG && sensorClockTimeStamp - HwDualSensorEventListenerImpl.this.mBackEnableTime < 1000000000) {
                printLuxAndCct(lux, cct, sensorClockTimeStamp, systemClockTimeStamp, event);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private void printLuxAndCct(int lux, int cct, long sensorClockTimeStamp, long systemClockTimeStamp, SensorEvent event) {
            if (HwDualSensorEventListenerImpl.mSensorVersion == 1) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp + " Xraw=" + event.values[0] + " Yraw=" + event.values[1] + " Zraw=" + event.values[2] + " IRraw=" + event.values[3]);
            } else if (HwDualSensorEventListenerImpl.mSensorVersion == 2) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp + " red=" + event.values[0] + " green=" + event.values[1] + " blue=" + event.values[2] + " IR=" + event.values[3]);
            } else if (HwDualSensorEventListenerImpl.mSensorVersion == 3) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp + " Craw=" + event.values[0] + " Rraw=" + event.values[1] + " Graw=" + event.values[2] + " Braw=" + event.values[3] + " Wraw=" + event.values[4]);
            } else if (HwDualSensorEventListenerImpl.mSensorVersion == 4 && event.values.length >= 11) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mBackWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp + " luxRaw=" + event.values[6] + " cctRaw=" + event.values[10]);
            }
        }
    };
    private final Object mBackSensorLock = new Object();
    /* access modifiers changed from: private */
    public int mBackSensorTimeOutTH = 5;
    /* access modifiers changed from: private */
    public int mBackTimeOutCount = 0;
    /* access modifiers changed from: private */
    public int mBackTimer = -1;
    /* access modifiers changed from: private */
    public int mBackWarmUpFlg = 0;
    private CameraManager mCameraManager;
    private final Context mContext;
    private HandlerThread mDualSensorProcessThread;
    /* access modifiers changed from: private */
    public volatile long mFlashLightOffTimeStampMs = 0;
    /* access modifiers changed from: private */
    public boolean mFrontEnable = false;
    /* access modifiers changed from: private */
    public long mFrontEnableTime = -1;
    private Sensor mFrontLightSensor;
    /* access modifiers changed from: private */
    public SensorDataSender mFrontSender;
    /* access modifiers changed from: private */
    public SensorData mFrontSensorData;
    private final SensorEventListener mFrontSensorEventListener = new SensorEventListener() {
        /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass4 */

        public void onSensorChanged(SensorEvent event) {
            long sensorClockTimeStamp;
            if (!HwDualSensorEventListenerImpl.this.mFrontEnable) {
                Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): mFrontEnable=false");
                return;
            }
            long systemClockTimeStamp = SystemClock.uptimeMillis();
            int lux = (int) (event.values[0] + 0.5f);
            int cct = (int) (event.values[1] + 0.5f);
            long sensorClockTimeStamp2 = event.timestamp;
            if (HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg >= 2) {
                sensorClockTimeStamp = sensorClockTimeStamp2;
                HwDualSensorEventListenerImpl.this.mFrontSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp2);
            } else if (sensorClockTimeStamp2 >= HwDualSensorEventListenerImpl.this.mFrontEnableTime) {
                sensorClockTimeStamp = sensorClockTimeStamp2;
                HwDualSensorEventListenerImpl.this.mFrontSensorData.setSensorData(lux, cct, systemClockTimeStamp, sensorClockTimeStamp2);
                HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.this;
                int unused = hwDualSensorEventListenerImpl.mFrontWarmUpFlg = hwDualSensorEventListenerImpl.mFrontWarmUpFlg + 1;
                if (HwDualSensorEventListenerImpl.this.mHandler != null) {
                    HwDualSensorEventListenerImpl.this.mHandler.removeMessages(1);
                    HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(1);
                }
            } else if (HwDualSensorEventListenerImpl.DEBUG) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): Front sensor not ready yet!");
                return;
            } else {
                return;
            }
            if (!HwDualSensorEventListenerImpl.DEBUG) {
                return;
            }
            if (sensorClockTimeStamp - HwDualSensorEventListenerImpl.this.mFrontEnableTime < 1000000000) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorEventListener.onSensorChanged(): warmUp=" + HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg + " lux=" + lux + " cct=" + cct + " sensorTime=" + sensorClockTimeStamp + " systemTime=" + systemClockTimeStamp);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mFrontSensorLock = new Object();
    /* access modifiers changed from: private */
    public int mFrontTimer = -1;
    /* access modifiers changed from: private */
    public int mFrontWarmUpFlg = 0;
    /* access modifiers changed from: private */
    public boolean mFusedEnable = false;
    /* access modifiers changed from: private */
    public long mFusedEnableTime = -1;
    /* access modifiers changed from: private */
    public SensorDataSender mFusedSender;
    /* access modifiers changed from: private */
    public FusedSensorData mFusedSensorData;
    /* access modifiers changed from: private */
    public final Object mFusedSensorLock = new Object();
    /* access modifiers changed from: private */
    public int mFusedTimer = -1;
    /* access modifiers changed from: private */
    public int mFusedWarmUpFlg = 0;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsFilterOn = true;
    private float mLuxScale = 1.016667f;
    private boolean mNeedNirCompensation = false;
    private double[] mPolyCoefs1 = {0.00189304246550637d, -0.0537671503168156d, 1.46102639307804d};
    private double[] mPolyCoefs2 = {-1.05424017372123E-4d, 0.00618684416953924d, 1.06133309650234d};
    private double[] mPolyCoefs3 = {-9.29273077531602E-7d, 1.63340136216729E-4d, 1.10127143743199d};
    private ThreadPoolExecutor mPool = new ThreadPoolExecutor(2, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(10), new DeThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    private double[] mRatioNir = {0.0447d, 0.0551d, 0.0531d, 0.0615d, 0.0391d, 0.0701d, 0.0531d, 0.0428d};
    private double[] mScaling = {1.4838d, 1.8705d, 1.8507d, 1.9243d, 1.7956d, 2.0007d, 1.923d, 2.3181d};
    private SensorManager mSensorManager;
    private double mSpectrumGain = 256.0d;
    private double[] mSpectrumParam = {130.2202d, 241.6954d, 731.8015d, 2453.8471d, 4208.88d, 2730.6698d, 618.0548d, 30.2623d, -5.9318d, -513.5437d};
    private double mSpectrumTime = 50.0d;
    /* access modifiers changed from: private */
    public int mTimer = -1;
    private CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass6 */

        public void onTorchModeUnavailable(String cameraId) {
            super.onTorchModeUnavailable(cameraId);
            long unused = HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = -2;
            Slog.i(HwDualSensorEventListenerImpl.TAG, "onTorchModeUnavailable, mFlashLightOffTimeStampMs set -2");
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            if (!cameraId.equals(HwDualSensorEventListenerImpl.this.mBackCameraId)) {
                return;
            }
            if (enabled) {
                long unused = HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = -1;
                Slog.i(HwDualSensorEventListenerImpl.TAG, "mFlashLightOffTimeStampMs set -1");
                return;
            }
            long unused2 = HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs = SystemClock.uptimeMillis();
            Slog.i(HwDualSensorEventListenerImpl.TAG, "mFlashLightOffTimeStampMs set " + HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs);
        }
    };

    private interface FusedSensorDataRefresher {
        void updateData(int i, int i2, long j, long j2);
    }

    /* access modifiers changed from: private */
    public interface SensorDataSender {
        int getRate();

        int getTimer();

        boolean needQueue();

        void sendData();

        void setTimer(int i);
    }

    static /* synthetic */ int access$1808(HwDualSensorEventListenerImpl x0) {
        int i = x0.mFusedWarmUpFlg;
        x0.mFusedWarmUpFlg = i + 1;
        return i;
    }

    static /* synthetic */ int access$4008(HwDualSensorEventListenerImpl x0) {
        int i = x0.mBackTimeOutCount;
        x0.mBackTimeOutCount = i + 1;
        return i;
    }

    private class DeThreadFactory implements ThreadFactory {
        private static final String NAME = "DisplayEngine_DualSensor";
        private int mCounter;

        private DeThreadFactory() {
        }

        public Thread newThread(Runnable runnable) {
            this.mCounter++;
            return new Thread(runnable, "DisplayEngine_DualSensor_Thread_" + this.mCounter);
        }
    }

    private HwDualSensorEventListenerImpl(Context context, SensorManager sensorManager) {
        this.mContext = context;
        if (!parseParameters()) {
            Slog.i(TAG, "parse parameters failed!");
        }
        this.mFrontSensorData = new SensorData(mFrontRateMillis);
        this.mBackSensorData = new SensorData(mBackRateMillis);
        this.mFusedSensorData = new FusedSensorData();
        this.mFrontSender = new SensorDataSender() {
            /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass1 */

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getRate() {
                return HwDualSensorEventListenerImpl.mFrontRateMillis;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mFrontTimer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void setTimer(int timer) {
                int unused = HwDualSensorEventListenerImpl.this.mFrontTimer = timer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void sendData() {
                HwDualSensorEventListenerImpl.this.mFrontSensorData.sendSensorData();
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mFrontEnable && HwDualSensorEventListenerImpl.this.mFrontWarmUpFlg >= 2;
            }
        };
        this.mBackSender = new SensorDataSender() {
            /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass2 */

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getRate() {
                return HwDualSensorEventListenerImpl.mBackRateMillis;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mBackTimer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void setTimer(int timer) {
                int unused = HwDualSensorEventListenerImpl.this.mBackTimer = timer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void sendData() {
                HwDualSensorEventListenerImpl.this.mBackSensorData.sendSensorData();
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mBackEnable && HwDualSensorEventListenerImpl.this.mBackWarmUpFlg >= 2;
            }
        };
        this.mFusedSender = new SensorDataSender() {
            /* class com.android.server.display.HwDualSensorEventListenerImpl.AnonymousClass3 */

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getRate() {
                return HwDualSensorEventListenerImpl.mFusedRateMillis;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public int getTimer() {
                return HwDualSensorEventListenerImpl.this.mFusedTimer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void setTimer(int timer) {
                int unused = HwDualSensorEventListenerImpl.this.mFusedTimer = timer;
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public void sendData() {
                synchronized (HwDualSensorEventListenerImpl.this.mFusedSensorLock) {
                    HwDualSensorEventListenerImpl.this.mFusedSensorData.sendFusedData();
                }
            }

            @Override // com.android.server.display.HwDualSensorEventListenerImpl.SensorDataSender
            public boolean needQueue() {
                return HwDualSensorEventListenerImpl.this.mFusedEnable && HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg >= 2;
            }
        };
        if (IS_HWDUAL_SENSOR_THREAD_DISABLE) {
            Slog.w(TAG, "HwDualSensorEventListenerImpl thread is disabled.");
        } else {
            this.mDualSensorProcessThread = new HandlerThread(TAG);
            this.mDualSensorProcessThread.start();
            this.mHandler = new DualSensorHandler(this.mDualSensorProcessThread.getLooper());
        }
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

    private void attachFrontSensorDataInner(Observer observer) {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            Slog.e(TAG, "SensorManager is null!");
            return;
        }
        if (this.mFrontLightSensor == null) {
            this.mFrontLightSensor = sensorManager.getDefaultSensor(5);
            if (DEBUG) {
                Slog.d(TAG, "Obtain mFrontLightSensor at first time.");
            }
        }
        enableFrontSensor();
        this.mFrontSensorData.addObserver(observer);
    }

    public void attachFrontSensorData(Observer observer) {
        this.mPool.execute(new Runnable(observer) {
            /* class com.android.server.display.$$Lambda$HwDualSensorEventListenerImpl$zVJPM7FQGIjTWf_MhwM4wSAQCKI */
            private final /* synthetic */ Observer f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwDualSensorEventListenerImpl.this.lambda$attachFrontSensorData$0$HwDualSensorEventListenerImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$attachFrontSensorData$0$HwDualSensorEventListenerImpl(Observer observer) {
        synchronized (this.mFrontSensorLock) {
            attachFrontSensorDataInner(observer);
        }
    }

    private void attachBackSensorDataInner(Observer observer) {
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            Slog.e(TAG, "SensorManager is null!");
            return;
        }
        if (this.mBackLightSensor == null) {
            List<Sensor> LightSensorList = sensorManager.getSensorList(65554);
            if (LightSensorList.size() == 1) {
                this.mBackLightSensor = LightSensorList.get(0);
                if (DEBUG) {
                    Slog.d(TAG, "Obtain mBackLightSensor at first time.");
                }
            } else if (DEBUG) {
                Slog.d(TAG, "more than one color sensor: " + LightSensorList.size());
                return;
            } else {
                return;
            }
        }
        enableBackSensor();
        this.mBackSensorData.addObserver(observer);
    }

    public void attachBackSensorData(Observer observer) {
        this.mPool.execute(new Runnable(observer) {
            /* class com.android.server.display.$$Lambda$HwDualSensorEventListenerImpl$bH_1ZeONSzV0wUySUmg1hmqcJQ */
            private final /* synthetic */ Observer f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwDualSensorEventListenerImpl.this.lambda$attachBackSensorData$1$HwDualSensorEventListenerImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$attachBackSensorData$1$HwDualSensorEventListenerImpl(Observer observer) {
        synchronized (this.mBackSensorLock) {
            attachBackSensorDataInner(observer);
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

    private void detachFrontSensorDataInner(Observer observer) {
        if (observer != null) {
            this.mFrontSensorData.deleteObserver(observer);
            if (this.mFrontSensorData.countObservers() == 0) {
                disableFrontSensor();
            }
        }
    }

    public void detachFrontSensorData(Observer observer) {
        this.mPool.execute(new Runnable(observer) {
            /* class com.android.server.display.$$Lambda$HwDualSensorEventListenerImpl$N_LeyumDurw4Wm2UOz2OoYm19aI */
            private final /* synthetic */ Observer f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwDualSensorEventListenerImpl.this.lambda$detachFrontSensorData$2$HwDualSensorEventListenerImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$detachFrontSensorData$2$HwDualSensorEventListenerImpl(Observer observer) {
        synchronized (this.mFrontSensorLock) {
            detachFrontSensorDataInner(observer);
        }
    }

    private void detachBackSensorDataInner(Observer observer) {
        if (observer != null) {
            this.mBackSensorData.deleteObserver(observer);
            if (this.mBackSensorData.countObservers() == 0) {
                disableBackSensor();
            }
        }
    }

    public void detachBackSensorData(Observer observer) {
        this.mPool.execute(new Runnable(observer) {
            /* class com.android.server.display.$$Lambda$HwDualSensorEventListenerImpl$F3quYngYUSTesl04PM0uMoOssik */
            private final /* synthetic */ Observer f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                HwDualSensorEventListenerImpl.this.lambda$detachBackSensorData$3$HwDualSensorEventListenerImpl(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$detachBackSensorData$3$HwDualSensorEventListenerImpl(Observer observer) {
        synchronized (this.mBackSensorLock) {
            detachBackSensorDataInner(observer);
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
            Slog.i(TAG, "enable front sensor...");
            this.mFrontEnable = true;
            this.mFrontWarmUpFlg = 0;
            this.mFrontEnableTime = SystemClock.elapsedRealtimeNanos();
            this.mFrontSensorData.setEnableTime(this.mFrontEnableTime);
            Handler handler = this.mHandler;
            if (handler != null) {
                this.mSensorManager.registerListener(this.mFrontSensorEventListener, this.mFrontLightSensor, mFrontRateMillis * 1000, handler);
            }
            Slog.i(TAG, "front sensor enabled");
        }
    }

    private void enableBackSensor() {
        if (!this.mBackEnable) {
            Slog.i(TAG, "enable back sensor...");
            this.mBackEnable = true;
            this.mBackWarmUpFlg = 0;
            this.mBackEnableTime = SystemClock.elapsedRealtimeNanos();
            this.mBackSensorData.setEnableTime(this.mBackEnableTime);
            Handler handler = this.mHandler;
            if (handler != null) {
                this.mSensorManager.registerListener(this.mBackSensorEventListener, this.mBackLightSensor, mBackRateMillis * 1000, handler);
            }
            Slog.i(TAG, "back sensor enabled");
        }
    }

    private void enableFusedSensor() {
        if (!this.mFusedEnable) {
            this.mFusedEnable = true;
            this.mFusedWarmUpFlg = 0;
            this.mFusedEnableTime = SystemClock.elapsedRealtimeNanos();
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.sendEmptyMessage(3);
            }
            if (DEBUG) {
                Slog.d(TAG, "Fused sensor enabled.");
            }
            int i = this.mBackSensorBypassCount;
            if (i > 0) {
                this.mBackSensorBypassCount = i - 1;
                Slog.i(TAG, "mBackSensorBypassCount-- :" + this.mBackSensorBypassCount);
            }
        }
    }

    private void disableFrontSensor() {
        if (this.mFrontEnable) {
            this.mFrontEnable = false;
            this.mSensorManager.unregisterListener(this.mFrontSensorEventListener);
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.removeMessages(1);
            }
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
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.removeMessages(2);
            }
            this.mBackSensorData.clearSensorData();
            if (DEBUG) {
                Slog.d(TAG, "Back sensor disabled.");
            }
        }
    }

    private void disableFusedSensor() {
        if (this.mFusedEnable) {
            this.mFusedEnable = false;
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.removeMessages(3);
            }
            this.mFusedSensorData.clearSensorData();
            if (DEBUG) {
                Slog.d(TAG, "Fused sensor disabled.");
            }
        }
    }

    /* access modifiers changed from: private */
    public static class SensorData extends Observable {
        private List<Integer> mCctDataList;
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
            this.mLuxDataList = new CopyOnWriteArrayList();
            this.mCctDataList = new CopyOnWriteArrayList();
        }

        /* access modifiers changed from: private */
        public void setSensorData(int lux, int cct, long systemClockTimeStamp, long sensorClockTimeStamp) {
            this.mLuxDataList.add(Integer.valueOf(lux));
            this.mCctDataList.add(Integer.valueOf(cct));
            this.mLastValueSystemTime = systemClockTimeStamp;
            this.mLastValueSensorTime = sensorClockTimeStamp;
        }

        /* access modifiers changed from: private */
        public void sendSensorData() {
            int i;
            int average;
            int average2;
            int count = 0;
            int sum = 0;
            for (Integer data : this.mLuxDataList) {
                sum += data.intValue();
                count++;
            }
            if (count != 0 && (average2 = sum / count) >= 0) {
                this.mLastSensorLuxValue = average2;
            }
            this.mLuxDataList.clear();
            int count2 = 0;
            int sum2 = 0;
            for (Integer data2 : this.mCctDataList) {
                sum2 += data2.intValue();
                count2++;
            }
            if (count2 != 0 && (average = sum2 / count2) >= 0) {
                this.mLastSensorCctValue = average;
            }
            this.mCctDataList.clear();
            int i2 = this.mLastSensorLuxValue;
            if (i2 < 0 || (i = this.mLastSensorCctValue) < 0) {
                Slog.i(HwDualSensorEventListenerImpl.TAG, "SensorData.sendSensorData(): skip. mLastSensorLuxValue=" + this.mLastSensorLuxValue + " mLastSensorCctValue=" + this.mLastSensorCctValue);
                return;
            }
            long[] data3 = {(long) i2, (long) i, this.mLastValueSystemTime, this.mLastValueSensorTime, (long) i2, (long) i2};
            if (HwDualSensorEventListenerImpl.DEBUG && SystemClock.elapsedRealtimeNanos() - this.mEnableTime < 1000000000) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "SensorData.sendSensorData(): lux=" + this.mLastSensorLuxValue + " cct=" + this.mLastSensorCctValue + " systemTime=" + this.mLastValueSystemTime + " sensorTime=" + this.mLastValueSensorTime);
            }
            setChanged();
            notifyObservers(data3);
        }

        /* access modifiers changed from: private */
        public void setEnableTime(long timeNanos) {
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

        /* access modifiers changed from: private */
        public void clearSensorData() {
            this.mLuxDataList.clear();
            this.mCctDataList.clear();
            this.mLastSensorLuxValue = -1;
            this.mLastSensorCctValue = -1;
            this.mLastValueSystemTime = -1;
            this.mLastValueSensorTime = -1;
            this.mEnableTime = -1;
        }
    }

    /* access modifiers changed from: private */
    public class FusedSensorData extends Observable {
        private float mAdjustMin;
        private float mBackFloorThresh;
        private long mBackLastValueSystemTime = -1;
        private int[] mBackLuxArray = {-1, -1, -1};
        private long mBackLuxDeviation;
        private int mBackLuxFiltered = -1;
        private int mBackLuxIndex = 0;
        private float mBackLuxRelativeChange = 0.0f;
        private int mBackLuxStaProInd;
        private float mBackRoofThresh;
        public SensorDataObserver mBackSensorDataObserver;
        /* access modifiers changed from: private */
        public boolean mBackUpdated;
        private int mCurBackCct = -1;
        private float mCurBackFiltered = 0.0f;
        private int mCurBackLux = -1;
        private int mCurFrontCct = -1;
        private float mCurFrontFiltered = 0.0f;
        private int mCurFrontLux = -1;
        private float mDarkRoomDelta;
        private float mDarkRoomDelta1;
        private float mDarkRoomDelta2;
        private float mDarkRoomThresh;
        private float mDarkRoomThresh1;
        private float mDarkRoomThresh2;
        private float mDarkRoomThresh3;
        /* access modifiers changed from: private */
        public long mDebugPrintTime = -1;
        private long mFrontLastValueSystemTime = -1;
        private int[] mFrontLuxArray = {-1, -1, -1};
        private int mFrontLuxFiltered = -1;
        private int mFrontLuxIndex = 0;
        private float mFrontLuxRelativeChange = 0.0f;
        private int mFrontLuxStaProInd;
        public SensorDataObserver mFrontSensorDataObserver;
        /* access modifiers changed from: private */
        public boolean mFrontUpdated;
        private int mFusedCct = -1;
        private int mFusedLux = -1;
        private long mLastValueBackSensorTime = -1;
        private long mLastValueFrontSensorTime = -1;
        private float mModifyFactor;
        private float mPreBackFiltered = 0.0f;
        private float mPreFrontFiltered = 0.0f;
        private float mStaProb;
        private int[][] mStaProbLUT = ((int[][]) Array.newInstance(int.class, 5, 5));

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
                long unused = FusedSensorData.this.mDebugPrintTime = SystemClock.elapsedRealtimeNanos();
                if (HwDualSensorEventListenerImpl.DEBUG && FusedSensorData.this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.update(): " + this.mName + " warmUp=" + HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg + " lux=" + lux + " cct=" + cct + " systemTime=" + systemTimeStamp + " sensorTime=" + sensorTimeStamp);
                }
                this.mRefresher.updateData(lux, cct, systemTimeStamp, sensorTimeStamp);
                if (HwDualSensorEventListenerImpl.this.mFusedWarmUpFlg < 2) {
                    if (FusedSensorData.this.mFrontUpdated && FusedSensorData.this.mBackUpdated) {
                        HwDualSensorEventListenerImpl.access$1808(HwDualSensorEventListenerImpl.this);
                    }
                    if (HwDualSensorEventListenerImpl.this.mHandler != null) {
                        HwDualSensorEventListenerImpl.this.mHandler.removeMessages(3);
                        HwDualSensorEventListenerImpl.this.mHandler.sendEmptyMessage(3);
                    }
                }
            }
        }

        public FusedSensorData() {
            int[][] iArr = this.mStaProbLUT;
            iArr[0][0] = 0;
            iArr[0][1] = 20;
            iArr[0][2] = 90;
            iArr[0][3] = 20;
            iArr[0][4] = 0;
            iArr[1][0] = 20;
            iArr[1][1] = 50;
            iArr[1][2] = 90;
            iArr[1][3] = 50;
            iArr[1][4] = 20;
            iArr[2][0] = 90;
            iArr[2][1] = 90;
            iArr[2][2] = 95;
            iArr[2][3] = 90;
            iArr[2][4] = 90;
            iArr[3][0] = 20;
            iArr[3][1] = 50;
            iArr[3][2] = 90;
            iArr[3][3] = 50;
            iArr[3][4] = 20;
            iArr[4][0] = 0;
            iArr[4][1] = 20;
            iArr[4][2] = 90;
            iArr[4][3] = 20;
            iArr[4][4] = 0;
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
                /* class com.android.server.display.HwDualSensorEventListenerImpl.FusedSensorData.AnonymousClass1 */

                @Override // com.android.server.display.HwDualSensorEventListenerImpl.FusedSensorDataRefresher
                public void updateData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
                    FusedSensorData.this.setFrontData(lux, cct, systemTimeStamp, sensorTimeStamp);
                }
            }, "FrontSensor");
            this.mBackSensorDataObserver = new SensorDataObserver(new FusedSensorDataRefresher(HwDualSensorEventListenerImpl.this) {
                /* class com.android.server.display.HwDualSensorEventListenerImpl.FusedSensorData.AnonymousClass2 */

                @Override // com.android.server.display.HwDualSensorEventListenerImpl.FusedSensorDataRefresher
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
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.setFrontData(): mCurFrontFiltered=" + this.mCurFrontFiltered + " mFrontLuxRelativeChange= " + this.mFrontLuxRelativeChange + " lux=" + lux + " cct=" + cct + " systemTime=" + this.mFrontLastValueSystemTime + " sensorTime=" + this.mLastValueFrontSensorTime);
            }
        }

        public void setBackData(int lux, int cct, long systemTimeStamp, long sensorTimeStamp) {
            this.mBackLuxStaProInd = getLuxStableProbabilityIndex(lux, false);
            this.mCurBackCct = cct;
            this.mBackLastValueSystemTime = systemTimeStamp;
            this.mLastValueBackSensorTime = sensorTimeStamp;
            this.mBackUpdated = true;
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.setBackData(): mCurBackFiltered=" + this.mCurBackFiltered + " mBackLuxRelativeChange=" + this.mBackLuxRelativeChange + " lux=" + lux + " cct=" + cct + " systemTime=" + this.mBackLastValueSystemTime + " sensorTime=" + this.mLastValueBackSensorTime);
            }
        }

        private int getLuxStableProbabilityIndex(int lux, boolean frontSensorIsSelected) {
            float curFiltered;
            if (lux < 0 && HwDualSensorEventListenerImpl.DEBUG) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "getLuxStableProbabilityIndex exception: lux=" + lux + " frontSensorIsSelected=" + frontSensorIsSelected);
            }
            int[] luxArray = frontSensorIsSelected ? this.mFrontLuxArray : this.mBackLuxArray;
            int luxArrayIndex = frontSensorIsSelected ? this.mFrontLuxIndex : this.mBackLuxIndex;
            float preFiltered = frontSensorIsSelected ? this.mPreFrontFiltered : this.mPreBackFiltered;
            int[] stabilityThresholdList = frontSensorIsSelected ? HwDualSensorEventListenerImpl.FRONT_SENSOR_STABILITY_THRESHOLD : HwDualSensorEventListenerImpl.BACK_SENSOR_STABILITY_THRESHOLD;
            luxArray[luxArrayIndex] = lux;
            int luxArrayIndex2 = (luxArrayIndex + 1) % 3;
            int sum = 0;
            int count = 0;
            for (int i = 0; i < 3; i++) {
                if (luxArray[i] > 0) {
                    sum += luxArray[i];
                    count++;
                }
            }
            if (!HwDualSensorEventListenerImpl.this.mIsFilterOn || count <= 0) {
                curFiltered = (float) lux;
            } else {
                curFiltered = ((float) sum) / ((float) count);
            }
            if (!frontSensorIsSelected) {
                this.mBackLuxDeviation = 0;
                for (int curInd = 0; curInd < luxArray.length; curInd++) {
                    this.mBackLuxDeviation += Math.abs(((long) luxArray[curInd]) - ((long) curFiltered));
                }
                this.mBackLuxDeviation /= (long) luxArray.length;
            }
            float luxRelativeChange = 100.0f;
            if (Float.compare(preFiltered, 0.0f) != 0) {
                luxRelativeChange = ((curFiltered - preFiltered) * 100.0f) / preFiltered;
            }
            int targetIndex = 0;
            int stabilityThresholdListSize = stabilityThresholdList.length;
            while (targetIndex < stabilityThresholdListSize && Float.compare(luxRelativeChange, (float) stabilityThresholdList[targetIndex]) > 0) {
                targetIndex++;
            }
            if (targetIndex > stabilityThresholdListSize) {
                targetIndex = stabilityThresholdListSize;
            }
            if (frontSensorIsSelected) {
                this.mCurFrontFiltered = curFiltered;
                this.mFrontLuxRelativeChange = luxRelativeChange;
                this.mFrontLuxIndex = luxArrayIndex2;
            } else {
                this.mCurBackFiltered = curFiltered;
                this.mBackLuxRelativeChange = luxRelativeChange;
                this.mBackLuxIndex = luxArrayIndex2;
            }
            return targetIndex;
        }

        private void updateFusedData() {
            getStabilizedLuxAndCct();
        }

        private void getStabilizedLuxAndCct() {
            float curDarkRoomDelta;
            float f = this.mCurFrontFiltered;
            this.mFrontLuxFiltered = (int) f;
            float f2 = this.mCurBackFiltered;
            this.mBackLuxFiltered = (int) f2;
            if (Float.compare(f, f2) >= 0) {
                this.mCurFrontLux = (int) this.mCurFrontFiltered;
                this.mCurBackLux = (int) this.mCurBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    int i = this.mCurFrontLux;
                    if (i < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = i;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    this.mFusedLux = this.mCurFrontLux;
                    int i2 = this.mCurFrontCct;
                    int i3 = this.mCurBackCct;
                    if (i2 <= i3) {
                        i2 = i3;
                    }
                    this.mFusedCct = i2;
                } else if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    return;
                } else {
                    return;
                }
            } else if (isFlashlightOn()) {
                this.mBackLuxFiltered = this.mFrontLuxFiltered;
                this.mCurFrontLux = (int) this.mCurFrontFiltered;
                this.mCurBackLux = (int) this.mCurBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    int i4 = this.mCurFrontLux;
                    if (i4 < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = i4;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    this.mFusedLux = this.mCurFrontLux;
                    int i5 = this.mCurFrontCct;
                    int i6 = this.mCurBackCct;
                    if (i5 <= i6) {
                        i5 = i6;
                    }
                    this.mFusedCct = i5;
                    if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): mFusedLux=" + this.mFusedLux + " mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux + " mFusedCct=" + this.mFusedCct + " mCurFrontCct=" + this.mCurFrontCct + " mCurBackCct=" + this.mCurBackCct);
                    }
                } else if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    return;
                } else {
                    return;
                }
            } else {
                float f3 = this.mCurBackFiltered;
                float f4 = this.mCurFrontFiltered;
                float darkRoomDelta3 = f3 - f4;
                if (f4 >= 0.0f && f4 <= this.mDarkRoomThresh) {
                    curDarkRoomDelta = this.mDarkRoomDelta;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh1) <= 0) {
                    float f5 = this.mCurFrontFiltered;
                    float f6 = this.mDarkRoomThresh;
                    float f7 = this.mDarkRoomDelta1;
                    float f8 = this.mDarkRoomDelta;
                    curDarkRoomDelta = (((f5 - f6) * (f7 - f8)) / (this.mDarkRoomThresh1 - f6)) + f8;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh2) < 0) {
                    float f9 = this.mCurFrontFiltered;
                    float f10 = this.mDarkRoomThresh1;
                    float f11 = this.mDarkRoomDelta2;
                    float f12 = this.mDarkRoomDelta1;
                    curDarkRoomDelta = (((f9 - f10) * (f11 - f12)) / (this.mDarkRoomThresh2 - f10)) + f12;
                } else if (Float.compare(this.mCurFrontFiltered, this.mDarkRoomThresh3) < 0) {
                    float f13 = this.mCurFrontFiltered;
                    float f14 = this.mDarkRoomThresh2;
                    float f15 = this.mDarkRoomDelta2;
                    curDarkRoomDelta = f15 + (((f13 - f14) * (darkRoomDelta3 - f15)) / (this.mDarkRoomThresh3 - f14));
                } else {
                    curDarkRoomDelta = darkRoomDelta3;
                }
                this.mBackFloorThresh = this.mCurFrontFiltered + curDarkRoomDelta;
                float f16 = this.mCurBackFiltered;
                float f17 = this.mBackFloorThresh;
                if (f16 >= f17) {
                    f16 = f17;
                }
                this.mCurBackFiltered = f16;
                this.mStaProb = (float) this.mStaProbLUT[this.mBackLuxStaProInd][this.mFrontLuxStaProInd];
                float f18 = this.mStaProb;
                if (f18 == 0.0f) {
                    this.mPreFrontFiltered = this.mCurFrontFiltered;
                    this.mPreBackFiltered = this.mCurBackFiltered;
                } else {
                    if (f18 == 20.0f) {
                        this.mModifyFactor = 0.02f;
                    } else if (f18 == 50.0f) {
                        this.mModifyFactor = 0.01f;
                    } else if (f18 == 90.0f) {
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
                    float f19 = this.mCurFrontFiltered;
                    float f20 = this.mPreFrontFiltered;
                    if (f19 < f20) {
                        this.mPreFrontFiltered = f20 - FrontAdjustment;
                    } else if (f19 > f20) {
                        this.mPreFrontFiltered = f20 + FrontAdjustment;
                    }
                    float f21 = this.mCurBackFiltered;
                    float f22 = this.mPreBackFiltered;
                    if (f21 < f22) {
                        this.mPreBackFiltered = f22 - BackAdjustment;
                    } else if (this.mCurFrontFiltered > this.mPreFrontFiltered) {
                        this.mPreBackFiltered = f22 + BackAdjustment;
                    }
                }
                this.mCurFrontLux = (int) this.mPreFrontFiltered;
                this.mCurBackLux = (int) this.mPreBackFiltered;
                if (this.mCurFrontLux >= 0 || this.mCurBackLux >= 0) {
                    int i7 = this.mCurFrontLux;
                    if (i7 < 0) {
                        this.mCurFrontLux = this.mCurBackLux;
                        this.mCurFrontCct = this.mCurBackCct;
                    } else if (this.mCurBackLux < 0) {
                        this.mCurBackLux = i7;
                        this.mCurBackCct = this.mCurFrontCct;
                    }
                    int i8 = this.mCurFrontLux;
                    int i9 = this.mCurBackLux;
                    if (i8 <= i9) {
                        i8 = i9;
                    }
                    this.mFusedLux = i8;
                    int i10 = this.mCurFrontCct;
                    int i11 = this.mCurBackCct;
                    if (i10 <= i11) {
                        i10 = i11;
                    }
                    this.mFusedCct = i10;
                } else if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): updateFusedData returned, mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux);
                    return;
                } else {
                    return;
                }
            }
            if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.updateFusedData(): mFusedLux=" + this.mFusedLux + " mCurFrontLux=" + this.mCurFrontLux + " mCurBackLux=" + this.mCurBackLux + " mFusedCct=" + this.mFusedCct + " mCurFrontCct=" + this.mCurFrontCct + " mCurBackCct=" + this.mCurBackCct);
            }
        }

        public void sendFusedData() {
            if (this.mFrontUpdated) {
                if (HwDualSensorEventListenerImpl.this.mBackSensorBypassCount <= 0 && !this.mBackUpdated) {
                    HwDualSensorEventListenerImpl.access$4008(HwDualSensorEventListenerImpl.this);
                    if (HwDualSensorEventListenerImpl.this.mBackTimeOutCount >= HwDualSensorEventListenerImpl.this.mBackSensorTimeOutTH) {
                        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.this;
                        int unused = hwDualSensorEventListenerImpl.mBackSensorBypassCount = hwDualSensorEventListenerImpl.mBackSensorBypassCountMax;
                        Slog.i(HwDualSensorEventListenerImpl.TAG, "mBackTimeOutCount start: " + HwDualSensorEventListenerImpl.this.mBackTimeOutCount);
                    } else if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                        Slog.d(HwDualSensorEventListenerImpl.TAG, "Skip this sendFusedData! mFrontUpdated=" + this.mFrontUpdated + " mBackUpdated=" + this.mBackUpdated);
                        return;
                    } else {
                        return;
                    }
                }
                int unused2 = HwDualSensorEventListenerImpl.this.mBackTimeOutCount = 0;
                updateFusedData();
                long lastValueSensorTime = this.mLastValueFrontSensorTime;
                long j = this.mLastValueBackSensorTime;
                if (lastValueSensorTime <= j) {
                    lastValueSensorTime = j;
                }
                long lastValueSystemTime = this.mFrontLastValueSystemTime;
                long j2 = this.mBackLastValueSystemTime;
                if (lastValueSystemTime <= j2) {
                    lastValueSystemTime = j2;
                }
                int i = this.mFusedLux;
                int i2 = this.mFusedCct;
                long[] data = {(long) i, (long) i2, lastValueSystemTime, lastValueSensorTime, (long) this.mFrontLuxFiltered, (long) this.mBackLuxFiltered};
                if (i < 0 || i2 < 0) {
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.sendFusedData(): skip. mFusedLux=" + this.mFusedLux + " mFusedCct=" + this.mFusedCct);
                    return;
                }
                if (HwDualSensorEventListenerImpl.DEBUG && this.mDebugPrintTime - HwDualSensorEventListenerImpl.this.mFusedEnableTime < 1000000000) {
                    Slog.d(HwDualSensorEventListenerImpl.TAG, "FusedSensorData.sendFusedData(): lux=" + this.mFusedLux + " cct=" + this.mFusedCct + " sensorTime=" + lastValueSensorTime + " FrontSensorTime=" + this.mLastValueFrontSensorTime + " BackSensorTime=" + this.mLastValueBackSensorTime);
                }
                setChanged();
                notifyObservers(data);
            } else if (HwDualSensorEventListenerImpl.DEBUG) {
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
            long j = this.mFrontLastValueSystemTime;
            long j2 = this.mBackLastValueSystemTime;
            return j > j2 ? j : j2;
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
            this.mCurFrontFiltered = 0.0f;
            this.mPreFrontFiltered = 0.0f;
            this.mCurBackFiltered = 0.0f;
            this.mPreBackFiltered = 0.0f;
            this.mFrontLuxIndex = 0;
            this.mBackLuxIndex = 0;
            this.mFrontLuxRelativeChange = 0.0f;
            this.mBackLuxRelativeChange = 0.0f;
            this.mFrontLuxStaProInd = 0;
            this.mBackLuxStaProInd = 0;
            this.mAdjustMin = 1.0f;
            this.mFrontUpdated = false;
            this.mBackUpdated = false;
            this.mFrontLuxArray = new int[]{-1, -1, -1};
            this.mBackLuxArray = new int[]{-1, -1, -1};
        }

        private void setStabilityProbabilityLUT() {
            if (HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT == null) {
                if (HwDualSensorEventListenerImpl.DEBUG) {
                    Slog.i(HwDualSensorEventListenerImpl.TAG, "STABILIZED_PROBABILITY_LUT is null");
                }
            } else if (HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT.length == 25) {
                for (int rowInd = 0; rowInd < 5; rowInd++) {
                    for (int colInd = 0; colInd < 5; colInd++) {
                        this.mStaProbLUT[rowInd][colInd] = HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT[(rowInd * 5) + colInd];
                    }
                }
            } else if (HwDualSensorEventListenerImpl.DEBUG) {
                Slog.i(HwDualSensorEventListenerImpl.TAG, "STABILIZED_PROBABILITY_LUT.length=" + HwDualSensorEventListenerImpl.STABILIZED_PROBABILITY_LUT.length);
            }
        }

        private boolean isFlashlightOn() {
            int access$4400 = HwDualSensorEventListenerImpl.mFlashlightDetectionMode;
            if (access$4400 == 0) {
                return this.mCurBackFiltered > this.mBackRoofThresh || this.mBackLuxDeviation > HwDualSensorEventListenerImpl.BACK_LUX_DEVIATION_THRESH;
            }
            if (access$4400 == 1) {
                if (HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs == -1 || SystemClock.uptimeMillis() - HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs < ((long) HwDualSensorEventListenerImpl.FLASHLIGHT_OFF_TIME_THRESHOLD_MS)) {
                    return true;
                }
                if (HwDualSensorEventListenerImpl.this.mFlashLightOffTimeStampMs == -2) {
                    return this.mCurBackFiltered > this.mBackRoofThresh || this.mBackLuxDeviation > HwDualSensorEventListenerImpl.BACK_LUX_DEVIATION_THRESH;
                }
            }
            return false;
        }
    }

    private double[] getNirCompensationIfNeeded(SensorEvent raw) {
        if (!this.mNeedNirCompensation) {
            return new double[]{(double) raw.values[0], (double) raw.values[1], (double) raw.values[2], (double) raw.values[3], (double) raw.values[4], (double) raw.values[5], (double) raw.values[6], (double) raw.values[7], (double) raw.values[8], (double) raw.values[9]};
        }
        double nirClear = ((double) raw.values[9]) - ((((((((((double) raw.values[0]) / this.mScaling[0]) + (((double) raw.values[1]) / this.mScaling[1])) + (((double) raw.values[2]) / this.mScaling[2])) + (((double) raw.values[3]) / this.mScaling[3])) + (((double) raw.values[4]) / this.mScaling[4])) + (((double) raw.values[5]) / this.mScaling[5])) + (((double) raw.values[6]) / this.mScaling[6])) + (((double) raw.values[7]) / this.mScaling[7]));
        double[] out = new double[10];
        for (int i = 0; i < 8; i++) {
            out[i] = ((double) raw.values[i]) - ((((double) ((raw.values[8] * SPEC_NIR_WRIGHT) / raw.values[9])) * this.mRatioNir[i]) * nirClear);
        }
        out[8] = (double) raw.values[8];
        out[9] = (double) raw.values[9];
        return out;
    }

    private double getPolyVal(double[] coef, double lux) {
        return (coef[0] * lux * lux) + (coef[1] * lux) + coef[2];
    }

    /* access modifiers changed from: private */
    public int[] convertSpectrumToLux(SensorEvent event) {
        double coefImp;
        int[] retArr = {0, 0};
        if (event.values[9] == 0.0f) {
            return retArr;
        }
        double[] raw = getNirCompensationIfNeeded(event);
        double[] dArr = this.mSpectrumParam;
        double luxValue = ((((((((((((dArr[0] * raw[0]) + (dArr[1] * raw[1])) + (dArr[2] * raw[2])) + (dArr[3] * raw[3])) + (dArr[4] * raw[4])) + (dArr[5] * raw[5])) + (dArr[6] * raw[6])) + (dArr[7] * raw[7])) + (dArr[8] * raw[8])) + (dArr[9] * raw[9])) / this.mSpectrumGain) / this.mSpectrumTime) * ((double) this.mLuxScale);
        if (luxValue < 15.0d) {
            coefImp = getPolyVal(this.mPolyCoefs1, luxValue);
        } else if (luxValue >= 15.0d && luxValue < 75.0d) {
            coefImp = getPolyVal(this.mPolyCoefs2, luxValue);
        } else if (luxValue < 75.0d || luxValue >= 110.0d) {
            coefImp = 1.0d;
        } else {
            coefImp = getPolyVal(this.mPolyCoefs3, luxValue);
        }
        int lux = (int) (0.5d + (luxValue * coefImp));
        if (lux < 0) {
            lux = 0;
        }
        retArr[0] = lux;
        return retArr;
    }

    /* access modifiers changed from: private */
    public int[] convertRGBCW2LuxAndCct(Float Craw, Float Rraw, Float Graw, Float Braw, Float Wraw) {
        float Z;
        float Y;
        float X;
        float lux_f;
        if (Float.compare(ATIME_RGBCW * AGAIN_RGBCW, 0.0f) == 0) {
            Slog.d(TAG, " ATIME_RGBCW=" + ATIME_RGBCW + " AGAIN_RGBCW=" + AGAIN_RGBCW);
            return new int[]{0, 0};
        }
        float Cnorm = ((PRODUCTION_CALIBRATION_C * 12800.0f) * Craw.floatValue()) / (ATIME_RGBCW * AGAIN_RGBCW);
        float Rnorm = ((PRODUCTION_CALIBRATION_R * 12800.0f) * Rraw.floatValue()) / (ATIME_RGBCW * AGAIN_RGBCW);
        float Gnorm = ((PRODUCTION_CALIBRATION_G * 12800.0f) * Graw.floatValue()) / (ATIME_RGBCW * AGAIN_RGBCW);
        float Bnorm = ((PRODUCTION_CALIBRATION_B * 12800.0f) * Braw.floatValue()) / (ATIME_RGBCW * AGAIN_RGBCW);
        float Wnorm = ((PRODUCTION_CALIBRATION_W * 12800.0f) * Wraw.floatValue()) / (ATIME_RGBCW * AGAIN_RGBCW);
        int lux = 0;
        int cct = 0;
        if (Float.compare(Wnorm, 0.0f) != 0) {
            if (((Wnorm * 2.6f) - Cnorm) / (2.6f * Wnorm) > IR_BOUNDRY_RGBCW) {
                lux_f = (LUX_COEF_HIGH_1 * Cnorm) + (LUX_COEF_HIGH_2 * Rnorm) + (LUX_COEF_HIGH_3 * Gnorm) + (LUX_COEF_HIGH_4 * Bnorm) + (LUX_COEF_HIGH_5 * Wnorm);
                X = (COLOR_MAT_RGBCW_HIGH_11 * Cnorm) + (COLOR_MAT_RGBCW_HIGH_12 * Rnorm) + (COLOR_MAT_RGBCW_HIGH_13 * Gnorm) + (COLOR_MAT_RGBCW_HIGH_14 * Bnorm) + (COLOR_MAT_RGBCW_HIGH_15 * Wnorm);
                Y = (COLOR_MAT_RGBCW_HIGH_21 * Cnorm) + (COLOR_MAT_RGBCW_HIGH_22 * Rnorm) + (COLOR_MAT_RGBCW_HIGH_23 * Gnorm) + (COLOR_MAT_RGBCW_HIGH_24 * Bnorm) + (COLOR_MAT_RGBCW_HIGH_25 * Wnorm);
                Z = (COLOR_MAT_RGBCW_HIGH_31 * Cnorm) + (COLOR_MAT_RGBCW_HIGH_32 * Rnorm) + (COLOR_MAT_RGBCW_HIGH_33 * Gnorm) + (COLOR_MAT_RGBCW_HIGH_34 * Bnorm) + (COLOR_MAT_RGBCW_HIGH_35 * Wnorm);
            } else {
                lux_f = (LUX_COEF_LOW_1 * Cnorm) + (LUX_COEF_LOW_2 * Rnorm) + (LUX_COEF_LOW_3 * Gnorm) + (LUX_COEF_LOW_4 * Bnorm) + (LUX_COEF_LOW_5 * Wnorm);
                X = (COLOR_MAT_RGBCW_LOW_11 * Cnorm) + (COLOR_MAT_RGBCW_LOW_12 * Rnorm) + (COLOR_MAT_RGBCW_LOW_13 * Gnorm) + (COLOR_MAT_RGBCW_LOW_14 * Bnorm) + (COLOR_MAT_RGBCW_LOW_15 * Wnorm);
                Y = (COLOR_MAT_RGBCW_LOW_21 * Cnorm) + (COLOR_MAT_RGBCW_LOW_22 * Rnorm) + (COLOR_MAT_RGBCW_LOW_23 * Gnorm) + (COLOR_MAT_RGBCW_LOW_24 * Bnorm) + (COLOR_MAT_RGBCW_LOW_25 * Wnorm);
                Z = (COLOR_MAT_RGBCW_LOW_31 * Cnorm) + (COLOR_MAT_RGBCW_LOW_32 * Rnorm) + (COLOR_MAT_RGBCW_LOW_33 * Gnorm) + (COLOR_MAT_RGBCW_LOW_34 * Bnorm) + (COLOR_MAT_RGBCW_LOW_35 * Wnorm);
            }
            float XYZ = X + Y + Z;
            if (Float.compare(XYZ, 0.0f) != 0) {
                float x = X / XYZ;
                float y = Y / XYZ;
                if (Float.compare(y - 0.1858f, 0.0f) != 0) {
                    float tempvalue = (1.0f / (y - 0.1858f)) * (x - 0.332f);
                    cct = (int) ((((((-499.0f * tempvalue) * tempvalue) * tempvalue) + ((3525.0f * tempvalue) * tempvalue)) - (6823.3f * tempvalue)) + 5520.33f + 0.5f);
                }
            }
            lux = (int) (lux_f + 0.5f);
            if (lux < 0) {
                lux = 0;
            }
        }
        return new int[]{lux, cct};
    }

    /* access modifiers changed from: private */
    public int[] convertXYZ2LuxAndCct(Float Xraw, Float Yraw, Float Zraw, Float IRraw) {
        float Z;
        float Y;
        float lux_f;
        float X;
        float f;
        float Xnorm = ((PRODUCTION_CALIBRATION_X * 1600.128f) * Xraw.floatValue()) / (ATIME * AGAIN);
        float Ynorm = ((PRODUCTION_CALIBRATION_Y * 1600.128f) * Yraw.floatValue()) / (ATIME * AGAIN);
        float Znorm = ((PRODUCTION_CALIBRATION_Z * 1600.128f) * Zraw.floatValue()) / (ATIME * AGAIN);
        float IR1norm = ((PRODUCTION_CALIBRATION_IR1 * 1600.128f) * IRraw.floatValue()) / (ATIME * AGAIN);
        int lux = 0;
        int cct = 0;
        if (Float.compare(Ynorm, 0.0f) != 0) {
            if (IR1norm / Ynorm > IR_BOUNDRY) {
                lux_f = (LUX_COEF_HIGH_X * Xnorm) + (LUX_COEF_HIGH_Y * Ynorm) + (LUX_COEF_HIGH_Z * Znorm) + (LUX_COEF_HIGH_IR * IR1norm) + LUX_COEF_HIGH_OFFSET;
                X = (COLOR_MAT_HIGH_11 * Xnorm) + (COLOR_MAT_HIGH_12 * Ynorm) + (COLOR_MAT_HIGH_13 * Znorm) + (COLOR_MAT_HIGH_14 * IR1norm);
                Y = (COLOR_MAT_HIGH_21 * Xnorm) + (COLOR_MAT_HIGH_22 * Ynorm) + (COLOR_MAT_HIGH_23 * Znorm) + (COLOR_MAT_HIGH_24 * IR1norm);
                Z = (COLOR_MAT_HIGH_31 * Xnorm) + (COLOR_MAT_HIGH_32 * Ynorm) + (COLOR_MAT_HIGH_33 * Znorm) + (COLOR_MAT_HIGH_34 * IR1norm);
            } else {
                lux_f = (LUX_COEF_LOW_X * Xnorm) + (LUX_COEF_LOW_Y * Ynorm) + (LUX_COEF_LOW_Z * Znorm) + (LUX_COEF_LOW_IR * IR1norm) + LUX_COEF_LOW_OFFSET;
                X = (COLOR_MAT_LOW_11 * Xnorm) + (COLOR_MAT_LOW_12 * Ynorm) + (COLOR_MAT_LOW_13 * Znorm) + (COLOR_MAT_LOW_14 * IR1norm);
                Y = (COLOR_MAT_LOW_21 * Xnorm) + (COLOR_MAT_LOW_22 * Ynorm) + (COLOR_MAT_LOW_23 * Znorm) + (COLOR_MAT_LOW_24 * IR1norm);
                Z = (COLOR_MAT_LOW_31 * Xnorm) + (COLOR_MAT_LOW_32 * Ynorm) + (COLOR_MAT_LOW_33 * Znorm) + (COLOR_MAT_LOW_34 * IR1norm);
            }
            float XYZ = X + Y + Z;
            if (Float.compare(XYZ, 0.0f) != 0) {
                float x = X / XYZ;
                float y = Y / XYZ;
                if (Float.compare(y - 0.1858f, 0.0f) != 0) {
                    float tempvalue = (1.0f / (y - 0.1858f)) * (x - 0.332f);
                    f = 0.5f;
                    cct = (int) ((((((-499.0f * tempvalue) * tempvalue) * tempvalue) + ((3525.0f * tempvalue) * tempvalue)) - (6823.3f * tempvalue)) + 5520.33f + 0.5f);
                } else {
                    f = 0.5f;
                }
            } else {
                f = 0.5f;
            }
            lux = (int) (lux_f + f);
        }
        return new int[]{lux, cct};
    }

    /* access modifiers changed from: private */
    public int[] convertRGB2LuxAndCct(float red, float green, float blue, float IR) {
        float lux_f;
        float lux_f2;
        float cct_f = 0.0f;
        if (Float.compare(green, 0.0f) <= 0) {
            return new int[]{0, 0};
        }
        if (Float.compare(IR / green, 0.5f) > 0) {
            lux_f = (LUX_COEF_HIGH_RED * red) + (LUX_COEF_HIGH_GREEN * green) + (LUX_COEF_HIGH_BLUE * blue);
        } else {
            lux_f = (LUX_COEF_LOW_RED * red) + (LUX_COEF_LOW_GREEN * green) + (LUX_COEF_LOW_BLUE * blue);
        }
        float rgbSum = red + green + blue;
        if (Float.compare(rgbSum, 0.0f) <= 0) {
            if (DEBUG) {
                Slog.d(TAG, "convertRGB2LuxAndCct: Illegal sensor value, rgbSum <= 0!");
            }
            return new int[]{0, 0};
        }
        float redRatio = red / rgbSum;
        float greenRatio = green / rgbSum;
        float x = X_CONSTANT + (X_R_COEF * redRatio) + (X_R_QUADRATIC_COEF * redRatio * redRatio) + (X_G_COEF * greenRatio) + (X_RG_COEF * greenRatio * redRatio) + (X_G_QUADRATIC_COEF * greenRatio * greenRatio);
        float y = Y_CONSTANT + (Y_R_COEF * redRatio) + (Y_R_QUADRATIC_COEF * redRatio * redRatio) + (Y_G_COEF * greenRatio) + (Y_RG_COEF * redRatio * greenRatio) + (Y_G_QUADRATIC_COEF * greenRatio * greenRatio);
        if (Float.compare(y, 0.1858f) >= 0) {
            float n = (x - 0.332f) / (y - 0.1858f);
            lux_f2 = lux_f;
            float cct_f2 = (((((float) Math.pow((double) n, 3.0d)) * -449.0f) + (((float) Math.pow((double) n, 2.0d)) * 3525.0f)) - (6823.3f * n)) + 5520.33f;
            float cct_f3 = 13000.0f;
            if (Float.compare(cct_f2, 13000.0f) < 0) {
                cct_f3 = cct_f2;
            }
            cct_f = 0.0f;
            if (Float.compare(cct_f3, 0.0f) > 0) {
                cct_f = cct_f3;
            }
        } else {
            lux_f2 = lux_f;
        }
        return new int[]{(int) (lux_f2 + 0.5f), (int) (cct_f + 0.5f)};
    }

    /* access modifiers changed from: private */
    public int[] convertNormalizedType2LuxAndCct(float rawLux, float rawCct) {
        int[] retArr = {(int) ((rawLux / 100.0f) + 0.5f), (int) (0.5f + rawCct)};
        if (retArr[0] < 0) {
            retArr[0] = 0;
        }
        if (retArr[1] < 0) {
            retArr[1] = 0;
        }
        return retArr;
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
                int unused = HwDualSensorEventListenerImpl.this.mTimer = newTimer;
            } else if (newTimer < HwDualSensorEventListenerImpl.this.mTimer) {
                int unused2 = HwDualSensorEventListenerImpl.this.mTimer = newTimer;
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
                int timer2 = timer - HwDualSensorEventListenerImpl.this.mTimer;
                if (timer2 < 0) {
                    timer2 = 0;
                }
                if (timer2 == 0) {
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
            int unused = HwDualSensorEventListenerImpl.this.mTimer = timer;
            queueMessage();
        }

        private void queueMessage() {
            this.mTimestamp = SystemClock.uptimeMillis();
            removeMessages(0);
            sendEmptyMessageDelayed(0, (long) HwDualSensorEventListenerImpl.this.mTimer);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                processTimer(HwDualSensorEventListenerImpl.this.mFrontSender);
                processTimer(HwDualSensorEventListenerImpl.this.mBackSender);
                processTimer(HwDualSensorEventListenerImpl.this.mFusedSender);
                int unused = HwDualSensorEventListenerImpl.this.mTimer = -1;
                updateTimer(HwDualSensorEventListenerImpl.this.mFrontTimer);
                updateTimer(HwDualSensorEventListenerImpl.this.mBackTimer);
                updateTimer(HwDualSensorEventListenerImpl.this.mFusedTimer);
                if (HwDualSensorEventListenerImpl.this.mTimer != -1) {
                    queueMessage();
                }
            } else if (i == 1) {
                processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mFrontSender);
            } else if (i == 2) {
                processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mBackSender);
            } else if (i != 3) {
                Slog.i(HwDualSensorEventListenerImpl.TAG, "handleMessage: Invalid message");
            } else {
                processTimerAsTrigger(HwDualSensorEventListenerImpl.this.mFusedSender);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:0x0086  */
    private boolean parseParameters() {
        StringBuilder sb;
        boolean parseResult = false;
        parseResult = false;
        parseResult = false;
        parseResult = false;
        parseResult = false;
        parseResult = false;
        String xmlPath = getXmlPath();
        if (xmlPath == null) {
            return false;
        }
        if (DEBUG) {
            Slog.d(TAG, "parse dual sensor parameters from xmlPath: " + xmlPath);
        }
        FileInputStream inputStream = null;
        inputStream = null;
        inputStream = null;
        try {
            inputStream = new FileInputStream(xmlPath);
            parseResult = parseParametersFromXML(inputStream);
            try {
                inputStream.close();
            } catch (IOException e) {
                e = e;
                sb = new StringBuilder();
            }
        } catch (FileNotFoundException e2) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    e = e3;
                    sb = new StringBuilder();
                }
            }
        } catch (Exception e4) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                    e = e5;
                    sb = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e6) {
                    Slog.i(TAG, "input stream close uncorrectly: " + e6);
                }
            }
            throw th;
        }
        if (DEBUG) {
            Slog.d(TAG, "parseResult=" + parseResult);
        }
        printParameters();
        return parseResult;
        sb.append("input stream close uncorrectly: ");
        sb.append(e);
        Slog.i(TAG, sb.toString());
        if (DEBUG) {
        }
        printParameters();
        return parseResult;
    }

    private String getXmlPath() {
        File xmlFile = HwCfgFilePolicy.getCfgFile(DUAL_SENSOR_XML_PATH, 0);
        if (xmlFile == null) {
            Slog.w(TAG, "get xmlFile :/xml/lcd/DualSensorConfig.xml failed!");
            return null;
        }
        try {
            return xmlFile.getCanonicalPath();
        } catch (IOException e) {
            Slog.e(TAG, "get xmlCanonicalPath error IOException!");
            return null;
        }
    }

    private boolean parseParametersFromXML(InputStream inStream) {
        boolean backSensorParamLoadStarted = false;
        boolean moduleSensorOptionsLoadStarted = false;
        boolean algorithmParametersLoadStarted = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    String nodeName = parser.getName();
                    if (nodeName.equals("BackSensorParameters")) {
                        backSensorParamLoadStarted = true;
                    } else if (backSensorParamLoadStarted && nodeName.equals("IR_BOUNDRY_RGBCW")) {
                        IR_BOUNDRY_RGBCW = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxrgbcwCoefficientsLow")) {
                        parsergbcwLuxCoefficients(parser.nextText(), true);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ColorrgbcwMatrixLow")) {
                        parsergbcwColorMatrix(parser.nextText(), true);
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxrgbcwCoefficientsHigh")) {
                        parsergbcwLuxCoefficients(parser.nextText(), false);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ColorrgbcwMatrixHigh")) {
                        parsergbcwColorMatrix(parser.nextText(), false);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ATime_rgbcw")) {
                        ATIME_RGBCW = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("AGain_rgbcw")) {
                        AGAIN_RGBCW = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationR")) {
                        PRODUCTION_CALIBRATION_R = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationG")) {
                        PRODUCTION_CALIBRATION_G = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationB")) {
                        PRODUCTION_CALIBRATION_B = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationC")) {
                        PRODUCTION_CALIBRATION_C = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationW")) {
                        PRODUCTION_CALIBRATION_W = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("IRBoundry")) {
                        IR_BOUNDRY = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefficientsLow")) {
                        parseLuxCoefficients(parser.nextText(), true);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ColorMatrixLow")) {
                        parseColorMatrix(parser.nextText(), true);
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefficientsHigh")) {
                        parseLuxCoefficients(parser.nextText(), false);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ColorMatrixHigh")) {
                        parseColorMatrix(parser.nextText(), false);
                    } else if (backSensorParamLoadStarted && nodeName.equals("ATime")) {
                        ATIME = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("AGain")) {
                        AGAIN = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxScale")) {
                        this.mLuxScale = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationX")) {
                        PRODUCTION_CALIBRATION_X = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationY")) {
                        PRODUCTION_CALIBRATION_Y = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationZ")) {
                        PRODUCTION_CALIBRATION_Z = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("ProductionCalibrationIR1")) {
                        PRODUCTION_CALIBRATION_IR1 = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("FrontSensorRateMillis")) {
                        mFrontRateMillis = Integer.parseInt(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("BackSensorRateMillis")) {
                        mBackRateMillis = Integer.parseInt(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("FusedSensorRateMillis")) {
                        mFusedRateMillis = Integer.parseInt(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SensorVersion")) {
                        mSensorVersion = Integer.parseInt(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("RatioIRGreen")) {
                        RATIO_IR_GREEN = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefHighRed")) {
                        LUX_COEF_HIGH_RED = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefHighGreen")) {
                        LUX_COEF_HIGH_GREEN = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefHighBlue")) {
                        LUX_COEF_HIGH_BLUE = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefLowRed")) {
                        LUX_COEF_LOW_RED = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefLowGreen")) {
                        LUX_COEF_LOW_GREEN = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("LuxCoefLowBlue")) {
                        LUX_COEF_LOW_BLUE = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XConstant")) {
                        X_CONSTANT = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XRCoef")) {
                        X_R_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XRQuadCoef")) {
                        X_R_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XGCoef")) {
                        X_G_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XRGCoef")) {
                        X_RG_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("XGQuadCoef")) {
                        X_G_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YConstant")) {
                        Y_CONSTANT = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YRCoef")) {
                        Y_R_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YRQuadCoef")) {
                        Y_R_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YGCoef")) {
                        Y_G_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YRGCoef")) {
                        Y_RG_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("YGQuadCoef")) {
                        Y_G_QUADRATIC_COEF = Float.parseFloat(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumLuxPram")) {
                        parseSpectrumLuxCoefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumScalingPram")) {
                        parseSpectrumScalingCoefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumNirPram")) {
                        parseSpectrumNirCoefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumPoly1Pram")) {
                        parseSpectrumPoly1Coefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumPoly2Pram")) {
                        parseSpectrumPoly2Coefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumPoly3Pram")) {
                        parseSpectrumPoly3Coefficients(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumGain")) {
                        this.mSpectrumGain = Double.parseDouble(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("SpectrumTime")) {
                        this.mSpectrumTime = Double.parseDouble(parser.nextText());
                    } else if (backSensorParamLoadStarted && nodeName.equals("NeedNirCompensation")) {
                        this.mNeedNirCompensation = Boolean.parseBoolean(parser.nextText());
                    } else if (nodeName.equals("ModuleSensorOptions")) {
                        moduleSensorOptionsLoadStarted = true;
                    } else if (moduleSensorOptionsLoadStarted) {
                        mModuleSensorMap.put(nodeName, Integer.valueOf(Integer.parseInt(parser.nextText())));
                    } else if (nodeName.equals("AlgorithmParameters")) {
                        algorithmParametersLoadStarted = true;
                    } else if (algorithmParametersLoadStarted && nodeName.equals("FrontSensorStabilityThresholdList")) {
                        parseSensorStabilityThresholdList(parser.nextText(), true);
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackSensorStabilityThresholdList")) {
                        parseSensorStabilityThresholdList(parser.nextText(), false);
                    } else if (algorithmParametersLoadStarted && nodeName.equals("StabilizedProbabilityLUT")) {
                        parseStabilizedProbabilityLUT(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackRoofThresh")) {
                        BACK_ROOF_THRESH = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackFloorThresh")) {
                        BACK_FLOOR_THRESH = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomThresh")) {
                        DARK_ROOM_THRESH = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomDelta")) {
                        DARK_ROOM_DELTA = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomDelta1")) {
                        DARK_ROOM_DELTA1 = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomDelta2")) {
                        DARK_ROOM_DELTA2 = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomThresh1")) {
                        DARK_ROOM_THRESH1 = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomThresh2")) {
                        DARK_ROOM_THRESH2 = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("DarkRoomThresh3")) {
                        DARK_ROOM_THRESH3 = Float.parseFloat(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackLuxDeviationThresh")) {
                        BACK_LUX_DEVIATION_THRESH = Long.parseLong(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("FlashlightDetectionMode")) {
                        mFlashlightDetectionMode = Integer.parseInt(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("FlashlightOffTimeThresholdMs")) {
                        FLASHLIGHT_OFF_TIME_THRESHOLD_MS = Integer.parseInt(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackSensorTimeOutTH")) {
                        this.mBackSensorTimeOutTH = Integer.parseInt(parser.nextText());
                    } else if (algorithmParametersLoadStarted && nodeName.equals("BackSensorBypassCountMax")) {
                        this.mBackSensorBypassCountMax = Integer.parseInt(parser.nextText());
                    } else if (!algorithmParametersLoadStarted || !nodeName.equals("IsFilterOn")) {
                        Slog.d(TAG, "nothing to do");
                    } else {
                        this.mIsFilterOn = Boolean.parseBoolean(parser.nextText());
                    }
                } else if (eventType == 3) {
                    String nodeName2 = parser.getName();
                    if (nodeName2.equals("BackSensorParameters")) {
                        backSensorParamLoadStarted = false;
                    } else if (nodeName2.equals("ModuleSensorOptions")) {
                        moduleSensorOptionsLoadStarted = false;
                    } else if (nodeName2.equals("AlgorithmParameters")) {
                        algorithmParametersLoadStarted = false;
                    }
                }
            }
            return true;
        } catch (XmlPullParserException e) {
            Slog.i(TAG, "XmlPullParserException: " + e);
            return false;
        } catch (IOException e2) {
            Slog.i(TAG, "IOException: " + e2);
            return false;
        } catch (NumberFormatException e3) {
            Slog.i(TAG, "NumberFormatException: " + e3);
            return false;
        } catch (RuntimeException e4) {
            Slog.i(TAG, "RuntimeException: " + e4);
            return false;
        } catch (Exception e5) {
            Slog.w(TAG, "Exception!");
            return false;
        }
    }

    private boolean parsergbcwLuxCoefficients(String nodeContent, boolean isLow) {
        if (nodeContent == null) {
            Slog.e(TAG, "nodeContent is null");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 5);
        if (splitedContent == null) {
            Slog.e(TAG, "splitedContent is null");
            return false;
        } else if (splitedContent.length != 5) {
            Slog.e(TAG, "splitedContent.length=" + splitedContent.length);
            return false;
        } else {
            if (isLow) {
                LUX_COEF_LOW_1 = Float.parseFloat(splitedContent[0]);
                LUX_COEF_LOW_2 = Float.parseFloat(splitedContent[1]);
                LUX_COEF_LOW_3 = Float.parseFloat(splitedContent[2]);
                LUX_COEF_LOW_4 = Float.parseFloat(splitedContent[3]);
                LUX_COEF_LOW_5 = Float.parseFloat(splitedContent[4]);
            } else {
                LUX_COEF_HIGH_1 = Float.parseFloat(splitedContent[0]);
                LUX_COEF_HIGH_2 = Float.parseFloat(splitedContent[1]);
                LUX_COEF_HIGH_3 = Float.parseFloat(splitedContent[2]);
                LUX_COEF_HIGH_4 = Float.parseFloat(splitedContent[3]);
                LUX_COEF_HIGH_5 = Float.parseFloat(splitedContent[4]);
            }
            return true;
        }
    }

    private boolean parsergbcwColorMatrix(String nodeContent, boolean isLow) {
        if (nodeContent == null) {
            Slog.e(TAG, "nodeContent is null");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 15);
        if (splitedContent == null) {
            Slog.e(TAG, "splitedContent is null");
            return false;
        } else if (splitedContent.length != 15) {
            Slog.e(TAG, "splitedContent.length=" + splitedContent.length);
            return false;
        } else {
            if (isLow) {
                COLOR_MAT_RGBCW_LOW_11 = Float.parseFloat(splitedContent[0]);
                COLOR_MAT_RGBCW_LOW_12 = Float.parseFloat(splitedContent[1]);
                COLOR_MAT_RGBCW_LOW_13 = Float.parseFloat(splitedContent[2]);
                COLOR_MAT_RGBCW_LOW_14 = Float.parseFloat(splitedContent[3]);
                COLOR_MAT_RGBCW_LOW_15 = Float.parseFloat(splitedContent[4]);
                COLOR_MAT_RGBCW_LOW_21 = Float.parseFloat(splitedContent[5]);
                COLOR_MAT_RGBCW_LOW_22 = Float.parseFloat(splitedContent[6]);
                COLOR_MAT_RGBCW_LOW_23 = Float.parseFloat(splitedContent[7]);
                COLOR_MAT_RGBCW_LOW_24 = Float.parseFloat(splitedContent[8]);
                COLOR_MAT_RGBCW_LOW_25 = Float.parseFloat(splitedContent[9]);
                COLOR_MAT_RGBCW_LOW_31 = Float.parseFloat(splitedContent[10]);
                COLOR_MAT_RGBCW_LOW_32 = Float.parseFloat(splitedContent[11]);
                COLOR_MAT_RGBCW_LOW_33 = Float.parseFloat(splitedContent[12]);
                COLOR_MAT_RGBCW_LOW_34 = Float.parseFloat(splitedContent[13]);
                COLOR_MAT_RGBCW_LOW_35 = Float.parseFloat(splitedContent[14]);
            } else {
                COLOR_MAT_RGBCW_HIGH_11 = Float.parseFloat(splitedContent[0]);
                COLOR_MAT_RGBCW_HIGH_12 = Float.parseFloat(splitedContent[1]);
                COLOR_MAT_RGBCW_HIGH_13 = Float.parseFloat(splitedContent[2]);
                COLOR_MAT_RGBCW_HIGH_14 = Float.parseFloat(splitedContent[3]);
                COLOR_MAT_RGBCW_HIGH_15 = Float.parseFloat(splitedContent[4]);
                COLOR_MAT_RGBCW_HIGH_21 = Float.parseFloat(splitedContent[5]);
                COLOR_MAT_RGBCW_HIGH_22 = Float.parseFloat(splitedContent[6]);
                COLOR_MAT_RGBCW_HIGH_23 = Float.parseFloat(splitedContent[7]);
                COLOR_MAT_RGBCW_HIGH_24 = Float.parseFloat(splitedContent[8]);
                COLOR_MAT_RGBCW_HIGH_25 = Float.parseFloat(splitedContent[9]);
                COLOR_MAT_RGBCW_HIGH_31 = Float.parseFloat(splitedContent[10]);
                COLOR_MAT_RGBCW_HIGH_32 = Float.parseFloat(splitedContent[11]);
                COLOR_MAT_RGBCW_HIGH_33 = Float.parseFloat(splitedContent[12]);
                COLOR_MAT_RGBCW_HIGH_34 = Float.parseFloat(splitedContent[13]);
                COLOR_MAT_RGBCW_HIGH_35 = Float.parseFloat(splitedContent[14]);
            }
            return true;
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

    private double[] parseDouble(String content, int num) {
        String[] splitedContent = content.split(",", num);
        double[] out = new double[num];
        for (int i = 0; i < num; i++) {
            out[i] = Double.parseDouble(splitedContent[i]);
        }
        return out;
    }

    private boolean parseSpectrumLuxCoefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mSpectrumParam = parseDouble(nodeContent, 10);
        Slog.i(TAG, "mSpectrumParam = " + Arrays.toString(this.mSpectrumParam));
        return true;
    }

    private boolean parseSpectrumScalingCoefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mScaling = parseDouble(nodeContent, 8);
        Slog.i(TAG, "mScaling = " + Arrays.toString(this.mScaling));
        return true;
    }

    private boolean parseSpectrumNirCoefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mRatioNir = parseDouble(nodeContent, 8);
        Slog.i(TAG, "mRatioNir = " + Arrays.toString(this.mRatioNir));
        return true;
    }

    private boolean parseSpectrumPoly1Coefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mPolyCoefs1 = parseDouble(nodeContent, 3);
        Slog.i(TAG, "mPolyCoefs1 = " + Arrays.toString(this.mPolyCoefs1));
        return true;
    }

    private boolean parseSpectrumPoly2Coefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mPolyCoefs2 = parseDouble(nodeContent, 3);
        Slog.i(TAG, "mPolyCoefs2 = " + Arrays.toString(this.mPolyCoefs2));
        return true;
    }

    private boolean parseSpectrumPoly3Coefficients(String nodeContent) {
        if (nodeContent == null) {
            return false;
        }
        this.mPolyCoefs3 = parseDouble(nodeContent, 3);
        Slog.i(TAG, "mPolyCoefs3 = " + Arrays.toString(this.mPolyCoefs3));
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
        Hashtable<String, Integer> hashtable = mModuleSensorMap;
        if (hashtable == null || !hashtable.containsKey(moduleName)) {
            Slog.i(TAG, moduleName + " getModuleSensorOption=" + -1);
            return -1;
        }
        Slog.i(TAG, moduleName + " getModuleSensorOption=" + mModuleSensorMap.get(moduleName));
        return mModuleSensorMap.get(moduleName).intValue();
    }

    private boolean parseSensorStabilityThresholdList(String nodeContent, boolean isFrontSensor) {
        if (nodeContent == null) {
            Slog.i(TAG, "parseSensorStabilityThresholdList nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 4);
        int[] parsedArray = new int[4];
        int index = 0;
        while (index < 4) {
            try {
                parsedArray[index] = Integer.parseInt(splitedContent[index]);
                index++;
            } catch (NumberFormatException e) {
                Slog.i(TAG, "NumberFormatException: " + e);
                return false;
            }
        }
        if (isFrontSensor) {
            FRONT_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
            return true;
        }
        BACK_SENSOR_STABILITY_THRESHOLD = Arrays.copyOf(parsedArray, parsedArray.length);
        return true;
    }

    private boolean parseStabilizedProbabilityLUT(String nodeContent) {
        if (nodeContent == null) {
            Slog.d(TAG, "parseStabilizedProbabilityLUT nodeContent is null.");
            return false;
        }
        String[] splitedContent = nodeContent.split(",", 25);
        int[] parsedArray = new int[25];
        int index = 0;
        while (index < 25) {
            try {
                parsedArray[index] = Integer.parseInt(splitedContent[index]);
                index++;
            } catch (NumberFormatException e) {
                Slog.i(TAG, "NumberFormatException: " + e);
                return false;
            }
        }
        STABILIZED_PROBABILITY_LUT = Arrays.copyOf(parsedArray, parsedArray.length);
        return true;
    }

    private void printParameters() {
        if (DEBUG) {
            int i = mSensorVersion;
            if (i == 1) {
                Slog.d(TAG, "mSensorVersion=" + mSensorVersion + " IR_BOUNDRY=" + IR_BOUNDRY);
                Slog.d(TAG, "LUX_COEF_LOW_X=" + LUX_COEF_LOW_X + " LUX_COEF_LOW_Y=" + LUX_COEF_LOW_Y + " LUX_COEF_LOW_Z=" + LUX_COEF_LOW_Z + " LUX_COEF_LOW_IR=" + LUX_COEF_LOW_IR + " LUX_COEF_LOW_OFFSET=" + LUX_COEF_LOW_OFFSET);
                Slog.d(TAG, "LUX_COEF_HIGH_X=" + LUX_COEF_HIGH_X + " LUX_COEF_HIGH_Y=" + LUX_COEF_HIGH_Y + " LUX_COEF_HIGH_Z=" + LUX_COEF_HIGH_Z + " LUX_COEF_HIGH_IR=" + LUX_COEF_HIGH_IR + " LUX_COEF_HIGH_OFFSET=" + LUX_COEF_HIGH_OFFSET);
                Slog.d(TAG, "COLOR_MAT_LOW_11=" + COLOR_MAT_LOW_11 + " COLOR_MAT_LOW_12=" + COLOR_MAT_LOW_12 + " COLOR_MAT_LOW_13=" + COLOR_MAT_LOW_13 + " COLOR_MAT_LOW_14=" + COLOR_MAT_LOW_14 + " COLOR_MAT_LOW_21=" + COLOR_MAT_LOW_21 + " COLOR_MAT_LOW_22=" + COLOR_MAT_LOW_22 + " COLOR_MAT_LOW_23=" + COLOR_MAT_LOW_23 + " COLOR_MAT_LOW_24=" + COLOR_MAT_LOW_24 + " COLOR_MAT_LOW_31=" + COLOR_MAT_LOW_31 + " COLOR_MAT_LOW_32=" + COLOR_MAT_LOW_32 + " COLOR_MAT_LOW_33=" + COLOR_MAT_LOW_33 + " COLOR_MAT_LOW_34=" + COLOR_MAT_LOW_34);
                Slog.d(TAG, "COLOR_MAT_HIGH_11=" + COLOR_MAT_HIGH_11 + " COLOR_MAT_HIGH_12=" + COLOR_MAT_HIGH_12 + " COLOR_MAT_HIGH_13=" + COLOR_MAT_HIGH_13 + " COLOR_MAT_HIGH_14=" + COLOR_MAT_HIGH_14 + " COLOR_MAT_HIGH_21=" + COLOR_MAT_HIGH_21 + " COLOR_MAT_HIGH_22=" + COLOR_MAT_HIGH_22 + " COLOR_MAT_HIGH_23=" + COLOR_MAT_HIGH_23 + " COLOR_MAT_HIGH_24=" + COLOR_MAT_HIGH_24 + " COLOR_MAT_HIGH_31=" + COLOR_MAT_HIGH_31 + " COLOR_MAT_HIGH_32=" + COLOR_MAT_HIGH_32 + " COLOR_MAT_HIGH_33=" + COLOR_MAT_HIGH_33 + " COLOR_MAT_HIGH_34=" + COLOR_MAT_HIGH_34);
                StringBuilder sb = new StringBuilder();
                sb.append("ATime=");
                sb.append(ATIME);
                sb.append(" AGain=");
                sb.append(AGAIN);
                Slog.d(TAG, sb.toString());
                StringBuilder sb2 = new StringBuilder();
                sb2.append("mLuxScale=");
                sb2.append(this.mLuxScale);
                Slog.d(TAG, sb2.toString());
                StringBuilder sb3 = new StringBuilder();
                sb3.append("ProductionCalibration:");
                sb3.append(PRODUCTION_CALIBRATION_X);
                sb3.append(" ");
                sb3.append(PRODUCTION_CALIBRATION_Y);
                sb3.append(" ");
                sb3.append(PRODUCTION_CALIBRATION_Z);
                sb3.append(" ");
                sb3.append(PRODUCTION_CALIBRATION_IR1);
                Slog.d(TAG, sb3.toString());
            } else if (i == 2) {
                Slog.d(TAG, "mSensorVersion=" + mSensorVersion + " RATIO_IR_GREEN=" + RATIO_IR_GREEN);
                Slog.d(TAG, "LUX_COEF_HIGH_RED=" + LUX_COEF_HIGH_RED + " LUX_COEF_HIGH_GREEN=" + LUX_COEF_HIGH_GREEN + " LUX_COEF_HIGH_BLUE=" + LUX_COEF_HIGH_BLUE);
                Slog.d(TAG, "LUX_COEF_LOW_RED=" + LUX_COEF_LOW_RED + " LUX_COEF_LOW_GREEN=" + LUX_COEF_LOW_GREEN + " LUX_COEF_LOW_BLUE=" + LUX_COEF_LOW_BLUE);
                Slog.d(TAG, "X_CONSTANT=" + X_CONSTANT + " X_R_COEF=" + X_R_COEF + " X_R_QUADRATIC_COEF=" + X_R_QUADRATIC_COEF + " X_G_COEF=" + X_G_COEF + " X_RG_COEF=" + X_RG_COEF + " X_G_QUADRATIC_COEF=" + X_G_QUADRATIC_COEF);
                Slog.d(TAG, "Y_CONSTANT=" + Y_CONSTANT + " Y_R_COEF=" + Y_R_COEF + " Y_R_QUADRATIC_COEF=" + Y_R_QUADRATIC_COEF + " Y_G_COEF=" + Y_G_COEF + " Y_RG_COEF=" + Y_RG_COEF + " Y_G_QUADRATIC_COEF=" + Y_G_QUADRATIC_COEF);
            } else if (i == 3) {
                Slog.d(TAG, "mSensorVersion=" + mSensorVersion + " IR_BOUNDRY_RGBCW=" + IR_BOUNDRY_RGBCW);
                Slog.d(TAG, "LOW_1=" + LUX_COEF_LOW_1 + " LOW_2=" + LUX_COEF_LOW_2 + " LOW_3=" + LUX_COEF_LOW_3 + " LOW_4=" + LUX_COEF_LOW_4 + " LOW_5=" + LUX_COEF_LOW_5);
                Slog.d(TAG, "HIGH_1=" + LUX_COEF_HIGH_1 + " HIGH_2=" + LUX_COEF_HIGH_2 + " HIGH_3=" + LUX_COEF_HIGH_3 + " HIGH_4=" + LUX_COEF_HIGH_4 + " HIGH_5=" + LUX_COEF_HIGH_5);
                Slog.d(TAG, "LOW_11=" + COLOR_MAT_RGBCW_LOW_11 + " LOW_12=" + COLOR_MAT_RGBCW_LOW_12 + " LOW_13=" + COLOR_MAT_RGBCW_LOW_13 + " LOW_14=" + COLOR_MAT_RGBCW_LOW_14 + " LOW_15=" + COLOR_MAT_RGBCW_LOW_15 + " LOW_21=" + COLOR_MAT_RGBCW_LOW_21 + " LOW_22=" + COLOR_MAT_RGBCW_LOW_22 + " LOW_23=" + COLOR_MAT_RGBCW_LOW_23 + " LOW_24=" + COLOR_MAT_RGBCW_LOW_24 + " LOW_25=" + COLOR_MAT_RGBCW_LOW_25 + " LOW_31=" + COLOR_MAT_RGBCW_LOW_31 + " LOW_32=" + COLOR_MAT_RGBCW_LOW_32 + " LOW_33=" + COLOR_MAT_RGBCW_LOW_33 + " LOW_34=" + COLOR_MAT_RGBCW_LOW_34 + " LOW_35=" + COLOR_MAT_RGBCW_LOW_35);
                Slog.d(TAG, "HIGH_11=" + COLOR_MAT_RGBCW_HIGH_11 + " HIGH_12=" + COLOR_MAT_RGBCW_HIGH_12 + " HIGH_13=" + COLOR_MAT_RGBCW_HIGH_13 + " HIGH_14=" + COLOR_MAT_RGBCW_HIGH_14 + " HIGH_15=" + COLOR_MAT_RGBCW_HIGH_15 + " HIGH_21=" + COLOR_MAT_RGBCW_HIGH_21 + " HIGH_22=" + COLOR_MAT_RGBCW_HIGH_22 + " HIGH_23=" + COLOR_MAT_RGBCW_HIGH_23 + " HIGH_24=" + COLOR_MAT_RGBCW_HIGH_24 + " HIGH_25=" + COLOR_MAT_RGBCW_HIGH_25 + " HIGH_31=" + COLOR_MAT_RGBCW_HIGH_31 + " HIGH_32=" + COLOR_MAT_RGBCW_HIGH_32 + " HIGH_33=" + COLOR_MAT_RGBCW_HIGH_33 + " HIGH_34=" + COLOR_MAT_RGBCW_HIGH_34 + " HIGH_35=" + COLOR_MAT_RGBCW_HIGH_35);
                StringBuilder sb4 = new StringBuilder();
                sb4.append("ATIME_RGBCW=");
                sb4.append(ATIME_RGBCW);
                sb4.append(" AGAIN_RGBCW=");
                sb4.append(AGAIN_RGBCW);
                Slog.d(TAG, sb4.toString());
                StringBuilder sb5 = new StringBuilder();
                sb5.append("ProductionCalibration:");
                sb5.append(PRODUCTION_CALIBRATION_C);
                sb5.append(" ");
                sb5.append(PRODUCTION_CALIBRATION_R);
                sb5.append(" ");
                sb5.append(PRODUCTION_CALIBRATION_G);
                sb5.append(" ");
                sb5.append(PRODUCTION_CALIBRATION_B);
                sb5.append(" ");
                sb5.append(PRODUCTION_CALIBRATION_W);
                Slog.d(TAG, sb5.toString());
            }
            Slog.d(TAG, "mFrontRateMillis=" + mFrontRateMillis + " mBackRateMillis=" + mBackRateMillis + " mFusedRateMillis=" + mFusedRateMillis);
            StringBuilder sb6 = new StringBuilder();
            sb6.append("FRONT_SENSOR_STABILITY_THRESHOLD:");
            sb6.append(Arrays.toString(FRONT_SENSOR_STABILITY_THRESHOLD));
            Slog.d(TAG, sb6.toString());
            Slog.d(TAG, "BACK_SENSOR_STABILITY_THRESHOLD:" + Arrays.toString(BACK_SENSOR_STABILITY_THRESHOLD));
            Slog.d(TAG, "STABILIZED_PROBABILITY_LUT:" + Arrays.toString(STABILIZED_PROBABILITY_LUT));
            Slog.d(TAG, "BACK_ROOF_THRESH=" + BACK_ROOF_THRESH + " BACK_FLOOR_THRESH=" + BACK_FLOOR_THRESH + " DARK_ROOM_THRESH=" + DARK_ROOM_THRESH + " DARK_ROOM_DELTA=" + DARK_ROOM_DELTA);
            Slog.d(TAG, "DARK_ROOM_THRESH1=" + DARK_ROOM_THRESH1 + " DARK_ROOM_THRESH2=" + DARK_ROOM_THRESH2 + " DARK_ROOM_THRESH3=" + DARK_ROOM_THRESH3 + " DARK_ROOM_DELTA1=" + DARK_ROOM_DELTA1 + " DARK_ROOM_DELTA2=" + DARK_ROOM_DELTA2);
            StringBuilder sb7 = new StringBuilder();
            sb7.append("mFlashlightDetectionMode=");
            sb7.append(mFlashlightDetectionMode);
            sb7.append("FLASHLIGHT_OFF_TIME_THRESHOLD_MS=");
            sb7.append(FLASHLIGHT_OFF_TIME_THRESHOLD_MS);
            Slog.d(TAG, sb7.toString());
            Slog.d(TAG, "mBackSensorTimeOutTH=" + this.mBackSensorTimeOutTH + "mBackSensorBypassCountMax=" + this.mBackSensorBypassCountMax);
            StringBuilder sb8 = new StringBuilder();
            sb8.append("mIsFilterOn=");
            sb8.append(this.mIsFilterOn);
            Slog.d(TAG, sb8.toString());
            Slog.i(TAG, "mSpectrumGain =" + this.mSpectrumGain + ", mSpectrumTime = " + this.mSpectrumTime + ", mNeedNirCompensation = " + this.mNeedNirCompensation + ", mLuxScale = " + this.mLuxScale);
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
            String[] ids = this.mCameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                    this.mBackCameraId = id;
                }
            }
            if (this.mBackCameraId != null) {
                try {
                    this.mCameraManager.registerTorchCallback(this.mTorchCallback, (Handler) null);
                } catch (IllegalArgumentException e) {
                    mFlashlightDetectionMode = 0;
                    Slog.w(TAG, "IllegalArgumentException " + e);
                }
            } else {
                Slog.w(TAG, "mBackCameraId is null");
                mFlashlightDetectionMode = 0;
            }
        } catch (CameraAccessException e2) {
            mFlashlightDetectionMode = 0;
            Slog.w(TAG, "CameraAccessException " + e2);
        }
    }
}

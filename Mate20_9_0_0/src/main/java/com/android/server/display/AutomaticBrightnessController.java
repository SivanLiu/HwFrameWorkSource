package com.android.server.display;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.BacklightBrightness;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.EventLog;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import com.android.server.EventLogTags;
import com.android.server.HwServiceFactory;
import com.android.server.job.controllers.JobStatus;
import com.android.server.os.HwBootFail;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

public class AutomaticBrightnessController {
    private static final int AMBIENT_LIGHT_HORIZON = 10000;
    private static final int AMBIENT_LIGHT_LONG_HORIZON_MILLIS = 10000;
    private static final long AMBIENT_LIGHT_PREDICTION_TIME_MILLIS = 100;
    private static final int AMBIENT_LIGHT_SHORT_HORIZON_MILLIS = 2000;
    protected static long BRIGHTENING_LIGHT_DEBOUNCE = 2000;
    private static final long BRIGHTENING_LIGHT_DEBOUNCE_MORE_QUICKLLY = 1000;
    protected static float BRIGHTENING_LIGHT_HYSTERESIS = 0.1f;
    private static final int BRIGHTNESS_ADJUSTMENT_SAMPLE_DEBOUNCE_MILLIS = 10000;
    private static float BrightenDebounceTimePara = 0.0f;
    private static float BrightenDebounceTimeParaBig = 0.6f;
    private static float BrightenDeltaLuxMax = 0.0f;
    private static float BrightenDeltaLuxMin = 0.0f;
    private static float BrightenDeltaLuxPara = 0.0f;
    protected static float DARKENING_LIGHT_HYSTERESIS = 0.2f;
    private static final boolean DEBUG;
    private static final boolean DEBUG_CONTROLLER = false;
    private static final boolean DEBUG_PRETEND_LIGHT_SENSOR_ABSENT = false;
    private static float DarkenDebounceTimePara = 1.0f;
    private static float DarkenDebounceTimeParaBig = 0.0f;
    private static float DarkenDeltaLuxMax = 0.0f;
    private static float DarkenDeltaLuxMin = 0.0f;
    private static float DarkenDeltaLuxPara = 1.0f;
    protected static final int INT_BRIGHTNESS_COVER_MODE = SystemProperties.getInt("ro.config.hw_cover_brightness", 60);
    private static final int LIGHT_SENSOR_RATE_MILLIS = 300;
    protected static final int MSG_BRIGHTNESS_ADJUSTMENT_SAMPLE = 2;
    protected static final int MSG_INVALIDATE_SHORT_TERM_MODEL = 3;
    protected static final int MSG_UPDATE_AMBIENT_LUX = 1;
    protected static final int MSG_UPDATE_BRIGHTNESS = 4;
    protected static final boolean NEED_NEW_FILTER_ALGORITHM = needNewFilterAlgorithm();
    private static final float SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT_MAX_GAMMA = 3.0f;
    private static final int SHORT_TERM_MODEL_TIMEOUT_MILLIS = 30000;
    private static float Stability = 0.0f;
    private static final String TAG = "AutomaticBrightnessController";
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = false;
    protected long DARKENING_LIGHT_DEBOUNCE = 8000;
    private float SHORT_TERM_MODEL_THRESHOLD_RATIO = 0.6f;
    private final int mAmbientLightHorizon;
    protected AmbientLightRingBuffer mAmbientLightRingBuffer;
    protected AmbientLightRingBuffer mAmbientLightRingBufferFilter;
    protected float mAmbientLux;
    protected boolean mAmbientLuxValid;
    private final long mBrighteningLightDebounceConfig;
    private float mBrighteningLuxThreshold;
    private int mBrightnessAdjustmentSampleOldBrightness;
    private float mBrightnessAdjustmentSampleOldLux;
    private boolean mBrightnessAdjustmentSamplePending;
    protected boolean mBrightnessEnlarge = false;
    private final BrightnessMappingStrategy mBrightnessMapper;
    protected int mBrightnessNoLimitSetByApp = -1;
    protected final Callbacks mCallbacks;
    protected int mCurrentLightSensorRate;
    private final long mDarkeningLightDebounceConfig;
    private float mDarkeningLuxThreshold;
    private int mDisplayPolicy = 0;
    private final float mDozeScaleFactor;
    protected boolean mFirstAutoBrightness;
    protected boolean mFirstBrightnessAfterProximityNegative = false;
    protected AutomaticBrightnessHandler mHandler;
    private final HysteresisLevels mHysteresisLevels;
    protected final int mInitialLightSensorRate;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    protected final Sensor mLightSensor;
    protected long mLightSensorEnableElapsedTimeNanos;
    protected long mLightSensorEnableTime;
    protected boolean mLightSensorEnabled;
    protected final SensorEventListener mLightSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (AutomaticBrightnessController.this.mLightSensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float lux = event.values[0];
                long timeStamp = event.timestamp;
                if (AutomaticBrightnessController.DEBUG && time - AutomaticBrightnessController.this.mLightSensorEnableTime < 4000) {
                    String str = AutomaticBrightnessController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ambient lux=");
                    stringBuilder.append(lux);
                    stringBuilder.append(",timeStamp =");
                    stringBuilder.append(timeStamp);
                    Slog.d(str, stringBuilder.toString());
                }
                if ((!HwServiceFactory.shouldFilteInvalidSensorVal(lux) || AutomaticBrightnessController.INT_BRIGHTNESS_COVER_MODE != 0) && !AutomaticBrightnessController.this.interceptHandleLightSensorEvent(timeStamp, lux)) {
                    AutomaticBrightnessController.this.handleLightSensorEvent(time, lux);
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    protected int mLightSensorWarmUpTimeConfig;
    protected int mMaxBrightnessSetByCryogenic = 255;
    protected boolean mMaxBrightnessSetByCryogenicBypass = false;
    protected boolean mMaxBrightnessSetByCryogenicBypassDelayed = false;
    protected int mMaxBrightnessSetByThermal = 255;
    private final int mNormalLightSensorRate;
    protected long mPowerOffTimestamp = 0;
    protected long mPowerOnTimestamp = 0;
    private long mPrintLogTime = 0;
    protected int mRecentLightSamples;
    protected final boolean mResetAmbientLuxAfterWarmUpConfig;
    protected int mScreenAutoBrightness = -1;
    protected Spline mScreenAutoBrightnessSpline;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private int mScreenBrightnessRangeSetByAppMax;
    private int mScreenBrightnessRangeSetByAppMin;
    protected final SensorManager mSensorManager;
    protected boolean mSetbrightnessImmediateEnable = false;
    private float mShortTermModelAnchor;
    private boolean mShortTermModelValid;
    protected int mUpdateAutoBrightnessCount;
    private boolean mUseTwilight;
    protected boolean mWakeupFromSleep = true;
    private final int mWeightingIntercept;

    protected static final class AmbientLightRingBuffer {
        private static final float BUFFER_SLACK = 1.5f;
        private static final boolean DEBUG = false;
        private static final String TAG = "AmbientLightRingBuffer";
        private static final int mLargemSmallStabilityTimeConstant = 10;
        private static float mLuxBufferAvg = 0.0f;
        private static float mLuxBufferAvgMax = 0.0f;
        private static float mLuxBufferAvgMin = 0.0f;
        private static final int mSmallStabilityTimeConstant = 20;
        private static float mStability = 0.0f;
        private int mCapacity;
        private int mCount;
        private int mEnd;
        private float[] mRingLux;
        private long[] mRingTime;
        private int mStart;

        public AmbientLightRingBuffer(long lightSensorRate, int ambientLightHorizon) {
            if (AutomaticBrightnessController.NEED_NEW_FILTER_ALGORITHM) {
                this.mCapacity = (int) Math.ceil((double) ((((float) ambientLightHorizon) * BUFFER_SLACK) / ((float) lightSensorRate)));
            } else {
                this.mCapacity = 50;
            }
            this.mRingLux = new float[this.mCapacity];
            this.mRingTime = new long[this.mCapacity];
        }

        public float getLux(int index) {
            return this.mRingLux[offsetOf(index)];
        }

        public long getTime(int index) {
            return this.mRingTime[offsetOf(index)];
        }

        public void push(long time, float lux) {
            int next = this.mEnd;
            if (this.mCount == this.mCapacity) {
                int newSize = this.mCapacity * 2;
                float[] newRingLux = new float[newSize];
                long[] newRingTime = new long[newSize];
                int length = this.mCapacity - this.mStart;
                System.arraycopy(this.mRingLux, this.mStart, newRingLux, 0, length);
                System.arraycopy(this.mRingTime, this.mStart, newRingTime, 0, length);
                if (this.mStart != 0) {
                    System.arraycopy(this.mRingLux, 0, newRingLux, length, this.mStart);
                    System.arraycopy(this.mRingTime, 0, newRingTime, length, this.mStart);
                }
                this.mRingLux = newRingLux;
                this.mRingTime = newRingTime;
                next = this.mCapacity;
                this.mCapacity = newSize;
                this.mStart = 0;
            }
            this.mRingTime[next] = time;
            this.mRingLux[next] = lux;
            this.mEnd = next + 1;
            if (this.mEnd == this.mCapacity) {
                this.mEnd = 0;
            }
            this.mCount++;
        }

        public void prune(long horizon) {
            if (this.mCount != 0) {
                while (this.mCount > 1) {
                    int next = this.mStart + 1;
                    if (next >= this.mCapacity) {
                        next -= this.mCapacity;
                    }
                    if (this.mRingTime[next] > horizon) {
                        break;
                    }
                    this.mStart = next;
                    this.mCount--;
                }
                if (this.mRingTime[this.mStart] < horizon) {
                    this.mRingTime[this.mStart] = horizon;
                }
            }
        }

        public int size() {
            return this.mCount;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append('[');
            for (int i = 0; i < this.mCount; i++) {
                long next = i + 1 < this.mCount ? getTime(i + 1) : SystemClock.uptimeMillis();
                if (i != 0) {
                    buf.append(", ");
                }
                buf.append(getLux(i));
                buf.append(" / ");
                buf.append(next - getTime(i));
                buf.append("ms");
            }
            buf.append(']');
            return buf.toString();
        }

        public String toString(int n) {
            StringBuffer buf = new StringBuffer();
            buf.append('[');
            int i = this.mCount - n;
            while (i >= 0 && i < this.mCount) {
                if (i != this.mCount - n) {
                    buf.append(", ");
                }
                buf.append(getLux(i));
                buf.append(SliceAuthority.DELIMITER);
                buf.append(getTime(i));
                i++;
            }
            buf.append(']');
            return buf.toString();
        }

        private int offsetOf(int index) {
            if (index >= this.mCount || index < 0) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            index += this.mStart;
            if (index >= this.mCapacity) {
                return index - this.mCapacity;
            }
            return index;
        }

        public float calculateStability() {
            if (this.mCount == 0) {
                return 0.0f;
            }
            float luxT2;
            int indexMax;
            int index1;
            int T2;
            float luxk3;
            float tmp;
            float Stability1;
            float currentLux = getLux(this.mCount - 1);
            calculateAvg();
            int indexMax2 = 0;
            int T2Max = 0;
            int T1Max = 0;
            float luxT2Max = currentLux;
            float luxT1Max = currentLux;
            int indexMin = 0;
            int T2Min = 0;
            int T1Min = 0;
            float luxT2Min = currentLux;
            float luxT1Min = currentLux;
            int index = 0;
            int T22 = 0;
            int T1 = 0;
            float luxT22 = currentLux;
            float luxT1 = currentLux;
            int j = 0;
            while (j < this.mCount - 1) {
                currentLux = getLux((this.mCount - 1) - j);
                float luxT12 = luxT1;
                luxT1 = getLux(((this.mCount - 1) - j) - 1);
                if (((mLuxBufferAvg <= currentLux && mLuxBufferAvg >= luxT1) || (mLuxBufferAvg >= currentLux && mLuxBufferAvg <= luxT1)) && !(mLuxBufferAvg == currentLux && mLuxBufferAvg == luxT1)) {
                    luxT12 = currentLux;
                    index = j;
                    T22 = ((this.mCount - 1) - j) - 1;
                    T1 = (this.mCount - 1) - j;
                    luxT22 = luxT1;
                }
                if ((mLuxBufferAvgMin > currentLux || mLuxBufferAvgMin < luxT1) && (mLuxBufferAvgMin < currentLux || mLuxBufferAvgMin > luxT1)) {
                    luxT2 = luxT22;
                } else if (mLuxBufferAvgMin == currentLux && mLuxBufferAvgMin == luxT1) {
                    luxT2 = luxT22;
                } else {
                    luxT1Min = currentLux;
                    luxT2Min = luxT1;
                    luxT2 = luxT22;
                    indexMin = j;
                    T2Min = ((this.mCount - 1) - j) - 1;
                    T1Min = (this.mCount - 1) - j;
                }
                if (((mLuxBufferAvgMax > currentLux || mLuxBufferAvgMax < luxT1) && (mLuxBufferAvgMax < currentLux || mLuxBufferAvgMax > luxT1)) || (mLuxBufferAvgMax == currentLux && mLuxBufferAvgMax == luxT1)) {
                    indexMax = indexMax2;
                } else {
                    float f = luxT1;
                    luxT1Max = currentLux;
                    indexMax = j;
                    T2Max = ((this.mCount - 1) - j) - 1;
                    T1Max = (this.mCount - 1) - j;
                    luxT2Max = f;
                }
                if (index != 0 && ((indexMin != 0 || indexMax != 0) && ((index <= indexMin && index >= indexMax) || (index >= indexMin && index <= indexMax)))) {
                    break;
                }
                j++;
                indexMax2 = indexMax;
                luxT1 = luxT12;
                luxT22 = luxT2;
            }
            luxT2 = luxT22;
            indexMax = indexMax2;
            if (indexMax <= indexMin) {
                index1 = indexMax;
                j = indexMin;
            } else {
                index1 = indexMin;
                j = indexMax;
            }
            int k1 = (this.mCount - 1) - index1;
            while (true) {
                int index12 = index1;
                if (k1 > this.mCount - 1) {
                    T2 = T22;
                    break;
                } else if (k1 == this.mCount - 1) {
                    T2 = T22;
                    break;
                } else {
                    currentLux = getLux(k1);
                    T2 = T22;
                    T22 = getLux(k1 + 1);
                    if (indexMax > indexMin) {
                        if (currentLux <= T22) {
                            break;
                        }
                        T1 = k1 + 1;
                    } else if (currentLux >= T22) {
                        break;
                    } else {
                        T1 = k1 + 1;
                    }
                    k1++;
                    index1 = index12;
                    T22 = T2;
                }
            }
            index1 = (this.mCount - 1) - j;
            k1 = T2;
            while (index1 >= 0) {
                int i;
                if (index1 == 0) {
                    i = j;
                    break;
                }
                luxk3 = getLux(index1);
                i = j;
                j = getLux(index1 - 1);
                if (indexMax > indexMin) {
                    if (luxk3 >= j) {
                        break;
                    }
                    k1 = index1 - 1;
                } else if (luxk3 <= j) {
                    break;
                } else {
                    k1 = index1 - 1;
                }
                index1--;
                j = i;
            }
            index1 = (this.mCount - 1) - T1;
            j = k1;
            luxk3 = calculateStabilityFactor(T1, this.mCount - 1);
            luxT22 = calcluateAvg(T1, this.mCount - 1);
            float s2 = calculateStabilityFactor(0, k1);
            luxT1Min = Math.abs(luxT22 - calcluateAvg(0, k1));
            float k = Math.abs((getLux(T1) - getLux(k1)) / ((float) (T1 - k1)));
            if (k < 10.0f / (k + 5.0f)) {
                tmp = k;
            } else {
                tmp = 10.0f / (k + 5.0f);
            }
            if (tmp > 20.0f / (luxT1Min + 10.0f)) {
                tmp = 20.0f / (luxT1Min + 10.0f);
            }
            if (index1 > 20) {
                Stability1 = luxk3;
                int i2 = k1;
                float f2 = luxT22;
            } else {
                k = (float) Math.exp((double) (index1 - 20));
                luxT1 = (float) (20 - index1);
                Stability1 = ((k * luxk3) + (luxT1 * tmp)) / (k + luxT1);
            }
            if (j > 10) {
                k = s2;
            } else {
                luxT1 = (float) Math.exp((double) (j - 10));
                luxT22 = (float) (10 - j);
                k = ((luxT1 * s2) + (luxT22 * tmp)) / (luxT1 + luxT22);
            }
            if (index1 > 20) {
                mStability = Stability1;
            } else {
                luxT1 = (float) Math.exp((double) (index1 - 20));
                luxT22 = (float) (20 - index1);
                mStability = ((luxT1 * Stability1) + (luxT22 * k)) / (luxT1 + luxT22);
            }
            return mStability;
        }

        private void calculateAvg() {
            if (this.mCount != 0) {
                float currentLux = getLux(this.mCount - 1);
                float luxBufferSum = 0.0f;
                float luxBufferMin = currentLux;
                float luxBufferMax = currentLux;
                for (int i = this.mCount - 1; i >= 0; i--) {
                    float lux = getLux(i);
                    if (lux > luxBufferMax) {
                        luxBufferMax = lux;
                    }
                    if (lux < luxBufferMin) {
                        luxBufferMin = lux;
                    }
                    luxBufferSum += lux;
                }
                mLuxBufferAvg = luxBufferSum / ((float) this.mCount);
                mLuxBufferAvgMax = (mLuxBufferAvg + luxBufferMax) / 2.0f;
                mLuxBufferAvgMin = (mLuxBufferAvg + luxBufferMin) / 2.0f;
            }
        }

        private float calcluateAvg(int start, int end) {
            float sum = 0.0f;
            for (int i = start; i <= end; i++) {
                sum += getLux(i);
            }
            if (end < start) {
                return 0.0f;
            }
            return sum / ((float) ((end - start) + 1));
        }

        private float calculateStabilityFactor(int start, int end) {
            int size = (end - start) + 1;
            float sum = 0.0f;
            float sigma = 0.0f;
            if (size <= 1) {
                return 0.0f;
            }
            for (int i = start; i <= end; i++) {
                sum += getLux(i);
            }
            float avg = sum / ((float) size);
            for (int i2 = start; i2 <= end; i2++) {
                sigma += (getLux(i2) - avg) * (getLux(i2) - avg);
            }
            float ss = sigma / ((float) (size - 1));
            if (avg == 0.0f) {
                return 0.0f;
            }
            return ss / avg;
        }
    }

    protected final class AutomaticBrightnessHandler extends Handler {
        public AutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessController.this.updateAmbientLux();
                    return;
                case 2:
                    AutomaticBrightnessController.this.collectBrightnessAdjustmentSample();
                    return;
                case 3:
                    AutomaticBrightnessController.this.invalidateShortTermModel();
                    return;
                case 4:
                    AutomaticBrightnessController.this.updateBrightnessIfNoAmbientLuxReported();
                    return;
                default:
                    return;
            }
        }
    }

    public interface Callbacks {
        void updateBrightness();

        void updateProximityState(boolean z);
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public void setBacklightBrightness(BacklightBrightness backlightBrightness) {
        this.mScreenBrightnessRangeSetByAppMin = backlightBrightness.min;
        this.mScreenBrightnessRangeSetByAppMax = backlightBrightness.max;
    }

    public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
    }

    public static boolean needNewFilterAlgorithm() {
        String product = getProductName();
        if (product == null) {
            return false;
        }
        boolean flag = product.contains("next");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NEWFILTER flag = ");
        stringBuilder.append(flag);
        Slog.e(str, stringBuilder.toString());
        return flag;
    }

    public static String getProductName() {
        String boardname = readFileByChars("/proc/device-tree/hisi,boardname").trim();
        String productName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        if (boardname == null) {
            return HealthServiceWrapper.INSTANCE_VENDOR;
        }
        String[] arrays = boardname.split("_");
        if (arrays == null || arrays.length < 2) {
            productName = HealthServiceWrapper.INSTANCE_VENDOR;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(arrays[0]);
            stringBuilder.append("_");
            stringBuilder.append(arrays[1]);
            productName = stringBuilder.toString();
        }
        return productName.toLowerCase();
    }

    private static String readFileByChars(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.canRead()) {
            Reader reader = null;
            char[] tempChars = new char[512];
            StringBuilder sb = new StringBuilder();
            try {
                reader = new InputStreamReader(new FileInputStream(fileName));
                while (true) {
                    int read = reader.read(tempChars);
                    int charRead = read;
                    if (read != -1) {
                        sb.append(tempChars, 0, charRead);
                    } else {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    }
                }
                reader.close();
            } catch (IOException e1) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("read file name error, file name is:");
                stringBuilder.append(fileName);
                Slog.e(str, stringBuilder.toString());
                e1.printStackTrace();
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                    }
                }
            }
            return sb.toString();
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("file is exists : ");
        stringBuilder2.append(file.exists());
        stringBuilder2.append(" file can read : ");
        stringBuilder2.append(file.canRead());
        Slog.d(str2, stringBuilder2.toString());
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    public AutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, BrightnessMappingStrategy mapper, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels hysteresisLevels) {
        this.mCallbacks = callbacks;
        this.mSensorManager = sensorManager;
        this.mBrightnessMapper = mapper;
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mScreenBrightnessRangeSetByAppMin = this.mScreenBrightnessRangeMinimum;
        this.mScreenBrightnessRangeSetByAppMax = this.mScreenBrightnessRangeMaximum;
        this.mLightSensorWarmUpTimeConfig = lightSensorWarmUpTime;
        this.mDozeScaleFactor = dozeScaleFactor;
        this.mNormalLightSensorRate = lightSensorRate;
        this.mInitialLightSensorRate = initialLightSensorRate;
        this.mCurrentLightSensorRate = -1;
        this.mBrighteningLightDebounceConfig = brighteningLightDebounceConfig;
        this.mDarkeningLightDebounceConfig = darkeningLightDebounceConfig;
        this.mResetAmbientLuxAfterWarmUpConfig = resetAmbientLuxAfterWarmUpConfig;
        this.mAmbientLightHorizon = 10000;
        this.mWeightingIntercept = 10000;
        this.mHysteresisLevels = hysteresisLevels;
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = -1.0f;
        this.mHandler = new AutomaticBrightnessHandler(looper);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer((long) this.mNormalLightSensorRate, this.mAmbientLightHorizon);
        if (NEED_NEW_FILTER_ALGORITHM) {
            this.mAmbientLightRingBufferFilter = new AmbientLightRingBuffer((long) this.mNormalLightSensorRate, this.mAmbientLightHorizon);
        }
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
    }

    public int getAutomaticScreenBrightness() {
        int brightness = this.mScreenAutoBrightness;
        if (brightness >= 0) {
            brightness = MathUtils.constrain(brightness, this.mScreenBrightnessRangeSetByAppMin, this.mScreenBrightnessRangeSetByAppMax);
        }
        if (this.mBrightnessNoLimitSetByApp > 0) {
            return this.mBrightnessNoLimitSetByApp;
        }
        if (!(this.mMaxBrightnessSetByCryogenicBypass || this.mMaxBrightnessSetByCryogenicBypassDelayed || brightness <= this.mMaxBrightnessSetByCryogenic)) {
            brightness = this.mMaxBrightnessSetByCryogenic;
        }
        setBrightnessLimitedByThermal(brightness > this.mMaxBrightnessSetByThermal);
        if (brightness > this.mMaxBrightnessSetByThermal) {
            brightness = this.mMaxBrightnessSetByThermal;
        }
        brightness = getAutoBrightnessBaseInOutDoorLimit(brightness);
        if (this.mDisplayPolicy == 1) {
            return (int) (((float) brightness) * this.mDozeScaleFactor);
        }
        return brightness;
    }

    public float getAutomaticScreenBrightnessAdjustment() {
        return this.mBrightnessMapper.getAutoBrightnessAdjustment();
    }

    public void configure(boolean enable, BrightnessConfiguration configuration, float brightness, boolean userChangedBrightness, float adjustment, boolean userChangedAutoBrightnessAdjustment, int displayPolicy) {
        boolean z = true;
        boolean dozing = displayPolicy == 1;
        boolean changed = setBrightnessConfiguration(configuration) | setDisplayPolicy(displayPolicy);
        if (userChangedAutoBrightnessAdjustment) {
            changed |= setAutoBrightnessAdjustment(adjustment);
        }
        if (userChangedBrightness && enable) {
            changed |= setScreenBrightnessByUser(brightness);
        }
        boolean userInitiatedChange = userChangedBrightness || userChangedAutoBrightnessAdjustment;
        if (userInitiatedChange && enable && !dozing) {
            prepareBrightnessAdjustmentSample();
        }
        if (!enable || dozing) {
            z = false;
        }
        if (setLightSensorEnabled(z) | changed) {
            updateAutoBrightness(false);
        }
    }

    public boolean hasUserDataPoints() {
        return this.mBrightnessMapper.hasUserDataPoints();
    }

    public boolean isDefaultConfig() {
        return this.mBrightnessMapper.isDefaultConfig();
    }

    public BrightnessConfiguration getDefaultConfig() {
        return this.mBrightnessMapper.getDefaultConfig();
    }

    private boolean setDisplayPolicy(int policy) {
        if (this.mDisplayPolicy == policy) {
            return false;
        }
        int oldPolicy = this.mDisplayPolicy;
        this.mDisplayPolicy = policy;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Display policy transitioning from ");
            stringBuilder.append(oldPolicy);
            stringBuilder.append(" to ");
            stringBuilder.append(policy);
            Slog.d(str, stringBuilder.toString());
        }
        if (!isInteractivePolicy(policy) && isInteractivePolicy(oldPolicy)) {
            this.mHandler.sendEmptyMessageDelayed(3, 30000);
        } else if (isInteractivePolicy(policy) && !isInteractivePolicy(oldPolicy)) {
            this.mHandler.removeMessages(3);
        }
        return true;
    }

    private static boolean isInteractivePolicy(int policy) {
        return policy == 3 || policy == 2 || policy == 4;
    }

    private boolean setScreenBrightnessByUser(float brightness) {
        if (!this.mAmbientLuxValid) {
            return false;
        }
        this.mBrightnessMapper.addUserDataPoint(this.mAmbientLux, brightness);
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = this.mAmbientLux;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ShortTermModel: anchor=");
            stringBuilder.append(this.mShortTermModelAnchor);
            Slog.d(str, stringBuilder.toString());
        }
        return true;
    }

    public void resetShortTermModel() {
        this.mBrightnessMapper.clearUserDataPoints();
        this.mShortTermModelValid = true;
        this.mShortTermModelAnchor = -1.0f;
    }

    private void invalidateShortTermModel() {
        if (DEBUG) {
            Slog.d(TAG, "ShortTermModel: invalidate user data");
        }
        this.mShortTermModelValid = false;
    }

    public boolean setBrightnessConfiguration(BrightnessConfiguration configuration) {
        if (!this.mBrightnessMapper.setBrightnessConfiguration(configuration)) {
            return false;
        }
        resetShortTermModel();
        return true;
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Automatic Brightness Controller Configuration:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessRangeMinimum=");
        stringBuilder.append(this.mScreenBrightnessRangeMinimum);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessRangeMaximum=");
        stringBuilder.append(this.mScreenBrightnessRangeMaximum);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessRangeSetByAppMin=");
        stringBuilder.append(this.mScreenBrightnessRangeSetByAppMin);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessRangeSetByAppMax=");
        stringBuilder.append(this.mScreenBrightnessRangeSetByAppMax);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mDozeScaleFactor=");
        stringBuilder.append(this.mDozeScaleFactor);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mInitialLightSensorRate=");
        stringBuilder.append(this.mInitialLightSensorRate);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mNormalLightSensorRate=");
        stringBuilder.append(this.mNormalLightSensorRate);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLightSensorWarmUpTimeConfig=");
        stringBuilder.append(this.mLightSensorWarmUpTimeConfig);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mBrighteningLightDebounceConfig=");
        stringBuilder.append(this.mBrighteningLightDebounceConfig);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mDarkeningLightDebounceConfig=");
        stringBuilder.append(this.mDarkeningLightDebounceConfig);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mResetAmbientLuxAfterWarmUpConfig=");
        stringBuilder.append(this.mResetAmbientLuxAfterWarmUpConfig);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAmbientLightHorizon=");
        stringBuilder.append(this.mAmbientLightHorizon);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mWeightingIntercept=");
        stringBuilder.append(this.mWeightingIntercept);
        pw.println(stringBuilder.toString());
        pw.println();
        pw.println("Automatic Brightness Controller State:");
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLightSensor=");
        stringBuilder.append(this.mLightSensor);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLightSensorEnabled=");
        stringBuilder.append(this.mLightSensorEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLightSensorEnableTime=");
        stringBuilder.append(TimeUtils.formatUptime(this.mLightSensorEnableTime));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCurrentLightSensorRate=");
        stringBuilder.append(this.mCurrentLightSensorRate);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAmbientLux=");
        stringBuilder.append(this.mAmbientLux);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAmbientLuxValid=");
        stringBuilder.append(this.mAmbientLuxValid);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mBrighteningLuxThreshold=");
        stringBuilder.append(this.mBrighteningLuxThreshold);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mDarkeningLuxThreshold=");
        stringBuilder.append(this.mDarkeningLuxThreshold);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLastObservedLux=");
        stringBuilder.append(this.mLastObservedLux);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLastObservedLuxTime=");
        stringBuilder.append(TimeUtils.formatUptime(this.mLastObservedLuxTime));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mRecentLightSamples=");
        stringBuilder.append(this.mRecentLightSamples);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAmbientLightRingBuffer=");
        stringBuilder.append(this.mAmbientLightRingBuffer);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenAutoBrightness=");
        stringBuilder.append(this.mScreenAutoBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mDisplayPolicy=");
        stringBuilder.append(DisplayPowerRequest.policyToString(this.mDisplayPolicy));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mShortTermModelAnchor=");
        stringBuilder.append(this.mShortTermModelAnchor);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mShortTermModelValid=");
        stringBuilder.append(this.mShortTermModelValid);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mBrightnessAdjustmentSamplePending=");
        stringBuilder.append(this.mBrightnessAdjustmentSamplePending);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mBrightnessAdjustmentSampleOldLux=");
        stringBuilder.append(this.mBrightnessAdjustmentSampleOldLux);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mBrightnessAdjustmentSampleOldBrightness=");
        stringBuilder.append(this.mBrightnessAdjustmentSampleOldBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mShortTermModelValid=");
        stringBuilder.append(this.mShortTermModelValid);
        pw.println(stringBuilder.toString());
        pw.println();
        this.mBrightnessMapper.dump(pw);
        pw.println();
        this.mHysteresisLevels.dump(pw);
    }

    protected boolean setLightSensorEnabled(boolean enable) {
        String str;
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mFirstAutoBrightness = true;
                this.mUpdateAutoBrightnessCount = 0;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mLightSensorEnableElapsedTimeNanos = SystemClock.elapsedRealtimeNanos();
                this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                if (this.mWakeupFromSleep) {
                    this.mHandler.sendEmptyMessageAtTime(4, this.mLightSensorEnableTime + 200);
                }
                if (DEBUG) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Enable LightSensor at time:mLightSensorEnableTime=");
                    stringBuilder.append(SystemClock.uptimeMillis());
                    stringBuilder.append(",mLightSensorEnableElapsedTimeNanos=");
                    stringBuilder.append(this.mLightSensorEnableElapsedTimeNanos);
                    Slog.d(str, stringBuilder.toString());
                }
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mFirstAutoBrightness = false;
            this.mAmbientLuxValid = this.mResetAmbientLuxAfterWarmUpConfig ^ true;
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            clearFilterAlgoParas();
            if (NEED_NEW_FILTER_ALGORITHM) {
                this.mAmbientLightRingBufferFilter.clear();
            }
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(4);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            if (DEBUG) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Disable LightSensor at time:");
                stringBuilder2.append(SystemClock.uptimeMillis());
                Slog.d(str, stringBuilder2.toString());
            }
        }
        return false;
    }

    protected void handleLightSensorEvent(long time, float lux) {
        Trace.traceCounter(131072, "ALS", (int) lux);
        this.mHandler.removeMessages(1);
        if (this.mAmbientLightRingBuffer.size() == 0) {
            adjustLightSensorRate(this.mNormalLightSensorRate);
        }
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    protected boolean getSetbrightnessImmediateEnableForCaliTest() {
        return this.mSetbrightnessImmediateEnable;
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mRecentLightSamples++;
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    private void adjustLightSensorRate(int lightSensorRate) {
        if (lightSensorRate != this.mCurrentLightSensorRate) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("adjustLightSensorRate: previousRate=");
                stringBuilder.append(this.mCurrentLightSensorRate);
                stringBuilder.append(", currentRate=");
                stringBuilder.append(lightSensorRate);
                Slog.d(str, stringBuilder.toString());
            }
            this.mCurrentLightSensorRate = lightSensorRate;
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, lightSensorRate * 1000, this.mHandler);
        }
    }

    protected boolean setAutoBrightnessAdjustment(float adjustment) {
        return this.mBrightnessMapper.setAutoBrightnessAdjustment(adjustment);
    }

    private void setAmbientLux(float lux) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAmbientLux(");
            stringBuilder.append(lux);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (lux < 0.0f) {
            Slog.w(TAG, "Ambient lux was negative, ignoring and setting to 0");
            lux = 0.0f;
        }
        this.mAmbientLux = lux;
        updatepara(this.mAmbientLightRingBuffer);
        if (NEED_NEW_FILTER_ALGORITHM) {
            updatepara(this.mAmbientLightRingBuffer.calculateStability());
            setDarkenThreshold();
            setBrightenThreshold();
        }
        this.mBrighteningLuxThreshold = this.mHysteresisLevels.getBrighteningThreshold(lux);
        this.mDarkeningLuxThreshold = this.mHysteresisLevels.getDarkeningThreshold(lux);
        if (!this.mShortTermModelValid && this.mShortTermModelAnchor != -1.0f) {
            float minAmbientLux = this.mShortTermModelAnchor - (this.mShortTermModelAnchor * this.SHORT_TERM_MODEL_THRESHOLD_RATIO);
            float maxAmbientLux = this.mShortTermModelAnchor + (this.mShortTermModelAnchor * this.SHORT_TERM_MODEL_THRESHOLD_RATIO);
            String str2;
            StringBuilder stringBuilder2;
            if (minAmbientLux >= this.mAmbientLux || this.mAmbientLux >= maxAmbientLux) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ShortTermModel: reset data, ambient lux is ");
                stringBuilder2.append(this.mAmbientLux);
                stringBuilder2.append("(");
                stringBuilder2.append(minAmbientLux);
                stringBuilder2.append(", ");
                stringBuilder2.append(maxAmbientLux);
                stringBuilder2.append(")");
                Slog.d(str2, stringBuilder2.toString());
                resetShortTermModel();
                return;
            }
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ShortTermModel: re-validate user data, ambient lux is ");
                stringBuilder2.append(minAmbientLux);
                stringBuilder2.append(" < ");
                stringBuilder2.append(this.mAmbientLux);
                stringBuilder2.append(" < ");
                stringBuilder2.append(maxAmbientLux);
                Slog.d(str2, stringBuilder2.toString());
            }
            this.mShortTermModelValid = true;
        }
    }

    private float calculateAmbientLux(long now, long horizon) {
        AutomaticBrightnessController automaticBrightnessController = this;
        long j = now;
        long j2 = horizon;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calculateAmbientLux(");
            stringBuilder.append(j);
            stringBuilder.append(", ");
            stringBuilder.append(j2);
            stringBuilder.append(")");
            Slog.d(str, stringBuilder.toString());
        }
        if (NEED_NEW_FILTER_ALGORITHM) {
            return calculateAmbientLuxForNewPolicy(now);
        }
        int N = automaticBrightnessController.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        }
        int i;
        String str2;
        int endIndex = 0;
        long horizonStartTime = j - j2;
        int i2 = 0;
        while (i2 < N - 1 && automaticBrightnessController.mAmbientLightRingBuffer.getTime(i2 + 1) <= horizonStartTime) {
            endIndex++;
            i2++;
        }
        if (DEBUG) {
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("calculateAmbientLux: selected endIndex=");
            stringBuilder2.append(endIndex);
            stringBuilder2.append(", point=(");
            stringBuilder2.append(automaticBrightnessController.mAmbientLightRingBuffer.getTime(endIndex));
            stringBuilder2.append(", ");
            stringBuilder2.append(automaticBrightnessController.mAmbientLightRingBuffer.getLux(endIndex));
            stringBuilder2.append(")");
            Slog.d(str3, stringBuilder2.toString());
        }
        float sum = 0.0f;
        float totalWeight = 0.0f;
        long endTime = AMBIENT_LIGHT_PREDICTION_TIME_MILLIS;
        int i3 = N - 1;
        while (i3 >= endIndex) {
            long eventTime = automaticBrightnessController.mAmbientLightRingBuffer.getTime(i3);
            if (i3 == endIndex && eventTime < horizonStartTime) {
                eventTime = horizonStartTime;
            }
            j2 = eventTime - j;
            float weight = automaticBrightnessController.calculateWeight(j2, endTime);
            int i4;
            if (weight < 0.0f) {
                i4 = N;
                i = endIndex;
                break;
            }
            float lux = automaticBrightnessController.mAmbientLightRingBuffer.getLux(i3);
            if (DEBUG) {
                str2 = TAG;
                i4 = N;
                StringBuilder stringBuilder3 = new StringBuilder();
                i = endIndex;
                stringBuilder3.append("calculateAmbientLux: [");
                stringBuilder3.append(j2);
                stringBuilder3.append(", ");
                stringBuilder3.append(endTime);
                stringBuilder3.append("]: lux=");
                stringBuilder3.append(lux);
                stringBuilder3.append(", weight=");
                stringBuilder3.append(weight);
                Slog.d(str2, stringBuilder3.toString());
            } else {
                i4 = N;
                i = endIndex;
            }
            totalWeight += weight;
            sum += lux * weight;
            endTime = j2;
            i3--;
            N = i4;
            endIndex = i;
            automaticBrightnessController = this;
            j = now;
            j2 = horizon;
        }
        i = endIndex;
        if (DEBUG) {
            str2 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("calculateAmbientLux: totalWeight=");
            stringBuilder4.append(totalWeight);
            stringBuilder4.append(", newAmbientLux=");
            stringBuilder4.append(sum / totalWeight);
            Slog.d(str2, stringBuilder4.toString());
        }
        return sum / totalWeight;
    }

    private float calculateAmbientLuxForNewPolicy(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "calculateAmbientLux: No ambient light readings available");
            return -1.0f;
        } else if (N < 5) {
            return this.mAmbientLightRingBuffer.getLux(N - 1);
        } else {
            float sum = this.mAmbientLightRingBuffer.getLux(N - 1);
            float luxMin = this.mAmbientLightRingBuffer.getLux(N - 1);
            float luxMax = this.mAmbientLightRingBuffer.getLux(N - 1);
            for (int i = N - 2; i >= (N - 1) - 4; i--) {
                if (luxMin > this.mAmbientLightRingBuffer.getLux(i)) {
                    luxMin = this.mAmbientLightRingBuffer.getLux(i);
                }
                if (luxMax < this.mAmbientLightRingBuffer.getLux(i)) {
                    luxMax = this.mAmbientLightRingBuffer.getLux(i);
                }
                sum += this.mAmbientLightRingBuffer.getLux(i);
            }
            return ((sum - luxMin) - luxMax) / SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT_MAX_GAMMA;
        }
    }

    private float calculateWeight(long startDelta, long endDelta) {
        return weightIntegral(endDelta) - weightIntegral(startDelta);
    }

    private float weightIntegral(long x) {
        return ((float) x) * (((((float) x) * 0.5f) * SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT_MAX_GAMMA) + ((float) this.mWeightingIntercept));
    }

    private void updatepara(float stability) {
        Stability = stability;
        if (Stability > 100.0f) {
            Stability = 100.0f;
            DarkenDebounceTimeParaBig = 1.0f;
            BRIGHTENING_LIGHT_HYSTERESIS = Stability / 100.0f;
        } else if (Stability < 5.0f) {
            Stability = 5.0f;
            DarkenDebounceTimeParaBig = 0.1f;
            BRIGHTENING_LIGHT_HYSTERESIS = Stability / 100.0f;
        } else {
            DarkenDebounceTimeParaBig = 1.0f;
            BRIGHTENING_LIGHT_HYSTERESIS = Stability / 100.0f;
        }
        BRIGHTENING_LIGHT_DEBOUNCE = (long) (((double) (800.0f * BrightenDebounceTimeParaBig)) * ((((double) (BrightenDebounceTimePara * Stability)) / 100.0d) + 1.0d));
        this.DARKENING_LIGHT_DEBOUNCE = (long) (4000.0d * (1.0d + (((double) (DarkenDebounceTimePara * Stability)) / 100.0d)));
    }

    private void setDarkenThreshold() {
        if (this.mAmbientLux >= 1000.0f) {
            DarkenDeltaLuxMax = this.mAmbientLux - 500.0f;
            DarkenDeltaLuxMin = DarkenDeltaLuxMax;
            Stability = 5.0f;
        } else if (this.mAmbientLux >= 500.0f) {
            DarkenDeltaLuxMax = Math.min(this.mAmbientLux - 100.0f, 500.0f);
            DarkenDeltaLuxMin = DarkenDeltaLuxMax;
            Stability = 5.0f;
        } else if (this.mAmbientLux >= 100.0f) {
            DarkenDeltaLuxMax = Math.min(this.mAmbientLux - 10.0f, 400.0f);
            DarkenDeltaLuxMin = DarkenDeltaLuxMax;
        } else if (this.mAmbientLux >= 10.0f) {
            DarkenDeltaLuxMax = Math.min(this.mAmbientLux - 5.0f, 95.0f);
            DarkenDeltaLuxMin = DarkenDeltaLuxMax;
        } else {
            DarkenDeltaLuxMax = Math.min(this.mAmbientLux, 5.0f);
            DarkenDeltaLuxMin = DarkenDeltaLuxMax;
        }
        DarkenDeltaLuxMax *= 1.0f + ((DarkenDeltaLuxPara * (Stability - 5.0f)) / 100.0f);
    }

    public void setBrightenThreshold() {
        if (this.mAmbientLux >= 1000.0f) {
            BrightenDeltaLuxMax = 989.0f;
            BrightenDeltaLuxMin = BrightenDeltaLuxMax;
        } else if (this.mAmbientLux >= 500.0f) {
            BrightenDeltaLuxMax = (0.5f * this.mAmbientLux) + 489.0f;
            BrightenDeltaLuxMin = BrightenDeltaLuxMax;
        } else if (this.mAmbientLux >= 100.0f) {
            BrightenDeltaLuxMax = (0.5f * this.mAmbientLux) + 489.0f;
            BrightenDeltaLuxMin = (1.375f * this.mAmbientLux) + 51.5f;
        } else if (this.mAmbientLux >= 10.0f) {
            BrightenDeltaLuxMax = Math.min((20.0f * this.mAmbientLux) - 181.0f, (4.0f * this.mAmbientLux) + 139.0f);
            BrightenDeltaLuxMin = Math.min((this.mAmbientLux * 5.0f) - 31.0f, (1.5f * this.mAmbientLux) + 39.0f);
        } else if (this.mAmbientLux >= 2.0f) {
            BrightenDeltaLuxMax = (0.5f * this.mAmbientLux) + 14.0f;
            BrightenDeltaLuxMin = BrightenDeltaLuxMax;
        } else {
            BrightenDeltaLuxMax = 15.0f;
            BrightenDeltaLuxMin = BrightenDeltaLuxMax;
        }
        BrightenDeltaLuxMax *= 1.0f + ((BrightenDeltaLuxPara * (Stability - 5.0f)) / 100.0f);
    }

    protected long nextAmbientLightBrighteningTransition(long time) {
        if (NEED_NEW_FILTER_ALGORITHM) {
            return nextAmbientLightBrighteningTransitionForNewPolicy(time);
        }
        long earliestValidTime = time;
        int i = this.mAmbientLightRingBuffer.size() - 1;
        while (i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > this.mBrighteningLuxThreshold) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
            i--;
        }
        return getNextAmbientLightBrighteningTime(earliestValidTime);
    }

    private long nextAmbientLightBrighteningTransitionForNewPolicy(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean BrightenChange;
            float BrightenDeltaLux = this.mAmbientLightRingBufferFilter.getLux(i) - this.mAmbientLux;
            if (BrightenDeltaLux > BrightenDeltaLuxMax) {
                BrightenChange = true;
            } else if (BrightenDeltaLux <= BrightenDeltaLuxMin || Stability >= 50.0f) {
                BrightenChange = false;
            } else {
                BrightenChange = true;
            }
            if (!BrightenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return this.mBrighteningLightDebounceConfig + earliestValidTime;
    }

    protected long nextAmbientLightDarkeningTransition(long time) {
        if (NEED_NEW_FILTER_ALGORITHM) {
            return nextAmbientLightDarkeningTransitionForNewPolicy(time);
        }
        long earliestValidTime = time;
        int i = this.mAmbientLightRingBuffer.size() - 1;
        while (i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < this.mDarkeningLuxThreshold) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
            i--;
        }
        return getNextAmbientLightDarkeningTime(earliestValidTime);
    }

    private long nextAmbientLightDarkeningTransitionForNewPolicy(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean DarkenChange;
            float DeltaLux = this.mAmbientLux - this.mAmbientLightRingBufferFilter.getLux(i);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" mAmbientLux =");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(",mAmbientLightRingBufferFilter.getLux(i)=");
            stringBuilder.append(this.mAmbientLightRingBufferFilter.getLux(i));
            stringBuilder.append(", Stability=");
            stringBuilder.append(Stability);
            Slog.d("filter", stringBuilder.toString());
            if (DeltaLux >= DarkenDeltaLuxMax && Stability < 15.0f) {
                DarkenChange = true;
            } else if (DeltaLux < DarkenDeltaLuxMin || Stability >= 5.0f) {
                DarkenChange = false;
            } else {
                DarkenChange = true;
            }
            if (!DarkenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return this.mDarkeningLightDebounceConfig + earliestValidTime;
    }

    protected void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.push(time, this.mLastObservedLux);
        this.mAmbientLightRingBuffer.prune(time - ((long) this.mAmbientLightHorizon));
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        long timeWhenSensorWarmedUp;
        float ambientLux;
        boolean needToBrighten;
        long nextTransitionTime;
        long j = time;
        boolean needToDarken = false;
        if (!this.mAmbientLuxValid) {
            timeWhenSensorWarmedUp = ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime;
            if (j < timeWhenSensorWarmedUp) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAmbientLux: Sensor not  ready yet: time=");
                    stringBuilder.append(j);
                    stringBuilder.append(", timeWhenSensorWarmedUp=");
                    stringBuilder.append(timeWhenSensorWarmedUp);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mHandler.sendEmptyMessageAtTime(1, timeWhenSensorWarmedUp);
                return;
            }
            ambientLux = calculateAmbientLux(j, 2000);
            updateBuffer(j, ambientLux, 10000);
            if (NEED_NEW_FILTER_ALGORITHM) {
                this.mAmbientLightRingBufferFilter.push(j, ambientLux);
                this.mAmbientLightRingBufferFilter.prune(j - ((long) this.mAmbientLightHorizon));
            }
            setAmbientLux(ambientLux);
            this.mAmbientLuxValid = true;
            if (this.mWakeupFromSleep) {
                this.mWakeupFromSleep = false;
                this.mFirstAutoBrightness = true;
            }
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateAmbientLux: Initializing: mAmbientLightRingBuffer=");
                stringBuilder2.append(this.mAmbientLightRingBuffer);
                stringBuilder2.append(", mAmbientLux=");
                stringBuilder2.append(this.mAmbientLux);
                Slog.d(str2, stringBuilder2.toString());
            }
            updateAutoBrightness(true);
        }
        ambientLux = calculateAmbientLux(j, 2000);
        updateBuffer(j, ambientLux, 10000);
        if (NEED_NEW_FILTER_ALGORITHM) {
            this.mAmbientLightRingBufferFilter.push(j, ambientLux);
            this.mAmbientLightRingBufferFilter.prune(j - ((long) this.mAmbientLightHorizon));
            updatepara(this.mAmbientLightRingBuffer.calculateStability());
            setDarkenThreshold();
            setBrightenThreshold();
        }
        timeWhenSensorWarmedUp = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        float slowAmbientLux = calculateAmbientLux(j, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        float fastAmbientLux = calculateAmbientLux(j, 2000);
        if (NEED_NEW_FILTER_ALGORITHM) {
            if (slowAmbientLux - this.mAmbientLux >= BrightenDeltaLuxMax) {
                needToBrighten = true;
            } else if (slowAmbientLux - this.mAmbientLux < BrightenDeltaLuxMin || Stability >= 50.0f) {
                needToBrighten = false;
            } else {
                needToBrighten = true;
            }
            boolean z = needToBrighten && timeWhenSensorWarmedUp <= j;
            needToBrighten = z;
            if (this.mAmbientLux - slowAmbientLux >= DarkenDeltaLuxMax) {
                z = true;
            } else if (this.mAmbientLux - slowAmbientLux < DarkenDeltaLuxMin || Stability >= 5.0f) {
                z = false;
            } else {
                z = true;
            }
            if (z && nextDarkenTransition <= j) {
                needToDarken = true;
            }
        } else {
            needToBrighten = slowAmbientLux >= this.mBrighteningLuxThreshold && fastAmbientLux >= this.mBrighteningLuxThreshold && timeWhenSensorWarmedUp <= j;
            if (slowAmbientLux <= this.mDarkeningLuxThreshold && fastAmbientLux <= this.mDarkeningLuxThreshold && nextDarkenTransition <= j) {
                needToDarken = true;
            }
        }
        if ((needToBrighten | needToDarken) != 0) {
            setAmbientLux(fastAmbientLux);
            if (DEBUG) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("updateAmbientLux: ");
                stringBuilder3.append(fastAmbientLux > this.mAmbientLux ? "Brightened" : "Darkened");
                stringBuilder3.append(": mBrighteningLuxThreshold=");
                stringBuilder3.append(this.mBrighteningLuxThreshold);
                stringBuilder3.append(", mAmbientLightRingBuffer=");
                stringBuilder3.append(this.mAmbientLightRingBuffer);
                stringBuilder3.append(", mAmbientLux=");
                stringBuilder3.append(this.mAmbientLux);
                Slog.d(str3, stringBuilder3.toString());
            }
            updateAutoBrightness(true);
            timeWhenSensorWarmedUp = nextAmbientLightBrighteningTransition(time);
            nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        }
        long nextTransitionTime2 = Math.min(nextDarkenTransition, timeWhenSensorWarmedUp);
        if (nextTransitionTime2 > j) {
            long j2 = timeWhenSensorWarmedUp;
            nextTransitionTime = nextTransitionTime2;
        } else {
            nextTransitionTime = ((long) this.mNormalLightSensorRate) + j;
        }
        boolean z2 = DEBUG;
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime);
    }

    protected void updateAutoBrightness(boolean sendUpdate) {
        if (this.mAmbientLuxValid) {
            int newScreenAutoBrightness = getAdjustLightValByPgMode(getPowerSavingBrightness(clampScreenBrightness(Math.round(255.0f * this.mScreenAutoBrightnessSpline.interpolate(this.mAmbientLux)))));
            if (this.mScreenAutoBrightness != newScreenAutoBrightness || this.mFirstAutoBrightness || this.mFirstBrightnessAfterProximityNegative) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAutoBrightness: mScreenAutoBrightness=");
                    stringBuilder.append(this.mScreenAutoBrightness);
                    stringBuilder.append(", newScreenAutoBrightness=");
                    stringBuilder.append(newScreenAutoBrightness);
                    Slog.d(str, stringBuilder.toString());
                }
                if (newScreenAutoBrightness > this.mScreenAutoBrightness) {
                    this.mBrightnessEnlarge = true;
                } else {
                    this.mBrightnessEnlarge = false;
                }
                this.mScreenAutoBrightness = newScreenAutoBrightness;
                this.mFirstAutoBrightness = false;
                this.mFirstBrightnessAfterProximityNegative = false;
                this.mUpdateAutoBrightnessCount++;
                if (this.mUpdateAutoBrightnessCount == HwBootFail.STAGE_BOOT_SUCCESS) {
                    this.mUpdateAutoBrightnessCount = 2;
                    Slog.i(TAG, "mUpdateAutoBrightnessCount == Integer.MAX_VALUE,so set it be 2");
                }
                if (sendUpdate) {
                    this.mCallbacks.updateBrightness();
                }
            }
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "mAmbientLuxValid= false,sensor is not ready");
        }
    }

    public void updateAutoDBWhenSameBrightness(int brightness) {
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void prepareBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mHandler.removeMessages(2);
        } else {
            this.mBrightnessAdjustmentSamplePending = true;
            this.mBrightnessAdjustmentSampleOldLux = this.mAmbientLuxValid ? this.mAmbientLux : -1.0f;
            this.mBrightnessAdjustmentSampleOldBrightness = this.mScreenAutoBrightness;
        }
        this.mHandler.sendEmptyMessageDelayed(2, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void cancelBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            this.mHandler.removeMessages(2);
        }
    }

    private void collectBrightnessAdjustmentSample() {
        if (this.mBrightnessAdjustmentSamplePending) {
            this.mBrightnessAdjustmentSamplePending = false;
            if (this.mAmbientLuxValid && this.mScreenAutoBrightness >= 0) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Auto-brightness adjustment changed by user: lux=");
                    stringBuilder.append(this.mAmbientLux);
                    stringBuilder.append(", brightness=");
                    stringBuilder.append(this.mScreenAutoBrightness);
                    stringBuilder.append(", ring=");
                    stringBuilder.append(this.mAmbientLightRingBuffer);
                    Slog.d(str, stringBuilder.toString());
                }
                EventLog.writeEvent(EventLogTags.AUTO_BRIGHTNESS_ADJ, new Object[]{Float.valueOf(this.mBrightnessAdjustmentSampleOldLux), Integer.valueOf(this.mBrightnessAdjustmentSampleOldBrightness), Float.valueOf(this.mAmbientLux), Integer.valueOf(this.mScreenAutoBrightness)});
            }
        }
    }

    protected float getBrighteningLuxThreshold() {
        return this.mBrighteningLuxThreshold;
    }

    protected float getDarkeningLuxThreshold() {
        return this.mDarkeningLuxThreshold;
    }

    protected float getDarkeningLightHystersis() {
        return DARKENING_LIGHT_HYSTERESIS;
    }

    protected float getBrighteningLightHystersis() {
        return BRIGHTENING_LIGHT_HYSTERESIS;
    }

    protected boolean calcNeedToBrighten(float ambient) {
        return true;
    }

    protected boolean calcNeedToDarken(float ambient) {
        return true;
    }

    protected long getNextAmbientLightBrighteningTime(long earliedtime) {
        return BRIGHTENING_LIGHT_DEBOUNCE + earliedtime;
    }

    protected long getNextAmbientLightDarkeningTime(long earliedtime) {
        return this.DARKENING_LIGHT_DEBOUNCE + earliedtime;
    }

    public void setPowerStatus(boolean powerStatus) {
    }

    protected boolean interceptHandleLightSensorEvent(long time, float lux) {
        return false;
    }

    protected void saveOffsetAlgorithmParas() {
    }

    protected void updateIntervenedAutoBrightness(int brightness) {
        this.mScreenAutoBrightness = brightness;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update IntervenedAutoBrightness:mScreenAutoBrightness = ");
            stringBuilder.append(this.mScreenAutoBrightness);
            Slog.d(str, stringBuilder.toString());
        }
    }

    protected void clearFilterAlgoParas() {
    }

    protected void updatepara(AmbientLightRingBuffer mAmbientLightRingBuffer) {
    }

    protected void updateBuffer(long time, float ambientLux, int horizon) {
    }

    protected boolean decideToBrighten(float ambientLux) {
        return ambientLux >= this.mBrighteningLuxThreshold && calcNeedToBrighten(ambientLux);
    }

    protected boolean decideToDarken(float ambientLux) {
        return ambientLux <= this.mDarkeningLuxThreshold && calcNeedToDarken(ambientLux);
    }

    protected float getLuxStability() {
        return 0.0f;
    }

    public long getLightSensorEnableTime() {
        return this.mLightSensorEnableTime;
    }

    protected void updateBrightnessIfNoAmbientLuxReported() {
    }

    public int getUpdateAutoBrightnessCount() {
        return this.mUpdateAutoBrightnessCount;
    }

    public void updateCurrentUserId(int userId) {
    }

    protected SensorManager getSensorManager() {
        return this.mSensorManager;
    }

    public void updatePowerPolicy(int policy) {
    }

    public boolean getPowerStatus() {
        return false;
    }

    public void setCoverModeStatus(boolean isclosed) {
    }

    public boolean getCoverModeFastResponseFlag() {
        return false;
    }

    public void setBackSensorCoverModeBrightness(int brightness) {
    }

    public void setCameraModeBrightnessLineEnable(boolean cameraModeBrightnessLineEnable) {
    }

    public boolean getCameraModeChangeAnimationEnable() {
        return false;
    }

    public boolean getReadingModeChangeAnimationEnable() {
        return false;
    }

    public boolean getReadingModeBrightnessLineEnable() {
        return false;
    }

    public boolean getRebootAutoModeEnable() {
        return false;
    }

    public void setKeyguardLockedStatus(boolean isLocked) {
    }

    public boolean getOutdoorAnimationFlag() {
        return false;
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        return 0;
    }

    public void setMaxBrightnessFromThermal(int brightness) {
    }

    public void setMaxBrightnessFromCryogenic(int brightness) {
    }

    public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
        return brightness;
    }

    public void setManualModeEnableForPg(boolean manualModeEnableForPg) {
    }

    public boolean getRebootFirstBrightnessAnimationEnable() {
        return false;
    }

    protected int getAdjustLightValByPgMode(int rawLightVal) {
        return rawLightVal;
    }

    protected int getPowerSavingBrightness(int brightness) {
        return brightness;
    }

    protected void setBrightnessLimitedByThermal(boolean isLimited) {
    }

    public void setBrightnessNoLimit(int brightness, int time) {
    }

    public int getAutoBrightnessBaseInOutDoorLimit(int brightness) {
        return brightness;
    }

    public boolean getDarkAdaptDimmingEnable() {
        return false;
    }

    public void clearDarkAdaptDimmingEnable() {
    }

    public boolean getAutoPowerSavingUseManualAnimationTimeEnable() {
        return false;
    }

    public boolean getAutoPowerSavingAnimationEnable() {
        return false;
    }

    public void setAutoPowerSavingAnimationEnable(boolean enable) {
    }

    public void getUserDragInfo(Bundle data) {
    }

    public void setPersonalizedBrightnessCurveLevel(int curveLevel) {
    }

    public void updateNewBrightnessCurveTmp() {
    }

    public void updateNewBrightnessCurve() {
    }

    public List<PointF> getCurrentDefaultNewCurveLine() {
        return null;
    }

    public boolean getAnimationGameChangeEnable() {
        return false;
    }
}

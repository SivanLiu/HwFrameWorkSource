package com.huawei.uifirst.smartslide;

import android.content.Context;
import android.util.Log;
import android.view.animation.AnimationUtils;
import java.util.HashMap;

public class SmartSlideOverScroller {
    private static final String DECELERATION_TIME_CONSTANT = "decelerationTimeConstant";
    private static final float DECELERATION_TIME_CONSTANT_DEFAULT = -0.405f;
    private static final String DECELERATION_TIME_SLOPE = "decelerationTimeSlope";
    private static final float DECELERATION_TIME_SLOPE_DEFAULT = 0.528f;
    private static final float DEFAULT_LCD_LENGTH = 6.0f;
    private static final float DEFAULT_MULTIPLE_FLING_LENGTH_THRESHOLD = 125.0f;
    private static final float DEFAULT_MULTIPLE_FLING_TIME_THRESHOLD = 400.0f;
    private static final float DEFAULT_MULTIPLE_VELOCITY = 1.5f;
    private static final float DEFAULT_SCREEN_DISPLAY_HEIGHT = 1920.0f;
    private static final float DEFAULT_SCREEN_PPI = 402.0f;
    private static final String EXP_COEFFICIENT = "expCoefficient";
    private static final String EXP_COFFICIENT_SLOW_DOWN = "expCofficientSlowDown";
    private static final float FLING_LENGTH_FACTOR = 0.31095f;
    private static final int FLING_TIMES_BIG_THAN_NINE = 9;
    private static final int FLING_TIMES_INIT = 1;
    private static final int FLING_TIMES_UNDER_FIVE = 5;
    private static final String FLING_TIME_THRESHOLD = "flingTimeThreshold";
    private static final float INCH_TO_MM_FACTOR = 25.4f;
    private static final String IS_ENABLE = "isEnable";
    private static final int LAST_FLING_TIME_INIT = 0;
    private static final float LAST_VELOCITY_GAIN_INIT = 1.0f;
    private static final String LOG_TAG = "OverScrollerOptimization";
    private static final int MAX_FLING_VELOCITY_GAIN = 16;
    private static final int NB_SAMPLES_OPTIMIZATION = 600;
    private static final float SLOPE_PARAMETER_GET_DISTANCE_DEFAULT = 4.2f;
    private static final float SLOPE_PARAMETER_SLOW_DOWN_DEFAULT = 6.0f;
    private static final float SPLINE_DISTANCE_COMPLETE = 0.9f;
    private static final float VELOCITY_GAIN_FACTOR = 0.45f;
    private static final float VELOCITY_GAIN_UNDER_FIVE = 1.0f;
    private static final String VELOCITY_MULTIPLIER = "velocityMultiplier";
    private static boolean mIsApplicationEnable = false;
    private static float mSlopeParameterGetDistance;
    private static float mSlopeParameterSlowDown;
    private HashMap<String, String> mConfigData;
    private float mDecelerationTimeConstantGetTime;
    private float mDecelerationTimeSlopeGetTime;
    private long mLastFlingTimeSave = 0;
    private double mLastVelocityGainSave;
    private float mLcdLength;
    private float mMultipleFlingLengthThreshold;
    private int mMultipleFlingTIMES = 0;
    private float mMultipleFlingTimeThreshold;
    private float mMultipleVelocity;
    private float mScreenDisplayHeightPixels;
    private float mScreenPPI;
    private SmartSlideOverScrollerConfig mSmartSlideOverScrollerConfig;

    public SmartSlideOverScroller(Context context) {
        Log.i(LOG_TAG, "start init SmartSlideOverScroller and get the overscroller config");
        if (this.mSmartSlideOverScrollerConfig == null) {
            this.mSmartSlideOverScrollerConfig = new SmartSlideOverScrollerConfig();
        }
        this.mConfigData = new HashMap();
        initOverScrollerConfig(context);
    }

    private void initOverScrollerConfig(Context context) {
        float f = 6.0f;
        if (context != null) {
            this.mConfigData = this.mSmartSlideOverScrollerConfig.getOverScrollerConfig(context);
            this.mScreenDisplayHeightPixels = this.mSmartSlideOverScrollerConfig.getScreenHeight_ByResources(context);
            this.mScreenDisplayHeightPixels = this.mScreenDisplayHeightPixels < 0.0f ? DEFAULT_SCREEN_DISPLAY_HEIGHT : this.mScreenDisplayHeightPixels;
            this.mLcdLength = Float.parseFloat(this.mSmartSlideOverScrollerConfig.getScreenSize_ByDeviceFile());
            this.mLcdLength = this.mLcdLength <= 0.0f ? 6.0f : this.mLcdLength;
            this.mScreenPPI = this.mSmartSlideOverScrollerConfig.getScreenPPI_ByResources(context);
            this.mScreenPPI = this.mScreenPPI <= 0.0f ? DEFAULT_SCREEN_PPI : this.mScreenPPI;
            this.mMultipleFlingLengthThreshold = this.mSmartSlideOverScrollerConfig.getScreenPPI_ByResources(context) * FLING_LENGTH_FACTOR;
            this.mMultipleFlingLengthThreshold = this.mMultipleFlingLengthThreshold <= 0.0f ? DEFAULT_MULTIPLE_FLING_LENGTH_THRESHOLD : this.mMultipleFlingLengthThreshold;
            mSlopeParameterGetDistance = Float.parseFloat((String) this.mConfigData.get(EXP_COEFFICIENT));
            boolean isGetDistanceReasonable = mSlopeParameterGetDistance >= 2.1999998f && mSlopeParameterGetDistance <= 6.2f;
            mSlopeParameterGetDistance = isGetDistanceReasonable ? mSlopeParameterGetDistance : SLOPE_PARAMETER_GET_DISTANCE_DEFAULT;
            this.mDecelerationTimeSlopeGetTime = Float.parseFloat((String) this.mConfigData.get(DECELERATION_TIME_SLOPE));
            this.mDecelerationTimeConstantGetTime = Float.parseFloat((String) this.mConfigData.get(DECELERATION_TIME_CONSTANT));
            this.mMultipleFlingTimeThreshold = Float.parseFloat((String) this.mConfigData.get(FLING_TIME_THRESHOLD));
            this.mMultipleFlingTimeThreshold = this.mMultipleFlingTimeThreshold <= 0.0f ? DEFAULT_MULTIPLE_FLING_TIME_THRESHOLD : this.mMultipleFlingTimeThreshold;
            mSlopeParameterSlowDown = Float.parseFloat((String) this.mConfigData.get(EXP_COFFICIENT_SLOW_DOWN));
            boolean isSlowDownReasonable = mSlopeParameterSlowDown >= 4.0f && mSlopeParameterSlowDown <= 8.0f;
            if (isSlowDownReasonable) {
                f = mSlopeParameterSlowDown;
            }
            mSlopeParameterSlowDown = f;
            this.mMultipleVelocity = Float.parseFloat((String) this.mConfigData.get(VELOCITY_MULTIPLIER));
            this.mMultipleVelocity = this.mMultipleVelocity < DEFAULT_MULTIPLE_VELOCITY ? DEFAULT_MULTIPLE_VELOCITY : this.mMultipleVelocity;
            mIsApplicationEnable = Boolean.parseBoolean((String) this.mConfigData.get(IS_ENABLE));
            return;
        }
        this.mScreenDisplayHeightPixels = DEFAULT_SCREEN_DISPLAY_HEIGHT;
        this.mLcdLength = 6.0f;
        this.mScreenPPI = DEFAULT_SCREEN_PPI;
        this.mMultipleFlingLengthThreshold = DEFAULT_MULTIPLE_FLING_LENGTH_THRESHOLD;
        mSlopeParameterGetDistance = SLOPE_PARAMETER_GET_DISTANCE_DEFAULT;
        this.mDecelerationTimeSlopeGetTime = DECELERATION_TIME_SLOPE_DEFAULT;
        this.mDecelerationTimeConstantGetTime = DECELERATION_TIME_CONSTANT_DEFAULT;
        this.mMultipleFlingTimeThreshold = DEFAULT_MULTIPLE_FLING_TIME_THRESHOLD;
        mSlopeParameterSlowDown = 6.0f;
        this.mMultipleVelocity = DEFAULT_MULTIPLE_VELOCITY;
        mIsApplicationEnable = false;
    }

    public float getScreenPPI() {
        return this.mScreenPPI;
    }

    public boolean getAppEnable() {
        return mIsApplicationEnable;
    }

    public int fling(int startX, int startY, int velocityX, int velocityY, float oldVelocityX, float oldVelocityY, int minX, int maxX, int minY, int maxY, int overX, int overY, float slidingDistance) {
        long time = AnimationUtils.currentAnimationTimeMillis();
        if (this.mLastFlingTimeSave == 0) {
            this.mMultipleFlingTIMES = 1;
            this.mLastVelocityGainSave = 1.0d;
            this.mLastFlingTimeSave = time;
            return (int) (this.mMultipleVelocity * ((float) velocityY));
        }
        float delValueX = Math.abs(Math.signum((float) velocityX) - Math.signum(oldVelocityX));
        float delValueY = Math.abs(Math.signum((float) velocityY) - Math.signum(oldVelocityY));
        if (delValueX > 1.0E-7f || delValueY > 1.0E-7f) {
            this.mMultipleFlingTIMES = 1;
            this.mLastFlingTimeSave = 0;
            this.mLastVelocityGainSave = 1.0d;
        }
        if (((float) Math.abs(time - this.mLastFlingTimeSave)) > this.mMultipleFlingTimeThreshold) {
            this.mMultipleFlingTIMES = 1;
            this.mLastFlingTimeSave = 0;
            this.mLastVelocityGainSave = 1.0d;
        } else if (Math.abs(slidingDistance) >= this.mMultipleFlingLengthThreshold) {
            this.mMultipleFlingTIMES++;
            this.mLastFlingTimeSave = time;
        }
        if (Math.signum((float) velocityX) == Math.signum(oldVelocityX) && Math.signum((float) velocityY) == Math.signum(oldVelocityY)) {
            if (this.mMultipleFlingTIMES < 5) {
                velocityY = (int) (((double) velocityY) * 1.0d);
                this.mLastVelocityGainSave = 1.0d;
            } else if (this.mMultipleFlingTIMES > 9) {
                velocityGain = (double) ((float) (((double) ((Math.abs(slidingDistance) * ((float) (this.mMultipleFlingTIMES - 1))) / this.mScreenDisplayHeightPixels)) + this.mLastVelocityGainSave));
                if (16.0d <= velocityGain) {
                    velocityGain = 16.0d;
                }
                velocityY = (int) (((double) velocityY) * velocityGain);
                this.mLastVelocityGainSave = velocityGain;
            } else {
                double velocityGainMax = ((((double) ((this.mMultipleFlingTIMES - 3) * (this.mMultipleFlingTIMES + 2))) / 2.0d) * 0.44999998807907104d) + 1.0d;
                velocityGain = (double) ((float) (((double) ((Math.abs(slidingDistance) * ((float) (this.mMultipleFlingTIMES - 1))) / this.mScreenDisplayHeightPixels)) + this.mLastVelocityGainSave));
                if (velocityGainMax <= velocityGain) {
                    velocityGain = velocityGainMax;
                }
                velocityY = (int) (((double) velocityY) * velocityGain);
                this.mLastVelocityGainSave = velocityGain;
            }
        }
        return (int) (this.mMultipleVelocity * ((float) velocityY));
    }

    public double getSplineFlingDistance(int velocity) {
        return ((double) (((float) Math.abs(velocity)) / mSlopeParameterGetDistance)) * (1.0d - Math.exp(((double) (-mSlopeParameterGetDistance)) * ((double) getSplineFlingDuration(velocity))));
    }

    public double getDistanceDiff(int velocity) {
        return ((double) Math.signum((float) velocity)) * ((((double) (((float) Math.abs(velocity)) / mSlopeParameterGetDistance)) * (1.0d - Math.exp((double) ((-mSlopeParameterGetDistance) * ((float) getSplineFlingDuration(velocity)))))) - (((double) (((float) Math.abs(velocity)) / mSlopeParameterGetDistance)) * (1.0d - Math.exp((double) ((-mSlopeParameterGetDistance) * ((float) getSplineFlingDuration(velocity)))))));
    }

    public int getSplineFlingDuration(int velocity) {
        double flingTime = (((double) this.mDecelerationTimeSlopeGetTime) * Math.log((double) ((((float) Math.abs(velocity)) / this.mScreenPPI) * INCH_TO_MM_FACTOR))) + ((double) this.mDecelerationTimeConstantGetTime);
        if (flingTime < 0.0d) {
            flingTime = 1.0d;
        }
        Log.d(LOG_TAG, "fling time is flingTime = " + flingTime + " velocity = " + velocity);
        return (int) (1000.0d * flingTime);
    }

    public double getUpdateDistance(long currentTime, int splineDuration, int splineDistance, double splineDistanceDiff) {
        float distanceCoef = (((float) Math.exp((double) (((-mSlopeParameterSlowDown) * 595.0f) / 600.0f))) + 1.0f) - ((float) Math.exp((double) ((-mSlopeParameterSlowDown) * (((float) currentTime) / ((float) splineDuration)))));
        double distance = ((double) (((float) splineDistance) * distanceCoef)) - (splineDistanceDiff / 600.0d);
        if (Math.abs(distance) < ((double) (((float) Math.abs(splineDistance)) * SPLINE_DISTANCE_COMPLETE))) {
            return distance;
        }
        if (distanceCoef >= 1.0f) {
            distanceCoef = 1.0f;
        }
        return (double) (((float) splineDistance) * distanceCoef);
    }

    public float getUpdateVelocity(long currentTime, int splineDuration, int velocity) {
        return ((float) velocity) * ((float) Math.exp((double) ((-mSlopeParameterSlowDown) * (((float) currentTime) / ((float) splineDuration)))));
    }
}

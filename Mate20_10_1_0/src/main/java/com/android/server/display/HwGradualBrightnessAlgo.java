package com.android.server.display;

import android.animation.ValueAnimator;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader;

/* access modifiers changed from: package-private */
public final class HwGradualBrightnessAlgo {
    private static final float CRITERION_TIME = 40.0f;
    private static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final float DEFAULT_AMOUNT = 157.0f;
    private static final float FAST_TIME = 0.5f;
    private static final int FRAME_RATE_LEVEL0 = 60;
    private static final float FRAME_RATE_LEVEL0_TIME_DELTA_MIN = 0.016f;
    private static final int FRAME_RATE_LEVEL1 = 90;
    private static final float FRAME_RATE_LEVEL1_TIME_DELTA_MIN = 0.011f;
    private static final int FRAME_RATE_LEVEL2 = 120;
    private static final float FRAME_RATE_LEVEL2_TIME_DELTA_MIN = 0.008f;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final float JND_ADJ_PARA = 0.09f;
    private static final float JND_CURVE_PARA = 0.0029f;
    private static final float MAX_BRIGHTNESS = 10000.0f;
    private static final String TAG = "HwGradualBrightnessAlgo";
    private static final float TIME_DELTA = 0.016666668f;
    private float mAnimatedStep = 1.0f;
    private boolean mAnimatedStepRoundEnabled = false;
    private float mAnimatedValue;
    private boolean mAnimationEnabled = false;
    private float mAnimationEqualRatioMa;
    private float mAnimationEqualRatioMb;
    private float mAnimationEqualRatioMq;
    private float mAnimationEqualRatioMq0;
    private float mAnimationEqualRatioMqDefault = 0.99839747f;
    public boolean mAutoBrightnessIntervened = false;
    public boolean mAutoBrightnessMode;
    private float mAutoDuration = 0.0f;
    private StepType mAutoStepType = StepType.LINEAR;
    private float mBrightenFixStepsThreshold = 2.0f;
    private float mBrightnessMax = MAX_BRIGHTNESS;
    private float mBrightnessMin = 156.0f;
    private boolean mBrightnessModeAnimationEnable = false;
    private int mBrightnessModeAnimationTime = 500;
    private boolean mCameraModeEnable = false;
    private boolean mCoverModeAnimationFast = false;
    private float mCoverModeAnimationTime = 1.0f;
    private int mCurrentValue;
    private boolean mDarkAdaptAnimationDimmingEnable;
    private float mDarkenFixStepsThreshold = 20.0f;
    private final HwBrightnessXmlLoader.Data mData = HwBrightnessXmlLoader.getData();
    private float mDecreaseFixAmount;
    private float mDuration = 0.0f;
    private boolean mFastDarkenDimmingEnable = false;
    private boolean mFirstRebootAnimationEnable = true;
    private boolean mFirstTimeCalculateAmount = false;
    public boolean mFirstValidAutoBrightness = false;
    private int mFrameRate = 60;
    private boolean mFrontCameraDimmingEnable = false;
    private boolean mGameModeEnable = false;
    private boolean mKeyguardUnlockedFastDarkenDimmingEnable = false;
    private float mManualDuration = 0.0f;
    private boolean mManualModeAnimationEnable = false;
    private boolean mManualPowerSavingAnimationEnable = false;
    private boolean mManualThermalModeAnimationEnable = false;
    private int mMillisecond;
    private boolean mNightUpPowerOnWithDimmingEnable = false;
    private boolean mOutdoorAnimationFlag = false;
    private boolean mPowerDimRecoveryState = false;
    private boolean mPowerDimState = false;
    private int mRate;
    private boolean mReadingModeEnable = false;
    private boolean mScreenLocked = false;
    private int mState;
    private float mStepAdjValue = 1.0f;
    private int mTargetValue;
    private float mTimeDelta = TIME_DELTA;
    private float mTimeDeltaMinValue = FRAME_RATE_LEVEL0_TIME_DELTA_MIN;
    private boolean mfastAnimtionFlag = false;

    private enum StepType {
        LINEAR,
        CURVE
    }

    static {
        boolean z = false;
        if (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) {
            z = true;
        }
        HWDEBUG = z;
    }

    public HwGradualBrightnessAlgo() {
        initAnimationEqualRatioPara();
        if (this.mData.rebootFirstBrightnessAnimationEnable) {
            this.mTargetValue = this.mData.rebootFirstBrightness;
            this.mCurrentValue = this.mData.rebootFirstBrightness;
            this.mAnimatedValue = (float) this.mData.rebootFirstBrightness;
        }
    }

    public void initAnimationEqualRatioPara() {
        this.mAnimationEqualRatioMa = (this.mData.screenBrightnessMaxNit - this.mData.screenBrightnessMinNit) / (this.mBrightnessMax - this.mBrightnessMin);
        this.mAnimationEqualRatioMb = this.mData.screenBrightnessMaxNit - (this.mBrightnessMax * this.mAnimationEqualRatioMa);
        float N_max = (this.mData.darkenGradualTimeMax * 60.0f) - 1.0f;
        if (Math.abs(N_max) < 1.0E-7f) {
            this.mAnimationEqualRatioMq0 = this.mAnimationEqualRatioMqDefault;
        } else {
            this.mAnimationEqualRatioMq0 = (float) Math.pow((double) (this.mData.screenBrightnessMinNit / this.mData.screenBrightnessMaxNit), (double) (1.0f / N_max));
        }
        if (DEBUG) {
            Slog.i(TAG, "Init AnimationEqualRatioPara: Ma=" + this.mAnimationEqualRatioMa + ",Mb=" + this.mAnimationEqualRatioMb + ",Nmax=" + N_max + ",Mq0=" + this.mAnimationEqualRatioMq0 + ",MaxNit=" + this.mData.screenBrightnessMaxNit + ",MinNit=" + this.mData.screenBrightnessMinNit);
        }
    }

    private float getAnimatedStepByEyeSensitiveCurve(float currentValue, float targetValue, float duration) {
        if (this.mData.animationEqualRatioEnable && currentValue > targetValue) {
            this.mAnimatedStep = getAnimatedStepByEqualRatio(currentValue, targetValue, duration);
            return this.mAnimatedStep;
        } else if (currentValue == 0.0f) {
            Slog.i(TAG, "currentValue is 0, set to target value!");
            return targetValue;
        } else {
            if (duration <= 0.116f && currentValue > targetValue) {
                Slog.e(TAG, "duration is not valid, set to 3.0!");
                duration = 3.0f;
            }
            if (this.mFirstTimeCalculateAmount) {
                this.mStepAdjValue = getFirstTimeAmountByEyeSensitiveCurve(currentValue, targetValue, duration);
            }
            this.mAnimatedStep = JND_CURVE_PARA * currentValue;
            this.mAnimatedStep *= (float) Math.pow((double) (targetValue / MAX_BRIGHTNESS), 0.09000000357627869d);
            if (duration >= 20.0f && duration < 30.0f) {
                duration += 1.0f;
            }
            this.mAnimatedStep = ((this.mAnimatedStep * this.mStepAdjValue) * CRITERION_TIME) / duration;
            this.mAnimatedStep = getMinAnimatedStepByEyeSensitiveCurve(this.mAnimatedStep);
            if (this.mScreenLocked && currentValue > targetValue && targetValue < this.mData.darkenTargetForKeyguard && this.mAnimatedStep < this.mData.minAnimatingStepForKeyguard) {
                if (HWDEBUG) {
                    Slog.i(TAG, "mScreenLocked mAnimatedStep=" + this.mAnimatedStep + ",minStep=" + this.mData.minAnimatingStepForKeyguard);
                }
                this.mAnimatedStep = this.mData.minAnimatingStepForKeyguard;
            } else if (currentValue > targetValue && targetValue < ((float) this.mData.darkenTargetFor255) && this.mAnimatedStep < this.mData.minAnimatingStep) {
                if (this.mFirstTimeCalculateAmount && this.mData.minAnimatingStep > 0.0f) {
                    float linearAvgTime = ((currentValue - targetValue) / this.mData.minAnimatingStep) * this.mTimeDelta;
                    if (HWFLOW) {
                        Slog.i(TAG, "DarkenByEyeSensitive ResetAnimatedStep=" + this.mAnimatedStep + "-->mAnimatedStep=" + this.mData.minAnimatingStep + ",linearAvgTime=" + linearAvgTime);
                    }
                }
                this.mAnimatedStep = this.mData.minAnimatingStep;
            }
            return this.mAnimatedStep;
        }
    }

    private float getAnimatedStepByEqualRatio(float currentValue, float targetValue, float duration) {
        float f = this.mAnimationEqualRatioMa;
        float f2 = this.mAnimationEqualRatioMb;
        if ((f * targetValue) + f2 < 1.0E-12f || (f * currentValue) + f2 < 1.0E-12f || this.mAnimationEqualRatioMq0 < 1.0E-12f) {
            Slog.e(TAG, "Error: the screen brightness is minus");
            return DEFAULT_AMOUNT;
        }
        if (this.mFirstTimeCalculateAmount) {
            float avgTime = ((float) ((Math.log((double) (((f * targetValue) + f2) / ((f * currentValue) + f2))) / Math.log((double) this.mAnimationEqualRatioMq0)) + 1.0d)) / 60.0f;
            if (avgTime < this.mData.darkenGradualTimeMin) {
                float f3 = this.mAnimationEqualRatioMa;
                float f4 = this.mAnimationEqualRatioMb;
                this.mAnimationEqualRatioMq = (float) Math.pow((double) (((f3 * targetValue) + f4) / ((f3 * currentValue) + f4)), (double) (1.0f / ((this.mData.darkenGradualTimeMin * 60.0f) - 1.0f)));
            } else {
                this.mAnimationEqualRatioMq = this.mAnimationEqualRatioMq0;
            }
            if (DEBUG) {
                Slog.i(TAG, "avgTime=" + avgTime + ",Mq=" + this.mAnimationEqualRatioMq + ",Ma=" + this.mAnimationEqualRatioMa + ",Mb=" + this.mAnimationEqualRatioMb + ",mAnimatedValue=" + this.mAnimatedValue);
            }
        }
        if (currentValue > targetValue) {
            float f5 = this.mAnimationEqualRatioMq;
            this.mAnimatedStep = ((1.0f - f5) * this.mAnimatedValue) + (((1.0f - f5) * this.mAnimationEqualRatioMb) / this.mAnimationEqualRatioMa);
        }
        if (this.mAnimatedStep < 1.0E-12f) {
            Slog.e(TAG, "Error: the animate step is invalid,mAnimatedStep=157.0");
            this.mAnimatedStep = DEFAULT_AMOUNT;
        }
        return this.mAnimatedStep;
    }

    private float getFirstTimeAmountByEyeSensitiveCurve(float currentValue, float targetValue, float duration) {
        float avgTime;
        float avgPara = ((((float) Math.pow((double) (targetValue / MAX_BRIGHTNESS), 0.09000000357627869d)) * JND_CURVE_PARA) * CRITERION_TIME) / duration;
        float stepAdjValue = 1.0f;
        if (currentValue > targetValue) {
            avgTime = ((float) (Math.log((double) (targetValue / currentValue)) / Math.log((double) (1.0f - avgPara)))) * this.mTimeDelta;
            float avgDimmingTimeMin = this.mData.darkenGradualTimeMin;
            if (targetValue >= ((float) this.mData.darkenTargetFor255) && targetValue < this.mData.darkenNoFlickerTarget && avgTime < this.mData.darkenNoFlickerTargetGradualTimeMin) {
                avgDimmingTimeMin = this.mData.darkenNoFlickerTargetGradualTimeMin;
            }
            if (avgTime < avgDimmingTimeMin) {
                stepAdjValue = avgTime / avgDimmingTimeMin;
            }
            Slog.i(TAG, "DarkenByEyeSensitive avgTime=" + avgTime + ",avgDimmingTimeMin=" + avgDimmingTimeMin);
        } else {
            avgTime = ((float) (Math.log((double) (targetValue / currentValue)) / Math.log((double) (avgPara + 1.0f)))) * this.mTimeDelta;
            if (avgTime < duration) {
                stepAdjValue = avgTime / duration;
            }
        }
        if (HWFLOW) {
            Slog.i(TAG, "getAnimatedStep avgTime= " + avgTime + ",avgPara" + avgPara + ",stepAdjValue=" + stepAdjValue + ",duration=" + duration);
        }
        return stepAdjValue;
    }

    private float getMinAnimatedStepByEyeSensitiveCurve(float animatedStep) {
        if (animatedStep >= 1.0f && this.mAnimatedStepRoundEnabled) {
            return (float) Math.round(animatedStep);
        }
        if (animatedStep < 1.0f && animatedStep >= 0.5f && this.mStepAdjValue == 1.0f) {
            return 0.5f;
        }
        if (animatedStep >= 0.5f || this.mStepAdjValue != 1.0f) {
            return animatedStep;
        }
        return 0.25f;
    }

    public float getAnimatedValue() {
        float amount;
        if (ValueAnimator.getDurationScale() == 0.0f || this.mRate == 0) {
            this.mAnimatedValue = (float) this.mTargetValue;
        } else {
            if (this.mAutoBrightnessMode) {
                amount = getAutoModeAnimtionAmount();
                this.mDuration = this.mAutoDuration;
            } else {
                amount = getManualModeAnimtionAmount();
                this.mDuration = this.mManualDuration;
            }
            int i = this.mTargetValue;
            if (i > this.mCurrentValue) {
                float f = this.mAnimatedValue;
                this.mAnimatedValue = f + amount < ((float) i) ? f + amount : (float) i;
            } else {
                float f2 = this.mAnimatedValue;
                this.mAnimatedValue = f2 - amount > ((float) i) ? f2 - amount : (float) i;
            }
        }
        return this.mAnimatedValue;
    }

    private StepType getAutoModeAnimtionStepType(float duration) {
        if (this.mDarkAdaptAnimationDimmingEnable) {
            return StepType.LINEAR;
        }
        if (this.mCurrentValue < this.mData.linearDimmingValueTh && this.mTargetValue < this.mData.darkenTargetFor255) {
            if (HWFLOW) {
                Slog.i(TAG, "setDimming-LINEAR,mCurrentValue=" + this.mCurrentValue + ",mTargetValue=" + this.mTargetValue);
            }
            return StepType.LINEAR;
        } else if (!this.mData.useVariableStep || ((duration < this.mDarkenFixStepsThreshold || this.mTargetValue >= this.mCurrentValue) && (duration < this.mBrightenFixStepsThreshold || this.mTargetValue < this.mCurrentValue))) {
            return StepType.LINEAR;
        } else {
            return StepType.CURVE;
        }
    }

    public float getAutoModeAnimtionAmount() {
        float duration;
        float amount;
        float duration2;
        float duration3;
        if (!this.mFirstTimeCalculateAmount) {
            return getAmount();
        }
        if (this.mFirstRebootAnimationEnable && this.mData.rebootFirstBrightnessAnimationEnable) {
            duration = this.mData.rebootFirstBrightnessAutoTime;
            this.mFirstRebootAnimationEnable = false;
            Slog.i(TAG, "The mFirstRebootAnimationEnable state,duration=" + duration);
        } else if (this.mBrightnessModeAnimationEnable) {
            duration = ((float) this.mBrightnessModeAnimationTime) / 1000.0f;
            this.mBrightnessModeAnimationEnable = false;
            Slog.i(TAG, "The Auto mBrightnessModeAnimationEnable state,duration=" + duration);
        } else if (this.mAnimationEnabled) {
            duration = ((float) this.mMillisecond) / 1000.0f;
            if (DEBUG) {
                Slog.i(TAG, "The mAnimationEnabled state,duration=" + duration);
            }
        } else if (this.mCoverModeAnimationFast) {
            duration = this.mCoverModeAnimationTime;
            if (DEBUG) {
                Slog.i(TAG, "LabcCoverMode mCoverModeFast=" + this.mCoverModeAnimationFast);
            }
        } else if (this.mData.autoPowerSavingUseManualAnimationTimeEnable && this.mManualPowerSavingAnimationEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration = this.mData.manualPowerSavingAnimationDarkenTime;
            } else {
                duration = this.mData.manualPowerSavingAnimationBrightenTime;
            }
            this.mManualPowerSavingAnimationEnable = false;
            if (DEBUG) {
                Slog.i(TAG, "The autoPowerSavingUseManualAnimationTimeEnable state,duration=" + duration);
            }
        } else if (this.mData.frontCameraMaxBrightnessEnable && this.mFrontCameraDimmingEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration3 = this.mData.frontCameraDimmingDarkenTime;
            } else {
                duration3 = this.mData.frontCameraDimmingBrightenTime;
            }
            if (HWFLOW) {
                Slog.i(TAG, "frontCameraDimming auto duration=" + duration);
            }
        } else if (this.mFirstValidAutoBrightness || this.mAutoBrightnessIntervened || this.mfastAnimtionFlag) {
            float duration4 = 0.5f;
            if (this.mFirstValidAutoBrightness && !this.mAutoBrightnessIntervened && this.mNightUpPowerOnWithDimmingEnable && this.mData.nightUpModeEnable && this.mfastAnimtionFlag) {
                duration4 = this.mData.nightUpModePowOnDimTime;
                Slog.i(TAG, "NightUpBrightMode dimmingTime duration=" + duration4);
            }
            if (this.mAutoBrightnessIntervened) {
                duration4 = this.mData.seekBarDimTime;
            }
            if (this.mFirstValidAutoBrightness && this.mData.resetAmbientLuxGraTime > 0.0f) {
                duration4 = this.mData.resetAmbientLuxGraTime;
                if (DEBUG) {
                    Slog.i(TAG, "resetAmbientLuxGraTime=" + duration4);
                }
            }
            if (DEBUG) {
                Slog.i(TAG, "mFirstValidAuto=" + this.mFirstValidAutoBrightness + ",mAutoIntervened=" + this.mAutoBrightnessIntervened + "mfastAnimtionFlag=" + this.mfastAnimtionFlag);
            }
        } else if (this.mKeyguardUnlockedFastDarkenDimmingEnable && this.mTargetValue < this.mCurrentValue && this.mData.keyguardUnlockedDimmingTime > 0.0f) {
            duration = this.mData.keyguardUnlockedDimmingTime;
            if (DEBUG) {
                Slog.i(TAG, "mKeyguardUnlockedFastDarkenDimmingEnable AnimationTime=" + duration);
            }
        } else if (this.mFastDarkenDimmingEnable && this.mTargetValue < this.mCurrentValue && this.mData.resetAmbientLuxFastDarkenDimmingTime > 0.0f) {
            duration = this.mData.resetAmbientLuxFastDarkenDimmingTime;
            if (DEBUG) {
                Slog.i(TAG, "mFastDarkenDimmingEnable AnimationTime=" + duration);
            }
        } else if (this.mGameModeEnable) {
            int i = this.mTargetValue;
            if (i >= this.mCurrentValue) {
                duration2 = this.mData.gameModeBrightenAnimationTime;
            } else if (i >= this.mData.gameModeDarkentenLongTarget || this.mCurrentValue <= this.mData.gameModeDarkentenLongTarget) {
                duration2 = this.mData.gameModeDarkentenAnimationTime;
            } else {
                Slog.i(TAG, "GameModeEnable mTargetValue=" + this.mTargetValue + ",mCurrentValue=" + this.mCurrentValue);
                duration2 = this.mData.gameModeDarkentenLongAnimationTime;
            }
            Slog.i(TAG, "GameModeEnable AnimationTime=" + duration);
        } else if (this.mCameraModeEnable) {
            duration = this.mData.cameraAnimationTime;
            Slog.i(TAG, "CameraMode AnimationTime=" + this.mData.cameraAnimationTime);
        } else if (this.mReadingModeEnable) {
            duration = this.mData.readingAnimationTime;
            Slog.i(TAG, "ReadingMode AnimationTime=" + this.mData.readingAnimationTime);
        } else {
            duration = this.mTargetValue < this.mCurrentValue ? getAutoModeDarkTime() : getAutoModeBrightTime();
        }
        if (duration <= 0.0f) {
            Slog.w(TAG, "setDimTime=FAST_TIME, duration=" + duration);
            duration = 0.5f;
        }
        this.mAutoStepType = getAutoModeAnimtionStepType(duration);
        if (this.mAutoStepType == StepType.LINEAR) {
            amount = resetDarkenLinearAmount((((float) Math.abs(this.mCurrentValue - this.mTargetValue)) / duration) * this.mTimeDelta);
        } else {
            amount = getAnimatedStepByEyeSensitiveCurve(this.mAnimatedValue, (float) this.mTargetValue, duration);
        }
        this.mAutoDuration = duration;
        this.mDecreaseFixAmount = amount;
        if (this.mCurrentValue == 0) {
            Slog.i(TAG, "mCurrentValue=0,return DEFAULT_AMOUNT,mTargetValue=" + this.mTargetValue);
            return DEFAULT_AMOUNT;
        }
        if (this.mTimeDelta > this.mTimeDeltaMinValue) {
            this.mFirstTimeCalculateAmount = false;
        }
        if (DEBUG) {
            Slog.i(TAG, "AutoMode=" + this.mAutoBrightnessMode + ",Target=" + this.mTargetValue + ",Current=" + this.mCurrentValue + ",amount=" + amount + ",duration=" + duration + ",StepType=" + this.mAutoStepType + ",mTimeDelta=" + this.mTimeDelta);
        }
        this.mDarkAdaptAnimationDimmingEnable = false;
        return amount;
    }

    private float resetDarkenLinearAmount(float linearAmount) {
        int i = this.mCurrentValue;
        int i2 = this.mTargetValue;
        if (i <= i2 || i2 >= this.mData.darkenTargetFor255 || linearAmount >= this.mData.minAnimatingStep || this.mData.minAnimatingStep <= 0.0f) {
            return linearAmount;
        }
        float avgTime = (((float) (this.mCurrentValue - this.mTargetValue)) / this.mData.minAnimatingStep) * this.mTimeDelta;
        Slog.i(TAG, "resetDarkenLinearAmount=" + linearAmount + "-->amount=" + this.mData.minAnimatingStep + "resetAvgTime=" + avgTime);
        return this.mData.minAnimatingStep;
    }

    public float getDuration() {
        return this.mDuration;
    }

    private float getAmount() {
        if (this.mAutoStepType == StepType.LINEAR) {
            return this.mDecreaseFixAmount;
        }
        return getAnimatedStepByEyeSensitiveCurve(this.mAnimatedValue, (float) this.mTargetValue, this.mAutoDuration);
    }

    public float getAutoModeDarkTime() {
        float duration;
        if (this.mData.useVariableStep) {
            duration = this.mData.darkenGradualTimeMax;
        } else {
            duration = this.mData.darkenGradualTime;
        }
        if (this.mDarkAdaptAnimationDimmingEnable) {
            duration = (float) this.mData.unadapt2AdaptingDimSec;
            if (DEBUG) {
                Slog.i(TAG, "unadapt2AdaptingDimSec = " + duration);
            }
        }
        if (this.mOutdoorAnimationFlag && this.mData.outdoorAnimationDarkenTime > 0.0f) {
            duration = this.mData.outdoorAnimationDarkenTime;
            if (DEBUG) {
                Slog.i(TAG, "outdoorAnimationDarkenTime = " + duration);
            }
        }
        if (this.mScreenLocked && this.mData.keyguardAnimationDarkenTime > 0.0f) {
            duration = this.mData.keyguardAnimationDarkenTime;
            if (DEBUG) {
                Slog.i(TAG, "keyguardAnimationDarkenTime=" + duration);
            }
        }
        if (this.mTargetValue < this.mData.darkenTargetFor255 && this.mCurrentValue < this.mData.darkenCurrentFor255 && (Math.abs(duration - this.mData.darkenGradualTime) < 1.0E-7f || Math.abs(duration - this.mData.darkenGradualTimeMax) < 1.0E-7f)) {
            duration = 0.5f;
        }
        if (this.mPowerDimState) {
            if (this.mScreenLocked && this.mCurrentValue < this.mData.keyguardFastDimBrightness) {
                duration = this.mData.keyguardFastDimTime;
                Slog.i(TAG, "mScreenLocked,mCurrentValue=" + this.mCurrentValue);
            } else if (this.mData.darkenGradualTime > this.mData.dimTime || (this.mData.useVariableStep && this.mData.darkenGradualTimeMax > this.mData.dimTime)) {
                duration = this.mData.dimTime;
            }
            if (DEBUG) {
                Slog.i(TAG, "The Dim state");
            }
        }
        if (this.mPowerDimRecoveryState) {
            this.mPowerDimRecoveryState = false;
            duration = 0.5f;
            if (DEBUG) {
                Slog.i(TAG, "The Dim state Recovery");
            }
        }
        return duration;
    }

    public float getAutoModeBrightTime() {
        float duration = this.mData.brightenGradualTime;
        if (this.mPowerDimRecoveryState) {
            this.mPowerDimRecoveryState = false;
            if (DEBUG) {
                Slog.i(TAG, "The Dim state Recovery");
            }
            return 0.5f;
        } else if (this.mScreenLocked && this.mData.keyguardAnimationBrightenTime > 0.0f) {
            float duration2 = this.mData.keyguardAnimationBrightenTime;
            if (DEBUG) {
                Slog.i(TAG, "keyguardAnimationBrightenTime=" + duration2);
            }
            return duration2;
        } else if (this.mOutdoorAnimationFlag && this.mData.outdoorAnimationBrightenTime > 0.0f) {
            float duration3 = this.mData.outdoorAnimationBrightenTime;
            if (DEBUG) {
                Slog.i(TAG, "outdoorAnimationBrightenTime=" + duration3);
            }
            return duration3;
        } else if (this.mTargetValue < this.mData.darkenTargetFor255) {
            return getFlickerBrightenDimmingTime();
        } else {
            if (this.mCurrentValue < this.mData.brightenTimeLongCurrentTh && this.mTargetValue - this.mCurrentValue > this.mData.brightenTimeLongAmountMin) {
                duration = this.mData.brightenGradualTimeLong;
                if (DEBUG) {
                    Slog.i(TAG, "mCurrentValue=" + this.mCurrentValue + ",LongCurrentTh=" + this.mData.brightenTimeLongCurrentTh + ",brightenGradualTimeLong=" + duration);
                }
            }
            return duration;
        }
    }

    private float getFlickerBrightenDimmingTime() {
        float dimmingTime;
        if (this.mData.brightenFlickerTargetMin > ((int) this.mBrightnessMin) && this.mData.brightenFlickerGradualTimeMax - this.mData.brightenFlickerGradualTimeMin > 0.0f) {
            if (this.mTargetValue - this.mCurrentValue <= this.mData.brightenFlickerAmountMin || this.mTargetValue <= this.mData.brightenFlickerTargetMin) {
                dimmingTime = this.mData.brightenFlickerGradualTimeMin;
            } else {
                dimmingTime = ((((float) (this.mTargetValue - this.mData.brightenFlickerTargetMin)) * (this.mData.brightenFlickerGradualTimeMax - this.mData.brightenFlickerGradualTimeMin)) / ((float) (this.mData.darkenTargetFor255 - this.mData.brightenFlickerTargetMin))) + this.mData.brightenFlickerGradualTimeMin;
            }
            Slog.i(TAG, "FlickerBrightenTime=" + dimmingTime + ",deltaAmount=" + (this.mTargetValue - this.mCurrentValue));
            return dimmingTime;
        } else if (this.mData.targetFor255BrightenTime <= 0.0f || this.mData.targetFor255BrightenTime > this.mData.brightenGradualTime) {
            return 0.5f;
        } else {
            return this.mData.targetFor255BrightenTime;
        }
    }

    public float getManualModeTime() {
        float duration;
        float duration2;
        float duration3;
        float duration4 = this.mData.seekBarDimTime;
        if (this.mFirstRebootAnimationEnable && this.mData.rebootFirstBrightnessAnimationEnable) {
            this.mFirstRebootAnimationEnable = false;
            duration4 = this.mData.rebootFirstBrightnessManualTime;
            Slog.i(TAG, "The mFirstRebootAnimationEnable state,duration=" + duration4);
        } else if (this.mManualModeAnimationEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration4 = this.mData.manualAnimationDarkenTime;
            } else {
                duration4 = this.mData.manualAnimationBrightenTime;
            }
            if (DEBUG) {
                Slog.i(TAG, "The mManualModeAnimationEnable state,duration=" + duration4);
            }
        } else if (this.mManualPowerSavingAnimationEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration3 = this.mData.manualPowerSavingAnimationDarkenTime;
            } else {
                duration3 = this.mData.manualPowerSavingAnimationBrightenTime;
            }
            this.mManualPowerSavingAnimationEnable = false;
            if (DEBUG) {
                Slog.i(TAG, "The mManualPowerSavingAnimationEnable state,duration=" + duration4);
            }
        }
        if (this.mData.frontCameraMaxBrightnessEnable && this.mFrontCameraDimmingEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration2 = this.mData.frontCameraDimmingDarkenTime;
            } else {
                duration2 = this.mData.frontCameraDimmingBrightenTime;
            }
            if (HWFLOW) {
                Slog.i(TAG, "frontCameraDimming duration=" + duration4);
            }
        }
        if (this.mAnimationEnabled) {
            duration4 = ((float) this.mMillisecond) / 1000.0f;
            if (DEBUG) {
                Slog.i(TAG, "The mAnimationEnabled state,duration=" + duration4);
            }
        }
        if (this.mManualThermalModeAnimationEnable) {
            if (this.mTargetValue < this.mCurrentValue) {
                duration = this.mData.manualThermalModeAnimationDarkenTime;
            } else {
                duration = this.mData.manualThermalModeAnimationBrightenTime;
            }
            this.mManualThermalModeAnimationEnable = false;
            if (DEBUG) {
                Slog.i(TAG, "The mManualThermalModeAnimationEnable state,duration=" + duration4);
            }
        }
        if (this.mBrightnessModeAnimationEnable) {
            duration4 = ((float) this.mBrightnessModeAnimationTime) / 1000.0f;
            this.mBrightnessModeAnimationEnable = false;
            if (DEBUG) {
                Slog.i(TAG, "The manual mBrightnessModeAnimationEnable state,duration=" + duration4);
            }
        }
        if (this.mPowerDimState) {
            if (this.mScreenLocked && this.mCurrentValue < this.mData.keyguardFastDimBrightness) {
                duration4 = this.mData.keyguardFastDimTime;
                Slog.i(TAG, "mScreenLocked,mCurrentValue=" + this.mCurrentValue);
            } else if (this.mData.darkenGradualTime > this.mData.dimTime || (this.mData.useVariableStep && this.mData.darkenGradualTimeMax > this.mData.dimTime)) {
                duration4 = this.mData.dimTime;
            }
            if (DEBUG) {
                Slog.i(TAG, "The Dim state");
            }
        }
        if (this.mPowerDimRecoveryState) {
            this.mPowerDimRecoveryState = false;
            duration4 = this.mData.manualFastTimeFor255;
            if (DEBUG) {
                Slog.i(TAG, "The Dim state Recovery");
            }
        }
        return duration4;
    }

    public float getManualModeAnimtionAmount() {
        if (!this.mFirstTimeCalculateAmount) {
            return this.mDecreaseFixAmount;
        }
        float duration = getManualModeTime();
        if (duration <= 0.0f) {
            Slog.w(TAG, "setDimTime=FAST_TIME, duration=" + duration);
            duration = 0.5f;
        }
        this.mManualDuration = duration;
        float f = this.mTimeDelta;
        float amount = (((float) Math.abs(this.mCurrentValue - this.mTargetValue)) / duration) * f;
        this.mDecreaseFixAmount = amount;
        if (f > this.mTimeDeltaMinValue) {
            this.mFirstTimeCalculateAmount = false;
        }
        if (!DEBUG) {
            return amount;
        }
        Slog.i(TAG, "AutoMode=" + this.mAutoBrightnessMode + ",Target=" + this.mTargetValue + ",Current=" + this.mCurrentValue + ",amount=" + amount + ",duration=" + duration);
        return amount;
    }

    public void updateTargetAndRate(int target, int rate) {
        if (this.mTargetValue != target) {
            this.mFirstTimeCalculateAmount = true;
        }
        this.mTargetValue = target;
        this.mRate = rate;
    }

    public void updateCurrentBrightnessValue(float currentValue) {
        this.mCurrentValue = Math.round(currentValue);
        this.mAnimatedValue = currentValue;
    }

    public void setPowerDimState(int state) {
        this.mPowerDimState = state == 2;
        if (this.mPowerDimState) {
            this.mFirstValidAutoBrightness = false;
        }
        if (this.mState == 2 && state == 3) {
            this.mPowerDimRecoveryState = true;
        }
        this.mState = state;
    }

    public void updateAdjustMode(boolean automode) {
        this.mAutoBrightnessMode = automode;
    }

    public void autoModeIsIntervened(boolean intervened) {
        this.mAutoBrightnessIntervened = intervened;
    }

    public void isFirstValidAutoBrightness(boolean firstValidAutoBrightness) {
        this.mFirstValidAutoBrightness = firstValidAutoBrightness;
    }

    public void updateFastAnimationFlag(boolean fastAnimtionFlag) {
        this.mfastAnimtionFlag = fastAnimtionFlag;
    }

    public void updateCoverModeFastAnimationFlag(boolean coverModeAmitionFast) {
        this.mCoverModeAnimationFast = coverModeAmitionFast;
    }

    public void updateCameraModeChangeAnimationEnable(boolean cameraModeEnable) {
        this.mCameraModeEnable = cameraModeEnable;
    }

    public void updateGameModeChangeAnimationEnable(boolean gameModeEnable) {
        this.mGameModeEnable = gameModeEnable;
    }

    public void updateReadingModeChangeAnimationEnable(boolean readingModeEnable) {
        this.mReadingModeEnable = readingModeEnable;
    }

    public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) {
        this.mAnimationEnabled = animationEnabled;
        this.mMillisecond = millisecond;
        if (this.mMillisecond < 0) {
            this.mAnimationEnabled = false;
            Slog.e(TAG, "error input mMillisecond=" + this.mMillisecond + ",set mAnimationEnabled=" + this.mAnimationEnabled);
        }
    }

    public void updateScreenLockedAnimationEnable(boolean screenLockedEnable) {
        this.mScreenLocked = screenLockedEnable;
    }

    public void updateOutdoorAnimationFlag(boolean outdoorAnimationFlag) {
        this.mOutdoorAnimationFlag = outdoorAnimationFlag;
    }

    public void updatemManualModeAnimationEnable(boolean manualModeAnimationEnable) {
        this.mManualModeAnimationEnable = manualModeAnimationEnable;
    }

    public void updateManualPowerSavingAnimationEnable(boolean manualPowerSavingAnimationEnable) {
        this.mManualPowerSavingAnimationEnable = manualPowerSavingAnimationEnable;
    }

    public void updateManualThermalModeAnimationEnable(boolean manualThermalModeAnimationEnable) {
        this.mManualThermalModeAnimationEnable = manualThermalModeAnimationEnable;
    }

    public void updateBrightnessModeAnimationEnable(boolean animationEnable, int time) {
        this.mBrightnessModeAnimationTime = time;
        if (time < 0) {
            this.mBrightnessModeAnimationEnable = false;
            Slog.e(TAG, "error input time,time=,set mBrightnessModeAnimationEnable=" + this.mBrightnessModeAnimationEnable);
        }
        this.mBrightnessModeAnimationEnable = animationEnable;
    }

    public void updateDarkAdaptAnimationDimmingEnable(boolean enable) {
        this.mDarkAdaptAnimationDimmingEnable = enable;
    }

    public void updateFastDarkenDimmingEnable(boolean enable) {
        if (enable != this.mFastDarkenDimmingEnable && DEBUG) {
            Slog.i(TAG, "updateFastDarkenDimmingEnable enable=" + enable);
        }
        this.mFastDarkenDimmingEnable = enable;
    }

    public void updateKeyguardUnlockedFastDarkenDimmingEnable(boolean enable) {
        if (enable != this.mKeyguardUnlockedFastDarkenDimmingEnable && DEBUG) {
            Slog.i(TAG, "updateKeyguardUnlockedFastDarkenDimmingEnable enable=" + enable);
        }
        this.mKeyguardUnlockedFastDarkenDimmingEnable = enable;
    }

    public void updateNightUpPowerOnWithDimmingEnable(boolean enable) {
        if (enable != this.mNightUpPowerOnWithDimmingEnable && enable && HWFLOW) {
            Slog.i(TAG, "NightUpBrightMode mNightUpPowerOnWithDimmingEnable=" + this.mNightUpPowerOnWithDimmingEnable + "-->enable=" + enable);
        }
        this.mNightUpPowerOnWithDimmingEnable = enable;
    }

    public void updateFrameRate(int frameRate) {
        if (frameRate <= 0) {
            Slog.w(TAG, "setDefault frameRate, frameRate=" + frameRate);
            this.mTimeDeltaMinValue = FRAME_RATE_LEVEL0_TIME_DELTA_MIN;
            this.mTimeDelta = TIME_DELTA;
            this.mFrameRate = 60;
            return;
        }
        if (frameRate == 60) {
            this.mTimeDeltaMinValue = FRAME_RATE_LEVEL0_TIME_DELTA_MIN;
        } else if (frameRate == FRAME_RATE_LEVEL1) {
            this.mTimeDeltaMinValue = FRAME_RATE_LEVEL1_TIME_DELTA_MIN;
        } else if (frameRate == 120) {
            this.mTimeDeltaMinValue = FRAME_RATE_LEVEL2_TIME_DELTA_MIN;
        } else {
            Slog.w(TAG, "updateFrameRate, frameRate not match, brightness dimmingTime is not accurate");
        }
        this.mTimeDelta = 1.0f / ((float) frameRate);
        if (frameRate != this.mFrameRate && HWFLOW) {
            Slog.i(TAG, "updateFrameRate, mTimeDeltaMinValue=" + this.mTimeDeltaMinValue + ",mTimeDelta=" + this.mTimeDelta + ",frameRate=" + frameRate);
        }
        this.mFrameRate = frameRate;
    }

    public void updateFrontCameraDimmingEnable(boolean dimmingEnable) {
        if (dimmingEnable != this.mFrontCameraDimmingEnable) {
            if (HWFLOW) {
                Slog.i(TAG, "updateFrontCameraDimmingEnable=" + dimmingEnable);
            }
            this.mFrontCameraDimmingEnable = dimmingEnable;
        }
    }

    public void clearAnimatedValuePara() {
        this.mFirstValidAutoBrightness = false;
        this.mAutoBrightnessIntervened = false;
        this.mPowerDimState = false;
        this.mPowerDimRecoveryState = false;
        this.mfastAnimtionFlag = false;
        this.mCoverModeAnimationFast = false;
        this.mCameraModeEnable = false;
        this.mReadingModeEnable = false;
        this.mOutdoorAnimationFlag = false;
        this.mManualModeAnimationEnable = false;
        this.mManualPowerSavingAnimationEnable = false;
        this.mManualThermalModeAnimationEnable = false;
        this.mFirstRebootAnimationEnable = false;
        this.mBrightnessModeAnimationEnable = false;
        this.mDarkAdaptAnimationDimmingEnable = false;
        this.mGameModeEnable = false;
        this.mFrontCameraDimmingEnable = false;
    }
}

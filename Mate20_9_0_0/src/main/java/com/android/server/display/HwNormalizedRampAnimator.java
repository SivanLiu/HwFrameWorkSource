package com.android.server.display;

import android.os.Bundle;
import android.util.IntProperty;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import com.huawei.displayengine.DisplayEngineManager;

public final class HwNormalizedRampAnimator<T> extends RampAnimator<T> {
    private static final int DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int DEFAULT_MIN_BRIGHTNESS = 4;
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    private boolean DEBUG;
    private boolean DEBUG_CONTROLLER = false;
    private String TAG = "HwNormalizedRampAnimator";
    private boolean mBrightnessAdjustMode;
    private final Data mData;
    private DisplayEngineManager mDisplayEngineManager;
    private Bundle mHBMData;
    private HwGradualBrightnessAlgo mHwGradualBrightnessAlgo;
    private boolean mModeOffForRGBW;
    private final Runnable mNormalizedAnimationCallback;
    private boolean mProximityState;
    private boolean mProximityStateRecovery;
    private int mTargetValueChange;
    private int mTargetValueForRGBW;

    public HwNormalizedRampAnimator(T object, IntProperty<T> property) {
        super(object, property);
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(this.TAG, 4));
        this.DEBUG = z;
        this.mBrightnessAdjustMode = false;
        this.mProximityState = false;
        this.mProximityStateRecovery = false;
        this.mModeOffForRGBW = true;
        this.mNormalizedAnimationCallback = new Runnable() {
            public void run() {
                HwNormalizedRampAnimator.this.mAnimatedValue = HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.getAnimatedValue();
                HwNormalizedRampAnimator.this.updateHBMData(HwNormalizedRampAnimator.this.mTargetValue, HwNormalizedRampAnimator.this.mRate, HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.getDuration());
                HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.updateCurrentBrightnessValue(HwNormalizedRampAnimator.this.mAnimatedValue);
                int oldCurrentValue = HwNormalizedRampAnimator.this.mCurrentValue;
                HwNormalizedRampAnimator.this.mCurrentValue = Math.round(HwNormalizedRampAnimator.this.mAnimatedValue);
                if (HwNormalizedRampAnimator.this.mData.animatingForRGBWEnable && HwNormalizedRampAnimator.this.mModeOffForRGBW && (HwNormalizedRampAnimator.this.mTargetValueChange != HwNormalizedRampAnimator.this.mTargetValue || (HwNormalizedRampAnimator.this.mProximityStateRecovery && HwNormalizedRampAnimator.this.mTargetValue != HwNormalizedRampAnimator.this.mCurrentValue))) {
                    HwNormalizedRampAnimator.this.mModeOffForRGBW = false;
                    HwNormalizedRampAnimator.this.mDisplayEngineManager.setScene(21, 16);
                    if (HwNormalizedRampAnimator.this.DEBUG) {
                        Slog.d(HwNormalizedRampAnimator.this.TAG, "send DE_ACTION_MODE_ON For RGBW");
                    }
                    HwNormalizedRampAnimator.this.mTargetValueChange = HwNormalizedRampAnimator.this.mTargetValue;
                    HwNormalizedRampAnimator.this.mProximityStateRecovery = false;
                }
                if (oldCurrentValue != HwNormalizedRampAnimator.this.mCurrentValue) {
                    HwNormalizedRampAnimator.this.mProperty.setValue(HwNormalizedRampAnimator.this.mObject, HwNormalizedRampAnimator.this.mCurrentValue);
                    if (HwNormalizedRampAnimator.this.DEBUG && HwNormalizedRampAnimator.this.DEBUG_CONTROLLER) {
                        String access$800 = HwNormalizedRampAnimator.this.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mCurrentValue=");
                        stringBuilder.append(HwNormalizedRampAnimator.this.mCurrentValue);
                        Slog.d(access$800, stringBuilder.toString());
                    }
                }
                if (HwNormalizedRampAnimator.this.mTargetValue != HwNormalizedRampAnimator.this.mCurrentValue) {
                    HwNormalizedRampAnimator.this.postAnimationCallback();
                    return;
                }
                HwNormalizedRampAnimator.this.mAnimating = false;
                HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.clearAnimatedValuePara();
                if (HwNormalizedRampAnimator.this.mListener != null) {
                    HwNormalizedRampAnimator.this.mListener.onAnimationEnd();
                }
                if (HwNormalizedRampAnimator.this.mData.animatingForRGBWEnable) {
                    HwNormalizedRampAnimator.this.mModeOffForRGBW = true;
                    HwNormalizedRampAnimator.this.mDisplayEngineManager.setScene(21, 17);
                    if (HwNormalizedRampAnimator.this.DEBUG) {
                        Slog.i(HwNormalizedRampAnimator.this.TAG, "send DE_ACTION_MODE_Off For RGBW");
                    }
                }
            }
        };
        this.mHwGradualBrightnessAlgo = new HwGradualBrightnessAlgo();
        this.mData = HwBrightnessXmlLoader.getData();
        this.mDisplayEngineManager = new DisplayEngineManager();
        if (this.mData.rebootFirstBrightnessAnimationEnable) {
            this.mFirstTime = false;
        }
        this.mHBMData = new Bundle();
        updateHBMData(this.mTargetValue, this.mRate, this.mHwGradualBrightnessAlgo.getDuration());
    }

    public boolean animateTo(int target, int rate) {
        if (this.mData.animatingForRGBWEnable && rate <= 0 && target == 0 && this.mTargetValueForRGBW >= 4) {
            this.mModeOffForRGBW = true;
            this.mDisplayEngineManager.setScene(21, 17);
            if (this.DEBUG) {
                Slog.d(this.TAG, "send DE_ACTION_MODE_off For RGBW");
            }
        }
        this.mTargetValueForRGBW = target;
        float targetOut = (float) target;
        if (target > 4 && this.mData.darkLightLevelMaxThreshold > 4 && target > this.mData.darkLightLevelMinThreshold && target < this.mData.darkLightLevelMaxThreshold) {
            float ratio = (float) Math.pow((double) ((targetOut - 4.0f) / ((float) (this.mData.darkLightLevelMaxThreshold - 4))), (double) this.mData.darkLightLevelRatio);
            targetOut = 4.0f + (((float) (this.mData.darkLightLevelMaxThreshold - 4)) * ratio);
            if (this.DEBUG) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DarkLightLevel targetIn255 =");
                stringBuilder.append(target);
                stringBuilder.append(",targetOut255=");
                stringBuilder.append(targetOut);
                stringBuilder.append(",ratio=");
                stringBuilder.append(ratio);
                Slog.d(str, stringBuilder.toString());
            }
        }
        target = (int) ((10000.0f * targetOut) / 255.0f);
        boolean ret = super.animateTo(target, rate);
        if (rate == 0) {
            updateHBMData(target, rate, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO);
        }
        return ret;
    }

    protected void notifyAlgoUpdateCurrentValue() {
        this.mHwGradualBrightnessAlgo.updateTargetAndRate(this.mTargetValue, this.mRate);
        this.mHwGradualBrightnessAlgo.updateCurrentBrightnessValue((float) this.mCurrentValue);
    }

    public void updateBrightnessRampPara(boolean automode, int updateAutoBrightnessCount, boolean intervened, int state) {
        if (this.DEBUG && this.DEBUG_CONTROLLER) {
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("automode=");
            stringBuilder.append(automode);
            stringBuilder.append(",updateBrightnessCount=");
            stringBuilder.append(updateAutoBrightnessCount);
            stringBuilder.append(",intervened=");
            stringBuilder.append(intervened);
            stringBuilder.append(",state=");
            stringBuilder.append(state);
            Slog.d(str, stringBuilder.toString());
        }
        this.mBrightnessAdjustMode = automode;
        if (this.mBrightnessAdjustMode) {
            HwGradualBrightnessAlgo hwGradualBrightnessAlgo = this.mHwGradualBrightnessAlgo;
            boolean z = true;
            if (!(updateAutoBrightnessCount == 1 || updateAutoBrightnessCount == 0)) {
                z = false;
            }
            hwGradualBrightnessAlgo.isFirstValidAutoBrightness(z);
        }
        this.mHwGradualBrightnessAlgo.updateAdjustMode(automode);
        this.mHwGradualBrightnessAlgo.autoModeIsIntervened(intervened);
        this.mHwGradualBrightnessAlgo.setPowerDimState(state);
    }

    public void updateFastAnimationFlag(boolean fastAnimtionFlag) {
        this.mHwGradualBrightnessAlgo.updateFastAnimationFlag(fastAnimtionFlag);
    }

    public void updateCoverModeFastAnimationFlag(boolean coverModeAmitionFast) {
        this.mHwGradualBrightnessAlgo.updateCoverModeFastAnimationFlag(coverModeAmitionFast);
    }

    public void updateCameraModeChangeAnimationEnable(boolean cameraModeEnable) {
        this.mHwGradualBrightnessAlgo.updateCameraModeChangeAnimationEnable(cameraModeEnable);
    }

    public void updateGameModeChangeAnimationEnable(boolean gameModeEnable) {
        this.mHwGradualBrightnessAlgo.updateGameModeChangeAnimationEnable(gameModeEnable);
    }

    public void updateReadingModeChangeAnimationEnable(boolean readingModeEnable) {
        this.mHwGradualBrightnessAlgo.updateReadingModeChangeAnimationEnable(readingModeEnable);
    }

    public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) {
        if (this.mHwGradualBrightnessAlgo != null) {
            this.mHwGradualBrightnessAlgo.setBrightnessAnimationTime(animationEnabled, millisecond);
        } else {
            Slog.e(this.TAG, "mHwGradualBrightnessAlgo=null,can not setBrightnessAnimationTime");
        }
    }

    public void updateScreenLockedAnimationEnable(boolean screenLockedEnable) {
        this.mHwGradualBrightnessAlgo.updateScreenLockedAnimationEnable(screenLockedEnable);
    }

    public void updateOutdoorAnimationFlag(boolean specialAnimtionFlag) {
        this.mHwGradualBrightnessAlgo.updateOutdoorAnimationFlag(specialAnimtionFlag);
    }

    public void updatemManualModeAnimationEnable(boolean manualModeAnimationEnable) {
        this.mHwGradualBrightnessAlgo.updatemManualModeAnimationEnable(manualModeAnimationEnable);
    }

    public void updateManualPowerSavingAnimationEnable(boolean manualPowerSavingAnimationEnable) {
        this.mHwGradualBrightnessAlgo.updateManualPowerSavingAnimationEnable(manualPowerSavingAnimationEnable);
    }

    public void updateManualThermalModeAnimationEnable(boolean manualThermalModeAnimationEnable) {
        this.mHwGradualBrightnessAlgo.updateManualThermalModeAnimationEnable(manualThermalModeAnimationEnable);
    }

    public void updateBrightnessModeAnimationEnable(boolean animationEnable, int time) {
        this.mHwGradualBrightnessAlgo.updateBrightnessModeAnimationEnable(animationEnable, time);
    }

    public void updateDarkAdaptAnimationDimmingEnable(boolean enable) {
        this.mHwGradualBrightnessAlgo.updateDarkAdaptAnimationDimmingEnable(enable);
    }

    protected void postAnimationCallback() {
        this.mHwGradualBrightnessAlgo.updateTargetAndRate(this.mTargetValue, this.mRate);
        this.mChoreographer.postCallback(1, this.mNormalizedAnimationCallback, null);
    }

    protected void cancelAnimationCallback() {
        this.mChoreographer.removeCallbacks(1, this.mNormalizedAnimationCallback, null);
    }

    public void updateProximityState(boolean proximityState) {
        if (this.mData.animatingForRGBWEnable && !proximityState && this.mProximityState) {
            this.mProximityStateRecovery = true;
        }
        this.mProximityState = proximityState;
        if (proximityState && this.mAnimating && this.mTargetValue < this.mCurrentValue) {
            this.mAnimating = false;
            cancelAnimationCallback();
            this.mTargetValue = this.mCurrentValue;
            this.mHwGradualBrightnessAlgo.clearAnimatedValuePara();
            if (this.mListener != null) {
                this.mListener.onAnimationEnd();
            }
            if (this.mData.animatingForRGBWEnable) {
                this.mModeOffForRGBW = true;
                this.mDisplayEngineManager.setScene(21, 17);
                if (this.DEBUG) {
                    Slog.d(this.TAG, "send DE_ACTION_MODE_OFF For RGBW");
                }
            }
            if (this.DEBUG) {
                String str = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" proximityState=");
                stringBuilder.append(proximityState);
                stringBuilder.append(",mTargetValue=");
                stringBuilder.append(this.mTargetValue);
                stringBuilder.append(",mCurrentValue=");
                stringBuilder.append(this.mCurrentValue);
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    public int getCurrentBrightness() {
        return (this.mCurrentValue * 255) / 10000;
    }

    private void updateHBMData(int target, int rate, float duration) {
        if (this.mHBMData.getInt("target") != target || this.mHBMData.getInt("rate") != rate || ((double) Math.abs(this.mHBMData.getFloat("duration") - duration)) > 1.0E-6d) {
            this.mHBMData.putInt("target", target);
            this.mHBMData.putInt("rate", rate);
            this.mHBMData.putFloat("duration", duration);
            String str = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("hbm_dimming target=");
            stringBuilder.append(this.mHBMData.getInt("target"));
            stringBuilder.append(" rate=");
            stringBuilder.append(this.mHBMData.getInt("rate"));
            stringBuilder.append(" duration=");
            stringBuilder.append(this.mHBMData.getFloat("duration"));
            Slog.d(str, stringBuilder.toString());
            this.mDisplayEngineManager.setDataToFilter("HBM", this.mHBMData);
        }
    }
}

package com.android.server.display;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.util.IntProperty;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.displayengine.IDisplayEngineCallback;
import huawei.com.android.server.fingerprint.FingerViewController;

public final class HwNormalizedRampAnimator<T> extends RampAnimator<T> {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final boolean DEBUG_CONTROLLER = false;
    private static final int DEFAULT_MAX_BRIGHTNESS = 255;
    private static final int DEFAULT_MIN_BRIGHTNESS = 4;
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int MSG_UPDATE_BRIGHTNESS_ALPHA = 1;
    private static final int NIGHT_UP_POWER_ON_DEFFAULT_RATE = 100;
    /* access modifiers changed from: private */
    public static String TAG = "HwNormalizedRampAnimator";
    private boolean mBrightnessAdjustMode = false;
    /* access modifiers changed from: private */
    public int mBrightnessLevel = -1;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final HwBrightnessXmlLoader.Data mData;
    private final IDisplayEngineCallback mDisplayEngineCallback = new IDisplayEngineCallback.Stub() {
        /* class com.android.server.display.HwNormalizedRampAnimator.AnonymousClass1 */

        @Override // com.huawei.displayengine.IDisplayEngineCallback
        public void onEvent(int event, int extra) {
            if (event == 1) {
                String access$000 = HwNormalizedRampAnimator.TAG;
                Slog.i(access$000, "onEvent, frameRate=" + extra);
                HwNormalizedRampAnimator.this.updateFrameRate(extra);
            }
        }

        @Override // com.huawei.displayengine.IDisplayEngineCallback
        public void onEventWithData(int event, PersistableBundle data) {
        }
    };
    /* access modifiers changed from: private */
    public DisplayEngineManager mDisplayEngineManager;
    private FingerViewController mFingerViewController = null;
    private Bundle mHBMData;
    private Handler mHandler = new Handler() {
        /* class com.android.server.display.HwNormalizedRampAnimator.AnonymousClass3 */

        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                Slog.e(HwNormalizedRampAnimator.TAG, "Invalid message");
                return;
            }
            HwNormalizedRampAnimator hwNormalizedRampAnimator = HwNormalizedRampAnimator.this;
            hwNormalizedRampAnimator.updateFingerGradualBrightness(hwNormalizedRampAnimator.mBrightnessLevel);
        }
    };
    private boolean mHbmAheadEnable = SystemProperties.getBoolean("ro.config.fp_hbm_ahead", false);
    /* access modifiers changed from: private */
    public HwGradualBrightnessAlgo mHwGradualBrightnessAlgo;
    private boolean mKeyguradIsLocked = false;
    /* access modifiers changed from: private */
    public boolean mModeOffForRGBW = true;
    private boolean mNightUpPowerOnWithDimmingEnable = false;
    private final Runnable mNormalizedAnimationCallback = new Runnable() {
        /* class com.android.server.display.HwNormalizedRampAnimator.AnonymousClass2 */

        /* JADX DEBUG: Multi-variable search result rejected for r1v30, resolved type: android.util.IntProperty */
        /* JADX WARN: Multi-variable type inference failed */
        public void run() {
            HwNormalizedRampAnimator hwNormalizedRampAnimator = HwNormalizedRampAnimator.this;
            hwNormalizedRampAnimator.mAnimatedValue = hwNormalizedRampAnimator.mHwGradualBrightnessAlgo.getAnimatedValue();
            HwNormalizedRampAnimator hwNormalizedRampAnimator2 = HwNormalizedRampAnimator.this;
            hwNormalizedRampAnimator2.updateHBMData(hwNormalizedRampAnimator2.mTargetValue, HwNormalizedRampAnimator.this.mRate, HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.getDuration());
            HwNormalizedRampAnimator.this.mHwGradualBrightnessAlgo.updateCurrentBrightnessValue(HwNormalizedRampAnimator.this.mAnimatedValue);
            int oldCurrentValue = HwNormalizedRampAnimator.this.mCurrentValue;
            HwNormalizedRampAnimator hwNormalizedRampAnimator3 = HwNormalizedRampAnimator.this;
            hwNormalizedRampAnimator3.mCurrentValue = Math.round(hwNormalizedRampAnimator3.mAnimatedValue);
            if (HwNormalizedRampAnimator.this.mData.animatingForRGBWEnable && HwNormalizedRampAnimator.this.mModeOffForRGBW && (HwNormalizedRampAnimator.this.mTargetValueChange != HwNormalizedRampAnimator.this.mTargetValue || (HwNormalizedRampAnimator.this.mProximityStateRecovery && HwNormalizedRampAnimator.this.mTargetValue != HwNormalizedRampAnimator.this.mCurrentValue))) {
                boolean unused = HwNormalizedRampAnimator.this.mModeOffForRGBW = false;
                HwNormalizedRampAnimator.this.mDisplayEngineManager.setScene(21, 16);
                if (HwNormalizedRampAnimator.DEBUG) {
                    Slog.i(HwNormalizedRampAnimator.TAG, "send DE_ACTION_MODE_ON For RGBW");
                }
                HwNormalizedRampAnimator hwNormalizedRampAnimator4 = HwNormalizedRampAnimator.this;
                int unused2 = hwNormalizedRampAnimator4.mTargetValueChange = hwNormalizedRampAnimator4.mTargetValue;
                boolean unused3 = HwNormalizedRampAnimator.this.mProximityStateRecovery = false;
            }
            if (oldCurrentValue != HwNormalizedRampAnimator.this.mCurrentValue) {
                HwNormalizedRampAnimator hwNormalizedRampAnimator5 = HwNormalizedRampAnimator.this;
                hwNormalizedRampAnimator5.updateBrightnessViewAlpha(hwNormalizedRampAnimator5.mCurrentValue);
                HwNormalizedRampAnimator.this.mProperty.setValue(HwNormalizedRampAnimator.this.mObject, HwNormalizedRampAnimator.this.mCurrentValue);
                boolean unused4 = HwNormalizedRampAnimator.DEBUG;
            }
            if (HwNormalizedRampAnimator.this.mTargetValue != HwNormalizedRampAnimator.this.mCurrentValue) {
                HwNormalizedRampAnimator.this.postAnimationCallback();
                return;
            }
            HwNormalizedRampAnimator hwNormalizedRampAnimator6 = HwNormalizedRampAnimator.this;
            hwNormalizedRampAnimator6.mAnimating = false;
            hwNormalizedRampAnimator6.mHwGradualBrightnessAlgo.clearAnimatedValuePara();
            if (HwNormalizedRampAnimator.this.mListener != null) {
                HwNormalizedRampAnimator.this.mListener.onAnimationEnd();
            }
            if (HwNormalizedRampAnimator.this.mData.animatingForRGBWEnable) {
                boolean unused5 = HwNormalizedRampAnimator.this.mModeOffForRGBW = true;
                HwNormalizedRampAnimator.this.mDisplayEngineManager.setScene(21, 17);
                if (HwNormalizedRampAnimator.DEBUG) {
                    Slog.i(HwNormalizedRampAnimator.TAG, "send DE_ACTION_MODE_Off For RGBW");
                }
            }
        }
    };
    private boolean mProximityState = false;
    /* access modifiers changed from: private */
    public boolean mProximityStateRecovery = false;
    /* access modifiers changed from: private */
    public int mTargetValueChange;
    private int mTargetValueForRGBW;
    private int mTargetValueLast = -1;

    static {
        boolean z = false;
        if (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) {
            z = true;
        }
        HWDEBUG = z;
    }

    public HwNormalizedRampAnimator(T object, IntProperty<T> property, Context context) {
        super(object, property);
        this.mContext = context;
        this.mHwGradualBrightnessAlgo = new HwGradualBrightnessAlgo();
        this.mData = HwBrightnessXmlLoader.getData();
        this.mDisplayEngineManager = new DisplayEngineManager();
        if (this.mData.rebootFirstBrightnessAnimationEnable) {
            this.mFirstTime = false;
        }
        this.mHBMData = new Bundle();
        updateHBMData(this.mTargetValue, this.mRate, this.mHwGradualBrightnessAlgo.getDuration());
        this.mDisplayEngineManager.registerCallback(this.mDisplayEngineCallback);
    }

    public boolean animateTo(int target, int rate) {
        if (this.mTargetValueLast == 0 && target > 0) {
            Slog.w(TAG, "animateTo: target changing from zero to non-zero with dimming, reset rate to 0!");
            rate = 0;
        }
        if (this.mData.nightUpModeEnable && target != 0 && rate == 0 && this.mNightUpPowerOnWithDimmingEnable) {
            rate = 100;
            String str = TAG;
            Slog.i(str, "NightUpBrightMode set nightUpModeEnable rate=" + 100 + ",target=" + target);
        }
        this.mTargetValueLast = target;
        if (this.mData.updateBrightnessViewAlphaEnable || this.mHbmAheadEnable) {
            if (target == 0) {
                this.mBrightnessLevel = 0;
            }
            if (((target > 0 && this.mBrightnessLevel == 0) || (target > 0 && rate == 0)) && target != this.mBrightnessLevel) {
                this.mHandler.sendEmptyMessage(1);
                if (HWFLOW) {
                    String str2 = TAG;
                    Slog.i(str2, "BrightnessViewAlpha mBrightnessLevel=" + this.mBrightnessLevel + "-->target=" + target + ",rate=" + rate);
                }
                this.mBrightnessLevel = target;
            }
        }
        if (this.mData.animatingForRGBWEnable && rate <= 0 && target == 0 && this.mTargetValueForRGBW >= 4) {
            this.mModeOffForRGBW = true;
            this.mDisplayEngineManager.setScene(21, 17);
            if (DEBUG) {
                Slog.i(TAG, "send DE_ACTION_MODE_off For RGBW");
            }
        }
        this.mTargetValueForRGBW = target;
        float targetOut = (float) target;
        if (target > 4 && this.mData.darkLightLevelMaxThreshold > 4 && target > this.mData.darkLightLevelMinThreshold && target < this.mData.darkLightLevelMaxThreshold) {
            float ratio = (float) Math.pow((double) ((targetOut - 4.0f) / ((float) (this.mData.darkLightLevelMaxThreshold - 4))), (double) this.mData.darkLightLevelRatio);
            targetOut = (((float) (this.mData.darkLightLevelMaxThreshold - 4)) * ratio) + 4.0f;
            if (DEBUG) {
                String str3 = TAG;
                Slog.i(str3, "DarkLightLevel targetIn255=" + target + ",targetOut255=" + targetOut + ",ratio=" + ratio);
            }
        }
        int target2 = (int) ((10000.0f * targetOut) / 255.0f);
        boolean ret = HwNormalizedRampAnimator.super.animateTo(target2, rate);
        if (rate == 0) {
            updateHBMData(target2, rate, 0.0f);
        }
        return ret;
    }

    /* access modifiers changed from: protected */
    public void notifyAlgoUpdateCurrentValue() {
        this.mHwGradualBrightnessAlgo.updateTargetAndRate(this.mTargetValue, this.mRate);
        this.mHwGradualBrightnessAlgo.updateCurrentBrightnessValue((float) this.mCurrentValue);
    }

    public void updateBrightnessRampPara(boolean automode, int updateAutoBrightnessCount, boolean intervened, int state) {
        boolean z = DEBUG;
        this.mBrightnessAdjustMode = automode;
        if (this.mBrightnessAdjustMode) {
            HwGradualBrightnessAlgo hwGradualBrightnessAlgo = this.mHwGradualBrightnessAlgo;
            boolean z2 = true;
            if (!(updateAutoBrightnessCount == 1 || updateAutoBrightnessCount == 0)) {
                z2 = false;
            }
            hwGradualBrightnessAlgo.isFirstValidAutoBrightness(z2);
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
        HwGradualBrightnessAlgo hwGradualBrightnessAlgo = this.mHwGradualBrightnessAlgo;
        if (hwGradualBrightnessAlgo != null) {
            hwGradualBrightnessAlgo.setBrightnessAnimationTime(animationEnabled, millisecond);
        } else {
            Slog.e(TAG, "mHwGradualBrightnessAlgo=null,can not setBrightnessAnimationTime");
        }
    }

    public void updateScreenLockedAnimationEnable(boolean screenLockedEnable) {
        if ((this.mData.updateBrightnessViewAlphaEnable || this.mHbmAheadEnable) && screenLockedEnable != this.mKeyguradIsLocked) {
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "mKeyguradIsLocked= " + this.mKeyguradIsLocked + "-->screenLockedEnable=" + screenLockedEnable + ",mBrightnessLevel=" + this.mBrightnessLevel);
            }
            if (screenLockedEnable && this.mBrightnessLevel > 0) {
                if (HWFLOW) {
                    String str2 = TAG;
                    Slog.i(str2, "BrightnessViewAlpha updateAlpha mKeyguradIsLocked= " + this.mKeyguradIsLocked + "-->screenLockedEnable=" + screenLockedEnable + ",mBrightnessLevel=" + this.mBrightnessLevel);
                }
                this.mHandler.sendEmptyMessage(1);
            }
            this.mKeyguradIsLocked = screenLockedEnable;
        }
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

    public void updateFastDarkenDimmingEnable(boolean enable) {
        this.mHwGradualBrightnessAlgo.updateFastDarkenDimmingEnable(enable);
    }

    public void updateKeyguardUnlockedFastDarkenDimmingEnable(boolean enable) {
        this.mHwGradualBrightnessAlgo.updateKeyguardUnlockedFastDarkenDimmingEnable(enable);
    }

    /* access modifiers changed from: protected */
    public void postAnimationCallback() {
        this.mHwGradualBrightnessAlgo.updateTargetAndRate(this.mTargetValue, this.mRate);
        this.mChoreographer.postCallback(1, this.mNormalizedAnimationCallback, null);
    }

    /* access modifiers changed from: protected */
    public void cancelAnimationCallback() {
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
                if (DEBUG) {
                    Slog.i(TAG, "send DE_ACTION_MODE_OFF For RGBW");
                }
            }
            if (DEBUG) {
                String str = TAG;
                Slog.i(str, "proximityState=" + proximityState + ",mTargetValue=" + this.mTargetValue + ",mCurrentValue=" + this.mCurrentValue);
            }
        }
    }

    public int getCurrentBrightness() {
        return (this.mCurrentValue * 255) / 10000;
    }

    /* access modifiers changed from: private */
    public void updateHBMData(int target, int rate, float duration) {
        Bundle bundle = this.mHBMData;
        if (bundle == null) {
            Slog.w(TAG, "mHBMData == null, no updateHBMData");
        } else if (bundle.getInt("target") != target || this.mHBMData.getInt("rate") != rate || ((double) Math.abs(this.mHBMData.getFloat("duration") - duration)) > 1.0E-6d) {
            this.mHBMData.putInt("target", target);
            this.mHBMData.putInt("rate", rate);
            this.mHBMData.putFloat("duration", duration);
            String str = TAG;
            Slog.i(str, "hbm_dimming target=" + this.mHBMData.getInt("target") + " rate=" + this.mHBMData.getInt("rate") + " duration=" + this.mHBMData.getFloat("duration"));
            this.mDisplayEngineManager.setDataToFilter("HBM", this.mHBMData);
        }
    }

    /* access modifiers changed from: private */
    public void updateBrightnessViewAlpha(int brightness) {
        int brightnessLevel;
        if ((this.mData.updateBrightnessViewAlphaEnable || this.mHbmAheadEnable) && (brightnessLevel = (int) Math.ceil((double) ((((float) brightness) * 255.0f) / 10000.0f))) != this.mBrightnessLevel) {
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "BrightnessViewAlpha mBrightnessLevel=" + this.mBrightnessLevel + "-->brightnessLevel=" + brightnessLevel + ",brightness=" + brightness + ",locked=" + this.mKeyguradIsLocked);
            }
            this.mBrightnessLevel = brightnessLevel;
            if (brightnessLevel > 0 && this.mKeyguradIsLocked) {
                this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateFingerGradualBrightness(int brightness) {
        if (this.mFingerViewController == null) {
            this.mFingerViewController = FingerViewController.getInstance(this.mContext);
        }
        if (HWDEBUG) {
            String str = TAG;
            Slog.i(str, "BrightnessViewAlpha brightnessForAlpha=" + brightness);
        }
        try {
            Class classz = Class.forName("huawei.com.android.server.fingerprint.FingerViewController");
            FingerViewController fingerViewController = this.mFingerViewController;
            classz.getDeclaredMethod("setHighlightViewAlpha", Integer.TYPE).invoke(this.mFingerViewController, Integer.valueOf(brightness));
        } catch (Exception e) {
            Slog.e(TAG, "BrightnessViewAlpha setHighlightViewAlpha exception");
        }
    }

    public void updateNightUpPowerOnWithDimmingEnable(boolean enable) {
        if (this.mHwGradualBrightnessAlgo != null && this.mData.nightUpModeEnable) {
            this.mNightUpPowerOnWithDimmingEnable = enable;
            this.mHwGradualBrightnessAlgo.updateNightUpPowerOnWithDimmingEnable(enable);
        }
    }

    /* access modifiers changed from: private */
    public void updateFrameRate(int frameRate) {
        HwGradualBrightnessAlgo hwGradualBrightnessAlgo = this.mHwGradualBrightnessAlgo;
        if (hwGradualBrightnessAlgo != null) {
            hwGradualBrightnessAlgo.updateFrameRate(frameRate);
        }
    }

    public void updateFrontCameraDimmingEnable(boolean dimmingEnable) {
        if (this.mHwGradualBrightnessAlgo != null && this.mData.frontCameraMaxBrightnessEnable) {
            this.mHwGradualBrightnessAlgo.updateFrontCameraDimmingEnable(dimmingEnable);
        }
    }
}

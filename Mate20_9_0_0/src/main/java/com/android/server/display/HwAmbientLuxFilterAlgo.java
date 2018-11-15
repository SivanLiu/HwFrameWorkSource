package com.android.server.display;

import android.graphics.PointF;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.systemui.shared.recents.hwutil.HwRecentsTaskUtils;
import java.util.Calendar;

public class HwAmbientLuxFilterAlgo {
    private static final int AMBIENT_LIGHT_HORIZON = 20000;
    private static final int AMBIENT_MAX_LUX = 40000;
    private static final int AMBIENT_MIN_LUX = 0;
    private static final int AMBIENT_SCENE_HORIZON = 80000;
    private static final boolean DEBUG;
    private static final int EXTRA_DELAY_TIME = 100;
    private static final int MIN_BRIGHTNESS = 4;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final String TAG = "HwAmbientLuxFilterAlgo";
    private static final long mNormDarkenDebounceTimeForBackSensorCoverMode = 500;
    private static final float mRatioForDarkenBackSensorCoverMode = 0.5f;
    private HwRingBuffer mAmbientLightRingBuffer;
    private HwRingBuffer mAmbientLightRingBufferFilter;
    private HwRingBuffer mAmbientLightRingBufferScene;
    protected float mAmbientLux;
    private float mAmbientLuxNewMax;
    private float mAmbientLuxNewMin;
    public boolean mAutoBrightnessIntervened = false;
    private float mAutoModeEnableFirstLux;
    private int mBackSensorCoverModeBrightness;
    private int mBrightPointCnt = -1;
    private float mBrightenDeltaLuxMax;
    private float mBrightenDeltaLuxMin;
    private boolean mCoverModeDayEnable = false;
    private boolean mCoverState = false;
    private float mDarkenDeltaLuxForBackSensorCoverMode;
    private float mDarkenDeltaLuxMax;
    private float mDarkenDeltaLuxMin;
    private final Data mData;
    private boolean mDayModeEnable = false;
    private boolean mFirstAmbientLux = true;
    private boolean mFirstSetBrightness = true;
    private boolean mGameModeEnable = false;
    private boolean mIsCoverModeFastResponseFlag = false;
    private boolean mIsclosed = false;
    private boolean mKeyguardIsLocked;
    private float mLastCloseScreenLux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private HwRingBuffer mLastCloseScreenRingBuffer;
    private int mLastCloseTime = -1;
    private float mLastObservedLux;
    private final int mLightSensorRate;
    private final Object mLock = new Object();
    private float mLuxBufferAvg = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mLuxBufferAvgMax = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mLuxBufferAvgMin = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private boolean mNeedToSendProximityDebounceMsg = false;
    private boolean mNeedToUpdateBrightness;
    public long mNextTransitionTime = -1;
    protected long mNormBrighenDebounceTime;
    protected long mNormBrighenDebounceTimeForSmallThr;
    protected long mNormDarkenDebounceTime;
    protected long mNormDarkenDebounceTimeForSmallThr;
    private boolean mOffsetResetEnable = false;
    private float mOffsetValidAmbientBrightenDeltaLux;
    private float mOffsetValidAmbientDarkenDeltaLux;
    private float mOffsetValidAmbientLux;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPowerStatus = false;
    private long mPrintLogTime = 0;
    private int mProximity = -1;
    private int mProximityNegativeDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
    private int mProximityPositiveDebounceTime = 150;
    private boolean mProximityPositiveStatus;
    private int mResponseDurationPoints;
    private float mSceneAmbientLuxMax;
    private float mSceneAmbientLuxMin;
    private float mSceneAmbientLuxWeight;
    private float mStability = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mStabilityBrightenConstant = 101.0f;
    private float mStabilityBrightenConstantForSmallThr;
    private float mStabilityDarkenConstant = 101.0f;
    private float mStabilityDarkenConstantForSmallThr;
    private float mStabilityForSmallThr = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float mlastFilterLux;

    public interface Callbacks {
        void updateBrightness();
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwAmbientLuxFilterAlgo(int lightSensorRate) {
        this.mLightSensorRate = lightSensorRate;
        this.mNeedToUpdateBrightness = false;
        this.mAmbientLightRingBuffer = new HwRingBuffer(50);
        this.mAmbientLightRingBufferFilter = new HwRingBuffer(50);
        this.mAmbientLightRingBufferScene = new HwRingBuffer(GestureNavConst.GESTURE_MOVE_TIME_THRESHOLD_4);
        this.mLastCloseScreenRingBuffer = new HwRingBuffer(50);
        this.mData = HwBrightnessXmlLoader.getData();
    }

    public void isFirstAmbientLux(boolean isFirst) {
        this.mFirstAmbientLux = isFirst;
    }

    public void handleLightSensorEvent(long time, float lux) {
        synchronized (this.mLock) {
            if (!this.mFirstAmbientLux && lux > this.mData.darkLightLuxMinThreshold && lux < this.mData.darkLightLuxMaxThreshold && this.mData.darkLightLuxMinThreshold < this.mData.darkLightLuxMaxThreshold) {
                lux += this.mData.darkLightLuxDelta;
                if (lux < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    lux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                }
            }
            if (lux > 40000.0f) {
                lux = 40000.0f;
            }
            try {
                applyLightSensorMeasurement(time, lux);
                updateAmbientLux(time);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mAmbientLightRingBuffer.prune(time - HwRecentsTaskUtils.MAX_REMOVE_TASK_TIME);
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
    }

    public float getCurrentAmbientLux() {
        return this.mAmbientLux;
    }

    private void setAmbientLux(float lux) {
        this.mAmbientLux = (float) Math.round(lux);
        if (this.mAmbientLux < 10.0f) {
            this.mStabilityBrightenConstantForSmallThr = 0.5f;
            this.mStabilityDarkenConstantForSmallThr = 0.5f;
        } else if (this.mAmbientLux < 10.0f || this.mAmbientLux >= 50.0f) {
            this.mStabilityBrightenConstantForSmallThr = 5.0f;
            this.mStabilityDarkenConstantForSmallThr = 5.0f;
        } else {
            this.mStabilityBrightenConstantForSmallThr = 3.0f;
            this.mStabilityDarkenConstantForSmallThr = 3.0f;
        }
        updatepara(this.mAmbientLightRingBuffer, this.mAmbientLux);
        this.mResponseDurationPoints = 0;
    }

    public void updateAmbientLux() {
        synchronized (this.mLock) {
            long time = SystemClock.uptimeMillis();
            try {
                this.mAmbientLightRingBuffer.push(time, this.mLastObservedLux);
                this.mAmbientLightRingBuffer.prune(time - HwRecentsTaskUtils.MAX_REMOVE_TASK_TIME);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateAmbientLux:time=");
                    stringBuilder.append(time);
                    stringBuilder.append(",mLastObservedLux=");
                    stringBuilder.append(this.mLastObservedLux);
                    Slog.d(str, stringBuilder.toString());
                }
                updateAmbientLux(time);
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private float modifyFirstAmbientLux(float ambientLux) {
        int N = this.mLastCloseScreenRingBuffer.size();
        if (N > 0 && this.mData.initNumLastBuffer > 0) {
            float tmpLux;
            float sumLux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            float interfere = this.mData.initDoubleSensorInterfere;
            int cntValidData = 0;
            for (int i = N - 1; i >= 0; i--) {
                tmpLux = this.mLastCloseScreenRingBuffer.getLux(i);
                if (Math.abs(this.mAmbientLightRingBuffer.getTime(0) - this.mLastCloseScreenRingBuffer.getTime(i)) < this.mData.initValidCloseTime && Math.abs(ambientLux - tmpLux) < 1.5f * interfere) {
                    sumLux += tmpLux;
                    cntValidData++;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LastScreenBuffer: sumLux=");
            stringBuilder.append(sumLux);
            stringBuilder.append(", cntValidData=");
            stringBuilder.append(cntValidData);
            stringBuilder.append(", InambientLux=");
            stringBuilder.append(ambientLux);
            Slog.i(str, stringBuilder.toString());
            if (((float) cntValidData) > 1.0E-7f && ambientLux < ((float) this.mData.initUpperLuxThreshold) + 1.0E-6f && sumLux / ((float) cntValidData) < 1.0E-6f + ambientLux) {
                float ave = sumLux / ((float) cntValidData);
                tmpLux = 1.0f / (((float) Math.exp((double) ((-this.mData.initSigmoidFuncSlope) * (interfere - Math.abs(ambientLux - ave))))) + 1.0f);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("modifyFirstAmbientLux : lambda=");
                stringBuilder2.append(tmpLux);
                stringBuilder2.append(", ave");
                stringBuilder2.append(ave);
                stringBuilder2.append(", ambientLux=");
                stringBuilder2.append(ambientLux);
                Slog.i(str2, stringBuilder2.toString());
                return (tmpLux * ave) + ((1.0f - tmpLux) * ambientLux);
            }
        }
        return ambientLux;
    }

    private float calcAmbientLuxInCoverState(float ambientLux) {
        String str;
        StringBuilder stringBuilder;
        if (this.mPowerStatus) {
            if (!this.mData.coverModelastCloseScreenEnable) {
                ambientLux = this.mLastCloseScreenLux;
                this.mAmbientLightRingBuffer.putLux(0, ambientLux);
                this.mAmbientLightRingBufferFilter.putLux(0, ambientLux);
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LabcCoverMode1 use lastCloseScreenLux=");
                    stringBuilder.append(this.mLastCloseScreenLux);
                    Slog.i(str, stringBuilder.toString());
                }
            }
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LabcCoverMode1 ambientLux=");
                stringBuilder.append(ambientLux);
                stringBuilder.append(",mCoverState=");
                stringBuilder.append(this.mCoverState);
                stringBuilder.append(",mPowerStatus=");
                stringBuilder.append(this.mPowerStatus);
                Slog.i(str, stringBuilder.toString());
            }
        } else {
            StringBuilder stringBuilder2;
            if (this.mData.lastCloseScreenEnable) {
                ambientLux = this.mLastCloseScreenLux;
            } else if (this.mData.backSensorCoverModeEnable) {
                ambientLux = (float) getLuxFromDefaultBrightnessLevel((float) this.mBackSensorCoverModeBrightness);
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("BackSensorCoverMode ambientLux=");
                stringBuilder2.append(ambientLux);
                stringBuilder2.append(", coverModeBrightness=");
                stringBuilder2.append(this.mBackSensorCoverModeBrightness);
                Slog.i(str, stringBuilder2.toString());
            } else if (!this.mData.coverModeDayEnable) {
                ambientLux = this.mData.coverModeFirstLux;
            } else if (this.mCoverModeDayEnable) {
                ambientLux = (float) getLuxFromDefaultBrightnessLevel((float) this.mData.coverModeDayBrightness);
            } else {
                ambientLux = (float) getLuxFromDefaultBrightnessLevel((float) getCoverModeBrightnessFromLastScreenBrightness());
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("LabcCoverMode NewambientLux=");
                stringBuilder2.append(ambientLux);
                stringBuilder2.append(",LastScreenBrightness=");
                stringBuilder2.append(getCoverModeBrightnessFromLastScreenBrightness());
                Slog.i(str, stringBuilder2.toString());
            }
            this.mAmbientLightRingBuffer.putLux(0, ambientLux);
            this.mAmbientLightRingBufferFilter.putLux(0, ambientLux);
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LabcCoverMode ambientLux=");
                stringBuilder.append(ambientLux);
                stringBuilder.append(",mCoverState=");
                stringBuilder.append(this.mCoverState);
                stringBuilder.append(",mPowerStatus=");
                stringBuilder.append(this.mPowerStatus);
                Slog.i(str, stringBuilder.toString());
            }
        }
        return ambientLux;
    }

    private void updateAmbientLux(long time) {
        long nextBrightenTransitionOffset;
        long nextDarkenTransitionOffset;
        boolean needToBrightenOffset;
        boolean needToDarkenOffset;
        boolean z;
        String str;
        StringBuilder stringBuilder;
        long j = time;
        float filterLux = prefilterAmbientLux(j, this.mData.preMethodNum);
        float lastestLux = getOrigLastAmbientLux(time);
        updateBuffer(j, filterLux, AMBIENT_LIGHT_HORIZON);
        updateBufferForScene(j, filterLux, AMBIENT_SCENE_HORIZON);
        this.mlastFilterLux = getFilterLastAmbientLux(time);
        float ambientLux = postfilterAmbientLux(j, this.mData.postMethodNum);
        if (this.mFirstAmbientLux) {
            if (this.mCoverState) {
                this.mCoverState = false;
                ambientLux = calcAmbientLuxInCoverState(ambientLux);
            }
            ambientLux = modifyFirstAmbientLux(ambientLux);
            setAmbientLux(ambientLux);
            if (this.mData.offsetValidAmbientLuxEnable) {
                setOffsetValidAmbientLux(ambientLux);
            }
            this.mFirstAmbientLux = false;
            this.mCoverModeDayEnable = false;
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateAmbientLux: Initializing: mAmbientLightRingBuffer=");
                stringBuilder2.append(this.mAmbientLightRingBuffer);
                stringBuilder2.append(", mAmbientLux=");
                stringBuilder2.append(this.mAmbientLux);
                stringBuilder2.append(",mLastCloseScreenLux=");
                stringBuilder2.append(this.mLastCloseScreenLux);
                stringBuilder2.append(",mAmbientLightRingBufferFilter=");
                stringBuilder2.append(this.mAmbientLightRingBufferFilter);
                Slog.d(str2, stringBuilder2.toString());
            }
            this.mNeedToUpdateBrightness = true;
        }
        float ambientLux2 = ambientLux;
        updateNewAmbientLuxFromScene(this.mAmbientLightRingBufferScene);
        updatepara(this.mAmbientLightRingBuffer, this.mAmbientLux);
        long nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        boolean needToBrighten = decideToBrighten(ambientLux2);
        boolean needToBrightenNew = decideToBrighten(lastestLux);
        boolean needToDarken = decideToDarken(ambientLux2);
        boolean needToDarkenNew = decideToDarken(lastestLux);
        if (this.mData.offsetValidAmbientLuxEnable) {
            nextBrightenTransitionOffset = nextAmbientLightBrighteningTransitionForOffset(time);
            nextDarkenTransitionOffset = nextAmbientLightDarkeningTransitionExtended(j, this.mOffsetValidAmbientLux, this.mOffsetValidAmbientDarkenDeltaLux, (long) this.mData.offsetDarkenDebounceTime);
            needToBrightenOffset = decideToBrightenForOffset(ambientLux2);
            needToDarkenOffset = decideToDarkenForOffset(ambientLux2);
            z = needToBrightenOffset && nextBrightenTransitionOffset <= j;
            needToBrightenOffset = z;
            z = needToDarkenOffset && nextDarkenTransitionOffset <= j;
            needToDarkenOffset = z;
            if (this.mData.offsetValidAmbientLuxEnable && (needToBrightenOffset | needToDarkenOffset) != 0) {
                setOffsetValidAmbientLux(ambientLux2);
            }
        }
        if (((float) this.mBrightPointCnt) > -1.0E-6f) {
            this.mBrightPointCnt++;
        }
        if (this.mBrightPointCnt > this.mData.outdoorResponseCount) {
            this.mBrightPointCnt = -1;
        }
        nextBrightenTransitionOffset = nextAmbientLightBrighteningTransitionForSmallThr(time);
        nextDarkenTransitionOffset = nextAmbientLightDarkeningTransitionExtended(j, this.mAmbientLux, this.mDarkenDeltaLuxMin, this.mNormDarkenDebounceTimeForSmallThr);
        needToBrightenOffset = decideToBrightenForSmallThr(ambientLux2);
        needToDarkenOffset = decideToDarkenForSmallThr(ambientLux2);
        z = needToBrightenOffset && nextBrightenTransitionOffset <= j;
        needToBrightenOffset = z;
        z = needToDarkenOffset && nextDarkenTransitionOffset <= j;
        needToDarkenOffset = z;
        z = (needToBrighten && needToBrightenNew && nextBrightenTransition <= j) || needToBrightenOffset;
        boolean needToDarken2 = (needToDarken && needToDarkenNew && nextDarkenTransition <= j) || needToDarkenOffset || needToDarkenForBackSensorCoverMode(j, ambientLux2);
        float brightenLux = this.mAmbientLux + this.mBrightenDeltaLuxMax;
        float darkenLux = this.mAmbientLux - this.mDarkenDeltaLuxMax;
        if (!DEBUG || j - this.mPrintLogTime <= 2000) {
            float f = lastestLux;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("t=");
            stringBuilder.append(j);
            stringBuilder.append(",lx=");
            stringBuilder.append(this.mAmbientLightRingBuffer.toString(8.4E-45f));
            stringBuilder.append(",mLx=");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(",s=");
            stringBuilder.append(this.mStability);
            stringBuilder.append(",ss=");
            stringBuilder.append(this.mStabilityForSmallThr);
            stringBuilder.append(",AuIntervened=");
            stringBuilder.append(this.mAutoBrightnessIntervened);
            stringBuilder.append(",mOffLux=");
            stringBuilder.append(this.mOffsetValidAmbientLux);
            stringBuilder.append(",mProximityState=");
            stringBuilder.append(this.mProximityPositiveStatus);
            stringBuilder.append(",bLux=");
            stringBuilder.append(brightenLux);
            stringBuilder.append(",dLux=");
            stringBuilder.append(darkenLux);
            stringBuilder.append(",mDt=");
            stringBuilder.append(this.mNormDarkenDebounceTime);
            stringBuilder.append(",mBt=");
            stringBuilder.append(this.mNormBrighenDebounceTime);
            stringBuilder.append(",mMax=");
            stringBuilder.append(this.mAmbientLuxNewMax);
            stringBuilder.append(",mMin=");
            stringBuilder.append(this.mAmbientLuxNewMin);
            stringBuilder.append(",mu=");
            stringBuilder.append(this.mSceneAmbientLuxWeight);
            stringBuilder.append(",sMax=");
            stringBuilder.append(this.mSceneAmbientLuxMax);
            stringBuilder.append(",sMin=");
            stringBuilder.append(this.mSceneAmbientLuxMin);
            Slog.d(str, stringBuilder.toString());
            this.mPrintLogTime = j;
        }
        if ((z | needToDarken2) != 0) {
            this.mBrightPointCnt = 0;
            setAmbientLux(ambientLux2);
            if (this.mData.offsetValidAmbientLuxEnable && (needToBrightenOffset || needToDarkenOffset)) {
                setOffsetValidAmbientLux(ambientLux2);
            }
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateAmbientLux: ");
                stringBuilder.append(z ? "Brightened" : "Darkened");
                stringBuilder.append(", mAmbientLightRingBuffer=");
                stringBuilder.append(this.mAmbientLightRingBuffer.toString(6));
                stringBuilder.append(",mPxs=");
                stringBuilder.append(this.mProximityPositiveStatus);
                stringBuilder.append(", mAmbientLux=");
                stringBuilder.append(this.mAmbientLux);
                stringBuilder.append(",s=");
                stringBuilder.append(this.mStability);
                stringBuilder.append(",ss=");
                stringBuilder.append(this.mStabilityForSmallThr);
                stringBuilder.append(",needBs=");
                stringBuilder.append(needToBrightenOffset);
                stringBuilder.append(",needDs=");
                stringBuilder.append(needToDarkenOffset);
                stringBuilder.append(", mAmbientLightRingBufferF=");
                stringBuilder.append(this.mAmbientLightRingBufferFilter.toString(6));
                Slog.d(str, stringBuilder.toString());
            }
            if (DEBUG && this.mIsCoverModeFastResponseFlag) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CoverModeBResponseTime=");
                stringBuilder.append(this.mData.coverModeBrightenResponseTime);
                stringBuilder.append(",CoverModeDResponseTime=");
                stringBuilder.append(this.mData.coverModeDarkenResponseTime);
                Slog.i(str, stringBuilder.toString());
            }
            if (DEBUG && this.mPowerStatus) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("PowerOnBT=");
                stringBuilder.append(this.mData.powerOnBrightenDebounceTime);
                stringBuilder.append(",PowerOnDT=");
                stringBuilder.append(this.mData.powerOnDarkenDebounceTime);
                Slog.i(str, stringBuilder.toString());
            }
            this.mNeedToUpdateBrightness = true;
            nextBrightenTransition = nextAmbientLightBrighteningTransition(time);
            nextDarkenTransition = nextAmbientLightDarkeningTransition(time);
        }
        this.mNextTransitionTime = nextDarkenTransition < nextBrightenTransition ? nextDarkenTransition : nextBrightenTransition;
        if (this.mNextTransitionTime > j) {
            nextDarkenTransitionOffset = this.mNextTransitionTime + ((long) this.mLightSensorRate);
        } else {
            boolean z2 = needToDarkenOffset;
            nextDarkenTransitionOffset = ((long) this.mLightSensorRate) + j;
        }
        this.mNextTransitionTime = nextDarkenTransitionOffset + 100;
        if (DEBUG && j - this.mPrintLogTime > 2000) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateAmbientLux: Scheduling ambient lux update for ");
            stringBuilder.append(this.mNextTransitionTime);
            stringBuilder.append(TimeUtils.formatUptime(this.mNextTransitionTime));
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void updateNewAmbientLuxFromScene(HwRingBuffer hwBuffer) {
        int N = hwBuffer.size();
        this.mAmbientLuxNewMax = this.mAmbientLux;
        this.mAmbientLuxNewMin = this.mAmbientLux;
        this.mSceneAmbientLuxMax = this.mAmbientLux;
        this.mSceneAmbientLuxMin = this.mAmbientLux;
        if (this.mResponseDurationPoints == Integer.MAX_VALUE) {
            this.mResponseDurationPoints = Integer.MAX_VALUE;
        } else {
            this.mResponseDurationPoints++;
        }
        if (N != 0 && N >= this.mData.sceneGapPoints && this.mResponseDurationPoints - this.mData.sceneMinPoints >= this.mData.sceneGapPoints && this.mData.sceneMaxPoints >= this.mData.sceneMinPoints && this.mData.sceneMaxPoints + this.mData.sceneGapPoints <= 228) {
            updateSceneBufferAmbientLuxMaxMinAvg(hwBuffer, this.mResponseDurationPoints < this.mData.sceneMaxPoints + this.mData.sceneGapPoints ? N - this.mResponseDurationPoints : (N - this.mData.sceneMaxPoints) - this.mData.sceneGapPoints, N - this.mData.sceneGapPoints);
            this.mSceneAmbientLuxWeight = ((float) this.mData.sceneGapPoints) / ((float) this.mResponseDurationPoints);
            if (this.mAmbientLux > this.mSceneAmbientLuxMax) {
                this.mAmbientLuxNewMax = (this.mSceneAmbientLuxWeight * this.mAmbientLux) + ((1.0f - this.mSceneAmbientLuxWeight) * this.mSceneAmbientLuxMax);
            }
            if (this.mAmbientLux > this.mSceneAmbientLuxMin) {
                this.mAmbientLuxNewMin = (this.mSceneAmbientLuxWeight * this.mAmbientLux) + ((1.0f - this.mSceneAmbientLuxWeight) * this.mSceneAmbientLuxMin);
            }
            correctAmbientLux();
        }
    }

    private void updateSceneBufferAmbientLuxMaxMinAvg(HwRingBuffer buffer, int start, int end) {
        int N = buffer.size();
        if (N == 0 || end < start || start > N - 1 || end < 0 || start < 0 || end > N - 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SceneBufferAmbientLux input error,end=");
            stringBuilder.append(end);
            stringBuilder.append(",start=");
            stringBuilder.append(start);
            stringBuilder.append(",N=");
            stringBuilder.append(N);
            Slog.i(str, stringBuilder.toString());
            return;
        }
        float luxMin = buffer.getLux(start);
        float luxMax = buffer.getLux(start);
        float luxMin2 = luxMin;
        luxMin = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        for (int i = start; i <= end; i++) {
            float lux = buffer.getLux(i);
            if (lux > luxMax) {
                luxMax = lux;
            }
            if (lux < luxMin2) {
                luxMin2 = lux;
            }
            luxMin += lux;
        }
        float luxMean = (float) Math.round(luxMin / ((float) ((end - start) + 1)));
        this.mSceneAmbientLuxMax = (this.mData.sceneAmbientLuxMaxWeight * luxMean) + ((1.0f - this.mData.sceneAmbientLuxMaxWeight) * luxMax);
        this.mSceneAmbientLuxMin = (this.mData.sceneAmbientLuxMinWeight * luxMean) + ((1.0f - this.mData.sceneAmbientLuxMinWeight) * luxMin2);
    }

    private void correctAmbientLux() {
        String str;
        StringBuilder stringBuilder;
        float ambientLuxDarkenDelta = calculateDarkenThresholdDelta(this.mAmbientLux);
        float ambientLuxNewMaxBrightenDelta = calculateBrightenThresholdDelta(this.mAmbientLuxNewMax);
        float ambientLuxNewMinBrightenDelta = calculateBrightenThresholdDelta(this.mAmbientLuxNewMin);
        if (this.mAmbientLux - ambientLuxDarkenDelta > this.mAmbientLuxNewMax - 1.0E-5f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" Reset mAmbientLuxNewMax:mAmbientLux");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(", ambientLuxDarkenDelta=");
            stringBuilder.append(ambientLuxDarkenDelta);
            stringBuilder.append(", mAmbientLuxNewMax=");
            stringBuilder.append(this.mAmbientLuxNewMax);
            Slog.i(str, stringBuilder.toString());
            this.mAmbientLuxNewMax = this.mAmbientLux;
        }
        if (this.mAmbientLux > (this.mAmbientLuxNewMax + ambientLuxNewMaxBrightenDelta) - 1.0E-5f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" Reset mAmbientLuxNewMax:mAmbientLux");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(", ambientLuxNewMaxBrightenDelta=");
            stringBuilder.append(ambientLuxNewMaxBrightenDelta);
            stringBuilder.append(", mAmbientLuxNewMax=");
            stringBuilder.append(this.mAmbientLuxNewMax);
            Slog.i(str, stringBuilder.toString());
            this.mAmbientLuxNewMax = this.mAmbientLux;
        }
        if (this.mAmbientLux - ambientLuxDarkenDelta > this.mAmbientLuxNewMin - 1.0E-5f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" Reset mAmbientLuxNewMin:mAmbientLux");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(", ambientLuxDarkenDelta=");
            stringBuilder.append(ambientLuxDarkenDelta);
            stringBuilder.append(", mAmbientLuxNewMin=");
            stringBuilder.append(this.mAmbientLuxNewMin);
            Slog.i(str, stringBuilder.toString());
            this.mAmbientLuxNewMin = this.mAmbientLux;
        }
        if (this.mAmbientLux > (this.mAmbientLuxNewMin + ambientLuxNewMinBrightenDelta) - 1.0E-5f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" Reset mAmbientLuxNewMin:mAmbientLux");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(", ambientLuxNewMinBrightenDelta=");
            stringBuilder.append(ambientLuxNewMinBrightenDelta);
            stringBuilder.append(", mAmbientLuxNewMin=");
            stringBuilder.append(this.mAmbientLuxNewMin);
            Slog.i(str, stringBuilder.toString());
            this.mAmbientLuxNewMin = this.mAmbientLux;
        }
    }

    public boolean needToUpdateBrightness() {
        return this.mNeedToUpdateBrightness;
    }

    public boolean brightnessUpdated() {
        this.mNeedToUpdateBrightness = false;
        return false;
    }

    public boolean needToSendUpdateAmbientLuxMsg() {
        return this.mNextTransitionTime > 0;
    }

    public long getSendUpdateAmbientLuxMsgTime() {
        return this.mNextTransitionTime;
    }

    private long getNextAmbientLightBrighteningTime(long earliedtime) {
        if (this.mIsCoverModeFastResponseFlag) {
            return this.mData.coverModeBrightenResponseTime + earliedtime;
        }
        if (getKeyguardLockedBrightenEnable()) {
            return ((long) this.mData.keyguardResponseBrightenTime) + earliedtime;
        }
        if (getOutdoorModeBrightenEnable()) {
            return ((long) this.mData.outdoorResponseBrightenTime) + earliedtime;
        }
        if (getProximityPositiveBrightenEnable()) {
            return ((long) this.mData.proximityResponseBrightenTime) + earliedtime;
        }
        if (this.mPowerStatus) {
            if (getSlowResponsePowerStatus()) {
                return (((long) this.mData.powerOnBrightenDebounceTime) + earliedtime) + ((long) this.mData.initSlowReponseBrightTime);
            }
            return ((long) this.mData.powerOnBrightenDebounceTime) + earliedtime;
        } else if (this.mGameModeEnable) {
            return this.mData.gameModeBrightenDebounceTime + earliedtime;
        } else {
            return this.mNormBrighenDebounceTime + earliedtime;
        }
    }

    private long getNextAmbientLightDarkeningTime(long earliedtime) {
        if (this.mIsCoverModeFastResponseFlag) {
            return this.mData.coverModeDarkenResponseTime + earliedtime;
        }
        if (getKeyguardLockedDarkenEnable()) {
            return ((long) this.mData.keyguardResponseDarkenTime) + earliedtime;
        }
        if (getOutdoorModeDarkenEnable()) {
            return ((long) this.mData.outdoorResponseDarkenTime) + earliedtime;
        }
        if (this.mPowerStatus) {
            return ((long) this.mData.powerOnDarkenDebounceTime) + earliedtime;
        }
        if (this.mGameModeEnable) {
            return this.mData.gameModeDarkenDebounceTime + earliedtime;
        }
        return this.mNormDarkenDebounceTime + earliedtime;
    }

    /* JADX WARNING: Missing block: B:15:0x0034, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getKeyguardLockedBrightenEnable() {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "getKeyguardLocked no lux");
            return false;
        } else if (!this.mKeyguardIsLocked || this.mAmbientLightRingBuffer.getLux(N - 1) <= this.mData.keyguardLuxThreshold || this.mData.keyguardResponseBrightenTime <= 0 || getProximityPositiveBrightenEnable()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean getOutdoorModeBrightenEnable() {
        if (((float) this.mBrightPointCnt) <= -1.0E-6f || this.mAmbientLux <= ((float) this.mData.outdoorLowerLuxThreshold) || this.mData.outdoorResponseBrightenTime <= 0) {
            return false;
        }
        return true;
    }

    private boolean getProximityPositiveBrightenEnable() {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "getProximityPositive no lux");
            return false;
        } else if (!this.mData.allowLabcUseProximity || !this.mProximityPositiveStatus || this.mAmbientLightRingBuffer.getLux(N - 1) >= this.mData.proximityLuxThreshold || this.mData.proximityResponseBrightenTime <= 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean getSlowResponsePowerStatus() {
        int N = this.mAmbientLightRingBuffer.size();
        if (N > 0) {
            for (int i = 0; i < N; i++) {
                if (this.mAmbientLightRingBuffer.getLux(i) > ((float) this.mData.initSlowReponseUpperLuxThreshold) + 1.0E-6f) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean getKeyguardLockedDarkenEnable() {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "getKeyguardLocked no lux");
            return false;
        } else if (!this.mKeyguardIsLocked || this.mAmbientLightRingBuffer.getLux(N - 1) <= this.mData.keyguardLuxThreshold || this.mData.keyguardResponseDarkenTime <= 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean getOutdoorModeDarkenEnable() {
        if (((float) this.mBrightPointCnt) <= -1.0E-6f || this.mAmbientLux <= ((float) this.mData.outdoorLowerLuxThreshold) || this.mData.outdoorResponseDarkenTime <= 0) {
            return false;
        }
        return true;
    }

    public void setPowerStatus(boolean powerStatus) {
        this.mPowerStatus = powerStatus;
    }

    public void clear() {
        synchronized (this.mLock) {
            int i;
            int pruneN;
            String str;
            StringBuilder stringBuilder;
            if (DEBUG) {
                Slog.d(TAG, "clear buffer data and algo flags");
            }
            this.mLastCloseScreenLux = this.mAmbientLux;
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("LabcCoverMode clear: mLastCloseScreenLux=");
                stringBuilder2.append(this.mLastCloseScreenLux);
                Slog.d(str2, stringBuilder2.toString());
            }
            if (this.mData.initNumLastBuffer > 0) {
                int N = this.mAmbientLightRingBuffer.size();
                for (i = 0; i < N; i++) {
                    this.mLastCloseScreenRingBuffer.push(this.mAmbientLightRingBuffer.getTime(i), this.mAmbientLightRingBuffer.getLux(i));
                }
                i = this.mLastCloseScreenRingBuffer.size() - this.mData.initNumLastBuffer;
                pruneN = i > 0 ? i : 0;
                if (pruneN > 0) {
                    this.mLastCloseScreenRingBuffer.prune(1 + this.mLastCloseScreenRingBuffer.getTime(pruneN - 1));
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mLastCloseScreenRingBuffer=");
                    stringBuilder.append(this.mLastCloseScreenRingBuffer.toString(this.mLastCloseScreenRingBuffer.size()));
                    Slog.d(str, stringBuilder.toString());
                }
            } else {
                Slog.i(TAG, "mLastCloseScreenRingBuffer is set empty!");
            }
            this.mIsCoverModeFastResponseFlag = false;
            this.mAutoBrightnessIntervened = false;
            this.mProximityPositiveStatus = false;
            this.mProximity = -1;
            this.mPendingProximity = -1;
            this.mAmbientLightRingBuffer.clear();
            this.mAmbientLightRingBufferFilter.clear();
            this.mAmbientLightRingBufferScene.clear();
            this.mBrightPointCnt = -1;
            if (this.mData.dayModeAlgoEnable || this.mData.offsetResetEnable) {
                this.mFirstSetBrightness = false;
                Calendar c = Calendar.getInstance();
                int lastCloseDay = c.get(6);
                i = c.get(11);
                pruneN = c.get(12);
                this.mLastCloseTime = (((lastCloseDay * 24) * 60) + (i * 60)) + pruneN;
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DayMode: lastCloseDay=");
                    stringBuilder.append(lastCloseDay);
                    stringBuilder.append(",lastCloseHour=");
                    stringBuilder.append(i);
                    stringBuilder.append(",lastCloseMinute=");
                    stringBuilder.append(pruneN);
                    stringBuilder.append(",mLastCloseTime=");
                    stringBuilder.append(this.mLastCloseTime);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private void updateBuffer(long time, float ambientLux, int horizon) {
        this.mAmbientLightRingBufferFilter.push(time, ambientLux);
        this.mAmbientLightRingBufferFilter.prune(time - ((long) horizon));
    }

    private void updateBufferForScene(long time, float ambientLux, int horizon) {
        this.mAmbientLightRingBufferScene.push(time, ambientLux);
        this.mAmbientLightRingBufferScene.prune(time - ((long) horizon));
    }

    private void updatepara(HwRingBuffer buffer, float lux) {
        float stability = calculateStability(buffer);
        float stabilityForSmallThr = calculateStabilityForSmallThr(buffer);
        if (stability > 100.0f) {
            this.mStability = 100.0f;
        } else if (stability < ((float) this.mData.stabilityConstant)) {
            this.mStability = (float) this.mData.stabilityConstant;
        } else {
            this.mStability = stability;
        }
        if (stabilityForSmallThr > 100.0f) {
            this.mStabilityForSmallThr = 100.0f;
        } else {
            this.mStabilityForSmallThr = stabilityForSmallThr;
        }
        float mLux = (float) Math.round(lux);
        if (mLux >= this.mData.brightTimeDelayLuxThreshold || this.mlastFilterLux >= this.mData.brightTimeDelayLuxThreshold || !this.mData.brightTimeDelayEnable) {
            this.mNormBrighenDebounceTime = (long) (((float) this.mData.brighenDebounceTime) * (((this.mData.brightenDebounceTimeParaBig * (this.mStability - ((float) this.mData.stabilityConstant))) / 100.0f) + 1.0f));
        } else {
            this.mNormBrighenDebounceTime = (long) this.mData.brightTimeDelay;
        }
        if (mLux >= this.mData.darkTimeDelayLuxThreshold || !this.mData.darkTimeDelayEnable) {
            this.mNormDarkenDebounceTime = (long) (((float) this.mData.darkenDebounceTime) * (1.0f + ((this.mData.darkenDebounceTimeParaBig * (this.mStability - ((float) this.mData.stabilityConstant))) / 100.0f)));
        } else {
            float ambientLuxDarkenDelta = calculateDarkenThresholdDelta(this.mAmbientLux);
            float currentAmbientLux = buffer.getLux(buffer.size() - 1);
            float luxNormalizedFactor = ((this.mData.darkTimeDelayBeta2 * (this.mAmbientLux - currentAmbientLux)) + (this.mData.darkTimeDelayBeta1 * ((this.mAmbientLux - currentAmbientLux) - ambientLuxDarkenDelta))) + 1.0f;
            if (luxNormalizedFactor < 0.001f) {
                this.mNormDarkenDebounceTime = (long) (((float) this.mData.darkTimeDelay) + this.mData.darkTimeDelayBeta0);
            } else {
                this.mNormDarkenDebounceTime = ((long) this.mData.darkTimeDelay) + ((long) ((this.mData.darkTimeDelayBeta0 * (1.0f + (this.mData.darkTimeDelayBeta2 * 1.0f))) / luxNormalizedFactor));
            }
        }
        this.mNormBrighenDebounceTimeForSmallThr = (long) this.mData.brighenDebounceTimeForSmallThr;
        this.mNormDarkenDebounceTimeForSmallThr = (long) this.mData.darkenDebounceTimeForSmallThr;
        setDarkenThresholdNew(this.mAmbientLuxNewMin);
        setBrightenThresholdNew(this.mAmbientLuxNewMax);
    }

    private void setBrightenThresholdNew(float amLux) {
        this.mBrightenDeltaLuxMax = calculateBrightenThresholdDelta(amLux);
        this.mBrightenDeltaLuxMin = this.mBrightenDeltaLuxMax * this.mData.ratioForBrightnenSmallThr;
        this.mBrightenDeltaLuxMax *= 1.0f + ((this.mData.brightenDeltaLuxPara * (this.mStability - ((float) this.mData.stabilityConstant))) / 100.0f);
        if (((float) this.mBrightPointCnt) > -1.0E-6f && this.mAmbientLux > ((float) this.mData.outdoorLowerLuxThreshold) && this.mData.outdoorResponseBrightenRatio > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            this.mBrightenDeltaLuxMax *= this.mData.outdoorResponseBrightenRatio;
        }
    }

    private void setDarkenThresholdNew(float amLux) {
        this.mDarkenDeltaLuxMax = calculateDarkenThresholdDelta(amLux);
        if (this.mAmbientLux < 10.0f) {
            this.mDarkenDeltaLuxMin = this.mDarkenDeltaLuxMax;
        } else {
            this.mDarkenDeltaLuxMin = this.mDarkenDeltaLuxMax * this.mData.ratioForDarkenSmallThr;
        }
        this.mDarkenDeltaLuxForBackSensorCoverMode = this.mDarkenDeltaLuxMax * 0.5f;
        this.mDarkenDeltaLuxMax *= 1.0f + ((this.mData.darkenDeltaLuxPara * (this.mStability - ((float) this.mData.stabilityConstant))) / 100.0f);
        if (((float) this.mBrightPointCnt) > -1.0E-6f && this.mAmbientLux > ((float) this.mData.outdoorLowerLuxThreshold) && this.mData.outdoorResponseDarkenRatio > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            this.mDarkenDeltaLuxMax *= this.mData.outdoorResponseDarkenRatio;
        }
    }

    private float calculateBrightenThresholdDelta(float amLux) {
        float brightenThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        PointF temp1 = null;
        for (PointF temp : this.mData.brightenlinePoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (amLux < temp.x) {
                PointF temp2 = temp;
                if (temp2.x > temp1.x) {
                    return (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (amLux - temp1.x)) + temp1.y;
                }
                if (!DEBUG) {
                    return 1.0f;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Brighten_temp1.x <= temp2.x,x");
                stringBuilder.append(temp.x);
                stringBuilder.append(", y = ");
                stringBuilder.append(temp.y);
                Slog.i(str, stringBuilder.toString());
                return 1.0f;
            }
            temp1 = temp;
            brightenThreshold = temp1.y;
        }
        return brightenThreshold;
    }

    private float calculateDarkenThresholdDelta(float amLux) {
        float darkenThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        PointF temp1 = null;
        for (PointF temp : this.mData.darkenlinePoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (amLux < temp.x) {
                PointF temp2 = temp;
                if (temp2.x > temp1.x) {
                    float darkenThresholdTmp = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (amLux - temp1.x)) + temp1.y;
                    float f = 1.0f;
                    if (darkenThresholdTmp > 1.0f) {
                        f = darkenThresholdTmp;
                    }
                    return f;
                } else if (!DEBUG) {
                    return 1.0f;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Darken_temp1.x <= temp2.x,x");
                    stringBuilder.append(temp.x);
                    stringBuilder.append(", y = ");
                    stringBuilder.append(temp.y);
                    Slog.i(str, stringBuilder.toString());
                    return 1.0f;
                }
            }
            temp1 = temp;
            darkenThreshold = temp1.y;
        }
        return darkenThreshold;
    }

    private float prefilterAmbientLux(long now, int filterMethodNum) {
        if (filterMethodNum == 1) {
            return prefilterMeanFilter(now);
        }
        if (filterMethodNum == 2) {
            return prefilterWeightedMeanFilter(now);
        }
        return prefilterNoFilter(now);
    }

    private float prefilterNoFilter(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N != 0) {
            return this.mAmbientLightRingBuffer.getLux(N - 1);
        }
        Slog.e(TAG, "prefilterNoFilter: No ambient light readings available, return 0");
        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    }

    private float prefilterMeanFilter(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "prefilterMeanFilter: No ambient light readings available, return 0");
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (this.mData.preMeanFilterNum <= 0 || this.mData.preMeanFilterNoFilterNum < this.mData.preMeanFilterNum) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("prefilterMeanFilter: ErrorPara, return 0, MeanFilterNum=");
            stringBuilder.append(this.mData.preMeanFilterNum);
            stringBuilder.append(",MeanFilterNoFilterNum=");
            stringBuilder.append(this.mData.preMeanFilterNoFilterNum);
            Slog.e(str, stringBuilder.toString());
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (N <= this.mData.preMeanFilterNoFilterNum) {
            return this.mAmbientLightRingBuffer.getLux(N - 1);
        } else {
            float sum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            for (int i = N - 1; i >= N - this.mData.preMeanFilterNum; i--) {
                sum += this.mAmbientLightRingBuffer.getLux(i);
            }
            return (float) Math.round(sum / ((float) this.mData.preMeanFilterNum));
        }
    }

    private float prefilterWeightedMeanFilter(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            Slog.e(TAG, "prefilterWeightedMeanFilter: No ambient light readings available, return 0");
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (this.mData.preWeightedMeanFilterNum <= 0 || this.mData.preWeightedMeanFilterNoFilterNum < this.mData.preWeightedMeanFilterNum) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("prefilterWeightedMeanFilter: ErrorPara, return 0, WeightedMeanFilterNum=");
            stringBuilder.append(this.mData.preWeightedMeanFilterNum);
            stringBuilder.append(",WeightedMeanFilterNoFilterNum=");
            stringBuilder.append(this.mData.preWeightedMeanFilterNoFilterNum);
            Slog.e(str, stringBuilder.toString());
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else {
            float tempLux = this.mAmbientLightRingBuffer.getLux(N - 1);
            if (N <= this.mData.preWeightedMeanFilterNoFilterNum) {
                return tempLux;
            }
            int i;
            float maxLux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            float sum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            float totalWeight = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            for (i = N - 1; i >= N - this.mData.preWeightedMeanFilterMaxFuncLuxNum; i--) {
                tempLux = this.mAmbientLightRingBuffer.getLux(i);
                if (tempLux >= maxLux) {
                    maxLux = tempLux;
                }
            }
            for (i = N - 1; i >= N - this.mData.preWeightedMeanFilterNum; i--) {
                float weight;
                if (this.mAmbientLightRingBuffer.getLux(i) != GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || maxLux > this.mData.preWeightedMeanFilterLuxTh) {
                    weight = 1.0f * 1.0f;
                } else {
                    weight = this.mData.preWeightedMeanFilterAlpha * 1.0f;
                }
                totalWeight += weight;
                sum += this.mAmbientLightRingBuffer.getLux(i) * weight;
            }
            return (float) Math.round(sum / totalWeight);
        }
    }

    private float getOrigLastAmbientLux(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N != 0) {
            return this.mAmbientLightRingBuffer.getLux(N - 1);
        }
        Slog.e(TAG, "OrigAmbient: No ambient light readings available, return 0");
        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    }

    private float getFilterLastAmbientLux(long now) {
        int N = this.mAmbientLightRingBufferFilter.size();
        if (N != 0) {
            return this.mAmbientLightRingBufferFilter.getLux(N - 1);
        }
        Slog.e(TAG, "FilterLastAmbient: No ambient light readings available, return 0");
        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    }

    private float postfilterAmbientLux(long now, int filterMethodNum) {
        if (filterMethodNum == 1) {
            return postfilterMeanFilter(now);
        }
        if (filterMethodNum == 2) {
            return postfilterMaxMinAvgFilter(now);
        }
        return postfilterNoFilter(now);
    }

    private float postfilterNoFilter(long now) {
        int N = this.mAmbientLightRingBufferFilter.size();
        if (N != 0) {
            return this.mAmbientLightRingBufferFilter.getLux(N - 1);
        }
        Slog.e(TAG, "postfilterNoFilter: No ambient light readings available, return 0");
        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    }

    private float postfilterMeanFilter(long now) {
        int N = this.mAmbientLightRingBufferFilter.size();
        if (N == 0) {
            Slog.e(TAG, "prefilterMeanFilter: No ambient light readings available, return 0");
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (this.mData.postMeanFilterNum <= 0 || this.mData.postMeanFilterNoFilterNum < this.mData.postMeanFilterNum) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("postfilterMeanFilter: ErrorPara, return 0, MeanFilterNum=");
            stringBuilder.append(this.mData.postMeanFilterNum);
            stringBuilder.append(",MeanFilterNoFilterNum=");
            stringBuilder.append(this.mData.postMeanFilterNum);
            Slog.e(str, stringBuilder.toString());
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (N <= this.mData.postMeanFilterNoFilterNum) {
            return this.mAmbientLightRingBufferFilter.getLux(N - 1);
        } else {
            float sum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            for (int i = N - 1; i >= N - this.mData.postMeanFilterNum; i--) {
                sum += this.mAmbientLightRingBufferFilter.getLux(i);
            }
            return (float) Math.round(sum / ((float) this.mData.postMeanFilterNum));
        }
    }

    private float postfilterMaxMinAvgFilter(long now) {
        int N = this.mAmbientLightRingBufferFilter.size();
        if (N == 0) {
            Slog.e(TAG, "postfilterMaxMinAvgFilter: No ambient light readings available, return 0");
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (this.mData.postMaxMinAvgFilterNum <= 0 || this.mData.postMaxMinAvgFilterNoFilterNum < this.mData.postMaxMinAvgFilterNum) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("postfilterMaxMinAvgFilter: ErrorPara, return 0, PostMaxMinAvgFilterNoFilterNum=");
            stringBuilder.append(this.mData.postMaxMinAvgFilterNoFilterNum);
            stringBuilder.append(",PostMaxMinAvgFilterNum=");
            stringBuilder.append(this.mData.postMaxMinAvgFilterNum);
            Slog.e(str, stringBuilder.toString());
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        } else if (N <= this.mData.postMaxMinAvgFilterNoFilterNum) {
            return this.mAmbientLightRingBufferFilter.getLux(N - 1);
        } else {
            float sum = this.mAmbientLightRingBufferFilter.getLux(N - 1);
            float luxMin = this.mAmbientLightRingBufferFilter.getLux(N - 1);
            float luxMax = this.mAmbientLightRingBufferFilter.getLux(N - 1);
            for (int i = N - 2; i >= N - this.mData.postMaxMinAvgFilterNum; i--) {
                if (luxMin > this.mAmbientLightRingBufferFilter.getLux(i)) {
                    luxMin = this.mAmbientLightRingBufferFilter.getLux(i);
                }
                if (luxMax < this.mAmbientLightRingBufferFilter.getLux(i)) {
                    luxMax = this.mAmbientLightRingBufferFilter.getLux(i);
                }
                sum += this.mAmbientLightRingBufferFilter.getLux(i);
            }
            return ((sum - luxMin) - luxMax) / 3.0f;
        }
    }

    private long nextAmbientLightBrighteningTransition(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean BrightenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) - this.mAmbientLuxNewMax > this.mBrightenDeltaLuxMax) {
                BrightenChange = true;
            } else {
                BrightenChange = false;
            }
            if (!BrightenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return getNextAmbientLightBrighteningTime(earliestValidTime);
    }

    private long nextAmbientLightBrighteningTransitionForSmallThr(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean BrightenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) - this.mAmbientLux > this.mBrightenDeltaLuxMin) {
                BrightenChange = true;
            } else {
                BrightenChange = false;
            }
            if (!BrightenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return this.mNormBrighenDebounceTimeForSmallThr + earliestValidTime;
    }

    private long nextAmbientLightDarkeningTransition(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean DarkenChange;
            if (this.mAmbientLuxNewMin - this.mAmbientLightRingBufferFilter.getLux(i) >= this.mDarkenDeltaLuxMax) {
                DarkenChange = true;
            } else {
                DarkenChange = false;
            }
            if (!DarkenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return getNextAmbientLightDarkeningTime(earliestValidTime);
    }

    private long nextAmbientLightDarkeningTransitionExtended(long time, float lux, float deltaLux, long debounceTime) {
        long earliestValidTime = time;
        int i = this.mAmbientLightRingBufferFilter.size() - 1;
        while (i >= 0 && lux - this.mAmbientLightRingBufferFilter.getLux(i) >= deltaLux) {
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
            i--;
        }
        return earliestValidTime + debounceTime;
    }

    private boolean decideToDarkenForBackSensorCoverMode(float ambientLux) {
        boolean needToDarken;
        if (this.mAmbientLux - ambientLux >= this.mDarkenDeltaLuxForBackSensorCoverMode) {
            needToDarken = true;
        } else {
            needToDarken = false;
        }
        return (!needToDarken || this.mAutoBrightnessIntervened || this.mProximityPositiveStatus) ? false : true;
    }

    private boolean needToDarkenForBackSensorCoverMode(long time, float lux) {
        if (this.mData.backSensorCoverModeEnable && this.mIsCoverModeFastResponseFlag && decideToDarkenForBackSensorCoverMode(lux)) {
            if (nextAmbientLightDarkeningTransitionExtended(time, this.mAmbientLux, this.mDarkenDeltaLuxForBackSensorCoverMode, 500) <= time) {
                if (DEBUG) {
                    Slog.d(TAG, "BackSensorCoverMode needToDarkenForBackSensorCoverMode");
                }
                return true;
            }
        }
        return false;
    }

    private boolean decideToBrighten(float ambientLux) {
        boolean needToBrighten;
        if (ambientLux - this.mAmbientLuxNewMax < this.mBrightenDeltaLuxMax || this.mStability >= this.mStabilityBrightenConstant) {
            needToBrighten = false;
        } else {
            needToBrighten = true;
        }
        return needToBrighten && !this.mAutoBrightnessIntervened;
    }

    private boolean decideToBrightenForSmallThr(float ambientLux) {
        boolean needToBrighten;
        if (ambientLux - this.mAmbientLux < this.mBrightenDeltaLuxMin || this.mStabilityForSmallThr >= this.mStabilityBrightenConstantForSmallThr) {
            needToBrighten = false;
        } else {
            needToBrighten = true;
        }
        return (!needToBrighten || this.mAutoBrightnessIntervened || this.mProximityPositiveStatus) ? false : true;
    }

    private boolean decideToDarken(float ambientLux) {
        boolean needToDarken;
        if (this.mAmbientLuxNewMin - ambientLux < this.mDarkenDeltaLuxMax || this.mStability > this.mStabilityDarkenConstant) {
            needToDarken = false;
        } else {
            needToDarken = true;
        }
        return (!needToDarken || this.mAutoBrightnessIntervened || this.mProximityPositiveStatus) ? false : true;
    }

    private boolean decideToDarkenForSmallThr(float ambientLux) {
        boolean needToDarken;
        if (this.mAmbientLux - ambientLux < this.mDarkenDeltaLuxMin || this.mStabilityForSmallThr > this.mStabilityDarkenConstantForSmallThr) {
            needToDarken = false;
        } else {
            needToDarken = true;
        }
        return (!needToDarken || this.mAutoBrightnessIntervened || this.mProximityPositiveStatus) ? false : true;
    }

    public boolean getProximityPositiveEnable() {
        return this.mData.allowLabcUseProximity && this.mProximityPositiveStatus;
    }

    public float getOffsetValidAmbientLux() {
        if (this.mData.offsetValidAmbientLuxEnable) {
            return this.mOffsetValidAmbientLux;
        }
        return this.mAmbientLux;
    }

    public float getValidAmbientLux(float lux) {
        float luxtmp = lux;
        if (luxtmp < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            luxtmp = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (luxtmp > 40000.0f) {
            return 40000.0f;
        }
        return luxtmp;
    }

    public void setCurrentAmbientLux(float lux) {
        if (((int) this.mAmbientLux) != ((int) lux)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setOffsetLux mAmbientLux=");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(",lux=");
            stringBuilder.append(lux);
            Slog.i(str, stringBuilder.toString());
            this.mAmbientLux = getValidAmbientLux(lux);
        }
    }

    private void setOffsetValidAmbientLux(float lux) {
        this.mOffsetValidAmbientLux = (float) Math.round(lux);
        this.mOffsetValidAmbientBrightenDeltaLux = calculateBrightenThresholdDelta(this.mOffsetValidAmbientLux);
        this.mOffsetValidAmbientDarkenDeltaLux = calculateDarkenThresholdDelta(this.mOffsetValidAmbientLux);
    }

    private long nextAmbientLightBrighteningTransitionForOffset(long time) {
        long earliestValidTime = time;
        for (int i = this.mAmbientLightRingBufferFilter.size() - 1; i >= 0; i--) {
            boolean BrightenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) - this.mOffsetValidAmbientLux > this.mOffsetValidAmbientBrightenDeltaLux) {
                BrightenChange = true;
            } else {
                BrightenChange = false;
            }
            if (!BrightenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return ((long) this.mData.offsetBrightenDebounceTime) + earliestValidTime;
    }

    private boolean decideToBrightenForOffset(float ambientLux) {
        boolean needToBrighten;
        if (ambientLux - this.mOffsetValidAmbientLux >= this.mOffsetValidAmbientBrightenDeltaLux) {
            needToBrighten = true;
        } else {
            needToBrighten = false;
        }
        return needToBrighten && !this.mAutoBrightnessIntervened;
    }

    private boolean decideToDarkenForOffset(float ambientLux) {
        boolean needToDarken;
        if (this.mOffsetValidAmbientLux - ambientLux >= this.mOffsetValidAmbientDarkenDeltaLux) {
            needToDarken = true;
        } else {
            needToDarken = false;
        }
        return needToDarken && !this.mAutoBrightnessIntervened;
    }

    private float calculateStability(HwRingBuffer buffer) {
        HwRingBuffer hwRingBuffer = buffer;
        int N = buffer.size();
        if (N <= 1) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        float lux1;
        float luxT1;
        int indexMax;
        int index1;
        int index2;
        float luxk3;
        float tmp;
        float currentLux = hwRingBuffer.getLux(N - 1);
        calculateAvg(buffer);
        int indexMax2 = 0;
        float luxT2Max = currentLux;
        float luxT1Max = currentLux;
        int indexMin = 0;
        float luxT2Min = currentLux;
        float luxT1Min = currentLux;
        int index = 0;
        int T2 = 0;
        int T1 = 0;
        float luxT2 = currentLux;
        float luxT12 = currentLux;
        int j = 0;
        while (j < N - 1) {
            lux1 = hwRingBuffer.getLux((N - 1) - j);
            float currentLux2 = currentLux;
            currentLux = hwRingBuffer.getLux(((N - 1) - j) - 1);
            luxT1 = luxT12;
            if (((this.mLuxBufferAvg > lux1 || this.mLuxBufferAvg < currentLux) && (this.mLuxBufferAvg < lux1 || this.mLuxBufferAvg > currentLux)) || (Math.abs(this.mLuxBufferAvg - lux1) < 1.0E-7f && Math.abs(this.mLuxBufferAvg - currentLux) < 1.0E-7f)) {
                luxT12 = luxT1;
            } else {
                luxT12 = lux1;
                luxT2 = currentLux;
                index = j;
                T2 = ((N - 1) - j) - 1;
                T1 = (N - 1) - j;
            }
            float luxT13 = luxT12;
            if (((this.mLuxBufferAvgMin <= lux1 && this.mLuxBufferAvgMin >= currentLux) || (this.mLuxBufferAvgMin >= lux1 && this.mLuxBufferAvgMin <= currentLux)) && (Math.abs(this.mLuxBufferAvgMin - lux1) >= 1.0E-7f || Math.abs(this.mLuxBufferAvgMin - currentLux) >= 1.0E-7f)) {
                indexMin = j;
                luxT2Min = currentLux;
                luxT1Min = lux1;
            }
            if (((this.mLuxBufferAvgMax > lux1 || this.mLuxBufferAvgMax < currentLux) && (this.mLuxBufferAvgMax < lux1 || this.mLuxBufferAvgMax > currentLux)) || (Math.abs(this.mLuxBufferAvgMax - lux1) < 1.0E-7f && Math.abs(this.mLuxBufferAvgMax - currentLux) < 1.0E-7f)) {
                luxT12 = luxT2Max;
                indexMax = indexMax2;
            } else {
                indexMax = j;
                luxT1Max = lux1;
                luxT12 = currentLux;
            }
            if (index != 0 && ((indexMin != 0 || indexMax != 0) && ((index <= indexMin && index >= indexMax) || (index >= indexMin && index <= indexMax)))) {
                luxT1 = luxT13;
                break;
            }
            j++;
            indexMax2 = indexMax;
            currentLux = currentLux2;
            luxT2Max = luxT12;
            luxT12 = luxT13;
        }
        luxT1 = luxT12;
        luxT12 = luxT2Max;
        indexMax = indexMax2;
        if (indexMax <= indexMin) {
            index1 = indexMax;
            index2 = indexMin;
        } else {
            index1 = indexMin;
            index2 = indexMax;
        }
        j = (N - 1) - index1;
        while (true) {
            int index12 = index1;
            float f;
            if (j > N - 1) {
                break;
            } else if (j == N - 1) {
                f = luxT12;
                break;
            } else {
                lux1 = hwRingBuffer.getLux(j);
                f = luxT12;
                luxT12 = hwRingBuffer.getLux(j + 1);
                if (indexMax > indexMin) {
                    if (lux1 <= luxT12) {
                        break;
                    }
                    T1 = j + 1;
                } else if (lux1 >= luxT12) {
                    break;
                } else {
                    T1 = j + 1;
                }
                j++;
                index1 = index12;
                luxT12 = f;
            }
        }
        index1 = (N - 1) - index2;
        while (index1 >= 0 && index1 != 0) {
            luxk3 = hwRingBuffer.getLux(index1);
            luxT12 = hwRingBuffer.getLux(index1 - 1);
            if (indexMax > indexMin) {
                if (luxk3 >= luxT12) {
                    break;
                }
                T2 = index1 - 1;
            } else if (luxk3 <= luxT12) {
                break;
            } else {
                T2 = index1 - 1;
            }
            index1--;
        }
        index1 = (N - 1) - T1;
        j = T2;
        luxT12 = calculateStabilityFactor(hwRingBuffer, T1, N - 1);
        currentLux = calcluateAvg(hwRingBuffer, T1, N - 1);
        float s2 = calculateStabilityFactor(hwRingBuffer, 0, T2);
        float avg2 = calcluateAvg(hwRingBuffer, 0, T2);
        luxT2 = Math.abs(currentLux - avg2);
        float k = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (T1 != T2) {
            k = Math.abs((hwRingBuffer.getLux(T1) - hwRingBuffer.getLux(T2)) / ((float) (T1 - T2)));
        }
        if (k < 10.0f / (k + 5.0f)) {
            tmp = k;
        } else {
            tmp = 10.0f / (k + 5.0f);
        }
        if (tmp > 20.0f / (luxT2 + 10.0f)) {
            tmp = 20.0f / (luxT2 + 10.0f);
        }
        if (index1 > this.mData.stabilityTime1) {
            avg2 = luxT12;
            float f2 = luxT2;
            int i = T1;
        } else {
            avg2 = (float) Math.exp((double) (index1 - this.mData.stabilityTime1));
            luxT2 = (float) (this.mData.stabilityTime1 - index1);
            avg2 = ((avg2 * luxT12) + (luxT2 * tmp)) / (avg2 + luxT2);
        }
        if (j > this.mData.stabilityTime2) {
            luxT2 = s2;
            float f3 = luxT12;
        } else {
            float Stability2 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            luxT12 = (float) Math.exp((double) (j - this.mData.stabilityTime2));
            luxT2 = (float) (this.mData.stabilityTime2 - j);
            luxT2 = ((luxT12 * s2) + (luxT2 * tmp)) / (luxT12 + luxT2);
        }
        if (index1 > this.mData.stabilityTime1) {
            luxT12 = avg2;
            float f4 = currentLux;
            int i2 = j;
        } else {
            currentLux = (float) Math.exp((double) (index1 - this.mData.stabilityTime1));
            luxk3 = (float) (this.mData.stabilityTime1 - index1);
            luxT12 = ((currentLux * avg2) + (luxk3 * luxT2)) / (currentLux + luxk3);
        }
        return luxT12;
    }

    private void calculateAvg(HwRingBuffer buffer) {
        int N = buffer.size();
        if (N != 0) {
            float currentLux = buffer.getLux(N - 1);
            float luxBufferSum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            float luxBufferMin = currentLux;
            float luxBufferMax = currentLux;
            for (int i = N - 1; i >= 0; i--) {
                float lux = buffer.getLux(i);
                if (lux > luxBufferMax) {
                    luxBufferMax = lux;
                }
                if (lux < luxBufferMin) {
                    luxBufferMin = lux;
                }
                luxBufferSum += lux;
            }
            this.mLuxBufferAvg = luxBufferSum / ((float) N);
            this.mLuxBufferAvgMax = (this.mLuxBufferAvg + luxBufferMax) / 2.0f;
            this.mLuxBufferAvgMin = (this.mLuxBufferAvg + luxBufferMin) / 2.0f;
        }
    }

    private float calculateStabilityForSmallThr(HwRingBuffer buffer) {
        int N = buffer.size();
        if (N <= 1) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        if (N <= 15) {
            return calculateStabilityFactor(buffer, 0, N - 1);
        }
        return calculateStabilityFactor(buffer, 0, 14);
    }

    private float calcluateAvg(HwRingBuffer buffer, int start, int end) {
        float sum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        for (int i = start; i <= end; i++) {
            sum += buffer.getLux(i);
        }
        if (end < start) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        return sum / ((float) ((end - start) + 1));
    }

    private float calculateStabilityFactor(HwRingBuffer buffer, int start, int end) {
        int size = (end - start) + 1;
        float sum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float sigma = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        if (size <= 1) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        for (int i = start; i <= end; i++) {
            sum += buffer.getLux(i);
        }
        float avg = sum / ((float) size);
        for (int i2 = start; i2 <= end; i2++) {
            sigma += (buffer.getLux(i2) - avg) * (buffer.getLux(i2) - avg);
        }
        float ss = sigma / ((float) (size - 1));
        if (avg == GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
        return ss / avg;
    }

    public boolean reportValueWhenSensorOnChange() {
        return this.mData.reportValueWhenSensorOnChange;
    }

    public int getProximityState() {
        return this.mProximity;
    }

    public boolean needToUseProximity() {
        return this.mData.allowLabcUseProximity;
    }

    public boolean needToSendProximityDebounceMsg() {
        return this.mNeedToSendProximityDebounceMsg;
    }

    public long getPendingProximityDebounceTime() {
        return this.mPendingProximityDebounceTime;
    }

    public void setCoverModeStatus(boolean isclosed) {
        if (!isclosed && this.mIsclosed) {
            this.mCoverState = true;
        }
        this.mIsclosed = isclosed;
    }

    public void setCoverModeFastResponseFlag(boolean isFast) {
        this.mIsCoverModeFastResponseFlag = isFast;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LabcCoverMode mIsCoverModeFastResponseFlag=");
            stringBuilder.append(this.mIsCoverModeFastResponseFlag);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public void setBackSensorCoverModeBrightness(int brightness) {
        if (brightness > 0) {
            this.mBackSensorCoverModeBrightness = brightness;
        }
    }

    public boolean getLastCloseScreenEnable() {
        return this.mData.lastCloseScreenEnable ^ 1;
    }

    private void setProximityState(boolean proximityPositive) {
        this.mProximityPositiveStatus = proximityPositive;
        if (!this.mProximityPositiveStatus) {
            this.mNeedToUpdateBrightness = true;
            if (DEBUG) {
                Slog.i(TAG, "Proximity sets brightness");
            }
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1;
        }
    }

    public void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mPendingProximity == 0 && !positive) {
            return;
        }
        if (this.mPendingProximity != 1 || !positive) {
            if (positive) {
                this.mPendingProximity = 1;
                this.mPendingProximityDebounceTime = ((long) this.mProximityPositiveDebounceTime) + time;
            } else {
                this.mPendingProximity = 0;
                this.mPendingProximityDebounceTime = ((long) this.mProximityNegativeDebounceTime) + time;
            }
            debounceProximitySensor();
        }
    }

    public void debounceProximitySensor() {
        this.mNeedToSendProximityDebounceMsg = false;
        if (this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            if (this.mPendingProximityDebounceTime <= SystemClock.uptimeMillis()) {
                this.mProximity = this.mPendingProximity;
                if (this.mProximity == 1) {
                    setProximityState(true);
                } else if (this.mProximity == 0) {
                    setProximityState(false);
                }
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("debounceProximitySensor:mProximity=");
                    stringBuilder.append(this.mProximity);
                    Slog.d(str, stringBuilder.toString());
                }
                clearPendingProximityDebounceTime();
                return;
            }
            this.mNeedToSendProximityDebounceMsg = true;
        }
    }

    public int getpowerOnFastResponseLuxNum() {
        return this.mData.powerOnFastResponseLuxNum;
    }

    public boolean getCameraModeBrightnessLineEnable() {
        return this.mData.cameraModeEnable;
    }

    public boolean getReadingModeBrightnessLineEnable() {
        return this.mData.readingModeEnable;
    }

    public void setKeyguardLockedStatus(boolean isLocked) {
        this.mKeyguardIsLocked = isLocked;
    }

    public boolean getOutdoorAnimationFlag() {
        return ((float) this.mData.outdoorLowerLuxThreshold) < this.mAmbientLux;
    }

    public boolean getDayModeEnable() {
        return this.mDayModeEnable;
    }

    public boolean getOffsetResetEnable() {
        return this.mOffsetResetEnable;
    }

    public void setAutoModeEnableFirstLux(float lux) {
        this.mAutoModeEnableFirstLux = getValidAmbientLux(lux);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setAutoModeEnableFirstLux=");
        stringBuilder.append(this.mAutoModeEnableFirstLux);
        Slog.i(str, stringBuilder.toString());
    }

    public void setDayModeEnable() {
        String str;
        StringBuilder stringBuilder;
        Calendar c = Calendar.getInstance();
        int openDay = c.get(6);
        int openHour = c.get(11);
        int openMinute = c.get(12);
        int openTime = (((openDay * 24) * 60) + (openHour * 60)) + openMinute;
        if (this.mData.dayModeAlgoEnable && (this.mFirstSetBrightness || openTime - this.mLastCloseTime >= this.mData.dayModeSwitchTime)) {
            this.mDayModeEnable = false;
            if (this.mData.dayModeBeginTime < this.mData.dayModeEndTime) {
                if (openHour >= this.mData.dayModeBeginTime && openHour < this.mData.dayModeEndTime) {
                    this.mDayModeEnable = true;
                }
            } else if (openHour >= this.mData.dayModeBeginTime || openHour < this.mData.dayModeEndTime) {
                this.mDayModeEnable = true;
            }
        }
        if (this.mData.offsetResetEnable) {
            this.mOffsetResetEnable = false;
            if (openTime - this.mLastCloseTime >= this.mData.offsetResetSwitchTime) {
                this.mOffsetResetEnable = true;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("offsetResetEnable detime=");
                stringBuilder.append(openTime - this.mLastCloseTime);
                Slog.i(str, stringBuilder.toString());
            } else {
                float luxBright = this.mAutoModeEnableFirstLux + calculateBrightenThresholdDelta(this.mAutoModeEnableFirstLux);
                float luxDark = this.mAutoModeEnableFirstLux - calculateDarkenThresholdDelta(this.mAutoModeEnableFirstLux);
                if (Math.abs(this.mAutoModeEnableFirstLux - this.mLastCloseScreenLux) > ((float) this.mData.offsetResetShortLuxDelta) && ((luxBright > this.mLastCloseScreenLux || luxDark < this.mLastCloseScreenLux) && openTime - this.mLastCloseTime >= this.mData.offsetResetShortSwitchTime)) {
                    this.mOffsetResetEnable = true;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("offsetResetEnableShort detime=");
                    stringBuilder2.append(openTime - this.mLastCloseTime);
                    stringBuilder2.append(",mFirstLux=");
                    stringBuilder2.append(this.mAutoModeEnableFirstLux);
                    stringBuilder2.append(",mCloseLux=");
                    stringBuilder2.append(this.mLastCloseScreenLux);
                    stringBuilder2.append(",luxBright=");
                    stringBuilder2.append(luxBright);
                    stringBuilder2.append(",luxDark=");
                    stringBuilder2.append(luxDark);
                    Slog.i(str2, stringBuilder2.toString());
                }
            }
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("DayMode:openDay=");
            stringBuilder.append(openDay);
            stringBuilder.append(",openHour=");
            stringBuilder.append(openHour);
            stringBuilder.append(",openMinute=");
            stringBuilder.append(openMinute);
            stringBuilder.append(",openTime=");
            stringBuilder.append(openTime);
            stringBuilder.append(", mLastCloseTime");
            stringBuilder.append(this.mLastCloseTime);
            stringBuilder.append(", mFirstSetBrightness");
            stringBuilder.append(this.mFirstSetBrightness);
            stringBuilder.append(",mDayModeEnable=");
            stringBuilder.append(this.mDayModeEnable);
            stringBuilder.append(",mOffsetResetEnable=");
            stringBuilder.append(this.mOffsetResetEnable);
            stringBuilder.append(",offsetResetSwitchTime=");
            stringBuilder.append(this.mData.offsetResetSwitchTime);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        if (this.mData.coverModeDayEnable) {
            return getCoverModeBrightnessFromLastScreenLux(this.mAmbientLux);
        }
        return this.mData.coverModeDayBrightness;
    }

    public void setCoverModeDayEnable(boolean coverModeDayEnable) {
        this.mCoverModeDayEnable = coverModeDayEnable;
    }

    public boolean getCoverModeDayEnable() {
        return this.mCoverModeDayEnable;
    }

    private int getCoverModeBrightnessFromLastScreenLux(float amLux) {
        if (this.mData.coverModeBrighnessLinePoints == null || amLux < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LabcCoverMode error input,set MIN_BRIGHTNESS,amLux=");
            stringBuilder.append(amLux);
            Slog.e(str, stringBuilder.toString());
            return 4;
        }
        float coverModebrightness = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        PointF temp1 = null;
        for (PointF temp : this.mData.coverModeBrighnessLinePoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (amLux < temp.x) {
                PointF temp2 = temp;
                if (temp2.x <= temp1.x) {
                    coverModebrightness = 4.0f;
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("LabcCoverMode,set MIN_BRIGHTNESS,Brighten_temp1.x <= temp2.x,x");
                        stringBuilder2.append(temp.x);
                        stringBuilder2.append(", y = ");
                        stringBuilder2.append(temp.y);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                } else {
                    coverModebrightness = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (amLux - temp1.x)) + temp1.y;
                }
                return (int) coverModebrightness;
            }
            temp1 = temp;
            coverModebrightness = temp1.y;
        }
        return (int) coverModebrightness;
    }

    private int getLuxFromDefaultBrightnessLevel(float brightnessLevel) {
        if (this.mData.defaultBrighnessLinePoints == null || brightnessLevel < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LabcCoverMode,error input,set MIN_Lux,brightnessLevel=");
            stringBuilder.append(brightnessLevel);
            Slog.e(str, stringBuilder.toString());
            return 0;
        } else if (brightnessLevel == 255.0f) {
            Slog.i(TAG, "LabcCoverMode,brightnessLevel=MAX_Brightness,getMaxLux=40000");
            return AMBIENT_MAX_LUX;
        } else {
            float lux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            PointF temp1 = null;
            for (PointF temp : this.mData.defaultBrighnessLinePoints) {
                if (temp1 == null) {
                    temp1 = temp;
                }
                if (brightnessLevel < temp.y) {
                    PointF temp2 = temp;
                    if (temp2.y <= temp1.y) {
                        lux = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        if (DEBUG) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("LabcCoverMode,set MIN_Lux,Brighten_temp1.x <= temp2.x,x");
                            stringBuilder2.append(temp.x);
                            stringBuilder2.append(", y = ");
                            stringBuilder2.append(temp.y);
                            Slog.d(str2, stringBuilder2.toString());
                        }
                    } else {
                        lux = (((temp2.x - temp1.x) / (temp2.y - temp1.y)) * (brightnessLevel - temp1.y)) + temp1.x;
                    }
                    return (int) lux;
                }
                temp1 = temp;
                lux = temp1.x;
            }
            return (int) lux;
        }
    }

    public void setGameModeEnable(boolean enable) {
        this.mGameModeEnable = enable;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GameBrightMode set mGameModeEnable=");
            stringBuilder.append(this.mGameModeEnable);
            Slog.d(str, stringBuilder.toString());
        }
    }
}

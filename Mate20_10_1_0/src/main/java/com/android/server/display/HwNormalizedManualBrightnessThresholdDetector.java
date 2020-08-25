package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader;

public class HwNormalizedManualBrightnessThresholdDetector {
    private static final int AMBIENT_LIGHT_HORIZON = 10000;
    private static final int AMBIENT_MAX_LUX = 40000;
    private static boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int INDOOR_UI = 1;
    private static final int OUTDOOR_UI = 2;
    private static final String TAG = "HwNormalizedManualBrightnessThresholdDetector";
    protected boolean mAmChangeFlagForHBM = false;
    protected HwRingBuffer mAmbientLightRingBuffer;
    protected HwRingBuffer mAmbientLightRingBufferFilter;
    protected float mAmbientLux;
    private float mAmbientLuxForFrontCamera;
    private float mBrightenDeltaLuxMax = 0.0f;
    private boolean mCurrentLuxUpForFrontCameraEnable;
    private float mDarkenDeltaLuxMax = 0.0f;
    private final HwBrightnessXmlLoader.Data mData;
    private float mFilterLux;
    private HwAmbientLightTransition mHwAmbientLightTransition;
    private int mLastInOutState = 1;
    private final Object mLock = new Object();

    public HwNormalizedManualBrightnessThresholdDetector(HwBrightnessXmlLoader.Data data) {
        this.mData = data;
        this.mAmbientLightRingBuffer = new HwRingBuffer(50);
        this.mAmbientLightRingBufferFilter = new HwRingBuffer(50);
        this.mHwAmbientLightTransition = new HwAmbientLightTransition();
    }

    public void clearAmbientLightRingBuffer() {
        synchronized (this.mLock) {
            this.mAmbientLightRingBuffer.clear();
            this.mAmbientLightRingBufferFilter.clear();
            Slog.i(TAG, "clearAmbientLightRingBuffer");
        }
    }

    public void handleLightSensorEvent(long time, float lux) {
        synchronized (this.mLock) {
            if (lux > 40000.0f) {
                lux = 40000.0f;
            }
            applyLightSensorMeasurement(time, lux);
            updateAmbientLux(time);
        }
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mAmbientLightRingBuffer.prune(time - 10000);
        this.mAmbientLightRingBuffer.push(time, lux);
    }

    private float calculateAmbientLuxForNewPolicy(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            if (!DEBUG) {
                return -1.0f;
            }
            Slog.v(TAG, "calculateAmbientLux: No ambient light readings available");
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
            return ((sum - luxMin) - luxMax) / 3.0f;
        }
    }

    private long nextAmbientLightBrighteningTransitionForNewPolicy(long time) {
        boolean BrightenChange;
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mData.manualBrighenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            if (this.mAmbientLightRingBufferFilter.getLux(i) - this.mAmbientLux > this.mBrightenDeltaLuxMax) {
                BrightenChange = true;
            } else {
                BrightenChange = false;
            }
            if (!BrightenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return earliestValidTime + debounceTime;
    }

    private long nextAmbientLightDarkeningTransitionForNewPolicy(long time) {
        boolean DarkenChange;
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mData.manualDarkenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            if (this.mAmbientLux - this.mAmbientLightRingBufferFilter.getLux(i) >= this.mDarkenDeltaLuxMax) {
                DarkenChange = true;
            } else {
                DarkenChange = false;
            }
            if (!DarkenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
        }
        return earliestValidTime + debounceTime;
    }

    private void setBrightenThresholdNew() {
        PointF temp1 = null;
        for (PointF temp : this.mData.manualBrightenlinePoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (this.mAmbientLux >= temp.x) {
                temp1 = temp;
                this.mBrightenDeltaLuxMax = temp1.y;
            } else if (temp.x <= temp1.x) {
                this.mBrightenDeltaLuxMax = 1.0f;
                if (DEBUG) {
                    Slog.i(TAG, "Brighten_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                    return;
                }
                return;
            } else {
                this.mBrightenDeltaLuxMax = (((temp.y - temp1.y) / (temp.x - temp1.x)) * (this.mAmbientLux - temp1.x)) + temp1.y;
                return;
            }
        }
    }

    private void setDarkenThresholdNew() {
        PointF temp1 = null;
        for (PointF temp : this.mData.manualDarkenlinePoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (this.mAmbientLux < temp.x) {
                float f = 1.0f;
                if (temp.x <= temp1.x) {
                    this.mDarkenDeltaLuxMax = 1.0f;
                    if (DEBUG) {
                        Slog.i(TAG, "Darken_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                        return;
                    }
                    return;
                }
                float DarkenDeltaLuxMaxtmp = (((temp.y - temp1.y) / (temp.x - temp1.x)) * (this.mAmbientLux - temp1.x)) + temp1.y;
                if (DarkenDeltaLuxMaxtmp > 1.0f) {
                    f = DarkenDeltaLuxMaxtmp;
                }
                this.mDarkenDeltaLuxMax = f;
                return;
            }
            temp1 = temp;
            this.mDarkenDeltaLuxMax = temp1.y;
        }
    }

    private void updateAmbientLux(long time) {
        this.mFilterLux = calculateAmbientLuxForNewPolicy(time);
        if (this.mAmbientLightRingBuffer.size() == 1) {
            Slog.i(TAG, "fist sensor lux and filteredlux=" + this.mFilterLux + ",time=" + time);
            if (this.mData.frontCameraMaxBrightnessEnable) {
                updateAmbientLuxForFrontCamera(this.mFilterLux);
            }
        }
        this.mAmbientLightRingBufferFilter.push(time, this.mFilterLux);
        this.mAmbientLightRingBufferFilter.prune(time - 10000);
        long nextBrightenTransition = nextAmbientLightBrighteningTransitionForNewPolicy(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransitionForNewPolicy(time);
        this.mAmChangeFlagForHBM = false;
        updateAmbientLuxParametersForFrontCamera(time, this.mFilterLux);
        if (nextBrightenTransition <= time || nextDarkenTransition <= time) {
            if (DEBUG) {
                Slog.i(TAG, "filteredlux=" + this.mFilterLux + ",time=" + time + ",nextBTime=" + nextBrightenTransition + ",nextDTime=" + nextDarkenTransition);
            }
            updateParaForHBM(this.mFilterLux);
        }
    }

    private void updateParaForHBM(float lux) {
        if (lux >= ((float) this.mData.outDoorThreshold)) {
            this.mAmbientLux = lux;
            this.mLastInOutState = 2;
            this.mAmChangeFlagForHBM = true;
        }
        if (lux < ((float) this.mData.inDoorThreshold)) {
            this.mAmbientLux = lux;
            this.mLastInOutState = 1;
            this.mAmChangeFlagForHBM = true;
        }
        if (lux < ((float) this.mData.outDoorThreshold) && lux >= ((float) this.mData.inDoorThreshold)) {
            this.mAmbientLux = lux;
            if (this.mAmbientLightRingBufferFilter.size() == 1) {
                this.mLastInOutState = 1;
            }
            this.mAmChangeFlagForHBM = true;
        }
        setBrightenThresholdNew();
        setDarkenThresholdNew();
        if (DEBUG) {
            Slog.i(TAG, "update_lux =" + this.mAmbientLux + ",IN_OUT_DoorFlag =" + this.mLastInOutState + ",mBrightenDeltaLuxMax=" + this.mBrightenDeltaLuxMax + ",mDarkenDeltaLuxMax=" + this.mDarkenDeltaLuxMax);
        }
    }

    public float getAmbientLuxForHBM() {
        return this.mAmbientLux;
    }

    public boolean getLuxChangedFlagForHBM() {
        return this.mAmChangeFlagForHBM;
    }

    public void setLuxChangedFlagForHBM() {
        this.mAmChangeFlagForHBM = false;
    }

    public int getIndoorOutdoorFlagForHBM() {
        return this.mLastInOutState;
    }

    public float getFilterLuxFromManualMode() {
        return this.mFilterLux;
    }

    public int getCurrentFilteredAmbientLux() {
        return (int) this.mFilterLux;
    }

    private void updateAmbientLuxForFrontCamera(float lux) {
        this.mAmbientLuxForFrontCamera = lux;
        this.mCurrentLuxUpForFrontCameraEnable = this.mAmbientLuxForFrontCamera >= this.mData.frontCameraLuxThreshold;
        if (DEBUG) {
            Slog.i(TAG, "updateAmbientLuxForFrontCamera,lux=" + lux + ",mCurrentLuxUpEnable=" + this.mCurrentLuxUpForFrontCameraEnable);
        }
    }

    private void updateAmbientLuxParametersForFrontCamera(long time, float ambientLux) {
        HwAmbientLightTransition hwAmbientLightTransition;
        if (this.mData.frontCameraMaxBrightnessEnable && (hwAmbientLightTransition = this.mHwAmbientLightTransition) != null) {
            long nextBrightenTransitionForFrontCamera = hwAmbientLightTransition.getNextAmbientLightTransitionTime(this.mAmbientLightRingBufferFilter, time, true);
            long nextDarkenTransitionForFrontCamera = this.mHwAmbientLightTransition.getNextAmbientLightTransitionTime(this.mAmbientLightRingBufferFilter, time, false);
            if ((!this.mCurrentLuxUpForFrontCameraEnable && ambientLux >= this.mData.frontCameraBrightenLuxThreshold && nextBrightenTransitionForFrontCamera <= time) || (this.mCurrentLuxUpForFrontCameraEnable && ambientLux < this.mData.frontCameraDarkenLuxThreshold && nextDarkenTransitionForFrontCamera <= time)) {
                updateAmbientLuxForFrontCamera(ambientLux);
            }
        }
    }

    public float getAmbientLuxForFrontCamera() {
        return this.mAmbientLuxForFrontCamera;
    }
}

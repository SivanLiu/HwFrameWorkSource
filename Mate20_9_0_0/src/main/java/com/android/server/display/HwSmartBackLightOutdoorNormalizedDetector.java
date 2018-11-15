package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwSmartBackLightXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.util.List;

public class HwSmartBackLightOutdoorNormalizedDetector {
    protected static final int AMBIENT_LIGHT_HORIZON = 10000;
    private static final int AMBIENT_MAX_LUX = 40000;
    private static final int APICAL_INDOOR_UI = 1;
    private static final int APICAL_OUTDOOR_UI = 2;
    private static boolean DEBUG;
    private static String TAG = "HwSmartBackLightOutdoorNormalizedDetector";
    protected boolean mAmChangeFlagSBL = false;
    protected AmbientLightRingBufferForSbl mAmbientLightRingBuffer;
    protected AmbientLightRingBufferForSbl mAmbientLightRingBufferFilter;
    protected float mAmbientLux;
    protected int mBrighenDebounceTime;
    private float mBrightenDeltaLuxMax = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    protected List<PointF> mBrightenLinePointsList;
    protected List<PointF> mDarkLinePointsList;
    protected int mDarkenDebounceTime;
    private float mDarkenDeltaLuxMax = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    protected int mInDoorThreshold;
    protected boolean mInoutFlag = false;
    private int mLastApicalFlag = 1;
    protected int mOutDoorThreshold;

    protected final class AmbientLightRingBufferForSbl {
        private static final float BUFFER_SLACK = 1.5f;
        private static final int DEFAULT_CAPACITY = 50;
        private int mCapacity;
        private int mCount;
        private int mEnd;
        private float[] mRingLux;
        private long[] mRingTime;
        private int mStart;

        public AmbientLightRingBufferForSbl(HwSmartBackLightOutdoorNormalizedDetector this$0) {
            this(50);
        }

        public AmbientLightRingBufferForSbl(int initialCapacity) {
            this.mCapacity = initialCapacity;
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

        public boolean isEmpty() {
            return this.mCount == 0;
        }

        public void clear() {
            this.mStart = 0;
            this.mEnd = 0;
            this.mCount = 0;
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
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwSmartBackLightOutdoorNormalizedDetector() {
        getConfig();
        this.mAmbientLightRingBuffer = new AmbientLightRingBufferForSbl(this);
        this.mAmbientLightRingBufferFilter = new AmbientLightRingBufferForSbl(this);
    }

    protected void getConfig() {
        Data data = HwSmartBackLightXmlLoader.getData();
        this.mInDoorThreshold = data.inDoorThreshold;
        this.mOutDoorThreshold = data.outDoorThreshold;
        this.mBrighenDebounceTime = data.brighenDebounceTime;
        this.mDarkenDebounceTime = data.darkenDebounceTime;
        this.mBrightenLinePointsList = data.brightenLinePoints;
        this.mDarkLinePointsList = data.darkenLinePoints;
    }

    public void clearAmbientLightRingBuffer() {
        this.mAmbientLightRingBuffer.clear();
        this.mAmbientLightRingBufferFilter.clear();
    }

    public void handleLightSensorEvent(long time, float lux) {
        if (lux > 40000.0f) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lux >= max, lux=");
                stringBuilder.append(lux);
                Slog.i(str, stringBuilder.toString());
            }
            lux = 40000.0f;
        }
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mAmbientLightRingBuffer.prune(time - MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        this.mAmbientLightRingBuffer.push(time, lux);
    }

    protected float calculateAmbientLuxForNewPolicy(long now) {
        int N = this.mAmbientLightRingBuffer.size();
        if (N == 0) {
            if (DEBUG) {
                Slog.v(TAG, "calculateAmbientLux: No ambient light readings available");
            }
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

    protected long nextAmbientLightBrighteningTransitionForNewPolicy(long time) {
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mBrighenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            boolean BrightenChange;
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

    protected long nextAmbientLightDarkeningTransitionForNewPolicy(long time) {
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mDarkenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            boolean DarkenChange;
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

    private long nextAmbientLightBrighteningTransitionForOldPolicy(long time) {
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mBrighenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            boolean BrightenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) > ((float) this.mOutDoorThreshold)) {
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

    private long nextAmbientLightDarkeningTransitionForOldPolicy(long time) {
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mDarkenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0; i--) {
            boolean DarkenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) < ((float) this.mInDoorThreshold)) {
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

    private long nextAmbientLightDarkeningTransitionForInOut(long time) {
        int N = this.mAmbientLightRingBufferFilter.size();
        long debounceTime = (long) this.mBrighenDebounceTime;
        if (1 == N) {
            debounceTime = 0;
        }
        long earliestValidTime = time;
        int i = N - 1;
        while (i >= 0) {
            boolean DarkenChange;
            if (this.mAmbientLightRingBufferFilter.getLux(i) <= ((float) this.mInDoorThreshold) || this.mAmbientLightRingBufferFilter.getLux(i) >= ((float) this.mOutDoorThreshold)) {
                DarkenChange = false;
            } else {
                DarkenChange = true;
            }
            if (!DarkenChange) {
                break;
            }
            earliestValidTime = this.mAmbientLightRingBufferFilter.getTime(i);
            i--;
        }
        return earliestValidTime + debounceTime;
    }

    public void setBrightenThresholdNew() {
        if (this.mBrightenLinePointsList != null) {
            PointF temp1 = null;
            for (PointF temp : this.mBrightenLinePointsList) {
                if (temp1 == null) {
                    temp1 = temp;
                }
                if (this.mAmbientLux < temp.x) {
                    PointF temp2 = temp;
                    if (temp2.x <= temp1.x) {
                        this.mBrightenDeltaLuxMax = 1.0f;
                        if (DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Brighten_temp1.x <= temp2.x,x");
                            stringBuilder.append(temp.x);
                            stringBuilder.append(", y = ");
                            stringBuilder.append(temp.y);
                            Slog.i(str, stringBuilder.toString());
                        }
                    } else {
                        this.mBrightenDeltaLuxMax = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (this.mAmbientLux - temp1.x)) + temp1.y;
                    }
                }
                temp1 = temp;
                this.mBrightenDeltaLuxMax = temp1.y;
            }
        }
    }

    private void setDarkenThresholdNew() {
        if (this.mDarkLinePointsList != null) {
            PointF temp1 = null;
            for (PointF temp : this.mDarkLinePointsList) {
                if (temp1 == null) {
                    temp1 = temp;
                }
                if (this.mAmbientLux < temp.x) {
                    PointF temp2 = temp;
                    float f = 1.0f;
                    if (temp2.x <= temp1.x) {
                        this.mDarkenDeltaLuxMax = 1.0f;
                        if (DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Darken_temp1.x <= temp2.x,x");
                            stringBuilder.append(temp.x);
                            stringBuilder.append(", y = ");
                            stringBuilder.append(temp.y);
                            Slog.i(str, stringBuilder.toString());
                        }
                    } else {
                        float DarkenDeltaLuxMaxtmp = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (this.mAmbientLux - temp1.x)) + temp1.y;
                        if (DarkenDeltaLuxMaxtmp > 1.0f) {
                            f = DarkenDeltaLuxMaxtmp;
                        }
                        this.mDarkenDeltaLuxMax = f;
                    }
                }
                temp1 = temp;
                this.mDarkenDeltaLuxMax = temp1.y;
            }
        }
    }

    protected void updateAmbientLux(long time) {
        long j = time;
        float value = calculateAmbientLuxForNewPolicy(time);
        if (this.mAmbientLightRingBuffer.size() == 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fist sensor lux and filteredlux=");
            stringBuilder.append(value);
            stringBuilder.append(",time=");
            stringBuilder.append(j);
            Slog.i(str, stringBuilder.toString());
        }
        this.mAmbientLightRingBufferFilter.push(j, value);
        this.mAmbientLightRingBufferFilter.prune(j - MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        long nextBrightenTransition = nextAmbientLightBrighteningTransitionForNewPolicy(time);
        long nextDarkenTransition = nextAmbientLightDarkeningTransitionForNewPolicy(time);
        long nextBrightenTransitionOld = nextAmbientLightBrighteningTransitionForOldPolicy(time);
        long nextDarkenTransitionOld = nextAmbientLightDarkeningTransitionForOldPolicy(time);
        long nextInOutTransition = nextAmbientLightDarkeningTransitionForInOut(time);
        this.mAmChangeFlagSBL = false;
        boolean z = (nextBrightenTransition <= j && nextDarkenTransitionOld <= j) || ((nextDarkenTransition <= j && nextBrightenTransitionOld <= j) || ((nextBrightenTransition <= j && nextInOutTransition <= j) || ((nextDarkenTransition <= j && nextInOutTransition <= j) || ((nextBrightenTransitionOld <= j && this.mInoutFlag) || (nextDarkenTransitionOld <= j && this.mInoutFlag)))));
        boolean updateFlag = z;
        if ((nextBrightenTransition <= j && nextBrightenTransitionOld <= j) || ((nextDarkenTransition <= j && nextDarkenTransitionOld <= j) || updateFlag)) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                String str3 = str2;
                stringBuilder2.append("updateSBL_Flag=");
                stringBuilder2.append(updateFlag);
                stringBuilder2.append(",filteredlux=");
                stringBuilder2.append(value);
                stringBuilder2.append(",time=");
                stringBuilder2.append(j);
                stringBuilder2.append(",nextBTime=");
                stringBuilder2.append(nextBrightenTransition);
                stringBuilder2.append(",nextDTime=");
                stringBuilder2.append(nextDarkenTransition);
                stringBuilder2.append(",nextBTimeOld=");
                stringBuilder2.append(nextBrightenTransitionOld);
                stringBuilder2.append(",nextDTimeOld=");
                stringBuilder2.append(nextDarkenTransitionOld);
                stringBuilder2.append(",nextInOutTime=");
                stringBuilder2.append(nextInOutTransition);
                Slog.i(str3, stringBuilder2.toString());
            }
            updateParaForSBL(value);
        }
    }

    protected void updateParaForSBL(float lux) {
        float mAmbientLuxTmp = lux;
        if (mAmbientLuxTmp >= ((float) this.mOutDoorThreshold)) {
            this.mAmbientLux = mAmbientLuxTmp;
            this.mLastApicalFlag = 2;
            this.mAmChangeFlagSBL = true;
            this.mInoutFlag = false;
        }
        if (mAmbientLuxTmp < ((float) this.mInDoorThreshold)) {
            this.mAmbientLux = mAmbientLuxTmp;
            this.mLastApicalFlag = 1;
            this.mAmChangeFlagSBL = true;
            this.mInoutFlag = false;
        }
        if (mAmbientLuxTmp < ((float) this.mOutDoorThreshold) && mAmbientLuxTmp >= ((float) this.mInDoorThreshold)) {
            this.mAmbientLux = mAmbientLuxTmp;
            if (this.mAmbientLightRingBufferFilter.size() == 1) {
                this.mLastApicalFlag = 1;
            }
            this.mAmChangeFlagSBL = true;
            this.mInoutFlag = true;
        }
        setBrightenThresholdNew();
        setDarkenThresholdNew();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSBL_lux =");
            stringBuilder.append(this.mAmbientLux);
            stringBuilder.append(",IN_OUT_DoorFlag =");
            stringBuilder.append(this.mLastApicalFlag);
            stringBuilder.append(",mBrightenDeltaLuxMax=");
            stringBuilder.append(this.mBrightenDeltaLuxMax);
            stringBuilder.append(",mDarkenDeltaLuxMax=");
            stringBuilder.append(this.mDarkenDeltaLuxMax);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public float getAmbientLuxForSBL() {
        return this.mAmbientLux;
    }

    public boolean getLuxChangedFlagForSBL() {
        return this.mAmChangeFlagSBL;
    }

    public int getIndoorOutdoorFlagForSBL() {
        return this.mLastApicalFlag;
    }
}

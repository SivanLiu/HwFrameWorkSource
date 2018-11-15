package com.android.server.display;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

class DarkAdaptDetector {
    private static final int DEBUG_ACC_RATE_MAX = 20;
    private static final int DEBUG_ACC_RATE_MIN = 1;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String TAG = "DarkAdaptDetector";
    private final int DEBUG_ACC_RATE;
    private State mAdaptedState;
    private State mAdaptingState;
    private long mAutoModeOffTime;
    private long mAutoModeOnTime;
    private Data mData;
    private LuxFilter mFilter;
    private boolean mIsFirst;
    private Object mLock = new Object();
    private State mState;
    private State mUnAdaptedState;

    public enum AdaptState {
        UNADAPTED,
        ADAPTING,
        ADAPTED
    }

    private class LuxFilter {
        private final long LONG_FILTER_LIFE_DURATION_MILLIS;
        private final int MINUTE_PERIOD_MILLIS;
        private final long SHORT_FILTER_LIFE_DURATION_MILLIS;
        private int mContinueMinutes;
        private Queue<Data> mLongFilterQueue;
        private float mMinuteFilteredLuxMax = -1.0f;
        private int mMinuteLuxCnt;
        private float mMinuteLuxSum;
        private long mMinuteStartTime;
        private Queue<Data> mShortFilterQueue;

        private class Data {
            public final float lux;
            public final long time;

            public Data(long time, float lux) {
                this.time = time;
                this.lux = lux;
            }

            public String toString() {
                return String.format("(%,ds, %.1f)", new Object[]{Long.valueOf(this.time / 1000), Float.valueOf(this.lux)});
            }
        }

        public LuxFilter() {
            this.MINUTE_PERIOD_MILLIS = 60000 / DarkAdaptDetector.this.DEBUG_ACC_RATE;
            this.LONG_FILTER_LIFE_DURATION_MILLIS = (((long) DarkAdaptDetector.this.mData.adapting2AdaptedOffDurationFilterSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.SHORT_FILTER_LIFE_DURATION_MILLIS = ((((long) DarkAdaptDetector.this.mData.unadapt2AdaptingLongFilterSec) * 2) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.mLongFilterQueue = new LinkedList();
            this.mShortFilterQueue = new LinkedList();
        }

        public boolean update(long currentTime, float rawLux, float filteredLux) {
            boolean isLuxChanged = false;
            if (this.mMinuteStartTime != 0 && currentTime - this.mMinuteStartTime >= ((long) this.MINUTE_PERIOD_MILLIS)) {
                float minuteLuxAvg = this.mMinuteLuxSum / ((float) this.mMinuteLuxCnt);
                if (DarkAdaptDetector.HWFLOW) {
                    String str = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("update() ");
                    stringBuilder.append(String.format("avg=%.1f, max=%.1f", new Object[]{Float.valueOf(minuteLuxAvg), Float.valueOf(this.mMinuteFilteredLuxMax)}));
                    Slog.i(str, stringBuilder.toString());
                }
                queueLongData(new Data((this.mMinuteStartTime + currentTime) / 2, minuteLuxAvg), currentTime);
                queueShortData(new Data((this.mMinuteStartTime + currentTime) / 2, this.mMinuteFilteredLuxMax), currentTime);
                initMinuteFilter();
                this.mContinueMinutes++;
                isLuxChanged = true;
            }
            if (this.mMinuteStartTime == 0) {
                this.mMinuteStartTime = currentTime;
            }
            if (filteredLux > this.mMinuteFilteredLuxMax) {
                this.mMinuteFilteredLuxMax = filteredLux;
                isLuxChanged = true;
            }
            this.mMinuteLuxSum += rawLux;
            this.mMinuteLuxCnt++;
            return isLuxChanged;
        }

        public void clear(long currentTime) {
            if (this.mMinuteStartTime != 0) {
                if (currentTime - this.mMinuteStartTime >= ((long) (this.MINUTE_PERIOD_MILLIS / 2))) {
                    float minuteLuxAvg = this.mMinuteLuxSum / ((float) this.mMinuteLuxCnt);
                    if (DarkAdaptDetector.HWDEBUG) {
                        String str = DarkAdaptDetector.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("clear() ");
                        stringBuilder.append(String.format("avg=%.1f", new Object[]{Float.valueOf(minuteLuxAvg)}));
                        Slog.d(str, stringBuilder.toString());
                    }
                    queueLongData(new Data((this.mMinuteStartTime + currentTime) / 2, minuteLuxAvg), currentTime);
                }
                if (DarkAdaptDetector.HWDEBUG) {
                    String str2 = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("clear() ");
                    stringBuilder2.append(String.format("max=%.1f", new Object[]{Float.valueOf(this.mMinuteFilteredLuxMax)}));
                    Slog.d(str2, stringBuilder2.toString());
                }
                queueShortData(new Data((this.mMinuteStartTime + currentTime) / 2, this.mMinuteFilteredLuxMax), currentTime);
                initMinuteFilter();
                this.mContinueMinutes = 0;
            }
        }

        public float getLongFilterMaxLux(long currentTime, long duration) {
            return dequeueMaxLux(this.mLongFilterQueue, currentTime - duration);
        }

        public int getLongFilterContinueMinutes() {
            return this.mContinueMinutes;
        }

        public float getShortFilterMaxLux(long currentTime, long duration) {
            float max = -1.0f;
            if (duration > 0) {
                max = dequeueMaxLux(this.mShortFilterQueue, currentTime - duration);
            }
            return this.mMinuteFilteredLuxMax > max ? this.mMinuteFilteredLuxMax : max;
        }

        private void initMinuteFilter() {
            this.mMinuteStartTime = 0;
            this.mMinuteLuxSum = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.mMinuteFilteredLuxMax = -1.0f;
            this.mMinuteLuxCnt = 0;
        }

        private void queueLongData(Data data, long currentTime) {
            queueData(data, this.mLongFilterQueue, currentTime - this.LONG_FILTER_LIFE_DURATION_MILLIS);
            if (DarkAdaptDetector.HWDEBUG) {
                String str = DarkAdaptDetector.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("queueLongData() LongFilter=");
                stringBuilder.append(this.mLongFilterQueue);
                Slog.d(str, stringBuilder.toString());
            }
        }

        private void queueShortData(Data data, long currentTime) {
            queueData(data, this.mShortFilterQueue, currentTime - this.SHORT_FILTER_LIFE_DURATION_MILLIS);
            if (DarkAdaptDetector.HWDEBUG) {
                String str = DarkAdaptDetector.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("queueShortData() ShortFilter=");
                stringBuilder.append(this.mShortFilterQueue);
                Slog.d(str, stringBuilder.toString());
            }
        }

        private void queueData(Data data, Queue<Data> queue, long lifeTime) {
            if (data != null && queue != null) {
                queue.add(data);
                while (!queue.isEmpty() && ((Data) queue.element()).time < lifeTime) {
                    queue.remove();
                }
            }
        }

        private float dequeueMaxLux(Queue<Data> queue, long requireTime) {
            float max = -1.0f;
            if (queue == null) {
                return -1.0f;
            }
            for (Data data : queue) {
                if (data.time >= requireTime) {
                    max = data.lux > max ? data.lux : max;
                }
            }
            return max;
        }
    }

    private interface State {
        AdaptState getState();

        void handle(long j);

        void handleFirst(long j);
    }

    private class AdaptedState implements State {
        private final DarkAdaptDetector mDetector;

        public AdaptedState(DarkAdaptDetector detector) {
            this.mDetector = detector;
        }

        public AdaptState getState() {
            return AdaptState.ADAPTED;
        }

        public void handleFirst(long currentTime) {
            if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) > DarkAdaptDetector.this.mData.adapted2UnadaptShortFilterLux) {
                if (DarkAdaptDetector.HWFLOW) {
                    String str = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AdaptedState handleFirst() ");
                    stringBuilder.append(String.format("shortLux=%.1f", new Object[]{Float.valueOf(shortLux)}));
                    Slog.i(str, stringBuilder.toString());
                }
                this.mDetector.setState(AdaptState.UNADAPTED);
            }
        }

        public void handle(long currentTime) {
            if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) > DarkAdaptDetector.this.mData.adapted2UnadaptShortFilterLux) {
                if (DarkAdaptDetector.HWFLOW) {
                    String str = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AdaptedState handle() ");
                    stringBuilder.append(String.format("shortLux=%.1f", new Object[]{Float.valueOf(shortLux)}));
                    Slog.i(str, stringBuilder.toString());
                }
                this.mDetector.setState(AdaptState.UNADAPTED);
            }
        }
    }

    private class AdaptingState implements State {
        private final long TO_ADAPTED_OFF_DURATION_FILTER_MILLIS;
        private final long TO_ADAPTED_OFF_DURATION_MAX_MILLIS;
        private final long TO_ADAPTED_OFF_DURATION_MIN_MILLIS;
        private final DarkAdaptDetector mDetector;

        public AdaptingState(DarkAdaptDetector detector) {
            this.mDetector = detector;
            this.TO_ADAPTED_OFF_DURATION_MIN_MILLIS = (((long) DarkAdaptDetector.this.mData.adapting2AdaptedOffDurationMinSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.TO_ADAPTED_OFF_DURATION_FILTER_MILLIS = (((long) DarkAdaptDetector.this.mData.adapting2AdaptedOffDurationFilterSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.TO_ADAPTED_OFF_DURATION_MAX_MILLIS = (((long) DarkAdaptDetector.this.mData.adapting2AdaptedOffDurationMaxSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
        }

        public AdaptState getState() {
            return AdaptState.ADAPTING;
        }

        public void handleFirst(long currentTime) {
            if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) > DarkAdaptDetector.this.mData.adapting2UnadaptShortFilterLux) {
                if (DarkAdaptDetector.HWFLOW) {
                    String str = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AdaptingState handleFirst() ");
                    stringBuilder.append(String.format("shortLux=%.1f", new Object[]{Float.valueOf(shortLux)}));
                    Slog.i(str, stringBuilder.toString());
                }
                this.mDetector.setState(AdaptState.UNADAPTED);
                return;
            }
            long offDurationMillis = this.mDetector.getAutoModeOffDurationMillis();
            if (offDurationMillis > this.TO_ADAPTED_OFF_DURATION_MIN_MILLIS) {
                if (offDurationMillis <= this.TO_ADAPTED_OFF_DURATION_FILTER_MILLIS) {
                    if (this.mDetector.mFilter.getLongFilterMaxLux(currentTime, this.TO_ADAPTED_OFF_DURATION_FILTER_MILLIS) <= DarkAdaptDetector.this.mData.unadapt2AdaptingLongFilterLux) {
                        if (DarkAdaptDetector.HWFLOW) {
                            String str2 = DarkAdaptDetector.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("AdaptingState handleFirst() ");
                            stringBuilder2.append(String.format("offDuration=%,ds, lux=%.1f", new Object[]{Long.valueOf(offDurationMillis / 1000), Float.valueOf(longLux)}));
                            Slog.i(str2, stringBuilder2.toString());
                        }
                        this.mDetector.setState(AdaptState.ADAPTED);
                    }
                } else if (offDurationMillis <= this.TO_ADAPTED_OFF_DURATION_MAX_MILLIS && DarkAdaptDetector.this.isHourInRange(DarkAdaptDetector.this.getCurrentClockInHour(), DarkAdaptDetector.this.mData.adapting2AdaptedOnClockNoFilterBeginHour, DarkAdaptDetector.this.mData.adapting2AdaptedOnClockNoFilterEndHour)) {
                    this.mDetector.setState(AdaptState.ADAPTED);
                }
            }
        }

        public void handle(long currentTime) {
            if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) > DarkAdaptDetector.this.mData.adapting2UnadaptShortFilterLux) {
                if (DarkAdaptDetector.HWFLOW) {
                    String str = DarkAdaptDetector.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("AdaptingState handle() ");
                    stringBuilder.append(String.format("shortLux=%.1f", new Object[]{Float.valueOf(shortLux)}));
                    Slog.i(str, stringBuilder.toString());
                }
                this.mDetector.setState(AdaptState.UNADAPTED);
            }
        }
    }

    private class UnAdaptedState implements State {
        private final long TO_ADAPTED_OFF_DURATION_MILLIS;
        private final long TO_ADAPTING_FILTER_MILLIS;
        private final int TO_ADAPTING_FILTER_MINUTES;
        private final DarkAdaptDetector mDetector;

        public UnAdaptedState(DarkAdaptDetector detector) {
            this.mDetector = detector;
            this.TO_ADAPTED_OFF_DURATION_MILLIS = (((long) DarkAdaptDetector.this.mData.unadapt2AdaptedOffDurationMinSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.TO_ADAPTING_FILTER_MILLIS = (((long) DarkAdaptDetector.this.mData.unadapt2AdaptingLongFilterSec) * 1000) / ((long) DarkAdaptDetector.this.DEBUG_ACC_RATE);
            this.TO_ADAPTING_FILTER_MINUTES = DarkAdaptDetector.this.mData.unadapt2AdaptingLongFilterSec / 60;
        }

        public AdaptState getState() {
            return AdaptState.UNADAPTED;
        }

        public void handleFirst(long currentTime) {
            if (this.mDetector.getAutoModeOffDurationMillis() > this.TO_ADAPTED_OFF_DURATION_MILLIS) {
                if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) <= DarkAdaptDetector.this.mData.unadapt2AdaptedShortFilterLux && DarkAdaptDetector.this.isHourInRange(DarkAdaptDetector.this.getCurrentClockInHour(), DarkAdaptDetector.this.mData.unadapt2AdaptedOnClockNoFilterBeginHour, DarkAdaptDetector.this.mData.unadapt2AdaptedOnClockNoFilterEndHour)) {
                    if (DarkAdaptDetector.HWFLOW) {
                        String str = DarkAdaptDetector.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("UnAdaptedState handleFirst() ");
                        stringBuilder.append(String.format("offDuration=%,ds, lux=%.1f", new Object[]{Long.valueOf(offDurationMillis / 1000), Float.valueOf(shortFilterMaxLux)}));
                        Slog.i(str, stringBuilder.toString());
                    }
                    this.mDetector.setState(AdaptState.ADAPTED);
                }
            }
        }

        public void handle(long currentTime) {
            if (this.mDetector.mFilter.getLongFilterContinueMinutes() >= this.TO_ADAPTING_FILTER_MINUTES && this.mDetector.mFilter.getShortFilterMaxLux(currentTime, 0) <= DarkAdaptDetector.this.mData.unadapt2AdaptingShortFilterLux) {
                if (this.mDetector.mFilter.getShortFilterMaxLux(currentTime, this.TO_ADAPTING_FILTER_MILLIS) <= DarkAdaptDetector.this.mData.unadapt2AdaptingShortFilterLux) {
                    if (this.mDetector.mFilter.getLongFilterMaxLux(currentTime, this.TO_ADAPTING_FILTER_MILLIS) <= DarkAdaptDetector.this.mData.unadapt2AdaptingLongFilterLux) {
                        if (DarkAdaptDetector.HWFLOW) {
                            String str = DarkAdaptDetector.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("UnAdaptedState handle() ");
                            stringBuilder.append(String.format("shortLux=%.1f, longLux=%.1f", new Object[]{Float.valueOf(shortLux), Float.valueOf(longLux)}));
                            Slog.i(str, stringBuilder.toString());
                        }
                        this.mDetector.setState(AdaptState.ADAPTING);
                    }
                }
            }
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public DarkAdaptDetector(Data data) {
        int accRate = 1;
        this.mIsFirst = true;
        int accRate2 = SystemProperties.getInt("persist.darkadaptdetector.rate", 1);
        if (accRate2 >= 1) {
            accRate = accRate2;
        }
        accRate2 = 20;
        if (accRate <= 20) {
            accRate2 = accRate;
        }
        accRate = accRate2;
        this.DEBUG_ACC_RATE = accRate;
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DarkAdaptDetector() accRate=");
            stringBuilder.append(accRate);
            Slog.i(str, stringBuilder.toString());
        }
        this.mData = data;
        this.mFilter = new LuxFilter();
        this.mUnAdaptedState = new UnAdaptedState(this);
        this.mAdaptingState = new AdaptingState(this);
        this.mAdaptedState = new AdaptedState(this);
        this.mState = this.mUnAdaptedState;
    }

    public void updateLux(float rawLux, float filteredLux) {
        synchronized (this.mLock) {
            long currentTime = SystemClock.elapsedRealtime();
            boolean isLuxChanged = this.mFilter.update(currentTime, rawLux, filteredLux);
            if (this.mIsFirst) {
                this.mIsFirst = false;
                this.mAutoModeOnTime = currentTime;
                this.mState.handleFirst(currentTime);
            } else if (isLuxChanged) {
                this.mState.handle(currentTime);
            }
        }
    }

    public void setAutoModeOff() {
        synchronized (this.mLock) {
            long currentTime = SystemClock.elapsedRealtime();
            this.mFilter.clear(currentTime);
            this.mIsFirst = true;
            this.mAutoModeOffTime = currentTime;
        }
    }

    public AdaptState getState() {
        return this.mState.getState();
    }

    private void setState(AdaptState state) {
        AdaptState oldState = this.mState.getState();
        String str;
        StringBuilder stringBuilder;
        if (state == oldState) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setState() same state");
            stringBuilder.append(state);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setState() ");
            stringBuilder.append(oldState);
            stringBuilder.append(" -> ");
            stringBuilder.append(state);
            Slog.i(str, stringBuilder.toString());
        }
        switch (state) {
            case UNADAPTED:
                this.mState = this.mUnAdaptedState;
                break;
            case ADAPTING:
                this.mState = this.mAdaptingState;
                break;
            case ADAPTED:
                this.mState = this.mAdaptedState;
                break;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setState() ");
                stringBuilder.append(state);
                stringBuilder.append(" unknow");
                Slog.e(str, stringBuilder.toString());
                break;
        }
    }

    private long getAutoModeOffDurationMillis() {
        return this.mAutoModeOnTime - this.mAutoModeOffTime;
    }

    private int getCurrentClockInHour() {
        return Calendar.getInstance().get(11);
    }

    private boolean isHourInRange(int curr, int begin, int end) {
        boolean z = true;
        if (begin == end) {
            return true;
        }
        if (begin < end) {
            if (curr < begin || curr >= end) {
                z = false;
            }
            return z;
        }
        if (curr < begin && curr >= end) {
            z = false;
        }
        return z;
    }
}

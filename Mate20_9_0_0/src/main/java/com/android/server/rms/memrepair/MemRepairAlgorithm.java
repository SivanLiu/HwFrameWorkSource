package com.android.server.rms.memrepair;

import android.rms.iaware.AwareLog;
import java.util.Arrays;

public class MemRepairAlgorithm {
    public static final int DVALUE_RISE_ALL = 256;
    public static final int DVALUE_RISE_EXCEED_HALF = 64;
    public static final int DVALUE_RISE_EXCEED_ONETHIRD = 32;
    public static final int DVALUE_RISE_EXCEED_TWOTHIRD = 128;
    public static final int DVALUE_RISE_FIRST = 4;
    public static final int DVALUE_RISE_LAST = 8;
    public static final int DVALUE_RISE_MIDDLE = 16;
    public static final int DVALUE_STATE_NEGATIVE = 2;
    public static final int DVALUE_STATE_NONE = 0;
    public static final int DVALUE_STATE_POSITIVE = 1;
    public static final int ESTIMATE_CONTINUE = 3;
    public static final int ESTIMATE_DECREASE = 2;
    public static final int ESTIMATE_FAIL = 5;
    public static final int ESTIMATE_INCREASE = 1;
    public static final int ESTIMATE_OK = 6;
    public static final int ESTIMATE_PARAMS = 4;
    private static final int MAX_DVALUE_FLOAT_PERCENT = 30;
    private static final int MIN_DVALUE_FLOAT_PERCENT = 1;
    private static final int MIN_DVALUE_ZERO_SUM = 10;
    private static final String TAG = "AwareMem_MRAlgo";

    private static class BaseHolder {
        int mSrcValueCount;
        long[] mSrcValues;

        private BaseHolder() {
        }

        public void updateSrcValue(long[] srcValues, int srcValueCount) {
            this.mSrcValues = srcValues;
            this.mSrcValueCount = srcValueCount;
        }
    }

    public static class CallbackData {
        public int mDValueState;
        private int mDValueZeroSum;
        public long[] mDValues;

        public void update(int dValueState, long[] dValues, int dValueCount) {
            this.mDValueState = dValueState;
            this.mDValues = Arrays.copyOf(dValues, dValueCount);
        }

        public boolean isIncreased() {
            int riseMiddle = 1;
            if ((this.mDValueState & 256) != 0) {
                return true;
            }
            int riseFirst = (this.mDValueState & 4) != 0 ? 1 : 0;
            int riseLast = (this.mDValueState & 8) != 0 ? 1 : 0;
            if ((this.mDValueState & 16) == 0) {
                riseMiddle = 0;
            }
            return estimated(riseFirst, riseMiddle, riseLast);
        }

        private boolean estimated(int riseFirst, int riseMiddle, int riseLast) {
            int sum = (riseFirst + riseLast) + riseMiddle;
            if ((this.mDValueState & 128) != 0 && sum > 0) {
                return true;
            }
            if ((this.mDValueState & 64) != 0) {
                if (sum > 1) {
                    return true;
                }
                if (sum > 0 && this.mDValueZeroSum > 20) {
                    return true;
                }
            }
            if ((this.mDValueState & 32) != 0) {
                if (sum > 2) {
                    return true;
                }
                if (sum > 1 && (2 == riseFirst + riseLast || 2 == riseMiddle + riseLast)) {
                    return true;
                }
                if (sum > 1 && this.mDValueZeroSum > 30) {
                    return true;
                }
            }
            return false;
        }
    }

    public interface MRCallback {
        int estimateLinear(Object obj, CallbackData callbackData);
    }

    public static class MemRepairHolder extends BaseHolder {
        int mFloatPercent;
        int mMaxZoneCount;
        int mMinIncDValue;
        int mMinZoneCount;

        public /* bridge */ /* synthetic */ void updateSrcValue(long[] jArr, int i) {
            super.updateSrcValue(jArr, i);
        }

        private MemRepairHolder() {
            super();
        }

        public MemRepairHolder(int minIncDValue, int minZoneCount, int maxZoneCount) {
            super();
            this.mMinIncDValue = minIncDValue;
            this.mMinZoneCount = minZoneCount;
            this.mMaxZoneCount = maxZoneCount;
        }

        public void updateCollectCount(int minZoneCount, int maxZoneCount) {
            this.mMinZoneCount = minZoneCount;
            this.mMaxZoneCount = maxZoneCount;
        }

        public void updateFloatPercent(int floatPercent) {
            this.mFloatPercent = floatPercent;
        }

        public boolean isValid() {
            if (this.mSrcValues == null || this.mSrcValueCount < 1 || this.mSrcValueCount > this.mSrcValues.length) {
                AwareLog.e(MemRepairAlgorithm.TAG, "invalid member");
                return false;
            } else if (this.mMinZoneCount < 1 || this.mMinZoneCount >= this.mMaxZoneCount) {
                AwareLog.e(MemRepairAlgorithm.TAG, "invalid min/max zone count");
                return false;
            } else if (this.mFloatPercent >= 1 && this.mFloatPercent <= 30) {
                return true;
            } else {
                AwareLog.e(MemRepairAlgorithm.TAG, "invalid percent");
                return false;
            }
        }
    }

    public static int translateMemRepair(MemRepairHolder srcData, MRCallback callback, Object cbUser) {
        MemRepairHolder memRepairHolder = srcData;
        MRCallback mRCallback = callback;
        Object obj;
        if (memRepairHolder == null || mRCallback == null) {
            obj = cbUser;
            AwareLog.e(TAG, "invalid parameter");
            return 4;
        } else if (!srcData.isValid()) {
            return 4;
        } else {
            CallbackData cbData = new CallbackData();
            int result = 5;
            int i = 1;
            long[] dValues = new long[(memRepairHolder.mSrcValueCount - 1)];
            long[] zValues = new long[memRepairHolder.mSrcValueCount];
            int mergesCount = (memRepairHolder.mMaxZoneCount - memRepairHolder.mMinZoneCount) + 1;
            int curZoneCount = memRepairHolder.mMaxZoneCount;
            int prevMergeSize = 0;
            int mergeIdx = 0;
            while (mergeIdx < mergesCount) {
                int mergeSize = memRepairHolder.mSrcValueCount / curZoneCount;
                curZoneCount--;
                if (prevMergeSize != mergeSize) {
                    prevMergeSize = mergeSize;
                    if (mergeSize < i || memRepairHolder.mSrcValueCount / mergeSize < memRepairHolder.mMinZoneCount) {
                        break;
                    }
                    int dValueCount = getAndUpdateDValues(zValues, getAndUpdateZValues(memRepairHolder, mergeSize, zValues), dValues);
                    if (estimateSumResult(dValues, dValueCount) != 2) {
                        cbData.update(estimateDValue(cbData, memRepairHolder, dValues, dValueCount), dValues, dValueCount);
                        result = mRCallback.estimateLinear(cbUser, cbData);
                        if (result != i) {
                            if (result != 3) {
                                break;
                            }
                        } else {
                            return i;
                        }
                    }
                    return 2;
                }
                obj = cbUser;
                mergeIdx++;
                i = 1;
            }
            obj = cbUser;
            return result;
        }
    }

    private static int getAndUpdateZValues(MemRepairHolder srcData, int mergeSize, long[] zValues) {
        Arrays.fill(zValues, 0);
        int zValueCount = 0;
        int sum = 0;
        for (int j = 0; j < srcData.mSrcValueCount; j++) {
            sum = (int) (((long) sum) + srcData.mSrcValues[j]);
            if ((j + 1) % mergeSize == 0) {
                int zValueCount2 = zValueCount + 1;
                zValues[zValueCount] = (long) (sum / mergeSize);
                sum = 0;
                zValueCount = zValueCount2;
            }
        }
        if (sum > 0 && zValueCount > 0) {
            zValues[zValueCount - 1] = ((zValues[zValueCount - 1] * ((long) mergeSize)) + ((long) sum)) / ((long) (mergeSize + (srcData.mSrcValueCount % mergeSize)));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("zValueCount=");
        stringBuilder.append(zValueCount);
        stringBuilder.append(",zValues=");
        stringBuilder.append(Arrays.toString(zValues));
        AwareLog.d(str, stringBuilder.toString());
        return zValueCount;
    }

    private static int getAndUpdateDValues(long[] zValues, int zValueCount, long[] dValues) {
        Arrays.fill(dValues, 0);
        int dValueCount = 0;
        long prevValue = zValues[0];
        int j = 1;
        while (j < zValueCount) {
            int dValueCount2 = dValueCount + 1;
            dValues[dValueCount] = zValues[j] - prevValue;
            prevValue = zValues[j];
            j++;
            dValueCount = dValueCount2;
        }
        return dValueCount;
    }

    private static int estimateSumResult(long[] dValues, int dValueCount) {
        int sum = 0;
        for (int i = 0; i < dValueCount; i++) {
            sum = (int) (((long) sum) + dValues[i]);
        }
        if (sum > 0) {
            return 6;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dValues sum=");
        stringBuilder.append(sum);
        stringBuilder.append(", decreased");
        AwareLog.d(str, stringBuilder.toString());
        return 2;
    }

    private static int estimateDValue(CallbackData cbData, MemRepairHolder srcData, long[] dValues, int dValueCount) {
        int state = checkNegativeState(srcData.mFloatPercent, dValues, dValueCount);
        if ((state & 2) != 0) {
            AwareLog.d(TAG, "DVALUE_HAVE_NEGATIVE");
            return state;
        } else if (srcData.mMinIncDValue <= 0) {
            return state | 1;
        } else {
            int[] diffs = new int[dValueCount];
            cbData.mDValueZeroSum = getAndUpdateDZValue(dValues, dValueCount, diffs, srcData.mMinIncDValue);
            int firstRiseIdx = getFirstRiseIndex(diffs, dValueCount);
            if (firstRiseIdx <= -1 || firstRiseIdx != dValueCount - 1) {
                if (firstRiseIdx > -1) {
                    state |= 4;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DVALUE_RISE_FIRST_ONLY index=");
                    stringBuilder.append(firstRiseIdx);
                    AwareLog.d(str, stringBuilder.toString());
                }
                int lastRiseIdx = getLastRiseIndex(diffs, dValueCount);
                if (lastRiseIdx > -1) {
                    state |= 8;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DVALUE_RISE_LAST_ONLY index=");
                    stringBuilder2.append(lastRiseIdx);
                    AwareLog.d(str2, stringBuilder2.toString());
                }
                int middleRiseCount = getMiddleRiseCount(diffs, dValueCount, firstRiseIdx, lastRiseIdx);
                if (middleRiseCount > 0) {
                    state |= 16;
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("DVALUE_RISE_MIDDLE count=");
                    stringBuilder3.append(middleRiseCount);
                    AwareLog.d(str3, stringBuilder3.toString());
                }
                int lastRiseCount = 0;
                int firstRiseCount = firstRiseIdx > -1 ? firstRiseIdx + 1 : 0;
                if (lastRiseIdx > 0) {
                    lastRiseCount = dValueCount - lastRiseIdx;
                }
                int riseCount = (firstRiseCount + middleRiseCount) + lastRiseCount;
                if (riseCount * 3 > dValueCount * 2) {
                    state |= 128;
                } else if (riseCount * 2 > dValueCount) {
                    state |= 64;
                } else if (riseCount * 3 > dValueCount) {
                    state |= 32;
                }
                return state | 1;
            }
            state |= 256;
            AwareLog.d(TAG, "DVALUE_RISE_ALL");
            return state;
        }
    }

    private static int checkNegativeState(int floatPercent, long[] dValues, int dValueCount) {
        int i;
        int sumNegative = 0;
        int sumPositive = 0;
        int negaCount = 0;
        for (i = 0; i < dValueCount; i++) {
            negaCount += dValues[i] < 0 ? 1 : 0;
            if (dValues[i] > 0) {
                sumPositive = (int) (((long) sumPositive) + dValues[i]);
            } else {
                sumNegative = (int) (((long) sumNegative) + dValues[i]);
            }
        }
        i = 100;
        if (sumPositive > 0) {
            i = ((-sumNegative) * 100) / sumPositive;
        }
        if (i > floatPercent || negaCount * 3 > dValueCount) {
            return 2;
        }
        return 0;
    }

    private static int getAndUpdateDZValue(long[] dValues, int dValueCount, int[] dzValues, int minIncDValue) {
        Arrays.fill(dzValues, 0);
        int zeroSum = 0;
        int i = 0;
        while (i < dValueCount) {
            dzValues[i] = dValues[i] < 0 ? -1 : (int) (dValues[i] / ((long) minIncDValue));
            zeroSum += dzValues[i] > 0 ? dzValues[i] : 0;
            i++;
        }
        return zeroSum;
    }

    private static int getFirstRiseIndex(int[] diffs, int dValueCount) {
        int firstRiseIdx = 0;
        if (diffs[0] <= 0) {
            firstRiseIdx = -1;
        }
        int index = 1;
        while (index < dValueCount && diffs[index] > 0 && firstRiseIdx == index - 1) {
            firstRiseIdx = index;
            index++;
        }
        return firstRiseIdx;
    }

    private static int getLastRiseIndex(int[] diffs, int dValueCount) {
        int lastRiseIdx = diffs[dValueCount + -1] > 0 ? dValueCount - 1 : -1;
        int index = dValueCount - 2;
        while (index >= 0 && diffs[index] > 0 && lastRiseIdx == index + 1) {
            lastRiseIdx = index;
            index--;
        }
        return lastRiseIdx;
    }

    private static int getMiddleRiseCount(int[] diffs, int dValueCount, int firstRiseIdx, int lastRiseIdx) {
        int middleRiseCount = 0;
        int i = (firstRiseIdx < 0 || firstRiseIdx >= dValueCount) ? -1 : firstRiseIdx;
        firstRiseIdx = i;
        i = (lastRiseIdx < 0 || lastRiseIdx >= dValueCount) ? dValueCount - 1 : lastRiseIdx;
        lastRiseIdx = i;
        for (i = firstRiseIdx + 1; i < lastRiseIdx; i++) {
            if (diffs[i] > 0) {
                middleRiseCount++;
            }
        }
        return middleRiseCount;
    }
}

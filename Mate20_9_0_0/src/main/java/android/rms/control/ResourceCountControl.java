package android.rms.control;

import android.os.SystemClock;
import android.rms.utils.Utils;
import android.util.Log;
import java.util.HashMap;

public class ResourceCountControl {
    private static final String TAG = "RMS.ResourceCountControl";
    private final HashMap<Long, RecordReourceCount> mResourceCountMap = new HashMap();

    static final class RecordReourceCount {
        private int mCount;
        private int mOverLoadNum;
        private long mReportTimeMillis;
        private int mTotalCount;
        private boolean mWaterFlag = true;

        RecordReourceCount(int totalCount, int count, int overLoadNum, long timeMills) {
            this.mTotalCount = totalCount;
            this.mCount = count;
            this.mOverLoadNum = overLoadNum;
            this.mReportTimeMillis = timeMills;
        }
    }

    /* JADX WARNING: Missing block: B:25:0x0086, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:30:0x0091, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkCountOverload(long id, int threshold, int hardThreshold, int waterThreshold, int total, int resourceType) {
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkCountOverload:threshold=");
            stringBuilder.append(threshold);
            stringBuilder.append(" / id=");
            stringBuilder.append(id);
            Log.d(str, stringBuilder.toString());
        }
        RecordReourceCount record = getResourceCountRecord(id, true);
        synchronized (record) {
            if (-1 == total) {
                try {
                    record.mCount = record.mCount + 1;
                } catch (Throwable th) {
                }
            } else {
                record.mCount = total;
            }
            if (record.mTotalCount < record.mCount) {
                record.mTotalCount = record.mCount;
            }
            if ((record.mWaterFlag && record.mCount > threshold) || record.mCount > hardThreshold) {
                record.mWaterFlag = false;
                if (Utils.DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ResourceCountOverload: count = ");
                    stringBuilder2.append(record.mCount);
                    stringBuilder2.append(", totalCount = ");
                    stringBuilder2.append(record.mTotalCount);
                    Log.d(str2, stringBuilder2.toString());
                }
            } else if (record.mCount < waterThreshold) {
                record.mWaterFlag = true;
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x001e, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:14:0x0041, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getOverloadNumber(long id) {
        synchronized (this.mResourceCountMap) {
            RecordReourceCount record = (RecordReourceCount) this.mResourceCountMap.get(Long.valueOf(id));
            if (record != null) {
                int overNumber = record.mOverLoadNum;
                record.mOverLoadNum = 0;
                if (Utils.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getOverloadNumber: overNumber =");
                    stringBuilder.append(overNumber);
                    Log.d(str, stringBuilder.toString());
                }
            } else if (Utils.DEBUG) {
                Log.d(TAG, "getOverloadNumber: don't have this record");
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0045, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getTotalCount(long id) {
        synchronized (this.mResourceCountMap) {
            RecordReourceCount record = (RecordReourceCount) this.mResourceCountMap.get(Long.valueOf(id));
            if (record == null) {
                if (Utils.DEBUG) {
                    Log.d(TAG, "getTotalCount: don't have this record");
                }
                return 0;
            }
            int totalCount = record.mTotalCount;
            record.mTotalCount = record.mCount;
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getTotalCount: totalCount =");
                stringBuilder.append(totalCount);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public int getCount(long id) {
        synchronized (this.mResourceCountMap) {
            RecordReourceCount record = (RecordReourceCount) this.mResourceCountMap.get(Long.valueOf(id));
            if (record == null) {
                if (Utils.DEBUG) {
                    Log.d(TAG, "getTotalCount: don't have this record");
                }
                return 0;
            }
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCount: mCount =");
                stringBuilder.append(record.mCount);
                Log.d(str, stringBuilder.toString());
            }
            int access$000 = record.mCount;
            return access$000;
        }
    }

    private RecordReourceCount getResourceCountRecord(long id, boolean isCreate) {
        RecordReourceCount record;
        synchronized (this.mResourceCountMap) {
            record = (RecordReourceCount) this.mResourceCountMap.get(Long.valueOf(id));
            if (record == null && isCreate) {
                record = createResourceCountRecordLocked(id);
            }
        }
        return record;
    }

    private RecordReourceCount createResourceCountRecordLocked(long id) {
        RecordReourceCount record = new RecordReourceCount(0, 0, 0, SystemClock.uptimeMillis());
        this.mResourceCountMap.put(Long.valueOf(id), record);
        return record;
    }

    public void removeResourceCountRecord(long id) {
        synchronized (this.mResourceCountMap) {
            if (((RecordReourceCount) this.mResourceCountMap.get(Long.valueOf(id))) != null) {
                this.mResourceCountMap.remove(Long.valueOf(id));
            }
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeResourceCountRecord id/");
                stringBuilder.append(id);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    public void reduceCurrentCount(long id) {
        RecordReourceCount record = getResourceCountRecord(id, null);
        if (record == null) {
            if (Utils.DEBUG) {
                Log.d(TAG, "updateCurrentCount: error record");
            }
            return;
        }
        synchronized (record) {
            if (record.mCount > 0) {
                record.mCount = record.mCount - 1;
            }
        }
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reduceCurrentCount  count/");
            stringBuilder.append(record.mCount);
            stringBuilder.append(", id=");
            stringBuilder.append(id);
            stringBuilder.append(", mTotalCount=");
            stringBuilder.append(record.mTotalCount);
            Log.d(str, stringBuilder.toString());
        }
    }

    public boolean isReportTime(long id, int timeInterval, long preReportTime, int totalTimeInterval) {
        RecordReourceCount record = getResourceCountRecord(id, false);
        if (record == null) {
            return false;
        }
        long currentTime = SystemClock.uptimeMillis();
        record.mOverLoadNum = record.mOverLoadNum + 1;
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ResourceFlowControl.isReportTime:  id:");
            stringBuilder.append(id);
            stringBuilder.append(" timeInterval:");
            stringBuilder.append(timeInterval);
            stringBuilder.append(" preReportTime:");
            stringBuilder.append(preReportTime);
            stringBuilder.append(" totalTimeInterval:");
            stringBuilder.append(totalTimeInterval);
            stringBuilder.append(" currentTime:");
            stringBuilder.append(currentTime);
            Log.d(str, stringBuilder.toString());
        }
        if (currentTime - record.mReportTimeMillis < ((long) timeInterval) || currentTime - preReportTime < ((long) totalTimeInterval)) {
            return false;
        }
        record.mReportTimeMillis = currentTime;
        return true;
    }
}

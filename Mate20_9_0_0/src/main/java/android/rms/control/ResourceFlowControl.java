package android.rms.control;

import android.os.SystemClock;
import android.rms.utils.Utils;
import android.util.Log;
import java.util.HashMap;

public class ResourceFlowControl {
    private static final String TAG = "RMS.ResourceFlowControl";
    private long mCurrentTime = 0;
    private final HashMap<Long, RecordReourceSpeed> mResourceSpeedMap = new HashMap();

    static final class RecordReourceSpeed {
        private int mContinuousOverLoadNum;
        private int mCountInPeroid;
        private int mOverLoadNum;
        private long mReportTimeMillis;
        private long mTimeMillis;
        private int mTotalCount;

        RecordReourceSpeed(int totalCount, int countInPeroid, int overLoadNum, int mContinuousOverLoadNum, long timeMillis, long reportTimeMillis) {
            this.mTotalCount = totalCount;
            this.mCountInPeroid = countInPeroid;
            this.mOverLoadNum = overLoadNum;
            this.mContinuousOverLoadNum = mContinuousOverLoadNum;
            this.mTimeMillis = timeMillis;
            this.mReportTimeMillis = reportTimeMillis;
        }
    }

    public boolean checkSpeedOverload(long id, int threshold, int loopInterval) {
        boolean flag = false;
        RecordReourceSpeed record = getResourceSpeedRecord(id, loopInterval);
        synchronized (record) {
            long currentTime = SystemClock.uptimeMillis();
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkSpeedOverload: /countinperoid=");
                stringBuilder.append(record.mCountInPeroid);
                stringBuilder.append(" /overloadnum =");
                stringBuilder.append(record.mOverLoadNum);
                stringBuilder.append(" /mTimeMillis =");
                stringBuilder.append(record.mTimeMillis);
                stringBuilder.append(" /currentTime =");
                stringBuilder.append(currentTime);
                Log.d(str, stringBuilder.toString());
            }
            if (currentTime - record.mTimeMillis > 2 * ((long) loopInterval)) {
                record.mTimeMillis = (currentTime / ((long) loopInterval)) * ((long) loopInterval);
                record.mCountInPeroid = 0;
                record.mContinuousOverLoadNum = 0;
            } else if (currentTime - record.mTimeMillis > ((long) loopInterval) && currentTime - record.mTimeMillis <= ((long) (2 * loopInterval))) {
                record.mTimeMillis = (currentTime / ((long) loopInterval)) * ((long) loopInterval);
                if (record.mCountInPeroid > threshold) {
                    record.mContinuousOverLoadNum = record.mContinuousOverLoadNum + 1;
                } else {
                    record.mContinuousOverLoadNum = 0;
                }
                record.mCountInPeroid = 0;
            }
            record.mCountInPeroid = record.mCountInPeroid + 1;
            record.mTotalCount = record.mTotalCount + 1;
            if (record.mCountInPeroid > threshold) {
                this.mCurrentTime = currentTime;
                flag = true;
            }
        }
        return flag;
    }

    public boolean isReportTime(long id, int loopInterval, long preReportTime, int totalTimeInterval) {
        RecordReourceSpeed record = getResourceSpeedRecord(id, loopInterval);
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ResourceFlowControl.isReportTime:  id:");
            stringBuilder.append(id);
            stringBuilder.append(" timeInterval:");
            stringBuilder.append(loopInterval);
            stringBuilder.append(" preReportTime:");
            stringBuilder.append(preReportTime);
            stringBuilder.append(" totalTimeInterval:");
            stringBuilder.append(totalTimeInterval);
            stringBuilder.append(" currentTime:");
            stringBuilder.append(this.mCurrentTime);
            stringBuilder.append(" ReportTimeInThisApp:");
            stringBuilder.append(record.mReportTimeMillis);
            Log.d(str, stringBuilder.toString());
        }
        record.mOverLoadNum = record.mOverLoadNum + 1;
        if ((this.mCurrentTime - record.mReportTimeMillis < ((long) loopInterval) && record.mReportTimeMillis != 0) || this.mCurrentTime - preReportTime < ((long) totalTimeInterval)) {
            return false;
        }
        record.mReportTimeMillis = this.mCurrentTime;
        return true;
    }

    /* JADX WARNING: Missing block: B:9:0x001e, code skipped:
            return 0;
     */
    /* JADX WARNING: Missing block: B:16:0x0045, code skipped:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getOverloadNumber(long id) {
        synchronized (this.mResourceSpeedMap) {
            RecordReourceSpeed record = (RecordReourceSpeed) this.mResourceSpeedMap.get(Long.valueOf(id));
            if (record != null) {
                int overNumber = record.mOverLoadNum;
                record.mOverLoadNum = 0;
                if (Utils.DEBUG || Log.HWINFO) {
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

    /* JADX WARNING: Missing block: B:15:0x003e, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getCountInPeroid(long id) {
        synchronized (this.mResourceSpeedMap) {
            RecordReourceSpeed record = (RecordReourceSpeed) this.mResourceSpeedMap.get(Long.valueOf(id));
            if (record == null) {
                if (Utils.DEBUG) {
                    Log.d(TAG, "getOverloadPeroid: don't have this record");
                }
                return 0;
            }
            int countInPeroid = record.mCountInPeroid;
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCountInPeroid: countInPeroid =");
                stringBuilder.append(countInPeroid);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x003e, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getOverloadPeroid(long id) {
        synchronized (this.mResourceSpeedMap) {
            RecordReourceSpeed record = (RecordReourceSpeed) this.mResourceSpeedMap.get(Long.valueOf(id));
            if (record == null) {
                if (Utils.DEBUG) {
                    Log.d(TAG, "getOverloadPeroid: don't have this record");
                }
                return 0;
            }
            int overPeroid = record.mContinuousOverLoadNum;
            if (Utils.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getOverloadPeroid: overPeroid =");
                stringBuilder.append(overPeroid);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private RecordReourceSpeed getResourceSpeedRecord(long id, int loopInterval) {
        RecordReourceSpeed record;
        synchronized (this.mResourceSpeedMap) {
            record = (RecordReourceSpeed) this.mResourceSpeedMap.get(Long.valueOf(id));
            if (record == null) {
                record = createResourceSpeedRecordLocked(id, loopInterval);
            }
        }
        return record;
    }

    private RecordReourceSpeed createResourceSpeedRecordLocked(long id, int loopInterval) {
        RecordReourceSpeed record = new RecordReourceSpeed(0, 0, 0, 0, (SystemClock.uptimeMillis() / ((long) loopInterval)) * ((long) loopInterval), 0);
        this.mResourceSpeedMap.put(Long.valueOf(id), record);
        return record;
    }

    public void removeResourceSpeedRecord(long id) {
        synchronized (this.mResourceSpeedMap) {
            if (((RecordReourceSpeed) this.mResourceSpeedMap.get(Long.valueOf(id))) != null) {
                this.mResourceSpeedMap.remove(Long.valueOf(id));
                if (Utils.DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeResourceSpeedRecord id/");
                    stringBuilder.append(id);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
    }
}

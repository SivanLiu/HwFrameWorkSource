package com.android.server.location.ntp;

import android.os.SystemClock;
import android.util.Log;

public class TimeManager {
    private static boolean DBG = true;
    private static final long GPS_UTC_REFERENCE_TIME = 946656000;
    private static final long INVAILID_TIME = 0;
    private long mExpireTime;
    private String mTag;
    private long mTimeSynsBoot;
    private long mTimestamp;

    public TimeManager(String tag, long expireTime) {
        this.mExpireTime = expireTime;
        this.mTag = tag;
    }

    public long getCurrentTime() {
        String str;
        StringBuilder stringBuilder;
        if (DBG) {
            str = this.mTag;
            stringBuilder = new StringBuilder();
            stringBuilder.append("beginning mTimestamp is ");
            stringBuilder.append(this.mTimestamp);
            Log.d(str, stringBuilder.toString());
        }
        if (this.mTimestamp < GPS_UTC_REFERENCE_TIME) {
            return 0;
        }
        if (ElapsedRealTimeCheck.getInstance().canTrustElapsedRealTime()) {
            long timeTillNow = SystemClock.elapsedRealtime() - this.mTimeSynsBoot;
            if (timeTillNow >= this.mExpireTime) {
                if (DBG) {
                    Log.d(this.mTag, "getCurrentTime INVAILID_TIME");
                }
                return 0;
            }
            if (DBG) {
                str = this.mTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("end mTimestamp is ");
                stringBuilder.append(this.mTimestamp);
                Log.d(str, stringBuilder.toString());
            }
            if (DBG) {
                str = this.mTag;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getCurrentTime:");
                stringBuilder.append(this.mTimestamp + timeTillNow);
                Log.d(str, stringBuilder.toString());
            }
            return this.mTimestamp + timeTillNow;
        }
        if (DBG) {
            Log.d(this.mTag, "getCurrentTime ElapsedRealTime INVAILID_TIME");
        }
        return 0;
    }

    public void setCurrentTime(long msTime, long msTimeSynsBoot) {
        this.mTimestamp = msTime;
        this.mTimeSynsBoot = msTimeSynsBoot;
        if (DBG) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCurrentTime mTimestamp:");
            stringBuilder.append(this.mTimestamp);
            stringBuilder.append(" mTimeReference:");
            stringBuilder.append(this.mTimeSynsBoot);
            Log.d(str, stringBuilder.toString());
        }
    }

    public long getmTimestamp() {
        return this.mTimestamp;
    }
}

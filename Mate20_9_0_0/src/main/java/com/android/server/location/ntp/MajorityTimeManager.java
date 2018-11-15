package com.android.server.location.ntp;

import android.content.Context;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.server.location.InjectTimeRecord;
import java.util.Arrays;

public class MajorityTimeManager {
    private static final int DEFAULT_UNCERTAINTY = 30;
    private static final long GPS_UTC_REFERENCE_TIME = 946656000;
    private static final long INVAILID_TIME = 0;
    private static final int PRIORITY_GPS = 3;
    private static final int PRIORITY_NITZ = 2;
    private static final int PRIORITY_NTP = 1;
    private static final String TAG = "MajorityTimeManager";

    static class TimeRecord implements Comparable<TimeRecord> {
        private int priority;
        private long timeValue;

        public TimeRecord(long timeValue, int priority) {
            this.timeValue = timeValue;
            this.priority = priority;
        }

        public int compareTo(TimeRecord o) {
            if (this.timeValue < o.timeValue) {
                return -1;
            }
            if (this.timeValue > o.timeValue) {
                return 1;
            }
            return 0;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (this.timeValue != ((TimeRecord) o).timeValue) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (int) (this.timeValue ^ (this.timeValue >>> 32));
        }
    }

    public static InjectTimeRecord getInjectTime(Context context, long gpsTime, long ntpTime, long nitzTime) {
        long gpsTime2;
        long j = ntpTime;
        long j2 = nitzTime;
        InjectTimeRecord injectTimeRecord = new InjectTimeRecord();
        if (0 == j2 && Global.getInt(context.getContentResolver(), "auto_time", 0) != 0) {
            Log.i(TAG, "nitzTime is invalid, get systemTime.");
        }
        if (gpsTime < GPS_UTC_REFERENCE_TIME) {
            gpsTime2 = 0;
        } else {
            gpsTime2 = gpsTime;
        }
        String str;
        StringBuilder stringBuilder;
        if (0 != gpsTime2 && 0 != j && 0 != j2) {
            double avg = ((double) ((gpsTime2 + j) + j2)) / 3.0d;
            TimeRecord[] timeArr = new TimeRecord[]{new TimeRecord(gpsTime2, 3), new TimeRecord(j2, 2), new TimeRecord(j, 1)};
            Arrays.sort(timeArr);
            long gpsTime3 = gpsTime2;
            if (avg - ((double) timeArr[0].timeValue) < ((double) timeArr[2].timeValue) - avg) {
                choose(injectTimeRecord, timeArr[0], timeArr[1]);
            } else if (avg - ((double) timeArr[0].timeValue) > ((double) timeArr[2].timeValue) - avg) {
                choose(injectTimeRecord, timeArr[1], timeArr[2]);
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Grouping failed, inject gps time: ");
                gpsTime2 = gpsTime3;
                stringBuilder2.append(gpsTime2);
                Log.e(str2, stringBuilder2.toString());
                injectTimeRecord.setInjectTime(gpsTime2);
                injectTimeRecord.setUncertainty(30);
            }
        } else if (0 != gpsTime2 && 0 != j2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Gps time and nitz time is valid, inject gps time: ");
            stringBuilder.append(gpsTime2);
            Log.i(str, stringBuilder.toString());
            injectTimeRecord.setInjectTime(gpsTime2);
            injectTimeRecord.setUncertainty(getUncertainty(gpsTime2, j2));
        } else if (0 != j && 0 != j2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ntp time and nitz time is valid, inject ntp time: ");
            stringBuilder.append(j);
            Log.i(str, stringBuilder.toString());
            injectTimeRecord.setInjectTime(j);
            injectTimeRecord.setUncertainty(getUncertainty(ntpTime, nitzTime));
        } else if (0 != gpsTime2 && 0 != j) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Gps time and ntp time is valid, inject gps time: ");
            stringBuilder.append(gpsTime2);
            Log.i(str, stringBuilder.toString());
            injectTimeRecord.setInjectTime(gpsTime2);
            injectTimeRecord.setUncertainty(getUncertainty(gpsTime2, j));
        } else if (0 != gpsTime2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Gps time is valid, inject gps time: ");
            stringBuilder.append(gpsTime2);
            Log.i(str, stringBuilder.toString());
            injectTimeRecord.setInjectTime(gpsTime2);
            injectTimeRecord.setUncertainty(30);
        } else if (0 != j2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Nitz time is valid, inject nitz time: ");
            stringBuilder.append(j2);
            Log.i(str, stringBuilder.toString());
            injectTimeRecord.setInjectTime(j2);
            injectTimeRecord.setUncertainty(30);
        } else {
            Log.i(TAG, "Get time failed, inject invalid time.");
            injectTimeRecord.setInjectTime(0);
        }
        return injectTimeRecord;
    }

    private static void choose(InjectTimeRecord injectTimeRecord, TimeRecord x, TimeRecord y) {
        TimeRecord timeRecord = x.priority > y.priority ? x : y;
        injectTimeRecord.setInjectTime(timeRecord.timeValue);
        injectTimeRecord.setUncertainty(getUncertainty(x.timeValue, y.timeValue));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Inject time : ");
        stringBuilder.append(timeRecord.timeValue);
        stringBuilder.append(", priority : ");
        stringBuilder.append(timeRecord.priority);
        Log.i(str, stringBuilder.toString());
    }

    private static int getUncertainty(long x, long y) {
        int difference = (int) Math.abs(x - y);
        return difference > 30 ? difference : 30;
    }
}

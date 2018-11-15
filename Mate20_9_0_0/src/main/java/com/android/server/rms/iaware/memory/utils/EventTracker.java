package com.android.server.rms.iaware.memory.utils;

import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.DumpData;
import android.rms.iaware.StatisticsData;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.huawei.displayengine.IDisplayEngineService;
import java.util.ArrayList;
import java.util.Iterator;

public final class EventTracker {
    private static final int MAX_DUMP_RECORD_TIME = 60000;
    public static final int MEMORY_FEATURE_ID = FeatureType.getFeatureId(FeatureType.FEATURE_MEMORY);
    private static final int STATISTICS_TYPE_ACTION = 2;
    private static final String TAG = "AwareMem_EventTracker";
    public static final int TRACK_TYPE_END = 1004;
    public static final int TRACK_TYPE_EXIT = 1001;
    public static final int TRACK_TYPE_INIT = 1000;
    public static final int TRACK_TYPE_KILL = 1002;
    public static final int TRACK_TYPE_STOP = 1005;
    public static final int TRACK_TYPE_TRIG = 1003;
    private static EventTracker mEventTracker = null;
    private int mEvent = 0;
    private ArrayList<DumpData> mRecordData = new ArrayList();
    private ArrayList<StatisticsData> mStatisticsData = new ArrayList();
    private long mTimeStamp = 0;
    private boolean mValid = false;

    public static EventTracker getInstance() {
        EventTracker eventTracker;
        synchronized (EventTracker.class) {
            if (mEventTracker == null) {
                mEventTracker = new EventTracker();
            }
            eventTracker = mEventTracker;
        }
        return eventTracker;
    }

    private EventTracker() {
    }

    public static String toString(int event) {
        switch (event) {
            case IDisplayEngineService.DE_ACTION_PG_BROWSER_FRONT /*10001*/:
                return "TOUCH_DOWN";
            case 15001:
                return "APP_PROCESS_LAUNCHER_BEGIN";
            case 15003:
                return "APP_PROCESS_EXIT_BEGIN";
            case 15005:
                return "APP_ACTIVITY_BEGIN";
            case 20011:
                return "SCREEN_ON";
            case 30002:
                return "POLLING_TIMEOUT";
            case 80001:
                return "TOUCH_UP";
            case 85001:
                return "APP_PROCESS_LAUNCHER_FINISH";
            case 85003:
                return "APP_PROCESS_EXIT_FINIFH";
            case 85005:
                return "APP_ACTIVITY_FINISH";
            case 90011:
                return "SCREEN_OFF";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown:");
                stringBuilder.append(event);
                return stringBuilder.toString();
        }
    }

    private static String format(int event, long timeStamp) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(toString(event));
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(timeStamp);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public void trackEvent(int type, int newEvent, long newTimeStamp, String info) {
        String str;
        StringBuilder stringBuilder;
        switch (type) {
            case 1000:
                this.mValid = true;
                this.mEvent = newEvent;
                this.mTimeStamp = newTimeStamp;
                break;
            case 1001:
                if (newEvent <= 0) {
                    this.mValid = false;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(format(this.mEvent, this.mTimeStamp));
                    stringBuilder.append(" is abandoned for ");
                    stringBuilder.append(info);
                    AwareLog.i(str, stringBuilder.toString());
                    break;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(format(newEvent, newTimeStamp));
                stringBuilder.append(" is abandoned for event ");
                stringBuilder.append(format(this.mEvent, this.mTimeStamp));
                stringBuilder.append(" is ");
                stringBuilder.append(info);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case 1002:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(format(this.mEvent, this.mTimeStamp));
                stringBuilder.append(" kill ");
                stringBuilder.append(info);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case 1003:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(format(this.mEvent, this.mTimeStamp));
                stringBuilder.append(" trigger ");
                stringBuilder.append(info);
                AwareLog.i(str, stringBuilder.toString());
                break;
            case 1004:
                this.mValid = false;
                break;
            case 1005:
                if (this.mValid) {
                    this.mValid = false;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(format(this.mEvent, this.mTimeStamp));
                    stringBuilder.append(" is removed for received event ");
                    stringBuilder.append(format(newEvent, newTimeStamp));
                    AwareLog.i(str, stringBuilder.toString());
                    break;
                }
                break;
            default:
                return;
        }
    }

    public ArrayList<DumpData> getDumpData(int time) {
        Throwable th;
        long currenttime = System.currentTimeMillis();
        synchronized (this.mRecordData) {
            int i;
            try {
                if (this.mRecordData.isEmpty()) {
                    return null;
                }
                ArrayList<DumpData> tempdumplist = new ArrayList();
                int i2 = this.mRecordData.size() > 5 ? this.mRecordData.size() - 5 : 0;
                while (i2 < this.mRecordData.size()) {
                    DumpData tempDd = (DumpData) this.mRecordData.get(i2);
                    if (currenttime - tempDd.getTime() < ((long) time) * 1000) {
                        tempdumplist.add(tempDd);
                    }
                    i2++;
                }
                i = time;
                if (tempdumplist.isEmpty()) {
                    return null;
                }
                return tempdumplist;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public void insertDumpData(long time, String operation, int exetime, String reason) {
        if (operation != null && reason != null) {
            DumpData Dd = new DumpData(time, MEMORY_FEATURE_ID, operation, exetime, reason);
            synchronized (this.mRecordData) {
                if (this.mRecordData.isEmpty()) {
                    this.mRecordData.add(Dd);
                } else {
                    boolean flag = true;
                    while (!this.mRecordData.isEmpty()) {
                        if (!(time - ((DumpData) this.mRecordData.get(0)).getTime() > AppHibernateCst.DELAY_ONE_MINS)) {
                            break;
                        }
                        this.mRecordData.remove(0);
                    }
                    this.mRecordData.add(Dd);
                }
            }
        }
    }

    public ArrayList<StatisticsData> getStatisticsData() {
        synchronized (this.mStatisticsData) {
            if (this.mStatisticsData.isEmpty()) {
                return null;
            }
            ArrayList<StatisticsData> tempList = new ArrayList();
            int listSize = this.mStatisticsData.size();
            for (int i = 0; i < listSize; i++) {
                StatisticsData tempSd = (StatisticsData) this.mStatisticsData.get(i);
                tempList.add(new StatisticsData(tempSd.getFeatureId(), tempSd.getType(), tempSd.getSubType(), tempSd.getOccurCount(), tempSd.getTotalTime(), tempSd.getEffect(), tempSd.getStartTime(), tempSd.getEndTime()));
            }
            clearArrayList();
            return tempList;
        }
    }

    public void insertStatisticData(String subtype, int exetime, int effect) {
        String str = subtype;
        if (str != null) {
            synchronized (this.mStatisticsData) {
                int size = this.mStatisticsData.size();
                long now = System.currentTimeMillis();
                for (int i = 0; i < size; i++) {
                    StatisticsData data = (StatisticsData) this.mStatisticsData.get(i);
                    if (data.getSubType().equals(str)) {
                        data.setTotalTime(data.getTotalTime() + exetime);
                        data.setOccurCount(data.getOccurCount() + 1);
                        data.setEffect(data.getEffect() + effect);
                        data.setEndTime(now);
                        return;
                    }
                }
                this.mStatisticsData.add(new StatisticsData(MEMORY_FEATURE_ID, 2, str, 1, exetime, effect, now, now));
            }
        }
    }

    private void clearArrayList() {
        synchronized (this.mStatisticsData) {
            if (this.mStatisticsData.isEmpty()) {
                return;
            }
            Iterator<StatisticsData> it = this.mStatisticsData.iterator();
            long now = System.currentTimeMillis();
            while (it.hasNext()) {
                StatisticsData tempSd = (StatisticsData) it.next();
                tempSd.setOccurCount(0);
                tempSd.setTotalTime(0);
                tempSd.setEffect(0);
                tempSd.setStartTime(now);
                tempSd.setEndTime(now);
            }
        }
    }
}

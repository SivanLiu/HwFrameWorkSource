package com.huawei.opcollect.strategy;

import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SpanTimerTrigger implements ITimerTrigger {
    private static final String TAG = "SpanTimerTrigger";
    private long mEndTime;
    private int mInterval;
    private long mNextRealTime;
    private long mStartTime;

    public SpanTimerTrigger(int beginH, int beginM, int beginS, int endH, int endM, int endS, int interval) {
        long startSecond = (((((long) beginH) * 60) + ((long) beginM)) * 60) + ((long) beginS);
        long endSecond = (((((long) endH) * 60) + ((long) endM)) * 60) + ((long) endS);
        endSecond = endSecond < startSecond ? endSecond + OPCollectUtils.ONEDAYINSECOND : endSecond;
        this.mStartTime = startSecond;
        this.mEndTime = endSecond - startSecond;
        this.mInterval = interval;
        this.mNextRealTime = 0;
    }

    public static List<ITimerTrigger> fromJson(JSONArray jsonObj) throws JSONException {
        String sTime;
        String eTime;
        if (jsonObj == null || jsonObj.length() <= 0) {
            return null;
        }
        List<ITimerTrigger> triggerList = new ArrayList<>();
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            JSONObject item = jsonObj.optJSONObject(i);
            if (!(item == null || (sTime = item.getString("starttime")) == null)) {
                String[] sHms = sTime.split(":");
                if (sHms.length == 3 && (eTime = item.getString("endtime")) != null) {
                    String[] eHms = eTime.split(":");
                    if (eHms.length == 3) {
                        triggerList.add(new SpanTimerTrigger(Integer.parseInt(sHms[0]), Integer.parseInt(sHms[1]), Integer.parseInt(sHms[2]), Integer.parseInt(eHms[0]), Integer.parseInt(eHms[1]), Integer.parseInt(eHms[2]), item.getInt("interval")));
                    }
                }
            }
        }
        return triggerList;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public boolean checkTrigger(Calendar calNow, long secondOfDay, long rtNow, OdmfActionManager.NextTimer nxtTimer) {
        long timeInSecond = ((OPCollectUtils.ONEDAYINSECOND + secondOfDay) - this.mStartTime) % OPCollectUtils.ONEDAYINSECOND;
        if (timeInSecond > this.mEndTime) {
            this.mNextRealTime = 0;
            nxtTimer.update((OPCollectUtils.ONEDAYINSECOND - timeInSecond) * 1000);
            return false;
        }
        if (this.mNextRealTime == 0) {
            this.mNextRealTime = rtNow;
        }
        if (this.mNextRealTime > rtNow) {
            nxtTimer.update((this.mNextRealTime - rtNow) * 1000);
            return false;
        }
        this.mNextRealTime = ((long) this.mInterval) + rtNow;
        nxtTimer.update(((long) this.mInterval) * 1000);
        OPCollectLog.r(TAG, "mInterval:" + this.mInterval + " mNextRealTime:" + this.mNextRealTime);
        return true;
    }

    public long getStartTime() {
        return this.mStartTime;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public String toString(String prefix) {
        String prefix2 = "" + prefix;
        StringBuilder sb = new StringBuilder(prefix).append("<-SpanTimerTrigger->");
        sb.append(System.lineSeparator()).append(prefix2).append("mStartTime: ").append(OPCollectUtils.formatTimeInSecond(this.mStartTime));
        sb.append(System.lineSeparator()).append(prefix2).append("mEndTime: ").append(OPCollectUtils.formatTimeInSecond(this.mEndTime + this.mStartTime));
        sb.append(System.lineSeparator()).append(prefix2).append("mInterval: ").append(this.mInterval);
        sb.append(System.lineSeparator()).append(prefix2).append("mNextRealTime: ").append(this.mNextRealTime);
        return sb.toString();
    }
}

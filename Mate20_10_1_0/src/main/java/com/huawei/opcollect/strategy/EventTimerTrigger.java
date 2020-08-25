package com.huawei.opcollect.strategy;

import android.os.SystemClock;
import com.huawei.opcollect.strategy.OdmfActionManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventTimerTrigger implements ITimerTrigger {
    private static final String TAG = "EventTimerTrigger";
    private long mDurationTime;
    private long mEndRealTime = -1;
    private String mEvent;
    private long mInterval;
    private long mNextRealTime = -1;

    public EventTimerTrigger(String event, int durationtime, int interval) {
        this.mEvent = event;
        this.mDurationTime = (long) durationtime;
        this.mInterval = (long) interval;
    }

    public static List<EventTimerTrigger> fromJson(JSONArray jsonObj) throws JSONException {
        if (jsonObj == null || jsonObj.length() <= 0) {
            return null;
        }
        List<EventTimerTrigger> triggerList = new ArrayList<>();
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            JSONObject item = jsonObj.optJSONObject(i);
            if (item != null) {
                triggerList.add(new EventTimerTrigger(item.getString("eventname"), item.getInt("durationtime"), item.getInt("interval")));
            }
        }
        return triggerList;
    }

    public boolean startTimer(String eventname) {
        if (!this.mEvent.equals(eventname)) {
            return false;
        }
        long nowRealTime = SystemClock.elapsedRealtime() / 1000;
        if (this.mNextRealTime < 0) {
            this.mNextRealTime = nowRealTime;
        }
        this.mEndRealTime = this.mDurationTime + nowRealTime;
        return true;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public boolean checkTrigger(Calendar calNow, long secondOfDay, long rtNow, OdmfActionManager.NextTimer nxtTimer) {
        if (this.mNextRealTime < 0) {
            return false;
        }
        if (rtNow < this.mNextRealTime) {
            nxtTimer.update((this.mNextRealTime - rtNow) * 1000);
            return false;
        }
        this.mNextRealTime = this.mInterval + rtNow;
        if (this.mNextRealTime > this.mEndRealTime) {
            this.mNextRealTime = -1;
            this.mEndRealTime = -1;
        } else {
            nxtTimer.update((this.mNextRealTime - rtNow) * 1000);
        }
        return true;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public String toString(String prefix) {
        String prefix2 = "" + prefix;
        StringBuilder sb = new StringBuilder(prefix).append("<-EventTimerTrigger->");
        sb.append(System.lineSeparator()).append(prefix2).append("mEvent: ").append(this.mEvent);
        sb.append(System.lineSeparator()).append(prefix2).append("mDurationTime: ").append(this.mDurationTime);
        sb.append(System.lineSeparator()).append(prefix2).append("mInterval: ").append(this.mInterval);
        sb.append(System.lineSeparator()).append(prefix2).append("mNextRealTime: ").append(this.mNextRealTime);
        sb.append(System.lineSeparator()).append(prefix2).append("mEndRealTime: ").append(this.mEndRealTime);
        return sb.toString();
    }
}

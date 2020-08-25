package com.huawei.opcollect.strategy;

import android.os.SystemClock;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectLog;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class LoopTimerTrigger implements ITimerTrigger {
    private static final String TAG = "LoopTimerTrigger";
    private long mInterval;
    private long mNextRealTime = (SystemClock.elapsedRealtime() / 1000);

    private LoopTimerTrigger(int interval) {
        this.mInterval = (long) interval;
    }

    static List<ITimerTrigger> fromJson(JSONArray jsonObj) throws JSONException {
        if (jsonObj == null || jsonObj.length() <= 0) {
            return null;
        }
        List<ITimerTrigger> triggerList = new ArrayList<>();
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            JSONObject item = jsonObj.optJSONObject(i);
            if (item != null) {
                triggerList.add(new LoopTimerTrigger(item.getInt("interval")));
            }
        }
        if (triggerList.size() <= 1) {
            return triggerList;
        }
        OPCollectLog.e(TAG, "Multi loopTimerTrigger are configured.");
        return null;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public boolean checkTrigger(Calendar calNow, long secondOfDay, long rtNow, OdmfActionManager.NextTimer nxtTimer) {
        if (rtNow < this.mNextRealTime) {
            return false;
        }
        this.mNextRealTime = this.mInterval + rtNow;
        nxtTimer.update(this.mInterval * 1000);
        return true;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public String toString(String prefix) {
        String prefix2 = "" + prefix;
        StringBuilder sb = new StringBuilder(prefix).append("<-LoopTimerTrigger->");
        sb.append(System.lineSeparator()).append(prefix2).append("mInterval: ").append(this.mInterval);
        sb.append(System.lineSeparator()).append(prefix2).append("mNextRealTime: ").append(this.mNextRealTime);
        return sb.toString();
    }
}

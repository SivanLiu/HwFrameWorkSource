package com.huawei.opcollect.strategy;

import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TimeDisable {
    private long mEndTime;
    private long mStartTime;

    public TimeDisable(int beginH, int beginM, int beginS, int endH, int endM, int endS) {
        long startSecond = (((((long) beginH) * 60) + ((long) beginM)) * 60) + ((long) beginS);
        long endSecond = (((((long) endH) * 60) + ((long) endM)) * 60) + ((long) endS);
        endSecond = endSecond < startSecond ? endSecond + OPCollectUtils.ONEDAYINSECOND : endSecond;
        this.mStartTime = startSecond;
        this.mEndTime = endSecond - startSecond;
    }

    public static List<TimeDisable> fromJson(JSONArray jsonObj) throws JSONException {
        String sTime;
        String eTime;
        if (jsonObj == null || jsonObj.length() <= 0) {
            return null;
        }
        List<TimeDisable> triggerList = new ArrayList<>();
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            JSONObject item = jsonObj.optJSONObject(i);
            if (!(item == null || (sTime = item.getString("starttime")) == null)) {
                String[] sHms = sTime.split(":");
                if (sHms.length == 3 && (eTime = item.getString("endtime")) != null) {
                    String[] eHms = eTime.split(":");
                    if (eHms.length == 3) {
                        triggerList.add(new TimeDisable(Integer.parseInt(sHms[0]), Integer.parseInt(sHms[1]), Integer.parseInt(sHms[2]), Integer.parseInt(eHms[0]), Integer.parseInt(eHms[1]), Integer.parseInt(eHms[2])));
                    }
                }
            }
        }
        return triggerList;
    }

    public boolean checkDisable(Calendar calNow, long secondOfDay, OdmfActionManager.NextTimer nxtTimer) {
        long timeInSecond = ((secondOfDay + OPCollectUtils.ONEDAYINSECOND) - this.mStartTime) % OPCollectUtils.ONEDAYINSECOND;
        if (timeInSecond > this.mEndTime) {
            nxtTimer.update((OPCollectUtils.ONEDAYINSECOND - timeInSecond) * 1000);
            return false;
        }
        nxtTimer.update((this.mEndTime - timeInSecond) * 1000);
        return true;
    }

    public String toString(String prefix) {
        String prefix2 = "" + prefix;
        StringBuilder sb = new StringBuilder(prefix).append("<-TimeDisable->");
        sb.append(System.lineSeparator()).append(prefix2).append("mStartTime: ").append(OPCollectUtils.formatTimeInSecond(this.mStartTime));
        sb.append(System.lineSeparator()).append(prefix2).append("mEndTime: ").append(OPCollectUtils.formatTimeInSecond(this.mEndTime + this.mStartTime));
        return sb.toString();
    }
}

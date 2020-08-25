package com.huawei.opcollect.strategy;

import android.text.format.DateUtils;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OneShotTimerTrigger implements ITimerTrigger {
    private static final int ERROR_TOLERANCE_INSECOND = 10800;
    private static final int PERIOD_TYPE_DAY = 1;
    private static final int PERIOD_TYPE_MONTH = 3;
    private static final int PERIOD_TYPE_WEEK = 2;
    private static final int PERIOD_TYPE_YEAR = 4;
    private static final int RANDOM_TIME_IN_SECOND = 7200;
    private static final String TAG = "OneShotTimerTrigger";
    private int mPeriodType;
    private SpanTimerTrigger mSpanTrigger;
    private Calendar mStartDate;

    public OneShotTimerTrigger(int h, int m, int s) {
        this.mPeriodType = 1;
        initialize(h, m, s);
    }

    public OneShotTimerTrigger(int week, int h, int m, int s) {
        this.mPeriodType = 2;
        this.mStartDate = Calendar.getInstance();
        this.mStartDate.set(2000, 1, week);
        initialize(h, m, s);
    }

    public OneShotTimerTrigger(long day, int h, int m, int s) {
        this.mPeriodType = 3;
        this.mStartDate = Calendar.getInstance();
        this.mStartDate.set(2000, 1, (int) day);
        initialize(h, m, s);
    }

    public OneShotTimerTrigger(int month, int day, int h, int m, int s) {
        this.mPeriodType = 4;
        this.mStartDate = Calendar.getInstance();
        this.mStartDate.set(2000, month, day);
        initialize(h, m, s);
    }

    private void initialize(int h, int m, int s) {
        int randomTime = new SecureRandom().nextInt(RANDOM_TIME_IN_SECOND);
        OPCollectLog.i(TAG, "randomTime: " + randomTime);
        this.mSpanTrigger = new SpanTimerTrigger(h, m, s + randomTime, 0, 0, 0, 21600);
    }

    public static List<ITimerTrigger> fromJson(JSONArray jsonObj) throws JSONException {
        if (jsonObj == null || jsonObj.length() <= 0) {
            return null;
        }
        List<ITimerTrigger> triggerList = new ArrayList<>();
        OneShotTimerTrigger trigger = null;
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            JSONObject item = jsonObj.optJSONObject(i);
            if (item == null) {
                trigger = trigger;
            } else {
                String timeStr = item.getString("time");
                if (timeStr == null) {
                    trigger = trigger;
                } else {
                    String period = item.optString("period");
                    try {
                        if ("Year".equals(period)) {
                            Date date = new SimpleDateFormat("MM-dd HH:mm:ss").parse(timeStr);
                            int month = date.getMonth();
                            int dayOfMonth = date.getDate();
                            OPCollectLog.i(TAG, timeStr + " month " + month + " day " + dayOfMonth);
                            trigger = new OneShotTimerTrigger(month, dayOfMonth, date.getHours(), date.getMinutes(), date.getSeconds());
                        } else if ("Month".equals(period)) {
                            Date date2 = new SimpleDateFormat("dd HH:mm:ss").parse(timeStr);
                            long dayOfMonth2 = (long) date2.getDate();
                            OPCollectLog.i(TAG, timeStr + " day " + dayOfMonth2);
                            trigger = new OneShotTimerTrigger(dayOfMonth2, date2.getHours(), date2.getMinutes(), date2.getSeconds());
                        } else if ("Week".equals(period)) {
                            Date date3 = new SimpleDateFormat("d HH:mm:ss").parse(timeStr);
                            int dayOfWeek = date3.getDate();
                            OPCollectLog.i(TAG, timeStr + " week " + dayOfWeek);
                            trigger = new OneShotTimerTrigger(dayOfWeek, date3.getHours(), date3.getMinutes(), date3.getSeconds());
                        } else {
                            Date date4 = new SimpleDateFormat("HH:mm:ss").parse(timeStr);
                            trigger = new OneShotTimerTrigger(date4.getHours(), date4.getMinutes(), date4.getSeconds());
                        }
                        triggerList.add(trigger);
                    } catch (ParseException e) {
                        OPCollectLog.e(TAG, "Error while parsing time " + e);
                        trigger = trigger;
                    }
                }
            }
        }
        return triggerList;
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public boolean checkTrigger(Calendar calNow, long secondOfDay, long rtNow, OdmfActionManager.NextTimer nxtTimer) {
        switch (this.mPeriodType) {
            case 1:
                break;
            case 2:
                if (calNow.get(7) != this.mStartDate.get(5)) {
                    return false;
                }
                break;
            case 3:
                if (calNow.get(5) != this.mStartDate.get(5)) {
                    return false;
                }
                break;
            case 4:
                if (!(calNow.get(2) == this.mStartDate.get(2) && calNow.get(5) == this.mStartDate.get(5))) {
                    return false;
                }
            default:
                return false;
        }
        return this.mSpanTrigger.checkTrigger(calNow, secondOfDay, rtNow, nxtTimer);
    }

    @Override // com.huawei.opcollect.strategy.ITimerTrigger
    public String toString(String prefix) {
        long startTime = this.mSpanTrigger.getStartTime();
        StringBuilder sb = new StringBuilder(prefix).append("<-OneShotTimerTrigger->");
        sb.append(System.lineSeparator()).append("" + prefix).append("mStartTime: ").append(OPCollectUtils.formatTimeInSecond(startTime));
        switch (this.mPeriodType) {
            case 1:
                sb.append(" every day");
                break;
            case 2:
                sb.append(" every ").append(DateUtils.getDayOfWeekString(this.mStartDate.get(5), 30));
                break;
            case 3:
                sb.append(" ").append(this.mStartDate.get(5)).append(" every month");
                break;
            case 4:
                sb.append(" every ").append(new SimpleDateFormat("MMM d", Locale.ENGLISH).format(this.mStartDate.getTime()));
                break;
        }
        return sb.toString();
    }
}

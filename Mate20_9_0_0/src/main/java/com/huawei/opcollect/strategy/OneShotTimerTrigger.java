package com.huawei.opcollect.strategy;

import android.text.format.DateUtils;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
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
    private static final String TAG = "OneShotTimerTrigger";
    private int mPeriodType;
    private SpanTimerTrigger mSpanTrigger;
    private Calendar mStartDate;

    private void initialize(int h, int m, int s) {
        int seconds = ((((h * 60) + m) * 60) + s) + ERROR_TOLERANCE_INSECOND;
        this.mSpanTrigger = new SpanTimerTrigger(h, m, s, (seconds / 3600) % 24, (seconds / 60) % 60, seconds % 60, 21600);
    }

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

    public static List<ITimerTrigger> fromJson(JSONArray json_obj) throws JSONException {
        if (json_obj == null || json_obj.length() <= 0) {
            return null;
        }
        List<ITimerTrigger> list_trigger = new ArrayList();
        OneShotTimerTrigger trigger = null;
        int length = json_obj.length();
        int i = 0;
        while (true) {
            OneShotTimerTrigger trigger2 = trigger;
            if (i >= length) {
                return list_trigger;
            }
            JSONObject item = json_obj.optJSONObject(i);
            if (item == null) {
                trigger = trigger2;
            } else {
                String timeStr = item.getString("time");
                if (timeStr == null) {
                    trigger = trigger2;
                } else {
                    String period = item.optString("period");
                    try {
                        Date date;
                        if ("Year".equals(period)) {
                            date = new SimpleDateFormat("MM-dd HH:mm:ss").parse(timeStr);
                            int month = date.getMonth();
                            int dayOfMonth = date.getDate();
                            OPCollectLog.i(TAG, timeStr + " month " + month + " day " + dayOfMonth);
                            trigger = new OneShotTimerTrigger(month, dayOfMonth, date.getHours(), date.getMinutes(), date.getSeconds());
                        } else if ("Month".equals(period)) {
                            date = new SimpleDateFormat("dd HH:mm:ss").parse(timeStr);
                            long dayOfMonth2 = (long) date.getDate();
                            OPCollectLog.i(TAG, timeStr + " day " + dayOfMonth2);
                            OneShotTimerTrigger oneShotTimerTrigger = new OneShotTimerTrigger(dayOfMonth2, date.getHours(), date.getMinutes(), date.getSeconds());
                        } else if ("Week".equals(period)) {
                            date = new SimpleDateFormat("d HH:mm:ss").parse(timeStr);
                            int dayOfWeek = date.getDate();
                            OPCollectLog.i(TAG, timeStr + " week " + dayOfWeek);
                            trigger = new OneShotTimerTrigger(dayOfWeek, date.getHours(), date.getMinutes(), date.getSeconds());
                        } else {
                            date = new SimpleDateFormat("HH:mm:ss").parse(timeStr);
                            trigger = new OneShotTimerTrigger(date.getHours(), date.getMinutes(), date.getSeconds());
                        }
                        list_trigger.add(trigger);
                    } catch (ParseException e) {
                        OPCollectLog.e(TAG, "Error while parsing time\n" + e);
                        trigger = trigger2;
                    }
                }
            }
            i++;
        }
    }

    public boolean checkTrigger(Calendar calNow, long secondOfDay, long rtNow, NextTimer nxttimer) {
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
        return this.mSpanTrigger.checkTrigger(calNow, secondOfDay, rtNow, nxttimer);
    }

    public String toString(String prefix) {
        long starttime = this.mSpanTrigger.getStartTime();
        String prefix2 = OPCollectUtils.DUMP_PRINT_PREFIX + prefix;
        StringBuilder sb = new StringBuilder(prefix).append("<-OneShotTimerTrigger->");
        sb.append("\n").append(prefix2).append("mStartTime: ").append(OPCollectUtils.formatTimeInSecond(starttime));
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

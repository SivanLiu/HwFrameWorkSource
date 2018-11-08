package com.huawei.android.hishow;

import android.hishow.AlarmInfo;

public class AlarmInfoEx {
    public static final int DEFAULT_ALARM_ID = -1;
    public static final int EVERYDAY_CODE = 127;
    public static final int EVERYDAY_RING = 2;
    public static final int JUSTONCE_RING = 0;
    public static final int MONTOFRIDAY_CODE = 31;
    public static final int MONTOFRIDAY_RING = 1;
    public static final int NODAY_CODE = 0;
    public static final String TAG = "AlarmInfoEx";
    public static final int USER_DEFINED_RING = 3;
    public static final int WORKINGDAT_RING = 4;
    private int alarmType;
    private String alert;
    private int daysOfWeek;
    private String daysOfWeekShow;
    private int daysOfWeekType;
    private boolean enabled;
    private int hour;
    private int id;
    private String label;
    private int minutes;
    private long time;
    private boolean vibrate;
    private int volume;

    public AlarmInfoEx(AlarmInfo ai) {
        this.id = ai.getId();
        this.enabled = ai.isEnabled();
        this.hour = ai.getHour();
        this.minutes = ai.getMinutes();
        this.daysOfWeek = ai.getDaysOfWeek();
        this.daysOfWeekType = ai.getDaysOfWeekType();
        this.daysOfWeekShow = ai.getDaysOfWeekShow();
        this.time = ai.getTime();
        this.vibrate = ai.isVibrate();
        this.label = ai.getLabel();
        this.alert = ai.getAlert();
        this.volume = ai.getVolume();
        this.alarmType = ai.getAlarmType();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getHour() {
        return this.hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinutes() {
        return this.minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public int getDaysOfWeek() {
        return this.daysOfWeek;
    }

    public void setDaysOfWeek(int daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public int getDaysOfWeekType() {
        return this.daysOfWeekType;
    }

    public void setDaysOfWeekType(int daysOfWeekType) {
        this.daysOfWeekType = daysOfWeekType;
    }

    public long getTime() {
        return this.time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getDaysOfWeekShow() {
        return this.daysOfWeekShow;
    }

    public void setDaysOfWeekShow(String daysOfWeekShow) {
        this.daysOfWeekShow = daysOfWeekShow;
    }

    public boolean isVibrate() {
        return this.vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getVolume() {
        return this.volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getAlert() {
        return this.alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public int getAlarmType() {
        return this.alarmType;
    }

    public void setAlarmType(int alarmtype) {
        this.alarmType = alarmtype;
    }
}

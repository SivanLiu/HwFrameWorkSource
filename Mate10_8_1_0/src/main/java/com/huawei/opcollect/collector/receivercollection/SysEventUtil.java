package com.huawei.opcollect.collector.receivercollection;

import com.huawei.nb.model.collectencrypt.RawSysEvent;
import com.huawei.nb.query.Query;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.EventIdConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class SysEventUtil {
    public static final String EVENT_AIRPLANE_OFF = "contentobserver.airmode_off";
    public static final String EVENT_AIRPLANE_ON = "contentobserver.airmode_on";
    public static final String EVENT_APP_INSTALL = "broadcast.app_install";
    public static final String EVENT_APP_UNINSTALL = "broadcast.app_uninstall";
    public static final String EVENT_APP_UPDATE = "broadcast.app_update";
    public static final String EVENT_BLUETOOTH_CONNECTED = "broadcast.bluetooth_connected";
    public static final String EVENT_BLUETOOTH_DISCONNECTED = "broadcast.bluetooth_disconnected";
    public static final String EVENT_BLUETOOTH_OFF = "contentobserver.bluetooth_off";
    public static final String EVENT_BLUETOOTH_ON = "contentobserver.bluetooth_on";
    public static final String EVENT_BOOT_COMPLETED = "broadcast.boot_completed";
    public static final String EVENT_DESKCLOCK_ALARM = "broadcast.deskclock";
    public static final String EVENT_EYECOMFORT_OFF = "contentobserver.eyecomfort_off";
    public static final String EVENT_EYECOMFORT_ON = "contentobserver.eyecomfort_on";
    public static final String EVENT_FOREGROUND_APP_CHANGE = "calback.foreground_app_change";
    public static final String EVENT_FULL_POWER = "broadcast.full_power";
    public static final String EVENT_GPS_OFF = "contentobserver.gps_off";
    public static final String EVENT_GPS_ON = "contentobserver.gps_on";
    public static final String EVENT_HEADSET_PLUG = "broadcast.headset_plug";
    public static final String EVENT_HEADSET_UNPLUG = "broadcast.headset_unplug";
    public static final String EVENT_INPUTMETHOD_CHANGE = "contentobserver.inputmethod_change";
    public static final String EVENT_LOCATION_CHANGE = "broadcast.location_change";
    public static final String EVENT_LOCK_SCREEN = "broadcast.lock_screen";
    public static final String EVENT_LOW_POWER = "broadcast.low_power";
    public static final String EVENT_MOBILE_CONNECTED = "broadcast.mobile_connected";
    public static final String EVENT_MOBILE_DISCONNECTED = "broadcast.mobile_disconnected";
    public static final String EVENT_NET_CLOSE = "broadcast.net_close";
    public static final String EVENT_NET_OPEN = "broadcast.net_open";
    public static final String EVENT_NOTIFICATION_COMING = "broadcast.notification_coming";
    public static final String EVENT_PHONE_CALL_COMING = "broadcast.call_coming";
    public static final String EVENT_POWER_CONNECTED = "broadcast.power_connected";
    public static final String EVENT_POWER_DISCONNECTED = "broadcast.power_disconnected";
    public static final String EVENT_REBOOT = "broadcast.reboot";
    public static final String EVENT_ROTATE_OFF = "contentobserver.rotate_off";
    public static final String EVENT_ROTATE_ON = "contentobserver.rotate_on";
    public static final String EVENT_SCREEN_OFF = "broadcast.screen_off";
    public static final String EVENT_SCREEN_ON = "broadcast.screen_on";
    public static final String EVENT_SHUTDOWN_PHONE = "broadcast.shutdown_phone";
    public static final String EVENT_SMS_COMING = "broadcast.sms_coming";
    public static final String EVENT_TAKE_PICTURE = "broadcast.take_picture";
    public static final String EVENT_TIMEZONE_CHANGE = "broadcast.tomezone_change";
    public static final String EVENT_TIME_CHANGE = "broadcast.time_change";
    public static final String EVENT_VOLUME_DOWN = "broadcast.volume_down";
    public static final String EVENT_VOLUME_UP = "broadcast.volume_up";
    public static final String EVENT_WALLPAPER_CHANGE = "wallpaper_change";
    public static final String EVENT_WIFI_CONNECTED = "broadcast.wifi_connected";
    public static final String EVENT_WIFI_DISCONNECTED = "broadcast.wifi_disconnected";
    public static final String EVENT_WIFI_OFF = "broadcast.wifi_off";
    public static final String EVENT_WIFI_ON = "broadcast.wifi_on";
    private static final String TAG = "SysEventUtil";

    public static int querySysEventDailyCount(String eventName) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        Date midnight = calendar.getTime();
        OPCollectLog.d(TAG, "midnight: " + midnight.toString());
        int num = (int) OdmfCollectScheduler.getInstance().getOdmfHelper().queryManageObjectCount(Query.select(RawSysEvent.class).greaterThanOrEqualTo("mTimeStamp", midnight).equalTo("mEventName", eventName).count("*"));
        OPCollectLog.r(TAG, "eventName: " + eventName + " num: " + num);
        return num;
    }

    public static void collectSysEventData(String eventName) {
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, getRawSysEvent(eventName, EventIdConstant.PURPOSE_STR_BLANK)).sendToTarget();
    }

    public static void collectSysEventData(String eventName, String eventParam) {
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, getRawSysEvent(eventName, eventParam)).sendToTarget();
    }

    private static RawSysEvent getRawSysEvent(String eventName, String eventParam) {
        RawSysEvent rawSysEvent = new RawSysEvent();
        rawSysEvent.setMReservedText(OPCollectUtils.formatCurrentTime());
        rawSysEvent.setMReservedInt(Integer.valueOf(0));
        rawSysEvent.setMTimeStamp(OPCollectUtils.getCurrentTime());
        rawSysEvent.setMEventName(eventName);
        rawSysEvent.setMEventParam(eventParam);
        return rawSysEvent;
    }
}

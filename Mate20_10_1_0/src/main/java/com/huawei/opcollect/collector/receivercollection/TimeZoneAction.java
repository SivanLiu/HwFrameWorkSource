package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneAction extends ReceiverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "TimeZoneAction";
    private static TimeZoneAction instance = null;

    private TimeZoneAction(String name, Context context) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_TIMEZONE_CHANGE));
        OPCollectLog.r("TimeZoneAction", "TimeZoneAction");
    }

    public static TimeZoneAction getInstance(Context context) {
        TimeZoneAction timeZoneAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new TimeZoneAction("TimeZoneAction", context);
            }
            timeZoneAction = instance;
        }
        return timeZoneAction;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mReceiver == null && this.mContext != null) {
            this.mReceiver = new TimeZoneBroadcastReceiver();
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.TIMEZONE_CHANGED"), null, OdmfCollectScheduler.getInstance().getCtrlHandler());
            OPCollectLog.r("TimeZoneAction", "enabled");
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyTimeZoneActionInstance();
        return true;
    }

    private static void destroyTimeZoneActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    class TimeZoneBroadcastReceiver extends BroadcastReceiver {
        TimeZoneBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                OPCollectLog.r("TimeZoneAction", "onReceive: " + action);
                if ("android.intent.action.TIMEZONE_CHANGED".equalsIgnoreCase(action)) {
                    TimeZoneAction.this.perform();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        super.execute();
        SysEventUtil.collectSysEventData(SysEventUtil.EVENT_TIMEZONE_CHANGE, new SimpleDateFormat("z", Locale.getDefault()).format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.getDefault()).getTime()));
        return true;
    }
}

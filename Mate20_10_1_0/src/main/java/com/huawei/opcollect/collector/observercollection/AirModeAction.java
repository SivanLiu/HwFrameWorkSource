package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.utils.OPCollectLog;

public class AirModeAction extends ObserverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "AirModeAction";
    private static AirModeAction instance = null;
    private Handler mHandler = new Handler();

    private AirModeAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_AIRPLANE_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_AIRPLANE_OFF));
    }

    public static AirModeAction getInstance(Context context) {
        AirModeAction airModeAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new AirModeAction(context, "AirModeAction");
            }
            airModeAction = instance;
        }
        return airModeAction;
    }

    private final class MyContentObserver extends ContentObserver {
        MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            AirModeAction.this.perform();
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mObserver == null) {
            if (this.mContext == null) {
                OPCollectLog.e("AirModeAction", "context is null");
                return;
            }
            this.mObserver = new MyContentObserver(this.mHandler);
            try {
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mObserver);
            } catch (RuntimeException e) {
                OPCollectLog.e("AirModeAction", "registerContentObserver failed: " + e.getMessage());
            }
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyAirModeActionInstance();
        return true;
    }

    private static void destroyAirModeActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e("AirModeAction", "context is null");
            return false;
        }
        try {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on") == 0) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_AIRPLANE_OFF);
            } else {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_AIRPLANE_ON);
            }
            return true;
        } catch (Settings.SettingNotFoundException e) {
            OPCollectLog.e("AirModeAction", "SettingNotFoundException:" + e.getMessage());
            return false;
        }
    }
}

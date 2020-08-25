package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.utils.OPCollectLog;

public class PowerSavingAction extends ObserverAction {
    private static final Object LOCK = new Object();
    private static final String SMART_MODE_STATUS = "SmartModeStatus";
    private static final String TAG = "PowerSavingAction";
    private static PowerSavingAction instance = null;
    private Handler mHandler = new Handler();

    private PowerSavingAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.POWER_SAVING_STATUS));
    }

    public static PowerSavingAction getInstance(Context context) {
        PowerSavingAction powerSavingAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PowerSavingAction(context, SysEventUtil.POWER_SAVING_STATUS);
            }
            powerSavingAction = instance;
        }
        return powerSavingAction;
    }

    private final class MyContentObserver extends ContentObserver {
        MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            OPCollectLog.i(PowerSavingAction.TAG, "onChange");
            PowerSavingAction.this.perform();
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mObserver == null && this.mContext != null) {
            this.mObserver = new MyContentObserver(this.mHandler);
            try {
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SMART_MODE_STATUS), true, this.mObserver);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "registerContentObserver failed: " + e.getMessage());
            }
            SysEventUtil.collectKVSysEventData("battery/power_saving_status", SysEventUtil.POWER_SAVING_STATUS, getPowerSavingState());
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e(TAG, "context is null.");
            return false;
        }
        SysEventUtil.collectSysEventData(SysEventUtil.POWER_SAVING_STATUS, getPowerSavingState());
        SysEventUtil.collectKVSysEventData("battery/power_saving_status", SysEventUtil.POWER_SAVING_STATUS, getPowerSavingState());
        return true;
    }

    private String getPowerSavingState() {
        int state = 0;
        try {
            state = Settings.System.getInt(this.mContext.getContentResolver(), SMART_MODE_STATUS);
        } catch (Settings.SettingNotFoundException e) {
            OPCollectLog.e(TAG, "SettingNotFoundException:" + e.getMessage());
        }
        if (state == 4) {
            return SysEventUtil.ON;
        }
        return SysEventUtil.OFF;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyPowerSavingActionInstance();
        return true;
    }

    private static void destroyPowerSavingActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}

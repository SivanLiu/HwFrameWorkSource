package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.utils.OPCollectLog;

public class ZenModeAction extends ObserverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "ZenModeAction";
    public static final String ZEN_MODE = "zen_mode";
    private static ZenModeAction instance = null;
    private Handler mHandler = new Handler();

    private ZenModeAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.DISTURB_STATUS));
    }

    public static ZenModeAction getInstance(Context context) {
        ZenModeAction zenModeAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new ZenModeAction(context, SysEventUtil.DISTURB_STATUS);
            }
            zenModeAction = instance;
        }
        return zenModeAction;
    }

    private final class MyContentObserver extends ContentObserver {
        MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            OPCollectLog.i(ZenModeAction.TAG, "onChange.");
            ZenModeAction.this.perform();
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mObserver == null && this.mContext != null) {
            this.mObserver = new MyContentObserver(this.mHandler);
            try {
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(ZEN_MODE), true, this.mObserver);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "registerContentObserver failed: " + e.getMessage());
            }
            SysEventUtil.collectKVSysEventData("sound/no_disturb_status", SysEventUtil.DISTURB_STATUS, getZenModeState());
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e(TAG, "context is null.");
            return false;
        }
        SysEventUtil.collectSysEventData(SysEventUtil.DISTURB_STATUS, getZenModeState());
        SysEventUtil.collectKVSysEventData("sound/no_disturb_status", SysEventUtil.DISTURB_STATUS, getZenModeState());
        return true;
    }

    private String getZenModeState() {
        int state = 0;
        try {
            state = Settings.Global.getInt(this.mContext.getContentResolver(), ZEN_MODE);
        } catch (Settings.SettingNotFoundException e) {
            OPCollectLog.e(TAG, "SettingNotFoundException:" + e.getMessage());
        }
        if (state != 0) {
            return SysEventUtil.ON;
        }
        return SysEventUtil.OFF;
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyZenModeActionInstance();
        return true;
    }

    private static void destroyZenModeActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}

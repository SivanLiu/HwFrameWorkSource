package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.utils.OPCollectLog;

public class EyeComfortStateAction extends ObserverAction {
    private static final String EYES_PROTECTION_SWITCH = "eyes_protection_mode";
    private static final Object LOCK = new Object();
    private static final String TAG = "EyeComfortStateAction";
    private static EyeComfortStateAction instance = null;
    private Handler mHandler = new Handler();

    private EyeComfortStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_EYECOMFORT_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_EYECOMFORT_OFF));
    }

    public static EyeComfortStateAction getInstance(Context context) {
        EyeComfortStateAction eyeComfortStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new EyeComfortStateAction(context, "EyeComfortStateAction");
            }
            eyeComfortStateAction = instance;
        }
        return eyeComfortStateAction;
    }

    private final class MyContentObserver extends ContentObserver {
        MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            EyeComfortStateAction.this.perform();
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mObserver == null) {
            if (this.mContext == null) {
                OPCollectLog.e("EyeComfortStateAction", "context is null");
                return;
            }
            this.mObserver = new MyContentObserver(this.mHandler);
            try {
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(EYES_PROTECTION_SWITCH), true, this.mObserver);
            } catch (RuntimeException e) {
                OPCollectLog.e("EyeComfortStateAction", "registerContentObserver failed: " + e.getMessage());
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e("EyeComfortStateAction", "context is null");
            return false;
        }
        try {
            if (Settings.System.getInt(this.mContext.getContentResolver(), EYES_PROTECTION_SWITCH) == 0) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_EYECOMFORT_OFF);
            } else {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_EYECOMFORT_ON);
            }
            return true;
        } catch (Settings.SettingNotFoundException e) {
            OPCollectLog.e("EyeComfortStateAction", "SettingNotFoundException:" + e.getMessage());
            return false;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyEyeComfortStateActionInstance();
        return true;
    }

    private static void destroyEyeComfortStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}

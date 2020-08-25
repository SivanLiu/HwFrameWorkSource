package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.huawei.opcollect.collector.receivercollection.SysEventUtil;
import com.huawei.opcollect.utils.OPCollectLog;

public class RotationStateAction extends ObserverAction {
    private static final Object LOCK = new Object();
    private static final String TAG = "RotationStateAction";
    private static RotationStateAction instance = null;
    private Handler mHandler = new Handler();

    private RotationStateAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_ROTATE_ON) + SysEventUtil.querySysEventDailyCount(SysEventUtil.EVENT_ROTATE_OFF));
    }

    public static RotationStateAction getInstance(Context context) {
        RotationStateAction rotationStateAction;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new RotationStateAction(context, "RotationStateAction");
            }
            rotationStateAction = instance;
        }
        return rotationStateAction;
    }

    private final class MyContentObserver extends ContentObserver {
        MyContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            RotationStateAction.this.perform();
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void enable() {
        super.enable();
        if (this.mObserver == null && this.mContext != null) {
            this.mObserver = new MyContentObserver(this.mHandler);
            try {
                this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), true, this.mObserver);
            } catch (RuntimeException e) {
                OPCollectLog.e("RotationStateAction", "registerContentObserver failed: " + e.getMessage());
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.huawei.opcollect.strategy.Action
    public boolean execute() {
        if (this.mContext == null) {
            OPCollectLog.e("RotationStateAction", "context is null.");
            return false;
        }
        try {
            if (Settings.System.getInt(this.mContext.getContentResolver(), "accelerometer_rotation") == 0) {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_ROTATE_OFF);
            } else {
                SysEventUtil.collectSysEventData(SysEventUtil.EVENT_ROTATE_ON);
            }
            return true;
        } catch (Settings.SettingNotFoundException e) {
            OPCollectLog.e("RotationStateAction", "SettingNotFoundException:" + e.getMessage());
            return false;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public boolean destroy() {
        super.destroy();
        destroyRotationStateActionInstance();
        return true;
    }

    private static void destroyRotationStateActionInstance() {
        synchronized (LOCK) {
            instance = null;
        }
    }
}

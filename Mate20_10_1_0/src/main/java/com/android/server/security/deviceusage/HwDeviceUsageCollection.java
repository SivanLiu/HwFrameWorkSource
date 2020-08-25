package com.android.server.security.deviceusage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.intellicom.common.SmartDualCardConsts;

public class HwDeviceUsageCollection {
    private static final String ACTION_CALLS_TABLE_ADD_ENTRY = "com.android.server.telecom.intent.action.CALLS_ADD_ENTRY";
    private static final String CALL_DURATION = "duration";
    private static final long CALL_ENOUGH_TIME = 600;
    private static final long CHARGING_ENOUGH_TIME = 5;
    private static final long INVALID_TIME = -1;
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final long MILLIS_OF_ONE_SECOND = 1000;
    private static final long SCREEN_ON_ENOUGH_TIME = 36000;
    private static final int SET_TIME_SUCCESS = 1;
    private static final String TAG = "HwDeviceUsageCollection";
    private static final int TYPE_HAS_GET_TIME = 7;
    private static final int TYPE_OBTAIN_CALL_LOG = 1;
    private static final int TYPE_OBTAIN_CHARGING = 2;
    private static final int TYPE_OBTAIN_SCREEN_OFF = 4;
    private static final int TYPE_OBTAIN_SCREEN_ON = 3;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.deviceusage.HwDeviceUsageCollection.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                Slog.e(HwDeviceUsageCollection.TAG, "onReceive action is null");
                return;
            }
            String action = intent.getAction();
            if (HwDeviceUsageCollection.IS_HW_DEBUG) {
                Slog.d(HwDeviceUsageCollection.TAG, "action " + action);
            }
            char c = 65535;
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF)) {
                        c = 1;
                        break;
                    }
                    break;
                case -1454123155:
                    if (action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON)) {
                        c = 2;
                        break;
                    }
                    break;
                case 1019184907:
                    if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                        c = 0;
                        break;
                    }
                    break;
                case 1280405454:
                    if (action.equals(HwDeviceUsageCollection.ACTION_CALLS_TABLE_ADD_ENTRY)) {
                        c = 3;
                        break;
                    }
                    break;
            }
            if (c == 0) {
                HwDeviceUsageCollection.this.obtainCharging();
            } else if (c == 1) {
                HwDeviceUsageCollection.this.obtainScreenOff();
            } else if (c == 2) {
                HwDeviceUsageCollection.this.obtainScreenOn();
                if (HwDeviceUsageCollection.IS_HW_DEBUG) {
                    Slog.d(HwDeviceUsageCollection.TAG, "mScreenOnTime action " + HwDeviceUsageCollection.this.mScreenOnTime);
                }
            } else if (c != 3) {
                Slog.w(HwDeviceUsageCollection.TAG, "Receive error broadcast");
            } else if (HwDeviceUsageCollection.this.mTelephonyManager != null && HwDeviceUsageCollection.this.mTelephonyManager.getSimState() != 1) {
                HwDeviceUsageCollection.this.obtainCallLog(intent.getLongExtra(HwDeviceUsageCollection.CALL_DURATION, 0));
            }
        }
    };
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private HwDeviceUsageOEMINFO mHwDeviceUsageOemInfo;
    /* access modifiers changed from: private */
    public boolean mIsGetTime = false;
    private Runnable mJudgeIsDeviceUseRunnable = new Runnable() {
        /* class com.android.server.security.deviceusage.HwDeviceUsageCollection.AnonymousClass2 */

        public void run() {
            if (!HwDeviceUsageCollection.this.mIsGetTime) {
                boolean isDeviceUsed = HwDeviceUsageCollection.this.getScreenOnTime() >= HwDeviceUsageCollection.SCREEN_ON_ENOUGH_TIME && HwDeviceUsageCollection.this.getChargeTime() >= HwDeviceUsageCollection.CHARGING_ENOUGH_TIME && HwDeviceUsageCollection.this.getTalkTime() >= HwDeviceUsageCollection.CALL_ENOUGH_TIME;
                if (HwDeviceUsageCollection.IS_HW_DEBUG) {
                    Slog.d(HwDeviceUsageCollection.TAG, "isDeviceUsed = " + isDeviceUsed);
                }
                if (isDeviceUsed) {
                    HwDeviceUsageCollection.this.isFirstUseDevice();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public long mScreenOnTime = -1;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;

    public HwDeviceUsageCollection(Context context) {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, TAG);
        }
        this.mContext = context;
        this.mHwDeviceUsageOemInfo = HwDeviceUsageOEMINFO.getInstance();
    }

    public void onStart() {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "onStart");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        intentFilter.addAction(ACTION_CALLS_TABLE_ADD_ENTRY);
        intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_ON);
        intentFilter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SCREEN_OFF);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mHandlerThread = new HandlerThread("HwDeviceUsageCollectionThread");
        this.mHandlerThread.start();
        this.mHandler = new DeviceUsageHandler(this.mHandlerThread.getLooper());
        this.mHandler.post(this.mJudgeIsDeviceUseRunnable);
    }

    /* access modifiers changed from: private */
    public void obtainCallLog(long duration) {
        if (duration > 0) {
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = Long.valueOf(duration / 1000);
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void obtainCharging() {
        Message msg = Message.obtain();
        msg.what = 2;
        this.mHandler.sendMessage(msg);
    }

    /* access modifiers changed from: private */
    public void obtainScreenOn() {
        Message msg = Message.obtain();
        msg.what = 3;
        this.mHandler.sendMessage(msg);
    }

    /* access modifiers changed from: private */
    public void obtainScreenOff() {
        Message msg = Message.obtain();
        msg.what = 4;
        this.mHandler.sendMessage(msg);
    }

    private boolean isTimeNull() {
        return getFirstUseTime() == 0;
    }

    /* access modifiers changed from: private */
    public void handleCharging() {
        long mChargeTime = getChargeTime() + 1;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "mChargeTime " + mChargeTime);
        }
        setChargeTime(mChargeTime);
        this.mHandler.post(this.mJudgeIsDeviceUseRunnable);
    }

    /* access modifiers changed from: private */
    public void handleScreenOn() {
        this.mScreenOnTime = SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: private */
    public void handleScreenOff() {
        if (this.mScreenOnTime >= 0) {
            long onTime = (SystemClock.elapsedRealtime() - this.mScreenOnTime) / 1000;
            if (IS_HW_DEBUG) {
                Slog.d(TAG, "mOnTime " + onTime);
            }
            this.mScreenOnTime = -1;
            if (onTime > 0) {
                long screenOnTime = getScreenOnTime() + onTime;
                if (IS_HW_DEBUG) {
                    Slog.d(TAG, "screenOnTime " + screenOnTime);
                }
                setScreenOnTime(screenOnTime);
                this.mHandler.post(this.mJudgeIsDeviceUseRunnable);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleCallLog(long talkTime) {
        long mTalkTime = getTalkTime() + talkTime;
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "mTalkTime " + mTalkTime);
        }
        setTalkTime(mTalkTime);
        this.mHandler.post(this.mJudgeIsDeviceUseRunnable);
    }

    /* access modifiers changed from: private */
    public void isFirstUseDevice() {
        if (isTimeNull()) {
            getTime();
        }
    }

    private void reportTime(long time) {
        HwDeviceUsageReport mHwDeviceUsageReport = new HwDeviceUsageReport(this.mContext);
        if (!isTimeNull()) {
            mHwDeviceUsageReport.reportFirstUseTime(time);
        }
    }

    private void getTime() {
        if (!this.mIsGetTime) {
            this.mIsGetTime = true;
            HwDeviceFirstUseTime mHwDeviceFirstUseTime = new HwDeviceFirstUseTime(this.mContext, this.mHandler);
            mHwDeviceFirstUseTime.start();
            mHwDeviceFirstUseTime.triggerGetFirstUseTime();
        }
    }

    /* access modifiers changed from: private */
    public void handleHasGetTime(long time) {
        if (setFirstUseTime(time) == 1) {
            reportTime(time);
        }
    }

    public boolean isOpenFlagSet() {
        return this.mHwDeviceUsageOemInfo.isOpenFlagSet();
    }

    public long getScreenOnTime() {
        return this.mHwDeviceUsageOemInfo.getScreenOnTime();
    }

    public long getChargeTime() {
        return this.mHwDeviceUsageOemInfo.getChargeTime();
    }

    public long getTalkTime() {
        return this.mHwDeviceUsageOemInfo.getTalkTime();
    }

    public long getFirstUseTime() {
        return this.mHwDeviceUsageOemInfo.getFirstUseTime();
    }

    public void setOpenFlag(int flag) {
        this.mHwDeviceUsageOemInfo.setOpenFlag(flag);
    }

    public void setScreenOnTime(long time) {
        this.mHwDeviceUsageOemInfo.setScreenOnTime(time);
    }

    public void setChargeTime(long time) {
        this.mHwDeviceUsageOemInfo.setChargeTime(time);
    }

    public void setTalkTime(long time) {
        this.mHwDeviceUsageOemInfo.setTalkTime(time);
    }

    public int setFirstUseTime(long time) {
        return this.mHwDeviceUsageOemInfo.setFirstUseTime(time);
    }

    private class DeviceUsageHandler extends Handler {
        DeviceUsageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 1) {
                if (i == 2) {
                    HwDeviceUsageCollection.this.handleCharging();
                } else if (i == 3) {
                    HwDeviceUsageCollection.this.handleScreenOn();
                } else if (i == 4) {
                    HwDeviceUsageCollection.this.handleScreenOff();
                } else if (i != 7) {
                    Slog.w(HwDeviceUsageCollection.TAG, "obtain error message");
                } else if (msg.obj instanceof Long) {
                    HwDeviceUsageCollection.this.handleHasGetTime(((Long) msg.obj).longValue());
                }
            } else if (msg.obj instanceof Long) {
                HwDeviceUsageCollection.this.handleCallLog(((Long) msg.obj).longValue());
            }
        }
    }
}

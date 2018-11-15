package com.android.server.locksettings;

import android.content.Context;
import android.os.Binder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.locksettings.LockSettingsStrongAuth.StrongAuthTimeoutAlarmListener;

public class HwLockSettingsStrongAuth extends LockSettingsStrongAuth {
    private static final int MSG_SCHEDULE_WEAK_AUTH_TIMEOUT = 10;
    private static final String WEAK_AUTH_TIMEOUT_ALARM_TAG = "LockSettingsWeakAuth.timeoutForUserHw";
    private static final int mWeekInteractTime = 14400000;

    protected class HwStrongAuthTimeoutAlarmListener extends StrongAuthTimeoutAlarmListener {
        private long mAlarmTriggerTime = 0;
        private boolean mIsStrongAuth;

        public HwStrongAuthTimeoutAlarmListener(int userId) {
            super(HwLockSettingsStrongAuth.this, userId);
        }

        public void setTrigerTime(boolean isStrong, long alarmTime) {
            this.mIsStrongAuth = isStrong;
            this.mAlarmTriggerTime = alarmTime;
        }

        public void onAlarm() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("STRONG_AUTH_REQUIRED_AFTER_TIMEOUT with ");
            stringBuilder.append(this.mIsStrongAuth);
            Slog.w("LockSettings", stringBuilder.toString());
            super.onAlarm();
        }
    }

    public HwLockSettingsStrongAuth(Context context) {
        super(context);
    }

    public void reportSuccessfulWeakAuthUnlock(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("report WeakAuth from ");
        stringBuilder.append(Binder.getCallingUid());
        Slog.w("LockSettings", stringBuilder.toString());
        this.mHandler.obtainMessage(10, userId, 0).sendToTarget();
    }

    public void reportSuccessfulStrongAuthUnlock(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("report StrongAuth from ");
        stringBuilder.append(Binder.getCallingUid());
        Slog.w("LockSettings", stringBuilder.toString());
        super.reportSuccessfulStrongAuthUnlock(userId);
    }

    protected void handleScheduleWeakAuthTimeout(int userId) {
        long when = SystemClock.elapsedRealtime() + 14400000;
        HwStrongAuthTimeoutAlarmListener alarm = (HwStrongAuthTimeoutAlarmListener) this.mStrongAuthTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
        if (alarm == null) {
            Slog.w("LockSettings", "WeakAuth and no alarm exits");
        } else if (alarm.mAlarmTriggerTime >= when) {
            Slog.v("LockSettings", "WeakAuth skiped.");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WeakAuth update alarm to ");
            stringBuilder.append(when);
            Slog.w("LockSettings", stringBuilder.toString());
            this.mAlarmManager.cancel(alarm);
            alarm.setTrigerTime(false, when);
            this.mAlarmManager.set(3, when, WEAK_AUTH_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
        }
    }

    protected void handleExtendMessage(Message msg) {
        if (msg.what == 10) {
            handleScheduleWeakAuthTimeout(msg.arg1);
        }
    }

    protected StrongAuthTimeoutAlarmListener createAlarmListener(int userId) {
        return new HwStrongAuthTimeoutAlarmListener(userId);
    }
}

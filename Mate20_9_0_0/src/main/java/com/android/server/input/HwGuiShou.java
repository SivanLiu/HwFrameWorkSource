package com.android.server.input;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;

public class HwGuiShou {
    private static final String FINGERPRINT_SLIDE_SWITCH = "fingerprint_slide_switch";
    private static boolean GUISHOU_ENABLED = false;
    private static final int SIM_SUB1 = 0;
    private static final int SIM_SUB2 = 1;
    private static final String TAG = "HwGuiShou";
    private Context mContext;
    private FingerprintNavigation mFingerprintNavigationFilter;
    private FpSlideSwitchSettingsObserver mFpSlideSwitchSettingsObserver;
    private Handler mHandler;
    private HwInputManagerService mHwInputManagerService;
    private int mInCallCount = 0;
    private boolean mIsPhoneStateListenerNotRegister = true;
    private Looper mLooper;
    private TelephonyManager mPhoneManager;
    private ContentResolver mResolver;
    private int mTempSlideSwitch = 0;

    class FpSlideSwitchSettingsObserver extends ContentObserver {
        public boolean mmForceUpdateChange = false;

        FpSlideSwitchSettingsObserver(Handler handler) {
            super(handler);
        }

        public void registerContentObserver(int userId) {
            HwGuiShou.this.mResolver.registerContentObserver(Secure.getUriFor(HwGuiShou.FINGERPRINT_SLIDE_SWITCH), false, this, userId);
        }

        public void onChange(boolean selfChange) {
            int i = 1;
            boolean injectSlide = Secure.getIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, 0, ActivityManager.getCurrentUser()) != 0;
            if (this.mmForceUpdateChange || HwGuiShou.this.mInCallCount <= 0 || injectSlide) {
                this.mmForceUpdateChange = false;
                String str = HwGuiShou.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("open fingerprint nav=");
                stringBuilder.append(injectSlide);
                Log.d(str, stringBuilder.toString());
                ContentResolver access$100 = HwGuiShou.this.mResolver;
                String str2 = HwGuiShou.FINGERPRINT_SLIDE_SWITCH;
                if (!injectSlide) {
                    i = 0;
                }
                System.putInt(access$100, str2, i);
                return;
            }
            Secure.putIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, 1, ActivityManager.getCurrentUser());
        }
    }

    static {
        boolean z = false;
        GUISHOU_ENABLED = false;
        boolean isStopAlarmDisabled = SystemProperties.getBoolean("ro.config.fp_rm_alarm", false);
        boolean isGallerySlideEnabled = SystemProperties.getBoolean("ro.config.fp_navigation", true);
        boolean isNotificationSlideEnabledByAdd = SystemProperties.getBoolean("ro.config.fp_add_notification", true);
        boolean isNotificationSlideDisabledByRm = SystemProperties.getBoolean("ro.config.fp_rm_notification", false);
        if (!(isStopAlarmDisabled || isGallerySlideEnabled || (isNotificationSlideEnabledByAdd && !isNotificationSlideDisabledByRm))) {
            z = true;
        }
        GUISHOU_ENABLED = z;
    }

    public HwGuiShou(HwInputManagerService inputService, Context context, Handler handler, FingerprintNavigation fn) {
        this.mContext = context;
        this.mHandler = handler;
        this.mHwInputManagerService = inputService;
        this.mFingerprintNavigationFilter = fn;
        if (context != null) {
            this.mResolver = context.getContentResolver();
        }
    }

    public static boolean isGuiShouEnabled() {
        return GUISHOU_ENABLED;
    }

    public static boolean isFPNavigationInThreeAppsEnabled(boolean injectCamera, boolean answerCall, boolean stopAlarm) {
        boolean z = true;
        if (isGuiShouEnabled()) {
            return true;
        }
        if (injectCamera || answerCall || stopAlarm) {
            z = false;
        }
        return z;
    }

    private boolean isDefaultSlideSwitchOn() {
        if (this.mHwInputManagerService != null) {
            return this.mHwInputManagerService.isDefaultSlideSwitchOn();
        }
        return true;
    }

    private void registerPhoneStateListenerBySub(int sub) {
        this.mTempSlideSwitch = isDefaultSlideSwitchOn();
        PhoneStateListener listener = new PhoneStateListener(Integer.valueOf(sub), this.mLooper) {
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case 0:
                        Slog.i(HwGuiShou.TAG, "call state: idle");
                        HwGuiShou.this.mInCallCount = HwGuiShou.this.mInCallCount - 1;
                        if (HwGuiShou.this.mInCallCount == 0) {
                            Secure.putIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, HwGuiShou.this.mTempSlideSwitch, ActivityManager.getCurrentUser());
                            return;
                        } else if (HwGuiShou.this.mInCallCount < 0) {
                            HwGuiShou.this.mInCallCount = 0;
                            return;
                        } else {
                            return;
                        }
                    case 1:
                        Slog.i(HwGuiShou.TAG, "call state: ringing");
                        if (HwGuiShou.this.mInCallCount == 0) {
                            if (HwGuiShou.this.mFingerprintNavigationFilter.isAlarm()) {
                                HwGuiShou.this.mTempSlideSwitch = HwGuiShou.this.isDefaultSlideSwitchOn();
                            } else {
                                HwGuiShou.this.mTempSlideSwitch = Secure.getIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, 0, ActivityManager.getCurrentUser());
                            }
                        }
                        HwGuiShou.this.mInCallCount = HwGuiShou.this.mInCallCount + 1;
                        Secure.putIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, 1, ActivityManager.getCurrentUser());
                        return;
                    case 2:
                        Slog.i(HwGuiShou.TAG, "call state: offhook");
                        HwGuiShou.this.mTempSlideSwitch = HwGuiShou.this.isDefaultSlideSwitchOn();
                        HwGuiShou.this.mFpSlideSwitchSettingsObserver.mmForceUpdateChange = true;
                        Secure.putIntForUser(HwGuiShou.this.mResolver, HwGuiShou.FINGERPRINT_SLIDE_SWITCH, HwGuiShou.this.mTempSlideSwitch, ActivityManager.getCurrentUser());
                        return;
                    default:
                        return;
                }
            }
        };
        if (this.mPhoneManager != null) {
            this.mPhoneManager.listen(listener, 481);
        }
    }

    public void registerPhoneStateListener() {
        if (isGuiShouEnabled()) {
            this.mLooper = Looper.myLooper();
            this.mPhoneManager = TelephonyManager.from(this.mContext);
            if (this.mPhoneManager != null && this.mIsPhoneStateListenerNotRegister) {
                registerPhoneStateListenerBySub(0);
                registerPhoneStateListenerBySub(1);
                this.mIsPhoneStateListenerNotRegister = false;
            }
        }
    }

    public void registerFpSlideSwitchSettingsObserver() {
        if (isGuiShouEnabled()) {
            this.mFpSlideSwitchSettingsObserver = new FpSlideSwitchSettingsObserver(this.mHandler);
            this.mFpSlideSwitchSettingsObserver.registerContentObserver(UserHandle.myUserId());
        }
    }

    public void registerFpSlideSwitchSettingsObserver(int newUserId) {
        if (isGuiShouEnabled()) {
            this.mFpSlideSwitchSettingsObserver = new FpSlideSwitchSettingsObserver(this.mHandler);
            this.mFpSlideSwitchSettingsObserver.registerContentObserver(newUserId);
        }
    }

    public void setFingerprintSlideSwitchValue(boolean value) {
        if (isGuiShouEnabled()) {
            Secure.putIntForUser(this.mResolver, FINGERPRINT_SLIDE_SWITCH, value, ActivityManager.getCurrentUser());
        }
    }
}

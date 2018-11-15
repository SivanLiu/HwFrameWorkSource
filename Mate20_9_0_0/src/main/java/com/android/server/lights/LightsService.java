package com.android.server.lights;

import android.app.ActivityManager;
import android.content.Context;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.provider.Settings.Secure;
import android.util.Flog;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.display.DisplayTransformManager;
import com.android.server.usb.UsbAudioDevice;
import com.huawei.pgmng.common.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class LightsService extends SystemService {
    static final boolean DEBUG = false;
    private static final int DEFAULT_MAX_BRIGHTNESS = 255;
    private static final boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
    private static final int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    private static final String MAX_BRIGHTNESS_PATH = "/sys/class/leds/lcd_backlight0/max_brightness";
    static final String TAG = "LightsService";
    private static boolean inMirrorLinkBrightnessMode = false;
    private static long mAmountTime = 0;
    protected static boolean mHasShutDown = false;
    private static boolean mIsAutoAdjust = false;
    private static int mLcdBrightness = 100;
    public static int mMaxBrightnessFromKernel = 255;
    private static double mRatio = 1.0d;
    boolean POWER_CURVE_BLIGHT_SUPPORT;
    private boolean mBrightnessConflict;
    private int mCurBrightness = 100;
    private Handler mH;
    protected boolean mIsHighPrecision;
    final LightImpl[] mLights = new LightImpl[LightsManager.LIGHT_ID_COUNT];
    private int mLimitedMaxBrightness;
    private final LightsManager mService;
    private boolean mVrModeEnabled;
    protected boolean mWriteAutoBrightnessDbEnable;

    private final class LightImpl extends Light {
        private int mBrightnessMode;
        private int mColor;
        private int mCurrentBrightness;
        private boolean mFlashing;
        private int mId;
        private boolean mInitialized;
        private int mLastBrightnessMode;
        private int mLastColor;
        private int mMode;
        private int mOffMS;
        private int mOnMS;
        private boolean mUseLowPersistenceForVR;
        private boolean mVrModeEnabled;

        /* synthetic */ LightImpl(LightsService x0, int x1, AnonymousClass1 x2) {
            this(x1);
        }

        private LightImpl(int id) {
            this.mId = id;
        }

        public void setLcdRatio(int ratio, boolean autoAdjust) {
            LightsService.mIsAutoAdjust = autoAdjust;
            if (ratio > 100 || ratio < 1) {
                LightsService.mRatio = 1.0d;
            } else {
                LightsService.mRatio = ((double) ratio) / 100.0d;
            }
            String str = LightsService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLcdRatio ratio:");
            stringBuilder.append(ratio);
            stringBuilder.append(" autoAdjust:");
            stringBuilder.append(autoAdjust);
            Slog.i(str, stringBuilder.toString());
            if (!LightsService.this.POWER_CURVE_BLIGHT_SUPPORT) {
                setLightGradualChange(LightsService.mLcdBrightness, 0, true);
            }
        }

        public void configBrightnessRange(int ratioMin, int ratioMax, int autoLimit) {
            if (ratioMin == ratioMax && ratioMin == -1 && !LightsService.this.POWER_CURVE_BLIGHT_SUPPORT) {
                synchronized (this) {
                    if (autoLimit > 0) {
                        LightsService.this.mLimitedMaxBrightness = autoLimit;
                        if (LightsService.this.mCurBrightness > autoLimit && !LightsService.this.POWER_CURVE_BLIGHT_SUPPORT) {
                            setLightGradualChange(autoLimit, 0, true);
                        }
                    } else {
                        LightsService.this.mLimitedMaxBrightness = -1;
                        if (LightsService.mLcdBrightness > 0 && !LightsService.this.POWER_CURVE_BLIGHT_SUPPORT) {
                            setLightGradualChange(LightsService.mLcdBrightness, 0, true);
                        }
                    }
                }
                return;
            }
            Utils.configBrightnessRange(ratioMin, ratioMax, autoLimit);
        }

        public void sendSmartBackLightWithRefreshFrames(int enable, int level, int value, int frames, boolean setAfterRefresh, int enable2, int value2) {
            LightsService.this.sendSmartBackLightWithRefreshFramesImpl(this.mId, enable, level, value, frames, setAfterRefresh, enable2, value2);
        }

        public void writeAutoBrightnessDbEnable(boolean enable) {
            LightsService.this.mWriteAutoBrightnessDbEnable = enable;
            if (enable) {
                LightsService.this.sendUpdateaAutoBrightnessDbMsg();
            }
        }

        public void updateUserId(int userId) {
            LightsService.this.updateCurrentUserId(userId);
        }

        public boolean isHighPrecision() {
            return true;
        }

        public int getMaxBrightnessFromKernel() {
            return LightsService.mMaxBrightnessFromKernel;
        }

        public void updateBrightnessAdjustMode(boolean mode) {
            LightsService.this.updateBrightnessMode(mode);
        }

        public void sendSmartBackLight(int enable, int level, int value) {
            synchronized (this) {
                value = value > NetworkConstants.ARP_HWTYPE_RESERVED_HI ? NetworkConstants.ARP_HWTYPE_RESERVED_HI : value;
                int lightValue = (NetworkConstants.ARP_HWTYPE_RESERVED_HI & value) | (((enable & 1) << 24) | ((level & 255) << 16));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set smart backlight. enable is ");
                stringBuilder.append(enable);
                stringBuilder.append(",level is ");
                stringBuilder.append(level);
                stringBuilder.append(",value is ");
                stringBuilder.append(value);
                stringBuilder.append(",lightValue is ");
                stringBuilder.append(lightValue);
                Flog.i(NetdResponseCode.BandwidthControl, stringBuilder.toString());
                LightsService.setLight_native(this.mId, lightValue, 0, 0, 0, 0);
            }
        }

        public void sendCustomBackLight(int backlight) {
            if (!LightsService.inMirrorLinkBrightnessMode) {
                synchronized (this) {
                    LightsService.setLight_native(this.mId, backlight, 0, 0, 0, 0);
                }
            }
        }

        public void sendAmbientLight(int ambientLight) {
            synchronized (this) {
                LightsService.setLight_native(this.mId, ambientLight, 0, 0, 0, 0);
            }
        }

        public void sendSREWithRefreshFrames(int enable, int ambientLightThreshold, int ambientLight, int frames, boolean setAfterRefresh, int enable2, int ambientLight2) {
            LightsService.this.sendSREWithRefreshFramesImpl(this.mId, enable, ambientLightThreshold, ambientLight, frames, setAfterRefresh, enable2, ambientLight2);
        }

        public void setBrightness(int brightness) {
            if (!LightsService.inMirrorLinkBrightnessMode) {
                setBrightness(brightness, 0);
            }
        }

        public void setMirrorLinkBrightness(int target) {
            synchronized (this) {
                String str = LightsService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setMirrorLinkBrightnessStatus  brightness is ");
                stringBuilder.append(target);
                Slog.i(str, stringBuilder.toString());
                int brightness = LightsService.this.mapIntoRealBacklightLevel((target * 10000) / 255);
                if (LightsService.this.mIsHighPrecision) {
                    setLightLocked_10000stage(brightness & NetworkConstants.ARP_HWTYPE_RESERVED_HI, 0, 0, 0, 0);
                } else {
                    int color = brightness & 255;
                    setLightLocked(((UsbAudioDevice.kAudioDeviceMetaMask | (color << 16)) | (color << 8)) | color, 0, 0, 0, 0);
                }
            }
        }

        public void setMirrorLinkBrightnessStatus(boolean status) {
            String str = LightsService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMirrorLinkBrightnessStatus  status is ");
            stringBuilder.append(status);
            Slog.i(str, stringBuilder.toString());
            LightsService.inMirrorLinkBrightnessMode = status;
        }

        /* JADX WARNING: Missing block: B:59:0x0130, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setBrightness(int brightness, int brightnessMode) {
            this.mCurrentBrightness = brightness;
            if (!LightsService.inMirrorLinkBrightnessMode && !LightsService.mHasShutDown) {
                if (this.mId == 0) {
                    LightsService.this.mBrightnessConflict = true;
                    LightsService.mLcdBrightness = brightness;
                    if (LightsService.this.mLimitedMaxBrightness > 0 && brightness > LightsService.this.mLimitedMaxBrightness) {
                        brightness = LightsService.this.mLimitedMaxBrightness;
                    }
                    LightsService.this.sendUpdateaAutoBrightnessDbMsg();
                    if (brightness == 0 || ((!LightsService.mIsAutoAdjust && LightsService.mRatio >= 1.0d) || LightsService.this.POWER_CURVE_BLIGHT_SUPPORT)) {
                        LightsService.this.mCurBrightness = brightness;
                    } else {
                        setLightGradualChange(brightness, brightnessMode, false);
                        return;
                    }
                }
                synchronized (this) {
                    String str;
                    StringBuilder stringBuilder;
                    if (brightnessMode == 2) {
                        str = LightsService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("setBrightness with LOW_PERSISTENCE unexpected #");
                        stringBuilder.append(this.mId);
                        stringBuilder.append(": brightness=0x");
                        stringBuilder.append(Integer.toHexString(brightness));
                        Slog.w(str, stringBuilder.toString());
                        return;
                    }
                    brightness = LightsService.this.mapIntoRealBacklightLevel(brightness);
                    if (LightsService.this.mIsHighPrecision) {
                        int color = NetworkConstants.ARP_HWTYPE_RESERVED_HI & brightness;
                        if (this.mId == 2 && LightsService.FRONT_FINGERPRINT_NAVIGATION && LightsService.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                            int color2 = (color * 255) / LightsService.this.getNormalizedMaxBrightness();
                            if (color2 == 0 && brightness != 0) {
                                color2 = 1;
                            }
                            color2 &= 255;
                            String str2 = LightsService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("HighPrecision, Set button brihtness:");
                            stringBuilder2.append(color2);
                            stringBuilder2.append(", bcaklight brightness:");
                            stringBuilder2.append(brightness);
                            Slog.d(str2, stringBuilder2.toString());
                            setLightLocked(color2, 0, 0, 0, brightnessMode);
                            return;
                        }
                        setLightLocked_10000stage(color, 0, 0, 0, brightnessMode);
                    } else {
                        int color3 = brightness & 255;
                        if (this.mId == 2 && LightsService.FRONT_FINGERPRINT_NAVIGATION && LightsService.FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1) {
                            str = LightsService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Set button brihtness:");
                            stringBuilder.append(color3);
                            Slog.i(str, stringBuilder.toString());
                            setLightLocked(color3, 0, 0, 0, brightnessMode);
                            return;
                        }
                        setLightLocked(((UsbAudioDevice.kAudioDeviceMetaMask | (color3 << 16)) | (color3 << 8)) | color3, 0, 0, 0, brightnessMode);
                    }
                }
            }
        }

        public int getCurrentBrightness() {
            return this.mCurrentBrightness;
        }

        private void setLightGradualChange(int brightness, int brightnessMode, boolean isPGset) {
            Throwable th;
            int regulateTime;
            int amount;
            int tarBrightness = brightness;
            if (LightsService.mRatio < 1.0d) {
                tarBrightness = Utils.getRatioBright(tarBrightness, LightsService.mRatio);
            }
            if (LightsService.mIsAutoAdjust) {
                tarBrightness = Utils.getAutoAdjustBright(tarBrightness);
            }
            if (!isPGset) {
                if (LightsService.this.mCurBrightness == 0 && tarBrightness > 0) {
                    LightsService.mAmountTime = SystemClock.elapsedRealtime();
                }
                if (SystemClock.elapsedRealtime() - LightsService.mAmountTime < 1000) {
                    LightsService.this.mCurBrightness = tarBrightness;
                }
            }
            if (LightsService.this.mLimitedMaxBrightness > 0 && tarBrightness > LightsService.this.mLimitedMaxBrightness) {
                tarBrightness = LightsService.this.mLimitedMaxBrightness;
            }
            int tarBrightness2 = tarBrightness;
            tarBrightness = 1;
            int brightnessGap = 25;
            if (brightness > 255 || isHighPrecision()) {
                tarBrightness = (1 + 1) * 39;
                brightnessGap = (25 + 1) * 39;
            }
            int minAmount = tarBrightness;
            int brightnessGap2 = brightnessGap;
            tarBrightness = 20;
            if (LightsService.mRatio < 1.0d) {
                tarBrightness = 16;
            }
            tarBrightness = Math.abs(LightsService.this.mCurBrightness - tarBrightness2) / tarBrightness;
            int transitionTime = DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE;
            boolean z = false;
            LightsService.this.mBrightnessConflict = false;
            synchronized (this) {
                int i;
                int regulateTime2 = 0;
                brightnessGap = tarBrightness;
                tarBrightness = 0;
                while (true) {
                    try {
                        if (!LightsService.this.mBrightnessConflict && regulateTime2 >= transitionTime) {
                            brightnessGap = minAmount;
                        }
                        if (LightsService.this.mBrightnessConflict) {
                            try {
                                if (LightsService.mRatio != 1.0d || LightsService.mLcdBrightness - LightsService.this.mCurBrightness <= brightnessGap2) {
                                    Slog.i(LightsService.TAG, "set brightness confict and break...");
                                } else {
                                    brightnessGap = Math.abs(LightsService.this.mCurBrightness - tarBrightness2) / 5;
                                    LightsService.this.mBrightnessConflict = z;
                                    String str = LightsService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("confict and set amount = ");
                                    stringBuilder.append(brightnessGap);
                                    Slog.i(str, stringBuilder.toString());
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                i = transitionTime;
                                throw th;
                            }
                        }
                        int amount2 = brightnessGap;
                        try {
                            boolean z2;
                            tarBrightness = Utils.getAnimatedValue(tarBrightness2, LightsService.this.mCurBrightness, amount2);
                            LightsService.this.mCurBrightness = tarBrightness;
                            tarBrightness = LightsService.this.mapIntoRealBacklightLevel(tarBrightness);
                            if (LightsService.this.mIsHighPrecision) {
                                regulateTime = regulateTime2;
                                amount = amount2;
                                z2 = false;
                                i = transitionTime;
                                try {
                                    setLightLocked_10000stage(tarBrightness & NetworkConstants.ARP_HWTYPE_RESERVED_HI, 0, 0, 0, brightnessMode);
                                } catch (Throwable th3) {
                                    th = th3;
                                    brightnessGap = amount;
                                    regulateTime2 = regulateTime;
                                    throw th;
                                }
                            }
                            regulateTime = regulateTime2;
                            amount = amount2;
                            i = transitionTime;
                            z2 = false;
                            brightnessGap = tarBrightness & 255;
                            setLightLocked(((UsbAudioDevice.kAudioDeviceMetaMask | (brightnessGap << 16)) | (brightnessGap << 8)) | brightnessGap, 0, 0, 0, brightnessMode);
                            if (LightsService.mLcdBrightness == 0) {
                                Slog.w(LightsService.TAG, "synchronized conflict...");
                                brightnessGap = amount;
                                regulateTime2 = regulateTime;
                                break;
                            }
                            if (LightsService.this.mCurBrightness != tarBrightness2) {
                                SystemClock.sleep(16);
                                regulateTime2 = regulateTime + 16;
                            } else {
                                regulateTime2 = regulateTime;
                            }
                            try {
                                if (LightsService.this.mCurBrightness == tarBrightness2) {
                                    break;
                                }
                                brightnessGap = amount;
                                z = z2;
                                transitionTime = i;
                            } catch (Throwable th4) {
                                th = th4;
                                brightnessGap = amount;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            regulateTime = regulateTime2;
                            i = transitionTime;
                            brightnessGap = amount2;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        regulateTime = regulateTime2;
                        i = transitionTime;
                        throw th;
                    }
                }
                Slog.i(LightsService.TAG, "set brightness confict and break...");
                i = transitionTime;
                try {
                    LightsService.this.mBrightnessConflict = true;
                } catch (Throwable th7) {
                    th = th7;
                    throw th;
                }
            }
        }

        public void setColor(int color) {
            synchronized (this) {
                setLightLocked(color, 0, 0, 0, 0);
            }
        }

        public void setFlashing(int color, int mode, int onMS, int offMS) {
            synchronized (this) {
                setLightLocked(color, mode, onMS, offMS, 0);
            }
        }

        public void pulse() {
            pulse(UsbAudioDevice.kAudioDeviceClassMask, 7);
        }

        public void pulse(int color, int onMS) {
            synchronized (this) {
                if (this.mColor == 0 && !this.mFlashing) {
                    setLightLocked(color, 2, onMS, 1000, 0);
                    this.mColor = 0;
                    LightsService.this.mH.sendMessageDelayed(Message.obtain(LightsService.this.mH, 1, this), (long) onMS);
                }
            }
        }

        public void turnOff() {
            synchronized (this) {
                setLightLocked(0, 0, 0, 0, 0);
            }
        }

        public void setVrMode(boolean enabled) {
            synchronized (this) {
                if (this.mVrModeEnabled != enabled) {
                    this.mVrModeEnabled = enabled;
                    this.mUseLowPersistenceForVR = LightsService.this.getVrDisplayMode() == 0;
                    if (shouldBeInLowPersistenceMode()) {
                        this.mLastBrightnessMode = this.mBrightnessMode;
                    }
                }
            }
        }

        private void stopFlashing() {
            synchronized (this) {
                setLightLocked(this.mColor, 0, 0, 0, 0);
            }
        }

        private void setLightLocked(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (shouldBeInLowPersistenceMode()) {
                brightnessMode = 2;
            } else if (brightnessMode == 2) {
                brightnessMode = this.mLastBrightnessMode;
            }
            if (!this.mInitialized || color != this.mColor || mode != this.mMode || onMS != this.mOnMS || offMS != this.mOffMS || this.mBrightnessMode != brightnessMode) {
                this.mInitialized = true;
                this.mLastColor = this.mColor;
                this.mColor = color;
                this.mMode = mode;
                this.mOnMS = onMS;
                this.mOffMS = offMS;
                this.mBrightnessMode = brightnessMode;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setLight(");
                stringBuilder.append(this.mId);
                stringBuilder.append(", 0x");
                stringBuilder.append(Integer.toHexString(color));
                stringBuilder.append(")");
                Trace.traceBegin(131072, stringBuilder.toString());
                try {
                    LightsService.setLight_native(this.mId, color, mode, onMS, offMS, brightnessMode);
                } finally {
                    Trace.traceEnd(131072);
                }
            }
        }

        private boolean shouldBeInLowPersistenceMode() {
            return this.mVrModeEnabled && this.mUseLowPersistenceForVR;
        }

        private void setLightLocked_10000stage(int color, int mode, int onMS, int offMS, int brightnessMode) {
            if (color != this.mColor || mode != this.mMode || onMS != this.mOnMS || offMS != this.mOffMS) {
                this.mColor = color;
                this.mMode = mode;
                this.mOnMS = onMS;
                this.mOffMS = offMS;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setLight(");
                stringBuilder.append(this.mId);
                stringBuilder.append(", ");
                stringBuilder.append(color);
                stringBuilder.append(")");
                Trace.traceBegin(131072, stringBuilder.toString());
                try {
                    LightsService.setLight_native(LightsManager.LIGHT_ID_BACKLIGHT_10000, color, mode, onMS, offMS, brightnessMode);
                } finally {
                    Trace.traceEnd(131072);
                }
            }
        }

        public int getDeviceActualBrightnessLevel() {
            return LightsService.this.getDeviceActualBrightnessLevelImpl();
        }

        public int getDeviceActualBrightnessNit() {
            return LightsService.this.getDeviceActualBrightnessNitImpl();
        }

        public int getDeviceStandardBrightnessNit() {
            return LightsService.this.getDeviceStandardBrightnessNitImpl();
        }
    }

    private static native void finalize_native();

    protected static native void refreshFrames_native();

    static native void setBackLightMaxLevel_native(int i);

    static native void setHighPrecisionFlag_native(long j, int i);

    static native void setLight_native(int i, int i2, int i3, int i4, int i5, int i6);

    public LightsService(Context context) {
        super(context);
        int i = 0;
        this.mIsHighPrecision = false;
        boolean z = true;
        this.mWriteAutoBrightnessDbEnable = true;
        this.mLimitedMaxBrightness = -1;
        this.mBrightnessConflict = false;
        if (SystemProperties.get("ro.config.blight_power_curve", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).length() <= 0) {
            z = false;
        }
        this.POWER_CURVE_BLIGHT_SUPPORT = z;
        this.mService = new LightsManager() {
            public Light getLight(int id) {
                if (id < 0 || id >= LightsManager.LIGHT_ID_COUNT) {
                    return null;
                }
                return LightsService.this.mLights[id];
            }
        };
        this.mH = new Handler() {
            public void handleMessage(Message msg) {
                msg.obj.stopFlashing();
            }
        };
        mHasShutDown = false;
        while (i < LightsManager.LIGHT_ID_COUNT) {
            this.mLights[i] = new LightImpl(this, i, null);
            i++;
        }
        getMaxBrightnessFromKerenl();
        setLight_native(3, 0, 0, 0, 0, 0);
    }

    public void onStart() {
        publishLocalService(LightsManager.class, this.mService);
    }

    public void onBootPhase(int phase) {
    }

    private int getVrDisplayMode() {
        return Secure.getIntForUser(getContext().getContentResolver(), "vr_display_mode", 0, ActivityManager.getCurrentUser());
    }

    protected void finalize() throws Throwable {
        finalize_native();
        super.finalize();
    }

    public void getMaxBrightnessFromKerenl() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File(MAX_BRIGHTNESS_PATH)));
            String readLine = reader.readLine();
            String tempString = readLine;
            if (readLine != null) {
                mMaxBrightnessFromKernel = Integer.parseInt(tempString);
            }
            try {
                reader.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e2) {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e3) {
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    public void sendSmartBackLightWithRefreshFramesImpl(int id, int enable, int level, int value, int frames, boolean setAfterRefresh, int enable2, int value2) {
    }

    public void sendSREWithRefreshFramesImpl(int id, int enable, int ambientLightThreshold, int ambientLight, int frames, boolean setAfterRefresh, int enable2, int ambientLight2) {
    }

    protected void sendUpdateaAutoBrightnessDbMsg() {
    }

    protected void updateBrightnessMode(boolean mode) {
    }

    protected int getLcdBrightnessMode() {
        return mLcdBrightness;
    }

    protected int mapIntoRealBacklightLevel(int level) {
        return level;
    }

    protected void updateCurrentUserId(int userId) {
    }

    public int getDeviceActualBrightnessLevelImpl() {
        return 0;
    }

    public int getDeviceActualBrightnessNitImpl() {
        return 0;
    }

    public int getDeviceStandardBrightnessNitImpl() {
        return 0;
    }

    protected int getNormalizedMaxBrightness() {
        return mMaxBrightnessFromKernel;
    }
}

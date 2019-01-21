package com.android.internal.app;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class ColorDisplayController {
    public static final int AUTO_MODE_CUSTOM = 1;
    public static final int AUTO_MODE_DISABLED = 0;
    public static final int AUTO_MODE_TWILIGHT = 2;
    public static final int COLOR_MODE_AUTOMATIC = 3;
    public static final int COLOR_MODE_BOOSTED = 1;
    public static final int COLOR_MODE_NATURAL = 0;
    public static final int COLOR_MODE_SATURATED = 2;
    private static final boolean DEBUG = false;
    private static final String TAG = "ColorDisplayController";
    private Callback mCallback;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private MetricsLogger mMetricsLogger;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AutoMode {
    }

    public interface Callback {
        void onActivated(boolean activated) {
        }

        void onAutoModeChanged(int autoMode) {
        }

        void onCustomStartTimeChanged(LocalTime startTime) {
        }

        void onCustomEndTimeChanged(LocalTime endTime) {
        }

        void onColorTemperatureChanged(int colorTemperature) {
        }

        void onDisplayColorModeChanged(int displayColorMode) {
        }

        void onAccessibilityTransformChanged(boolean state) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorMode {
    }

    public ColorDisplayController(Context context) {
        this(context, ActivityManager.getCurrentUser());
    }

    public ColorDisplayController(Context context, int userId) {
        this.mContext = context.getApplicationContext();
        this.mUserId = userId;
        this.mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String setting = uri == null ? null : uri.getLastPathSegment();
                if (setting != null) {
                    ColorDisplayController.this.onSettingChanged(setting);
                }
            }
        };
    }

    public boolean isActivated() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_activated", 0, this.mUserId) == 1;
    }

    public boolean setActivated(boolean activated) {
        if (isActivated() != activated) {
            Secure.putStringForUser(this.mContext.getContentResolver(), "night_display_last_activated_time", LocalDateTime.now().toString(), this.mUserId);
        }
        return Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_activated", activated, this.mUserId);
    }

    public LocalDateTime getLastActivatedTime() {
        String lastActivatedTime = Secure.getStringForUser(this.mContext.getContentResolver(), "night_display_last_activated_time", this.mUserId);
        if (lastActivatedTime != null) {
            try {
                return LocalDateTime.parse(lastActivatedTime);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastActivatedTime)), ZoneId.systemDefault());
                } catch (NumberFormatException | DateTimeException e2) {
                }
            }
        }
        return null;
    }

    public int getAutoMode() {
        int autoMode = Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_auto_mode", -1, this.mUserId);
        if (autoMode == -1) {
            autoMode = this.mContext.getResources().getInteger(17694765);
        }
        if (autoMode == 0 || autoMode == 1 || autoMode == 2) {
            return autoMode;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid autoMode: ");
        stringBuilder.append(autoMode);
        Slog.e(str, stringBuilder.toString());
        return 0;
    }

    public int getAutoModeRaw() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_auto_mode", -1, this.mUserId);
    }

    public boolean setAutoMode(int autoMode) {
        if (autoMode == 0 || autoMode == 1 || autoMode == 2) {
            if (getAutoMode() != autoMode) {
                Secure.putStringForUser(this.mContext.getContentResolver(), "night_display_last_activated_time", null, this.mUserId);
                getMetricsLogger().write(new LogMaker(MetricsEvent.ACTION_NIGHT_DISPLAY_AUTO_MODE_CHANGED).setType(4).setSubtype(autoMode));
            }
            return Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_auto_mode", autoMode, this.mUserId);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid autoMode: ");
        stringBuilder.append(autoMode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public LocalTime getCustomStartTime() {
        int startTimeValue = Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_custom_start_time", -1, this.mUserId);
        if (startTimeValue == -1) {
            startTimeValue = this.mContext.getResources().getInteger(17694767);
        }
        return LocalTime.ofSecondOfDay((long) (startTimeValue / 1000));
    }

    public boolean setCustomStartTime(LocalTime startTime) {
        if (startTime != null) {
            getMetricsLogger().write(new LogMaker(MetricsEvent.ACTION_NIGHT_DISPLAY_AUTO_MODE_CUSTOM_TIME_CHANGED).setType(4).setSubtype(0));
            return Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_custom_start_time", startTime.toSecondOfDay() * 1000, this.mUserId);
        }
        throw new IllegalArgumentException("startTime cannot be null");
    }

    public LocalTime getCustomEndTime() {
        int endTimeValue = Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_custom_end_time", -1, this.mUserId);
        if (endTimeValue == -1) {
            endTimeValue = this.mContext.getResources().getInteger(17694766);
        }
        return LocalTime.ofSecondOfDay((long) (endTimeValue / 1000));
    }

    public boolean setCustomEndTime(LocalTime endTime) {
        if (endTime != null) {
            getMetricsLogger().write(new LogMaker(MetricsEvent.ACTION_NIGHT_DISPLAY_AUTO_MODE_CUSTOM_TIME_CHANGED).setType(4).setSubtype(1));
            return Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_custom_end_time", endTime.toSecondOfDay() * 1000, this.mUserId);
        }
        throw new IllegalArgumentException("endTime cannot be null");
    }

    public int getColorTemperature() {
        int colorTemperature = Secure.getIntForUser(this.mContext.getContentResolver(), "night_display_color_temperature", -1, this.mUserId);
        if (colorTemperature == -1) {
            colorTemperature = getDefaultColorTemperature();
        }
        int minimumTemperature = getMinimumColorTemperature();
        int maximumTemperature = getMaximumColorTemperature();
        if (colorTemperature < minimumTemperature) {
            return minimumTemperature;
        }
        if (colorTemperature > maximumTemperature) {
            return maximumTemperature;
        }
        return colorTemperature;
    }

    public boolean setColorTemperature(int colorTemperature) {
        return Secure.putIntForUser(this.mContext.getContentResolver(), "night_display_color_temperature", colorTemperature, this.mUserId);
    }

    private int getCurrentColorModeFromSystemProperties() {
        int i = 0;
        int displayColorSetting = SystemProperties.getInt("persist.sys.sf.native_mode", 0);
        if (displayColorSetting == 0) {
            if (!"1.0".equals(SystemProperties.get("persist.sys.sf.color_saturation"))) {
                i = 1;
            }
            return i;
        } else if (displayColorSetting == 1) {
            return 2;
        } else {
            if (displayColorSetting == 2) {
                return 3;
            }
            return -1;
        }
    }

    private boolean isColorModeAvailable(int colorMode) {
        int[] availableColorModes = this.mContext.getResources().getIntArray(17235987);
        if (availableColorModes != null) {
            for (int mode : availableColorModes) {
                if (mode == colorMode) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getColorMode() {
        if (getAccessibilityTransformActivated()) {
            if (isColorModeAvailable(2)) {
                return 2;
            }
            if (isColorModeAvailable(3)) {
                return 3;
            }
        }
        int colorMode = System.getIntForUser(this.mContext.getContentResolver(), "display_color_mode", -1, this.mUserId);
        if (colorMode == -1) {
            colorMode = getCurrentColorModeFromSystemProperties();
        }
        if (!isColorModeAvailable(colorMode)) {
            if (colorMode == 1 && isColorModeAvailable(0)) {
                colorMode = 0;
            } else if (colorMode == 2 && isColorModeAvailable(3)) {
                colorMode = 3;
            } else if (colorMode == 3 && isColorModeAvailable(2)) {
                colorMode = 2;
            } else {
                colorMode = -1;
            }
        }
        return colorMode;
    }

    public void setColorMode(int colorMode) {
        if (isColorModeAvailable(colorMode)) {
            System.putIntForUser(this.mContext.getContentResolver(), "display_color_mode", colorMode, this.mUserId);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid colorMode: ");
        stringBuilder.append(colorMode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getMinimumColorTemperature() {
        return this.mContext.getResources().getInteger(17694835);
    }

    public int getMaximumColorTemperature() {
        return this.mContext.getResources().getInteger(17694834);
    }

    public int getDefaultColorTemperature() {
        return this.mContext.getResources().getInteger(17694833);
    }

    public boolean getAccessibilityTransformActivated() {
        ContentResolver cr = this.mContext.getContentResolver();
        if (Secure.getIntForUser(cr, "accessibility_display_inversion_enabled", 0, this.mUserId) == 1 || Secure.getIntForUser(cr, "accessibility_display_daltonizer_enabled", 0, this.mUserId) == 1) {
            return true;
        }
        return false;
    }

    private void onSettingChanged(String setting) {
        if (this.mCallback != null) {
            Object obj = -1;
            switch (setting.hashCode()) {
                case -2038150513:
                    if (setting.equals("night_display_auto_mode")) {
                        obj = 1;
                        break;
                    }
                    break;
                case -1761668069:
                    if (setting.equals("night_display_custom_end_time")) {
                        obj = 3;
                        break;
                    }
                    break;
                case -969458956:
                    if (setting.equals("night_display_color_temperature")) {
                        obj = 4;
                        break;
                    }
                    break;
                case -686921934:
                    if (setting.equals("accessibility_display_daltonizer_enabled")) {
                        obj = 7;
                        break;
                    }
                    break;
                case -551230169:
                    if (setting.equals("accessibility_display_inversion_enabled")) {
                        obj = 6;
                        break;
                    }
                    break;
                case 800115245:
                    if (setting.equals("night_display_activated")) {
                        obj = null;
                        break;
                    }
                    break;
                case 1561688220:
                    if (setting.equals("display_color_mode")) {
                        obj = 5;
                        break;
                    }
                    break;
                case 1578271348:
                    if (setting.equals("night_display_custom_start_time")) {
                        obj = 2;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    this.mCallback.onActivated(isActivated());
                    return;
                case 1:
                    this.mCallback.onAutoModeChanged(getAutoMode());
                    return;
                case 2:
                    this.mCallback.onCustomStartTimeChanged(getCustomStartTime());
                    return;
                case 3:
                    this.mCallback.onCustomEndTimeChanged(getCustomEndTime());
                    return;
                case 4:
                    this.mCallback.onColorTemperatureChanged(getColorTemperature());
                    return;
                case 5:
                    this.mCallback.onDisplayColorModeChanged(getColorMode());
                    return;
                case 6:
                case 7:
                    this.mCallback.onAccessibilityTransformChanged(getAccessibilityTransformActivated());
                    return;
                default:
                    return;
            }
        }
    }

    public void setListener(Callback callback) {
        Callback oldCallback = this.mCallback;
        if (oldCallback != callback) {
            this.mCallback = callback;
            if (callback == null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
            } else if (oldCallback == null) {
                ContentResolver cr = this.mContext.getContentResolver();
                cr.registerContentObserver(Secure.getUriFor("night_display_activated"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("night_display_auto_mode"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("night_display_custom_start_time"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("night_display_custom_end_time"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("night_display_color_temperature"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(System.getUriFor("display_color_mode"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("accessibility_display_inversion_enabled"), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor("accessibility_display_daltonizer_enabled"), false, this.mContentObserver, this.mUserId);
            }
        }
    }

    private MetricsLogger getMetricsLogger() {
        if (this.mMetricsLogger == null) {
            this.mMetricsLogger = new MetricsLogger();
        }
        return this.mMetricsLogger;
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(17956995);
    }
}

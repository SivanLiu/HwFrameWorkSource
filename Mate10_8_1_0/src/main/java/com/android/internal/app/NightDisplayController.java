package com.android.internal.app;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Slog;
import com.android.internal.R;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class NightDisplayController {
    public static final int AUTO_MODE_CUSTOM = 1;
    public static final int AUTO_MODE_DISABLED = 0;
    public static final int AUTO_MODE_TWILIGHT = 2;
    public static final int COLOR_MODE_BOOSTED = 1;
    public static final int COLOR_MODE_NATURAL = 0;
    public static final int COLOR_MODE_SATURATED = 2;
    private static final boolean DEBUG = false;
    private static final String PERSISTENT_PROPERTY_NATIVE_MODE = "persist.sys.sf.native_mode";
    private static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";
    private static final String TAG = "NightDisplayController";
    private Callback mCallback;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private final int mUserId;

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
    }

    public NightDisplayController(Context context) {
        this(context, ActivityManager.getCurrentUser());
    }

    public NightDisplayController(Context context, int userId) {
        this.mContext = context.getApplicationContext();
        this.mUserId = userId;
        this.mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String lastPathSegment = uri == null ? null : uri.getLastPathSegment();
                if (lastPathSegment != null) {
                    NightDisplayController.this.onSettingChanged(lastPathSegment);
                }
            }
        };
    }

    public boolean isActivated() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, 0, this.mUserId) == 1;
    }

    public boolean setActivated(boolean activated) {
        if (isActivated() != activated) {
            Secure.putStringForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, LocalDateTime.now().toString(), this.mUserId);
        }
        return Secure.putIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, activated ? 1 : 0, this.mUserId);
    }

    public LocalDateTime getLastActivatedTime() {
        String lastActivatedTime = Secure.getStringForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, this.mUserId);
        if (lastActivatedTime != null) {
            try {
                return LocalDateTime.parse(lastActivatedTime);
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastActivatedTime)), ZoneId.systemDefault());
                } catch (DateTimeException e2) {
                }
            }
        }
        return null;
    }

    public int getAutoMode() {
        int autoMode = Secure.getIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_AUTO_MODE, -1, this.mUserId);
        if (autoMode == -1) {
            autoMode = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayAutoMode);
        }
        if (autoMode == 0 || autoMode == 1 || autoMode == 2) {
            return autoMode;
        }
        Slog.e(TAG, "Invalid autoMode: " + autoMode);
        return 0;
    }

    public boolean setAutoMode(int autoMode) {
        if (autoMode == 0 || autoMode == 1 || autoMode == 2) {
            if (getAutoMode() != autoMode) {
                Secure.putStringForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_LAST_ACTIVATED_TIME, null, this.mUserId);
            }
            return Secure.putIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_AUTO_MODE, autoMode, this.mUserId);
        }
        throw new IllegalArgumentException("Invalid autoMode: " + autoMode);
    }

    public LocalTime getCustomStartTime() {
        int startTimeValue = Secure.getIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_CUSTOM_START_TIME, -1, this.mUserId);
        if (startTimeValue == -1) {
            startTimeValue = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayCustomStartTime);
        }
        return LocalTime.ofSecondOfDay((long) (startTimeValue / 1000));
    }

    public boolean setCustomStartTime(LocalTime startTime) {
        if (startTime != null) {
            return Secure.putIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_CUSTOM_START_TIME, startTime.toSecondOfDay() * 1000, this.mUserId);
        }
        throw new IllegalArgumentException("startTime cannot be null");
    }

    public LocalTime getCustomEndTime() {
        int endTimeValue = Secure.getIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_CUSTOM_END_TIME, -1, this.mUserId);
        if (endTimeValue == -1) {
            endTimeValue = this.mContext.getResources().getInteger(R.integer.config_defaultNightDisplayCustomEndTime);
        }
        return LocalTime.ofSecondOfDay((long) (endTimeValue / 1000));
    }

    public boolean setCustomEndTime(LocalTime endTime) {
        if (endTime != null) {
            return Secure.putIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_CUSTOM_END_TIME, endTime.toSecondOfDay() * 1000, this.mUserId);
        }
        throw new IllegalArgumentException("endTime cannot be null");
    }

    public int getColorTemperature() {
        int colorTemperature = Secure.getIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE, -1, this.mUserId);
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
        return Secure.putIntForUser(this.mContext.getContentResolver(), Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE, colorTemperature, this.mUserId);
    }

    public int getColorMode() {
        int i = 0;
        int colorMode = System.getIntForUser(this.mContext.getContentResolver(), System.DISPLAY_COLOR_MODE, -1, this.mUserId);
        if (colorMode >= 0 && colorMode <= 2) {
            return colorMode;
        }
        if ("1".equals(SystemProperties.get(PERSISTENT_PROPERTY_NATIVE_MODE))) {
            return 2;
        }
        if (!"1.0".equals(SystemProperties.get(PERSISTENT_PROPERTY_SATURATION))) {
            i = 1;
        }
        return i;
    }

    public void setColorMode(int colorMode) {
        if (colorMode >= 0 && colorMode <= 2) {
            System.putIntForUser(this.mContext.getContentResolver(), System.DISPLAY_COLOR_MODE, colorMode, this.mUserId);
        }
    }

    public int getMinimumColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureMin);
    }

    public int getMaximumColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureMax);
    }

    public int getDefaultColorTemperature() {
        return this.mContext.getResources().getInteger(R.integer.config_nightDisplayColorTemperatureDefault);
    }

    private void onSettingChanged(String setting) {
        if (this.mCallback == null) {
            return;
        }
        if (setting.equals(Secure.NIGHT_DISPLAY_ACTIVATED)) {
            this.mCallback.onActivated(isActivated());
        } else if (setting.equals(Secure.NIGHT_DISPLAY_AUTO_MODE)) {
            this.mCallback.onAutoModeChanged(getAutoMode());
        } else if (setting.equals(Secure.NIGHT_DISPLAY_CUSTOM_START_TIME)) {
            this.mCallback.onCustomStartTimeChanged(getCustomStartTime());
        } else if (setting.equals(Secure.NIGHT_DISPLAY_CUSTOM_END_TIME)) {
            this.mCallback.onCustomEndTimeChanged(getCustomEndTime());
        } else if (setting.equals(Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE)) {
            this.mCallback.onColorTemperatureChanged(getColorTemperature());
        } else if (setting.equals(System.DISPLAY_COLOR_MODE)) {
            this.mCallback.onDisplayColorModeChanged(getColorMode());
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
                cr.registerContentObserver(Secure.getUriFor(Secure.NIGHT_DISPLAY_ACTIVATED), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor(Secure.NIGHT_DISPLAY_AUTO_MODE), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor(Secure.NIGHT_DISPLAY_CUSTOM_START_TIME), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor(Secure.NIGHT_DISPLAY_CUSTOM_END_TIME), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(Secure.getUriFor(Secure.NIGHT_DISPLAY_COLOR_TEMPERATURE), false, this.mContentObserver, this.mUserId);
                cr.registerContentObserver(System.getUriFor(System.DISPLAY_COLOR_MODE), false, this.mContentObserver, this.mUserId);
            }
        }
    }

    public static boolean isAvailable(Context context) {
        return context.getResources().getBoolean(R.bool.config_nightDisplayAvailable);
    }
}

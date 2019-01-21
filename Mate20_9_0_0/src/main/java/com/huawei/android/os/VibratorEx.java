package com.huawei.android.os;

import android.app.ActivityThread;
import android.content.Context;
import android.os.Process;
import android.util.Log;

public class VibratorEx {
    public static final String HW_VIBRATOR_TPYE_ALLSCREEN_UPGLIDE_MULTITASK = "haptic.allscreen.upglide_multitask";
    public static final String HW_VIBRATOR_TPYE_BATTERY_CHARGING = "haptic.battery.charging";
    public static final String HW_VIBRATOR_TPYE_CAMERA_CLICK = "haptic.camera.click";
    public static final String HW_VIBRATOR_TPYE_CAMERA_FOCUS = "haptic.camera.focus";
    public static final String HW_VIBRATOR_TPYE_CAMERA_GEAR_SLIP = "haptic.camera.gear_slip";
    public static final String HW_VIBRATOR_TPYE_CAMERA_MODE_SWITCH = "haptic.camera.mode_switch";
    public static final String HW_VIBRATOR_TPYE_CAMERA_PORTRAIT_SWITCH = "haptic.camera.portrait_switch";
    public static final String HW_VIBRATOR_TPYE_CLOCK_STOPWATCH = "haptic.clock.stopwatch";
    public static final String HW_VIBRATOR_TPYE_CLOCK_TIMER = "haptic.clock.timer";
    public static final String HW_VIBRATOR_TPYE_CONTACTS_LETTERS_INDEX = "haptic.contacts.letters_index";
    public static final String HW_VIBRATOR_TPYE_CONTACTS_LIST_SCROLL = "haptic.contacts.list_scroll";
    public static final String HW_VIBRATOR_TPYE_CONTROL_DATE_SCROLL = "haptic.control.date_scroll";
    public static final String HW_VIBRATOR_TPYE_CONTROL_EDIT_SLIP = "haptic.control.edit_slip";
    public static final String HW_VIBRATOR_TPYE_CONTROL_LETTERS_SCROLL = "haptic.control.letters_scroll";
    public static final String HW_VIBRATOR_TPYE_CONTROL_TIME_SCROLL = "haptic.control.time_scroll";
    public static final String HW_VIBRATOR_TPYE_DESKTOP_LONG_PRESS = "haptic.desktop.long_press";
    public static final String HW_VIBRATOR_TPYE_DIALLER_CLICK = "haptic.dialler.click";
    public static final String HW_VIBRATOR_TPYE_DIALLER_LONG_PRESS = "haptic.dialler.long_press";
    public static final String HW_VIBRATOR_TPYE_FINGERPRINT_ENTERING = "haptic.fingerprint.entering";
    public static final String HW_VIBRATOR_TPYE_FINGERPRINT_LIFT = "haptic.fingerprint.lift";
    public static final String HW_VIBRATOR_TPYE_FINGERPRINT_LONG_PRESS = "haptic.fingerprint.long_press";
    public static final String HW_VIBRATOR_TPYE_FINGERPRINT_UNLOCK_FAIL = "haptic.fingerprint.unlock_fail";
    public static final String HW_VIBRATOR_TPYE_GAME_SHOOTING = "haptic.game.shooting";
    public static final String HW_VIBRATOR_TPYE_HIVOICE_CLICK = "haptic.hivoice.click";
    public static final String HW_VIBRATOR_TPYE_LOCKSCREEN_UNLOCK_CLICK = "haptic.lockscreen.unlock_click";
    public static final String HW_VIBRATOR_TPYE_LOCKSCREEN_UNLOCK_SLIP = "haptic.lockscreen.unlock_slip";
    public static final String HW_VIBRATOR_TPYE_NULL = "haptic.null";
    public static final String HW_VIBRATOR_TPYE_VIRTUALNAVIGATION_CLICK_BACK = "haptic.virtual_navigation.click_back";
    public static final String HW_VIBRATOR_TPYE_VIRTUALNAVIGATION_CLICK_HOME = "haptic.virtual_navigation.click_home";
    public static final String HW_VIBRATOR_TPYE_VIRTUALNAVIGATION_CLICK_MULTITASK = "haptic.virtual_navigation.click_multitask";
    public static final String HW_VIBRATOR_TPYE_VIRTUALNAVIGATION_LONGPRESS_HOME = "haptic.virtual_navigation.long_press";
    public static final String HW_VIBRATOR_TPYE_WALLET_TIME_SCROLL = "haptic.wallet.time_scroll";
    public static final String HW_VIBRATOR_TPYE_WEATHER_RAIN = "haptic.weather.rain";
    public static final String HW_VIBRATOR_TPYE_WEATHER_THUNDER = "haptic.weather.thunder";
    private static final String TAG = "VibratorEx";
    private final String mPackageName;

    public VibratorEx() {
        this.mPackageName = ActivityThread.currentPackageName();
    }

    protected VibratorEx(Context context) {
        this.mPackageName = context.getOpPackageName();
    }

    public boolean isSupportHwVibrator(String type) {
        if (type != null && !type.equals("")) {
            return HwVibrator.isSupportHwVibrator(type);
        }
        Log.w(TAG, "can not set an empty vibrator type");
        return false;
    }

    public void setHwVibrator(String type) {
        if (type == null || type.equals("")) {
            Log.w(TAG, "can not set an empty vibrator type");
        } else {
            HwVibrator.setHwVibrator(Process.myUid(), this.mPackageName, type);
        }
    }

    public void stopHwVibrator(String type) {
        if (type == null || type.equals("")) {
            Log.w(TAG, "can not set an empty vibrator type");
        } else {
            HwVibrator.stopHwVibrator(Process.myUid(), this.mPackageName, type);
        }
    }

    public void setHwParameter(String command) {
        if (command == null || command.equals("")) {
            Log.w(TAG, "can not set an empty vibrator command");
        } else {
            HwVibrator.setHwParameter(command);
        }
    }

    public String getHwParameter(String command) {
        if (command != null && !command.equals("")) {
            return HwVibrator.getHwParameter(command);
        }
        Log.w(TAG, "can not set an empty vibrator command");
        return null;
    }
}

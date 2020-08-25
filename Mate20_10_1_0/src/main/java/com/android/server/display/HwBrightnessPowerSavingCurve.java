package com.android.server.display;

import android.graphics.PointF;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

public class HwBrightnessPowerSavingCurve {
    private static final float DEFAULT_POWERSAVING_RATIO = 1.0f;
    private static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    private static final boolean HWFLOW;
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final String TAG = "HwBrightnessPowerSavingCurve";
    private int mMaxBrightnesIndoor = 255;
    List<PointF> mPowerSavingBrighnessLinePointsList = null;
    private boolean mPowerSavingEnable = false;
    private float mScreenBrightnessMaxNit = 530.0f;
    private float mScreenBrightnessMinNit = 2.0f;

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        HWFLOW = z;
    }

    public HwBrightnessPowerSavingCurve(int maxBrightnesIndoor, float screenBrightnessMinNit, float screenBrightnessMaxNit) {
        this.mMaxBrightnesIndoor = maxBrightnesIndoor;
        this.mScreenBrightnessMinNit = screenBrightnessMinNit;
        this.mScreenBrightnessMaxNit = screenBrightnessMaxNit;
        parsePowerSavingCure(SystemProperties.get("ro.config.blight_power_curve", ""));
    }

    private void parsePowerSavingCure(String powerSavingCure) {
        if (powerSavingCure == null || powerSavingCure.length() <= 0) {
            Slog.i(TAG, "powerSavingCure == null");
            return;
        }
        List<PointF> list = this.mPowerSavingBrighnessLinePointsList;
        if (list != null) {
            list.clear();
        } else {
            this.mPowerSavingBrighnessLinePointsList = new ArrayList();
        }
        String[] powerSavingPoints = powerSavingCure.split(";");
        int i = 0;
        while (i < powerSavingPoints.length) {
            try {
                String[] point = powerSavingPoints[i].split(",");
                this.mPowerSavingBrighnessLinePointsList.add(new PointF(Float.parseFloat(point[0]), Float.parseFloat(point[1])));
                i++;
            } catch (NumberFormatException e) {
                this.mPowerSavingBrighnessLinePointsList.clear();
                Slog.w(TAG, "parse PowerSaving curve error");
                return;
            }
        }
        List<PointF> list2 = this.mPowerSavingBrighnessLinePointsList;
        if (list2 != null) {
            int listSize = list2.size();
            for (int i2 = 0; i2 < listSize; i2++) {
                PointF pointTmp = this.mPowerSavingBrighnessLinePointsList.get(i2);
                if (HWFLOW) {
                    Slog.i(TAG, "PowerSavingPointsList x = " + pointTmp.x + ", y = " + pointTmp.y);
                }
            }
        }
    }

    public int getPowerSavingBrightness(int brightness) {
        float powerRatio = covertBrightnessToPowerRatio(brightness);
        int tembrightness = (int) (((float) brightness) * powerRatio);
        if (HWFLOW && tembrightness != brightness) {
            Slog.i(TAG, "NewCurveModePowerSaving  tembrightness=" + tembrightness + ",brightness=" + brightness + ",ratio=" + powerRatio);
        }
        return tembrightness;
    }

    public void setPowerSavingEnable(boolean powerSavingEnable) {
        if (powerSavingEnable != this.mPowerSavingEnable) {
            this.mPowerSavingEnable = powerSavingEnable;
            if (HWFLOW) {
                Slog.i(TAG, "PowerSaving mPowerSavingEnable=" + this.mPowerSavingEnable);
            }
        }
    }

    private float covertBrightnessToPowerRatio(int brightness) {
        if (brightness >= this.mMaxBrightnesIndoor) {
            return 1.0f;
        }
        int brightnessNit = covertBrightnessLevelToNit(brightness);
        if (this.mPowerSavingEnable) {
            return getPowerSavingRatio(brightnessNit);
        }
        return 1.0f;
    }

    private int covertBrightnessLevelToNit(int brightness) {
        if (brightness == 0) {
            return brightness;
        }
        if (brightness < 4) {
            brightness = 4;
        }
        if (brightness > 255) {
            brightness = 255;
        }
        float f = this.mScreenBrightnessMaxNit;
        float f2 = this.mScreenBrightnessMinNit;
        return (int) (((((float) (brightness - 4)) * (f - f2)) / 251.0f) + f2);
    }

    private float getPowerSavingRatio(int brightnssnit) {
        List<PointF> list = this.mPowerSavingBrighnessLinePointsList;
        if (list == null || list.size() == 0 || brightnssnit < 0) {
            Slog.e(TAG, "PowerSavingBrighnessLinePointsList warning,set PowerSavingRatio,brightnssnit=" + brightnssnit);
            return 1.0f;
        }
        int linePointsListLength = this.mPowerSavingBrighnessLinePointsList.size();
        if (((float) brightnssnit) < this.mPowerSavingBrighnessLinePointsList.get(0).x) {
            return 1.0f;
        }
        PointF temp1 = null;
        float tmpPowerSavingRatio = 1.0f;
        int i = 0;
        while (true) {
            if (i >= linePointsListLength) {
                break;
            }
            PointF temp = this.mPowerSavingBrighnessLinePointsList.get(i);
            if (temp1 == null) {
                temp1 = temp;
            }
            if (((float) brightnssnit) >= temp.x) {
                temp1 = temp;
                tmpPowerSavingRatio = temp1.y;
                i++;
            } else if (temp.x <= temp1.x) {
                tmpPowerSavingRatio = 1.0f;
                Slog.w(TAG, "temp2[0] <= temp1[0] warning,set default tmpPowerSavingRatio");
            } else {
                tmpPowerSavingRatio = (((temp.y - temp1.y) / (temp.x - temp1.x)) * (((float) brightnssnit) - temp1.x)) + temp1.y;
            }
        }
        if (tmpPowerSavingRatio <= 1.0f && tmpPowerSavingRatio >= 0.0f) {
            return tmpPowerSavingRatio;
        }
        Slog.w(TAG, "tmpPowerSavingRatio warning,set default value");
        return 1.0f;
    }
}

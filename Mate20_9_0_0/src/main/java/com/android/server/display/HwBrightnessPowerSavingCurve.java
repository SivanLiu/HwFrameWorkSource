package com.android.server.display;

import android.graphics.PointF;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.gesture.GestureNavConst;
import java.util.ArrayList;
import java.util.List;

public class HwBrightnessPowerSavingCurve {
    private static final float DEFAULT_POWERSAVING_RATIO = 1.0f;
    private static final boolean HWDEBUG;
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
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
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
        if (this.mPowerSavingBrighnessLinePointsList != null) {
            this.mPowerSavingBrighnessLinePointsList.clear();
        } else {
            this.mPowerSavingBrighnessLinePointsList = new ArrayList();
        }
        String[] powerSavingPoints = powerSavingCure.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        int i = 0;
        int i2 = 0;
        while (i2 < powerSavingPoints.length) {
            try {
                String[] point = powerSavingPoints[i2].split(",");
                this.mPowerSavingBrighnessLinePointsList.add(new PointF(Float.parseFloat(point[0]), Float.parseFloat(point[1])));
                i2++;
            } catch (NumberFormatException e) {
                this.mPowerSavingBrighnessLinePointsList.clear();
                Slog.w(TAG, "parse PowerSaving curve error");
                return;
            }
        }
        if (this.mPowerSavingBrighnessLinePointsList != null) {
            int listSize = this.mPowerSavingBrighnessLinePointsList.size();
            while (i < listSize) {
                PointF pointTmp = (PointF) this.mPowerSavingBrighnessLinePointsList.get(i);
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PowerSavingPointsList x = ");
                    stringBuilder.append(pointTmp.x);
                    stringBuilder.append(", y = ");
                    stringBuilder.append(pointTmp.y);
                    Slog.d(str, stringBuilder.toString());
                }
                i++;
            }
        }
    }

    public int getPowerSavingBrightness(int brightness) {
        float powerRatio = covertBrightnessToPowerRatio(brightness);
        int tembrightness = (int) (((float) brightness) * powerRatio);
        if (HWFLOW && tembrightness != brightness) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NewCurveModePowerSaving  tembrightness=");
            stringBuilder.append(tembrightness);
            stringBuilder.append(",brightness=");
            stringBuilder.append(brightness);
            stringBuilder.append(",ratio=");
            stringBuilder.append(powerRatio);
            Slog.i(str, stringBuilder.toString());
        }
        return tembrightness;
    }

    public void setPowerSavingEnable(boolean powerSavingEnable) {
        if (powerSavingEnable != this.mPowerSavingEnable) {
            this.mPowerSavingEnable = powerSavingEnable;
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PowerSaving mPowerSavingEnable=");
                stringBuilder.append(this.mPowerSavingEnable);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    private float covertBrightnessToPowerRatio(int brightness) {
        if (brightness >= this.mMaxBrightnesIndoor) {
            return 1.0f;
        }
        int brightnessNit = covertBrightnessLevelToNit(brightness);
        float powerRatio = 1.0f;
        if (this.mPowerSavingEnable) {
            powerRatio = getPowerSavingRatio(brightnessNit);
        }
        return powerRatio;
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
        return (int) (((((float) (brightness - 4)) * (this.mScreenBrightnessMaxNit - this.mScreenBrightnessMinNit)) / 251.0f) + this.mScreenBrightnessMinNit);
    }

    private float getPowerSavingRatio(int brightnssnit) {
        if (this.mPowerSavingBrighnessLinePointsList == null || this.mPowerSavingBrighnessLinePointsList.size() == 0 || brightnssnit < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PowerSavingBrighnessLinePointsList warning,set PowerSavingRatio,brightnssnit=");
            stringBuilder.append(brightnssnit);
            Slog.e(str, stringBuilder.toString());
            return 1.0f;
        }
        int linePointsListLength = this.mPowerSavingBrighnessLinePointsList.size();
        int i = 0;
        if (((float) brightnssnit) < ((PointF) this.mPowerSavingBrighnessLinePointsList.get(0)).x) {
            return 1.0f;
        }
        PointF temp1 = null;
        float tmpPowerSavingRatio = 1.0f;
        while (i < linePointsListLength) {
            PointF temp = (PointF) this.mPowerSavingBrighnessLinePointsList.get(i);
            if (temp1 == null) {
                temp1 = temp;
            }
            if (((float) brightnssnit) < temp.x) {
                PointF temp2 = temp;
                if (temp2.x <= temp1.x) {
                    tmpPowerSavingRatio = 1.0f;
                    Slog.w(TAG, "temp2[0] <= temp1[0] warning,set default tmpPowerSavingRatio");
                } else {
                    tmpPowerSavingRatio = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (((float) brightnssnit) - temp1.x)) + temp1.y;
                }
                if (tmpPowerSavingRatio > 1.0f || tmpPowerSavingRatio < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    Slog.w(TAG, "tmpPowerSavingRatio warning,set default value");
                    tmpPowerSavingRatio = 1.0f;
                }
                return tmpPowerSavingRatio;
            }
            temp1 = temp;
            tmpPowerSavingRatio = temp1.y;
            i++;
        }
        Slog.w(TAG, "tmpPowerSavingRatio warning,set default value");
        tmpPowerSavingRatio = 1.0f;
        return tmpPowerSavingRatio;
    }
}

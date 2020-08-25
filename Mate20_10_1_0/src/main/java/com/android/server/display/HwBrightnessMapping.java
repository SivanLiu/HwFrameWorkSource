package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader;
import java.util.Iterator;
import java.util.List;

public class HwBrightnessMapping {
    private static final int DEFAULT_VALUE = 0;
    private static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    private static final boolean HWFLOW;
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final String TAG = "HwBrightnessMapping";
    private final HwBrightnessXmlLoader.Data mData = HwBrightnessXmlLoader.getData();
    private List<PointF> mMappinglinePointsList;

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        HWFLOW = z;
    }

    public HwBrightnessMapping(List<PointF> linePointsList) {
        this.mMappinglinePointsList = linePointsList;
    }

    public int getMappingBrightnessForRealNit(int level) {
        List<PointF> list = this.mMappinglinePointsList;
        if (list == null || level < 4) {
            Slog.w(TAG, "mMappinglinePointsList == null || level<min,level=" + level);
            return level;
        }
        float mappingBrightness = (float) level;
        PointF temp1 = null;
        Iterator iter = list.iterator();
        while (true) {
            if (!iter.hasNext()) {
                break;
            }
            PointF temp = iter.next();
            if (temp1 == null) {
                temp1 = temp;
            }
            if (((float) level) >= temp.x) {
                temp1 = temp;
                mappingBrightness = temp1.y;
            } else if (temp.x <= temp1.x) {
                mappingBrightness = (float) level;
                if (HWFLOW) {
                    Slog.d(TAG, "mappingBrightness_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                }
            } else {
                mappingBrightness = (((temp.y - temp1.y) / (temp.x - temp1.x)) * (((float) level) - temp1.x)) + temp1.y;
            }
        }
        return (int) (0.5f + mappingBrightness);
    }

    public int convertBrightnessLevelToNit(int level) {
        float brightnessNitTmp;
        if (level <= 0) {
            return 0;
        }
        if (this.mData.brightnessLevelToNitMappingEnable) {
            brightnessNitTmp = convertBrightnessLevelToNitInternel(this.mData.brightnessLevelToNitLinePoints, (float) level);
        } else {
            brightnessNitTmp = ((((float) (level - 4)) * (this.mData.screenBrightnessMaxNit - this.mData.screenBrightnessMinNit)) / 251.0f) + this.mData.screenBrightnessMinNit;
        }
        if (brightnessNitTmp < this.mData.screenBrightnessMinNit) {
            brightnessNitTmp = this.mData.screenBrightnessMinNit;
        }
        if (brightnessNitTmp > this.mData.screenBrightnessMaxNit) {
            brightnessNitTmp = this.mData.screenBrightnessMaxNit;
        }
        if (HWDEBUG) {
            Slog.d(TAG, "LevelToNit,level=" + level + ",nit=" + brightnessNitTmp);
        }
        return (int) (0.5f + brightnessNitTmp);
    }

    public int convertBrightnessNitToLevel(int brightnessNit) {
        float brightnessLevelTmp;
        if (brightnessNit <= 0) {
            return 0;
        }
        if (this.mData.brightnessLevelToNitMappingEnable) {
            brightnessLevelTmp = convertBrightnessNitToLevelInternel(this.mData.brightnessLevelToNitLinePoints, (float) brightnessNit);
        } else {
            brightnessLevelTmp = (((((float) brightnessNit) - this.mData.screenBrightnessMinNit) * 251.0f) / (this.mData.screenBrightnessMaxNit - this.mData.screenBrightnessMinNit)) + 4.0f;
        }
        if (brightnessLevelTmp < 4.0f) {
            brightnessLevelTmp = 4.0f;
        }
        if (brightnessLevelTmp > 255.0f) {
            brightnessLevelTmp = 255.0f;
        }
        if (HWDEBUG) {
            Slog.d(TAG, "NitToLevel,brightnessLevelTmp=" + brightnessLevelTmp + ",brightnessNit=" + brightnessNit);
        }
        return (int) (0.5f + brightnessLevelTmp);
    }

    private float convertBrightnessLevelToNitInternel(List<PointF> linePoints, float brightness) {
        if (linePoints == null || linePoints.size() == 0 || brightness <= 0.0f) {
            return 0.0f;
        }
        float brightnessNitTmp = 0.0f;
        PointF temp1 = null;
        Iterator<PointF> it = linePoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PointF pointItem = it.next();
            if (temp1 == null) {
                temp1 = pointItem;
            }
            if (brightness >= pointItem.x) {
                temp1 = pointItem;
                brightnessNitTmp = temp1.y;
            } else if (pointItem.x <= temp1.x) {
                brightnessNitTmp = 0.0f;
                Slog.w(TAG, "LevelToNit,brightnessNitTmpdefault=" + 0.0f + ",for_temp1.x <= temp2.x,x" + pointItem.x + ", y = " + pointItem.y);
            } else {
                brightnessNitTmp = (((pointItem.y - temp1.y) / (pointItem.x - temp1.x)) * (brightness - temp1.x)) + temp1.y;
            }
        }
        if (HWDEBUG) {
            Slog.d(TAG, "LevelToNit,brightness=" + brightness + ",TobrightnessNitTmp=" + brightnessNitTmp);
        }
        return brightnessNitTmp;
    }

    private float convertBrightnessNitToLevelInternel(List<PointF> linePoints, float brightnessNit) {
        if (linePoints == null || linePoints.size() == 0 || brightnessNit <= 0.0f) {
            return 0.0f;
        }
        float brightnessLevel = 0.0f;
        PointF temp1 = null;
        Iterator<PointF> it = linePoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PointF pointItem = it.next();
            if (temp1 == null) {
                temp1 = pointItem;
            }
            if (brightnessNit >= pointItem.y) {
                temp1 = pointItem;
                brightnessLevel = temp1.x;
            } else if (pointItem.y <= temp1.y) {
                brightnessLevel = 0.0f;
                Slog.w(TAG, "NitToLevel,brightnessLevel=" + 0.0f + ",for_temp1.y <= temp2.y,x" + pointItem.x + ", y = " + pointItem.y);
            } else {
                brightnessLevel = (((pointItem.x - temp1.x) / (pointItem.y - temp1.y)) * (brightnessNit - temp1.y)) + temp1.x;
            }
        }
        if (HWDEBUG) {
            Slog.d(TAG, "NitToLevel,brightnessLevel=" + brightnessLevel + ",brightnessNit=" + brightnessNit);
        }
        return brightnessLevel;
    }
}

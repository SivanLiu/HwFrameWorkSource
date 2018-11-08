package com.android.server.display;

import android.graphics.PointF;
import android.util.Log;
import android.util.Slog;
import java.util.List;

public class HwBrightnessMapping {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final String TAG = "HwBrightnessMapping";
    private List<PointF> mMappinglinePointsList;

    static {
        boolean z = true;
        boolean isLoggable = !Log.HWLog ? Log.HWModuleLog ? Log.isLoggable(TAG, 3) : false : true;
        HWDEBUG = isLoggable;
        if (!Log.HWINFO) {
            z = Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false;
        }
        HWFLOW = z;
    }

    public HwBrightnessMapping(List<PointF> linePointsList) {
        this.mMappinglinePointsList = linePointsList;
    }

    public int getMappingBrightnessForRealNit(int level) {
        if (this.mMappinglinePointsList == null || level < 4) {
            Slog.w(TAG, "mMappinglinePointsList == null || level<min,level=" + level);
            return level;
        }
        float mappingBrightness = (float) level;
        PointF temp1 = null;
        for (PointF temp : this.mMappinglinePointsList) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (((float) level) < temp.x) {
                PointF temp2 = temp;
                if (temp.x <= temp1.x) {
                    mappingBrightness = (float) level;
                    if (HWFLOW) {
                        Slog.d(TAG, "mappingBrightness_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                    }
                } else {
                    mappingBrightness = (((temp.y - temp1.y) / (temp.x - temp1.x)) * (((float) level) - temp1.x)) + temp1.y;
                }
                return (int) (0.5f + mappingBrightness);
            }
            temp1 = temp;
            mappingBrightness = temp.y;
        }
        return (int) (0.5f + mappingBrightness);
    }
}

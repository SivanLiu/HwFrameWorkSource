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
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public HwBrightnessMapping(List<PointF> linePointsList) {
        this.mMappinglinePointsList = linePointsList;
    }

    public int getMappingBrightnessForRealNit(int level) {
        if (this.mMappinglinePointsList == null || level < 4) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mMappinglinePointsList == null || level<min,level=");
            stringBuilder.append(level);
            Slog.w(str, stringBuilder.toString());
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
                if (temp2.x <= temp1.x) {
                    mappingBrightness = (float) level;
                    if (HWFLOW) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mappingBrightness_temp1.x <= temp2.x,x");
                        stringBuilder2.append(temp.x);
                        stringBuilder2.append(", y = ");
                        stringBuilder2.append(temp.y);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                } else {
                    mappingBrightness = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (((float) level) - temp1.x)) + temp1.y;
                }
                return (int) (0.5f + mappingBrightness);
            }
            temp1 = temp;
            mappingBrightness = temp1.y;
        }
        return (int) (0.5f + mappingBrightness);
    }
}

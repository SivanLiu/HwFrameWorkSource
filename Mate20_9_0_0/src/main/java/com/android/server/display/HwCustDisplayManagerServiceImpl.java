package com.android.server.display;

import android.os.SystemProperties;
import android.util.Slog;

public class HwCustDisplayManagerServiceImpl extends HwCustDisplayManagerService {
    public static final int DISPLAY_LOW_POWER_LEVEL_FHD = 0;
    public static final int DISPLAY_LOW_POWER_LEVEL_HD = 1;
    public static final int MAX_LOW_POWER_DISPLAY_LEVEL = 1;
    private static final String TAG = "HwCustDisplayManagerServiceImpl";
    private static final boolean isRogSupported = SystemProperties.getBoolean("ro.config.ROG", false);
    private int mLowPowerDisplayLevel = SystemProperties.getInt("persist.sys.res.level", 0);

    public void setLowPowerDisplayLevel(int level) {
        if (isRogSupported) {
            Integer lowResLevel = Integer.valueOf(level);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLowPowerDisplayLevel level = ");
            stringBuilder.append(level);
            Slog.i(str, stringBuilder.toString());
            if (level != this.mLowPowerDisplayLevel) {
                if (level < 0 || level > 1) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("set lowpower display level failed, invalid value:");
                    stringBuilder.append(level);
                    Slog.e(str, stringBuilder.toString());
                    return;
                }
                SystemProperties.set("persist.sys.res.level", lowResLevel.toString());
                if (level == 1) {
                    SystemProperties.set("persist.sys.res.icon_size", "112");
                } else {
                    SystemProperties.set("persist.sys.res.icon_size", "-1");
                }
                this.mLowPowerDisplayLevel = level;
            }
        }
    }

    public int getLowPowerDisplayLevel() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLowPowerDisplayLevel level = ");
        stringBuilder.append(this.mLowPowerDisplayLevel);
        Slog.i(str, stringBuilder.toString());
        return this.mLowPowerDisplayLevel;
    }
}

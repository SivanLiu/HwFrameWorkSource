package android.support.v4.content.res;

import android.content.res.Resources;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;

public final class ConfigurationHelper {
    private ConfigurationHelper() {
    }

    public static int getDensityDpi(@NonNull Resources resources) {
        if (VERSION.SDK_INT >= 17) {
            return resources.getConfiguration().densityDpi;
        }
        return resources.getDisplayMetrics().densityDpi;
    }
}

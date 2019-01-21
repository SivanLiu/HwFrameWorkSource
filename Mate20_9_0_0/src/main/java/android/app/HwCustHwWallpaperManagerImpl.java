package android.app;

import android.content.Context;
import android.provider.Settings.System;

public class HwCustHwWallpaperManagerImpl extends HwCustHwWallpaperManager {
    public boolean isScrollWallpaper(int max, int min, int width, int height, Context context) {
        if ("true".equals(System.getString(context.getContentResolver(), "isScrollWallpaper")) && width == 2 * min && height == max) {
            return true;
        }
        return false;
    }
}

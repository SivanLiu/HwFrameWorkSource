package android.common;

import android.app.Application;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public interface HwActivityThread {
    void changeToSpecialModel(String str);

    boolean decodeBitmapOptEnable();

    Drawable getCacheDrawableFromAware(int i, Resources resources, int i2, AssetManager assetManager);

    String getWechatScanActivity();

    boolean getWechatScanOpt();

    void hitDrawableCache(int i);

    boolean isLiteSysLoadEnable();

    int isPerfOptEnable(int i);

    boolean isScene(int i);

    void postCacheDrawableToAware(int i, Resources resources, long j, int i2, AssetManager assetManager);

    void reportBindApplicationToAware(Application application, String str);
}

package com.huawei.iimagekit.shadow;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class NoneShadow {
    public static void doBlur(Bitmap bitmapForBlur, Bitmap blurredBitmap, int radius) {
        blurredBitmap.copy(Bitmap.createBitmap(bitmapForBlur.getWidth(), bitmapForBlur.getHeight(), Config.ARGB_8888).getConfig(), true);
    }
}

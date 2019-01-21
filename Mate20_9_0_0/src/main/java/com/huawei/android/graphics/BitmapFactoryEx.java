package com.huawei.android.graphics;

import android.graphics.BitmapFactory.Options;

public class BitmapFactoryEx {
    public static void setInThumbnailMode(Options options, boolean inThumbnailMode) {
        if (options != null) {
            options.inThumbnailMode = inThumbnailMode;
        }
    }
}

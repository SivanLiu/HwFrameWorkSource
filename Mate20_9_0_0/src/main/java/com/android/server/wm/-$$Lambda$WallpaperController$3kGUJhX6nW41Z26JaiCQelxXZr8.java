package com.android.server.wm;

import com.android.internal.util.ToBooleanFunction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperController$3kGUJhX6nW41Z26JaiCQelxXZr8 implements ToBooleanFunction {
    private final /* synthetic */ WallpaperController f$0;

    public /* synthetic */ -$$Lambda$WallpaperController$3kGUJhX6nW41Z26JaiCQelxXZr8(WallpaperController wallpaperController) {
        this.f$0 = wallpaperController;
    }

    public final boolean apply(Object obj) {
        return WallpaperController.lambda$getTopVisibleWallpaper$2(this.f$0, (WindowState) obj);
    }
}

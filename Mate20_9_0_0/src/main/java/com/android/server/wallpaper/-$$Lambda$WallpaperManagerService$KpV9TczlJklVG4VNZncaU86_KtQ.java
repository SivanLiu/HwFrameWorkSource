package com.android.server.wallpaper;

import com.android.server.wallpaper.WallpaperManagerService.WallpaperData;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperManagerService$KpV9TczlJklVG4VNZncaU86_KtQ implements Runnable {
    private final /* synthetic */ WallpaperManagerService f$0;
    private final /* synthetic */ WallpaperData f$1;
    private final /* synthetic */ WallpaperData f$2;

    public /* synthetic */ -$$Lambda$WallpaperManagerService$KpV9TczlJklVG4VNZncaU86_KtQ(WallpaperManagerService wallpaperManagerService, WallpaperData wallpaperData, WallpaperData wallpaperData2) {
        this.f$0 = wallpaperManagerService;
        this.f$1 = wallpaperData;
        this.f$2 = wallpaperData2;
    }

    public final void run() {
        WallpaperManagerService.lambda$switchUser$0(this.f$0, this.f$1, this.f$2);
    }
}

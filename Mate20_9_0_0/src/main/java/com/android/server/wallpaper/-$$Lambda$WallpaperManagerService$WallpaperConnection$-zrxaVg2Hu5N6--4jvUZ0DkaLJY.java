package com.android.server.wallpaper;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperManagerService$WallpaperConnection$-zrxaVg2Hu5N6--4jvUZ0DkaLJY implements Runnable {
    private final /* synthetic */ WallpaperConnection f$0;

    public /* synthetic */ -$$Lambda$WallpaperManagerService$WallpaperConnection$-zrxaVg2Hu5N6--4jvUZ0DkaLJY(WallpaperConnection wallpaperConnection) {
        this.f$0 = wallpaperConnection;
    }

    public final void run() {
        this.f$0.processDisconnect(this.f$0);
    }
}

package android.app;

import android.util.Pair;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperManager$Globals$1AcnQUORvPlCjJoNqdxfQT4o4Nw implements Runnable {
    private final /* synthetic */ Globals f$0;
    private final /* synthetic */ Pair f$1;
    private final /* synthetic */ WallpaperColors f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;

    public /* synthetic */ -$$Lambda$WallpaperManager$Globals$1AcnQUORvPlCjJoNqdxfQT4o4Nw(Globals globals, Pair pair, WallpaperColors wallpaperColors, int i, int i2) {
        this.f$0 = globals;
        this.f$1 = pair;
        this.f$2 = wallpaperColors;
        this.f$3 = i;
        this.f$4 = i2;
    }

    public final void run() {
        Globals.lambda$onWallpaperColorsChanged$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4);
    }
}

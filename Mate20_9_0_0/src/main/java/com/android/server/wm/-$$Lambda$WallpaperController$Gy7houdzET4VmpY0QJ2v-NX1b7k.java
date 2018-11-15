package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperController$Gy7houdzET4VmpY0QJ2v-NX1b7k implements Predicate {
    private final /* synthetic */ WindowState f$0;

    public /* synthetic */ -$$Lambda$WallpaperController$Gy7houdzET4VmpY0QJ2v-NX1b7k(WindowState windowState) {
        this.f$0 = windowState;
    }

    public final boolean test(Object obj) {
        return WallpaperController.lambda$updateWallpaperWindowsTarget$1(this.f$0, (WindowState) obj);
    }
}

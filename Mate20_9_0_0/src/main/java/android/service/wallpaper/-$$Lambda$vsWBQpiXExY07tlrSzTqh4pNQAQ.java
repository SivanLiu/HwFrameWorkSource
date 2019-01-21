package android.service.wallpaper;

import android.service.wallpaper.WallpaperService.Engine;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$vsWBQpiXExY07tlrSzTqh4pNQAQ implements Runnable {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ -$$Lambda$vsWBQpiXExY07tlrSzTqh4pNQAQ(Engine engine) {
        this.f$0 = engine;
    }

    public final void run() {
        this.f$0.notifyColorsChanged();
    }
}

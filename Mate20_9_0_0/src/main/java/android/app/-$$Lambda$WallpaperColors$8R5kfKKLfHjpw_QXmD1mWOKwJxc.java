package android.app;

import com.android.internal.graphics.palette.Palette.Swatch;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperColors$8R5kfKKLfHjpw_QXmD1mWOKwJxc implements Predicate {
    private final /* synthetic */ float f$0;

    public /* synthetic */ -$$Lambda$WallpaperColors$8R5kfKKLfHjpw_QXmD1mWOKwJxc(float f) {
        this.f$0 = f;
    }

    public final boolean test(Object obj) {
        return WallpaperColors.lambda$fromBitmap$0(this.f$0, (Swatch) obj);
    }
}

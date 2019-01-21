package android.app;

import android.app.WallpaperManager.OnColorsChangedListener;
import android.util.Pair;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WallpaperManager$Globals$2yG7V1sbMECCnlFTLyjKWKqNoYI implements Predicate {
    private final /* synthetic */ OnColorsChangedListener f$0;

    public /* synthetic */ -$$Lambda$WallpaperManager$Globals$2yG7V1sbMECCnlFTLyjKWKqNoYI(OnColorsChangedListener onColorsChangedListener) {
        this.f$0 = onColorsChangedListener;
    }

    public final boolean test(Object obj) {
        return Globals.lambda$removeOnColorsChangedListener$0(this.f$0, (Pair) obj);
    }
}

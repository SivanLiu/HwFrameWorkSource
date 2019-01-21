package android.view;

import android.graphics.Rect;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ViewGroup$ViewLocationHolder$QbO7cM0ULKe25a7bfXG3VH6DB0c implements Predicate {
    private final /* synthetic */ Rect f$0;
    private final /* synthetic */ Rect f$1;

    public /* synthetic */ -$$Lambda$ViewGroup$ViewLocationHolder$QbO7cM0ULKe25a7bfXG3VH6DB0c(Rect rect, Rect rect2) {
        this.f$0 = rect;
        this.f$1 = rect2;
    }

    public final boolean test(Object obj) {
        return ((View) obj).getBoundsOnScreen(this.f$0, true);
    }
}

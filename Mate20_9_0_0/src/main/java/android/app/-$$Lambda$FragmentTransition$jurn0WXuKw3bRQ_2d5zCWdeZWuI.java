package android.app;

import android.graphics.Rect;
import android.util.ArrayMap;
import android.view.View;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FragmentTransition$jurn0WXuKw3bRQ_2d5zCWdeZWuI implements Runnable {
    private final /* synthetic */ Fragment f$0;
    private final /* synthetic */ Fragment f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ ArrayMap f$3;
    private final /* synthetic */ View f$4;
    private final /* synthetic */ Rect f$5;

    public /* synthetic */ -$$Lambda$FragmentTransition$jurn0WXuKw3bRQ_2d5zCWdeZWuI(Fragment fragment, Fragment fragment2, boolean z, ArrayMap arrayMap, View view, Rect rect) {
        this.f$0 = fragment;
        this.f$1 = fragment2;
        this.f$2 = z;
        this.f$3 = arrayMap;
        this.f$4 = view;
        this.f$5 = rect;
    }

    public final void run() {
        FragmentTransition.lambda$configureSharedElementsReordered$2(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
    }
}

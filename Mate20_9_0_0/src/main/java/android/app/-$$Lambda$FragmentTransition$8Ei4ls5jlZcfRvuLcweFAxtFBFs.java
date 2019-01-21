package android.app;

import android.transition.Transition;
import android.view.View;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FragmentTransition$8Ei4ls5jlZcfRvuLcweFAxtFBFs implements Runnable {
    private final /* synthetic */ Transition f$0;
    private final /* synthetic */ View f$1;
    private final /* synthetic */ Fragment f$2;
    private final /* synthetic */ ArrayList f$3;
    private final /* synthetic */ ArrayList f$4;
    private final /* synthetic */ ArrayList f$5;
    private final /* synthetic */ Transition f$6;

    public /* synthetic */ -$$Lambda$FragmentTransition$8Ei4ls5jlZcfRvuLcweFAxtFBFs(Transition transition, View view, Fragment fragment, ArrayList arrayList, ArrayList arrayList2, ArrayList arrayList3, Transition transition2) {
        this.f$0 = transition;
        this.f$1 = view;
        this.f$2 = fragment;
        this.f$3 = arrayList;
        this.f$4 = arrayList2;
        this.f$5 = arrayList3;
        this.f$6 = transition2;
    }

    public final void run() {
        FragmentTransition.lambda$scheduleTargetChange$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
    }
}

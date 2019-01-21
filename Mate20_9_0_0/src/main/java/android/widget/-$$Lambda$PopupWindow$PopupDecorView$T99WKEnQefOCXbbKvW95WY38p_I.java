package android.widget;

import android.transition.Transition;
import android.transition.Transition.TransitionListener;
import android.view.View;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PopupWindow$PopupDecorView$T99WKEnQefOCXbbKvW95WY38p_I implements Runnable {
    private final /* synthetic */ PopupDecorView f$0;
    private final /* synthetic */ TransitionListener f$1;
    private final /* synthetic */ Transition f$2;
    private final /* synthetic */ View f$3;

    public /* synthetic */ -$$Lambda$PopupWindow$PopupDecorView$T99WKEnQefOCXbbKvW95WY38p_I(PopupDecorView popupDecorView, TransitionListener transitionListener, Transition transition, View view) {
        this.f$0 = popupDecorView;
        this.f$1 = transitionListener;
        this.f$2 = transition;
        this.f$3 = view;
    }

    public final void run() {
        PopupDecorView.lambda$startExitTransition$0(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}

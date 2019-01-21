package android.widget;

import android.view.ViewTreeObserver.OnScrollChangedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PopupWindow$nV1HS3Nc6Ck5JRIbIHe3mkyHWzc implements OnScrollChangedListener {
    private final /* synthetic */ PopupWindow f$0;

    public /* synthetic */ -$$Lambda$PopupWindow$nV1HS3Nc6Ck5JRIbIHe3mkyHWzc(PopupWindow popupWindow) {
        this.f$0 = popupWindow;
    }

    public final void onScrollChanged() {
        this.f$0.alignToAnchor();
    }
}

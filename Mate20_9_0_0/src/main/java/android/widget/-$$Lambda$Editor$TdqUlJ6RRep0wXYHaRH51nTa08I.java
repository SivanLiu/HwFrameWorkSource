package android.widget;

import android.widget.PopupWindow.OnDismissListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Editor$TdqUlJ6RRep0wXYHaRH51nTa08I implements OnDismissListener {
    private final /* synthetic */ Editor f$0;

    public /* synthetic */ -$$Lambda$Editor$TdqUlJ6RRep0wXYHaRH51nTa08I(Editor editor) {
        this.f$0 = editor;
    }

    public final void onDismiss() {
        this.f$0.stopTextActionMode();
    }
}

package android.widget;

import android.widget.SearchView.SearchAutoComplete;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SearchView$SearchAutoComplete$qdPU54FiW6QTzCbsg7P4cSs3cJ8 implements Runnable {
    private final /* synthetic */ SearchAutoComplete f$0;

    public /* synthetic */ -$$Lambda$SearchView$SearchAutoComplete$qdPU54FiW6QTzCbsg7P4cSs3cJ8(SearchAutoComplete searchAutoComplete) {
        this.f$0 = searchAutoComplete;
    }

    public final void run() {
        this.f$0.showSoftInputIfNecessary();
    }
}

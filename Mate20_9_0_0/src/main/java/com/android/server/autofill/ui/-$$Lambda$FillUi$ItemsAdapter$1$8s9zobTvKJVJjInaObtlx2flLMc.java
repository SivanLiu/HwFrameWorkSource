package com.android.server.autofill.ui;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FillUi$ItemsAdapter$1$8s9zobTvKJVJjInaObtlx2flLMc implements Predicate {
    private final /* synthetic */ CharSequence f$0;

    public /* synthetic */ -$$Lambda$FillUi$ItemsAdapter$1$8s9zobTvKJVJjInaObtlx2flLMc(CharSequence charSequence) {
        this.f$0 = charSequence;
    }

    public final boolean test(Object obj) {
        return ((ViewItem) obj).matches(this.f$0);
    }
}

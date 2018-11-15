package com.android.server.autofill;

import android.app.assist.AssistStructure.ViewNode;
import android.view.autofill.AutofillId;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Helper$nK3g_oXXf8NGajcUf0W5JsQzf3w implements ViewNodeFilter {
    private final /* synthetic */ AutofillId f$0;

    public /* synthetic */ -$$Lambda$Helper$nK3g_oXXf8NGajcUf0W5JsQzf3w(AutofillId autofillId) {
        this.f$0 = autofillId;
    }

    public final boolean matches(ViewNode viewNode) {
        return this.f$0.equals(viewNode.getAutofillId());
    }
}

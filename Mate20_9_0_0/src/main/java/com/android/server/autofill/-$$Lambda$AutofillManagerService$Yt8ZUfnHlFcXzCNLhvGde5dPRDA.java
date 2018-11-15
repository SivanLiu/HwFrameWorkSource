package com.android.server.autofill;

import android.os.Bundle;
import android.os.UserManagerInternal.UserRestrictionsListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AutofillManagerService$Yt8ZUfnHlFcXzCNLhvGde5dPRDA implements UserRestrictionsListener {
    private final /* synthetic */ AutofillManagerService f$0;

    public /* synthetic */ -$$Lambda$AutofillManagerService$Yt8ZUfnHlFcXzCNLhvGde5dPRDA(AutofillManagerService autofillManagerService) {
        this.f$0 = autofillManagerService;
    }

    public final void onUserRestrictionsChanged(int i, Bundle bundle, Bundle bundle2) {
        AutofillManagerService.lambda$new$0(this.f$0, i, bundle, bundle2);
    }
}

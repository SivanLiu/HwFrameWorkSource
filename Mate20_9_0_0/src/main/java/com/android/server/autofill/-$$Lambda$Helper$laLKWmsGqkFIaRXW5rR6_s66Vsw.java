package com.android.server.autofill;

import android.app.assist.AssistStructure.ViewNode;
import com.android.internal.util.ArrayUtils;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Helper$laLKWmsGqkFIaRXW5rR6_s66Vsw implements ViewNodeFilter {
    private final /* synthetic */ String[] f$0;

    public /* synthetic */ -$$Lambda$Helper$laLKWmsGqkFIaRXW5rR6_s66Vsw(String[] strArr) {
        this.f$0 = strArr;
    }

    public final boolean matches(ViewNode viewNode) {
        return ArrayUtils.contains(this.f$0, viewNode.getIdEntry());
    }
}

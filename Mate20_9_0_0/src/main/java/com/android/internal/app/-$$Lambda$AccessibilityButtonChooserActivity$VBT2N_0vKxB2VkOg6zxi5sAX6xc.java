package com.android.internal.app;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityButtonChooserActivity$VBT2N_0vKxB2VkOg6zxi5sAX6xc implements OnItemClickListener {
    private final /* synthetic */ AccessibilityButtonChooserActivity f$0;

    public /* synthetic */ -$$Lambda$AccessibilityButtonChooserActivity$VBT2N_0vKxB2VkOg6zxi5sAX6xc(AccessibilityButtonChooserActivity accessibilityButtonChooserActivity) {
        this.f$0 = accessibilityButtonChooserActivity;
    }

    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        this.f$0.onTargetSelected((AccessibilityButtonTarget) this.f$0.mTargets.get(i));
    }
}

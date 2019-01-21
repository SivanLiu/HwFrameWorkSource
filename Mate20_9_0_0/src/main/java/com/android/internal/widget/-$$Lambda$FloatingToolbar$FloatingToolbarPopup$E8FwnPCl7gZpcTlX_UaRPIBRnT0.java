package com.android.internal.widget;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FloatingToolbar$FloatingToolbarPopup$E8FwnPCl7gZpcTlX_UaRPIBRnT0 implements OnItemClickListener {
    private final /* synthetic */ FloatingToolbarPopup f$0;
    private final /* synthetic */ OverflowPanel f$1;

    public /* synthetic */ -$$Lambda$FloatingToolbar$FloatingToolbarPopup$E8FwnPCl7gZpcTlX_UaRPIBRnT0(FloatingToolbarPopup floatingToolbarPopup, OverflowPanel overflowPanel) {
        this.f$0 = floatingToolbarPopup;
        this.f$1 = overflowPanel;
    }

    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        FloatingToolbarPopup.lambda$createOverflowPanel$2(this.f$0, this.f$1, adapterView, view, i, j);
    }
}

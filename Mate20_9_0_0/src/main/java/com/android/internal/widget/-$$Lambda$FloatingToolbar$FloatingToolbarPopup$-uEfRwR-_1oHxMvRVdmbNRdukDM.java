package com.android.internal.widget;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FloatingToolbar$FloatingToolbarPopup$-uEfRwR-_1oHxMvRVdmbNRdukDM implements OnClickListener {
    private final /* synthetic */ FloatingToolbarPopup f$0;
    private final /* synthetic */ ImageButton f$1;

    public /* synthetic */ -$$Lambda$FloatingToolbar$FloatingToolbarPopup$-uEfRwR-_1oHxMvRVdmbNRdukDM(FloatingToolbarPopup floatingToolbarPopup, ImageButton imageButton) {
        this.f$0 = floatingToolbarPopup;
        this.f$1 = imageButton;
    }

    public final void onClick(View view) {
        FloatingToolbarPopup.lambda$createOverflowButton$1(this.f$0, this.f$1, view);
    }
}

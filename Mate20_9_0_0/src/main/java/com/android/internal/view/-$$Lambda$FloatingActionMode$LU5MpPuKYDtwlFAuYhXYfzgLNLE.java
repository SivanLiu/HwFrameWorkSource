package com.android.internal.view;

import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FloatingActionMode$LU5MpPuKYDtwlFAuYhXYfzgLNLE implements OnMenuItemClickListener {
    private final /* synthetic */ FloatingActionMode f$0;

    public /* synthetic */ -$$Lambda$FloatingActionMode$LU5MpPuKYDtwlFAuYhXYfzgLNLE(FloatingActionMode floatingActionMode) {
        this.f$0 = floatingActionMode;
    }

    public final boolean onMenuItemClick(MenuItem menuItem) {
        return this.f$0.mMenu.performItemAction(menuItem, 0);
    }
}

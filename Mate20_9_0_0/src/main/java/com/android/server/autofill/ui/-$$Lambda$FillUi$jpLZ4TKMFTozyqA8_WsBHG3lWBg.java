package com.android.server.autofill.ui;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FillUi$jpLZ4TKMFTozyqA8_WsBHG3lWBg implements OnItemClickListener {
    private final /* synthetic */ FillUi f$0;

    public /* synthetic */ -$$Lambda$FillUi$jpLZ4TKMFTozyqA8_WsBHG3lWBg(FillUi fillUi) {
        this.f$0 = fillUi;
    }

    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        this.f$0.mCallback.onDatasetPicked(this.f$0.mAdapter.getItem(i).dataset);
    }
}

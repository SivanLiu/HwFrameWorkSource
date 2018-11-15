package com.android.systemui.shared.recents.model;

import android.util.SparseArray;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI implements TaskFilter {
    public static final /* synthetic */ -$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI INSTANCE = new -$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI();

    private /* synthetic */ -$$Lambda$TaskStack$gkuBLLtJ6FV7PDAxT-_KECDzTOI() {
    }

    public final boolean acceptTask(SparseArray sparseArray, Task task, int i) {
        return task.isStackTask;
    }
}

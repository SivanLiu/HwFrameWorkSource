package com.android.server.am;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockTaskController$CFBSOJhWPyFwVT85DSWkAEj1wF0 implements Runnable {
    private final /* synthetic */ LockTaskController f$0;
    private final /* synthetic */ TaskRecord f$1;

    public /* synthetic */ -$$Lambda$LockTaskController$CFBSOJhWPyFwVT85DSWkAEj1wF0(LockTaskController lockTaskController, TaskRecord taskRecord) {
        this.f$0 = lockTaskController;
        this.f$1 = taskRecord;
    }

    public final void run() {
        this.f$0.performStopLockTask(this.f$1.userId);
    }
}

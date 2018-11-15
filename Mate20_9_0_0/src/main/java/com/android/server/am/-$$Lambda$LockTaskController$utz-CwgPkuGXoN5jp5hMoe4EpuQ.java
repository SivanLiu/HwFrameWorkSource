package com.android.server.am;

import android.content.Intent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockTaskController$utz-CwgPkuGXoN5jp5hMoe4EpuQ implements Runnable {
    private final /* synthetic */ LockTaskController f$0;
    private final /* synthetic */ Intent f$1;
    private final /* synthetic */ TaskRecord f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$LockTaskController$utz-CwgPkuGXoN5jp5hMoe4EpuQ(LockTaskController lockTaskController, Intent intent, TaskRecord taskRecord, int i) {
        this.f$0 = lockTaskController;
        this.f$1 = intent;
        this.f$2 = taskRecord;
        this.f$3 = i;
    }

    public final void run() {
        this.f$0.performStartLockTask(this.f$1.getComponent().getPackageName(), this.f$2.userId, this.f$3);
    }
}

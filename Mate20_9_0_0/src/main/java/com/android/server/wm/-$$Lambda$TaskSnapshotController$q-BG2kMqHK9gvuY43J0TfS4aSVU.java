package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy.ScreenOffListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskSnapshotController$q-BG2kMqHK9gvuY43J0TfS4aSVU implements Runnable {
    private final /* synthetic */ TaskSnapshotController f$0;
    private final /* synthetic */ ScreenOffListener f$1;

    public /* synthetic */ -$$Lambda$TaskSnapshotController$q-BG2kMqHK9gvuY43J0TfS4aSVU(TaskSnapshotController taskSnapshotController, ScreenOffListener screenOffListener) {
        this.f$0 = taskSnapshotController;
        this.f$1 = screenOffListener;
    }

    public final void run() {
        TaskSnapshotController.lambda$screenTurningOff$2(this.f$0, this.f$1);
    }
}

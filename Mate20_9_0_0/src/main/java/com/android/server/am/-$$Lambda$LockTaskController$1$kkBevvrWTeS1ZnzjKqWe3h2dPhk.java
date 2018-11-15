package com.android.server.am;

import com.android.server.am.LockTaskController.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockTaskController$1$kkBevvrWTeS1ZnzjKqWe3h2dPhk implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$LockTaskController$1$kkBevvrWTeS1ZnzjKqWe3h2dPhk(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        LockTaskController.this.mWindowManager.disableKeyguard(LockTaskController.this.mToken, LockTaskController.LOCK_TASK_TAG);
    }
}

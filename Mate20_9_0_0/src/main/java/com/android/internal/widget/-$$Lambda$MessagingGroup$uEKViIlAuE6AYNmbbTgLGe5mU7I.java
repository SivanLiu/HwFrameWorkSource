package com.android.internal.widget;

import android.view.View;
import android.view.ViewGroup;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MessagingGroup$uEKViIlAuE6AYNmbbTgLGe5mU7I implements Runnable {
    private final /* synthetic */ ViewGroup f$0;
    private final /* synthetic */ View f$1;
    private final /* synthetic */ MessagingMessage f$2;

    public /* synthetic */ -$$Lambda$MessagingGroup$uEKViIlAuE6AYNmbbTgLGe5mU7I(ViewGroup viewGroup, View view, MessagingMessage messagingMessage) {
        this.f$0 = viewGroup;
        this.f$1 = view;
        this.f$2 = messagingMessage;
    }

    public final void run() {
        MessagingGroup.lambda$removeMessage$0(this.f$0, this.f$1, this.f$2);
    }
}

package com.android.server.notification;

import android.net.Uri;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NotificationRecord$XgkrZGcjOHPHem34oE9qLGy3siA implements Consumer {
    private final /* synthetic */ NotificationRecord f$0;

    public /* synthetic */ -$$Lambda$NotificationRecord$XgkrZGcjOHPHem34oE9qLGy3siA(NotificationRecord notificationRecord) {
        this.f$0 = notificationRecord;
    }

    public final void accept(Object obj) {
        this.f$0.visitGrantableUri((Uri) obj, false);
    }
}

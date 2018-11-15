package com.android.server.notification;

import com.android.server.notification.NotificationManagerService.NotificationListeners;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NotificationManagerService$NotificationListeners$Zsu32u2gOsCsEatsnnXTLoY37u0 implements Runnable {
    private final /* synthetic */ NotificationListeners f$0;
    private final /* synthetic */ NotificationRecord f$1;

    public /* synthetic */ -$$Lambda$NotificationManagerService$NotificationListeners$Zsu32u2gOsCsEatsnnXTLoY37u0(NotificationListeners notificationListeners, NotificationRecord notificationRecord) {
        this.f$0 = notificationListeners;
        this.f$1 = notificationRecord;
    }

    public final void run() {
        NotificationManagerService.this.updateUriPermissions(null, this.f$1, null, 0);
    }
}

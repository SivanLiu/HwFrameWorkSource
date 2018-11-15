package com.android.server.notification;

import android.app.NotificationChannel;
import android.os.UserHandle;
import com.android.server.notification.ManagedServices.ManagedServiceInfo;
import com.android.server.notification.NotificationManagerService.NotificationListeners;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NotificationManagerService$NotificationListeners$E8qsF-PrFYYUtUGked50-pRub20 implements Runnable {
    private final /* synthetic */ NotificationListeners f$0;
    private final /* synthetic */ ManagedServiceInfo f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ UserHandle f$3;
    private final /* synthetic */ NotificationChannel f$4;
    private final /* synthetic */ int f$5;

    public /* synthetic */ -$$Lambda$NotificationManagerService$NotificationListeners$E8qsF-PrFYYUtUGked50-pRub20(NotificationListeners notificationListeners, ManagedServiceInfo managedServiceInfo, String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) {
        this.f$0 = notificationListeners;
        this.f$1 = managedServiceInfo;
        this.f$2 = str;
        this.f$3 = userHandle;
        this.f$4 = notificationChannel;
        this.f$5 = i;
    }

    public final void run() {
        NotificationListeners.lambda$notifyNotificationChannelChanged$1(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
    }
}

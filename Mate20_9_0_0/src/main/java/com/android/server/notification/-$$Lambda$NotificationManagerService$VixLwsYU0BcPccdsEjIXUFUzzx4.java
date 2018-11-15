package com.android.server.notification;

import android.os.VibrationEffect;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NotificationManagerService$VixLwsYU0BcPccdsEjIXUFUzzx4 implements Runnable {
    private final /* synthetic */ NotificationManagerService f$0;
    private final /* synthetic */ NotificationRecord f$1;
    private final /* synthetic */ VibrationEffect f$2;

    public /* synthetic */ -$$Lambda$NotificationManagerService$VixLwsYU0BcPccdsEjIXUFUzzx4(NotificationManagerService notificationManagerService, NotificationRecord notificationRecord, VibrationEffect vibrationEffect) {
        this.f$0 = notificationManagerService;
        this.f$1 = notificationRecord;
        this.f$2 = vibrationEffect;
    }

    public final void run() {
        NotificationManagerService.lambda$playVibration$0(this.f$0, this.f$1, this.f$2);
    }
}

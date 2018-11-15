package com.android.server.notification;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ouaYRM5YVYoMkUW8dm6TnIjLfgg implements Predicate {
    private final /* synthetic */ NotificationManagerService f$0;

    public /* synthetic */ -$$Lambda$ouaYRM5YVYoMkUW8dm6TnIjLfgg(NotificationManagerService notificationManagerService) {
        this.f$0 = notificationManagerService;
    }

    public final boolean test(Object obj) {
        return this.f$0.canUseManagedServices((String) obj);
    }
}

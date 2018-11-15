package com.android.server.devicepolicy;

import android.app.admin.SecurityLog.SecurityEvent;
import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI implements Comparator {
    public static final /* synthetic */ -$$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI INSTANCE = new -$$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI();

    private /* synthetic */ -$$Lambda$SecurityLogMonitor$y5Q3dMmmJ8bk5nBh8WR2MUroKrI() {
    }

    public final int compare(Object obj, Object obj2) {
        return Long.signum(((SecurityEvent) obj).getTimeNanos() - ((SecurityEvent) obj2).getTimeNanos());
    }
}

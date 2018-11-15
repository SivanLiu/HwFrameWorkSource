package com.android.server.appwidget;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppWidgetService$HIwvoPMyKqEhLVIiysgUKH8QJg8 implements Runnable {
    private final /* synthetic */ AppWidgetService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$AppWidgetService$HIwvoPMyKqEhLVIiysgUKH8QJg8(AppWidgetService appWidgetService, int i) {
        this.f$0 = appWidgetService;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mImpl.onUserUnlocked(this.f$1);
    }
}

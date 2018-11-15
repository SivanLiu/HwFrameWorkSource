package com.android.server.am;

import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RecentTasks$NgzE6eN0wIO1cgLW7RzciPDBTHk implements Comparator {
    public static final /* synthetic */ -$$Lambda$RecentTasks$NgzE6eN0wIO1cgLW7RzciPDBTHk INSTANCE = new -$$Lambda$RecentTasks$NgzE6eN0wIO1cgLW7RzciPDBTHk();

    private /* synthetic */ -$$Lambda$RecentTasks$NgzE6eN0wIO1cgLW7RzciPDBTHk() {
    }

    public final int compare(Object obj, Object obj2) {
        return (((TaskRecord) obj2).taskId - ((TaskRecord) obj).taskId);
    }
}

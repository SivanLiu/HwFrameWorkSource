package com.android.server;

import com.android.internal.util.function.QuintConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw implements QuintConsumer {
    public static final /* synthetic */ -$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw INSTANCE = new -$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw();

    private /* synthetic */ -$$Lambda$AppOpsService$lxgFmOnGguOiLyfUZbyOpNBfTVw() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
        ((AppOpsService) obj).notifyOpChanged((ModeCallback) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (String) obj5);
    }
}

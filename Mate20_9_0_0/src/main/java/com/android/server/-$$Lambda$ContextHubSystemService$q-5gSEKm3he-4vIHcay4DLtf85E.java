package com.android.server;

import android.content.Context;
import com.android.server.location.ContextHubService;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubSystemService$q-5gSEKm3he-4vIHcay4DLtf85E implements Runnable {
    private final /* synthetic */ ContextHubSystemService f$0;
    private final /* synthetic */ Context f$1;

    public /* synthetic */ -$$Lambda$ContextHubSystemService$q-5gSEKm3he-4vIHcay4DLtf85E(ContextHubSystemService contextHubSystemService, Context context) {
        this.f$0 = contextHubSystemService;
        this.f$1 = context;
    }

    public final void run() {
        this.f$0.mContextHubService = new ContextHubService(this.f$1);
    }
}

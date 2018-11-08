package com.android.server;

import android.content.Context;
import android.util.Log;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.location.ContextHubService;
import java.util.concurrent.Future;

class ContextHubSystemService extends SystemService {
    private static final String TAG = "ContextHubSystemService";
    private ContextHubService mContextHubService;
    private Future<?> mInit;

    public ContextHubSystemService(Context context) {
        super(context);
        this.mInit = SystemServerInitThreadPool.get().submit(new -$Lambda$Ganck_s9Kl5o2K6eVDoQTKLc-6g((byte) 1, this, context), "Init ContextHubSystemService");
    }

    /* synthetic */ void lambda$-com_android_server_ContextHubSystemService_1237(Context context) {
        this.mContextHubService = new ContextHubService(context);
    }

    public void onStart() {
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            Log.d(TAG, "onBootPhase: PHASE_SYSTEM_SERVICES_READY");
            ConcurrentUtils.waitForFutureNoInterrupt(this.mInit, "Wait for ContextHubSystemService init");
            this.mInit = null;
            publishBinderService("contexthub", this.mContextHubService);
        }
    }
}

package com.android.server.connectivity.tethering;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;

public class SimChangeListener {
    private static final boolean DBG = false;
    private static final String TAG = SimChangeListener.class.getSimpleName();
    private BroadcastReceiver mBroadcastReceiver;
    private final Runnable mCallback;
    private final Context mContext;
    private final AtomicInteger mSimBcastGenerationNumber = new AtomicInteger(0);
    private final Handler mTarget;

    private class SimChangeBroadcastReceiver extends BroadcastReceiver {
        private final int mGenerationNumber;
        private boolean mSimNotLoadedSeen = false;

        public SimChangeBroadcastReceiver(int generationNumber) {
            this.mGenerationNumber = generationNumber;
        }

        public void onReceive(Context context, Intent intent) {
            if (this.mGenerationNumber == SimChangeListener.this.mSimBcastGenerationNumber.get()) {
                String state = intent.getStringExtra("ss");
                Log.d(SimChangeListener.TAG, "got Sim changed to state " + state + ", mSimNotLoadedSeen=" + this.mSimNotLoadedSeen);
                if (SimChangeListener.this.isSimCardLoaded(state)) {
                    if (this.mSimNotLoadedSeen) {
                        this.mSimNotLoadedSeen = false;
                        SimChangeListener.this.mCallback.run();
                    }
                    return;
                }
                this.mSimNotLoadedSeen = true;
            }
        }
    }

    public SimChangeListener(Context ctx, Handler handler, Runnable onSimCardLoadedCallback) {
        this.mContext = ctx;
        this.mTarget = handler;
        this.mCallback = onSimCardLoadedCallback;
    }

    public int generationNumber() {
        return this.mSimBcastGenerationNumber.get();
    }

    public void startListening() {
        if (this.mBroadcastReceiver == null) {
            this.mBroadcastReceiver = new SimChangeBroadcastReceiver(this.mSimBcastGenerationNumber.incrementAndGet());
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SIM_STATE_CHANGED");
            this.mContext.registerReceiver(this.mBroadcastReceiver, filter, null, this.mTarget);
        }
    }

    public void stopListening() {
        if (this.mBroadcastReceiver != null) {
            this.mSimBcastGenerationNumber.incrementAndGet();
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            this.mBroadcastReceiver = null;
        }
    }

    private boolean isSimCardLoaded(String state) {
        return "LOADED".equals(state);
    }
}

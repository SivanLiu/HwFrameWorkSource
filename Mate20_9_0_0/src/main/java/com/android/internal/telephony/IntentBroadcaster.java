package com.android.internal.telephony;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class IntentBroadcaster {
    private static final String TAG = "IntentBroadcaster";
    private static IntentBroadcaster sIntentBroadcaster;
    private Map<Integer, Intent> mRebroadcastIntents = new HashMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_UNLOCKED")) {
                synchronized (IntentBroadcaster.this.mRebroadcastIntents) {
                    Iterator iterator = IntentBroadcaster.this.mRebroadcastIntents.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry pair = (Entry) iterator.next();
                        Intent i = (Intent) pair.getValue();
                        i.putExtra("rebroadcastOnUnlock", true);
                        iterator.remove();
                        IntentBroadcaster intentBroadcaster = IntentBroadcaster.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Rebroadcasting intent ");
                        stringBuilder.append(i.getAction());
                        stringBuilder.append(" ");
                        stringBuilder.append(i.getStringExtra("ss"));
                        stringBuilder.append(" for slotId ");
                        stringBuilder.append(pair.getKey());
                        intentBroadcaster.logd(stringBuilder.toString());
                        ActivityManager.broadcastStickyIntent(i, -1);
                    }
                }
            }
        }
    };

    private IntentBroadcaster(Context context) {
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
    }

    public static IntentBroadcaster getInstance(Context context) {
        if (sIntentBroadcaster == null) {
            sIntentBroadcaster = new IntentBroadcaster(context);
        }
        return sIntentBroadcaster;
    }

    public static IntentBroadcaster getInstance() {
        return sIntentBroadcaster;
    }

    public void broadcastStickyIntent(Intent intent, int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Broadcasting and adding intent for rebroadcast: ");
        stringBuilder.append(intent.getAction());
        stringBuilder.append(" ");
        stringBuilder.append(intent.getStringExtra("ss"));
        stringBuilder.append(" for slotId ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        synchronized (this.mRebroadcastIntents) {
            ActivityManager.broadcastStickyIntent(intent, -1);
            this.mRebroadcastIntents.put(Integer.valueOf(slotId), intent);
        }
    }

    private void logd(String s) {
        Log.d(TAG, s);
    }
}

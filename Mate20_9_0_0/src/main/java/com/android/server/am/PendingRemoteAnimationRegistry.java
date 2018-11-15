package com.android.server.am;

import android.app.ActivityOptions;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.RemoteAnimationAdapter;

class PendingRemoteAnimationRegistry {
    private static final long TIMEOUT_MS = 3000;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap();
    private final Handler mHandler;
    private final ActivityManagerService mService;

    private class Entry {
        final RemoteAnimationAdapter adapter;
        final String packageName;

        Entry(String packageName, RemoteAnimationAdapter adapter) {
            this.packageName = packageName;
            this.adapter = adapter;
            PendingRemoteAnimationRegistry.this.mHandler.postDelayed(new -$$Lambda$PendingRemoteAnimationRegistry$Entry$nMsaTjyghAPVeCjs7XjsdMM78mc(this, packageName), PendingRemoteAnimationRegistry.TIMEOUT_MS);
        }

        public static /* synthetic */ void lambda$new$0(Entry entry, String packageName) {
            synchronized (PendingRemoteAnimationRegistry.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (((Entry) PendingRemoteAnimationRegistry.this.mEntries.get(packageName)) == entry) {
                        PendingRemoteAnimationRegistry.this.mEntries.remove(packageName);
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    PendingRemoteAnimationRegistry(ActivityManagerService service, Handler handler) {
        this.mService = service;
        this.mHandler = handler;
    }

    void addPendingAnimation(String packageName, RemoteAnimationAdapter adapter) {
        this.mEntries.put(packageName, new Entry(packageName, adapter));
    }

    ActivityOptions overrideOptionsIfNeeded(String callingPackage, ActivityOptions options) {
        Entry entry = (Entry) this.mEntries.get(callingPackage);
        if (entry == null) {
            return options;
        }
        if (options == null) {
            options = ActivityOptions.makeRemoteAnimation(entry.adapter);
        } else {
            options.setRemoteAnimationAdapter(entry.adapter);
        }
        this.mEntries.remove(callingPackage);
        return options;
    }
}

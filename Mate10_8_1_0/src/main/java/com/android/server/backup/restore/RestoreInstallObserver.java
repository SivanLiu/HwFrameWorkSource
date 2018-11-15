package com.android.server.backup.restore;

import android.app.PackageInstallObserver;
import android.os.Bundle;
import com.android.internal.annotations.GuardedBy;
import java.util.concurrent.atomic.AtomicBoolean;

public class RestoreInstallObserver extends PackageInstallObserver {
    @GuardedBy("mDone")
    private final AtomicBoolean mDone = new AtomicBoolean();
    private String mPackageName;
    private int mResult;

    public void reset() {
        synchronized (this.mDone) {
            this.mDone.set(false);
        }
    }

    public void waitForCompletion() {
        synchronized (this.mDone) {
            while (!this.mDone.get()) {
                try {
                    this.mDone.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public int getResult() {
        return this.mResult;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void onPackageInstalled(String packageName, int returnCode, String msg, Bundle extras) {
        synchronized (this.mDone) {
            this.mResult = returnCode;
            this.mPackageName = packageName;
            this.mDone.set(true);
            this.mDone.notifyAll();
        }
    }
}

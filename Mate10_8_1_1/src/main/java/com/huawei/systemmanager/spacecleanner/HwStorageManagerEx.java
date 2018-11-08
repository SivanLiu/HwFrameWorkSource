package com.huawei.systemmanager.spacecleanner;

import android.content.Context;
import android.os.storage.StorageManager;

public class HwStorageManagerEx {
    private StorageManager mImpl = null;

    public HwStorageManagerEx(Context context) {
        this.mImpl = (StorageManager) context.getSystemService("storage");
    }

    public long getUndiscardInfo() {
        return (long) this.mImpl.getUndiscardInfo();
    }

    public int getMinTimeCost() {
        return this.mImpl.getMinTimeCost();
    }

    public int startClean() {
        return this.mImpl.startClean();
    }

    public int stopClean() {
        return this.mImpl.stopClean();
    }

    public int getPercentComplete() {
        return this.mImpl.getPercentComplete();
    }

    public int getNotificationLevel() {
        return this.mImpl.getNotificationLevel();
    }
}

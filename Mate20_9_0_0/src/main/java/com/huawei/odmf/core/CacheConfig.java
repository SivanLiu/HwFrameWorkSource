package com.huawei.odmf.core;

import com.huawei.odmf.utils.ODMFCache;
import com.huawei.odmf.utils.Singleton;

public class CacheConfig {
    private static final Singleton<CacheConfig> gDefault = new Singleton<CacheConfig>() {
        public CacheConfig create() {
            return new CacheConfig();
        }
    };
    private boolean isOpenObjectCache;
    private final Object lock;
    private int objectCacheNum;

    /* synthetic */ CacheConfig(AnonymousClass1 x0) {
        this();
    }

    public boolean isOpenObjectCache() {
        boolean z;
        synchronized (this.lock) {
            z = this.isOpenObjectCache;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:26:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setOpenObjectCache(boolean openObjectCache) {
        synchronized (this.lock) {
            boolean current = this.isOpenObjectCache;
            this.isOpenObjectCache = openObjectCache;
            if (!current && !this.isOpenObjectCache) {
            } else if (current && this.isOpenObjectCache) {
            } else {
                ODMFCache cache = PersistentStoreCoordinator.getDefault().getObjectsCache();
                if (cache != null) {
                    cache.clear();
                }
                if (this.isOpenObjectCache) {
                    PersistentStoreCoordinator.getDefault().createObjectsCache();
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setOpenObjectCache(boolean openObjectCache, int objectCacheNum) {
        synchronized (this.lock) {
            boolean current = this.isOpenObjectCache;
            this.isOpenObjectCache = openObjectCache;
            if (current || this.isOpenObjectCache) {
                ODMFCache cache = PersistentStoreCoordinator.getDefault().getObjectsCache();
                if (cache != null) {
                    cache.clear();
                }
                if (this.isOpenObjectCache) {
                    setObjectCacheNum(objectCacheNum);
                    PersistentStoreCoordinator.getDefault().createObjectsCache();
                }
            }
        }
    }

    public int getObjectCacheNum() {
        int i;
        synchronized (this.lock) {
            i = this.objectCacheNum;
        }
        return i;
    }

    private void setObjectCacheNum(int objectCacheNum) {
        synchronized (this.lock) {
            this.objectCacheNum = objectCacheNum;
        }
    }

    private CacheConfig() {
        this.lock = new Object();
        this.isOpenObjectCache = false;
        this.objectCacheNum = 100;
    }

    public static CacheConfig getDefault() {
        return (CacheConfig) gDefault.get();
    }
}

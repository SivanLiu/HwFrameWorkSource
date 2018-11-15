package com.huawei.odmf.core;

import android.content.Context;
import android.net.Uri;
import com.huawei.odmf.exception.ODMFRuntimeException;
import java.util.Map;

public class PersistentStoreFactory {
    public static int createPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, String modelPath) {
        return PersistentStoreCoordinator.getDefault().createPersistentStore(uri, storeConfiguration, appCtx, modelPath);
    }

    public static int createEncryptedPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, String modelPath, byte[] key) throws ODMFRuntimeException {
        if (key == null || key.length == 0) {
            throw new ODMFRuntimeException("encrypted database must be set a not null key");
        }
        try {
            int createEncryptedPersistentStore = PersistentStoreCoordinator.getDefault().createEncryptedPersistentStore(uri, storeConfiguration, appCtx, modelPath, key);
            return createEncryptedPersistentStore;
        } finally {
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) 0;
            }
        }
    }

    public static int createCrossPersistentStore(Uri uri, Configuration storeConfiguration, Context appCtx, Map<Uri, byte[]> uriList) {
        return PersistentStoreCoordinator.getDefault().createCrossPersistentStore(uri, storeConfiguration, appCtx, uriList);
    }

    public static PersistentStore getPersistentStore(Uri uri) {
        return PersistentStoreCoordinator.getDefault().getPersistentStore(uri);
    }
}

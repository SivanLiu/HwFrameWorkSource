package com.huawei.odmf.core;

import android.content.Context;

class EncryptedAndroidSqlPersistentStore extends AndroidSqlPersistentStore {
    EncryptedAndroidSqlPersistentStore(Context context, String modelPath, String uriString, Configuration configuration, byte[] key) {
        super(context, modelPath, uriString, configuration, key);
    }

    @Override // com.huawei.odmf.core.PersistentStore
    public void resetDatabaseEncryptKey(byte[] oldKey, byte[] newKey) {
        try {
            this.db.resetDatabaseEncryptKey(oldKey, newKey);
        } finally {
            clearKey(oldKey);
            clearKey(newKey);
        }
    }
}

package com.android.server.locksettings.recoverablekeystore.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.locksettings.recoverablekeystore.TestOnlyInsecureCertificateHelper;
import com.android.server.locksettings.recoverablekeystore.WrappedKey;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class RecoverableKeyStoreDb {
    private static final String CERT_PATH_ENCODING = "PkiPath";
    private static final int IDLE_TIMEOUT_SECONDS = 30;
    private static final int LAST_SYNCED_AT_UNSYNCED = -1;
    private static final String TAG = "RecoverableKeyStoreDb";
    private final RecoverableKeyStoreDbHelper mKeyStoreDbHelper;
    private final TestOnlyInsecureCertificateHelper mTestOnlyInsecureCertificateHelper = new TestOnlyInsecureCertificateHelper();

    public static RecoverableKeyStoreDb newInstance(Context context) {
        RecoverableKeyStoreDbHelper helper = new RecoverableKeyStoreDbHelper(context);
        helper.setWriteAheadLoggingEnabled(true);
        helper.setIdleConnectionTimeout(30);
        return new RecoverableKeyStoreDb(helper);
    }

    private RecoverableKeyStoreDb(RecoverableKeyStoreDbHelper keyStoreDbHelper) {
        this.mKeyStoreDbHelper = keyStoreDbHelper;
    }

    public long insertKey(int userId, int uid, String alias, WrappedKey wrappedKey) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", Integer.valueOf(userId));
        values.put("uid", Integer.valueOf(uid));
        values.put("alias", alias);
        values.put("nonce", wrappedKey.getNonce());
        values.put("wrapped_key", wrappedKey.getKeyMaterial());
        values.put("last_synced_at", Integer.valueOf(-1));
        values.put("platform_key_generation_id", Integer.valueOf(wrappedKey.getPlatformKeyGenerationId()));
        values.put("recovery_status", Integer.valueOf(wrappedKey.getRecoveryStatus()));
        return db.replace("keys", null, values);
    }

    public WrappedKey getKey(int uid, String alias) {
        Throwable th;
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("keys", new String[]{"_id", "nonce", "wrapped_key", "platform_key_generation_id", "recovery_status"}, "uid = ? AND alias = ?", new String[]{Integer.toString(uid), alias}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d WrappedKey entries found for uid=%d alias='%s'. Should only ever be 0 or 1.", new Object[]{Integer.valueOf(count), Integer.valueOf(uid), alias}));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                WrappedKey wrappedKey = new WrappedKey(cursor2.getBlob(cursor2.getColumnIndexOrThrow("nonce")), cursor2.getBlob(cursor2.getColumnIndexOrThrow("wrapped_key")), cursor2.getInt(cursor2.getColumnIndexOrThrow("platform_key_generation_id")), cursor2.getInt(cursor2.getColumnIndexOrThrow("recovery_status")));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return wrappedKey;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public boolean removeKey(int uid, String alias) {
        if (this.mKeyStoreDbHelper.getWritableDatabase().delete("keys", "uid = ? AND alias = ?", new String[]{Integer.toString(uid), alias}) > 0) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:14:0x005f, code skipped:
            if (r1 != null) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:15:0x0061, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Map<String, Integer> getStatusForAllKeys(int uid) {
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("keys", new String[]{"_id", "alias", "recovery_status"}, "uid = ?", new String[]{Integer.toString(uid)}, null, null, null);
        HashMap<String, Integer> statuses = new HashMap();
        while (cursor2.moveToNext()) {
            statuses.put(cursor2.getString(cursor2.getColumnIndexOrThrow("alias")), Integer.valueOf(cursor2.getInt(cursor2.getColumnIndexOrThrow("recovery_status"))));
        }
        if (cursor2 != null) {
            $closeResource(null, cursor2);
        }
        return statuses;
    }

    public int setRecoveryStatus(int uid, String alias, int status) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("recovery_status", Integer.valueOf(status));
        return db.update("keys", values, "uid = ? AND alias = ?", new String[]{String.valueOf(uid), alias});
    }

    /* JADX WARNING: Missing block: B:14:0x008a, code skipped:
            if (r1 != null) goto L_0x008c;
     */
    /* JADX WARNING: Missing block: B:15:0x008c, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Map<String, WrappedKey> getAllKeys(int userId, int recoveryAgentUid, int platformKeyGenerationId) {
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("keys", new String[]{"_id", "nonce", "wrapped_key", "alias", "recovery_status"}, "user_id = ? AND uid = ? AND platform_key_generation_id = ?", new String[]{Integer.toString(userId), Integer.toString(recoveryAgentUid), Integer.toString(platformKeyGenerationId)}, null, null, null);
        HashMap<String, WrappedKey> keys = new HashMap();
        while (cursor2.moveToNext()) {
            keys.put(cursor2.getString(cursor2.getColumnIndexOrThrow("alias")), new WrappedKey(cursor2.getBlob(cursor2.getColumnIndexOrThrow("nonce")), cursor2.getBlob(cursor2.getColumnIndexOrThrow("wrapped_key")), platformKeyGenerationId, cursor2.getInt(cursor2.getColumnIndexOrThrow("recovery_status"))));
        }
        if (cursor2 != null) {
            $closeResource(null, cursor2);
        }
        return keys;
    }

    public long setPlatformKeyGenerationId(int userId, int generationId) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", Integer.valueOf(userId));
        values.put("platform_key_generation_id", Integer.valueOf(generationId));
        long result = db.replace("user_metadata", null, values);
        if (result != -1) {
            invalidateKeysWithOldGenerationId(userId, generationId);
        }
        return result;
    }

    public void invalidateKeysWithOldGenerationId(int userId, int newGenerationId) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("recovery_status", Integer.valueOf(3));
        db.update("keys", values, "user_id = ? AND platform_key_generation_id < ?", new String[]{String.valueOf(userId), String.valueOf(newGenerationId)});
    }

    public void invalidateKeysForUserIdOnCustomScreenLock(int userId) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("recovery_status", Integer.valueOf(3));
        db.update("keys", values, "user_id = ?", new String[]{String.valueOf(userId)});
    }

    /* JADX WARNING: Missing block: B:17:0x004d, code skipped:
            if (r1 != null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:18:0x004f, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPlatformKeyGenerationId(int userId) {
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("user_metadata", new String[]{"platform_key_generation_id"}, "user_id = ?", new String[]{Integer.toString(userId)}, null, null, null);
        if (cursor2.getCount() == 0) {
            if (cursor2 != null) {
                $closeResource(null, cursor2);
            }
            return -1;
        }
        cursor2.moveToFirst();
        int i = cursor2.getInt(cursor2.getColumnIndexOrThrow("platform_key_generation_id"));
        if (cursor2 != null) {
            $closeResource(null, cursor2);
        }
        return i;
    }

    public long setRecoveryServicePublicKey(int userId, int uid, PublicKey publicKey) {
        return setBytes(userId, uid, "public_key", publicKey.getEncoded());
    }

    public Long getRecoveryServiceCertSerial(int userId, int uid, String rootAlias) {
        return getLong(userId, uid, rootAlias, "cert_serial");
    }

    public long setRecoveryServiceCertSerial(int userId, int uid, String rootAlias, long serial) {
        return setLong(userId, uid, rootAlias, "cert_serial", serial);
    }

    public CertPath getRecoveryServiceCertPath(int userId, int uid, String rootAlias) {
        byte[] bytes = getBytes(userId, uid, rootAlias, "cert_path");
        if (bytes == null) {
            return null;
        }
        try {
            return decodeCertPath(bytes);
        } catch (CertificateException e) {
            Log.wtf(TAG, String.format(Locale.US, "Recovery service CertPath entry cannot be decoded for userId=%d uid=%d.", new Object[]{Integer.valueOf(userId), Integer.valueOf(uid)}), e);
            return null;
        }
    }

    public long setRecoveryServiceCertPath(int userId, int uid, String rootAlias, CertPath certPath) throws CertificateEncodingException {
        if (certPath.getCertificates().size() != 0) {
            return setBytes(userId, uid, rootAlias, "cert_path", certPath.getEncoded(CERT_PATH_ENCODING));
        }
        throw new CertificateEncodingException("No certificate contained in the cert path.");
    }

    /* JADX WARNING: Missing block: B:14:0x0055, code skipped:
            if (r1 != null) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:15:0x0057, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<Integer> getRecoveryAgents(int userId) {
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("recovery_service_metadata", new String[]{"uid"}, "user_id = ?", new String[]{Integer.toString(userId)}, null, null, null);
        ArrayList<Integer> result = new ArrayList(cursor2.getCount());
        while (cursor2.moveToNext()) {
            result.add(Integer.valueOf(cursor2.getInt(cursor2.getColumnIndexOrThrow("uid"))));
        }
        if (cursor2 != null) {
            $closeResource(null, cursor2);
        }
        return result;
    }

    public PublicKey getRecoveryServicePublicKey(int userId, int uid) {
        byte[] keyBytes = getBytes(userId, uid, "public_key");
        if (keyBytes == null) {
            return null;
        }
        try {
            return decodeX509Key(keyBytes);
        } catch (InvalidKeySpecException e) {
            Log.wtf(TAG, String.format(Locale.US, "Recovery service public key entry cannot be decoded for userId=%d uid=%d.", new Object[]{Integer.valueOf(userId), Integer.valueOf(uid)}));
            return null;
        }
    }

    public long setRecoverySecretTypes(int userId, int uid, int[] secretTypes) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        StringJoiner joiner = new StringJoiner(",");
        Arrays.stream(secretTypes).forEach(new -$$Lambda$RecoverableKeyStoreDb$knfkhmVPS_11tGWkGt87bH4xjYg(joiner));
        values.put("secret_types", joiner.toString());
        ensureRecoveryServiceMetadataEntryExists(userId, uid);
        return (long) db.update("recovery_service_metadata", values, "user_id = ? AND uid = ?", new String[]{String.valueOf(userId), String.valueOf(uid)});
    }

    /* JADX WARNING: Failed to process nested try/catch */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int[] getRecoverySecretTypes(int userId, int uid) {
        Throwable th;
        Throwable th2;
        SQLiteDatabase db = this.mKeyStoreDbHelper.getReadableDatabase();
        String[] projection = new String[]{"_id", "user_id", "uid", "secret_types"};
        selectionArguments = new String[2];
        int i = 0;
        selectionArguments[0] = Integer.toString(userId);
        selectionArguments[1] = Integer.toString(uid);
        Cursor cursor = db.query("recovery_service_metadata", projection, "user_id = ? AND uid = ?", selectionArguments, null, null, null);
        try {
            int count = cursor.getCount();
            int[] iArr;
            if (count == 0) {
                iArr = new int[0];
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return iArr;
            } else if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d deviceId entries found for userId=%d uid=%d. Should only ever be 0 or 1.", new Object[]{Integer.valueOf(count), Integer.valueOf(userId), Integer.valueOf(uid)}));
                iArr = new int[0];
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return iArr;
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndexOrThrow("secret_types");
                if (cursor.isNull(idx)) {
                    iArr = new int[0];
                    if (cursor != null) {
                        $closeResource(null, cursor);
                    }
                    return iArr;
                }
                String csv = cursor.getString(idx);
                if (TextUtils.isEmpty(csv)) {
                    iArr = new int[0];
                    if (cursor != null) {
                        $closeResource(null, cursor);
                    }
                    return iArr;
                }
                String[] types = csv.split(",");
                int[] result = new int[types.length];
                while (i < types.length) {
                    try {
                        result[i] = Integer.parseInt(types[i]);
                    } catch (NumberFormatException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("String format error ");
                        stringBuilder.append(e);
                        Log.wtf(str, stringBuilder.toString());
                    }
                    i++;
                }
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return result;
            }
        } catch (Throwable th3) {
            th2 = th3;
        }
        if (cursor != null) {
            $closeResource(th, cursor);
        }
        throw th2;
    }

    public long setActiveRootOfTrust(int userId, int uid, String rootAlias) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("active_root_of_trust", rootAlias);
        ensureRecoveryServiceMetadataEntryExists(userId, uid);
        return (long) db.update("recovery_service_metadata", values, "user_id = ? AND uid = ?", new String[]{String.valueOf(userId), String.valueOf(uid)});
    }

    public String getActiveRootOfTrust(int userId, int uid) {
        Throwable th;
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("recovery_service_metadata", new String[]{"_id", "user_id", "uid", "active_root_of_trust"}, "user_id = ? AND uid = ?", new String[]{Integer.toString(userId), Integer.toString(uid)}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d deviceId entries found for userId=%d uid=%d. Should only ever be 0 or 1.", new Object[]{Integer.valueOf(count), Integer.valueOf(userId), Integer.valueOf(uid)}));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                int idx = cursor2.getColumnIndexOrThrow("active_root_of_trust");
                if (cursor2.isNull(idx)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                String result = cursor2.getString(idx);
                if (TextUtils.isEmpty(result)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return result;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    public long setCounterId(int userId, int uid, long counterId) {
        return setLong(userId, uid, "counter_id", counterId);
    }

    public Long getCounterId(int userId, int uid) {
        return getLong(userId, uid, "counter_id");
    }

    public long setServerParams(int userId, int uid, byte[] serverParams) {
        return setBytes(userId, uid, "server_params", serverParams);
    }

    public byte[] getServerParams(int userId, int uid) {
        return getBytes(userId, uid, "server_params");
    }

    public long setSnapshotVersion(int userId, int uid, long snapshotVersion) {
        return setLong(userId, uid, "snapshot_version", snapshotVersion);
    }

    public Long getSnapshotVersion(int userId, int uid) {
        return getLong(userId, uid, "snapshot_version");
    }

    public long setShouldCreateSnapshot(int userId, int uid, boolean pending) {
        return setLong(userId, uid, "should_create_snapshot", pending ? 1 : 0);
    }

    public boolean getShouldCreateSnapshot(int userId, int uid) {
        Long res = getLong(userId, uid, "should_create_snapshot");
        return (res == null || res.longValue() == 0) ? false : true;
    }

    private Long getLong(int userId, int uid, String key) {
        Throwable th;
        String str = key;
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("recovery_service_metadata", new String[]{"_id", "user_id", "uid", str}, "user_id = ? AND uid = ?", new String[]{Integer.toString(userId), Integer.toString(uid)}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", new Object[]{Integer.valueOf(count), Integer.valueOf(userId), Integer.valueOf(uid)}));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                int idx = cursor2.getColumnIndexOrThrow(str);
                if (cursor2.isNull(idx)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                Long valueOf = Long.valueOf(cursor2.getLong(idx));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return valueOf;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    private long setLong(int userId, int uid, String key, long value) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, Long.valueOf(value));
        String[] selectionArguments = new String[]{Integer.toString(userId), Integer.toString(uid)};
        ensureRecoveryServiceMetadataEntryExists(userId, uid);
        return (long) db.update("recovery_service_metadata", values, "user_id = ? AND uid = ?", selectionArguments);
    }

    private byte[] getBytes(int userId, int uid, String key) {
        Throwable th;
        String str = key;
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        Cursor cursor2 = cursor.query("recovery_service_metadata", new String[]{"_id", "user_id", "uid", str}, "user_id = ? AND uid = ?", new String[]{Integer.toString(userId), Integer.toString(uid)}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                Log.wtf(TAG, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", new Object[]{Integer.valueOf(count), Integer.valueOf(userId), Integer.valueOf(uid)}));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                int idx = cursor2.getColumnIndexOrThrow(str);
                if (cursor2.isNull(idx)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                byte[] blob = cursor2.getBlob(idx);
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return blob;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    private long setBytes(int userId, int uid, String key, byte[] value) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, value);
        String[] selectionArguments = new String[]{Integer.toString(userId), Integer.toString(uid)};
        ensureRecoveryServiceMetadataEntryExists(userId, uid);
        return (long) db.update("recovery_service_metadata", values, "user_id = ? AND uid = ?", selectionArguments);
    }

    private byte[] getBytes(int userId, int uid, String rootAlias, String key) {
        Throwable th;
        String str = key;
        String rootAlias2 = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(rootAlias);
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        int i = 3;
        Cursor cursor2 = cursor.query("root_of_trust", new String[]{"_id", "user_id", "uid", "root_alias", str}, "user_id = ? AND uid = ? AND root_alias = ?", new String[]{Integer.toString(userId), Integer.toString(uid), rootAlias2}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                String str2 = TAG;
                Object[] objArr = new Object[i];
                objArr[0] = Integer.valueOf(count);
                objArr[1] = Integer.valueOf(userId);
                objArr[2] = Integer.valueOf(uid);
                Log.wtf(str2, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", objArr));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                int idx = cursor2.getColumnIndexOrThrow(str);
                if (cursor2.isNull(idx)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                byte[] blob = cursor2.getBlob(idx);
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return blob;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    private long setBytes(int userId, int uid, String rootAlias, String key, byte[] value) {
        rootAlias = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(rootAlias);
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, value);
        String[] selectionArguments = new String[]{Integer.toString(userId), Integer.toString(uid), rootAlias};
        ensureRootOfTrustEntryExists(userId, uid, rootAlias);
        return (long) db.update("root_of_trust", values, "user_id = ? AND uid = ? AND root_alias = ?", selectionArguments);
    }

    private Long getLong(int userId, int uid, String rootAlias, String key) {
        Throwable th;
        String str = key;
        String rootAlias2 = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(rootAlias);
        SQLiteDatabase cursor = this.mKeyStoreDbHelper.getReadableDatabase();
        int i = 3;
        Cursor cursor2 = cursor.query("root_of_trust", new String[]{"_id", "user_id", "uid", "root_alias", str}, "user_id = ? AND uid = ? AND root_alias = ?", new String[]{Integer.toString(userId), Integer.toString(uid), rootAlias2}, null, null, null);
        try {
            int count = cursor2.getCount();
            if (count == 0) {
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else if (count > 1) {
                String str2 = TAG;
                Object[] objArr = new Object[i];
                objArr[0] = Integer.valueOf(count);
                objArr[1] = Integer.valueOf(userId);
                objArr[2] = Integer.valueOf(uid);
                Log.wtf(str2, String.format(Locale.US, "%d entries found for userId=%d uid=%d. Should only ever be 0 or 1.", objArr));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return null;
            } else {
                cursor2.moveToFirst();
                int idx = cursor2.getColumnIndexOrThrow(str);
                if (cursor2.isNull(idx)) {
                    if (cursor2 != null) {
                        $closeResource(null, cursor2);
                    }
                    return null;
                }
                Long valueOf = Long.valueOf(cursor2.getLong(idx));
                if (cursor2 != null) {
                    $closeResource(null, cursor2);
                }
                return valueOf;
            }
        } catch (Throwable th2) {
            if (cursor2 != null) {
                $closeResource(th, cursor2);
            }
        }
    }

    private long setLong(int userId, int uid, String rootAlias, String key, long value) {
        rootAlias = this.mTestOnlyInsecureCertificateHelper.getDefaultCertificateAliasIfEmpty(rootAlias);
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(key, Long.valueOf(value));
        String[] selectionArguments = new String[]{Integer.toString(userId), Integer.toString(uid), rootAlias};
        ensureRootOfTrustEntryExists(userId, uid, rootAlias);
        return (long) db.update("root_of_trust", values, "user_id = ? AND uid = ? AND root_alias = ?", selectionArguments);
    }

    private void ensureRecoveryServiceMetadataEntryExists(int userId, int uid) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", Integer.valueOf(userId));
        values.put("uid", Integer.valueOf(uid));
        db.insertWithOnConflict("recovery_service_metadata", null, values, 4);
    }

    private void ensureRootOfTrustEntryExists(int userId, int uid, String rootAlias) {
        SQLiteDatabase db = this.mKeyStoreDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", Integer.valueOf(userId));
        values.put("uid", Integer.valueOf(uid));
        values.put("root_alias", rootAlias);
        db.insertWithOnConflict("root_of_trust", null, values, 4);
    }

    public void close() {
        this.mKeyStoreDbHelper.close();
    }

    private static PublicKey decodeX509Key(byte[] keyBytes) throws InvalidKeySpecException {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static CertPath decodeCertPath(byte[] bytes) throws CertificateException {
        try {
            return CertificateFactory.getInstance("X.509").generateCertPath(new ByteArrayInputStream(bytes), CERT_PATH_ENCODING);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }
}

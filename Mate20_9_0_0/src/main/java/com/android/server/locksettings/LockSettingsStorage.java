package com.android.server.locksettings;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.PersistentDataBlockManagerInternal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class LockSettingsStorage {
    private static final String BASE_ZERO_LOCK_PATTERN_FILE = "gatekeeper.gesture.key";
    private static final String CHILD_PROFILE_LOCK_FILE = "gatekeeper.profile.key";
    private static final String[] COLUMNS_FOR_PREFETCH = new String[]{"name", COLUMN_VALUE};
    private static final String[] COLUMNS_FOR_QUERY = new String[]{COLUMN_VALUE};
    private static final String COLUMN_KEY = "name";
    private static final String COLUMN_USERID = "user";
    private static final String COLUMN_VALUE = "value";
    private static final boolean DEBUG = false;
    private static final Object DEFAULT = new Object();
    private static final String LEGACY_LOCK_PASSWORD_FILE = "password.key";
    private static final String LEGACY_LOCK_PATTERN_FILE = "gesture.key";
    private static final String LOCK_PASSWORD_FILE = "gatekeeper.password.key";
    private static final String LOCK_PATTERN_FILE = "gatekeeper.pattern.key";
    private static final String SYNTHETIC_PASSWORD_DIRECTORY = "spblob/";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "LockSettingsStorage";
    private final Cache mCache = new Cache();
    private final Context mContext;
    private final Object mFileWriteLock = new Object();
    private final DatabaseHelper mOpenHelper;
    private PersistentDataBlockManagerInternal mPersistentDataBlockManagerInternal;

    private static class Cache {
        private final ArrayMap<CacheKey, Object> mCache;
        private final CacheKey mCacheKey;
        private int mVersion;

        private static final class CacheKey {
            static final int TYPE_FETCHED = 2;
            static final int TYPE_FILE = 1;
            static final int TYPE_KEY_VALUE = 0;
            String key;
            int type;
            int userId;

            private CacheKey() {
            }

            public CacheKey set(int type, String key, int userId) {
                this.type = type;
                this.key = key;
                this.userId = userId;
                return this;
            }

            public boolean equals(Object obj) {
                boolean z = false;
                if (!(obj instanceof CacheKey)) {
                    return false;
                }
                CacheKey o = (CacheKey) obj;
                if (this.userId == o.userId && this.type == o.type && this.key.equals(o.key)) {
                    z = true;
                }
                return z;
            }

            public int hashCode() {
                return (this.key.hashCode() ^ this.userId) ^ this.type;
            }
        }

        private Cache() {
            this.mCache = new ArrayMap();
            this.mCacheKey = new CacheKey();
            this.mVersion = 0;
        }

        String peekKeyValue(String key, String defaultValue, int userId) {
            Object cached = peek(null, key, userId);
            return cached == LockSettingsStorage.DEFAULT ? defaultValue : (String) cached;
        }

        boolean hasKeyValue(String key, int userId) {
            return contains(0, key, userId);
        }

        void putKeyValue(String key, String value, int userId) {
            put(0, key, value, userId);
        }

        void putKeyValueIfUnchanged(String key, Object value, int userId, int version) {
            putIfUnchanged(0, key, value, userId, version);
        }

        byte[] peekFile(String fileName) {
            return (byte[]) peek(1, fileName, -1);
        }

        boolean hasFile(String fileName) {
            return contains(1, fileName, -1);
        }

        void putFile(String key, byte[] value) {
            put(1, key, value, -1);
        }

        void putFileIfUnchanged(String key, byte[] value, int version) {
            putIfUnchanged(1, key, value, -1, version);
        }

        void setFetched(int userId) {
            put(2, "isFetched", "true", userId);
        }

        boolean isFetched(int userId) {
            return contains(2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, userId);
        }

        private synchronized void put(int type, String key, Object value, int userId) {
            this.mCache.put(new CacheKey().set(type, key, userId), value);
            this.mVersion++;
        }

        private synchronized void putIfUnchanged(int type, String key, Object value, int userId, int version) {
            if (!contains(type, key, userId) && this.mVersion == version) {
                put(type, key, value, userId);
            }
        }

        private synchronized boolean contains(int type, String key, int userId) {
            return this.mCache.containsKey(this.mCacheKey.set(type, key, userId));
        }

        private synchronized Object peek(int type, String key, int userId) {
            return this.mCache.get(this.mCacheKey.set(type, key, userId));
        }

        private synchronized int getVersion() {
            return this.mVersion;
        }

        synchronized void removeUser(int userId) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                if (((CacheKey) this.mCache.keyAt(i)).userId == userId) {
                    this.mCache.removeAt(i);
                }
            }
            this.mVersion++;
        }

        synchronized void purgePath(String path) {
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                CacheKey entry = (CacheKey) this.mCache.keyAt(i);
                if (entry.type == 1 && entry.key.startsWith(path)) {
                    this.mCache.removeAt(i);
                }
            }
            this.mVersion++;
        }

        synchronized void clear() {
            this.mCache.clear();
            this.mVersion++;
        }
    }

    public interface Callback {
        void initialize(SQLiteDatabase sQLiteDatabase);
    }

    @VisibleForTesting
    public static class CredentialHash {
        static final int VERSION_GATEKEEPER = 1;
        static final int VERSION_LEGACY = 0;
        byte[] hash;
        boolean isBaseZeroPattern;
        int type;
        int version;

        private CredentialHash(byte[] hash, int type, int version) {
            this(hash, type, version, false);
        }

        private CredentialHash(byte[] hash, int type, int version, boolean isBaseZeroPattern) {
            if (type != -1) {
                if (hash == null) {
                    throw new RuntimeException("Empty hash for CredentialHash");
                }
            } else if (hash != null) {
                throw new RuntimeException("None type CredentialHash should not have hash");
            }
            this.hash = hash;
            this.type = type;
            this.version = version;
            this.isBaseZeroPattern = isBaseZeroPattern;
        }

        private static CredentialHash createBaseZeroPattern(byte[] hash) {
            return new CredentialHash(hash, 1, 1, true);
        }

        static CredentialHash create(byte[] hash, int type) {
            if (type != -1) {
                return new CredentialHash(hash, type, 1);
            }
            throw new RuntimeException("Bad type for CredentialHash");
        }

        static CredentialHash createEmptyHash() {
            return new CredentialHash(null, -1, 1);
        }

        public byte[] toBytes() {
            Preconditions.checkState(this.isBaseZeroPattern ^ 1, "base zero patterns are not serializable");
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.write(this.version);
                dos.write(this.type);
                if (this.hash == null || this.hash.length <= 0) {
                    dos.writeInt(0);
                } else {
                    dos.writeInt(this.hash.length);
                    dos.write(this.hash);
                }
                dos.close();
                return os.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static CredentialHash fromBytes(byte[] bytes) {
            try {
                DataInputStream is = new DataInputStream(new ByteArrayInputStream(bytes));
                int version = is.read();
                int type = is.read();
                int hashSize = is.readInt();
                byte[] hash = null;
                if (hashSize > 0) {
                    hash = new byte[hashSize];
                    is.readFully(hash);
                }
                return new CredentialHash(hash, type, version);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "locksettings.db";
        private static final int DATABASE_VERSION = 2;
        private static final int IDLE_CONNECTION_TIMEOUT_MS = 30000;
        private static final String TAG = "LockSettingsDB";
        private Callback mCallback;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 2);
            setWriteAheadLoggingEnabled(true);
            setIdleConnectionTimeout(30000);
        }

        public void setCallback(Callback callback) {
            this.mCallback = callback;
        }

        private void createTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE locksettings (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,user INTEGER,value TEXT);");
        }

        public void onCreate(SQLiteDatabase db) {
            createTable(db);
            if (this.mCallback != null) {
                this.mCallback.initialize(db);
            }
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            int upgradeVersion = oldVersion;
            if (upgradeVersion == 1) {
                upgradeVersion = 2;
            }
            if (upgradeVersion != 2) {
                Log.w(TAG, "Failed to upgrade database!");
            }
        }
    }

    public static class PersistentData {
        public static final PersistentData NONE = new PersistentData(0, -10000, 0, null);
        public static final int TYPE_NONE = 0;
        public static final int TYPE_SP = 1;
        public static final int TYPE_SP_WEAVER = 2;
        static final byte VERSION_1 = (byte) 1;
        static final int VERSION_1_HEADER_SIZE = 10;
        final byte[] payload;
        final int qualityForUi;
        final int type;
        final int userId;

        private PersistentData(int type, int userId, int qualityForUi, byte[] payload) {
            this.type = type;
            this.userId = userId;
            this.qualityForUi = qualityForUi;
            this.payload = payload;
        }

        public static PersistentData fromBytes(byte[] frpData) {
            if (frpData == null || frpData.length == 0) {
                return NONE;
            }
            DataInputStream is = new DataInputStream(new ByteArrayInputStream(frpData));
            try {
                byte version = is.readByte();
                if (version == (byte) 1) {
                    int type = is.readByte() & 255;
                    int userId = is.readInt();
                    int qualityForUi = is.readInt();
                    byte[] payload = new byte[(frpData.length - 10)];
                    System.arraycopy(frpData, 10, payload, 0, payload.length);
                    return new PersistentData(type, userId, qualityForUi, payload);
                }
                String str = LockSettingsStorage.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown PersistentData version code: ");
                stringBuilder.append(version);
                Slog.wtf(str, stringBuilder.toString());
                return NONE;
            } catch (IOException e) {
                Slog.wtf(LockSettingsStorage.TAG, "Could not parse PersistentData", e);
                return NONE;
            }
        }

        public static byte[] toBytes(int persistentType, int userId, int qualityForUi, byte[] payload) {
            boolean z = false;
            if (persistentType == 0) {
                if (payload == null) {
                    z = true;
                }
                Preconditions.checkArgument(z, "TYPE_NONE must have empty payload");
                return null;
            }
            if (payload != null && payload.length > 0) {
                z = true;
            }
            Preconditions.checkArgument(z, "empty payload must only be used with TYPE_NONE");
            ByteArrayOutputStream os = new ByteArrayOutputStream(10 + payload.length);
            DataOutputStream dos = new DataOutputStream(os);
            try {
                dos.writeByte(1);
                dos.writeByte(persistentType);
                dos.writeInt(userId);
                dos.writeInt(qualityForUi);
                dos.write(payload);
                return os.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("ByteArrayOutputStream cannot throw IOException");
            }
        }
    }

    public LockSettingsStorage(Context context) {
        this.mContext = context;
        this.mOpenHelper = new DatabaseHelper(context);
    }

    public void setDatabaseOnCreateCallback(Callback callback) {
        this.mOpenHelper.setCallback(callback);
    }

    public void writeKeyValue(String key, String value, int userId) {
        writeKeyValue(this.mOpenHelper.getWritableDatabase(), key, value, userId);
    }

    public void writeKeyValue(SQLiteDatabase db, String key, String value, int userId) {
        ContentValues cv = new ContentValues();
        cv.put("name", key);
        cv.put(COLUMN_USERID, Integer.valueOf(userId));
        cv.put(COLUMN_VALUE, value);
        db.beginTransaction();
        try {
            db.delete(TABLE, "name=? AND user=?", new String[]{key, Integer.toString(userId)});
            db.insert(TABLE, null, cv);
            db.setTransactionSuccessful();
            this.mCache.putKeyValue(key, value, userId);
        } finally {
            db.endTransaction();
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001a, code:
            r0 = DEFAULT;
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r2 = r12.mOpenHelper.getReadableDatabase().query(TABLE, COLUMNS_FOR_QUERY, "user=? AND name=?", new java.lang.String[]{java.lang.Integer.toString(r15), r13}, null, null, null);
            r4 = r2;
     */
    /* JADX WARNING: Missing block: B:13:0x003f, code:
            if (r2 == null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:15:0x0045, code:
            if (r4.moveToFirst() == false) goto L_0x004c;
     */
    /* JADX WARNING: Missing block: B:16:0x0047, code:
            r0 = r4.getString(0);
     */
    /* JADX WARNING: Missing block: B:17:0x004c, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:18:0x0050, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:19:0x0051, code:
            android.util.Log.w(TAG, "readKeyValue got err:", r2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String readKeyValue(String key, String defaultValue, int userId) {
        int version;
        synchronized (this.mCache) {
            if (this.mCache.hasKeyValue(key, userId)) {
                String peekKeyValue = this.mCache.peekKeyValue(key, defaultValue, userId);
                return peekKeyValue;
            }
            version = this.mCache.getVersion();
        }
        this.mCache.putKeyValueIfUnchanged(key, result, userId, version);
        return result == DEFAULT ? defaultValue : (String) result;
    }

    /* JADX WARNING: Missing block: B:10:?, code:
            r3 = r11.mOpenHelper.getReadableDatabase().query(TABLE, COLUMNS_FOR_PREFETCH, "user=?", new java.lang.String[]{java.lang.Integer.toString(r12)}, null, null, null);
            r4 = r3;
     */
    /* JADX WARNING: Missing block: B:11:0x0039, code:
            if (r3 == null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:13:0x003f, code:
            if (r4.moveToNext() == false) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:14:0x0041, code:
            r11.mCache.putKeyValueIfUnchanged(r4.getString(0), r4.getString(1), r12, r1);
     */
    /* JADX WARNING: Missing block: B:15:0x004f, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:16:0x0053, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:17:0x0054, code:
            android.util.Log.w(TAG, "prefetchUser got err:", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void prefetchUser(int userId) {
        synchronized (this.mCache) {
            if (this.mCache.isFetched(userId)) {
                return;
            } else {
                this.mCache.setFetched(userId);
                int version = this.mCache.getVersion();
            }
        }
        readCredentialHash(userId);
    }

    private CredentialHash readPasswordHashIfExists(int userId) {
        byte[] stored = readFile(getLockPasswordFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return new CredentialHash(stored, 2, 1, null);
        }
        stored = readFile(getLegacyLockPasswordFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return new CredentialHash(stored, 2, 0, null);
        }
        Log.i(TAG, "readPatternHash , cannot get any PasswordHash");
        return null;
    }

    private CredentialHash readPatternHashIfExists(int userId) {
        byte[] stored = readFile(getLockPatternFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return new CredentialHash(stored, 1, 1, null);
        }
        stored = readFile(getBaseZeroLockPatternFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return CredentialHash.createBaseZeroPattern(stored);
        }
        stored = readFile(getLegacyLockPatternFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return new CredentialHash(stored, 1, 0, null);
        }
        Log.i(TAG, "readPatternHash , cannot get any PatternHash");
        return null;
    }

    public CredentialHash readCredentialHash(int userId) {
        CredentialHash passwordHash = readPasswordHashIfExists(userId);
        CredentialHash patternHash = readPatternHashIfExists(userId);
        if (passwordHash == null || patternHash == null) {
            if (passwordHash != null) {
                return passwordHash;
            }
            if (patternHash != null) {
                return patternHash;
            }
            return CredentialHash.createEmptyHash();
        } else if (passwordHash.version == 1) {
            return passwordHash;
        } else {
            return patternHash;
        }
    }

    public void removeChildProfileLock(int userId) {
        try {
            deleteFile(getChildProfileLockFile(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeChildProfileLock(int userId, byte[] lock) {
        writeFile(getChildProfileLockFile(userId), lock);
    }

    public byte[] readChildProfileLock(int userId) {
        return readFile(getChildProfileLockFile(userId));
    }

    public boolean hasChildProfileLock(int userId) {
        return hasFile(getChildProfileLockFile(userId));
    }

    public boolean hasPassword(int userId) {
        return hasFile(getLockPasswordFilename(userId)) || hasFile(getLegacyLockPasswordFilename(userId));
    }

    public boolean hasPattern(int userId) {
        return hasFile(getLockPatternFilename(userId)) || hasFile(getBaseZeroLockPatternFilename(userId)) || hasFile(getLegacyLockPatternFilename(userId));
    }

    public boolean hasCredential(int userId) {
        return hasPassword(userId) || hasPattern(userId);
    }

    protected boolean hasFile(String name) {
        byte[] contents = readFile(name);
        return contents != null && contents.length > 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x008e  */
    /* JADX WARNING: Missing block: B:12:0x0028, code:
            r0 = null;
            r2 = null;
     */
    /* JADX WARNING: Missing block: B:14:?, code:
            r0 = new java.io.RandomAccessFile(r8, "r");
            r2 = new byte[((int) r0.length())];
            r0.readFully(r2, 0, r2.length);
            r0.close();
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0048, code:
            r3 = e;
     */
    /* JADX WARNING: Missing block: B:18:0x0049, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Missing block: B:21:0x0062, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:23:?, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Cannot read file ");
            r5.append(r3);
            android.util.Slog.e(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x0079, code:
            if (r0 != null) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:26:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:27:0x007f, code:
            r3 = e;
     */
    /* JADX WARNING: Missing block: B:28:0x0080, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Missing block: B:31:0x008e, code:
            dumpFileInfo(r8);
     */
    /* JADX WARNING: Missing block: B:34:0x0097, code:
            if (r0 != null) goto L_0x0099;
     */
    /* JADX WARNING: Missing block: B:36:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:37:0x009d, code:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:38:0x009e, code:
            r5 = new java.lang.StringBuilder();
            r5.append("Error closing file ");
            r5.append(r4);
            android.util.Slog.e(TAG, r5.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected byte[] readFile(String name) {
        int version;
        synchronized (this.mCache) {
            if (this.mCache.hasFile(name)) {
                byte[] cached = this.mCache.peekFile(name);
                if (ArrayUtils.isEmpty(cached)) {
                    Slog.e(TAG, "read file from cache is empty.");
                } else {
                    return cached;
                }
            }
            version = this.mCache.getVersion();
        }
        r5.append("Error closing file ");
        r5.append(e);
        Slog.e(r4, r5.toString());
        if (ArrayUtils.isEmpty(stored)) {
        }
        this.mCache.putFileIfUnchanged(name, stored, version);
        return stored;
    }

    private void dumpFileInfo(String name) {
        File f = new File(name);
        if (f.exists() && f.isFile()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("size of file:");
            stringBuilder.append(name);
            stringBuilder.append(" = ");
            stringBuilder.append(f.length());
            stringBuilder.append("; readable: ");
            stringBuilder.append(f.canRead());
            stringBuilder.append("; last modified ");
            stringBuilder.append(f.lastModified());
            Slog.e(str, stringBuilder.toString());
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.locksettings.LockSettingsStorage.writeFile(java.lang.String, byte[]):void, dom blocks: [B:11:0x0022, B:19:0x0041]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void writeFile(java.lang.String r8, byte[] r9) {
        /*
        r7 = this;
        r0 = r7.mFileWriteLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = new java.io.RandomAccessFile;	 Catch:{ IOException -> 0x0040 }
        r3 = "rws";	 Catch:{ IOException -> 0x0040 }
        r2.<init>(r8, r3);	 Catch:{ IOException -> 0x0040 }
        r1 = r2;	 Catch:{ IOException -> 0x0040 }
        if (r9 == 0) goto L_0x0019;	 Catch:{ IOException -> 0x0040 }
    L_0x000f:
        r2 = r9.length;	 Catch:{ IOException -> 0x0040 }
        if (r2 != 0) goto L_0x0013;	 Catch:{ IOException -> 0x0040 }
    L_0x0012:
        goto L_0x0019;	 Catch:{ IOException -> 0x0040 }
    L_0x0013:
        r2 = 0;	 Catch:{ IOException -> 0x0040 }
        r3 = r9.length;	 Catch:{ IOException -> 0x0040 }
        r1.write(r9, r2, r3);	 Catch:{ IOException -> 0x0040 }
        goto L_0x001e;	 Catch:{ IOException -> 0x0040 }
    L_0x0019:
        r2 = 0;	 Catch:{ IOException -> 0x0040 }
        r1.setLength(r2);	 Catch:{ IOException -> 0x0040 }
    L_0x001e:
        r1.close();	 Catch:{ IOException -> 0x0040 }
        r1.close();	 Catch:{ IOException -> 0x0026 }
    L_0x0025:
        goto L_0x0072;
    L_0x0026:
        r2 = move-exception;
        r3 = "LockSettingsStorage";	 Catch:{ all -> 0x007f }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x007f }
        r4.<init>();	 Catch:{ all -> 0x007f }
        r5 = "Error closing file ";	 Catch:{ all -> 0x007f }
        r4.append(r5);	 Catch:{ all -> 0x007f }
        r4.append(r2);	 Catch:{ all -> 0x007f }
        r4 = r4.toString();	 Catch:{ all -> 0x007f }
    L_0x003a:
        android.util.Slog.e(r3, r4);	 Catch:{ all -> 0x007f }
        goto L_0x0025;
    L_0x003e:
        r2 = move-exception;
        goto L_0x0079;
    L_0x0040:
        r2 = move-exception;
        r3 = "LockSettingsStorage";	 Catch:{ all -> 0x003e }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x003e }
        r4.<init>();	 Catch:{ all -> 0x003e }
        r5 = "Error writing to file ";	 Catch:{ all -> 0x003e }
        r4.append(r5);	 Catch:{ all -> 0x003e }
        r4.append(r2);	 Catch:{ all -> 0x003e }
        r4 = r4.toString();	 Catch:{ all -> 0x003e }
        android.util.Slog.e(r3, r4);	 Catch:{ all -> 0x003e }
        if (r1 == 0) goto L_0x0072;
    L_0x0059:
        r1.close();	 Catch:{ IOException -> 0x005d }
        goto L_0x0025;
    L_0x005d:
        r2 = move-exception;
        r3 = "LockSettingsStorage";	 Catch:{ all -> 0x007f }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x007f }
        r4.<init>();	 Catch:{ all -> 0x007f }
        r5 = "Error closing file ";	 Catch:{ all -> 0x007f }
        r4.append(r5);	 Catch:{ all -> 0x007f }
        r4.append(r2);	 Catch:{ all -> 0x007f }
        r4 = r4.toString();	 Catch:{ all -> 0x007f }
        goto L_0x003a;	 Catch:{ all -> 0x007f }
    L_0x0072:
        r2 = r7.mCache;	 Catch:{ all -> 0x007f }
        r2.putFile(r8, r9);	 Catch:{ all -> 0x007f }
        monitor-exit(r0);	 Catch:{ all -> 0x007f }
        return;
    L_0x0079:
        if (r1 == 0) goto L_0x0098;
    L_0x007b:
        r1.close();	 Catch:{ IOException -> 0x0081 }
        goto L_0x0098;
    L_0x007f:
        r1 = move-exception;
        goto L_0x0099;
    L_0x0081:
        r3 = move-exception;
        r4 = "LockSettingsStorage";	 Catch:{ all -> 0x007f }
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x007f }
        r5.<init>();	 Catch:{ all -> 0x007f }
        r6 = "Error closing file ";	 Catch:{ all -> 0x007f }
        r5.append(r6);	 Catch:{ all -> 0x007f }
        r5.append(r3);	 Catch:{ all -> 0x007f }
        r5 = r5.toString();	 Catch:{ all -> 0x007f }
        android.util.Slog.e(r4, r5);	 Catch:{ all -> 0x007f }
    L_0x0098:
        throw r2;	 Catch:{ all -> 0x007f }
    L_0x0099:
        monitor-exit(r0);	 Catch:{ all -> 0x007f }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.locksettings.LockSettingsStorage.writeFile(java.lang.String, byte[]):void");
    }

    protected void deleteFile(String name) {
        synchronized (this.mFileWriteLock) {
            File file = new File(name);
            if (file.exists()) {
                file.delete();
                this.mCache.putFile(name, null);
            }
        }
    }

    public void writeCredentialHash(CredentialHash hash, int userId) {
        byte[] patternHash = null;
        byte[] passwordHash = null;
        if (hash.type == 2) {
            passwordHash = hash.hash;
        } else if (hash.type == 1) {
            patternHash = hash.hash;
        }
        writeFile(getLockPasswordFilename(userId), passwordHash);
        writeFile(getLockPatternFilename(userId), patternHash);
    }

    @VisibleForTesting
    String getLockPatternFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LOCK_PASSWORD_FILE);
    }

    @VisibleForTesting
    String getLegacyLockPatternFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LEGACY_LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getLegacyLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, LEGACY_LOCK_PASSWORD_FILE);
    }

    private String getBaseZeroLockPatternFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, BASE_ZERO_LOCK_PATTERN_FILE);
    }

    @VisibleForTesting
    String getChildProfileLockFile(int userId) {
        return getLockCredentialFilePathForUser(userId, CHILD_PROFILE_LOCK_FILE);
    }

    protected String getLockCredentialFilePathForUser(int userId, String basename) {
        String dataSystemDirectory = new StringBuilder();
        dataSystemDirectory.append(Environment.getDataDirectory().getAbsolutePath());
        dataSystemDirectory.append("/system/");
        dataSystemDirectory = dataSystemDirectory.toString();
        if (userId != 0) {
            return new File(Environment.getUserSystemDirectory(userId), basename).getAbsolutePath();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dataSystemDirectory);
        stringBuilder.append(basename);
        return stringBuilder.toString();
    }

    public void writeSyntheticPasswordState(int userId, long handle, String name, byte[] data) {
        ensureSyntheticPasswordDirectoryForUser(userId);
        writeFile(getSynthenticPasswordStateFilePathForUser(userId, handle, name), data);
    }

    public byte[] readSyntheticPasswordState(int userId, long handle, String name) {
        return readFile(getSynthenticPasswordStateFilePathForUser(userId, handle, name));
    }

    public void deleteSyntheticPasswordState(int userId, long handle, String name) {
        RandomAccessFile raf;
        Throwable th;
        Throwable th2;
        String path = getSynthenticPasswordStateFilePathForUser(userId, handle, name);
        File file = new File(path);
        if (file.exists()) {
            try {
                raf = new RandomAccessFile(path, "rws");
                try {
                    raf.write(new byte[((int) raf.length())]);
                    raf.close();
                    file.delete();
                    this.mCache.putFile(path, null);
                    return;
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to zeroize ");
                stringBuilder.append(path);
                Slog.w(str, stringBuilder.toString(), e);
            } catch (Throwable th4) {
                file.delete();
            }
        } else {
            return;
        }
        throw th;
        if (th22 != null) {
            try {
                raf.close();
            } catch (Throwable th5) {
                th22.addSuppressed(th5);
            }
        } else {
            raf.close();
        }
        throw th;
    }

    public Map<Integer, List<Long>> listSyntheticPasswordHandlesForAllUsers(String stateName) {
        Map<Integer, List<Long>> result = new ArrayMap();
        for (UserInfo user : UserManager.get(this.mContext).getUsers(false)) {
            result.put(Integer.valueOf(user.id), listSyntheticPasswordHandlesForUser(stateName, user.id));
        }
        return result;
    }

    public List<Long> listSyntheticPasswordHandlesForUser(String stateName, int userId) {
        File baseDir = getSyntheticPasswordDirectoryForUser(userId);
        List<Long> result = new ArrayList();
        File[] files = baseDir.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            String[] parts = file.getName().split("\\.");
            if (parts.length == 2 && parts[1].equals(stateName)) {
                try {
                    result.add(Long.valueOf(Long.parseUnsignedLong(parts[0], 16)));
                } catch (NumberFormatException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse handle ");
                    stringBuilder.append(parts[0]);
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }
        return result;
    }

    @VisibleForTesting
    protected File getSyntheticPasswordDirectoryForUser(int userId) {
        return new File(Environment.getDataSystemDeDirectory(userId), SYNTHETIC_PASSWORD_DIRECTORY);
    }

    private void ensureSyntheticPasswordDirectoryForUser(int userId) {
        File baseDir = getSyntheticPasswordDirectoryForUser(userId);
        if (!baseDir.exists()) {
            if (userId == 2147483646) {
                Log.w(TAG, "Parentcontrol doesn't have userinfo, using mkdirs instead!");
                baseDir.mkdirs();
                return;
            }
            baseDir.mkdir();
        }
    }

    @VisibleForTesting
    protected String getSynthenticPasswordStateFilePathForUser(int userId, long handle, String name) {
        return new File(getSyntheticPasswordDirectoryForUser(userId), String.format("%016x.%s", new Object[]{Long.valueOf(handle), name})).getAbsolutePath();
    }

    public void removeUser(int userId) {
        String name;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        if (((UserManager) this.mContext.getSystemService(COLUMN_USERID)).getProfileParent(userId) == null) {
            synchronized (this.mFileWriteLock) {
                name = getLockPasswordFilename(userId);
                File file = new File(name);
                if (file.exists()) {
                    file.delete();
                    this.mCache.putFile(name, null);
                }
                name = getLockPatternFilename(userId);
                file = new File(name);
                if (file.exists()) {
                    file.delete();
                    this.mCache.putFile(name, null);
                }
            }
        } else {
            removeChildProfileLock(userId);
        }
        File spStateDir = getSyntheticPasswordDirectoryForUser(userId);
        try {
            db.beginTransaction();
            name = TABLE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("user='");
            stringBuilder.append(userId);
            stringBuilder.append("'");
            db.delete(name, stringBuilder.toString(), null);
            db.setTransactionSuccessful();
            this.mCache.removeUser(userId);
            this.mCache.purgePath(spStateDir.getAbsolutePath());
        } finally {
            db.endTransaction();
        }
    }

    @VisibleForTesting
    void closeDatabase() {
        this.mOpenHelper.close();
    }

    @VisibleForTesting
    void clearCache() {
        this.mCache.clear();
    }

    public PersistentDataBlockManagerInternal getPersistentDataBlock() {
        if (this.mPersistentDataBlockManagerInternal == null) {
            this.mPersistentDataBlockManagerInternal = (PersistentDataBlockManagerInternal) LocalServices.getService(PersistentDataBlockManagerInternal.class);
        }
        return this.mPersistentDataBlockManagerInternal;
    }

    public void writePersistentDataBlock(int persistentType, int userId, int qualityForUi, byte[] payload) {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlock();
        if (persistentDataBlock != null) {
            persistentDataBlock.setFrpCredentialHandle(PersistentData.toBytes(persistentType, userId, qualityForUi, payload));
        }
    }

    public PersistentData readPersistentDataBlock() {
        PersistentDataBlockManagerInternal persistentDataBlock = getPersistentDataBlock();
        if (persistentDataBlock == null) {
            return PersistentData.NONE;
        }
        try {
            return PersistentData.fromBytes(persistentDataBlock.getFrpCredentialHandle());
        } catch (IllegalStateException e) {
            Slog.e(TAG, "Error reading persistent data block", e);
            return PersistentData.NONE;
        }
    }

    protected String getLockCredentialFilePathForUser2(int userId, String basename) {
        return getLockCredentialFilePathForUser(userId, basename);
    }
}

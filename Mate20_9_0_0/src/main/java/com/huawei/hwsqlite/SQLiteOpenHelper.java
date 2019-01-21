package com.huawei.hwsqlite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import com.huawei.hwsqlite.SQLiteDatabase.CursorFactory;
import java.io.File;
import java.util.Arrays;

public abstract class SQLiteOpenHelper {
    private static final boolean DEBUG_STRICT_READONLY = false;
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();
    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private boolean mEnableWriteAheadLogging;
    private byte[] mEncryptKey;
    private final SQLiteErrorHandler mErrorHandler;
    private final CursorFactory mFactory;
    private boolean mIsInitializing;
    private final int mMinimumSupportedVersion;
    private final String mName;
    private final int mNewVersion;
    private int mOpenFlags;

    public abstract void onCreate(SQLiteDatabase sQLiteDatabase);

    public abstract void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2);

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, SQLiteErrorHandler errorHandler) {
        this(context, name, factory, version, 0, errorHandler);
    }

    @SuppressLint({"AvoidMax/Min"})
    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, int minimumSupportedVersion, SQLiteErrorHandler errorHandler) {
        if (version >= 1) {
            this.mContext = context;
            this.mName = name;
            this.mFactory = factory;
            this.mNewVersion = version;
            this.mErrorHandler = errorHandler;
            this.mMinimumSupportedVersion = Math.max(0, minimumSupportedVersion);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Version must be >= 1, was ");
        stringBuilder.append(version);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String getDatabaseName() {
        return this.mName;
    }

    public void setWriteAheadLoggingEnabled(boolean enabled) {
        synchronized (this) {
            if (this.mEnableWriteAheadLogging != enabled) {
                if (!(this.mDatabase == null || !this.mDatabase.isOpen() || this.mDatabase.isReadOnly())) {
                    if (enabled) {
                        this.mDatabase.enableWriteAheadLogging();
                    } else {
                        this.mDatabase.disableWriteAheadLogging();
                    }
                }
                this.mEnableWriteAheadLogging = enabled;
            }
        }
    }

    public void setDatabaseOpenFlags(int openFlags) {
        if (((~-2130706432) & openFlags) == 0) {
            synchronized (this) {
                if (this.mDatabase != null) {
                    if (this.mDatabase.isOpen()) {
                        throw new IllegalStateException("Set open flags after database opened");
                    }
                }
                this.mOpenFlags |= openFlags;
            }
            return;
        }
        throw new IllegalArgumentException("Invalid open flags");
    }

    public void setDatabaseEncrypted(byte[] key) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Empty encrypt key");
        }
        synchronized (this) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    throw new IllegalStateException("Set encrypted after database opened");
                }
            }
            this.mEncryptKey = Arrays.copyOf(key, key.length);
        }
    }

    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(true);
        }
        return databaseLocked;
    }

    public SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase databaseLocked;
        synchronized (this) {
            databaseLocked = getDatabaseLocked(false);
        }
        return databaseLocked;
    }

    private SQLiteDatabase getDatabaseLocked(boolean writable) {
        if (this.mDatabase != null) {
            if (!this.mDatabase.isOpen()) {
                this.mDatabase = null;
            } else if (!(writable && this.mDatabase.isReadOnly())) {
                return this.mDatabase;
            }
        }
        if (this.mIsInitializing) {
            throw new IllegalStateException("getDatabase called recursively");
        }
        StringBuilder stringBuilder;
        SQLiteDatabase db = this.mDatabase;
        try {
            this.mIsInitializing = true;
            if (db != null) {
                if (writable && db.isReadOnly()) {
                    db.reopenReadWrite();
                }
            } else if (this.mName == null) {
                db = SQLiteDatabase.create(null);
            } else {
                String path;
                if (this.mContext != null) {
                    path = this.mContext.getDatabasePath(this.mName).getPath();
                } else {
                    path = this.mName;
                }
                int flags = this.mOpenFlags | 268435456;
                if (this.mEnableWriteAheadLogging) {
                    flags |= 536870912;
                }
                if (this.mEncryptKey != null) {
                    flags |= SQLiteDatabase.ENABLE_DATABASE_ENCRYPTION;
                }
                db = SQLiteDatabase.openDatabase(path, this.mFactory, flags, this.mErrorHandler, this.mEncryptKey);
            }
        } catch (SQLiteException ex) {
            if (writable) {
                throw ex;
            } else {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Couldn't open ");
                stringBuilder2.append(this.mName);
                stringBuilder2.append(" for writing (will try read-only):");
                Log.e(str, stringBuilder2.toString(), ex);
                if (this.mContext != null) {
                    str = this.mContext.getDatabasePath(this.mName).getPath();
                } else {
                    str = this.mName;
                }
                int flags2 = 1 | this.mOpenFlags;
                if (this.mEncryptKey != null) {
                    flags2 |= SQLiteDatabase.ENABLE_DATABASE_ENCRYPTION;
                }
                db = SQLiteDatabase.openDatabase(str, this.mFactory, flags2, this.mErrorHandler, this.mEncryptKey);
            }
        } catch (Throwable th) {
            this.mIsInitializing = false;
            if (!(db == null || db == this.mDatabase)) {
                db.close();
            }
        }
        onConfigure(db);
        int version = db.getVersion();
        if (version != this.mNewVersion) {
            if (db.isReadOnly()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Can't upgrade read-only database from version ");
                stringBuilder.append(db.getVersion());
                stringBuilder.append(" to ");
                stringBuilder.append(this.mNewVersion);
                stringBuilder.append(": ");
                stringBuilder.append(this.mName);
                throw new SQLiteException(stringBuilder.toString());
            } else if (version <= 0 || version >= this.mMinimumSupportedVersion) {
                db.beginTransaction();
                if (version == 0) {
                    onCreate(db);
                } else if (version > this.mNewVersion) {
                    onDowngrade(db, version, this.mNewVersion);
                } else {
                    onUpgrade(db, version, this.mNewVersion);
                }
                db.setVersion(this.mNewVersion);
                db.setTransactionSuccessful();
                db.endTransaction();
            } else {
                File databaseFile = new File(db.getPath());
                onBeforeDelete(db);
                db.close();
                if (SQLiteDatabase.deleteDatabase(databaseFile)) {
                    this.mIsInitializing = false;
                    SQLiteDatabase databaseLocked = getDatabaseLocked(writable);
                    this.mIsInitializing = false;
                    if (!(db == null || db == this.mDatabase)) {
                        db.close();
                    }
                    return databaseLocked;
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to delete obsolete database ");
                stringBuilder3.append(this.mName);
                stringBuilder3.append(" with version ");
                stringBuilder3.append(version);
                throw new IllegalStateException(stringBuilder3.toString());
            }
        }
        onOpen(db);
        if (db.isReadOnly()) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Opened ");
            stringBuilder.append(this.mName);
            stringBuilder.append(" in read-only mode");
            Log.w(str2, stringBuilder.toString());
        }
        this.mDatabase = db;
        this.mIsInitializing = false;
        if (!(db == null || db == this.mDatabase)) {
            db.close();
        }
        return db;
    }

    public synchronized void close() {
        if (this.mIsInitializing) {
            throw new IllegalStateException("Closed during initialization");
        } else if (this.mDatabase != null && this.mDatabase.isOpen()) {
            this.mDatabase.close();
            this.mDatabase = null;
        }
    }

    public void onConfigure(SQLiteDatabase db) {
    }

    public void onBeforeDelete(SQLiteDatabase db) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't downgrade database from version ");
        stringBuilder.append(oldVersion);
        stringBuilder.append(" to ");
        stringBuilder.append(newVersion);
        throw new SQLiteException(stringBuilder.toString());
    }

    public void onOpen(SQLiteDatabase db) {
    }
}

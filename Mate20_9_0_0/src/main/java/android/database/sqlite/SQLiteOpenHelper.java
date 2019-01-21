package android.database.sqlite;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.CursorResourceWrapper;
import android.database.CursorWindow;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase.OpenParams;
import android.database.sqlite.SQLiteDatabase.OpenParams.Builder;
import android.database.sqlite.SQLiteDatabaseEx.DatabaseConnectionExclusiveHandler;
import android.os.FileUtils;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.io.File;

public abstract class SQLiteOpenHelper {
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();
    private DatabaseConnectionExclusiveHandler mConnectionExclusiveHandler;
    private final Context mContext;
    private SQLiteDatabase mDatabase;
    private boolean mEnableExclusiveConnection;
    private boolean mIsInitializing;
    private final int mMinimumSupportedVersion;
    private final String mName;
    private final int mNewVersion;
    private Builder mOpenParamsBuilder;

    public abstract void onCreate(SQLiteDatabase sQLiteDatabase);

    public abstract void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2);

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version) {
        this(context, name, factory, version, null);
    }

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        this(context, name, factory, version, 0, errorHandler);
    }

    public SQLiteOpenHelper(Context context, String name, int version, OpenParams openParams) {
        this(context, name, version, 0, openParams.toBuilder());
    }

    public SQLiteOpenHelper(Context context, String name, CursorFactory factory, int version, int minimumSupportedVersion, DatabaseErrorHandler errorHandler) {
        this(context, name, version, minimumSupportedVersion, new Builder());
        this.mOpenParamsBuilder.setCursorFactory(factory);
        this.mOpenParamsBuilder.setErrorHandler(errorHandler);
    }

    private SQLiteOpenHelper(Context context, String name, int version, int minimumSupportedVersion, Builder openParamsBuilder) {
        Preconditions.checkNotNull(openParamsBuilder);
        if (version >= 1) {
            this.mContext = context;
            this.mName = name;
            this.mNewVersion = version;
            this.mMinimumSupportedVersion = Math.max(0, minimumSupportedVersion);
            this.mOpenParamsBuilder = openParamsBuilder;
            this.mOpenParamsBuilder.addOpenFlags(268435456);
            if (CursorResourceWrapper.isNeedResProtect(this.mContext)) {
                CursorWindow.setCursorResource(new CursorResourceWrapper(context));
            }
            this.mOpenParamsBuilder = openParamsBuilder;
            this.mOpenParamsBuilder.addOpenFlags(268435456);
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
            if (this.mOpenParamsBuilder.isWriteAheadLoggingEnabled() != enabled) {
                if (!(this.mDatabase == null || !this.mDatabase.isOpen() || this.mDatabase.isReadOnly())) {
                    if (enabled) {
                        this.mDatabase.enableWriteAheadLogging();
                    } else {
                        this.mDatabase.disableWriteAheadLogging();
                    }
                }
                this.mOpenParamsBuilder.setWriteAheadLoggingEnabled(enabled);
            }
            this.mOpenParamsBuilder.addOpenFlags(1073741824);
        }
    }

    public void setLookasideConfig(int slotSize, int slotCount) {
        synchronized (this) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    throw new IllegalStateException("Lookaside memory config cannot be changed after opening the database");
                }
            }
            this.mOpenParamsBuilder.setLookasideConfig(slotSize, slotCount);
        }
    }

    public void setOpenParams(OpenParams openParams) {
        Preconditions.checkNotNull(openParams);
        synchronized (this) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    throw new IllegalStateException("OpenParams cannot be set after opening the database");
                }
            }
            setOpenParamsBuilder(new Builder(openParams));
        }
    }

    private void setOpenParamsBuilder(Builder openParamsBuilder) {
        this.mOpenParamsBuilder = openParamsBuilder;
        this.mOpenParamsBuilder.addOpenFlags(268435456);
    }

    public void setIdleConnectionTimeout(long idleConnectionTimeoutMs) {
        synchronized (this) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    throw new IllegalStateException("Connection timeout setting cannot be changed after opening the database");
                }
            }
            this.mOpenParamsBuilder.setIdleConnectionTimeout(idleConnectionTimeoutMs);
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
        File filePath;
        StringBuilder stringBuilder;
        SQLiteDatabase db = this.mDatabase;
        OpenParams params;
        try {
            this.mIsInitializing = true;
            if (db != null) {
                if (writable && db.isReadOnly()) {
                    db.reopenReadWrite();
                }
            } else if (this.mName == null) {
                db = SQLiteDatabase.createInMemory(this.mOpenParamsBuilder.build());
            } else {
                filePath = this.mContext.getDatabasePath(this.mName);
                params = this.mOpenParamsBuilder.build();
                db = SQLiteDatabase.openDatabase(filePath, params);
                setFilePermissionsForDb(filePath.getPath());
                db.enableExclusiveConnection(this.mEnableExclusiveConnection, this.mConnectionExclusiveHandler);
            }
        } catch (SQLException ex) {
            if (writable) {
                throw ex;
            } else {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Couldn't open ");
                stringBuilder2.append(this.mName);
                stringBuilder2.append(" for writing (will try read-only):");
                Log.e(str, stringBuilder2.toString(), ex);
                db = SQLiteDatabase.openDatabase(filePath, params.toBuilder().addOpenFlags(1).build());
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
                filePath = new File(db.getPath());
                onBeforeDelete(db);
                db.close();
                if (SQLiteDatabase.deleteDatabase(filePath)) {
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
            filePath = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Opened ");
            stringBuilder.append(this.mName);
            stringBuilder.append(" in read-only mode");
            Log.w(filePath, stringBuilder.toString());
        }
        this.mDatabase = db;
        this.mIsInitializing = false;
        if (!(db == null || db == this.mDatabase)) {
            db.close();
        }
        return db;
    }

    private static void setFilePermissionsForDb(String dbPath) {
        FileUtils.setPermissions(dbPath, DevicePolicyManager.PROFILE_KEYGUARD_FEATURES_AFFECT_OWNER, -1, -1);
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

    public void setExclusiveConnectionEnabled(boolean enabled, DatabaseConnectionExclusiveHandler connectionExclusiveHandler) {
        synchronized (this) {
            this.mEnableExclusiveConnection = enabled;
            this.mConnectionExclusiveHandler = connectionExclusiveHandler;
        }
    }
}

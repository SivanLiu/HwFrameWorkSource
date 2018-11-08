package com.huawei.hwsqlite;

import android.util.Log;
import android.util.Pair;
import java.io.File;

public final class SQLiteDefaultErrorHandler implements SQLiteErrorHandler {
    private static final String TAG = "SQLiteDefErrHandler";

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onCorruption(SQLiteDatabase dbObj) {
        Log.e(TAG, "Corruption reported by sqlite on database: " + dbObj.getPath());
        if (!dbObj.isOpen()) {
            if (dbObj.reopenOnOpenCorruption()) {
                deleteDatabaseFile(dbObj.getPath());
            }
        } else if (dbObj.reopenOnOpenCorruption()) {
            Iterable iterable = null;
            try {
                iterable = dbObj.getAttachedDbs();
            } catch (SQLiteException e) {
            } catch (Throwable th) {
                Throwable th2 = th;
                if (r0 == null) {
                    deleteDatabaseFile(dbObj.getPath());
                } else {
                    for (Pair<String, String> p : r0) {
                        deleteDatabaseFile((String) p.second);
                    }
                }
            }
            dbObj.close();
            if (r0 != null) {
                for (Pair<String, String> p2 : r0) {
                    deleteDatabaseFile((String) p2.second);
                }
            } else {
                deleteDatabaseFile(dbObj.getPath());
            }
        } else {
            dbObj.close();
        }
    }

    private void deleteDatabaseFile(String fileName) {
        if (!fileName.equalsIgnoreCase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH) && fileName.trim().length() != 0) {
            Log.e(TAG, "deleting the database file: " + fileName);
            try {
                SQLiteDatabase.deleteDatabase(new File(fileName));
            } catch (Exception e) {
                Log.w(TAG, "delete failed: " + e.getMessage());
            }
        }
    }
}

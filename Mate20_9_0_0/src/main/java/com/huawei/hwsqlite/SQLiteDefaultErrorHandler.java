package com.huawei.hwsqlite;

import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.util.List;

public final class SQLiteDefaultErrorHandler implements SQLiteErrorHandler {
    private static final String TAG = "SQLiteDefErrHandler";

    /* JADX WARNING: Removed duplicated region for block: B:14:0x003f A:{PHI: r0 , ExcHandler: all (th java.lang.Throwable), Splitter:B:11:0x0039} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:18:0x0046, code skipped:
            if (r0 != null) goto L_0x0048;
     */
    /* JADX WARNING: Missing block: B:19:0x0048, code skipped:
            r2 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:21:0x0050, code skipped:
            if (r2.hasNext() != false) goto L_0x0052;
     */
    /* JADX WARNING: Missing block: B:22:0x0052, code skipped:
            deleteDatabaseFile((java.lang.String) ((android.util.Pair) r2.next()).second);
     */
    /* JADX WARNING: Missing block: B:23:0x0060, code skipped:
            deleteDatabaseFile(r6.getPath());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onCorruption(SQLiteDatabase dbObj) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Corruption reported by sqlite on database: ");
        stringBuilder.append(dbObj.getPath());
        Log.e(str, stringBuilder.toString());
        if (!dbObj.isOpen()) {
            if (dbObj.reopenOnOpenCorruption()) {
                deleteDatabaseFile(dbObj.getPath());
            }
        } else if (dbObj.reopenOnOpenCorruption()) {
            List<Pair<String, String>> attachedDbs = null;
            try {
                attachedDbs = dbObj.getAttachedDbs();
                dbObj.close();
            } catch (SQLiteException e) {
            } catch (Throwable th) {
            }
            if (attachedDbs != null) {
                for (Pair<String, String> p : attachedDbs) {
                    deleteDatabaseFile((String) p.second);
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deleting the database file: ");
            stringBuilder.append(fileName);
            Log.e(str, stringBuilder.toString());
            try {
                SQLiteDatabase.deleteDatabase(new File(fileName));
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("delete failed: ");
                stringBuilder2.append(e.getMessage());
                Log.w(str2, stringBuilder2.toString());
            }
        }
    }
}

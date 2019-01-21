package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseConfiguration;
import android.database.sqlite.SQLiteException;
import android.os.FileUtils;
import android.util.Log;
import android.util.Pair;
import java.io.File;
import java.util.List;

public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {
    private static final String TAG = "DefaultDatabaseErrorHandler";

    /* JADX WARNING: Removed duplicated region for block: B:8:0x002f A:{PHI: r0 , ExcHandler: all (th java.lang.Throwable), Splitter:B:5:0x0029} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:12:0x0039, code skipped:
            if (r0 != null) goto L_0x003b;
     */
    /* JADX WARNING: Missing block: B:13:0x003b, code skipped:
            r2 = r0.iterator();
     */
    /* JADX WARNING: Missing block: B:15:0x0043, code skipped:
            if (r2.hasNext() != false) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:16:0x0045, code skipped:
            deleteDatabaseFile((java.lang.String) ((android.util.Pair) r2.next()).second);
     */
    /* JADX WARNING: Missing block: B:17:0x0053, code skipped:
            deleteDatabaseFile(r6.getPath());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onCorruption(SQLiteDatabase dbObj) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Corruption reported by sqlite on database: ");
        stringBuilder.append(dbObj.getPath());
        Log.e(str, stringBuilder.toString());
        if (dbObj.isOpen()) {
            List<Pair<String, String>> attachedDbs = null;
            try {
                attachedDbs = dbObj.getAttachedDbs();
                addLogListenter(attachedDbs, dbObj);
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
            return;
        }
        deleteDatabaseFile(dbObj.getPath());
    }

    private void addLogListenter(List<Pair<String, String>> attachedDbs, SQLiteDatabase dbObj) {
        if (attachedDbs != null) {
            for (Pair<String, String> p : attachedDbs) {
                logPrint((String) p.second);
            }
            return;
        }
        logPrint(dbObj.getPath());
    }

    private void logPrint(String fileName) {
        File file = new File(fileName);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(fileName);
        stringBuilder.append("-corrupted");
        FileUtils.copyFile(file, new File(stringBuilder.toString()));
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(fileName);
        stringBuilder2.append("-journal");
        file = new File(stringBuilder2.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(fileName);
        stringBuilder.append("-journalcorrupted");
        FileUtils.copyFile(file, new File(stringBuilder.toString()));
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(fileName);
        stringBuilder2.append("-wal");
        file = new File(stringBuilder2.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(fileName);
        stringBuilder.append("-walcorrupted");
        FileUtils.copyFile(file, new File(stringBuilder.toString()));
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

package com.android.server.security.securityprofile;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

class B200DatabaseImporter {
    public static final String CATEGORY_TABLE = "category";
    private static final String[] COLUMNS_FOR_QUERY = new String[]{"package", "category"};
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_PACKAGE = "package";
    private static final String DATABASE_NAME = "securityprofile.db";
    private static final String TAG = "SecurityProfileService";

    B200DatabaseImporter() {
    }

    /* JADX WARNING: Missing block: B:16:0x0058, code skipped:
            if (r1 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:17:0x005a, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:22:0x0068, code skipped:
            if (r1 == null) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:23:0x006b, code skipped:
            r2.close();
            r13.deleteDatabase(DATABASE_NAME);
     */
    /* JADX WARNING: Missing block: B:24:0x0074, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static List<String> getBlackListAndDeleteDatabase(Context context) {
        List<String> blackList = new ArrayList();
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase("/data/system/securityprofile.db", null, 1);
            try {
                String[] strArr = COLUMNS_FOR_QUERY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("category=");
                stringBuilder.append(String.valueOf(1));
                cursor = db.query("category", strArr, stringBuilder.toString(), null, null, null, null);
                if (cursor == null) {
                    Slog.e(TAG, "can not read database.");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return blackList;
                }
                while (cursor.moveToNext()) {
                    blackList.add(cursor.getString(cursor.getColumnIndexOrThrow("package")));
                }
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "find format errors in database.");
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (SQLiteException e2) {
            Slog.e(TAG, "can not open readable database.");
            return blackList;
        }
    }
}

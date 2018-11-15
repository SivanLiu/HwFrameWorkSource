package com.android.server.hidata.wavemapping.dao;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.ArrayList;
import java.util.List;

public class DatabaseDBDao {
    private static final String TAG;
    private String CREATE_TEMP_STD_DATA_TABLE_NAME = "CREATE TABLE IF NOT EXISTS _TEMP_STD (LOCATION VARCHAR(200), BATCH VARCHAR(20),DATAS TEXT)";
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(DatabaseDBDao.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:7:0x004d, code:
            if (r1 != null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:8:0x004f, code:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:13:0x006a, code:
            if (r1 == null) goto L_0x0085;
     */
    /* JADX WARNING: Missing block: B:16:0x0082, code:
            if (r1 == null) goto L_0x0085;
     */
    /* JADX WARNING: Missing block: B:17:0x0085, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> findTableName(String key) {
        StringBuilder stringBuilder;
        String sql = new StringBuilder();
        sql.append("select name from sqlite_master where type='table' and name like '%");
        sql.append(key);
        sql.append("%';");
        sql = sql.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findTableName :sql=");
        stringBuilder2.append(sql);
        LogUtil.i(stringBuilder2.toString());
        Cursor cursor = null;
        List<String> tables = new ArrayList();
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findTableName IllegalArgumentException: ");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findTableName Exception: ");
            stringBuilder.append(e2);
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean createTempStdDataTable(String location) {
        StringBuilder stringBuilder;
        try {
            String sql = this.CREATE_TEMP_STD_DATA_TABLE_NAME;
            CharSequence charSequence = Constant.TEMP_STD_DATA_TABLE_NAME;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(location);
            stringBuilder2.append(Constant.TEMP_STD_DATA_TABLE_NAME);
            sql = sql.replace(charSequence, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("createTempStdDataTable:");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            this.db.execSQL(sql);
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("createTempStdDataTable,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return true;
    }

    public boolean dropTempStdDataTable(String location) {
        StringBuilder stringBuilder;
        try {
            SQLiteDatabase sQLiteDatabase = this.db;
            stringBuilder = new StringBuilder();
            stringBuilder.append("DROP TABLE IF EXISTS ");
            stringBuilder.append(location);
            stringBuilder.append(Constant.TEMP_STD_DATA_TABLE_NAME);
            sQLiteDatabase.execSQL(stringBuilder.toString());
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("dropTempStdDataTable:");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
        }
        return true;
    }
}

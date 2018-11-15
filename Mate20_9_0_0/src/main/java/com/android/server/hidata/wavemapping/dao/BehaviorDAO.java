package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BehaviorDAO {
    private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(BehaviorDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public boolean insert(int batch) {
        if (getBatch() > 0) {
            update(batch);
            return true;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("UPDATETIME", new SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(new Date(System.currentTimeMillis())));
        cValue.put("BATCH", Integer.valueOf(batch));
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.BEHAVIOR_TABLE_NAME, null, cValue);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            this.db.endTransaction();
            return false;
        } catch (Throwable th) {
            this.db.endTransaction();
            throw th;
        }
    }

    public boolean update(int batch) {
        if (batch == 0) {
            LogUtil.d("update failure,batch == 0");
            return false;
        }
        String sql = "UPDATE BEHAVIOR_MAINTAIN SET UPDATETIME = ?,BATCH = ?";
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        Object[] args = new Object[]{dateFormat.format(new Date(System.currentTimeMillis())), Integer.valueOf(batch)};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin:");
        stringBuilder.append(sql);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("UPDATE exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return false;
        } finally {
            this.db.endTransaction();
            return false;
        }
        return true;
    }

    public boolean remove() {
        try {
            this.db.execSQL("DELETE FROM BEHAVIOR_MAINTAIN", null);
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0022, code:
            if (r1 != null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:9:0x0024, code:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:14:0x0043, code:
            if (r1 == null) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:17:0x005f, code:
            if (r1 == null) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:18:0x0062, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getBatch() {
        StringBuilder stringBuilder;
        int batch = 0;
        Cursor cursor = null;
        if (this.db == null) {
            return 0;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM BEHAVIOR_MAINTAIN", null);
            if (cursor.moveToNext()) {
                batch = cursor.getInt(cursor.getColumnIndex("BATCH"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getBatch IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getBatch Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

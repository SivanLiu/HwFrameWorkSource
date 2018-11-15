package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class HisQoEChrDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();
    private String freqLocation = "UNKNOWN";
    private int hQoeGoodCnt = 0;
    private int hQoePoorCnt = 0;
    private int hQoeQueryCnt = 0;
    private int hQoeUnknownDB = 0;
    private int hQoeUnknownSpace = 0;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(HisQoEChrDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:7:0x005b, code:
            if (r4 != null) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:8:0x005d, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:13:0x007d, code:
            if (r4 == null) goto L_0x009c;
     */
    /* JADX WARNING: Missing block: B:16:0x0099, code:
            if (r4 == null) goto L_0x009c;
     */
    /* JADX WARNING: Missing block: B:17:0x009c, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getCntNumByLoc in HistQoeChrTable found:");
            r3.append(r2);
            r3.append(" location:");
            r3.append(r7.freqLocation);
            r3.append(":");
            r3.append(r7.hQoeQueryCnt);
            r3.append(":");
            r3.append(r7.hQoeGoodCnt);
            r3.append(":");
            r3.append(r7.hQoePoorCnt);
            r3.append(":");
            r3.append(r7.hQoeUnknownDB);
            r3.append(":");
            r3.append(r7.hQoeUnknownSpace);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x00ec, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getCountersByLocation() {
        StringBuilder stringBuilder;
        String sql = "SELECT QUERYCNT, GOODCNT, POORCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_HISTQOERPT WHERE FREQLOCNAME = ?";
        String[] args = new String[]{this.freqLocation};
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null) {
            return false;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                found = true;
                this.hQoeQueryCnt = cursor.getInt(cursor.getColumnIndexOrThrow("QUERYCNT"));
                this.hQoeGoodCnt = cursor.getInt(cursor.getColumnIndexOrThrow("GOODCNT"));
                this.hQoePoorCnt = cursor.getInt(cursor.getColumnIndexOrThrow("POORCNT"));
                this.hQoeUnknownDB = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNDB"));
                this.hQoeUnknownSpace = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNSPACE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Argument getCntNumByLoc in HistQoeChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getCntNumByLoc in HistQoeChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x005c, code:
            if (r4 != null) goto L_0x005e;
     */
    /* JADX WARNING: Missing block: B:8:0x005e, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:13:0x007e, code:
            if (r4 == null) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:16:0x009a, code:
            if (r4 == null) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:17:0x009d, code:
            r2 = new java.lang.StringBuilder();
            r2.append("getCountersByLocation in HistQoeChrTable found:");
            r2.append(r3);
            r2.append(" location:");
            r2.append(r8);
            r2.append(":");
            r2.append(r7.hQoeQueryCnt);
            r2.append(":");
            r2.append(r7.hQoeGoodCnt);
            r2.append(":");
            r2.append(r7.hQoePoorCnt);
            r2.append(":");
            r2.append(r7.hQoeUnknownDB);
            r2.append(":");
            r2.append(r7.hQoeUnknownSpace);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r2.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x00eb, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getCountersByLocation(String location) {
        StringBuilder stringBuilder;
        String sql = "SELECT QUERYCNT, GOODCNT, POORCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_HISTQOERPT WHERE FREQLOCNAME = ?";
        String[] args = new String[]{location};
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null || location == null) {
            return false;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                found = true;
                this.hQoeQueryCnt = cursor.getInt(cursor.getColumnIndexOrThrow("QUERYCNT"));
                this.hQoeGoodCnt = cursor.getInt(cursor.getColumnIndexOrThrow("GOODCNT"));
                this.hQoePoorCnt = cursor.getInt(cursor.getColumnIndexOrThrow("POORCNT"));
                this.hQoeUnknownDB = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNDB"));
                this.hQoeUnknownSpace = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNSPACE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getCountersByLocation IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getCountersByLocation Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x001f, code:
            if (r4 != null) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:8:0x0021, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:13:0x0040, code:
            if (r4 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:16:0x005c, code:
            if (r4 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:17:0x005f, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getRecordByLoc in HistQoeChrTable found:");
            r3.append(r2);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x0073, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getRecordByLoc() {
        StringBuilder stringBuilder;
        String sql = "SELECT * FROM CHR_HISTQOERPT WHERE FREQLOCNAME = ?";
        String[] args = new String[]{this.freqLocation};
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null) {
            return false;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                found = true;
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Argument getRecordByLoc in HistQoeChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getRecordByLoc in HistQoeChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean updateRecordByLoc() {
        String sql = "UPDATE CHR_HISTQOERPT SET QUERYCNT = ?, GOODCNT = ?, POORCNT = ?, UNKNOWNDB = ?, UNKNOWNSPACE  = ? WHERE FREQLOCNAME = ?";
        args = new Object[6];
        boolean z = false;
        args[0] = Integer.valueOf(this.hQoeQueryCnt);
        args[1] = Integer.valueOf(this.hQoeGoodCnt);
        args[2] = Integer.valueOf(this.hQoePoorCnt);
        args[3] = Integer.valueOf(this.hQoeUnknownDB);
        args[4] = Integer.valueOf(this.hQoeUnknownSpace);
        args[5] = this.freqLocation;
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRecordByLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            return z;
        }
        return true;
    }

    public boolean insertRecordByLoc() {
        if (getRecordByLoc()) {
            return updateRecordByLoc();
        }
        ContentValues cValueBase = new ContentValues();
        cValueBase.put("FREQLOCNAME", this.freqLocation);
        cValueBase.put("QUERYCNT", Integer.valueOf(this.hQoeQueryCnt));
        cValueBase.put("GOODCNT", Integer.valueOf(this.hQoeGoodCnt));
        cValueBase.put("POORCNT", Integer.valueOf(this.hQoePoorCnt));
        cValueBase.put("UNKNOWNDB", Integer.valueOf(this.hQoeUnknownDB));
        cValueBase.put("UNKNOWNSPACE", Integer.valueOf(this.hQoeUnknownSpace));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertRecordByLoc in HistQoeChrTable  location:");
        stringBuilder.append(this.freqLocation);
        stringBuilder.append(":");
        stringBuilder.append(this.hQoeQueryCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.hQoeGoodCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.hQoePoorCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.hQoeUnknownDB);
        stringBuilder.append(":");
        stringBuilder.append(this.hQoeUnknownSpace);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.CHR_HISTQOERPT, null, cValueBase);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("insertRecordByLoc exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            this.db.endTransaction();
            return false;
        } catch (Throwable th) {
            this.db.endTransaction();
            throw th;
        }
    }

    public boolean resetRecord(String loc) {
        String sql = "UPDATE CHR_HISTQOERPT SET QUERYCNT = 0, GOODCNT = 0, POORCNT = 0, DATARX = 0, DATATX = 0, UNKNOWNDB = 0, UNKNOWNSPACE = 0 WHERE FREQLOCNAME = ? ";
        boolean e = true;
        String[] args = new String[1];
        boolean z = false;
        args[0] = loc;
        if (!getCountersByLocation(loc)) {
            return false;
        }
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
            return e;
        } catch (SQLException e2) {
            e = e2;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resetRecord by loc of CHR_HISTQOERPT exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            resetChrCnt();
        }
    }

    public boolean delRecord() {
        String sql = "DELETE FROM CHR_HISTQOERPT";
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, null);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delRecord of delRecord exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            this.db.endTransaction();
            return false;
        } catch (Throwable th) {
            this.db.endTransaction();
            throw th;
        }
    }

    public void resetChrCnt() {
        this.hQoeQueryCnt = 0;
        this.hQoeGoodCnt = 0;
        this.hQoePoorCnt = 0;
        this.hQoeUnknownDB = 0;
        this.hQoeUnknownSpace = 0;
    }

    public void accQueryCnt() {
        this.hQoeQueryCnt++;
    }

    public int getQueryCnt() {
        return this.hQoeQueryCnt;
    }

    public void accGoodCnt() {
        this.hQoeGoodCnt++;
    }

    public int getGoodCnt() {
        return this.hQoeGoodCnt;
    }

    public void accPoorCnt() {
        this.hQoePoorCnt++;
    }

    public int getPoorCnt() {
        return this.hQoePoorCnt;
    }

    public void accUnknownDB() {
        this.hQoeUnknownDB++;
    }

    public int getUnknownDB() {
        return this.hQoeUnknownDB;
    }

    public void accUnknownSpace() {
        this.hQoeUnknownSpace++;
    }

    public int getUnknownSpace() {
        return this.hQoeUnknownSpace;
    }

    public void setLocation(String location) {
        if (location != null) {
            this.freqLocation = location;
            resetChrCnt();
        }
    }

    public String getLocation() {
        return this.freqLocation;
    }
}

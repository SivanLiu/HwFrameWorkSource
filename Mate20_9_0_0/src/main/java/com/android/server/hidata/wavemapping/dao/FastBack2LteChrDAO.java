package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class FastBack2LteChrDAO {
    private static final String TAG;
    private int cells4G = 0;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();
    private int fastBack = 0;
    private int inLteCnt = 0;
    private String location = "UNKNOWN";
    private int lowRatCnt = 0;
    private int outLteCnt = 0;
    private int refCnt = 0;
    private int successBack = 0;
    private int sumcells4G = 0;
    private int sumfastBack = 0;
    private int suminLteCnt = 0;
    private int sumlowRatCnt = 0;
    private int sumoutLteCnt = 0;
    private int sumrefCnt = 0;
    private int sumsuccessBack = 0;
    private int sumunknownDB = 0;
    private int sumunknownSpace = 0;
    private int unknownDB = 0;
    private int unknownSpace = 0;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(FastBack2LteChrDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public void resetFastBack2LteSumCount() {
        this.sumlowRatCnt = 0;
        this.suminLteCnt = 0;
        this.sumoutLteCnt = 0;
        this.sumfastBack = 0;
        this.sumsuccessBack = 0;
        this.sumcells4G = 0;
        this.sumrefCnt = 0;
        this.sumunknownDB = 0;
        this.sumunknownSpace = 0;
    }

    public void resetFastBack2LteCount() {
        this.lowRatCnt = 0;
        this.inLteCnt = 0;
        this.outLteCnt = 0;
        this.fastBack = 0;
        this.successBack = 0;
        this.cells4G = 0;
        this.refCnt = 0;
        this.unknownDB = 0;
        this.unknownSpace = 0;
    }

    /* JADX WARNING: Missing block: B:10:0x00a5, code skipped:
            if (r2 != null) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:20:0x00e3, code skipped:
            if (r2 == null) goto L_0x00e6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getTotalCounters() {
        StringBuilder stringBuilder;
        String sql = "SELECT LOWRATCNT, INLTECNT, OUTLTECNT, FASTBACK, SUCCESSBACK, CELLS4G, REFCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_FASTBACK2LTE";
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null) {
            return false;
        }
        try {
            resetFastBack2LteSumCount();
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                found = true;
                this.sumlowRatCnt += cursor.getInt(cursor.getColumnIndexOrThrow("LOWRATCNT"));
                this.suminLteCnt += cursor.getInt(cursor.getColumnIndexOrThrow("INLTECNT"));
                this.sumoutLteCnt += cursor.getInt(cursor.getColumnIndexOrThrow("OUTLTECNT"));
                this.sumfastBack += cursor.getInt(cursor.getColumnIndexOrThrow("FASTBACK"));
                this.sumsuccessBack += cursor.getInt(cursor.getColumnIndexOrThrow("SUCCESSBACK"));
                this.sumcells4G += cursor.getInt(cursor.getColumnIndexOrThrow("CELLS4G"));
                this.sumrefCnt += cursor.getInt(cursor.getColumnIndexOrThrow("REFCNT"));
                this.sumunknownDB += cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNDB"));
                this.sumunknownSpace += cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNSPACE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Argument getTotalCounters in Back2LteChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getTotalCounters in Back2LteChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getTotalCounters in Back2LteChrTable found:");
            stringBuilder2.append(found);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumlowRatCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.suminLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumoutLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumfastBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumsuccessBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumcells4G);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumrefCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumunknownDB);
            stringBuilder2.append(":");
            stringBuilder2.append(this.sumunknownSpace);
            LogUtil.d(stringBuilder2.toString());
            return found;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x008c, code skipped:
            if (r4 != null) goto L_0x008e;
     */
    /* JADX WARNING: Missing block: B:17:0x00ca, code skipped:
            if (r4 == null) goto L_0x00cd;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getCountersByLocation(String loc) {
        StringBuilder stringBuilder;
        String sql = "SELECT LOWRATCNT, INLTECNT, OUTLTECNT, FASTBACK, SUCCESSBACK, CELLS4G, REFCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_FASTBACK2LTE WHERE FREQLOCNAME = ?";
        String[] args = new String[]{loc};
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null) {
            return false;
        }
        try {
            resetFastBack2LteCount();
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                found = true;
                this.lowRatCnt = cursor.getInt(cursor.getColumnIndexOrThrow("LOWRATCNT"));
                this.inLteCnt = cursor.getInt(cursor.getColumnIndexOrThrow("INLTECNT"));
                this.outLteCnt = cursor.getInt(cursor.getColumnIndexOrThrow("OUTLTECNT"));
                this.fastBack = cursor.getInt(cursor.getColumnIndexOrThrow("FASTBACK"));
                this.successBack = cursor.getInt(cursor.getColumnIndexOrThrow("SUCCESSBACK"));
                this.cells4G = cursor.getInt(cursor.getColumnIndexOrThrow("CELLS4G"));
                this.refCnt = cursor.getInt(cursor.getColumnIndexOrThrow("REFCNT"));
                this.unknownDB = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNDB"));
                this.unknownSpace = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNSPACE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Argument getCountersByLocation in Back2LteChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getCountersByLocation in Back2LteChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCountersByLocation in Back2LteChrTable found:");
            stringBuilder2.append(found);
            stringBuilder2.append(" location:");
            stringBuilder2.append(this.location);
            stringBuilder2.append(":");
            stringBuilder2.append(this.lowRatCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.inLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.outLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.fastBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.successBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.cells4G);
            stringBuilder2.append(":");
            stringBuilder2.append(this.refCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.unknownDB);
            stringBuilder2.append(":");
            stringBuilder2.append(this.unknownSpace);
            LogUtil.d(stringBuilder2.toString());
            return found;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x008b, code skipped:
            if (r4 != null) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:17:0x00c9, code skipped:
            if (r4 == null) goto L_0x00cc;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getCountersByLocation() {
        StringBuilder stringBuilder;
        String sql = "SELECT LOWRATCNT, INLTECNT, OUTLTECNT, FASTBACK, SUCCESSBACK, CELLS4G, REFCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_FASTBACK2LTE WHERE FREQLOCNAME = ?";
        String[] args = new String[]{this.location};
        boolean found = false;
        Cursor cursor = null;
        if (this.db == null) {
            return false;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                found = true;
                this.lowRatCnt = cursor.getInt(cursor.getColumnIndexOrThrow("LOWRATCNT"));
                this.inLteCnt = cursor.getInt(cursor.getColumnIndexOrThrow("INLTECNT"));
                this.outLteCnt = cursor.getInt(cursor.getColumnIndexOrThrow("OUTLTECNT"));
                this.fastBack = cursor.getInt(cursor.getColumnIndexOrThrow("FASTBACK"));
                this.successBack = cursor.getInt(cursor.getColumnIndexOrThrow("SUCCESSBACK"));
                this.cells4G = cursor.getInt(cursor.getColumnIndexOrThrow("CELLS4G"));
                this.refCnt = cursor.getInt(cursor.getColumnIndexOrThrow("REFCNT"));
                this.unknownDB = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNDB"));
                this.unknownSpace = cursor.getInt(cursor.getColumnIndexOrThrow("UNKNOWNSPACE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Argument getCountersByLocation in Back2LteChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getCountersByLocation in Back2LteChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCountersByLocation in Back2LteChrTable found:");
            stringBuilder2.append(found);
            stringBuilder2.append(" location:");
            stringBuilder2.append(this.location);
            stringBuilder2.append(":");
            stringBuilder2.append(this.lowRatCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.inLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.outLteCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.fastBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.successBack);
            stringBuilder2.append(":");
            stringBuilder2.append(this.cells4G);
            stringBuilder2.append(":");
            stringBuilder2.append(this.refCnt);
            stringBuilder2.append(":");
            stringBuilder2.append(this.unknownDB);
            stringBuilder2.append(":");
            stringBuilder2.append(this.unknownSpace);
            LogUtil.d(stringBuilder2.toString());
            return found;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x001f, code skipped:
            if (r4 != null) goto L_0x0021;
     */
    /* JADX WARNING: Missing block: B:17:0x005c, code skipped:
            if (r4 == null) goto L_0x005f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getRecordByLoc() {
        StringBuilder stringBuilder;
        String sql = "SELECT LOWRATCNT, INLTECNT, OUTLTECNT, FASTBACK, SUCCESSBACK, CELLS4G, REFCNT, UNKNOWNDB, UNKNOWNSPACE FROM CHR_FASTBACK2LTE WHERE FREQLOCNAME = ?";
        String[] args = new String[]{this.location};
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
            stringBuilder.append("Argument getRecordByLoc in Back2LteChrTable IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception getRecordByLoc in Back2LteChrTable Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getRecordByLoc in Back2LteChrTable found:");
            stringBuilder2.append(found);
            LogUtil.i(stringBuilder2.toString());
            return found;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean updateRecordByLoc() {
        String sql = "UPDATE CHR_FASTBACK2LTE SET LOWRATCNT = ?, INLTECNT = ?, OUTLTECNT = ?, FASTBACK = ?, SUCCESSBACK = ?, CELLS4G = ?, REFCNT = ?, UNKNOWNDB = ?, UNKNOWNSPACE  = ? WHERE FREQLOCNAME = ?";
        args = new Object[10];
        boolean z = false;
        args[0] = Integer.valueOf(this.lowRatCnt);
        args[1] = Integer.valueOf(this.inLteCnt);
        args[2] = Integer.valueOf(this.outLteCnt);
        args[3] = Integer.valueOf(this.fastBack);
        args[4] = Integer.valueOf(this.successBack);
        args[5] = Integer.valueOf(this.cells4G);
        args[6] = Integer.valueOf(this.refCnt);
        args[7] = Integer.valueOf(this.unknownDB);
        args[8] = Integer.valueOf(this.unknownSpace);
        args[9] = this.location;
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
        cValueBase.put("FREQLOCNAME", this.location);
        cValueBase.put("LOWRATCNT", Integer.valueOf(this.lowRatCnt));
        cValueBase.put("INLTECNT", Integer.valueOf(this.inLteCnt));
        cValueBase.put("OUTLTECNT", Integer.valueOf(this.outLteCnt));
        cValueBase.put("FASTBACK", Integer.valueOf(this.fastBack));
        cValueBase.put("SUCCESSBACK", Integer.valueOf(this.successBack));
        cValueBase.put("CELLS4G", Integer.valueOf(this.cells4G));
        cValueBase.put("REFCNT", Integer.valueOf(this.refCnt));
        cValueBase.put("UNKNOWNDB", Integer.valueOf(this.unknownDB));
        cValueBase.put("UNKNOWNSPACE", Integer.valueOf(this.unknownSpace));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertRecordByLoc in Back2LteChrTable  location:");
        stringBuilder.append(this.location);
        stringBuilder.append(":");
        stringBuilder.append(this.lowRatCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.inLteCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.outLteCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.fastBack);
        stringBuilder.append(":");
        stringBuilder.append(this.successBack);
        stringBuilder.append(":");
        stringBuilder.append(this.cells4G);
        stringBuilder.append(":");
        stringBuilder.append(this.refCnt);
        stringBuilder.append(":");
        stringBuilder.append(this.unknownDB);
        stringBuilder.append(":");
        stringBuilder.append(this.unknownSpace);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.FASTBACK2LTECHR_NAME, null, cValueBase);
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
        String sql = "UPDATE CHR_FASTBACK2LTE SET LOWRATCNT = 0, INLTECNT = 0, OUTLTECNT = 0, FASTBACK = 0, SUCCESSBACK = 0, CELLS4G = 0, REFCNT = 0, UNKNOWNDB = 0, UNKNOWNSPACE  = 0 WHERE FREQLOCNAME = ? ";
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
            stringBuilder.append("resetRecord by loc of STA_BACK2LTE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            resetFastBack2LteCount();
            resetFastBack2LteSumCount();
        }
    }

    public boolean resetRecord() {
        String sql = "UPDATE CHR_FASTBACK2LTE SET LOWRATCNT = 0, INLTECNT = 0, OUTLTECNT = 0, FASTBACK = 0, SUCCESSBACK = 0, CELLS4G = 0, REFCNT = 0, UNKNOWNDB = 0, UNKNOWNSPACE  = 0";
        boolean z = false;
        if (!getTotalCounters()) {
            return false;
        }
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, null);
            this.db.setTransactionSuccessful();
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resetRecord of STA_BACK2LTE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            resetFastBack2LteCount();
            resetFastBack2LteSumCount();
        }
    }

    public boolean delRecord() {
        String sql = "DELETE FROM CHR_FASTBACK2LTE";
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, null);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delRecord of STA_BACK2LTE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            this.db.endTransaction();
            return false;
        } catch (Throwable th) {
            this.db.endTransaction();
            throw th;
        }
    }

    public void addlowRatCnt() {
        this.lowRatCnt++;
    }

    public int getlowRatCnt() {
        return this.lowRatCnt;
    }

    public void addinLteCnt() {
        this.inLteCnt++;
    }

    public int getinLteCnt() {
        return this.inLteCnt;
    }

    public void addoutLteCnt() {
        this.outLteCnt++;
    }

    public int getoutLteCnt() {
        return this.outLteCnt;
    }

    public void addfastBack() {
        this.fastBack++;
    }

    public int getfastBack() {
        return this.fastBack;
    }

    public void addsuccessBack() {
        this.successBack++;
    }

    public int getsuccessBack() {
        return this.successBack;
    }

    public void setcells4G(int num) {
        this.cells4G = num;
    }

    public int getcells4G() {
        return this.cells4G;
    }

    public void addrefCnt() {
        this.refCnt++;
    }

    public int getrefCnt() {
        return this.refCnt;
    }

    public void addunknownDB() {
        this.unknownDB++;
    }

    public int getUnknown2DB() {
        return this.unknownDB;
    }

    public void addunknownSpace() {
        this.unknownSpace++;
    }

    public int getUnknown2Space() {
        return this.unknownSpace;
    }

    public void setLocation(String location) {
        if (location != null) {
            this.location = location;
            resetFastBack2LteCount();
        }
    }

    public String getLocation() {
        return this.location;
    }

    public int getSumlowRatCnt() {
        return this.sumlowRatCnt;
    }

    public int getSuminLteCnt() {
        return this.suminLteCnt;
    }

    public int getSumoutLteCnt() {
        return this.sumoutLteCnt;
    }

    public int getSumfastBack() {
        return this.sumfastBack;
    }

    public int getSumsuccessBack() {
        return this.sumsuccessBack;
    }

    public int getSumcells4G() {
        return this.sumcells4G;
    }

    public int getSumrefCnt() {
        return this.sumrefCnt;
    }

    public int getSumunknownDB() {
        return this.sumunknownDB;
    }

    public int getSumunknownSpace() {
        return this.sumunknownSpace;
    }
}

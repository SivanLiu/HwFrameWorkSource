package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.Arrays;

public class LocationDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(LocationDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public boolean insert(String frequentlocation) {
        if (frequentlocation == null) {
            LogUtil.d("insert failed ,frequentlocation == null");
            return false;
        } else if (0 < getOOBTime()) {
            return updateFrequentLocation(frequentlocation);
        } else {
            ContentValues cValue = new ContentValues();
            long now = System.currentTimeMillis();
            cValue.put("UPDATETIME", Long.valueOf(now));
            cValue.put("OOBTIME", Long.valueOf(now));
            cValue.put("FREQUENTLOCATION", frequentlocation);
            try {
                this.db.beginTransaction();
                this.db.insert(Constant.LOCATION_TABLE_NAME, null, cValue);
                this.db.setTransactionSuccessful();
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("insert exception: ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return false;
            } finally {
                this.db.endTransaction();
                return false;
            }
            return true;
        }
    }

    public boolean insertOOBTime(long OOBTime) {
        if (0 < getOOBTime()) {
            return false;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("OOBTIME", Long.valueOf(OOBTime));
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.LOCATION_TABLE_NAME, null, cValue);
            this.db.setTransactionSuccessful();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            this.db.endTransaction();
            return false;
        }
        return true;
    }

    public boolean updateFrequentLocation(String frequentlocation) {
        if (frequentlocation == null) {
            LogUtil.d("update failure,frequentlocation == null");
            return false;
        }
        String sql = "UPDATE FREQUENT_LOCATION SET UPDATETIME = ?,FREQUENTLOCATION = ?";
        Object[] args = new Object[2];
        args[0] = Long.valueOf(System.currentTimeMillis());
        boolean z = true;
        args[1] = frequentlocation;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: frequentlocation = ");
        stringBuilder.append(frequentlocation);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean updateBenefitCHRTime(long now) {
        if (0 == getOOBTime()) {
            LogUtil.i("updateBenefitCHRTime: OOBTime = 0");
            return false;
        }
        String sql = "UPDATE FREQUENT_LOCATION SET CHRBENEFITUPLOADTIME = ?";
        boolean z = true;
        Object[] args = new Object[]{Long.valueOf(now)};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: CHRBENEFITUPLOADTIME = ");
        stringBuilder.append(now);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean updateSpaceUserCHRTime(long now) {
        if (0 == getOOBTime()) {
            LogUtil.i("updateSpaceUserCHRTime: OOBTime = 0");
            return false;
        }
        String sql = "UPDATE FREQUENT_LOCATION SET CHRSPACEUSERUPLOADTIME = ?";
        boolean z = true;
        Object[] args = new Object[]{Long.valueOf(now)};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: CHRSPACEUSERUPLOADTIME = ");
        stringBuilder.append(now);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean remove() {
        try {
            this.db.execSQL("DELETE FROM FREQUENT_LOCATION", null);
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
            if (r0 == null) goto L_0x0079;
     */
    /* JADX WARNING: Missing block: B:19:0x0064, code:
            r2 = new java.lang.StringBuilder();
            r2.append("getFrequentLocation, frequentlocation:");
            r2.append(r0);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r2.toString());
     */
    /* JADX WARNING: Missing block: B:20:0x0079, code:
            com.android.server.hidata.wavemapping.util.LogUtil.d("getFrequentLocation, NO DATA");
     */
    /* JADX WARNING: Missing block: B:21:0x007e, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getFrequentLocation() {
        StringBuilder stringBuilder;
        String frequentlocation = null;
        Cursor cursor = null;
        if (this.db == null) {
            return null;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM FREQUENT_LOCATION", null);
            if (cursor.moveToNext()) {
                frequentlocation = cursor.getString(cursor.getColumnIndexOrThrow("FREQUENTLOCATION"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFrequentLocation IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFrequentLocation Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0023, code:
            if (r2 != null) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:14:0x0044, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:17:0x0060, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:18:0x0063, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getBenefitCHRTime, time:");
            r3.append(r0);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x0077, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getBenefitCHRTime() {
        StringBuilder stringBuilder;
        long time = 0;
        Cursor cursor = null;
        if (this.db == null) {
            return 0;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM FREQUENT_LOCATION", null);
            if (cursor.moveToNext()) {
                time = cursor.getLong(cursor.getColumnIndexOrThrow("CHRBENEFITUPLOADTIME"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getBenefitCHRTime IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getBenefitCHRTime Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0023, code:
            if (r2 != null) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:14:0x0044, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:17:0x0060, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:18:0x0063, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getSpaceUserCHRTime, time:");
            r3.append(r0);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x0077, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getSpaceUserCHRTime() {
        StringBuilder stringBuilder;
        long time = 0;
        Cursor cursor = null;
        if (this.db == null) {
            return 0;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM FREQUENT_LOCATION", null);
            if (cursor.moveToNext()) {
                time = cursor.getLong(cursor.getColumnIndexOrThrow("CHRSPACEUSERUPLOADTIME"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getSpaceUserCHRTime IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getSpaceUserCHRTime Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0023, code:
            if (r2 != null) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:14:0x0044, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:17:0x0060, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:18:0x0063, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getlastUpdateTime, time:");
            r3.append(r0);
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x0077, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getlastUpdateTime() {
        StringBuilder stringBuilder;
        long time = 0;
        Cursor cursor = null;
        if (this.db == null) {
            return 0;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM FREQUENT_LOCATION", null);
            if (cursor.moveToNext()) {
                time = cursor.getLong(cursor.getColumnIndexOrThrow("UPDATETIME"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getlastUpdateTime IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getlastUpdateTime Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0023, code:
            if (r2 != null) goto L_0x0025;
     */
    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:14:0x0044, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:17:0x0060, code:
            if (r2 == null) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:18:0x0063, code:
            r3 = new java.lang.StringBuilder();
            r3.append("getOOBTime, OOBTime:");
            r3.append(r0);
            com.android.server.hidata.wavemapping.util.LogUtil.d(r3.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x0077, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getOOBTime() {
        StringBuilder stringBuilder;
        long oob_time = 0;
        Cursor cursor = null;
        if (this.db == null) {
            return 0;
        }
        try {
            cursor = this.db.rawQuery("SELECT * FROM FREQUENT_LOCATION", null);
            if (cursor.moveToNext()) {
                oob_time = cursor.getLong(cursor.getColumnIndexOrThrow("OOBTIME"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getOOBTime IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getOOBTime Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean insertCHR(String freqlocation) {
        if (findCHRbyFreqLoc(freqlocation).containsKey("FREQLOCATION")) {
            return false;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("FREQLOCATION", freqlocation);
        long oobtime = getOOBTime();
        long now = System.currentTimeMillis();
        int firstreport = Math.round(((float) (now - oobtime)) / 1247525376);
        cValue.put("FIRSTREPORT", Integer.valueOf(firstreport));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertCHR, OOBTime:");
        stringBuilder.append(oobtime);
        stringBuilder.append(" now: ");
        stringBuilder.append(now);
        stringBuilder.append(" first report:");
        stringBuilder.append(firstreport);
        LogUtil.d(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.CHR_LOCATION_TABLE_NAME, null, cValue);
            this.db.setTransactionSuccessful();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("insertCHR exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return false;
        } finally {
            this.db.endTransaction();
            return false;
        }
        return true;
    }

    public boolean accCHREnterybyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHREnterybyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET ENTERY = ENTERY + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: entry  ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHREnterybyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRLeavebyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRLeavebyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET LEAVE = LEAVE + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: leave  ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRLeavebyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRSpaceChangebyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRSpaceChangebyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET SPACECHANGE = SPACECHANGE + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: SPACECHANGE  ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRSpaceChangebyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRSpaceLeavebyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRSpaceChangebyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET SPACELEAVE = SPACELEAVE + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: SPACELEAVE  ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRSpaceLeavebyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean addCHRDurationbyFreqLoc(int duration, String location) {
        if (location == null) {
            LogUtil.d("addCHRDurationbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET DURATION = DURATION + ? WHERE FREQLOCATION = ?";
        Object[] args = new Object[2];
        args[0] = Integer.valueOf(duration);
        boolean z = true;
        args[1] = location;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: add duration  ");
        stringBuilder.append(duration);
        stringBuilder.append(" location:");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("addCHRDurationbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefNoSwitchFailbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefNoSwitchFailbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPNOSWITCHFAIL = UPNOSWITCHFAIL + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPNOSWITCHFAIL at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefNoSwitchFailbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefAutoFailbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefAutoFailbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPAUTOFAIL = UPAUTOFAIL + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPAUTOFAIL at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefAutoFailbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefAutoSuccbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefAutoSuccbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPAUTOSUCC = UPAUTOSUCC + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPAUTOSUCC at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefAutoSuccbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefManualSuccbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefManualSuccbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPMANUALSUCC = UPMANUALSUCC + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPMANUALSUCC at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefManualSuccbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefTotalSwitchbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefTotalSwitchbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPTOTALSWITCH = UPTOTALSWITCH + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPTOTALSWITCH at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefTotalSwitchbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefQueryCntbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefQueryCntbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPQRYCNT = UPQRYCNT + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPQRYCNT at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefQueryCntbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefResCntbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefResCntbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPRESCNT = UPRESCNT + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPRESCNT at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefResCntbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefUnknownDBbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefUnknownDBbyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPUNKNOWNDB = UPUNKNOWNDB + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPUNKNOWNDB at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefUnknownDBbyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean accCHRUserPrefUnknownSpacebyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("accCHRUserPrefUnknownSpacebyFreqLoc failure,frequent location == null");
            return false;
        }
        insertCHR(location);
        String sql = "UPDATE CHR_FREQUENT_LOCATION SET UPUNKNOWNSPACE = UPUNKNOWNSPACE + 1 WHERE FREQLOCATION = ?";
        boolean z = true;
        Object[] args = new Object[]{location};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin: UPUNKNOWNSPACE at ");
        stringBuilder.append(location);
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("accCHRUserPrefUnknownSpacebyFreqLoc exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        } finally {
            z = this.db;
            z.endTransaction();
            return false;
        }
        return z;
    }

    public boolean resetCHRbyFreqLoc(String location) {
        if (location == null) {
            LogUtil.d("resetCHRbyFreqLoc failure,frequent location == null");
            return false;
        } else if (!findCHRbyFreqLoc(location).containsKey("FREQLOCATION")) {
            return false;
        } else {
            String sql = "UPDATE CHR_FREQUENT_LOCATION SET ENTERY = 0,LEAVE = 0,DURATION = 0,SPACECHANGE = 0,SPACELEAVE = 0,UPTOTALSWITCH = 0,UPAUTOSUCC = 0,UPMANUALSUCC = 0,UPAUTOFAIL = 0,UPNOSWITCHFAIL = 0,UPQRYCNT = 0,UPRESCNT = 0,UPUNKNOWNDB = 0,UPUNKNOWNSPACE = 0,LPTOTALSWITCH = 0,LPDATARX = 0,LPDATATX = 0,LPDURATION = 0,LPOFFSET = 0,LPALREADYBEST = 0,LPNOTREACH = 0,LPBACK = 0,LPUNKNOWNDB = 0,LPUNKNOWNSPACE = 0 WHERE FREQLOCATION = ?";
            boolean z = true;
            Object[] args = new Object[]{location};
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update begin: RESET CHR at ");
            stringBuilder.append(location);
            LogUtil.i(stringBuilder.toString());
            try {
                this.db.beginTransaction();
                this.db.execSQL(sql, args);
                this.db.setTransactionSuccessful();
            } catch (SQLException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("resetCHRbyFreqLoc exception: ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return false;
            } finally {
                z = this.db;
                z.endTransaction();
                return false;
            }
            return z;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:83:0x0440  */
    /* JADX WARNING: Missing block: B:73:0x0414, code:
            if (r4 == null) goto L_0x043c;
     */
    /* JADX WARNING: Missing block: B:74:0x0416, code:
            r4.close();
     */
    /* JADX WARNING: Missing block: B:79:0x0439, code:
            if (r4 == null) goto L_0x043c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Bundle findCHRbyFreqLoc(String freqlocation) {
        Bundle results;
        IllegalArgumentException e;
        Exception e2;
        Throwable th;
        StringBuilder stringBuilder;
        String str = freqlocation;
        Bundle results2 = new Bundle();
        Cursor cursor = null;
        if (this.db == null) {
            results = results2;
        } else if (str == null) {
            results = results2;
        } else {
            String sql = "SELECT * FROM CHR_FREQUENT_LOCATION WHERE FREQLOCATION = ?";
            String[] args = new String[]{str};
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("findCHRbyFreqLocation: sql=");
            stringBuilder2.append(sql);
            stringBuilder2.append(" ,args=");
            stringBuilder2.append(Arrays.toString(args));
            LogUtil.i(stringBuilder2.toString());
            String sql2;
            String[] args2;
            try {
                Cursor cursor2;
                Cursor cursor3;
                cursor = this.db.rawQuery(sql, args);
                while (cursor.moveToNext()) {
                    try {
                        try {
                            int firstreport = cursor.getInt(cursor.getColumnIndexOrThrow("FIRSTREPORT"));
                            int entery = cursor.getInt(cursor.getColumnIndexOrThrow("ENTERY"));
                            int leave = cursor.getInt(cursor.getColumnIndexOrThrow("LEAVE"));
                            int duration = cursor.getInt(cursor.getColumnIndexOrThrow(Constant.USERDB_APP_NAME_DURATION));
                            int spacechange = cursor.getInt(cursor.getColumnIndexOrThrow("SPACECHANGE"));
                            int spaceleave = cursor.getInt(cursor.getColumnIndexOrThrow("SPACELEAVE"));
                            int uptotalswitch = cursor.getInt(cursor.getColumnIndexOrThrow("UPTOTALSWITCH"));
                            int upautosucc = cursor.getInt(cursor.getColumnIndexOrThrow("UPAUTOSUCC"));
                            int upmanualsucc = cursor.getInt(cursor.getColumnIndexOrThrow("UPMANUALSUCC"));
                            int upautofail = cursor.getInt(cursor.getColumnIndexOrThrow("UPAUTOFAIL"));
                            int upnoswitchfail = cursor.getInt(cursor.getColumnIndexOrThrow("UPNOSWITCHFAIL"));
                            sql2 = sql;
                            try {
                                int upqrycnt = cursor.getInt(cursor.getColumnIndexOrThrow("UPQRYCNT"));
                                args2 = args;
                                try {
                                    int uprescnt = cursor.getInt(cursor.getColumnIndexOrThrow("UPRESCNT"));
                                    int upunknowndb = cursor.getInt(cursor.getColumnIndexOrThrow("UPUNKNOWNDB"));
                                    int upunknownspace = cursor.getInt(cursor.getColumnIndexOrThrow("UPUNKNOWNSPACE"));
                                    int lptotalswitch = cursor.getInt(cursor.getColumnIndexOrThrow("LPTOTALSWITCH"));
                                    int lpdatarx = cursor.getInt(cursor.getColumnIndexOrThrow("LPDATARX"));
                                    int lpdatatx = cursor.getInt(cursor.getColumnIndexOrThrow("LPDATATX"));
                                    int lpduration = cursor.getInt(cursor.getColumnIndexOrThrow("LPDURATION"));
                                    int lpoffset = cursor.getInt(cursor.getColumnIndexOrThrow("LPOFFSET"));
                                    int lpalreadybest = cursor.getInt(cursor.getColumnIndexOrThrow("LPALREADYBEST"));
                                    int lpnotreach = cursor.getInt(cursor.getColumnIndexOrThrow("LPNOTREACH"));
                                    int lpback = cursor.getInt(cursor.getColumnIndexOrThrow("LPBACK"));
                                    int lpunknowndb = cursor.getInt(cursor.getColumnIndexOrThrow("LPUNKNOWNDB"));
                                    int lpunknownspace = cursor.getInt(cursor.getColumnIndexOrThrow("LPUNKNOWNSPACE"));
                                    cursor2 = cursor;
                                    try {
                                        results2.putString("FREQLOCATION", str);
                                        results2.putInt("FIRSTREPORT", firstreport);
                                        results2.putInt("ENTERY", entery);
                                        results2.putInt("LEAVE", leave);
                                        results2.putInt(Constant.USERDB_APP_NAME_DURATION, duration);
                                        results2.putInt("SPACECHANGE", spacechange);
                                        results2.putInt("SPACELEAVE", spaceleave);
                                        results2.putInt("UPTOTALSWITCH", uptotalswitch);
                                        results2.putInt("UPAUTOSUCC", upautosucc);
                                        results2.putInt("UPMANUALSUCC", upmanualsucc);
                                        results2.putInt("UPAUTOFAIL", upautofail);
                                        results2.putInt("UPNOSWITCHFAIL", upnoswitchfail);
                                        results2.putInt("UPQRYCNT", upqrycnt);
                                        int upqrycnt2 = upqrycnt;
                                        upqrycnt = uprescnt;
                                        results2.putInt("UPRESCNT", upqrycnt);
                                        int uprescnt2 = upqrycnt;
                                        upqrycnt = upunknowndb;
                                        results2.putInt("UPUNKNOWNDB", upqrycnt);
                                        int upunknowndb2 = upqrycnt;
                                        upqrycnt = upunknownspace;
                                        results2.putInt("UPUNKNOWNSPACE", upqrycnt);
                                        int upunknownspace2 = upqrycnt;
                                        upqrycnt = lptotalswitch;
                                        results2.putInt("LPTOTALSWITCH", upqrycnt);
                                        int lptotalswitch2 = upqrycnt;
                                        upqrycnt = lpdatarx;
                                        results2.putInt("LPDATARX", upqrycnt);
                                        int lpdatarx2 = upqrycnt;
                                        upqrycnt = lpdatatx;
                                        results2.putInt("LPDATATX", upqrycnt);
                                        int lpdatatx2 = upqrycnt;
                                        upqrycnt = lpduration;
                                        results2.putInt("LPDURATION", upqrycnt);
                                        int lpduration2 = upqrycnt;
                                        upqrycnt = lpoffset;
                                        results2.putInt("LPOFFSET", upqrycnt);
                                        int lpoffset2 = upqrycnt;
                                        upqrycnt = lpalreadybest;
                                        results2.putInt("LPALREADYBEST", upqrycnt);
                                        int lpalreadybest2 = upqrycnt;
                                        upqrycnt = lpnotreach;
                                        results2.putInt("LPNOTREACH", upqrycnt);
                                        int lpnotreach2 = upqrycnt;
                                        upqrycnt = lpback;
                                        results2.putInt("LPBACK", upqrycnt);
                                        int lpback2 = upqrycnt;
                                        upqrycnt = lpunknowndb;
                                        results2.putInt("LPUNKNOWNDB", upqrycnt);
                                        results2.putInt("LPUNKNOWNSPACE", lpunknownspace);
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        results = results2;
                                        try {
                                            stringBuilder3.append(" freqlocation:");
                                            stringBuilder3.append(str);
                                            stringBuilder3.append(",first report:");
                                            stringBuilder3.append(firstreport);
                                            stringBuilder3.append(",entery:");
                                            stringBuilder3.append(entery);
                                            stringBuilder3.append(",leave:");
                                            stringBuilder3.append(leave);
                                            stringBuilder3.append(",duration:");
                                            stringBuilder3.append(duration);
                                            stringBuilder3.append(",space change:");
                                            stringBuilder3.append(spacechange);
                                            stringBuilder3.append(",space leave:");
                                            stringBuilder3.append(spaceleave);
                                            LogUtil.i(stringBuilder3.toString());
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append(" uptotalswitch:");
                                            stringBuilder4.append(uptotalswitch);
                                            stringBuilder4.append(",upautosucc:");
                                            stringBuilder4.append(upautosucc);
                                            stringBuilder4.append(",upmanualsucc:");
                                            stringBuilder4.append(upmanualsucc);
                                            stringBuilder4.append(",upautofail:");
                                            stringBuilder4.append(upautofail);
                                            stringBuilder4.append(",upnoswitchfail:");
                                            stringBuilder4.append(upnoswitchfail);
                                            stringBuilder4.append(",upqrycnt:");
                                            stringBuilder4.append(upqrycnt2);
                                            stringBuilder4.append(",uprescnt:");
                                            firstreport = uprescnt2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(" upunknowndb:");
                                            firstreport = upunknowndb2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(" upunknownspace:");
                                            firstreport = upunknownspace2;
                                            stringBuilder4.append(firstreport);
                                            LogUtil.i(stringBuilder4.toString());
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append(" lptotalswitch:");
                                            firstreport = lptotalswitch2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpdatarx:");
                                            firstreport = lpdatarx2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpdatatx:");
                                            firstreport = lpdatatx2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpduration:");
                                            firstreport = lpduration2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpoffset:");
                                            firstreport = lpoffset2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpalreadybest:");
                                            firstreport = lpalreadybest2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(",lpnotreach:");
                                            firstreport = lpnotreach2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(" lpback:");
                                            firstreport = lpback2;
                                            stringBuilder4.append(firstreport);
                                            stringBuilder4.append(" lpunknowndb:");
                                            stringBuilder4.append(upqrycnt);
                                            stringBuilder4.append(" lpunknownspace:");
                                            stringBuilder4.append(lpunknownspace);
                                            LogUtil.i(stringBuilder4.toString());
                                            sql = sql2;
                                            args = args2;
                                            cursor = cursor2;
                                            results2 = results;
                                        } catch (IllegalArgumentException e3) {
                                            e = e3;
                                            cursor = cursor2;
                                        } catch (Exception e4) {
                                            e2 = e4;
                                            cursor = cursor2;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            cursor = cursor2;
                                        }
                                    } catch (IllegalArgumentException e5) {
                                        e = e5;
                                        results = results2;
                                        cursor = cursor2;
                                    } catch (Exception e6) {
                                        e2 = e6;
                                        results = results2;
                                        cursor = cursor2;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        results = results2;
                                        cursor = cursor2;
                                    }
                                } catch (IllegalArgumentException e7) {
                                    e = e7;
                                    results = results2;
                                    cursor2 = cursor;
                                } catch (Exception e8) {
                                    e2 = e8;
                                    results = results2;
                                    cursor2 = cursor;
                                } catch (Throwable th4) {
                                    th = th4;
                                    results = results2;
                                    cursor2 = cursor;
                                }
                            } catch (IllegalArgumentException e9) {
                                e = e9;
                                results = results2;
                                cursor2 = cursor;
                                args2 = args;
                            } catch (Exception e10) {
                                e2 = e10;
                                results = results2;
                                cursor2 = cursor;
                                args2 = args;
                            } catch (Throwable th5) {
                                th = th5;
                                results = results2;
                                cursor2 = cursor;
                                args2 = args;
                            }
                        } catch (IllegalArgumentException e11) {
                            e = e11;
                            results = results2;
                            cursor2 = cursor;
                            sql2 = sql;
                            args2 = args;
                        } catch (Exception e12) {
                            e2 = e12;
                            results = results2;
                            cursor2 = cursor;
                            sql2 = sql;
                            args2 = args;
                        } catch (Throwable th6) {
                            th = th6;
                            results = results2;
                            cursor2 = cursor;
                            sql2 = sql;
                            args2 = args;
                        }
                    } catch (IllegalArgumentException e13) {
                        e = e13;
                        results = results2;
                        cursor3 = cursor;
                        sql2 = sql;
                        args2 = args;
                    } catch (Exception e14) {
                        e2 = e14;
                        results = results2;
                        cursor3 = cursor;
                        sql2 = sql;
                        args2 = args;
                    } catch (Throwable th7) {
                        th = th7;
                        results = results2;
                        cursor3 = cursor;
                        sql2 = sql;
                        args2 = args;
                    }
                }
                results = results2;
                cursor2 = cursor;
                sql2 = sql;
                args2 = args;
                if (cursor2 != null) {
                    cursor3 = cursor2;
                    cursor3.close();
                } else {
                    cursor3 = cursor2;
                }
                cursor = cursor3;
            } catch (IllegalArgumentException e15) {
                e = e15;
                results = results2;
                sql2 = sql;
                args2 = args;
                stringBuilder = new StringBuilder();
                stringBuilder.append("findCHRbyFreqLocation IllegalArgumentException: ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e16) {
                e2 = e16;
                results = results2;
                sql2 = sql;
                args2 = args;
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findCHRbyFreqLocation Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Throwable th8) {
                    th = th8;
                    if (cursor != null) {
                    }
                    throw th;
                }
            } catch (Throwable th9) {
                th = th9;
                results = results2;
                sql2 = sql;
                args2 = args;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
            return results;
        }
        return results;
    }
}

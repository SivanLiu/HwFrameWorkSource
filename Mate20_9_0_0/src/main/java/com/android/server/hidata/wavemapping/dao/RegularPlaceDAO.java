package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.HashMap;

public class RegularPlaceDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(RegularPlaceDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public boolean insert(RegularPlaceInfo location) {
        if (findBySsid(location.getPlace(), location.isMainAp()) != null) {
            return true;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("SSID", location.getPlace());
        cValue.put("STATE", Integer.valueOf(location.getState()));
        cValue.put("BATCH", Integer.valueOf(location.getBatch()));
        cValue.put("FINGERNUM", Integer.valueOf(location.getFingerNum()));
        cValue.put("TEST_DAT_NUM", Integer.valueOf(location.getTestDataNum()));
        cValue.put("DISNUM", Integer.valueOf(location.getDisNum()));
        cValue.put("IDENTIFYNUM", Integer.valueOf(location.getIdentifyNum()));
        cValue.put("NO_OCURBSSIDS", location.getNoOcurBssids());
        cValue.put("MODELNAME", location.getModelName());
        cValue.put("BEGINTIME", Integer.valueOf(new TimeUtil().time2IntDate(TimeUtil.getTime())));
        cValue.put("ISMAINAP", String.valueOf(location.isMainAp() ? 1 : null));
        try {
            this.db.insert(Constant.REGULAR_PLACESTATE_TABLE_NAME, null, cValue);
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public boolean update(RegularPlaceInfo location) {
        if (location == null) {
            LogUtil.d("update failure,null == location");
            return false;
        }
        String i = String.valueOf(location.isMainAp() ? 1 : null);
        String sql = "UPDATE RGL_PLACESTATE SET STATE = ?,BATCH = ?,FINGERNUM= ? ,UPTIME = ?,TEST_DAT_NUM = ?,DISNUM = ?,IDENTIFYNUM = ?,NO_OCURBSSIDS = ?,MODELNAME = ? WHERE ISMAINAP = ? AND SSID = ? ";
        Object[] args = new Object[]{Integer.valueOf(location.getState()), Integer.valueOf(location.getBatch()), Integer.valueOf(location.getFingerNum()), TimeUtil.getTime(), Integer.valueOf(location.getTestDataNum()), Integer.valueOf(location.getDisNum()), Integer.valueOf(location.getIdentifyNum()), location.getNoOcurBssids(), location.getModelName(), i, location.getPlace()};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin, sql=");
        stringBuilder.append(sql);
        LogUtil.d(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("            , location=");
        stringBuilder.append(location.toString());
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.execSQL(sql, args);
            return true;
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public RegularPlaceInfo addRegularLocation(String place, boolean isMainAp) {
        RegularPlaceInfo regularPlaceInfo = null;
        if (place == null) {
            return null;
        }
        try {
            regularPlaceInfo = new RegularPlaceInfo(place, 3, 1, 0, 0, 0, 0, "", isMainAp);
            insert(regularPlaceInfo);
            return regularPlaceInfo;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addRegularLocation,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return regularPlaceInfo;
        }
    }

    public boolean remove(String place, boolean isMainAp) {
        String sql = "DELETE FROM RGL_PLACESTATE  WHERE SSID = ? and ISMAINAP = ? ";
        String i = String.valueOf(isMainAp ? 1 : null);
        try {
            this.db.execSQL(sql, new Object[]{place, i});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x00bb, code:
            if (r5 != null) goto L_0x00bd;
     */
    /* JADX WARNING: Missing block: B:16:0x00bd, code:
            r5.close();
     */
    /* JADX WARNING: Missing block: B:21:0x00dc, code:
            if (r5 == null) goto L_0x00fb;
     */
    /* JADX WARNING: Missing block: B:24:0x00f8, code:
            if (r5 == null) goto L_0x00fb;
     */
    /* JADX WARNING: Missing block: B:25:0x00fb, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public RegularPlaceInfo findAllBySsid(String place, boolean isMainAp) {
        StringBuilder stringBuilder;
        String str = place;
        RegularPlaceInfo placeInfo = null;
        if (str == null || str.equals("")) {
            boolean z = isMainAp;
            LogUtil.d("findBySsid place=null or place=");
            return null;
        }
        String sql = "SELECT SSID,STATE,BATCH,FINGERNUM,TEST_DAT_NUM,DISNUM,IDENTIFYNUM,NO_OCURBSSIDS,ISMAINAP,MODELNAME,BEGINTIME FROM RGL_PLACESTATE WHERE SSID = ? and ISMAINAP = ? ";
        Cursor cursor = null;
        if (this.db == null) {
            return null;
        }
        try {
            String i = String.valueOf(isMainAp ? 1 : null);
            cursor = this.db.rawQuery(sql, new String[]{str, i});
            if (cursor.moveToNext()) {
                placeInfo = new RegularPlaceInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getInt(cursor.getColumnIndexOrThrow("STATE")), cursor.getInt(cursor.getColumnIndexOrThrow("BATCH")), cursor.getInt(cursor.getColumnIndexOrThrow("FINGERNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("TEST_DAT_NUM")), cursor.getInt(cursor.getColumnIndexOrThrow("DISNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("IDENTIFYNUM")), cursor.getString(cursor.getColumnIndexOrThrow("NO_OCURBSSIDS")), cursor.getString(cursor.getColumnIndexOrThrow("ISMAINAP")).equals("1"));
                placeInfo.setModelName(cursor.getString(cursor.getColumnIndexOrThrow("MODELNAME")));
                placeInfo.setBeginTime(cursor.getInt(cursor.getColumnIndexOrThrow("BEGINTIME")));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsid IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsid Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x00ae, code:
            if (r5 != null) goto L_0x00b0;
     */
    /* JADX WARNING: Missing block: B:16:0x00b0, code:
            r5.close();
     */
    /* JADX WARNING: Missing block: B:21:0x00cf, code:
            if (r5 == null) goto L_0x00ee;
     */
    /* JADX WARNING: Missing block: B:24:0x00eb, code:
            if (r5 == null) goto L_0x00ee;
     */
    /* JADX WARNING: Missing block: B:25:0x00ee, code:
            return r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public RegularPlaceInfo findBySsid(String place, boolean isMainAp) {
        StringBuilder stringBuilder;
        String str = place;
        RegularPlaceInfo placeInfo = null;
        if (str == null || str.equals("")) {
            boolean z = isMainAp;
            LogUtil.d("findBySsid place=null or place=");
            return null;
        }
        String sql = "SELECT SSID,STATE,BATCH,FINGERNUM,TEST_DAT_NUM,DISNUM,IDENTIFYNUM,NO_OCURBSSIDS,ISMAINAP,MODELNAME FROM RGL_PLACESTATE WHERE SSID = ? and ISMAINAP = ? ";
        Cursor cursor = null;
        if (this.db == null) {
            return null;
        }
        try {
            String i = String.valueOf(isMainAp ? 1 : null);
            cursor = this.db.rawQuery(sql, new String[]{str, i});
            if (cursor.moveToNext()) {
                placeInfo = new RegularPlaceInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getInt(cursor.getColumnIndexOrThrow("STATE")), cursor.getInt(cursor.getColumnIndexOrThrow("BATCH")), cursor.getInt(cursor.getColumnIndexOrThrow("FINGERNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("TEST_DAT_NUM")), cursor.getInt(cursor.getColumnIndexOrThrow("DISNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("IDENTIFYNUM")), cursor.getString(cursor.getColumnIndexOrThrow("NO_OCURBSSIDS")), cursor.getString(cursor.getColumnIndexOrThrow("ISMAINAP")).equals("1"));
                placeInfo.setModelName(cursor.getString(cursor.getColumnIndexOrThrow("MODELNAME")));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsid IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsid Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x00cd, code:
            if (r3 != null) goto L_0x00cf;
     */
    /* JADX WARNING: Missing block: B:8:0x00cf, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:13:0x00ee, code:
            if (r3 == null) goto L_0x010d;
     */
    /* JADX WARNING: Missing block: B:16:0x010a, code:
            if (r3 == null) goto L_0x010d;
     */
    /* JADX WARNING: Missing block: B:17:0x010d, code:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<String, RegularPlaceInfo> findAllLocations() {
        StringBuilder stringBuilder;
        String sql = "SELECT SSID,STATE,BATCH,FINGERNUM,TEST_DAT_NUM,IDENTIFYNUM,DISNUM,NO_OCURBSSIDS,ISMAINAP,MODELNAME FROM RGL_PLACESTATE WHERE 1 = 1 ";
        Cursor cursor = null;
        HashMap<String, RegularPlaceInfo> placeInfoHashMap = new HashMap();
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                RegularPlaceInfo placeInfo = new RegularPlaceInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getInt(cursor.getColumnIndexOrThrow("STATE")), cursor.getInt(cursor.getColumnIndexOrThrow("BATCH")), cursor.getInt(cursor.getColumnIndexOrThrow("FINGERNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("TEST_DAT_NUM")), cursor.getInt(cursor.getColumnIndexOrThrow("DISNUM")), cursor.getInt(cursor.getColumnIndexOrThrow("IDENTIFYNUM")), cursor.getString(cursor.getColumnIndexOrThrow("NO_OCURBSSIDS")), cursor.getString(cursor.getColumnIndexOrThrow("ISMAINAP")).equals("1"));
                placeInfo.setModelName(cursor.getString(cursor.getColumnIndexOrThrow("MODELNAME")));
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" findAllLocations:place:");
                stringBuilder2.append(placeInfo.toString());
                LogUtil.d(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(placeInfo.getPlace());
                stringBuilder2.append(Constant.RESULT_SEPERATE);
                stringBuilder2.append(cursor.getString(cursor.getColumnIndexOrThrow("ISMAINAP")));
                placeInfoHashMap.put(stringBuilder2.toString(), placeInfo);
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllLocations IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllLocations Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

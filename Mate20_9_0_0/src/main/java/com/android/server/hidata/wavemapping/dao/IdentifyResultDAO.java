package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.IdentifyResult;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;

public class IdentifyResultDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(IdentifyResultDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:7:0x0057, code skipped:
            if (r2 != null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:17:0x009b, code skipped:
            if (r2 == null) goto L_0x009e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<IdentifyResult> findAll() {
        String str;
        StringBuilder stringBuilder;
        String sql = "SELECT PRELABLE,SSID,SERVERMAC,UPTIME,MODELNAME FROM IDENTIFY_RESULT";
        List<IdentifyResult> identifyResultList = new ArrayList();
        Cursor cursor = null;
        String[] tempMacs = null;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                IdentifyResult tempIdentifyResult = new IdentifyResult();
                tempIdentifyResult.setPreLabel(cursor.getInt(cursor.getColumnIndexOrThrow("PRELABLE")));
                tempIdentifyResult.setSsid(cursor.getString(cursor.getColumnIndexOrThrow("SSID")));
                tempIdentifyResult.setServeMac(cursor.getString(cursor.getColumnIndexOrThrow("SERVERMAC")));
                tempIdentifyResult.setModelName(cursor.getString(cursor.getColumnIndexOrThrow("MODELNAME")));
                identifyResultList.add(tempIdentifyResult);
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll Exception: ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            return identifyResultList;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x001e, code skipped:
            if (r1 != null) goto L_0x0020;
     */
    /* JADX WARNING: Missing block: B:17:0x0062, code skipped:
            if (r1 == null) goto L_0x0065;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int findAllCount() {
        String str;
        StringBuilder stringBuilder;
        String sql = "SELECT count(1) as CNT FROM IDENTIFY_RESULT";
        Cursor cursor = null;
        int allCnt = 0;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                allCnt = cursor.getInt(cursor.getColumnIndexOrThrow("CNT"));
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll Exception: ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            return allCnt;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean remove(String ssid, boolean isMainAp) {
        String sql = "DELETE FROM IDENTIFY_RESULT  WHERE SSID = ? and ISMAINAP = ? ";
        String i = String.valueOf(isMainAp ? 1 : null);
        try {
            this.db.execSQL(sql, new Object[]{ssid, i});
            return true;
        } catch (SQLException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean insert(IdentifyResult identifyResult, boolean isMainAp) {
        try {
            ContentValues cValue = new ContentValues();
            cValue.put("SSID", identifyResult.getSsid());
            cValue.put("PRELABLE", Integer.valueOf(identifyResult.getPreLabel()));
            cValue.put("SERVERMAC", identifyResult.getServeMac());
            cValue.put("MODELNAME", identifyResult.getModelName());
            cValue.put("UPTIME", TimeUtil.getTime());
            cValue.put("ISMAINAP", String.valueOf(isMainAp ? 1 : null));
            this.db.insert(Constant.IDENTIFY_RESULT_TABLE_NAME, null, cValue);
            return true;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0079, code skipped:
            if (r3 != null) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:27:0x00ba, code skipped:
            if (r3 == null) goto L_0x00bd;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<IdentifyResult> findBySsid(String ssid, boolean isMainAp) {
        String str;
        StringBuilder stringBuilder;
        List<IdentifyResult> identifyResultList = new ArrayList();
        if (ssid == null || ssid.equals("")) {
            LogUtil.d("findBySsid ssid=null or ssid=");
            return identifyResultList;
        } else if (this.db == null) {
            return identifyResultList;
        } else {
            String sql = "SELECT SSID,PRELABLE,SERVERMAC,UPTIME,MODELNAME FROM IDENTIFY_RESULT WHERE SSID = ? AND ISMAINAP = ? ";
            Cursor cursor = null;
            try {
                String i = String.valueOf(isMainAp ? 1 : null);
                cursor = this.db.rawQuery(sql, new String[]{ssid, i});
                while (cursor.moveToNext()) {
                    IdentifyResult tempIdentifyResult = new IdentifyResult();
                    tempIdentifyResult.setPreLabel(cursor.getInt(cursor.getColumnIndexOrThrow("PRELABLE")));
                    tempIdentifyResult.setSsid(cursor.getString(cursor.getColumnIndexOrThrow("SSID")));
                    tempIdentifyResult.setServeMac(cursor.getString(cursor.getColumnIndexOrThrow("SERVERMAC")));
                    tempIdentifyResult.setModelName(cursor.getString(cursor.getColumnIndexOrThrow("MODELNAME")));
                    identifyResultList.add(tempIdentifyResult);
                }
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("findBySsid IllegalArgumentException: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("findBySsid Exception: ");
                stringBuilder.append(e2.getMessage());
                Log.e(str, stringBuilder.toString());
                if (cursor != null) {
                    cursor.close();
                }
                return identifyResultList;
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }
}

package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.ApInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnterpriseApDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(EnterpriseApDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public boolean insert(ApInfo apInfo) {
        if (findBySsid(apInfo.getSsid()) != null) {
            return true;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("SSID", apInfo.getSsid());
        cValue.put("UPTIME", apInfo.getUptime());
        try {
            this.db.insert(Constant.ENTERPRISE_AP_TABLE_NAME, null, cValue);
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0019, code:
            if (r3 != null) goto L_0x001b;
     */
    /* JADX WARNING: Missing block: B:8:0x001b, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:13:0x003a, code:
            if (r3 == null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:16:0x0059, code:
            if (r3 == null) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:17:0x005c, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int findAllCount() {
        StringBuilder stringBuilder;
        String sql = "SELECT count(1) FROM ENTERPRISE_AP WHERE 1 = 1";
        int cnt = 0;
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                cnt = cursor.getInt(0);
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0024, code:
            if (r3 != null) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:8:0x0026, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:13:0x0045, code:
            if (r3 == null) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:16:0x0064, code:
            if (r3 == null) goto L_0x0067;
     */
    /* JADX WARNING: Missing block: B:17:0x0067, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Set<String> findAll() {
        StringBuilder stringBuilder;
        String sql = "SELECT SSID FROM ENTERPRISE_AP WHERE 1 = 1";
        Set<String> aps = new HashSet();
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                aps.add(cursor.getString(cursor.getColumnIndexOrThrow("SSID")));
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAll Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0033, code:
            if (r3 != null) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:8:0x0035, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:13:0x0054, code:
            if (r3 == null) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:16:0x0073, code:
            if (r3 == null) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:17:0x0076, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<ApInfo> findAllAps() {
        StringBuilder stringBuilder;
        String sql = "SELECT SSID,MAC,UPTIME FROM ENTERPRISE_AP WHERE 1 = 1";
        List<ApInfo> apInfos = new ArrayList();
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                apInfos.add(new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME"))));
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllAps IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllAps Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean update(ApInfo apInfo) {
        if (apInfo == null) {
            LogUtil.d("update failure,null == place");
            return false;
        }
        try {
            this.db.execSQL("UPDATE ENTERPRISE_AP SET UPTIME = ? WHERE SSID = ? ", new Object[]{apInfo.getUptime(), apInfo.getSsid()});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public boolean remove(String ssid) {
        if (ssid == null || ssid.equals("")) {
            return false;
        }
        try {
            this.db.execSQL("DELETE FROM ENTERPRISE_AP  WHERE SSID = ? ", new Object[]{ssid});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0042, code:
            if (r2 != null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:13:0x0044, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:18:0x0063, code:
            if (r2 == null) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:21:0x007f, code:
            if (r2 == null) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:22:0x0082, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApInfo findBySsid(String ssid) {
        StringBuilder stringBuilder;
        ApInfo apInfo = null;
        if (ssid == null || ssid.equals("")) {
            LogUtil.d("findBySsid ssid=null or ssid=");
            return null;
        }
        String sql = "SELECT SSID,UPTIME FROM ENTERPRISE_AP WHERE SSID = ? ";
        Cursor cursor = null;
        if (this.db == null) {
            return null;
        }
        try {
            cursor = this.db.rawQuery(sql, new String[]{ssid});
            if (cursor.moveToNext()) {
                apInfo = new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME")));
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

    /* JADX WARNING: Missing block: B:12:0x0042, code:
            if (r2 != null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:13:0x0044, code:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:21:0x006a, code:
            if (r2 == null) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:24:0x0086, code:
            if (r2 == null) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:25:0x0089, code:
            if (r0 == null) goto L_0x00b5;
     */
    /* JADX WARNING: Missing block: B:26:0x008b, code:
            r0.setUptime(com.android.server.hidata.wavemapping.util.TimeUtil.getTime());
     */
    /* JADX WARNING: Missing block: B:27:0x0096, code:
            if (update(r0) != false) goto L_0x00b5;
     */
    /* JADX WARNING: Missing block: B:28:0x0098, code:
            com.android.server.hidata.wavemapping.util.LogUtil.d("findBySsidForUpdateTime update failure");
            r3 = new java.lang.StringBuilder();
            r3.append("                                      ,apinfo: ");
            r3.append(r0.toString());
            com.android.server.hidata.wavemapping.util.LogUtil.i(r3.toString());
     */
    /* JADX WARNING: Missing block: B:29:0x00b5, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApInfo findBySsidForUpdateTime(String ssid) {
        StringBuilder stringBuilder;
        ApInfo apInfo = null;
        if (ssid == null || ssid.equals("")) {
            LogUtil.d("findBySsidForUpdateTime ssid=null or ssid=");
            return null;
        }
        String sql = "SELECT SSID,UPTIME FROM ENTERPRISE_AP WHERE SSID = ? ";
        Cursor cursor = null;
        if (this.db == null) {
            return null;
        }
        try {
            cursor = this.db.rawQuery(sql, new String[]{ssid});
            if (cursor.moveToNext()) {
                apInfo = new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME")));
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsidForUpdateTime IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findBySsidForUpdateTime Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

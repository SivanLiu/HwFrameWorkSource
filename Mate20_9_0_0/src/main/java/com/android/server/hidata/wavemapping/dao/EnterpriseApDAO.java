package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.ApInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
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

    /* JADX WARNING: Missing block: B:7:0x0019, code skipped:
            if (r3 != null) goto L_0x001b;
     */
    /* JADX WARNING: Missing block: B:17:0x0059, code skipped:
            if (r3 == null) goto L_0x005c;
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
            if (cursor != null) {
                cursor.close();
            }
            return cnt;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0024, code skipped:
            if (r3 != null) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:17:0x0064, code skipped:
            if (r3 == null) goto L_0x0067;
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
            if (cursor != null) {
                cursor.close();
            }
            return aps;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0033, code skipped:
            if (r3 != null) goto L_0x0035;
     */
    /* JADX WARNING: Missing block: B:17:0x0073, code skipped:
            if (r3 == null) goto L_0x0076;
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
            if (cursor != null) {
                cursor.close();
            }
            return apInfos;
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

    /* JADX WARNING: Missing block: B:12:0x0042, code skipped:
            if (r2 != null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:22:0x007f, code skipped:
            if (r2 == null) goto L_0x0082;
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
            if (cursor != null) {
                cursor.close();
            }
            return apInfo;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0042, code skipped:
            if (r2 != null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:25:0x0086, code skipped:
            if (r2 == null) goto L_0x0089;
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
            if (cursor != null) {
                cursor.close();
            }
            if (apInfo != null) {
                apInfo.setUptime(TimeUtil.getTime());
                if (!update(apInfo)) {
                    LogUtil.d("findBySsidForUpdateTime update failure");
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("                                      ,apinfo: ");
                    stringBuilder2.append(apInfo.toString());
                    LogUtil.i(stringBuilder2.toString());
                }
            }
            return apInfo;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

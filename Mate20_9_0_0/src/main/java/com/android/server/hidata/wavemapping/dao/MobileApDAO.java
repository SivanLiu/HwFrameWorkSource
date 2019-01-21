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
import java.util.List;

public class MobileApDAO {
    private static final String TAG;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(MobileApDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    /* JADX WARNING: Missing block: B:22:0x008c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean insert(ApInfo apInfo) {
        if (apInfo == null || apInfo.getSsid() == null || apInfo.getSsid().equals("") || apInfo.getMac() == null || apInfo.getMac().equals("")) {
            return false;
        }
        if (findBySsid(apInfo.getSsid(), apInfo.getMac()) != null) {
            return true;
        }
        ContentValues cValue = new ContentValues();
        cValue.put("SSID", apInfo.getSsid());
        cValue.put("MAC", apInfo.getMac());
        cValue.put("UPTIME", apInfo.getUptime());
        cValue.put("SRCTYPE", Integer.valueOf(apInfo.getSrcType()));
        try {
            this.db.insert(Constant.MOBILE_AP_TABLE_NAME, null, cValue);
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:7:0x003d, code skipped:
            if (r3 != null) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:17:0x007d, code skipped:
            if (r3 == null) goto L_0x0080;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<ApInfo> findAllAps() {
        StringBuilder stringBuilder;
        String sql = "SELECT SSID,MAC,UPTIME FROM MOBILE_AP WHERE 1 = 1";
        List<ApInfo> apInfos = new ArrayList();
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                apInfos.add(new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("MAC")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME"))));
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

    /* JADX WARNING: Missing block: B:7:0x0021, code skipped:
            if (r2 != null) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:17:0x0061, code skipped:
            if (r2 == null) goto L_0x0064;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int findAllCountBySrctype(int srctype) {
        StringBuilder stringBuilder;
        String sql = "SELECT count(1) FROM MOBILE_AP WHERE SRCTYPE = ? ";
        int cnt = 0;
        Cursor cursor = null;
        try {
            cursor = this.db.rawQuery(sql, new String[]{String.valueOf(srctype)});
            while (cursor.moveToNext()) {
                cnt = cursor.getInt(0);
            }
        } catch (IllegalArgumentException e) {
            LogUtil.d(sql);
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllCountBySrctype IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllCountBySrctype Exception: ");
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

    public boolean update(ApInfo apInfo) {
        if (apInfo == null) {
            LogUtil.d("update failure,null == place");
            return false;
        }
        String sql = "UPDATE MOBILE_AP SET UPTIME = ? WHERE SSID = ? AND MAC = ? ";
        Object[] args = new Object[]{apInfo.getUptime(), apInfo.getSsid(), apInfo.getMac()};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update begin:");
        stringBuilder.append(sql);
        stringBuilder.append("apInfo:");
        stringBuilder.append(apInfo.toString());
        LogUtil.i(stringBuilder.toString());
        try {
            this.db.execSQL(sql, args);
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("UPDATE exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0043, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean remove(String ssid, String mac) {
        if (ssid == null || ssid.equals("") || mac == null || mac.equals("")) {
            return false;
        }
        try {
            this.db.execSQL("DELETE FROM MOBILE_AP  WHERE SSID = ? AND MAC = ? ", new Object[]{ssid, mac});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:15:0x005b, code skipped:
            if (r2 != null) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:25:0x0098, code skipped:
            if (r2 == null) goto L_0x009b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApInfo findBySsid(String ssid, String mac) {
        StringBuilder stringBuilder;
        ApInfo apInfo = null;
        if (ssid == null || ssid.equals("")) {
            LogUtil.d("findBySsid ssid=null or ssid=");
            return null;
        } else if (mac == null || mac.equals("")) {
            LogUtil.d("findBySsid mac=null or mac=");
            return null;
        } else {
            String sql = "SELECT SSID,MAC,UPTIME FROM MOBILE_AP WHERE SSID = ? AND MAC = ? ";
            Cursor cursor = null;
            if (this.db == null) {
                return null;
            }
            try {
                cursor = this.db.rawQuery(sql, new String[]{ssid, mac});
                if (cursor.moveToNext()) {
                    apInfo = new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("MAC")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME")));
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
    }

    /* JADX WARNING: Missing block: B:15:0x005b, code skipped:
            if (r2 != null) goto L_0x005d;
     */
    /* JADX WARNING: Missing block: B:25:0x0098, code skipped:
            if (r2 == null) goto L_0x009b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ApInfo findBySsidForUpdateTime(String ssid, String bssid) {
        StringBuilder stringBuilder;
        ApInfo apInfo = null;
        if (ssid == null || ssid.equals("")) {
            LogUtil.d("findBySsidForUpdateTime ssid=null or ssid =");
            return null;
        } else if (bssid == null || bssid.equals("")) {
            LogUtil.d("findBySsidForUpdateTime bssid=null or bssid =");
            return null;
        } else {
            String sql = "SELECT SSID,MAC,UPTIME FROM MOBILE_AP WHERE SSID = ? AND MAC = ? ";
            Cursor cursor = null;
            if (this.db == null) {
                return null;
            }
            try {
                cursor = this.db.rawQuery(sql, new String[]{ssid, bssid});
                if (cursor.moveToNext()) {
                    apInfo = new ApInfo(cursor.getString(cursor.getColumnIndexOrThrow("SSID")), cursor.getString(cursor.getColumnIndexOrThrow("MAC")), cursor.getString(cursor.getColumnIndexOrThrow("UPTIME")));
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
}

package com.android.server.wifi.wifipro.hwintelligencewifi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.android.server.wifi.wifipro.HwDualBandMessageUtil;
import com.android.server.wifi.wifipro.WifiproUtils;
import com.android.server.wifi.wifipro.wifiscangenie.WifiScanGenieDataBaseImpl;
import java.util.ArrayList;
import java.util.List;

public class DataBaseManager {
    private static final String TAG = "DataBaseManager";
    private SQLiteDatabase mDatabase;
    private DataBaseHelper mHelper;
    private Object mLock = new Object();
    private WifiManager mWifiManager;

    public DataBaseManager(Context context) {
        Log.e(MessageUtil.TAG, "DataBaseManager()");
        this.mHelper = new DataBaseHelper(context);
        try {
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            Log.e(MessageUtil.TAG, "DataBaseManager(), can't open database!");
        }
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    /* JADX WARNING: Missing block: B:14:0x006d, code skipped:
            if (r3 != null) goto L_0x006f;
     */
    /* JADX WARNING: Missing block: B:16:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:21:0x008c, code skipped:
            if (r3 == null) goto L_0x008f;
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r2 = com.android.server.wifi.wifipro.hwintelligencewifi.MessageUtil.TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("getAllApInfos infos.size()=");
            r4.append(r1.size());
            android.util.Log.e(r2, r4.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x00ad, code skipped:
            if (r1.size() <= 0) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:25:0x00af, code skipped:
            r2 = r1.iterator();
     */
    /* JADX WARNING: Missing block: B:27:0x00b7, code skipped:
            if (r2.hasNext() == false) goto L_0x00e2;
     */
    /* JADX WARNING: Missing block: B:28:0x00b9, code skipped:
            r4 = (com.android.server.wifi.wifipro.hwintelligencewifi.APInfoData) r2.next();
            r5 = queryCellInfoByBssid(r4.getBssid());
     */
    /* JADX WARNING: Missing block: B:29:0x00cb, code skipped:
            if (r5.size() == 0) goto L_0x00d0;
     */
    /* JADX WARNING: Missing block: B:30:0x00cd, code skipped:
            r4.setCellInfo(r5);
     */
    /* JADX WARNING: Missing block: B:31:0x00d0, code skipped:
            r6 = getNearbyApInfo(r4.getBssid());
     */
    /* JADX WARNING: Missing block: B:32:0x00dc, code skipped:
            if (r6.size() == 0) goto L_0x00e1;
     */
    /* JADX WARNING: Missing block: B:33:0x00de, code skipped:
            r4.setNearbyAPInfos(r6);
     */
    /* JADX WARNING: Missing block: B:36:0x00e3, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:41:0x00eb, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<APInfoData> getAllApInfos() {
        synchronized (this.mLock) {
            ArrayList<APInfoData> infos = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM BSSIDTable", null);
                    while (c.moveToNext()) {
                        infos.add(new APInfoData(c.getString(c.getColumnIndex("bssid")), c.getString(c.getColumnIndex("ssid")), c.getInt(c.getColumnIndex("inbacklist")), c.getInt(c.getColumnIndex(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE)), c.getLong(c.getColumnIndex(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_TIME)), c.getInt(c.getColumnIndex("isHome"))));
                    }
                } catch (Exception e) {
                    try {
                        String str = MessageUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getAllApInfos:");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addApInfos(String bssid, String ssid, String cellid, int authtype) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    inlineAddBssidIdInfo(bssid, ssid, authtype);
                    inlineAddCellInfo(bssid, cellid);
                    inlineAddNearbyApInfo(bssid);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void delAPInfos(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    delBssidInfo(bssid);
                    delCellidInfoByBssid(bssid);
                    delNearbyApInfo(bssid);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    Log.e(MessageUtil.TAG, "closeDB()");
                    this.mDatabase.close();
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0016, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCellInfo(String bssid, String cellid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    inlineAddCellInfo(bssid, cellid);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x003d, code skipped:
            if (r2 != null) goto L_0x003f;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:24:0x005c, code skipped:
            if (r2 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:27:0x0060, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:32:0x0068, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getNearbyApInfo(String bssid) {
        synchronized (this.mLock) {
            List<String> datas = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen() || bssid == null) {
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM APInfoTable where bssid like ?", new String[]{bssid});
                    while (c.moveToNext()) {
                        String nearbyBssid = c.getString(c.getColumnIndex("nearbybssid"));
                        if (nearbyBssid != null) {
                            datas.add(nearbyBssid);
                        }
                    }
                } catch (Exception e) {
                    try {
                        String str = MessageUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getNearbyApInfo Exception:");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0016, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addNearbyApInfo(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    inlineAddNearbyApInfo(bssid);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0055, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBssidTimer(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (bssid != null) {
                    long time = System.currentTimeMillis();
                    String str = MessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateBssidTimer time = ");
                    stringBuilder.append(time);
                    Log.w(str, stringBuilder.toString());
                    ContentValues values = new ContentValues();
                    values.put(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_TIME, Long.valueOf(time));
                    try {
                        this.mDatabase.update(DataBaseHelper.BSSID_TABLE_NAME, values, "bssid like ?", new String[]{bssid});
                    } catch (SQLiteException e) {
                        Log.w(MessageUtil.TAG, "updateBssidTimer, update, SQLiteException");
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x003b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBssidIsInBlackList(String bssid, int inblacklist) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (bssid != null) {
                    ContentValues values = new ContentValues();
                    values.put("inbacklist", Integer.valueOf(inblacklist));
                    try {
                        this.mDatabase.update(DataBaseHelper.BSSID_TABLE_NAME, values, "bssid like ?", new String[]{bssid});
                    } catch (SQLiteException e) {
                        Log.w(MessageUtil.TAG, "updateBssidIsInBlackList, update, SQLiteException");
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x003b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBssidIsHome(String bssid, int isHome) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (bssid != null) {
                    ContentValues values = new ContentValues();
                    values.put("isHome", Integer.valueOf(isHome));
                    try {
                        this.mDatabase.update(DataBaseHelper.BSSID_TABLE_NAME, values, "bssid like ?", new String[]{bssid});
                    } catch (SQLiteException e) {
                        Log.w(MessageUtil.TAG, "updateBssidIsHome, update, SQLiteException");
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void delNearbyApInfo(String bssid) {
        synchronized (this.mLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || bssid == null) {
            } else {
                try {
                    this.mDatabase.delete(DataBaseHelper.APINFO_TABLE_NAME, "bssid like ?", new String[]{bssid});
                } catch (SQLiteException e) {
                    Log.w(MessageUtil.TAG, "delNearbyApInfo, delete, SQLiteException");
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0039, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateSsid(String bssid, String ssid) {
        synchronized (this.mLock) {
            if (!(this.mDatabase == null || !this.mDatabase.isOpen() || bssid == null)) {
                if (ssid != null) {
                    ContentValues values = new ContentValues();
                    values.put("ssid", ssid);
                    try {
                        this.mDatabase.update(DataBaseHelper.BSSID_TABLE_NAME, values, "bssid like ?", new String[]{bssid});
                    } catch (SQLiteException e) {
                        Log.w(MessageUtil.TAG, "updateSsid, update, SQLiteException");
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x003b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateAuthType(String bssid, int authtype) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (bssid != null) {
                    ContentValues values = new ContentValues();
                    values.put(HwDualBandMessageUtil.MSG_KEY_AUTHTYPE, Integer.valueOf(authtype));
                    try {
                        this.mDatabase.update(DataBaseHelper.BSSID_TABLE_NAME, values, "bssid like ?", new String[]{bssid});
                    } catch (SQLiteException e) {
                        Log.w(MessageUtil.TAG, "updateAuthType, update, SQLiteException");
                    }
                }
            }
        }
    }

    private void inlineAddBssidIdInfo(String bssid, String ssid, int authtype) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null && ssid != null) {
            long time = System.currentTimeMillis();
            try {
                this.mDatabase.execSQL("INSERT INTO BSSIDTable VALUES(null, ?,?,?,?,?,?)", new Object[]{bssid, ssid, Integer.valueOf(0), Integer.valueOf(authtype), Long.valueOf(time), Integer.valueOf(0)});
            } catch (SQLiteException e) {
                Log.w(MessageUtil.TAG, "inlineAddBssidIdInfo, execSQL, SQLiteException");
            }
        }
    }

    private void delBssidInfo(String bssid) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null) {
            try {
                this.mDatabase.delete(DataBaseHelper.BSSID_TABLE_NAME, "bssid like ?", new String[]{bssid});
            } catch (SQLiteException e) {
                Log.w(MessageUtil.TAG, "delBssidInfo, delete, SQLiteException");
            }
        }
    }

    private void inlineAddCellInfo(String bssid, String cellid) {
        int rssi = CellStateMonitor.getCellRssi();
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null && cellid != null) {
            try {
                this.mDatabase.execSQL("INSERT INTO CELLIDTable VALUES(null, ?, ?, ?)", new Object[]{bssid, cellid, Integer.valueOf(rssi)});
            } catch (SQLiteException e) {
                Log.w(MessageUtil.TAG, "inlineAddCellInfo, execSQL, SQLiteException");
            }
        }
    }

    private void delCellidInfoByBssid(String bssid) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null) {
            try {
                this.mDatabase.delete(DataBaseHelper.CELLID_TABLE_NAME, "bssid like ?", new String[]{bssid});
            } catch (SQLiteException e) {
                Log.w(MessageUtil.TAG, "delCellidInfoByBssid, delete, SQLiteException");
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0043, code skipped:
            if (r2 != null) goto L_0x0045;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:24:0x0062, code skipped:
            if (r2 == null) goto L_0x0065;
     */
    /* JADX WARNING: Missing block: B:27:0x0066, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<CellInfoData> queryCellInfoByBssid(String bssid) {
        synchronized (this.mLock) {
            List<CellInfoData> datas = new ArrayList();
            if (bssid == null) {
                return datas;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM CELLIDTable where bssid like ?", new String[]{bssid});
                while (c.moveToNext()) {
                    String cellid = c.getString(c.getColumnIndex(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_CELLID));
                    int rssi = c.getInt(c.getColumnIndex(HwDualBandMessageUtil.MSG_KEY_RSSI));
                    if (!(cellid == null || rssi == 0)) {
                        datas.add(new CellInfoData(cellid, rssi));
                    }
                }
            } catch (Exception e) {
                try {
                    String str = MessageUtil.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("queryCellInfoByBssid:");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    private void inlineAddNearbyApInfo(String bssid) {
        int num = 0;
        List<ScanResult> lists = WifiproUtils.getScanResultsFromWsm();
        if (lists != null) {
            String str = MessageUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addNearbyApInfo lists.size = ");
            stringBuilder.append(lists.size());
            Log.w(str, stringBuilder.toString());
            for (ScanResult result : lists) {
                if (num < 20) {
                    addNearbyApInfo(bssid, result.BSSID);
                    num++;
                }
            }
        }
    }

    private void addNearbyApInfo(String bssid, String nearbyBssid) {
        if (this.mDatabase != null && this.mDatabase.isOpen() && bssid != null && nearbyBssid != null) {
            try {
                this.mDatabase.execSQL("INSERT INTO APInfoTable VALUES(null, ?, ?)", new Object[]{bssid, nearbyBssid});
            } catch (SQLiteException e) {
                Log.w(MessageUtil.TAG, "addNearbyApInfo, execSQL, SQLiteException");
            }
        }
    }
}

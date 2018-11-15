package com.android.server.wifi.wifipro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class WifiProHistoryDBManager {
    private static long AGING_MS_OF_EACH_DAY = 1800000;
    private static final int DBG_LOG_LEVEL = 1;
    private static final int ERROR_LOG_LEVEL = 3;
    private static final int INFO_LOG_LEVEL = 2;
    private static final short MAX_AP_INFO_RECORD_NUM = (short) 1;
    private static long MS_OF_ONE_DAY = 86400000;
    private static final String TAG = "WifiProHistoryDBManager";
    private static long TOO_OLD_VALID_DAY = 10;
    private static WifiProHistoryDBManager mBQEDataBaseManager;
    private static int printLogLevel = 1;
    private int mApRecordCount = 0;
    private Object mBqeLock = new Object();
    private SQLiteDatabase mDatabase;
    private WifiProHistoryDBHelper mHelper;
    private int mHomeApRecordCount = 0;
    private boolean mNeedDelOldDualBandApInfo;

    public WifiProHistoryDBManager(Context context) {
        Log.w(TAG, "WifiProHistoryDBManager()");
        this.mHelper = new WifiProHistoryDBHelper(context);
        try {
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            loge("WifiProHistoryDBManager(), can't open database!");
        }
    }

    public static WifiProHistoryDBManager getInstance(Context context) {
        if (mBQEDataBaseManager == null) {
            mBQEDataBaseManager = new WifiProHistoryDBManager(context);
        }
        return mBQEDataBaseManager;
    }

    /* JADX WARNING: Missing block: B:11:0x001f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeDB() {
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            } else {
                Log.w(TAG, "closeDB()");
                this.mDatabase.close();
            }
        }
    }

    private boolean deleteHistoryRecord(String dbTableName, String apBssid) {
        logi("deleteHistoryRecord enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("deleteHistoryRecord database error.");
                return false;
            } else if (apBssid == null) {
                loge("deleteHistoryRecord null error.");
                return false;
            } else {
                try {
                    this.mDatabase.delete(dbTableName, "apBSSID like ?", new String[]{apBssid});
                    return true;
                } catch (SQLException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deleteHistoryRecord error:");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                    return false;
                }
            }
        }
    }

    public boolean deleteApInfoRecord(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, apBssid);
    }

    private boolean checkHistoryRecordExist(String dbTableName, String apBssid) {
        boolean ret = false;
        Cursor c = null;
        StringBuilder stringBuilder;
        try {
            SQLiteDatabase sQLiteDatabase = this.mDatabase;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SELECT * FROM ");
            stringBuilder.append(dbTableName);
            stringBuilder.append(" where apBSSID like ?");
            c = sQLiteDatabase.rawQuery(stringBuilder.toString(), new String[]{apBssid});
            int rcdCount = c.getCount();
            if (rcdCount > 0) {
                ret = true;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkHistoryRecordExist read from:");
            stringBuilder.append(dbTableName);
            stringBuilder.append(", get record: ");
            stringBuilder.append(rcdCount);
            logd(stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return ret;
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkHistoryRecordExist error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private boolean updateApInfoRecord(WifiProApInfoRecord dbr) {
        ContentValues values = new ContentValues();
        values.put("apSSID", dbr.apSSID);
        values.put("apSecurityType", Integer.valueOf(dbr.apSecurityType));
        values.put("firstConnectTime", Long.valueOf(dbr.firstConnectTime));
        values.put("lastConnectTime", Long.valueOf(dbr.lastConnectTime));
        values.put("lanDataSize", Integer.valueOf(dbr.lanDataSize));
        values.put("highSpdFreq", Integer.valueOf(dbr.highSpdFreq));
        values.put("totalUseTime", Integer.valueOf(dbr.totalUseTime));
        values.put("totalUseTimeAtNight", Integer.valueOf(dbr.totalUseTimeAtNight));
        values.put("totalUseTimeAtWeekend", Integer.valueOf(dbr.totalUseTimeAtWeekend));
        values.put("judgeHomeAPTime", Long.valueOf(dbr.judgeHomeAPTime));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{dbr.apBSSID});
            if (rowChg == 0) {
                loge("updateApInfoRecord update failed.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateApInfoRecord update succ, rowChg=");
            stringBuilder.append(rowChg);
            logd(stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateApInfoRecord error:");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertApInfoRecord(WifiProApInfoRecord dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProApInfoRecodTable VALUES(null,  ?, ?, ?, ?, ?,    ?, ?, ?, ?, ?,?)", new Object[]{dbr.apBSSID, dbr.apSSID, Integer.valueOf(dbr.apSecurityType), Long.valueOf(dbr.firstConnectTime), Long.valueOf(dbr.lastConnectTime), Integer.valueOf(dbr.lanDataSize), Integer.valueOf(dbr.highSpdFreq), Integer.valueOf(dbr.totalUseTime), Integer.valueOf(dbr.totalUseTimeAtNight), Integer.valueOf(dbr.totalUseTimeAtWeekend), Long.valueOf(dbr.judgeHomeAPTime)});
            logi("insertApInfoRecord add a record succ.");
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertApInfoRecord error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public boolean addOrUpdateApInfoRecord(WifiProApInfoRecord dbr) {
        logd("addOrUpdateApInfoRecord enter.");
        synchronized (this.mBqeLock) {
            boolean updateApInfoRecord;
            if (this.mDatabase == null || !this.mDatabase.isOpen() || dbr == null) {
                loge("addOrUpdateApInfoRecord error.");
                return false;
            } else if (dbr.apBSSID == null) {
                loge("addOrUpdateApInfoRecord null error.");
                return false;
            } else if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_AP_INFO_TB_NAME, dbr.apBSSID)) {
                updateApInfoRecord = updateApInfoRecord(dbr);
                return updateApInfoRecord;
            } else {
                updateApInfoRecord = insertApInfoRecord(dbr);
                return updateApInfoRecord;
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x0100, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean queryApInfoRecord(String apBssid, WifiProApInfoRecord dbr) {
        int recCnt = 0;
        logd("queryApInfoRecord enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryApInfoRecord database error.");
                return false;
            } else if (apBssid == null || dbr == null) {
                loge("queryApInfoRecord null error.");
                return false;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable where apBSSID like ?", new String[]{apBssid});
                    while (c.moveToNext()) {
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("read record id = ");
                        stringBuilder.append(c.getInt(c.getColumnIndex("_id")));
                        logd(stringBuilder.toString());
                        if (recCnt == 1) {
                            dbr.apBSSID = apBssid;
                            dbr.apSSID = c.getString(c.getColumnIndex("apSSID"));
                            dbr.apSecurityType = c.getInt(c.getColumnIndex("apSecurityType"));
                            dbr.firstConnectTime = c.getLong(c.getColumnIndex("firstConnectTime"));
                            dbr.lastConnectTime = c.getLong(c.getColumnIndex("lastConnectTime"));
                            dbr.lanDataSize = c.getInt(c.getColumnIndex("lanDataSize"));
                            dbr.highSpdFreq = c.getInt(c.getColumnIndex("highSpdFreq"));
                            dbr.totalUseTime = c.getInt(c.getColumnIndex("totalUseTime"));
                            dbr.totalUseTimeAtNight = c.getInt(c.getColumnIndex("totalUseTimeAtNight"));
                            dbr.totalUseTimeAtWeekend = c.getInt(c.getColumnIndex("totalUseTimeAtWeekend"));
                            dbr.judgeHomeAPTime = c.getLong(c.getColumnIndex("judgeHomeAPTime"));
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("read record succ, LastConnectTime:");
                            stringBuilder.append(dbr.lastConnectTime);
                            logi(stringBuilder.toString());
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (recCnt > 1) {
                        loge("more than one record error. use first record.");
                    } else if (recCnt == 0) {
                        logi("queryApInfoRecord not record.");
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("queryApInfoRecord error:");
                        stringBuilder2.append(e);
                        loge(stringBuilder2.toString());
                        if (c != null) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0060, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int querySameSSIDApCount(String apBSSID, String apSsid, int secType) {
        int recCnt = 0;
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("querySameSSIDApCount database error.");
                return 0;
            } else if (apBSSID == null || apSsid == null) {
                loge("querySameSSIDApCount null error.");
                return 0;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable where (apSSID like ?) and (apSecurityType = ?) and (apBSSID != ?)", new String[]{apSsid, String.valueOf(secType), apBSSID});
                    recCnt = c.getCount();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("querySameSSIDApCount read same (SSID:");
                    stringBuilder.append(apSsid);
                    stringBuilder.append(", secType:");
                    stringBuilder.append(secType);
                    stringBuilder.append(") and different BSSID record count=");
                    stringBuilder.append(recCnt);
                    logi(stringBuilder.toString());
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("querySameSSIDApCount error:");
                        stringBuilder2.append(e);
                        loge(stringBuilder2.toString());
                        if (c != null) {
                            c.close();
                        }
                        return recCnt;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.wifi.wifipro.WifiProHistoryDBManager.removeTooOldApInfoRecord():boolean, dom blocks: [B:58:0x0165, B:68:0x0178]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x018f A:{SYNTHETIC, Splitter: B:71:0x018f} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x018f A:{SYNTHETIC, Splitter: B:71:0x018f} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0198 A:{Catch:{ all -> 0x0195, all -> 0x01af }} */
    public boolean removeTooOldApInfoRecord() {
        /*
        r25 = this;
        r1 = r25;
        r2 = 0;
        r0 = new java.util.Vector;
        r0.<init>();
        r3 = r0;
        r0 = new java.util.Date;
        r0.<init>();
        r4 = r0;
        r5 = r4.getTime();
        r7 = 0;
        r9 = 0;
        r0 = "removeTooOldApInfoRecord enter.";
        r1.logd(r0);
        r11 = r1.mBqeLock;
        monitor-enter(r11);
        r0 = r1.mDatabase;	 Catch:{ all -> 0x01a8 }
        if (r0 == 0) goto L_0x019c;	 Catch:{ all -> 0x01a8 }
    L_0x0023:
        r0 = r1.mDatabase;	 Catch:{ all -> 0x01a8 }
        r0 = r0.isOpen();	 Catch:{ all -> 0x01a8 }
        if (r0 != 0) goto L_0x0031;	 Catch:{ all -> 0x01a8 }
    L_0x002b:
        r20 = r2;	 Catch:{ all -> 0x01a8 }
        r21 = r4;	 Catch:{ all -> 0x01a8 }
        goto L_0x01a0;	 Catch:{ all -> 0x01a8 }
    L_0x0031:
        r3.clear();	 Catch:{ all -> 0x01a8 }
        r0 = 0;
        r13 = r0;
        r14 = r1.mDatabase;	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r15 = "SELECT * FROM WifiProApInfoRecodTable";	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = r14.rawQuery(r15, r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r13 = r0;	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0.<init>();	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r14 = "all record count=";	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0.append(r14);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r14 = r13.getCount();	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0.append(r14);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = r0.toString();	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r1.logd(r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
    L_0x0057:
        r0 = r13.moveToNext();	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        if (r0 == 0) goto L_0x011f;	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
    L_0x005d:
        r0 = "lastConnectTime";	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = r13.getColumnIndex(r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r14 = r13.getLong(r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r7 = r14;	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = "totalUseTime";	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = r13.getColumnIndex(r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r0 = r13.getInt(r0);	 Catch:{ SQLException -> 0x0173, all -> 0x016d }
        r9 = (long) r0;
        r14 = r5 - r7;
        r16 = MS_OF_ONE_DAY;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r14 = r14 / r16;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r16 = TOO_OLD_VALID_DAY;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r0 = (r14 > r16 ? 1 : (r14 == r16 ? 0 : -1));	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        if (r0 <= 0) goto L_0x0100;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
    L_0x007f:
        r16 = AGING_MS_OF_EACH_DAY;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r16 = r16 * r14;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r16 = r9 - r16;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r18 = 0;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r0 = (r16 > r18 ? 1 : (r16 == r18 ? 0 : -1));	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        if (r0 >= 0) goto L_0x0100;	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
    L_0x008b:
        r0 = "check result: need delete.";	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r1.logi(r0);	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r0 = "_id";	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r0 = r13.getColumnIndex(r0);	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r0 = r13.getInt(r0);	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r12 = "apSSID";	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r12 = r13.getColumnIndex(r12);	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r12 = r13.getString(r12);	 Catch:{ SQLException -> 0x0117, all -> 0x010e }
        r20 = r2;
        r2 = "apBSSID";	 Catch:{ SQLException -> 0x00f9, all -> 0x00f2 }
        r2 = r13.getColumnIndex(r2);	 Catch:{ SQLException -> 0x00f9, all -> 0x00f2 }
        r2 = r13.getString(r2);	 Catch:{ SQLException -> 0x00f9, all -> 0x00f2 }
        r21 = r4;
        r4 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x00ed, all -> 0x00e8 }
        r4.<init>();	 Catch:{ SQLException -> 0x00ed, all -> 0x00e8 }
        r22 = r9;
        r9 = "check record: ssid:";	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r9);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r12);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r9 = ", id:";	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r9);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r0);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r9 = ", pass time:";	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r9);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r9 = r5 - r7;	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4.append(r9);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r4 = r4.toString();	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r1.logi(r4);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        r3.add(r2);	 Catch:{ SQLException -> 0x00e3, all -> 0x00de }
        goto L_0x0106;
    L_0x00de:
        r0 = move-exception;
        r9 = r22;
        goto L_0x0196;
    L_0x00e3:
        r0 = move-exception;
        r9 = r22;
        goto L_0x0178;
    L_0x00e8:
        r0 = move-exception;
        r22 = r9;
        goto L_0x0196;
    L_0x00ed:
        r0 = move-exception;
        r22 = r9;
        goto L_0x0178;
    L_0x00f2:
        r0 = move-exception;
        r21 = r4;
        r22 = r9;
        goto L_0x0196;
    L_0x00f9:
        r0 = move-exception;
        r21 = r4;
        r22 = r9;
        goto L_0x0178;
    L_0x0100:
        r20 = r2;
        r21 = r4;
        r22 = r9;
    L_0x0106:
        r2 = r20;
        r4 = r21;
        r9 = r22;
        goto L_0x0057;
    L_0x010e:
        r0 = move-exception;
        r20 = r2;
        r21 = r4;
        r22 = r9;
        goto L_0x0196;
    L_0x0117:
        r0 = move-exception;
        r20 = r2;
        r21 = r4;
        r22 = r9;
        goto L_0x0178;
    L_0x011f:
        r20 = r2;
        r21 = r4;
        r0 = r3.size();	 Catch:{ SQLException -> 0x016b }
        r2 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x016b }
        r2.<init>();	 Catch:{ SQLException -> 0x016b }
        r4 = "start delete ";	 Catch:{ SQLException -> 0x016b }
        r2.append(r4);	 Catch:{ SQLException -> 0x016b }
        r2.append(r0);	 Catch:{ SQLException -> 0x016b }
        r4 = " records.";	 Catch:{ SQLException -> 0x016b }
        r2.append(r4);	 Catch:{ SQLException -> 0x016b }
        r2 = r2.toString();	 Catch:{ SQLException -> 0x016b }
        r1.logi(r2);	 Catch:{ SQLException -> 0x016b }
        r2 = 0;	 Catch:{ SQLException -> 0x016b }
    L_0x0141:
        if (r2 >= r0) goto L_0x0163;	 Catch:{ SQLException -> 0x016b }
    L_0x0143:
        r12 = r3.get(r2);	 Catch:{ SQLException -> 0x016b }
        r12 = (java.lang.String) r12;	 Catch:{ SQLException -> 0x016b }
        if (r12 != 0) goto L_0x014c;	 Catch:{ SQLException -> 0x016b }
    L_0x014b:
        goto L_0x0163;	 Catch:{ SQLException -> 0x016b }
    L_0x014c:
        r14 = r1.mDatabase;	 Catch:{ SQLException -> 0x016b }
        r15 = "WifiProApInfoRecodTable";	 Catch:{ SQLException -> 0x016b }
        r4 = "apBSSID like ?";	 Catch:{ SQLException -> 0x016b }
        r24 = r0;	 Catch:{ SQLException -> 0x016b }
        r0 = 1;	 Catch:{ SQLException -> 0x016b }
        r0 = new java.lang.String[r0];	 Catch:{ SQLException -> 0x016b }
        r16 = 0;	 Catch:{ SQLException -> 0x016b }
        r0[r16] = r12;	 Catch:{ SQLException -> 0x016b }
        r14.delete(r15, r4, r0);	 Catch:{ SQLException -> 0x016b }
        r2 = r2 + 1;
        r0 = r24;
        goto L_0x0141;
    L_0x0163:
        if (r13 == 0) goto L_0x0168;
    L_0x0165:
        r13.close();	 Catch:{ all -> 0x01af }
    L_0x0168:
        monitor-exit(r11);	 Catch:{ all -> 0x01af }
        r0 = 1;
        return r0;
    L_0x016b:
        r0 = move-exception;
        goto L_0x0178;
    L_0x016d:
        r0 = move-exception;
        r20 = r2;
        r21 = r4;
        goto L_0x0196;
    L_0x0173:
        r0 = move-exception;
        r20 = r2;
        r21 = r4;
    L_0x0178:
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0195 }
        r2.<init>();	 Catch:{ all -> 0x0195 }
        r4 = "removeTooOldApInfoRecord error:";	 Catch:{ all -> 0x0195 }
        r2.append(r4);	 Catch:{ all -> 0x0195 }
        r2.append(r0);	 Catch:{ all -> 0x0195 }
        r2 = r2.toString();	 Catch:{ all -> 0x0195 }
        r1.loge(r2);	 Catch:{ all -> 0x0195 }
        if (r13 == 0) goto L_0x0192;
    L_0x018f:
        r13.close();	 Catch:{ all -> 0x01af }
    L_0x0192:
        monitor-exit(r11);	 Catch:{ all -> 0x01af }
        r2 = 0;	 Catch:{ all -> 0x01af }
        return r2;	 Catch:{ all -> 0x01af }
    L_0x0195:
        r0 = move-exception;	 Catch:{ all -> 0x01af }
    L_0x0196:
        if (r13 == 0) goto L_0x019b;	 Catch:{ all -> 0x01af }
    L_0x0198:
        r13.close();	 Catch:{ all -> 0x01af }
    L_0x019b:
        throw r0;	 Catch:{ all -> 0x01af }
    L_0x019c:
        r20 = r2;	 Catch:{ all -> 0x01af }
        r21 = r4;	 Catch:{ all -> 0x01af }
    L_0x01a0:
        r0 = "removeTooOldApInfoRecord database error.";	 Catch:{ all -> 0x01af }
        r1.loge(r0);	 Catch:{ all -> 0x01af }
        monitor-exit(r11);	 Catch:{ all -> 0x01af }
        r2 = 0;	 Catch:{ all -> 0x01af }
        return r2;	 Catch:{ all -> 0x01af }
    L_0x01a8:
        r0 = move-exception;	 Catch:{ all -> 0x01af }
        r20 = r2;	 Catch:{ all -> 0x01af }
        r21 = r4;	 Catch:{ all -> 0x01af }
    L_0x01ad:
        monitor-exit(r11);	 Catch:{ all -> 0x01af }
        throw r0;
    L_0x01af:
        r0 = move-exception;
        goto L_0x01ad;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wifi.wifipro.WifiProHistoryDBManager.removeTooOldApInfoRecord():boolean");
    }

    /* JADX WARNING: Missing block: B:21:0x008d, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean statisticApInfoRecord() {
        logd("statisticApInfoRecord enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("statisticApInfoRecord database error.");
                return false;
            }
            this.mHomeApRecordCount = 0;
            this.mApRecordCount = 0;
            Cursor c = null;
            StringBuilder stringBuilder;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProApInfoRecodTable", null);
                this.mApRecordCount = c.getCount();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("all record count=");
                stringBuilder2.append(this.mApRecordCount);
                logi(stringBuilder2.toString());
                while (c.moveToNext()) {
                    if (c.getLong(c.getColumnIndex("judgeHomeAPTime")) > 0) {
                        String ssid = c.getString(c.getColumnIndex("apSSID"));
                        this.mHomeApRecordCount++;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("check record: Home ap ssid:");
                        stringBuilder.append(ssid);
                        stringBuilder.append(", total:");
                        stringBuilder.append(this.mHomeApRecordCount);
                        logi(stringBuilder.toString());
                    }
                }
                if (c != null) {
                    c.close();
                }
            } catch (SQLException e) {
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("removeTooOldApInfoRecord error:");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                    if (c != null) {
                        c.close();
                    }
                    return false;
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    public int getTotRecordCount() {
        int i;
        synchronized (this.mBqeLock) {
            i = this.mApRecordCount;
        }
        return i;
    }

    public int getHomeApRecordCount() {
        int i;
        synchronized (this.mBqeLock) {
            i = this.mHomeApRecordCount;
        }
        return i;
    }

    private boolean insertEnterpriseApRecord(String apSSID, int secType) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProEnterpriseAPTable VALUES(null,  ?, ?)", new Object[]{apSSID, Integer.valueOf(secType)});
            logi("insertEnterpriseApRecord add a record succ.");
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertEnterpriseApRecord error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public boolean addOrUpdateEnterpriseApRecord(String apSSID, int secType) {
        logd("addOrUpdateEnterpriseApRecord enter.");
        synchronized (this.mBqeLock) {
            StringBuilder stringBuilder;
            if (this.mDatabase == null || !this.mDatabase.isOpen() || apSSID == null) {
                loge("addOrUpdateEnterpriseApRecord error.");
                return false;
            } else if (queryEnterpriseApRecord(apSSID, secType)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("already exist the record ssid:");
                stringBuilder.append(apSSID);
                logi(stringBuilder.toString());
                return true;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("add record here ssid:");
                stringBuilder.append(apSSID);
                logi(stringBuilder.toString());
                boolean insertEnterpriseApRecord = insertEnterpriseApRecord(apSSID, secType);
                return insertEnterpriseApRecord;
            }
        }
    }

    public boolean queryEnterpriseApRecord(String apSSID, int secType) {
        logd("queryEnterpriseApRecord enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryEnterpriseApRecord database error.");
                return false;
            } else if (apSSID == null) {
                loge("queryEnterpriseApRecord null error.");
                return false;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProEnterpriseAPTable where (apSSID like ?) and (apSecurityType = ?)", new String[]{apSSID, String.valueOf(secType)});
                    int recCnt = c.getCount();
                    if (c != null) {
                        c.close();
                    }
                    if (recCnt > 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SSID:");
                        stringBuilder.append(apSSID);
                        stringBuilder.append(", security: ");
                        stringBuilder.append(secType);
                        stringBuilder.append(" is in Enterprise Ap table. count:");
                        stringBuilder.append(recCnt);
                        logi(stringBuilder.toString());
                        return true;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SSID:");
                    stringBuilder2.append(apSSID);
                    stringBuilder2.append(", security: ");
                    stringBuilder2.append(secType);
                    stringBuilder2.append(" is not in Enterprise Ap table. count:");
                    stringBuilder2.append(recCnt);
                    logi(stringBuilder2.toString());
                    return false;
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("queryEnterpriseApRecord error:");
                        stringBuilder3.append(e);
                        loge(stringBuilder3.toString());
                        if (c != null) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    public boolean deleteEnterpriseApRecord(String tableName, String ssid, int secType) {
        if (tableName == null || ssid == null) {
            loge("deleteHistoryRecord null error.");
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delete record of same (SSID:");
        stringBuilder.append(ssid);
        stringBuilder.append(", secType:");
        stringBuilder.append(secType);
        stringBuilder.append(") from ");
        stringBuilder.append(tableName);
        logi(stringBuilder.toString());
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("deleteHistoryRecord database error.");
                return false;
            }
            StringBuilder stringBuilder2;
            try {
                int delCount = this.mDatabase.delete(tableName, "(apSSID like ?) and (apSecurityType = ?)", new String[]{ssid, String.valueOf(secType)});
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("delete record count=");
                stringBuilder2.append(delCount);
                logd(stringBuilder2.toString());
                return true;
            } catch (SQLException e) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("deleteHistoryRecord error:");
                stringBuilder2.append(e);
                loge(stringBuilder2.toString());
                return false;
            }
        }
    }

    public boolean deleteRelateApRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, apBssid);
    }

    public boolean deleteApQualityRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, apBssid);
    }

    public boolean deleteDualBandApInfoRcd(String apBssid) {
        return deleteHistoryRecord(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, apBssid);
    }

    public boolean deleteRelateApRcd(String apBssid, String relatedBSSID) {
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("deleteRelateApRcd database error.");
                return false;
            } else if (apBssid == null || relatedBSSID == null) {
                loge("deleteRelateApRcd null error.");
                return false;
            } else {
                try {
                    this.mDatabase.delete(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, "(apBSSID like ?) and (RelatedBSSID like ?)", new String[]{apBssid, relatedBSSID});
                    return true;
                } catch (SQLException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deleteRelateApRcd error:");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                    return false;
                }
            }
        }
    }

    public boolean deleteRelate5GAPRcd(String relatedBSSID) {
        logi("deleteRelateApRcd enter");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("deleteRelateApRcd database error.");
                return false;
            } else if (relatedBSSID == null) {
                loge("deleteRelateApRcd null error.");
                return false;
            } else {
                try {
                    this.mDatabase.delete(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, "(RelatedBSSID like ?)", new String[]{relatedBSSID});
                    return true;
                } catch (SQLException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deleteRelateApRcd error:");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                    return false;
                }
            }
        }
    }

    private boolean deleteAllDualBandAPRcd(String apBssid) {
        return deleteDualBandApInfoRcd(apBssid) && deleteApQualityRcd(apBssid) && deleteRelateApRcd(apBssid) && deleteRelate5GAPRcd(apBssid);
    }

    private boolean updateApQualityRcd(WifiProApQualityRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("RTT_Product", dbr.mRTT_Product);
        values.put("RTT_PacketVolume", dbr.mRTT_PacketVolume);
        values.put("HistoryAvgRtt", dbr.mHistoryAvgRtt);
        values.put("OTA_LostRateValue", dbr.mOTA_LostRateValue);
        values.put("OTA_PktVolume", dbr.mOTA_PktVolume);
        values.put("OTA_BadPktProduct", dbr.mOTA_BadPktProduct);
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, values, "apBSSID like ?", new String[]{dbr.apBSSID});
            if (rowChg == 0) {
                loge("updateApQualityRcd update failed.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateApQualityRcd update succ, rowChg=");
            stringBuilder.append(rowChg);
            logd(stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateApQualityRcd error:");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertApQualityRcd(WifiProApQualityRcd dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProApQualityTable VALUES(null,  ?, ?, ?, ?, ?,   ?, ?)", new Object[]{dbr.apBSSID, dbr.mRTT_Product, dbr.mRTT_PacketVolume, dbr.mHistoryAvgRtt, dbr.mOTA_LostRateValue, dbr.mOTA_PktVolume, dbr.mOTA_BadPktProduct});
            logi("insertApQualityRcd add a record succ.");
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertApQualityRcd error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public boolean addOrUpdateApQualityRcd(WifiProApQualityRcd dbr) {
        logd("addOrUpdateApQualityRcd enter.");
        synchronized (this.mBqeLock) {
            boolean updateApQualityRcd;
            if (this.mDatabase == null || !this.mDatabase.isOpen() || dbr == null) {
                loge("addOrUpdateApQualityRcd error.");
                return false;
            } else if (dbr.apBSSID == null) {
                loge("addOrUpdateApQualityRcd null error.");
                return false;
            } else if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_QUALITY_TB_NAME, dbr.apBSSID)) {
                updateApQualityRcd = updateApQualityRcd(dbr);
                return updateApQualityRcd;
            } else {
                updateApQualityRcd = insertApQualityRcd(dbr);
                return updateApQualityRcd;
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x00a1, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean queryApQualityRcd(String apBssid, WifiProApQualityRcd dbr) {
        int recCnt = 0;
        logd("queryApQualityRcd enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryApQualityRcd database error.");
                return false;
            } else if (apBssid == null || dbr == null) {
                loge("queryApQualityRcd null error.");
                return false;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProApQualityTable where apBSSID like ?", new String[]{apBssid});
                    while (c.moveToNext()) {
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        } else if (recCnt == 1) {
                            dbr.apBSSID = apBssid;
                            dbr.mRTT_Product = c.getBlob(c.getColumnIndex("RTT_Product"));
                            dbr.mRTT_PacketVolume = c.getBlob(c.getColumnIndex("RTT_PacketVolume"));
                            dbr.mHistoryAvgRtt = c.getBlob(c.getColumnIndex("HistoryAvgRtt"));
                            dbr.mOTA_LostRateValue = c.getBlob(c.getColumnIndex("OTA_LostRateValue"));
                            dbr.mOTA_PktVolume = c.getBlob(c.getColumnIndex("OTA_PktVolume"));
                            dbr.mOTA_BadPktProduct = c.getBlob(c.getColumnIndex("OTA_BadPktProduct"));
                            logi("read record succ");
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (recCnt > 1) {
                        loge("more than one record error. use first record.");
                    } else if (recCnt == 0) {
                        logi("queryApQualityRcd not record.");
                        return false;
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("queryApQualityRcd error:");
                        stringBuilder.append(e);
                        loge(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    private boolean updateRelateApRcd(WifiProRelateApRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("RelateType", Integer.valueOf(dbr.mRelateType));
        values.put("MaxCurrentRSSI", Integer.valueOf(dbr.mMaxCurrentRSSI));
        values.put("MaxRelatedRSSI", Integer.valueOf(dbr.mMaxRelatedRSSI));
        values.put("MinCurrentRSSI", Integer.valueOf(dbr.mMinCurrentRSSI));
        values.put("MinRelatedRSSI", Integer.valueOf(dbr.mMinRelatedRSSI));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_RELATE_AP_TB_NAME, values, "(apBSSID like ?) and (RelatedBSSID like ?)", new String[]{dbr.apBSSID, dbr.mRelatedBSSID});
            if (rowChg == 0) {
                loge("updateRelateApRcd update failed.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRelateApRcd update succ, rowChg=");
            stringBuilder.append(rowChg);
            logd(stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateRelateApRcd error:");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertRelateApRcd(WifiProRelateApRcd dbr) {
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProRelateApTable VALUES(null,  ?, ?, ?, ?, ?, ?, ?)", new Object[]{dbr.apBSSID, dbr.mRelatedBSSID, Integer.valueOf(dbr.mRelateType), Integer.valueOf(dbr.mMaxCurrentRSSI), Integer.valueOf(dbr.mMaxRelatedRSSI), Integer.valueOf(dbr.mMinCurrentRSSI), Integer.valueOf(dbr.mMinRelatedRSSI)});
            logi("insertRelateApRcd add a record succ.");
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertRelateApRcd error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    private boolean checkRelateApRcdExist(String apBssid, String relatedBSSID) {
        boolean ret = false;
        Cursor c = null;
        StringBuilder stringBuilder;
        try {
            c = this.mDatabase.rawQuery("SELECT * FROM WifiProRelateApTable where (apBSSID like ?) and (RelatedBSSID like ?)", new String[]{apBssid, relatedBSSID});
            int rcdCount = c.getCount();
            if (rcdCount > 0) {
                ret = true;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkRelateApRcdExist get record: ");
            stringBuilder.append(rcdCount);
            logd(stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return ret;
        } catch (SQLException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkRelateApRcdExist error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    public boolean addOrUpdateRelateApRcd(WifiProRelateApRcd dbr) {
        logd("addOrUpdateRelateApRcd enter.");
        synchronized (this.mBqeLock) {
            boolean updateRelateApRcd;
            if (this.mDatabase == null || !this.mDatabase.isOpen() || dbr == null) {
                loge("addOrUpdateRelateApRcd error.");
                return false;
            } else if (dbr.apBSSID == null || dbr.mRelatedBSSID == null) {
                loge("addOrUpdateRelateApRcd null error.");
                return false;
            } else if (checkRelateApRcdExist(dbr.apBSSID, dbr.mRelatedBSSID)) {
                updateRelateApRcd = updateRelateApRcd(dbr);
                return updateRelateApRcd;
            } else {
                updateRelateApRcd = insertRelateApRcd(dbr);
                return updateRelateApRcd;
            }
        }
    }

    public boolean queryRelateApRcd(String apBssid, List<WifiProRelateApRcd> relateApList) {
        int recCnt = 0;
        logd("queryRelateApRcd enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryRelateApRcd database error.");
                return false;
            } else if (apBssid == null || relateApList == null) {
                loge("queryRelateApRcd null error.");
                return false;
            } else {
                relateApList.clear();
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProRelateApTable where apBSSID like ?", new String[]{apBssid});
                    while (c.moveToNext()) {
                        recCnt++;
                        if (recCnt > 20) {
                            break;
                        }
                        WifiProRelateApRcd dbr = new WifiProRelateApRcd(apBssid);
                        dbr.mRelatedBSSID = c.getString(c.getColumnIndex("RelatedBSSID"));
                        dbr.mRelateType = c.getShort(c.getColumnIndex("RelateType"));
                        dbr.mMaxCurrentRSSI = c.getInt(c.getColumnIndex("MaxCurrentRSSI"));
                        dbr.mMaxRelatedRSSI = c.getInt(c.getColumnIndex("MaxRelatedRSSI"));
                        dbr.mMinCurrentRSSI = c.getInt(c.getColumnIndex("MinCurrentRSSI"));
                        dbr.mMinRelatedRSSI = c.getInt(c.getColumnIndex("MinRelatedRSSI"));
                        relateApList.add(dbr);
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (recCnt == 0) {
                        logi("queryRelateApRcd not record.");
                        return false;
                    }
                    return true;
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("queryRelateApRcd error:");
                        stringBuilder.append(e);
                        loge(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    private boolean updateDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        ContentValues values = new ContentValues();
        values.put("apSSID", dbr.mApSSID);
        values.put("InetCapability", dbr.mInetCapability);
        values.put("ServingBand", dbr.mServingBand);
        values.put("ApAuthType", dbr.mApAuthType);
        values.put("ChannelFrequency", Integer.valueOf(dbr.mChannelFrequency));
        values.put("DisappearCount", Integer.valueOf(dbr.mDisappearCount));
        values.put("isInBlackList", Integer.valueOf(dbr.isInBlackList));
        values.put("UpdateTime", Long.valueOf(dbr.mUpdateTime));
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{dbr.apBSSID});
            if (rowChg == 0) {
                loge("updateDualBandApInfoRcd update failed.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDualBandApInfoRcd update succ, rowChg=");
            stringBuilder.append(rowChg);
            logd(stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateDualBandApInfoRcd error:");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    private boolean insertDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        if (this.mNeedDelOldDualBandApInfo || getDualBandApInfoSize() >= 500) {
            if (!this.mNeedDelOldDualBandApInfo) {
                this.mNeedDelOldDualBandApInfo = true;
            }
            if (!deleteOldestDualBandApInfo()) {
                return false;
            }
        }
        try {
            this.mDatabase.execSQL("INSERT INTO WifiProDualBandApInfoRcdTable VALUES(null,  ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{dbr.apBSSID, dbr.mApSSID, dbr.mInetCapability, dbr.mServingBand, dbr.mApAuthType, Integer.valueOf(dbr.mChannelFrequency), Integer.valueOf(dbr.mDisappearCount), Integer.valueOf(dbr.isInBlackList), Long.valueOf(dbr.mUpdateTime)});
            logi("insertDualBandApInfoRcd add a record succ.");
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insertDualBandApInfoRcd error:");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public boolean addOrUpdateDualBandApInfoRcd(WifiProDualBandApInfoRcd dbr) {
        logd("addOrUpdateDualBandApInfoRcd enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || dbr == null) {
                loge("addOrUpdateDualBandApInfoRcd error.");
                return false;
            } else if (dbr.apBSSID == null) {
                loge("addOrUpdateDualBandApInfoRcd null error.");
                return false;
            } else {
                dbr.mUpdateTime = System.currentTimeMillis();
                boolean updateDualBandApInfoRcd;
                if (checkHistoryRecordExist(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, dbr.apBSSID)) {
                    updateDualBandApInfoRcd = updateDualBandApInfoRcd(dbr);
                    return updateDualBandApInfoRcd;
                }
                updateDualBandApInfoRcd = insertDualBandApInfoRcd(dbr);
                return updateDualBandApInfoRcd;
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x00c6, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean queryDualBandApInfoRcd(String apBssid, WifiProDualBandApInfoRcd dbr) {
        int recCnt = 0;
        logd("queryDualBandApInfoRcd enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryDualBandApInfoRcd database error.");
                return false;
            } else if (apBssid == null || dbr == null) {
                loge("queryDualBandApInfoRcd null error.");
                return false;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apBSSID like ?", new String[]{apBssid});
                    while (c.moveToNext()) {
                        recCnt++;
                        if (recCnt > 1) {
                            break;
                        } else if (recCnt == 1) {
                            dbr.apBSSID = apBssid;
                            dbr.mApSSID = c.getString(c.getColumnIndex("apSSID"));
                            dbr.mInetCapability = Short.valueOf(c.getShort(c.getColumnIndex("InetCapability")));
                            dbr.mServingBand = Short.valueOf(c.getShort(c.getColumnIndex("ServingBand")));
                            dbr.mApAuthType = Short.valueOf(c.getShort(c.getColumnIndex("ApAuthType")));
                            dbr.mChannelFrequency = c.getInt(c.getColumnIndex("ChannelFrequency"));
                            dbr.mDisappearCount = c.getShort(c.getColumnIndex("DisappearCount"));
                            dbr.isInBlackList = c.getShort(c.getColumnIndex("isInBlackList"));
                            dbr.mUpdateTime = c.getLong(c.getColumnIndex("UpdateTime"));
                            logi("read record succ");
                        }
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (recCnt > 1) {
                        loge("more than one record error. use first record.");
                    } else if (recCnt == 0) {
                        logi("queryDualBandApInfoRcd not record.");
                        return false;
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("queryDualBandApInfoRcd error:");
                        stringBuilder.append(e);
                        loge(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return false;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:25:0x00c9, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<WifiProDualBandApInfoRcd> queryDualBandApInfoRcdBySsid(String ssid) {
        List<WifiProDualBandApInfoRcd> mRecList = new ArrayList();
        logd("queryDualBandApInfoRcd enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryDualBandApInfoRcdBySsid database error.");
                return null;
            } else if (ssid == null) {
                loge("queryDualBandApInfoRcdBySsid null error.");
                return null;
            } else {
                Cursor c = null;
                try {
                    c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apSSID like ?", new String[]{ssid});
                    while (c.moveToNext()) {
                        WifiProDualBandApInfoRcd dbr = new WifiProDualBandApInfoRcd(null);
                        dbr.apBSSID = c.getString(c.getColumnIndex("apBSSID"));
                        dbr.mApSSID = ssid;
                        dbr.mInetCapability = Short.valueOf(c.getShort(c.getColumnIndex("InetCapability")));
                        dbr.mServingBand = Short.valueOf(c.getShort(c.getColumnIndex("ServingBand")));
                        dbr.mApAuthType = Short.valueOf(c.getShort(c.getColumnIndex("ApAuthType")));
                        dbr.mChannelFrequency = c.getInt(c.getColumnIndex("ChannelFrequency"));
                        dbr.mDisappearCount = c.getShort(c.getColumnIndex("DisappearCount"));
                        dbr.isInBlackList = c.getShort(c.getColumnIndex("isInBlackList"));
                        dbr.mUpdateTime = c.getLong(c.getColumnIndex("UpdateTime"));
                        logi("read record succ");
                        mRecList.add(dbr);
                    }
                    if (c != null) {
                        c.close();
                    }
                    if (mRecList.size() == 0) {
                        logi("queryDualBandApInfoRcdBySsid not record.");
                    }
                } catch (SQLException e) {
                    try {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("queryDualBandApInfoRcdBySsid error:");
                        stringBuilder.append(e);
                        loge(stringBuilder.toString());
                        if (c != null) {
                            c.close();
                        }
                        return null;
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x00ac, code:
            if (r3 != null) goto L_0x00ae;
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:21:0x00c9, code:
            if (r3 == null) goto L_0x00cc;
     */
    /* JADX WARNING: Missing block: B:24:0x00cd, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<WifiProDualBandApInfoRcd> getAllDualBandApInfo() {
        logd("getAllDualBandApInfo enter.");
        synchronized (this.mBqeLock) {
            ArrayList<WifiProDualBandApInfoRcd> apInfoList = new ArrayList();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("queryDualBandApInfoRcd database error.");
                return apInfoList;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable", null);
                while (c.moveToNext()) {
                    WifiProDualBandApInfoRcd dbr = new WifiProDualBandApInfoRcd(c.getString(c.getColumnIndex("apBSSID")));
                    dbr.mApSSID = c.getString(c.getColumnIndex("apSSID"));
                    dbr.mInetCapability = Short.valueOf(c.getShort(c.getColumnIndex("InetCapability")));
                    dbr.mServingBand = Short.valueOf(c.getShort(c.getColumnIndex("ServingBand")));
                    dbr.mApAuthType = Short.valueOf(c.getShort(c.getColumnIndex("ApAuthType")));
                    dbr.mChannelFrequency = c.getInt(c.getColumnIndex("ChannelFrequency"));
                    dbr.mDisappearCount = c.getShort(c.getColumnIndex("DisappearCount"));
                    dbr.isInBlackList = c.getShort(c.getColumnIndex("isInBlackList"));
                    dbr.mUpdateTime = c.getLong(c.getColumnIndex("UpdateTime"));
                    apInfoList.add(dbr);
                }
            } catch (SQLException e) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("queryDualBandApInfoRcd error:");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    private boolean updateApRSSIThreshold(String apBSSID, String rssiThreshold) {
        ContentValues values = new ContentValues();
        values.put("RSSIThreshold", rssiThreshold);
        try {
            int rowChg = this.mDatabase.update(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, values, "apBSSID like ?", new String[]{apBSSID});
            if (rowChg == 0) {
                loge("updateApRSSIThreshold update failed.");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateApRSSIThreshold update succ, rowChg=");
            stringBuilder.append(rowChg);
            logd(stringBuilder.toString());
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateApRSSIThreshold error:");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    public boolean addOrUpdateApRSSIThreshold(String apBSSID, String rssiThreshold) {
        logd("addOrUpdateApRSSIThreshold enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || apBSSID == null || rssiThreshold == null) {
                loge("addOrUpdateApRSSIThreshold error.");
                return false;
            }
            if (!checkHistoryRecordExist(WifiProHistoryDBHelper.WP_DUAL_BAND_AP_INFO_TB_NAME, apBSSID)) {
                insertDualBandApInfoRcd(new WifiProDualBandApInfoRcd(apBSSID));
            }
            boolean updateApRSSIThreshold = updateApRSSIThreshold(apBSSID, rssiThreshold);
            return updateApRSSIThreshold;
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0057, code:
            if (r3 != null) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:20:?, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:25:0x0074, code:
            if (r3 == null) goto L_0x0077;
     */
    /* JADX WARNING: Missing block: B:28:0x0078, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String queryApRSSIThreshold(String apBssid) {
        int recCnt = 0;
        String result = null;
        logd("queryApRSSIThreshold enter.");
        synchronized (this.mBqeLock) {
            if (this.mDatabase == null || !this.mDatabase.isOpen() || apBssid == null) {
                loge("queryApRSSIThreshold database error.");
                return null;
            }
            Cursor c = null;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable where apBSSID like ?", new String[]{apBssid});
                while (c.moveToNext()) {
                    recCnt++;
                    if (recCnt > 1) {
                        break;
                    } else if (recCnt == 1) {
                        result = c.getString(c.getColumnIndex("RSSIThreshold"));
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("read record succ, RSSIThreshold = ");
                        stringBuilder.append(result);
                        logi(stringBuilder.toString());
                    }
                }
            } catch (SQLException e) {
                try {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("queryApRSSIThreshold error:");
                    stringBuilder2.append(e);
                    loge(stringBuilder2.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }
    }

    public int getDualBandApInfoSize() {
        logd("getDualBandApInfoSize enter.");
        synchronized (this.mBqeLock) {
            int result = -1;
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                loge("getDualBandApInfoSize database error.");
                return -1;
            }
            String stringBuilder;
            Cursor c = null;
            StringBuilder stringBuilder2;
            try {
                c = this.mDatabase.rawQuery("SELECT * FROM WifiProDualBandApInfoRcdTable", null);
                result = c.getCount();
                if (c != null) {
                    c.close();
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getDualBandApInfoSize: ");
                stringBuilder2.append(result);
                stringBuilder = stringBuilder2.toString();
            } catch (SQLException e) {
                StringBuilder stringBuilder3;
                try {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getDualBandApInfoSize error:");
                    stringBuilder3.append(e);
                    loge(stringBuilder3.toString());
                    if (c != null) {
                        c.close();
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getDualBandApInfoSize: ");
                    stringBuilder2.append(-1);
                    stringBuilder = stringBuilder2.toString();
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getDualBandApInfoSize: ");
                    stringBuilder3.append(-1);
                    logd(stringBuilder3.toString());
                }
            }
            logd(stringBuilder);
            return result;
        }
    }

    private boolean deleteOldestDualBandApInfo() {
        List<WifiProDualBandApInfoRcd> allApInfos = getAllDualBandApInfo();
        if (allApInfos.size() <= 0) {
            return false;
        }
        WifiProDualBandApInfoRcd oldestApInfo = (WifiProDualBandApInfoRcd) allApInfos.get(0);
        for (WifiProDualBandApInfoRcd apInfo : allApInfos) {
            if (apInfo.mUpdateTime < oldestApInfo.mUpdateTime) {
                oldestApInfo = apInfo;
            }
        }
        return deleteAllDualBandAPRcd(oldestApInfo.apBSSID);
    }

    private void logd(String msg) {
        if (printLogLevel <= 1) {
            Log.d(TAG, msg);
        }
    }

    private void logi(String msg) {
        if (printLogLevel <= 2) {
            Log.i(TAG, msg);
        }
    }

    private void loge(String msg) {
        if (printLogLevel <= 3) {
            Log.e(TAG, msg);
        }
    }
}

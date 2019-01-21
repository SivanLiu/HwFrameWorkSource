package com.android.server.hidata.wavemapping.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.SpaceExpInfo;
import com.android.server.hidata.wavemapping.service.HwHistoryQoEResourceBuilder;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class SpaceUserDAO {
    private static final int INTERVAL_24HR = 86400000;
    private static final String TAG;
    private static HwHistoryQoEResourceBuilder mQoeAppBuilder = null;
    private String ScrbId = "NA";
    private String ScrbId_print = "NA";
    private String curFreqLoc = Constant.NAME_FREQLOCATION_OTHER;
    private SQLiteDatabase db = DatabaseSingleton.getInstance();
    private String modelVerAllAp = "0";
    private String modelVerMainAp = "0";
    private long time1stClean = 0;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(SpaceUserDAO.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public SpaceUserDAO() {
        mQoeAppBuilder = HwHistoryQoEResourceBuilder.getInstance();
        this.time1stClean = System.currentTimeMillis();
    }

    public void setFreqLocation(String location) {
        if (location != null) {
            this.curFreqLoc = location;
            deleteOldRecords();
        }
    }

    public void setScrbId(String ScrbId) {
        if (ScrbId != null) {
            this.ScrbId = ScrbId;
            if (ScrbId.length() > 10) {
                this.ScrbId_print = ScrbId.substring(0, 10);
            } else {
                this.ScrbId_print = ScrbId;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setScrbId :");
            stringBuilder.append(this.ScrbId_print);
            LogUtil.v(stringBuilder.toString());
        }
    }

    public void setModelVer(String model_AllAp, String model_MainAp) {
        if (model_AllAp != null) {
            this.modelVerAllAp = model_AllAp;
        }
        if (model_MainAp != null) {
            this.modelVerMainAp = model_MainAp;
        }
    }

    public boolean insertBase(SpaceExpInfo spaceinfo) {
        if (networkIdFoundBaseBySpaceNetwork(spaceinfo.getSpaceID(), spaceinfo.getSpaceIDMain(), spaceinfo.getNetworkId(), spaceinfo.getNetworkName(), spaceinfo.getNetworkFreq()) != null) {
            return update(spaceinfo);
        }
        ContentValues cValueBase = new ContentValues();
        cValueBase.put("SCRBID", this.ScrbId);
        cValueBase.put("FREQLOCNAME", this.curFreqLoc);
        cValueBase.put("SPACEID", spaceinfo.getSpaceID());
        cValueBase.put("SPACEIDMAIN", spaceinfo.getSpaceIDMain());
        cValueBase.put("NETWORKNAME", spaceinfo.getNetworkName());
        cValueBase.put("NETWORKFREQ", spaceinfo.getNetworkFreq());
        cValueBase.put("NETWORKID", spaceinfo.getNetworkId());
        cValueBase.put("SIGNAL_VALUE", Integer.valueOf(spaceinfo.getSignalValue()));
        cValueBase.put("POWER_CONSUMPTION", Long.valueOf(spaceinfo.getPowerConsumption()));
        cValueBase.put("DATA_RX", Long.valueOf(spaceinfo.getDataRx()));
        cValueBase.put("DATA_TX", Long.valueOf(spaceinfo.getDataTx()));
        cValueBase.put("USER_PREF_OPT_IN", Integer.valueOf(spaceinfo.getUserPrefOptIn()));
        cValueBase.put("USER_PREF_OPT_OUT", Integer.valueOf(spaceinfo.getUserPrefOptOut()));
        cValueBase.put("USER_PREF_STAY", Integer.valueOf(spaceinfo.getUserPrefStay()));
        cValueBase.put("USER_PREF_TOTAL_COUNT", Integer.valueOf(spaceinfo.getUserPrefTotalCount()));
        cValueBase.put("DURATION_CONNECTED", Long.valueOf(spaceinfo.getDuration()));
        cValueBase.put("NW_TYPE", Long.valueOf(spaceinfo.getNetworkType()));
        cValueBase.put("MODEL_VER_ALLAP", this.modelVerAllAp);
        cValueBase.put("MODEL_VER_MAINAP", this.modelVerMainAp);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insert BASE, FreqLoc:");
        stringBuilder.append(this.curFreqLoc);
        stringBuilder.append(", ");
        stringBuilder.append(spaceinfo.toString());
        stringBuilder.append(", modelVer:");
        stringBuilder.append(this.modelVerAllAp);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(this.modelVerMainAp);
        LogUtil.i(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("insert BASE, scrib ID:");
        stringBuilder.append(this.ScrbId_print);
        LogUtil.v(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.insert(Constant.SPACEUSER_TABLE_NAME, null, cValueBase);
            this.db.setTransactionSuccessful();
            this.db.endTransaction();
            if (0 == this.time1stClean) {
                this.time1stClean = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.time1stClean > 86400000) {
                deleteOldRecords();
            }
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("insert exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            this.db.endTransaction();
            return false;
        } catch (Throwable th) {
            this.db.endTransaction();
            throw th;
        }
    }

    public boolean insertApp(SpaceExpInfo spaceinfo) {
        Exception e;
        StringBuilder stringBuilder;
        boolean result = true;
        for (Entry<Integer, Float> entry : mQoeAppBuilder.getQoEAppList().entrySet()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Constant.USERDB_APP_NAME_PREFIX);
            stringBuilder2.append(entry.getKey());
            String appName = stringBuilder2.toString();
            HashMap<String, Long> mapDuration = spaceinfo.getMapAppDuration();
            int good;
            int poor;
            try {
                if (mapDuration.containsKey(appName)) {
                    long duration = ((Long) mapDuration.get(appName)).longValue();
                    if (duration > 0) {
                        int poor2;
                        int poor3;
                        long duration2;
                        if (spaceinfo.getMapAppQoeGood().containsKey(appName)) {
                            good = ((Integer) spaceinfo.getMapAppQoeGood().get(appName)).intValue();
                        } else {
                            good = 0;
                        }
                        try {
                            if (spaceinfo.getMapAppQoePoor().containsKey(appName)) {
                                poor2 = ((Integer) spaceinfo.getMapAppQoePoor().get(appName)).intValue();
                            } else {
                                poor2 = 0;
                            }
                        } catch (Exception e2) {
                            e = e2;
                            poor = 0;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("insertApp exception: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            result = false;
                        }
                        try {
                            poor3 = poor2;
                            duration2 = duration;
                        } catch (Exception e3) {
                            e = e3;
                            poor = poor2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("insertApp exception: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            result = false;
                        }
                        try {
                            if (networkIdFoundAppSpaceNetwork(appName, spaceinfo.getSpaceID(), spaceinfo.getSpaceIDMain(), spaceinfo.getNetworkId(), spaceinfo.getNetworkName(), spaceinfo.getNetworkFreq()) != null) {
                                try {
                                    if (!updateApp(appName, duration2, good, poor3, spaceinfo)) {
                                        result = false;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("updateApp APP fail:");
                                        stringBuilder2.append(appName);
                                        stringBuilder2.append(",FreqLoc:");
                                        stringBuilder2.append(this.curFreqLoc);
                                        LogUtil.w(stringBuilder2.toString());
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("updateApp APP fail, nwId:");
                                        stringBuilder2.append(spaceinfo.getNetworkId());
                                        LogUtil.v(stringBuilder2.toString());
                                    }
                                    poor = poor3;
                                } catch (Exception e4) {
                                    e = e4;
                                    poor = poor3;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("insertApp exception: ");
                                    stringBuilder.append(e.getMessage());
                                    LogUtil.e(stringBuilder.toString());
                                    result = false;
                                }
                            } else {
                                int good2 = new ContentValues();
                                good2.put("SCRBID", this.ScrbId);
                                good2.put("FREQLOCNAME", this.curFreqLoc);
                                good2.put("SPACEID", spaceinfo.getSpaceID());
                                good2.put("SPACEIDMAIN", spaceinfo.getSpaceIDMain());
                                good2.put("NETWORKNAME", spaceinfo.getNetworkName());
                                good2.put("NETWORKFREQ", spaceinfo.getNetworkFreq());
                                good2.put("NETWORKID", spaceinfo.getNetworkId());
                                good2.put("NW_TYPE", Long.valueOf(spaceinfo.getNetworkType()));
                                int poor4 = duration2;
                                good2.put(Constant.USERDB_APP_NAME_DURATION, Long.valueOf(poor4));
                                poor = poor3;
                                try {
                                    SQLiteDatabase sQLiteDatabase;
                                    good2.put(Constant.USERDB_APP_NAME_POOR, Integer.valueOf(poor));
                                    good2.put(Constant.USERDB_APP_NAME_GOOD, Integer.valueOf(good));
                                    good2.put("MODEL_VER_ALLAP", this.modelVerAllAp);
                                    good2.put("MODEL_VER_MAINAP", this.modelVerMainAp);
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("insert APP:");
                                    stringBuilder2.append(appName);
                                    stringBuilder2.append(",FreqLoc:");
                                    stringBuilder2.append(this.curFreqLoc);
                                    stringBuilder2.append(", duration:");
                                    stringBuilder2.append(poor4);
                                    stringBuilder2.append(", poor:");
                                    stringBuilder2.append(poor);
                                    stringBuilder2.append(", good:");
                                    stringBuilder2.append(good);
                                    stringBuilder2.append(", modelVer:");
                                    stringBuilder2.append(this.modelVerAllAp);
                                    stringBuilder2.append(Constant.RESULT_SEPERATE);
                                    stringBuilder2.append(this.modelVerMainAp);
                                    LogUtil.i(stringBuilder2.toString());
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("insert APP:");
                                    stringBuilder2.append(appName);
                                    stringBuilder2.append(", scrib ID:");
                                    stringBuilder2.append(this.ScrbId_print);
                                    LogUtil.v(stringBuilder2.toString());
                                    try {
                                        this.db.beginTransaction();
                                        this.db.insert(appName, null, good2);
                                        this.db.setTransactionSuccessful();
                                        sQLiteDatabase = this.db;
                                    } catch (Exception e5) {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("db insert exception: ");
                                        stringBuilder3.append(e5.getMessage());
                                        LogUtil.e(stringBuilder3.toString());
                                        result = false;
                                        sQLiteDatabase = this.db;
                                    }
                                    sQLiteDatabase.endTransaction();
                                } catch (Exception e6) {
                                    e5 = e6;
                                } catch (Throwable th) {
                                    this.db.endTransaction();
                                }
                            }
                        } catch (Exception e7) {
                            e5 = e7;
                            poor = poor3;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("insertApp exception: ");
                            stringBuilder.append(e5.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            result = false;
                        }
                    }
                } else {
                    good = 0;
                    poor = 0;
                }
            } catch (Exception e8) {
                e5 = e8;
                good = 0;
                poor = 0;
                stringBuilder = new StringBuilder();
                stringBuilder.append("insertApp exception: ");
                stringBuilder.append(e5.getMessage());
                LogUtil.e(stringBuilder.toString());
                result = false;
            }
        }
        return result;
    }

    public boolean update(SpaceExpInfo spaceinfo) {
        String sql = "UPDATE SPACEUSER_BASE SET SIGNAL_VALUE = ?,USER_PREF_OPT_IN = ?,USER_PREF_OPT_OUT = ?,USER_PREF_STAY = ?,USER_PREF_TOTAL_COUNT = ?, POWER_CONSUMPTION = ?, DATA_RX = ?, DATA_TX = ?, DURATION_CONNECTED = ? WHERE SCRBID = ? AND FREQLOCNAME = ? AND NETWORKID = ? AND NETWORKNAME = ? AND NETWORKFREQ = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND UPDATE_DATE = (date('now', 'localtime'))";
        args = new Object[18];
        boolean z = false;
        args[0] = Integer.valueOf(spaceinfo.getSignalValue());
        args[1] = Integer.valueOf(spaceinfo.getUserPrefOptIn());
        args[2] = Integer.valueOf(spaceinfo.getUserPrefOptOut());
        args[3] = Integer.valueOf(spaceinfo.getUserPrefStay());
        args[4] = Integer.valueOf(spaceinfo.getUserPrefTotalCount());
        args[5] = Long.valueOf(spaceinfo.getPowerConsumption());
        args[6] = Long.valueOf(spaceinfo.getDataRx());
        args[7] = Long.valueOf(spaceinfo.getDataTx());
        args[8] = Long.valueOf(spaceinfo.getDuration());
        args[9] = this.ScrbId;
        args[10] = this.curFreqLoc;
        args[11] = spaceinfo.getNetworkId();
        args[12] = spaceinfo.getNetworkName();
        args[13] = spaceinfo.getNetworkFreq();
        args[14] = spaceinfo.getSpaceID();
        args[15] = spaceinfo.getSpaceIDMain();
        args[16] = this.modelVerAllAp;
        args[17] = this.modelVerMainAp;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update BASE begin: FreqLoc:");
        stringBuilder.append(this.curFreqLoc);
        stringBuilder.append(", modelVer:");
        stringBuilder.append(this.modelVerAllAp);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(this.modelVerMainAp);
        stringBuilder.append(", ");
        stringBuilder.append(spaceinfo.toString());
        LogUtil.i(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("update BASE begin: scribId:");
        stringBuilder.append(this.ScrbId_print);
        LogUtil.v(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("UPDATE exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            return z;
        }
        return true;
    }

    public boolean updateApp(String appName, long duration, int good, int poor, SpaceExpInfo spaceinfo) {
        String sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(appName);
        sql.append(" SET DURATION = ?,POORCOUNT = ?,GOODCOUNT = ? WHERE SCRBID = ? AND FREQLOCNAME = ? AND NETWORKID = ? AND NETWORKNAME = ? AND NETWORKFREQ = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND UPDATE_DATE = (date('now', 'localtime'))");
        sql = sql.toString();
        args = new Object[12];
        boolean z = false;
        args[0] = Long.valueOf(duration);
        args[1] = Integer.valueOf(poor);
        args[2] = Integer.valueOf(good);
        args[3] = this.ScrbId;
        args[4] = this.curFreqLoc;
        args[5] = spaceinfo.getNetworkId();
        args[6] = spaceinfo.getNetworkName();
        args[7] = spaceinfo.getNetworkFreq();
        args[8] = spaceinfo.getSpaceID();
        args[9] = spaceinfo.getSpaceIDMain();
        args[10] = this.modelVerAllAp;
        args[11] = this.modelVerMainAp;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update APP:");
        stringBuilder.append(appName);
        stringBuilder.append(" begin: FreqLoc:");
        stringBuilder.append(this.curFreqLoc);
        stringBuilder.append(", duration:");
        stringBuilder.append(duration);
        stringBuilder.append(", poor:");
        stringBuilder.append(poor);
        stringBuilder.append(", good:");
        stringBuilder.append(good);
        stringBuilder.append(", modelVer:");
        stringBuilder.append(this.modelVerAllAp);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(this.modelVerMainAp);
        LogUtil.i(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("update APP:");
        stringBuilder.append(appName);
        stringBuilder.append(" begin: scribId:");
        stringBuilder.append(this.ScrbId_print);
        LogUtil.v(stringBuilder.toString());
        try {
            this.db.beginTransaction();
            this.db.execSQL(sql, args);
            this.db.setTransactionSuccessful();
        } catch (SQLException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("UPDATE exception: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return z;
        } finally {
            z = this.db;
            z.endTransaction();
            return z;
        }
        return true;
    }

    public boolean remove(String spaceid_allAp, String spaceid_mainAp, String networkid) {
        try {
            this.db.execSQL("DELETE FROM SPACEUSER_BASE  WHERE FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND NETWORKID = ?", new Object[]{this.curFreqLoc, spaceid_allAp, spaceid_mainAp, networkid});
            return true;
        } catch (SQLException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove exception: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0055, code skipped:
            if (r2 != null) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:18:0x0093, code skipped:
            if (r2 == null) goto L_0x0096;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String networkIdFoundBaseBySpaceNetwork(String spaceid_allAp, String spaceid_mainAp, String networkid, String networkname, String networkfreq) {
        StringBuilder stringBuilder;
        String sql = "SELECT NETWORKID, UPDATE_DATE FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND NETWORKID = ? AND NETWORKNAME = ? AND NETWORKFREQ = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND UPDATE_DATE = (date('now', 'localtime'))";
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, spaceid_mainAp, networkid, networkname, networkfreq, this.modelVerAllAp, this.modelVerMainAp};
        Cursor cursor = null;
        String netId = null;
        String date = null;
        if (this.db == null) {
            return null;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                netId = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKID"));
                date = cursor.getString(cursor.getColumnIndexOrThrow("UPDATE_DATE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("networkIdFoundBaseBySpaceNetwork IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("networkIdFoundBaseBySpaceNetwork Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2;
            if (netId != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundBaseBySpaceNetwork, Date:");
                stringBuilder2.append(date);
                stringBuilder2.append(", FreqLoc:");
                stringBuilder2.append(this.curFreqLoc);
                stringBuilder2.append(", SPACEID:");
                stringBuilder2.append(spaceid_allAp);
                stringBuilder2.append(", SPACEIDMAIN:");
                stringBuilder2.append(spaceid_mainAp);
                stringBuilder2.append(", modelVer:");
                stringBuilder2.append(this.modelVerAllAp);
                stringBuilder2.append(Constant.RESULT_SEPERATE);
                stringBuilder2.append(this.modelVerMainAp);
                LogUtil.i(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundBaseBySpaceNetwork, netId:");
                stringBuilder2.append(netId);
                stringBuilder2.append(", ScrbId:");
                stringBuilder2.append(this.ScrbId_print);
                LogUtil.v(stringBuilder2.toString());
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundBaseBySpaceNetwork, NO DATA, sql:");
                stringBuilder2.append(sql);
                stringBuilder2.append(", FreqLoc:");
                stringBuilder2.append(this.curFreqLoc);
                stringBuilder2.append(", SPACEID:");
                stringBuilder2.append(spaceid_allAp);
                stringBuilder2.append(", SPACEIDMAIN:");
                stringBuilder2.append(spaceid_mainAp);
                stringBuilder2.append(", modelVer:");
                stringBuilder2.append(this.modelVerAllAp);
                stringBuilder2.append(Constant.RESULT_SEPERATE);
                stringBuilder2.append(this.modelVerMainAp);
                LogUtil.d(stringBuilder2.toString());
            }
            return netId;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0069, code skipped:
            if (r2 != null) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:18:0x00a7, code skipped:
            if (r2 == null) goto L_0x00aa;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String networkIdFoundAppSpaceNetwork(String appName, String spaceid_allAp, String spaceid_mainAp, String networkid, String networkname, String networkfreq) {
        StringBuilder stringBuilder;
        String sql = new StringBuilder();
        sql.append("SELECT NETWORKID, UPDATE_DATE FROM ");
        sql.append(appName);
        sql.append(" WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND NETWORKID = ? AND NETWORKNAME = ? AND NETWORKFREQ = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND UPDATE_DATE = (date('now', 'localtime'))");
        sql = sql.toString();
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, spaceid_mainAp, networkid, networkname, networkfreq, this.modelVerAllAp, this.modelVerMainAp};
        Cursor cursor = null;
        String netId = null;
        String date = null;
        if (this.db == null) {
            return null;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            if (cursor.moveToNext()) {
                netId = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKID"));
                date = cursor.getString(cursor.getColumnIndexOrThrow("UPDATE_DATE"));
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("networkIdFoundAppSpaceNetwork IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("networkIdFoundAppSpaceNetwork Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            StringBuilder stringBuilder2;
            if (netId != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundAppSpaceNetwork, appName");
                stringBuilder2.append(appName);
                stringBuilder2.append(", Date:");
                stringBuilder2.append(date);
                stringBuilder2.append(", FreqLoc:");
                stringBuilder2.append(this.curFreqLoc);
                stringBuilder2.append(", SPACEID:");
                stringBuilder2.append(spaceid_allAp);
                stringBuilder2.append(", SPACEIDMAIN:");
                stringBuilder2.append(spaceid_mainAp);
                stringBuilder2.append(", modelVer:");
                stringBuilder2.append(this.modelVerAllAp);
                stringBuilder2.append(Constant.RESULT_SEPERATE);
                stringBuilder2.append(this.modelVerMainAp);
                LogUtil.i(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundAppSpaceNetwork, appName");
                stringBuilder2.append(appName);
                stringBuilder2.append(", netId:");
                stringBuilder2.append(netId);
                stringBuilder2.append(", ScrbId:");
                stringBuilder2.append(this.ScrbId_print);
                LogUtil.v(stringBuilder2.toString());
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networkIdFoundAppSpaceNetwork, NO DATA, sql:");
                stringBuilder2.append(sql);
                stringBuilder2.append(", FreqLoc:");
                stringBuilder2.append(this.curFreqLoc);
                stringBuilder2.append(", SPACEID:");
                stringBuilder2.append(spaceid_allAp);
                stringBuilder2.append(", SPACEIDMAIN:");
                stringBuilder2.append(spaceid_mainAp);
                stringBuilder2.append(", modelVer:");
                stringBuilder2.append(this.modelVerAllAp);
                stringBuilder2.append(Constant.RESULT_SEPERATE);
                stringBuilder2.append(this.modelVerMainAp);
                LogUtil.d(stringBuilder2.toString());
            }
            return netId;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getAllAppTitleString() {
        String appName = "";
        StringBuffer sql_title = new StringBuffer();
        for (Entry<Integer, Float> entry : mQoeAppBuilder.getQoEAppList().entrySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.USERDB_APP_NAME_PREFIX);
            stringBuilder.append(entry.getKey());
            appName = stringBuilder.toString();
            String sql_title1 = new StringBuilder();
            sql_title1.append(appName);
            sql_title1.append(".");
            sql_title1.append(Constant.USERDB_APP_NAME_DURATION);
            sql_title1.append(" AS ");
            sql_title1.append(appName);
            sql_title1.append(Constant.RESULT_SEPERATE);
            sql_title1.append(Constant.USERDB_APP_NAME_DURATION);
            sql_title1 = sql_title1.toString();
            String sql_title2 = new StringBuilder();
            sql_title2.append(appName);
            sql_title2.append(".");
            sql_title2.append(Constant.USERDB_APP_NAME_POOR);
            sql_title2.append(" AS ");
            sql_title2.append(appName);
            sql_title2.append(Constant.RESULT_SEPERATE);
            sql_title2.append(Constant.USERDB_APP_NAME_POOR);
            sql_title2 = sql_title2.toString();
            String sql_title3 = new StringBuilder();
            sql_title3.append(appName);
            sql_title3.append(".");
            sql_title3.append(Constant.USERDB_APP_NAME_GOOD);
            sql_title3.append(" AS ");
            sql_title3.append(appName);
            sql_title3.append(Constant.RESULT_SEPERATE);
            sql_title3.append(Constant.USERDB_APP_NAME_GOOD);
            sql_title3 = sql_title3.toString();
            sql_title.append(", ");
            sql_title.append(sql_title1);
            sql_title.append(", ");
            sql_title.append(sql_title2);
            sql_title.append(", ");
            sql_title.append(sql_title3);
        }
        return sql_title.toString();
    }

    private String getAllAppJoinString() {
        String appName = "";
        StringBuffer sql_title = new StringBuffer();
        for (Entry<Integer, Float> entry : mQoeAppBuilder.getQoEAppList().entrySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.USERDB_APP_NAME_PREFIX);
            stringBuilder.append(entry.getKey());
            appName = stringBuilder.toString();
            String sql_join = new StringBuilder();
            sql_join.append(" LEFT OUTER JOIN ");
            sql_join.append(appName);
            sql_join.append(" USING(SCRBID,FREQLOCNAME,SPACEID,SPACEIDMAIN,NETWORKID,NETWORKNAME,NETWORKFREQ,NW_TYPE,UPDATE_DATE)");
            sql_title.append(sql_join.toString());
        }
        return sql_title.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x0383 A:{Catch:{ IllegalArgumentException -> 0x0384, Exception -> 0x035f, all -> 0x0354, all -> 0x03aa }} */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x03ad  */
    /* JADX WARNING: Missing block: B:79:0x034e, code skipped:
            if (r13 != null) goto L_0x0350;
     */
    /* JADX WARNING: Missing block: B:92:0x03a6, code skipped:
            if (r13 == null) goto L_0x03a9;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<String, SpaceExpInfo> findAllByTwoSpaces(String spaceid_allAp, String spaceid_mainAp) {
        IllegalArgumentException e;
        HashMap<String, SpaceExpInfo> spaceInfoHashMap;
        StringBuilder stringBuilder;
        Exception e2;
        Throwable th;
        String sql_common = "SPACEUSER_BASE.SPACEID,SPACEUSER_BASE.SPACEIDMAIN,SPACEUSER_BASE.NETWORKID,SPACEUSER_BASE.NETWORKNAME,SPACEUSER_BASE.NETWORKFREQ,SPACEUSER_BASE.NW_TYPE,";
        String sql_base = "SIGNAL_VALUE,USER_PREF_OPT_IN,USER_PREF_OPT_OUT,USER_PREF_STAY,USER_PREF_TOTAL_COUNT,POWER_CONSUMPTION,DURATION_CONNECTED";
        String sql_app_title = getAllAppTitleString();
        String sql_app_join = getAllAppJoinString();
        String sql_condition = " WHERE SPACEUSER_BASE.SCRBID = ? AND SPACEUSER_BASE.FREQLOCNAME = ? AND SPACEUSER_BASE.SPACEID = ? AND SPACEUSER_BASE.SPACEIDMAIN = ? AND SPACEUSER_BASE.MODEL_VER_ALLAP = ? AND SPACEUSER_BASE.MODEL_VER_MAINAP = ? AND SPACEUSER_BASE.UPDATE_DATE = (date('now', 'localtime'))";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("SELECT ");
        stringBuilder2.append(sql_common);
        stringBuilder2.append(sql_base);
        stringBuilder2.append(sql_app_title);
        stringBuilder2.append(" FROM ");
        stringBuilder2.append(Constant.SPACEUSER_TABLE_NAME);
        stringBuilder2.append(sql_app_join);
        stringBuilder2.append(sql_condition);
        String sql = stringBuilder2.toString();
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, spaceid_mainAp, this.modelVerAllAp, this.modelVerMainAp};
        Cursor cursor = null;
        HashMap<String, SpaceExpInfo> spaceInfoHashMap2 = new HashMap();
        String appName = "";
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: sql_comm=SELECT ");
        stringBuilder2.append(sql_common);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: sql_base=");
        stringBuilder2.append(sql_base);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: sql_appt=");
        stringBuilder2.append(sql_app_title);
        LogUtil.i(stringBuilder2.toString());
        LogUtil.i(" findAllByTwoSpaces: sql_from= FROM SPACEUSER_BASE");
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: sql_appj=");
        stringBuilder2.append(sql_app_join);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: sql_cond=");
        stringBuilder2.append(sql_condition);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" findAllByTwoSpaces: args=");
        stringBuilder2.append(Arrays.toString(Arrays.copyOfRange(args, 1, args.length)));
        LogUtil.v(stringBuilder2.toString());
        String sql_base2;
        String sql_app_title2;
        String str;
        String str2;
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                StringBuilder spaceId = new StringBuilder(cursor.getString(cursor.getColumnIndexOrThrow("SPACEID")));
                StringBuilder spaceIdmain = new StringBuilder(cursor.getString(cursor.getColumnIndexOrThrow("SPACEIDMAIN")));
                HashMap<String, Long> duration_app = new HashMap();
                HashMap<String, Integer> qoe_app_poor = new HashMap();
                HashMap<String, Integer> qoe_app_good = new HashMap();
                sql_base2 = sql_base;
                try {
                    sql_base = mQoeAppBuilder.getQoEAppList();
                    sql_app_title2 = sql_app_title;
                } catch (IllegalArgumentException e3) {
                    e = e3;
                    sql_app_title2 = sql_app_title;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e4) {
                    e2 = e4;
                    sql_app_title2 = sql_app_title;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAllByTwoSpaces Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                    return spaceInfoHashMap;
                } catch (Throwable th2) {
                    th = th2;
                    sql_app_title2 = sql_app_title;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
                try {
                    String mAppMap;
                    Iterator spaceInfoHashMap3 = sql_base.entrySet().iterator();
                    while (spaceInfoHashMap3.hasNext()) {
                        try {
                            Entry<Integer, Float> entry = (Entry) spaceInfoHashMap3.next();
                            mAppMap = sql_base;
                            sql_base = new StringBuilder();
                            Iterator it = spaceInfoHashMap3;
                            sql_base.append(Constant.USERDB_APP_NAME_PREFIX);
                            str = sql_app_join;
                            Entry<Integer, Float> entry2 = entry;
                            try {
                                sql_base.append(entry2.getKey());
                                sql_base = sql_base.toString();
                            } catch (IllegalArgumentException e5) {
                                e = e5;
                                spaceInfoHashMap = spaceInfoHashMap2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                                stringBuilder.append(e.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (Exception e6) {
                                e2 = e6;
                                spaceInfoHashMap = spaceInfoHashMap2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("findAllByTwoSpaces Exception: ");
                                stringBuilder.append(e2.getMessage());
                                LogUtil.e(stringBuilder.toString());
                                if (cursor != null) {
                                }
                                return spaceInfoHashMap;
                            } catch (Throwable th3) {
                                th = th3;
                                if (cursor != null) {
                                }
                                throw th;
                            }
                            try {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(sql_base);
                                stringBuilder3.append(Constant.RESULT_SEPERATE);
                                stringBuilder3.append(Constant.USERDB_APP_NAME_DURATION);
                                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(stringBuilder3.toString()));
                                if (duration > 0) {
                                    duration_app.put(sql_base, Long.valueOf(duration));
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append(sql_base);
                                    stringBuilder4.append(Constant.RESULT_SEPERATE);
                                    stringBuilder4.append(Constant.USERDB_APP_NAME_POOR);
                                    qoe_app_poor.put(sql_base, Integer.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(stringBuilder4.toString()))));
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append(sql_base);
                                    stringBuilder4.append(Constant.RESULT_SEPERATE);
                                    stringBuilder4.append(Constant.USERDB_APP_NAME_GOOD);
                                    qoe_app_good.put(sql_base, Integer.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(stringBuilder4.toString()))));
                                }
                                appName = sql_base;
                                sql_base = mAppMap;
                                spaceInfoHashMap3 = it;
                                sql_app_join = str;
                            } catch (IllegalArgumentException e7) {
                                e = e7;
                                appName = sql_base;
                                spaceInfoHashMap = spaceInfoHashMap2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                                stringBuilder.append(e.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (Exception e8) {
                                e2 = e8;
                                appName = sql_base;
                                spaceInfoHashMap = spaceInfoHashMap2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("findAllByTwoSpaces Exception: ");
                                stringBuilder.append(e2.getMessage());
                                LogUtil.e(stringBuilder.toString());
                                if (cursor != null) {
                                }
                                return spaceInfoHashMap;
                            } catch (Throwable th4) {
                                th = th4;
                                appName = sql_base;
                                if (cursor != null) {
                                }
                                throw th;
                            }
                        } catch (IllegalArgumentException e9) {
                            e = e9;
                            str = sql_app_join;
                            str2 = sql_condition;
                            spaceInfoHashMap = spaceInfoHashMap2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e10) {
                            e2 = e10;
                            str = sql_app_join;
                            str2 = sql_condition;
                            spaceInfoHashMap = spaceInfoHashMap2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAllByTwoSpaces Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            if (cursor != null) {
                            }
                            return spaceInfoHashMap;
                        } catch (Throwable th5) {
                            th = th5;
                            str = sql_app_join;
                            str2 = sql_condition;
                            spaceInfoHashMap = spaceInfoHashMap2;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                    }
                    mAppMap = sql_base;
                    str = sql_app_join;
                    try {
                        str2 = sql_condition;
                    } catch (IllegalArgumentException e11) {
                        e = e11;
                        str2 = sql_condition;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e12) {
                        e2 = e12;
                        str2 = sql_condition;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAllByTwoSpaces Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (cursor != null) {
                        }
                        return spaceInfoHashMap;
                    } catch (Throwable th6) {
                        th = th6;
                        str2 = sql_condition;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                    try {
                        String spaceInfo = new SpaceExpInfo(spaceId, spaceIdmain, cursor.getString(cursor.getColumnIndexOrThrow("NETWORKID")), cursor.getString(cursor.getColumnIndexOrThrow("NETWORKNAME")), cursor.getString(cursor.getColumnIndexOrThrow("NETWORKFREQ")), qoe_app_poor, qoe_app_good, duration_app, 0, 0, 0, cursor.getInt(cursor.getColumnIndexOrThrow("SIGNAL_VALUE")), (long) cursor.getInt(cursor.getColumnIndexOrThrow("POWER_CONSUMPTION")), cursor.getInt(cursor.getColumnIndexOrThrow("USER_PREF_OPT_IN")), cursor.getInt(cursor.getColumnIndexOrThrow("USER_PREF_OPT_OUT")), cursor.getInt(cursor.getColumnIndexOrThrow("USER_PREF_STAY")), cursor.getInt(cursor.getColumnIndexOrThrow("USER_PREF_TOTAL_COUNT")), cursor.getLong(cursor.getColumnIndexOrThrow("DURATION_CONNECTED")), cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE")));
                        sql_base = new StringBuilder();
                        sql_base.append(" findAllByTwoSpaces:SPACEID:");
                        sql_base.append(spaceInfo.getSpaceID());
                        sql_base.append(",SPACEIDMAIN:");
                        sql_base.append(spaceInfo.getSpaceIDMain());
                        sql_base.append(",FreqLoc:");
                        sql_base.append(this.curFreqLoc);
                        sql_base.append(spaceInfo.toString());
                        LogUtil.i(sql_base.toString());
                        spaceInfoHashMap = spaceInfoHashMap2;
                        try {
                            spaceInfoHashMap.put(spaceInfo.getNetworkId(), spaceInfo);
                            spaceInfoHashMap2 = spaceInfoHashMap;
                            sql_base = sql_base2;
                            sql_app_title = sql_app_title2;
                            sql_app_join = str;
                            sql_condition = str2;
                        } catch (IllegalArgumentException e13) {
                            e = e13;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e14) {
                            e2 = e14;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAllByTwoSpaces Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            if (cursor != null) {
                            }
                            return spaceInfoHashMap;
                        }
                    } catch (IllegalArgumentException e15) {
                        e = e15;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e16) {
                        e2 = e16;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAllByTwoSpaces Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (cursor != null) {
                        }
                        return spaceInfoHashMap;
                    } catch (Throwable th7) {
                        th = th7;
                        spaceInfoHashMap = spaceInfoHashMap2;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                } catch (IllegalArgumentException e17) {
                    e = e17;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e18) {
                    e2 = e18;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAllByTwoSpaces Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    if (cursor != null) {
                    }
                    return spaceInfoHashMap;
                } catch (Throwable th8) {
                    th = th8;
                    str = sql_app_join;
                    str2 = sql_condition;
                    spaceInfoHashMap = spaceInfoHashMap2;
                    if (cursor != null) {
                    }
                    throw th;
                }
            }
            sql_app_title2 = sql_app_title;
            str = sql_app_join;
            str2 = sql_condition;
            spaceInfoHashMap = spaceInfoHashMap2;
        } catch (IllegalArgumentException e19) {
            e = e19;
            sql_base2 = sql_base;
            sql_app_title2 = sql_app_title;
            str = sql_app_join;
            str2 = sql_condition;
            spaceInfoHashMap = spaceInfoHashMap2;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllByTwoSpaces IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e20) {
            e2 = e20;
            sql_base2 = sql_base;
            sql_app_title2 = sql_app_title;
            str = sql_app_join;
            str2 = sql_condition;
            spaceInfoHashMap = spaceInfoHashMap2;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAllByTwoSpaces Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
            }
            return spaceInfoHashMap;
        } catch (Throwable th9) {
            th = th9;
            if (cursor != null) {
            }
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:8:0x003a, code skipped:
            if (r3 != null) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:18:0x0078, code skipped:
            if (r3 == null) goto L_0x007b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void deleteOldRecords() {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        String sql = "SELECT COUNT(DISTINCT UPDATE_DATE) RECORDCNT, MIN(UPDATE_DATE) EXPDATE FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ?";
        String[] args = new String[]{this.ScrbId, this.curFreqLoc};
        Cursor cursor = null;
        int recordsCnt = 0;
        String expireDate = null;
        if (this.db != null) {
            try {
                cursor = this.db.rawQuery(sql, args);
                if (cursor.moveToNext()) {
                    recordsCnt = cursor.getInt(cursor.getColumnIndexOrThrow("RECORDCNT"));
                    expireDate = cursor.getString(cursor.getColumnIndexOrThrow("EXPDATE"));
                }
            } catch (IllegalArgumentException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("networkIdFoundBaseBySpaceNetwork IllegalArgumentException: ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("networkIdFoundBaseBySpaceNetwork Exception: ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
                if (cursor != null) {
                    cursor.close();
                }
                if (30 < recordsCnt) {
                    LogUtil.i("deleteOldRecords: current records exceed (30) days");
                    try {
                        this.db.execSQL("DELETE FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND UPDATE_DATE <= ?", new String[]{this.ScrbId, this.curFreqLoc, expireDate});
                    } catch (IllegalArgumentException e3) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("networkIdFoundBaseBySpaceNetwork IllegalArgumentException: ");
                        stringBuilder2.append(e3.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                    } catch (Exception e4) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("networkIdFoundBaseBySpaceNetwork Exception: ");
                        stringBuilder2.append(e4.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                    }
                } else {
                    LogUtil.i("deleteOldRecords: keep data");
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x0281 A:{Catch:{ IllegalArgumentException -> 0x0282, Exception -> 0x025c, all -> 0x0250, all -> 0x02a9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x02ac  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0281 A:{Catch:{ IllegalArgumentException -> 0x0282, Exception -> 0x025c, all -> 0x0250, all -> 0x02a9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x02ac  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x02ac  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0281 A:{Catch:{ IllegalArgumentException -> 0x0282, Exception -> 0x025c, all -> 0x0250, all -> 0x02a9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x02ac  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0281 A:{Catch:{ IllegalArgumentException -> 0x0282, Exception -> 0x025c, all -> 0x0250, all -> 0x02a9 }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x02ac  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0281 A:{Catch:{ IllegalArgumentException -> 0x0282, Exception -> 0x025c, all -> 0x0250, all -> 0x02a9 }} */
    /* JADX WARNING: Missing block: B:51:0x024a, code skipped:
            if (r15 != null) goto L_0x024c;
     */
    /* JADX WARNING: Missing block: B:64:0x02a5, code skipped:
            if (r15 != null) goto L_0x024c;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<String, List> findAppQoEgroupBySpace(String appName, String spaceid_allAp, String spaceid_mainAp) {
        IllegalArgumentException e;
        StringBuilder stringBuilder;
        Exception e2;
        Throwable th;
        String str = spaceid_allAp;
        String str2 = spaceid_mainAp;
        String title_idCount = "COUNT(\"NETWORKID\")";
        String title_freqCount = "COUNT(\"NETWORKFREQ\")";
        String title_duration = "SUM(\"DURATION\")";
        String title_poor = "SUM(\"POORCOUNT\")";
        String title_good = "SUM(\"GOODCOUNT\")";
        String title_dayCount = "COUNT(DISTINCT UPDATE_DATE)";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("SELECT NETWORKNAME, NW_TYPE, ");
        stringBuilder2.append(title_idCount);
        stringBuilder2.append(", ");
        stringBuilder2.append(title_freqCount);
        stringBuilder2.append(", ");
        stringBuilder2.append(title_duration);
        stringBuilder2.append(", ");
        stringBuilder2.append(title_poor);
        stringBuilder2.append(", ");
        stringBuilder2.append(title_good);
        stringBuilder2.append(", ");
        stringBuilder2.append(title_dayCount);
        stringBuilder2.append(" FROM ");
        stringBuilder2.append(appName);
        stringBuilder2.append(" WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? GROUP BY NETWORKNAME");
        String sql = stringBuilder2.toString();
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, str, str2, this.modelVerAllAp, this.modelVerMainAp};
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("findAppQoEgroupBySpace:");
        stringBuilder2.append(sql);
        stringBuilder2.append(",FreqLoc:");
        stringBuilder2.append(this.curFreqLoc);
        stringBuilder2.append(", space_allAp:");
        stringBuilder2.append(str);
        stringBuilder2.append(", space_mainAp:");
        stringBuilder2.append(str2);
        stringBuilder2.append(", modelVer:");
        stringBuilder2.append(this.modelVerAllAp);
        stringBuilder2.append(Constant.RESULT_SEPERATE);
        stringBuilder2.append(this.modelVerMainAp);
        LogUtil.i(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("findAppQoEgroupBySpace, ScrbId:");
        stringBuilder2.append(this.ScrbId_print);
        LogUtil.v(stringBuilder2.toString());
        HashMap<String, List> results = new HashMap();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        String title_idCount2;
        String title_freqCount2;
        String title_duration2;
        String title_poor2;
        String str3;
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                int appGood;
                int appPoor;
                List appQoe = new ArrayList();
                int networktype = cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE"));
                int netIdCnt = cursor.getInt(cursor.getColumnIndexOrThrow(title_idCount));
                int netFreqCnt = cursor.getInt(cursor.getColumnIndexOrThrow(title_freqCount));
                title_idCount2 = title_idCount;
                try {
                    title_idCount = cursor.getInt(cursor.getColumnIndexOrThrow(title_duration));
                    title_freqCount2 = title_freqCount;
                    try {
                        appGood = cursor.getInt(cursor.getColumnIndexOrThrow(title_good));
                        title_duration2 = title_duration;
                        try {
                            appPoor = cursor.getInt(cursor.getColumnIndexOrThrow(title_poor));
                            title_poor2 = title_poor;
                        } catch (IllegalArgumentException e3) {
                            e = e3;
                            title_poor2 = title_poor;
                            str3 = title_good;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e4) {
                            e2 = e4;
                            title_poor2 = title_poor;
                            str3 = title_good;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoEgroupBySpace Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            if (cursor != null) {
                                cursor.close();
                            }
                            return results;
                        } catch (Throwable th2) {
                            th = th2;
                            title_poor2 = title_poor;
                            str3 = title_good;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    } catch (IllegalArgumentException e5) {
                        e = e5;
                        title_duration2 = title_duration;
                        title_poor2 = title_poor;
                        str3 = title_good;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e6) {
                        e2 = e6;
                        title_duration2 = title_duration;
                        title_poor2 = title_poor;
                        str3 = title_good;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoEgroupBySpace Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (cursor != null) {
                        }
                        return results;
                    } catch (Throwable th3) {
                        th = th3;
                        title_duration2 = title_duration;
                        title_poor2 = title_poor;
                        str3 = title_good;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                } catch (IllegalArgumentException e7) {
                    e = e7;
                    title_freqCount2 = title_freqCount;
                    title_duration2 = title_duration;
                    title_poor2 = title_poor;
                    str3 = title_good;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e8) {
                    e2 = e8;
                    title_freqCount2 = title_freqCount;
                    title_duration2 = title_duration;
                    title_poor2 = title_poor;
                    str3 = title_good;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoEgroupBySpace Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    if (cursor != null) {
                    }
                    return results;
                } catch (Throwable th4) {
                    th = th4;
                    title_freqCount2 = title_freqCount;
                    title_duration2 = title_duration;
                    title_poor2 = title_poor;
                    str3 = title_good;
                    if (cursor != null) {
                    }
                    throw th;
                }
                try {
                    int days = cursor.getInt(cursor.getColumnIndexOrThrow(title_dayCount));
                    str3 = title_good;
                    try {
                        appQoe.add(Integer.valueOf(networktype));
                        appQoe.add(Integer.valueOf(netIdCnt));
                        appQoe.add(Integer.valueOf(netFreqCnt));
                        appQoe.add(Integer.valueOf(title_idCount));
                        appQoe.add(Integer.valueOf(appGood));
                        appQoe.add(Integer.valueOf(appPoor));
                        appQoe.add(Integer.valueOf(0));
                        appQoe.add(Integer.valueOf(0));
                        appQoe.add(Integer.valueOf(days));
                        str = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKNAME"));
                        results.put(str, appQoe);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(" networkname:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",type:");
                        stringBuilder3.append(networktype);
                        stringBuilder3.append(", appDuration:");
                        stringBuilder3.append(title_idCount);
                        stringBuilder3.append(",appPoor:");
                        stringBuilder3.append(appPoor);
                        stringBuilder3.append(",appGood:");
                        stringBuilder3.append(appGood);
                        stringBuilder3.append(", modelVer:");
                        stringBuilder3.append(this.modelVerAllAp);
                        stringBuilder3.append(Constant.RESULT_SEPERATE);
                        stringBuilder3.append(this.modelVerMainAp);
                        LogUtil.i(stringBuilder3.toString());
                        title_idCount = title_idCount2;
                        title_freqCount = title_freqCount2;
                        title_duration = title_duration2;
                        title_poor = title_poor2;
                        title_good = str3;
                        str = spaceid_allAp;
                        str2 = spaceid_mainAp;
                    } catch (IllegalArgumentException e9) {
                        e = e9;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e10) {
                        e2 = e10;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoEgroupBySpace Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        if (cursor != null) {
                        }
                        return results;
                    }
                } catch (IllegalArgumentException e11) {
                    e = e11;
                    str3 = title_good;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e12) {
                    e2 = e12;
                    str3 = title_good;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoEgroupBySpace Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    if (cursor != null) {
                    }
                    return results;
                } catch (Throwable th5) {
                    th = th5;
                    str3 = title_good;
                    if (cursor != null) {
                    }
                    throw th;
                }
            }
            title_freqCount2 = title_freqCount;
            title_duration2 = title_duration;
            title_poor2 = title_poor;
            str3 = title_good;
        } catch (IllegalArgumentException e13) {
            e = e13;
            title_idCount2 = title_idCount;
            title_freqCount2 = title_freqCount;
            title_duration2 = title_duration;
            title_poor2 = title_poor;
            str3 = title_good;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAppQoEgroupBySpace IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e14) {
            e2 = e14;
            title_idCount2 = title_idCount;
            title_freqCount2 = title_freqCount;
            title_duration2 = title_duration;
            title_poor2 = title_poor;
            str3 = title_good;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAppQoEgroupBySpace Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
            }
            return results;
        } catch (Throwable th6) {
            th = th6;
            if (cursor != null) {
            }
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0113, code skipped:
            if (r7 != null) goto L_0x0115;
     */
    /* JADX WARNING: Missing block: B:19:0x0150, code skipped:
            if (r7 == null) goto L_0x0153;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<String, Bundle> findUserPrefByTwoSpaces(String spaceid_allAp, String spaceid_mainAp) {
        StringBuilder stringBuilder;
        String str = spaceid_allAp;
        String str2 = spaceid_mainAp;
        String sql = "SELECT NETWORKNAME, NW_TYPE, SUM(USER_PREF_OPT_IN), SUM(USER_PREF_OPT_OUT), SUM(USER_PREF_STAY), SUM(DURATION_CONNECTED) FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? GROUP BY NETWORKNAME";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("findUserPrefByTwoSpaces:");
        stringBuilder2.append(sql);
        stringBuilder2.append(",FreqLoc:");
        stringBuilder2.append(this.curFreqLoc);
        stringBuilder2.append(", space_allAp:");
        stringBuilder2.append(str);
        stringBuilder2.append(", space_mainAp:");
        stringBuilder2.append(str2);
        LogUtil.i(stringBuilder2.toString());
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, str, str2, this.modelVerAllAp, this.modelVerMainAp};
        HashMap<String, Bundle> results = new HashMap();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                int user_pref_opt_in = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_IN)"));
                int user_pref_opt_out = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_OUT)"));
                int user_pref_stay = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_STAY)"));
                String networkname = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKNAME"));
                int networktype = cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE"));
                long duration_connected = cursor.getLong(cursor.getColumnIndexOrThrow("SUM(DURATION_CONNECTED)"));
                Bundle statistics = new Bundle();
                statistics.putString("networkname", networkname);
                statistics.putInt("networktype", networktype);
                statistics.putInt("user_pref_opt_in", user_pref_opt_in);
                statistics.putInt("user_pref_opt_out", user_pref_opt_out);
                statistics.putInt("user_pref_stay", user_pref_stay);
                statistics.putLong("duration_connected", duration_connected);
                results.put(networkname, statistics);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" networkname:");
                stringBuilder3.append(networkname);
                stringBuilder3.append(",type:");
                stringBuilder3.append(networktype);
                stringBuilder3.append(",user_pref_opt_in:");
                stringBuilder3.append(user_pref_opt_in);
                stringBuilder3.append(",user_pref_opt_out:");
                stringBuilder3.append(user_pref_opt_out);
                stringBuilder3.append(",user_pref_stay:");
                stringBuilder3.append(user_pref_stay);
                stringBuilder3.append(",duration_connected:");
                stringBuilder3.append(duration_connected);
                LogUtil.i(stringBuilder3.toString());
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByTwoSpaces IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByTwoSpaces Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            return results;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x00f8, code skipped:
            if (r3 != null) goto L_0x00fa;
     */
    /* JADX WARNING: Missing block: B:19:0x0135, code skipped:
            if (r3 == null) goto L_0x0138;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<String, Bundle> findUserPrefByAllApSpaces(String spaceid_allAp) {
        StringBuilder stringBuilder;
        String sql = "SELECT NETWORKNAME, NW_TYPE, SUM(USER_PREF_OPT_IN), SUM(USER_PREF_OPT_OUT), SUM(USER_PREF_STAY), SUM(DURATION_CONNECTED) FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND MODEL_VER_ALLAP = ? GROUP BY NETWORKNAME";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("findUserPrefByTwoSpaces:");
        stringBuilder2.append(sql);
        stringBuilder2.append(",FreqLoc:");
        stringBuilder2.append(this.curFreqLoc);
        stringBuilder2.append(", space_allAp:");
        stringBuilder2.append(spaceid_allAp);
        LogUtil.i(stringBuilder2.toString());
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, this.modelVerAllAp};
        HashMap<String, Bundle> results = new HashMap();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                int user_pref_opt_in = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_IN)"));
                int user_pref_opt_out = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_OUT)"));
                int user_pref_stay = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_STAY)"));
                String networkname = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKNAME"));
                int networktype = cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE"));
                long duration_connected = cursor.getLong(cursor.getColumnIndexOrThrow("SUM(DURATION_CONNECTED)"));
                Bundle statistics = new Bundle();
                statistics.putString("networkname", networkname);
                statistics.putInt("networktype", networktype);
                statistics.putInt("user_pref_opt_in", user_pref_opt_in);
                statistics.putInt("user_pref_opt_out", user_pref_opt_out);
                statistics.putInt("user_pref_stay", user_pref_stay);
                statistics.putLong("duration_connected", duration_connected);
                results.put(networkname, statistics);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" networkname:");
                stringBuilder3.append(networkname);
                stringBuilder3.append(",type:");
                stringBuilder3.append(networktype);
                stringBuilder3.append(",user_pref_opt_in:");
                stringBuilder3.append(user_pref_opt_in);
                stringBuilder3.append(",user_pref_opt_out:");
                stringBuilder3.append(user_pref_opt_out);
                stringBuilder3.append(",user_pref_stay:");
                stringBuilder3.append(user_pref_stay);
                stringBuilder3.append(",duration_connected:");
                stringBuilder3.append(duration_connected);
                LogUtil.i(stringBuilder3.toString());
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByTwoSpaces IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByTwoSpaces Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            return results;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Bundle find4gCoverageByCurrLoc() {
        return queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND (SPACEID != ? OR SPACEIDMAIN != ?) AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND NETWORKNAME='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", new String[]{this.ScrbId, this.curFreqLoc, "0", "0", this.modelVerAllAp, this.modelVerMainAp});
    }

    public Bundle find4gCoverageByBothSpace(String spaceid_allAp, String spaceid_mainAp) {
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, spaceid_mainAp, this.modelVerAllAp, this.modelVerMainAp};
        Bundle coverage4G = queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND NETWORKNAME='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args);
        coverage4G.putLong("duration_out4g", queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND SPACEIDMAIN = ? AND MODEL_VER_ALLAP = ? AND MODEL_VER_MAINAP = ? AND NETWORKNAME!='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args).getLong("total_duration"));
        return coverage4G;
    }

    public Bundle find4gCoverageByAllApSpace(String spaceid_allAp) {
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_allAp, this.modelVerAllAp};
        Bundle coverage4G = queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND MODEL_VER_ALLAP = ? AND NETWORKNAME='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args);
        coverage4G.putLong("duration_out4g", queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND MODEL_VER_ALLAP = ? AND NETWORKNAME!='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args).getLong("total_duration"));
        return coverage4G;
    }

    public Bundle find4gCoverageByMainApSpace(String spaceid_mainAp) {
        String[] args = new String[]{this.ScrbId, this.curFreqLoc, spaceid_mainAp, this.modelVerMainAp};
        Bundle coverage4G = queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEIDMAIN = ? AND MODEL_VER_MAINAP = ? AND NETWORKNAME='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args);
        coverage4G.putLong("duration_out4g", queryCoverage("SELECT NW_TYPE, SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, COUNT(SIGNAL_VALUE) CELL_NUM, SUM(DURATION_CONNECTED) TOTAL_DURATION FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEIDMAIN = ? AND MODEL_VER_MAINAP = ? AND NETWORKNAME!='4G' AND NW_TYPE=0 GROUP BY NW_TYPE", args).getLong("total_duration"));
        return coverage4G;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:97:0x0340=Splitter:B:97:0x0340, B:103:0x0367=Splitter:B:103:0x0367} */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0388  */
    /* JADX WARNING: Missing block: B:99:0x0358, code skipped:
            if (r7 == null) goto L_0x0382;
     */
    /* JADX WARNING: Missing block: B:100:0x035a, code skipped:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:105:0x037f, code skipped:
            if (r7 == null) goto L_0x0382;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<Integer, Bundle> findAppQoERecByFreqLoc(String freqlocation) {
        IllegalArgumentException e;
        StringBuilder stringBuilder;
        Exception e2;
        Throwable th;
        String str = freqlocation;
        String sql = "";
        int count = 0;
        HashMap<Integer, Float> mAppMap = mQoeAppBuilder.getQoEAppList();
        HashMap<Integer, Bundle> results = new HashMap();
        ArrayList<String> argList = new ArrayList();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        String appTableName;
        for (Entry<Integer, Float> entry : mAppMap.entrySet()) {
            int appNum = ((Integer) entry.getKey()).intValue();
            appTableName = new StringBuilder();
            appTableName.append(Constant.USERDB_APP_NAME_PREFIX);
            appTableName.append(appNum);
            appTableName = appTableName.toString();
            StringBuilder stringBuilder2;
            if (count == 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SELECT ");
                stringBuilder2.append(appNum);
                stringBuilder2.append(" AS NAME, SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, COUNT(DISTINCT NETWORKID) NWIDCNT, NETWORKNAME, COUNT(DISTINCT NETWORKFREQ) NWFREQCNT, NW_TYPE, COUNT(DISTINCT UPDATE_DATE) REC, SUM(DURATION), SUM(POORCOUNT), SUM(GOODCOUNT) FROM ");
                stringBuilder2.append(appTableName);
                stringBuilder2.append(" WHERE SCRBID = ? AND FREQLOCNAME = ? AND (SPACEID != 0 OR SPACEIDMAIN != 0) GROUP BY NETWORKNAME, SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, NW_TYPE, NAME");
                sql = stringBuilder2.toString();
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(sql);
                stringBuilder2.append(" UNION SELECT ");
                stringBuilder2.append(appNum);
                stringBuilder2.append(" AS NAME, SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, COUNT(DISTINCT NETWORKID) NWIDCNT, NETWORKNAME, COUNT(DISTINCT NETWORKFREQ) NWFREQCNT, NW_TYPE, COUNT(DISTINCT UPDATE_DATE) REC, SUM(DURATION), SUM(POORCOUNT), SUM(GOODCOUNT) FROM ");
                stringBuilder2.append(appTableName);
                stringBuilder2.append(" WHERE SCRBID = ? AND FREQLOCNAME = ? AND (SPACEID != 0 OR SPACEIDMAIN != 0) GROUP BY NETWORKNAME, SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, NW_TYPE, NAME");
                sql = stringBuilder2.toString();
            }
            count++;
            argList.add(this.ScrbId);
            argList.add(str);
        }
        String sql2 = new StringBuilder();
        sql2.append(sql);
        sql2.append(" ORDER BY SUM(DURATION) DESC");
        sql2 = sql2.toString();
        String[] args = (String[]) argList.toArray(new String[argList.size()]);
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("findAppQoERecByFreqLoc:");
        stringBuilder3.append(sql2);
        stringBuilder3.append(",FreqLoc:");
        stringBuilder3.append(str);
        stringBuilder3.append(",args: ");
        stringBuilder3.append(Arrays.toString(args));
        LogUtil.i(stringBuilder3.toString());
        count = 0;
        HashMap<Integer, Float> mAppMap2;
        int i;
        ArrayList<String> arrayList;
        String str2;
        String[] strArr;
        try {
            Cursor cursor2;
            Cursor cursor3;
            cursor = this.db.rawQuery(sql2, args);
            while (cursor.moveToNext()) {
                String modelVerAllap;
                String modelVerMainap;
                int nwidcnt;
                String networkname;
                short nwfreqcnt;
                int nwType;
                short rec;
                int app;
                long duration;
                int poorcount;
                int goodcount;
                Bundle statistics;
                try {
                    count++;
                    try {
                        sql = cursor.getString(cursor.getColumnIndexOrThrow("SPACEID"));
                        modelVerAllap = cursor.getString(cursor.getColumnIndexOrThrow("MODEL_VER_ALLAP"));
                        appTableName = cursor.getString(cursor.getColumnIndexOrThrow("SPACEIDMAIN"));
                        modelVerMainap = cursor.getString(cursor.getColumnIndexOrThrow("MODEL_VER_MAINAP"));
                        nwidcnt = cursor.getInt(cursor.getColumnIndexOrThrow("NWIDCNT"));
                        networkname = cursor.getString(cursor.getColumnIndexOrThrow("NETWORKNAME"));
                        nwfreqcnt = cursor.getShort(cursor.getColumnIndexOrThrow("NWFREQCNT"));
                        nwType = cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE"));
                        rec = cursor.getShort(cursor.getColumnIndexOrThrow("REC"));
                        mAppMap2 = mAppMap;
                    } catch (IllegalArgumentException e3) {
                        e = e3;
                        i = count;
                        mAppMap2 = mAppMap;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e4) {
                        e2 = e4;
                        i = count;
                        mAppMap2 = mAppMap;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        try {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            i = count;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = count;
                        mAppMap2 = mAppMap;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                    try {
                        app = cursor.getInt(cursor.getColumnIndexOrThrow("NAME"));
                        arrayList = argList;
                    } catch (IllegalArgumentException e5) {
                        e = e5;
                        i = count;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e6) {
                        e2 = e6;
                        i = count;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Throwable th4) {
                        th = th4;
                        i = count;
                        arrayList = argList;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                    try {
                        duration = cursor.getLong(cursor.getColumnIndexOrThrow("SUM(DURATION)"));
                        poorcount = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(POORCOUNT)"));
                        str2 = sql2;
                        try {
                            goodcount = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(GOODCOUNT)"));
                            strArr = args;
                        } catch (IllegalArgumentException e7) {
                            e = e7;
                            i = count;
                            cursor2 = cursor;
                            strArr = args;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e8) {
                            e2 = e8;
                            i = count;
                            cursor2 = cursor;
                            strArr = args;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Throwable th5) {
                            th = th5;
                            i = count;
                            cursor2 = cursor;
                            strArr = args;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                        try {
                            statistics = new Bundle();
                            cursor2 = cursor;
                        } catch (IllegalArgumentException e9) {
                            e = e9;
                            i = count;
                            cursor2 = cursor;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e10) {
                            e2 = e10;
                            i = count;
                            cursor2 = cursor;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Throwable th6) {
                            th = th6;
                            i = count;
                            cursor2 = cursor;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                    } catch (IllegalArgumentException e11) {
                        e = e11;
                        i = count;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e12) {
                        e2 = e12;
                        i = count;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Throwable th7) {
                        th = th7;
                        i = count;
                        cursor2 = cursor;
                        str2 = sql2;
                        strArr = args;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                } catch (IllegalArgumentException e13) {
                    e = e13;
                    mAppMap2 = mAppMap;
                    arrayList = argList;
                    cursor3 = cursor;
                    str2 = sql2;
                    strArr = args;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e14) {
                    e2 = e14;
                    mAppMap2 = mAppMap;
                    arrayList = argList;
                    cursor3 = cursor;
                    str2 = sql2;
                    strArr = args;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Throwable th8) {
                    th = th8;
                    mAppMap2 = mAppMap;
                    arrayList = argList;
                    cursor3 = cursor;
                    str2 = sql2;
                    strArr = args;
                    i = count;
                    if (cursor != null) {
                    }
                    throw th;
                }
                try {
                    statistics.putString("spaceid", sql);
                    statistics.putString("modelVerAllap", modelVerAllap);
                    statistics.putString("spaceidmain", appTableName);
                    statistics.putString("modelVerMainap", modelVerMainap);
                    statistics.putInt("nwidcnt", nwidcnt);
                    statistics.putString("networkname", networkname);
                    statistics.putShort("nwfreqcnt", nwfreqcnt);
                    statistics.putInt("nwType", nwType);
                    statistics.putShort("rec", rec);
                    long duration2 = duration;
                    statistics.putLong("duration", duration2);
                    statistics.putInt("app", app);
                    statistics.putInt("poorcount", poorcount);
                    statistics.putInt("goodcount", goodcount);
                    results.put(Integer.valueOf(count), statistics);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    i = count;
                    try {
                        stringBuilder4.append(" networkname:");
                        stringBuilder4.append(networkname);
                        stringBuilder4.append(",app:");
                        stringBuilder4.append(app);
                        stringBuilder4.append(",poorcount:");
                        stringBuilder4.append(poorcount);
                        stringBuilder4.append(",goodcount:");
                        stringBuilder4.append(goodcount);
                        stringBuilder4.append(",duration:");
                        stringBuilder4.append(duration2);
                        stringBuilder4.append(",spaceid:");
                        stringBuilder4.append(sql);
                        LogUtil.i(stringBuilder4.toString());
                        mAppMap = mAppMap2;
                        argList = arrayList;
                        sql2 = str2;
                        args = strArr;
                        cursor = cursor2;
                        count = i;
                        str = freqlocation;
                    } catch (IllegalArgumentException e15) {
                        e = e15;
                        cursor = cursor2;
                        count = i;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e16) {
                        e2 = e16;
                        cursor = cursor2;
                        count = i;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Throwable th9) {
                        th = th9;
                        cursor = cursor2;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                } catch (IllegalArgumentException e17) {
                    e = e17;
                    i = count;
                    cursor = cursor2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e18) {
                    e2 = e18;
                    i = count;
                    cursor = cursor2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Throwable th10) {
                    th = th10;
                    i = count;
                    cursor = cursor2;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            arrayList = argList;
            cursor2 = cursor;
            str2 = sql2;
            strArr = args;
            if (cursor2 != null) {
                cursor3 = cursor2;
                cursor3.close();
            } else {
                cursor3 = cursor2;
            }
            cursor = cursor3;
        } catch (IllegalArgumentException e19) {
            e = e19;
            mAppMap2 = mAppMap;
            arrayList = argList;
            str2 = sql2;
            strArr = args;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAppQoERecByFreqLoc IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e20) {
            e2 = e20;
            mAppMap2 = mAppMap;
            arrayList = argList;
            str2 = sql2;
            strArr = args;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findAppQoERecByFreqLoc Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th11) {
            th = th11;
            mAppMap2 = mAppMap;
            arrayList = argList;
            str2 = sql2;
            strArr = args;
            i = 0;
            if (cursor != null) {
            }
            throw th;
        }
        return results;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:79:0x0288=Splitter:B:79:0x0288, B:85:0x02ac=Splitter:B:85:0x02ac} */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02cb  */
    /* JADX WARNING: Missing block: B:81:0x02a0, code skipped:
            if (r7 != null) goto L_0x02a2;
     */
    /* JADX WARNING: Missing block: B:82:0x02a2, code skipped:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:87:0x02c4, code skipped:
            if (r7 != null) goto L_0x02a2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HashMap<Integer, Bundle> findUserExpRecByFreqLoc(String freqlocation) {
        IllegalArgumentException e;
        Cursor cursor;
        StringBuilder stringBuilder;
        Exception e2;
        Throwable th;
        String str = freqlocation;
        String sql = "SELECT SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, COUNT(DISTINCT NETWORKID) NWIDCNT, NETWORKNAME, COUNT(DISTINCT NETWORKFREQ) NWFREQCNT, NW_TYPE, COUNT(DISTINCT UPDATE_DATE) REC,SUM(DURATION_CONNECTED), SUM(DATA_RX), SUM(DATA_TX), SUM(SIGNAL_VALUE*DURATION_CONNECTED)/SUM(DURATION_CONNECTED) AVG_SIGNAL, SUM(USER_PREF_OPT_IN), SUM(USER_PREF_OPT_OUT), SUM(USER_PREF_STAY), SUM(POWER_CONSUMPTION) FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND (SPACEID != 0 OR SPACEIDMAIN != 0) GROUP BY NETWORKNAME, SPACEID, MODEL_VER_ALLAP, SPACEIDMAIN, MODEL_VER_MAINAP, NW_TYPE ORDER BY SUM(DURATION_CONNECTED) DESC";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("findUserPrefByFreqLoc:");
        stringBuilder2.append(sql);
        stringBuilder2.append(",FreqLoc:");
        stringBuilder2.append(str);
        LogUtil.i(stringBuilder2.toString());
        String[] args = new String[]{this.ScrbId, str};
        int count = 0;
        HashMap<Integer, Bundle> results = new HashMap();
        Cursor cursor2 = null;
        if (this.db == null) {
            return results;
        }
        HashMap<Integer, Bundle> results2;
        String sql2;
        String[] args2;
        try {
            Cursor cursor3;
            cursor2 = this.db.rawQuery(sql, args);
            while (cursor2.moveToNext()) {
                String networkname;
                int nwType;
                int user_pref_opt_in;
                int user_pref_opt_out;
                int user_pref_stay;
                Bundle statistics;
                long duration_connected;
                try {
                    String spaceid;
                    String modelVerAllap;
                    String spaceidmain;
                    String modelVerMainap;
                    int nwidcnt;
                    short nwfreqcnt;
                    short rec;
                    long duration_connected2;
                    long datarx;
                    long datatx;
                    short avgSignal;
                    HashMap<Integer, Bundle> results3;
                    count++;
                    try {
                        spaceid = cursor2.getString(cursor2.getColumnIndexOrThrow("SPACEID"));
                        modelVerAllap = cursor2.getString(cursor2.getColumnIndexOrThrow("MODEL_VER_ALLAP"));
                        spaceidmain = cursor2.getString(cursor2.getColumnIndexOrThrow("SPACEIDMAIN"));
                        modelVerMainap = cursor2.getString(cursor2.getColumnIndexOrThrow("MODEL_VER_MAINAP"));
                        nwidcnt = cursor2.getInt(cursor2.getColumnIndexOrThrow("NWIDCNT"));
                        networkname = cursor2.getString(cursor2.getColumnIndexOrThrow("NETWORKNAME"));
                        nwfreqcnt = cursor2.getShort(cursor2.getColumnIndexOrThrow("NWFREQCNT"));
                        nwType = cursor2.getInt(cursor2.getColumnIndexOrThrow("NW_TYPE"));
                        rec = cursor2.getShort(cursor2.getColumnIndexOrThrow("REC"));
                        duration_connected2 = cursor2.getLong(cursor2.getColumnIndexOrThrow("SUM(DURATION_CONNECTED)"));
                        datarx = cursor2.getLong(cursor2.getColumnIndexOrThrow("SUM(DATA_RX)"));
                        datatx = cursor2.getLong(cursor2.getColumnIndexOrThrow("SUM(DATA_TX)"));
                        avgSignal = cursor2.getShort(cursor2.getColumnIndexOrThrow("AVG_SIGNAL"));
                        user_pref_opt_in = cursor2.getInt(cursor2.getColumnIndexOrThrow("SUM(USER_PREF_OPT_IN)"));
                        sql2 = sql;
                        try {
                            user_pref_opt_out = cursor2.getInt(cursor2.getColumnIndexOrThrow("SUM(USER_PREF_OPT_OUT)"));
                            args2 = args;
                        } catch (IllegalArgumentException e3) {
                            e = e3;
                            args2 = args;
                            results2 = results;
                            cursor = cursor2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e4) {
                            e2 = e4;
                            args2 = args;
                            results2 = results;
                            cursor = cursor2;
                            try {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                                stringBuilder.append(e2.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (Throwable th2) {
                                th = th2;
                                if (cursor2 != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            args2 = args;
                            results2 = results;
                            cursor = cursor2;
                            if (cursor2 != null) {
                            }
                            throw th;
                        }
                        try {
                            user_pref_stay = cursor2.getInt(cursor2.getColumnIndexOrThrow("SUM(USER_PREF_STAY)"));
                            results3 = results;
                        } catch (IllegalArgumentException e5) {
                            e = e5;
                            results2 = results;
                            cursor = cursor2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e6) {
                            e2 = e6;
                            results2 = results;
                            cursor = cursor2;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Throwable th4) {
                            th = th4;
                            results2 = results;
                            cursor = cursor2;
                            if (cursor2 != null) {
                            }
                            throw th;
                        }
                    } catch (IllegalArgumentException e7) {
                        e = e7;
                        sql2 = sql;
                        args2 = args;
                        results2 = results;
                        cursor = cursor2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e8) {
                        e2 = e8;
                        sql2 = sql;
                        args2 = args;
                        results2 = results;
                        cursor = cursor2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Throwable th5) {
                        th = th5;
                        sql2 = sql;
                        args2 = args;
                        results2 = results;
                        cursor = cursor2;
                        if (cursor2 != null) {
                        }
                        throw th;
                    }
                    try {
                        long powerconsumption = cursor2.getLong(cursor2.getColumnIndexOrThrow("SUM(POWER_CONSUMPTION)"));
                        statistics = new Bundle();
                        cursor = cursor2;
                        try {
                            statistics.putString("spaceid", spaceid);
                            statistics.putString("modelVerAllap", modelVerAllap);
                            statistics.putString("spaceidmain", spaceidmain);
                            statistics.putString("modelVerMainap", modelVerMainap);
                            statistics.putInt("nwidcnt", nwidcnt);
                            statistics.putString("networkname", networkname);
                            statistics.putShort("nwfreqcnt", nwfreqcnt);
                            statistics.putInt("nwType", nwType);
                            statistics.putShort("rec", rec);
                            duration_connected = duration_connected2;
                            statistics.putLong("duration_connected", duration_connected);
                            long datarx2 = datarx;
                            statistics.putLong("datarx", datarx2);
                            statistics.putLong("datatx", datatx);
                            statistics.putShort("avgSignal", avgSignal);
                            statistics.putInt("user_pref_opt_in", user_pref_opt_in);
                            statistics.putInt("user_pref_opt_out", user_pref_opt_out);
                            statistics.putInt("user_pref_stay", user_pref_stay);
                            long powerconsumption2 = powerconsumption;
                            statistics.putLong("powerconsumption", powerconsumption2);
                            results2 = results3;
                        } catch (IllegalArgumentException e9) {
                            e = e9;
                            results2 = results3;
                            cursor2 = cursor;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e10) {
                            e2 = e10;
                            results2 = results3;
                            cursor2 = cursor;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Throwable th6) {
                            th = th6;
                            results2 = results3;
                            cursor2 = cursor;
                            if (cursor2 != null) {
                            }
                            throw th;
                        }
                    } catch (IllegalArgumentException e11) {
                        e = e11;
                        cursor = cursor2;
                        results2 = results3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e12) {
                        e2 = e12;
                        cursor = cursor2;
                        results2 = results3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Throwable th7) {
                        th = th7;
                        cursor = cursor2;
                        results2 = results3;
                        if (cursor2 != null) {
                        }
                        throw th;
                    }
                } catch (IllegalArgumentException e13) {
                    e = e13;
                    sql2 = sql;
                    args2 = args;
                    results2 = results;
                    cursor3 = cursor2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e14) {
                    e2 = e14;
                    sql2 = sql;
                    args2 = args;
                    results2 = results;
                    cursor3 = cursor2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Throwable th8) {
                    th = th8;
                    sql2 = sql;
                    args2 = args;
                    results2 = results;
                    cursor3 = cursor2;
                    if (cursor2 != null) {
                    }
                    throw th;
                }
                try {
                    results2.put(Integer.valueOf(count), statistics);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" networkname:");
                    stringBuilder2.append(networkname);
                    stringBuilder2.append(",type:");
                    stringBuilder2.append(nwType);
                    stringBuilder2.append(",user_pref_opt_in:");
                    stringBuilder2.append(user_pref_opt_in);
                    stringBuilder2.append(",user_pref_opt_out:");
                    stringBuilder2.append(user_pref_opt_out);
                    stringBuilder2.append(",user_pref_stay:");
                    stringBuilder2.append(user_pref_stay);
                    stringBuilder2.append(",duration_connected:");
                    stringBuilder2.append(duration_connected);
                    LogUtil.i(stringBuilder2.toString());
                    results = results2;
                    sql = sql2;
                    args = args2;
                    cursor2 = cursor;
                    str = freqlocation;
                } catch (IllegalArgumentException e15) {
                    e = e15;
                    cursor2 = cursor;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception e16) {
                    e2 = e16;
                    cursor2 = cursor;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("findUserPrefByFreqLoc Exception: ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Throwable th9) {
                    th = th9;
                    cursor2 = cursor;
                    if (cursor2 != null) {
                    }
                    throw th;
                }
            }
            args2 = args;
            results2 = results;
            cursor = cursor2;
            if (cursor != null) {
                cursor3 = cursor;
                cursor3.close();
            } else {
                cursor3 = cursor;
            }
            cursor2 = cursor3;
        } catch (IllegalArgumentException e17) {
            e = e17;
            sql2 = sql;
            args2 = args;
            results2 = results;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByFreqLoc IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e18) {
            e2 = e18;
            sql2 = sql;
            args2 = args;
            results2 = results;
            stringBuilder = new StringBuilder();
            stringBuilder.append("findUserPrefByFreqLoc Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th10) {
            th = th10;
            sql2 = sql;
            args2 = args;
            results2 = results;
            if (cursor2 != null) {
                cursor2.close();
            }
            throw th;
        }
        return results2;
    }

    /* JADX WARNING: Missing block: B:9:0x00d6, code skipped:
            if (r8 != null) goto L_0x00d8;
     */
    /* JADX WARNING: Missing block: B:19:0x0113, code skipped:
            if (r8 == null) goto L_0x0116;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Bundle getUerPrefTotalCountDurationByAllApSpaces(String freqlocation, String spaceid, String modelVerAllAp) {
        String str = freqlocation;
        String sql = "SELECT SUM(DURATION_CONNECTED), SUM(USER_PREF_OPT_IN), SUM(USER_PREF_OPT_OUT), SUM(USER_PREF_STAY) FROM SPACEUSER_BASE WHERE SCRBID = ? AND FREQLOCNAME = ? AND SPACEID = ? AND MODEL_VER_ALLAP = ? GROUP BY SPACEID, MODEL_VER_ALLAP";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getUerPrefTotalCountDurationByAllApSpaces:");
        stringBuilder.append(sql);
        stringBuilder.append(",FreqLoc:");
        stringBuilder.append(str);
        LogUtil.i(stringBuilder.toString());
        String[] args = new String[]{this.ScrbId, str, spaceid, modelVerAllAp};
        int count = 0;
        Bundle results = new Bundle();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        StringBuilder stringBuilder2;
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                count++;
                long duration_connected = cursor.getLong(cursor.getColumnIndexOrThrow("SUM(DURATION_CONNECTED)"));
                int user_pref_opt_in = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_IN)"));
                int user_pref_opt_out = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_OPT_OUT)"));
                int user_pref_stay = cursor.getInt(cursor.getColumnIndexOrThrow("SUM(USER_PREF_STAY)"));
                int totalCount = Math.round(((float) (user_pref_opt_in + user_pref_opt_out)) / 2.0f) + user_pref_stay;
                results.putInt("totalDuration", ((int) duration_connected) / 1000);
                results.putInt("totalCount", totalCount);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" totalDuration:");
                stringBuilder2.append(((int) duration_connected) / 1000);
                stringBuilder2.append(",totalCount:");
                stringBuilder2.append(totalCount);
                stringBuilder2.append(",user_pref_opt_in:");
                stringBuilder2.append(user_pref_opt_in);
                stringBuilder2.append(",user_pref_opt_out:");
                stringBuilder2.append(user_pref_opt_out);
                stringBuilder2.append(",user_pref_stay:");
                stringBuilder2.append(user_pref_stay);
                stringBuilder2.append(",duration_connected:");
                stringBuilder2.append(duration_connected);
                LogUtil.i(stringBuilder2.toString());
            }
        } catch (IllegalArgumentException e) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getUerPrefTotalCountDurationByAllApSpaces IllegalArgumentException: ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        } catch (Exception e2) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getUerPrefTotalCountDurationByAllApSpaces Exception: ");
            stringBuilder2.append(e2.getMessage());
            LogUtil.e(stringBuilder2.toString());
            if (cursor != null) {
                cursor.close();
            }
            return results;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x00a8, code skipped:
            if (r1 != null) goto L_0x00aa;
     */
    /* JADX WARNING: Missing block: B:19:0x00e5, code skipped:
            if (r1 == null) goto L_0x00e8;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Bundle queryCoverage(String sql, String[] args) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("query4gCoverage: aql=");
        stringBuilder2.append(sql);
        stringBuilder2.append(" ,args=");
        stringBuilder2.append(Arrays.toString(Arrays.copyOfRange(args, 1, args.length)));
        LogUtil.i(stringBuilder2.toString());
        Bundle results = new Bundle();
        Cursor cursor = null;
        if (this.db == null) {
            return results;
        }
        try {
            cursor = this.db.rawQuery(sql, args);
            while (cursor.moveToNext()) {
                int cellNum = cursor.getInt(cursor.getColumnIndexOrThrow("CELL_NUM"));
                int avgSignal = cursor.getInt(cursor.getColumnIndexOrThrow("AVG_SIGNAL"));
                long totalDuration = cursor.getLong(cursor.getColumnIndexOrThrow("TOTAL_DURATION"));
                int networktype = cursor.getInt(cursor.getColumnIndexOrThrow("NW_TYPE"));
                results.putInt("networktype", networktype);
                results.putInt("cell_num", cellNum);
                results.putInt("avg_signal", avgSignal);
                results.putLong("total_duration", totalDuration);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" type:");
                stringBuilder3.append(networktype);
                stringBuilder3.append(",cell_num:");
                stringBuilder3.append(cellNum);
                stringBuilder3.append(",avg_signal:");
                stringBuilder3.append(avgSignal);
                stringBuilder3.append(",total_duration:");
                stringBuilder3.append(totalDuration);
                LogUtil.i(stringBuilder3.toString());
            }
        } catch (IllegalArgumentException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("query4gCoverage IllegalArgumentException: ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("query4gCoverage Exception: ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
            if (cursor != null) {
                cursor.close();
            }
            return results;
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

package com.android.server.hidata.wavemapping.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ContextManager;
import com.android.server.hidata.wavemapping.service.HwHistoryQoEResourceBuilder;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.Map.Entry;

public class DatabaseSingleton extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    private static final String TAG;
    private static DatabaseSingleton mDataBaseOpenHelper = null;
    private static HwHistoryQoEResourceBuilder mQoeAppBuilder = null;
    private String CREATE_APP_TABLE_FRONT = "CREATE TABLE IF NOT EXISTS ";
    private String CREATE_APP_TABLE_TITLE = " (SCRBID TEXT, UPDATE_DATE DATE DEFAULT (date('now', 'localtime')), FREQLOCNAME TEXT, SPACEID TEXT, SPACEIDMAIN TEXT, NETWORKNAME TEXT, NETWORKID TEXT, NETWORKFREQ TEXT, NW_TYPE INTEGER, DURATION INTEGER, POORCOUNT INTEGER, GOODCOUNT INTEGER, MODEL_VER_ALLAP TEXT, MODEL_VER_MAINAP TEXT)";
    private String CREATE_BEHAVIOR_TABLE = "CREATE TABLE IF NOT EXISTS BEHAVIOR_MAINTAIN (UPDATETIME TEXT, BATCH INTEGER)";
    private String CREATE_CHR_HISTQOERPT = "CREATE TABLE IF NOT EXISTS CHR_HISTQOERPT(FREQLOCNAME TEXT, QUERYCNT INTEGER, GOODCNT INTEGER, POORCNT INTEGER, DATARX INTEGER, DATATX INTEGER, UNKNOWNDB INTEGER, UNKNOWNSPACE INTEGER)";
    private String CREATE_CHR_LOCATION_TABLE = "CREATE TABLE IF NOT EXISTS CHR_FREQUENT_LOCATION (FREQLOCATION TEXT, FIRSTREPORT INTEGER DEFAULT 0, ENTERY INTEGER DEFAULT 0, LEAVE INTEGER DEFAULT 0, DURATION INTEGER DEFAULT 0, SPACECHANGE INTEGER DEFAULT 0, SPACELEAVE INTEGER DEFAULT 0, UPTOTALSWITCH INTEGER DEFAULT 0, UPAUTOSUCC INTEGER DEFAULT 0, UPMANUALSUCC INTEGER DEFAULT 0, UPAUTOFAIL INTEGER DEFAULT 0, UPNOSWITCHFAIL INTEGER DEFAULT 0,UPQRYCNT INTEGER DEFAULT 0, UPRESCNT INTEGER DEFAULT 0,UPUNKNOWNDB INTEGER DEFAULT 0,UPUNKNOWNSPACE INTEGER DEFAULT 0, LPTOTALSWITCH INTEGER DEFAULT 0, LPDATARX INTEGER DEFAULT 0, LPDATATX INTEGER DEFAULT 0, LPDURATION INTEGER DEFAULT 0, LPOFFSET INTEGER DEFAULT 0, LPALREADYBEST INTEGER DEFAULT 0, LPNOTREACH INTEGER DEFAULT 0, LPBACK INTEGER DEFAULT 0, LPUNKNOWNDB INTEGER DEFAULT 0, LPUNKNOWNSPACE INTEGER DEFAULT 0)";
    private String CREATE_ENTERPRISE_AP_TABLE = "CREATE TABLE IF NOT EXISTS ENTERPRISE_AP (SSID TEXT,MAC TEXT,UPTIME TEXT)";
    private String CREATE_IDENTIFY_RESULT_TABLE = "CREATE TABLE IF NOT EXISTS IDENTIFY_RESULT (SSID TEXT,PRELABLE INTEGER,SERVERMAC TEXT,UPTIME TEXT,ISMAINAP BOOLEAN,MODELNAME TEXT)";
    private String CREATE_LOCATION_TABLE = "CREATE TABLE IF NOT EXISTS FREQUENT_LOCATION (OOBTIME INTEGER DEFAULT 0, UPDATETIME INTEGER DEFAULT 0, CHRBENEFITUPLOADTIME INTEGER DEFAULT 0, CHRSPACEUSERUPLOADTIME INTEGER DEFAULT 0, FREQUENTLOCATION TEXT)";
    private String CREATE_MOBILE_AP_TABLE = "CREATE TABLE IF NOT EXISTS MOBILE_AP (SSID TEXT,MAC TEXT, UPTIME TEXT,SRCTYPE INTEGER)";
    private String CREATE_REGULAR_PLACESTATE_TABLE = "CREATE TABLE IF NOT EXISTS RGL_PLACESTATE (SSID TEXT, STATE INTEGER,BATCH INTEGER,FINGERNUM INTEGER,TEST_DAT_NUM INTEGER,DISNUM INTEGER, UPTIME TEXT,IDENTIFYNUM INTEGER,NO_OCURBSSIDS TEXT,ISMAINAP BOOLEAN,MODELNAME TEXT,BEGINTIME INTEGER)";
    private String CREATE_SPACE_TABLE_NEW = "CREATE TABLE IF NOT EXISTS SPACEUSER_BASE (SCRBID TEXT, UPDATE_DATE DATE DEFAULT (date('now', 'localtime')), FREQLOCNAME TEXT, SPACEID TEXT, SPACEIDMAIN TEXT, NETWORKNAME TEXT, NETWORKID TEXT, NETWORKFREQ TEXT, NW_TYPE INTEGER, USER_PREF_OPT_IN INTEGER, USER_PREF_OPT_OUT INTEGER, USER_PREF_STAY INTEGER, USER_PREF_TOTAL_COUNT INTEGER, POWER_CONSUMPTION INTEGER, DATA_RX INTEGER, DATA_TX INTEGER, SIGNAL_VALUE INTEGER, DURATION_CONNECTED INTEGER, MODEL_VER_ALLAP TEXT, MODEL_VER_MAINAP TEXT)";
    private String CREATE_STA_BACK2LTE = "CREATE TABLE IF NOT EXISTS CHR_FASTBACK2LTE(FREQLOCNAME TEXT, LOWRATCNT INTEGER, INLTECNT INTEGER, OUTLTECNT INTEGER, FASTBACK INTEGER, SUCCESSBACK INTEGER, CELLS4G INTEGER, REFCNT INTEGER, UNKNOWNDB INTEGER, UNKNOWNSPACE INTEGER)";

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(DatabaseSingleton.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private DatabaseSingleton(Context context) {
        super(context, Constant.getDbPath(), null, 2);
        mQoeAppBuilder = HwHistoryQoEResourceBuilder.getInstance();
    }

    public static synchronized SQLiteDatabase getInstance() {
        SQLiteDatabase writableDatabase;
        synchronized (DatabaseSingleton.class) {
            try {
                LogUtil.i("SQLiteDatabase getInstance begin.");
                if (mDataBaseOpenHelper == null) {
                    LogUtil.d("SQLiteDatabase getInstance ,mDataBaseOpenHelper == null");
                    Context context = ContextManager.getInstance().getContext();
                    if (context == null) {
                        LogUtil.d(" context is null ");
                    }
                    Constant.checkPath(context);
                    mDataBaseOpenHelper = new DatabaseSingleton(context);
                    LogUtil.i(" DatabaseSingleton init db sucess");
                }
                writableDatabase = mDataBaseOpenHelper.getWritableDatabase();
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" Exception SQLiteDatabase getInstance: ");
                stringBuilder.append(e);
                stringBuilder.append(" cons.DATABASE_FILE_PATH :");
                stringBuilder.append(Constant.getDbPath());
                LogUtil.e(stringBuilder.toString());
                return null;
            }
        }
        return writableDatabase;
    }

    public void onCreate(SQLiteDatabase db) {
        StringBuilder stringBuilder;
        try {
            db.beginTransaction();
            String sql = this.CREATE_REGULAR_PLACESTATE_TABLE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql01=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            db.execSQL(this.CREATE_ENTERPRISE_AP_TABLE);
            db.execSQL(this.CREATE_MOBILE_AP_TABLE);
            sql = this.CREATE_IDENTIFY_RESULT_TABLE;
            db.execSQL(sql);
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql02=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            sql = this.CREATE_BEHAVIOR_TABLE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql04=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            sql = this.CREATE_SPACE_TABLE_NEW;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql05=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            sql = this.CREATE_LOCATION_TABLE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql06=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            sql = this.CREATE_STA_BACK2LTE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql07=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            sql = this.CREATE_CHR_LOCATION_TABLE;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql08=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            sql = this.CREATE_CHR_HISTQOERPT;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onCreate,sql09=");
            stringBuilder.append(sql);
            LogUtil.i(stringBuilder.toString());
            db.execSQL(sql);
            for (Entry<Integer, Float> entry : mQoeAppBuilder.getQoEAppList().entrySet()) {
                String tableName = new StringBuilder();
                tableName.append(Constant.USERDB_APP_NAME_PREFIX);
                tableName.append(entry.getKey());
                tableName = tableName.toString();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.CREATE_APP_TABLE_FRONT);
                stringBuilder2.append(tableName);
                stringBuilder2.append(this.CREATE_APP_TABLE_TITLE);
                sql = stringBuilder2.toString();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onCreate,app_sql=");
                stringBuilder2.append(sql);
                LogUtil.i(stringBuilder2.toString());
                db.execSQL(sql);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" Exception onCreate: ");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
        } catch (Throwable th) {
            db.endTransaction();
        }
        db.endTransaction();
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUpgrade: ");
        stringBuilder.append(oldVersion);
        stringBuilder.append(" to ");
        stringBuilder.append(newVersion);
        LogUtil.i(stringBuilder.toString());
        try {
            db.execSQL("DROP TABLE IF EXISTS RGL_PLACESTATE");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS ENTERPRISE_AP");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS MOBILE_AP");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS IDENTIFY_RESULT");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS SPACEUSER_BASE");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS BEHAVIOR_MAINTAIN");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS FREQUENT_LOCATION");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_FREQUENT_LOCATION");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_HISTQOERPT");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_FASTBACK2LTE");
            onCreate(db);
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" Exception onUpgrade: ");
            stringBuilder2.append(e);
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUpgrade: ");
        stringBuilder.append(oldVersion);
        stringBuilder.append(" to ");
        stringBuilder.append(newVersion);
        LogUtil.i(stringBuilder.toString());
        try {
            db.execSQL("DROP TABLE IF EXISTS RGL_PLACESTATE");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS ENTERPRISE_AP");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS MOBILE_AP");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS IDENTIFY_RESULT");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS SPACEUSER_BASE");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS BEHAVIOR_MAINTAIN");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS FREQUENT_LOCATION");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_FREQUENT_LOCATION");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_HISTQOERPT");
            onCreate(db);
            db.execSQL("DROP TABLE IF EXISTS CHR_FASTBACK2LTE");
            onCreate(db);
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" Exception onDowngrade: ");
            stringBuilder2.append(e);
            LogUtil.e(stringBuilder2.toString());
        }
    }
}

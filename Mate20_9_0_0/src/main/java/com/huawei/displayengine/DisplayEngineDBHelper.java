package com.huawei.displayengine;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DisplayEngineDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "DisplayEngine.db";
    public static final int DATABASE_VERSION = 10;
    public static final String TABLE_NAME_ALGORITHM_ESCW = "AlgorithmESCW";
    public static final String TABLE_NAME_BRIGHTNESS_CURVE_DEFAULT = "BrightnessCurveDefault";
    public static final String TABLE_NAME_BRIGHTNESS_CURVE_HIGH = "BrightnessCurveHigh";
    public static final String TABLE_NAME_BRIGHTNESS_CURVE_LOW = "BrightnessCurveLow";
    public static final String TABLE_NAME_BRIGHTNESS_CURVE_MIDDLE = "BrightnessCurveMiddle";
    public static final String TABLE_NAME_DATA_CLEANER = "DataCleaner";
    public static final String TABLE_NAME_DRAG_INFORMATION = "UserDragInformation";
    public static final String TABLE_NAME_USER_PREFERENCES = "UserPreferences";
    private static final String TAG = "DE J DisplayEngineDBHelper";

    public DisplayEngineDBHelper(Context context) {
        super(context, DATABASE_NAME, null, 10);
    }

    public void onCreate(SQLiteDatabase db) {
        createDragInformationTable(db);
        createUserPreferencesTable(db);
        createBrightnessCurveTable(db, "BrightnessCurveLow");
        createBrightnessCurveTable(db, "BrightnessCurveMiddle");
        createBrightnessCurveTable(db, "BrightnessCurveHigh");
        createBrightnessCurveTable(db, "BrightnessCurveDefault");
        createAlgorithmESCWTable(db);
        createDataCleanerTable(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 9) {
            db.execSQL("DROP TABLE IF EXISTS UserDragInformation");
            createDragInformationTable(db);
            return;
        }
        db.execSQL("DROP TABLE IF EXISTS UserDragInformation");
        db.execSQL("DROP TABLE IF EXISTS UserPreferences");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveLow");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveMiddle");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveHigh");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveDefault");
        db.execSQL("DROP TABLE IF EXISTS AlgorithmESCW");
        db.execSQL("DROP TABLE IF EXISTS DataCleaner");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS UserDragInformation");
        db.execSQL("DROP TABLE IF EXISTS UserPreferences");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveLow");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveMiddle");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveHigh");
        db.execSQL("DROP TABLE IF EXISTS BrightnessCurveDefault");
        db.execSQL("DROP TABLE IF EXISTS AlgorithmESCW");
        db.execSQL("DROP TABLE IF EXISTS DataCleaner");
        onCreate(db);
    }

    public static void createDragInformationTable(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [UserDragInformation] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[TIMESTAMP] INTEGER UNIQUE, ");
        sBuffer.append("[PRIORITY] INTEGER, ");
        sBuffer.append("[STARTPOINT] REAL, ");
        sBuffer.append("[STOPPOINT] REAL, ");
        sBuffer.append("[AL] INTEGER, ");
        sBuffer.append("[PROXIMITYPOSITIVE] INTEGER, ");
        sBuffer.append("[USERID] INTEGER, ");
        sBuffer.append("[APPTYPE] INTEGER, ");
        sBuffer.append("[GAMESTATE] INTEGER, ");
        sBuffer.append("[PACKAGE] TEXT)");
        try {
            db.execSQL(sBuffer.toString());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createDragInformationTable succss.");
            stringBuilder.append(sBuffer.toString());
            DElog.i(str, stringBuilder.toString());
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createDragInformationTable error:");
            stringBuilder2.append(e.getMessage());
            DElog.e(str2, stringBuilder2.toString());
        }
    }

    public static void createUserPreferencesTable(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [UserPreferences] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY, ");
        sBuffer.append("[USERID] INTEGER, ");
        sBuffer.append("[APPTYPE] INTEGER, ");
        sBuffer.append("[AL] INTEGER, ");
        sBuffer.append("[DELTA] INTEGER)");
        try {
            db.execSQL(sBuffer.toString());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createUserPreferencesTable succss.");
            stringBuilder.append(sBuffer.toString());
            DElog.i(str, stringBuilder.toString());
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createUserPreferencesTable error:");
            stringBuilder2.append(e.getMessage());
            DElog.e(str2, stringBuilder2.toString());
        }
    }

    public static void createBrightnessCurveTable(SQLiteDatabase db, String table) {
        StringBuffer sBuffer = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE if not exists [");
        stringBuilder.append(table);
        stringBuilder.append("] (");
        sBuffer.append(stringBuilder.toString());
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY, ");
        sBuffer.append("[USERID] INTEGER, ");
        sBuffer.append("[AL] REAL, ");
        sBuffer.append("[BL] REAL)");
        try {
            db.execSQL(sBuffer.toString());
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createBrightnessCurveTable succss.");
            stringBuilder2.append(sBuffer.toString());
            DElog.i(str, stringBuilder2.toString());
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("createBrightnessCurveTable error:");
            stringBuilder3.append(e.getMessage());
            DElog.e(str2, stringBuilder3.toString());
        }
    }

    public static void createAlgorithmESCWTable(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [AlgorithmESCW] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY, ");
        sBuffer.append("[USERID] INTEGER, ");
        sBuffer.append("[ESCW] REAL)");
        try {
            db.execSQL(sBuffer.toString());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createAlgorithmESCWTable succss.");
            stringBuilder.append(sBuffer.toString());
            DElog.i(str, stringBuilder.toString());
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createAlgorithmESCWTable error:");
            stringBuilder2.append(e.getMessage());
            DElog.e(str2, stringBuilder2.toString());
        }
    }

    public static void createDataCleanerTable(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [DataCleaner] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY, ");
        sBuffer.append("[RANGEFLAG] INTEGER, ");
        sBuffer.append("[TIMESTAMP] INTEGER)");
        try {
            db.execSQL(sBuffer.toString());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createDataCleanerTable succss.");
            stringBuilder.append(sBuffer.toString());
            DElog.i(str, stringBuilder.toString());
        } catch (SQLException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createDataCleanerTable error:");
            stringBuilder2.append(e.getMessage());
            DElog.e(str2, stringBuilder2.toString());
        }
    }
}

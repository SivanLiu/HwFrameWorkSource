package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HwQoEQualityDataBase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "HwQoEQuality.db";
    public static final int DATABASE_VERSION = 3;
    public static final String HW_QOE_QUALITY_NAME = "HwQoEQualityRecordTable";
    public static final String HW_QOE_WECHAT_AP_NAME = "HwQoEWeChatAPRecordTable";
    public static final String HW_QOE_WECHAT_NAME = "HwQoEWeChatRecordTable";

    public HwQoEQualityDataBase(Context context) {
        super(context, DATABASE_NAME, null, 3);
    }

    public void onCreate(SQLiteDatabase db) {
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [HwQoEQualityRecordTable] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[BSSID] TEXT, ");
        sBuffer.append("[RSSI] INTEGER, ");
        sBuffer.append("[APPType] INTEGER, ");
        sBuffer.append("[Thoughtput] INT8)");
        db.execSQL(sBuffer.toString());
        sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [HwQoEWeChatRecordTable] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[IMSI] TEXT, ");
        sBuffer.append("[APPType] INTEGER, ");
        sBuffer.append("[Thoughtput] LONG, ");
        sBuffer.append("[Duration] LONG, ");
        sBuffer.append("[Timestamp] LONG)");
        db.execSQL(sBuffer.toString());
        sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [HwQoEWeChatAPRecordTable] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[SSID] TEXT, ");
        sBuffer.append("[authType] INTEGER, ");
        sBuffer.append("[apType] INTEGER, ");
        sBuffer.append("[appType] INTEGER, ");
        sBuffer.append("[blackCount] INTEGER)");
        db.execSQL(sBuffer.toString());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS HwQoEQualityRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatAPRecordTable");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS HwQoEQualityRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatAPRecordTable");
        onCreate(db);
    }
}

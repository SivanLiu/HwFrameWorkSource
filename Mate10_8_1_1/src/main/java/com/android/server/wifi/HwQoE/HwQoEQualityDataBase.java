package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HwQoEQualityDataBase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "HwQoEQuality.db";
    public static final int DATABASE_VERSION = 4;
    public static final String HW_QOE_QUALITY_NAME = "HwQoEQualityRecordTable";
    public static final String HW_QOE_WECHAT_AP_NAME = "HwQoEWeChatAPRecordTable";
    public static final String HW_QOE_WECHAT_NAME = "HwQoEWeChatRecordTable";
    public static final String HW_QOE_WECHAT_STATISTICS = "HwQoEWeChatStatisticsTable";

    public HwQoEQualityDataBase(Context context) {
        super(context, DATABASE_NAME, null, 4);
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
        sBuffer = new StringBuffer();
        sBuffer.append("CREATE TABLE if not exists [HwQoEWeChatStatisticsTable] (");
        sBuffer.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, ");
        sBuffer.append("[APPType] INTEGER, ");
        sBuffer.append("[CallTotalCnt] INTEGER, ");
        sBuffer.append("[StartInWiFiCnt] INTEGER, ");
        sBuffer.append("[StartInCellularCnt] INTEGER, ");
        sBuffer.append("[CallInCellularDur] INTEGER, ");
        sBuffer.append("[CallInWiFiDur] INTEGER, ");
        sBuffer.append("[CellLv1Cnt] INTEGER, ");
        sBuffer.append("[CellLv2Cnt] INTEGER, ");
        sBuffer.append("[CellLv3Cnt] INTEGER, ");
        sBuffer.append("[WiFiLv1Cnt] INTEGER, ");
        sBuffer.append("[WiFiLv2Cnt] INTEGER, ");
        sBuffer.append("[WiFiLv3Cnt] INTEGER, ");
        sBuffer.append("[TrfficCell] INTEGER, ");
        sBuffer.append("[VipSwitchCnt] INTEGER, ");
        sBuffer.append("[StallSwitchCnt] INTEGER, ");
        sBuffer.append("[StallSwitch0Cnt] INTEGER, ");
        sBuffer.append("[StallSwitch1Cnt] INTEGER, ");
        sBuffer.append("[StallSwitchAbove1Cnt] INTEGER, ");
        sBuffer.append("[Switch2CellCnt] INTEGER, ");
        sBuffer.append("[Switch2WifiCnt] INTEGER, ");
        sBuffer.append("[LastUploadTime] LONG, ");
        sBuffer.append("[Reserved] INTEGER)");
        db.execSQL(sBuffer.toString());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS HwQoEQualityRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatAPRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatStatisticsTable");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS HwQoEQualityRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatAPRecordTable");
        db.execSQL("DROP TABLE IF EXISTS HwQoEWeChatStatisticsTable");
        onCreate(db);
    }
}

package com.huawei.displayengine;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.ArrayMap;
import java.util.ArrayList;

public class DisplayEngineDBManager {
    private static final String TAG = "DE J DisplayEngineDBManager";
    private static volatile DisplayEngineDBManager mInstance = null;
    private static Object mLock = new Object();
    private static final ArrayMap<String, TableProcessor> mTableProcessors = new ArrayMap();
    private SQLiteDatabase mDatabase = null;
    private DisplayEngineDBHelper mHelper;
    private Object mdbLock = new Object();

    public static class AlgorithmESCWKey {
        public static final String ESCW = "ESCW";
        public static final String TAG = "AlgorithmESCW";
        public static final String USERID = "UserID";
    }

    public static class BrightnessCurveKey {
        public static final String AL = "AmbientLight";
        public static final String BL = "BackLight";
        public static final String USERID = "UserID";

        public static class Default {
            public static final String TAG = "BrightnessCurveDefault";
        }

        public static class High {
            public static final String TAG = "BrightnessCurveHigh";
        }

        public static class Low {
            public static final String TAG = "BrightnessCurveLow";
        }

        public static class Middle {
            public static final String TAG = "BrightnessCurveMiddle";
        }
    }

    public static class DataCleanerKey {
        public static final String RANGEFLAG = "RangeFlag";
        public static final String TAG = "DataCleaner";
        public static final String TIMESTAMP = "TimeStamp";
        public static final String USERID = "UserID";
    }

    public static class DragInformationKey {
        public static final String AL = "AmbientLight";
        public static final String APPTYPE = "AppType";
        public static final String GAMESTATE = "GameState";
        public static final String PACKAGE = "PackageName";
        public static final String PRIORITY = "Priority";
        public static final String PROXIMITYPOSITIVE = "ProximityPositive";
        public static final String STARTPOINT = "StartPoint";
        public static final String STOPPOINT = "StopPoint";
        public static final String TAG = "DragInfo";
        public static final String TIMESTAMP = "TimeStamp";
        public static final String USERID = "UserID";
        public static final String _ID = "_ID";
    }

    public static class QueryInfoKey {
        public static final String NUMBERLIMIT = "NumberLimit";
    }

    private class TableProcessor {
        protected int mMaxSize = 0;

        public boolean setMaxSize(int size) {
            return false;
        }

        public boolean addorUpdateRecord(Bundle data) {
            return false;
        }

        public ArrayList<Bundle> getAllRecords(Bundle info) {
            if (info == null) {
                return getAllRecords();
            }
            return null;
        }

        public int getSize(Bundle info) {
            int size;
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (info == null) {
                    try {
                        size = getSizeWithoutLock();
                    } catch (Throwable th) {
                    }
                } else {
                    size = getSizeWithoutLock(info);
                }
            }
            return size;
        }

        protected ArrayList<Bundle> getAllRecords() {
            return null;
        }

        protected int getSizeWithoutLock() {
            return 0;
        }

        protected int getSizeWithoutLock(Bundle info) {
            return 0;
        }
    }

    public static class UserPreferencesKey {
        public static final String AL = "AmbientLight";
        public static final String APPTYPE = "AppType";
        public static final String DELTA = "BackLightDelta";
        public static final String TAG = "UserPref";
        public static final String USERID = "UserID";
    }

    private class AlgorithmESCWTableProcessor extends TableProcessor {
        public AlgorithmESCWTableProcessor() {
            super();
        }

        private boolean pretreatmentForAddorUpdateRecord(Bundle data, int userID, float[] escwValues) {
            if (userID < 0 || escwValues == null || (this.mMaxSize > 0 && escwValues.length > this.mMaxSize)) {
                String str;
                StringBuilder stringBuilder;
                if (escwValues == null) {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("AlgorithmESCWTableProcessor.addorUpdateRecord error: userID=");
                    stringBuilder.append(userID);
                    stringBuilder.append(" escw=null");
                    DElog.e(str, stringBuilder.toString());
                } else {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("AlgorithmESCWTableProcessor.addorUpdateRecord error: userID=");
                    stringBuilder.append(userID);
                    stringBuilder.append(" escw size=");
                    stringBuilder.append(escwValues.length);
                    stringBuilder.append(" max size=");
                    stringBuilder.append(this.mMaxSize);
                    DElog.e(str, stringBuilder.toString());
                }
                return false;
            } else if (clearWithoutLock(data)) {
                return true;
            } else {
                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.addorUpdateRecord() error: clear last records!");
                return false;
            }
        }

        public boolean addorUpdateRecord(Bundle data) {
            StringBuilder stringBuilder;
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (data != null) {
                        int userID = data.getInt("UserID");
                        float[] escwValues = data.getFloatArray(AlgorithmESCWKey.ESCW);
                        if (pretreatmentForAddorUpdateRecord(data, userID, escwValues)) {
                            try {
                                StringBuilder stringBuilder2;
                                StringBuffer text = new StringBuffer();
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("AlgorithmESCWTableProcessor add record succ: userID=");
                                stringBuilder3.append(userID);
                                stringBuilder3.append(" escw={");
                                text.append(stringBuilder3.toString());
                                for (float append : escwValues) {
                                    DisplayEngineDBManager.this.mDatabase.execSQL("INSERT INTO AlgorithmESCW VALUES(?, ?, ?)", new Object[]{Integer.valueOf(i + 1), Integer.valueOf(userID), Float.valueOf(escwValues[i])});
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(append);
                                    stringBuilder2.append(";");
                                    text.append(stringBuilder2.toString());
                                }
                                String str = DisplayEngineDBManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(text.toString());
                                stringBuilder2.append("}");
                                DElog.i(str, stringBuilder2.toString());
                                return true;
                            } catch (SQLException e) {
                                StringBuffer text2 = new StringBuffer();
                                text2.append("AlgorithmESCWTableProcessor add record escw={");
                                for (float append2 : escwValues) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(append2);
                                    stringBuilder.append(";");
                                    text2.append(stringBuilder.toString());
                                }
                                String str2 = DisplayEngineDBManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(text2.toString());
                                stringBuilder.append("}, error:");
                                stringBuilder.append(e.getMessage());
                                DElog.e(str2, stringBuilder.toString());
                                return false;
                            }
                        }
                        return false;
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.addorUpdateRecord error: Invalid input!");
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:21:0x0061, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:27:0x008f, code skipped:
            if (r4 != null) goto L_0x0091;
     */
        /* JADX WARNING: Missing block: B:29:?, code skipped:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:35:0x00b3, code skipped:
            if (r4 == null) goto L_0x00b6;
     */
        /* JADX WARNING: Missing block: B:38:0x00b7, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ArrayList<Bundle> getAllRecords(Bundle info) {
            ArrayList<Bundle> records = new ArrayList();
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (info != null) {
                        int userID = info.getInt("UserID");
                        if (userID < 0) {
                            String str = DisplayEngineDBManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("AlgorithmESCWTableProcessor.getAllRecords() invalid input: userID=");
                            stringBuilder.append(userID);
                            DElog.e(str, stringBuilder.toString());
                            return null;
                        }
                        Cursor c = null;
                        try {
                            c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM AlgorithmESCW where USERID = ?", new String[]{String.valueOf(userID)});
                            if (c == null) {
                                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.getAllRecords() query database error.");
                                if (c != null) {
                                    c.close();
                                }
                            } else {
                                while (c.moveToNext()) {
                                    Bundle record = new Bundle();
                                    record.putInt("UserID", c.getInt(c.getColumnIndex("USERID")));
                                    record.putFloat(AlgorithmESCWKey.ESCW, c.getFloat(c.getColumnIndex(AlgorithmESCWKey.ESCW)));
                                    records.add(record);
                                }
                            }
                        } catch (SQLException e) {
                            records = null;
                            try {
                                String str2 = DisplayEngineDBManager.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("AlgorithmESCWTableProcessor.getAllRecords() error:");
                                stringBuilder2.append(e.getMessage());
                                DElog.w(str2, stringBuilder2.toString());
                            } catch (Throwable th) {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.getAllRecords() mDatabase error or info is null.");
                return null;
            }
        }

        protected int getSizeWithoutLock(Bundle info) {
            int size = 0;
            if (DisplayEngineDBManager.this.mDatabase == null || !DisplayEngineDBManager.this.mDatabase.isOpen() || info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.getSizeWithoutLock() mDatabase error or info is null.");
                return 0;
            }
            int userID = info.getInt("UserID");
            String str;
            if (userID < 0) {
                str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmESCWTableProcessor.getSizeWithoutLock() invalid input: userID=");
                stringBuilder.append(userID);
                DElog.e(str, stringBuilder.toString());
                return 0;
            }
            str = new StringBuilder();
            str.append("SELECT COUNT(*) FROM AlgorithmESCW where USERID = ");
            str.append(userID);
            String str2;
            StringBuilder stringBuilder2;
            try {
                size = (int) DisplayEngineDBManager.this.mDatabase.compileStatement(str.toString()).simpleQueryForLong();
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AlgorithmESCWTableProcessor.getSizeWithoutLock() return ");
                stringBuilder2.append(size);
                DElog.d(str2, stringBuilder2.toString());
                return size;
            } catch (SQLException e) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AlgorithmESCWTableProcessor.getSizeWithoutLock() error:");
                stringBuilder2.append(e.getMessage());
                DElog.w(str2, stringBuilder2.toString());
                return size;
            }
        }

        protected boolean clearWithoutLock(Bundle info) {
            String str;
            StringBuilder stringBuilder;
            boolean ret = false;
            if (!DisplayEngineDBManager.this.checkDatabaseStatusIsOk() || info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.clearWithoutLock() mDatabase error or info is null.");
                return false;
            }
            int userID = info.getInt("UserID");
            StringBuilder stringBuilder2;
            if (userID < 0) {
                String str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AlgorithmESCWTableProcessor.clearWithoutLock() invalid input: userID=");
                stringBuilder2.append(userID);
                DElog.e(str2, stringBuilder2.toString());
                return false;
            }
            try {
                if (getSizeWithoutLock(info) > 0) {
                    SQLiteDatabase access$200 = DisplayEngineDBManager.this.mDatabase;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DELETE FROM AlgorithmESCW where USERID = ");
                    stringBuilder2.append(userID);
                    access$200.execSQL(stringBuilder2.toString());
                }
                DElog.i(DisplayEngineDBManager.TAG, "AlgorithmESCWTableProcessor.clearWithoutLock() sucess.");
                ret = true;
            } catch (SQLException e) {
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmESCWTableProcessor.clearWithoutLock() error:");
                stringBuilder.append(e.getMessage());
                DElog.w(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("AlgorithmESCWTableProcessor.clearWithoutLock() error:");
                stringBuilder.append(e2.getMessage());
                DElog.w(str, stringBuilder.toString());
            }
            return ret;
        }
    }

    private class BrightnessCurveTableProcessor extends TableProcessor {
        private final String mTableName;

        public BrightnessCurveTableProcessor(String name) {
            super();
            if (name == null) {
                this.mTableName = null;
                DElog.e(DisplayEngineDBManager.TAG, "BrightnessCurveTableProcessor invalid input name=null!");
                return;
            }
            Object obj = -1;
            int hashCode = name.hashCode();
            if (hashCode != -1518388362) {
                if (hashCode != 174475712) {
                    if (hashCode != 310490675) {
                        if (hashCode == 1524927843 && name.equals("BrightnessCurveDefault")) {
                            obj = 3;
                        }
                    } else if (name.equals("BrightnessCurveMiddle")) {
                        obj = 1;
                    }
                } else if (name.equals("BrightnessCurveHigh")) {
                    obj = 2;
                }
            } else if (name.equals("BrightnessCurveLow")) {
                obj = null;
            }
            switch (obj) {
                case null:
                    this.mTableName = "BrightnessCurveLow";
                    break;
                case 1:
                    this.mTableName = "BrightnessCurveMiddle";
                    break;
                case 2:
                    this.mTableName = "BrightnessCurveHigh";
                    break;
                case 3:
                    this.mTableName = "BrightnessCurveDefault";
                    break;
                default:
                    this.mTableName = null;
                    String str = DisplayEngineDBManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("BrightnessCurveTableProcessor unknown name=");
                    stringBuilder.append(name);
                    DElog.e(str, stringBuilder.toString());
                    break;
            }
        }

        /* JADX WARNING: Missing block: B:24:0x0071, code skipped:
            if (r3 != null) goto L_0x0073;
     */
        /* JADX WARNING: Missing block: B:26:?, code skipped:
            r3.close();
     */
        /* JADX WARNING: Missing block: B:32:0x009f, code skipped:
            if (r3 == null) goto L_0x00a2;
     */
        /* JADX WARNING: Missing block: B:34:?, code skipped:
            r7.mMaxSize = r8;
            r2 = com.huawei.displayengine.DisplayEngineDBManager.TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("TableProcessor[");
            r4.append(r7.mTableName);
            r4.append("].setMaxSize(");
            r4.append(r7.mMaxSize);
            r4.append(") success.");
            com.huawei.displayengine.DElog.i(r2, r4.toString());
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean setMaxSize(int size) {
            boolean ret = true;
            if (size > 0) {
                synchronized (DisplayEngineDBManager.this.mdbLock) {
                    if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                        Cursor c = null;
                        try {
                            c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT DISTINCT USERID FROM UserDragInformation", null);
                            if (c != null && c.getCount() > 0) {
                                while (c.moveToNext()) {
                                    Bundle info = new Bundle();
                                    info.putInt("UserID", c.getInt(c.getColumnIndex("USERID")));
                                    if (getSizeWithoutLock(info) > size && !clearWithoutLock(info)) {
                                        ret = false;
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            ret = false;
                            try {
                                String str = DisplayEngineDBManager.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("TableProcessor[");
                                stringBuilder.append(this.mTableName);
                                stringBuilder.append("].setMaxSize() failed to get all the user IDs, error:");
                                stringBuilder.append(e.getMessage());
                                DElog.w(str, stringBuilder.toString());
                            } catch (Throwable th) {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    } else {
                        String str2 = DisplayEngineDBManager.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("TableProcessor[");
                        stringBuilder2.append(this.mTableName);
                        stringBuilder2.append("].setMaxSize() mDatabase error!");
                        DElog.e(str2, stringBuilder2.toString());
                        return false;
                    }
                }
            }
            ret = false;
            String str3 = DisplayEngineDBManager.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("TableProcessor[");
            stringBuilder3.append(this.mTableName);
            stringBuilder3.append("].setMaxSize invalid input: size=");
            stringBuilder3.append(size);
            DElog.e(str3, stringBuilder3.toString());
            return ret;
        }

        private boolean pretreatmentForAddorUpdateRecord(Bundle data, int userID, float[] alValues, float[] blValues) {
            String str;
            StringBuilder stringBuilder;
            if (userID < 0 || alValues == null || blValues == null || alValues.length != blValues.length || (this.mMaxSize > 0 && alValues.length > this.mMaxSize)) {
                if (alValues == null || blValues == null) {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("TableProcessor[");
                    stringBuilder.append(this.mTableName);
                    stringBuilder.append("].addorUpdateRecord error: userID=");
                    stringBuilder.append(userID);
                    stringBuilder.append(" al=null or bl=null");
                    DElog.e(str, stringBuilder.toString());
                } else {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("TableProcessor[");
                    stringBuilder.append(this.mTableName);
                    stringBuilder.append("].addorUpdateRecord error: userID=");
                    stringBuilder.append(userID);
                    stringBuilder.append(" al size=");
                    stringBuilder.append(alValues.length);
                    stringBuilder.append(" bl size=");
                    stringBuilder.append(blValues.length);
                    stringBuilder.append(" max size=");
                    stringBuilder.append(this.mMaxSize);
                    DElog.e(str, stringBuilder.toString());
                }
                return false;
            } else if (clearWithoutLock(data)) {
                return true;
            } else {
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("TableProcessor[");
                stringBuilder.append(this.mTableName);
                stringBuilder.append("].addorUpdateRecord error: clear last records!");
                DElog.e(str, stringBuilder.toString());
                return false;
            }
        }

        public boolean addorUpdateRecord(Bundle data) {
            StringBuilder stringBuilder;
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (data != null) {
                        int userID = data.getInt("UserID");
                        float[] alValues = data.getFloatArray("AmbientLight");
                        float[] blValues = data.getFloatArray(BrightnessCurveKey.BL);
                        if (pretreatmentForAddorUpdateRecord(data, userID, alValues, blValues)) {
                            StringBuilder stringBuilder2;
                            try {
                                StringBuffer text = new StringBuffer();
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("TableProcessor[");
                                stringBuilder3.append(this.mTableName);
                                stringBuilder3.append("] add record succ: userID=");
                                stringBuilder3.append(userID);
                                stringBuilder3.append(" points={");
                                text.append(stringBuilder3.toString());
                                for (int i = 0; i < alValues.length; i++) {
                                    SQLiteDatabase access$200 = DisplayEngineDBManager.this.mDatabase;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("INSERT INTO ");
                                    stringBuilder4.append(this.mTableName);
                                    stringBuilder4.append(" VALUES(?, ?, ?, ?)");
                                    access$200.execSQL(stringBuilder4.toString(), new Object[]{Integer.valueOf(i + 1), Integer.valueOf(userID), Float.valueOf(alValues[i]), Float.valueOf(blValues[i])});
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(alValues[i]);
                                    stringBuilder2.append(",");
                                    stringBuilder2.append(blValues[i]);
                                    stringBuilder2.append(";");
                                    text.append(stringBuilder2.toString());
                                }
                                String str = DisplayEngineDBManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(text.toString());
                                stringBuilder2.append("}");
                                DElog.i(str, stringBuilder2.toString());
                                return true;
                            } catch (SQLException e) {
                                StringBuffer text2 = new StringBuffer();
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("TableProcessor[");
                                stringBuilder2.append(this.mTableName);
                                stringBuilder2.append("] add record userID=");
                                stringBuilder2.append(userID);
                                stringBuilder2.append(" points={");
                                text2.append(stringBuilder2.toString());
                                for (int i2 = 0; i2 < alValues.length; i2++) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(alValues[i2]);
                                    stringBuilder.append(",");
                                    stringBuilder.append(blValues[i2]);
                                    stringBuilder.append(";");
                                    text2.append(stringBuilder.toString());
                                }
                                String str2 = DisplayEngineDBManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(text2.toString());
                                stringBuilder.append("}, error:");
                                stringBuilder.append(e.getMessage());
                                DElog.e(str2, stringBuilder.toString());
                                return false;
                            }
                        }
                        return false;
                    }
                }
                String str3 = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("TableProcessor[");
                stringBuilder5.append(this.mTableName);
                stringBuilder5.append("].addorUpdateRecord error: Invalid input!");
                DElog.e(str3, stringBuilder5.toString());
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:21:0x0097, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:27:0x00d4, code skipped:
            if (r4 != null) goto L_0x00d6;
     */
        /* JADX WARNING: Missing block: B:29:?, code skipped:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:35:0x0102, code skipped:
            if (r4 == null) goto L_0x0105;
     */
        /* JADX WARNING: Missing block: B:38:0x0106, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ArrayList<Bundle> getAllRecords(Bundle info) {
            ArrayList<Bundle> records = new ArrayList();
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (info != null) {
                        int userID = info.getInt("UserID");
                        if (userID < 0) {
                            String str = DisplayEngineDBManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("TableProcessor[");
                            stringBuilder.append(this.mTableName);
                            stringBuilder.append("].getAllRecords() invalid input: userID=");
                            stringBuilder.append(userID);
                            DElog.e(str, stringBuilder.toString());
                            return null;
                        }
                        Cursor c = null;
                        StringBuilder stringBuilder2;
                        String str2;
                        try {
                            SQLiteDatabase access$200 = DisplayEngineDBManager.this.mDatabase;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("SELECT * FROM ");
                            stringBuilder2.append(this.mTableName);
                            stringBuilder2.append(" where USERID = ? ORDER BY AL ASC");
                            c = access$200.rawQuery(stringBuilder2.toString(), new String[]{String.valueOf(userID)});
                            if (c == null) {
                                str2 = DisplayEngineDBManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("TableProcessor[");
                                stringBuilder2.append(this.mTableName);
                                stringBuilder2.append("].getAllRecords() query database error.");
                                DElog.e(str2, stringBuilder2.toString());
                                if (c != null) {
                                    c.close();
                                }
                            } else {
                                while (c.moveToNext()) {
                                    Bundle record = new Bundle();
                                    record.putInt("UserID", c.getInt(c.getColumnIndex("USERID")));
                                    record.putFloat("AmbientLight", c.getFloat(c.getColumnIndex("AL")));
                                    record.putFloat(BrightnessCurveKey.BL, c.getFloat(c.getColumnIndex("BL")));
                                    records.add(record);
                                }
                            }
                        } catch (SQLException e) {
                            records = null;
                            try {
                                str2 = DisplayEngineDBManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("TableProcessor[");
                                stringBuilder2.append(this.mTableName);
                                stringBuilder2.append("].getAllRecords() error:");
                                stringBuilder2.append(e.getMessage());
                                DElog.w(str2, stringBuilder2.toString());
                            } catch (Throwable th) {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    }
                }
                String str3 = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("TableProcessor[");
                stringBuilder3.append(this.mTableName);
                stringBuilder3.append("].getAllRecords() mDatabase error or info is null.");
                DElog.e(str3, stringBuilder3.toString());
                return null;
            }
        }

        protected int getSizeWithoutLock(Bundle info) {
            int size = 0;
            if (DisplayEngineDBManager.this.mDatabase == null || !DisplayEngineDBManager.this.mDatabase.isOpen() || info == null) {
                String str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("TableProcessor[");
                stringBuilder.append(this.mTableName);
                stringBuilder.append("].getSizeWithoutLock() mDatabase error.");
                DElog.e(str, stringBuilder.toString());
                return 0;
            }
            int userID = info.getInt("UserID");
            String str2;
            if (userID < 0) {
                str2 = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("TableProcessor[");
                stringBuilder2.append(this.mTableName);
                stringBuilder2.append("].getSizeWithoutLock() invalid input: userID=");
                stringBuilder2.append(userID);
                DElog.e(str2, stringBuilder2.toString());
                return 0;
            }
            str2 = new StringBuilder();
            str2.append("SELECT COUNT(*) FROM ");
            str2.append(this.mTableName);
            str2.append(" where USERID = ");
            str2.append(userID);
            String str3;
            StringBuilder stringBuilder3;
            try {
                size = (int) DisplayEngineDBManager.this.mDatabase.compileStatement(str2.toString()).simpleQueryForLong();
                str3 = DisplayEngineDBManager.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("TableProcessor[");
                stringBuilder3.append(this.mTableName);
                stringBuilder3.append("].getSizeWithoutLock() return ");
                stringBuilder3.append(size);
                DElog.i(str3, stringBuilder3.toString());
                return size;
            } catch (SQLException e) {
                str3 = DisplayEngineDBManager.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("TableProcessor[");
                stringBuilder3.append(this.mTableName);
                stringBuilder3.append("].getSizeWithoutLock() error:");
                stringBuilder3.append(e.getMessage());
                DElog.w(str3, stringBuilder3.toString());
                return size;
            }
        }

        protected boolean clearWithoutLock(Bundle info) {
            boolean ret = false;
            StringBuilder stringBuilder;
            if (!DisplayEngineDBManager.this.checkDatabaseStatusIsOk() || info == null) {
                String str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("TableProcessor[");
                stringBuilder.append(this.mTableName);
                stringBuilder.append("].clearWithoutLock() mDatabase error or info is null.");
                DElog.e(str, stringBuilder.toString());
                return false;
            }
            int userID = info.getInt("UserID");
            String str2;
            StringBuilder stringBuilder2;
            if (userID < 0) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("TableProcessor[");
                stringBuilder2.append(this.mTableName);
                stringBuilder2.append("].clearWithoutLock() invalid input: userID=");
                stringBuilder2.append(userID);
                DElog.e(str2, stringBuilder2.toString());
                return false;
            }
            try {
                if (getSizeWithoutLock(info) > 0) {
                    SQLiteDatabase access$200 = DisplayEngineDBManager.this.mDatabase;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DELETE FROM ");
                    stringBuilder.append(this.mTableName);
                    stringBuilder.append(" where USERID = ");
                    stringBuilder.append(userID);
                    access$200.execSQL(stringBuilder.toString());
                }
                String str3 = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("TableProcessor[");
                stringBuilder.append(this.mTableName);
                stringBuilder.append("].clearWithoutLock() sucess.");
                DElog.i(str3, stringBuilder.toString());
                ret = true;
            } catch (SQLException e) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("TableProcessor[");
                stringBuilder2.append(this.mTableName);
                stringBuilder2.append("].clearWithoutLock() error:");
                stringBuilder2.append(e.getMessage());
                DElog.w(str2, stringBuilder2.toString());
            } catch (IllegalArgumentException e2) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("TableProcessor[");
                stringBuilder2.append(this.mTableName);
                stringBuilder2.append("].clearWithoutLock() error:");
                stringBuilder2.append(e2.getMessage());
                DElog.w(str2, stringBuilder2.toString());
            }
            return ret;
        }
    }

    private class DataCleanerTableProcessor extends TableProcessor {
        public DataCleanerTableProcessor() {
            super();
        }

        private boolean pretreatmentForAddorUpdateRecord(Bundle data, int userID, int flag, long time) {
            if (time > 0 && userID >= 0 && flag >= 0) {
                return true;
            }
            String str = DisplayEngineDBManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DataCleanerTableProcessor.addorUpdateRecord error: userID=");
            stringBuilder.append(userID);
            stringBuilder.append(" time=");
            stringBuilder.append(time);
            stringBuilder.append(" RangeFalg=");
            stringBuilder.append(flag);
            DElog.e(str, stringBuilder.toString());
            return false;
        }

        /* JADX WARNING: Missing block: B:24:0x0134, code skipped:
            if (r1 != null) goto L_0x0136;
     */
        /* JADX WARNING: Missing block: B:26:?, code skipped:
            r1.close();
     */
        /* JADX WARNING: Missing block: B:32:0x0170, code skipped:
            if (r1 == null) goto L_0x0173;
     */
        /* JADX WARNING: Missing block: B:35:0x0174, code skipped:
            return r10;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean addorUpdateRecord(Bundle data) {
            Bundle bundle = data;
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                boolean ret = true;
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (bundle != null) {
                        int userID = bundle.getInt("UserID");
                        int flag = bundle.getInt(DataCleanerKey.RANGEFLAG);
                        long time = bundle.getLong("TimeStamp");
                        if (pretreatmentForAddorUpdateRecord(bundle, userID, flag, time)) {
                            Cursor c = null;
                            try {
                                c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM DataCleaner where _id = ?", new String[]{String.valueOf(userID)});
                                if (c == null || c.getCount() <= 0) {
                                    DisplayEngineDBManager.this.mDatabase.execSQL("INSERT INTO DataCleaner VALUES(?, ?, ?)", new Object[]{Integer.valueOf(userID), Integer.valueOf(flag), Long.valueOf(time)});
                                    String str = DisplayEngineDBManager.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("DataCleanerTableProcessor add a record succ: userID=");
                                    stringBuilder.append(userID);
                                    stringBuilder.append(" time=");
                                    stringBuilder.append(time);
                                    stringBuilder.append(" RangeFalg=");
                                    stringBuilder.append(flag);
                                    DElog.i(str, stringBuilder.toString());
                                } else {
                                    ContentValues values = new ContentValues();
                                    values.put("_id", Integer.valueOf(userID));
                                    values.put("RANGEFLAG", Integer.valueOf(flag));
                                    values.put("TIMESTAMP", Long.valueOf(time));
                                    int rows = DisplayEngineDBManager.this.mDatabase.update("DataCleaner", values, "_id = ?", new String[]{String.valueOf(userID)});
                                    String str2;
                                    StringBuilder stringBuilder2;
                                    if (rows == 0) {
                                        str2 = DisplayEngineDBManager.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("DataCleanerTableProcessor update failed: userID=");
                                        stringBuilder2.append(userID);
                                        stringBuilder2.append(" RangeFlag=");
                                        stringBuilder2.append(flag);
                                        stringBuilder2.append(" timeStamp=");
                                        stringBuilder2.append(time);
                                        DElog.e(str2, stringBuilder2.toString());
                                        ret = false;
                                    } else {
                                        str2 = DisplayEngineDBManager.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("DataCleanerTableProcessor update succ: rows=");
                                        stringBuilder2.append(rows);
                                        stringBuilder2.append(" userID=");
                                        stringBuilder2.append(userID);
                                        stringBuilder2.append(" RangeFlag=");
                                        stringBuilder2.append(flag);
                                        stringBuilder2.append(" timeStamp=");
                                        stringBuilder2.append(time);
                                        DElog.i(str2, stringBuilder2.toString());
                                    }
                                }
                            } catch (SQLException e) {
                                try {
                                    String str3 = DisplayEngineDBManager.TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("DataCleanerTableProcessor add a record userID=");
                                    stringBuilder3.append(userID);
                                    stringBuilder3.append(" time=");
                                    stringBuilder3.append(time);
                                    stringBuilder3.append(" RangeFalg=");
                                    stringBuilder3.append(flag);
                                    stringBuilder3.append(", error:");
                                    stringBuilder3.append(e.getMessage());
                                    DElog.e(str3, stringBuilder3.toString());
                                    ret = false;
                                } catch (Throwable th) {
                                    if (c != null) {
                                        c.close();
                                    }
                                }
                            }
                        } else {
                            return false;
                        }
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "DataCleanerTableProcessor.addorUpdateRecord error: Invalid input!");
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:17:0x003c, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:23:0x0079, code skipped:
            if (r2 != null) goto L_0x007b;
     */
        /* JADX WARNING: Missing block: B:25:?, code skipped:
            r2.close();
     */
        /* JADX WARNING: Missing block: B:31:0x009d, code skipped:
            if (r2 == null) goto L_0x00a0;
     */
        /* JADX WARNING: Missing block: B:34:0x00a1, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected ArrayList<Bundle> getAllRecords() {
            ArrayList<Bundle> records = new ArrayList();
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    Cursor c = null;
                    try {
                        c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM DataCleaner", null);
                        if (c == null) {
                            DElog.e(DisplayEngineDBManager.TAG, "DataCleanerTableProcessor.getAllRecords() query database error.");
                            if (c != null) {
                                c.close();
                            }
                        } else {
                            while (c.moveToNext()) {
                                Bundle record = new Bundle();
                                record.putInt("UserID", c.getInt(c.getColumnIndex("_id")));
                                record.putInt(DataCleanerKey.RANGEFLAG, c.getInt(c.getColumnIndex("RANGEFLAG")));
                                record.putLong("TimeStamp", c.getLong(c.getColumnIndex("TIMESTAMP")));
                                records.add(record);
                            }
                        }
                    } catch (SQLException e) {
                        records = null;
                        try {
                            String str = DisplayEngineDBManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("DataCleanerTableProcessor.getAllRecords() error:");
                            stringBuilder.append(e.getMessage());
                            DElog.w(str, stringBuilder.toString());
                        } catch (Throwable th) {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                } else {
                    DElog.e(DisplayEngineDBManager.TAG, "DataCleanerTableProcessor.getAllRecords() mDatabase error.");
                    return null;
                }
            }
        }

        protected int getSizeWithoutLock() {
            int size = 0;
            if (DisplayEngineDBManager.this.mDatabase == null || !DisplayEngineDBManager.this.mDatabase.isOpen()) {
                DElog.e(DisplayEngineDBManager.TAG, "DataCleanerTableProcessor.getSizeWithoutLock() mDatabase error.");
                return 0;
            }
            String str;
            StringBuilder stringBuilder;
            try {
                size = (int) DisplayEngineDBManager.this.mDatabase.compileStatement("SELECT COUNT(*) FROM DataCleaner").simpleQueryForLong();
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DataCleanerTableProcessor.getSizeWithoutLock() return ");
                stringBuilder.append(size);
                DElog.d(str, stringBuilder.toString());
                return size;
            } catch (SQLException e) {
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DataCleanerTableProcessor.getSizeWithoutLock() error:");
                stringBuilder.append(e.getMessage());
                DElog.w(str, stringBuilder.toString());
                return size;
            }
        }
    }

    private class DragInformationTableProcessor extends TableProcessor {
        public DragInformationTableProcessor() {
            super();
        }

        /* JADX WARNING: Missing block: B:24:0x005d, code skipped:
            if (r3 != null) goto L_0x005f;
     */
        /* JADX WARNING: Missing block: B:26:?, code skipped:
            r3.close();
     */
        /* JADX WARNING: Missing block: B:32:0x0081, code skipped:
            if (r3 == null) goto L_0x0084;
     */
        /* JADX WARNING: Missing block: B:34:?, code skipped:
            r7.mMaxSize = r8;
            r2 = com.huawei.displayengine.DisplayEngineDBManager.TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("DragInformationTableProcessor.setMaxSize(");
            r4.append(r7.mMaxSize);
            r4.append(") success.");
            com.huawei.displayengine.DElog.i(r2, r4.toString());
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean setMaxSize(int size) {
            boolean ret = true;
            if (size > 0) {
                synchronized (DisplayEngineDBManager.this.mdbLock) {
                    if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                        Cursor c = null;
                        try {
                            c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT DISTINCT USERID FROM UserDragInformation", null);
                            if (c != null && c.getCount() > 0) {
                                while (c.moveToNext()) {
                                    Bundle info = new Bundle();
                                    info.putInt("UserID", c.getInt(c.getColumnIndex("USERID")));
                                    int realSize = getSizeWithoutLock(info);
                                    if (realSize > size && !deleteRecordsWithoutLock(info, realSize - size)) {
                                        ret = false;
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            ret = false;
                            try {
                                String str = DisplayEngineDBManager.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("DragInformationTableProcessor.setMaxSize() failed to get all the user IDs, error:");
                                stringBuilder.append(e.getMessage());
                                DElog.w(str, stringBuilder.toString());
                            } catch (Throwable th) {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    } else {
                        DElog.e(DisplayEngineDBManager.TAG, "DragInformationTableProcessor.setMaxSize() mDatabase error!");
                        return false;
                    }
                }
            }
            ret = false;
            String str2 = DisplayEngineDBManager.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DragInformationTableProcessor.setMaxSize() invalid input: size=");
            stringBuilder2.append(size);
            DElog.e(str2, stringBuilder2.toString());
            return ret;
        }

        private boolean pretreatmentForAddorUpdateRecord(Bundle data, int userID, int appType, long time) {
            if (userID < 0 || appType < 0 || time <= 0) {
                String str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DragInformationTableProcessor.addorUpdateRecord invalid input: time=");
                stringBuilder.append(time);
                stringBuilder.append(" userID=");
                stringBuilder.append(userID);
                stringBuilder.append(" appType=");
                stringBuilder.append(appType);
                DElog.e(str, stringBuilder.toString());
                return false;
            }
            if (this.mMaxSize > 0) {
                int realSize = getSizeWithoutLock(data);
                if (realSize < this.mMaxSize || deleteRecordsWithoutLock(data, (realSize - this.mMaxSize) + 1)) {
                    return true;
                }
                return false;
            }
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:88:0x031e  */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x0318  */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x0318  */
        /* JADX WARNING: Removed duplicated region for block: B:88:0x031e  */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* JADX WARNING: Removed duplicated region for block: B:137:0x044f A:{SYNTHETIC, Splitter:B:137:0x044f} */
        /* JADX WARNING: Removed duplicated region for block: B:146:0x045b A:{Catch:{ all -> 0x0454 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean addorUpdateRecord(Bundle data) {
            SQLException e;
            String pkgName;
            int gameState;
            String str;
            StringBuilder stringBuilder;
            boolean ret;
            Throwable th;
            int i;
            boolean ret2;
            Bundle bundle = data;
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                int i2 = 0;
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (bundle != null) {
                        long time = bundle.getLong("TimeStamp");
                        int priority = bundle.getInt(DragInformationKey.PRIORITY);
                        float start = bundle.getFloat(DragInformationKey.STARTPOINT);
                        float stop = bundle.getFloat(DragInformationKey.STOPPOINT);
                        int al = bundle.getInt("AmbientLight");
                        boolean cover = bundle.getBoolean(DragInformationKey.PROXIMITYPOSITIVE);
                        int userID = bundle.getInt("UserID");
                        int appType = bundle.getInt("AppType");
                        boolean gameState2 = bundle.getInt(DragInformationKey.GAMESTATE);
                        String pkgName2 = bundle.getString(DragInformationKey.PACKAGE);
                        boolean gameState3 = gameState2;
                        int appType2 = appType;
                        int userID2 = userID;
                        int al2 = al;
                        boolean cover2 = cover;
                        if (pretreatmentForAddorUpdateRecord(bundle, userID, appType2, time)) {
                            Cursor c = null;
                            int al3;
                            boolean cover3;
                            boolean z;
                            try {
                                String str2;
                                c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM UserDragInformation where TIMESTAMP = ?", new String[]{String.valueOf(time)});
                                ContentValues values = new ContentValues();
                                values.put("TIMESTAMP", Long.valueOf(time));
                                values.put("PRIORITY", Integer.valueOf(priority));
                                values.put("STARTPOINT", Float.valueOf(start));
                                values.put("STOPPOINT", Float.valueOf(stop));
                                al3 = al2;
                                try {
                                    values.put("AL", Integer.valueOf(al3));
                                    str2 = "PROXIMITYPOSITIVE";
                                    cover3 = cover2;
                                    if (cover3) {
                                        i2 = 1;
                                    }
                                } catch (SQLException e2) {
                                    e = e2;
                                    z = true;
                                    pkgName = pkgName2;
                                    gameState = gameState3;
                                    al = appType2;
                                    i2 = userID2;
                                    cover3 = cover2;
                                    try {
                                        str = DisplayEngineDBManager.TAG;
                                        stringBuilder = new StringBuilder();
                                        try {
                                            stringBuilder.append("DragInformationTableProcessor add a record time=");
                                            stringBuilder.append(time);
                                            stringBuilder.append(",start=");
                                            stringBuilder.append(start);
                                            stringBuilder.append(",stop=");
                                            stringBuilder.append(stop);
                                            stringBuilder.append(",al=");
                                            stringBuilder.append(al3);
                                            stringBuilder.append(",proximitypositive=");
                                            stringBuilder.append(cover3);
                                            stringBuilder.append(",userID=");
                                            stringBuilder.append(i2);
                                            stringBuilder.append(",appType=");
                                            stringBuilder.append(al);
                                            stringBuilder.append(",gameState=");
                                            stringBuilder.append(gameState);
                                            stringBuilder.append(",pkgName=");
                                            stringBuilder.append(pkgName);
                                            stringBuilder.append(", error:");
                                            stringBuilder.append(e.getMessage());
                                            DElog.e(str, stringBuilder.toString());
                                            ret = false;
                                            if (c != null) {
                                            }
                                            return ret;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            if (c != null) {
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        i = priority;
                                        if (c != null) {
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    z = true;
                                    pkgName = pkgName2;
                                    ret2 = gameState3;
                                    al = appType2;
                                    i2 = userID2;
                                    cover3 = cover2;
                                    i = priority;
                                    if (c != null) {
                                    }
                                    throw th;
                                }
                                try {
                                    values.put(str2, Integer.valueOf(i2));
                                    i2 = userID2;
                                    try {
                                        values.put("USERID", Integer.valueOf(i2));
                                        z = true;
                                        al = appType2;
                                    } catch (SQLException e3) {
                                        e = e3;
                                        z = true;
                                        pkgName = pkgName2;
                                        gameState = gameState3;
                                        al = appType2;
                                        str = DisplayEngineDBManager.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("DragInformationTableProcessor add a record time=");
                                        stringBuilder.append(time);
                                        stringBuilder.append(",start=");
                                        stringBuilder.append(start);
                                        stringBuilder.append(",stop=");
                                        stringBuilder.append(stop);
                                        stringBuilder.append(",al=");
                                        stringBuilder.append(al3);
                                        stringBuilder.append(",proximitypositive=");
                                        stringBuilder.append(cover3);
                                        stringBuilder.append(",userID=");
                                        stringBuilder.append(i2);
                                        stringBuilder.append(",appType=");
                                        stringBuilder.append(al);
                                        stringBuilder.append(",gameState=");
                                        stringBuilder.append(gameState);
                                        stringBuilder.append(",pkgName=");
                                        stringBuilder.append(pkgName);
                                        stringBuilder.append(", error:");
                                        stringBuilder.append(e.getMessage());
                                        DElog.e(str, stringBuilder.toString());
                                        ret = false;
                                        if (c != null) {
                                        }
                                        return ret;
                                    } catch (Throwable th5) {
                                        th = th5;
                                        z = true;
                                        pkgName = pkgName2;
                                        ret2 = gameState3;
                                        al = appType2;
                                        i = priority;
                                        if (c != null) {
                                        }
                                        throw th;
                                    }
                                } catch (SQLException e4) {
                                    e = e4;
                                    z = true;
                                    pkgName = pkgName2;
                                    gameState = gameState3;
                                    al = appType2;
                                    i2 = userID2;
                                    str = DisplayEngineDBManager.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("DragInformationTableProcessor add a record time=");
                                    stringBuilder.append(time);
                                    stringBuilder.append(",start=");
                                    stringBuilder.append(start);
                                    stringBuilder.append(",stop=");
                                    stringBuilder.append(stop);
                                    stringBuilder.append(",al=");
                                    stringBuilder.append(al3);
                                    stringBuilder.append(",proximitypositive=");
                                    stringBuilder.append(cover3);
                                    stringBuilder.append(",userID=");
                                    stringBuilder.append(i2);
                                    stringBuilder.append(",appType=");
                                    stringBuilder.append(al);
                                    stringBuilder.append(",gameState=");
                                    stringBuilder.append(gameState);
                                    stringBuilder.append(",pkgName=");
                                    stringBuilder.append(pkgName);
                                    stringBuilder.append(", error:");
                                    stringBuilder.append(e.getMessage());
                                    DElog.e(str, stringBuilder.toString());
                                    ret = false;
                                    if (c != null) {
                                    }
                                    return ret;
                                } catch (Throwable th6) {
                                    th = th6;
                                    z = true;
                                    pkgName = pkgName2;
                                    ret2 = gameState3;
                                    al = appType2;
                                    i2 = userID2;
                                    i = priority;
                                    if (c != null) {
                                    }
                                    throw th;
                                }
                                try {
                                    String pkgName3;
                                    Cursor c2;
                                    StringBuilder stringBuilder2;
                                    values.put("APPTYPE", Integer.valueOf(al));
                                    gameState = gameState3;
                                    try {
                                        values.put("GAMESTATE", Integer.valueOf(gameState));
                                        pkgName3 = pkgName2;
                                        try {
                                            values.put("PACKAGE", pkgName3);
                                            if (c != null) {
                                                try {
                                                    if (c.getCount() > 0) {
                                                        c2 = c;
                                                        try {
                                                            String pkgName4 = pkgName3;
                                                            try {
                                                                int rows = DisplayEngineDBManager.this.mDatabase.update(DisplayEngineDBHelper.TABLE_NAME_DRAG_INFORMATION, values, "TIMESTAMP = ?", new String[]{String.valueOf(time)});
                                                                String str3;
                                                                if (rows == 0) {
                                                                    str3 = DisplayEngineDBManager.TAG;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("DragInformationTableProcessor update failed: time=");
                                                                    stringBuilder2.append(time);
                                                                    stringBuilder2.append(",priority=");
                                                                    stringBuilder2.append(priority);
                                                                    stringBuilder2.append(",start=");
                                                                    stringBuilder2.append(start);
                                                                    stringBuilder2.append(",stop=");
                                                                    stringBuilder2.append(stop);
                                                                    stringBuilder2.append(",al=");
                                                                    stringBuilder2.append(al3);
                                                                    stringBuilder2.append(",proximitypositive=");
                                                                    stringBuilder2.append(cover3);
                                                                    stringBuilder2.append(",userID=");
                                                                    stringBuilder2.append(i2);
                                                                    stringBuilder2.append(",appType=");
                                                                    stringBuilder2.append(al);
                                                                    stringBuilder2.append(",gameState=");
                                                                    stringBuilder2.append(gameState);
                                                                    stringBuilder2.append(",pkgName=");
                                                                    pkgName3 = pkgName4;
                                                                    stringBuilder2.append(pkgName3);
                                                                    DElog.e(str3, stringBuilder2.toString());
                                                                    gameState2 = false;
                                                                    ContentValues contentValues = values;
                                                                } else {
                                                                    pkgName3 = pkgName4;
                                                                    str3 = DisplayEngineDBManager.TAG;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("DragInformationTableProcessor update succ: rows=");
                                                                    stringBuilder2.append(rows);
                                                                    stringBuilder2.append(" time=");
                                                                    stringBuilder2.append(time);
                                                                    stringBuilder2.append(",priority=");
                                                                    stringBuilder2.append(priority);
                                                                    stringBuilder2.append(",start=");
                                                                    stringBuilder2.append(start);
                                                                    stringBuilder2.append(",stop=");
                                                                    stringBuilder2.append(stop);
                                                                    stringBuilder2.append(",al=");
                                                                    stringBuilder2.append(al3);
                                                                    stringBuilder2.append(",proximitypositive=");
                                                                    stringBuilder2.append(cover3);
                                                                    stringBuilder2.append(",userID=");
                                                                    stringBuilder2.append(i2);
                                                                    stringBuilder2.append(",appType=");
                                                                    stringBuilder2.append(al);
                                                                    stringBuilder2.append(",gameState=");
                                                                    stringBuilder2.append(gameState);
                                                                    stringBuilder2.append(",pkgName=");
                                                                    stringBuilder2.append(pkgName3);
                                                                    DElog.i(str3, stringBuilder2.toString());
                                                                    gameState2 = z;
                                                                }
                                                                ret = gameState2;
                                                                if (c2 != null) {
                                                                    c2.close();
                                                                }
                                                                i = priority;
                                                            } catch (SQLException e5) {
                                                                e = e5;
                                                                c = c2;
                                                                pkgName = pkgName4;
                                                                str = DisplayEngineDBManager.TAG;
                                                                stringBuilder = new StringBuilder();
                                                                stringBuilder.append("DragInformationTableProcessor add a record time=");
                                                                stringBuilder.append(time);
                                                                stringBuilder.append(",start=");
                                                                stringBuilder.append(start);
                                                                stringBuilder.append(",stop=");
                                                                stringBuilder.append(stop);
                                                                stringBuilder.append(",al=");
                                                                stringBuilder.append(al3);
                                                                stringBuilder.append(",proximitypositive=");
                                                                stringBuilder.append(cover3);
                                                                stringBuilder.append(",userID=");
                                                                stringBuilder.append(i2);
                                                                stringBuilder.append(",appType=");
                                                                stringBuilder.append(al);
                                                                stringBuilder.append(",gameState=");
                                                                stringBuilder.append(gameState);
                                                                stringBuilder.append(",pkgName=");
                                                                stringBuilder.append(pkgName);
                                                                stringBuilder.append(", error:");
                                                                stringBuilder.append(e.getMessage());
                                                                DElog.e(str, stringBuilder.toString());
                                                                ret = false;
                                                                if (c != null) {
                                                                }
                                                                return ret;
                                                            } catch (Throwable th7) {
                                                                th = th7;
                                                                i = priority;
                                                                c = c2;
                                                                pkgName = pkgName4;
                                                                if (c != null) {
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (SQLException e6) {
                                                            e = e6;
                                                            pkgName = pkgName3;
                                                            c = c2;
                                                            str = DisplayEngineDBManager.TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("DragInformationTableProcessor add a record time=");
                                                            stringBuilder.append(time);
                                                            stringBuilder.append(",start=");
                                                            stringBuilder.append(start);
                                                            stringBuilder.append(",stop=");
                                                            stringBuilder.append(stop);
                                                            stringBuilder.append(",al=");
                                                            stringBuilder.append(al3);
                                                            stringBuilder.append(",proximitypositive=");
                                                            stringBuilder.append(cover3);
                                                            stringBuilder.append(",userID=");
                                                            stringBuilder.append(i2);
                                                            stringBuilder.append(",appType=");
                                                            stringBuilder.append(al);
                                                            stringBuilder.append(",gameState=");
                                                            stringBuilder.append(gameState);
                                                            stringBuilder.append(",pkgName=");
                                                            stringBuilder.append(pkgName);
                                                            stringBuilder.append(", error:");
                                                            stringBuilder.append(e.getMessage());
                                                            DElog.e(str, stringBuilder.toString());
                                                            ret = false;
                                                            if (c != null) {
                                                            }
                                                            return ret;
                                                        } catch (Throwable th8) {
                                                            th = th8;
                                                            pkgName = pkgName3;
                                                            i = priority;
                                                            c = c2;
                                                            if (c != null) {
                                                            }
                                                            throw th;
                                                        }
                                                    }
                                                } catch (SQLException e7) {
                                                    e = e7;
                                                    c2 = c;
                                                    pkgName = pkgName3;
                                                    str = DisplayEngineDBManager.TAG;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("DragInformationTableProcessor add a record time=");
                                                    stringBuilder.append(time);
                                                    stringBuilder.append(",start=");
                                                    stringBuilder.append(start);
                                                    stringBuilder.append(",stop=");
                                                    stringBuilder.append(stop);
                                                    stringBuilder.append(",al=");
                                                    stringBuilder.append(al3);
                                                    stringBuilder.append(",proximitypositive=");
                                                    stringBuilder.append(cover3);
                                                    stringBuilder.append(",userID=");
                                                    stringBuilder.append(i2);
                                                    stringBuilder.append(",appType=");
                                                    stringBuilder.append(al);
                                                    stringBuilder.append(",gameState=");
                                                    stringBuilder.append(gameState);
                                                    stringBuilder.append(",pkgName=");
                                                    stringBuilder.append(pkgName);
                                                    stringBuilder.append(", error:");
                                                    stringBuilder.append(e.getMessage());
                                                    DElog.e(str, stringBuilder.toString());
                                                    ret = false;
                                                    if (c != null) {
                                                    }
                                                    return ret;
                                                } catch (Throwable th9) {
                                                    th = th9;
                                                    c2 = c;
                                                    pkgName = pkgName3;
                                                    i = priority;
                                                    if (c != null) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                            c2 = c;
                                        } catch (SQLException e8) {
                                            e = e8;
                                            pkgName = pkgName3;
                                            str = DisplayEngineDBManager.TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("DragInformationTableProcessor add a record time=");
                                            stringBuilder.append(time);
                                            stringBuilder.append(",start=");
                                            stringBuilder.append(start);
                                            stringBuilder.append(",stop=");
                                            stringBuilder.append(stop);
                                            stringBuilder.append(",al=");
                                            stringBuilder.append(al3);
                                            stringBuilder.append(",proximitypositive=");
                                            stringBuilder.append(cover3);
                                            stringBuilder.append(",userID=");
                                            stringBuilder.append(i2);
                                            stringBuilder.append(",appType=");
                                            stringBuilder.append(al);
                                            stringBuilder.append(",gameState=");
                                            stringBuilder.append(gameState);
                                            stringBuilder.append(",pkgName=");
                                            stringBuilder.append(pkgName);
                                            stringBuilder.append(", error:");
                                            stringBuilder.append(e.getMessage());
                                            DElog.e(str, stringBuilder.toString());
                                            ret = false;
                                            if (c != null) {
                                                c.close();
                                            }
                                            return ret;
                                        } catch (Throwable th10) {
                                            th = th10;
                                            pkgName = pkgName3;
                                            i = priority;
                                            if (c != null) {
                                                c.close();
                                            }
                                            throw th;
                                        }
                                    } catch (SQLException e9) {
                                        e = e9;
                                        pkgName = pkgName2;
                                        str = DisplayEngineDBManager.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("DragInformationTableProcessor add a record time=");
                                        stringBuilder.append(time);
                                        stringBuilder.append(",start=");
                                        stringBuilder.append(start);
                                        stringBuilder.append(",stop=");
                                        stringBuilder.append(stop);
                                        stringBuilder.append(",al=");
                                        stringBuilder.append(al3);
                                        stringBuilder.append(",proximitypositive=");
                                        stringBuilder.append(cover3);
                                        stringBuilder.append(",userID=");
                                        stringBuilder.append(i2);
                                        stringBuilder.append(",appType=");
                                        stringBuilder.append(al);
                                        stringBuilder.append(",gameState=");
                                        stringBuilder.append(gameState);
                                        stringBuilder.append(",pkgName=");
                                        stringBuilder.append(pkgName);
                                        stringBuilder.append(", error:");
                                        stringBuilder.append(e.getMessage());
                                        DElog.e(str, stringBuilder.toString());
                                        ret = false;
                                        if (c != null) {
                                        }
                                        return ret;
                                    } catch (Throwable th11) {
                                        th = th11;
                                        pkgName = pkgName2;
                                        i = priority;
                                        if (c != null) {
                                        }
                                        throw th;
                                    }
                                    try {
                                        ContentValues values2 = values;
                                        long rowID = DisplayEngineDBManager.this.mDatabase.insert(DisplayEngineDBHelper.TABLE_NAME_DRAG_INFORMATION, null, values2);
                                        if (rowID == -1) {
                                            str2 = DisplayEngineDBManager.TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("DragInformationTableProcessor insert failed: time=");
                                            stringBuilder2.append(time);
                                            stringBuilder2.append(",priority=");
                                            stringBuilder2.append(priority);
                                            stringBuilder2.append(",start=");
                                            stringBuilder2.append(start);
                                            stringBuilder2.append(",stop=");
                                            stringBuilder2.append(stop);
                                            stringBuilder2.append(",al=");
                                            stringBuilder2.append(al3);
                                            stringBuilder2.append(",proximitypositive=");
                                            stringBuilder2.append(cover3);
                                            stringBuilder2.append(",userID=");
                                            stringBuilder2.append(i2);
                                            stringBuilder2.append(",appType=");
                                            stringBuilder2.append(al);
                                            stringBuilder2.append(",gameState=");
                                            stringBuilder2.append(gameState);
                                            stringBuilder2.append(",pkgName=");
                                            stringBuilder2.append(pkgName3);
                                            DElog.e(str2, stringBuilder2.toString());
                                            ret = false;
                                            if (c2 != null) {
                                            }
                                            i = priority;
                                        } else {
                                            long rowID2 = rowID;
                                            ContentValues contentValues2 = values2;
                                            str2 = DisplayEngineDBManager.TAG;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("DragInformationTableProcessor add a record(");
                                            String pkgName5 = pkgName3;
                                            try {
                                                stringBuilder3.append(rowID2);
                                                stringBuilder3.append(") succ: time=");
                                                stringBuilder3.append(time);
                                                stringBuilder3.append(",priority=");
                                                stringBuilder3.append(priority);
                                                stringBuilder3.append(",start=");
                                                stringBuilder3.append(start);
                                                stringBuilder3.append(",stop=");
                                                stringBuilder3.append(stop);
                                                stringBuilder3.append(",al=");
                                                stringBuilder3.append(al3);
                                                stringBuilder3.append(",proximitypositive=");
                                                stringBuilder3.append(cover3);
                                                stringBuilder3.append(",userID=");
                                                stringBuilder3.append(i2);
                                                stringBuilder3.append(",appType=");
                                                stringBuilder3.append(al);
                                                stringBuilder3.append(",gameState=");
                                                stringBuilder3.append(gameState);
                                                stringBuilder3.append(",pkgName=");
                                                pkgName = pkgName5;
                                            } catch (SQLException e10) {
                                                e = e10;
                                                c = c2;
                                                pkgName = pkgName5;
                                                str = DisplayEngineDBManager.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("DragInformationTableProcessor add a record time=");
                                                stringBuilder.append(time);
                                                stringBuilder.append(",start=");
                                                stringBuilder.append(start);
                                                stringBuilder.append(",stop=");
                                                stringBuilder.append(stop);
                                                stringBuilder.append(",al=");
                                                stringBuilder.append(al3);
                                                stringBuilder.append(",proximitypositive=");
                                                stringBuilder.append(cover3);
                                                stringBuilder.append(",userID=");
                                                stringBuilder.append(i2);
                                                stringBuilder.append(",appType=");
                                                stringBuilder.append(al);
                                                stringBuilder.append(",gameState=");
                                                stringBuilder.append(gameState);
                                                stringBuilder.append(",pkgName=");
                                                stringBuilder.append(pkgName);
                                                stringBuilder.append(", error:");
                                                stringBuilder.append(e.getMessage());
                                                DElog.e(str, stringBuilder.toString());
                                                ret = false;
                                                if (c != null) {
                                                }
                                                return ret;
                                            } catch (Throwable th12) {
                                                th = th12;
                                                c = c2;
                                                pkgName = pkgName5;
                                                i = priority;
                                                if (c != null) {
                                                }
                                                throw th;
                                            }
                                            try {
                                                stringBuilder3.append(pkgName);
                                                DElog.i(str2, stringBuilder3.toString());
                                                ret = z;
                                                if (c2 != null) {
                                                }
                                                i = priority;
                                            } catch (SQLException e11) {
                                                e = e11;
                                                c = c2;
                                                str = DisplayEngineDBManager.TAG;
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("DragInformationTableProcessor add a record time=");
                                                stringBuilder.append(time);
                                                stringBuilder.append(",start=");
                                                stringBuilder.append(start);
                                                stringBuilder.append(",stop=");
                                                stringBuilder.append(stop);
                                                stringBuilder.append(",al=");
                                                stringBuilder.append(al3);
                                                stringBuilder.append(",proximitypositive=");
                                                stringBuilder.append(cover3);
                                                stringBuilder.append(",userID=");
                                                stringBuilder.append(i2);
                                                stringBuilder.append(",appType=");
                                                stringBuilder.append(al);
                                                stringBuilder.append(",gameState=");
                                                stringBuilder.append(gameState);
                                                stringBuilder.append(",pkgName=");
                                                stringBuilder.append(pkgName);
                                                stringBuilder.append(", error:");
                                                stringBuilder.append(e.getMessage());
                                                DElog.e(str, stringBuilder.toString());
                                                ret = false;
                                                if (c != null) {
                                                }
                                                return ret;
                                            } catch (Throwable th13) {
                                                th = th13;
                                                c = c2;
                                                i = priority;
                                                if (c != null) {
                                                }
                                                throw th;
                                            }
                                        }
                                    } catch (SQLException e12) {
                                        e = e12;
                                        pkgName = pkgName3;
                                        c = c2;
                                        str = DisplayEngineDBManager.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("DragInformationTableProcessor add a record time=");
                                        stringBuilder.append(time);
                                        stringBuilder.append(",start=");
                                        stringBuilder.append(start);
                                        stringBuilder.append(",stop=");
                                        stringBuilder.append(stop);
                                        stringBuilder.append(",al=");
                                        stringBuilder.append(al3);
                                        stringBuilder.append(",proximitypositive=");
                                        stringBuilder.append(cover3);
                                        stringBuilder.append(",userID=");
                                        stringBuilder.append(i2);
                                        stringBuilder.append(",appType=");
                                        stringBuilder.append(al);
                                        stringBuilder.append(",gameState=");
                                        stringBuilder.append(gameState);
                                        stringBuilder.append(",pkgName=");
                                        stringBuilder.append(pkgName);
                                        stringBuilder.append(", error:");
                                        stringBuilder.append(e.getMessage());
                                        DElog.e(str, stringBuilder.toString());
                                        ret = false;
                                        if (c != null) {
                                        }
                                        return ret;
                                    } catch (Throwable th14) {
                                        th = th14;
                                        pkgName = pkgName3;
                                        c = c2;
                                        i = priority;
                                        if (c != null) {
                                        }
                                        throw th;
                                    }
                                } catch (SQLException e13) {
                                    e = e13;
                                    pkgName = pkgName2;
                                    gameState = gameState3;
                                    str = DisplayEngineDBManager.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("DragInformationTableProcessor add a record time=");
                                    stringBuilder.append(time);
                                    stringBuilder.append(",start=");
                                    stringBuilder.append(start);
                                    stringBuilder.append(",stop=");
                                    stringBuilder.append(stop);
                                    stringBuilder.append(",al=");
                                    stringBuilder.append(al3);
                                    stringBuilder.append(",proximitypositive=");
                                    stringBuilder.append(cover3);
                                    stringBuilder.append(",userID=");
                                    stringBuilder.append(i2);
                                    stringBuilder.append(",appType=");
                                    stringBuilder.append(al);
                                    stringBuilder.append(",gameState=");
                                    stringBuilder.append(gameState);
                                    stringBuilder.append(",pkgName=");
                                    stringBuilder.append(pkgName);
                                    stringBuilder.append(", error:");
                                    stringBuilder.append(e.getMessage());
                                    DElog.e(str, stringBuilder.toString());
                                    ret = false;
                                    if (c != null) {
                                    }
                                    return ret;
                                } catch (Throwable th15) {
                                    th = th15;
                                    pkgName = pkgName2;
                                    ret2 = gameState3;
                                    i = priority;
                                    if (c != null) {
                                    }
                                    throw th;
                                }
                            } catch (SQLException e14) {
                                e = e14;
                                z = true;
                                pkgName = pkgName2;
                                gameState = gameState3;
                                al = appType2;
                                i2 = userID2;
                                al3 = al2;
                                cover3 = cover2;
                                str = DisplayEngineDBManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("DragInformationTableProcessor add a record time=");
                                stringBuilder.append(time);
                                stringBuilder.append(",start=");
                                stringBuilder.append(start);
                                stringBuilder.append(",stop=");
                                stringBuilder.append(stop);
                                stringBuilder.append(",al=");
                                stringBuilder.append(al3);
                                stringBuilder.append(",proximitypositive=");
                                stringBuilder.append(cover3);
                                stringBuilder.append(",userID=");
                                stringBuilder.append(i2);
                                stringBuilder.append(",appType=");
                                stringBuilder.append(al);
                                stringBuilder.append(",gameState=");
                                stringBuilder.append(gameState);
                                stringBuilder.append(",pkgName=");
                                stringBuilder.append(pkgName);
                                stringBuilder.append(", error:");
                                stringBuilder.append(e.getMessage());
                                DElog.e(str, stringBuilder.toString());
                                ret = false;
                                if (c != null) {
                                }
                                return ret;
                            } catch (Throwable th16) {
                                th = th16;
                                z = true;
                                pkgName = pkgName2;
                                ret2 = gameState3;
                                al = appType2;
                                i2 = userID2;
                                al3 = al2;
                                cover3 = cover2;
                                i = priority;
                                if (c != null) {
                                }
                                throw th;
                            }
                        }
                        return false;
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "DragInformationTableProcessor.addorUpdateRecord() mDatabase error or Invalid input!");
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:28:0x0095, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:38:0x0150, code skipped:
            if (r5 != null) goto L_0x0152;
     */
        /* JADX WARNING: Missing block: B:40:?, code skipped:
            r5.close();
     */
        /* JADX WARNING: Missing block: B:44:0x0172, code skipped:
            if (r5 == null) goto L_0x0175;
     */
        /* JADX WARNING: Missing block: B:47:0x0176, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ArrayList<Bundle> getAllRecords(Bundle info) {
            SQLException e;
            ArrayList<Bundle> records = new ArrayList();
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (info != null) {
                        int userID = info.getInt("UserID");
                        if (userID < 0) {
                            String str = DisplayEngineDBManager.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("DragInformationTableProcessor.getAllRecords() invalid input: userID=");
                            stringBuilder.append(userID);
                            DElog.e(str, stringBuilder.toString());
                            return null;
                        }
                        int numLimit = info.getInt(QueryInfoKey.NUMBERLIMIT, -1);
                        Cursor c = null;
                        if (numLimit < 1) {
                            try {
                                c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM UserDragInformation where USERID = ? ORDER BY _id ASC", new String[]{String.valueOf(userID)});
                            } catch (SQLException e2) {
                                records = null;
                                try {
                                    String str2 = DisplayEngineDBManager.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("DragInformationTableProcessor.getAllRecords() error:");
                                    stringBuilder2.append(e2.getMessage());
                                    DElog.w(str2, stringBuilder2.toString());
                                } catch (Throwable th) {
                                    if (c != null) {
                                        c.close();
                                    }
                                }
                            }
                        } else {
                            SQLiteDatabase access$200 = DisplayEngineDBManager.this.mDatabase;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("SELECT * FROM UserDragInformation where USERID = ? ORDER BY _id DESC LIMIT ");
                            stringBuilder3.append(numLimit);
                            c = access$200.rawQuery(stringBuilder3.toString(), new String[]{String.valueOf(userID)});
                        }
                        if (c == null) {
                            DElog.e(DisplayEngineDBManager.TAG, "DragInformationTableProcessor.getAllRecords() query database error.");
                            if (c != null) {
                                c.close();
                            }
                        } else {
                            while (c.moveToNext()) {
                                e2 = new Bundle();
                                e2.putLong(DragInformationKey._ID, c.getLong(c.getColumnIndex("_id")));
                                e2.putLong("TimeStamp", c.getLong(c.getColumnIndex("TIMESTAMP")));
                                e2.putInt(DragInformationKey.PRIORITY, c.getInt(c.getColumnIndex("PRIORITY")));
                                e2.putFloat(DragInformationKey.STARTPOINT, c.getFloat(c.getColumnIndex("STARTPOINT")));
                                e2.putFloat(DragInformationKey.STOPPOINT, c.getFloat(c.getColumnIndex("STOPPOINT")));
                                e2.putInt("AmbientLight", c.getInt(c.getColumnIndex("AL")));
                                e2.putBoolean(DragInformationKey.PROXIMITYPOSITIVE, c.getInt(c.getColumnIndex("PROXIMITYPOSITIVE")) == 1);
                                e2.putInt("UserID", c.getInt(c.getColumnIndex("USERID")));
                                e2.putInt("AppType", c.getInt(c.getColumnIndex("APPTYPE")));
                                e2.putInt(DragInformationKey.GAMESTATE, c.getInt(c.getColumnIndex("GAMESTATE")));
                                e2.putString(DragInformationKey.PACKAGE, c.getString(c.getColumnIndex("PACKAGE")));
                                records.add(e2);
                            }
                        }
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "DragInformationTableProcessor.getAllRecords() mDatabase error or Invalid input!");
                return null;
            }
        }

        protected int getSizeWithoutLock(Bundle info) {
            int size = 0;
            if (DisplayEngineDBManager.this.mDatabase == null || !DisplayEngineDBManager.this.mDatabase.isOpen() || info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "DragInformationTableProcessor.getSizeWithoutLock() mDatabase error.");
                return 0;
            }
            int userID = info.getInt("UserID");
            String str;
            if (userID < 0) {
                str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DragInformationTableProcessor.getSizeWithoutLock() invalid input: userID=");
                stringBuilder.append(userID);
                DElog.e(str, stringBuilder.toString());
                return 0;
            }
            str = new StringBuilder();
            str.append("SELECT COUNT(*) FROM UserDragInformation where USERID = ");
            str.append(userID);
            String str2;
            StringBuilder stringBuilder2;
            try {
                size = (int) DisplayEngineDBManager.this.mDatabase.compileStatement(str.toString()).simpleQueryForLong();
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DragInformationTableProcessor.getSizeWithoutLock() return ");
                stringBuilder2.append(size);
                DElog.d(str2, stringBuilder2.toString());
                return size;
            } catch (SQLException e) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DragInformationTableProcessor.getSizeWithoutLock() error:");
                stringBuilder2.append(e.getMessage());
                DElog.w(str2, stringBuilder2.toString());
                return size;
            }
        }

        protected boolean deleteRecordsWithoutLock(Bundle info, int count) {
            boolean ret = false;
            if (!DisplayEngineDBManager.this.checkDatabaseStatusIsOk() || info == null || count <= 0) {
                String str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DragInformationTableProcessor.deleteRecordsWithoutLock() mDatabase error, info is null or count=");
                stringBuilder.append(count);
                DElog.e(str, stringBuilder.toString());
                return false;
            }
            int userID = info.getInt("UserID");
            String str2;
            StringBuilder stringBuilder2;
            if (userID < 0) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DragInformationTableProcessor.deleteRecordsWithoutLock() invalid input: userID=");
                stringBuilder2.append(userID);
                DElog.e(str2, stringBuilder2.toString());
                return false;
            }
            try {
                int rows = DisplayEngineDBManager.this.mDatabase;
                str2 = DisplayEngineDBHelper.TABLE_NAME_DRAG_INFORMATION;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("_id IN(SELECT _id FROM UserDragInformation where USERID = ");
                stringBuilder2.append(userID);
                stringBuilder2.append(" ORDER BY PRIORITY DESC, _id ASC LIMIT ");
                stringBuilder2.append(count);
                stringBuilder2.append(")");
                rows = rows.delete(str2, stringBuilder2.toString(), null);
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DragInformationTableProcessor.deleteRecordsWithoutLock(userID=");
                stringBuilder2.append(userID);
                stringBuilder2.append(", count=");
                stringBuilder2.append(count);
                stringBuilder2.append(") sucess. Delete ");
                stringBuilder2.append(rows);
                stringBuilder2.append(" records.");
                DElog.i(str2, stringBuilder2.toString());
                ret = true;
            } catch (SQLException e) {
                str2 = DisplayEngineDBManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DragInformationTableProcessor.deleteRecordsWithoutLock() error:");
                stringBuilder2.append(e.getMessage());
                DElog.w(str2, stringBuilder2.toString());
            }
            return ret;
        }
    }

    private class UserPreferencesTableProcessor extends TableProcessor {
        private static final int mMaxSegmentLength = 255;

        public UserPreferencesTableProcessor() {
            super();
        }

        private boolean pretreatmentForAddorUpdateRecord(Bundle data, int userID, int appType, int[] alValues, int[] deltaValues) {
            String str;
            StringBuilder stringBuilder;
            if (userID < 0 || appType < 0 || alValues == null || deltaValues == null || alValues.length != deltaValues.length || alValues.length > 255 || (this.mMaxSize > 0 && alValues.length > this.mMaxSize)) {
                if (alValues == null || deltaValues == null) {
                    DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.addorUpdateRecord error: al=null or delta=null");
                } else {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UserPreferencesTableProcessor.addorUpdateRecord error: userID=");
                    stringBuilder.append(userID);
                    stringBuilder.append(" appType=");
                    stringBuilder.append(appType);
                    stringBuilder.append(" al size=");
                    stringBuilder.append(alValues.length);
                    stringBuilder.append(" delta size=");
                    stringBuilder.append(deltaValues.length);
                    stringBuilder.append(" max size=");
                    stringBuilder.append(this.mMaxSize);
                    DElog.e(str, stringBuilder.toString());
                }
                return false;
            } else if (clearWithoutLock(data)) {
                return true;
            } else {
                str = DisplayEngineDBManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("UserPreferencesTableProcessor.addorUpdateRecord(userID=");
                stringBuilder.append(userID);
                stringBuilder.append(", appType=");
                stringBuilder.append(appType);
                stringBuilder.append(") error: clear last records!");
                DElog.e(str, stringBuilder.toString());
                return false;
            }
        }

        public boolean addorUpdateRecord(Bundle data) {
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    if (data != null) {
                        int userID = data.getInt("UserID", -1);
                        int appType = data.getInt("AppType", -1);
                        int[] alValues = data.getIntArray("AmbientLight");
                        int[] deltaValues = data.getIntArray(UserPreferencesKey.DELTA);
                        if (pretreatmentForAddorUpdateRecord(data, userID, appType, alValues, deltaValues)) {
                            int i;
                            StringBuilder stringBuilder;
                            String str;
                            try {
                                StringBuffer text = new StringBuffer();
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("UserPreferencesTableProcessor add record succ: userID=");
                                stringBuilder2.append(userID);
                                stringBuilder2.append(", appType=");
                                stringBuilder2.append(appType);
                                stringBuilder2.append(", segment={");
                                text.append(stringBuilder2.toString());
                                int id = (userID << 16) + (appType << 8);
                                for (i = 0; i < alValues.length; i++) {
                                    DisplayEngineDBManager.this.mDatabase.execSQL("INSERT INTO UserPreferences VALUES(?, ?, ?, ?, ?)", new Object[]{Integer.valueOf((id + i) + 1), Integer.valueOf(userID), Integer.valueOf(appType), Integer.valueOf(alValues[i]), Integer.valueOf(deltaValues[i])});
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(alValues[i]);
                                    stringBuilder.append(",");
                                    stringBuilder.append(deltaValues[i]);
                                    stringBuilder.append(";");
                                    text.append(stringBuilder.toString());
                                }
                                str = DisplayEngineDBManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(text.toString());
                                stringBuilder.append("}");
                                DElog.i(str, stringBuilder.toString());
                                return true;
                            } catch (SQLException e) {
                                StringBuffer text2 = new StringBuffer();
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("UserPreferencesTableProcessor add record userID=");
                                stringBuilder3.append(userID);
                                stringBuilder3.append(", appType=");
                                stringBuilder3.append(appType);
                                stringBuilder3.append(", segment={");
                                text2.append(stringBuilder3.toString());
                                for (i = 0; i < alValues.length; i++) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(alValues[i]);
                                    stringBuilder.append(",");
                                    stringBuilder.append(deltaValues[i]);
                                    stringBuilder.append(";");
                                    text2.append(stringBuilder.toString());
                                }
                                str = DisplayEngineDBManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(text2.toString());
                                stringBuilder.append("}, error:");
                                stringBuilder.append(e.getMessage());
                                DElog.e(str, stringBuilder.toString());
                                return false;
                            }
                        }
                        return false;
                    }
                }
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.addorUpdateRecord error: Invalid input!");
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:24:0x006a, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:30:0x0098, code skipped:
            if (r5 != null) goto L_0x009a;
     */
        /* JADX WARNING: Missing block: B:32:?, code skipped:
            r5.close();
     */
        /* JADX WARNING: Missing block: B:38:0x00bc, code skipped:
            if (r5 == null) goto L_0x00bf;
     */
        /* JADX WARNING: Missing block: B:41:0x00c0, code skipped:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ArrayList<Bundle> getAllRecords(Bundle info) {
            if (info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.getAllRecords invalid input: info=null");
                return null;
            }
            int userID = info.getInt("UserID", -1);
            int appType = info.getInt("AppType", -1);
            if (userID < 0 || appType < 0) {
                String str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UserPreferencesTableProcessor.getAllRecords invalid input: userID=");
                stringBuilder.append(userID);
                stringBuilder.append(" appType=");
                stringBuilder.append(appType);
                DElog.e(str, stringBuilder.toString());
                return null;
            }
            ArrayList<Bundle> records = new ArrayList();
            synchronized (DisplayEngineDBManager.this.mdbLock) {
                if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                    Cursor c = null;
                    try {
                        c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM UserPreferences WHERE USERID = ? AND APPTYPE = ?", new String[]{String.valueOf(userID), String.valueOf(appType)});
                        if (c == null) {
                            DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.getAllRecords() query database error.");
                            if (c != null) {
                                c.close();
                            }
                        } else {
                            while (c.moveToNext()) {
                                Bundle record = new Bundle();
                                record.putInt("AmbientLight", c.getInt(c.getColumnIndex("AL")));
                                record.putInt(UserPreferencesKey.DELTA, c.getInt(c.getColumnIndex("DELTA")));
                                records.add(record);
                            }
                        }
                    } catch (SQLException e) {
                        records = null;
                        try {
                            String str2 = DisplayEngineDBManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("UserPreferencesTableProcessor.getAllRecords() error:");
                            stringBuilder2.append(e.getMessage());
                            DElog.w(str2, stringBuilder2.toString());
                        } catch (Throwable th) {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                } else {
                    DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.getAllRecords() mDatabase error.");
                    return null;
                }
            }
        }

        /* JADX WARNING: Missing block: B:19:0x0074, code skipped:
            if (r4 != null) goto L_0x0076;
     */
        /* JADX WARNING: Missing block: B:20:0x0076, code skipped:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:25:0x0097, code skipped:
            if (r4 == null) goto L_0x00a8;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected int getSizeWithoutLock(Bundle info) {
            if (info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.getSizeWithoutLock() invalid input: info=null");
                return 0;
            }
            int userID = info.getInt("UserID", -1);
            int appType = info.getInt("AppType", -1);
            if (userID < 0 || appType < 0) {
                String str = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UserPreferencesTableProcessor.getSizeWithoutLock() invalid input: userID=");
                stringBuilder.append(userID);
                stringBuilder.append(" appType=");
                stringBuilder.append(appType);
                DElog.e(str, stringBuilder.toString());
                return 0;
            }
            int size = 0;
            if (DisplayEngineDBManager.this.mDatabase == null || !DisplayEngineDBManager.this.mDatabase.isOpen()) {
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.getSizeWithoutLock() mDatabase error.");
            } else {
                Cursor c = null;
                try {
                    c = DisplayEngineDBManager.this.mDatabase.rawQuery("SELECT * FROM UserPreferences where (USERID = ?) and (APPTYPE = ?)", new String[]{String.valueOf(userID), String.valueOf(appType)});
                    if (c != null && c.getCount() > 0) {
                        size = c.getCount();
                    }
                    String str2 = DisplayEngineDBManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("UserPreferencesTableProcessor.getSizeWithoutLock() return ");
                    stringBuilder2.append(size);
                    DElog.i(str2, stringBuilder2.toString());
                } catch (SQLException e) {
                    String str3 = DisplayEngineDBManager.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("UserPreferencesTableProcessor.getSizeWithoutLock() error:");
                    stringBuilder3.append(e.getMessage());
                    DElog.w(str3, stringBuilder3.toString());
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                    throw th;
                }
            }
            return size;
        }

        protected boolean clearWithoutLock(Bundle info) {
            String str;
            StringBuilder stringBuilder;
            if (info == null) {
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.clearWithoutLock() invalid input: info=null");
                return false;
            }
            int userID = info.getInt("UserID", -1);
            int appType = info.getInt("AppType", -1);
            if (userID < 0 || appType < 0) {
                String str2 = DisplayEngineDBManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UserPreferencesTableProcessor.clearWithoutLock() invalid input: userID=");
                stringBuilder2.append(userID);
                stringBuilder2.append(" appType=");
                stringBuilder2.append(appType);
                DElog.e(str2, stringBuilder2.toString());
                return false;
            } else if (DisplayEngineDBManager.this.checkDatabaseStatusIsOk()) {
                boolean ret = false;
                try {
                    if (getSizeWithoutLock(info) > 0) {
                        DisplayEngineDBManager.this.mDatabase.execSQL("DELETE FROM UserPreferences where (USERID = ?) and (APPTYPE = ?)", new Object[]{Integer.valueOf(userID), Integer.valueOf(appType)});
                    }
                    DElog.i(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.clearWithoutLock() sucess.");
                    ret = true;
                } catch (SQLException e) {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UserPreferencesTableProcessor.clearWithoutLock() error:");
                    stringBuilder.append(e.getMessage());
                    DElog.w(str, stringBuilder.toString());
                } catch (IllegalArgumentException e2) {
                    str = DisplayEngineDBManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("UserPreferencesTableProcessor.clearWithoutLock() error:");
                    stringBuilder.append(e2.getMessage());
                    DElog.w(str, stringBuilder.toString());
                }
                return ret;
            } else {
                DElog.e(DisplayEngineDBManager.TAG, "UserPreferencesTableProcessor.clearWithoutLock() mDatabase error.");
                return false;
            }
        }
    }

    private DisplayEngineDBManager(Context context) {
        mTableProcessors.put(DragInformationKey.TAG, new DragInformationTableProcessor());
        mTableProcessors.put(UserPreferencesKey.TAG, new UserPreferencesTableProcessor());
        mTableProcessors.put("BrightnessCurveLow", new BrightnessCurveTableProcessor("BrightnessCurveLow"));
        mTableProcessors.put("BrightnessCurveMiddle", new BrightnessCurveTableProcessor("BrightnessCurveMiddle"));
        mTableProcessors.put("BrightnessCurveHigh", new BrightnessCurveTableProcessor("BrightnessCurveHigh"));
        mTableProcessors.put("BrightnessCurveDefault", new BrightnessCurveTableProcessor("BrightnessCurveDefault"));
        mTableProcessors.put("AlgorithmESCW", new AlgorithmESCWTableProcessor());
        mTableProcessors.put("DataCleaner", new DataCleanerTableProcessor());
        this.mHelper = new DisplayEngineDBHelper(context);
        openDatabase();
    }

    public static DisplayEngineDBManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (mLock) {
                if (mInstance == null) {
                    mInstance = new DisplayEngineDBManager(context);
                }
            }
        }
        return mInstance;
    }

    public boolean setMaxSize(String name, int size) {
        TableProcessor processor = (TableProcessor) mTableProcessors.get(name);
        if (processor != null && size > 0) {
            return processor.setMaxSize(size);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid input for setMaxSize(");
        stringBuilder.append(name);
        stringBuilder.append(") size=");
        stringBuilder.append(size);
        DElog.e(str, stringBuilder.toString());
        return false;
    }

    public boolean addorUpdateRecord(String name, Bundle data) {
        TableProcessor processor = (TableProcessor) mTableProcessors.get(name);
        if (processor != null && data != null) {
            return processor.addorUpdateRecord(data);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid input for addorUpdateRecord:");
        stringBuilder.append(name);
        stringBuilder.append(" is not support or data is null!");
        DElog.e(str, stringBuilder.toString());
        return false;
    }

    public int getSize(String name, Bundle info) {
        TableProcessor processor = (TableProcessor) mTableProcessors.get(name);
        if (processor != null) {
            return processor.getSize(info);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid input for getSize:");
        stringBuilder.append(name);
        stringBuilder.append(" is not support!");
        DElog.e(str, stringBuilder.toString());
        return 0;
    }

    public int getSize(String name) {
        return getSize(name, null);
    }

    public ArrayList<Bundle> getAllRecords(String name, Bundle info) {
        TableProcessor processor = (TableProcessor) mTableProcessors.get(name);
        if (processor != null) {
            return processor.getAllRecords(info);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid input for getAllRecords:");
        stringBuilder.append(name);
        stringBuilder.append(" is not support!");
        DElog.e(str, stringBuilder.toString());
        return null;
    }

    public ArrayList<Bundle> getAllRecords(String name) {
        return getAllRecords(name, null);
    }

    private void openDatabase() {
        try {
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to open DisplayEngine.db error:");
            stringBuilder.append(e.getMessage());
            DElog.e(str, stringBuilder.toString());
        }
    }

    private boolean checkDatabaseStatusIsOk() {
        if (this.mDatabase == null || !this.mDatabase.isOpen()) {
            openDatabase();
        }
        return this.mDatabase != null && this.mDatabase.isOpen();
    }
}

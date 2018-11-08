package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.huawei.android.pushagent.a.a.c;

public class b extends SQLiteOpenHelper {
    private static b a = null;
    private static b b = null;

    private b(Context context) {
        super(context, "push.db", null, 1);
        c.a("PushSelfShowLog", "DBHelper instance, version is 1");
    }

    private b(Context context, String str) {
        super(context, str, null, 1);
        c.a("PushSelfShowLog", "DBHelper instance, version is 1");
    }

    public static synchronized b a(Context context) {
        synchronized (b.class) {
            if (a == null) {
                a = new b(context);
                b bVar = a;
                return bVar;
            }
            bVar = a;
            return bVar;
        }
    }

    public static synchronized b a(Context context, String str) {
        synchronized (b.class) {
            if (b == null) {
                b = new b(context, str);
                b bVar = b;
                return bVar;
            }
            bVar = b;
            return bVar;
        }
    }

    private void a(SQLiteDatabase sQLiteDatabase) {
        c.a("PushSelfShowLog", "updateVersionFrom0To1");
        Object -l_2_R;
        try {
            -l_2_R = new ContentValues();
            -l_2_R.put("token", " ".getBytes("UTF-8"));
            sQLiteDatabase.update("pushmsg", -l_2_R, null, null);
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", -l_2_R2.toString(), -l_2_R2);
        }
    }

    private boolean a(SQLiteDatabase sQLiteDatabase, String str) {
        Cursor cursor;
        int -l_6_I = 0;
        if (sQLiteDatabase == null) {
            return false;
        }
        Object -l_3_R = "(tbl_name='" + str + "')";
        cursor = null;
        try {
            Object -l_4_R = sQLiteDatabase.query("sqlite_master", null, -l_3_R, null, null, null, null);
            if (-l_4_R == null) {
                if (-l_4_R != null) {
                    -l_4_R.close();
                }
                return false;
            }
            -l_4_R.moveToFirst();
            if (-l_4_R.getCount() > 0) {
                -l_6_I = 1;
            }
            if (-l_4_R != null) {
                -l_4_R.close();
            }
            return -l_6_I;
        } catch (Object -l_5_R) {
            c.d("PushSelfShowLog", -l_5_R.toString(), -l_5_R);
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        c.a("PushSelfShowLog", "onCreate");
        if (a(sQLiteDatabase, "pushmsg")) {
            c.a("PushSelfShowLog", "old table is exist");
            onUpgrade(sQLiteDatabase, 0, 1);
            return;
        }
        try {
            sQLiteDatabase.execSQL("create table notify(url  TEXT  PRIMARY KEY , bmp  BLOB );");
            sQLiteDatabase.execSQL("create table pushmsg( _id INTEGER PRIMARY KEY AUTOINCREMENT, url  TEXT  , token  BLOB ,msg  BLOB );");
        } catch (Object -l_2_R) {
            c.d("PushSelfShowLog", -l_2_R.toString(), -l_2_R);
        }
    }

    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        c.a("PushSelfShowLog", "onDowngrade,oldVersion:" + i + ",newVersion:" + i2);
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        c.a("PushSelfShowLog", "onUpgrade,oldVersion:" + i + ",newVersion:" + i2);
        if (i == 0) {
            a(sQLiteDatabase);
        }
    }
}

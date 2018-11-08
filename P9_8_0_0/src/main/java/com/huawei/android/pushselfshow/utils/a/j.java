package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.huawei.android.pushagent.a.a.c;
import java.io.File;

public class j {
    private static j a = new j();

    private j() {
    }

    private SQLiteDatabase a(String str) {
        Object -l_2_R = new File(str);
        if (-l_2_R.exists()) {
            return SQLiteDatabase.openDatabase(str, null, 0);
        }
        Object -l_3_R = -l_2_R.getParentFile();
        if (!(-l_3_R == null || -l_3_R.exists() || !-l_3_R.mkdirs())) {
            c.e("PushLogSC2907", "datafiledir.mkdirs true");
        }
        return SQLiteDatabase.openOrCreateDatabase(str, null);
    }

    public static j a() {
        return a;
    }

    private void a(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.close();
    }

    public Cursor a(String str, String str2, String str3) {
        SQLiteDatabase -l_4_R = a(str);
        if (-l_4_R == null) {
            return null;
        }
        Object -l_5_R = -l_4_R.query(str2, null, str3, null, null, null, null);
        -l_5_R.moveToFirst();
        a(-l_4_R);
        return -l_5_R;
    }

    public Cursor a(String str, String str2, String[] strArr) {
        SQLiteDatabase -l_4_R = a(str);
        if (-l_4_R == null) {
            return null;
        }
        Object -l_5_R = -l_4_R.rawQuery(str2, strArr);
        -l_5_R.moveToFirst();
        a(-l_4_R);
        return -l_5_R;
    }

    public void a(Context context, String str, String str2) {
        SQLiteDatabase -l_4_R = a(str);
        if (-l_4_R != null) {
            -l_4_R.execSQL(str2);
            a(-l_4_R);
        }
    }

    public void a(Context context, String str, String str2, ContentValues contentValues) {
        SQLiteDatabase -l_5_R = a(str);
        if (-l_5_R != null) {
            -l_5_R.insert(str2, null, contentValues);
            a(-l_5_R);
        }
    }

    public void a(String str, String str2, String str3, String[] strArr) {
        SQLiteDatabase -l_5_R = a(str);
        if (-l_5_R != null) {
            -l_5_R.delete(str2, str3, strArr);
            a(-l_5_R);
        }
    }

    public boolean a(String str, String str2) {
        boolean z = false;
        Object -l_3_R = a(str, "sqlite_master", "(tbl_name='" + str2 + "')");
        if (-l_3_R != null) {
            int -l_4_I = -l_3_R.getCount();
            -l_3_R.close();
            if (-l_4_I > 0) {
                z = true;
            }
            return z;
        }
        c.a("PushLogSC2907", "cursor is null.");
        return false;
    }
}

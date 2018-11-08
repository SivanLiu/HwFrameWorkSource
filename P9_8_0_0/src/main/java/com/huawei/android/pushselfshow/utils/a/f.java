package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.huawei.android.pushagent.a.a.c;

public class f implements c {
    public Cursor a(Context context, Uri uri, String str, String[] strArr) throws Exception {
        return context.getContentResolver().query(uri, null, null, strArr, null);
    }

    public void a(Context context, Uri uri, String str, ContentValues contentValues) throws Exception {
        context.getContentResolver().insert(uri, contentValues);
    }

    public void a(Context context, i iVar) throws Exception {
        if (context == null) {
            c.d("PushSelfShowLog", "context is null");
        } else if (iVar != null) {
            Object -l_3_R = iVar.a();
            Object -l_4_R = iVar.c();
            Object -l_5_R = iVar.d();
            if (-l_3_R == null) {
                c.d("PushSelfShowLog", "uri is null");
            } else if (-l_4_R == null || -l_4_R.length() == 0) {
                c.d("PushSelfShowLog", "whereClause is null");
            } else if (-l_5_R == null || -l_5_R.length == 0) {
                c.d("PushSelfShowLog", "whereArgs is null");
            } else {
                Object -l_6_R = context.getContentResolver();
                if (-l_6_R != null) {
                    -l_6_R.delete(-l_3_R, -l_4_R, -l_5_R);
                } else {
                    c.d("PushSelfShowLog", "resolver is null");
                }
            }
        } else {
            c.d("PushSelfShowLog", "sqlParam is null");
        }
    }
}

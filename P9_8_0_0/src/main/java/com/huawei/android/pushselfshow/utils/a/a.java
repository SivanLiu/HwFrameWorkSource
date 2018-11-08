package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.c;
import java.util.ArrayList;

public class a {
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ArrayList a(Context context, String str) {
        Object -l_2_R = new ArrayList();
        try {
            String -l_3_R = c(context, "hwpushApp.db");
            if (TextUtils.isEmpty(-l_3_R)) {
                c.a("PushSelfShowLog", "database is null,can't queryAppinfo");
                return -l_2_R;
            }
            c.a("PushSelfShowLog", "dbName path is " + -l_3_R);
            if (j.a().a(-l_3_R, "openmarket")) {
                String[] -l_4_R = new String[]{str};
                Object -l_6_R = j.a().a(-l_3_R, "select * from openmarket where package = ?;", -l_4_R);
                if (-l_6_R != null) {
                    Object -l_7_R;
                    try {
                        if (-l_6_R.getCount() > 0) {
                            while (true) {
                                -l_7_R = -l_6_R.getString(-l_6_R.getColumnIndex("msgid"));
                                -l_2_R.add(-l_7_R);
                                c.a("TAG", "msgid and packageName is  " + -l_7_R + "," + str);
                                if (-l_6_R.moveToNext()) {
                                }
                            }
                            -l_6_R.close();
                        }
                        try {
                            -l_6_R.close();
                        } catch (Object -l_7_R2) {
                            c.e("PushSelfShowLog", "cursor.close() ", -l_7_R2);
                        }
                    } catch (Object -l_7_R22) {
                        c.d("TAG", "queryAppinfo error " + -l_7_R22.toString(), -l_7_R22);
                    } catch (Throwable th) {
                        try {
                            -l_6_R.close();
                        } catch (Object -l_9_R) {
                            c.e("PushSelfShowLog", "cursor.close() ", -l_9_R);
                        }
                    }
                } else {
                    c.a("PushSelfShowLog", "cursor is null.");
                    return -l_2_R;
                }
            }
            return -l_2_R;
        } catch (Object -l_3_R2) {
            c.e("PushSelfShowLog", "queryAppinfo error", -l_3_R2);
        }
    }

    public static void a(Context context, String str, String str2) {
        try {
            if (!context.getDatabasePath("hwpushApp.db").exists()) {
                context.openOrCreateDatabase("hwpushApp.db", 0, null).close();
            }
            String -l_4_R = c(context, "hwpushApp.db");
            if (TextUtils.isEmpty(-l_4_R)) {
                c.d("PushSelfShowLog", "database is null,can't insert appinfo into db");
                return;
            }
            c.a("PushSelfShowLog", "dbName path is " + -l_4_R);
            if (!j.a().a(-l_4_R, "openmarket")) {
                j.a().a(context, -l_4_R, "create table openmarket(    _id INTEGER PRIMARY KEY AUTOINCREMENT,     msgid  TEXT,    package TEXT);");
            }
            ContentValues -l_5_R = new ContentValues();
            -l_5_R.put("msgid", str);
            -l_5_R.put("package", str2);
            j.a().a(context, -l_4_R, "openmarket", -l_5_R);
        } catch (Object -l_3_R) {
            c.e("PushSelfShowLog", "insertAppinfo error", -l_3_R);
        }
    }

    public static void b(Context context, String str) {
        try {
            String -l_2_R = c(context, "hwpushApp.db");
            if (TextUtils.isEmpty(-l_2_R)) {
                c.d("PushSelfShowLog", "database is null,can't delete appinfo");
                return;
            }
            c.a("PushSelfShowLog", "dbName path is " + -l_2_R);
            if (j.a().a(-l_2_R, "openmarket")) {
                j.a().a(-l_2_R, "openmarket", "package = ?", new String[]{str});
            }
        } catch (Object -l_2_R2) {
            c.e("PushSelfShowLog", "Delete Appinfo error", -l_2_R2);
        }
    }

    private static String c(Context context, String str) {
        Object -l_2_R = "";
        if (context == null) {
            return -l_2_R;
        }
        Object -l_3_R = context.getDatabasePath("hwpushApp.db");
        if (-l_3_R.exists()) {
            -l_2_R = -l_3_R.getAbsolutePath();
        }
        return -l_2_R;
    }
}

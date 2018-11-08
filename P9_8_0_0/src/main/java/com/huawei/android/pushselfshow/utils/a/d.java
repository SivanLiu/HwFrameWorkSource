package com.huawei.android.pushselfshow.utils.a;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.richpush.favorites.e;
import com.huawei.android.pushselfshow.richpush.provider.RichMediaProvider.a;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import java.util.ArrayList;

public class d {
    public static ArrayList a(Context context, String str) {
        String -l_3_R;
        Object -l_2_R = new ArrayList();
        Object -l_3_R2 = "";
        String[] strArr = null;
        Cursor -l_5_R = null;
        if (str != null) {
            -l_3_R = "SELECT pushmsg._id,pushmsg.msg,pushmsg.token,pushmsg.url,notify.bmp  FROM pushmsg LEFT OUTER JOIN notify ON pushmsg.url = notify.url and pushmsg.url = ? order by pushmsg._id desc";
            strArr = new String[]{str};
        } else {
            -l_3_R = "SELECT pushmsg._id,pushmsg.msg,pushmsg.token,pushmsg.url,notify.bmp  FROM pushmsg LEFT OUTER JOIN notify ON pushmsg.url = notify.url order by pushmsg._id desc limit 1000;";
        }
        try {
            -l_5_R = e.a().a(context, a.f, -l_3_R, strArr);
        } catch (Object -l_6_R) {
            c.d("PushSelfShowLog", -l_6_R.toString(), -l_6_R);
        }
        if (-l_5_R != null) {
            while (-l_5_R.moveToNext()) {
                try {
                    int -l_6_I = -l_5_R.getInt(0);
                    Object -l_7_R = -l_5_R.getBlob(1);
                    if (-l_7_R != null) {
                        com.huawei.android.pushselfshow.c.a -l_8_R = new com.huawei.android.pushselfshow.c.a(-l_7_R, " ".getBytes("UTF-8"));
                        if (!-l_8_R.b()) {
                            c.a("PushSelfShowLog", "parseMessage failed");
                        }
                        -l_5_R.getString(3);
                        Object -l_9_R = new e();
                        -l_9_R.a(-l_6_I);
                        -l_9_R.a(-l_8_R);
                        -l_2_R.add(-l_9_R);
                    } else {
                        c.d("PushSelfShowLog", "msg is null");
                    }
                } catch (Object -l_6_R2) {
                    c.d("TAG", "query favo error " + -l_6_R2.toString(), -l_6_R2);
                } finally {
                    -l_5_R.close();
                }
            }
            c.e("PushSelfShowLog", "query favo size is " + -l_2_R.size());
            return -l_2_R;
        }
        c.a("PushSelfShowLog", "cursor is null.");
        return -l_2_R;
    }

    public static void a(Context context, int i) {
        Object -l_2_R;
        try {
            -l_2_R = new i();
            -l_2_R.a(a.g);
            -l_2_R.a("pushmsg");
            -l_2_R.b("_id = ?");
            -l_2_R.a(new String[]{String.valueOf(i)});
            e.a().a(context, -l_2_R);
        } catch (Object -l_2_R2) {
            c.d("PushSelfShowLog", -l_2_R2.toString(), -l_2_R2);
        }
    }

    public static boolean a(Context context, String str, com.huawei.android.pushselfshow.c.a aVar) {
        if (context == null || str == null || aVar == null) {
            try {
                c.e("PushSelfShowLog", "insertPushMsginfo ilegle param");
                return false;
            } catch (Object -l_3_R) {
                c.e("PushSelfShowLog", "insertBmpinfo error", -l_3_R);
                return false;
            }
        }
        ContentValues -l_3_R2 = new ContentValues();
        -l_3_R2.put(CheckVersionField.CHECK_VERSION_SERVER_URL, str);
        -l_3_R2.put("msg", aVar.c());
        -l_3_R2.put("token", " ".getBytes("UTF-8"));
        c.a("PushSelfShowLog", "insertPushMsginfo select url is %s ,rpl is %s", str, aVar.x());
        Object -l_4_R = a(context, str);
        Object -l_5_R = aVar.x();
        int -l_6_I = 0;
        while (-l_6_I < -l_4_R.size()) {
            if (((e) -l_4_R.get(-l_6_I)).b() != null && -l_5_R.equals(((e) -l_4_R.get(-l_6_I)).b().x())) {
                c.a("PushSelfShowLog", -l_5_R + " already exist");
                return true;
            }
            -l_6_I++;
        }
        c.e("PushSelfShowLog", "insertPushMsginfo " + -l_3_R2.toString());
        e.a().a(context, a.e, "pushmsg", -l_3_R2);
        return true;
    }
}

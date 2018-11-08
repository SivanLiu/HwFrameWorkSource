package com.huawei.android.pushselfshow.d;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.huawei.android.pushagent.PushReceiver.ACTION;
import com.huawei.android.pushagent.PushReceiver.KEY_TYPE;
import com.huawei.android.pushselfshow.c.a;
import com.huawei.android.pushselfshow.utils.d;
import java.security.SecureRandom;
import java.util.Date;

public class c {
    public static PendingIntent a(Context context, int i, String str) {
        Object -l_3_R = new Intent(ACTION.ACTION_NOTIFICATION_MSG_CLICK).setPackage(context.getPackageName()).setFlags(32);
        -l_3_R.putExtra(KEY_TYPE.PUSH_KEY_NOTIFY_ID, i);
        -l_3_R.putExtra(KEY_TYPE.PUSH_KEY_CLICK_BTN, str);
        int -l_4_I = (context.getPackageName() + str + new SecureRandom().nextInt() + new Date().toString()).hashCode();
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "getPendingIntent,requestCode:" + -l_4_I);
        return PendingIntent.getBroadcast(context, -l_4_I, -l_3_R, 134217728);
    }

    public static String a(Context context, a aVar) {
        if (context == null || aVar == null) {
            return "";
        }
        if (!TextUtils.isEmpty(aVar.p())) {
            return aVar.p();
        }
        return context.getResources().getString(context.getApplicationInfo().labelRes);
    }

    public static void a(Context context, int i, RemoteViews remoteViews, a aVar) {
        if (context == null || remoteViews == null || aVar == null) {
            com.huawei.android.pushagent.a.a.c.c("PushSelfShowLog", "showRightBtn error");
            return;
        }
        if (a.STYLE_2.ordinal() == aVar.D() || a.STYLE_3.ordinal() == aVar.D() || a.STYLE_4.ordinal() == aVar.D()) {
            if (!(TextUtils.isEmpty(aVar.E()[0]) || TextUtils.isEmpty(aVar.G()[0]))) {
                int -l_4_I = d.a(context, "id", "right_btn");
                remoteViews.setViewVisibility(-l_4_I, 0);
                remoteViews.setTextViewText(-l_4_I, aVar.E()[0]);
                remoteViews.setOnClickPendingIntent(-l_4_I, a(context, i, aVar.G()[0]));
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void a(Context context, Bitmap bitmap, RemoteViews remoteViews) {
        if (context != null && remoteViews != null && com.huawei.android.pushselfshow.utils.a.b()) {
            if (bitmap != null) {
                remoteViews.setImageViewBitmap(d.a(context, "id", "icon"), bitmap);
            } else {
                int -l_3_I = context.getApplicationInfo().icon;
                if (-l_3_I == 0) {
                    -l_3_I = context.getResources().getIdentifier("btn_star_big_on", "drawable", "android");
                    if (-l_3_I == 0) {
                        -l_3_I = 17301651;
                    }
                }
                remoteViews.setImageViewResource(d.a(context, "id", "icon"), -l_3_I);
            }
        }
    }
}

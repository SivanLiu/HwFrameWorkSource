package com.huawei.android.pushselfshow.d;

import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.Notification.InboxStyle;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.c.a;
import com.huawei.android.pushselfshow.utils.d;

public class e {
    public static RemoteViews a(Context context, int i, Bitmap bitmap, a aVar) {
        RemoteViews -l_4_R = new RemoteViews(context.getPackageName(), d.a(context, "layout", "hwpush_layout7"));
        c.a(context, bitmap, -l_4_R);
        -l_4_R.setTextViewText(d.a(context, "id", "title"), c.a(context, aVar));
        -l_4_R.setTextViewText(d.a(context, "id", "text"), aVar.n());
        if (aVar.F() == null || aVar.F().length <= 0 || aVar.G() == null || aVar.G().length <= 0 || aVar.F().length != aVar.G().length) {
            return -l_4_R;
        }
        Object -l_5_R = new com.huawei.android.pushselfshow.utils.c.a();
        -l_4_R.removeAllViews(d.a(context, "id", "linear_buttons"));
        int -l_6_I = 0;
        while (-l_6_I < aVar.F().length) {
            Object -l_7_R = new RemoteViews(context.getPackageName(), d.a(context, "layout", "hwpush_buttons_layout"));
            Bitmap -l_8_R = null;
            if (!TextUtils.isEmpty(aVar.F()[-l_6_I])) {
                -l_8_R = -l_5_R.a(context, aVar.F()[-l_6_I]);
            }
            if (!(-l_8_R == null || TextUtils.isEmpty(aVar.G()[-l_6_I]))) {
                int -l_9_I = d.a(context, "id", "small_btn");
                -l_7_R.setImageViewBitmap(-l_9_I, -l_8_R);
                -l_7_R.setOnClickPendingIntent(-l_9_I, c.a(context, i, aVar.G()[-l_6_I]));
                -l_4_R.addView(d.a(context, "id", "linear_buttons"), -l_7_R);
            }
            -l_6_I++;
        }
        return -l_4_R;
    }

    public static RemoteViews a(Context context, Bitmap bitmap, a aVar) {
        Object -l_3_R = new RemoteViews(context.getPackageName(), d.a(context, "layout", "hwpush_layout8"));
        Bitmap -l_4_R = null;
        if (!TextUtils.isEmpty(aVar.J())) {
            -l_4_R = new com.huawei.android.pushselfshow.utils.c.a().a(context, aVar.J());
        }
        if (-l_4_R == null) {
            return null;
        }
        -l_3_R.setViewVisibility(d.a(context, "id", "big_pic"), 0);
        -l_3_R.setImageViewBitmap(d.a(context, "id", "big_pic"), -l_4_R);
        return -l_3_R;
    }

    public static void a(Context context, Builder builder, int i, Bitmap bitmap, a aVar) {
        if (aVar == null || aVar.n() == null) {
            c.b("PushSelfShowLog", "msg is null");
        } else if (!TextUtils.isEmpty(aVar.n()) && aVar.n().contains("##")) {
            builder.setTicker(aVar.n().replace("##", "，"));
            if (com.huawei.android.pushselfshow.utils.a.c()) {
                int -l_8_I;
                builder.setLargeIcon(bitmap);
                builder.setContentTitle(c.a(context, aVar));
                Object -l_5_R = new InboxStyle();
                Object -l_6_R = aVar.n().split("##");
                int -l_7_I = -l_6_R.length;
                if (-l_7_I > 4) {
                    -l_7_I = 4;
                }
                if (!TextUtils.isEmpty(aVar.I())) {
                    -l_5_R.setBigContentTitle(aVar.I());
                    builder.setContentText(aVar.I());
                    if (4 == -l_7_I) {
                        -l_7_I--;
                    }
                }
                for (-l_8_I = 0; -l_8_I < -l_7_I; -l_8_I++) {
                    -l_5_R.addLine(-l_6_R[-l_8_I]);
                }
                if (aVar.E() != null && aVar.E().length > 0) {
                    -l_8_I = 0;
                    while (-l_8_I < aVar.E().length) {
                        if (!(TextUtils.isEmpty(aVar.E()[-l_8_I]) || TextUtils.isEmpty(aVar.G()[-l_8_I]))) {
                            builder.addAction(0, aVar.E()[-l_8_I], c.a(context, i, aVar.G()[-l_8_I]));
                        }
                        -l_8_I++;
                    }
                }
                builder.setStyle(-l_5_R);
                return;
            }
            builder.setContentText(aVar.n().replace("##", "，"));
        }
    }

    public static boolean b(Context context, Builder builder, int i, Bitmap bitmap, a aVar) {
        builder.setContentTitle(c.a(context, aVar));
        builder.setContentText(aVar.n());
        builder.setLargeIcon(bitmap);
        if (!com.huawei.android.pushselfshow.utils.a.c()) {
            return true;
        }
        Object -l_5_R = new com.huawei.android.pushselfshow.utils.c.a();
        Bitmap -l_6_R = null;
        if (!TextUtils.isEmpty(aVar.J())) {
            -l_6_R = -l_5_R.a(context, aVar.J());
        }
        if (-l_6_R == null) {
            return false;
        }
        Object -l_7_R = new BigPictureStyle();
        -l_7_R.bigPicture(-l_6_R);
        int -l_8_I = 0;
        while (-l_8_I < aVar.E().length) {
            if (!(TextUtils.isEmpty(aVar.E()[-l_8_I]) || TextUtils.isEmpty(aVar.G()[-l_8_I]))) {
                builder.addAction(0, aVar.E()[-l_8_I], c.a(context, i, aVar.G()[-l_8_I]));
            }
            -l_8_I++;
        }
        builder.setStyle(-l_7_R);
        return true;
    }
}

package com.huawei.android.pushselfshow.d;

import android.app.Notification;
import android.app.Notification.Builder;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.c.a;
import com.huawei.android.pushselfshow.utils.d;

public class f {
    public static Builder a(Context context, Builder builder, int i, a aVar, Bitmap bitmap) {
        c.a("PushSelfShowLog", "Notification addStyle");
        if (context == null || builder == null || aVar == null) {
            return builder;
        }
        Object -l_5_R = a.STYLE_1;
        if (aVar.D() >= 0 && aVar.D() < a.values().length) {
            -l_5_R = a.values()[aVar.D()];
        }
        switch (-l_5_R) {
            case STYLE_2:
                builder.setContent(a(context, i, bitmap, aVar));
                break;
            case STYLE_4:
                builder.setContent(b(context, i, bitmap, aVar));
                break;
            case STYLE_5:
                e.a(context, builder, i, bitmap, aVar);
                break;
            case STYLE_6:
                if (!e.b(context, builder, i, bitmap, aVar)) {
                    return null;
                }
                break;
            case STYLE_7:
                builder.setContent(e.a(context, i, bitmap, aVar));
                break;
            case STYLE_8:
                Object -l_6_R = e.a(context, bitmap, aVar);
                if (-l_6_R != null) {
                    builder.setContent(-l_6_R);
                    break;
                }
                return null;
        }
        return builder;
    }

    public static Notification a(Context context, Notification notification, int i, a aVar, Bitmap bitmap) {
        if (notification == null || aVar == null) {
            return notification;
        }
        RemoteViews a;
        Object -l_5_R = a.STYLE_1;
        if (aVar.D() >= 0 && aVar.D() < a.values().length) {
            -l_5_R = a.values()[aVar.D()];
        }
        switch (-l_5_R) {
            case STYLE_2:
                a = a(context, i, bitmap, aVar);
                break;
            case STYLE_4:
                a = b(context, i, bitmap, aVar);
                break;
            case STYLE_7:
                a = e.a(context, i, bitmap, aVar);
                break;
            case STYLE_8:
                Object -l_6_R = e.a(context, bitmap, aVar);
                if (-l_6_R != null) {
                    notification.contentView = -l_6_R;
                    break;
                }
                return null;
        }
        notification.contentView = a;
        return notification;
    }

    private static RemoteViews a(Context context, int i, Bitmap bitmap, a aVar) {
        RemoteViews -l_4_R = new RemoteViews(context.getPackageName(), d.c(context, "hwpush_layout2"));
        c.a(context, bitmap, -l_4_R);
        c.a(context, i, -l_4_R, aVar);
        -l_4_R.setTextViewText(d.e(context, "title"), c.a(context, aVar));
        -l_4_R.setTextViewText(d.e(context, "text"), aVar.n());
        return -l_4_R;
    }

    private static RemoteViews b(Context context, int i, Bitmap bitmap, a aVar) {
        RemoteViews -l_4_R = new RemoteViews(context.getPackageName(), d.c(context, "hwpush_layout4"));
        c.a(context, bitmap, -l_4_R);
        c.a(context, i, -l_4_R, aVar);
        -l_4_R.setTextViewText(d.e(context, "title"), c.a(context, aVar));
        if (aVar.H() == null || aVar.H().length <= 0) {
            return -l_4_R;
        }
        Object -l_5_R = new com.huawei.android.pushselfshow.utils.c.a();
        -l_4_R.removeAllViews(d.e(context, "linear_icons"));
        Object -l_6_R = null;
        for (int -l_7_I = 0; -l_7_I < aVar.H().length; -l_7_I++) {
            Object -l_8_R = new RemoteViews(context.getPackageName(), d.a(context, "layout", "hwpush_icons_layout"));
            if (!TextUtils.isEmpty(aVar.H()[-l_7_I])) {
                -l_6_R = -l_5_R.a(context, aVar.H()[-l_7_I]);
            }
            if (-l_6_R != null) {
                c.a("PushSelfShowLog", "rescale bitmap success");
                -l_8_R.setImageViewBitmap(d.a(context, "id", "smallicon"), -l_6_R);
                -l_4_R.addView(d.a(context, "id", "linear_icons"), -l_8_R);
            }
        }
        return -l_4_R;
    }
}

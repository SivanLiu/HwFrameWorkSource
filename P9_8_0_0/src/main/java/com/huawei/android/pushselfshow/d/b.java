package com.huawei.android.pushselfshow.d;

import android.R.drawable;
import android.annotation.SuppressLint;
import android.app.Notification.Builder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.a;
import com.huawei.android.pushselfshow.utils.d;

public class b {
    @SuppressLint({"InlinedApi"})
    private static float a(Context context) {
        float -l_1_F = (float) a.a(context, 48.0f);
        try {
            float -l_2_F = context.getResources().getDimension(17104901);
            if (-l_2_F > 0.0f && -l_1_F > -l_2_F) {
                -l_1_F = -l_2_F;
            }
        } catch (Object -l_2_R) {
            c.c("PushSelfShowLog", -l_2_R.toString());
        }
        c.a("PushSelfShowLog", "getRescaleBitmapSize:" + -l_1_F);
        return -l_1_F;
    }

    public static int a(Context context, com.huawei.android.pushselfshow.c.a aVar) {
        int -l_2_I = 0;
        if (context == null || aVar == null) {
            c.b("PushSelfShowLog", "enter getSmallIconId, context or msg is null");
            return 0;
        }
        if ("com.huawei.android.pushagent".equals(aVar.k())) {
            -l_2_I = d.g(context, "hwpush_status_icon");
        }
        if (-l_2_I == 0) {
            -l_2_I = context.getApplicationInfo().icon;
        }
        if (-l_2_I == 0) {
            -l_2_I = context.getResources().getIdentifier("btn_star_big_on", "drawable", "android");
            c.a("PushSelfShowLog", "icon is btn_star_big_on ");
            if (-l_2_I == 0) {
                -l_2_I = 17301651;
                c.a("PushSelfShowLog", "icon is sym_def_app_icon ");
            }
        }
        return -l_2_I;
    }

    public static int a(Context context, String str, String str2, Object obj) {
        Object -l_5_R;
        try {
            -l_5_R = context.getPackageName() + ".R";
            c.a("PushSelfShowLog", "try to refrect " + -l_5_R + " typeName is " + str2);
            Object -l_7_R = Class.forName(-l_5_R).getClasses();
            c.a("PushSelfShowLog", "sonClassArr length " + -l_7_R.length);
            Object -l_8_R = null;
            for (Object -l_10_R : -l_7_R) {
                c.a("PushSelfShowLog", "sonTypeClass,query sonclass is  %s", -l_10_R.getName().substring(-l_5_R.length() + 1) + " sonClass.getName() is" + -l_10_R.getName());
                if (str2.equals(-l_10_R.getName().substring(-l_5_R.length() + 1))) {
                    -l_8_R = -l_10_R;
                    break;
                }
            }
            if (-l_8_R == null) {
                c.a("PushSelfShowLog", "sonTypeClass is null");
                Object -l_9_R = context.getPackageName() + ".R$" + str2;
                c.a("PushSelfShowLog", "try to refrect 2 " + -l_9_R + " typeName is " + str2);
                c.a("PushSelfShowLog", " refect res id 2 is %s", "" + Class.forName(-l_9_R).getField(str).getInt(obj));
                return Class.forName(-l_9_R).getField(str).getInt(obj);
            }
            c.a("PushSelfShowLog", " refect res id is %s", "" + -l_8_R.getField(str).getInt(obj));
            return -l_8_R.getField(str).getInt(obj);
        } catch (Object -l_5_R2) {
            c.d("PushSelfShowLog", "ClassNotFound failed,", -l_5_R2);
            return 0;
        } catch (Object -l_5_R22) {
            c.d("PushSelfShowLog", "NoSuchFieldException failed,", -l_5_R22);
            return 0;
        } catch (Object -l_5_R222) {
            c.d("PushSelfShowLog", "IllegalAccessException failed,", -l_5_R222);
            return 0;
        } catch (Object -l_5_R2222) {
            c.d("PushSelfShowLog", "IllegalArgumentException failed,", -l_5_R2222);
            return 0;
        } catch (Object -l_5_R22222) {
            c.d("PushSelfShowLog", "IndexOutOfBoundsException failed,", -l_5_R22222);
            return 0;
        } catch (Object -l_5_R222222) {
            c.d("PushSelfShowLog", "  failed,", -l_5_R222222);
            return 0;
        }
    }

    public static void a(Context context, Builder builder, com.huawei.android.pushselfshow.c.a aVar) {
        if (context == null || builder == null || aVar == null) {
            c.d("PushSelfShowLog", "msg is null");
            return;
        }
        if (d(context, aVar)) {
            c.b("PushSelfShowLog", "get small icon from " + aVar.k());
            Object -l_3_R = c(context, aVar);
            if (-l_3_R == null) {
                builder.setSmallIcon(a(context, aVar));
            } else {
                builder.setSmallIcon(-l_3_R);
            }
        } else {
            builder.setSmallIcon(a(context, aVar));
        }
    }

    public static Bitmap b(Context context, com.huawei.android.pushselfshow.c.a aVar) {
        if (context == null || aVar == null) {
            return null;
        }
        Bitmap bitmap = null;
        Object -l_3_R;
        try {
            if (aVar.o() != null && aVar.o().length() > 0) {
                -l_3_R = new com.huawei.android.pushselfshow.utils.c.a();
                int -l_4_I = 0;
                if (!aVar.o().equals("" + aVar.a())) {
                    -l_4_I = a(context, aVar.o(), "drawable", new drawable());
                    if (-l_4_I == 0) {
                        -l_4_I = context.getResources().getIdentifier(aVar.o(), "drawable", "android");
                    }
                    c.a("PushSelfShowLog", "msg.notifyIcon is " + aVar.o() + ",and defaultIcon is " + -l_4_I);
                }
                if (-l_4_I == 0) {
                    Bitmap -l_5_R = -l_3_R.a(context, aVar.o());
                    c.a("PushSelfShowLog", "get bitmap from new downloaded ");
                    if (-l_5_R != null) {
                        c.a("PushSelfShowLog", "height:" + -l_5_R.getHeight() + ",width:" + -l_5_R.getWidth());
                        float -l_6_F = a(context);
                        -l_5_R = -l_3_R.a(context, -l_5_R, -l_6_F, -l_6_F);
                    }
                    if (-l_5_R != null) {
                        bitmap = -l_5_R;
                    }
                } else {
                    bitmap = BitmapFactory.decodeResource(context.getResources(), -l_4_I);
                }
            }
            if (com.huawei.android.pushagent.a.a.a.a() < 11) {
                if (bitmap == null) {
                    if (!"com.huawei.android.pushagent".equals(aVar.k())) {
                        c.b("PushSelfShowLog", "get left bitmap from " + aVar.k());
                        bitmap = ((BitmapDrawable) context.getPackageManager().getApplicationIcon(aVar.k())).getBitmap();
                    }
                }
                return bitmap;
            }
            c.b("PushSelfShowLog", "huawei phone, and emui5.0, need not show large icon.");
            return bitmap;
        } catch (Object -l_3_R2) {
            c.d("PushSelfShowLog", "" + -l_3_R2.toString(), -l_3_R2);
        } catch (Object -l_3_R22) {
            c.d("PushSelfShowLog", "" + -l_3_R22.toString(), -l_3_R22);
        }
    }

    public static Icon c(Context context, com.huawei.android.pushselfshow.c.a aVar) {
        if (context == null || aVar == null) {
            c.d("PushSelfShowLog", "getSmallIcon, context is null");
            return null;
        } else if (VERSION.SDK_INT >= 23) {
            try {
                return Icon.createWithResource(aVar.k(), context.getPackageManager().getApplicationInfo(aVar.k(), 0).icon);
            } catch (Object -l_3_R) {
                c.d("PushSelfShowLog", -l_3_R.toString());
                return null;
            } catch (Object -l_3_R2) {
                c.d("PushSelfShowLog", -l_3_R2.toString(), -l_3_R2);
                return null;
            }
        } else {
            c.b("PushSelfShowLog", "getSmallIcon failed, Build.VERSION less than 23");
            return null;
        }
    }

    private static boolean d(Context context, com.huawei.android.pushselfshow.c.a aVar) {
        if (!"com.huawei.android.pushagent".equals(aVar.k()) && VERSION.SDK_INT >= 23) {
            if (a.e(context) || a.f(context)) {
                return true;
            }
        }
        return false;
    }
}

package com.huawei.android.pushselfshow.utils;

import android.content.Context;
import com.huawei.android.pushagent.a.a.c;

public class d {
    public static int a(Context context, String str) {
        return a(context, "string", str);
    }

    public static int a(Context context, String str, String str2) {
        try {
            int -l_3_I = context.getResources().getIdentifier(str2, str, context.getPackageName());
            if (-l_3_I == 0) {
                Object -l_5_R = Class.forName(context.getPackageName() + ".R$" + str).getField(str2);
                -l_3_I = Integer.parseInt(-l_5_R.get(-l_5_R.getName()).toString());
                if (-l_3_I == 0) {
                    c.b("ResourceLoader", "Error-resourceType=" + str + "--resourceName=" + str2 + "--resourceId =" + -l_3_I);
                }
            }
            return -l_3_I;
        } catch (Object -l_3_R) {
            c.d("ResourceLoader", "!!!! ResourceLoader: ClassNotFoundException-resourceType=" + str + "--resourceName=" + str2, -l_3_R);
            return 0;
        } catch (Object -l_3_R2) {
            c.d("ResourceLoader", "!!!! ResourceLoader: NoSuchFieldException-resourceType=" + str + "--resourceName=" + str2, -l_3_R2);
            return 0;
        } catch (Object -l_3_R22) {
            c.d("ResourceLoader", "!!!! ResourceLoader: NumberFormatException-resourceType=" + str + "--resourceName=" + str2, -l_3_R22);
            return 0;
        } catch (Object -l_3_R222) {
            c.d("ResourceLoader", "!!!! ResourceLoader: IllegalAccessException-resourceType=" + str + "--resourceName=" + str2, -l_3_R222);
            return 0;
        } catch (Object -l_3_R2222) {
            c.d("ResourceLoader", "!!!! ResourceLoader: IllegalArgumentException-resourceType=" + str + "--resourceName=" + str2, -l_3_R2222);
            return 0;
        }
    }

    public static int b(Context context, String str) {
        return a(context, "plurals", str);
    }

    public static int c(Context context, String str) {
        return a(context, "layout", str);
    }

    public static int d(Context context, String str) {
        return a(context, "menu", str);
    }

    public static int e(Context context, String str) {
        return a(context, "id", str);
    }

    public static int f(Context context, String str) {
        return a(context, "color", str);
    }

    public static int g(Context context, String str) {
        return a(context, "drawable", str);
    }
}

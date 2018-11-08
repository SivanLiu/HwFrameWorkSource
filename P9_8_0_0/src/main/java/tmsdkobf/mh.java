package tmsdkobf;

import java.lang.reflect.Field;

public final class mh {
    public static Class<?> zK;

    public static Object a(Object obj, String str) throws Exception {
        return obj.getClass().getField(str).get(obj);
    }

    public static Object a(Object obj, String str, Object[] objArr) throws Exception {
        Object -l_3_R = obj.getClass();
        Object -l_4_R = new Class[objArr.length];
        int -l_6_I = objArr.length;
        for (int -l_5_I = 0; -l_5_I < -l_6_I; -l_5_I++) {
            -l_4_R[-l_5_I] = objArr[-l_5_I].getClass();
            Class cls;
            if (-l_4_R[-l_5_I] == Integer.class) {
                cls = Integer.TYPE;
                -l_4_R[-l_5_I] = cls;
            } else if (-l_4_R[-l_5_I] == Boolean.class) {
                cls = Boolean.TYPE;
                -l_4_R[-l_5_I] = cls;
            }
        }
        return -l_3_R.getMethod(str, -l_4_R).invoke(obj, objArr);
    }

    public static Object a(String str, Object[] objArr) throws Exception {
        Object -l_3_R;
        Object -l_2_R = Class.forName(str);
        if (objArr != null) {
            -l_3_R = new Class[objArr.length];
            int -l_5_I = objArr.length;
            for (int -l_4_I = 0; -l_4_I < -l_5_I; -l_4_I++) {
                -l_3_R[-l_4_I] = objArr[-l_4_I].getClass();
            }
        } else {
            -l_3_R = null;
        }
        return -l_2_R.getConstructor(-l_3_R).newInstance(objArr);
    }

    public static final boolean bY(String str) {
        Object -l_1_R = null;
        try {
            -l_1_R = Class.forName(str);
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
        }
        zK = -l_1_R;
        return zK != null;
    }

    public static final int e(String str, int -l_2_I) {
        Object -l_3_R = getField(str);
        if (-l_3_R != null) {
            try {
                -l_2_I = -l_3_R.getInt(null);
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
            } catch (Object -l_4_R2) {
                -l_4_R2.printStackTrace();
            }
        }
        return -l_2_I;
    }

    private static final Field getField(String str) {
        Object -l_1_R = null;
        if (zK != null) {
            try {
                -l_1_R = zK.getDeclaredField(str);
                -l_1_R.setAccessible(true);
            } catch (Object -l_2_R) {
                -l_2_R.printStackTrace();
            } catch (Object -l_2_R2) {
                -l_2_R2.printStackTrace();
            }
        }
        return -l_1_R;
    }
}

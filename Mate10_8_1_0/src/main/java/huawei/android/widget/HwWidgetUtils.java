package huawei.android.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import java.lang.reflect.Field;

public class HwWidgetUtils {
    private static final int ACTIONBAR_BACKGROUND_THEMED_FLAG = 0;
    private static final String TAG = "HwWidgetUtils";

    public static final boolean isActionbarBackgroundThemed(Context context) {
        boolean z = false;
        if (context == null) {
            return false;
        }
        if (context.getResources().getColor(33882153) == 0) {
            z = true;
        }
        return z;
    }

    public static boolean isHwTheme(Context context) {
        boolean z = false;
        TypedValue tv = getThemeOfEmui(context);
        if (tv == null) {
            return false;
        }
        if (tv.type == 16) {
            z = true;
        }
        return z;
    }

    public static boolean isHwLightTheme(Context context) {
        boolean z = true;
        TypedValue tv = getThemeOfEmui(context);
        if (tv == null) {
            return false;
        }
        if (!(tv.type == 16 && tv.data > 0 && tv.data % 3 == 1)) {
            z = false;
        }
        return z;
    }

    public static boolean isHwDarkTheme(Context context) {
        boolean z = false;
        TypedValue tv = getThemeOfEmui(context);
        if (tv == null) {
            return false;
        }
        if (tv.type == 16 && tv.data > 0 && tv.data % 3 == 2) {
            z = true;
        }
        return z;
    }

    public static boolean isHwEmphasizeTheme(Context context) {
        boolean z = false;
        TypedValue tv = getThemeOfEmui(context);
        if (tv == null) {
            return false;
        }
        if (tv.type == 16 && tv.data > 0 && tv.data % 3 == 0) {
            z = true;
        }
        return z;
    }

    private static TypedValue getThemeOfEmui(Context context) {
        if (context == null) {
            return null;
        }
        int themeOfEmuiId = context.getResources().getIdentifier("themeOfEmui", "attr", "androidhwext");
        if (themeOfEmuiId == 0) {
            return null;
        }
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(themeOfEmuiId, tv, true);
        return tv;
    }

    public static final int[] getResourceDeclareStyleableIntArray(String pkgName, String name) {
        if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(name)) {
            return null;
        }
        try {
            return (int[]) Class.forName(pkgName + ".R$styleable").getField(name).get(null);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "getResourceDeclareStyleableIntArray, exception");
            return null;
        }
    }

    public static final int getResourceDeclareStyleableInt(String pkgName, String declareStyleable, String indexName) {
        if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(declareStyleable) || TextUtils.isEmpty(indexName)) {
            return 0;
        }
        try {
            Class<?> cls = Class.forName(pkgName + ".R$styleable");
            Field fieldStyle = cls.getField(declareStyleable);
            int[] idArray = (int[]) fieldStyle.get(null);
            int index = ((Integer) cls.getField(indexName).get(null)).intValue();
            if (idArray == null || index < 0 || index >= idArray.length) {
                return 0;
            }
            return idArray[index];
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "getResourceDeclareStyleableInt, exception");
            return 0;
        }
    }

    public static final int getResourceDeclareStyleableIndex(String pkgName, String indexName) {
        if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(indexName)) {
            return 0;
        }
        try {
            return ((Integer) Class.forName(pkgName + ".R$styleable").getField(indexName).get(null)).intValue();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "getResourceDeclareStyleableIndex, exception");
            return 0;
        }
    }
}

package huawei.android.widget.loader;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.util.Log;
import java.lang.reflect.Field;

public class ResLoader {
    private static final String HW_SYSTEM_RES_PACKAGE_NAME = "androidhwext";
    private static final String HW_THEME_META_NAME = "hwc-theme";
    private static final String HW_WIDGET_THEME = "HwWidgetTheme";
    private static final String PLUGIN_RES_PACKAGE_NAME = "com.huawei.hwwidgetplugin";
    private static final String RES_TYPE_STYLEABLE = "styleable";
    private static final String TAG = "ResLoader";
    private static ResLoader instance;
    private String mPluginPath;
    private boolean mUseHwframeworkRes = true;

    private ResLoader() {
    }

    public static synchronized ResLoader getInstance() {
        ResLoader resLoader;
        synchronized (ResLoader.class) {
            if (instance == null) {
                instance = new ResLoader();
            }
            resLoader = instance;
        }
        return resLoader;
    }

    public Context getContext(Context base) {
        return (isInHwSystem() || getTheme(base) == null) ? base : new PluginContextWrapper(base);
    }

    public int getIdentifier(Context context, String type, String name) {
        if (context != null) {
            return getIdentifier(context, type, name, getPackageName(context));
        }
        Log.w(TAG, "getIdentifier,context is null");
        return 0;
    }

    private int getIdentifier(Context context, String type, String name, String packageName) {
        if ("styleable".equals(type)) {
            return reflectIdForInt(context, packageName, "styleable", name);
        }
        return getResources(context).getIdentifier(name, type, packageName);
    }

    public String getPackageName(Context context) {
        if (isInHwSystem()) {
            return HW_SYSTEM_RES_PACKAGE_NAME;
        }
        return context != null ? context.getPackageName() : "";
    }

    public int[] getIdentifierArray(Context context, String type, String name) {
        if (context == null) {
            Log.w(TAG, "getIdentifierArray, context is null");
            return null;
        }
        Object ids = reflectId(context, getPackageName(context), type, name);
        if (ids instanceof int[]) {
            return (int[]) ids;
        }
        Log.e(TAG, "getIdentifierArray: can not get the resource id array, please check the resource type and name");
        return null;
    }

    private int reflectIdForInt(Context context, String packageName, String type, String name) {
        Object id = reflectId(context, packageName, type, name);
        if (id instanceof Integer) {
            return ((Integer) id).intValue();
        }
        Log.e(TAG, "getIdentifier: can not get the resource id, please check the resource type and name.");
        return -1;
    }

    public Theme getTheme(Context context) {
        if (context == null) {
            Log.w(TAG, "getTheme, context is null");
            return null;
        }
        Theme theme = context.getTheme();
        if (isInHwSystem()) {
            return theme;
        }
        String themeFromMetaData = null;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            try {
                ActivityInfo info = activity.getPackageManager().getActivityInfo(activity.getComponentName(), 128);
                if (info == null || info.metaData == null) {
                    ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
                    if (!(appInfo == null || appInfo.metaData == null)) {
                        themeFromMetaData = appInfo.metaData.getString(HW_THEME_META_NAME);
                    }
                } else {
                    themeFromMetaData = info.metaData.getString(HW_THEME_META_NAME);
                }
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (themeFromMetaData == null) {
            return theme;
        }
        int themeId = -1;
        Object obj = -1;
        if (themeFromMetaData.hashCode() == -734899914 && themeFromMetaData.equals(HW_WIDGET_THEME)) {
            obj = null;
        }
        if (obj != null) {
            Log.e(TAG, "getTheme: the theme from meta is wrong");
        } else {
            themeId = reflectIdForInt(context, PLUGIN_RES_PACKAGE_NAME, "style", "Theme_Emui");
        }
        if (themeId > 0) {
            theme = getResources(context).newTheme();
            theme.applyStyle(themeId, true);
        }
        return theme;
    }

    public Resources getResources(Context context) {
        return context != null ? context.getResources() : null;
    }

    private Object reflectId(Context context, String packageName, String type, String name) {
        StringBuilder stringBuilder;
        ClassLoader classLoader = context.getClassLoader();
        String clazzName;
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(packageName);
            stringBuilder2.append(".R$");
            stringBuilder2.append(type);
            Class<?> clazz = classLoader.loadClass(stringBuilder2.toString());
            clazzName = new StringBuilder();
            clazzName.append(packageName);
            clazzName.append(".R$");
            clazzName.append(type);
            clazzName = clazzName.toString();
            if (clazz != null) {
                Field field = clazz.getField(name);
                field.setAccessible(true);
                return field.get(clazz);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            clazzName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getIdentifier: com.huawei.hwwidgetplugin.R.");
            stringBuilder.append(type);
            stringBuilder.append(" not found");
            Log.e(clazzName, stringBuilder.toString());
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            clazzName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getIdentifier: ");
            stringBuilder.append(packageName);
            stringBuilder.append(".R.");
            stringBuilder.append(type);
            stringBuilder.append(".");
            stringBuilder.append(name);
            stringBuilder.append(" not found");
            Log.e(clazzName, stringBuilder.toString());
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            clazzName = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getIdentifier: IllegalAccessException of ");
            stringBuilder.append(packageName);
            stringBuilder.append(".R.");
            stringBuilder.append(type);
            stringBuilder.append(".");
            stringBuilder.append(name);
            Log.e(clazzName, stringBuilder.toString());
        }
        return Integer.valueOf(0);
    }

    public int getInternalId(String type, String name) {
        String str;
        StringBuilder stringBuilder;
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("com.android.internal.R$");
            stringBuilder2.append(type);
            Field field = Class.forName(stringBuilder2.toString()).getField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getInternalId: com.android.internal.R.");
            stringBuilder.append(type);
            stringBuilder.append(" not found");
            Log.e(str, stringBuilder.toString());
            return 0;
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getInternalId: com.android.internal.R.");
            stringBuilder.append(type);
            stringBuilder.append(".");
            stringBuilder.append(name);
            stringBuilder.append(" not found");
            Log.e(str, stringBuilder.toString());
            return 0;
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getInternalId: IllegalAccessException of com.android.internal.R.");
            stringBuilder.append(type);
            stringBuilder.append(".");
            stringBuilder.append(name);
            Log.e(str, stringBuilder.toString());
            return 0;
        }
    }

    private boolean isInHwSystem() {
        return this.mUseHwframeworkRes;
    }

    public void setInHwSystem(boolean enable) {
        this.mUseHwframeworkRes = enable;
    }
}

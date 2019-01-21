package com.huawei.featurelayer;

import android.content.Context;
import android.util.Log;
import com.huawei.featurelayer.featureframework.IFeatureFramework;
import dalvik.system.PathClassLoader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HwFeatureLoader {
    private static final String BUILDIN_SYSTEM_FEATURE_CONFIG = "/system/etc/featurelist.json";
    private static final String FEATURE_CONFIG_FEATURES = "features";
    private static final String FEATURE_CONFIG_FILE = "file";
    private static final String FEATURE_CONFIG_PACKAGE = "package";
    private static final String FEATURE_CONFIG_VERSION = "version";
    private static final String FEATURE_FRAMEWORK_ENTRY = "FeatureEntry";
    private static final String FEATURE_FRAMEWORK_ENTRY_METHOD = "connect";
    private static final String FEATURE_FRAMEWORK_PKG = "com.huawei.featurelayer.featureframework";
    private static final String INSTALLED_SYSTEM_FEATURE_CONFIG = "/data/systemfeature/systemfeature/systemfeature/featurelist.json";
    private static final String TAG = "FLTAG.HwFeatureLoader";
    private static Method sCreateEntry = null;

    static class FeatureInfoItem {
        String path;
        int versionX = -1;
        int versionY = -1;
        int versionZ = -1;

        FeatureInfoItem(String path, String version) {
            this.path = path;
            String[] bits = version.split("\\.");
            if (bits.length == 3) {
                try {
                    this.versionX = Integer.valueOf(bits[0]).intValue();
                    this.versionY = Integer.valueOf(bits[1]).intValue();
                    this.versionZ = Integer.valueOf(bits[2]).intValue();
                } catch (NumberFormatException e) {
                    String str = HwFeatureLoader.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("FeatureInfoItem() format error: ");
                    stringBuilder.append(version);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }

        int compareVersion(FeatureInfoItem item) {
            if (item == null) {
                return 1;
            }
            if (this.versionX != item.versionX) {
                return this.versionX - item.versionX;
            }
            if (this.versionY != item.versionY) {
                return this.versionY - item.versionY;
            }
            return this.versionZ - item.versionZ;
        }
    }

    public static class SystemFeature {
        private static IFeatureFramework sFeatureFramework = null;

        public static void loadFeatureFramework(Context context) {
            sFeatureFramework = HwFeatureLoader.loadFeatureFramework(context);
        }

        public static IFeatureFramework getFeatureFramework() {
            return sFeatureFramework;
        }

        public static void preloadClasses() {
        }
    }

    public static class SystemServiceFeature {
        private static IFeatureFramework sFeatureFramework = null;

        public static void loadFeatureFramework(Context context) {
            sFeatureFramework = HwFeatureLoader.loadFeatureFramework(context);
        }

        public static IFeatureFramework getFeatureFramework() {
            return sFeatureFramework;
        }
    }

    private static FeatureInfoItem getFeatureFrameworkInfo(String json) {
        StringBuilder stringBuilder;
        String content = "";
        int j = 0;
        try {
            content = new String(Files.readAllBytes(Paths.get(json, new String[0])), "utf-8");
        } catch (IOException e) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFeatureFrameworkInfo() parse error: ");
            stringBuilder.append(json);
            Log.i(str, stringBuilder.toString());
        }
        if (content.isEmpty()) {
            return null;
        }
        try {
            JSONArray array = new JSONObject(content).getJSONArray(FEATURE_CONFIG_FEATURES);
            int length = array.length();
            while (j < length) {
                JSONObject feature = array.getJSONObject(j);
                if ("com.huawei.featurelayer.featureframework".equals(feature.getString(FEATURE_CONFIG_PACKAGE))) {
                    return new FeatureInfoItem(feature.getString(FEATURE_CONFIG_FILE), feature.getString(FEATURE_CONFIG_VERSION));
                }
                j++;
            }
        } catch (JSONException e2) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFeatureFrameworkInfo() file: ");
            stringBuilder.append(json);
            stringBuilder.append(" , error: ");
            stringBuilder.append(e2);
            Log.e(str2, stringBuilder.toString());
        }
        return null;
    }

    private static String getNewerAPKPath(FeatureInfoItem buildin, FeatureInfoItem installed) {
        if (buildin != null) {
            return buildin.compareVersion(installed) >= 0 ? buildin.path : installed.path;
        } else if (installed != null) {
            return installed.path;
        } else {
            Log.w(TAG, "getNewerAPKPath() NO feature framework buildin or installed!");
            return null;
        }
    }

    private static boolean loadFeatureFramework(String apkPath) {
        try {
            sCreateEntry = new PathClassLoader(apkPath, "", null).loadClass("com.huawei.featurelayer.featureframework.FeatureEntry").getDeclaredMethod(FEATURE_FRAMEWORK_ENTRY_METHOD, new Class[]{Context.class});
            return true;
        } catch (Exception e) {
            Log.e(TAG, "loadFeatureFramework() ", e);
            return false;
        }
    }

    private static void loadFeatureFramework() {
        FeatureInfoItem buildin = getFeatureFrameworkInfo(BUILDIN_SYSTEM_FEATURE_CONFIG);
        FeatureInfoItem installed = getFeatureFrameworkInfo(INSTALLED_SYSTEM_FEATURE_CONFIG);
        String newerPath = getNewerAPKPath(buildin, installed);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadFeatureFramework() ");
        stringBuilder.append(newerPath);
        Log.i(str, stringBuilder.toString());
        if (newerPath != null && !loadFeatureFramework(newerPath) && installed != null && newerPath.equals(installed.path) && buildin != null && buildin.path != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("loadFeatureFramework() reload: ");
            stringBuilder.append(buildin.path);
            Log.i(str, stringBuilder.toString());
            loadFeatureFramework(buildin.path);
        }
    }

    public static IFeatureFramework loadFeatureFramework(Context context) {
        try {
            if (sCreateEntry == null) {
                loadFeatureFramework();
            }
            if (sCreateEntry != null) {
                return (IFeatureFramework) sCreateEntry.invoke(null, new Object[]{context});
            }
            Log.e(TAG, "loadFeatureFramework() sCreateEntry is null");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "loadFeatureFramework() ", e);
            return null;
        }
    }
}

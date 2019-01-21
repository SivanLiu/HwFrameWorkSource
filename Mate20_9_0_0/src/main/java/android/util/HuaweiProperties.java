package android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class HuaweiProperties {
    private static String TAG = "HuaweiProperties";
    public static String VENDOR_HUAWEI_CONFIG_PATH = "/vendor/etc/framework_res_configs.xml";
    private static ArrayMap<String, String> mVendorPropertiesArrayMap = new ArrayMap();

    static {
        loadAllProperties(VENDOR_HUAWEI_CONFIG_PATH);
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0080  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void loadAllProperties(String filePath) {
        Properties prop;
        IOException e;
        String str;
        StringBuilder stringBuilder;
        Set<String> propertiesString = null;
        if (new File(filePath).exists()) {
            prop = new Properties();
            FileInputStream str2 = null;
            try {
                str2 = new FileInputStream(filePath);
                prop.loadFromXML(str2);
                try {
                    str2.close();
                } catch (IOException e2) {
                    e = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            } catch (IOException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("close file execption : ");
                stringBuilder.append(e3);
                Slog.w(str, stringBuilder.toString());
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e4) {
                        e3 = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e5) {
                        String str3 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("get Hw property execption : ");
                        stringBuilder2.append(e5);
                        Slog.w(str3, stringBuilder2.toString());
                    }
                }
            }
            try {
                propertiesString = prop.stringPropertyNames();
            } catch (NumberFormatException e6) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("get Hw property execption : ");
                stringBuilder.append(e6);
                Slog.w(str, stringBuilder.toString());
            }
            if (propertiesString != null) {
                for (String str4 : propertiesString) {
                    mVendorPropertiesArrayMap.put(str4, prop.getProperty(str4));
                }
            }
        }
        return;
        stringBuilder.append("get Hw property execption : ");
        stringBuilder.append(e3);
        Slog.w(str4, stringBuilder.toString());
        propertiesString = prop.stringPropertyNames();
        if (propertiesString != null) {
        }
    }

    public static String getProperties(String filePath, String searchProperty) {
        if (new File(filePath).exists()) {
            return (String) mVendorPropertiesArrayMap.get(searchProperty);
        }
        return null;
    }
}

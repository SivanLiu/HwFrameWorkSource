package android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class HuaweiProperties {
    private static String TAG = "HuaweiProperties";
    public static String VENDOR_HUAWEI_CONFIG_PATH = "/vendor/etc/framework_res_configs.xml";
    private static ArrayMap<String, String> mVendorPropertiesArrayMap = new ArrayMap();

    static {
        loadAllProperties(VENDOR_HUAWEI_CONFIG_PATH);
    }

    private static void loadAllProperties(String filePath) {
        IOException e;
        Throwable th;
        Iterable propertiesString = null;
        if (new File(filePath).exists()) {
            Properties prop = new Properties();
            FileInputStream fileInputStream = null;
            try {
                FileInputStream str = new FileInputStream(filePath);
                try {
                    prop.loadFromXML(str);
                    if (str != null) {
                        try {
                            str.close();
                        } catch (IOException e2) {
                            Slog.w(TAG, "get Hw property execption : " + e2);
                        }
                    }
                    fileInputStream = str;
                } catch (IOException e3) {
                    e2 = e3;
                    fileInputStream = str;
                    try {
                        Slog.w(TAG, "close file execption : " + e2);
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e22) {
                                Slog.w(TAG, "get Hw property execption : " + e22);
                            }
                        }
                        propertiesString = prop.stringPropertyNames();
                        if (r6 != null) {
                            for (String key : r6) {
                                mVendorPropertiesArrayMap.put(key, prop.getProperty(key));
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e222) {
                                Slog.w(TAG, "get Hw property execption : " + e222);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileInputStream = str;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (IOException e4) {
                e222 = e4;
                Slog.w(TAG, "close file execption : " + e222);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                propertiesString = prop.stringPropertyNames();
                if (r6 != null) {
                    for (String key2 : r6) {
                        mVendorPropertiesArrayMap.put(key2, prop.getProperty(key2));
                    }
                }
            }
            try {
                propertiesString = prop.stringPropertyNames();
            } catch (NumberFormatException e5) {
                Slog.w(TAG, "get Hw property execption : " + e5);
            }
            if (r6 != null) {
                for (String key22 : r6) {
                    mVendorPropertiesArrayMap.put(key22, prop.getProperty(key22));
                }
            }
        }
    }

    public static String getProperties(String filePath, String searchProperty) {
        if (new File(filePath).exists()) {
            return (String) mVendorPropertiesArrayMap.get(searchProperty);
        }
        return null;
    }
}

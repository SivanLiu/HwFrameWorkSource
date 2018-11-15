package com.android.server.display;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwPgSceneDetectionAppName {
    private static final String APP_NAME = "AppName";
    private static final boolean DEBUG;
    private static final String NO_USE_POWERCURE_APP = "NoUsePowerCureApp";
    private static final String SceneDetectionNoUsePowerCure = "SceneDetectionNoUsePowerCure";
    private static final String TAG = "HwPgSceneDetectionAppName";
    private static final String XML_NAME = "HwPgSceneDetectionAppName.xml";
    private static HwPgSceneDetectionAppName mInstance;
    private static HashSet<String> mNoUsePowerCureUniqueAppName = getHwPgSceneDetectionAppName();

    static {
        boolean isLoggable = !Log.HWINFO ? Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false : true;
        DEBUG = isLoggable;
    }

    public static boolean isNoUsePowerCureAppEnable(String appName) {
        if (appName == null || mNoUsePowerCureUniqueAppName == null || mNoUsePowerCureUniqueAppName.isEmpty()) {
            return false;
        }
        return mNoUsePowerCureUniqueAppName.contains(appName);
    }

    private static File getXmlFile() {
        try {
            String xmlPath = String.format("/xml/lcd/%s", new Object[]{XML_NAME});
            File xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, 0);
            if (xmlFile != null) {
                return xmlFile;
            }
            Slog.w(TAG, "get xmlFile :" + xmlPath + " failed!");
            return null;
        } catch (NoClassDefFoundError e) {
            Slog.w(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            return null;
        }
    }

    private static HashSet<String> getHwPgSceneDetectionAppName() {
        Throwable th;
        FileInputStream fileInputStream = null;
        HashSet<String> retSet = new HashSet();
        boolean noUsePowerCureAppListLoaded = false;
        File file = getXmlFile();
        if (file == null || (file.exists() ^ 1) != 0) {
            Slog.w(TAG, "HwPgSceneDetectionAppName file==null");
            return null;
        }
        Slog.i(TAG, "get PgSceneDetectionAppListXML=" + file.getAbsolutePath());
        XmlPullParser parser = Xml.newPullParser();
        try {
            InputStream fileInputStream2 = new FileInputStream(file);
            InputStream stream;
            try {
                parser.setInput(fileInputStream2, null);
                for (int event = parser.getEventType(); event != 1; event = parser.next()) {
                    String name = parser.getName();
                    switch (event) {
                        case 2:
                            if (!name.equals(SceneDetectionNoUsePowerCure)) {
                                break;
                            }
                            noUsePowerCureAppListLoaded = true;
                            break;
                        case 3:
                            if (noUsePowerCureAppListLoaded && name.equals(NO_USE_POWERCURE_APP)) {
                                retSet.add(parser.getAttributeValue(null, APP_NAME));
                            }
                            if (!name.equals(SceneDetectionNoUsePowerCure)) {
                                break;
                            }
                            noUsePowerCureAppListLoaded = false;
                            break;
                        default:
                            break;
                    }
                }
                if (noUsePowerCureAppListLoaded) {
                    if (fileInputStream2 != null) {
                        try {
                            fileInputStream2.close();
                        } catch (IOException e) {
                            Slog.e(TAG, "File Stream close IOException!");
                        }
                    }
                    stream = fileInputStream2;
                    return null;
                }
                if (DEBUG) {
                    if (retSet.isEmpty()) {
                        Slog.d(TAG, "noUsePowerCureAppList==null");
                    } else {
                        for (String apkName : retSet) {
                            Slog.d(TAG, "noUsePowerCureAppList apkName=" + apkName);
                        }
                    }
                }
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (IOException e2) {
                        Slog.e(TAG, "File Stream close IOException!");
                    }
                }
                return retSet;
            } catch (FileNotFoundException e3) {
                fileInputStream = fileInputStream2;
                try {
                    Slog.e(TAG, "getXmlFile error FileNotFoundException!");
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "File Stream close IOException!");
                        }
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e5) {
                            Slog.e(TAG, "File Stream close IOException!");
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                stream = fileInputStream2;
                Slog.e(TAG, "getXmlFile error IOException!");
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e7) {
                        Slog.e(TAG, "File Stream close IOException!");
                    }
                }
                return null;
            } catch (XmlPullParserException e8) {
                stream = fileInputStream2;
                Slog.e(TAG, "getXmlFile error XmlPullParserException!");
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e9) {
                        Slog.e(TAG, "File Stream close IOException!");
                    }
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                stream = fileInputStream2;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e10) {
            Slog.e(TAG, "getXmlFile error FileNotFoundException!");
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (IOException e11) {
            Slog.e(TAG, "getXmlFile error IOException!");
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (XmlPullParserException e12) {
            Slog.e(TAG, "getXmlFile error XmlPullParserException!");
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        }
    }
}

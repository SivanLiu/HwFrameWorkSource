package com.android.server.display;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
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
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get xmlFile :");
            stringBuilder.append(xmlPath);
            stringBuilder.append(" failed!");
            Slog.w(str, stringBuilder.toString());
            return null;
        } catch (NoClassDefFoundError e) {
            Slog.w(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            return null;
        }
    }

    private static HashSet<String> getHwPgSceneDetectionAppName() {
        FileInputStream stream = null;
        HashSet<String> retSet = new HashSet();
        boolean noUsePowerCureAppListLoaded = false;
        File file = getXmlFile();
        if (file == null || !file.exists()) {
            Slog.w(TAG, "HwPgSceneDetectionAppName file==null");
            return null;
        }
        String mConfigFilePath = file.getAbsolutePath();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get PgSceneDetectionAppListXML=");
        stringBuilder.append(mConfigFilePath);
        Slog.i(str, stringBuilder.toString());
        XmlPullParser parser = Xml.newPullParser();
        try {
            stream = new FileInputStream(file);
            parser.setInput(stream, null);
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
                try {
                    stream.close();
                } catch (IOException e) {
                    Slog.e(TAG, "File Stream close IOException!");
                }
                return null;
            }
            if (DEBUG) {
                if (retSet.isEmpty()) {
                    Slog.d(TAG, "noUsePowerCureAppList==null");
                } else {
                    Iterator it = retSet.iterator();
                    while (it.hasNext()) {
                        String apkName = (String) it.next();
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("noUsePowerCureAppList apkName=");
                        stringBuilder2.append(apkName);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                }
            }
            try {
                stream.close();
            } catch (IOException e2) {
                Slog.e(TAG, "File Stream close IOException!");
            }
            return retSet;
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "getXmlFile error FileNotFoundException!");
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e4) {
            Slog.e(TAG, "getXmlFile error IOException!");
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e5) {
            Slog.e(TAG, "getXmlFile error XmlPullParserException!");
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6) {
                    Slog.e(TAG, "File Stream close IOException!");
                }
            }
        }
    }
}

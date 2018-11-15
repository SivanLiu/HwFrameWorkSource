package com.android.server.forcerotation;

import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;

public class HwForceRotationConfigLoader {
    private static final String FORCE_ROTATION_CFG_FILE = "force_rotation_application_list.xml";
    private static final int FORCE_ROTATION_TYPE = 0;
    private static final String TAG = "HwForceRotationConfigLoader";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_ELEMENT_BLACK_LIST = "forcerotation_applications";
    private static final String XML_ELEMENT_NOT_COMPONENT_NAME = "not_component_name";
    private static final String XML_ELEMENT_PACKAGE_NAME = "package_name";

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0064 A:{Catch:{ FileNotFoundException -> 0x0058, XmlPullParserException -> 0x0055, IOException -> 0x0052, all -> 0x004f }} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00bc A:{SYNTHETIC, Splitter: B:38:0x00bc} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwForceRotationConfig load() {
        HwForceRotationConfig config = new HwForceRotationConfig();
        InputStream inputStream = null;
        File forceRotationCfgFile = null;
        try {
            forceRotationCfgFile = HwCfgFilePolicy.getCfgFile("xml/force_rotation_application_list.xml", 0);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        } catch (Exception e2) {
            Log.d(TAG, "HwCfgFilePolicy get force_rotation_application_list exception");
        }
        if (forceRotationCfgFile != null) {
            try {
                if (forceRotationCfgFile.exists()) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("blackList:");
                    stringBuilder.append(forceRotationCfgFile);
                    stringBuilder.append(" is exist");
                    Slog.v(str, stringBuilder.toString());
                    inputStream = new FileInputStream(forceRotationCfgFile);
                    if (inputStream != null) {
                        XmlPullParser xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            if (xmlEventType == 2 && "package_name".equals(xmlParser.getName())) {
                                config.addForceRotationAppName(xmlParser.getAttributeValue(null, "name"));
                            } else if (xmlEventType != 2 || !XML_ELEMENT_NOT_COMPONENT_NAME.equals(xmlParser.getName())) {
                                if (xmlEventType == 3 && XML_ELEMENT_BLACK_LIST.equals(xmlParser.getName())) {
                                    break;
                                }
                            } else {
                                config.addNotSupportForceRotationAppActivityName(xmlParser.getAttributeValue(null, "name"));
                            }
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (FileNotFoundException e3) {
                            Log.e(TAG, "load force rotation config: IO Exception while closing stream", e3);
                        }
                    }
                    return config;
                }
            } catch (FileNotFoundException e32) {
                Log.e(TAG, "load force rotation config: ", e32);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (FileNotFoundException e322) {
                Log.e(TAG, "load force rotation config: ", e322);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (FileNotFoundException e3222) {
                Log.e(TAG, "load force rotation config: ", e3222);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "load force rotation config: IO Exception while closing stream", e4);
                    }
                }
            }
        }
        Slog.w(TAG, "force_rotation_application_list.xml is not exist");
        if (inputStream != null) {
        }
        if (inputStream != null) {
        }
        return config;
    }
}

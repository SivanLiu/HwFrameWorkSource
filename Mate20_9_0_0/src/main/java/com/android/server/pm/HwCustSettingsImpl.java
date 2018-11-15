package com.android.server.pm;

import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwCustSettingsImpl extends HwCustSettings {
    private static final String FILE_SUB_USER_NOSYSAPPS_LIST = "hdn_subuser_nosysapps_config.xml";
    private static final boolean IS_DOCOMO = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    static final String TAG = "HwCustSettingsImpl";
    private static AtomicBoolean mIsCheckNosysAppsFinished = new AtomicBoolean(false);
    private ArrayList<String> mNosysAppLists = new ArrayList();

    public boolean isInNosysAppList(String packageName) {
        if (!IS_DOCOMO) {
            return false;
        }
        if (mIsCheckNosysAppsFinished.compareAndSet(false, true)) {
            readNosysAppsFiles();
        }
        return this.mNosysAppLists.contains(packageName);
    }

    private void readNosysAppsFiles() {
        ArrayList<File> nosysAppsFileList = new ArrayList();
        try {
            nosysAppsFileList = HwCfgFilePolicy.getCfgFileList("xml/hdn_subuser_nosysapps_config.xml", 0);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        Iterator it = nosysAppsFileList.iterator();
        while (it.hasNext()) {
            loadNosysAppsFromXml((File) it.next());
        }
    }

    /* JADX WARNING: Missing block: B:20:?, code:
            r1.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadNosysAppsFromXml(File configFile) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        if (configFile.exists()) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(configFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                int depth = parser.getDepth();
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if ((next == 3 && parser.getDepth() <= depth) || type == 1) {
                        try {
                            break;
                        } catch (IOException e2) {
                            e = e2;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    } else if (type == 2) {
                        if (parser.getName().equals("add_app")) {
                            this.mNosysAppLists.add(parser.getAttributeValue(0));
                        }
                    }
                }
            } catch (FileNotFoundException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("file is not exist ");
                stringBuilder.append(e3);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                        e = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (XmlPullParserException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(configFile);
                stringBuilder.append(" ");
                stringBuilder.append(e5);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6) {
                        e = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(configFile);
                stringBuilder.append(" ");
                stringBuilder.append(e7);
                Slog.e(str, stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e8) {
                        e7 = e8;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e9) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed close stream ");
                        stringBuilder.append(e9);
                        Slog.e(TAG, stringBuilder.toString());
                    }
                }
            }
        }
        return;
        stringBuilder.append("failed close stream ");
        stringBuilder.append(e7);
        Slog.e(str, stringBuilder.toString());
    }
}

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
        int nosysListSize = nosysAppsFileList.size();
        for (int i = 0; i < nosysListSize; i++) {
            loadNosysAppsFromXml((File) nosysAppsFileList.get(i));
        }
    }

    private void loadNosysAppsFromXml(File configFile) {
        IOException e;
        FileNotFoundException e2;
        Throwable th;
        XmlPullParserException e3;
        if (configFile.exists()) {
            FileInputStream fileInputStream = null;
            try {
                FileInputStream stream = new FileInputStream(configFile);
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream, null);
                    int depth = parser.getDepth();
                    while (true) {
                        int type = parser.next();
                        if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                            if (type == 2 && parser.getName().equals("add_app")) {
                                this.mNosysAppLists.add(parser.getAttributeValue(0));
                            }
                        }
                    }
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e4) {
                            Slog.e(TAG, "failed close stream " + e4);
                        }
                    }
                } catch (FileNotFoundException e5) {
                    e2 = e5;
                    fileInputStream = stream;
                    try {
                        Slog.e(TAG, "file is not exist " + e2);
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e42) {
                                Slog.e(TAG, "failed close stream " + e42);
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e422) {
                                Slog.e(TAG, "failed close stream " + e422);
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e6) {
                    e3 = e6;
                    fileInputStream = stream;
                    Slog.e(TAG, "failed parsing " + configFile + " " + e3);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e4222) {
                            Slog.e(TAG, "failed close stream " + e4222);
                        }
                    }
                } catch (IOException e7) {
                    e4222 = e7;
                    fileInputStream = stream;
                    Slog.e(TAG, "failed parsing " + configFile + " " + e4222);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e42222) {
                            Slog.e(TAG, "failed close stream " + e42222);
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileInputStream = stream;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (FileNotFoundException e8) {
                e2 = e8;
                Slog.e(TAG, "file is not exist " + e2);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (XmlPullParserException e9) {
                e3 = e9;
                Slog.e(TAG, "failed parsing " + configFile + " " + e3);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e10) {
                e42222 = e10;
                Slog.e(TAG, "failed parsing " + configFile + " " + e42222);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
        }
    }
}

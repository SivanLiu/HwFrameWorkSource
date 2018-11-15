package com.huawei.pgmng.plug;

import android.util.Log;
import android.util.Xml;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BackLightAdjCfg {
    private static final String TAG = "BackLightAdjCfg";
    private static ArrayList<String> mNeedAdjBackLightApp;

    static {
        mNeedAdjBackLightApp = null;
        mNeedAdjBackLightApp = getNeedAdjBackLightAppFromCfg("xml/backlight_adj_app.xml");
    }

    boolean isApkShouldAdjBackLight(String pkgName) {
        if (pkgName == null || mNeedAdjBackLightApp == null) {
            return false;
        }
        return mNeedAdjBackLightApp.contains(pkgName);
    }

    private static ArrayList<String> getNeedAdjBackLightAppFromCfg(String fileName) {
        InputStream stream;
        Throwable th;
        String PKG_NAME = "pkg_name";
        FileInputStream fileInputStream = null;
        String POPULAR_GAME = "popular_app";
        ArrayList<String> retList = new ArrayList();
        try {
            File file = HwCfgFilePolicy.getCfgFile(fileName, 0);
            if (file == null || (file.exists() ^ 1) != 0) {
                return retList;
            }
            XmlPullParser mParser = Xml.newPullParser();
            try {
                InputStream fileInputStream2 = new FileInputStream(file);
                try {
                    mParser.setInput(fileInputStream2, null);
                    for (int event = mParser.getEventType(); event != 1; event = mParser.next()) {
                        String name = mParser.getName();
                        switch (event) {
                            case 3:
                                if (!name.equals(POPULAR_GAME)) {
                                    break;
                                }
                                retList.add(mParser.getAttributeValue(null, PKG_NAME));
                                break;
                            default:
                                break;
                        }
                    }
                    if (fileInputStream2 != null) {
                        try {
                            fileInputStream2.close();
                        } catch (IOException e) {
                            Log.e(TAG, "File Stream close IOException!");
                        }
                    }
                    stream = fileInputStream2;
                } catch (FileNotFoundException e2) {
                    fileInputStream = fileInputStream2;
                    try {
                        retList.clear();
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "File Stream close IOException!");
                            }
                        }
                        return retList;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e4) {
                                Log.e(TAG, "File Stream close IOException!");
                            }
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    stream = fileInputStream2;
                    retList.clear();
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                            Log.e(TAG, "File Stream close IOException!");
                        }
                    }
                    return retList;
                } catch (XmlPullParserException e7) {
                    stream = fileInputStream2;
                    retList.clear();
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e8) {
                            Log.e(TAG, "File Stream close IOException!");
                        }
                    }
                    return retList;
                } catch (Throwable th3) {
                    th = th3;
                    stream = fileInputStream2;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (FileNotFoundException e9) {
                retList.clear();
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return retList;
            } catch (IOException e10) {
                retList.clear();
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return retList;
            } catch (XmlPullParserException e11) {
                retList.clear();
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return retList;
            }
            return retList;
        } catch (NoClassDefFoundError e12) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            return retList;
        }
    }
}

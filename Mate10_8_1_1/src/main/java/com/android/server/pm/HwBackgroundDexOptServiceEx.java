package com.android.server.pm;

import android.content.Context;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class HwBackgroundDexOptServiceEx implements IHwBackgroundDexOptServiceEx {
    private static final String ATTR_NAME = "name";
    private static final String CUST_FILE_PATH = "/xml/hw_aot_compile_apps_config.xml";
    static final String TAG = "HwBackgroundDexOptServiceEx";
    private static final String TAG_NAME = "speed";
    private ArraySet<String> SPEED_MODE_PKGS;

    public HwBackgroundDexOptServiceEx(IHwBackgroundDexOptInner bdos, Context context) {
        this.SPEED_MODE_PKGS = null;
        this.SPEED_MODE_PKGS = getAllNeedForSpeedApps();
    }

    public int getReason(int reason, int reasonBackgroudDexopt, int reasonSpeedDexopt, String pkg) {
        if (this.SPEED_MODE_PKGS == null) {
            this.SPEED_MODE_PKGS = getAllNeedForSpeedApps();
        }
        if (this.SPEED_MODE_PKGS == null || !this.SPEED_MODE_PKGS.contains(pkg)) {
            return reasonBackgroudDexopt;
        }
        return reasonSpeedDexopt;
    }

    private ArraySet<String> getAllNeedForSpeedApps() {
        try {
            File file = HwCfgFilePolicy.getCfgFile(CUST_FILE_PATH, 0);
            if (file != null) {
                return readSpeedAppsFromXml(file);
            }
            Log.i(TAG, "hw_aot_compile_apps_config not exist");
            return null;
        } catch (NoClassDefFoundError e) {
            Log.i(TAG, "get speed apps failed:" + e);
            return null;
        } catch (Throwable th) {
            return null;
        }
    }

    private ArraySet<String> readSpeedAppsFromXml(File config) {
        XmlPullParserException e;
        IOException e2;
        FileInputStream fileInputStream = null;
        ArraySet<String> speedApps = null;
        if (!config.exists() || (config.canRead() ^ 1) != 0) {
            return null;
        }
        try {
            FileInputStream stream = new FileInputStream(config);
            try {
                int type;
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
                if (type != 2) {
                    Log.w(TAG, "Failed parsing config, can't find start tag");
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return null;
                }
                ArraySet<String> speedApps2 = new ArraySet();
                try {
                    int outerDepth = parser.getDepth();
                    while (true) {
                        type = parser.next();
                        if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (IOException e4) {
                                }
                            }
                        } else if (!(type == 3 || type == 4)) {
                            if (parser.getName().equals(TAG_NAME)) {
                                String name = parser.getAttributeValue(null, "name");
                                if (!TextUtils.isEmpty(name)) {
                                    speedApps2.add(name);
                                }
                            } else {
                                Log.w(TAG, "Unknown element under <config>: " + parser.getName());
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                    if (stream != null) {
                        stream.close();
                    }
                    return speedApps2;
                } catch (XmlPullParserException e5) {
                    e = e5;
                    speedApps = speedApps2;
                    fileInputStream = stream;
                } catch (IOException e6) {
                    e2 = e6;
                    speedApps = speedApps2;
                    fileInputStream = stream;
                } catch (Throwable th) {
                    speedApps = speedApps2;
                    fileInputStream = stream;
                }
            } catch (XmlPullParserException e7) {
                e = e7;
                fileInputStream = stream;
                try {
                    Log.w(TAG, "Failed parsing config " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e8) {
                        }
                    }
                    return speedApps;
                } catch (Throwable th2) {
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e9) {
                        }
                    }
                    return speedApps;
                }
            } catch (IOException e10) {
                e2 = e10;
                fileInputStream = stream;
                Log.w(TAG, "Failed parsing config " + e2);
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e11) {
                    }
                }
                return speedApps;
            } catch (Throwable th3) {
                fileInputStream = stream;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return speedApps;
            }
        } catch (XmlPullParserException e12) {
            e = e12;
            Log.w(TAG, "Failed parsing config " + e);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return speedApps;
        } catch (IOException e13) {
            e2 = e13;
            Log.w(TAG, "Failed parsing config " + e2);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return speedApps;
        }
    }
}

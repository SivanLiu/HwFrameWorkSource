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
        ArraySet<String> speedPkgs = null;
        try {
            File file = HwCfgFilePolicy.getCfgFile(CUST_FILE_PATH, 0);
            if (file == null) {
                Log.i(TAG, "hw_aot_compile_apps_config not exist");
            } else {
                speedPkgs = readSpeedAppsFromXml(file);
            }
        } catch (NoClassDefFoundError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get speed apps failed:");
            stringBuilder.append(e);
            Log.i(str, stringBuilder.toString());
        } catch (Throwable th) {
            return null;
        }
        return speedPkgs;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0043 A:{SYNTHETIC, Splitter: B:16:0x0043} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0032 A:{Catch:{ XmlPullParserException -> 0x00cb, IOException -> 0x00a9, all -> 0x00a7 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArraySet<String> readSpeedAppsFromXml(File config) {
        String str;
        StringBuilder stringBuilder;
        FileInputStream stream = null;
        if (!config.exists() || !config.canRead()) {
            return null;
        }
        try {
            int type;
            stream = new FileInputStream(config);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                type = next;
                if (next == 2 || type == 1) {
                    if (type == 2) {
                        Log.w(TAG, "Failed parsing config, can't find start tag");
                        try {
                            stream.close();
                        } catch (IOException e) {
                        }
                        return null;
                    }
                    ArraySet<String> speedApps = new ArraySet();
                    next = parser.getDepth();
                    while (true) {
                        int next2 = parser.next();
                        type = next2;
                        if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                            try {
                                stream.close();
                            } catch (IOException e2) {
                            }
                            return speedApps;
                        } else if (type != 3) {
                            if (type != 4) {
                                String name;
                                if (parser.getName().equals(TAG_NAME)) {
                                    name = parser.getAttributeValue(null, "name");
                                    if (!TextUtils.isEmpty(name)) {
                                        speedApps.add(name);
                                    }
                                } else {
                                    name = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Unknown element under <config>: ");
                                    stringBuilder2.append(parser.getName());
                                    Log.w(name, stringBuilder2.toString());
                                    XmlUtils.skipCurrentTag(parser);
                                }
                            }
                        }
                    }
                }
            }
            if (type == 2) {
            }
        } catch (XmlPullParserException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed parsing config ");
            stringBuilder.append(e3);
            Log.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e4) {
                }
            }
            return null;
        } catch (IOException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed parsing config ");
            stringBuilder.append(e5);
            Log.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6) {
                }
            }
            return null;
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e7) {
                }
            }
            return null;
        }
    }
}

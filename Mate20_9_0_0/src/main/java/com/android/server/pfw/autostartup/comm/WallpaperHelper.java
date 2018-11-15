package com.android.server.pfw.autostartup.comm;

import android.os.Environment;
import android.util.Xml;
import com.android.server.pfw.log.HwPFWLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class WallpaperHelper {
    private static final String TAG = "WallpaperHelper";
    private static final String WALLPAPER_INFO = "wallpaper_info.xml";

    private static File getWallpaperDir(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:78:0x01a0  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x019f A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static String getWallpaperPkgName(int userId) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        String componentName = null;
        File file = new File(getWallpaperDir(userId), WALLPAPER_INFO);
        if (!file.exists()) {
            return null;
        }
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            int type;
            do {
                type = parser.next();
                if (type == 2) {
                    if ("wp".equals(parser.getName())) {
                        componentName = parser.getAttributeValue(null, "component");
                    }
                }
            } while (type != 1);
            try {
                stream.close();
            } catch (IOException e2) {
                e = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (FileNotFoundException e3) {
            HwPFWLogger.w(TAG, "no current wallpaper -- first boot?");
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e4) {
                    e = e4;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (NullPointerException e5) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e5);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e6) {
                    e = e6;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (NumberFormatException e7) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e7);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e8) {
                    e = e8;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (XmlPullParserException e9) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e9);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e10) {
                    e = e10;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IOException e11) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e11);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e12) {
                    e11 = e12;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IndexOutOfBoundsException e13) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e13);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e14) {
                    e11 = e14;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (RuntimeException e15) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e15);
            HwPFWLogger.w(str, stringBuilder.toString());
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e16) {
                    e11 = e16;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e112) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("failed close stream ");
                    stringBuilder2.append(e112);
                    HwPFWLogger.e(TAG, stringBuilder2.toString());
                }
            }
        }
        if (componentName != null) {
            return null;
        }
        int sep = componentName.indexOf(47);
        if (sep < 0 || sep + 1 >= componentName.length()) {
            return null;
        }
        return componentName.substring(0, sep);
        stringBuilder.append("failed close stream ");
        stringBuilder.append(e112);
        HwPFWLogger.e(str, stringBuilder.toString());
        if (componentName != null) {
        }
    }
}

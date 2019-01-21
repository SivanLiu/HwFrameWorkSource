package huawei.android.widget;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;
import android.widget.IHwWechatOptimize;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwWechatOptimizeImpl implements IHwWechatOptimize {
    private static final String ATTR_NAME = "name";
    private static String CONFIG_FILEPATH = "/data/app_acc/app_config.xml";
    private static final boolean Debug = true;
    private static String SWITCH_FILEPATH = "/data/app_acc/app_switch.xml";
    private static final String TAG = "HwWechatOptimizeImpl";
    private static final String TEXT_NAME = "AppList";
    private static final String XML_TAG_APPNAME = "packageName";
    private static final String XML_TAG_CONFIG = "config";
    private static final String XML_TAG_FLINGVELOCITY = "flingVelocity";
    private static final String XML_TAG_IDLEVELOCITY = "idleVelocity";
    private static final String XML_TAG_ITEM = "item";
    private static final String XML_TAG_SWITCH = "switch";
    private static final String XML_TAG_VERSION = "supportVersion";
    private static HwWechatOptimizeImpl mHwWechatOptimizeImpl = null;
    private AppData mAppData = null;
    private String mCurrentPackageName = null;
    private boolean mIsEffect = false;
    private boolean mIsFling = false;

    private static class AppData {
        public String mAppName;
        public int mFlingVelocity;
        public int mIdleVelocity;
        public String mSupportVersion;

        public AppData(String name, String supportVersion, int flingVelocity, int idleVelocity) {
            this.mAppName = name;
            this.mSupportVersion = supportVersion;
            this.mFlingVelocity = flingVelocity;
            this.mIdleVelocity = idleVelocity;
        }
    }

    public static synchronized HwWechatOptimizeImpl getInstance() {
        HwWechatOptimizeImpl hwWechatOptimizeImpl;
        synchronized (HwWechatOptimizeImpl.class) {
            if (mHwWechatOptimizeImpl == null) {
                mHwWechatOptimizeImpl = new HwWechatOptimizeImpl();
            }
            hwWechatOptimizeImpl = mHwWechatOptimizeImpl;
        }
        return hwWechatOptimizeImpl;
    }

    private HwWechatOptimizeImpl() {
        if (SystemProperties.getBoolean("persist.sys.enable_iaware", false)) {
            this.mCurrentPackageName = ActivityThread.currentPackageName();
            if (this.mCurrentPackageName != null && !this.mCurrentPackageName.isEmpty()) {
                if ("com.tencent.mm".equals(this.mCurrentPackageName) && isSwitchEnabled() && loadConfigFile()) {
                    this.mIsEffect = true;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mIsEffect:");
                stringBuilder.append(this.mIsEffect);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private File getFile(String fileName) {
        return new File(fileName);
    }

    private boolean isSwitchEnabled() {
        File file = getFile(SWITCH_FILEPATH);
        if (!file.exists()) {
            return false;
        }
        InputStream is = null;
        String parser = null;
        try {
            is = new FileInputStream(file);
            XmlPullParser parser2 = Xml.newPullParser();
            parser2.setInput(is, StandardCharsets.UTF_8.name());
            int outerDepth = parser2.getDepth();
            while (true) {
                int next = parser2.next();
                int type = next;
                if (next == 1 || (type == 3 && parser2.getDepth() <= outerDepth)) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "close file input stream fail!");
                    }
                    if (parser2 != null) {
                        try {
                            ((KXmlParser) parser2).close();
                        } catch (Exception e2) {
                            Log.e(TAG, "parser close error");
                        }
                    }
                    return false;
                } else if (type != 3) {
                    if (type != 4) {
                        if (XML_TAG_SWITCH.equals(parser2.getName())) {
                            if (Integer.parseInt(parser2.nextText()) == 1) {
                                try {
                                    is.close();
                                } catch (IOException e3) {
                                    Log.e(TAG, "close file input stream fail!");
                                }
                                if (parser2 != null) {
                                    try {
                                        ((KXmlParser) parser2).close();
                                    } catch (Exception e4) {
                                        Log.e(TAG, "parser close error");
                                    }
                                }
                                return true;
                            }
                            try {
                                is.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "close file input stream fail!");
                            }
                            if (parser2 != null) {
                                try {
                                    ((KXmlParser) parser2).close();
                                } catch (Exception e6) {
                                    Log.e(TAG, "parser close error");
                                }
                            }
                            return false;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e7) {
            Log.e(TAG, "failed parsing switch file parser error");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e8) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e9) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (IOException e10) {
            Log.e(TAG, "failed parsing switch file IO error ");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e11) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e12) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (NumberFormatException e13) {
            Log.e(TAG, "switch number format error");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e14) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e15) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e16) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e17) {
                    Log.e(TAG, "parser close error");
                }
            }
        }
    }

    private boolean loadConfigFile() {
        File file = getFile(CONFIG_FILEPATH);
        if (!file.exists()) {
            return false;
        }
        InputStream is = null;
        XmlPullParser parser = null;
        try {
            is = new FileInputStream(file);
            parser = Xml.newPullParser();
            parser.setInput(is, StandardCharsets.UTF_8.name());
            int outerDepth = parser.getDepth();
            String tagName = null;
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        Log.e(TAG, "close file input stream fail!");
                    }
                    if (parser != null) {
                        try {
                            ((KXmlParser) parser).close();
                        } catch (Exception e2) {
                            Log.e(TAG, "parser close error");
                        }
                    }
                    return false;
                } else if (type != 3) {
                    if (type != 4) {
                        if (XML_TAG_CONFIG.equals(parser.getName()) && TEXT_NAME.equals(parser.getAttributeValue(null, ATTR_NAME))) {
                            boolean appOptimized = false;
                            if (checkAppListFromXml(parser)) {
                                appOptimized = true;
                            }
                            try {
                                is.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "close file input stream fail!");
                            }
                            if (parser != null) {
                                try {
                                    ((KXmlParser) parser).close();
                                } catch (Exception e4) {
                                    Log.e(TAG, "parser close error");
                                }
                            }
                            return appOptimized;
                        }
                    }
                }
            }
        } catch (XmlPullParserException e5) {
            Log.e(TAG, "failed parsing config file parser error");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e7) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (IOException e8) {
            Log.e(TAG, "failed parsing config file IO error ");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e9) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e10) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (NumberFormatException e11) {
            Log.e(TAG, "config number format error");
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e12) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e13) {
                    Log.e(TAG, "parser close error");
                }
            }
            return false;
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e14) {
                    Log.e(TAG, "close file input stream fail!");
                }
            }
            if (parser != null) {
                try {
                    ((KXmlParser) parser).close();
                } catch (Exception e15) {
                    Log.e(TAG, "parser close error");
                }
            }
        }
    }

    private boolean checkAppListFromXml(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return false;
            }
            if (type != 3) {
                if (type != 4) {
                    if (XML_TAG_ITEM.equals(parser.getName())) {
                        this.mAppData = new AppData();
                        readAppDataFromXml(parser, this.mAppData);
                        if (!(this.mAppData.mAppName == null || this.mAppData.mSupportVersion == null || !this.mAppData.mAppName.equals(this.mCurrentPackageName))) {
                            return isWechatVersionSupport(this.mAppData.mAppName, this.mAppData.mSupportVersion);
                        }
                    }
                }
            }
        }
        return false;
    }

    void readAppDataFromXml(XmlPullParser parser, AppData appdata) throws XmlPullParserException, IOException, NumberFormatException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    String tag = parser.getName();
                    if ("packageName".equals(tag)) {
                        appdata.mAppName = parser.nextText();
                    } else if (XML_TAG_VERSION.equals(tag)) {
                        appdata.mSupportVersion = parser.nextText();
                    } else if (XML_TAG_FLINGVELOCITY.equals(tag)) {
                        appdata.mFlingVelocity = Integer.parseInt(parser.nextText());
                    } else if (XML_TAG_IDLEVELOCITY.equals(tag)) {
                        appdata.mIdleVelocity = Integer.parseInt(parser.nextText());
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown  tag: ");
                        stringBuilder.append(tag);
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    public boolean isWechatOptimizeEffect() {
        return this.mIsEffect;
    }

    public int getWechatFlingVelocity() {
        if (this.mAppData == null) {
            return 0;
        }
        return this.mAppData.mFlingVelocity;
    }

    public int getWechatIdleVelocity() {
        if (this.mAppData == null) {
            return 0;
        }
        return this.mAppData.mIdleVelocity;
    }

    public boolean isWechatFling() {
        return this.mIsFling;
    }

    public void setWechatFling(boolean isFling) {
        this.mIsFling = isFling;
    }

    private boolean isWechatVersionSupport(String appName, String supportVersion) {
        Context context = ActivityThread.currentApplication();
        if (context == null) {
            return false;
        }
        try {
            int currentVersionCode = context.getPackageManager().getPackageInfo(appName, 0).versionCode;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isWechatVersionSupport currentVersionCode:");
            stringBuilder.append(currentVersionCode);
            Log.d(str, stringBuilder.toString());
            return versionInRange(currentVersionCode, supportVersion);
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    boolean versionInRange(int checkedVersion, String versionRanage) {
        if (versionRanage == null) {
            return false;
        }
        int checkedVersionEnd;
        int versionIndex = versionRanage.indexOf(";");
        String versionPreRange;
        if (versionIndex >= 0) {
            versionPreRange = versionRanage.substring(0, versionIndex);
        } else {
            versionPreRange = versionRanage;
        }
        for (String[] VersionStartAndEnd : versionPreRange.split(",")) {
            String[] VersionStartAndEnd2 = VersionStartAndEnd2.split("-");
            if (VersionStartAndEnd2.length >= 2) {
                try {
                    int checkedVersionStart = Integer.parseInt(VersionStartAndEnd2[0]);
                    checkedVersionEnd = Integer.parseInt(VersionStartAndEnd2[1]);
                    if (checkedVersion >= checkedVersionStart && checkedVersion <= checkedVersionEnd) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "version number format error");
                    return false;
                }
            }
        }
        if (versionIndex >= 0) {
            String[] versionPostArray = versionRanage.substring(versionIndex + 1).split(",");
            checkedVersionEnd = versionPostArray.length;
            int i = 0;
            while (i < checkedVersionEnd) {
                int specialVersion = 0;
                try {
                    if (checkedVersion == Integer.parseInt(versionPostArray[i])) {
                        return true;
                    }
                    i++;
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "version number format error");
                    return false;
                }
            }
        }
        return false;
    }
}

package com.android.server.magicwin;

import android.content.Context;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.HwMwUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.magicwin.HwMagicWindowConfig;
import com.huawei.cust.HwCfgFilePolicy;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class HwMagicWindowConfigLoader {
    public static final String CLOUD_CONFIG_FILE_PATH = "/data/system/";
    public static final String CLOUD_CONFIG_FILE_PREFIX = "magicWindowFeature_";
    public static final String CLOUD_PACKAGE_CONFIG_FILE_NAME = "magicWindowFeature_magic_window_application_list.xml";
    public static final String HOME_CONFIG_FILE_NAME = "magic_window_homepage_list.xml";
    public static final String LAST_VERSION = "lastVersion";
    public static final String LOCAL_PART_CONFIG_FILE_PATH = "/xml/";
    public static final String PACKAGE_CONFIG_FILE_NAME = "magic_window_application_list.xml";
    private static final String SETTING_CONFIG_BACKUP_FILE_NAME = "magic_window_setting_config-backup.xml";
    private static final String SETTING_CONFIG_FILE_NAME = "magic_window_setting_config.xml";
    public static final String SYSTEM_CONFIG_FILE_NAME = "magic_window_system_config.xml";
    private static final String TAG = "HwMagicWindowConfigLoader";
    private static final String XML_ATTRIBUTE_BACKGROUND = "support_background";
    private static final String XML_ATTRIBUTE_BACK_MIDDLE = "is_back_to_middle";
    private static final String XML_ATTRIBUTE_BOTTOM_PADDING = "bottom_padding";
    private static final String XML_ATTRIBUTE_CMAERA_PREVIEW = "support_camera_preview";
    private static final String XML_ATTRIBUTE_CORNER_RADIUS = "corner_radius";
    private static final String XML_ATTRIBUTE_DEFAULT_SETTING = "default_setting";
    private static final String XML_ATTRIBUTE_DYNAMIC_EFFECT = "support_dynamic_effect";
    private static final String XML_ATTRIBUTE_FULLSCREEN_VIDEO = "support_fullscreen_video";
    private static final String XML_ATTRIBUTE_HOME = "home";
    private static final String XML_ATTRIBUTE_HOST_VIEW_THRESHOLD = "host_view_threshold";
    private static final String XML_ATTRIBUTE_IS_DRAGABLE = "is_dragable";
    private static final String XML_ATTRIBUTE_LEFT_PADDING = "left_padding";
    private static final String XML_ATTRIBUTE_LEFT_RESUME = "support_multi_resume";
    private static final String XML_ATTRIBUTE_MID_DRAG_PADDING = "mid_drag_padding";
    private static final String XML_ATTRIBUTE_MID_PADDING = "mid_padding";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_ATTRIBUTE_NEED_RELAUNCH = "need_relaunch";
    private static final String XML_ATTRIBUTE_NOTCH_ADAPT = "notch_adapt";
    private static final String XML_ATTRIBUTE_OPEN_CAPABILITY = "support_open_capability";
    private static final String XML_ATTRIBUTE_RIGHT_PADDING = "right_padding";
    private static final String XML_ATTRIBUTE_ROUND_ANGLE = "support_round_angle";
    private static final String XML_ATTRIBUTE_SCALE_ENABLED = "is_scaled";
    private static final String XML_ATTRIBUTE_SETTING_CONDIG = "setting_config";
    private static final String XML_ATTRIBUTE_SETTING_DRAG_MODE = "hwDragMode";
    private static final String XML_ATTRIBUTE_SETTING_ENABLED = "hwMagicWinEnabled";
    private static final String XML_ATTRIBUTE_SETTING_SHOWN = "hwDialogShown";
    private static final String XML_ATTRIBUTE_TOP_PADDING = "top_padding";
    private static final String XML_ATTRIBUTE_WINDOW_MODE = "window_mode";
    private static final String XML_ELEMENT_PACKAGE = "package";
    private static final String XML_ELEMENT_SETTING = "setting";
    private static final String XML_ELEMENT_SYSTEM = "system";
    private File mBackupSettingFilename;
    private Context mContext;
    private File mSettingFilename;
    private File mSystemDir;

    public HwMagicWindowConfigLoader(Context cxt, int userId) {
        this.mContext = cxt;
        String currentVersion = SystemProperties.get("ro.build.version.incremental", "");
        if (!currentVersion.equals(Settings.Global.getString(this.mContext.getContentResolver(), LAST_VERSION))) {
            Slog.i(TAG, "This is a new version");
            Settings.Global.putString(this.mContext.getContentResolver(), LAST_VERSION, currentVersion);
            clearDownloadConfig("/data/system/", CLOUD_PACKAGE_CONFIG_FILE_NAME);
        }
        initSettingsDirForUser(userId);
    }

    public void initSettingsDirForUser(int userId) {
        File dataDirectory = Environment.getDataDirectory();
        this.mSystemDir = new File(dataDirectory, "system/users/" + userId);
        boolean result = false;
        try {
            result = this.mSystemDir.mkdirs();
        } catch (SecurityException e) {
            Slog.e(TAG, "Exception throw while Making dir");
        }
        if (!result) {
            Slog.e(TAG, "Making dir failed");
        }
        FileUtils.setPermissions(this.mSystemDir.toString(), 509, -1, -1);
        this.mSettingFilename = new File(this.mSystemDir, SETTING_CONFIG_FILE_NAME);
        this.mBackupSettingFilename = new File(this.mSystemDir, SETTING_CONFIG_BACKUP_FILE_NAME);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00ee, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00ef, code lost:
        android.util.Slog.e(com.android.server.magicwin.HwMagicWindowConfigLoader.TAG, r8 + "load config: ", r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0102, code lost:
        if (0 != 0) goto L_0x0104;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:?, code lost:
        r6.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x0108, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0109, code lost:
        r1 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00ee A[ExcHandler: IOException | NumberFormatException | XmlPullParserException (r0v18 'e' java.lang.Exception A[CUSTOM_DECLARE]), Splitter:B:6:0x0022] */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0113  */
    /* JADX WARNING: Removed duplicated region for block: B:56:? A[RETURN, SYNTHETIC] */
    public void loadPackage(HwMagicWindowConfig config) {
        StringBuilder sb;
        InputStream inputStreamPkg = null;
        String packageConfigFileName = null;
        try {
            File magicWindowPkgFile = new File("/data/system/", CLOUD_PACKAGE_CONFIG_FILE_NAME);
            if (magicWindowPkgFile.exists()) {
                packageConfigFileName = CLOUD_PACKAGE_CONFIG_FILE_NAME;
                try {
                    Slog.i(TAG, packageConfigFileName + " is exist");
                    inputStreamPkg = new FileInputStream(magicWindowPkgFile);
                } catch (IOException | NumberFormatException | XmlPullParserException e) {
                }
            } else {
                Slog.i(TAG, ((String) null) + " is not exist");
                packageConfigFileName = PACKAGE_CONFIG_FILE_NAME;
                File magicWindowPkgFile2 = HwCfgFilePolicy.getCfgFile("/xml/magic_window_application_list.xml", 0);
                if (magicWindowPkgFile2 != null) {
                    Slog.i(TAG, packageConfigFileName + " is exist");
                    inputStreamPkg = new FileInputStream(magicWindowPkgFile2);
                } else {
                    Slog.i(TAG, packageConfigFileName + " is not exist");
                }
            }
            if (inputStreamPkg != null) {
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStreamPkg, null);
                for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                    if (xmlEventType == 2 && XML_ELEMENT_PACKAGE.equals(xmlParser.getName())) {
                        parsePackageXml(config, xmlParser);
                    }
                }
            }
            if (inputStreamPkg != null) {
                try {
                    inputStreamPkg.close();
                } catch (IOException e2) {
                    e = e2;
                    sb = new StringBuilder();
                }
            }
        } catch (Exception e3) {
            Slog.e(TAG, ((String) null) + "load config: ", e3);
            if (0 != 0) {
                try {
                    inputStreamPkg.close();
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStreamPkg.close();
                } catch (IOException e5) {
                    Slog.e(TAG, ((String) null) + "load  config: IO Exception while closing stream", e5);
                }
            }
            throw th;
        }
        if (!HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.v(TAG, packageConfigFileName + "load config out");
            return;
        }
        return;
        sb.append(packageConfigFileName);
        sb.append("load  config: IO Exception while closing stream");
        Slog.e(TAG, sb.toString(), e);
        if (!HwMwUtils.MAGICWIN_LOG_SWITCH) {
        }
    }

    private void parsePackageXml(HwMagicWindowConfig config, XmlPullParser xmlParser) {
        String packageName = xmlParser.getAttributeValue(null, "name");
        String windowMode = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_WINDOW_MODE);
        String fullscreenVideo = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_FULLSCREEN_VIDEO);
        String leftResume = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_LEFT_RESUME);
        String cameraPreview = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_CMAERA_PREVIEW);
        String isScaleEnabled = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_SCALE_ENABLED);
        String needRelaunch = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_NEED_RELAUNCH);
        String defaultSetting = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_DEFAULT_SETTING);
        String isDragable = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_IS_DRAGABLE);
        String isNotchAdapted = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_NOTCH_ADAPT);
        String packageHome = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_HOME);
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.d(TAG, "parser: packageName = " + packageName);
            Slog.d(TAG, "parser: windowMode = " + windowMode);
            Slog.d(TAG, "parser: fullscreenVideo = " + fullscreenVideo);
            Slog.d(TAG, "parser: leftResume = " + leftResume);
            Slog.d(TAG, "parser: cameraPreview = " + cameraPreview);
            Slog.d(TAG, "parser: isScaleEnabled = " + isScaleEnabled);
            Slog.d(TAG, "parser: needRelaunch = " + needRelaunch);
            Slog.d(TAG, "parser: defaultSetting = " + defaultSetting);
            Slog.d(TAG, "parser: isDragable = " + isDragable);
            Slog.d(TAG, "parser: isNotchAdapted = " + isNotchAdapted);
            Slog.d(TAG, "parser: home = " + packageHome);
        }
        if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(windowMode) && !TextUtils.isEmpty(fullscreenVideo)) {
            if (!TextUtils.isEmpty(leftResume) && !TextUtils.isEmpty(cameraPreview)) {
                if (TextUtils.isEmpty(needRelaunch)) {
                    return;
                }
                if (!TextUtils.isEmpty(isScaleEnabled)) {
                    int mWindowMode = Integer.parseInt(windowMode);
                    if (config.getIsSupportOpenCap() || !(mWindowMode == -2 || mWindowMode == -1)) {
                        config.createPackage(new HwMagicWindowConfig.PackageConfig(packageName, windowMode, fullscreenVideo, leftResume, cameraPreview, isScaleEnabled, needRelaunch, defaultSetting, isDragable, isNotchAdapted));
                        if (!TextUtils.isEmpty(packageHome)) {
                            config.createHome(packageName, packageHome.split(",", 0));
                        }
                    }
                }
            }
        }
    }

    public void loadSystem(HwMagicWindowConfig config) {
        InputStream inputStreamSystem = null;
        try {
            File magicWindowSystemFile = HwCfgFilePolicy.getCfgFile("/xml/magic_window_system_config.xml", 0);
            if (magicWindowSystemFile != null) {
                Slog.v(TAG, "magic_window_system_config.xml is exist");
                inputStreamSystem = new FileInputStream(magicWindowSystemFile);
            } else {
                Slog.v(TAG, "magic_window_system_config.xml is not exist");
            }
            if (inputStreamSystem != null) {
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStreamSystem, null);
                for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                    if (xmlEventType == 2 && XML_ELEMENT_SYSTEM.equals(xmlParser.getName())) {
                        parseSystemXml(config, xmlParser);
                    }
                }
            }
            if (inputStreamSystem != null) {
                try {
                    inputStreamSystem.close();
                } catch (IOException e) {
                    Slog.e(TAG, "magic_window_system_config.xmlload  config: IO Exception while closing stream", e);
                }
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "magic_window_system_config.xmlload config: ", e2);
            if (0 != 0) {
                inputStreamSystem.close();
            }
        } catch (XmlPullParserException e3) {
            Slog.e(TAG, "magic_window_system_config.xmlload config: ", e3);
            if (0 != 0) {
                inputStreamSystem.close();
            }
        } catch (IOException e4) {
            Slog.e(TAG, "magic_window_system_config.xmlload config: ", e4);
            if (0 != 0) {
                inputStreamSystem.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStreamSystem.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "magic_window_system_config.xmlload  config: IO Exception while closing stream", e5);
                }
            }
            throw th;
        }
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.v(TAG, "magic_window_system_config.xmlload config out");
        }
    }

    private void parseSystemXml(HwMagicWindowConfig config, XmlPullParser xmlParser) {
        String leftPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_LEFT_PADDING);
        String topPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_TOP_PADDING);
        String rightPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_RIGHT_PADDING);
        String bottomPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_BOTTOM_PADDING);
        String midPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_MID_PADDING);
        String midDragPadding = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_MID_DRAG_PADDING);
        String supportRoundAngle = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_ROUND_ANGLE);
        String supportDynamicEffect = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_DYNAMIC_EFFECT);
        String supportBackground = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_BACKGROUND);
        String supportOpenCapability = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_OPEN_CAPABILITY);
        String isBackToMiddle = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_BACK_MIDDLE);
        String cornerRadius = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_CORNER_RADIUS);
        String hostViewThreshold = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_HOST_VIEW_THRESHOLD);
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.d(TAG, "parser: leftPadding = " + leftPadding);
            Slog.d(TAG, "parser: topPadding = " + topPadding);
            Slog.d(TAG, "parser: rightPadding = " + rightPadding);
            Slog.d(TAG, "parser: bottomPadding = " + bottomPadding);
            Slog.d(TAG, "parser: midPadding = " + midPadding);
            Slog.d(TAG, "parser: midDragPadding = " + midDragPadding);
            Slog.d(TAG, "parser: supportRoundAngle = " + supportRoundAngle);
            Slog.d(TAG, "parser: supportDynamicEffect = " + supportDynamicEffect);
            Slog.d(TAG, "parser: supportBackground = " + supportBackground);
            Slog.d(TAG, "parser: supportOpenCapability = " + supportOpenCapability);
            Slog.d(TAG, "parser: isBackToMiddle = " + isBackToMiddle);
            Slog.d(TAG, "parser: cornerRadius = " + cornerRadius);
            Slog.d(TAG, "parser: hostViewThreshold = " + hostViewThreshold);
        }
        if (!TextUtils.isEmpty(leftPadding) && !TextUtils.isEmpty(topPadding) && !TextUtils.isEmpty(rightPadding)) {
            if (!TextUtils.isEmpty(bottomPadding) && !TextUtils.isEmpty(midPadding) && !TextUtils.isEmpty(midDragPadding)) {
                if (!TextUtils.isEmpty(supportRoundAngle) && !TextUtils.isEmpty(supportDynamicEffect)) {
                    if (TextUtils.isEmpty(supportBackground)) {
                        return;
                    }
                    if (!TextUtils.isEmpty(isBackToMiddle)) {
                        HwMagicWindowConfig.SystemConfig systemConfig = new HwMagicWindowConfig.SystemConfig(leftPadding, topPadding, rightPadding, bottomPadding, midPadding, midDragPadding, supportRoundAngle, supportDynamicEffect, supportBackground, isBackToMiddle, cornerRadius);
                        systemConfig.setHostViewThreshold(hostViewThreshold);
                        config.createSystem(systemConfig);
                        config.setOpenCapability(supportOpenCapability);
                    }
                }
            }
        }
    }

    public void readSetting(HwMagicWindowConfig config) {
        FileInputStream settingsFileStream = null;
        if (this.mBackupSettingFilename.exists()) {
            try {
                settingsFileStream = new FileInputStream(this.mBackupSettingFilename);
                if (this.mSettingFilename.exists()) {
                    Slog.v(TAG, "Cleaning upmagic_window_setting_config.xml");
                    this.mSettingFilename.delete();
                }
            } catch (IOException e) {
                Slog.e(TAG, "magic_window_setting_config-backup.xmlload config: ", e);
            }
        }
        if (settingsFileStream == null) {
            try {
                if (!this.mSettingFilename.exists()) {
                    Slog.v(TAG, "magic_window_setting_config.xml not found");
                    if (settingsFileStream != null) {
                        try {
                            settingsFileStream.close();
                            return;
                        } catch (IOException e2) {
                            Slog.e(TAG, "magic_window_setting_config.xmlload config: IO Exception while closing stream", e2);
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    settingsFileStream = new FileInputStream(this.mSettingFilename);
                }
            } catch (FileNotFoundException e3) {
                Slog.e(TAG, "magic_window_setting_config.xmlload config: ", e3);
                if (settingsFileStream != null) {
                    settingsFileStream.close();
                }
            } catch (XmlPullParserException e4) {
                Slog.e(TAG, "magic_window_setting_config.xmlload config: ", e4);
                if (settingsFileStream != null) {
                    settingsFileStream.close();
                }
            } catch (IOException e5) {
                Slog.e(TAG, "magic_window_setting_config.xmlload config: ", e5);
                if (settingsFileStream != null) {
                    settingsFileStream.close();
                }
            } catch (Throwable th) {
                if (settingsFileStream != null) {
                    try {
                        settingsFileStream.close();
                    } catch (IOException e6) {
                        Slog.e(TAG, "magic_window_setting_config.xmlload config: IO Exception while closing stream", e6);
                    }
                }
                throw th;
            }
        }
        XmlPullParser xmlParser = Xml.newPullParser();
        xmlParser.setInput(settingsFileStream, null);
        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
            if (xmlEventType == 2 && XML_ELEMENT_SETTING.equals(xmlParser.getName())) {
                String packageName = xmlParser.getAttributeValue(null, "name");
                String hwMagicWinEnabled = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_SETTING_ENABLED);
                String hwDialogShown = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_SETTING_SHOWN);
                String hwDragMode = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_SETTING_DRAG_MODE);
                if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
                    Slog.d(TAG, "parser: packageName = " + packageName);
                    Slog.d(TAG, "parser: hwMagicWinEnabled = " + hwMagicWinEnabled);
                    Slog.d(TAG, "parser: hwDialogShown = " + hwDialogShown);
                    Slog.d(TAG, "parser: hwDragMode = " + hwDragMode);
                }
                config.createSetting(packageName, hwMagicWinEnabled, hwDialogShown, hwDragMode);
            }
        }
        try {
            settingsFileStream.close();
        } catch (IOException e7) {
            Slog.e(TAG, "magic_window_setting_config.xmlload config: IO Exception while closing stream", e7);
        }
        if (HwMwUtils.MAGICWIN_LOG_SWITCH) {
            Slog.d(TAG, "magic_window_setting_config.xmlload config out");
        }
    }

    public void writeSetting(HwMagicWindowConfig config) {
        long startTime = SystemClock.uptimeMillis();
        if (this.mSettingFilename.exists()) {
            if (this.mBackupSettingFilename.exists()) {
                this.mSettingFilename.delete();
                Slog.v(TAG, "magic_window_setting_config.xmldelete old file");
            } else if (!this.mSettingFilename.renameTo(this.mBackupSettingFilename)) {
                Slog.e(TAG, "Unable to backup magic_window_setting_config,  current changes will be lost at reboot");
                return;
            }
        }
        FileOutputStream fstr = null;
        BufferedOutputStream settingsFileStream = null;
        try {
            FileOutputStream fstr2 = new FileOutputStream(this.mSettingFilename);
            BufferedOutputStream settingsFileStream2 = new BufferedOutputStream(fstr2);
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(settingsFileStream2, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, XML_ATTRIBUTE_SETTING_CONDIG);
            for (HwMagicWindowConfig.SettingConfig host : config.getHwMagicWinSettingConfigs().values()) {
                serializer.startTag(null, XML_ELEMENT_SETTING);
                serializer.attribute(null, "name", host.getName());
                serializer.attribute(null, XML_ATTRIBUTE_SETTING_ENABLED, String.valueOf(host.getHwMagicWinEnabled()));
                serializer.attribute(null, XML_ATTRIBUTE_SETTING_SHOWN, String.valueOf(host.getHwDialogShown()));
                serializer.attribute(null, XML_ATTRIBUTE_SETTING_DRAG_MODE, String.valueOf(host.getDragMode()));
                serializer.endTag(null, XML_ELEMENT_SETTING);
            }
            serializer.endTag(null, XML_ATTRIBUTE_SETTING_CONDIG);
            serializer.endDocument();
            settingsFileStream2.flush();
            FileUtils.sync(fstr2);
            this.mBackupSettingFilename.delete();
            FileUtils.setPermissions(this.mSettingFilename.toString(), 432, -1, -1);
            Slog.v(TAG, "write magic_window_setting_config.xml  took " + (SystemClock.uptimeMillis() - startTime) + "ms");
            try {
                fstr2.close();
                settingsFileStream2.close();
            } catch (IOException e) {
                Slog.e(TAG, "magic_window_setting_config.xmlwrite config: IO Exception while closing stream", e);
            }
        } catch (IOException e2) {
            Slog.e(TAG, "Unable to write host recognize settings, current changes will be lost at reboot", e2);
            if (0 != 0) {
                try {
                    fstr.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "magic_window_setting_config.xmlwrite config: IO Exception while closing stream", e3);
                    if (this.mSettingFilename.exists()) {
                        return;
                    }
                }
            }
            if (0 != 0) {
                settingsFileStream.close();
            }
            if (this.mSettingFilename.exists() && !this.mSettingFilename.delete()) {
                Slog.w(TAG, "Failed to clean up mangled file: " + this.mSettingFilename);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    fstr.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "magic_window_setting_config.xmlwrite config: IO Exception while closing stream", e4);
                    throw th;
                }
            }
            if (0 != 0) {
                settingsFileStream.close();
            }
            throw th;
        }
    }

    private void clearDownloadConfig(String path, String file) {
        try {
            File configFile = new File(path, file);
            if (configFile.exists() && configFile.isFile()) {
                configFile.delete();
            }
        } catch (SecurityException e) {
            Slog.w(TAG, "Delete file failed. Path: " + path + file);
        }
    }
}

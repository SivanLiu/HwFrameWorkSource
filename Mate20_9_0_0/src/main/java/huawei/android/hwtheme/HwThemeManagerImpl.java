package huawei.android.hwtheme;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.Notification;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.IOverlayManager.Stub;
import android.content.om.OverlayInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageParser.Activity;
import android.content.pm.ResolveInfo;
import android.content.pm.ResolveInfoUtils;
import android.content.res.AbsResourcesImpl;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.ConfigurationEx;
import android.content.res.HwResources;
import android.content.res.HwResourcesImpl;
import android.content.res.IHwConfiguration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hwtheme.HwThemeManager;
import android.hwtheme.HwThemeManager.IHwThemeManager;
import android.os.Environment;
import android.os.FreezeScreenScene;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.WindowManager;
import com.huawei.android.hwutil.CommandLineUtil;
import com.huawei.hsm.permission.StubController;
import com.huawei.utils.reflect.EasyInvokeFactory;
import huawei.android.hwutil.IconCache;
import huawei.android.hwutil.IconCache.CacheEntry;
import huawei.android.provider.HwSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HwThemeManagerImpl implements IHwThemeManager {
    private static final String ALARM_CLASS_POSTFIX = "_alarm.";
    static final String BLURRED_WALLPAPER = "/blurwallpaper";
    private static final String CALENDAR_CLASS_POSTFIX = "_calendar.";
    static final String CURRENT_HOMEWALLPAPER_NAME = "home_wallpaper_0.jpg";
    static final String CURRENT_HOMEWALLPAPER_NAME_PNG = "home_wallpaper_0.png";
    static final String CUST_THEME_NAME = "hw_def_theme";
    static final String CUST_WALLPAPER = "/data/cust/wallpaper/wallpaper1.jpg";
    static final String CUST_WALLPAPER_DIR = "/data/cust/wallpaper/";
    static final String CUST_WALLPAPER_FILE_NAME = "wallpaper1.jpg";
    static final boolean DEBUG = false;
    private static final String DESCRIPTOR = "huawei.com.android.server.IPackageManager";
    private static final ComponentName DOCOMOHOME_COMPONENT = new ComponentName("com.nttdocomo.android.dhome", "com.nttdocomo.android.dhome.HomeActivity");
    private static final ComponentName DRAWERHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.drawer.DrawerLauncher");
    private static final String EMAIL_CLASS_POSTFIX = "_email.";
    private static final String FWK_HONOR_TAG = "com.android.frameworkhwext.honor";
    private static final String FWK_NOVA_TAG = "com.android.frameworkhwext.nova";
    private static final int HWTHEME_DISABLED = 0;
    private static final String HWTHEME_GET_NOTIFICATION_INFO = "com.huawei.hwtheme.permission.GET_NOTIFICATION_INFO";
    private static final String IPACKAGE_MANAGER_DESCRIPTOR = "huawei.com.android.server.IPackageManager";
    private static final boolean IS_COTA_FEATURE = SystemProperties.getBoolean("ro.config.hw_cota", false);
    private static final boolean IS_HOTA_RESTORE_THEME = SystemProperties.getBoolean("ro.config.hw_hotaRestoreTheme", false);
    private static final boolean IS_REGIONAL_PHONE_FEATURE = SystemProperties.getBoolean("ro.config.region_phone_feature", false);
    private static final boolean IS_SHOW_CUSTUI_DEFAULT = SystemProperties.getBoolean("ro.config.show_custui_default", false);
    private static final boolean IS_SHOW_DCMUI = SystemProperties.getBoolean("ro.config.hw_show_dcmui", false);
    private static final boolean IS_SUPPORT_CLONE_APP = SystemProperties.getBoolean("ro.config.hw_support_clone_app", false);
    private static final String KEY_DISPLAY_MODE = "display_mode";
    public static final String LIVEWALLPAPER_FILE = "livepaper.xml";
    private static final String MESSAGE_CLASS_POSTFIX = "_message.";
    private static final ComponentName NEWSIMPLEHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.newsimpleui.NewSimpleLauncher");
    public static final String NODE_LIVEWALLPAPER_CLASS = "classname";
    public static final String NODE_LIVEWALLPAPER_PACKAGE = "pkgname";
    private static final String NOTIFI_CLASS_POSTFIX = "_notification.";
    static final String PATH_DATASKIN_WALLPAPER = "/data/themes/";
    static final String PATH_DATA_USERS = "/data/system/users/";
    private static final String PROP_WALLPAPER = "ro.config.wallpaper";
    private static final String RINGTONE_CLASS_POSTFIX = "_ringtone.";
    private static final ComponentName SIMPLEHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.simpleui.SimpleUILauncher");
    private static final ComponentName SIMPLELAUNCHERHOME_COMPONENT = new ComponentName("com.huawei.android.simplelauncher", "com.huawei.android.simplelauncher.unihome.UniHomeLauncher");
    private static final int STYLE_DATA = 1;
    private static final String SYSTEM_APP = "android";
    private static final String SYSTEM_APP_HWEXT = "androidhwext";
    static final String TAG = "HwThemeManagerImpl";
    private static final String THEME_FONTS_BASE_PATH = "/data/skin/fonts/";
    private static final ComponentName UNIHOME_COMPONENT = new ComponentName("com.huawei.android.launcher", "com.huawei.android.launcher.unihome.UniHomeLauncher");
    static final String WALLPAPER_INFO = "/wallpaper_info.xml";
    private static ResolveInfoUtils resolveInfoUtils = ((ResolveInfoUtils) EasyInvokeFactory.getInvokeUtils(ResolveInfoUtils.class));
    private static boolean sIsDefaultThemeOk = true;
    private static final boolean sIsHwtFlipFontOn = (SystemProperties.getInt("ro.config.hwtheme", 0) != 0);
    private static final int transaction_pmGetResourcePackageName = 1004;
    private final IHwConfiguration lastHwConfig = initConfigurationEx();
    private Locale lastLocale = Locale.getDefault();
    private List<ComponentState> mDisablelaunchers = new ArrayList();
    private Map<Integer, Integer> mLauncherMap = new HashMap();
    private Object mLockForClone = new Object();
    private IPackageManager mPackageManagerService;
    private HashMap<String, String> mPackageNameMap = new HashMap();
    private CacheEntry mTempRemovedEntry;

    private static class ComponentState {
        int mSetState = 2;
        ComponentName mlauncherComponent;

        public ComponentState(ComponentName name) {
            this.mlauncherComponent = name;
        }

        public void setComponentEnable(int userId) {
            String str = HwThemeManagerImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSimpleUIConfig mlauncherComponent =");
            stringBuilder.append(this.mlauncherComponent);
            stringBuilder.append(",mSetState=");
            stringBuilder.append(this.mSetState);
            Log.d(str, stringBuilder.toString());
            try {
                AppGlobals.getPackageManager().setComponentEnabledSetting(this.mlauncherComponent, this.mSetState, 1, userId);
            } catch (Exception e) {
                String str2 = HwThemeManagerImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setComponentEnabledSetting  because e: ");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
        }
    }

    public static class LivewallpaperXmlInfo {
        public String mClassName;
        public String mPackageName;
    }

    public IHwConfiguration initConfigurationEx() {
        try {
            return (IHwConfiguration) Class.forName("android.content.res.ConfigurationEx").newInstance();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reflection exception is ");
            stringBuilder.append(e);
            Log.e("Configuration", stringBuilder.toString());
            return null;
        }
    }

    public void initForThemeFont(Configuration config) {
        ((ConfigurationEx) this.lastHwConfig).hwtheme = ((ConfigurationEx) config.extraConfig).hwtheme;
        if (config.locale != null) {
            this.lastLocale = (Locale) config.locale.clone();
        }
    }

    public HwThemeManagerImpl() {
        initLauncherComponent();
    }

    private void setThemeWallpaper(String fn, Context ctx) {
        File file = new File(fn);
        if (file.exists()) {
            WallpaperManager wpm = (WallpaperManager) ctx.getSystemService("wallpaper");
            InputStream ips = null;
            try {
                ips = new FileInputStream(file);
                wpm.setStream(ips);
                try {
                    ips.close();
                } catch (Exception e) {
                }
            } catch (FileNotFoundException e2) {
                Log.w(TAG, "pwm setwallpaper not found err:", e2);
                if (ips != null) {
                    ips.close();
                }
            } catch (IOException e3) {
                Log.w(TAG, "pwm setwallpaper io err:", e3);
                if (ips != null) {
                    ips.close();
                }
            } catch (Throwable th) {
                if (ips != null) {
                    try {
                        ips.close();
                    } catch (Exception e4) {
                    }
                }
            }
            return;
        }
        Log.w(TAG, "pwm setwallpaper stopped");
    }

    private boolean isFileExists(String filename) {
        if (filename == null) {
            return false;
        }
        return new File(filename).exists();
    }

    public void setTheme(String theme_path) {
    }

    public boolean installHwTheme(String themePath) {
        return installHwTheme(themePath, false, UserHandle.myUserId());
    }

    public boolean installHwTheme(String themePath, boolean setwallpaper) {
        return installHwTheme(themePath, setwallpaper, UserHandle.myUserId());
    }

    public boolean installHwTheme(String themePath, boolean setwallpaper, int userId) {
        return pmInstallHwTheme(1002, "pmInstallHwTheme", themePath, setwallpaper, userId);
    }

    public boolean installDefaultHwTheme(Context ctx) {
        return installDefaultHwTheme(ctx, UserHandle.myUserId());
    }

    public boolean installDefaultHwTheme(Context ctx, int userId) {
        if (isDhomeThemeAdaptBackcolor()) {
            installDefaultDhomeTheme(ctx);
        }
        String defname = getDefaultHwThemePack(ctx);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the default theme: ");
        stringBuilder.append(defname);
        Log.w(str, stringBuilder.toString());
        return installHwTheme(defname, false, userId);
    }

    public boolean makeIconCache(boolean clearall) {
        return true;
    }

    public boolean saveIconToCache(Bitmap bitmap, String fn, boolean clearold) {
        return true;
    }

    private String getDefaultHwThemePack(Context ctx) {
        String colors_themes = System.getString(ctx.getContentResolver(), "colors_themes");
        if (TextUtils.isEmpty(colors_themes)) {
            Log.w(TAG, "colors and themes is empty!");
            return System.getString(ctx.getContentResolver(), CUST_THEME_NAME);
        }
        String str;
        String[] colorsAndThemes = colors_themes.split(";");
        HashMap<String, String> mHwColorThemes = new HashMap();
        for (String str2 : colorsAndThemes) {
            String[] color_theme = str2.split(",");
            if (color_theme.length != 2 || color_theme[0].isEmpty()) {
                String str3 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid color and theme : ");
                stringBuilder.append(str2);
                Log.w(str3, stringBuilder.toString());
            } else {
                mHwColorThemes.put(color_theme[0].toLowerCase(Locale.US), color_theme[1]);
            }
        }
        if (mHwColorThemes.isEmpty()) {
            Log.w(TAG, "has no valid color-theme!");
            return System.getString(ctx.getContentResolver(), CUST_THEME_NAME);
        }
        String mColor = SystemProperties.get("ro.config.devicecolor");
        String mBackColor = SystemProperties.get("ro.config.backcolor");
        if (mColor == null) {
            return System.getString(ctx.getContentResolver(), CUST_THEME_NAME);
        }
        String hwThemePath = (String) mHwColorThemes.get(mColor.toLowerCase(Locale.US));
        if (isFileExists(hwThemePath)) {
            System.putString(ctx.getContentResolver(), CUST_THEME_NAME, hwThemePath);
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("The TP color: ");
            stringBuilder2.append(mColor);
            stringBuilder2.append(", Theme path: ");
            stringBuilder2.append(hwThemePath);
            Log.w(str2, stringBuilder2.toString());
            return hwThemePath;
        }
        if (hwThemePath == null && !TextUtils.isEmpty(mBackColor)) {
            str2 = new StringBuilder();
            str2.append(mColor);
            str2.append("+");
            str2.append(mBackColor);
            str2 = str2.toString();
            hwThemePath = (String) mHwColorThemes.get(str2.toLowerCase(Locale.US));
            if (isFileExists(hwThemePath)) {
                System.putString(ctx.getContentResolver(), CUST_THEME_NAME, hwThemePath);
                String str4 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("The group color: ");
                stringBuilder3.append(str2);
                stringBuilder3.append(", Theme path: ");
                stringBuilder3.append(hwThemePath);
                Log.w(str4, stringBuilder3.toString());
                return hwThemePath;
            }
        }
        return System.getString(ctx.getContentResolver(), CUST_THEME_NAME);
    }

    public boolean isDhomeThemeAdaptBackcolor() {
        return SystemProperties.getBoolean("ro.config.hw_dhome_theme", false);
    }

    private String getDhomeThemeName(Context ctx) {
        String DOCOMO_COLOR_THEMES = "dcm_color_themes";
        String DOCOMO_DEFAULT_THEME = "dcm_default_theme";
        String DOCOMO_DEFAULT_THEME_NAME = System.getString(ctx.getContentResolver(), "dcm_default_theme");
        String colorThemes = System.getString(ctx.getContentResolver(), "dcm_color_themes");
        String IS_DOCOMO_MULTI_THEMES = System.getString(ctx.getContentResolver(), "dcm_multi_themes");
        if (IS_DOCOMO_MULTI_THEMES != null && IS_DOCOMO_MULTI_THEMES.equals("true")) {
            colorThemes = System.getString(ctx.getContentResolver(), "dcm_color_multi_themes");
        }
        if (TextUtils.isEmpty(colorThemes)) {
            Log.w(TAG, "dcm colors and themes is empty!");
            return DOCOMO_DEFAULT_THEME_NAME;
        }
        String[] colors_themes = colorThemes.split(";");
        HashMap<String, String> dHomeColorThemes = new HashMap();
        for (String str : colors_themes) {
            String[] color_theme = str.split(",");
            if (color_theme.length != 2 || color_theme[0].isEmpty()) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid dcm color and theme : ");
                stringBuilder.append(str);
                Log.w(str2, stringBuilder.toString());
            } else {
                dHomeColorThemes.put(color_theme[0].toLowerCase(Locale.US), color_theme[1]);
            }
        }
        if (dHomeColorThemes.isEmpty()) {
            Log.w(TAG, "has no valid dcm color_theme!");
            return DOCOMO_DEFAULT_THEME_NAME;
        }
        String mBackColor = SystemProperties.get("ro.config.backcolor");
        if (mBackColor == null || mBackColor.isEmpty()) {
            Log.w(TAG, "has no backcolor property,use default docomo theme");
            return DOCOMO_DEFAULT_THEME_NAME;
        }
        String dcmThemeName = (String) dHomeColorThemes.get(mBackColor.toLowerCase(Locale.US));
        if (dcmThemeName == null || dcmThemeName.isEmpty()) {
            Log.w(TAG, "has no theme adapt to backcolor,use default docomo theme");
            return DOCOMO_DEFAULT_THEME_NAME;
        }
        System.putString(ctx.getContentResolver(), "dcm_default_theme", dcmThemeName);
        String str3 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get docomo default theme OK,dcmThemePath:");
        stringBuilder2.append(dcmThemeName);
        Log.w(str3, stringBuilder2.toString());
        return dcmThemeName;
    }

    private void linkTheme(String origin, String target) {
        CommandLineUtil.link("system", origin, target);
    }

    private void giveRWToPath(String path) {
        CommandLineUtil.chmod("system", "0775", path);
        CommandLineUtil.chown("system", "system", "media_rw", path);
    }

    private String getDhomeThemePath(Context ctx, String themePath, String def) {
        if (ctx == null || TextUtils.isEmpty(themePath)) {
            return def;
        }
        String path = System.getString(ctx.getContentResolver(), themePath);
        return TextUtils.isEmpty(path) ? def : path;
    }

    public boolean installDefaultDhomeTheme(Context ctx) {
        String DOCOMO_ALL_THEMES_DIR = new StringBuilder();
        DOCOMO_ALL_THEMES_DIR.append(Environment.getDataDirectory());
        DOCOMO_ALL_THEMES_DIR.append("/kisekae");
        DOCOMO_ALL_THEMES_DIR = DOCOMO_ALL_THEMES_DIR.toString();
        String DOCOMO_PREFABRICATE_THEMES_DIR = getDhomeThemePath(ctx, "dcm_theme_path", "/cust/docomo/jp/themes/");
        String DOCOMO_DEFAULT_THEME_PATH = new StringBuilder();
        DOCOMO_DEFAULT_THEME_PATH.append(DOCOMO_ALL_THEMES_DIR);
        DOCOMO_DEFAULT_THEME_PATH.append("/kisekae0.kin");
        DOCOMO_DEFAULT_THEME_PATH = DOCOMO_DEFAULT_THEME_PATH.toString();
        String deviceThemeName = getDhomeThemeName(ctx);
        String originThemePath = new StringBuilder();
        originThemePath.append(DOCOMO_PREFABRICATE_THEMES_DIR);
        originThemePath.append(deviceThemeName);
        originThemePath = originThemePath.toString();
        File themefolderDir = new File(DOCOMO_ALL_THEMES_DIR);
        File originThemeFile = new File(originThemePath);
        if (UserHandle.getCallingUserId() != 0) {
            return false;
        }
        String hwThemeFileName = new StringBuilder();
        hwThemeFileName.append(PATH_DATASKIN_WALLPAPER);
        hwThemeFileName.append(UserHandle.myUserId());
        hwThemeFileName = hwThemeFileName.toString();
        File hwThemePath = new File(hwThemeFileName);
        if (!(hwThemeFileName.contains("..") || hwThemePath.exists())) {
            if (!hwThemePath.mkdirs()) {
                return false;
            }
            giveRWToPath(PATH_DATASKIN_WALLPAPER);
        }
        if (themefolderDir.exists() || themefolderDir.mkdir()) {
            giveRWToPath(DOCOMO_ALL_THEMES_DIR);
            String IS_DOCOMO_MULTI_THEMES = System.getString(ctx.getContentResolver(), "dcm_multi_themes");
            String str;
            if (IS_DOCOMO_MULTI_THEMES == null || !IS_DOCOMO_MULTI_THEMES.equals("true")) {
                str = DOCOMO_PREFABRICATE_THEMES_DIR;
                if (originThemeFile.exists()) {
                    linkTheme(originThemePath, DOCOMO_DEFAULT_THEME_PATH);
                }
            } else if (TextUtils.isEmpty(deviceThemeName)) {
                str = DOCOMO_PREFABRICATE_THEMES_DIR;
            } else {
                String[] deviceThemeNames = deviceThemeName.split(":");
                File originThemeFile2 = originThemeFile;
                int i = 0;
                int linkThemeTag = 0;
                while (i < deviceThemeNames.length) {
                    String DOCOMO_ALL_THEMES_DIR2;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(DOCOMO_PREFABRICATE_THEMES_DIR);
                    str = DOCOMO_PREFABRICATE_THEMES_DIR;
                    stringBuilder.append(deviceThemeNames[i]);
                    String originMultiThemePath = stringBuilder.toString();
                    File originThemeFile3 = new File(originMultiThemePath);
                    if (originThemeFile3.exists()) {
                        String DOCOMO_MULTI_THEME_PATH = new StringBuilder();
                        DOCOMO_MULTI_THEME_PATH.append(DOCOMO_ALL_THEMES_DIR);
                        DOCOMO_ALL_THEMES_DIR2 = DOCOMO_ALL_THEMES_DIR;
                        DOCOMO_MULTI_THEME_PATH.append("/kisekae");
                        DOCOMO_ALL_THEMES_DIR = linkThemeTag + 1;
                        DOCOMO_MULTI_THEME_PATH.append(linkThemeTag);
                        DOCOMO_MULTI_THEME_PATH.append(".kin");
                        DOCOMO_MULTI_THEME_PATH = DOCOMO_MULTI_THEME_PATH.toString();
                        linkTheme(originMultiThemePath, DOCOMO_MULTI_THEME_PATH);
                        linkThemeTag = DOCOMO_ALL_THEMES_DIR;
                        String str2 = DOCOMO_MULTI_THEME_PATH;
                    } else {
                        DOCOMO_ALL_THEMES_DIR2 = DOCOMO_ALL_THEMES_DIR;
                    }
                    i++;
                    originThemeFile2 = originThemeFile3;
                    DOCOMO_PREFABRICATE_THEMES_DIR = str;
                    DOCOMO_ALL_THEMES_DIR = DOCOMO_ALL_THEMES_DIR2;
                    Context context = ctx;
                }
                str = DOCOMO_PREFABRICATE_THEMES_DIR;
                originThemeFile = originThemeFile2;
            }
            if (DOCOMO_DEFAULT_THEME_PATH == null || !isFileExists(DOCOMO_DEFAULT_THEME_PATH)) {
                DOCOMO_ALL_THEMES_DIR = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(DOCOMO_DEFAULT_THEME_PATH);
                stringBuilder2.append(" not exist");
                Log.w(DOCOMO_ALL_THEMES_DIR, stringBuilder2.toString());
                return false;
            }
            saveDhomeThemeInfo(originThemeFile, DOCOMO_DEFAULT_THEME_PATH);
            return true;
        }
        Log.w(TAG, "mkdir /data/kisekae fail !!!");
        return false;
    }

    private void saveDhomeThemeInfo(File originThemeFile, String defaultThemePath) {
        if (originThemeFile == null || TextUtils.isEmpty(defaultThemePath)) {
            Log.e(TAG, "saveDhomeThemeInfo :: origin theme file or default theme path invalid.");
            return;
        }
        Properties properties = new Properties();
        properties.put("default_theme_path", defaultThemePath);
        String themeName = originThemeFile.getName();
        String str = "current_docomo_theme_type";
        Object obj = themeName.contains("kisekae_Cute.kin") ? "silver" : themeName.contains("village.kin") ? "gold" : "unknow";
        properties.put(str, obj);
        properties.put("current_docomo_theme_name", themeName);
        str = "deleted_docomo_themes";
        obj = themeName.contains("kisekae_Cute.kin") ? "village.kin" : themeName.contains("village.kin") ? "kisekae_Cute.kin" : "unknow";
        properties.put(str, obj);
        FileOutputStream fos = null;
        String deviceThemeInfoPath = new StringBuilder();
        deviceThemeInfoPath.append(defaultThemePath.substring(0, defaultThemePath.lastIndexOf(47)));
        deviceThemeInfoPath.append("/DeviceThemeInfo.properties");
        deviceThemeInfoPath = deviceThemeInfoPath.toString();
        try {
            fos = new FileOutputStream(deviceThemeInfoPath);
            properties.store(fos, null);
            giveRWToPath(deviceThemeInfoPath);
            try {
                fos.close();
            } catch (IOException e) {
                Log.w(TAG, "FileOutputStream close failed!");
            }
        } catch (FileNotFoundException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(deviceThemeInfoPath);
            stringBuilder.append(" not found");
            Log.w(str2, stringBuilder.toString());
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e3) {
            Log.w(TAG, "DeviceThemeInfo.properties store failed!");
            if (fos != null) {
                fos.close();
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Log.w(TAG, "FileOutputStream close failed!");
                }
            }
        }
    }

    public void updateConfiguration(boolean changeuser) {
        try {
            Configuration curConfig = new Configuration();
            curConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
            curConfig.extraConfig.setConfigItem(1, curConfig.extraConfig.getConfigItem(1) + 1);
            ActivityManagerNative.getDefault().updateConfiguration(curConfig);
            if ((!HwPCUtils.enabled() || ActivityThread.currentActivityThread() == null || !HwPCUtils.isValidExtDisplayId(ActivityThread.currentActivityThread().getDisplayId())) && !changeuser) {
                updateOverlaysThems();
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateConfiguration occurs exception e.msg = ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    private void updateOverlaysThems() {
        RemoteException remoteException;
        int newUserId = UserHandle.myUserId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("persist.deep.theme_");
        stringBuilder.append(newUserId);
        String themePath = stringBuilder.toString();
        HwThemeManager.setOverLayThemePath(themePath);
        HwThemeManager.setOverLayThemeType(SystemProperties.get(themePath));
        IOverlayManager overlayManager = Stub.asInterface(ServiceManager.getService("overlay"));
        if (overlayManager != null) {
            List<OverlayInfo> frameworkhwextinfos = null;
            List<OverlayInfo> frameworkinfos = null;
            try {
                frameworkhwextinfos = overlayManager.getOverlayInfosForTarget(SYSTEM_APP_HWEXT, newUserId);
                frameworkinfos = overlayManager.getOverlayInfosForTarget("android", newUserId);
            } catch (RemoteException e) {
                Log.e(TAG, "fail get fwk overlayinfos");
            }
            int size = 0;
            if (frameworkhwextinfos != null) {
                size = frameworkhwextinfos.size();
                if (frameworkinfos != null) {
                    frameworkhwextinfos.addAll(frameworkinfos);
                    size = frameworkhwextinfos.size();
                }
            }
            int size2 = size;
            size = 0;
            while (true) {
                int i = size;
                if (i < size2) {
                    String packageName = ((OverlayInfo) frameworkhwextinfos.get(i)).packageName;
                    if (!TextUtils.isEmpty(packageName)) {
                        boolean isDarkTheme = HwThemeManager.isDeepDarkTheme();
                        boolean isHonorProduct = HwThemeManager.isHonorProduct();
                        if ((FWK_NOVA_TAG.equals(packageName) && HwThemeManager.isNovaProduct()) || (FWK_HONOR_TAG.equals(packageName) && isHonorProduct)) {
                            try {
                                overlayManager.setEnabledExclusive(packageName, true, newUserId);
                            } catch (RemoteException e2) {
                                RemoteException remoteException2 = e2;
                                String str = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("fail set ");
                                stringBuilder2.append(packageName);
                                stringBuilder2.append(" account access");
                                Log.e(str, stringBuilder2.toString());
                            }
                        } else {
                            if (!(isDarkTheme || newUserId == 0 || !packageName.contains("frameworkhwext"))) {
                                try {
                                    overlayManager.setEnabled(packageName, false, 0);
                                } catch (RemoteException e22) {
                                    remoteException = e22;
                                    Log.e(TAG, "fail set enable the primary user");
                                }
                            }
                            if (isDarkTheme || isHonorProduct || !packageName.contains("frameworkhwext")) {
                                if (isDarkTheme && packageName.contains("dark")) {
                                    try {
                                        overlayManager.setEnabledExclusive(packageName, true, newUserId);
                                        if (newUserId != 0) {
                                            overlayManager.setEnabledExclusive(packageName, true, 0);
                                        }
                                    } catch (RemoteException e3) {
                                        Log.e(TAG, "fail set dark account access");
                                    }
                                }
                                if (!isDarkTheme && isHonorProduct && packageName.contains("honor")) {
                                    try {
                                        overlayManager.setEnabledExclusive(packageName, true, newUserId);
                                        if (newUserId != 0) {
                                            try {
                                                overlayManager.setEnabledExclusive(packageName, true, 0);
                                            } catch (RemoteException e4) {
                                            }
                                        }
                                    } catch (RemoteException e5) {
                                        Log.e(TAG, "fail set honor account access");
                                        size = i + 1;
                                    }
                                    size = i + 1;
                                }
                            } else {
                                try {
                                    overlayManager.setEnabled(packageName, false, newUserId);
                                } catch (RemoteException e222) {
                                    remoteException = e222;
                                    Log.e(TAG, "fail set false account access");
                                }
                            }
                        }
                    }
                    size = i + 1;
                } else {
                    Log.i(TAG, "HwThemeManagerImpl#updateOverlaysThems end");
                    return;
                }
            }
        }
    }

    public void updateConfiguration() {
        updateConfiguration(false);
    }

    public Resources getResources(AssetManager assets, DisplayMetrics dm, Configuration config, DisplayAdjustments displayAdjustments, IBinder token) {
        return new HwResources(assets, dm, config, displayAdjustments, token);
    }

    public Resources getResources() {
        return new HwResources();
    }

    public Resources getResources(boolean system) {
        return new HwResources(system);
    }

    public Resources getResources(ClassLoader classLoader) {
        return new HwResources(classLoader);
    }

    public AbsResourcesImpl getHwResourcesImpl() {
        return new HwResourcesImpl();
    }

    public InputStream getDefaultWallpaperIS(Context context, int userId) {
        InputStream is = null;
        WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService("wallpaper");
        try {
            File googleCustWallpaperFile;
            String path = SystemProperties.get(PROP_WALLPAPER);
            if (!TextUtils.isEmpty(path)) {
                googleCustWallpaperFile = new File(path);
                if (googleCustWallpaperFile.exists()) {
                    is = new FileInputStream(googleCustWallpaperFile);
                    wallpaperManager.setStream(is);
                    return is;
                }
            }
            if (new File(CUST_WALLPAPER_DIR, CUST_WALLPAPER_FILE_NAME).exists()) {
                is = new FileInputStream(CUST_WALLPAPER);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(PATH_DATASKIN_WALLPAPER);
                stringBuilder.append(userId);
                stringBuilder.append("/wallpaper/");
                googleCustWallpaperFile = new File(stringBuilder.toString(), CURRENT_HOMEWALLPAPER_NAME);
                if (!googleCustWallpaperFile.exists()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(PATH_DATASKIN_WALLPAPER);
                    stringBuilder2.append(userId);
                    stringBuilder2.append("/wallpaper/");
                    googleCustWallpaperFile = new File(stringBuilder2.toString(), CURRENT_HOMEWALLPAPER_NAME_PNG);
                }
                if (googleCustWallpaperFile.exists()) {
                    is = new FileInputStream(googleCustWallpaperFile);
                }
            }
        } catch (IOException e) {
        }
        if (is == null) {
            is = context.getResources().openRawResource(17302116);
        }
        return is;
    }

    public void updateIconCache(PackageItemInfo packageItemInfo, String name, String packageName, int icon, int packageIcon) {
        int id = icon != 0 ? icon : packageIcon;
        String resPackageName = packageName;
        String idAndPackageName = new StringBuilder();
        idAndPackageName.append(id);
        idAndPackageName.append("#");
        idAndPackageName.append(resPackageName);
        idAndPackageName = idAndPackageName.toString();
        if (!IconCache.contains(idAndPackageName)) {
            String tmpName = name != null ? name : resPackageName;
            String lc = tmpName != null ? tmpName.toLowerCase() : null;
            if (lc != null && lc.indexOf("shortcut") < 0 && lc.indexOf(".cts") < 0) {
                CacheEntry ce = new CacheEntry();
                ce.name = tmpName;
                ce.type = icon != 0 ? 1 : 0;
                IconCache.add(idAndPackageName, ce);
            }
        }
    }

    public void updateResolveInfoIconCache(ResolveInfo resolveInfo, int icon, String resolvePackageName) {
        if (!(icon == 0 || resolveInfo == null)) {
            if (resolvePackageName != null) {
                updateIconCache(null, null, resolvePackageName, icon, 0);
                return;
            }
            PackageItemInfo ci = resolveInfoUtils.getComponentInfo(resolveInfo);
            if (ci != null) {
                updateIconCache(ci, ci.name, ci.packageName, icon, 0);
            }
        }
    }

    public void removeIconCache(String name, String packageName, int icon, int packageIcon) {
        int id = icon != 0 ? icon : packageIcon;
        String resPackageName = getResourcePackageName(1004, "getResourcesPackageName", packageName, id);
        String idAndPackageName = new StringBuilder();
        idAndPackageName.append(id);
        idAndPackageName.append("#");
        idAndPackageName.append(resPackageName);
        idAndPackageName = idAndPackageName.toString();
        if (IconCache.contains(idAndPackageName)) {
            this.mTempRemovedEntry = IconCache.get(idAndPackageName);
            IconCache.remove(idAndPackageName);
        }
    }

    public void restoreIconCache(String packageName, int icon) {
        if (this.mTempRemovedEntry != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(icon);
            stringBuilder.append("#");
            stringBuilder.append(packageName);
            IconCache.add(stringBuilder.toString(), this.mTempRemovedEntry);
            this.mTempRemovedEntry = null;
        }
    }

    public int getThemeColor(int[] data, int index, TypedValue value, Resources resources, boolean flag) {
        TypedValue mValue = value;
        if (!flag || mValue.resourceId == 0) {
            return data[index + 1];
        }
        try {
            return resources.getColor(mValue.resourceId);
        } catch (NotFoundException e) {
            return data[index + 1];
        }
    }

    public int getHwThemeLauncherIconSize(ActivityManager am, Resources resources) {
        Resources res = resources;
        int configIconSize = SystemProperties.getInt("ro.config.app_big_icon_size", -1);
        int multiResSize = SystemProperties.getInt("persist.sys.res.icon_size", -1);
        if (configIconSize > 0 && multiResSize > 0) {
            configIconSize = multiResSize;
        }
        return configIconSize == -1 ? (int) res.getDimension(34472064) : configIconSize;
    }

    public boolean isTRingtones(String path) {
        return path.indexOf(RINGTONE_CLASS_POSTFIX) > 0;
    }

    public boolean isTNotifications(String path) {
        return path.indexOf(NOTIFI_CLASS_POSTFIX) > 0 || path.indexOf(CALENDAR_CLASS_POSTFIX) > 0 || path.indexOf(EMAIL_CLASS_POSTFIX) > 0 || path.indexOf(MESSAGE_CLASS_POSTFIX) > 0;
    }

    public boolean isTAlarms(String path) {
        return path.indexOf(ALARM_CLASS_POSTFIX) > 0;
    }

    public IHwConfiguration initHwConfiguration() {
        return new ConfigurationEx();
    }

    private String getResourcePackageNameFromMap(String packageName) {
        String str;
        synchronized (this.mPackageNameMap) {
            str = (String) this.mPackageNameMap.get(packageName);
        }
        return str;
    }

    private void addResourcePackageName(String oldPkgName, String newPkgName) {
        synchronized (this.mPackageNameMap) {
            this.mPackageNameMap.put(oldPkgName, newPkgName);
        }
    }

    public int getShadowcolor(TypedArray a, int attr) {
        return a.getColor(attr, 0);
    }

    public void applyDefaultHwTheme(boolean checkState, Context mContext) {
        applyDefaultHwTheme(checkState, mContext, UserHandle.myUserId());
    }

    public void applyDefaultHwTheme(boolean checkState, Context mContext, int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HwThemeManager.HWT_PATH_THEME);
        stringBuilder.append("/");
        stringBuilder.append(userId);
        boolean skinExist = new File(stringBuilder.toString()).exists();
        boolean installFlagExist = isFileExists(HwThemeManager.HWT_PATH_SKIN_INSTALL_FLAG);
        boolean isSet = !skinExist || ((checkState && !sIsDefaultThemeOk) || installFlagExist);
        boolean isThemeInvalid = false;
        if (skinExist && !installFlagExist && checkState) {
            isThemeInvalid = isThemeInvalid(userId);
        }
        if (isSet || isThemeInvalid) {
            setIsDefaultThemeOk(installDefaultHwTheme(mContext, userId));
            isHotaRestoreThemeOrFirstBoot(mContext);
        } else if ((isSupportThemeRestore() && isCustChange(mContext) && !isThemeChange(mContext)) || isHotaRestoreThemeOrFirstBoot(mContext)) {
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(PATH_DATA_USERS);
            stringBuilder3.append(userId);
            if (new File(stringBuilder3.toString(), WALLPAPER_INFO).exists()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PATH_DATA_USERS);
                stringBuilder2.append(userId);
                stringBuilder2.append(WALLPAPER_INFO);
                CommandLineUtil.rm("system", stringBuilder2.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PATH_DATA_USERS);
            stringBuilder2.append(userId);
            if (new File(stringBuilder2.toString(), BLURRED_WALLPAPER).exists()) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(PATH_DATA_USERS);
                stringBuilder4.append(userId);
                stringBuilder4.append(BLURRED_WALLPAPER);
                CommandLineUtil.rm("system", stringBuilder4.toString());
            }
            installCustomerTheme(mContext, userId);
            HwThemeManager.updateConfiguration();
        }
    }

    private static void setIsDefaultThemeOk(boolean isOk) {
        sIsDefaultThemeOk = isOk;
    }

    public void addSimpleUIConfig(Activity activity) {
        if (activity.metaData.getBoolean("simpleuimode", false)) {
            ActivityInfo activityInfo = activity.info;
            activityInfo.configChanges |= StubController.PERMISSION_SMSLOG_WRITE;
        }
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManagerService == null) {
            this.mPackageManagerService = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManagerService;
    }

    private boolean pmCreateThemeFolder(int code, String transactName, String paramName, boolean paramValue, int userId) {
        IBinder pmsBinder = getPackageManager().asBinder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        if (pmsBinder != null) {
            try {
                data.writeInterfaceToken("huawei.com.android.server.IPackageManager");
                data.writeInt(userId);
                pmsBinder.transact(code, data, reply, 0);
                reply.readException();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
            }
        }
        reply.recycle();
        data.recycle();
        return result;
    }

    private boolean pmInstallHwTheme(int code, String transactName, String paramName, boolean paramValue, int userId) {
        IBinder pmsBinder = getPackageManager().asBinder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        boolean result = false;
        boolean result2 = false;
        if (pmsBinder != null) {
            try {
                data.writeInterfaceToken("huawei.com.android.server.IPackageManager");
                data.writeString(paramName);
                data.writeInt(paramValue);
                data.writeInt(userId);
                pmsBinder.transact(code, data, reply, 0);
                reply.readException();
                if (reply.readInt() != 0) {
                    result = true;
                }
                result2 = result;
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
            }
        }
        reply.recycle();
        data.recycle();
        return result2;
    }

    private void initLauncherComponent() {
        this.mDisablelaunchers.clear();
        this.mLauncherMap.clear();
        if (IS_SHOW_DCMUI) {
            this.mDisablelaunchers.add(new ComponentState(DOCOMOHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(UNIHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(SIMPLELAUNCHERHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(SIMPLEHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(DRAWERHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(NEWSIMPLEHOME_COMPONENT));
        } else if (IS_SHOW_CUSTUI_DEFAULT) {
            this.mDisablelaunchers.add(new ComponentState(DOCOMOHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(SIMPLEHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(DRAWERHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(UNIHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(NEWSIMPLEHOME_COMPONENT));
        } else {
            this.mDisablelaunchers.add(new ComponentState(UNIHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(SIMPLEHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(DRAWERHOME_COMPONENT));
            this.mDisablelaunchers.add(new ComponentState(NEWSIMPLEHOME_COMPONENT));
        }
        this.mLauncherMap.put(Integer.valueOf(0), Integer.valueOf(0));
        this.mLauncherMap.put(Integer.valueOf(1), Integer.valueOf(0));
        this.mLauncherMap.put(Integer.valueOf(2), Integer.valueOf(1));
        this.mLauncherMap.put(Integer.valueOf(4), Integer.valueOf(2));
        this.mLauncherMap.put(Integer.valueOf(5), Integer.valueOf(3));
    }

    private void enableLaunchers(int type, int userId) {
        int disableSize = this.mDisablelaunchers.size();
        int i = 0;
        while (i < disableSize) {
            ComponentState componentstate = (ComponentState) this.mDisablelaunchers.get(i);
            componentstate.mSetState = i == type ? 1 : 2;
            componentstate.setComponentEnable(userId);
            i++;
        }
    }

    public void retrieveSimpleUIConfig(ContentResolver cr, Configuration config, int userId) {
        int simpleuiVal = System.getIntForUser(cr, HwSettings.System.SIMPLEUI_MODE, 0, ActivityManager.getCurrentUser());
        int launcherConfig = System.getInt(cr, "hw_launcher_desktop_mode", 0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateSimpleUIConfig simpleuiVal =");
        stringBuilder.append(simpleuiVal);
        stringBuilder.append(",launcherConfig=");
        stringBuilder.append(launcherConfig);
        Log.d(str, stringBuilder.toString());
        int i = 1;
        if (simpleuiVal == 0 || (IS_REGIONAL_PHONE_FEATURE && Secure.getInt(cr, "user_setup_complete", 0) != 1)) {
            if (launcherConfig != 0) {
                i = launcherConfig;
            }
            simpleuiVal = i;
        }
        config.extraConfig.setConfigItem(2, simpleuiVal);
        try {
            enableLaunchers(((Integer) this.mLauncherMap.get(Integer.valueOf(simpleuiVal))).intValue(), userId);
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("retrieveSimpleUIConfig enableLaunchers e=");
            stringBuilder.append(e);
            Log.d(str, stringBuilder.toString());
        }
    }

    public void updateSimpleUIConfig(ContentResolver cr, Configuration config, int configChanges) {
        if ((StubController.PERMISSION_SMSLOG_WRITE & configChanges) != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSimpleUIConfig SIMPLEUI_MODE putIntForUser =");
            stringBuilder.append(config.extraConfig.getConfigItem(2));
            Log.w(str, stringBuilder.toString());
            System.putIntForUser(cr, HwSettings.System.SIMPLEUI_MODE, config.extraConfig.getConfigItem(2), ActivityManager.getCurrentUser());
        }
    }

    public Bitmap getThemeBitmap(Resources res, int id, Rect padding) {
        TypedValue outValue = new TypedValue();
        res.getValue(id, outValue, true);
        if (outValue.string == null) {
            return null;
        }
        String file = outValue.string.toString();
        if (file.endsWith(".png") || file.endsWith(".jpg")) {
            return res.getThemeBitmap(outValue, id, padding);
        }
        return null;
    }

    public Bitmap getThemeBitmap(Resources res, int id) {
        return getThemeBitmap(res, id, null);
    }

    private static boolean isNoThemeFont() {
        File fontdir = new File(THEME_FONTS_BASE_PATH, "");
        if (!fontdir.exists() || fontdir.list() == null || fontdir.list().length == 0) {
            return true;
        }
        return false;
    }

    public void setThemeFont() {
        if (sIsHwtFlipFontOn) {
            Canvas.freeCaches();
            Canvas.freeTextLayoutCaches();
        }
    }

    public boolean setThemeFontOnConfigChg(Configuration newConfig) {
        if (sIsHwtFlipFontOn) {
            boolean isHwThemeChanged = false;
            if (!(this.lastHwConfig == null || newConfig == null)) {
                if ((StubController.PERMISSION_CALLLOG_WRITE & this.lastHwConfig.updateFrom(newConfig.extraConfig)) != 0) {
                    isHwThemeChanged = true;
                }
            }
            boolean isLocaleChanged = false;
            if (!(newConfig == null || newConfig.locale == null || (this.lastLocale != null && this.lastLocale.equals(newConfig.locale)))) {
                this.lastLocale = newConfig.locale != null ? (Locale) newConfig.locale.clone() : null;
                isLocaleChanged = true;
            }
            if (isHwThemeChanged || isLocaleChanged) {
                setThemeFont();
                return true;
            }
        }
        return false;
    }

    public boolean isTargetFamily(String familyName) {
        return familyName != null && familyName.equals("chnfzxh");
    }

    public boolean shouldUseAdditionalChnFont(String familyName) {
        if (isTargetFamily(familyName)) {
            String curLang = Locale.getDefault().getLanguage();
            if (isNoThemeFont() && (curLang.contains("zh") || curLang.contains("en"))) {
                return true;
            }
        }
        return false;
    }

    public Bitmap generateBitmap(Context context, Bitmap bm, int width, int height) {
        OutOfMemoryError e;
        Bitmap bitmap = bm;
        int i = width;
        int i2 = height;
        if (bitmap == null) {
            return null;
        }
        WindowManager wm = (WindowManager) context.getSystemService(FreezeScreenScene.WINDOW_PARAM);
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics);
        bitmap.setDensity(metrics.noncompatDensityDpi);
        Point size = new Point();
        display.getRealSize(size);
        int max = size.x > size.y ? size.x : size.y;
        int min = size.x < size.y ? size.x : size.y;
        WindowManager windowManager;
        if (i <= 0 || i2 <= 0) {
        } else if ((bm.getWidth() == i && bm.getHeight() == i2) || ((bm.getWidth() == min && bm.getHeight() == max) || ((bm.getWidth() == max && bm.getHeight() == max) || (bm.getWidth() == max && bm.getHeight() == min)))) {
            windowManager = wm;
        } else {
            try {
                Bitmap newbm = Bitmap.createBitmap(i, i2, Config.ARGB_8888);
                if (newbm == null) {
                    try {
                        Log.w(TAG, "Can't generate default bitmap, newbm = null");
                        return bitmap;
                    } catch (OutOfMemoryError e2) {
                        e = e2;
                        windowManager = wm;
                        Log.w(TAG, "Can't generate default bitmap", e);
                        return bitmap;
                    }
                }
                newbm.setDensity(metrics.noncompatDensityDpi);
                Canvas c = new Canvas(newbm);
                Rect targetRect = new Rect();
                targetRect.right = bm.getWidth();
                targetRect.bottom = bm.getHeight();
                int deltaw = i - targetRect.right;
                int deltah = i2 - targetRect.bottom;
                if (deltaw > 0 || deltah > 0) {
                    try {
                        float tempWidth = ((float) i) / ((float) targetRect.right);
                        float tempHeight = ((float) i2) / ((float) targetRect.bottom);
                        float scale = tempWidth > tempHeight ? tempWidth : tempHeight;
                        targetRect.right = (int) (((float) targetRect.right) * scale);
                        targetRect.bottom = (int) (((float) targetRect.bottom) * scale);
                        deltaw = i - targetRect.right;
                        deltah = i2 - targetRect.bottom;
                    } catch (OutOfMemoryError e3) {
                        e = e3;
                        Log.w(TAG, "Can't generate default bitmap", e);
                        return bitmap;
                    }
                }
                windowManager = wm;
                targetRect.offset(deltaw / 2, deltah / 2);
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
                c.drawBitmap(bitmap, null, targetRect, paint);
                bm.recycle();
                return newbm;
            } catch (OutOfMemoryError e4) {
                e = e4;
                windowManager = wm;
                Log.w(TAG, "Can't generate default bitmap", e);
                return bitmap;
            }
        }
        return bitmap;
    }

    private String getResourcePackageName(int code, String transactName, String packageName, int icon) {
        String name = getResourcePackageNameFromMap(packageName);
        if (name != null) {
            return name;
        }
        IBinder resBinder = getPackageManager().asBinder();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        String result = null;
        if (resBinder != null) {
            try {
                data.writeInterfaceToken("huawei.com.android.server.IPackageManager");
                data.writeString(packageName);
                data.writeInt(icon);
                data.writeInt(UserHandle.myUserId());
                resBinder.transact(code, data, reply, 0);
                reply.readException();
                result = reply.readString();
            } catch (RemoteException localRemoteException) {
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
            }
        }
        reply.recycle();
        data.recycle();
        if (!(result == null || result.equals(""))) {
            addResourcePackageName(packageName, result);
        }
        return result;
    }

    public Drawable getJoinBitmap(Context context, Drawable srcDraw, int backgroundId) {
        context.enforceCallingOrSelfPermission(HWTHEME_GET_NOTIFICATION_INFO, "getJoinBitmap");
        Resources r = context.getResources();
        if (r != null) {
            return r.getImpl().getHwResourcesImpl().getJoinBitmap(srcDraw, backgroundId);
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:37:0x015b, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Drawable getClonedDrawable(Context context, Drawable drawable) {
        Throwable th;
        Float f;
        int i;
        int i2;
        int i3;
        Context context2 = context;
        Drawable drawable2 = drawable;
        if (!IS_SUPPORT_CLONE_APP) {
            return drawable2;
        }
        Drawable clone = context.getResources().getDrawable(33751231);
        if (clone == null) {
            return drawable2;
        }
        if (drawable2 == null) {
            clone.setColorFilter(context2.getColor(33882311), Mode.SRC_IN);
            clone.setBounds(0, 0, clone.getIntrinsicWidth(), clone.getIntrinsicHeight());
            return clone;
        }
        int cloneWidth;
        int cloneHeight;
        Float markBgRatio = Float.valueOf(Float.parseFloat(context.getResources().getString(34472193)));
        Float markBgPercentage = Float.valueOf(Float.parseFloat(context.getResources().getString(34472194)));
        int srcWidth = drawable.getIntrinsicWidth();
        int srcHeight = drawable.getIntrinsicHeight();
        int tempSrcWidth = (int) (((float) srcWidth) * markBgPercentage.floatValue());
        int tempSrcHeight = (int) (((float) srcHeight) * markBgPercentage.floatValue());
        int markBgRadius = tempSrcWidth > tempSrcHeight ? tempSrcHeight : tempSrcWidth;
        if (clone.getIntrinsicWidth() >= clone.getIntrinsicHeight()) {
            cloneWidth = (int) (((float) markBgRadius) / markBgRatio.floatValue());
            cloneHeight = (int) (((1.0d * ((double) cloneWidth)) * ((double) clone.getIntrinsicHeight())) / ((double) clone.getIntrinsicWidth()));
        } else {
            cloneHeight = (int) (((float) markBgRadius) / markBgRatio.floatValue());
            cloneWidth = (int) (((1.0d * ((double) cloneHeight)) * ((double) clone.getIntrinsicWidth())) / ((double) clone.getIntrinsicHeight()));
        }
        synchronized (this.mLockForClone) {
            try {
                Bitmap newBitMap = Bitmap.createBitmap(srcWidth, srcWidth, Config.ARGB_8888);
                Canvas canvas = new Canvas(newBitMap);
                try {
                    drawable2.setBounds(0, 0, srcWidth, srcWidth);
                    drawable2.draw(canvas);
                    markBgRatio = new Paint();
                } catch (Throwable th2) {
                    th = th2;
                    f = markBgPercentage;
                    i = srcWidth;
                    i2 = srcHeight;
                    i3 = tempSrcWidth;
                    throw th;
                }
                try {
                } catch (Throwable th3) {
                    th = th3;
                    i = srcWidth;
                    i2 = srcHeight;
                    i3 = tempSrcWidth;
                    throw th;
                }
                try {
                    markBgRatio.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
                    markBgRatio.setAntiAlias(true);
                    canvas.save();
                    markBgRatio.setColor(context2.getColor(33882311));
                    canvas.translate((float) (srcWidth - markBgRadius), (float) (srcHeight - markBgRadius));
                    canvas.drawCircle(((float) markBgRadius) / 2.0f, ((float) markBgRadius) / 2.0f, ((float) markBgRadius) / 2.0f, markBgRatio);
                    canvas.translate(((float) (markBgRadius - cloneWidth)) / 2.0f, ((float) (markBgRadius - cloneHeight)) / 2.0f);
                    canvas.scale((((float) cloneWidth) * 1.0f) / ((float) clone.getIntrinsicWidth()), (((float) cloneHeight) * 1.0f) / ((float) clone.getIntrinsicHeight()));
                    clone.setBounds(0, 0, clone.getIntrinsicWidth(), clone.getIntrinsicHeight());
                    clone.draw(canvas);
                    canvas.restore();
                    markBgRatio.setXfermode(null);
                    markBgPercentage = new BitmapDrawable(context.getResources(), newBitMap);
                    if (drawable2 instanceof BitmapDrawable) {
                        markBgPercentage.setTargetDensity(((BitmapDrawable) drawable2).getBitmap().getDensity());
                    }
                } catch (Throwable th4) {
                    th = th4;
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                Float f2 = markBgRatio;
                f = markBgPercentage;
                i = srcWidth;
                i2 = srcHeight;
                i3 = tempSrcWidth;
                throw th;
            }
        }
    }

    public Drawable getHwBadgeDrawable(Notification notification, Context context, Drawable drawable) {
        if (!notification.extras.getBoolean("com.huawei.isIntentProtectedApp")) {
            return null;
        }
        Drawable trustSpace = context.getResources().getDrawable(33751237);
        trustSpace.setColorFilter(context.getColor(33882311), Mode.SRC_IN);
        trustSpace.setBounds(0, 0, trustSpace.getIntrinsicWidth(), trustSpace.getIntrinsicHeight());
        return trustSpace;
    }

    public String getDefaultLiveWallpaper(int userId) {
        String str;
        StringBuilder stringBuilder;
        Document document = null;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        String livepaperpath = new StringBuilder();
        livepaperpath.append("/data/themes//");
        livepaperpath.append(userId);
        livepaperpath = livepaperpath.toString();
        try {
            document = builderFactory.newDocumentBuilder().parse(new File(livepaperpath, LIVEWALLPAPER_FILE));
        } catch (ParserConfigurationException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" ParserConfigurationException ");
            stringBuilder.append(livepaperpath);
            Log.e(str, stringBuilder.toString());
        } catch (SAXException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SAXException ");
            stringBuilder.append(livepaperpath);
            Log.e(str, stringBuilder.toString());
        } catch (IOException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IOException ");
            stringBuilder.append(livepaperpath);
            Log.e(str, stringBuilder.toString());
        } catch (Exception e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception ");
            stringBuilder.append(livepaperpath);
            Log.e(str, stringBuilder.toString());
        }
        if (document == null) {
            return null;
        }
        Element rootElement = document.getDocumentElement();
        if (rootElement == null) {
            return null;
        }
        LivewallpaperXmlInfo livewallpaperInfo = new LivewallpaperXmlInfo();
        NodeList itemNodes = rootElement.getChildNodes();
        int length = itemNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node itemNode = itemNodes.item(i);
            if (itemNode.getNodeType() == (short) 1) {
                if (NODE_LIVEWALLPAPER_PACKAGE.equals(itemNode.getNodeName())) {
                    livewallpaperInfo.mPackageName = itemNode.getTextContent();
                } else if (NODE_LIVEWALLPAPER_CLASS.equals(itemNode.getNodeName())) {
                    livewallpaperInfo.mClassName = itemNode.getTextContent();
                }
            }
        }
        StringBuffer livewallpaperStr = new StringBuffer();
        livewallpaperStr.append(livewallpaperInfo.mPackageName);
        livewallpaperStr.append("/");
        livewallpaperStr.append(livewallpaperInfo.mClassName);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("HwThemeManager#getDefaultLiveWallpaper livewallpaperStr =");
        stringBuilder2.append(livewallpaperStr.toString());
        Log.e(str2, stringBuilder2.toString());
        return livewallpaperStr.toString();
    }

    public boolean installCustomerTheme(Context ctx, int userId) {
        String defname = getDefaultHwThemePack(ctx);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the new default theme: ");
        stringBuilder.append(defname);
        Log.w(str, stringBuilder.toString());
        if (isFileExists(defname)) {
            return installHwTheme(defname, true, userId);
        }
        return false;
    }

    public static boolean isSupportThemeRestore() {
        return IS_REGIONAL_PHONE_FEATURE || IS_COTA_FEATURE;
    }

    public boolean isCustChange(Context context) {
        try {
            String originalVendorCountry;
            String currentVendorCountry;
            if (IS_REGIONAL_PHONE_FEATURE) {
                originalVendorCountry = Secure.getString(context.getContentResolver(), "vendor_country");
                currentVendorCountry = SystemProperties.get("ro.hw.custPath", "");
                if (originalVendorCountry == null) {
                    Secure.putString(context.getContentResolver(), "vendor_country", currentVendorCountry);
                    return false;
                } else if (!originalVendorCountry.equals(currentVendorCountry)) {
                    Secure.putString(context.getContentResolver(), "vendor_country", currentVendorCountry);
                    return true;
                }
            } else if (IS_COTA_FEATURE) {
                originalVendorCountry = Secure.getString(context.getContentResolver(), "cotaVersion");
                currentVendorCountry = SystemProperties.get("ro.product.CotaVersion", "");
                if (originalVendorCountry == null) {
                    Secure.putString(context.getContentResolver(), "cotaVersion", currentVendorCountry);
                    return false;
                } else if (!currentVendorCountry.equals(originalVendorCountry)) {
                    Secure.putString(context.getContentResolver(), "cotaVersion", currentVendorCountry);
                    return true;
                }
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check cust Exception e : ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return false;
    }

    public static boolean isThemeChange(Context context) {
        return "true".equals(Secure.getString(context.getContentResolver(), "isUserChangeTheme"));
    }

    private boolean isThemeInvalid(int userId) {
        long startTime = SystemClock.elapsedRealtime();
        String themePath = new StringBuilder();
        themePath.append(HwThemeManager.HWT_PATH_THEME);
        themePath.append("/");
        themePath.append(userId);
        themePath = themePath.toString();
        String defaultWallpaperFileName = new StringBuilder();
        defaultWallpaperFileName.append(themePath);
        defaultWallpaperFileName.append("/wallpaper/home_wallpaper_0.jpg");
        if (isFileInvalid(defaultWallpaperFileName.toString())) {
            Log.d(TAG, "Theme is not valid");
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("check theme took ");
        stringBuilder.append(SystemClock.elapsedRealtime() - startTime);
        Log.d(str, stringBuilder.toString());
        return false;
    }

    /* JADX WARNING: Missing block: B:19:0x0037, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            android.util.Log.w(TAG, "isFileInvalid IOException ");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isFileInvalid(String fileName) {
        if (fileName != null && isFileExists(fileName)) {
            boolean isNotValid = false;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fileName);
                byte[] buffer = new byte[1024];
                while (true) {
                    int read = fis.read(buffer);
                    int realLength = read;
                    if (read == -1) {
                        break;
                    }
                    for (read = 0; read < realLength; read++) {
                        if (buffer[read] != (byte) 0) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                Log.w(TAG, "isFileInvalid IOException ");
                            }
                            return false;
                        }
                    }
                }
            } catch (RuntimeException e2) {
                Log.w(TAG, "isFileInvalid RuntimeException ");
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e3) {
                Log.w(TAG, "isFileInvalid Exception ");
                if (fis != null) {
                    fis.close();
                }
            } catch (Throwable th) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                        Log.w(TAG, "isFileInvalid IOException ");
                    }
                }
            }
            if (isNotValid) {
                return true;
            }
        }
        return false;
    }

    private boolean isHotaRestoreThemeOrFirstBoot(Context context) {
        if (IS_HOTA_RESTORE_THEME) {
            String buildVersion;
            try {
                String originalVersion = Secure.getString(context.getContentResolver(), "custVersion");
                buildVersion = SystemProperties.get("ro.build.display.id", "");
                String custCVersion = SystemProperties.get("ro.product.CustCVersion", "");
                String custDVersion = SystemProperties.get("ro.product.CustDVersion", "");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(buildVersion);
                stringBuilder.append(custCVersion);
                stringBuilder.append(custDVersion);
                if (!stringBuilder.toString().equals(originalVersion)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(buildVersion);
                    stringBuilder2.append(custCVersion);
                    stringBuilder2.append(custDVersion);
                    Secure.putString(context.getContentResolver(), "custVersion", stringBuilder2.toString());
                    return true;
                }
            } catch (Exception e) {
                buildVersion = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("check cust Exception e : ");
                stringBuilder3.append(e);
                Log.e(buildVersion, stringBuilder3.toString());
            }
        }
        return false;
    }
}

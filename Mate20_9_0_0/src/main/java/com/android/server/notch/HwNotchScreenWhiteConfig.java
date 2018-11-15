package com.android.server.notch;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.WindowManager.LayoutParams;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

public class HwNotchScreenWhiteConfig {
    private static final boolean CHINA_AREA = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static final String CURRENT_ROM_VERSION = SystemProperties.get("ro.build.version.incremental", "B001");
    public static final String DISPLAY_NOTCH_STATUS = "display_notch_status";
    public static final int NOTCH_MODE_ALWAYS = 1;
    public static final int NOTCH_MODE_NEVER = 2;
    private static final int NOTCH_SCREEN_TYPE = 0;
    private static final String NOTCH_SCREEN_WHITE_LIST = "notch_screen_whitelist.xml";
    private static final String TAG = "HwNotchScreenWhiteConfig";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_NONE_NOTCH_NAME = "none_notch_app";
    private static final String XML_NONE_NOTCH_NAME_HIDE = "none_notch_app_hide";
    private static final String XML_NONE_NOTCH_NAME_WITH_STATUSBAR = "none_notch_app_with_statusbar";
    private static final String XML_NOTCH_APP_NAME = "notch_app";
    private static final String XML_NOTCH_APP_NAME_HIDE = "notch_app_hide";
    private static final String XML_NOTCH_SYSTEM_APP = "system_app";
    private static final String XML_Notch_WhITE_LIST = "notchscreen_whitelist";
    private static HwNotchScreenWhiteConfig hwNotchScreenWhiteConfig;
    private static final Integer[] typeSupport = new Integer[]{Integer.valueOf(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS), Integer.valueOf(HwArbitrationDEFS.MSG_MPLINK_UNBIND_FAIL), Integer.valueOf(HwArbitrationDEFS.MSG_MPLINK_AI_DEVICE_COEX_MODE)};
    private Map<String, Integer> mAppUseNotchModeMap;
    private Map<String, Boolean> mInstalledAppVersionCode;
    private boolean mIsNotchSwitchOpen;
    private Map<String, Integer> mWhiteAppVersionCode;
    private List<String> noneNotchAppInfos;
    private List<String> noneNotchAppWithStatusbarInfos;
    private List<String> noneNotchHideAppInfos;
    private List<String> notchAppHideInfos;
    private List<String> notchAppInfos;
    private List<String> systemAppInfos;

    public static abstract class NotchSwitchListener {
        final ContentObserver mObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange, Uri uri) {
                NotchSwitchListener.this.onChange();
            }
        };

        public abstract void onChange();
    }

    private static class WhitelistUpdateThread extends Thread {
        Context mContext = null;
        String mFileName = null;

        protected WhitelistUpdateThread(Context context, String fileName) {
            super("config update thread");
            this.mContext = context;
            this.mFileName = fileName;
        }

        public void run() {
            if (this.mFileName != null) {
                FileInputStream inputStream = null;
                ParcelFileDescriptor openFileDescriptor = null;
                try {
                    if (HwNotchScreenWhiteConfig.CHINA_AREA) {
                        openFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(Uri.parse(this.mFileName), "data/system");
                        FileDescriptor fileDescriptor = openFileDescriptor.getFileDescriptor();
                        if (fileDescriptor != null) {
                            inputStream = new FileInputStream(fileDescriptor);
                        }
                    } else {
                        inputStream = new FileInputStream(new File(this.mFileName));
                    }
                    File targetFileTemp = createFileForWrite();
                    if (!(targetFileTemp == null || inputStream == null)) {
                        parseConfigsToTargetFile(targetFileTemp, inputStream);
                    }
                } catch (FileNotFoundException e) {
                    String str = HwNotchScreenWhiteConfig.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("FileNotFoundException:");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    closeParcelFileDescriptor(null);
                    closeInputStream(null);
                }
                closeParcelFileDescriptor(openFileDescriptor);
                closeInputStream(inputStream);
            }
        }

        public static File createFileForWrite() {
            File dataSystemDirectory = Environment.getDataSystemDirectory();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notch_screen_whitelist_");
            stringBuilder.append(HwNotchScreenWhiteConfig.CURRENT_ROM_VERSION);
            stringBuilder.append(".xml");
            File file = new File(dataSystemDirectory, stringBuilder.toString());
            if (!file.exists() || file.delete()) {
                try {
                    if (file.createNewFile()) {
                        file.setReadable(true, false);
                        return file;
                    }
                    Log.e(HwNotchScreenWhiteConfig.TAG, "createFileForWrite createNewFile error!");
                    return null;
                } catch (IOException ioException) {
                    String str = HwNotchScreenWhiteConfig.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ioException: ");
                    stringBuilder2.append(ioException);
                    Log.e(str, stringBuilder2.toString());
                    return null;
                }
            }
            Log.e(HwNotchScreenWhiteConfig.TAG, "delete file error!");
            return null;
        }

        private void parseConfigsToTargetFile(File targetFile, FileInputStream inputStream) {
            String str;
            StringBuilder stringBuilder;
            BufferedReader reader = null;
            FileOutputStream outputStream = null;
            InputStreamReader inputStreamReader = null;
            StringBuilder targetStringBuilder = new StringBuilder();
            boolean recordStarted = true;
            try {
                inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                reader = new BufferedReader(inputStreamReader);
                while (true) {
                    String readLine = reader.readLine();
                    String tempLineString = readLine;
                    if (readLine == null) {
                        break;
                    }
                    tempLineString = tempLineString.trim();
                    if (recordStarted) {
                        targetStringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                        recordStarted = false;
                    } else {
                        targetStringBuilder.append("\n");
                        targetStringBuilder.append(tempLineString);
                    }
                }
                outputStream = new FileOutputStream(targetFile);
                byte[] outputString = targetStringBuilder.toString().getBytes("utf-8");
                outputStream.write(outputString, 0, outputString.length);
            } catch (IOException e) {
                deleteAbnormalXml(targetFile);
                str = HwNotchScreenWhiteConfig.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parseConfigsToTargetFile IOException :");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            } catch (RuntimeException e2) {
                deleteAbnormalXml(targetFile);
                str = HwNotchScreenWhiteConfig.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("parseConfigsToTargetFile RuntimeException :");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                closeBufferedReader(null);
                closeInputStreamReader(null);
                closeFileOutputStream(null);
                HwNotchScreenWhiteConfig.getInstance().updateWhiteListData();
            }
            closeBufferedReader(reader);
            closeInputStreamReader(inputStreamReader);
            closeFileOutputStream(outputStream);
            HwNotchScreenWhiteConfig.getInstance().updateWhiteListData();
        }

        private void deleteAbnormalXml(File file) {
            try {
                if (file.exists() && !file.delete()) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "delete abnormal xml error!");
                }
            } catch (Exception e) {
                Log.e(HwNotchScreenWhiteConfig.TAG, "Delete the abnormal xml Fail!!!");
            }
        }

        private void closeBufferedReader(BufferedReader bufferedReader) {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "closeBufferedReader error!");
                }
            }
        }

        private void closeInputStreamReader(InputStreamReader inputStreamReader) {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "closeInputStreamReader error!");
                }
            }
        }

        private void closeInputStream(InputStream inputStream) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "closeInputStream error!");
                }
            }
        }

        private void closeFileOutputStream(FileOutputStream fileOutputStream) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "closeFileOutputStream error!");
                }
            }
        }

        private void closeParcelFileDescriptor(ParcelFileDescriptor openFileDescriptor) {
            if (openFileDescriptor != null) {
                try {
                    openFileDescriptor.close();
                } catch (IOException e) {
                    Log.e(HwNotchScreenWhiteConfig.TAG, "openFileDescriptor error!");
                }
            }
        }
    }

    public static HwNotchScreenWhiteConfig getInstance() {
        if (hwNotchScreenWhiteConfig == null) {
            hwNotchScreenWhiteConfig = new HwNotchScreenWhiteConfig();
        }
        return hwNotchScreenWhiteConfig;
    }

    private HwNotchScreenWhiteConfig() {
        initData();
    }

    public boolean notchSupportWindow(LayoutParams attrs) {
        return Arrays.asList(typeSupport).contains(Integer.valueOf(attrs.type));
    }

    public void updateWhiteListData() {
        initData();
    }

    private void initData() {
        this.notchAppInfos = new ArrayList();
        this.noneNotchAppInfos = new ArrayList();
        this.notchAppHideInfos = new ArrayList();
        this.noneNotchHideAppInfos = new ArrayList();
        this.noneNotchAppWithStatusbarInfos = new ArrayList();
        this.systemAppInfos = new ArrayList();
        this.mWhiteAppVersionCode = new HashMap();
        this.mInstalledAppVersionCode = new HashMap();
        this.mAppUseNotchModeMap = new HashMap();
        loadNotchScreenWhiteList();
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0084 A:{Catch:{ FileNotFoundException -> 0x0078, XmlPullParserException -> 0x0075, IOException -> 0x0072, all -> 0x006f }} */
    /* JADX WARNING: Removed duplicated region for block: B:84:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x014f A:{SYNTHETIC, Splitter: B:57:0x014f} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadNotchScreenWhiteList() {
        String str;
        InputStream inputStream = null;
        File notchScreenFile = null;
        try {
            File dataSystemDirectory = Environment.getDataSystemDirectory();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notch_screen_whitelist_");
            stringBuilder.append(CURRENT_ROM_VERSION);
            stringBuilder.append(".xml");
            File newFile = new File(dataSystemDirectory, stringBuilder.toString());
            if (newFile.exists()) {
                notchScreenFile = newFile;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("update new file:");
                stringBuilder.append(newFile);
                Log.i(str, stringBuilder.toString());
            } else {
                notchScreenFile = HwCfgFilePolicy.getCfgFile("xml/notch_screen_whitelist.xml", 0);
            }
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        } catch (Exception e2) {
            Log.d(TAG, "HwCfgFilePolicy get notch_screen_whitelist exception");
        }
        if (notchScreenFile != null) {
            try {
                if (notchScreenFile.exists()) {
                    inputStream = new FileInputStream(notchScreenFile);
                    if (inputStream != null) {
                        XmlPullParser xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            if (xmlEventType == 2 && XML_NOTCH_APP_NAME.equals(xmlParser.getName())) {
                                str = xmlParser.getAttributeValue(null, "name");
                                addNotchAppInfo(str);
                                getVersionCode(xmlParser, str);
                            } else if (xmlEventType == 2 && XML_NOTCH_APP_NAME_HIDE.equals(xmlParser.getName())) {
                                str = xmlParser.getAttributeValue(null, "name");
                                addNotchAppHideInfo(str);
                                getVersionCode(xmlParser, str);
                            } else if (xmlEventType == 2 && XML_NONE_NOTCH_NAME.equals(xmlParser.getName())) {
                                str = xmlParser.getAttributeValue(null, "name");
                                addNoneNotchAppInfo(str);
                                getVersionCode(xmlParser, str);
                            } else if (xmlEventType == 2 && XML_NONE_NOTCH_NAME_WITH_STATUSBAR.equals(xmlParser.getName())) {
                                str = xmlParser.getAttributeValue(null, "name");
                                addNoneNotchAppWithStatusbarInfo(str);
                                getVersionCode(xmlParser, str);
                            } else if (xmlEventType == 2 && XML_NONE_NOTCH_NAME_HIDE.equals(xmlParser.getName())) {
                                str = xmlParser.getAttributeValue(null, "name");
                                addNoneNotchAppHideInfo(str);
                                getVersionCode(xmlParser, str);
                            } else if (xmlEventType != 2 || !XML_NOTCH_SYSTEM_APP.equals(xmlParser.getName())) {
                                if (xmlEventType == 3 && XML_Notch_WhITE_LIST.equals(xmlParser.getName())) {
                                    break;
                                }
                            } else {
                                addSystemAppInfo(xmlParser.getAttributeValue(null, "name"));
                            }
                        }
                    }
                    if (inputStream == null) {
                        try {
                            inputStream.close();
                            return;
                        } catch (FileNotFoundException e3) {
                            Log.e(TAG, "load notch screen config: IO Exception while closing stream", e3);
                            return;
                        }
                    }
                    return;
                }
            } catch (FileNotFoundException e32) {
                Log.e(TAG, "load notch screen config: ", e32);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (FileNotFoundException e322) {
                Log.e(TAG, "load notch screen config: ", e322);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (FileNotFoundException e3222) {
                Log.e(TAG, "load notch screen config: ", e3222);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "load notch screen config: IO Exception while closing stream", e4);
                    }
                }
            }
        }
        Slog.w(TAG, "notch_screen_whitelist.xml is not exist");
        if (inputStream != null) {
        }
        if (inputStream == null) {
        }
    }

    private void getVersionCode(XmlPullParser xmlParser, String className) {
        String code = xmlParser.getAttributeValue(null, "versionCode");
        if (!TextUtils.isEmpty(code) && !TextUtils.isEmpty(className)) {
            int versionCode = 0;
            String packageName = className.split("/")[0].trim();
            try {
                versionCode = Integer.parseInt(code);
            } catch (NumberFormatException e) {
                Log.e(TAG, "NumberFormatException: ", e);
            }
            if (!this.mWhiteAppVersionCode.containsKey(packageName)) {
                this.mWhiteAppVersionCode.put(packageName, Integer.valueOf(versionCode));
            }
            if (versionCode != 0 && getVersionCodeForPackage(packageName) >= versionCode) {
                this.mInstalledAppVersionCode.put(packageName, Boolean.valueOf(true));
            }
        }
    }

    private int getVersionCodeForPackage(String packageName) {
        PackageInfo pkgInfo = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageInfo(packageName, 786432, Binder.getCallingUid(), ActivityManager.getCurrentUser());
        if (pkgInfo != null) {
            return pkgInfo.versionCode;
        }
        return 0;
    }

    private boolean isAdaptationNotchScreen(String packageName) {
        if (packageName != null && this.mInstalledAppVersionCode.containsKey(packageName)) {
            return ((Boolean) this.mInstalledAppVersionCode.get(packageName)).booleanValue();
        }
        return false;
    }

    public void updateVersionCodeInNoch(String packageName, String flag, int updateVersionCode) {
        if (packageName != null && flag != null && this.mWhiteAppVersionCode.containsKey(packageName)) {
            if ("add".equals(flag)) {
                int whiteAppVersionCode = ((Integer) this.mWhiteAppVersionCode.get(packageName)).intValue();
                if (whiteAppVersionCode != 0 && updateVersionCode >= whiteAppVersionCode) {
                    this.mInstalledAppVersionCode.put(packageName, Boolean.valueOf(true));
                }
            } else if ("removed".equals(flag) && this.mInstalledAppVersionCode.containsKey(packageName)) {
                this.mInstalledAppVersionCode.remove(packageName);
            }
        }
    }

    public boolean isNotchAppInfo(WindowState win) {
        String appInfo = win.toString();
        int size = this.notchAppInfos.size();
        int i = 0;
        while (i < size) {
            if (appInfo != null && appInfo.contains((CharSequence) this.notchAppInfos.get(i))) {
                return isAdaptationNotchScreen(win.getOwningPackage()) ^ 1;
            }
            i++;
        }
        return false;
    }

    public boolean isNoneNotchAppInfo(WindowState win) {
        String appInfo = win.toString();
        int size = this.noneNotchAppInfos.size();
        int i = 0;
        while (i < size) {
            if (appInfo != null && appInfo.contains((CharSequence) this.noneNotchAppInfos.get(i))) {
                return isAdaptationNotchScreen(win.getOwningPackage()) ^ 1;
            }
            i++;
        }
        return false;
    }

    public boolean isNotchAppHideInfo(WindowState win) {
        String appInfo = win.toString();
        int size = this.notchAppHideInfos.size();
        int i = 0;
        while (i < size) {
            if (appInfo != null && appInfo.contains((CharSequence) this.notchAppHideInfos.get(i))) {
                return isAdaptationNotchScreen(win.getOwningPackage()) ^ 1;
            }
            i++;
        }
        return false;
    }

    public boolean isNoneNotchAppHideInfo(WindowState win) {
        String appInfo = win.toString();
        int size = this.noneNotchHideAppInfos.size();
        int i = 0;
        while (i < size) {
            if (appInfo != null && appInfo.contains((CharSequence) this.noneNotchHideAppInfos.get(i))) {
                return isAdaptationNotchScreen(win.getOwningPackage()) ^ 1;
            }
            i++;
        }
        return false;
    }

    public boolean isNoneNotchAppWithStatusbarInfo(WindowState win) {
        String appInfo = win.toString();
        int size = this.noneNotchAppWithStatusbarInfos.size();
        int i = 0;
        while (i < size) {
            if (appInfo != null && appInfo.contains((CharSequence) this.noneNotchAppWithStatusbarInfos.get(i))) {
                return isAdaptationNotchScreen(win.getOwningPackage()) ^ 1;
            }
            i++;
        }
        return false;
    }

    public boolean isSystemAppInfo(String systemInfo) {
        int size = this.systemAppInfos.size();
        int i = 0;
        while (i < size) {
            if (systemInfo != null && systemInfo.contains((CharSequence) this.systemAppInfos.get(i))) {
                return true;
            }
            i++;
        }
        return false;
    }

    private void addNotchAppInfo(String notchApp) {
        if (!this.notchAppInfos.contains(notchApp)) {
            this.notchAppInfos.add(notchApp);
        }
    }

    private void addNoneNotchAppInfo(String noneNotchApp) {
        if (!this.noneNotchAppInfos.contains(noneNotchApp)) {
            this.noneNotchAppInfos.add(noneNotchApp);
        }
    }

    private void addNotchAppHideInfo(String notchAppHide) {
        if (!this.notchAppHideInfos.contains(notchAppHide)) {
            this.notchAppHideInfos.add(notchAppHide);
        }
    }

    private void addNoneNotchAppHideInfo(String noneNotchAppHide) {
        if (!this.noneNotchHideAppInfos.contains(noneNotchAppHide)) {
            this.noneNotchHideAppInfos.add(noneNotchAppHide);
        }
    }

    private void addNoneNotchAppWithStatusbarInfo(String noneNotchAppWithStatusbar) {
        if (!this.noneNotchAppWithStatusbarInfos.contains(noneNotchAppWithStatusbar)) {
            this.noneNotchAppWithStatusbarInfos.add(noneNotchAppWithStatusbar);
        }
    }

    private void addSystemAppInfo(String systemApp) {
        if (!this.systemAppInfos.contains(systemApp)) {
            this.systemAppInfos.add(systemApp);
        }
    }

    public static void registerNotchSwitchListener(Context context, NotchSwitchListener listener) {
        context.getContentResolver().registerContentObserver(Secure.getUriFor("display_notch_status"), true, listener.mObserver, -1);
    }

    public static void unregisterNotchSwitchListener(Context context, NotchSwitchListener listener) {
        context.getContentResolver().unregisterContentObserver(listener.mObserver);
    }

    public void updateWhitelistByHot(Context context, String fileName) {
        new WhitelistUpdateThread(context, fileName).start();
    }

    public List<String> getNotchSystemApps() {
        return this.systemAppInfos;
    }

    public void removeAppUseNotchMode(String packageName) {
        if (packageName != null) {
            this.mAppUseNotchModeMap.remove(packageName);
        }
    }

    public void updateAppUseNotchMode(String packageName, int mode) {
        if (packageName != null) {
            this.mAppUseNotchModeMap.put(packageName, Integer.valueOf(mode));
        }
    }

    /* JADX WARNING: Missing block: B:9:0x001e, code:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getAppUseNotchMode(String packageName) {
        if (packageName == null || this.mIsNotchSwitchOpen || !this.mAppUseNotchModeMap.containsKey(packageName)) {
            return -1;
        }
        return ((Integer) this.mAppUseNotchModeMap.get(packageName)).intValue();
    }

    public void setNotchSwitchStatus(boolean isNotchSwitchOpen) {
        this.mIsNotchSwitchOpen = isNotchSwitchOpen;
    }
}

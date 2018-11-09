package com.android.server.wm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.WindowManagerPolicy.WindowState;
import com.android.server.devicepolicy.StorageUtils;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwGestureNavWhiteConfig {
    private static final String CURRENT_ROM_VERSION = SystemProperties.get("ro.build.version.incremental", "B001");
    private static final String TAG = "HwGestureNav";
    private static final String WHITE_LIST = "GestureNav_whitelist.xml";
    private static final String XML_ATTRIBUTE_ACTION = "action";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_VERSION = "version";
    private static final String XML_WINDOW = "window";
    private static final String XML_WhITE_LIST = "whitelist";
    private static HwGestureNavWhiteConfig hwGestureNavWhiteConfig;
    final boolean DEBUG;
    final int FLAG_FULLSCREEN = 1024;
    final int FLAG_GESTNAV_SLIDER_ONE = 1;
    Context mContext;
    WindowState mCurrentWin;
    WindowState mNewWin;
    WindowManagerService mService;
    private List<GestureNavAttr> mWindows;

    private class GestureNavAttr {
        boolean action = false;
        String name = null;

        GestureNavAttr() {
        }

        public String toString() {
            return "name:" + this.name + " action:" + this.action;
        }
    }

    private static class WhitelistReadThread extends Thread {
        protected WhitelistReadThread() {
            super("HwGestureNavWhiteConfig update thread");
        }

        public void run() {
            HwGestureNavWhiteConfig.getInstance().initList();
        }
    }

    private class WhitelistUpdateThread extends Thread {
        Context mContext = null;
        String mFileName = null;

        protected WhitelistUpdateThread(Context context, String fileName) {
            super("config update thread");
            this.mContext = context;
            this.mFileName = fileName;
        }

        public void run() {
            if (this.mFileName != null) {
                FileInputStream stream = HwGestureNavWhiteConfig.this.getStreamFromPath(this.mContext, this.mFileName);
                String target = Environment.getDataSystemDirectory() + "/" + HwGestureNavWhiteConfig.CURRENT_ROM_VERSION + "-" + HwGestureNavWhiteConfig.WHITE_LIST;
                if (stream != null) {
                    try {
                        Slog.d(HwGestureNavWhiteConfig.TAG, "target " + target);
                        HwGestureNavWhiteConfig.this.copyFile(stream, target);
                        HwGestureNavWhiteConfig.this.updateconfig();
                    } finally {
                        IoUtils.closeQuietly(stream);
                    }
                }
            }
        }
    }

    public static synchronized HwGestureNavWhiteConfig getInstance() {
        HwGestureNavWhiteConfig hwGestureNavWhiteConfig;
        synchronized (HwGestureNavWhiteConfig.class) {
            if (hwGestureNavWhiteConfig == null) {
                hwGestureNavWhiteConfig = new HwGestureNavWhiteConfig();
                Slog.d(TAG, "getInstance " + hwGestureNavWhiteConfig);
            }
            hwGestureNavWhiteConfig = hwGestureNavWhiteConfig;
        }
        return hwGestureNavWhiteConfig;
    }

    public void initWmsServer(WindowManagerService service, Context context) {
        Slog.d(TAG, "initWmsServer " + this);
        this.mService = service;
        this.mContext = context;
    }

    public void updateconfig() {
        unInitList();
        initList();
    }

    public void updatewindow(WindowState win) {
        this.mNewWin = win;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isEnable() {
        this.mCurrentWin = this.mNewWin;
        if (this.mCurrentWin != null && this.mService != null) {
            int rotation = this.mService.getDefaultDisplayContentLocked().getRotation();
            if (rotation == 0 || rotation == 2) {
                Slog.d(TAG, "rotation is " + rotation);
                return false;
            }
            GestureNavAttr whitename = findInList();
            if (whitename != null) {
                return whitename.action;
            }
            boolean z;
            int LastSystemUiFlags = 0;
            if (this.mService.mPolicy instanceof HwPhoneWindowManager) {
                LastSystemUiFlags = ((HwPhoneWindowManager) this.mService.mPolicy).getLastSystemUiFlags();
            }
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder append = new StringBuilder().append("win:return ");
                if ((this.mCurrentWin.getAttrs().flags & 1024) != 0) {
                    z = true;
                } else {
                    z = false;
                }
                append = append.append(z).append(" ");
                if ((LastSystemUiFlags & 4) != 0) {
                    z = true;
                } else {
                    z = false;
                }
                Slog.d(str, append.append(z).append(" win:").append(this.mCurrentWin).append(" extra ").append(checkgestnavflags(1)).toString());
            }
            if ((this.mCurrentWin.getAttrs().flags & 1024) == 0 && ((LastSystemUiFlags & 4) == 0 || (this.mCurrentWin.toString().contains(GestureNavConst.STATUSBAR_WINDOW) ^ 1) == 0)) {
                z = false;
            } else {
                z = checkgestnavflags(1) ^ 1;
            }
        } else if (this.mService == null) {
            Slog.d(TAG, "mService == null" + this);
        }
    }

    private boolean checkgestnavflags(int flags) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        String pckName = this.mCurrentWin.getAttrs().packageName;
        if (this.DEBUG) {
            Slog.d(TAG, "pckName" + pckName);
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info == null || (info.gestnav_extra_flags & flags) != flags) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            Slog.e(TAG, "not found app" + pckName + "exception=" + ex.toString() + "mCurrentWin " + this.mCurrentWin);
            return false;
        }
    }

    private HwGestureNavWhiteConfig() {
        boolean z = true;
        if (!SystemProperties.getBoolean("ro.debuggable", false)) {
            z = SystemProperties.getBoolean("persist.sys.huawei.debug.on", false);
        }
        this.DEBUG = z;
        this.mWindows = new ArrayList();
        new WhitelistReadThread().start();
    }

    private void initList() {
        long now = System.nanoTime();
        loadconfig();
        Slog.d(TAG, "load config use:" + (System.nanoTime() - now));
    }

    private GestureNavAttr findInList() {
        GestureNavAttr object = findInList(this.mWindows);
        if (object != null) {
            return object;
        }
        return null;
    }

    private String getKeyString(String key) {
        if (key == null) {
            return null;
        }
        if (key.substring(key.length() - 1).equals("*")) {
            return " " + key.substring(0, key.length() - 1);
        }
        return " " + key + "}";
    }

    private GestureNavAttr findInList(List<GestureNavAttr> list) {
        String wininfo = this.mCurrentWin.toString();
        int size = list.size();
        if (this.DEBUG) {
            Slog.d(TAG, "win:" + wininfo + " " + list);
        }
        for (int i = 0; i < size; i++) {
            if (wininfo.contains(((GestureNavAttr) list.get(i)).name)) {
                return (GestureNavAttr) list.get(i);
            }
        }
        return null;
    }

    private void unInitList() {
        this.mWindows.clear();
    }

    private void addWindowToList(GestureNavAttr window) {
        if (this.DEBUG) {
            Slog.d(TAG, "add name:" + window.name + " action " + window.action);
        }
        if (!this.mWindows.contains(window)) {
            this.mWindows.add(window);
        }
    }

    private void loadconfig() {
        String filePath = Environment.getDataSystemDirectory() + "/" + CURRENT_ROM_VERSION + "-" + WHITE_LIST;
        File configfile = new File(filePath);
        if (configfile != null ? configfile.exists() : false) {
            Slog.d(TAG, "load config:" + filePath);
        } else {
            configfile = HwCfgFilePolicy.getCfgFile("xml/GestureNav_whitelist.xml", 0);
            Slog.d(TAG, "load defalut config...");
        }
        loadconfig(configfile);
    }

    private void loadconfig(File configfile) {
        XmlPullParser xmlParser;
        int xmlEventType;
        InputStream inputStream = null;
        if (configfile != null) {
            try {
                if (configfile.exists()) {
                    inputStream = new FileInputStream(configfile);
                    if (inputStream != null) {
                        xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            if (this.DEBUG) {
                                Slog.d(TAG, "xmlname " + xmlParser.getName());
                                Slog.d(TAG, "EventType " + xmlEventType);
                            }
                            if (xmlEventType == 2 || !XML_WINDOW.equals(xmlParser.getName())) {
                                if (xmlEventType == 2) {
                                    if (XML_VERSION.equals(xmlParser.getName())) {
                                        Log.d(TAG, "whitelist version :" + xmlParser.getAttributeValue(null, "name"));
                                    }
                                }
                                if (xmlEventType != 3) {
                                    continue;
                                } else if (XML_WhITE_LIST.equals(xmlParser.getName())) {
                                    break;
                                }
                            } else {
                                GestureNavAttr window = new GestureNavAttr();
                                window.name = getKeyString(xmlParser.getAttributeValue(null, "name"));
                                String value = xmlParser.getAttributeValue(null, "action");
                                window.action = true;
                                if (value.equals(StorageUtils.SDCARD_RWMOUNTED_STATE)) {
                                    window.action = false;
                                }
                                addWindowToList(window);
                            }
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e);
                            return;
                        }
                    }
                }
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "load GestureNav FileNotFoundException: ", e2);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e3) {
                        Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e3);
                        return;
                    }
                }
                return;
            } catch (XmlPullParserException e4) {
                Log.e(TAG, "load GestureNav XmlPullParserException: ", e4);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e32) {
                        Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e32);
                        return;
                    }
                }
                return;
            } catch (IOException e322) {
                Log.e(TAG, "load GestureNav IOException: ", e322);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                        return;
                    } catch (IOException e3222) {
                        Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e3222);
                        return;
                    }
                }
                return;
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e32222) {
                        Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e32222);
                    }
                }
            }
        }
        Slog.w(TAG, "GestureNav_whitelist.xml is not exist");
        if (inputStream != null) {
            xmlParser = Xml.newPullParser();
            xmlParser.setInput(inputStream, null);
            for (xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                if (this.DEBUG) {
                    Slog.d(TAG, "xmlname " + xmlParser.getName());
                    Slog.d(TAG, "EventType " + xmlEventType);
                }
                if (xmlEventType == 2) {
                }
                if (xmlEventType == 2) {
                    if (XML_VERSION.equals(xmlParser.getName())) {
                        Log.d(TAG, "whitelist version :" + xmlParser.getAttributeValue(null, "name"));
                    }
                }
                if (xmlEventType != 3) {
                    if (XML_WhITE_LIST.equals(xmlParser.getName())) {
                        break;
                    }
                } else {
                    continue;
                }
            }
        }
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private boolean copyFile(FileInputStream srcStream, String filePath) {
        if (srcStream == null || filePath == null) {
            return false;
        }
        boolean result;
        File dest = new File(filePath);
        if (dest != null && dest.exists()) {
            dest.delete();
        }
        if (dest != null) {
            try {
                dest.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "IOException:" + e);
            }
        }
        FileChannel fileChannel = null;
        FileChannel fileChannel2 = null;
        try {
            fileChannel = srcStream.getChannel();
            fileChannel2 = new FileOutputStream(dest).getChannel();
            fileChannel.transferTo(0, fileChannel.size(), fileChannel2);
            result = true;
        } catch (FileNotFoundException e2) {
            result = false;
            e2.printStackTrace();
        } catch (IOException e3) {
            result = false;
            e3.printStackTrace();
        }
        if (fileChannel != null) {
            try {
                fileChannel.close();
            } catch (IOException e32) {
                result = false;
                e32.printStackTrace();
            }
        }
        if (fileChannel2 != null) {
            try {
                fileChannel2.close();
            } catch (IOException e322) {
                result = false;
                e322.printStackTrace();
            }
        }
        return result;
    }

    private FileInputStream getStreamFromPath(Context Context, String fileName) {
        try {
            File file = new File(fileName);
            if (file != null) {
                return new FileInputStream(file);
            }
            return null;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:" + e);
            return null;
        }
    }

    public void updateWhitelistByHot(Context context, String fileName) {
        new WhitelistUpdateThread(context, fileName).start();
    }
}

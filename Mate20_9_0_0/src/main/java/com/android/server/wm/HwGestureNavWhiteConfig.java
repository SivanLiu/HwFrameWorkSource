package com.android.server.wm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.server.gesture.GestureNavConst;
import com.android.server.policy.HwPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy.WindowState;
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
    final int FLAG_GESTNAV_SLIDER_ONE;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("name:");
            stringBuilder.append(this.name);
            stringBuilder.append(" action:");
            stringBuilder.append(this.action);
            return stringBuilder.toString();
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
                String target = new StringBuilder();
                target.append(Environment.getDataSystemDirectory());
                target.append("/");
                target.append(HwGestureNavWhiteConfig.CURRENT_ROM_VERSION);
                target.append("-");
                target.append(HwGestureNavWhiteConfig.WHITE_LIST);
                target = target.toString();
                if (stream != null) {
                    try {
                        String str = HwGestureNavWhiteConfig.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("target ");
                        stringBuilder.append(target);
                        Slog.d(str, stringBuilder.toString());
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getInstance ");
                stringBuilder.append(hwGestureNavWhiteConfig);
                Slog.d(str, stringBuilder.toString());
            }
            hwGestureNavWhiteConfig = hwGestureNavWhiteConfig;
        }
        return hwGestureNavWhiteConfig;
    }

    public void initWmsServer(WindowManagerService service, Context context) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initWmsServer ");
        stringBuilder.append(this);
        Slog.d(str, stringBuilder.toString());
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

    /* JADX WARNING: Missing block: B:43:0x00b7, code skipped:
            return r1;
     */
    /* JADX WARNING: Missing block: B:53:0x00eb, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean isEnable() {
        this.mCurrentWin = this.mNewWin;
        boolean z = false;
        if (this.mCurrentWin != null) {
            if (this.mService != null) {
                int rotation = this.mService.getDefaultDisplayContentLocked().getRotation();
                if (rotation != 0) {
                    if (rotation != 2) {
                        GestureNavAttr whitename = findInList();
                        if (whitename != null) {
                            return whitename.action;
                        }
                        int LastSystemUiFlags = 0;
                        if (this.mService.mPolicy instanceof HwPhoneWindowManager) {
                            LastSystemUiFlags = ((HwPhoneWindowManager) this.mService.mPolicy).getLastSystemUiFlags();
                        }
                        if (this.DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("win:return ");
                            stringBuilder.append((this.mCurrentWin.getAttrs().flags & 1024) != 0);
                            stringBuilder.append(" ");
                            stringBuilder.append((LastSystemUiFlags & 4) != 0);
                            stringBuilder.append(" win:");
                            stringBuilder.append(this.mCurrentWin);
                            stringBuilder.append(" extra ");
                            stringBuilder.append(checkgestnavflags(1));
                            Slog.d(str, stringBuilder.toString());
                        }
                        if (!(((this.mCurrentWin.getAttrs().flags & 1024) == 0 && ((LastSystemUiFlags & 4) == 0 || this.mCurrentWin.toString().contains(GestureNavConst.STATUSBAR_WINDOW))) || checkgestnavflags(1))) {
                            z = true;
                        }
                    }
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("rotation is ");
                stringBuilder2.append(rotation);
                Slog.d(str2, stringBuilder2.toString());
                return false;
            }
        }
        if (this.mService == null) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mService == null");
            stringBuilder3.append(this);
            Slog.d(str3, stringBuilder3.toString());
        }
    }

    private boolean checkgestnavflags(int flags) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        String pckName = this.mCurrentWin.getAttrs().packageName;
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pckName");
            stringBuilder.append(pckName);
            Slog.d(str, stringBuilder.toString());
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info == null || (info.gestnav_extra_flags & flags) != flags) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("not found app");
            stringBuilder2.append(pckName);
            stringBuilder2.append("exception=");
            stringBuilder2.append(ex.toString());
            stringBuilder2.append("mCurrentWin ");
            stringBuilder2.append(this.mCurrentWin);
            Slog.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    private HwGestureNavWhiteConfig() {
        boolean z = true;
        this.FLAG_GESTNAV_SLIDER_ONE = 1;
        if (!(SystemProperties.getBoolean("ro.debuggable", false) || SystemProperties.getBoolean("persist.sys.huawei.debug.on", false))) {
            z = false;
        }
        this.DEBUG = z;
        this.mWindows = new ArrayList();
        new WhitelistReadThread().start();
    }

    private void initList() {
        long now = System.nanoTime();
        loadconfig();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("load config use:");
        stringBuilder.append(System.nanoTime() - now);
        Slog.d(str, stringBuilder.toString());
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
        StringBuilder stringBuilder;
        if (key.substring(key.length() - 1).equals("*")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" ");
            stringBuilder.append(key.substring(0, key.length() - 1));
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" ");
        stringBuilder.append(key);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private GestureNavAttr findInList(List<GestureNavAttr> list) {
        String wininfo = this.mCurrentWin.toString();
        int size = list.size();
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("win:");
            stringBuilder.append(wininfo);
            stringBuilder.append(" ");
            stringBuilder.append(list);
            Slog.d(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("add name:");
            stringBuilder.append(window.name);
            stringBuilder.append(" action ");
            stringBuilder.append(window.action);
            Slog.d(str, stringBuilder.toString());
        }
        if (!this.mWindows.contains(window)) {
            this.mWindows.add(window);
        }
    }

    private void loadconfig() {
        String filePath = new StringBuilder();
        filePath.append(Environment.getDataSystemDirectory());
        filePath.append("/");
        filePath.append(CURRENT_ROM_VERSION);
        filePath.append("-");
        filePath.append(WHITE_LIST);
        filePath = filePath.toString();
        File configfile = new File(filePath);
        if (configfile.exists()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("load config:");
            stringBuilder.append(filePath);
            Slog.d(str, stringBuilder.toString());
        } else {
            configfile = HwCfgFilePolicy.getCfgFile("xml/GestureNav_whitelist.xml", 0);
            Slog.d(TAG, "load defalut config...");
        }
        loadconfig(configfile);
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0027 A:{Catch:{ FileNotFoundException -> 0x001b, XmlPullParserException -> 0x0018, IOException -> 0x0015, all -> 0x0012 }} */
    /* JADX WARNING: Removed duplicated region for block: B:69:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00ea A:{SYNTHETIC, Splitter:B:42:0x00ea} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadconfig(File configfile) {
        InputStream inputStream = null;
        XmlPullParser xmlParser = null;
        if (configfile != null) {
            try {
                if (configfile.exists()) {
                    inputStream = new FileInputStream(configfile);
                    if (inputStream != null) {
                        xmlParser = Xml.newPullParser();
                        xmlParser.setInput(inputStream, null);
                        for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                            String str;
                            StringBuilder stringBuilder;
                            if (this.DEBUG) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("xmlname ");
                                stringBuilder.append(xmlParser.getName());
                                Slog.d(str, stringBuilder.toString());
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("EventType ");
                                stringBuilder.append(xmlEventType);
                                Slog.d(str, stringBuilder.toString());
                            }
                            if (xmlEventType == 2 && XML_WINDOW.equals(xmlParser.getName())) {
                                GestureNavAttr window = new GestureNavAttr();
                                window.name = getKeyString(xmlParser.getAttributeValue(null, "name"));
                                String value = xmlParser.getAttributeValue(null, "action");
                                window.action = true;
                                if (value.equals("false")) {
                                    window.action = false;
                                }
                                addWindowToList(window);
                            } else if (xmlEventType != 2 || !XML_VERSION.equals(xmlParser.getName())) {
                                if (xmlEventType == 3 && XML_WhITE_LIST.equals(xmlParser.getName())) {
                                    break;
                                }
                            } else {
                                String name = xmlParser.getAttributeValue(null, "name");
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("whitelist version :");
                                stringBuilder.append(name);
                                Log.d(str, stringBuilder.toString());
                            }
                        }
                    }
                    if (inputStream == null) {
                        try {
                            inputStream.close();
                            return;
                        } catch (IOException e) {
                            Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e);
                            return;
                        }
                    }
                    return;
                }
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "load GestureNav FileNotFoundException: ", e2);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (XmlPullParserException e22) {
                Log.e(TAG, "load GestureNav XmlPullParserException: ", e22);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (IOException e222) {
                Log.e(TAG, "load GestureNav IOException: ", e222);
                if (inputStream != null) {
                    inputStream.close();
                    return;
                }
                return;
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "load GestureNav config: IO Exception while closing stream", e3);
                    }
                }
            }
        }
        Slog.w(TAG, "GestureNav_whitelist.xml is not exist");
        if (inputStream != null) {
        }
        if (inputStream == null) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0067 A:{SYNTHETIC, Splitter:B:26:0x0067} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0074 A:{SYNTHETIC, Splitter:B:31:0x0074} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0067 A:{SYNTHETIC, Splitter:B:26:0x0067} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0074 A:{SYNTHETIC, Splitter:B:31:0x0074} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0067 A:{SYNTHETIC, Splitter:B:26:0x0067} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0074 A:{SYNTHETIC, Splitter:B:31:0x0074} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0067 A:{SYNTHETIC, Splitter:B:26:0x0067} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0074 A:{SYNTHETIC, Splitter:B:31:0x0074} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean copyFile(FileInputStream srcStream, String filePath) {
        boolean result;
        FileNotFoundException e;
        IOException e2;
        if (srcStream == null || filePath == null) {
            return false;
        }
        File dest = new File(filePath);
        if (dest.exists()) {
            dest.delete();
        }
        try {
            dest.createNewFile();
        } catch (IOException e3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException:");
            stringBuilder.append(e3);
            Log.e(str, stringBuilder.toString());
        }
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = srcStream.getChannel();
            FileChannel dstChannel2 = new FileOutputStream(dest).getChannel();
            try {
                srcChannel.transferTo(0, srcChannel.size(), dstChannel2);
                result = true;
                dstChannel = dstChannel2;
            } catch (FileNotFoundException e4) {
                e = e4;
                dstChannel = dstChannel2;
                result = false;
                e.printStackTrace();
                if (srcChannel != null) {
                }
                if (dstChannel != null) {
                }
                return result;
            } catch (IOException e5) {
                e2 = e5;
                dstChannel = dstChannel2;
                result = false;
                e2.printStackTrace();
                if (srcChannel != null) {
                }
                if (dstChannel != null) {
                }
                return result;
            }
        } catch (FileNotFoundException e6) {
            e = e6;
            result = false;
            e.printStackTrace();
            if (srcChannel != null) {
            }
            if (dstChannel != null) {
            }
            return result;
        } catch (IOException e7) {
            e2 = e7;
            result = false;
            e2.printStackTrace();
            if (srcChannel != null) {
            }
            if (dstChannel != null) {
            }
            return result;
        }
        if (srcChannel != null) {
            try {
                srcChannel.close();
            } catch (IOException e22) {
                result = false;
                e22.printStackTrace();
            }
        }
        if (dstChannel != null) {
            try {
                dstChannel.close();
            } catch (IOException e222) {
                result = false;
                e222.printStackTrace();
            }
        }
        return result;
    }

    private FileInputStream getStreamFromPath(Context Context, String fileName) {
        ParcelFileDescriptor parcelFileDesc = null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(fileName));
            if (parcelFileDesc != null) {
                try {
                    parcelFileDesc.close();
                } catch (IOException e) {
                    Log.e(TAG, "parcelFileDesc error!");
                }
            }
        } catch (FileNotFoundException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FileNotFoundException:");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
            if (parcelFileDesc != null) {
                parcelFileDesc.close();
            }
        } catch (Throwable th) {
            if (parcelFileDesc != null) {
                try {
                    parcelFileDesc.close();
                } catch (IOException e3) {
                    Log.e(TAG, "parcelFileDesc error!");
                }
            }
        }
        return inputStream;
    }

    public void updateWhitelistByHot(Context context, String fileName) {
        new WhitelistUpdateThread(context, fileName).start();
    }
}

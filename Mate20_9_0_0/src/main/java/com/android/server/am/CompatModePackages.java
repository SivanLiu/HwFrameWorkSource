package com.android.server.am;

import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.AbsLocationManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class CompatModePackages {
    public static final int COMPAT_FLAG_DONT_ASK = 1;
    public static final int COMPAT_FLAG_ENABLED = 2;
    private static final int MSG_WRITE = 300;
    private static final String TAG = "ActivityManager";
    private static final String TAG_CONFIGURATION;
    private final AtomicFile mFile;
    private final CompatHandler mHandler;
    private final HashMap<String, Integer> mPackages = new HashMap();
    private final ActivityManagerService mService;

    private final class CompatHandler extends Handler {
        public CompatHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 300) {
                CompatModePackages.this.saveCompatModes();
            }
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActivityManager");
        stringBuilder.append(ActivityManagerDebugConfig.POSTFIX_CONFIGURATION);
        TAG_CONFIGURATION = stringBuilder.toString();
    }

    public CompatModePackages(ActivityManagerService service, File systemDir, Handler handler) {
        this.mService = service;
        this.mFile = new AtomicFile(new File(systemDir, "packages-compat.xml"), "compat-mode");
        this.mHandler = new CompatHandler(handler.getLooper());
        FileInputStream fis = null;
        try {
            fis = this.mFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
            }
            if (eventType == 1) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                return;
            }
            if ("compat-packages".equals(parser.getName())) {
                eventType = parser.next();
                do {
                    if (eventType == 2) {
                        String tagName = parser.getName();
                        if (parser.getDepth() == 2 && AbsLocationManagerService.DEL_PKG.equals(tagName)) {
                            String pkg = parser.getAttributeValue(null, Settings.ATTR_NAME);
                            if (pkg != null) {
                                String mode = parser.getAttributeValue(null, "mode");
                                int modeInt = 0;
                                if (mode != null) {
                                    try {
                                        modeInt = Integer.parseInt(mode);
                                    } catch (NumberFormatException e2) {
                                    }
                                }
                                this.mPackages.put(pkg, Integer.valueOf(modeInt));
                            }
                        }
                    }
                    eventType = parser.next();
                } while (eventType != 1);
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                }
            }
        } catch (XmlPullParserException e4) {
            Slog.w("ActivityManager", "Error reading compat-packages", e4);
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e5) {
            if (fis != null) {
                Slog.w("ActivityManager", "Error reading compat-packages", e5);
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e6) {
                }
            }
        }
    }

    public HashMap<String, Integer> getPackages() {
        return this.mPackages;
    }

    private int getPackageFlags(String packageName) {
        Integer flags = (Integer) this.mPackages.get(packageName);
        return flags != null ? flags.intValue() : 0;
    }

    public void handlePackageDataClearedLocked(String packageName) {
        removePackage(packageName);
    }

    public void handlePackageUninstalledLocked(String packageName) {
        removePackage(packageName);
    }

    private void removePackage(String packageName) {
        if (this.mPackages.containsKey(packageName)) {
            this.mPackages.remove(packageName);
            scheduleWrite();
        }
    }

    public void handlePackageAddedLocked(String packageName, boolean updated) {
        ApplicationInfo ai = null;
        boolean mayCompat = false;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (ai != null) {
            CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
            if (!(ci.alwaysSupportsScreen() || ci.neverSupportsScreen())) {
                mayCompat = true;
            }
            if (updated && !mayCompat && this.mPackages.containsKey(packageName)) {
                this.mPackages.remove(packageName);
                scheduleWrite();
            }
        }
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(300), JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    public CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo ai) {
        Configuration globalConfig = this.mService.getGlobalConfiguration();
        return new CompatibilityInfo(ai, globalConfig.screenLayout, globalConfig.smallestScreenWidthDp, (getPackageFlags(ai.packageName) & 2) != 0);
    }

    public int computeCompatModeLocked(ApplicationInfo ai) {
        int i = 0;
        boolean enabled = (getPackageFlags(ai.packageName) & 2) != 0;
        Configuration globalConfig = this.mService.getGlobalConfiguration();
        CompatibilityInfo info = new CompatibilityInfo(ai, globalConfig.screenLayout, globalConfig.smallestScreenWidthDp, enabled);
        if (info.alwaysSupportsScreen()) {
            return -2;
        }
        if (info.neverSupportsScreen()) {
            return -1;
        }
        if (enabled) {
            i = 1;
        }
        return i;
    }

    public boolean getFrontActivityAskCompatModeLocked() {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked();
        if (r == null) {
            return false;
        }
        return getPackageAskCompatModeLocked(r.packageName);
    }

    public boolean getPackageAskCompatModeLocked(String packageName) {
        return (getPackageFlags(packageName) & 1) == 0;
    }

    public void setFrontActivityAskCompatModeLocked(boolean ask) {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked();
        if (r != null) {
            setPackageAskCompatModeLocked(r.packageName, ask);
        }
    }

    public void setPackageAskCompatModeLocked(String packageName, boolean ask) {
        setPackageFlagLocked(packageName, 1, ask);
    }

    private void setPackageFlagLocked(String packageName, int flag, boolean set) {
        int curFlags = getPackageFlags(packageName);
        int newFlags = set ? (~flag) & curFlags : curFlags | flag;
        if (curFlags != newFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            scheduleWrite();
        }
    }

    public int getFrontActivityScreenCompatModeLocked() {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked();
        if (r == null) {
            return -3;
        }
        return computeCompatModeLocked(r.info.applicationInfo);
    }

    public void setFrontActivityScreenCompatModeLocked(int mode) {
        ActivityRecord r = this.mService.getFocusedStack().topRunningActivityLocked();
        if (r == null) {
            Slog.w("ActivityManager", "setFrontActivityScreenCompatMode failed: no top activity");
        } else {
            setPackageScreenCompatModeLocked(r.info.applicationInfo, mode);
        }
    }

    public int getPackageScreenCompatModeLocked(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, UserHandle.getCallingUserId());
        } catch (RemoteException e) {
        }
        if (ai == null) {
            return -3;
        }
        return computeCompatModeLocked(ai);
    }

    public void setPackageScreenCompatModeLocked(String packageName, int mode) {
        ApplicationInfo ai = null;
        try {
            ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPackageScreenCompatMode failed: unknown package ");
            stringBuilder.append(packageName);
            Slog.w("ActivityManager", stringBuilder.toString());
            return;
        }
        setPackageScreenCompatModeLocked(ai, mode);
    }

    private void setPackageScreenCompatModeLocked(ApplicationInfo ai, int mode) {
        boolean enable;
        StringBuilder stringBuilder;
        String packageName = ai.packageName;
        int curFlags = getPackageFlags(packageName);
        switch (mode) {
            case 0:
                enable = false;
                break;
            case 1:
                enable = true;
                break;
            case 2:
                if ((curFlags & 2) != 0) {
                    enable = false;
                    break;
                } else {
                    enable = true;
                    break;
                }
            default:
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown screen compat mode req #");
                stringBuilder2.append(mode);
                stringBuilder2.append("; ignoring");
                Slog.w("ActivityManager", stringBuilder2.toString());
                return;
        }
        int newFlags = curFlags;
        if (enable) {
            newFlags |= 2;
        } else {
            newFlags &= -3;
        }
        CompatibilityInfo ci = compatibilityInfoForPackageLocked(ai);
        if (ci.alwaysSupportsScreen()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring compat mode change of ");
            stringBuilder.append(packageName);
            stringBuilder.append("; compatibility never needed");
            Slog.w("ActivityManager", stringBuilder.toString());
            newFlags = 0;
        }
        if (ci.neverSupportsScreen()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring compat mode change of ");
            stringBuilder.append(packageName);
            stringBuilder.append("; compatibility always needed");
            Slog.w("ActivityManager", stringBuilder.toString());
            newFlags = 0;
        }
        if (newFlags != curFlags) {
            if (newFlags != 0) {
                this.mPackages.put(packageName, Integer.valueOf(newFlags));
            } else {
                this.mPackages.remove(packageName);
            }
            ci = compatibilityInfoForPackageLocked(ai);
            scheduleWrite();
            ActivityStack stack = this.mService.getFocusedStack();
            ActivityRecord starting = stack.restartPackage(packageName);
            int i = this.mService.mLruProcesses.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    ProcessRecord app = (ProcessRecord) this.mService.mLruProcesses.get(i2);
                    if (app.pkgList.containsKey(packageName)) {
                        try {
                            if (app.thread != null) {
                                if (ActivityManagerDebugConfig.DEBUG_CONFIGURATION) {
                                    String str = TAG_CONFIGURATION;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Sending to proc ");
                                    stringBuilder3.append(app.processName);
                                    stringBuilder3.append(" new compat ");
                                    stringBuilder3.append(ci);
                                    Slog.v(str, stringBuilder3.toString());
                                }
                                app.thread.updatePackageCompatibilityInfo(packageName, ci);
                            }
                        } catch (Exception e) {
                        }
                    }
                    i = i2 - 1;
                } else if (starting != null) {
                    starting.ensureActivityConfiguration(0, false);
                    stack.ensureActivitiesVisibleLocked(starting, 0, false);
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:51:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00e5  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00e5  */
    /* JADX WARNING: Removed duplicated region for block: B:51:? A:{SYNTHETIC, RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void saveCompatModes() {
        HashMap<String, Integer> pkgs;
        IOException e1;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pkgs = new HashMap(this.mPackages);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        String str = null;
        FileOutputStream fos = null;
        HashMap<String, Integer> hashMap;
        try {
            fos = this.mFile.startWrite();
            FastXmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "compat-packages");
            IPackageManager pm = AppGlobals.getPackageManager();
            Configuration globalConfig = this.mService.getGlobalConfiguration();
            int screenLayout = globalConfig.screenLayout;
            int smallestScreenWidthDp = globalConfig.smallestScreenWidthDp;
            Iterator<Entry<String, Integer>> it = pkgs.entrySet().iterator();
            while (true) {
                Iterator<Entry<String, Integer>> it2 = it;
                if (it2.hasNext()) {
                    Entry<String, Integer> entry = (Entry) it2.next();
                    String pkg = (String) entry.getKey();
                    int mode = ((Integer) entry.getValue()).intValue();
                    if (mode != 0) {
                        ApplicationInfo ai = str;
                        try {
                            ai = pm.getApplicationInfo(pkg, 0, 0);
                        } catch (RemoteException e) {
                        } catch (IOException e2) {
                            e1 = e2;
                            hashMap = pkgs;
                            Slog.w("ActivityManager", "Error writing compat packages", e1);
                            if (fos != null) {
                            }
                        }
                        if (ai != null) {
                            CompatibilityInfo info = new CompatibilityInfo(ai, screenLayout, smallestScreenWidthDp, false);
                            if (!info.alwaysSupportsScreen()) {
                                if (!info.neverSupportsScreen()) {
                                    out.startTag(str, AbsLocationManagerService.DEL_PKG);
                                    out.attribute(str, Settings.ATTR_NAME, pkg);
                                    hashMap = pkgs;
                                    try {
                                        out.attribute(null, "mode", Integer.toString(mode));
                                        out.endTag(null, AbsLocationManagerService.DEL_PKG);
                                        it = it2;
                                        pkgs = hashMap;
                                        str = null;
                                    } catch (IOException e3) {
                                        e1 = e3;
                                        Slog.w("ActivityManager", "Error writing compat packages", e1);
                                        if (fos != null) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    hashMap = pkgs;
                    it = it2;
                    pkgs = hashMap;
                    str = null;
                } else {
                    out.endTag(null, "compat-packages");
                    out.endDocument();
                    this.mFile.finishWrite(fos);
                    return;
                }
            }
        } catch (IOException e4) {
            e1 = e4;
            hashMap = pkgs;
            Slog.w("ActivityManager", "Error writing compat packages", e1);
            if (fos != null) {
                this.mFile.failWrite(fos);
            }
        }
    }
}

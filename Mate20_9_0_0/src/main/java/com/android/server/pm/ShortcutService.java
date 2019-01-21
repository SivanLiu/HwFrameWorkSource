package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.usage.UsageStatsManagerInternal;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IShortcutService.Stub;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.ShortcutServiceInternal.ShortcutChangeListener;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.TypedValue;
import android.util.Xml;
import android.view.IWindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.StatLogger;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.power.IHwShutdownThread;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ShortcutService extends Stub {
    private static Predicate<ResolveInfo> ACTIVITY_NOT_EXPORTED = new Predicate<ResolveInfo>() {
        public boolean test(ResolveInfo ri) {
            return ri.activityInfo.exported ^ 1;
        }
    };
    private static final String ATTR_VALUE = "value";
    static final boolean DEBUG = false;
    static final boolean DEBUG_LOAD = false;
    static final boolean DEBUG_PROCSTATE = false;
    @VisibleForTesting
    static final String DEFAULT_ICON_PERSIST_FORMAT = CompressFormat.PNG.name();
    @VisibleForTesting
    static final int DEFAULT_ICON_PERSIST_QUALITY = 100;
    @VisibleForTesting
    static final int DEFAULT_MAX_ICON_DIMENSION_DP = 96;
    @VisibleForTesting
    static final int DEFAULT_MAX_ICON_DIMENSION_LOWRAM_DP = 48;
    @VisibleForTesting
    static final int DEFAULT_MAX_SHORTCUTS_PER_APP = 5;
    @VisibleForTesting
    static final int DEFAULT_MAX_UPDATES_PER_INTERVAL = 10;
    @VisibleForTesting
    static final long DEFAULT_RESET_INTERVAL_SEC = 86400;
    @VisibleForTesting
    static final int DEFAULT_SAVE_DELAY_MS = 3000;
    static final String DIRECTORY_BITMAPS = "bitmaps";
    @VisibleForTesting
    static final String DIRECTORY_DUMP = "shortcut_dump";
    @VisibleForTesting
    static final String DIRECTORY_PER_USER = "shortcut_service";
    private static final String DUMMY_MAIN_ACTIVITY = "android.__dummy__";
    private static List<ResolveInfo> EMPTY_RESOLVE_INFO = new ArrayList(0);
    @VisibleForTesting
    static final String FILENAME_BASE_STATE = "shortcut_service.xml";
    @VisibleForTesting
    static final String FILENAME_USER_PACKAGES = "shortcuts.xml";
    private static final String KEY_ICON_SIZE = "iconSize";
    private static final String KEY_LOW_RAM = "lowRam";
    private static final String KEY_SHORTCUT = "shortcut";
    private static final String LAUNCHER_INTENT_CATEGORY = "android.intent.category.LAUNCHER";
    static final int OPERATION_ADD = 1;
    static final int OPERATION_SET = 0;
    static final int OPERATION_UPDATE = 2;
    private static final int PACKAGE_MATCH_FLAGS = 794624;
    private static Predicate<PackageInfo> PACKAGE_NOT_INSTALLED = new Predicate<PackageInfo>() {
        public boolean test(PackageInfo pi) {
            return ShortcutService.isInstalled(pi) ^ 1;
        }
    };
    private static final int PROCESS_STATE_FOREGROUND_THRESHOLD = 4;
    static final String TAG = "ShortcutService";
    private static final String TAG_LAST_RESET_TIME = "last_reset_time";
    private static final String TAG_ROOT = "root";
    private static Set<String> sObtainShortCutPermissionpackageNames = new HashSet();
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AtomicBoolean mBootCompleted;
    final Context mContext;
    @GuardedBy("mLock")
    private List<Integer> mDirtyUserIds;
    private final Handler mHandler;
    private final IPackageManager mIPackageManager;
    private CompressFormat mIconPersistFormat;
    private int mIconPersistQuality;
    @GuardedBy("mLock")
    private Exception mLastWtfStacktrace;
    @GuardedBy("mLock")
    private final ArrayList<ShortcutChangeListener> mListeners;
    private final Object mLock;
    private int mMaxIconDimension;
    private int mMaxShortcuts;
    int mMaxUpdatesPerInterval;
    private final PackageManagerInternal mPackageManagerInternal;
    @VisibleForTesting
    final BroadcastReceiver mPackageMonitor;
    @GuardedBy("mLock")
    private long mRawLastResetTime;
    final BroadcastReceiver mReceiver;
    private long mResetInterval;
    private int mSaveDelayMillis;
    private final Runnable mSaveDirtyInfoRunner;
    private final ShortcutBitmapSaver mShortcutBitmapSaver;
    private final ShortcutDumpFiles mShortcutDumpFiles;
    @GuardedBy("mLock")
    private final SparseArray<ShortcutNonPersistentUser> mShortcutNonPersistentUsers;
    private final ShortcutRequestPinProcessor mShortcutRequestPinProcessor;
    private final StatLogger mStatLogger;
    @GuardedBy("mLock")
    final SparseLongArray mUidLastForegroundElapsedTime;
    private final IUidObserver mUidObserver;
    @GuardedBy("mLock")
    final SparseIntArray mUidState;
    @GuardedBy("mUnlockedUsers")
    final SparseBooleanArray mUnlockedUsers;
    private final UsageStatsManagerInternal mUsageStatsManagerInternal;
    private final UserManagerInternal mUserManagerInternal;
    @GuardedBy("mLock")
    private final SparseArray<ShortcutUser> mUsers;
    @GuardedBy("mLock")
    private int mWtfCount;

    static class CommandException extends Exception {
        public CommandException(String message) {
            super(message);
        }
    }

    @VisibleForTesting
    interface ConfigConstants {
        public static final String KEY_ICON_FORMAT = "icon_format";
        public static final String KEY_ICON_QUALITY = "icon_quality";
        public static final String KEY_MAX_ICON_DIMENSION_DP = "max_icon_dimension_dp";
        public static final String KEY_MAX_ICON_DIMENSION_DP_LOWRAM = "max_icon_dimension_dp_lowram";
        public static final String KEY_MAX_SHORTCUTS = "max_shortcuts";
        public static final String KEY_MAX_UPDATES_PER_INTERVAL = "max_updates_per_interval";
        public static final String KEY_RESET_INTERVAL_SEC = "reset_interval_sec";
        public static final String KEY_SAVE_DELAY_MILLIS = "save_delay_ms";
    }

    static class DumpFilter {
        private boolean mCheckInClear = false;
        private boolean mDumpCheckIn = false;
        private boolean mDumpDetails = true;
        private boolean mDumpFiles = false;
        private boolean mDumpMain = true;
        private boolean mDumpUid = false;
        private List<Pattern> mPackagePatterns = new ArrayList();
        private List<Integer> mUsers = new ArrayList();

        DumpFilter() {
        }

        void addPackageRegex(String regex) {
            this.mPackagePatterns.add(Pattern.compile(regex));
        }

        public void addPackage(String packageName) {
            addPackageRegex(Pattern.quote(packageName));
        }

        void addUser(int userId) {
            this.mUsers.add(Integer.valueOf(userId));
        }

        boolean isPackageMatch(String packageName) {
            if (this.mPackagePatterns.size() == 0) {
                return true;
            }
            for (int i = 0; i < this.mPackagePatterns.size(); i++) {
                if (((Pattern) this.mPackagePatterns.get(i)).matcher(packageName).find()) {
                    return true;
                }
            }
            return false;
        }

        boolean isUserMatch(int userId) {
            if (this.mUsers.size() == 0) {
                return true;
            }
            for (int i = 0; i < this.mUsers.size(); i++) {
                if (((Integer) this.mUsers.get(i)).intValue() == userId) {
                    return true;
                }
            }
            return false;
        }

        public boolean shouldDumpCheckIn() {
            return this.mDumpCheckIn;
        }

        public void setDumpCheckIn(boolean dumpCheckIn) {
            this.mDumpCheckIn = dumpCheckIn;
        }

        public boolean shouldCheckInClear() {
            return this.mCheckInClear;
        }

        public void setCheckInClear(boolean checkInClear) {
            this.mCheckInClear = checkInClear;
        }

        public boolean shouldDumpMain() {
            return this.mDumpMain;
        }

        public void setDumpMain(boolean dumpMain) {
            this.mDumpMain = dumpMain;
        }

        public boolean shouldDumpUid() {
            return this.mDumpUid;
        }

        public void setDumpUid(boolean dumpUid) {
            this.mDumpUid = dumpUid;
        }

        public boolean shouldDumpFiles() {
            return this.mDumpFiles;
        }

        public void setDumpFiles(boolean dumpFiles) {
            this.mDumpFiles = dumpFiles;
        }

        public boolean shouldDumpDetails() {
            return this.mDumpDetails;
        }

        public void setDumpDetails(boolean dumpDetails) {
            this.mDumpDetails = dumpDetails;
        }
    }

    @VisibleForTesting
    static class FileOutputStreamWithPath extends FileOutputStream {
        private final File mFile;

        public FileOutputStreamWithPath(File file) throws FileNotFoundException {
            super(file);
            this.mFile = file;
        }

        public File getFile() {
            return this.mFile;
        }
    }

    static class InvalidFileFormatException extends Exception {
        public InvalidFileFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private class LocalService extends ShortcutServiceInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(ShortcutService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Missing block: B:34:0x0099, code skipped:
            return com.android.server.pm.ShortcutService.access$300(r3.this$0, r30);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public List<ShortcutInfo> getShortcuts(int launcherUserId, String callingPackage, long changedSince, String packageName, List<String> shortcutIds, ComponentName componentName, int queryFlags, int userId, int callingPid, int callingUid) {
            ArrayList<ShortcutInfo> ret;
            Throwable th;
            ArrayList<ShortcutInfo> arrayList;
            int i = launcherUserId;
            int i2 = userId;
            ArrayList<ShortcutInfo> ret2 = new ArrayList();
            int cloneFlag = (queryFlags & 4) != 0 ? 4 : 11;
            List<String> shortcutIds2 = packageName == null ? null : shortcutIds;
            synchronized (ShortcutService.this.mLock) {
                int i3;
                try {
                    ShortcutService.this.throwIfUserLockedL(i2);
                    ShortcutService.this.throwIfUserLockedL(i);
                    String str = callingPackage;
                    ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                    LocalService localService;
                    if (packageName != null) {
                        ret = ret2;
                        try {
                            getShortcutsInnerLocked(i, str, packageName, shortcutIds2, changedSince, componentName, queryFlags, i2, ret2, cloneFlag, callingPid, callingUid);
                            localService = this;
                            i3 = userId;
                        } catch (Throwable th2) {
                            th = th2;
                            arrayList = ret;
                            i3 = userId;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    }
                    ret = ret2;
                    try {
                        i3 = userId;
                        try {
                            -$$Lambda$ShortcutService$LocalService$Q0t7aDuDFJ8HWAf1NHW1dGQjOf8 -__lambda_shortcutservice_localservice_q0t7adudfj8hwaf1nhw1dgqjof8 = -__lambda_shortcutservice_localservice_q0t7adudfj8hwaf1nhw1dgqjof8;
                            localService = this;
                        } catch (Throwable th4) {
                            th = th4;
                            arrayList = ret;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                        try {
                            ShortcutService.this.getUserShortcutsLocked(i3).forAllPackages(new -$$Lambda$ShortcutService$LocalService$Q0t7aDuDFJ8HWAf1NHW1dGQjOf8(this, launcherUserId, callingPackage, shortcutIds2, changedSince, componentName, queryFlags, i3, ret, cloneFlag, callingPid, callingUid));
                        } catch (Throwable th5) {
                            th = th5;
                            arrayList = ret;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        arrayList = ret;
                        i3 = userId;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    arrayList = ret2;
                    i3 = i2;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }

        @GuardedBy("ShortcutService.this.mLock")
        private void getShortcutsInnerLocked(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, long changedSince, ComponentName componentName, int queryFlags, int userId, ArrayList<ShortcutInfo> ret, int cloneFlag, int callingPid, int callingUid) {
            ArraySet<String> arraySet;
            List<String> list = shortcutIds;
            int i = queryFlags;
            if (list == null) {
                arraySet = null;
            } else {
                arraySet = new ArraySet(list);
            }
            ArraySet<String> ids = arraySet;
            ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
            if (p != null) {
                boolean z = false;
                boolean matchDynamic = (i & 1) != 0;
                boolean matchPinned = (i & 2) != 0;
                boolean matchManifest = (i & 8) != 0;
                if (ShortcutService.this.canSeeAnyPinnedShortcut(callingPackage, launcherUserId, callingPid, callingUid) && (i & 1024) != 0) {
                    z = true;
                }
                boolean getPinnedByAnyLauncher = z;
                p.findAll(ret, new -$$Lambda$ShortcutService$LocalService$ltDE7qm9grkumxffFI8cLCFpNqU(changedSince, ids, componentName, matchDynamic, matchPinned, getPinnedByAnyLauncher, matchManifest), cloneFlag, callingPackage, launcherUserId, getPinnedByAnyLauncher);
            }
        }

        static /* synthetic */ boolean lambda$getShortcutsInnerLocked$1(long changedSince, ArraySet ids, ComponentName componentName, boolean matchDynamic, boolean matchPinned, boolean getPinnedByAnyLauncher, boolean matchManifest, ShortcutInfo si) {
            if (si.getLastChangedTimestamp() < changedSince) {
                return false;
            }
            if (ids != null && !ids.contains(si.getId())) {
                return false;
            }
            if (componentName != null && si.getActivity() != null && !si.getActivity().equals(componentName)) {
                return false;
            }
            if (matchDynamic && si.isDynamic()) {
                return true;
            }
            if ((matchPinned || getPinnedByAnyLauncher) && si.isPinned()) {
                return true;
            }
            if (matchManifest && si.isDeclaredInManifest()) {
                return true;
            }
            return false;
        }

        public boolean isPinnedByCaller(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            boolean z;
            Preconditions.checkStringNotEmpty(packageName, "packageName");
            Preconditions.checkStringNotEmpty(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutInfo si = getShortcutInfoLocked(launcherUserId, callingPackage, packageName, shortcutId, userId, false);
                z = si != null && si.isPinned();
            }
            return z;
        }

        @GuardedBy("ShortcutService.this.mLock")
        private ShortcutInfo getShortcutInfoLocked(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId, boolean getPinnedByAnyLauncher) {
            String str = packageName;
            String str2 = shortcutId;
            int i = userId;
            Preconditions.checkStringNotEmpty(str, "packageName");
            Preconditions.checkStringNotEmpty(str2, "shortcutId");
            ShortcutService.this.throwIfUserLockedL(i);
            int i2 = launcherUserId;
            ShortcutService.this.throwIfUserLockedL(i2);
            ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(i).getPackageShortcutsIfExists(str);
            ShortcutInfo shortcutInfo = null;
            if (p == null) {
                return null;
            }
            ArrayList<ShortcutInfo> list = new ArrayList(1);
            p.findAll(list, new -$$Lambda$ShortcutService$LocalService$a6cj3oQpS-Z6FB4DytB0FytYmiM(str2), 0, callingPackage, i2, getPinnedByAnyLauncher);
            if (list.size() != 0) {
                shortcutInfo = (ShortcutInfo) list.get(0);
            }
            return shortcutInfo;
        }

        public void pinShortcuts(int launcherUserId, String callingPackage, String packageName, List<String> shortcutIds, int userId) {
            Preconditions.checkStringNotEmpty(packageName, "packageName");
            Preconditions.checkNotNull(shortcutIds, "shortcutIds");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutLauncher launcher = ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId);
                launcher.attemptToRestoreIfNeededAndSave();
                launcher.pinShortcuts(userId, packageName, shortcutIds, false);
            }
            ShortcutService.this.packageShortcutsChanged(packageName, userId);
            ShortcutService.this.verifyStates();
        }

        public Intent[] createShortcutIntents(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId, int callingPid, int callingUid) {
            Throwable th;
            int i = launcherUserId;
            String str = callingPackage;
            String str2 = shortcutId;
            int i2 = userId;
            String str3 = packageName;
            Preconditions.checkStringNotEmpty(str3, "packageName can't be empty");
            Preconditions.checkStringNotEmpty(str2, "shortcutId can't be empty");
            synchronized (ShortcutService.this.mLock) {
                try {
                    ShortcutService.this.throwIfUserLockedL(i2);
                    ShortcutService.this.throwIfUserLockedL(i);
                    ShortcutService.this.getLauncherShortcutsLocked(str, i2, i).attemptToRestoreIfNeededAndSave();
                    boolean getPinnedByAnyLauncher = ShortcutService.this.canSeeAnyPinnedShortcut(str, i, callingPid, callingUid);
                    ShortcutInfo si = getShortcutInfoLocked(i, str, str3, str2, i2, getPinnedByAnyLauncher);
                    if (si != null && si.isEnabled()) {
                        if (si.isAlive() || getPinnedByAnyLauncher) {
                            Intent[] intents = si.getIntents();
                            return intents;
                        }
                    }
                    String str4 = ShortcutService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Shortcut ");
                    stringBuilder.append(str2);
                    stringBuilder.append(" does not exist or disabled");
                    Log.e(str4, stringBuilder.toString());
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        public void addListener(ShortcutChangeListener listener) {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.mListeners.add((ShortcutChangeListener) Preconditions.checkNotNull(listener));
            }
        }

        /* JADX WARNING: Missing block: B:13:0x004c, code skipped:
            return r2;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getShortcutIconResId(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Preconditions.checkNotNull(callingPackage, "callingPackage");
            Preconditions.checkNotNull(packageName, "packageName");
            Preconditions.checkNotNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                int i = 0;
                if (p == null) {
                    return 0;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo != null && shortcutInfo.hasIconResource()) {
                    i = shortcutInfo.getIconResourceId();
                }
            }
        }

        /* JADX WARNING: Missing block: B:27:0x0084, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public ParcelFileDescriptor getShortcutIconFd(int launcherUserId, String callingPackage, String packageName, String shortcutId, int userId) {
            Preconditions.checkNotNull(callingPackage, "callingPackage");
            Preconditions.checkNotNull(packageName, "packageName");
            Preconditions.checkNotNull(shortcutId, "shortcutId");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.throwIfUserLockedL(userId);
                ShortcutService.this.throwIfUserLockedL(launcherUserId);
                ShortcutService.this.getLauncherShortcutsLocked(callingPackage, userId, launcherUserId).attemptToRestoreIfNeededAndSave();
                ShortcutPackage p = ShortcutService.this.getUserShortcutsLocked(userId).getPackageShortcutsIfExists(packageName);
                if (p == null) {
                    return null;
                }
                ShortcutInfo shortcutInfo = p.findShortcutById(shortcutId);
                if (shortcutInfo != null) {
                    if (shortcutInfo.hasIconFile()) {
                        String path = ShortcutService.this.mShortcutBitmapSaver.getBitmapPathMayWaitLocked(shortcutInfo);
                        if (path == null) {
                            Slog.w(ShortcutService.TAG, "null bitmap detected in getShortcutIconFd()");
                            return null;
                        }
                        try {
                            ParcelFileDescriptor open = ParcelFileDescriptor.open(new File(path), 268435456);
                            return open;
                        } catch (FileNotFoundException e) {
                            String str = ShortcutService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Icon file not found: ");
                            stringBuilder.append(path);
                            Slog.e(str, stringBuilder.toString());
                            return null;
                        }
                    }
                }
            }
        }

        public boolean hasShortcutHostPermission(int launcherUserId, String callingPackage, int callingPid, int callingUid) {
            return ShortcutService.this.hasShortcutHostPermission(callingPackage, launcherUserId, callingPid, callingUid);
        }

        public void setShortcutHostPackage(String type, String packageName, int userId) {
            ShortcutService.this.setShortcutHostPackage(type, packageName, userId);
        }

        public boolean requestPinAppWidget(String callingPackage, AppWidgetProviderInfo appWidget, Bundle extras, IntentSender resultIntent, int userId) {
            Preconditions.checkNotNull(appWidget);
            return ShortcutService.this.requestPinItem(callingPackage, userId, null, appWidget, extras, resultIntent);
        }

        public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
            return ShortcutService.this.isRequestPinItemSupported(callingUserId, requestType);
        }

        public boolean isForegroundDefaultLauncher(String callingPackage, int callingUid) {
            Preconditions.checkNotNull(callingPackage);
            ComponentName defaultLauncher = ShortcutService.this.getDefaultLauncher(UserHandle.getUserId(callingUid));
            if (defaultLauncher == null || !callingPackage.equals(defaultLauncher.getPackageName())) {
                return false;
            }
            synchronized (ShortcutService.this.mLock) {
                if (ShortcutService.this.isUidForegroundLocked(callingUid)) {
                    return true;
                }
                return false;
            }
        }
    }

    private class MyShellCommand extends ShellCommand {
        private int mUserId;

        private MyShellCommand() {
            this.mUserId = 0;
        }

        /* synthetic */ MyShellCommand(ShortcutService x0, AnonymousClass1 x1) {
            this();
        }

        private void parseOptionsLocked(boolean takeUser) throws CommandException {
            String opt;
            StringBuilder stringBuilder;
            while (true) {
                String nextOption = getNextOption();
                opt = nextOption;
                if (nextOption != null) {
                    Object obj = -1;
                    if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                        obj = null;
                    }
                    if (obj == null && takeUser) {
                        this.mUserId = UserHandle.parseUserArg(getNextArgRequired());
                        if (!ShortcutService.this.isUserUnlockedL(this.mUserId)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("User ");
                            stringBuilder.append(this.mUserId);
                            stringBuilder.append(" is not running or locked");
                            throw new CommandException(stringBuilder.toString());
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown option: ");
                        stringBuilder.append(opt);
                    }
                } else {
                    return;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown option: ");
            stringBuilder.append(opt);
            throw new CommandException(stringBuilder.toString());
        }

        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            int i = -1;
            try {
                switch (cmd.hashCode()) {
                    case -1117067818:
                        if (cmd.equals("verify-states")) {
                            i = 8;
                            break;
                        }
                        break;
                    case -749565587:
                        if (cmd.equals("clear-shortcuts")) {
                            i = 7;
                            break;
                        }
                        break;
                    case -139706031:
                        if (cmd.equals("reset-all-throttling")) {
                            i = 1;
                            break;
                        }
                        break;
                    case -76794781:
                        if (cmd.equals("override-config")) {
                            i = 2;
                            break;
                        }
                        break;
                    case 188791973:
                        if (cmd.equals("reset-throttling")) {
                            i = 0;
                            break;
                        }
                        break;
                    case 237193516:
                        if (cmd.equals("clear-default-launcher")) {
                            i = 4;
                            break;
                        }
                        break;
                    case 1190495043:
                        if (cmd.equals("get-default-launcher")) {
                            i = 5;
                            break;
                        }
                        break;
                    case 1411888601:
                        if (cmd.equals("unload-user")) {
                            i = 6;
                            break;
                        }
                        break;
                    case 1964247424:
                        if (cmd.equals("reset-config")) {
                            i = 3;
                            break;
                        }
                        break;
                    default:
                        break;
                }
                switch (i) {
                    case 0:
                        handleResetThrottling();
                        break;
                    case 1:
                        handleResetAllThrottling();
                        break;
                    case 2:
                        handleOverrideConfig();
                        break;
                    case 3:
                        handleResetConfig();
                        break;
                    case 4:
                        handleClearDefaultLauncher();
                        break;
                    case 5:
                        handleGetDefaultLauncher();
                        break;
                    case 6:
                        handleUnloadUser();
                        break;
                    case 7:
                        handleClearShortcuts();
                        break;
                    case 8:
                        handleVerifyStates();
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
                pw.println("Success");
                return 0;
            } catch (CommandException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error: ");
                stringBuilder.append(e.getMessage());
                pw.println(stringBuilder.toString());
                return 1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Usage: cmd shortcut COMMAND [options ...]");
            pw.println();
            pw.println("cmd shortcut reset-throttling [--user USER_ID]");
            pw.println("    Reset throttling for all packages and users");
            pw.println();
            pw.println("cmd shortcut reset-all-throttling");
            pw.println("    Reset the throttling state for all users");
            pw.println();
            pw.println("cmd shortcut override-config CONFIG");
            pw.println("    Override the configuration for testing (will last until reboot)");
            pw.println();
            pw.println("cmd shortcut reset-config");
            pw.println("    Reset the configuration set with \"update-config\"");
            pw.println();
            pw.println("cmd shortcut clear-default-launcher [--user USER_ID]");
            pw.println("    Clear the cached default launcher");
            pw.println();
            pw.println("cmd shortcut get-default-launcher [--user USER_ID]");
            pw.println("    Show the default launcher");
            pw.println();
            pw.println("cmd shortcut unload-user [--user USER_ID]");
            pw.println("    Unload a user from the memory");
            pw.println("    (This should not affect any observable behavior)");
            pw.println();
            pw.println("cmd shortcut clear-shortcuts [--user USER_ID] PACKAGE");
            pw.println("    Remove all shortcuts from a package, including pinned shortcuts");
            pw.println();
        }

        private void handleResetThrottling() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String str = ShortcutService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cmd: handleResetThrottling: user=");
                stringBuilder.append(this.mUserId);
                Slog.i(str, stringBuilder.toString());
                ShortcutService.this.resetThrottlingInner(this.mUserId);
            }
        }

        private void handleResetAllThrottling() {
            Slog.i(ShortcutService.TAG, "cmd: handleResetAllThrottling");
            ShortcutService.this.resetAllThrottlingInner();
        }

        private void handleOverrideConfig() throws CommandException {
            String config = getNextArgRequired();
            String str = ShortcutService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cmd: handleOverrideConfig: ");
            stringBuilder.append(config);
            Slog.i(str, stringBuilder.toString());
            synchronized (ShortcutService.this.mLock) {
                if (ShortcutService.this.updateConfigurationLocked(config)) {
                } else {
                    throw new CommandException("override-config failed.  See logcat for details.");
                }
            }
        }

        private void handleResetConfig() {
            Slog.i(ShortcutService.TAG, "cmd: handleResetConfig");
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.loadConfigurationLocked();
            }
        }

        private void clearLauncher() {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.getUserShortcutsLocked(this.mUserId).forceClearLauncher();
            }
        }

        private void showLauncher() {
            synchronized (ShortcutService.this.mLock) {
                ShortcutService.this.hasShortcutHostPermissionInner("-", this.mUserId);
                PrintWriter outPrintWriter = getOutPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Launcher: ");
                stringBuilder.append(ShortcutService.this.getUserShortcutsLocked(this.mUserId).getLastKnownLauncher());
                outPrintWriter.println(stringBuilder.toString());
            }
        }

        private void handleClearDefaultLauncher() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                clearLauncher();
            }
        }

        private void handleGetDefaultLauncher() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                clearLauncher();
                showLauncher();
            }
        }

        private void handleUnloadUser() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String str = ShortcutService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cmd: handleUnloadUser: user=");
                stringBuilder.append(this.mUserId);
                Slog.i(str, stringBuilder.toString());
                ShortcutService.this.handleStopUser(this.mUserId);
            }
        }

        private void handleClearShortcuts() throws CommandException {
            synchronized (ShortcutService.this.mLock) {
                parseOptionsLocked(true);
                String packageName = getNextArgRequired();
                String str = ShortcutService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cmd: handleClearShortcuts: user");
                stringBuilder.append(this.mUserId);
                stringBuilder.append(", ");
                stringBuilder.append(packageName);
                Slog.i(str, stringBuilder.toString());
                ShortcutService.this.cleanUpPackageForAllLoadedUsers(packageName, this.mUserId, true);
            }
        }

        private void handleVerifyStates() throws CommandException {
            try {
                ShortcutService.this.verifyStatesForce();
            } catch (Throwable th) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(th.getMessage());
                stringBuilder.append("\n");
                stringBuilder.append(Log.getStackTraceString(th));
                CommandException commandException = new CommandException(stringBuilder.toString());
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ShortcutOperation {
    }

    @VisibleForTesting
    interface Stats {
        public static final int ASYNC_PRELOAD_USER_DELAY = 15;
        public static final int CHECK_LAUNCHER_ACTIVITY = 12;
        public static final int CHECK_PACKAGE_CHANGES = 8;
        public static final int CLEANUP_DANGLING_BITMAPS = 5;
        public static final int COUNT = 17;
        public static final int GET_ACTIVITY_WITH_METADATA = 6;
        public static final int GET_APPLICATION_INFO = 3;
        public static final int GET_APPLICATION_RESOURCES = 9;
        public static final int GET_DEFAULT_HOME = 0;
        public static final int GET_DEFAULT_LAUNCHER = 16;
        public static final int GET_INSTALLED_PACKAGES = 7;
        public static final int GET_LAUNCHER_ACTIVITY = 11;
        public static final int GET_PACKAGE_INFO = 1;
        public static final int GET_PACKAGE_INFO_WITH_SIG = 2;
        public static final int IS_ACTIVITY_ENABLED = 13;
        public static final int LAUNCHER_PERMISSION_CHECK = 4;
        public static final int PACKAGE_UPDATE_CHECK = 14;
        public static final int RESOURCE_NAME_LOOKUP = 10;
    }

    public static final class Lifecycle extends SystemService {
        final ShortcutService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ShortcutService(context);
        }

        public void onStart() {
            publishBinderService(ShortcutService.KEY_SHORTCUT, this.mService);
        }

        public void onBootPhase(int phase) {
            this.mService.onBootPhase(phase);
        }

        public void onStopUser(int userHandle) {
            this.mService.handleStopUser(userHandle);
        }

        public void onUnlockUser(int userId) {
            this.mService.handleUnlockUser(userId);
        }
    }

    static {
        sObtainShortCutPermissionpackageNames.add("com.huawei.recsys");
        sObtainShortCutPermissionpackageNames.add("com.huawei.hiboard");
        sObtainShortCutPermissionpackageNames.add("com.huawei.intelligent");
        sObtainShortCutPermissionpackageNames.add("com.huawei.hiassistant");
    }

    public ShortcutService(Context context) {
        this(context, BackgroundThread.get().getLooper(), false);
    }

    @VisibleForTesting
    ShortcutService(Context context, Looper looper, boolean onlyForPackageManagerApis) {
        this.mLock = new Object();
        this.mListeners = new ArrayList(1);
        this.mUsers = new SparseArray();
        this.mShortcutNonPersistentUsers = new SparseArray();
        this.mUidState = new SparseIntArray();
        this.mUidLastForegroundElapsedTime = new SparseLongArray();
        this.mDirtyUserIds = new ArrayList();
        this.mBootCompleted = new AtomicBoolean();
        this.mUnlockedUsers = new SparseBooleanArray();
        this.mStatLogger = new StatLogger(new String[]{"getHomeActivities()", "Launcher permission check", "getPackageInfo()", "getPackageInfo(SIG)", "getApplicationInfo", "cleanupDanglingBitmaps", "getActivity+metadata", "getInstalledPackages", "checkPackageChanges", "getApplicationResources", "resourceNameLookup", "getLauncherActivity", "checkLauncherActivity", "isActivityEnabled", "packageUpdateCheck", "asyncPreloadUserDelay", "getDefaultLauncher()"});
        this.mWtfCount = 0;
        this.mUidObserver = new IUidObserver.Stub() {
            public void onUidStateChanged(int uid, int procState, long procStateSeq) {
                ShortcutService.this.injectPostToHandler(new -$$Lambda$ShortcutService$3$n_VdEzyBcjs0pGZO8GnB0FoTgR0(this, uid, procState));
            }

            public void onUidGone(int uid, boolean disabled) {
                ShortcutService.this.injectPostToHandler(new -$$Lambda$ShortcutService$3$WghiV-HLnzJqZabObC5uHCmb960(this, uid));
            }

            public void onUidActive(int uid) {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }
        };
        this.mSaveDirtyInfoRunner = new -$$Lambda$jZzCUQd1whVIqs_s1XMLbFqTP_E(this);
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ShortcutService.this.mBootCompleted.get()) {
                    try {
                        if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                            ShortcutService.this.handleLocaleChanged();
                        }
                    } catch (Exception e) {
                        ShortcutService.this.wtf("Exception in mReceiver.onReceive", e);
                    }
                }
            }
        };
        this.mPackageMonitor = new BroadcastReceiver() {
            /* JADX WARNING: Missing block: B:19:0x0051, code skipped:
            if ("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED".equals(r1) == false) goto L_0x0059;
     */
            /* JADX WARNING: Missing block: B:20:0x0053, code skipped:
            r11.this$0.injectRestoreCallingIdentity(r2);
     */
            /* JADX WARNING: Missing block: B:21:0x0058, code skipped:
            return;
     */
            /* JADX WARNING: Missing block: B:23:?, code skipped:
            r4 = r13.getData();
     */
            /* JADX WARNING: Missing block: B:24:0x005d, code skipped:
            if (r4 == null) goto L_0x0064;
     */
            /* JADX WARNING: Missing block: B:25:0x005f, code skipped:
            r5 = r4.getSchemeSpecificPart();
     */
            /* JADX WARNING: Missing block: B:26:0x0064, code skipped:
            r5 = null;
     */
            /* JADX WARNING: Missing block: B:27:0x0065, code skipped:
            if (r5 != null) goto L_0x0083;
     */
            /* JADX WARNING: Missing block: B:28:0x0067, code skipped:
            r6 = com.android.server.pm.ShortcutService.TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Intent broadcast does not contain package name: ");
            r7.append(r13);
            android.util.Slog.w(r6, r7.toString());
     */
            /* JADX WARNING: Missing block: B:29:0x007d, code skipped:
            r11.this$0.injectRestoreCallingIdentity(r2);
     */
            /* JADX WARNING: Missing block: B:30:0x0082, code skipped:
            return;
     */
            /* JADX WARNING: Missing block: B:32:?, code skipped:
            r7 = false;
            r6 = r13.getBooleanExtra("android.intent.extra.REPLACING", false);
            r9 = r1.hashCode();
     */
            /* JADX WARNING: Missing block: B:33:0x0092, code skipped:
            if (r9 == 172491798) goto L_0x00c1;
     */
            /* JADX WARNING: Missing block: B:35:0x0097, code skipped:
            if (r9 == 267468725) goto L_0x00b7;
     */
            /* JADX WARNING: Missing block: B:37:0x009c, code skipped:
            if (r9 == 525384130) goto L_0x00ad;
     */
            /* JADX WARNING: Missing block: B:39:0x00a1, code skipped:
            if (r9 == 1544582882) goto L_0x00a4;
     */
            /* JADX WARNING: Missing block: B:42:0x00aa, code skipped:
            if (r1.equals("android.intent.action.PACKAGE_ADDED") == false) goto L_0x00cb;
     */
            /* JADX WARNING: Missing block: B:45:0x00b3, code skipped:
            if (r1.equals("android.intent.action.PACKAGE_REMOVED") == false) goto L_0x00cb;
     */
            /* JADX WARNING: Missing block: B:46:0x00b5, code skipped:
            r7 = true;
     */
            /* JADX WARNING: Missing block: B:48:0x00bd, code skipped:
            if (r1.equals("android.intent.action.PACKAGE_DATA_CLEARED") == false) goto L_0x00cb;
     */
            /* JADX WARNING: Missing block: B:49:0x00bf, code skipped:
            r7 = true;
     */
            /* JADX WARNING: Missing block: B:51:0x00c7, code skipped:
            if (r1.equals("android.intent.action.PACKAGE_CHANGED") == false) goto L_0x00cb;
     */
            /* JADX WARNING: Missing block: B:52:0x00c9, code skipped:
            r7 = true;
     */
            /* JADX WARNING: Missing block: B:53:0x00cb, code skipped:
            r7 = true;
     */
            /* JADX WARNING: Missing block: B:54:0x00cc, code skipped:
            switch(r7) {
                case 0: goto L_0x00e4;
                case 1: goto L_0x00dc;
                case 2: goto L_0x00d6;
                case 3: goto L_0x00d0;
                default: goto L_0x00cf;
            };
     */
            /* JADX WARNING: Missing block: B:56:0x00d0, code skipped:
            com.android.server.pm.ShortcutService.access$1200(r11.this$0, r5, r0);
     */
            /* JADX WARNING: Missing block: B:57:0x00d6, code skipped:
            com.android.server.pm.ShortcutService.access$1100(r11.this$0, r5, r0);
     */
            /* JADX WARNING: Missing block: B:58:0x00dc, code skipped:
            if (r6 != false) goto L_0x00ff;
     */
            /* JADX WARNING: Missing block: B:59:0x00de, code skipped:
            com.android.server.pm.ShortcutService.access$1000(r11.this$0, r5, r0);
     */
            /* JADX WARNING: Missing block: B:60:0x00e4, code skipped:
            if (r6 == false) goto L_0x00ec;
     */
            /* JADX WARNING: Missing block: B:61:0x00e6, code skipped:
            com.android.server.pm.ShortcutService.access$800(r11.this$0, r5, r0);
     */
            /* JADX WARNING: Missing block: B:62:0x00ec, code skipped:
            com.android.server.pm.ShortcutService.access$900(r11.this$0, r5, r0);
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                long token;
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                String str;
                if (userId == -10000) {
                    str = ShortcutService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Intent broadcast does not contain user handle: ");
                    stringBuilder.append(intent);
                    Slog.w(str, stringBuilder.toString());
                    return;
                }
                str = intent.getAction();
                token = ShortcutService.this.injectClearCallingIdentity();
                try {
                    synchronized (ShortcutService.this.mLock) {
                        if (ShortcutService.this.isUserUnlockedL(userId)) {
                            ShortcutService.this.getUserShortcutsLocked(userId).clearLauncher();
                        } else {
                            ShortcutService.this.injectRestoreCallingIdentity(token);
                            return;
                        }
                    }
                } catch (Exception e) {
                    try {
                        ShortcutService.this.wtf("Exception in mPackageMonitor.onReceive", e);
                    } catch (Throwable th) {
                        ShortcutService.this.injectRestoreCallingIdentity(token);
                    }
                }
                ShortcutService.this.injectRestoreCallingIdentity(token);
            }
        };
        this.mContext = (Context) Preconditions.checkNotNull(context);
        LocalServices.addService(ShortcutServiceInternal.class, new LocalService(this, null));
        this.mHandler = new Handler(looper);
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mPackageManagerInternal = (PackageManagerInternal) Preconditions.checkNotNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        this.mUserManagerInternal = (UserManagerInternal) Preconditions.checkNotNull((UserManagerInternal) LocalServices.getService(UserManagerInternal.class));
        this.mUsageStatsManagerInternal = (UsageStatsManagerInternal) Preconditions.checkNotNull((UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class));
        this.mActivityManagerInternal = (ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class));
        this.mShortcutRequestPinProcessor = new ShortcutRequestPinProcessor(this, this.mLock);
        this.mShortcutBitmapSaver = new ShortcutBitmapSaver(this);
        this.mShortcutDumpFiles = new ShortcutDumpFiles(this);
        if (!onlyForPackageManagerApis) {
            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
            packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            packageFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
            packageFilter.addDataScheme("package");
            packageFilter.setPriority(1000);
            this.mContext.registerReceiverAsUser(this.mPackageMonitor, UserHandle.ALL, packageFilter, null, this.mHandler);
            IntentFilter preferedActivityFilter = new IntentFilter();
            preferedActivityFilter.addAction("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
            preferedActivityFilter.setPriority(1000);
            this.mContext.registerReceiverAsUser(this.mPackageMonitor, UserHandle.ALL, preferedActivityFilter, null, this.mHandler);
            IntentFilter localeFilter = new IntentFilter();
            localeFilter.addAction("android.intent.action.LOCALE_CHANGED");
            localeFilter.setPriority(1000);
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, localeFilter, null, this.mHandler);
            injectRegisterUidObserver(this.mUidObserver, 3);
        }
    }

    long getStatStartTime() {
        return this.mStatLogger.getTime();
    }

    void logDurationStat(int statId, long start) {
        this.mStatLogger.logDurationStat(statId, start);
    }

    public String injectGetLocaleTagsForUser(int userId) {
        return LocaleList.getDefault().toLanguageTags();
    }

    void handleOnUidStateChanged(int uid, int procState) {
        synchronized (this.mLock) {
            this.mUidState.put(uid, procState);
            if (isProcessStateForeground(procState)) {
                this.mUidLastForegroundElapsedTime.put(uid, injectElapsedRealtime());
            }
        }
    }

    private boolean isProcessStateForeground(int processState) {
        return processState <= 4;
    }

    @GuardedBy("mLock")
    boolean isUidForegroundLocked(int uid) {
        if (uid == 1000 || isProcessStateForeground(this.mUidState.get(uid, 19))) {
            return true;
        }
        return isProcessStateForeground(this.mActivityManagerInternal.getUidProcessState(uid));
    }

    @GuardedBy("mLock")
    long getUidLastForegroundElapsedTimeLocked(int uid) {
        return this.mUidLastForegroundElapsedTime.get(uid);
    }

    void onBootPhase(int phase) {
        if (phase == 480) {
            initialize();
        } else if (phase == 1000) {
            this.mBootCompleted.set(true);
        }
    }

    void handleUnlockUser(int userId) {
        synchronized (this.mUnlockedUsers) {
            this.mUnlockedUsers.put(userId, true);
        }
        injectRunOnNewThread(new -$$Lambda$ShortcutService$QFWliMhWloedhnaZCwVKaqKPVb4(this, getStatStartTime(), userId));
    }

    public static /* synthetic */ void lambda$handleUnlockUser$0(ShortcutService shortcutService, long start, int userId) {
        synchronized (shortcutService.mLock) {
            shortcutService.logDurationStat(15, start);
            shortcutService.getUserShortcutsLocked(userId);
        }
    }

    void handleStopUser(int userId) {
        synchronized (this.mLock) {
            unloadUserLocked(userId);
            synchronized (this.mUnlockedUsers) {
                this.mUnlockedUsers.put(userId, false);
            }
        }
    }

    @GuardedBy("mLock")
    private void unloadUserLocked(int userId) {
        saveDirtyInfo();
        this.mUsers.delete(userId);
    }

    private AtomicFile getBaseStateFile() {
        File path = new File(injectSystemDataPath(), FILENAME_BASE_STATE);
        path.mkdirs();
        return new AtomicFile(path);
    }

    private void initialize() {
        synchronized (this.mLock) {
            loadConfigurationLocked();
            loadBaseStateLocked();
        }
    }

    private void loadConfigurationLocked() {
        updateConfigurationLocked(injectShortcutManagerConstants());
    }

    @VisibleForTesting
    boolean updateConfigurationLocked(String config) {
        int i;
        boolean result = true;
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(config);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Bad shortcut manager settings", e);
            result = false;
        }
        this.mSaveDelayMillis = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_SAVE_DELAY_MILLIS, 3000));
        this.mResetInterval = Math.max(1, parser.getLong(ConfigConstants.KEY_RESET_INTERVAL_SEC, DEFAULT_RESET_INTERVAL_SEC) * 1000);
        this.mMaxUpdatesPerInterval = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_UPDATES_PER_INTERVAL, 10));
        this.mMaxShortcuts = Math.max(0, (int) parser.getLong(ConfigConstants.KEY_MAX_SHORTCUTS, 5));
        if (injectIsLowRamDevice()) {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP_LOWRAM, 48);
        } else {
            i = (int) parser.getLong(ConfigConstants.KEY_MAX_ICON_DIMENSION_DP, 96);
        }
        this.mMaxIconDimension = injectDipToPixel(Math.max(1, i));
        this.mIconPersistFormat = CompressFormat.valueOf(parser.getString(ConfigConstants.KEY_ICON_FORMAT, DEFAULT_ICON_PERSIST_FORMAT));
        this.mIconPersistQuality = (int) parser.getLong(ConfigConstants.KEY_ICON_QUALITY, 100);
        return result;
    }

    @VisibleForTesting
    String injectShortcutManagerConstants() {
        return Global.getString(this.mContext.getContentResolver(), "shortcut_manager_constants");
    }

    @VisibleForTesting
    int injectDipToPixel(int dip) {
        return (int) TypedValue.applyDimension(1, (float) dip, this.mContext.getResources().getDisplayMetrics());
    }

    static String parseStringAttribute(XmlPullParser parser, String attribute) {
        return parser.getAttributeValue(null, attribute);
    }

    static boolean parseBooleanAttribute(XmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute) == 1;
    }

    static boolean parseBooleanAttribute(XmlPullParser parser, String attribute, boolean def) {
        return parseLongAttribute(parser, attribute, (long) def) == 1;
    }

    static int parseIntAttribute(XmlPullParser parser, String attribute) {
        return (int) parseLongAttribute(parser, attribute);
    }

    static int parseIntAttribute(XmlPullParser parser, String attribute, int def) {
        return (int) parseLongAttribute(parser, attribute, (long) def);
    }

    static long parseLongAttribute(XmlPullParser parser, String attribute) {
        return parseLongAttribute(parser, attribute, 0);
    }

    static long parseLongAttribute(XmlPullParser parser, String attribute, long def) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return def;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error parsing long ");
            stringBuilder.append(value);
            Slog.e(str, stringBuilder.toString());
            return def;
        }
    }

    static ComponentName parseComponentNameAttribute(XmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return ComponentName.unflattenFromString(value);
    }

    static Intent parseIntentAttributeNoDefault(XmlPullParser parser, String attribute) {
        String value = parseStringAttribute(parser, attribute);
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Intent.parseUri(value, 0);
        } catch (URISyntaxException e) {
            Slog.e(TAG, "Error parsing intent", e);
            return null;
        }
    }

    static Intent parseIntentAttribute(XmlPullParser parser, String attribute) {
        Intent parsed = parseIntentAttributeNoDefault(parser, attribute);
        if (parsed == null) {
            return new Intent("android.intent.action.VIEW");
        }
        return parsed;
    }

    static void writeTagValue(XmlSerializer out, String tag, String value) throws IOException {
        if (!TextUtils.isEmpty(value)) {
            out.startTag(null, tag);
            out.attribute(null, ATTR_VALUE, value);
            out.endTag(null, tag);
        }
    }

    static void writeTagValue(XmlSerializer out, String tag, long value) throws IOException {
        writeTagValue(out, tag, Long.toString(value));
    }

    static void writeTagValue(XmlSerializer out, String tag, ComponentName name) throws IOException {
        if (name != null) {
            writeTagValue(out, tag, name.flattenToString());
        }
    }

    static void writeTagExtra(XmlSerializer out, String tag, PersistableBundle bundle) throws IOException, XmlPullParserException {
        if (bundle != null) {
            out.startTag(null, tag);
            bundle.saveToXml(out);
            out.endTag(null, tag);
        }
    }

    static void writeAttr(XmlSerializer out, String name, CharSequence value) throws IOException {
        if (!TextUtils.isEmpty(value)) {
            out.attribute(null, name, value.toString());
        }
    }

    static void writeAttr(XmlSerializer out, String name, long value) throws IOException {
        writeAttr(out, name, String.valueOf(value));
    }

    static void writeAttr(XmlSerializer out, String name, boolean value) throws IOException {
        if (value) {
            writeAttr(out, name, (CharSequence) "1");
        } else {
            writeAttr(out, name, (CharSequence) "0");
        }
    }

    static void writeAttr(XmlSerializer out, String name, ComponentName comp) throws IOException {
        if (comp != null) {
            writeAttr(out, name, comp.flattenToString());
        }
    }

    static void writeAttr(XmlSerializer out, String name, Intent intent) throws IOException {
        if (intent != null) {
            writeAttr(out, name, intent.toUri(0));
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void saveBaseStateLocked() {
        AtomicFile file = getBaseStateFile();
        FileOutputStream outs = null;
        try {
            outs = file.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(outs, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_ROOT);
            writeTagValue(out, TAG_LAST_RESET_TIME, this.mRawLastResetTime);
            out.endTag(null, TAG_ROOT);
            out.endDocument();
            file.finishWrite(outs);
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write to file ");
            stringBuilder.append(file.getBaseFile());
            Slog.e(str, stringBuilder.toString(), e);
            file.failWrite(outs);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x00a9 A:{ExcHandler: IOException | XmlPullParserException (r3_1 'e' java.lang.Exception), Splitter:B:1:0x0008} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:15:0x0039, code skipped:
            r8 = TAG;
            r10 = new java.lang.StringBuilder();
            r10.append("Invalid root tag: ");
            r10.append(r9);
            android.util.Slog.e(r8, r10.toString());
     */
    /* JADX WARNING: Missing block: B:16:0x004f, code skipped:
            if (r3 == null) goto L_0x0054;
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:19:0x0054, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:32:0x008e, code skipped:
            if (r3 == null) goto L_0x00c9;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:43:0x00a0, code skipped:
            r6 = move-exception;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            r4.addSuppressed(r6);
     */
    /* JADX WARNING: Missing block: B:48:0x00a9, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:49:0x00aa, code skipped:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Failed to read file ");
            r5.append(r2.getBaseFile());
            android.util.Slog.e(r4, r5.toString(), r3);
            r12.mRawLastResetTime = 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mLock")
    private void loadBaseStateLocked() {
        this.mRawLastResetTime = 0;
        AtomicFile file = getBaseStateFile();
        FileInputStream in;
        try {
            in = file.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                int type = next;
                if (next == 1) {
                    break;
                } else if (type == 2) {
                    next = parser.getDepth();
                    String tag = parser.getName();
                    if (next != 1) {
                        Object obj = -1;
                        if (tag.hashCode() == -68726522) {
                            if (tag.equals(TAG_LAST_RESET_TIME)) {
                                obj = null;
                            }
                        }
                        if (obj != null) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid tag: ");
                            stringBuilder.append(tag);
                            Slog.e(str, stringBuilder.toString());
                        } else {
                            this.mRawLastResetTime = parseLongAttribute(parser, ATTR_VALUE);
                        }
                    } else if (!TAG_ROOT.equals(tag)) {
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException | XmlPullParserException e2) {
        } catch (Throwable th) {
            if (in != null) {
                if (r4 != null) {
                    in.close();
                } else {
                    in.close();
                }
            }
        }
        getLastResetTimeLocked();
    }

    @VisibleForTesting
    final File getUserFile(int userId) {
        return new File(injectUserDataPath(userId), FILENAME_USER_PACKAGES);
    }

    @GuardedBy("mLock")
    private void saveUserLocked(int userId) {
        if (this.mUserManagerInternal.isRemovingUser(userId)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("user ");
            stringBuilder.append(userId);
            stringBuilder.append(" is removing, save shortcut info return.");
            Slog.i(str, stringBuilder.toString());
            return;
        }
        File path = getUserFile(userId);
        this.mShortcutBitmapSaver.waitForAllSavesLocked();
        path.getParentFile().mkdirs();
        AtomicFile file = new AtomicFile(path);
        FileOutputStream os = null;
        try {
            os = file.startWrite();
            saveUserInternalLocked(userId, os, false);
            file.finishWrite(os);
            cleanupDanglingBitmapDirectoriesLocked(userId);
        } catch (IOException | XmlPullParserException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to write to file ");
            stringBuilder2.append(file.getBaseFile());
            Slog.e(str2, stringBuilder2.toString(), e);
            file.failWrite(os);
        }
    }

    @GuardedBy("mLock")
    private void saveUserInternalLocked(int userId, OutputStream os, boolean forBackup) throws IOException, XmlPullParserException {
        BufferedOutputStream bos = new BufferedOutputStream(os);
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(bos, StandardCharsets.UTF_8.name());
        out.startDocument(null, Boolean.valueOf(true));
        getUserShortcutsLocked(userId).saveToXml(out, forBackup);
        out.endDocument();
        bos.flush();
        os.flush();
    }

    static IOException throwForInvalidTag(int depth, String tag) throws IOException {
        throw new IOException(String.format("Invalid tag '%s' found at depth %d", new Object[]{tag, Integer.valueOf(depth)}));
    }

    static void warnForInvalidTag(int depth, String tag) throws IOException {
        Slog.w(TAG, String.format("Invalid tag '%s' found at depth %d", new Object[]{tag, Integer.valueOf(depth)}));
    }

    private ShortcutUser loadUserLocked(int userId) {
        AtomicFile file = new AtomicFile(getUserFile(userId));
        ShortcutUser ret = null;
        try {
            FileInputStream in = file.openRead();
            try {
                ret = loadUserInternal(userId, in, false);
                return ret;
            } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read file ");
                stringBuilder.append(file.getBaseFile());
                Slog.e(str, stringBuilder.toString(), e);
                return ret;
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            return null;
        }
    }

    private ShortcutUser loadUserInternal(int userId, InputStream is, boolean fromBackup) throws XmlPullParserException, IOException, InvalidFileFormatException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ShortcutUser ret = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(bis, StandardCharsets.UTF_8.name());
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return ret;
            }
            if (type == 2) {
                next = parser.getDepth();
                String tag = parser.getName();
                if (next == 1 && "user".equals(tag)) {
                    ret = ShortcutUser.loadFromXml(this, parser, userId, fromBackup);
                } else {
                    throwForInvalidTag(next, tag);
                }
            }
        }
    }

    private void scheduleSaveBaseState() {
        scheduleSaveInner(-10000);
    }

    void scheduleSaveUser(int userId) {
        scheduleSaveInner(userId);
    }

    private void scheduleSaveInner(int userId) {
        synchronized (this.mLock) {
            if (!this.mDirtyUserIds.contains(Integer.valueOf(userId))) {
                this.mDirtyUserIds.add(Integer.valueOf(userId));
            }
        }
        this.mHandler.removeCallbacks(this.mSaveDirtyInfoRunner);
        this.mHandler.postDelayed(this.mSaveDirtyInfoRunner, (long) this.mSaveDelayMillis);
    }

    @VisibleForTesting
    void saveDirtyInfo() {
        try {
            synchronized (this.mLock) {
                for (int i = this.mDirtyUserIds.size() - 1; i >= 0; i--) {
                    int userId = ((Integer) this.mDirtyUserIds.get(i)).intValue();
                    if (userId == -10000) {
                        saveBaseStateLocked();
                    } else {
                        saveUserLocked(userId);
                    }
                }
                this.mDirtyUserIds.clear();
            }
        } catch (Exception e) {
            wtf("Exception in saveDirtyInfo", e);
        }
    }

    @GuardedBy("mLock")
    long getLastResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime;
    }

    @GuardedBy("mLock")
    long getNextResetTimeLocked() {
        updateTimesLocked();
        return this.mRawLastResetTime + this.mResetInterval;
    }

    static boolean isClockValid(long time) {
        return time >= 1420070400;
    }

    @GuardedBy("mLock")
    private void updateTimesLocked() {
        long now = injectCurrentTimeMillis();
        long prevLastResetTime = this.mRawLastResetTime;
        if (this.mRawLastResetTime == 0) {
            this.mRawLastResetTime = now;
        } else if (now < this.mRawLastResetTime) {
            if (isClockValid(now)) {
                Slog.w(TAG, "Clock rewound");
                this.mRawLastResetTime = now;
            }
        } else if (this.mRawLastResetTime + this.mResetInterval <= now) {
            this.mRawLastResetTime = ((now / this.mResetInterval) * this.mResetInterval) + (this.mRawLastResetTime % this.mResetInterval);
        }
        if (prevLastResetTime != this.mRawLastResetTime) {
            scheduleSaveBaseState();
        }
    }

    protected boolean isUserUnlockedL(int userId) {
        synchronized (this.mUnlockedUsers) {
            if (this.mUnlockedUsers.get(userId)) {
                return true;
            }
            return this.mUserManagerInternal.isUserUnlockingOrUnlocked(userId);
        }
    }

    void throwIfUserLockedL(int userId) {
        if (!isUserUnlockedL(userId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User ");
            stringBuilder.append(userId);
            stringBuilder.append(" is locked or not running");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    @GuardedBy("mLock")
    private boolean isUserLoadedLocked(int userId) {
        return this.mUsers.get(userId) != null;
    }

    @GuardedBy("mLock")
    ShortcutUser getUserShortcutsLocked(int userId) {
        if (!isUserUnlockedL(userId)) {
            wtf("User still locked");
        }
        ShortcutUser userPackages = (ShortcutUser) this.mUsers.get(userId);
        if (userPackages == null) {
            userPackages = loadUserLocked(userId);
            if (userPackages == null) {
                userPackages = new ShortcutUser(this, userId);
            }
            this.mUsers.put(userId, userPackages);
            checkPackageChanges(userId);
        }
        return userPackages;
    }

    @GuardedBy("mLock")
    ShortcutNonPersistentUser getNonPersistentUserLocked(int userId) {
        ShortcutNonPersistentUser ret = (ShortcutNonPersistentUser) this.mShortcutNonPersistentUsers.get(userId);
        if (ret != null) {
            return ret;
        }
        ret = new ShortcutNonPersistentUser(this, userId);
        this.mShortcutNonPersistentUsers.put(userId, ret);
        return ret;
    }

    @GuardedBy("mLock")
    void forEachLoadedUserLocked(Consumer<ShortcutUser> c) {
        for (int i = this.mUsers.size() - 1; i >= 0; i--) {
            c.accept((ShortcutUser) this.mUsers.valueAt(i));
        }
    }

    @GuardedBy("mLock")
    ShortcutPackage getPackageShortcutsLocked(String packageName, int userId) {
        return getUserShortcutsLocked(userId).getPackageShortcuts(packageName);
    }

    @GuardedBy("mLock")
    ShortcutPackage getPackageShortcutsForPublisherLocked(String packageName, int userId) {
        ShortcutPackage ret = getUserShortcutsLocked(userId).getPackageShortcuts(packageName);
        ret.getUser().onCalledByPublisher(packageName);
        return ret;
    }

    @GuardedBy("mLock")
    ShortcutLauncher getLauncherShortcutsLocked(String packageName, int ownerUserId, int launcherUserId) {
        return getUserShortcutsLocked(ownerUserId).getLauncherShortcuts(packageName, launcherUserId);
    }

    void removeIconLocked(ShortcutInfo shortcut) {
        this.mShortcutBitmapSaver.removeIcon(shortcut);
    }

    public void cleanupBitmapsForPackage(int userId, String packageName) {
        File packagePath = new File(getUserBitmapFilePath(userId), packageName);
        if (packagePath.isDirectory()) {
            if (!(FileUtils.deleteContents(packagePath) && packagePath.delete())) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to remove directory ");
                stringBuilder.append(packagePath);
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    @GuardedBy("mLock")
    private void cleanupDanglingBitmapDirectoriesLocked(int userId) {
        long start = getStatStartTime();
        ShortcutUser user = getUserShortcutsLocked(userId);
        File[] children = getUserBitmapFilePath(userId).listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    String packageName = child.getName();
                    if (user.hasPackage(packageName)) {
                        cleanupDanglingBitmapFilesLocked(userId, user, packageName, child);
                    } else {
                        cleanupBitmapsForPackage(userId, packageName);
                    }
                }
            }
            logDurationStat(5, start);
        }
    }

    private void cleanupDanglingBitmapFilesLocked(int userId, ShortcutUser user, String packageName, File path) {
        ArraySet<String> usedFiles = user.getPackageShortcuts(packageName).getUsedBitmapFiles();
        for (File child : path.listFiles()) {
            if (child.isFile() && !usedFiles.contains(child.getName())) {
                child.delete();
            }
        }
    }

    FileOutputStreamWithPath openIconFileForWrite(int userId, ShortcutInfo shortcut) throws IOException {
        File packagePath = new File(getUserBitmapFilePath(userId), shortcut.getPackage());
        if (!packagePath.isDirectory()) {
            packagePath.mkdirs();
            if (packagePath.isDirectory()) {
                SELinux.restorecon(packagePath);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create directory ");
                stringBuilder.append(packagePath);
                throw new IOException(stringBuilder.toString());
            }
        }
        String baseName = String.valueOf(injectCurrentTimeMillis());
        int suffix = 0;
        while (true) {
            String str;
            String filename = new StringBuilder();
            if (suffix == 0) {
                str = baseName;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(baseName);
                stringBuilder2.append("_");
                stringBuilder2.append(suffix);
                str = stringBuilder2.toString();
            }
            filename.append(str);
            filename.append(".png");
            File file = new File(packagePath, filename.toString());
            if (!file.exists()) {
                return new FileOutputStreamWithPath(file);
            }
            suffix++;
        }
    }

    void saveIconAndFixUpShortcutLocked(ShortcutInfo shortcut) {
        if (!shortcut.hasIconFile() && !shortcut.hasIconResource()) {
            long token = injectClearCallingIdentity();
            try {
                removeIconLocked(shortcut);
                Icon icon = shortcut.getIcon();
                if (icon == null) {
                    injectRestoreCallingIdentity(token);
                    return;
                }
                int maxIconDimension = this.mMaxIconDimension;
                int type = icon.getType();
                Bitmap bitmap;
                if (type != 5) {
                    switch (type) {
                        case 1:
                            bitmap = icon.getBitmap();
                            break;
                        case 2:
                            injectValidateIconResPackage(shortcut, icon);
                            shortcut.setIconResourceId(icon.getResId());
                            shortcut.addFlags(4);
                            shortcut.clearIcon();
                            injectRestoreCallingIdentity(token);
                            return;
                        default:
                            throw ShortcutInfo.getInvalidIconException();
                    }
                }
                bitmap = icon.getBitmap();
                maxIconDimension = (int) (((float) maxIconDimension) * (1.0f + (2.0f * AdaptiveIconDrawable.getExtraInsetFraction())));
                this.mShortcutBitmapSaver.saveBitmapLocked(shortcut, maxIconDimension, this.mIconPersistFormat, this.mIconPersistQuality);
                shortcut.clearIcon();
                injectRestoreCallingIdentity(token);
            } catch (Throwable th) {
                injectRestoreCallingIdentity(token);
            }
        }
    }

    void injectValidateIconResPackage(ShortcutInfo shortcut, Icon icon) {
        if (!shortcut.getPackage().equals(icon.getResPackage())) {
            throw new IllegalArgumentException("Icon resource must reside in shortcut owner package");
        }
    }

    static Bitmap shrinkBitmap(Bitmap in, int maxSize) {
        int ow = in.getWidth();
        int oh = in.getHeight();
        if (ow <= maxSize && oh <= maxSize) {
            return in;
        }
        int longerDimension = Math.max(ow, oh);
        int nw = (ow * maxSize) / longerDimension;
        int nh = (oh * maxSize) / longerDimension;
        Bitmap scaledBitmap = Bitmap.createBitmap(nw, nh, Config.ARGB_8888);
        new Canvas(scaledBitmap).drawBitmap(in, null, new RectF(0.0f, 0.0f, (float) nw, (float) nh), null);
        return scaledBitmap;
    }

    void fixUpShortcutResourceNamesAndValues(ShortcutInfo si) {
        Resources publisherRes = injectGetResourcesForApplicationAsUser(si.getPackage(), si.getUserId());
        if (publisherRes != null) {
            long start = getStatStartTime();
            try {
                si.lookupAndFillInResourceNames(publisherRes);
                si.resolveResourceStrings(publisherRes);
            } finally {
                logDurationStat(10, start);
            }
        }
    }

    private boolean isCallerSystem() {
        return UserHandle.isSameApp(injectBinderCallingUid(), 1000);
    }

    private boolean isCallerShell() {
        int callingUid = injectBinderCallingUid();
        return callingUid == IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME || callingUid == 0;
    }

    private void enforceSystemOrShell() {
        if (!isCallerSystem() && !isCallerShell()) {
            throw new SecurityException("Caller must be system or shell");
        }
    }

    private void enforceShell() {
        if (!isCallerShell()) {
            throw new SecurityException("Caller must be shell");
        }
    }

    private void enforceSystem() {
        if (!isCallerSystem()) {
            throw new SecurityException("Caller must be system");
        }
    }

    private void enforceResetThrottlingPermission() {
        if (!isCallerSystem()) {
            enforceCallingOrSelfPermission("android.permission.RESET_SHORTCUT_MANAGER_THROTTLING", null);
        }
    }

    private void enforceCallingOrSelfPermission(String permission, String message) {
        if (!isCallerSystem()) {
            injectEnforceCallingPermission(permission, message);
        }
    }

    @VisibleForTesting
    void injectEnforceCallingPermission(String permission, String message) {
        this.mContext.enforceCallingPermission(permission, message);
    }

    private void verifyCaller(String packageName, int userId) {
        Preconditions.checkStringNotEmpty(packageName, "packageName");
        if (!isCallerSystem()) {
            int callingUid = injectBinderCallingUid();
            if (UserHandle.getUserId(callingUid) != userId) {
                throw new SecurityException("Invalid user-ID");
            } else if (injectGetPackageUid(packageName, userId) == callingUid) {
                Preconditions.checkState(isEphemeralApp(packageName, userId) ^ 1, "Ephemeral apps can't use ShortcutManager");
            } else {
                throw new SecurityException("Calling package name mismatch");
            }
        }
    }

    private void verifyShortcutInfoPackage(String callerPackage, ShortcutInfo si) {
        if (si != null && !Objects.equals(callerPackage, si.getPackage())) {
            EventLog.writeEvent(1397638484, new Object[]{"109824443", Integer.valueOf(-1), BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS});
            throw new SecurityException("Shortcut package name mismatch");
        }
    }

    private void verifyShortcutInfoPackages(String callerPackage, List<ShortcutInfo> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            verifyShortcutInfoPackage(callerPackage, (ShortcutInfo) list.get(i));
        }
    }

    void injectPostToHandler(Runnable r) {
        this.mHandler.post(r);
    }

    void injectRunOnNewThread(Runnable r) {
        new Thread(r).start();
    }

    void enforceMaxActivityShortcuts(int numShortcuts) {
        if (numShortcuts > this.mMaxShortcuts) {
            throw new IllegalArgumentException("Max number of dynamic shortcuts exceeded");
        }
    }

    int getMaxActivityShortcuts() {
        return this.mMaxShortcuts;
    }

    void packageShortcutsChanged(String packageName, int userId) {
        notifyListeners(packageName, userId);
        scheduleSaveUser(userId);
    }

    private void notifyListeners(String packageName, int userId) {
        injectPostToHandler(new -$$Lambda$ShortcutService$DzwraUeMWDwA0XDfFxd3sGOsA0E(this, userId, packageName));
    }

    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r0 = r1.size() - 1;
     */
    /* JADX WARNING: Missing block: B:12:0x0019, code skipped:
            if (r0 < 0) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:13:0x001b, code skipped:
            ((android.content.pm.ShortcutServiceInternal.ShortcutChangeListener) r1.get(r0)).onShortcutChanged(r5, r4);
     */
    /* JADX WARNING: Missing block: B:14:0x0024, code skipped:
            r0 = r0 - 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static /* synthetic */ void lambda$notifyListeners$1(ShortcutService shortcutService, int userId, String packageName) {
        try {
            synchronized (shortcutService.mLock) {
                if (shortcutService.isUserUnlockedL(userId)) {
                    ArrayList<ShortcutChangeListener> copy = new ArrayList(shortcutService.mListeners);
                }
            }
        } catch (Exception e) {
        }
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcut, boolean forUpdate, boolean forPinRequest) {
        if (shortcut.isReturnedByServer()) {
            Log.w(TAG, "Re-publishing ShortcutInfo returned by server is not supported. Some information such as icon may lost from shortcut.");
        }
        Preconditions.checkNotNull(shortcut, "Null shortcut detected");
        if (shortcut.getActivity() != null) {
            boolean equals = shortcut.getPackage().equals(shortcut.getActivity().getPackageName());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot publish shortcut: activity ");
            stringBuilder.append(shortcut.getActivity());
            stringBuilder.append(" does not belong to package ");
            stringBuilder.append(shortcut.getPackage());
            Preconditions.checkState(equals, stringBuilder.toString());
            equals = injectIsMainActivity(shortcut.getActivity(), shortcut.getUserId());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot publish shortcut: activity ");
            stringBuilder.append(shortcut.getActivity());
            stringBuilder.append(" is not main activity");
            Preconditions.checkState(equals, stringBuilder.toString());
        }
        if (!forUpdate) {
            shortcut.enforceMandatoryFields(forPinRequest);
            if (!forPinRequest) {
                Preconditions.checkState(shortcut.getActivity() != null, "Cannot publish shortcut: target activity is not set");
            }
        }
        if (shortcut.getIcon() != null) {
            ShortcutInfo.validateIcon(shortcut.getIcon());
        }
        shortcut.replaceFlags(0);
    }

    private void fixUpIncomingShortcutInfo(ShortcutInfo shortcut, boolean forUpdate) {
        fixUpIncomingShortcutInfo(shortcut, forUpdate, false);
    }

    public void validateShortcutForPinRequest(ShortcutInfo shortcut) {
        fixUpIncomingShortcutInfo(shortcut, false, true);
    }

    private void fillInDefaultActivity(List<ShortcutInfo> shortcuts) {
        ComponentName defaultActivity = null;
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) shortcuts.get(i);
            if (si.getActivity() == null) {
                if (defaultActivity == null) {
                    defaultActivity = injectGetDefaultMainActivity(si.getPackage(), si.getUserId());
                    boolean z = defaultActivity != null;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Launcher activity not found for package ");
                    stringBuilder.append(si.getPackage());
                    Preconditions.checkState(z, stringBuilder.toString());
                }
                si.setActivity(defaultActivity);
            }
        }
    }

    private void assignImplicitRanks(List<ShortcutInfo> shortcuts) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) shortcuts.get(i)).setImplicitRank(i);
        }
    }

    private List<ShortcutInfo> setReturnedByServer(List<ShortcutInfo> shortcuts) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) shortcuts.get(i)).setReturnedByServer();
        }
        return shortcuts;
    }

    public boolean setDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(packageName, newShortcuts);
        int size = newShortcuts.size();
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            fillInDefaultActivity(newShortcuts);
            int i = 0;
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 0);
            if (ps.tryApiCall(unlimited)) {
                ps.clearAllImplicitRanks();
                assignImplicitRanks(newShortcuts);
                for (int i2 = 0; i2 < size; i2++) {
                    fixUpIncomingShortcutInfo((ShortcutInfo) newShortcuts.get(i2), false);
                }
                ps.deleteAllDynamicShortcuts(true);
                while (i < size) {
                    ps.addOrReplaceDynamicShortcut((ShortcutInfo) newShortcuts.get(i));
                    i++;
                }
                ps.adjustRanks();
                packageShortcutsChanged(packageName, userId);
                verifyStates();
                return true;
            }
            return false;
        }
    }

    public boolean updateShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        String str = packageName;
        int i = userId;
        verifyCaller(str, i);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(str, newShortcuts);
        int size = newShortcuts.size();
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(str, i);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 2);
            if (ps.tryApiCall(unlimited)) {
                ps.clearAllImplicitRanks();
                assignImplicitRanks(newShortcuts);
                for (int i2 = 0; i2 < size; i2++) {
                    ShortcutInfo source = (ShortcutInfo) newShortcuts.get(i2);
                    fixUpIncomingShortcutInfo(source, true);
                    ShortcutInfo target = ps.findShortcutById(source.getId());
                    if (target != null) {
                        if (target.isVisibleToPublisher()) {
                            if (target.isEnabled() != source.isEnabled()) {
                                Slog.w(TAG, "ShortcutInfo.enabled cannot be changed with updateShortcuts()");
                            }
                            if (source.hasRank()) {
                                target.setRankChanged();
                                target.setImplicitRank(source.getImplicitRank());
                            }
                            boolean replacingIcon = source.getIcon() != null;
                            if (replacingIcon) {
                                removeIconLocked(target);
                            }
                            target.copyNonNullFieldsFrom(source);
                            target.setTimestamp(injectCurrentTimeMillis());
                            if (replacingIcon) {
                                saveIconAndFixUpShortcutLocked(target);
                            }
                            if (replacingIcon || source.hasStringResources()) {
                                fixUpShortcutResourceNamesAndValues(target);
                            }
                        }
                    }
                }
                ps.adjustRanks();
                packageShortcutsChanged(str, i);
                verifyStates();
                return true;
            }
            return false;
        }
    }

    public boolean addDynamicShortcuts(String packageName, ParceledListSlice shortcutInfoList, int userId) {
        verifyCaller(packageName, userId);
        List<ShortcutInfo> newShortcuts = shortcutInfoList.getList();
        verifyShortcutInfoPackages(packageName, newShortcuts);
        int size = newShortcuts.size();
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncluded(newShortcuts, true);
            fillInDefaultActivity(newShortcuts);
            ps.enforceShortcutCountsBeforeOperation(newShortcuts, 1);
            ps.clearAllImplicitRanks();
            assignImplicitRanks(newShortcuts);
            if (ps.tryApiCall(unlimited)) {
                for (int i = 0; i < size; i++) {
                    ShortcutInfo newShortcut = (ShortcutInfo) newShortcuts.get(i);
                    fixUpIncomingShortcutInfo(newShortcut, false);
                    newShortcut.setRankChanged();
                    ps.addOrReplaceDynamicShortcut(newShortcut);
                }
                ps.adjustRanks();
                packageShortcutsChanged(packageName, userId);
                verifyStates();
                return true;
            }
            return false;
        }
    }

    public boolean requestPinShortcut(String packageName, ShortcutInfo shortcut, IntentSender resultIntent, int userId) {
        Preconditions.checkNotNull(shortcut);
        Preconditions.checkArgument(shortcut.isEnabled(), "Shortcut must be enabled");
        return requestPinItem(packageName, userId, shortcut, null, null, resultIntent);
    }

    public Intent createShortcutResultIntent(String packageName, ShortcutInfo shortcut, int userId) throws RemoteException {
        Intent ret;
        Preconditions.checkNotNull(shortcut);
        Preconditions.checkArgument(shortcut.isEnabled(), "Shortcut must be enabled");
        verifyCaller(packageName, userId);
        verifyShortcutInfoPackage(packageName, shortcut);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ret = this.mShortcutRequestPinProcessor.createShortcutResultIntent(shortcut, userId);
        }
        verifyStates();
        return ret;
    }

    private boolean requestPinItem(String packageName, int userId, ShortcutInfo shortcut, AppWidgetProviderInfo appWidget, Bundle extras, IntentSender resultIntent) {
        boolean ret;
        verifyCaller(packageName, userId);
        verifyShortcutInfoPackage(packageName, shortcut);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            Preconditions.checkState(isUidForegroundLocked(injectBinderCallingUid()), "Calling application must have a foreground activity or a foreground service");
            if (shortcut != null) {
                ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
                if (ps.isShortcutExistsAndInvisibleToPublisher(shortcut.getId())) {
                    ps.updateInvisibleShortcutForPinRequestWith(shortcut);
                    packageShortcutsChanged(packageName, userId);
                }
            }
            ret = this.mShortcutRequestPinProcessor.requestPinItemLocked(shortcut, appWidget, extras, userId, resultIntent);
        }
        verifyStates();
        return ret;
    }

    public void disableShortcuts(String packageName, List shortcutIds, CharSequence disabledMessage, int disabledMessageResId, int userId) {
        String str = packageName;
        List list = shortcutIds;
        int i = userId;
        verifyCaller(str, i);
        Preconditions.checkNotNull(list, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(i);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(str, i);
            ps.ensureImmutableShortcutsNotIncludedWithIds(list, true);
            String disabledMessageString = disabledMessage == null ? null : disabledMessage.toString();
            int i2 = shortcutIds.size() - 1;
            while (true) {
                int i3 = i2;
                if (i3 >= 0) {
                    String id = (String) Preconditions.checkStringNotEmpty((String) list.get(i3));
                    if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                        ps.disableWithId(id, disabledMessageString, disabledMessageResId, false, true, 1);
                    }
                    i2 = i3 - 1;
                } else {
                    ps.adjustRanks();
                }
            }
        }
        packageShortcutsChanged(str, i);
        verifyStates();
    }

    public void enableShortcuts(String packageName, List shortcutIds, int userId) {
        verifyCaller(packageName, userId);
        Preconditions.checkNotNull(shortcutIds, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            int i = shortcutIds.size() - 1;
            while (true) {
                int i2 = i;
                if (i2 >= 0) {
                    String id = (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i2));
                    if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                        ps.enableWithId(id);
                    }
                    i = i2 - 1;
                }
            }
        }
        packageShortcutsChanged(packageName, userId);
        verifyStates();
    }

    public void removeDynamicShortcuts(String packageName, List shortcutIds, int userId) {
        verifyCaller(packageName, userId);
        Preconditions.checkNotNull(shortcutIds, "shortcutIds must be provided");
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            ShortcutPackage ps = getPackageShortcutsForPublisherLocked(packageName, userId);
            ps.ensureImmutableShortcutsNotIncludedWithIds(shortcutIds, true);
            for (int i = shortcutIds.size() - 1; i >= 0; i--) {
                String id = (String) Preconditions.checkStringNotEmpty((String) shortcutIds.get(i));
                if (ps.isShortcutExistsAndVisibleToPublisher(id)) {
                    ps.deleteDynamicWithId(id, true);
                }
            }
            ps.adjustRanks();
        }
        packageShortcutsChanged(packageName, userId);
        verifyStates();
    }

    public void removeAllDynamicShortcuts(String packageName, int userId) {
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            getPackageShortcutsForPublisherLocked(packageName, userId).deleteAllDynamicShortcuts(true);
        }
        packageShortcutsChanged(packageName, userId);
        verifyStates();
    }

    public ParceledListSlice<ShortcutInfo> getDynamicShortcuts(String packageName, int userId) {
        ParceledListSlice shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 9, -$$Lambda$ShortcutService$vv6Ko6L2p38nn3EYcL5PZxcyRyk.INSTANCE);
        }
        return shortcutsWithQueryLocked;
    }

    public ParceledListSlice<ShortcutInfo> getManifestShortcuts(String packageName, int userId) {
        ParceledListSlice shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 9, -$$Lambda$ShortcutService$FW40Da1L1EZJ_usDX0ew1qRMmtc.INSTANCE);
        }
        return shortcutsWithQueryLocked;
    }

    public ParceledListSlice<ShortcutInfo> getPinnedShortcuts(String packageName, int userId) {
        ParceledListSlice shortcutsWithQueryLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            shortcutsWithQueryLocked = getShortcutsWithQueryLocked(packageName, userId, 9, -$$Lambda$ShortcutService$K2g8Oho05j5S7zVOkoQrHzM_Gig.INSTANCE);
        }
        return shortcutsWithQueryLocked;
    }

    @GuardedBy("mLock")
    private ParceledListSlice<ShortcutInfo> getShortcutsWithQueryLocked(String packageName, int userId, int cloneFlags, Predicate<ShortcutInfo> query) {
        ArrayList<ShortcutInfo> ret = new ArrayList();
        getPackageShortcutsForPublisherLocked(packageName, userId).findAll(ret, query, cloneFlags);
        return new ParceledListSlice(setReturnedByServer(ret));
    }

    public int getMaxShortcutCountPerActivity(String packageName, int userId) throws RemoteException {
        verifyCaller(packageName, userId);
        return this.mMaxShortcuts;
    }

    public int getRemainingCallCount(String packageName, int userId) {
        int apiCallCount;
        verifyCaller(packageName, userId);
        boolean unlimited = injectHasUnlimitedShortcutsApiCallsPermission(injectBinderCallingPid(), injectBinderCallingUid());
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            apiCallCount = this.mMaxUpdatesPerInterval - getPackageShortcutsForPublisherLocked(packageName, userId).getApiCallCount(unlimited);
        }
        return apiCallCount;
    }

    public long getRateLimitResetTime(String packageName, int userId) {
        long nextResetTimeLocked;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            nextResetTimeLocked = getNextResetTimeLocked();
        }
        return nextResetTimeLocked;
    }

    public int getIconMaxDimensions(String packageName, int userId) {
        int i;
        verifyCaller(packageName, userId);
        synchronized (this.mLock) {
            i = this.mMaxIconDimension;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:9:0x002e, code skipped:
            r0 = injectClearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:11:?, code skipped:
            r6.mUsageStatsManagerInternal.reportShortcutUsage(r7, r8, r9);
     */
    /* JADX WARNING: Missing block: B:14:0x003d, code skipped:
            injectRestoreCallingIdentity(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportShortcutUsed(String packageName, String shortcutId, int userId) {
        verifyCaller(packageName, userId);
        Preconditions.checkNotNull(shortcutId);
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            if (getPackageShortcutsForPublisherLocked(packageName, userId).findShortcutById(shortcutId) == null) {
                Log.w(TAG, String.format("reportShortcutUsed: package %s doesn't have shortcut %s", new Object[]{packageName, shortcutId}));
            }
        }
    }

    public boolean isRequestPinItemSupported(int callingUserId, int requestType) {
        long token = injectClearCallingIdentity();
        try {
            boolean isRequestPinItemSupported = this.mShortcutRequestPinProcessor.isRequestPinItemSupported(callingUserId, requestType);
            return isRequestPinItemSupported;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    public void resetThrottling() {
        enforceSystemOrShell();
        resetThrottlingInner(getCallingUserId());
    }

    void resetThrottlingInner(int userId) {
        synchronized (this.mLock) {
            if (isUserUnlockedL(userId)) {
                getUserShortcutsLocked(userId).resetThrottling();
                scheduleSaveUser(userId);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ShortcutManager: throttling counter reset for user ");
                stringBuilder.append(userId);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("User ");
            stringBuilder2.append(userId);
            stringBuilder2.append(" is locked or not running");
            Log.w(str2, stringBuilder2.toString());
        }
    }

    void resetAllThrottlingInner() {
        synchronized (this.mLock) {
            this.mRawLastResetTime = injectCurrentTimeMillis();
        }
        scheduleSaveBaseState();
        Slog.i(TAG, "ShortcutManager: throttling counter reset for all users");
    }

    public void onApplicationActive(String packageName, int userId) {
        enforceResetThrottlingPermission();
        synchronized (this.mLock) {
            if (isUserUnlockedL(userId)) {
                getPackageShortcutsLocked(packageName, userId).resetRateLimitingForCommandLineNoSaving();
                saveUserLocked(userId);
                return;
            }
        }
    }

    boolean hasShortcutHostPermission(String callingPackage, int userId, int callingPid, int callingUid) {
        if (canSeeAnyPinnedShortcut(callingPackage, userId, callingPid, callingUid)) {
            return true;
        }
        long start = getStatStartTime();
        try {
            boolean hasShortcutHostPermissionInner = hasShortcutHostPermissionInner(callingPackage, userId);
            return hasShortcutHostPermissionInner;
        } finally {
            logDurationStat(4, start);
        }
    }

    private boolean isCallerSystemApp(String packageName, int userId) {
        PackageInfo packageInfo = getPackageInfoWithSignatures(packageName, userId);
        boolean z = false;
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return false;
        }
        int i = packageInfo.applicationInfo.flags;
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if ((i & 1) != 0) {
            z = true;
        }
        return z;
    }

    boolean canSeeAnyPinnedShortcut(String callingPackage, int userId, int callingPid, int callingUid) {
        if (injectHasAccessShortcutsPermission(callingPid, callingUid)) {
            return true;
        }
        boolean hasHostPackage;
        synchronized (this.mLock) {
            hasHostPackage = getNonPersistentUserLocked(userId).hasHostPackage(callingPackage);
        }
        return hasHostPackage;
    }

    @VisibleForTesting
    boolean injectHasAccessShortcutsPermission(int callingPid, int callingUid) {
        return this.mContext.checkPermission("android.permission.ACCESS_SHORTCUTS", callingPid, callingUid) == 0;
    }

    @VisibleForTesting
    boolean injectHasUnlimitedShortcutsApiCallsPermission(int callingPid, int callingUid) {
        return this.mContext.checkPermission("android.permission.UNLIMITED_SHORTCUTS_API_CALLS", callingPid, callingUid) == 0;
    }

    @VisibleForTesting
    boolean hasShortcutHostPermissionInner(String packageName, int userId) {
        synchronized (this.mLock) {
            throwIfUserLockedL(userId);
            if (isCallerSystemApp(packageName, userId) && sObtainShortCutPermissionpackageNames.contains(packageName)) {
                return true;
            }
            ShortcutUser user = getUserShortcutsLocked(userId);
            ComponentName cached = user.getCachedLauncher();
            if (cached == null || !cached.getPackageName().equals(packageName)) {
                ComponentName detected = getDefaultLauncher(userId);
                user.setLauncher(detected);
                if (detected != null) {
                    boolean equals = detected.getPackageName().equals(packageName);
                    return equals;
                }
                return false;
            }
            return true;
        }
    }

    ComponentName getDefaultLauncher(int userId) {
        int i = userId;
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            ComponentName detected;
            synchronized (this.mLock) {
                throwIfUserLockedL(userId);
                ShortcutUser user = getUserShortcutsLocked(userId);
                List<ResolveInfo> allHomeCandidates = new ArrayList();
                long startGetHomeActivitiesAsUser = getStatStartTime();
                ComponentName defaultLauncher = this.mPackageManagerInternal.getHomeActivitiesAsUser(allHomeCandidates, i);
                logDurationStat(0, startGetHomeActivitiesAsUser);
                if (defaultLauncher != null) {
                    detected = defaultLauncher;
                } else {
                    detected = user.getLastKnownLauncher();
                    if (detected != null) {
                        if (!injectIsActivityEnabledAndExported(detected, i)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Cached launcher ");
                            stringBuilder.append(detected);
                            stringBuilder.append(" no longer exists");
                            Slog.w(str, stringBuilder.toString());
                            detected = null;
                            user.clearLauncher();
                        }
                    }
                }
                if (detected == null) {
                    int size = allHomeCandidates.size();
                    int lastPriority = Integer.MIN_VALUE;
                    int i2 = 0;
                    while (true) {
                        int i3 = i2;
                        if (i3 >= size) {
                            break;
                        }
                        ShortcutUser user2 = user;
                        ResolveInfo ri = (ResolveInfo) allHomeCandidates.get(i3);
                        if (ri.activityInfo.applicationInfo.isSystemApp()) {
                            if (ri.priority >= lastPriority) {
                                detected = ri.activityInfo.getComponentName();
                                lastPriority = ri.priority;
                            }
                        }
                        i2 = i3 + 1;
                        user = user2;
                        i = userId;
                    }
                }
            }
            injectRestoreCallingIdentity(token);
            logDurationStat(16, start);
            return detected;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(16, start);
        }
    }

    public void setShortcutHostPackage(String type, String packageName, int userId) {
        synchronized (this.mLock) {
            getNonPersistentUserLocked(userId).setShortcutHostPackage(type, packageName);
        }
    }

    private void cleanUpPackageForAllLoadedUsers(String packageName, int packageUserId, boolean appStillExists) {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(new -$$Lambda$ShortcutService$7yTF58WvWYAkuD-B4tHmI0K7nyI(this, packageName, packageUserId, appStillExists));
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void cleanUpPackageLocked(String packageName, int owningUserId, int packageUserId, boolean appStillExists) {
        boolean wasUserLoaded = isUserLoadedLocked(owningUserId);
        ShortcutUser user = getUserShortcutsLocked(owningUserId);
        boolean doNotify = false;
        if (packageUserId == owningUserId && user.removePackage(packageName) != null) {
            doNotify = true;
        }
        user.removeLauncher(packageUserId, packageName);
        user.forAllLaunchers(new -$$Lambda$ShortcutService$okoC4pj1SnB9cHJpbZ-Xc5MyThk(packageName, packageUserId));
        user.forAllPackages(-$$Lambda$ShortcutService$9V6lStUbS0hFjsnM72O75L8xvNI.INSTANCE);
        scheduleSaveUser(owningUserId);
        if (doNotify) {
            notifyListeners(packageName, owningUserId);
        }
        if (appStillExists && packageUserId == owningUserId) {
            user.rescanPackageIfNeeded(packageName, true);
        }
        if (!wasUserLoaded) {
            unloadUserLocked(owningUserId);
        }
    }

    void handleLocaleChanged() {
        scheduleSaveBaseState();
        synchronized (this.mLock) {
            long token = injectClearCallingIdentity();
            try {
                forEachLoadedUserLocked(-$$Lambda$ShortcutService$OcNf5Tg_F4Us34H7SfC_e_tnGzw.INSTANCE);
            } finally {
                injectRestoreCallingIdentity(token);
            }
        }
    }

    @VisibleForTesting
    void checkPackageChanges(int ownerUserId) {
        if (injectIsSafeModeEnabled()) {
            Slog.i(TAG, "Safe mode, skipping checkPackageChanges()");
            return;
        }
        long start = getStatStartTime();
        try {
            ArrayList<PackageWithUser> gonePackages = new ArrayList();
            synchronized (this.mLock) {
                ShortcutUser user = getUserShortcutsLocked(ownerUserId);
                user.forAllPackageItems(new -$$Lambda$ShortcutService$KKtB89b9du8RtyDY2LIMGlzZzzg(this, gonePackages));
                if (gonePackages.size() > 0) {
                    for (int i = gonePackages.size() - 1; i >= 0; i--) {
                        PackageWithUser pu = (PackageWithUser) gonePackages.get(i);
                        cleanUpPackageLocked(pu.packageName, ownerUserId, pu.userId, false);
                    }
                }
                rescanUpdatedPackagesLocked(ownerUserId, user.getLastAppScanTime());
            }
            logDurationStat(8, start);
            verifyStates();
        } catch (Throwable th) {
            logDurationStat(8, start);
        }
    }

    public static /* synthetic */ void lambda$checkPackageChanges$6(ShortcutService shortcutService, ArrayList gonePackages, ShortcutPackageItem spi) {
        if (!(spi.getPackageInfo().isShadow() || shortcutService.isPackageInstalled(spi.getPackageName(), spi.getPackageUserId()))) {
            gonePackages.add(PackageWithUser.of(spi));
        }
    }

    @GuardedBy("mLock")
    private void rescanUpdatedPackagesLocked(int userId, long lastScanTime) {
        ShortcutUser user = getUserShortcutsLocked(userId);
        long now = injectCurrentTimeMillis();
        forUpdatedPackages(userId, lastScanTime, injectBuildFingerprint().equals(user.getLastAppScanOsFingerprint()) ^ 1, new -$$Lambda$ShortcutService$XepsJlzLd-VitYi8_ThhUsx37Ok(this, user, userId));
        user.setLastAppScanTime(now);
        user.setLastAppScanOsFingerprint(injectBuildFingerprint());
        scheduleSaveUser(userId);
    }

    public static /* synthetic */ void lambda$rescanUpdatedPackagesLocked$7(ShortcutService shortcutService, ShortcutUser user, int userId, ApplicationInfo ai) {
        user.attemptToRestoreIfNeededAndSave(shortcutService, ai.packageName, userId);
        user.rescanPackageIfNeeded(ai.packageName, true);
    }

    private void handlePackageAdded(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(userId);
            user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
            user.rescanPackageIfNeeded(packageName, true);
        }
        verifyStates();
    }

    private void handlePackageUpdateFinished(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = getUserShortcutsLocked(userId);
            user.attemptToRestoreIfNeededAndSave(this, packageName, userId);
            if (isPackageInstalled(packageName, userId)) {
                user.rescanPackageIfNeeded(packageName, true);
            }
        }
        verifyStates();
    }

    private void handlePackageRemoved(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId, false);
        verifyStates();
    }

    private void handlePackageDataCleared(String packageName, int packageUserId) {
        cleanUpPackageForAllLoadedUsers(packageName, packageUserId, true);
        verifyStates();
    }

    private void handlePackageChanged(String packageName, int packageUserId) {
        if (isPackageInstalled(packageName, packageUserId)) {
            synchronized (this.mLock) {
                getUserShortcutsLocked(packageUserId).rescanPackageIfNeeded(packageName, true);
            }
            verifyStates();
            return;
        }
        handlePackageRemoved(packageName, packageUserId);
    }

    final PackageInfo getPackageInfoWithSignatures(String packageName, int userId) {
        return getPackageInfo(packageName, userId, true);
    }

    final PackageInfo getPackageInfo(String packageName, int userId) {
        return getPackageInfo(packageName, userId, false);
    }

    int injectGetPackageUid(String packageName, int userId) {
        long token = injectClearCallingIdentity();
        try {
            int packageUid = this.mIPackageManager.getPackageUid(packageName, PACKAGE_MATCH_FLAGS, userId);
            injectRestoreCallingIdentity(token);
            return packageUid;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            injectRestoreCallingIdentity(token);
            return -1;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            throw th;
        }
    }

    @VisibleForTesting
    final PackageInfo getPackageInfo(String packageName, int userId, boolean getSignatures) {
        return isInstalledOrNull(injectPackageInfoWithUninstalled(packageName, userId, getSignatures));
    }

    @VisibleForTesting
    PackageInfo injectPackageInfoWithUninstalled(String packageName, int userId, boolean getSignatures) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        int i = 1;
        try {
            PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(packageName, PACKAGE_MATCH_FLAGS | (getSignatures ? 134217728 : 0), userId);
            injectRestoreCallingIdentity(token);
            if (getSignatures) {
                i = 2;
            }
            logDurationStat(i, start);
            return packageInfo;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            injectRestoreCallingIdentity(token);
            if (getSignatures) {
                i = 2;
            }
            logDurationStat(i, start);
            return null;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            if (getSignatures) {
                i = 2;
            }
            logDurationStat(i, start);
            throw th;
        }
    }

    @VisibleForTesting
    final ApplicationInfo getApplicationInfo(String packageName, int userId) {
        return isInstalledOrNull(injectApplicationInfoWithUninstalled(packageName, userId));
    }

    @VisibleForTesting
    ApplicationInfo injectApplicationInfoWithUninstalled(String packageName, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            ApplicationInfo applicationInfo = this.mIPackageManager.getApplicationInfo(packageName, PACKAGE_MATCH_FLAGS, userId);
            injectRestoreCallingIdentity(token);
            logDurationStat(3, start);
            return applicationInfo;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            injectRestoreCallingIdentity(token);
            logDurationStat(3, start);
            return null;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(3, start);
            throw th;
        }
    }

    final ActivityInfo getActivityInfoWithMetadata(ComponentName activity, int userId) {
        return isInstalledOrNull(injectGetActivityInfoWithMetadataWithUninstalled(activity, userId));
    }

    @VisibleForTesting
    ActivityInfo injectGetActivityInfoWithMetadataWithUninstalled(ComponentName activity, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            ActivityInfo activityInfo = this.mIPackageManager.getActivityInfo(activity, 794752, userId);
            injectRestoreCallingIdentity(token);
            logDurationStat(6, start);
            return activityInfo;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            injectRestoreCallingIdentity(token);
            logDurationStat(6, start);
            return null;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(6, start);
            throw th;
        }
    }

    @VisibleForTesting
    final List<PackageInfo> getInstalledPackages(int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            List<PackageInfo> all = injectGetPackagesWithUninstalled(userId);
            all.removeIf(PACKAGE_NOT_INSTALLED);
            injectRestoreCallingIdentity(token);
            logDurationStat(7, start);
            return all;
        } catch (RemoteException e) {
            Slog.wtf(TAG, "RemoteException", e);
            injectRestoreCallingIdentity(token);
            logDurationStat(7, start);
            return null;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(7, start);
            throw th;
        }
    }

    @VisibleForTesting
    List<PackageInfo> injectGetPackagesWithUninstalled(int userId) throws RemoteException {
        ParceledListSlice<PackageInfo> parceledList = this.mIPackageManager.getInstalledPackages(PACKAGE_MATCH_FLAGS, userId);
        if (parceledList == null) {
            return Collections.emptyList();
        }
        return parceledList.getList();
    }

    private void forUpdatedPackages(int userId, long lastScanTime, boolean afterOta, Consumer<ApplicationInfo> callback) {
        List<PackageInfo> list = getInstalledPackages(userId);
        for (int i = list.size() - 1; i >= 0; i--) {
            PackageInfo pi = (PackageInfo) list.get(i);
            if (afterOta || pi.lastUpdateTime >= lastScanTime) {
                callback.accept(pi.applicationInfo);
            }
        }
    }

    private boolean isApplicationFlagSet(String packageName, int userId, int flags) {
        ApplicationInfo ai = injectApplicationInfoWithUninstalled(packageName, userId);
        return ai != null && (ai.flags & flags) == flags;
    }

    private static boolean isInstalled(ApplicationInfo ai) {
        return (ai == null || !ai.enabled || (ai.flags & DumpState.DUMP_VOLUMES) == 0) ? false : true;
    }

    private static boolean isEphemeralApp(ApplicationInfo ai) {
        return ai != null && ai.isInstantApp();
    }

    private static boolean isInstalled(PackageInfo pi) {
        return pi != null && isInstalled(pi.applicationInfo);
    }

    private static boolean isInstalled(ActivityInfo ai) {
        return ai != null && isInstalled(ai.applicationInfo);
    }

    private static ApplicationInfo isInstalledOrNull(ApplicationInfo ai) {
        return isInstalled(ai) ? ai : null;
    }

    private static PackageInfo isInstalledOrNull(PackageInfo pi) {
        return isInstalled(pi) ? pi : null;
    }

    private static ActivityInfo isInstalledOrNull(ActivityInfo ai) {
        return isInstalled(ai) ? ai : null;
    }

    boolean isPackageInstalled(String packageName, int userId) {
        return getApplicationInfo(packageName, userId) != null;
    }

    boolean isEphemeralApp(String packageName, int userId) {
        return isEphemeralApp(getApplicationInfo(packageName, userId));
    }

    XmlResourceParser injectXmlMetaData(ActivityInfo activityInfo, String key) {
        return activityInfo.loadXmlMetaData(this.mContext.getPackageManager(), key);
    }

    Resources injectGetResourcesForApplicationAsUser(String packageName, int userId) {
        long start = getStatStartTime();
        long token = injectClearCallingIdentity();
        try {
            Resources resourcesForApplicationAsUser = this.mContext.getPackageManager().getResourcesForApplicationAsUser(packageName, userId);
            injectRestoreCallingIdentity(token);
            logDurationStat(9, start);
            return resourcesForApplicationAsUser;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Resources for package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" not found");
            Slog.e(str, stringBuilder.toString());
            injectRestoreCallingIdentity(token);
            logDurationStat(9, start);
            return null;
        } catch (Throwable th) {
            injectRestoreCallingIdentity(token);
            logDurationStat(9, start);
            throw th;
        }
    }

    private Intent getMainActivityIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory(LAUNCHER_INTENT_CATEGORY);
        return intent;
    }

    @VisibleForTesting
    List<ResolveInfo> queryActivities(Intent baseIntent, String packageName, ComponentName activity, int userId) {
        baseIntent.setPackage((String) Preconditions.checkNotNull(packageName));
        if (activity != null) {
            baseIntent.setComponent(activity);
        }
        return queryActivities(baseIntent, userId, true);
    }

    List<ResolveInfo> queryActivities(Intent intent, int userId, boolean exportedOnly) {
        long token = injectClearCallingIdentity();
        try {
            List<ResolveInfo> resolved = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, PACKAGE_MATCH_FLAGS, userId);
            if (resolved == null || resolved.size() == 0) {
                return EMPTY_RESOLVE_INFO;
            }
            if (!isInstalled(((ResolveInfo) resolved.get(0)).activityInfo)) {
                return EMPTY_RESOLVE_INFO;
            }
            if (exportedOnly) {
                resolved.removeIf(ACTIVITY_NOT_EXPORTED);
            }
            return resolved;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    ComponentName injectGetDefaultMainActivity(String packageName, int userId) {
        long start = getStatStartTime();
        try {
            ComponentName componentName = null;
            List<ResolveInfo> resolved = queryActivities(getMainActivityIntent(), packageName, null, userId);
            if (resolved.size() != 0) {
                componentName = ((ResolveInfo) resolved.get(0)).activityInfo.getComponentName();
            }
            logDurationStat(11, start);
            return componentName;
        } catch (Throwable th) {
            logDurationStat(11, start);
        }
    }

    boolean injectIsMainActivity(ComponentName activity, int userId) {
        long start = getStatStartTime();
        boolean z = false;
        if (activity == null) {
            try {
                wtf("null activity detected");
            } finally {
                logDurationStat(12, start);
            }
        } else if (DUMMY_MAIN_ACTIVITY.equals(activity.getClassName())) {
            logDurationStat(12, start);
            return true;
        } else {
            if (queryActivities(getMainActivityIntent(), activity.getPackageName(), activity, userId).size() > 0) {
                z = true;
            }
            logDurationStat(12, start);
            return z;
        }
        return false;
    }

    ComponentName getDummyMainActivity(String packageName) {
        return new ComponentName(packageName, DUMMY_MAIN_ACTIVITY);
    }

    boolean isDummyMainActivity(ComponentName name) {
        return name != null && DUMMY_MAIN_ACTIVITY.equals(name.getClassName());
    }

    List<ResolveInfo> injectGetMainActivities(String packageName, int userId) {
        long start = getStatStartTime();
        try {
            List<ResolveInfo> queryActivities = queryActivities(getMainActivityIntent(), packageName, null, userId);
            return queryActivities;
        } finally {
            logDurationStat(12, start);
        }
    }

    @VisibleForTesting
    boolean injectIsActivityEnabledAndExported(ComponentName activity, int userId) {
        long start = getStatStartTime();
        try {
            boolean z = queryActivities(new Intent(), activity.getPackageName(), activity, userId).size() > 0;
            logDurationStat(13, start);
            return z;
        } catch (Throwable th) {
            logDurationStat(13, start);
        }
    }

    ComponentName injectGetPinConfirmationActivity(String launcherPackageName, int launcherUserId, int requestType) {
        String action;
        Preconditions.checkNotNull(launcherPackageName);
        if (requestType == 1) {
            action = "android.content.pm.action.CONFIRM_PIN_SHORTCUT";
        } else {
            action = "android.content.pm.action.CONFIRM_PIN_APPWIDGET";
        }
        Iterator it = queryActivities(new Intent(action).setPackage(launcherPackageName), launcherUserId, null).iterator();
        if (it.hasNext()) {
            return ((ResolveInfo) it.next()).activityInfo.getComponentName();
        }
        return null;
    }

    boolean injectIsSafeModeEnabled() {
        long token = injectClearCallingIdentity();
        boolean e;
        try {
            e = IWindowManager.Stub.asInterface(ServiceManager.getService("window")).isSafeModeEnabled();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            return false;
        } finally {
            injectRestoreCallingIdentity(token);
        }
    }

    int getParentOrSelfUserId(int userId) {
        return this.mUserManagerInternal.getProfileParentId(userId);
    }

    void injectSendIntentSender(IntentSender intentSender, Intent extras) {
        if (intentSender != null) {
            try {
                intentSender.sendIntent(this.mContext, 0, extras, null, null);
            } catch (SendIntentException e) {
                Slog.w(TAG, "sendIntent failed().", e);
            }
        }
    }

    boolean shouldBackupApp(String packageName, int userId) {
        return isApplicationFlagSet(packageName, userId, 32768);
    }

    static boolean shouldBackupApp(PackageInfo pi) {
        return (pi.applicationInfo.flags & 32768) != 0;
    }

    public byte[] getBackupPayload(int userId) {
        enforceSystem();
        synchronized (this.mLock) {
            if (isUserUnlockedL(userId)) {
                ShortcutUser user = getUserShortcutsLocked(userId);
                if (user == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't backup: user not found: id=");
                    stringBuilder.append(userId);
                    wtf(stringBuilder.toString());
                    return null;
                }
                user.forAllPackageItems(-$$Lambda$ShortcutService$qeFlXbEdNY-s36xnqPf5bs5axg0.INSTANCE);
                user.forAllPackages(-$$Lambda$ShortcutService$bdUeaAkcePSCZBDxdJttl1FPOmI.INSTANCE);
                user.forAllLaunchers(-$$Lambda$ShortcutService$5PQDuMeuJAK9L5YMuS3D3xeOzEc.INSTANCE);
                scheduleSaveUser(userId);
                saveDirtyInfo();
                ByteArrayOutputStream os = new ByteArrayOutputStream(32768);
                try {
                    saveUserInternalLocked(userId, os, true);
                    byte[] payload = os.toByteArray();
                    this.mShortcutDumpFiles.save("backup-1-payload.txt", payload);
                    return payload;
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Backup failed.", e);
                    return null;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Can't backup: user ");
            stringBuilder2.append(userId);
            stringBuilder2.append(" is locked or not running");
            wtf(stringBuilder2.toString());
            return null;
        }
    }

    public void applyRestore(byte[] payload, int userId) {
        enforceSystem();
        synchronized (this.mLock) {
            if (isUserUnlockedL(userId)) {
                this.mShortcutDumpFiles.save("restore-0-start.txt", new -$$Lambda$ShortcutService$38xbKnaiRwgkQIdkh2OdULJL_hQ(this));
                this.mShortcutDumpFiles.save("restore-1-payload.xml", payload);
                try {
                    ShortcutUser restored = loadUserInternal(userId, new ByteArrayInputStream(payload), true);
                    this.mShortcutDumpFiles.save("restore-2.txt", new -$$Lambda$ShortcutService$w7_ouiisHmMMzTkQ_HUAHbawlLY(this));
                    getUserShortcutsLocked(userId).mergeRestoredFile(restored);
                    this.mShortcutDumpFiles.save("restore-3.txt", new -$$Lambda$ShortcutService$w7_ouiisHmMMzTkQ_HUAHbawlLY(this));
                    rescanUpdatedPackagesLocked(userId, 0);
                    this.mShortcutDumpFiles.save("restore-4.txt", new -$$Lambda$ShortcutService$w7_ouiisHmMMzTkQ_HUAHbawlLY(this));
                    this.mShortcutDumpFiles.save("restore-5-finish.txt", new -$$Lambda$ShortcutService$vKI79Gf4pKq8ASWghBXV-NKhZwk(this));
                    saveUserLocked(userId);
                    return;
                } catch (InvalidFileFormatException | IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Restoration failed.", e);
                    return;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't restore: user ");
            stringBuilder.append(userId);
            stringBuilder.append(" is locked or not running");
            wtf(stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$applyRestore$11(ShortcutService shortcutService, PrintWriter pw) {
        pw.print("Start time: ");
        shortcutService.dumpCurrentTime(pw);
        pw.println();
    }

    public static /* synthetic */ void lambda$applyRestore$12(ShortcutService shortcutService, PrintWriter pw) {
        pw.print("Finish time: ");
        shortcutService.dumpCurrentTime(pw);
        pw.println();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            dumpNoCheck(fd, pw, args);
        }
    }

    @VisibleForTesting
    void dumpNoCheck(FileDescriptor fd, PrintWriter pw, String[] args) {
        DumpFilter filter = parseDumpArgs(args);
        if (filter.shouldDumpCheckIn()) {
            dumpCheckin(pw, filter.shouldCheckInClear());
            return;
        }
        if (filter.shouldDumpMain()) {
            dumpInner(pw, filter);
            pw.println();
        }
        if (filter.shouldDumpUid()) {
            dumpUid(pw);
            pw.println();
        }
        if (filter.shouldDumpFiles()) {
            dumpDumpFiles(pw);
            pw.println();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:62:0x0103 A:{LOOP_END, LOOP:1: B:60:0x0100->B:62:0x0103} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static DumpFilter parseDumpArgs(String[] args) {
        DumpFilter filter = new DumpFilter();
        if (args == null) {
            return filter;
        }
        int argIndex = 0;
        while (argIndex < args.length) {
            int argIndex2 = argIndex + 1;
            String arg = args[argIndex];
            if ("-c".equals(arg)) {
                filter.setDumpCheckIn(true);
            } else if ("--checkin".equals(arg)) {
                filter.setDumpCheckIn(true);
                filter.setCheckInClear(true);
            } else if ("-a".equals(arg) || "--all".equals(arg)) {
                filter.setDumpUid(true);
                filter.setDumpFiles(true);
            } else if ("-u".equals(arg) || "--uid".equals(arg)) {
                filter.setDumpUid(true);
            } else if ("-f".equals(arg) || "--files".equals(arg)) {
                filter.setDumpFiles(true);
            } else if ("-n".equals(arg) || "--no-main".equals(arg)) {
                filter.setDumpMain(false);
            } else {
                int argIndex3;
                if ("--user".equals(arg)) {
                    if (argIndex2 < args.length) {
                        argIndex3 = argIndex2 + 1;
                        try {
                            filter.addUser(Integer.parseInt(args[argIndex2]));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid user ID", e);
                        }
                    }
                    throw new IllegalArgumentException("Missing user ID for --user");
                } else if ("-p".equals(arg) || "--package".equals(arg)) {
                    if (argIndex2 < args.length) {
                        argIndex3 = argIndex2 + 1;
                        filter.addPackageRegex(args[argIndex2]);
                        filter.setDumpDetails(false);
                    } else {
                        throw new IllegalArgumentException("Missing package name for --package");
                    }
                } else if (arg.startsWith("-")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown option ");
                    stringBuilder.append(arg);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else {
                    argIndex = argIndex2;
                    while (argIndex < args.length) {
                        int argIndex4 = argIndex + 1;
                        filter.addPackage(args[argIndex]);
                        argIndex = argIndex4;
                    }
                    return filter;
                }
                argIndex = argIndex3;
            }
            argIndex = argIndex2;
        }
        while (argIndex < args.length) {
        }
        return filter;
    }

    private void dumpInner(PrintWriter pw) {
        dumpInner(pw, new DumpFilter());
    }

    private void dumpInner(PrintWriter pw, DumpFilter filter) {
        synchronized (this.mLock) {
            if (filter.shouldDumpDetails()) {
                long now = injectCurrentTimeMillis();
                pw.print("Now: [");
                pw.print(now);
                pw.print("] ");
                pw.print(formatTime(now));
                pw.print("  Raw last reset: [");
                pw.print(this.mRawLastResetTime);
                pw.print("] ");
                pw.print(formatTime(this.mRawLastResetTime));
                long last = getLastResetTimeLocked();
                pw.print("  Last reset: [");
                pw.print(last);
                pw.print("] ");
                pw.print(formatTime(last));
                long next = getNextResetTimeLocked();
                pw.print("  Next reset: [");
                pw.print(next);
                pw.print("] ");
                pw.print(formatTime(next));
                pw.println();
                pw.println();
                pw.print("  Config:");
                pw.print("    Max icon dim: ");
                pw.println(this.mMaxIconDimension);
                pw.print("    Icon format: ");
                pw.println(this.mIconPersistFormat);
                pw.print("    Icon quality: ");
                pw.println(this.mIconPersistQuality);
                pw.print("    saveDelayMillis: ");
                pw.println(this.mSaveDelayMillis);
                pw.print("    resetInterval: ");
                pw.println(this.mResetInterval);
                pw.print("    maxUpdatesPerInterval: ");
                pw.println(this.mMaxUpdatesPerInterval);
                pw.print("    maxShortcutsPerActivity: ");
                pw.println(this.mMaxShortcuts);
                pw.println();
                this.mStatLogger.dump(pw, "  ");
                pw.println();
                pw.print("  #Failures: ");
                pw.println(this.mWtfCount);
                if (this.mLastWtfStacktrace != null) {
                    pw.print("  Last failure stack trace: ");
                    pw.println(Log.getStackTraceString(this.mLastWtfStacktrace));
                }
                pw.println();
                this.mShortcutBitmapSaver.dumpLocked(pw, "  ");
                pw.println();
            }
            int i = 0;
            for (int i2 = 0; i2 < this.mUsers.size(); i2++) {
                ShortcutUser user = (ShortcutUser) this.mUsers.valueAt(i2);
                if (filter.isUserMatch(user.getUserId())) {
                    user.dump(pw, "  ", filter);
                    pw.println();
                }
            }
            while (i < this.mShortcutNonPersistentUsers.size()) {
                ShortcutNonPersistentUser user2 = (ShortcutNonPersistentUser) this.mShortcutNonPersistentUsers.valueAt(i);
                if (filter.isUserMatch(user2.getUserId())) {
                    user2.dump(pw, "  ", filter);
                    pw.println();
                }
                i++;
            }
        }
    }

    private void dumpUid(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("** SHORTCUT MANAGER UID STATES (dumpsys shortcut -n -u)");
            for (int i = 0; i < this.mUidState.size(); i++) {
                int uid = this.mUidState.keyAt(i);
                int state = this.mUidState.valueAt(i);
                pw.print("    UID=");
                pw.print(uid);
                pw.print(" state=");
                pw.print(state);
                if (isProcessStateForeground(state)) {
                    pw.print("  [FG]");
                }
                pw.print("  last FG=");
                pw.print(this.mUidLastForegroundElapsedTime.get(uid));
                pw.println();
            }
        }
    }

    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    private void dumpCurrentTime(PrintWriter pw) {
        pw.print(formatTime(injectCurrentTimeMillis()));
    }

    private void dumpCheckin(PrintWriter pw, boolean clear) {
        synchronized (this.mLock) {
            try {
                JSONArray users = new JSONArray();
                for (int i = 0; i < this.mUsers.size(); i++) {
                    users.put(((ShortcutUser) this.mUsers.valueAt(i)).dumpCheckin(clear));
                }
                JSONObject result = new JSONObject();
                result.put(KEY_SHORTCUT, users);
                result.put(KEY_LOW_RAM, injectIsLowRamDevice());
                result.put(KEY_ICON_SIZE, this.mMaxIconDimension);
                pw.println(result.toString(1));
            } catch (JSONException e) {
                Slog.e(TAG, "Unable to write in json", e);
            }
        }
    }

    private void dumpDumpFiles(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("** SHORTCUT MANAGER FILES (dumpsys shortcut -n -f)");
            this.mShortcutDumpFiles.dumpAll(pw);
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        Throwable th;
        enforceShell();
        long token = injectClearCallingIdentity();
        try {
            try {
                resultReceiver.send(new MyShellCommand(this, null).exec(this, in, out, err, args, callback, resultReceiver), null);
                injectRestoreCallingIdentity(token);
            } catch (Throwable th2) {
                th = th2;
                injectRestoreCallingIdentity(token);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            ResultReceiver resultReceiver2 = resultReceiver;
            injectRestoreCallingIdentity(token);
            throw th;
        }
    }

    @VisibleForTesting
    long injectCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @VisibleForTesting
    long injectElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @VisibleForTesting
    long injectUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    @VisibleForTesting
    int injectBinderCallingUid() {
        return getCallingUid();
    }

    @VisibleForTesting
    int injectBinderCallingPid() {
        return getCallingPid();
    }

    private int getCallingUserId() {
        return UserHandle.getUserId(injectBinderCallingUid());
    }

    @VisibleForTesting
    long injectClearCallingIdentity() {
        return Binder.clearCallingIdentity();
    }

    @VisibleForTesting
    void injectRestoreCallingIdentity(long token) {
        Binder.restoreCallingIdentity(token);
    }

    String injectBuildFingerprint() {
        return Build.FINGERPRINT;
    }

    final void wtf(String message) {
        wtf(message, null);
    }

    void wtf(String message, Throwable e) {
        if (e == null) {
            e = new RuntimeException("Stacktrace");
        }
        synchronized (this.mLock) {
            this.mWtfCount++;
            this.mLastWtfStacktrace = new Exception("Last failure was logged here:");
        }
        Slog.wtf(TAG, message, e);
    }

    @VisibleForTesting
    File injectSystemDataPath() {
        return Environment.getDataSystemDirectory();
    }

    @VisibleForTesting
    File injectUserDataPath(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), DIRECTORY_PER_USER);
    }

    public File getDumpPath() {
        return new File(injectUserDataPath(0), DIRECTORY_DUMP);
    }

    @VisibleForTesting
    boolean injectIsLowRamDevice() {
        return ActivityManager.isLowRamDeviceStatic();
    }

    @VisibleForTesting
    void injectRegisterUidObserver(IUidObserver observer, int which) {
        try {
            ActivityManager.getService().registerUidObserver(observer, which, -1, null);
        } catch (RemoteException e) {
        }
    }

    File getUserBitmapFilePath(int userId) {
        return new File(injectUserDataPath(userId), DIRECTORY_BITMAPS);
    }

    @VisibleForTesting
    SparseArray<ShortcutUser> getShortcutsForTest() {
        return this.mUsers;
    }

    @VisibleForTesting
    int getMaxShortcutsForTest() {
        return this.mMaxShortcuts;
    }

    @VisibleForTesting
    int getMaxUpdatesPerIntervalForTest() {
        return this.mMaxUpdatesPerInterval;
    }

    @VisibleForTesting
    long getResetIntervalForTest() {
        return this.mResetInterval;
    }

    @VisibleForTesting
    int getMaxIconDimensionForTest() {
        return this.mMaxIconDimension;
    }

    @VisibleForTesting
    CompressFormat getIconPersistFormatForTest() {
        return this.mIconPersistFormat;
    }

    @VisibleForTesting
    int getIconPersistQualityForTest() {
        return this.mIconPersistQuality;
    }

    @VisibleForTesting
    ShortcutPackage getPackageShortcutForTest(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = (ShortcutUser) this.mUsers.get(userId);
            if (user == null) {
                return null;
            }
            ShortcutPackage shortcutPackage = (ShortcutPackage) user.getAllPackagesForTest().get(packageName);
            return shortcutPackage;
        }
    }

    @VisibleForTesting
    ShortcutInfo getPackageShortcutForTest(String packageName, String shortcutId, int userId) {
        synchronized (this.mLock) {
            ShortcutPackage pkg = getPackageShortcutForTest(packageName, userId);
            if (pkg == null) {
                return null;
            }
            ShortcutInfo findShortcutById = pkg.findShortcutById(shortcutId);
            return findShortcutById;
        }
    }

    @VisibleForTesting
    ShortcutLauncher getLauncherShortcutForTest(String packageName, int userId) {
        synchronized (this.mLock) {
            ShortcutUser user = (ShortcutUser) this.mUsers.get(userId);
            if (user == null) {
                return null;
            }
            ShortcutLauncher shortcutLauncher = (ShortcutLauncher) user.getAllLaunchersForTest().get(PackageWithUser.of(userId, packageName));
            return shortcutLauncher;
        }
    }

    @VisibleForTesting
    ShortcutRequestPinProcessor getShortcutRequestPinProcessorForTest() {
        return this.mShortcutRequestPinProcessor;
    }

    @VisibleForTesting
    boolean injectShouldPerformVerification() {
        return false;
    }

    final void verifyStates() {
        if (injectShouldPerformVerification()) {
            verifyStatesInner();
        }
    }

    private final void verifyStatesForce() {
        verifyStatesInner();
    }

    private void verifyStatesInner() {
        synchronized (this.mLock) {
            forEachLoadedUserLocked(-$$Lambda$ShortcutService$iMiNoPdGwz3-PvMFlFpgdWX24mE.INSTANCE);
        }
    }

    @VisibleForTesting
    void waitForBitmapSavesForTest() {
        synchronized (this.mLock) {
            this.mShortcutBitmapSaver.waitForAllSavesLocked();
        }
    }
}

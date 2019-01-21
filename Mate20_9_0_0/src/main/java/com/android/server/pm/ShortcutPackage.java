package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.am.HwBroadcastRadarUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutPackage extends ShortcutPackageItem {
    private static final String ATTR_ACTIVITY = "activity";
    private static final String ATTR_BITMAP_PATH = "bitmap-path";
    private static final String ATTR_CALL_COUNT = "call-count";
    private static final String ATTR_DISABLED_MESSAGE = "dmessage";
    private static final String ATTR_DISABLED_MESSAGE_RES_ID = "dmessageid";
    private static final String ATTR_DISABLED_MESSAGE_RES_NAME = "dmessagename";
    private static final String ATTR_DISABLED_REASON = "disabled-reason";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_ICON_RES_ID = "icon-res";
    private static final String ATTR_ICON_RES_NAME = "icon-resname";
    private static final String ATTR_ID = "id";
    private static final String ATTR_INTENT_LEGACY = "intent";
    private static final String ATTR_INTENT_NO_EXTRA = "intent-base";
    private static final String ATTR_LAST_RESET = "last-reset";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAME_XMLUTILS = "name";
    private static final String ATTR_RANK = "rank";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_TEXT_RES_ID = "textid";
    private static final String ATTR_TEXT_RES_NAME = "textname";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_TITLE_RES_ID = "titleid";
    private static final String ATTR_TITLE_RES_NAME = "titlename";
    private static final String KEY_BITMAPS = "bitmaps";
    private static final String KEY_BITMAP_BYTES = "bitmapBytes";
    private static final String KEY_DYNAMIC = "dynamic";
    private static final String KEY_MANIFEST = "manifest";
    private static final String KEY_PINNED = "pinned";
    private static final String NAME_CATEGORIES = "categories";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_EXTRAS = "extras";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_INTENT_EXTRAS_LEGACY = "intent-extras";
    static final String TAG_ROOT = "package";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_STRING_ARRAY_XMLUTILS = "string-array";
    private static final String TAG_VERIFY = "ShortcutService.verify";
    private int mApiCallCount;
    private long mLastKnownForegroundElapsedTime;
    private long mLastResetTime;
    private final int mPackageUid;
    final Comparator<ShortcutInfo> mShortcutRankComparator;
    final Comparator<ShortcutInfo> mShortcutTypeAndRankComparator;
    private final ArrayMap<String, ShortcutInfo> mShortcuts;

    private ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName, ShortcutPackageInfo spi) {
        ShortcutPackageInfo shortcutPackageInfo;
        if (spi != null) {
            shortcutPackageInfo = spi;
        } else {
            shortcutPackageInfo = ShortcutPackageInfo.newEmpty();
        }
        super(shortcutUser, packageUserId, packageName, shortcutPackageInfo);
        this.mShortcuts = new ArrayMap();
        this.mShortcutTypeAndRankComparator = -$$Lambda$ShortcutPackage$ZN-r6tS0M7WKGK6nbXyJZPwNRGc.INSTANCE;
        this.mShortcutRankComparator = -$$Lambda$ShortcutPackage$hEXnzlESoRjagj8Pd9f4PrqudKE.INSTANCE;
        this.mPackageUid = shortcutUser.mService.injectGetPackageUid(packageName, packageUserId);
    }

    public ShortcutPackage(ShortcutUser shortcutUser, int packageUserId, String packageName) {
        this(shortcutUser, packageUserId, packageName, null);
    }

    public int getOwnerUserId() {
        return getPackageUserId();
    }

    public int getPackageUid() {
        return this.mPackageUid;
    }

    public Resources getPackageResources() {
        return this.mShortcutUser.mService.injectGetResourcesForApplicationAsUser(getPackageName(), getPackageUserId());
    }

    public int getShortcutCount() {
        return this.mShortcuts.size();
    }

    protected boolean canRestoreAnyVersion() {
        return false;
    }

    protected void onRestored(int restoreBlockReason) {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            si.clearFlags(4096);
            si.setDisabledReason(restoreBlockReason);
            if (restoreBlockReason != 0) {
                si.addFlags(64);
            }
        }
        refreshPinnedFlags();
    }

    public ShortcutInfo findShortcutById(String id) {
        return (ShortcutInfo) this.mShortcuts.get(id);
    }

    public boolean isShortcutExistsAndInvisibleToPublisher(String id) {
        ShortcutInfo si = findShortcutById(id);
        return (si == null || si.isVisibleToPublisher()) ? false : true;
    }

    public boolean isShortcutExistsAndVisibleToPublisher(String id) {
        ShortcutInfo si = findShortcutById(id);
        return si != null && si.isVisibleToPublisher();
    }

    private void ensureNotImmutable(ShortcutInfo shortcut, boolean ignoreInvisible) {
        if (shortcut != null && shortcut.isImmutable()) {
            if (!ignoreInvisible || shortcut.isVisibleToPublisher()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Manifest shortcut ID=");
                stringBuilder.append(shortcut.getId());
                stringBuilder.append(" may not be manipulated via APIs");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public void ensureNotImmutable(String id, boolean ignoreInvisible) {
        ensureNotImmutable((ShortcutInfo) this.mShortcuts.get(id), ignoreInvisible);
    }

    public void ensureImmutableShortcutsNotIncludedWithIds(List<String> shortcutIds, boolean ignoreInvisible) {
        for (int i = shortcutIds.size() - 1; i >= 0; i--) {
            ensureNotImmutable((String) shortcutIds.get(i), ignoreInvisible);
        }
    }

    public void ensureImmutableShortcutsNotIncluded(List<ShortcutInfo> shortcuts, boolean ignoreInvisible) {
        for (int i = shortcuts.size() - 1; i >= 0; i--) {
            ensureNotImmutable(((ShortcutInfo) shortcuts.get(i)).getId(), ignoreInvisible);
        }
    }

    private ShortcutInfo forceDeleteShortcutInner(String id) {
        ShortcutInfo shortcut = (ShortcutInfo) this.mShortcuts.remove(id);
        if (shortcut != null) {
            this.mShortcutUser.mService.removeIconLocked(shortcut);
            shortcut.clearFlags(35);
        }
        return shortcut;
    }

    private void forceReplaceShortcutInner(ShortcutInfo newShortcut) {
        ShortcutService s = this.mShortcutUser.mService;
        forceDeleteShortcutInner(newShortcut.getId());
        s.saveIconAndFixUpShortcutLocked(newShortcut);
        s.fixUpShortcutResourceNamesAndValues(newShortcut);
        this.mShortcuts.put(newShortcut.getId(), newShortcut);
    }

    public void addOrReplaceDynamicShortcut(ShortcutInfo newShortcut) {
        boolean wasPinned;
        Preconditions.checkArgument(newShortcut.isEnabled(), "add/setDynamicShortcuts() cannot publish disabled shortcuts");
        newShortcut.addFlags(1);
        ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(newShortcut.getId());
        if (oldShortcut == null) {
            wasPinned = false;
        } else {
            oldShortcut.ensureUpdatableWith(newShortcut, false);
            wasPinned = oldShortcut.isPinned();
        }
        if (wasPinned) {
            newShortcut.addFlags(2);
        }
        forceReplaceShortcutInner(newShortcut);
    }

    private void removeOrphans() {
        int i;
        ArrayList<String> removeList = null;
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (!si.isAlive()) {
                if (removeList == null) {
                    removeList = new ArrayList();
                }
                removeList.add(si.getId());
            }
        }
        if (removeList != null) {
            for (i = removeList.size() - 1; i >= 0; i--) {
                forceDeleteShortcutInner((String) removeList.get(i));
            }
        }
    }

    public void deleteAllDynamicShortcuts(boolean ignoreInvisible) {
        long now = this.mShortcutUser.mService.injectCurrentTimeMillis();
        boolean changed = false;
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isDynamic() && (!ignoreInvisible || si.isVisibleToPublisher())) {
                changed = true;
                si.setTimestamp(now);
                si.clearFlags(1);
                si.setRank(0);
            }
        }
        if (changed) {
            removeOrphans();
        }
    }

    public boolean deleteDynamicWithId(String shortcutId, boolean ignoreInvisible) {
        return deleteOrDisableWithId(shortcutId, false, false, ignoreInvisible, 0) == null;
    }

    private boolean disableDynamicWithId(String shortcutId, boolean ignoreInvisible, int disabledReason) {
        return deleteOrDisableWithId(shortcutId, true, false, ignoreInvisible, disabledReason) == null;
    }

    public void disableWithId(String shortcutId, String disabledMessage, int disabledMessageResId, boolean overrideImmutable, boolean ignoreInvisible, int disabledReason) {
        ShortcutInfo disabled = deleteOrDisableWithId(shortcutId, true, overrideImmutable, ignoreInvisible, disabledReason);
        if (disabled == null) {
            return;
        }
        if (disabledMessage != null) {
            disabled.setDisabledMessage(disabledMessage);
        } else if (disabledMessageResId != 0) {
            disabled.setDisabledMessageResId(disabledMessageResId);
            this.mShortcutUser.mService.fixUpShortcutResourceNamesAndValues(disabled);
        }
    }

    private ShortcutInfo deleteOrDisableWithId(String shortcutId, boolean disable, boolean overrideImmutable, boolean ignoreInvisible, int disabledReason) {
        boolean z = disable == (disabledReason != 0);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable and disabledReason disagree: ");
        stringBuilder.append(disable);
        stringBuilder.append(" vs ");
        stringBuilder.append(disabledReason);
        Preconditions.checkState(z, stringBuilder.toString());
        ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(shortcutId);
        if (oldShortcut == null || (!oldShortcut.isEnabled() && ignoreInvisible && !oldShortcut.isVisibleToPublisher())) {
            return null;
        }
        if (!overrideImmutable) {
            ensureNotImmutable(oldShortcut, true);
        }
        if (oldShortcut.isPinned()) {
            oldShortcut.setRank(0);
            oldShortcut.clearFlags(33);
            if (disable) {
                oldShortcut.addFlags(64);
                if (oldShortcut.getDisabledReason() == 0) {
                    oldShortcut.setDisabledReason(disabledReason);
                }
            }
            oldShortcut.setTimestamp(this.mShortcutUser.mService.injectCurrentTimeMillis());
            if (this.mShortcutUser.mService.isDummyMainActivity(oldShortcut.getActivity())) {
                oldShortcut.setActivity(null);
            }
            return oldShortcut;
        }
        forceDeleteShortcutInner(shortcutId);
        return null;
    }

    public void enableWithId(String shortcutId) {
        ShortcutInfo shortcut = (ShortcutInfo) this.mShortcuts.get(shortcutId);
        if (shortcut != null) {
            ensureNotImmutable(shortcut, true);
            shortcut.clearFlags(64);
            shortcut.setDisabledReason(0);
        }
    }

    public void updateInvisibleShortcutForPinRequestWith(ShortcutInfo shortcut) {
        Preconditions.checkNotNull((ShortcutInfo) this.mShortcuts.get(shortcut.getId()));
        this.mShortcutUser.mService.validateShortcutForPinRequest(shortcut);
        shortcut.addFlags(2);
        forceReplaceShortcutInner(shortcut);
        adjustRanks();
    }

    public void refreshPinnedFlags() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) this.mShortcuts.valueAt(i)).clearFlags(2);
        }
        this.mShortcutUser.forAllLaunchers(new -$$Lambda$ShortcutPackage$ibOAVgfKWMZFYSeVV_hLNx6jogk(this));
        removeOrphans();
    }

    public static /* synthetic */ void lambda$refreshPinnedFlags$0(ShortcutPackage shortcutPackage, ShortcutLauncher launcherShortcuts) {
        ArraySet<String> pinned = launcherShortcuts.getPinnedShortcutIds(shortcutPackage.getPackageName(), shortcutPackage.getPackageUserId());
        if (pinned != null && pinned.size() != 0) {
            for (int i = pinned.size() - 1; i >= 0; i--) {
                ShortcutInfo si = (ShortcutInfo) shortcutPackage.mShortcuts.get((String) pinned.valueAt(i));
                if (si != null) {
                    si.addFlags(2);
                }
            }
        }
    }

    public int getApiCallCount(boolean unlimited) {
        ShortcutService s = this.mShortcutUser.mService;
        if (s.isUidForegroundLocked(this.mPackageUid) || this.mLastKnownForegroundElapsedTime < s.getUidLastForegroundElapsedTimeLocked(this.mPackageUid) || unlimited) {
            this.mLastKnownForegroundElapsedTime = s.injectElapsedRealtime();
            resetRateLimiting();
        }
        long last = s.getLastResetTimeLocked();
        long now = s.injectCurrentTimeMillis();
        if (!ShortcutService.isClockValid(now) || this.mLastResetTime <= now) {
            if (this.mLastResetTime < last) {
                this.mApiCallCount = 0;
                this.mLastResetTime = last;
            }
            return this.mApiCallCount;
        }
        Slog.w(TAG, "Clock rewound");
        this.mLastResetTime = now;
        this.mApiCallCount = 0;
        return this.mApiCallCount;
    }

    public boolean tryApiCall(boolean unlimited) {
        ShortcutService s = this.mShortcutUser.mService;
        if (getApiCallCount(unlimited) >= s.mMaxUpdatesPerInterval) {
            return false;
        }
        this.mApiCallCount++;
        s.scheduleSaveUser(getOwnerUserId());
        return true;
    }

    public void resetRateLimiting() {
        if (this.mApiCallCount > 0) {
            this.mApiCallCount = 0;
            this.mShortcutUser.mService.scheduleSaveUser(getOwnerUserId());
        }
    }

    public void resetRateLimitingForCommandLineNoSaving() {
        this.mApiCallCount = 0;
        this.mLastResetTime = 0;
    }

    public void findAll(List<ShortcutInfo> result, Predicate<ShortcutInfo> query, int cloneFlag) {
        findAll(result, query, cloneFlag, null, 0, false);
    }

    public void findAll(List<ShortcutInfo> result, Predicate<ShortcutInfo> query, int cloneFlag, String callingLauncher, int launcherUserId, boolean getPinnedByAnyLauncher) {
        if (!getPackageInfo().isShadow()) {
            ArraySet<String> pinnedByCallerSet;
            ShortcutService s = this.mShortcutUser.mService;
            if (callingLauncher == null) {
                pinnedByCallerSet = null;
            } else {
                pinnedByCallerSet = s.getLauncherShortcutsLocked(callingLauncher, getPackageUserId(), launcherUserId).getPinnedShortcutIds(getPackageName(), getPackageUserId());
            }
            for (int i = 0; i < this.mShortcuts.size(); i++) {
                ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
                boolean isPinnedByCaller = callingLauncher == null || (pinnedByCallerSet != null && pinnedByCallerSet.contains(si.getId()));
                if (getPinnedByAnyLauncher || !si.isFloating() || isPinnedByCaller) {
                    ShortcutInfo clone = si.clone(cloneFlag);
                    if (!(getPinnedByAnyLauncher || isPinnedByCaller)) {
                        clone.clearFlags(2);
                    }
                    if (query == null || query.test(clone)) {
                        if (!isPinnedByCaller) {
                            clone.clearFlags(2);
                        }
                        result.add(clone);
                    }
                }
            }
        }
    }

    public void resetThrottling() {
        this.mApiCallCount = 0;
    }

    public ArraySet<String> getUsedBitmapFiles() {
        ArraySet<String> usedFiles = new ArraySet(this.mShortcuts.size());
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.getBitmapPath() != null) {
                usedFiles.add(getFileName(si.getBitmapPath()));
            }
        }
        return usedFiles;
    }

    private static String getFileName(String path) {
        int sep = path.lastIndexOf(File.separatorChar);
        if (sep == -1) {
            return path;
        }
        return path.substring(sep + 1);
    }

    private boolean areAllActivitiesStillEnabled() {
        if (this.mShortcuts.size() == 0) {
            return true;
        }
        ShortcutService s = this.mShortcutUser.mService;
        ArrayList<ComponentName> checked = new ArrayList(4);
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ComponentName activity = ((ShortcutInfo) this.mShortcuts.valueAt(i)).getActivity();
            if (!checked.contains(activity)) {
                checked.add(activity);
                if (!(activity == null || s.injectIsActivityEnabledAndExported(activity, getOwnerUserId()))) {
                    return false;
                }
            }
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:75:0x015f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x0146  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean rescanPackageIfNeeded(boolean isNewApp, boolean forceRescan) {
        ShortcutService s = this.mShortcutUser.mService;
        long start = s.getStatStartTime();
        try {
            PackageInfo pi = this.mShortcutUser.mService;
            boolean packageName = getPackageName();
            pi = pi.getPackageInfo(packageName, getPackageUserId());
            packageName = false;
            if (pi == null) {
                return packageName;
            }
            if (!(isNewApp || forceRescan)) {
                if (getPackageInfo().getVersionCode() == pi.getLongVersionCode() && getPackageInfo().getLastUpdateTime() == pi.lastUpdateTime && areAllActivitiesStillEnabled()) {
                    s.logDurationStat(14, start);
                    return false;
                }
            }
            s.logDurationStat(14, start);
            PackageInfo pi2 = pi;
            List<ShortcutInfo> newManifestShortcutList = null;
            try {
                newManifestShortcutList = ShortcutParser.parseShortcuts(this.mShortcutUser.mService, getPackageName(), getPackageUserId());
            } catch (IOException | XmlPullParserException e) {
                Slog.e(TAG, "Failed to load shortcuts from AndroidManifest.xml.", e);
            }
            int manifestShortcutSize = newManifestShortcutList == null ? 0 : newManifestShortcutList.size();
            if (isNewApp && manifestShortcutSize == 0) {
                return false;
            }
            getPackageInfo().updateFromPackageInfo(pi2);
            long newVersionCode = getPackageInfo().getVersionCode();
            int i = 1;
            int i2 = this.mShortcuts.size() - 1;
            while (i2 >= 0) {
                int manifestShortcutSize2;
                ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i2);
                if (si.getDisabledReason() == 100 && getPackageInfo().getBackupSourceVersionCode() <= newVersionCode) {
                    String str = TAG;
                    manifestShortcutSize2 = manifestShortcutSize;
                    Object[] objArr = new Object[i];
                    objArr[0] = si.getId();
                    Slog.i(str, String.format("Restoring shortcut: %s", objArr));
                    si.clearFlags(64);
                    si.setDisabledReason(0);
                } else {
                    manifestShortcutSize2 = manifestShortcutSize;
                }
                i2--;
                manifestShortcutSize = manifestShortcutSize2;
                i = 1;
            }
            if (!isNewApp) {
                Resources publisherRes = null;
                for (int i3 = this.mShortcuts.size() - 1; i3 >= 0; i3--) {
                    ShortcutInfo si2 = (ShortcutInfo) this.mShortcuts.valueAt(i3);
                    if (si2.isDynamic()) {
                        if (si2.getActivity() == null) {
                            s.wtf("null activity detected.");
                        } else if (!s.injectIsMainActivity(si2.getActivity(), getPackageUserId())) {
                            Slog.w(TAG, String.format("%s is no longer main activity. Disabling shorcut %s.", new Object[]{getPackageName(), si2.getId()}));
                            if (disableDynamicWithId(si2.getId(), false, 2)) {
                                continue;
                            }
                            if (si2.hasAnyResources()) {
                                if (!si2.isOriginallyFromManifest()) {
                                    if (publisherRes == null) {
                                        publisherRes = getPackageResources();
                                        if (publisherRes == null) {
                                            break;
                                        }
                                    }
                                    si2.lookupAndFillInResourceIds(publisherRes);
                                }
                                si2.setTimestamp(s.injectCurrentTimeMillis());
                            } else {
                                continue;
                            }
                        }
                    }
                    if (si2.hasAnyResources()) {
                    }
                }
            }
            publishManifestShortcuts(newManifestShortcutList);
            if (newManifestShortcutList != null) {
                pushOutExcessShortcuts();
            }
            s.verifyStates();
            s.packageShortcutsChanged(getPackageName(), getPackageUserId());
            return true;
        } finally {
            s.logDurationStat(14, start);
        }
    }

    private boolean publishManifestShortcuts(List<ShortcutInfo> newManifestShortcutList) {
        int i;
        boolean changed = false;
        ArraySet<String> toDisableList = null;
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isManifestShortcut()) {
                if (toDisableList == null) {
                    toDisableList = new ArraySet();
                }
                toDisableList.add(si.getId());
            }
        }
        if (newManifestShortcutList != null) {
            i = newManifestShortcutList.size();
            int i2 = 0;
            while (i2 < i) {
                changed = true;
                ShortcutInfo newShortcut = (ShortcutInfo) newManifestShortcutList.get(i2);
                boolean newDisabled = newShortcut.isEnabled() ^ 1;
                String id = newShortcut.getId();
                ShortcutInfo oldShortcut = (ShortcutInfo) this.mShortcuts.get(id);
                boolean wasPinned = false;
                if (oldShortcut != null) {
                    if (!oldShortcut.isOriginallyFromManifest()) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Shortcut with ID=");
                        stringBuilder.append(newShortcut.getId());
                        stringBuilder.append(" exists but is not from AndroidManifest.xml, not updating.");
                        Slog.e(str, stringBuilder.toString());
                        i2++;
                    } else if (oldShortcut.isPinned()) {
                        wasPinned = true;
                        newShortcut.addFlags(2);
                    }
                }
                if (!newDisabled || wasPinned) {
                    forceReplaceShortcutInner(newShortcut);
                    if (!(newDisabled || toDisableList == null)) {
                        toDisableList.remove(id);
                    }
                    i2++;
                } else {
                    i2++;
                }
            }
        }
        if (toDisableList != null) {
            for (i = toDisableList.size() - 1; i >= 0; i--) {
                changed = true;
                disableWithId((String) toDisableList.valueAt(i), null, 0, true, false, 2);
            }
            removeOrphans();
        }
        adjustRanks();
        return changed;
    }

    private boolean pushOutExcessShortcuts() {
        ShortcutService service = this.mShortcutUser.mService;
        int maxShortcuts = service.getMaxActivityShortcuts();
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            if (list.size() > maxShortcuts) {
                Collections.sort(list, this.mShortcutTypeAndRankComparator);
                for (int inner = list.size() - 1; inner >= maxShortcuts; inner--) {
                    ShortcutInfo shortcut = (ShortcutInfo) list.get(inner);
                    if (shortcut.isManifestShortcut()) {
                        service.wtf("Found manifest shortcuts in excess list.");
                    } else {
                        deleteDynamicWithId(shortcut.getId(), true);
                    }
                }
            }
        }
        return false;
    }

    static /* synthetic */ int lambda$new$1(ShortcutInfo a, ShortcutInfo b) {
        if (a.isManifestShortcut() && !b.isManifestShortcut()) {
            return -1;
        }
        if (a.isManifestShortcut() || !b.isManifestShortcut()) {
            return Integer.compare(a.getRank(), b.getRank());
        }
        return 1;
    }

    private ArrayMap<ComponentName, ArrayList<ShortcutInfo>> sortShortcutsToActivities() {
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> activitiesToShortcuts = new ArrayMap();
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (!si.isFloating()) {
                ComponentName activity = si.getActivity();
                if (activity == null) {
                    this.mShortcutUser.mService.wtf("null activity detected.");
                } else {
                    ArrayList<ShortcutInfo> list = (ArrayList) activitiesToShortcuts.get(activity);
                    if (list == null) {
                        list = new ArrayList();
                        activitiesToShortcuts.put(activity, list);
                    }
                    list.add(si);
                }
            }
        }
        return activitiesToShortcuts;
    }

    private void incrementCountForActivity(ArrayMap<ComponentName, Integer> counts, ComponentName cn, int increment) {
        Integer oldValue = (Integer) counts.get(cn);
        if (oldValue == null) {
            oldValue = Integer.valueOf(0);
        }
        counts.put(cn, Integer.valueOf(oldValue.intValue() + increment));
    }

    public void enforceShortcutCountsBeforeOperation(List<ShortcutInfo> newList, int operation) {
        int i;
        ShortcutInfo shortcut;
        ShortcutService service = this.mShortcutUser.mService;
        ArrayMap<ComponentName, Integer> counts = new ArrayMap(4);
        for (i = this.mShortcuts.size() - 1; i >= 0; i--) {
            shortcut = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (shortcut.isManifestShortcut()) {
                incrementCountForActivity(counts, shortcut.getActivity(), 1);
            } else if (shortcut.isDynamic() && operation != 0) {
                incrementCountForActivity(counts, shortcut.getActivity(), 1);
            }
        }
        for (i = newList.size() - 1; i >= 0; i--) {
            shortcut = (ShortcutInfo) newList.get(i);
            ComponentName newActivity = shortcut.getActivity();
            if (newActivity != null) {
                ShortcutInfo original = (ShortcutInfo) this.mShortcuts.get(shortcut.getId());
                if (original == null) {
                    if (operation != 2) {
                        incrementCountForActivity(counts, newActivity, 1);
                    }
                } else if (!original.isFloating() || operation != 2) {
                    if (operation != 0) {
                        ComponentName oldActivity = original.getActivity();
                        if (!original.isFloating()) {
                            incrementCountForActivity(counts, oldActivity, -1);
                        }
                    }
                    incrementCountForActivity(counts, newActivity, 1);
                }
            } else if (operation != 2) {
                service.wtf("Activity must not be null at this point");
            }
        }
        for (i = counts.size() - 1; i >= 0; i--) {
            service.enforceMaxActivityShortcuts(((Integer) counts.valueAt(i)).intValue());
        }
    }

    public void resolveResourceStrings() {
        ShortcutService s = this.mShortcutUser.mService;
        boolean changed = false;
        Resources publisherRes = null;
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.hasStringResources()) {
                changed = true;
                if (publisherRes == null) {
                    publisherRes = getPackageResources();
                    if (publisherRes == null) {
                        break;
                    }
                }
                si.resolveResourceStrings(publisherRes);
                si.setTimestamp(s.injectCurrentTimeMillis());
            }
        }
        if (changed) {
            s.packageShortcutsChanged(getPackageName(), getPackageUserId());
        }
    }

    public void clearAllImplicitRanks() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            ((ShortcutInfo) this.mShortcuts.valueAt(i)).clearImplicitRankAndRankChangedFlag();
        }
    }

    static /* synthetic */ int lambda$new$2(ShortcutInfo a, ShortcutInfo b) {
        int ret = Integer.compare(a.getRank(), b.getRank());
        if (ret != 0) {
            return ret;
        }
        if (a.isRankChanged() != b.isRankChanged()) {
            return a.isRankChanged() ? -1 : 1;
        }
        ret = Integer.compare(a.getImplicitRank(), b.getImplicitRank());
        if (ret != 0) {
            return ret;
        }
        return a.getId().compareTo(b.getId());
    }

    public void adjustRanks() {
        ShortcutService s = this.mShortcutUser.mService;
        long now = s.injectCurrentTimeMillis();
        int i = this.mShortcuts.size();
        while (true) {
            i--;
            if (i < 0) {
                break;
            }
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(i);
            if (si.isFloating() && si.getRank() != 0) {
                si.setTimestamp(now);
                si.setRank(0);
            }
        }
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            Collections.sort(list, this.mShortcutRankComparator);
            int size = list.size();
            int rank = 0;
            for (int i2 = 0; i2 < size; i2++) {
                ShortcutInfo si2 = (ShortcutInfo) list.get(i2);
                if (!si2.isManifestShortcut()) {
                    if (si2.isDynamic()) {
                        int rank2 = rank + 1;
                        if (si2.getRank() != rank) {
                            si2.setTimestamp(now);
                            si2.setRank(rank);
                        }
                        rank = rank2;
                    } else {
                        s.wtf("Non-dynamic shortcut found.");
                    }
                }
            }
        }
    }

    public boolean hasNonManifestShortcuts() {
        for (int i = this.mShortcuts.size() - 1; i >= 0; i--) {
            if (!((ShortcutInfo) this.mShortcuts.valueAt(i)).isDeclaredInManifest()) {
                return true;
            }
        }
        return false;
    }

    public void dump(PrintWriter pw, String prefix, DumpFilter filter) {
        pw.println();
        pw.print(prefix);
        pw.print("Package: ");
        pw.print(getPackageName());
        pw.print("  UID: ");
        pw.print(this.mPackageUid);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Calls: ");
        int i = 0;
        pw.print(getApiCallCount(false));
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last known FG: ");
        pw.print(this.mLastKnownForegroundElapsedTime);
        pw.println();
        pw.print(prefix);
        pw.print("  ");
        pw.print("Last reset: [");
        pw.print(this.mLastResetTime);
        pw.print("] ");
        pw.print(ShortcutService.formatTime(this.mLastResetTime));
        pw.println();
        ShortcutPackageInfo packageInfo = getPackageInfo();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  ");
        packageInfo.dump(pw, stringBuilder.toString());
        pw.println();
        pw.print(prefix);
        pw.println("  Shortcuts:");
        long totalBitmapSize = 0;
        ArrayMap<String, ShortcutInfo> shortcuts = this.mShortcuts;
        int size = shortcuts.size();
        while (i < size) {
            ShortcutInfo si = (ShortcutInfo) shortcuts.valueAt(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append("    ");
            pw.println(si.toDumpString(stringBuilder2.toString()));
            if (si.getBitmapPath() != null) {
                long len = new File(si.getBitmapPath()).length();
                pw.print(prefix);
                pw.print("      ");
                pw.print("bitmap size=");
                pw.println(len);
                totalBitmapSize += len;
            }
            i++;
        }
        pw.print(prefix);
        pw.print("  ");
        pw.print("Total bitmap size: ");
        pw.print(totalBitmapSize);
        pw.print(" (");
        pw.print(Formatter.formatFileSize(this.mShortcutUser.mService.mContext, totalBitmapSize));
        pw.println(")");
    }

    public JSONObject dumpCheckin(boolean clear) throws JSONException {
        JSONObject result = super.dumpCheckin(clear);
        int numDynamic = 0;
        int numPinned = 0;
        int numManifest = 0;
        int numBitmaps = 0;
        long totalBitmapSize = 0;
        ArrayMap<String, ShortcutInfo> shortcuts = this.mShortcuts;
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ShortcutInfo si = (ShortcutInfo) shortcuts.valueAt(i);
            if (si.isDynamic()) {
                numDynamic++;
            }
            if (si.isDeclaredInManifest()) {
                numManifest++;
            }
            if (si.isPinned()) {
                numPinned++;
            }
            if (si.getBitmapPath() != null) {
                numBitmaps++;
                totalBitmapSize += new File(si.getBitmapPath()).length();
            }
        }
        result.put(KEY_DYNAMIC, numDynamic);
        result.put(KEY_MANIFEST, numManifest);
        result.put(KEY_PINNED, numPinned);
        result.put(KEY_BITMAPS, numBitmaps);
        result.put(KEY_BITMAP_BYTES, totalBitmapSize);
        return result;
    }

    public void saveToXml(XmlSerializer out, boolean forBackup) throws IOException, XmlPullParserException {
        int size = this.mShortcuts.size();
        if (size != 0 || this.mApiCallCount != 0) {
            out.startTag(null, "package");
            ShortcutService.writeAttr(out, Settings.ATTR_NAME, getPackageName());
            ShortcutService.writeAttr(out, ATTR_CALL_COUNT, (long) this.mApiCallCount);
            ShortcutService.writeAttr(out, ATTR_LAST_RESET, this.mLastResetTime);
            getPackageInfo().saveToXml(this.mShortcutUser.mService, out, forBackup);
            for (int j = 0; j < size; j++) {
                saveShortcut(out, (ShortcutInfo) this.mShortcuts.valueAt(j), forBackup, getPackageInfo().isBackupAllowed());
            }
            out.endTag(null, "package");
        }
    }

    private void saveShortcut(XmlSerializer out, ShortcutInfo si, boolean forBackup, boolean appSupportsBackup) throws IOException, XmlPullParserException {
        ShortcutService s = this.mShortcutUser.mService;
        if (!forBackup || (si.isPinned() && si.isEnabled())) {
            int i = 0;
            boolean shouldBackupDetails = !forBackup || appSupportsBackup;
            if (si.isIconPendingSave()) {
                s.removeIconLocked(si);
            }
            out.startTag(null, TAG_SHORTCUT);
            ShortcutService.writeAttr(out, ATTR_ID, si.getId());
            ShortcutService.writeAttr(out, ATTR_ACTIVITY, si.getActivity());
            ShortcutService.writeAttr(out, ATTR_TITLE, si.getTitle());
            ShortcutService.writeAttr(out, ATTR_TITLE_RES_ID, (long) si.getTitleResId());
            ShortcutService.writeAttr(out, ATTR_TITLE_RES_NAME, si.getTitleResName());
            ShortcutService.writeAttr(out, ATTR_TEXT, si.getText());
            ShortcutService.writeAttr(out, ATTR_TEXT_RES_ID, (long) si.getTextResId());
            ShortcutService.writeAttr(out, ATTR_TEXT_RES_NAME, si.getTextResName());
            if (shouldBackupDetails) {
                ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE, si.getDisabledMessage());
                ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_ID, (long) si.getDisabledMessageResourceId());
                ShortcutService.writeAttr(out, ATTR_DISABLED_MESSAGE_RES_NAME, si.getDisabledMessageResName());
            }
            ShortcutService.writeAttr(out, ATTR_DISABLED_REASON, (long) si.getDisabledReason());
            ShortcutService.writeAttr(out, "timestamp", si.getLastChangedTimestamp());
            if (forBackup) {
                ShortcutService.writeAttr(out, ATTR_FLAGS, (long) (si.getFlags() & -2062));
                if (getPackageInfo().getVersionCode() == 0) {
                    s.wtf("Package version code should be available at this point.");
                }
            } else {
                ShortcutService.writeAttr(out, ATTR_RANK, (long) si.getRank());
                ShortcutService.writeAttr(out, ATTR_FLAGS, (long) si.getFlags());
                ShortcutService.writeAttr(out, ATTR_ICON_RES_ID, (long) si.getIconResourceId());
                ShortcutService.writeAttr(out, ATTR_ICON_RES_NAME, si.getIconResName());
                ShortcutService.writeAttr(out, ATTR_BITMAP_PATH, si.getBitmapPath());
            }
            if (shouldBackupDetails) {
                Set<String> cat = si.getCategories();
                if (cat != null && cat.size() > 0) {
                    out.startTag(null, "categories");
                    XmlUtils.writeStringArrayXml((String[]) cat.toArray(new String[cat.size()]), "categories", out);
                    out.endTag(null, "categories");
                }
                Intent[] intentsNoExtras = si.getIntentsNoExtras();
                PersistableBundle[] intentsExtras = si.getIntentPersistableExtrases();
                int numIntents = intentsNoExtras.length;
                while (i < numIntents) {
                    out.startTag(null, HwBroadcastRadarUtil.KEY_BROADCAST_INTENT);
                    ShortcutService.writeAttr(out, ATTR_INTENT_NO_EXTRA, intentsNoExtras[i]);
                    ShortcutService.writeTagExtra(out, TAG_EXTRAS, intentsExtras[i]);
                    out.endTag(null, HwBroadcastRadarUtil.KEY_BROADCAST_INTENT);
                    i++;
                }
                ShortcutService.writeTagExtra(out, TAG_EXTRAS, si.getExtras());
            }
            out.endTag(null, TAG_SHORTCUT);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0083  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0070  */
    /* JADX WARNING: Missing block: B:17:0x005d, code skipped:
            if (r6.equals(TAG_SHORTCUT) == false) goto L_0x006b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ShortcutPackage loadFromXml(ShortcutService s, ShortcutUser shortcutUser, XmlPullParser parser, boolean fromBackup) throws IOException, XmlPullParserException {
        String packageName = ShortcutService.parseStringAttribute(parser, Settings.ATTR_NAME);
        ShortcutPackage ret = new ShortcutPackage(shortcutUser, shortcutUser.getUserId(), packageName);
        ret.mApiCallCount = ShortcutService.parseIntAttribute(parser, ATTR_CALL_COUNT);
        ret.mLastResetTime = ShortcutService.parseLongAttribute(parser, ATTR_LAST_RESET);
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            Object obj = 1;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return ret;
            }
            if (type == 2) {
                next = parser.getDepth();
                String tag = parser.getName();
                if (next == outerDepth + 1) {
                    int hashCode = tag.hashCode();
                    if (hashCode != -1923478059) {
                        if (hashCode == -342500282) {
                        }
                    } else if (tag.equals("package-info")) {
                        obj = null;
                        switch (obj) {
                            case null:
                                ret.getPackageInfo().loadFromXml(parser, fromBackup);
                                continue;
                                continue;
                            case 1:
                                ShortcutInfo si = parseShortcut(parser, packageName, shortcutUser.getUserId(), fromBackup);
                                ret.mShortcuts.put(si.getId(), si);
                                continue;
                                continue;
                        }
                    }
                    obj = -1;
                    switch (obj) {
                        case null:
                            break;
                        case 1:
                            break;
                    }
                }
                ShortcutService.warnForInvalidTag(next, tag);
            }
        }
        return ret;
    }

    /* JADX WARNING: Missing block: B:44:0x015d, code skipped:
            r8 = r49;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static ShortcutInfo parseShortcut(XmlPullParser parser, String packageName, int userId, boolean fromBackup) throws IOException, XmlPullParserException {
        int flags;
        XmlPullParser xmlPullParser = parser;
        PersistableBundle intentPersistableExtrasLegacy = null;
        ArrayList<Intent> intents = new ArrayList();
        PersistableBundle extras = null;
        ArraySet<String> categories = null;
        String id = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_ID);
        ComponentName activityComponent = ShortcutService.parseComponentNameAttribute(xmlPullParser, ATTR_ACTIVITY);
        String title = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TITLE);
        int titleResId = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_TITLE_RES_ID);
        String titleResName = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TITLE_RES_NAME);
        String text = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TEXT);
        int textResId = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_TEXT_RES_ID);
        String textResName = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_TEXT_RES_NAME);
        String disabledMessage = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE);
        int disabledMessageResId = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE_RES_ID);
        String disabledMessageResName = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_DISABLED_MESSAGE_RES_NAME);
        int disabledReason = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_DISABLED_REASON);
        Intent intentLegacy = ShortcutService.parseIntentAttributeNoDefault(xmlPullParser, HwBroadcastRadarUtil.KEY_BROADCAST_INTENT);
        int rank = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_RANK);
        long lastChangedTimestamp = ShortcutService.parseLongAttribute(xmlPullParser, "timestamp");
        int flags2 = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_FLAGS);
        int iconResId = (int) ShortcutService.parseLongAttribute(xmlPullParser, ATTR_ICON_RES_ID);
        String iconResName = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_ICON_RES_NAME);
        String bitmapPath = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_BITMAP_PATH);
        int outerDepth = parser.getDepth();
        while (true) {
            int outerDepth2 = outerDepth;
            outerDepth = parser.next();
            int type = outerDepth;
            int i;
            int i2;
            if (outerDepth == 1) {
                i = type;
                i2 = outerDepth2;
            } else if (type != 3 || parser.getDepth() > outerDepth2) {
                if (type != 2) {
                    i2 = outerDepth2;
                } else {
                    int i3;
                    outerDepth = parser.getDepth();
                    String tag = parser.getName();
                    int i4 = 0;
                    switch (tag.hashCode()) {
                        case -1289032093:
                            if (tag.equals(TAG_EXTRAS)) {
                                i3 = 2;
                                break;
                            }
                        case -1183762788:
                            if (tag.equals(HwBroadcastRadarUtil.KEY_BROADCAST_INTENT)) {
                                i3 = 1;
                                break;
                            }
                        case -1044333900:
                            if (tag.equals(TAG_INTENT_EXTRAS_LEGACY)) {
                                i3 = 0;
                                break;
                            }
                        case -1024600675:
                            if (tag.equals(TAG_STRING_ARRAY_XMLUTILS)) {
                                i3 = 4;
                                break;
                            }
                        case 1296516636:
                            if (tag.equals("categories")) {
                                i3 = 3;
                                break;
                            }
                        default:
                            i3 = -1;
                            break;
                    }
                    switch (i3) {
                        case 0:
                            i = type;
                            i2 = outerDepth2;
                            intentPersistableExtrasLegacy = PersistableBundle.restoreFromXml(parser);
                            break;
                        case 1:
                            i2 = outerDepth2;
                            intents.add(parseIntent(parser));
                            break;
                        case 2:
                            i2 = outerDepth2;
                            extras = PersistableBundle.restoreFromXml(parser);
                            break;
                        case 3:
                            i = type;
                            i2 = outerDepth2;
                            break;
                        case 4:
                            i = type;
                            if (!"categories".equals(ShortcutService.parseStringAttribute(xmlPullParser, Settings.ATTR_NAME))) {
                                i2 = outerDepth2;
                                break;
                            }
                            String[] ar = XmlUtils.readThisStringArrayXml(xmlPullParser, TAG_STRING_ARRAY_XMLUTILS, 0);
                            i2 = outerDepth2;
                            categories = new ArraySet(ar.length);
                            while (true) {
                                type = i4;
                                if (type >= ar.length) {
                                    break;
                                }
                                categories.add(ar[type]);
                                i4 = type + 1;
                            }
                        default:
                            throw ShortcutService.throwForInvalidTag(outerDepth, tag);
                    }
                }
                outerDepth = i2;
            } else {
                i = type;
                i2 = outerDepth2;
            }
        }
        if (intentLegacy != null) {
            ShortcutInfo.setIntentExtras(intentLegacy, intentPersistableExtrasLegacy);
            intents.clear();
            intents.add(intentLegacy);
        }
        if (disabledReason == 0 && (flags2 & 64) != 0) {
            disabledReason = 1;
        }
        int disabledReason2 = disabledReason;
        if (fromBackup) {
            flags = flags2 | 4096;
        } else {
            flags = flags2;
        }
        int iconResId2 = iconResId;
        int rank2 = rank;
        return new ShortcutInfo(userId, id, packageName, activityComponent, null, title, titleResId, titleResName, text, textResId, textResName, disabledMessage, disabledMessageResId, disabledMessageResName, categories, (Intent[]) intents.toArray(new Intent[intents.size()]), rank2, extras, lastChangedTimestamp, flags, iconResId2, iconResName, bitmapPath, disabledReason2);
    }

    private static Intent parseIntent(XmlPullParser parser) throws IOException, XmlPullParserException {
        Intent intent = ShortcutService.parseIntentAttribute(parser, ATTR_INTENT_NO_EXTRA);
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                return intent;
            }
            if (type == 2) {
                next = parser.getDepth();
                String tag = parser.getName();
                Object obj = -1;
                if (tag.hashCode() == -1289032093 && tag.equals(TAG_EXTRAS)) {
                    obj = null;
                }
                if (obj == null) {
                    ShortcutInfo.setIntentExtras(intent, PersistableBundle.restoreFromXml(parser));
                } else {
                    throw ShortcutService.throwForInvalidTag(next, tag);
                }
            }
        }
        return intent;
    }

    @VisibleForTesting
    List<ShortcutInfo> getAllShortcutsForTest() {
        return new ArrayList(this.mShortcuts.values());
    }

    public void verifyStates() {
        int outer;
        String str;
        StringBuilder stringBuilder;
        super.verifyStates();
        boolean failed = false;
        ShortcutService s = this.mShortcutUser.mService;
        ArrayMap<ComponentName, ArrayList<ShortcutInfo>> all = sortShortcutsToActivities();
        for (outer = all.size() - 1; outer >= 0; outer--) {
            ArrayList<ShortcutInfo> list = (ArrayList) all.valueAt(outer);
            if (list.size() > this.mShortcutUser.mService.getMaxActivityShortcuts()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": activity ");
                stringBuilder.append(all.keyAt(outer));
                stringBuilder.append(" has ");
                stringBuilder.append(((ArrayList) all.valueAt(outer)).size());
                stringBuilder.append(" shortcuts.");
                Log.e(str, stringBuilder.toString());
            }
            Collections.sort(list, -$$Lambda$ShortcutPackage$DImOsVxMicPEAJPTxf_RRXuc70I.INSTANCE);
            ArrayList<ShortcutInfo> dynamicList = new ArrayList(list);
            dynamicList.removeIf(-$$Lambda$ShortcutPackage$Uf55CaKs9xv-osb2umPmXq3W2lM.INSTANCE);
            ArrayList<ShortcutInfo> manifestList = new ArrayList(list);
            dynamicList.removeIf(-$$Lambda$ShortcutPackage$9YSAfuJJkDxYR6ZL5AWyxpKsC_Y.INSTANCE);
            verifyRanksSequential(dynamicList);
            verifyRanksSequential(manifestList);
        }
        for (outer = this.mShortcuts.size() - 1; outer >= 0; outer--) {
            ShortcutInfo si = (ShortcutInfo) this.mShortcuts.valueAt(outer);
            if (!(si.isDeclaredInManifest() || si.isDynamic() || si.isPinned())) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" is not manifest, dynamic or pinned.");
                Log.e(str, stringBuilder.toString());
            }
            if (si.isDeclaredInManifest() && si.isDynamic()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" is both dynamic and manifest at the same time.");
                Log.e(str, stringBuilder.toString());
            }
            if (si.getActivity() == null && !si.isFloating()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" has null activity, but not floating.");
                Log.e(str, stringBuilder.toString());
            }
            if ((si.isDynamic() || si.isManifestShortcut()) && !si.isEnabled()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" is not floating, but is disabled.");
                Log.e(str, stringBuilder.toString());
            }
            if (si.isFloating() && si.getRank() != 0) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" is floating, but has rank=");
                stringBuilder.append(si.getRank());
                Log.e(str, stringBuilder.toString());
            }
            if (si.getIcon() != null) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" still has an icon");
                Log.e(str, stringBuilder.toString());
            }
            if (si.hasAdaptiveBitmap() && !si.hasIconFile()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" has adaptive bitmap but was not saved to a file.");
                Log.e(str, stringBuilder.toString());
            }
            if (si.hasIconFile() && si.hasIconResource()) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" has both resource and bitmap icons");
                Log.e(str, stringBuilder.toString());
            }
            if (si.isEnabled() != (si.getDisabledReason() == 0)) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" isEnabled() and getDisabledReason() disagree: ");
                stringBuilder.append(si.isEnabled());
                stringBuilder.append(" vs ");
                stringBuilder.append(si.getDisabledReason());
                Log.e(str, stringBuilder.toString());
            }
            if (si.getDisabledReason() == 100 && getPackageInfo().getBackupSourceVersionCode() == -1) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" RESTORED_VERSION_LOWER with no backup source version code.");
                Log.e(str, stringBuilder.toString());
            }
            if (s.isDummyMainActivity(si.getActivity())) {
                failed = true;
                str = TAG_VERIFY;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" has a dummy target activity");
                Log.e(str, stringBuilder.toString());
            }
        }
        if (failed) {
            throw new IllegalStateException("See logcat for errors");
        }
    }

    private boolean verifyRanksSequential(List<ShortcutInfo> list) {
        boolean failed = false;
        for (int i = 0; i < list.size(); i++) {
            ShortcutInfo si = (ShortcutInfo) list.get(i);
            if (si.getRank() != i) {
                failed = true;
                String str = TAG_VERIFY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(getPackageName());
                stringBuilder.append(": shortcut ");
                stringBuilder.append(si.getId());
                stringBuilder.append(" rank=");
                stringBuilder.append(si.getRank());
                stringBuilder.append(" but expected to be ");
                stringBuilder.append(i);
                Log.e(str, stringBuilder.toString());
            }
        }
        return failed;
    }
}

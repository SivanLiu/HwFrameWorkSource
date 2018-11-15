package com.android.server.pm;

import android.content.Intent;
import android.content.pm.InstantAppInfo;
import android.content.pm.PackageParser.Package;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.ByteStringUtils;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class InstantAppRegistry {
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG = false;
    private static final long DEFAULT_INSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_INSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final long DEFAULT_UNINSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_UNINSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final String INSTANT_APPS_FOLDER = "instant";
    private static final String INSTANT_APP_ANDROID_ID_FILE = "android_id";
    private static final String INSTANT_APP_COOKIE_FILE_PREFIX = "cookie_";
    private static final String INSTANT_APP_COOKIE_FILE_SIFFIX = ".dat";
    private static final String INSTANT_APP_ICON_FILE = "icon.png";
    private static final String INSTANT_APP_METADATA_FILE = "metadata.xml";
    private static final String LOG_TAG = "InstantAppRegistry";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final CookiePersistence mCookiePersistence = new CookiePersistence(BackgroundThread.getHandler().getLooper());
    @GuardedBy("mService.mPackages")
    private SparseArray<SparseBooleanArray> mInstalledInstantAppUids;
    @GuardedBy("mService.mPackages")
    private SparseArray<SparseArray<SparseBooleanArray>> mInstantGrants;
    private final PackageManagerService mService;
    @GuardedBy("mService.mPackages")
    private SparseArray<List<UninstalledInstantAppState>> mUninstalledInstantApps;

    private final class CookiePersistence extends Handler {
        private static final long PERSIST_COOKIE_DELAY_MILLIS = 1000;
        private final SparseArray<ArrayMap<Package, SomeArgs>> mPendingPersistCookies = new SparseArray();

        public CookiePersistence(Looper looper) {
            super(looper);
        }

        public void schedulePersistLPw(int userId, Package pkg, byte[] cookie) {
            File newCookieFile = InstantAppRegistry.computeInstantCookieFile(pkg.packageName, PackageUtils.computeSignaturesSha256Digest(pkg.mSigningDetails.signatures), userId);
            if (!pkg.mSigningDetails.hasSignatures()) {
                Slog.wtf(InstantAppRegistry.LOG_TAG, "Parsed Instant App contains no valid signatures!");
            }
            File oldCookieFile = InstantAppRegistry.peekInstantCookieFile(pkg.packageName, userId);
            if (!(oldCookieFile == null || newCookieFile.equals(oldCookieFile))) {
                oldCookieFile.delete();
            }
            cancelPendingPersistLPw(pkg, userId);
            addPendingPersistCookieLPw(userId, pkg, cookie, newCookieFile);
            sendMessageDelayed(obtainMessage(userId, pkg), 1000);
        }

        public byte[] getPendingPersistCookieLPr(Package pkg, int userId) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser != null) {
                SomeArgs state = (SomeArgs) pendingWorkForUser.get(pkg);
                if (state != null) {
                    return (byte[]) state.arg1;
                }
            }
            return null;
        }

        public void cancelPendingPersistLPw(Package pkg, int userId) {
            removeMessages(userId, pkg);
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state != null) {
                state.recycle();
            }
        }

        private void addPendingPersistCookieLPw(int userId, Package pkg, byte[] cookie, File cookieFile) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            if (pendingWorkForUser == null) {
                pendingWorkForUser = new ArrayMap();
                this.mPendingPersistCookies.put(userId, pendingWorkForUser);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = cookie;
            args.arg2 = cookieFile;
            pendingWorkForUser.put(pkg, args);
        }

        private SomeArgs removePendingPersistCookieLPr(Package pkg, int userId) {
            ArrayMap<Package, SomeArgs> pendingWorkForUser = (ArrayMap) this.mPendingPersistCookies.get(userId);
            SomeArgs state = null;
            if (pendingWorkForUser != null) {
                state = (SomeArgs) pendingWorkForUser.remove(pkg);
                if (pendingWorkForUser.isEmpty()) {
                    this.mPendingPersistCookies.remove(userId);
                }
            }
            return state;
        }

        public void handleMessage(Message message) {
            int userId = message.what;
            Package pkg = message.obj;
            SomeArgs state = removePendingPersistCookieLPr(pkg, userId);
            if (state != null) {
                byte[] cookie = state.arg1;
                File cookieFile = state.arg2;
                state.recycle();
                InstantAppRegistry.this.persistInstantApplicationCookie(cookie, pkg.packageName, cookieFile, userId);
            }
        }
    }

    private static final class UninstalledInstantAppState {
        final InstantAppInfo mInstantAppInfo;
        final long mTimestamp;

        public UninstalledInstantAppState(InstantAppInfo instantApp, long timestamp) {
            this.mInstantAppInfo = instantApp;
            this.mTimestamp = timestamp;
        }
    }

    public InstantAppRegistry(PackageManagerService service) {
        this.mService = service;
    }

    public byte[] getInstantAppCookieLPw(String packageName, int userId) {
        Package pkg = (Package) this.mService.mPackages.get(packageName);
        if (pkg == null) {
            return null;
        }
        byte[] pendingCookie = this.mCookiePersistence.getPendingPersistCookieLPr(pkg, userId);
        if (pendingCookie != null) {
            return pendingCookie;
        }
        File cookieFile = peekInstantCookieFile(packageName, userId);
        if (cookieFile != null && cookieFile.exists()) {
            try {
                return IoUtils.readFileAsByteArray(cookieFile.toString());
            } catch (IOException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading cookie file: ");
                stringBuilder.append(cookieFile);
                Slog.w(str, stringBuilder.toString());
            }
        }
        return null;
    }

    public boolean setInstantAppCookieLPw(String packageName, byte[] cookie, int userId) {
        if (cookie != null && cookie.length > 0) {
            int maxCookieSize = this.mService.mContext.getPackageManager().getInstantAppCookieMaxBytes();
            if (cookie.length > maxCookieSize) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Instant app cookie for package ");
                stringBuilder.append(packageName);
                stringBuilder.append(" size ");
                stringBuilder.append(cookie.length);
                stringBuilder.append(" bytes while max size is ");
                stringBuilder.append(maxCookieSize);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        }
        Package pkg = (Package) this.mService.mPackages.get(packageName);
        if (pkg == null) {
            return false;
        }
        this.mCookiePersistence.schedulePersistLPw(userId, pkg, cookie);
        return true;
    }

    /* JADX WARNING: Missing block: B:20:?, code:
            r0 = new java.io.FileOutputStream(r7);
     */
    /* JADX WARNING: Missing block: B:23:?, code:
            r0.write(r5, 0, r5.length);
     */
    /* JADX WARNING: Missing block: B:25:?, code:
            $closeResource(null, r0);
     */
    /* JADX WARNING: Missing block: B:31:?, code:
            $closeResource(r1, r0);
     */
    /* JADX WARNING: Missing block: B:33:0x004f, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:34:0x0050, code:
            r1 = LOG_TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Error writing instant app cookie file: ");
            r2.append(r7);
            android.util.Slog.e(r1, r2.toString(), r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void persistInstantApplicationCookie(byte[] cookie, String packageName, File cookieFile, int userId) {
        synchronized (this.mService.mPackages) {
            File appDir = getInstantApplicationDir(packageName, userId);
            if (appDir.exists() || appDir.mkdirs()) {
                if (cookieFile.exists() && !cookieFile.delete()) {
                    Slog.e(LOG_TAG, "Cannot delete instant app cookie file");
                }
                if (cookie == null || cookie.length <= 0) {
                    return;
                }
            }
            Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    public Bitmap getInstantAppIconLPw(String packageName, int userId) {
        File iconFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ICON_FILE);
        if (iconFile.exists()) {
            return BitmapFactory.decodeFile(iconFile.toString());
        }
        return null;
    }

    public String getInstantAppAndroidIdLPw(String packageName, int userId) {
        File idFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
        if (idFile.exists()) {
            try {
                return IoUtils.readFileAsString(idFile.getAbsolutePath());
            } catch (IOException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to read instant app android id file: ");
                stringBuilder.append(idFile);
                Slog.e(str, stringBuilder.toString(), e);
            }
        }
        return generateInstantAppAndroidIdLPw(packageName, userId);
    }

    private String generateInstantAppAndroidIdLPw(String packageName, int userId) {
        byte[] randomBytes = new byte[8];
        new SecureRandom().nextBytes(randomBytes);
        String id = ByteStringUtils.toHexString(randomBytes).toLowerCase(Locale.US);
        File appDir = getInstantApplicationDir(packageName, userId);
        if (appDir.exists() || appDir.mkdirs()) {
            File idFile = new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_ANDROID_ID_FILE);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(idFile);
                fos.write(id.getBytes());
                $closeResource(null, fos);
            } catch (IOException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error writing instant app android id file: ");
                stringBuilder.append(idFile);
                Slog.e(str, stringBuilder.toString(), e);
            } catch (Throwable th) {
                $closeResource(r5, fos);
            }
            return id;
        }
        Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
        return id;
    }

    public List<InstantAppInfo> getInstantAppsLPr(int userId) {
        List<InstantAppInfo> installedApps = getInstalledInstantApplicationsLPr(userId);
        List<InstantAppInfo> uninstalledApps = getUninstalledInstantApplicationsLPr(userId);
        if (installedApps == null) {
            return uninstalledApps;
        }
        if (uninstalledApps != null) {
            installedApps.addAll(uninstalledApps);
        }
        return installedApps;
    }

    public void onPackageInstalledLPw(Package pkg, int[] userIds) {
        Package packageR = pkg;
        int[] iArr = userIds;
        PackageSetting ps = packageR.mExtras;
        if (ps != null) {
            for (int userId : iArr) {
                if (this.mService.mPackages.get(packageR.packageName) != null && ps.getInstalled(userId)) {
                    propagateInstantAppPermissionsIfNeeded(packageR, userId);
                    if (ps.getInstantApp(userId)) {
                        addInstantAppLPw(userId, ps.appId);
                    }
                    removeUninstalledInstantAppStateLPw(new -$$Lambda$InstantAppRegistry$o-Qxi7Gaam-yhhMK-IMWv499oME(packageR), userId);
                    File instantAppDir = getInstantApplicationDir(packageR.packageName, userId);
                    new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
                    new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
                    File currentCookieFile = peekInstantCookieFile(packageR.packageName, userId);
                    if (currentCookieFile == null) {
                        continue;
                    } else {
                        String cookieName = currentCookieFile.getName();
                        String currentCookieSha256 = cookieName.substring(INSTANT_APP_COOKIE_FILE_PREFIX.length(), cookieName.length() - INSTANT_APP_COOKIE_FILE_SIFFIX.length());
                        if (!packageR.mSigningDetails.checkCapability(currentCookieSha256, 1)) {
                            String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(packageR.mSigningDetails.signatures);
                            int length = signaturesSha256Digests.length;
                            int i = 0;
                            while (i < length) {
                                if (!signaturesSha256Digests[i].equals(currentCookieSha256)) {
                                    i++;
                                } else {
                                    return;
                                }
                            }
                            String str = LOG_TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Signature for package ");
                            stringBuilder.append(packageR.packageName);
                            stringBuilder.append(" changed - dropping cookie");
                            Slog.i(str, stringBuilder.toString());
                            this.mCookiePersistence.cancelPendingPersistLPw(packageR, userId);
                            currentCookieFile.delete();
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }

    public void onPackageUninstalledLPw(Package pkg, int[] userIds) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            for (int userId : userIds) {
                if (this.mService.mPackages.get(pkg.packageName) == null || !ps.getInstalled(userId)) {
                    if (ps.getInstantApp(userId)) {
                        addUninstalledInstantAppLPw(pkg, userId);
                        removeInstantAppLPw(userId, ps.appId);
                    } else {
                        deleteDir(getInstantApplicationDir(pkg.packageName, userId));
                        this.mCookiePersistence.cancelPendingPersistLPw(pkg, userId);
                        removeAppLPw(userId, ps.appId);
                    }
                }
            }
        }
    }

    public void onUserRemovedLPw(int userId) {
        if (this.mUninstalledInstantApps != null) {
            this.mUninstalledInstantApps.remove(userId);
            if (this.mUninstalledInstantApps.size() <= 0) {
                this.mUninstalledInstantApps = null;
            }
        }
        if (this.mInstalledInstantAppUids != null) {
            this.mInstalledInstantAppUids.remove(userId);
            if (this.mInstalledInstantAppUids.size() <= 0) {
                this.mInstalledInstantAppUids = null;
            }
        }
        if (this.mInstantGrants != null) {
            this.mInstantGrants.remove(userId);
            if (this.mInstantGrants.size() <= 0) {
                this.mInstantGrants = null;
            }
        }
        deleteDir(getInstantApplicationsDir(userId));
    }

    public boolean isInstantAccessGranted(int userId, int targetAppId, int instantAppId) {
        if (this.mInstantGrants == null) {
            return false;
        }
        SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
        if (targetAppList == null) {
            return false;
        }
        SparseBooleanArray instantGrantList = (SparseBooleanArray) targetAppList.get(targetAppId);
        if (instantGrantList == null) {
            return false;
        }
        return instantGrantList.get(instantAppId);
    }

    /* JADX WARNING: Missing block: B:29:0x0070, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void grantInstantAccessLPw(int userId, Intent intent, int targetAppId, int instantAppId) {
        if (this.mInstalledInstantAppUids != null) {
            SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
            if (instantAppList != null && instantAppList.get(instantAppId) && !instantAppList.get(targetAppId)) {
                if (intent != null && "android.intent.action.VIEW".equals(intent.getAction())) {
                    Set<String> categories = intent.getCategories();
                    if (categories != null && categories.contains("android.intent.category.BROWSABLE")) {
                        return;
                    }
                }
                if (this.mInstantGrants == null) {
                    this.mInstantGrants = new SparseArray();
                }
                SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
                if (targetAppList == null) {
                    targetAppList = new SparseArray();
                    this.mInstantGrants.put(userId, targetAppList);
                }
                SparseBooleanArray instantGrantList = (SparseBooleanArray) targetAppList.get(targetAppId);
                if (instantGrantList == null) {
                    instantGrantList = new SparseBooleanArray();
                    targetAppList.put(targetAppId, instantGrantList);
                }
                instantGrantList.put(instantAppId, true);
            }
        }
    }

    public void addInstantAppLPw(int userId, int instantAppId) {
        if (this.mInstalledInstantAppUids == null) {
            this.mInstalledInstantAppUids = new SparseArray();
        }
        SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
        if (instantAppList == null) {
            instantAppList = new SparseBooleanArray();
            this.mInstalledInstantAppUids.put(userId, instantAppList);
        }
        instantAppList.put(instantAppId, true);
    }

    private void removeInstantAppLPw(int userId, int instantAppId) {
        if (this.mInstalledInstantAppUids != null) {
            SparseBooleanArray instantAppList = (SparseBooleanArray) this.mInstalledInstantAppUids.get(userId);
            if (instantAppList != null) {
                instantAppList.delete(instantAppId);
                if (this.mInstantGrants != null) {
                    SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
                    if (targetAppList != null) {
                        for (int i = targetAppList.size() - 1; i >= 0; i--) {
                            ((SparseBooleanArray) targetAppList.valueAt(i)).delete(instantAppId);
                        }
                    }
                }
            }
        }
    }

    private void removeAppLPw(int userId, int targetAppId) {
        if (this.mInstantGrants != null) {
            SparseArray<SparseBooleanArray> targetAppList = (SparseArray) this.mInstantGrants.get(userId);
            if (targetAppList != null) {
                targetAppList.delete(targetAppId);
            }
        }
    }

    private void addUninstalledInstantAppLPw(Package pkg, int userId) {
        InstantAppInfo uninstalledApp = createInstantAppInfoForPackage(pkg, userId, null);
        if (uninstalledApp != null) {
            if (this.mUninstalledInstantApps == null) {
                this.mUninstalledInstantApps = new SparseArray();
            }
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates == null) {
                uninstalledAppStates = new ArrayList();
                this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
            }
            uninstalledAppStates.add(new UninstalledInstantAppState(uninstalledApp, System.currentTimeMillis()));
            writeUninstalledInstantAppMetadata(uninstalledApp, userId);
            writeInstantApplicationIconLPw(pkg, userId);
        }
    }

    private void writeInstantApplicationIconLPw(Package pkg, int userId) {
        if (getInstantApplicationDir(pkg.packageName, userId).exists()) {
            Bitmap bitmap;
            Drawable icon = pkg.applicationInfo.loadIcon(this.mService.mContext.getPackageManager());
            if (icon instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) icon).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                icon.draw(canvas);
            }
            FileOutputStream out;
            try {
                out = new FileOutputStream(new File(getInstantApplicationDir(pkg.packageName, userId), INSTANT_APP_ICON_FILE));
                bitmap.compress(CompressFormat.PNG, 100, out);
                $closeResource(null, out);
            } catch (Exception e) {
                Slog.e(LOG_TAG, "Error writing instant app icon", e);
            } catch (Throwable th) {
                $closeResource(r5, out);
            }
        }
    }

    boolean hasInstantApplicationMetadataLPr(String packageName, int userId) {
        boolean z = false;
        if (packageName == null) {
            return false;
        }
        if (hasUninstalledInstantAppStateLPr(packageName, userId) || hasInstantAppMetadataLPr(packageName, userId)) {
            z = true;
        }
        return z;
    }

    public void deleteInstantApplicationMetadataLPw(String packageName, int userId) {
        if (packageName != null) {
            removeUninstalledInstantAppStateLPw(new -$$Lambda$InstantAppRegistry$eaYsiecM_Rq6dliDvliwVtj695o(packageName), userId);
            File instantAppDir = getInstantApplicationDir(packageName, userId);
            new File(instantAppDir, INSTANT_APP_METADATA_FILE).delete();
            new File(instantAppDir, INSTANT_APP_ICON_FILE).delete();
            new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).delete();
            File cookie = peekInstantCookieFile(packageName, userId);
            if (cookie != null) {
                cookie.delete();
            }
        }
    }

    private void removeUninstalledInstantAppStateLPw(Predicate<UninstalledInstantAppState> criteria, int userId) {
        if (this.mUninstalledInstantApps != null) {
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                for (int i = uninstalledAppStates.size() - 1; i >= 0; i--) {
                    if (criteria.test((UninstalledInstantAppState) uninstalledAppStates.get(i))) {
                        uninstalledAppStates.remove(i);
                        if (uninstalledAppStates.isEmpty()) {
                            this.mUninstalledInstantApps.remove(userId);
                            if (this.mUninstalledInstantApps.size() <= 0) {
                                this.mUninstalledInstantApps = null;
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean hasUninstalledInstantAppStateLPr(String packageName, int userId) {
        if (this.mUninstalledInstantApps == null) {
            return false;
        }
        List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
        if (uninstalledAppStates == null) {
            return false;
        }
        int appCount = uninstalledAppStates.size();
        for (int i = 0; i < appCount; i++) {
            if (packageName.equals(((UninstalledInstantAppState) uninstalledAppStates.get(i)).mInstantAppInfo.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInstantAppMetadataLPr(String packageName, int userId) {
        File instantAppDir = getInstantApplicationDir(packageName, userId);
        return new File(instantAppDir, INSTANT_APP_METADATA_FILE).exists() || new File(instantAppDir, INSTANT_APP_ICON_FILE).exists() || new File(instantAppDir, INSTANT_APP_ANDROID_ID_FILE).exists() || peekInstantCookieFile(packageName, userId) != null;
    }

    void pruneInstantApps() {
        try {
            pruneInstantApps(JobStatus.NO_LATEST_RUNTIME, Global.getLong(this.mService.mContext.getContentResolver(), "installed_instant_app_max_cache_period", 15552000000L), Global.getLong(this.mService.mContext.getContentResolver(), "uninstalled_instant_app_max_cache_period", 15552000000L));
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed and uninstalled instant apps", e);
        }
    }

    boolean pruneInstalledInstantApps(long neededSpace, long maxInstalledCacheDuration) {
        try {
            return pruneInstantApps(neededSpace, maxInstalledCacheDuration, JobStatus.NO_LATEST_RUNTIME);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed instant apps", e);
            return false;
        }
    }

    boolean pruneUninstalledInstantApps(long neededSpace, long maxUninstalledCacheDuration) {
        try {
            return pruneInstantApps(neededSpace, JobStatus.NO_LATEST_RUNTIME, maxUninstalledCacheDuration);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning uninstalled instant apps", e);
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:43:0x00b8, code:
            r4 = r0;
     */
    /* JADX WARNING: Missing block: B:44:0x00b9, code:
            if (r13 == null) goto L_0x00e7;
     */
    /* JADX WARNING: Missing block: B:45:0x00bb, code:
            r0 = r13.size();
            r6 = 0;
     */
    /* JADX WARNING: Missing block: B:46:0x00c0, code:
            if (r6 >= r0) goto L_0x00e7;
     */
    /* JADX WARNING: Missing block: B:48:0x00d9, code:
            if (r1.mService.deletePackageX((java.lang.String) r13.get(r6), -1, 0, 2) != 1) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:50:0x00e1, code:
            if (r5.getUsableSpace() < r30) goto L_0x00e4;
     */
    /* JADX WARNING: Missing block: B:51:0x00e3, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:52:0x00e4, code:
            r6 = r6 + 1;
     */
    /* JADX WARNING: Missing block: B:53:0x00e7, code:
            r6 = r1.mService.mPackages;
     */
    /* JADX WARNING: Missing block: B:54:0x00eb, code:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:56:?, code:
            r0 = com.android.server.pm.UserManagerService.getInstance().getUserIds();
            r7 = r0.length;
            r8 = 0;
     */
    /* JADX WARNING: Missing block: B:57:0x00f6, code:
            if (r8 >= r7) goto L_0x017e;
     */
    /* JADX WARNING: Missing block: B:58:0x00f8, code:
            r9 = r0[r8];
            r1.removeUninstalledInstantAppStateLPw(new com.android.server.pm.-$$Lambda$InstantAppRegistry$BuKCbLr_MGBazMPl54-pWTuGHYY(r2), r9);
            r10 = getInstantApplicationsDir(r9);
     */
    /* JADX WARNING: Missing block: B:59:0x010a, code:
            if (r10.exists() != false) goto L_0x0115;
     */
    /* JADX WARNING: Missing block: B:60:0x010d, code:
            r25 = r0;
     */
    /* JADX WARNING: Missing block: B:61:0x0115, code:
            r11 = r10.listFiles();
     */
    /* JADX WARNING: Missing block: B:62:0x0119, code:
            if (r11 != null) goto L_0x011c;
     */
    /* JADX WARNING: Missing block: B:63:0x011c, code:
            r12 = r11.length;
            r25 = r0;
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:64:0x0120, code:
            if (r0 >= r12) goto L_0x0172;
     */
    /* JADX WARNING: Missing block: B:65:0x0122, code:
            r1 = r11[r0];
     */
    /* JADX WARNING: Missing block: B:66:0x012c, code:
            if (r1.isDirectory() != false) goto L_0x0135;
     */
    /* JADX WARNING: Missing block: B:67:0x012e, code:
            r27 = r4;
            r28 = r7;
     */
    /* JADX WARNING: Missing block: B:69:0x0135, code:
            r27 = r4;
     */
    /* JADX WARNING: Missing block: B:71:?, code:
            r28 = r7;
            r4 = new java.io.File(r1, INSTANT_APP_METADATA_FILE);
     */
    /* JADX WARNING: Missing block: B:72:0x0145, code:
            if (r4.exists() != false) goto L_0x0148;
     */
    /* JADX WARNING: Missing block: B:74:0x0154, code:
            if ((java.lang.System.currentTimeMillis() - r4.lastModified()) <= r2) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:75:0x0156, code:
            deleteDir(r1);
     */
    /* JADX WARNING: Missing block: B:76:0x015f, code:
            if (r5.getUsableSpace() < r30) goto L_0x0133;
     */
    /* JADX WARNING: Missing block: B:77:0x0161, code:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:79:0x0163, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:80:0x0164, code:
            r0 = r0 + 1;
            r4 = r27;
            r7 = r28;
            r1 = r29;
     */
    /* JADX WARNING: Missing block: B:81:0x0172, code:
            r8 = r8 + 1;
            r0 = r25;
            r4 = r4;
            r7 = r7;
            r1 = r29;
     */
    /* JADX WARNING: Missing block: B:82:0x017e, code:
            r27 = r4;
     */
    /* JADX WARNING: Missing block: B:83:0x0180, code:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:85:0x0182, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:86:0x0183, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:87:0x0184, code:
            r27 = r4;
     */
    /* JADX WARNING: Missing block: B:88:0x0186, code:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:89:0x0187, code:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:90:0x0188, code:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean pruneInstantApps(long neededSpace, long maxInstalledCacheDuration, long maxUninstalledCacheDuration) throws IOException {
        Throwable th;
        List<String> list;
        InstantAppRegistry instantAppRegistry = this;
        long j = maxUninstalledCacheDuration;
        StorageManager storage = (StorageManager) instantAppRegistry.mService.mContext.getSystemService(StorageManager.class);
        File file = storage.findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        if (file.getUsableSpace() >= neededSpace) {
            return true;
        }
        long now = System.currentTimeMillis();
        synchronized (instantAppRegistry.mService.mPackages) {
            StorageManager storage2;
            long now2;
            try {
                int[] allUsers = PackageManagerService.sUserManager.getUserIds();
                int packageCount = instantAppRegistry.mService.mPackages.size();
                List<String> packagesToDelete = null;
                int i = 0;
                while (i < packageCount) {
                    try {
                        Package pkg = (Package) instantAppRegistry.mService.mPackages.valueAt(i);
                        if (now - pkg.getLatestPackageUseTimeInMills() >= maxInstalledCacheDuration && (pkg.mExtras instanceof PackageSetting)) {
                            PackageSetting ps = pkg.mExtras;
                            boolean installedOnlyAsInstantApp = false;
                            storage2 = storage;
                            try {
                                now2 = now;
                                for (int userId : allUsers) {
                                    if (ps.getInstalled(userId)) {
                                        if (!ps.getInstantApp(userId)) {
                                            installedOnlyAsInstantApp = false;
                                            break;
                                        }
                                        installedOnlyAsInstantApp = true;
                                    }
                                }
                                if (installedOnlyAsInstantApp) {
                                    if (packagesToDelete == null) {
                                        packagesToDelete = new ArrayList();
                                    }
                                    packagesToDelete.add(pkg.packageName);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } else {
                            storage2 = storage;
                            now2 = now;
                        }
                        i++;
                        storage = storage2;
                        now = now2;
                    } catch (Throwable th3) {
                        th = th3;
                        storage2 = storage;
                        now2 = now;
                        list = packagesToDelete;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        throw th;
                    }
                }
                storage2 = storage;
                now2 = now;
                if (packagesToDelete != null) {
                    packagesToDelete.sort(new -$$Lambda$InstantAppRegistry$UOn4sUy4zBQuofxUbY8RBYhkNSE(instantAppRegistry));
                }
            } catch (Throwable th5) {
                th = th5;
                storage2 = storage;
                now2 = now;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    public static /* synthetic */ int lambda$pruneInstantApps$2(InstantAppRegistry instantAppRegistry, String lhs, String rhs) {
        Package lhsPkg = (Package) instantAppRegistry.mService.mPackages.get(lhs);
        Package rhsPkg = (Package) instantAppRegistry.mService.mPackages.get(rhs);
        if (lhsPkg == null && rhsPkg == null) {
            return 0;
        }
        if (lhsPkg == null) {
            return -1;
        }
        if (rhsPkg == null || lhsPkg.getLatestPackageUseTimeInMills() > rhsPkg.getLatestPackageUseTimeInMills()) {
            return 1;
        }
        if (lhsPkg.getLatestPackageUseTimeInMills() < rhsPkg.getLatestPackageUseTimeInMills()) {
            return -1;
        }
        if (!(lhsPkg.mExtras instanceof PackageSetting) || !(rhsPkg.mExtras instanceof PackageSetting)) {
            return 0;
        }
        if (lhsPkg.mExtras.firstInstallTime > rhsPkg.mExtras.firstInstallTime) {
            return 1;
        }
        return -1;
    }

    static /* synthetic */ boolean lambda$pruneInstantApps$3(long maxUninstalledCacheDuration, UninstalledInstantAppState state) {
        return System.currentTimeMillis() - state.mTimestamp > maxUninstalledCacheDuration;
    }

    private List<InstantAppInfo> getInstalledInstantApplicationsLPr(int userId) {
        List<InstantAppInfo> result = null;
        int packageCount = this.mService.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            Package pkg = (Package) this.mService.mPackages.valueAt(i);
            PackageSetting ps = pkg.mExtras;
            if (ps != null && ps.getInstantApp(userId)) {
                InstantAppInfo info = createInstantAppInfoForPackage(pkg, userId, true);
                if (info != null) {
                    if (result == null) {
                        result = new ArrayList();
                    }
                    result.add(info);
                }
            }
        }
        return result;
    }

    private InstantAppInfo createInstantAppInfoForPackage(Package pkg, int userId, boolean addApplicationInfo) {
        PackageSetting ps = pkg.mExtras;
        if (ps == null || !ps.getInstalled(userId)) {
            return null;
        }
        String[] requestedPermissions = new String[pkg.requestedPermissions.size()];
        pkg.requestedPermissions.toArray(requestedPermissions);
        Set<String> permissions = ps.getPermissionsState().getPermissions(userId);
        String[] grantedPermissions = new String[permissions.size()];
        permissions.toArray(grantedPermissions);
        if (addApplicationInfo) {
            return new InstantAppInfo(pkg.applicationInfo, requestedPermissions, grantedPermissions);
        }
        return new InstantAppInfo(pkg.applicationInfo.packageName, pkg.applicationInfo.loadLabel(this.mService.mContext.getPackageManager()), requestedPermissions, grantedPermissions);
    }

    private List<InstantAppInfo> getUninstalledInstantApplicationsLPr(int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates = getUninstalledInstantAppStatesLPr(userId);
        if (uninstalledAppStates == null || uninstalledAppStates.isEmpty()) {
            return null;
        }
        List<InstantAppInfo> uninstalledApps = null;
        int stateCount = uninstalledAppStates.size();
        for (int i = 0; i < stateCount; i++) {
            UninstalledInstantAppState uninstalledAppState = (UninstalledInstantAppState) uninstalledAppStates.get(i);
            if (uninstalledApps == null) {
                uninstalledApps = new ArrayList();
            }
            uninstalledApps.add(uninstalledAppState.mInstantAppInfo);
        }
        return uninstalledApps;
    }

    private void propagateInstantAppPermissionsIfNeeded(Package pkg, int userId) {
        InstantAppInfo appInfo = peekOrParseUninstalledInstantAppInfo(pkg.packageName, userId);
        if (appInfo != null && !ArrayUtils.isEmpty(appInfo.getGrantedPermissions())) {
            long identity = Binder.clearCallingIdentity();
            try {
                for (String grantedPermission : appInfo.getGrantedPermissions()) {
                    if (this.mService.mSettings.canPropagatePermissionToInstantApp(grantedPermission) && pkg.requestedPermissions.contains(grantedPermission)) {
                        this.mService.grantRuntimePermission(pkg.packageName, grantedPermission, userId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private InstantAppInfo peekOrParseUninstalledInstantAppInfo(String packageName, int userId) {
        if (this.mUninstalledInstantApps != null) {
            List<UninstalledInstantAppState> uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                int appCount = uninstalledAppStates.size();
                for (int i = 0; i < appCount; i++) {
                    UninstalledInstantAppState uninstalledAppState = (UninstalledInstantAppState) uninstalledAppStates.get(i);
                    if (uninstalledAppState.mInstantAppInfo.getPackageName().equals(packageName)) {
                        return uninstalledAppState.mInstantAppInfo;
                    }
                }
            }
        }
        UninstalledInstantAppState uninstalledAppState2 = parseMetadataFile(new File(getInstantApplicationDir(packageName, userId), INSTANT_APP_METADATA_FILE));
        if (uninstalledAppState2 == null) {
            return null;
        }
        return uninstalledAppState2.mInstantAppInfo;
    }

    private List<UninstalledInstantAppState> getUninstalledInstantAppStatesLPr(int userId) {
        List<UninstalledInstantAppState> uninstalledAppStates = null;
        if (this.mUninstalledInstantApps != null) {
            uninstalledAppStates = (List) this.mUninstalledInstantApps.get(userId);
            if (uninstalledAppStates != null) {
                return uninstalledAppStates;
            }
        }
        File instantAppsDir = getInstantApplicationsDir(userId);
        if (instantAppsDir.exists()) {
            File[] files = instantAppsDir.listFiles();
            if (files != null) {
                for (File instantDir : files) {
                    if (instantDir.isDirectory()) {
                        UninstalledInstantAppState uninstalledAppState = parseMetadataFile(new File(instantDir, INSTANT_APP_METADATA_FILE));
                        if (uninstalledAppState != null) {
                            if (uninstalledAppStates == null) {
                                uninstalledAppStates = new ArrayList();
                            }
                            uninstalledAppStates.add(uninstalledAppState);
                        }
                    }
                }
            }
        }
        if (uninstalledAppStates != null) {
            if (this.mUninstalledInstantApps == null) {
                this.mUninstalledInstantApps = new SparseArray();
            }
            this.mUninstalledInstantApps.put(userId, uninstalledAppStates);
        }
        return uninstalledAppStates;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x003b A:{Splitter: B:6:0x001f, ExcHandler: org.xmlpull.v1.XmlPullParserException (r5_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:11:0x003b, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            r7 = new java.lang.StringBuilder();
            r7.append("Failed parsing instant metadata file: ");
            r7.append(r9);
     */
    /* JADX WARNING: Missing block: B:14:0x0052, code:
            throw new java.lang.IllegalStateException(r7.toString(), r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static UninstalledInstantAppState parseMetadataFile(File metadataFile) {
        if (!metadataFile.exists()) {
            return null;
        }
        try {
            FileInputStream in = new AtomicFile(metadataFile).openRead();
            File instantDir = metadataFile.getParentFile();
            long timestamp = metadataFile.lastModified();
            String packageName = instantDir.getName();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(in, StandardCharsets.UTF_8.name());
                UninstalledInstantAppState uninstalledInstantAppState = new UninstalledInstantAppState(parseMetadata(parser, packageName), timestamp);
                IoUtils.closeQuietly(in);
                return uninstalledInstantAppState;
            } catch (Exception e) {
            } catch (Throwable th) {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(LOG_TAG, "No instant metadata file");
            return null;
        }
    }

    private static File computeInstantCookieFile(String packageName, String sha256Digest, int userId) {
        File appDir = getInstantApplicationDir(packageName, userId);
        String cookieFile = new StringBuilder();
        cookieFile.append(INSTANT_APP_COOKIE_FILE_PREFIX);
        cookieFile.append(sha256Digest);
        cookieFile.append(INSTANT_APP_COOKIE_FILE_SIFFIX);
        return new File(appDir, cookieFile.toString());
    }

    private static File peekInstantCookieFile(String packageName, int userId) {
        File appDir = getInstantApplicationDir(packageName, userId);
        if (!appDir.exists()) {
            return null;
        }
        File[] files = appDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.isDirectory() && file.getName().startsWith(INSTANT_APP_COOKIE_FILE_PREFIX) && file.getName().endsWith(INSTANT_APP_COOKIE_FILE_SIFFIX)) {
                return file;
            }
        }
        return null;
    }

    private static InstantAppInfo parseMetadata(XmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if ("package".equals(parser.getName())) {
                return parsePackage(parser, packageName);
            }
        }
        return null;
    }

    private static InstantAppInfo parsePackage(XmlPullParser parser, String packageName) throws IOException, XmlPullParserException {
        String label = parser.getAttributeValue(null, ATTR_LABEL);
        List<String> outRequestedPermissions = new ArrayList();
        List<String> outGrantedPermissions = new ArrayList();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PERMISSIONS.equals(parser.getName())) {
                parsePermissions(parser, outRequestedPermissions, outGrantedPermissions);
            }
        }
        String[] requestedPermissions = new String[outRequestedPermissions.size()];
        outRequestedPermissions.toArray(requestedPermissions);
        String[] grantedPermissions = new String[outGrantedPermissions.size()];
        outGrantedPermissions.toArray(grantedPermissions);
        return new InstantAppInfo(packageName, label, requestedPermissions, grantedPermissions);
    }

    private static void parsePermissions(XmlPullParser parser, List<String> outRequestedPermissions, List<String> outGrantedPermissions) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (TAG_PERMISSION.equals(parser.getName())) {
                String permission = XmlUtils.readStringAttribute(parser, "name");
                outRequestedPermissions.add(permission);
                if (XmlUtils.readBooleanAttribute(parser, ATTR_GRANTED)) {
                    outGrantedPermissions.add(permission);
                }
            }
        }
    }

    private void writeUninstalledInstantAppMetadata(InstantAppInfo instantApp, int userId) {
        File appDir = getInstantApplicationDir(instantApp.getPackageName(), userId);
        if (appDir.exists() || appDir.mkdirs()) {
            AtomicFile destination = new AtomicFile(new File(appDir, INSTANT_APP_METADATA_FILE));
            FileOutputStream out = null;
            try {
                out = destination.startWrite();
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(out, StandardCharsets.UTF_8.name());
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startDocument(null, Boolean.valueOf(true));
                serializer.startTag(null, "package");
                serializer.attribute(null, ATTR_LABEL, instantApp.loadLabel(this.mService.mContext.getPackageManager()).toString());
                serializer.startTag(null, TAG_PERMISSIONS);
                for (String permission : instantApp.getRequestedPermissions()) {
                    serializer.startTag(null, TAG_PERMISSION);
                    serializer.attribute(null, "name", permission);
                    if (ArrayUtils.contains(instantApp.getGrantedPermissions(), permission)) {
                        serializer.attribute(null, ATTR_GRANTED, String.valueOf(true));
                    }
                    serializer.endTag(null, TAG_PERMISSION);
                }
                serializer.endTag(null, TAG_PERMISSIONS);
                serializer.endTag(null, "package");
                serializer.endDocument();
                destination.finishWrite(out);
            } catch (Throwable th) {
                IoUtils.closeQuietly(out);
            }
            IoUtils.closeQuietly(out);
        }
    }

    private static File getInstantApplicationsDir(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), INSTANT_APPS_FOLDER);
    }

    private static File getInstantApplicationDir(String packageName, int userId) {
        return new File(getInstantApplicationsDir(userId), packageName);
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDir(file);
            }
        }
        dir.delete();
    }
}

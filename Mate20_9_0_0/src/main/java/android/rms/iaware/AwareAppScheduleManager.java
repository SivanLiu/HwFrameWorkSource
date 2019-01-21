package android.rms.iaware;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.HwResources;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AwareAppScheduleManager {
    private static final String APP_WECHAT_NAME = "com.tencent.mm";
    private static final String ART64_PATH = "/oat/arm64/base.art";
    private static final String ART_PATH = "/oat/arm/base.art";
    private static final String BASE_APK = "/base.apk";
    private static final String DATA_APP = "/data/app/";
    private static final String FILE_NAME_DECODEBITMAP = "hw_cached_resid.list";
    private static final int FINISH_HANDLER_DELAY_TIME = 10000;
    private static final int FINISH_LEARNED_DELAY_TIME = 5000;
    private static final String FRAMEWORK_RES_RESOURCE = "/system/framework/framework-res.apk";
    private static final int LENGTH_POLICY = 8;
    private static final int MAX_CACHED_SIZE = 256;
    private static final int MAX_RESID_COUNT = 20;
    private static final int MSG_FINISH_HANDLER = 3;
    private static final int MSG_FINISH_LEARNED = 2;
    private static final int MSG_READ_FROM_DISK = 1;
    private static final long OPT_VALID_TIME = 120000;
    private static final String PRIMARY_PROF = "primary.prof";
    private static final String SPLIT_NAME = "/";
    private static final int STATUS_FINISH_HANDLER = 3;
    private static final int STATUS_FINISH_LEARN = 2;
    private static final int STATUS_INIT = 0;
    private static final int STATUS_INIT_SUCCESS = 1;
    private static final String TAG = "AwareAppScheduleManager";
    private static final String TEMP_ART64_PATH = "/oat/arm64/temp.art";
    private static final String TEMP_ART_PATH = "/oat/arm/temp.art";
    private static final String TEMP_ODEX64_PATH = "/oat/arm64/temp.odex";
    private static final String TEMP_ODEX_PATH = "/oat/arm/temp.odex";
    private static final int TRY_MAX_TIMES = 25;
    private static final String VDEX64_PATH = "/oat/arm64/base.vdex";
    private static final String VDEX_PATH = "/oat/arm/base.vdex";
    private static AwareAppScheduleManager sInstance;
    private ApplicationInfo mAppInfo;
    private int mAppVersionCode = 0;
    private File mCacheDir;
    private Map<Integer, Boolean> mCacheDrawableResIds = new HashMap();
    private Context mContext;
    private Object mDecodeBitmapFileLock = new Object();
    private int mDecodeTime = 10000000;
    private AtomicBoolean mDrawableCacheFeature = new AtomicBoolean(false);
    private SparseArray<Drawable> mDrawableCaches = new SparseArray();
    private volatile AppScheduleHandler mHandler = null;
    private HandlerThread mHandlerThread;
    private AtomicBoolean mHasNewResId = new AtomicBoolean(false);
    private AtomicInteger mLoadDrawableResId = new AtomicInteger(0);
    private String mPackageName;
    private AtomicBoolean mResultShouldReplaced = new AtomicBoolean(false);
    private long mStartTime = 0;
    private AtomicInteger mStatus = new AtomicInteger(0);
    private String mSystemVersion = "";
    private Thread mUiThread;
    private String mWechatScanActivity;
    private boolean mWechatScanOpt = false;

    private final class AppScheduleHandler extends Handler {
        public AppScheduleHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AwareAppScheduleManager.this.readFromDisk();
                    return;
                case 2:
                    AwareAppScheduleManager.this.finishLearn();
                    return;
                case 3:
                    AwareAppScheduleManager.this.finishHandler();
                    return;
                default:
                    return;
            }
        }
    }

    private class AppScheduleSDKCallback extends Binder implements IInterface {
        private static final String SDK_CALLBACK_DESCRIPTOR = "android.rms.iaware.AppScheduleSDKCallback";
        private static final int TRANSACTION_initAppSchedulePolicy = 1;

        public AppScheduleSDKCallback() {
            attachInterface(this, SDK_CALLBACK_DESCRIPTOR);
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (System.currentTimeMillis() - AwareAppScheduleManager.this.mStartTime > 5000) {
                AwareLog.w(AwareAppScheduleManager.TAG, "init policy is later!");
                return false;
            } else if (code != 1) {
                return super.onTransact(code, data, reply, flags);
            } else {
                try {
                    data.enforceInterface(SDK_CALLBACK_DESCRIPTOR);
                    int[] policy = new int[8];
                    data.readIntArray(policy);
                    AwareAppScheduleManager.this.mWechatScanActivity = data.readString();
                    if (policy.length != 8) {
                        AwareLog.e(AwareAppScheduleManager.TAG, "policy is error");
                        return false;
                    }
                    initAppSchedulePolicy(policy);
                    return true;
                } catch (SecurityException e) {
                    AwareLog.e(AwareAppScheduleManager.TAG, "enforceInterface SDK_CALLBACK_DESCRIPTOR failed");
                    return false;
                }
            }
        }

        public IBinder asBinder() {
            return this;
        }

        private void initAppSchedulePolicy(int[] policy) {
            boolean z = false;
            String str;
            StringBuilder stringBuilder;
            if (policy[0] == 0) {
                AwareAppScheduleManager.this.mStatus.set(0);
                str = AwareAppScheduleManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("init: failed cause feature is disabled process:");
                stringBuilder.append(AwareAppScheduleManager.this.mPackageName);
                AwareLog.d(str, stringBuilder.toString());
                return;
            }
            AwareAppScheduleManager.this.mCacheDir = AwareAppScheduleManager.this.mContext.getFilesDir();
            AwareAppScheduleManager.this.mDrawableCacheFeature.set(1 == policy[1]);
            AwareAppScheduleManager.this.mDecodeTime = policy[2];
            AwareAppScheduleManager awareAppScheduleManager = AwareAppScheduleManager.this;
            if (AwareAppScheduleManager.APP_WECHAT_NAME.equals(AwareAppScheduleManager.this.mPackageName) && 1 == policy[3]) {
                z = true;
            }
            awareAppScheduleManager.mWechatScanOpt = z;
            AwareAppScheduleManager.this.mStatus.set(1);
            AwareAppScheduleManager.this.mSystemVersion = VERSION.INCREMENTAL;
            if (AwareAppScheduleManager.this.mDrawableCacheFeature.get() && AwareAppScheduleManager.this.mHandlerThread == null) {
                AwareAppScheduleManager.this.mHandlerThread = new HandlerThread("queued-work-looper-schedule-handler", 10);
                AwareAppScheduleManager.this.mHandlerThread.start();
                AwareAppScheduleManager.this.mHandler = new AppScheduleHandler(AwareAppScheduleManager.this.mHandlerThread.getLooper());
                AwareAppScheduleManager.this.mHandler.sendMessage(AwareAppScheduleManager.this.mHandler.obtainMessage(1));
                AwareAppScheduleManager.this.mHandler.sendMessageDelayed(AwareAppScheduleManager.this.mHandler.obtainMessage(2), 5000);
                AwareAppScheduleManager.this.mHandler.sendMessageDelayed(AwareAppScheduleManager.this.mHandler.obtainMessage(3), 10000);
            }
            AwareAppScheduleManager.this.compileArt(AwareAppScheduleManager.this.mAppInfo);
            str = AwareAppScheduleManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("init: success process:");
            stringBuilder.append(AwareAppScheduleManager.this.mPackageName);
            AwareLog.i(str, stringBuilder.toString());
        }
    }

    public static synchronized AwareAppScheduleManager getInstance() {
        AwareAppScheduleManager awareAppScheduleManager;
        synchronized (AwareAppScheduleManager.class) {
            if (sInstance == null) {
                sInstance = new AwareAppScheduleManager();
            }
            awareAppScheduleManager = sInstance;
        }
        return awareAppScheduleManager;
    }

    private AwareAppScheduleManager() {
    }

    private boolean hasBaseApk(ApplicationInfo appInfo) {
        return appInfo.sourceDir.contains(BASE_APK);
    }

    private boolean hasArt(ApplicationInfo appInfo) {
        String appPath = appInfo.sourceDir.substring(0, appInfo.sourceDir.lastIndexOf(SPLIT_NAME));
        File artFile = new File(appPath, ART_PATH);
        File art64File = new File(appPath, ART64_PATH);
        File artTempFile = new File(appPath, TEMP_ART_PATH);
        File art64TempFile = new File(appPath, TEMP_ART64_PATH);
        File odexTempFile = new File(appPath, TEMP_ODEX_PATH);
        File odex64TempFile = new File(appPath, TEMP_ODEX64_PATH);
        if (artFile.exists() || art64File.exists() || artTempFile.exists() || art64TempFile.exists() || odexTempFile.exists() || odex64TempFile.exists()) {
            return true;
        }
        return false;
    }

    private boolean IsValidTimeScope(File apkFile, String appPath, String cmpPath) {
        File cmpFile = new File(appPath, cmpPath);
        boolean z = false;
        if (!cmpFile.exists()) {
            return false;
        }
        long apkTime = apkFile.lastModified();
        long cmpTime = cmpFile.lastModified();
        if ((cmpTime > apkTime ? cmpTime - apkTime : 0) <= OPT_VALID_TIME) {
            z = true;
        }
        return z;
    }

    private boolean hasValidVdex(ApplicationInfo appInfo) {
        boolean z = false;
        String appPath = appInfo.sourceDir.substring(0, appInfo.sourceDir.lastIndexOf(SPLIT_NAME));
        File apkFile = new File(appPath, BASE_APK);
        if (!apkFile.exists()) {
            return false;
        }
        if (IsValidTimeScope(apkFile, appPath, VDEX_PATH) || IsValidTimeScope(apkFile, appPath, VDEX64_PATH)) {
            z = true;
        }
        return z;
    }

    private boolean hasValidProfile(ApplicationInfo appInfo) {
        File profileFile = new File(Environment.getDataProfilesDePackageDirectory(UserHandle.myUserId(), appInfo.packageName), PRIMARY_PROF);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("profile path ");
        stringBuilder.append(profileFile.getPath());
        stringBuilder.append(", length ");
        stringBuilder.append(profileFile.length());
        AwareLog.d(str, stringBuilder.toString());
        return profileFile.exists() && profileFile.isFile() && profileFile.length() > 0;
    }

    private boolean isDataApp(ApplicationInfo appInfo) {
        return appInfo.sourceDir.contains(DATA_APP);
    }

    private boolean needNotCompile(ApplicationInfo appInfo) {
        return appInfo.sourceDir == null || appInfo.packageName == null || !isDataApp(appInfo) || !hasBaseApk(appInfo) || hasArt(appInfo) || !hasValidVdex(appInfo);
    }

    private void compileArt(final ApplicationInfo appInfo) {
        String str;
        StringBuilder stringBuilder;
        if (!SystemProperties.getBoolean("persist.sys.aware.compile.enable", true)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName ");
            stringBuilder.append(appInfo.packageName);
            stringBuilder.append(" compile feature is closed. setprop persist.sys.aware.compile.enable true or false.");
            AwareLog.i(str, stringBuilder.toString());
        } else if (needNotCompile(appInfo)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName ");
            stringBuilder.append(appInfo.packageName);
            stringBuilder.append(", source dir ");
            stringBuilder.append(appInfo.sourceDir);
            AwareLog.d(str, stringBuilder.toString());
        } else {
            new Thread("queued-work-looper-fast-compile") {
                public void run() {
                    String str;
                    StringBuilder stringBuilder;
                    try {
                        str = AwareAppScheduleManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(appInfo.packageName);
                        stringBuilder.append(", Tread fast compile try begin");
                        AwareLog.d(str, stringBuilder.toString());
                        Thread.currentThread();
                        Thread.sleep(5000);
                        int i = 25;
                        while (i > 0) {
                            String str2 = AwareAppScheduleManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("try ");
                            stringBuilder2.append((25 - i) + 1);
                            stringBuilder2.append(", Tread wait profile");
                            AwareLog.d(str2, stringBuilder2.toString());
                            Thread.currentThread();
                            Thread.sleep(2000);
                            if (AwareAppScheduleManager.this.hasArt(appInfo)) {
                                AwareLog.d(AwareAppScheduleManager.TAG, "Tread artFile or art64File exists");
                                break;
                            } else if (AwareAppScheduleManager.this.hasValidProfile(appInfo)) {
                                str2 = AwareAppScheduleManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("try ");
                                stringBuilder2.append((25 - i) + 1);
                                stringBuilder2.append(", has valid profile, begin to compile.");
                                AwareLog.i(str2, stringBuilder2.toString());
                                boolean result = ActivityThread.getPackageManager().performDexOptMode(appInfo.packageName, true, "speed-profile-opt", false, true, null);
                                str2 = AwareAppScheduleManager.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("result ");
                                stringBuilder2.append(result);
                                stringBuilder2.append(", Thread fast-compile end");
                                AwareLog.i(str2, stringBuilder2.toString());
                                break;
                            } else {
                                i--;
                            }
                        }
                    } catch (RemoteException e) {
                        AwareLog.w(AwareAppScheduleManager.TAG, "fast_compile_thread synchronized process failed!");
                    } catch (InterruptedException e2) {
                        AwareLog.w(AwareAppScheduleManager.TAG, "fast_compile_thread interrupted");
                    }
                    str = AwareAppScheduleManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(appInfo.packageName);
                    stringBuilder.append(", Thread fast compile try end");
                    AwareLog.d(str, stringBuilder.toString());
                }
            }.start();
        }
    }

    public void init(String processName, Application app) {
        if (processName != null && app != null) {
            this.mContext = app.getBaseContext();
            if (this.mContext != null) {
                this.mAppInfo = this.mContext.getApplicationInfo();
                if (this.mAppInfo == null || this.mAppInfo.uid < 10000) {
                    AwareLog.d(TAG, "special uid caller");
                } else if (!isSystemUnRemovablePkg(this.mAppInfo)) {
                    this.mPackageName = this.mContext.getPackageName();
                    if (!processName.equals(this.mPackageName)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("not main process, processName:");
                        stringBuilder.append(processName);
                        stringBuilder.append(", mPackageName:");
                        stringBuilder.append(this.mPackageName);
                        AwareLog.d(str, stringBuilder.toString());
                    } else if (this.mStatus.get() > 0) {
                        AwareLog.w(TAG, "has enabled");
                    } else {
                        this.mStartTime = System.currentTimeMillis();
                        IAwareSdk.asyncReportDataWithCallback(3030, this.mPackageName, new AppScheduleSDKCallback(), this.mStartTime);
                        this.mAppVersionCode = this.mAppInfo.versionCode;
                        this.mUiThread = Thread.currentThread();
                    }
                }
            }
        }
    }

    private boolean isSystemUnRemovablePkg(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & 1) != 0 && (applicationInfo.hwFlags & 100663296) == 0;
    }

    /* JADX WARNING: Missing block: B:46:0x00c9, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:47:0x00ca, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:48:0x00cb, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Drawable getCacheDrawableFromAware(int resId, Resources wrapper, int cookie, AssetManager asset) {
        if (this.mDrawableCacheFeature == null || !this.mDrawableCacheFeature.get() || this.mResultShouldReplaced.get() || wrapper == null || this.mStatus.get() == 0 || this.mStatus.get() > 2 || this.mLoadDrawableResId.get() == resId) {
            return null;
        }
        Drawable dr;
        synchronized (this.mDrawableCaches) {
            dr = (Drawable) this.mDrawableCaches.get(resId);
        }
        if (dr != null) {
            if (!(wrapper instanceof HwResources) && !Resources.class.getName().equals(wrapper.getClass().getName())) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get cache drawable wrapper is not HwResources, resId: ");
                stringBuilder.append(resId);
                AwareLog.d(str, stringBuilder.toString());
                return null;
            } else if (!checkCookie(resId, cookie, asset)) {
                return null;
            } else {
                synchronized (this.mCacheDrawableResIds) {
                    if (!((Boolean) this.mCacheDrawableResIds.get(Integer.valueOf(resId))).booleanValue()) {
                        this.mCacheDrawableResIds.put(Integer.valueOf(resId), Boolean.valueOf(true));
                    }
                }
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("get cache drawable from aware success resId = ");
                stringBuilder2.append(resId);
                stringBuilder2.append(", packagename = ");
                stringBuilder2.append(this.mPackageName);
                AwareLog.i(str2, stringBuilder2.toString());
            }
        }
        return dr;
    }

    private boolean checkCookie(int resId, int cookie, AssetManager asset) {
        if (asset == null || cookie == 0 || asset.getApkAssets() == null || asset.getApkAssets().length < cookie) {
            return false;
        }
        String cookieName = asset.getApkAssets()[cookie - 1].getAssetPath();
        if (cookieName == null) {
            return false;
        }
        if (cookieName.equals(this.mAppInfo.sourceDir) || cookieName.equals(FRAMEWORK_RES_RESOURCE)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cache drawable cookieName is not main package, resId: ");
        stringBuilder.append(resId);
        stringBuilder.append(", cookieName:");
        stringBuilder.append(cookieName);
        AwareLog.d(str, stringBuilder.toString());
        return false;
    }

    /* JADX WARNING: Missing block: B:35:0x00c6, code skipped:
            if (r5.mHasNewResId.get() != false) goto L_0x00cd;
     */
    /* JADX WARNING: Missing block: B:36:0x00c8, code skipped:
            r5.mHasNewResId.set(true);
     */
    /* JADX WARNING: Missing block: B:37:0x00cd, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:43:0x00d2, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void postCacheDrawableToAware(int resId, Resources wrapper, long time, int cookie, AssetManager asset) {
        if (this.mDrawableCacheFeature != null && this.mDrawableCacheFeature.get() && time >= ((long) this.mDecodeTime) && this.mStatus.get() == 1 && wrapper != null) {
            if (Thread.currentThread() != this.mUiThread) {
                AwareLog.d(TAG, "postCacheDrawableToAware not mUiThread");
            } else if (this.mLoadDrawableResId.get() != resId) {
                String str;
                if (!(wrapper instanceof HwResources) && !Resources.class.getName().equals(wrapper.getClass().getName())) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("post cache drawable wrapper is not HwResources, resId: ");
                    stringBuilder.append(resId);
                    stringBuilder.append(", wrapper:");
                    stringBuilder.append(wrapper);
                    AwareLog.d(str, stringBuilder.toString());
                } else if (checkCookie(resId, cookie, asset)) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("post cache drawable res id to aware, resId = ");
                    stringBuilder2.append(resId);
                    stringBuilder2.append(", packagename = ");
                    stringBuilder2.append(this.mPackageName);
                    stringBuilder2.append(", cost time = ");
                    stringBuilder2.append(time);
                    Log.i(str, stringBuilder2.toString());
                    synchronized (this.mCacheDrawableResIds) {
                        if (this.mCacheDrawableResIds.containsKey(Integer.valueOf(resId))) {
                            return;
                        }
                        this.mCacheDrawableResIds.put(Integer.valueOf(resId), Boolean.valueOf(true));
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x006a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void hitDrawableCache(int resId) {
        if (this.mDrawableCacheFeature != null && this.mDrawableCacheFeature.get() && this.mStatus.get() != 0 && this.mStatus.get() <= 2) {
            synchronized (this.mCacheDrawableResIds) {
                if (this.mCacheDrawableResIds.containsKey(Integer.valueOf(resId))) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("get cache drawable from cachedDrawable, resid: ");
                    stringBuilder.append(resId);
                    AwareLog.d(str, stringBuilder.toString());
                    if (!((Boolean) this.mCacheDrawableResIds.get(Integer.valueOf(resId))).booleanValue()) {
                        this.mCacheDrawableResIds.put(Integer.valueOf(resId), Boolean.valueOf(true));
                    }
                }
            }
        }
    }

    public boolean getWechatScanOpt() {
        return this.mWechatScanOpt;
    }

    public String getWechatScanActivity() {
        return this.mWechatScanActivity;
    }

    /* JADX WARNING: Missing block: B:18:0x0098, code skipped:
            if (r1 != r2) goto L_0x00a9;
     */
    /* JADX WARNING: Missing block: B:19:0x009a, code skipped:
            android.rms.iaware.AwareLog.d(TAG, "hit rate is 0, clear learn result before and do not need to write");
            deleteFile(r7.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
     */
    /* JADX WARNING: Missing block: B:20:0x00a8, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:21:0x00a9, code skipped:
            if (r1 != 0) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:23:0x00b1, code skipped:
            if (r7.mHasNewResId.get() != false) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:24:0x00b3, code skipped:
            android.rms.iaware.AwareLog.d(TAG, "the learn result does not change, do not need to write");
     */
    /* JADX WARNING: Missing block: B:25:0x00ba, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:26:0x00bb, code skipped:
            writeToFile(r7.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP, r0);
     */
    /* JADX WARNING: Missing block: B:27:0x00c2, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeDrawable() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("begin write cache data to disk, packageName = ");
        stringBuilder.append(this.mPackageName);
        AwareLog.i(str, stringBuilder.toString());
        int notHitCount = 0;
        synchronized (this.mCacheDrawableResIds) {
            int size = this.mCacheDrawableResIds.size();
            if (size >= 1) {
                if (size <= 20) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(this.mSystemVersion);
                    sb.append("\n");
                    sb.append(this.mAppVersionCode);
                    for (Entry<Integer, Boolean> entry : this.mCacheDrawableResIds.entrySet()) {
                        if (((Boolean) entry.getValue()).booleanValue()) {
                            sb.append("\n");
                            sb.append(entry.getKey());
                        } else {
                            notHitCount++;
                        }
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("write cache resid to disk, packageName = ");
                    stringBuilder2.append(this.mPackageName);
                    stringBuilder2.append(", line: ");
                    stringBuilder2.append(this.mCacheDrawableResIds);
                    AwareLog.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    private void readFromDisk() {
        generateDrawable();
    }

    private void generateDrawable() {
        String str;
        StringBuilder stringBuilder;
        if (this.mDrawableCacheFeature.get()) {
            List<String> resIds = readFromFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP, 256);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("read cache resid from disk, packageName = ");
            stringBuilder2.append(this.mPackageName);
            stringBuilder2.append(", line: ");
            stringBuilder2.append(resIds);
            AwareLog.d(str2, stringBuilder2.toString());
            int size = resIds.size();
            String str3;
            if (size < 3) {
                if (size != 0) {
                    str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("read cached resid size not right , its:");
                    stringBuilder3.append(size);
                    AwareLog.w(str3, stringBuilder3.toString());
                }
                this.mResultShouldReplaced.set(true);
                deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                return;
            }
            try {
                StringBuilder stringBuilder4;
                if (!this.mSystemVersion.equals((String) resIds.get(0))) {
                    AwareLog.w(TAG, "system version has changed");
                    this.mResultShouldReplaced.set(true);
                    deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                } else if (Integer.parseInt((String) resIds.get(1)) != this.mAppVersionCode) {
                    str3 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(this.mPackageName);
                    stringBuilder4.append(" version has changed ");
                    AwareLog.d(str3, stringBuilder4.toString());
                    this.mResultShouldReplaced.set(true);
                    deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                } else {
                    int resId = 0;
                    int i = 22;
                    if (size <= 22) {
                        i = size;
                    }
                    size = i;
                    for (i = 2; i < size; i++) {
                        try {
                            resId = Integer.parseInt((String) resIds.get(i));
                            synchronized (this.mCacheDrawableResIds) {
                                if (!this.mCacheDrawableResIds.containsKey(Integer.valueOf(resId))) {
                                    this.mCacheDrawableResIds.put(Integer.valueOf(resId), Boolean.valueOf(false));
                                }
                            }
                            this.mLoadDrawableResId.set(resId);
                            Drawable dr = this.mContext.getDrawable(resId);
                            this.mLoadDrawableResId.set(0);
                            if (dr != null) {
                                synchronized (this.mDrawableCaches) {
                                    this.mDrawableCaches.put(resId, dr);
                                }
                            }
                        } catch (NumberFormatException e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("create drawable parse resId error:");
                            stringBuilder.append(resId);
                            AwareLog.w(str, stringBuilder.toString());
                            deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                        } catch (NotFoundException e2) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("create drawable not found error:");
                            stringBuilder.append(resId);
                            AwareLog.d(str, stringBuilder.toString());
                            deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                        } catch (RuntimeException e3) {
                            String str4 = TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("create drawable failed:");
                            stringBuilder4.append(resId);
                            AwareLog.d(str4, stringBuilder4.toString());
                            deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
                            return;
                        }
                    }
                }
            } catch (NumberFormatException e4) {
                String str5 = TAG;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append(this.mPackageName);
                stringBuilder5.append(" versioncode parse error ");
                AwareLog.w(str5, stringBuilder5.toString());
                this.mResultShouldReplaced.set(true);
                deleteFile(this.mDecodeBitmapFileLock, FILE_NAME_DECODEBITMAP);
            }
        }
    }

    private List<String> readFromFile(Object lock, String filename, int maxsize) {
        List<String> resIds;
        synchronized (lock) {
            resIds = new AtomicFileUtils(new File(this.mCacheDir, filename)).readFileLines(maxsize);
        }
        return resIds;
    }

    private void writeToFile(Object lock, String filename, StringBuilder sb) {
        synchronized (lock) {
            new AtomicFileUtils(new File(this.mCacheDir, filename)).writeFileLine(sb);
        }
    }

    private void deleteFile(Object lock, String filename) {
        synchronized (lock) {
            new AtomicFileUtils(new File(this.mCacheDir, filename)).deleteFile();
        }
    }

    private void finishHandler() {
        AwareLog.d(TAG, "finishHandler");
        this.mStatus.set(3);
        this.mHandler = null;
        writeDrawable();
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
        }
        synchronized (this.mDrawableCaches) {
            this.mDrawableCaches.clear();
        }
    }

    private void finishLearn() {
        AwareLog.d(TAG, "finishLearn");
        this.mStatus.set(2);
    }
}

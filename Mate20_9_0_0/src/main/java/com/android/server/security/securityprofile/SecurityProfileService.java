package com.android.server.security.securityprofile;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.media.projection.IMediaProjectionManager.Stub;
import android.media.projection.MediaProjectionInfo;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionManager.Callback;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import com.android.server.security.securityprofile.PolicyEngine.Action;
import com.android.server.security.securityprofile.PolicyEngine.State;
import com.huawei.android.app.HwActivityManager;
import huawei.android.security.ISecurityProfileService;
import huawei.android.security.securityprofile.ApkDigest;
import huawei.android.security.securityprofile.DigestMatcher;
import huawei.android.security.securityprofile.PolicyExtractor;
import huawei.android.security.securityprofile.PolicyExtractor.PolicyNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

public class SecurityProfileService implements IHwSecurityPlugin {
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            return new SecurityProfileService(context);
        }

        public String getPluginPermission() {
            return SecurityProfileService.MANAGE_SECURITYPROFILE;
        }
    };
    private static final String MANAGE_SECURITYPROFILE = "com.huawei.permission.MANAGE_SECURITYPROFILE";
    private static final int MAX_TASK = 10;
    private static final String TAG = "SecurityProfileService";
    private static final int TOAST_LONG_DELAY = 3500;
    private ActivityManager activityManager;
    private long blackToastTime = 0;
    private final Context mContext;
    BroadcastReceiver mDefaultUserBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                Slog.i(SecurityProfileService.TAG, "receive Intent.ACTION_BOOT_COMPLETED");
                if (SecurityProfileService.this.mPolicyEngine.isNewVersionFirstBoot()) {
                    new Thread(new VerifyInstalledPackagesRunnable(context, SecurityProfileUtils.getInstalledPackages(context))).start();
                } else {
                    Slog.i(SecurityProfileService.TAG, "not new version first boot,do not verify all installed package");
                }
            }
        }
    };
    private final Handler mHandler = new Handler();
    MediaProjectionStopper mMediaProjectionStopper = null;
    BroadcastReceiver mMultiUserBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String packageName = uri.getSchemeSpecificPart();
                    SecurityProfileService.this.mPolicyEngine.updatePackageInformation(packageName);
                    if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                        InstallerDataBase.getInstance().setInstallerPackageName(context, packageName);
                    }
                }
            }
        }
    };
    private PolicyEngine mPolicyEngine = null;
    private ScreenshotProtector screenshotProtector = new ScreenshotProtector();

    private class MediaProjectionStopper {
        public MediaProjectionInfo mActiveProjection = null;
        private MediaProjectionManager mProjectionService = null;
        public String mProtectedPackage = null;

        private class MediaProjectionCallback extends Callback {
            private MediaProjectionCallback() {
            }

            /* synthetic */ MediaProjectionCallback(MediaProjectionStopper x0, AnonymousClass1 x1) {
                this();
            }

            public void onStart(MediaProjectionInfo info) {
                synchronized (MediaProjectionStopper.this) {
                    MediaProjectionStopper.this.mActiveProjection = info;
                }
            }

            public void onStop(MediaProjectionInfo info) {
                synchronized (MediaProjectionStopper.this) {
                    MediaProjectionStopper.this.mActiveProjection = null;
                }
            }
        }

        public MediaProjectionStopper() {
            installCallback();
        }

        private void installCallback() {
            long token = Binder.clearCallingIdentity();
            try {
                if (Stub.asInterface(ServiceManager.getService("media_projection")) != null) {
                    this.mProjectionService = (MediaProjectionManager) SecurityProfileService.this.mContext.getSystemService("media_projection");
                    if (this.mProjectionService != null) {
                        this.mProjectionService.addCallback(new MediaProjectionCallback(this, null), null);
                        synchronized (this) {
                            this.mActiveProjection = this.mProjectionService.getActiveProjectionInfo();
                        }
                    }
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        }

        private MediaProjectionInfo getActiveProjectionInfo() {
            MediaProjectionInfo mediaProjectionInfo;
            synchronized (this) {
                mediaProjectionInfo = this.mActiveProjection;
            }
            return mediaProjectionInfo;
        }

        public void stopMediaProjection() {
            try {
                MediaProjectionInfo mpi = getActiveProjectionInfo();
                if (mpi != null) {
                    String projectionPack = mpi.getPackageName();
                    if (projectionPack != null && !SecurityProfileService.this.isSystemApp(projectionPack)) {
                        SecurityProfileService.this.stopPackages(new String[]{projectionPack});
                        if (this.mProtectedPackage != null) {
                            ScreenshotProtectorCallback callback = SecurityProfileService.this.screenshotProtector.getRegister(this.mProtectedPackage);
                            if (callback != null) {
                                SecurityProfileService.this.screenshotProtector.notifyInfo(callback, projectionPack);
                            }
                            this.mProtectedPackage = null;
                        }
                        this.mProjectionService.stopActiveProjection();
                        Slog.d(SecurityProfileService.TAG, "MediaProjection is stopped by SecurityProfileService.");
                        this.mActiveProjection = null;
                        this.mProjectionService.stopActiveProjection();
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
                Slog.e(SecurityProfileService.TAG, "failed to stop MediaProjection.");
            }
        }
    }

    private class PackageStopper implements Runnable {
        private String[] packageList = null;

        public PackageStopper(String[] packages) {
            this.packageList = packages;
        }

        public void run() {
            if (!(this.packageList == null || SecurityProfileService.this.activityManager == null)) {
                for (String packageName : this.packageList) {
                    String str = SecurityProfileService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(" is stopped by SecurityProfileService.");
                    Slog.d(str, stringBuilder.toString());
                    SecurityProfileService.this.activityManager.forceStopPackageAsUser(packageName, -1);
                }
            }
        }
    }

    private static class ScreenshotProtector {
        private final Object mLock;
        private List<ScreenshotProtectorCallback> screenshotList;

        private ScreenshotProtector() {
            this.screenshotList = new ArrayList();
            this.mLock = new Object();
        }

        /* synthetic */ ScreenshotProtector(AnonymousClass1 x0) {
            this();
        }

        public ScreenshotProtectorCallback getRegister(String app) {
            synchronized (this.mLock) {
                for (ScreenshotProtectorCallback callback : this.screenshotList) {
                    if (callback.isProtectedApp(app)) {
                        return callback;
                    }
                }
                return null;
            }
        }

        public void notifyInfo(ScreenshotProtectorCallback callback, String projectionApp) {
            synchronized (this.mLock) {
                if (callback.isActive()) {
                    callback.notifyInfo(projectionApp);
                }
            }
        }

        public void register(ScreenshotProtectorCallback callback) {
            if (callback != null) {
                synchronized (this.mLock) {
                    callback.setActiveStatus(true);
                    this.screenshotList.add(callback);
                }
            }
        }

        public void unregister(ScreenshotProtectorCallback callback) {
            if (callback != null) {
                synchronized (this.mLock) {
                    callback.setActiveStatus(false);
                    this.screenshotList.remove(callback);
                }
            }
        }
    }

    private class SecurityProfileServiceImpl extends ISecurityProfileService.Stub {
        private SecurityProfileServiceImpl() {
        }

        /* synthetic */ SecurityProfileServiceImpl(SecurityProfileService x0, AnonymousClass1 x1) {
            this();
        }

        public void updateBlackApp(List packageName, int action) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            switch (action) {
                case 1:
                    SecurityProfileService.this.mPolicyEngine.updateBlackApp(packageName);
                    return;
                case 2:
                    SecurityProfileService.this.mPolicyEngine.addBlackApp(packageName);
                    return;
                case 3:
                    SecurityProfileService.this.mPolicyEngine.removeBlackApp(packageName);
                    return;
                default:
                    return;
            }
        }

        public boolean isBlackApp(String packageName) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            return SecurityProfileService.this.mPolicyEngine.isBlackApp(packageName);
        }

        public int addDomainPolicy(byte[] domainPolicy) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            JSONObject policy = PolicyVerifier.verifyAndDecodePolicy(domainPolicy);
            if (policy == null) {
                return 1;
            }
            SecurityProfileService.this.mPolicyEngine.addPolicy(policy);
            return 0;
        }

        public boolean requestAccess(String subject, String object, String subsystem, List<String> list, String operation, int timeout) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            return SecurityProfileService.this.mPolicyEngine.requestAccess(subject, object, subsystem, operation, timeout);
        }

        public boolean checkAccess(String subject, String object, String subsystem, List<String> list, String operation) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            return SecurityProfileService.this.mPolicyEngine.checkAccess(subject, object, subsystem, operation);
        }

        public List<String> getLabels(String packageName, ApkDigest digest) {
            return SecurityProfileService.this.mPolicyEngine.getLabels(packageName, digest);
        }

        public boolean isPackageSigned(String packageName) {
            return SecurityProfileService.this.mPolicyEngine.isPackageSigned(packageName);
        }

        public String getInstallerPackageName(String packageName) {
            return InstallerDataBase.getInstance().getInstaller(SecurityProfileService.this.mContext, packageName);
        }
    }

    private class VerifyInstalledPackagesRunnable implements Runnable {
        Context context;
        List<String> packageNameList;

        public VerifyInstalledPackagesRunnable(Context context, List<String> inPackageNameList) {
            this.packageNameList = inPackageNameList;
            this.context = context;
        }

        public void run() {
            if (this.packageNameList != null) {
                long beginTime = System.currentTimeMillis();
                String str = SecurityProfileService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[SEAPP_TimeUsage]VerifyInstalledPackages  thread begin:");
                stringBuilder.append(beginTime);
                Slog.d(str, stringBuilder.toString());
                for (String packageName : this.packageNameList) {
                    try {
                        SecurityProfileService.this.verifyPackage(packageName, SecurityProfileUtils.getInstalledApkPath(packageName, this.context));
                    } catch (Exception e) {
                        String str2 = SecurityProfileService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("VerifyInstalledPackagesRunnable Exception ");
                        stringBuilder2.append(e.getMessage());
                        Slog.e(str2, stringBuilder2.toString());
                    }
                }
                long endTime = System.currentTimeMillis();
                str = SecurityProfileService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[SEAPP_TimeUsage]VerifyInstalledPackages  thread end:");
                stringBuilder.append(endTime);
                stringBuilder.append(", Total usage:");
                stringBuilder.append(endTime - beginTime);
                stringBuilder.append("ms");
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    private final class LocalServiceImpl implements SecurityProfileInternal {
        private LocalServiceImpl() {
        }

        /* synthetic */ LocalServiceImpl(SecurityProfileService x0, AnonymousClass1 x1) {
            this();
        }

        public boolean shouldPreventInteraction(int type, String targetPackage, int callerUid, int callerPid, String callerPackage, int userId) {
            return SecurityProfileService.this.shouldPreventInteraction(type, targetPackage, callerUid, callerPid, callerPackage, userId);
        }

        public boolean shouldPreventMediaProjection(int uid) {
            return SecurityProfileService.this.shouldPreventMediaProjection(uid);
        }

        public void handleActivityResuming(String packageName) {
            if (packageName != null) {
                if (!SecurityProfileService.this.mPolicyEngine.requestAccessWithExtraLabel("", packageName, SecurityProfileService.this.getExtraObjectLabel(packageName), "MediaProjection", "Record", 0)) {
                    String str = SecurityProfileService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleActivityResuming packageName:");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" is Media protect app,need stop Media Projection");
                    Slog.e(str, stringBuilder.toString());
                    SecurityProfileService.this.mMediaProjectionStopper.stopMediaProjection();
                }
            }
        }

        public void registerScreenshotProtector(ScreenshotProtectorCallback callback) {
            SecurityProfileService.this.screenshotProtector.register(callback);
        }

        public void unregisterScreenshotProtector(ScreenshotProtectorCallback callback) {
            SecurityProfileService.this.screenshotProtector.unregister(callback);
        }

        public boolean verifyPackage(String packageName, String path) {
            long beginTime = System.currentTimeMillis();
            String str = SecurityProfileService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[SEAPP_TimeUsage]verifyPackage begin:");
            stringBuilder.append(beginTime);
            Slog.d(str, stringBuilder.toString());
            boolean result = SecurityProfileService.this.verifyPackage(packageName, path);
            long endTime = System.currentTimeMillis();
            str = SecurityProfileService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[SEAPP_TimeUsage]verifyPackage end:");
            stringBuilder.append(endTime);
            stringBuilder.append(", Total usage:");
            stringBuilder.append(endTime - beginTime);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
            return result;
        }
    }

    public SecurityProfileService(Context context) {
        this.mContext = context;
    }

    public SecurityProfileInternal getLocalServiceImpl() {
        return new LocalServiceImpl(this, null);
    }

    public void onStart() {
        long beginTime = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SEAPP_TimeUsage]SecurityProfileService onStart begin:");
        stringBuilder.append(beginTime);
        Slog.d(str, stringBuilder.toString());
        this.activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (this.mMediaProjectionStopper == null) {
            this.mMediaProjectionStopper = new MediaProjectionStopper();
        }
        this.mPolicyEngine = new PolicyEngine(this.mContext);
        this.mPolicyEngine.addAction("StopScreenRecording", new Action() {
            public void execute(int timeout) {
                SecurityProfileService.this.mMediaProjectionStopper.stopMediaProjection();
            }
        });
        this.mPolicyEngine.addState("NoScreenRecording", new State() {
            public boolean evaluate() {
                return SecurityProfileService.this.mMediaProjectionStopper.mActiveProjection == null;
            }
        });
        this.mPolicyEngine.start();
        LocalServices.addService(SecurityProfileInternal.class, getLocalServiceImpl());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mMultiUserBroadcastReceiver, UserHandle.ALL, filter, null, null);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mDefaultUserBroadcastReceiver, filter2);
        long endTime = System.currentTimeMillis();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[SEAPP_TimeUsage]SecurityProfileService onStart end:");
        stringBuilder2.append(endTime);
        stringBuilder2.append(", Total usage:");
        stringBuilder2.append(endTime - beginTime);
        stringBuilder2.append("ms");
        Slog.d(str2, stringBuilder2.toString());
    }

    public void onStop() {
        Slog.e(TAG, "SecurityProfileService stoped");
    }

    public IBinder asBinder() {
        return new SecurityProfileServiceImpl(this, null);
    }

    public boolean verifyPackage(String packageName, String apkPath) {
        String str;
        StringBuilder stringBuilder;
        try {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verifyPackage apkPath:");
            stringBuilder.append(apkPath);
            Slog.d(str, stringBuilder.toString());
            byte[] jws = PolicyExtractor.getPolicy(apkPath);
            ApkDigest apkDigest = PolicyExtractor.getDigest(packageName, jws);
            JSONObject policy = PolicyVerifier.verifyAndDecodePolicy(jws);
            if (policy == null) {
                Slog.e(TAG, "Policy verification failed");
                return false;
            } else if (DigestMatcher.packageMatchesDigest(apkPath, apkDigest)) {
                this.mPolicyEngine.setPackageSigned(packageName, true);
                this.mPolicyEngine.addPolicy(policy);
                return true;
            } else {
                Slog.e(TAG, "Package digest did not match policy digest");
                return false;
            }
        } catch (PolicyNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not found Exception ");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return true;
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verifyPackage IOException ");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verifyPackage Exception ");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private String getExtraObjectLabel(String packageName) {
        if (this.screenshotProtector.getRegister(packageName) != null) {
            return "Red";
        }
        return null;
    }

    public boolean shouldPreventMediaProjection(int uid) {
        if (uid < 10000) {
            return false;
        }
        String[] projectionPackages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (projectionPackages == null || projectionPackages.length == 0) {
            return false;
        }
        String pack;
        Set<String> projectionSet = new HashSet();
        for (String pack2 : projectionPackages) {
            projectionSet.add(pack2);
        }
        if (this.activityManager == null) {
            return false;
        }
        for (RunningTaskInfo taskInfo : this.activityManager.getRunningTasks(10)) {
            pack2 = taskInfo.topActivity == null ? null : taskInfo.topActivity.getPackageName();
            if (pack2 != null) {
                if (!projectionSet.contains(pack2)) {
                    if (HwActivityManager.isTaskVisible(taskInfo.id)) {
                        if (!this.mPolicyEngine.requestAccessWithExtraLabel((String) projectionSet.toArray()[0], pack2, getExtraObjectLabel(pack2), "MediaProjection", "Record", 0)) {
                            ScreenshotProtectorCallback callback = this.screenshotProtector.getRegister(pack2);
                            if (callback != null) {
                                stopPackages(projectionPackages);
                                this.screenshotProtector.notifyInfo(callback, projectionPackages[0]);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean shouldPreventInteraction(int type, String targetPackage, int callerUid, int callerPid, String callerPackage, int userId) {
        String str = targetPackage;
        long t = System.nanoTime();
        if (str == null) {
            return false;
        }
        String extraObjectLabel = null;
        if (type == 0) {
            extraObjectLabel = getExtraObjectLabel(str);
            if (extraObjectLabel != null) {
                this.mMediaProjectionStopper.mProtectedPackage = str;
            }
        }
        if (this.mPolicyEngine.requestAccessWithExtraLabel(callerPackage, str, extraObjectLabel, "Intent", "Send", 0)) {
            extraObjectLabel = callerPackage;
            return false;
        }
        if (type != 0) {
            extraObjectLabel = callerPackage;
        } else if (isLauncherApp(callerPackage)) {
            long curTime = System.currentTimeMillis();
            if (curTime < this.blackToastTime || curTime - this.blackToastTime > 3500) {
                this.blackToastTime = curTime;
                String text = this.mContext.getResources().getString(33686192);
                if (text != null) {
                    showToast(text);
                }
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(" is not allowed to start.");
        Slog.d(str2, stringBuilder.toString());
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("shouldPreventInteraction ");
        stringBuilder.append(String.valueOf(System.nanoTime() - t));
        Slog.d(str2, stringBuilder.toString());
        return true;
    }

    private boolean isLauncherApp(String target) {
        PackageManager manager = this.mContext.getPackageManager();
        if (manager == null) {
            return false;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfo = manager.resolveActivity(intent, 65536);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        String launcher = resolveInfo.activityInfo.packageName;
        if (launcher == null || !launcher.equals(target)) {
            return false;
        }
        return true;
    }

    private void showToast(final String text) {
        UiThread.getHandler().post(new Runnable() {
            public void run() {
                Toast toast = Toast.makeText(SecurityProfileService.this.mContext, text, 1);
                LayoutParams windowParams = toast.getWindowParams();
                windowParams.privateFlags |= 16;
                toast.show();
            }
        });
    }

    private void stopPackages(String[] packageNames) {
        this.mHandler.post(new PackageStopper(packageNames));
    }

    private boolean isSystemApp(String pack) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(pack, 0);
            if (info == null) {
                return false;
            }
            if (info.uid < 10000 || (info.flags & 1) != 0) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("can not check if ");
            stringBuilder.append(pack);
            stringBuilder.append(" is a system app");
            Slog.e(str, stringBuilder.toString());
        }
    }
}

package com.android.server.security.securityprofile;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionInfo;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.pm.auth.HwCertXmlHandler;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.securityprofile.PolicyEngine;
import com.android.server.security.securityprofile.PolicyVerifier;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.HwActivityTaskManager;
import com.huawei.android.app.IHwActivityNotifierEx;
import huawei.android.security.ISecurityProfileService;
import huawei.android.security.securityprofile.ApkDigest;
import huawei.android.security.securityprofile.ApkSigningBlockUtils;
import huawei.android.security.securityprofile.HwSignedInfo;
import huawei.android.security.securityprofile.PolicyExtractor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

public class SecurityProfileService implements IHwSecurityPlugin {
    private static final String ACTION_STOP_SCREEN_RECORDING = "StopScreenRecording";
    private static final int ACTION_UPDATE_ALL = 1;
    private static final boolean CALCULATE_APKDIGEST = "true".equalsIgnoreCase(SystemProperties.get("ro.config.iseapp_calculate_apkdigest", "true"));
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            return new SecurityProfileService(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return SecurityProfileService.MANAGE_SECURITYPROFILE;
        }
    };
    /* access modifiers changed from: private */
    public static final boolean DEBUG = SecurityProfileUtils.DEBUG;
    private static final List<String> HUAWEI_INSTALLERS = Arrays.asList("com.huawei.appmarket", "com.huawei.gamebox");
    private static final String HW_SIGNATURE_OR_SYSTEM = "com.huawei.permission.HW_SIGNATURE_OR_SYSTEM";
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final String MANAGE_SECURITYPROFILE = "com.huawei.permission.MANAGE_SECURITYPROFILE";
    private static final int MAX_TASK = 10;
    private static final String REASON = "activityLifeState";
    private static final String STATE_NO_SCREEN_RECORDING = "NoScreenRecording";
    private static final boolean SUPPORT_HW_SEAPP = "true".equalsIgnoreCase(SystemProperties.get("ro.config.support_iseapp", "false"));
    private static final String TAG = "SecurityProfileService";
    private static final int TOAST_LONG_DELAY = 3500;
    private ListenResumeCustomActivityNotifier listenResumeCustomActivityNotifier = null;
    private PolicyEngine.Action mActionStopScreenRecording = new PolicyEngine.Action(ACTION_STOP_SCREEN_RECORDING) {
        /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass2 */

        /* access modifiers changed from: package-private */
        @Override // com.android.server.security.securityprofile.PolicyEngine.Action
        public void execute(int timeout) {
            SecurityProfileService.this.mMediaProjectionStopper.stopMediaProjection();
        }

        @Override // com.android.server.security.securityprofile.PolicyEngine.Action
        public boolean equals(Object action) {
            return (action instanceof PolicyEngine.Action) && this.mName.equals(((PolicyEngine.Action) action).mName);
        }

        @Override // com.android.server.security.securityprofile.PolicyEngine.Action
        public int hashCode() {
            return this.mName.hashCode();
        }
    };
    /* access modifiers changed from: private */
    public ActivityManager mActivityManager;
    private long mBlackToastTime = 0;
    /* access modifiers changed from: private */
    public final Context mContext;
    private BroadcastReceiver mDefaultUserBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass5 */

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                if (SecurityProfileService.DEBUG) {
                    Log.i(SecurityProfileService.TAG, "receive Intent.ACTION_BOOT_COMPLETED");
                }
                if (SecurityProfileService.this.mPolicyEngine.isNeedPolicyRecover()) {
                    new Thread(new VerifyInstalledPackagesRunnable(SecurityProfileUtils.getInstalledPackages(context)), "PolicyRecoverWorkerThread").start();
                } else if (SecurityProfileService.DEBUG) {
                    Log.i(SecurityProfileService.TAG, "not need to recover policy, do not verify all installed package");
                }
            }
        }
    };
    private final Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public final HwCertXmlHandler mHwCertXmlHandler;
    /* access modifiers changed from: private */
    public MediaProjectionStopper mMediaProjectionStopper = null;
    private BroadcastReceiver mMultiUserBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass4 */

        public void onReceive(final Context context, Intent intent) {
            Uri uri;
            final String action = intent.getAction();
            if (("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) && (uri = intent.getData()) != null) {
                String outerPackageName = uri.getSchemeSpecificPart();
                if (outerPackageName == null) {
                    Log.w(SecurityProfileService.TAG, "onReceive outerPackageName null");
                    return;
                }
                final String packageName = SecurityProfileUtils.replaceLineSeparator(outerPackageName);
                SecurityProfileUtils.getWorkerThreadPool().execute(new Runnable() {
                    /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass4.AnonymousClass1 */

                    public void run() {
                        try {
                            if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                                boolean unused = SecurityProfileService.this.verifyPackage(packageName, SecurityProfileUtils.getInstalledApkPath(packageName, context));
                            }
                            SecurityProfileService.this.mPolicyEngine.updatePackageInformation(packageName);
                        } catch (Exception e) {
                            Log.e(SecurityProfileService.TAG, "Failed to update policy into database!");
                        }
                        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                            InstallerDataBase.getInstance().setInstallerPackageName(context, packageName);
                        }
                    }
                });
            }
        }
    };
    /* access modifiers changed from: private */
    public PolicyEngine mPolicyEngine = null;
    /* access modifiers changed from: private */
    public ScreenshotProtector mScreenshotProtector = new ScreenshotProtector();
    private PolicyEngine.State mStateNoScreenRecording = new PolicyEngine.State(STATE_NO_SCREEN_RECORDING) {
        /* class com.android.server.security.securityprofile.SecurityProfileService.AnonymousClass3 */

        /* access modifiers changed from: package-private */
        @Override // com.android.server.security.securityprofile.PolicyEngine.State
        public boolean evaluate() {
            return SecurityProfileService.this.isNoScreenRecording();
        }

        @Override // com.android.server.security.securityprofile.PolicyEngine.State
        public boolean equals(Object state) {
            return (state instanceof PolicyEngine.State) && this.mName.equals(((PolicyEngine.State) state).mName);
        }

        @Override // com.android.server.security.securityprofile.PolicyEngine.State
        public int hashCode() {
            return this.mName.hashCode();
        }
    };

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public SecurityProfileService(Context context) {
        this.mContext = context;
        this.mHwCertXmlHandler = new HwCertXmlHandler();
    }

    private SecurityProfileInternal getLocalServiceImpl() {
        return new LocalServiceImpl();
    }

    /* access modifiers changed from: private */
    public boolean isNoScreenRecording() {
        if (this.mMediaProjectionStopper.mActiveProjection == null) {
            return true;
        }
        String projectionPackageName = this.mMediaProjectionStopper.mActiveProjection.getPackageName();
        if (projectionPackageName == null) {
            Log.w(TAG, "mActiveProjection is not null, but we can not get projectionPackageName from it!");
            return true;
        } else if (!SecurityProfileUtils.isSystemApp(this.mContext, projectionPackageName)) {
            return false;
        } else {
            if (SecurityProfileUtils.isAccessibilitySelectToSpeakActive(this.mContext)) {
                return true;
            }
            Log.w(TAG, projectionPackageName + " is systemApp and it is recording");
            return false;
        }
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        long beginTime = 0;
        if (DEBUG) {
            beginTime = System.currentTimeMillis();
            Log.d(TAG, "[SEAPP_TimeUsage]SecurityProfileService onStart begin: " + beginTime);
        }
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (this.mMediaProjectionStopper == null) {
            this.mMediaProjectionStopper = new MediaProjectionStopper();
        }
        this.mPolicyEngine = new PolicyEngine(this.mContext);
        this.mPolicyEngine.addAction(ACTION_STOP_SCREEN_RECORDING, this.mActionStopScreenRecording);
        this.mPolicyEngine.addState(STATE_NO_SCREEN_RECORDING, this.mStateNoScreenRecording);
        this.mPolicyEngine.start();
        LocalServices.addService(SecurityProfileInternal.class, getLocalServiceImpl());
        this.listenResumeCustomActivityNotifier = new ListenResumeCustomActivityNotifier();
        ActivityManagerEx.registerHwActivityNotifier(this.listenResumeCustomActivityNotifier, "activityLifeState");
        if (SUPPORT_HW_SEAPP && IS_CHINA_AREA) {
            IntentFilter packageChangedFilter = new IntentFilter();
            packageChangedFilter.addAction("android.intent.action.PACKAGE_ADDED");
            packageChangedFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            packageChangedFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            packageChangedFilter.addDataScheme("package");
            this.mContext.registerReceiverAsUser(this.mMultiUserBroadcastReceiver, UserHandle.ALL, packageChangedFilter, null, null);
            IntentFilter bootCompletedFilter = new IntentFilter();
            bootCompletedFilter.addAction("android.intent.action.BOOT_COMPLETED");
            this.mContext.registerReceiver(this.mDefaultUserBroadcastReceiver, bootCompletedFilter);
        }
        if (DEBUG) {
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "[SEAPP_TimeUsage]SecurityProfileService onStart end: " + endTime + " Total usage: " + (endTime - beginTime) + "ms");
        }
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
        ActivityManagerEx.unregisterHwActivityNotifier(this.listenResumeCustomActivityNotifier);
        Log.e(TAG, "SecurityProfileService stopped");
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.security.securityprofile.SecurityProfileService$SecurityProfileServiceImpl, android.os.IBinder] */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return new SecurityProfileServiceImpl();
    }

    private void handleActivityResuming(String packageName) {
        if (packageName != null && !this.mPolicyEngine.requestAccessWithExtraLabel("", new PolicyEngine.PolicyObject(packageName, getExtraObjectLabel(packageName)), new PolicyEngine.PolicyAdverbial("MediaProjection", "Record", 0))) {
            Log.w(TAG, "handleActivityResuming packageName: " + packageName + " is Media protect app, need stop Media Projection");
            this.mMediaProjectionStopper.stopMediaProjection();
        }
    }

    /* access modifiers changed from: private */
    public boolean verifyPackage(String packageName, String apkPath) {
        try {
            if (DEBUG) {
                Log.d(TAG, "verifyPackage apkPath: " + apkPath);
            }
            JSONObject policy = PolicyVerifier.getValidPolicyFromApkPath(packageName, apkPath);
            this.mPolicyEngine.setPackageSigned(packageName, true);
            this.mPolicyEngine.addPolicy(policy);
            return true;
        } catch (PolicyExtractor.PolicyNotFoundException e) {
            Log.w(TAG, "verifyPackage must return for huawei policy not found: " + e.getMessage() + ", apkPath: " + apkPath);
            return true;
        } catch (PolicyVerifier.PolicyVerifyFailedException e2) {
            Log.w(TAG, "verifyPackage must return for policy verify failed: " + e2.getMessage() + ", apkPath: " + apkPath);
            return false;
        } catch (Exception e3) {
            Log.e(TAG, "Failed to verify policy!");
            return false;
        }
    }

    private boolean isInstalledByHuaweiAppMarket(String packageName) {
        if (HUAWEI_INSTALLERS.contains(InstallerDataBase.getInstance().getInstallerPackageName(this.mContext, packageName))) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isInstalledAppCanGetHwSignedInfo(String packageName) {
        if (this.mPolicyEngine.isPackageSigned(packageName)) {
            return true;
        }
        if (!CALCULATE_APKDIGEST || !isInstalledByHuaweiAppMarket(packageName)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public HwSignedInfo getActiveHwSignedInfo(String packageName, int flags) {
        HwSignedInfo hwSignedInfo = new HwSignedInfo(packageName);
        if ((flags & 2) != 0) {
            try {
                hwSignedInfo.labelsList = this.mPolicyEngine.getLabels(packageName, null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get signed info for " + packageName + ", flags: " + flags + ", error!");
            }
        }
        if ((flags & 1) != 0) {
            hwSignedInfo.apkDigest = getActiveHwSignedDigest(packageName);
        }
        return hwSignedInfo;
    }

    private class ListenResumeCustomActivityNotifier extends IHwActivityNotifierEx {
        private ListenResumeCustomActivityNotifier() {
        }

        public void call(Bundle extras) {
            if (extras == null || extras.isEmpty()) {
                Log.w(SecurityProfileService.TAG, "Activity life state callback extras get null!");
                return;
            }
            String packageName = extras.getString("package");
            if (TextUtils.isEmpty(packageName)) {
                Log.w(SecurityProfileService.TAG, "Activity life state callback extras get package null!");
                return;
            }
            ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
            if (spc != null) {
                spc.handleActivityResuming(packageName);
            }
        }
    }

    private ApkDigest getActiveHwSignedDigest(String packageName) {
        String apkPath = SecurityProfileUtils.getInstalledApkPath(packageName, this.mContext);
        try {
            return PolicyExtractor.getApkDigestFromPolicyBlock(packageName, PolicyExtractor.getPolicyBlock(apkPath));
        } catch (PolicyExtractor.PolicyNotFoundException e) {
            return ApkSigningBlockUtils.calculateApkDigest(apkPath);
        }
    }

    private String getExtraObjectLabel(String packageName) {
        if (this.mScreenshotProtector.getRegister(packageName) == null) {
            return null;
        }
        if (!DEBUG) {
            return "Red";
        }
        Log.d(TAG, "getExtraObjectLabel: " + packageName + ", label: Red");
        return "Red";
    }

    private boolean shouldPreventMediaProjection(int uid) {
        ScreenshotProtectorCallback callback;
        if (uid < 10000) {
            if (DEBUG) {
                Log.d(TAG, "not PreventMediaProjection: system Uid " + uid);
            }
            return false;
        }
        String[] projectionPackages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (projectionPackages == null || projectionPackages.length == 0) {
            Log.w(TAG, "not PreventMediaProjection: get no packages for Uid" + uid);
            return false;
        }
        Set<String> projectionSet = new HashSet<>(Arrays.asList(projectionPackages));
        ActivityManager activityManager = this.mActivityManager;
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(10);
        if (runningTasks == null) {
            Log.w(TAG, "getRunningTasks null");
            return false;
        }
        for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
            String foreground = taskInfo.topActivity == null ? null : taskInfo.topActivity.getPackageName();
            if (foreground != null && !projectionSet.contains(foreground) && HwActivityTaskManager.isTaskVisible(taskInfo.id) && !this.mPolicyEngine.requestAccessWithExtraLabel((String) projectionSet.toArray()[0], new PolicyEngine.PolicyObject(foreground, getExtraObjectLabel(foreground)), new PolicyEngine.PolicyAdverbial("MediaProjection", "Record", 0)) && (callback = this.mScreenshotProtector.getRegister(foreground)) != null) {
                if (DEBUG) {
                    Log.d(TAG, "package: " + foreground + "is protected, try to stop projection");
                }
                stopPackages(projectionPackages);
                this.mScreenshotProtector.notifyInfo(callback, projectionPackages[0]);
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean shouldPreventInteraction(int type, String targetPackage, IntentCaller caller, int userId) {
        if (targetPackage == null) {
            return false;
        }
        String extraObjectLabel = null;
        if (type == 0 && (extraObjectLabel = getExtraObjectLabel(targetPackage)) != null) {
            this.mMediaProjectionStopper.mProtectedPackage = targetPackage;
        }
        if (this.mPolicyEngine.requestAccessWithExtraLabel(caller.packageName, new PolicyEngine.PolicyObject(targetPackage, extraObjectLabel), new PolicyEngine.PolicyAdverbial("Intent", "Send", 0))) {
            return false;
        }
        if (type == 0 && SecurityProfileUtils.isLauncherApp(this.mContext, caller.packageName)) {
            long curTime = System.currentTimeMillis();
            if (isBlackToastNotExist(curTime)) {
                this.mBlackToastTime = curTime;
                SecurityProfileUtils.showToast(this.mContext, this.mContext.getResources().getString(33686234));
            }
        }
        Log.w(TAG, targetPackage + " is not allowed to start.");
        return true;
    }

    private boolean isBlackToastNotExist(long curTime) {
        long j = this.mBlackToastTime;
        return curTime < j || curTime - j > 3500;
    }

    /* access modifiers changed from: private */
    public void stopPackages(String[] packageNames) {
        this.mHandler.post(new PackageStopper(packageNames));
    }

    /* access modifiers changed from: private */
    public static class ScreenshotProtector {
        private static final int DEFAULT_NUM_OF_CALLBACKS = 16;
        private final Object mLock;
        private List<ScreenshotProtectorCallback> mScreenshotCallbacks;

        private ScreenshotProtector() {
            this.mLock = new Object();
            this.mScreenshotCallbacks = new ArrayList(16);
        }

        public ScreenshotProtectorCallback getRegister(String app) {
            synchronized (this.mLock) {
                for (ScreenshotProtectorCallback callback : this.mScreenshotCallbacks) {
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
                    this.mScreenshotCallbacks.add(callback);
                }
            }
        }

        public void unregister(ScreenshotProtectorCallback callback) {
            if (callback != null) {
                synchronized (this.mLock) {
                    callback.setActiveStatus(false);
                    this.mScreenshotCallbacks.remove(callback);
                }
            }
        }
    }

    private class VerifyInstalledPackagesRunnable implements Runnable {
        List<String> mPackageNameList;

        VerifyInstalledPackagesRunnable(List<String> inPackageNameList) {
            this.mPackageNameList = inPackageNameList;
        }

        public void run() {
            if (this.mPackageNameList != null) {
                long beginTime = 0;
                if (SecurityProfileService.DEBUG) {
                    beginTime = System.currentTimeMillis();
                    Log.d(SecurityProfileService.TAG, "[SEAPP_TimeUsage]VerifyInstalledPackages thread begin: " + beginTime);
                }
                for (String packageName : this.mPackageNameList) {
                    SecurityProfileService securityProfileService = SecurityProfileService.this;
                    boolean unused = securityProfileService.verifyPackage(packageName, SecurityProfileUtils.getInstalledApkPath(packageName, securityProfileService.mContext));
                }
                SecurityProfileService.this.mPolicyEngine.setPolicyRecoverFlag(false);
                if (SecurityProfileService.DEBUG) {
                    long endTime = System.currentTimeMillis();
                    Log.d(SecurityProfileService.TAG, "[SEAPP_TimeUsage]VerifyInstalledPackages thread end: " + endTime + " Total usage: " + (endTime - beginTime) + "ms");
                }
            }
        }
    }

    private class SecurityProfileServiceImpl extends ISecurityProfileService.Stub {
        private static final int RESULT_CODE_ADD_POLICY_FAIL = 1;
        private static final int RESULT_CODE_ADD_POLICY_SUCC = 0;

        private SecurityProfileServiceImpl() {
        }

        public void updateBlackApp(List<String> packages, int action) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            long token = Binder.clearCallingIdentity();
            if (action == 1) {
                SecurityProfileService.this.mPolicyEngine.updateBlackApp(packages);
                SecurityProfileService.this.killBlackApps(packages);
            } else if (action == 2) {
                SecurityProfileService.this.mPolicyEngine.addBlackApp(packages);
                SecurityProfileService.this.killBlackApps(packages);
            } else if (action == 3) {
                try {
                    SecurityProfileService.this.mPolicyEngine.removeBlackApp(packages);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(token);
        }

        public boolean updateMdmCertBlacklist(List<String> blacklist, int action) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            if (action == 1) {
                return SecurityProfileService.this.mHwCertXmlHandler.updateMdmCertBlacklist(blacklist);
            }
            return false;
        }

        public boolean isBlackApp(String packageName) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            return SecurityProfileService.this.mPolicyEngine.isBlackApp(packageName);
        }

        public int addDomainPolicy(byte[] domainPolicy) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            try {
                SecurityProfileService.this.mPolicyEngine.addPolicy(PolicyVerifier.verifyAndDecodePolicy(domainPolicy));
                return 0;
            } catch (PolicyVerifier.PolicyVerifyFailedException e) {
                Log.w(SecurityProfileService.TAG, "Failed to verify when add policy: " + e.getMessage());
                return 1;
            } catch (Exception e2) {
                Log.e(SecurityProfileService.TAG, "Failed to add policy!");
                return 1;
            }
        }

        public List<String> getLabels(String packageName, ApkDigest digest) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            if (packageName == null) {
                return Collections.emptyList();
            }
            return SecurityProfileService.this.mPolicyEngine.getLabels(packageName, digest);
        }

        public HwSignedInfo getActiveHwSignedInfo(String packageName, int flags) {
            SecurityProfileService.this.mContext.enforceCallingOrSelfPermission(SecurityProfileService.MANAGE_SECURITYPROFILE, "Must have com.huawei.permission.MANAGE_SECURITYPROFILE permission.");
            if (packageName == null) {
                return null;
            }
            if (!SecurityProfileService.this.isInstalledAppCanGetHwSignedInfo(packageName)) {
                return new HwSignedInfo(packageName);
            }
            return SecurityProfileService.this.getActiveHwSignedInfo(packageName, flags);
        }
    }

    /* access modifiers changed from: private */
    public void killBlackApps(List<String> packages) {
        if (packages != null && packages.size() != 0) {
            for (String packageName : packages) {
                this.mActivityManager.forceStopPackageAsUser(packageName, -1);
                Log.w(TAG, packageName + " is stopped by SecurityProfileService, for black apps.");
            }
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public final class LocalServiceImpl implements SecurityProfileInternal {
        public LocalServiceImpl() {
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public boolean shouldPreventInteraction(int type, String targetPackage, IntentCaller caller, int userId) {
            return SecurityProfileService.this.shouldPreventInteraction(type, targetPackage, caller, userId);
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public boolean shouldPreventMediaProjection(int uid) {
            return false;
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public void handleActivityResuming(String packageName) {
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public void registerScreenshotProtector(ScreenshotProtectorCallback callback) {
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public void unregisterScreenshotProtector(ScreenshotProtectorCallback callback) {
        }

        @Override // com.android.server.security.securityprofile.SecurityProfileInternal
        public boolean verifyPackage(String packageName, File path) {
            long beginTime = 0;
            if (SecurityProfileService.DEBUG) {
                beginTime = System.currentTimeMillis();
                Log.d(SecurityProfileService.TAG, "[SEAPP_TimeUsage]verifyPackage begin: " + beginTime);
            }
            if (packageName == null || path == null) {
                Log.e(SecurityProfileService.TAG, "verifyPackageSecurityPolicy illegal params");
                return false;
            }
            try {
                boolean result = SecurityProfileService.this.verifyPackage(packageName, path.getCanonicalPath() + "/base.apk");
                if (SecurityProfileService.DEBUG) {
                    long endTime = System.currentTimeMillis();
                    Log.d(SecurityProfileService.TAG, "[SEAPP_TimeUsage]verifyPackage end: " + endTime + ", Total usage: " + (endTime - beginTime) + "ms");
                }
                return result;
            } catch (IOException e) {
                Log.e(SecurityProfileService.TAG, "Failed to get path from apk file: " + e.getMessage());
                return false;
            }
        }
    }

    private class PackageStopper implements Runnable {
        private String[] mPackageList = null;

        PackageStopper(String[] packages) {
            this.mPackageList = packages;
        }

        public void run() {
            if (this.mPackageList != null && SecurityProfileService.this.mActivityManager != null) {
                String[] strArr = this.mPackageList;
                for (String packageName : strArr) {
                    SecurityProfileService.this.mActivityManager.forceStopPackageAsUser(packageName, -1);
                    Log.w(SecurityProfileService.TAG, packageName + " is stopped by SecurityProfileService.");
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public class MediaProjectionStopper {
        MediaProjectionInfo mActiveProjection = null;
        private MediaProjectionManager mProjectionService = null;
        String mProtectedPackage = null;

        MediaProjectionStopper() {
            installCallback();
        }

        private void installCallback() {
            long token = Binder.clearCallingIdentity();
            try {
                this.mProjectionService = (MediaProjectionManager) SecurityProfileService.this.mContext.getSystemService("media_projection");
                if (this.mProjectionService == null) {
                    Log.w(SecurityProfileService.TAG, "installCallback getSystemService null");
                    return;
                }
                this.mProjectionService.addCallback(new MediaProjectionCallback(), null);
                synchronized (this) {
                    this.mActiveProjection = this.mProjectionService.getActiveProjectionInfo();
                }
                Binder.restoreCallingIdentity(token);
            } finally {
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
                    if (projectionPack == null) {
                        Log.w(SecurityProfileService.TAG, "stopMediaProjection, but get MediaProjectionInfo without packageName");
                    } else if (!SecurityProfileUtils.isSystemApp(SecurityProfileService.this.mContext, projectionPack)) {
                        SecurityProfileService.this.stopPackages(new String[]{projectionPack});
                        if (this.mProtectedPackage != null) {
                            ScreenshotProtectorCallback callback = SecurityProfileService.this.mScreenshotProtector.getRegister(this.mProtectedPackage);
                            if (callback != null) {
                                SecurityProfileService.this.mScreenshotProtector.notifyInfo(callback, projectionPack);
                            }
                            this.mProtectedPackage = null;
                        }
                        this.mProjectionService.stopActiveProjection();
                        this.mActiveProjection = null;
                        Log.w(SecurityProfileService.TAG, "MediaProjection is stopped by SecurityProfileService.");
                    } else if (SecurityProfileService.DEBUG) {
                        Log.d(SecurityProfileService.TAG, "not stopMediaProjection system App: " + projectionPack);
                    }
                }
            } catch (Exception e) {
                Log.e(SecurityProfileService.TAG, "Failed to stop media projection!");
            }
        }

        private class MediaProjectionCallback extends MediaProjectionManager.Callback {
            private MediaProjectionCallback() {
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
    }
}

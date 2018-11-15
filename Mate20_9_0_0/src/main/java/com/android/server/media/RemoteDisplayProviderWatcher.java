package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public final class RemoteDisplayProviderWatcher {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "RemoteDisplayProvider";
    private final Callback mCallback;
    private final Context mContext;
    private final Handler mHandler;
    private final PackageManager mPackageManager;
    private final ArrayList<RemoteDisplayProviderProxy> mProviders = new ArrayList();
    private boolean mRunning;
    private final BroadcastReceiver mScanPackagesReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (RemoteDisplayProviderWatcher.DEBUG) {
                String str = RemoteDisplayProviderWatcher.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received package manager broadcast: ");
                stringBuilder.append(intent);
                Slog.d(str, stringBuilder.toString());
            }
            RemoteDisplayProviderWatcher.this.scanPackages();
        }
    };
    private final Runnable mScanPackagesRunnable = new Runnable() {
        public void run() {
            RemoteDisplayProviderWatcher.this.scanPackages();
        }
    };
    private final int mUserId;

    public interface Callback {
        void addProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy);

        void removeProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy);
    }

    public RemoteDisplayProviderWatcher(Context context, Callback callback, Handler handler, int userId) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = handler;
        this.mUserId = userId;
        this.mPackageManager = context.getPackageManager();
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Watcher");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mUserId=");
        stringBuilder.append(this.mUserId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mRunning=");
        stringBuilder.append(this.mRunning);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("  mProviders.size()=");
        stringBuilder.append(this.mProviders.size());
        pw.println(stringBuilder.toString());
    }

    public void start() {
        if (!this.mRunning) {
            this.mRunning = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REPLACED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addDataScheme("package");
            this.mContext.registerReceiverAsUser(this.mScanPackagesReceiver, new UserHandle(this.mUserId), filter, null, this.mHandler);
            this.mHandler.post(this.mScanPackagesRunnable);
        }
    }

    public void stop() {
        if (this.mRunning) {
            this.mRunning = false;
            this.mContext.unregisterReceiver(this.mScanPackagesReceiver);
            this.mHandler.removeCallbacks(this.mScanPackagesRunnable);
            for (int i = this.mProviders.size() - 1; i >= 0; i--) {
                ((RemoteDisplayProviderProxy) this.mProviders.get(i)).stop();
            }
        }
    }

    private void scanPackages() {
        if (this.mRunning) {
            int targetIndex = 0;
            for (ResolveInfo resolveInfo : this.mPackageManager.queryIntentServicesAsUser(new Intent("com.android.media.remotedisplay.RemoteDisplayProvider"), 0, this.mUserId)) {
                ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                if (serviceInfo != null && verifyServiceTrusted(serviceInfo)) {
                    int targetIndex2;
                    int sourceIndex = findProvider(serviceInfo.packageName, serviceInfo.name);
                    RemoteDisplayProviderProxy provider;
                    if (sourceIndex < 0) {
                        provider = new RemoteDisplayProviderProxy(this.mContext, new ComponentName(serviceInfo.packageName, serviceInfo.name), this.mUserId);
                        provider.start();
                        targetIndex2 = targetIndex + 1;
                        this.mProviders.add(targetIndex, provider);
                        this.mCallback.addProvider(provider);
                    } else if (sourceIndex >= targetIndex) {
                        provider = (RemoteDisplayProviderProxy) this.mProviders.get(sourceIndex);
                        provider.start();
                        provider.rebindIfDisconnected();
                        targetIndex2 = targetIndex + 1;
                        Collections.swap(this.mProviders, sourceIndex, targetIndex);
                    }
                    targetIndex = targetIndex2;
                }
            }
            if (targetIndex < this.mProviders.size()) {
                for (int i = this.mProviders.size() - 1; i >= targetIndex; i--) {
                    RemoteDisplayProviderProxy provider2 = (RemoteDisplayProviderProxy) this.mProviders.get(i);
                    this.mCallback.removeProvider(provider2);
                    this.mProviders.remove(provider2);
                    provider2.stop();
                }
            }
        }
    }

    private boolean verifyServiceTrusted(ServiceInfo serviceInfo) {
        String str;
        StringBuilder stringBuilder;
        if (serviceInfo.permission == null || !serviceInfo.permission.equals("android.permission.BIND_REMOTE_DISPLAY")) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring remote display provider service because it did not require the BIND_REMOTE_DISPLAY permission in its manifest: ");
            stringBuilder.append(serviceInfo.packageName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(serviceInfo.name);
            Slog.w(str, stringBuilder.toString());
            return false;
        } else if (hasCaptureVideoPermission(serviceInfo.packageName)) {
            return true;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring remote display provider service because it does not have the CAPTURE_VIDEO_OUTPUT or CAPTURE_SECURE_VIDEO_OUTPUT permission: ");
            stringBuilder.append(serviceInfo.packageName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(serviceInfo.name);
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean hasCaptureVideoPermission(String packageName) {
        if (this.mPackageManager.checkPermission("android.permission.CAPTURE_VIDEO_OUTPUT", packageName) == 0 || this.mPackageManager.checkPermission("android.permission.CAPTURE_SECURE_VIDEO_OUTPUT", packageName) == 0) {
            return true;
        }
        return false;
    }

    private int findProvider(String packageName, String className) {
        int count = this.mProviders.size();
        for (int i = 0; i < count; i++) {
            if (((RemoteDisplayProviderProxy) this.mProviders.get(i)).hasComponentName(packageName, className)) {
                return i;
            }
        }
        return -1;
    }
}

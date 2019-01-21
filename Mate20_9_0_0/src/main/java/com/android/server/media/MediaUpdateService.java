package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.media.IMediaExtractorUpdateService;
import android.media.IMediaExtractorUpdateService.Stub;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.pm.DumpState;

public class MediaUpdateService extends SystemService {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String EXTRACTOR_UPDATE_SERVICE_NAME = "media.extractor.update";
    private static final String MEDIA_UPDATE_PACKAGE_NAME = SystemProperties.get("ro.mediacomponents.package");
    private static final String TAG = "MediaUpdateService";
    final Handler mHandler = new Handler();
    private IMediaExtractorUpdateService mMediaExtractorUpdateService;

    public MediaUpdateService(Context context) {
        super(context);
    }

    public void onStart() {
        if (("userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE)) && !TextUtils.isEmpty(MEDIA_UPDATE_PACKAGE_NAME)) {
            connect();
            registerBroadcastReceiver();
        }
    }

    private void connect() {
        IBinder binder = ServiceManager.getService(EXTRACTOR_UPDATE_SERVICE_NAME);
        if (binder != null) {
            try {
                binder.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        Slog.w(MediaUpdateService.TAG, "mediaextractor died; reconnecting");
                        MediaUpdateService.this.mMediaExtractorUpdateService = null;
                        MediaUpdateService.this.connect();
                    }
                }, 0);
            } catch (Exception e) {
                binder = null;
            }
        }
        if (binder != null) {
            this.mMediaExtractorUpdateService = Stub.asInterface(binder);
            this.mHandler.post(new Runnable() {
                public void run() {
                    MediaUpdateService.this.packageStateChanged();
                }
            });
            return;
        }
        Slog.w(TAG, "media.extractor.update not found.");
    }

    private void registerBroadcastReceiver() {
        BroadcastReceiver updateReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Removed duplicated region for block: B:21:0x0051  */
            /* JADX WARNING: Removed duplicated region for block: B:20:0x004b  */
            /* JADX WARNING: Removed duplicated region for block: B:19:0x0045  */
            /* JADX WARNING: Removed duplicated region for block: B:21:0x0051  */
            /* JADX WARNING: Removed duplicated region for block: B:20:0x004b  */
            /* JADX WARNING: Removed duplicated region for block: B:19:0x0045  */
            /* JADX WARNING: Missing block: B:13:0x0033, code skipped:
            if (r0.equals("android.intent.action.PACKAGE_REMOVED") != false) goto L_0x0041;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                int i = 0;
                if (intent.getIntExtra("android.intent.extra.user_handle", 0) == 0) {
                    String action = intent.getAction();
                    int hashCode = action.hashCode();
                    if (hashCode != 172491798) {
                        if (hashCode != 525384130) {
                            if (hashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) {
                                i = 2;
                                switch (i) {
                                    case 0:
                                        if (!intent.getExtras().getBoolean("android.intent.extra.REPLACING")) {
                                            MediaUpdateService.this.packageStateChanged();
                                            break;
                                        }
                                        return;
                                    case 1:
                                        MediaUpdateService.this.packageStateChanged();
                                        break;
                                    case 2:
                                        MediaUpdateService.this.packageStateChanged();
                                        break;
                                }
                            }
                        }
                    } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        i = 1;
                        switch (i) {
                            case 0:
                                break;
                            case 1:
                                break;
                            case 2:
                                break;
                        }
                    }
                    i = -1;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart(MEDIA_UPDATE_PACKAGE_NAME, 0);
        getContext().registerReceiverAsUser(updateReceiver, UserHandle.ALL, filter, null, null);
    }

    private void packageStateChanged() {
        String str;
        ApplicationInfo packageInfo = null;
        boolean pluginsAvailable = false;
        try {
            packageInfo = getContext().getPackageManager().getApplicationInfo(MEDIA_UPDATE_PACKAGE_NAME, DumpState.DUMP_DEXOPT);
            pluginsAvailable = packageInfo.enabled;
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package '");
            stringBuilder.append(MEDIA_UPDATE_PACKAGE_NAME);
            stringBuilder.append("' not installed");
            Slog.v(str2, stringBuilder.toString());
        }
        if (!(packageInfo == null || VERSION.SDK_INT == packageInfo.targetSdkVersion)) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("This update package is not for this platform version. Ignoring. platform:");
            stringBuilder2.append(VERSION.SDK_INT);
            stringBuilder2.append(" targetSdk:");
            stringBuilder2.append(packageInfo.targetSdkVersion);
            Slog.w(str, stringBuilder2.toString());
            pluginsAvailable = false;
        }
        str = (packageInfo == null || !pluginsAvailable) ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : packageInfo.sourceDir;
        loadExtractorPlugins(str);
    }

    private void loadExtractorPlugins(String apkPath) {
        try {
            if (this.mMediaExtractorUpdateService != null) {
                this.mMediaExtractorUpdateService.loadPlugins(apkPath);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error in loadPlugins", e);
        }
    }
}

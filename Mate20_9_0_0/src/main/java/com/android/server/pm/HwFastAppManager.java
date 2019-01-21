package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Slog;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HwFastAppManager {
    private static final String ACTION_FAST_APP_ENGINE_READY = "com.huawei.fastapp.FASTAPP_ENGINE_READY";
    private static final Uri FAST_APP_CONTENT_URI = Uri.parse("content://com.huawei.fastapp.provider/installed_app_info");
    private static final String FAST_APP_PACKAGE_NAME = "com.huawei.fastapp";
    private static final String FAST_APP_PROVIDER = "com.huawei.fastapp.provider";
    private static final String FAST_APP_SHA256 = "uSglwr1dbW0efznuzReEO32QFvYRE2t1RBvG9NPwDwU=";
    private static final String TAG = "HwFastAppManager";
    final Context mContext;
    private PackageInfo mFastAppBasePackageInfo = null;
    private final List<String> mFastAppCallingStack = new ArrayList();
    private FastAppObserver mFastAppObserver = null;
    private final HashMap<String, FastAppRecord> mFastAppRecordStack = new HashMap();
    private Handler mHandler = null;
    private final List<String> mSupportApkList = new ArrayList();
    private BroadcastReceiver receiver = null;

    private class FastAppObserver extends ContentObserver {
        private static final String FAST_APP_INFO_PACKAGE_NAME = "app_package_name";
        private static final String FAST_APP_INFO_SIGNATURE = "app_signature_info";

        public FastAppObserver(Handler handler) {
            super(handler);
        }

        public boolean deliverSelfNotifications() {
            return true;
        }

        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        /* JADX WARNING: Missing block: B:25:0x007e, code skipped:
            if (r8 != null) goto L_0x0080;
     */
        /* JADX WARNING: Missing block: B:26:0x0080, code skipped:
            r8.close();
     */
        /* JADX WARNING: Missing block: B:36:0x00a4, code skipped:
            if (r8 == null) goto L_0x00a7;
     */
        /* JADX WARNING: Missing block: B:37:0x00a7, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver contentResolver = HwFastAppManager.this.mContext.getContentResolver();
            if (contentResolver == null) {
                Slog.w(HwFastAppManager.TAG, "FastAppOnserver.onChange: content resolver is null");
                return;
            }
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(HwFastAppManager.FAST_APP_CONTENT_URI, new String[]{FAST_APP_INFO_PACKAGE_NAME, FAST_APP_INFO_SIGNATURE}, null, null, null, null);
                if (cursor == null) {
                    Slog.w(HwFastAppManager.TAG, "FastAppOnserver.onChange: cursor is null");
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
                int packageNameColumn = cursor.getColumnIndexOrThrow(FAST_APP_INFO_PACKAGE_NAME);
                int certificateColumn = cursor.getColumnIndexOrThrow(FAST_APP_INFO_SIGNATURE);
                synchronized (HwFastAppManager.this.mFastAppRecordStack) {
                    HwFastAppManager.this.mFastAppRecordStack.clear();
                    cursor.moveToPosition(-1);
                    while (cursor.moveToNext()) {
                        String packageName = cursor.getString(packageNameColumn);
                        String certificate = cursor.getString(certificateColumn);
                        if (packageName != null) {
                            if (certificate != null) {
                                HwFastAppManager.this.mFastAppRecordStack.put(packageName, new FastAppRecord(packageName, certificate));
                            }
                        }
                    }
                }
            } catch (Throwable error) {
                try {
                    String str = HwFastAppManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("FastAppOnserver.onChange: error when update app: ");
                    stringBuilder.append(error.getMessage());
                    Slog.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    private class FastAppRecord {
        public String packageName;
        public String signature;

        public FastAppRecord(String packageName, String signature) {
            this.packageName = packageName;
            this.signature = signature;
        }

        public PackageInfo toPackageInfo(PackageInfo base) {
            PackageInfo packageInfo = base != null ? base : new PackageInfo();
            packageInfo.signatures = new Signature[]{new Signature(this.signature)};
            packageInfo.packageName = this.packageName;
            return packageInfo;
        }
    }

    public HwFastAppManager(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mSupportApkList.add("com.tencent.mm");
        this.mSupportApkList.add("com.sina.weibo");
        this.mSupportApkList.add("com.tencent.mobileqq");
    }

    public PackageInfo getPacakgeInfoForFastApp(String packageName, int flag, int callingUid) {
        if (packageName == null) {
            return null;
        }
        String callingName = this.mContext.getPackageManager().getNameForUid(callingUid);
        if (callingName == null || !isNeedGetFastAppInfo(flag, callingName)) {
            return null;
        }
        synchronized (this.mFastAppRecordStack) {
            FastAppRecord fastappInfo = (FastAppRecord) this.mFastAppRecordStack.get(packageName);
            if (fastappInfo == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPacakgeInfoForFastApp: ");
                stringBuilder.append(packageName);
                stringBuilder.append(" does not exist");
                Slog.w(str, stringBuilder.toString());
                return null;
            }
            try {
                PackageInfo fastAppPackageInfo = fastappInfo.toPackageInfo(this.mFastAppBasePackageInfo);
                this.mFastAppCallingStack.add(packageName);
                return fastAppPackageInfo;
            } catch (Throwable error) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getPacakgeInfoForFastApp: error when get fastapp package: ");
                stringBuilder2.append(error.getMessage());
                Slog.e(str2, stringBuilder2.toString());
                return null;
            }
        }
    }

    private boolean isNeedGetFastAppInfo(int flag, String callingPackageName) {
        if ((flag & 64) == 0 || !this.mSupportApkList.contains(callingPackageName)) {
            return false;
        }
        return true;
    }

    private boolean registerFastAppObserver() {
        if (this.mFastAppObserver != null) {
            return true;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            return false;
        }
        try {
            ProviderInfo provInfo = packageManager.resolveContentProvider(FAST_APP_PROVIDER, 0);
            if (provInfo != null) {
                if (FAST_APP_PACKAGE_NAME.equals(provInfo.packageName)) {
                    this.mFastAppBasePackageInfo = packageManager.getPackageInfo(FAST_APP_PACKAGE_NAME, 64);
                    if (checkFastAppSignature(this.mFastAppBasePackageInfo.signatures)) {
                        ContentResolver contentResolver = this.mContext.getContentResolver();
                        if (contentResolver == null) {
                            Slog.w(TAG, "registerFastAppObserver: content resolver is null");
                            return false;
                        }
                        this.mFastAppObserver = new FastAppObserver(this.mHandler);
                        contentResolver.registerContentObserver(FAST_APP_CONTENT_URI, true, this.mFastAppObserver);
                        this.mFastAppObserver.dispatchChange(true, FAST_APP_CONTENT_URI);
                        return true;
                    }
                    Slog.w(TAG, "registerFastAppObserver: fail to check fastapp signature");
                    return false;
                }
            }
            Slog.w(TAG, "registerFastAppObserver: provInfo is wrong");
            return false;
        } catch (Throwable error) {
            this.mFastAppObserver = null;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerFastAppObserver: error when register observer: ");
            stringBuilder.append(error.getMessage());
            Slog.w(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean checkFastAppSignature(Signature[] signatures) {
        if (signatures == null || signatures.length == 0) {
            return false;
        }
        try {
            for (Signature signature : signatures) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(signature.toByteArray());
                if (FAST_APP_SHA256.equals(Base64.encodeToString(messageDigest.digest(), 2))) {
                    return true;
                }
            }
            return false;
        } catch (Throwable th) {
            Slog.w(TAG, "checkFastAppSignature: error when getSHA256");
            return false;
        }
    }

    public void systemReady() {
        try {
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (HwFastAppManager.ACTION_FAST_APP_ENGINE_READY.equals(intent.getAction()) && HwFastAppManager.this.registerFastAppObserver()) {
                        HwFastAppManager.this.mContext.unregisterReceiver(this);
                    }
                }
            }, new IntentFilter(ACTION_FAST_APP_ENGINE_READY));
        } catch (Throwable error) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("systemReady: error when register Receiver: ");
            stringBuilder.append(error.getMessage());
            Slog.e(str, stringBuilder.toString());
        }
    }

    public void updateActivityInfo(int flags, Intent intent, int callingUid, List<ResolveInfo> list) {
        if (intent != null) {
            String callingName = this.mContext.getPackageManager().getNameForUid(callingUid);
            if (callingName != null && this.mSupportApkList.contains(callingName)) {
                ComponentName comp = intent.getComponent();
                if (comp != null) {
                    ComponentName baseComp = null;
                    String packageName = comp.getPackageName();
                    if (this.mFastAppCallingStack.contains(packageName)) {
                        String activityName = new StringBuilder();
                        activityName.append(FAST_APP_PACKAGE_NAME);
                        activityName.append(comp.getClassName().substring(packageName.length()));
                        baseComp = new ComponentName(FAST_APP_PACKAGE_NAME, activityName.toString());
                        this.mFastAppCallingStack.remove(packageName);
                    }
                    if (baseComp == null) {
                        Slog.e(TAG, "updateActivityInfo baseComp not found");
                        return;
                    }
                    try {
                        ActivityInfo baseInfo = this.mContext.getPackageManager().getActivityInfo(baseComp, flags);
                        ResolveInfo ri = new ResolveInfo();
                        ri.activityInfo = baseInfo;
                        list.add(ri);
                    } catch (NameNotFoundException e) {
                        Slog.e(TAG, "updateActivityInfo not found");
                    }
                }
            }
        }
    }
}

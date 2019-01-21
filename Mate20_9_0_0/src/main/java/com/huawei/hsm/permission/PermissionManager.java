package com.huawei.hsm.permission;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hsm.HwSystemManager.Notifier;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Slog;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PermissionManager {
    private static final String TAG = PermissionManager.class.getSimpleName();

    public static boolean canStartActivity(Context context, Intent intent) {
        boolean z = true;
        if (intent == null) {
            return true;
        }
        boolean block = CallPermission.blockStartActivity(context, intent) || ConnectPermission.blockStartActivity(context, intent);
        if (block) {
            z = false;
        }
        return z;
    }

    public static boolean canSendBroadcast(Context context, Intent intent) {
        if (intent == null) {
            return true;
        }
        return new SendBroadcastPermission(context).allowSendBroadcast(intent);
    }

    public static void insertSendBroadcastRecord(String pkgName, String action, int uid) {
        new SendBroadcastPermission().insertSendBroadcastRecord(pkgName, action, uid);
    }

    public static boolean allowOp(Uri uri, int action) {
        return ContentPermission.allowContentOpInner(uri, action);
    }

    public static boolean allowOp(String destAddr, String smsBody, PendingIntent sentIntent) {
        return SmsPermission.getInstance().isSmsBlocked(destAddr, smsBody, sentIntent) ^ 1;
    }

    public static boolean allowOp(String destAddr, String smsBody, List<PendingIntent> sentIntents) {
        return SmsPermission.getInstance().isSmsBlocked(destAddr, smsBody, (List) sentIntents) ^ 1;
    }

    public static void authenticateSmsSend(Notifier callback, int callingUid, int smsId, String smsBody, String smsAddress) {
        SmsPermission.getInstance().authenticateSmsSend(callback, callingUid, smsId, smsBody, smsAddress);
    }

    public static void notifyBackgroundMgr(String pkgName, int pid, int uidOf3RdApk, int permType, int permCfg) {
        BackgroundPermManager.getInstance().notifyBackgroundMgr(pkgName, pid, uidOf3RdApk, permType, permCfg);
    }

    public static boolean allowOp(int type) {
        if (1024 == type) {
            CameraPermission cameraPermission = new CameraPermission();
            cameraPermission.remind();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("camera remind result:");
            stringBuilder.append(cameraPermission.isCameraBlocked ^ 1);
            Slog.i(str, stringBuilder.toString());
            return 1 ^ cameraPermission.isCameraBlocked;
        } else if (128 == type) {
            return new AudioRecordPermission().remindWithResult();
        } else {
            if (StubController.PERMISSION_GET_PACKAGE_LIST == type) {
                return new AppListPermission().allowOp();
            }
            if (StubController.RMD_PERMISSION_CODE == type) {
                return new ReadMotionDataPermission().allowOp();
            }
            if (StubController.RHD_PERMISSION_CODE == type) {
                return new ReadHealthDataPermission().allowOp();
            }
            if (StubController.PERMISSION_EDIT_SHORTCUT == type) {
                return new PinShortcutPermission().allowOp();
            }
            return true;
        }
    }

    public static boolean allowOp(Context cxt, int type) {
        if (StubController.PERMISSION_BLUETOOTH == type) {
            return ConnectPermission.allowOpenBt(cxt);
        }
        if (StubController.PERMISSION_MOBILEDATE == type) {
            return ConnectPermission.allowOpenMobile(cxt);
        }
        if (StubController.PERMISSION_WIFI == type) {
            return ConnectPermission.allowOpenWifi(cxt);
        }
        if (8 == type) {
            return 1 ^ new LocationPermission(cxt).isLocationBlocked();
        }
        return true;
    }

    public static boolean allowOp(Context cxt, int type, boolean enable) {
        if (enable) {
            return allowOp(cxt, type);
        }
        return true;
    }

    public static Location getFakeLocation(String name) {
        return LocationPermission.getFakeLocation(name);
    }

    public static Cursor getDummyCursor(ContentResolver resolver, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return ContentPermission.getDummyCursor(resolver, uri, projection, selection, selectionArgs, sortOrder);
    }

    public static void setOutputFile(MediaRecorder recorder) throws IllegalStateException, IOException {
        Slog.d(TAG, "set put File null");
        FileOutputStream fos = new FileOutputStream("dev/null");
        try {
            recorder.setInterOutputFile(fos.getFD());
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Slog.e(TAG, "close output file fail");
            }
        }
    }
}

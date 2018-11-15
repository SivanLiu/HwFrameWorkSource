package com.android.server.wm;

import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Slog;
import com.android.server.policy.IconUtilities;

class AlertWindowNotification {
    private static final String CHANNEL_PREFIX = "com.android.server.wm.AlertWindowNotification - ";
    private static final int NOTIFICATION_ID = 0;
    private static final String PKG_NOTE_PAD = "com.example.android.notepad";
    private static final String TAG = "AlertWindowNotification";
    private static NotificationChannelGroup sChannelGroup;
    private static int sNextRequestCode = 0;
    private IconUtilities mIconUtilities;
    private final NotificationManager mNotificationManager = ((NotificationManager) this.mService.mContext.getSystemService("notification"));
    private String mNotificationTag;
    private final String mPackageName;
    private boolean mPosted;
    private final int mRequestCode;
    private final WindowManagerService mService;

    AlertWindowNotification(WindowManagerService service, String packageName) {
        this.mService = service;
        this.mPackageName = packageName;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CHANNEL_PREFIX);
        stringBuilder.append(this.mPackageName);
        this.mNotificationTag = stringBuilder.toString();
        int i = sNextRequestCode;
        sNextRequestCode = i + 1;
        this.mRequestCode = i;
        this.mIconUtilities = new IconUtilities(this.mService.mContext);
    }

    void post() {
        this.mService.mH.post(new -$$Lambda$AlertWindowNotification$iVtcJMb6VtqtAgEtGUDCkGay0tM(this));
    }

    void cancel(boolean deleteChannel) {
        this.mService.mH.post(new -$$Lambda$AlertWindowNotification$ZuqSYML-X-nkNVTba_yeIT9hJ1s(this, deleteChannel));
    }

    private void onCancelNotification(boolean deleteChannel) {
        if (this.mPosted) {
            this.mPosted = false;
            this.mNotificationManager.cancel(this.mNotificationTag, 0);
            if (deleteChannel) {
                this.mNotificationManager.deleteNotificationChannel(this.mNotificationTag);
            }
        }
    }

    private void onPostNotification() {
        if (!this.mPosted) {
            this.mPosted = true;
            Context context = this.mService.mContext;
            PackageManager pm = context.getPackageManager();
            ApplicationInfo aInfo = getApplicationInfo(pm, this.mPackageName);
            String appName = aInfo != null ? pm.getApplicationLabel(aInfo).toString() : this.mPackageName;
            if (aInfo == null || (aInfo.privateFlags & 8) == 0 || !PKG_NOTE_PAD.equals(this.mPackageName)) {
                createNotificationChannel(context, appName);
                String message = context.getString(17039572, new Object[]{appName});
                Bundle extras = new Bundle();
                extras.putStringArray("android.foregroundApps", new String[]{this.mPackageName});
                Builder builder = new Builder(context, this.mNotificationTag).setOngoing(true).setContentTitle(context.getString(17039573, new Object[]{appName})).setContentText(message).setSmallIcon(17301708).setColor(context.getColor(17170784)).setStyle(new BigTextStyle().bigText(message)).setLocalOnly(true).addExtras(extras).setContentIntent(getContentIntent(context, this.mPackageName));
                if (aInfo != null) {
                    Drawable drawable = pm.getApplicationIcon(aInfo);
                    if (drawable != null) {
                        builder.setLargeIcon(this.mIconUtilities.createIconBitmap(drawable));
                    }
                }
                this.mNotificationManager.notify(this.mNotificationTag, 0, builder.build());
                return;
            }
            this.mPosted = false;
        }
    }

    private PendingIntent getContentIntent(Context context, String packageName) {
        Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.fromParts("package", packageName, null));
        intent.setFlags(268468224);
        return PendingIntent.getActivity(context, this.mRequestCode, intent, 268435456);
    }

    private void createNotificationChannel(Context context, String appName) {
        if (sChannelGroup == null) {
            sChannelGroup = new NotificationChannelGroup(CHANNEL_PREFIX, this.mService.mContext.getString(17039570));
            this.mNotificationManager.createNotificationChannelGroup(sChannelGroup);
        }
        String nameChannel = context.getString(17039571, new Object[]{appName});
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createNotificationChannel for appName:");
        stringBuilder.append(appName);
        stringBuilder.append(", PackageName is:");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(",nameChannel is:");
        stringBuilder.append(nameChannel);
        Slog.w(str, stringBuilder.toString());
        if (this.mNotificationManager.getNotificationChannel(this.mNotificationTag) == null) {
            NotificationChannel channel = new NotificationChannel(this.mNotificationTag, nameChannel, 1);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setBlockableSystem(true);
            channel.setGroup(sChannelGroup.getId());
            channel.setBypassDnd(true);
            this.mNotificationManager.createNotificationChannel(channel);
        }
    }

    private ApplicationInfo getApplicationInfo(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return null;
        }
    }
}

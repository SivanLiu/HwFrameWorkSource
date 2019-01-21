package com.huawei.android.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.util.NotificationMessagingUtil;

public class NotificationEx {

    public static class Builder {
        public static void setAppName(android.app.Notification.Builder builder, CharSequence appName) {
            builder.setAppName(appName);
        }
    }

    public static void setLatestEventInfo(Notification notification, Context context, CharSequence contentTitle, CharSequence contentText, PendingIntent contentIntent) {
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
    }

    public static String getExtraSubstituteAppName() {
        return "android.substName";
    }

    public static boolean isGroupSummary(Notification notification) {
        return notification.isGroupSummary();
    }

    public static boolean isGroupChild(Notification notification) {
        return notification.isGroupChild();
    }

    public static String loadHeaderAppName(android.app.Notification.Builder builder) {
        return builder.loadHeaderAppName();
    }

    public static String getExtraApplicationInfo() {
        return "android.appInfo";
    }

    public static boolean showsTime(Notification notification) {
        return notification.showsTime();
    }

    public static boolean isColorized(Notification notification) {
        return notification.isColorized();
    }

    public static RemoteViews createContentView(android.app.Notification.Builder builder, boolean increasedHeight) {
        return builder.createContentView(increasedHeight);
    }

    public static RemoteViews makeLowPriorityContentView(android.app.Notification.Builder builder, boolean useRegularSubtext) {
        return builder.makeLowPriorityContentView(useRegularSubtext);
    }

    public static void makeHeaderExpanded(RemoteViews result) {
        android.app.Notification.Builder.makeHeaderExpanded(result);
    }

    public static RemoteViews makeAmbientNotification(android.app.Notification.Builder builder) {
        return builder.makeAmbientNotification();
    }

    public static RemoteViews makePublicAmbientNotification(android.app.Notification.Builder builder) {
        return builder.makePublicAmbientNotification();
    }

    public static RemoteViews makePublicContentView(android.app.Notification.Builder builder) {
        return builder.makePublicContentView();
    }

    public static RemoteViews createHeadsUpContentView(android.app.Notification.Builder builder, boolean increasedHeight) {
        return builder.createHeadsUpContentView(increasedHeight);
    }

    public static RemoteViews makeNotificationHeader(android.app.Notification.Builder builder, boolean ambient) {
        return builder.makeNotificationHeader(ambient);
    }

    public static Context getPackageContext(StatusBarNotification sbn, Context context) {
        if (sbn != null) {
            return sbn.getPackageContext(context);
        }
        return null;
    }

    public static String getOpPkg(StatusBarNotification sbn) {
        if (sbn != null) {
            return sbn.getOpPkg();
        }
        return null;
    }

    public static int getUid(StatusBarNotification sbn) {
        if (sbn != null) {
            return sbn.getUid();
        }
        return -1;
    }

    public static int getInitialPid(StatusBarNotification sbn) {
        if (sbn != null) {
            return sbn.getInitialPid();
        }
        return -1;
    }

    public static StatusBarNotification getStatusBarNotification(String pkg, String opPkg, int id, String tag, int uid, int initialPid, Notification notification, UserHandle user, String overrideGroupKey, long postTime) {
        return new StatusBarNotification(pkg, opPkg, id, tag, uid, initialPid, notification, user, overrideGroupKey, postTime);
    }

    public static boolean isImportantMessaging(Context context, StatusBarNotification sbn, int importance) {
        return new NotificationMessagingUtil(context).isImportantMessaging(sbn, importance);
    }

    public static int resolveContrastColor(Context context, int notificationColor, int backgroundColor) {
        return NotificationColorUtil.resolveContrastColor(context, notificationColor, backgroundColor);
    }

    public static boolean isGrayscaleIcon(Context context, Drawable drawable) {
        return NotificationColorUtil.getInstance(context).isGrayscaleIcon(drawable);
    }

    public static int getDismissalShade() {
        return 3;
    }
}

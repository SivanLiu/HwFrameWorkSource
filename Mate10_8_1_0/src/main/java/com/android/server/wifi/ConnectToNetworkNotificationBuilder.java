package com.android.server.wifi;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Action.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import com.android.internal.notification.SystemNotificationChannels;

public class ConnectToNetworkNotificationBuilder {
    public static final String ACTION_CONNECT_TO_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.CONNECT_TO_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.PICK_WIFI_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE = "com.android.server.wifi.ConnectToNetworkNotification.PICK_NETWORK_AFTER_FAILURE";
    public static final String ACTION_USER_DISMISSED_NOTIFICATION = "com.android.server.wifi.ConnectToNetworkNotification.USER_DISMISSED_NOTIFICATION";
    private Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private Resources mResources;

    public ConnectToNetworkNotificationBuilder(Context context, FrameworkFacade framework) {
        this.mContext = context;
        this.mResources = context.getResources();
        this.mFrameworkFacade = framework;
    }

    public Notification createConnectToNetworkNotification(ScanResult network) {
        Action connectAction = new Builder(null, this.mResources.getText(17041261), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK)).build();
        return createNotificationBuilder(this.mContext.getText(17041264), network.SSID).addAction(connectAction).addAction(new Builder(null, this.mResources.getText(17041260), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK)).build()).build();
    }

    public Notification createNetworkConnectingNotification(ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17041266), network.SSID).setProgress(0, 0, true).build();
    }

    public Notification createNetworkConnectedNotification(ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17041265), network.SSID).build();
    }

    public Notification createNetworkFailedNotification() {
        return createNotificationBuilder(this.mContext.getText(17041267), this.mContext.getText(17041262)).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE)).setAutoCancel(true).build();
    }

    private Notification.Builder createNotificationBuilder(CharSequence title, CharSequence content) {
        return this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_AVAILABLE).setSmallIcon(17303456).setTicker(title).setContentTitle(title).setContentText(content).setDeleteIntent(getPrivateBroadcast(ACTION_USER_DISMISSED_NOTIFICATION)).setShowWhen(false).setLocalOnly(true).setColor(this.mResources.getColor(17170772, this.mContext.getTheme()));
    }

    private PendingIntent getPrivateBroadcast(String action) {
        return this.mFrameworkFacade.getBroadcast(this.mContext, 0, new Intent(action).setPackage("android"), 134217728);
    }
}

package com.android.server.wifi;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Action.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;

public class ConnectToNetworkNotificationBuilder {
    public static final String ACTION_CONNECT_TO_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.CONNECT_TO_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK = "com.android.server.wifi.ConnectToNetworkNotification.PICK_WIFI_NETWORK";
    public static final String ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE = "com.android.server.wifi.ConnectToNetworkNotification.PICK_NETWORK_AFTER_FAILURE";
    public static final String ACTION_USER_DISMISSED_NOTIFICATION = "com.android.server.wifi.ConnectToNetworkNotification.USER_DISMISSED_NOTIFICATION";
    public static final String AVAILABLE_NETWORK_NOTIFIER_TAG = "com.android.server.wifi.ConnectToNetworkNotification.AVAILABLE_NETWORK_NOTIFIER_TAG";
    private Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private Resources mResources;

    public ConnectToNetworkNotificationBuilder(Context context, FrameworkFacade framework) {
        this.mContext = context;
        this.mResources = context.getResources();
        this.mFrameworkFacade = framework;
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0028  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x003f  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0028  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x003f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Notification createConnectToAvailableNetworkNotification(String notifierTag, ScanResult network) {
        Object obj;
        CharSequence title;
        Action connectAction;
        int hashCode = notifierTag.hashCode();
        if (hashCode != 594918769) {
            if (hashCode == 2017428693 && notifierTag.equals(OpenNetworkNotifier.TAG)) {
                obj = null;
                switch (obj) {
                    case null:
                        title = this.mContext.getText(17041388);
                        break;
                    case 1:
                        title = this.mContext.getText(17041385);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown network notifier.");
                        stringBuilder.append(notifierTag);
                        Log.wtf("ConnectToNetworkNotificationBuilder", stringBuilder.toString());
                        return null;
                }
                connectAction = new Builder(null, this.mResources.getText(17041384), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, notifierTag)).build();
                return createNotificationBuilder(title, network.SSID, notifierTag).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).addAction(connectAction).addAction(new Builder(null, this.mResources.getText(17041383), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).build()).build();
            }
        } else if (notifierTag.equals(CarrierNetworkNotifier.TAG)) {
            obj = 1;
            switch (obj) {
                case null:
                    break;
                case 1:
                    break;
                default:
                    break;
            }
            connectAction = new Builder(null, this.mResources.getText(17041384), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, notifierTag)).build();
            return createNotificationBuilder(title, network.SSID, notifierTag).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).addAction(connectAction).addAction(new Builder(null, this.mResources.getText(17041383), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).build()).build();
        }
        obj = -1;
        switch (obj) {
            case null:
                break;
            case 1:
                break;
            default:
                break;
        }
        connectAction = new Builder(null, this.mResources.getText(17041384), getPrivateBroadcast(ACTION_CONNECT_TO_NETWORK, notifierTag)).build();
        return createNotificationBuilder(title, network.SSID, notifierTag).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).addAction(connectAction).addAction(new Builder(null, this.mResources.getText(17041383), getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK, notifierTag)).build()).build();
    }

    public Notification createNetworkConnectingNotification(String notifierTag, ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17041390), network.SSID, notifierTag).setProgress(0, 0, true).build();
    }

    public Notification createNetworkConnectedNotification(String notifierTag, ScanResult network) {
        return createNotificationBuilder(this.mContext.getText(17041389), network.SSID, notifierTag).build();
    }

    public Notification createNetworkFailedNotification(String notifierTag) {
        return createNotificationBuilder(this.mContext.getText(17041391), this.mContext.getText(17041386), notifierTag).setContentIntent(getPrivateBroadcast(ACTION_PICK_WIFI_NETWORK_AFTER_CONNECT_FAILURE, notifierTag)).setAutoCancel(true).build();
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0029 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002c A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002a  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0029 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002c A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int getNotifierRequestCode(String notifierTag) {
        int hashCode = notifierTag.hashCode();
        if (hashCode == 594918769) {
            if (notifierTag.equals(CarrierNetworkNotifier.TAG)) {
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 2017428693 && notifierTag.equals(OpenNetworkNotifier.TAG)) {
            hashCode = 0;
            switch (hashCode) {
                case 0:
                    return 1;
                case 1:
                    return 2;
                default:
                    return 0;
            }
        }
        hashCode = -1;
        switch (hashCode) {
            case 0:
                break;
            case 1:
                break;
            default:
                break;
        }
    }

    private Notification.Builder createNotificationBuilder(CharSequence title, CharSequence content, String extraData) {
        return this.mFrameworkFacade.makeNotificationBuilder(this.mContext, SystemNotificationChannels.NETWORK_AVAILABLE).setSmallIcon(17303481).setTicker(title).setContentTitle(title).setContentText(content).setDeleteIntent(getPrivateBroadcast(ACTION_USER_DISMISSED_NOTIFICATION, extraData)).setShowWhen(false).setLocalOnly(true).setColor(this.mResources.getColor(17170784, this.mContext.getTheme()));
    }

    private PendingIntent getPrivateBroadcast(String action, String extraData) {
        Intent intent = new Intent(action).setPackage("android");
        int requestCode = 0;
        if (extraData != null) {
            intent.putExtra(AVAILABLE_NETWORK_NOTIFIER_TAG, extraData);
            requestCode = getNotifierRequestCode(extraData);
        }
        return this.mFrameworkFacade.getBroadcast(this.mContext, requestCode, intent, 134217728);
    }
}

package com.android.server.wifi.wifipro;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class WifiProUIDisplayManager {
    public static final String ACTION_HIGH_MOBILE_DATA_DELETE = "com.android.server.wifi.wifipro.hmd.delete";
    public static final String ACTION_HIGH_MOBILE_DATA_ROVE_IN = "com.android.server.wifi.wifipro.hmd.rove.in";
    private static final String AUTO_CONNECT_WIFI = "auto_connect_wifi";
    private static final int AUTO_CONNECT_WIFI_AUTO = 1;
    private static final int AUTO_CONNECT_WIFI_NEVER = 2;
    private static final int AUTO_CONNECT_WIFI_NOTIFY = 0;
    private static final String BCM_PERMISSION = "huawei.permission.RECEIVE_WIFI_PRO_STATE";
    private static final boolean DBG = true;
    public static final int DIALOG_STATUS_CANCEL = 1;
    public static final int DIALOG_STATUS_OK = 2;
    public static final int DIALOG_TYPE_AUTO_CONNECT_WIFI = 3;
    public static final int DIALOG_TYPE_CONNECT_INVALID_AP = 4;
    public static final int DIALOG_TYPE_CONNECT_PROTAL_AP_NO_LOGIN = 5;
    public static final int DIALOG_TYPE_CONNECT_WIFI_POOR = 6;
    public static final int DIALOG_TYPE_WIFI_HANDOVER_MOBILE = 102;
    public static final int DIALOG_TYPE_WIFI_HANDOVER_MOBILE_WIFI_NO_INTERNET = 2;
    public static final int DIALOG_TYPE_WIFI_HANDOVER_MOBILE_WIFI_POOR = 1;
    public static final int DIALOG_TYPE_WIFI_HANDOVER_WIFI = 101;
    private static final String EXTRA_NETWORK_CHANGE_TYPE = "extra_network_change_type";
    private static final String EXTRA_WIFI_NO_INTERNET = "extra_wifipro_no_Internet";
    private static final int HMDF_NOTIFICATION_TAG = 188223;
    private static final String TAG = "WiFi_PRO_WifiProUIDisplayManager";
    public static final int TOAST_TYPE_RECOVER_WIFI_CONNECT = 4;
    public static final int TOAST_TYPE_WIFI_DISENABLE = 2;
    public static final int TOAST_TYPE_WIFI_HANDOVER_MOBILE = 1;
    public static final int TOAST_TYPE_WIFI_UNUSABLE = 3;
    private static final String WIFI_PRO_INTERNET_ACCESS_CHANGE_ACTION = "huawei.wifi.pro.INTERNET_ACCESS_CHANGE";
    private static final String WIFI_PRO_NETWORK_CHANGE_ACTION = "huawei.wifi.pro.NETWORK_CHANGE";
    private static final String WIFI_PRO_NOTIFY_CHANNEL = "wifi_pro_notify_channel";
    private static final String WIFI_TO_MOBILE = "wifi_to_pdp";
    private static final int WIFI_TO_MOBILE_AUTO = 1;
    private static final int WIFI_TO_MOBILE_NEVER = 2;
    private static final int WIFI_TO_MOBILE_NOTIFY = 0;
    private static WifiProUIDisplayManager mWifiProUIDisplayManager;
    private int invalidAPUserChoice = -1;
    private IWifiProUICallBack mCallBack;
    private Context mContext;
    private Notification mHmdNotification = null;
    private NotificationManager mHmdNotificationManager = null;
    private OnDismissListener mInvalidAPSingleChoiceListener = new OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            WifiProUIDisplayManager.this.invalidAPUserChoice = 1;
        }
    };
    private AlertDialog mInvalidAPSingleChoiceWarningDialog = null;
    public volatile boolean mIsNotificationShown;
    private volatile boolean mIsRemindWifiHandoverMobile;
    private OnDismissListener mSwitchListener = new OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            WifiProUIDisplayManager.this.mWifiToMobileDialog = null;
        }
    };
    private AlertDialog mWifiToMobileDialog = null;

    private WifiProUIDisplayManager(Context context, IWifiProUICallBack callBack) {
        this.mContext = context;
        this.mCallBack = callBack;
        this.mIsRemindWifiHandoverMobile = false;
        this.mHmdNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mHmdNotificationManager.createNotificationChannel(new NotificationChannel(WIFI_PRO_NOTIFY_CHANNEL, context.getText(33685835), 2));
    }

    public static WifiProUIDisplayManager createInstance(Context context, IWifiProUICallBack callBack) {
        if (mWifiProUIDisplayManager == null) {
            mWifiProUIDisplayManager = new WifiProUIDisplayManager(context, callBack);
        }
        return mWifiProUIDisplayManager;
    }

    public void registerCallBack(IWifiProUICallBack callBack) {
        if (this.mCallBack == null) {
            this.mCallBack = callBack;
        }
    }

    public boolean getIsRemindWifiHandoverMobile() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsRemindWifiHandoverMobile = ");
        stringBuilder.append(this.mIsRemindWifiHandoverMobile);
        Log.i(str, stringBuilder.toString());
        return this.mIsRemindWifiHandoverMobile;
    }

    public void unRegisterCallBack() {
        this.mCallBack = null;
    }

    private void sendWifiProUICallBack(int type, int status) {
        if (this.mCallBack != null) {
            this.mCallBack.onUserConfirm(type, status);
        }
    }

    public void cancelAllDialog() {
        if (this.mInvalidAPSingleChoiceWarningDialog != null) {
            this.mInvalidAPSingleChoiceWarningDialog.cancel();
        }
        if (this.mWifiToMobileDialog != null) {
            this.mWifiToMobileDialog.cancel();
        }
    }

    public void showWifiProDialog(int key) {
        cancelAllDialog();
        switch (key) {
            case 1:
                switchToMobileNetwork(true);
                return;
            case 2:
                switchToMobileNetwork(false);
                return;
            case 4:
                showChoiceWarningDialog(false, true);
                return;
            case 5:
                showChoiceWarningDialog(true, true);
                return;
            case 6:
                showChoiceWarningDialog(false, false);
                return;
            default:
                return;
        }
    }

    public void showWifiProToast(int key) {
        switch (key) {
            case 1:
                showToast(getNetworkSwitchToMobileToast());
                return;
            case 2:
                showToast(getWlanDisconnectedToast());
                return;
            default:
                return;
        }
    }

    public void resetAlertDialog(int key) {
        if (key != 1) {
            switch (key) {
            }
        }
    }

    private void showToast(String info) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showToast +  ");
        stringBuilder.append(info);
        Log.i(str, stringBuilder.toString());
        Toast.makeText(this.mContext, info, 1).show();
    }

    public void showToastL(String info) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showToast +  ");
        stringBuilder.append(info);
        Log.i(str, stringBuilder.toString());
    }

    private AlertDialog createSwitchWifiToMobileWarning(final boolean internet) {
        Builder buider = new Builder(this.mContext, 33947691);
        View view = LayoutInflater.from(buider.getContext()).inflate(34013281, null);
        final CheckBox checkBox = (CheckBox) view.findViewById(34603192);
        if (checkBox != null) {
            if (internet) {
                checkBox.setVisibility(0);
            } else {
                checkBox.setVisibility(8);
            }
        }
        TextView textView = (TextView) view.findViewById(34603193);
        buider.setView(view);
        if (internet) {
            buider.setTitle(33685587);
        } else {
            buider.setTitle(33685588);
        }
        if (textView != null) {
            textView.setText(33685590);
        }
        buider.setIcon(17301543);
        buider.setPositiveButton(33685567, new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Log.i(WifiProUIDisplayManager.TAG, "you have chose to switch to Mobile data service!");
                WifiProUIDisplayManager.this.sendWifiProUICallBack(102, 2);
                if (checkBox != null && internet) {
                    WifiProUIDisplayManager.this.mIsRemindWifiHandoverMobile = checkBox.isChecked();
                    String str = WifiProUIDisplayManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("wifi poor, user choose swith to mobile! remeber choose : ");
                    stringBuilder.append(WifiProUIDisplayManager.this.mIsRemindWifiHandoverMobile);
                    Log.i(str, stringBuilder.toString());
                    WifiProUIDisplayManager.this.checkUserChoice(checkBox.isChecked(), true);
                }
            }
        });
        buider.setNegativeButton(33685568, new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Log.i(WifiProUIDisplayManager.TAG, "you have chose to disconnect Mobile data service!");
                WifiProUIDisplayManager.this.sendWifiProUICallBack(102, 1);
            }
        });
        AlertDialog dialog = buider.create();
        dialog.setCancelable(false);
        dialog.getWindow().setType(2008);
        return dialog;
    }

    private void switchToMobileNetwork(boolean isInternet) {
        this.mWifiToMobileDialog = createSwitchWifiToMobileWarning(isInternet);
        this.mWifiToMobileDialog.setOnDismissListener(this.mSwitchListener);
        this.mWifiToMobileDialog.show();
    }

    private void showChoiceWarningDialog(boolean isNologin, boolean isNoInternet) {
        this.mInvalidAPSingleChoiceWarningDialog = createInvalidAPSingleChoiceWarning(isNologin, isNoInternet);
        this.mInvalidAPSingleChoiceWarningDialog.setOnDismissListener(this.mInvalidAPSingleChoiceListener);
        this.mInvalidAPSingleChoiceWarningDialog.show();
    }

    private void checkUserChoice(boolean rememberChoice, boolean enableDataConnect) {
        int showPopState;
        if (!rememberChoice) {
            showPopState = 0;
        } else if (enableDataConnect) {
            showPopState = 1;
        } else {
            showPopState = 2;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkUserChoice showPopState:");
        stringBuilder.append(showPopState);
        stringBuilder.append(", enableDataConnect:");
        stringBuilder.append(enableDataConnect);
        Log.i(str, stringBuilder.toString());
        System.putInt(this.mContext.getContentResolver(), WIFI_TO_MOBILE, showPopState);
    }

    private void checkUserChoiceAutoWifi(boolean rememberChoice, boolean enableDataConnect) {
        int showPopState;
        if (!rememberChoice) {
            showPopState = 0;
        } else if (enableDataConnect) {
            showPopState = 1;
        } else {
            showPopState = 2;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkUserChoiceAutoWifi showPopState:");
        stringBuilder.append(showPopState);
        stringBuilder.append(", enableDataConnect:");
        stringBuilder.append(enableDataConnect);
        Log.i(str, stringBuilder.toString());
        System.putInt(this.mContext.getContentResolver(), AUTO_CONNECT_WIFI, showPopState);
    }

    private AlertDialog createInvalidAPSingleChoiceWarning(boolean isNologin, boolean isNoInternet) {
        Builder buider = new Builder(this.mContext, 33947691);
        if (!isNoInternet) {
            buider.setTitle(33685587);
        } else if (isNologin) {
            buider.setTitle(33685589);
        }
        buider.setMessage(33686198);
        buider.setPositiveButton(33685567, new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                String str = WifiProUIDisplayManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("createInvalidAPSingleChoiceWarning choose ");
                stringBuilder.append(WifiProUIDisplayManager.this.invalidAPUserChoice);
                Log.i(str, stringBuilder.toString());
                WifiProUIDisplayManager.this.sendWifiProUICallBack(101, 2);
            }
        });
        buider.setNegativeButton(33685568, new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Log.i(WifiProUIDisplayManager.TAG, "createInvalidAPSingleChoiceWarning choose cancel");
                WifiProUIDisplayManager.this.sendWifiProUICallBack(101, 1);
            }
        });
        AlertDialog dialog = buider.create();
        dialog.setCancelable(false);
        dialog.getWindow().setType(2008);
        return dialog;
    }

    public void showNotificationAutoOpenWLAN() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        String title = this.mContext.getResources().getString(33685892);
        String text = this.mContext.getResources().getString(33685893);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$WifiSettingsActivity"));
        intent.setFlags(335544320);
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setWhen(System.currentTimeMillis()).setTicker(title).setAutoCancel(true).setContentTitle(title).setContentText(text).setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, 1073741824)).setShowWhen(true);
        notificationManager.notify(33685578, builder.build());
    }

    public void showNotificationAutoDisableWLAN() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        String title = this.mContext.getResources().getString(33685894);
        String text = this.mContext.getResources().getString(33685895);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$WifiSettingsActivity"));
        intent.setFlags(335544320);
        Notification.Builder builder = new Notification.Builder(this.mContext);
        builder.setWhen(System.currentTimeMillis()).setTicker(title).setAutoCancel(true).setContentTitle(title).setContentText(text).setContentIntent(PendingIntent.getActivity(this.mContext, 0, intent, 1073741824)).setShowWhen(true);
        notificationManager.notify(33685581, builder.build());
    }

    public void removeNotificationAutoOpenWlan() {
        ((NotificationManager) this.mContext.getSystemService("notification")).cancel(33685578);
    }

    public void removeNotificationAutoDisableWlan() {
        ((NotificationManager) this.mContext.getSystemService("notification")).cancel(33685581);
    }

    public void showHMDNotification(int mobileDateSize, boolean isHeadsupDisplay) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(mobileDateSize);
        String title = this.mContext.getResources().getString(33685832).replace("%d", stringBuilder.toString());
        String text = this.mContext.getResources().getString(33685833);
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" title= ");
        stringBuilder2.append(title);
        stringBuilder2.append(", text= ");
        stringBuilder2.append(text);
        Log.i(str, stringBuilder2.toString());
        str = this.mContext.getPackageName();
        PendingIntent roveInPendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(ACTION_HIGH_MOBILE_DATA_ROVE_IN).setPackage(str), 268435456, UserHandle.ALL);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(ACTION_HIGH_MOBILE_DATA_DELETE).setPackage(str), 268435456, UserHandle.ALL);
        RemoteViews remoteViews = new RemoteViews(this.mContext.getPackageName(), 34013234);
        remoteViews.setImageViewResource(34603194, 33751240);
        remoteViews.setTextViewText(34603195, title);
        remoteViews.setTextViewText(34603196, text);
        Notification.Builder b = new Notification.Builder(this.mContext);
        b.setPriority(2);
        b.setContentIntent(roveInPendingIntent);
        b.setDeleteIntent(deletePendingIntent);
        b.setAutoCancel(true);
        b.setUsesChronometer(true);
        b.setTicker("");
        b.setContent(remoteViews);
        b.setWhen(System.currentTimeMillis());
        if (isHeadsupDisplay) {
            b.setDefaults(6);
        }
        b.setChannelId(WIFI_PRO_NOTIFY_CHANNEL);
        b.setSmallIcon(33751240);
        b.setVisibility(1);
        Bitmap icon = BitmapFactory.decodeResource(this.mContext.getResources(), 33751240);
        if (icon != null) {
            b.setLargeIcon(icon);
        }
        this.mHmdNotification = b.build();
        if (isHeadsupDisplay) {
            this.mHmdNotification.headsUpContentView = remoteViews;
        }
        this.mHmdNotificationManager.notify(HMDF_NOTIFICATION_TAG, this.mHmdNotification);
    }

    public void cleanUpheadNotificationHMD() {
        Log.i(TAG, "clean HMD Notification enter.");
        this.mHmdNotificationManager.cancel(HMDF_NOTIFICATION_TAG);
    }

    public Notification.Builder showPortalNotificationStatusBar(String ssid, String tag, int id, Notification.Builder builder) {
        String packageName = this.mContext.getPackageName();
        String titleText = this.mContext.getResources().getString(33685835);
        String contentText = this.mContext.getResources().getString(33685836);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(contentText);
        stringBuilder.append(" ");
        stringBuilder.append(ssid);
        contentText = stringBuilder.toString();
        PendingIntent usePendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent("com.huawei.wifipro.action.ACTION_PORTAL_USED_BY_USER").setPackage(packageName), 268435456, UserHandle.ALL);
        PendingIntent cancelPortalPendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent("com.huawei.wifipro.action.ACTION_PORTAL_CANCELED_BY_USER").setPackage(packageName), 268435456, UserHandle.ALL);
        if (builder == null) {
            builder = new Notification.Builder(this.mContext);
            builder.setPriority(2);
            builder.setContentIntent(usePendingIntent);
            builder.setDeleteIntent(cancelPortalPendingIntent);
            builder.setAutoCancel(true);
            builder.setTicker("");
            builder.setGroup("wifipro_notify_group");
            builder.setContentTitle(titleText);
            builder.setContentText(contentText);
            builder.setChannelId(WIFI_PRO_NOTIFY_CHANNEL);
            builder.setSmallIcon(getBitampIcon(33751227));
            builder.setVisibility(1);
        } else {
            builder.setContentIntent(usePendingIntent);
            builder.setDeleteIntent(cancelPortalPendingIntent);
            builder.setContentText(contentText);
        }
        Notification notification = builder.build();
        notification.flags |= 32;
        this.mHmdNotificationManager.notify(tag, id, notification);
        return builder;
    }

    public void cancelPortalNotificationStatusBar(String tag, int id) {
        this.mHmdNotificationManager.cancel(tag, id);
    }

    private String getNetworkSwitchToMobileToast() {
        return this.mContext.getResources().getString(33685576);
    }

    private String getWlanDisconnectedToast() {
        return this.mContext.getResources().getString(33685580);
    }

    public void notificateNetWorkHandover(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notificateNetWorkHandover state = ");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        Intent intent = new Intent(WIFI_PRO_NETWORK_CHANGE_ACTION);
        intent.setFlags(67108864);
        intent.putExtra(EXTRA_NETWORK_CHANGE_TYPE, state);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BCM_PERMISSION);
    }

    public void notificateNetAccessChange(boolean no_internet) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notificateNetAccessChange no_internet = ");
        stringBuilder.append(no_internet);
        Log.d(str, stringBuilder.toString());
        Intent intent = new Intent(WIFI_PRO_INTERNET_ACCESS_CHANGE_ACTION);
        intent.setFlags(67108864);
        intent.putExtra(EXTRA_WIFI_NO_INTERNET, no_internet);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, BCM_PERMISSION);
    }

    public void shownAccessNotification(boolean accessAPNotificationShown) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("shownAccessNotification  accessAPNotificationShown = ");
        stringBuilder.append(accessAPNotificationShown);
        stringBuilder.append(", mIsNotificationShown = ");
        stringBuilder.append(this.mIsNotificationShown);
        Log.d(str, stringBuilder.toString());
        Intent intent = new Intent();
        if (accessAPNotificationShown) {
            if (!this.mIsNotificationShown) {
                this.mIsNotificationShown = true;
                intent.setAction(WifiproUtils.ACTION_NOTIFY_INTERNET_ACCESS_AP_FOUND);
            } else {
                return;
            }
        } else if (this.mIsNotificationShown) {
            this.mIsNotificationShown = false;
            intent.setAction(WifiproUtils.ACTION_NOTIFY_INTERNET_ACCESS_AP_OUT_OF_RANGE);
        } else {
            return;
        }
        intent.setFlags(67108864);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private Icon getBitampIcon(int resId) {
        Drawable drawable = this.mContext.getResources().getDrawable(resId);
        if (drawable == null) {
            Log.w(TAG, "drawable is null");
            return null;
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, drawable.getOpacity() != -1 ? Config.ARGB_8888 : Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return Icon.createWithBitmap(bitmap);
    }
}

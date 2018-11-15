package com.android.server.hidata.arbitration;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.widget.Toast;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwArbitrationDisplay {
    private static final String ACTION_WIFI_PLUS_ACTIVITY = "android.settings.WIFI_PLUS_SETTINGS";
    private static final int HiData_High_NOTIFICATION_ID = 288223;
    private static final int HiData_Low_NOTIFICATION_ID = 288224;
    private static final String NotificationHighChannelTAG = "hidata_brain_high_tag";
    private static final String NotificationLowChannelTAG = "hidata_brain_low_tag";
    private static final String TAG;
    private static HwArbitrationDisplay instance;
    private Context mContext;
    private Handler mHBDHandler;
    private NotificationManager mNotificationManager;
    private long mRO_StartRxBytes;
    private long mRO_StartTxBytes;
    private boolean mplinkEnableState = false;
    private boolean noNotify25MB;
    private boolean noNotify50MB;
    private boolean noSetHighChanel;
    private boolean noSetLowChanel;
    private boolean smartmpEnableState = false;
    private boolean startMonitorDataFlow;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HwArbitrationDEFS.BASE_TAG);
        stringBuilder.append(HwArbitrationDisplay.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private HwArbitrationDisplay(Context mContext) {
        HwArbitrationCommonUtils.logD(TAG, "init HwArbitrationDisplay");
        this.mContext = mContext;
        resetState();
        initHBDHandler();
        this.mNotificationManager = (NotificationManager) mContext.getSystemService("notification");
    }

    public static synchronized HwArbitrationDisplay getInstance(Context context) {
        HwArbitrationDisplay hwArbitrationDisplay;
        synchronized (HwArbitrationDisplay.class) {
            if (instance == null) {
                instance = new HwArbitrationDisplay(context);
            }
            hwArbitrationDisplay = instance;
        }
        return hwArbitrationDisplay;
    }

    private void resetState() {
        HwArbitrationCommonUtils.logD(TAG, "resetState Notification State");
        this.noNotify25MB = true;
        this.noNotify50MB = true;
        this.noSetLowChanel = true;
        this.noSetHighChanel = true;
        this.mRO_StartRxBytes = 0;
        this.mRO_StartTxBytes = 0;
        this.startMonitorDataFlow = true;
    }

    public void requestDataMonitor(boolean enable, int id) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable = ");
        stringBuilder.append(enable);
        stringBuilder.append(", id = ");
        stringBuilder.append(id);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (!(!enable || this.mplinkEnableState || this.smartmpEnableState)) {
            this.mHBDHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_Display_Start_Monitor_Network);
        }
        if (1 == id) {
            this.mplinkEnableState = enable;
        }
        if (2 == id) {
            this.smartmpEnableState = enable;
        }
        if (!enable && !this.mplinkEnableState && !this.smartmpEnableState) {
            this.mHBDHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_Display_stop_Monotor_network);
        }
    }

    private void periodCheckHighDataFlow() {
        if (this.startMonitorDataFlow) {
            HwArbitrationCommonUtils.logD(TAG, "start Hidata_Notification monitor");
            this.mRO_StartRxBytes = TrafficStats.getMobileRxBytes();
            this.mRO_StartTxBytes = TrafficStats.getMobileTxBytes();
            if (this.mRO_StartTxBytes < 0 || this.mRO_StartRxBytes < 0) {
                this.mRO_StartRxBytes = 0;
                this.mRO_StartTxBytes = 0;
                HwArbitrationCommonUtils.logD(TAG, "read rx tx error");
            }
            this.startMonitorDataFlow = false;
            return;
        }
        String str;
        StringBuilder stringBuilder;
        long rxBytes = TrafficStats.getMobileRxBytes();
        long txBytes = TrafficStats.getMobileTxBytes();
        if (rxBytes < 0 || txBytes < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read rx tx error, rx=");
            stringBuilder.append(rxBytes);
            stringBuilder.append(", tx=");
            stringBuilder.append(txBytes);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("rxBytes: ");
        stringBuilder.append(rxBytes);
        stringBuilder.append(", txBytes: ");
        stringBuilder.append(txBytes);
        stringBuilder.append(", mRO_StartRxBytes: ");
        stringBuilder.append(this.mRO_StartRxBytes);
        stringBuilder.append(", mRO_StartTxBytes: ");
        stringBuilder.append(this.mRO_StartTxBytes);
        stringBuilder.append(", CostBytes: ");
        stringBuilder.append((((rxBytes - this.mRO_StartRxBytes) + txBytes) - this.mRO_StartTxBytes) / MemoryConstant.MB_SIZE);
        stringBuilder.append("MB");
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        if (rxBytes >= this.mRO_StartRxBytes && txBytes >= this.mRO_StartTxBytes) {
            long totalCostBytes = (rxBytes - this.mRO_StartRxBytes) + (txBytes - this.mRO_StartTxBytes);
            if (totalCostBytes > Constant.MAX_FILE_SIZE && this.noNotify50MB) {
                this.noNotify25MB = false;
                this.noNotify50MB = false;
                HwArbitrationCommonUtils.logD(TAG, "show Hidata_Notification: 50MB");
                showHiDataHighNotification(((int) totalCostBytes) / HighBitsCompModeID.MODE_COLOR_ENHANCE);
            } else if (totalCostBytes > 26214400 && this.noNotify25MB) {
                this.noNotify25MB = false;
                HwArbitrationCommonUtils.logD(TAG, "show Hidata_Notification: 25MB");
                showHiDataHighNotification(((int) totalCostBytes) / HighBitsCompModeID.MODE_COLOR_ENHANCE);
            } else if (totalCostBytes > Constant.MAX_FILE_SIZE) {
                int notifyInt = ((int) totalCostBytes) / HighBitsCompModeID.MODE_COLOR_ENHANCE;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("show Hidata_Notification: ");
                stringBuilder2.append(notifyInt);
                stringBuilder2.append("MB");
                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                getInstance(this.mContext).showHiDataLowNotification(notifyInt);
            }
        }
    }

    private Notification getNotification(int mobileDateSize, boolean isHigh) {
        Builder b;
        int i = mobileDateSize;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showHiDataNotification: ");
        stringBuilder.append(i);
        stringBuilder.append("MB");
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        str = this.mContext.getResources().getString(33686080);
        String content = this.mContext.getResources().getString(33686079);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("");
        stringBuilder2.append(i);
        String title = str.replace("%d", stringBuilder2.toString());
        String suitch = this.mContext.getResources().getString(33686078);
        PendingIntent roveInPendingIntent = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(HwArbitrationDEFS.ACTION_HiData_DATA_ROVE_IN).setPackage(this.mContext.getPackageName()), 268435456, UserHandle.ALL);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this.mContext, 1, new Intent(ACTION_WIFI_PLUS_ACTIVITY), 268435456);
        Action action = new Action.Builder(33751955, suitch, roveInPendingIntent).build();
        if (isHigh) {
            b = new Builder(this.mContext, NotificationHighChannelTAG);
        } else {
            b = new Builder(this.mContext, NotificationLowChannelTAG);
        }
        b.setContentIntent(activityPendingIntent);
        b.setAutoCancel(true);
        b.setUsesChronometer(true);
        b.setContentTitle(title);
        b.setContentText(content);
        b.setVisibility(1);
        b.setTicker("");
        b.setShowWhen(true);
        b.setUsesChronometer(false);
        b.setSmallIcon(33751955);
        b.addAction(action);
        b.setOngoing(true);
        return b.build();
    }

    private void showHiDataHighNotification(int mobileDateSize) {
        if (this.mNotificationManager == null) {
            HwArbitrationCommonUtils.logE(TAG, "High NotificationManager is null!");
            return;
        }
        if (this.noSetHighChanel) {
            this.noSetHighChanel = false;
            this.mNotificationManager.deleteNotificationChannel(NotificationLowChannelTAG);
            NotificationChannel highNotificationChannel = new NotificationChannel(NotificationHighChannelTAG, this.mContext.getResources().getString(33686075), 4);
            highNotificationChannel.setSound(System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            highNotificationChannel.enableLights(true);
            highNotificationChannel.enableVibration(true);
            this.mNotificationManager.createNotificationChannel(highNotificationChannel);
        }
        this.mNotificationManager.notify(HiData_High_NOTIFICATION_ID, getNotification(mobileDateSize, true));
    }

    private void cancelHiDataHighNotification() {
        if (this.mNotificationManager == null) {
            HwArbitrationCommonUtils.logE(TAG, "mNotificationManager is null!");
            return;
        }
        HwArbitrationCommonUtils.logD(TAG, "cancel the High notification!");
        this.mNotificationManager.cancel(HiData_High_NOTIFICATION_ID);
    }

    private void showHiDataLowNotification(int mobileDateSize) {
        if (this.mNotificationManager == null) {
            HwArbitrationCommonUtils.logE(TAG, "High NotificationManager is null!");
            return;
        }
        if (this.noSetLowChanel) {
            this.noSetLowChanel = false;
            cancelHiDataHighNotification();
            this.mNotificationManager.deleteNotificationChannel(NotificationHighChannelTAG);
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(NotificationLowChannelTAG, this.mContext.getResources().getString(33686075), 2));
        }
        this.mNotificationManager.notify(HiData_Low_NOTIFICATION_ID, getNotification(mobileDateSize, false));
    }

    private void cancelHiDataLowNotification() {
        if (this.mNotificationManager == null) {
            HwArbitrationCommonUtils.logE(TAG, "mNotificationManager is null!");
            return;
        }
        HwArbitrationCommonUtils.logD(TAG, "cancel the Low notification!");
        this.mNotificationManager.cancel(HiData_Low_NOTIFICATION_ID);
    }

    private void initHBDHandler() {
        HandlerThread handlerThread = new HandlerThread("hidata_brain_display_handler");
        handlerThread.start();
        this.mHBDHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case HwArbitrationDEFS.MSG_Display_Start_Monitor_Network /*3002*/:
                        HwArbitrationCommonUtils.logD(HwArbitrationDisplay.TAG, "MSG_Display_Start_Monitor_Network");
                        HwArbitrationDisplay.this.periodCheckHighDataFlow();
                        sendEmptyMessageDelayed(HwArbitrationDEFS.MSG_Display_Start_Monitor_Network, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                        return;
                    case HwArbitrationDEFS.MSG_Display_stop_Monotor_network /*3003*/:
                        HwArbitrationCommonUtils.logD(HwArbitrationDisplay.TAG, "MSG_Display_stop_Monotor_network");
                        removeMessages(HwArbitrationDEFS.MSG_Display_Start_Monitor_Network);
                        HwArbitrationDisplay.this.cancelHiDataHighNotification();
                        HwArbitrationDisplay.this.cancelHiDataLowNotification();
                        HwArbitrationDisplay.this.resetState();
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public static void setToast(Context mContext, String info) {
        HwArbitrationCommonUtils.logD(TAG, "show HiData_Toast");
        Toast.makeText(mContext, info, 1).show();
    }
}

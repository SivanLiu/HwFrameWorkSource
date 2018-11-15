package com.android.internal.telephony.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes.Builder;
import android.net.Uri;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import java.util.Arrays;

public class NotificationChannelController {
    public static final String CHANNEL_ID_ALERT = "alert";
    public static final String CHANNEL_ID_CALL_FORWARD = "callForward";
    private static final String CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED = "mobileDataAlert";
    public static final String CHANNEL_ID_MOBILE_DATA_STATUS = "mobileDataAlertNew";
    public static final String CHANNEL_ID_SMS = "sms";
    public static final String CHANNEL_ID_VOICE_MAIL = "voiceMail";
    public static final String CHANNEL_ID_WFC = "wfc";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                NotificationChannelController.createAll(context);
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && -1 != SubscriptionManager.getDefaultSubscriptionId()) {
                NotificationChannelController.migrateVoicemailNotificationSettings(context);
            }
        }
    };

    private static void createAll(Context context) {
        new NotificationChannel(CHANNEL_ID_ALERT, context.getText(17040500), 3).setSound(System.DEFAULT_NOTIFICATION_URI, new Builder().setUsage(5).build());
        new NotificationChannel(CHANNEL_ID_MOBILE_DATA_STATUS, context.getText(17040499), 2).setBlockableSystem(true);
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannels(Arrays.asList(new NotificationChannel[]{new NotificationChannel(CHANNEL_ID_CALL_FORWARD, context.getText(17040492), 2), new NotificationChannel(CHANNEL_ID_SMS, context.getText(17040507), 4), new NotificationChannel(CHANNEL_ID_WFC, context.getText(17040513), 2), alertChannel, mobileDataStatusChannel}));
        if (getChannel(CHANNEL_ID_VOICE_MAIL, context) != null) {
            migrateVoicemailNotificationSettings(context);
        }
        if (getChannel(CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED, context) != null) {
            ((NotificationManager) context.getSystemService(NotificationManager.class)).deleteNotificationChannel(CHANNEL_ID_MOBILE_DATA_ALERT_DEPRECATED);
        }
    }

    public NotificationChannelController(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        createAll(context);
    }

    public static NotificationChannel getChannel(String channelId, Context context) {
        return ((NotificationManager) context.getSystemService(NotificationManager.class)).getNotificationChannel(channelId);
    }

    private static void migrateVoicemailNotificationSettings(Context context) {
        NotificationChannel voiceMailChannel = new NotificationChannel(CHANNEL_ID_VOICE_MAIL, context.getText(17040511), 3);
        voiceMailChannel.enableVibration(VoicemailNotificationSettingsUtil.getVibrationPreference(context));
        Uri sound = VoicemailNotificationSettingsUtil.getRingTonePreference(context);
        if (sound == null) {
            sound = System.DEFAULT_NOTIFICATION_URI;
        }
        voiceMailChannel.setSound(sound, new Builder().setUsage(5).build());
        ((NotificationManager) context.getSystemService(NotificationManager.class)).createNotificationChannel(voiceMailChannel);
    }
}

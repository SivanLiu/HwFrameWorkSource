package com.android.internal.telephony;

import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings.Global;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import com.android.internal.telephony.util.NotificationChannelController;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CarrierServiceStateTracker extends Handler {
    protected static final int CARRIER_EVENT_BASE = 100;
    protected static final int CARRIER_EVENT_DATA_DEREGISTRATION = 104;
    protected static final int CARRIER_EVENT_DATA_REGISTRATION = 103;
    protected static final int CARRIER_EVENT_VOICE_DEREGISTRATION = 102;
    protected static final int CARRIER_EVENT_VOICE_REGISTRATION = 101;
    private static final String LOG_TAG = "CSST";
    public static final int NOTIFICATION_EMERGENCY_NETWORK = 1001;
    public static final int NOTIFICATION_PREF_NETWORK = 1000;
    private static final int UNINITIALIZED_DELAY_VALUE = -1;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            PersistableBundle b = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfigForSubId(CarrierServiceStateTracker.this.mPhone.getSubId());
            for (Entry<Integer, NotificationType> entry : CarrierServiceStateTracker.this.mNotificationTypeMap.entrySet()) {
                ((NotificationType) entry.getValue()).setDelay(b);
            }
            CarrierServiceStateTracker.this.handleConfigChanges();
        }
    };
    private final Map<Integer, NotificationType> mNotificationTypeMap = new HashMap();
    private Phone mPhone;
    private ServiceStateTracker mSST;

    public interface NotificationType {
        int getDelay();

        Builder getNotificationBuilder();

        int getTypeId();

        boolean sendMessage();

        void setDelay(PersistableBundle persistableBundle);
    }

    public class EmergencyNetworkNotification implements NotificationType {
        private int mDelay = -1;
        private final int mTypeId;

        EmergencyNetworkNotification(int typeId) {
            this.mTypeId = typeId;
        }

        public void setDelay(PersistableBundle bundle) {
            if (bundle == null) {
                Rlog.e(CarrierServiceStateTracker.LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = bundle.getInt("emergency_notification_delay_int");
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "reading time to delay notification emergency: " + this.mDelay);
        }

        public int getDelay() {
            return this.mDelay;
        }

        public int getTypeId() {
            return this.mTypeId;
        }

        public boolean sendMessage() {
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "EmergencyNetworkNotification: sendMessage() w/values: ," + CarrierServiceStateTracker.this.isPhoneVoiceRegistered() + "," + this.mDelay + "," + CarrierServiceStateTracker.this.isPhoneRegisteredForWifiCalling() + "," + CarrierServiceStateTracker.this.mSST.isRadioOn());
            if (this.mDelay == -1 || CarrierServiceStateTracker.this.isPhoneVoiceRegistered() || (CarrierServiceStateTracker.this.isPhoneRegisteredForWifiCalling() ^ 1) != 0) {
                return false;
            }
            return true;
        }

        public Builder getNotificationBuilder() {
            Context context = CarrierServiceStateTracker.this.mPhone.getContext();
            CharSequence title = context.getText(17039408);
            CharSequence details = context.getText(17039407);
            return new Builder(context).setContentTitle(title).setStyle(new BigTextStyle().bigText(details)).setContentText(details).setChannel(NotificationChannelController.CHANNEL_ID_WFC);
        }
    }

    public class PrefNetworkNotification implements NotificationType {
        private int mDelay = -1;
        private final int mTypeId;

        PrefNetworkNotification(int typeId) {
            this.mTypeId = typeId;
        }

        public void setDelay(PersistableBundle bundle) {
            if (bundle == null) {
                Rlog.e(CarrierServiceStateTracker.LOG_TAG, "bundle is null");
                return;
            }
            this.mDelay = bundle.getInt("network_notification_delay_int");
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "reading time to delay notification emergency: " + this.mDelay);
        }

        public int getDelay() {
            return this.mDelay;
        }

        public int getTypeId() {
            return this.mTypeId;
        }

        public boolean sendMessage() {
            Rlog.i(CarrierServiceStateTracker.LOG_TAG, "PrefNetworkNotification: sendMessage() w/values: ," + CarrierServiceStateTracker.this.isPhoneStillRegistered() + "," + this.mDelay + "," + CarrierServiceStateTracker.this.isGlobalMode() + "," + CarrierServiceStateTracker.this.mSST.isRadioOn());
            if (this.mDelay == -1 || CarrierServiceStateTracker.this.isPhoneStillRegistered() || CarrierServiceStateTracker.this.isGlobalMode() || CarrierServiceStateTracker.this.isRadioOffOrAirplaneMode()) {
                return false;
            }
            return true;
        }

        public Builder getNotificationBuilder() {
            Context context = CarrierServiceStateTracker.this.mPhone.getContext();
            PendingIntent settingsIntent = PendingIntent.getActivity(context, 0, new Intent("android.settings.DATA_ROAMING_SETTINGS"), 1073741824);
            CharSequence title = context.getText(17039455);
            CharSequence details = context.getText(17039454);
            return new Builder(context).setContentTitle(title).setStyle(new BigTextStyle().bigText(details)).setContentText(details).setChannel(NotificationChannelController.CHANNEL_ID_ALERT).setContentIntent(settingsIntent);
        }
    }

    public CarrierServiceStateTracker(Phone phone, ServiceStateTracker sst) {
        this.mPhone = phone;
        this.mSST = sst;
        phone.getContext().registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        registerNotificationTypes();
    }

    private void registerNotificationTypes() {
        this.mNotificationTypeMap.put(Integer.valueOf(1000), new PrefNetworkNotification(1000));
        this.mNotificationTypeMap.put(Integer.valueOf(1001), new EmergencyNetworkNotification(1001));
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 101:
            case 102:
            case CARRIER_EVENT_DATA_REGISTRATION /*103*/:
            case 104:
                handleConfigChanges();
                return;
            case 1000:
            case 1001:
                Rlog.d(LOG_TAG, "sending notification after delay: " + msg.what);
                NotificationType notificationType = (NotificationType) this.mNotificationTypeMap.get(Integer.valueOf(msg.what));
                if (notificationType != null) {
                    sendNotification(notificationType);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private boolean isPhoneStillRegistered() {
        boolean z = true;
        if (this.mSST.mSS == null) {
            return true;
        }
        if (!(this.mSST.mSS.getVoiceRegState() == 0 || this.mSST.mSS.getDataRegState() == 0)) {
            z = false;
        }
        return z;
    }

    private boolean isPhoneVoiceRegistered() {
        boolean z = true;
        if (this.mSST.mSS == null) {
            return true;
        }
        if (this.mSST.mSS.getVoiceRegState() != 0) {
            z = false;
        }
        return z;
    }

    private boolean isPhoneRegisteredForWifiCalling() {
        Rlog.d(LOG_TAG, "isPhoneRegisteredForWifiCalling: " + this.mPhone.isWifiCallingEnabled());
        return this.mPhone.isWifiCallingEnabled();
    }

    public boolean isRadioOffOrAirplaneMode() {
        boolean z = true;
        try {
            int airplaneMode = Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0);
            if (this.mSST.isRadioOn() && airplaneMode == 0) {
                z = false;
            }
            return z;
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get AIRPLACE_MODE_ON.");
            return true;
        }
    }

    private boolean isGlobalMode() {
        boolean z = true;
        try {
            if (Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode" + this.mPhone.getSubId(), Phone.PREFERRED_NT_MODE) != 10) {
                z = false;
            }
            return z;
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Unable to get PREFERRED_NETWORK_MODE.");
            return true;
        }
    }

    private void handleConfigChanges() {
        for (Entry<Integer, NotificationType> entry : this.mNotificationTypeMap.entrySet()) {
            NotificationType notificationType = (NotificationType) entry.getValue();
            if (evaluateSendingMessage(notificationType)) {
                Message notificationMsg = obtainMessage(notificationType.getTypeId(), null);
                Rlog.i(LOG_TAG, "starting timer for notifications." + notificationType.getTypeId());
                sendMessageDelayed(notificationMsg, (long) getDelay(notificationType));
            } else {
                cancelNotification(notificationType.getTypeId());
                Rlog.i(LOG_TAG, "canceling notifications: " + notificationType.getTypeId());
            }
        }
    }

    public boolean evaluateSendingMessage(NotificationType notificationType) {
        return notificationType.sendMessage();
    }

    public int getDelay(NotificationType notificationType) {
        return notificationType.getDelay();
    }

    public Builder getNotificationBuilder(NotificationType notificationType) {
        return notificationType.getNotificationBuilder();
    }

    public NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService("notification");
    }

    public void sendNotification(NotificationType notificationType) {
        if (evaluateSendingMessage(notificationType)) {
            Context context = this.mPhone.getContext();
            Builder builder = getNotificationBuilder(notificationType);
            builder.setWhen(System.currentTimeMillis()).setAutoCancel(true).setSmallIcon(17301642).setColor(context.getResources().getColor(17170772));
            getNotificationManager(context).notify(notificationType.getTypeId(), builder.build());
        }
    }

    public void cancelNotification(int notificationId) {
        Context context = this.mPhone.getContext();
        removeMessages(notificationId);
        getNotificationManager(context).cancel(notificationId);
    }
}

package android.telephony.euicc;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.euicc.IEuiccController;
import com.android.internal.telephony.euicc.IEuiccController.Stub;

public class EuiccManager {
    public static final String ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS = "android.telephony.euicc.action.MANAGE_EMBEDDED_SUBSCRIPTIONS";
    public static final String ACTION_PROVISION_EMBEDDED_SUBSCRIPTION = "android.telephony.euicc.action.PROVISION_EMBEDDED_SUBSCRIPTION";
    public static final String ACTION_RESOLVE_ERROR = "android.telephony.euicc.action.RESOLVE_ERROR";
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_ERROR = 2;
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_OK = 0;
    public static final int EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR = 1;
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DETAILED_CODE";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTION";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_DOWNLOADABLE_SUBSCRIPTIONS";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_ACTION";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT";
    public static final String EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT = "android.telephony.euicc.extra.EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT";
    public static final String EXTRA_FORCE_PROVISION = "android.telephony.euicc.extra.FORCE_PROVISION";
    public static final String META_DATA_CARRIER_ICON = "android.telephony.euicc.carriericon";
    private final Context mContext;
    private final IEuiccController mController = Stub.asInterface(ServiceManager.getService("econtroller"));

    public EuiccManager(Context context) {
        this.mContext = context;
    }

    public boolean isEnabled() {
        return this.mController != null;
    }

    public String getEid() {
        if (!isEnabled()) {
            return null;
        }
        try {
            return this.mController.getEid();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void downloadSubscription(DownloadableSubscription subscription, boolean switchAfterDownload, PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.downloadSubscription(subscription, switchAfterDownload, this.mContext.getOpPackageName(), callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void startResolutionActivity(Activity activity, int requestCode, Intent resultIntent, PendingIntent callbackIntent) throws SendIntentException {
        PendingIntent resolutionIntent = (PendingIntent) resultIntent.getParcelableExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_INTENT);
        if (resolutionIntent == null) {
            throw new IllegalArgumentException("Invalid result intent");
        }
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT, callbackIntent);
        activity.startIntentSenderForResult(resolutionIntent.getIntentSender(), requestCode, fillInIntent, 0, 0, 0);
    }

    public void continueOperation(Intent resolutionIntent, Bundle resolutionExtras) {
        if (isEnabled()) {
            try {
                this.mController.continueOperation(resolutionIntent, resolutionExtras);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        PendingIntent callbackIntent = (PendingIntent) resolutionIntent.getParcelableExtra(EXTRA_EMBEDDED_SUBSCRIPTION_RESOLUTION_CALLBACK_INTENT);
        if (callbackIntent != null) {
            sendUnavailableError(callbackIntent);
        }
    }

    public void getDownloadableSubscriptionMetadata(DownloadableSubscription subscription, PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.getDownloadableSubscriptionMetadata(subscription, this.mContext.getOpPackageName(), callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void getDefaultDownloadableSubscriptionList(PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.getDefaultDownloadableSubscriptionList(this.mContext.getOpPackageName(), callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public EuiccInfo getEuiccInfo() {
        if (!isEnabled()) {
            return null;
        }
        try {
            return this.mController.getEuiccInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteSubscription(int subscriptionId, PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.deleteSubscription(subscriptionId, this.mContext.getOpPackageName(), callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void switchToSubscription(int subscriptionId, PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.switchToSubscription(subscriptionId, this.mContext.getOpPackageName(), callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void updateSubscriptionNickname(int subscriptionId, String nickname, PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.updateSubscriptionNickname(subscriptionId, nickname, callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void eraseSubscriptions(PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.eraseSubscriptions(callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    public void retainSubscriptionsForFactoryReset(PendingIntent callbackIntent) {
        if (isEnabled()) {
            try {
                this.mController.retainSubscriptionsForFactoryReset(callbackIntent);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        sendUnavailableError(callbackIntent);
    }

    private static void sendUnavailableError(PendingIntent callbackIntent) {
        try {
            callbackIntent.send(2);
        } catch (CanceledException e) {
        }
    }
}

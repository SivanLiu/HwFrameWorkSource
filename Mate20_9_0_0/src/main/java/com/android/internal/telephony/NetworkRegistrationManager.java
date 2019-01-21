package com.android.internal.telephony;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.INetworkService.Stub;
import android.telephony.INetworkServiceCallback;
import android.telephony.NetworkRegistrationState;
import android.telephony.Rlog;
import java.util.Hashtable;
import java.util.Map;

public class NetworkRegistrationManager {
    private static final String TAG = NetworkRegistrationManager.class.getSimpleName();
    private final Map<NetworkRegStateCallback, Message> mCallbackTable = new Hashtable();
    private final CarrierConfigManager mCarrierConfigManager;
    private RegManagerDeathRecipient mDeathRecipient;
    private final Phone mPhone;
    private final RegistrantList mRegStateChangeRegistrants = new RegistrantList();
    private Stub mServiceBinder;
    private final int mTransportType;

    private class NetworkRegStateCallback extends INetworkServiceCallback.Stub {
        private NetworkRegStateCallback() {
        }

        public void onGetNetworkRegistrationStateComplete(int result, NetworkRegistrationState state) {
            NetworkRegistrationManager networkRegistrationManager = NetworkRegistrationManager.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onGetNetworkRegistrationStateComplete result ");
            stringBuilder.append(result);
            stringBuilder.append(" state ");
            stringBuilder.append(state);
            networkRegistrationManager.logd(stringBuilder.toString());
            Message onCompleteMessage = (Message) NetworkRegistrationManager.this.mCallbackTable.remove(this);
            if (onCompleteMessage != null) {
                onCompleteMessage.arg1 = result;
                onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj, state, null);
                onCompleteMessage.sendToTarget();
                return;
            }
            NetworkRegistrationManager.this.loge("onCompleteMessage is null");
        }

        public void onNetworkStateChanged() {
            NetworkRegistrationManager.this.logd("onNetworkStateChanged");
            NetworkRegistrationManager.this.mRegStateChangeRegistrants.notifyRegistrants();
        }
    }

    private class NetworkServiceConnection implements ServiceConnection {
        private NetworkServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            NetworkRegistrationManager.this.logd("service connected.");
            NetworkRegistrationManager.this.mServiceBinder = (Stub) service;
            NetworkRegistrationManager.this.mDeathRecipient = new RegManagerDeathRecipient(name);
            try {
                NetworkRegistrationManager.this.mServiceBinder.linkToDeath(NetworkRegistrationManager.this.mDeathRecipient, 0);
                NetworkRegistrationManager.this.mServiceBinder.createNetworkServiceProvider(NetworkRegistrationManager.this.mPhone.getPhoneId());
                NetworkRegistrationManager.this.mServiceBinder.registerForNetworkRegistrationStateChanged(NetworkRegistrationManager.this.mPhone.getPhoneId(), new NetworkRegStateCallback());
            } catch (RemoteException exception) {
                NetworkRegistrationManager.this.mDeathRecipient.binderDied();
                NetworkRegistrationManager networkRegistrationManager = NetworkRegistrationManager.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException ");
                stringBuilder.append(exception);
                networkRegistrationManager.logd(stringBuilder.toString());
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            NetworkRegistrationManager networkRegistrationManager = NetworkRegistrationManager.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onServiceDisconnected ");
            stringBuilder.append(name);
            networkRegistrationManager.logd(stringBuilder.toString());
            if (NetworkRegistrationManager.this.mServiceBinder != null) {
                NetworkRegistrationManager.this.mServiceBinder.unlinkToDeath(NetworkRegistrationManager.this.mDeathRecipient, 0);
            }
        }
    }

    private class RegManagerDeathRecipient implements DeathRecipient {
        private final ComponentName mComponentName;

        RegManagerDeathRecipient(ComponentName name) {
            this.mComponentName = name;
        }

        public void binderDied() {
            NetworkRegistrationManager networkRegistrationManager = NetworkRegistrationManager.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NetworkService(");
            stringBuilder.append(this.mComponentName);
            stringBuilder.append(" transport type ");
            stringBuilder.append(NetworkRegistrationManager.this.mTransportType);
            stringBuilder.append(") died.");
            networkRegistrationManager.logd(stringBuilder.toString());
        }
    }

    public NetworkRegistrationManager(int transportType, Phone phone) {
        this.mTransportType = transportType;
        this.mPhone = phone;
        this.mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        bindService();
    }

    public boolean isServiceConnected() {
        return this.mServiceBinder != null && this.mServiceBinder.isBinderAlive();
    }

    public void unregisterForNetworkRegistrationStateChanged(Handler h) {
        this.mRegStateChangeRegistrants.remove(h);
    }

    public void registerForNetworkRegistrationStateChanged(Handler h, int what, Object obj) {
        logd("registerForNetworkRegistrationStateChanged");
        Registrant r = new Registrant(h, what, obj);
        this.mRegStateChangeRegistrants.addUnique(h, what, obj);
    }

    public void getNetworkRegistrationState(int domain, Message onCompleteMessage) {
        if (onCompleteMessage != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getNetworkRegistrationState domain ");
            stringBuilder.append(domain);
            logd(stringBuilder.toString());
            if (isServiceConnected()) {
                NetworkRegStateCallback callback = new NetworkRegStateCallback();
                try {
                    this.mCallbackTable.put(callback, onCompleteMessage);
                    this.mServiceBinder.getNetworkRegistrationState(this.mPhone.getPhoneId(), domain, callback);
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getNetworkRegistrationState RemoteException ");
                    stringBuilder2.append(e);
                    Rlog.e(str, stringBuilder2.toString());
                    this.mCallbackTable.remove(callback);
                    onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj, null, e);
                    onCompleteMessage.sendToTarget();
                }
                return;
            }
            logd("service not connected.");
            onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj, null, new IllegalStateException("Service not connected."));
            onCompleteMessage.sendToTarget();
        }
    }

    private boolean bindService() {
        Intent intent = new Intent("android.telephony.NetworkService");
        intent.setPackage(getPackageName());
        try {
            return this.mPhone.getContext().bindService(intent, new NetworkServiceConnection(), 1);
        } catch (SecurityException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bindService failed ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    private String getPackageName() {
        int resourceId;
        String carrierConfig;
        switch (this.mTransportType) {
            case 1:
                resourceId = 17039861;
                carrierConfig = "carrier_network_service_wwan_package_override_string";
                break;
            case 2:
                resourceId = 17039859;
                carrierConfig = "carrier_network_service_wlan_package_override_string";
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Transport type not WWAN or WLAN. type=");
                stringBuilder.append(this.mTransportType);
                throw new IllegalStateException(stringBuilder.toString());
        }
        String packageName = this.mPhone.getContext().getResources().getString(resourceId);
        PersistableBundle b = this.mCarrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (b != null) {
            packageName = b.getString(carrierConfig, packageName);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Binding to packageName ");
        stringBuilder2.append(packageName);
        stringBuilder2.append(" for transport type");
        stringBuilder2.append(this.mTransportType);
        logd(stringBuilder2.toString());
        return packageName;
    }

    private int logd(String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("] ");
        stringBuilder.append(msg);
        return Rlog.d(str, stringBuilder.toString());
    }

    private int loge(String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("] ");
        stringBuilder.append(msg);
        return Rlog.e(str, stringBuilder.toString());
    }
}

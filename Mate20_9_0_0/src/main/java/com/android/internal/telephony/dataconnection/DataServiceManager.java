package com.android.internal.telephony.dataconnection;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.IDataService;
import android.telephony.data.IDataServiceCallback.Stub;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataServiceManager {
    static final String DATA_CALL_RESPONSE = "data_call_response";
    private static final boolean DBG = false;
    private static final String TAG = DataServiceManager.class.getSimpleName();
    private final AppOpsManager mAppOps;
    private boolean mBound;
    private final CarrierConfigManager mCarrierConfigManager;
    private ComponentName mComponentName;
    private final RegistrantList mDataCallListChangedRegistrants = new RegistrantList();
    private DataServiceManagerDeathRecipient mDeathRecipient;
    private IDataService mIDataService;
    private final Map<IBinder, Message> mMessageMap = new ConcurrentHashMap();
    private final IPackageManager mPackageManager;
    private final Phone mPhone;
    private final RegistrantList mServiceBindingChangedRegistrants = new RegistrantList();
    private final int mTransportType;

    private final class CellularDataServiceCallback extends Stub {
        private CellularDataServiceCallback() {
        }

        public void onSetupDataCallComplete(int resultCode, DataCallResponse response) {
            Message msg = (Message) DataServiceManager.this.mMessageMap.remove(asBinder());
            if (msg != null) {
                msg.getData().putParcelable(DataServiceManager.DATA_CALL_RESPONSE, response);
                DataServiceManager.this.sendCompleteMessage(msg, resultCode);
                return;
            }
            DataServiceManager.this.loge("Unable to find the message for setup call response.");
        }

        public void onDeactivateDataCallComplete(int resultCode) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), resultCode);
        }

        public void onSetInitialAttachApnComplete(int resultCode) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), resultCode);
        }

        public void onSetDataProfileComplete(int resultCode) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), resultCode);
        }

        public void onGetDataCallListComplete(int resultCode, List<DataCallResponse> list) {
            DataServiceManager.this.sendCompleteMessage((Message) DataServiceManager.this.mMessageMap.remove(asBinder()), resultCode);
        }

        public void onDataCallListChanged(List<DataCallResponse> dataCallList) {
            DataServiceManager.this.mDataCallListChangedRegistrants.notifyRegistrants(new AsyncResult(null, dataCallList, null));
        }
    }

    private final class CellularDataServiceConnection implements ServiceConnection {
        private CellularDataServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            DataServiceManager.this.mComponentName = name;
            DataServiceManager.this.mIDataService = IDataService.Stub.asInterface(service);
            DataServiceManager.this.mDeathRecipient = new DataServiceManagerDeathRecipient();
            DataServiceManager.this.mBound = true;
            try {
                service.linkToDeath(DataServiceManager.this.mDeathRecipient, 0);
                DataServiceManager.this.mIDataService.createDataServiceProvider(DataServiceManager.this.mPhone.getPhoneId());
                DataServiceManager.this.mIDataService.registerForDataCallListChanged(DataServiceManager.this.mPhone.getPhoneId(), new CellularDataServiceCallback());
                DataServiceManager.this.mServiceBindingChangedRegistrants.notifyResult(Boolean.valueOf(true));
            } catch (RemoteException e) {
                DataServiceManager.this.mDeathRecipient.binderDied();
                DataServiceManager dataServiceManager = DataServiceManager.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Remote exception. ");
                stringBuilder.append(e);
                dataServiceManager.loge(stringBuilder.toString());
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            DataServiceManager.this.mIDataService.asBinder().unlinkToDeath(DataServiceManager.this.mDeathRecipient, 0);
            DataServiceManager.this.mIDataService = null;
            DataServiceManager.this.mBound = false;
            DataServiceManager.this.mServiceBindingChangedRegistrants.notifyResult(Boolean.valueOf(false));
        }
    }

    private class DataServiceManagerDeathRecipient implements DeathRecipient {
        private DataServiceManagerDeathRecipient() {
        }

        public void binderDied() {
            DataServiceManager dataServiceManager = DataServiceManager.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DataService(");
            stringBuilder.append(DataServiceManager.this.mComponentName);
            stringBuilder.append(" transport type ");
            stringBuilder.append(DataServiceManager.this.mTransportType);
            stringBuilder.append(") died.");
            dataServiceManager.loge(stringBuilder.toString());
        }
    }

    private void grantPermissionsToService(String packageName) {
        String[] pkgToGrant = new String[]{packageName};
        try {
            this.mPackageManager.grantDefaultPermissionsToEnabledTelephonyDataServices(pkgToGrant, this.mPhone.getContext().getUserId());
            this.mAppOps.setMode(75, this.mPhone.getContext().getUserId(), pkgToGrant[0], 0);
        } catch (RemoteException e) {
            loge("Binder to package manager died, permission grant for DataService failed.");
            throw e.rethrowAsRuntimeException();
        }
    }

    private void revokePermissionsFromUnusedDataServices() {
        Set<String> dataServices = getAllDataServicePackageNames();
        for (int transportType : new int[]{1, 2}) {
            dataServices.remove(getDataServicePackageName(transportType));
        }
        try {
            String[] dataServicesArray = new String[dataServices.size()];
            dataServices.toArray(dataServicesArray);
            this.mPackageManager.revokeDefaultPermissionsFromDisabledTelephonyDataServices(dataServicesArray, this.mPhone.getContext().getUserId());
            for (String pkg : dataServices) {
                this.mAppOps.setMode(75, this.mPhone.getContext().getUserId(), pkg, 2);
            }
        } catch (RemoteException e) {
            loge("Binder to package manager died; failed to revoke DataService permissions.");
            throw e.rethrowAsRuntimeException();
        }
    }

    public DataServiceManager(Phone phone, int transportType) {
        this.mPhone = phone;
        this.mTransportType = transportType;
        this.mBound = false;
        this.mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mAppOps = (AppOpsManager) phone.getContext().getSystemService("appops");
        bindDataService();
    }

    private void bindDataService() {
        revokePermissionsFromUnusedDataServices();
        String packageName = getDataServicePackageName();
        if (TextUtils.isEmpty(packageName)) {
            loge("Can't find the binding package");
            return;
        }
        grantPermissionsToService(packageName);
        try {
            if (!this.mPhone.getContext().bindService(new Intent("android.telephony.data.DataService").setPackage(packageName), new CellularDataServiceConnection(), 1)) {
                loge("Cannot bind to the data service.");
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot bind to the data service. Exception: ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
        }
    }

    private Set<String> getAllDataServicePackageNames() {
        List<ResolveInfo> dataPackages = this.mPhone.getContext().getPackageManager().queryIntentServices(new Intent("android.telephony.data.DataService"), 1048576);
        HashSet<String> packageNames = new HashSet();
        for (ResolveInfo info : dataPackages) {
            if (info.serviceInfo != null) {
                packageNames.add(info.serviceInfo.packageName);
            }
        }
        return packageNames;
    }

    private String getDataServicePackageName() {
        return getDataServicePackageName(this.mTransportType);
    }

    private String getDataServicePackageName(int transportType) {
        int resourceId;
        String carrierConfig;
        switch (transportType) {
            case 1:
                resourceId = 17039860;
                carrierConfig = "carrier_data_service_wwan_package_override_string";
                break;
            case 2:
                resourceId = 17039858;
                carrierConfig = "carrier_data_service_wlan_package_override_string";
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
            return b.getString(carrierConfig, packageName);
        }
        return packageName;
    }

    private void sendCompleteMessage(Message msg, int code) {
        if (msg != null) {
            msg.arg1 = code;
            msg.sendToTarget();
        }
    }

    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming, boolean allowRoaming, int reason, LinkProperties linkProperties, Message onCompleteMessage) {
        Message message = onCompleteMessage;
        if (this.mBound) {
            CellularDataServiceCallback callback = null;
            if (message != null) {
                callback = new CellularDataServiceCallback();
                this.mMessageMap.put(callback.asBinder(), message);
            }
            CellularDataServiceCallback callback2 = callback;
            try {
                this.mIDataService.setupDataCall(this.mPhone.getPhoneId(), accessNetworkType, dataProfile, isRoaming, allowRoaming, reason, linkProperties, callback2);
            } catch (RemoteException e) {
                loge("Cannot invoke setupDataCall on data service.");
                if (callback2 != null) {
                    this.mMessageMap.remove(callback2.asBinder());
                }
                sendCompleteMessage(message, 4);
            }
            return;
        }
        loge("Data service not bound.");
        sendCompleteMessage(message, 4);
    }

    public void deactivateDataCall(int cid, int reason, Message onCompleteMessage) {
        if (this.mBound) {
            CellularDataServiceCallback callback = null;
            if (onCompleteMessage != null) {
                callback = new CellularDataServiceCallback();
                this.mMessageMap.put(callback.asBinder(), onCompleteMessage);
            }
            try {
                this.mIDataService.deactivateDataCall(this.mPhone.getPhoneId(), cid, reason, callback);
            } catch (RemoteException e) {
                loge("Cannot invoke deactivateDataCall on data service.");
                if (callback != null) {
                    this.mMessageMap.remove(callback.asBinder());
                }
                sendCompleteMessage(onCompleteMessage, 4);
            }
            return;
        }
        loge("Data service not bound.");
        sendCompleteMessage(onCompleteMessage, 4);
    }

    public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming, Message onCompleteMessage) {
        if (this.mBound) {
            CellularDataServiceCallback callback = null;
            if (onCompleteMessage != null) {
                callback = new CellularDataServiceCallback();
                this.mMessageMap.put(callback.asBinder(), onCompleteMessage);
            }
            try {
                this.mIDataService.setInitialAttachApn(this.mPhone.getPhoneId(), dataProfile, isRoaming, callback);
            } catch (RemoteException e) {
                loge("Cannot invoke setInitialAttachApn on data service.");
                if (callback != null) {
                    this.mMessageMap.remove(callback.asBinder());
                }
                sendCompleteMessage(onCompleteMessage, 4);
            }
            return;
        }
        loge("Data service not bound.");
        sendCompleteMessage(onCompleteMessage, 4);
    }

    public void setDataProfile(List<DataProfile> dps, boolean isRoaming, Message onCompleteMessage) {
        if (this.mBound) {
            CellularDataServiceCallback callback = null;
            if (onCompleteMessage != null) {
                callback = new CellularDataServiceCallback();
                this.mMessageMap.put(callback.asBinder(), onCompleteMessage);
            }
            try {
                this.mIDataService.setDataProfile(this.mPhone.getPhoneId(), dps, isRoaming, callback);
            } catch (RemoteException e) {
                loge("Cannot invoke setDataProfile on data service.");
                if (callback != null) {
                    this.mMessageMap.remove(callback.asBinder());
                }
                sendCompleteMessage(onCompleteMessage, 4);
            }
            return;
        }
        loge("Data service not bound.");
        sendCompleteMessage(onCompleteMessage, 4);
    }

    public void getDataCallList(Message onCompleteMessage) {
        if (this.mBound) {
            CellularDataServiceCallback callback = null;
            if (onCompleteMessage != null) {
                callback = new CellularDataServiceCallback();
                this.mMessageMap.put(callback.asBinder(), onCompleteMessage);
            }
            try {
                this.mIDataService.getDataCallList(this.mPhone.getPhoneId(), callback);
            } catch (RemoteException e) {
                loge("Cannot invoke getDataCallList on data service.");
                if (callback != null) {
                    this.mMessageMap.remove(callback.asBinder());
                }
                sendCompleteMessage(onCompleteMessage, 4);
            }
            return;
        }
        loge("Data service not bound.");
        sendCompleteMessage(onCompleteMessage, 4);
    }

    public void registerForDataCallListChanged(Handler h, int what) {
        if (h != null) {
            this.mDataCallListChangedRegistrants.addUnique(h, what, null);
        }
    }

    public void unregisterForDataCallListChanged(Handler h) {
        if (h != null) {
            this.mDataCallListChangedRegistrants.remove(h);
        }
    }

    public void registerForServiceBindingChanged(Handler h, int what, Object obj) {
        if (h != null) {
            this.mServiceBindingChangedRegistrants.addUnique(h, what, obj);
        }
    }

    public void unregisterForServiceBindingChanged(Handler h) {
        if (h != null) {
            this.mServiceBindingChangedRegistrants.remove(h);
        }
    }

    public int getTransportType() {
        return this.mTransportType;
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}

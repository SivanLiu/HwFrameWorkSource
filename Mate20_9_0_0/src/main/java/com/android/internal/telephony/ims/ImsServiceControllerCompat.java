package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsServiceController;
import com.android.ims.internal.IImsServiceController.Stub;
import com.android.internal.telephony.ims.ImsServiceController.ImsServiceControllerCallbacks;

public class ImsServiceControllerCompat extends ImsServiceController {
    private static final String TAG = "ImsSCCompat";
    private final SparseArray<ImsConfigCompatAdapter> mConfigCompatAdapters = new SparseArray();
    private final SparseArray<MmTelFeatureCompatAdapter> mMmTelCompatAdapters = new SparseArray();
    private final SparseArray<ImsRegistrationCompatAdapter> mRegCompatAdapters = new SparseArray();
    private IImsServiceController mServiceController;

    public ImsServiceControllerCompat(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
        super(context, componentName, callbacks);
    }

    protected String getServiceInterface() {
        return "android.telephony.ims.compat.ImsService";
    }

    public void enableIms(int slotId) {
        MmTelFeatureCompatAdapter adapter = (MmTelFeatureCompatAdapter) this.mMmTelCompatAdapters.get(slotId);
        if (adapter == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableIms: adapter null for slot :");
            stringBuilder.append(slotId);
            Log.w(str, stringBuilder.toString());
            return;
        }
        try {
            adapter.enableIms();
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Couldn't enable IMS: ");
            stringBuilder2.append(e.getMessage());
            Log.w(str2, stringBuilder2.toString());
        }
    }

    public void disableIms(int slotId) {
        MmTelFeatureCompatAdapter adapter = (MmTelFeatureCompatAdapter) this.mMmTelCompatAdapters.get(slotId);
        if (adapter == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableIms: adapter null for slot :");
            stringBuilder.append(slotId);
            Log.w(str, stringBuilder.toString());
            return;
        }
        try {
            adapter.disableIms();
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Couldn't enable IMS: ");
            stringBuilder2.append(e.getMessage());
            Log.w(str2, stringBuilder2.toString());
        }
    }

    public IImsRegistration getRegistration(int slotId) throws RemoteException {
        ImsRegistrationCompatAdapter adapter = (ImsRegistrationCompatAdapter) this.mRegCompatAdapters.get(slotId);
        if (adapter != null) {
            return adapter.getBinder();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRegistration: Registration does not exist for slot ");
        stringBuilder.append(slotId);
        Log.w(str, stringBuilder.toString());
        return null;
    }

    public IImsConfig getConfig(int slotId) throws RemoteException {
        ImsConfigCompatAdapter adapter = (ImsConfigCompatAdapter) this.mConfigCompatAdapters.get(slotId);
        if (adapter != null) {
            return adapter.getIImsConfig();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getConfig: Config does not exist for slot ");
        stringBuilder.append(slotId);
        Log.w(str, stringBuilder.toString());
        return null;
    }

    protected void notifyImsServiceReady() throws RemoteException {
        Log.d(TAG, "notifyImsServiceReady");
    }

    protected IInterface createImsFeature(int slotId, int featureType, IImsFeatureStatusCallback c) throws RemoteException {
        switch (featureType) {
            case 1:
                return createMMTelCompat(slotId, c);
            case 2:
                return createRcsFeature(slotId, c);
            default:
                return null;
        }
    }

    protected void removeImsFeature(int slotId, int featureType, IImsFeatureStatusCallback c) throws RemoteException {
        if (featureType == 1) {
            this.mMmTelCompatAdapters.remove(slotId);
            this.mRegCompatAdapters.remove(slotId);
            this.mConfigCompatAdapters.remove(slotId);
        }
        this.mServiceController.removeImsFeature(slotId, featureType, c);
    }

    protected void setServiceController(IBinder serviceController) {
        this.mServiceController = Stub.asInterface(serviceController);
    }

    protected boolean isServiceControllerAvailable() {
        return this.mServiceController != null;
    }

    protected MmTelInterfaceAdapter getInterface(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
        IImsMMTelFeature feature = this.mServiceController.createMMTelFeature(slotId, c);
        if (feature != null) {
            return new MmTelInterfaceAdapter(slotId, feature.asBinder());
        }
        Log.w(TAG, "createMMTelCompat: createMMTelFeature returned null.");
        return null;
    }

    private IImsMmTelFeature createMMTelCompat(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
        MmTelFeatureCompatAdapter mmTelAdapter = new MmTelFeatureCompatAdapter(this.mContext, slotId, getInterface(slotId, c));
        this.mMmTelCompatAdapters.put(slotId, mmTelAdapter);
        ImsRegistrationCompatAdapter regAdapter = new ImsRegistrationCompatAdapter();
        mmTelAdapter.addRegistrationAdapter(regAdapter);
        this.mRegCompatAdapters.put(slotId, regAdapter);
        this.mConfigCompatAdapters.put(slotId, new ImsConfigCompatAdapter(mmTelAdapter.getOldConfigInterface()));
        return mmTelAdapter.getBinder();
    }

    private IImsRcsFeature createRcsFeature(int slotId, IImsFeatureStatusCallback c) {
        return null;
    }
}

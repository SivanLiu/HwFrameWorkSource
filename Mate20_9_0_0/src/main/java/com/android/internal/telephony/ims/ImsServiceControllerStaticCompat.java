package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsService;
import com.android.ims.internal.IImsService.Stub;
import com.android.internal.telephony.ims.ImsServiceController.ImsServiceControllerCallbacks;

public class ImsServiceControllerStaticCompat extends ImsServiceControllerCompat {
    private static final String IMS_SERVICE_NAME = "ims";
    private static final String TAG = "ImsSCStaticCompat";
    private IImsService mImsServiceCompat = null;

    public ImsServiceControllerStaticCompat(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
        super(context, componentName, callbacks);
    }

    public boolean startBindToService(Intent intent, ImsServiceConnection connection, int flags) {
        IBinder binder = ServiceManager.checkService(IMS_SERVICE_NAME);
        if (binder == null) {
            return false;
        }
        connection.onServiceConnected(new ComponentName(this.mContext, ImsServiceControllerStaticCompat.class), binder);
        return true;
    }

    protected void setServiceController(IBinder serviceController) {
        this.mImsServiceCompat = Stub.asInterface(serviceController);
    }

    protected MmTelInterfaceAdapter getInterface(int slotId, IImsFeatureStatusCallback c) throws RemoteException {
        if (this.mImsServiceCompat != null) {
            return new ImsServiceInterfaceAdapter(slotId, this.mImsServiceCompat.asBinder());
        }
        Log.w(TAG, "getInterface: IImsService returned null.");
        return null;
    }
}

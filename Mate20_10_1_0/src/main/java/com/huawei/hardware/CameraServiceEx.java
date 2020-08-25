package com.huawei.hardware;

import android.hardware.ICameraService;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import com.huawei.annotation.HwSystemApi;

@HwSystemApi
public class CameraServiceEx {
    private static final String TAG = "CameraServiceAdapter";
    private ICameraService cameraService = null;

    public CameraServiceEx(IBinder cameraServiceBinder) {
        try {
            this.cameraService = ICameraService.Stub.asInterface(cameraServiceBinder);
        } catch (ServiceSpecificException e) {
            this.cameraService = null;
            Log.e(TAG, "ServiceSpecificException while set cameraService.");
        } catch (Exception e2) {
            this.cameraService = null;
            Log.e(TAG, "Exception while set cameraService.");
        }
    }

    public void setCommand(String commandType, String commandValue) {
        ICameraService iCameraService = this.cameraService;
        if (iCameraService == null) {
            Log.e(TAG, "cameraService is null.");
            return;
        }
        try {
            iCameraService.setCommand(commandType, commandValue);
        } catch (ServiceSpecificException e) {
            this.cameraService = null;
            Log.e(TAG, "ServiceSpecificException while setCommand.");
        } catch (RemoteException e2) {
            this.cameraService = null;
            Log.e(TAG, "RemoteException while setCommand.");
        } catch (Exception e3) {
            this.cameraService = null;
            Log.e(TAG, "Exception while setCommand.");
        }
    }
}

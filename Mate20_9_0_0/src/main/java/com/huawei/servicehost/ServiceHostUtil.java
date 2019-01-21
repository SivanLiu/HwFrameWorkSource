package com.huawei.servicehost;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.servicehost.normal.IIPEvent4Metadata;
import com.huawei.servicehost.normal.IIPRequest4Metadata;

public class ServiceHostUtil {
    private static final boolean DEBUG = false;
    private static final String TAG = "ServiceHostUtil";

    public void setMetadata(IIPRequest4Metadata request, ServiceHostMetadata metadata) {
        try {
            request.setMetadata(metadata.getNativeMetadata());
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set metadata error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public TotalCaptureResult getTotalCaptureResult(IIPEvent4Metadata event4Metadata) {
        CameraMetadataNative metadataNative = null;
        try {
            metadataNative = event4Metadata.getMetadata();
            if (metadataNative == null) {
                Log.e(TAG, "result is null!");
                return null;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get total capture result error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return new TotalCaptureResult(metadataNative, -1);
    }

    public CaptureResult getCaptureResult(IIPEvent4Metadata event4Metadata) {
        CameraMetadataNative metadataNative = null;
        try {
            metadataNative = event4Metadata.getMetadata();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get capture result error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return new CaptureResult(metadataNative, -1);
    }

    public CameraCharacteristics getCharacteristics(IIPEvent4Metadata event4Metadata) {
        CameraMetadataNative metadataNative = null;
        try {
            metadataNative = event4Metadata.getMetadata();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get characteristics error: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return new CameraCharacteristics(metadataNative);
    }
}

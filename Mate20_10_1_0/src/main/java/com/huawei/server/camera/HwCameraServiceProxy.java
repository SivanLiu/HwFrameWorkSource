package com.huawei.server.camera;

import android.content.Context;
import android.hardware.display.HwFoldScreenState;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.android.os.HwVibrator;
import com.huawei.android.os.ServiceManagerEx;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.hwextdevice.HWExtDeviceManager;

public class HwCameraServiceProxy extends DefaultHwCameraServiceProxy {
    private static final String BUNDLE_OPTION_NAME_CAMERA_STATE = "cameraState";
    private static final String BUNDLE_OPTION_NAME_CLIENT_NAME = "clientName";
    private static final String BUNDLE_OPTION_NAME_FACING = "facing";
    private static final String CAMERA_ID_UNKNOWN = "-1";
    private static final String CLIENT_NAME_UNKNOWN = "unknown";
    private static final String CONFIG_HALL = "ro.config.hw_hall_prop";
    private static final String CONFIG_MEDIA_ASSISTANT = "ro.config.hw_media_assistant";
    private static final String CONFIG_VIDEO_CALL_ASSISTANT = "ro.config.videocalldistributedassist";
    private static final int DECIMAL_RADIX = 10;
    private static final int FACING_UNKNOWN = -1;
    private static final HwCameraFoldExImpl HW_CAMERA_FOLD_EX_IMPL = new HwCameraFoldExImpl();
    private static final String HW_CAMERA_PACKAGE_NAME = "com.huawei.camera";
    private static final boolean IS_FOLD_SCREEN_DEVICE = HwFoldScreenState.isFoldScreenDevice();
    private static final String MEDIA_ASSIST_INTERFACE = "com.huawei.systemserver.mediaassist.IMediaAssist";
    private static final String MEDIA_ASSIST_SERVICE = "media_assist";
    private static final int NOTIFY_SURFACEFLINGER_FRONT_CAMERA_CLOSE = 8012;
    private static final int NOTIFY_SURFACEFLINGER_FRONT_CAMERA_OPEN = 8011;
    private static final String TAG = "HwCameraServiceProxy";
    private static final int TYPE_CAMERA_AUTO = 1;
    private static final int TYPE_CAMERA_MANAUL = 2;
    private static final int TYPE_CAMERA_NORMAL = 0;
    private Context cameraContext;
    private int cameraDeviceType = 0;
    private HwCameraAutoImpl hwCameraAuto = null;
    private HwCameraManualImpl hwCameraManual = null;

    private void initHwCameraServiceProxyImpl() {
        int i = this.cameraDeviceType;
        if (i == 1) {
            this.hwCameraAuto = new HwCameraAutoImpl(this.cameraContext);
        } else if (i == TYPE_CAMERA_MANAUL) {
            this.hwCameraManual = new HwCameraManualImpl(this.cameraContext);
        }
    }

    private int getCameraDeviceType() {
        HWExtDeviceManager hwDeviceManager = HWExtDeviceManager.getInstance(this.cameraContext);
        if (hwDeviceManager != null && hwDeviceManager.supportMotionFeature(3100)) {
            Log.i(TAG, "Camera device is auto popup camera.");
            return 1;
        } else if (getSystemProperty(CONFIG_HALL, 0) == 1) {
            Log.i(TAG, "Camera device is manaul slide camera.");
            return TYPE_CAMERA_MANAUL;
        } else {
            Log.i(TAG, "Camera device is normal camera.");
            return 0;
        }
    }

    private void handleCameraState(int newCameraState, int facing, String clientName) {
        HwCameraManualImpl hwCameraManualImpl;
        if (newCameraState == 0) {
            HwCameraManualImpl hwCameraManualImpl2 = this.hwCameraManual;
            if (hwCameraManualImpl2 != null) {
                hwCameraManualImpl2.unRegisterSlideHallService();
            }
            if (this.cameraDeviceType == TYPE_CAMERA_MANAUL) {
                this.hwCameraManual = new HwCameraManualImpl(this.cameraContext);
                this.hwCameraManual.handleCameraState(newCameraState, facing, clientName);
            }
        }
        HwCameraAutoImpl hwCameraAutoImpl = this.hwCameraAuto;
        if (hwCameraAutoImpl != null) {
            hwCameraAutoImpl.handleCameraState(newCameraState, facing, clientName);
        }
        if (newCameraState != 0 && (hwCameraManualImpl = this.hwCameraManual) != null) {
            hwCameraManualImpl.handleCameraState(newCameraState, facing, clientName);
        }
    }

    public void notifyCameraStateChange(String cameraId, int newCameraState, int facing, String clientName) {
        handleCameraState(newCameraState, facing, clientName);
        setDisplayModeByCameraState(newCameraState, clientName);
        notifyCameraStateChangeForMediaAssistant(cameraId, newCameraState, facing, clientName);
        notifyCameraStateChangeForVibrator(cameraId, newCameraState, facing, clientName);
        notifyCameraStateChangeForSurfaceFlinger(cameraId, newCameraState, facing, clientName);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x006f, code lost:
        if (r7 != null) goto L_0x007d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x007b, code lost:
        if (r7 == null) goto L_0x0080;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x007d, code lost:
        r7.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0080, code lost:
        r6.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:?, code lost:
        return;
     */
    private void notifyCameraStateChangeForMediaAssistant(String cameraId, int newCameraState, int facing, String clientName) {
        IBinder mediaAssist;
        Log.i(TAG, "notifyCameraStateChangeForMediaAssistant cameraId " + cameraId + ", newCameraState " + newCameraState + ", facing " + facing);
        boolean isMediaAssitant = SystemPropertiesEx.getBoolean(CONFIG_MEDIA_ASSISTANT, false);
        boolean isVideocallAssitant = SystemPropertiesEx.getBoolean(CONFIG_VIDEO_CALL_ASSISTANT, false);
        if (!isMediaAssitant && !isVideocallAssitant) {
            return;
        }
        if ((newCameraState == 1 || newCameraState == TYPE_CAMERA_MANAUL) && (mediaAssist = ServiceManagerEx.getService(MEDIA_ASSIST_SERVICE)) != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            if (data == null) {
                Log.e(TAG, "data is null");
                return;
            }
            try {
                data.writeInterfaceToken(MEDIA_ASSIST_INTERFACE);
                data.writeString(cameraId);
                data.writeInt(facing);
                data.writeInt(newCameraState);
                data.writeString(clientName);
                mediaAssist.transact(1, data, reply, 0);
                if (reply != null) {
                    reply.readException();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "notifyCameraState transact error");
            } catch (Throwable th) {
                if (reply != null) {
                    reply.recycle();
                }
                data.recycle();
                throw th;
            }
        }
    }

    private void setDisplayModeByCameraState(int newCameraState, String clientName) {
        if (!IS_FOLD_SCREEN_DEVICE || !HW_CAMERA_PACKAGE_NAME.equals(clientName)) {
            Log.d(TAG, "It is not the fold camera device.");
        } else {
            HW_CAMERA_FOLD_EX_IMPL.handleWithCameraState(newCameraState);
        }
    }

    private void notifyCameraStateChangeForVibrator(String cameraId, int newCameraState, int facing, String clientName) {
        Log.i(TAG, "notifyCameraStateChangeForVibrator begin cameraId " + cameraId + ", newCameraState " + newCameraState + ", facing " + facing);
        if (newCameraState == 1 || newCameraState == TYPE_CAMERA_MANAUL) {
            Bundle options = new Bundle();
            options.putInt(BUNDLE_OPTION_NAME_CAMERA_STATE, newCameraState);
            options.putInt(BUNDLE_OPTION_NAME_FACING, facing);
            options.putString(BUNDLE_OPTION_NAME_CLIENT_NAME, clientName);
            HwVibrator.notifyVibrateOptions(options);
        }
        Log.i(TAG, "notifyCameraStateChangeForVibrator end");
    }

    public void binderDied() {
        Log.i(TAG, "binderDied: Native camera service has died");
        notifyCameraStateChangeForVibrator(CAMERA_ID_UNKNOWN, TYPE_CAMERA_MANAUL, FACING_UNKNOWN, CLIENT_NAME_UNKNOWN);
        notifyCameraStateChangeForSurfaceFlinger(CAMERA_ID_UNKNOWN, TYPE_CAMERA_MANAUL, 1, CLIENT_NAME_UNKNOWN);
    }

    public void initHwCameraServiceProxyParams(Context context) {
        Log.i(TAG, "initHwCameraServiceProxyParams in HwCameraServiceProxy");
        this.cameraContext = context;
        this.cameraDeviceType = getCameraDeviceType();
        initHwCameraServiceProxyImpl();
    }

    private int getSystemProperty(String key, int defaultValue) {
        String val = SystemPropertiesEx.get(key);
        if (val.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val, DECIMAL_RADIX);
        } catch (NumberFormatException e) {
            Log.e(TAG, "NumberFormatException");
            return defaultValue;
        }
    }

    private void notifyCameraStateChangeForSurfaceFlinger(String cameraId, int newCameraState, int facing, String clientName) {
        if (facing != 1) {
            return;
        }
        if (newCameraState == 1 || newCameraState == TYPE_CAMERA_MANAUL) {
            int status = newCameraState == 1 ? NOTIFY_SURFACEFLINGER_FRONT_CAMERA_OPEN : NOTIFY_SURFACEFLINGER_FRONT_CAMERA_CLOSE;
            Parcel dataIn = Parcel.obtain();
            try {
                IBinder sfBinder = ServiceManagerEx.getService("SurfaceFlinger");
                if (sfBinder == null) {
                    Log.i(TAG, "sfBinder == null");
                } else if (sfBinder.transact(status, dataIn, null, 1)) {
                    Log.i(TAG, "notifyCameraStateChangeForSurfaceFlinger " + status + " transact success!");
                } else {
                    Log.e(TAG, "notifyCameraStateChangeForSurfaceFlinger " + status + " transact failed!");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException notifyCameraStateChangeForSurfaceFlinger");
            } catch (Throwable th) {
                dataIn.recycle();
                throw th;
            }
            dataIn.recycle();
        }
    }
}

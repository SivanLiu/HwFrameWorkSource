package com.huawei.server.camera;

import android.hardware.display.HwFoldScreenState;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.huawei.android.fsm.HwFoldScreenManager;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;
import com.huawei.android.fsm.IFoldDisplayModeListener;
import com.huawei.android.os.ServiceManagerEx;
import com.huawei.android.server.LocalServicesEx;
import com.huawei.hardware.CameraServiceEx;

public class HwCameraFoldExImpl implements IBinder.DeathRecipient {
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    private static final int DEFAULT_FLAGS = 0;
    private static final String HW_FOLD_DISPLAY_MODE = "hw_fold_display_mode";
    private static final String HW_FOLD_DISPLAY_NO_SUB_MODE = "no_sub_mode";
    private static final String HW_FOLD_DISPLAY_SUB_MODE = "sub_mode";
    private static final boolean IS_FOLD_SCREEN_DEVICE = HwFoldScreenState.isFoldScreenDevice();
    private static final String TAG = "HwCameraFoldExImpl";
    private static CameraServiceEx foldCameraService = null;
    /* access modifiers changed from: private */
    public static int foldDisplayMode = DEFAULT_FLAGS;
    /* access modifiers changed from: private */
    public static boolean isIdle = true;
    private static boolean isNeedRegister = true;
    private IBinder cameraServiceBinder = null;
    private final IFoldDisplayModeListener displaymodeListener = new IFoldDisplayModeListener.Stub() {
        /* class com.huawei.server.camera.HwCameraFoldExImpl.AnonymousClass1 */

        public void onScreenDisplayModeChange(int displayMode) {
            String commandVaule;
            Log.d(HwCameraFoldExImpl.TAG, "onScreenDisplayModeChanges: from " + HwCameraFoldExImpl.foldDisplayMode + " to " + displayMode);
            if (HwCameraFoldExImpl.isIdle) {
                Log.d(HwCameraFoldExImpl.TAG, "The camera is in the idle state.");
                return;
            }
            int unused = HwCameraFoldExImpl.foldDisplayMode = displayMode;
            if (displayMode == 3) {
                commandVaule = HwCameraFoldExImpl.HW_FOLD_DISPLAY_SUB_MODE;
            } else {
                commandVaule = HwCameraFoldExImpl.HW_FOLD_DISPLAY_NO_SUB_MODE;
            }
            HwCameraFoldExImpl.this.setCommand(HwCameraFoldExImpl.HW_FOLD_DISPLAY_MODE, commandVaule);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.huawei.server.camera.HwCameraFoldExImpl$1 */
        /* JADX WARN: Multi-variable type inference failed */
        public IBinder asBinder() {
            return this;
        }
    };
    private HwFoldScreenManagerInternal foldScreenManagerInternal = null;

    public HwCameraFoldExImpl() {
        if (!IS_FOLD_SCREEN_DEVICE) {
            Log.d(TAG, "It is not the fold camera device.");
            return;
        }
        Log.d(TAG, "The HwCameraFoldExImpl");
        initFoldScreenService();
        initCameraService();
    }

    private void initFoldScreenService() {
        if (this.foldScreenManagerInternal == null) {
            Log.d(TAG, "initFoldScreenService");
            this.foldScreenManagerInternal = (HwFoldScreenManagerInternal) LocalServicesEx.getService(HwFoldScreenManagerInternal.class);
        }
    }

    private void initCameraService() {
        Log.d(TAG, "initCameraService");
        this.cameraServiceBinder = ServiceManagerEx.getService(CAMERA_SERVICE_BINDER_NAME);
        IBinder iBinder = this.cameraServiceBinder;
        if (iBinder == null) {
            Log.e(TAG, "The cameraServiceBinder is null");
            return;
        }
        try {
            iBinder.linkToDeath(this, DEFAULT_FLAGS);
            if (foldCameraService == null) {
                foldCameraService = new CameraServiceEx(this.cameraServiceBinder);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Could not link to death of native camera service");
        }
    }

    public void handleWithCameraState(int newCameraState) {
        if (newCameraState == 0) {
            registerListener();
        }
        if (newCameraState == 3) {
            unregisterListener();
        }
        if (newCameraState == 1) {
            Log.d(TAG, "It is camera state active.");
            setActive();
        }
        if (newCameraState == 2) {
            setIdle();
        }
    }

    private void registerListener() {
        if (!IS_FOLD_SCREEN_DEVICE) {
            Log.d(TAG, "It is not the fold camera device and does not need register listener.");
            return;
        }
        Log.d(TAG, "registerListener");
        if (isNeedRegister) {
            registerDisplayModeListener();
            isNeedRegister = false;
        }
    }

    private void unregisterListener() {
        if (!IS_FOLD_SCREEN_DEVICE) {
            Log.d(TAG, "It is not the fold camera device and does not need unregister listener.");
            return;
        }
        Log.d(TAG, "unregisterListener");
        if (!isNeedRegister) {
            unregisterDisplayModeListener();
            isNeedRegister = true;
        }
    }

    private void setActive() {
        if (!IS_FOLD_SCREEN_DEVICE) {
            Log.d(TAG, "It is not the fold camera device and does not need set the mode.");
            return;
        }
        isIdle = false;
        Log.d(TAG, "setActive");
        setCommandByCurrentMode();
    }

    private void setIdle() {
        Log.d(TAG, "setIdle");
        isIdle = true;
    }

    /* access modifiers changed from: private */
    public void setCommand(String commandType, String commandValue) {
        IBinder iBinder;
        Log.d(TAG, "setCommand the type is " + commandType + ", and the value is " + commandValue);
        if (foldCameraService == null || (iBinder = this.cameraServiceBinder) == null || !iBinder.pingBinder()) {
            Log.d(TAG, "The foldCameraService is not ok and get camera service again.");
            initCameraService();
        }
        foldCameraService.setCommand(commandType, commandValue);
    }

    private void registerDisplayModeListener() {
        Log.d(TAG, "register display mode listener");
        try {
            HwFoldScreenManager.registerFoldDisplayMode(this.displaymodeListener);
        } catch (IllegalArgumentException error) {
            Log.e(TAG, "Register with IllegalArgumentException" + error.getMessage());
        } catch (NoSuchMethodError error2) {
            Log.e(TAG, "Register with NoSuchMethodError" + error2.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Register with Exception");
        }
    }

    private void unregisterDisplayModeListener() {
        Log.d(TAG, "unregister display mode listener");
        try {
            HwFoldScreenManager.unregisterFoldDisplayMode(this.displaymodeListener);
        } catch (IllegalArgumentException error) {
            Log.e(TAG, "Unregister with IllegalArgumentException" + error.getMessage());
        } catch (NoSuchMethodError error2) {
            Log.e(TAG, "Unregister with NoSuchMethodError" + error2.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unregister with Exception");
        }
    }

    private void setCommandByCurrentMode() {
        String commandVaule;
        initFoldScreenService();
        HwFoldScreenManagerInternal hwFoldScreenManagerInternal = this.foldScreenManagerInternal;
        if (hwFoldScreenManagerInternal == null) {
            Log.e(TAG, "The foldScreenManagerInternal is null.");
            return;
        }
        int displayMode = hwFoldScreenManagerInternal.getDisplayMode();
        Log.d(TAG, "setCommandByCurrentMode: " + displayMode);
        if (displayMode == 3) {
            commandVaule = HW_FOLD_DISPLAY_SUB_MODE;
        } else if (displayMode == 2) {
            commandVaule = HW_FOLD_DISPLAY_NO_SUB_MODE;
        } else if (displayMode == 1) {
            commandVaule = HW_FOLD_DISPLAY_NO_SUB_MODE;
        } else {
            Log.d(TAG, "setCommandByCurrentMode: the unknown display mode.");
            return;
        }
        setCommand(HW_FOLD_DISPLAY_MODE, commandVaule);
    }

    public void binderDied() {
        Log.e(TAG, "Native camera service has died");
        this.cameraServiceBinder = null;
        foldCameraService = null;
    }
}

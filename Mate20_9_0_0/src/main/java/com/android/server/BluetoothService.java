package com.android.server;

import android.content.Context;

class BluetoothService extends SystemService {
    private static final String TAG = "BluetoothService";
    private BluetoothManagerService mBluetoothManagerService;

    public BluetoothService(Context context) {
        super(context);
        this.mBluetoothManagerService = HwServiceFactory.getHwBluetoothManagerService().createHwBluetoothManagerService(context);
    }

    public void onStart() {
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            publishBinderService("bluetooth_manager", this.mBluetoothManagerService);
        } else if (phase == 550) {
            HwLog.d(TAG, "onBootPhase: PHASE_SYSTEM_SERVICES_READY");
            this.mBluetoothManagerService.handleOnBootPhase();
        }
    }

    public void onSwitchUser(int userHandle) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSwitchUser: switching to user ");
        stringBuilder.append(userHandle);
        HwLog.d(str, stringBuilder.toString());
        this.mBluetoothManagerService.handleOnSwitchUser(userHandle);
    }

    public void onUnlockUser(int userHandle) {
        this.mBluetoothManagerService.handleOnUnlockUser(userHandle);
    }
}

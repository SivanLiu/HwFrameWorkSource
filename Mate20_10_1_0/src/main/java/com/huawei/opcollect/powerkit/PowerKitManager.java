package com.huawei.opcollect.powerkit;

import android.content.Context;
import android.os.RemoteException;
import com.huawei.android.powerkit.HuaweiPowerKit;
import com.huawei.android.powerkit.PowerKitConnection;
import com.huawei.opcollect.utils.OPCollectLog;

public class PowerKitManager {
    private static final Object LOCK = new Object();
    private static final String TAG = "PowerKitManager";
    private static PowerKitManager instance = null;
    private Context mContext;
    /* access modifiers changed from: private */
    public boolean mIsPowerKitConnected = false;
    private HuaweiPowerKit mPowerKit;
    private PowerKitConnection mPowerKitConnection;

    private PowerKitManager(Context context) {
        OPCollectLog.r(TAG, TAG);
        this.mContext = context;
        this.mPowerKitConnection = new PowerKitConnection() {
            /* class com.huawei.opcollect.powerkit.PowerKitManager.AnonymousClass1 */

            @Override // com.huawei.android.powerkit.PowerKitConnection
            public void onServiceDisconnected() {
                OPCollectLog.r(PowerKitManager.TAG, "PowerKit disconnected");
                boolean unused = PowerKitManager.this.mIsPowerKitConnected = false;
            }

            @Override // com.huawei.android.powerkit.PowerKitConnection
            public void onServiceConnected() {
                OPCollectLog.r(PowerKitManager.TAG, "PowerKitManager connected");
                boolean unused = PowerKitManager.this.mIsPowerKitConnected = true;
            }
        };
        this.mPowerKit = HuaweiPowerKit.getInstance(this.mContext, this.mPowerKitConnection);
    }

    public static PowerKitManager getInstance(Context context) {
        PowerKitManager powerKitManager;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new PowerKitManager(context);
            }
            powerKitManager = instance;
        }
        return powerKitManager;
    }

    public boolean isUserSleeping() {
        if (!this.mIsPowerKitConnected) {
            return false;
        }
        try {
            return this.mPowerKit.isUserSleeping();
        } catch (RemoteException e) {
            OPCollectLog.r(TAG, "PowerKit exception: " + e.getMessage());
            return false;
        }
    }
}

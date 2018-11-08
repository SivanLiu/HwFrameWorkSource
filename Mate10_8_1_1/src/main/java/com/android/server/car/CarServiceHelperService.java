package com.android.server.car;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.car.ICarServiceHelper.Stub;
import com.android.server.SystemService;

public class CarServiceHelperService extends SystemService {
    private static final String CAR_SERVICE_INTERFACE = "android.car.ICar";
    private static final String TAG = "CarServiceHelper";
    private IBinder mCarService;
    private final ServiceConnection mCarServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Slog.i(CarServiceHelperService.TAG, "**CarService connected**");
            CarServiceHelperService.this.mCarService = iBinder;
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken(CarServiceHelperService.CAR_SERVICE_INTERFACE);
            data.writeStrongBinder(CarServiceHelperService.this.mHelper.asBinder());
            try {
                CarServiceHelperService.this.mCarService.transact(1, data, null, 1);
            } catch (RemoteException e) {
                Slog.w(CarServiceHelperService.TAG, "RemoteException from car service", e);
                CarServiceHelperService.this.handleCarServiceCrash();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            CarServiceHelperService.this.handleCarServiceCrash();
        }
    };
    private final ICarServiceHelperImpl mHelper = new ICarServiceHelperImpl();

    private class ICarServiceHelperImpl extends Stub {
        private ICarServiceHelperImpl() {
        }
    }

    public CarServiceHelperService(Context context) {
        super(context);
    }

    public void onStart() {
        Intent intent = new Intent();
        intent.setPackage("com.android.car");
        intent.setAction(CAR_SERVICE_INTERFACE);
        if (!getContext().bindServiceAsUser(intent, this.mCarServiceConnection, 1, UserHandle.SYSTEM)) {
            Slog.wtf(TAG, "cannot start car service");
        }
    }

    private void handleCarServiceCrash() {
    }
}

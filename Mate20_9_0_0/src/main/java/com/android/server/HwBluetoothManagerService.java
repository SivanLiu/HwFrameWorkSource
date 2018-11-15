package com.android.server;

import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothProfileServiceConnection;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.huawei.hsm.permission.StubController;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class HwBluetoothManagerService extends BluetoothManagerService {
    private static final String TAG = "HwBluetoothManagerService";
    private static BluetoothParaManager mBluetoothParaManager = null;

    public /* bridge */ /* synthetic */ boolean bindBluetoothProfileService(int x0, IBluetoothProfileServiceConnection x1) {
        return super.bindBluetoothProfileService(x0, x1);
    }

    public /* bridge */ /* synthetic */ boolean disable(String x0, boolean x1) throws RemoteException {
        return super.disable(x0, x1);
    }

    public /* bridge */ /* synthetic */ boolean disableRadio() {
        return super.disableRadio();
    }

    public /* bridge */ /* synthetic */ void dump(FileDescriptor x0, PrintWriter x1, String[] x2) {
        super.dump(x0, x1, x2);
    }

    public /* bridge */ /* synthetic */ boolean enable(String x0) throws RemoteException {
        return super.enable(x0);
    }

    public /* bridge */ /* synthetic */ boolean enableNoAutoConnect(String x0) {
        return super.enableNoAutoConnect(x0);
    }

    public /* bridge */ /* synthetic */ boolean enableRadio() {
        return super.enableRadio();
    }

    public /* bridge */ /* synthetic */ String getAddress() {
        return super.getAddress();
    }

    public /* bridge */ /* synthetic */ IBluetoothGatt getBluetoothGatt() {
        return super.getBluetoothGatt();
    }

    public /* bridge */ /* synthetic */ String getName() {
        return super.getName();
    }

    public /* bridge */ /* synthetic */ void getNameAndAddress() {
        super.getNameAndAddress();
    }

    public /* bridge */ /* synthetic */ int getState() {
        return super.getState();
    }

    public /* bridge */ /* synthetic */ void handleOnBootPhase() {
        super.handleOnBootPhase();
    }

    public /* bridge */ /* synthetic */ void handleOnSwitchUser(int x0) {
        super.handleOnSwitchUser(x0);
    }

    public /* bridge */ /* synthetic */ void handleOnUnlockUser(int x0) {
        super.handleOnUnlockUser(x0);
    }

    public /* bridge */ /* synthetic */ boolean isBleAppPresent() {
        return super.isBleAppPresent();
    }

    public /* bridge */ /* synthetic */ boolean isBleScanAlwaysAvailable() {
        return super.isBleScanAlwaysAvailable();
    }

    public /* bridge */ /* synthetic */ boolean isEnabled() {
        return super.isEnabled();
    }

    public /* bridge */ /* synthetic */ boolean isRadioEnabled() {
        return super.isRadioEnabled();
    }

    public /* bridge */ /* synthetic */ IBluetooth registerAdapter(IBluetoothManagerCallback x0) {
        return super.registerAdapter(x0);
    }

    public /* bridge */ /* synthetic */ void registerStateChangeCallback(IBluetoothStateChangeCallback x0) {
        super.registerStateChangeCallback(x0);
    }

    public /* bridge */ /* synthetic */ void unbindAndFinish() {
        super.unbindAndFinish();
    }

    public /* bridge */ /* synthetic */ void unbindBluetoothProfileService(int x0, IBluetoothProfileServiceConnection x1) {
        super.unbindBluetoothProfileService(x0, x1);
    }

    public /* bridge */ /* synthetic */ void unregisterAdapter(IBluetoothManagerCallback x0) {
        super.unregisterAdapter(x0);
    }

    public /* bridge */ /* synthetic */ void unregisterStateChangeCallback(IBluetoothStateChangeCallback x0) {
        super.unregisterStateChangeCallback(x0);
    }

    public /* bridge */ /* synthetic */ int updateBleAppCount(IBinder x0, boolean x1, String x2) {
        return super.updateBleAppCount(x0, x1, x2);
    }

    public HwBluetoothManagerService(Context context) {
        super(context);
        if (mBluetoothParaManager == null) {
            mBluetoothParaManager = new BluetoothParaManager(context);
        }
    }

    public boolean checkPrecondition(int uid) {
        return StubController.checkPrecondition(uid);
    }
}

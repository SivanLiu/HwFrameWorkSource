package android.bluetooth;

import android.bluetooth.IBluetoothGattCallback.Stub;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class BluetoothGatt implements BluetoothProfile {
    static final int AUTHENTICATION_MITM = 2;
    static final int AUTHENTICATION_NONE = 0;
    static final int AUTHENTICATION_NO_MITM = 1;
    private static final int AUTH_RETRY_STATE_IDLE = 0;
    private static final int AUTH_RETRY_STATE_MITM = 2;
    private static final int AUTH_RETRY_STATE_NO_MITM = 1;
    public static final int CONNECTION_PRIORITY_BALANCED = 0;
    public static final int CONNECTION_PRIORITY_HIGH = 1;
    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
    private static final int CONN_STATE_CLOSED = 4;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_IDLE = 0;
    private static final boolean DBG = true;
    public static final int GATT_CONNECTION_CONGESTED = 143;
    public static final int GATT_FAILURE = 257;
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 5;
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 15;
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 13;
    public static final int GATT_INVALID_OFFSET = 7;
    public static final int GATT_READ_NOT_PERMITTED = 2;
    public static final int GATT_REQUEST_NOT_SUPPORTED = 6;
    public static final int GATT_SUCCESS = 0;
    public static final int GATT_WRITE_NOT_PERMITTED = 3;
    private static final String TAG = "BluetoothGatt";
    private static final boolean VDBG = false;
    private int mAuthRetryState;
    private boolean mAutoConnect;
    private final IBluetoothGattCallback mBluetoothGattCallback = new Stub() {
        public void onClientRegistered(int status, int clientIf) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onClientRegistered() - status=");
            stringBuilder.append(status);
            stringBuilder.append(" clientIf=");
            stringBuilder.append(clientIf);
            Log.d(str, stringBuilder.toString());
            BluetoothGatt.this.mClientIf = clientIf;
            if (status != 0) {
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onConnectionStateChange(BluetoothGatt.this, 257, 0);
                        }
                    }
                });
                synchronized (BluetoothGatt.this.mStateLock) {
                    BluetoothGatt.this.mConnState = 0;
                }
                return;
            }
            try {
                if (12 == (BluetoothGatt.this.mTransport & 12)) {
                    BluetoothGatt.this.mService.fastClientConnect(BluetoothGatt.this.mClientIf, BluetoothGatt.this.mDevice.getAddress(), BluetoothGatt.this.mAutoConnect ^ 1, BluetoothGatt.this.mTransport);
                } else {
                    BluetoothGatt.this.mService.clientConnect(BluetoothGatt.this.mClientIf, BluetoothGatt.this.mDevice.getAddress(), BluetoothGatt.this.mAutoConnect ^ 1, BluetoothGatt.this.mTransport, BluetoothGatt.this.mOpportunistic, BluetoothGatt.this.mPhy);
                }
            } catch (RemoteException e) {
                Log.e(BluetoothGatt.TAG, "", e);
            }
        }

        public void onPhyUpdate(String address, final int txPhy, final int rxPhy, final int status) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPhyUpdate() - status=");
            stringBuilder.append(status);
            stringBuilder.append(" address=");
            stringBuilder.append(address);
            stringBuilder.append(" txPhy=");
            stringBuilder.append(txPhy);
            stringBuilder.append(" rxPhy=");
            stringBuilder.append(rxPhy);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onPhyUpdate(BluetoothGatt.this, txPhy, rxPhy, status);
                        }
                    }
                });
            }
        }

        public void onPhyRead(String address, final int txPhy, final int rxPhy, final int status) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPhyRead() - status=");
            stringBuilder.append(status);
            stringBuilder.append(" address=");
            stringBuilder.append(address);
            stringBuilder.append(" txPhy=");
            stringBuilder.append(txPhy);
            stringBuilder.append(" rxPhy=");
            stringBuilder.append(rxPhy);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onPhyRead(BluetoothGatt.this, txPhy, rxPhy, status);
                        }
                    }
                });
            }
        }

        public void onClientConnectionState(final int status, int clientIf, boolean connected, String address) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onClientConnectionState() - status=");
            stringBuilder.append(status);
            stringBuilder.append(" clientIf=");
            stringBuilder.append(clientIf);
            stringBuilder.append(" device=");
            stringBuilder.append(address);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                final int profileState = connected ? 2 : 0;
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onConnectionStateChange(BluetoothGatt.this, status, profileState);
                        }
                    }
                });
                synchronized (BluetoothGatt.this.mStateLock) {
                    if (connected) {
                        try {
                            BluetoothGatt.this.mConnState = 2;
                        } catch (Throwable th) {
                            while (true) {
                            }
                        }
                    } else {
                        BluetoothGatt.this.mConnState = 0;
                    }
                }
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
            }
        }

        public void onSearchComplete(String address, List<BluetoothGattService> services, final int status) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSearchComplete() = Device=");
            stringBuilder.append(address);
            stringBuilder.append(" Status=");
            stringBuilder.append(status);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                for (BluetoothGattService s : services) {
                    s.setDevice(BluetoothGatt.this.mDevice);
                }
                BluetoothGatt.this.mServices.addAll(services);
                for (BluetoothGattService s2 : BluetoothGatt.this.mServices) {
                    ArrayList<BluetoothGattService> includedServices = new ArrayList(s2.getIncludedServices());
                    s2.getIncludedServices().clear();
                    Iterator it = includedServices.iterator();
                    while (it.hasNext()) {
                        BluetoothGattService brokenRef = (BluetoothGattService) it.next();
                        BluetoothGattService includedService = BluetoothGatt.this.getService(BluetoothGatt.this.mDevice, brokenRef.getUuid(), brokenRef.getInstanceId());
                        if (includedService != null) {
                            s2.addIncludedService(includedService);
                        } else {
                            Log.e(BluetoothGatt.TAG, "Broken GATT database: can't find included service.");
                        }
                    }
                }
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onServicesDiscovered(BluetoothGatt.this, status);
                        }
                    }
                });
            }
        }

        public void onCharacteristicRead(String address, final int status, int handle, final byte[] value) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
                if (status == 5 || status == 15) {
                    int i = 2;
                    if (BluetoothGatt.this.mAuthRetryState != 2) {
                        try {
                            if (BluetoothGatt.this.mAuthRetryState == 0) {
                                i = 1;
                            }
                            BluetoothGatt.this.mService.readCharacteristic(BluetoothGatt.this.mClientIf, address, handle, i);
                            BluetoothGatt.this.mAuthRetryState = BluetoothGatt.this.mAuthRetryState + 1;
                            return;
                        } catch (RemoteException e) {
                            Log.e(BluetoothGatt.TAG, "", e);
                        }
                    }
                }
                BluetoothGatt.this.mAuthRetryState = 0;
                final BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
                if (characteristic == null) {
                    Log.w(BluetoothGatt.TAG, "onCharacteristicRead() failed to find characteristic!");
                } else {
                    BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                        public void run() {
                            BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                            if (callback != null) {
                                if (status == 0) {
                                    characteristic.setValue(value);
                                }
                                callback.onCharacteristicRead(BluetoothGatt.this, characteristic, status);
                            }
                        }
                    });
                }
            }
        }

        public void onCharacteristicWrite(String address, final int status, int handle) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
                final BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
                if (characteristic != null) {
                    if (status == 5 || status == 15) {
                        int i = 2;
                        if (BluetoothGatt.this.mAuthRetryState != 2) {
                            try {
                                if (BluetoothGatt.this.mAuthRetryState == 0) {
                                    i = 1;
                                }
                                BluetoothGatt.this.mService.writeCharacteristic(BluetoothGatt.this.mClientIf, address, handle, characteristic.getWriteType(), i, characteristic.getValue());
                                BluetoothGatt.this.mAuthRetryState = BluetoothGatt.this.mAuthRetryState + 1;
                                return;
                            } catch (RemoteException e) {
                                Log.e(BluetoothGatt.TAG, "", e);
                            }
                        }
                    }
                    BluetoothGatt.this.mAuthRetryState = 0;
                    BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                        public void run() {
                            BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                            if (callback != null) {
                                callback.onCharacteristicWrite(BluetoothGatt.this, characteristic, status);
                            }
                        }
                    });
                }
            }
        }

        public void onNotify(String address, int handle, final byte[] value) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                final BluetoothGattCharacteristic characteristic = BluetoothGatt.this.getCharacteristicById(BluetoothGatt.this.mDevice, handle);
                if (characteristic != null) {
                    BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                        public void run() {
                            BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                            if (callback != null) {
                                characteristic.setValue(value);
                                callback.onCharacteristicChanged(BluetoothGatt.this, characteristic);
                            }
                        }
                    });
                }
            }
        }

        public void onDescriptorRead(String address, final int status, int handle, final byte[] value) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
                final BluetoothGattDescriptor descriptor = BluetoothGatt.this.getDescriptorById(BluetoothGatt.this.mDevice, handle);
                if (descriptor != null) {
                    if (status == 5 || status == 15) {
                        int i = 2;
                        if (BluetoothGatt.this.mAuthRetryState != 2) {
                            try {
                                if (BluetoothGatt.this.mAuthRetryState == 0) {
                                    i = 1;
                                }
                                BluetoothGatt.this.mService.readDescriptor(BluetoothGatt.this.mClientIf, address, handle, i);
                                BluetoothGatt.this.mAuthRetryState = BluetoothGatt.this.mAuthRetryState + 1;
                                return;
                            } catch (RemoteException e) {
                                Log.e(BluetoothGatt.TAG, "", e);
                            }
                        }
                    }
                    BluetoothGatt.this.mAuthRetryState = 0;
                    BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                        public void run() {
                            BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                            if (callback != null) {
                                if (status == 0) {
                                    descriptor.setValue(value);
                                }
                                callback.onDescriptorRead(BluetoothGatt.this, descriptor, status);
                            }
                        }
                    });
                }
            }
        }

        public void onDescriptorWrite(String address, final int status, int handle) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
                final BluetoothGattDescriptor descriptor = BluetoothGatt.this.getDescriptorById(BluetoothGatt.this.mDevice, handle);
                if (descriptor != null) {
                    if (status == 5 || status == 15) {
                        int i = 2;
                        if (BluetoothGatt.this.mAuthRetryState != 2) {
                            try {
                                if (BluetoothGatt.this.mAuthRetryState == 0) {
                                    i = 1;
                                }
                                BluetoothGatt.this.mService.writeDescriptor(BluetoothGatt.this.mClientIf, address, handle, i, descriptor.getValue());
                                BluetoothGatt.this.mAuthRetryState = BluetoothGatt.this.mAuthRetryState + 1;
                                return;
                            } catch (RemoteException e) {
                                Log.e(BluetoothGatt.TAG, "", e);
                            }
                        }
                    }
                    BluetoothGatt.this.mAuthRetryState = 0;
                    BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                        public void run() {
                            BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                            if (callback != null) {
                                callback.onDescriptorWrite(BluetoothGatt.this, descriptor, status);
                            }
                        }
                    });
                }
            }
        }

        public void onExecuteWrite(String address, final int status) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                synchronized (BluetoothGatt.this.mDeviceBusy) {
                    BluetoothGatt.this.mDeviceBusy = Boolean.valueOf(false);
                }
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onReliableWriteCompleted(BluetoothGatt.this, status);
                        }
                    }
                });
            }
        }

        public void onReadRemoteRssi(String address, final int rssi, final int status) {
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onReadRemoteRssi(BluetoothGatt.this, rssi, status);
                        }
                    }
                });
            }
        }

        public void onConfigureMTU(String address, final int mtu, final int status) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConfigureMTU() - Device=");
            stringBuilder.append(address);
            stringBuilder.append(" mtu=");
            stringBuilder.append(mtu);
            stringBuilder.append(" status=");
            stringBuilder.append(status);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onMtuChanged(BluetoothGatt.this, mtu, status);
                        }
                    }
                });
            }
        }

        public void onConnectionUpdated(String address, int interval, int latency, int timeout, int status) {
            String str = BluetoothGatt.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectionUpdated() - Device=");
            stringBuilder.append(address);
            stringBuilder.append(" interval=");
            stringBuilder.append(interval);
            stringBuilder.append(" latency=");
            stringBuilder.append(latency);
            stringBuilder.append(" timeout=");
            stringBuilder.append(timeout);
            stringBuilder.append(" status=");
            stringBuilder.append(status);
            Log.d(str, stringBuilder.toString());
            if (address.equals(BluetoothGatt.this.mDevice.getAddress())) {
                final int i = interval;
                final int i2 = latency;
                final int i3 = timeout;
                final int i4 = status;
                BluetoothGatt.this.runOrQueueCallback(new Runnable() {
                    public void run() {
                        BluetoothGattCallback callback = BluetoothGatt.this.mCallback;
                        if (callback != null) {
                            callback.onConnectionUpdated(BluetoothGatt.this, i, i2, i3, i4);
                        }
                    }
                });
            }
        }
    };
    private volatile BluetoothGattCallback mCallback;
    private int mClientIf;
    private int mConnState;
    private BluetoothDevice mDevice;
    private Boolean mDeviceBusy = Boolean.valueOf(false);
    private Handler mHandler;
    private boolean mOpportunistic;
    private int mPhy;
    private IBluetoothGatt mService;
    private List<BluetoothGattService> mServices;
    private final Object mStateLock = new Object();
    private int mTransport;

    BluetoothGatt(IBluetoothGatt iGatt, BluetoothDevice device, int transport, boolean opportunistic, int phy) {
        this.mService = iGatt;
        this.mDevice = device;
        this.mTransport = transport;
        this.mPhy = phy;
        this.mOpportunistic = opportunistic;
        this.mServices = new ArrayList();
        this.mConnState = 0;
        this.mAuthRetryState = 0;
    }

    public void close() {
        Log.d(TAG, "close()");
        unregisterApp();
        this.mConnState = 4;
        this.mAuthRetryState = 0;
    }

    BluetoothGattService getService(BluetoothDevice device, UUID uuid, int instanceId) {
        for (BluetoothGattService svc : this.mServices) {
            if (svc.getDevice().equals(device) && svc.getInstanceId() == instanceId && svc.getUuid().equals(uuid)) {
                return svc;
            }
        }
        return null;
    }

    BluetoothGattCharacteristic getCharacteristicById(BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : this.mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                if (charac.getInstanceId() == instanceId) {
                    return charac;
                }
            }
        }
        return null;
    }

    BluetoothGattDescriptor getDescriptorById(BluetoothDevice device, int instanceId) {
        for (BluetoothGattService svc : this.mServices) {
            for (BluetoothGattCharacteristic charac : svc.getCharacteristics()) {
                for (BluetoothGattDescriptor desc : charac.getDescriptors()) {
                    if (desc.getInstanceId() == instanceId) {
                        return desc;
                    }
                }
            }
        }
        return null;
    }

    private void runOrQueueCallback(Runnable cb) {
        if (this.mHandler == null) {
            try {
                cb.run();
                return;
            } catch (Exception ex) {
                Log.w(TAG, "Unhandled exception in callback", ex);
                return;
            }
        }
        this.mHandler.post(cb);
    }

    private boolean registerApp(BluetoothGattCallback callback, Handler handler) {
        Log.d(TAG, "registerApp()");
        if (this.mService == null) {
            return false;
        }
        this.mCallback = callback;
        this.mHandler = handler;
        UUID uuid = UUID.randomUUID();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerApp() - UUID=");
        stringBuilder.append(uuid);
        Log.d(str, stringBuilder.toString());
        try {
            this.mService.registerClient(new ParcelUuid(uuid), this.mBluetoothGattCallback);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    private void unregisterApp() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterApp() - mClientIf=");
        stringBuilder.append(this.mClientIf);
        Log.d(str, stringBuilder.toString());
        if (this.mService != null && this.mClientIf != 0) {
            try {
                this.mCallback = null;
                this.mService.unregisterClient(this.mClientIf);
                this.mClientIf = 0;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    boolean connect(Boolean autoConnect, BluetoothGattCallback callback, Handler handler) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        stringBuilder.append(", auto: ");
        stringBuilder.append(autoConnect);
        Log.d(str, stringBuilder.toString());
        synchronized (this.mStateLock) {
            if (this.mConnState == 0) {
                this.mConnState = 1;
            } else {
                throw new IllegalStateException("Not idle");
            }
        }
        this.mAutoConnect = autoConnect.booleanValue();
        if (registerApp(callback, handler)) {
            return true;
        }
        synchronized (this.mStateLock) {
            this.mConnState = 0;
        }
        Log.e(TAG, "Failed to register callback");
        return false;
    }

    public void disconnect() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelOpen() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        Log.d(str, stringBuilder.toString());
        if (this.mService != null && this.mClientIf != 0) {
            try {
                this.mService.clientDisconnect(this.mClientIf, this.mDevice.getAddress());
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    public boolean connect() {
        Log.i(TAG, "connect is called");
        try {
            this.mService.clientConnect(this.mClientIf, this.mDevice.getAddress(), false, this.mTransport, this.mOpportunistic, this.mPhy);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions) {
        try {
            this.mService.clientSetPreferredPhy(this.mClientIf, this.mDevice.getAddress(), txPhy, rxPhy, phyOptions);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    public void readPhy() {
        try {
            this.mService.clientReadPhy(this.mClientIf, this.mDevice.getAddress());
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public boolean discoverServices() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("discoverServices() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        this.mServices.clear();
        try {
            this.mService.discoverServices(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean discoverServiceByUuid(UUID uuid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("discoverServiceByUuid() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        this.mServices.clear();
        try {
            this.mService.discoverServiceByUuid(this.mClientIf, this.mDevice.getAddress(), new ParcelUuid(uuid));
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public List<BluetoothGattService> getServices() {
        List<BluetoothGattService> result = new ArrayList();
        for (BluetoothGattService service : this.mServices) {
            if (service.getDevice().equals(this.mDevice)) {
                result.add(service);
            }
        }
        return result;
    }

    public BluetoothGattService getService(UUID uuid) {
        for (BluetoothGattService service : this.mServices) {
            if (service.getDevice().equals(this.mDevice) && service.getUuid().equals(uuid)) {
                return service;
            }
        }
        return null;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & 2) == 0 || this.mService == null || this.mClientIf == 0) {
            return false;
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            return false;
        }
        BluetoothDevice device = service.getDevice();
        if (device == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.readCharacteristic(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), 0);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public boolean readUsingCharacteristicUuid(UUID uuid, int startHandle, int endHandle) {
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.readUsingCharacteristicUuid(this.mClientIf, this.mDevice.getAddress(), new ParcelUuid(uuid), startHandle, endHandle, 0);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (((characteristic.getProperties() & 8) == 0 && (characteristic.getProperties() & 4) == 0) || this.mService == null || this.mClientIf == 0 || characteristic.getValue() == null) {
            return false;
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            return false;
        }
        BluetoothDevice device = service.getDevice();
        if (device == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.writeCharacteristic(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), characteristic.getWriteType(), 0, characteristic.getValue());
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor) {
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) {
            return false;
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            return false;
        }
        BluetoothDevice device = service.getDevice();
        if (device == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.readDescriptor(this.mClientIf, device.getAddress(), descriptor.getInstanceId(), 0);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        if (this.mService == null || this.mClientIf == 0 || descriptor.getValue() == null) {
            return false;
        }
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (characteristic == null) {
            return false;
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            return false;
        }
        BluetoothDevice device = service.getDevice();
        if (device == null) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.writeDescriptor(this.mClientIf, device.getAddress(), descriptor.getInstanceId(), 0, descriptor.getValue());
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public boolean beginReliableWrite() {
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.beginReliableWrite(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean executeReliableWrite() {
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        synchronized (this.mDeviceBusy) {
            if (this.mDeviceBusy.booleanValue()) {
                return false;
            }
            this.mDeviceBusy = Boolean.valueOf(true);
            try {
                this.mService.endReliableWrite(this.mClientIf, this.mDevice.getAddress(), true);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
                this.mDeviceBusy = Boolean.valueOf(false);
                return false;
            }
        }
    }

    public void abortReliableWrite() {
        if (this.mService != null && this.mClientIf != 0) {
            try {
                this.mService.endReliableWrite(this.mClientIf, this.mDevice.getAddress(), false);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    }

    @Deprecated
    public void abortReliableWrite(BluetoothDevice mDevice) {
        abortReliableWrite();
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCharacteristicNotification() - uuid: ");
        stringBuilder.append(characteristic.getUuid());
        stringBuilder.append(" enable: ");
        stringBuilder.append(enable);
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        BluetoothGattService service = characteristic.getService();
        if (service == null) {
            return false;
        }
        BluetoothDevice device = service.getDevice();
        if (device == null) {
            return false;
        }
        try {
            this.mService.registerForNotification(this.mClientIf, device.getAddress(), characteristic.getInstanceId(), enable);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean refresh() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("refresh() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.refreshDevice(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean readRemoteRssi() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("readRssi() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.readRemoteRssi(this.mClientIf, this.mDevice.getAddress());
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean requestMtu(int mtu) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("configureMTU() - device: ");
        stringBuilder.append(this.mDevice.getAddress());
        stringBuilder.append(" mtu: ");
        stringBuilder.append(mtu);
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.configureMTU(this.mClientIf, this.mDevice.getAddress(), mtu);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean requestConnectionPriority(int connectionPriority) {
        if (connectionPriority < 0 || connectionPriority > 2) {
            throw new IllegalArgumentException("connectionPriority not within valid range");
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestConnectionPriority() - params: ");
        stringBuilder.append(connectionPriority);
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.connectionParameterUpdate(this.mClientIf, this.mDevice.getAddress(), connectionPriority);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public boolean requestLeConnectionUpdate(int minConnectionInterval, int maxConnectionInterval, int slaveLatency, int supervisionTimeout, int minConnectionEventLen, int maxConnectionEventLen) {
        int i = minConnectionInterval;
        int i2 = maxConnectionInterval;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestLeConnectionUpdate() - min=(");
        stringBuilder.append(i);
        stringBuilder.append(")");
        stringBuilder.append(((double) i) * 1.25d);
        stringBuilder.append("msec, max=(");
        stringBuilder.append(i2);
        stringBuilder.append(")");
        stringBuilder.append(1.25d * ((double) i2));
        stringBuilder.append("msec, latency=");
        int i3 = slaveLatency;
        stringBuilder.append(i3);
        stringBuilder.append(", timeout=");
        int i4 = supervisionTimeout;
        stringBuilder.append(i4);
        stringBuilder.append("msec, min_ce=");
        int i5 = minConnectionEventLen;
        stringBuilder.append(i5);
        stringBuilder.append(", max_ce=");
        stringBuilder.append(maxConnectionEventLen);
        Log.d(str, stringBuilder.toString());
        if (this.mService == null || this.mClientIf == 0) {
            return false;
        }
        try {
            this.mService.leConnectionUpdate(this.mClientIf, this.mDevice.getAddress(), i, i2, i3, i4, i5, maxConnectionEventLen);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            return false;
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    public List<BluetoothDevice> getConnectedDevices() {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectedDevices instead.");
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        throw new UnsupportedOperationException("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}

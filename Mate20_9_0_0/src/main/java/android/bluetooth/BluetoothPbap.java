package android.bluetooth;

import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BluetoothPbap implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    private static final String TAG = "BluetoothPbap";
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange: up=");
            stringBuilder.append(up);
            BluetoothPbap.log(stringBuilder.toString());
            if (up) {
                synchronized (BluetoothPbap.this.mConnection) {
                    try {
                        if (BluetoothPbap.this.mService == null) {
                            BluetoothPbap.log("Binding service...");
                            BluetoothPbap.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothPbap.TAG, "", re);
                    }
                }
                return;
            }
            BluetoothPbap.log("Unbinding service...");
            synchronized (BluetoothPbap.this.mConnection) {
                try {
                    BluetoothPbap.this.mService = null;
                    BluetoothPbap.this.mContext.unbindService(BluetoothPbap.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothPbap.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothPbap.log("Proxy object connected");
            BluetoothPbap.this.mService = IBluetoothPbap.Stub.asInterface(service);
            if (BluetoothPbap.this.mServiceListener != null) {
                BluetoothPbap.this.mServiceListener.onServiceConnected(BluetoothPbap.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            BluetoothPbap.log("Proxy object disconnected");
            BluetoothPbap.this.mService = null;
            if (BluetoothPbap.this.mServiceListener != null) {
                BluetoothPbap.this.mServiceListener.onServiceDisconnected();
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothPbap mService;
    private ServiceListener mServiceListener;

    public interface ServiceListener {
        void onServiceConnected(BluetoothPbap bluetoothPbap);

        void onServiceDisconnected();
    }

    public BluetoothPbap(Context context, ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothPbap.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth Pbap Service with ");
        stringBuilder.append(intent);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    public synchronized void close() {
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
        synchronized (this.mConnection) {
            if (this.mService != null) {
                try {
                    this.mService = null;
                    this.mContext.unbindService(this.mConnection);
                } catch (Exception re) {
                    Log.e(TAG, "", re);
                }
            }
        }
        this.mServiceListener = null;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        log("getConnectedDevices()");
        IBluetoothPbap service = this.mService;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
        try {
            return service.getConnectedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return new ArrayList();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getConnectionState: device=");
        stringBuilder.append(device);
        log(stringBuilder.toString());
        IBluetoothPbap service = this.mService;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return 0;
        }
        try {
            return service.getConnectionState(device);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return 0;
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDevicesMatchingConnectionStates: states=");
        stringBuilder.append(Arrays.toString(states));
        log(stringBuilder.toString());
        IBluetoothPbap service = this.mService;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
        try {
            return service.getDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return new ArrayList();
        }
    }

    public boolean isConnected(BluetoothDevice device) {
        return getConnectionState(device) == 2;
    }

    public boolean disconnect(BluetoothDevice device) {
        log("disconnect()");
        IBluetoothPbap service = this.mService;
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
            return false;
        }
        try {
            service.disconnect(device);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    private static void log(String msg) {
    }
}

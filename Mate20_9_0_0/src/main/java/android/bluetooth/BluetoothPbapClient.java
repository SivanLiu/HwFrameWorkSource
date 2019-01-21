package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothPbapClient implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private static final String TAG = "BluetoothPbapClient";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                synchronized (BluetoothPbapClient.this.mConnection) {
                    try {
                        if (BluetoothPbapClient.this.mService == null) {
                            BluetoothPbapClient.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothPbapClient.TAG, "", re);
                    }
                }
                return;
            }
            synchronized (BluetoothPbapClient.this.mConnection) {
                try {
                    BluetoothPbapClient.this.mService = null;
                    BluetoothPbapClient.this.mContext.unbindService(BluetoothPbapClient.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothPbapClient.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothPbapClient.this.mService = IBluetoothPbapClient.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothPbapClient.this.mServiceListener != null) {
                BluetoothPbapClient.this.mServiceListener.onServiceConnected(17, BluetoothPbapClient.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            synchronized (BluetoothPbapClient.this.mConnection) {
                if (BluetoothPbapClient.this.mService != null) {
                    try {
                        BluetoothPbapClient.this.mService = null;
                        BluetoothPbapClient.this.mContext.unbindService(BluetoothPbapClient.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothPbapClient.TAG, "", re);
                    }
                }
            }
            if (BluetoothPbapClient.this.mServiceListener != null) {
                BluetoothPbapClient.this.mServiceListener.onServiceDisconnected(17);
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothPbapClient mService;
    private ServiceListener mServiceListener;

    BluetoothPbapClient(Context context, ServiceListener l) {
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

    private boolean doBind() {
        Intent intent = new Intent(IBluetoothPbapClient.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth PBAP Client Service with ");
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

    public boolean connect(BluetoothDevice device) {
        IBluetoothPbapClient service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        IBluetoothPbapClient service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                service.disconnect(device);
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothPbapClient service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getConnectedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        IBluetoothPbapClient service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return new ArrayList();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        IBluetoothPbapClient service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == 12) {
            return true;
        }
        log("Bluetooth is Not enabled");
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        IBluetoothPbapClient service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device)) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        } else if (priority != 0 && priority != 100) {
            return false;
        } else {
            try {
                return service.setPriority(device, priority);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
    }

    public int getPriority(BluetoothDevice device) {
        IBluetoothPbapClient service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getPriority(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
                return 0;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }
}

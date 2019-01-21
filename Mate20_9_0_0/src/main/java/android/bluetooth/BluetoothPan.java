package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothPan.Stub;
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

public final class BluetoothPan implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_LOCAL_ROLE = "android.bluetooth.pan.extra.LOCAL_ROLE";
    public static final int LOCAL_NAP_ROLE = 1;
    public static final int LOCAL_PANU_ROLE = 2;
    public static final int PAN_CONNECT_FAILED_ALREADY_CONNECTED = 1001;
    public static final int PAN_CONNECT_FAILED_ATTEMPT_FAILED = 1002;
    public static final int PAN_DISCONNECT_FAILED_NOT_CONNECTED = 1000;
    public static final int PAN_OPERATION_GENERIC_FAILURE = 1003;
    public static final int PAN_OPERATION_SUCCESS = 1004;
    public static final int PAN_ROLE_NONE = 0;
    public static final int REMOTE_NAP_ROLE = 1;
    public static final int REMOTE_PANU_ROLE = 2;
    private static final String TAG = "BluetoothPan";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object connected");
            BluetoothPan.this.mPanService = Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothPan.this.mServiceListener != null) {
                BluetoothPan.this.mServiceListener.onServiceConnected(5, BluetoothPan.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothPan.TAG, "BluetoothPAN Proxy object disconnected");
            synchronized (BluetoothPan.this.mConnection) {
                if (BluetoothPan.this.mPanService != null) {
                    try {
                        BluetoothPan.this.mPanService = null;
                        BluetoothPan.this.mContext.unbindService(BluetoothPan.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothPan.TAG, "", re);
                    }
                }
            }
            if (BluetoothPan.this.mServiceListener != null) {
                BluetoothPan.this.mServiceListener.onServiceDisconnected(5);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothPan mPanService;
    private ServiceListener mServiceListener;
    private final IBluetoothStateChangeCallback mStateChangeCallback = new IBluetoothStateChangeCallback.Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean on) {
            String str = BluetoothPan.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange on: ");
            stringBuilder.append(on);
            Log.d(str, stringBuilder.toString());
            if (on) {
                try {
                    if (BluetoothPan.this.mPanService == null) {
                        BluetoothPan.this.doBind();
                        return;
                    }
                    return;
                } catch (IllegalStateException e) {
                    Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service: ", e);
                    return;
                } catch (SecurityException e2) {
                    Log.e(BluetoothPan.TAG, "onBluetoothStateChange: could not bind to PAN service: ", e2);
                    return;
                }
            }
            synchronized (BluetoothPan.this.mConnection) {
                try {
                    BluetoothPan.this.mPanService = null;
                    BluetoothPan.this.mContext.unbindService(BluetoothPan.this.mConnection);
                } catch (Exception re) {
                    Log.e(BluetoothPan.TAG, "", re);
                }
            }
        }
    };

    BluetoothPan(Context context, ServiceListener l) {
        this.mContext = context;
        this.mServiceListener = l;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            this.mAdapter.getBluetoothManager().registerStateChangeCallback(this.mStateChangeCallback);
        } catch (RemoteException re) {
            Log.w(TAG, "Unable to register BluetoothStateChangeCallback", re);
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothPan.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth Pan Service with ");
        stringBuilder.append(intent);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    void close() {
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mStateChangeCallback);
            } catch (RemoteException re) {
                Log.w(TAG, "Unable to unregister BluetoothStateChangeCallback", re);
            }
        }
        synchronized (this.mConnection) {
            if (this.mPanService != null) {
                try {
                    this.mPanService = null;
                    this.mContext.unbindService(this.mConnection);
                } catch (Exception re2) {
                    Log.e(TAG, "", re2);
                }
            }
        }
        this.mServiceListener = null;
    }

    protected void finalize() {
        close();
    }

    public boolean connect(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothPan service = this.mPanService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Stack:");
                stringBuilder2.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder2.toString());
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disconnect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothPan service = this.mPanService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Stack:");
                stringBuilder2.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder2.toString());
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothPan service = this.mPanService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getConnectedDevices();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return new ArrayList();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        IBluetoothPan service = this.mPanService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            return service.getDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return new ArrayList();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        IBluetoothPan service = this.mPanService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
                return 0;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return 0;
    }

    public void setBluetoothTethering(boolean value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBluetoothTethering(");
        stringBuilder.append(value);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothPan service = this.mPanService;
        if (service != null && isEnabled()) {
            try {
                service.setBluetoothTethering(value);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Stack:");
                stringBuilder2.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder2.toString());
            }
        }
    }

    public boolean isTetheringOn() {
        IBluetoothPan service = this.mPanService;
        if (service != null && isEnabled()) {
            try {
                return service.isTetheringOn();
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
            }
        }
        return false;
    }

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}

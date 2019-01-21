package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothMapClient implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_MESSAGE_DELIVERED_SUCCESSFULLY = "android.bluetooth.mapmce.profile.action.MESSAGE_DELIVERED_SUCCESSFULLY";
    public static final String ACTION_MESSAGE_RECEIVED = "android.bluetooth.mapmce.profile.action.MESSAGE_RECEIVED";
    public static final String ACTION_MESSAGE_SENT_SUCCESSFULLY = "android.bluetooth.mapmce.profile.action.MESSAGE_SENT_SUCCESSFULLY";
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    public static final String EXTRA_MESSAGE_HANDLE = "android.bluetooth.mapmce.profile.extra.MESSAGE_HANDLE";
    public static final String EXTRA_SENDER_CONTACT_NAME = "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_NAME";
    public static final String EXTRA_SENDER_CONTACT_URI = "android.bluetooth.mapmce.profile.extra.SENDER_CONTACT_URI";
    public static final int RESULT_CANCELED = 2;
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int STATE_ERROR = -1;
    private static final String TAG = "BluetoothMapClient";
    private static final boolean VDBG = Log.isLoggable(TAG, 2);
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            if (BluetoothMapClient.DBG) {
                String str = BluetoothMapClient.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onBluetoothStateChange: up=");
                stringBuilder.append(up);
                Log.d(str, stringBuilder.toString());
            }
            if (up) {
                synchronized (BluetoothMapClient.this.mConnection) {
                    try {
                        if (BluetoothMapClient.this.mService == null) {
                            if (BluetoothMapClient.VDBG) {
                                Log.d(BluetoothMapClient.TAG, "Binding service...");
                            }
                            BluetoothMapClient.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothMapClient.TAG, "", re);
                    }
                }
                return;
            }
            if (BluetoothMapClient.VDBG) {
                Log.d(BluetoothMapClient.TAG, "Unbinding service...");
            }
            synchronized (BluetoothMapClient.this.mConnection) {
                try {
                    BluetoothMapClient.this.mService = null;
                    BluetoothMapClient.this.mContext.unbindService(BluetoothMapClient.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothMapClient.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (BluetoothMapClient.DBG) {
                Log.d(BluetoothMapClient.TAG, "Proxy object connected");
            }
            BluetoothMapClient.this.mService = IBluetoothMapClient.Stub.asInterface(service);
            if (BluetoothMapClient.this.mServiceListener != null) {
                BluetoothMapClient.this.mServiceListener.onServiceConnected(18, BluetoothMapClient.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (BluetoothMapClient.DBG) {
                Log.d(BluetoothMapClient.TAG, "Proxy object disconnected");
            }
            synchronized (BluetoothMapClient.this.mConnection) {
                if (BluetoothMapClient.this.mService != null) {
                    try {
                        BluetoothMapClient.this.mService = null;
                        BluetoothMapClient.this.mContext.unbindService(BluetoothMapClient.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothMapClient.TAG, "", re);
                    }
                }
            }
            if (BluetoothMapClient.this.mServiceListener != null) {
                BluetoothMapClient.this.mServiceListener.onServiceDisconnected(18);
            }
        }
    };
    private final Context mContext;
    private volatile IBluetoothMapClient mService;
    private ServiceListener mServiceListener;

    BluetoothMapClient(Context context, ServiceListener l) {
        if (DBG) {
            Log.d(TAG, "Create BluetoothMapClient proxy object");
        }
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
        Intent intent = new Intent(IBluetoothMapClient.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth MAP MCE Service with ");
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

    public void close() {
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

    public boolean isConnected(BluetoothDevice device) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isConnected(");
            stringBuilder.append(device);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
        if (service != null) {
            try {
                return service.isConnected(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) {
                Log.d(TAG, Log.getStackTraceString(new Throwable()));
            }
            return false;
        }
    }

    public boolean connect(BluetoothDevice device) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("connect(");
            stringBuilder.append(device);
            stringBuilder.append(")for MAPS MCE");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
        if (service != null) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) {
                Log.d(TAG, Log.getStackTraceString(new Throwable()));
            }
            return false;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("disconnect(");
            stringBuilder.append(device);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        if (DBG) {
            Log.d(TAG, "getConnectedDevices()");
        }
        IBluetoothMapClient service = this.mService;
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
        if (DBG) {
            Log.d(TAG, "getDevicesMatchingStates()");
        }
        IBluetoothMapClient service = this.mService;
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
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getConnectionState(");
            stringBuilder.append(device);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
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

    public boolean setPriority(BluetoothDevice device, int priority) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPriority(");
            stringBuilder.append(device);
            stringBuilder.append(", ");
            stringBuilder.append(priority);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
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
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPriority(");
            stringBuilder.append(device);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
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

    public boolean sendMessage(BluetoothDevice device, Uri[] contacts, String message, PendingIntent sentIntent, PendingIntent deliveredIntent) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendMessage(");
            stringBuilder.append(device);
            stringBuilder.append(", ");
            stringBuilder.append(contacts);
            stringBuilder.append(", ");
            stringBuilder.append(message);
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device)) {
            return false;
        }
        try {
            return service.sendMessage(device, contacts, message, sentIntent, deliveredIntent);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    public boolean getUnreadMessages(BluetoothDevice device) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getUnreadMessages(");
            stringBuilder.append(device);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        IBluetoothMapClient service = this.mService;
        if (service == null || !isEnabled() || !isValidDevice(device)) {
            return false;
        }
        try {
            return service.getUnreadMessages(device);
        } catch (RemoteException e) {
            Log.e(TAG, Log.getStackTraceString(new Throwable()));
            return false;
        }
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.getState() == 12) {
            return true;
        }
        if (DBG) {
            Log.d(TAG, "Bluetooth is Not enabled");
        }
        return false;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }
}

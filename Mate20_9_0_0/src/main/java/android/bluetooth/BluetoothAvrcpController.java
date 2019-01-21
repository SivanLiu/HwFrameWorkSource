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

public final class BluetoothAvrcpController implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.action.PLAYER_SETTING";
    private static final boolean DBG = false;
    public static final String EXTRA_PLAYER_SETTING = "android.bluetooth.avrcp-controller.profile.extra.PLAYER_SETTING";
    private static final String TAG = "BluetoothAvrcpController";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                synchronized (BluetoothAvrcpController.this.mConnection) {
                    try {
                        if (BluetoothAvrcpController.this.mService == null) {
                            BluetoothAvrcpController.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothAvrcpController.TAG, "", re);
                    }
                }
                return;
            }
            synchronized (BluetoothAvrcpController.this.mConnection) {
                try {
                    BluetoothAvrcpController.this.mService = null;
                    BluetoothAvrcpController.this.mContext.unbindService(BluetoothAvrcpController.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothAvrcpController.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothAvrcpController.this.mService = IBluetoothAvrcpController.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothAvrcpController.this.mServiceListener != null) {
                BluetoothAvrcpController.this.mServiceListener.onServiceConnected(12, BluetoothAvrcpController.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            synchronized (BluetoothAvrcpController.this.mConnection) {
                if (BluetoothAvrcpController.this.mService != null) {
                    try {
                        BluetoothAvrcpController.this.mService = null;
                        BluetoothAvrcpController.this.mContext.unbindService(BluetoothAvrcpController.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothAvrcpController.TAG, "", re);
                    }
                }
            }
            if (BluetoothAvrcpController.this.mServiceListener != null) {
                BluetoothAvrcpController.this.mServiceListener.onServiceDisconnected(12);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothAvrcpController mService;
    private ServiceListener mServiceListener;

    BluetoothAvrcpController(Context context, ServiceListener l) {
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
        Intent intent = new Intent(IBluetoothAvrcpController.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth AVRCP Controller Service with ");
        stringBuilder.append(intent);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    void close() {
        this.mServiceListener = null;
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
    }

    public void finalize() {
        close();
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothAvrcpController service = this.mService;
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
        IBluetoothAvrcpController service = this.mService;
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
        IBluetoothAvrcpController service = this.mService;
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

    public BluetoothAvrcpPlayerSettings getPlayerSettings(BluetoothDevice device) {
        BluetoothAvrcpPlayerSettings settings = null;
        IBluetoothAvrcpController service = this.mService;
        if (service != null && isEnabled()) {
            try {
                settings = service.getPlayerSettings(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error talking to BT service in getMetadata() ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
        return settings;
    }

    public boolean setPlayerApplicationSetting(BluetoothAvrcpPlayerSettings plAppSetting) {
        IBluetoothAvrcpController service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return false;
        }
        try {
            return service.setPlayerApplicationSetting(plAppSetting);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error talking to BT service in setPlayerApplicationSetting() ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void sendGroupNavigationCmd(BluetoothDevice device, int keyCode, int keyState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendGroupNavigationCmd dev = ");
        stringBuilder.append(device);
        stringBuilder.append(" key ");
        stringBuilder.append(keyCode);
        stringBuilder.append(" State = ");
        stringBuilder.append(keyState);
        Log.d(str, stringBuilder.toString());
        IBluetoothAvrcpController service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return;
        }
        try {
            service.sendGroupNavigationCmd(device, keyCode, keyState);
        } catch (RemoteException e) {
            Log.e(TAG, "Error talking to BT service in sendGroupNavigationCmd()", e);
        }
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

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

public final class BluetoothHidHost implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_HANDSHAKE = "android.bluetooth.input.profile.action.HANDSHAKE";
    public static final String ACTION_IDLE_TIME_CHANGED = "android.bluetooth.input.profile.action.IDLE_TIME_CHANGED";
    public static final String ACTION_PROTOCOL_MODE_CHANGED = "android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED";
    public static final String ACTION_REPORT = "android.bluetooth.input.profile.action.REPORT";
    public static final String ACTION_VIRTUAL_UNPLUG_STATUS = "android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS";
    private static final boolean DBG = true;
    public static final String EXTRA_IDLE_TIME = "android.bluetooth.BluetoothHidHost.extra.IDLE_TIME";
    public static final String EXTRA_PROTOCOL_MODE = "android.bluetooth.BluetoothHidHost.extra.PROTOCOL_MODE";
    public static final String EXTRA_REPORT = "android.bluetooth.BluetoothHidHost.extra.REPORT";
    public static final String EXTRA_REPORT_BUFFER_SIZE = "android.bluetooth.BluetoothHidHost.extra.REPORT_BUFFER_SIZE";
    public static final String EXTRA_REPORT_ID = "android.bluetooth.BluetoothHidHost.extra.REPORT_ID";
    public static final String EXTRA_REPORT_TYPE = "android.bluetooth.BluetoothHidHost.extra.REPORT_TYPE";
    public static final String EXTRA_STATUS = "android.bluetooth.BluetoothHidHost.extra.STATUS";
    public static final String EXTRA_VIRTUAL_UNPLUG_STATUS = "android.bluetooth.BluetoothHidHost.extra.VIRTUAL_UNPLUG_STATUS";
    public static final int INPUT_CONNECT_FAILED_ALREADY_CONNECTED = 5001;
    public static final int INPUT_CONNECT_FAILED_ATTEMPT_FAILED = 5002;
    public static final int INPUT_DISCONNECT_FAILED_NOT_CONNECTED = 5000;
    public static final int INPUT_OPERATION_GENERIC_FAILURE = 5003;
    public static final int INPUT_OPERATION_SUCCESS = 5004;
    public static final int PROTOCOL_BOOT_MODE = 1;
    public static final int PROTOCOL_REPORT_MODE = 0;
    public static final int PROTOCOL_UNSUPPORTED_MODE = 255;
    public static final byte REPORT_TYPE_FEATURE = (byte) 3;
    public static final byte REPORT_TYPE_INPUT = (byte) 1;
    public static final byte REPORT_TYPE_OUTPUT = (byte) 2;
    private static final String TAG = "BluetoothHidHost";
    private static final boolean VDBG = false;
    public static final int VIRTUAL_UNPLUG_STATUS_FAIL = 1;
    public static final int VIRTUAL_UNPLUG_STATUS_SUCCESS = 0;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            String str = BluetoothHidHost.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange: up=");
            stringBuilder.append(up);
            Log.d(str, stringBuilder.toString());
            if (up) {
                synchronized (BluetoothHidHost.this.mConnection) {
                    try {
                        if (BluetoothHidHost.this.mService == null) {
                            BluetoothHidHost.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothHidHost.TAG, "", re);
                    }
                }
                return;
            }
            synchronized (BluetoothHidHost.this.mConnection) {
                try {
                    BluetoothHidHost.this.mService = null;
                    BluetoothHidHost.this.mContext.unbindService(BluetoothHidHost.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothHidHost.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothHidHost.TAG, "Proxy object connected");
            BluetoothHidHost.this.mService = IBluetoothHidHost.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothHidHost.this.mServiceListener != null) {
                BluetoothHidHost.this.mServiceListener.onServiceConnected(4, BluetoothHidHost.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothHidHost.TAG, "Proxy object disconnected");
            synchronized (BluetoothHidHost.this.mConnection) {
                if (BluetoothHidHost.this.mService != null) {
                    try {
                        BluetoothHidHost.this.mService = null;
                        BluetoothHidHost.this.mContext.unbindService(BluetoothHidHost.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothHidHost.TAG, "", re);
                    }
                }
            }
            if (BluetoothHidHost.this.mServiceListener != null) {
                BluetoothHidHost.this.mServiceListener.onServiceDisconnected(4);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothHidHost mService;
    private ServiceListener mServiceListener;

    BluetoothHidHost(Context context, ServiceListener l) {
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
        Intent intent = new Intent(IBluetoothHidHost.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth HID Service with ");
        stringBuilder.append(intent);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    void close() {
        infolog("close()");
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
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
        IBluetoothHidHost service = this.mService;
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
        IBluetoothHidHost service = this.mService;
        if (service == null || !isEnabled()) {
            if (service == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList();
        }
        try {
            if (this.mService.getConnectedDevices() == null) {
                return new ArrayList();
            }
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
        IBluetoothHidHost service = this.mService;
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
        IBluetoothHidHost service = this.mService;
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

    public boolean setPriority(BluetoothDevice device, int priority) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPriority(");
        stringBuilder.append(device);
        stringBuilder.append(", ");
        stringBuilder.append(priority);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
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
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Stack:");
                stringBuilder2.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder2.toString());
                return false;
            }
        }
    }

    public int getPriority(BluetoothDevice device) {
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getPriority(device);
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

    private boolean isEnabled() {
        return this.mAdapter.getState() == 12;
    }

    private static boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    public boolean virtualUnplug(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("virtualUnplug(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.virtualUnplug(device);
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

    public boolean getProtocolMode(BluetoothDevice device) {
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getProtocolMode(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean setProtocolMode(BluetoothDevice device, int protocolMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setProtocolMode(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.setProtocolMode(device, protocolMode);
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

    public boolean getReport(BluetoothDevice device, byte reportType, byte reportId, int bufferSize) {
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getReport(device, reportType, reportId, bufferSize);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean setReport(BluetoothDevice device, byte reportType, String report) {
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.setReport(device, reportType, report);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    public boolean sendData(BluetoothDevice device, String report) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendData(");
        stringBuilder.append(device);
        stringBuilder.append("), report=");
        stringBuilder.append(report);
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.sendData(device, report);
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

    public boolean getIdleTime(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIdletime(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getIdleTime(device);
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

    public boolean setIdleTime(BluetoothDevice device, byte idleTime) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setIdletime(");
        stringBuilder.append(device);
        stringBuilder.append("), idleTime=");
        stringBuilder.append(idleTime);
        log(stringBuilder.toString());
        IBluetoothHidHost service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.setIdleTime(device, idleTime);
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

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void infolog(String msg) {
        Log.i(TAG, msg);
    }
}

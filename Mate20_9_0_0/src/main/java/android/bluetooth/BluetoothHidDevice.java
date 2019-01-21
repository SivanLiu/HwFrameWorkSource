package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class BluetoothHidDevice implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED";
    public static final byte ERROR_RSP_INVALID_PARAM = (byte) 4;
    public static final byte ERROR_RSP_INVALID_RPT_ID = (byte) 2;
    public static final byte ERROR_RSP_NOT_READY = (byte) 1;
    public static final byte ERROR_RSP_SUCCESS = (byte) 0;
    public static final byte ERROR_RSP_UNKNOWN = (byte) 14;
    public static final byte ERROR_RSP_UNSUPPORTED_REQ = (byte) 3;
    public static final byte PROTOCOL_BOOT_MODE = (byte) 0;
    public static final byte PROTOCOL_REPORT_MODE = (byte) 1;
    public static final byte REPORT_TYPE_FEATURE = (byte) 3;
    public static final byte REPORT_TYPE_INPUT = (byte) 1;
    public static final byte REPORT_TYPE_OUTPUT = (byte) 2;
    public static final byte SUBCLASS1_COMBO = (byte) -64;
    public static final byte SUBCLASS1_KEYBOARD = (byte) 64;
    public static final byte SUBCLASS1_MOUSE = Byte.MIN_VALUE;
    public static final byte SUBCLASS1_NONE = (byte) 0;
    public static final byte SUBCLASS2_CARD_READER = (byte) 6;
    public static final byte SUBCLASS2_DIGITIZER_TABLET = (byte) 5;
    public static final byte SUBCLASS2_GAMEPAD = (byte) 2;
    public static final byte SUBCLASS2_JOYSTICK = (byte) 1;
    public static final byte SUBCLASS2_REMOTE_CONTROL = (byte) 3;
    public static final byte SUBCLASS2_SENSING_DEVICE = (byte) 4;
    public static final byte SUBCLASS2_UNCATEGORIZED = (byte) 0;
    private static final String TAG = BluetoothHidDevice.class.getSimpleName();
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        public void onBluetoothStateChange(boolean up) {
            String access$000 = BluetoothHidDevice.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange: up=");
            stringBuilder.append(up);
            Log.d(access$000, stringBuilder.toString());
            synchronized (BluetoothHidDevice.this.mConnection) {
                if (up) {
                    try {
                        if (BluetoothHidDevice.this.mService == null) {
                            Log.d(BluetoothHidDevice.TAG, "Binding HID Device service...");
                            BluetoothHidDevice.this.doBind();
                        }
                    } catch (IllegalStateException e) {
                        Log.e(BluetoothHidDevice.TAG, "onBluetoothStateChange: could not bind to HID Dev service: ", e);
                    } catch (SecurityException e2) {
                        Log.e(BluetoothHidDevice.TAG, "onBluetoothStateChange: could not bind to HID Dev service: ", e2);
                    }
                } else {
                    Log.d(BluetoothHidDevice.TAG, "Unbinding service...");
                    BluetoothHidDevice.this.doUnbind();
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothHidDevice.TAG, "onServiceConnected()");
            BluetoothHidDevice.this.mService = IBluetoothHidDevice.Stub.asInterface(service);
            if (BluetoothHidDevice.this.mServiceListener != null) {
                BluetoothHidDevice.this.mServiceListener.onServiceConnected(19, BluetoothHidDevice.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothHidDevice.TAG, "onServiceDisconnected()");
            synchronized (BluetoothHidDevice.this.mConnection) {
                if (BluetoothHidDevice.this.mService != null) {
                    try {
                        BluetoothHidDevice.this.mService = null;
                        BluetoothHidDevice.this.mContext.unbindService(BluetoothHidDevice.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothHidDevice.TAG, "", re);
                    }
                }
            }
            if (BluetoothHidDevice.this.mServiceListener != null) {
                BluetoothHidDevice.this.mServiceListener.onServiceDisconnected(19);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothHidDevice mService;
    private ServiceListener mServiceListener;

    public static abstract class Callback {
        private static final String TAG = "BluetoothHidDevCallback";

        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAppStatusChanged: pluggedDevice=");
            stringBuilder.append(pluggedDevice);
            stringBuilder.append(" registered=");
            stringBuilder.append(registered);
            Log.d(str, stringBuilder.toString());
        }

        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onConnectionStateChanged: device=");
            stringBuilder.append(device);
            stringBuilder.append(" state=");
            stringBuilder.append(state);
            Log.d(str, stringBuilder.toString());
        }

        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onGetReport: device=");
            stringBuilder.append(device);
            stringBuilder.append(" type=");
            stringBuilder.append(type);
            stringBuilder.append(" id=");
            stringBuilder.append(id);
            stringBuilder.append(" bufferSize=");
            stringBuilder.append(bufferSize);
            Log.d(str, stringBuilder.toString());
        }

        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetReport: device=");
            stringBuilder.append(device);
            stringBuilder.append(" type=");
            stringBuilder.append(type);
            stringBuilder.append(" id=");
            stringBuilder.append(id);
            Log.d(str, stringBuilder.toString());
        }

        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetProtocol: device=");
            stringBuilder.append(device);
            stringBuilder.append(" protocol=");
            stringBuilder.append(protocol);
            Log.d(str, stringBuilder.toString());
        }

        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onInterruptData: device=");
            stringBuilder.append(device);
            stringBuilder.append(" reportId=");
            stringBuilder.append(reportId);
            Log.d(str, stringBuilder.toString());
        }

        public void onVirtualCableUnplug(BluetoothDevice device) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onVirtualCableUnplug: device=");
            stringBuilder.append(device);
            Log.d(str, stringBuilder.toString());
        }
    }

    private static class CallbackWrapper extends IBluetoothHidDeviceCallback.Stub {
        private final Callback mCallback;
        private final Executor mExecutor;

        CallbackWrapper(Executor executor, Callback callback) {
            this.mExecutor = executor;
            this.mCallback = callback;
        }

        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$NFluHjT4zTfYBRXClu_2k6mPKFI(this, pluggedDevice, registered));
        }

        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$qtStwQVkGfOs2iJIiePWqJJpi0w(this, device, state));
        }

        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$Eyz_qG6mvTlh6a8Bp41ZoEJzQCQ(this, device, type, id, bufferSize));
        }

        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$3bTGVlfKj7Y0SZdifW_Ya2myDKs(this, device, type, id, data));
        }

        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$ypkr5GGxsAkGSBiLjIRwg-PzqCM(this, device, protocol));
        }

        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$xW99-tc95OmGApoKnpQ9q1TXb9k(this, device, reportId, data));
        }

        public void onVirtualCableUnplug(BluetoothDevice device) {
            clearCallingIdentity();
            this.mExecutor.execute(new -$$Lambda$BluetoothHidDevice$CallbackWrapper$jiodzbAJAcleQCwlDcBjvDddELM(this, device));
        }
    }

    BluetoothHidDevice(Context context, ServiceListener listener) {
        this.mContext = context;
        this.mServiceListener = listener;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BluetoothInputHost failed to unregisterStateChangeCallback: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothHidDevice.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not bind to Bluetooth HID Device Service with ");
            stringBuilder.append(intent);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        Log.d(TAG, "Bound to HID Device Service");
        return true;
    }

    void doUnbind() {
        if (this.mService != null) {
            this.mService = null;
            try {
                this.mContext.unbindService(this.mConnection);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unable to unbind HidDevService", e);
            }
        }
    }

    void close() {
        IBluetoothManager mgr = this.mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(this.mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to unregisterStateChangeCallback: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
            }
        }
        synchronized (this.mConnection) {
            doUnbind();
        }
        this.mServiceListener = null;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            return new ArrayList();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            return 0;
        }
    }

    public boolean registerApp(BluetoothHidDeviceAppSdpSettings sdp, BluetoothHidDeviceAppQosSettings inQos, BluetoothHidDeviceAppQosSettings outQos, Executor executor, Callback callback) {
        if (sdp == null) {
            throw new IllegalArgumentException("sdp parameter cannot be null");
        } else if (executor == null) {
            throw new IllegalArgumentException("executor parameter cannot be null");
        } else if (callback != null) {
            IBluetoothHidDevice service = this.mService;
            if (service != null) {
                try {
                    return service.registerApp(sdp, inQos, outQos, new CallbackWrapper(executor, callback));
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                    return false;
                }
            }
            Log.w(TAG, "Proxy not attached to service");
            return false;
        } else {
            throw new IllegalArgumentException("callback parameter cannot be null");
        }
    }

    public boolean unregisterApp() {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.unregisterApp();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    public boolean sendReport(BluetoothDevice device, int id, byte[] data) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.sendReport(device, id, data);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    public boolean replyReport(BluetoothDevice device, byte type, byte id, byte[] data) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.replyReport(device, type, id, data);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    public boolean reportError(BluetoothDevice device, byte error) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.reportError(device, error);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    public String getUserAppName() {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.getUserAppName();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            return "";
        }
    }

    public boolean connect(BluetoothDevice device) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }

    public boolean disconnect(BluetoothDevice device) {
        IBluetoothHidDevice service = this.mService;
        if (service != null) {
            try {
                return service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
                return false;
            }
        }
        Log.w(TAG, "Proxy not attached to service");
        return false;
    }
}

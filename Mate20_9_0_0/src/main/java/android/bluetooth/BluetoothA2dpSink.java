package android.bluetooth;

import android.bluetooth.BluetoothProfile.ServiceListener;
import android.bluetooth.IBluetoothStateChangeCallback.Stub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class BluetoothA2dpSink implements BluetoothProfile {
    public static final String ACTION_AUDIO_CONFIG_CHANGED = "android.bluetooth.a2dp-sink.profile.action.AUDIO_CONFIG_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = true;
    public static final String EXTRA_AUDIO_CONFIG = "android.bluetooth.a2dp-sink.profile.extra.AUDIO_CONFIG";
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothA2dpSink";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onBluetoothStateChange(boolean up) {
            String str = BluetoothA2dpSink.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onBluetoothStateChange: up=");
            stringBuilder.append(up);
            Log.d(str, stringBuilder.toString());
            if (up) {
                synchronized (BluetoothA2dpSink.this.mConnection) {
                    try {
                        if (BluetoothA2dpSink.this.mService == null) {
                            BluetoothA2dpSink.this.doBind();
                        }
                    } catch (Exception re) {
                        Log.e(BluetoothA2dpSink.TAG, "", re);
                    }
                }
                return;
            }
            synchronized (BluetoothA2dpSink.this.mConnection) {
                try {
                    BluetoothA2dpSink.this.mService = null;
                    BluetoothA2dpSink.this.mContext.unbindService(BluetoothA2dpSink.this.mConnection);
                } catch (Exception re2) {
                    Log.e(BluetoothA2dpSink.TAG, "", re2);
                }
            }
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object connected");
            BluetoothA2dpSink.this.mService = IBluetoothA2dpSink.Stub.asInterface(Binder.allowBlocking(service));
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceConnected(11, BluetoothA2dpSink.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(BluetoothA2dpSink.TAG, "Proxy object disconnected");
            synchronized (BluetoothA2dpSink.this.mConnection) {
                if (BluetoothA2dpSink.this.mService != null) {
                    try {
                        BluetoothA2dpSink.this.mService = null;
                        BluetoothA2dpSink.this.mContext.unbindService(BluetoothA2dpSink.this.mConnection);
                    } catch (Exception re) {
                        Log.e(BluetoothA2dpSink.TAG, "", re);
                    }
                }
            }
            if (BluetoothA2dpSink.this.mServiceListener != null) {
                BluetoothA2dpSink.this.mServiceListener.onServiceDisconnected(11);
            }
        }
    };
    private Context mContext;
    private volatile IBluetoothA2dpSink mService;
    private ServiceListener mServiceListener;

    BluetoothA2dpSink(Context context, ServiceListener l) {
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
        Intent intent = new Intent(IBluetoothA2dpSink.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && this.mContext.bindServiceAsUser(intent, this.mConnection, 0, this.mContext.getUser())) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not bind to Bluetooth A2DP Service with ");
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

    public boolean connect(BluetoothDevice device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connect(");
        stringBuilder.append(device);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothA2dpSink service = this.mService;
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
        IBluetoothA2dpSink service = this.mService;
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
        IBluetoothA2dpSink service = this.mService;
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
        IBluetoothA2dpSink service = this.mService;
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
        IBluetoothA2dpSink service = this.mService;
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

    public BluetoothAudioConfig getAudioConfig(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.getAudioConfig(device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stack:");
                stringBuilder.append(Log.getStackTraceString(new Throwable()));
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
        if (service == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return null;
    }

    public boolean setPriority(BluetoothDevice device, int priority) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPriority(");
        stringBuilder.append(device);
        stringBuilder.append(", ");
        stringBuilder.append(priority);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        IBluetoothA2dpSink service = this.mService;
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
        IBluetoothA2dpSink service = this.mService;
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

    public boolean isA2dpPlaying(BluetoothDevice device) {
        IBluetoothA2dpSink service = this.mService;
        if (service != null && isEnabled() && isValidDevice(device)) {
            try {
                return service.isA2dpPlaying(device);
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

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "disconnected";
            case 1:
                return "connecting";
            case 2:
                return UsbManager.USB_CONNECTED;
            case 3:
                return "disconnecting";
            default:
                switch (state) {
                    case 10:
                        return "playing";
                    case 11:
                        return "not playing";
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<unknown state ");
                        stringBuilder.append(state);
                        stringBuilder.append(">");
                        return stringBuilder.toString();
                }
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

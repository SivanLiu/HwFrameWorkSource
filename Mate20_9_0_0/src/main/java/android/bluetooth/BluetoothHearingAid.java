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
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class BluetoothHearingAid implements BluetoothProfile {
    public static final String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED";
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_PLAYING_STATE_CHANGED = "android.bluetooth.hearingaid.profile.action.PLAYING_STATE_CHANGED";
    private static final boolean DBG = false;
    public static final long HI_SYNC_ID_INVALID = 0;
    public static final int MODE_BINAURAL = 1;
    public static final int MODE_MONAURAL = 0;
    public static final int SIDE_LEFT = 0;
    public static final int SIDE_RIGHT = 1;
    public static final int STATE_NOT_PLAYING = 11;
    public static final int STATE_PLAYING = 10;
    private static final String TAG = "BluetoothHearingAid";
    private static final boolean VDBG = false;
    private BluetoothAdapter mAdapter;
    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback = new Stub() {
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                try {
                    BluetoothHearingAid.this.mServiceLock.readLock().lock();
                    if (BluetoothHearingAid.this.mService == null) {
                        BluetoothHearingAid.this.doBind();
                    }
                } catch (Exception re) {
                    Log.e(BluetoothHearingAid.TAG, "", re);
                } catch (Throwable th) {
                    BluetoothHearingAid.this.mServiceLock.readLock().unlock();
                }
                BluetoothHearingAid.this.mServiceLock.readLock().unlock();
                return;
            }
            try {
                BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                BluetoothHearingAid.this.mService = null;
                BluetoothHearingAid.this.mContext.unbindService(BluetoothHearingAid.this.mConnection);
            } catch (Exception re2) {
                Log.e(BluetoothHearingAid.TAG, "", re2);
            } catch (Throwable th2) {
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
            }
            BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            try {
                BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                BluetoothHearingAid.this.mService = IBluetoothHearingAid.Stub.asInterface(Binder.allowBlocking(service));
                if (BluetoothHearingAid.this.mServiceListener != null) {
                    BluetoothHearingAid.this.mServiceListener.onServiceConnected(21, BluetoothHearingAid.this);
                }
            } finally {
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            try {
                BluetoothHearingAid.this.mServiceLock.writeLock().lock();
                if (BluetoothHearingAid.this.mService != null) {
                    BluetoothHearingAid.this.mService = null;
                    BluetoothHearingAid.this.mContext.unbindService(BluetoothHearingAid.this.mConnection);
                }
            } catch (Exception e) {
                Log.e(BluetoothHearingAid.TAG, "", e);
            } catch (Throwable th) {
                BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
            }
            BluetoothHearingAid.this.mServiceLock.writeLock().unlock();
            if (BluetoothHearingAid.this.mServiceListener != null) {
                BluetoothHearingAid.this.mServiceListener.onServiceDisconnected(21);
            }
        }
    };
    private Context mContext;
    @GuardedBy("mServiceLock")
    private IBluetoothHearingAid mService;
    private ServiceListener mServiceListener;
    private final ReentrantReadWriteLock mServiceLock = new ReentrantReadWriteLock();

    BluetoothHearingAid(Context context, ServiceListener l) {
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

    void doBind() {
        Intent intent = new Intent(IBluetoothHearingAid.class.getName());
        ComponentName comp = intent.resolveSystemService(this.mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !this.mContext.bindServiceAsUser(intent, this.mConnection, 0, Process.myUserHandle())) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not bind to Bluetooth Hearing Aid Service with ");
            stringBuilder.append(intent);
            Log.e(str, stringBuilder.toString());
        }
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
        try {
            this.mServiceLock.writeLock().lock();
            if (this.mService != null) {
                this.mService = null;
                this.mContext.unbindService(this.mConnection);
            }
        } catch (Exception re) {
            Log.e(TAG, "", re);
        } catch (Throwable th) {
            this.mServiceLock.writeLock().unlock();
        }
        this.mServiceLock.writeLock().unlock();
    }

    public void finalize() {
    }

    public boolean connect(BluetoothDevice device) {
        boolean connect;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                connect = this.mService.connect(device);
                return connect;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return false;
        } finally {
            connect = this.mServiceLock.readLock();
            connect.unlock();
            return false;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        boolean disconnect;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                disconnect = this.mService.disconnect(device);
                return disconnect;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return false;
        } finally {
            disconnect = this.mServiceLock.readLock();
            disconnect.unlock();
            return false;
        }
    }

    public List<BluetoothDevice> getConnectedDevices() {
        List<BluetoothDevice> arrayList;
        List<BluetoothDevice> e;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                ArrayList arrayList2 = new ArrayList();
                this.mServiceLock.readLock().unlock();
                return arrayList2;
            }
            e = this.mService.getConnectedDevices();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            arrayList = new ArrayList();
            return arrayList;
        } finally {
            arrayList = this.mServiceLock.readLock();
            arrayList.unlock();
        }
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> arrayList;
        List<BluetoothDevice> e;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                ArrayList arrayList2 = new ArrayList();
                this.mServiceLock.readLock().unlock();
                return arrayList2;
            }
            e = this.mService.getDevicesMatchingConnectionStates(states);
            return e;
        } catch (RemoteException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            arrayList = new ArrayList();
            return arrayList;
        } finally {
            arrayList = this.mServiceLock.readLock();
            arrayList.unlock();
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        int connectionState;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                connectionState = this.mService.getConnectionState(device);
                return connectionState;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            connectionState = this.mServiceLock.readLock();
            connectionState.unlock();
            return 0;
        }
    }

    public boolean setActiveDevice(BluetoothDevice device) {
        boolean z = false;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && (device == null || isValidDevice(device))) {
                this.mService.setActiveDevice(device);
                z = true;
                return z;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return z;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public List<BluetoothDevice> getActiveDevices() {
        List<BluetoothDevice> arrayList;
        List<BluetoothDevice> e;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                ArrayList arrayList2 = new ArrayList();
                this.mServiceLock.readLock().unlock();
                return arrayList2;
            }
            e = this.mService.getActiveDevices();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            arrayList = new ArrayList();
            return arrayList;
        } finally {
            arrayList = this.mServiceLock.readLock();
            arrayList.unlock();
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:12:0x002b=Splitter:B:12:0x002b, B:16:0x003b=Splitter:B:16:0x003b} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setPriority(BluetoothDevice device, int priority) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled() || !isValidDevice(device)) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return false;
            } else if (priority != 0 && priority != 100) {
                return false;
            } else {
                boolean priority2 = this.mService.setPriority(device, priority);
                this.mServiceLock.readLock().unlock();
                return priority2;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return false;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getPriority(BluetoothDevice device) {
        int priority;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                priority = this.mService.getPriority(device);
                return priority;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            priority = this.mServiceLock.readLock();
            priority.unlock();
            return 0;
        }
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

    public int getVolume() {
        int volume;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null || !isEnabled()) {
                if (this.mService == null) {
                    Log.w(TAG, "Proxy not attached to service");
                }
                this.mServiceLock.readLock().unlock();
                return 0;
            }
            volume = this.mService.getVolume();
            return volume;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            volume = this.mServiceLock.readLock();
            volume.unlock();
            return 0;
        }
    }

    public void adjustVolume(int direction) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
                this.mServiceLock.readLock().unlock();
            } else if (isEnabled()) {
                this.mService.adjustVolume(direction);
                this.mServiceLock.readLock().unlock();
            } else {
                this.mServiceLock.readLock().unlock();
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
        }
    }

    public void setVolume(int volume) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
                this.mServiceLock.readLock().unlock();
            } else if (isEnabled()) {
                this.mService.setVolume(volume);
                this.mServiceLock.readLock().unlock();
            } else {
                this.mServiceLock.readLock().unlock();
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            this.mServiceLock.readLock().unlock();
        }
    }

    public long getHiSyncId(BluetoothDevice device) {
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
                return 0;
            }
            if (isEnabled()) {
                if (isValidDevice(device)) {
                    long hiSyncId = this.mService.getHiSyncId(device);
                    this.mServiceLock.readLock().unlock();
                    return hiSyncId;
                }
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            this.mServiceLock.readLock().unlock();
        }
    }

    public int getDeviceSide(BluetoothDevice device) {
        int deviceSide;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                deviceSide = this.mService.getDeviceSide(device);
                return deviceSide;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            deviceSide = this.mServiceLock.readLock();
            deviceSide.unlock();
            return 0;
        }
    }

    public int getDeviceMode(BluetoothDevice device) {
        int deviceMode;
        try {
            this.mServiceLock.readLock().lock();
            if (this.mService != null && isEnabled() && isValidDevice(device)) {
                deviceMode = this.mService.getDeviceMode(device);
                return deviceMode;
            }
            if (this.mService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            this.mServiceLock.readLock().unlock();
            return 0;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stack:");
            stringBuilder.append(Log.getStackTraceString(new Throwable()));
            Log.e(str, stringBuilder.toString());
            return 0;
        } finally {
            deviceMode = this.mServiceLock.readLock();
            deviceMode.unlock();
            return 0;
        }
    }

    private boolean isEnabled() {
        if (this.mAdapter.getState() == 12) {
            return true;
        }
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress())) {
            return true;
        }
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
